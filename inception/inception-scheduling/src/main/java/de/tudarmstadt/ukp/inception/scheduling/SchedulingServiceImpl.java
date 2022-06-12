/*
 * Licensed to the Technische Universität Darmstadt under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The Technische Universität Darmstadt 
 * licenses this file to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.
 *  
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package de.tudarmstadt.ukp.inception.scheduling;

import static de.tudarmstadt.ukp.inception.scheduling.MatchResult.NO_MATCH;
import static de.tudarmstadt.ukp.inception.scheduling.MatchResult.UNQUEUE_EXISTING_AND_QUEUE_THIS;
import static java.util.concurrent.TimeUnit.SECONDS;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.function.Predicate;

import org.apache.commons.lang3.Validate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.AutowireCapableBeanFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.context.event.EventListener;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.security.core.session.SessionDestroyedEvent;
import org.springframework.security.core.session.SessionInformation;
import org.springframework.security.core.session.SessionRegistry;

import de.tudarmstadt.ukp.clarin.webanno.api.event.AfterProjectRemovedEvent;
import de.tudarmstadt.ukp.clarin.webanno.api.event.BeforeProjectRemovedEvent;
import de.tudarmstadt.ukp.clarin.webanno.model.Project;
import de.tudarmstadt.ukp.clarin.webanno.security.model.User;
import de.tudarmstadt.ukp.inception.scheduling.config.SchedulingProperties;
import de.tudarmstadt.ukp.inception.scheduling.config.SchedulingServiceAutoConfiguration;

/**
 * <p>
 * This class is exposed as a Spring Component via
 * {@link SchedulingServiceAutoConfiguration#schedulingService}.
 * </p>
 */
public class SchedulingServiceImpl
    implements SchedulingService, DisposableBean
{
    private static final Logger log = LoggerFactory.getLogger(SchedulingService.class);

    private final ApplicationContext applicationContext;
    private final ThreadPoolExecutor executor;
    private final ScheduledExecutorService watchdog;
    private final SessionRegistry sessionRegistry;

    private final List<Task> runningTasks;
    private final List<Task> enqueuedTasks;
    private final Set<Project> deletionPending;

    @Autowired
    public SchedulingServiceImpl(ApplicationContext aApplicationContext,
            SchedulingProperties aConfig, SessionRegistry aSessionRegistry)
    {
        sessionRegistry = aSessionRegistry;
        applicationContext = aApplicationContext;
        executor = new InspectableThreadPoolExecutor(aConfig.getNumberOfThreads(),
                aConfig.getQueueSize(), this::beforeExecute, this::afterExecute);
        runningTasks = Collections.synchronizedList(new ArrayList<>());
        enqueuedTasks = Collections.synchronizedList(new ArrayList<>());
        deletionPending = Collections.synchronizedSet(new LinkedHashSet<>());
        watchdog = Executors.newScheduledThreadPool(1);
        watchdog.scheduleAtFixedRate(this::scheduleEligibleTasks, 5, 5, SECONDS);
    }

    private void beforeExecute(Thread aThread, Runnable aRunnable)
    {
        runningTasks.add((Task) aRunnable);
        log.debug("Starting task [{}]", aRunnable);
    }

    private void afterExecute(Runnable aRunnable, Throwable aThrowable)
    {
        runningTasks.remove(aRunnable);
        log.debug("Completed task [{}]", aRunnable);
        scheduleEligibleTasks();
    }

    /**
     * @return tasks which have not been handed to the executor yet.
     */
    @Override
    public List<Task> getEnqueuedTasks()
    {
        // We return copy here, as else the list the receiver sees might be updated
        // when new tasks are running or existing ones stopped.
        return new ArrayList<>(enqueuedTasks);
    }

    /**
     * @return tasks which have been handed to the executor but have not yet been started.
     */
    @Override
    public List<Task> getScheduledTasks()
    {
        List<Task> result = new ArrayList<>();
        executor.getQueue().forEach(r -> result.add((Task) r));
        return result;
    }

    /**
     * @return tasks which have been already been started.
     */
    @Override
    public List<Task> getRunningTasks()
    {
        // We return copy here, as else the list the receiver sees might be updated
        // when new tasks are running or existing ones stopped.
        return new ArrayList<>(runningTasks);
    }

    @Override
    public List<Task> getScheduledAndRunningTasks()
    {
        List<Task> result = new ArrayList<>();
        result.addAll(getScheduledTasks());
        result.addAll(getRunningTasks());
        return result;
    }

    @Override
    public List<Task> getAllTasks()
    {
        List<Task> result = new ArrayList<>();
        result.addAll(getEnqueuedTasks());
        result.addAll(getScheduledTasks());
        result.addAll(getRunningTasks());
        return result;
    }

    /**
     * Enqueue a task. This may not immediately schedule or run the task. If an equivalent task
     * (i.e. one that {@link Object#equals} the given task) has already been scheduled or started,
     * the given task will be queued. Queued tasks only become eligible for actual scheduling or
     * running once no equivalent task is running or scheduled. If the queue already contains an
     * equivalent to the given task, then the old task is replaced with the new one in the queue.
     * <p>
     * The separation between enqueued and scheduled tasks is necessary to allow the ability to run
     * multiple tasks in parallel while at the same time avoiding running equivalent tasks in
     * parallel.
     * 
     * @param aTask
     *            the task to be enqueued.
     */
    @Override
    public synchronized void enqueue(Task aTask)
    {
        if (aTask.getProject() != null && deletionPending.contains(aTask.getProject())) {
            log.debug("Not enqueuing task [{}] for project {} pending deletion", aTask,
                    aTask.getProject());
            return;
        }

        List<Task> tasksToUnqueue = new ArrayList<>();
        for (Task enqueuedTask : enqueuedTasks) {
            switch (matchTask(aTask, enqueuedTask)) {
            case DISCARD_OR_QUEUE_THIS:
                // Check if the incoming task should be discarded
                log.debug("Matching task already queued - keeping existing: [{}] and discarding "
                        + "incoming [{}]", enqueuedTask, aTask);
                return;
            case UNQUEUE_EXISTING_AND_QUEUE_THIS:
                // Check if any existing tasks should be replaced with the new incoming task (i.e.
                // the incoming task supersedes them).
                tasksToUnqueue.add(enqueuedTask);
                break;
            case NO_MATCH:
                // Ignore
                break;
            }
        }

        for (Task taskToUnqueue : tasksToUnqueue) {
            log.debug("Matching task already queued - unqueuing exsting: [{}] in favor of "
                    + "incoming [{}]", taskToUnqueue, aTask);
            enqueuedTasks.remove(taskToUnqueue);
        }

        if (containsMatchingTask(getScheduledTasks(), aTask)) {
            log.debug("Matching task already scheduled - adding to queue: [{}]", aTask);
            enqueuedTasks.add(aTask);
            return;
        }

        if (containsMatchingTask(getRunningTasks(), aTask)) {
            log.debug("Matching task already running - adding to queue: [{}]", aTask);
            enqueuedTasks.add(aTask);
            return;
        }

        if (!aTask.isReadyToStart()) {
            log.debug("Task not yet ready to start - adding to queue: [{}]", aTask);
            enqueuedTasks.add(aTask);
            return;
        }

        schedule(aTask);

        logState();
    }

    private MatchResult matchTask(Task aTask, Task aEnqueueTask)
    {
        if (aTask instanceof MatchableTask) {
            return ((MatchableTask) aTask).matches(aEnqueueTask);
        }

        return aTask.equals(aEnqueueTask) ? UNQUEUE_EXISTING_AND_QUEUE_THIS : NO_MATCH;
    }

    private boolean containsMatchingTask(Collection<Task> aTasks, Task aTask)
    {
        if (aTask instanceof MatchableTask) {
            return aTasks.stream().anyMatch(t -> ((MatchableTask) aTask).matches(t) != NO_MATCH);
        }

        return aTasks.contains(aTask);
    }

    /**
     * Send a task to the scheduler. Once a task has been scheduled, it will be executed eventually.
     * 
     * @param aTask
     *            the task to be scheduled.
     */
    private void schedule(Task aTask)
    {
        log.debug("Scheduling task [{}]", aTask);

        // This auto-wires the task fields manually
        AutowireCapableBeanFactory factory = applicationContext.getAutowireCapableBeanFactory();
        factory.autowireBean(aTask);
        factory.initializeBean(aTask, "transientTask");

        executor.execute(aTask);
    }

    private synchronized void scheduleEligibleTasks()
    {
        Iterator<Task> i = enqueuedTasks.iterator();

        while (i.hasNext()) {
            Task t = i.next();
            if (!getScheduledAndRunningTasks().contains(t) && t.isReadyToStart()) {
                i.remove();
                schedule(t);
            }
        }

        logState();
    }

    /**
     * Removes all task for the user with name {@code aUsername} from the scheduler's queue.
     * 
     * @param aUserName
     *            The name of the user whose tasks will be removed.
     */
    @Override
    public void stopAllTasksForUser(String aUserName)
    {
        Validate.notNull(aUserName, "User name must be specified");

        stopAllTasksMatching(
                t -> t.getUser().map(_user -> aUserName.equals(_user.getUsername())).orElse(false));
    }

    /**
     * Removes all task for the given project from the scheduler's queue.
     * 
     * @param aProject
     *            The project whose tasks will be removed.
     */
    @Override
    public void stopAllTasksForProject(Project aProject)
    {
        Validate.notNull(aProject, "Project name must be specified");

        stopAllTasksMatching(t -> t.getProject().equals(aProject));
    }

    @Override
    public synchronized void stopAllTasksMatching(Predicate<Task> aPredicate)
    {
        enqueuedTasks.removeIf(aPredicate);
        executor.getQueue().removeIf(runnable -> aPredicate.test((Task) runnable));

        // TODO: Stop the running tasks as well
    }

    @EventListener
    public void beforeProjectRemoved(BeforeProjectRemovedEvent aEvent) throws IOException
    {
        deletionPending.add(aEvent.getProject());
        stopAllTasksForProject(aEvent.getProject());
    }

    @EventListener
    public void afterProjectRemoved(AfterProjectRemovedEvent aEvent) throws IOException
    {
        stopAllTasksForProject(aEvent.getProject());
        deletionPending.remove(aEvent.getProject());
    }

    @EventListener
    @Order(Ordered.HIGHEST_PRECEDENCE)
    public void onSessionDestroyed(SessionDestroyedEvent event)
    {
        SessionInformation info = sessionRegistry.getSessionInformation(event.getId());
        // Could be an anonymous session without information.
        if (info == null) {
            return;
        }

        String username = null;
        if (info.getPrincipal() instanceof String) {
            username = (String) info.getPrincipal();
        }

        if (info.getPrincipal() instanceof User) {
            username = ((User) info.getPrincipal()).getUsername();
        }

        if (username != null) {
            stopAllTasksForUser(username);
        }
    }

    @Override
    public void destroy()
    {
        log.info("Shutting down scheduling service!");
        enqueuedTasks.clear();
        executor.getQueue().clear();
        watchdog.shutdownNow();
        executor.shutdownNow();
    }

    private void logState()
    {
        getEnqueuedTasks().forEach(t -> log.debug("Queued   : {}", t));
        getScheduledTasks().forEach(t -> log.debug("Scheduled: {}", t));
        getRunningTasks().forEach(t -> log.debug("Running  : {}", t));
    }

    @Override
    public void executeSync(Task aTask)
    {
        AutowireCapableBeanFactory factory = applicationContext.getAutowireCapableBeanFactory();
        factory.autowireBean(aTask);
        factory.initializeBean(aTask, "transientTask");
        aTask.execute(); // Execute synchronously - blocking
    }
}

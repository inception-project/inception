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
import java.lang.invoke.MethodHandles;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
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
import org.springframework.context.ApplicationContext;
import org.springframework.context.event.EventListener;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.security.core.session.SessionDestroyedEvent;
import org.springframework.security.core.session.SessionRegistry;

import de.tudarmstadt.ukp.clarin.webanno.model.Project;
import de.tudarmstadt.ukp.clarin.webanno.security.model.User;
import de.tudarmstadt.ukp.inception.project.api.event.AfterProjectRemovedEvent;
import de.tudarmstadt.ukp.inception.project.api.event.BeforeProjectRemovedEvent;
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
    private static final Logger LOG = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    private final ApplicationContext applicationContext;
    private final ThreadPoolExecutor executor;
    private final ScheduledExecutorService watchdog;
    private final SessionRegistry sessionRegistry;

    private final List<Task> runningTasks;
    private final List<Task> enqueuedTasks;
    private final List<Task> pendingAcknowledgement;
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
        pendingAcknowledgement = Collections.synchronizedList(new ArrayList<>());
        deletionPending = Collections.synchronizedSet(new LinkedHashSet<>());
        watchdog = Executors.newScheduledThreadPool(1);
        watchdog.scheduleAtFixedRate(this::scheduleEligibleTasks, 5, 5, SECONDS);
        watchdog.scheduleAtFixedRate(this::cleanUpTasks, 10, 10, SECONDS);
    }

    private void beforeExecute(Thread aThread, Runnable aRunnable)
    {
        Validate.notNull(aRunnable, "Task cannot be null");
        runningTasks.add((Task) aRunnable);
        LOG.debug("Starting task: {} ", aRunnable);
    }

    private void afterExecute(Runnable aRunnable, Throwable aThrowable)
    {
        Validate.notNull(aRunnable, "Task cannot be null");

        var task = (Task) aRunnable;

        LOG.debug("Ended task [{}]: {}", task, task.getMonitor().getState());
        handleTaskEnded(task);

        scheduleEligibleTasks();
    }

    private void handleTaskEnded(Task aTask)
    {
        runningTasks.remove(aTask);
        if (aTask.getMonitor().isCancelled() || !aTask.getScope().isDestroyOnEnd()) {
            pendingAcknowledgement.add(aTask);
        }
        else {
            aTask.destroy();
        }
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

    /**
     * @return tasks which are no longer running (completed, failed) and which require the user to
     *         acknowledge the result.
     */
    @Override
    public List<Task> getTasksPendingAcknowledgment()
    {
        // We return copy here, as else the list the receiver sees might be updated
        // when new tasks are running or existing ones stopped.
        return new ArrayList<>(pendingAcknowledgement);
    }

    @Override
    public List<Task> getScheduledAndRunningTasks()
    {
        var result = new ArrayList<Task>();
        result.addAll(getScheduledTasks());
        result.addAll(getRunningTasks());
        return result;
    }

    @Override
    public List<Task> getAllTasks()
    {
        var result = new ArrayList<Task>();
        result.addAll(getEnqueuedTasks());
        result.addAll(getScheduledTasks());
        result.addAll(getRunningTasks());
        result.addAll(getTasksPendingAcknowledgment());
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
        Validate.notNull(aTask, "Task cannot be null");

        if (aTask.getProject() != null && deletionPending.contains(aTask.getProject())) {
            LOG.debug("Not enqueuing task [{}] for project {} pending deletion", aTask,
                    aTask.getProject());
            return;
        }

        var tasksToUnqueue = new ArrayList<Task>();
        for (var enqueuedTask : enqueuedTasks) {
            switch (matchTask(aTask, enqueuedTask)) {
            case DISCARD_OR_QUEUE_THIS:
                // Check if the incoming task should be discarded
                LOG.debug("Matching task already queued - keeping existing: [{}] and discarding "
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
            LOG.debug("Matching task already queued - unqueuing exsting: [{}] in favor of "
                    + "incoming [{}]", taskToUnqueue, aTask);
            enqueuedTasks.remove(taskToUnqueue);
        }

        if (containsMatchingTask(getScheduledTasks(), aTask)) {
            LOG.debug("Matching task already scheduled - adding to queue: [{}]", aTask);
            enqueuedTasks.add(aTask);
            return;
        }

        if (containsMatchingTask(getRunningTasks(), aTask)) {
            LOG.debug("Matching task already running - adding to queue: [{}]", aTask);
            enqueuedTasks.add(aTask);
            return;
        }

        if (!aTask.isReadyToStart()) {
            LOG.debug("Task not yet ready to start - adding to queue: [{}]", aTask);
            enqueuedTasks.add(aTask);
            return;
        }

        schedule(aTask);

        logState();
    }

    private MatchResult matchTask(Task aTask, Task aEnqueueTask)
    {
        if (aTask instanceof MatchableTask task) {
            return task.matches(aEnqueueTask);
        }

        return aTask.equals(aEnqueueTask) ? UNQUEUE_EXISTING_AND_QUEUE_THIS : NO_MATCH;
    }

    private boolean containsMatchingTask(Collection<Task> aTasks, Task aTask)
    {
        if (aTask instanceof MatchableTask task) {
            return aTasks.stream().anyMatch(t -> task.matches(t) != NO_MATCH);
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
        LOG.debug("Scheduling task [{}]", aTask);

        try {
            // This auto-wires the task fields manually
            var factory = applicationContext.getAutowireCapableBeanFactory();
            factory.autowireBean(aTask);
            factory.initializeBean(aTask, "transientTask");
        }
        catch (Exception e) {
            LOG.error("Error initializing task [{}]", aTask, e);
        }

        executor.execute(aTask);
    }

    private synchronized void cleanUpTasks()
    {
        // var activeSessionCount = 0;
        var activeUsers = new HashSet<String>();
        for (var principal : sessionRegistry.getAllPrincipals()) {
            var sessions = sessionRegistry.getAllSessions(principal, false);
            if (!sessions.isEmpty()) {
                activeUsers.add(getUsernameFromPrincipal(principal));
                // activeSessionCount += sessions.size();
            }
        }

        // LOG.debug("Found a total of [{}] active sessions for users {}", activeSessionCount,
        // activeUsers);

        stopAllTasksMatching(t -> {
            var requiresActiveUser = t.getScope().isRemoveWhenUserSessionEnds()
                    || t.getScope().isRemoveWhenLastUserSessionEnds();

            var ownedByActiveUser = t.getUser().map(u -> activeUsers.contains(u.getUsername()))
                    .orElse(false);

            if (requiresActiveUser && !ownedByActiveUser) {
                LOG.debug("Task {} requires active user but user is not logged in - cleaning up",
                        t);
                return true;
            }

            return false;
        });

        logState();
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
    public synchronized Optional<Task> findTask(Predicate<Task> aPredicate)
    {
        return enqueuedTasks.stream().filter(aPredicate).findFirst() //
                .or(() -> executor.getQueue().stream().map(Task.class::cast).filter(aPredicate)
                        .findFirst())
                .or(() -> runningTasks.stream().filter(aPredicate).findFirst())
                .or(() -> pendingAcknowledgement.stream().filter(aPredicate).findFirst());
    }

    @Override
    public synchronized void stopAllTasksMatching(Predicate<Task> aPredicate)
    {
        enqueuedTasks.removeIf(task -> {
            if (aPredicate.test(task)) {
                task.destroy();
                return true;
            }
            return false;
        });

        executor.getQueue().removeIf(runnable -> {
            var task = (Task) runnable;
            if (aPredicate.test(task)) {
                task.destroy();
                return true;
            }
            return false;
        });

        runningTasks.forEach(task -> {
            if (aPredicate.test(task)) {
                task.getMonitor().cancel();
                // The task will be destroyed if necessary by the afterExecute callback
            }
        });

        pendingAcknowledgement.removeIf(runnable -> {
            var task = (Task) runnable;
            if (aPredicate.test(task)) {
                task.destroy();
                return true;
            }
            return false;
        });
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

    // Set order so this is handled before session info is removed from sessionRegistry
    @Order(Ordered.HIGHEST_PRECEDENCE)
    @EventListener
    public void onSessionDestroyed(SessionDestroyedEvent event)
    {
        LOG.debug("Cleaning up tasks on session destroyed");

        var sessionInfo = sessionRegistry.getSessionInformation(event.getId());

        // Could be an anonymous session without information.
        if (sessionInfo == null) {
            return;
        }

        var username = getUsernameFromPrincipal(sessionInfo.getPrincipal());
        if (username == null) {
            return;
        }

        var userHasOtherSession = isSessionOwnerLoggedInToOtherActiveSession(
                sessionInfo.getPrincipal());

        stopAllTasksMatching(t -> {
            if (!t.getUser().map(_user -> username.equals(_user.getUsername())).orElse(false)) {
                return false;
            }

            if (t.getScope().isRemoveWhenUserSessionEnds()) {
                LOG.debug("Stopping task {} because session has ended", t);
                return true;
            }

            if (t.getScope().isRemoveWhenLastUserSessionEnds() && !userHasOtherSession) {
                LOG.debug("Stopping task {} because last session of user [{}] has ended", t,
                        username);
                return true;
            }

            return false;
        });
    }

    private boolean isSessionOwnerLoggedInToOtherActiveSession(Object aPrincipal)
    {
        return !sessionRegistry.getAllSessions(aPrincipal, false).isEmpty();
    }

    private String getUsernameFromPrincipal(Object aPrincipal)
    {
        if (aPrincipal instanceof String username) {
            return username;
        }

        if (aPrincipal instanceof User user) {
            return user.getUsername();
        }

        return null;
    }

    @Override
    public void destroy()
    {
        LOG.info("Shutting down scheduling service!");
        watchdog.shutdownNow();
        executor.shutdownNow();

        enqueuedTasks.clear();
        executor.getQueue().clear();
        pendingAcknowledgement.clear();

        try {
            watchdog.awaitTermination(30, SECONDS);
        }
        catch (InterruptedException e) {
            // Ignore
        }

        try {
            executor.awaitTermination(30, SECONDS);
        }
        catch (InterruptedException e) {
            // Ignore
        }
    }

    private void logState()
    {
        getEnqueuedTasks().forEach(t -> LOG.debug("Queued      : {}", t));
        getScheduledTasks().forEach(t -> LOG.debug("Scheduled   : {}", t));
        getRunningTasks().forEach(t -> LOG.debug("Running     : {}", t));
        getTasksPendingAcknowledgment().forEach(t -> LOG.debug("Pending ack : {}", t));
    }

    @Override
    public void executeSync(Task aTask)
    {
        try {
            var factory = applicationContext.getAutowireCapableBeanFactory();
            factory.autowireBean(aTask);
            factory.initializeBean(aTask, "transientTask");

            LOG.debug("Starting task (sync): {} ", aTask);
            runningTasks.add(aTask);
            aTask.runSync();
        }
        finally {
            LOG.debug("Ended task (sync) [{}]: {}", aTask, aTask.getMonitor().getState());
            handleTaskEnded(aTask);
        }
    }
}

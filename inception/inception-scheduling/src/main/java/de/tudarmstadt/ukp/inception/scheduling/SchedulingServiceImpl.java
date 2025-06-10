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
import static java.lang.System.currentTimeMillis;
import static java.lang.System.identityHashCode;
import static java.util.concurrent.TimeUnit.SECONDS;

import java.io.IOException;
import java.lang.invoke.MethodHandles;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicInteger;
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
    private final Map<Project, AtomicInteger> suspended;

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
        suspended = Collections.synchronizedMap(new LinkedHashMap<>());
        watchdog = Executors.newScheduledThreadPool(1);
        watchdog.scheduleAtFixedRate(this::scheduleEligibleTasks, 5, 5, SECONDS);
        watchdog.scheduleAtFixedRate(this::cleanUpTasks, 10, 10, SECONDS);
    }

    private void beforeExecute(Thread aThread, Runnable aRunnable)
    {
        Validate.notNull(aRunnable, "Task cannot be null");
        synchronized (runningTasks) {
            if (!runningTasks.contains((Task) aRunnable)) {
                runningTasks.add((Task) aRunnable);
            }
            else {
                LOG.warn("Task running: {}", aRunnable);
            }
        }
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
        synchronized (runningTasks) {
            runningTasks.remove(aTask);
        }

        synchronized (pendingAcknowledgement) {
            // if (aTask.getMonitor().isCancelled() || !aTask.getScope().isDestroyOnEnd()) {
            pendingAcknowledgement.add(aTask);
            // }
            // else {
            // aTask.destroy();
            // }
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
            case QUEUE_THIS:
                // Queue this task to be run potentially after other matching tasks have been
                // completed
                break;
            case NO_MATCH:
                // Ignore
                break;
            }
        }

        for (var taskToUnqueue : tasksToUnqueue) {
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

        if (aTask.getProject() != null && suspended.containsKey(aTask.getProject())) {
            LOG.debug("Tasks for project suspended - adding to queue: [{}]", aTask);
            enqueuedTasks.add(aTask);
            return;
        }

        schedule(aTask);

        logState();
    }

    @Override
    public void suspendTasks(Project aProject) throws TimeoutException
    {
        synchronized (suspended) {
            var suspendCount = suspended.computeIfAbsent(aProject, v -> new AtomicInteger(0));
            suspendCount.incrementAndGet();
            LOG.debug("Suspending tasks for {} [{}] [{}]", aProject, identityHashCode(aProject),
                    suspendCount);
        }

        waitForProjectTasksToEnd(aProject, Duration.ofMinutes(1), true);
    }

    private void waitForProjectTasksToEnd(Project aProject, Duration aTimeout,
            boolean aResumeOnTimeout)
        throws TimeoutException
    {
        var startTime = currentTimeMillis();
        var timeoutMillis = aTimeout.toMillis();

        while (runningTasks.stream()
                .anyMatch(t -> aProject.getId() != null ? aProject.equals(t.getProject())
                        : aProject == t.getProject())) {
            // LOG.trace("Waiting for running tasks to end on project {}", aProject);

            if (currentTimeMillis() - startTime > timeoutMillis) {
                var msg = new StringBuilder();
                msg.append("Waiting for tasks related to project ");
                msg.append(aProject);
                msg.append(" to finish took longer than ");
                msg.append(aTimeout);
                msg.append("\n");
                msg.append("The following tasks are still running:\n");
                runningTasks.stream() //
                        .filter(t -> aProject.equals(t.getProject())) //
                        .forEach(t -> {
                            msg.append("- ").append(t).append("\n");
                            for (var frame : t.getThread().getStackTrace()) {
                                msg.append("  ");
                                msg.append(frame);
                                msg.append("\n");
                            }
                        });

                if (aResumeOnTimeout) {
                    resumeTasks(aProject);
                }

                throw new TimeoutException(msg.toString());
            }

            try {
                Thread.sleep(50);
            }
            catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                LOG.error("Thread was interrupted while waiting for tasks to end on project {}",
                        aProject, e);
                throw new IllegalStateException(
                        "Thread was interrupted while waiting for tasks to end on project ["
                                + aProject.getName() + "]");
            }
        }
    }

    @Override
    public void resumeTasks(Project aProject)
    {
        synchronized (suspended) {
            var suspendCount = suspended.get(aProject);
            if (suspendCount != null) {
                LOG.debug("Resuming tasks for {} [{}] [{}]", aProject, identityHashCode(aProject),
                        suspendCount);
                if (suspendCount.decrementAndGet() == 0) {
                    suspended.remove(aProject);
                    scheduleEligibleTasks();
                }
            }
        }
    }

    @Override
    public SuspensionContext whileSuspended(Project aProject) throws TimeoutException
    {
        suspendTasks(aProject);

        return new SuspensionContext()
        {
            @Override
            public void close()
            {
                resumeTasks(aProject);
            }
        };
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
        pendingAcknowledgement.removeIf(runnable -> {
            var task = (Task) runnable;

            if (task.getScope().isDestroyOnEnd()
                    && currentTimeMillis() - task.getMonitor().getEndTime() > 5000) {
                LOG.debug("Destroying self-destrucing task after quiet period: {}", task);
                task.destroy();
                return true;
            }

            return false;
        });

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
        var i = enqueuedTasks.iterator();

        while (i.hasNext()) {
            var t = i.next();
            if (!t.isReadyToStart()) {
                continue;
            }

            if (getScheduledAndRunningTasks().contains(t)) {
                continue;
            }

            if (t.getProject() != null && suspended.containsKey(t.getProject())) {
                continue;
            }

            i.remove();
            schedule(t);
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
    public synchronized int stopAllTasksMatching(Predicate<Task> aPredicate)
    {
        AtomicInteger count = new AtomicInteger();

        enqueuedTasks.removeIf(task -> {
            if (aPredicate.test(task)) {
                LOG.debug("Destroying queued task: {}", task);
                task.destroy();
                count.incrementAndGet();
                return true;
            }
            return false;
        });

        executor.getQueue().removeIf(runnable -> {
            var task = (Task) runnable;
            if (aPredicate.test(task)) {
                LOG.debug("Destroying scheduled task: {}", task);
                task.destroy();
                count.incrementAndGet();
                return true;
            }
            return false;
        });

        runningTasks.forEach(task -> {
            if (aPredicate.test(task)) {
                LOG.debug("Canceling running task: {}", task);
                task.getMonitor().cancel();
                count.incrementAndGet();
                // The task will be destroyed if necessary by the afterExecute callback
            }
        });

        pendingAcknowledgement.removeIf(runnable -> {
            var task = (Task) runnable;
            if (aPredicate.test(task)) {
                LOG.debug("Destroying ack-pending task: {}", task);
                task.destroy();
                count.incrementAndGet();
                return true;
            }
            return false;
        });

        return count.get();
    }

    @Order(Ordered.HIGHEST_PRECEDENCE)
    @EventListener
    public void onBeforeProjectRemoved(BeforeProjectRemovedEvent aEvent) throws TimeoutException
    {
        deletionPending.add(aEvent.getProject());
        stopAllTasksForProject(aEvent.getProject());
        var timeout = Duration.ofMinutes(1);
        try {
            waitForProjectTasksToEnd(aEvent.getProject(), timeout, false);
        }
        catch (TimeoutException e) {
            LOG.warn(
                    "Running tasks for project {} did not terminate after {} - trying to interrupt them",
                    aEvent.getProject(), timeout);
            // Try interrupting running threads
            runningTasks.forEach(task -> {
                try {
                    LOG.warn("Interrupting: {}", task);
                    task.getThread().interrupt();
                }
                catch (Throwable t) {
                    LOG.error("Error while interrupting hanging task {}", t, e);
                }
            });
            waitForProjectTasksToEnd(aEvent.getProject(), timeout, false);
        }
    }

    @Order(Ordered.HIGHEST_PRECEDENCE)
    @EventListener
    public void onAfterProjectRemoved(AfterProjectRemovedEvent aEvent) throws IOException
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
        if (LOG.isDebugEnabled()) {
            var lines = new ArrayList<String>();
            getEnqueuedTasks().forEach(t -> lines.add("  Queued      : " + t));
            getScheduledTasks().forEach(t -> lines.add("  Scheduled   : " + t));
            getRunningTasks().forEach(t -> lines.add("  Running     : " + t));
            suspended.forEach((p, c) -> lines.add("  Suspended   : " + p + " [" + c + "]"));
            deletionPending.forEach(p -> lines.add("  Deleting    : " + p));
            getTasksPendingAcknowledgment().forEach(t -> lines.add("  Pending ack : " + t));
            if (!lines.isEmpty()) {
                LOG.debug("Scheduler state:");
                lines.forEach(LOG::debug);
            }
        }
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

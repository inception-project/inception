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

import static java.util.concurrent.TimeUnit.SECONDS;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ThreadPoolExecutor;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.AutowireCapableBeanFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;

import de.tudarmstadt.ukp.inception.scheduling.config.SchedulingProperties;

@Component
public class SchedulingService
    implements DisposableBean
{
    private static final Logger log = LoggerFactory.getLogger(SchedulingService.class);

    private final ApplicationContext applicationContext;
    private final ThreadPoolExecutor executor;
    private final ScheduledExecutorService watchdog;

    private final List<Task> runningTasks;
    private final List<Task> enqueuedTasks;

    @Autowired
    public SchedulingService(ApplicationContext aApplicationContext, SchedulingProperties aConfig)
    {
        applicationContext = aApplicationContext;
        executor = new InspectableThreadPoolExecutor(aConfig.getNumberOfThreads(),
                aConfig.getQueueSize(), this::beforeExecute, this::afterExecute);
        runningTasks = Collections.synchronizedList(new ArrayList<>());
        enqueuedTasks = Collections.synchronizedList(new ArrayList<>());
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
    public List<Task> getEnqueuedTasks()
    {
        // We return copy here, as else the list the receiver sees might be updated
        // when new tasks are running or existing ones stopped.
        return new ArrayList<>(enqueuedTasks);
    }

    /**
     * @return tasks which have been handed to the executor but have not yet been started.
     */
    public List<Task> getScheduledTasks()
    {
        List<Task> result = new ArrayList<>();
        executor.getQueue().forEach(r -> result.add((Task) r));
        return result;
    }

    /**
     * @return tasks which have been already been started.
     */
    public List<Task> getRunningTasks()
    {
        // We return copy here, as else the list the receiver sees might be updated
        // when new tasks are running or existing ones stopped.
        return new ArrayList<>(runningTasks);
    }

    public List<Task> getScheduledAndRunningTasks()
    {
        List<Task> result = new ArrayList<>();
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
    public synchronized void enqueue(Task aTask)
    {
        if (enqueuedTasks.contains(aTask)) {
            Task previousTask = enqueuedTasks.set(enqueuedTasks.indexOf(aTask), aTask);
            log.debug("Equivalent task already queued - updated queue: [{}] replaced with [{}]",
                    previousTask, aTask);
            return;
        }

        if (getScheduledTasks().contains(aTask)) {
            log.debug("Equivalent task already scheduled - adding to queue: [{}]", aTask);
            enqueuedTasks.add(aTask);
            return;
        }

        if (getRunningTasks().contains(aTask)) {
            log.debug("Equivalent task already running - adding to queue: [{}]", aTask);
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
    public synchronized void stopAllTasksForUser(String aUserName)
    {
        enqueuedTasks.removeIf(e -> {
            return ((Task) e).getUser().getUsername().equals(aUserName);
        });

        executor.getQueue().removeIf(e -> {
            return ((Task) e).getUser().getUsername().equals(aUserName);
        });

        // TODO: Stop the running tasks as well
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
}

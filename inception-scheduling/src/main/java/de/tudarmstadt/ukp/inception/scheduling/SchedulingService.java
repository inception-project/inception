/*
 * Copyright 2019
 * Ubiquitous Knowledge Processing (UKP) Lab
 * Technische Universität Darmstadt
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package de.tudarmstadt.ukp.inception.scheduling;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ThreadPoolExecutor;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.AutowireCapableBeanFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;

@Component
public class SchedulingService
        implements DisposableBean
{
    private static final Logger log = LoggerFactory.getLogger(SchedulingService.class);

    private static final int NUMBER_OF_THREADS = 4;
    private static final int QUEUE_SIZE = 100;

    private final ApplicationContext applicationContext;
    private final ThreadPoolExecutor executor;

    private final List<Task> runningTasks;

    @Autowired
    public SchedulingService(ApplicationContext aApplicationContext)
    {
        applicationContext = aApplicationContext;
        executor = new InspectableThreadPoolExecutor(NUMBER_OF_THREADS, QUEUE_SIZE,
                this::beforeExecute, this::afterExecute);
        runningTasks = Collections.synchronizedList(new ArrayList<>());
    }

    private void beforeExecute(Thread aThread, Runnable aRunnable)
    {
        runningTasks.add((Task) aRunnable);
    }

    private void afterExecute(Runnable aRunnable, Throwable aThrowable)
    {
        runningTasks.remove(aRunnable);
    }

    public List<Task> getScheduledTasks()
    {
        List<Task> result = new ArrayList<>();
        executor.getQueue().forEach(r -> result.add((Task) r));
        return result;
    }

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

    public void enqueue(Task aTask)
    {
        // This autowires the task fields manually.
        AutowireCapableBeanFactory factory = applicationContext.getAutowireCapableBeanFactory();
        factory.autowireBean(aTask);
        factory.initializeBean(aTask, "transientTask");

        executor.execute(aTask);
    }

    /**
     * Removes all task for the user with name {@code aUsername} from the scheduler's queue.
     * @param aUserName The name of the user whose tasks will be removed.
     */
    public synchronized void stopAllTasksForUser(String aUserName)
    {
        // TODO: Stop the running tasks also
        executor.getQueue().removeIf(e -> {
            Task task = (Task) e;
            return task.getUser().getUsername().equals(aUserName);
        });
    }

    @Override
    public void destroy()
    {
        log.info("Shutting down scheduling service!");
        executor.shutdownNow();
    }


}

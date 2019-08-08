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

import org.apache.wicket.Application;
import org.apache.wicket.protocol.ws.WebSocketSettings;
import org.apache.wicket.protocol.ws.api.IWebSocketConnection;
import org.apache.wicket.protocol.ws.api.registry.IWebSocketConnectionRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.AutowireCapableBeanFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.event.EventListener;
import org.springframework.security.core.session.SessionInformation;
import org.springframework.security.core.session.SessionRegistry;
import org.springframework.stereotype.Component;

import de.tudarmstadt.ukp.inception.scheduling.config.SchedulingProperties;

@Component
public class SchedulingService
        implements DisposableBean
{
    private static final Logger log = LoggerFactory.getLogger(SchedulingService.class);

    private final ApplicationContext applicationContext;
    private final ThreadPoolExecutor executor;
    private @Autowired SessionRegistry sessionRegistry;
    private @Autowired Application application;
    private @Autowired ApplicationEventPublisher appEventPublisher;

    private final List<Task> runningTasks;

    @Autowired
    public SchedulingService(ApplicationContext aApplicationContext, SchedulingProperties aConfig)
    {
        applicationContext = aApplicationContext;
        executor = new InspectableThreadPoolExecutor(
                aConfig.getNumberOfThreads(), aConfig.getQueueSize(),
                this::beforeExecute, this::afterExecute);
        runningTasks = Collections.synchronizedList(new ArrayList<>());
    }

    private void beforeExecute(Thread aThread, Runnable aRunnable)
    {
        Task task = (Task) aRunnable;
        runningTasks.add(task);
        appEventPublisher.publishEvent(
                new TaskUpdateEvent(task, task.getUser().getUsername(), TaskState.RUNNING, 0.0));
    }

    private void afterExecute(Runnable aRunnable, Throwable aThrowable)
    {
        Task task = (Task) aRunnable;
        runningTasks.remove(task);
        appEventPublisher.publishEvent(
                new TaskUpdateEvent(task, task.getUser().getUsername(), TaskState.DONE, 1.0));
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

    public synchronized void enqueue(Task aTask)
    {
        if (getScheduledTasks().contains(aTask)) {
            log.debug("Task already in queue: {}", aTask);
            return;
        }

        log.debug("Enqueuing task [{}]", aTask);
        appEventPublisher.publishEvent(new TaskUpdateEvent(aTask, aTask.getUser().getUsername(),
                TaskState.SCHEDULED, 0.0));
        
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

    @EventListener
    public void distributeWebSocketMessage(TaskUpdateEvent aTaskUpdateEvent)
    {
        log.info(String.format("Distributing event: %s", aTaskUpdateEvent.toString()));

        if (application == null) {
            log.error("Wicket Application is null");
            return;
        }

        WebSocketSettings webSocketSettings = WebSocketSettings.Holder.get(application);
        IWebSocketConnectionRegistry webSocketConnectionRegistry = webSocketSettings
                .getConnectionRegistry();

        // get all connections for the user
        List<IWebSocketConnection> userConnections = new ArrayList<>();

        // log.info(String.format("Found %d sessions for user %s", sessionRegistry
        // .getAllSessions(taskUpdate.getUser(), false).size(),
        // taskUpdate.getUser()));

        List<String> ids = new ArrayList<>();
        for (SessionInformation sessionInfo : sessionRegistry
                .getAllSessions(aTaskUpdateEvent.getUser(), false)) {
            // log.info(String.format("SessionId connections for user %s is %s",
            // taskUpdate.getUser(),
            // sessionInfo.getSessionId()));

            userConnections.addAll(webSocketConnectionRegistry.getConnections(application,
                    sessionInfo.getSessionId()));
            ids.add(sessionInfo.getSessionId());
        }

//        log.info(String.format("Found %d connections for user %s with ids %s \n",
//                userConnections.size(), aTaskUpdateEvent.getUser(),
//                ids.stream().collect(Collectors.joining(","))));

        // send message to all connections
        for (IWebSocketConnection connection : userConnections) {
            connection.sendMessage(aTaskUpdateEvent);
            log.info(String.format("Send event: %s", aTaskUpdateEvent.toString()));
        }

    }
}

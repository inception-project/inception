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

import java.util.List;
import java.util.Optional;
import java.util.concurrent.TimeoutException;
import java.util.function.Predicate;

import de.tudarmstadt.ukp.clarin.webanno.model.Project;

public interface SchedulingService
{
    /**
     * @return tasks which have not been handed to the executor yet.
     */
    List<Task> getEnqueuedTasks();

    /**
     * @return tasks which have been handed to the executor but have not yet been started.
     */
    List<Task> getScheduledTasks();

    /**
     * @return tasks which have been already been started.
     */
    List<Task> getRunningTasks();

    /**
     * @return tasks which are no longer running (completed, failed).
     */
    List<Task> getTasksPendingAcknowledgment();

    List<Task> getScheduledAndRunningTasks();

    List<Task> getAllTasks();

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
    void enqueue(Task aTask);

    Optional<Task> findTask(Predicate<Task> aPredicate);

    /**
     * Removes all task for the user with name {@code aUsername} from the scheduler's queue.
     * 
     * @param aUserName
     *            The name of the user whose tasks will be removed.
     */
    void stopAllTasksForUser(String aUserName);

    int stopAllTasksMatching(Predicate<Task> aPredicate);

    /**
     * Removes all task for the given project from the scheduler's queue.
     * 
     * @param aProject
     *            The project whose tasks will be removed.
     */
    void stopAllTasksForProject(Project aProject);

    /**
     * Execute a task immediately and synchronously (blocking) in the current thread.
     * 
     * @param aTask
     *            the task to be executed.
     */
    void executeSync(Task aTask);

    void suspendTasks(Project aProject) throws TimeoutException;

    void resumeTasks(Project aProject);

    SuspensionContext whileSuspended(Project aProject) throws TimeoutException;

    interface SuspensionContext
        extends AutoCloseable
    {
        @Override
        void close();
    }
}

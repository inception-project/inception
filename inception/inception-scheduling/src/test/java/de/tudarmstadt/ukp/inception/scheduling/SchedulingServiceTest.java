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

import static java.time.Duration.ofSeconds;
import static java.util.Arrays.asList;
import static java.util.concurrent.TimeUnit.SECONDS;
import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.List;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.config.AutowireCapableBeanFactory;
import org.springframework.context.ApplicationContext;

import de.tudarmstadt.ukp.clarin.webanno.model.Project;
import de.tudarmstadt.ukp.clarin.webanno.security.model.User;
import de.tudarmstadt.ukp.inception.scheduling.config.SchedulingProperties;

@ExtendWith(MockitoExtension.class)
public class SchedulingServiceTest
{
    private @Mock ApplicationContext mockContext;

    private SchedulingServiceImpl sut;

    @BeforeEach
    public void setUp()
    {
        when(mockContext.getAutowireCapableBeanFactory())
                .thenReturn(mock(AutowireCapableBeanFactory.class));

        sut = new SchedulingServiceImpl(mockContext, new SchedulingProperties(), null);
    }

    @AfterEach
    public void tearDown()
    {
        sut.destroy();
    }

    @Test
    public void thatRunningTasksCanBeRetrieved()
    {
        List<Task> tasks = asList( //
                buildDummyTask("user1", "project1"), //
                buildDummyTask("user1", "project2"), //
                buildDummyTask("user2", "project1"));

        for (Task task : tasks) {
            sut.enqueue(task);
        }

        // Wait until the threads have actually been started
        await().atMost(15, SECONDS).until(() -> sut.getRunningTasks().size() == tasks.size());

        assertThat(sut.getRunningTasks()) //
                .as("All enqueued tasks should be running")
                .containsExactlyInAnyOrderElementsOf(tasks);
    }

    @Test
    public void thatTasksForUserCanBeStopped()
    {
        List<Task> tasks = asList( //
                buildDummyTask("testUser", "project1"), //
                buildDummyTask("unimportantUser1", "project1"), //
                buildDummyTask("unimportantUser2", "project2"), //
                buildDummyTask("unimportantUser3", "project3"), //
                buildDummyTask("testUser", "project2"), //
                buildDummyTask("unimportantUser4", "project4"), //
                buildDummyTask("testUser", "project3"), //
                buildDummyTask("unimportantUser1", "project2"), //
                buildDummyTask("testUser", "project4"), //
                buildDummyTask("unimportantUser2", "project3"), //
                buildDummyTask("unimportantUser3", "project4"), //
                buildDummyTask("testUser", "project2"), //
                buildDummyTask("testUser", "project2"));
        Task[] tasksToRemove = tasks.stream()
                .filter(t -> t.getUser().get().getUsername().equals("testUser"))
                .toArray(Task[]::new);

        for (Task task : tasks) {
            sut.enqueue(task);
        }

        sut.stopAllTasksForUser("testUser");

        assertThat(sut.getScheduledTasks()).as("Tasks for 'testUser' should have been removed'")
                .doesNotContain(tasksToRemove);
    }

    private User buildUser(String aUsername)
    {
        return new User(aUsername);
    }

    private Project buildProject(String aProjectName)
    {
        Project project = new Project();
        project.setSlug(aProjectName);
        project.setName(aProjectName);
        return project;
    }

    private Task buildDummyTask(String aUsername, String aProjectName)
    {
        var task = DummyTask.builder().withSessionOwner(buildUser(aUsername))
                .withProject(buildProject(aProjectName)).build();
        task.afterPropertiesSet();
        return task;
    }

    /**
     * DummyTask is a task that does nothing and just sleeps until interrupted. if interrupted, it
     * just finishes running and returns.
     */
    private static class DummyTask
        extends Task
    {
        private static final String TYPE = "DummyTask";

        DummyTask(Builder<? extends Builder<?>> aBuilder)
        {
            super(aBuilder.withType(TYPE).withTrigger("test"));
        }

        @Override
        public void execute()
        {
            while (!Thread.currentThread().isInterrupted()) {
                try {
                    Thread.sleep(1000);
                }
                catch (InterruptedException e) {
                    break;
                }
            }
        }

        public static Builder<Builder<?>> builder()
        {
            return new Builder<>();
        }

        public static class Builder<T extends Builder<?>>
            extends DebouncingTask.Builder<T>
        {
            protected Builder()
            {
                withDebounceDelay(ofSeconds(3));
            }

            public DummyTask build()
            {
                return new DummyTask(this);
            }
        }
    }
}

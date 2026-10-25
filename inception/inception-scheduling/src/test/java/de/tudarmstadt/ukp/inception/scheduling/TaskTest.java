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

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

import de.tudarmstadt.ukp.clarin.webanno.model.Project;
import de.tudarmstadt.ukp.clarin.webanno.security.model.User;

public class TaskTest
{
    private final Project project = Project.builder().withName("project").build();

    @Test
    void thatTaskWithSessionOwnerIsNotSystemTask()
    {
        var owner = new User("user");

        var task = DummyTask.builder() //
                .withSessionOwner(owner) //
                .withProject(project) //
                .build();

        assertThat(task.isSystemTask()).isFalse();
        assertThat(task.getSessionOwner()).contains(owner);
    }

    @Test
    void thatSystemTaskHasNoSessionOwner()
    {
        var task = DummyTask.builder() //
                .asSystemTask() //
                .withProject(project) //
                .build();

        assertThat(task.isSystemTask()).isTrue();
        assertThat(task.getSessionOwner()).isEmpty();
    }

    @Test
    void thatTaskWithoutSessionOwnerAndSystemTaskMarkerIsRejected()
    {
        assertThatThrownBy(() -> DummyTask.builder() //
                .withProject(project) //
                .build()) //
                        .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void thatTaskWithNullSessionOwnerIsRejected()
    {
        assertThatThrownBy(() -> DummyTask.builder() //
                .withSessionOwner(null) //
                .withProject(project) //
                .build()) //
                        .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void thatSystemTaskWithSessionOwnerIsRejected()
    {
        assertThatThrownBy(() -> DummyTask.builder() //
                .withSessionOwner(new User("user")) //
                .asSystemTask() //
                .withProject(project) //
                .build()) //
                        .isInstanceOf(IllegalArgumentException.class);
    }

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
            // no-op
        }

        public static Builder<Builder<?>> builder()
        {
            return new Builder<>();
        }

        public static class Builder<T extends Builder<?>>
            extends Task.Builder<T>
        {
            public DummyTask build()
            {
                return new DummyTask(this);
            }
        }
    }
}

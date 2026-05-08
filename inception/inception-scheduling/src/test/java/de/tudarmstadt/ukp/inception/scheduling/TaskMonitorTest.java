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

import org.junit.jupiter.api.Test;

import de.tudarmstadt.ukp.clarin.webanno.model.Project;
import de.tudarmstadt.ukp.inception.support.logging.LogMessage;

public class TaskMonitorTest
{
    private Project buildProject(String aProjectName)
    {
        Project project = new Project();
        project.setSlug(aProjectName);
        project.setName(aProjectName);
        return project;
    }

    @Test
    public void duplicateMessagesAreConflated()
    {
        var task = DummyTask.builder().withProject(buildProject("dummy-project")).build();
        task.afterPropertiesSet();
        var monitor = task.getMonitor();

        var m1 = LogMessage.info("source", "Hello");
        var m2 = LogMessage.info("source", "Goodbye");

        // Add the same message multiple times
        monitor.update(up -> up.addMessage(m1));
        monitor.update(up -> up.addMessage(m1));
        monitor.update(up -> up.addMessage(m1));

        // Add a different message to force insertion of the repeat-summary message
        monitor.update(up -> up.addMessage(m2));

        var messages = monitor.getMessages();

        assertThat(messages).hasSize(3);

        var it = messages.iterator();
        assertThat(it.next()).isEqualTo(m1);
        assertThat(it.next().getMessage()).isEqualTo("... repeated 2 times");
        assertThat(it.next()).isEqualTo(m2);
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

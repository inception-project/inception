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
package de.tudarmstadt.ukp.inception.assistant;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import de.tudarmstadt.ukp.inception.assistant.model.MMessage;
import de.tudarmstadt.ukp.inception.assistant.model.MTextMessage;

class MemoryTest
{
    private Memory sut;

    @BeforeEach
    void setUp()
    {
        sut = new Memory();
    }

    @Test
    void ephemeralIsSkippedWhenNotInDebugMode()
    {
        var msg = MTextMessage.builder() //
                .withContent("ephemeral") //
                .withRole("assistant") //
                .withActor("bot") //
                .ephemeral() //
                .build();

        sut.setDebugMode(false);
        sut.recordMessage(msg);

        assertThat(sut.getInternalChatHistory()).isEmpty();
    }

    @Test
    void ephemeralIsRecordedWhenInDebugMode()
    {
        var msg = MTextMessage.builder() //
                .withContent("ephemeral") //
                .withRole("assistant") //
                .withActor("bot") //
                .ephemeral() //
                .build();

        sut.setDebugMode(true);
        sut.recordMessage(msg);

        List<MMessage> messages = sut.getMessages();
        assertThat(messages).hasSize(1);
        assertThat(messages.get(0)).isInstanceOf(MTextMessage.class);
    }

    @Test
    void appendsFragmentsWhenBothNotDone()
    {
        var id = UUID.randomUUID();

        var part1 = MTextMessage.builder() //
                .withId(id) //
                .withRole("user") //
                .withActor("me") //
                .withContent("Hello") //
                .notDone() //
                .build();

        var part2 = MTextMessage.builder() //
                .withId(id) //
                .withRole("user") //
                .withActor("me") //
                .withContent(" World") //
                .notDone() //
                .build();

        sut.recordMessage(part1);
        sut.recordMessage(part2);

        List<MMessage> messages = sut.getMessages();
        assertThat(messages).hasSize(1);
        var stored = (MTextMessage) messages.get(0);
        assertThat(stored.textRepresentation()).isEqualTo("Hello World");
        assertThat(stored.done()).isFalse();
    }

    @Test
    void doneFragmentReplacesExistingFragments()
    {
        var id = UUID.randomUUID();

        var part1 = MTextMessage.builder() //
                .withId(id) //
                .withRole("user") //
                .withActor("me") //
                .withContent("Hello") //
                .notDone() //
                .build();

        var finalPart = MTextMessage.builder() //
                .withId(id) //
                .withRole("user") //
                .withActor("me") //
                .withContent("Final") //
                .done() //
                .build();

        sut.recordMessage(part1);
        sut.recordMessage(finalPart);

        var messages = sut.getMessages();
        assertThat(messages).hasSize(1);
        var stored = (MTextMessage) messages.get(0);
        assertThat(stored.textRepresentation()).isEqualTo("Final");
        assertThat(stored.done()).isTrue();
    }

    @Test
    void messageWithContextIsInsertedBeforeContext()
    {
        var base = MTextMessage.builder() //
                .withRole("assistant") //
                .withActor("bot") //
                .withContent("A") //
                .build();

        sut.recordMessage(base);

        var withContext = MTextMessage.builder() //
                .withRole("assistant") //
                .withActor("bot") //
                .withContent("B") //
                .withContext(base.id()) //
                .build();

        sut.recordMessage(withContext);

        assertThat(sut.getMessages()).containsExactly(withContext, base);
    }
}

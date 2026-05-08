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

import static java.util.Collections.unmodifiableList;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Objects;

import de.tudarmstadt.ukp.inception.assistant.model.MChatMessage;
import de.tudarmstadt.ukp.inception.assistant.model.MMessage;
import de.tudarmstadt.ukp.inception.assistant.model.MTextMessage;

/**
 * Maintains the message history for the assistant.
 */
class Memory
{
    private boolean debugMode;
    private List<MMessage> messages = new LinkedList<>();

    public List<MMessage> getMessages()
    {
        return unmodifiableList(new ArrayList<>(messages));
    }

    public void clearMessages()
    {
        synchronized (messages) {
            messages.clear();
        }
    }

    public synchronized void setDebugMode(boolean aOnOff)
    {
        debugMode = aOnOff;
    }

    public synchronized boolean isDebugMode()
    {
        return debugMode;
    }

    public void recordMessage(MMessage aMessage)
    {
        synchronized (messages) {
            // If a message with the same ID already exists, update it
            if (aMessage instanceof MTextMessage textMsg) {
                var i = messages.listIterator(messages.size());

                while (i.hasPrevious()) {
                    var m = i.previous();
                    if (m instanceof MTextMessage existingTextMsg) {
                        if (Objects.equals(existingTextMsg.id(), textMsg.id())) {
                            if (textMsg.done()) {
                                i.set(textMsg);
                            }
                            else {
                                i.set(existingTextMsg.append(textMsg));
                            }
                            return;
                        }
                    }
                }
            }

            // If the message has a context message, we need to insert it before that context
            // message
            if (aMessage instanceof MChatMessage chatMessage && chatMessage.context() != null) {
                var i = 0;
                for (var m : messages) {
                    if (m instanceof MChatMessage otherChatMessage
                            && otherChatMessage.id().equals(chatMessage.context())) {
                        messages.add(i, aMessage);
                        return;
                    }
                    i++;
                }
            }

            // Otherwise add it to the end
            messages.add(aMessage);
        }
    }

    public List<MChatMessage> getUserChatHistory()
    {
        return getMessages().stream() //
                .filter(MChatMessage.class::isInstance) //
                .map(MChatMessage.class::cast) //
                .filter(msg -> isDebugMode() || (!msg.internal() && !msg.ephemeral())) //
                .toList();
    }

    public List<MChatMessage> getInternalChatHistory()
    {
        return getMessages().stream() //
                .filter(MChatMessage.class::isInstance) //
                .map(MChatMessage.class::cast) //
                .filter(msg -> !msg.ephemeral()) //
                .filter(msg -> msg.done()) //
                .toList();
    }
}

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

import java.util.Map.Entry;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import org.apache.commons.lang3.Validate;

import de.tudarmstadt.ukp.clarin.webanno.model.Project;
import de.tudarmstadt.ukp.inception.assistant.AssistantServiceImpl.AssistantStateKey;
import de.tudarmstadt.ukp.inception.assistant.model.MChatMessage;

public class MemoryManager
{
    private final ConcurrentMap<AssistantStateKey, Memory> memories;

    public MemoryManager()
    {
        memories = new ConcurrentHashMap<>();
    }

    Memory getMemory(String aSessionOwner, Project aProject)
    {
        synchronized (memories) {
            return memories.computeIfAbsent(new AssistantStateKey(aSessionOwner, aProject.getId()),
                    (v) -> new Memory());
        }
    }

    void clearMemories(Project aProject)
    {
        Validate.notNull(aProject, "Project must be specified");

        synchronized (memories) {
            memories.keySet().removeIf(key -> Objects.equals(aProject.getId(), key.projectId()));
        }
    }

    void clearMemories(String aSessionOwner)
    {
        Validate.notNull(aSessionOwner, "Username must be specified");

        synchronized (memories) {
            memories.keySet().removeIf(key -> aSessionOwner.equals(key.user()));
        }
    }

    void clearMemories(String aSessionOwner, Project aProject)
    {
        synchronized (memories) {
            memories.entrySet().stream() //
                    .filter(e -> aSessionOwner.equals(e.getKey().user())
                            && Objects.equals(aProject.getId(), e.getKey().projectId())) //
                    .map(Entry::getValue) //
                    .forEach(state -> state.clearMessages());
        }
    }

    void setDebugMode(String aSessionOwner, Project aProject, boolean aOnOff)
    {
        synchronized (memories) {
            getMemory(aSessionOwner, aProject).setDebugMode(aOnOff);
        }
    }

    boolean isDebugMode(String aSessionOwner, Project aProject)
    {
        synchronized (memories) {
            return getMemory(aSessionOwner, aProject).isDebugMode();
        }
    }

    void recordMessage(String aSessionOwner, Project aProject, MChatMessage aMessage)
    {
        var memory = getMemory(aSessionOwner, aProject);
        memory.recordMessage(aMessage);
    }
}

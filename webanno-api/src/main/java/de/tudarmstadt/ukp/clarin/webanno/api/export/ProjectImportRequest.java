/*
 * Copyright 2018
 * Ubiquitous Knowledge Processing (UKP) Lab and FG Language Technology
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
package de.tudarmstadt.ukp.clarin.webanno.api.export;

import java.io.Serializable;
import java.util.Optional;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

import de.tudarmstadt.ukp.clarin.webanno.security.model.User;

public class ProjectImportRequest
    implements Serializable
{
    private static final long serialVersionUID = -4486934192675904995L;

    public static final String FORMAT_AUTO = "AUTO";

    public int progress = 0;

    private final Queue<String> messages = new ConcurrentLinkedQueue<>();
    
    private final boolean createMissingUsers;
    private final boolean importPermissions;
    private final Optional<User> manager;

    /**
     * Request the import of a project, optionally creating any users referenced in the project
     * but missing in the current instance.
     */
    public ProjectImportRequest(boolean aCreateMissingUsers)
    {
        createMissingUsers = aCreateMissingUsers;
        importPermissions = true;
        manager = Optional.empty();
    }

    public ProjectImportRequest(boolean aCreateMissingUsers, boolean aImportPermissions)
    {
        createMissingUsers = aCreateMissingUsers;
        importPermissions = aImportPermissions;
        manager = Optional.empty();
    }

    public ProjectImportRequest(boolean aCreateMissingUsers, boolean aImportPermissions,
            Optional<User> aManager)
    {
        createMissingUsers = aCreateMissingUsers;
        importPermissions = aImportPermissions;
        manager = aManager;
    }

    public void addMessage(String aMessage)
    {
        // Avoid repeating the same message over for different users
        if (!messages.contains(aMessage)) {
            messages.add(aMessage);
        }
    }

    public Queue<String> getMessages()
    {
        return messages;
    }

    public boolean isCreateMissingUsers()
    {
        return createMissingUsers;
    }
    
    public boolean isImportPermissions()
    {
        return importPermissions;
    }
    
    public Optional<User> getManager()
    {
        return manager;
    }
}

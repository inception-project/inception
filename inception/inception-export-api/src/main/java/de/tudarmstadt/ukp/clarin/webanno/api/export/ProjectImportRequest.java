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

    private final Queue<String> messages = new ConcurrentLinkedQueue<>();

    private final boolean createMissingUsers;
    private final boolean importPermissions;
    private final User manager;

    private ProjectImportRequest(Builder builder)
    {
        createMissingUsers = builder.createMissingUsers;
        importPermissions = builder.importPermissions;
        manager = builder.manager;
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
        return Optional.ofNullable(manager);
    }

    public static Builder builder()
    {
        return new Builder();
    }

    public static final class Builder
    {
        private boolean createMissingUsers;
        private boolean importPermissions;
        private User manager;

        private Builder()
        {
        }

        public Builder withCreateMissingUsers(boolean aCreateMissingUsers)
        {
            createMissingUsers = aCreateMissingUsers;
            return this;
        }

        public Builder withImportPermissions(boolean aImportPermissions)
        {
            importPermissions = aImportPermissions;
            return this;
        }

        public Builder withManager(User aManager)
        {
            manager = aManager;
            return this;
        }

        public ProjectImportRequest build()
        {
            return new ProjectImportRequest(this);
        }
    }
}

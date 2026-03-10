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
package de.tudarmstadt.ukp.inception.curation.api;

import java.util.Collection;
import java.util.List;

import de.tudarmstadt.ukp.clarin.webanno.model.Project;
import de.tudarmstadt.ukp.clarin.webanno.model.SourceDocument;
import de.tudarmstadt.ukp.clarin.webanno.security.model.User;

public interface CurationSessionService
{
    List<User> getSelectedUsers(String aCurrentUser, long aProjectId);

    void setSelectedUsers(String aCurrentUser, long aProjectId, Collection<User> aUsers);

    @Deprecated
    boolean existsSession(String aSessionOwner, long aProjectId);

    @Deprecated
    void startSession(String aSessionOwner, Project aProject, boolean aOwnDocument);

    @Deprecated
    void closeSession(String aCurrentUser, long aProjectId);

    String getCurationTarget(String aUser, long aProjectId);

    User getCurationTargetUser(String aUser, long aProjectId);

    List<User> listUsersReadyForCuration(String aUsername, Project aProject,
            SourceDocument aDocument);

    List<User> listCuratableUsers(String aSessionOwner, SourceDocument aDocument);

    void setDefaultSelectedUsersForDocument(String aSessionOwner, SourceDocument aDocument);
}

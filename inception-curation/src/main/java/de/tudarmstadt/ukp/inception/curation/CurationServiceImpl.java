/*
 * Copyright 2019
 * Ubiquitous Knowledge Processing (UKP) Lab
 * Technische Universität Darmstadt
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package de.tudarmstadt.ukp.inception.curation;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.apache.uima.cas.CAS;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import de.tudarmstadt.ukp.clarin.webanno.api.DocumentService;
import de.tudarmstadt.ukp.clarin.webanno.model.SourceDocument;
import de.tudarmstadt.ukp.clarin.webanno.security.model.User;

@Component
public class CurationServiceImpl implements CurationService
{
    // stores info on which users are selected and which doc is the curation-doc
    private ConcurrentMap<CurationStateKey, CurationState> curationStates;
    
    private @Autowired DocumentService documentService;

    public CurationServiceImpl()
    {
        curationStates = new ConcurrentHashMap<>();
    }
    
    /**
     * Key to identify curation session for a specific user and project
     */
    private class CurationStateKey
    {
        private String username;
        private long projectId;
        
        public CurationStateKey(String aUser, long aProject) {
            username = aUser;
            projectId = aProject;
        }

        @Override
        public int hashCode()
        {
            return new HashCodeBuilder().append(username).append(projectId).toHashCode();
        }

        @Override
        public boolean equals(Object aOther)
        {
            if (!(aOther instanceof CurationStateKey)) {
                return false;
            }
            CurationStateKey castOther = (CurationStateKey) aOther;
            return new EqualsBuilder().append(username, castOther.username)
                    .append(projectId, castOther.projectId).isEquals();
        }
    }
    
    private CurationState getCurationState(String aUser, long aProjectId) {
        synchronized (curationStates) {
            return curationStates.computeIfAbsent(new CurationStateKey(aUser, aProjectId), 
                key -> new CurationState(aUser));
        }
    }
    
    private class CurationState
    {
        private List<User> selectedUsers;
        // source document of the curated document
        // the curationdoc can be retrieved from user (CURATION or current) and projectId
        private String curationUser;
                
        public CurationState(String aUser)
        {
            curationUser = aUser;
        }

        public List<User> getSelectedUsers()
        {
            return selectedUsers;
        }

        public void setSelectedUsers(Collection<User> aSelectedUsers)
        {
            selectedUsers = new ArrayList<>(aSelectedUsers);
        }

        public String getCurationName()
        {
            return curationUser;
        }

        public void setCurationName(String aCurationName)
        {
            curationUser = aCurationName;
        }
    }

    @Override
    public Optional<List<User>> listUsersSelectedForCuration(String aCurrentUser, long aProjectId)
    {
        return Optional.ofNullable(getCurationState(aCurrentUser, aProjectId).getSelectedUsers());
    }

    @Override
    public Optional<CAS> retrieveCurationCAS(String aUser, long aProjectId, SourceDocument aDoc)
        throws IOException
    {
        String curationUser = getCurationState(aUser, aProjectId).getCurationName();
        if (curationUser == null) {
            return Optional.empty();
        }
        
        return Optional.of(documentService
                .readAnnotationCas(aDoc, curationUser));
    }

    @Override
    public void updateUsersSelectedForCuration(String aCurrentUser, long aProjectId,
            Collection<User> aSelectedUsers)
    {
        synchronized (curationStates)
        {
            getCurationState(aCurrentUser, aProjectId).setSelectedUsers(aSelectedUsers);
        }
    }

    @Override
    public void updateCurationName(String aCurrentUser, long aProjectId, String aUserName)
    {
        synchronized (curationStates)
        {
            getCurationState(aCurrentUser, aProjectId).setCurationName(aUserName);;
        }
    }

    @Override
    public void removeCurrentUserInformation(String aCurrentUser, long aProjectId)
    {
        synchronized (curationStates)
        {
            curationStates.remove(new CurationStateKey(aCurrentUser, aProjectId));
        }
    }

}

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

import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.springframework.stereotype.Component;

import de.tudarmstadt.ukp.clarin.webanno.model.Project;
import de.tudarmstadt.ukp.clarin.webanno.security.model.User;

@Component
public class CurationServiceImpl implements CurationService
{
    // stores info on which users are selected and which doc is the curation-doc
    private ConcurrentMap<CurationStateKey, CurationState> curationStates;

    public CurationServiceImpl()
    {
        // TODO Auto-generated constructor stub
        curationStates = new ConcurrentHashMap<>();
    }
    
    /**
     * Key to identify curation session for a specific user and project
     */
    private class CurationStateKey
    {
        private String username;
        private long projectId;
        
        public CurationStateKey(User aUser, Project aProject) {
            username = aUser.getUsername();
            projectId = aProject.getId();
        }
        
        public String getUsername()
        {
            return username;
        }

        public long getProjectId()
        {
            return projectId;
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
    
    private CurationState getCurationState(User aUser, Project aProject) {
        return curationStates.get(new CurationStateKey(aUser, aProject));
    }
    
    private class CurationState
    {
        //TODO
    }

    @Override
    public List<User> listUsersSelectedForCuration(User aCurrentUser, Project aProject)
    {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public void updateUsersSelectedForCuration(User aCurrentUser, Project aProject)
    {
        // TODO Auto-generated method stub
        
    }
}

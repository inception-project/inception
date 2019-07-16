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
import java.util.Collection;
import java.util.List;
import java.util.Optional;

import org.apache.uima.cas.CAS;

import de.tudarmstadt.ukp.clarin.webanno.model.SourceDocument;
import de.tudarmstadt.ukp.clarin.webanno.security.model.User;

public interface CurationService
{
    /**
     * List users that were selected to be shown for curation by the given user
     */
    public Optional<List<User>> listUsersSelectedForCuration(String aCurrentUser, long aProjectId);
    
    /**
     * retrieves CAS associated with curation doc for the given user
     */
    public Optional<CAS> retrieveCurationCAS(String aUser, long aProjectId, SourceDocument aDoc)
        throws IOException;
    
    /**
     * Store the users that were selected to be shown for curation by the given user
     */
    public void updateUsersSelectedForCuration(String aCurrentUser, long aProjectId, 
            Collection<User> aUsers);
    
    /**
     * Store which name the curated document should be associated with
     */
    public void updateCurationName(String aCurrentUser, long aProjectId, 
            String aCurationName);
    
    /**
     * Removed stored curation information after user session has ended
     */
    public void removeCurrentUserInformation(String aCurrentUser, long aProjectId);
}

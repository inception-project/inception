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
import java.util.Map;
import java.util.Optional;

import org.apache.uima.cas.CAS;

import de.tudarmstadt.ukp.clarin.webanno.api.annotation.model.AnnotatorState;
import de.tudarmstadt.ukp.clarin.webanno.model.SourceDocument;
import de.tudarmstadt.ukp.clarin.webanno.security.model.User;
import de.tudarmstadt.ukp.inception.curation.merge.MergeStrategy;

public interface CurationService
{
    /**
     * List users that were selected to be shown for curation by the given user
     */
    public Optional<List<User>> listUsersSelectedForCuration(String aCurrentUser, long aProjectId);
    
    /**
     * Retrieves CAS associated with curation doc for the given user
     */
    public Optional<CAS> retrieveCurationCAS(String aUser, long aProjectId, SourceDocument aDoc)
        throws IOException;
    
    /**
     * Write to CAS associated with curation doc for the given user and update timestamp
     */
    public void writeCurationCas(CAS aTargetCas, AnnotatorState aState, long aProjectId);
    
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
     * Remove stored curation information on given user
     */
    public void removeCurrentUserInformation(String aCurrentUser, long aProjectId);

    /**
     * Remove information on users that were selected to be shown for curation by the given user 
     */
    public void clearUsersSelectedForCuration(String aUsername, Long aId);

    /**
     * Retrieve cases for the given document for the given users
     */
    public Map<String, CAS> retrieveUserCases(Collection<User> aUsers, SourceDocument aDoc);

    /**
     * Returns the name of the user corresponding to the CAS used as curation (target) CAS
     */
    public String retrieveCurationTarget(String aUser, long aProjectId);

    /**
     * Returns the merge strategy that the user previously selected or the manual one as default
     */
    public MergeStrategy retrieveMergeStrategy(String aUsername,
            long aProjectId);

    /**
     * Store the selected merge-strategy for the given user and project
     */
    public void updateMergeStrategy(String aCurrentUser, long aProjectId, MergeStrategy aStrategy);

    /**
     * Returns the user corresponding to the CAS used as curation (target) CAS
     */
    public User retrieveCurationUser(String aUser, long aProjectId);
}

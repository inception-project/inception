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
import java.util.Set;

import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationSet;
import de.tudarmstadt.ukp.clarin.webanno.model.Project;
import de.tudarmstadt.ukp.clarin.webanno.model.SourceDocument;
import de.tudarmstadt.ukp.clarin.webanno.security.model.User;

public interface CurationSessionService
{
    /**
     * @return the subset of {@code aCandidates} that is currently selected for curation, i.e. the
     *         candidates the session owner has not explicitly deselected. The session stores the
     *         <em>deselected</em> data owners, so a candidate that was never deselected counts as
     *         selected - this is why a freshly started session curates all annotators by default.
     */
    Set<AnnotationSet> getSelectedDataOwners(String aCurrentUser, long aProjectId,
            Collection<AnnotationSet> aCandidates);

    /**
     * Record the selection state for the given {@code aCandidates}: any candidate not contained in
     * {@code aSelected} becomes deselected, any candidate contained in {@code aSelected} is
     * un-deselected. The deselected state is kept per project and keyed by data owner, and the
     * state of data owners outside {@code aCandidates} is left untouched. So a data owner that does
     * not appear as a candidate for the current document keeps its previous state, but a data owner
     * that is a candidate in several documents shares a single project-wide selection state -
     * toggling such a data owner while curating one document also affects the others.
     */
    void setSelectedDataOwners(String aCurrentUser, long aProjectId,
            Collection<AnnotationSet> aCandidates, Collection<AnnotationSet> aSelected);

    @Deprecated
    boolean existsSession(String aSessionOwner, long aProjectId);

    @Deprecated
    void startSession(String aSessionOwner, Project aProject, boolean aOwnDocument);

    @Deprecated
    void closeSession(String aCurrentUser, long aProjectId);

    String getCurationTarget(String aUser, long aProjectId);

    User getCurationTargetUser(String aUser, long aProjectId);

    boolean isShowAll(String aSessionOwner, Long aProjectId);

    void setShowAll(String aSessionOwner, Long aProjectId, boolean aValue);

    boolean isShowScore(String aSessionOwner, Long aProjectId);

    void setShowScore(String aSessionOwner, Long aProjectId, boolean aValue);

    List<AnnotationSet> listDataOwnersReadyForCuration(String aUsername, Project aProject,
            SourceDocument aDocument);

    List<AnnotationSet> listCuratableDataOwners(String aSessionOwner, SourceDocument aDocument);
}

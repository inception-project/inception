/*
 * Copyright 2019
 * Ubiquitous Knowledge Processing (UKP) Lab
 * Technische Universität Darmstadt
 * 
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
package de.tudarmstadt.ukp.inception.ui.curation.sidebar;

import java.io.IOException;
import java.util.Collection;
import java.util.List;

import org.apache.uima.UIMAException;

import de.tudarmstadt.ukp.clarin.webanno.model.Project;
import de.tudarmstadt.ukp.clarin.webanno.model.SourceDocument;
import de.tudarmstadt.ukp.clarin.webanno.security.model.User;
import de.tudarmstadt.ukp.inception.curation.merge.strategy.MergeStrategyFactory;
import de.tudarmstadt.ukp.inception.curation.model.CurationWorkflow;
import de.tudarmstadt.ukp.inception.rendering.editorstate.AnnotatorState;

public interface CurationSidebarService
{
    /**
     * @return list of users that were selected to be shown for curation by the given user
     */
    @SuppressWarnings("javadoc")
    List<User> getSelectedUsers(String aCurrentUser, long aProjectId);

    /**
     * Store the users that were selected to be shown for curation by the given user.
     */
    @SuppressWarnings("javadoc")
    void setSelectedUsers(String aCurrentUser, long aProjectId, Collection<User> aUsers);

    /**
     * Start a new curation session.
     */
    @SuppressWarnings("javadoc")
    boolean existsSession(String aSessionOwner, long aProjectId);

    /**
     * Start a new curation session.
     */
    @SuppressWarnings("javadoc")
    void startSession(String aSessionOwner, Project aProjectId, boolean aOwnDocument);

    /**
     * Stop a running curation session.
     */
    @SuppressWarnings("javadoc")
    void closeSession(String aCurrentUser, long aProjectId);

    /**
     * @return the name of the user corresponding to the CAS used as curation (target) CAS
     */
    @SuppressWarnings("javadoc")
    String getCurationTarget(String aUser, long aProjectId);

    /**
     * @return the user corresponding to the CAS used as curation (target) CAS
     */
    @SuppressWarnings("javadoc")
    User getCurationTargetUser(String aUser, long aProjectId);

    /**
     * @return list of users that were selected to be shown for curation by the given user and have
     *         finished the given document.
     */
    @SuppressWarnings("javadoc")
    List<User> listUsersReadyForCuration(String aUsername, Project aProject,
            SourceDocument aDocument);

    List<User> listCuratableUsers(String aSessionOwner, SourceDocument aDocument);

    /**
     * @return if user in given annotator state is curating and has finished it
     * 
     * @param state
     *            the annotator state
     * @param currentUsername
     *            the currently logged in user
     */
    boolean isCurationFinished(AnnotatorState state, String currentUsername);

    void setShowAll(String aUsername, Long aProjectId, boolean aValue);

    boolean isShowAll(String aUsername, Long aProjectId);

    void setShowScore(String aUsername, Long aProjectId, boolean aValue);

    boolean isShowScore(String aUsername, Long aProjectId);

    void setDefaultSelectedUsersForDocument(String aSessionOwner, SourceDocument aDocument);

    MergeStrategyFactory<?> merge(AnnotatorState aState, CurationWorkflow aWorkflow,
            String aCurator, Collection<User> aUsers, boolean aClearTargetCas)
        throws IOException, UIMAException;
}

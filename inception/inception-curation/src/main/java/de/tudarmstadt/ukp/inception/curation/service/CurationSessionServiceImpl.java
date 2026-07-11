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
package de.tudarmstadt.ukp.inception.curation.service;

import static de.tudarmstadt.ukp.clarin.webanno.model.PermissionLevel.ANNOTATOR;
import static de.tudarmstadt.ukp.inception.curation.model.CurationSessionPreferences.KEY_CURATION_SESSION;
import static de.tudarmstadt.ukp.inception.support.WebAnnoConst.CURATION_USER;
import static java.util.Collections.emptyList;
import static java.util.Collections.emptySet;
import static java.util.stream.Collectors.toCollection;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.springframework.context.event.EventListener;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.security.core.session.SessionDestroyedEvent;
import org.springframework.security.core.session.SessionRegistry;
import org.springframework.transaction.annotation.Transactional;

import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationSet;
import de.tudarmstadt.ukp.clarin.webanno.model.Project;
import de.tudarmstadt.ukp.clarin.webanno.model.SourceDocument;
import de.tudarmstadt.ukp.clarin.webanno.security.UserDao;
import de.tudarmstadt.ukp.clarin.webanno.security.model.User;
import de.tudarmstadt.ukp.inception.curation.api.CurationSessionService;
import de.tudarmstadt.ukp.inception.curation.model.CurationSessionPreferences;
import de.tudarmstadt.ukp.inception.curation.sidebar.CurationSidebarProperties;
import de.tudarmstadt.ukp.inception.documents.api.DocumentService;
import de.tudarmstadt.ukp.inception.preferences.PreferencesService;
import de.tudarmstadt.ukp.inception.project.api.ProjectService;

public class CurationSessionServiceImpl
    implements CurationSessionService
{
    private final ConcurrentMap<CurationSessionKey, CurationSession> sessions;

    private final PreferencesService preferencesService;
    private final SessionRegistry sessionRegistry;
    private final ProjectService projectService;
    private final UserDao userRegistry;
    private final CurationSidebarProperties curationSidebarProperties;
    private final CurationDocumentService curationDocumentService;
    private final DocumentService documentService;

    public CurationSessionServiceImpl(PreferencesService aPreferencesService,
            SessionRegistry aSessionRegistry, ProjectService aProjectService, UserDao aUserRegistry,
            CurationSidebarProperties aCurationSidebarProperties,
            CurationDocumentService aCurationDocumentService, DocumentService aDocumentService)
    {
        sessions = new ConcurrentHashMap<>();
        preferencesService = aPreferencesService;
        sessionRegistry = aSessionRegistry;
        projectService = aProjectService;
        userRegistry = aUserRegistry;
        curationSidebarProperties = aCurationSidebarProperties;
        curationDocumentService = aCurationDocumentService;
        documentService = aDocumentService;
    }

    @Transactional
    @Override
    public void setSelectedDataOwners(String aSessionOwner, long aProjectId,
            Collection<AnnotationSet> aCandidates, Collection<AnnotationSet> aSelected)
    {
        synchronized (sessions) {
            var session = sessions.get(new CurationSessionKey(aSessionOwner, aProjectId));
            if (session == null) {
                return;
            }

            var deselected = new LinkedHashSet<>(session.getDeselectedDataOwners());
            deselected.removeAll(aCandidates);
            for (var candidate : aCandidates) {
                if (!aSelected.contains(candidate)) {
                    deselected.add(candidate);
                }
            }
            session.setDeselectedDataOwners(deselected);
        }
    }

    @Transactional
    @Override
    public Set<AnnotationSet> getSelectedDataOwners(String aSessionOwner, long aProjectId,
            Collection<AnnotationSet> aCandidates)
    {
        synchronized (sessions) {
            var session = sessions.get(new CurationSessionKey(aSessionOwner, aProjectId));
            if (session == null) {
                return emptySet();
            }

            var selected = new LinkedHashSet<>(aCandidates);
            selected.removeAll(session.getDeselectedDataOwners());
            return selected;
        }
    }

    @Transactional
    @Override
    public List<AnnotationSet> listDataOwnersReadyForCuration(String aSessionOwner,
            Project aProject, SourceDocument aDocument)
    {
        String curationTarget;
        Set<AnnotationSet> deselected;
        synchronized (sessions) {
            var session = sessions.get(new CurationSessionKey(aSessionOwner, aProject.getId()));
            if (session == null) {
                return emptyList();
            }

            curationTarget = session.getCurationTarget();
            deselected = new LinkedHashSet<>(session.getDeselectedDataOwners());
        }

        // The curatable-user lookup hits the database, so we run it outside the sessions lock,
        // having captured the session state we need above.
        return listCuratableUserNames(aDocument, aSessionOwner, curationTarget).stream() //
                .map(AnnotationSet::forUser) //
                .filter(dataOwner -> !deselected.contains(dataOwner)) //
                .toList();
    }

    @Transactional
    @Override
    public List<AnnotationSet> listCuratableDataOwners(String aSessionOwner,
            SourceDocument aDocument)
    {
        var project = aDocument.getProject();
        var curationTarget = getCurationTarget(aSessionOwner, project.getId());
        var curatableUserNames = listCuratableUserNames(aDocument, aSessionOwner, curationTarget);

        var dataOwners = documentService.getDataOwners(project, curatableUserNames);
        return curatableUserNames.stream().map(dataOwners::get).toList();
    }

    /**
     * @return the usernames of the annotators that can be curated for the given document: the
     *         curatable users, excluding the session owner unless they are curating into their own
     *         document (i.e. the curation target is not the {@code CURATION_USER}).
     */
    private List<String> listCuratableUserNames(SourceDocument aDocument, String aSessionOwner,
            String aCurationTarget)
    {
        return curationDocumentService.listCuratableUsers(aDocument).stream() //
                .map(User::getUsername) //
                .filter(username -> !username.equals(aSessionOwner)
                        || CURATION_USER.equals(aCurationTarget)) //
                .toList();
    }

    @Override
    @Deprecated
    public boolean existsSession(String aSessionOwner, long aProjectId)
    {
        synchronized (sessions) {
            return sessions.containsKey(new CurationSessionKey(aSessionOwner, aProjectId));
        }
    }

    @Override
    @Deprecated
    public void startSession(String aSessionOwner, Project aProject, boolean aOwnDocument)
    {
        synchronized (sessions) {
            getSession(aSessionOwner, aProject.getId());
            setCurationTarget(aSessionOwner, aProject, aOwnDocument);
        }
    }

    @Override
    @Deprecated
    public void closeSession(String aSessionOwner, long aProjectId)
    {
        synchronized (sessions) {
            sessions.remove(new CurationSessionKey(aSessionOwner, aProjectId));
        }
    }

    @Transactional
    @Override
    public String getCurationTarget(String aSessionOwner, long aProjectId)
    {
        String curationUser;
        synchronized (sessions) {
            curationUser = getSession(aSessionOwner, aProjectId).getCurationTarget();
        }

        if (curationUser == null) {
            return aSessionOwner;
        }

        return curationUser;
    }

    @Transactional
    @Override
    public User getCurationTargetUser(String aSessionOwner, long aProjectId)
    {
        String curationUser;
        synchronized (sessions) {
            curationUser = getSession(aSessionOwner, aProjectId).getCurationTarget();
        }

        if (curationUser == null) {
            return userRegistry.get(aSessionOwner);
        }

        if (CURATION_USER.equals(curationUser)) {
            return userRegistry.getCurationUser();
        }

        return userRegistry.get(curationUser);
    }

    @Transactional
    public boolean isShowAll(String aSessionOwner, Long aProjectId)
    {
        synchronized (sessions) {
            return getSession(aSessionOwner, aProjectId).isShowAll();
        }
    }

    @Transactional
    public void setShowAll(String aSessionOwner, Long aProjectId, boolean aValue)
    {
        synchronized (sessions) {
            getSession(aSessionOwner, aProjectId).setShowAll(aValue);
        }
    }

    @Transactional
    public boolean isShowScore(String aSessionOwner, Long aProjectId)
    {
        synchronized (sessions) {
            return getSession(aSessionOwner, aProjectId).isShowScore();
        }
    }

    @Transactional
    public void setShowScore(String aSessionOwner, Long aProjectId, boolean aValue)
    {
        synchronized (sessions) {
            getSession(aSessionOwner, aProjectId).setShowScore(aValue);
        }
    }

    // Set order so this is handled before session info is removed from sessionRegistry
    @Order(Ordered.HIGHEST_PRECEDENCE)
    @EventListener
    @Transactional
    public void onSessionDestroyed(SessionDestroyedEvent event)
    {
        var info = sessionRegistry.getSessionInformation(event.getId());

        if (info == null) {
            return;
        }

        User user = null;
        if (info.getPrincipal() instanceof String) {
            user = userRegistry.get((String) info.getPrincipal());
        }

        if (info.getPrincipal() instanceof User) {
            user = (User) info.getPrincipal();
        }

        if (user == null) {
            // This happens e.g. when a session for "anonymousUser" is destroyed or if (for some
            // reason), the user owning the session no longer exists in the system.
            return;
        }

        // FIXME: Seems kind of pointless to first store everything and then delete it...
        storeCurationSettings(user);
        closeAllSessions(user);
    }

    /**
     * Store which name the curated document should be associated with
     */
    @Transactional
    private void setCurationTarget(String aSessionOwner, Project aProject, boolean aOwnDocument)
    {
        synchronized (sessions) {
            var session = sessions.get(new CurationSessionKey(aSessionOwner, aProject.getId()));
            if (session == null) {
                return;
            }

            if (curationSidebarProperties.isOwnUserCurationTargetEnabled() && aOwnDocument
                    && projectService.hasRole(aSessionOwner, aProject, ANNOTATOR)) {
                session.setCurationTarget(aSessionOwner);
            }
            else {
                session.setCurationTarget(CURATION_USER);
            }
        }
    }

    private CurationSession getSession(String aSessionOwner, long aProjectId)
    {
        synchronized (sessions) {
            if (sessions.containsKey(new CurationSessionKey(aSessionOwner, aProjectId))) {
                return sessions.get(new CurationSessionKey(aSessionOwner, aProjectId));
            }
            else {
                return readSession(aSessionOwner, aProjectId);
            }
        }
    }

    private CurationSession readSession(String aSessionOwner, long aProjectId)
    {
        var user = userRegistry.get(aSessionOwner);
        var project = projectService.getProject(aProjectId);

        var traits = (user != null && project != null) ? preferencesService
                .loadOptionalTraitsForUserAndProject(KEY_CURATION_SESSION, user, project)
                .orElse(null) : null;

        CurationSession state;
        if (traits == null) {
            state = new CurationSession(aSessionOwner);
        }
        else {
            var deselectedDataOwners = traits.getDeselectedDataOwners().stream() //
                    .map(AnnotationSet::forUser) //
                    .collect(toCollection(LinkedHashSet::new));
            state = new CurationSession(traits.getCurationTarget(), deselectedDataOwners);
        }

        sessions.put(new CurationSessionKey(aSessionOwner, aProjectId), state);
        return state;
    }

    /**
     * Write this user's active curation sessions to the preferences store.
     */
    private void storeCurationSettings(User aSessionOwner)
    {
        var username = aSessionOwner.getUsername();

        // Snapshot the traits for this user's active sessions under the lock, then persist them
        // outside it. We iterate only the sessions this user actually has instead of scanning all
        // accessible projects.
        var traitsByProject = new LinkedHashMap<Long, CurationSessionPreferences>();
        synchronized (sessions) {
            for (var entry : sessions.entrySet()) {
                if (!entry.getKey().username.equals(username)) {
                    continue;
                }

                var state = entry.getValue();
                var traits = new CurationSessionPreferences();
                traits.setCurationTarget(state.getCurationTarget());
                traits.setDeselectedDataOwners(state.getDeselectedDataOwners().stream() //
                        .map(AnnotationSet::id) //
                        .collect(toCollection(LinkedHashSet::new)));
                traitsByProject.put(entry.getKey().projectId, traits);
            }
        }

        for (var entry : traitsByProject.entrySet()) {
            var project = projectService.getProject(entry.getKey());
            if (project == null) {
                continue;
            }

            preferencesService.saveTraitsForUserAndProject(KEY_CURATION_SESSION, aSessionOwner,
                    project, entry.getValue());
        }
    }

    private void closeAllSessions(User aSessionOwner)
    {
        synchronized (sessions) {
            sessions.keySet().removeIf(key -> key.username.equals(aSessionOwner.getUsername()));
        }
    }

    private static class CurationSessionKey
    {
        private final String username;
        private final long projectId;

        public CurationSessionKey(String aUser, long aProject)
        {
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
            if (!(aOther instanceof CurationSessionKey)) {
                return false;
            }

            var castOther = (CurationSessionKey) aOther;
            return new EqualsBuilder().append(username, castOther.username)
                    .append(projectId, castOther.projectId).isEquals();
        }
    }

    private class CurationSession
    {
        // The data owners the session owner has explicitly deselected. Everything not listed here
        // counts as selected, so an empty set means "curate all annotators".
        private Set<AnnotationSet> deselectedDataOwners = new LinkedHashSet<>();
        // to find source document of the curated document
        // the curationdoc can be retrieved from user (CURATION or current) and projectId
        private String curationTarget;
        private boolean showAll;
        private boolean showScore = true;

        public CurationSession(String aUser)
        {
            curationTarget = aUser;
        }

        public CurationSession(String aCurationTarget,
                Collection<AnnotationSet> aDeselectedDataOwners)
        {
            curationTarget = aCurationTarget;
            deselectedDataOwners = new LinkedHashSet<>(aDeselectedDataOwners);
        }

        public Set<AnnotationSet> getDeselectedDataOwners()
        {
            return deselectedDataOwners;
        }

        public void setDeselectedDataOwners(Collection<AnnotationSet> aDeselectedDataOwners)
        {
            deselectedDataOwners = new LinkedHashSet<>(aDeselectedDataOwners);
        }

        public String getCurationTarget()
        {
            return curationTarget;
        }

        public void setCurationTarget(String aCurationTarget)
        {
            curationTarget = aCurationTarget;
        }

        public boolean isShowAll()
        {
            return showAll;
        }

        public void setShowAll(boolean aShowAll)
        {
            showAll = aShowAll;
        }

        public boolean isShowScore()
        {
            return showScore;
        }

        public void setShowScore(boolean aShowScore)
        {
            showScore = aShowScore;
        }
    }
}

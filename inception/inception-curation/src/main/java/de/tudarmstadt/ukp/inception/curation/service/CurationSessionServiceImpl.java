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
import static de.tudarmstadt.ukp.inception.support.WebAnnoConst.CURATION_USER;
import static java.util.Collections.emptyList;
import static java.util.Collections.unmodifiableList;
import static java.util.stream.Collectors.toSet;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import org.apache.commons.lang3.Validate;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.springframework.context.event.EventListener;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.security.core.session.SessionDestroyedEvent;
import org.springframework.security.core.session.SessionRegistry;
import org.springframework.transaction.annotation.Transactional;

import de.tudarmstadt.ukp.clarin.webanno.model.Project;
import de.tudarmstadt.ukp.clarin.webanno.model.SourceDocument;
import de.tudarmstadt.ukp.clarin.webanno.security.UserDao;
import de.tudarmstadt.ukp.clarin.webanno.security.model.User;
import de.tudarmstadt.ukp.inception.curation.api.CurationSessionService;
import de.tudarmstadt.ukp.inception.curation.model.CurationSettings;
import de.tudarmstadt.ukp.inception.curation.model.CurationSettingsId;
import de.tudarmstadt.ukp.inception.curation.sidebar.CurationSidebarProperties;
import de.tudarmstadt.ukp.inception.project.api.ProjectService;
import jakarta.persistence.EntityManager;

public class CurationSessionServiceImpl
    implements CurationSessionService
{
    private final ConcurrentMap<CurationSessionKey, CurationSession> sessions;

    private final EntityManager entityManager;
    private final SessionRegistry sessionRegistry;
    private final ProjectService projectService;
    private final UserDao userRegistry;
    private final CurationSidebarProperties curationSidebarProperties;
    private final CurationDocumentService curationDocumentService;

    public CurationSessionServiceImpl(EntityManager aEntityManager,
            SessionRegistry aSessionRegistry, ProjectService aProjectService, UserDao aUserRegistry,
            CurationSidebarProperties aCurationSidebarProperties,
            CurationDocumentService aCurationDocumentService)
    {
        sessions = new ConcurrentHashMap<>();
        entityManager = aEntityManager;
        sessionRegistry = aSessionRegistry;
        projectService = aProjectService;
        userRegistry = aUserRegistry;
        curationSidebarProperties = aCurationSidebarProperties;
        curationDocumentService = aCurationDocumentService;
    }

    @Transactional
    @Override
    public void setSelectedUsers(String aSessionOwner, long aProjectId,
            Collection<User> aSelectedUsers)
    {
        synchronized (sessions) {
            var session = sessions.get(new CurationSessionKey(aSessionOwner, aProjectId));
            if (session == null) {
                return;
            }

            session.setSelectedUsers(aSelectedUsers);
        }
    }

    @Transactional
    @Override
    public List<User> getSelectedUsers(String aSessionOwner, long aProjectId)
    {
        synchronized (sessions) {
            var session = sessions.get(new CurationSessionKey(aSessionOwner, aProjectId));
            if (session == null) {
                return emptyList();
            }

            var selectedUsers = session.getSelectedUsers();
            return selectedUsers == null ? emptyList() : unmodifiableList(selectedUsers);
        }
    }

    @Transactional
    @Override
    public List<User> listUsersReadyForCuration(String aSessionOwner, Project aProject,
            SourceDocument aDocument)
    {
        synchronized (sessions) {
            var session = sessions.get(new CurationSessionKey(aSessionOwner, aProject.getId()));
            if (session == null) {
                return emptyList();
            }

            var selectedUsers = session.getSelectedUsers();
            if (selectedUsers == null || selectedUsers.isEmpty()) {
                return emptyList();
            }

            var finishedUsers = curationDocumentService.listCuratableUsers(aDocument);
            finishedUsers.retainAll(selectedUsers);
            return finishedUsers;
        }
    }

    @Transactional
    @Override
    public List<User> listCuratableUsers(String aSessionOwner, SourceDocument aDocument)
    {
        var curationTarget = getCurationTarget(aSessionOwner, aDocument.getProject().getId());
        return curationDocumentService.listCuratableUsers(aDocument).stream()
                .filter(user -> !user.getUsername().equals(aSessionOwner)
                        || curationTarget.equals(CURATION_USER))
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

    @Override
    public void setDefaultSelectedUsersForDocument(String aSessionOwner, SourceDocument aDocument)
    {
        var project = aDocument.getProject();

        if (!existsSession(aSessionOwner, project.getId())) {
            return;
        }

        // The set of curatable annotators can change from document to document, so we reset the
        // selected users every time the document is switched
        setSelectedUsers(aSessionOwner, project.getId(),
                listCuratableUsers(aSessionOwner, aDocument));
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
        var settings = queryDBForSetting(aSessionOwner, aProjectId);

        CurationSession state;
        if (settings.isEmpty()) {
            state = new CurationSession(aSessionOwner);
        }
        else {
            var setting = settings.get(0);
            var project = projectService.getProject(aProjectId);
            var users = new ArrayList<User>();
            if (!setting.getSelectedUserNames().isEmpty()) {
                setting.getSelectedUserNames().stream().map(username -> userRegistry.get(username))
                        .filter(user -> projectService.hasAnyRole(user, project)) //
                        .forEach(users::add);
            }
            state = new CurationSession(setting.getCurationUserName(), users);
        }

        sessions.put(new CurationSessionKey(aSessionOwner, aProjectId), state);
        return state;
    }

    private List<CurationSettings> queryDBForSetting(String aSessionOwner, long aProjectId)
    {
        Validate.notBlank(aSessionOwner, "Session owner must be specified");
        Validate.notNull(aProjectId, "Project must be specified");

        var query = "FROM " + CurationSettings.class.getName() //
                + " o WHERE o.username = :username " //
                + "AND o.projectId = :projectId";

        return entityManager //
                .createQuery(query, CurationSettings.class) //
                .setParameter("username", aSessionOwner) //
                .setParameter("projectId", aProjectId) //
                .setMaxResults(1) //
                .getResultList();
    }

    /**
     * Write settings for all projects of this user to the data base
     */
    private void storeCurationSettings(User aSessionOwner)
    {
        var aUsername = aSessionOwner.getUsername();

        for (var project : projectService.listAccessibleProjects(aSessionOwner)) {
            var projectId = project.getId();
            Set<String> usernames = null;
            if (sessions.containsKey(new CurationSessionKey(aUsername, projectId))) {

                var state = sessions.get(new CurationSessionKey(aUsername, projectId));
                // user does not exist anymore or is anonymous authentication
                if (state == null) {
                    continue;
                }

                if (state.getSelectedUsers() != null) {
                    usernames = state.getSelectedUsers().stream() //
                            .map(User::getUsername) //
                            .collect(toSet());
                }

                // get setting from context and update values if it exists, else save new setting
                // to db
                var setting = entityManager.find(CurationSettings.class,
                        new CurationSettingsId(projectId, aUsername));

                if (setting != null) {
                    setting.setSelectedUserNames(usernames);
                    setting.setCurationUserName(state.getCurationTarget());
                }
                else {
                    setting = new CurationSettings(aUsername, projectId, state.getCurationTarget(),
                            usernames);
                    entityManager.persist(setting);
                }
            }
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
        private List<User> selectedUsers;
        // to find source document of the curated document
        // the curationdoc can be retrieved from user (CURATION or current) and projectId
        private String curationTarget;
        private boolean showAll;
        private boolean showScore = true;

        public CurationSession(String aUser)
        {
            curationTarget = aUser;
        }

        public CurationSession(String aCurationTarget, List<User> aSelectedUsers)
        {
            curationTarget = aCurationTarget;
            selectedUsers = new ArrayList<>(aSelectedUsers);
        }

        public List<User> getSelectedUsers()
        {
            return selectedUsers;
        }

        public void setSelectedUsers(Collection<User> aSelectedUsers)
        {
            selectedUsers = new ArrayList<>(aSelectedUsers);
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

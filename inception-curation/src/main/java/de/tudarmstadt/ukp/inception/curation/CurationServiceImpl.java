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

import static de.tudarmstadt.ukp.clarin.webanno.api.WebAnnoConst.CURATION_USER;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.stream.Collectors;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;

import org.apache.commons.lang3.Validate;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.apache.uima.cas.CAS;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.event.EventListener;
import org.springframework.security.core.session.SessionDestroyedEvent;
import org.springframework.security.core.session.SessionInformation;
import org.springframework.security.core.session.SessionRegistry;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import de.tudarmstadt.ukp.clarin.webanno.api.CasStorageService;
import de.tudarmstadt.ukp.clarin.webanno.api.DocumentService;
import de.tudarmstadt.ukp.clarin.webanno.api.ProjectService;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.model.AnnotatorState;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.model.AnnotatorStateUtils;
import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationDocument;
import de.tudarmstadt.ukp.clarin.webanno.model.Project;
import de.tudarmstadt.ukp.clarin.webanno.model.SourceDocument;
import de.tudarmstadt.ukp.clarin.webanno.security.UserDao;
import de.tudarmstadt.ukp.clarin.webanno.security.model.User;
import de.tudarmstadt.ukp.inception.curation.merge.ManualMergeStrategy;
import de.tudarmstadt.ukp.inception.curation.merge.MergeStrategy;

@Component
public class CurationServiceImpl
    implements CurationService
{
    private Logger log = LoggerFactory.getLogger(getClass());
    
    // stores info on which users are selected and which doc is the curation-doc
    private ConcurrentMap<CurationStateKey, CurationState> curationStates;
    
    @PersistenceContext
    private EntityManager entityManager;
    
    private @Autowired DocumentService documentService;
    private @Autowired SessionRegistry sessionRegistry;
    private @Autowired ProjectService projectService;
    private @Autowired UserDao userRegistry;
    private @Autowired CasStorageService casStorageService;

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
    
    private CurationState getCurationState(String aUser, long aProjectId)
    {
        if (curationStates.containsKey(new CurationStateKey(aUser, aProjectId))) {
            return curationStates.get(new CurationStateKey(aUser, aProjectId));
        }
        else {
            return getSettingsFromDB(aUser, aProjectId);
        }
    }
    
    @Transactional
    private CurationState getSettingsFromDB(String aUsername, long aProjectId)
    {
        List<CurationSettings> settings = queryDBForSetting(aUsername, aProjectId);
        
        CurationState state;
        if (settings.isEmpty()) {
            state = new CurationState(aUsername);
        }
        else {
            CurationSettings setting = settings.get(0);
            Project project = projectService.getProject(aProjectId);
            List<User> users = new ArrayList<>();
            if (!setting.getSelectedUserNames().isEmpty()) {
                users = setting.getSelectedUserNames().stream()
                    .map(username -> userRegistry.get(username))
                    .filter(user -> projectService.existsProjectPermission(user, project))
                    .collect(Collectors.toList());
            }
            state = new CurationState(setting.getCurationUserName(), 
                    users);
        }
        
        curationStates.put(new CurationStateKey(aUsername, aProjectId), state);
        return state;
    }

    private List<CurationSettings> queryDBForSetting(String aUsername, long aProjectId)
    {
        Validate.notBlank(aUsername, "User must be specified");
        Validate.notNull(aProjectId, "project must be specified");
        
        String query = "FROM " + CurationSettings.class.getName() 
                + " o WHERE o.username = :username " 
                + "AND o.projectId = :projectId";
        
        List<CurationSettings> settings = entityManager
                .createQuery(query, CurationSettings.class)
                .setParameter("username", aUsername)
                .setParameter("projectId", aProjectId)
                .setMaxResults(1)
                .getResultList();
        return settings;
    }

    private class CurationState
    {
        private List<User> selectedUsers;
        // to find source document of the curated document
        // the curationdoc can be retrieved from user (CURATION or current) and projectId
        private String curationUser;
        private MergeStrategy selectedStrategy;
                
        public CurationState(String aUser)
        {
            curationUser = aUser;
            selectedStrategy = new ManualMergeStrategy();
        }

        public CurationState(String aCurationUserName,
                List<User> aSelectedUsers)
        {
            curationUser = aCurationUserName;
            selectedUsers = new ArrayList<>(aSelectedUsers);
            selectedStrategy = new ManualMergeStrategy();
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

        public void setMergeStrategy(MergeStrategy aStrategy)
        {
            selectedStrategy = aStrategy;
        }

        public MergeStrategy getMergeStrategy()
        {
            return selectedStrategy;
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
    public synchronized void writeCurationCas(CAS aTargetCas, AnnotatorState aState,
            long aProjectId)
    {
        SourceDocument doc = aState.getDocument();
        String curatorName = getCurationState(aState.getUser().getUsername(), aProjectId)
                .getCurationName();
        try {
            User curator;
            if (curatorName.equals(CURATION_USER)) {
                curator = new User(CURATION_USER);
            }
            else {
                curator = userRegistry.get(curatorName);
            }
            AnnotationDocument annoDoc = documentService.createOrGetAnnotationDocument(doc,
                    curator);
            documentService.writeAnnotationCas(aTargetCas, annoDoc, true);
            AnnotatorStateUtils.updateDocumentTimestampAfterWrite(aState,
                    casStorageService.getCasTimestamp(doc, curatorName));
        }
        catch (IOException e) {
            log.warn(String.format("Could not write CAS for user %s and document %d", curatorName,
                    doc.getId()));
            e.printStackTrace();
        }
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
    public void updateMergeStrategy(String aCurrentUser, long aProjectId, MergeStrategy aStrategy)
    {
        synchronized (curationStates)
        {
            getCurationState(aCurrentUser, aProjectId).setMergeStrategy(aStrategy);;
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
    
    @EventListener
    @Transactional
    public void onSessionDestroyed(SessionDestroyedEvent event)
    {
        SessionInformation info = sessionRegistry.getSessionInformation(event.getId());
        
        if (info == null) {
            return;
        }
        
        User user = userRegistry.get((String) info.getPrincipal());
        if (user == null) {
            // This happens e.g. when a session for "anonymousUser" is destroyed or if (for some
            // reason), the user owning the session no longer exists in the system.
            return;
        }
        
        storeCurationSettings(user);
        clearState(user);
    }

    /**
     * Write settings for all projects of this user to the data base
     */
    private void storeCurationSettings(User aUser)
    {
        String aUsername = aUser.getUsername();
        
        for (Project project : projectService.listAccessibleProjects(aUser)) {
            Long projectId = project.getId();
            Set<String> usernames = null;
            if (curationStates.containsKey(new CurationStateKey(aUsername, projectId))) {
                
                CurationState state = curationStates
                        .get(new CurationStateKey(aUsername, projectId));
                // user does not exist anymore or is anonymous authentication
                if (state == null) {
                    continue;
                }
                
                if (state.getSelectedUsers() != null) {
                    usernames = state.getSelectedUsers().stream().map(User::getUsername)
                            .collect(Collectors.toSet());
                }
                
                // get setting from context and update values if it exists, else save new setting
                // to db
                CurationSettings setting = entityManager.find(CurationSettings.class,
                        new CurationSettingsId(projectId, aUsername));

                if (setting != null) {
                    setting.setSelectedUserNames(usernames);
                    setting.setCurationUserName(state.getCurationName());
                }
                else {
                    setting = new CurationSettings(aUsername, projectId, state.getCurationName(),
                            usernames);
                    entityManager.persist(setting);
                }
            }
        }
    }

    private void clearState(User aUser)
    {
        projectService.listAccessibleProjects(aUser).stream()
            .map(Project::getId)
            .forEach(pId -> removeCurrentUserInformation(aUser.getUsername(), pId));
    }

    @Override
    public void clearUsersSelectedForCuration(String aUsername, Long aProjectId)
    {
        synchronized (curationStates)
        {
            getCurationState(aUsername, aProjectId).setSelectedUsers(new ArrayList<>());
        }   
    }

    @Override
    public Map<String, CAS> retrieveUserCases(Collection<User> aUsers, SourceDocument aDoc)
    {
        Map<String, CAS> casses = new HashMap<>();
        for (User user : aUsers) {
            try {
                String username = user.getUsername();
                casses.put(username, documentService.readAnnotationCas(aDoc,
                        username));
            }
            catch (IOException e) {
                log.warn(String.format("Could not retrieve CAS for user %s and document %d",
                        user.getUsername(), aDoc.getId()));
                e.printStackTrace();
            }
        }
        return casses;
    }

    @Override
    public String retrieveCurationTarget(String aUser, long aProjectId)
    {
        String curationUser = getCurationState(aUser, aProjectId).getCurationName();
        if (curationUser == null) {
            // default to user as curation target
            return aUser;
        }
        return curationUser;
    }
    
    @Override
    public User retrieveCurationUser(String aUser, long aProjectId)
    {
        String curationUser = getCurationState(aUser, aProjectId).getCurationName();
        if (curationUser == null) {
            // default to user as curation target
            return userRegistry.get(aUser);
        }
        else if (curationUser.equals(CURATION_USER)) {
            return new User(CURATION_USER);
        }
        else {
            return userRegistry.get(curationUser);
        }
    }

    @Override
    public MergeStrategy retrieveMergeStrategy(String aUsername,
            long aProjectId)
    {
        return getCurationState(aUsername, aProjectId).getMergeStrategy();
    }

}

/*
 * Copyright 2012
 * Ubiquitous Knowledge Processing (UKP) Lab and FG Language Technology
 * Technische Universität Darmstadt
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package de.tudarmstadt.ukp.clarin.webanno.project;

import static de.tudarmstadt.ukp.clarin.webanno.model.SourceDocumentState.ANNOTATION_FINISHED;
import static de.tudarmstadt.ukp.clarin.webanno.model.SourceDocumentState.ANNOTATION_IN_PROGRESS;
import static de.tudarmstadt.ukp.clarin.webanno.model.SourceDocumentState.CURATION_FINISHED;
import static de.tudarmstadt.ukp.clarin.webanno.model.SourceDocumentState.CURATION_IN_PROGRESS;
import static de.tudarmstadt.ukp.clarin.webanno.model.SourceDocumentState.NEW;
import static java.util.Comparator.comparingInt;
import static org.apache.commons.io.IOUtils.closeQuietly;
import static org.apache.commons.io.IOUtils.copyLarge;

import java.beans.PropertyDescriptor;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.List;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Properties;
import java.util.Set;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import javax.persistence.EntityManager;
import javax.persistence.NoResultException;
import javax.persistence.PersistenceContext;
import javax.persistence.Query;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.beans.BeanWrapper;
import org.springframework.beans.PropertyAccessorFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.SmartLifecycle;
import org.springframework.context.annotation.ClassPathScanningCandidateComponentProvider;
import org.springframework.core.type.filter.AnnotationTypeFilter;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

import de.tudarmstadt.ukp.clarin.webanno.api.ProjectService;
import de.tudarmstadt.ukp.clarin.webanno.api.ProjectType;
import de.tudarmstadt.ukp.clarin.webanno.api.SecurityUtil;
import de.tudarmstadt.ukp.clarin.webanno.api.event.AfterProjectCreatedEvent;
import de.tudarmstadt.ukp.clarin.webanno.api.event.BeforeProjectRemovedEvent;
import de.tudarmstadt.ukp.clarin.webanno.api.event.ProjectStateChangedEvent;
import de.tudarmstadt.ukp.clarin.webanno.export.model.ExportedProjectPermission;
import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationDocumentState;
import de.tudarmstadt.ukp.clarin.webanno.model.Mode;
import de.tudarmstadt.ukp.clarin.webanno.model.PermissionLevel;
import de.tudarmstadt.ukp.clarin.webanno.model.Project;
import de.tudarmstadt.ukp.clarin.webanno.model.ProjectPermission;
import de.tudarmstadt.ukp.clarin.webanno.model.ProjectState;
import de.tudarmstadt.ukp.clarin.webanno.model.SourceDocument;
import de.tudarmstadt.ukp.clarin.webanno.security.UserDao;
import de.tudarmstadt.ukp.clarin.webanno.security.model.Authority;
import de.tudarmstadt.ukp.clarin.webanno.security.model.User;
import de.tudarmstadt.ukp.clarin.webanno.support.ZipUtils;
import de.tudarmstadt.ukp.clarin.webanno.support.logging.Logging;

@Component(ProjectService.SERVICE_NAME)
public class ProjectServiceImpl
    implements ProjectService, SmartLifecycle
{
    private final Logger log = LoggerFactory.getLogger(getClass());

    private @PersistenceContext EntityManager entityManager;
    private @Autowired UserDao userRepository;
    private @Autowired ApplicationEventPublisher applicationEventPublisher;

    @Value(value = "${repository.path}")
    private File dir;

    // The annotation preference properties File name
    private static final String annotationPreferencePropertiesFileName = "annotation.properties";

    private boolean running = false;

    private List<ProjectType> projectTypes;
    
    public ProjectServiceImpl()
    {
        // Nothing to do
    }
    
    @Override
    @Transactional
    public void createProject(Project aProject)
        throws IOException
    {
        if (aProject.getId() != null) {
            throw new IllegalArgumentException("Project has already been created before.");
        }
        
        aProject.setCreated(new Date());
        entityManager.persist(aProject);
        
        try (MDC.MDCCloseable closable = MDC.putCloseable(Logging.KEY_PROJECT_ID,
                String.valueOf(aProject.getId()))) {
            log.info("Created project [{}]({})", aProject.getName(), aProject.getId());
        }
        
        String path = dir.getAbsolutePath() + "/" + PROJECT_FOLDER + "/" + aProject.getId();
        FileUtils.forceMkdir(new File(path));
        
        applicationEventPublisher.publishEvent(new AfterProjectCreatedEvent(this, aProject));
    }

    @Override
    @Transactional
    public void updateProject(Project aProject)
    {
        entityManager.merge(aProject);
    }
    
    @Override
    @Transactional(isolation = Isolation.READ_UNCOMMITTED)
    public void recalculateProjectState(Project aProject)
    {
        Project project;
        try {
            project = getProject(aProject.getId());
        }
        catch (NoResultException e) {
            // This happens when this method is called as part of deleting an entire project.
            // In such a case, the project may no longer be available, so there is no point in
            // updating its state. So then we do nothing here.
            return;
        }
        
        // This query is better because we do not inject strings into the query string, but it
        // does not work on HSQLDB (on MySQL it seems to work).
        // See: https://github.com/webanno/webanno/issues/1011
//        String query = 
//                "SELECT new " + SourceDocumentStateStats.class.getName() + "(" +
//                "COUNT(*) AS num, " +
//                "SUM(CASE WHEN state = :an  THEN 1 ELSE 0 END), " +
//                "SUM(CASE WHEN (state = :aip OR state is NULL) THEN 1 ELSE 0 END), " +
//                "SUM(CASE WHEN state = :af  THEN 1 ELSE 0 END), " +
//                "SUM(CASE WHEN state = :cip THEN 1 ELSE 0 END), " +
//                "SUM(CASE WHEN state = :cf  THEN 1 ELSE 0 END)) " +
//                "FROM SourceDocument " + 
//                "WHERE project = :project";
//        
//        SourceDocumentStateStats stats = entityManager.createQuery(
//                        query, SourceDocumentStateStats.class)
//                .setParameter("project", aProject)
//                .setParameter("an", SourceDocumentState.NEW)
//                .setParameter("aip", SourceDocumentState.ANNOTATION_IN_PROGRESS)
//                .setParameter("af", SourceDocumentState.ANNOTATION_FINISHED)
//                .setParameter("cip", SourceDocumentState.CURATION_IN_PROGRESS)
//                .setParameter("cf", SourceDocumentState.CURATION_FINISHED)
//                .getSingleResult();
        
        String query = 
                "SELECT new " + SourceDocumentStateStats.class.getName() + "(" +
                "COUNT(*), " +
                "SUM(CASE WHEN state = '" + NEW.getId() + "'  THEN 1 ELSE 0 END), " +
                "SUM(CASE WHEN (state = '" + ANNOTATION_IN_PROGRESS.getId() + 
                        "' OR state is NULL) THEN 1 ELSE 0 END), " +
                "SUM(CASE WHEN state = '" + ANNOTATION_FINISHED.getId() + 
                        "'  THEN 1 ELSE 0 END), " +
                "SUM(CASE WHEN state = '" + CURATION_IN_PROGRESS.getId() + 
                        "' THEN 1 ELSE 0 END), " +
                "SUM(CASE WHEN state = '" + CURATION_FINISHED.getId() + "'  THEN 1 ELSE 0 END)) " +
                "FROM SourceDocument " + 
                "WHERE project = :project";
        
        SourceDocumentStateStats stats = entityManager.createQuery(
                        query, SourceDocumentStateStats.class)
                .setParameter("project", aProject)
                .getSingleResult();
        
        ProjectState oldState = project.getState();

        // We had some strange reports about being unable to calculate the project state, so to
        // be better able to debug this, we add some more detailed information to the exception
        // message here.
        try {
            project.setState(stats.getProjectState());
        }
        catch (IllegalStateException e) {
            StringBuilder sb = new StringBuilder();
            sb.append("\nDetailed document states in project [" + aProject.getName() + "]("
                    + aProject.getId() + "):\n");
            String detailQuery = "SELECT id, name, state FROM " + SourceDocument.class.getName()
                    + " WHERE project = :project";
            Query q = entityManager.createQuery(detailQuery).setParameter("project", aProject);
            for (Object res : q.getResultList()) {
                sb.append("- ");
                sb.append(Arrays.toString((Object[]) res));
                sb.append('\n');
            }
            IllegalStateException ne = new IllegalStateException(e.getMessage() + sb, e.getCause());
            ne.setStackTrace(e.getStackTrace());
            throw ne;
        }
        
        if (!Objects.equals(oldState, project.getState())) {
            applicationEventPublisher.publishEvent(
                    new ProjectStateChangedEvent(this, project, oldState));
        }
        
        updateProject(project);
    }

    @Override
    @Transactional
    public void createProjectPermission(ProjectPermission aPermission)
    {
        entityManager.persist(aPermission);
        
        try (MDC.MDCCloseable closable = MDC.putCloseable(Logging.KEY_PROJECT_ID,
                String.valueOf(aPermission.getProject().getId()))) {
            log.info("Created permission [{}] for user [{}] on project [{}]({})",
                    aPermission.getLevel(), aPermission.getUser(),
                    aPermission.getProject().getName(), aPermission.getProject().getId());
        }
    }

    @Override
    @Transactional
    public boolean existsProject(String aName)
    {
        String query = 
                "FROM Project " +
                "WHERE name = :name";
        try {
            entityManager
                    .createQuery(query, Project.class)
                    .setParameter("name", aName)
                    .getSingleResult();
            return true;
        }
        catch (NoResultException ex) {
            return false;
        }
    }
    
    @Override
    public boolean existsProjectPermission(User aUser, Project aProject)
    {
        String query =
                "FROM ProjectPermission " + 
                "WHERE user = :user AND project = :project";
        List<ProjectPermission> projectPermissions = entityManager
                .createQuery(query, ProjectPermission.class)
                .setParameter("user", aUser.getUsername())
                .setParameter("project", aProject)
                .getResultList();
        
        // if at least one permission level exist
        if (projectPermissions.size() > 0) {
            return true;
        }
        else {
            return false;
        }
    }

    @Override
    @Transactional
    public boolean existsProjectPermissionLevel(User aUser, Project aProject,
            PermissionLevel aLevel)
    {
        String query =
                "FROM ProjectPermission " + 
                "WHERE user = :user AND project = :project AND level = :level";
        try {
            entityManager
                    .createQuery(query, ProjectPermission.class)
                    .setParameter("user", aUser.getUsername())
                    .setParameter("project", aProject)
                    .setParameter("level", aLevel)
                    .getSingleResult();
            return true;
        }
        catch (NoResultException ex) {
            return false;
        }
    }

    @Override
    @Transactional
    public boolean existsProjectTimeStamp(Project aProject, String aUsername)
    {
        try {
            if (getProjectTimeStamp(aProject, aUsername) == null) {
                return false;
            }
            return true;
        }
        catch (NoResultException ex) {
            return false;
        }
    }

    @Override
    public boolean existsProjectTimeStamp(Project aProject)
    {
        try {
            if (getProjectTimeStamp(aProject) == null) {
                return false;
            }
            return true;
        }
        catch (NoResultException ex) {
            return false;
        }
    }
    
    @Override
    public File getProjectLogFile(Project aProject)
    {
        return new File(dir.getAbsolutePath() + "/" + PROJECT_FOLDER + "/" + "project-"
                + aProject.getId() + ".log");
    }

    @Override
    public File getGuidelinesFolder(Project aProject)
    {
        return new File(dir.getAbsolutePath() + "/" + PROJECT_FOLDER + "/" + aProject.getId() + "/"
                + GUIDELINES_FOLDER + "/");
    }

    @Override
    public File getMetaInfFolder(Project aProject)
    {
        return new File(dir.getAbsolutePath() + "/" + PROJECT_FOLDER + "/" + aProject.getId() + "/"
                + META_INF_FOLDER + "/");
    }

    @Deprecated
    @Override
    @Transactional(noRollbackFor = NoResultException.class)
    public List<Authority> listAuthorities(User aUser)
    {
        return userRepository.listAuthorities(aUser);
    }

    @Override
    public File getGuideline(Project aProject, String aFilename)
    {
        return new File(dir.getAbsolutePath() + "/" + PROJECT_FOLDER + "/" + aProject.getId() + "/"
                + GUIDELINES_FOLDER + "/" + aFilename);
    }

    @Override
    @Transactional(noRollbackFor = NoResultException.class)
    public List<ProjectPermission> listProjectPermissionLevel(User aUser, Project aProject)
    {
        String query = 
                "FROM ProjectPermission " +
                "WHERE user =:user AND project =:project";
        return entityManager
                .createQuery(query, ProjectPermission.class)
                .setParameter("user", aUser.getUsername())
                .setParameter("project", aProject)
                .getResultList();
    }

    @Override
    @Transactional(noRollbackFor = NoResultException.class)
    public List<PermissionLevel> getProjectPermissionLevels(User aUser, Project aProject)
    {
        String query = 
                "SELECT level " +
                "FROM ProjectPermission " +
                "WHERE user = :user AND " + "project = :project";
        try {
            return entityManager
                    .createQuery(query, PermissionLevel.class)
                    .setParameter("user", aUser.getUsername())
                    .setParameter("project", aProject)
                    .getResultList();
        }
        catch (NoResultException e) {
            return Collections.emptyList();
        }
    }
    
    @Override
    @Transactional(noRollbackFor = NoResultException.class)
    public void setProjectPermissionLevels(User aUser, Project aProject,
            Collection<PermissionLevel> aLevels)
    {
        Set<PermissionLevel> levelsToBeGranted = new HashSet<>(aLevels);
        List<ProjectPermission> permissions = new ArrayList<>();
        try {
            permissions.addAll(listProjectPermissionLevel(aUser, aProject));
        }
        catch (NoResultException e) {
            // Nothing to do
        }
        
        // Remove permissions that no longer exist
        for (ProjectPermission permission : permissions) {
            if (!aLevels.contains(permission.getLevel())) {
                removeProjectPermission(permission);
            }
            else {
                levelsToBeGranted.remove(permission.getLevel());
            }
        }
        
        // Grant new permissions
        for (PermissionLevel level : levelsToBeGranted) {
            createProjectPermission(new ProjectPermission(aProject, aUser.getUsername(), level));
        }
    }

    @Override
    public List<User> listProjectUsersWithPermissions(Project aProject)
    {
        String query = 
                "SELECT DISTINCT perm.user " +
                "FROM ProjectPermission AS perm " +
                "WHERE perm.project = :project " +
                "ORDER BY perm.user ASC";
        List<String> usernames = entityManager
                .createQuery(query, String.class)
                .setParameter("project", aProject)
                .getResultList();

        List<User> users = new ArrayList<>();

        for (String username : usernames) {
            if (userRepository.exists(username)) {
                users.add(userRepository.get(username));
            }
        }
        return users;
    }

    @Override
    public List<User> listProjectUsersWithPermissions(Project aProject,
            PermissionLevel aPermissionLevel)
    {
        String query = 
                "SELECT DISTINCT user " +
                "FROM ProjectPermission " +
                "WHERE project = :project AND level = :level " +
                "ORDER BY user ASC";
        List<String> usernames = entityManager
                .createQuery(query, String.class)
                .setParameter("project", aProject)
                .setParameter("level", aPermissionLevel)
                .getResultList();
        List<User> users = new ArrayList<>();
        for (String username : usernames) {
            if (userRepository.exists(username)) {
                users.add(userRepository.get(username));
            }
        }
        return users;
    }

    @Override
    @Transactional
    public Project getProject(String aName)
    {
        String query = 
                "FROM Project " + 
                "WHERE name = :name";
        return entityManager
                .createQuery(query, Project.class)
                .setParameter("name", aName)
                .getSingleResult();
    }

    @Override
    public Project getProject(long aId)
    {
        String query = 
                "FROM Project " +
                "WHERE id = :id";
        return entityManager
                .createQuery(query, Project.class)
                .setParameter("id", aId)
                .getSingleResult();
    }

    @Override
    public void createGuideline(Project aProject, File aContent, String aFileName)
        throws IOException
    {
        try (InputStream is = new FileInputStream(aContent)) {
            createGuideline(aProject, is, aFileName);
        }
    }
    
    @Override
    public void createGuideline(Project aProject, InputStream aIS, String aFileName)
        throws IOException
    {
        String guidelinePath = dir.getAbsolutePath() + "/" + PROJECT_FOLDER + "/" + aProject.getId()
                + "/" + GUIDELINES_FOLDER + "/";
        FileUtils.forceMkdir(new File(guidelinePath));
        copyLarge(aIS, new FileOutputStream(new File(guidelinePath + aFileName)));

        try (MDC.MDCCloseable closable = MDC.putCloseable(Logging.KEY_PROJECT_ID,
                String.valueOf(aProject.getId()))) {
            log.info("Created guidelines file [{}] in project [{}]({})",
                    aFileName, aProject.getName(), aProject.getId());
        }
    }

    @Override
    @Transactional(noRollbackFor = NoResultException.class)
    public List<ProjectPermission> getProjectPermissions(Project aProject)
    {
        String query = 
                "FROM ProjectPermission " +
                "WHERE project = :project";
        return entityManager
                .createQuery(query, ProjectPermission.class)
                .setParameter("project", aProject)
                .getResultList();
    }

    @Override
    @Transactional
    public Date getProjectTimeStamp(Project aProject, String aUsername)
    {
        String query = 
                "SELECT MAX(ann.timestamp) " +
                "FROM AnnotationDocument AS ann " +
                "WHERE ann.project = :project AND ann.user = :user";
        return entityManager
                .createQuery(query, Date.class)
                .setParameter("project", aProject).setParameter("user", aUsername)
                .getSingleResult();
    }

    @Override
    public Date getProjectTimeStamp(Project aProject)
    {
        String query = 
                "SELECT MAX(doc.timestamp) " +
                "FROM SourceDocument AS doc " +
                "WHERE doc.project = :project";
        return entityManager
                .createQuery(query, Date.class)
                .setParameter("project", aProject)
                .getSingleResult();
    }

    @Override
    @Transactional(noRollbackFor = NoResultException.class)
    public List<Project> listProjectsWithFinishedAnnos()
    {
        String query = 
                "SELECT DISTINCT ann.project " +
                "FROM AnnotationDocument AS ann " +
                "WHERE ann.state = :state";
        return entityManager
                .createQuery(query, Project.class)
                .setParameter("state", AnnotationDocumentState.FINISHED)
                .getResultList();
    }

    @Override
    public List<String> listGuidelines(Project aProject)
    {
        // list all guideline files
        File[] files = getGuidelinesFolder(aProject).listFiles();

        // Name of the guideline files
        List<String> annotationGuidelineFiles = new ArrayList<>();
        if (files != null) {
            for (File file : files) {
                annotationGuidelineFiles.add(file.getName());
            }
        }

        return annotationGuidelineFiles;
    }

    @Override
    @Transactional
    public List<Project> listProjects()
    {
        String query = 
                "FROM Project " +
                "ORDER BY name ASC";
        return entityManager
                .createQuery(query, Project.class)
                .getResultList();
    }

    @Override
    public Properties loadUserSettings(String aUsername, Project aProject)
        throws IOException
    {
        Properties property = new Properties();
        property.load(new FileInputStream(new File(dir.getAbsolutePath() + "/" + PROJECT_FOLDER
                + "/" + aProject.getId() + "/" + SETTINGS_FOLDER + "/" + aUsername + "/"
                + annotationPreferencePropertiesFileName)));
        return property;
    }

    @Override
    @Transactional
    public void removeProject(Project aProject)
        throws IOException
    {
        // remove metadata from DB
        Project project = aProject;
        if (!entityManager.contains(project)) {
            project = entityManager.merge(project);
        }
        
        applicationEventPublisher.publishEvent(new BeforeProjectRemovedEvent(this, aProject));

        for (ProjectPermission permissions : getProjectPermissions(aProject)) {
            entityManager.remove(permissions);
        }
                
        entityManager.remove(project);
        
        // remove the project directory from the file system
        String path = dir.getAbsolutePath() + "/" + PROJECT_FOLDER + "/" + aProject.getId();
        try {
            FileUtils.deleteDirectory(new File(path));
        }
        catch (FileNotFoundException e) {
            try (MDC.MDCCloseable closable = MDC.putCloseable(Logging.KEY_PROJECT_ID,
                    String.valueOf(aProject.getId()))) {
                log.info("Project directory to be deleted was not found: [{}]. Ignoring.", path);
            }
        }

        try (MDC.MDCCloseable closable = MDC.putCloseable(Logging.KEY_PROJECT_ID,
                String.valueOf(aProject.getId()))) {
            log.info("Removed project [{}]({})", aProject.getName(), aProject.getId());
        }
    }

    @Override
    public void removeGuideline(Project aProject, String aFileName)
        throws IOException
    {
        FileUtils.forceDelete(new File(dir.getAbsolutePath() + "/" + PROJECT_FOLDER + "/"
                + aProject.getId() + "/" + GUIDELINES_FOLDER + "/" + aFileName));
        
        try (MDC.MDCCloseable closable = MDC.putCloseable(Logging.KEY_PROJECT_ID,
                String.valueOf(aProject.getId()))) {
            log.info("Removed guidelines file [{}] from project [{}]({})", aFileName,
                    aProject.getName(), aProject.getId());
        }
    }

    @Override
    @Transactional
    public void removeProjectPermission(ProjectPermission aPermission)
    {
        entityManager.remove(aPermission);
        
        try (MDC.MDCCloseable closable = MDC.putCloseable(Logging.KEY_PROJECT_ID,
                String.valueOf(aPermission.getProject().getId()))) {
            log.info("Removed permission [{}] for user [{}] on project [{}]({})",
                    aPermission.getLevel(), aPermission.getUser(),
                    aPermission.getProject().getName(), aPermission.getProject().getId());
        }
    }

    @Override
    public void savePropertiesFile(Project aProject, InputStream aIs, String aFileName)
        throws IOException
    {
        String path = dir.getAbsolutePath() + "/" + PROJECT_FOLDER + "/" + aProject.getId() + "/"
                + FilenameUtils.getFullPath(aFileName);
        FileUtils.forceMkdir(new File(path));

        File newTcfFile = new File(path, FilenameUtils.getName(aFileName));
        OutputStream os = null;
        try {
            os = new FileOutputStream(newTcfFile);
            copyLarge(aIs, os);
        }
        finally {
            closeQuietly(os);
            closeQuietly(aIs);
        }
    }

    @Override
    public <T> void saveUserSettings(String aUsername, Project aProject, Mode aSubject,
            T aConfigurationObject)
        throws IOException
    {
        BeanWrapper wrapper = PropertyAccessorFactory.forBeanPropertyAccess(aConfigurationObject);
        Properties props = new Properties();
        for (PropertyDescriptor value : wrapper.getPropertyDescriptors()) {
            if (wrapper.getPropertyValue(value.getName()) == null) {
                continue;
            }
            props.setProperty(aSubject + "." + value.getName(),
                    wrapper.getPropertyValue(value.getName()).toString());
        }
        String propertiesPath = dir.getAbsolutePath() + "/" + PROJECT_FOLDER + "/"
                + aProject.getId() + "/" + SETTINGS_FOLDER + "/" + aUsername;
        // append existing preferences for the other mode
        if (new File(propertiesPath, annotationPreferencePropertiesFileName).exists()) {
            for (Entry<Object, Object> entry : loadUserSettings(aUsername, aProject).entrySet()) {
                String key = entry.getKey().toString();
                // Maintain other Modes of annotations confs than this one
                if (!key.substring(0, key.indexOf(".")).equals(aSubject.toString())) {
                    props.put(entry.getKey(), entry.getValue());
                }
            }
        }
        
//        for (String name : props.stringPropertyNames()) {
//            log.info("{} = {}", name, props.getProperty(name));
//        }
        
        FileUtils.forceMkdir(new File(propertiesPath));
        props.store(new FileOutputStream(new File(propertiesPath,
                annotationPreferencePropertiesFileName)), null);

        try (MDC.MDCCloseable closable = MDC.putCloseable(Logging.KEY_PROJECT_ID,
                String.valueOf(aProject.getId()))) {
            log.info("Saved preferences for user [{}] in project [{}]({})", aUsername,
                    aProject.getName(), aProject.getId());
        }
    }
    
    @Override
    public List<Project> listAccessibleProjects(User user)
    {
        List<Project> allowedProject = new ArrayList<>();
        List<Project> allProjects = listProjects();

        // if global admin, list all projects
        if (SecurityUtil.isSuperAdmin(this, user)) {
            return allProjects;
        }

        // else only list projects where she is admin / user / curator
        for (Project project : allProjects) {
            if (SecurityUtil.isProjectAdmin(project, this, user)
                    || SecurityUtil.isAnnotator(project, this, user)
                    || SecurityUtil.isCurator(project, this, user)) {
                allowedProject.add(project);
            }
        }
        return allowedProject;
    }

    @Override
    public List<Project> listManageableProjects(User user)
    {
        List<Project> allowedProject = new ArrayList<>();
        List<Project> allProjects = listProjects();

        // if global admin, show all projects
        if (SecurityUtil.isSuperAdmin(this, user)) {
            return allProjects;
        }

        // else only projects she is admin of
        for (Project project : allProjects) {
            if (SecurityUtil.isProjectAdmin(project, this, user)) {
                allowedProject.add(project);
            }
        }
        return allowedProject;
    }
    
    @Override
    @Transactional
    public void onProjectImport(ZipFile aZip,
            de.tudarmstadt.ukp.clarin.webanno.export.model.ExportedProject aExportedProject,
            Project aProject)
        throws Exception
    {
        // create project log
        createProjectLog(aZip, aProject);
        
        // create project guideline
        createProjectGuideline(aZip, aProject);
        
        // create project META-INF
        createProjectMetaInf(aZip, aProject);

        // Import project permissions
        createProjectPermission(aExportedProject, aProject);
    }

    /**
     * copy project log files from the exported project
     * @param zip the ZIP file.
     * @param aProject the project.
     * @throws IOException if an I/O error occurs.
     */
    @SuppressWarnings("rawtypes")
    @Deprecated
    private void createProjectLog(ZipFile zip, Project aProject)
        throws IOException
    {
        for (Enumeration zipEnumerate = zip.entries(); zipEnumerate.hasMoreElements();) {
            ZipEntry entry = (ZipEntry) zipEnumerate.nextElement();

            // Strip leading "/" that we had in ZIP files prior to 2.0.8 (bug #985)
            String entryName = ZipUtils.normalizeEntryName(entry);
            
            if (entryName.startsWith(LOG_FOLDER + "/")) {
                FileUtils.copyInputStreamToFile(zip.getInputStream(entry),
                        getProjectLogFile(aProject));
                log.info("Imported log for project [" + aProject.getName() + "] with id ["
                        + aProject.getId() + "]");
            }
        }
    }
    
    /**
     * copy guidelines from the exported project
     * @param zip the ZIP file.
     * @param aProject the project.
     * @throws IOException if an I/O error occurs.
     */
    @Deprecated
    @SuppressWarnings("rawtypes")
    private void createProjectGuideline(ZipFile zip, Project aProject)
        throws IOException
    {
        for (Enumeration zipEnumerate = zip.entries(); zipEnumerate.hasMoreElements();) {
            ZipEntry entry = (ZipEntry) zipEnumerate.nextElement();
            
            // Strip leading "/" that we had in ZIP files prior to 2.0.8 (bug #985)
            String entryName = ZipUtils.normalizeEntryName(entry);
            
            if (entryName.startsWith(GUIDELINES_FOLDER + "/")) {
                String fileName = FilenameUtils.getName(entry.getName());
                if (fileName.trim().isEmpty()) {
                    continue;
                }
                File guidelineDir = getGuidelinesFolder(aProject);
                FileUtils.forceMkdir(guidelineDir);
                FileUtils.copyInputStreamToFile(zip.getInputStream(entry), new File(guidelineDir,
                        fileName));
                
                log.info("Imported guideline [" + fileName + "] for project [" + aProject.getName()
                        + "] with id [" + aProject.getId() + "]");
            }
        }
    }
    
    /**
     * copy Project META_INF from the exported project
     * @param zip the ZIP file.
     * @param aProject the project.
     * @throws IOException if an I/O error occurs.
     */
    @Deprecated
    @SuppressWarnings("rawtypes")
    private void createProjectMetaInf(ZipFile zip, Project aProject)
        throws IOException
    {
        for (Enumeration zipEnumerate = zip.entries(); zipEnumerate.hasMoreElements();) {
            ZipEntry entry = (ZipEntry) zipEnumerate.nextElement();

            // Strip leading "/" that we had in ZIP files prior to 2.0.8 (bug #985)
            String entryName = ZipUtils.normalizeEntryName(entry);

            if (entryName.startsWith(META_INF_FOLDER + "/")) {
                File metaInfDir = new File(getMetaInfFolder(aProject),
                        FilenameUtils.getPath(entry.getName().replace(META_INF_FOLDER + "/", "")));
                // where the file reside in the META-INF/... directory
                FileUtils.forceMkdir(metaInfDir);
                FileUtils.copyInputStreamToFile(zip.getInputStream(entry), new File(metaInfDir,
                        FilenameUtils.getName(entry.getName())));
                
                log.info("Imported META-INF for project [" + aProject.getName() + "] with id ["
                        + aProject.getId() + "]");
            }
        }
    }

    /**
     * Create {@link ProjectPermission} from the exported
     * {@link de.tudarmstadt.ukp.clarin.webanno.export.model.ExportedProjectPermission}
     * 
     * @param aImportedProjectSetting
     *            the imported project.
     * @param aImportedProject
     *            the project.
     * @throws IOException
     *             if an I/O error occurs.
     */
    @Deprecated
    private void createProjectPermission(
            de.tudarmstadt.ukp.clarin.webanno.export.model.ExportedProject aImportedProjectSetting,
            Project aImportedProject)
        throws IOException
    {
        for (ExportedProjectPermission importedPermission : aImportedProjectSetting
                .getProjectPermissions()) {
            ProjectPermission permission = new ProjectPermission();
            permission.setLevel(importedPermission.getLevel());
            permission.setProject(aImportedProject);
            permission.setUser(importedPermission.getUser());
            createProjectPermission(permission);
        }
    }
    
    @Override
    public boolean isRunning()
    {
        return running;
    }

    @Override
    public void start()
    {
        running = true;
        scanProjectTypes();
    }

    @Override
    public void stop()
    {
        running = false;
    }

    @Override
    public int getPhase()
    {
        return Integer.MAX_VALUE;
    }

    @Override
    public boolean isAutoStartup()
    {
        return true;
    }

    @Override
    public void stop(Runnable aCallback)
    {
        stop();
        aCallback.run();
    }

    private void scanProjectTypes()
    {
        projectTypes = new ArrayList<>();

        // Scan for project type annotations
        ClassPathScanningCandidateComponentProvider scanner = 
                new ClassPathScanningCandidateComponentProvider(false);
        scanner.addIncludeFilter(new AnnotationTypeFilter(ProjectType.class));

        for (BeanDefinition bd : scanner.findCandidateComponents("de.tudarmstadt.ukp")) {
            try {
                Class<?> clazz = Class.forName(bd.getBeanClassName());
                ProjectType pt = clazz.getAnnotation(ProjectType.class);

                if (projectTypes.stream().anyMatch(t -> t.id().equals(pt.id()))) {
                    log.debug("Ignoring duplicate project type: {} ({})", pt.id(), pt.prio());
                }
                else {
                    log.debug("Found project type: {} ({})", pt.id(), pt.prio());
                    projectTypes.add(pt);
                }
            }
            catch (ClassNotFoundException e) {
                log.error("Class [{}] not found", bd.getBeanClassName(), e);
            }
        }
        projectTypes.sort(comparingInt(ProjectType::prio));
    }

    @Override
    public List<ProjectType> listProjectTypes()
    {
        return Collections.unmodifiableList(projectTypes);
    }
}

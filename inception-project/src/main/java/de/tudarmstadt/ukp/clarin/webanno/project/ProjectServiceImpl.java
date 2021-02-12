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
package de.tudarmstadt.ukp.clarin.webanno.project;

import static de.tudarmstadt.ukp.clarin.webanno.model.SourceDocumentState.ANNOTATION_FINISHED;
import static de.tudarmstadt.ukp.clarin.webanno.model.SourceDocumentState.ANNOTATION_IN_PROGRESS;
import static de.tudarmstadt.ukp.clarin.webanno.model.SourceDocumentState.CURATION_FINISHED;
import static de.tudarmstadt.ukp.clarin.webanno.model.SourceDocumentState.CURATION_IN_PROGRESS;
import static de.tudarmstadt.ukp.clarin.webanno.model.SourceDocumentState.NEW;
import static java.nio.file.Files.newDirectoryStream;
import static java.util.Arrays.asList;
import static java.util.stream.Collectors.toList;
import static org.apache.commons.io.IOUtils.closeQuietly;
import static org.apache.commons.io.IOUtils.copyLarge;
import static org.apache.commons.lang3.time.DurationFormatUtils.formatDurationWords;
import static org.hibernate.annotations.QueryHints.CACHEABLE;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.DirectoryStream;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.Deque;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import javax.persistence.EntityManager;
import javax.persistence.NoResultException;
import javax.persistence.PersistenceContext;
import javax.persistence.Query;

import org.apache.commons.collections4.SetUtils;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang3.ClassUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.SmartLifecycle;
import org.springframework.context.annotation.Lazy;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.context.event.EventListener;
import org.springframework.core.annotation.AnnotationAwareOrderComparator;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

import de.tudarmstadt.ukp.clarin.webanno.api.ProjectService;
import de.tudarmstadt.ukp.clarin.webanno.api.RepositoryProperties;
import de.tudarmstadt.ukp.clarin.webanno.api.event.AfterProjectCreatedEvent;
import de.tudarmstadt.ukp.clarin.webanno.api.event.BeforeProjectRemovedEvent;
import de.tudarmstadt.ukp.clarin.webanno.api.event.ProjectStateChangedEvent;
import de.tudarmstadt.ukp.clarin.webanno.api.project.ProjectInitializer;
import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationDocumentState;
import de.tudarmstadt.ukp.clarin.webanno.model.PermissionLevel;
import de.tudarmstadt.ukp.clarin.webanno.model.Project;
import de.tudarmstadt.ukp.clarin.webanno.model.ProjectPermission;
import de.tudarmstadt.ukp.clarin.webanno.model.ProjectState;
import de.tudarmstadt.ukp.clarin.webanno.model.SourceDocument;
import de.tudarmstadt.ukp.clarin.webanno.security.UserDao;
import de.tudarmstadt.ukp.clarin.webanno.security.model.Authority;
import de.tudarmstadt.ukp.clarin.webanno.security.model.User;
import de.tudarmstadt.ukp.clarin.webanno.support.io.FastIOUtils;
import de.tudarmstadt.ukp.clarin.webanno.support.logging.Logging;

@Component(ProjectService.SERVICE_NAME)
public class ProjectServiceImpl
    implements ProjectService, SmartLifecycle
{
    private final Logger log = LoggerFactory.getLogger(getClass());

    private @PersistenceContext EntityManager entityManager;
    private final UserDao userRepository;
    private final ApplicationEventPublisher applicationEventPublisher;
    private final RepositoryProperties repositoryProperties;
    private final List<ProjectInitializer> initializerProxy;
    private List<ProjectInitializer> initializers;

    private boolean running = false;

    @Autowired
    public ProjectServiceImpl(UserDao aUserRepository,
            ApplicationEventPublisher aApplicationEventPublisher,
            RepositoryProperties aRepositoryProperties,
            @Lazy @Autowired(required = false) List<ProjectInitializer> aInitializerProxy)
    {
        userRepository = aUserRepository;
        applicationEventPublisher = aApplicationEventPublisher;
        repositoryProperties = aRepositoryProperties;
        initializerProxy = aInitializerProxy;
    }

    /**
     * This constructor is used for testing to set specific test objects for fields
     */
    public ProjectServiceImpl(UserDao aUserRepository,
            ApplicationEventPublisher aApplicationEventPublisher,
            RepositoryProperties aRepositoryProperties, List<ProjectInitializer> aInitializerProxy,
            EntityManager aEntityManager)
    {
        this(aUserRepository, aApplicationEventPublisher, aRepositoryProperties, aInitializerProxy);
        entityManager = aEntityManager;
    }

    @Override
    @Transactional
    public void createProject(Project aProject) throws IOException
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

        String path = repositoryProperties.getPath().getAbsolutePath() + "/" + PROJECT_FOLDER + "/"
                + aProject.getId();
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
        // String query =
        // "SELECT new " + SourceDocumentStateStats.class.getName() + "(" +
        // "COUNT(*) AS num, " +
        // "SUM(CASE WHEN state = :an THEN 1 ELSE 0 END), " +
        // "SUM(CASE WHEN (state = :aip OR state is NULL) THEN 1 ELSE 0 END), " +
        // "SUM(CASE WHEN state = :af THEN 1 ELSE 0 END), " +
        // "SUM(CASE WHEN state = :cip THEN 1 ELSE 0 END), " +
        // "SUM(CASE WHEN state = :cf THEN 1 ELSE 0 END)) " +
        // "FROM SourceDocument " +
        // "WHERE project = :project";
        //
        // SourceDocumentStateStats stats = entityManager.createQuery(
        // query, SourceDocumentStateStats.class)
        // .setParameter("project", aProject)
        // .setParameter("an", SourceDocumentState.NEW)
        // .setParameter("aip", SourceDocumentState.ANNOTATION_IN_PROGRESS)
        // .setParameter("af", SourceDocumentState.ANNOTATION_FINISHED)
        // .setParameter("cip", SourceDocumentState.CURATION_IN_PROGRESS)
        // .setParameter("cf", SourceDocumentState.CURATION_FINISHED)
        // .getSingleResult();

        // @formatter:off
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
        // @formatter:on

        SourceDocumentStateStats stats = entityManager
                .createQuery(query, SourceDocumentStateStats.class)
                .setParameter("project", aProject).getSingleResult();

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
            applicationEventPublisher
                    .publishEvent(new ProjectStateChangedEvent(this, project, oldState));
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
        String query = "FROM Project " + "WHERE name = :name";
        try {
            entityManager.createQuery(query, Project.class).setParameter("name", aName)
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
        String query = "FROM ProjectPermission " + "WHERE user = :user AND project = :project";
        List<ProjectPermission> projectPermissions = entityManager
                .createQuery(query, ProjectPermission.class)
                .setParameter("user", aUser.getUsername()).setParameter("project", aProject)
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
        String query = "FROM ProjectPermission "
                + "WHERE user = :user AND project = :project AND level = :level";
        try {
            entityManager.createQuery(query, ProjectPermission.class)
                    .setParameter("user", aUser.getUsername()).setParameter("project", aProject)
                    .setParameter("level", aLevel).getSingleResult();
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
    public File getProjectFolder(Project aProject)
    {
        return new File(repositoryProperties.getPath().getAbsolutePath() + "/" + PROJECT_FOLDER
                + "/" + aProject.getId());
    }

    @Override
    public File getProjectLogFile(Project aProject)
    {
        return new File(repositoryProperties.getPath().getAbsolutePath() + "/" + PROJECT_FOLDER
                + "/" + "project-" + aProject.getId() + ".log");
    }

    @Override
    public File getGuidelinesFolder(Project aProject)
    {
        return new File(repositoryProperties.getPath().getAbsolutePath() + "/" + PROJECT_FOLDER
                + "/" + aProject.getId() + "/" + GUIDELINES_FOLDER + "/");
    }

    @Override
    public File getMetaInfFolder(Project aProject)
    {
        return new File(repositoryProperties.getPath().getAbsolutePath() + "/" + PROJECT_FOLDER
                + "/" + aProject.getId() + "/" + META_INF_FOLDER + "/");
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
        return new File(repositoryProperties.getPath().getAbsolutePath() + "/" + PROJECT_FOLDER
                + "/" + aProject.getId() + "/" + GUIDELINES_FOLDER + "/" + aFilename);
    }

    @Override
    @Transactional(noRollbackFor = NoResultException.class)
    public List<ProjectPermission> listProjectPermissionLevel(User aUser, Project aProject)
    {
        String query = String.join("\n", //
                "FROM ProjectPermission ", //
                "WHERE user =:user AND project =:project ", //
                "ORDER BY level");

        return entityManager.createQuery(query, ProjectPermission.class) //
                .setParameter("user", aUser.getUsername()) //
                .setParameter("project", aProject) //
                .setHint(CACHEABLE, true) //
                .getResultList();
    }

    @Override
    @Transactional
    public boolean hasRole(User aUser, Project aProject, PermissionLevel... aRoles)
    {
        if (aRoles == null || aRoles.length == 0) {
            String query = String.join("\n", //
                    "SELECT COUNT(*) FROM ProjectPermission ", //
                    "WHERE user = :user AND project = :project", //
                    "ORDER BY level");

            return entityManager.createQuery(query, Long.class) //
                    .setParameter("user", aUser.getUsername()) //
                    .setParameter("project", aProject) //
                    .getSingleResult() > 0;

        }

        String query = String.join("\n", //
                "SELECT COUNT(*) FROM ProjectPermission ", //
                "WHERE user = :user AND project = :project AND level IN (:roles)", //
                "ORDER BY level");

        return entityManager.createQuery(query, Long.class) //
                .setParameter("user", aUser.getUsername()) //
                .setParameter("project", aProject) //
                .setParameter("roles", asList(aRoles)) //
                .getSingleResult() > 0;
    }

    @Override
    @Transactional(noRollbackFor = NoResultException.class)
    public List<PermissionLevel> getProjectPermissionLevels(User aUser, Project aProject)
    {
        String query = String.join("\n", //
                "SELECT level", //
                "FROM ProjectPermission", //
                "WHERE user = :user AND", //
                "      project = :project");

        try {
            return entityManager.createQuery(query, PermissionLevel.class) //
                    .setParameter("user", aUser.getUsername()) //
                    .setParameter("project", aProject) //
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
        String query = "SELECT DISTINCT u FROM User u, ProjectPermission pp "
                + "WHERE pp.user = u.username " + "AND pp.project = :project "
                + "ORDER BY u.username ASC";
        List<User> users = entityManager.createQuery(query, User.class)
                .setParameter("project", aProject).getResultList();
        return users;
    }

    @Override
    public List<User> listProjectUsersWithPermissions(Project aProject,
            PermissionLevel aPermissionLevel)
    {
        String query = "SELECT DISTINCT u FROM User u, ProjectPermission pp "
                + "WHERE pp.user = u.username " + "AND pp.project = :project AND pp.level = :level "
                + "ORDER BY u.username ASC";
        List<User> users = entityManager.createQuery(query, User.class)
                .setParameter("project", aProject).setParameter("level", aPermissionLevel)
                .getResultList();
        return users;
    }

    @Override
    @Transactional
    public Project getProject(String aName)
    {
        String query = "FROM Project " + "WHERE name = :name";
        return entityManager.createQuery(query, Project.class).setParameter("name", aName)
                .getSingleResult();
    }

    @Override
    public Project getProject(long aId)
    {
        String query = "FROM Project " + "WHERE id = :id";
        return entityManager.createQuery(query, Project.class).setParameter("id", aId)
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
        String guidelinePath = repositoryProperties.getPath().getAbsolutePath() + "/"
                + PROJECT_FOLDER + "/" + aProject.getId() + "/" + GUIDELINES_FOLDER + "/";
        FileUtils.forceMkdir(new File(guidelinePath));
        copyLarge(aIS, new FileOutputStream(new File(guidelinePath + aFileName)));

        try (MDC.MDCCloseable closable = MDC.putCloseable(Logging.KEY_PROJECT_ID,
                String.valueOf(aProject.getId()))) {
            log.info("Created guidelines file [{}] in project [{}]({})", aFileName,
                    aProject.getName(), aProject.getId());
        }
    }

    @Override
    @Transactional(noRollbackFor = NoResultException.class)
    public List<ProjectPermission> getProjectPermissions(Project aProject)
    {
        String query = "FROM ProjectPermission " + "WHERE project = :project";
        return entityManager.createQuery(query, ProjectPermission.class)
                .setParameter("project", aProject).getResultList();
    }

    @Override
    @Transactional
    public Date getProjectTimeStamp(Project aProject, String aUsername)
    {
        String query = "SELECT MAX(ann.timestamp) " + "FROM AnnotationDocument AS ann "
                + "WHERE ann.project = :project AND ann.user = :user";
        return entityManager.createQuery(query, Date.class).setParameter("project", aProject)
                .setParameter("user", aUsername).getSingleResult();
    }

    @Override
    public Date getProjectTimeStamp(Project aProject)
    {
        String query = "SELECT MAX(doc.timestamp) " + "FROM SourceDocument AS doc "
                + "WHERE doc.project = :project";
        return entityManager.createQuery(query, Date.class).setParameter("project", aProject)
                .getSingleResult();
    }

    @Override
    @Transactional(noRollbackFor = NoResultException.class)
    public List<Project> listProjectsWithFinishedAnnos()
    {
        String query = "SELECT DISTINCT ann.project " + "FROM AnnotationDocument AS ann "
                + "WHERE ann.state = :state";
        return entityManager.createQuery(query, Project.class)
                .setParameter("state", AnnotationDocumentState.FINISHED).getResultList();
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
    public boolean hasGuidelines(Project aProject)
    {
        try (DirectoryStream<Path> d = newDirectoryStream(getGuidelinesFolder(aProject).toPath())) {
            return d.iterator().hasNext();
        }
        catch (IOException e) {
            // This may not be the best way to handle it, but if is a fairly sound assertion and
            // saves the calling code from having to handle the exception.
            return false;
        }
    }

    @Override
    @Transactional
    public List<Project> listProjects()
    {
        String query = "FROM Project " + "ORDER BY name ASC";
        return entityManager.createQuery(query, Project.class).getResultList();
    }

    @Override
    @Transactional
    public void removeProject(Project aProject) throws IOException
    {
        long start = System.currentTimeMillis();

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
        String path = repositoryProperties.getPath().getAbsolutePath() + "/" + PROJECT_FOLDER + "/"
                + aProject.getId();
        try {
            FastIOUtils.delete(new File(path));
        }
        catch (FileNotFoundException | NoSuchFileException e) {
            try (MDC.MDCCloseable closable = MDC.putCloseable(Logging.KEY_PROJECT_ID,
                    String.valueOf(aProject.getId()))) {
                log.info("Project directory to be deleted was not found: [{}]. Ignoring.", path);
            }
        }

        try (MDC.MDCCloseable closable = MDC.putCloseable(Logging.KEY_PROJECT_ID,
                String.valueOf(aProject.getId()))) {
            log.info("Removed project [{}]({}) ({})", aProject.getName(), aProject.getId(),
                    formatDurationWords(System.currentTimeMillis() - start, true, true));
        }
    }

    @Override
    public void removeGuideline(Project aProject, String aFileName) throws IOException
    {
        FileUtils.forceDelete(
                new File(repositoryProperties.getPath().getAbsolutePath() + "/" + PROJECT_FOLDER
                        + "/" + aProject.getId() + "/" + GUIDELINES_FOLDER + "/" + aFileName));

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
        String path = repositoryProperties.getPath().getAbsolutePath() + "/" + PROJECT_FOLDER + "/"
                + aProject.getId() + "/" + FilenameUtils.getFullPath(aFileName);
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
    public List<Project> listAccessibleProjects(User user)
    {
        List<Project> allowedProject = new ArrayList<>();
        List<Project> allProjects = listProjects();

        // if global admin, list all projects
        if (userRepository.isAdministrator(user)) {
            return allProjects;
        }

        // else only list projects where she is admin / user / curator
        for (Project project : allProjects) {
            if (this.isManager(project, user) || this.isAnnotator(project, user)
                    || this.isCurator(project, user)) {
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
        if (userRepository.isAdministrator(user)) {
            return allProjects;
        }

        // else only projects she is admin of
        for (Project project : allProjects) {
            if (this.isManager(project, user)) {
                allowedProject.add(project);
            }
        }
        return allowedProject;
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

    @Override
    public boolean managesAnyProject(User user)
    {
        if (userRepository.isAdministrator(user)) {
            return true;
        }

        if (userRepository.isProjectCreator(user)) {
            return true;
        }

        for (Project project : listProjects()) {
            if (isManager(project, user)) {
                return true;
            }
        }

        return false;
    }

    @Override
    public boolean isManager(Project aProject, User aUser)
    {
        boolean projectAdmin = false;
        try {
            List<ProjectPermission> permissionLevels = listProjectPermissionLevel(aUser, aProject);
            for (ProjectPermission permissionLevel : permissionLevels) {
                if (StringUtils.equalsIgnoreCase(permissionLevel.getLevel().getName(),
                        PermissionLevel.MANAGER.getName())) {
                    projectAdmin = true;
                    break;
                }
            }
        }
        catch (NoResultException ex) {
            log.info("No permision is given to this user " + ex);
        }

        return projectAdmin;
    }

    @Override
    @Deprecated
    public boolean isAdmin(Project aProject, User aUser)
    {
        return isManager(aProject, aUser);
    }

    @Override
    public boolean isCurator(Project aProject, User aUser)
    {
        boolean curator = false;
        try {
            List<ProjectPermission> permissionLevels = listProjectPermissionLevel(aUser, aProject);
            for (ProjectPermission permissionLevel : permissionLevels) {
                if (StringUtils.equalsIgnoreCase(permissionLevel.getLevel().getName(),
                        PermissionLevel.CURATOR.getName())) {
                    curator = true;
                    break;
                }
            }
        }
        catch (NoResultException ex) {
            log.info("No permision is given to this user " + ex);
        }

        return curator;
    }

    @Override
    public boolean isAnnotator(Project aProject, User aUser)
    {
        boolean user = false;
        try {
            List<ProjectPermission> permissionLevels = listProjectPermissionLevel(aUser, aProject);
            for (ProjectPermission permissionLevel : permissionLevels) {
                if (StringUtils.equalsIgnoreCase(permissionLevel.getLevel().getName(),
                        PermissionLevel.ANNOTATOR.getName())) {
                    user = true;
                    break;
                }
            }
        }

        catch (NoResultException ex) {
            log.info("No permision is given to this user " + ex);
        }

        return user;
    }

    @EventListener
    public void onContextRefreshedEvent(ContextRefreshedEvent aEvent)
    {
        init();
    }

    /* package private */ void init()
    {
        List<ProjectInitializer> inits = new ArrayList<>();

        if (initializerProxy != null) {
            inits.addAll(initializerProxy);
            AnnotationAwareOrderComparator.sort(inits);

            Set<Class<? extends ProjectInitializer>> initializerClasses = new HashSet<>();
            for (ProjectInitializer init : inits) {
                if (initializerClasses.add(init.getClass())) {
                    log.info("Found project initializer: {}",
                            ClassUtils.getAbbreviatedName(init.getClass(), 20));
                }
                else {
                    throw new IllegalStateException("There cannot be more than once instance "
                            + "of each project initializer class! Duplicate instance of class: "
                            + init.getClass());
                }
            }
        }

        initializers = Collections.unmodifiableList(inits);
    }

    @Override
    public List<ProjectInitializer> listProjectInitializers()
    {
        return initializers;
    }

    @Override
    @Transactional
    public void initializeProject(Project aProject) throws IOException
    {
        initializeProject(aProject, initializers.stream() //
                .filter(ProjectInitializer::applyByDefault) //
                .collect(Collectors.toList()));
    }

    private ProjectInitializer findProjectInitializer(Class<? extends ProjectInitializer> aType)
    {
        return initializers.stream().filter(i -> aType.isAssignableFrom(i.getClass())) //
                .findFirst() //
                .orElseThrow(() -> new IllegalArgumentException(
                        "No initializer of type [" + aType + "] exists!"));
    }

    private Set<ProjectInitializer> collectDependencies(List<ProjectInitializer> aInitializers)
    {
        Set<ProjectInitializer> seen = new LinkedHashSet<>();

        Deque<ProjectInitializer> deque = new LinkedList<>(aInitializers);
        while (!deque.isEmpty()) {
            ProjectInitializer initializer = deque.poll();

            if (seen.contains(initializer)) {
                continue;
            }

            seen.add(initializer);

            for (Class<? extends ProjectInitializer> depClass : initializer.getDependencies()) {
                deque.add(findProjectInitializer(depClass));
            }
        }

        return seen;
    }

    @Override
    @Transactional
    public void initializeProject(Project aProject, List<ProjectInitializer> aInitializers)
        throws IOException
    {
        Set<Class<? extends ProjectInitializer>> allInits = new HashSet<>();
        Set<Class<? extends ProjectInitializer>> applied = new HashSet<>();
        for (ProjectInitializer initializer : initializers) {
            allInits.add(initializer.getClass());
            if (initializer.alreadyApplied(aProject)) {
                applied.add(initializer.getClass());
            }
        }

        Deque<ProjectInitializer> toApply = new LinkedList<>(collectDependencies(aInitializers));
        Set<ProjectInitializer> initsDeferred = SetUtils.newIdentityHashSet();
        while (!toApply.isEmpty()) {
            ProjectInitializer initializer = toApply.pop();
            String initializerName = initializer.getName();

            if (applied.contains(initializer.getClass())) {
                log.debug("Skipping project initializer that was already applied: [{}]",
                        initializerName);
                continue;
            }

            if (!allInits.containsAll(initializer.getDependencies())) {
                throw new IllegalStateException("Missing dependencies of [" + initializerName
                        + "] initializer from "
                        + toApply.stream().map(ProjectInitializer::getName).collect(toList()));
            }

            if (initsDeferred.contains(initializer)) {
                throw new IllegalStateException("Circular initializer dependencies in "
                        + initsDeferred.stream().map(ProjectInitializer::getName).collect(toList())
                        + " via [" + initializerName + "]");
            }

            if (applied.containsAll(initializer.getDependencies())) {
                log.debug("Applying project initializer: [{}]", initializerName);
                initializer.configure(aProject);
                applied.add(initializer.getClass());
                initsDeferred.clear();
            }
            else {
                log.debug("Deferring project initializer as dependencies are not yet fulfilled: {}",
                        initializer);
                toApply.add(initializer);
                initsDeferred.add(initializer);
            }
        }
    }

    @Override
    public List<Project> listProjectsForAgreement()
    {
        String query = "SELECT DISTINCT p FROM Project p, ProjectPermission pp "
                + "WHERE pp.project = p.id " + "AND pp.level = :annotator "
                + "GROUP BY p.id HAVING count(*) > 1 " + "ORDER BY p.name ASC";
        List<Project> projects = entityManager.createQuery(query, Project.class)
                .setParameter("annotator", PermissionLevel.ANNOTATOR).getResultList();
        return projects;
    }

    @Override
    public List<Project> listManageableCuratableProjects(User aUser)
    {
        String query = "SELECT DISTINCT p FROM Project p, ProjectPermission pp "
                + "WHERE pp.project = p.id "
                + "AND pp.user = :username AND (pp.level = :curator OR pp.level = :manager)"
                + "ORDER BY p.name ASC";
        List<Project> projects = entityManager.createQuery(query, Project.class)
                .setParameter("username", aUser.getUsername())
                .setParameter("curator", PermissionLevel.CURATOR)
                .setParameter("manager", PermissionLevel.MANAGER).getResultList();
        return projects;
    }
}

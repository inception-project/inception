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

import static de.tudarmstadt.ukp.clarin.webanno.model.PermissionLevel.MANAGER;
import static de.tudarmstadt.ukp.clarin.webanno.model.Project.MAX_PROJECT_SLUG_LENGTH;
import static de.tudarmstadt.ukp.clarin.webanno.model.Project.MIN_PROJECT_SLUG_LENGTH;
import static de.tudarmstadt.ukp.clarin.webanno.model.Project.isValidProjectSlug;
import static de.tudarmstadt.ukp.clarin.webanno.model.Project.isValidProjectSlugInitialCharacter;
import static de.tudarmstadt.ukp.clarin.webanno.security.UserDao.EMPTY_PASSWORD;
import static de.tudarmstadt.ukp.clarin.webanno.security.model.Role.ROLE_USER;
import static de.tudarmstadt.ukp.inception.project.api.ProjectService.withProjectLogger;
import static java.lang.Math.min;
import static java.lang.String.join;
import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static java.util.Collections.unmodifiableList;
import static java.util.stream.Collectors.groupingBy;
import static java.util.stream.Collectors.toCollection;
import static java.util.stream.Collectors.toList;
import static org.apache.commons.io.IOUtils.copyLarge;
import static org.apache.commons.lang3.StringUtils.startsWith;
import static org.apache.commons.lang3.StringUtils.substringAfter;
import static org.apache.commons.lang3.time.DurationFormatUtils.formatDurationWords;
import static org.hibernate.annotations.QueryHints.CACHEABLE;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.invoke.MethodHandles;
import java.nio.file.NoSuchFileException;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Random;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.commons.collections4.SetUtils;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang3.ClassUtils;
import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.Validate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.context.event.EventListener;
import org.springframework.core.annotation.AnnotationAwareOrderComparator;
import org.springframework.transaction.annotation.Transactional;

import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationDocumentState;
import de.tudarmstadt.ukp.clarin.webanno.model.PermissionLevel;
import de.tudarmstadt.ukp.clarin.webanno.model.Project;
import de.tudarmstadt.ukp.clarin.webanno.model.ProjectPermission;
import de.tudarmstadt.ukp.clarin.webanno.model.ProjectPermission_;
import de.tudarmstadt.ukp.clarin.webanno.model.ProjectState;
import de.tudarmstadt.ukp.clarin.webanno.model.ProjectUserPermissions;
import de.tudarmstadt.ukp.clarin.webanno.model.Project_;
import de.tudarmstadt.ukp.clarin.webanno.project.config.ProjectServiceAutoConfiguration;
import de.tudarmstadt.ukp.clarin.webanno.security.Realm;
import de.tudarmstadt.ukp.clarin.webanno.security.UserDao;
import de.tudarmstadt.ukp.clarin.webanno.security.model.User;
import de.tudarmstadt.ukp.inception.documents.api.RepositoryProperties;
import de.tudarmstadt.ukp.inception.project.api.FeatureInitializer;
import de.tudarmstadt.ukp.inception.project.api.ProjectInitializationRequest;
import de.tudarmstadt.ukp.inception.project.api.ProjectInitializer;
import de.tudarmstadt.ukp.inception.project.api.ProjectService;
import de.tudarmstadt.ukp.inception.project.api.event.AfterProjectCreatedEvent;
import de.tudarmstadt.ukp.inception.project.api.event.AfterProjectRemovedEvent;
import de.tudarmstadt.ukp.inception.project.api.event.BeforeProjectRemovedEvent;
import de.tudarmstadt.ukp.inception.project.api.event.ProjectPermissionsChangedEvent;
import de.tudarmstadt.ukp.inception.project.api.event.ProjectStateChangedEvent;
import de.tudarmstadt.ukp.inception.support.io.FastIOUtils;
import de.tudarmstadt.ukp.inception.support.logging.BaseLoggers;
import jakarta.persistence.EntityManager;
import jakarta.persistence.NoResultException;

/**
 * <p>
 * This class is exposed as a Spring Component via
 * {@link ProjectServiceAutoConfiguration#projectService}.
 * </p>
 */
public class ProjectServiceImpl
    implements ProjectService
{
    private static final Logger LOG = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    public static final int RANDOM_USERNAME_BYTE_LENGTH = 16;

    private final EntityManager entityManager;
    private final UserDao userRepository;
    private final ApplicationEventPublisher applicationEventPublisher;
    private final RepositoryProperties repositoryProperties;
    private final List<ProjectInitializer> projectInitializerProxy;
    private final List<FeatureInitializer> featureInitializerProxy;
    private final SecureRandom random;

    private List<ProjectInitializer> projectInitializers;
    private List<FeatureInitializer> featureInitializers;

    public ProjectServiceImpl(UserDao aUserRepository,
            ApplicationEventPublisher aApplicationEventPublisher,
            RepositoryProperties aRepositoryProperties,
            List<ProjectInitializer> aProjectInitializerProxy,
            List<FeatureInitializer> aFeatureInitializerProxy, EntityManager aEntityManager)
    {
        entityManager = aEntityManager;
        userRepository = aUserRepository;
        applicationEventPublisher = aApplicationEventPublisher;
        repositoryProperties = aRepositoryProperties;
        projectInitializerProxy = aProjectInitializerProxy;
        featureInitializerProxy = aFeatureInitializerProxy;
        random = new SecureRandom();
    }

    @Override
    @Transactional
    public Project createProject(Project aProject) throws IOException
    {
        if (aProject.getId() != null) {
            throw new IllegalArgumentException("Project has already been created before.");
        }

        aProject.setCreated(new Date());
        entityManager.persist(aProject);

        try (var logCtx = withProjectLogger(aProject)) {
            LOG.info("Created project {}", aProject);

            var path = repositoryProperties.getPath().getAbsolutePath() + "/" + PROJECT_FOLDER + "/"
                    + aProject.getId();
            FileUtils.forceMkdir(new File(path));

            applicationEventPublisher.publishEvent(new AfterProjectCreatedEvent(this, aProject));
        }

        return aProject;
    }

    @Override
    @Transactional
    public void updateProject(Project aProject)
    {
        entityManager.merge(aProject);
    }

    @Deprecated
    @Override
    @Transactional
    public void createProjectPermission(ProjectPermission aPermission)
    {
        try (var logCtx = withProjectLogger(aPermission.getProject())) {
            entityManager.persist(aPermission);

            LOG.info("Created permission [{}] for user [{}] on project {}", aPermission.getLevel(),
                    aPermission.getUser(), aPermission.getProject());

            applicationEventPublisher.publishEvent(new ProjectPermissionsChangedEvent(this,
                    aPermission.getProject(), asList(aPermission), emptyList()));
        }
    }

    @Override
    @Transactional(readOnly = true)
    public boolean existsProjectWithName(String aName)
    {
        String query = "FROM Project WHERE name = :name";
        try {
            entityManager.createQuery(query, Project.class) //
                    .setParameter("name", aName) //
                    .getSingleResult();
            return true;
        }
        catch (NoResultException ex) {
            return false;
        }
    }

    @Override
    @Transactional(readOnly = true)
    public boolean existsProjectWithSlug(String aSlug)
    {
        String query = "FROM Project WHERE slug = :slug";
        try {
            entityManager.createQuery(query, Project.class) //
                    .setParameter("slug", aSlug) //
                    .getSingleResult();
            return true;
        }
        catch (NoResultException ex) {
            return false;
        }
    }

    @Deprecated
    @Override
    @Transactional(readOnly = true)
    public boolean existsProjectPermission(User aUser, Project aProject)
    {
        return hasAnyRole(aUser, aProject);
    }

    @Override
    @Deprecated
    @Transactional(readOnly = true)
    public boolean existsProjectPermissionLevel(User aUser, Project aProject,
            PermissionLevel aLevel)
    {
        return hasRole(aUser, aProject, aLevel);
    }

    @Override
    public File getProjectLogFile(Project aProject)
    {
        return new File(repositoryProperties.getPath().getAbsolutePath() + "/" + PROJECT_FOLDER
                + "/" + "project-" + aProject.getId() + ".log");
    }

    @Override
    public File getMetaInfFolder(Project aProject)
    {
        return new File(repositoryProperties.getPath().getAbsolutePath() + "/" + PROJECT_FOLDER
                + "/" + aProject.getId() + "/" + META_INF_FOLDER + "/");
    }

    @Override
    @Transactional(readOnly = true)
    public List<PermissionLevel> listRoles(Project aProject, User aUser)
    {
        return listRoles(aProject, aUser.getUsername());
    }

    @Override
    @Transactional(readOnly = true)
    public List<PermissionLevel> listRoles(Project aProject, String aUser)
    {
        var query = join("\n", //
                "SELECT level ", //
                "FROM ProjectPermission ", //
                "WHERE user =:user AND project =:project ", //
                "ORDER BY level");

        return entityManager.createQuery(query, PermissionLevel.class) //
                .setParameter("user", aUser) //
                .setParameter("project", aProject) //
                .getResultList();
    }

    @Override
    @Transactional
    public List<ProjectPermission> listProjectPermissionLevel(User aUser, Project aProject)
    {
        return listProjectPermissionLevel(aUser.getUsername(), aProject);
    }

    @Override
    @Transactional
    public List<ProjectPermission> listProjectPermissionLevel(String aUser, Project aProject)
    {
        String query = join("\n", //
                "FROM ProjectPermission ", //
                "WHERE user =:user AND project =:project ", //
                "ORDER BY user, level");

        return entityManager.createQuery(query, ProjectPermission.class) //
                .setParameter("user", aUser) //
                .setParameter("project", aProject) //
                .setHint(CACHEABLE, true) //
                .getResultList();
    }

    @Override
    @Transactional
    public List<ProjectPermission> listProjectPermissions(User aUser)
    {
        String query = join("\n", //
                "FROM ProjectPermission ", //
                "WHERE user =:user ", //
                "ORDER BY project.name, level");

        return entityManager.createQuery(query, ProjectPermission.class) //
                .setParameter("user", aUser.getUsername()) //
                .setHint(CACHEABLE, true) //
                .getResultList();
    }

    @Override
    @Transactional
    public List<ProjectPermission> listProjectPermissions(Project aProject)
    {
        var query = join("\n", //
                "FROM ProjectPermission ", //
                "WHERE project = :project", //
                "ORDER BY user, level");

        return entityManager.createQuery(query, ProjectPermission.class) //
                .setParameter("project", aProject) //
                .getResultList();
    }

    @Override
    @Transactional
    public List<User> listProjectBoundUsers(Project aProject)
    {
        return userRepository.listAllUsersFromRealm(getRealm(aProject));
    }

    @Override
    @Transactional(readOnly = true)
    public List<ProjectUserPermissions> listProjectUserPermissions(Project aProject)
    {
        var userMap = new HashMap<String, User>();
        listUsersWithAnyRoleInProject(aProject).stream() //
                .forEach(user -> userMap.put(user.getUsername(), user));

        var perUserPermissions = listProjectPermissions(aProject).stream() //
                .collect(groupingBy(ProjectPermission::getUser));

        return perUserPermissions.entrySet().stream() //
                .map(entry -> {
                    var username = entry.getKey();
                    var user = userMap.get(entry.getKey());
                    var roles = entry.getValue().stream() //
                            .map(ProjectPermission::getLevel) //
                            .sorted() //
                            .collect(toCollection(LinkedHashSet::new));
                    return new ProjectUserPermissions(aProject, username, user, roles);
                }) //
                .sorted(this::compareProjectUserPermissions) //
                .collect(toList());
    }

    @Override
    @Transactional(readOnly = true)
    public ProjectUserPermissions getProjectUserPermissions(Project aProject, User aUser)
    {
        // Collect all permissions for the given user in this project
        var roles = listProjectPermissions(aProject).stream()
                .filter(perm -> perm.getUser().equals(aUser.getUsername()))
                .map(ProjectPermission::getLevel) //
                .sorted() //
                .collect(toCollection(LinkedHashSet::new));

        return new ProjectUserPermissions(aProject, aUser.getUsername(), aUser, roles);
    }

    private int compareProjectUserPermissions(ProjectUserPermissions a, ProjectUserPermissions b)
    {
        if (a.getUser().isPresent() && b.getUser().isPresent()) {
            return ObjectUtils.compare(a.getUser().get().getUiName(),
                    b.getUser().get().getUiName());
        }

        if (a.getUser().isPresent() && !b.getUser().isPresent()) {
            return -1;
        }

        if (!a.getUser().isPresent() && b.getUser().isPresent()) {
            return 1;
        }

        return ObjectUtils.compare(a.getUsername(), b.getUsername());
    }

    @Override
    @Transactional(readOnly = true)
    public boolean hasAnyRole(User aUser, Project aProject)
    {
        var query = join("\n", //
                "SELECT COUNT(*) FROM ProjectPermission ", //
                "WHERE user = :user AND project = :project");

        return entityManager.createQuery(query, Long.class) //
                .setParameter("user", aUser.getUsername()) //
                .setParameter("project", aProject) //
                .getSingleResult() > 0;
    }

    @Override
    @Transactional(readOnly = true)
    public boolean hasRole(User aUser, Project aProject, PermissionLevel aRole,
            PermissionLevel... aMoreRoles)
    {
        return hasRole(aUser.getUsername(), aProject, aRole, aMoreRoles);
    }

    @Override
    @Transactional(readOnly = true)
    public boolean hasRole(String aUser, Project aProject, PermissionLevel aRole,
            PermissionLevel... aMoreRoles)
    {
        Validate.notNull(aRole, "hasRole() requires at least one role to check");

        var roles = new LinkedHashSet<>();
        roles.add(aRole);
        if (aMoreRoles != null) {
            roles.addAll(asList(aMoreRoles));
        }

        var query = join("\n", //
                "SELECT COUNT(*) FROM ProjectPermission ", //
                "WHERE user = :user AND project = :project AND level IN (:roles)");

        return entityManager.createQuery(query, Long.class) //
                .setParameter("user", aUser) //
                .setParameter("project", aProject) //
                .setParameter("roles", roles) //
                .getSingleResult() > 0;
    }

    @Override
    @Transactional(readOnly = true)
    public boolean hasRoleInAnyProject(User aUser, PermissionLevel aRole,
            PermissionLevel... aMoreRoles)
    {
        return hasRoleInAnyProject(aUser.getUsername(), aRole, aMoreRoles);
    }

    @Override
    @Transactional(readOnly = true)
    public boolean hasRoleInAnyProject(String aUser, PermissionLevel aRole,
            PermissionLevel... aMoreRoles)
    {
        Validate.notNull(aRole, "hasRoleInAnyProject() requires at least one role to check");

        var roles = new LinkedHashSet<>();
        roles.add(aRole);
        if (aMoreRoles != null) {
            roles.addAll(asList(aMoreRoles));
        }

        var query = join("\n", //
                "SELECT COUNT(*) FROM ProjectPermission ", //
                "WHERE user = :user AND level IN (:roles)");

        return entityManager.createQuery(query, Long.class) //
                .setParameter("user", aUser) //
                .setParameter("roles", roles) //
                .getSingleResult() > 0;
    }

    @Deprecated
    @Override
    @Transactional
    public List<PermissionLevel> getProjectPermissionLevels(User aUser, Project aProject)
    {
        return listRoles(aProject, aUser);
    }

    @Override
    @Transactional
    public void assignRole(Project aProject, User aUser, PermissionLevel... aRoles)
    {
        var roles = new LinkedHashSet<>(listRoles(aProject, aUser));
        roles.addAll(asList(aRoles));
        setProjectPermissionLevels(aUser, aProject, roles);
    }

    @Deprecated
    @Override
    @Transactional
    public void assignRole(Project aProject, String aUser, PermissionLevel... aRoles)
    {
        var wrappedUser = new User(aUser);
        var roles = new LinkedHashSet<>(listRoles(aProject, wrappedUser));
        roles.addAll(asList(aRoles));
        setProjectPermissionLevels(wrappedUser, aProject, roles);
    }

    @Override
    @Transactional
    public void revokeRole(Project aProject, User aUser, PermissionLevel... aRoles)
    {
        var roles = new LinkedHashSet<>(listRoles(aProject, aUser));
        roles.removeAll(asList(aRoles));
        setProjectPermissionLevels(aUser, aProject, roles);
    }

    @Override
    @Transactional
    public void revokeAllRoles(Project aProject, User aUser)
    {
        setProjectPermissionLevels(aUser, aProject, emptyList());
    }

    @Override
    @Transactional(noRollbackFor = NoResultException.class)
    public void setProjectPermissionLevels(User aUser, Project aProject,
            Collection<PermissionLevel> aLevels)
    {
        setProjectPermissionLevels(aUser.getUsername(), aProject, aLevels);
    }

    @Override
    @Transactional(noRollbackFor = NoResultException.class)
    public void setProjectPermissionLevels(String aUser, Project aProject,
            Collection<PermissionLevel> aLevels)
    {
        try (var logCtx = withProjectLogger(aProject)) {
            var levelsToBeGranted = new HashSet<PermissionLevel>(aLevels);
            var permissions = new ArrayList<ProjectPermission>();
            try {
                permissions.addAll(listProjectPermissionLevel(aUser, aProject));
            }
            catch (NoResultException e) {
                // Nothing to do
            }

            // Remove permissions that no longer exist
            var revokedPermissions = new ArrayList<ProjectPermission>();
            for (var permission : permissions) {
                if (!aLevels.contains(permission.getLevel())) {
                    revokedPermissions.add(permission);

                    entityManager.remove(permission);

                    LOG.info("Removed permission [{}] for user [{}] on project {}",
                            permission.getLevel(), permission.getUser(), permission.getProject());
                }
                else {
                    levelsToBeGranted.remove(permission.getLevel());
                }
            }

            // Grant new permissions
            var grantedPermissions = new ArrayList<ProjectPermission>();
            for (var level : levelsToBeGranted) {
                var permission = new ProjectPermission(aProject, aUser, level);

                grantedPermissions.add(permission);

                entityManager.persist(permission);

                LOG.info("Granted permission [{}] for user {} on project {}", level, aUser,
                        aProject);
            }

            applicationEventPublisher.publishEvent(new ProjectPermissionsChangedEvent(this,
                    aProject, grantedPermissions, revokedPermissions));
        }
    }

    @Deprecated
    @Override
    @Transactional
    public void leaveProject(User aObject, Project aProject)
    {
        revokeAllRoles(aProject, aObject);
    }

    @Override
    @Transactional
    public List<User> listUsersWithAnyRoleInProject(Project aProject)
    {
        var query = join("\n", //
                "SELECT DISTINCT u FROM User u, ProjectPermission pp ", //
                "WHERE pp.user = u.username ", //
                "AND pp.project = :project ", //
                "ORDER BY u.username ASC");

        return entityManager.createQuery(query, User.class) //
                .setParameter("project", aProject) //
                .getResultList();
    }

    @Override
    @Transactional
    public List<User> listUsersWithRoleInProject(Project aProject, PermissionLevel aPermissionLevel)
    {
        var query = join("\n", //
                "SELECT DISTINCT u FROM User u, ProjectPermission pp ", //
                "WHERE pp.user = u.username ", //
                "AND pp.project = :project ", //
                "AND pp.level = :level ", //
                "ORDER BY u.username ASC");

        return entityManager.createQuery(query, User.class) //
                .setParameter("project", aProject) //
                .setParameter("level", aPermissionLevel) //
                .getResultList();
    }

    @Override
    @Transactional(noRollbackFor = NoResultException.class)
    public Project getProjectBySlug(String aSlug)
    {
        var query = "FROM Project WHERE slug = :slug";
        return entityManager.createQuery(query, Project.class) //
                .setParameter("slug", aSlug) //
                .getSingleResult();
    }

    @Override
    @Transactional(noRollbackFor = NoResultException.class)
    public Project getProject(long aId)
    {
        var query = "FROM Project " + "WHERE id = :id";
        return entityManager.createQuery(query, Project.class) //
                .setParameter("id", aId) //
                .getSingleResult();
    }

    @Deprecated
    @Override
    public List<ProjectPermission> getProjectPermissions(Project aProject)
    {
        return listProjectPermissions(aProject);
    }

    @Deprecated
    @Override
    @Transactional(readOnly = true)
    public Date getProjectTimeStamp(Project aProject, String aUsername)
    {
        var query = join("\n", //
                "SELECT MAX(ann.timestamp) ", //
                "FROM AnnotationDocument AS ann ", //
                "WHERE ann.project = :project AND ann.user = :user");

        return entityManager.createQuery(query, Date.class) //
                .setParameter("project", aProject) //
                .setParameter("user", aUsername) //
                .getSingleResult();
    }

    @Override
    public Realm getRealm(Project aProject)
    {
        return Realm.forProject(aProject.getId(), aProject.getName());
    }

    @Override
    @Transactional
    public Optional<User> getProjectBoundUser(Project aProject, String aUiName)
    {
        var realm = getRealm(aProject);

        return Optional.ofNullable(userRepository.getUserByRealmAndUiName(realm, aUiName));

    }

    @Override
    @Transactional
    public User createProjectBoundUser(Project aProject, String aUiName)
    {
        var realm = getRealm(aProject);

        return userRepository.create(User.builder() //
                .withUsername(generateRandomUsername()) //
                .withUiName(aUiName) //
                .withPassword(EMPTY_PASSWORD) //
                .withRealm(realm) //
                .withEnabled(true) //
                .withRoles(ROLE_USER) //
                .build());
    }

    @Override
    @Transactional
    public User getOrCreateProjectBoundUser(Project aProject, String aUiName)
    {
        var realm = getRealm(aProject);

        var user = userRepository.getUserByRealmAndUiName(realm, aUiName);

        if (user != null) {
            return user;
        }

        return userRepository.create(User.builder() //
                .withUsername(generateRandomUsername()) //
                .withUiName(aUiName) //
                .withPassword(EMPTY_PASSWORD) //
                .withRealm(realm) //
                .withEnabled(true) //
                .withRoles(ROLE_USER) //
                .build());
    }

    @Override
    @Transactional
    public void deleteProjectBoundUser(Project aProject, User aUser)
    {
        var realm = getRealm(aProject);

        var user = userRepository.getUserByRealmAndUiName(realm, aUser.getUiName());
        if (user == null) {
            throw new IllegalArgumentException(
                    "User " + aUser + " does not exist in project " + aProject);
        }

        revokeAllRoles(aProject, aUser);

        userRepository.delete(user);
    }

    private String generateRandomUsername()
    {
        nextName: while (true) {
            var bytes = new byte[RANDOM_USERNAME_BYTE_LENGTH];
            random.nextBytes(bytes);
            var randomUserId = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);

            // Do not accept base64 values which contain characters that are not safe for file
            // names / not valid as usernames
            if (!userRepository.isValidUsername(randomUserId)) {
                continue nextName;
            }

            if (!userRepository.exists(randomUserId)) {
                return randomUserId;
            }
        }
    }

    @Deprecated
    @Override
    @Transactional(readOnly = true)
    public Date getProjectTimeStamp(Project aProject)
    {
        var query = join("\n", //
                "SELECT MAX(doc.timestamp) ", //
                "FROM SourceDocument AS doc ", //
                "WHERE doc.project = :project");

        return entityManager.createQuery(query, Date.class) //
                .setParameter("project", aProject) //
                .getSingleResult();
    }

    @Override
    @Transactional
    public List<Project> listProjectsWithFinishedAnnos()
    {
        var query = join("\n", //
                "SELECT DISTINCT ann.project ", //
                "FROM AnnotationDocument AS ann ", //
                "WHERE ann.state = :state");
        return entityManager.createQuery(query, Project.class)
                .setParameter("state", AnnotationDocumentState.FINISHED) //
                .getResultList();
    }

    @Override
    @Transactional
    public List<Project> listProjects()
    {
        var query = "FROM Project ORDER BY name ASC";
        return entityManager.createQuery(query, Project.class).getResultList();
    }

    @Override
    @Transactional
    public void removeProject(Project aProject) throws IOException
    {
        try (var logCtx = withProjectLogger(aProject)) {
            var start = System.currentTimeMillis();

            // remove metadata from DB
            var project = aProject;
            if (!entityManager.contains(project)) {
                project = entityManager.merge(project);
            }

            applicationEventPublisher.publishEvent(new BeforeProjectRemovedEvent(this, project));

            for (var permissions : getProjectPermissions(project)) {
                entityManager.remove(permissions);
            }

            entityManager.remove(project);

            // remove the project directory from the file system
            var path = repositoryProperties.getPath().getAbsolutePath() + "/" + PROJECT_FOLDER + "/"
                    + project.getId();
            try {
                FastIOUtils.delete(new File(path));
            }
            catch (FileNotFoundException | NoSuchFileException e) {
                LOG.info("Project directory to be deleted was not found: [{}]. Ignoring.", path);
            }

            applicationEventPublisher.publishEvent(new AfterProjectRemovedEvent(this, project));

            LOG.info("Removed project {} ({})", project,
                    formatDurationWords(System.currentTimeMillis() - start, true, true));
        }
    }

    @Override
    @Transactional
    @Deprecated
    public void removeProjectPermission(ProjectPermission aPermission)
    {
        try (var logCtx = withProjectLogger(aPermission.getProject())) {
            entityManager.remove(aPermission);

            LOG.info("Removed permission [{}] for user [{}] on project {}", aPermission.getLevel(),
                    aPermission.getUser(), aPermission.getProject());

            applicationEventPublisher.publishEvent(new ProjectPermissionsChangedEvent(this,
                    aPermission.getProject(), emptyList(), asList(aPermission)));
        }
    }

    @Deprecated
    @Override
    public void savePropertiesFile(Project aProject, InputStream aIs, String aFileName)
        throws IOException
    {
        String path = repositoryProperties.getPath().getAbsolutePath() + "/" + PROJECT_FOLDER + "/"
                + aProject.getId() + "/" + FilenameUtils.getFullPath(aFileName);
        FileUtils.forceMkdir(new File(path));

        File newTcfFile = new File(path, FilenameUtils.getName(aFileName));
        try (OutputStream os = new FileOutputStream(newTcfFile)) {
            copyLarge(aIs, os);
        }
    }

    @Override
    @Transactional
    public List<Project> listAccessibleProjects(User aUser)
    {
        // if global admin, list all projects
        if (userRepository.isAdministrator(aUser)) {
            return listProjects();
        }

        return listProjectsWithUserHavingAnyRole(aUser);
    }

    @Override
    @Transactional
    public Map<Project, Set<PermissionLevel>> listAccessibleProjectsWithPermissions(User aUser)
    {
        Map<Project, Set<PermissionLevel>> result = new LinkedHashMap<>();

        // Admins have access to any project, but they may not have actual roles in them, so we
        // add all the projects without roles and then fill in any roles later
        if (userRepository.isAdministrator(aUser)) {
            for (var project : listProjects()) {
                result.computeIfAbsent(project, _p -> new LinkedHashSet<>());
            }
        }

        var permissionAssignments = listProjectPermissions(aUser);
        for (var perm : permissionAssignments) {
            var levels = result.computeIfAbsent(perm.getProject(), _p -> new HashSet<>());
            levels.add(perm.getLevel());
        }

        return result;
    }

    @Override
    @Transactional
    public List<Project> listManageableProjects(User aUser)
    {
        // if global admin, show all projects
        if (userRepository.isAdministrator(aUser)) {
            return listProjects();
        }

        return listProjectsWithUserHavingRole(aUser, MANAGER);
    }

    @Override
    @Transactional(readOnly = true)
    public boolean managesAnyProject(User aUser)
    {
        if (userRepository.isAdministrator(aUser)) {
            return true;
        }

        if (userRepository.isProjectCreator(aUser)) {
            return true;
        }

        if (!listProjectsWithUserHavingRole(aUser, MANAGER).isEmpty()) {
            return true;
        }

        return false;
    }

    @EventListener
    @Transactional
    public void onContextRefreshedEvent(ContextRefreshedEvent aEvent)
    {
        init();
        addMissingProjectSlugs();
    }

    /* package private */ void init()
    {
        var projectInits = new ArrayList<ProjectInitializer>();

        if (projectInitializerProxy != null) {
            projectInits.addAll(projectInitializerProxy);
            AnnotationAwareOrderComparator.sort(projectInits);

            var initializerClasses = new HashSet<Class<? extends ProjectInitializer>>();
            for (var init : projectInits) {
                if (initializerClasses.add(init.getClass())) {
                    LOG.debug("Found project initializer: {}",
                            ClassUtils.getAbbreviatedName(init.getClass(), 20));
                }
                else {
                    throw new IllegalStateException("There cannot be more than once instance "
                            + "of each project initializer class! Duplicate instance of class: "
                            + init.getClass());
                }
            }
        }

        BaseLoggers.BOOT_LOG.info("Found [{}] project initializers", projectInits.size());

        projectInitializers = unmodifiableList(projectInits);

        var featureInits = new ArrayList<FeatureInitializer>();

        if (featureInitializerProxy != null) {
            featureInits.addAll(featureInitializerProxy);
            AnnotationAwareOrderComparator.sort(featureInits);

            var initializerClasses = new HashSet<Class<? extends FeatureInitializer>>();
            for (var init : featureInits) {
                if (initializerClasses.add(init.getClass())) {
                    LOG.debug("Found feature initializer: {}",
                            ClassUtils.getAbbreviatedName(init.getClass(), 20));
                }
                else {
                    throw new IllegalStateException("There cannot be more than once instance "
                            + "of each feature initializer class! Duplicate instance of class: "
                            + init.getClass());
                }
            }
        }

        BaseLoggers.BOOT_LOG.info("Found [{}] feature initializers", projectInits.size());

        featureInitializers = unmodifiableList(featureInits);
    }

    private void addMissingProjectSlugs()
    {
        var query = "SELECT p FROM Project p WHERE slug IS NULL";
        var projects = entityManager.createQuery(query, Project.class).getResultList();
        for (var project : projects) {
            var slug = deriveSlugFromName(project.getName());
            if (!isValidProjectSlug(slug)) {
                LOG.warn("Attempt to derive slug from project name [{}] resulted in invalid slug "
                        + "[{}], generating random slug...", project.getName(), slug);
                slug = generateRandomSlug();
            }
            slug = deriveUniqueSlug(slug);
            project.setSlug(slug);
            LOG.info("Auto-generated slug [{}] for project {}", slug, project);
        }
    }

    private String generateRandomSlug()
    {
        var validChars = "abcdefghijklmnopqrstuvwxyz";
        var rnd = new Random();
        var sb = new StringBuilder();
        for (var i = 0; i < 32; i++) {
            sb.append(validChars.charAt(rnd.nextInt(validChars.length())));
        }
        return sb.toString();
    }

    @Override
    public List<ProjectInitializer> listProjectInitializers()
    {
        return projectInitializers;
    }

    @Override
    public List<FeatureInitializer> listFeatureInitializers()
    {
        return featureInitializers;
    }

    @Override
    @Transactional
    public void initializeProject(ProjectInitializationRequest aRequest) throws IOException
    {
        initializeProject(aRequest, projectInitializers.stream() //
                .filter(ProjectInitializer::applyByDefault) //
                .collect(Collectors.toList()));
    }

    private ProjectInitializer findProjectInitializer(Class<? extends ProjectInitializer> aType)
    {
        return projectInitializers.stream().filter(i -> aType.isAssignableFrom(i.getClass())) //
                .findFirst() //
                .orElseThrow(() -> new IllegalArgumentException(
                        "No initializer of type [" + aType + "] exists!"));
    }

    private Set<ProjectInitializer> collectDependencies(List<ProjectInitializer> aInitializers)
    {
        var seen = new LinkedHashSet<ProjectInitializer>();

        var deque = new LinkedList<ProjectInitializer>(aInitializers);
        while (!deque.isEmpty()) {
            var initializer = deque.poll();

            if (seen.contains(initializer)) {
                continue;
            }

            seen.add(initializer);

            for (var depClass : initializer.getDependencies()) {
                deque.add(findProjectInitializer(depClass));
            }
        }

        return seen;
    }

    @Override
    @Transactional
    public void initializeProject(ProjectInitializationRequest aRequest,
            List<ProjectInitializer> aInitializers)
        throws IOException
    {
        var allInits = new HashSet<Class<? extends ProjectInitializer>>();
        var applied = new HashSet<Class<? extends ProjectInitializer>>();
        for (var initializer : projectInitializers) {
            allInits.add(initializer.getClass());
            if (initializer.alreadyApplied(aRequest.getProject())) {
                applied.add(initializer.getClass());
            }
        }

        var toApply = new LinkedList<ProjectInitializer>(collectDependencies(aInitializers));
        Set<ProjectInitializer> initsDeferred = SetUtils.newIdentityHashSet();
        while (!toApply.isEmpty()) {
            var initializer = toApply.pop();
            var initializerName = initializer.getName();

            if (applied.contains(initializer.getClass())) {
                LOG.debug("Skipping project initializer that was already applied: [{}]",
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
                LOG.debug("Applying project initializer: [{}]", initializerName);
                initializer.configure(aRequest);
                applied.add(initializer.getClass());
                initsDeferred.clear();
            }
            else {
                LOG.debug("Deferring project initializer as dependencies are not yet fulfilled: {}",
                        initializer);
                toApply.add(initializer);
                initsDeferred.add(initializer);
            }
        }
    }

    @Override
    @Transactional
    public List<Project> listProjectsWithUserHavingRole(User aUser, PermissionLevel aRole,
            PermissionLevel... aMoreRoles)
    {
        Validate.notNull(aRole,
                "listProjectsWithUserHavingRole() requires at least one role to check");

        var roles = new LinkedHashSet<>();
        roles.add(aRole);
        if (aMoreRoles != null) {
            roles.addAll(asList(aMoreRoles));
        }

        var cb = entityManager.getCriteriaBuilder();
        var cq = cb.createQuery(Project.class);
        var permission = cq.from(ProjectPermission.class);
        var project = permission.join(ProjectPermission_.project);
        cq.select(project).distinct(true);
        cq.where(cb.and( //
                cb.equal(permission.get(ProjectPermission_.user), aUser.getUsername()), //
                permission.get(ProjectPermission_.level).in(roles)));
        cq.orderBy(cb.asc(project.get(Project_.name)));
        return entityManager.createQuery(cq).getResultList();

        // String query = String.join("\n", //
        // "SELECT DISTINCT p FROM Project p, ProjectPermission pp ", //
        // "WHERE pp.project = p.id ", //
        // "AND pp.user = :username ", //
        // "AND pp.level IN (:roles) ", //
        // "ORDER BY p.name ASC");
        // List<Project> projects = entityManager.createQuery(query, Project.class)
        // .setParameter("username", aUser.getUsername()) //
        // .setParameter("roles", roles) //
        // .getResultList();
        // return projects;
    }

    @Override
    @Transactional
    public List<Project> listProjectsWithUserHavingAnyRole(User aUser)
    {
        var query = join("\n", //
                "SELECT DISTINCT pp.project FROM ProjectPermission pp ", //
                "WHERE pp.user = :username ", //
                "ORDER BY pp.project.name ASC");
        var projects = entityManager.createQuery(query, Project.class) //
                .setParameter("username", aUser.getUsername()) //
                .getResultList();
        return projects;
    }

    @Override
    @Transactional
    public void setProjectState(Project aProject, ProjectState aState)
    {
        var oldState = aProject.getState();

        aProject.setState(aState);
        updateProject(aProject);

        if (!Objects.equals(oldState, aProject.getState())) {
            applicationEventPublisher
                    .publishEvent(new ProjectStateChangedEvent(this, aProject, oldState));
        }
    }

    @Override
    public String deriveSlugFromName(String aName)
    {
        if (aName == null) {
            return null;
        }

        var name = aName.trim().toLowerCase(Locale.ROOT);
        if (name.isEmpty()) {
            return "";
        }

        var buf = new StringBuilder();
        var lastCharMappedToUnderscore = true;
        for (var i = 0; i < min(name.length(), MAX_PROJECT_SLUG_LENGTH); i++) {
            var c = name.charAt(i);
            if (Project.isValidProjectSlugCharacter(c)) {
                lastCharMappedToUnderscore = c == '_';
                buf.append(c);
                continue;
            }

            if (c == ' ') {
                buf.append('-');
                continue;
            }

            if (lastCharMappedToUnderscore) {
                continue;
            }

            buf.append("_");
            lastCharMappedToUnderscore = true;
        }

        if (buf.length() == 0) {
            return "";
        }

        if (!isValidProjectSlugInitialCharacter(buf.charAt(0))) {
            buf.insert(0, 'x');
            if (buf.length() > MAX_PROJECT_SLUG_LENGTH) {
                buf.setLength(MAX_PROJECT_SLUG_LENGTH);
            }
        }

        while (buf.length() < MIN_PROJECT_SLUG_LENGTH) {
            buf.append("_");
        }

        return buf.toString();
    }

    @Transactional
    @Override
    public String deriveUniqueSlug(String aSlug)
    {
        var i = 0;
        var slug = aSlug;
        while (true) {
            if (!existsProjectWithSlug(slug)) {
                return slug;
            }

            i++;
            var suffix = "-" + i;
            if (MAX_PROJECT_SLUG_LENGTH - aSlug.length() <= suffix.length()) {
                slug = aSlug.substring(0, MAX_PROJECT_SLUG_LENGTH - suffix.length()) + suffix;
            }
            else {
                slug = aSlug + suffix;
            }
        }
    }

    @Transactional
    @Override
    public Realm getRealm(String aRealmId)
    {
        if (!startsWith(aRealmId, Realm.REALM_PROJECT_PREFIX)) {
            throw new IllegalArgumentException(
                    "Project realm must start with [" + Realm.REALM_PROJECT_PREFIX + "]");
        }

        var projectId = Long.valueOf(substringAfter(aRealmId, Realm.REALM_PROJECT_PREFIX));
        var project = getProject(projectId);
        if (project != null) {
            return new Realm(aRealmId, "<Project> " + project.getName());
        }
        else {
            return new Realm(aRealmId, "<Project (deleted)>: " + projectId + ">");
        }
    }
}

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
package de.tudarmstadt.ukp.clarin.webanno.api.dao;

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
import java.util.Date;
import java.util.List;
import java.util.Properties;
import java.util.Map.Entry;

import javax.annotation.Resource;
import javax.persistence.EntityManager;
import javax.persistence.NoResultException;
import javax.persistence.PersistenceContext;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.beans.BeanWrapper;
import org.springframework.beans.PropertyAccessorFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.transaction.annotation.Transactional;

import de.tudarmstadt.ukp.clarin.webanno.api.AnnotationSchemaService;
import de.tudarmstadt.ukp.clarin.webanno.api.DocumentService;
import de.tudarmstadt.ukp.clarin.webanno.api.ProjectService;
import de.tudarmstadt.ukp.clarin.webanno.api.UserDao;
import de.tudarmstadt.ukp.clarin.webanno.constraints.ConstraintsService;
import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationDocumentState;
import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationFeature;
import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationLayer;
import de.tudarmstadt.ukp.clarin.webanno.model.Authority;
import de.tudarmstadt.ukp.clarin.webanno.model.ConstraintSet;
import de.tudarmstadt.ukp.clarin.webanno.model.Mode;
import de.tudarmstadt.ukp.clarin.webanno.model.PermissionLevel;
import de.tudarmstadt.ukp.clarin.webanno.model.Project;
import de.tudarmstadt.ukp.clarin.webanno.model.ProjectPermission;
import de.tudarmstadt.ukp.clarin.webanno.model.SourceDocument;
import de.tudarmstadt.ukp.clarin.webanno.model.TagSet;
import de.tudarmstadt.ukp.clarin.webanno.model.User;
import de.tudarmstadt.ukp.clarin.webanno.support.logging.Logging;

public class ProjectServiceImpl
    implements ProjectService
{
    private final Logger log = LoggerFactory.getLogger(getClass());

    @PersistenceContext
    private EntityManager entityManager;
    
    @Resource(name = "userRepository")
    private UserDao userRepository;

    @Resource(name = "annotationService")
    private AnnotationSchemaService annotationService;

    @Resource(name = "documentService")
    private DocumentService documentService;

    @Resource(name = "constraintsService")
    private ConstraintsService constraintsService;

    @Value(value = "${repository.path}")
    private File dir;

    // The annotation preference properties File name
    private static final String annotationPreferencePropertiesFileName = "annotation.properties";

    public ProjectServiceImpl()
    {
        // Nothing to do
    }
    
    @Override
    @Transactional
    public void createProject(Project aProject)
        throws IOException
    {
        entityManager.persist(aProject);
        String path = dir.getAbsolutePath() + PROJECT + aProject.getId();
        FileUtils.forceMkdir(new File(path));
        
        try (MDC.MDCCloseable closable = MDC.putCloseable(Logging.KEY_PROJECT_ID,
                String.valueOf(aProject.getId()))) {
            log.info("Created project [{}]({})", aProject.getName(), aProject.getId());
        }
    }

    @Override
    @Transactional
    public void updateProject(Project aProject)
    {
        entityManager.merge(aProject);
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
        try {
            entityManager.createQuery("FROM Project WHERE name = :name", Project.class)
                    .setParameter("name", aName).getSingleResult();
            return true;
        }
        catch (NoResultException ex) {
            return false;
        }
    }
    
    @Override
    public boolean existsProjectPermission(User aUser, Project aProject)
    {

        List<ProjectPermission> projectPermissions = entityManager
                .createQuery(
                        "FROM ProjectPermission WHERE user = :user AND " + "project =:project",
                        ProjectPermission.class).setParameter("user", aUser.getUsername())
                .setParameter("project", aProject).getResultList();
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
    public boolean existsProjectPermissionLevel(User aUser, Project aProject, PermissionLevel aLevel)
    {
        try {
            entityManager
                    .createQuery(
                            "FROM ProjectPermission WHERE user = :user AND "
                                    + "project =:project AND level =:level",
                            ProjectPermission.class).setParameter("user", aUser.getUsername())
                    .setParameter("project", aProject).setParameter("level", aLevel)
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
        return new File(dir.getAbsolutePath() + PROJECT + "project-" + aProject.getId() + ".log");
    }

    @Override
    public File getGuidelinesFile(Project aProject)
    {
        return new File(dir.getAbsolutePath() + PROJECT + aProject.getId() + GUIDELINE);
    }

    @Override
    public File getMetaInfFolder(Project aProject)
    {
        return new File(dir.getAbsolutePath() + PROJECT + aProject.getId() + META_INF);
    }

    @Override
    @Transactional(noRollbackFor = NoResultException.class)
    public List<Authority> listAuthorities(User aUser)
    {
        return entityManager
                .createQuery("FROM Authority where username =:username", Authority.class)
                .setParameter("username", aUser).getResultList();
    }

    @Override
    public File getGuideline(Project aProject, String aFilename)
    {
        return new File(dir.getAbsolutePath() + PROJECT + aProject.getId() + GUIDELINE + aFilename);
    }

    @Override
    @Transactional(noRollbackFor = NoResultException.class)
    public List<ProjectPermission> listProjectPermissionLevel(User aUser, Project aProject)
    {
        return entityManager
                .createQuery("FROM ProjectPermission WHERE user =:user AND " + "project =:project",
                        ProjectPermission.class).setParameter("user", aUser.getUsername())
                .setParameter("project", aProject).getResultList();
    }

    @Override
    public List<User> listProjectUsersWithPermissions(Project aProject)
    {

        List<String> usernames = entityManager
                .createQuery(
                        "SELECT DISTINCT user FROM ProjectPermission WHERE "
                                + "project =:project ORDER BY user ASC", String.class)
                .setParameter("project", aProject).getResultList();

        List<User> users = new ArrayList<User>();

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
        List<String> usernames = entityManager
                .createQuery(
                        "SELECT DISTINCT user FROM ProjectPermission WHERE "
                                + "project =:project AND level =:level ORDER BY user ASC",
                        String.class).setParameter("project", aProject)
                .setParameter("level", aPermissionLevel).getResultList();
        List<User> users = new ArrayList<User>();
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
        return entityManager.createQuery("FROM Project WHERE name = :name", Project.class)
                .setParameter("name", aName).getSingleResult();
    }

    @Override
    public Project getProject(long aId)
    {
        return entityManager.createQuery("FROM Project WHERE id = :id", Project.class)
                .setParameter("id", aId).getSingleResult();
    }

    @Override
    public void createGuideline(Project aProject, File aContent, String aFileName, String aUsername)
        throws IOException
    {
        String guidelinePath = dir.getAbsolutePath() + PROJECT + aProject.getId() + GUIDELINE;
        FileUtils.forceMkdir(new File(guidelinePath));
        copyLarge(new FileInputStream(aContent), new FileOutputStream(new File(guidelinePath
                + aFileName)));

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
        return entityManager
                .createQuery("FROM ProjectPermission WHERE project =:project",
                        ProjectPermission.class).setParameter("project", aProject).getResultList();
    }

    @Override
    @Transactional
    public Date getProjectTimeStamp(Project aProject, String aUsername)
    {
        return entityManager
                .createQuery(
                        "SELECT max(timestamp) FROM AnnotationDocument WHERE project = :project "
                                + " AND user = :user", Date.class)
                .setParameter("project", aProject).setParameter("user", aUsername)
                .getSingleResult();
    }

    @Override
    public Date getProjectTimeStamp(Project aProject)
    {
        return entityManager
                .createQuery("SELECT max(timestamp) FROM SourceDocument WHERE project = :project",
                        Date.class).setParameter("project", aProject).getSingleResult();
    }

    @Override
    @Transactional(noRollbackFor = NoResultException.class)
    public List<Project> listProjectsWithFinishedAnnos()
    {

        return entityManager
                .createQuery("SELECT DISTINCT project FROM AnnotationDocument WHERE state = :state",
                        Project.class)
                .setParameter("state", AnnotationDocumentState.FINISHED.getName()).getResultList();

    }

    @Override
    public List<String> listGuidelines(Project aProject)
    {
        // list all guideline files
        File[] files = new File(dir.getAbsolutePath() + PROJECT + aProject.getId() + GUIDELINE)
                .listFiles();

        // Name of the guideline files
        List<String> annotationGuidelineFiles = new ArrayList<String>();
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
        return entityManager.createQuery("FROM Project  ORDER BY name ASC ", Project.class)
                .getResultList();
    }

    @Override
    public Properties loadUserSettings(String aUsername, Project aProject)
        throws FileNotFoundException, IOException
    {
        Properties property = new Properties();
        property.load(new FileInputStream(new File(dir.getAbsolutePath() + PROJECT
                + aProject.getId() + SETTINGS + aUsername + "/"
                + annotationPreferencePropertiesFileName)));
        return property;
    }

    @Override
    @Transactional
    public void removeProject(Project aProject)
        throws IOException
    {
        for (SourceDocument document : documentService.listSourceDocuments(aProject)) {
            documentService.removeSourceDocument(document);
        }

        for (AnnotationFeature feature : annotationService.listAnnotationFeature(aProject)) {
            annotationService.removeAnnotationFeature(feature);
        }

        // remove the layers too
        for (AnnotationLayer layer : annotationService.listAnnotationLayer(aProject)) {
            annotationService.removeAnnotationLayer(layer);
        }

        for (TagSet tagSet : annotationService.listTagSets(aProject)) {
            annotationService.removeTagSet(tagSet);
        }

        for (ProjectPermission permissions : getProjectPermissions(aProject)) {
            entityManager.remove(permissions);
        }
        
        //Remove Constraints
        for (ConstraintSet set: constraintsService.listConstraintSets(aProject) ){
            constraintsService.removeConstraintSet(set);
        }
        
        // remove metadata from DB
        Project project = aProject;
        if (!entityManager.contains(project)) {
            project = entityManager.merge(project);
        }
        entityManager.remove(project);
        
        // remove the project directory from the file system
        String path = dir.getAbsolutePath() + PROJECT + aProject.getId();
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
    public void removeGuideline(Project aProject, String aFileName, String username)
        throws IOException
    {
        FileUtils.forceDelete(new File(dir.getAbsolutePath() + PROJECT + aProject.getId()
                + GUIDELINE + aFileName));
        
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
        String path = dir.getAbsolutePath() + PROJECT + aProject.getId() + "/"
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
        Properties property = new Properties();
        for (PropertyDescriptor value : wrapper.getPropertyDescriptors()) {
            if (wrapper.getPropertyValue(value.getName()) == null) {
                continue;
            }
            property.setProperty(aSubject + "." + value.getName(),
                    wrapper.getPropertyValue(value.getName()).toString());
        }
        String propertiesPath = dir.getAbsolutePath() + PROJECT + aProject.getId() + SETTINGS
                + aUsername;
        // append existing preferences for the other mode
        if (new File(propertiesPath, annotationPreferencePropertiesFileName).exists()) {
            // aSubject = aSubject.equals(Mode.ANNOTATION) ? Mode.CURATION :
            // Mode.ANNOTATION;
            for (Entry<Object, Object> entry : loadUserSettings(aUsername, aProject).entrySet()) {
                String key = entry.getKey().toString();
                // Maintain other Modes of annotations confs than this one
                if (!key.substring(0, key.indexOf(".")).equals(aSubject.toString())) {
                    property.put(entry.getKey(), entry.getValue());
                }
            }
        }
        FileUtils.forceMkdir(new File(propertiesPath));
        property.store(new FileOutputStream(new File(propertiesPath,
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
        List<Project> allowedProject = new ArrayList<Project>();
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
}

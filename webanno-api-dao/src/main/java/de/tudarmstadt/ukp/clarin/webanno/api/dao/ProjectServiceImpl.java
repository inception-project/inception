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
import java.util.Collections;
import java.util.Date;
import java.util.Enumeration;
import java.util.List;
import java.util.Map.Entry;
import java.util.Comparator;
import java.util.Properties;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

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
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.SmartLifecycle;
import org.springframework.context.annotation.ClassPathScanningCandidateComponentProvider;
import org.springframework.core.type.filter.AnnotationTypeFilter;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import de.tudarmstadt.ukp.clarin.webanno.api.ProjectLifecycleAware;
import de.tudarmstadt.ukp.clarin.webanno.api.ProjectLifecycleAwareRegistry;
import de.tudarmstadt.ukp.clarin.webanno.api.ProjectService;
import de.tudarmstadt.ukp.clarin.webanno.api.ProjectType;
import de.tudarmstadt.ukp.clarin.webanno.api.SecurityUtil;
import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationDocumentState;
import de.tudarmstadt.ukp.clarin.webanno.model.Mode;
import de.tudarmstadt.ukp.clarin.webanno.model.PermissionLevel;
import de.tudarmstadt.ukp.clarin.webanno.model.Project;
import de.tudarmstadt.ukp.clarin.webanno.model.ProjectPermission;
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

    @PersistenceContext
    private EntityManager entityManager;
    
    @Resource(name = "userRepository")
    private UserDao userRepository;

    @Resource
    private ProjectLifecycleAwareRegistry projectLifecycleAwareRegistry;

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
        entityManager.persist(aProject);
        String path = dir.getAbsolutePath() + PROJECT + aProject.getId();
        FileUtils.forceMkdir(new File(path));
        
        try (MDC.MDCCloseable closable = MDC.putCloseable(Logging.KEY_PROJECT_ID,
                String.valueOf(aProject.getId()))) {
            log.info("Created project [{}]({})", aProject.getName(), aProject.getId());
        }
        
        // Notify all relevant service so that they can initialize themselves for the given project
        for (ProjectLifecycleAware bean : projectLifecycleAwareRegistry.getBeans()) {
            try {
                bean.afterProjectCreate(aProject);
            }
            catch (IOException e) {
                throw e;
            }
            catch (Exception e) {
                throw new IllegalStateException(e);
            }
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
        List<String> usernames = entityManager
                .createQuery(
                        "SELECT DISTINCT user FROM ProjectPermission WHERE "
                                + "project =:project AND level =:level ORDER BY user ASC",
                        String.class).setParameter("project", aProject)
                .setParameter("level", aPermissionLevel).getResultList();
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
        // remove metadata from DB
        Project project = aProject;
        if (!entityManager.contains(project)) {
            project = entityManager.merge(project);
        }
        
        // Notify all relevant service so that they can clean up themselves before we remove the
        // project - notification happens in reverse order
        List<ProjectLifecycleAware> beans = new ArrayList<>(
                projectLifecycleAwareRegistry.getBeans());
        Collections.reverse(beans);
        for (ProjectLifecycleAware bean : beans) {
            try {
                bean.beforeProjectRemove(aProject);
            }
            catch (IOException e) {
                throw e;
            }
            catch (Exception e) {
                throw new IllegalStateException(e);
            }
        }

        for (ProjectPermission permissions : getProjectPermissions(aProject)) {
            entityManager.remove(permissions);
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
            de.tudarmstadt.ukp.clarin.webanno.export.model.Project aExportedProject,
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
    private void createProjectLog(ZipFile zip, Project aProject)
        throws IOException
    {
        for (Enumeration zipEnumerate = zip.entries(); zipEnumerate.hasMoreElements();) {
            ZipEntry entry = (ZipEntry) zipEnumerate.nextElement();

            // Strip leading "/" that we had in ZIP files prior to 2.0.8 (bug #985)
            String entryName = ZipUtils.normalizeEntryName(entry);
            
            if (entryName.startsWith(LOG_DIR)) {
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
    @SuppressWarnings("rawtypes")
    private void createProjectGuideline(ZipFile zip, Project aProject)
        throws IOException
    {
        for (Enumeration zipEnumerate = zip.entries(); zipEnumerate.hasMoreElements();) {
            ZipEntry entry = (ZipEntry) zipEnumerate.nextElement();
            
            // Strip leading "/" that we had in ZIP files prior to 2.0.8 (bug #985)
            String entryName = ZipUtils.normalizeEntryName(entry);
            
            if (entryName.startsWith(GUIDELINE)) {
                String fileName = FilenameUtils.getName(entry.getName());
                if(fileName.trim().isEmpty()){
                    continue;
                }
                File guidelineDir = getGuidelinesFile(aProject);
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
    @SuppressWarnings("rawtypes")
    private void createProjectMetaInf(ZipFile zip, Project aProject)
        throws IOException
    {
        for (Enumeration zipEnumerate = zip.entries(); zipEnumerate.hasMoreElements();) {
            ZipEntry entry = (ZipEntry) zipEnumerate.nextElement();

            // Strip leading "/" that we had in ZIP files prior to 2.0.8 (bug #985)
            String entryName = ZipUtils.normalizeEntryName(entry);

            if (entryName.startsWith(META_INF)) {
                File metaInfDir = new File(getMetaInfFolder(aProject),
                        FilenameUtils.getPath(entry.getName().replace(META_INF, "")));
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
     * {@link de.tudarmstadt.ukp.clarin.webanno.export.model.ProjectPermission}
     * @param aImportedProjectSetting the imported project.
     * @param aImportedProject the project.
     * @throws IOException if an I/O error occurs.
     */
    private void createProjectPermission(
            de.tudarmstadt.ukp.clarin.webanno.export.model.Project aImportedProjectSetting,
            Project aImportedProject)
        throws IOException
    {
        for (de.tudarmstadt.ukp.clarin.webanno.export.model.ProjectPermission importedPermission : aImportedProjectSetting
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
        projectTypes.sort(Comparator.comparingInt(ProjectType::prio));
    }

    @Override
    public List<ProjectType> listProjectTypes()
    {
        return Collections.unmodifiableList(projectTypes);
    }
}

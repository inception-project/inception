/*******************************************************************************
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
 ******************************************************************************/
package de.tudarmstadt.ukp.clarin.webanno.webapp.dao;

import static org.apache.commons.io.IOUtils.closeQuietly;
import static org.apache.commons.io.IOUtils.copyLarge;
import static org.uimafit.factory.AnalysisEngineFactory.createPrimitiveDescription;
import static org.uimafit.pipeline.SimplePipeline.runPipeline;

import java.beans.PropertyDescriptor;
import java.io.File;
import java.io.FileFilter;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Properties;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.annotation.Resource;
import javax.persistence.EntityManager;
import javax.persistence.NoResultException;
import javax.persistence.PersistenceContext;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.io.comparator.LastModifiedFileComparator;
import org.apache.log4j.FileAppender;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.apache.log4j.PatternLayout;
import org.apache.uima.UIMAException;
import org.apache.uima.analysis_engine.AnalysisEngine;
import org.apache.uima.analysis_engine.AnalysisEngineDescription;
import org.apache.uima.analysis_engine.AnalysisEngineProcessException;
import org.apache.uima.cas.CAS;
import org.apache.uima.collection.CollectionReader;
import org.apache.uima.jcas.JCas;
import org.apache.uima.resource.ResourceInitializationException;
import org.springframework.beans.BeanWrapper;
import org.springframework.beans.PropertyAccessorFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DataRetrievalFailureException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.transaction.annotation.Transactional;
import org.uimafit.factory.AnalysisEngineFactory;
import org.uimafit.factory.CollectionReaderFactory;
import org.uimafit.factory.JCasFactory;

import de.tudarmstadt.ukp.clarin.webanno.api.AnnotationService;
import de.tudarmstadt.ukp.clarin.webanno.api.RepositoryService;
import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationDocument;
import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationDocumentState;
import de.tudarmstadt.ukp.clarin.webanno.model.Authority;
import de.tudarmstadt.ukp.clarin.webanno.model.CrowdJob;
import de.tudarmstadt.ukp.clarin.webanno.model.Mode;
import de.tudarmstadt.ukp.clarin.webanno.model.PermissionLevel;
import de.tudarmstadt.ukp.clarin.webanno.model.Project;
import de.tudarmstadt.ukp.clarin.webanno.model.ProjectPermission;
import de.tudarmstadt.ukp.clarin.webanno.model.SourceDocument;
import de.tudarmstadt.ukp.clarin.webanno.model.TagSet;
import de.tudarmstadt.ukp.clarin.webanno.model.User;
import de.tudarmstadt.ukp.dkpro.core.api.io.JCasFileWriter_ImplBase;
import de.tudarmstadt.ukp.dkpro.core.api.metadata.type.DocumentMetaData;
import de.tudarmstadt.ukp.dkpro.core.io.bincas.SerializedCasReader;
import de.tudarmstadt.ukp.dkpro.core.io.bincas.SerializedCasWriter;
import eu.clarin.weblicht.wlfxb.io.WLFormatException;

public class RepositoryServiceDbData
    implements RepositoryService
{
    public static Logger createLog(Project aProject, String aUser)
        throws IOException
    {
        Logger logger = Logger.getLogger(RepositoryService.class);
        String targetLog = dir.getAbsolutePath() + PROJECT + "project-" + aProject.getId() + ".log";
        FileAppender apndr = new FileAppender(new PatternLayout("%d [" + aUser + "] %m%n"),
                targetLog, true);
        logger.addAppender(apndr);
        logger.setLevel((Level) Level.ALL);
        return logger;
    }

    @Resource(name = "annotationService")
    private AnnotationService annotationService;

    @Value(value = "${backup.keep.time}")
    private long backupKeepTime;

    @Value(value = "${backup.interval}")
    private long backupInterval;

    @Value(value = "${backup.keep.number}")
    private int backupKeepNumber;

    @Resource(name = "formats")
    private Properties readWriteFileFormats;

    private static final String PROJECT = "/project/";
    private static final String DOCUMENT = "/document/";
    private static final String SOURCE = "/source";
    private static final String GUIDELINE = "/guideline/";
    private static final String ANNOTATION = "/annotation";
    private static final String SETTINGS = "/settings/";
    private static final String META_INF = "/META-INF/";

    private static final String TEMPLATE = "/crowdtemplates/";

    private static final String CURATION_USER = "CURATION_USER";
    private static final String CORRECTION_USER = "CORRECTION_USER";

    @PersistenceContext
    private EntityManager entityManager;

    private static File dir;

    // The annotation preference properties File name
    String annotationPreferencePropertiesFileName;

    /*
     * @Resource(name = "formats") private Properties readWriteFileFormats;
     */
    private Object lock = new Object();

    public RepositoryServiceDbData()
    {

    }

    @Override
    @Transactional
    public void createAnnotationDocument(AnnotationDocument aAnnotationDocument)
    {
        if (aAnnotationDocument.getId() == 0) {
            entityManager.persist(aAnnotationDocument);
        }
        else {
            entityManager.merge(aAnnotationDocument);
        }
    }

    /**
     * Renames a file.
     *
     * @throws IOException
     *             if the file cannot be renamed.
     * @return the target file.
     */
    private File renameFile(File aFrom, File aTo)
        throws IOException
    {
        if (!aFrom.renameTo(aTo)) {
            throw new IOException("Cannot renamed file [" + aFrom + "] to [" + aTo + "]");
        }

        // We are not sure if File is mutable. This makes sure we get a new file in any case.
        return new File(aTo.getPath());
    }

    /**
     * Get the folder where the annotations are stored. Creates the folder if necessary.
     *
     * @throws IOException
     *             if the folder cannot be created.
     */
    private File getAnnotationFolder(SourceDocument aDocument)
        throws IOException
    {
        File annotationFolder = new File(dir, PROJECT + aDocument.getProject().getId() + DOCUMENT
                + aDocument.getId() + ANNOTATION);
        FileUtils.forceMkdir(annotationFolder);
        return annotationFolder;
    }

    @Override
    @Transactional
    public void createAnnotationDocumentContent(JCas aJcas, SourceDocument aDocument, User aUser)
        throws IOException
    {

        createAnnotationContent(aDocument, aJcas, aUser.getUsername(), aUser);
    }

    @Override
    @Transactional
    public void createProject(Project aProject, User aUser)
        throws IOException
    {
        entityManager.persist(aProject);
        String path = dir.getAbsolutePath() + PROJECT + aProject.getId();
        FileUtils.forceMkdir(new File(path));
        createLog(aProject, aUser.getUsername())
                .info(" Created  Project [" + aProject.getName() + "] with ID [" + aProject.getId()
                        + "]");
        createLog(aProject, aUser.getUsername()).removeAllAppenders();
    }

    @Override
    @Transactional
    public void createCrowdJob(CrowdJob aCrowdJob)
    {
        if (aCrowdJob.getId() == 0) {
            entityManager.persist(aCrowdJob);
        }
        else {
            entityManager.merge(aCrowdJob);
        }
    }

    @Override
    @Transactional
    public void createProjectPermission(ProjectPermission aPermission)
        throws IOException
    {
        entityManager.persist(aPermission);
        createLog(aPermission.getProject(), aPermission.getUser()).info(
                " New Permission created on Project[" + aPermission.getProject().getName()
                        + "] for user [" + aPermission.getUser() + "] with permission ["
                        + aPermission.getLevel() + "]" + "]");
        createLog(aPermission.getProject(), aPermission.getUser()).removeAllAppenders();
    }

    @Override
    @Transactional
    public void createSourceDocument(SourceDocument aDocument, User aUser)
        throws IOException
    {
        if (aDocument.getId() == 0) {
            entityManager.persist(aDocument);
        }
        else {
            entityManager.merge(aDocument);
        }

    }

    @Override
    @Transactional
    public boolean existsAnnotationDocument(SourceDocument aDocument, User aUser)
    {
        try {
            entityManager
                    .createQuery(
                            "FROM AnnotationDocument WHERE project = :project "
                                    + " AND document = :document AND user = :user",
                            AnnotationDocument.class)
                    .setParameter("project", aDocument.getProject())
                    .setParameter("document", aDocument).setParameter("user", aUser.getUsername())
                    .getSingleResult();
            return true;
        }
        catch (NoResultException ex) {
            return false;
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
    @Transactional
    public boolean existsCrowdJob(String aName)
    {
        try {
            entityManager.createQuery("FROM CrowdJob WHERE name = :name", CrowdJob.class)
                    .setParameter("name", aName).getSingleResult();
            return true;
        }
        catch (NoResultException ex) {
            return false;
        }
    }

    @Override
    public boolean existProjectPermission(User aUser, Project aProject)
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
    public boolean existProjectPermissionLevel(User aUser, Project aProject, PermissionLevel aLevel)
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
    public boolean existSourceDocument(Project aProject, String aFileName)
    {
        try {
            entityManager
                    .createQuery(
                            "FROM SourceDocument WHERE project = :project AND " + "name =:name ",
                            SourceDocument.class).setParameter("project", aProject)
                    .setParameter("name", aFileName).getSingleResult();
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

            Date timestamp = entityManager
                    .createQuery(
                            "SELECT max(timestamp) FROM AnnotationDocument WHERE project = :project "
                                    + " AND user = :user", Date.class)
                    .setParameter("project", aProject).setParameter("user", aUsername)
                    .getSingleResult();
            if (timestamp != null) {
                return true;
            }
            else {
                return false;
            }
        }
        catch (NoResultException ex) {
            return false;
        }
    }

    @Override
    public boolean existUser(String username)
    {

        try {
            getUser(username);
            return true;
        }
        catch (NoResultException e) {
            return false;
        }
    }

    /**
     * A new directory is created using UUID so that every exported file will reside in its own
     * directory. This is useful as the written file can have multiple extensions based on the
     * Writer class used.
     */

    @Override
    @Transactional
    public File exportAnnotationDocument(SourceDocument aDocument, Project aProject, String aUser,
            Class aWriter, String aFileName, Mode aMode)
        throws UIMAException, IOException, WLFormatException, ClassNotFoundException
    {
        File exportTempDir = File.createTempFile("webanno", "export");
        exportTempDir.delete();
        exportTempDir.mkdirs();

        File annotationFolder = getAnnotationFolder(aDocument);
        String serializedCaseFileName;
        if (aMode.equals(Mode.ANNOTATION)) {
            serializedCaseFileName = aUser + ".ser";
        }
        else {
            serializedCaseFileName = CURATION_USER + ".ser";
        }

        CollectionReader reader = CollectionReaderFactory
                .createCollectionReader(SerializedCasReader.class, SerializedCasReader.PARAM_PATH,
                        annotationFolder, SerializedCasReader.PARAM_PATTERNS, new String[] { "[+]"
                                + serializedCaseFileName });
        if (!reader.hasNext()) {
            throw new FileNotFoundException("Annotation file [" + serializedCaseFileName
                    + "] not found in [" + annotationFolder + "]");
        }

        AnalysisEngineDescription writer = createPrimitiveDescription(aWriter,
                JCasFileWriter_ImplBase.PARAM_PATH, exportTempDir,
                JCasFileWriter_ImplBase.PARAM_STRIP_EXTENSION, true);

        CAS cas = JCasFactory.createJCas().getCas();
        reader.getNext(cas);
        // Get the original TCF file and preserve it
        DocumentMetaData documentMetadata = DocumentMetaData.get(cas.getJCas());
        // Update the source file name in case it is changed for some reason

        File currentDocumentUri = new File(dir.getAbsolutePath() + PROJECT + aProject.getId()
                + DOCUMENT + aDocument.getId() + SOURCE);

        documentMetadata.setDocumentUri(new File(currentDocumentUri, aFileName).toURI().toURL()
                .toExternalForm());

        documentMetadata.setDocumentBaseUri(currentDocumentUri.toURI().toURL().toExternalForm());

        documentMetadata.setCollectionId(currentDocumentUri.toURI().toURL().toExternalForm());

        documentMetadata.setDocumentUri(new File(dir.getAbsolutePath() + PROJECT + aProject.getId()
                + DOCUMENT + aDocument.getId() + SOURCE + "/" + aFileName).toURI().toURL()
                .toExternalForm());
        runPipeline(cas, writer);

        createLog(aProject, aUser).info(
                " Exported file [" + aDocument.getName() + "] with ID [" + aDocument.getId()
                        + "] from Project[" + aProject.getId() + "]");
        createLog(aProject, aUser).removeAllAppenders();

        if (exportTempDir.listFiles().length > 1) {
            try {
                DaoUtils.zipFolder(exportTempDir,
                        new File(exportTempDir.getAbsolutePath() + ".zip"));
            }
            catch (Exception e) {
                createLog(aProject, aUser).info("Unable to create Zip File");
            }
            return new File(exportTempDir.getAbsolutePath() + ".zip");
        }
        return exportTempDir.listFiles()[0];

    }

    @Override
    public File exportSourceDocument(SourceDocument aDocument, Project aProject)
    {
        File documentUri = new File(dir.getAbsolutePath() + PROJECT + aProject.getId() + DOCUMENT
                + aDocument.getId() + SOURCE);
        return new File(documentUri, aDocument.getName());
    }

    @Override
    public File exportAnnotationDocument(SourceDocument aDocument, Project aProject, String aUser)
    {
        File documentUri = new File(dir.getAbsolutePath() + PROJECT + aProject.getId() + DOCUMENT
                + aDocument.getId() + ANNOTATION);
        return new File(documentUri, aUser + ".ser");
    }

    @Override
    public File exportProjectLog(Project aProject)
    {
        return new File(dir.getAbsolutePath() + PROJECT + "project-" + aProject.getId() + ".log");
    }

    @Override
    public File exportGuideLines(Project aProject)
    {
        return new File(dir.getAbsolutePath() + PROJECT + aProject.getId() + GUIDELINE);
    }

    @Override
    public File exportProjectMetaInf(Project aProject)
    {
        return new File(dir.getAbsolutePath() + PROJECT + aProject.getId() + META_INF);
    }

    @Override
    @Transactional(noRollbackFor = NoResultException.class)
    public AnnotationDocument getAnnotationDocument(SourceDocument aDocument, User aUser)
    {

        return entityManager
                .createQuery(
                        "FROM AnnotationDocument WHERE document = :document AND " + "user =:user"
                                + " AND project = :project", AnnotationDocument.class)
                .setParameter("document", aDocument).setParameter("user", aUser.getUsername())
                .setParameter("project", aDocument.getProject()).getSingleResult();
    }

    @Override
    @Transactional
    public JCas getAnnotationDocumentContent(AnnotationDocument aAnnotationDocument)
        throws IOException, UIMAException, ClassNotFoundException
    {
        return getAnnotationContent(aAnnotationDocument.getDocument(),
                aAnnotationDocument.getUser());
    }

    @Override
    @Transactional(noRollbackFor = NoResultException.class)
    public List<Authority> listAuthorities(User aUser)
    {
        return entityManager.createQuery("FROM Authority where user =:user", Authority.class)
                .setParameter("user", aUser).getResultList();
    }

    @Override
    public File getDir()
    {
        return dir;
    }

    @Override
    public File getGuideline(Project aProject, String aFilename)
    {
        return new File(dir.getAbsolutePath() + PROJECT + aProject.getId() + GUIDELINE + aFilename);
    }

    @Override
    public File getTemplate(String fileName)
        throws IOException
    {
        FileUtils.forceMkdir(new File(dir.getAbsolutePath() + TEMPLATE));
        return new File(dir.getAbsolutePath() + TEMPLATE, fileName);
    }

    @Override
    @Transactional(noRollbackFor = NoResultException.class)
    public ProjectPermission getPermisionLevel(User aUser, Project aProject)
    {
        return entityManager
                .createQuery("FROM ProjectPermission WHERE user =:user AND " + "project =:project",
                        ProjectPermission.class).setParameter("user", aUser.getUsername())
                .setParameter("project", aProject).getSingleResult();
    }

    @Override
    @Transactional(noRollbackFor = NoResultException.class)
    @SuppressWarnings("unchecked")
    public List<ProjectPermission> listProjectPermisionLevel(User aUser, Project aProject)
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
            if (existUser(username)) {
                users.add(getUser(username));
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
            if (existUser(username)) {
                users.add(getUser(username));
            }
        }
        return users;
    }

    @Override
    @Transactional
    public ProjectPermission getProjectPermission(User aUser, Project aProject)
    {
        return entityManager
                .createQuery("FROM ProjectPermission WHERE user =:user AND " + "project =:project",
                        ProjectPermission.class).setParameter("user", aUser.getUsername())
                .setParameter("project", aProject).getSingleResult();
    }

    @Override
    @Transactional
    public Project getProject(String aName)
    {
        return entityManager.createQuery("FROM Project WHERE name = :name", Project.class)
                .setParameter("name", aName).getSingleResult();
    }

    @Override
    @Transactional
    public CrowdJob getCrowdJob(String aName)
    {
        return entityManager.createQuery("FROM CrowdJob WHERE name = :name", CrowdJob.class)
                .setParameter("name", aName).getSingleResult();
    }

    @Override
    public Project getProject(long aId)
    {
        return entityManager.createQuery("FROM Project WHERE id = :id", Project.class)
                .setParameter("id", aId).getSingleResult();
    }

    @Override
    public void writeGuideline(Project aProject, File aContent, String aFileName)
        throws IOException
    {
        String guidelinePath = dir.getAbsolutePath() + PROJECT + aProject.getId() + GUIDELINE;
        FileUtils.forceMkdir(new File(guidelinePath));
        copyLarge(new FileInputStream(aContent), new FileOutputStream(new File(guidelinePath
                + aFileName)));
    }

    @Override
    @Transactional(noRollbackFor = NoResultException.class)
    public List<ProjectPermission> getProjectPermisions(Project aProject)
    {
        return entityManager
                .createQuery("FROM ProjectPermission WHERE project =:project",
                        ProjectPermission.class).setParameter("project", aProject).getResultList();
    }

    @Override
    @Transactional(noRollbackFor = NoResultException.class)
    public SourceDocument getSourceDocument(String aDocumentName, Project aProject)
    {

        return entityManager
                .createQuery("FROM SourceDocument WHERE name = :name AND project =:project",
                        SourceDocument.class).setParameter("name", aDocumentName)
                .setParameter("project", aProject).getSingleResult();
    }

    @Override
    @Transactional
    public File getSourceDocumentContent(Project aProject, SourceDocument aDocument)
    {
        String path = dir.getAbsolutePath() + PROJECT + aProject.getId() + DOCUMENT
                + aDocument.getId() + SOURCE;
        return new File(path + "/" + aDocument.getName());
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
    @Transactional(noRollbackFor = NoResultException.class)
    public User getUser(String aUsername)
    {
        return entityManager.createQuery("FROM User WHERE username =:username", User.class)
                .setParameter("username", aUsername).getSingleResult();
    }

    @Override
    @Transactional(noRollbackFor = NoResultException.class)
    public boolean existsFinishedAnnotation(SourceDocument aDocument, Project aProject)
    {
        List<AnnotationDocument> annotationDocuments = entityManager
                .createQuery(
                        "FROM AnnotationDocument WHERE document = :document AND "
                                + "project = :project", AnnotationDocument.class)
                .setParameter("document", aDocument).setParameter("project", aProject)
                .getResultList();
        for (AnnotationDocument annotationDocument : annotationDocuments) {
            if (annotationDocument.getState().equals(AnnotationDocumentState.FINISHED)) {
                return true;
            }
        }

        return false;
    }

    @Override
    @Transactional(noRollbackFor = NoResultException.class)
    public boolean isAnnotationFinished(SourceDocument aDocument, Project aProject, User aUser)
    {
        try {

            AnnotationDocument annotationDocument = entityManager
                    .createQuery(
                            "FROM AnnotationDocument WHERE document = :document AND "
                                    + "project = :project AND user =:user",
                            AnnotationDocument.class).setParameter("document", aDocument)
                    .setParameter("project", aProject).setParameter("user", aUser.getUsername())
                    .getSingleResult();
            if (annotationDocument.getState().equals(AnnotationDocumentState.FINISHED)) {
                return true;
            }
            else {
                return false;
            }
        }
        // User even didn't start annotating
        catch (NoResultException e) {
            return false;
        }
    }

    @Override
    @Transactional(noRollbackFor = NoResultException.class)
    public List<AnnotationDocument> listAnnotationDocument(Project aProject,
            SourceDocument aDocument)
    {
        // Get all annotators in the project
        List<String> users = entityManager
                .createQuery(
                        "SELECT DISTINCT user FROM ProjectPermission WHERE project = :project "
                                + "AND level = :level", String.class)
                .setParameter("project", aProject).setParameter("level", PermissionLevel.USER)
                .getResultList();

        // Bail out already. HQL doesn't seem to like queries with an empty parameter right of "in"
        if (users.isEmpty()) {
            return new ArrayList<AnnotationDocument>();
        }

        return entityManager
                .createQuery(
                        "FROM AnnotationDocument WHERE project = :project AND document = :document "
                                + "AND user in (:users)", AnnotationDocument.class)
                .setParameter("project", aProject).setParameter("users", users)
                .setParameter("document", aDocument).getResultList();
    }

    @Override
    @Transactional(noRollbackFor = NoResultException.class)
    public List<AnnotationDocument> listAnnotationDocument(SourceDocument aSourceDocument)
    {
        return entityManager
                .createQuery(
                        "FROM AnnotationDocument WHERE project = :project AND document = :document",
                        AnnotationDocument.class)
                .setParameter("project", aSourceDocument.getProject())
                .setParameter("document", aSourceDocument).getResultList();
    }

    @Override
    public List<String> listAnnotationGuidelineDocument(Project aProject)
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
        return entityManager.createQuery("FROM Project", Project.class).getResultList();
    }

    @Override
    @Transactional
    public List<CrowdJob> listCrowdJobs()
    {
        return entityManager.createQuery("FROM CrowdJob", CrowdJob.class).getResultList();
    }

    @Override
    @Transactional
    public List<CrowdJob> listCrowdJobs(Project aProject)
    {
        return entityManager.createQuery("FROM CrowdJob where project =:project", CrowdJob.class)
                .setParameter("project", aProject).getResultList();
    }

    @Override
    @Transactional(noRollbackFor = NoResultException.class)
    public List<SourceDocument> listSourceDocuments(Project aProject)
    {
        return entityManager
                .createQuery("FROM SourceDocument where project =:project", SourceDocument.class)
                .setParameter("project", aProject).getResultList();
    }

    @Override
    @Transactional
    public List<User> listUsers()
    {
        return entityManager.createQuery("FROM User", User.class).getResultList();
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
    public void removeProject(Project aProject, User aUser)
        throws IOException
    {

        for (SourceDocument document : listSourceDocuments(aProject)) {
            removeSourceDocument(document, aUser);
        }

        for (TagSet tagset : annotationService.listTagSets(aProject)) {
            annotationService.removeTagSet(tagset);
        }
        // remove a timestamp entry for this project, if exists
        for (User user : listProjectUsersWithPermissions(aProject)) {
            if (existsProjectTimeStamp(aProject, user.getUsername())) {
                entityManager.remove(getProjectTimeStamp(aProject, user.getUsername()));
            }
        }
        // timestamp for the curator
        if (existsProjectTimeStamp(aProject, CURATION_USER)) {
            entityManager.remove(getProjectTimeStamp(aProject, CURATION_USER));
        }
        // remove the project directory from the file system
        String path = dir.getAbsolutePath() + PROJECT + aProject.getId();
        try {
            FileUtils.forceDelete(new File(path));
        }
        catch (FileNotFoundException e) {
            createLog(aProject, aUser.getUsername()).warn(
                    "Project directory to be deleted was not found: [" + path + "]. Ignoring.");
        }

        for (ProjectPermission permisions : getProjectPermisions(aProject)) {
            entityManager.remove(permisions);
        }
        // remove metadata from DB
        entityManager.remove(aProject);
        createLog(aProject, aUser.getUsername()).info(
                " Removed Project [" + aProject.getName() + "] with ID [" + aProject.getId() + "]");
        createLog(aProject, aUser.getUsername()).removeAllAppenders();

    }

    @Override
    @Transactional
    public void removeCrowdJob(CrowdJob crowdProject)
    {
        entityManager.remove(entityManager.merge(crowdProject));
    }

    @Override
    public void removeAnnotationGuideline(Project aProject, String aFileName)
        throws IOException
    {
        FileUtils.forceDelete(new File(dir.getAbsolutePath() + PROJECT + aProject.getId()
                + GUIDELINE + aFileName));
    }

    @Override
    public void removeCurationDocumentContent(SourceDocument aSourceDocument)
        throws IOException
    {
        if (new File(getAnnotationFolder(aSourceDocument), CURATION_USER + ".ser").exists()) {
            FileUtils.forceDelete(new File(getAnnotationFolder(aSourceDocument), CURATION_USER
                    + ".ser"));
        }
    }

    @Override
    @Transactional
    public void removeProjectPermission(ProjectPermission projectPermission)
        throws IOException
    {
        entityManager.remove(projectPermission);
        createLog(projectPermission.getProject(), projectPermission.getUser()).info(
                " Removed Project Permission [" + projectPermission.getLevel() + "] for the USer ["
                        + projectPermission.getUser() + "] From project ["
                        + projectPermission.getProject().getId() + "]");
        createLog(projectPermission.getProject(), projectPermission.getUser()).removeAllAppenders();

    }

    @Override
    @Transactional
    public void removeSourceDocument(SourceDocument aDocument, User aUser)
        throws IOException
    {

        for (AnnotationDocument annotationDocument : listAnnotationDocument(aDocument)) {
            removeAnnotationDocument(annotationDocument);
        }
        // remove it from the crowd job, if it belongs already
        for (CrowdJob crowdJob : listCrowdJobs(aDocument.getProject())) {
            if (crowdJob.getDocuments().contains(aDocument)) {
                crowdJob.getDocuments().remove(aDocument);
                entityManager.persist(crowdJob);
            }
        }

        entityManager.remove(aDocument);

        String path = dir.getAbsolutePath() + PROJECT + aDocument.getProject().getId() + DOCUMENT
                + aDocument.getId();
        // remove from file both source and related annotation file
        if (new File(path).exists()) {
            FileUtils.forceDelete(new File(path));
        }
        createLog(aDocument.getProject(), aUser.getUsername()).info(
                " Removed Document [" + aDocument.getName() + "] with ID [" + aDocument.getId()
                        + "] from Project [" + aDocument.getProject().getId() + "]");
        createLog(aDocument.getProject(), aUser.getUsername()).removeAllAppenders();

    }

    @Override
    @Transactional
    public void removeAnnotationDocument(AnnotationDocument aAnnotationDocument)
    {
        entityManager.remove(aAnnotationDocument);
    }

    public void setDir(File aDir)
    {
        dir = aDir;
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
            property.setProperty(aSubject + "." + value.getName(),
                    wrapper.getPropertyValue(value.getName()).toString());
        }
        String propertiesPath = dir.getAbsolutePath() + PROJECT + aProject.getId() + SETTINGS
                + aUsername;
        // append existing preferences for the other mode
        if (new File(propertiesPath, annotationPreferencePropertiesFileName).exists()) {
            // aSubject = aSubject.equals(Mode.ANNOTATION) ? Mode.CURATION : Mode.ANNOTATION;
            for (Entry<Object, Object> entry : loadUserSettings(aUsername, aProject).entrySet()) {
                String key = entry.getKey().toString();
                // Maintain other Modes of annotations confs than this one
                if (!key.substring(0, key.indexOf(".")).equals(aSubject.toString())) {
                    property.put(entry.getKey(), entry.getValue());
                }
            }
        }
        FileUtils
                .forceDeleteOnExit(new File(propertiesPath, annotationPreferencePropertiesFileName));
        FileUtils.forceMkdir(new File(propertiesPath));
        property.save(new FileOutputStream(new File(propertiesPath,
                annotationPreferencePropertiesFileName)), null);

        createLog(aProject, aUsername).info(
                " Saved preferences file [" + annotationPreferencePropertiesFileName
                        + "] for project [" + aProject.getName() + "] with ID [" + aProject.getId()
                        + "] to location: [" + propertiesPath + "]");
        createLog(aProject, aUsername).removeAllAppenders();

    }

    @Override
    @Transactional
    public void uploadSourceDocument(File aFile, SourceDocument aDocument, long aProjectId,
            User aUser)
        throws IOException
    {
        String path = dir.getAbsolutePath() + PROJECT + aProjectId + DOCUMENT + aDocument.getId()
                + SOURCE;
        FileUtils.forceMkdir(new File(path));
        File newTcfFile = new File(path, aDocument.getName());

        InputStream is = null;
        OutputStream os = null;
        try {
            os = new FileOutputStream(newTcfFile);
            is = new FileInputStream(aFile);
            copyLarge(is, os);
        }
        finally {
            closeQuietly(os);
            closeQuietly(is);
        }

        createLog(aDocument.getProject(), aUser.getUsername()).info(
                " Imported file [" + aDocument.getName() + "] with ID [" + aDocument.getId()
                        + "] to Project [" + aDocument.getProject().getId() + "]");
        createLog(aDocument.getProject(), aUser.getUsername()).removeAllAppenders();

    }

    @Override
    @Transactional
    public void uploadSourceDocument(InputStream aIs, SourceDocument aDocument, long aProjectId,
            User aUser)
        throws IOException
    {
        String path = dir.getAbsolutePath() + PROJECT + aProjectId + DOCUMENT + aDocument.getId()
                + SOURCE;
        FileUtils.forceMkdir(new File(path));
        File newTcfFile = new File(path, aDocument.getName());

        OutputStream os = null;
        try {
            os = new FileOutputStream(newTcfFile);
            copyLarge(aIs, os);
        }
        finally {
            closeQuietly(os);
            closeQuietly(aIs);
        }

        createLog(aDocument.getProject(), aUser.getUsername()).info(
                " Imported file [" + aDocument.getName() + "] with ID [" + aDocument.getId()
                        + "] to Project [" + aDocument.getProject().getId() + "]");
        createLog(aDocument.getProject(), aUser.getUsername()).removeAllAppenders();

    }

    private void writeContent(SourceDocument aDocument, JCas aJcas, String aUsername)
        throws IOException
    {
        try {
            File targetPath = getAnnotationFolder(aDocument);
            AnalysisEngine writer = AnalysisEngineFactory.createPrimitive(
                    SerializedCasWriter.class, SerializedCasWriter.PARAM_PATH, targetPath,
                    SerializedCasWriter.PARAM_USE_DOCUMENT_ID, true);
            DocumentMetaData md;
            try {
                md = DocumentMetaData.get(aJcas);
            }
            catch (IllegalArgumentException e) {
                md = DocumentMetaData.create(aJcas);
            }
            md.setDocumentId(aUsername);
            writer.process(aJcas);
        }
        catch (ResourceInitializationException e) {
            throw new IOException(e);
        }
        catch (AnalysisEngineProcessException e) {
            throw new IOException(e);
        }
    }

    @Override
    public List<String> getReadableFormatsLabel()
        throws ClassNotFoundException
    {
        List<String> readableFormats = new ArrayList<String>();
        Set<String> key = (Set) readWriteFileFormats.keySet();

        for (String keyvalue : key) {
            if (keyvalue.contains(".label")) {
                String readerLabel = keyvalue.substring(0, keyvalue.lastIndexOf(".label"));
                if (readWriteFileFormats.getProperty(readerLabel + ".reader") != null) {
                    readableFormats.add(readWriteFileFormats.getProperty(keyvalue));
                }
            }
        }
        return readableFormats;
    }

    @Override
    public String getReadableFormatId(String aLabel)
        throws ClassNotFoundException
    {
        Set<String> key = (Set) readWriteFileFormats.keySet();
        String readableFormat = "";
        for (String keyvalue : key) {
            if (keyvalue.contains(".label")) {

                if (readWriteFileFormats.getProperty(keyvalue).equals(aLabel)) {
                    readableFormat = keyvalue.substring(0, keyvalue.lastIndexOf(".label"));
                    break;
                }
            }
        }
        return readableFormat;
    }

    @Override
    public Map<String, Class> getReadableFormats()
        throws ClassNotFoundException
    {
        Map<String, Class> readableFormats = new HashMap<String, Class>();
        Set<String> key = (Set) readWriteFileFormats.keySet();

        for (String keyvalue : key) {
            if (keyvalue.contains(".label")) {
                String readerLabel = keyvalue.substring(0, keyvalue.lastIndexOf(".label"));
                if (readWriteFileFormats.getProperty(readerLabel + ".reader") != null) {
                    readableFormats.put(readerLabel, Class.forName(readWriteFileFormats
                            .getProperty(readerLabel + ".reader")));
                }
            }
        }
        return readableFormats;
    }

    @Override
    public List<String> getWritableFormatsLabel()
        throws ClassNotFoundException
    {
        List<String> writableFormats = new ArrayList<String>();
        Set<String> keys = (Set) readWriteFileFormats.keySet();

        for (String keyvalue : keys) {
            if (keyvalue.contains(".label")) {
                String writerLabel = keyvalue.substring(0, keyvalue.lastIndexOf(".label"));
                if (readWriteFileFormats.getProperty(writerLabel + ".writer") != null) {
                    writableFormats.add(readWriteFileFormats.getProperty(keyvalue));
                }
            }
        }
        return writableFormats;
    }

    @Override
    public String getWritableFormatId(String aLabel)
        throws ClassNotFoundException
    {
        Set<String> keys = (Set) readWriteFileFormats.keySet();
        String writableFormat = "";
        for (String keyvalue : keys) {
            if (keyvalue.contains(".label")) {
                if (readWriteFileFormats.getProperty(keyvalue).equals(aLabel)) {
                    writableFormat = keyvalue.substring(0, keyvalue.lastIndexOf(".label"));
                    break;
                }
            }
        }
        return writableFormat;
    }

    @Override
    public Map<String, Class> getWritableFormats()
        throws ClassNotFoundException
    {
        Map<String, Class> writableFormats = new HashMap<String, Class>();
        Set<String> keys = (Set) readWriteFileFormats.keySet();

        for (String keyvalue : keys) {
            if (keyvalue.contains(".label")) {
                String writerLabel = keyvalue.substring(0, keyvalue.lastIndexOf(".label"));
                if (readWriteFileFormats.getProperty(writerLabel + ".writer") != null) {
                    writableFormats.put(writerLabel, Class.forName(readWriteFileFormats
                            .getProperty(writerLabel + ".writer")));
                }
            }
        }
        return writableFormats;
    }

    public String getAnnotationPreferencePropertiesFileName()
    {
        return annotationPreferencePropertiesFileName;
    }

    public void setAnnotationPreferencePropertiesFileName(
            String aAnnotationPreferencePropertiesFileName)
    {
        annotationPreferencePropertiesFileName = aAnnotationPreferencePropertiesFileName;
    }

    @Override
    @PreAuthorize("hasAnyRole('ROLE_ADMIN','ROLE_USER')")
    public void createCorrectionDocumentContent(JCas aJcas, SourceDocument aDocument, User aUser)
        throws IOException
    {
        createAnnotationContent(aDocument, aJcas, CORRECTION_USER, aUser);
    }

    @Override
    @PreAuthorize("hasAnyRole('ROLE_ADMIN','ROLE_USER')")
    public void createCurationDocumentContent(JCas aJcas, SourceDocument aDocument, User aUser)
        throws IOException
    {
        createAnnotationContent(aDocument, aJcas, CURATION_USER, aUser);
    }

    @Override
    public JCas getCorrectionDocumentContent(SourceDocument aDocument)
        throws UIMAException, IOException, ClassNotFoundException
    {
        return getAnnotationContent(aDocument, CORRECTION_USER);
    }

    @Override
    public JCas getCurationDocumentContent(SourceDocument aDocument)
        throws UIMAException, IOException, ClassNotFoundException
    {

        return getAnnotationContent(aDocument, CURATION_USER);
    }

    /**
     * Creates an annotation document (either user's annotation document or CURATION_USER's
     * annotation document)
     *
     * @param aDocument
     *            the {@link SourceDocument}
     * @param aJcas
     *            The annotated CAS object
     * @param aUserName
     *            the user who annotates the document if it is user's annotation document OR the
     *            CURATION_USER
     * @param aUser
     *            The user who annotates the document OR the curator who curates the document
     * @throws IOException
     */

    private void createAnnotationContent(SourceDocument aDocument, JCas aJcas, String aUserName,
            User aUser)
        throws IOException
    {
        synchronized (lock) {
            File annotationFolder = getAnnotationFolder(aDocument);
            FileUtils.forceMkdir(annotationFolder);

            final String username = aUserName;

            File currentVersion = new File(annotationFolder, username + ".ser");
            File oldVersion = new File(annotationFolder, username + ".ser.old");

            // Save current version
            try {
                // Make a backup of the current version of the file before overwriting
                if (currentVersion.exists()) {
                    renameFile(currentVersion, oldVersion);
                }

                // Now write the new version to "<username>.ser" or CURATION_USER.ser
                writeContent(aDocument, aJcas, aUserName);
                createLog(aDocument.getProject(), aUser.getUsername()).info(
                        " Updated annotation file [" + aDocument.getName() + "] " + "with ID ["
                                + aDocument.getId() + "] in project ID ["
                                + aDocument.getProject().getId() + "]");
                createLog(aDocument.getProject(), aUser.getUsername()).removeAllAppenders();

                // If the saving was successful, we delete the old version
                if (oldVersion.exists()) {
                    FileUtils.forceDelete(oldVersion);
                }
            }
            catch (IOException e) {
                // If we could not save the new version, restore the old one.
                FileUtils.forceDelete(currentVersion);
                // If this is the first version, there is no old version, so do not restore anything
                if (oldVersion.exists()) {
                    renameFile(oldVersion, currentVersion);
                }
                // Now abort anyway
                throw e;
            }

            // Manage history
            if (backupInterval > 0) {
                // Determine the reference point in time based on the current version
                long now = currentVersion.lastModified();

                // Get all history files for the current user
                File[] history = annotationFolder.listFiles(new FileFilter()
                {
                    private Matcher matcher = Pattern.compile(
                            Pattern.quote(username) + "\\.ser\\.[0-9]+\\.bak").matcher("");

                    @Override
                    public boolean accept(File aFile)
                    {
                        // Check if the filename matches the pattern given above.
                        return matcher.reset(aFile.getName()).matches();
                    }
                });

                // Sort the files (oldest one first)
                Arrays.sort(history, LastModifiedFileComparator.LASTMODIFIED_COMPARATOR);

                // Check if we need to make a new history file
                boolean historyFileCreated = false;
                File historyFile = new File(annotationFolder, username + ".ser." + now + ".bak");
                if (history.length == 0) {
                    // If there is no history yet but we should keep history, then we create a
                    // history file in any case.
                    FileUtils.copyFile(currentVersion, historyFile);
                    historyFileCreated = true;
                }
                else {
                    // Check if the newest history file is significantly older than the current one
                    File latestHistory = history[history.length - 1];
                    if (latestHistory.lastModified() + backupInterval < now) {
                        FileUtils.copyFile(currentVersion, historyFile);
                        historyFileCreated = true;
                    }
                }

                // Prune history based on number of backup
                if (historyFileCreated) {
                    // The new version is not in the history, so we keep that in any case. That
                    // means we need to keep one less.
                    int toKeep = Math.max(backupKeepNumber - 1, 0);
                    if ((backupKeepNumber > 0) && (toKeep < history.length)) {
                        // Copy the oldest files to a new array
                        File[] toRemove = new File[history.length - toKeep];
                        System.arraycopy(history, 0, toRemove, 0, toRemove.length);

                        // Restrict the history to what is left
                        File[] newHistory = new File[toKeep];
                        if (toKeep > 0) {
                            System.arraycopy(history, toRemove.length, newHistory, 0,
                                    newHistory.length);
                        }
                        history = newHistory;

                        // Remove these old files
                        for (File file : toRemove) {
                            FileUtils.forceDelete(file);
                            createLog(aDocument.getProject(), aUser.getUsername()).info(
                                    "Removed surplus history file [" + file.getName() + "] "
                                            + " for document with ID [" + aDocument.getId()
                                            + "] in project ID [" + aDocument.getProject().getId()
                                            + "]");
                            createLog(aDocument.getProject(), aUser.getUsername())
                                    .removeAllAppenders();
                        }
                    }

                    // Prune history based on time
                    if (backupKeepTime > 0) {
                        for (File file : history) {
                            if ((file.lastModified() + backupKeepTime) < now) {
                                FileUtils.forceDelete(file);
                                createLog(aDocument.getProject(), aUser.getUsername()).info(
                                        "Removed outdated history file [" + file.getName() + "] "
                                                + " for document with ID [" + aDocument.getId()
                                                + "] in project ID ["
                                                + aDocument.getProject().getId() + "]");
                                createLog(aDocument.getProject(), aUser.getUsername())
                                        .removeAllAppenders();
                            }
                        }
                    }
                }
            }

            // update timestamp now
            AnnotationDocument annotationDocument = getAnnotationDocument(aDocument, aUser);
            annotationDocument.setTimestamp(new Timestamp(new Date().getTime()));
            createAnnotationDocument(annotationDocument);

        }
    }

    /**
     * For a given {@link SourceDocument}, return the {@link AnnotationDocument} for the user or for
     * the CURATION_USER
     *
     * @param aDocument
     *            the {@link SourceDocument}
     * @param aUsername
     *            the {@link User} who annotates the {@link SourceDocument} or the CURATION_USER
     */
    private JCas getAnnotationContent(SourceDocument aDocument, String aUsername)
        throws IOException
    {
        synchronized (lock) {

            File annotationFolder = getAnnotationFolder(aDocument);

            String file = aUsername + ".ser";

            try {
                CAS cas = JCasFactory.createJCas().getCas();
                CollectionReader reader = CollectionReaderFactory.createCollectionReader(
                        SerializedCasReader.class, SerializedCasReader.PARAM_PATH,
                        annotationFolder, SerializedCasReader.PARAM_PATTERNS, new String[] { "[+]"
                                + file });
                if (!reader.hasNext()) {
                    throw new FileNotFoundException("Annotation document of user [" + aUsername
                            + "] for source document [" + aDocument.getName() + "] ("
                            + aDocument.getId() + "). not found in project["
                            + aDocument.getProject().getName() + "] ("
                            + aDocument.getProject().getId() + ")");
                }
                reader.getNext(cas);

                return cas.getJCas();
            }
            catch (IOException e) {
                throw new DataRetrievalFailureException("Unable to parse annotation", e);
            }
            catch (UIMAException e) {
                throw new DataRetrievalFailureException("Unable to parse annotation", e);
            }
        }
    }

    @Override
    public boolean isRemoteProject(Project project)
    {
        return new File(dir, PROJECT + project.getId() + META_INF).exists();
    }
}

/*******************************************************************************
 * Copyright 2012
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 ******************************************************************************/
package de.tudarmstadt.ukp.clarin.webanno.brat.dao;

import static org.apache.commons.io.IOUtils.closeQuietly;
import static org.apache.commons.io.IOUtils.copyLarge;
import static org.uimafit.factory.AnalysisEngineFactory.createPrimitiveDescription;
import static org.uimafit.pipeline.SimplePipeline.runPipeline;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileFilter;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import javax.annotation.Resource;
import javax.persistence.EntityManager;
import javax.persistence.NoResultException;
import javax.persistence.PersistenceContext;

import org.apache.commons.io.FileUtils;
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
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DataRetrievalFailureException;
import org.springframework.transaction.annotation.Transactional;
import org.uimafit.factory.AnalysisEngineFactory;
import org.uimafit.factory.CollectionReaderFactory;
import org.uimafit.factory.JCasFactory;

import de.tudarmstadt.ukp.clarin.webanno.api.AnnotationService;
import de.tudarmstadt.ukp.clarin.webanno.api.RepositoryService;
import de.tudarmstadt.ukp.clarin.webanno.brat.controller.BratAjaxCasUtil;
import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationDocument;
import de.tudarmstadt.ukp.clarin.webanno.model.Authority;
import de.tudarmstadt.ukp.clarin.webanno.model.Project;
import de.tudarmstadt.ukp.clarin.webanno.model.ProjectPermissions;
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
    public static Logger createLog(Project aProject, User aUser)
        throws IOException
    {
        Logger logger = Logger.getLogger(RepositoryService.class);
        String targetLog = dir.getAbsolutePath() + PROJECT + "project-" + aProject.getId() + ".log";
        FileAppender apndr = new FileAppender(new PatternLayout("%d [" + aUser.getUsername()
                + "] %m%n"), targetLog, true);
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

    private static final String ANNOTATION = "/annotation";

    @PersistenceContext
    private EntityManager entityManager;

    private static File dir;

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

        entityManager.persist(aAnnotationDocument);
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
    private File getAnnotationFolder(AnnotationDocument aAnnotationDocument)
        throws IOException
    {
        File annotationFolder = new File(dir, PROJECT + aAnnotationDocument.getProject().getId()
                + DOCUMENT + aAnnotationDocument.getDocument().getId() + ANNOTATION);
        FileUtils.forceMkdir(annotationFolder);
        return annotationFolder;
    }

    @Override
    @Transactional
    public void createAnnotationDocumentContent(JCas aJcas, AnnotationDocument aAnnotationDocument,
            User aUser)
        throws IOException
    {
        synchronized (lock) {
            File annotationFolder = getAnnotationFolder(aAnnotationDocument);
            FileUtils.forceMkdir(annotationFolder);

            final String username = aAnnotationDocument.getUser().getUsername();

            File currentVersion = new File(annotationFolder, username + ".ser");
            File oldVersion = new File(annotationFolder, username + ".ser.old");

            // Save current version
            try {
                // Make a backup of the current version of the file before overwriting
                if (currentVersion.exists()) {
                    renameFile(currentVersion, oldVersion);
                }

                // Now write the new version to "<username>.ser"
                writeContent(aAnnotationDocument, aJcas);
                createLog(aAnnotationDocument.getProject(), aUser).info(
                        " Updated annotation file [" + aAnnotationDocument.getName() + "] "
                                + "with ID [" + aAnnotationDocument.getDocument().getId()
                                + "] in project ID [" + aAnnotationDocument.getProject().getId()
                                + "]");
                createLog(aAnnotationDocument.getProject(), aUser).removeAllAppenders();

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
                            createLog(aAnnotationDocument.getProject(), aUser).info(
                                    "Removed surplus history file [" + file.getName() + "] "
                                            + " for document with ID ["
                                            + aAnnotationDocument.getDocument().getId()
                                            + "] in project ID ["
                                            + aAnnotationDocument.getProject().getId() + "]");
                            createLog(aAnnotationDocument.getProject(), aUser).removeAllAppenders();
                        }
                    }

                    // Prune history based on time
                    if (backupKeepTime > 0) {
                        for (File file : history) {
                            if ((file.lastModified() + backupKeepTime) < now) {
                                FileUtils.forceDelete(file);
                                createLog(aAnnotationDocument.getProject(), aUser).info(
                                        "Removed outdated history file [" + file.getName() + "] "
                                                + " for document with ID ["
                                                + aAnnotationDocument.getDocument().getId()
                                                + "] in project ID ["
                                                + aAnnotationDocument.getProject().getId() + "]");
                                createLog(aAnnotationDocument.getProject(), aUser)
                                        .removeAllAppenders();
                            }
                        }
                    }
                }
            }
        }
    }

    @Override
    @Transactional
    public void createProject(Project aProject, User aUser)
        throws IOException
    {
        entityManager.persist(aProject);
        String path = dir.getAbsolutePath() + PROJECT + aProject.getId();
        FileUtils.forceMkdir(new File(path));
        createLog(aProject, aUser)
                .info(" Created  Project [" + aProject.getName() + "] with ID [" + aProject.getId()
                        + "]");
        createLog(aProject, aUser).removeAllAppenders();
    }

    @Override
    @Transactional
    public void createProjectPermission(ProjectPermissions aPermission)
        throws IOException
    {
        entityManager.persist(aPermission);
        createLog(aPermission.getProject(), aPermission.getUser()).info(
                " New Permission created on Project[" + aPermission.getProject().getName()
                        + "] for user [" + aPermission.getUser().getUsername()
                        + "] with permission [" + aPermission.getLevel() + "]" + "]");
        createLog(aPermission.getProject(), aPermission.getUser()).removeAllAppenders();
    }

    @Override
    @Transactional
    public void createSourceDocument(SourceDocument aDocument, User aUser)
        throws IOException
    {

        entityManager.persist(aDocument);
    }

    /**
     * A new directory is created using UUID so that every exported file will reside in its own
     * directory. This is useful as the written file can have multiple extensions based on the
     * Writer class used.
     */

    @Override
    @Transactional
    public File exportAnnotationDocument(SourceDocument aDocument, Project aProject, User aUser,
            Class aWriter)
        throws UIMAException, IOException, WLFormatException, ClassNotFoundException
    {
        File exportTempDir = File.createTempFile("webanno", "export");
        exportTempDir.delete();
        exportTempDir.mkdirs();

        File annotationFolder = getAnnotationFolder(getAnnotationDocument(aDocument, aUser));
        String fileName = aUser.getUsername() + ".ser";

        CollectionReader reader = CollectionReaderFactory.createCollectionReader(
                SerializedCasReader.class, SerializedCasReader.PARAM_PATH, annotationFolder,
                SerializedCasReader.PARAM_PATTERNS, new String[] { "[+]" + fileName });
        if (!reader.hasNext()) {
            throw new FileNotFoundException("Annotation file [" + fileName + "] not found in ["
                    + annotationFolder + "]");
        }

        AnalysisEngineDescription writer = createPrimitiveDescription(aWriter,
                JCasFileWriter_ImplBase.PARAM_PATH, exportTempDir,
                JCasFileWriter_ImplBase.PARAM_STRIP_EXTENSION, true);

        runPipeline(reader, writer);

        createLog(aProject, aUser).info(
                " Exported file [" + aDocument.getName() + "] with ID [" + aDocument.getId()
                        + "] from Project[" + aProject.getId() + "]");
        createLog(aProject, aUser).removeAllAppenders();
        // FIXME make it zip file
        if (exportTempDir.listFiles().length > 1) {
            try {
                zipFolder(exportTempDir.getAbsolutePath(), exportTempDir.getName() + ".zip");
            }
            catch (Exception e) {
                createLog(aProject, aUser).info("Unable to create Zip File");
            }
            return new File(exportTempDir.getName() + ".zip");
        }
        return exportTempDir.listFiles()[0];

    }

    @Override
    @Transactional(noRollbackFor = NoResultException.class)
    public AnnotationDocument getAnnotationDocument(SourceDocument aDocument, User aUser)
    {

        return entityManager
                .createQuery(
                        "FROM AnnotationDocument WHERE document = :document AND " + "user =:user",
                        AnnotationDocument.class).setParameter("document", aDocument)
                .setParameter("user", aUser).getSingleResult();
    }

    @Override
    @Transactional
    public JCas getAnnotationDocumentContent(AnnotationDocument aAnnotationDocument)
        throws IOException, UIMAException, ClassNotFoundException
    {
        synchronized (lock) {

            File annotationFolder = getAnnotationFolder(aAnnotationDocument);

            String file = aAnnotationDocument.getUser().getUsername() + ".ser";

            // if the annotation file has been deleted, for some reason, recreate it
            if (!new File(annotationFolder, file).exists()) {

                JCas jCas = BratAjaxCasUtil.getJCasFromFile(
                        getSourceDocumentContent(aAnnotationDocument.getProject(),
                                aAnnotationDocument.getDocument()),
                        getReadableFormats().get(aAnnotationDocument.getDocument().getFormat()));
                return jCas;
            }
            else {
                try {
                    CAS cas = JCasFactory.createJCas().getCas();
                    CollectionReader reader = CollectionReaderFactory.createCollectionReader(
                            SerializedCasReader.class, SerializedCasReader.PARAM_PATH,
                            annotationFolder, SerializedCasReader.PARAM_PATTERNS,
                            new String[] { "[+]" + file });
                    if (!reader.hasNext()) {
                        throw new FileNotFoundException("Annotation file [" + file
                                + "] not found in [" + annotationFolder + "]");
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
                /*
                 * catch (SAXException e) { throw new
                 * DataRetrievalFailureException("Unable to parse annotation", e); }
                 */
            }
        }
    }

    @Override
    @Transactional(noRollbackFor = NoResultException.class)
    public List<Authority> getAuthorities(User aUser)
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
    @Transactional(noRollbackFor = NoResultException.class)
    public String getPermisionLevel(User aUser, Project aProject)
    {
        return entityManager
                .createQuery(
                        "Select level FROM ProjectPermissions WHERE user =:user AND "
                                + "project =:project", String.class).setParameter("user", aUser)
                .setParameter("project", aProject).getSingleResult();
    }

    @Override
    @Transactional(noRollbackFor = NoResultException.class)
    public List<String> listProjectPermisionLevels(User aUser, Project aProject)
    {
        return entityManager
                .createQuery(
                        "Select level FROM ProjectPermissions WHERE user =:user AND "
                                + "project =:project", String.class).setParameter("user", aUser)
                .setParameter("project", aProject).getResultList();
    }

    @Override
    @Transactional
    public ProjectPermissions getProjectPermission(User aUser, Project aProject)
    {
        return entityManager
                .createQuery(
                        "FROM ProjectPermissions WHERE user =:user AND " + "project =:project",
                        ProjectPermissions.class).setParameter("user", aUser)
                .setParameter("project", aProject).getSingleResult();
    }

    @Override
    @Transactional
    public List<Project> getProjects(String aName)
    {
        return entityManager.createQuery("FROM Project WHERE name = :name", Project.class)
                .setParameter("name", aName).getResultList();
    }

    @Override
    @Transactional(noRollbackFor = NoResultException.class)
    public List<ProjectPermissions> getProjectPermisions(Project aProject)
    {
        return entityManager
                .createQuery("FROM ProjectPermissions WHERE project =:project",
                        ProjectPermissions.class).setParameter("project", aProject).getResultList();
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
    @Transactional(noRollbackFor = NoResultException.class)
    public User getUser(String aUsername)
    {
        return entityManager.createQuery("FROM User WHERE username =:username", User.class)
                .setParameter("username", aUsername).getSingleResult();
    }

    @Override
    @Transactional(noRollbackFor = NoResultException.class)
    public List<AnnotationDocument> listAnnotationDocument(SourceDocument aDocument)
    {
        return entityManager
                .createQuery("FROM AnnotationDocument WHERE document = :document",
                        AnnotationDocument.class).setParameter("document", aDocument)
                .getResultList();
    }

    /*
     * @Override
     *
     * @Transactional public String getAuthority(User aUser) { return entityManager.createQuery(
     * "SELECT role FROM Authority where users =:users", String.class) .setParameter("users",
     * aUser).getSingleResult(); }
     */
    private List<AnnotationDocument> listAnnotationDocuments()
    {
        return entityManager.createQuery("From AnnotationDocument", AnnotationDocument.class)
                .getResultList();
    }

    @Override
    @Transactional
    public List<Project> listProjects()
    {
        return entityManager.createQuery("FROM Project", Project.class).getResultList();
    }

    @Override
    @Transactional
    public List<String> listProjectUsers(Project aproject)
    {
        List<String> users = entityManager
                .createQuery(
                        "SELECT i.username FROM Project s JOIN s.users i WHERE s.id = :projectId",
                        String.class).setParameter("projectId", aproject.getId()).getResultList();
        return users;
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
    @Transactional
    public void removeProject(Project aProject, User aUser)
        throws IOException
    {

        for (SourceDocument document : listSourceDocuments(aProject)) {
            removeSourceDocument(document, aUser);
            // removeAnnotationDocument(document);
        }

        for (TagSet tagset : annotationService.listTagSets(aProject)) {
            annotationService.removeTagSet(tagset);
        }
        // remove the project directory from the file system
        String path = dir.getAbsolutePath() + PROJECT + aProject.getId();
        try {
            FileUtils.forceDelete(new File(path));
        }
        catch (FileNotFoundException e) {
            createLog(aProject, aUser).warn(
                    "Project directory to be deleted was not found: [" + path + "]. Ignoring.");
        }

        for (ProjectPermissions permisions : getProjectPermisions(aProject)) {
            entityManager.remove(permisions);
        }
        // remove metadata from DB
        entityManager.remove(aProject);
        createLog(aProject, aUser).info(
                " Removed Project [" + aProject.getName() + "] with ID [" + aProject.getId() + "]");
        createLog(aProject, aUser).removeAllAppenders();

    }

    @Override
    @Transactional
    public void removeProjectPermission(ProjectPermissions projectPermission)
        throws IOException
    {
        entityManager.remove(projectPermission);
        createLog(projectPermission.getProject(), projectPermission.getUser()).info(
                " Removed Project Permission [" + projectPermission.getLevel() + "] for the USer ["
                        + projectPermission.getUser().getUsername() + "] From project ["
                        + projectPermission.getProject().getId() + "]");
        createLog(projectPermission.getProject(), projectPermission.getUser()).removeAllAppenders();

    }

    @Override
    @Transactional
    public void removeSourceDocument(SourceDocument aDocument, User aUser)
        throws IOException
    {

        // remove metadata from DB
        entityManager.remove(aDocument);
        for (AnnotationDocument annotationDocument : listAnnotationDocuments()) {
            entityManager.remove(annotationDocument);
        }

        String path = dir.getAbsolutePath() + PROJECT + aDocument.getProject().getId() + DOCUMENT
                + aDocument.getId();
        // remove from file both source and related annotation file
        FileUtils.forceDelete(new File(path));
        createLog(aDocument.getProject(), aUser).info(
                " Removed Document [" + aDocument.getName() + "] with ID [" + aDocument.getId()
                        + "] from Project [" + aDocument.getProject().getId() + "]");
        createLog(aDocument.getProject(), aUser).removeAllAppenders();

    }

    public void setDir(File aDir)
    {
        dir = aDir;
    }

    @Override
    @Transactional
    public void uploadSourceDocument(String aText, SourceDocument aDocument, long aProjectId,
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
            is = new ByteArrayInputStream(aText.getBytes("UTF-8"));
            copyLarge(is, os);
        }
        finally {
            closeQuietly(os);
            closeQuietly(is);
        }

        createLog(aDocument.getProject(), aUser).info(
                " Imported file [" + aDocument.getName() + "] with ID [" + aDocument.getId()
                        + "] to Project [" + aDocument.getProject().getId() + "]");
        createLog(aDocument.getProject(), aUser).removeAllAppenders();

    }

    private void writeContent(AnnotationDocument aAnnotationDocument, JCas aJcas)
        throws IOException
    {
        try {
            File targetPath = getAnnotationFolder(aAnnotationDocument);
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
            md.setDocumentId(aAnnotationDocument.getUser().getUsername());
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
    public Map<String, Class> getReadableFormats()
        throws ClassNotFoundException
    {
        Map<String, Class> readableFormats = new HashMap<String, Class>();
        Set<String> key = (Set) readWriteFileFormats.keySet();

        for (String keyvalue : key) {
            if (keyvalue.contains(".label")) {
                String readerLabel = keyvalue.substring(0, keyvalue.lastIndexOf(".label"));
                if (readWriteFileFormats.getProperty(readerLabel + ".reader") != null) {
                    readableFormats.put(readWriteFileFormats.getProperty(keyvalue), Class
                            .forName(readWriteFileFormats.getProperty(readerLabel + ".reader")));
                }
            }
        }
        return readableFormats;
    }

    @Override
    public Map<String, Class> getWritableFormats()
        throws ClassNotFoundException
    {
        Map<String, Class> writableFormats = new HashMap<String, Class>();
        Set<String> keys = (Set) readWriteFileFormats.keySet();

        for (String key : keys) {
            if (key.contains(".label")) {
                String writerLabel = key.substring(0, key.lastIndexOf(".label"));
                if (readWriteFileFormats.getProperty(writerLabel + ".writer") != null) {
                    writableFormats.put(readWriteFileFormats.getProperty(key), Class
                            .forName(readWriteFileFormats.getProperty(writerLabel + ".writer")));
                }
            }
        }
        return writableFormats;
    }

    private void zipFolder(String srcFolder, String destZipFile)
        throws Exception
    {
        ZipOutputStream zip = null;
        FileOutputStream fileWriter = null;

        fileWriter = new FileOutputStream(destZipFile);
        zip = new ZipOutputStream(fileWriter);

        addFolderToZip("", srcFolder, zip);
        zip.flush();
        zip.close();
    }

    private void addFileToZip(String path, String srcFile, ZipOutputStream zip)
        throws Exception
    {

        File folder = new File(srcFile);
        if (folder.isDirectory()) {
            addFolderToZip(path, srcFile, zip);
        }
        else {
            byte[] buf = new byte[1024];
            int len;
            FileInputStream in = new FileInputStream(srcFile);
            zip.putNextEntry(new ZipEntry(path + "/" + folder.getName()));
            while ((len = in.read(buf)) > 0) {
                zip.write(buf, 0, len);
            }
        }
    }

    private void addFolderToZip(String path, String srcFolder, ZipOutputStream zip)
        throws Exception
    {
        File folder = new File(srcFolder);

        for (String fileName : folder.list()) {
            if (path.equals("")) {
                addFileToZip(folder.getName(), srcFolder + "/" + fileName, zip);
            }
            else {
                addFileToZip(path + "/" + folder.getName(), srcFolder + "/" + fileName, zip);
            }
        }
    }

}

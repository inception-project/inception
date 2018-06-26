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

import static de.tudarmstadt.ukp.clarin.webanno.api.ProjectService.DOCUMENT_FOLDER;
import static de.tudarmstadt.ukp.clarin.webanno.api.ProjectService.PROJECT_FOLDER;
import static de.tudarmstadt.ukp.clarin.webanno.api.ProjectService.SOURCE_FOLDER;
import static de.tudarmstadt.ukp.clarin.webanno.api.WebAnnoConst.INITIAL_CAS_PSEUDO_USER;
import static de.tudarmstadt.ukp.clarin.webanno.model.SourceDocumentStateTransition.NEW_TO_ANNOTATION_IN_PROGRESS;
import static java.util.Objects.isNull;
import static org.apache.commons.io.IOUtils.copyLarge;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.TreeMap;

import javax.persistence.EntityManager;
import javax.persistence.NoResultException;
import javax.persistence.PersistenceContext;

import org.apache.commons.io.FileUtils;
import org.apache.uima.UIMAException;
import org.apache.uima.cas.CAS;
import org.apache.uima.cas.CASException;
import org.apache.uima.jcas.JCas;
import org.apache.uima.resource.ResourceInitializationException;
import org.apache.uima.resource.metadata.TypeSystemDescription;
import org.apache.uima.util.CasCreationUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import de.tudarmstadt.ukp.clarin.webanno.api.CasStorageService;
import de.tudarmstadt.ukp.clarin.webanno.api.DocumentService;
import de.tudarmstadt.ukp.clarin.webanno.api.ImportExportService;
import de.tudarmstadt.ukp.clarin.webanno.api.ProjectService;
import de.tudarmstadt.ukp.clarin.webanno.api.WebAnnoConst;
import de.tudarmstadt.ukp.clarin.webanno.api.event.AfterAnnotationUpdateEvent;
import de.tudarmstadt.ukp.clarin.webanno.api.event.AfterDocumentCreatedEvent;
import de.tudarmstadt.ukp.clarin.webanno.api.event.AfterDocumentResetEvent;
import de.tudarmstadt.ukp.clarin.webanno.api.event.AnnotationStateChangeEvent;
import de.tudarmstadt.ukp.clarin.webanno.api.event.BeforeDocumentRemovedEvent;
import de.tudarmstadt.ukp.clarin.webanno.api.event.DocumentStateChangedEvent;
import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationDocument;
import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationDocumentState;
import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationDocumentStateTransition;
import de.tudarmstadt.ukp.clarin.webanno.model.PermissionLevel;
import de.tudarmstadt.ukp.clarin.webanno.model.Project;
import de.tudarmstadt.ukp.clarin.webanno.model.SourceDocument;
import de.tudarmstadt.ukp.clarin.webanno.model.SourceDocumentState;
import de.tudarmstadt.ukp.clarin.webanno.model.SourceDocumentStateTransition;
import de.tudarmstadt.ukp.clarin.webanno.security.UserDao;
import de.tudarmstadt.ukp.clarin.webanno.security.model.User;
import de.tudarmstadt.ukp.clarin.webanno.support.logging.Logging;

@Component(DocumentService.SERVICE_NAME)
public class DocumentServiceImpl
    implements DocumentService, InitializingBean
{
    private final Logger log = LoggerFactory.getLogger(getClass());

    @PersistenceContext
    private EntityManager entityManager;

    private @Autowired UserDao userRepository;
    private @Autowired CasStorageService casStorageService;
    private @Autowired ImportExportService importExportService;
    private @Autowired ProjectService projectService;
    private @Autowired ApplicationEventPublisher applicationEventPublisher;
    
    @Value(value = "${repository.path}")
    private File dir;

    @Override
    public void afterPropertiesSet()
    {
        log.info("Document repository path: " + dir);
    }
    
    @Override
    public File getDir()
    {
        return dir;
    }
    
    @Override
    public File getDocumentFolder(SourceDocument aDocument)
        throws IOException
    {
        File sourceDocFolder = new File(dir, "/" + PROJECT_FOLDER + "/" + aDocument.getProject().getId() + "/" + DOCUMENT_FOLDER + "/"
                + aDocument.getId() + "/" + SOURCE_FOLDER);
        FileUtils.forceMkdir(sourceDocFolder);
        return sourceDocFolder;
    }

    @Override
    @Transactional
    public void createSourceDocument(SourceDocument aDocument)
    {
        if (isNull(aDocument.getId())) {
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
    public void createAnnotationDocument(AnnotationDocument aAnnotationDocument)
    {
        if (isNull(aAnnotationDocument.getId())) {
            entityManager.persist(aAnnotationDocument);
            
            try (MDC.MDCCloseable closable = MDC.putCloseable(Logging.KEY_PROJECT_ID,
                    String.valueOf(aAnnotationDocument.getProject().getId()))) {
                log.info(
                        "Created annotation document [{}] for user [{}] for source document "
                        + "[{}]({}) in project [{}]({})",
                        aAnnotationDocument.getId(), aAnnotationDocument.getUser(), 
                        aAnnotationDocument.getDocument().getName(),
                        aAnnotationDocument.getDocument().getId(),
                        aAnnotationDocument.getProject().getName(),
                        aAnnotationDocument.getProject().getId());
            }
        }
        else {
            entityManager.merge(aAnnotationDocument);
        }
    }

    @Override
    @Transactional
    public boolean existsCas(SourceDocument aSourceDocument, String aUsername)
        throws IOException
    {
        return casStorageService.existsCas(aSourceDocument, aUsername);
    }

    @Override
    @Transactional
    public boolean existsAnnotationCas(AnnotationDocument aAnnotationDocument)
        throws IOException
    {
        return existsCas(aAnnotationDocument.getDocument(), aAnnotationDocument.getUser());
    }

    @Override
    @Transactional
    public boolean existsSourceDocument(Project aProject, String aFileName)
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
    public File getSourceDocumentFile(SourceDocument aDocument)
    {
        File documentUri = new File(
                dir.getAbsolutePath() + "/" + PROJECT_FOLDER + "/" + aDocument.getProject().getId()
                        + "/" + DOCUMENT_FOLDER + "/" + aDocument.getId() + "/" + SOURCE_FOLDER);
        return new File(documentUri, aDocument.getName());
    }

    @Override
    public File getCasFile(SourceDocument aDocument, String aUser) throws IOException
    {
        return new File(casStorageService.getAnnotationFolder(aDocument), aUser + ".ser");
    }
    
    @Override
    @Transactional(noRollbackFor = NoResultException.class)
    public AnnotationDocument createOrGetAnnotationDocument(SourceDocument aDocument, User aUser)
    {
        // Check if there is an annotation document entry in the database. If there is none,
        // create one.
        AnnotationDocument annotationDocument = null;
        if (!existsAnnotationDocument(aDocument, aUser)) {
            annotationDocument = new AnnotationDocument();
            annotationDocument.setDocument(aDocument);
            annotationDocument.setName(aDocument.getName());
            annotationDocument.setUser(aUser.getUsername());
            annotationDocument.setProject(aDocument.getProject());
            createAnnotationDocument(annotationDocument);
        }
        else {
            annotationDocument = getAnnotationDocument(aDocument, aUser);
        }

        return annotationDocument;
    }

    @Override
    @Transactional(noRollbackFor = NoResultException.class)
    public AnnotationDocument getAnnotationDocument(SourceDocument aDocument, User aUser)
    {
        return getAnnotationDocument(aDocument, aUser.getUsername());
    }

    @Override
    @Transactional(noRollbackFor = NoResultException.class)
    public AnnotationDocument getAnnotationDocument(SourceDocument aDocument, String aUser)
    {
        return entityManager
                .createQuery(
                        "FROM AnnotationDocument WHERE document = :document AND " + "user =:user"
                                + " AND project = :project", AnnotationDocument.class)
                .setParameter("document", aDocument).setParameter("user", aUser)
                .setParameter("project", aDocument.getProject()).getSingleResult();
    }
    
    @Override
    @Transactional(noRollbackFor = NoResultException.class)
    public SourceDocument getSourceDocument(Project aProject, String aDocumentName)
    {
        return entityManager
                .createQuery("FROM SourceDocument WHERE name = :name AND project =:project",
                        SourceDocument.class).setParameter("name", aDocumentName)
                .setParameter("project", aProject).getSingleResult();
    }
    
    @Override
    @Transactional(noRollbackFor = NoResultException.class)
    public SourceDocument getSourceDocument(long aProjectId, long aSourceDocId)
    {              
        return entityManager
                .createQuery("FROM SourceDocument WHERE id = :docid AND project.id =:pid",
                        SourceDocument.class)
                .setParameter("docid", aSourceDocId).setParameter("pid", aProjectId)
                .getSingleResult();
    }
    
    @Override
    @Transactional
    public SourceDocumentState setSourceDocumentState(SourceDocument aDocument,
            SourceDocumentState aState)
    {
        SourceDocumentState oldState = aDocument.getState();
        
        aDocument.setState(aState);
        
        createSourceDocument(aDocument);
        
        // Notify about change in document state
        if (!Objects.equals(oldState, aDocument.getState())) {
            applicationEventPublisher
                    .publishEvent(new DocumentStateChangedEvent(this, aDocument, oldState));
        }
        
        return oldState;
    }

    @Override
    @Transactional
    public SourceDocumentState transitionSourceDocumentState(SourceDocument aDocument,
            SourceDocumentStateTransition aTransition)
    {
        return setSourceDocumentState(aDocument,
                SourceDocumentStateTransition.transition(aTransition));
    }

    @Override
    @Transactional(noRollbackFor = NoResultException.class)
    public boolean existsFinishedAnnotation(SourceDocument aDocument)
    {
        String query = 
                "SELECT COUNT(*) " +
                "FROM AnnotationDocument " + 
                "WHERE document = :document AND state = :state";
        
        long count = entityManager.createQuery(query, Long.class)
            .setParameter("document", aDocument)
            .setParameter("state", AnnotationDocumentState.FINISHED)
            .getSingleResult();

        return count > 0;
    }

    @Override
    @Transactional(noRollbackFor = NoResultException.class)
    public boolean existsFinishedAnnotation(Project aProject)
    {
        String query = 
                "SELECT COUNT(*) " +
                "FROM AnnotationDocument " + 
                "WHERE document.project = :project AND state = :state";
        
        long count = entityManager.createQuery(query, Long.class)
            .setParameter("project", aProject)
            .setParameter("state", AnnotationDocumentState.FINISHED)
            .getSingleResult();

        return count > 0;
    }

    @Override
    public List<AnnotationDocument> listFinishedAnnotationDocuments(Project aProject)
    {
        // Get all annotators in the project
        List<String> users = getAllAnnotators(aProject);
        // Bail out already. HQL doesn't seem to like queries with an empty
        // parameter right of "in"
        if (users.isEmpty()) {
            return new ArrayList<>();
        }

        return entityManager
                .createQuery(
                        "FROM AnnotationDocument WHERE project = :project AND state = :state"
                                + " AND user in (:users)", AnnotationDocument.class)
                .setParameter("project", aProject).setParameter("users", users)
                .setParameter("state", AnnotationDocumentState.FINISHED).getResultList();
    }

    @Override
    @Transactional(noRollbackFor = NoResultException.class)
    public List<AnnotationDocument> listAllAnnotationDocuments(SourceDocument aSourceDocument)
    {
        return entityManager
                .createQuery(
                        "FROM AnnotationDocument WHERE project = :project AND document = :document",
                        AnnotationDocument.class)
                .setParameter("project", aSourceDocument.getProject())
                .setParameter("document", aSourceDocument).getResultList();
    }

    @Override
    @Transactional(noRollbackFor = NoResultException.class)
    public List<SourceDocument> listSourceDocuments(Project aProject)
    {
        List<SourceDocument> sourceDocuments = entityManager
                .createQuery("FROM SourceDocument where project =:project ORDER BY name ASC",
                        SourceDocument.class)
                .setParameter("project", aProject).getResultList();
        List<SourceDocument> tabSepDocuments = new ArrayList<>();
        for (SourceDocument sourceDocument : sourceDocuments) {
            if (sourceDocument.getFormat().equals(WebAnnoConst.TAB_SEP)) {
                tabSepDocuments.add(sourceDocument);
            }
        }
        sourceDocuments.removeAll(tabSepDocuments);
        return sourceDocuments;
    }

    @Override
    @Transactional
    public void removeSourceDocument(SourceDocument aDocument)
        throws IOException
    {
        // BeforeDocumentRemovedEvent is triggered first, since methods that rely 
        // on it might need to have access to the associated annotation documents 
        applicationEventPublisher.publishEvent(new BeforeDocumentRemovedEvent(this, aDocument));
        
        for (AnnotationDocument annotationDocument : listAllAnnotationDocuments(aDocument)) {
            removeAnnotationDocument(annotationDocument);
        }
        
        entityManager.remove(
                entityManager.contains(aDocument) ? aDocument : entityManager.merge(aDocument));

        String path = dir.getAbsolutePath() + "/" + PROJECT_FOLDER + "/" + aDocument.getProject().getId() + "/" + DOCUMENT_FOLDER + "/"
                + aDocument.getId();
        // remove from file both source and related annotation file
        if (new File(path).exists()) {
            FileUtils.forceDelete(new File(path));
        }

        try (MDC.MDCCloseable closable = MDC.putCloseable(Logging.KEY_PROJECT_ID,
                String.valueOf(aDocument.getProject().getId()))) {
            Project project = aDocument.getProject();
            log.info("Removed source document [{}]({}) from project [{}]({})", aDocument.getName(),
                    aDocument.getId(), project.getName(), project.getId());
        }
    }

    @Override
    @Transactional
    public void removeAnnotationDocument(AnnotationDocument aAnnotationDocument)
    {
        entityManager.remove(aAnnotationDocument);
    }

    @Override
    @Transactional
    public void uploadSourceDocument(File aFile, SourceDocument aDocument)
        throws IOException
    {
        try (InputStream is = new FileInputStream(aFile)) {
            uploadSourceDocument(is, aDocument);
        }
    }

    @Override
    @Transactional
    public void uploadSourceDocument(InputStream aIs, SourceDocument aDocument)
        throws IOException
    {
        // Create the metadata record - this also assigns the ID to the document
        createSourceDocument(aDocument);
        
        // Import the actual content
        File targetFile = getSourceDocumentFile(aDocument);
        JCas jcas;
        try {
            FileUtils.forceMkdir(targetFile.getParentFile());
            
            try (OutputStream os = new FileOutputStream(targetFile)) {
                copyLarge(aIs, os);
            }
            
            // Check if the file has a valid format / can be converted without error
            // This requires that the document ID has already been assigned
            jcas = createInitialCas(aDocument);
        }
        catch (IOException e) {
            FileUtils.forceDelete(targetFile);
            removeSourceDocument(aDocument);
            throw e;
        }
        catch (Exception e) {
            FileUtils.forceDelete(targetFile);
            removeSourceDocument(aDocument);
            throw new IOException(e.getMessage(), e);
        }

        applicationEventPublisher
                .publishEvent(new AfterDocumentCreatedEvent(this, aDocument, jcas));
        
        try (MDC.MDCCloseable closable = MDC.putCloseable(Logging.KEY_PROJECT_ID,
                String.valueOf(aDocument.getProject().getId()))) {
            Project project = aDocument.getProject();
            log.info("Imported source document [{}]({}) to project [{}]({})", 
                    aDocument.getName(), aDocument.getId(), project.getName(), project.getId());
        }
    }
    
    @Override
    public boolean existsInitialCas(SourceDocument aDocument)
        throws IOException
    {
        return existsCas(aDocument, INITIAL_CAS_PSEUDO_USER);
    }

    @Override
    public JCas createInitialCas(SourceDocument aDocument)
        throws UIMAException, IOException
    {
        return createInitialCas(aDocument, true);
    }

    @Override
    public JCas createInitialCas(SourceDocument aDocument, boolean aAnalyzeRepairAndSave)
        throws UIMAException, IOException
    {
        // Normally, the initial CAS should be created on document import, but after
        // adding this feature, the existing projects do not yet have initial CASes, so
        // we create them here lazily
        JCas jcas = importExportService.importCasFromFile(getSourceDocumentFile(aDocument),
                aDocument.getProject(), aDocument.getFormat());
        
        if (aAnalyzeRepairAndSave) {
            casStorageService.analyzeAndRepair(aDocument, INITIAL_CAS_PSEUDO_USER, jcas.getCas());
            
            CasPersistenceUtils.writeSerializedCas(jcas,
                    getCasFile(aDocument, INITIAL_CAS_PSEUDO_USER));
        }
        
        return jcas;
    }

    @Override
    public JCas readInitialCas(SourceDocument aDocument)
        throws CASException, ResourceInitializationException, IOException
    {
        return readInitialCas(aDocument, true);
    }

    @Override
    public JCas readInitialCas(SourceDocument aDocument, boolean aAnalyzeAndRepair)
        throws CASException, ResourceInitializationException, IOException
    {
        CAS cas = CasCreationUtils.createCas((TypeSystemDescription) null, null, null);
        
        CasPersistenceUtils.readSerializedCas(cas, getCasFile(aDocument, INITIAL_CAS_PSEUDO_USER));
        
        if (aAnalyzeAndRepair) {
            casStorageService.analyzeAndRepair(aDocument, INITIAL_CAS_PSEUDO_USER, cas);
        }
        
        return cas.getJCas();
    }

    @Override
    public JCas createOrReadInitialCas(SourceDocument aDocument)
        throws IOException, UIMAException
    {
        if (existsInitialCas(aDocument)) {
            return readInitialCas(aDocument);
        }
        else {
            return createInitialCas(aDocument);
        }
    }
        
    
    @Override
    @Transactional
    @Deprecated
    public JCas readAnnotationCas(SourceDocument aDocument, User aUser)
        throws IOException
    {
        // Check if there is an annotation document entry in the database. If there is none,
        // create one.
        AnnotationDocument annotationDocument = createOrGetAnnotationDocument(aDocument, aUser);

        // Change the state of the source document to in progress
        transitionSourceDocumentState(aDocument, NEW_TO_ANNOTATION_IN_PROGRESS);

        return readAnnotationCas(annotationDocument);
    }

    @Override
    @Transactional
    public JCas readAnnotationCas(AnnotationDocument aAnnotationDocument)
        throws IOException
    {
        return readAnnotationCas(aAnnotationDocument, true);
    }
    
    @Override
    @Transactional
    public JCas readAnnotationCas(AnnotationDocument aAnnotationDocument, boolean aAnalyzeAndRepair)
        throws IOException
    {
        // If there is no CAS yet for the annotation document, create one.
        JCas jcas = null;
        SourceDocument aDocument = aAnnotationDocument.getDocument();
        String user = aAnnotationDocument.getUser();
        if (!existsCas(aAnnotationDocument.getDocument(), user)) {
            // Convert the source file into an annotation CAS
            try {
                if (!existsInitialCas(aDocument)) {
                    jcas = createInitialCas(aDocument, aAnalyzeAndRepair);
                }

                // Ok, so at this point, we either have the lazily converted CAS already loaded
                // or we know that we can load the existing initial CAS.
                if (jcas == null) {
                    jcas = readInitialCas(aDocument, aAnalyzeAndRepair);
                }
            }
            catch (Exception e) {
                log.error("The reader for format [" + aDocument.getFormat()
                        + "] is unable to digest data", e);
                throw new IOException("The reader for format [" + aDocument.getFormat()
                        + "] is unable to digest data: " + e.getMessage());
            }
            casStorageService.writeCas(aDocument, jcas, user);
        }
        else {
            // Read existing CAS
            // We intentionally do not upgrade the CAS here because in general the IDs
            // must remain stable. If an upgrade is required the caller should do it
            jcas = casStorageService.readCas(aDocument, user, aAnalyzeAndRepair);
        }

        return jcas;
    }

    @Override
    @Transactional
    public void writeAnnotationCas(JCas aJCas, AnnotationDocument aAnnotationDocument,
            boolean aUpdateTimestamp)
        throws IOException
    {
        casStorageService.writeCas(aAnnotationDocument.getDocument(), aJCas,
                aAnnotationDocument.getUser());
        
        if (aUpdateTimestamp) {
            // FIXME REC Does it really make sense to set the accessed sentence from the source
            // document?!
            aAnnotationDocument.setSentenceAccessed(
                    aAnnotationDocument.getDocument().getSentenceAccessed());
            aAnnotationDocument.setTimestamp(new Timestamp(new Date().getTime()));
            setAnnotationDocumentState(aAnnotationDocument, AnnotationDocumentState.IN_PROGRESS);
        }
        
        applicationEventPublisher
                .publishEvent(new AfterAnnotationUpdateEvent(this, aAnnotationDocument, aJCas));
    }
    
    
    @Override
    public void deleteAnnotationCas(AnnotationDocument aAnnotationDocument) throws IOException
    {
        casStorageService.deleteCas(aAnnotationDocument.getDocument(),
                aAnnotationDocument.getUser());
    }
    
    @Override
    @Transactional
    public void writeAnnotationCas(JCas aJcas, SourceDocument aDocument, User aUser,
            boolean aUpdateTimestamp)
        throws IOException
    {
        AnnotationDocument annotationDocument = getAnnotationDocument(aDocument, aUser);
        writeAnnotationCas(aJcas, annotationDocument, aUpdateTimestamp);
    }

    @Override
    public void resetAnnotationCas(SourceDocument aDocument, User aUser)
        throws UIMAException, IOException
    {
        AnnotationDocument adoc = getAnnotationDocument(aDocument, aUser);
        JCas jcas = createOrReadInitialCas(aDocument);
        writeAnnotationCas(jcas, aDocument, aUser, false);
        applicationEventPublisher.publishEvent(new AfterDocumentResetEvent(this, adoc, jcas));
    }
    
    /**
     * Return true if there exist at least one annotation document FINISHED for annotation for this
     * {@link SourceDocument}
     *
     * @param aSourceDocument
     *            the source document.
     * @param aProject
     *            the project.
     * @return if a finished document exists.
     */
    @Override
    public boolean existFinishedDocument(SourceDocument aSourceDocument, Project aProject)
    {
        List<de.tudarmstadt.ukp.clarin.webanno.model.AnnotationDocument> annotationDocuments =
                listAnnotationDocuments(aSourceDocument);
        boolean finishedAnnotationDocumentExist = false;
        for (de.tudarmstadt.ukp.clarin.webanno.model.AnnotationDocument annotationDocument : 
            annotationDocuments) {
            if (annotationDocument.getState().equals(AnnotationDocumentState.FINISHED)) {
                finishedAnnotationDocumentExist = true;
                break;
            }
        }
        return finishedAnnotationDocumentExist;
    }
    
    @Override
    public Map<SourceDocument, AnnotationDocument> listAnnotatableDocuments(Project aProject,
            User aUser)
    {
        // First get the source documents
        List<SourceDocument> sourceDocuments = entityManager
                .createQuery(
                        "FROM SourceDocument " +
                        "WHERE project = (:project)",
                        SourceDocument.class)
                .setParameter("project", aProject)
                .getResultList();

        // Next we get all the annotation document records. We can use these to filter out
        // documents which are IGNOREed for given users.
        List<AnnotationDocument> annotationDocuments = entityManager
                .createQuery(
                        "FROM AnnotationDocument " +
                        "WHERE user = (:username) AND project = (:project)",
                        AnnotationDocument.class)
                .setParameter("username", aUser.getUsername())
                .setParameter("project", aProject)
                .getResultList();

        // First we add all the source documents
        Map<SourceDocument, AnnotationDocument> map = new TreeMap<>(SourceDocument.NAME_COMPARATOR);
        for (SourceDocument doc : sourceDocuments) {
            map.put(doc, null);
        }

        // Now we link the source documents to the annotation documents and remove IGNOREed
        // documents
        for (AnnotationDocument adoc : annotationDocuments) {
            switch (adoc.getState()) {
            case IGNORE:
                map.remove(adoc.getDocument());
                break;
            default:
                map.put(adoc.getDocument(), adoc);
                break;
            }
        }

        return map;
    }
    
    @Override
    @Transactional(noRollbackFor = NoResultException.class)
    public boolean isAnnotationFinished(SourceDocument aDocument, User aUser)
    {
        try {
            AnnotationDocument annotationDocument = entityManager
                    .createQuery(
                            "FROM AnnotationDocument WHERE document = :document AND "
                                    + "user =:user", AnnotationDocument.class)
                    .setParameter("document", aDocument).setParameter("user", aUser.getUsername())
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
    public List<AnnotationDocument> listAnnotationDocuments(SourceDocument aDocument)
    {
        // Get all annotators in the project
        List<String> users = getAllAnnotators(aDocument.getProject());
        // Bail out already. HQL doesn't seem to like queries with an empty
        // parameter right of "in"
        if (users.isEmpty()) {
            return new ArrayList<>();
        }

        return entityManager
                .createQuery(
                        "FROM AnnotationDocument WHERE project = :project AND document = :document "
                                + "AND user in (:users)", AnnotationDocument.class)
                .setParameter("project", aDocument.getProject()).setParameter("users", users)
                .setParameter("document", aDocument).getResultList();
    }
    
    @Override
    public List<AnnotationDocument> listAnnotationDocuments(Project aProject, User aUser)
    {
        return entityManager
                .createQuery("FROM AnnotationDocument WHERE project = :project AND user = :user",
                        AnnotationDocument.class)
                .setParameter("project", aProject).setParameter("user", aUser.getUsername())
                .getResultList();
    }

    @Override
    public int numberOfExpectedAnnotationDocuments(Project aProject)
    {

        // Get all annotators in the project
        List<String> users = getAllAnnotators(aProject);
        // Bail out already. HQL doesn't seem to like queries with an empty
        // parameter right of "in"
        if (users.isEmpty()) {
            return 0;
        }

        int ignored = 0;
        List<AnnotationDocument> annotationDocuments = entityManager
                .createQuery(
                        "FROM AnnotationDocument WHERE project = :project AND user in (:users)",
                        AnnotationDocument.class).setParameter("project", aProject)
                .setParameter("users", users).getResultList();
        for (AnnotationDocument annotationDocument : annotationDocuments) {
            if (annotationDocument.getState().equals(AnnotationDocumentState.IGNORE)) {
                ignored++;
            }
        }
        return listSourceDocuments(aProject).size() * users.size() - ignored;

    }
    
    private List<String> getAllAnnotators(Project aProject)
    {
        // Get all annotators in the project
        List<String> users = entityManager
                .createQuery(
                        "SELECT DISTINCT user FROM ProjectPermission WHERE project = :project "
                                + "AND level = :level", String.class)
                .setParameter("project", aProject).setParameter("level", PermissionLevel.USER)
                .getResultList();

        // check if the username is in the Users database (imported projects
        // might have username
        // in the ProjectPermission entry while it is not in the Users database
        List<String> notInUsers = new ArrayList<>();
        for (String user : users) {
            if (!userRepository.exists(user)) {
                notInUsers.add(user);
            }
        }
        users.removeAll(notInUsers);

        return users;
    }
    
    @Override
    @Transactional
    public AnnotationDocumentState setAnnotationDocumentState(AnnotationDocument aDocument,
            AnnotationDocumentState aState)
    {
        AnnotationDocumentState oldState = aDocument.getState();
        
        aDocument.setState(aState);

        createAnnotationDocument(aDocument);

        if (!Objects.equals(oldState, aDocument.getState())) {
            applicationEventPublisher
                    .publishEvent(new AnnotationStateChangeEvent(this, aDocument, oldState));
        }

        return oldState;
    }

    @Override
    @Transactional
    public AnnotationDocumentState transitionAnnotationDocumentState(AnnotationDocument aDocument,
            AnnotationDocumentStateTransition aTransition)
    {
        return setAnnotationDocumentState(aDocument,
                AnnotationDocumentStateTransition.transition(aTransition));
    }
    
    @TransactionalEventListener(phase = TransactionPhase.BEFORE_COMMIT)
    public void onDocumentStateChangeEvent(DocumentStateChangedEvent aEvent)
    {
        projectService.recalculateProjectState(aEvent.getDocument().getProject());
    }

    @TransactionalEventListener(phase = TransactionPhase.BEFORE_COMMIT)
    public void onAfterDocumentCreatedEvent(AfterDocumentCreatedEvent aEvent)
    {
        projectService.recalculateProjectState(aEvent.getDocument().getProject());
    }

    @TransactionalEventListener(phase = TransactionPhase.BEFORE_COMMIT)
    public void onBeforeDocumentRemovedEvent(BeforeDocumentRemovedEvent aEvent)
    {
        projectService.recalculateProjectState(aEvent.getDocument().getProject());
    }
}

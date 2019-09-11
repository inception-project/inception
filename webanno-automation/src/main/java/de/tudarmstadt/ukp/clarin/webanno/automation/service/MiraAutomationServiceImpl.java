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
package de.tudarmstadt.ukp.clarin.webanno.automation.service;

import static de.tudarmstadt.ukp.clarin.webanno.api.ProjectService.ANNOTATION_FOLDER;
import static de.tudarmstadt.ukp.clarin.webanno.api.ProjectService.PROJECT_FOLDER;
import static de.tudarmstadt.ukp.clarin.webanno.api.ProjectService.SOURCE_FOLDER;
import static de.tudarmstadt.ukp.clarin.webanno.api.WebAnnoConst.MIRA;
import static de.tudarmstadt.ukp.clarin.webanno.api.WebAnnoConst.MIRA_TEMPLATE;
import static de.tudarmstadt.ukp.clarin.webanno.api.WebAnnoConst.TRAIN;
import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;
import static org.apache.commons.io.IOUtils.copyLarge;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import javax.persistence.EntityManager;
import javax.persistence.NoResultException;
import javax.persistence.PersistenceContext;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.io.LineIterator;
import org.apache.uima.UIMAException;
import org.apache.uima.cas.CAS;
import org.apache.uima.cas.CASException;
import org.apache.uima.resource.ResourceInitializationException;
import org.apache.uima.resource.metadata.TypeSystemDescription;
import org.apache.uima.util.CasCreationUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import de.tudarmstadt.ukp.clarin.webanno.api.ImportExportService;
import de.tudarmstadt.ukp.clarin.webanno.api.RepositoryProperties;
import de.tudarmstadt.ukp.clarin.webanno.api.WebAnnoConst;
import de.tudarmstadt.ukp.clarin.webanno.automation.model.AutomationStatus;
import de.tudarmstadt.ukp.clarin.webanno.automation.model.MiraTemplate;
import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationFeature;
import de.tudarmstadt.ukp.clarin.webanno.model.Project;
import de.tudarmstadt.ukp.clarin.webanno.model.TrainingDocument;
import de.tudarmstadt.ukp.clarin.webanno.support.logging.Logging;

@Component(AutomationService.SERVICE_NAME)
public class MiraAutomationServiceImpl
    implements AutomationService
{
    private final Logger log = LoggerFactory.getLogger(getClass());
    
    private final File dir;
    private final AutomationCasStorageService automationCasStorageService;
    private final ImportExportService importExportService;
    
    @PersistenceContext
    private EntityManager entityManager;
    
    @Autowired
    public MiraAutomationServiceImpl(RepositoryProperties aRepositoryProperties,
            AutomationCasStorageService aAutomationCasStorageService,
            ImportExportService aImportExportService)
    {
        dir = aRepositoryProperties.getPath();
        automationCasStorageService = aAutomationCasStorageService;
        importExportService = aImportExportService;
    }
    
    @Override
    public List<String> listTemplates(Project aProject)
    {
        // list all MIRA template files
        File[] files = new File(dir.getAbsolutePath() + "/" + PROJECT_FOLDER + "/"
                + aProject.getId() + MIRA + MIRA_TEMPLATE).listFiles();

        // Name of the MIRA template files
        List<String> templateFiles = new ArrayList<>();
        if (files != null) {
            for (File file : files) {
                templateFiles.add(file.getName());
            }
        }

        return templateFiles;
    }

    @Override
    @Transactional
    public List<MiraTemplate> listMiraTemplates(Project aProject)
    {
        List<MiraTemplate> allTenplates = entityManager.createQuery(
                "FROM MiraTemplate ORDER BY trainFeature ASC ", MiraTemplate.class).getResultList();
        List<MiraTemplate> templatesInThisProject = new ArrayList<>();
        for (MiraTemplate miraTemplate : allTenplates) {
            if (nonNull(miraTemplate.getTrainFeature()) && Objects.equals(
                    miraTemplate.getTrainFeature().getProject().getId(), aProject.getId())) {
                templatesInThisProject.add(miraTemplate);
            }
        }
        return templatesInThisProject;
    }

    @Override
    public void removeTemplate(Project aProject, String aFileName, String aUsername)
        throws IOException
    {
        FileUtils.forceDelete(new File(dir.getAbsolutePath() + "/" + PROJECT_FOLDER + "/"
                + aProject.getId() + MIRA + MIRA_TEMPLATE + aFileName));
        
        Logging.setMDC(aProject.getId(), aUsername);
        log.info("Removed template file [{}] from project [{}] ({})", aFileName, aProject.getName(),
                aProject.getId());
        Logging.clearMDC();
    }

    @Override
    public void createTemplate(Project aProject, File aContent, String aFileName, String aUsername)
        throws IOException
    {
        String templatePath = dir.getAbsolutePath() + "/" + PROJECT_FOLDER + "/" + aProject.getId()
                + MIRA + MIRA_TEMPLATE;
        FileUtils.forceMkdir(new File(templatePath));
        copyLarge(new FileInputStream(aContent), new FileOutputStream(new File(templatePath
                + aFileName)));

        Logging.setMDC(aProject.getId(), aUsername);
        log.info("Removed template file [{}] from project [{}] ({})", aFileName, aProject.getName(),
                aProject.getId());
        Logging.clearMDC();
    }

    @Override
    @Transactional
    public void removeMiraTemplate(MiraTemplate aTemplate)
    {
        try {
            removeAutomationStatus(getAutomationStatus(aTemplate));
        }
        catch (NoResultException e) {
            // do nothing - automation was not started and no status created for this template
        }
        entityManager.remove(aTemplate);
    }
    
    @Override
    @Transactional
    public void createAutomationStatus(AutomationStatus aStatus)
    {
        entityManager.persist(aStatus);
    }

    @Override
    public boolean existsAutomationStatus(MiraTemplate aTemplate)
    {
        try {
            entityManager
                    .createQuery("FROM AutomationStatus WHERE template =:template",
                            AutomationStatus.class).setParameter("template", aTemplate)
                    .getSingleResult();
            return true;
        }
        catch (NoResultException ex) {
            return false;
        }
    }

    @Override
    @Transactional
    public AutomationStatus getAutomationStatus(MiraTemplate aTemplate)
    {
        return entityManager
                .createQuery("FROM AutomationStatus WHERE template =:template",
                        AutomationStatus.class).setParameter("template", aTemplate)
                .getSingleResult();
    }

    @Override
    @Transactional
    public void removeAutomationStatus(AutomationStatus aStstus)
    {

        entityManager.remove(aStstus);
    }

    @Override
    public File getMiraModel(AnnotationFeature aFeature, boolean aOtherLayer,
            TrainingDocument aDocument)
    {
        if (aDocument != null) {
            return new File(getMiraDir(aFeature), aDocument.getId() + "- "
                    + aDocument.getProject().getId() + "-model");
        }
        else if (aOtherLayer) {
            return new File(getMiraDir(aFeature), aFeature.getId() + "-model");
        }
        else {
            return new File(getMiraDir(aFeature), aFeature.getLayer().getId() + "-"
                    + aFeature.getId() + "-model");
        }
    }

    @Override
    public File getMiraDir(AnnotationFeature aFeature)
    {
        return new File(dir, "/" + PROJECT_FOLDER + "/" + aFeature.getProject().getId() + MIRA);
    }

    @Override
    @Transactional
    public void createTemplate(MiraTemplate aTemplate)
    {
        if (isNull(aTemplate.getId())) {
            entityManager.persist(aTemplate);
        }
        else {
            entityManager.merge(aTemplate);
        }
    }

    @Override
    @Transactional(noRollbackFor = NoResultException.class)
    public MiraTemplate getMiraTemplate(AnnotationFeature aFeature)
    {
        return entityManager
                .createQuery("FROM MiraTemplate WHERE trainFeature =:trainFeature",
                        MiraTemplate.class).setParameter("trainFeature", aFeature)
                .getSingleResult();
    }

    @Override
    public boolean existsMiraTemplate(AnnotationFeature aFeature)
    {
        try {
            entityManager
                    .createQuery("FROM MiraTemplate WHERE trainFeature =:trainFeature",
                            MiraTemplate.class).setParameter("trainFeature", aFeature)
                    .getSingleResult();
            return true;
        }
        catch (NoResultException ex) {
            return false;
        }
    }

    @Override
    @Transactional(noRollbackFor = NoResultException.class)
    public List<TrainingDocument> listTabSepDocuments(Project aProject)
    {
        List<TrainingDocument> trainingDocuments = entityManager
                .createQuery("FROM TrainingDocument where project =:project",
                        TrainingDocument.class)
                .setParameter("project", aProject).getResultList();
        List<TrainingDocument> tabSepDocuments = new ArrayList<>();
        for (TrainingDocument trainingDocument : trainingDocuments) {
            if (trainingDocument.getFormat().equals(WebAnnoConst.TAB_SEP)) {
                tabSepDocuments.add(trainingDocument);
            }
        }
        return tabSepDocuments;
    }
    
    @Override
    @Transactional
    public boolean existsTrainingDocument(Project aProject, String aFileName)
    {
        try {
            entityManager
                    .createQuery(
                            "FROM TrainingDocument WHERE project = :project AND " + "name =:name ",
                            TrainingDocument.class).setParameter("project", aProject)
                    .setParameter("name", aFileName).getSingleResult();
            return true;
        }
        catch (NoResultException ex) {
            return false;
        }
    }
    
    @Override
    @Transactional(noRollbackFor = NoResultException.class)
    public File getDocumentFolder(TrainingDocument trainingDocument)
        throws IOException
    {
        File trainingDocFolder = new File(dir,
                "/" + PROJECT_FOLDER + "/" + trainingDocument.getProject().getId() + TRAIN
                        + trainingDocument.getId() + "/" + SOURCE_FOLDER);
        FileUtils.forceMkdir(trainingDocFolder);
        return trainingDocFolder;
    }

    @Override
    @Transactional(noRollbackFor = NoResultException.class)
    public List<TrainingDocument> listTrainingDocuments(Project aProject)
    {
        // both TAB_SEP and WebAnno training documents
        List<TrainingDocument> trainingDocuments = entityManager
                .createQuery("FROM TrainingDocument where project =:project",
                        TrainingDocument.class)
                .setParameter("project", aProject).getResultList();
       /*
        * List<TrainingDocument> webAnnoTraiingDocuments = new ArrayList<TrainingDocument>(); for
        * (TrainingDocument trainingDocument : trainingDocuments) { if
        * (trainingDocument.getFormat().equals(WebAnnoConst.TAB_SEP)) {
        * webAnnoTraiingDocuments.add(trainingDocument); } }
        */
        return trainingDocuments;
    }
    
    @Override
    @Transactional(noRollbackFor = NoResultException.class)
    public TrainingDocument getTrainingDocument(Project aProject, String aDocumentName)
    {
        return entityManager
                .createQuery("FROM TrainingDocument WHERE name = :name AND project =:project",
                        TrainingDocument.class)
                .setParameter("name", aDocumentName).setParameter("project", aProject)
                .getSingleResult();
    }
    
    @Override
    @Transactional
    public void removeTrainingDocument(TrainingDocument aDocument)
        throws IOException
    {       
        entityManager.remove(aDocument);

        String path = dir.getAbsolutePath() + "/" + PROJECT_FOLDER + "/"
                + aDocument.getProject().getId() + TRAIN + aDocument.getId();
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
    public CAS readTrainingAnnotationCas(TrainingDocument aTrainingAnnotationDocument)
        throws IOException
    {
        // If there is no CAS yet for the annotation document, create one.
        CAS cas = null;
        if (!existsCas(aTrainingAnnotationDocument)) {
            // Convert the source file into an annotation CAS
            try {
                if (!existsInitialCas(aTrainingAnnotationDocument)) {
                    cas = createInitialCas(aTrainingAnnotationDocument);
                }

                // Ok, so at this point, we either have the lazily converted CAS already loaded
                // or we know that we can load the existing initial CAS.
                if (cas == null) {
                    cas = readInitialCas(aTrainingAnnotationDocument);
                }
            }
            catch (Exception e) {
                log.error("The reader for format [" + aTrainingAnnotationDocument.getFormat()
                        + "] is unable to digest data", e);
                throw new IOException(
                        "The reader for format [" + aTrainingAnnotationDocument.getFormat()
                                + "] is unable to digest data: " + e.getMessage());
            }
            automationCasStorageService.writeCas(aTrainingAnnotationDocument, cas);
        }
        else {
            // Read existing CAS
            // We intentionally do not upgrade the CAS here because in general the IDs
            // must remain stable. If an upgrade is required the caller should do it
            cas = automationCasStorageService.readCas(aTrainingAnnotationDocument);
        }

        return cas;
    }
    
    
    @Override
    @Transactional
    public void createTrainingDocument(TrainingDocument aDocument)
        throws IOException
    {
        if (isNull(aDocument.getId())) {
            entityManager.persist(aDocument);
        }
        else {
            entityManager.merge(aDocument);
        }
    }
    
    @Override
    public boolean existsInitialCas(TrainingDocument aDocument)
        throws IOException
    {
        return existsCas(aDocument);
    }
    
    @Override
    @Transactional
    public boolean existsCas(TrainingDocument aTrainingDocument)
        throws IOException
    {
        return new File(automationCasStorageService.getAutomationFolder(aTrainingDocument), aTrainingDocument.getName() + ".ser")
                .exists();
    }
    

    @Override
    public CAS createInitialCas(TrainingDocument aDocument)
        throws UIMAException, IOException, ClassNotFoundException
    {
        CAS cas = importExportService.importCasFromFile(getTrainingDocumentFile(aDocument),
                aDocument.getProject(), aDocument.getFormat());
        automationCasStorageService.analyzeAndRepair(aDocument, cas);
        CasPersistenceUtils.writeSerializedCas(cas, getCasFile(aDocument));

        return cas;
    }

    @Override
    public File getTrainingDocumentFile(TrainingDocument aDocument)
    {
        File documentUri = new File(dir.getAbsolutePath() + "/" + PROJECT_FOLDER + "/"
                + aDocument.getProject().getId() + TRAIN + aDocument.getId() + "/" + SOURCE_FOLDER);
        return new File(documentUri, aDocument.getName());
    }

    @Override
    public CAS readInitialCas(TrainingDocument aDocument)
        throws CASException, ResourceInitializationException, IOException
    {
        CAS cas = CasCreationUtils.createCas((TypeSystemDescription) null, null, null);

        CasPersistenceUtils.readSerializedCas(cas, getCasFile(aDocument));

        automationCasStorageService.analyzeAndRepair(aDocument, cas);

        return cas;
    }

    @Override
    public CAS createOrReadInitialCas(TrainingDocument aDocument)
        throws IOException, UIMAException, ClassNotFoundException
    {
        if (existsInitialCas(aDocument)) {
            return readInitialCas(aDocument);
        }
        else {
            return createInitialCas(aDocument);
        }
    }

    @Override
    public File getCasFile(TrainingDocument aDocument)
    {
        File documentUri = new File(
                dir.getAbsolutePath() + "/" + PROJECT_FOLDER + "/" + aDocument.getProject().getId()
                        + TRAIN + aDocument.getId() + "/" + ANNOTATION_FOLDER);
        return new File(documentUri, FilenameUtils.removeExtension(aDocument.getName()) + ".ser");
    }
    
    @Override
    @Transactional
    public void uploadTrainingDocument(File aFile, TrainingDocument aDocument)
        throws IOException
    {
        // Check if the file has a valid format / can be converted without error
        CAS cas = null;
        try {
            if (aDocument.getFormat().equals(WebAnnoConst.TAB_SEP)) {
                if (!isTabSepFileFormatCorrect(aFile)) {
                    throw new IOException(
                            "This TAB-SEP file is not in correct format. It should have two columns separated by TAB!");
                }
            }
            else {
                cas = importExportService.importCasFromFile(aFile, aDocument.getProject(),
                        aDocument.getFormat());
                automationCasStorageService.analyzeAndRepair(aDocument, cas);
            }
        }
        catch (IOException e) {
            removeTrainingDocument(aDocument);
            throw e;
        }
        catch (Exception e) {
            removeTrainingDocument(aDocument);
            throw new IOException(e.getMessage(), e);
        }

        // Copy the original file into the repository
        File targetFile = getTrainingDocumentFile(aDocument);
        FileUtils.forceMkdir(targetFile.getParentFile());
        FileUtils.copyFile(aFile, targetFile);

        // Copy the initial conversion of the file into the repository
        if (cas != null) {
            CasPersistenceUtils.writeSerializedCas(cas, getCasFile(aDocument));
        }

        try (MDC.MDCCloseable closable = MDC.putCloseable(Logging.KEY_PROJECT_ID,
                String.valueOf(aDocument.getProject().getId()))) {
            Project project = aDocument.getProject();
            log.info("Imported training document [{}]({}) to project [{}]({})", 
                    aDocument.getName(), aDocument.getId(), project.getName(), project.getId());
        }
    }

    /**
     * Check if a TAB-Sep training file is in correct format before importing
     */
    private boolean isTabSepFileFormatCorrect(File aFile)
    {
        try {
            LineIterator it = new LineIterator(new FileReader(aFile));
            while (it.hasNext()) {
                String line = it.next();
                if (line.trim().length() == 0) {
                    continue;
                }
                if (line.split("\t").length != 2) {
                    return false;
                }
            }
        }
        catch (Exception e) {
            return false;
        }
        return true;
    }
}

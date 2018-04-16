/*
 * Copyright 2017
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
package de.tudarmstadt.ukp.clarin.webanno.export;

import static de.tudarmstadt.ukp.clarin.webanno.api.WebAnnoConst.INITIAL_CAS_PSEUDO_USER;
import static de.tudarmstadt.ukp.clarin.webanno.export.ProjectExportRequest.FORMAT_AUTO;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.apache.uima.UIMAException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.tudarmstadt.ukp.clarin.webanno.api.AnnotationSchemaService;
import de.tudarmstadt.ukp.clarin.webanno.api.DocumentService;
import de.tudarmstadt.ukp.clarin.webanno.api.ImportExportService;
import de.tudarmstadt.ukp.clarin.webanno.api.ProjectService;
import de.tudarmstadt.ukp.clarin.webanno.api.WebAnnoConst;
import de.tudarmstadt.ukp.clarin.webanno.automation.model.MiraTemplate;
import de.tudarmstadt.ukp.clarin.webanno.automation.service.AutomationService;
import de.tudarmstadt.ukp.clarin.webanno.constraints.ConstraintsService;
import de.tudarmstadt.ukp.clarin.webanno.export.model.ExportedAnnotationDocument;
import de.tudarmstadt.ukp.clarin.webanno.export.model.ExportedAnnotationFeature;
import de.tudarmstadt.ukp.clarin.webanno.export.model.ExportedAnnotationFeatureReference;
import de.tudarmstadt.ukp.clarin.webanno.export.model.ExportedAnnotationLayer;
import de.tudarmstadt.ukp.clarin.webanno.export.model.ExportedAnnotationLayerReference;
import de.tudarmstadt.ukp.clarin.webanno.export.model.ExportedMiraTemplate;
import de.tudarmstadt.ukp.clarin.webanno.export.model.ExportedProject;
import de.tudarmstadt.ukp.clarin.webanno.export.model.ExportedProjectPermission;
import de.tudarmstadt.ukp.clarin.webanno.export.model.ExportedSourceDocument;
import de.tudarmstadt.ukp.clarin.webanno.export.model.ExportedTag;
import de.tudarmstadt.ukp.clarin.webanno.export.model.ExportedTagSet;
import de.tudarmstadt.ukp.clarin.webanno.export.model.ExportedTrainingDocument;
import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationDocumentState;
import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationFeature;
import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationLayer;
import de.tudarmstadt.ukp.clarin.webanno.model.ConstraintSet;
import de.tudarmstadt.ukp.clarin.webanno.model.Mode;
import de.tudarmstadt.ukp.clarin.webanno.model.Project;
import de.tudarmstadt.ukp.clarin.webanno.model.SourceDocumentState;
import de.tudarmstadt.ukp.clarin.webanno.model.Tag;
import de.tudarmstadt.ukp.clarin.webanno.model.TagSet;
import de.tudarmstadt.ukp.clarin.webanno.model.TrainingDocument;
import de.tudarmstadt.ukp.clarin.webanno.security.UserDao;
import de.tudarmstadt.ukp.clarin.webanno.security.model.User;
import de.tudarmstadt.ukp.clarin.webanno.tsv.WebannoTsv3XWriter;

public class ExportUtil
{
    private static final Logger LOG = LoggerFactory.getLogger(ExportUtil.class);
    
    private static final String ANNOTATION_ORIGINAL_FOLDER = "/annotation/";
    private static final String CONSTRAINTS = "/constraints/";
    private static final String LOG_FOLDER = "/" + ProjectService.LOG_FOLDER;
    private static final String GUIDELINES_FOLDER = "/" + ImportUtil.GUIDELINE;
    private static final String ANNOTATION_CAS_FOLDER = "/"
            + ImportUtil.ANNOTATION_AS_SERIALISED_CAS + "/";
    private static final String META_INF = "/" + ImportUtil.META_INF;
    private static final String SOURCE_FOLDER = "/" + ImportUtil.SOURCE;
    private static final String TRAIN_FOLDER = "/" + ImportUtil.TRAIN;
    private static final String CORRECTION_USER = "CORRECTION_USER";
    private static final String CURATION_AS_SERIALISED_CAS = "/"
            + ImportUtil.CURATION_AS_SERIALISED_CAS + "/";
    private static final String CURATION_FOLDER = "/curation/";

    @Deprecated
    public static ExportedProject exportProjectSettings(
            AnnotationSchemaService annotationService,
            Optional<AutomationService> automationService, DocumentService documentService,
            ProjectService projectService, Project aProject, File aProjectSettings,
            File aExportTempDir)
    {
        ExportedProject exProjekt = new ExportedProject();
        exProjekt.setDescription(aProject.getDescription());
        exProjekt.setName(aProject.getName());
        // In older versions of WebAnno, the mode was an enum which was serialized as upper-case
        // during export but as lower-case in the database. This is compensating for this case.
        exProjekt.setMode(StringUtils.upperCase(aProject.getMode(), Locale.US));
        exProjekt.setScriptDirection(aProject.getScriptDirection());
        exProjekt.setVersion(aProject.getVersion());
        exProjekt.setDisableExport(aProject.isDisableExport());
        exProjekt.setCreated(aProject.getCreated());
        exProjekt.setUpdated(aProject.getUpdated());

        List<ExportedAnnotationLayer> exLayers = new ArrayList<>();
        // Store map of layer and its equivalent exLayer so that the attach type is attached later
        Map<AnnotationLayer, ExportedAnnotationLayer> layerToExLayers = new HashMap<>();
        // Store map of feature and its equivalent exFeature so that the attach feature is attached
        // later
        Map<AnnotationFeature, ExportedAnnotationFeature> featureToExFeatures = new HashMap<>();
        for (AnnotationLayer layer : annotationService.listAnnotationLayer(aProject)) {
            exLayers.add(ImportUtil.exportLayerDetails(layerToExLayers, featureToExFeatures,
                    layer, annotationService));
        }

        // add the attach type and attache feature to the exported layer and
        // exported feature
        for (AnnotationLayer layer : layerToExLayers.keySet()) {
            if (layer.getAttachType() != null) {
                layerToExLayers.get(layer).setAttachType(
                        new ExportedAnnotationLayerReference(layer.getAttachType().getName()));
            }
            if (layer.getAttachFeature() != null) {
                layerToExLayers.get(layer).setAttachFeature(
                        new ExportedAnnotationFeatureReference(layer.getAttachFeature()));
            }
        }
        exProjekt.setLayers(exLayers);

        List<ExportedTagSet> extTagSets = new ArrayList<>();
        for (TagSet tagSet : annotationService.listTagSets(aProject)) {
            ExportedTagSet exTagSet = new ExportedTagSet();
            exTagSet.setCreateTag(tagSet.isCreateTag());
            exTagSet.setDescription(tagSet.getDescription());
            exTagSet.setLanguage(tagSet.getLanguage());
            exTagSet.setName(tagSet.getName());
            List<ExportedTag> exTags = new ArrayList<>();
            for (Tag tag : annotationService.listTags(tagSet)) {
                ExportedTag exTag = new ExportedTag();
                exTag.setDescription(tag.getDescription());
                exTag.setName(tag.getName());
                exTags.add(exTag);
            }
            exTagSet.setTags(exTags);
            extTagSets.add(exTagSet);
        }

        exProjekt.setTagSets(extTagSets);
      
        List<ExportedSourceDocument> sourceDocuments = new ArrayList<>();
        List<ExportedAnnotationDocument> annotationDocuments = new ArrayList<>();

  
        // add source documents to a project
        List<de.tudarmstadt.ukp.clarin.webanno.model.SourceDocument> documents = documentService
                .listSourceDocuments(aProject);
        for (de.tudarmstadt.ukp.clarin.webanno.model.SourceDocument sourceDocument : documents) {

            ExportedSourceDocument exDocument = new ExportedSourceDocument();
            exDocument.setFormat(sourceDocument.getFormat());
            exDocument.setName(sourceDocument.getName());
            exDocument.setState(sourceDocument.getState());
            exDocument.setTimestamp(sourceDocument.getTimestamp());
            exDocument.setSentenceAccessed(sourceDocument.getSentenceAccessed());
            exDocument.setCreated(sourceDocument.getCreated());
            exDocument.setUpdated(sourceDocument.getUpdated());

            // add annotation document to Project
            for (de.tudarmstadt.ukp.clarin.webanno.model.AnnotationDocument annotationDocument : 
                    documentService.listAnnotationDocuments(sourceDocument)) {
                ExportedAnnotationDocument annotationDocumentToExport = 
                        new ExportedAnnotationDocument();
                annotationDocumentToExport.setName(annotationDocument.getName());
                annotationDocumentToExport.setState(annotationDocument.getState());
                annotationDocumentToExport.setUser(annotationDocument.getUser());
                annotationDocumentToExport.setTimestamp(annotationDocument.getTimestamp());
                annotationDocumentToExport
                        .setSentenceAccessed(annotationDocument.getSentenceAccessed());
                annotationDocumentToExport.setCreated(annotationDocument.getCreated());
                annotationDocumentToExport.setUpdated(annotationDocument.getUpdated());
                annotationDocuments.add(annotationDocumentToExport);
            }
            sourceDocuments.add(exDocument);

        }

        exProjekt.setSourceDocuments(sourceDocuments);
        exProjekt.setAnnotationDocuments(annotationDocuments);
        
        if (automationService.isPresent()) {
            List<ExportedTrainingDocument> trainDocuments = new ArrayList<>();
            List<TrainingDocument> trainingDocuments = automationService.get()
                    .listTrainingDocuments(aProject);
            
            Map<String, ExportedAnnotationFeature> fm = new HashMap<>();
            for (ExportedAnnotationFeature f : featureToExFeatures.values()) {
                fm.put(f.getName(), f);
            }
            for (TrainingDocument trainingDocument : trainingDocuments) {
                ExportedTrainingDocument exDocument = new ExportedTrainingDocument();
                exDocument.setFormat(trainingDocument.getFormat());
                exDocument.setName(trainingDocument.getName());
                exDocument.setState(trainingDocument.getState());
                exDocument.setTimestamp(trainingDocument.getTimestamp());
                exDocument.setSentenceAccessed(trainingDocument.getSentenceAccessed());
                if (trainingDocument.getFeature() != null) {
                    exDocument.setFeature(new ExportedAnnotationFeatureReference(
                            trainingDocument.getFeature()));
                }
                trainDocuments.add(exDocument);
            }
            
            exProjekt.setProperty("training_documents", trainDocuments);
        }
        else {
            exProjekt.setProperty("training_documents", new ArrayList<>());
        }

        List<ExportedProjectPermission> projectPermissions = new ArrayList<>();

        // add project permissions to the project
        for (User user : projectService.listProjectUsersWithPermissions(aProject)) {
            for (de.tudarmstadt.ukp.clarin.webanno.model.ProjectPermission permission : 
                    projectService.listProjectPermissionLevel(user, aProject)) {
                ExportedProjectPermission permissionToExport = new ExportedProjectPermission();
                permissionToExport.setLevel(permission.getLevel());
                permissionToExport.setUser(user.getUsername());
                projectPermissions.add(permissionToExport);
            }
        }

        exProjekt.setProjectPermissions(projectPermissions);

        // export automation Mira template
        if (automationService.isPresent()) {
            List<ExportedMiraTemplate> exTemplates =
                    new ArrayList<>();
            for (MiraTemplate template : automationService.get().listMiraTemplates(aProject)) {
                ExportedMiraTemplate exTemplate =
                        new ExportedMiraTemplate();
                exTemplate.setAnnotateAndPredict(template.isAnnotateAndRepeat());
                exTemplate.setAutomationStarted(template.isAutomationStarted());
                exTemplate.setCurrentLayer(template.isCurrentLayer());
                exTemplate.setResult(template.getResult());
                exTemplate.setTrainFeature(new ExportedAnnotationFeatureReference(
                        template.getTrainFeature()));
    
                if (template.getOtherFeatures().size() > 0) {
                    Set<ExportedAnnotationFeatureReference> exOtherFeatures = new HashSet<>();
                    for (AnnotationFeature feature : template.getOtherFeatures()) {
                        exOtherFeatures.add(new ExportedAnnotationFeatureReference(feature));
                    }
                    exTemplate.setOtherFeatures(exOtherFeatures);
                }
                exTemplates.add(exTemplate);
            }
    
            exProjekt.setProperty("mira_templates", exTemplates);
        }
        else {
            exProjekt.setProperty("mira_templates", new ArrayList<>());
        }
        
        return exProjekt;
    }

    /**
     * Copy source documents from the file system of this project to the export folder
     */
    @Deprecated
    public static void exportSourceDocuments(DocumentService documentService,
            ProjectExportRequest model, Project aProject, File aCopyDir)
        throws IOException, ProjectExportException
    {
        File sourceDocumentDir = new File(aCopyDir + SOURCE_FOLDER);
        FileUtils.forceMkdir(sourceDocumentDir);
        // Get all the source documents from the project
        List<de.tudarmstadt.ukp.clarin.webanno.model.SourceDocument> documents = documentService
                .listSourceDocuments(aProject);
        int i = 1;
        for (de.tudarmstadt.ukp.clarin.webanno.model.SourceDocument sourceDocument : documents) {
            try {
                FileUtils.copyFileToDirectory(documentService.getSourceDocumentFile(sourceDocument),
                        sourceDocumentDir);
                model.progress = (int) Math.ceil(((double) i) / documents.size() * 10.0);
                i++;
                LOG.info("Exported content for source document ["
                        + sourceDocument.getId() + "] in project [" + aProject.getName()
                        + "] with id [" + aProject.getId() + "]");
            }
            catch (FileNotFoundException e) {
                // error(e.getMessage());
                StringBuilder errorMessage = new StringBuilder();
                errorMessage.append("Source file '");
                errorMessage.append(sourceDocument.getName());
                errorMessage.append("' related to project couldn't be located in repository");
                LOG.error(errorMessage.toString(), ExceptionUtils.getRootCause(e));
                model.addMessage(errorMessage.toString());
                throw new ProjectExportException(
                        "Couldn't find some source file(s) related to project");
                // continue;

            }
        }
    }
    
    /**
     * Export {@link TrainingDocument}
     */
    @Deprecated
    public static void exportTrainingDocuments(AutomationService automationService,
            ProjectExportRequest model, Project aProject, File aCopyDir)
        throws IOException, ProjectExportException
    {
        File trainDocumentDir = new File(aCopyDir + TRAIN_FOLDER);
        FileUtils.forceMkdir(trainDocumentDir);
        // Get all the training documents from the project
        List<TrainingDocument> documents = automationService
                .listTrainingDocuments(aProject);
        int i = 1;
        for (TrainingDocument trainingDocument : documents) {
            try {
                FileUtils.copyFileToDirectory(
                        automationService.getTrainingDocumentFile(trainingDocument),
                        trainDocumentDir);
                model.progress = (int) Math.ceil(((double) i) / documents.size() * 10.0);
                i++;
                LOG.info("Imported content for training document ["
                        + trainingDocument.getId() + "] in project [" + aProject.getName()
                        + "] with id [" + aProject.getId() + "]");
            } catch (FileNotFoundException e) {
//              error(e.getMessage());
                StringBuilder errorMessage = new StringBuilder();
                errorMessage.append("Source file '");
                errorMessage.append(trainingDocument.getName());
                errorMessage.append("' related to project couldn't be located in repository");
                LOG.error(errorMessage.toString(), ExceptionUtils.getRootCause(e));
                model.addMessage(errorMessage.toString());
                throw new ProjectExportException("Couldn't find some source file(s) related to project");
//              continue;
            }
        }
    }

    /**
     * Copy annotation document as Serialized CAS from the file system of this project to the
     * export folder.
     */
    @Deprecated
    public static void exportAnnotationDocuments(DocumentService documentService,
            ImportExportService importExportService, UserDao userRepository,
            ProjectExportRequest aModel, File aCopyDir)
        throws IOException, UIMAException, ClassNotFoundException
    {
        Project project = aModel.getProject();
        
        List<de.tudarmstadt.ukp.clarin.webanno.model.SourceDocument> documents = documentService
                .listSourceDocuments(project);
        int i = 1;
        int initProgress = aModel.progress;
        for (de.tudarmstadt.ukp.clarin.webanno.model.SourceDocument sourceDocument : documents) {
            //
            // Export initial CASes
            //
            
            // The initial CAS must always be exported to ensure that the converted source document
            // will *always* have the state it had at the time of the initial import. We we do have
            // a reliably initial CAS and instead lazily convert whenever an annotator starts
            // annotating, then we could end up with two annotators having two different versions of
            // their CAS e.g. if there was a code change in the reader component that affects its
            // output.

            // If the initial CAS does not exist yet, it must be created before export.
            documentService.createOrReadInitialCas(sourceDocument);
            
            File targetDir = new File(aCopyDir.getAbsolutePath() + ANNOTATION_CAS_FOLDER
                    + sourceDocument.getName());
            FileUtils.forceMkdir(targetDir);
            
            File initialCasFile = documentService.getCasFile(sourceDocument,
                    INITIAL_CAS_PSEUDO_USER);
            
            FileUtils.copyFileToDirectory(initialCasFile, targetDir);
            
            LOG.info("Exported annotation document content for user [" + INITIAL_CAS_PSEUDO_USER
                    + "] for source document [" + sourceDocument.getId() + "] in project ["
                    + project.getName() + "] with id [" + project.getId() + "]");

            //
            // Export per-user annotation document
            // 
            
            // Determine which format to use for export
            String formatId;
            if (FORMAT_AUTO.equals(aModel.getFormat())) {
                formatId = sourceDocument.getFormat();
            }
            else {
                formatId = importExportService.getWritableFormatId(aModel.getFormat());
            }
            Class<?> writer = importExportService.getWritableFormats().get(formatId);
            if (writer == null) {
                String msg = "[" + sourceDocument.getName()
                        + "] No writer found for format [" + formatId
                        + "] - exporting as WebAnno TSV instead.";
                aModel.addMessage(msg);
                writer = WebannoTsv3XWriter.class;
            }

            // Export annotations from regular users
            for (de.tudarmstadt.ukp.clarin.webanno.model.AnnotationDocument annotationDocument : 
                    documentService.listAnnotationDocuments(sourceDocument)) {
                // copy annotation document only for ACTIVE users and the state of the 
                // annotation document is not NEW/IGNORE
                if (
                        userRepository.get(annotationDocument.getUser()) != null && 
                        !annotationDocument.getState().equals(AnnotationDocumentState.NEW) && 
                        !annotationDocument.getState().equals(AnnotationDocumentState.IGNORE)
                ) {
                    File annotationDocumentAsSerialisedCasDir = new File(
                            aCopyDir.getAbsolutePath() + ANNOTATION_CAS_FOLDER
                                    + sourceDocument.getName());
                    File annotationDocumentDir = new File(aCopyDir.getAbsolutePath()
                            + ANNOTATION_ORIGINAL_FOLDER + sourceDocument.getName());

                    FileUtils.forceMkdir(annotationDocumentAsSerialisedCasDir);
                    FileUtils.forceMkdir(annotationDocumentDir);

                    File annotationFileAsSerialisedCas = documentService.getCasFile(
                            sourceDocument, annotationDocument.getUser());

                    File annotationFile = null;
                    if (annotationFileAsSerialisedCas.exists() && writer != null) {
                        annotationFile = importExportService.exportAnnotationDocument(
                                sourceDocument, annotationDocument.getUser(), writer,
                                annotationDocument.getUser(), Mode.ANNOTATION, false);
                    }
                    
                    if (annotationFileAsSerialisedCas.exists()) {
                        FileUtils.copyFileToDirectory(annotationFileAsSerialisedCas,
                                annotationDocumentAsSerialisedCasDir);
                        if (writer != null) {
                            FileUtils.copyFileToDirectory(annotationFile, annotationDocumentDir);
                            FileUtils.forceDelete(annotationFile);
                        }
                    }
                    
                    LOG.info("Exported annotation document content for user ["
                            + annotationDocument.getUser() + "] for source document ["
                            + sourceDocument.getId() + "] in project [" + project.getName()
                            + "] with id [" + project.getId() + "]");
                }
            }
            
            // BEGIN FIXME #1224 CURATION_USER and CORRECTION_USER files should be exported in
            // annotation_ser
            // If this project is a correction project, add the auto-annotated CAS to same
            // folder as CURATION_FOLDER
            if (WebAnnoConst.PROJECT_TYPE_AUTOMATION.equals(project.getMode())
                    || WebAnnoConst.PROJECT_TYPE_CORRECTION.equals(project.getMode())) {
                File correctionCasFile = documentService.getCasFile(sourceDocument,
                        CORRECTION_USER);
                if (correctionCasFile.exists()) {
                    // Copy CAS - this is used when importing the project again
                    File curationCasDir = new File(aCopyDir + CURATION_AS_SERIALISED_CAS
                            + sourceDocument.getName());
                    FileUtils.forceMkdir(curationCasDir);
                    FileUtils.copyFileToDirectory(correctionCasFile, curationCasDir);
                    
                    // Copy secondary export format for convenience - not used during import
                    File curationDir = new File(
                            aCopyDir + CURATION_FOLDER + sourceDocument.getName());
                    FileUtils.forceMkdir(curationDir);
                    File correctionFile = importExportService.exportAnnotationDocument(
                            sourceDocument, CORRECTION_USER, writer, CORRECTION_USER,
                            Mode.CORRECTION);
                    FileUtils.copyFileToDirectory(correctionFile, curationDir);
                    FileUtils.forceDelete(correctionFile);
                }
            }
            // END FIXME #1224 CURATION_USER and CORRECTION_USER files should be exported in
            // annotation_ser
            
            aModel.progress = initProgress
                    + (int) Math.ceil(((double) i) / documents.size() * 80.0);
            i++;
        }
    }

    /**
     * Copy Project logs from the file system of this project to the export folder
     */
    @Deprecated
    public static void exportProjectLog(ProjectService projectService, Project aProject,
            File aCopyDir)
        throws IOException
    {
        File logDir = new File(aCopyDir + LOG_FOLDER);
        FileUtils.forceMkdir(logDir);
        if (projectService.getProjectLogFile(aProject).exists()) {
            FileUtils.copyFileToDirectory(projectService.getProjectLogFile(aProject), logDir);
        }
    }

    /**
     * Copy Project guidelines from the file system of this project to the export folder
     */
    @Deprecated
    public static void exportGuideLine(ProjectService projectService, Project aProject,
            File aCopyDir)
        throws IOException
    {
        File guidelineDir = new File(aCopyDir + GUIDELINES_FOLDER);
        FileUtils.forceMkdir(guidelineDir);
        File annotationGuidlines = projectService.getGuidelinesFolder(aProject);
        if (annotationGuidlines.exists()) {
            for (File annotationGuideline : annotationGuidlines.listFiles()) {
                FileUtils.copyFileToDirectory(annotationGuideline, guidelineDir);
            }
        }
    }

    /**
     * Copy Project guidelines from the file system of this project to the export folder
     */
    @Deprecated
    public static void exportProjectMetaInf(ProjectService projectService, Project aProject,
            File aCopyDir)
        throws IOException
    {
        File metaInfDir = new File(aCopyDir + META_INF);
        FileUtils.forceMkdir(metaInfDir);
        File metaInf = projectService.getMetaInfFolder(aProject);
        if (metaInf.exists()) {
            FileUtils.copyDirectory(metaInf, metaInfDir);
        }
    }
    
    /**
     * Copy Project Constraints from file system of this project to export folder
     */
    @Deprecated
    public static void exportProjectConstraints(ConstraintsService constraintsService,
            Project project, File exportTempDir)
        throws IOException
    {
        File constraintsDir = new File(exportTempDir + CONSTRAINTS);
        FileUtils.forceMkdir(constraintsDir);
        String fileName;
        for (ConstraintSet set : constraintsService.listConstraintSets(project)) {
            fileName = set.getName();
            // Copying with file's original name to save ConstraintSet's name
            FileUtils.copyFile(constraintsService.exportConstraintAsFile(set),
                    new File(constraintsDir, fileName));
        }
    }
    
    /**
     * Copy, if exists, curation documents to a folder that will be exported as Zip file
     * 
     * @param aCopyDir
     *            The folder where curated documents are copied to be exported as Zip File
     */
    @Deprecated
    public static void exportCuratedDocuments(DocumentService documentService,
            ImportExportService importExportService, ProjectExportRequest aModel, File aCopyDir,
            boolean aIncludeInProgress)
        throws UIMAException, IOException, ClassNotFoundException,
        ProjectExportException
    {
        Project project = aModel.getProject();
        
        // Get all the source documents from the project
        List<de.tudarmstadt.ukp.clarin.webanno.model.SourceDocument> documents = documentService
                .listSourceDocuments(project);

        // Determine which format to use for export.
        Class<?> writer;
        if (FORMAT_AUTO.equals(aModel.getFormat())) {
            writer = WebannoTsv3XWriter.class;
        }
        else {
            writer = importExportService.getWritableFormats().get(
                    importExportService.getWritableFormatId(aModel.getFormat()));
            if (writer == null) {
                writer = WebannoTsv3XWriter.class;
            }
        }
        
        int initProgress = aModel.progress - 1;
        int i = 1;
        for (de.tudarmstadt.ukp.clarin.webanno.model.SourceDocument sourceDocument : documents) {
            File curationCasDir = new File(aCopyDir + CURATION_AS_SERIALISED_CAS
                    + sourceDocument.getName());
            FileUtils.forceMkdir(curationCasDir);

            File curationDir = new File(aCopyDir + CURATION_FOLDER + sourceDocument.getName());
            FileUtils.forceMkdir(curationDir);

            // If depending on aInProgress, include only the the curation documents that are
            // finished or also the ones that are in progress
            if (
                (aIncludeInProgress && 
                    SourceDocumentState.CURATION_IN_PROGRESS.equals(sourceDocument.getState())) ||
                SourceDocumentState.CURATION_FINISHED.equals(sourceDocument.getState())
            ) {
                File curationCasFile = documentService.getCasFile(sourceDocument,
                        WebAnnoConst.CURATION_USER);
                if (curationCasFile.exists()) {
                    // Copy CAS - this is used when importing the project again
                    FileUtils.copyFileToDirectory(curationCasFile, curationCasDir);

                    // Copy secondary export format for convenience - not used during import
                    try {
                        File curationFile = importExportService.exportAnnotationDocument(
                                sourceDocument, WebAnnoConst.CURATION_USER, writer,
                                WebAnnoConst.CURATION_USER, Mode.CURATION);
                        FileUtils.copyFileToDirectory(curationFile, curationDir);
                        FileUtils.forceDelete(curationFile);
                    }
                    catch (Exception e) {
                        // error("Unexpected error while exporting project: " +
                        // ExceptionUtils.getRootCauseMessage(e) );
                        throw new ProjectExportException(
                                "Aborting due to unrecoverable error while exporting!");
                    }
                }
            }

            aModel.progress = initProgress
                    + (int) Math.ceil(((double) i) / documents.size() * 10.0);
            i++;
        }
    }
}

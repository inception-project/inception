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
package de.tudarmstadt.ukp.clarin.webanno.ui.project;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
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
import de.tudarmstadt.ukp.clarin.webanno.export.model.AnnotationDocument;
import de.tudarmstadt.ukp.clarin.webanno.export.model.ProjectPermission;
import de.tudarmstadt.ukp.clarin.webanno.export.model.SourceDocument;
import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationDocumentState;
import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationFeature;
import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationLayer;
import de.tudarmstadt.ukp.clarin.webanno.model.ConstraintSet;
import de.tudarmstadt.ukp.clarin.webanno.model.Mode;
import de.tudarmstadt.ukp.clarin.webanno.model.Project;
import de.tudarmstadt.ukp.clarin.webanno.model.SourceDocumentState;
import de.tudarmstadt.ukp.clarin.webanno.model.Tag;
import de.tudarmstadt.ukp.clarin.webanno.model.TagSet;
import de.tudarmstadt.ukp.clarin.webanno.security.UserDao;
import de.tudarmstadt.ukp.clarin.webanno.security.model.User;
import de.tudarmstadt.ukp.clarin.webanno.tsv.WebannoTsv3Writer;
import de.tudarmstadt.ukp.clarin.webanno.ui.project.ImportUtil;
import de.tudarmstadt.ukp.clarin.webanno.ui.project.ProjectExportException;
import de.tudarmstadt.ukp.clarin.webanno.ui.project.ProjectPage;
import de.tudarmstadt.ukp.clarin.webanno.ui.project.ProjectExportPanel.ProjectExportModel;

public class ExportUtil
{
    private static final Logger LOG = LoggerFactory.getLogger(ProjectPage.class);
    
    private static final String FORMAT_AUTO = "AUTO";
    
    private static final String ANNOTATION_ORIGINAL_FOLDER = "/annotation/";
    private static final String CONSTRAINTS = "/constraints/";
    private static final String LOG_FOLDER = "/" + ProjectService.LOG_DIR;
    private static final String GUIDELINES_FOLDER = "/" + ImportUtil.GUIDELINE;
    private static final String ANNOTATION_CAS_FOLDER = "/"
            + ImportUtil.ANNOTATION_AS_SERIALISED_CAS + "/";
    private static final String META_INF = "/" + ImportUtil.META_INF;
    private static final String SOURCE_FOLDER = "/" + ImportUtil.SOURCE;
    private static final String CORRECTION_USER = "CORRECTION_USER";
    private static final String CURATION_AS_SERIALISED_CAS = "/"
            + ImportUtil.CURATION_AS_SERIALISED_CAS + "/";
    private static final String CURATION_FOLDER = "/curation/";

    public ExportUtil()
    {
        // TODO Auto-generated constructor stub
    }

    public static de.tudarmstadt.ukp.clarin.webanno.export.model.Project exportProjectSettings(
            AnnotationSchemaService annotationService, AutomationService automationService,
            DocumentService documentService, ProjectService projectService, Project aProject,
            File aProjectSettings, File aExportTempDir)
    {
        de.tudarmstadt.ukp.clarin.webanno.export.model.Project exProjekt = new de.tudarmstadt.ukp.clarin.webanno.export.model.Project();
        exProjekt.setDescription(aProject.getDescription());
        exProjekt.setName(aProject.getName());
        // In older versions of WebAnno, the mode was an enum which was serialized as upper-case
        // during export but as lower-case in the database. This is compensating for this case.
        exProjekt.setMode(StringUtils.upperCase(aProject.getMode(), Locale.US));
        exProjekt.setScriptDirection(aProject.getScriptDirection());
        exProjekt.setVersion(aProject.getVersion());
        exProjekt.setDisableExport(aProject.isDisableExport());

        List<de.tudarmstadt.ukp.clarin.webanno.export.model.AnnotationLayer> exLayers = new ArrayList<>();
        // Store map of layer and its equivalent exLayer so that the attach type is attached later
        Map<AnnotationLayer, de.tudarmstadt.ukp.clarin.webanno.export.model.AnnotationLayer> layerToExLayers = new HashMap<>();
        // Store map of feature and its equivalent exFeature so that the attach feature is attached
        // later
        Map<AnnotationFeature, de.tudarmstadt.ukp.clarin.webanno.export.model.AnnotationFeature> featureToExFeatures = new HashMap<>();
        for (AnnotationLayer layer : annotationService.listAnnotationLayer(aProject)) {
            exLayers.add(ImportUtil.exportLayerDetails(layerToExLayers, featureToExFeatures,
                    layer, annotationService));
        }

        // add the attach type and attache feature to the exported layer and
        // exported feature
        for (AnnotationLayer layer : layerToExLayers.keySet()) {
            if (layer.getAttachType() != null) {
                layerToExLayers.get(layer).setAttachType(
                        layerToExLayers.get(layer.getAttachType()));
            }
            if (layer.getAttachFeature() != null) {
                layerToExLayers.get(layer).setAttachFeature(
                        featureToExFeatures.get(layer.getAttachFeature()));
            }
        }
        exProjekt.setLayers(exLayers);

        List<de.tudarmstadt.ukp.clarin.webanno.export.model.TagSet> extTagSets = new ArrayList<>();
        for (TagSet tagSet : annotationService.listTagSets(aProject)) {
            de.tudarmstadt.ukp.clarin.webanno.export.model.TagSet exTagSet = new de.tudarmstadt.ukp.clarin.webanno.export.model.TagSet();
            exTagSet.setCreateTag(tagSet.isCreateTag());
            exTagSet.setDescription(tagSet.getDescription());
            exTagSet.setLanguage(tagSet.getLanguage());
            exTagSet.setName(tagSet.getName());
            List<de.tudarmstadt.ukp.clarin.webanno.export.model.Tag> exTags = new ArrayList<>();
            for (Tag tag : annotationService.listTags(tagSet)) {
                de.tudarmstadt.ukp.clarin.webanno.export.model.Tag exTag = new de.tudarmstadt.ukp.clarin.webanno.export.model.Tag();
                exTag.setDescription(tag.getDescription());
                exTag.setName(tag.getName());
                exTags.add(exTag);
            }
            exTagSet.setTags(exTags);
            extTagSets.add(exTagSet);
        }

        exProjekt.setTagSets(extTagSets);
        List<SourceDocument> sourceDocuments = new ArrayList<SourceDocument>();
        List<AnnotationDocument> annotationDocuments = new ArrayList<AnnotationDocument>();

        // Store map of source document and exSourceDocument
        Map<de.tudarmstadt.ukp.clarin.webanno.model.SourceDocument, SourceDocument> exDocuments = new HashMap<>();
        // add source documents to a project
        List<de.tudarmstadt.ukp.clarin.webanno.model.SourceDocument> documents = documentService
                .listSourceDocuments(aProject);
        for (de.tudarmstadt.ukp.clarin.webanno.model.SourceDocument sourceDocument : documents) {

            SourceDocument exDocument = new SourceDocument();
            exDocument.setFormat(sourceDocument.getFormat());
            exDocument.setName(sourceDocument.getName());
            exDocument.setState(sourceDocument.getState());
            exDocument.setTimestamp(sourceDocument.getTimestamp());
            exDocument.setSentenceAccessed(sourceDocument.getSentenceAccessed());
            exDocument.setProcessed(false);

            // add annotation document to Project
            for (de.tudarmstadt.ukp.clarin.webanno.model.AnnotationDocument annotationDocument : documentService
                    .listAnnotationDocuments(sourceDocument)) {
                AnnotationDocument annotationDocumentToExport = new AnnotationDocument();
                annotationDocumentToExport.setName(annotationDocument.getName());
                annotationDocumentToExport.setState(annotationDocument.getState());
                annotationDocumentToExport.setUser(annotationDocument.getUser());
                annotationDocumentToExport.setTimestamp(annotationDocument.getTimestamp());
                annotationDocumentToExport
                        .setSentenceAccessed(annotationDocument.getSentenceAccessed());
                annotationDocuments.add(annotationDocumentToExport);
            }
            sourceDocuments.add(exDocument);
            exDocuments.put(sourceDocument, exDocument);
        }

        exProjekt.setSourceDocuments(sourceDocuments);
        exProjekt.setAnnotationDocuments(annotationDocuments);

        List<ProjectPermission> projectPermissions = new ArrayList<ProjectPermission>();

        // add project permissions to the project
        for (User user : projectService.listProjectUsersWithPermissions(aProject)) {
            for (de.tudarmstadt.ukp.clarin.webanno.model.ProjectPermission permission : projectService
                    .listProjectPermissionLevel(user, aProject)) {
                ProjectPermission permissionToExport = new ProjectPermission();
                permissionToExport.setLevel(permission.getLevel());
                permissionToExport.setUser(user.getUsername());
                projectPermissions.add(permissionToExport);
            }
        }

        exProjekt.setProjectPermissions(projectPermissions);

        // export automation Mira template
        List<de.tudarmstadt.ukp.clarin.webanno.export.model.MiraTemplate> exTemplates = new ArrayList<>();
        for (MiraTemplate template : automationService.listMiraTemplates(aProject)) {
            de.tudarmstadt.ukp.clarin.webanno.export.model.MiraTemplate exTemplate = new de.tudarmstadt.ukp.clarin.webanno.export.model.MiraTemplate();
            exTemplate.setAnnotateAndPredict(template.isAnnotateAndRepeat());
            exTemplate.setAutomationStarted(template.isAutomationStarted());
            exTemplate.setCurrentLayer(template.isCurrentLayer());
            exTemplate.setResult(template.getResult());
            exTemplate.setTrainFeature(featureToExFeatures.get(template.getTrainFeature()));

            if (template.getOtherFeatures().size() > 0) {
                Set<de.tudarmstadt.ukp.clarin.webanno.export.model.AnnotationFeature> exOtherFeatures = new HashSet<>();
                for (AnnotationFeature feature : template.getOtherFeatures()) {
                    exOtherFeatures.add(featureToExFeatures.get(feature));
                }
                exTemplate.setOtherFeatures(exOtherFeatures);
            }
            exTemplates.add(exTemplate);
        }

        exProjekt.setMiraTemplates(exTemplates);
        
        return exProjekt;
    }

    /**
     * Copy source documents from the file system of this project to the export folder
     */
    public static void exportSourceDocuments(DocumentService documentService,
            AutomationService automationService, ProjectExportModel model, Project aProject,
            File aCopyDir)
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
            } catch (FileNotFoundException e) {
//              error(e.getMessage());
                StringBuffer errorMessage = new StringBuffer();
                errorMessage.append("Source file '");
                errorMessage.append(sourceDocument.getName());
                errorMessage.append("' related to project couldn't be located in repository");
                LOG.error(errorMessage.toString(), ExceptionUtils.getRootCause(e));
                model.messages.add(errorMessage.toString());
                throw new ProjectExportException("Couldn't find some source file(s) related to project");
//              continue;
                
            }
        }
    }

    /**
     * Copy annotation document as Serialized CAS from the file system of this project to the
     * export folder.
     */
    public static void exportAnnotationDocuments(DocumentService documentService,
            ImportExportService importExportService, UserDao userRepository,
            ProjectExportModel aModel, File aCopyDir)
        throws IOException, UIMAException, ClassNotFoundException
    {
        List<de.tudarmstadt.ukp.clarin.webanno.model.SourceDocument> documents = documentService
                .listSourceDocuments(aModel.project);
        int i = 1;
        int initProgress = aModel.progress;
        for (de.tudarmstadt.ukp.clarin.webanno.model.SourceDocument sourceDocument : documents) {
            // Determine which format to use for export
            String formatId;
            if (FORMAT_AUTO.equals(aModel.format)) {
                formatId = sourceDocument.getFormat();
            }
            else {
                formatId = importExportService.getWritableFormatId(aModel.format);
            }
            Class<?> writer = importExportService.getWritableFormats().get(formatId);
            if (writer == null) {
                String msg = "[" + sourceDocument.getName()
                        + "] No writer found for format [" + formatId
                        + "] - exporting as WebAnno TSV instead.";
                // Avoid repeating the same message over for different users
                if (!aModel.messages.contains(msg)) {
                    aModel.messages.add(msg);
                }
                writer = WebannoTsv3Writer.class;
            }

            // Export annotations from regular users
            for (de.tudarmstadt.ukp.clarin.webanno.model.AnnotationDocument annotationDocument : documentService
                    .listAnnotationDocuments(sourceDocument)) {
                // copy annotation document only for ACTIVE users and the state of the 
                // annotation document is not NEW/IGNORE
                if (userRepository.get(annotationDocument.getUser()) != null
                        && !annotationDocument.getState().equals(AnnotationDocumentState.NEW)
                        && !annotationDocument.getState()
                                .equals(AnnotationDocumentState.IGNORE)) {
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
                        annotationFile = importExportService.exportAnnotationDocument(sourceDocument,
                                annotationDocument.getUser(), writer,
                                annotationDocument.getUser(), Mode.ANNOTATION, false);
                    }
                    if (annotationFileAsSerialisedCas.exists()) {
                        FileUtils.copyFileToDirectory(annotationFileAsSerialisedCas,
                                annotationDocumentAsSerialisedCasDir);
                        if (writer != null) {
                            FileUtils
                                    .copyFileToDirectory(annotationFile, annotationDocumentDir);
                            FileUtils.forceDelete(annotationFile);
                        }
                    }
                }
            }
            
            // BEGIN FIXME #1224 CURATION_USER and CORRECTION_USER files should be exported in annotation_ser
            // If this project is a correction project, add the auto-annotated  CAS to same 
            // folder as CURATION_FOLDER
            if (WebAnnoConst.PROJECT_TYPE_AUTOMATION.equals(aModel.project.getMode())
                    || WebAnnoConst.PROJECT_TYPE_CORRECTION.equals(aModel.project.getMode())) {
                File correctionCasFile = documentService.getCasFile(sourceDocument,
                        CORRECTION_USER);
                if (correctionCasFile.exists()) {
                    // Copy CAS - this is used when importing the project again
                    File curationCasDir = new File(aCopyDir + CURATION_AS_SERIALISED_CAS
                            + sourceDocument.getName());
                    FileUtils.forceMkdir(curationCasDir);
                    FileUtils.copyFileToDirectory(correctionCasFile, curationCasDir);
                    
                    // Copy secondary export format for convenience - not used during import
                    File curationDir = new File(aCopyDir + CURATION_FOLDER + sourceDocument.getName());
                    FileUtils.forceMkdir(curationDir);
                    File correctionFile = importExportService.exportAnnotationDocument(sourceDocument,
                            CORRECTION_USER, writer, CORRECTION_USER, Mode.CORRECTION);
                    FileUtils.copyFileToDirectory(correctionFile, curationDir);
                    FileUtils.forceDelete(correctionFile);
                }
            }
            // END FIXME #1224 CURATION_USER and CORRECTION_USER files should be exported in annotation_ser
            
            aModel.progress = initProgress + (int) Math.ceil(((double) i) / documents.size() * 80.0);
            i++;
        }
    }

    /**
     * Copy Project logs from the file system of this project to the export folder
     */
    public static void exportProjectLog(ProjectService projectService, Project aProject, File aCopyDir)
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
    public static void exportGuideLine(ProjectService projectService, Project aProject, File aCopyDir)
        throws IOException
    {
        File guidelineDir = new File(aCopyDir + GUIDELINES_FOLDER);
        FileUtils.forceMkdir(guidelineDir);
        File annotationGuidlines = projectService.getGuidelinesFile(aProject);
        if (annotationGuidlines.exists()) {
            for (File annotationGuideline : annotationGuidlines.listFiles()) {
                FileUtils.copyFileToDirectory(annotationGuideline, guidelineDir);
            }
        }
    }

    /**
     * Copy Project guidelines from the file system of this project to the export folder
     */
    public static void exportProjectMetaInf(ProjectService projectService, Project aProject, File aCopyDir)
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
    public static void exportProjectConstraints(ConstraintsService constraintsService, Project project,
            File exportTempDir)
        throws IOException
    {
        File constraintsDir = new File(exportTempDir + CONSTRAINTS);
        FileUtils.forceMkdir(constraintsDir);
        String fileName;
        for (ConstraintSet set : constraintsService.listConstraintSets(project)) {
            fileName = set.getName();
         /*
          * Copying with file's original name to save ConstraintSet's name
          */
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
    public static void exportCuratedDocuments(DocumentService documentService,
            ImportExportService importExportService, ProjectExportModel aModel, File aCopyDir,
            boolean aIncludeInProgress)
        throws FileNotFoundException, UIMAException, IOException, ClassNotFoundException,
        ProjectExportException
    {
        // Get all the source documents from the project
        List<de.tudarmstadt.ukp.clarin.webanno.model.SourceDocument> documents = documentService
                .listSourceDocuments(aModel.project);

        // Determine which format to use for export.
        Class<?> writer;
        if (FORMAT_AUTO.equals(aModel.format)) {
            writer = WebannoTsv3Writer.class;
        }
        else {
            writer = importExportService.getWritableFormats().get(
                    importExportService.getWritableFormatId(aModel.format));
            if (writer == null) {
                writer = WebannoTsv3Writer.class;
            }
        }
        
        int initProgress = aModel.progress-1;
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
                File curationCasFile = documentService.getCasFile(sourceDocument, WebAnnoConst.CURATION_USER);
                if (curationCasFile.exists()) {
                    // Copy CAS - this is used when importing the project again
                    FileUtils.copyFileToDirectory(curationCasFile, curationCasDir);
                    
                    // Copy secondary export format for convenience - not used during import
                    try {
                        File curationFile = importExportService.exportAnnotationDocument(sourceDocument,
                                WebAnnoConst.CURATION_USER, writer, WebAnnoConst.CURATION_USER, Mode.CURATION);
                        FileUtils.copyFileToDirectory(curationFile, curationDir);
                        FileUtils.forceDelete(curationFile);
                    } catch (Exception e) {
                        //error("Unexpected error while exporting project: " + ExceptionUtils.getRootCauseMessage(e) );
                        throw new ProjectExportException("Aborting due to unrecoverable error while exporting!");
                    }
                }
            }
            
            aModel.progress = initProgress+ (int) Math.ceil(((double) i)/documents.size()*10.0);
            i++;
        }
    }
}

/*
 * Copyright 2018
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
package de.tudarmstadt.ukp.clarin.webanno.automation.service.export;

import static java.util.Arrays.asList;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import de.tudarmstadt.ukp.clarin.webanno.api.AnnotationSchemaService;
import de.tudarmstadt.ukp.clarin.webanno.api.dao.export.exporters.LayerExporter;
import de.tudarmstadt.ukp.clarin.webanno.api.export.ProjectExportException;
import de.tudarmstadt.ukp.clarin.webanno.api.export.ProjectExportRequest;
import de.tudarmstadt.ukp.clarin.webanno.api.export.ProjectExporter;
import de.tudarmstadt.ukp.clarin.webanno.api.export.ProjectImportRequest;
import de.tudarmstadt.ukp.clarin.webanno.automation.service.AutomationService;
import de.tudarmstadt.ukp.clarin.webanno.automation.service.export.model.ExportedTrainingDocument;
import de.tudarmstadt.ukp.clarin.webanno.export.model.ExportedAnnotationFeatureReference;
import de.tudarmstadt.ukp.clarin.webanno.export.model.ExportedProject;
import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationFeature;
import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationLayer;
import de.tudarmstadt.ukp.clarin.webanno.model.Project;
import de.tudarmstadt.ukp.clarin.webanno.model.TrainingDocument;
import de.tudarmstadt.ukp.clarin.webanno.support.logging.LogMessage;

@Component
public class AutomationTrainingDocumentExporter
    implements ProjectExporter
{
    private static final String TRAIN = "train";
    private static final String TRAIN_FOLDER = "/" + TRAIN;
    private static final String TRAINING_DOCUMENTS = "training_documents";
    
    private final Logger log = LoggerFactory.getLogger(getClass());

    private @Autowired AutomationService automationService;
    private @Autowired AnnotationSchemaService annotationService;

    @Override
    public List<Class<? extends ProjectExporter>> getImportDependencies()
    {
        return asList(LayerExporter.class);
    }

    @Override
    public void exportData(ProjectExportRequest aRequest, ExportedProject aExProject, File aCopyDir)
        throws Exception
    {
        exportTrainingDocuments(aRequest.getProject(), aExProject);
        exportTrainingDocumentContents(aRequest, aExProject, aCopyDir);
    }
    
    private void exportTrainingDocuments(Project aProject, ExportedProject aExProject)
    {
        List<ExportedTrainingDocument> trainDocuments = new ArrayList<>();
        List<TrainingDocument> trainingDocuments = automationService
                .listTrainingDocuments(aProject);
        
        for (TrainingDocument trainingDocument : trainingDocuments) {
            ExportedTrainingDocument exDocument = new ExportedTrainingDocument();
            exDocument.setFormat(trainingDocument.getFormat());
            exDocument.setName(trainingDocument.getName());
            exDocument.setState(trainingDocument.getState());
            exDocument.setTimestamp(trainingDocument.getTimestamp());
            exDocument.setSentenceAccessed(trainingDocument.getSentenceAccessed());
            // During imported, we only really use the name of the feature to look up the
            // actual AnnotationFeature in the project
            if (trainingDocument.getFeature() != null) {
                exDocument.setFeature(
                        new ExportedAnnotationFeatureReference(trainingDocument.getFeature()));
            }
            trainDocuments.add(exDocument);
        }
        
        aExProject.setProperty(TRAINING_DOCUMENTS, trainDocuments);
    }
    
    private void exportTrainingDocumentContents(ProjectExportRequest aRequest,
            ExportedProject aExProject, File aCopyDir)
        throws IOException, ProjectExportException
    {
        Project project = aRequest.getProject();
        File trainDocumentDir = new File(aCopyDir + TRAIN_FOLDER);
        FileUtils.forceMkdir(trainDocumentDir);
        // Get all the training documents from the project
        List<TrainingDocument> documents = automationService.listTrainingDocuments(project);
        int i = 1;
        for (TrainingDocument trainingDocument : documents) {
            try {
                FileUtils.copyFileToDirectory(
                        automationService.getTrainingDocumentFile(trainingDocument),
                        trainDocumentDir);
                aRequest.progress = (int) Math.ceil(((double) i) / documents.size() * 10.0);
                i++;
                log.info("Imported content for training document [" + trainingDocument.getId()
                        + "] in project [" + project.getName() + "] with id [" + project.getId()
                        + "]");
            }
            catch (FileNotFoundException e) {
                log.error("Source file [{}] related to project couldn't be located in repository",
                        trainingDocument.getName(), ExceptionUtils.getRootCause(e));
                aRequest.addMessage(LogMessage.error(this,
                        "Source file [%s] related to project couldn't be located in repository",
                        trainingDocument.getName()));
                throw new ProjectExportException(
                        "Couldn't find some source file(s) related to project");
            }
        }
    }
    
    @Override
    public void importData(ProjectImportRequest aRequest, Project aProject,
            ExportedProject aExProject, ZipFile aZip)
        throws Exception
    {
        importTrainingDocuments(aExProject, aProject);
        importTrainingDocumentContents(aZip, aProject);
    }
    
    private void importTrainingDocuments(ExportedProject aExProject, Project aProject)
        throws IOException
    {
        ExportedTrainingDocument[] trainingDocuments = aExProject
                .getArrayProperty(TRAINING_DOCUMENTS, ExportedTrainingDocument.class);
        
        for (ExportedTrainingDocument importedTrainingDocument : trainingDocuments) {
            TrainingDocument trainingDocument = new TrainingDocument();
            trainingDocument.setFormat(importedTrainingDocument.getFormat());
            trainingDocument.setName(importedTrainingDocument.getName());
            trainingDocument.setState(importedTrainingDocument.getState());
            trainingDocument.setProject(aProject);
            trainingDocument.setTimestamp(importedTrainingDocument.getTimestamp());
            trainingDocument.setSentenceAccessed(importedTrainingDocument.getSentenceAccessed());
            if (importedTrainingDocument.getFeature() != null) {
                AnnotationLayer trainingLayer = annotationService
                        .findLayer(aProject, importedTrainingDocument.getFeature().getLayer());
                AnnotationFeature trainingFeature = annotationService
                        .getFeature(importedTrainingDocument.getFeature().getName(), trainingLayer);
                trainingDocument.setFeature(trainingFeature);
            }
            automationService.createTrainingDocument(trainingDocument);
        }
    }

    private void importTrainingDocumentContents(ZipFile zip, Project aProject) throws IOException
    {
        for (Enumeration<? extends ZipEntry> zipEnumerate = zip.entries(); zipEnumerate
                .hasMoreElements();) {
            ZipEntry entry = zipEnumerate.nextElement();

            // Strip leading "/" that we had in ZIP files prior to 2.0.8 (bug #985)
            String entryName = ProjectExporter.normalizeEntryName(entry);

            if (entryName.startsWith(TRAIN)) {
                String fileName = FilenameUtils.getName(entryName);
                if (fileName.trim().isEmpty()) {
                    continue;
                }
                TrainingDocument trainingDocument = automationService.getTrainingDocument(aProject,
                        fileName);
                File trainigFilePath = automationService.getTrainingDocumentFile(trainingDocument);
                FileUtils.copyInputStreamToFile(zip.getInputStream(entry), trainigFilePath);

                log.info("Imported content for training document [" + trainingDocument.getId()
                        + "] in project [" + aProject.getName() + "] with id [" + aProject.getId()
                        + "]");
            }
        }
    }
}

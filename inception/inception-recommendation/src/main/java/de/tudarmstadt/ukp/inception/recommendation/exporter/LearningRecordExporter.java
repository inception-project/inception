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
package de.tudarmstadt.ukp.inception.recommendation.exporter;

import static java.util.Arrays.asList;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.zip.ZipFile;
import java.util.zip.ZipOutputStream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import de.tudarmstadt.ukp.clarin.webanno.api.export.FullProjectExportRequest;
import de.tudarmstadt.ukp.clarin.webanno.api.export.ProjectExportTaskMonitor;
import de.tudarmstadt.ukp.clarin.webanno.api.export.ProjectExporter;
import de.tudarmstadt.ukp.clarin.webanno.api.export.ProjectImportRequest;
import de.tudarmstadt.ukp.clarin.webanno.export.model.ExportedProject;
import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationFeature;
import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationLayer;
import de.tudarmstadt.ukp.clarin.webanno.model.Project;
import de.tudarmstadt.ukp.clarin.webanno.model.SourceDocument;
import de.tudarmstadt.ukp.inception.documents.api.DocumentService;
import de.tudarmstadt.ukp.inception.recommendation.api.LearningRecordService;
import de.tudarmstadt.ukp.inception.recommendation.api.model.LearningRecord;
import de.tudarmstadt.ukp.inception.recommendation.config.RecommenderServiceAutoConfiguration;
import de.tudarmstadt.ukp.inception.schema.api.AnnotationSchemaService;
import de.tudarmstadt.ukp.inception.schema.exporters.AnnotationDocumentExporter;
import de.tudarmstadt.ukp.inception.schema.exporters.LayerExporter;

/**
 * <p>
 * This class is exposed as a Spring Component via
 * {@link RecommenderServiceAutoConfiguration#learningRecordExporter}.
 * </p>
 */
public class LearningRecordExporter
    implements ProjectExporter
{
    private static final String KEY = "learning_records";
    private static final Logger LOG = LoggerFactory.getLogger(LearningRecordExporter.class);

    private final AnnotationSchemaService annotationService;
    private final DocumentService documentService;
    private final LearningRecordService learningRecordService;

    @Autowired
    public LearningRecordExporter(AnnotationSchemaService aAnnotationService,
            DocumentService aDocumentService, LearningRecordService aLearningRecordService)
    {
        annotationService = aAnnotationService;
        documentService = aDocumentService;
        learningRecordService = aLearningRecordService;
    }

    @Override
    public List<Class<? extends ProjectExporter>> getImportDependencies()
    {
        return asList(LayerExporter.class, AnnotationDocumentExporter.class);
    }

    @Override
    public List<Class<? extends ProjectExporter>> getExportDependencies()
    {
        return asList(LayerExporter.class, AnnotationDocumentExporter.class);
    }

    @Override
    public void exportData(FullProjectExportRequest aRequest, ProjectExportTaskMonitor aMonitor,
            ExportedProject aExProject, ZipOutputStream aFile)
    {
        var project = aRequest.getProject();

        var exportedLearningRecords = new ArrayList<ExportedLearningRecord>();
        for (var record : learningRecordService.listLearningRecords(project)) {
            var exportedRecord = new ExportedLearningRecord();
            exportedRecord.setAnnotation(record.getAnnotation());
            exportedRecord.setActionDate(record.getActionDate());
            exportedRecord.setChangeLocation(record.getChangeLocation());
            exportedRecord.setDocumentName(record.getSourceDocument().getName());
            exportedRecord.setFeature(record.getAnnotationFeature().getName());
            exportedRecord.setLayerName(record.getLayer().getName());
            exportedRecord.setOffsetBegin(record.getOffsetBegin());
            exportedRecord.setOffsetEnd(record.getOffsetEnd());
            exportedRecord.setOffsetBegin2(record.getOffsetBegin2());
            exportedRecord.setOffsetEnd2(record.getOffsetEnd2());
            exportedRecord.setSuggestionType(record.getSuggestionType());
            exportedRecord.setTokenText(record.getTokenText());
            exportedRecord.setUser(record.getUser());
            exportedRecord.setUserAction(record.getUserAction());
            exportedLearningRecords.add(exportedRecord);
        }

        aExProject.setProperty(KEY, exportedLearningRecords);
        int n = exportedLearningRecords.size();
        LOG.info("Exported [{}] learning records for project [{}]", n, project.getName());
    }

    @Override
    public void importData(ProjectImportRequest aRequest, Project aProject,
            ExportedProject aExProject, ZipFile aZip)
    {
        int eventCount = 0;

        var learningRecords = aExProject.getArrayProperty(KEY, ExportedLearningRecord.class);

        var documentCache = new HashMap<String, SourceDocument>();
        var layerCache = new HashMap<String, AnnotationLayer>();
        var featureCache = new HashMap<String, AnnotationFeature>();

        var batch = new ArrayList<LearningRecord>();
        for (var exportedRecord : learningRecords) {
            // Flush events
            if (batch.size() >= 50_000) {
                learningRecordService
                        .createLearningRecords(batch.stream().toArray(LearningRecord[]::new));
                batch.clear();
                LOG.trace("... {} learning records imported ...", eventCount);
            }

            var record = new LearningRecord();
            record.setAnnotation(exportedRecord.getAnnotation());
            record.setActionDate(exportedRecord.getActionDate());
            record.setChangeLocation(exportedRecord.getChangeLocation());
            record.setOffsetBegin(exportedRecord.getOffsetBegin());
            record.setOffsetEnd(exportedRecord.getOffsetEnd());
            record.setOffsetBegin2(exportedRecord.getOffsetBegin2());
            record.setOffsetEnd2(exportedRecord.getOffsetEnd2());
            record.setSuggestionType(exportedRecord.getSuggestionType());
            record.setTokenText(exportedRecord.getTokenText());
            record.setUser(exportedRecord.getUser());
            record.setUserAction(exportedRecord.getUserAction());

            var document = documentCache.computeIfAbsent(exportedRecord.getDocumentName(),
                    $ -> documentService.getSourceDocument(aProject, $));
            record.setSourceDocument(document);

            var layer = layerCache.computeIfAbsent(exportedRecord.getLayerName(),
                    $ -> annotationService.findLayer(aProject, $));
            record.setLayer(layer);

            var feature = featureCache.computeIfAbsent(exportedRecord.getFeature(),
                    $ -> annotationService.getFeature($, layer));
            record.setAnnotationFeature(feature);

            batch.add(record);
        }

        // Flush remaining records
        learningRecordService.createLearningRecords(batch.stream().toArray(LearningRecord[]::new));

        int n = learningRecords.length;
        LOG.info("Imported [{}] learning records for project [{}]", n, aProject.getName());
    }
}

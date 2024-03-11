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
package de.tudarmstadt.ukp.inception.processing.recommender;

import static de.tudarmstadt.ukp.clarin.webanno.api.casstorage.CasAccessMode.EXCLUSIVE_WRITE_ACCESS;
import static de.tudarmstadt.ukp.clarin.webanno.api.casstorage.CasUpgradeMode.AUTO_CAS_UPGRADE;
import static de.tudarmstadt.ukp.clarin.webanno.model.AnnotationDocumentState.FINISHED;
import static de.tudarmstadt.ukp.clarin.webanno.model.AnnotationDocumentState.IN_PROGRESS;
import static de.tudarmstadt.ukp.clarin.webanno.model.AnnotationDocumentState.NEW;
import static de.tudarmstadt.ukp.clarin.webanno.model.AnnotationDocumentStateChangeFlag.EXPLICIT_ANNOTATOR_USER_ACTION;
import static de.tudarmstadt.ukp.inception.recommendation.api.model.LearningRecordChangeLocation.AUTO_ACCEPT;
import static de.tudarmstadt.ukp.inception.recommendation.api.model.LearningRecordUserAction.ACCEPTED;
import static de.tudarmstadt.ukp.inception.scheduling.TaskScope.PROJECT;

import java.io.IOException;
import java.io.Serializable;
import java.lang.invoke.MethodHandles;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

import org.apache.commons.lang3.Validate;
import org.apache.uima.cas.AnnotationBaseFS;
import org.apache.uima.cas.CAS;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationFeature;
import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationLayer;
import de.tudarmstadt.ukp.clarin.webanno.model.SourceDocument;
import de.tudarmstadt.ukp.clarin.webanno.security.UserDao;
import de.tudarmstadt.ukp.inception.annotation.storage.CasStorageSession;
import de.tudarmstadt.ukp.inception.documents.api.DocumentService;
import de.tudarmstadt.ukp.inception.recommendation.api.SuggestionSupport;
import de.tudarmstadt.ukp.inception.recommendation.api.SuggestionSupportRegistry;
import de.tudarmstadt.ukp.inception.recommendation.api.model.Predictions;
import de.tudarmstadt.ukp.inception.recommendation.api.model.Recommender;
import de.tudarmstadt.ukp.inception.recommendation.tasks.PredictionTask;
import de.tudarmstadt.ukp.inception.recommendation.tasks.RecommendationTask_ImplBase;
import de.tudarmstadt.ukp.inception.scheduling.ProjectTask;
import de.tudarmstadt.ukp.inception.scheduling.SchedulingService;
import de.tudarmstadt.ukp.inception.scheduling.TaskState;
import de.tudarmstadt.ukp.inception.schema.api.AnnotationSchemaService;
import de.tudarmstadt.ukp.inception.schema.api.adapter.AnnotationException;
import de.tudarmstadt.ukp.inception.support.WebAnnoConst;
import de.tudarmstadt.ukp.inception.support.logging.LogMessage;
import de.tudarmstadt.ukp.inception.ui.core.docanno.layer.DocumentMetadataLayerAdapter;
import de.tudarmstadt.ukp.inception.ui.core.docanno.layer.DocumentMetadataLayerSupport;

public class BulkPredictionTask
    extends RecommendationTask_ImplBase
    implements ProjectTask
{
    public static final String TYPE = "BulkPredictionTask";

    private static final Logger LOG = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    private final String dataOwner;
    private final Recommender recommender;
    private final Map<AnnotationFeature, Serializable> processingMetadata;

    private @Autowired UserDao userService;
    private @Autowired DocumentService documentService;
    private @Autowired SchedulingService schedulingService;
    private @Autowired SuggestionSupportRegistry suggestionSupportRegistry;
    private @Autowired AnnotationSchemaService schemaService;

    public BulkPredictionTask(Builder<? extends Builder<?>> aBuilder)
    {
        super(aBuilder.withType(TYPE).withCancellable(true).withScope(PROJECT));

        recommender = aBuilder.recommender;
        dataOwner = aBuilder.dataOwner;
        processingMetadata = aBuilder.processingMetadata;
    }

    @Override
    public String getTitle()
    {
        return "Processing documents of user " + dataOwner + " using " + recommender.getName()
                + "...";
    }

    @Override
    public void execute()
    {
        var dataOwnerUser = userService.get(dataOwner);
        var monitor = getMonitor();
        var processedDocumentsCount = 0;
        var annotationsCount = 0;
        var suggestionsCount = 0;

        while (true) {
            // Find all documents currently in the document (which may have changed since the last
            // iteration)
            var annotatableDocuments = documentService.listAnnotatableDocuments(getProject(),
                    dataOwnerUser);

            // Find all documents that still need processing (i.e. which are in state NEW explicitly
            // or implicitly).
            var processableDocuments = annotatableDocuments.entrySet().stream() //
                    .filter(e -> e.getValue() == null || e.getValue().getState() == NEW) //
                    .map(e -> e.getKey()) //
                    .toList();

            var maxProgress = annotatableDocuments.size();
            var progress = maxProgress - processableDocuments.size();
            if (processableDocuments.isEmpty() || monitor.isCancelled()) {
                monitor.setProgressWithMessage(progress, maxProgress,
                        LogMessage.info(this,
                                "%d annotations generated from %d suggestions in %d documents",
                                annotationsCount, suggestionsCount, processedDocumentsCount));
                if (monitor.isCancelled()) {
                    monitor.setState(TaskState.CANCELLED);
                }
                break;
            }

            var doc = processableDocuments.get(0);
            var annDoc = documentService.createOrGetAnnotationDocument(doc, dataOwnerUser);

            monitor.setProgressWithMessage(progress, maxProgress,
                    LogMessage.info(this, "%s", doc.getName()));

            try (var session = CasStorageSession.openNested()) {
                var predictions = generatePredictions(doc);
                suggestionsCount += predictions.getNewSuggestionCount();

                documentService.setAnnotationDocumentState(annDoc, IN_PROGRESS,
                        EXPLICIT_ANNOTATOR_USER_ACTION);
                var cas = documentService.readAnnotationCas(doc, dataOwner, AUTO_CAS_UPGRADE,
                        EXCLUSIVE_WRITE_ACCESS);

                addProcessingMetadataAnnotation(doc, cas);

                annotationsCount += autoAccept(doc, predictions, cas);

                documentService.writeAnnotationCas(cas, doc, dataOwner, true);
                documentService.setAnnotationDocumentState(annDoc, FINISHED,
                        EXPLICIT_ANNOTATOR_USER_ACTION);

                processedDocumentsCount++;
            }
            catch (IOException e) {
                LOG.error("Error loading/saving CAS for [{}]@{}: {}", dataOwner, doc);
            }
            catch (AnnotationException e) {
                LOG.error("Error creating processing metadata annotation", e);
            }
        }
    }

    private void addProcessingMetadataAnnotation(SourceDocument doc, CAS cas) throws AnnotationException
    {
        var metadataAnnotationCache = new HashMap<AnnotationLayer, AnnotationBaseFS>();
        for (var metadataEntry : processingMetadata.entrySet()) {
            var layer = metadataEntry.getKey().getLayer();
            if (!DocumentMetadataLayerSupport.TYPE.equals(layer.getType())) {
                continue;
            }

            var adapter = (DocumentMetadataLayerAdapter) schemaService.getAdapter(layer);

            var anno = metadataAnnotationCache.get(layer);
            if (anno == null) {
                anno = adapter.add(doc, dataOwner, cas);
                metadataAnnotationCache.put(layer, anno);
            }

            adapter.setFeatureValue(doc, dataOwner, anno, metadataEntry.getKey(),
                    metadataEntry.getValue());
        }
    }

    private Predictions generatePredictions(SourceDocument doc)
    {
        var predictionTask = PredictionTask.builder() //
                .withSessionOwner(getUser().get()) //
                .withTrigger("Bulk prediction") //
                .withCurrentDocument(doc) //
                .withDataOwner(dataOwner) //
                .withParentTask(this) //
                .withIsolated(true) //
                .withRecommender(recommender) //
                .build();
        schedulingService.executeSync(predictionTask);
        return predictionTask.getPredictions();
    }

    private int autoAccept(SourceDocument aDocument, Predictions predictions, CAS cas)
    {
        var accepted = 0;
        var suggestionSupportCache = new HashMap<Recommender, Optional<SuggestionSupport>>();

        for (var prediction : predictions.getPredictionsByDocument(aDocument.getName())) {
            if (!Objects.equals(prediction.getRecommenderId(), recommender.getId())) {
                continue;
            }

            var suggestionSupport = suggestionSupportCache.computeIfAbsent(recommender,
                    suggestionSupportRegistry::findGenericExtension);
            if (suggestionSupport.isEmpty()) {
                continue;
            }

            var feature = recommender.getFeature();
            var adapter = schemaService.getAdapter(recommender.getLayer());
            adapter.silenceEvents();

            try {
                suggestionSupport.get().acceptSuggestion(null, aDocument, dataOwner, cas, adapter,
                        feature, prediction, AUTO_ACCEPT, ACCEPTED);
                accepted++;
            }
            catch (AnnotationException e) {
                LOG.debug("Not auto-accepting suggestion: {}", e.getMessage());
            }
        }

        predictions.log(LogMessage.info(this, "Auto-accepted [%d] suggestions", accepted));
        LOG.debug("Auto-accepted [{}] suggestions", accepted);
        return accepted;
    }

    public static Builder<Builder<?>> builder()
    {
        return new Builder<>();
    }

    public static class Builder<T extends Builder<?>>
        extends RecommendationTask_ImplBase.Builder<T>
    {
        private Recommender recommender;
        private String dataOwner;
        private Map<AnnotationFeature, Serializable> processingMetadata;

        @SuppressWarnings("unchecked")
        public T withRecommender(Recommender aRecommender)
        {
            recommender = aRecommender;
            return (T) this;
        }

        @SuppressWarnings("unchecked")
        public T withProcessingMetadata(Map<AnnotationFeature, Serializable> aProcessingMetadata)
        {
            processingMetadata = aProcessingMetadata;
            return (T) this;
        }

        /**
         * @param aDataOwner
         *            the user owning the annotations currently shown in the editor (this can differ
         *            from the user owning the session e.g. if a manager views another users
         *            annotations or a curator is performing curation to the
         *            {@link WebAnnoConst#CURATION_USER})
         */
        @SuppressWarnings("unchecked")
        public T withDataOwner(String aDataOwner)
        {
            dataOwner = aDataOwner;
            return (T) this;
        }

        public BulkPredictionTask build()
        {
            withProject(recommender.getProject());

            Validate.notNull(sessionOwner, "BulkPredictionTask requires a session owner");
            Validate.notNull(dataOwner, "BulkPredictionTask requires a data owner");
            Validate.notNull(recommender, "BulkPredictionTask requires a recommender");
            Validate.notNull(project, "BulkPredictionTask requires a project");

            return new BulkPredictionTask(this);
        }
    }
}

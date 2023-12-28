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
package de.tudarmstadt.ukp.inception.recommendation.tasks;

import static java.lang.System.currentTimeMillis;
import static java.util.stream.Collectors.toList;

import java.lang.invoke.MethodHandles;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;

import de.tudarmstadt.ukp.clarin.webanno.model.Project;
import de.tudarmstadt.ukp.clarin.webanno.model.SourceDocument;
import de.tudarmstadt.ukp.clarin.webanno.security.model.User;
import de.tudarmstadt.ukp.inception.annotation.storage.CasStorageSession;
import de.tudarmstadt.ukp.inception.documents.api.DocumentService;
import de.tudarmstadt.ukp.inception.recommendation.api.RecommendationService;
import de.tudarmstadt.ukp.inception.recommendation.api.model.Predictions;
import de.tudarmstadt.ukp.inception.recommendation.event.RecommenderTaskNotificationEvent;
import de.tudarmstadt.ukp.inception.support.WebAnnoConst;
import de.tudarmstadt.ukp.inception.support.logging.LogMessage;

public class PredictionTask
    extends RecommendationTask_ImplBase
{
    private static final Logger LOG = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    private @Autowired RecommendationService recommendationService;
    private @Autowired DocumentService documentService;
    private @Autowired ApplicationEventPublisher appEventPublisher;

    private final SourceDocument currentDocument;
    private final int predictionBegin;
    private final int predictionEnd;
    private final String dataOwner;

    /**
     * Create a new training task.
     * 
     * @param aSessionOwner
     *            the user owning the training session.
     * @param aTrigger
     *            the trigger that caused the selection to be scheduled.
     * @param aCurrentDocument
     *            the document currently open in the editor.
     * @param aDataOwner
     *            the user owning the annotations currently shown in the editor (this can differ
     *            from the user owning the session e.g. if a manager views another users annotations
     *            or a curator is performing curation to the {@link WebAnnoConst#CURATION_USER})
     */
    public PredictionTask(User aSessionOwner, String aTrigger, SourceDocument aCurrentDocument,
            String aDataOwner)
    {
        this(aSessionOwner, aTrigger, aCurrentDocument, -1, -1, aDataOwner);
    }

    public PredictionTask(User aSessionOwner, String aTrigger, SourceDocument aCurrentDocument,
            int aBegin, int aEnd, String aDataOwner)
    {
        super(aSessionOwner, aCurrentDocument.getProject(), aTrigger);
        currentDocument = aCurrentDocument;
        predictionBegin = aBegin;
        predictionEnd = aEnd;
        dataOwner = aDataOwner;
    }

    @Override
    public String getTitle()
    {
        return "Generating annotation suggestions...";
    }

    @Override
    public void execute()
    {
        try (var session = CasStorageSession.openNested()) {
            var project = getProject();
            var sessionOwner = getUser().orElseThrow();
            var sessionOwnerName = sessionOwner.getUsername();

            var startTime = System.currentTimeMillis();
            var predictions = generatePredictions(sessionOwner);
            predictions.inheritLog(getLogMessages());
            logPredictionComplete(predictions, startTime, sessionOwnerName);

            recommendationService.putIncomingPredictions(sessionOwner, project, predictions);

            if (predictions.hasNewSuggestions()) {
                appEventPublisher.publishEvent(RecommenderTaskNotificationEvent
                        .builder(this, project, sessionOwner.getUsername()) //
                        .withMessage(LogMessage.info(this,
                                predictions.getNewSuggestionCount() + " new predictions available" //
                                        + " (some may be hidden/merged)")) //
                        .build());
            }
            else {
                appEventPublisher.publishEvent(RecommenderTaskNotificationEvent
                        .builder(this, project, sessionOwner.getUsername()) //
                        .withMessage(
                                LogMessage.info(this, "Prediction run produced no new suggestions")) //
                        .build());
            }

            // We reset this in case the state was not properly cleared, e.g. the AL session
            // was started but then the browser closed. Places where it is set include
            // - ActiveLearningSideBar::moveToNextRecommendation
            recommendationService.setPredictForAllDocuments(sessionOwnerName, project, false);
        }
    }

    private Predictions generatePredictions(User sessionOwner)
    {
        var project = getProject();
        var sessionOwnerName = sessionOwner.getUsername();
        var docs = documentService.listSourceDocuments(project);

        // Do we need to predict ALL documents (e.g. in active learning mode)
        if (recommendationService.isPredictForAllDocuments(sessionOwnerName, project)) {
            logPredictionStartedForAllDocuments(sessionOwnerName, project, docs);
            return recommendationService.computePredictions(sessionOwner, project, docs, dataOwner,
                    getMonitor());
        }

        // Limit prediction to a single document and inherit the rest
        var inherit = docs.stream() //
                .filter(d -> !d.equals(currentDocument)) //
                .collect(toList());

        logPredictionStartedForOneDocument(sessionOwnerName, project, inherit);

        return recommendationService.computePredictions(sessionOwner, project, currentDocument,
                dataOwner, inherit, predictionBegin, predictionEnd, getMonitor());
    }

    private void logPredictionComplete(Predictions aPredictions, long startTime, String username)
    {
        var duration = currentTimeMillis() - startTime;
        LOG.debug("[{}][{}]: Prediction complete ({} ms)", getId(), username, duration);
        aPredictions.log(LogMessage.info(this, "Prediction complete (%d ms).", duration));
    }

    private void logPredictionStartedForOneDocument(String username, Project project,
            List<SourceDocument> inherit)
    {
        LOG.debug(
                "[{}][{}]: Starting prediction for project [{}] on one document "
                        + "(inheriting [{}]) triggered by [{}]",
                getId(), username, project, inherit.size(), getTrigger());
        info("Starting prediction triggered by [%s]...", getTrigger());
    }

    private void logPredictionStartedForAllDocuments(String username, Project project,
            List<SourceDocument> docs)
    {
        LOG.debug(
                "[{}][{}]: Starting prediction for project [{}] on [{}] documents triggered by [{}]",
                getId(), username, project, docs.size(), getTrigger());
        info("Starting prediction triggered by [%s]...", getTrigger());
    }
}

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

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import de.tudarmstadt.ukp.clarin.webanno.api.DocumentService;
import de.tudarmstadt.ukp.clarin.webanno.model.Project;
import de.tudarmstadt.ukp.clarin.webanno.model.SourceDocument;
import de.tudarmstadt.ukp.clarin.webanno.security.model.User;
import de.tudarmstadt.ukp.inception.annotation.storage.CasStorageSession;
import de.tudarmstadt.ukp.inception.recommendation.api.RecommendationService;
import de.tudarmstadt.ukp.inception.recommendation.api.model.Predictions;

/**
 * This consumer predicts new annotations for a given annotation layer, if a classification tool for
 * this layer was selected previously.
 */
public class PredictionTask
    extends RecommendationTask_ImplBase
{
    private Logger log = LoggerFactory.getLogger(getClass());

    private @Autowired RecommendationService recommendationService;
    private @Autowired DocumentService documentService;

    private final SourceDocument currentDocument;
    private final int predictionBegin;
    private final int predictionEnd;

    public PredictionTask(User aUser, String aTrigger, SourceDocument aCurrentDocument)
    {
        this(aUser, aTrigger, aCurrentDocument, -1, -1);
    }

    public PredictionTask(User aUser, String aTrigger, SourceDocument aCurrentDocument, int aBegin,
            int aEnd)
    {
        super(aUser, aCurrentDocument.getProject(), aTrigger);
        currentDocument = aCurrentDocument;
        predictionBegin = aBegin;
        predictionEnd = aEnd;
    }

    @Override
    public void execute()
    {
        try (CasStorageSession session = CasStorageSession.openNested()) {
            long startTime = System.currentTimeMillis();
            User user = getUser().orElseThrow();
            String username = user.getUsername();
            Project project = getProject();
            Predictions predictions;

            List<SourceDocument> docs = documentService.listSourceDocuments(project);

            // Limit prediction to a single document and inherit the rest?
            if (recommendationService.isPredictForAllDocuments(username, project)) {
                log.debug(
                        "[{}][{}]: Starting prediction for project [{}] on [{}] docs triggered by [{}]",
                        getId(), username, project, docs.size(), getTrigger());

                predictions = recommendationService.computePredictions(user, project, docs);
            }
            else {
                List<SourceDocument> inherit = docs.stream() //
                        .filter(d -> !d.equals(currentDocument)) //
                        .collect(toList());

                log.debug(
                        "[{}][{}]: Starting prediction for project [{}] on one doc "
                                + "(inheriting [{}]) triggered by [{}]",
                        getId(), username, project, inherit.size(), getTrigger());

                predictions = recommendationService.computePredictions(user, project,
                        currentDocument, inherit, predictionBegin, predictionEnd);
            }

            predictions.inheritLog(getLogMessages());

            log.debug("[{}][{}]: Prediction complete ({} ms)", getId(), username,
                    currentTimeMillis() - startTime);

            recommendationService.putIncomingPredictions(user, project, predictions);

            // We reset this in case the state was not properly cleared, e.g. the AL session
            // was started but then the browser closed. Places where it is set include
            // - ActiveLearningSideBar::moveToNextRecommendation
            recommendationService.setPredictForAllDocuments(username, project, false);
        }
    }
}

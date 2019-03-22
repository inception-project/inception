/*
 * Copyright 2017
 * Ubiquitous Knowledge Processing (UKP) Lab
 * Technische Universität Darmstadt
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
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

import java.io.IOException;
import java.util.List;
import java.util.Optional;

import org.apache.uima.UIMAException;
import org.apache.uima.cas.CAS;
import org.apache.uima.cas.CASException;
import org.apache.uima.cas.admin.CASMgr;
import org.apache.uima.cas.impl.CASCompleteSerializer;
import org.apache.uima.cas.impl.Serialization;
import org.apache.uima.resource.ResourceInitializationException;
import org.apache.uima.resource.metadata.TypeSystemDescription;
import org.apache.uima.util.CasCreationUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import de.tudarmstadt.ukp.clarin.webanno.api.AnnotationSchemaService;
import de.tudarmstadt.ukp.clarin.webanno.api.DocumentService;
import de.tudarmstadt.ukp.clarin.webanno.model.Project;
import de.tudarmstadt.ukp.clarin.webanno.model.SourceDocument;
import de.tudarmstadt.ukp.clarin.webanno.security.model.User;
import de.tudarmstadt.ukp.inception.recommendation.api.LearningRecordService;
import de.tudarmstadt.ukp.inception.recommendation.api.RecommendationService;
import de.tudarmstadt.ukp.inception.recommendation.api.model.Predictions;
import de.tudarmstadt.ukp.inception.recommendation.util.PredictionUtil;
import de.tudarmstadt.ukp.inception.scheduling.Task;

/**
 * This consumer predicts new annotations for a given annotation layer, if a classification tool for
 * this layer was selected previously.
 */
public class PredictionTask
    extends Task
{
    

    private Logger log = LoggerFactory.getLogger(getClass());

    private @Autowired AnnotationSchemaService annoService;
    private @Autowired RecommendationService recommendationService;
    private @Autowired DocumentService documentService;
    private @Autowired LearningRecordService learningRecordService;

    public PredictionTask(User aUser, Project aProject, String aTrigger)
    {
        super(aUser, aProject, aTrigger);
    }

    @Override
    public void run()
    {
        User user = getUser();

        Project project = getProject();
        Predictions model = new Predictions(project, getUser());
        List<SourceDocument> documents = documentService.listSourceDocuments(project);

        log.info("[{}]: Starting prediction...", user.getUsername());
        long startTime = System.currentTimeMillis();
        nextDocument: for (SourceDocument document : documents) {
            Optional<CAS> originalCas = Optional.empty();
            Optional<CAS> predictionCas = Optional.empty();
            // We lazily load the CAS only at this point because that allows us to skip
            // loading the CAS entirely if there is no enabled layer or recommender.
            // If the CAS cannot be loaded, then we skip to the next document.
            if (!originalCas.isPresent()) {
                try {
                    originalCas = Optional.of(documentService.readAnnotationCas(document,
                            user.getUsername()).getCas());
                }
                catch (IOException e) {
                    log.error(
                            "Cannot read annotation CAS for user [{}] of document "
                                    + "[{}]({}) in project [{}]({}) - skipping document",
                            user.getUsername(), document.getName(), document.getId(),
                            project.getName(), project.getId(), e);
                    continue nextDocument;
                }
                try {
                    annoService.upgradeCasIfRequired(originalCas.get(), document,
                            user.getUsername());
                }
                catch (UIMAException | IOException e) {
                    log.error(
                            "Cannot upgrade annotation CAS for user [{}] of document "
                                    + "[{}]({}) in project [{}]({}) - skipping document",
                            user.getUsername(), document.getName(), document.getId(),
                            project.getName(), project.getId(), e);
                    continue nextDocument;
                }
                try {
                    predictionCas = Optional.of(cloneCAS(originalCas.get()));
                }
                catch (UIMAException e) {
                    log.error("Cannot clone annotation CAS for user [{}] of document "
                            + "[{}]({}) in project [{}]({}) - skipping document",
                            user.getUsername(), document.getName(), document.getId(),
                            project.getName(), project.getId(), e);
                    continue nextDocument;
                }
            }
            model = PredictionUtil.getPredictions(document, annoService, recommendationService, originalCas, predictionCas, user, project, learningRecordService);
        }
        log.info("[{}]: Prediction complete ({} ms)", user.getUsername(),
                (System.currentTimeMillis() - startTime));

        recommendationService.putIncomingPredictions(getUser(), project, model);
    }
    
    private CAS cloneCAS(CAS aCAS) throws ResourceInitializationException, CASException
    {
        CAS clone = CasCreationUtils.createCas((TypeSystemDescription) null, null, null);
        
        CASCompleteSerializer ser = Serialization.serializeCASComplete((CASMgr) aCAS);
        Serialization.deserializeCASComplete(ser, (CASMgr) clone);
        
        // Make sure JCas is properly initialized too
        clone.getJCas();

        return clone;
    }

}

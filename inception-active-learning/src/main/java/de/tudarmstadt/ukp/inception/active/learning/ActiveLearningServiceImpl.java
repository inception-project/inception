/*
 * Copyright 2018
 * Ubiquitous Knowledge Processing (UKP) Lab
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
package de.tudarmstadt.ukp.inception.active.learning;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.uima.cas.Type;
import org.apache.uima.cas.text.AnnotationFS;
import org.apache.uima.fit.util.CasUtil;
import org.apache.uima.jcas.JCas;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import de.tudarmstadt.ukp.clarin.webanno.api.CasStorageService;
import de.tudarmstadt.ukp.clarin.webanno.api.DocumentService;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.model.AnnotatorState;
import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationLayer;
import de.tudarmstadt.ukp.clarin.webanno.model.Project;
import de.tudarmstadt.ukp.clarin.webanno.model.SourceDocument;
import de.tudarmstadt.ukp.inception.recommendation.api.RecommendationService;
import de.tudarmstadt.ukp.inception.recommendation.api.model.AnnotationObject;
import de.tudarmstadt.ukp.inception.recommendation.api.model.Predictions;

@Component
public class ActiveLearningServiceImpl
    implements ActiveLearningService
{
    private final Logger LOG = LoggerFactory.getLogger(getClass());
    private final DocumentService documentService;
    private final RecommendationService recommendationService;
    private @Autowired CasStorageService casStorageService;

    @Autowired
    public ActiveLearningServiceImpl(DocumentService aDocumentService,
            RecommendationService aRecommendationService)
    {
        documentService = aDocumentService;
        recommendationService = aRecommendationService;
    }

    @Override
    public List<List<AnnotationObject>> getRecommendationFromRecommendationModel(
            AnnotatorState aState, AnnotationLayer aLayer)
    {
        Predictions model = recommendationService.getPredictions(aState.getUser(),
                aState.getProject());

        if (model == null) {
            return new ArrayList<>();
        }

        // getRecommendationsForThisDocument(model);
        return getRecommendationsForWholeProject(model, aLayer, aState.getProject(), aState
            .getUser().getUsername());
    }

//    private List<List<AnnotationObject>> getRecommendationsForThisDocument(AnnotatorState aState,
//            Predictions model, JCas aJcas, AnnotationLayer aLayer)
//    {
//        int windowBegin = 0;
//        int windowEnd = aJcas.getDocumentText().length() - 1;
//        // TODO #176 use the document Id once it it available in the CAS
//        return model.getPredictions(aState.getDocument().getName(), aLayer, windowBegin,
//                windowEnd, aJcas);
//    }

    @Override public List<List<AnnotationObject>> getRecommendationsForWholeProject(
        Predictions model, AnnotationLayer aLayer, Project aProject, String aUsername)
    {
        List<List<AnnotationObject>> result = new ArrayList<>();

        Map<String, List<List<AnnotationObject>>> recommendationsMap = model
            .getPredictionsForWholeProject(aLayer, documentService, true);

        Set<String> documentNameSet = recommendationsMap.keySet();

        for (String documentName : documentNameSet) {

            SourceDocument aDocument = documentService.getSourceDocument(aProject, documentName);
            List<List<AnnotationObject>> aoForEachDocument = recommendationsMap.get(documentName);
            try {
                JCas aJcas = casStorageService.readCas(aDocument, aUsername);
                Type type = CasUtil.getType(aJcas.getCas(), aLayer.getName());
                List<AnnotationFS> existingAnnotations = CasUtil
                    .selectCovered(aJcas.getCas(), type, 0, aJcas.getDocumentText().length() - 1);
                List<Integer> existingOffsets = existingAnnotations.stream().map(e -> e.getBegin())
                    .collect(Collectors.toList());
                for (List<AnnotationObject> aoList : aoForEachDocument) {
                    aoList.removeIf(
                        ao -> existingOffsets.contains(ao.getOffset().getBeginCharacter()));
                }
            }
            catch (IOException e) {
                LOG.error("Error: " + e.getMessage(), e);
            }
            result.addAll(aoForEachDocument);
        }

        result.removeIf(l -> l.isEmpty());
        return result;
    }
    
    public List<AnnotationObject> getFlattenedRecommendationsFromRecommendationModel(JCas aJcas,
            AnnotatorState aState, AnnotationLayer aSelectedLayer)
    {
        int windowBegin = 0;
        int windowEnd = aJcas.getDocumentText().length() - 1;
        Predictions model = recommendationService.getPredictions(aState.getUser(),
                aState.getProject());
        // TODO #176 use the document Id once it it available in the CAS
        return model.getFlattenedPredictions(aState.getDocument().getName(), aSelectedLayer,
            windowBegin, windowEnd, aJcas, true);
    }
}

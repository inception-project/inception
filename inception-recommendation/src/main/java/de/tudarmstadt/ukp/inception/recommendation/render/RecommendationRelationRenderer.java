/*
 * Copyright 2020
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
package de.tudarmstadt.ukp.inception.recommendation.render;

import static de.tudarmstadt.ukp.clarin.webanno.api.annotation.util.WebAnnoCasUtil.getDocumentTitle;
import static org.apache.uima.fit.util.CasUtil.selectAt;

import java.util.Collections;

import org.apache.uima.cas.CAS;
import org.apache.uima.cas.Type;
import org.apache.uima.cas.text.AnnotationFS;
import org.apache.uima.fit.util.CasUtil;

import de.tudarmstadt.ukp.clarin.webanno.api.AnnotationSchemaService;
import de.tudarmstadt.ukp.clarin.webanno.api.DocumentService;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.adapter.RelationAdapter;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.feature.FeatureSupportRegistry;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.model.AnnotatorState;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.model.VID;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.rendering.model.VArc;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.rendering.model.VDocument;
import de.tudarmstadt.ukp.clarin.webanno.api.dao.CasMetadataUtils;
import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationLayer;
import de.tudarmstadt.ukp.inception.recommendation.api.LearningRecordService;
import de.tudarmstadt.ukp.inception.recommendation.api.RecommendationService;
import de.tudarmstadt.ukp.inception.recommendation.api.model.Predictions;
import de.tudarmstadt.ukp.inception.recommendation.api.model.Preferences;
import de.tudarmstadt.ukp.inception.recommendation.api.model.RelationPosition;
import de.tudarmstadt.ukp.inception.recommendation.api.model.RelationSuggestion;
import de.tudarmstadt.ukp.inception.recommendation.api.model.SuggestionDocumentGroup;
import de.tudarmstadt.ukp.inception.recommendation.api.model.SuggestionGroup;

/**
 * Render spans.
 */
public class RecommendationRelationRenderer
    implements RecommendationTypeRenderer
{
    private final RelationAdapter typeAdapter;

    public RecommendationRelationRenderer(RelationAdapter aTypeAdapter)
    {
        typeAdapter = aTypeAdapter;
    }

    /**
     * Add annotations from the CAS, which is controlled by the window size, to the VDocument
     * {@link VDocument}
     *
     * @param aCas
     *            The CAS object containing annotations
     * @param vdoc
     *            A VDocument containing annotations for the given layer
     * @param aState
     *            Data model for brat annotations
     */
    @Override
    public void render(CAS aCas, VDocument vdoc, AnnotatorState aState, AnnotationLayer aLayer,
            RecommendationService aRecommendationService,
            LearningRecordService learningRecordService, AnnotationSchemaService aAnnotationService,
            FeatureSupportRegistry aFsRegistry, DocumentService aDocumentService,
            int aWindowBeginOffset, int aWindowEndOffset)
    {
        if (aCas == null || aRecommendationService == null) {
            return;
        }

        Predictions predictions = aRecommendationService.getPredictions(aState.getUser(),
                aState.getProject());

        // No recommendations available at all
        if (predictions == null) {
            return;
        }

        // TODO #176 use the document Id once it it available in the CAS
        String sourceDocumentName = CasMetadataUtils.getSourceDocumentName(aCas)
                .orElseGet(() -> getDocumentTitle(aCas));
        SuggestionDocumentGroup<RelationSuggestion> groupedPredictions = predictions
                .getGroupedPredictions(RelationSuggestion.class, sourceDocumentName, aLayer,
                        aWindowBeginOffset, aWindowEndOffset);

        // No recommendations to render for this layer
        if (groupedPredictions.isEmpty()) {
            return;
        }

        aRecommendationService.calculateRelationSuggestionVisibility(aCas,
                aState.getUser().getUsername(), aLayer, groupedPredictions, aWindowBeginOffset,
                aWindowEndOffset);

        Preferences pref = aRecommendationService.getPreferences(aState.getUser(),
                aLayer.getProject());

        String bratTypeName = typeAdapter.getEncodedTypeName();

        Type type = CasUtil.getType(aCas, aLayer.getName());
        Type attachType = CasUtil.getType(aCas, aLayer.getAttachType().getName());

        for (SuggestionGroup<RelationSuggestion> group : groupedPredictions) {
            // TODO: Sort by confidence
            for (RelationSuggestion suggestion : group) {

                // Skip rendering AnnotationObjects that should not be rendered
                if (!pref.isShowAllPredictions() && !suggestion.isVisible()) {
                    continue;
                }

                RelationPosition position = suggestion.getPosition();
                int sourceBegin = position.getSourceBegin();
                int sourceEnd = position.getSourceEnd();
                int targetBegin = position.getTargetBegin();
                int targetEnd = position.getTargetEnd();

                // FIXME: We get the first match for the (begin, end) span. With stacking, there can
                // be more than one and we need to get the right one then which does not need to be
                // the first.
                AnnotationFS source = selectAt(aCas, attachType, sourceBegin, sourceEnd).stream()
                        .findFirst().orElse(null);

                AnnotationFS target = selectAt(aCas, attachType, targetBegin, targetEnd).stream()
                        .findFirst().orElse(null);

                VArc arc = new VArc(aLayer, suggestion.getVID(), bratTypeName, new VID(source),
                        new VID(target), suggestion.getUiLabel(), Collections.emptyMap(), COLOR);

                vdoc.add(arc);
            }
        }
    }
}

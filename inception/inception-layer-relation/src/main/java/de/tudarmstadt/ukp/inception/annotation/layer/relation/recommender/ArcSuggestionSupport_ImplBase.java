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
package de.tudarmstadt.ukp.inception.annotation.layer.relation.recommender;

import static de.tudarmstadt.ukp.inception.recommendation.api.model.AnnotationSuggestion.FLAG_OVERLAP;

import java.lang.invoke.MethodHandles;
import java.util.Collection;

import org.apache.commons.collections4.MultiValuedMap;
import org.apache.commons.collections4.multimap.ArrayListValuedHashMap;
import org.apache.uima.cas.CAS;
import org.apache.uima.cas.Type;
import org.apache.uima.cas.text.AnnotationFS;
import org.apache.uima.fit.util.CasUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.lang.Nullable;

import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationFeature;
import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationLayer;
import de.tudarmstadt.ukp.clarin.webanno.model.SourceDocument;
import de.tudarmstadt.ukp.inception.recommendation.api.LearningRecordService;
import de.tudarmstadt.ukp.inception.recommendation.api.RecommendationService;
import de.tudarmstadt.ukp.inception.recommendation.api.SuggestionSupport_ImplBase;
import de.tudarmstadt.ukp.inception.recommendation.api.model.AnnotationSuggestion;
import de.tudarmstadt.ukp.inception.recommendation.api.model.ArcSuggestion_ImplBase;
import de.tudarmstadt.ukp.inception.recommendation.api.model.LearningRecord;
import de.tudarmstadt.ukp.inception.recommendation.api.model.LearningRecordChangeLocation;
import de.tudarmstadt.ukp.inception.recommendation.api.model.LearningRecordUserAction;
import de.tudarmstadt.ukp.inception.recommendation.api.model.Position;
import de.tudarmstadt.ukp.inception.recommendation.api.model.RelationPosition;
import de.tudarmstadt.ukp.inception.recommendation.api.model.SuggestionGroup;
import de.tudarmstadt.ukp.inception.schema.api.AnnotationSchemaService;
import de.tudarmstadt.ukp.inception.schema.api.adapter.TypeAdapter;

public abstract class ArcSuggestionSupport_ImplBase
    extends SuggestionSupport_ImplBase
{
    private static final Logger LOG = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    public ArcSuggestionSupport_ImplBase(RecommendationService aRecommendationService,
            LearningRecordService aLearningRecordService,
            ApplicationEventPublisher aApplicationEventPublisher,
            AnnotationSchemaService aSchemaService)
    {
        super(aRecommendationService, aLearningRecordService, aApplicationEventPublisher,
                aSchemaService);
    }

    @Nullable
    protected Type getAnnotationType(CAS aCas, AnnotationLayer aLayer)
    {
        // NOTE: In order to avoid having to upgrade the "original CAS" in computePredictions,this
        // method is implemented in such a way that it gracefully handles cases where the CAS and
        // the project type system are not in sync - specifically the CAS where the project defines
        // layers or features which do not exist in the CAS.

        try {
            return CasUtil.getAnnotationType(aCas, aLayer.getName());
        }
        catch (IllegalArgumentException e) {
            // Type does not exist in the type system of the CAS. Probably it has not been upgraded
            // to the latest version of the type system yet. If this is the case, we'll just skip.
            return null;
        }
    }

    public abstract String getType();

    @Override
    public <T extends AnnotationSuggestion> void calculateSuggestionVisibility(String aSessionOwner,
            SourceDocument aDocument, CAS aCas, String aUser, AnnotationLayer aLayer,
            Collection<SuggestionGroup<T>> aRecommendations, int aWindowBegin, int aWindowEnd)
    {
        var type = getAnnotationType(aCas, aLayer);

        if (type == null) {
            // The type does not exist in the type system of the CAS. Probably it has not
            // been upgraded to the latest version of the type system yet. If this is the case,
            // we'll just skip.
            return;
        }

        var adapter = schemaService.getAdapter(aLayer);

        // Group annotations by relation position, that is (source, target) address
        var groupedAnnotations = groupAnnotationsInWindow(aCas, adapter, aWindowBegin, aWindowEnd);

        // Collect all suggestions of the given layer
        var groupedSuggestions = aRecommendations.stream()
                .filter(group -> group.getLayerId() == aLayer.getId()) //
                .map(group -> (SuggestionGroup<ArcSuggestion_ImplBase<?>>) group) //
                .toList();

        // Get previously rejected suggestions
        var groupedRecordedAnnotations = new ArrayListValuedHashMap<Position, LearningRecord>();
        for (var learningRecord : learningRecordService.listLearningRecords(aSessionOwner, aUser,
                aLayer)) {
            var relationPosition = new RelationPosition(learningRecord.getOffsetSourceBegin(),
                    learningRecord.getOffsetSourceEnd(), learningRecord.getOffsetTargetBegin(),
                    learningRecord.getOffsetTargetEnd());

            groupedRecordedAnnotations.put(relationPosition, learningRecord);
        }

        for (var feature : schemaService.listSupportedFeatures(aLayer)) {
            var feat = type.getFeatureByBaseName(feature.getName());

            if (feat == null) {
                // The feature does not exist in the type system of the CAS. Probably it has not
                // been upgraded to the latest version of the type system yet. If this is the case,
                // we'll just skip.
                return;
            }

            for (var group : groupedSuggestions) {
                if (!feature.getName().equals(group.getFeature())) {
                    continue;
                }

                group.showAll(AnnotationSuggestion.FLAG_ALL);

                var position = group.getPosition();

                // FIXME: Looks like we need to implement not hiding relations if stacking is
                // enabled.

                // If any annotation at this position has a non-null label for this feature,
                // then we hide the suggestion group
                for (var annotationFS : groupedAnnotations.get(position)) {
                    var label = annotationFS.getFeatureValueAsString(feat);
                    if (annotationFS.getFeatureValueAsString(feat) != null) {
                        for (var suggestion : group) {
                            if (suggestion.isCorrection() && !suggestion.labelEquals(label)) {
                                continue;
                            }

                            suggestion.hide(FLAG_OVERLAP);
                        }
                    }
                }

                // Hide previously rejected suggestions
                for (var learningRecord : groupedRecordedAnnotations.get(position)) {
                    for (var suggestion : group) {
                        if (suggestion.labelEquals(learningRecord.getAnnotation())) {
                            suggestion.hideSuggestion(learningRecord.getUserAction());
                        }
                    }
                }
            }
        }
    }

    protected abstract MultiValuedMap<Position, AnnotationFS> groupAnnotationsInWindow(CAS aCas,
            TypeAdapter aAdapter, int aWindowBegin, int aWindowEnd);

    @Override
    public LearningRecord toLearningRecord(SourceDocument aDocument, String aDataOwner,
            AnnotationSuggestion aSuggestion, AnnotationFeature aFeature,
            LearningRecordUserAction aUserAction, LearningRecordChangeLocation aLocation)
    {
        var pos = ((ArcSuggestion_ImplBase) aSuggestion).getPosition();
        var record = new LearningRecord();
        record.setUser(aDataOwner);
        record.setSourceDocument(aDocument);
        record.setUserAction(aUserAction);
        record.setOffsetBegin(pos.getSourceBegin());
        record.setOffsetEnd(pos.getSourceEnd());
        record.setOffsetBegin2(pos.getTargetBegin());
        record.setOffsetEnd2(pos.getTargetEnd());
        record.setTokenText("");
        record.setAnnotation(aSuggestion.getLabel());
        record.setLayer(aFeature.getLayer());
        record.setSuggestionType(getType());
        record.setChangeLocation(aLocation);
        record.setAnnotationFeature(aFeature);
        return record;
    }
}

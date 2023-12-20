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
package de.tudarmstadt.ukp.inception.recommendation.service;

import static de.tudarmstadt.ukp.inception.recommendation.api.model.AnnotationSuggestion.FLAG_SKIPPED;
import static de.tudarmstadt.ukp.inception.recommendation.api.model.AnnotationSuggestion.FLAG_TRANSIENT_REJECTED;
import static de.tudarmstadt.ukp.inception.support.WebAnnoConst.FEAT_REL_SOURCE;
import static de.tudarmstadt.ukp.inception.support.WebAnnoConst.FEAT_REL_TARGET;
import static org.apache.uima.fit.util.CasUtil.selectAt;

import java.lang.invoke.MethodHandles;
import java.util.ArrayList;

import org.apache.uima.cas.CAS;
import org.apache.uima.cas.text.AnnotationFS;
import org.apache.uima.fit.util.CasUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEventPublisher;

import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationFeature;
import de.tudarmstadt.ukp.clarin.webanno.model.SourceDocument;
import de.tudarmstadt.ukp.inception.annotation.layer.relation.RelationAdapter;
import de.tudarmstadt.ukp.inception.recommendation.api.LayerRecommendationSupport_ImplBase;
import de.tudarmstadt.ukp.inception.recommendation.api.LearningRecordService;
import de.tudarmstadt.ukp.inception.recommendation.api.RecommendationService;
import de.tudarmstadt.ukp.inception.recommendation.api.model.LearningRecordChangeLocation;
import de.tudarmstadt.ukp.inception.recommendation.api.model.LearningRecordUserAction;
import de.tudarmstadt.ukp.inception.recommendation.api.model.RelationSuggestion;
import de.tudarmstadt.ukp.inception.schema.api.adapter.AnnotationException;

public class RelationRecommendationSupportImpl
    extends LayerRecommendationSupport_ImplBase<RelationAdapter, RelationSuggestion>
{
    private static final Logger LOG = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    public RelationRecommendationSupportImpl(RecommendationService aRecommendationService,
            LearningRecordService aLearningRecordService,
            ApplicationEventPublisher aApplicationEventPublisher)
    {
        super(aRecommendationService, aLearningRecordService, aApplicationEventPublisher);
    }

    /**
     * Uses the given annotation suggestion to create a new annotation or to update a feature in an
     * existing annotation.
     * 
     * @param aDocument
     *            the source document to which the annotations belong
     * @param aDataOwner
     *            the annotator user to whom the annotations belong
     * @param aCas
     *            the CAS containing the annotations
     * @param aAdapter
     *            an adapter for the layer to upsert
     * @param aFeature
     *            the feature on the layer that should be upserted
     * @param aSuggestion
     *            the suggestion
     * @param aLocation
     *            the location from where the change was triggered
     * @param aAction
     *            TODO
     * @return the created/updated annotation.
     * @throws AnnotationException
     *             if there was an annotation-level problem
     */
    @Override
    public AnnotationFS acceptSuggestion(String aSessionOwner, SourceDocument aDocument,
            String aDataOwner, CAS aCas, RelationAdapter aAdapter, AnnotationFeature aFeature,
            RelationSuggestion aSuggestion, LearningRecordChangeLocation aLocation,
            LearningRecordUserAction aAction)
        throws AnnotationException
    {
        var sourceBegin = aSuggestion.getPosition().getSourceBegin();
        var sourceEnd = aSuggestion.getPosition().getSourceEnd();
        var targetBegin = aSuggestion.getPosition().getTargetBegin();
        var targetEnd = aSuggestion.getPosition().getTargetEnd();

        // Check if there is already a relation for the given source and target
        var type = CasUtil.getType(aCas, aAdapter.getAnnotationTypeName());
        var attachType = CasUtil.getType(aCas, aAdapter.getAttachTypeName());

        var sourceFeature = type.getFeatureByBaseName(FEAT_REL_SOURCE);
        var targetFeature = type.getFeatureByBaseName(FEAT_REL_TARGET);

        // The begin and end feature of a relation in the CAS are of the dependent/target
        // annotation. See also RelationAdapter::createRelationAnnotation.
        // We use that fact to search for existing relations for this relation suggestion
        var candidates = new ArrayList<AnnotationFS>();
        for (AnnotationFS relationCandidate : selectAt(aCas, type, targetBegin, targetEnd)) {
            AnnotationFS source = (AnnotationFS) relationCandidate.getFeatureValue(sourceFeature);
            AnnotationFS target = (AnnotationFS) relationCandidate.getFeatureValue(targetFeature);

            if (source == null || target == null) {
                continue;
            }

            if (source.getBegin() == sourceBegin && source.getEnd() == sourceEnd
                    && target.getBegin() == targetBegin && target.getEnd() == targetEnd) {
                candidates.add(relationCandidate);
            }
        }

        AnnotationFS annotation = null;
        if (candidates.size() == 1) {
            // One candidate, we just return it
            annotation = candidates.get(0);
        }
        else if (candidates.size() == 2) {
            LOG.warn("Found multiple candidates for upserting relation from suggestion");
            annotation = candidates.get(0);
        }

        // We did not find a relation for this suggestion, so we create a new one
        if (annotation == null) {
            // FIXME: We get the first match for the (begin, end) span. With stacking, there can
            // be more than one and we need to get the right one then which does not need to be
            // the first. We wait for #2135 to fix this. When stacking is enabled, then also
            // consider creating a new relation instead of upserting an existing one.

            var source = selectAt(aCas, attachType, sourceBegin, sourceEnd).stream().findFirst()
                    .orElse(null);
            var target = selectAt(aCas, attachType, targetBegin, targetEnd).stream().findFirst()
                    .orElse(null);

            if (source == null || target == null) {
                String msg = "Cannot find source or target annotation for upserting relation";
                LOG.error(msg);
                throw new IllegalStateException(msg);
            }

            annotation = aAdapter.add(aDocument, aDataOwner, source, target, aCas);
        }

        commmitAcceptedLabel(aSessionOwner, aDocument, aDataOwner, aCas, aAdapter, aFeature,
                aSuggestion, aSuggestion.getLabel(), annotation, aLocation, aAction);

        return annotation;
    }

    @Override
    public void rejectSuggestion(String aSessionOwner, SourceDocument aDocument, String aDataOwner,
            RelationSuggestion aSuggestion, LearningRecordChangeLocation aAction)
    {
        // Hide the suggestion. This is faster than having to recalculate the visibility status
        // for
        // the entire document or even for the part visible on screen.
        aSuggestion.hide(FLAG_TRANSIENT_REJECTED);

        // TODO: See span recommendation support...

    }

    @Override
    public void skipSuggestion(String aSessionOwner, SourceDocument aDocument, String aDataOwner,
            RelationSuggestion aSuggestion, LearningRecordChangeLocation aAction)
        throws AnnotationException
    {
        // Hide the suggestion. This is faster than having to recalculate the visibility status
        // for
        // the entire document or even for the part visible on screen.
        aSuggestion.hide(FLAG_SKIPPED);

        // TODO: Log rejection
        // TODO: Publish rejection event
    }
}

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
package de.tudarmstadt.ukp.inception.ui.core.docanno.sidebar;

import org.apache.commons.lang3.NotImplementedException;
import org.apache.uima.cas.AnnotationBaseFS;
import org.apache.uima.cas.CAS;
import org.apache.uima.jcas.tcas.Annotation;
import org.springframework.context.ApplicationEventPublisher;

import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationFeature;
import de.tudarmstadt.ukp.clarin.webanno.model.SourceDocument;
import de.tudarmstadt.ukp.inception.recommendation.api.LayerRecommendationSupport_ImplBase;
import de.tudarmstadt.ukp.inception.recommendation.api.LearningRecordService;
import de.tudarmstadt.ukp.inception.recommendation.api.RecommendationService;
import de.tudarmstadt.ukp.inception.recommendation.api.model.LearningRecordChangeLocation;
import de.tudarmstadt.ukp.inception.recommendation.api.model.LearningRecordUserAction;
import de.tudarmstadt.ukp.inception.recommendation.api.model.MetadataSuggestion;
import de.tudarmstadt.ukp.inception.schema.api.adapter.AnnotationException;
import de.tudarmstadt.ukp.inception.ui.core.docanno.layer.DocumentMetadataLayerAdapter;
import de.tudarmstadt.ukp.inception.ui.core.docanno.layer.DocumentMetadataLayerTraits;

public class DocumentMetadataRecommendationSupport
    extends LayerRecommendationSupport_ImplBase<DocumentMetadataLayerAdapter, MetadataSuggestion>

{
    public DocumentMetadataRecommendationSupport(RecommendationService aRecommendationService,
            LearningRecordService aLearningRecordService,
            ApplicationEventPublisher aApplicationEventPublisher)
    {
        super(aRecommendationService, aLearningRecordService, aApplicationEventPublisher);
    }

    @Override
    public AnnotationBaseFS acceptSuggestion(String aSessionOwner, SourceDocument aDocument,
            String aDataOwner, CAS aCas, DocumentMetadataLayerAdapter aAdapter,
            AnnotationFeature aFeature, MetadataSuggestion aSuggestion,
            LearningRecordChangeLocation aLocation, LearningRecordUserAction aAction)
        throws AnnotationException
    {
        var aValue = aSuggestion.getLabel();

        var candidates = aCas.<Annotation> select(aAdapter.getAnnotationTypeName()) //
                .asList();

        var candidateWithEmptyLabel = candidates.stream() //
                .filter(c -> aAdapter.getFeatureValue(aFeature, c) == null) //
                .findFirst();

        AnnotationBaseFS annotation;
        if (candidateWithEmptyLabel.isPresent()) {
            // If there is an annotation where the predicted feature is unset, use it ...
            annotation = candidateWithEmptyLabel.get();
        }
        else if (candidates.isEmpty() || !aAdapter.getTraits(DocumentMetadataLayerTraits.class)
                .map(DocumentMetadataLayerTraits::isSingleton).orElse(false)) {
            // ... if not or if stacking is allowed, then we create a new annotation - this also
            // takes care of attaching to an annotation if necessary
            var newAnnotation = aAdapter.add(aDocument, aDataOwner, aCas);
            annotation = newAnnotation;
        }
        else {
            // ... if yes and stacking is not allowed, then we update the feature on the existing
            // annotation
            annotation = candidates.get(0);
        }

        commmitAcceptedLabel(aSessionOwner, aDocument, aDataOwner, aCas, aAdapter, aFeature,
                aSuggestion, aValue, annotation, aLocation, LearningRecordUserAction.ACCEPTED);

        return annotation;
    }

    @Override
    public void rejectSuggestion(String aSessionOwner, SourceDocument aDocument, String aDataOwner,
            MetadataSuggestion aSuggestion, LearningRecordChangeLocation aAction)
        throws AnnotationException
    {
        throw new NotImplementedException("Not yet implemented");
    }

    @Override
    public void skipSuggestion(String aSessionOwner, SourceDocument aDocument, String aDataOwner,
            MetadataSuggestion aSuggestion, LearningRecordChangeLocation aAction)
        throws AnnotationException
    {
        throw new NotImplementedException("Not yet implemented");
    }
}

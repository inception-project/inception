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
package de.tudarmstadt.ukp.inception.annotation.feature.link.recommender;

import java.util.Map;

import org.apache.uima.cas.CAS;
import org.apache.uima.cas.Type;
import org.apache.uima.cas.text.AnnotationFS;
import org.apache.uima.fit.util.CasUtil;

import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationFeature;
import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationLayer;
import de.tudarmstadt.ukp.inception.annotation.layer.relation.recommender.ArcSuggestionRenderer_ImplBase;
import de.tudarmstadt.ukp.inception.recommendation.api.RecommendationService;
import de.tudarmstadt.ukp.inception.recommendation.api.model.LinkSuggestion;
import de.tudarmstadt.ukp.inception.rendering.vmodel.VArc;
import de.tudarmstadt.ukp.inception.rendering.vmodel.VID;
import de.tudarmstadt.ukp.inception.schema.api.AnnotationSchemaService;
import de.tudarmstadt.ukp.inception.schema.api.feature.FeatureSupportRegistry;

public class LinkSuggestionRenderer
    extends ArcSuggestionRenderer_ImplBase<LinkSuggestion>
{
    public LinkSuggestionRenderer(RecommendationService aRecommendationService,
            AnnotationSchemaService aAnnotationService, FeatureSupportRegistry aFsRegistry)
    {
        super(aRecommendationService, aAnnotationService, aFsRegistry);
    }

    @Override
    protected VArc renderArc(AnnotationLayer aLayer, LinkSuggestion suggestion, AnnotationFS source,
            AnnotationFS target, Map<String, String> featureAnnotation)
    {
        return new VArc(aLayer, suggestion.getVID(), VID.of(source), VID.of(target),
                "\uD83E\uDD16 " + suggestion.getUiLabel(), featureAnnotation, COLOR);
    }

    @Override
    protected Type getSourceType(CAS aCas, AnnotationLayer aLayer, AnnotationFeature aFeature)
    {
        return CasUtil.getType(aCas, aLayer.getName());
    }

    @Override
    protected Type getTargetType(CAS aCas, AnnotationLayer aLayer, AnnotationFeature aFeature)
    {
        return CasUtil.getType(aCas, aFeature.getType());
    }
}

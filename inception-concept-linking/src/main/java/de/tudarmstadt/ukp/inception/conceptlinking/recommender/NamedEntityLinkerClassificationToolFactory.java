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

package de.tudarmstadt.ukp.inception.conceptlinking.recommender;

import static de.tudarmstadt.ukp.clarin.webanno.api.WebAnnoConst.SPAN_TYPE;
import static de.tudarmstadt.ukp.clarin.webanno.model.AnchoringMode.SINGLE_TOKEN;
import static de.tudarmstadt.ukp.clarin.webanno.model.AnchoringMode.TOKENS;
import static java.util.Arrays.asList;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import de.tudarmstadt.ukp.clarin.webanno.api.AnnotationSchemaService;
import de.tudarmstadt.ukp.clarin.webanno.api.DocumentService;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.feature.FeatureSupportRegistry;
import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationFeature;
import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationLayer;
import de.tudarmstadt.ukp.inception.conceptlinking.service.ConceptLinkingService;
import de.tudarmstadt.ukp.inception.kb.KnowledgeBaseService;
import de.tudarmstadt.ukp.inception.recommendation.api.ClassificationTool;
import de.tudarmstadt.ukp.inception.recommendation.api.ClassificationToolFactory;

@Component
public class NamedEntityLinkerClassificationToolFactory
    implements ClassificationToolFactory<Object, Void>
{
    public static final String PREFIX = "kb:";

    private @Autowired KnowledgeBaseService kbService;
    private @Autowired ConceptLinkingService clService;
    private @Autowired DocumentService docService;
    private @Autowired AnnotationSchemaService annoService;
    private @Autowired FeatureSupportRegistry fsRegistry;

    // This is a string literal so we can rename/refactor the class without it changing its ID
    // and without the database starting to refer to non-existing recommendation tools.
    public static final String ID = "de.tudarmstadt.ukp.inception.conceptlinking.recommender"
        + ".NamedEntityLinkerClassificationTool";

    @Override
    public String getId()
    {
        return ID;
    }

    @Override
    public String getName()
    {
        return "Named Entity Linker";
    }

    @Override
    public ClassificationTool<Object> createTool(long aRecommenderId, String aFeature,
        AnnotationLayer aLayer, int aMaxPredictions)
    {
        return new NamedEntityLinkerClassificationTool(aRecommenderId, aFeature, aLayer, kbService,
            clService, docService, annoService, fsRegistry);
    }

    @Override
    public boolean accepts(AnnotationLayer aLayer, AnnotationFeature aFeature)
    {
        if (aLayer == null || aFeature == null) {
            return false;
        }
        
        return asList(SINGLE_TOKEN, TOKENS).contains(aLayer.getAnchoringMode())
            && !aLayer.isCrossSentence() && SPAN_TYPE.equals(aLayer.getType())
            && aFeature.getType().startsWith(PREFIX);
    }
}

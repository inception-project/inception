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

package de.tudarmstadt.ukp.inception.conceptlinking.recommender;

import static de.tudarmstadt.ukp.clarin.webanno.model.AnchoringMode.SINGLE_TOKEN;
import static de.tudarmstadt.ukp.clarin.webanno.model.AnchoringMode.TOKENS;
import static de.tudarmstadt.ukp.clarin.webanno.support.WebAnnoConst.SPAN_TYPE;
import static java.util.Arrays.asList;

import org.apache.wicket.model.IModel;
import org.springframework.beans.factory.annotation.Autowired;

import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationFeature;
import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationLayer;
import de.tudarmstadt.ukp.inception.conceptlinking.config.EntityLinkingServiceAutoConfiguration;
import de.tudarmstadt.ukp.inception.conceptlinking.service.ConceptLinkingService;
import de.tudarmstadt.ukp.inception.kb.ConceptFeatureTraits;
import de.tudarmstadt.ukp.inception.kb.KnowledgeBaseService;
import de.tudarmstadt.ukp.inception.recommendation.api.model.Recommender;
import de.tudarmstadt.ukp.inception.recommendation.api.recommender.RecommendationEngine;
import de.tudarmstadt.ukp.inception.recommendation.api.recommender.RecommendationEngineFactoryImplBase;
import de.tudarmstadt.ukp.inception.schema.feature.FeatureSupportRegistry;

/**
 * <p>
 * This class is exposed as a Spring Component via
 * {@link EntityLinkingServiceAutoConfiguration#namedEntityLinkerFactory}.
 * </p>
 */
public class NamedEntityLinkerFactory
    extends RecommendationEngineFactoryImplBase<NamedEntityLinkerTraits>
{
    // This is a string literal so we can rename/refactor the class without it changing its ID
    // and without the database starting to refer to non-existing recommendation tools.
    public static final String ID = "de.tudarmstadt.ukp.inception.conceptlinking.recommender"
            + ".NamedEntityLinkerClassificationTool";

    private static final String PREFIX = "kb:";

    private final KnowledgeBaseService kbService;
    private final ConceptLinkingService clService;
    private final FeatureSupportRegistry fsRegistry;

    @Autowired
    public NamedEntityLinkerFactory(KnowledgeBaseService aKbService,
            ConceptLinkingService aClService, FeatureSupportRegistry aFsRegistry)
    {
        kbService = aKbService;
        clService = aClService;
        fsRegistry = aFsRegistry;
    }

    @Override
    public String getId()
    {
        return ID;
    }

    @Override
    public RecommendationEngine build(Recommender aRecommender)
    {
        NamedEntityLinkerTraits linkerTraits = readTraits(aRecommender);
        ConceptFeatureTraits featureTraits = fsRegistry.readTraits(aRecommender.getFeature(),
                ConceptFeatureTraits::new);

        return new NamedEntityLinker(aRecommender, linkerTraits, kbService, clService, fsRegistry,
                featureTraits);
    }

    @Override
    public String getName()
    {
        return "Named Entity Linker";
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

    @Override
    public NamedEntityLinkerTraitsEditor createTraitsEditor(String aId, IModel<Recommender> aModel)
    {
        return new NamedEntityLinkerTraitsEditor(aId, aModel);
    }

    @Override
    public NamedEntityLinkerTraits createTraits()
    {
        return new NamedEntityLinkerTraits();
    }

    @Override
    public boolean isEvaluable()
    {
        return false;
    }

    @Override
    public boolean isMultipleRecommendationProvider()
    {
        return true;
    }
}

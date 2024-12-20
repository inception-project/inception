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
package de.tudarmstadt.ukp.inception.project.initializers.basic;

import static de.tudarmstadt.ukp.inception.project.initializers.basic.BasicRelationLayerInitializer.BASIC_RELATION_LABEL_FEATURE_NAME;
import static de.tudarmstadt.ukp.inception.project.initializers.basic.BasicRelationLayerInitializer.BASIC_RELATION_LAYER_NAME;
import static de.tudarmstadt.ukp.inception.project.initializers.basic.BasicSpanLayerInitializer.BASIC_SPAN_LABEL_FEATURE_NAME;
import static java.util.Arrays.asList;

import java.io.IOException;
import java.util.List;

import de.tudarmstadt.ukp.clarin.webanno.model.Project;
import de.tudarmstadt.ukp.inception.project.api.ProjectInitializer;
import de.tudarmstadt.ukp.inception.project.initializers.basic.config.InceptionBasicProjectInitializersAutoConfiguration;
import de.tudarmstadt.ukp.inception.recommendation.api.RecommendationService;
import de.tudarmstadt.ukp.inception.recommendation.api.model.Recommender;
import de.tudarmstadt.ukp.inception.recommendation.imls.stringmatch.relation.StringMatchingRelationRecommenderFactory;
import de.tudarmstadt.ukp.inception.recommendation.imls.stringmatch.relation.StringMatchingRelationRecommenderTraits;
import de.tudarmstadt.ukp.inception.schema.api.AnnotationSchemaService;

/**
 * <p>
 * This class is exposed as a Spring Component via
 * {@link InceptionBasicProjectInitializersAutoConfiguration#basicRelationRecommenderInitializer}.
 * </p>
 */
public class BasicRelationRecommenderInitializer
    implements ProjectInitializer
{
    private final AnnotationSchemaService schemaService;
    private final RecommendationService recommendationService;
    private final StringMatchingRelationRecommenderFactory recommenderFactory;

    public BasicRelationRecommenderInitializer(RecommendationService aRecommenderService,
            AnnotationSchemaService aAnnotationService,
            StringMatchingRelationRecommenderFactory aRecommenderFactory)
    {
        recommendationService = aRecommenderService;
        schemaService = aAnnotationService;
        recommenderFactory = aRecommenderFactory;
    }

    @Override
    public String getName()
    {
        return "Generic relation recommender";
    }

    @Override
    public boolean applyByDefault()
    {
        return false;
    }

    @Override
    public boolean alreadyApplied(Project aProject)
    {
        return recommendationService.existsRecommender(aProject, getName());
    }

    @Override
    public List<Class<? extends ProjectInitializer>> getDependencies()
    {
        return asList(BasicSpanLayerInitializer.class);
    }

    @Override
    public void configure(Project aProject) throws IOException
    {
        var relationLayer = schemaService.findLayer(aProject, BASIC_RELATION_LAYER_NAME);
        var labelFeature = schemaService.getFeature(BASIC_RELATION_LABEL_FEATURE_NAME,
                relationLayer);

        var recommender = new Recommender(getName(), relationLayer);
        recommender.setFeature(labelFeature);
        recommender.setMaxRecommendations(3);
        recommender.setThreshold(0.0d);
        recommender.setTool(recommenderFactory.getId());

        var factory = recommendationService
                .<StringMatchingRelationRecommenderTraits> getRecommenderFactory(recommender).get();
        var traits = factory.readTraits(recommender);
        traits.setAdjunctFeature(BASIC_SPAN_LABEL_FEATURE_NAME);
        factory.writeTraits(recommender, traits);

        recommendationService.createOrUpdateRecommender(recommender);
    }
}

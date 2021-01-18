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
package de.tudarmstadt.ukp.inception.initializers;

import static de.tudarmstadt.ukp.inception.initializers.BasicSpanLayerInitializer.BASIC_SPAN_LABEL_FEATURE_NAME;
import static de.tudarmstadt.ukp.inception.initializers.BasicSpanLayerInitializer.BASIC_SPAN_LAYER_NAME;
import static java.util.Arrays.asList;

import java.io.IOException;
import java.util.List;

import org.springframework.stereotype.Component;

import de.tudarmstadt.ukp.clarin.webanno.api.AnnotationSchemaService;
import de.tudarmstadt.ukp.clarin.webanno.api.project.ProjectInitializer;
import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationFeature;
import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationLayer;
import de.tudarmstadt.ukp.clarin.webanno.model.Project;
import de.tudarmstadt.ukp.inception.recommendation.api.RecommendationService;
import de.tudarmstadt.ukp.inception.recommendation.api.model.Recommender;
import de.tudarmstadt.ukp.inception.recommendation.imls.stringmatch.StringMatchingRecommenderFactory;

@Component
public class BasicSpanRecommenderInitializer
    implements ProjectInitializer
{
    private final AnnotationSchemaService annotationService;
    private final RecommendationService recommendationService;
    private final StringMatchingRecommenderFactory recommenderFactory;

    public BasicSpanRecommenderInitializer(RecommendationService aRecommenderService,
            AnnotationSchemaService aAnnotationService,
            StringMatchingRecommenderFactory aRecommenderFactory)
    {
        recommendationService = aRecommenderService;
        annotationService = aAnnotationService;
        recommenderFactory = aRecommenderFactory;
    }

    @Override
    public String getName()
    {
        return "Basic span recommender";
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
        AnnotationLayer spanLayer = annotationService.findLayer(aProject, BASIC_SPAN_LAYER_NAME);
        AnnotationFeature labelFeature = annotationService.getFeature(BASIC_SPAN_LABEL_FEATURE_NAME,
                spanLayer);

        Recommender recommender = new Recommender(getName(), spanLayer);
        recommender.setFeature(labelFeature);
        recommender.setMaxRecommendations(3);
        recommender.setThreshold(0.0d);
        recommender.setTool(recommenderFactory.getId());

        recommendationService.createOrUpdateRecommender(recommender);
    }
}

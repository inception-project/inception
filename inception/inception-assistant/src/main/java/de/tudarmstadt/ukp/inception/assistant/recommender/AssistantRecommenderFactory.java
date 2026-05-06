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
package de.tudarmstadt.ukp.inception.assistant.recommender;

import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationFeature;
import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationLayer;
import de.tudarmstadt.ukp.inception.recommendation.api.model.Recommender;
import de.tudarmstadt.ukp.inception.recommendation.api.recommender.RecommendationEngine;
import de.tudarmstadt.ukp.inception.recommendation.api.recommender.RecommendationEngineFactoryImplBase;

/**
 * Factory for creating the AI Assistant recommender. This is a special "interactive" recommender
 * that doesn't actually run autonomously - instead, suggestions are created by the assistant tools
 * on demand.
 */
public class AssistantRecommenderFactory
    extends RecommendationEngineFactoryImplBase<Void>
{
    // This is a string literal so we can rename/refactor the class without it changing its ID
    // and without the database starting to refer to non-existing recommendation tools.
    public static final String ID = "de.tudarmstadt.ukp.inception.assistant.AssistantRecommender";

    @Override
    public String getId()
    {
        return ID;
    }

    @Override
    public String getName()
    {
        return "AI Assistant";
    }

    @Override
    public boolean isInteractive(Recommender aRecommender)
    {
        // Mark as interactive to prevent automatic execution
        // Suggestions are created only when assistant tools are explicitly invoked
        return true;
    }

    @Override
    public boolean isEvaluable()
    {
        // Assistant recommendations are not evaluated for quality/accuracy
        return false;
    }

    @Override
    public RecommendationEngine build(Recommender aRecommender)
    {
        // Return a no-op engine since suggestions are created by tools, not by engine execution
        return new AssistantRecommendationEngine(aRecommender);
    }

    @Override
    public boolean accepts(AnnotationLayer aLayer)
    {
        // Accept all layers - assistant can work with any annotation type
        return aLayer != null;
    }

    @Override
    public boolean accepts(AnnotationFeature aFeature)
    {
        // Accept all features - assistant can annotate any feature
        return aFeature != null;
    }
}

package de.tudarmstadt.ukp.inception.recommendation.api.foo;

import de.tudarmstadt.ukp.inception.recommendation.api.model.Recommender;

public interface RecommondationEngineFactory {
    RecommendationEngine build(Recommender aRecommender);
    String getEvaluationUnit();
    String getName();
}

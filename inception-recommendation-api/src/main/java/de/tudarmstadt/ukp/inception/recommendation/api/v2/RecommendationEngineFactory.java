package de.tudarmstadt.ukp.inception.recommendation.api.v2;

import de.tudarmstadt.ukp.inception.recommendation.api.model.Recommender;

public interface RecommendationEngineFactory {
    RecommendationEngine build(Recommender aRecommender);
    String getEvaluationUnit();
    String getName();
}

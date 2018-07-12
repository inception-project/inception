package de.tudarmstadt.ukp.inception.recommendation.api.v2;

import java.util.Collection;

import org.apache.uima.cas.CAS;

public interface RecommendationEngine {
    void train(RecommenderContext aContext, Collection<CAS> aCasses);
    void predict(RecommenderContext aContext, CAS aCas);
    double evaluate(RecommenderContext aContext, Collection<CAS> aCas,
                    EvaluationStrategy aEvaluationStrategy);
}

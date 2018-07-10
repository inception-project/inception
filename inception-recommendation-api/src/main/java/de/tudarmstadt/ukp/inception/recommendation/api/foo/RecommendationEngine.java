package de.tudarmstadt.ukp.inception.recommendation.api.foo;

import org.apache.uima.jcas.JCas;

import java.util.Collection;

public interface RecommendationEngine {
    void train(Collection<JCas> aCasses);
    void predict(JCas aCas);
    double evaluate(Collection<JCas> aCasses, EvaluationStrategy aEvaluationStrategy);
}

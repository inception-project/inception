package de.tudarmstadt.ukp.inception.recommendation.api.foo;

import org.apache.uima.jcas.JCas;

import java.util.Collection;

public class OpenNLPRecommendationEngine implements RecommendationEngine {

    @Override
    public void train(Collection<JCas> aCasses) {

    }

    @Override
    public void predict(JCas aCas) {

    }

    @Override
    public double evaluate(Collection<JCas> aCasses, EvaluationStrategy aEvaluationStrategy) {
        return 0;
    }
}

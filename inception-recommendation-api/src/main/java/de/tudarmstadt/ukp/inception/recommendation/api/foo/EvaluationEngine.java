package de.tudarmstadt.ukp.inception.recommendation.api.foo;

import org.apache.uima.jcas.JCas;

public interface EvaluationEngine {
    void addData(JCas aPredictedCas, JCas aGoldCas);
    double evaluate();
}

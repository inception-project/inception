package de.tudarmstadt.ukp.inception.recommendation.api.v2;

import org.apache.uima.jcas.JCas;

public interface EvaluationEngine {
    void addData(JCas aPredictedCas, JCas aGoldCas);
    double evaluate();
}

package de.tudarmstadt.ukp.inception.recommendation.api.foo;

import org.apache.uima.jcas.JCas;

import java.util.List;

public class SequenceEvaluationEngine implements EvaluationEngine {

    @Override
    public void addData(JCas aPredictedCas, JCas aGoldCas) {

    }

    @Override
    public double evaluate() {
        return 0;
    }
}

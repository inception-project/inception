package de.tudarmstadt.ukp.inception.recommendation.api.foo;

import de.tudarmstadt.ukp.inception.recommendation.api.model.Recommender;
import org.apache.uima.jcas.JCas;

import java.util.Collection;
import java.util.List;

public abstract class SequenceRecommondationEngine implements RecommendationEngine {

    public SequenceRecommondationEngine(Recommender aRecommender) {
        // Set parameters based on recommender
    }

    @Override
    public void train(Collection<JCas> aCasses) {
        // Do feature extraction
        List<Sequence> sequences = null;
        trainOnSequence(sequences);
    }

    public abstract void trainOnSequence(List<Sequence> aSequences);

    @Override
    public void predict(JCas aCas) {
        // Do feature extraction
        List<Sequence> sequences = null;

        Sequence sequence = predictSequence(sequences.get(0));

        // Mutate JCas in place
    }

    public abstract Sequence predictSequence(Sequence aSequence);

    @Override
    public boolean supportsMetric(EvaluationEngine aEvaluationEngine) {
        return false;
    }
}

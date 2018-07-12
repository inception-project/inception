package de.tudarmstadt.ukp.inception.recommendation.api.v2;

public interface EvaluationStrategy {
    void setTotal(int aTotal);
    void add(Class<?> aClass);
    boolean belongsToTrainingSet(Class<?> aClass);
}

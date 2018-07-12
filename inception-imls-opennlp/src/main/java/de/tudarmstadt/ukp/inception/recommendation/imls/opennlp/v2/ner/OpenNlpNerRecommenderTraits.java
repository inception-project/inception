package de.tudarmstadt.ukp.inception.recommendation.imls.opennlp.v2.ner;

import opennlp.tools.util.TrainingParameters;

public class OpenNlpNerRecommenderTraits {

    private final String language;

    public OpenNlpNerRecommenderTraits(String language) {
        this.language = language;
    }

    public String getLanguage() {
        return language;
    }

    public TrainingParameters getParameters() {
        TrainingParameters parameters = new TrainingParameters();
        return parameters;
    }
}

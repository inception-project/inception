package de.tudarmstadt.ukp.inception.recommendation.imls.opennlp.v2.ner;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.apache.uima.cas.CAS;
import org.apache.uima.cas.Feature;
import org.apache.uima.cas.Type;
import org.apache.uima.cas.text.AnnotationFS;
import org.apache.uima.fit.util.CasUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.tudarmstadt.ukp.dkpro.core.api.segmentation.type.Sentence;
import de.tudarmstadt.ukp.dkpro.core.api.segmentation.type.Token;
import de.tudarmstadt.ukp.inception.recommendation.api.model.Recommender;
import de.tudarmstadt.ukp.inception.recommendation.api.type.PredictedSpan;
import de.tudarmstadt.ukp.inception.recommendation.api.v2.EvaluationStrategy;
import de.tudarmstadt.ukp.inception.recommendation.api.v2.RecommendationEngine;
import de.tudarmstadt.ukp.inception.recommendation.api.v2.RecommenderContext;
import de.tudarmstadt.ukp.inception.recommendation.imls.opennlp.ner.NameSampleStream;
import opennlp.tools.namefind.NameFinderME;
import opennlp.tools.namefind.NameSample;
import opennlp.tools.namefind.TokenNameFinderFactory;
import opennlp.tools.namefind.TokenNameFinderModel;
import opennlp.tools.util.Span;
import opennlp.tools.util.TrainingParameters;

public class OpenNlpNerRecommender
    implements RecommendationEngine
{
    private static final Logger LOG = LoggerFactory.getLogger(OpenNlpNerRecommender.class);
    private static final String KEY_MODEL = "model";

    private final String layerName;
    private final String featureName;
    private final OpenNlpNerRecommenderTraits traits;

    public OpenNlpNerRecommender(Recommender aRecommender, OpenNlpNerRecommenderTraits aTraits) {
        layerName = aRecommender.getLayer().getName();
        featureName = aRecommender.getFeature();
        traits = aTraits;
    }

    @Override
    public void train(RecommenderContext aContext, Collection<CAS> aCasses)
    {
        List<NameSample> nameSamples = new ArrayList<>();
        for (CAS cas : aCasses) {
            Type tokenType = CasUtil.getType(cas, Sentence.class);
            for (AnnotationFS sentence : CasUtil.select(cas, tokenType)) {
                NameSample nameSample = createNameSample(cas, sentence);
                nameSamples.add(nameSample);
            }
        }

        TrainingParameters parameters = traits.getParameters();

        try (NameSampleStream stream = new NameSampleStream(nameSamples)) {
            TokenNameFinderFactory tokenNameFinderFactory = new TokenNameFinderFactory();
            TokenNameFinderModel model = NameFinderME.train(traits.getLanguage(), null, stream,
                parameters, tokenNameFinderFactory);
            aContext.set(KEY_MODEL, model);
        } catch (IOException e) {
            LOG.error("Exception during training the OpenNLP Named Entity Recognizer model.", e);
        }
    }

    @Override
    public void predict(RecommenderContext aContext, CAS aCas)
    {
        TokenNameFinderModel model = aContext.get(KEY_MODEL);
        NameFinderME finder = new NameFinderME(model);

        Type sentenceType = CasUtil.getType(aCas, Sentence.class);
        Type predictionType = CasUtil.getAnnotationType(aCas, PredictedSpan.class);
        Feature confidenceFeature = predictionType.getFeatureByBaseName("score");
        Feature labelFeature = predictionType.getFeatureByBaseName("label");

        for (AnnotationFS sentence : CasUtil.select(aCas, sentenceType)) {
            List<AnnotationFS> tokenAnnotations = extractTokens(aCas, sentence);
            String[] tokens = tokenAnnotations.stream()
                .map(AnnotationFS::getCoveredText)
                .toArray(String[]::new);

            Span[] predictions = finder.find(tokens);

            for (Span prediction : predictions) {
                int begin = tokenAnnotations.get(prediction.getStart()).getBegin();
                int end = tokenAnnotations.get(prediction.getEnd() - 1).getEnd();
                AnnotationFS annotation = aCas.createAnnotation(predictionType, begin, end);
                annotation.setDoubleValue(confidenceFeature, prediction.getProb());
                annotation.setStringValue(labelFeature, prediction.getType());
            }
        }
    }

    @Override
    public double evaluate(RecommenderContext aContext, Collection<CAS> aCasses,
                           EvaluationStrategy aEvaluationStrategy)
    {
        return 0;
    }

    private NameSample createNameSample(CAS aCas, AnnotationFS aSentence) {
        String[] tokens = extractTokens(aCas, aSentence).stream()
            .map(AnnotationFS::getCoveredText)
            .toArray(String[]::new);
        Span[] annotatedSpans = extractAnnotatedSpans(aCas, aSentence);
        return new NameSample(tokens, annotatedSpans, true);
    }

    private List<AnnotationFS> extractTokens(CAS aCas, AnnotationFS aSentence) {
        Type tokenType = CasUtil.getType(aCas, Token.class);
        return CasUtil.selectCovered(tokenType, aSentence);
    }

    private Span[] extractAnnotatedSpans(CAS aCas, AnnotationFS aSentence) {
        Type annotationType = CasUtil.getType(aCas, layerName);
        Feature feature = annotationType.getFeatureByBaseName(featureName);
        List<AnnotationFS> annotations = CasUtil.selectCovered(annotationType, aSentence);
        int numberOfAnnotations = annotations.size();
        Span[] result = new Span[numberOfAnnotations];
        for (int i = 0; i < numberOfAnnotations; i++) {
            AnnotationFS annotation = annotations.get(i);
            // TODO: Check whether OpenNLP spans want token index or char offsets
            int begin = annotation.getBegin();
            int end = annotation.getEnd();
            String label = annotation.getFeatureValueAsString(feature);
            result[i] = new Span(begin, end, label);
        }
        return result;
    }
}

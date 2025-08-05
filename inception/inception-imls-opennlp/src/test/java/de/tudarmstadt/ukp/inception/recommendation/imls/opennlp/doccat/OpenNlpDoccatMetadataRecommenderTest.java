/*
 * Licensed to the Technische Universität Darmstadt under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The Technische Universität Darmstadt 
 * licenses this file to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.
 *  
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package de.tudarmstadt.ukp.inception.recommendation.imls.opennlp.doccat;

import static de.tudarmstadt.ukp.inception.support.test.recommendation.RecommenderTestHelper.getPredictionFSes;
import static de.tudarmstadt.ukp.inception.support.uima.FeatureStructureBuilder.buildFS;
import static java.util.Arrays.asList;
import static org.assertj.core.api.Assertions.assertThat;

import java.lang.invoke.MethodHandles;
import java.util.List;

import org.apache.uima.cas.CAS;
import org.apache.uima.fit.factory.CasFactory;
import org.apache.uima.fit.factory.TypeSystemDescriptionFactory;
import org.apache.uima.resource.ResourceInitializationException;
import org.apache.uima.resource.metadata.FeatureDescription;
import org.apache.uima.resource.metadata.TypeDescription;
import org.apache.uima.resource.metadata.TypeSystemDescription;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationFeature;
import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationLayer;
import de.tudarmstadt.ukp.inception.annotation.layer.document.api.DocumentMetadataLayerSupport;
import de.tudarmstadt.ukp.inception.recommendation.api.RecommenderTypeSystemUtils;
import de.tudarmstadt.ukp.inception.recommendation.api.evaluation.PercentageBasedSplitter;
import de.tudarmstadt.ukp.inception.recommendation.api.model.Recommender;
import de.tudarmstadt.ukp.inception.recommendation.api.recommender.PredictionContext;
import de.tudarmstadt.ukp.inception.recommendation.api.recommender.RecommenderContext;
import de.tudarmstadt.ukp.inception.support.uima.SegmentationUtils;

public class OpenNlpDoccatMetadataRecommenderTest
{
    private static final Logger LOG = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    private RecommenderContext context;
    private Recommender recommender;
    private OpenNlpDoccatRecommenderTraits traits;
    private TypeSystemDescription tsd;
    private AnnotationLayer layer;
    private AnnotationFeature feature;
    private TypeDescription metadataType;
    private FeatureDescription metadataLabelFeature;

    @BeforeEach
    public void setUp() throws Exception
    {
        context = new RecommenderContext();

        tsd = TypeSystemDescriptionFactory.createTypeSystemDescription();

        metadataType = tsd.addType("custom.Metadata", "", CAS.TYPE_NAME_ANNOTATION_BASE);
        metadataLabelFeature = metadataType.addFeature("value", "", CAS.TYPE_NAME_STRING);

        layer = AnnotationLayer.builder() //
                .withId(1l) //
                .withName(metadataType.getName()) //
                .withType(DocumentMetadataLayerSupport.TYPE) //
                .build();
        feature = AnnotationFeature.builder() //
                .withLayer(layer) //
                .withName(metadataLabelFeature.getName()) //
                .build();
        recommender = Recommender.builder() //
                .withId(1l) //
                .withName("recommender") //
                .withLayer(layer) //
                .withFeature(feature) //
                .build();

        traits = new OpenNlpDoccatRecommenderTraits();
        traits.setNumThreads(2);
        traits.setTrainingSetSizeLimit(250);
        traits.setPredictionLimit(250);
    }

    @Test
    public void thatTrainingWorks() throws Exception
    {
        var sut = new OpenNlpDoccatMetadataRecommender(recommender, traits);
        var casList = trainingDocuments();

        sut.train(context, casList);

        assertThat(context.get(OpenNlpDoccatRecommender.KEY_MODEL)).as("Model has been set")
                .isPresent();
    }

    @Test
    public void thatPredictionWorks() throws Exception
    {
        var sut = new OpenNlpDoccatMetadataRecommender(recommender, traits);
        var casList = trainingDocuments();

        sut.train(context, casList);

        var predictionCas = makePredictionCas("I like cars.", feature);
        sut.predict(new PredictionContext(context), predictionCas);

        var predictions = getPredictionFSes(predictionCas, layer.getName());

        assertThat(predictions).as("Predictions have been written to CAS").isNotEmpty();
    }

    @Test
    public void thatEvaluationWorks() throws Exception
    {
        var splitStrategy = new PercentageBasedSplitter(0.8, 10);
        var sut = new OpenNlpDoccatMetadataRecommender(recommender, traits);
        var casList = trainingDocuments();

        var result = sut.evaluate(casList, splitStrategy);

        var fscore = result.computeF1Score();
        var accuracy = result.computeAccuracyScore();
        var precision = result.computePrecisionScore();
        var recall = result.computeRecallScore();

        LOG.info("F1-Score:  {}", fscore);
        LOG.info("Accuracy:  {}", accuracy);
        LOG.info("Precision: {}", precision);
        LOG.info("Recall:    {}", recall);

        assertThat(fscore).isStrictlyBetween(0.0, 1.0);
        assertThat(precision).isStrictlyBetween(0.0, 1.0);
        assertThat(recall).isStrictlyBetween(0.0, 1.0);
        assertThat(accuracy).isStrictlyBetween(0.0, 1.0);
    }

    private List<CAS> trainingDocuments() throws Exception
    {
        return asList( //
                createLabeledCas("I like wine.", "positive"),
                createLabeledCas("I like cats.", "positive"),
                createLabeledCas("I like trees.", "positive"),
                createLabeledCas("I like dogs.", "positive"),
                createLabeledCas("I like sausages.", "positive"),
                createLabeledCas("I hate pain.", "negative"),
                createLabeledCas("I hate eggs.", "negative"),
                createLabeledCas("I hate farts.", "negative"),
                createLabeledCas("I hate jelly.", "negative"),
                createLabeledCas("I hate girkins.", "negative"));
    }

    private CAS makePredictionCas(String aText, AnnotationFeature aFeature)
        throws ResourceInitializationException
    {
        RecommenderTypeSystemUtils.addPredictionFeaturesToTypeSystem(tsd, asList(aFeature));
        var predictionCas = CasFactory.createCas(tsd);
        predictionCas.setDocumentText(aText);
        SegmentationUtils.splitSentences(predictionCas);
        SegmentationUtils.tokenize(predictionCas);
        return predictionCas;
    }

    private CAS createLabeledCas(String aText, String aLabel) throws Exception
    {
        var cas = CasFactory.createCas(tsd);
        cas.setDocumentText(aText);

        buildFS(cas, feature.getLayer().getName()) //
                .withFeature(feature.getName(), aLabel) //
                .buildAndAddToIndexes();

        SegmentationUtils.splitSentences(cas);
        SegmentationUtils.tokenize(cas);

        return cas;
    }
}

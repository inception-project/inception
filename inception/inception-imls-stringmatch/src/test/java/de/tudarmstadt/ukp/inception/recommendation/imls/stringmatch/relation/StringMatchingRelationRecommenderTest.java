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
package de.tudarmstadt.ukp.inception.recommendation.imls.stringmatch.relation;

import static de.tudarmstadt.ukp.clarin.webanno.api.casstorage.CasAccessMode.EXCLUSIVE_WRITE_ACCESS;
import static de.tudarmstadt.ukp.inception.annotation.layer.relation.api.RelationLayerSupport.FEAT_REL_SOURCE;
import static de.tudarmstadt.ukp.inception.annotation.layer.relation.api.RelationLayerSupport.FEAT_REL_TARGET;
import static de.tudarmstadt.ukp.inception.support.test.recommendation.RecommenderTestHelper.getPredictions;
import static java.nio.file.Files.newInputStream;
import static de.tudarmstadt.ukp.inception.recommendation.api.evaluation.DataSplitter.TargetSet.TEST;
import static de.tudarmstadt.ukp.inception.recommendation.api.evaluation.DataSplitter.TargetSet.TRAIN;
import static de.tudarmstadt.ukp.inception.support.test.recommendation.RecommenderTestHelper.getScore;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;

import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.uima.cas.CAS;
import org.apache.uima.cas.Feature;
import org.apache.uima.cas.Type;
import org.apache.uima.cas.text.AnnotationFS;
import org.apache.uima.fit.factory.CasFactory;
import org.apache.uima.fit.util.CasUtil;
import org.apache.uima.util.XmlCasDeserializer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import de.tudarmstadt.ukp.clarin.webanno.api.casstorage.session.CasStorageSession;
import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationFeature;
import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationLayer;
import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationSet;
import de.tudarmstadt.ukp.dkpro.core.api.segmentation.type.Sentence;
import de.tudarmstadt.ukp.inception.recommendation.api.evaluation.DataSplitter;
import de.tudarmstadt.ukp.inception.recommendation.api.model.Recommender;
import de.tudarmstadt.ukp.inception.recommendation.api.recommender.PredictionContext;
import de.tudarmstadt.ukp.inception.recommendation.api.recommender.RecommenderContext;
import de.tudarmstadt.ukp.inception.support.test.recommendation.RecommenderTestHelper;

public class StringMatchingRelationRecommenderTest
{
    private static final String RELATION_LAYER = "webanno.custom.MyRelationLayer";
    private static final String RELATION_BASE = "webanno.custom.MyRelationBase";

    private RecommenderContext context;
    private Recommender recommender;
    private StringMatchingRelationRecommenderTraits traits;
    private CasStorageSession casStorageSession;
    private int casCounter;

    @BeforeEach
    public void setUp()
    {
        casStorageSession = CasStorageSession.open();
        context = new RecommenderContext();
        recommender = buildRecommender();
        traits = new StringMatchingRelationRecommenderTraits();
        traits.setAdjunctFeature("value");
    }

    @AfterEach
    public void tearDown()
    {
        casStorageSession.close();
    }

    @Test
    public void thatTrainingWorks() throws Exception
    {
        StringMatchingRelationRecommender sut = new StringMatchingRelationRecommender(recommender,
                traits);
        CAS cas = loadSimpleCas();
        sut.train(context, Collections.singletonList(cas));

        assertThat(context.get(StringMatchingRelationRecommender.KEY_MODEL))
                .as("Model has been set").isNotNull();
    }

    @Test
    public void thatPredictingWorks() throws Exception
    {
        StringMatchingRelationRecommender sut = new StringMatchingRelationRecommender(recommender,
                traits);
        CAS cas = loadSimpleCas();
        sut.train(context, Collections.singletonList(cas));

        sut.predict(new PredictionContext(context), cas);

        List<AnnotationFS> predictions = getPredictions(cas, RELATION_LAYER);

        assertThat(predictions).as("Predictions have been written to CAS").isNotEmpty();

        // Assert the prediction for Hannover -> located_in -> Lower Saxony
        Type relationType = CasUtil.getType(cas, RELATION_LAYER);
        Type baseType = CasUtil.getType(cas, RELATION_BASE);
        AnnotationFS prediction = CasUtil.selectSingleAt(cas, relationType, 25, 33); // Hannover

        Feature governorFeature = relationType.getFeatureByBaseName(FEAT_REL_SOURCE);
        Feature dependentFeature = relationType.getFeatureByBaseName(FEAT_REL_TARGET);
        Feature predictedFeature = relationType.getFeatureByBaseName("value");
        Feature attachFeature = baseType.getFeatureByBaseName("value");

        AnnotationFS governor = (AnnotationFS) prediction.getFeatureValue(governorFeature);
        AnnotationFS dependent = (AnnotationFS) prediction.getFeatureValue(dependentFeature);

        String relationLabel = prediction.getStringValue(predictedFeature);
        String governorLabel = governor.getStringValue(attachFeature);
        String dependentLabel = dependent.getStringValue(attachFeature);

        assertThat(prediction).isNotNull();
        assertThat(governor.getCoveredText()).isEqualTo("Hannover");
        assertThat(dependent.getCoveredText()).isEqualTo("Lower Saxony");

        assertThat(relationLabel).isEqualTo("located in");
        assertThat(governorLabel).isEqualTo("city");
        assertThat(dependentLabel).isEqualTo("state");
    }

    @Test
    public void thatPredictionScoresAreRelativeFrequenciesPerDistinctLabel() throws Exception
    {
        var sut = new StringMatchingRelationRecommender(recommender, traits);

        var trainingCas = buildCas( //
                rel("Darmstadt", "city", "Hessen", "state", "located in"), //
                rel("Kassel", "city", "Hessen", "state", "located in"), //
                rel("Hannover", "city", "Lower Saxony", "state", "located in"), //
                rel("Wiesbaden", "city", "Hessen", "state", "capital of"));
        sut.train(context, List.of(trainingCas));

        var cas = buildCas(unrelated("Mainz", "city", "Rhineland", "state"));
        sut.predict(new PredictionContext(context), cas);

        assertThat(getPredictions(cas, RELATION_LAYER)) //
                .extracting(fs -> getLabel(fs), fs -> getScore(fs, "value")) //
                .containsExactlyInAnyOrder( //
                        tuple("located in", 0.75), //
                        tuple("capital of", 0.25));
    }

    @Test
    public void thatEvaluationUsesGovernorAndDependentAsKey() throws Exception
    {
        var sut = new StringMatchingRelationRecommender(recommender, traits);

        var cas = buildCas( //
                rel("Darmstadt", "city", "Hessen", "state", "located in"), //
                rel("Kassel", "city", "Hessen", "state", "located in"), //
                rel("Hannover", "city", "Lower Saxony", "state", "located in"), //
                rel("Celle", "city", "Lower Saxony", "state", "located in"), //
                rel("Goethe", "person", "Frankfurt", "city", "born in"), //
                rel("Schiller", "person", "Marbach", "city", "born in"));

        // Alternate between training and test set
        var counter = new AtomicInteger();
        DataSplitter splitter = $ -> counter.getAndIncrement() % 2 == 0 ? TRAIN : TEST;

        var result = sut.evaluate(List.of(cas), splitter);

        assertThat(result.getTrainingSetSize()).isEqualTo(3);
        assertThat(result.getTestSetSize()).isEqualTo(3);
        assertThat(result.computeAccuracyScore()).isEqualTo(1.0);
        assertThat(result.computeF1Score()).isEqualTo(1.0);
    }

    private static String[] rel(String aGovernor, String aGovernorLabel, String aDependent,
            String aDependentLabel, String aRelationLabel)
    {
        return new String[] { aGovernor, aGovernorLabel, aDependent, aDependentLabel,
                aRelationLabel };
    }

    private static String[] unrelated(String aGovernor, String aGovernorLabel, String aDependent,
            String aDependentLabel)
    {
        return rel(aGovernor, aGovernorLabel, aDependent, aDependentLabel, null);
    }

    /**
     * Creates one sentence of the form "governor is in dependent ." per given relation. Relations
     * without a label only get their endpoints.
     */
    private CAS buildCas(String[]... aRelations) throws Exception
    {
        var root = Paths.get("src", "test", "resources", "relation", "simple");
        var cas = CasFactory.createCasFromPath(root.resolve("TypeSystem.xml").toString());
        casStorageSession.add(AnnotationSet.forTest("cas" + casCounter++), EXCLUSIVE_WRITE_ACCESS,
                cas);
        RecommenderTestHelper.addPredictionFeatures(cas, RELATION_LAYER, "value");

        var text = new StringBuilder();
        var offsets = new ArrayList<int[]>();
        for (var r : aRelations) {
            var sentenceBegin = text.length();
            var governorBegin = text.length();
            text.append(r[0]);
            var governorEnd = text.length();
            text.append(" is in ");
            var dependentBegin = text.length();
            text.append(r[2]);
            var dependentEnd = text.length();
            text.append(" .");
            offsets.add(new int[] { sentenceBegin, governorBegin, governorEnd, dependentBegin,
                    dependentEnd, text.length() });
            text.append("\n");
        }
        cas.setDocumentText(text.toString());

        var sentenceType = CasUtil.getType(cas, Sentence.class);
        var baseType = CasUtil.getType(cas, RELATION_BASE);
        var relationType = CasUtil.getType(cas, RELATION_LAYER);
        var attachFeature = baseType.getFeatureByBaseName("value");
        var governorFeature = relationType.getFeatureByBaseName(FEAT_REL_SOURCE);
        var dependentFeature = relationType.getFeatureByBaseName(FEAT_REL_TARGET);
        var labelFeature = relationType.getFeatureByBaseName("value");

        for (int i = 0; i < aRelations.length; i++) {
            var r = aRelations[i];
            var o = offsets.get(i);

            cas.addFsToIndexes(cas.createAnnotation(sentenceType, o[0], o[5]));

            var governor = cas.createAnnotation(baseType, o[1], o[2]);
            governor.setStringValue(attachFeature, r[1]);
            cas.addFsToIndexes(governor);

            var dependent = cas.createAnnotation(baseType, o[3], o[4]);
            dependent.setStringValue(attachFeature, r[3]);
            cas.addFsToIndexes(dependent);

            if (r[4] != null) {
                var relation = cas.createAnnotation(relationType, o[3], o[4]);
                relation.setFeatureValue(governorFeature, governor);
                relation.setFeatureValue(dependentFeature, dependent);
                relation.setStringValue(labelFeature, r[4]);
                cas.addFsToIndexes(relation);
            }
        }

        return cas;
    }

    private static String getLabel(AnnotationFS aRelation)
    {
        return aRelation.getStringValue(aRelation.getType().getFeatureByBaseName("value"));
    }

    private static Recommender buildRecommender()
    {
        var baseLayer = new AnnotationLayer();
        baseLayer.setName(RELATION_BASE);

        var relationLayer = new AnnotationLayer();
        relationLayer.setName(RELATION_LAYER);

        var feature = new AnnotationFeature();
        feature.setName("value");

        relationLayer.setAttachType(baseLayer);
        relationLayer.setAttachFeature(feature);

        var recommender = new Recommender();
        recommender.setLayer(relationLayer);
        recommender.setFeature(feature);
        recommender.setMaxRecommendations(3);

        return recommender;
    }

    private CAS loadSimpleCas() throws Exception
    {
        var root = Paths.get("src", "test", "resources", "relation", "simple");

        var cas = CasFactory.createCasFromPath(root.resolve("TypeSystem.xml").toString());

        try (var is = newInputStream(root.resolve("relation_test.xmi"))) {
            XmlCasDeserializer.deserialize(is, cas);
            casStorageSession.add(AnnotationSet.forTest("testDataCas"), EXCLUSIVE_WRITE_ACCESS,
                    cas);
            RecommenderTestHelper.addPredictionFeatures(cas, RELATION_LAYER, "value");

            return cas;
        }
    }
}

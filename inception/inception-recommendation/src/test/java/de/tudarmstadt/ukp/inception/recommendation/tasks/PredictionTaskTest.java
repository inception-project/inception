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
package de.tudarmstadt.ukp.inception.recommendation.tasks;

import static de.tudarmstadt.ukp.clarin.webanno.api.casstorage.CasAccessMode.EXCLUSIVE_WRITE_ACCESS;
import static de.tudarmstadt.ukp.inception.recommendation.api.RecommendationService.FEATURE_NAME_AUTO_ACCEPT_MODE_SUFFIX;
import static de.tudarmstadt.ukp.inception.recommendation.api.RecommendationService.FEATURE_NAME_CORRECTION_EXPLANATION_SUFFIX;
import static de.tudarmstadt.ukp.inception.recommendation.api.RecommendationService.FEATURE_NAME_CORRECTION_SUFFIX;
import static de.tudarmstadt.ukp.inception.recommendation.api.RecommendationService.FEATURE_NAME_IS_PREDICTION;
import static de.tudarmstadt.ukp.inception.recommendation.api.RecommendationService.FEATURE_NAME_SCORE_EXPLANATION_SUFFIX;
import static de.tudarmstadt.ukp.inception.recommendation.api.RecommendationService.FEATURE_NAME_SCORE_SUFFIX;
import static java.util.Arrays.asList;
import static org.apache.uima.fit.factory.JCasFactory.createText;
import static org.apache.uima.util.TypeSystemUtil.typeSystem2TypeSystemDescription;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doCallRealMethod;
import static org.mockito.Mockito.when;

import java.util.Arrays;

import org.apache.uima.cas.CAS;
import org.apache.uima.cas.Feature;
import org.apache.uima.resource.metadata.TypeSystemDescription;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import de.tudarmstadt.ukp.clarin.webanno.api.casstorage.session.CasStorageSession;
import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationFeature;
import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationLayer;
import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationSet;
import de.tudarmstadt.ukp.clarin.webanno.model.Project;
import de.tudarmstadt.ukp.clarin.webanno.model.SourceDocument;
import de.tudarmstadt.ukp.clarin.webanno.security.model.User;
import de.tudarmstadt.ukp.dkpro.core.api.ner.type.NamedEntity;
import de.tudarmstadt.ukp.inception.annotation.layer.span.api.SpanLayerSupport;
import de.tudarmstadt.ukp.inception.recommendation.api.model.AnnotationSuggestion;
import de.tudarmstadt.ukp.inception.recommendation.api.model.Offset;
import de.tudarmstadt.ukp.inception.recommendation.api.model.Predictions;
import de.tudarmstadt.ukp.inception.recommendation.api.model.Recommender;
import de.tudarmstadt.ukp.inception.recommendation.api.model.SpanSuggestion;
import de.tudarmstadt.ukp.inception.rendering.model.Range;
import de.tudarmstadt.ukp.inception.schema.service.AnnotationSchemaServiceImpl;

@ExtendWith(MockitoExtension.class)
class PredictionTaskTest
{
    private static final String TRIGGER = "test";
    private static final String DATA_OWNER = "user";

    private User sessionOwner;
    private Project project;
    private SourceDocument document;
    private AnnotationLayer layer;
    private AnnotationFeature feature;

    @BeforeEach
    void setup()
    {
        sessionOwner = User.builder() //
                .withUsername("user") //
                .build();
        project = Project.builder().build();
        document = SourceDocument.builder() //
                .withId(1l) //
                .withName("doc1") //
                .withProject(project) //
                .build();
        layer = AnnotationLayer.builder() //
                .withId(1l) //
                .forJCasClass(NamedEntity.class) //
                .withType(SpanLayerSupport.TYPE) //
                .build();
        feature = AnnotationFeature.builder() //
                .withId(1l) //
                .withName(NamedEntity._FeatName_value) //
                .withType(CAS.TYPE_NAME_STRING) //
                .withLayer(layer) //
                .build();
    }

    @Test
    public void monkeyPatchTypeSystem_WithNer_CreatesScoreFeatures() throws Exception
    {
        var schemaService = Mockito.mock(AnnotationSchemaServiceImpl.class);

        var sut = PredictionTask.builder() //
                .withSessionOwner(sessionOwner) //
                .withTrigger(TRIGGER) //
                .withCurrentDocument(document) //
                .withDataOwner(DATA_OWNER) //
                .build();
        sut.setSchemaService(schemaService);

        var jCas = createText("I am text CAS", "de");

        when(schemaService.getFullProjectTypeSystem(project))
                .thenReturn(typeSystem2TypeSystemDescription(jCas.getTypeSystem()));
        when(schemaService.listAnnotationFeature(project)).thenReturn(asList(feature));
        doCallRealMethod().when(schemaService).upgradeCas(any(CAS.class), any(CAS.class),
                any(TypeSystemDescription.class));

        try (var session = CasStorageSession.open()) {
            session.add(AnnotationSet.forTest("jCas"), EXCLUSIVE_WRITE_ACCESS, jCas.getCas());
            sut.cloneAndMonkeyPatchCAS(project, jCas.getCas(), jCas.getCas());
        }

        assertThat(jCas.getTypeSystem().getType(layer.getName())) //
                .extracting(Feature::getShortName) //
                .containsExactlyInAnyOrder( //
                        "sofa", //
                        "begin", //
                        "end", //
                        "value", //
                        feature.getName() + FEATURE_NAME_SCORE_SUFFIX, //
                        feature.getName() + FEATURE_NAME_SCORE_EXPLANATION_SUFFIX, //
                        feature.getName() + FEATURE_NAME_CORRECTION_SUFFIX, //
                        feature.getName() + FEATURE_NAME_CORRECTION_EXPLANATION_SUFFIX, //
                        feature.getName() + FEATURE_NAME_AUTO_ACCEPT_MODE_SUFFIX, //
                        "identifier", //
                        FEATURE_NAME_IS_PREDICTION);
    }

    @Test
    void testReconciliation() throws Exception
    {
        var rec = Recommender.builder() //
                .withId(1l) //
                .withName("rec") //
                .withLayer(layer) //
                .withFeature(feature) //
                .build();

        var existingSuggestions = Arrays.<AnnotationSuggestion> asList( //
                SpanSuggestion.builder() //
                        .withId(0) //
                        .withPosition(0, 10) //
                        .withDocument(document) //
                        .withLabel("aged") //
                        .withRecommender(rec) //
                        .build(),
                SpanSuggestion.builder() //
                        .withId(1) //
                        .withPosition(0, 10) //
                        .withDocument(document) //
                        .withLabel("removed") //
                        .withRecommender(rec) //
                        .build());
        var activePredictions = new Predictions(sessionOwner, sessionOwner.getUsername(), project);
        activePredictions.inheritSuggestions(existingSuggestions);

        var newSuggestions = Arrays.<AnnotationSuggestion> asList( //
                SpanSuggestion.builder() //
                        .withId(2) //
                        .withPosition(0, 10) //
                        .withDocument(document) //
                        .withLabel("aged") //
                        .withRecommender(rec) //
                        .build(),
                SpanSuggestion.builder() //
                        .withId(3) //
                        .withPosition(new Offset(0, 10)) //
                        .withDocument(document) //
                        .withLabel("added") //
                        .withRecommender(rec) //
                        .build());

        var result = PredictionTask.reconcile(activePredictions, document, rec, new Range(0, 10),
                newSuggestions);

        assertThat(result.suggestions()) //
                .extracting( //
                        AnnotationSuggestion::getId, //
                        AnnotationSuggestion::getLabel, //
                        AnnotationSuggestion::getAge) //
                .containsExactlyInAnyOrder( //
                        tuple(0, "aged", 1), tuple(3, "added", 0));
    }
}

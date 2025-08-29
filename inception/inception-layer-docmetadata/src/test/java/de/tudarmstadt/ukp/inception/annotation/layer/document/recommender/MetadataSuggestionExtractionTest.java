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
package de.tudarmstadt.ukp.inception.annotation.layer.document.recommender;

import static de.tudarmstadt.ukp.inception.recommendation.api.RecommendationService.FEATURE_NAME_IS_PREDICTION;
import static de.tudarmstadt.ukp.inception.support.uima.FeatureStructureBuilder.buildFS;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;

import org.apache.uima.cas.CAS;
import org.apache.uima.fit.factory.CasFactory;
import org.apache.uima.fit.factory.TypeSystemDescriptionFactory;
import org.apache.uima.resource.metadata.FeatureDescription;
import org.apache.uima.resource.metadata.TypeDescription;
import org.apache.uima.resource.metadata.TypeSystemDescription;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationFeature;
import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationLayer;
import de.tudarmstadt.ukp.clarin.webanno.model.Project;
import de.tudarmstadt.ukp.clarin.webanno.model.SourceDocument;
import de.tudarmstadt.ukp.inception.annotation.layer.document.api.DocumentMetadataLayerSupport;
import de.tudarmstadt.ukp.inception.recommendation.api.LearningRecordService;
import de.tudarmstadt.ukp.inception.recommendation.api.RecommendationService;
import de.tudarmstadt.ukp.inception.recommendation.api.RecommenderTypeSystemUtils;
import de.tudarmstadt.ukp.inception.recommendation.api.model.MetadataSuggestion;
import de.tudarmstadt.ukp.inception.recommendation.api.model.Recommender;
import de.tudarmstadt.ukp.inception.recommendation.api.recommender.ExtractionContext;
import de.tudarmstadt.ukp.inception.schema.api.AnnotationSchemaService;
import de.tudarmstadt.ukp.inception.support.uima.SegmentationUtils;

@ExtendWith(MockitoExtension.class)
class MetadataSuggestionExtractionTest
{
    private @Mock RecommendationService recommendationService;
    private @Mock LearningRecordService learningRecordService;
    private @Mock ApplicationEventPublisher applicationEventPublisher;
    private @Mock AnnotationSchemaService schemaService;

    private Project project;
    private SourceDocument document;
    private TypeSystemDescription tsd;
    private CAS originalCas;
    private TypeDescription metadataType;
    private FeatureDescription metadataLabelFeature;

    private MetadataSuggestionSupport sut;

    @BeforeEach
    void setup() throws Exception
    {
        project = Project.builder() //
                .withId(1l) //
                .withName("Test") //
                .build();
        document = SourceDocument.builder() //
                .withId(1l) //
                .withProject(project) //
                .withName("Doc") //
                .build();

        tsd = TypeSystemDescriptionFactory.createTypeSystemDescription();

        metadataType = tsd.addType("custom.Metadata", "", CAS.TYPE_NAME_ANNOTATION_BASE);
        metadataLabelFeature = metadataType.addFeature("value", "", CAS.TYPE_NAME_STRING);

        originalCas = CasFactory.createCas(tsd);
        originalCas.setDocumentText("This is a test.");

        SegmentationUtils.splitSentences(originalCas);
        SegmentationUtils.tokenize(originalCas);

        sut = new MetadataSuggestionSupport(recommendationService, learningRecordService,
                applicationEventPublisher, schemaService);
    }

    @Test
    void testDocumentMetadataExtraction() throws Exception
    {
        var layer = AnnotationLayer.builder() //
                .withId(1l) //
                .withName(metadataType.getName()) //
                .withType(DocumentMetadataLayerSupport.TYPE) //
                .build();
        var feature = AnnotationFeature.builder() //
                .withLayer(layer) //
                .withName(metadataLabelFeature.getName()) //
                .build();
        var recommender = Recommender.builder() //
                .withId(1l) //
                .withName("recommender") //
                .withProject(project) //
                .withLayer(layer) //
                .withFeature(feature) //
                .build();
        AnnotationFeature[] aFeatures = { feature };

        var predictionCas = RecommenderTypeSystemUtils.makePredictionCas(originalCas, aFeatures);

        buildFS(predictionCas, feature.getLayer().getName()) //
                .withFeature(feature.getName(), "happy") //
                .withFeature(FEATURE_NAME_IS_PREDICTION, true) //
                .buildAndAddToIndexes();

        var ctx = new ExtractionContext(0, recommender, document, originalCas, predictionCas);
        var suggestions = sut.extractSuggestions(ctx);

        assertThat(suggestions) //
                .filteredOn(a -> a instanceof MetadataSuggestion) //
                .map(a -> (MetadataSuggestion) a) //
                .extracting( //
                        MetadataSuggestion::getRecommenderName, //
                        MetadataSuggestion::getLabel) //
                .containsExactly( //
                        tuple(recommender.getName(), "happy"));
    }
}

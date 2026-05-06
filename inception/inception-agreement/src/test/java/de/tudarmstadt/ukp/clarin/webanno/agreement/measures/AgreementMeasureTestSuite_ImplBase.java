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
package de.tudarmstadt.ukp.clarin.webanno.agreement.measures;

import static de.tudarmstadt.ukp.clarin.webanno.agreement.measures.AgreementTestUtils.HOST_TYPE;
import static de.tudarmstadt.ukp.clarin.webanno.agreement.measures.AgreementTestUtils.LINK_TYPE;
import static de.tudarmstadt.ukp.clarin.webanno.agreement.measures.AgreementTestUtils.MULTI_VALUE_SPAN_TYPE;
import static de.tudarmstadt.ukp.clarin.webanno.agreement.measures.AgreementTestUtils.createMultiLinkWithRoleTestTypeSystem;
import static de.tudarmstadt.ukp.clarin.webanno.agreement.measures.AgreementTestUtils.createMultiValueStringTestTypeSystem;
import static de.tudarmstadt.ukp.clarin.webanno.agreement.measures.AgreementTestUtils.makeLinkFS;
import static de.tudarmstadt.ukp.clarin.webanno.agreement.measures.AgreementTestUtils.makeLinkHostFS;
import static de.tudarmstadt.ukp.clarin.webanno.model.AnchoringMode.SINGLE_TOKEN;
import static de.tudarmstadt.ukp.clarin.webanno.model.LinkMode.NONE;
import static de.tudarmstadt.ukp.clarin.webanno.model.LinkMode.WITH_ROLE;
import static de.tudarmstadt.ukp.clarin.webanno.model.MultiValueMode.ARRAY;
import static de.tudarmstadt.ukp.clarin.webanno.model.OverlapMode.NO_OVERLAP;
import static de.tudarmstadt.ukp.inception.support.uima.AnnotationBuilder.buildAnnotation;
import static java.util.Arrays.asList;
import static org.apache.uima.cas.CAS.TYPE_NAME_STRING;
import static org.apache.uima.fit.factory.CasFactory.createCas;
import static org.apache.uima.fit.factory.CasFactory.createText;
import static org.apache.uima.fit.factory.JCasFactory.createJCas;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.apache.uima.cas.CAS;
import org.apache.uima.fit.factory.JCasFactory;
import org.dkpro.statistics.agreement.IAnnotationStudy;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import de.tudarmstadt.ukp.clarin.webanno.agreement.FullAgreementResult_ImplBase;
import de.tudarmstadt.ukp.clarin.webanno.constraints.ConstraintsService;
import de.tudarmstadt.ukp.clarin.webanno.curation.casdiff.DiffAdapterRegistryImpl;
import de.tudarmstadt.ukp.clarin.webanno.curation.casdiff.DiffSupportRegistryImpl;
import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationFeature;
import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationLayer;
import de.tudarmstadt.ukp.clarin.webanno.model.Project;
import de.tudarmstadt.ukp.clarin.webanno.model.TagSet;
import de.tudarmstadt.ukp.dkpro.core.api.lexmorph.type.pos.POS;
import de.tudarmstadt.ukp.dkpro.core.api.segmentation.type.Lemma;
import de.tudarmstadt.ukp.dkpro.core.api.segmentation.type.Token;
import de.tudarmstadt.ukp.inception.annotation.feature.bool.BooleanFeatureSupport;
import de.tudarmstadt.ukp.inception.annotation.feature.link.LinkFeatureSupport;
import de.tudarmstadt.ukp.inception.annotation.feature.number.NumberFeatureSupport;
import de.tudarmstadt.ukp.inception.annotation.feature.string.StringFeatureSupport;
import de.tudarmstadt.ukp.inception.annotation.layer.behaviors.LayerBehaviorRegistryImpl;
import de.tudarmstadt.ukp.inception.annotation.layer.chain.ChainLayerSupportImpl;
import de.tudarmstadt.ukp.inception.annotation.layer.relation.RelationLayerSupportImpl;
import de.tudarmstadt.ukp.inception.annotation.layer.relation.curation.RelationDiffSupport;
import de.tudarmstadt.ukp.inception.annotation.layer.span.SpanLayerSupportImpl;
import de.tudarmstadt.ukp.inception.annotation.layer.span.api.SpanLayerSupport;
import de.tudarmstadt.ukp.inception.annotation.layer.span.curation.SpanDiffSupport;
import de.tudarmstadt.ukp.inception.schema.api.AnnotationSchemaService;
import de.tudarmstadt.ukp.inception.schema.api.feature.FeatureSupportRegistryImpl;
import de.tudarmstadt.ukp.inception.schema.api.layer.LayerSupportRegistryImpl;

@ExtendWith(MockitoExtension.class)
public class AgreementMeasureTestSuite_ImplBase
{
    protected @Mock ConstraintsService constraintsService;
    protected @Mock AnnotationSchemaService annotationService;

    protected Project project;
    protected List<AnnotationLayer> layers;
    protected List<AnnotationFeature> features;
    protected LayerSupportRegistryImpl layerRegistry;
    protected DiffSupportRegistryImpl diffSupportRegistry;
    protected DiffAdapterRegistryImpl diffAdapterRegistry;

    @BeforeEach
    public void setup()
    {
        project = new Project();
        layers = new ArrayList<>();
        features = new ArrayList<>();

        var featureSupportRegistry = new FeatureSupportRegistryImpl(
                asList(new StringFeatureSupport(), new BooleanFeatureSupport(),
                        new NumberFeatureSupport(), new LinkFeatureSupport(annotationService)));
        featureSupportRegistry.init();

        var layerBehaviorRegistry = new LayerBehaviorRegistryImpl(asList());
        layerBehaviorRegistry.init();

        layerRegistry = new LayerSupportRegistryImpl(asList(
                new SpanLayerSupportImpl(featureSupportRegistry, null, layerBehaviorRegistry,
                        constraintsService),
                new RelationLayerSupportImpl(featureSupportRegistry, null, layerBehaviorRegistry,
                        constraintsService),
                new ChainLayerSupportImpl(featureSupportRegistry, null, layerBehaviorRegistry,
                        constraintsService)));
        layerRegistry.init();

        diffSupportRegistry = new DiffSupportRegistryImpl(asList( //
                new SpanDiffSupport(), //
                new RelationDiffSupport(annotationService)));
        diffSupportRegistry.init();

        diffAdapterRegistry = new DiffAdapterRegistryImpl(annotationService, diffSupportRegistry);

        lenient().when(annotationService.getAdapter(any())).thenAnswer(a -> {
            var layer = a.getArgument(0, AnnotationLayer.class);
            return layerRegistry.getLayerSupport(layer).createAdapter(layer,
                    () -> features.stream().filter(f -> f.getLayer().equals(layer)).toList());
        });
    }

    public <R extends FullAgreementResult_ImplBase<S>, T extends DefaultAgreementTraits, S extends IAnnotationStudy> //
            R multiLinkWithRoleLabelDifferenceTest(AgreementMeasureSupport<T, R, S> aSupport)
                throws Exception
    {
        var layer = AnnotationLayer.builder() //
                .withId(1l) //
                .withName(HOST_TYPE) //
                .withType(SpanLayerSupport.TYPE) //
                .withProject(project) //
                .withAnchoringMode(SINGLE_TOKEN) //
                .withOverlapMode(NO_OVERLAP) //
                .build();
        layers.add(layer);

        var feature = AnnotationFeature.builder() //
                .withId(1l) //
                .withLayer(layer) //
                .withName("links") //
                .withType(Token.class.getName()) //
                .withLinkMode(WITH_ROLE) //
                .withLinkTypeName(LINK_TYPE) //
                .withLinkTypeRoleFeatureName("role") //
                .withLinkTypeTargetFeatureName("target") //
                .withMultiValueMode(ARRAY) //
                .build();
        features.add(feature);

        var traits = aSupport.createTraits();

        var jcasA = createJCas(createMultiLinkWithRoleTestTypeSystem());
        jcasA.setDocumentText("This is a test.");
        makeLinkHostFS(jcasA, 0, 0, makeLinkFS(jcasA, "slot1", 0, 0));

        var jcasB = createJCas(createMultiLinkWithRoleTestTypeSystem());
        jcasB.setDocumentText("This is a test.");
        makeLinkHostFS(jcasB, 0, 0, makeLinkFS(jcasB, "slot2", 0, 0));

        var casByUser = new LinkedHashMap<String, CAS>();
        casByUser.put("user1", jcasA.getCas());
        casByUser.put("user2", jcasB.getCas());

        AgreementMeasure<R> measure = aSupport.createMeasure(feature, traits);

        return measure.getAgreement(casByUser);
    }

    public <R extends FullAgreementResult_ImplBase<S>, T extends DefaultAgreementTraits, S extends IAnnotationStudy> //
            R multiValueStringPartialAgreement(AgreementMeasureSupport<T, R, S> aSupport)
                throws Exception
    {
        var layer = new AnnotationLayer(MULTI_VALUE_SPAN_TYPE, MULTI_VALUE_SPAN_TYPE,
                SpanLayerSupport.TYPE, project, false, SINGLE_TOKEN, NO_OVERLAP);
        layer.setId(1l);
        layers.add(layer);

        var feature = new AnnotationFeature(project, layer, "values", "values",
                CAS.TYPE_NAME_STRING_ARRAY);
        feature.setId(1l);
        feature.setLinkMode(NONE);
        feature.setMode(ARRAY);
        features.add(feature);

        var traits = aSupport.createTraits();

        var user1 = createCas(createMultiValueStringTestTypeSystem());
        user1.setDocumentText("This is a test.");
        buildAnnotation(user1, MULTI_VALUE_SPAN_TYPE) //
                .at(0, 4) //
                .withFeature("values", asList("a")) //
                .buildAndAddToIndexes();

        var user2 = createCas(createMultiValueStringTestTypeSystem());
        user2.setDocumentText("This is a test.");
        buildAnnotation(user2, MULTI_VALUE_SPAN_TYPE) //
                .at(0, 4) //
                .withFeature("values", asList("a", "b")) //
                .buildAndAddToIndexes();

        AgreementMeasure<R> measure = aSupport.createMeasure(feature, traits);

        return measure.getAgreement(Map.of( //
                "user1", user1, //
                "user2", user2));
    }

    public <R extends FullAgreementResult_ImplBase<S>, T extends DefaultAgreementTraits, S extends IAnnotationStudy> //
            R selfOverlappingAgreement(AgreementMeasureSupport<T, R, S> aSupport) throws Exception
    {
        var layer = new AnnotationLayer(MULTI_VALUE_SPAN_TYPE, MULTI_VALUE_SPAN_TYPE,
                SpanLayerSupport.TYPE, project, false, SINGLE_TOKEN, NO_OVERLAP);
        layer.setId(1l);
        layers.add(layer);

        var feature = new AnnotationFeature(project, layer, "values", "values",
                CAS.TYPE_NAME_STRING_ARRAY);
        feature.setId(1l);
        feature.setLinkMode(NONE);
        feature.setMode(ARRAY);
        features.add(feature);

        var traits = aSupport.createTraits();

        var user1 = createCas(createMultiValueStringTestTypeSystem());
        user1.setDocumentText("This is a test.");
        buildAnnotation(user1, MULTI_VALUE_SPAN_TYPE) //
                .at(0, 4) //
                .withFeature("values", asList("a")) //
                .buildAndAddToIndexes();
        buildAnnotation(user1, MULTI_VALUE_SPAN_TYPE) //
                .at(0, 7) //
                .withFeature("values", asList("a")) //
                .buildAndAddToIndexes();
        buildAnnotation(user1, MULTI_VALUE_SPAN_TYPE) //
                .at(0, 7) //
                .withFeature("values", asList("b")) //
                .buildAndAddToIndexes();

        var user2 = createCas(createMultiValueStringTestTypeSystem());
        user2.setDocumentText("This is a test.");
        buildAnnotation(user2, MULTI_VALUE_SPAN_TYPE) //
                .at(0, 4) //
                .withFeature("values", asList("a", "b")) //
                .buildAndAddToIndexes();

        AgreementMeasure<R> measure = aSupport.createMeasure(feature, traits);

        return measure.getAgreement(Map.of( //
                "user1", user1, //
                "user2", user2));
    }

    public <R extends FullAgreementResult_ImplBase<S>, T extends DefaultAgreementTraits, S extends IAnnotationStudy> //
            R twoEmptyCasTest(AgreementMeasureSupport<T, R, S> aSupport) throws Exception
    {
        var layer = new AnnotationLayer(Lemma.class.getName(), Lemma.class.getSimpleName(),
                SpanLayerSupport.TYPE, project, false, SINGLE_TOKEN, NO_OVERLAP);
        layer.setId(1l);
        layers.add(layer);

        var feature = new AnnotationFeature(project, layer, "value", "value",
                Token.class.getName());
        feature.setId(1l);
        features.add(feature);

        var traits = aSupport.createTraits();

        var text = "";

        var user1Cas = JCasFactory.createText(text).getCas();

        var user2Cas = JCasFactory.createText(text).getCas();

        var casByUser = new LinkedHashMap<String, CAS>();
        casByUser.put("user1", user1Cas);
        casByUser.put("user2", user2Cas);

        AgreementMeasure<R> measure = aSupport.createMeasure(feature, traits);

        return measure.getAgreement(casByUser);
    }

    public <R extends FullAgreementResult_ImplBase<S>, T extends DefaultAgreementTraits, S extends IAnnotationStudy> //
            R threeCasesWithAnnotationOnlyInThird(AgreementMeasureSupport<T, R, S> aSupport)
                throws Exception
    {
        var layer = new AnnotationLayer(POS.class.getName(), POS.class.getSimpleName(),
                SpanLayerSupport.TYPE, project, false, SINGLE_TOKEN, NO_OVERLAP);
        layer.setId(1l);
        layers.add(layer);

        var feature = new AnnotationFeature(project, layer, POS._FeatName_PosValue,
                POS._FeatName_PosValue, TYPE_NAME_STRING);
        feature.setId(1l);
        features.add(feature);

        var user1 = createText("test");
        var user2 = createText("test");
        var user3 = createText("test");
        buildAnnotation(user3, POS.class).at(0, 4) //
                .withFeature(POS._FeatName_PosValue, "test") //
                .buildAndAddToIndexes();

        AgreementMeasure<R> measure = aSupport.createMeasure(feature, aSupport.createTraits());

        return measure.getAgreement(Map.of( //
                "user1", user1, //
                "user2", user2, //
                "user3", user3));
    }

    public <R extends FullAgreementResult_ImplBase<S>, T extends DefaultAgreementTraits, S extends IAnnotationStudy> //
            R twoWithoutLabelTest(AgreementMeasureSupport<T, R, S> aSupport, T aTraits)
                throws Exception
    {
        var layer = new AnnotationLayer(POS.class.getName(), POS.class.getSimpleName(),
                SpanLayerSupport.TYPE, project, false, SINGLE_TOKEN, NO_OVERLAP);
        layer.setId(1l);
        layers.add(layer);

        var feature = new AnnotationFeature(project, layer, "PosValue", "PosValue",
                TYPE_NAME_STRING);
        feature.setId(1l);
        features.add(feature);

        var user1 = JCasFactory.createText("test");

        new POS(user1, 0, 1).addToIndexes();
        new POS(user1, 1, 2).addToIndexes();
        POS p1 = new POS(user1, 3, 4);
        p1.setPosValue("A");
        p1.addToIndexes();

        var user2 = JCasFactory.createText("test");

        new POS(user2, 0, 1).addToIndexes();
        new POS(user2, 2, 3).addToIndexes();
        var p2 = new POS(user2, 3, 4);
        p2.setPosValue("B");
        p2.addToIndexes();

        var casByUser = new LinkedHashMap<String, CAS>();
        casByUser.put("user1", user1.getCas());
        casByUser.put("user2", user2.getCas());

        var measure = aSupport.createMeasure(feature, aTraits);

        return measure.getAgreement(casByUser);
    }

    public <R extends FullAgreementResult_ImplBase<S>, T extends DefaultAgreementTraits, S extends IAnnotationStudy> //
            R fullSingleCategoryAgreementWithTagset(AgreementMeasureSupport<T, R, S> aSupport,
                    T aTraits)
                throws Exception
    {
        var tagset = new TagSet(project, "tagset");

        var layer = new AnnotationLayer(POS.class.getName(), POS.class.getSimpleName(),
                SpanLayerSupport.TYPE, project, false, SINGLE_TOKEN, NO_OVERLAP);
        layer.setId(1l);
        layers.add(layer);

        var feature = new AnnotationFeature(project, layer, "PosValue", "PosValue",
                TYPE_NAME_STRING);
        feature.setId(1l);
        feature.setTagset(tagset);
        features.add(feature);

        var user1 = createText("test");

        buildAnnotation(user1, POS.class).at(0, 1) //
                .withFeature(POS._FeatName_PosValue, "+") //
                .buildAndAddToIndexes();

        var user2 = createText("test");

        buildAnnotation(user2, POS.class).at(0, 1) //
                .withFeature(POS._FeatName_PosValue, "+") //
                .buildAndAddToIndexes();

        return aSupport.createMeasure(feature, aTraits).getAgreement(Map.of( //
                "user1", user1, //
                "user2", user2));
    }
}

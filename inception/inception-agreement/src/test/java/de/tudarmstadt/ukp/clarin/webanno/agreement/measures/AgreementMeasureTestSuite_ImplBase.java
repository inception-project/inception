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
import static de.tudarmstadt.ukp.clarin.webanno.curation.casdiff.LinkCompareBehavior.LINK_TARGET_AS_LABEL;
import static de.tudarmstadt.ukp.clarin.webanno.model.AnchoringMode.SINGLE_TOKEN;
import static de.tudarmstadt.ukp.clarin.webanno.model.LinkMode.NONE;
import static de.tudarmstadt.ukp.clarin.webanno.model.LinkMode.WITH_ROLE;
import static de.tudarmstadt.ukp.clarin.webanno.model.MultiValueMode.ARRAY;
import static de.tudarmstadt.ukp.clarin.webanno.model.OverlapMode.NO_OVERLAP;
import static de.tudarmstadt.ukp.clarin.webanno.support.WebAnnoConst.SPAN_TYPE;
import static de.tudarmstadt.ukp.clarin.webanno.support.uima.AnnotationBuilder.buildAnnotation;
import static java.util.Arrays.asList;
import static org.apache.uima.cas.CAS.TYPE_NAME_STRING;
import static org.apache.uima.fit.factory.CasFactory.createCas;
import static org.apache.uima.fit.factory.CasFactory.createText;
import static org.apache.uima.fit.factory.JCasFactory.createJCas;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.apache.uima.cas.CAS;
import org.apache.uima.fit.factory.JCasFactory;
import org.apache.uima.jcas.JCas;
import org.dkpro.statistics.agreement.IAnnotationStudy;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationFeature;
import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationLayer;
import de.tudarmstadt.ukp.clarin.webanno.model.Project;
import de.tudarmstadt.ukp.clarin.webanno.model.Tag;
import de.tudarmstadt.ukp.clarin.webanno.model.TagSet;
import de.tudarmstadt.ukp.clarin.webanno.support.uima.AnnotationBuilder;
import de.tudarmstadt.ukp.dkpro.core.api.lexmorph.type.pos.POS;
import de.tudarmstadt.ukp.dkpro.core.api.segmentation.type.Lemma;
import de.tudarmstadt.ukp.dkpro.core.api.segmentation.type.Token;
import de.tudarmstadt.ukp.inception.annotation.feature.bool.BooleanFeatureSupport;
import de.tudarmstadt.ukp.inception.annotation.feature.link.LinkFeatureSupport;
import de.tudarmstadt.ukp.inception.annotation.feature.number.NumberFeatureSupport;
import de.tudarmstadt.ukp.inception.annotation.feature.string.StringFeatureSupport;
import de.tudarmstadt.ukp.inception.annotation.layer.behaviors.LayerBehaviorRegistryImpl;
import de.tudarmstadt.ukp.inception.annotation.layer.behaviors.LayerSupportRegistryImpl;
import de.tudarmstadt.ukp.inception.annotation.layer.chain.ChainLayerSupport;
import de.tudarmstadt.ukp.inception.annotation.layer.relation.RelationLayerSupport;
import de.tudarmstadt.ukp.inception.annotation.layer.span.SpanLayerSupport;
import de.tudarmstadt.ukp.inception.schema.AnnotationSchemaService;
import de.tudarmstadt.ukp.inception.schema.service.FeatureSupportRegistryImpl;

@ExtendWith(MockitoExtension.class)
public class AgreementMeasureTestSuite_ImplBase
{
    protected @Mock AnnotationSchemaService annotationService;

    protected Project project;
    protected List<AnnotationLayer> layers;
    protected List<AnnotationFeature> features;
    protected LayerSupportRegistryImpl layerRegistry;

    @BeforeEach
    public void setup()
    {
        project = new Project();
        layers = new ArrayList<>();
        features = new ArrayList<>();

        FeatureSupportRegistryImpl featureSupportRegistry = new FeatureSupportRegistryImpl(
                asList(new StringFeatureSupport(), new BooleanFeatureSupport(),
                        new NumberFeatureSupport(), new LinkFeatureSupport(annotationService)));
        featureSupportRegistry.init();

        LayerBehaviorRegistryImpl layerBehaviorRegistry = new LayerBehaviorRegistryImpl(asList());
        layerBehaviorRegistry.init();

        layerRegistry = new LayerSupportRegistryImpl(asList(
                new SpanLayerSupport(featureSupportRegistry, null, layerBehaviorRegistry),
                new RelationLayerSupport(featureSupportRegistry, null, layerBehaviorRegistry),
                new ChainLayerSupport(featureSupportRegistry, null, layerBehaviorRegistry)));
        layerRegistry.init();
    }

    public <R extends Serializable, T extends DefaultAgreementTraits, S extends IAnnotationStudy> //
    R multiLinkWithRoleLabelDifferenceTest(AgreementMeasureSupport<T, R, S> aSupport)
        throws Exception
    {
        AnnotationLayer layer = new AnnotationLayer(HOST_TYPE, HOST_TYPE, SPAN_TYPE, project, false,
                SINGLE_TOKEN, NO_OVERLAP);
        layer.setId(1l);
        layers.add(layer);

        AnnotationFeature feature = new AnnotationFeature(project, layer, "links", "links",
                Token.class.getName());
        feature.setId(1l);
        feature.setLinkMode(WITH_ROLE);
        feature.setLinkTypeName(LINK_TYPE);
        feature.setLinkTypeRoleFeatureName("role");
        feature.setLinkTypeTargetFeatureName("target");
        features.add(feature);

        T traits = aSupport.createTraits();
        traits.setLinkCompareBehavior(LINK_TARGET_AS_LABEL);

        JCas jcasA = createJCas(createMultiLinkWithRoleTestTypeSystem());
        jcasA.setDocumentText("This is a test.");
        makeLinkHostFS(jcasA, 0, 0, makeLinkFS(jcasA, "slot1", 0, 0));

        JCas jcasB = createJCas(createMultiLinkWithRoleTestTypeSystem());
        jcasB.setDocumentText("This is a test.");
        makeLinkHostFS(jcasB, 0, 0, makeLinkFS(jcasB, "slot2", 0, 0));

        Map<String, List<CAS>> casByUser = new LinkedHashMap<>();
        casByUser.put("user1", asList(jcasA.getCas()));
        casByUser.put("user2", asList(jcasB.getCas()));

        AgreementMeasure<R> measure = aSupport.createMeasure(feature, traits);

        return measure.getAgreement(casByUser);
    }

    public <R extends Serializable, T extends DefaultAgreementTraits, S extends IAnnotationStudy> //
    R multiValueStringPartialAgreement(AgreementMeasureSupport<T, R, S> aSupport) throws Exception
    {
        AnnotationLayer layer = new AnnotationLayer(MULTI_VALUE_SPAN_TYPE, MULTI_VALUE_SPAN_TYPE,
                SPAN_TYPE, project, false, SINGLE_TOKEN, NO_OVERLAP);
        layer.setId(1l);
        layers.add(layer);

        AnnotationFeature feature = new AnnotationFeature(project, layer, "values", "values",
                CAS.TYPE_NAME_STRING_ARRAY);
        feature.setId(1l);
        feature.setLinkMode(NONE);
        feature.setMode(ARRAY);
        features.add(feature);

        T traits = aSupport.createTraits();
        traits.setLinkCompareBehavior(LINK_TARGET_AS_LABEL);

        CAS user1 = createCas(createMultiValueStringTestTypeSystem());
        user1.setDocumentText("This is a test.");
        AnnotationBuilder.buildAnnotation(user1, MULTI_VALUE_SPAN_TYPE) //
                .at(0, 4) //
                .withFeature("values", asList("a")) //
                .buildAndAddToIndexes();

        CAS user2 = createCas(createMultiValueStringTestTypeSystem());
        user2.setDocumentText("This is a test.");
        AnnotationBuilder.buildAnnotation(user2, MULTI_VALUE_SPAN_TYPE) //
                .at(0, 4) //
                .withFeature("values", asList("a", "b")) //
                .buildAndAddToIndexes();

        AgreementMeasure<R> measure = aSupport.createMeasure(feature, traits);

        return measure.getAgreement(Map.of( //
                "user1", asList(user1), //
                "user2", asList(user2)));
    }

    public <R extends Serializable, T extends DefaultAgreementTraits, S extends IAnnotationStudy> //
    R twoEmptyCasTest(AgreementMeasureSupport<T, R, S> aSupport) throws Exception
    {
        AnnotationLayer layer = new AnnotationLayer(Lemma.class.getName(),
                Lemma.class.getSimpleName(), SPAN_TYPE, project, false, SINGLE_TOKEN, NO_OVERLAP);
        layer.setId(1l);
        layers.add(layer);

        AnnotationFeature feature = new AnnotationFeature(project, layer, "value", "value",
                Token.class.getName());
        feature.setId(1l);
        features.add(feature);

        T traits = aSupport.createTraits();

        String text = "";

        CAS user1Cas = JCasFactory.createText(text).getCas();

        CAS user2Cas = JCasFactory.createText(text).getCas();

        Map<String, List<CAS>> casByUser = new LinkedHashMap<>();
        casByUser.put("user1", asList(user1Cas));
        casByUser.put("user2", asList(user2Cas));

        AgreementMeasure<R> measure = aSupport.createMeasure(feature, traits);

        return measure.getAgreement(casByUser);
    }

    public <R extends Serializable, T extends DefaultAgreementTraits, S extends IAnnotationStudy> //
    R singleNoDifferencesWithAdditionalCasTest(AgreementMeasureSupport<T, R, S> aSupport)
        throws Exception
    {
        AnnotationLayer layer = new AnnotationLayer(POS.class.getName(), POS.class.getSimpleName(),
                SPAN_TYPE, project, false, SINGLE_TOKEN, NO_OVERLAP);
        layer.setId(1l);
        layers.add(layer);

        AnnotationFeature feature = new AnnotationFeature(project, layer, POS._FeatName_PosValue,
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
                "user1", asList(user1), //
                "user2", asList(user2), //
                "user3", asList(user3)));
    }

    public <R extends Serializable, T extends DefaultAgreementTraits, S extends IAnnotationStudy> //
    R twoWithoutLabelTest(AgreementMeasureSupport<T, R, S> aSupport, T aTraits) throws Exception
    {
        AnnotationLayer layer = new AnnotationLayer(POS.class.getName(), POS.class.getSimpleName(),
                SPAN_TYPE, project, false, SINGLE_TOKEN, NO_OVERLAP);
        layer.setId(1l);
        layers.add(layer);

        AnnotationFeature feature = new AnnotationFeature(project, layer, "PosValue", "PosValue",
                CAS.TYPE_NAME_STRING);
        feature.setId(1l);
        features.add(feature);

        JCas user1 = JCasFactory.createText("test");

        new POS(user1, 0, 1).addToIndexes();
        new POS(user1, 1, 2).addToIndexes();
        POS p1 = new POS(user1, 3, 4);
        p1.setPosValue("A");
        p1.addToIndexes();

        JCas user2 = JCasFactory.createText("test");

        new POS(user2, 0, 1).addToIndexes();
        new POS(user2, 2, 3).addToIndexes();
        POS p2 = new POS(user2, 3, 4);
        p2.setPosValue("B");
        p2.addToIndexes();

        Map<String, List<CAS>> casByUser = new LinkedHashMap<>();
        casByUser.put("user1", asList(user1.getCas()));
        casByUser.put("user2", asList(user2.getCas()));

        AgreementMeasure<R> measure = aSupport.createMeasure(feature, aTraits);

        return measure.getAgreement(casByUser);
    }

    public <R extends Serializable, T extends DefaultAgreementTraits, S extends IAnnotationStudy> //
    R fullSingleCategoryAgreementWithTagset(AgreementMeasureSupport<T, R, S> aSupport, T aTraits)
        throws Exception
    {
        TagSet tagset = new TagSet(project, "tagset");
        Tag tag1 = new Tag(tagset, "+");
        Tag tag2 = new Tag(tagset, "-");
        // when(annotationService.listTags(tagset)).thenReturn(asList(tag1, tag2));

        AnnotationLayer layer = new AnnotationLayer(POS.class.getName(), POS.class.getSimpleName(),
                SPAN_TYPE, project, false, SINGLE_TOKEN, NO_OVERLAP);
        layer.setId(1l);
        layers.add(layer);

        AnnotationFeature feature = new AnnotationFeature(project, layer, "PosValue", "PosValue",
                CAS.TYPE_NAME_STRING);
        feature.setId(1l);
        feature.setTagset(tagset);
        features.add(feature);

        CAS user1 = createText("test");

        buildAnnotation(user1, POS.class).at(0, 1) //
                .withFeature(POS._FeatName_PosValue, "+") //
                .buildAndAddToIndexes();

        CAS user2 = createText("test");

        buildAnnotation(user2, POS.class).at(0, 1) //
                .withFeature(POS._FeatName_PosValue, "+") //
                .buildAndAddToIndexes();

        return aSupport.createMeasure(feature, aTraits).getAgreement(Map.of( //
                "user1", asList(user1), //
                "user2", asList(user2)));
    }

    public <R extends Serializable, T extends DefaultAgreementTraits, S extends IAnnotationStudy> //
    R twoDocumentsNoOverlap(AgreementMeasureSupport<T, R, S> aSupport, T aTraits) throws Exception
    {
        TagSet tagset = new TagSet(project, "tagset");
        Tag tag1 = new Tag(tagset, "+");
        Tag tag2 = new Tag(tagset, "-");
        // when(annotationService.listTags(tagset)).thenReturn(asList(tag1, tag2));

        AnnotationLayer layer = new AnnotationLayer(POS.class.getName(), POS.class.getSimpleName(),
                SPAN_TYPE, project, false, SINGLE_TOKEN, NO_OVERLAP);
        layer.setId(1l);
        layers.add(layer);

        AnnotationFeature feature = new AnnotationFeature(project, layer, "PosValue", "PosValue",
                CAS.TYPE_NAME_STRING);
        feature.setId(1l);
        feature.setTagset(tagset);
        features.add(feature);

        CAS user1a = createText("test");
        CAS user1b = createText("test");

        buildAnnotation(user1a, POS.class).at(0, 1) //
                .withFeature(POS._FeatName_PosValue, "+") //
                .buildAndAddToIndexes();

        CAS user2a = createText("test");
        CAS user2b = createText("test");

        buildAnnotation(user2b, POS.class).at(0, 1) //
                .withFeature(POS._FeatName_PosValue, "+") //
                .buildAndAddToIndexes();

        return aSupport.createMeasure(feature, aTraits).getAgreement(Map.of( //
                "user1", asList(user1a, user1b), //
                "user2", asList(user2a, user2b)));
    }
}

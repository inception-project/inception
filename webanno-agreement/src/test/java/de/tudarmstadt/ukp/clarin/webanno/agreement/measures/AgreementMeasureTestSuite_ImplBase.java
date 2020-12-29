/*
 * Copyright 2019
 * Ubiquitous Knowledge Processing (UKP) Lab and FG Language Technology
 * Technische Universität Darmstadt
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
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
import static de.tudarmstadt.ukp.clarin.webanno.agreement.measures.AgreementTestUtils.createMultiLinkWithRoleTestTypeSystem;
import static de.tudarmstadt.ukp.clarin.webanno.agreement.measures.AgreementTestUtils.makeLinkFS;
import static de.tudarmstadt.ukp.clarin.webanno.agreement.measures.AgreementTestUtils.makeLinkHostFS;
import static de.tudarmstadt.ukp.clarin.webanno.api.WebAnnoConst.SPAN_TYPE;
import static de.tudarmstadt.ukp.clarin.webanno.curation.casdiff.LinkCompareBehavior.LINK_TARGET_AS_LABEL;
import static de.tudarmstadt.ukp.clarin.webanno.model.AnchoringMode.SINGLE_TOKEN;
import static de.tudarmstadt.ukp.clarin.webanno.model.LinkMode.WITH_ROLE;
import static de.tudarmstadt.ukp.clarin.webanno.model.OverlapMode.NO_OVERLAP;
import static java.util.Arrays.asList;
import static org.apache.uima.fit.factory.JCasFactory.createJCas;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.apache.uima.cas.CAS;
import org.apache.uima.fit.factory.JCasFactory;
import org.apache.uima.jcas.JCas;
import org.dkpro.statistics.agreement.coding.ICodingAnnotationStudy;
import org.junit.Before;
import org.mockito.Mock;

import de.tudarmstadt.ukp.clarin.webanno.api.AnnotationSchemaService;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.feature.BooleanFeatureSupport;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.feature.FeatureSupportRegistryImpl;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.feature.NumberFeatureSupport;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.feature.SlotFeatureSupport;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.feature.StringFeatureSupport;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.layer.ChainLayerSupport;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.layer.LayerBehaviorRegistryImpl;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.layer.LayerSupportRegistryImpl;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.layer.RelationLayerSupport;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.layer.SpanLayerSupport;
import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationFeature;
import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationLayer;
import de.tudarmstadt.ukp.clarin.webanno.model.Project;
import de.tudarmstadt.ukp.dkpro.core.api.lexmorph.type.pos.POS;
import de.tudarmstadt.ukp.dkpro.core.api.segmentation.type.Lemma;
import de.tudarmstadt.ukp.dkpro.core.api.segmentation.type.Token;

public class AgreementMeasureTestSuite_ImplBase
{
    protected @Mock AnnotationSchemaService annotationService;

    protected Project project;
    protected List<AnnotationLayer> layers;
    protected List<AnnotationFeature> features;

    @Before
    public void setup()
    {
        initMocks(this);

        project = new Project();
        layers = new ArrayList<>();
        features = new ArrayList<>();

        FeatureSupportRegistryImpl featureSupportRegistry = new FeatureSupportRegistryImpl(
                asList(new StringFeatureSupport(), new BooleanFeatureSupport(),
                        new NumberFeatureSupport(), new SlotFeatureSupport(annotationService)));
        featureSupportRegistry.init();

        LayerBehaviorRegistryImpl layerBehaviorRegistry = new LayerBehaviorRegistryImpl(asList());
        layerBehaviorRegistry.init();

        LayerSupportRegistryImpl layerRegistry = new LayerSupportRegistryImpl(asList(
                new SpanLayerSupport(featureSupportRegistry, null, layerBehaviorRegistry),
                new RelationLayerSupport(featureSupportRegistry, null, layerBehaviorRegistry),
                new ChainLayerSupport(featureSupportRegistry, null, layerBehaviorRegistry)));
        layerRegistry.init();

        when(annotationService.listSupportedLayers(any())).thenReturn(layers);
        when(annotationService.listAnnotationLayer(any())).thenReturn(layers);
        when(annotationService.listSupportedFeatures(any(AnnotationLayer.class)))
                .thenReturn(features);
        when(annotationService.listAnnotationFeature(any(AnnotationLayer.class)))
                .thenReturn(features);
        when(annotationService.getAdapter(any(AnnotationLayer.class))).then(_call -> {
            AnnotationLayer l = _call.getArgument(0);
            return layerRegistry.getLayerSupport(l).createAdapter(l,
                    () -> annotationService.listAnnotationFeature(l));
        });
    }

    public <R extends Serializable, T extends DefaultAgreementTraits> R multiLinkWithRoleLabelDifferenceTest(
            AggreementMeasureSupport<T, R, ICodingAnnotationStudy> aSupport)
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
        makeLinkHostFS(jcasA, 0, 0, makeLinkFS(jcasA, "slot1", 0, 0));

        JCas jcasB = createJCas(createMultiLinkWithRoleTestTypeSystem());
        makeLinkHostFS(jcasB, 0, 0, makeLinkFS(jcasB, "slot2", 0, 0));

        Map<String, List<CAS>> casByUser = new LinkedHashMap<>();
        casByUser.put("user1", asList(jcasA.getCas()));
        casByUser.put("user2", asList(jcasB.getCas()));

        AggreementMeasure<R> measure = aSupport.createMeasure(feature, traits);

        return measure.getAgreement(casByUser);
    }

    public <R extends Serializable, T extends DefaultAgreementTraits> R twoEmptyCasTest(
            AggreementMeasureSupport<T, R, ICodingAnnotationStudy> aSupport)
        throws Exception
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

        CAS user1Cas = JCasFactory.createJCas().getCas();
        user1Cas.setDocumentText(text);

        CAS user2Cas = JCasFactory.createJCas().getCas();
        user2Cas.setDocumentText(text);

        Map<String, List<CAS>> casByUser = new LinkedHashMap<>();
        casByUser.put("user1", asList(user1Cas));
        casByUser.put("user2", asList(user2Cas));

        AggreementMeasure<R> measure = aSupport.createMeasure(feature, traits);

        return measure.getAgreement(casByUser);
    }

    public <R extends Serializable, T extends DefaultAgreementTraits> R singleNoDifferencesWithAdditionalCasTest(
            AggreementMeasureSupport<T, R, ICodingAnnotationStudy> aSupport)
        throws Exception
    {
        AnnotationLayer layer = new AnnotationLayer(POS.class.getName(), POS.class.getSimpleName(),
                SPAN_TYPE, project, false, SINGLE_TOKEN, NO_OVERLAP);
        layer.setId(1l);
        layers.add(layer);

        AnnotationFeature feature = new AnnotationFeature(project, layer, "PosValue", "PosValue",
                CAS.TYPE_NAME_STRING);
        feature.setId(1l);
        features.add(feature);

        T traits = aSupport.createTraits();

        JCas user1 = JCasFactory.createJCas();
        user1.setDocumentText("test");

        JCas user2 = JCasFactory.createJCas();
        user2.setDocumentText("test");

        JCas user3 = JCasFactory.createJCas();
        user3.setDocumentText("test");
        POS pos3 = new POS(user3, 0, 4);
        pos3.setPosValue("test");
        pos3.addToIndexes();

        Map<String, List<CAS>> casByUser = new LinkedHashMap<>();
        casByUser.put("user1", asList(user1.getCas()));
        casByUser.put("user2", asList(user2.getCas()));
        casByUser.put("user3", asList(user3.getCas()));

        AggreementMeasure<R> measure = aSupport.createMeasure(feature, traits);

        return measure.getAgreement(casByUser);
    }

    public <R extends Serializable, T extends DefaultAgreementTraits> R twoWithoutLabelTest(
            AggreementMeasureSupport<T, R, ICodingAnnotationStudy> aSupport, T aTraits)
        throws Exception
    {
        AnnotationLayer layer = new AnnotationLayer(POS.class.getName(), POS.class.getSimpleName(),
                SPAN_TYPE, project, false, SINGLE_TOKEN, NO_OVERLAP);
        layer.setId(1l);
        layers.add(layer);

        AnnotationFeature feature = new AnnotationFeature(project, layer, "PosValue", "PosValue",
                CAS.TYPE_NAME_STRING);
        feature.setId(1l);
        features.add(feature);

        JCas user1 = JCasFactory.createJCas();
        user1.setDocumentText("test");
        new POS(user1, 0, 1).addToIndexes();
        new POS(user1, 1, 2).addToIndexes();
        POS p1 = new POS(user1, 3, 4);
        p1.setPosValue("A");
        p1.addToIndexes();

        JCas user2 = JCasFactory.createJCas();
        user2.setDocumentText("test");
        new POS(user2, 0, 1).addToIndexes();
        new POS(user2, 2, 3).addToIndexes();
        POS p2 = new POS(user2, 3, 4);
        p2.setPosValue("B");
        p2.addToIndexes();

        Map<String, List<CAS>> casByUser = new LinkedHashMap<>();
        casByUser.put("user1", asList(user1.getCas()));
        casByUser.put("user2", asList(user2.getCas()));

        AggreementMeasure<R> measure = aSupport.createMeasure(feature, aTraits);

        return measure.getAgreement(casByUser);
    }
}

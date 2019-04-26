/*
 * Copyright 2015
 * Ubiquitous Knowledge Processing (UKP) Lab and FG Language Technology
 * Technische Universität Darmstadt
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package de.tudarmstadt.ukp.clarin.webanno.ui.curation.util;

import static de.tudarmstadt.ukp.clarin.webanno.api.WebAnnoConst.FEAT_REL_SOURCE;
import static de.tudarmstadt.ukp.clarin.webanno.api.WebAnnoConst.FEAT_REL_TARGET;
import static de.tudarmstadt.ukp.clarin.webanno.api.WebAnnoConst.SPAN_TYPE;
import static de.tudarmstadt.ukp.clarin.webanno.model.AnchoringMode.SINGLE_TOKEN;
import static de.tudarmstadt.ukp.clarin.webanno.model.AnchoringMode.TOKENS;
import static de.tudarmstadt.ukp.clarin.webanno.model.Mode.CURATION;
import static de.tudarmstadt.ukp.clarin.webanno.model.OverlapMode.NO_OVERLAP;
import static de.tudarmstadt.ukp.clarin.webanno.model.OverlapMode.OVERLAP_ONLY;
import static de.tudarmstadt.ukp.clarin.webanno.ui.curation.util.DiffTestUtils.HOST_TYPE;
import static java.util.Arrays.asList;
import static org.apache.uima.fit.factory.JCasFactory.createJCas;
import static org.apache.uima.fit.util.CasUtil.getType;
import static org.apache.uima.fit.util.CasUtil.selectCovered;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.apache.uima.cas.CAS;
import org.apache.uima.cas.Feature;
import org.apache.uima.cas.FeatureStructure;
import org.apache.uima.cas.Type;
import org.apache.uima.cas.text.AnnotationFS;
import org.apache.uima.fit.factory.JCasFactory;
import org.apache.uima.fit.util.CasUtil;
import org.apache.uima.jcas.JCas;
import org.assertj.core.api.Assertions;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

import de.tudarmstadt.ukp.clarin.webanno.api.AnnotationSchemaService;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.exception.AnnotationException;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.feature.FeatureSupportRegistryImpl;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.feature.PrimitiveUimaFeatureSupport;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.feature.SlotFeatureSupport;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.layer.ChainLayerSupport;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.layer.LayerBehaviorRegistryImpl;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.layer.LayerSupportRegistryImpl;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.layer.RelationLayerSupport;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.layer.SpanLayerSupport;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.model.AnnotatorState;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.model.AnnotatorStateImpl;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.util.WebAnnoCasUtil;
import de.tudarmstadt.ukp.clarin.webanno.curation.casdiff.CasDiff2;
import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationFeature;
import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationLayer;
import de.tudarmstadt.ukp.clarin.webanno.model.LinkMode;
import de.tudarmstadt.ukp.clarin.webanno.model.Mode;
import de.tudarmstadt.ukp.clarin.webanno.model.OverlapMode;
import de.tudarmstadt.ukp.clarin.webanno.model.Project;
import de.tudarmstadt.ukp.clarin.webanno.security.model.User;
import de.tudarmstadt.ukp.dkpro.core.api.lexmorph.type.pos.POS;
import de.tudarmstadt.ukp.dkpro.core.api.ner.type.NamedEntity;
import de.tudarmstadt.ukp.dkpro.core.api.segmentation.type.Token;
import de.tudarmstadt.ukp.dkpro.core.api.syntax.type.dependency.Dependency;

public class CopyAnnotationTest
{
    private @Mock AnnotationSchemaService schemaService;
    
    private LayerSupportRegistryImpl layerSupportRegistry;
    private FeatureSupportRegistryImpl featureSupportRegistry;
    private Project project;
    private AnnotationLayer tokenLayer;
    private AnnotationFeature tokenPosFeature;
    private AnnotationLayer posLayer;
    private AnnotationFeature posFeature;
    private AnnotationLayer neLayer;
    private AnnotationFeature neFeature;
    private AnnotationLayer slotLayer;
    private AnnotationFeature slotFeature;
    private AnnotationFeature stringFeature;
    
    @Before
    public void setup()
    {
        initMocks(this);
        
        project = new Project();
        
        tokenLayer = new AnnotationLayer(Token.class.getName(), "Token", SPAN_TYPE, null, true,
                SINGLE_TOKEN, NO_OVERLAP);
        
        tokenPosFeature = new AnnotationFeature();
        tokenPosFeature.setName("pos");
        tokenPosFeature.setEnabled(true);
        tokenPosFeature.setType(POS.class.getName());
        tokenPosFeature.setUiName("pos");
        tokenPosFeature.setLayer(tokenLayer);
        tokenPosFeature.setProject(project);
        tokenPosFeature.setVisible(true);
        
        posLayer = new AnnotationLayer(POS.class.getName(), "POS", SPAN_TYPE, project, true,
                SINGLE_TOKEN, NO_OVERLAP);
        posLayer.setAttachType(tokenLayer);
        posLayer.setAttachFeature(tokenPosFeature);
        
        posFeature = new AnnotationFeature();
        posFeature.setName("PosValue");
        posFeature.setEnabled(true);
        posFeature.setType(CAS.TYPE_NAME_STRING);
        posFeature.setUiName("PosValue");
        posFeature.setLayer(posLayer);
        posFeature.setProject(project);
        posFeature.setVisible(true);
        
        neLayer = new AnnotationLayer(NamedEntity.class.getName(), "Named Entity", SPAN_TYPE,
                project, true, TOKENS, OVERLAP_ONLY);
        
        neFeature = new AnnotationFeature();
        neFeature.setName("value");
        neFeature.setEnabled(true);
        neFeature.setType(CAS.TYPE_NAME_STRING);
        neFeature.setUiName("value");
        neFeature.setLayer(neLayer);
        neFeature.setProject(project);
        neFeature.setVisible(true);
        
        slotLayer = new AnnotationLayer(HOST_TYPE, HOST_TYPE, SPAN_TYPE, project, false,
                SINGLE_TOKEN, NO_OVERLAP);
        slotFeature = new AnnotationFeature();
        slotFeature.setName("links");
        slotFeature.setEnabled(true);
        slotFeature.setType(Token.class.getName());
        slotFeature.setLinkMode(LinkMode.WITH_ROLE);
        slotFeature.setUiName("f1");
        slotFeature.setLayer(slotLayer);
        slotFeature.setProject(project);
        slotFeature.setVisible(true);
        stringFeature = new AnnotationFeature();
        stringFeature.setName("f1");
        stringFeature.setEnabled(true);
        stringFeature.setType(CAS.TYPE_NAME_STRING);
        stringFeature.setUiName("f1");
        stringFeature.setLayer(slotLayer);
        stringFeature.setProject(project);
        stringFeature.setVisible(true);
        
        when(schemaService.listAnnotationFeature(any(AnnotationLayer.class))).thenAnswer(call -> { 
            AnnotationLayer type = call.getArgument(0, AnnotationLayer.class);
            if (type.getName().equals(POS.class.getName())) {
                return asList(posFeature);
            }
            if (type.getName().equals(NamedEntity.class.getName())) {
                return asList(neFeature);
            }
            if (type.getName().equals(DiffTestUtils.HOST_TYPE)) {
                return asList(slotFeature, stringFeature);
            }
            throw new IllegalStateException("Unknown layer type: " + type.getName());
        });

        when(schemaService.getAdapter(any(AnnotationLayer.class))).thenAnswer(call -> { 
            AnnotationLayer type = call.getArgument(0, AnnotationLayer.class);
            return layerSupportRegistry.getLayerSupport(type).createAdapter(type);
        });
        
        featureSupportRegistry = new FeatureSupportRegistryImpl(
                asList(new PrimitiveUimaFeatureSupport(),
                        new SlotFeatureSupport(schemaService)));
        featureSupportRegistry.init();

        LayerBehaviorRegistryImpl layerBehaviorRegistry = new LayerBehaviorRegistryImpl(asList());
        layerBehaviorRegistry.init();

        layerSupportRegistry = new LayerSupportRegistryImpl(asList(
                new SpanLayerSupport(featureSupportRegistry, null, schemaService,
                        layerBehaviorRegistry),
                new RelationLayerSupport(featureSupportRegistry, null, schemaService,
                        layerBehaviorRegistry),
                new ChainLayerSupport(featureSupportRegistry, null, schemaService,
                        layerBehaviorRegistry)));
        layerSupportRegistry.init();
    }
    
    @Test
    public void simpleCopyToEmptyTest()
        throws Exception
    {
        AnnotatorState state = new AnnotatorStateImpl(CURATION);
        state.setUser(new User());
        
        CAS jcas = createJCas().getCas();
        AnnotationFS clickedFs = createNEAnno(jcas, "NN", 0, 0);

        CAS mergeCas = createJCas().getCas();
        createTokenAnno(mergeCas, 0, 0);

        MergeCas.addSpanAnnotation(state, schemaService,
                neLayer, mergeCas, clickedFs, false);

        assertEquals(1, selectCovered(mergeCas, getType(mergeCas, NamedEntity.class), 0, 0).size());
    }

    private AnnotationFS createNEAnno(CAS aCas, String aValue, int aBegin, int aEnd)
    {
        Type type = aCas.getTypeSystem().getType(NamedEntity.class.getTypeName());
        AnnotationFS clickedFs = aCas.createAnnotation(type, aBegin, aEnd);
        Feature value = type.getFeatureByBaseName("value");
        clickedFs.setStringValue(value, aValue);
        aCas.addFsToIndexes(clickedFs);
        return clickedFs;
    }

    private AnnotationFS createPOSAnno(CAS aCas, String aValue, int aBegin, int aEnd)
    {
        Type type = aCas.getTypeSystem().getType(POS.class.getTypeName());
        
        AnnotationFS clickedFs = aCas.createAnnotation(type, aBegin, aEnd);
        Feature posValue = type.getFeatureByBaseName("PosValue");
        clickedFs.setStringValue(posValue, aValue);
        aCas.addFsToIndexes(clickedFs);
        return clickedFs;
    }

    private AnnotationFS createTokenAnno(CAS aCas, int aBegin, int aEnd)
    {
        Type type = aCas.getTypeSystem().getType(Token.class.getTypeName());
        AnnotationFS token = aCas.createAnnotation(type, aBegin, aEnd);
        aCas.addFsToIndexes(token);
        return token;
    }

    @Test
    public void simpleCopyToSameExistingAnnoTest()
        throws Exception
    {
        CAS jcas = createJCas().getCas();
        Type type = jcas.getTypeSystem().getType(POS.class.getTypeName());
        AnnotationFS clickedFs = createPOSAnno(jcas, "NN", 0, 0);

        CAS mergeCas = createJCas().getCas();
        AnnotationFS existingFs = mergeCas.createAnnotation(type, 0, 0);
        Feature posValue = type.getFeatureByBaseName("PosValue");
        existingFs.setStringValue(posValue, "NN");
        mergeCas.addFsToIndexes(existingFs);

        Assertions.assertThatExceptionOfType(AnnotationException.class)
                .isThrownBy(() -> MergeCas.addSpanAnnotation(new AnnotatorStateImpl(Mode.CURATION),
                        schemaService, posLayer, mergeCas, clickedFs, false))
                .withMessageContaining("annotation already exists");
    }

    @Test
    public void simpleCopyToDiffExistingAnnoWithNoStackingTest()
        throws Exception
    {
        CAS jcas = createJCas().getCas();
        Type type = jcas.getTypeSystem().getType(POS.class.getTypeName());
        AnnotationFS clickedFs = createPOSAnno(jcas, "NN", 0, 0);

        CAS mergeCAs = createJCas().getCas();
        AnnotationFS existingFs = mergeCAs.createAnnotation(type, 0, 0);
        Feature posValue = type.getFeatureByBaseName("PosValue");
        existingFs.setStringValue(posValue, "NE");
        mergeCAs.addFsToIndexes(existingFs);

        MergeCas.addSpanAnnotation(new AnnotatorStateImpl(CURATION), schemaService, posLayer,
                mergeCAs, clickedFs, false);

        assertEquals(1, CasUtil.selectCovered(mergeCAs, type, 0, 0).size());
    }

    @Test
    public void simpleCopyToDiffExistingAnnoWithStackingTest()
        throws Exception
    {
        AnnotatorState state = new AnnotatorStateImpl(CURATION);
        state.setUser(new User());
        
        neLayer.setOverlapMode(OverlapMode.ANY_OVERLAP);

        CAS jcas = createJCas().getCas();
        Type type = jcas.getTypeSystem().getType(NamedEntity.class.getTypeName());
        AnnotationFS clickedFs = createNEAnno(jcas, "NN", 0, 0);

        CAS mergeCAs = createJCas().getCas();
        createTokenAnno(mergeCAs, 0, 0);
        AnnotationFS existingFs = mergeCAs.createAnnotation(type, 0, 0);
        Feature posValue = type.getFeatureByBaseName("value");
        existingFs.setStringValue(posValue, "NE");
        mergeCAs.addFsToIndexes(existingFs);

        MergeCas.addSpanAnnotation(state, schemaService, neLayer, mergeCAs, clickedFs, true);

        assertEquals(2, selectCovered(mergeCAs, type, 0, 0).size());
    }

    @Test
    public void copySpanWithSlotNoStackingTest()
        throws Exception
    {
        slotLayer.setOverlapMode(OverlapMode.NO_OVERLAP);
        
        JCas jcasA = createJCas(DiffTestUtils.createMultiLinkWithRoleTestTypeSytem("f1"));
        Type type = jcasA.getTypeSystem().getType(DiffTestUtils.HOST_TYPE);
        Feature feature = type.getFeatureByBaseName("f1");

        AnnotationFS clickedFs = DiffTestUtils.makeLinkHostMultiSPanFeatureFS(jcasA, 0, 0, feature, "A",
                DiffTestUtils.makeLinkFS(jcasA, "slot1", 0, 0));

        JCas mergeCAs = JCasFactory
                .createJCas(DiffTestUtils.createMultiLinkWithRoleTestTypeSytem("f1"));

        DiffTestUtils.makeLinkHostMultiSPanFeatureFS(mergeCAs, 0, 0, feature, "C",
                DiffTestUtils.makeLinkFS(mergeCAs, "slot1", 0, 0));

        MergeCas.addSpanAnnotation(new AnnotatorStateImpl(CURATION), schemaService, slotLayer,
                mergeCAs.getCas(), clickedFs, false);

        assertEquals(1, selectCovered(mergeCAs.getCas(), type, 0, 0).size());
    }

    @Test
    public void copySpanWithSlotWithStackingTest()
        throws Exception
    {
        AnnotatorState state = new AnnotatorStateImpl(CURATION);
        state.setUser(new User());
        
        slotLayer.setAnchoringMode(TOKENS);
        slotLayer.setOverlapMode(OverlapMode.ANY_OVERLAP);
        
        JCas jcasA = createJCas(DiffTestUtils.createMultiLinkWithRoleTestTypeSytem("f1"));
        Type type = jcasA.getTypeSystem().getType(DiffTestUtils.HOST_TYPE);
        Feature feature = type.getFeatureByBaseName("f1");

        AnnotationFS clickedFs = DiffTestUtils.makeLinkHostMultiSPanFeatureFS(jcasA, 0, 0, feature, "A",
                DiffTestUtils.makeLinkFS(jcasA, "slot1", 0, 0));

        JCas mergeCAs = JCasFactory
                .createJCas(DiffTestUtils.createMultiLinkWithRoleTestTypeSytem("f1"));

        DiffTestUtils.makeLinkHostMultiSPanFeatureFS(mergeCAs, 0, 0, feature, "C",
                DiffTestUtils.makeLinkFS(mergeCAs, "slot1", 0, 0));

        MergeCas.addSpanAnnotation(state, schemaService, slotLayer, mergeCAs.getCas(), clickedFs,
                true);

        assertEquals(2, selectCovered(mergeCAs.getCas(), type, 0, 0).size());
    }

    @Test
    public void copyLinkToEmptyTest()
        throws Exception
    {
        JCas mergeCas = createJCas(DiffTestUtils.createMultiLinkWithRoleTestTypeSytem("f1"));
        Type type = mergeCas.getTypeSystem().getType(DiffTestUtils.HOST_TYPE);
        Feature feature = type.getFeatureByBaseName("f1");

        AnnotationFS mergeFs = DiffTestUtils.makeLinkHostMultiSPanFeatureFS(mergeCas, 0, 0, feature,
                "A");

        FeatureStructure copyFS = DiffTestUtils.makeLinkFS(mergeCas, "slot1", 0, 0);

        List<FeatureStructure> linkFs = new ArrayList<>();
        linkFs.add(copyFS);
        WebAnnoCasUtil.setLinkFeatureValue(mergeFs, type.getFeatureByBaseName("links"), linkFs);

        JCas jcasA = createJCas(DiffTestUtils.createMultiLinkWithRoleTestTypeSytem("f1"));
        DiffTestUtils.makeLinkHostMultiSPanFeatureFS(jcasA, 0, 0, feature, "A",
                DiffTestUtils.makeLinkFS(jcasA, "slot1", 0, 0));

        Map<String, List<CAS>> casByUser = new LinkedHashMap<>();
        casByUser.put("user1", asList(mergeCas.getCas()));
        casByUser.put("user2", asList(jcasA.getCas()));

        List<String> entryTypes = asList(DiffTestUtils.HOST_TYPE);

        CasDiff2.SpanDiffAdapter adapter = new CasDiff2.SpanDiffAdapter(DiffTestUtils.HOST_TYPE);
        adapter.addLinkFeature("links", "role", "target");
        List<? extends CasDiff2.DiffAdapter> diffAdapters = asList(adapter);

        CasDiff2.DiffResult diff = CasDiff2.doDiff(entryTypes, diffAdapters,
                CasDiff2.LinkCompareBehavior.LINK_TARGET_AS_LABEL, casByUser);

        assertEquals(0, diff.getDifferingConfigurationSets().size());
        assertEquals(0, diff.getIncompleteConfigurationSets().size());
    }

    @Test
    public void copyLinkToExistingButDiffLinkTest()
        throws Exception
    {

        JCas mergeCas = JCasFactory
                .createJCas(DiffTestUtils.createMultiLinkWithRoleTestTypeSytem("f1"));
        Type type = mergeCas.getTypeSystem().getType(DiffTestUtils.HOST_TYPE);
        Feature feature = type.getFeatureByBaseName("f1");

        AnnotationFS mergeFs = DiffTestUtils.makeLinkHostMultiSPanFeatureFS(mergeCas, 0, 0, feature,
                "A", DiffTestUtils.makeLinkFS(mergeCas, "slot1", 0, 0));

        FeatureStructure copyFS = DiffTestUtils.makeLinkFS(mergeCas, "slot2", 0, 0);

        List<FeatureStructure> linkFs = new ArrayList<>();
        linkFs.add(copyFS);
        WebAnnoCasUtil.setLinkFeatureValue(mergeFs, type.getFeatureByBaseName("links"), linkFs);

        JCas jcasA = createJCas(DiffTestUtils.createMultiLinkWithRoleTestTypeSytem("f1"));
        DiffTestUtils.makeLinkHostMultiSPanFeatureFS(jcasA, 0, 0, feature, "A",
                DiffTestUtils.makeLinkFS(jcasA, "slot1", 0, 0));

        Map<String, List<CAS>> casByUser = new LinkedHashMap<>();
        casByUser.put("user1", asList(mergeCas.getCas()));
        casByUser.put("user2", asList(jcasA.getCas()));

        List<String> entryTypes = asList(DiffTestUtils.HOST_TYPE);

        CasDiff2.SpanDiffAdapter adapter = new CasDiff2.SpanDiffAdapter(DiffTestUtils.HOST_TYPE);
        adapter.addLinkFeature("links", "role", "target");
        List<? extends CasDiff2.DiffAdapter> diffAdapters = asList(adapter);

        CasDiff2.DiffResult diff = CasDiff2.doDiff(entryTypes, diffAdapters,
                CasDiff2.LinkCompareBehavior.LINK_TARGET_AS_LABEL, casByUser);

        assertEquals(0, diff.getDifferingConfigurationSets().size());
        assertEquals(2, diff.getIncompleteConfigurationSets().size());
    }

    @Test
    public void simpleCopyRelationToEmptyAnnoTest()
        throws Exception
    {
        CAS jcas = createJCas().getCas();
        Type type = jcas.getTypeSystem().getType(Dependency.class.getTypeName());

        AnnotationFS originClickedToken = createTokenAnno(jcas, 0, 0);
        AnnotationFS targetClickedToken = createTokenAnno(jcas, 1, 1);

        AnnotationFS originClicked = createPOSAnno(jcas, "NN", 0, 0);
        AnnotationFS targetClicked = createPOSAnno(jcas, "NN", 1, 1);

        jcas.addFsToIndexes(originClicked);
        jcas.addFsToIndexes(targetClicked);

        originClickedToken.setFeatureValue(originClickedToken.getType().getFeatureByBaseName("pos"),
                originClicked);
        targetClickedToken.setFeatureValue(targetClickedToken.getType().getFeatureByBaseName("pos"),
                targetClicked);

        Feature sourceFeature = type.getFeatureByBaseName(FEAT_REL_SOURCE);
        Feature targetFeature = type.getFeatureByBaseName(FEAT_REL_TARGET);

        AnnotationFS clickedFs = jcas.createAnnotation(type, 0, 1);
        clickedFs.setFeatureValue(sourceFeature, originClickedToken);
        clickedFs.setFeatureValue(targetFeature, targetClickedToken);
        jcas.addFsToIndexes(clickedFs);

        CAS mergeCAs = createJCas().getCas();
        AnnotationFS origin = createPOSAnno(mergeCAs, "NN", 0, 0);
        AnnotationFS target = createPOSAnno(mergeCAs, "NN", 1, 1);

        mergeCAs.addFsToIndexes(origin);
        mergeCAs.addFsToIndexes(target);

        AnnotationFS originToken = createTokenAnno(mergeCAs, 0, 0);
        AnnotationFS targetToken = createTokenAnno(mergeCAs, 1, 1);
        originToken.setFeatureValue(originToken.getType().getFeatureByBaseName("pos"), origin);
        targetToken.setFeatureValue(targetToken.getType().getFeatureByBaseName("pos"), target);

        mergeCAs.addFsToIndexes(originToken);
        mergeCAs.addFsToIndexes(targetToken);

        MergeCas.addRelationArcAnnotation(mergeCAs, clickedFs, true, false, originToken,
                targetToken);
        
        assertEquals(1, selectCovered(mergeCAs, type, 0, 1).size());
    }

    @Test
    public void simpleCopyRelationToStackedTargetsTest()
        throws Exception
    {
        CAS jcas = createJCas().getCas();
        Type type = jcas.getTypeSystem().getType(Dependency.class.getTypeName());

        AnnotationFS originClickedToken = createTokenAnno(jcas, 0, 0);
        AnnotationFS targetClickedToken = createTokenAnno(jcas, 1, 1);

        AnnotationFS originClicked = createPOSAnno(jcas, "NN", 0, 0);
        AnnotationFS targetClicked = createPOSAnno(jcas, "NN", 1, 1);

        jcas.addFsToIndexes(originClicked);
        jcas.addFsToIndexes(targetClicked);

        originClickedToken.setFeatureValue(originClickedToken.getType().getFeatureByBaseName("pos"),
                originClicked);
        targetClickedToken.setFeatureValue(targetClickedToken.getType().getFeatureByBaseName("pos"),
                targetClicked);

        Feature sourceFeature = type.getFeatureByBaseName(FEAT_REL_SOURCE);
        Feature targetFeature = type.getFeatureByBaseName(FEAT_REL_TARGET);

        AnnotationFS clickedFs = jcas.createAnnotation(type, 0, 1);
        clickedFs.setFeatureValue(sourceFeature, originClickedToken);
        clickedFs.setFeatureValue(targetFeature, targetClickedToken);
        jcas.addFsToIndexes(clickedFs);

        CAS mergeCAs = createJCas().getCas();
        AnnotationFS origin = createPOSAnno(mergeCAs, "NN", 0, 0);
        AnnotationFS target = createPOSAnno(mergeCAs, "NN", 1, 1);

        mergeCAs.addFsToIndexes(origin);
        mergeCAs.addFsToIndexes(target);

        AnnotationFS originToken = createTokenAnno(mergeCAs, 0, 0);
        AnnotationFS targetToken = createTokenAnno(mergeCAs, 1, 1);
        originToken.setFeatureValue(originToken.getType().getFeatureByBaseName("pos"), origin);
        targetToken.setFeatureValue(targetToken.getType().getFeatureByBaseName("pos"), target);

        mergeCAs.addFsToIndexes(originToken);
        mergeCAs.addFsToIndexes(targetToken);

        AnnotationFS origin2 = createPOSAnno(mergeCAs, "NN", 0, 0);
        AnnotationFS target2 = createPOSAnno(mergeCAs, "NN", 1, 1);

        mergeCAs.addFsToIndexes(origin2);
        mergeCAs.addFsToIndexes(target2);

        AnnotationFS originToken2 = createTokenAnno(mergeCAs, 0, 0);
        AnnotationFS targetToken2 = createTokenAnno(mergeCAs, 1, 1);
        originToken2.setFeatureValue(originToken.getType().getFeatureByBaseName("pos"), origin2);
        targetToken2.setFeatureValue(targetToken.getType().getFeatureByBaseName("pos"), target2);

        mergeCAs.addFsToIndexes(originToken2);
        mergeCAs.addFsToIndexes(targetToken2);

        assertThatExceptionOfType(AnnotationException.class)
                .isThrownBy(() -> MergeCas.addRelationArcAnnotation(mergeCAs, clickedFs, true,
                        false, originToken, targetToken))
                .withMessageContaining("Stacked sources exist");
    }

    @Test
    public void thatMergingRelationIsRejectedIfAlreadyExists()
        throws Exception
    {
        CAS jcas = createJCas().getCas();
        Type type = jcas.getTypeSystem().getType(Dependency.class.getTypeName());

        AnnotationFS originClickedToken = createTokenAnno(jcas, 0, 0);
        AnnotationFS targetClickedToken = createTokenAnno(jcas, 1, 1);

        AnnotationFS originClicked = createPOSAnno(jcas, "NN", 0, 0);
        AnnotationFS targetClicked = createPOSAnno(jcas, "NN", 1, 1);

        jcas.addFsToIndexes(originClicked);
        jcas.addFsToIndexes(targetClicked);

        originClickedToken.setFeatureValue(originClickedToken.getType().getFeatureByBaseName("pos"),
                originClicked);
        targetClickedToken.setFeatureValue(targetClickedToken.getType().getFeatureByBaseName("pos"),
                targetClicked);

        Feature sourceFeature = type.getFeatureByBaseName(FEAT_REL_SOURCE);
        Feature targetFeature = type.getFeatureByBaseName(FEAT_REL_TARGET);

        AnnotationFS clickedFs = jcas.createAnnotation(type, 0, 1);
        clickedFs.setFeatureValue(sourceFeature, originClickedToken);
        clickedFs.setFeatureValue(targetFeature, targetClickedToken);
        jcas.addFsToIndexes(clickedFs);

        CAS mergeCAs = createJCas().getCas();
        AnnotationFS origin = createPOSAnno(mergeCAs, "NN", 0, 0);
        AnnotationFS target = createPOSAnno(mergeCAs, "NN", 1, 1);

        mergeCAs.addFsToIndexes(origin);
        mergeCAs.addFsToIndexes(target);

        AnnotationFS originToken = createTokenAnno(mergeCAs, 0, 0);
        AnnotationFS targetToken = createTokenAnno(mergeCAs, 1, 1);
        originToken.setFeatureValue(originToken.getType().getFeatureByBaseName("pos"), origin);
        targetToken.setFeatureValue(targetToken.getType().getFeatureByBaseName("pos"), target);

        mergeCAs.addFsToIndexes(originToken);
        mergeCAs.addFsToIndexes(targetToken);

        AnnotationFS existing = mergeCAs.createAnnotation(type, 0, 1);
        existing.setFeatureValue(sourceFeature, originToken);
        existing.setFeatureValue(targetFeature, targetToken);
        mergeCAs.addFsToIndexes(clickedFs);

        assertThatExceptionOfType(AnnotationException.class)
                .isThrownBy(() -> MergeCas.addRelationArcAnnotation(mergeCAs, clickedFs, true,
                        false, originToken, targetToken))
                .withMessageContaining("annotation already exists");
    }
}

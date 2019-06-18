/*
 * Copyright 2015
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
package de.tudarmstadt.ukp.clarin.webanno.curation.casmerge;

import static de.tudarmstadt.ukp.clarin.webanno.api.WebAnnoConst.FEAT_REL_SOURCE;
import static de.tudarmstadt.ukp.clarin.webanno.api.WebAnnoConst.FEAT_REL_TARGET;
import static de.tudarmstadt.ukp.clarin.webanno.api.WebAnnoConst.RELATION_TYPE;
import static de.tudarmstadt.ukp.clarin.webanno.api.WebAnnoConst.SPAN_TYPE;
import static de.tudarmstadt.ukp.clarin.webanno.curation.CurationTestUtils.HOST_TYPE;
import static de.tudarmstadt.ukp.clarin.webanno.curation.CurationTestUtils.createMultiLinkWithRoleTestTypeSytem;
import static de.tudarmstadt.ukp.clarin.webanno.curation.CurationTestUtils.loadWebAnnoTSV;
import static de.tudarmstadt.ukp.clarin.webanno.curation.CurationTestUtils.loadWebAnnoTsv3;
import static de.tudarmstadt.ukp.clarin.webanno.curation.CurationTestUtils.makeLinkFS;
import static de.tudarmstadt.ukp.clarin.webanno.curation.CurationTestUtils.makeLinkHostFS;
import static de.tudarmstadt.ukp.clarin.webanno.curation.CurationTestUtils.makeLinkHostMultiSPanFeatureFS;
import static de.tudarmstadt.ukp.clarin.webanno.curation.CurationTestUtils.readWebAnnoTSV;
import static de.tudarmstadt.ukp.clarin.webanno.curation.casdiff.CasDiff.LinkCompareBehavior.LINK_TARGET_AS_LABEL;
import static de.tudarmstadt.ukp.clarin.webanno.curation.casdiff.CasDiff.RelationDiffAdapter.DEPENDENCY_DIFF_ADAPTER;
import static de.tudarmstadt.ukp.clarin.webanno.curation.casdiff.CasDiff.SpanDiffAdapter.NER_DIFF_ADAPTER;
import static de.tudarmstadt.ukp.clarin.webanno.curation.casdiff.CasDiff.SpanDiffAdapter.POS_DIFF_ADAPTER;
import static de.tudarmstadt.ukp.clarin.webanno.curation.casdiff.CasDiff.doDiff;
import static de.tudarmstadt.ukp.clarin.webanno.model.AnchoringMode.CHARACTERS;
import static de.tudarmstadt.ukp.clarin.webanno.model.AnchoringMode.SINGLE_TOKEN;
import static de.tudarmstadt.ukp.clarin.webanno.model.AnchoringMode.TOKENS;
import static de.tudarmstadt.ukp.clarin.webanno.model.Mode.CURATION;
import static de.tudarmstadt.ukp.clarin.webanno.model.OverlapMode.NO_OVERLAP;
import static de.tudarmstadt.ukp.clarin.webanno.model.OverlapMode.OVERLAP_ONLY;
import static java.util.Arrays.asList;
import static org.apache.uima.fit.factory.AnalysisEngineFactory.createEngineDescription;
import static org.apache.uima.fit.factory.JCasFactory.createJCas;
import static org.apache.uima.fit.factory.JCasFactory.createText;
import static org.apache.uima.fit.pipeline.SimplePipeline.runPipeline;
import static org.apache.uima.fit.util.CasUtil.getType;
import static org.apache.uima.fit.util.CasUtil.select;
import static org.apache.uima.fit.util.CasUtil.selectCovered;
import static org.apache.uima.fit.util.JCasUtil.select;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.junit.Assert.assertEquals;
import static org.junit.Assume.assumeTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.io.FileUtils;
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
import org.junit.Rule;
import org.junit.Test;
import org.mockito.Mock;

import de.tudarmstadt.ukp.clarin.webanno.api.AnnotationSchemaService;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.exception.AnnotationException;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.feature.FeatureSupport;
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
import de.tudarmstadt.ukp.clarin.webanno.curation.CurationTestUtils;
import de.tudarmstadt.ukp.clarin.webanno.curation.casdiff.CasDiff;
import de.tudarmstadt.ukp.clarin.webanno.curation.casdiff.CasDiff.DiffAdapter;
import de.tudarmstadt.ukp.clarin.webanno.curation.casdiff.CasDiff.DiffResult;
import de.tudarmstadt.ukp.clarin.webanno.curation.casdiff.CasDiff.RelationDiffAdapter;
import de.tudarmstadt.ukp.clarin.webanno.curation.casdiff.CasDiff.SpanDiffAdapter;
import de.tudarmstadt.ukp.clarin.webanno.curation.casdiff.CasDiff.SpanPosition;
import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationFeature;
import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationLayer;
import de.tudarmstadt.ukp.clarin.webanno.model.LinkMode;
import de.tudarmstadt.ukp.clarin.webanno.model.MultiValueMode;
import de.tudarmstadt.ukp.clarin.webanno.model.OverlapMode;
import de.tudarmstadt.ukp.clarin.webanno.model.Project;
import de.tudarmstadt.ukp.clarin.webanno.model.SourceDocument;
import de.tudarmstadt.ukp.clarin.webanno.security.model.User;
import de.tudarmstadt.ukp.clarin.webanno.tsv.WebannoTsv3XWriter;
import de.tudarmstadt.ukp.dkpro.core.api.lexmorph.type.pos.POS;
import de.tudarmstadt.ukp.dkpro.core.api.metadata.type.DocumentMetaData;
import de.tudarmstadt.ukp.dkpro.core.api.ner.type.NamedEntity;
import de.tudarmstadt.ukp.dkpro.core.api.segmentation.type.Sentence;
import de.tudarmstadt.ukp.dkpro.core.api.segmentation.type.Token;
import de.tudarmstadt.ukp.dkpro.core.api.syntax.type.dependency.Dependency;
import de.tudarmstadt.ukp.dkpro.core.testing.DkproTestContext;

public class CasMergeTest
{
    private @Mock AnnotationSchemaService schemaService;
    
    private CasMerge sut;
    
    private LayerSupportRegistryImpl layerSupportRegistry;
    private FeatureSupportRegistryImpl featureSupportRegistry;
    private Project project;
    private AnnotationLayer sentenceLayer;
    private AnnotationLayer tokenLayer;
    private AnnotationFeature tokenPosFeature;
    private AnnotationLayer posLayer;
    private AnnotationFeature posFeature;
    private AnnotationFeature posCoarseFeature;
    private AnnotationLayer neLayer;
    private AnnotationFeature neFeature;
    private AnnotationFeature neIdentifierFeature;
    private AnnotationLayer depLayer;
    private AnnotationFeature depFeature;
    private AnnotationFeature depFlavorFeature;
    private AnnotationLayer slotLayer;
    private AnnotationFeature slotFeature;
    private AnnotationFeature stringFeature;
    private AnnotationLayer multiValRel;
    private AnnotationFeature multiValRelRel1;
    private AnnotationFeature multiValRelRel2;
    private AnnotationLayer multiValSpan;
    private AnnotationFeature multiValSpanF1;
    private AnnotationFeature multiValSpanF2;
    private SourceDocument document;
    private List<String> entryTypes;
    
    private static final RelationDiffAdapter MULTIVALREL_DIFF_ADAPTER = new RelationDiffAdapter(
            "webanno.custom.Multivalrel", "Dependent", "Governor", "rel1", "rel2");
    private static final SpanDiffAdapter MULTIVALSPAN_DIFF_ADAPTER = new SpanDiffAdapter(
            "webanno.custom.Multivalspan", "f1", "f2");
    
    @Before
    public void setup() throws Exception
    {
        initMocks(this);
        
        entryTypes = new ArrayList<>();
        entryTypes.add(Sentence.class.getName());
        entryTypes.add(Token.class.getName());
        
        project = new Project();
        
        document = new SourceDocument();
        document.setProject(project);
        document.setName("document");

        sentenceLayer = new AnnotationLayer(Sentence.class.getName(), "Sentence", SPAN_TYPE, null,
                true, CHARACTERS, NO_OVERLAP);

        tokenLayer = new AnnotationLayer(Token.class.getName(), "Token", SPAN_TYPE, null, true,
                CHARACTERS, NO_OVERLAP);
        
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

        posCoarseFeature = new AnnotationFeature();
        posCoarseFeature.setName("coarseValue");
        posCoarseFeature.setEnabled(true);
        posCoarseFeature.setType(CAS.TYPE_NAME_STRING);
        posCoarseFeature.setUiName("coarseValue");
        posCoarseFeature.setLayer(posLayer);
        posCoarseFeature.setProject(project);
        posCoarseFeature.setVisible(true);

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

        neIdentifierFeature = new AnnotationFeature();
        neIdentifierFeature.setName("identifier");
        neIdentifierFeature.setEnabled(true);
        neIdentifierFeature.setType(CAS.TYPE_NAME_STRING);
        neIdentifierFeature.setUiName("identifier");
        neIdentifierFeature.setLayer(neLayer);
        neIdentifierFeature.setProject(project);
        neIdentifierFeature.setVisible(true);

        depLayer = new AnnotationLayer(Dependency.class.getName(), "Dependency", RELATION_TYPE,
                project, true, SINGLE_TOKEN, OVERLAP_ONLY);
        depLayer.setAttachType(tokenLayer);
        depLayer.setAttachFeature(tokenPosFeature);

        depFeature = new AnnotationFeature();
        depFeature.setName("DependencyType");
        depFeature.setEnabled(true);
        depFeature.setType(CAS.TYPE_NAME_STRING);
        depFeature.setUiName("Relation");
        depFeature.setLayer(depLayer);
        depFeature.setProject(project);
        depFeature.setVisible(true);

        depFlavorFeature = new AnnotationFeature();
        depFlavorFeature.setName("flavor");
        depFlavorFeature.setEnabled(true);
        depFlavorFeature.setType(CAS.TYPE_NAME_STRING);
        depFlavorFeature.setUiName("flavor");
        depFlavorFeature.setLayer(depLayer);
        depFlavorFeature.setProject(project);
        depFlavorFeature.setVisible(true);

        slotLayer = new AnnotationLayer(HOST_TYPE, HOST_TYPE, SPAN_TYPE, project, false,
                SINGLE_TOKEN, NO_OVERLAP);
        
        slotFeature = new AnnotationFeature();
        slotFeature.setName("links");
        slotFeature.setEnabled(true);
        slotFeature.setType(Token.class.getName());
        slotFeature.setMode(MultiValueMode.ARRAY);
        slotFeature.setLinkMode(LinkMode.WITH_ROLE);
        slotFeature.setLinkTypeName(CurationTestUtils.LINK_TYPE);
        slotFeature.setLinkTypeRoleFeatureName("role");
        slotFeature.setLinkTypeTargetFeatureName("target");
        slotFeature.setUiName("links");
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
        
        multiValSpan = new AnnotationLayer("webanno.custom.Multivalspan", "Multivalspan", SPAN_TYPE,
                project, true, TOKENS, OVERLAP_ONLY);
        
        multiValSpanF1 = new AnnotationFeature();
        multiValSpanF1.setName("f1");
        multiValSpanF1.setEnabled(true);
        multiValSpanF1.setType(CAS.TYPE_NAME_STRING);
        multiValSpanF1.setUiName("f1");
        multiValSpanF1.setLayer(multiValSpan);
        multiValSpanF1.setProject(project);
        multiValSpanF1.setVisible(true);
        
        multiValSpanF2 = new AnnotationFeature();
        multiValSpanF2.setName("f2");
        multiValSpanF2.setEnabled(true);
        multiValSpanF2.setType(CAS.TYPE_NAME_STRING);
        multiValSpanF2.setUiName("f2");
        multiValSpanF2.setLayer(multiValSpan);
        multiValSpanF2.setProject(project);
        multiValSpanF2.setVisible(true);
        
        multiValRel = new AnnotationLayer("webanno.custom.Multivalrel", "Multivalrel",
                RELATION_TYPE, project, true, SINGLE_TOKEN, OVERLAP_ONLY);
        multiValRel.setAttachType(multiValSpan);
        
        multiValRelRel1 = new AnnotationFeature();
        multiValRelRel1.setName("rel1");
        multiValRelRel1.setEnabled(true);
        multiValRelRel1.setType(CAS.TYPE_NAME_STRING);
        multiValRelRel1.setUiName("rel1");
        multiValRelRel1.setLayer(multiValSpan);
        multiValRelRel1.setProject(project);
        multiValRelRel1.setVisible(true);
        
        multiValRelRel2 = new AnnotationFeature();
        multiValRelRel2.setName("rel2");
        multiValRelRel2.setEnabled(true);
        multiValRelRel2.setType(CAS.TYPE_NAME_STRING);
        multiValRelRel2.setUiName("rel2");
        multiValRelRel2.setLayer(multiValSpan);
        multiValRelRel2.setProject(project);
        multiValRelRel2.setVisible(true);        
        
        when(schemaService.findLayer(any(Project.class), any(String.class))).thenAnswer(call -> {
            String type = call.getArgument(1, String.class);
            if (type.equals(Sentence.class.getName())) {
                return sentenceLayer;
            }
            if (type.equals(Token.class.getName())) {
                return tokenLayer;
            }
            if (type.equals(Dependency.class.getName())) {
                return depLayer;
            }
            if (type.equals(POS.class.getName())) {
                return posLayer;
            }
            if (type.equals(NamedEntity.class.getName())) {
                return neLayer;
            }
            if (type.equals(CurationTestUtils.HOST_TYPE)) {
                return slotLayer;
            }
            if (type.equals("webanno.custom.Multivalrel")) {
                return multiValRel;
            }
            if (type.equals("webanno.custom.Multivalspan")) {
                return multiValSpan;
            }
            throw new IllegalStateException("Unknown layer type: " + type);
        });
        
        when(schemaService.listAnnotationFeature(any(AnnotationLayer.class))).thenAnswer(call -> { 
            AnnotationLayer type = call.getArgument(0, AnnotationLayer.class);
            if (type.getName().equals(Sentence.class.getName())) {
                return asList();
            }
            if (type.getName().equals(Token.class.getName())) {
                return asList();
            }
            if (type.getName().equals(Dependency.class.getName())) {
                return asList(depFeature, depFlavorFeature);
            }
            if (type.getName().equals(POS.class.getName())) {
                return asList(posFeature, posCoarseFeature);
            }
            if (type.getName().equals(NamedEntity.class.getName())) {
                return asList(neFeature, neIdentifierFeature);
            }
            if (type.getName().equals(HOST_TYPE)) {
                return asList(slotFeature, stringFeature);
            }
            if (type.getName().equals("webanno.custom.Multivalrel")) {
                return asList(multiValRelRel1, multiValRelRel2);
            }
            if (type.getName().equals("webanno.custom.Multivalspan")) {
                return asList(multiValSpanF1, multiValSpanF2);
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
        
        sut = new CasMerge(schemaService);
    }


    @Test
    public void simpleSpanNoDiffNoLabelTest()
        throws Exception
    {
        Map<String, List<CAS>> casByUser = loadWebAnnoTSV(null, 
                "mergecas/simplespan/1sentence.tsv",
                "mergecas/simplespan/1sentence.tsv");
        
        JCas curatorCas = createText(casByUser.values().stream()
                .flatMap(Collection::stream).findFirst().get().getDocumentText());

        entryTypes.add(POS.class.getName());

        List<SpanDiffAdapter> diffAdapters = asList(POS_DIFF_ADAPTER);

        DiffResult result = doDiff(entryTypes, diffAdapters, LINK_TARGET_AS_LABEL, casByUser);

        sut.reMergeCas(result, document, null, curatorCas.getCas(), getSingleCasByUser(casByUser));

        CAS actual = readWebAnnoTSV("mergecas/simplespan/1sentence.tsv", null);
        
        casByUser = new HashMap<>();
        casByUser.put("actual", asList(actual));
        casByUser.put("merge", asList(curatorCas.getCas()));

        result = doDiff(entryTypes, diffAdapters, LINK_TARGET_AS_LABEL, casByUser);

        assertEquals(0, result.getDifferingConfigurationSets().size());
        assertEquals(0, result.getIncompleteConfigurationSets().size());
    }

    @Test
    public void simpleSpanDiffNoLabelTest()
        throws Exception
    {
        Map<String, List<CAS>> casByUser = loadWebAnnoTSV(null, 
                "mergecas/simplespan/1sentence.tsv",
                "mergecas/simplespan/1sentenceempty.tsv");
        
        JCas curatorCas = createText(casByUser.values().stream()
                .flatMap(Collection::stream).findFirst().get().getDocumentText());

        entryTypes.add(POS.class.getName());

        List<SpanDiffAdapter> diffAdapters = asList(POS_DIFF_ADAPTER);

        DiffResult result = doDiff(entryTypes, diffAdapters, LINK_TARGET_AS_LABEL, casByUser);

        sut.reMergeCas(result, document, null, curatorCas.getCas(), getSingleCasByUser(casByUser));

        casByUser = new HashMap<>();
        CAS actual = readWebAnnoTSV("mergecas/simplespan/1sentenceempty.tsv", null);
        casByUser.put("actual", asList(actual));
        casByUser.put("merge", asList(curatorCas.getCas()));

        result = doDiff(entryTypes, diffAdapters, LINK_TARGET_AS_LABEL, casByUser);

        assertEquals(0, result.getDifferingConfigurationSets().size());
        assertEquals(0, result.getIncompleteConfigurationSets().size());
    }

    /**
     * If one annotator has provided an annotation at a given position and the other annotator did
     * not (i.e. the annotations are incomplete), then this should be detected as a disagreement. 
     */
    @Test
    public void thatIncompleteAnnotationIsNotMerged()
        throws Exception
    {
        entryTypes.add(POS.class.getName());
        
        JCas user1 = JCasFactory.createText("word");
        token(user1, 0, 4, "X");
        
        JCas user2 = JCasFactory.createText("word");
        token(user2, 0, 4, null);
        
        Map<String, List<CAS>> casByUser = new LinkedHashMap<>();
        casByUser.put("user1", asList(user1.getCas()));
        casByUser.put("user2", asList(user2.getCas()));
        
        JCas curatorCas = createText(casByUser.values().stream()
                .flatMap(Collection::stream).findFirst().get().getDocumentText());
        
        DiffResult result = doDiff(entryTypes, asList(POS_DIFF_ADAPTER), LINK_TARGET_AS_LABEL,
                casByUser);

        sut.reMergeCas(result, document, null, curatorCas.getCas(), getSingleCasByUser(casByUser));

        assertThat(result.getDifferingConfigurationSets()).isEmpty();
        assertThat(result.getIncompleteConfigurationSets().values())
                .extracting(set -> set.getPosition())
                .usingFieldByFieldElementComparator()
                .containsExactly(new SpanPosition(null, null, 0, POS.class.getName(), 0, 4, "word",
                        null, null, -1, -1, null, null));
        
        assertThat(select(curatorCas, POS.class)).isEmpty();
    }
    
    /**
     * If one annotator has provided an annotation at a given position and the other annotator did
     * not (i.e. the annotations are incomplete), then this should be detected as a disagreement. 
     */
    @Test
    public void thatIncompleteAnnotationIsMerged()
        throws Exception
    {
        entryTypes.add(POS.class.getName());
        
        JCas user1 = JCasFactory.createText("word");
        token(user1, 0, 4, "X");
        
        JCas user2 = JCasFactory.createText("word");
        token(user2, 0, 4, null);
        
        Map<String, List<CAS>> casByUser = new LinkedHashMap<>();
        casByUser.put("user1", asList(user1.getCas()));
        casByUser.put("user2", asList(user2.getCas()));
        
        JCas curatorCas = createText(casByUser.values().stream()
                .flatMap(Collection::stream).findFirst().get().getDocumentText());
        
        DiffResult result = doDiff(entryTypes, asList(POS_DIFF_ADAPTER), LINK_TARGET_AS_LABEL,
                casByUser);

        sut.setMergeIncompleteAnnotations(true);
        sut.reMergeCas(result, document, null, curatorCas.getCas(), getSingleCasByUser(casByUser));

        assertThat(result.getDifferingConfigurationSets()).isEmpty();
        assertThat(result.getIncompleteConfigurationSets().values())
                .extracting(set -> set.getPosition())
                .usingFieldByFieldElementComparator()
                .containsExactly(new SpanPosition(null, null, 0, POS.class.getName(), 0, 4, "word",
                        null, null, -1, -1, null, null));
        
        assertThat(select(curatorCas, POS.class)).hasSize(1);
    }

    private Token token(JCas aJCas, int aBegin, int aEnd, String aPos)
    {
        POS pos = null;
        if (aPos != null) {
            pos = new POS(aJCas, aBegin, aEnd);
            pos.setPosValue(aPos);
            pos.addToIndexes();
        }
        Token token = new Token(aJCas, aBegin, aEnd);
        token.setPos(pos);
        token.addToIndexes();
        return token;
    }
    
    @Test
    public void simpleSpanNoDiffWithLabelTest()
        throws Exception
    {
        Map<String, List<CAS>> casByUser = loadWebAnnoTSV(null,
                "mergecas/simplespan/1sentenceposlabel.tsv",
                "mergecas/simplespan/1sentenceposlabel.tsv");
        
        JCas curatorCas = createText(casByUser.values().stream()
                .flatMap(Collection::stream).findFirst().get().getDocumentText());

        entryTypes.add(POS.class.getName());

        List<SpanDiffAdapter> diffAdapters = asList(POS_DIFF_ADAPTER);

        DiffResult result = doDiff(entryTypes, diffAdapters, LINK_TARGET_AS_LABEL, casByUser);

        sut.reMergeCas(result, document, null, curatorCas.getCas(), getSingleCasByUser(casByUser));

        casByUser = new HashMap<>();
        CAS actual = readWebAnnoTSV("mergecas/simplespan/1sentenceposlabel.tsv",
                null);
        casByUser.put("actual", asList(actual));
        casByUser.put("merge", asList(curatorCas.getCas()));

        result = doDiff(entryTypes, diffAdapters, LINK_TARGET_AS_LABEL, casByUser);

        assertEquals(0, result.getDifferingConfigurationSets().size());
        assertEquals(0, result.getIncompleteConfigurationSets().size());
    }

    @Test
    public void simpleSpanDiffWithLabelTest()
        throws Exception
    {
        Map<String, List<CAS>> casByUser = loadWebAnnoTSV(null,
                "mergecas/simplespan/1sentenceposlabel.tsv",
                "mergecas/simplespan/1sentenceposlabel2.tsv");
        
        JCas curatorCas = createText(casByUser.values().stream()
                .flatMap(Collection::stream).findFirst().get().getDocumentText());

        entryTypes.add(POS.class.getName());

        List<SpanDiffAdapter> diffAdapters = asList(POS_DIFF_ADAPTER);

        DiffResult result = doDiff(entryTypes, diffAdapters, LINK_TARGET_AS_LABEL,
                casByUser);

        sut.reMergeCas(result, document, null, curatorCas.getCas(), getSingleCasByUser(casByUser));

        casByUser = new HashMap<>();
        CAS actual = readWebAnnoTSV("mergecas/simplespan/1sentenceempty.tsv", null);
        casByUser.put("actual", asList(actual));
        casByUser.put("merge", asList(curatorCas.getCas()));

        result = CasDiff.doDiff(entryTypes, diffAdapters, LINK_TARGET_AS_LABEL, casByUser);

        assertEquals(0, result.getDifferingConfigurationSets().size());
        assertEquals(0, result.getIncompleteConfigurationSets().size());
    }

    @Test
    public void simpleSpanDiffIncompleteTest()
        throws Exception
    {
        Map<String, List<CAS>> casByUser = loadWebAnnoTSV(null,
                "mergecas/simplespan/1sentenceposlabel2.tsv",
                "mergecas/simplespan/1sentenceposlabel3.tsv");
        
        JCas curatorCas = createText(casByUser.values().stream()
                .flatMap(Collection::stream).findFirst().get().getDocumentText());

        entryTypes.add(POS.class.getName());

        List<SpanDiffAdapter> diffAdapters = asList(POS_DIFF_ADAPTER);

        DiffResult result = doDiff(entryTypes, diffAdapters, LINK_TARGET_AS_LABEL, casByUser);

        sut.reMergeCas(result, document, null, curatorCas.getCas(), getSingleCasByUser(casByUser));

        CAS actual = readWebAnnoTSV("mergecas/simplespan/1sentenceposlabel2and3merged.tsv", null);
        casByUser = new HashMap<>();
        casByUser.put("actual", asList(actual));
        casByUser.put("merge", asList(curatorCas.getCas()));

        result = doDiff(entryTypes, diffAdapters, LINK_TARGET_AS_LABEL, casByUser);

        assertEquals(0, result.getDifferingConfigurationSets().size());
        assertEquals(2, result.getIncompleteConfigurationSets().size());
    }
    
    @Test
    public void simpleSpanDiffWithLabelStackingTest()
        throws Exception
    {
        Map<String, List<CAS>> casByUser = loadWebAnnoTSV(null,
                "mergecas/simplespan/1sentenceNEstacked.tsv",
                "mergecas/simplespan/1sentenceNEstacked.tsv");
        
        JCas curatorCas = createText(casByUser.values().stream()
                .flatMap(Collection::stream).findFirst().get().getDocumentText());

        entryTypes.add(NamedEntity.class.getName());

        List<SpanDiffAdapter> diffAdapters = asList(NER_DIFF_ADAPTER);

        DiffResult result = doDiff(entryTypes, diffAdapters, LINK_TARGET_AS_LABEL, casByUser);

        sut.reMergeCas(result, document, null, curatorCas.getCas(), getSingleCasByUser(casByUser));

        CAS actual = readWebAnnoTSV("mergecas/simplespan/1sentenceNEempty.tsv", null);
        casByUser = new HashMap<>();
        casByUser.put("actual", asList(actual));
        casByUser.put("merge", asList(curatorCas.getCas()));

        result = doDiff(entryTypes, diffAdapters, LINK_TARGET_AS_LABEL, casByUser);

        assertEquals(0, result.getDifferingConfigurationSets().size());
        assertEquals(0, result.getIncompleteConfigurationSets().size());
    }

    @Test
    public void simpleSpanDiffWithLabelStacking2Test()
        throws Exception
    {
        Map<String, List<CAS>> casByUser = loadWebAnnoTSV(null,
                "mergecas/simplespan/1sentenceNE.tsv",
                "mergecas/simplespan/1sentenceNEstacked.tsv");
        
        JCas curatorCas = createText(casByUser.values().stream()
                .flatMap(Collection::stream).findFirst().get().getDocumentText());

        entryTypes.add(NamedEntity.class.getName());

        List<SpanDiffAdapter> diffAdapters = asList(NER_DIFF_ADAPTER);

        DiffResult result = doDiff(entryTypes, diffAdapters, LINK_TARGET_AS_LABEL, casByUser);

        sut.reMergeCas(result, document, null, curatorCas.getCas(), getSingleCasByUser(casByUser));

        CAS actual = readWebAnnoTSV("mergecas/simplespan/1sentenceNEempty.tsv", null);
        casByUser = new HashMap<>();
        casByUser.put("actual", asList(actual));
        casByUser.put("merge", asList(curatorCas.getCas()));

        result = doDiff(entryTypes, diffAdapters, LINK_TARGET_AS_LABEL, casByUser);

        assertEquals(0, result.getDifferingConfigurationSets().size());
        assertEquals(0, result.getIncompleteConfigurationSets().size());
    }

    @Test
    public void thatMultipleStackedAnnotationsAreNotMerged()
        throws Exception
    {
        Map<String, List<CAS>> casByUser = loadWebAnnoTSV(null,
                // this document contains two stacked NE annoatations (ORG, ORGpart)
                "mergecas/simplespan/1sentenceNE.tsv",
                // this document contains three stacked NE annoatations (ORG, ORG, ORGpart)
                "mergecas/simplespan/1sentenceNEstacked2.tsv");
        
        JCas curatorCas = createText(casByUser.values().stream()
                .flatMap(Collection::stream).findFirst().get().getDocumentText());

        entryTypes.add(NamedEntity.class.getName());

        List<SpanDiffAdapter> diffAdapters = asList(NER_DIFF_ADAPTER);

        DiffResult result = doDiff(entryTypes, diffAdapters, LINK_TARGET_AS_LABEL, casByUser);

        sut.reMergeCas(result, document, null, curatorCas.getCas(), getSingleCasByUser(casByUser));

        // If there are any stacked annotations, then no merging is performed
        assertThat(select(curatorCas, NamedEntity.class))
                .isEmpty();
    }

    @Test
    public void simpleSpanNoDiffMultiFeatureTest()
        throws Exception
    {
        Map<String, List<CAS>> casByUser = new HashMap<>();
        casByUser.put("user1", asList(loadWebAnnoTsv3(
                "testsuite/" + testContext.getMethodName() + "/user1.tsv")
                        .getCas()));
        casByUser.put("user2", asList(loadWebAnnoTsv3(
                "testsuite/" + testContext.getMethodName() + "/user2.tsv")
                        .getCas()));
        JCas curatorCas = createText(casByUser.values().stream().flatMap(Collection::stream)
                .findFirst().get().getDocumentText());

        entryTypes.add(NamedEntity.class.getName());

        List<? extends DiffAdapter> diffAdapters = asList(NER_DIFF_ADAPTER);

        DiffResult result = doDiff(entryTypes, diffAdapters, LINK_TARGET_AS_LABEL, casByUser);

        result.print(System.out);

        sut.reMergeCas(result, document, null, curatorCas.getCas(), getSingleCasByUser(casByUser));

        assertMatch(curatorCas);
    }

    @Test
    public void simpleSpanDiffMultiFeatureTest()
        throws Exception
    {
        Map<String, List<CAS>> casByUser = new HashMap<>();
        casByUser.put("user1", asList(loadWebAnnoTsv3(
                "testsuite/" + testContext.getMethodName() + "/user1.tsv")
                        .getCas()));
        casByUser.put("user2", asList(loadWebAnnoTsv3(
                "testsuite/" + testContext.getMethodName() + "/user2.tsv")
                        .getCas()));
        JCas curatorCas = createText(casByUser.values().stream().flatMap(Collection::stream)
                .findFirst().get().getDocumentText());

        entryTypes.add(NamedEntity.class.getName());

        List<? extends DiffAdapter> diffAdapters = asList(NER_DIFF_ADAPTER);

        DiffResult result = doDiff(entryTypes, diffAdapters, LINK_TARGET_AS_LABEL, casByUser);

        result.print(System.out);

        sut.reMergeCas(result, document, null, curatorCas.getCas(), getSingleCasByUser(casByUser));

        assertMatch(curatorCas);
    }

    @Test
    public void simpleRelNoDiffTest()
        throws Exception
    {
        Map<String, List<CAS>> casByUser = loadWebAnnoTSV(null,
                "mergecas/rels/1sentencesamerel.tsv", 
                "mergecas/rels/1sentencesamerel.tsv");
        
        JCas curatorCas = createText(casByUser.values().stream()
                .flatMap(Collection::stream).findFirst().get().getDocumentText());

        entryTypes.add(Dependency.class.getName());
        entryTypes.add(POS.class.getName());

        List<? extends DiffAdapter> diffAdapters = asList(
                DEPENDENCY_DIFF_ADAPTER,
                POS_DIFF_ADAPTER);

        DiffResult result = doDiff(entryTypes, diffAdapters, LINK_TARGET_AS_LABEL, casByUser);

        sut.reMergeCas(result, document, null, curatorCas.getCas(), getSingleCasByUser(casByUser));

        CAS actual = CurationTestUtils.readWebAnnoTSV("mergecas/rels/1sentencesamerel.tsv", null);
        casByUser = new HashMap<>();
        casByUser.put("actual", asList(actual));
        casByUser.put("merge", asList(curatorCas.getCas()));

        result = doDiff(entryTypes, diffAdapters, LINK_TARGET_AS_LABEL, casByUser);

        assertEquals(0, result.getDifferingConfigurationSets().size());
        assertEquals(0, result.getIncompleteConfigurationSets().size());
    }

    @Test
    public void simpleRelGovDiffTest()
        throws Exception
    {
        Map<String, List<CAS>> casByUser = loadWebAnnoTSV(null,
                "mergecas/rels/1sentencesamerel.tsv", 
                "mergecas/rels/1sentencesamerel2.tsv");
        
        JCas curatorCas = createText(casByUser.values().stream()
                .flatMap(Collection::stream).findFirst().get().getDocumentText());

        entryTypes.add(Dependency.class.getName());
        entryTypes.add(POS.class.getName());

        List<? extends DiffAdapter> diffAdapters = asList(
                DEPENDENCY_DIFF_ADAPTER,
                POS_DIFF_ADAPTER);

        DiffResult result = doDiff(entryTypes, diffAdapters, LINK_TARGET_AS_LABEL, casByUser);

        sut.reMergeCas(result, document, null, curatorCas.getCas(), getSingleCasByUser(casByUser));

        casByUser = new HashMap<>();
        CAS actual = readWebAnnoTSV("mergecas/rels/1sentencesamerel3.tsv", null);
        casByUser.put("actual", asList(actual));
        casByUser.put("merge", asList(curatorCas.getCas()));

        result = doDiff(entryTypes, diffAdapters, LINK_TARGET_AS_LABEL, casByUser);

        assertEquals(0, result.getDifferingConfigurationSets().size());
        assertEquals(0, result.getIncompleteConfigurationSets().size());
    }

    @Test
    public void simpleRelTypeDiffTest()
        throws Exception
    {
        Map<String, List<CAS>> casByUser = loadWebAnnoTSV(null,
                "mergecas/rels/1sentencesamerel.tsv", 
                "mergecas/rels/1sentencesamerel4.tsv");
        
        JCas curatorCas = createText(casByUser.values().stream()
                .flatMap(Collection::stream).findFirst().get().getDocumentText());

        entryTypes.add(Dependency.class.getName());
        entryTypes.add(POS.class.getName());

        List<? extends DiffAdapter> diffAdapters = asList(
                DEPENDENCY_DIFF_ADAPTER,
                POS_DIFF_ADAPTER);

        DiffResult result = doDiff(entryTypes, diffAdapters, LINK_TARGET_AS_LABEL, casByUser);

        sut.reMergeCas(result, document, null, curatorCas.getCas(), getSingleCasByUser(casByUser));

        CAS actual = readWebAnnoTSV("mergecas/rels/1sentencesamerel5.tsv", null);
        casByUser = new HashMap<>();
        casByUser.put("actual", asList(actual));
        casByUser.put("merge", asList(curatorCas.getCas()));

        result = doDiff(entryTypes, diffAdapters, LINK_TARGET_AS_LABEL, casByUser);

        assertEquals(0, result.getDifferingConfigurationSets().size());
        assertEquals(0, result.getIncompleteConfigurationSets().size());
    }

    @Test
    public void simpleRelGovStackedTest()
        throws Exception
    {
        Map<String, List<CAS>> casByUser = new HashMap<>();
        casByUser.put("user1", asList(loadWebAnnoTsv3(
                "testsuite/" + testContext.getMethodName() + "/user1.tsv")
                        .getCas()));
        casByUser.put("user2", asList(loadWebAnnoTsv3(
                "testsuite/" + testContext.getMethodName() + "/user2.tsv")
                        .getCas()));
        JCas curatorCas = createText(casByUser.values().stream().flatMap(Collection::stream)
                .findFirst().get().getDocumentText());

        entryTypes.add("webanno.custom.Multivalrel");
        entryTypes.add("webanno.custom.Multivalspan");

        List<? extends DiffAdapter> diffAdapters = asList(MULTIVALREL_DIFF_ADAPTER,
                MULTIVALSPAN_DIFF_ADAPTER);

        DiffResult result = doDiff(entryTypes, diffAdapters, LINK_TARGET_AS_LABEL, casByUser);

        result.print(System.out);

        sut.reMergeCas(result, document, null, curatorCas.getCas(), getSingleCasByUser(casByUser));

        assertMatch(curatorCas);        
    }

    @Test
    public void relStackedTest()
        throws Exception
    {
        Map<String, List<CAS>> casByUser = new HashMap<>();
        casByUser.put("user1", asList(loadWebAnnoTsv3(
                "testsuite/" + testContext.getMethodName() + "/user1.tsv")
                        .getCas()));
        casByUser.put("user2", asList(loadWebAnnoTsv3(
                "testsuite/" + testContext.getMethodName() + "/user2.tsv")
                        .getCas()));
        JCas curatorCas = createText(casByUser.values().stream().flatMap(Collection::stream)
                .findFirst().get().getDocumentText());

        entryTypes.add("webanno.custom.Multivalrel");
        entryTypes.add("webanno.custom.Multivalspan");

        List<? extends DiffAdapter> diffAdapters = asList(MULTIVALREL_DIFF_ADAPTER,
                MULTIVALSPAN_DIFF_ADAPTER);

        DiffResult result = doDiff(entryTypes, diffAdapters, LINK_TARGET_AS_LABEL, casByUser);

        result.print(System.out);

        sut.reMergeCas(result, document, null, curatorCas.getCas(), getSingleCasByUser(casByUser));

        assertMatch(curatorCas);        
    }

    @Test
    public void relationLabelTest()
        throws Exception
    {
        Map<String, List<CAS>> casByUser = new HashMap<>();
        casByUser.put("user1", asList(loadWebAnnoTsv3(
                "testsuite/" + testContext.getMethodName() + "/user1.tsv")
                        .getCas()));
        casByUser.put("user2", asList(loadWebAnnoTsv3(
                "testsuite/" + testContext.getMethodName() + "/user2.tsv")
                        .getCas()));
        JCas curatorCas = createText(casByUser.values().stream().flatMap(Collection::stream)
                .findFirst().get().getDocumentText());

        entryTypes.add(POS.class.getName());
        entryTypes.add(Dependency.class.getName());

        List<? extends DiffAdapter> diffAdapters = asList(DEPENDENCY_DIFF_ADAPTER);

        DiffResult result = doDiff(entryTypes, diffAdapters, LINK_TARGET_AS_LABEL, casByUser);

        result.print(System.out);

        sut.reMergeCas(result, document, null, curatorCas.getCas(), getSingleCasByUser(casByUser));

        assertMatch(curatorCas);
    }

    @Test
    public void multiLinkWithRoleNoDifferenceTest()
        throws Exception
    {
        JCas jcasA = createJCas(createMultiLinkWithRoleTestTypeSytem("f1"));
        makeLinkHostFS(jcasA, 0, 0, makeLinkFS(jcasA, "slot1", 0, 0));
        makeLinkHostFS(jcasA, 10, 10, makeLinkFS(jcasA, "slot1", 10, 10));

        JCas jcasB = createJCas(createMultiLinkWithRoleTestTypeSytem("f1"));
        makeLinkHostFS(jcasB, 0, 0, makeLinkFS(jcasB, "slot1", 0, 0));
        makeLinkHostFS(jcasB, 10, 10, makeLinkFS(jcasB, "slot1", 10, 10));

        Map<String, List<CAS>> casByUser = new LinkedHashMap<>();
        casByUser.put("user1", asList(jcasA.getCas()));
        casByUser.put("user2", asList(jcasB.getCas()));

        JCas curatorCas = createJCas(createMultiLinkWithRoleTestTypeSytem("f1"));
        curatorCas.setDocumentText(casByUser.values().stream().flatMap(Collection::stream)
                .findFirst().get().getDocumentText());

        entryTypes.add(HOST_TYPE);

        SpanDiffAdapter adapter = new SpanDiffAdapter(HOST_TYPE);
        adapter.addLinkFeature("links", "role", "target");
        List<? extends DiffAdapter> diffAdapters = asList(adapter);

        DiffResult result = doDiff(entryTypes, diffAdapters, LINK_TARGET_AS_LABEL, casByUser);

        result.print(System.out);

        sut.reMergeCas(result, document, null, curatorCas.getCas(), getSingleCasByUser(casByUser));

        casByUser = new HashMap<>();
        casByUser.put("actual", asList(jcasA.getCas()));
        casByUser.put("merge", asList(curatorCas.getCas()));

        result = doDiff(entryTypes, diffAdapters, LINK_TARGET_AS_LABEL, casByUser);

        assertEquals(0, result.getDifferingConfigurationSets().size());
        assertEquals(0, result.getIncompleteConfigurationSets().size());
    }

    @Test
    public void multiLinkWithRoleLabelDifferenceTest()
        throws Exception
    {
        JCas jcasA = createJCas(createMultiLinkWithRoleTestTypeSytem("f1"));
        makeLinkHostFS(jcasA, 0, 0, makeLinkFS(jcasA, "slot1", 0, 0));

        JCas jcasB = createJCas(createMultiLinkWithRoleTestTypeSytem("f1"));
        makeLinkHostFS(jcasB, 0, 0, makeLinkFS(jcasB, "slot2", 0, 0));

        Map<String, List<CAS>> casByUser = new LinkedHashMap<>();
        casByUser.put("user1", asList(jcasA.getCas()));
        casByUser.put("user2", asList(jcasB.getCas()));

        JCas curatorCas = createJCas(createMultiLinkWithRoleTestTypeSytem("f1"));
        curatorCas.setDocumentText(casByUser.values().stream().flatMap(Collection::stream)
                .findFirst().get().getDocumentText());

        entryTypes.add(HOST_TYPE);

        SpanDiffAdapter adapter = new SpanDiffAdapter(HOST_TYPE);
        adapter.addLinkFeature("links", "role", "target");
        List<? extends DiffAdapter> diffAdapters = asList(adapter);

        DiffResult result = doDiff(entryTypes, diffAdapters, LINK_TARGET_AS_LABEL, casByUser);

        result.print(System.out);

        sut.reMergeCas(result, document, null, curatorCas.getCas(), getSingleCasByUser(casByUser));

        Type hostType = curatorCas.getCas().getTypeSystem().getType(HOST_TYPE);
        FeatureSupport slotSupport = featureSupportRegistry.getFeatureSupport(slotFeature);
        
        assertThat(select(curatorCas.getCas(), hostType))
                .hasSize(1);

        assertThat(select(curatorCas.getCas(), hostType).stream()
                .map(host -> (List) slotSupport.getFeatureValue(slotFeature, host)))
                .allMatch(Collection::isEmpty);
    }

    @Test
    public void multiLinkWithRoleTargetDifferenceTest()
        throws Exception
    {
        JCas jcasA = createJCas(createMultiLinkWithRoleTestTypeSytem("f1"));
        makeLinkHostFS(jcasA, 0, 0, makeLinkFS(jcasA, "slot1", 0, 0));

        JCas jcasB = createJCas(createMultiLinkWithRoleTestTypeSytem("f1"));
        makeLinkHostFS(jcasB, 0, 0, makeLinkFS(jcasB, "slot1", 10, 10));

        Map<String, List<CAS>> casByUser = new LinkedHashMap<>();
        casByUser.put("user1", asList(jcasA.getCas()));
        casByUser.put("user2", asList(jcasB.getCas()));

        JCas curatorCas = createJCas(createMultiLinkWithRoleTestTypeSytem("f1"));
        curatorCas.setDocumentText(casByUser.values().stream().flatMap(Collection::stream)
                .findFirst().get().getDocumentText());

        entryTypes.add(HOST_TYPE);

        SpanDiffAdapter adapter = new SpanDiffAdapter(HOST_TYPE);
        adapter.addLinkFeature("links", "role", "target");
        List<? extends DiffAdapter> diffAdapters = asList(adapter);

        DiffResult result = doDiff(entryTypes, diffAdapters, LINK_TARGET_AS_LABEL, casByUser);

        result.print(System.out);

        sut.reMergeCas(result, document, null, curatorCas.getCas(), getSingleCasByUser(casByUser));

        Type hostType = curatorCas.getCas().getTypeSystem().getType(HOST_TYPE);
        FeatureSupport slotSupport = featureSupportRegistry.getFeatureSupport(slotFeature);
        
        assertThat(select(curatorCas.getCas(), hostType))
                .hasSize(1);

        assertThat(select(curatorCas.getCas(), hostType).stream()
                .map(host -> (List) slotSupport.getFeatureValue(slotFeature, host)))
                .allMatch(Collection::isEmpty);
    }

    @Test
    public void multiLinkMultiHostTest()
        throws Exception
    {
        // Creating two span stacked annotations. This should cause the data not to be merged.
        JCas jcasA = createJCas(createMultiLinkWithRoleTestTypeSytem("f1"));
        makeLinkHostFS(jcasA, 0, 0, makeLinkFS(jcasA, "slot1", 0, 0));
        makeLinkHostFS(jcasA, 0, 0, makeLinkFS(jcasA, "slot1", 0, 0));

        JCas jcasB = createJCas(createMultiLinkWithRoleTestTypeSytem("f1"));
        makeLinkHostFS(jcasB, 0, 0, makeLinkFS(jcasB, "slot1", 0, 0));

        Map<String, List<CAS>> casByUser = new LinkedHashMap<>();
        casByUser.put("user1", asList(jcasA.getCas()));
        casByUser.put("user2", asList(jcasB.getCas()));

        CAS curatorCas = createJCas(createMultiLinkWithRoleTestTypeSytem("f1")).getCas();
        curatorCas.setDocumentText(casByUser.values().stream().flatMap(Collection::stream)
                .findFirst().get().getDocumentText());

        entryTypes.add(HOST_TYPE);

        SpanDiffAdapter adapter = new SpanDiffAdapter(HOST_TYPE);
        adapter.addLinkFeature("links", "role", "target");
        
        DiffResult result = doDiff(entryTypes, asList(adapter), LINK_TARGET_AS_LABEL, casByUser);

        result.print(System.out);

        sut.reMergeCas(result, document, null, curatorCas, getSingleCasByUser(casByUser));

        assertThat(select(curatorCas, getType(curatorCas, HOST_TYPE)))
                .isEmpty();
    }

    @Test
    public void multiLinkMultiSpanRoleDiffTest()
        throws Exception
    {
        JCas jcasA = createJCas(createMultiLinkWithRoleTestTypeSytem("f1"));
        Type type = jcasA.getTypeSystem().getType(HOST_TYPE);
        Feature feature = type.getFeatureByBaseName("f1");

        makeLinkHostMultiSPanFeatureFS(jcasA, 0, 0, feature, "A",
                makeLinkFS(jcasA, "slot1", 0, 0));

        JCas jcasB = createJCas(createMultiLinkWithRoleTestTypeSytem("f1"));
        makeLinkHostMultiSPanFeatureFS(jcasB, 0, 0, feature, "A",
                makeLinkFS(jcasB, "slot2", 0, 0));

        Map<String, List<CAS>> casByUser = new LinkedHashMap<>();
        casByUser.put("user1", asList(jcasA.getCas()));
        casByUser.put("user2", asList(jcasB.getCas()));

        JCas curatorCas = createJCas(createMultiLinkWithRoleTestTypeSytem("f1"));
        curatorCas.setDocumentText(casByUser.values().stream().flatMap(Collection::stream)
                .findFirst().get().getDocumentText());

        entryTypes.add(HOST_TYPE);

        SpanDiffAdapter adapter = new SpanDiffAdapter(HOST_TYPE);
        adapter.addLinkFeature("links", "role", "target");
        List<? extends DiffAdapter> diffAdapters = asList(adapter);

        DiffResult result = doDiff(entryTypes, diffAdapters, LINK_TARGET_AS_LABEL, casByUser);

        result.print(System.out);

        sut.reMergeCas(result, document, null, curatorCas.getCas(), getSingleCasByUser(casByUser));

        Type hostType = curatorCas.getTypeSystem().getType(HOST_TYPE);

        assertThat(select(curatorCas.getCas(), hostType))
                .hasSize(1);
    }

    private Map<String, CAS> getSingleCasByUser(Map<String, List<CAS>> aCasByUserSingle)
    {
        Map<String, CAS> casByUserSingle = new HashMap<>();
        for (String user : aCasByUserSingle.keySet()) {
            casByUserSingle.put(user, aCasByUserSingle.get(user).get(0));
        }

        return casByUserSingle;
    }

    @Test
    public void simpleCopyToEmptyTest()
        throws Exception
    {
        AnnotatorState state = new AnnotatorStateImpl(CURATION);
        state.setUser(new User());
        
        CAS jcas = createJCas().getCas();
        AnnotationFS clickedFs = createNEAnno(jcas, "NN", 0, 0);

        CAS curatorCas = createJCas().getCas();
        createTokenAnno(curatorCas, 0, 0);

        sut.mergeSpanAnnotation(null, null, neLayer, curatorCas, clickedFs, false);

        assertThat(selectCovered(curatorCas, getType(curatorCas, NamedEntity.class), 0, 0))
                .hasSize(1);
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
                .isThrownBy(() -> sut.mergeSpanAnnotation(null, null, posLayer, mergeCas, 
                        clickedFs, false))
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

        sut.mergeSpanAnnotation(null, null, posLayer, mergeCAs, clickedFs, false);

        assertEquals(1, CasUtil.selectCovered(mergeCAs, type, 0, 0).size());
    }

    @Test
    public void simpleCopyToDiffExistingAnnoWithStackingTest()
        throws Exception
    {
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

        sut.mergeSpanAnnotation(null, null, neLayer, mergeCAs, clickedFs, true);

        assertEquals(2, selectCovered(mergeCAs, type, 0, 0).size());
    }

    @Test
    public void copySpanWithSlotNoStackingTest()
        throws Exception
    {
        slotLayer.setOverlapMode(OverlapMode.NO_OVERLAP);
        
        JCas jcasA = createJCas(CurationTestUtils.createMultiLinkWithRoleTestTypeSytem("f1"));
        Type type = jcasA.getTypeSystem().getType(CurationTestUtils.HOST_TYPE);
        Feature feature = type.getFeatureByBaseName("f1");

        AnnotationFS clickedFs = CurationTestUtils.makeLinkHostMultiSPanFeatureFS(jcasA, 0, 0, feature, "A",
                CurationTestUtils.makeLinkFS(jcasA, "slot1", 0, 0));

        JCas mergeCAs = JCasFactory
                .createJCas(CurationTestUtils.createMultiLinkWithRoleTestTypeSytem("f1"));

        CurationTestUtils.makeLinkHostMultiSPanFeatureFS(mergeCAs, 0, 0, feature, "C",
                CurationTestUtils.makeLinkFS(mergeCAs, "slot1", 0, 0));

        sut.mergeSpanAnnotation(null, null, slotLayer, mergeCAs.getCas(), clickedFs, false);

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
        
        JCas jcasA = createJCas(CurationTestUtils.createMultiLinkWithRoleTestTypeSytem("f1"));
        Type type = jcasA.getTypeSystem().getType(CurationTestUtils.HOST_TYPE);
        Feature feature = type.getFeatureByBaseName("f1");

        AnnotationFS clickedFs = CurationTestUtils.makeLinkHostMultiSPanFeatureFS(jcasA, 0, 0, feature, "A",
                CurationTestUtils.makeLinkFS(jcasA, "slot1", 0, 0));

        JCas mergeCAs = JCasFactory
                .createJCas(CurationTestUtils.createMultiLinkWithRoleTestTypeSytem("f1"));

        CurationTestUtils.makeLinkHostMultiSPanFeatureFS(mergeCAs, 0, 0, feature, "C",
                CurationTestUtils.makeLinkFS(mergeCAs, "slot1", 0, 0));

        sut.mergeSpanAnnotation(null, null, slotLayer, mergeCAs.getCas(), clickedFs, true);

        assertEquals(2, selectCovered(mergeCAs.getCas(), type, 0, 0).size());
    }

    @Test
    public void copyLinkToEmptyTest()
        throws Exception
    {
        JCas mergeCas = createJCas(CurationTestUtils.createMultiLinkWithRoleTestTypeSytem("f1"));
        Type type = mergeCas.getTypeSystem().getType(CurationTestUtils.HOST_TYPE);
        Feature feature = type.getFeatureByBaseName("f1");

        AnnotationFS mergeFs = makeLinkHostMultiSPanFeatureFS(mergeCas, 0, 0, feature, "A");

        FeatureStructure copyFS = CurationTestUtils.makeLinkFS(mergeCas, "slot1", 0, 0);

        List<FeatureStructure> linkFs = new ArrayList<>();
        linkFs.add(copyFS);
        WebAnnoCasUtil.setLinkFeatureValue(mergeFs, type.getFeatureByBaseName("links"), linkFs);

        JCas jcasA = createJCas(CurationTestUtils.createMultiLinkWithRoleTestTypeSytem("f1"));
        makeLinkHostMultiSPanFeatureFS(jcasA, 0, 0, feature, "A",
                makeLinkFS(jcasA, "slot1", 0, 0));

        Map<String, List<CAS>> casByUser = new LinkedHashMap<>();
        casByUser.put("user1", asList(mergeCas.getCas()));
        casByUser.put("user2", asList(jcasA.getCas()));

        List<String> entryTypes = asList(CurationTestUtils.HOST_TYPE);

        CasDiff.SpanDiffAdapter adapter = new CasDiff.SpanDiffAdapter(CurationTestUtils.HOST_TYPE);
        adapter.addLinkFeature("links", "role", "target");
        List<? extends CasDiff.DiffAdapter> diffAdapters = asList(adapter);

        CasDiff.DiffResult diff = CasDiff.doDiff(entryTypes, diffAdapters,
                CasDiff.LinkCompareBehavior.LINK_TARGET_AS_LABEL, casByUser);

        assertEquals(0, diff.getDifferingConfigurationSets().size());
        assertEquals(0, diff.getIncompleteConfigurationSets().size());
    }

    @Test
    public void copyLinkToExistingButDiffLinkTest()
        throws Exception
    {

        JCas mergeCas = JCasFactory
                .createJCas(CurationTestUtils.createMultiLinkWithRoleTestTypeSytem("f1"));
        Type type = mergeCas.getTypeSystem().getType(CurationTestUtils.HOST_TYPE);
        Feature feature = type.getFeatureByBaseName("f1");

        AnnotationFS mergeFs = makeLinkHostMultiSPanFeatureFS(mergeCas, 0, 0, feature,
                "A", makeLinkFS(mergeCas, "slot1", 0, 0));

        FeatureStructure copyFS = makeLinkFS(mergeCas, "slot2", 0, 0);

        List<FeatureStructure> linkFs = new ArrayList<>();
        linkFs.add(copyFS);
        WebAnnoCasUtil.setLinkFeatureValue(mergeFs, type.getFeatureByBaseName("links"), linkFs);

        JCas jcasA = createJCas(CurationTestUtils.createMultiLinkWithRoleTestTypeSytem("f1"));
        makeLinkHostMultiSPanFeatureFS(jcasA, 0, 0, feature, "A",
                makeLinkFS(jcasA, "slot1", 0, 0));

        Map<String, List<CAS>> casByUser = new LinkedHashMap<>();
        casByUser.put("user1", asList(mergeCas.getCas()));
        casByUser.put("user2", asList(jcasA.getCas()));

        List<String> entryTypes = asList(CurationTestUtils.HOST_TYPE);

        CasDiff.SpanDiffAdapter adapter = new CasDiff.SpanDiffAdapter(CurationTestUtils.HOST_TYPE);
        adapter.addLinkFeature("links", "role", "target");
        List<? extends CasDiff.DiffAdapter> diffAdapters = asList(adapter);

        CasDiff.DiffResult diff = CasDiff.doDiff(entryTypes, diffAdapters,
                CasDiff.LinkCompareBehavior.LINK_TARGET_AS_LABEL, casByUser);

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

        sut.mergeRelationAnnotation(null, null, depLayer, mergeCAs, clickedFs, false);
        
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
                .isThrownBy(() -> sut.mergeRelationAnnotation(null, null, depLayer, mergeCAs, 
                        clickedFs, false))
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
                .isThrownBy(() -> sut.mergeRelationAnnotation(null, null, depLayer, mergeCAs, 
                        clickedFs, false))
                .withMessageContaining("annotation already exists");
    }
    
    private void assertMatch(JCas curatorCas)
        throws Exception
    {
        String referenceFolder = "src/test/resources/testsuite/" + testContext.getMethodName();
        File targetFolder = testContext.getTestOutputFolder();
        
        DocumentMetaData dmd = DocumentMetaData.get(curatorCas);
        dmd.setDocumentId("curator");
        runPipeline(curatorCas, createEngineDescription(WebannoTsv3XWriter.class,
                WebannoTsv3XWriter.PARAM_TARGET_LOCATION, targetFolder,
                WebannoTsv3XWriter.PARAM_OVERWRITE, true));
        
        File referenceFile = new File(referenceFolder, "curator.tsv");
        assumeTrue("No reference data available for this test.", referenceFile.exists());
        
        File actualFile = new File(targetFolder, "curator.tsv");
        
        String reference = FileUtils.readFileToString(referenceFile, "UTF-8");
        String actual = FileUtils.readFileToString(actualFile, "UTF-8");
        
        assertEquals(reference, actual);
    }
    
    @Rule
    public DkproTestContext testContext = new DkproTestContext();
}

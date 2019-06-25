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
package de.tudarmstadt.ukp.clarin.webanno.curation.casmerge;

import static de.tudarmstadt.ukp.clarin.webanno.api.WebAnnoConst.RELATION_TYPE;
import static de.tudarmstadt.ukp.clarin.webanno.api.WebAnnoConst.SPAN_TYPE;
import static de.tudarmstadt.ukp.clarin.webanno.curation.CurationTestUtils.HOST_TYPE;
import static de.tudarmstadt.ukp.clarin.webanno.curation.casdiff.CasDiff.RelationDiffAdapter.DEPENDENCY_DIFF_ADAPTER;
import static de.tudarmstadt.ukp.clarin.webanno.curation.casdiff.CasDiff.SpanDiffAdapter.NER_DIFF_ADAPTER;
import static de.tudarmstadt.ukp.clarin.webanno.curation.casdiff.CasDiff.SpanDiffAdapter.POS_DIFF_ADAPTER;
import static de.tudarmstadt.ukp.clarin.webanno.model.AnchoringMode.CHARACTERS;
import static de.tudarmstadt.ukp.clarin.webanno.model.AnchoringMode.SINGLE_TOKEN;
import static de.tudarmstadt.ukp.clarin.webanno.model.AnchoringMode.TOKENS;
import static de.tudarmstadt.ukp.clarin.webanno.model.OverlapMode.NO_OVERLAP;
import static de.tudarmstadt.ukp.clarin.webanno.model.OverlapMode.OVERLAP_ONLY;
import static java.util.Arrays.asList;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;

import java.util.ArrayList;
import java.util.List;

import org.apache.uima.cas.CAS;
import org.junit.Before;
import org.mockito.Mock;

import de.tudarmstadt.ukp.clarin.webanno.api.AnnotationSchemaService;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.feature.FeatureSupportRegistryImpl;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.feature.PrimitiveUimaFeatureSupport;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.feature.SlotFeatureSupport;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.layer.ChainLayerSupport;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.layer.LayerBehaviorRegistryImpl;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.layer.LayerSupportRegistryImpl;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.layer.RelationLayerSupport;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.layer.SpanLayerSupport;
import de.tudarmstadt.ukp.clarin.webanno.curation.CurationTestUtils;
import de.tudarmstadt.ukp.clarin.webanno.curation.casdiff.CasDiff.DiffAdapter;
import de.tudarmstadt.ukp.clarin.webanno.curation.casdiff.CasDiff.RelationDiffAdapter;
import de.tudarmstadt.ukp.clarin.webanno.curation.casdiff.CasDiff.SpanDiffAdapter;
import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationFeature;
import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationLayer;
import de.tudarmstadt.ukp.clarin.webanno.model.LinkMode;
import de.tudarmstadt.ukp.clarin.webanno.model.MultiValueMode;
import de.tudarmstadt.ukp.clarin.webanno.model.Project;
import de.tudarmstadt.ukp.clarin.webanno.model.SourceDocument;
import de.tudarmstadt.ukp.dkpro.core.api.lexmorph.type.pos.POS;
import de.tudarmstadt.ukp.dkpro.core.api.ner.type.NamedEntity;
import de.tudarmstadt.ukp.dkpro.core.api.segmentation.type.Sentence;
import de.tudarmstadt.ukp.dkpro.core.api.segmentation.type.Token;
import de.tudarmstadt.ukp.dkpro.core.api.syntax.type.dependency.Dependency;

public class CasMergeTestBase
{
    protected @Mock AnnotationSchemaService schemaService;
    
    protected CasMerge sut;
    
    protected LayerSupportRegistryImpl layerSupportRegistry;
    protected FeatureSupportRegistryImpl featureSupportRegistry;
    protected Project project;
    protected AnnotationLayer sentenceLayer;
    protected AnnotationLayer tokenLayer;
    protected AnnotationFeature tokenPosFeature;
    protected AnnotationLayer posLayer;
    protected AnnotationFeature posFeature;
    protected AnnotationFeature posCoarseFeature;
    protected AnnotationLayer neLayer;
    protected AnnotationFeature neFeature;
    protected AnnotationFeature neIdentifierFeature;
    protected AnnotationLayer depLayer;
    protected AnnotationFeature depFeature;
    protected AnnotationFeature depFlavorFeature;
    protected AnnotationLayer slotLayer;
    protected AnnotationFeature slotFeature;
    protected AnnotationFeature stringFeature;
    protected AnnotationLayer multiValRel;
    protected AnnotationFeature multiValRelRel1;
    protected AnnotationFeature multiValRelRel2;
    protected AnnotationLayer multiValSpan;
    protected AnnotationFeature multiValSpanF1;
    protected AnnotationFeature multiValSpanF2;
    protected SourceDocument document;
    protected List<String> entryTypes;
    protected List<DiffAdapter> diffAdapters;
    
    protected static final RelationDiffAdapter MULTIVALREL_DIFF_ADAPTER = new RelationDiffAdapter(
            "webanno.custom.Multivalrel", "Dependent", "Governor", "rel1", "rel2");
    protected static final SpanDiffAdapter MULTIVALSPAN_DIFF_ADAPTER = new SpanDiffAdapter(
            "webanno.custom.Multivalspan", "f1", "f2");
    
    @Before
    public void setup() throws Exception
    {
        initMocks(this);
        
        
        SpanDiffAdapter slotHostDiffAdapter = new SpanDiffAdapter(HOST_TYPE);
        slotHostDiffAdapter.addLinkFeature("links", "role", "target");

        diffAdapters = new ArrayList<>();
        diffAdapters.add(POS_DIFF_ADAPTER);
        diffAdapters.add(NER_DIFF_ADAPTER);
        diffAdapters.add(DEPENDENCY_DIFF_ADAPTER);
        diffAdapters.add(MULTIVALREL_DIFF_ADAPTER);
        diffAdapters.add(MULTIVALSPAN_DIFF_ADAPTER);
        diffAdapters.add(slotHostDiffAdapter);
        
        entryTypes = new ArrayList<>();
        entryTypes.add(Sentence.class.getName());
        entryTypes.add(Token.class.getName());
        entryTypes.add(POS.class.getName());
        entryTypes.add(NamedEntity.class.getName());
        entryTypes.add(Dependency.class.getName());
        entryTypes.add(CurationTestUtils.HOST_TYPE);
        entryTypes.add("webanno.custom.Multivalrel");
        entryTypes.add("webanno.custom.Multivalspan");
        
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
}

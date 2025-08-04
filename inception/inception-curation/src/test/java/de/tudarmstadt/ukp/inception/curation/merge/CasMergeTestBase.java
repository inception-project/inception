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
package de.tudarmstadt.ukp.inception.curation.merge;

import static de.tudarmstadt.ukp.clarin.webanno.model.AnchoringMode.CHARACTERS;
import static de.tudarmstadt.ukp.clarin.webanno.model.AnchoringMode.SINGLE_TOKEN;
import static de.tudarmstadt.ukp.clarin.webanno.model.AnchoringMode.TOKENS;
import static de.tudarmstadt.ukp.clarin.webanno.model.OverlapMode.NO_OVERLAP;
import static de.tudarmstadt.ukp.clarin.webanno.model.OverlapMode.OVERLAP_ONLY;
import static de.tudarmstadt.ukp.inception.annotation.feature.link.LinkFeatureDiffMode.EXCLUDE;
import static de.tudarmstadt.ukp.inception.annotation.feature.link.LinkFeatureMultiplicityMode.ONE_TARGET_MULTIPLE_ROLES;
import static de.tudarmstadt.ukp.inception.annotation.layer.relation.curation.RelationDiffAdapterImpl.DEPENDENCY_DIFF_ADAPTER;
import static de.tudarmstadt.ukp.inception.annotation.layer.span.curation.SpanDiffAdapterImpl.NER_DIFF_ADAPTER;
import static de.tudarmstadt.ukp.inception.annotation.layer.span.curation.SpanDiffAdapterImpl.POS_DIFF_ADAPTER;
import static de.tudarmstadt.ukp.inception.curation.merge.CurationTestUtils.HOST_TYPE;
import static java.util.Arrays.asList;
import static org.apache.uima.cas.CAS.TYPE_NAME_STRING;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import de.tudarmstadt.ukp.clarin.webanno.constraints.ConstraintsService;
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
import de.tudarmstadt.ukp.inception.annotation.feature.bool.BooleanFeatureSupport;
import de.tudarmstadt.ukp.inception.annotation.feature.link.LinkFeatureSupport;
import de.tudarmstadt.ukp.inception.annotation.feature.number.NumberFeatureSupport;
import de.tudarmstadt.ukp.inception.annotation.feature.string.StringFeatureSupport;
import de.tudarmstadt.ukp.inception.annotation.layer.behaviors.LayerBehaviorRegistryImpl;
import de.tudarmstadt.ukp.inception.annotation.layer.chain.ChainLayerSupportImpl;
import de.tudarmstadt.ukp.inception.annotation.layer.document.DocumentMetadataLayerSupportImpl;
import de.tudarmstadt.ukp.inception.annotation.layer.document.api.DocumentMetadataLayerSupport;
import de.tudarmstadt.ukp.inception.annotation.layer.document.curation.DocumentMetadataDiffAdapter;
import de.tudarmstadt.ukp.inception.annotation.layer.relation.RelationLayerSupportImpl;
import de.tudarmstadt.ukp.inception.annotation.layer.relation.api.RelationDiffAdapter;
import de.tudarmstadt.ukp.inception.annotation.layer.relation.api.RelationLayerSupport;
import de.tudarmstadt.ukp.inception.annotation.layer.relation.curation.RelationDiffAdapterImpl;
import de.tudarmstadt.ukp.inception.annotation.layer.span.SpanLayerSupportImpl;
import de.tudarmstadt.ukp.inception.annotation.layer.span.api.SpanDiffAdapter;
import de.tudarmstadt.ukp.inception.annotation.layer.span.api.SpanLayerSupport;
import de.tudarmstadt.ukp.inception.annotation.layer.span.curation.SpanDiffAdapterImpl;
import de.tudarmstadt.ukp.inception.curation.api.DiffAdapter;
import de.tudarmstadt.ukp.inception.schema.api.AnnotationSchemaService;
import de.tudarmstadt.ukp.inception.schema.api.feature.FeatureSupportRegistryImpl;
import de.tudarmstadt.ukp.inception.schema.api.layer.LayerSupportRegistryImpl;

@ExtendWith(MockitoExtension.class)
public class CasMergeTestBase
{
    protected static final String DOCUMENT_LABEL_TYPE = "custom.DocumentLabel";

    protected @Mock ConstraintsService constraintsService;
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
    protected AnnotationFeature slotFeature2;
    protected AnnotationFeature stringFeature;
    protected AnnotationLayer multiValRel;
    protected AnnotationFeature multiValRelRel1;
    protected AnnotationFeature multiValRelRel2;
    protected AnnotationLayer multiValSpan;
    protected AnnotationFeature multiValSpanF1;
    protected AnnotationFeature multiValSpanF2;
    protected SourceDocument document;
    protected AnnotationLayer documentLabelLayer;
    protected AnnotationFeature documentLabelLayerFeature;

    protected List<DiffAdapter> diffAdapters;
    protected SpanDiffAdapter slotHostDiffAdapter;
    protected DocumentMetadataDiffAdapter documentLabelDiffAdapter;

    protected static final RelationDiffAdapter MULTIVALREL_DIFF_ADAPTER = new RelationDiffAdapterImpl(
            "webanno.custom.Multivalrel", "Dependent", "Governor", "rel1", "rel2");
    protected static final SpanDiffAdapter MULTIVALSPAN_DIFF_ADAPTER = new SpanDiffAdapterImpl(
            "webanno.custom.Multivalspan", "f1", "f2");

    @BeforeEach
    public void setup() throws Exception
    {
        slotHostDiffAdapter = new SpanDiffAdapterImpl(HOST_TYPE);
        slotHostDiffAdapter.addLinkFeature("links", "role", "target", ONE_TARGET_MULTIPLE_ROLES,
                EXCLUDE);
        slotHostDiffAdapter.addLinkFeature("altLinks", "role", "target", ONE_TARGET_MULTIPLE_ROLES,
                EXCLUDE);

        documentLabelDiffAdapter = new DocumentMetadataDiffAdapter(DOCUMENT_LABEL_TYPE, "label");

        diffAdapters = new ArrayList<>();
        // diffAdapters.add(TOKEN_DIFF_ADAPTER);
        // diffAdapters.add(SENTENCE_DIFF_ADAPTER);
        diffAdapters.add(POS_DIFF_ADAPTER);
        diffAdapters.add(NER_DIFF_ADAPTER);
        diffAdapters.add(DEPENDENCY_DIFF_ADAPTER);
        diffAdapters.add(MULTIVALREL_DIFF_ADAPTER);
        diffAdapters.add(MULTIVALSPAN_DIFF_ADAPTER);
        diffAdapters.add(slotHostDiffAdapter);
        diffAdapters.add(documentLabelDiffAdapter);

        project = new Project();

        document = new SourceDocument();
        document.setProject(project);
        document.setName("document");

        sentenceLayer = new AnnotationLayer(Sentence.class.getName(), "Sentence",
                SpanLayerSupport.TYPE, null, true, CHARACTERS, NO_OVERLAP);

        tokenLayer = new AnnotationLayer(Token.class.getName(), "Token", SpanLayerSupport.TYPE,
                null, true, CHARACTERS, NO_OVERLAP);

        tokenPosFeature = new AnnotationFeature();
        tokenPosFeature.setName("pos");
        tokenPosFeature.setEnabled(true);
        tokenPosFeature.setType(POS.class.getName());
        tokenPosFeature.setUiName("pos");
        tokenPosFeature.setLayer(tokenLayer);
        tokenPosFeature.setProject(project);
        tokenPosFeature.setVisible(true);
        tokenPosFeature.setCuratable(true);

        posLayer = new AnnotationLayer(POS.class.getName(), "POS", SpanLayerSupport.TYPE, project,
                true, SINGLE_TOKEN, NO_OVERLAP);
        posLayer.setAttachType(tokenLayer);
        posLayer.setAttachFeature(tokenPosFeature);

        posFeature = new AnnotationFeature();
        posFeature.setName("PosValue");
        posFeature.setEnabled(true);
        posFeature.setType(TYPE_NAME_STRING);
        posFeature.setUiName("PosValue");
        posFeature.setLayer(posLayer);
        posFeature.setProject(project);
        posFeature.setVisible(true);
        posFeature.setCuratable(true);

        posCoarseFeature = new AnnotationFeature();
        posCoarseFeature.setName("coarseValue");
        posCoarseFeature.setEnabled(true);
        posCoarseFeature.setType(TYPE_NAME_STRING);
        posCoarseFeature.setUiName("coarseValue");
        posCoarseFeature.setLayer(posLayer);
        posCoarseFeature.setProject(project);
        posCoarseFeature.setVisible(true);
        posCoarseFeature.setCuratable(true);

        neLayer = new AnnotationLayer(NamedEntity.class.getName(), "Named Entity",
                SpanLayerSupport.TYPE, project, true, TOKENS, OVERLAP_ONLY);

        neFeature = new AnnotationFeature();
        neFeature.setName("value");
        neFeature.setEnabled(true);
        neFeature.setType(TYPE_NAME_STRING);
        neFeature.setUiName("value");
        neFeature.setLayer(neLayer);
        neFeature.setProject(project);
        neFeature.setVisible(true);
        neFeature.setCuratable(true);

        neIdentifierFeature = new AnnotationFeature();
        neIdentifierFeature.setName("identifier");
        neIdentifierFeature.setEnabled(true);
        neIdentifierFeature.setType(TYPE_NAME_STRING);
        neIdentifierFeature.setUiName("identifier");
        neIdentifierFeature.setLayer(neLayer);
        neIdentifierFeature.setProject(project);
        neIdentifierFeature.setVisible(true);
        neIdentifierFeature.setCuratable(true);

        depLayer = new AnnotationLayer(Dependency.class.getName(), "Dependency",
                RelationLayerSupport.TYPE, project, true, SINGLE_TOKEN, OVERLAP_ONLY);
        depLayer.setAttachType(tokenLayer);
        depLayer.setAttachFeature(tokenPosFeature);

        depFeature = new AnnotationFeature();
        depFeature.setName("DependencyType");
        depFeature.setEnabled(true);
        depFeature.setType(TYPE_NAME_STRING);
        depFeature.setUiName("Relation");
        depFeature.setLayer(depLayer);
        depFeature.setProject(project);
        depFeature.setVisible(true);
        depFeature.setCuratable(true);

        depFlavorFeature = new AnnotationFeature();
        depFlavorFeature.setName("flavor");
        depFlavorFeature.setEnabled(true);
        depFlavorFeature.setType(TYPE_NAME_STRING);
        depFlavorFeature.setUiName("flavor");
        depFlavorFeature.setLayer(depLayer);
        depFlavorFeature.setProject(project);
        depFlavorFeature.setVisible(true);
        depFlavorFeature.setCuratable(true);

        slotLayer = new AnnotationLayer(HOST_TYPE, HOST_TYPE, SpanLayerSupport.TYPE, project, false,
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
        slotFeature.setCuratable(true);

        slotFeature2 = new AnnotationFeature();
        slotFeature2.setName("altLinks");
        slotFeature2.setEnabled(true);
        slotFeature2.setType(Token.class.getName());
        slotFeature2.setMode(MultiValueMode.ARRAY);
        slotFeature2.setLinkMode(LinkMode.WITH_ROLE);
        slotFeature2.setLinkTypeName(CurationTestUtils.LINK_TYPE);
        slotFeature2.setLinkTypeRoleFeatureName("role");
        slotFeature2.setLinkTypeTargetFeatureName("target");
        slotFeature2.setUiName("links");
        slotFeature2.setLayer(slotLayer);
        slotFeature2.setProject(project);
        slotFeature2.setVisible(true);
        slotFeature2.setCuratable(true);

        stringFeature = new AnnotationFeature();
        stringFeature.setName("f1");
        stringFeature.setEnabled(true);
        stringFeature.setType(TYPE_NAME_STRING);
        stringFeature.setUiName("f1");
        stringFeature.setLayer(slotLayer);
        stringFeature.setProject(project);
        stringFeature.setVisible(true);
        stringFeature.setCuratable(true);

        multiValSpan = new AnnotationLayer("webanno.custom.Multivalspan", "Multivalspan",
                SpanLayerSupport.TYPE, project, true, TOKENS, OVERLAP_ONLY);

        multiValSpanF1 = new AnnotationFeature();
        multiValSpanF1.setName("f1");
        multiValSpanF1.setEnabled(true);
        multiValSpanF1.setType(TYPE_NAME_STRING);
        multiValSpanF1.setUiName("f1");
        multiValSpanF1.setLayer(multiValSpan);
        multiValSpanF1.setProject(project);
        multiValSpanF1.setVisible(true);
        multiValSpanF1.setCuratable(true);

        multiValSpanF2 = new AnnotationFeature();
        multiValSpanF2.setName("f2");
        multiValSpanF2.setEnabled(true);
        multiValSpanF2.setType(TYPE_NAME_STRING);
        multiValSpanF2.setUiName("f2");
        multiValSpanF2.setLayer(multiValSpan);
        multiValSpanF2.setProject(project);
        multiValSpanF2.setVisible(true);
        multiValSpanF2.setCuratable(true);

        multiValRel = new AnnotationLayer("webanno.custom.Multivalrel", "Multivalrel",
                RelationLayerSupport.TYPE, project, true, SINGLE_TOKEN, OVERLAP_ONLY);
        multiValRel.setAttachType(multiValSpan);

        multiValRelRel1 = new AnnotationFeature();
        multiValRelRel1.setName("rel1");
        multiValRelRel1.setEnabled(true);
        multiValRelRel1.setType(TYPE_NAME_STRING);
        multiValRelRel1.setUiName("rel1");
        multiValRelRel1.setLayer(multiValSpan);
        multiValRelRel1.setProject(project);
        multiValRelRel1.setVisible(true);
        multiValRelRel1.setCuratable(true);

        multiValRelRel2 = new AnnotationFeature();
        multiValRelRel2.setName("rel2");
        multiValRelRel2.setEnabled(true);
        multiValRelRel2.setType(TYPE_NAME_STRING);
        multiValRelRel2.setUiName("rel2");
        multiValRelRel2.setLayer(multiValSpan);
        multiValRelRel2.setProject(project);
        multiValRelRel2.setVisible(true);
        multiValRelRel2.setCuratable(true);

        documentLabelLayer = AnnotationLayer.builder() //
                .withName(DOCUMENT_LABEL_TYPE) //
                .withType(DocumentMetadataLayerSupport.TYPE) // )
                .build();

        documentLabelLayerFeature = new AnnotationFeature();
        documentLabelLayerFeature.setName("label");
        documentLabelLayerFeature.setEnabled(true);
        documentLabelLayerFeature.setType(TYPE_NAME_STRING);
        documentLabelLayerFeature.setUiName("label");
        documentLabelLayerFeature.setLayer(documentLabelLayer);
        documentLabelLayerFeature.setProject(project);
        documentLabelLayerFeature.setVisible(true);
        documentLabelLayerFeature.setCuratable(true);

        lenient().when(schemaService.findLayer(any(Project.class), any(String.class)))
                .thenAnswer(call -> {
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
                    if (type.equals(DOCUMENT_LABEL_TYPE)) {
                        return documentLabelLayer;
                    }
                    if (type.equals("webanno.custom.Multivalrel")) {
                        return multiValRel;
                    }
                    if (type.equals("webanno.custom.Multivalspan")) {
                        return multiValSpan;
                    }
                    throw new IllegalStateException("Unknown layer type: " + type);
                });

        lenient().when(schemaService.listSupportedFeatures((any(AnnotationLayer.class))))
                .thenAnswer(call -> schemaService
                        .listAnnotationFeature(call.getArgument(0, AnnotationLayer.class)));

        lenient().when(schemaService.listAnnotationFeature(any(AnnotationLayer.class)))
                .thenAnswer(call -> {
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
                        return asList(slotFeature, slotFeature2, stringFeature);
                    }
                    if (type.getName().equals(DOCUMENT_LABEL_TYPE)) {
                        return asList(documentLabelLayerFeature);
                    }
                    if (type.getName().equals("webanno.custom.Multivalrel")) {
                        return asList(multiValRelRel1, multiValRelRel2);
                    }
                    if (type.getName().equals("webanno.custom.Multivalspan")) {
                        return asList(multiValSpanF1, multiValSpanF2);
                    }
                    throw new IllegalStateException("Unknown layer type: " + type.getName());
                });

        lenient().when(schemaService.getAdapter(any(AnnotationLayer.class))).thenAnswer(call -> {
            AnnotationLayer type = call.getArgument(0, AnnotationLayer.class);
            return layerSupportRegistry.getLayerSupport(type).createAdapter(type,
                    () -> schemaService.listAnnotationFeature(type));
        });

        featureSupportRegistry = new FeatureSupportRegistryImpl(
                asList(new StringFeatureSupport(), new BooleanFeatureSupport(),
                        new NumberFeatureSupport(), new LinkFeatureSupport(schemaService)));
        featureSupportRegistry.init();

        var layerBehaviorRegistry = new LayerBehaviorRegistryImpl(asList());
        layerBehaviorRegistry.init();

        layerSupportRegistry = new LayerSupportRegistryImpl(asList(
                new SpanLayerSupportImpl(featureSupportRegistry, null, layerBehaviorRegistry,
                        constraintsService),
                new RelationLayerSupportImpl(featureSupportRegistry, null, layerBehaviorRegistry,
                        constraintsService),
                new ChainLayerSupportImpl(featureSupportRegistry, null, layerBehaviorRegistry,
                        constraintsService),
                new DocumentMetadataLayerSupportImpl(featureSupportRegistry, null, null,
                        layerBehaviorRegistry, constraintsService)));
        layerSupportRegistry.init();

        sut = new CasMerge(schemaService, null);
    }
}

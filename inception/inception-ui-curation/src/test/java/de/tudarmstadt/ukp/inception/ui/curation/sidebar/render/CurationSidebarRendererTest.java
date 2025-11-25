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
package de.tudarmstadt.ukp.inception.ui.curation.sidebar.render;

import static de.tudarmstadt.ukp.clarin.webanno.model.LinkMode.WITH_ROLE;
import static de.tudarmstadt.ukp.clarin.webanno.model.MultiValueMode.ARRAY;
import static de.tudarmstadt.ukp.inception.support.WebAnnoConst.CURATION_USER;
import static java.util.Arrays.asList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.lang.invoke.MethodHandles;
import java.util.ArrayList;
import java.util.List;

import org.apache.uima.cas.CAS;
import org.apache.uima.cas.text.AnnotationFS;
import org.apache.uima.fit.factory.CasFactory;
import org.apache.uima.resource.metadata.impl.TypeSystemDescription_impl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.tudarmstadt.ukp.clarin.webanno.constraints.ConstraintsService;
import de.tudarmstadt.ukp.clarin.webanno.curation.casdiff.DiffAdapterRegistryImpl;
import de.tudarmstadt.ukp.clarin.webanno.curation.casdiff.DiffSupportRegistryImpl;
import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationFeature;
import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationLayer;
import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationSet;
import de.tudarmstadt.ukp.clarin.webanno.model.OverlapMode;
import de.tudarmstadt.ukp.clarin.webanno.model.Project;
import de.tudarmstadt.ukp.clarin.webanno.model.SourceDocument;
import de.tudarmstadt.ukp.clarin.webanno.security.UserDao;
import de.tudarmstadt.ukp.clarin.webanno.security.model.User;
import de.tudarmstadt.ukp.inception.annotation.feature.bool.BooleanFeatureSupport;
import de.tudarmstadt.ukp.inception.annotation.feature.link.LinkFeatureMultiplicityMode;
import de.tudarmstadt.ukp.inception.annotation.feature.link.LinkFeatureSupport;
import de.tudarmstadt.ukp.inception.annotation.feature.link.LinkFeatureTraits;
import de.tudarmstadt.ukp.inception.annotation.feature.number.NumberFeatureSupport;
import de.tudarmstadt.ukp.inception.annotation.feature.string.StringFeatureSupport;
import de.tudarmstadt.ukp.inception.annotation.layer.behaviors.LayerBehaviorRegistryImpl;
import de.tudarmstadt.ukp.inception.annotation.layer.relation.RelationLayerSupportImpl;
import de.tudarmstadt.ukp.inception.annotation.layer.relation.api.RelationLayerSupport;
import de.tudarmstadt.ukp.inception.annotation.layer.relation.curation.RelationDiffSupport;
import de.tudarmstadt.ukp.inception.annotation.layer.span.SpanLayerSupportImpl;
import de.tudarmstadt.ukp.inception.annotation.layer.span.api.SpanAdapter;
import de.tudarmstadt.ukp.inception.annotation.layer.span.api.SpanLayerSupport;
import de.tudarmstadt.ukp.inception.annotation.layer.span.curation.SpanDiffSupport;
import de.tudarmstadt.ukp.inception.curation.api.DiffAdapterRegistry;
import de.tudarmstadt.ukp.inception.documents.api.DocumentService;
import de.tudarmstadt.ukp.inception.rendering.request.RenderRequest;
import de.tudarmstadt.ukp.inception.rendering.vmodel.VDocument;
import de.tudarmstadt.ukp.inception.rendering.vmodel.VID;
import de.tudarmstadt.ukp.inception.schema.api.AnnotationSchemaService;
import de.tudarmstadt.ukp.inception.schema.api.feature.FeatureSupportRegistryImpl;
import de.tudarmstadt.ukp.inception.schema.api.feature.LinkWithRoleModel;
import de.tudarmstadt.ukp.inception.schema.api.layer.LayerSupportRegistryImpl;
import de.tudarmstadt.ukp.inception.support.json.JSONUtil;
import de.tudarmstadt.ukp.inception.ui.curation.sidebar.CurationSidebarService;

@ExtendWith(MockitoExtension.class)
class CurationSidebarRendererTest
{
    private static final Logger LOG = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    private @Mock ConstraintsService constraintsService;
    private @Mock CurationSidebarService curationService;
    private @Mock DocumentService documentService;
    private @Mock UserDao userRepository;
    private @Mock AnnotationSchemaService schemaService;

    private FeatureSupportRegistryImpl featureSupportRegistry;
    private LayerBehaviorRegistryImpl layerBehaviorRegistry;
    private LayerSupportRegistryImpl layerSupportRegistry;

    private CurationSidebarRenderer sut;

    private Project project;
    private SourceDocument doc;
    private VDocument vdoc;

    private User curator;
    private CAS curatorCas;

    private User anno1;
    private CAS anno1Cas;

    private User anno2;
    private CAS anno2Cas;

    private AnnotationLayer spanLayer;
    private AnnotationLayer relationLayer;
    private AnnotationFeature spanLayerLinkFeature;
    private SpanAdapter spanLayerAdapter;
    private DiffSupportRegistryImpl diffSupportRegistry;
    private DiffAdapterRegistry diffAdapterRegistry;

    @BeforeEach
    void setup() throws Exception
    {
        featureSupportRegistry = new FeatureSupportRegistryImpl(asList( //
                new StringFeatureSupport(), //
                new BooleanFeatureSupport(), //
                new NumberFeatureSupport(), //
                new LinkFeatureSupport(schemaService)));
        featureSupportRegistry.init();

        layerBehaviorRegistry = new LayerBehaviorRegistryImpl(asList());
        layerBehaviorRegistry.init();

        layerSupportRegistry = new LayerSupportRegistryImpl(asList( //
                new SpanLayerSupportImpl(featureSupportRegistry, null, layerBehaviorRegistry,
                        constraintsService), //
                new RelationLayerSupportImpl(featureSupportRegistry, null, layerBehaviorRegistry,
                        constraintsService)));
        layerSupportRegistry.init();

        diffSupportRegistry = new DiffSupportRegistryImpl(asList( //
                new SpanDiffSupport(), //
                new RelationDiffSupport(schemaService)));
        diffSupportRegistry.init();

        diffAdapterRegistry = new DiffAdapterRegistryImpl(schemaService, diffSupportRegistry);

        sut = new CurationSidebarRenderer(curationService, layerSupportRegistry, documentService,
                userRepository, schemaService, diffAdapterRegistry);

        curator = User.builder() //
                .withUsername(CURATION_USER) //
                .build();

        anno1 = User.builder() //
                .withUsername("anno1") //
                .build();

        anno2 = User.builder() //
                .withUsername("anno2") //
                .build();

        project = Project.builder() //
                .withId(1l) //
                .withName("project") //
                .build();

        doc = SourceDocument.builder() //
                .withId(1l) //
                .withProject(project) //
                .withName("document") //
                .build();

        spanLayer = AnnotationLayer.builder() //
                .withId(1l) //
                .withProject(project) //
                .withName("custom.Span") //
                .withType(SpanLayerSupport.TYPE) //
                .build();

        spanLayerLinkFeature = AnnotationFeature.builder() //
                .withId(1l) //
                .withProject(project) //
                .withLayer(spanLayer) //
                .withName("links") //
                .withType(spanLayer.getName()) //
                .withLinkMode(WITH_ROLE) //
                .withLinkTypeName(spanLayer.getName() + "Link") //
                .withLinkTypeRoleFeatureName("role") //
                .withLinkTypeTargetFeatureName("target") //
                .withMultiValueMode(ARRAY) //
                .build();

        relationLayer = AnnotationLayer.builder() //
                .withId(2l) //
                .withProject(project) //
                .withName("custom.Relation") //
                .withType(RelationLayerSupport.TYPE) //
                .build();

        when(userRepository.getCurrentUsername()).thenReturn(curator.getUsername());

        when(curationService.listUsersReadyForCuration(curator.getUsername(), project, doc))
                .thenReturn(asList(anno1, anno2));

        var allFeaturesInProject = asList(spanLayerLinkFeature);
        when(schemaService.getAdapter(any())).thenAnswer(call -> {
            var layer = call.getArgument(0, AnnotationLayer.class);
            var support = layerSupportRegistry.findExtension(layer).get();
            return support.createAdapter(layer, () -> allFeaturesInProject);
        });
        when(schemaService.listSupportedFeatures(any(Project.class)))
                .thenReturn(allFeaturesInProject);
        when(schemaService.listAnnotationFeature(any(Project.class)))
                .thenReturn(allFeaturesInProject);
    }

    static List<Arguments> combinations()
    {
        var combinations = new ArrayList<Arguments>();
        for (var overlapMode : OverlapMode.values()) {
            for (var linkFeatureMultiplicityMode : LinkFeatureMultiplicityMode.values()) {
                combinations.add(Arguments.of(overlapMode, linkFeatureMultiplicityMode));
            }
        }
        return combinations;
    }

    /**
     * The two spans have the same type, position and value. Thus, when rendering them, they should
     * be conflated, even though they are from the same annotator.
     */
    @ParameterizedTest
    @MethodSource("combinations")
    void thatEquivalentStackedSpansFromSingleAnnotatorAreConflated(OverlapMode aOverlapMode,
            LinkFeatureMultiplicityMode aLinkFeatureMultiplicityMode)
        throws Exception
    {
        tuneLayers(aOverlapMode, aLinkFeatureMultiplicityMode);
        var request = renderRequest("anchor");

        var anchorA = spanLayerAdapter.add(doc, anno1.getUsername(), anno1Cas, 0, 6);
        var anchorB = spanLayerAdapter.add(doc, anno1.getUsername(), anno1Cas, 0, 6);

        sut.render(vdoc, request);

        assertThat(vdoc.spans()) //
                .extracting(span -> span.getVid()) //
                .containsExactlyInAnyOrder( //
                        curationVid(anno1, anchorB));
    }

    /**
     * The two spans have the same type, position and value. They should be conflated across
     * annotators.
     */
    @ParameterizedTest
    @MethodSource("combinations")
    void thatEquivalentStackedSpansFromTwoAnnotatorAreConflated(OverlapMode aOverlapMode,
            LinkFeatureMultiplicityMode aLinkFeatureMultiplicityMode)
        throws Exception
    {
        tuneLayers(aOverlapMode, aLinkFeatureMultiplicityMode);
        var request = renderRequest("anchor");

        var anchorA = spanLayerAdapter.add(doc, anno1.getUsername(), anno1Cas, 0, 6);
        var anchorB = spanLayerAdapter.add(doc, anno2.getUsername(), anno2Cas, 0, 6);

        sut.render(vdoc, request);

        assertThat(vdoc.spans()) //
                .extracting(span -> span.getVid()) //
                .containsExactlyInAnyOrder( //
                        curationVid(anno1, anchorB));
        assertThat(vdoc.comments()) //
                .extracting(comment -> comment.getComment()) //
                .containsExactlyInAnyOrder("Annotators: anno1, anno2");
    }

    /**
     * A span annotation in the curator's CAS hides the spans from the annotators.
     */
    @ParameterizedTest
    @MethodSource("combinations")
    void thatEquivalentSpanFromCuratorHidesSpansFromAnnotators(OverlapMode aOverlapMode,
            LinkFeatureMultiplicityMode aLinkFeatureMultiplicityMode)
        throws Exception
    {
        tuneLayers(aOverlapMode, aLinkFeatureMultiplicityMode);
        var request = renderRequest("anchor");

        var anchorA = spanLayerAdapter.add(doc, anno1.getUsername(), anno1Cas, 0, 6);
        var anchorB = spanLayerAdapter.add(doc, anno2.getUsername(), anno2Cas, 0, 6);
        var anchorC = spanLayerAdapter.add(doc, curator.getUsername(), curatorCas, 0, 6);

        sut.render(vdoc, request);

        // Yes, empty because normally the curator's span would be rendered by the regular
        // rendering pipeline which we do not have here. The sidebar render only needs to
        // suppress its own rendering in this case.
        assertThat(vdoc.spans()).isEmpty();
    }

    /**
     * An overlapping span annotation in the curator's CAS hides the spans from the annotators.
     */
    @ParameterizedTest
    @MethodSource("combinations")
    void thatOverlappingEquivalentSpanFromCuratorHidesSpansFromAnnotators(OverlapMode aOverlapMode,
            LinkFeatureMultiplicityMode aLinkFeatureMultiplicityMode)
        throws Exception
    {
        tuneLayers(aOverlapMode, aLinkFeatureMultiplicityMode);
        var request = renderRequest("0123 567 90");

        var anchorA = spanLayerAdapter.add(doc, anno1.getUsername(), anno1Cas, 0, 4);
        var anchorB = spanLayerAdapter.add(doc, anno2.getUsername(), anno2Cas, 5, 11);
        var anchorC = spanLayerAdapter.add(doc, curator.getUsername(), curatorCas, 0, 8);

        sut.render(vdoc, request);

        switch (aOverlapMode) {
        case NO_OVERLAP, STACKING_ONLY:
            // Yes, empty because normally the curator's span would be rendered by the regular
            // rendering pipeline which we do not have here. The sidebar render only needs to
            // suppress its own rendering in this case.
            assertThat(vdoc.spans()).isEmpty();
            break;
        case OVERLAP_ONLY, ANY_OVERLAP:
            assertThat(vdoc.spans()) //
                    .extracting(span -> span.getVid()) //
                    .containsExactlyInAnyOrder( //
                            curationVid(anno1, anchorA), //
                            curationVid(anno2, anchorB));
            break;
        }
    }

    @ParameterizedTest
    @MethodSource("combinations")
    void thatFullyMergingStackedSpansWithLinksWorks(OverlapMode aOverlapMode,
            LinkFeatureMultiplicityMode aLinkFeatureMultiplicityMode)
        throws Exception
    {
        tuneLayers(aOverlapMode, aLinkFeatureMultiplicityMode);
        var request = renderRequest("anchor filler1 filler2");

        var anno1AnchorA = spanLayerAdapter.add(doc, anno1.getUsername(), anno1Cas, 0, 6);
        var anno1AnchorB = spanLayerAdapter.add(doc, anno1.getUsername(), anno1Cas, 0, 6);
        var anno1Filler1 = spanLayerAdapter.add(doc, anno1.getUsername(), anno1Cas, 7, 14);
        var anno1Filler2 = spanLayerAdapter.add(doc, anno1.getUsername(), anno1Cas, 15, 22);

        spanLayerAdapter.setFeatureValue(doc, anno1.getUsername(), anno1AnchorA,
                spanLayerLinkFeature, asList(new LinkWithRoleModel("role1", anno1Filler1)));

        spanLayerAdapter.setFeatureValue(doc, anno1.getUsername(), anno1AnchorB,
                spanLayerLinkFeature, asList(new LinkWithRoleModel("role1", anno1Filler2)));

        sut.render(vdoc, request);

        assertThat(vdoc.spans()) //
                .extracting(span -> span.getVid()) //
                .containsExactlyInAnyOrder( //
                        curationVid(anno1, anno1AnchorA), //
                        curationVid(anno1, anno1AnchorB), //
                        curationVid(anno1, anno1Filler1), //
                        curationVid(anno1, anno1Filler2));
        assertThat(vdoc.arcs()) //
                .extracting(span -> span.getVid()) //
                .containsExactlyInAnyOrder( //
                        curationVid(anno1, anno1AnchorA, 0, 0), //
                        curationVid(anno1, anno1AnchorB, 0, 0));

        LOG.info("'Merge' anchorA to the curator's CAS");
        var curatorAnchorA = spanLayerAdapter.add(doc, curator.getUsername(), curatorCas,
                anno1AnchorA.getBegin(), anno1AnchorA.getEnd());

        resetVDoc();
        sut.render(vdoc, request);

        switch (aOverlapMode) {
        case NO_OVERLAP, OVERLAP_ONLY:
            assertThat(vdoc.spans()) //
                    .extracting(span -> span.getVid()) //
                    .containsExactlyInAnyOrder( //
                            curationVid(anno1, anno1Filler1), //
                            curationVid(anno1, anno1Filler2));
            assertThat(vdoc.arcs()) //
                    .extracting(span -> span.getVid()) //
                    .containsExactlyInAnyOrder( //
                            curationVid(anno1, anno1AnchorA, 0, 0), //
                            curationVid(anno1, anno1AnchorB, 0, 0));
            break;
        case STACKING_ONLY, ANY_OVERLAP:
            assertThat(vdoc.spans()) //
                    .extracting(span -> span.getVid()) //
                    .containsExactlyInAnyOrder( //
                            curationVid(anno1, anno1AnchorA), //
                            curationVid(anno1, anno1AnchorB), //
                            curationVid(anno1, anno1Filler1), //
                            curationVid(anno1, anno1Filler2));
            assertThat(vdoc.arcs()) //
                    .extracting(span -> span.getVid()) //
                    .containsExactlyInAnyOrder( //
                            curationVid(anno1, anno1AnchorA, 0, 0), //
                            curationVid(anno1, anno1AnchorB, 0, 0));
            break;
        }

        LOG.info("'Merge' filler 1");
        var curatorFiller1 = spanLayerAdapter.add(doc, curator.getUsername(), curatorCas,
                anno1Filler1.getBegin(), anno1Filler1.getEnd());

        resetVDoc();
        sut.render(vdoc, request);

        switch (aOverlapMode) {
        case NO_OVERLAP, OVERLAP_ONLY:
            assertThat(vdoc.spans()) //
                    .extracting(span -> span.getVid()) //
                    .containsExactlyInAnyOrder( //
                            curationVid(anno1, anno1Filler2));
            assertThat(vdoc.arcs()) //
                    .extracting(span -> span.getVid()) //
                    .containsExactlyInAnyOrder( //
                            curationVid(anno1, anno1AnchorA, 0, 0), //
                            curationVid(anno1, anno1AnchorB, 0, 0));
            break;
        case STACKING_ONLY, ANY_OVERLAP:
            assertThat(vdoc.spans()) //
                    .extracting(span -> span.getVid()) //
                    .containsExactlyInAnyOrder( //
                            curationVid(anno1, anno1AnchorA), //
                            curationVid(anno1, anno1AnchorB), //
                            curationVid(anno1, anno1Filler2));
            assertThat(vdoc.arcs()) //
                    .extracting(span -> span.getVid()) //
                    .containsExactlyInAnyOrder( //
                            curationVid(anno1, anno1AnchorA, 0, 0), //
                            curationVid(anno1, anno1AnchorB, 0, 0));
            break;
        }

        LOG.info("'Merge' link from anchorA to filler 1");
        spanLayerAdapter.setFeatureValue(doc, curator.getUsername(), curatorAnchorA,
                spanLayerLinkFeature, asList(new LinkWithRoleModel("role1", curatorFiller1)));

        resetVDoc();
        sut.render(vdoc, request);

        switch (aOverlapMode) {
        case NO_OVERLAP, OVERLAP_ONLY:
            assertThat(vdoc.spans()) //
                    .extracting(span -> span.getVid()) //
                    .containsExactlyInAnyOrder( //
                            curationVid(anno1, anno1Filler2));
            break;
        case STACKING_ONLY, ANY_OVERLAP:
            switch (aLinkFeatureMultiplicityMode) {
            case ONE_TARGET_MULTIPLE_ROLES:
                assertThat(vdoc.spans()) //
                        .extracting(span -> span.getVid()) //
                        .containsExactlyInAnyOrder( //
                                curationVid(anno1, anno1Filler2));
                break;
            case MULTIPLE_TARGETS_MULTIPLE_ROLES, MULTIPLE_TARGETS_ONE_ROLE:
                assertThat(vdoc.spans()) //
                        .extracting(span -> span.getVid()) //
                        .containsExactlyInAnyOrder( //
                                curationVid(anno1, anno1AnchorB), //
                                curationVid(anno1, anno1Filler2));
                break;
            }
            break;
        }

        switch (aLinkFeatureMultiplicityMode) {
        case ONE_TARGET_MULTIPLE_ROLES:
            assertThat(vdoc.arcs()) //
                    .isEmpty();
            break;
        case MULTIPLE_TARGETS_MULTIPLE_ROLES, MULTIPLE_TARGETS_ONE_ROLE:
            assertThat(vdoc.arcs()) //
                    .extracting(span -> span.getVid()) //
                    .containsExactlyInAnyOrder( //
                            curationVid(anno1, anno1AnchorB, 0, 0));
            break;
        }

        LOG.info("'Merge' filler 2");
        var curatorFiller2 = spanLayerAdapter.add(doc, curator.getUsername(), curatorCas,
                anno1Filler2.getBegin(), anno1Filler2.getEnd());

        resetVDoc();
        sut.render(vdoc, request);

        switch (aOverlapMode) {
        case NO_OVERLAP, OVERLAP_ONLY:
            assertThat(vdoc.spans()) //
                    .isEmpty();
            break;
        case STACKING_ONLY, ANY_OVERLAP:
            switch (aLinkFeatureMultiplicityMode) {
            case ONE_TARGET_MULTIPLE_ROLES:
                assertThat(vdoc.spans()) //
                        .isEmpty();
                break;
            case MULTIPLE_TARGETS_MULTIPLE_ROLES, MULTIPLE_TARGETS_ONE_ROLE:
                assertThat(vdoc.spans()) //
                        .extracting(span -> span.getVid()) //
                        .containsExactlyInAnyOrder( //
                                curationVid(anno1, anno1AnchorB));
                break;
            }
            break;
        }

        switch (aLinkFeatureMultiplicityMode) {
        case ONE_TARGET_MULTIPLE_ROLES:
            assertThat(vdoc.arcs()) //
                    .isEmpty();
            break;
        case MULTIPLE_TARGETS_MULTIPLE_ROLES, MULTIPLE_TARGETS_ONE_ROLE:
            assertThat(vdoc.arcs()) //
                    .extracting(span -> span.getVid()) //
                    .containsExactlyInAnyOrder( //
                            curationVid(anno1, anno1AnchorB, 0, 0));
            break;
        }

        LOG.info("'Merge' anchorB to the curator's CAS");
        var curatorAnchorB = spanLayerAdapter.add(doc, curator.getUsername(), curatorCas,
                anno1AnchorB.getBegin(), anno1AnchorB.getEnd());

        resetVDoc();
        sut.render(vdoc, request);

        switch (aOverlapMode) {
        case NO_OVERLAP, OVERLAP_ONLY:
            assertThat(vdoc.spans()) //
                    .isEmpty();
            break;
        case STACKING_ONLY, ANY_OVERLAP:
            switch (aLinkFeatureMultiplicityMode) {
            case ONE_TARGET_MULTIPLE_ROLES:
                assertThat(vdoc.spans()) //
                        .isEmpty();
                break;
            case MULTIPLE_TARGETS_MULTIPLE_ROLES, MULTIPLE_TARGETS_ONE_ROLE:
                assertThat(vdoc.spans()) //
                        .extracting(span -> span.getVid()) //
                        .containsExactlyInAnyOrder( //
                                curationVid(anno1, anno1AnchorB));
                break;
            }
            break;
        }

        switch (aLinkFeatureMultiplicityMode) {
        case ONE_TARGET_MULTIPLE_ROLES:
            assertThat(vdoc.arcs()) //
                    .isEmpty();
            break;
        case MULTIPLE_TARGETS_MULTIPLE_ROLES, MULTIPLE_TARGETS_ONE_ROLE:
            assertThat(vdoc.arcs()) //
                    .extracting(span -> span.getVid()) //
                    .containsExactlyInAnyOrder( //
                            curationVid(anno1, anno1AnchorB, 0, 0));
            break;
        }

        LOG.info("'Merge' link from anchorB to filler 2");
        spanLayerAdapter.setFeatureValue(doc, curator.getUsername(), curatorAnchorB,
                spanLayerLinkFeature, asList(new LinkWithRoleModel("role1", curatorFiller2)));

        resetVDoc();
        sut.render(vdoc, request);

        assertThat(vdoc.spans()).isEmpty();
        assertThat(vdoc.arcs()).isEmpty();
    }

    private CurationVID curationVid(User aUser, AnnotationFS anchorA)
    {
        return new CurationVID(aUser.getUsername(), VID.builder().forAnnotation(anchorA).build());
    }

    private CurationVID curationVid(User aUser, AnnotationFS anchorA, int aAttribute, int aSlot)
    {
        return new CurationVID(aUser.getUsername(), VID.builder().forAnnotation(anchorA)
                .withAttribute(aAttribute).withSlot(aSlot).build());
    }

    private RenderRequest renderRequest(String aText) throws Exception
    {
        spanLayerAdapter = (SpanAdapter) schemaService.getAdapter(spanLayer);

        var tsd = new TypeSystemDescription_impl();
        for (var layer : asList(spanLayer, relationLayer)) {
            var support = layerSupportRegistry.findGenericExtension(layer).get();
            support.generateTypes(tsd, layer, schemaService.listSupportedFeatures(project));
        }

        curatorCas = CasFactory.createCas(tsd);
        curatorCas.setDocumentText(aText);
        anno1Cas = CasFactory.createCas(tsd);
        anno1Cas.setDocumentText(aText);
        anno2Cas = CasFactory.createCas(tsd);
        anno2Cas.setDocumentText(aText);

        vdoc = new VDocument();
        vdoc.setText(aText);
        vdoc.setWindow(0, aText.length());

        when(documentService.readAnnotationCas(doc, AnnotationSet.forUser(anno1.getUsername())))
                .thenReturn(anno1Cas);
        when(documentService.readAnnotationCas(doc, AnnotationSet.forUser(anno2.getUsername())))
                .thenReturn(anno2Cas);

        return RenderRequest.builder() //
                .withDocument(doc, curator) //
                .withCas(curatorCas) //
                .withWindow(0, curatorCas.getDocumentText().length()) //
                .withVisibleLayers(asList(spanLayer, relationLayer)) //
                .withAllLayers(asList(spanLayer, relationLayer)) //
                .build();
    }

    private void resetVDoc()
    {
        var oldVDoc = vdoc;

        vdoc = new VDocument();
        vdoc.setText(oldVDoc.getText());
        vdoc.setWindow(0, vdoc.getText().length());
    }

    private void tuneLayers(OverlapMode aOverlapMode,
            LinkFeatureMultiplicityMode aLinkFeatureMultiplicityMode)
        throws IOException
    {
        spanLayer.setOverlapMode(aOverlapMode);

        var spanLayerLinkFeatureTraits = new LinkFeatureTraits();
        spanLayerLinkFeatureTraits.setEnableRoleLabels(true);
        spanLayerLinkFeatureTraits.setMultiplicityMode(aLinkFeatureMultiplicityMode);
        spanLayerLinkFeature.setTraits(JSONUtil.toJsonString(spanLayerLinkFeatureTraits));
    }
}

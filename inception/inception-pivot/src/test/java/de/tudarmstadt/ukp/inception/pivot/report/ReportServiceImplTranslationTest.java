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
package de.tudarmstadt.ukp.inception.pivot.report;

import static de.tudarmstadt.ukp.clarin.webanno.model.AnnotationDocumentState.FINISHED;
import static de.tudarmstadt.ukp.clarin.webanno.model.AnnotationDocumentState.IN_PROGRESS;
import static java.util.Arrays.asList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationFeature;
import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationLayer;
import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationSet;
import de.tudarmstadt.ukp.clarin.webanno.model.Project;
import de.tudarmstadt.ukp.clarin.webanno.model.SourceDocument;
import de.tudarmstadt.ukp.clarin.webanno.security.UserDao;
import de.tudarmstadt.ukp.clarin.webanno.security.model.User;
import de.tudarmstadt.ukp.inception.documents.api.DocumentService;
import de.tudarmstadt.ukp.inception.pivot.api.aggregator.AggregatorSupport;
import de.tudarmstadt.ukp.inception.pivot.api.aggregator.AggregatorSupportRegistry;
import de.tudarmstadt.ukp.inception.pivot.api.extractor.ExtractorBindingResolutionContext;
import de.tudarmstadt.ukp.inception.pivot.api.extractor.ExtractorSupport;
import de.tudarmstadt.ukp.inception.pivot.api.extractor.ExtractorSupportRegistry;
import de.tudarmstadt.ukp.inception.pivot.api.extractor.FeatureBinding;
import de.tudarmstadt.ukp.inception.pivot.api.extractor.GeneralBinding;
import de.tudarmstadt.ukp.inception.pivot.api.extractor.LayerBinding;
import de.tudarmstadt.ukp.inception.pivot.api.report.AggregatorDef;
import de.tudarmstadt.ukp.inception.pivot.api.report.ExtractorDef;
import de.tudarmstadt.ukp.inception.pivot.api.report.ReportDef;
import de.tudarmstadt.ukp.inception.schema.api.AnnotationSchemaService;
import de.tudarmstadt.ukp.inception.support.logging.LogMessage;

@ExtendWith(MockitoExtension.class)
class ReportServiceImplTranslationTest
{
    private @Mock AnnotationSchemaService schemaService;
    private @Mock ExtractorSupportRegistry extractorRegistry;
    private @Mock AggregatorSupportRegistry aggregatorRegistry;
    private @Mock DocumentService documentService;
    private @Mock UserDao userService;

    private @Mock ExtractorSupport docNameSupport;
    private @Mock ExtractorSupport featureValueSupport;
    private @Mock AggregatorSupport countSupport;

    private ReportServiceImpl sut;

    private Project project;
    private AnnotationLayer tokenLayer;
    private AnnotationFeature posFeature;
    private User alice;
    private User curationUser;
    private User initialCasUser;
    private SourceDocument doc1;

    @BeforeEach
    void setUp()
    {
        sut = new ReportServiceImpl(null, schemaService, extractorRegistry, aggregatorRegistry,
                documentService, userService);

        project = new Project();
        project.setName("p");

        tokenLayer = new AnnotationLayer();
        tokenLayer.setName("webanno.custom.Token");
        tokenLayer.setUiName("Token");
        tokenLayer.setProject(project);

        posFeature = new AnnotationFeature();
        posFeature.setName("pos");
        posFeature.setUiName("POS");
        posFeature.setLayer(tokenLayer);

        alice = new User();
        alice.setUsername("alice");

        curationUser = new User();
        curationUser.setUsername("CURATION_USER");

        initialCasUser = new User();
        initialCasUser.setUsername("INITIAL_CAS");

        doc1 = new SourceDocument();
        doc1.setName("doc1.txt");
        doc1.setProject(project);
    }

    @Test
    void toDefSerialisesLayerAndFeatureExtractors()
    {
        var decl = new ReportDecl();
        decl.setAggregator(new AggregatorDecl("count", "Count", false));
        decl.setRowExtractors(asList( //
                new ExtractorDecl("documentName", "Token :: <doc>", new LayerBinding(tokenLayer)), //
                new ExtractorDecl("featureValue", "Token.POS", new FeatureBinding(posFeature))));
        decl.getAnnotators().add(AnnotationSet.forUser(alice));
        decl.getDocuments().add(doc1);
        decl.getStates().add(FINISHED);

        var def = sut.toDef(decl);

        assertThat(def.getAggregator()).isEqualTo(new AggregatorDef("count"));
        assertThat(def.getRowExtractors()).containsExactly( //
                new ExtractorDef("documentName", "webanno.custom.Token", null), //
                new ExtractorDef("featureValue", "webanno.custom.Token", "pos"));
        assertThat(def.getFilter().getAnnotators()).containsExactly("alice");
        assertThat(def.getFilter().getDocuments()).containsExactly("doc1.txt");
        assertThat(def.getFilter().getStates()).containsExactly(FINISHED);
    }

    @Test
    void toDefHandlesNullAggregatorAndEmptyLists()
    {
        var def = sut.toDef(new ReportDecl());

        assertThat(def.getAggregator()).isNull();
        assertThat(def.getRowExtractors()).isEmpty();
        assertThat(def.getColExtractors()).isEmpty();
        assertThat(def.getCellExtractors()).isEmpty();
        assertThat(def.getFilter().getAnnotators()).isEmpty();
    }

    @Test
    void toDefAndResolveRoundTripGeneralExtractor()
    {
        var decl = new ReportDecl();
        decl.setRowExtractors(
                asList(new ExtractorDecl("documentName", "<doc>", new GeneralBinding())));

        var def = sut.toDef(decl);
        assertThat(def.getRowExtractors()) //
                .containsExactly(new ExtractorDef("documentName", null, null));

        doReturn(Optional.of(docNameSupport)).when(extractorRegistry).getExtension("documentName");
        when(docNameSupport.getId()).thenReturn("documentName");
        when(docNameSupport.bindingFromDef(any(), any())).thenReturn(new GeneralBinding());
        when(docNameSupport.accepts(new GeneralBinding())).thenReturn(true);
        when(docNameSupport.renderLabel(new GeneralBinding())).thenReturn("<doc>");

        var resolved = sut.resolve(def, project);
        assertThat(resolved.problems()).isEmpty();
        assertThat(resolved.decl().getRowExtractors()).hasSize(1);
        assertThat(resolved.decl().getRowExtractors().get(0).binding())
                .isInstanceOf(GeneralBinding.class);
        assertThat(resolved.decl().getRowExtractors().get(0).id()).isEqualTo("documentName");
    }

    @Test
    void resolveHappyPath()
    {
        givenLayersInProject(tokenLayer);
        givenFeaturesOnLayer(tokenLayer, posFeature);
        givenLayerExtractor("documentName", "Token :: <doc>", docNameSupport);
        givenFeatureExtractor("featureValue", "Token.POS", featureValueSupport);
        givenAggregator("count", "Count", false, countSupport);
        givenProjectDataOwners(project, alice);
        givenProjectDocuments(project, doc1);

        var def = new ReportDef();
        def.setAggregator(new AggregatorDef("count"));
        def.setRowExtractors(asList( //
                new ExtractorDef("documentName", "webanno.custom.Token", null), //
                new ExtractorDef("featureValue", "webanno.custom.Token", "pos")));
        def.getFilter().setAnnotators(asList("alice"));
        def.getFilter().setDocuments(asList("doc1.txt"));
        def.getFilter().setStates(asList(IN_PROGRESS));

        var resolved = sut.resolve(def, project);

        assertThat(resolved.problems()).isEmpty();
        assertThat(resolved.decl().getAggregator()) //
                .isEqualTo(new AggregatorDecl("count", "Count", false));
        assertThat(resolved.decl().getRowExtractors()).extracting(ExtractorDecl::id) //
                .containsExactly("documentName", "featureValue");
        assertThat(resolved.decl().getRowExtractors()).extracting(ExtractorDecl::binding) //
                .containsExactly(new LayerBinding(tokenLayer), new FeatureBinding(posFeature));
        assertThat(resolved.decl().getAnnotators()).extracting(AnnotationSet::id) //
                .containsExactly("alice");
        assertThat(resolved.decl().getDocuments()).containsExactly(doc1);
        assertThat(resolved.decl().getStates()).containsExactly(IN_PROGRESS);
    }

    @Test
    void resolveDropsMissingLayer()
    {
        givenLayersInProject(); // no layers
        givenLayerExtractor("documentName", "doc", docNameSupport);

        var def = new ReportDef();
        def.setRowExtractors(asList(new ExtractorDef("documentName", "missing.Layer", null)));

        var resolved = sut.resolve(def, project);

        assertThat(resolved.decl().getRowExtractors()).isEmpty();
        assertThat(resolved.problems()).hasSize(1).first().asString() //
                .contains("missing.Layer");
    }

    @Test
    void resolveDropsMissingFeature()
    {
        givenLayersInProject(tokenLayer);
        givenFeaturesOnLayer(tokenLayer); // no features
        givenFeatureExtractor("featureValue", "feat", featureValueSupport);

        var def = new ReportDef();
        def.setRowExtractors(asList( //
                new ExtractorDef("featureValue", "webanno.custom.Token", "gone")));

        var resolved = sut.resolve(def, project);

        assertThat(resolved.decl().getRowExtractors()).isEmpty();
        assertThat(resolved.problems()).hasSize(1).first().asString().contains("gone");
    }

    @Test
    void resolveDropsExtractorWhenSupportRemoved()
    {
        // The registry has no support for these ids — simulates extension removal. An unstubbed
        // getExtension(...) returns Optional.empty(), so the extractors are dropped.

        var def = new ReportDef();
        def.setRowExtractors(asList( //
                new ExtractorDef("documentName", "webanno.custom.Token", null), //
                new ExtractorDef("featureValue", "webanno.custom.Token", "pos")));

        var resolved = sut.resolve(def, project);

        assertThat(resolved.decl().getRowExtractors()).isEmpty();
        assertThat(resolved.problems()).extracting(LogMessage::getMessage) //
                .anyMatch(p -> p.contains("documentName")) //
                .anyMatch(p -> p.contains("featureValue"));
    }

    @Test
    void resolveAggregatorMissingProducesProblem()
    {
        givenLayersInProject(tokenLayer);
        when(aggregatorRegistry.getExtension("count")).thenReturn(Optional.empty());

        var def = new ReportDef();
        def.setAggregator(new AggregatorDef("count"));

        var resolved = sut.resolve(def, project);

        assertThat(resolved.decl().getAggregator()).isNull();
        assertThat(resolved.problems()).hasSize(1).first().asString().contains("count");
    }

    @Test
    void resolveDropsUnknownAnnotatorsAndDocuments()
    {
        givenLayersInProject(tokenLayer);
        givenProjectDataOwners(project); // no annotators
        givenProjectDocuments(project); // no docs

        var def = new ReportDef();
        def.getFilter().setAnnotators(asList("ghost"));
        def.getFilter().setDocuments(asList("missing.txt"));

        var resolved = sut.resolve(def, project);

        assertThat(resolved.decl().getAnnotators()).isEmpty();
        assertThat(resolved.decl().getDocuments()).isEmpty();
        assertThat(resolved.problems()).hasSize(2).extracting(LogMessage::getMessage) //
                .anyMatch(p -> p.contains("ghost")) //
                .anyMatch(p -> p.contains("missing.txt"));
    }

    @Test
    void resolveAcceptsCurationAndInitialCasUsers()
    {
        givenLayersInProject(tokenLayer);
        givenProjectDataOwners(project); // no annotators, just the pseudo-users

        var def = new ReportDef();
        def.getFilter().setAnnotators(asList("CURATION_USER", "INITIAL_CAS"));

        var resolved = sut.resolve(def, project);

        assertThat(resolved.problems()).isEmpty();
        assertThat(resolved.decl().getAnnotators()).extracting(AnnotationSet::id) //
                .containsExactly("CURATION_USER", "INITIAL_CAS");
    }

    // ----- mock setup helpers --------------------------------------------------------

    private void givenLayersInProject(AnnotationLayer... aLayers)
    {
        when(schemaService.listAnnotationLayer(project)).thenReturn(asList(aLayers));
    }

    private void givenFeaturesOnLayer(AnnotationLayer aLayer, AnnotationFeature... aFeatures)
    {
        lenient().when(schemaService.listAnnotationFeature(aLayer)).thenReturn(asList(aFeatures));
        // resolve() snapshots all project features in one query, grouped by layer.
        lenient().when(schemaService.listAnnotationFeature(project)).thenReturn(asList(aFeatures));
    }

    private void givenLayerExtractor(String aId, String aName, ExtractorSupport aSupport)
    {
        lenient().when(aSupport.getId()).thenReturn(aId);
        lenient().doReturn(Optional.of(aSupport)).when(extractorRegistry).getExtension(aId);
        lenient().when(aSupport.accepts(any())).thenReturn(true);
        lenient().when(aSupport.renderLabel(any())).thenReturn(aName);
        lenient().when(aSupport.bindingFromDef(any(), any())).thenAnswer(inv -> {
            var def = inv.getArgument(0, ExtractorDef.class);
            var ctx = inv.getArgument(1, ExtractorBindingResolutionContext.class);
            var layer = ctx.resolveLayer(def.getLayer());
            return layer != null ? new LayerBinding(layer) : null;
        });
    }

    private void givenFeatureExtractor(String aId, String aName, ExtractorSupport aSupport)
    {
        lenient().when(aSupport.getId()).thenReturn(aId);
        lenient().doReturn(Optional.of(aSupport)).when(extractorRegistry).getExtension(aId);
        lenient().when(aSupport.accepts(any())).thenReturn(true);
        lenient().when(aSupport.renderLabel(any())).thenReturn(aName);
        lenient().when(aSupport.bindingFromDef(any(), any())).thenAnswer(inv -> {
            var def = inv.getArgument(0, ExtractorDef.class);
            var ctx = inv.getArgument(1, ExtractorBindingResolutionContext.class);
            var feature = ctx.resolveFeature(def.getLayer(), def.getFeature());
            return feature != null ? new FeatureBinding(feature) : null;
        });
    }

    private void givenAggregator(String aId, String aName, boolean aSupportsCells,
            AggregatorSupport aSupport)
    {
        lenient().when(aSupport.getId()).thenReturn(aId);
        lenient().when(aSupport.getName()).thenReturn(aName);
        lenient().when(aSupport.supportsCells()).thenReturn(aSupportsCells);
        lenient().when(aggregatorRegistry.getExtension(aId)).thenReturn(Optional.of(aSupport));
    }

    private void givenProjectDataOwners(Project aProject, User... aUsers)
    {
        var dataOwners = new java.util.ArrayList<AnnotationSet>();
        for (var u : aUsers) {
            dataOwners.add(AnnotationSet.forUser(u));
        }
        lenient().when(documentService.listDataOwners(aProject)).thenReturn(dataOwners);
        lenient().when(userService.getCurationUser()).thenReturn(curationUser);
        lenient().when(userService.getInitialCasUser()).thenReturn(initialCasUser);
    }

    private void givenProjectDocuments(Project aProject, SourceDocument... aDocs)
    {
        lenient().when(documentService.listSourceDocuments(aProject)).thenReturn(asList(aDocs));
    }
}

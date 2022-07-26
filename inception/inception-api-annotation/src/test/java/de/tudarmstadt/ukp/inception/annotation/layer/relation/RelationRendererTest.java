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
package de.tudarmstadt.ukp.inception.annotation.layer.relation;

import static de.tudarmstadt.ukp.clarin.webanno.model.AnchoringMode.SINGLE_TOKEN;
import static de.tudarmstadt.ukp.clarin.webanno.model.OverlapMode.ANY_OVERLAP;
import static de.tudarmstadt.ukp.clarin.webanno.model.OverlapMode.NO_OVERLAP;
import static de.tudarmstadt.ukp.clarin.webanno.model.OverlapMode.OVERLAP_ONLY;
import static de.tudarmstadt.ukp.clarin.webanno.model.OverlapMode.STACKING_ONLY;
import static de.tudarmstadt.ukp.clarin.webanno.support.WebAnnoConst.FEAT_REL_SOURCE;
import static de.tudarmstadt.ukp.clarin.webanno.support.WebAnnoConst.FEAT_REL_TARGET;
import static de.tudarmstadt.ukp.clarin.webanno.support.WebAnnoConst.RELATION_TYPE;
import static de.tudarmstadt.ukp.clarin.webanno.support.WebAnnoConst.SPAN_TYPE;
import static de.tudarmstadt.ukp.inception.rendering.vmodel.VCommentType.ERROR;
import static de.tudarmstadt.ukp.inception.rendering.vmodel.VCommentType.YIELD;
import static java.util.Arrays.asList;
import static org.apache.uima.fit.util.JCasUtil.select;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

import java.util.ArrayList;
import java.util.List;

import org.apache.uima.cas.text.AnnotationFS;
import org.apache.uima.fit.factory.JCasFactory;
import org.apache.uima.fit.testing.factory.TokenBuilder;
import org.apache.uima.jcas.JCas;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationFeature;
import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationLayer;
import de.tudarmstadt.ukp.clarin.webanno.model.Project;
import de.tudarmstadt.ukp.clarin.webanno.model.SourceDocument;
import de.tudarmstadt.ukp.dkpro.core.api.lexmorph.type.pos.POS;
import de.tudarmstadt.ukp.dkpro.core.api.segmentation.type.Sentence;
import de.tudarmstadt.ukp.dkpro.core.api.segmentation.type.Token;
import de.tudarmstadt.ukp.dkpro.core.api.syntax.type.dependency.Dependency;
import de.tudarmstadt.ukp.inception.annotation.layer.behaviors.LayerBehaviorRegistryImpl;
import de.tudarmstadt.ukp.inception.annotation.layer.behaviors.LayerSupportRegistryImpl;
import de.tudarmstadt.ukp.inception.annotation.layer.chain.ChainLayerSupport;
import de.tudarmstadt.ukp.inception.annotation.layer.span.SpanLayerSupport;
import de.tudarmstadt.ukp.inception.rendering.vmodel.VComment;
import de.tudarmstadt.ukp.inception.rendering.vmodel.VDocument;
import de.tudarmstadt.ukp.inception.rendering.vmodel.VID;
import de.tudarmstadt.ukp.inception.schema.feature.FeatureSupportRegistry;

@ExtendWith(MockitoExtension.class)
public class RelationRendererTest
{
    private FeatureSupportRegistry featureSupportRegistry;
    private LayerSupportRegistryImpl layerSupportRegistry;
    private Project project;
    private AnnotationLayer depLayer;
    private AnnotationFeature dependencyLayerGovernor;
    private AnnotationFeature dependencyLayerDependent;
    private JCas jcas;
    private SourceDocument document;
    private String username;
    private List<RelationLayerBehavior> behaviors;

    @BeforeEach
    public void setup() throws Exception
    {
        if (jcas == null) {
            jcas = JCasFactory.createJCas();
        }
        else {
            jcas.reset();
        }

        username = "user";

        project = new Project();
        project.setId(1l);

        document = new SourceDocument();
        document.setId(1l);
        document.setProject(project);

        // Set up annotation schema with POS and Dependency
        AnnotationLayer tokenLayer = new AnnotationLayer(Token.class.getName(), "Token", SPAN_TYPE,
                project, true, SINGLE_TOKEN, NO_OVERLAP);
        tokenLayer.setId(1l);
        AnnotationFeature tokenLayerPos = new AnnotationFeature(1l, tokenLayer, "pos",
                POS.class.getName());

        AnnotationLayer posLayer = new AnnotationLayer(POS.class.getName(), "POS", SPAN_TYPE,
                project, true, SINGLE_TOKEN, NO_OVERLAP);
        posLayer.setId(2l);

        depLayer = new AnnotationLayer(Dependency.class.getName(), "Dependency", RELATION_TYPE,
                project, true, SINGLE_TOKEN, OVERLAP_ONLY);
        depLayer.setId(3l);
        depLayer.setAttachType(tokenLayer);
        depLayer.setAttachFeature(tokenLayerPos);
        dependencyLayerGovernor = new AnnotationFeature(2l, depLayer, "Governor",
                Token.class.getName());
        dependencyLayerDependent = new AnnotationFeature(3l, depLayer, "Dependent",
                Token.class.getName());

        LayerBehaviorRegistryImpl layerBehaviorRegistry = new LayerBehaviorRegistryImpl(asList());
        layerBehaviorRegistry.init();

        featureSupportRegistry = mock(FeatureSupportRegistry.class);
        layerSupportRegistry = new LayerSupportRegistryImpl(asList(
                new SpanLayerSupport(featureSupportRegistry, null, layerBehaviorRegistry),
                new RelationLayerSupport(featureSupportRegistry, null, layerBehaviorRegistry),
                new ChainLayerSupport(featureSupportRegistry, null, layerBehaviorRegistry)));
        layerSupportRegistry.init();

        behaviors = asList(new RelationAttachmentBehavior(), new RelationOverlapBehavior(),
                new RelationCrossSentenceBehavior());
    }

    @Test
    public void thatRelationCrossSentenceBehaviorOnRenderGeneratesErrors() throws Exception
    {
        TokenBuilder<Token, Sentence> builder = new TokenBuilder<>(Token.class, Sentence.class);
        builder.buildTokens(jcas, "This is a test .\nThis is sentence two .");

        for (Token t : select(jcas, Token.class)) {
            POS pos = new POS(jcas, t.getBegin(), t.getEnd());
            t.setPos(pos);
            pos.addToIndexes();
        }

        RelationAdapter adapter = new RelationAdapter(layerSupportRegistry, featureSupportRegistry,
                null, depLayer, FEAT_REL_TARGET, FEAT_REL_SOURCE,
                () -> asList(dependencyLayerGovernor, dependencyLayerDependent), behaviors);

        List<POS> posAnnotations = new ArrayList<>(select(jcas, POS.class));

        POS source = posAnnotations.get(0);
        POS target = posAnnotations.get(posAnnotations.size() - 1);

        depLayer.setCrossSentence(true);
        AnnotationFS dep = adapter.add(document, username, source, target, jcas.getCas());

        depLayer.setCrossSentence(false);
        RelationRenderer sut = new RelationRenderer(adapter, layerSupportRegistry,
                featureSupportRegistry, asList(new RelationCrossSentenceBehavior()));

        VDocument vdoc = new VDocument();
        sut.render(jcas.getCas(), asList(), vdoc, 0, jcas.getDocumentText().length());

        assertThat(vdoc.comments()).usingFieldByFieldElementComparator().contains(
                new VComment(dep, ERROR, "Crossing sentence boundaries is not permitted."));
    }

    @Test
    public void thatRelationOverlapBehaviorOnRenderGeneratesErrors() throws Exception
    {
        TokenBuilder<Token, Sentence> builder = new TokenBuilder<>(Token.class, Sentence.class);
        builder.buildTokens(jcas, "This is a test .\nThis is sentence two .");

        for (Token t : select(jcas, Token.class)) {
            POS pos = new POS(jcas, t.getBegin(), t.getEnd());
            t.setPos(pos);
            pos.addToIndexes();
        }

        RelationAdapter adapter = new RelationAdapter(layerSupportRegistry, featureSupportRegistry,
                null, depLayer, FEAT_REL_TARGET, FEAT_REL_SOURCE,
                () -> asList(dependencyLayerGovernor, dependencyLayerDependent), behaviors);

        List<POS> posAnnotations = new ArrayList<>(select(jcas, POS.class));

        POS source = posAnnotations.get(0);
        POS target = posAnnotations.get(1);

        RelationRenderer sut = new RelationRenderer(adapter, layerSupportRegistry,
                featureSupportRegistry, asList(new RelationOverlapBehavior()));

        // Create two annotations stacked annotations
        depLayer.setOverlapMode(ANY_OVERLAP);
        AnnotationFS dep1 = adapter.add(document, username, source, target, jcas.getCas());
        AnnotationFS dep2 = adapter.add(document, username, source, target, jcas.getCas());

        {
            depLayer.setOverlapMode(ANY_OVERLAP);
            VDocument vdoc = new VDocument();
            sut.render(jcas.getCas(), asList(), vdoc, 0, jcas.getDocumentText().length());

            assertThat(vdoc.comments()).filteredOn(c -> !YIELD.equals(c.getCommentType()))
                    .isEmpty();
        }

        {
            depLayer.setOverlapMode(STACKING_ONLY);
            VDocument vdoc = new VDocument();
            sut.render(jcas.getCas(), asList(), vdoc, 0, jcas.getDocumentText().length());

            assertThat(vdoc.comments()).filteredOn(c -> !YIELD.equals(c.getCommentType()))
                    .isEmpty();

        }

        {
            depLayer.setOverlapMode(OVERLAP_ONLY);
            VDocument vdoc = new VDocument();
            sut.render(jcas.getCas(), asList(), vdoc, 0, jcas.getDocumentText().length());

            assertThat(vdoc.comments()).filteredOn(c -> !YIELD.equals(c.getCommentType()))
                    .usingFieldByFieldElementComparator()
                    .contains(new VComment(dep1, ERROR, "Stacking is not permitted."),
                            new VComment(dep2, ERROR, "Stacking is not permitted."));
        }

        {
            depLayer.setOverlapMode(NO_OVERLAP);
            VDocument vdoc = new VDocument();
            sut.render(jcas.getCas(), asList(), vdoc, 0, jcas.getDocumentText().length());

            assertThat(vdoc.comments()).filteredOn(c -> !YIELD.equals(c.getCommentType()))
                    .usingFieldByFieldElementComparator()
                    .contains(new VComment(dep1, ERROR, "Stacking is not permitted."),
                            new VComment(dep2, ERROR, "Stacking is not permitted."));
        }

        // Remove the stacked annotation and introduce one that is purely overlapping
        adapter.delete(document, username, jcas.getCas(), new VID(dep2));
        depLayer.setOverlapMode(ANY_OVERLAP);
        AnnotationFS dep3 = adapter.add(document, username, source, posAnnotations.get(2),
                jcas.getCas());

        {
            depLayer.setOverlapMode(NO_OVERLAP);
            VDocument vdoc = new VDocument();
            sut.render(jcas.getCas(), asList(), vdoc, 0, jcas.getDocumentText().length());

            assertThat(vdoc.comments()).filteredOn(c -> !YIELD.equals(c.getCommentType()))
                    .usingFieldByFieldElementComparator()
                    .contains(new VComment(dep1, ERROR, "Overlap is not permitted."),
                            new VComment(dep3, ERROR, "Overlap is not permitted."));
        }
    }
}

/*
 * Copyright 2018
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
package de.tudarmstadt.ukp.clarin.webanno.api.annotation.adapter;

import static de.tudarmstadt.ukp.clarin.webanno.api.WebAnnoConst.FEAT_REL_SOURCE;
import static de.tudarmstadt.ukp.clarin.webanno.api.WebAnnoConst.FEAT_REL_TARGET;
import static de.tudarmstadt.ukp.clarin.webanno.api.WebAnnoConst.PROJECT_TYPE_ANNOTATION;
import static de.tudarmstadt.ukp.clarin.webanno.api.WebAnnoConst.RELATION_TYPE;
import static de.tudarmstadt.ukp.clarin.webanno.api.WebAnnoConst.SPAN_TYPE;
import static de.tudarmstadt.ukp.clarin.webanno.model.AnchoringMode.SINGLE_TOKEN;
import static java.util.Arrays.asList;
import static org.apache.uima.fit.util.JCasUtil.select;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang3.tuple.Pair;
import org.apache.uima.cas.text.AnnotationFS;
import org.apache.uima.fit.factory.JCasFactory;
import org.apache.uima.fit.testing.factory.TokenBuilder;
import org.apache.uima.fit.util.FSUtil;
import org.apache.uima.jcas.JCas;
import org.junit.Before;
import org.junit.Test;

import de.tudarmstadt.ukp.clarin.webanno.api.annotation.exception.AnnotationException;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.exception.MultipleSentenceCoveredException;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.feature.FeatureSupportRegistry;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.feature.FeatureSupportRegistryImpl;
import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationFeature;
import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationLayer;
import de.tudarmstadt.ukp.clarin.webanno.model.Project;
import de.tudarmstadt.ukp.clarin.webanno.model.SourceDocument;
import de.tudarmstadt.ukp.clarin.webanno.support.logging.LogMessage;
import de.tudarmstadt.ukp.dkpro.core.api.lexmorph.type.pos.POS;
import de.tudarmstadt.ukp.dkpro.core.api.segmentation.type.Sentence;
import de.tudarmstadt.ukp.dkpro.core.api.segmentation.type.Token;
import de.tudarmstadt.ukp.dkpro.core.api.syntax.type.dependency.Dependency;

public class RelationAdapterTest
{
    private FeatureSupportRegistry featureSupportRegistry;
    private Project project;
    private AnnotationLayer depLayer;
    private AnnotationFeature dependencyLayerGovernor;
    private AnnotationFeature dependencyLayerDependent;
    private JCas jcas;
    private SourceDocument document;
    private String username;
    private List<RelationLayerBehavior> behaviors;

    @Before
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
        project.setMode(PROJECT_TYPE_ANNOTATION);
        
        document = new SourceDocument();
        document.setId(1l);
        document.setProject(project);
        
        // Set up annotation schema with POS and Dependency
        AnnotationLayer tokenLayer = new AnnotationLayer(Token.class.getName(), "Token", SPAN_TYPE,
                project, true, SINGLE_TOKEN);
        tokenLayer.setId(1l);
        AnnotationFeature tokenLayerPos = new AnnotationFeature(1l, tokenLayer, "pos",
                POS.class.getName());

        AnnotationLayer posLayer = new AnnotationLayer(POS.class.getName(), "POS", SPAN_TYPE,
                project, true, SINGLE_TOKEN);
        posLayer.setId(2l);

        depLayer = new AnnotationLayer(Dependency.class.getName(), "Dependency", RELATION_TYPE,
                project, true, SINGLE_TOKEN);
        depLayer.setId(3l);
        depLayer.setAttachType(tokenLayer);
        depLayer.setAttachFeature(tokenLayerPos);
        dependencyLayerGovernor = new AnnotationFeature(2l, depLayer, "Governor",
                Token.class.getName());
        dependencyLayerDependent = new AnnotationFeature(3l, depLayer, "Dependent",
                Token.class.getName());

        featureSupportRegistry = new FeatureSupportRegistryImpl(asList());
        
        behaviors = asList(new RelationAttachmentBehavior(), new RelationStackingBehavior(),
                new RelationCrossSentenceBehavior());
    }    
    
    @Test
    public void thatRelationAttachmentBehaviorOnCreateWorks() throws Exception
    {
        TokenBuilder<Token, Sentence> builder = new TokenBuilder<>(Token.class, Sentence.class);
        builder.buildTokens(jcas, "This is a test .");

        for (Token t : select(jcas, Token.class)) {
            POS pos = new POS(jcas, t.getBegin(), t.getEnd());
            t.setPos(pos);
            pos.addToIndexes();
        }

        RelationAdapter sut = new RelationAdapter(featureSupportRegistry, null, depLayer,
                FEAT_REL_TARGET, FEAT_REL_SOURCE,
                asList(dependencyLayerGovernor, dependencyLayerDependent), behaviors);

        List<POS> posAnnotations = new ArrayList<>(select(jcas, POS.class));
        List<Token> tokens = new ArrayList<>(select(jcas, Token.class));

        POS source = posAnnotations.get(0);
        POS target = posAnnotations.get(1);

        AnnotationFS dep = sut.add(document, username, source, target, jcas, 0,
                jcas.getDocumentText().length());

        assertThat(FSUtil.getFeature(dep, FEAT_REL_SOURCE, Token.class)).isEqualTo(tokens.get(0));
        assertThat(FSUtil.getFeature(dep, FEAT_REL_TARGET, Token.class)).isEqualTo(tokens.get(1));
    }
    
    @Test
    public void thatRelationCrossSentenceBehaviorOnCreateThrowsException() throws Exception
    {
        depLayer.setCrossSentence(false);
        
        TokenBuilder<Token, Sentence> builder = new TokenBuilder<>(Token.class, Sentence.class);
        builder.buildTokens(jcas, "This is a test .\nThis is sentence two .");

        for (Token t : select(jcas, Token.class)) {
            POS pos = new POS(jcas, t.getBegin(), t.getEnd());
            t.setPos(pos);
            pos.addToIndexes();
        }

        RelationAdapter sut = new RelationAdapter(featureSupportRegistry, null, depLayer,
                FEAT_REL_TARGET, FEAT_REL_SOURCE,
                asList(dependencyLayerGovernor, dependencyLayerDependent), behaviors);

        List<POS> posAnnotations = new ArrayList<>(select(jcas, POS.class));

        POS source = posAnnotations.get(0);
        POS target = posAnnotations.get(posAnnotations.size() - 1);

        assertThatExceptionOfType(MultipleSentenceCoveredException.class)
                .isThrownBy(() -> sut.add(document, username, source, target, jcas, 0, 
                        jcas.getDocumentText().length()))
                .withMessageContaining("multiple sentences");
    }
    
    @Test
    public void thatRelationCrossSentenceBehaviorOnValidateGeneratesErrors() throws Exception
    {
        TokenBuilder<Token, Sentence> builder = new TokenBuilder<>(Token.class, Sentence.class);
        builder.buildTokens(jcas, "This is a test .\nThis is sentence two .");

        for (Token t : select(jcas, Token.class)) {
            POS pos = new POS(jcas, t.getBegin(), t.getEnd());
            t.setPos(pos);
            pos.addToIndexes();
        }

        RelationAdapter sut = new RelationAdapter(featureSupportRegistry, null, depLayer,
                FEAT_REL_TARGET, FEAT_REL_SOURCE,
                asList(dependencyLayerGovernor, dependencyLayerDependent), behaviors);

        List<POS> posAnnotations = new ArrayList<>(select(jcas, POS.class));

        POS source = posAnnotations.get(0);
        POS target = posAnnotations.get(posAnnotations.size() - 1);

        depLayer.setCrossSentence(true);
        sut.add(document, username, source, target, jcas, 0, jcas.getDocumentText().length());
        
        depLayer.setCrossSentence(false);
        assertThat(sut.validate(jcas))
                .extracting(Pair::getLeft)
                .usingElementComparatorIgnoringFields("source", "message")
                .containsExactly(LogMessage.error(null, ""));
    }
    
    @Test
    public void thatRelationStackingBehaviorOnCreateDoesNotThrowException() throws Exception
    {
        TokenBuilder<Token, Sentence> builder = new TokenBuilder<>(Token.class, Sentence.class);
        builder.buildTokens(jcas, "This is a test .\nThis is sentence two .");

        for (Token t : select(jcas, Token.class)) {
            POS pos = new POS(jcas, t.getBegin(), t.getEnd());
            t.setPos(pos);
            pos.addToIndexes();
        }

        RelationAdapter sut = new RelationAdapter(featureSupportRegistry, null, depLayer,
                FEAT_REL_TARGET, FEAT_REL_SOURCE,
                asList(dependencyLayerGovernor, dependencyLayerDependent), behaviors);

        List<POS> posAnnotations = new ArrayList<>(select(jcas, POS.class));
        List<Token> tokens = new ArrayList<>(select(jcas, Token.class));

        POS source = posAnnotations.get(0);
        POS target = posAnnotations.get(1);

        depLayer.setAllowStacking(true);
        AnnotationFS dep1 = sut.add(document, username, source, target, jcas, 0,
                jcas.getDocumentText().length());
        AnnotationFS dep2 = sut.add(document, username, source, target, jcas, 0,
                jcas.getDocumentText().length());
        
        assertThat(FSUtil.getFeature(dep1, FEAT_REL_SOURCE, Token.class)).isEqualTo(tokens.get(0));
        assertThat(FSUtil.getFeature(dep1, FEAT_REL_TARGET, Token.class)).isEqualTo(tokens.get(1));
        assertThat(FSUtil.getFeature(dep2, FEAT_REL_SOURCE, Token.class)).isEqualTo(tokens.get(0));
        assertThat(FSUtil.getFeature(dep2, FEAT_REL_TARGET, Token.class)).isEqualTo(tokens.get(1));
    } 
    
    @Test
    public void thatRelationStackingBehaviorOnCreateThrowsException() throws Exception
    {
        depLayer.setAllowStacking(false);
        
        TokenBuilder<Token, Sentence> builder = new TokenBuilder<>(Token.class, Sentence.class);
        builder.buildTokens(jcas, "This is a test .\nThis is sentence two .");

        for (Token t : select(jcas, Token.class)) {
            POS pos = new POS(jcas, t.getBegin(), t.getEnd());
            t.setPos(pos);
            pos.addToIndexes();
        }

        RelationAdapter sut = new RelationAdapter(featureSupportRegistry, null, depLayer,
                FEAT_REL_TARGET, FEAT_REL_SOURCE,
                asList(dependencyLayerGovernor, dependencyLayerDependent), behaviors);

        List<POS> posAnnotations = new ArrayList<>(select(jcas, POS.class));

        POS source = posAnnotations.get(0);
        POS target = posAnnotations.get(1);

        // First annotation should work
        sut.add(document, username, source, target, jcas, 0, jcas.getDocumentText().length());
        
        // Second one at the same location should cause an error
        assertThatExceptionOfType(AnnotationException.class)
                .isThrownBy(() -> sut.add(document, username, source, target, jcas, 0, 
                        jcas.getDocumentText().length()))
                .withMessageContaining("stacking is not enabled");
    }
    
    @Test
    public void thatRelationStackingBehaviorOnValidateGeneratesErrors() throws Exception
    {
        TokenBuilder<Token, Sentence> builder = new TokenBuilder<>(Token.class, Sentence.class);
        builder.buildTokens(jcas, "This is a test .\nThis is sentence two .");

        for (Token t : select(jcas, Token.class)) {
            POS pos = new POS(jcas, t.getBegin(), t.getEnd());
            t.setPos(pos);
            pos.addToIndexes();
        }

        RelationAdapter sut = new RelationAdapter(featureSupportRegistry, null, depLayer,
                FEAT_REL_TARGET, FEAT_REL_SOURCE,
                asList(dependencyLayerGovernor, dependencyLayerDependent), behaviors);

        List<POS> posAnnotations = new ArrayList<>(select(jcas, POS.class));

        POS source = posAnnotations.get(0);
        POS target = posAnnotations.get(1);

        depLayer.setAllowStacking(true);
        sut.add(document, username, source, target, jcas, 0, jcas.getDocumentText().length());
        sut.add(document, username, source, target, jcas, 0, jcas.getDocumentText().length());
        
        depLayer.setAllowStacking(false);
        assertThat(sut.validate(jcas))
                .extracting(Pair::getLeft)
                .usingElementComparatorIgnoringFields("source", "message")
                .containsExactly(LogMessage.error(null, ""));
    }
}

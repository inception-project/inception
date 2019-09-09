/*
 * Copyright 2018
 * Ubiquitous Knowledge Processing (UKP) Lab
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
package de.tudarmstadt.ukp.inception.search.index.mtas;

import static de.tudarmstadt.ukp.clarin.webanno.api.WebAnnoConst.RELATION_TYPE;
import static de.tudarmstadt.ukp.clarin.webanno.api.WebAnnoConst.SPAN_TYPE;
import static de.tudarmstadt.ukp.clarin.webanno.model.AnchoringMode.SINGLE_TOKEN;
import static de.tudarmstadt.ukp.clarin.webanno.model.AnchoringMode.TOKENS;
import static de.tudarmstadt.ukp.clarin.webanno.model.OverlapMode.NO_OVERLAP;
import static java.util.Arrays.asList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;

import java.util.ArrayList;
import java.util.List;

import org.apache.uima.cas.CAS;
import org.apache.uima.fit.factory.JCasBuilder;
import org.apache.uima.fit.factory.JCasFactory;
import org.apache.uima.fit.testing.factory.TokenBuilder;
import org.apache.uima.jcas.JCas;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

import de.tudarmstadt.ukp.clarin.webanno.api.AnnotationSchemaService;
import de.tudarmstadt.ukp.clarin.webanno.api.WebAnnoConst;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.adapter.RelationAdapter;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.adapter.SpanAdapter;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.feature.FeatureSupportRegistryImpl;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.feature.PrimitiveUimaFeatureSupport;
import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationFeature;
import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationLayer;
import de.tudarmstadt.ukp.clarin.webanno.model.Project;
import de.tudarmstadt.ukp.dkpro.core.api.lexmorph.type.pos.POS;
import de.tudarmstadt.ukp.dkpro.core.api.ner.type.NamedEntity;
import de.tudarmstadt.ukp.dkpro.core.api.segmentation.type.Sentence;
import de.tudarmstadt.ukp.dkpro.core.api.segmentation.type.Token;
import de.tudarmstadt.ukp.dkpro.core.api.syntax.type.dependency.Dependency;
import de.tudarmstadt.ukp.inception.search.FeatureIndexingSupportRegistryImpl;
import de.tudarmstadt.ukp.inception.search.PrimitiveUimaIndexingSupport;
import mtas.analysis.token.MtasToken;
import mtas.analysis.token.MtasTokenCollection;

public class MtasUimaParserTest
{
    private Project project;
    private @Mock AnnotationSchemaService annotationSchemaService;
    private FeatureSupportRegistryImpl featureSupportRegistry;
    private FeatureIndexingSupportRegistryImpl featureIndexingSupportRegistry;
    private JCas jcas;
    
    @Before
    public void setup() throws Exception
    {
        initMocks(this);
        
        project = new Project();
        project.setId(1l);
        project.setName("test project");
        project.setMode(WebAnnoConst.PROJECT_TYPE_ANNOTATION);
        
        featureSupportRegistry = new FeatureSupportRegistryImpl(
                asList(new PrimitiveUimaFeatureSupport()));
        featureSupportRegistry.init();
        
        featureIndexingSupportRegistry = new FeatureIndexingSupportRegistryImpl(
                asList(new PrimitiveUimaIndexingSupport(featureSupportRegistry)));
        featureIndexingSupportRegistry.init();
        
        // Resetting the JCas is faster than re-creating it
        if (jcas == null) {
            jcas = JCasFactory.createJCas();
        }
        else {
            jcas.reset();
        }
    }
    
    @Test
    public void testSentencesAndTokens() throws Exception
    {
        TokenBuilder<Token, Sentence> builder = TokenBuilder.create(Token.class, Sentence.class);
        builder.buildTokens(jcas, "This is a test . \n This is sentence two .");

        // Only tokens and sentences here, no extra layers
        when(annotationSchemaService.listAnnotationLayer(project)).thenReturn(asList());
        
        MtasUimaParser sut = new MtasUimaParser(project, annotationSchemaService,
                featureIndexingSupportRegistry);
        MtasTokenCollection tc = sut.createTokenCollection(jcas.getCas());
        
        MtasUtils.print(tc);
        
        List<MtasToken> tokens = new ArrayList<>();
        tc.iterator().forEachRemaining(tokens::add);
        
        assertThat(tokens)
                .filteredOn(t -> "Token".equals(t.getPrefix()))
                .extracting(MtasToken::getPostfix)
                .containsExactly(
                        "This", "is", "a", "test", ".", "This", "is", "sentence", "two", ".");

        assertThat(tokens)
                .filteredOn(t -> "s".equals(t.getPrefix()))
                .extracting(MtasToken::getPostfix)
                .containsExactly(
                        "This is a test .", "This is sentence two .");
    }
    
    @Test
    public void testNamedEnity() throws Exception
    {
        JCasBuilder builder = new JCasBuilder(jcas);
        builder.add("I", Token.class);
        builder.add(" ");
        builder.add("am", Token.class);
        builder.add(" ");
        int begin = builder.getPosition();
        builder.add("John", Token.class);
        builder.add(" ");
        builder.add("Smith", Token.class);
        NamedEntity ne = new NamedEntity(jcas, begin, builder.getPosition());
        ne.setValue("PER");
        ne.addToIndexes();
        builder.add(" ");
        builder.add(".", Token.class);
        
        AnnotationLayer layer = new AnnotationLayer(NamedEntity.class.getName(),
                "Named Entity", SPAN_TYPE, project, true, TOKENS, NO_OVERLAP);
        when(annotationSchemaService.listAnnotationLayer(any(Project.class)))
                .thenReturn(asList(layer));

        when(annotationSchemaService.listAnnotationFeature(any(AnnotationLayer.class)))
                .thenReturn(asList(
                        new AnnotationFeature(1l, layer, "value", CAS.TYPE_NAME_STRING),
                        new AnnotationFeature(2l, layer, "identifier", CAS.TYPE_NAME_STRING)));
        
        MtasUimaParser sut = new MtasUimaParser(project, annotationSchemaService,
                featureIndexingSupportRegistry);
        MtasTokenCollection tc = sut.createTokenCollection(jcas.getCas());
        
        MtasUtils.print(tc);
        
        List<MtasToken> tokens = new ArrayList<>();
        tc.iterator().forEachRemaining(tokens::add);

        assertThat(tokens)
            .filteredOn(t -> t.getPrefix().startsWith("Named_Entity"))
            .extracting(MtasToken::getPrefix)
            .containsExactly("Named_Entity", "Named_Entity.value");

        assertThat(tokens)
            .filteredOn(t -> t.getPrefix().startsWith("Named_Entity"))
            .extracting(MtasToken::getPostfix)
            .containsExactly("", "PER");
    }
    
    @Test
    public void testZeroWidthSpanNotIndexed() throws Exception
    {
        TokenBuilder<Token, Sentence> builder = TokenBuilder.create(Token.class, Sentence.class);
        builder.buildTokens(jcas, "This is a test . \n This is sentence two .");

        NamedEntity zeroWidthNe = new NamedEntity(jcas, 4, 4);
        zeroWidthNe.setValue("OTH");
        zeroWidthNe.addToIndexes();
        
        AnnotationLayer layer = new AnnotationLayer(NamedEntity.class.getName(),
                "Named Entity", SPAN_TYPE, project, true, TOKENS, NO_OVERLAP);
        when(annotationSchemaService.listAnnotationLayer(any(Project.class)))
                .thenReturn(asList(layer));

        when(annotationSchemaService.listAnnotationFeature(any(AnnotationLayer.class)))
                .thenReturn(asList(
                        new AnnotationFeature(1l, layer, "value", CAS.TYPE_NAME_STRING),
                        new AnnotationFeature(2l, layer, "identifier", CAS.TYPE_NAME_STRING)));
        
        MtasUimaParser sut = new MtasUimaParser(project, annotationSchemaService,
                featureIndexingSupportRegistry);
        MtasTokenCollection tc = sut.createTokenCollection(jcas.getCas());
        
        MtasUtils.print(tc);
        
        List<MtasToken> tokens = new ArrayList<>();
        tc.iterator().forEachRemaining(tokens::add);

        assertThat(tokens)
            .filteredOn(t -> t.getPrefix().startsWith("Named_Entity"))
            .extracting(MtasToken::getPrefix)
            .isEmpty();
    }

    
    @Test
    public void testDependencyRelation() throws Exception
    {
        // Set up document with a dummy dependency relation
        jcas.setDocumentText("a b");
        Token t1 = new Token(jcas, 0, 1);
        t1.addToIndexes();
        
        POS p1 = new POS(jcas, t1.getBegin(), t1.getEnd());
        p1.setPosValue("A");
        t1.setPos(p1);
        p1.addToIndexes();

        Token t2 = new Token(jcas, 2, 3);
        t2.addToIndexes();

        POS p2 = new POS(jcas, t2.getBegin(), t2.getEnd());
        p2.setPosValue("B");
        t2.setPos(p2);
        p2.addToIndexes();
        
        Dependency d1 = new Dependency(jcas, t2.getBegin(), t2.getEnd());
        d1.setDependent(t2);
        d1.setGovernor(t1);
        d1.addToIndexes();
        
        // Set up annotation schema with POS and Dependency
        AnnotationLayer tokenLayer = new AnnotationLayer(Token.class.getName(), "Token",
                SPAN_TYPE, project, true, SINGLE_TOKEN, NO_OVERLAP);
        tokenLayer.setId(1l);
        AnnotationFeature tokenLayerPos = new AnnotationFeature(1l, tokenLayer, "pos",
                POS.class.getName());
        
        AnnotationLayer posLayer = new AnnotationLayer(POS.class.getName(), "POS",
                SPAN_TYPE, project, true, SINGLE_TOKEN, NO_OVERLAP);
        posLayer.setId(2l);
        AnnotationFeature posLayerValue = new AnnotationFeature(1l, posLayer, "PosValue",
                CAS.TYPE_NAME_STRING);
        
        AnnotationLayer depLayer = new AnnotationLayer(Dependency.class.getName(),
                "Dependency", RELATION_TYPE, project, true, SINGLE_TOKEN, NO_OVERLAP);
        depLayer.setId(3l);
        depLayer.setAttachType(tokenLayer);
        depLayer.setAttachFeature(tokenLayerPos);
        AnnotationFeature dependencyLayerGovernor = new AnnotationFeature(2l, depLayer,
                "Governor", Token.class.getName());
        AnnotationFeature dependencyLayerDependent = new AnnotationFeature(3l, depLayer,
                "Dependent", Token.class.getName());
            
        when(annotationSchemaService.listAnnotationLayer(any(Project.class)))
                .thenReturn(asList(tokenLayer, posLayer, depLayer));

        when(annotationSchemaService.listAnnotationFeature(tokenLayer))
                .thenReturn(asList(tokenLayerPos));

        when(annotationSchemaService.listAnnotationFeature(posLayer))
                .thenReturn(asList(posLayerValue));

        when(annotationSchemaService.listAnnotationFeature(depLayer))
                .thenReturn(asList(dependencyLayerGovernor, dependencyLayerDependent));

        when(annotationSchemaService.getAdapter(posLayer)).thenReturn(new SpanAdapter(
                featureSupportRegistry, null, posLayer, asList(posLayerValue), null));

        when(annotationSchemaService.getAdapter(depLayer)).thenReturn(new RelationAdapter(
                featureSupportRegistry, null, depLayer, depLayer.getId(), depLayer.getName(),
                WebAnnoConst.FEAT_REL_TARGET, WebAnnoConst.FEAT_REL_SOURCE,
                depLayer.getAttachFeature().getName(), depLayer.getAttachType().getName(),
                asList(dependencyLayerGovernor, dependencyLayerDependent)));
        
        MtasUimaParser sut = new MtasUimaParser(project, annotationSchemaService,
                featureIndexingSupportRegistry);
        MtasTokenCollection tc = sut.createTokenCollection(jcas.getCas());
        
        MtasUtils.print(tc);
        
        List<MtasToken> tokens = new ArrayList<>();
        tc.iterator().forEachRemaining(tokens::add);

        assertThat(tokens)
            .filteredOn(t -> t.getPrefix().startsWith("Dependency"))
            .extracting(t -> t.getPrefix() + "=" + t.getPostfix())
            .containsExactly(
                    "Dependency=b", 
                    "Dependency-source=a", 
                    "Dependency-source.PosValue=A",
                    "Dependency-target=b", 
                    "Dependency-target.PosValue=B");
    }
}

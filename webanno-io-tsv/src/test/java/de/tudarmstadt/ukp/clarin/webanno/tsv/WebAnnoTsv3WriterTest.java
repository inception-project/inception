/*
 * Copyright 2012
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
package de.tudarmstadt.ukp.clarin.webanno.tsv;

import static java.util.Arrays.asList;
import static org.apache.uima.fit.factory.AnalysisEngineFactory.createEngineDescription;
import static org.apache.uima.fit.util.JCasUtil.select;
import static org.junit.Assert.assertEquals;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.io.FileUtils;
import org.apache.uima.UIMAException;
import org.apache.uima.analysis_engine.AnalysisEngineDescription;
import org.apache.uima.analysis_engine.AnalysisEngineProcessException;
import org.apache.uima.cas.CAS;
import org.apache.uima.cas.FeatureStructure;
import org.apache.uima.cas.Type;
import org.apache.uima.cas.text.AnnotationFS;
import org.apache.uima.fit.factory.JCasFactory;
import org.apache.uima.fit.factory.TypeSystemDescriptionFactory;
import org.apache.uima.fit.pipeline.SimplePipeline;
import org.apache.uima.fit.testing.factory.TokenBuilder;
import org.apache.uima.fit.util.FSCollectionFactory;
import org.apache.uima.fit.util.FSUtil;
import org.apache.uima.jcas.JCas;
import org.apache.uima.resource.ResourceInitializationException;
import org.apache.uima.resource.metadata.TypeSystemDescription;
import org.apache.uima.util.CasCreationUtils;
import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;

import de.tudarmstadt.ukp.dkpro.core.api.metadata.type.DocumentMetaData;
import de.tudarmstadt.ukp.dkpro.core.api.ner.type.NamedEntity;
import de.tudarmstadt.ukp.dkpro.core.api.segmentation.type.Sentence;
import de.tudarmstadt.ukp.dkpro.core.api.segmentation.type.Token;
import de.tudarmstadt.ukp.dkpro.core.testing.DkproTestContext;

public class WebAnnoTsv3WriterTest
{
    @Test
    public void testZeroLengthSpansWithoutFeatureValues() throws Exception
    {
        JCas jcas = makeJCasOneSentence();
        
        // One at the beginning
        new NamedEntity(jcas, 0, 0).addToIndexes();

        // One at the end
        new NamedEntity(jcas, jcas.getDocumentText().length(), jcas.getDocumentText().length())
                .addToIndexes();

        writeAndAssertEquals(jcas, 
                WebannoTsv3Writer.PARAM_SPAN_LAYERS, asList(NamedEntity.class));
    }
    
    @Test
    public void testZeroLengthSpansWithFeatureValues() throws Exception
    {
        JCas jcas = makeJCasOneSentence();
        
        // One at the beginning
        NamedEntity ne1 = new NamedEntity(jcas, 0, 0);
        ne1.setValue("PERSON");
        ne1.addToIndexes();

        // One at the end
        NamedEntity ne2 = new NamedEntity(jcas, jcas.getDocumentText().length(),
                jcas.getDocumentText().length());
        ne2.setValue("ORG");
        ne2.addToIndexes();

        writeAndAssertEquals(jcas, 
                WebannoTsv3Writer.PARAM_SPAN_LAYERS, asList(NamedEntity.class));
    }

    @Test
    public void testZeroLengthSpansWithoutFeatures() throws Exception
    {
        JCas jcas = makeJCasOneSentence();
        
        CAS cas = jcas.getCas();
        
        Type simpleSpanType = cas.getTypeSystem().getType("webanno.custom.SimpleSpan");
        
        // One at the beginning
        AnnotationFS fs1 = cas.createAnnotation(simpleSpanType, 0, 0);
        cas.addFsToIndexes(fs1);

        // One at the end
        AnnotationFS fs2 = cas.createAnnotation(simpleSpanType, jcas.getDocumentText().length(),
                jcas.getDocumentText().length());
        cas.addFsToIndexes(fs2);

        writeAndAssertEquals(jcas, 
                WebannoTsv3Writer.PARAM_SPAN_LAYERS, asList("webanno.custom.SimpleSpan"));
    }

    @Test
    public void testZeroLengthStackedSpansWithoutFeatures() throws Exception
    {
        JCas jcas = makeJCasOneSentence();
        
        CAS cas = jcas.getCas();
        
        Type simpleSpanType = cas.getTypeSystem().getType("webanno.custom.SimpleSpan");
        
        // Two at the beginning
        AnnotationFS fs1 = cas.createAnnotation(simpleSpanType, 0, 0);
        cas.addFsToIndexes(fs1);
        AnnotationFS fs2 = cas.createAnnotation(simpleSpanType, 0, 0);
        cas.addFsToIndexes(fs2);

        // Two at the end
        AnnotationFS fs3 = cas.createAnnotation(simpleSpanType, jcas.getDocumentText().length(),
                jcas.getDocumentText().length());
        cas.addFsToIndexes(fs3);
        AnnotationFS fs4 = cas.createAnnotation(simpleSpanType, jcas.getDocumentText().length(),
                jcas.getDocumentText().length());
        cas.addFsToIndexes(fs4);

        writeAndAssertEquals(jcas, 
                WebannoTsv3Writer.PARAM_SPAN_LAYERS, asList("webanno.custom.SimpleSpan"));
    }

    @Test
    public void testTokenBoundedSpanWithFeatureValue() throws Exception
    {
        JCas jcas = makeJCasOneSentence();
        
        int n = 0;
        for (Token t : select(jcas, Token.class)) {
            NamedEntity ne = new NamedEntity(jcas, t.getBegin(), t.getEnd());
            ne.setValue("NE " + n);
            ne.addToIndexes();
            n++;
        }
        
        writeAndAssertEquals(jcas, 
                WebannoTsv3Writer.PARAM_SPAN_LAYERS, asList(NamedEntity.class));
    }

    @Test
    public void testTokenBoundedStackedSpanWithFeatureValue() throws Exception
    {
        JCas jcas = makeJCasOneSentence();
        
        for (Token t : select(jcas, Token.class)) {
            NamedEntity ne1 = new NamedEntity(jcas, t.getBegin(), t.getEnd());
            ne1.setValue("NE");
            ne1.addToIndexes();
            
            NamedEntity ne2 = new NamedEntity(jcas, t.getBegin(), t.getEnd());
            ne2.setValue("NE");
            ne2.addToIndexes();
        }
        
        writeAndAssertEquals(jcas, 
                WebannoTsv3Writer.PARAM_SPAN_LAYERS, asList(NamedEntity.class));
    }

    @Test
    public void testTokenBoundedSpanWithoutFeatureValue() throws Exception
    {
        JCas jcas = makeJCasOneSentence();
        
        for (Token t : select(jcas, Token.class)) {
            NamedEntity ne = new NamedEntity(jcas, t.getBegin(), t.getEnd());
            ne.addToIndexes();
        }
        
        writeAndAssertEquals(jcas, 
                WebannoTsv3Writer.PARAM_SPAN_LAYERS, asList(NamedEntity.class));
    }

    @Test
    public void testTokenBoundedSpanWithNastyFeatureValue() throws Exception
    {
        JCas jcas = makeJCasOneSentence();
        
        for (Token t : select(jcas, Token.class)) {
            NamedEntity ne = new NamedEntity(jcas, t.getBegin(), t.getEnd());
            ne.setValue("de.tudarmstadt.ukp.dkpro.core.api.ner.type.NamedEntity:value");
            ne.addToIndexes();
        }
        
        writeAndAssertEquals(jcas, 
                WebannoTsv3Writer.PARAM_SPAN_LAYERS, asList(NamedEntity.class));
    }

    @Test
    public void testTokenBoundedSpanWithUnderscoreFeatureValue() throws Exception
    {
        JCas jcas = makeJCasOneSentence();
        
        for (Token t : select(jcas, Token.class)) {
            NamedEntity ne = new NamedEntity(jcas, t.getBegin(), t.getEnd());
            ne.setValue("_");
            ne.addToIndexes();
        }
        
        writeAndAssertEquals(jcas, 
                WebannoTsv3Writer.PARAM_SPAN_LAYERS, asList(NamedEntity.class));
    }

    @Test
    public void testTokenBoundedSpanWithAsteriskFeatureValue() throws Exception
    {
        JCas jcas = makeJCasOneSentence();
        
        for (Token t : select(jcas, Token.class)) {
            NamedEntity ne = new NamedEntity(jcas, t.getBegin(), t.getEnd());
            ne.setValue("*");
            ne.addToIndexes();
        }
        
        writeAndAssertEquals(jcas, 
                WebannoTsv3Writer.PARAM_SPAN_LAYERS, asList(NamedEntity.class));
    }
    
    @Test
    public void testSingleTokenWithoutFeatureValue()
        throws Exception
    {
        JCas jCas = makeJCasOneSentence();
        NamedEntity neToken = new NamedEntity(jCas, 0, 4);
        neToken.addToIndexes();

        writeAndAssertEquals(jCas, WebannoTsv3Writer.PARAM_SPAN_LAYERS, asList(NamedEntity.class));
    }

    @Test
    public void testTokenBoundedBioLookAlike() throws Exception
    {
        JCas jcas = makeJCasOneSentence();
        
        int n = 0;
        for (Token t : select(jcas, Token.class)) {
            NamedEntity ne = new NamedEntity(jcas, t.getBegin(), t.getEnd());
            ne.setValue(((n == 0) ? "B-" : "I-") + "NOTBIO!");
            ne.addToIndexes();
            n++;
        }
        
        writeAndAssertEquals(jcas, 
                WebannoTsv3Writer.PARAM_SPAN_LAYERS, asList(NamedEntity.class));
    }

    @Test
    public void testTokenBoundedStackedLookAlike() throws Exception
    {
        JCas jcas = makeJCasOneSentence();
        
        int n = 0;
        for (Token t : select(jcas, Token.class)) {
            NamedEntity ne = new NamedEntity(jcas, t.getBegin(), t.getEnd());
            ne.setValue("NOTSTACKED[" + n + "]");
            ne.addToIndexes();
            n++;
        }
        
        writeAndAssertEquals(jcas, 
                WebannoTsv3Writer.PARAM_SPAN_LAYERS, asList(NamedEntity.class));
    }

    @Test
    public void testTokenBoundedSpanWithSpecialSymbolsValue() throws Exception
    {
        JCas jcas = makeJCasOneSentence();
        
        for (Token t : select(jcas, Token.class)) {
            NamedEntity ne = new NamedEntity(jcas, t.getBegin(), t.getEnd());
            ne.setValue("#*'\"`´\t:;{}|[ ]()\\§$%?=&_\n");
            ne.addToIndexes();
        }
        
        writeAndAssertEquals(jcas, 
                WebannoTsv3Writer.PARAM_SPAN_LAYERS, asList(NamedEntity.class));
    }
    
    @Test
    public void testMultiTokenSpanWithoutFeatureValue() throws Exception
    {
        JCas jcas = makeJCasOneSentence();
        
        NamedEntity ne = new NamedEntity(jcas, 0, jcas.getDocumentText().length());
        ne.addToIndexes();
        
        writeAndAssertEquals(jcas, 
                WebannoTsv3Writer.PARAM_SPAN_LAYERS, asList(NamedEntity.class));
    }

    @Test
    public void testMultiTokenStackedSpanWithoutFeatureValue() throws Exception
    {
        JCas jcas = makeJCasOneSentence();
        
        NamedEntity ne1 = new NamedEntity(jcas, 0, jcas.getDocumentText().length());
        ne1.addToIndexes();

        NamedEntity ne2 = new NamedEntity(jcas, 0, jcas.getDocumentText().length());
        ne2.addToIndexes();

        writeAndAssertEquals(jcas, 
                WebannoTsv3Writer.PARAM_SPAN_LAYERS, asList(NamedEntity.class));
    }

    @Test
    public void testMultiTokenSpanWithFeatureValue() throws Exception
    {
        JCas jcas = makeJCasOneSentence();
        
        NamedEntity ne = new NamedEntity(jcas, 0, jcas.getDocumentText().length());
        ne.setValue("PERSON");
        ne.addToIndexes();
        
        writeAndAssertEquals(jcas, 
                WebannoTsv3Writer.PARAM_SPAN_LAYERS, asList(NamedEntity.class));
    }
    
    @Test
    public void testMultiTokenStackedSpanWithFeatureValue() throws Exception
    {
        JCas jcas = makeJCasOneSentence();
        
        NamedEntity ne1 = new NamedEntity(jcas, 0, jcas.getDocumentText().length());
        ne1.setValue("PERSON");
        ne1.addToIndexes();

        NamedEntity ne2 = new NamedEntity(jcas, 0, jcas.getDocumentText().length());
        ne2.setValue("LOCATION");
        ne2.addToIndexes();

        writeAndAssertEquals(jcas, 
                WebannoTsv3Writer.PARAM_SPAN_LAYERS, asList(NamedEntity.class));
    }

    @Test
    public void testCrossSentenceSpanWithoutFeatureValue() throws Exception
    {
        JCas jcas = makeJCasTwoSentences();
        
        NamedEntity ne = new NamedEntity(jcas, 0, jcas.getDocumentText().length());
        ne.addToIndexes();
        
        writeAndAssertEquals(jcas, 
                WebannoTsv3Writer.PARAM_SPAN_LAYERS, asList(NamedEntity.class));
    }

    @Test
    public void testCrossSentenceSpanWithFeatureValue() throws Exception
    {
        JCas jcas = makeJCasTwoSentences();
        
        NamedEntity ne = new NamedEntity(jcas, 0, jcas.getDocumentText().length());
        ne.setValue("PERSON");
        ne.addToIndexes();
        
        writeAndAssertEquals(jcas, 
                WebannoTsv3Writer.PARAM_SPAN_LAYERS, asList(NamedEntity.class));
    }
    
    @Test
    public void testSingleTokenRelationWithoutFeatureValue() throws Exception
    {
        JCas jcas = makeJCasOneSentence();
        CAS cas = jcas.getCas();
        
        List<Token> tokens = new ArrayList<>(select(jcas, Token.class));
        
        Token gov = tokens.get(0);
        Token dep = tokens.get(tokens.size() - 1);

        Type relationType = cas.getTypeSystem().getType("webanno.custom.Relation");
        
        // One at the beginning
        AnnotationFS fs1 = cas.createAnnotation(relationType, dep.getBegin(), dep.getEnd());
        FSUtil.setFeature(fs1, "Governor", gov);
        FSUtil.setFeature(fs1, "Dependent", dep);
        cas.addFsToIndexes(fs1);
        
        writeAndAssertEquals(jcas, 
                WebannoTsv3Writer.PARAM_RELATION_LAYERS, asList("webanno.custom.Relation"));
    }

    @Test
    public void testSingleNonTokenRelationWithoutFeatureValue() throws Exception
    {
        JCas jcas = makeJCasOneSentence();
        CAS cas = jcas.getCas();
        
        List<Token> tokens = new ArrayList<>(select(jcas, Token.class));
        
        Token t1 = tokens.get(0);
        Token t2 = tokens.get(tokens.size() - 1);
        
        NamedEntity gov = new NamedEntity(jcas, t1.getBegin(), t1.getEnd());
        gov.addToIndexes();
        NamedEntity dep =  new NamedEntity(jcas, t2.getBegin(), t2.getEnd());
        dep.addToIndexes();

        Type relationType = cas.getTypeSystem().getType("webanno.custom.Relation");
        
        // One at the beginning
        AnnotationFS fs1 = cas.createAnnotation(relationType, dep.getBegin(), dep.getEnd());
        FSUtil.setFeature(fs1, "Governor", gov);
        FSUtil.setFeature(fs1, "Dependent", dep);
        cas.addFsToIndexes(fs1);
        
        writeAndAssertEquals(jcas, 
                WebannoTsv3Writer.PARAM_SPAN_LAYERS, asList(NamedEntity.class),
                WebannoTsv3Writer.PARAM_RELATION_LAYERS, asList("webanno.custom.Relation"));
    }

    @Test
    public void testSingleStackedNonTokenRelationWithoutFeatureValue() throws Exception
    {
        JCas jcas = makeJCasOneSentence();
        CAS cas = jcas.getCas();
        
        List<Token> tokens = new ArrayList<>(select(jcas, Token.class));
        
        Token t1 = tokens.get(0);
        Token t2 = tokens.get(tokens.size() - 1);
        
        NamedEntity gov = new NamedEntity(jcas, t1.getBegin(), t1.getEnd());
        gov.addToIndexes();
        new NamedEntity(jcas, t1.getBegin(), t1.getEnd()).addToIndexes();

        NamedEntity dep =  new NamedEntity(jcas, t2.getBegin(), t2.getEnd());
        dep.addToIndexes();
        new NamedEntity(jcas, t2.getBegin(), t2.getEnd()).addToIndexes();

        Type relationType = cas.getTypeSystem().getType("webanno.custom.Relation");
        
        // One at the beginning
        AnnotationFS fs1 = cas.createAnnotation(relationType, dep.getBegin(), dep.getEnd());
        FSUtil.setFeature(fs1, "Governor", gov);
        FSUtil.setFeature(fs1, "Dependent", dep);
        cas.addFsToIndexes(fs1);
        
        writeAndAssertEquals(jcas, 
                WebannoTsv3Writer.PARAM_SPAN_LAYERS, asList(NamedEntity.class),
                WebannoTsv3Writer.PARAM_RELATION_LAYERS, asList("webanno.custom.Relation"));
    }

    @Test
    public void testSingleStackedNonTokenOverlappingRelationWithoutFeatureValue() throws Exception
    {
        JCas jcas = makeJCasOneSentence();
        CAS cas = jcas.getCas();
        
        List<Token> tokens = new ArrayList<>(select(jcas, Token.class));
        
        Token t1 = tokens.get(0);
        Token t2 = tokens.get(tokens.size() - 1);
        
        NamedEntity gov = new NamedEntity(jcas, t1.getBegin(), t2.getEnd());
        gov.addToIndexes();
        new NamedEntity(jcas, t1.getBegin(), t2.getEnd()).addToIndexes();

        NamedEntity dep =  new NamedEntity(jcas, t2.getBegin(), t2.getEnd());
        dep.addToIndexes();
        new NamedEntity(jcas, t2.getBegin(), t2.getEnd()).addToIndexes();

        Type relationType = cas.getTypeSystem().getType("webanno.custom.Relation");
        
        // One at the beginning
        AnnotationFS fs1 = cas.createAnnotation(relationType, dep.getBegin(), dep.getEnd());
        FSUtil.setFeature(fs1, "Governor", gov);
        FSUtil.setFeature(fs1, "Dependent", dep);
        cas.addFsToIndexes(fs1);
        
        writeAndAssertEquals(jcas, 
                WebannoTsv3Writer.PARAM_SPAN_LAYERS, asList(NamedEntity.class),
                WebannoTsv3Writer.PARAM_RELATION_LAYERS, asList("webanno.custom.Relation"));
    }

    @Test
    public void testSingleNonTokenRelationWithoutFeature() throws Exception
    {
        JCas jcas = makeJCasOneSentence();
        CAS cas = jcas.getCas();
        
        List<Token> tokens = new ArrayList<>(select(jcas, Token.class));
        
        Token t1 = tokens.get(0);
        Token t2 = tokens.get(tokens.size() - 1);
        
        NamedEntity gov = new NamedEntity(jcas, t1.getBegin(), t1.getEnd());
        gov.addToIndexes();
        NamedEntity dep =  new NamedEntity(jcas, t2.getBegin(), t2.getEnd());
        dep.addToIndexes();

        Type relationType = cas.getTypeSystem().getType("webanno.custom.SimpleRelation");
        
        // One at the beginning
        AnnotationFS fs1 = cas.createAnnotation(relationType, dep.getBegin(), dep.getEnd());
        FSUtil.setFeature(fs1, "Governor", gov);
        FSUtil.setFeature(fs1, "Dependent", dep);
        cas.addFsToIndexes(fs1);
        
        writeAndAssertEquals(jcas, 
                WebannoTsv3Writer.PARAM_SPAN_LAYERS, asList(NamedEntity.class),
                WebannoTsv3Writer.PARAM_RELATION_LAYERS, asList("webanno.custom.SimpleRelation"));
    }

    @Test
    public void testSingleNonMultiTokenRelationWithoutFeatureValue() throws Exception
    {
        JCas jcas = makeJCasOneSentence();
        CAS cas = jcas.getCas();
        
        List<Token> tokens = new ArrayList<>(select(jcas, Token.class));
        
        Token t1 = tokens.get(0);
        Token t2 = tokens.get(1);
        Token t3 = tokens.get(2);
        Token t4 = tokens.get(3);
        
        NamedEntity gov = new NamedEntity(jcas, t1.getBegin(), t2.getEnd());
        gov.addToIndexes();
        NamedEntity dep =  new NamedEntity(jcas, t3.getBegin(), t4.getEnd());
        dep.addToIndexes();

        Type relationType = cas.getTypeSystem().getType("webanno.custom.Relation");
        
        // One at the beginning
        AnnotationFS fs1 = cas.createAnnotation(relationType, dep.getBegin(), dep.getEnd());
        FSUtil.setFeature(fs1, "Governor", gov);
        FSUtil.setFeature(fs1, "Dependent", dep);
        cas.addFsToIndexes(fs1);
        
        writeAndAssertEquals(jcas, 
                WebannoTsv3Writer.PARAM_SPAN_LAYERS, asList(NamedEntity.class),
                WebannoTsv3Writer.PARAM_RELATION_LAYERS, asList("webanno.custom.Relation"));
    }

    @Ignore("Relations between different layers not supported in WebAnno TSV 3 atm")
    @Test
    public void testSingleMixedRelationWithoutFeatureValue() throws Exception
    {
        JCas jcas = makeJCasOneSentence();
        CAS cas = jcas.getCas();
        
        List<Token> tokens = new ArrayList<>(select(jcas, Token.class));
        
        Token gov = tokens.get(0);
        
        Token t2 = tokens.get(tokens.size() - 1);
        NamedEntity dep =  new NamedEntity(jcas, t2.getBegin(), t2.getEnd());
        dep.addToIndexes();

        Type relationType = cas.getTypeSystem().getType("webanno.custom.Relation");
        
        // One at the beginning
        AnnotationFS fs1 = cas.createAnnotation(relationType, dep.getBegin(), dep.getEnd());
        FSUtil.setFeature(fs1, "Governor", gov);
        FSUtil.setFeature(fs1, "Dependent", dep);
        cas.addFsToIndexes(fs1);
        
        writeAndAssertEquals(jcas, 
                WebannoTsv3Writer.PARAM_SPAN_LAYERS, asList(NamedEntity.class),
                WebannoTsv3Writer.PARAM_RELATION_LAYERS, asList("webanno.custom.Relation"));
    }

    @Test
    public void testSingleTokenRelationWithFeatureValue() throws Exception
    {
        JCas jcas = makeJCasOneSentence();
        CAS cas = jcas.getCas();
        
        List<Token> tokens = new ArrayList<>(select(jcas, Token.class));
        
        Token gov = tokens.get(0);
        Token dep = tokens.get(tokens.size() - 1);

        Type relationType = cas.getTypeSystem().getType("webanno.custom.Relation");
        
        // One at the beginning
        AnnotationFS fs1 = cas.createAnnotation(relationType, dep.getBegin(), dep.getEnd());
        FSUtil.setFeature(fs1, "Governor", gov);
        FSUtil.setFeature(fs1, "Dependent", dep);
        FSUtil.setFeature(fs1, "value", "nsubj");
        cas.addFsToIndexes(fs1);
        
        writeAndAssertEquals(jcas, 
                WebannoTsv3Writer.PARAM_RELATION_LAYERS, asList("webanno.custom.Relation"));
    }

    @Test
    public void testSimpleSlotFeature() throws Exception
    {
        JCas jcas = makeJCasOneSentence();
        CAS cas = jcas.getCas();
        
        List<Token> tokens = new ArrayList<>(select(jcas, Token.class));
        
        Token t1 = tokens.get(0);
        Token t2 = tokens.get(1);
        Token t3 = tokens.get(2);
        
        Type type = cas.getTypeSystem().getType("webanno.custom.SimpleSpan");
        AnnotationFS s2 = cas.createAnnotation(type, t2.getBegin(), t2.getEnd());
        cas.addFsToIndexes(s2);
        AnnotationFS s3 = cas.createAnnotation(type, t3.getBegin(), t3.getEnd());
        cas.addFsToIndexes(s3);

        FeatureStructure link1 = makeLinkFS(jcas, "p1", s2);
        FeatureStructure link2 = makeLinkFS(jcas, "p2", s3);
        
        makeLinkHostFS(jcas, t1.getBegin(), t1.getEnd(), link1, link2);
        
        writeAndAssertEquals(jcas, 
                WebannoTsv3Writer.PARAM_SLOT_FEATS, asList("webanno.custom.SimpleLinkHost:links"),
                WebannoTsv3Writer.PARAM_SPAN_LAYERS, asList("webanno.custom.SimpleSpan", 
                        "webanno.custom.SimpleLinkHost"),
                WebannoTsv3Writer.PARAM_LINK_TYPES, asList("webanno.custom.LinkType"),
                WebannoTsv3Writer.PARAM_SLOT_TARGETS, asList("webanno.custom.SimpleSpan"));
    }

    @Test
    public void testSimpleSameRoleSlotFeature() throws Exception
    {
        JCas jcas = makeJCasOneSentence();
        CAS cas = jcas.getCas();
        
        List<Token> tokens = new ArrayList<>(select(jcas, Token.class));
        
        Token t1 = tokens.get(0);
        Token t2 = tokens.get(1);
        Token t3 = tokens.get(2);
        
        Type type = cas.getTypeSystem().getType("webanno.custom.SimpleSpan");
        AnnotationFS s2 = cas.createAnnotation(type, t2.getBegin(), t2.getEnd());
        cas.addFsToIndexes(s2);
        AnnotationFS s3 = cas.createAnnotation(type, t3.getBegin(), t3.getEnd());
        cas.addFsToIndexes(s3);

        FeatureStructure link1 = makeLinkFS(jcas, "p1", s2);
        FeatureStructure link2 = makeLinkFS(jcas, "p1", s3);
        
        makeLinkHostFS(jcas, t1.getBegin(), t1.getEnd(), link1, link2);
        
        writeAndAssertEquals(jcas, 
                WebannoTsv3Writer.PARAM_SLOT_FEATS, asList("webanno.custom.SimpleLinkHost:links"),
                WebannoTsv3Writer.PARAM_SPAN_LAYERS, asList("webanno.custom.SimpleSpan", 
                        "webanno.custom.SimpleLinkHost"),
                WebannoTsv3Writer.PARAM_LINK_TYPES, asList("webanno.custom.LinkType"),
                WebannoTsv3Writer.PARAM_SLOT_TARGETS, asList("webanno.custom.SimpleSpan"));
    }

    @Test
    public void testSimpleCrossSenenceSlotFeature() throws Exception
    {
        JCas jcas = makeJCasTwoSentences();
        CAS cas = jcas.getCas();
        
        List<Token> tokens = new ArrayList<>(select(jcas, Token.class));
        
        Token t1 = tokens.get(0);
        Token t2 = tokens.get(1);
        Token t3 = tokens.get(6);
        
        Type type = cas.getTypeSystem().getType("webanno.custom.SimpleSpan");
        AnnotationFS s2 = cas.createAnnotation(type, t2.getBegin(), t2.getEnd());
        cas.addFsToIndexes(s2);
        AnnotationFS s3 = cas.createAnnotation(type, t3.getBegin(), t3.getEnd());
        cas.addFsToIndexes(s3);

        FeatureStructure link1 = makeLinkFS(jcas, "p1", s2);
        FeatureStructure link2 = makeLinkFS(jcas, "p2", s3);
        
        makeLinkHostFS(jcas, t1.getBegin(), t1.getEnd(), link1, link2);
        
        writeAndAssertEquals(jcas, 
                WebannoTsv3Writer.PARAM_SLOT_FEATS, asList("webanno.custom.SimpleLinkHost:links"),
                WebannoTsv3Writer.PARAM_SPAN_LAYERS, asList("webanno.custom.SimpleSpan", 
                        "webanno.custom.SimpleLinkHost"),
                WebannoTsv3Writer.PARAM_LINK_TYPES, asList("webanno.custom.LinkType"),
                WebannoTsv3Writer.PARAM_SLOT_TARGETS, asList("webanno.custom.SimpleSpan"));
    }
    
    @Test
    public void testMultiTokenSlotFeature() throws Exception
    {
        JCas jcas = makeJCasOneSentence();
        CAS cas = jcas.getCas();
        
        List<Token> tokens = new ArrayList<>(select(jcas, Token.class));
        
        Token t1 = tokens.get(0);
        Token t2 = tokens.get(1);
        Token t3 = tokens.get(2);
        Token t4 = tokens.get(3);
        Token t5 = tokens.get(4);
        
        Type type = cas.getTypeSystem().getType("webanno.custom.SimpleSpan");
        AnnotationFS s2 = cas.createAnnotation(type, t2.getBegin(), t3.getEnd());
        cas.addFsToIndexes(s2);
        AnnotationFS s3 = cas.createAnnotation(type, t4.getBegin(), t5.getEnd());
        cas.addFsToIndexes(s3);

        FeatureStructure link1 = makeLinkFS(jcas, "p1", s2);
        FeatureStructure link2 = makeLinkFS(jcas, "p2", s3);
        
        makeLinkHostFS(jcas, t1.getBegin(), t1.getEnd(), link1, link2);
        
        writeAndAssertEquals(jcas, 
                WebannoTsv3Writer.PARAM_SLOT_FEATS, asList("webanno.custom.SimpleLinkHost:links"),
                WebannoTsv3Writer.PARAM_SPAN_LAYERS, asList("webanno.custom.SimpleSpan", 
                        "webanno.custom.SimpleLinkHost"),
                WebannoTsv3Writer.PARAM_LINK_TYPES, asList("webanno.custom.LinkType"),
                WebannoTsv3Writer.PARAM_SLOT_TARGETS, asList("webanno.custom.SimpleSpan"));
    }
    
    @Test
    public void testMultiTokenStackedSlotFeature() throws Exception
    {
        JCas jcas = makeJCasOneSentence();
        CAS cas = jcas.getCas();
        
        List<Token> tokens = new ArrayList<>(select(jcas, Token.class));
        
        Token t1 = tokens.get(0);
        Token t2 = tokens.get(1);
        Token t3 = tokens.get(2);
        
        Type type = cas.getTypeSystem().getType("webanno.custom.SimpleSpan");
        AnnotationFS s2 = cas.createAnnotation(type, t2.getBegin(), t3.getEnd());
        cas.addFsToIndexes(s2);
        AnnotationFS s3 = cas.createAnnotation(type, t2.getBegin(), t3.getEnd());
        cas.addFsToIndexes(s3);

        FeatureStructure link1 = makeLinkFS(jcas, "p1", s2);
        FeatureStructure link2 = makeLinkFS(jcas, "p2", s3);
        
        makeLinkHostFS(jcas, t1.getBegin(), t1.getEnd(), link1, link2);
        
        writeAndAssertEquals(jcas, 
                WebannoTsv3Writer.PARAM_SLOT_FEATS, asList("webanno.custom.SimpleLinkHost:links"),
                WebannoTsv3Writer.PARAM_SPAN_LAYERS, asList("webanno.custom.SimpleSpan", 
                        "webanno.custom.SimpleLinkHost"),
                WebannoTsv3Writer.PARAM_LINK_TYPES, asList("webanno.custom.LinkType"),
                WebannoTsv3Writer.PARAM_SLOT_TARGETS, asList("webanno.custom.SimpleSpan"));
    }

    @Test
    public void testZeroLengthSlotFeature1() throws Exception
    {
        JCas jcas = makeJCasOneSentence();
        CAS cas = jcas.getCas();
        
        List<Token> tokens = new ArrayList<>(select(jcas, Token.class));
        
        Token t1 = tokens.get(0);
        Token t2 = tokens.get(1);
        Token t3 = tokens.get(2);
        
        Type type = cas.getTypeSystem().getType("webanno.custom.SimpleSpan");
        AnnotationFS s2 = cas.createAnnotation(type, t2.getBegin(), t3.getEnd());
        cas.addFsToIndexes(s2);
        AnnotationFS s3 = cas.createAnnotation(type, t2.getBegin(), t3.getEnd());
        cas.addFsToIndexes(s3);

        FeatureStructure link1 = makeLinkFS(jcas, "p1", s2);
        FeatureStructure link2 = makeLinkFS(jcas, "p2", s3);
        
        makeLinkHostFS(jcas, t1.getBegin(), t1.getBegin(), link1, link2);
        
        writeAndAssertEquals(jcas, 
                WebannoTsv3Writer.PARAM_SLOT_FEATS, asList("webanno.custom.SimpleLinkHost:links"),
                WebannoTsv3Writer.PARAM_SPAN_LAYERS, asList("webanno.custom.SimpleSpan", 
                        "webanno.custom.SimpleLinkHost"),
                WebannoTsv3Writer.PARAM_LINK_TYPES, asList("webanno.custom.LinkType"),
                WebannoTsv3Writer.PARAM_SLOT_TARGETS, asList("webanno.custom.SimpleSpan"));
    }    
    
    @Test
    public void testZeroLengthSlotFeature2() throws Exception
    {
        JCas jcas = makeJCasOneSentence();
        CAS cas = jcas.getCas();
        
        List<Token> tokens = new ArrayList<>(select(jcas, Token.class));
        
        Token t1 = tokens.get(0);
        Token t2 = tokens.get(1);
        Token t3 = tokens.get(2);
        
        Type type = cas.getTypeSystem().getType("webanno.custom.SimpleSpan");
        AnnotationFS s2 = cas.createAnnotation(type, t2.getBegin(), t3.getEnd());
        cas.addFsToIndexes(s2);
        AnnotationFS s3 = cas.createAnnotation(type, t3.getEnd(), t3.getEnd());
        cas.addFsToIndexes(s3);

        FeatureStructure link1 = makeLinkFS(jcas, "p1", s2);
        FeatureStructure link2 = makeLinkFS(jcas, "p2", s3);
        
        makeLinkHostFS(jcas, t1.getBegin(), t1.getEnd(), link1, link2);
        
        writeAndAssertEquals(jcas, 
                WebannoTsv3Writer.PARAM_SLOT_FEATS, asList("webanno.custom.SimpleLinkHost:links"),
                WebannoTsv3Writer.PARAM_SPAN_LAYERS, asList("webanno.custom.SimpleSpan", 
                        "webanno.custom.SimpleLinkHost"),
                WebannoTsv3Writer.PARAM_LINK_TYPES, asList("webanno.custom.LinkType"),
                WebannoTsv3Writer.PARAM_SLOT_TARGETS, asList("webanno.custom.SimpleSpan"));
    }    
    
    @Test
    public void testSimpleChain() throws Exception
    {
        JCas jcas = makeJCasOneSentence();
        CAS cas = jcas.getCas();
        
        List<Token> tokens = new ArrayList<>(select(jcas, Token.class));
        
        Token t1 = tokens.get(0);
        Token t2 = tokens.get(1);
        Token t3 = tokens.get(2);
        
        Type head = cas.getTypeSystem().getType("webanno.custom.SimpleChain");
        Type link = cas.getTypeSystem().getType("webanno.custom.SimpleLink");
        AnnotationFS l3 = cas.createAnnotation(link, t3.getBegin(), t3.getEnd());
        cas.addFsToIndexes(l3);
        AnnotationFS l2 = cas.createAnnotation(link, t2.getBegin(), t2.getEnd());
        FSUtil.setFeature(l2, "next", l3);
        cas.addFsToIndexes(l2);
        AnnotationFS l1 = cas.createAnnotation(link, t1.getBegin(), t1.getEnd());
        FSUtil.setFeature(l1, "next", l2);
        cas.addFsToIndexes(l1);
        FeatureStructure h = cas.createFS(head);
        FSUtil.setFeature(h, "first", l1);
        cas.addFsToIndexes(h);

        writeAndAssertEquals(jcas, 
                WebannoTsv3Writer.PARAM_CHAIN_LAYERS, asList("webanno.custom.Simple"));
    }

    @Test
    public void testSentenceWithLineBreak() throws Exception
    {
        JCas jcas = makeJCasOneSentence("This is\na test .");
                
        NamedEntity neToken = new NamedEntity(jcas, 0, 4);
        neToken.addToIndexes();
        
        writeAndAssertEquals(jcas, WebannoTsv3Writer.PARAM_SPAN_LAYERS, asList(NamedEntity.class));
    }

    @Test
    public void testSentenceWithTab() throws Exception
    {
        JCas jcas = makeJCasOneSentence("This is\ta test .");
                
        NamedEntity neToken = new NamedEntity(jcas, 0, 4);
        neToken.addToIndexes();
        
        writeAndAssertEquals(jcas, WebannoTsv3Writer.PARAM_SPAN_LAYERS, asList(NamedEntity.class));
    }

    @Test
    public void testTwoSentencesWithNoSpaceInBetween() throws Exception
    {
        TypeSystemDescription global = TypeSystemDescriptionFactory.createTypeSystemDescription();
        TypeSystemDescription local = TypeSystemDescriptionFactory
                .createTypeSystemDescriptionFromPath(
                        "src/test/resources/desc/type/webannoTestTypes.xml");
       
        TypeSystemDescription merged = CasCreationUtils.mergeTypeSystems(asList(global, local));
        
        JCas jcas = JCasFactory.createJCas(merged);
        
        DocumentMetaData.create(jcas).setDocumentId("doc");
        jcas.setDocumentText("onetwo");
        new Token(jcas, 0, 3).addToIndexes();
        new Sentence(jcas, 0, 3).addToIndexes();
        new Token(jcas, 3, 6).addToIndexes();
        new Sentence(jcas, 3, 6).addToIndexes();
        
        writeAndAssertEquals(jcas);
    }

    private void writeAndAssertEquals(JCas aJCas, Object... aParams)
        throws IOException, ResourceInitializationException, AnalysisEngineProcessException
    {
        String targetFolder = "target/test-output/" + testContext.getTestOutputFolderName();
        String referenceFolder = "src/test/resources/tsv3-suite/" + testContext.getMethodName();
        
        List<Object> params = new ArrayList<>();
        params.addAll(asList(aParams));
        params.add(WebannoTsv3Writer.PARAM_TARGET_LOCATION);
        params.add(targetFolder);
        
        AnalysisEngineDescription writer = createEngineDescription(WebannoTsv3Writer.class,
                params.toArray(new Object[params.size()]));
        
        SimplePipeline.runPipeline(aJCas, writer);
        
        String reference = FileUtils.readFileToString(new File(referenceFolder, "reference.tsv"),
                "UTF-8");
        
        String actual = FileUtils.readFileToString(new File(targetFolder, "doc.tsv"), "UTF-8");
        
        assertEquals(reference, actual);
    }

    private JCas makeJCasOneSentence() throws UIMAException
    {
        TypeSystemDescription global = TypeSystemDescriptionFactory.createTypeSystemDescription();
        TypeSystemDescription local = TypeSystemDescriptionFactory
                .createTypeSystemDescriptionFromPath(
                        "src/test/resources/desc/type/webannoTestTypes.xml");
       
        TypeSystemDescription merged = CasCreationUtils.mergeTypeSystems(asList(global, local));
        
        JCas jcas = JCasFactory.createJCas(merged);
        
        DocumentMetaData.create(jcas).setDocumentId("doc");
        
        TokenBuilder<Token, Sentence> tb = new TokenBuilder<>(Token.class,
                Sentence.class);
        tb.buildTokens(jcas, "This is a test .");
        
        return jcas;
    }

    private JCas makeJCasTwoSentences() throws UIMAException
    {
        TypeSystemDescription global = TypeSystemDescriptionFactory.createTypeSystemDescription();
        TypeSystemDescription local = TypeSystemDescriptionFactory
                .createTypeSystemDescriptionFromPath(
                        "src/test/resources/desc/type/webannoTestTypes.xml");
       
        TypeSystemDescription merged = CasCreationUtils.mergeTypeSystems(asList(global, local));
        
        JCas jcas = JCasFactory.createJCas(merged);
        
        DocumentMetaData.create(jcas).setDocumentId("doc");
        
        TokenBuilder<Token, Sentence> tb = new TokenBuilder<>(Token.class,
                Sentence.class);
        tb.buildTokens(jcas, "He loves her .\nShe loves him not .");
        
        assertEquals(2, select(jcas, Sentence.class).size());
        
        return jcas;
    }
    
    private JCas makeJCasOneSentence(String aText) throws UIMAException
    {
        TypeSystemDescription global = TypeSystemDescriptionFactory.createTypeSystemDescription();
        TypeSystemDescription local = TypeSystemDescriptionFactory
                .createTypeSystemDescriptionFromPath(
                        "src/test/resources/desc/type/webannoTestTypes.xml");
       
        TypeSystemDescription merged = CasCreationUtils.mergeTypeSystems(asList(global, local));
        
        JCas jcas = JCasFactory.createJCas(merged);
        
        DocumentMetaData.create(jcas).setDocumentId("doc");
        
        TokenBuilder<Token, Sentence> tb = new TokenBuilder<>(Token.class,
                Sentence.class);
        tb.buildTokens(jcas, aText);
        
        // Remove the sentences generated by the token builder which treats the line break as a
        // sentence break
        for (Sentence s : select(jcas, Sentence.class)) {
            s.removeFromIndexes();
        }
        
        // Add a new sentence covering the whole text
        new Sentence(jcas, 0, jcas.getDocumentText().length()).addToIndexes();
        
        return jcas;
    }


    public static AnnotationFS makeLinkHostFS(JCas aJCas, int aBegin, int aEnd,
            FeatureStructure... aLinks)
    {
        Type hostType = aJCas.getTypeSystem().getType("webanno.custom.SimpleLinkHost");
        AnnotationFS hostA1 = aJCas.getCas().createAnnotation(hostType, aBegin, aEnd);
        hostA1.setFeatureValue(hostType.getFeatureByBaseName("links"),
                FSCollectionFactory.createFSArray(aJCas, asList(aLinks)));
        aJCas.getCas().addFsToIndexes(hostA1);
        return hostA1;
    }

    public static FeatureStructure makeLinkFS(JCas aJCas, String aSlotLabel, AnnotationFS aTarget)
    {
        Type linkType = aJCas.getTypeSystem().getType("webanno.custom.LinkType");
        FeatureStructure linkA1 = aJCas.getCas().createFS(linkType);
        linkA1.setStringValue(linkType.getFeatureByBaseName("role"), aSlotLabel);
        linkA1.setFeatureValue(linkType.getFeatureByBaseName("target"), aTarget);
        aJCas.getCas().addFsToIndexes(linkA1);

        return linkA1;
    }

    @Rule
    public DkproTestContext testContext = new DkproTestContext();
}

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
package de.tudarmstadt.ukp.clarin.webanno.tsv;

import static java.util.Arrays.asList;
import static org.apache.uima.fit.factory.AnalysisEngineFactory.createEngineDescription;
import static org.apache.uima.fit.util.FSUtil.setFeature;
import static org.apache.uima.fit.util.JCasUtil.select;
import static org.apache.uima.fit.util.JCasUtil.toText;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assumptions.assumeFalse;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.IntStream;

import org.apache.commons.io.FileUtils;
import org.apache.uima.UIMAException;
import org.apache.uima.analysis_engine.AnalysisEngineDescription;
import org.apache.uima.analysis_engine.AnalysisEngineProcessException;
import org.apache.uima.cas.CAS;
import org.apache.uima.cas.FeatureStructure;
import org.apache.uima.cas.Type;
import org.apache.uima.cas.text.AnnotationFS;
import org.apache.uima.fit.factory.ConfigurationParameterFactory;
import org.apache.uima.fit.factory.JCasFactory;
import org.apache.uima.fit.factory.TypeSystemDescriptionFactory;
import org.apache.uima.fit.pipeline.SimplePipeline;
import org.apache.uima.fit.testing.factory.TokenBuilder;
import org.apache.uima.fit.util.FSCollectionFactory;
import org.apache.uima.fit.util.FSUtil;
import org.apache.uima.jcas.JCas;
import org.apache.uima.jcas.tcas.Annotation;
import org.apache.uima.resource.ResourceInitializationException;
import org.apache.uima.resource.metadata.TypeSystemDescription;
import org.apache.uima.util.CasCreationUtils;
import org.dkpro.core.io.xmi.XmiWriter;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInfo;
import org.junit.jupiter.api.TestMethodOrder;

import de.tudarmstadt.ukp.dkpro.core.api.lexmorph.type.morph.MorphologicalFeatures;
import de.tudarmstadt.ukp.dkpro.core.api.lexmorph.type.pos.POS;
import de.tudarmstadt.ukp.dkpro.core.api.lexmorph.type.pos.POS_NOUN;
import de.tudarmstadt.ukp.dkpro.core.api.metadata.type.DocumentMetaData;
import de.tudarmstadt.ukp.dkpro.core.api.ner.type.NamedEntity;
import de.tudarmstadt.ukp.dkpro.core.api.segmentation.type.Lemma;
import de.tudarmstadt.ukp.dkpro.core.api.segmentation.type.Sentence;
import de.tudarmstadt.ukp.dkpro.core.api.segmentation.type.Stem;
import de.tudarmstadt.ukp.dkpro.core.api.segmentation.type.Token;
import de.tudarmstadt.ukp.dkpro.core.api.syntax.type.dependency.Dependency;
import webanno.custom.Relation;
import webanno.custom.Span;

@TestMethodOrder(MethodOrderer.MethodName.class)
public abstract class WebAnnoTsv3WriterTestBase
{
    protected abstract AnalysisEngineDescription makeWriter()
        throws ResourceInitializationException;

    protected abstract String getSuiteName() throws ResourceInitializationException;

    protected abstract boolean isKnownToFail(String aMethodName);

    private TestInfo testInfo;

    @BeforeEach
    public void storeTestInfo(TestInfo aTestInfo)
    {
        testInfo = aTestInfo;
    }

    @Test
    public void testTokenAttachedAnnotationsWithValues() throws Exception
    {
        JCas jcas = makeJCasOneSentence();

        List<Token> tokens = new ArrayList<>(select(jcas, Token.class));

        Token t1 = tokens.get(0);

        Lemma l1 = new Lemma(jcas, t1.getBegin(), t1.getEnd());
        l1.setValue("lemma1");
        l1.addToIndexes();
        t1.setLemma(l1);

        MorphologicalFeatures m1 = new MorphologicalFeatures(jcas, t1.getBegin(), t1.getEnd());
        m1.setValue("morph");
        m1.setTense("tense1");
        m1.addToIndexes();
        t1.setMorph(m1);

        POS p1 = new POS(jcas, t1.getBegin(), t1.getEnd());
        p1.setPosValue("pos1");
        p1.addToIndexes();
        t1.setPos(p1);

        Stem s1 = new Stem(jcas, t1.getBegin(), t1.getEnd());
        s1.setValue("stem1");
        s1.addToIndexes();
        t1.setStem(s1);

        writeAndAssertEquals(jcas);
    }

    @Test
    public void testDependencyWithValues() throws Exception
    {
        JCas jcas = makeJCasOneSentence();

        List<Token> tokens = new ArrayList<>(select(jcas, Token.class));
        Token t1 = tokens.get(0);
        Token t2 = tokens.get(1);

        POS p1 = new POS(jcas, t1.getBegin(), t1.getEnd());
        p1.setPosValue("POS1");
        p1.addToIndexes();
        t1.setPos(p1);

        POS p2 = new POS(jcas, t2.getBegin(), t2.getEnd());
        p2.setPosValue("POS2");
        p2.addToIndexes();
        t2.setPos(p2);

        Dependency dep1 = new Dependency(jcas);
        dep1.setGovernor(t1);
        dep1.setDependent(t2);
        // WebAnno legacy conventions
        // dep1.setBegin(min(dep1.getDependent().getBegin(), dep1.getGovernor().getBegin()));
        // dep1.setEnd(max(dep1.getDependent().getEnd(), dep1.getGovernor().getEnd()));
        // DKPro Core conventions
        dep1.setBegin(dep1.getDependent().getBegin());
        dep1.setEnd(dep1.getDependent().getEnd());
        dep1.addToIndexes();

        writeAndAssertEquals(jcas);
    }

    @Test
    public void testZeroLengthSpansWithoutFeatureValues() throws Exception
    {
        JCas jcas = makeJCasOneSentence();

        // One at the beginning
        new Span(jcas, 0, 0).addToIndexes();

        // One at the end
        new Span(jcas, jcas.getDocumentText().length(), jcas.getDocumentText().length())
                .addToIndexes();

        writeAndAssertEquals(jcas);
    }

    @Test
    public void testZeroLengthSpansWithFeatureValues() throws Exception
    {
        JCas jcas = makeJCasOneSentence();

        // One at the beginning
        Span ne1 = new Span(jcas, 0, 0);
        ne1.setValue("PERSON");
        ne1.addToIndexes();

        // One at the end
        Span ne2 = new Span(jcas, jcas.getDocumentText().length(), jcas.getDocumentText().length());
        ne2.setValue("ORG");
        ne2.addToIndexes();

        writeAndAssertEquals(jcas);
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

        writeAndAssertEquals(jcas);
    }

    @Test
    public void testZeroLengthSpanBetweenAdjacentTokens() throws Exception
    {
        JCas jcas = makeJCas();
        jcas.setDocumentText("word.");
        new Token(jcas, 0, 4).addToIndexes();
        new Token(jcas, 4, 5).addToIndexes();
        new Sentence(jcas, 0, 5).addToIndexes();

        CAS cas = jcas.getCas();
        Type simpleSpanType = cas.getTypeSystem().getType("webanno.custom.SimpleSpan");

        // Insert zero-width annotation between the adjacent tokens (at end of first token).
        AnnotationFS fs1a = cas.createAnnotation(simpleSpanType, 4, 4);
        cas.addFsToIndexes(fs1a);

        writeAndAssertEquals(jcas);
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

        writeAndAssertEquals(jcas);
    }

    @Test
    public void testTokenBoundedSpanWithFeatureValue() throws Exception
    {
        JCas jcas = makeJCasOneSentence();

        int n = 0;
        for (Token t : select(jcas, Token.class)) {
            Span ne = new Span(jcas, t.getBegin(), t.getEnd());
            ne.setValue("NE " + n);
            ne.addToIndexes();
            n++;
        }

        writeAndAssertEquals(jcas);
    }

    @Test
    public void testTokenBoundedStackedSpanWithFeatureValue() throws Exception
    {
        JCas jcas = makeJCasOneSentence();

        for (Token t : select(jcas, Token.class)) {
            Span ne1 = new Span(jcas, t.getBegin(), t.getEnd());
            ne1.setValue("NE");
            ne1.addToIndexes();

            Span ne2 = new Span(jcas, t.getBegin(), t.getEnd());
            ne2.setValue("NE");
            ne2.addToIndexes();
        }

        writeAndAssertEquals(jcas);
    }

    @Test
    public void testTokenBoundedSpanWithoutFeatureValue() throws Exception
    {
        JCas jcas = makeJCasOneSentence();

        for (Token t : select(jcas, Token.class)) {
            Span ne = new Span(jcas, t.getBegin(), t.getEnd());
            ne.addToIndexes();
        }

        writeAndAssertEquals(jcas);
    }

    @Test
    public void testTokenBoundedSpanWithNastyFeatureValue() throws Exception
    {
        JCas jcas = makeJCasOneSentence();

        for (Token t : select(jcas, Token.class)) {
            Span ne = new Span(jcas, t.getBegin(), t.getEnd());
            ne.setValue("de.tudarmstadt.ukp.dkpro.core.api.ner.type.NamedEntity:value");
            ne.addToIndexes();
        }

        writeAndAssertEquals(jcas);
    }

    @Test
    public void testTokenBoundedSpanWithUnderscoreFeatureValue() throws Exception
    {
        JCas jcas = makeJCasOneSentence();

        for (Token t : select(jcas, Token.class)) {
            Span ne = new Span(jcas, t.getBegin(), t.getEnd());
            ne.setValue("_");
            ne.addToIndexes();
        }

        writeAndAssertEquals(jcas);
    }

    @Test
    public void testTokenBoundedSpanWithAsteriskFeatureValue() throws Exception
    {
        JCas jcas = makeJCasOneSentence();

        for (Token t : select(jcas, Token.class)) {
            Span ne = new Span(jcas, t.getBegin(), t.getEnd());
            ne.setValue("*");
            ne.addToIndexes();
        }

        writeAndAssertEquals(jcas);
    }

    @Test
    public void testSingleTokenWithoutFeatureValue() throws Exception
    {
        JCas jCas = makeJCasOneSentence();
        Span neToken = new Span(jCas, 0, 4);
        neToken.addToIndexes();

        writeAndAssertEquals(jCas);
    }

    @Test
    public void testTokenBoundedBioLookAlike() throws Exception
    {
        JCas jcas = makeJCasOneSentence();

        int n = 0;
        for (Token t : select(jcas, Token.class)) {
            Span ne = new Span(jcas, t.getBegin(), t.getEnd());
            ne.setValue(((n == 0) ? "B-" : "I-") + "NOTBIO!");
            ne.addToIndexes();
            n++;
        }

        writeAndAssertEquals(jcas);
    }

    @Test
    public void testTokenBoundedStackedLookAlike() throws Exception
    {
        JCas jcas = makeJCasOneSentence();

        int n = 0;
        for (Token t : select(jcas, Token.class)) {
            Span ne = new Span(jcas, t.getBegin(), t.getEnd());
            ne.setValue("NOTSTACKED[" + n + "]");
            ne.addToIndexes();
            n++;
        }

        writeAndAssertEquals(jcas);
    }

    @Test
    public void testTokenBoundedSpanWithSpecialSymbolsValue() throws Exception
    {
        JCas jcas = makeJCasOneSentence();

        for (Token t : select(jcas, Token.class)) {
            Span ne = new Span(jcas, t.getBegin(), t.getEnd());
            ne.setValue("#*'\"`´\t:;{}|[ ]()\\§$%?=&_\n");
            ne.addToIndexes();
        }

        writeAndAssertEquals(jcas);
    }

    @Test
    public void testMultiTokenSpanWithoutFeatureValue() throws Exception
    {
        JCas jcas = makeJCasOneSentence();

        Span ne = new Span(jcas, 0, jcas.getDocumentText().length());
        ne.addToIndexes();

        writeAndAssertEquals(jcas);
    }

    @Test
    public void testSubMultiTokenSpanWithoutFeatureValue() throws Exception
    {
        JCas jcas = makeJCasOneSentence();

        Span ne1 = new Span(jcas, 0, 6);
        ne1.addToIndexes();

        writeAndAssertEquals(jcas);
    }

    @Test
    public void testSubMultiTokenSpanWithoutFeatureValue2() throws Exception
    {
        JCas jcas = makeJCasOneSentence();

        Span ne1 = new Span(jcas, 1, 6);
        ne1.addToIndexes();

        writeAndAssertEquals(jcas);
    }

    @Test
    public void testSubMultiTokenSpanWithoutFeatureValue3() throws Exception
    {
        JCas jcas = makeJCasOneSentence();

        new Span(jcas, 1, 6).addToIndexes();
        new Span(jcas, 6, 12).addToIndexes();

        writeAndAssertEquals(jcas);
    }

    @Test
    public void testSubMultiTokenSpanWithFeatureValue() throws Exception
    {
        JCas jcas = makeJCasOneSentence("aaaaaa bbbbbb cccccc");
        assertEquals(asList("aaaaaa", "bbbbbb", "cccccc"), toText(select(jcas, Token.class)));

        // @formatter:off
        //               1111111111 
        //     01234567890123456789
        //     --------------------
        //     aaaaaa bbbbbb cccccc
        //  1  ------               - single token
        //  2  ------+------        - multi-token
        //  3           --          - inside token
        //  4         ----          - token prefix
        //  5           ----        - token suffix
        //  6     ---+------        - multi-token prefix 
        //  7  ------+---           - multi-token suffix
        //  8     ---+---           - multi-token prefix + suffix
        //  9     ---+------+---    - multi-token prefix + full + suffix
        // 10            |          - zero-span inside token
        // 11         |             - zero-span beginning of token
        // 12               |       - zero-span end of token
        // @formatter:on

        List<Span> annotations = new ArrayList<>();
        annotations.add(new Span(jcas, 0, 6)); // 1
        annotations.add(new Span(jcas, 0, 13)); // 2
        annotations.add(new Span(jcas, 9, 11)); // 3
        annotations.add(new Span(jcas, 7, 11)); // 4
        annotations.add(new Span(jcas, 9, 13)); // 5
        annotations.add(new Span(jcas, 3, 13)); // 6
        annotations.add(new Span(jcas, 0, 10)); // 7
        annotations.add(new Span(jcas, 3, 10)); // 8
        annotations.add(new Span(jcas, 3, 17)); // 9
        annotations.add(new Span(jcas, 10, 10)); // 10
        annotations.add(new Span(jcas, 7, 7)); // 11
        annotations.add(new Span(jcas, 13, 13)); // 12
        IntStream.range(0, annotations.size()).forEach(idx -> {
            Span ne = annotations.get(idx);
            ne.setValue(String.valueOf(idx + 1));
            ne.addToIndexes();
        });

        writeAndAssertEquals(jcas);
    }

    @Test
    public void testStackedSubMultiTokenSpanWithFeatureValue() throws Exception
    {
        JCas jcas = makeJCasOneSentence("aaaaaa bbbbbb cccccc");
        assertEquals(asList("aaaaaa", "bbbbbb", "cccccc"), toText(select(jcas, Token.class)));

        // @formatter:off
        //               1111111111 
        //     01234567890123456789
        //     --------------------
        //     aaaaaa bbbbbb cccccc
        //  1  ------               - single token
        //  2  ------+------        - multi-token
        //  3           --          - inside token
        //  4         ----          - token prefix
        //  5           ----        - token suffix
        //  6     ---+------        - multi-token prefix 
        //  7  ------+---           - multi-token suffix
        //  8     ---+---           - multi-token prefix + suffix
        //  9     ---+------+---    - multi-token prefix + full + suffix
        // 10            |          - zero-span inside token
        // 11         |             - zero-span beginning of token
        // 12               |       - zero-span end of token
        // @formatter:on

        List<Span> annotations = new ArrayList<>();
        annotations.add(new Span(jcas, 0, 6)); // 1
        annotations.add(new Span(jcas, 0, 6)); // 1
        annotations.add(new Span(jcas, 0, 13)); // 2
        annotations.add(new Span(jcas, 0, 13)); // 2
        annotations.add(new Span(jcas, 9, 10)); // 3
        annotations.add(new Span(jcas, 9, 10)); // 3
        annotations.add(new Span(jcas, 7, 10)); // 4
        annotations.add(new Span(jcas, 7, 10)); // 4
        annotations.add(new Span(jcas, 9, 13)); // 5
        annotations.add(new Span(jcas, 9, 13)); // 5
        annotations.add(new Span(jcas, 3, 13)); // 6
        annotations.add(new Span(jcas, 3, 13)); // 6
        annotations.add(new Span(jcas, 0, 10)); // 7
        annotations.add(new Span(jcas, 0, 10)); // 7
        annotations.add(new Span(jcas, 3, 10)); // 8
        annotations.add(new Span(jcas, 3, 10)); // 8
        annotations.add(new Span(jcas, 3, 17)); // 9
        annotations.add(new Span(jcas, 3, 17)); // 9
        annotations.add(new Span(jcas, 10, 10)); // 10
        annotations.add(new Span(jcas, 10, 10)); // 10
        annotations.add(new Span(jcas, 7, 7)); // 11
        annotations.add(new Span(jcas, 7, 7)); // 11
        annotations.add(new Span(jcas, 13, 13)); // 12
        annotations.add(new Span(jcas, 13, 13)); // 12
        IntStream.range(0, annotations.size()).forEach(idx -> {
            Span ne = annotations.get(idx);
            ne.setValue(String.valueOf((idx / 2) + 1) + (idx % 2 == 0 ? "a" : "b"));
            ne.addToIndexes();
        });

        writeAndAssertEquals(jcas);
    }

    @Test
    public void testMultiTokenStackedSpanWithoutFeatureValue() throws Exception
    {
        JCas jcas = makeJCasOneSentence();

        Span ne1 = new Span(jcas, 0, jcas.getDocumentText().length());
        ne1.addToIndexes();

        Span ne2 = new Span(jcas, 0, jcas.getDocumentText().length());
        ne2.addToIndexes();

        writeAndAssertEquals(jcas);
    }

    @Test
    public void testMultiTokenSpanWithFeatureValue() throws Exception
    {
        JCas jcas = makeJCasOneSentence();

        Span ne = new Span(jcas, 0, jcas.getDocumentText().length());
        ne.setValue("PERSON");
        ne.addToIndexes();

        writeAndAssertEquals(jcas);
    }

    @Test
    public void testMultiTokenStackedSpanWithFeatureValue() throws Exception
    {
        JCas jcas = makeJCasOneSentence();

        Span ne1 = new Span(jcas, 0, jcas.getDocumentText().length());
        ne1.setValue("PERSON");
        ne1.addToIndexes();

        Span ne2 = new Span(jcas, 0, jcas.getDocumentText().length());
        ne2.setValue("LOCATION");
        ne2.addToIndexes();

        writeAndAssertEquals(jcas);
    }

    @Test
    public void testCrossSentenceSpanWithoutFeatureValue() throws Exception
    {
        JCas jcas = makeJCasTwoSentences();

        Span ne = new Span(jcas, 0, jcas.getDocumentText().length());
        ne.addToIndexes();

        writeAndAssertEquals(jcas);
    }

    @Test
    public void testCrossSentenceSpanWithFeatureValue() throws Exception
    {
        JCas jcas = makeJCasTwoSentences();

        Span ne = new Span(jcas, 0, jcas.getDocumentText().length());
        ne.setValue("PERSON");
        ne.addToIndexes();

        writeAndAssertEquals(jcas);
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
        // WebAnno legacy conventions
        // AnnotationFS fs1 = cas.createAnnotation(relationType,
        // min(dep.getBegin(), gov.getBegin()),
        // max(dep.getEnd(), gov.getEnd()));
        // DKPro Core conventions
        AnnotationFS fs1 = cas.createAnnotation(relationType, dep.getBegin(), dep.getEnd());
        FSUtil.setFeature(fs1, "Governor", gov);
        FSUtil.setFeature(fs1, "Dependent", dep);
        cas.addFsToIndexes(fs1);

        writeAndAssertEquals(jcas);
    }

    @Test
    public void testSingleNonTokenRelationWithoutFeatureValue() throws Exception
    {
        JCas jcas = makeJCasOneSentence();
        CAS cas = jcas.getCas();

        List<Token> tokens = new ArrayList<>(select(jcas, Token.class));

        Token t1 = tokens.get(0);
        Token t2 = tokens.get(tokens.size() - 1);

        Span gov = new Span(jcas, t1.getBegin(), t1.getEnd());
        gov.addToIndexes();
        Span dep = new Span(jcas, t2.getBegin(), t2.getEnd());
        dep.addToIndexes();

        Type relationType = cas.getTypeSystem().getType("webanno.custom.Relation");

        // One at the beginning
        // WebAnno legacy conventions
        // AnnotationFS fs1 = cas.createAnnotation(relationType,
        // min(dep.getBegin(), gov.getBegin()),
        // max(dep.getEnd(), gov.getEnd()));
        // DKPro Core conventions
        AnnotationFS fs1 = cas.createAnnotation(relationType, dep.getBegin(), dep.getEnd());
        FSUtil.setFeature(fs1, "Governor", gov);
        FSUtil.setFeature(fs1, "Dependent", dep);
        cas.addFsToIndexes(fs1);

        writeAndAssertEquals(jcas);
    }

    @Test
    public void testSingleStackedNonTokenRelationWithoutFeatureValue() throws Exception
    {
        JCas jcas = makeJCasOneSentence();
        CAS cas = jcas.getCas();

        List<Token> tokens = new ArrayList<>(select(jcas, Token.class));

        Token t1 = tokens.get(0);
        Token t2 = tokens.get(tokens.size() - 1);

        Span gov = new Span(jcas, t1.getBegin(), t1.getEnd());
        gov.addToIndexes();
        new Span(jcas, t1.getBegin(), t1.getEnd()).addToIndexes();

        Span dep = new Span(jcas, t2.getBegin(), t2.getEnd());
        dep.addToIndexes();
        new Span(jcas, t2.getBegin(), t2.getEnd()).addToIndexes();

        Type relationType = cas.getTypeSystem().getType("webanno.custom.Relation");

        // One at the beginning
        // WebAnno legacy conventions
        // AnnotationFS fs1 = cas.createAnnotation(relationType,
        // min(dep.getBegin(), gov.getBegin()),
        // max(dep.getEnd(), gov.getEnd()));
        // DKPro Core conventions
        AnnotationFS fs1 = cas.createAnnotation(relationType, dep.getBegin(), dep.getEnd());
        FSUtil.setFeature(fs1, "Governor", gov);
        FSUtil.setFeature(fs1, "Dependent", dep);
        cas.addFsToIndexes(fs1);

        writeAndAssertEquals(jcas);
    }

    @Test
    public void testSingleStackedNonTokenRelationWithoutFeatureValue2() throws Exception
    {
        JCas jcas = makeJCasOneSentence();
        CAS cas = jcas.getCas();

        List<Token> tokens = new ArrayList<>(select(jcas, Token.class));

        Token t1 = tokens.get(0);
        Token t2 = tokens.get(tokens.size() - 1);

        Span gov = new Span(jcas, t1.getBegin(), t1.getEnd());
        gov.addToIndexes();

        Span dep = new Span(jcas, t2.getBegin(), t2.getEnd());
        dep.addToIndexes();
        new Span(jcas, t2.getBegin(), t2.getEnd()).addToIndexes();

        Type relationType = cas.getTypeSystem().getType("webanno.custom.Relation");

        // One at the beginning
        // WebAnno legacy conventions
        // AnnotationFS fs1 = cas.createAnnotation(relationType,
        // min(dep.getBegin(), gov.getBegin()),
        // max(dep.getEnd(), gov.getEnd()));
        // DKPro Core conventions
        AnnotationFS fs1 = cas.createAnnotation(relationType, dep.getBegin(), dep.getEnd());
        FSUtil.setFeature(fs1, "Governor", gov);
        FSUtil.setFeature(fs1, "Dependent", dep);
        cas.addFsToIndexes(fs1);

        writeAndAssertEquals(jcas);
    }

    @Test
    public void testSingleStackedNonTokenRelationWithoutFeatureValue3() throws Exception
    {
        JCas jcas = makeJCasOneSentence();
        CAS cas = jcas.getCas();

        List<Token> tokens = new ArrayList<>(select(jcas, Token.class));

        Token t1 = tokens.get(0);
        Token t2 = tokens.get(tokens.size() - 1);

        Span gov = new Span(jcas, t1.getBegin(), t1.getEnd());
        gov.addToIndexes();
        new Span(jcas, t1.getBegin(), t1.getEnd()).addToIndexes();

        Span dep = new Span(jcas, t2.getBegin(), t2.getEnd());
        dep.addToIndexes();

        Type relationType = cas.getTypeSystem().getType("webanno.custom.Relation");

        // One at the beginning
        // WebAnno legacy conventions
        // AnnotationFS fs1 = cas.createAnnotation(relationType,
        // min(dep.getBegin(), gov.getBegin()),
        // max(dep.getEnd(), gov.getEnd()));
        // DKPro Core conventions
        AnnotationFS fs1 = cas.createAnnotation(relationType, dep.getBegin(), dep.getEnd());
        FSUtil.setFeature(fs1, "Governor", gov);
        FSUtil.setFeature(fs1, "Dependent", dep);
        cas.addFsToIndexes(fs1);

        writeAndAssertEquals(jcas);
    }

    @Test
    public void testSingleStackedNonTokenOverlappingRelationWithoutFeatureValue() throws Exception
    {
        JCas jcas = makeJCasOneSentence();
        CAS cas = jcas.getCas();

        List<Token> tokens = new ArrayList<>(select(jcas, Token.class));

        Token t1 = tokens.get(0);
        Token t2 = tokens.get(tokens.size() - 1);

        Span gov = new Span(jcas, t1.getBegin(), t2.getEnd());
        gov.addToIndexes();
        new Span(jcas, t1.getBegin(), t2.getEnd()).addToIndexes();

        Span dep = new Span(jcas, t2.getBegin(), t2.getEnd());
        dep.addToIndexes();
        new Span(jcas, t2.getBegin(), t2.getEnd()).addToIndexes();

        Type relationType = cas.getTypeSystem().getType("webanno.custom.Relation");

        // One at the beginning
        // WebAnno legacy conventions
        // AnnotationFS fs1 = cas.createAnnotation(relationType,
        // min(dep.getBegin(), gov.getBegin()),
        // max(dep.getEnd(), gov.getEnd()));
        // DKPro Core conventions
        AnnotationFS fs1 = cas.createAnnotation(relationType, dep.getBegin(), dep.getEnd());
        FSUtil.setFeature(fs1, "Governor", gov);
        FSUtil.setFeature(fs1, "Dependent", dep);
        cas.addFsToIndexes(fs1);

        writeAndAssertEquals(jcas);
    }

    @Test
    public void testSingleNonTokenRelationWithoutFeature() throws Exception
    {
        JCas jcas = makeJCasOneSentence();
        CAS cas = jcas.getCas();

        List<Token> tokens = new ArrayList<>(select(jcas, Token.class));

        Token t1 = tokens.get(0);
        Token t2 = tokens.get(tokens.size() - 1);

        Span gov = new Span(jcas, t1.getBegin(), t1.getEnd());
        gov.addToIndexes();
        Span dep = new Span(jcas, t2.getBegin(), t2.getEnd());
        dep.addToIndexes();

        Type relationType = cas.getTypeSystem().getType("webanno.custom.SimpleRelation");

        // One at the beginning
        // WebAnno legacy conventions
        // AnnotationFS fs1 = cas.createAnnotation(relationType,
        // min(dep.getBegin(), gov.getBegin()),
        // max(dep.getEnd(), gov.getEnd()));
        // DKPro Core conventions
        AnnotationFS fs1 = cas.createAnnotation(relationType, dep.getBegin(), dep.getEnd());
        FSUtil.setFeature(fs1, "Governor", gov);
        FSUtil.setFeature(fs1, "Dependent", dep);
        cas.addFsToIndexes(fs1);

        writeAndAssertEquals(jcas);
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

        Span gov = new Span(jcas, t1.getBegin(), t2.getEnd());
        gov.addToIndexes();
        Span dep = new Span(jcas, t3.getBegin(), t4.getEnd());
        dep.addToIndexes();

        Type relationType = cas.getTypeSystem().getType("webanno.custom.Relation");

        // One at the beginning
        // WebAnno legacy conventions
        // AnnotationFS fs1 = cas.createAnnotation(relationType,
        // min(dep.getBegin(), gov.getBegin()),
        // max(dep.getEnd(), gov.getEnd()));
        // DKPro Core conventions
        AnnotationFS fs1 = cas.createAnnotation(relationType, dep.getBegin(), dep.getEnd());
        FSUtil.setFeature(fs1, "Governor", gov);
        FSUtil.setFeature(fs1, "Dependent", dep);
        cas.addFsToIndexes(fs1);

        writeAndAssertEquals(jcas);
    }

    @Test
    public void testSingleNonMultiTokenRelationWithMultipleFeatureValues() throws Exception
    {
        JCas jcas = makeJCasOneSentence();
        CAS cas = jcas.getCas();

        List<Token> tokens = new ArrayList<>(select(jcas, Token.class));

        Token t1 = tokens.get(0);
        Token t2 = tokens.get(1);
        Token t3 = tokens.get(2);
        Token t4 = tokens.get(3);

        Span gov = new Span(jcas, t1.getBegin(), t2.getEnd());
        gov.addToIndexes();
        Span dep = new Span(jcas, t3.getBegin(), t4.getEnd());
        dep.addToIndexes();

        Type relationType = cas.getTypeSystem().getType("webanno.custom.ComplexRelation");

        // One at the beginning
        // WebAnno legacy conventions
        // AnnotationFS fs1 = cas.createAnnotation(relationType,
        // min(dep.getBegin(), gov.getBegin()),
        // max(dep.getEnd(), gov.getEnd()));
        // DKPro Core conventions
        AnnotationFS fs1 = cas.createAnnotation(relationType, dep.getBegin(), dep.getEnd());
        FSUtil.setFeature(fs1, "Governor", gov);
        FSUtil.setFeature(fs1, "Dependent", dep);
        FSUtil.setFeature(fs1, "value", "nsubj");
        FSUtil.setFeature(fs1, "boolValue", true);
        FSUtil.setFeature(fs1, "integerValue", 42);
        cas.addFsToIndexes(fs1);

        writeAndAssertEquals(jcas);
    }

    @Test
    public void testStackedNonMultiTokenRelationWithMultipleFeatureValues() throws Exception
    {
        JCas jcas = makeJCasOneSentence();
        CAS cas = jcas.getCas();

        List<Token> tokens = new ArrayList<>(select(jcas, Token.class));

        Token t1 = tokens.get(0);
        Token t2 = tokens.get(1);
        Token t3 = tokens.get(2);
        Token t4 = tokens.get(3);

        Span gov = new Span(jcas, t1.getBegin(), t2.getEnd());
        gov.addToIndexes();
        Span dep = new Span(jcas, t3.getBegin(), t4.getEnd());
        dep.addToIndexes();

        Type relationType = cas.getTypeSystem().getType("webanno.custom.ComplexRelation");

        // WebAnno legacy conventions
        // AnnotationFS fs1 = cas.createAnnotation(relationType,
        // min(dep.getBegin(), gov.getBegin()),
        // max(dep.getEnd(), gov.getEnd()));
        // DKPro Core conventions
        AnnotationFS fs1 = cas.createAnnotation(relationType, dep.getBegin(), dep.getEnd());
        FSUtil.setFeature(fs1, "Governor", gov);
        FSUtil.setFeature(fs1, "Dependent", dep);
        FSUtil.setFeature(fs1, "value", "nsubj");
        FSUtil.setFeature(fs1, "boolValue", true);
        FSUtil.setFeature(fs1, "integerValue", 42);
        cas.addFsToIndexes(fs1);

        // WebAnno legacy conventions
        // AnnotationFS fs2 = cas.createAnnotation(relationType,
        // min(dep.getBegin(), gov.getBegin()),
        // max(dep.getEnd(), gov.getEnd()));
        // DKPro Core conventions
        AnnotationFS fs2 = cas.createAnnotation(relationType, dep.getBegin(), dep.getEnd());
        FSUtil.setFeature(fs2, "Governor", gov);
        FSUtil.setFeature(fs2, "Dependent", dep);
        FSUtil.setFeature(fs2, "value", "obj");
        FSUtil.setFeature(fs2, "boolValue", false);
        FSUtil.setFeature(fs2, "integerValue", 43);
        cas.addFsToIndexes(fs2);

        writeAndAssertEquals(jcas);
    }

    @Disabled("Relations between different layers not supported in WebAnno TSV 3 atm")
    @Test
    public void testSingleMixedRelationWithoutFeatureValue() throws Exception
    {
        JCas jcas = makeJCasOneSentence();
        CAS cas = jcas.getCas();

        List<Token> tokens = new ArrayList<>(select(jcas, Token.class));

        Token gov = tokens.get(0);

        Token t2 = tokens.get(tokens.size() - 1);
        Span dep = new Span(jcas, t2.getBegin(), t2.getEnd());
        dep.addToIndexes();

        Type relationType = cas.getTypeSystem().getType("webanno.custom.Relation");

        // One at the beginning
        // WebAnno legacy conventions
        // AnnotationFS fs1 = cas.createAnnotation(relationType,
        // min(dep.getBegin(), gov.getBegin()),
        // max(dep.getEnd(), gov.getEnd()));
        // DKPro Core conventions
        AnnotationFS fs1 = cas.createAnnotation(relationType, dep.getBegin(), dep.getEnd());
        FSUtil.setFeature(fs1, "Governor", gov);
        FSUtil.setFeature(fs1, "Dependent", dep);
        cas.addFsToIndexes(fs1);

        writeAndAssertEquals(jcas);
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
        // WebAnno legacy conventions
        // AnnotationFS fs1 = cas.createAnnotation(relationType,
        // min(dep.getBegin(), gov.getBegin()),
        // max(dep.getEnd(), gov.getEnd()));
        // DKPro Core conventions
        AnnotationFS fs1 = cas.createAnnotation(relationType, dep.getBegin(), dep.getEnd());
        FSUtil.setFeature(fs1, "Governor", gov);
        FSUtil.setFeature(fs1, "Dependent", dep);
        FSUtil.setFeature(fs1, "value", "nsubj");
        cas.addFsToIndexes(fs1);

        writeAndAssertEquals(jcas);
    }

    @Test
    public void testSingleTokenRelationWithMultipleFeatureValues() throws Exception
    {
        JCas jcas = makeJCasOneSentence();
        CAS cas = jcas.getCas();

        List<Token> tokens = new ArrayList<>(select(jcas, Token.class));

        Token gov = tokens.get(0);
        Token dep = tokens.get(tokens.size() - 1);

        Type relationType = cas.getTypeSystem().getType("webanno.custom.ComplexRelation");

        // One at the beginning
        // WebAnno legacy conventions
        // AnnotationFS fs1 = cas.createAnnotation(relationType,
        // min(dep.getBegin(), gov.getBegin()),
        // max(dep.getEnd(), gov.getEnd()));
        // DKPro Core conventions
        AnnotationFS fs1 = cas.createAnnotation(relationType, dep.getBegin(), dep.getEnd());
        FSUtil.setFeature(fs1, "Governor", gov);
        FSUtil.setFeature(fs1, "Dependent", dep);
        FSUtil.setFeature(fs1, "value", "nsubj");
        FSUtil.setFeature(fs1, "boolValue", true);
        FSUtil.setFeature(fs1, "integerValue", 42);
        cas.addFsToIndexes(fs1);

        writeAndAssertEquals(jcas);
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

        writeAndAssertEquals(jcas);
    }

    @Test
    public void testUnsetSlotFeature() throws Exception
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

        makeLinkHostFS(jcas, "webanno.custom.FlexLinkHost", t1.getBegin(), t1.getEnd(),
                (FeatureStructure[]) null);

        writeAndAssertEquals(jcas);
    }

    @Test
    public void testSimpleSlotFeatureWithoutValues() throws Exception
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

        FeatureStructure link1 = makeLinkFS(jcas, null, s2);
        FeatureStructure link2 = makeLinkFS(jcas, null, s3);

        makeLinkHostFS(jcas, t1.getBegin(), t1.getEnd(), link1, link2);

        writeAndAssertEquals(jcas);
    }

    @Test
    public void testStackedSimpleSlotFeatureWithoutValues() throws Exception
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

        {
            FeatureStructure link1 = makeLinkFS(jcas, null, s2);
            FeatureStructure link2 = makeLinkFS(jcas, null, s3);
            makeLinkHostFS(jcas, t1.getBegin(), t1.getEnd(), link1, link2);
        }

        {
            FeatureStructure link1 = makeLinkFS(jcas, null, s2);
            FeatureStructure link2 = makeLinkFS(jcas, null, s3);
            makeLinkHostFS(jcas, t1.getBegin(), t1.getEnd(), link1, link2);
        }

        writeAndAssertEquals(jcas);
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

        writeAndAssertEquals(jcas);
    }

    @Test
    public void testComplexSlotFeatureWithoutValues() throws Exception
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

        FeatureStructure link1 = makeLinkFS(jcas, "webanno.custom.ComplexLinkType", null, s2);
        FeatureStructure link2 = makeLinkFS(jcas, "webanno.custom.ComplexLinkType", null, s3);

        makeLinkHostFS(jcas, "webanno.custom.ComplexLinkHost", t1.getBegin(), t1.getEnd(), link1,
                link2);

        writeAndAssertEquals(jcas);
    }

    @Test
    public void testStackedComplexSlotFeatureWithoutValues() throws Exception
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

        {
            FeatureStructure link1 = makeLinkFS(jcas, "webanno.custom.ComplexLinkType", null, s2);
            FeatureStructure link2 = makeLinkFS(jcas, "webanno.custom.ComplexLinkType", null, s3);
            makeLinkHostFS(jcas, "webanno.custom.ComplexLinkHost", t1.getBegin(), t1.getEnd(),
                    link1, link2);
        }

        {
            FeatureStructure link1 = makeLinkFS(jcas, "webanno.custom.ComplexLinkType", null, s2);
            FeatureStructure link2 = makeLinkFS(jcas, "webanno.custom.ComplexLinkType", null, s3);
            makeLinkHostFS(jcas, "webanno.custom.ComplexLinkHost", t1.getBegin(), t1.getEnd(),
                    link1, link2);
        }

        writeAndAssertEquals(jcas);
    }

    @Test
    public void testStackedComplexSlotFeatureWithoutSlotFillers() throws Exception
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

        AnnotationFS host1 = makeLinkHostFS(jcas, "webanno.custom.ComplexLinkHost", t1.getBegin(),
                t1.getEnd());
        setFeature(host1, "value", "val1");

        AnnotationFS host2 = makeLinkHostFS(jcas, "webanno.custom.ComplexLinkHost", t1.getBegin(),
                t1.getEnd());
        setFeature(host2, "value", "val2");

        writeAndAssertEquals(jcas);
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

        writeAndAssertEquals(jcas);
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

        writeAndAssertEquals(jcas);
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

        writeAndAssertEquals(jcas);
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

        writeAndAssertEquals(jcas);
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

        writeAndAssertEquals(jcas);
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

        makeChainHead(head,
                makeChainLink(link, cas, t1.getBegin(), t1.getEnd(), null, null, makeChainLink(
                        link, cas, t2.getBegin(), t2.getEnd(), null, null,
                        makeChainLink(link, cas, t3.getBegin(), t3.getEnd(), null, null, null))));

        writeAndAssertEquals(jcas);
    }

    @Test
    public void testMultiTokenChain() throws Exception
    {
        JCas jcas = makeJCasOneSentence();
        CAS cas = jcas.getCas();

        List<Token> tokens = new ArrayList<>(select(jcas, Token.class));

        Token t1 = tokens.get(0);
        Token t2 = tokens.get(1);
        Token t3 = tokens.get(2);
        Token t4 = tokens.get(3);

        Type head = cas.getTypeSystem().getType("webanno.custom.SimpleChain");
        Type link = cas.getTypeSystem().getType("webanno.custom.SimpleLink");

        makeChainHead(head, makeChainLink(link, cas, t1.getBegin(), t2.getEnd(), null, null,
                makeChainLink(link, cas, t3.getBegin(), t4.getEnd(), null, null, null)));

        writeAndAssertEquals(jcas);
    }

    @Test
    public void testStackedChain() throws Exception
    {
        JCas jcas = makeJCasOneSentence();
        CAS cas = jcas.getCas();

        List<Token> tokens = new ArrayList<>(select(jcas, Token.class));

        Token t1 = tokens.get(0);
        Token t2 = tokens.get(1);
        Token t3 = tokens.get(2);

        Type head = cas.getTypeSystem().getType("webanno.custom.SimpleChain");
        Type link = cas.getTypeSystem().getType("webanno.custom.SimpleLink");

        makeChainHead(head,
                makeChainLink(link, cas, t1.getBegin(), t1.getEnd(), null, null, makeChainLink(
                        link, cas, t2.getBegin(), t2.getEnd(), null, null,
                        makeChainLink(link, cas, t3.getBegin(), t3.getEnd(), null, null, null))));

        makeChainHead(head,
                makeChainLink(link, cas, t3.getBegin(), t3.getEnd(), null, null, makeChainLink(
                        link, cas, t2.getBegin(), t2.getEnd(), null, null,
                        makeChainLink(link, cas, t1.getBegin(), t1.getEnd(), null, null, null))));

        writeAndAssertEquals(jcas);
    }

    @Test
    public void testSubtokenChain() throws Exception
    {
        JCas jcas = makeJCasOneSentence();
        CAS cas = jcas.getCas();

        List<Token> tokens = new ArrayList<>(select(jcas, Token.class));

        Token t1 = tokens.get(0);
        Token t2 = tokens.get(1);
        Token t4 = tokens.get(3);

        Type head = cas.getTypeSystem().getType("webanno.custom.SimpleChain");
        Type link = cas.getTypeSystem().getType("webanno.custom.SimpleLink");

        makeChainHead(head,
                makeChainLink(link, cas, t1.getBegin() + 1, t1.getEnd() - 1, null, null,
                        makeChainLink(link, cas, t2.getBegin() + 1, t2.getEnd() - 1, null, null,
                                makeChainLink(link, cas, t4.getBegin() + 1, t4.getEnd() - 1, null,
                                        null, null))));

        writeAndAssertEquals(jcas);
    }

    @Test
    public void testSentenceWithLineBreak() throws Exception
    {
        JCas jcas = makeJCasOneSentence("This is\na test .");

        Span neToken = new Span(jcas, 0, 4);
        neToken.addToIndexes();

        writeAndAssertEquals(jcas);
    }

    @Test
    public void testSentenceWithTab() throws Exception
    {
        JCas jcas = makeJCasOneSentence("This is\ta test .");

        Span neToken = new Span(jcas, 0, 4);
        neToken.addToIndexes();

        writeAndAssertEquals(jcas);
    }

    @Test
    public void testSentenceWithEmoji() throws Exception
    {
        JCas jcas = makeJCasOneSentence("I like it 😊 .");

        Span neToken = new Span(jcas, 10, 12);
        neToken.addToIndexes();

        writeAndAssertEquals(jcas);
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

    /*
     * This is something that cannot be done through the editor UI but can happen when working with
     * externally created data.
     */
    @Test
    public void testAnnotationWithTrailingWhitespace() throws Exception
    {
        JCas jcas = JCasFactory.createJCas();

        DocumentMetaData.create(jcas).setDocumentId("doc");
        jcas.setDocumentText("one  two");
        new Token(jcas, 0, 3).addToIndexes();
        new Token(jcas, 5, 8).addToIndexes();
        new Sentence(jcas, 0, 8).addToIndexes();

        // NE has trailing whitespace - on export this should be silently dropped
        new NamedEntity(jcas, 0, 4).addToIndexes();

        writeAndAssertEquals(jcas);
    }

    /*
     * This is something that cannot be done through the editor UI but can happen when working with
     * externally created data.
     */
    @Test
    public void testAnnotationWithTrailingWhitespaceAtEnd() throws Exception
    {
        JCas jcas = JCasFactory.createJCas();

        DocumentMetaData.create(jcas).setDocumentId("doc");
        jcas.setDocumentText("one two ");
        new Token(jcas, 0, 3).addToIndexes();
        new Token(jcas, 4, 7).addToIndexes();
        new Sentence(jcas, 0, 7).addToIndexes();

        // NE has trailing whitespace - on export this should be silently dropped
        new NamedEntity(jcas, 4, 8).addToIndexes();

        writeAndAssertEquals(jcas);
    }

    /*
     * This is something that cannot be done through the editor UI but can happen when working with
     * externally created data.
     */
    @Test
    public void testAnnotationWithLeadingWhitespaceAtStart() throws Exception
    {
        JCas jcas = JCasFactory.createJCas();

        DocumentMetaData.create(jcas).setDocumentId("doc");
        jcas.setDocumentText(" one two");
        new Token(jcas, 1, 4).addToIndexes();
        new Token(jcas, 5, 8).addToIndexes();
        new Sentence(jcas, 1, 8).addToIndexes();

        // NE has leading whitespace - on export this should be silently dropped
        new NamedEntity(jcas, 0, 4).addToIndexes();

        writeAndAssertEquals(jcas);
    }

    /*
     * This is something that cannot be done through the editor UI but can happen when working with
     * externally created data.
     */
    @Test
    public void testAnnotationWithLeadingWhitespace() throws Exception
    {
        JCas jcas = JCasFactory.createJCas();

        DocumentMetaData.create(jcas).setDocumentId("doc");
        jcas.setDocumentText("one  two");
        new Token(jcas, 0, 3).addToIndexes();
        new Token(jcas, 5, 8).addToIndexes();
        new Sentence(jcas, 0, 8).addToIndexes();

        // NE has leading whitespace - on export this should be silently dropped
        new NamedEntity(jcas, 4, 8).addToIndexes();

        writeAndAssertEquals(jcas);
    }

    /*
     * This is something that cannot be done through the editor UI but can happen when working with
     * externally created data.
     */
    @Test
    public void testZeroWidthAnnotationBetweenTokenIsMovedToEndOfPreviousToken() throws Exception
    {
        JCas jcas = JCasFactory.createJCas();

        DocumentMetaData.create(jcas).setDocumentId("doc");
        jcas.setDocumentText("one  two");
        new Token(jcas, 0, 3).addToIndexes();
        new Token(jcas, 5, 8).addToIndexes();
        new Sentence(jcas, 0, 8).addToIndexes();

        // NE is after the end of the last token and should be moved to the end of the last token
        // otherwise it could not be represented in the TSV3 format.
        new NamedEntity(jcas, 4, 4).addToIndexes();

        writeAndAssertEquals(jcas);
    }

    /*
     * This is something that cannot be done through the editor UI but can happen when working with
     * externally created data.
     */
    @Test
    public void testZeroWidthAnnotationBeyondLastTokenIsMovedToEndOfLastToken() throws Exception
    {
        JCas jcas = JCasFactory.createJCas();

        DocumentMetaData.create(jcas).setDocumentId("doc");
        jcas.setDocumentText("one two  ");
        new Token(jcas, 0, 3).addToIndexes();
        new Token(jcas, 4, 7).addToIndexes();
        new Sentence(jcas, 0, 7).addToIndexes();

        // NE is after the end of the last token and should be moved to the end of the last token
        // otherwise it could not be represented in the TSV3 format.
        new NamedEntity(jcas, 8, 8).addToIndexes();

        writeAndAssertEquals(jcas);
    }

    /*
     * This is something that cannot be done through the editor UI but can happen when working with
     * externally created data.
     */
    @Test
    public void testZeroWidthAnnotationBeforeFirstTokenIsMovedToBeginOfFirstToken() throws Exception
    {
        JCas jcas = JCasFactory.createJCas();

        DocumentMetaData.create(jcas).setDocumentId("doc");
        jcas.setDocumentText("  one two");
        new Token(jcas, 2, 5).addToIndexes();
        new Token(jcas, 6, 9).addToIndexes();
        new Sentence(jcas, 2, 9).addToIndexes();

        // NE is after the end of the last token and should be moved to the end of the last token
        // otherwise it could not be represented in the TSV3 format.
        new NamedEntity(jcas, 1, 1).addToIndexes();

        writeAndAssertEquals(jcas);
    }

    @Test
    public void testElevatedType() throws Exception
    {
        JCas jcas = JCasFactory.createJCas();

        DocumentMetaData.create(jcas).setDocumentId("doc");
        jcas.setDocumentText("John");

        // Add an elevated type which is not a direct subtype of Annotation. This type not be picked
        // up by the schema analyzer but should still be serialized as the POS type which is in fact
        // picked up.
        POS_NOUN pos = new POS_NOUN(jcas, 0, 4);
        pos.setPosValue("NN");
        pos.setCoarseValue("NOUN");
        pos.addToIndexes();

        Token t = new Token(jcas, 0, 4);
        t.setPos(pos);
        t.addToIndexes();
        new Sentence(jcas, 0, 4).addToIndexes();

        writeAndAssertEquals(jcas);
    }

    @Test
    public void testSentenceId() throws Exception
    {
        JCas jcas = makeJCasTwoSentences();

        int n = 1;
        for (Sentence s : select(jcas, Sentence.class)) {
            s.setId("sent-" + n);
            n++;
        }

        writeAndAssertEquals(jcas);
    }

    @Test
    public void testSubTokenRelation() throws Exception
    {
        JCas jcas = makeJCasOneSentence("Test");
        Span s1 = new Span(jcas, 0, 1);
        s1.setValue("OTH");
        Span s2 = new Span(jcas, 3, 4);
        s2.setValue("OTH");
        Relation r = new Relation(jcas, s2.getBegin(), s2.getEnd());
        r.setGovernor(s1);
        r.setDependent(s2);
        asList(s1, s2, r).forEach(Annotation::addToIndexes);

        writeAndAssertEquals(jcas);
    }

    @Test
    public void testSubTokenRelation2() throws Exception
    {
        JCas jcas = makeJCasOneSentence("Test again");
        Span s1 = new Span(jcas, 0, 1);
        s1.setValue("OTH");
        Span s2 = new Span(jcas, 5, 10);
        s2.setValue("OTH");
        Relation r = new Relation(jcas, s2.getBegin(), s2.getEnd());
        r.setGovernor(s1);
        r.setDependent(s2);
        asList(s1, s2, r).forEach(Annotation::addToIndexes);

        writeAndAssertEquals(jcas);
    }

    @Test
    public void testSubTokenPrefix() throws Exception
    {
        JCas jcas = makeJCasOneSentence("Test");
        Span s = new Span(jcas, 0, 1);
        s.setValue("OTH");
        s.addToIndexes();

        writeAndAssertEquals(jcas);
    }

    private void writeAndAssertEquals(JCas aJCas, Object... aParams)
        throws IOException, ResourceInitializationException, AnalysisEngineProcessException
    {
        String methodName = testInfo.getTestMethod().get().getName();
        String className = testInfo.getTestClass().get().getSimpleName();

        assumeFalse(isKnownToFail(methodName), "This test is known to fail.");

        String targetFolder = "target/test-output/" + className + "/" + getSuiteName() + "/"
                + methodName;
        String referenceFolder = "src/test/resources/" + getSuiteName() + "/" + methodName;

        List<Object> params = new ArrayList<>();
        params.addAll(asList(aParams));
        params.add(WebannoTsv3XWriter.PARAM_TARGET_LOCATION);
        params.add(targetFolder);
        params.add(WebannoTsv3XWriter.PARAM_OVERWRITE);
        params.add(true);

        AnalysisEngineDescription tsv = makeWriter();
        for (int i = 0; i < params.size(); i += 2) {
            String name = (String) params.get(i);
            Object value = params.get(i + 1);
            if (ConfigurationParameterFactory.canParameterBeSet(tsv, name)) {
                ConfigurationParameterFactory.setParameter(tsv, name, value);
            }
        }

        AnalysisEngineDescription xmi = createEngineDescription( //
                XmiWriter.class, //
                XmiWriter.PARAM_TARGET_LOCATION, targetFolder, //
                XmiWriter.PARAM_OVERWRITE, true);

        SimplePipeline.runPipeline(aJCas, tsv, xmi);

        File referenceFile = new File(referenceFolder, "reference.tsv");
        assumeTrue(referenceFile.exists(), "No reference data available for this test.");

        File actualFile = new File(targetFolder, "doc.tsv");

        String reference = FileUtils.readFileToString(referenceFile, "UTF-8");
        String actual = FileUtils.readFileToString(actualFile, "UTF-8");

        assertThat(actual).isEqualToNormalizingNewlines(reference);
    }

    private static JCas makeJCas() throws UIMAException
    {
        TypeSystemDescription global = TypeSystemDescriptionFactory.createTypeSystemDescription();
        TypeSystemDescription local = TypeSystemDescriptionFactory
                .createTypeSystemDescriptionFromPath(
                        "src/test/resources/desc/type/webannoTestTypes.xml");

        TypeSystemDescription merged = CasCreationUtils.mergeTypeSystems(asList(global, local));

        JCas jcas = JCasFactory.createJCas(merged);

        DocumentMetaData.create(jcas).setDocumentId("doc");

        return jcas;
    }

    private static JCas makeJCasOneSentence() throws UIMAException
    {
        JCas jcas = makeJCas();

        TokenBuilder<Token, Sentence> tb = new TokenBuilder<>(Token.class, Sentence.class);
        tb.buildTokens(jcas, "This is a test .");

        return jcas;
    }

    private static JCas makeJCasTwoSentences() throws UIMAException
    {
        JCas jcas = makeJCas();

        TokenBuilder<Token, Sentence> tb = new TokenBuilder<>(Token.class, Sentence.class);
        tb.buildTokens(jcas, "He loves her .\nShe loves him not .");

        assertEquals(2, select(jcas, Sentence.class).size());

        return jcas;
    }

    private static JCas makeJCasOneSentence(String aText) throws UIMAException
    {
        JCas jcas = makeJCas();

        TokenBuilder<Token, Sentence> tb = new TokenBuilder<>(Token.class, Sentence.class);
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

    private static AnnotationFS makeLinkHostFS(JCas aJCas, int aBegin, int aEnd,
            FeatureStructure... aLinks)
    {
        return makeLinkHostFS(aJCas, "webanno.custom.SimpleLinkHost", aBegin, aEnd, aLinks);
    }

    private static AnnotationFS makeLinkHostFS(JCas aJCas, String aType, int aBegin, int aEnd,
            FeatureStructure... aLinks)
    {
        Type hostType = aJCas.getTypeSystem().getType(aType);
        AnnotationFS hostA1 = aJCas.getCas().createAnnotation(hostType, aBegin, aEnd);
        if (aLinks != null) {
            hostA1.setFeatureValue(hostType.getFeatureByBaseName("links"),
                    FSCollectionFactory.createFSArray(aJCas, asList(aLinks)));
        }
        aJCas.getCas().addFsToIndexes(hostA1);
        return hostA1;
    }

    private static FeatureStructure makeLinkFS(JCas aJCas, String aSlotLabel, AnnotationFS aTarget)
    {
        return makeLinkFS(aJCas, "webanno.custom.LinkType", aSlotLabel, aTarget);
    }

    private static FeatureStructure makeLinkFS(JCas aJCas, String aType, String aSlotLabel,
            AnnotationFS aTarget)
    {
        Type linkType = aJCas.getTypeSystem().getType(aType);
        FeatureStructure linkA1 = aJCas.getCas().createFS(linkType);
        linkA1.setStringValue(linkType.getFeatureByBaseName("role"), aSlotLabel);
        linkA1.setFeatureValue(linkType.getFeatureByBaseName("target"), aTarget);
        aJCas.getCas().addFsToIndexes(linkA1);

        return linkA1;
    }

    private static void makeChainHead(Type aType, AnnotationFS first)
    {
        CAS cas = first.getCAS();
        FeatureStructure h = cas.createFS(aType);
        FSUtil.setFeature(h, "first", first);
        cas.addFsToIndexes(h);
    }

    private static AnnotationFS makeChainLink(Type aType, CAS aCas, int aBegin, int aEnd,
            String aLabel, String aLinkLabel, AnnotationFS aNext)
    {
        AnnotationFS link = aCas.createAnnotation(aType, aBegin, aEnd);
        FSUtil.setFeature(link, "next", aNext);
        FSUtil.setFeature(link, "referenceType", aLabel);
        FSUtil.setFeature(link, "referenceRelation", aLinkLabel);
        aCas.addFsToIndexes(link);
        return link;
    }
}

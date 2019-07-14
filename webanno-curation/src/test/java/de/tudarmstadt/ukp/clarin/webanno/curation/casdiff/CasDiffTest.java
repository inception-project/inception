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
package de.tudarmstadt.ukp.clarin.webanno.curation.casdiff;

import static de.tudarmstadt.ukp.clarin.webanno.curation.CurationTestUtils.HOST_TYPE;
import static de.tudarmstadt.ukp.clarin.webanno.curation.CurationTestUtils.createMultiLinkWithRoleTestTypeSytem;
import static de.tudarmstadt.ukp.clarin.webanno.curation.CurationTestUtils.load;
import static de.tudarmstadt.ukp.clarin.webanno.curation.CurationTestUtils.loadWebAnnoTsv3;
import static de.tudarmstadt.ukp.clarin.webanno.curation.CurationTestUtils.makeLinkFS;
import static de.tudarmstadt.ukp.clarin.webanno.curation.CurationTestUtils.makeLinkHostFS;
import static de.tudarmstadt.ukp.clarin.webanno.curation.agreement.AgreementUtils.getCohenKappaAgreement;
import static de.tudarmstadt.ukp.clarin.webanno.curation.casdiff.CasDiff.RelationDiffAdapter.DEPENDENCY_DIFF_ADAPTER;
import static de.tudarmstadt.ukp.clarin.webanno.curation.casdiff.CasDiff.SpanDiffAdapter.POS_DIFF_ADAPTER;
import static java.util.Arrays.asList;
import static org.apache.uima.fit.factory.JCasFactory.createJCas;
import static org.apache.uima.fit.util.JCasUtil.select;
import static org.junit.Assert.assertEquals;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.apache.uima.cas.CAS;
import org.apache.uima.cas.Type;
import org.apache.uima.cas.text.AnnotationFS;
import org.apache.uima.fit.factory.JCasFactory;
import org.apache.uima.fit.factory.TypeSystemDescriptionFactory;
import org.apache.uima.fit.testing.factory.TokenBuilder;
import org.apache.uima.fit.util.FSUtil;
import org.apache.uima.jcas.JCas;
import org.apache.uima.resource.metadata.TypeSystemDescription;
import org.apache.uima.util.CasCreationUtils;
import org.dkpro.core.testing.DkproTestContext;
import org.junit.Rule;
import org.junit.Test;

import de.tudarmstadt.ukp.clarin.webanno.api.WebAnnoConst;
import de.tudarmstadt.ukp.clarin.webanno.curation.agreement.AgreementUtils;
import de.tudarmstadt.ukp.clarin.webanno.curation.agreement.AgreementUtils.AgreementResult;
import de.tudarmstadt.ukp.clarin.webanno.curation.agreement.AgreementUtils.ConcreteAgreementMeasure;
import de.tudarmstadt.ukp.clarin.webanno.curation.casdiff.CasDiff.DiffAdapter;
import de.tudarmstadt.ukp.clarin.webanno.curation.casdiff.CasDiff.DiffResult;
import de.tudarmstadt.ukp.clarin.webanno.curation.casdiff.CasDiff.LinkCompareBehavior;
import de.tudarmstadt.ukp.clarin.webanno.curation.casdiff.CasDiff.RelationDiffAdapter;
import de.tudarmstadt.ukp.clarin.webanno.curation.casdiff.CasDiff.SpanDiffAdapter;
import de.tudarmstadt.ukp.dkpro.core.api.lexmorph.type.pos.POS;
import de.tudarmstadt.ukp.dkpro.core.api.ner.type.NamedEntity;
import de.tudarmstadt.ukp.dkpro.core.api.segmentation.type.Lemma;
import de.tudarmstadt.ukp.dkpro.core.api.segmentation.type.Sentence;
import de.tudarmstadt.ukp.dkpro.core.api.segmentation.type.Token;
import de.tudarmstadt.ukp.dkpro.core.api.syntax.type.dependency.Dependency;
import de.tudarmstadt.ukp.dkpro.statistics.agreement.coding.ICodingAnnotationItem;

public class CasDiffTest
{
    @Test
    public void noDataTest()
        throws Exception
    {
        List<String> entryTypes = new ArrayList<>();
        
        List<DiffAdapter> diffAdapters = new ArrayList<>();

        Map<String, List<CAS>> casByUser = new LinkedHashMap<>();

        DiffResult result = CasDiff.doDiff(entryTypes, diffAdapters,
                LinkCompareBehavior.LINK_TARGET_AS_LABEL, casByUser);
        
        result.print(System.out);
        
        assertEquals(0, result.size());
        assertEquals(0, result.getDifferingConfigurationSets().size());
        assertEquals(0, result.getIncompleteConfigurationSets().size());
    }

    @Test
    public void singleEmptyCasTest()
        throws Exception
    {
        String text = "";
        
        CAS user1Cas = JCasFactory.createJCas().getCas();
        user1Cas.setDocumentText(text);
        
        Map<String, List<CAS>> casByUser = new LinkedHashMap<>();
        casByUser.put("user1", asList(user1Cas));

        List<String> entryTypes = asList(Token.class.getName());

        List<SpanDiffAdapter> diffAdapters = asList(new SpanDiffAdapter(Token.class.getName()));

        DiffResult result = CasDiff.doDiff(entryTypes, diffAdapters,
                LinkCompareBehavior.LINK_TARGET_AS_LABEL, casByUser);
        
        result.print(System.out);
        
        assertEquals(0, result.size());
        assertEquals(0, result.getDifferingConfigurationSets().size());
    }

    @Test
    public void twoEmptyCasTest()
        throws Exception
    {
        String text = "";
        
        CAS user1Cas = JCasFactory.createJCas().getCas();
        user1Cas.setDocumentText(text);

        CAS user2Cas = JCasFactory.createJCas().getCas();
        user2Cas.setDocumentText(text);

        Map<String, List<CAS>> casByUser = new LinkedHashMap<>();
        casByUser.put("user1", asList(user1Cas));
        casByUser.put("user2", asList(user2Cas));

        List<String> entryTypes = asList(Lemma.class.getName());

        List<SpanDiffAdapter> diffAdapters = asList(new SpanDiffAdapter(Lemma.class.getName()));

        DiffResult result = CasDiff.doDiff(entryTypes, diffAdapters,
                LinkCompareBehavior.LINK_TARGET_AS_LABEL, casByUser);
        
        result.print(System.out);
        
        assertEquals(0, result.size());
        assertEquals(0, result.getDifferingConfigurationSets().size());
        assertEquals(0, result.getIncompleteConfigurationSets().size());

        AgreementResult agreement = AgreementUtils.getCohenKappaAgreement(result, entryTypes.get(0),
                "value", casByUser);
        assertEquals(Double.NaN, agreement.getAgreement(), 0.000001d);
        assertEquals(0, agreement.getIncompleteSetsByPosition().size());
    }

    @Test
    public void multipleEmptyCasWithMissingOnesTest()
        throws Exception
    {
        String text = "";
        
        CAS user1Cas1 = null;

        CAS user1Cas2 = null;

        CAS user1Cas3 = JCasFactory.createJCas().getCas();
        user1Cas3.setDocumentText(text);

        CAS user1Cas4 = JCasFactory.createJCas().getCas();
        user1Cas4.setDocumentText(text);

        CAS user2Cas1 = JCasFactory.createJCas().getCas();
        user2Cas1.setDocumentText(text);

        CAS user2Cas2 = null;

        CAS user2Cas3 = null;

        CAS user2Cas4 = JCasFactory.createJCas().getCas();
        user2Cas4.setDocumentText(text);
        
        Map<String, List<CAS>> casByUser = new LinkedHashMap<>();
        casByUser.put("user1", asList(user1Cas1, user1Cas2, user1Cas3, user1Cas4));
        casByUser.put("user2", asList(user2Cas1, user2Cas2, user2Cas3, user2Cas4));

        List<String> entryTypes = asList(Lemma.class.getName());

        List<SpanDiffAdapter> diffAdapters = asList(new SpanDiffAdapter(Lemma.class.getName()));

        DiffResult result = CasDiff.doDiff(entryTypes, diffAdapters,
                LinkCompareBehavior.LINK_TARGET_AS_LABEL, casByUser);
        
        result.print(System.out);
        
        assertEquals(0, result.size());
        assertEquals(0, result.getDifferingConfigurationSets().size());
        assertEquals(0, result.getIncompleteConfigurationSets().size());

        AgreementResult agreement = AgreementUtils.getCohenKappaAgreement(result, entryTypes.get(0),
                "value", casByUser);
        assertEquals(Double.NaN, agreement.getAgreement(), 0.000001d);
        assertEquals(0, agreement.getIncompleteSetsByPosition().size());
    }
    @Test
    public void noDifferencesPosTest()
        throws Exception
    {
        Map<String, List<CAS>> casByUser = load(
                "casdiff/noDifferences/data.conll",
                "casdiff/noDifferences/data.conll");

        List<String> entryTypes = asList(POS.class.getName());
        
        List<SpanDiffAdapter> diffAdapters = asList(SpanDiffAdapter.POS_DIFF_ADAPTER);

        DiffResult result = CasDiff.doDiff(entryTypes, diffAdapters,
                LinkCompareBehavior.LINK_TARGET_AS_LABEL, casByUser);
        
        result.print(System.out);
        
        assertEquals(26, result.size());
        assertEquals(0, result.getDifferingConfigurationSets().size());
        assertEquals(0, result.getIncompleteConfigurationSets().size());

        AgreementResult agreement = AgreementUtils.getCohenKappaAgreement(result, entryTypes.get(0),
                "PosValue", casByUser);
        assertEquals(1.0d, agreement.getAgreement(), 0.000001d);
        assertEquals(0, agreement.getIncompleteSetsByPosition().size());
    }

    @Test
    public void noDifferencesDependencyTest()
        throws Exception
    {
        Map<String, List<CAS>> casByUser = load(
                "casdiff/noDifferences/data.conll",
                "casdiff/noDifferences/data.conll");

        List<String> entryTypes = asList(Dependency.class.getName());

        List<? extends DiffAdapter> diffAdapters = asList(DEPENDENCY_DIFF_ADAPTER);

        DiffResult result = CasDiff.doDiff(entryTypes, diffAdapters,
                LinkCompareBehavior.LINK_TARGET_AS_LABEL, casByUser);

        result.print(System.out);
        
        assertEquals(26, result.size());
        assertEquals(0, result.getDifferingConfigurationSets().size());
        assertEquals(0, result.getIncompleteConfigurationSets().size());

        AgreementResult agreement = AgreementUtils.getCohenKappaAgreement(result, entryTypes.get(0),
                "DependencyType", casByUser);
        assertEquals(1.0d, agreement.getAgreement(), 0.000001d);
        assertEquals(0, agreement.getIncompleteSetsByPosition().size());
    }

    @Test
    public void noDifferencesPosDependencyTest()
        throws Exception
    {
        Map<String, List<CAS>> casByUser = load(
                "casdiff/noDifferences/data.conll",
                "casdiff/noDifferences/data.conll");

        List<String> entryTypes = asList(POS.class.getName(), Dependency.class.getName());
        
        List<? extends DiffAdapter> diffAdapters = asList(POS_DIFF_ADAPTER,
                DEPENDENCY_DIFF_ADAPTER);

        DiffResult result = CasDiff.doDiff(entryTypes, diffAdapters,
                LinkCompareBehavior.LINK_TARGET_AS_LABEL, casByUser);

        result.print(System.out);
        
        assertEquals(52, result.size());
        assertEquals(26, result.size(POS.class.getName()));
        assertEquals(26, result.size(Dependency.class.getName()));
        assertEquals(0, result.getDifferingConfigurationSets().size());
        assertEquals(0, result.getIncompleteConfigurationSets().size());

        AgreementResult agreement = AgreementUtils.getCohenKappaAgreement(result, entryTypes.get(0),
                "PosValue", casByUser);
        assertEquals(1.0d, agreement.getAgreement(), 0.000001d);
        assertEquals(0, agreement.getIncompleteSetsByPosition().size());
    }

    @Test
    public void singleDifferencesTest()
        throws Exception
    {
        Map<String, List<CAS>> casByUser = load(
                "casdiff/singleSpanDifference/user1.conll",
                "casdiff/singleSpanDifference/user2.conll");

        List<String> entryTypes = asList(POS.class.getName());

        List<SpanDiffAdapter> diffAdapters = asList(SpanDiffAdapter.POS_DIFF_ADAPTER);

        DiffResult result = CasDiff.doDiff(entryTypes, diffAdapters,
                LinkCompareBehavior.LINK_TARGET_AS_LABEL, casByUser);

        result.print(System.out);
        
        assertEquals(1, result.size());
        assertEquals(1, result.getDifferingConfigurationSets().size());
        assertEquals(0, result.getIncompleteConfigurationSets().size());

        AgreementResult agreement = AgreementUtils.getCohenKappaAgreement(result, entryTypes.get(0),
                "PosValue", casByUser);
        assertEquals(0.0d, agreement.getAgreement(), 0.000001d);
        assertEquals(0, agreement.getIncompleteSetsByPosition().size());
    }

    @Test
    public void singleNoDifferencesWithAdditionalCas1Test()
        throws Exception
    {
        JCas user1 = JCasFactory.createJCas();
        user1.setDocumentText("test");

        JCas user2 = JCasFactory.createJCas();
        user2.setDocumentText("test");
        
        JCas user3 = JCasFactory.createJCas();
        user3.setDocumentText("test");
        POS pos3 = new POS(user3, 0, 4);
        pos3.setPosValue("test");
        pos3.addToIndexes();
        
        Map<String, List<CAS>> casByUser = new LinkedHashMap<>();
        casByUser.put("user1", asList(user1.getCas()));
        casByUser.put("user2", asList(user2.getCas()));
        casByUser.put("user3", asList(user3.getCas()));
        
        List<String> entryTypes = asList(POS.class.getName());

        List<SpanDiffAdapter> diffAdapters = asList(SpanDiffAdapter.POS_DIFF_ADAPTER);

        DiffResult result = CasDiff.doDiff(entryTypes, diffAdapters,
                LinkCompareBehavior.LINK_TARGET_AS_LABEL, casByUser);

        result.print(System.out);
        
        casByUser.remove("user3");
        
        AgreementResult agreement = AgreementUtils.getAgreement(
                ConcreteAgreementMeasure.KRIPPENDORFF_ALPHA_NOMINAL_AGREEMENT, false, result,
                entryTypes.get(0), "PosValue", casByUser);
        
        assertEquals(1, agreement.getTotalSetCount());
        assertEquals(1, agreement.getIrrelevantSets().size());
        assertEquals(0, agreement.getRelevantSetCount());
    }

    @Test
    public void singleNoDifferencesWithAdditionalCas2Test()
        throws Exception
    {
        JCas user1 = JCasFactory.createJCas();
        user1.setDocumentText("test");

        JCas user2 = JCasFactory.createJCas();
        user2.setDocumentText("test");
        
        JCas user3 = JCasFactory.createJCas();
        user3.setDocumentText("test");
        POS pos3 = new POS(user3, 0, 4);
        pos3.addToIndexes();
        
        Map<String, List<CAS>> casByUser = new LinkedHashMap<>();
        casByUser.put("user1", asList(user1.getCas()));
        casByUser.put("user2", asList(user2.getCas()));
        casByUser.put("user3", asList(user3.getCas()));
        
        List<String> entryTypes = asList(POS.class.getName());

        List<SpanDiffAdapter> diffAdapters = asList(SpanDiffAdapter.POS_DIFF_ADAPTER);

        DiffResult result = CasDiff.doDiff(entryTypes, diffAdapters,
                LinkCompareBehavior.LINK_TARGET_AS_LABEL, casByUser);

        result.print(System.out);
        
        casByUser.remove("user3");
        
        AgreementResult agreement = AgreementUtils.getAgreement(
                ConcreteAgreementMeasure.KRIPPENDORFF_ALPHA_NOMINAL_AGREEMENT, false, result,
                entryTypes.get(0), "PosValue", casByUser);
        
        assertEquals(1, agreement.getTotalSetCount());
        assertEquals(1, agreement.getIrrelevantSets().size());
        assertEquals(0, agreement.getRelevantSetCount());
    }

    @Test
    public void twoWithoutLabelTest()
        throws Exception
    {
        JCas user1 = JCasFactory.createJCas();
        user1.setDocumentText("test");
        new POS(user1, 0, 1).addToIndexes();
        new POS(user1, 1, 2).addToIndexes();
        POS p1 = new POS(user1, 3, 4);
        p1.setPosValue("A");
        p1.addToIndexes();

        JCas user2 = JCasFactory.createJCas();
        user2.setDocumentText("test");
        new POS(user2, 0, 1).addToIndexes();
        new POS(user2, 2, 3).addToIndexes();
        POS p2 = new POS(user2, 3, 4);
        p2.setPosValue("B");
        p2.addToIndexes();
        
        
        Map<String, List<CAS>> casByUser = new LinkedHashMap<>();
        casByUser.put("user1", asList(user1.getCas()));
        casByUser.put("user2", asList(user2.getCas()));
        
        List<String> entryTypes = asList(POS.class.getName());

        List<SpanDiffAdapter> diffAdapters = asList(SpanDiffAdapter.POS_DIFF_ADAPTER);

        DiffResult result = CasDiff.doDiff(entryTypes, diffAdapters,
                LinkCompareBehavior.LINK_TARGET_AS_LABEL, casByUser);

        result.print(System.out);
        
        AgreementResult agreement = AgreementUtils.getAgreement(
                ConcreteAgreementMeasure.KRIPPENDORFF_ALPHA_NOMINAL_AGREEMENT, false, result,
                entryTypes.get(0), "PosValue", casByUser);
        
        assertEquals(4, agreement.getTotalSetCount());
        assertEquals(0, agreement.getIrrelevantSets().size());
        // the following two counts are zero because the incomplete sets are not excluded!
        assertEquals(2, agreement.getIncompleteSetsByPosition().size());
        assertEquals(0, agreement.getIncompleteSetsByLabel().size());
        assertEquals(3, agreement.getSetsWithDifferences().size());
        assertEquals(4, agreement.getRelevantSetCount());
        assertEquals(0.4, agreement.getAgreement(), 0.01);
        ICodingAnnotationItem item1 = agreement.getStudy().getItem(0);
        ICodingAnnotationItem item2 = agreement.getStudy().getItem(1);
        ICodingAnnotationItem item3 = agreement.getStudy().getItem(2);
        assertEquals("", item1.getUnit(0).getCategory());
        assertEquals("", item1.getUnit(1).getCategory());
        assertEquals("", item2.getUnit(0).getCategory());
        assertEquals(null, item2.getUnit(1).getCategory());
        assertEquals(null, item3.getUnit(0).getCategory());
        assertEquals("", item3.getUnit(1).getCategory());
    }

    @Test
    public void someDifferencesTest()
        throws Exception
    {
        Map<String, List<CAS>> casByUser = load(
                "casdiff/someDifferences/user1.conll",
                "casdiff/someDifferences/user2.conll");

        List<String> entryTypes = asList(POS.class.getName());

        List<SpanDiffAdapter> diffAdapters = asList(SpanDiffAdapter.POS_DIFF_ADAPTER);

        DiffResult result = CasDiff.doDiff(entryTypes, diffAdapters,
                LinkCompareBehavior.LINK_TARGET_AS_LABEL, casByUser);

        result.print(System.out);
        
        assertEquals(26, result.size());
        assertEquals(4, result.getDifferingConfigurationSets().size());
        assertEquals(0, result.getIncompleteConfigurationSets().size());
        
        AgreementResult agreement = AgreementUtils.getCohenKappaAgreement(result, entryTypes.get(0),
                "PosValue", casByUser);
        assertEquals(0.836477987d, agreement.getAgreement(), 0.000001d);
        assertEquals(0, agreement.getIncompleteSetsByPosition().size());
    }

    @Test
    public void singleNoDifferencesTest()
        throws Exception
    {
        Map<String, List<CAS>> casByUser = load(
                "casdiff/singleSpanNoDifference/data.conll",
                "casdiff/singleSpanNoDifference/data.conll");

        List<String> entryTypes = asList(POS.class.getName());

        List<? extends DiffAdapter> diffAdapters = asList(new SpanDiffAdapter(POS.class.getName(),
                "PosValue"));

        DiffResult result = CasDiff.doDiff(entryTypes, diffAdapters,
                LinkCompareBehavior.LINK_TARGET_AS_LABEL, casByUser);

        result.print(System.out);
        
        assertEquals(1, result.size());
        assertEquals(0, result.getDifferingConfigurationSets().size());
        assertEquals(0, result.getIncompleteConfigurationSets().size());
        
        AgreementResult agreement = AgreementUtils.getCohenKappaAgreement(result, entryTypes.get(0),
                "PosValue", casByUser);
        assertEquals(Double.NaN, agreement.getAgreement(), 0.000001d);
        assertEquals(0, agreement.getIncompleteSetsByPosition().size());
    }

    @Test
    public void relationDistanceTest()
        throws Exception
    {
        Map<String, List<CAS>> casByUser = load(
                "casdiff/relationDistance/user1.conll",
                "casdiff/relationDistance/user2.conll");

        List<String> entryTypes = asList(Dependency.class.getName());

        List<? extends DiffAdapter> diffAdapters = asList(new RelationDiffAdapter(
                Dependency.class.getName(), "Dependent", "Governor", "DependencyType"));

        DiffResult result = CasDiff.doDiff(entryTypes, diffAdapters,
                LinkCompareBehavior.LINK_TARGET_AS_LABEL, casByUser);
        
        result.print(System.out);
        
        assertEquals(27, result.size());
        assertEquals(0, result.getDifferingConfigurationSets().size());
        assertEquals(2, result.getIncompleteConfigurationSets().size());
        
        AgreementResult agreement = AgreementUtils.getCohenKappaAgreement(result, entryTypes.get(0),
                "DependencyType", casByUser);
        assertEquals(1.0, agreement.getAgreement(), 0.000001d);
        assertEquals(2, agreement.getIncompleteSetsByPosition().size());
    }

    @Test
    public void spanLabelLabelTest()
        throws Exception
    {
        Map<String, List<CAS>> casByUser = load(
                "casdiff/spanLabel/user1.conll",
                "casdiff/spanLabel/user2.conll");

        List<String> entryTypes = asList(POS.class.getName());

        List<? extends DiffAdapter> diffAdapters = asList(new SpanDiffAdapter(POS.class.getName(),
                "PosValue"));

        DiffResult result = CasDiff.doDiff(entryTypes, diffAdapters,
                LinkCompareBehavior.LINK_TARGET_AS_LABEL, casByUser);
        
        result.print(System.out);
        
        assertEquals(26, result.size());
        assertEquals(1, result.getDifferingConfigurationSets().size());
        assertEquals(0, result.getIncompleteConfigurationSets().size());
        
        AgreementResult agreement = AgreementUtils.getCohenKappaAgreement(result, entryTypes.get(0),
                "PosValue", casByUser);
        assertEquals(0.958730d, agreement.getAgreement(), 0.000001d);
        assertEquals(0, agreement.getIncompleteSetsByPosition().size());
    }

    @Test
    public void relationLabelTest()
        throws Exception
    {
        Map<String, List<CAS>> casByUser = new HashMap<>();
        casByUser.put("user1",
                asList(loadWebAnnoTsv3("testsuite/" + testContext.getMethodName() + "/user1.tsv")
                        .getCas()));
        casByUser.put("user2",
                asList(loadWebAnnoTsv3("testsuite/" + testContext.getMethodName() + "/user2.tsv")
                        .getCas()));

        List<String> entryTypes = asList(Dependency.class.getName());

        List<? extends DiffAdapter> diffAdapters = asList(new RelationDiffAdapter(
                Dependency.class.getName(), "Dependent", "Governor", "DependencyType"));

        DiffResult result = CasDiff.doDiff(entryTypes, diffAdapters,
                LinkCompareBehavior.LINK_TARGET_AS_LABEL, casByUser);

        result.print(System.out);
        
        assertEquals(26, result.size());
        assertEquals(1, result.getDifferingConfigurationSets().size());
        assertEquals(0, result.getIncompleteConfigurationSets().size());
        
        AgreementResult agreement = AgreementUtils.getCohenKappaAgreement(result, entryTypes.get(0),
                "DependencyType", casByUser);
        assertEquals(0.958199d, agreement.getAgreement(), 0.000001d);
        assertEquals(0, agreement.getIncompleteSetsByPosition().size());
    }
    
    @Test
    public void relationStackedSpansTest()
        throws Exception
    {
        TypeSystemDescription global = TypeSystemDescriptionFactory.createTypeSystemDescription();
        TypeSystemDescription local = TypeSystemDescriptionFactory
                .createTypeSystemDescriptionFromPath(
                        "src/test/resources/desc/type/webannoTestTypes.xml");
       
        TypeSystemDescription merged = CasCreationUtils.mergeTypeSystems(asList(global, local));

        TokenBuilder<Token, Sentence> tb = new TokenBuilder<>(Token.class,
                Sentence.class);
        
        JCas jcasA = JCasFactory.createJCas(merged);
        {
            CAS casA = jcasA.getCas();
            tb.buildTokens(jcasA, "This is a test .");
            
            List<Token> tokensA = new ArrayList<>(select(jcasA, Token.class));
            Token t1A = tokensA.get(0);
            Token t2A = tokensA.get(tokensA.size() - 1);
            
            NamedEntity govA = new NamedEntity(jcasA, t1A.getBegin(), t1A.getEnd());
            govA.addToIndexes();
            // Here we add a stacked named entity!
            new NamedEntity(jcasA, t1A.getBegin(), t1A.getEnd()).addToIndexes();
            
            NamedEntity depA =  new NamedEntity(jcasA, t2A.getBegin(), t2A.getEnd());
            depA.addToIndexes();
    
            Type relationTypeA = casA.getTypeSystem().getType("webanno.custom.Relation");
            AnnotationFS fs1A = casA.createAnnotation(relationTypeA, depA.getBegin(),
                    depA.getEnd());
            FSUtil.setFeature(fs1A, "Governor", govA);
            FSUtil.setFeature(fs1A, "Dependent", depA);
            FSUtil.setFeature(fs1A, "value", "REL");
            casA.addFsToIndexes(fs1A);
        }

        JCas jcasB = JCasFactory.createJCas(merged);
        {
            CAS casB = jcasB.getCas();
            tb.buildTokens(jcasB, "This is a test .");
            
            List<Token> tokensB = new ArrayList<>(select(jcasB, Token.class));
            Token t1B = tokensB.get(0);
            Token t2B = tokensB.get(tokensB.size() - 1);
            
            NamedEntity govB = new NamedEntity(jcasB, t1B.getBegin(), t1B.getEnd());
            govB.addToIndexes();
            NamedEntity depB =  new NamedEntity(jcasB, t2B.getBegin(), t2B.getEnd());
            depB.addToIndexes();
    
            Type relationTypeB = casB.getTypeSystem().getType("webanno.custom.Relation");
            AnnotationFS fs1B = casB.createAnnotation(relationTypeB, depB.getBegin(),
                    depB.getEnd());
            FSUtil.setFeature(fs1B, "Governor", govB);
            FSUtil.setFeature(fs1B, "Dependent", depB);
            FSUtil.setFeature(fs1B, "value", "REL");
            casB.addFsToIndexes(fs1B);
        }

        Map<String, List<CAS>> casByUser = new LinkedHashMap<>();
        casByUser.put("user1", asList(jcasA.getCas()));
        casByUser.put("user2", asList(jcasB.getCas()));

        List<String> entryTypes = asList("webanno.custom.Relation");

        List<? extends DiffAdapter> diffAdapters = asList(new RelationDiffAdapter(
                "webanno.custom.Relation", WebAnnoConst.FEAT_REL_TARGET,
                WebAnnoConst.FEAT_REL_SOURCE, "value"));

        DiffResult diff = CasDiff.doDiff(entryTypes, diffAdapters,
                LinkCompareBehavior.LINK_TARGET_AS_LABEL, casByUser);
        
        diff.print(System.out);
        
        assertEquals(1, diff.size());
        assertEquals(0, diff.getDifferingConfigurationSets().size());
        assertEquals(0, diff.getIncompleteConfigurationSets().size());
        
        // Check against new impl
        AgreementResult agreement = AgreementUtils.getCohenKappaAgreement(diff,
                "webanno.custom.Relation", "value", casByUser);

        // Asserts
        System.out.printf("Agreement: %s%n", agreement.toString());
        AgreementUtils.dumpAgreementStudy(System.out, agreement);

        assertEquals(1, agreement.getPluralitySets().size());
    }

    @Test
    public void multiLinkWithRoleNoDifferenceTest()
        throws Exception
    {
        JCas jcasA = createJCas(createMultiLinkWithRoleTestTypeSytem());
        makeLinkHostFS(jcasA, 0, 0, makeLinkFS(jcasA, "slot1", 0, 0));        
        makeLinkHostFS(jcasA, 10, 10, makeLinkFS(jcasA, "slot1", 10, 10));

        JCas jcasB = createJCas(createMultiLinkWithRoleTestTypeSytem());
        makeLinkHostFS(jcasB, 0, 0, makeLinkFS(jcasB, "slot1", 0, 0));
        makeLinkHostFS(jcasB, 10, 10, makeLinkFS(jcasB, "slot1", 10, 10));

        Map<String, List<CAS>> casByUser = new LinkedHashMap<>();
        casByUser.put("user1", asList(jcasA.getCas()));
        casByUser.put("user2", asList(jcasB.getCas()));

        List<String> entryTypes = asList(HOST_TYPE);

        SpanDiffAdapter adapter = new SpanDiffAdapter(HOST_TYPE);
        adapter.addLinkFeature("links", "role", "target");
        List<? extends DiffAdapter> diffAdapters = asList(adapter);

        DiffResult diff = CasDiff.doDiff(entryTypes, diffAdapters,
                LinkCompareBehavior.LINK_TARGET_AS_LABEL, casByUser);
        
        diff.print(System.out);
        
        assertEquals(4, diff.size());
        assertEquals(0, diff.getDifferingConfigurationSets().size());
        assertEquals(0, diff.getIncompleteConfigurationSets().size());
        
        // Check against new impl
        AgreementResult agreement = getCohenKappaAgreement(diff, HOST_TYPE, "links",
                casByUser);

        // Asserts
        System.out.printf("Agreement: %s%n", agreement.toString());
        AgreementUtils.dumpAgreementStudy(System.out, agreement);
        
        assertEquals(1.0d, agreement.getAgreement(), 0.00001d);
    }

    @Test
    public void multiLinkWithRoleLabelDifferenceTest()
        throws Exception
    {
        JCas jcasA = createJCas(createMultiLinkWithRoleTestTypeSytem());
        makeLinkHostFS(jcasA, 0, 0, makeLinkFS(jcasA, "slot1", 0, 0));     

        JCas jcasB = JCasFactory.createJCas(createMultiLinkWithRoleTestTypeSytem());
        makeLinkHostFS(jcasB, 0, 0, makeLinkFS(jcasB, "slot2", 0, 0));

        Map<String, List<CAS>> casByUser = new LinkedHashMap<>();
        casByUser.put("user1", asList(jcasA.getCas()));
        casByUser.put("user2", asList(jcasB.getCas()));

        List<String> entryTypes = asList(HOST_TYPE);

        SpanDiffAdapter adapter = new SpanDiffAdapter(HOST_TYPE);
        adapter.addLinkFeature("links", "role", "target");
        List<? extends DiffAdapter> diffAdapters = asList(adapter);

        DiffResult diff = CasDiff.doDiff(entryTypes, diffAdapters,
                LinkCompareBehavior.LINK_TARGET_AS_LABEL, casByUser);
        
        diff.print(System.out);
        
        assertEquals(3, diff.size());
        assertEquals(0, diff.getDifferingConfigurationSets().size());
        assertEquals(2, diff.getIncompleteConfigurationSets().size());
        
        // Check against new impl
        AgreementResult agreement = AgreementUtils.getCohenKappaAgreement(diff, HOST_TYPE, "links",
                casByUser);

        // Asserts
        System.out.printf("Agreement: %s%n", agreement.toString());
        AgreementUtils.dumpAgreementStudy(System.out, agreement);
        
        assertEquals(Double.NaN, agreement.getAgreement(), 0.00001d);
    }

    @Test
    public void multiLinkWithRoleLabelDifferenceTest2()
        throws Exception
    {
        JCas jcasA = createJCas(createMultiLinkWithRoleTestTypeSytem());
        makeLinkHostFS(jcasA, 0, 0, makeLinkFS(jcasA, "slot1", 0, 0));     

        JCas jcasB = createJCas(createMultiLinkWithRoleTestTypeSytem());
        makeLinkHostFS(jcasB, 0, 0, makeLinkFS(jcasB, "slot2", 0, 0));

        Map<String, List<CAS>> casByUser = new LinkedHashMap<>();
        casByUser.put("user1", asList(jcasA.getCas()));
        casByUser.put("user2", asList(jcasB.getCas()));

        List<String> entryTypes = asList(HOST_TYPE);

        SpanDiffAdapter adapter = new SpanDiffAdapter(HOST_TYPE);
        adapter.addLinkFeature("links", "role", "target");
        List<? extends DiffAdapter> diffAdapters = asList(adapter);

        DiffResult diff = CasDiff.doDiff(entryTypes, diffAdapters,
                LinkCompareBehavior.LINK_ROLE_AS_LABEL, casByUser);
        
        diff.print(System.out);
        
        assertEquals(2, diff.size());
        assertEquals(1, diff.getDifferingConfigurationSets().size());
        assertEquals(0, diff.getIncompleteConfigurationSets().size());
        
        // Check against new impl
        AgreementResult agreement = AgreementUtils.getCohenKappaAgreement(diff, HOST_TYPE, "links",
                casByUser);

        // Asserts
        System.out.printf("Agreement: %s%n", agreement.toString());
        AgreementUtils.dumpAgreementStudy(System.out, agreement);
        
        assertEquals(0.0d, agreement.getAgreement(), 0.00001d);
    }

    @Test
    public void multiLinkWithRoleTargetDifferenceTest()
        throws Exception
    {
        JCas jcasA = createJCas(createMultiLinkWithRoleTestTypeSytem());
        makeLinkHostFS(jcasA, 0, 0, makeLinkFS(jcasA, "slot1", 0, 0));      

        JCas jcasB = createJCas(createMultiLinkWithRoleTestTypeSytem());
        makeLinkHostFS(jcasB, 0, 0, makeLinkFS(jcasB, "slot1", 10, 10));

        Map<String, List<CAS>> casByUser = new LinkedHashMap<>();
        casByUser.put("user1", asList(jcasA.getCas()));
        casByUser.put("user2", asList(jcasB.getCas()));

        List<String> entryTypes = asList(HOST_TYPE);

        SpanDiffAdapter adapter = new SpanDiffAdapter(HOST_TYPE);
        adapter.addLinkFeature("links", "role", "target");
        List<? extends DiffAdapter> diffAdapters = asList(adapter);

        DiffResult diff = CasDiff.doDiff(entryTypes, diffAdapters,
                LinkCompareBehavior.LINK_TARGET_AS_LABEL, casByUser);
        
        diff.print(System.out);
        
        assertEquals(2, diff.size());
        assertEquals(1, diff.getDifferingConfigurationSets().size());
        assertEquals(0, diff.getIncompleteConfigurationSets().size());

        // Check against new impl
        AgreementResult agreement = getCohenKappaAgreement(diff, HOST_TYPE, "links",
                casByUser);

        // Asserts
        System.out.printf("Agreement: %s%n", agreement.toString());
        AgreementUtils.dumpAgreementStudy(System.out, agreement);
        
        assertEquals(0.0, agreement.getAgreement(), 0.00001d);
    }

    @Test
    public void multiLinkWithRoleMultiTargetDifferenceTest()
        throws Exception
    {
        JCas jcasA = JCasFactory.createJCas(createMultiLinkWithRoleTestTypeSytem());
        makeLinkHostFS(jcasA, 0, 0, 
                makeLinkFS(jcasA, "slot1", 0, 0),
                makeLinkFS(jcasA, "slot1", 10, 10));      

        JCas jcasB = JCasFactory.createJCas(createMultiLinkWithRoleTestTypeSytem());
        makeLinkHostFS(jcasB, 0, 0, 
                makeLinkFS(jcasB, "slot1", 10, 10));

        Map<String, List<CAS>> casByUser = new LinkedHashMap<>();
        casByUser.put("user1", asList(jcasA.getCas()));
        casByUser.put("user2", asList(jcasB.getCas()));

        List<String> entryTypes = asList(HOST_TYPE);

        SpanDiffAdapter adapter = new SpanDiffAdapter(HOST_TYPE);
        adapter.addLinkFeature("links", "role", "target");
        List<? extends DiffAdapter> diffAdapters = asList(adapter);

        DiffResult diff = CasDiff.doDiff(entryTypes, diffAdapters,
                LinkCompareBehavior.LINK_TARGET_AS_LABEL, casByUser);
        
        diff.print(System.out);
        
        assertEquals(2, diff.size());
        assertEquals(1, diff.getDifferingConfigurationSets().size());
        assertEquals(0, diff.getIncompleteConfigurationSets().size());
        
//        // Check against new impl
//        AgreementResult agreement = AgreementUtils.getCohenKappaAgreement(diff, HOST_TYPE, "links",
//                casByUser);
//
//        // Asserts
//        System.out.printf("Agreement: %s%n", agreement.toString());
//        AgreementUtils.dumpAgreementStudy(System.out, agreement);
//        
//        assertEquals(0.0, agreement.getAgreement(), 0.00001d);
    }

    @Test
    public void multiLinkWithRoleMultiTargetDifferenceTest2()
        throws Exception
    {
        JCas jcasA = JCasFactory.createJCas(createMultiLinkWithRoleTestTypeSytem());
        makeLinkHostFS(jcasA, 0, 0, 
                makeLinkFS(jcasA, "slot1", 0, 0),
                makeLinkFS(jcasA, "slot1", 10, 10));

        JCas jcasB = JCasFactory.createJCas(createMultiLinkWithRoleTestTypeSytem());
        makeLinkHostFS(jcasB, 0, 0, 
                makeLinkFS(jcasB, "slot2", 10, 10));

        Map<String, List<CAS>> casByUser = new LinkedHashMap<>();
        casByUser.put("user1", asList(jcasA.getCas()));
        casByUser.put("user2", asList(jcasB.getCas()));

        List<String> entryTypes = asList(HOST_TYPE);

        SpanDiffAdapter adapter = new SpanDiffAdapter(HOST_TYPE);
        adapter.addLinkFeature("links", "role", "target");
        List<? extends DiffAdapter> diffAdapters = asList(adapter);

        DiffResult diff = CasDiff.doDiff(entryTypes, diffAdapters,
                LinkCompareBehavior.LINK_TARGET_AS_LABEL, casByUser);
        
        diff.print(System.out);
        
        assertEquals(3, diff.size());
        assertEquals(1, diff.getDifferingConfigurationSets().size());
        assertEquals(2, diff.getIncompleteConfigurationSets().size());

//        // Check against new impl
//        AgreementResult agreement = AgreementUtils.getCohenKappaAgreement(diff, HOST_TYPE, "links",
//                casByUser);
//
//        // Asserts
//        System.out.printf("Agreement: %s%n", agreement.toString());
//        AgreementUtils.dumpAgreementStudy(System.out, agreement);
//        
//        assertEquals(0.0, agreement.getAgreement(), 0.00001d);
    }
    
    @Rule
    public DkproTestContext testContext = new DkproTestContext();
}

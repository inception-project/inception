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
package de.tudarmstadt.ukp.clarin.webanno.curation.casdiff;

import static de.tudarmstadt.ukp.clarin.webanno.curation.casdiff.CasDiff.doDiff;
import static de.tudarmstadt.ukp.clarin.webanno.curation.casdiff.CurationTestUtils.HOST_TYPE;
import static de.tudarmstadt.ukp.clarin.webanno.curation.casdiff.CurationTestUtils.createMultiLinkWithRoleTestTypeSystem;
import static de.tudarmstadt.ukp.clarin.webanno.curation.casdiff.CurationTestUtils.load;
import static de.tudarmstadt.ukp.clarin.webanno.curation.casdiff.CurationTestUtils.loadWebAnnoTsv3;
import static de.tudarmstadt.ukp.clarin.webanno.curation.casdiff.CurationTestUtils.makeLinkFS;
import static de.tudarmstadt.ukp.clarin.webanno.curation.casdiff.CurationTestUtils.makeLinkHostFS;
import static de.tudarmstadt.ukp.clarin.webanno.curation.casdiff.LinkCompareBehavior.LINK_ROLE_AS_LABEL;
import static de.tudarmstadt.ukp.clarin.webanno.curation.casdiff.LinkCompareBehavior.LINK_TARGET_AS_LABEL;
import static de.tudarmstadt.ukp.clarin.webanno.curation.casdiff.relation.RelationDiffAdapter.DEPENDENCY_DIFF_ADAPTER;
import static de.tudarmstadt.ukp.clarin.webanno.curation.casdiff.span.SpanDiffAdapter.POS_DIFF_ADAPTER;
import static de.tudarmstadt.ukp.inception.support.uima.AnnotationBuilder.buildAnnotation;
import static java.util.Arrays.asList;
import static org.apache.uima.fit.factory.CasFactory.createText;
import static org.apache.uima.fit.factory.JCasFactory.createJCas;
import static org.apache.uima.fit.util.JCasUtil.select;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.apache.uima.cas.CAS;
import org.apache.uima.cas.Type;
import org.apache.uima.cas.text.AnnotationFS;
import org.apache.uima.fit.factory.CasFactory;
import org.apache.uima.fit.factory.JCasFactory;
import org.apache.uima.fit.factory.TypeSystemDescriptionFactory;
import org.apache.uima.fit.testing.factory.TokenBuilder;
import org.apache.uima.fit.util.FSUtil;
import org.apache.uima.jcas.JCas;
import org.apache.uima.resource.metadata.TypeSystemDescription;
import org.apache.uima.util.CasCreationUtils;
import org.junit.jupiter.api.Test;

import de.tudarmstadt.ukp.clarin.webanno.curation.casdiff.api.DiffAdapter;
import de.tudarmstadt.ukp.clarin.webanno.curation.casdiff.relation.RelationDiffAdapter;
import de.tudarmstadt.ukp.clarin.webanno.curation.casdiff.span.SpanDiffAdapter;
import de.tudarmstadt.ukp.dkpro.core.api.lexmorph.type.pos.POS;
import de.tudarmstadt.ukp.dkpro.core.api.ner.type.NamedEntity;
import de.tudarmstadt.ukp.dkpro.core.api.segmentation.type.Lemma;
import de.tudarmstadt.ukp.dkpro.core.api.segmentation.type.Sentence;
import de.tudarmstadt.ukp.dkpro.core.api.segmentation.type.Token;
import de.tudarmstadt.ukp.dkpro.core.api.syntax.type.dependency.Dependency;
import de.tudarmstadt.ukp.inception.support.WebAnnoConst;

public class CasDiffTest
{
    @Test
    public void noDataTest() throws Exception
    {
        var diffAdapters = new ArrayList<DiffAdapter>();

        var casByUser = new LinkedHashMap<String, CAS>();

        var result = doDiff(diffAdapters, LINK_TARGET_AS_LABEL, casByUser).toResult();

        // result.print(System.out);

        assertEquals(0, result.size());
        assertEquals(0, result.getDifferingConfigurationSets().size());
        assertEquals(0, result.getIncompleteConfigurationSets().size());
    }

    @Test
    public void singleEmptyCasTest() throws Exception
    {
        String text = "";

        var user1Cas = CasFactory.createCas();
        user1Cas.setDocumentText(text);

        var casByUser = Map.of("user1", user1Cas);

        var diffAdapters = asList(new SpanDiffAdapter(Token.class.getName()));

        var result = doDiff(diffAdapters, LINK_TARGET_AS_LABEL, casByUser).toResult();

        // result.print(System.out);

        assertEquals(0, result.size());
        assertEquals(0, result.getDifferingConfigurationSets().size());
    }

    @Test
    public void multipleEmptyCasWithMissingOnesTest() throws Exception
    {
        String text = "";

        CAS user1Cas1 = null;
        CAS user2Cas1 = createText(text);

        var casByUser = new LinkedHashMap<String, CAS>();
        casByUser.put("user1", user1Cas1);
        casByUser.put("user2", user2Cas1);

        var diffAdapters = asList(new SpanDiffAdapter(Lemma.class.getName()));

        var diff = doDiff(diffAdapters, LINK_TARGET_AS_LABEL, casByUser);
        var result = diff.toResult();

        // result.print(System.out);

        assertEquals(0, result.size());
        assertEquals(0, result.getDifferingConfigurationSets().size());
        assertEquals(0, result.getIncompleteConfigurationSets().size());

        // Todo: Agreement has moved to separate project - should create agreement test there
        // CodingAgreementResult agreement = AgreementUtils.getCohenKappaAgreement(diff,
        // entryTypes.get(0), "value", casByUser);
        // assertEquals(Double.NaN, agreement.getAgreement(), 0.000001d);
        // assertEquals(0, agreement.getIncompleteSetsByPosition().size());
    }

    @Test
    public void noDifferencesPosTest() throws Exception
    {
        var casByUser = load( //
                "casdiff/noDifferences/data.conll", //
                "casdiff/noDifferences/data.conll");

        var diffAdapters = asList(POS_DIFF_ADAPTER);

        var diff = doDiff(diffAdapters, LINK_TARGET_AS_LABEL, casByUser);
        var result = diff.toResult();

        // result.print(System.out);

        assertEquals(26, result.size());
        assertEquals(0, result.getDifferingConfigurationSets().size());
        assertEquals(0, result.getIncompleteConfigurationSets().size());

        // Todo: Agreement has moved to separate project - should create agreement test there
        // CodingAgreementResult agreement = AgreementUtils.getCohenKappaAgreement(diff,
        // entryTypes.get(0), "PosValue", casByUser);
        // assertEquals(1.0d, agreement.getAgreement(), 0.000001d);
        // assertEquals(0, agreement.getIncompleteSetsByPosition().size());
    }

    @Test
    public void noDifferencesDependencyTest() throws Exception
    {
        Map<String, CAS> casByUser = load( //
                "casdiff/noDifferences/data.conll", //
                "casdiff/noDifferences/data.conll");

        var diffAdapters = asList(DEPENDENCY_DIFF_ADAPTER);

        var diff = doDiff(diffAdapters, LINK_TARGET_AS_LABEL, casByUser);
        var result = diff.toResult();

        // result.print(System.out);

        assertEquals(26, result.size());
        assertEquals(0, result.getDifferingConfigurationSets().size());
        assertEquals(0, result.getIncompleteConfigurationSets().size());

        // Todo: Agreement has moved to separate project - should create agreement test there
        // CodingAgreementResult agreement = AgreementUtils.getCohenKappaAgreement(diff,
        // entryTypes.get(0), "DependencyType", casByUser);
        // assertEquals(1.0d, agreement.getAgreement(), 0.000001d);
        // assertEquals(0, agreement.getIncompleteSetsByPosition().size());
    }

    @Test
    public void noDifferencesPosDependencyTest() throws Exception
    {
        var casByUser = load( //
                "casdiff/noDifferences/data.conll", //
                "casdiff/noDifferences/data.conll");

        var diffAdapters = asList(POS_DIFF_ADAPTER, DEPENDENCY_DIFF_ADAPTER);

        var diff = doDiff(diffAdapters, LINK_TARGET_AS_LABEL, casByUser);
        var result = diff.toResult();

        // result.print(System.out);

        assertEquals(52, result.size());
        assertEquals(26, result.size(POS.class.getName()));
        assertEquals(26, result.size(Dependency.class.getName()));
        assertEquals(0, result.getDifferingConfigurationSets().size());
        assertEquals(0, result.getIncompleteConfigurationSets().size());

        // Todo: Agreement has moved to separate project - should create agreement test there
        // CodingAgreementResult agreement = AgreementUtils.getCohenKappaAgreement(diff,
        // entryTypes.get(0), "PosValue", casByUser);
        // assertEquals(1.0d, agreement.getAgreement(), 0.000001d);
        // assertEquals(0, agreement.getIncompleteSetsByPosition().size());
    }

    @Test
    public void singleDifferencesTest() throws Exception
    {
        var casByUser = load( //
                "casdiff/singleSpanDifference/user1.conll", //
                "casdiff/singleSpanDifference/user2.conll");

        var diffAdapters = asList(POS_DIFF_ADAPTER);

        var diff = doDiff(diffAdapters, LINK_TARGET_AS_LABEL, casByUser);
        var result = diff.toResult();

        // result.print(System.out);

        assertEquals(1, result.size());
        assertEquals(1, result.getDifferingConfigurationSets().size());
        assertEquals(0, result.getIncompleteConfigurationSets().size());

        // Todo: Agreement has moved to separate project - should create agreement test there
        // CodingAgreementResult agreement = AgreementUtils.getCohenKappaAgreement(diff,
        // entryTypes.get(0), "PosValue", casByUser);
        // assertEquals(0.0d, agreement.getAgreement(), 0.000001d);
        // assertEquals(0, agreement.getIncompleteSetsByPosition().size());
    }

    @Test
    public void someDifferencesTest() throws Exception
    {
        var casByUser = load( //
                "casdiff/someDifferences/user1.conll", //
                "casdiff/someDifferences/user2.conll");

        var diffAdapters = asList(POS_DIFF_ADAPTER);

        var diff = doDiff(diffAdapters, LINK_TARGET_AS_LABEL, casByUser);
        var result = diff.toResult();

        // result.print(System.out);

        assertEquals(26, result.size());
        assertEquals(4, result.getDifferingConfigurationSets().size());
        assertEquals(0, result.getIncompleteConfigurationSets().size());

        // Todo: Agreement has moved to separate project - should create agreement test there
        // CodingAgreementResult agreement = getCohenKappaAgreement(diff, entryTypes.get(0),
        // "PosValue", casByUser);
        // assertEquals(0.836477987d, agreement.getAgreement(), 0.000001d);
        // assertEquals(0, agreement.getIncompleteSetsByPosition().size());
    }

    @Test
    public void singleNoDifferencesTest() throws Exception
    {
        var casByUser = load( //
                "casdiff/singleSpanNoDifference/data.conll", //
                "casdiff/singleSpanNoDifference/data.conll");

        var diffAdapters = asList(new SpanDiffAdapter(POS.class.getName(), "PosValue"));

        var diff = doDiff(diffAdapters, LINK_TARGET_AS_LABEL, casByUser);
        var result = diff.toResult();

        // result.print(System.out);

        assertEquals(1, result.size());
        assertEquals(0, result.getDifferingConfigurationSets().size());
        assertEquals(0, result.getIncompleteConfigurationSets().size());

        // Todo: Agreement has moved to separate project - should create agreement test there
        // CodingAgreementResult agreement = getCohenKappaAgreement(diff, entryTypes.get(0),
        // "PosValue", casByUser);
        // assertEquals(NaN, agreement.getAgreement(), 0.000001d);
        // assertEquals(0, agreement.getIncompleteSetsByPosition().size());
    }

    @Test
    public void relationDistanceTest() throws Exception
    {
        var casByUser = load( //
                "casdiff/relationDistance/user1.conll", //
                "casdiff/relationDistance/user2.conll");

        var diffAdapters = asList(new RelationDiffAdapter(Dependency.class.getName(), "Dependent",
                "Governor", "DependencyType"));

        var diff = doDiff(diffAdapters, LINK_TARGET_AS_LABEL, casByUser);
        var result = diff.toResult();

        // result.print(System.out);

        assertEquals(27, result.size());
        assertEquals(0, result.getDifferingConfigurationSets().size());
        assertEquals(2, result.getIncompleteConfigurationSets().size());

        // Todo: Agreement has moved to separate project - should create agreement test there
        // CodingAgreementResult agreement = getCohenKappaAgreement(diff, entryTypes.get(0),
        // "DependencyType", casByUser);
        // assertEquals(1.0, agreement.getAgreement(), 0.000001d);
        // assertEquals(2, agreement.getIncompleteSetsByPosition().size());
    }

    @Test
    public void spanLabelLabelTest() throws Exception
    {
        var casByUser = load( //
                "casdiff/spanLabel/user1.conll", //
                "casdiff/spanLabel/user2.conll");

        var diffAdapters = asList(new SpanDiffAdapter(POS.class.getName(), "PosValue"));

        var diff = doDiff(diffAdapters, LINK_TARGET_AS_LABEL, casByUser);
        var result = diff.toResult();

        // result.print(System.out);

        assertEquals(26, result.size());
        assertEquals(1, result.getDifferingConfigurationSets().size());
        assertEquals(0, result.getIncompleteConfigurationSets().size());

        // Todo: Agreement has moved to separate project - should create agreement test there
        // CodingAgreementResult agreement = getCohenKappaAgreement(diff, entryTypes.get(0),
        // "PosValue", casByUser);
        // assertEquals(0.958730d, agreement.getAgreement(), 0.000001d);
        // assertEquals(0, agreement.getIncompleteSetsByPosition().size());
    }

    @Test
    public void relationLabelTest() throws Exception
    {
        var casByUser = new HashMap<String, CAS>();
        casByUser.put("user1", loadWebAnnoTsv3("casdiff/relationLabelTest/user1.tsv").getCas());
        casByUser.put("user2", loadWebAnnoTsv3("casdiff/relationLabelTest/user2.tsv").getCas());

        var diffAdapters = asList(new RelationDiffAdapter(Dependency.class.getName(), "Dependent",
                "Governor", "DependencyType"));

        var diff = doDiff(diffAdapters, LINK_TARGET_AS_LABEL, casByUser);
        var result = diff.toResult();

        // result.print(System.out);

        assertEquals(26, result.size());
        assertEquals(1, result.getDifferingConfigurationSets().size());
        assertEquals(0, result.getIncompleteConfigurationSets().size());

        // Todo: Agreement has moved to separate project - should create agreement test there
        // CodingAgreementResult agreement = getCohenKappaAgreement(diff, entryTypes.get(0),
        // "DependencyType", casByUser);
        // assertEquals(0.958199d, agreement.getAgreement(), 0.000001d);
        // assertEquals(0, agreement.getIncompleteSetsByPosition().size());
    }

    @Test
    public void relationStackedSpansTest() throws Exception
    {
        TypeSystemDescription global = TypeSystemDescriptionFactory.createTypeSystemDescription();
        TypeSystemDescription local = TypeSystemDescriptionFactory
                .createTypeSystemDescriptionFromPath(
                        "src/test/resources/desc/type/webannoTestTypes.xml");

        TypeSystemDescription merged = CasCreationUtils.mergeTypeSystems(asList(global, local));

        TokenBuilder<Token, Sentence> tb = new TokenBuilder<>(Token.class, Sentence.class);

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

            NamedEntity depA = new NamedEntity(jcasA, t2A.getBegin(), t2A.getEnd());
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
            NamedEntity depB = new NamedEntity(jcasB, t2B.getBegin(), t2B.getEnd());
            depB.addToIndexes();

            Type relationTypeB = casB.getTypeSystem().getType("webanno.custom.Relation");
            AnnotationFS fs1B = casB.createAnnotation(relationTypeB, depB.getBegin(),
                    depB.getEnd());
            FSUtil.setFeature(fs1B, "Governor", govB);
            FSUtil.setFeature(fs1B, "Dependent", depB);
            FSUtil.setFeature(fs1B, "value", "REL");
            casB.addFsToIndexes(fs1B);
        }

        var casByUser = new LinkedHashMap<String, CAS>();
        casByUser.put("user1", jcasA.getCas());
        casByUser.put("user2", jcasB.getCas());

        var diffAdapters = asList(new RelationDiffAdapter("webanno.custom.Relation",
                WebAnnoConst.FEAT_REL_TARGET, WebAnnoConst.FEAT_REL_SOURCE, "value"));

        var diff = doDiff(diffAdapters, LINK_TARGET_AS_LABEL, casByUser);
        var result = diff.toResult();

        // result.print(System.out);

        assertEquals(1, result.size());
        assertEquals(0, result.getDifferingConfigurationSets().size());
        assertEquals(0, result.getIncompleteConfigurationSets().size());

        // Todo: Agreement has moved to separate project - should create agreement test there
        // CodingAgreementResult agreement = AgreementUtils.getCohenKappaAgreement(diff,
        // "webanno.custom.Relation", "value", casByUser);
        //
        // // Asserts
        // System.out.printf("Agreement: %s%n", agreement.toString());
        // AgreementUtils.dumpAgreementStudy(System.out, agreement);
        //
        // assertEquals(1, agreement.getPluralitySets().size());
    }

    @Test
    public void multiValueStringFeatureDifferenceTestWithNull() throws Exception
    {
        var cas1 = createText("");
        buildAnnotation(cas1, "webanno.custom.SpanMultiValue") //
                .withFeature("values", asList("a", "b")) //
                .buildAndAddToIndexes();

        var cas2 = createText("");
        buildAnnotation(cas2, "webanno.custom.SpanMultiValue") //
                .buildAndAddToIndexes();

        var casByUser = Map.of( //
                "user1", cas1, //
                "user2", cas2);

        var adapter = new SpanDiffAdapter("webanno.custom.SpanMultiValue", "values");

        var diff = doDiff(asList(adapter), LINK_TARGET_AS_LABEL, casByUser);
        var result = diff.toResult();

        // result.print(System.out);

        assertThat(result.size()).isEqualTo(1);
        assertThat(result.getDifferingConfigurationSets()).hasSize(1);
        assertThat(result.getIncompleteConfigurationSets()).isEmpty();
    }

    @Test
    public void multiValueStringFeatureDifferenceTest() throws Exception
    {
        var cas1 = createText("");
        buildAnnotation(cas1, "webanno.custom.SpanMultiValue") //
                .withFeature("values", asList("a", "b")) //
                .buildAndAddToIndexes();

        var cas2 = createText("");
        buildAnnotation(cas2, "webanno.custom.SpanMultiValue") //
                .withFeature("values", asList("a")) //
                .buildAndAddToIndexes();

        var casByUser = Map.of( //
                "user1", cas1, //
                "user2", cas2);

        var adapter = new SpanDiffAdapter("webanno.custom.SpanMultiValue", "values");

        var diff = doDiff(asList(adapter), LINK_TARGET_AS_LABEL, casByUser);
        var result = diff.toResult();

        // result.print(System.out);

        assertThat(result.size()).isEqualTo(1);
        assertThat(result.getDifferingConfigurationSets()).hasSize(1);
        assertThat(result.getIncompleteConfigurationSets()).isEmpty();
    }

    @Test
    public void multiValueStringFeatureNoDifferenceTest() throws Exception
    {
        var cas1 = createText("");
        buildAnnotation(cas1, "webanno.custom.SpanMultiValue") //
                .withFeature("values", asList("a", "b")) //
                .buildAndAddToIndexes();

        var cas2 = createText("");
        buildAnnotation(cas2, "webanno.custom.SpanMultiValue") //
                .withFeature("values", asList("b", "a")) //
                .buildAndAddToIndexes();

        var casByUser = Map.of( //
                "user1", cas1, //
                "user2", cas2);

        var adapter = new SpanDiffAdapter("webanno.custom.SpanMultiValue", "values");

        var diff = doDiff(asList(adapter), LINK_TARGET_AS_LABEL, casByUser);
        var result = diff.toResult();

        // result.print(System.out);

        assertThat(result.size()).isEqualTo(1);
        assertThat(result.getDifferingConfigurationSets()).isEmpty();
        assertThat(result.getIncompleteConfigurationSets()).isEmpty();
    }

    @Test
    public void multiLinkWithRoleNoDifferenceTest() throws Exception
    {
        JCas jcasA = createJCas(createMultiLinkWithRoleTestTypeSystem());
        makeLinkHostFS(jcasA, 0, 0, makeLinkFS(jcasA, "slot1", 0, 0));
        makeLinkHostFS(jcasA, 10, 10, makeLinkFS(jcasA, "slot1", 10, 10));

        JCas jcasB = createJCas(createMultiLinkWithRoleTestTypeSystem());
        makeLinkHostFS(jcasB, 0, 0, makeLinkFS(jcasB, "slot1", 0, 0));
        makeLinkHostFS(jcasB, 10, 10, makeLinkFS(jcasB, "slot1", 10, 10));

        var casByUser = new LinkedHashMap<String, CAS>();
        casByUser.put("user1", jcasA.getCas());
        casByUser.put("user2", jcasB.getCas());

        var adapter = new SpanDiffAdapter(HOST_TYPE);
        adapter.addLinkFeature("links", "role", "target");
        var diff = doDiff(asList(adapter), LINK_TARGET_AS_LABEL, casByUser);
        var result = diff.toResult();

        // result.print(System.out);

        assertEquals(4, result.size());
        assertEquals(0, result.getDifferingConfigurationSets().size());
        assertEquals(0, result.getIncompleteConfigurationSets().size());

        // Todo: Agreement has moved to separate project - should create agreement test there
        // CodingAgreementResult agreement = getCohenKappaAgreement(diff, HOST_TYPE, "links",
        // casByUser);
        //
        // // Asserts
        // System.out.printf("Agreement: %s%n", agreement.toString());
        // AgreementUtils.dumpAgreementStudy(System.out, agreement);
        //
        // assertEquals(1.0d, agreement.getAgreement(), 0.00001d);
    }

    @Test
    public void multiLinkWithRoleLabelDifferenceTest2() throws Exception
    {
        JCas jcasA = createJCas(createMultiLinkWithRoleTestTypeSystem());
        makeLinkHostFS(jcasA, 0, 0, makeLinkFS(jcasA, "slot1", 0, 0));

        JCas jcasB = createJCas(createMultiLinkWithRoleTestTypeSystem());
        makeLinkHostFS(jcasB, 0, 0, makeLinkFS(jcasB, "slot2", 0, 0));

        var casByUser = new LinkedHashMap<String, CAS>();
        casByUser.put("user1", jcasA.getCas());
        casByUser.put("user2", jcasB.getCas());

        var adapter = new SpanDiffAdapter(HOST_TYPE);
        adapter.addLinkFeature("links", "role", "target");
        var diffAdapters = asList(adapter);

        var diff = doDiff(diffAdapters, LINK_ROLE_AS_LABEL, casByUser);
        var result = diff.toResult();

        // result.print(System.out);

        assertEquals(2, result.size());
        assertEquals(1, result.getDifferingConfigurationSets().size());
        assertEquals(0, result.getIncompleteConfigurationSets().size());

        // Todo: Agreement has moved to separate project - should create agreement test there
        // CodingAgreementResult agreement = AgreementUtils.getCohenKappaAgreement(diff, HOST_TYPE,
        // "links", casByUser);
        //
        // // Asserts
        // System.out.printf("Agreement: %s%n", agreement.toString());
        // AgreementUtils.dumpAgreementStudy(System.out, agreement);
        //
        // assertEquals(0.0d, agreement.getAgreement(), 0.00001d);
    }

    @Test
    public void multiLinkWithRoleTargetDifferenceTest() throws Exception
    {
        var jcasA = createJCas(createMultiLinkWithRoleTestTypeSystem());
        makeLinkHostFS(jcasA, 0, 0, makeLinkFS(jcasA, "slot1", 0, 0));

        var jcasB = createJCas(createMultiLinkWithRoleTestTypeSystem());
        makeLinkHostFS(jcasB, 0, 0, makeLinkFS(jcasB, "slot1", 10, 10));

        var casByUser = new LinkedHashMap<String, CAS>();
        casByUser.put("user1", jcasA.getCas());
        casByUser.put("user2", jcasB.getCas());

        var adapter = new SpanDiffAdapter(HOST_TYPE);
        adapter.addLinkFeature("links", "role", "target");
        var diffAdapters = asList(adapter);

        var diff = doDiff(diffAdapters, LINK_TARGET_AS_LABEL, casByUser);
        var result = diff.toResult();

        // result.print(System.out);

        assertEquals(2, result.size());
        assertEquals(1, result.getDifferingConfigurationSets().size());
        assertEquals(0, result.getIncompleteConfigurationSets().size());

        // Todo: Agreement has moved to separate project - should create agreement test there
        // CodingAgreementResult agreement = getCohenKappaAgreement(diff, HOST_TYPE, "links",
        // casByUser);
        //
        // // Asserts
        // System.out.printf("Agreement: %s%n", agreement.toString());
        // AgreementUtils.dumpAgreementStudy(System.out, agreement);
        //
        // assertEquals(0.0, agreement.getAgreement(), 0.00001d);
    }

    @Test
    public void multiLinkWithRoleMultiTargetDifferenceTest() throws Exception
    {
        var jcasA = JCasFactory.createJCas(createMultiLinkWithRoleTestTypeSystem());
        makeLinkHostFS(jcasA, 0, 0, makeLinkFS(jcasA, "slot1", 0, 0),
                makeLinkFS(jcasA, "slot1", 10, 10));

        var jcasB = JCasFactory.createJCas(createMultiLinkWithRoleTestTypeSystem());
        makeLinkHostFS(jcasB, 0, 0, makeLinkFS(jcasB, "slot1", 10, 10));

        var casByUser = new LinkedHashMap<String, CAS>();
        casByUser.put("user1", jcasA.getCas());
        casByUser.put("user2", jcasB.getCas());

        var adapter = new SpanDiffAdapter(HOST_TYPE);
        adapter.addLinkFeature("links", "role", "target");
        var diffAdapters = asList(adapter);

        var diff = doDiff(diffAdapters, LINK_TARGET_AS_LABEL, casByUser).toResult();

        // diff.print(System.out);

        assertEquals(2, diff.size());
        assertEquals(1, diff.getDifferingConfigurationSets().size());
        assertEquals(0, diff.getIncompleteConfigurationSets().size());

        // // Check against new impl
        // AgreementResult agreement = AgreementUtils.getCohenKappaAgreement(diff, HOST_TYPE,
        // "links",
        // casByUser);
        //
        // // Asserts
        // System.out.printf("Agreement: %s%n", agreement.toString());
        // AgreementUtils.dumpAgreementStudy(System.out, agreement);
        //
        // assertEquals(0.0, agreement.getAgreement(), 0.00001d);
    }

    @Test
    public void multiLinkWithRoleMultiTargetDifferenceTest2() throws Exception
    {
        var jcasA = JCasFactory.createJCas(createMultiLinkWithRoleTestTypeSystem());
        makeLinkHostFS(jcasA, 0, 0, makeLinkFS(jcasA, "slot1", 0, 0),
                makeLinkFS(jcasA, "slot1", 10, 10));

        var jcasB = JCasFactory.createJCas(createMultiLinkWithRoleTestTypeSystem());
        makeLinkHostFS(jcasB, 0, 0, makeLinkFS(jcasB, "slot2", 10, 10));

        var casByUser = new LinkedHashMap<String, CAS>();
        casByUser.put("user1", jcasA.getCas());
        casByUser.put("user2", jcasB.getCas());

        var adapter = new SpanDiffAdapter(HOST_TYPE);
        adapter.addLinkFeature("links", "role", "target");
        var diffAdapters = asList(adapter);

        var diff = doDiff(diffAdapters, LINK_TARGET_AS_LABEL, casByUser).toResult();

        // diff.print(System.out);

        assertEquals(3, diff.size());
        assertEquals(1, diff.getDifferingConfigurationSets().size());
        assertEquals(2, diff.getIncompleteConfigurationSets().size());

        // // Check against new impl
        // AgreementResult agreement = AgreementUtils.getCohenKappaAgreement(diff, HOST_TYPE,
        // "links",
        // casByUser);
        //
        // // Asserts
        // System.out.printf("Agreement: %s%n", agreement.toString());
        // AgreementUtils.dumpAgreementStudy(System.out, agreement);
        //
        // assertEquals(0.0, agreement.getAgreement(), 0.00001d);
    }
}

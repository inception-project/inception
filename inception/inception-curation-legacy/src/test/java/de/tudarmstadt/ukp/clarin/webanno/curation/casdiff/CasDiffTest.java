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
import static de.tudarmstadt.ukp.clarin.webanno.curation.casdiff.CasDiffSummaryState.AGREE;
import static de.tudarmstadt.ukp.clarin.webanno.curation.casdiff.CasDiffSummaryState.DISAGREE;
import static de.tudarmstadt.ukp.clarin.webanno.curation.casdiff.CasDiffSummaryState.INCOMPLETE;
import static de.tudarmstadt.ukp.clarin.webanno.curation.casdiff.CasDiffSummaryState.STACKED;
import static de.tudarmstadt.ukp.clarin.webanno.curation.casdiff.CasDiffSummaryState.calculateState;
import static de.tudarmstadt.ukp.clarin.webanno.curation.casdiff.CurationTestUtils.HOST_TYPE;
import static de.tudarmstadt.ukp.clarin.webanno.curation.casdiff.CurationTestUtils.SLOT_FILLER_TYPE;
import static de.tudarmstadt.ukp.clarin.webanno.curation.casdiff.CurationTestUtils.createMultiLinkWithRoleTestTypeSystem;
import static de.tudarmstadt.ukp.clarin.webanno.curation.casdiff.CurationTestUtils.load;
import static de.tudarmstadt.ukp.clarin.webanno.curation.casdiff.CurationTestUtils.loadWebAnnoTsv3;
import static de.tudarmstadt.ukp.clarin.webanno.curation.casdiff.CurationTestUtils.makeLinkFS;
import static de.tudarmstadt.ukp.clarin.webanno.curation.casdiff.CurationTestUtils.makeLinkHostFS;
import static de.tudarmstadt.ukp.inception.annotation.feature.link.LinkFeatureDiffMode.EXCLUDE;
import static de.tudarmstadt.ukp.inception.annotation.feature.link.LinkFeatureDiffMode.INCLUDE;
import static de.tudarmstadt.ukp.inception.annotation.feature.link.LinkFeatureMultiplicityMode.MULTIPLE_TARGETS_MULTIPLE_ROLES;
import static de.tudarmstadt.ukp.inception.annotation.feature.link.LinkFeatureMultiplicityMode.MULTIPLE_TARGETS_ONE_ROLE;
import static de.tudarmstadt.ukp.inception.annotation.feature.link.LinkFeatureMultiplicityMode.ONE_TARGET_MULTIPLE_ROLES;
import static de.tudarmstadt.ukp.inception.annotation.layer.relation.api.RelationLayerSupport.FEAT_REL_SOURCE;
import static de.tudarmstadt.ukp.inception.annotation.layer.relation.api.RelationLayerSupport.FEAT_REL_TARGET;
import static de.tudarmstadt.ukp.inception.annotation.layer.relation.curation.RelationDiffAdapterImpl.DEPENDENCY_DIFF_ADAPTER;
import static de.tudarmstadt.ukp.inception.annotation.layer.span.curation.SpanDiffAdapterImpl.POS_DIFF_ADAPTER;
import static de.tudarmstadt.ukp.inception.support.uima.AnnotationBuilder.buildAnnotation;
import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static org.apache.uima.fit.factory.CasFactory.createText;
import static org.apache.uima.fit.factory.JCasFactory.createJCas;
import static org.apache.uima.fit.util.JCasUtil.select;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;
import static org.assertj.core.api.InstanceOfAssertFactories.collection;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.lang3.StringUtils;
import org.apache.uima.cas.CAS;
import org.apache.uima.fit.factory.TypeSystemDescriptionFactory;
import org.apache.uima.fit.testing.factory.TokenBuilder;
import org.apache.uima.fit.util.FSUtil;
import org.apache.uima.jcas.JCas;
import org.apache.uima.util.CasCreationUtils;
import org.assertj.core.api.AbstractListAssert;
import org.assertj.core.api.ObjectAssert;
import org.assertj.core.groups.Tuple;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import de.tudarmstadt.ukp.dkpro.core.api.lexmorph.type.pos.POS;
import de.tudarmstadt.ukp.dkpro.core.api.ner.type.NamedEntity;
import de.tudarmstadt.ukp.dkpro.core.api.segmentation.type.Lemma;
import de.tudarmstadt.ukp.dkpro.core.api.segmentation.type.Sentence;
import de.tudarmstadt.ukp.dkpro.core.api.segmentation.type.Token;
import de.tudarmstadt.ukp.dkpro.core.api.syntax.type.dependency.Dependency;
import de.tudarmstadt.ukp.inception.annotation.layer.relation.curation.RelationDiffAdapterImpl;
import de.tudarmstadt.ukp.inception.annotation.layer.span.api.SpanDiffAdapter;
import de.tudarmstadt.ukp.inception.annotation.layer.span.api.SpanPosition;
import de.tudarmstadt.ukp.inception.annotation.layer.span.curation.SpanDiffAdapterImpl;

public class CasDiffTest
{
    private static final SpanDiffAdapter POS_VALUE_ADAPTER;
    private static final SpanDiffAdapter TOKEN_ADAPTER;
    private static final SpanDiffAdapter LEMMA_ADAPTER;

    static {
        POS_VALUE_ADAPTER = new SpanDiffAdapterImpl(POS.class.getName(), "PosValue");
        TOKEN_ADAPTER = new SpanDiffAdapterImpl(Token.class.getName());
        LEMMA_ADAPTER = new SpanDiffAdapterImpl(Lemma.class.getName());
    }

    @Test
    public void noDataTest() throws Exception
    {
        var casByUser = new LinkedHashMap<String, CAS>();

        var result = doDiff(emptyList(), casByUser).toResult();

        // result.print(System.out);

        assertThat(result.size()).isEqualTo(0);
        assertThat(result.getDifferingConfigurationSets()).isEmpty();
        assertThat(result.getIncompleteConfigurationSets()).isEmpty();
        assertThat(calculateState(result)).isEqualTo(AGREE);
    }

    @Test
    public void singleEmptyCasTest() throws Exception
    {
        var casByUser = Map.of("user1", createText(""));

        var result = doDiff(asList(TOKEN_ADAPTER), casByUser).toResult();

        assertThat(result.size()).isEqualTo(0);
        assertThat(result.getDifferingConfigurationSets()).isEmpty();
        assertThat(result.getIncompleteConfigurationSets()).isEmpty();
        assertThat(calculateState(result)).isEqualTo(AGREE);
    }

    @Test
    public void multipleEmptyCasWithMissingOnesTest() throws Exception
    {
        var casByUser = new LinkedHashMap<String, CAS>();
        casByUser.put("user1", null);
        casByUser.put("user2", createText(""));

        var result = doDiff(asList(LEMMA_ADAPTER), casByUser).toResult();

        assertThat(result.size()).isEqualTo(0);
        assertThat(result.getDifferingConfigurationSets()).isEmpty();
        assertThat(result.getIncompleteConfigurationSets()).isEmpty();
        assertThat(calculateState(result)).isEqualTo(AGREE);
    }

    @Test
    public void noDifferencesPosTest() throws Exception
    {
        var casByUser = load( //
                "casdiff/noDifferences/data.conll", //
                "casdiff/noDifferences/data.conll");

        var result = doDiff(asList(POS_DIFF_ADAPTER), casByUser).toResult();

        assertThat(result.size()).isEqualTo(26);
        assertThat(result.getDifferingConfigurationSets()).isEmpty();
        assertThat(result.getIncompleteConfigurationSets()).isEmpty();
        assertThat(calculateState(result)).isEqualTo(AGREE);
    }

    @Test
    public void noDifferencesPosDependencyTest() throws Exception
    {
        var casByUser = load( //
                "casdiff/noDifferences/data.conll", //
                "casdiff/noDifferences/data.conll");

        var result = doDiff(asList(POS_DIFF_ADAPTER, DEPENDENCY_DIFF_ADAPTER), casByUser)
                .toResult();

        assertThat(result.size()).isEqualTo(52);
        assertThat(result.size(POS.class.getName())).isEqualTo(26);
        assertThat(result.size(Dependency.class.getName())).isEqualTo(26);
        assertThat(result.getDifferingConfigurationSets()).isEmpty();
        assertThat(result.getIncompleteConfigurationSets()).isEmpty();
        assertThat(calculateState(result)).isEqualTo(AGREE);
    }

    @Test
    public void singleDifferencesTest() throws Exception
    {
        var casByUser = load( //
                "casdiff/singleSpanDifference/user1.conll", //
                "casdiff/singleSpanDifference/user2.conll");

        var result = doDiff(asList(POS_DIFF_ADAPTER), casByUser).toResult();

        assertEquals(1, result.size());
        assertEquals(1, result.getDifferingConfigurationSets().size());
        assertEquals(0, result.getIncompleteConfigurationSets().size());

        assertThat(result.size()).isEqualTo(1);
        assertThat(result.getDifferingConfigurationSets()).hasSize(1);
        assertThat(result.getIncompleteConfigurationSets()).isEmpty();
        assertThat(calculateState(result)).isEqualTo(DISAGREE);
    }

    @Test
    public void someDifferencesTest() throws Exception
    {
        var casByUser = load( //
                "casdiff/someDifferences/user1.conll", //
                "casdiff/someDifferences/user2.conll");

        var diffAdapters = asList(POS_DIFF_ADAPTER);

        var result = doDiff(diffAdapters, casByUser).toResult();

        assertThat(result.size()).isEqualTo(26);
        assertThat(result.getDifferingConfigurationSets()).hasSize(4);
        assertThat(result.getIncompleteConfigurationSets()).isEmpty();
        assertThat(calculateState(result)).isEqualTo(DISAGREE);
    }

    @Test
    public void singleNoDifferencesTest() throws Exception
    {
        var cas = createJCas();
        var pos1 = new POS(cas, 0, 0);
        var pos2 = new POS(cas, 0, 0);
        asList(pos1, pos2).forEach(cas::addFsToIndexes);

        var casByUser = Map.of("user1", cas.getCas());

        var result = doDiff(asList(POS_VALUE_ADAPTER), casByUser).toResult();

        assertThat(result.size()).isEqualTo(1);
        assertThat(result.getDifferingConfigurationSets()).isEmpty();
        assertThat(result.getIncompleteConfigurationSets()).isEmpty();
        assertThat(calculateState(result)).isEqualTo(AGREE);
    }

    @Test
    public void singleNoDifferencesTestMoreData() throws Exception
    {
        var casByUser = load( //
                "casdiff/singleSpanNoDifference/data.conll", //
                "casdiff/singleSpanNoDifference/data.conll");

        var result = doDiff(asList(POS_VALUE_ADAPTER), casByUser).toResult();

        assertThat(result.size()).isEqualTo(1);
        assertThat(result.getDifferingConfigurationSets()).isEmpty();
        assertThat(result.getIncompleteConfigurationSets()).isEmpty();
        assertThat(calculateState(result)).isEqualTo(AGREE);
    }

    @Test
    public void spanLabelLabelTest() throws Exception
    {
        var casByUser = load( //
                "casdiff/spanLabel/user1.conll", //
                "casdiff/spanLabel/user2.conll");

        var result = doDiff(asList(POS_VALUE_ADAPTER), casByUser).toResult();

        assertThat(result.size()).isEqualTo(26);
        assertThat(result.getDifferingConfigurationSets()).hasSize(1);
        assertThat(result.getIncompleteConfigurationSets()).isEmpty();
        assertThat(calculateState(result)).isEqualTo(DISAGREE);
    }

    @Nested
    class RelationLayerTests
    {

        @Test
        public void noDifferencesDependencyTest() throws Exception
        {
            var casByUser = load( //
                    "casdiff/noDifferences/data.conll", //
                    "casdiff/noDifferences/data.conll");

            var result = doDiff(asList(DEPENDENCY_DIFF_ADAPTER), casByUser).toResult();

            assertThat(result.size()).isEqualTo(26);
            assertThat(result.getDifferingConfigurationSets()).isEmpty();
            assertThat(result.getIncompleteConfigurationSets()).isEmpty();
            assertThat(calculateState(result)).isEqualTo(AGREE);
        }

        @Test
        public void relationDistanceTest() throws Exception
        {
            var casByUser = load( //
                    "casdiff/relationDistance/user1.conll", //
                    "casdiff/relationDistance/user2.conll");

            var diffAdapters = asList(new RelationDiffAdapterImpl(Dependency.class.getName(),
                    "Dependent", "Governor", "DependencyType"));

            var result = doDiff(diffAdapters, casByUser).toResult();

            assertThat(result.size()).isEqualTo(27);
            assertThat(result.getDifferingConfigurationSets()).isEmpty();
            assertThat(result.getIncompleteConfigurationSets()).hasSize(2);
            assertThat(calculateState(result)).isEqualTo(INCOMPLETE);
        }

        @Test
        public void relationLabelTest() throws Exception
        {
            var casByUser = new HashMap<String, CAS>();
            casByUser.put("user1", loadWebAnnoTsv3("casdiff/relationLabelTest/user1.tsv").getCas());
            casByUser.put("user2", loadWebAnnoTsv3("casdiff/relationLabelTest/user2.tsv").getCas());

            var diffAdapters = asList(DEPENDENCY_DIFF_ADAPTER);

            var result = doDiff(diffAdapters, casByUser).toResult();

            assertThat(result.size()).isEqualTo(26);
            assertThat(result.getDifferingConfigurationSets()).hasSize(1);
            assertThat(result.getIncompleteConfigurationSets()).isEmpty();
            assertThat(calculateState(result)).isEqualTo(DISAGREE);
        }

        @Test
        public void relationStackedSpansTest() throws Exception
        {
            var global = TypeSystemDescriptionFactory.createTypeSystemDescription();
            var local = TypeSystemDescriptionFactory.createTypeSystemDescriptionFromPath(
                    "src/test/resources/desc/type/webannoTestTypes.xml");

            var merged = CasCreationUtils.mergeTypeSystems(asList(global, local));

            var tb = new TokenBuilder<>(Token.class, Sentence.class);

            var jcasA = createJCas(merged);
            {
                var casA = jcasA.getCas();
                tb.buildTokens(jcasA, "This is a test .");

                var tokensA = new ArrayList<>(select(jcasA, Token.class));
                var t1A = tokensA.get(0);
                var t2A = tokensA.get(tokensA.size() - 1);

                var govA = new NamedEntity(jcasA, t1A.getBegin(), t1A.getEnd());
                govA.addToIndexes();
                // Here we add a stacked named entity!
                new NamedEntity(jcasA, t1A.getBegin(), t1A.getEnd()).addToIndexes();

                var depA = new NamedEntity(jcasA, t2A.getBegin(), t2A.getEnd());
                depA.addToIndexes();

                var relationTypeA = casA.getTypeSystem().getType("webanno.custom.Relation");
                var fs1A = casA.createAnnotation(relationTypeA, depA.getBegin(), depA.getEnd());
                FSUtil.setFeature(fs1A, "Governor", govA);
                FSUtil.setFeature(fs1A, "Dependent", depA);
                FSUtil.setFeature(fs1A, "value", "REL");
                casA.addFsToIndexes(fs1A);
            }

            var jcasB = createJCas(merged);
            {
                var casB = jcasB.getCas();
                tb.buildTokens(jcasB, "This is a test .");

                var tokensB = new ArrayList<>(select(jcasB, Token.class));
                var t1B = tokensB.get(0);
                var t2B = tokensB.get(tokensB.size() - 1);

                var govB = new NamedEntity(jcasB, t1B.getBegin(), t1B.getEnd());
                govB.addToIndexes();
                var depB = new NamedEntity(jcasB, t2B.getBegin(), t2B.getEnd());
                depB.addToIndexes();

                var relationTypeB = casB.getTypeSystem().getType("webanno.custom.Relation");
                var fs1B = casB.createAnnotation(relationTypeB, depB.getBegin(), depB.getEnd());
                FSUtil.setFeature(fs1B, "Governor", govB);
                FSUtil.setFeature(fs1B, "Dependent", depB);
                FSUtil.setFeature(fs1B, "value", "REL");
                casB.addFsToIndexes(fs1B);
            }

            var casByUser = new LinkedHashMap<String, CAS>();
            casByUser.put("user1", jcasA.getCas());
            casByUser.put("user2", jcasB.getCas());

            var diffAdapters = asList(new RelationDiffAdapterImpl("webanno.custom.Relation",
                    FEAT_REL_TARGET, FEAT_REL_SOURCE, "value"));

            var result = doDiff(diffAdapters, casByUser).toResult();

            assertThat(result.size()).isEqualTo(1);
            assertThat(result.getDifferingConfigurationSets()).isEmpty();
            assertThat(result.getIncompleteConfigurationSets()).isEmpty();
            assertThat(calculateState(result)).isEqualTo(AGREE);
        }
    }

    @Nested
    class MultiValueFeatureTests
    {
        private static final String TYPE_SPAN_MULTI_VALUE = "webanno.custom.SpanMultiValue";
        private static final SpanDiffAdapter SPAN_MULTI_VALUE_ADAPTER;

        static {
            SPAN_MULTI_VALUE_ADAPTER = new SpanDiffAdapterImpl(TYPE_SPAN_MULTI_VALUE, "values");
        }

        @Test
        public void two_annotators__a_b__b_a__agreement() throws Exception
        {
            var cas1 = createText("");
            buildAnnotation(cas1, TYPE_SPAN_MULTI_VALUE) //
                    .withFeature("values", asList("a", "b")) //
                    .buildAndAddToIndexes();

            var cas2 = createText("");
            buildAnnotation(cas2, TYPE_SPAN_MULTI_VALUE) //
                    .withFeature("values", asList("b", "a")) //
                    .buildAndAddToIndexes();

            var casByUser = Map.of( //
                    "user1", cas1, //
                    "user2", cas2);

            var result = doDiff(asList(SPAN_MULTI_VALUE_ADAPTER), casByUser).toResult();

            assertThat(result.size()).isEqualTo(1);
            assertThat(result.getDifferingConfigurationSets()).isEmpty();
            assertThat(result.getIncompleteConfigurationSets()).isEmpty();
            assertThat(calculateState(result)).isEqualTo(AGREE);
        }

        @Test
        public void two_annotators__a_b__a__disagreement() throws Exception
        {
            var cas1 = createText("");
            buildAnnotation(cas1, TYPE_SPAN_MULTI_VALUE) //
                    .withFeature("values", asList("a", "b")) //
                    .buildAndAddToIndexes();

            var cas2 = createText("");
            buildAnnotation(cas2, TYPE_SPAN_MULTI_VALUE) //
                    .withFeature("values", asList("a")) //
                    .buildAndAddToIndexes();

            var casByUser = Map.of( //
                    "user1", cas1, //
                    "user2", cas2);

            var result = doDiff(asList(SPAN_MULTI_VALUE_ADAPTER), casByUser).toResult();

            assertThat(result.size()).isEqualTo(1);
            assertThat(result.getDifferingConfigurationSets()).hasSize(1);
            assertThat(result.getIncompleteConfigurationSets()).isEmpty();
            assertThat(calculateState(result)).isEqualTo(DISAGREE);
        }

        @Test
        public void two_annotators__a_b__none__disagreement() throws Exception
        {
            var cas1 = createText("");
            buildAnnotation(cas1, TYPE_SPAN_MULTI_VALUE) //
                    .withFeature("values", asList("a", "b")) //
                    .buildAndAddToIndexes();

            var cas2 = createText("");
            buildAnnotation(cas2, TYPE_SPAN_MULTI_VALUE) //
                    .buildAndAddToIndexes();

            var casByUser = Map.of( //
                    "user1", cas1, //
                    "user2", cas2);

            var result = doDiff(asList(SPAN_MULTI_VALUE_ADAPTER), casByUser).toResult();

            assertThat(result.size()).isEqualTo(1);
            assertThat(result.getDifferingConfigurationSets()).hasSize(1);
            assertThat(result.getIncompleteConfigurationSets()).isEmpty();
            assertThat(calculateState(result)).isEqualTo(DISAGREE);
        }

        @Test
        public void two_annotators__a_b__incomplete() throws Exception
        {
            var cas1 = createText("");
            buildAnnotation(cas1, TYPE_SPAN_MULTI_VALUE) //
                    .withFeature("values", asList("a", "b")) //
                    .buildAndAddToIndexes();

            var cas2 = createText("");

            var casByUser = Map.of( //
                    "user1", cas1, //
                    "user2", cas2);

            var result = doDiff(asList(SPAN_MULTI_VALUE_ADAPTER), casByUser).toResult();

            assertThat(result.size()).isEqualTo(1);
            assertThat(result.getDifferingConfigurationSets()).isEmpty();
            assertThat(result.getIncompleteConfigurationSets()).hasSize(1);
            assertThat(calculateState(result)).isEqualTo(INCOMPLETE);
        }
    }

    @Nested
    class MultipleTargetsMultipleRolesIncludeLinksTests
    {
        private static final SpanDiffAdapter HOST_TYPE_ADAPTER;
        private static final SpanDiffAdapter FILLER_ADAPTER;

        private JCas jcasA;
        private JCas jcasB;
        private Map<String, CAS> casByUser;

        static {
            HOST_TYPE_ADAPTER = new SpanDiffAdapterImpl(HOST_TYPE);
            HOST_TYPE_ADAPTER.addLinkFeature("links", "role", "target",
                    MULTIPLE_TARGETS_MULTIPLE_ROLES, INCLUDE);

            FILLER_ADAPTER = new SpanDiffAdapterImpl(SLOT_FILLER_TYPE, "value");
        }

        @BeforeEach
        void setup() throws Exception
        {
            jcasA = createJCas(createMultiLinkWithRoleTestTypeSystem());
            jcasB = createJCas(createMultiLinkWithRoleTestTypeSystem());

            casByUser = new LinkedHashMap<String, CAS>();
            casByUser.put("user1", jcasA.getCas());
            casByUser.put("user2", jcasB.getCas());
        }

        @Test
        public void one_annotator__stacked_no_label_hosts__different_labels__stacked()
            throws Exception
        {
            makeLinkHostFS(jcasA, 0, 0, //
                    makeLinkFS(jcasA, "slot1", 0, 0));

            makeLinkHostFS(jcasA, 0, 0, //
                    makeLinkFS(jcasA, "slot2", 0, 0));

            casByUser.remove("user2");

            var result = doDiff(asList(HOST_TYPE_ADAPTER, FILLER_ADAPTER), casByUser).toResult();

            assertSpanPositionConfigurations(result.getConfigurationSets()) //
                    .containsExactlyInAnyOrder( //
                            tuple("LinkHost", 0, 0, -1, -1, null, Set.of("user1")), //
                            tuple("SlotFiller", 0, 0, -1, -1, null, Set.of("user1")), //
                            tuple("LinkHost", 0, 0, 0, 0, "slot1", Set.of("user1")), //
                            tuple("LinkHost", 0, 0, 0, 0, "slot2", Set.of("user1")));
            assertSpanPositionConfigurations(result.getDifferingConfigurationSets().values()) //
                    .containsExactlyInAnyOrder( //
                            tuple("LinkHost", 0, 0, -1, -1, null, Set.of("user1")));
            assertThat(result.getIncompleteConfigurationSets()).isEmpty();
            assertThat(calculateState(result)).isEqualTo(STACKED);
        }
    }

    @Nested
    class MultipleTargetsMultipleRolesExcludeLinksTests
    {
        private static final SpanDiffAdapter HOST_TYPE_ADAPTER;
        private static final SpanDiffAdapter FILLER_ADAPTER;

        private JCas jcasA;
        private JCas jcasB;
        private Map<String, CAS> casByUser;

        static {
            HOST_TYPE_ADAPTER = new SpanDiffAdapterImpl(HOST_TYPE);
            HOST_TYPE_ADAPTER.addLinkFeature("links", "role", "target",
                    MULTIPLE_TARGETS_MULTIPLE_ROLES, EXCLUDE);

            FILLER_ADAPTER = new SpanDiffAdapterImpl(SLOT_FILLER_TYPE, "value");
        }

        @BeforeEach
        void setup() throws Exception
        {
            jcasA = createJCas(createMultiLinkWithRoleTestTypeSystem());
            jcasB = createJCas(createMultiLinkWithRoleTestTypeSystem());

            casByUser = new LinkedHashMap<String, CAS>();
            casByUser.put("user1", jcasA.getCas());
            casByUser.put("user2", jcasB.getCas());
        }

        @Test
        public void one_annotator__redundant__agreement() throws Exception
        {
            makeLinkHostFS(jcasA, 0, 0, //
                    makeLinkFS(jcasA, "slot1", 0, 0), //
                    makeLinkFS(jcasA, "slot1", 0, 0));

            casByUser.remove("user2");

            var result = doDiff(asList(HOST_TYPE_ADAPTER, FILLER_ADAPTER), casByUser).toResult();

            assertSpanPositionConfigurations(result.getConfigurationSets()) //
                    .containsExactlyInAnyOrder( //
                            tuple("LinkHost", 0, 0, -1, -1, null, Set.of("user1")), //
                            tuple("SlotFiller", 0, 0, -1, -1, null, Set.of("user1")), //
                            tuple("LinkHost", 0, 0, 0, 0, "slot1", Set.of("user1")));
            assertThat(result.getDifferingConfigurationSets()).isEmpty();
            assertThat(result.getIncompleteConfigurationSets()).isEmpty();
            assertThat(calculateState(result)).isEqualTo(AGREE);
        }

        @Test
        public void one_annotator__different_labels__agreement() throws Exception
        {
            makeLinkHostFS(jcasA, 0, 0, //
                    makeLinkFS(jcasA, "slot1", 0, 0), //
                    makeLinkFS(jcasA, "slot2", 0, 0));

            casByUser.remove("user2");

            var result = doDiff(asList(HOST_TYPE_ADAPTER, FILLER_ADAPTER), casByUser).toResult();

            assertSpanPositionConfigurations(result.getConfigurationSets()) //
                    .containsExactlyInAnyOrder( //
                            tuple("LinkHost", 0, 0, -1, -1, null, Set.of("user1")), //
                            tuple("SlotFiller", 0, 0, -1, -1, null, Set.of("user1")), //
                            tuple("LinkHost", 0, 0, 0, 0, "slot1", Set.of("user1")), //
                            tuple("LinkHost", 0, 0, 0, 0, "slot2", Set.of("user1")));
            assertThat(result.getDifferingConfigurationSets()).isEmpty();
            assertThat(result.getIncompleteConfigurationSets()).isEmpty();
            assertThat(calculateState(result)).isEqualTo(AGREE);
        }

        @Test
        public void one_annotator__stacked_no_label_hosts__different_labels__agreement()
            throws Exception
        {
            makeLinkHostFS(jcasA, 0, 0, //
                    makeLinkFS(jcasA, "slot1", 0, 0));

            makeLinkHostFS(jcasA, 0, 0, //
                    makeLinkFS(jcasA, "slot2", 0, 0));

            casByUser.remove("user2");

            var result = doDiff(asList(HOST_TYPE_ADAPTER, FILLER_ADAPTER), casByUser).toResult();

            assertSpanPositionConfigurations(result.getConfigurationSets()) //
                    .containsExactlyInAnyOrder( //
                            tuple("LinkHost", 0, 0, -1, -1, null, Set.of("user1")), //
                            tuple("SlotFiller", 0, 0, -1, -1, null, Set.of("user1")), //
                            tuple("LinkHost", 0, 0, 0, 0, "slot1", Set.of("user1")), //
                            tuple("LinkHost", 0, 0, 0, 0, "slot2", Set.of("user1")));
            assertThat(result.getDifferingConfigurationSets().values()).isEmpty();
            assertThat(result.getIncompleteConfigurationSets()).isEmpty();
            assertThat(calculateState(result)).isEqualTo(AGREE);
        }

        @Test
        public void one_annotator__different_positions__agreement() throws Exception
        {
            makeLinkHostFS(jcasA, 0, 0, //
                    makeLinkFS(jcasA, "slot1", 0, 0), //
                    makeLinkFS(jcasA, "slot1", 10, 10));

            casByUser.remove("user2");

            var result = doDiff(asList(HOST_TYPE_ADAPTER, FILLER_ADAPTER), casByUser).toResult();

            assertSpanPositionConfigurations(result.getConfigurationSets()) //
                    .containsExactlyInAnyOrder( //
                            tuple("LinkHost", 0, 0, -1, -1, null, Set.of("user1")), //
                            tuple("SlotFiller", 0, 0, -1, -1, null, Set.of("user1")), //
                            tuple("SlotFiller", 10, 10, -1, -1, null, Set.of("user1")), //
                            tuple("LinkHost", 0, 0, 0, 0, "slot1", Set.of("user1")), //
                            tuple("LinkHost", 0, 0, 10, 10, "slot1", Set.of("user1")));
            assertThat(result.getDifferingConfigurationSets()).isEmpty();
            assertThat(result.getIncompleteConfigurationSets()).isEmpty();
            assertThat(calculateState(result)).isEqualTo(AGREE);
        }

        /**
         * Both annotators have a link host that has two slots filled and the role labels and
         * targets match. The user wants to consider these as two agreeing links.
         */
        @Test
        public void two_annotators__multiple_slots__agreement()
        {
            makeLinkHostFS(jcasA, 0, 0, //
                    makeLinkFS(jcasA, "slot1", 0, 0), //
                    makeLinkFS(jcasA, "slot2", 0, 0));

            makeLinkHostFS(jcasB, 0, 0, //
                    makeLinkFS(jcasB, "slot1", 0, 0), //
                    makeLinkFS(jcasB, "slot2", 0, 0));

            var result = doDiff(asList(HOST_TYPE_ADAPTER), casByUser).toResult();

            assertSpanPositionConfigurations(result.getConfigurationSets()) //
                    .containsExactlyInAnyOrder( //
                            tuple("LinkHost", 0, 0, -1, -1, null, Set.of("user1", "user2")), //
                            tuple("LinkHost", 0, 0, 0, 0, "slot1", Set.of("user1", "user2")), //
                            tuple("LinkHost", 0, 0, 0, 0, "slot2", Set.of("user1", "user2")));
            assertThat(result.getDifferingConfigurationSets()).isEmpty();
            assertThat(result.getIncompleteConfigurationSets()).isEmpty();
            assertThat(calculateState(result)).isEqualTo(AGREE);
        }

        @Test
        public void two_annotators__multiple_annotations__agreement() throws Exception
        {
            makeLinkHostFS(jcasA, 0, 0, //
                    makeLinkFS(jcasA, "slot1", 0, 0));
            makeLinkHostFS(jcasA, 10, 10, //
                    makeLinkFS(jcasA, "slot1", 10, 10));

            makeLinkHostFS(jcasB, 0, 0, //
                    makeLinkFS(jcasB, "slot1", 0, 0));
            makeLinkHostFS(jcasB, 10, 10, //
                    makeLinkFS(jcasB, "slot1", 10, 10));

            var result = doDiff(asList(HOST_TYPE_ADAPTER), casByUser).toResult();

            assertSpanPositionConfigurations(result.getConfigurationSets()) //
                    .containsExactlyInAnyOrder( //
                            tuple("LinkHost", 0, 0, -1, -1, null, Set.of("user1", "user2")), //
                            tuple("LinkHost", 10, 10, -1, -1, null, Set.of("user1", "user2")), //
                            tuple("LinkHost", 0, 0, 0, 0, "slot1", Set.of("user1", "user2")), //
                            tuple("LinkHost", 10, 10, 10, 10, "slot1", Set.of("user1", "user2")));
            assertThat(result.getDifferingConfigurationSets()).isEmpty();
            assertThat(result.getIncompleteConfigurationSets()).isEmpty();
            assertThat(calculateState(result)).isEqualTo(AGREE);
        }

        @Test
        public void two_annotators__disagreement() throws Exception
        {
            makeLinkHostFS(jcasA, 0, 0, //
                    makeLinkFS(jcasA, "slot1", 0, 0));

            makeLinkHostFS(jcasB, 0, 0, //
                    makeLinkFS(jcasB, "slot1", 10, 10));

            var result = doDiff(asList(HOST_TYPE_ADAPTER), casByUser).toResult();

            assertSpanPositionConfigurations(result.getConfigurationSets()) //
                    .containsExactlyInAnyOrder( //
                            tuple("LinkHost", 0, 0, -1, -1, null, Set.of("user1", "user2")), //
                            tuple("LinkHost", 0, 0, 0, 0, "slot1", Set.of("user1")), //
                            tuple("LinkHost", 0, 0, 10, 10, "slot1", Set.of("user2")));
            assertThat(result.getDifferingConfigurationSets()).isEmpty();
            assertSpanPositionConfigurations(result.getIncompleteConfigurationSets().values()) //
                    .containsExactlyInAnyOrder( //
                            tuple("LinkHost", 0, 0, 0, 0, "slot1", Set.of("user1")), //
                            tuple("LinkHost", 0, 0, 10, 10, "slot1", Set.of("user2")));
            assertThat(calculateState(result)).isEqualTo(INCOMPLETE);
        }

        @Test
        public void two_annotators__same_labels__incomplete() throws Exception
        {
            makeLinkHostFS(jcasA, 0, 0, //
                    makeLinkFS(jcasA, "slot1", 0, 0), //
                    makeLinkFS(jcasA, "slot1", 10, 10));

            makeLinkHostFS(jcasB, 0, 0, //
                    makeLinkFS(jcasB, "slot1", 10, 10));

            var result = doDiff(asList(HOST_TYPE_ADAPTER), casByUser).toResult();

            assertSpanPositionConfigurations(result.getConfigurationSets()) //
                    .containsExactlyInAnyOrder( //
                            tuple("LinkHost", 0, 0, -1, -1, null, Set.of("user1", "user2")), //
                            tuple("LinkHost", 0, 0, 0, 0, "slot1", Set.of("user1")), //
                            tuple("LinkHost", 0, 0, 10, 10, "slot1", Set.of("user1", "user2")));
            assertThat(result.getDifferingConfigurationSets()).isEmpty();
            assertSpanPositionConfigurations(result.getIncompleteConfigurationSets().values())
                    .containsExactlyInAnyOrder( //
                            tuple("LinkHost", 0, 0, 0, 0, "slot1", Set.of("user1")));
            assertThat(calculateState(result)).isEqualTo(INCOMPLETE);
        }

        @Test
        public void two_annotators__different_targets_and_labels_1__incomplete() throws Exception
        {
            makeLinkHostFS(jcasA, 0, 0, //
                    makeLinkFS(jcasA, "slot1", 0, 0));

            makeLinkHostFS(jcasB, 0, 0, //
                    makeLinkFS(jcasB, "slot2", 10, 10));

            var result = doDiff(asList(HOST_TYPE_ADAPTER), casByUser).toResult();

            assertSpanPositionConfigurations(result.getConfigurationSets()) //
                    .containsExactlyInAnyOrder( //
                            tuple("LinkHost", 0, 0, -1, -1, null, Set.of("user1", "user2")), //
                            tuple("LinkHost", 0, 0, 0, 0, "slot1", Set.of("user1")), //
                            tuple("LinkHost", 0, 0, 10, 10, "slot2", Set.of("user2")));
            assertThat(result.getDifferingConfigurationSets()).isEmpty();
            assertSpanPositionConfigurations(result.getIncompleteConfigurationSets().values()) //
                    .containsExactlyInAnyOrder( //
                            tuple("LinkHost", 0, 0, 0, 0, "slot1", Set.of("user1")), //
                            tuple("LinkHost", 0, 0, 10, 10, "slot2", Set.of("user2")));
            assertThat(calculateState(result)).isEqualTo(INCOMPLETE);
        }

        @Test
        public void two_annotators__different_targets_and_labels_2__incomplete() throws Exception
        {
            makeLinkHostFS(jcasA, 0, 0, //
                    makeLinkFS(jcasA, "slot1", 0, 0), //
                    makeLinkFS(jcasA, "slot1", 10, 10));

            makeLinkHostFS(jcasB, 0, 0, //
                    makeLinkFS(jcasB, "slot2", 10, 10));

            var result = doDiff(asList(HOST_TYPE_ADAPTER), casByUser).toResult();

            assertSpanPositionConfigurations(result.getConfigurationSets()) //
                    .containsExactlyInAnyOrder( //
                            tuple("LinkHost", 0, 0, -1, -1, null, Set.of("user1", "user2")), //
                            tuple("LinkHost", 0, 0, 0, 0, "slot1", Set.of("user1")), //
                            tuple("LinkHost", 0, 0, 10, 10, "slot1", Set.of("user1")), //
                            tuple("LinkHost", 0, 0, 10, 10, "slot2", Set.of("user2")));
            assertThat(result.getDifferingConfigurationSets()).isEmpty();
            assertSpanPositionConfigurations(result.getIncompleteConfigurationSets().values()) //
                    .containsExactlyInAnyOrder( //
                            tuple("LinkHost", 0, 0, 0, 0, "slot1", Set.of("user1")), //
                            tuple("LinkHost", 0, 0, 10, 10, "slot1", Set.of("user1")), //
                            tuple("LinkHost", 0, 0, 10, 10, "slot2", Set.of("user2")));
            assertThat(calculateState(result)).isEqualTo(INCOMPLETE);
        }
    }

    @Nested
    class OneTargetMultipleRolesTests
    {
        private static final SpanDiffAdapter HOST_TYPE_ADAPTER;
        private static final SpanDiffAdapter FILLER_ADAPTER;

        private JCas jcasA;
        private JCas jcasB;
        private Map<String, CAS> casByUser;

        static {
            HOST_TYPE_ADAPTER = new SpanDiffAdapterImpl(HOST_TYPE);
            HOST_TYPE_ADAPTER.addLinkFeature("links", "role", "target", ONE_TARGET_MULTIPLE_ROLES,
                    EXCLUDE);

            FILLER_ADAPTER = new SpanDiffAdapterImpl(SLOT_FILLER_TYPE, "value");
        }

        @BeforeEach
        void setup() throws Exception
        {
            jcasA = createJCas(createMultiLinkWithRoleTestTypeSystem());
            jcasB = createJCas(createMultiLinkWithRoleTestTypeSystem());

            casByUser = new LinkedHashMap<String, CAS>();
            casByUser.put("user1", jcasA.getCas());
            casByUser.put("user2", jcasB.getCas());
        }

        @Test
        public void one_annotator__agreement() throws Exception
        {
            makeLinkHostFS(jcasA, 0, 0, //
                    makeLinkFS(jcasA, "slot1", 0, 0), //
                    makeLinkFS(jcasA, "slot2", 0, 0));

            casByUser.remove("user2");

            var result = doDiff(asList(HOST_TYPE_ADAPTER, FILLER_ADAPTER), casByUser).toResult();

            assertSpanPositionConfigurations(result.getConfigurationSets()) //
                    .containsExactlyInAnyOrder( //
                            tuple("LinkHost", 0, 0, -1, -1, null, Set.of("user1")), //
                            tuple("SlotFiller", 0, 0, -1, -1, null, Set.of("user1")), //
                            tuple("LinkHost", 0, 0, 0, 0, "slot1", Set.of("user1")), //
                            tuple("LinkHost", 0, 0, 0, 0, "slot2", Set.of("user1")));
            assertThat(result.getDifferingConfigurationSets()).isEmpty();
            assertThat(result.getIncompleteConfigurationSets()).isEmpty();
            assertThat(calculateState(result)).isEqualTo(AGREE);
        }

        @Test
        public void one_annotator__redundant__agreement() throws Exception
        {
            makeLinkHostFS(jcasA, 0, 0, //
                    makeLinkFS(jcasA, "slot1", 0, 0), //
                    makeLinkFS(jcasA, "slot1", 0, 0));

            casByUser.remove("user2");

            var result = doDiff(asList(HOST_TYPE_ADAPTER, FILLER_ADAPTER), casByUser).toResult();

            assertSpanPositionConfigurations(result.getConfigurationSets()) //
                    .containsExactlyInAnyOrder( //
                            tuple("LinkHost", 0, 0, -1, -1, null, Set.of("user1")), //
                            tuple("SlotFiller", 0, 0, -1, -1, null, Set.of("user1")), //
                            tuple("LinkHost", 0, 0, 0, 0, "slot1", Set.of("user1")));
            assertThat(result.getConfigurationSets()).hasSize(3);
            assertThat(result.getDifferingConfigurationSets()).isEmpty();
            assertThat(result.getIncompleteConfigurationSets()).isEmpty();
            assertThat(calculateState(result)).isEqualTo(AGREE);
        }

        /**
         * Both annotators have a link host that has two slots filled and the role labels and
         * targets match. The user wants to consider these as two agreeing links.
         */
        @Test
        public void two_annotators__multiple_slots__agreement()
        {
            makeLinkHostFS(jcasA, 0, 0, //
                    makeLinkFS(jcasA, "slot1", 0, 0), //
                    makeLinkFS(jcasA, "slot2", 0, 0));

            makeLinkHostFS(jcasB, 0, 0, //
                    makeLinkFS(jcasB, "slot1", 0, 0), //
                    makeLinkFS(jcasB, "slot2", 0, 0));

            var result = doDiff(asList(HOST_TYPE_ADAPTER), casByUser).toResult();

            assertSpanPositionConfigurations(result.getConfigurationSets()) //
                    .containsExactlyInAnyOrder( //
                            tuple("LinkHost", 0, 0, -1, -1, null, Set.of("user1", "user2")), //
                            tuple("LinkHost", 0, 0, 0, 0, "slot1", Set.of("user1", "user2")), //
                            tuple("LinkHost", 0, 0, 0, 0, "slot2", Set.of("user1", "user2")));
            assertThat(result.getDifferingConfigurationSets()).isEmpty();
            assertThat(result.getIncompleteConfigurationSets()).isEmpty();
            assertThat(calculateState(result)).isEqualTo(AGREE);
        }

        @Test
        public void two_annotators__multiple_annotations__agreement() throws Exception
        {
            makeLinkHostFS(jcasA, 0, 0, //
                    makeLinkFS(jcasA, "slot1", 0, 0));
            makeLinkHostFS(jcasA, 10, 10, //
                    makeLinkFS(jcasA, "slot1", 10, 10));

            makeLinkHostFS(jcasB, 0, 0, //
                    makeLinkFS(jcasB, "slot1", 0, 0));
            makeLinkHostFS(jcasB, 10, 10, //
                    makeLinkFS(jcasB, "slot1", 10, 10));

            var result = doDiff(asList(HOST_TYPE_ADAPTER), casByUser).toResult();

            assertSpanPositionConfigurations(result.getConfigurationSets()) //
                    .containsExactlyInAnyOrder( //
                            tuple("LinkHost", 0, 0, -1, -1, null, Set.of("user1", "user2")), //
                            tuple("LinkHost", 10, 10, -1, -1, null, Set.of("user1", "user2")), //
                            tuple("LinkHost", 0, 0, 0, 0, "slot1", Set.of("user1", "user2")), //
                            tuple("LinkHost", 10, 10, 10, 10, "slot1", Set.of("user1", "user2")));
            assertThat(result.getDifferingConfigurationSets()).isEmpty();
            assertThat(result.getIncompleteConfigurationSets()).isEmpty();
            assertThat(calculateState(result)).isEqualTo(AGREE);
        }

        @Test
        public void two_annotators__disagreement() throws Exception
        {
            makeLinkHostFS(jcasA, 0, 0, //
                    makeLinkFS(jcasA, "slot1", 0, 0));

            makeLinkHostFS(jcasB, 0, 0, //
                    makeLinkFS(jcasB, "slot1", 10, 10));

            var result = doDiff(asList(HOST_TYPE_ADAPTER), casByUser).toResult();

            assertSpanPositionConfigurations(result.getConfigurationSets()) //
                    .as("ConfigurationSets") //
                    .containsExactlyInAnyOrder( //
                            tuple("LinkHost", 0, 0, -1, -1, null, Set.of("user1", "user2")), //
                            tuple("LinkHost", 0, 0, 0, 0, "slot1", Set.of("user1", "user2")));
            assertSpanPositionConfigurations(result.getDifferingConfigurationSets().values()) //
                    .as("DifferingConfigurationSets") //
                    .containsExactlyInAnyOrder( //
                            tuple("LinkHost", 0, 0, 0, 0, "slot1", Set.of("user1", "user2")));
            assertThat(result.getIncompleteConfigurationSets()).isEmpty();
            assertThat(calculateState(result)).isEqualTo(DISAGREE);
        }

        @Test
        public void two_annotators__incomplete() throws Exception
        {
            makeLinkHostFS(jcasA, 0, 0, //
                    makeLinkFS(jcasA, "slot1", 0, 0));

            makeLinkHostFS(jcasB, 0, 0, //
                    makeLinkFS(jcasB, "slot2", 10, 10));

            var result = doDiff(asList(HOST_TYPE_ADAPTER), casByUser).toResult();

            assertSpanPositionConfigurations(result.getConfigurationSets()) //
                    .containsExactlyInAnyOrder( //
                            tuple("LinkHost", 0, 0, -1, -1, null, Set.of("user1", "user2")), //
                            tuple("LinkHost", 0, 0, 0, 0, "slot1", Set.of("user1")), //
                            tuple("LinkHost", 0, 0, 10, 10, "slot2", Set.of("user2")));
            assertThat(result.getDifferingConfigurationSets()).isEmpty();
            assertSpanPositionConfigurations(result.getIncompleteConfigurationSets().values()) //
                    .containsExactlyInAnyOrder( //
                            tuple("LinkHost", 0, 0, 0, 0, "slot1", Set.of("user1")), //
                            tuple("LinkHost", 0, 0, 10, 10, "slot2", Set.of("user2")));
            assertThat(calculateState(result)).isEqualTo(INCOMPLETE);
        }

        @Test
        public void two_annotators__same_labels__stacked() throws Exception
        {
            makeLinkHostFS(jcasA, 0, 0, //
                    makeLinkFS(jcasA, "slot1", 0, 0), //
                    makeLinkFS(jcasA, "slot1", 10, 10));

            makeLinkHostFS(jcasB, 0, 0, //
                    makeLinkFS(jcasB, "slot1", 10, 10));

            var result = doDiff(asList(HOST_TYPE_ADAPTER), casByUser).toResult();

            assertSpanPositionConfigurations(result.getConfigurationSets()) //
                    .containsExactlyInAnyOrder( //
                            tuple("LinkHost", 0, 0, -1, -1, null, Set.of("user1", "user2")), //
                            tuple("LinkHost", 0, 0, 0, 0, "slot1", Set.of("user1", "user2")));
            assertSpanPositionConfigurations(result.getDifferingConfigurationSets().values()) //
                    .containsExactlyInAnyOrder( //
                            tuple("LinkHost", 0, 0, 0, 0, "slot1", Set.of("user1", "user2")));
            assertThat(result.getIncompleteConfigurationSets()).isEmpty();
            assertThat(calculateState(result)).isEqualTo(STACKED);
        }

        @Test
        public void two_annotators__different_labels__stacked() throws Exception
        {
            makeLinkHostFS(jcasA, 0, 0, //
                    makeLinkFS(jcasA, "slot1", 0, 0), //
                    makeLinkFS(jcasA, "slot1", 10, 10));

            makeLinkHostFS(jcasB, 0, 0, //
                    makeLinkFS(jcasB, "slot2", 10, 10));

            var result = doDiff(asList(HOST_TYPE_ADAPTER), casByUser).toResult();

            assertSpanPositionConfigurations(result.getConfigurationSets()) //
                    .containsExactlyInAnyOrder( //
                            tuple("LinkHost", 0, 0, -1, -1, null, Set.of("user1", "user2")), //
                            tuple("LinkHost", 0, 0, 0, 0, "slot1", Set.of("user1")), //
                            tuple("LinkHost", 0, 0, 10, 10, "slot2", Set.of("user2")));
            assertSpanPositionConfigurations(result.getDifferingConfigurationSets().values()) //
                    .containsExactlyInAnyOrder( //
                            tuple("LinkHost", 0, 0, 0, 0, "slot1", Set.of("user1")));
            assertSpanPositionConfigurations(result.getIncompleteConfigurationSets().values()) //
                    .containsExactlyInAnyOrder( //
                            tuple("LinkHost", 0, 0, 0, 0, "slot1", Set.of("user1")), //
                            tuple("LinkHost", 0, 0, 10, 10, "slot2", Set.of("user2")));
            assertThat(calculateState(result)).isEqualTo(STACKED);
        }
    }

    @Nested
    class MultipleTargetsOneRoleTests
    {
        private static final SpanDiffAdapter HOST_TYPE_ADAPTER;
        private static final SpanDiffAdapter FILLER_ADAPTER;

        private JCas jcasA;
        private JCas jcasB;
        private Map<String, CAS> casByUser;

        static {
            HOST_TYPE_ADAPTER = new SpanDiffAdapterImpl(HOST_TYPE);
            HOST_TYPE_ADAPTER.addLinkFeature("links", "role", "target", MULTIPLE_TARGETS_ONE_ROLE,
                    EXCLUDE);

            FILLER_ADAPTER = new SpanDiffAdapterImpl(SLOT_FILLER_TYPE, "value");
        }

        @BeforeEach
        void setup() throws Exception
        {
            jcasA = createJCas(createMultiLinkWithRoleTestTypeSystem());
            jcasB = createJCas(createMultiLinkWithRoleTestTypeSystem());

            casByUser = new LinkedHashMap<String, CAS>();
            casByUser.put("user1", jcasA.getCas());
            casByUser.put("user2", jcasB.getCas());
        }

        @Test
        public void one_annotator__agreement() throws Exception
        {
            makeLinkHostFS(jcasA, 0, 0, //
                    makeLinkFS(jcasA, "slot1", 0, 0));

            casByUser.remove("user2");

            var result = doDiff(asList(HOST_TYPE_ADAPTER, FILLER_ADAPTER), casByUser).toResult();

            assertSpanPositionConfigurations(result.getConfigurationSets()) //
                    .containsExactlyInAnyOrder( //
                            tuple("LinkHost", 0, 0, -1, -1, null, Set.of("user1")), //
                            tuple("SlotFiller", 0, 0, -1, -1, null, Set.of("user1")), //
                            tuple("LinkHost", 0, 0, 0, 0, "slot1", Set.of("user1")));
            assertThat(result.getConfigurationSets()).hasSize(3);
            assertThat(result.getDifferingConfigurationSets()).isEmpty();
            assertThat(result.getIncompleteConfigurationSets()).isEmpty();
            assertThat(calculateState(result)).isEqualTo(AGREE);
        }

        @Test
        public void one_annotator__stacked() throws Exception
        {
            makeLinkHostFS(jcasA, 0, 0, //
                    makeLinkFS(jcasA, "slot1", 0, 0), //
                    makeLinkFS(jcasA, "slot2", 0, 0));

            casByUser.remove("user2");

            var result = doDiff(asList(HOST_TYPE_ADAPTER, FILLER_ADAPTER), casByUser).toResult();

            assertSpanPositionConfigurations(result.getConfigurationSets()) //
                    .containsExactlyInAnyOrder( //
                            tuple("LinkHost", 0, 0, -1, -1, null, Set.of("user1")), //
                            tuple("SlotFiller", 0, 0, -1, -1, null, Set.of("user1")), //
                            tuple("LinkHost", 0, 0, 0, 0, "slot1", Set.of("user1")));
            assertSpanPositionConfigurations(result.getDifferingConfigurationSets().values()) //
                    .containsExactlyInAnyOrder( //
                            tuple("LinkHost", 0, 0, 0, 0, "slot1", Set.of("user1")));
            assertThat(result.getIncompleteConfigurationSets()).isEmpty();
            assertThat(calculateState(result)).isEqualTo(STACKED);
        }

        @Test
        public void two_annotators__agreement() throws Exception
        {
            makeLinkHostFS(jcasA, 0, 0, //
                    makeLinkFS(jcasA, "slot1", 0, 0));

            makeLinkHostFS(jcasB, 0, 0, //
                    makeLinkFS(jcasB, "slot1", 0, 0));

            var result = doDiff(asList(HOST_TYPE_ADAPTER), casByUser).toResult();

            assertSpanPositionConfigurations(result.getConfigurationSets()) //
                    .containsExactlyInAnyOrder( //
                            tuple("LinkHost", 0, 0, -1, -1, null, Set.of("user1", "user2")), //
                            tuple("LinkHost", 0, 0, 0, 0, "slot1", Set.of("user1", "user2")));
            assertThat(result.getDifferingConfigurationSets()).isEmpty();
            assertThat(result.getIncompleteConfigurationSets()).isEmpty();
            assertThat(calculateState(result)).isEqualTo(AGREE);
        }

        @Test
        public void two_annotators__disagreement() throws Exception
        {
            makeLinkHostFS(jcasA, 0, 0, //
                    makeLinkFS(jcasA, "slot1", 0, 0));

            makeLinkHostFS(jcasB, 0, 0, //
                    makeLinkFS(jcasB, "slot2", 0, 0));

            var result = doDiff(asList(HOST_TYPE_ADAPTER), casByUser).toResult();

            assertSpanPositionConfigurations(result.getConfigurationSets()) //
                    .containsExactlyInAnyOrder( //
                            tuple("LinkHost", 0, 0, -1, -1, null, Set.of("user1", "user2")), //
                            tuple("LinkHost", 0, 0, 0, 0, "slot1", Set.of("user1", "user2")));
            assertSpanPositionConfigurations(result.getDifferingConfigurationSets().values()) //
                    .containsExactlyInAnyOrder( //
                            tuple("LinkHost", 0, 0, 0, 0, "slot1", Set.of("user1", "user2")));
            assertThat(result.getIncompleteConfigurationSets()).isEmpty();
            assertThat(calculateState(result)).isEqualTo(DISAGREE);
        }

        @Test
        public void two_annotators__incomplete() throws Exception
        {
            makeLinkHostFS(jcasA, 0, 0, //
                    makeLinkFS(jcasA, "slot1", 0, 0));

            var result = doDiff(asList(HOST_TYPE_ADAPTER), casByUser).toResult();

            assertSpanPositionConfigurations(result.getConfigurationSets()) //
                    .containsExactlyInAnyOrder( //
                            tuple("LinkHost", 0, 0, -1, -1, null, Set.of("user1")), //
                            tuple("LinkHost", 0, 0, 0, 0, "slot1", Set.of("user1")));
            assertThat(result.getDifferingConfigurationSets()).isEmpty();
            assertSpanPositionConfigurations(result.getIncompleteConfigurationSets().values()) //
                    .containsExactlyInAnyOrder( //
                            tuple("LinkHost", 0, 0, -1, -1, null, Set.of("user1")), //
                            tuple("LinkHost", 0, 0, 0, 0, "slot1", Set.of("user1")));
            assertThat(calculateState(result)).isEqualTo(INCOMPLETE);
        }

        @Test
        public void two_annotators__stacked() throws Exception
        {
            makeLinkHostFS(jcasA, 0, 0, //
                    makeLinkFS(jcasA, "slot1", 0, 0), //
                    makeLinkFS(jcasA, "slot2", 0, 0));

            makeLinkHostFS(jcasB, 0, 0, //
                    makeLinkFS(jcasB, "slot1", 0, 0), //
                    makeLinkFS(jcasB, "slot2", 0, 0));

            var result = doDiff(asList(HOST_TYPE_ADAPTER), casByUser).toResult();

            assertSpanPositionConfigurations(result.getConfigurationSets()) //
                    .containsExactlyInAnyOrder( //
                            tuple("LinkHost", 0, 0, -1, -1, null, Set.of("user1", "user2")), //
                            tuple("LinkHost", 0, 0, 0, 0, "slot1", Set.of("user1", "user2")));
            assertSpanPositionConfigurations(result.getDifferingConfigurationSets().values()) //
                    .containsExactlyInAnyOrder( //
                            tuple("LinkHost", 0, 0, 0, 0, "slot1", Set.of("user1", "user2")));
            assertThat(result.getIncompleteConfigurationSets()).isEmpty();
            assertThat(calculateState(result)).isEqualTo(STACKED);
        }
    }

    private static AbstractListAssert<?, List<? extends Tuple>, Tuple, ObjectAssert<Tuple>> //
            assertSpanPositionConfigurations(Collection<ConfigurationSet> aSet)
    {
        return assertThat(aSet) //
                .asInstanceOf(collection(ConfigurationSet.class)) //
                .extracting( //
                        cfg -> StringUtils.substringAfterLast(cfg.getPosition().getType(), "."), //
                        cfg -> ((SpanPosition) cfg.getPosition()).getBegin(), //
                        cfg -> ((SpanPosition) cfg.getPosition()).getEnd(), //
                        cfg -> cfg.getPosition().getLinkTargetBegin(), //
                        cfg -> cfg.getPosition().getLinkTargetEnd(), //
                        cfg -> cfg.getPosition().getLinkRole(), //
                        cfg -> cfg.getCasGroupIds());
    }
}

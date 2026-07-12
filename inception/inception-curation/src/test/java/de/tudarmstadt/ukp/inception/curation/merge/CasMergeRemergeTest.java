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
package de.tudarmstadt.ukp.inception.curation.merge;

import static de.tudarmstadt.ukp.clarin.webanno.curation.casdiff.CasDiff.doDiff;
import static de.tudarmstadt.ukp.clarin.webanno.curation.casdiff.CasDiffSummaryState.AGREE;
import static de.tudarmstadt.ukp.clarin.webanno.curation.casdiff.CasDiffSummaryState.INCOMPLETE;
import static de.tudarmstadt.ukp.clarin.webanno.curation.casdiff.CasDiffSummaryState.calculateState;
import static de.tudarmstadt.ukp.dkpro.core.api.lexmorph.type.pos.POS._FeatName_PosValue;
import static de.tudarmstadt.ukp.dkpro.core.api.segmentation.type.Token._FeatName_pos;
import static de.tudarmstadt.ukp.inception.annotation.feature.link.LinkFeatureDiffMode.EXCLUDE;
import static de.tudarmstadt.ukp.inception.annotation.feature.link.LinkFeatureMultiplicityMode.ONE_TARGET_MULTIPLE_ROLES;
import static de.tudarmstadt.ukp.inception.curation.merge.CurationTestUtils.HOST_TYPE;
import static de.tudarmstadt.ukp.inception.curation.merge.CurationTestUtils.createMultiLinkWithRoleTestTypeSystem;
import static de.tudarmstadt.ukp.inception.curation.merge.CurationTestUtils.makeLinkFS;
import static de.tudarmstadt.ukp.inception.curation.merge.CurationTestUtils.makeLinkHostFS;
import static de.tudarmstadt.ukp.inception.support.uima.AnnotationBuilder.buildAnnotation;
import static de.tudarmstadt.ukp.inception.support.uima.WebAnnoCasUtil.createCasCopy;
import static java.util.Arrays.asList;
import static org.apache.uima.fit.factory.JCasFactory.createJCas;
import static org.apache.uima.fit.factory.JCasFactory.createText;
import static org.apache.uima.fit.util.CasUtil.getType;
import static org.apache.uima.fit.util.CasUtil.select;
import static org.apache.uima.fit.util.JCasUtil.select;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;
import static org.junit.jupiter.api.parallel.ExecutionMode.CONCURRENT;

import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.apache.uima.cas.CAS;
import org.apache.uima.cas.Type;
import org.apache.uima.cas.text.AnnotationFS;
import org.apache.uima.fit.factory.CasFactory;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.parallel.Execution;

import de.tudarmstadt.ukp.dkpro.core.api.lexmorph.type.pos.POS;
import de.tudarmstadt.ukp.dkpro.core.api.ner.type.NamedEntity;
import de.tudarmstadt.ukp.dkpro.core.api.segmentation.type.Token;
import de.tudarmstadt.ukp.inception.annotation.layer.span.api.SpanPosition;
import de.tudarmstadt.ukp.inception.annotation.layer.span.curation.SpanDiffAdapterImpl;
import de.tudarmstadt.ukp.inception.curation.merge.strategy.MergeIncompleteStrategy;
import de.tudarmstadt.ukp.inception.schema.api.feature.FeatureSupport;

@Execution(CONCURRENT)
public class CasMergeRemergeTest
    extends CasMergeTestBase
{
    private static final String DUMMY_USER = "dummyTargetUser";

    /**
     * If one annotator has provided an annotation at a given position and the other annotator did
     * not (i.e. the annotations are incomplete), then this should be detected as a disagreement.
     */
    @SuppressWarnings("javadoc")
    @Test
    public void thatIncompleteAnnotationIsNotMerged() throws Exception
    {
        var user1 = CasFactory.createText("word");
        createTokenAndOptionalPos(user1, 0, 4, "X");

        var user2 = CasFactory.createText("word");
        createTokenAndOptionalPos(user2, 0, 4, null);

        var casByUser = new LinkedHashMap<String, CAS>();
        casByUser.put("user1", user1);
        casByUser.put("user2", user2);

        var curatorCas = createText(casByUser.values().stream() //
                .findFirst().get() //
                .getDocumentText());

        var result = doDiff(diffAdapters, casByUser).toResult();

        sut.clearAndMergeCas(result, document, DUMMY_USER, curatorCas.getCas(), casByUser);

        assertThat(result.getDifferingConfigurationSets()).isEmpty();
        assertThat(result.getIncompleteConfigurationSets().values())
                .extracting(set -> set.getPosition()) //
                .usingRecursiveFieldByFieldElementComparator() //
                .containsExactly( //
                        SpanPosition.builder() //
                                .withType(POS.class.getName()) //
                                .withBegin(0) //
                                .withEnd(4) //
                                .withText("word") //
                                .build());

        assertThat(select(curatorCas, POS.class)).isEmpty();
        assertThat(calculateState(result)).isEqualTo(INCOMPLETE);
    }

    /**
     * If one annotator has provided an annotation at a given position and the other annotator did
     * not (i.e. the annotations are incomplete), then this should be detected as a disagreement.
     */
    @SuppressWarnings("javadoc")
    @Test
    public void thatIncompleteAnnotationIsMerged() throws Exception
    {
        var user1 = CasFactory.createText("word");
        createTokenAndOptionalPos(user1, 0, 4, "X");

        var user2 = CasFactory.createText("word");
        createTokenAndOptionalPos(user2, 0, 4, null);

        var casByUser = new LinkedHashMap<String, CAS>();
        casByUser.put("user1", user1);
        casByUser.put("user2", user2);

        var curatorCas = createCasCopy(user1);

        var result = doDiff(diffAdapters, casByUser).toResult();

        sut.setMergeStrategy(new MergeIncompleteStrategy());
        sut.clearAndMergeCas(result, document, DUMMY_USER, curatorCas, casByUser);

        assertThat(result.getDifferingConfigurationSets()).isEmpty();
        assertThat(result.getIncompleteConfigurationSets().values())
                .extracting(set -> set.getPosition()) //
                .usingRecursiveFieldByFieldElementComparator()//
                .containsExactly( //
                        SpanPosition.builder() //
                                .withType(POS.class.getName()) //
                                .withBegin(0) //
                                .withEnd(4) //
                                .withText("word") //
                                .build());

        assertThat(curatorCas.select(POS.class).asList()).hasSize(1);
        assertThat(calculateState(result)).isEqualTo(INCOMPLETE);
    }

    @Test
    public void multiLinkWithRoleNoDifferenceTest() throws Exception
    {
        var jcasA = createJCas(createMultiLinkWithRoleTestTypeSystem("f1"));
        makeLinkHostFS(jcasA, 0, 0, makeLinkFS(jcasA, "slot1", 0, 0));
        makeLinkHostFS(jcasA, 10, 10, makeLinkFS(jcasA, "slot1", 10, 10));

        var jcasB = createJCas(createMultiLinkWithRoleTestTypeSystem("f1"));
        makeLinkHostFS(jcasB, 0, 0, makeLinkFS(jcasB, "slot1", 0, 0));
        makeLinkHostFS(jcasB, 10, 10, makeLinkFS(jcasB, "slot1", 10, 10));

        Map<String, CAS> casByUser = new LinkedHashMap<String, CAS>();
        casByUser.put("user1", jcasA.getCas());
        casByUser.put("user2", jcasB.getCas());

        var curatorCas = createJCas(createMultiLinkWithRoleTestTypeSystem("f1"));
        curatorCas.setDocumentText(casByUser.values().stream().findFirst().get().getDocumentText());

        var result = doDiff(diffAdapters, casByUser).toResult();

        // result.print(System.out);

        sut.clearAndMergeCas(result, document, DUMMY_USER, curatorCas.getCas(), casByUser);

        casByUser = new HashMap<String, CAS>();
        casByUser.put("actual", jcasA.getCas());
        casByUser.put("merge", curatorCas.getCas());

        result = doDiff(diffAdapters, casByUser).toResult();

        assertThat(result.getDifferingConfigurationSets()).isEmpty();
        assertThat(result.getIncompleteConfigurationSets()).isEmpty();
        assertThat(calculateState(result)).isEqualTo(AGREE);
    }

    @Test
    public void multiLinkWithRoleLabelDifferenceTest() throws Exception
    {
        var jcasA = createJCas(createMultiLinkWithRoleTestTypeSystem("f1"));
        makeLinkHostFS(jcasA, 0, 0, makeLinkFS(jcasA, "slot1", 0, 0));

        var jcasB = createJCas(createMultiLinkWithRoleTestTypeSystem("f1"));
        makeLinkHostFS(jcasB, 0, 0, makeLinkFS(jcasB, "slot2", 0, 0));

        var casByUser = new LinkedHashMap<String, CAS>();
        casByUser.put("user1", jcasA.getCas());
        casByUser.put("user2", jcasB.getCas());

        var curatorCas = createJCas(createMultiLinkWithRoleTestTypeSystem("f1"));
        curatorCas.setDocumentText(casByUser.values().stream().findFirst().get().getDocumentText());

        var result = doDiff(diffAdapters, casByUser).toResult();

        // result.print(System.out);

        sut.clearAndMergeCas(result, document, DUMMY_USER, curatorCas.getCas(), casByUser);

        var hostType = curatorCas.getCas().getTypeSystem().getType(HOST_TYPE);
        FeatureSupport<?> slotSupport = featureSupportRegistry.findExtension(slotFeature)
                .orElseThrow();

        assertThat(select(curatorCas.getCas(), hostType)).hasSize(1);

        assertThat(select(curatorCas.getCas(), hostType).stream()
                .map(host -> (List) slotSupport.getFeatureValue(slotFeature, host)))
                        .allMatch(Collection::isEmpty);
        assertThat(calculateState(result)).isEqualTo(INCOMPLETE);
    }

    @Test
    public void multiLinkWithRoleTargetDifferenceTest() throws Exception
    {
        var jcasA = createJCas(createMultiLinkWithRoleTestTypeSystem("f1"));
        makeLinkHostFS(jcasA, 0, 0, makeLinkFS(jcasA, "slot1", 0, 0));

        var jcasB = createJCas(createMultiLinkWithRoleTestTypeSystem("f1"));
        makeLinkHostFS(jcasB, 0, 0, makeLinkFS(jcasB, "slot1", 10, 10));

        var casByUser = new LinkedHashMap<String, CAS>();
        casByUser.put("user1", jcasA.getCas());
        casByUser.put("user2", jcasB.getCas());

        var curatorCas = createJCas(createMultiLinkWithRoleTestTypeSystem("f1"));
        curatorCas.setDocumentText(casByUser.values().stream().findFirst().get().getDocumentText());

        var result = doDiff(diffAdapters, casByUser).toResult();

        // result.print(System.out);

        sut.clearAndMergeCas(result, document, DUMMY_USER, curatorCas.getCas(), casByUser);

        var hostType = curatorCas.getCas().getTypeSystem().getType(HOST_TYPE);
        FeatureSupport<?> slotSupport = featureSupportRegistry.findExtension(slotFeature)
                .orElseThrow();

        assertThat(select(curatorCas.getCas(), hostType)).hasSize(1);

        assertThat(select(curatorCas.getCas(), hostType).stream()
                .map(host -> (List) slotSupport.getFeatureValue(slotFeature, host)))
                        .allMatch(Collection::isEmpty);
        assertThat(calculateState(result)).isEqualTo(INCOMPLETE);
    }

    @Test
    public void multiLinkMultiHostTest() throws Exception
    {
        // Creating two span stacked annotations. This should cause the data not to be merged.
        var jcasA = createJCas(createMultiLinkWithRoleTestTypeSystem("f1"));
        makeLinkHostFS(jcasA, 0, 0, makeLinkFS(jcasA, "slot1", 0, 0));
        makeLinkHostFS(jcasA, 0, 0, makeLinkFS(jcasA, "slot1", 0, 0));

        var jcasB = createJCas(createMultiLinkWithRoleTestTypeSystem("f1"));
        makeLinkHostFS(jcasB, 0, 0, makeLinkFS(jcasB, "slot1", 0, 0));

        var casByUser = new LinkedHashMap<String, CAS>();
        casByUser.put("user1", jcasA.getCas());
        casByUser.put("user2", jcasB.getCas());

        var curatorCas = createJCas(createMultiLinkWithRoleTestTypeSystem("f1")).getCas();
        curatorCas.setDocumentText(casByUser.values().stream().findFirst().get().getDocumentText());

        var adapter = new SpanDiffAdapterImpl(HOST_TYPE);
        adapter.addLinkFeature("links", "role", "target", ONE_TARGET_MULTIPLE_ROLES, EXCLUDE);

        var result = doDiff(asList(adapter), casByUser).toResult();

        // result.print(System.out);

        sut.clearAndMergeCas(result, document, DUMMY_USER, curatorCas, casByUser);

        assertThat(select(curatorCas, getType(curatorCas, HOST_TYPE))).isEmpty();
        assertThat(calculateState(result)).isEqualTo(AGREE);
    }

    @Test
    public void multiLinkMultiSpanRoleDiffTest() throws Exception
    {
        var jcasA = createJCas(createMultiLinkWithRoleTestTypeSystem("f1"));
        var type = jcasA.getTypeSystem().getType(HOST_TYPE);
        var feature = type.getFeatureByBaseName("f1");

        makeLinkHostFS(jcasA, 0, 0, feature, "A", makeLinkFS(jcasA, "slot1", 0, 0));

        var jcasB = createJCas(createMultiLinkWithRoleTestTypeSystem("f1"));
        makeLinkHostFS(jcasB, 0, 0, feature, "A", makeLinkFS(jcasB, "slot2", 0, 0));

        var casByUser = new LinkedHashMap<String, CAS>();
        casByUser.put("user1", jcasA.getCas());
        casByUser.put("user2", jcasB.getCas());

        var curatorCas = createJCas(createMultiLinkWithRoleTestTypeSystem("f1"));
        curatorCas.setDocumentText(casByUser.values().stream().findFirst().get().getDocumentText());

        var result = doDiff(diffAdapters, casByUser).toResult();

        // result.print(System.out);

        sut.clearAndMergeCas(result, document, DUMMY_USER, curatorCas.getCas(), casByUser);

        Type hostType = curatorCas.getTypeSystem().getType(HOST_TYPE);

        assertThat(select(curatorCas.getCas(), hostType)).hasSize(1);
        assertThat(calculateState(result)).isEqualTo(INCOMPLETE);
    }

    /**
     * When merging into a target CAS without clearing it first (preserve-existing mode), empty
     * positions are filled while positions already occupied by an existing annotation (e.g. a
     * curator decision) are left untouched instead of being overwritten with the merged value. See
     * issue #5996.
     */
    @SuppressWarnings("javadoc")
    @Test
    public void thatMergeWithoutClearingPreservesExistingAndFillsEmpty() throws Exception
    {
        // Two annotators agree on POS "Y" at [0,4] and POS "Z" at [5,9]
        var user1 = CasFactory.createText("word word");
        createTokenAndOptionalPos(user1, 0, 4, "Y");
        createTokenAndOptionalPos(user1, 5, 9, "Z");

        var user2 = CasFactory.createText("word word");
        createTokenAndOptionalPos(user2, 0, 4, "Y");
        createTokenAndOptionalPos(user2, 5, 9, "Z");

        var casByUser = new LinkedHashMap<String, CAS>();
        casByUser.put("user1", user1);
        casByUser.put("user2", user2);

        // The curator has already decided on POS "X" at [0,4] and left [5,9] empty
        var curatorCas = CasFactory.createText("word word");
        createTokenAndOptionalPos(curatorCas, 0, 4, "X");
        createTokenAndOptionalPos(curatorCas, 5, 9, null);

        var result = doDiff(diffAdapters, casByUser).toResult();

        // Merge WITHOUT clearing the target CAS, preserving annotations already present
        sut.setMergeStrategy(new MergeIncompleteStrategy());
        sut.setPreserveExisting(true);
        var mergeContext = sut.mergeCas(result, document, DUMMY_USER, curatorCas, casByUser);

        // The curator's decision at [0,4] is preserved (not overwritten to "Y") and the empty
        // position at [5,9] is filled with the agreed-upon "Z".
        assertThat(curatorCas.select(POS.class).asList()) //
                .extracting(pos -> pos.getBegin(), pos -> pos.getEnd(), POS::getPosValue) //
                .containsExactlyInAnyOrder( //
                        tuple(0, 4, "X"), //
                        tuple(5, 9, "Z"));

        // The preserved curator decision is counted as preserved - not as an ordinary skip
        // (notMerged) and not conflated with the "already identical" case.
        assertThat(mergeContext.preserved) //
                .as("The curator's preserved POS decision is counted separately") //
                .isEqualTo(1);
        assertThat(mergeContext.created) //
                .as("The agreed-upon POS at the empty position is created") //
                .isEqualTo(1);
    }

    /**
     * In preserve-existing mode, a curator annotation on a stacking-capable layer must not block
     * merging an agreed-upon annotation at a different, unoccupied position of the same layer -
     * only the occupied position itself is left untouched. See issue #5996.
     */
    @SuppressWarnings("javadoc")
    @Test
    public void thatMergeWithoutClearingStillFillsUnoccupiedPositionsOnStackingLayer()
        throws Exception
    {
        // Two annotators agree on a Named Entity "LOC" at [5,9]
        var user1 = CasFactory.createText("word word");
        createTokenAndOptionalNe(user1, 0, 4, null);
        createTokenAndOptionalNe(user1, 5, 9, "LOC");

        var user2 = CasFactory.createText("word word");
        createTokenAndOptionalNe(user2, 0, 4, null);
        createTokenAndOptionalNe(user2, 5, 9, "LOC");

        var casByUser = new LinkedHashMap<String, CAS>();
        casByUser.put("user1", user1);
        casByUser.put("user2", user2);

        // The curator has already decided on a Named Entity "PER" at [0,4]
        var curatorCas = CasFactory.createText("word word");
        createTokenAndOptionalNe(curatorCas, 0, 4, "PER");
        createTokenAndOptionalNe(curatorCas, 5, 9, null);

        var result = doDiff(diffAdapters, casByUser).toResult();

        // Merge WITHOUT clearing the target CAS, preserving annotations already present
        sut.setMergeStrategy(new MergeIncompleteStrategy());
        sut.setPreserveExisting(true);
        sut.mergeCas(result, document, DUMMY_USER, curatorCas, casByUser);

        // The curator's Named Entity at [0,4] is preserved and, although the layer already contains
        // an annotation, the agreed-upon Named Entity at the unoccupied [5,9] is still merged in.
        assertThat(curatorCas.select(NamedEntity.class).asList()) //
                .extracting(ne -> ne.getBegin(), ne -> ne.getEnd(), NamedEntity::getValue) //
                .containsExactlyInAnyOrder( //
                        tuple(0, 4, "PER"), //
                        tuple(5, 9, "LOC"));
    }

    private AnnotationFS createTokenAndOptionalNe(CAS aCas, int aBegin, int aEnd, String aValue)
    {
        if (aValue != null) {
            buildAnnotation(aCas, NamedEntity.class) //
                    .at(aBegin, aEnd) //
                    .withFeature(NamedEntity._FeatName_value, aValue) //
                    .buildAndAddToIndexes();
        }

        return buildAnnotation(aCas, Token.class) //
                .at(aBegin, aEnd) //
                .buildAndAddToIndexes();
    }

    private AnnotationFS createTokenAndOptionalPos(CAS aCas, int aBegin, int aEnd, String aPos)
    {
        AnnotationFS pos = null;

        if (aPos != null) {
            pos = buildAnnotation(aCas, POS.class) //
                    .at(aBegin, aEnd) //
                    .withFeature(_FeatName_PosValue, aPos) //
                    .buildAndAddToIndexes();
        }

        return buildAnnotation(aCas, Token.class) //
                .at(aBegin, aEnd) //
                .withFeature(_FeatName_pos, pos) //
                .buildAndAddToIndexes();
    }
}

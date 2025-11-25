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
import static org.junit.jupiter.api.Assertions.assertEquals;
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

        assertEquals(0, result.getDifferingConfigurationSets().size());
        assertEquals(0, result.getIncompleteConfigurationSets().size());
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

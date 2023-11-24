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
import static de.tudarmstadt.ukp.clarin.webanno.curation.casdiff.LinkCompareBehavior.LINK_TARGET_AS_LABEL;
import static de.tudarmstadt.ukp.dkpro.core.api.lexmorph.type.pos.POS._FeatName_PosValue;
import static de.tudarmstadt.ukp.dkpro.core.api.segmentation.type.Token._FeatName_pos;
import static de.tudarmstadt.ukp.inception.curation.merge.CurationTestUtils.HOST_TYPE;
import static de.tudarmstadt.ukp.inception.curation.merge.CurationTestUtils.createMultiLinkWithRoleTestTypeSystem;
import static de.tudarmstadt.ukp.inception.curation.merge.CurationTestUtils.makeLinkFS;
import static de.tudarmstadt.ukp.inception.curation.merge.CurationTestUtils.makeLinkHostFS;
import static de.tudarmstadt.ukp.inception.support.uima.AnnotationBuilder.buildAnnotation;
import static java.util.Arrays.asList;
import static org.apache.uima.fit.factory.JCasFactory.createJCas;
import static org.apache.uima.fit.factory.JCasFactory.createText;
import static org.apache.uima.fit.util.CasUtil.getType;
import static org.apache.uima.fit.util.CasUtil.select;
import static org.apache.uima.fit.util.JCasUtil.select;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.apache.uima.cas.CAS;
import org.apache.uima.cas.Feature;
import org.apache.uima.cas.Type;
import org.apache.uima.cas.text.AnnotationFS;
import org.apache.uima.fit.factory.CasFactory;
import org.apache.uima.jcas.JCas;
import org.junit.jupiter.api.Test;

import de.tudarmstadt.ukp.clarin.webanno.curation.casdiff.CasDiff.DiffResult;
import de.tudarmstadt.ukp.clarin.webanno.curation.casdiff.span.SpanDiffAdapter;
import de.tudarmstadt.ukp.clarin.webanno.curation.casdiff.span.SpanPosition;
import de.tudarmstadt.ukp.dkpro.core.api.lexmorph.type.pos.POS;
import de.tudarmstadt.ukp.dkpro.core.api.segmentation.type.Token;
import de.tudarmstadt.ukp.inception.curation.merge.strategy.MergeIncompleteStrategy;
import de.tudarmstadt.ukp.inception.schema.api.feature.FeatureSupport;

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
        CAS user1 = CasFactory.createText("word");
        createTokenAndOptionalPos(user1, 0, 4, "X");

        CAS user2 = CasFactory.createText("word");
        createTokenAndOptionalPos(user2, 0, 4, null);

        Map<String, List<CAS>> casByUser = new LinkedHashMap<>();
        casByUser.put("user1", asList(user1));
        casByUser.put("user2", asList(user2));

        JCas curatorCas = createText(casByUser.values().stream() //
                .flatMap(Collection::stream) //
                .findFirst().get() //
                .getDocumentText());

        DiffResult result = doDiff(diffAdapters, LINK_TARGET_AS_LABEL, casByUser).toResult();

        sut.reMergeCas(result, document, DUMMY_USER, curatorCas.getCas(),
                getSingleCasByUser(casByUser));

        assertThat(result.getDifferingConfigurationSets()).isEmpty();
        assertThat(result.getIncompleteConfigurationSets().values())
                .extracting(set -> set.getPosition()) //
                .usingRecursiveFieldByFieldElementComparator() //
                .containsExactly( //
                        new SpanPosition(null, null, 0, POS.class.getName(), 0, 4, "word", null,
                                null, -1, -1, null, null));

        assertThat(select(curatorCas, POS.class)).isEmpty();
    }

    /**
     * If one annotator has provided an annotation at a given position and the other annotator did
     * not (i.e. the annotations are incomplete), then this should be detected as a disagreement.
     */
    @SuppressWarnings("javadoc")
    @Test
    public void thatIncompleteAnnotationIsMerged() throws Exception
    {
        CAS user1 = CasFactory.createText("word");
        createTokenAndOptionalPos(user1, 0, 4, "X");

        CAS user2 = CasFactory.createText("word");
        createTokenAndOptionalPos(user2, 0, 4, null);

        Map<String, List<CAS>> casByUser = new LinkedHashMap<>();
        casByUser.put("user1", asList(user1));
        casByUser.put("user2", asList(user2));

        JCas curatorCas = createText(casByUser.values().stream().flatMap(Collection::stream)
                .findFirst().get().getDocumentText());

        DiffResult result = doDiff(diffAdapters, LINK_TARGET_AS_LABEL, casByUser).toResult();

        sut.setMergeStrategy(new MergeIncompleteStrategy());
        sut.reMergeCas(result, document, DUMMY_USER, curatorCas.getCas(),
                getSingleCasByUser(casByUser));

        assertThat(result.getDifferingConfigurationSets()).isEmpty();
        assertThat(result.getIncompleteConfigurationSets().values())
                .extracting(set -> set.getPosition()) //
                .usingRecursiveFieldByFieldElementComparator()//
                .containsExactly(//
                        new SpanPosition(null, null, 0, POS.class.getName(), 0, 4, "word", null,
                                null, -1, -1, null, null));

        assertThat(select(curatorCas, POS.class)).hasSize(1);
    }

    @Test
    public void multiLinkWithRoleNoDifferenceTest() throws Exception
    {
        JCas jcasA = createJCas(createMultiLinkWithRoleTestTypeSystem("f1"));
        makeLinkHostFS(jcasA, 0, 0, makeLinkFS(jcasA, "slot1", 0, 0));
        makeLinkHostFS(jcasA, 10, 10, makeLinkFS(jcasA, "slot1", 10, 10));

        JCas jcasB = createJCas(createMultiLinkWithRoleTestTypeSystem("f1"));
        makeLinkHostFS(jcasB, 0, 0, makeLinkFS(jcasB, "slot1", 0, 0));
        makeLinkHostFS(jcasB, 10, 10, makeLinkFS(jcasB, "slot1", 10, 10));

        Map<String, List<CAS>> casByUser = new LinkedHashMap<>();
        casByUser.put("user1", asList(jcasA.getCas()));
        casByUser.put("user2", asList(jcasB.getCas()));

        JCas curatorCas = createJCas(createMultiLinkWithRoleTestTypeSystem("f1"));
        curatorCas.setDocumentText(casByUser.values().stream().flatMap(Collection::stream)
                .findFirst().get().getDocumentText());

        DiffResult result = doDiff(diffAdapters, LINK_TARGET_AS_LABEL, casByUser).toResult();

        // result.print(System.out);

        sut.reMergeCas(result, document, DUMMY_USER, curatorCas.getCas(),
                getSingleCasByUser(casByUser));

        casByUser = new HashMap<>();
        casByUser.put("actual", asList(jcasA.getCas()));
        casByUser.put("merge", asList(curatorCas.getCas()));

        result = doDiff(diffAdapters, LINK_TARGET_AS_LABEL, casByUser).toResult();

        assertEquals(0, result.getDifferingConfigurationSets().size());
        assertEquals(0, result.getIncompleteConfigurationSets().size());
    }

    @Test
    public void multiLinkWithRoleLabelDifferenceTest() throws Exception
    {
        JCas jcasA = createJCas(createMultiLinkWithRoleTestTypeSystem("f1"));
        makeLinkHostFS(jcasA, 0, 0, makeLinkFS(jcasA, "slot1", 0, 0));

        JCas jcasB = createJCas(createMultiLinkWithRoleTestTypeSystem("f1"));
        makeLinkHostFS(jcasB, 0, 0, makeLinkFS(jcasB, "slot2", 0, 0));

        Map<String, List<CAS>> casByUser = new LinkedHashMap<>();
        casByUser.put("user1", asList(jcasA.getCas()));
        casByUser.put("user2", asList(jcasB.getCas()));

        JCas curatorCas = createJCas(createMultiLinkWithRoleTestTypeSystem("f1"));
        curatorCas.setDocumentText(casByUser.values().stream().flatMap(Collection::stream)
                .findFirst().get().getDocumentText());

        DiffResult result = doDiff(diffAdapters, LINK_TARGET_AS_LABEL, casByUser).toResult();

        // result.print(System.out);

        sut.reMergeCas(result, document, DUMMY_USER, curatorCas.getCas(),
                getSingleCasByUser(casByUser));

        Type hostType = curatorCas.getCas().getTypeSystem().getType(HOST_TYPE);
        FeatureSupport<?> slotSupport = featureSupportRegistry.findExtension(slotFeature)
                .orElseThrow();

        assertThat(select(curatorCas.getCas(), hostType)).hasSize(1);

        assertThat(select(curatorCas.getCas(), hostType).stream()
                .map(host -> (List) slotSupport.getFeatureValue(slotFeature, host)))
                        .allMatch(Collection::isEmpty);
    }

    @Test
    public void multiLinkWithRoleTargetDifferenceTest() throws Exception
    {
        JCas jcasA = createJCas(createMultiLinkWithRoleTestTypeSystem("f1"));
        makeLinkHostFS(jcasA, 0, 0, makeLinkFS(jcasA, "slot1", 0, 0));

        JCas jcasB = createJCas(createMultiLinkWithRoleTestTypeSystem("f1"));
        makeLinkHostFS(jcasB, 0, 0, makeLinkFS(jcasB, "slot1", 10, 10));

        Map<String, List<CAS>> casByUser = new LinkedHashMap<>();
        casByUser.put("user1", asList(jcasA.getCas()));
        casByUser.put("user2", asList(jcasB.getCas()));

        JCas curatorCas = createJCas(createMultiLinkWithRoleTestTypeSystem("f1"));
        curatorCas.setDocumentText(casByUser.values().stream().flatMap(Collection::stream)
                .findFirst().get().getDocumentText());

        DiffResult result = doDiff(diffAdapters, LINK_TARGET_AS_LABEL, casByUser).toResult();

        // result.print(System.out);

        sut.reMergeCas(result, document, DUMMY_USER, curatorCas.getCas(),
                getSingleCasByUser(casByUser));

        Type hostType = curatorCas.getCas().getTypeSystem().getType(HOST_TYPE);
        FeatureSupport<?> slotSupport = featureSupportRegistry.findExtension(slotFeature)
                .orElseThrow();

        assertThat(select(curatorCas.getCas(), hostType)).hasSize(1);

        assertThat(select(curatorCas.getCas(), hostType).stream()
                .map(host -> (List) slotSupport.getFeatureValue(slotFeature, host)))
                        .allMatch(Collection::isEmpty);
    }

    @Test
    public void multiLinkMultiHostTest() throws Exception
    {
        // Creating two span stacked annotations. This should cause the data not to be merged.
        JCas jcasA = createJCas(createMultiLinkWithRoleTestTypeSystem("f1"));
        makeLinkHostFS(jcasA, 0, 0, makeLinkFS(jcasA, "slot1", 0, 0));
        makeLinkHostFS(jcasA, 0, 0, makeLinkFS(jcasA, "slot1", 0, 0));

        JCas jcasB = createJCas(createMultiLinkWithRoleTestTypeSystem("f1"));
        makeLinkHostFS(jcasB, 0, 0, makeLinkFS(jcasB, "slot1", 0, 0));

        Map<String, List<CAS>> casByUser = new LinkedHashMap<>();
        casByUser.put("user1", asList(jcasA.getCas()));
        casByUser.put("user2", asList(jcasB.getCas()));

        CAS curatorCas = createJCas(createMultiLinkWithRoleTestTypeSystem("f1")).getCas();
        curatorCas.setDocumentText(casByUser.values().stream().flatMap(Collection::stream)
                .findFirst().get().getDocumentText());

        SpanDiffAdapter adapter = new SpanDiffAdapter(HOST_TYPE);
        adapter.addLinkFeature("links", "role", "target");

        DiffResult result = doDiff(asList(adapter), LINK_TARGET_AS_LABEL, casByUser).toResult();

        // result.print(System.out);

        sut.reMergeCas(result, document, DUMMY_USER, curatorCas, getSingleCasByUser(casByUser));

        assertThat(select(curatorCas, getType(curatorCas, HOST_TYPE))).isEmpty();
    }

    @Test
    public void multiLinkMultiSpanRoleDiffTest() throws Exception
    {
        JCas jcasA = createJCas(createMultiLinkWithRoleTestTypeSystem("f1"));
        Type type = jcasA.getTypeSystem().getType(HOST_TYPE);
        Feature feature = type.getFeatureByBaseName("f1");

        makeLinkHostFS(jcasA, 0, 0, feature, "A", makeLinkFS(jcasA, "slot1", 0, 0));

        JCas jcasB = createJCas(createMultiLinkWithRoleTestTypeSystem("f1"));
        makeLinkHostFS(jcasB, 0, 0, feature, "A", makeLinkFS(jcasB, "slot2", 0, 0));

        Map<String, List<CAS>> casByUser = new LinkedHashMap<>();
        casByUser.put("user1", asList(jcasA.getCas()));
        casByUser.put("user2", asList(jcasB.getCas()));

        JCas curatorCas = createJCas(createMultiLinkWithRoleTestTypeSystem("f1"));
        curatorCas.setDocumentText(casByUser.values().stream().flatMap(Collection::stream)
                .findFirst().get().getDocumentText());

        DiffResult result = doDiff(diffAdapters, LINK_TARGET_AS_LABEL, casByUser).toResult();

        // result.print(System.out);

        sut.reMergeCas(result, document, DUMMY_USER, curatorCas.getCas(),
                getSingleCasByUser(casByUser));

        Type hostType = curatorCas.getTypeSystem().getType(HOST_TYPE);

        assertThat(select(curatorCas.getCas(), hostType)).hasSize(1);
    }

    private Map<String, CAS> getSingleCasByUser(Map<String, List<CAS>> aCasByUserSingle)
    {
        Map<String, CAS> casByUserSingle = new HashMap<>();
        for (String user : aCasByUserSingle.keySet()) {
            casByUserSingle.put(user, aCasByUserSingle.get(user).get(0));
        }

        return casByUserSingle;
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

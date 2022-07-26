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
import static de.tudarmstadt.ukp.clarin.webanno.model.AnchoringMode.TOKENS;
import static de.tudarmstadt.ukp.clarin.webanno.model.Mode.CURATION;
import static de.tudarmstadt.ukp.clarin.webanno.support.WebAnnoConst.FEAT_REL_SOURCE;
import static de.tudarmstadt.ukp.clarin.webanno.support.WebAnnoConst.FEAT_REL_TARGET;
import static de.tudarmstadt.ukp.clarin.webanno.support.uima.AnnotationBuilder.buildAnnotation;
import static de.tudarmstadt.ukp.dkpro.core.api.lexmorph.type.pos.POS._FeatName_PosValue;
import static de.tudarmstadt.ukp.dkpro.core.api.segmentation.type.Token._FeatName_pos;
import static de.tudarmstadt.ukp.inception.curation.merge.CasMergeOperationResult.ResultState.CREATED;
import static de.tudarmstadt.ukp.inception.curation.merge.CurationTestUtils.HOST_TYPE;
import static de.tudarmstadt.ukp.inception.curation.merge.CurationTestUtils.createMultiLinkWithRoleTestTypeSystem;
import static de.tudarmstadt.ukp.inception.curation.merge.CurationTestUtils.makeLinkFS;
import static de.tudarmstadt.ukp.inception.curation.merge.CurationTestUtils.makeLinkHostFS;
import static de.tudarmstadt.ukp.inception.curation.merge.CurationTestUtils.makeLinkHostMultiSPanFeatureFS;
import static java.util.Arrays.asList;
import static org.apache.uima.fit.factory.CasFactory.createCas;
import static org.apache.uima.fit.factory.JCasFactory.createJCas;
import static org.apache.uima.fit.factory.JCasFactory.createText;
import static org.apache.uima.fit.util.CasUtil.getType;
import static org.apache.uima.fit.util.CasUtil.select;
import static org.apache.uima.fit.util.CasUtil.selectCovered;
import static org.apache.uima.fit.util.FSUtil.getFeature;
import static org.apache.uima.fit.util.JCasUtil.select;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.assertj.core.api.Assertions.tuple;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.apache.uima.cas.CAS;
import org.apache.uima.cas.Feature;
import org.apache.uima.cas.FeatureStructure;
import org.apache.uima.cas.Type;
import org.apache.uima.cas.text.AnnotationFS;
import org.apache.uima.fit.factory.CasFactory;
import org.apache.uima.fit.factory.JCasFactory;
import org.apache.uima.fit.util.CasUtil;
import org.apache.uima.jcas.JCas;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;

import de.tudarmstadt.ukp.clarin.webanno.curation.casdiff.CasDiff.DiffResult;
import de.tudarmstadt.ukp.clarin.webanno.curation.casdiff.span.SpanDiffAdapter;
import de.tudarmstadt.ukp.clarin.webanno.curation.casdiff.span.SpanPosition;
import de.tudarmstadt.ukp.clarin.webanno.model.OverlapMode;
import de.tudarmstadt.ukp.clarin.webanno.security.model.User;
import de.tudarmstadt.ukp.dkpro.core.api.lexmorph.type.pos.POS;
import de.tudarmstadt.ukp.dkpro.core.api.ner.type.NamedEntity;
import de.tudarmstadt.ukp.dkpro.core.api.segmentation.type.Token;
import de.tudarmstadt.ukp.dkpro.core.api.syntax.type.dependency.Dependency;
import de.tudarmstadt.ukp.inception.curation.merge.strategy.MergeIncompleteStrategy;
import de.tudarmstadt.ukp.inception.editor.state.AnnotatorStateImpl;
import de.tudarmstadt.ukp.inception.rendering.editorstate.AnnotatorState;
import de.tudarmstadt.ukp.inception.schema.adapter.AnnotationException;
import de.tudarmstadt.ukp.inception.schema.feature.FeatureSupport;
import de.tudarmstadt.ukp.inception.schema.feature.FeatureUtil;

public class CasMergeTest
    extends CasMergeTestBase
{
    private static final String DUMMY_USER = "dummyTargetUser";

    /**
     * If one annotator has provided an annotation at a given position and the other annotator did
     * not (i.e. the annotations are incomplete), then this should be detected as a disagreement.
     */
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
                .extracting(set -> set.getPosition()).usingFieldByFieldElementComparator()
                .containsExactly(new SpanPosition(null, null, 0, POS.class.getName(), 0, 4, "word",
                        null, null, -1, -1, null, null));

        assertThat(select(curatorCas, POS.class)).isEmpty();
    }

    /**
     * If one annotator has provided an annotation at a given position and the other annotator did
     * not (i.e. the annotations are incomplete), then this should be detected as a disagreement.
     */
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
                .extracting(set -> set.getPosition()).usingFieldByFieldElementComparator()
                .containsExactly(new SpanPosition(null, null, 0, POS.class.getName(), 0, 4, "word",
                        null, null, -1, -1, null, null));

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

        makeLinkHostMultiSPanFeatureFS(jcasA, 0, 0, feature, "A", makeLinkFS(jcasA, "slot1", 0, 0));

        JCas jcasB = createJCas(createMultiLinkWithRoleTestTypeSystem("f1"));
        makeLinkHostMultiSPanFeatureFS(jcasB, 0, 0, feature, "A", makeLinkFS(jcasB, "slot2", 0, 0));

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

    @Test
    public void simpleCopyToEmptyTest() throws Exception
    {
        AnnotatorState state = new AnnotatorStateImpl(CURATION);
        state.setUser(new User());

        CAS jcas = createJCas().getCas();
        AnnotationFS clickedFs = createNEAnno(jcas, "NN", 0, 0);

        CAS curatorCas = createJCas().getCas();
        createToken(curatorCas, 0, 0);

        sut.mergeSpanAnnotation(document, DUMMY_USER, neLayer, curatorCas, clickedFs, false);

        assertThat(selectCovered(curatorCas, getType(curatorCas, NamedEntity.class), 0, 0))
                .hasSize(1);
    }

    @Test
    public void simpleCopyToSameExistingAnnoTest() throws Exception
    {
        CAS jcas = createJCas().getCas();
        Type type = jcas.getTypeSystem().getType(POS.class.getTypeName());
        AnnotationFS clickedFs = createPOSAnno(jcas, "NN", 0, 0);

        CAS mergeCas = createJCas().getCas();
        AnnotationFS existingFs = mergeCas.createAnnotation(type, 0, 0);
        Feature posValue = type.getFeatureByBaseName("PosValue");
        existingFs.setStringValue(posValue, "NN");
        mergeCas.addFsToIndexes(existingFs);

        Assertions.assertThatExceptionOfType(AnnotationException.class).isThrownBy(() -> sut
                .mergeSpanAnnotation(document, DUMMY_USER, posLayer, mergeCas, clickedFs, false))
                .withMessageContaining("annotation already exists");
    }

    @Test
    public void simpleCopyToDiffExistingAnnoWithNoStackingTest() throws Exception
    {
        CAS jcas = createJCas().getCas();
        Type type = jcas.getTypeSystem().getType(POS.class.getTypeName());
        AnnotationFS clickedFs = createPOSAnno(jcas, "NN", 0, 0);

        CAS mergeCAs = createJCas().getCas();
        AnnotationFS existingFs = mergeCAs.createAnnotation(type, 0, 0);
        Feature posValue = type.getFeatureByBaseName("PosValue");
        existingFs.setStringValue(posValue, "NE");
        mergeCAs.addFsToIndexes(existingFs);

        sut.mergeSpanAnnotation(document, DUMMY_USER, posLayer, mergeCAs, clickedFs, false);

        assertEquals(1, CasUtil.selectCovered(mergeCAs, type, 0, 0).size());
    }

    @Test
    public void simpleCopyToDiffExistingAnnoWithStackingTest() throws Exception
    {
        neLayer.setOverlapMode(OverlapMode.ANY_OVERLAP);

        CAS jcas = createJCas().getCas();
        Type type = jcas.getTypeSystem().getType(NamedEntity.class.getTypeName());
        AnnotationFS clickedFs = createNEAnno(jcas, "NN", 0, 0);

        CAS mergeCAs = createJCas().getCas();
        createToken(mergeCAs, 0, 0);
        AnnotationFS existingFs = mergeCAs.createAnnotation(type, 0, 0);
        Feature posValue = type.getFeatureByBaseName("value");
        existingFs.setStringValue(posValue, "NE");
        mergeCAs.addFsToIndexes(existingFs);

        sut.mergeSpanAnnotation(document, DUMMY_USER, neLayer, mergeCAs, clickedFs, true);

        assertEquals(2, selectCovered(mergeCAs, type, 0, 0).size());
    }

    @Test
    public void copySpanWithSlotNoStackingTest() throws Exception
    {
        slotLayer.setOverlapMode(OverlapMode.NO_OVERLAP);

        JCas jcasA = createJCas(CurationTestUtils.createMultiLinkWithRoleTestTypeSystem("f1"));
        Type type = jcasA.getTypeSystem().getType(CurationTestUtils.HOST_TYPE);
        Feature feature = type.getFeatureByBaseName("f1");

        AnnotationFS clickedFs = CurationTestUtils.makeLinkHostMultiSPanFeatureFS(jcasA, 0, 0,
                feature, "A", CurationTestUtils.makeLinkFS(jcasA, "slot1", 0, 0));

        JCas mergeCAs = JCasFactory
                .createJCas(CurationTestUtils.createMultiLinkWithRoleTestTypeSystem("f1"));

        CurationTestUtils.makeLinkHostMultiSPanFeatureFS(mergeCAs, 0, 0, feature, "C",
                CurationTestUtils.makeLinkFS(mergeCAs, "slot1", 0, 0));

        sut.mergeSpanAnnotation(document, DUMMY_USER, slotLayer, mergeCAs.getCas(), clickedFs,
                false);

        assertEquals(1, selectCovered(mergeCAs.getCas(), type, 0, 0).size());
    }

    @Test
    public void copySpanWithSlotWithStackingTest() throws Exception
    {
        AnnotatorState state = new AnnotatorStateImpl(CURATION);
        state.setUser(new User());

        slotLayer.setAnchoringMode(TOKENS);
        slotLayer.setOverlapMode(OverlapMode.ANY_OVERLAP);

        JCas jcasA = createJCas(CurationTestUtils.createMultiLinkWithRoleTestTypeSystem("f1"));
        Type type = jcasA.getTypeSystem().getType(CurationTestUtils.HOST_TYPE);
        Feature feature = type.getFeatureByBaseName("f1");

        AnnotationFS clickedFs = CurationTestUtils.makeLinkHostMultiSPanFeatureFS(jcasA, 0, 0,
                feature, "A", CurationTestUtils.makeLinkFS(jcasA, "slot1", 0, 0));

        JCas mergeCAs = JCasFactory
                .createJCas(CurationTestUtils.createMultiLinkWithRoleTestTypeSystem("f1"));

        CurationTestUtils.makeLinkHostMultiSPanFeatureFS(mergeCAs, 0, 0, feature, "C",
                CurationTestUtils.makeLinkFS(mergeCAs, "slot1", 0, 0));

        sut.mergeSpanAnnotation(document, DUMMY_USER, slotLayer, mergeCAs.getCas(), clickedFs,
                true);

        assertEquals(2, selectCovered(mergeCAs.getCas(), type, 0, 0).size());
    }

    @Test
    public void copyLinkToEmptyTest() throws Exception
    {
        JCas mergeCas = createJCas(CurationTestUtils.createMultiLinkWithRoleTestTypeSystem("f1"));
        Type type = mergeCas.getTypeSystem().getType(CurationTestUtils.HOST_TYPE);
        Feature feature = type.getFeatureByBaseName("f1");

        AnnotationFS mergeFs = makeLinkHostMultiSPanFeatureFS(mergeCas, 0, 0, feature, "A");

        FeatureStructure copyFS = CurationTestUtils.makeLinkFS(mergeCas, "slot1", 0, 0);

        List<FeatureStructure> linkFs = new ArrayList<>();
        linkFs.add(copyFS);
        FeatureUtil.setLinkFeatureValue(mergeFs, type.getFeatureByBaseName("links"), linkFs);

        JCas jcasA = createJCas(CurationTestUtils.createMultiLinkWithRoleTestTypeSystem("f1"));
        makeLinkHostMultiSPanFeatureFS(jcasA, 0, 0, feature, "A", makeLinkFS(jcasA, "slot1", 0, 0));

        Map<String, List<CAS>> casByUser = new LinkedHashMap<>();
        casByUser.put("user1", asList(mergeCas.getCas()));
        casByUser.put("user2", asList(jcasA.getCas()));

        DiffResult diff = doDiff(diffAdapters, LINK_TARGET_AS_LABEL, casByUser).toResult();

        assertEquals(0, diff.getDifferingConfigurationSets().size());
        assertEquals(0, diff.getIncompleteConfigurationSets().size());
    }

    @Test
    public void copyLinkToExistingButDiffLinkTest() throws Exception
    {
        JCas mergeCas = JCasFactory.createJCas(createMultiLinkWithRoleTestTypeSystem("f1"));
        Type type = mergeCas.getTypeSystem().getType(CurationTestUtils.HOST_TYPE);
        Feature feature = type.getFeatureByBaseName("f1");

        AnnotationFS mergeFs = makeLinkHostMultiSPanFeatureFS(mergeCas, 0, 0, feature, "A",
                makeLinkFS(mergeCas, "slot1", 0, 0));

        FeatureStructure copyFS = makeLinkFS(mergeCas, "slot2", 0, 0);

        List<FeatureStructure> linkFs = new ArrayList<>();
        linkFs.add(copyFS);
        FeatureUtil.setLinkFeatureValue(mergeFs, type.getFeatureByBaseName("links"), linkFs);

        JCas jcasA = createJCas(CurationTestUtils.createMultiLinkWithRoleTestTypeSystem("f1"));
        makeLinkHostMultiSPanFeatureFS(jcasA, 0, 0, feature, "A", makeLinkFS(jcasA, "slot1", 0, 0));

        Map<String, List<CAS>> casByUser = new LinkedHashMap<>();
        casByUser.put("user1", asList(mergeCas.getCas()));
        casByUser.put("user2", asList(jcasA.getCas()));

        DiffResult diff = doDiff(diffAdapters, LINK_TARGET_AS_LABEL, casByUser).toResult();

        assertEquals(0, diff.getDifferingConfigurationSets().size());
        assertEquals(2, diff.getIncompleteConfigurationSets().size());
    }

    @Test
    public void simpleCopyRelationToEmptyAnnoTest() throws Exception
    {
        CAS annCas = createCas();
        AnnotationFS clickedFs = createDependencyWithTokenAndPos(annCas, 0, 0, "NN", 1, 1, "NN");

        CAS mergeCas = createCas();
        createTokenAndOptionalPos(mergeCas, 0, 0, "NN");
        createTokenAndOptionalPos(mergeCas, 1, 1, "NN");

        sut.mergeRelationAnnotation(document, DUMMY_USER, depLayer, mergeCas, clickedFs, false);

        assertThat(selectCovered(mergeCas, getType(mergeCas, Dependency.class), 0, 1))
                .as("Relation was merged") //
                .hasSize(1);
    }

    @Test
    public void simpleCopyRelationToStackedTargetsTest() throws Exception
    {
        // Create a dependency relation with endpoints in the annotator CAS
        CAS annCas = createCas();
        AnnotationFS clickedFs = createDependencyWithTokenAndPos(annCas, 0, 0, "NN", 1, 1, "NN");

        // Create stacked endpoint candidates in the merge CAS
        CAS mergeCas = createCas();
        createTokenAndOptionalPos(mergeCas, 0, 0, "NN");
        createTokenAndOptionalPos(mergeCas, 0, 0, "NN");
        createTokenAndOptionalPos(mergeCas, 1, 1, "NN");
        createTokenAndOptionalPos(mergeCas, 1, 1, "NN");

        assertThatExceptionOfType(AnnotationException.class) //
                .as("Cannot merge when there are multiple/stacked candidates") //
                .isThrownBy(() -> sut.mergeRelationAnnotation(document, DUMMY_USER, depLayer,
                        mergeCas, clickedFs, false))
                .withMessageContaining("Stacked sources exist");
    }

    @Test
    public void thatMergingRelationIsRejectedIfAlreadyExists() throws Exception
    {
        CAS annCas = createJCas().getCas();
        AnnotationFS clickedFs = createDependencyWithTokenAndPos(annCas, 0, 0, "NN", 1, 1, "NN");

        CAS mergeCas = createJCas().getCas();
        createDependencyWithTokenAndPos(mergeCas, 0, 0, "NN", 1, 1, "NN");

        assertThatExceptionOfType(AnnotationException.class) //
                .as("Reject merging relation which already exists")
                .isThrownBy(() -> sut.mergeRelationAnnotation(document, DUMMY_USER, depLayer,
                        mergeCas, clickedFs, false))
                .withMessageContaining("annotation already exists");
    }

    @Test
    public void thatSecondRelationCanBeMergedWithSameTarget() throws Exception
    {
        CAS annCas = createJCas().getCas();
        AnnotationFS clickedFs = createDependencyWithTokenAndPos(annCas, 2, 2, "NN", 0, 0, "NN");

        CAS mergeCas = createJCas().getCas();
        createDependencyWithTokenAndPos(mergeCas, 1, 1, "NN", 0, 0, "NN");
        createTokenAndOptionalPos(mergeCas, 2, 2, "NN");

        CasMergeOperationResult result = sut.mergeRelationAnnotation(document, DUMMY_USER, depLayer,
                mergeCas, clickedFs, false);

        assertThat(result.getState()).isEqualTo(CREATED);
        assertThat(mergeCas.select(Dependency.class).asList()) //
                .extracting( //
                        dep -> getFeature(dep, FEAT_REL_SOURCE, AnnotationFS.class).getBegin(), //
                        dep -> getFeature(dep, FEAT_REL_SOURCE, AnnotationFS.class).getEnd(), //
                        dep -> getFeature(dep, FEAT_REL_TARGET, AnnotationFS.class).getBegin(), //
                        dep -> getFeature(dep, FEAT_REL_TARGET, AnnotationFS.class).getEnd())
                .containsExactly( //
                        tuple(1, 1, 0, 0), //
                        tuple(2, 2, 0, 0));
    }

    @Test
    public void thatSecondRelationCanBeMergedWithSameSource() throws Exception
    {
        CAS annCas = createJCas().getCas();
        AnnotationFS clickedFs = createDependencyWithTokenAndPos(annCas, 0, 0, "NN", 2, 2, "NN");

        CAS mergeCas = createJCas().getCas();
        createDependencyWithTokenAndPos(mergeCas, 0, 0, "NN", 1, 1, "NN");
        createTokenAndOptionalPos(mergeCas, 2, 2, "NN");

        CasMergeOperationResult result = sut.mergeRelationAnnotation(document, DUMMY_USER, depLayer,
                mergeCas, clickedFs, false);

        assertThat(result.getState()).isEqualTo(CREATED);
        assertThat(mergeCas.select(Dependency.class).asList()) //
                .extracting( //
                        dep -> getFeature(dep, FEAT_REL_SOURCE, AnnotationFS.class).getBegin(), //
                        dep -> getFeature(dep, FEAT_REL_SOURCE, AnnotationFS.class).getEnd(), //
                        dep -> getFeature(dep, FEAT_REL_TARGET, AnnotationFS.class).getBegin(), //
                        dep -> getFeature(dep, FEAT_REL_TARGET, AnnotationFS.class).getEnd())
                .containsExactly( //
                        tuple(0, 0, 1, 1), //
                        tuple(0, 0, 2, 2));
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

    private AnnotationFS createToken(CAS aCas, int aBegin, int aEnd)
    {
        Type type = aCas.getTypeSystem().getType(Token.class.getTypeName());
        AnnotationFS token = aCas.createAnnotation(type, aBegin, aEnd);
        aCas.addFsToIndexes(token);
        return token;
    }

    private AnnotationFS createDependency(CAS aCas, AnnotationFS aSrcToken, AnnotationFS aTgtToken)
    {
        return buildAnnotation(aCas, Dependency.class) //
                .at(aTgtToken.getBegin(), aTgtToken.getEnd()) //
                .withFeature(FEAT_REL_SOURCE, aSrcToken) //
                .withFeature(FEAT_REL_TARGET, aTgtToken) //
                .buildAndAddToIndexes();
    }

    private AnnotationFS createDependencyWithTokenAndPos(CAS aCas, int aSrcBegin, int aSrcEnd,
            String aSrcPos, int aTgtBegin, int aTgtEnd, String aTgtPos)
    {
        return createDependency(aCas, //
                createTokenAndOptionalPos(aCas, aSrcBegin, aSrcEnd, aSrcPos), //
                createTokenAndOptionalPos(aCas, aTgtBegin, aTgtEnd, aTgtPos));
    }

    private AnnotationFS createNEAnno(CAS aCas, String aValue, int aBegin, int aEnd)
    {
        Type type = aCas.getTypeSystem().getType(NamedEntity.class.getTypeName());
        AnnotationFS clickedFs = aCas.createAnnotation(type, aBegin, aEnd);
        Feature value = type.getFeatureByBaseName("value");
        clickedFs.setStringValue(value, aValue);
        aCas.addFsToIndexes(clickedFs);
        return clickedFs;
    }

    private AnnotationFS createPOSAnno(CAS aCas, String aValue, int aBegin, int aEnd)
    {
        Type type = aCas.getTypeSystem().getType(POS.class.getTypeName());

        AnnotationFS clickedFs = aCas.createAnnotation(type, aBegin, aEnd);
        Feature posValue = type.getFeatureByBaseName("PosValue");
        clickedFs.setStringValue(posValue, aValue);
        aCas.addFsToIndexes(clickedFs);
        return clickedFs;
    }
}

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
package de.tudarmstadt.ukp.clarin.webanno.curation.casmerge;

import static de.tudarmstadt.ukp.clarin.webanno.api.WebAnnoConst.FEAT_REL_SOURCE;
import static de.tudarmstadt.ukp.clarin.webanno.api.WebAnnoConst.FEAT_REL_TARGET;
import static de.tudarmstadt.ukp.clarin.webanno.curation.CurationTestUtils.HOST_TYPE;
import static de.tudarmstadt.ukp.clarin.webanno.curation.CurationTestUtils.createMultiLinkWithRoleTestTypeSystem;
import static de.tudarmstadt.ukp.clarin.webanno.curation.CurationTestUtils.makeLinkFS;
import static de.tudarmstadt.ukp.clarin.webanno.curation.CurationTestUtils.makeLinkHostFS;
import static de.tudarmstadt.ukp.clarin.webanno.curation.CurationTestUtils.makeLinkHostMultiSPanFeatureFS;
import static de.tudarmstadt.ukp.clarin.webanno.curation.casdiff.CasDiff.LinkCompareBehavior.LINK_TARGET_AS_LABEL;
import static de.tudarmstadt.ukp.clarin.webanno.curation.casdiff.CasDiff.doDiff;
import static de.tudarmstadt.ukp.clarin.webanno.model.AnchoringMode.TOKENS;
import static de.tudarmstadt.ukp.clarin.webanno.model.Mode.CURATION;
import static java.util.Arrays.asList;
import static org.apache.uima.fit.factory.JCasFactory.createJCas;
import static org.apache.uima.fit.factory.JCasFactory.createText;
import static org.apache.uima.fit.util.CasUtil.getType;
import static org.apache.uima.fit.util.CasUtil.select;
import static org.apache.uima.fit.util.CasUtil.selectCovered;
import static org.apache.uima.fit.util.JCasUtil.select;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.junit.Assert.assertEquals;

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
import org.apache.uima.fit.factory.JCasFactory;
import org.apache.uima.fit.util.CasUtil;
import org.apache.uima.jcas.JCas;
import org.assertj.core.api.Assertions;
import org.dkpro.core.testing.DkproTestContext;
import org.junit.Rule;
import org.junit.Test;

import de.tudarmstadt.ukp.clarin.webanno.api.annotation.exception.AnnotationException;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.feature.FeatureSupport;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.model.AnnotatorState;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.model.AnnotatorStateImpl;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.util.WebAnnoCasUtil;
import de.tudarmstadt.ukp.clarin.webanno.curation.CurationTestUtils;
import de.tudarmstadt.ukp.clarin.webanno.curation.casdiff.CasDiff;
import de.tudarmstadt.ukp.clarin.webanno.curation.casdiff.CasDiff.DiffResult;
import de.tudarmstadt.ukp.clarin.webanno.curation.casdiff.CasDiff.SpanDiffAdapter;
import de.tudarmstadt.ukp.clarin.webanno.curation.casdiff.CasDiff.SpanPosition;
import de.tudarmstadt.ukp.clarin.webanno.model.OverlapMode;
import de.tudarmstadt.ukp.clarin.webanno.security.model.User;
import de.tudarmstadt.ukp.dkpro.core.api.lexmorph.type.pos.POS;
import de.tudarmstadt.ukp.dkpro.core.api.ner.type.NamedEntity;
import de.tudarmstadt.ukp.dkpro.core.api.segmentation.type.Token;
import de.tudarmstadt.ukp.dkpro.core.api.syntax.type.dependency.Dependency;

public class CasMergeTest
    extends CasMergeTestBase
{
    /**
     * If one annotator has provided an annotation at a given position and the other annotator did
     * not (i.e. the annotations are incomplete), then this should be detected as a disagreement. 
     */
    @Test
    public void thatIncompleteAnnotationIsNotMerged()
        throws Exception
    {
        JCas user1 = JCasFactory.createText("word");
        token(user1, 0, 4, "X");
        
        JCas user2 = JCasFactory.createText("word");
        token(user2, 0, 4, null);
        
        Map<String, List<CAS>> casByUser = new LinkedHashMap<>();
        casByUser.put("user1", asList(user1.getCas()));
        casByUser.put("user2", asList(user2.getCas()));
        
        JCas curatorCas = createText(casByUser.values().stream()
                .flatMap(Collection::stream).findFirst().get().getDocumentText());
        
        DiffResult result = doDiff(entryTypes, diffAdapters, LINK_TARGET_AS_LABEL, casByUser);

        sut.reMergeCas(result, document, null, curatorCas.getCas(), getSingleCasByUser(casByUser));

        assertThat(result.getDifferingConfigurationSets()).isEmpty();
        assertThat(result.getIncompleteConfigurationSets().values())
                .extracting(set -> set.getPosition())
                .usingFieldByFieldElementComparator()
                .containsExactly(new SpanPosition(null, null, 0, POS.class.getName(), 0, 4, "word",
                        null, null, -1, -1, null, null));
        
        assertThat(select(curatorCas, POS.class)).isEmpty();
    }
    
    /**
     * If one annotator has provided an annotation at a given position and the other annotator did
     * not (i.e. the annotations are incomplete), then this should be detected as a disagreement. 
     */
    @Test
    public void thatIncompleteAnnotationIsMerged()
        throws Exception
    {
        JCas user1 = JCasFactory.createText("word");
        token(user1, 0, 4, "X");
        
        JCas user2 = JCasFactory.createText("word");
        token(user2, 0, 4, null);
        
        Map<String, List<CAS>> casByUser = new LinkedHashMap<>();
        casByUser.put("user1", asList(user1.getCas()));
        casByUser.put("user2", asList(user2.getCas()));
        
        JCas curatorCas = createText(casByUser.values().stream()
                .flatMap(Collection::stream).findFirst().get().getDocumentText());
        
        DiffResult result = doDiff(entryTypes, diffAdapters, LINK_TARGET_AS_LABEL, casByUser);

        sut.setMergeIncompleteAnnotations(true);
        sut.reMergeCas(result, document, null, curatorCas.getCas(), getSingleCasByUser(casByUser));

        assertThat(result.getDifferingConfigurationSets()).isEmpty();
        assertThat(result.getIncompleteConfigurationSets().values())
                .extracting(set -> set.getPosition())
                .usingFieldByFieldElementComparator()
                .containsExactly(new SpanPosition(null, null, 0, POS.class.getName(), 0, 4, "word",
                        null, null, -1, -1, null, null));
        
        assertThat(select(curatorCas, POS.class)).hasSize(1);
    }

    private Token token(JCas aJCas, int aBegin, int aEnd, String aPos)
    {
        POS pos = null;
        if (aPos != null) {
            pos = new POS(aJCas, aBegin, aEnd);
            pos.setPosValue(aPos);
            pos.addToIndexes();
        }
        Token token = new Token(aJCas, aBegin, aEnd);
        token.setPos(pos);
        token.addToIndexes();
        return token;
    }
    
    @Test
    public void multiLinkWithRoleNoDifferenceTest()
        throws Exception
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

        DiffResult result = doDiff(entryTypes, diffAdapters, LINK_TARGET_AS_LABEL, casByUser);

        result.print(System.out);

        sut.reMergeCas(result, document, null, curatorCas.getCas(), getSingleCasByUser(casByUser));

        casByUser = new HashMap<>();
        casByUser.put("actual", asList(jcasA.getCas()));
        casByUser.put("merge", asList(curatorCas.getCas()));

        result = doDiff(entryTypes, diffAdapters, LINK_TARGET_AS_LABEL, casByUser);

        assertEquals(0, result.getDifferingConfigurationSets().size());
        assertEquals(0, result.getIncompleteConfigurationSets().size());
    }

    @Test
    public void multiLinkWithRoleLabelDifferenceTest()
        throws Exception
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

        DiffResult result = doDiff(entryTypes, diffAdapters, LINK_TARGET_AS_LABEL, casByUser);

        result.print(System.out);

        sut.reMergeCas(result, document, null, curatorCas.getCas(), getSingleCasByUser(casByUser));

        Type hostType = curatorCas.getCas().getTypeSystem().getType(HOST_TYPE);
        FeatureSupport slotSupport = featureSupportRegistry.getFeatureSupport(slotFeature);
        
        assertThat(select(curatorCas.getCas(), hostType))
                .hasSize(1);

        assertThat(select(curatorCas.getCas(), hostType).stream()
                .map(host -> (List) slotSupport.getFeatureValue(slotFeature, host)))
                .allMatch(Collection::isEmpty);
    }

    @Test
    public void multiLinkWithRoleTargetDifferenceTest()
        throws Exception
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

        DiffResult result = doDiff(entryTypes, diffAdapters, LINK_TARGET_AS_LABEL, casByUser);

        result.print(System.out);

        sut.reMergeCas(result, document, null, curatorCas.getCas(), getSingleCasByUser(casByUser));

        Type hostType = curatorCas.getCas().getTypeSystem().getType(HOST_TYPE);
        FeatureSupport slotSupport = featureSupportRegistry.getFeatureSupport(slotFeature);
        
        assertThat(select(curatorCas.getCas(), hostType))
                .hasSize(1);

        assertThat(select(curatorCas.getCas(), hostType).stream()
                .map(host -> (List) slotSupport.getFeatureValue(slotFeature, host)))
                .allMatch(Collection::isEmpty);
    }

    @Test
    public void multiLinkMultiHostTest()
        throws Exception
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
        
        DiffResult result = doDiff(entryTypes, asList(adapter), LINK_TARGET_AS_LABEL, casByUser);

        result.print(System.out);

        sut.reMergeCas(result, document, null, curatorCas, getSingleCasByUser(casByUser));

        assertThat(select(curatorCas, getType(curatorCas, HOST_TYPE)))
                .isEmpty();
    }

    @Test
    public void multiLinkMultiSpanRoleDiffTest()
        throws Exception
    {
        JCas jcasA = createJCas(createMultiLinkWithRoleTestTypeSystem("f1"));
        Type type = jcasA.getTypeSystem().getType(HOST_TYPE);
        Feature feature = type.getFeatureByBaseName("f1");

        makeLinkHostMultiSPanFeatureFS(jcasA, 0, 0, feature, "A",
                makeLinkFS(jcasA, "slot1", 0, 0));

        JCas jcasB = createJCas(createMultiLinkWithRoleTestTypeSystem("f1"));
        makeLinkHostMultiSPanFeatureFS(jcasB, 0, 0, feature, "A",
                makeLinkFS(jcasB, "slot2", 0, 0));

        Map<String, List<CAS>> casByUser = new LinkedHashMap<>();
        casByUser.put("user1", asList(jcasA.getCas()));
        casByUser.put("user2", asList(jcasB.getCas()));

        JCas curatorCas = createJCas(createMultiLinkWithRoleTestTypeSystem("f1"));
        curatorCas.setDocumentText(casByUser.values().stream().flatMap(Collection::stream)
                .findFirst().get().getDocumentText());

        DiffResult result = doDiff(entryTypes, diffAdapters, LINK_TARGET_AS_LABEL, casByUser);

        result.print(System.out);

        sut.reMergeCas(result, document, null, curatorCas.getCas(), getSingleCasByUser(casByUser));

        Type hostType = curatorCas.getTypeSystem().getType(HOST_TYPE);

        assertThat(select(curatorCas.getCas(), hostType))
                .hasSize(1);
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
    public void simpleCopyToEmptyTest()
        throws Exception
    {
        AnnotatorState state = new AnnotatorStateImpl(CURATION);
        state.setUser(new User());
        
        CAS jcas = createJCas().getCas();
        AnnotationFS clickedFs = createNEAnno(jcas, "NN", 0, 0);

        CAS curatorCas = createJCas().getCas();
        createTokenAnno(curatorCas, 0, 0);

        sut.mergeSpanAnnotation(null, null, neLayer, curatorCas, clickedFs, false);

        assertThat(selectCovered(curatorCas, getType(curatorCas, NamedEntity.class), 0, 0))
                .hasSize(1);
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

    private AnnotationFS createTokenAnno(CAS aCas, int aBegin, int aEnd)
    {
        Type type = aCas.getTypeSystem().getType(Token.class.getTypeName());
        AnnotationFS token = aCas.createAnnotation(type, aBegin, aEnd);
        aCas.addFsToIndexes(token);
        return token;
    }

    @Test
    public void simpleCopyToSameExistingAnnoTest()
        throws Exception
    {
        CAS jcas = createJCas().getCas();
        Type type = jcas.getTypeSystem().getType(POS.class.getTypeName());
        AnnotationFS clickedFs = createPOSAnno(jcas, "NN", 0, 0);

        CAS mergeCas = createJCas().getCas();
        AnnotationFS existingFs = mergeCas.createAnnotation(type, 0, 0);
        Feature posValue = type.getFeatureByBaseName("PosValue");
        existingFs.setStringValue(posValue, "NN");
        mergeCas.addFsToIndexes(existingFs);

        Assertions.assertThatExceptionOfType(AnnotationException.class)
                .isThrownBy(() -> sut.mergeSpanAnnotation(null, null, posLayer, mergeCas, 
                        clickedFs, false))
                .withMessageContaining("annotation already exists");
    }

    @Test
    public void simpleCopyToDiffExistingAnnoWithNoStackingTest()
        throws Exception
    {
        CAS jcas = createJCas().getCas();
        Type type = jcas.getTypeSystem().getType(POS.class.getTypeName());
        AnnotationFS clickedFs = createPOSAnno(jcas, "NN", 0, 0);

        CAS mergeCAs = createJCas().getCas();
        AnnotationFS existingFs = mergeCAs.createAnnotation(type, 0, 0);
        Feature posValue = type.getFeatureByBaseName("PosValue");
        existingFs.setStringValue(posValue, "NE");
        mergeCAs.addFsToIndexes(existingFs);

        sut.mergeSpanAnnotation(null, null, posLayer, mergeCAs, clickedFs, false);

        assertEquals(1, CasUtil.selectCovered(mergeCAs, type, 0, 0).size());
    }

    @Test
    public void simpleCopyToDiffExistingAnnoWithStackingTest()
        throws Exception
    {
        neLayer.setOverlapMode(OverlapMode.ANY_OVERLAP);

        CAS jcas = createJCas().getCas();
        Type type = jcas.getTypeSystem().getType(NamedEntity.class.getTypeName());
        AnnotationFS clickedFs = createNEAnno(jcas, "NN", 0, 0);

        CAS mergeCAs = createJCas().getCas();
        createTokenAnno(mergeCAs, 0, 0);
        AnnotationFS existingFs = mergeCAs.createAnnotation(type, 0, 0);
        Feature posValue = type.getFeatureByBaseName("value");
        existingFs.setStringValue(posValue, "NE");
        mergeCAs.addFsToIndexes(existingFs);

        sut.mergeSpanAnnotation(null, null, neLayer, mergeCAs, clickedFs, true);

        assertEquals(2, selectCovered(mergeCAs, type, 0, 0).size());
    }

    @Test
    public void copySpanWithSlotNoStackingTest()
        throws Exception
    {
        slotLayer.setOverlapMode(OverlapMode.NO_OVERLAP);
        
        JCas jcasA = createJCas(CurationTestUtils.createMultiLinkWithRoleTestTypeSystem("f1"));
        Type type = jcasA.getTypeSystem().getType(CurationTestUtils.HOST_TYPE);
        Feature feature = type.getFeatureByBaseName("f1");

        AnnotationFS clickedFs = CurationTestUtils.makeLinkHostMultiSPanFeatureFS(jcasA, 0, 0, feature, "A",
                CurationTestUtils.makeLinkFS(jcasA, "slot1", 0, 0));

        JCas mergeCAs = JCasFactory
                .createJCas(CurationTestUtils.createMultiLinkWithRoleTestTypeSystem("f1"));

        CurationTestUtils.makeLinkHostMultiSPanFeatureFS(mergeCAs, 0, 0, feature, "C",
                CurationTestUtils.makeLinkFS(mergeCAs, "slot1", 0, 0));

        sut.mergeSpanAnnotation(null, null, slotLayer, mergeCAs.getCas(), clickedFs, false);

        assertEquals(1, selectCovered(mergeCAs.getCas(), type, 0, 0).size());
    }

    @Test
    public void copySpanWithSlotWithStackingTest()
        throws Exception
    {
        AnnotatorState state = new AnnotatorStateImpl(CURATION);
        state.setUser(new User());
        
        slotLayer.setAnchoringMode(TOKENS);
        slotLayer.setOverlapMode(OverlapMode.ANY_OVERLAP);
        
        JCas jcasA = createJCas(CurationTestUtils.createMultiLinkWithRoleTestTypeSystem("f1"));
        Type type = jcasA.getTypeSystem().getType(CurationTestUtils.HOST_TYPE);
        Feature feature = type.getFeatureByBaseName("f1");

        AnnotationFS clickedFs = CurationTestUtils.makeLinkHostMultiSPanFeatureFS(jcasA, 0, 0, feature, "A",
                CurationTestUtils.makeLinkFS(jcasA, "slot1", 0, 0));

        JCas mergeCAs = JCasFactory
                .createJCas(CurationTestUtils.createMultiLinkWithRoleTestTypeSystem("f1"));

        CurationTestUtils.makeLinkHostMultiSPanFeatureFS(mergeCAs, 0, 0, feature, "C",
                CurationTestUtils.makeLinkFS(mergeCAs, "slot1", 0, 0));

        sut.mergeSpanAnnotation(null, null, slotLayer, mergeCAs.getCas(), clickedFs, true);

        assertEquals(2, selectCovered(mergeCAs.getCas(), type, 0, 0).size());
    }

    @Test
    public void copyLinkToEmptyTest()
        throws Exception
    {
        JCas mergeCas = createJCas(CurationTestUtils.createMultiLinkWithRoleTestTypeSystem("f1"));
        Type type = mergeCas.getTypeSystem().getType(CurationTestUtils.HOST_TYPE);
        Feature feature = type.getFeatureByBaseName("f1");

        AnnotationFS mergeFs = makeLinkHostMultiSPanFeatureFS(mergeCas, 0, 0, feature, "A");

        FeatureStructure copyFS = CurationTestUtils.makeLinkFS(mergeCas, "slot1", 0, 0);

        List<FeatureStructure> linkFs = new ArrayList<>();
        linkFs.add(copyFS);
        WebAnnoCasUtil.setLinkFeatureValue(mergeFs, type.getFeatureByBaseName("links"), linkFs);

        JCas jcasA = createJCas(CurationTestUtils.createMultiLinkWithRoleTestTypeSystem("f1"));
        makeLinkHostMultiSPanFeatureFS(jcasA, 0, 0, feature, "A",
                makeLinkFS(jcasA, "slot1", 0, 0));

        Map<String, List<CAS>> casByUser = new LinkedHashMap<>();
        casByUser.put("user1", asList(mergeCas.getCas()));
        casByUser.put("user2", asList(jcasA.getCas()));

        CasDiff.DiffResult diff = CasDiff.doDiff(entryTypes, diffAdapters,
                CasDiff.LinkCompareBehavior.LINK_TARGET_AS_LABEL, casByUser);

        assertEquals(0, diff.getDifferingConfigurationSets().size());
        assertEquals(0, diff.getIncompleteConfigurationSets().size());
    }

    @Test
    public void copyLinkToExistingButDiffLinkTest()
        throws Exception
    {

        JCas mergeCas = JCasFactory
                .createJCas(CurationTestUtils.createMultiLinkWithRoleTestTypeSystem("f1"));
        Type type = mergeCas.getTypeSystem().getType(CurationTestUtils.HOST_TYPE);
        Feature feature = type.getFeatureByBaseName("f1");

        AnnotationFS mergeFs = makeLinkHostMultiSPanFeatureFS(mergeCas, 0, 0, feature,
                "A", makeLinkFS(mergeCas, "slot1", 0, 0));

        FeatureStructure copyFS = makeLinkFS(mergeCas, "slot2", 0, 0);

        List<FeatureStructure> linkFs = new ArrayList<>();
        linkFs.add(copyFS);
        WebAnnoCasUtil.setLinkFeatureValue(mergeFs, type.getFeatureByBaseName("links"), linkFs);

        JCas jcasA = createJCas(CurationTestUtils.createMultiLinkWithRoleTestTypeSystem("f1"));
        makeLinkHostMultiSPanFeatureFS(jcasA, 0, 0, feature, "A",
                makeLinkFS(jcasA, "slot1", 0, 0));

        Map<String, List<CAS>> casByUser = new LinkedHashMap<>();
        casByUser.put("user1", asList(mergeCas.getCas()));
        casByUser.put("user2", asList(jcasA.getCas()));

        CasDiff.DiffResult diff = CasDiff.doDiff(entryTypes, diffAdapters,
                CasDiff.LinkCompareBehavior.LINK_TARGET_AS_LABEL, casByUser);

        assertEquals(0, diff.getDifferingConfigurationSets().size());
        assertEquals(2, diff.getIncompleteConfigurationSets().size());
    }

    @Test
    public void simpleCopyRelationToEmptyAnnoTest()
        throws Exception
    {
        CAS jcas = createJCas().getCas();
        Type type = jcas.getTypeSystem().getType(Dependency.class.getTypeName());

        AnnotationFS originClickedToken = createTokenAnno(jcas, 0, 0);
        AnnotationFS targetClickedToken = createTokenAnno(jcas, 1, 1);

        AnnotationFS originClicked = createPOSAnno(jcas, "NN", 0, 0);
        AnnotationFS targetClicked = createPOSAnno(jcas, "NN", 1, 1);

        jcas.addFsToIndexes(originClicked);
        jcas.addFsToIndexes(targetClicked);

        originClickedToken.setFeatureValue(originClickedToken.getType().getFeatureByBaseName("pos"),
                originClicked);
        targetClickedToken.setFeatureValue(targetClickedToken.getType().getFeatureByBaseName("pos"),
                targetClicked);

        Feature sourceFeature = type.getFeatureByBaseName(FEAT_REL_SOURCE);
        Feature targetFeature = type.getFeatureByBaseName(FEAT_REL_TARGET);

        AnnotationFS clickedFs = jcas.createAnnotation(type, 0, 1);
        clickedFs.setFeatureValue(sourceFeature, originClickedToken);
        clickedFs.setFeatureValue(targetFeature, targetClickedToken);
        jcas.addFsToIndexes(clickedFs);

        CAS mergeCAs = createJCas().getCas();
        AnnotationFS origin = createPOSAnno(mergeCAs, "NN", 0, 0);
        AnnotationFS target = createPOSAnno(mergeCAs, "NN", 1, 1);

        mergeCAs.addFsToIndexes(origin);
        mergeCAs.addFsToIndexes(target);

        AnnotationFS originToken = createTokenAnno(mergeCAs, 0, 0);
        AnnotationFS targetToken = createTokenAnno(mergeCAs, 1, 1);
        originToken.setFeatureValue(originToken.getType().getFeatureByBaseName("pos"), origin);
        targetToken.setFeatureValue(targetToken.getType().getFeatureByBaseName("pos"), target);

        mergeCAs.addFsToIndexes(originToken);
        mergeCAs.addFsToIndexes(targetToken);

        sut.mergeRelationAnnotation(null, null, depLayer, mergeCAs, clickedFs, false);
        
        assertEquals(1, selectCovered(mergeCAs, type, 0, 1).size());
    }

    @Test
    public void simpleCopyRelationToStackedTargetsTest()
        throws Exception
    {
        CAS jcas = createJCas().getCas();
        Type type = jcas.getTypeSystem().getType(Dependency.class.getTypeName());

        AnnotationFS originClickedToken = createTokenAnno(jcas, 0, 0);
        AnnotationFS targetClickedToken = createTokenAnno(jcas, 1, 1);

        AnnotationFS originClicked = createPOSAnno(jcas, "NN", 0, 0);
        AnnotationFS targetClicked = createPOSAnno(jcas, "NN", 1, 1);

        jcas.addFsToIndexes(originClicked);
        jcas.addFsToIndexes(targetClicked);

        originClickedToken.setFeatureValue(originClickedToken.getType().getFeatureByBaseName("pos"),
                originClicked);
        targetClickedToken.setFeatureValue(targetClickedToken.getType().getFeatureByBaseName("pos"),
                targetClicked);

        Feature sourceFeature = type.getFeatureByBaseName(FEAT_REL_SOURCE);
        Feature targetFeature = type.getFeatureByBaseName(FEAT_REL_TARGET);

        AnnotationFS clickedFs = jcas.createAnnotation(type, 0, 1);
        clickedFs.setFeatureValue(sourceFeature, originClickedToken);
        clickedFs.setFeatureValue(targetFeature, targetClickedToken);
        jcas.addFsToIndexes(clickedFs);

        CAS mergeCAs = createJCas().getCas();
        AnnotationFS origin = createPOSAnno(mergeCAs, "NN", 0, 0);
        AnnotationFS target = createPOSAnno(mergeCAs, "NN", 1, 1);

        mergeCAs.addFsToIndexes(origin);
        mergeCAs.addFsToIndexes(target);

        AnnotationFS originToken = createTokenAnno(mergeCAs, 0, 0);
        AnnotationFS targetToken = createTokenAnno(mergeCAs, 1, 1);
        originToken.setFeatureValue(originToken.getType().getFeatureByBaseName("pos"), origin);
        targetToken.setFeatureValue(targetToken.getType().getFeatureByBaseName("pos"), target);

        mergeCAs.addFsToIndexes(originToken);
        mergeCAs.addFsToIndexes(targetToken);

        AnnotationFS origin2 = createPOSAnno(mergeCAs, "NN", 0, 0);
        AnnotationFS target2 = createPOSAnno(mergeCAs, "NN", 1, 1);

        mergeCAs.addFsToIndexes(origin2);
        mergeCAs.addFsToIndexes(target2);

        AnnotationFS originToken2 = createTokenAnno(mergeCAs, 0, 0);
        AnnotationFS targetToken2 = createTokenAnno(mergeCAs, 1, 1);
        originToken2.setFeatureValue(originToken.getType().getFeatureByBaseName("pos"), origin2);
        targetToken2.setFeatureValue(targetToken.getType().getFeatureByBaseName("pos"), target2);

        mergeCAs.addFsToIndexes(originToken2);
        mergeCAs.addFsToIndexes(targetToken2);

        assertThatExceptionOfType(AnnotationException.class)
                .isThrownBy(() -> sut.mergeRelationAnnotation(null, null, depLayer, mergeCAs, 
                        clickedFs, false))
                .withMessageContaining("Stacked sources exist");
    }

    @Test
    public void thatMergingRelationIsRejectedIfAlreadyExists()
        throws Exception
    {
        CAS jcas = createJCas().getCas();
        Type type = jcas.getTypeSystem().getType(Dependency.class.getTypeName());

        AnnotationFS originClickedToken = createTokenAnno(jcas, 0, 0);
        AnnotationFS targetClickedToken = createTokenAnno(jcas, 1, 1);

        AnnotationFS originClicked = createPOSAnno(jcas, "NN", 0, 0);
        AnnotationFS targetClicked = createPOSAnno(jcas, "NN", 1, 1);

        jcas.addFsToIndexes(originClicked);
        jcas.addFsToIndexes(targetClicked);

        originClickedToken.setFeatureValue(originClickedToken.getType().getFeatureByBaseName("pos"),
                originClicked);
        targetClickedToken.setFeatureValue(targetClickedToken.getType().getFeatureByBaseName("pos"),
                targetClicked);

        Feature sourceFeature = type.getFeatureByBaseName(FEAT_REL_SOURCE);
        Feature targetFeature = type.getFeatureByBaseName(FEAT_REL_TARGET);

        AnnotationFS clickedFs = jcas.createAnnotation(type, 0, 1);
        clickedFs.setFeatureValue(sourceFeature, originClickedToken);
        clickedFs.setFeatureValue(targetFeature, targetClickedToken);
        jcas.addFsToIndexes(clickedFs);

        CAS mergeCAs = createJCas().getCas();
        AnnotationFS origin = createPOSAnno(mergeCAs, "NN", 0, 0);
        AnnotationFS target = createPOSAnno(mergeCAs, "NN", 1, 1);

        mergeCAs.addFsToIndexes(origin);
        mergeCAs.addFsToIndexes(target);

        AnnotationFS originToken = createTokenAnno(mergeCAs, 0, 0);
        AnnotationFS targetToken = createTokenAnno(mergeCAs, 1, 1);
        originToken.setFeatureValue(originToken.getType().getFeatureByBaseName("pos"), origin);
        targetToken.setFeatureValue(targetToken.getType().getFeatureByBaseName("pos"), target);

        mergeCAs.addFsToIndexes(originToken);
        mergeCAs.addFsToIndexes(targetToken);

        AnnotationFS existing = mergeCAs.createAnnotation(type, 0, 1);
        existing.setFeatureValue(sourceFeature, originToken);
        existing.setFeatureValue(targetFeature, targetToken);
        mergeCAs.addFsToIndexes(clickedFs);

        assertThatExceptionOfType(AnnotationException.class)
                .isThrownBy(() -> sut.mergeRelationAnnotation(null, null, depLayer, mergeCAs, 
                        clickedFs, false))
                .withMessageContaining("annotation already exists");
    }
    
//    private void writeTestSuiteData(Map<String, List<CAS>> casByUser, JCas curatorCas)
//        throws Exception
//    {
//        runPipeline(casByUser.get("user1").get(0),
//                createEngineDescription(WebannoTsv3XWriter.class,
//                        WebannoTsv3XWriter.PARAM_SINGULAR_TARGET, true,
//                        WebannoTsv3XWriter.PARAM_OVERWRITE, true,
//                        WebannoTsv3XWriter.PARAM_TARGET_LOCATION,
//                        "target/bux/" + testContext.getMethodName() + "/user1.tsv"));
//        runPipeline(casByUser.get("user2").get(0),
//                createEngineDescription(WebannoTsv3XWriter.class,
//                        WebannoTsv3XWriter.PARAM_SINGULAR_TARGET, true,
//                        WebannoTsv3XWriter.PARAM_OVERWRITE, true,
//                        WebannoTsv3XWriter.PARAM_TARGET_LOCATION,
//                        "target/bux/" + testContext.getMethodName() + "/user2.tsv"));
//        runPipeline(curatorCas,
//                createEngineDescription(WebannoTsv3XWriter.class,
//                        WebannoTsv3XWriter.PARAM_SINGULAR_TARGET, true,
//                        WebannoTsv3XWriter.PARAM_OVERWRITE, true,
//                        WebannoTsv3XWriter.PARAM_TARGET_LOCATION,
//                        "target/bux/" + testContext.getMethodName() + "/curator.tsv"));    
//    }
    
    @Rule
    public DkproTestContext testContext = new DkproTestContext();
}

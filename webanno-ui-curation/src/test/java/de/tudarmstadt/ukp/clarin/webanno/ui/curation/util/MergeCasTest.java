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
package de.tudarmstadt.ukp.clarin.webanno.ui.curation.util;

import static de.tudarmstadt.ukp.clarin.webanno.api.WebAnnoConst.CURATION_USER;
import static de.tudarmstadt.ukp.clarin.webanno.api.WebAnnoConst.RELATION_TYPE;
import static de.tudarmstadt.ukp.clarin.webanno.api.WebAnnoConst.SPAN_TYPE;
import static java.util.Arrays.asList;
import static org.junit.Assert.assertEquals;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.apache.uima.cas.ArrayFS;
import org.apache.uima.cas.CAS;
import org.apache.uima.cas.Feature;
import org.apache.uima.cas.FeatureStructure;
import org.apache.uima.cas.Type;
import org.apache.uima.fit.factory.JCasFactory;
import org.apache.uima.fit.util.CasUtil;
import org.apache.uima.jcas.JCas;
import org.apache.uima.resource.metadata.TypeSystemDescription;
import org.apache.uima.util.CasCreationUtils;
import org.junit.Rule;
import org.junit.Test;

import de.tudarmstadt.ukp.clarin.webanno.api.annotation.util.WebAnnoCasUtil;
import de.tudarmstadt.ukp.clarin.webanno.curation.casdiff.CasDiff2;
import de.tudarmstadt.ukp.clarin.webanno.curation.casdiff.CasDiff2.ArcDiffAdapter;
import de.tudarmstadt.ukp.clarin.webanno.curation.casdiff.CasDiff2.DiffAdapter;
import de.tudarmstadt.ukp.clarin.webanno.curation.casdiff.CasDiff2.DiffResult;
import de.tudarmstadt.ukp.clarin.webanno.curation.casdiff.CasDiff2.LinkCompareBehavior;
import de.tudarmstadt.ukp.clarin.webanno.curation.casdiff.CasDiff2.SpanDiffAdapter;
import de.tudarmstadt.ukp.dkpro.core.api.lexmorph.type.pos.POS;
import de.tudarmstadt.ukp.dkpro.core.api.ner.type.NamedEntity;
import de.tudarmstadt.ukp.dkpro.core.api.syntax.type.dependency.Dependency;
import de.tudarmstadt.ukp.dkpro.core.testing.DkproTestContext;

public class MergeCasTest
{

    @Test
    public void simpleSpanNoDiffNoLabelTest()
        throws Exception
    {
        Map<String, List<CAS>> casByUser = DiffUtils.loadWebAnnoTSV(null,
                "mergecas/simplespan/1sentence.tsv", "mergecas/simplespan/1sentence.tsv");

        List<String> entryTypes = asList(POS.class.getName());

        List<SpanDiffAdapter> diffAdapters = asList(SpanDiffAdapter.POS);

        addRandomMergeCas(casByUser);

        DiffResult result = CasDiff2.doDiff(entryTypes, diffAdapters,
                LinkCompareBehavior.LINK_TARGET_AS_LABEL, casByUser);

        CAS mergeCas = MergeCas.reMergeCas(result, getSingleCasByUser(casByUser));

        casByUser = new HashMap<>();
        CAS actual = DiffUtils.readWebAnnoTSV("mergecas/simplespan/1sentence.tsv", null);
        casByUser.put("actual", asList(actual));
        casByUser.put("merge", asList(mergeCas));

        result = CasDiff2.doDiff(entryTypes, diffAdapters, LinkCompareBehavior.LINK_TARGET_AS_LABEL,
                casByUser);

        assertEquals(0, result.getDifferingConfigurationSets().size());
        assertEquals(0, result.getIncompleteConfigurationSets().size());

    }

    @Test
    public void simpleSpanDiffNoLabelTest()
        throws Exception
    {
        Map<String, List<CAS>> casByUser = DiffUtils.loadWebAnnoTSV(null,
                "mergecas/simplespan/1sentence.tsv", "mergecas/simplespan/1sentenceempty.tsv");

        List<String> entryTypes = asList(POS.class.getName());

        List<SpanDiffAdapter> diffAdapters = asList(SpanDiffAdapter.POS);

        addRandomMergeCas(casByUser);

        DiffResult result = CasDiff2.doDiff(entryTypes, diffAdapters,
                LinkCompareBehavior.LINK_TARGET_AS_LABEL, casByUser);

        CAS mergeCas = MergeCas.reMergeCas(result, getSingleCasByUser(casByUser));

        casByUser = new HashMap<>();
        CAS actual = DiffUtils.readWebAnnoTSV("mergecas/simplespan/1sentenceempty.tsv", null);
        casByUser.put("actual", asList(actual));
        casByUser.put("merge", asList(mergeCas));

        result = CasDiff2.doDiff(entryTypes, diffAdapters, LinkCompareBehavior.LINK_TARGET_AS_LABEL,
                casByUser);

        assertEquals(0, result.getDifferingConfigurationSets().size());
        assertEquals(0, result.getIncompleteConfigurationSets().size());

    }

    @Test
    public void simpleSpanDiffWithLabelAndEmptyTest()
        throws Exception
    {
        Map<String, List<CAS>> casByUser = DiffUtils.loadWebAnnoTSV(null,
                "mergecas/simplespan/1sentence.tsv", "mergecas/simplespan/1sentenceempty.tsv");

        List<String> entryTypes = asList(POS.class.getName());

        List<SpanDiffAdapter> diffAdapters = asList(SpanDiffAdapter.POS);

        addRandomMergeCas(casByUser);

        DiffResult result = CasDiff2.doDiff(entryTypes, diffAdapters,
                LinkCompareBehavior.LINK_TARGET_AS_LABEL, casByUser);

        CAS mergeCas = MergeCas.reMergeCas(result, getSingleCasByUser(casByUser));

        casByUser = new HashMap<>();
        CAS actual = DiffUtils.readWebAnnoTSV("mergecas/simplespan/1sentenceempty.tsv", null);
        casByUser.put("actual", asList(actual));
        casByUser.put("merge", asList(mergeCas));

        result = CasDiff2.doDiff(entryTypes, diffAdapters, LinkCompareBehavior.LINK_TARGET_AS_LABEL,
                casByUser);

        assertEquals(0, result.getDifferingConfigurationSets().size());
        assertEquals(0, result.getIncompleteConfigurationSets().size());

    }

    @Test
    public void simpleSpanNoDiffWithLabelTest()
        throws Exception
    {
        Map<String, List<CAS>> casByUser = DiffUtils.loadWebAnnoTSV(null,
                "mergecas/simplespan/1sentenceposlabel.tsv",
                "mergecas/simplespan/1sentenceposlabel.tsv");

        List<String> entryTypes = asList(POS.class.getName());

        List<SpanDiffAdapter> diffAdapters = asList(SpanDiffAdapter.POS);

        addRandomMergeCas(casByUser);

        DiffResult result = CasDiff2.doDiff(entryTypes, diffAdapters,
                LinkCompareBehavior.LINK_TARGET_AS_LABEL, casByUser);

        CAS mergeCas = MergeCas.reMergeCas(result, getSingleCasByUser(casByUser));

        casByUser = new HashMap<>();
        CAS actual = DiffUtils.readWebAnnoTSV("mergecas/simplespan/1sentenceposlabel.tsv", null);
        casByUser.put("actual", asList(actual));
        casByUser.put("merge", asList(mergeCas));

        result = CasDiff2.doDiff(entryTypes, diffAdapters, LinkCompareBehavior.LINK_TARGET_AS_LABEL,
                casByUser);

        assertEquals(0, result.getDifferingConfigurationSets().size());
        assertEquals(0, result.getIncompleteConfigurationSets().size());

    }

    @Test
    public void simpleSpanDiffWithLabelTest()
        throws Exception
    {
        Map<String, List<CAS>> casByUser = DiffUtils.loadWebAnnoTSV(null,
                "mergecas/simplespan/1sentenceposlabel.tsv",
                "mergecas/simplespan/1sentenceposlabel2.tsv");

        List<String> entryTypes = asList(POS.class.getName());

        List<SpanDiffAdapter> diffAdapters = asList(SpanDiffAdapter.POS);

        addRandomMergeCas(casByUser);

        DiffResult result = CasDiff2.doDiff(entryTypes, diffAdapters,
                LinkCompareBehavior.LINK_TARGET_AS_LABEL, casByUser);

        CAS mergeCas = MergeCas.reMergeCas(result, getSingleCasByUser(casByUser));

        casByUser = new HashMap<>();
        CAS actual = DiffUtils.readWebAnnoTSV("mergecas/simplespan/1sentenceempty.tsv", null);
        casByUser.put("actual", asList(actual));
        casByUser.put("merge", asList(mergeCas));

        result = CasDiff2.doDiff(entryTypes, diffAdapters, LinkCompareBehavior.LINK_TARGET_AS_LABEL,
                casByUser);

        assertEquals(0, result.getDifferingConfigurationSets().size());
        assertEquals(0, result.getIncompleteConfigurationSets().size());

    }

    @Test
    public void simpleSpanDiffWithLabelStackingTest()
        throws Exception
    {
        Map<String, List<CAS>> casByUser = DiffUtils.loadWebAnnoTSV(null,
                "mergecas/simplespan/1sentenceNEstacked.tsv",
                "mergecas/simplespan/1sentenceNEstacked.tsv");

        List<String> entryTypes = asList(NamedEntity.class.getName());

        List<SpanDiffAdapter> diffAdapters = asList(SpanDiffAdapter.NER);

        addRandomMergeCas(casByUser);

        DiffResult result = CasDiff2.doDiff(entryTypes, diffAdapters,
                LinkCompareBehavior.LINK_TARGET_AS_LABEL, casByUser);

        CAS mergeCas = MergeCas.reMergeCas(result, getSingleCasByUser(casByUser));

        casByUser = new HashMap<>();
        CAS actual = DiffUtils.readWebAnnoTSV("mergecas/simplespan/1sentenceNEempty.tsv", null);
        casByUser.put("actual", asList(actual));
        casByUser.put("merge", asList(mergeCas));

        result = CasDiff2.doDiff(entryTypes, diffAdapters, LinkCompareBehavior.LINK_TARGET_AS_LABEL,
                casByUser);

        assertEquals(0, result.getDifferingConfigurationSets().size());
        assertEquals(0, result.getIncompleteConfigurationSets().size());

    }

    @Test
    public void simpleSpanDiffWithLabelStacking2Test()
        throws Exception
    {
        Map<String, List<CAS>> casByUser = DiffUtils.loadWebAnnoTSV(null,
                "mergecas/simplespan/1sentenceNE.tsv",
                "mergecas/simplespan/1sentenceNEstacked.tsv");

        List<String> entryTypes = asList(NamedEntity.class.getName());

        List<SpanDiffAdapter> diffAdapters = asList(SpanDiffAdapter.NER);

        addRandomMergeCas(casByUser);

        DiffResult result = CasDiff2.doDiff(entryTypes, diffAdapters,
                LinkCompareBehavior.LINK_TARGET_AS_LABEL, casByUser);

        CAS mergeCas = MergeCas.reMergeCas(result, getSingleCasByUser(casByUser));

        casByUser = new HashMap<>();
        CAS actual = DiffUtils.readWebAnnoTSV("mergecas/simplespan/1sentenceNEempty.tsv", null);
        casByUser.put("actual", asList(actual));
        casByUser.put("merge", asList(mergeCas));

        result = CasDiff2.doDiff(entryTypes, diffAdapters, LinkCompareBehavior.LINK_TARGET_AS_LABEL,
                casByUser);

        assertEquals(0, result.getDifferingConfigurationSets().size());
        assertEquals(0, result.getIncompleteConfigurationSets().size());

    }

    @Test
    public void simpleSpanDiffWithLabelStacking3Test()
        throws Exception
    {
        Map<String, List<CAS>> casByUser = DiffUtils.loadWebAnnoTSV(null,
                "mergecas/simplespan/1sentenceNE.tsv",
                "mergecas/simplespan/1sentenceNEstacked2.tsv");

        List<String> entryTypes = asList(NamedEntity.class.getName());

        List<SpanDiffAdapter> diffAdapters = asList(SpanDiffAdapter.NER);

        addRandomMergeCas(casByUser);

        DiffResult result = CasDiff2.doDiff(entryTypes, diffAdapters,
                LinkCompareBehavior.LINK_TARGET_AS_LABEL, casByUser);

        CAS mergeCas = MergeCas.reMergeCas(result, getSingleCasByUser(casByUser));

        casByUser = new HashMap<>();
        CAS actual = DiffUtils.readWebAnnoTSV("mergecas/simplespan/1sentenceNEstacked2merge.tsv",
                null);
        casByUser.put("actual", asList(actual));
        casByUser.put("merge", asList(mergeCas));

        result = CasDiff2.doDiff(entryTypes, diffAdapters, LinkCompareBehavior.LINK_TARGET_AS_LABEL,
                casByUser);

        assertEquals(0, result.getDifferingConfigurationSets().size());
        assertEquals(0, result.getIncompleteConfigurationSets().size());

    }

    @Test
    public void simpleSpanNoDiffMultiFeatureTest()
        throws Exception
    {
        TypeSystemDescription customeTypes = DiffUtils.createCustomTypeSystem(SPAN_TYPE,
                "webanno.custom.Opinion", asList("aspect", "opinion"), null);

        Map<String, List<CAS>> casByUser = DiffUtils.loadWebAnnoTSV(customeTypes,
                "mergecas/spanmultifeature/1sentenceNENoFeature.tsv",
                "mergecas/spanmultifeature/1sentenceNENoFeature.tsv");

        List<String> entryTypes = asList("webanno.custom.Opinion");
        SpanDiffAdapter spanAdapter = new SpanDiffAdapter("webanno.custom.Opinion", "aspect",
                "opinion");

        List<SpanDiffAdapter> diffAdapters = asList(spanAdapter);

        addRandomMergeCas(casByUser);

        DiffResult result = CasDiff2.doDiff(entryTypes, diffAdapters,
                LinkCompareBehavior.LINK_TARGET_AS_LABEL, casByUser);

        CAS mergeCas = MergeCas.reMergeCas(result, getSingleCasByUser(casByUser));

        casByUser = new HashMap<>();
        CAS actual = DiffUtils.readWebAnnoTSV("mergecas/spanmultifeature/1sentenceNENoFeature.tsv",
                customeTypes);
        casByUser.put("actual", asList(actual));
        casByUser.put("merge", asList(mergeCas));

        result = CasDiff2.doDiff(entryTypes, diffAdapters, LinkCompareBehavior.LINK_TARGET_AS_LABEL,
                casByUser);

        assertEquals(0, result.getDifferingConfigurationSets().size());
        assertEquals(0, result.getIncompleteConfigurationSets().size());

    }

    @Test
    public void simpleSpanDiffMultiFeatureTest()
        throws Exception
    {
        TypeSystemDescription customeTypes = DiffUtils.createCustomTypeSystem(SPAN_TYPE,
                "webanno.custom.Opinion", asList("aspect", "opinion"), null);

        Map<String, List<CAS>> casByUser = DiffUtils.loadWebAnnoTSV(customeTypes,
                "mergecas/spanmultifeature/1sentenceNEFeatureA.tsv",
                "mergecas/spanmultifeature/1sentenceNEFeatureB.tsv");

        List<String> entryTypes = asList("webanno.custom.Opinion");
        SpanDiffAdapter spanAdapter = new SpanDiffAdapter("webanno.custom.Opinion", "aspect",
                "opinion");

        List<SpanDiffAdapter> diffAdapters = asList(spanAdapter);

        addRandomMergeCas(casByUser);

        DiffResult result = CasDiff2.doDiff(entryTypes, diffAdapters,
                LinkCompareBehavior.LINK_TARGET_AS_LABEL, casByUser);

        CAS mergeCas = MergeCas.reMergeCas(result, getSingleCasByUser(casByUser));

        casByUser = new HashMap<>();
        CAS actual = DiffUtils.readWebAnnoTSV(
                "mergecas/spanmultifeature/1sentenceNEFeatureempty.tsv", customeTypes);
        casByUser.put("actual", asList(actual));
        casByUser.put("merge", asList(mergeCas));

        result = CasDiff2.doDiff(entryTypes, diffAdapters, LinkCompareBehavior.LINK_TARGET_AS_LABEL,
                casByUser);

        assertEquals(0, result.getDifferingConfigurationSets().size());
        assertEquals(0, result.getIncompleteConfigurationSets().size());

    }

    @Test
    public void simpleRelNoDiffTest()
        throws Exception
    {
        Map<String, List<CAS>> casByUser = DiffUtils.loadWebAnnoTSV(null,
                "mergecas/rels/1sentencesamerel.tsv", "mergecas/rels/1sentencesamerel.tsv");

        List<String> entryTypes = asList(Dependency.class.getName(), POS.class.getName());

        List<? extends DiffAdapter> diffAdapters = asList(
                new ArcDiffAdapter(Dependency.class.getName(), "Dependent", "Governor",
                        "DependencyType"),
                SpanDiffAdapter.POS);

        addRandomMergeCas(casByUser);

        DiffResult result = CasDiff2.doDiff(entryTypes, diffAdapters,
                LinkCompareBehavior.LINK_TARGET_AS_LABEL, casByUser);

        CAS mergeCas = MergeCas.reMergeCas(result, getSingleCasByUser(casByUser));

        casByUser = new HashMap<>();
        CAS actual = DiffUtils.readWebAnnoTSV("mergecas/rels/1sentencesamerel.tsv", null);
        casByUser.put("actual", asList(actual));
        casByUser.put("merge", asList(mergeCas));

        result = CasDiff2.doDiff(entryTypes, diffAdapters, LinkCompareBehavior.LINK_TARGET_AS_LABEL,
                casByUser);

        assertEquals(0, result.getDifferingConfigurationSets().size());
        assertEquals(0, result.getIncompleteConfigurationSets().size());

    }

    @Test
    public void simpleRelGovDiffTest()
        throws Exception
    {
        Map<String, List<CAS>> casByUser = DiffUtils.loadWebAnnoTSV(null,
                "mergecas/rels/1sentencesamerel.tsv", "mergecas/rels/1sentencesamerel2.tsv");

        List<String> entryTypes = asList(Dependency.class.getName(), POS.class.getName());

        List<? extends DiffAdapter> diffAdapters = asList(
                new ArcDiffAdapter(Dependency.class.getName(), "Dependent", "Governor",
                        "DependencyType"),
                SpanDiffAdapter.POS);

        addRandomMergeCas(casByUser);

        DiffResult result = CasDiff2.doDiff(entryTypes, diffAdapters,
                LinkCompareBehavior.LINK_TARGET_AS_LABEL, casByUser);

        CAS mergeCas = MergeCas.reMergeCas(result, getSingleCasByUser(casByUser));

        casByUser = new HashMap<>();
        CAS actual = DiffUtils.readWebAnnoTSV("mergecas/rels/1sentencesamerel3.tsv", null);
        casByUser.put("actual", asList(actual));
        casByUser.put("merge", asList(mergeCas));

        result = CasDiff2.doDiff(entryTypes, diffAdapters, LinkCompareBehavior.LINK_TARGET_AS_LABEL,
                casByUser);

        assertEquals(0, result.getDifferingConfigurationSets().size());
        assertEquals(0, result.getIncompleteConfigurationSets().size());

    }

    @Test
    public void simpleRelTypeDiffTest()
        throws Exception
    {
        Map<String, List<CAS>> casByUser = DiffUtils.loadWebAnnoTSV(null,
                "mergecas/rels/1sentencesamerel.tsv", "mergecas/rels/1sentencesamerel4.tsv");

        List<String> entryTypes = asList(Dependency.class.getName(), POS.class.getName());

        List<? extends DiffAdapter> diffAdapters = asList(
                new ArcDiffAdapter(Dependency.class.getName(), "Dependent", "Governor",
                        "DependencyType"),
                SpanDiffAdapter.POS);

        addRandomMergeCas(casByUser);

        DiffResult result = CasDiff2.doDiff(entryTypes, diffAdapters,
                LinkCompareBehavior.LINK_TARGET_AS_LABEL, casByUser);

        CAS mergeCas = MergeCas.reMergeCas(result, getSingleCasByUser(casByUser));

        casByUser = new HashMap<>();
        CAS actual = DiffUtils.readWebAnnoTSV("mergecas/rels/1sentencesamerel5.tsv", null);
        casByUser.put("actual", asList(actual));
        casByUser.put("merge", asList(mergeCas));

        result = CasDiff2.doDiff(entryTypes, diffAdapters, LinkCompareBehavior.LINK_TARGET_AS_LABEL,
                casByUser);

        assertEquals(0, result.getDifferingConfigurationSets().size());
        assertEquals(0, result.getIncompleteConfigurationSets().size());

    }

    @Test
    public void simpleRelGovStackedTest()
        throws Exception
    {
        TypeSystemDescription customeTypesSpan = DiffUtils.createCustomTypeSystem(SPAN_TYPE,
                "webanno.custom.Multivalspan", asList("f1", "f2"), null);

        TypeSystemDescription customeTypesRel = DiffUtils.createCustomTypeSystem(RELATION_TYPE,
                "webanno.custom.Multivalrel", asList("rel1", "rel2"),
                "webanno.custom.Multivalspan");

        List<TypeSystemDescription> customTypes = new ArrayList<>();
        customTypes.add(customeTypesSpan);
        customTypes.add(customeTypesRel);
        TypeSystemDescription customType = CasCreationUtils.mergeTypeSystems(customTypes);

        Map<String, List<CAS>> casByUser = DiffUtils.loadWebAnnoTSV(customType,
                "mergecas/multivalspanrel/tale.tsv", "mergecas/multivalspanrel/tale.tsv");

        List<String> entryTypes = asList("webanno.custom.Multivalspan",
                "webanno.custom.Multivalrel");

        List<? extends DiffAdapter> diffAdapters = asList(
                new ArcDiffAdapter("webanno.custom.Multivalrel", "Dependent", "Governor", "rel1",
                        "rel2"),
                new SpanDiffAdapter("webanno.custom.Multivalspan", "f1", "f2"));

        addRandomMergeCas(casByUser);

        DiffResult result = CasDiff2.doDiff(entryTypes, diffAdapters,
                LinkCompareBehavior.LINK_TARGET_AS_LABEL, casByUser);

        CAS mergeCas = MergeCas.reMergeCas(result, getSingleCasByUser(casByUser));

        casByUser = new HashMap<>();
        CAS actual = DiffUtils.readWebAnnoTSV("mergecas/multivalspanrel/tale2.tsv", customType);
        casByUser.put("actual", asList(actual));
        casByUser.put("merge", asList(mergeCas));

        result = CasDiff2.doDiff(entryTypes, diffAdapters, LinkCompareBehavior.LINK_TARGET_AS_LABEL,
                casByUser);

        assertEquals(0, result.getDifferingConfigurationSets().size());
        assertEquals(0, result.getIncompleteConfigurationSets().size());

    }

    @Test
    public void relStackedTest()
        throws Exception
    {
        TypeSystemDescription customeTypesSpan = DiffUtils.createCustomTypeSystem(SPAN_TYPE,
                "webanno.custom.Multivalspan", asList("f1", "f2"), null);

        TypeSystemDescription customeTypesRel = DiffUtils.createCustomTypeSystem(RELATION_TYPE,
                "webanno.custom.Multivalrel", asList("rel1", "rel2"),
                "webanno.custom.Multivalspan");

        List<TypeSystemDescription> customTypes = new ArrayList<>();
        customTypes.add(customeTypesSpan);
        customTypes.add(customeTypesRel);
        TypeSystemDescription customType = CasCreationUtils.mergeTypeSystems(customTypes);

        Map<String, List<CAS>> casByUser = DiffUtils.loadXMI(customType,
                "mergecas/multivalspanrel/stackedrel1.xmi",
                "mergecas/multivalspanrel/stackedrel2.xmi");

        List<String> entryTypes = asList("webanno.custom.Multivalspan",
                "webanno.custom.Multivalrel");

        List<? extends DiffAdapter> diffAdapters = asList(
                new ArcDiffAdapter("webanno.custom.Multivalrel", "Dependent", "Governor", "rel1",
                        "rel2"),
                new SpanDiffAdapter("webanno.custom.Multivalspan", "f1", "f2"));

        addRandomMergeCas(casByUser);

        DiffResult result = CasDiff2.doDiff(entryTypes, diffAdapters,
                LinkCompareBehavior.LINK_TARGET_AS_LABEL, casByUser);

        CAS mergeCas = MergeCas.reMergeCas(result, getSingleCasByUser(casByUser));
        CAS actual = DiffUtils.readXMI("mergecas/multivalspanrel/stackedmerge.xmi", customType);

        Type relType = mergeCas.getTypeSystem().getType("webanno.custom.Multivalrel");
        int numRelMerge = CasUtil.select(mergeCas, relType).size();
        int numRelActual = CasUtil.select(actual, relType).size();

        Type spanType = mergeCas.getTypeSystem().getType("webanno.custom.Multivalspan");
        int numspanMerge = CasUtil.select(mergeCas, spanType).size();
        int numspanActual = CasUtil.select(actual, spanType).size();

        assertEquals(2, numRelMerge);
        assertEquals(2, numRelActual);

        assertEquals(4, numspanMerge);
        assertEquals(4, numspanActual);

    }

    @Test
    public void relationLabelTestTest()
        throws Exception
    {
        Map<String, List<CAS>> casByUser = DiffUtils.load("casdiff/relationLabel/user1.conll",
                "casdiff/relationLabel/user2.conll");

        List<String> entryTypes = asList(Dependency.class.getName());

        List<? extends DiffAdapter> diffAdapters = asList(new ArcDiffAdapter(
                Dependency.class.getName(), "Dependent", "Governor", "DependencyType"));

        addRandomMergeCas(casByUser);

        DiffResult result = CasDiff2.doDiff(entryTypes, diffAdapters,
                LinkCompareBehavior.LINK_TARGET_AS_LABEL, casByUser);

        result.print(System.out);

        CAS mergeCas = MergeCas.reMergeCas(result, getSingleCasByUser(casByUser));
        CAS actual = DiffUtils.read("casdiff/relationLabel/merge.conll");

        casByUser = new HashMap<>();

        casByUser.put("actual", asList(actual));
        casByUser.put("merge", asList(mergeCas));

        result = CasDiff2.doDiff(entryTypes, diffAdapters, LinkCompareBehavior.LINK_TARGET_AS_LABEL,
                casByUser);

        assertEquals(0, result.getDifferingConfigurationSets().size());
        assertEquals(0, result.getIncompleteConfigurationSets().size());
    }

    @Test
    public void multiLinkWithRoleNoDifferenceTest()
        throws Exception
    {
        JCas jcasA = JCasFactory.createJCas(DiffUtils.createMultiLinkWithRoleTestTypeSytem());
        DiffUtils.makeLinkHostFS(jcasA, 0, 0, DiffUtils.makeLinkFS(jcasA, "slot1", 0, 0));
        DiffUtils.makeLinkHostFS(jcasA, 10, 10, DiffUtils.makeLinkFS(jcasA, "slot1", 10, 10));

        JCas jcasB = JCasFactory.createJCas(DiffUtils.createMultiLinkWithRoleTestTypeSytem());
        DiffUtils.makeLinkHostFS(jcasB, 0, 0, DiffUtils.makeLinkFS(jcasB, "slot1", 0, 0));
        DiffUtils.makeLinkHostFS(jcasB, 10, 10, DiffUtils.makeLinkFS(jcasB, "slot1", 10, 10));

        Map<String, List<CAS>> casByUser = new LinkedHashMap<>();
        casByUser.put("user1", asList(jcasA.getCas()));
        casByUser.put("user2", asList(jcasB.getCas()));

        casByUser.put(CURATION_USER, asList(jcasA.getCas()));

        List<String> entryTypes = asList(DiffUtils.HOST_TYPE);

        SpanDiffAdapter adapter = new SpanDiffAdapter(DiffUtils.HOST_TYPE);
        adapter.addLinkFeature("links", "role", "target");
        List<? extends DiffAdapter> diffAdapters = asList(adapter);

        DiffResult result = CasDiff2.doDiff(entryTypes, diffAdapters,
                LinkCompareBehavior.LINK_TARGET_AS_LABEL, casByUser);

        result.print(System.out);

        CAS mergeCas = MergeCas.reMergeCas(result, getSingleCasByUser(casByUser));

        casByUser = new HashMap<>();
        casByUser.put("actual", asList(jcasA.getCas()));
        casByUser.put("merge", asList(mergeCas));

        result = CasDiff2.doDiff(entryTypes, diffAdapters, LinkCompareBehavior.LINK_TARGET_AS_LABEL,
                casByUser);

        assertEquals(0, result.getDifferingConfigurationSets().size());
        assertEquals(0, result.getIncompleteConfigurationSets().size());
    }

    @Test
    public void multiLinkWithRoleLabelDifferenceTest()
        throws Exception
    {
        JCas jcasA = JCasFactory.createJCas(DiffUtils.createMultiLinkWithRoleTestTypeSytem());
        DiffUtils.makeLinkHostFS(jcasA, 0, 0, DiffUtils.makeLinkFS(jcasA, "slot1", 0, 0));

        JCas jcasB = JCasFactory.createJCas(DiffUtils.createMultiLinkWithRoleTestTypeSytem());
        DiffUtils.makeLinkHostFS(jcasB, 0, 0, DiffUtils.makeLinkFS(jcasB, "slot2", 0, 0));

        Map<String, List<CAS>> casByUser = new LinkedHashMap<>();
        casByUser.put("user1", asList(jcasA.getCas()));
        casByUser.put("user2", asList(jcasB.getCas()));
        casByUser.put(CURATION_USER, asList(jcasA.getCas()));

        List<String> entryTypes = asList(DiffUtils.HOST_TYPE);

        SpanDiffAdapter adapter = new SpanDiffAdapter(DiffUtils.HOST_TYPE);
        adapter.addLinkFeature("links", "role", "target");
        List<? extends DiffAdapter> diffAdapters = asList(adapter);

        DiffResult result = CasDiff2.doDiff(entryTypes, diffAdapters,
                LinkCompareBehavior.LINK_TARGET_AS_LABEL, casByUser);

        result.print(System.out);

        CAS mergeCas = MergeCas.reMergeCas(result, getSingleCasByUser(casByUser));

        Type hostType = mergeCas.getTypeSystem().getType(DiffUtils.HOST_TYPE);
        int numHost = CasUtil.select(mergeCas, hostType).size();

        assertEquals(1, numHost);
        for (FeatureStructure host : CasUtil.select(mergeCas, hostType)) {
            ArrayFS linkFss = (ArrayFS) WebAnnoCasUtil.getFeatureFS(host, "links");
            assertEquals(0, linkFss.toArray().length);
        }

    }

    @Test
    public void multiLinkWithRoleTargetDifferenceTest()
        throws Exception
    {
        JCas jcasA = JCasFactory.createJCas(DiffUtils.createMultiLinkWithRoleTestTypeSytem());
        DiffUtils.makeLinkHostFS(jcasA, 0, 0, DiffUtils.makeLinkFS(jcasA, "slot1", 0, 0));

        JCas jcasB = JCasFactory.createJCas(DiffUtils.createMultiLinkWithRoleTestTypeSytem());
        DiffUtils.makeLinkHostFS(jcasB, 0, 0, DiffUtils.makeLinkFS(jcasB, "slot1", 10, 10));

        Map<String, List<CAS>> casByUser = new LinkedHashMap<>();
        casByUser.put("user1", asList(jcasA.getCas()));
        casByUser.put("user2", asList(jcasB.getCas()));

        casByUser.put(CURATION_USER, asList(jcasA.getCas()));

        List<String> entryTypes = asList(DiffUtils.HOST_TYPE);

        SpanDiffAdapter adapter = new SpanDiffAdapter(DiffUtils.HOST_TYPE);
        adapter.addLinkFeature("links", "role", "target");
        List<? extends DiffAdapter> diffAdapters = asList(adapter);

        DiffResult result = CasDiff2.doDiff(entryTypes, diffAdapters,
                LinkCompareBehavior.LINK_TARGET_AS_LABEL, casByUser);

        result.print(System.out);

        CAS mergeCas = MergeCas.reMergeCas(result, getSingleCasByUser(casByUser));

        Type hostType = mergeCas.getTypeSystem().getType(DiffUtils.HOST_TYPE);
        int numHost = CasUtil.select(mergeCas, hostType).size();

        assertEquals(1, numHost);
        for (FeatureStructure host : CasUtil.select(mergeCas, hostType)) {
            ArrayFS linkFss = (ArrayFS) WebAnnoCasUtil.getFeatureFS(host, "links");
            assertEquals(0, linkFss.toArray().length);
        }
    }

    @Test
    public void multiLinkMultiHostTest()
        throws Exception
    {
        JCas jcasA = JCasFactory.createJCas(DiffUtils.createMultiLinkWithRoleTestTypeSytem());
        DiffUtils.makeLinkHostFS(jcasA, 0, 0, DiffUtils.makeLinkFS(jcasA, "slot1", 0, 0));
        DiffUtils.makeLinkHostFS(jcasA, 0, 0, DiffUtils.makeLinkFS(jcasA, "slot1", 0, 0));

        JCas jcasB = JCasFactory.createJCas(DiffUtils.createMultiLinkWithRoleTestTypeSytem());
        DiffUtils.makeLinkHostFS(jcasB, 0, 0, DiffUtils.makeLinkFS(jcasB, "slot1", 0, 0));

        Map<String, List<CAS>> casByUser = new LinkedHashMap<>();
        casByUser.put("user1", asList(jcasA.getCas()));
        casByUser.put("user2", asList(jcasB.getCas()));

        casByUser.put(CURATION_USER, asList(jcasB.getCas()));

        List<String> entryTypes = asList(DiffUtils.HOST_TYPE);

        SpanDiffAdapter adapter = new SpanDiffAdapter(DiffUtils.HOST_TYPE);
        adapter.addLinkFeature("links", "role", "target");
        List<? extends DiffAdapter> diffAdapters = asList(adapter);

        DiffResult result = CasDiff2.doDiff(entryTypes, diffAdapters,
                LinkCompareBehavior.LINK_TARGET_AS_LABEL, casByUser);

        result.print(System.out);

        CAS mergeCas = MergeCas.reMergeCas(result, getSingleCasByUser(casByUser));

        Type hostType = mergeCas.getTypeSystem().getType(DiffUtils.HOST_TYPE);
        int numHost = CasUtil.select(mergeCas, hostType).size();

        assertEquals(0, numHost);
    }

    @Test
    public void multiLinkMultiSpanRoleDiffTest()
        throws Exception
    {

        JCas jcasA = JCasFactory.createJCas(DiffUtils.createMultiLinkWithRoleTestTypeSytem("f1"));
        Type type = jcasA.getTypeSystem().getType(DiffUtils.HOST_TYPE);
        Feature feature = type.getFeatureByBaseName("f1");

        DiffUtils.makeLinkHostMultiSPanFeatureFS(jcasA, 0, 0, feature, "A",
                DiffUtils.makeLinkFS(jcasA, "slot1", 0, 0));

        JCas jcasB = JCasFactory.createJCas(DiffUtils.createMultiLinkWithRoleTestTypeSytem("f1"));
        DiffUtils.makeLinkHostMultiSPanFeatureFS(jcasB, 0, 0, feature, "A",
                DiffUtils.makeLinkFS(jcasB, "slot2", 0, 0));

        Map<String, List<CAS>> casByUser = new LinkedHashMap<>();
        casByUser.put("user1", asList(jcasA.getCas()));
        casByUser.put("user2", asList(jcasB.getCas()));

        casByUser.put(CURATION_USER, asList(jcasA.getCas()));

        List<String> entryTypes = asList(DiffUtils.HOST_TYPE);

        SpanDiffAdapter adapter = new SpanDiffAdapter(DiffUtils.HOST_TYPE);
        adapter.addLinkFeature("links", "role", "target");
        List<? extends DiffAdapter> diffAdapters = asList(adapter);

        DiffResult result = CasDiff2.doDiff(entryTypes, diffAdapters,
                LinkCompareBehavior.LINK_TARGET_AS_LABEL, casByUser);

        result.print(System.out);

        CAS mergeCas = MergeCas.reMergeCas(result, getSingleCasByUser(casByUser));

        Type hostType = mergeCas.getTypeSystem().getType(DiffUtils.HOST_TYPE);
        int numHost = CasUtil.select(mergeCas, hostType).size();

        assertEquals(1, numHost);
    }

    private Map<String, CAS> getSingleCasByUser(Map<String, List<CAS>> aCasByUserSingle)
    {

        Map<String, CAS> casByUserSingle = new HashMap<>();
        for (String user : aCasByUserSingle.keySet()) {
            casByUserSingle.put(user, aCasByUserSingle.get(user).get(0));
        }

        return casByUserSingle;
    }

    private void addRandomMergeCas(Map<String, List<CAS>> casByUser)
    {
        String randomUser = casByUser.keySet().stream().findFirst().orElse(null);
        assert (randomUser != null);
        CAS randomCas = casByUser.get(randomUser).get(0);
        casByUser.put(CURATION_USER, asList(randomCas));
    }

    @Rule
    public DkproTestContext testContext = new DkproTestContext();
}

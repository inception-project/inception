/*
 * Copyright 2012
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
package de.tudarmstadt.ukp.clarin.webanno.tsv;

import static org.apache.uima.fit.factory.AnalysisEngineFactory.createEngineDescription;
import static org.apache.uima.fit.pipeline.SimplePipeline.runPipeline;
import static org.junit.Assert.assertEquals;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.uima.analysis_engine.AnalysisEngineDescription;
import org.apache.uima.cas.CAS;
import org.apache.uima.collection.CollectionReader;
import org.apache.uima.fit.factory.CollectionReaderFactory;
import org.apache.uima.fit.factory.JCasFactory;
import org.apache.uima.fit.util.JCasUtil;
import org.junit.Rule;
import org.junit.Test;

import de.tudarmstadt.ukp.dkpro.core.api.lexmorph.type.pos.POS;
import de.tudarmstadt.ukp.dkpro.core.api.ner.type.NamedEntity;
import de.tudarmstadt.ukp.dkpro.core.api.segmentation.type.Lemma;
import de.tudarmstadt.ukp.dkpro.core.api.segmentation.type.Sentence;
import de.tudarmstadt.ukp.dkpro.core.api.segmentation.type.Token;
import de.tudarmstadt.ukp.dkpro.core.api.syntax.type.dependency.Dependency;
import de.tudarmstadt.ukp.dkpro.core.testing.DkproTestContext;

public class WebAnnoTsv3ReaderWriterTest
{
    @Test
    public void test()
        throws Exception
    {
        String targetFolder = "target/test-output/" + testContext.getTestOutputFolderName();
        
        CollectionReader reader = CollectionReaderFactory.createReader(
                WebannoTsv3Reader.class,
                WebannoTsv3Reader.PARAM_SOURCE_LOCATION, "src/test/resources/tsv3/",
                WebannoTsv3Reader.PARAM_PATTERNS, "coref.tsv");

        List<String> slotFeatures = new ArrayList<>();
        List<String> slotTargets = new ArrayList<>();
        List<String> linkTypes = new ArrayList<>();
        List<String> spanLayers = new ArrayList<>();
        spanLayers.add(NamedEntity.class.getName());
        spanLayers.add(POS.class.getName());
        spanLayers.add(Lemma.class.getName());
        List<String> chainLayers = new ArrayList<>();
        chainLayers.add("de.tudarmstadt.ukp.dkpro.core.api.coref.type.Coreference");
        List<String> relationLayers = new ArrayList<>();
        relationLayers.add(Dependency.class.getName());

        AnalysisEngineDescription writer = createEngineDescription(
                WebannoTsv3Writer.class,
                WebannoTsv3Writer.PARAM_TARGET_LOCATION, targetFolder,
                WebannoTsv3Writer.PARAM_STRIP_EXTENSION, true, 
                WebannoTsv3Writer.PARAM_OVERWRITE, true, 
                WebannoTsv3Writer.PARAM_SPAN_LAYERS, spanLayers, 
                WebannoTsv3Writer.PARAM_SLOT_FEATS, slotFeatures,
                WebannoTsv3Writer.PARAM_SLOT_TARGETS, slotTargets,
                WebannoTsv3Writer.PARAM_LINK_TYPES, linkTypes, 
                WebannoTsv3Writer.PARAM_CHAIN_LAYERS, chainLayers,
                WebannoTsv3Writer.PARAM_RELATION_LAYERS, relationLayers);

        runPipeline(reader, writer);

        CollectionReader reader1 = CollectionReaderFactory.createReader(
                WebannoTsv3Reader.class,
                WebannoTsv3Reader.PARAM_SOURCE_LOCATION, "src/test/resources/tsv3/",
                WebannoTsv3Reader.PARAM_PATTERNS, "coref.tsv");

        CollectionReader reader2 = CollectionReaderFactory.createReader(
                WebannoTsv3Reader.class,
                WebannoTsv3Reader.PARAM_SOURCE_LOCATION, targetFolder,
                WebannoTsv3Reader.PARAM_PATTERNS, "coref.tsv");

        CAS cas1 = JCasFactory.createJCas().getCas();
        reader1.getNext(cas1);

        CAS cas2 = JCasFactory.createJCas().getCas();
        reader2.getNext(cas2);

        assertEquals(JCasUtil.select(cas2.getJCas(), Token.class).size(),
                JCasUtil.select(cas1.getJCas(), Token.class).size());
        assertEquals(JCasUtil.select(cas2.getJCas(), POS.class).size(),
                JCasUtil.select(cas1.getJCas(), POS.class).size());
        assertEquals(JCasUtil.select(cas2.getJCas(), Lemma.class).size(),
                JCasUtil.select(cas1.getJCas(), Lemma.class).size());
        assertEquals(JCasUtil.select(cas2.getJCas(), NamedEntity.class).size(),
                JCasUtil.select(cas1.getJCas(), NamedEntity.class).size());
        assertEquals(JCasUtil.select(cas2.getJCas(), Sentence.class).size(),
                JCasUtil.select(cas1.getJCas(), Sentence.class).size());
        assertEquals(JCasUtil.select(cas2.getJCas(), Dependency.class).size(),
                JCasUtil.select(cas1.getJCas(), Dependency.class).size());
    }
    
    @Test
    public void testEscaping() throws Exception
    {
        Map<String, String> samples = new LinkedHashMap<>();
        samples.put("hack\\sign", "hack\\\\sign");
        samples.put("[lala](url)", "\\[lala\\](url)");
        samples.put("(1|2)", "(1\\|2)");
        samples.put("under_score", "under\\_score");
        samples.put("from -> to", "from \\-> to");
        samples.put("complain; next", "complain\\; next");
        samples.put("1.0\t2.0", "1.0\\t2.0");
        samples.put("new\nline", "new\\nline");
        samples.put("A*-search", "A\\*-search");
        samples.put("[[jo]]->**mo**", "\\[\\[jo\\]\\]\\->\\*\\*mo\\*\\*");
        
        for (Entry<String, String> sample : samples.entrySet()) {
            assertEquals(sample.getValue(), WebannoTsv3Writer.replaceEscapeChars(sample.getKey()));
        }
        
        long start = System.currentTimeMillis();
        for (int n = 0; n < 100000; n++) {
            for (Entry<String, String> sample : samples.entrySet()) {
                WebannoTsv3Writer.replaceEscapeChars(sample.getKey());
            }
        }
        System.out.printf("Time: %dms%n", System.currentTimeMillis() - start);
    }

    @Rule
    public DkproTestContext testContext = new DkproTestContext();
}

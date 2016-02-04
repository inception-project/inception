/*******************************************************************************
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
 ******************************************************************************/
package de.tudarmstadt.ukp.clarin.webanno.tsv;

import static org.apache.uima.fit.factory.AnalysisEngineFactory.createEngineDescription;
import static org.apache.uima.fit.pipeline.SimplePipeline.runPipeline;
import static org.junit.Assert.assertEquals;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import org.apache.uima.analysis_engine.AnalysisEngineDescription;
import org.apache.uima.cas.CAS;
import org.apache.uima.collection.CollectionReader;
import org.apache.uima.fit.factory.CollectionReaderFactory;
import org.apache.uima.fit.factory.JCasFactory;
import org.apache.uima.fit.util.JCasUtil;
import org.junit.Test;

import de.tudarmstadt.ukp.dkpro.core.api.io.JCasFileWriter_ImplBase;
import de.tudarmstadt.ukp.dkpro.core.api.io.ResourceCollectionReaderBase;
import de.tudarmstadt.ukp.dkpro.core.api.lexmorph.type.pos.POS;
import de.tudarmstadt.ukp.dkpro.core.api.ner.type.NamedEntity;
import de.tudarmstadt.ukp.dkpro.core.api.segmentation.type.Lemma;
import de.tudarmstadt.ukp.dkpro.core.api.segmentation.type.Sentence;
import de.tudarmstadt.ukp.dkpro.core.api.segmentation.type.Token;
import de.tudarmstadt.ukp.dkpro.core.api.syntax.type.dependency.Dependency;

public class WebAnnoTsvReaderWriterTest {

	@Test
	public void test() throws Exception {

		CollectionReader reader = CollectionReaderFactory.createReader(WebannoCustomTsvReader.class,
				ResourceCollectionReaderBase.PARAM_SOURCE_LOCATION,
				new File("src/test/resources/tsv/").getAbsolutePath(), ResourceCollectionReaderBase.PARAM_PATTERNS,
				new String[] { "[+]example2.tsv" });

		List<String> slotFeatures = new ArrayList<String>();
		List<String> slotTargets = new ArrayList<String>();
		List<String> linkTypes = new ArrayList<String>();
		List<String> spanLayers = new ArrayList<String>();
		spanLayers.add(NamedEntity.class.getName());
		spanLayers.add(POS.class.getName());
		List<String> chainLayers = new ArrayList<String>();
		chainLayers.add("de.tudarmstadt.ukp.dkpro.core.api.coref.type.Coreference");
		List<String> relationLayers = new ArrayList<String>();
		relationLayers.add(Dependency.class.getName());

		AnalysisEngineDescription writer = createEngineDescription(WebannoCustomTsvWriter.class,
				JCasFileWriter_ImplBase.PARAM_TARGET_LOCATION, "target/test-output",
				JCasFileWriter_ImplBase.PARAM_STRIP_EXTENSION, true, "spanLayers", spanLayers, "slotFeatures",
				slotFeatures, "slotTargets", slotTargets, "linkTypes", linkTypes, "chainLayers", chainLayers,
				"relationLayers", relationLayers);

		runPipeline(reader, writer);

		CollectionReader reader1 = CollectionReaderFactory.createReader(WebannoCustomTsvReader.class,
				ResourceCollectionReaderBase.PARAM_SOURCE_LOCATION,
				new File("src/test/resources/tsv/").getAbsolutePath(), ResourceCollectionReaderBase.PARAM_PATTERNS,
				new String[] { "[+]example2.tsv" });

		CollectionReader reader2 = CollectionReaderFactory.createReader(WebannoCustomTsvReader.class,
				ResourceCollectionReaderBase.PARAM_SOURCE_LOCATION,
				new File("src/test/resources/tsv/").getAbsolutePath(), ResourceCollectionReaderBase.PARAM_PATTERNS,
				new String[] { "[+]example2.tsv" });

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
}

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
package de.tudarmstadt.ukp.clarin.webanno.tsv;

import static org.apache.uima.fit.factory.AnalysisEngineFactory.createEngineDescription;
import static org.apache.uima.fit.pipeline.SimplePipeline.runPipeline;
import static org.junit.jupiter.api.Assertions.assertEquals;

import org.apache.uima.analysis_engine.AnalysisEngineDescription;
import org.apache.uima.cas.CAS;
import org.apache.uima.collection.CollectionReader;
import org.apache.uima.fit.factory.CollectionReaderFactory;
import org.apache.uima.fit.factory.JCasFactory;
import org.apache.uima.fit.util.JCasUtil;
import org.junit.jupiter.api.Test;

import de.tudarmstadt.ukp.dkpro.core.api.lexmorph.type.pos.POS;
import de.tudarmstadt.ukp.dkpro.core.api.ner.type.NamedEntity;
import de.tudarmstadt.ukp.dkpro.core.api.segmentation.type.Lemma;
import de.tudarmstadt.ukp.dkpro.core.api.segmentation.type.Sentence;
import de.tudarmstadt.ukp.dkpro.core.api.segmentation.type.Token;
import de.tudarmstadt.ukp.dkpro.core.api.syntax.type.dependency.Dependency;

public class WebAnnoTsv3XReaderWriterTest
{
    @Test
    public void test() throws Exception
    {
        String targetFolder = "target/test-output/" + getClass().getSimpleName();

        // @formatter:off
        CollectionReader reader = CollectionReaderFactory.createReader(
                WebannoTsv3XReader.class,
                WebannoTsv3XReader.PARAM_SOURCE_LOCATION, "src/test/resources/tsv3/",
                WebannoTsv3XReader.PARAM_PATTERNS, "coref.tsv");

        AnalysisEngineDescription writer = createEngineDescription(
                WebannoTsv3XWriter.class,
                WebannoTsv3XWriter.PARAM_TARGET_LOCATION, targetFolder,
                WebannoTsv3XWriter.PARAM_STRIP_EXTENSION, true,
                WebannoTsv3XWriter.PARAM_OVERWRITE, true);

        runPipeline(reader, writer);

        CollectionReader reader1 = CollectionReaderFactory.createReader(
                WebannoTsv3XReader.class,
                WebannoTsv3XReader.PARAM_SOURCE_LOCATION, "src/test/resources/tsv3/",
                WebannoTsv3XReader.PARAM_PATTERNS, "coref.tsv");

        CollectionReader reader2 = CollectionReaderFactory.createReader(
                WebannoTsv3XReader.class,
                WebannoTsv3XReader.PARAM_SOURCE_LOCATION, targetFolder,
                WebannoTsv3XReader.PARAM_PATTERNS, "coref.tsv");
        // @formatter:on

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

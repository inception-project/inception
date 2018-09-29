/*
 * Copyright 2016
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
package de.tudarmstadt.ukp.clarin.webanno.conll;

import static org.apache.uima.fit.factory.AnalysisEngineFactory.createEngineDescription;
import static org.apache.uima.fit.factory.CollectionReaderFactory.createReaderDescription;
import static org.apache.uima.fit.pipeline.SimplePipeline.runPipeline;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.StringReader;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.uima.analysis_engine.AnalysisEngineDescription;
import org.apache.uima.collection.CollectionReaderDescription;
import org.junit.Rule;
import org.junit.Test;

import de.tudarmstadt.ukp.dkpro.core.testing.DkproTestContext;

public class ConllUReaderWriterTest
{
    @Test
    public void roundTrip()
        throws Exception
    {
        CollectionReaderDescription reader = createReaderDescription(
                ConllUReader.class, 
                ConllUReader.PARAM_SOURCE_LOCATION, "src/test/resources/conll/u",
                ConllUReader.PARAM_PATTERNS, "conllu-en-orig.conll");

        AnalysisEngineDescription writer = createEngineDescription(
                ConllUWriter.class,
                ConllUWriter.PARAM_TARGET_LOCATION, "target/test-output/ConllUReaderWriterTest-roundTrip",
                ConllUWriter.PARAM_FILENAME_SUFFIX, ".conll",
                ConllUWriter.PARAM_STRIP_EXTENSION, true);

        runPipeline(reader, writer);

        String reference = FileUtils.readFileToString(
                new File("src/test/resources/conll/u/conllu-en-ref.conll"), "UTF-8")
                .trim();
        String actual = FileUtils.readFileToString(
                new File("target/test-output/ConllUReaderWriterTest-roundTrip/conllu-en-orig.conll"),
                "UTF-8").trim();
        assertTrue(IOUtils.contentEqualsIgnoreEOL(new StringReader(reference),
                new StringReader(actual)));
    }

    @Rule
    public DkproTestContext testContext = new DkproTestContext();
}

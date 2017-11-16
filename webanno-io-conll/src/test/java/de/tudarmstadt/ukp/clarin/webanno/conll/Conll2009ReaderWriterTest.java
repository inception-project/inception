/*
 * Copyright 2014
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

public class Conll2009ReaderWriterTest
{
    @Test
    public void test()
        throws Exception
    {
        CollectionReaderDescription reader = createReaderDescription(
                Conll2009Reader.class, 
                Conll2009Reader.PARAM_SOURCE_LOCATION, "src/test/resources/conll/2009",
                Conll2009Reader.PARAM_PATTERNS, "en-orig.conll");

        AnalysisEngineDescription writer = createEngineDescription(
                Conll2009Writer.class,
                Conll2009Writer.PARAM_TARGET_LOCATION, "target/test-output/Conll2009ReaderWriterTest-test",
                Conll2009Writer.PARAM_FILENAME_EXTENSION, ".conll",
                Conll2009Writer.PARAM_STRIP_EXTENSION, true);

        runPipeline(reader, writer);

        String reference = FileUtils.readFileToString(
                new File("src/test/resources/conll/2009/en-ref.conll"), "UTF-8")
                .trim();
        String actual = FileUtils.readFileToString(
                new File("target/test-output/Conll2009ReaderWriterTest-test/en-orig.conll"),
                "UTF-8").trim();
        assertTrue(IOUtils.contentEqualsIgnoreEOL(new StringReader(reference),
                new StringReader(actual)));
    }

    @Rule
    public DkproTestContext testContext = new DkproTestContext();
}

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
import static org.assertj.core.api.Assertions.assertThat;

import java.io.File;

import org.apache.commons.io.FileUtils;
import org.apache.uima.analysis_engine.AnalysisEngineDescription;
import org.apache.uima.collection.CollectionReaderDescription;
import org.junit.Rule;
import org.junit.Test;

import de.tudarmstadt.ukp.dkpro.core.testing.DkproTestContext;

public class ConllCoreNlpReaderWriterTest
{
    @Test
    public void roundTrip()
        throws Exception
    {
        CollectionReaderDescription reader = createReaderDescription(
                ConllCoreNlpReader.class, 
                ConllCoreNlpReader.PARAM_SOURCE_LOCATION, "src/test/resources/conll/corenlp",
                ConllCoreNlpReader.PARAM_PATTERNS, "en-orig.conll");

        AnalysisEngineDescription writer = createEngineDescription(
                ConllCoreNlpWriter.class,
                ConllCoreNlpWriter.PARAM_TARGET_LOCATION, "target/test-output/ConllCoreNlpReaderWriterTest-roundTrip",
                ConllCoreNlpWriter.PARAM_FILENAME_SUFFIX, ".conll",
                ConllCoreNlpWriter.PARAM_STRIP_EXTENSION, true,
                ConllCoreNlpWriter.PARAM_OVERWRITE, true);

        runPipeline(reader, writer);

        String reference = FileUtils.readFileToString(
                new File("src/test/resources/conll/corenlp/en-ref.conll"), "UTF-8")
                .trim();
        String actual = FileUtils.readFileToString(
                new File("target/test-output/ConllCoreNlpReaderWriterTest-roundTrip/en-orig.conll"),
                "UTF-8").trim();
        
        assertThat(actual).isEqualToNormalizingNewlines(reference);
    }

    @Rule
    public DkproTestContext testContext = new DkproTestContext();
}

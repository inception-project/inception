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

import static org.apache.uima.fit.factory.AnalysisEngineFactory.createPrimitiveDescription;
import static org.apache.uima.fit.factory.CollectionReaderFactory.createCollectionReader;
import static org.apache.uima.fit.pipeline.SimplePipeline.runPipeline;
import static org.junit.Assert.assertEquals;

import java.io.File;

import org.apache.commons.io.FileUtils;
import org.apache.uima.analysis_engine.AnalysisEngineDescription;
import org.apache.uima.collection.CollectionReader;
import org.junit.Test;


public class WebAnnoTsvReaderWriterTest
{
    @Test

    public void test()
        throws Exception
    {
        CollectionReader reader = createCollectionReader(WebannoTsvReader.class, WebannoTsvReader.PARAM_PATH,
                new File("src/test/resources/tsv/").getAbsolutePath(), WebannoTsvReader.PARAM_PATTERNS,
                new String[] { "[+]example.tsv" });

        AnalysisEngineDescription writer = createPrimitiveDescription(WebannoTsvWriter.class,
                WebannoTsvWriter.PARAM_TARGET_LOCATION, "target/test-output", WebannoTsvWriter.PARAM_STRIP_EXTENSION,
                true);

    /*    AnalysisEngineDescription dumper = createPrimitiveDescription(CASDumpWriter.class,
                CASDumpWriter.PARAM_OUTPUT_FILE, "target/test-output/dump.txt");*/

      //  runPipeline(reader, writer, dumper);
        runPipeline(reader, writer);

        String reference = FileUtils.readFileToString(new File(
                "src/test/resources/tsv/example.tsv"), "UTF-8");
        String actual = FileUtils.readFileToString(
                new File("target/test-output/example.tsv"), "UTF-8");
        assertEquals(reference, actual);
    }
}

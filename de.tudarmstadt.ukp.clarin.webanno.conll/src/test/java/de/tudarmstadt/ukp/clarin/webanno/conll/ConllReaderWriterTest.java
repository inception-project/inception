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
package de.tudarmstadt.ukp.clarin.webanno.conll;

import static org.junit.Assert.assertEquals;
import static org.uimafit.factory.AnalysisEngineFactory.createPrimitiveDescription;
import static org.uimafit.factory.CollectionReaderFactory.createCollectionReader;
import static org.uimafit.pipeline.SimplePipeline.runPipeline;

import java.io.File;

import org.apache.commons.io.FileUtils;
import org.apache.uima.analysis_engine.AnalysisEngineDescription;
import org.apache.uima.collection.CollectionReader;
import org.junit.Test;
import org.uimafit.component.xwriter.CASDumpWriter;

public class ConllReaderWriterTest
{
    @Test

    public void test()
        throws Exception
    {
        CollectionReader reader = createCollectionReader(ConllReader.class, ConllReader.PARAM_PATH,
                new File("src/test/resources/conll/").getAbsolutePath(), ConllReader.PARAM_PATTERNS,
                new String[] { "[+]fk003_2006_08_ZH1.conll10" });

        AnalysisEngineDescription writer = createPrimitiveDescription(ConllWriter.class,
                ConllWriter.PARAM_PATH, "target/test-output", ConllWriter.PARAM_STRIP_EXTENSION,
                true);

        AnalysisEngineDescription dumper = createPrimitiveDescription(CASDumpWriter.class,
                CASDumpWriter.PARAM_OUTPUT_FILE, "target/test-output/dump.txt");

        runPipeline(reader, writer, dumper);

        String reference = FileUtils.readFileToString(new File(
                "src/test/resources/conll/fk003_2006_08_ZH1.conll10"), "UTF-8");
        String actual = FileUtils.readFileToString(
                new File("target/test-output/fk003_2006_08_ZH1.conll"), "UTF-8");
        assertEquals(reference, actual);
    }
}

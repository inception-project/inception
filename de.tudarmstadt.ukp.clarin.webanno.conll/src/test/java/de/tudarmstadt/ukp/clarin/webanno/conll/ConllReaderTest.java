/*******************************************************************************
 * Copyright 2012
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *   http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 ******************************************************************************/
package de.tudarmstadt.ukp.clarin.webanno.conll;

import static org.uimafit.factory.AnalysisEngineFactory.createPrimitive;
import static org.uimafit.factory.CollectionReaderFactory.createCollectionReader;
import static org.uimafit.util.JCasUtil.select;

import java.io.File;
import java.io.IOException;

import org.apache.uima.UIMAException;
import org.apache.uima.analysis_engine.AnalysisEngine;
import org.apache.uima.cas.CAS;
import org.apache.uima.collection.CollectionReader;
import org.apache.uima.jcas.JCas;
import org.junit.Test;
import org.uimafit.component.xwriter.CASDumpWriter;
import org.uimafit.factory.JCasFactory;

import de.tudarmstadt.ukp.dkpro.core.api.segmentation.type.Sentence;
import de.tudarmstadt.ukp.dkpro.core.api.syntax.type.dependency.Dependency;


public class ConllReaderTest
{

    @Test
    public void test()
        throws UIMAException, IOException
    {


        try {

            CollectionReader reader = createCollectionReader(ConllReader.class, ConllReader.PARAM_PATH,
                    new File("src/test/resources/conll/").getAbsolutePath(), ConllReader.PARAM_PATTERNS,
                    new String[] { "[+]fk003_2006_08_ZH1.conll10" });

            CAS cas = JCasFactory.createJCas().getCas();
            reader.getNext(cas);
            JCas jCas = cas.getJCas();
            File outputFile = new File("target/paula_To_Cas_Output.txt");
            AnalysisEngine writer = createPrimitive(CASDumpWriter.class,
                    CASDumpWriter.PARAM_OUTPUT_FILE, outputFile.getPath());
            writer.process(jCas);

        }
        finally {

        }
    }
}

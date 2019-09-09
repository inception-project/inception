/*
 * Copyright 2019
 * Ubiquitous Knowledge Processing (UKP) Lab
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

import static org.apache.commons.io.FileUtils.readFileToString;
import static org.apache.uima.fit.factory.AnalysisEngineFactory.createEngine;
import static org.assertj.core.api.Assertions.assertThat;

import java.io.File;

import org.apache.uima.analysis_engine.AnalysisEngine;
import org.apache.uima.fit.factory.JCasFactory;
import org.apache.uima.jcas.JCas;
import org.junit.Rule;
import org.junit.Test;

import de.tudarmstadt.ukp.dkpro.core.api.metadata.type.DocumentMetaData;
import de.tudarmstadt.ukp.dkpro.core.api.segmentation.type.Sentence;
import de.tudarmstadt.ukp.dkpro.core.api.segmentation.type.Token;
import de.tudarmstadt.ukp.dkpro.core.testing.DkproTestContext;

public class ConllUWriterTest
{
    @Test
    public void thatLineBreaksDoNotBreakTheFormat() throws Exception
    {
        String target = "target/test-output/" + testContext.getTestOutputFolderName();

        JCas jcas = JCasFactory.createText("Test\ntest.");
        new Sentence(jcas, 0, 10).addToIndexes();
        new Token(jcas, 0, 4).addToIndexes();
        new Token(jcas, 5, 9).addToIndexes();
        new Token(jcas, 9, 10).addToIndexes();

        DocumentMetaData dmd = DocumentMetaData.create(jcas);
        dmd.setDocumentId("output");

        AnalysisEngine writer = createEngine(ConllUWriter.class, 
                ConllUWriter.PARAM_TARGET_LOCATION, target);

        writer.process(jcas);

        String reference = readFileToString(
                new File("src/test/resources/conll/u_v2/conllu-linebreaks.conll"), "UTF-8").trim();
        String actual = readFileToString(new File(target, "output.conll"), "UTF-8").trim();

        assertThat(actual).isEqualToNormalizingNewlines(reference);
    }
    
    @Rule
    public DkproTestContext testContext = new DkproTestContext();
}

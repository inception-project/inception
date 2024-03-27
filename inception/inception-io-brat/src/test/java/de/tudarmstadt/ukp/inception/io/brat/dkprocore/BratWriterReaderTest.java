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
package de.tudarmstadt.ukp.inception.io.brat.dkprocore;

import static org.apache.uima.fit.factory.AnalysisEngineFactory.createEngine;
import static org.apache.uima.fit.factory.CollectionReaderFactory.createReader;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;

import java.io.File;

import org.apache.uima.fit.factory.JCasFactory;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import de.tudarmstadt.ukp.dkpro.core.api.metadata.type.DocumentMetaData;
import de.tudarmstadt.ukp.dkpro.core.api.ner.type.NamedEntity;

public class BratWriterReaderTest
{
    @Test
    void thatShortTypedDataCanBeRead(@TempDir File aTempDir) throws Exception
    {
        var jcas = JCasFactory.createJCas();
        jcas.setDocumentText("John");

        var ne = new NamedEntity(jcas, 0, 4);
        ne.setValue("PER");
        ne.addToIndexes();

        var dmd = DocumentMetaData.create(jcas);
        dmd.setDocumentId("test.txt");

        var writer = createEngine( //
                BratWriter.class, //
                BratWriter.PARAM_TARGET_LOCATION, aTempDir, //
                BratWriter.PARAM_SHORT_TYPE_NAMES, true, //
                BratWriter.PARAM_SHORT_ATTRIBUTE_NAMES, true);

        writer.process(jcas);

        jcas.reset();

        var reader = createReader( //
                BratReader.class, //
                BratReader.PARAM_SOURCE_LOCATION, aTempDir, //
                BratReader.PARAM_PATTERNS, "*.ann");

        reader.getNext(jcas.getCas());

        assertThat(jcas.select(NamedEntity.class).asList()) //
                .extracting(NamedEntity::getCoveredText, NamedEntity::getValue) //
                .containsExactly(tuple("John", "PER"));
    }
}

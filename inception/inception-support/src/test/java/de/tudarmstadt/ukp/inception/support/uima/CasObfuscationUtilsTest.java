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
package de.tudarmstadt.ukp.inception.support.uima;

import static org.assertj.core.api.Assertions.assertThat;

import org.apache.uima.fit.factory.JCasFactory;
import org.apache.uima.jcas.tcas.Annotation;
import org.junit.jupiter.api.Test;

import de.tudarmstadt.ukp.dkpro.core.api.metadata.type.DocumentMetaData;

public class CasObfuscationUtilsTest
{
    @Test
    void thatObfuscationObfuscatesStrings() throws Exception
    {
        var jcas = JCasFactory.createJCas();
        jcas.setDocumentText("Hello World 1234!");
        jcas.setDocumentLanguage("en");

        var dmd = DocumentMetaData.create(jcas);
        dmd.setDocumentTitle("My Secret Title");
        dmd.setDocumentId("ID-12345");

        var ann = new Annotation(jcas, 0, 5);
        ann.addToIndexes();

        var obfCas = CasObfuscationUtils.obfuscate(jcas.getCas());

        assertThat(obfCas).isNotNull();
        assertThat(obfCas.getDocumentText()).isNotNull();
        assertThat(obfCas.getDocumentText().length()).isEqualTo(jcas.getDocumentText().length());
        assertThat(obfCas.getDocumentText()).isNotEqualTo(jcas.getDocumentText());
        assertThat(obfCas.getDocumentLanguage()).isNotEqualTo(jcas.getDocumentLanguage());

        var obfDmd = DocumentMetaData.get(obfCas);
        assertThat(obfDmd).isNotNull();
        assertThat(obfDmd.getDocumentTitle()).isNotEqualTo(dmd.getDocumentTitle());
        assertThat(obfDmd.getDocumentId()).isNotEqualTo(dmd.getDocumentId());

        var obfJCas = obfCas.getJCas();
        var annIter = obfJCas.getAnnotationIndex(Annotation.class).iterator();
        boolean found = false;
        while (annIter.hasNext()) {
            var a = annIter.next();
            if (a.getBegin() == 0 && a.getEnd() == 5) {
                found = true;
                break;
            }
        }
        assertThat(found).isTrue();
    }

}

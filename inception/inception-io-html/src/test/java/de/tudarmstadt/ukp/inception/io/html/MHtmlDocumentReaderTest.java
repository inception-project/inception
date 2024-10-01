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
package de.tudarmstadt.ukp.inception.io.html;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.apache.uima.fit.factory.CollectionReaderFactory.createReader;
import static org.apache.uima.fit.factory.JCasFactory.createJCas;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.contentOf;

import java.io.File;

import org.junit.jupiter.api.Test;

class MHtmlDocumentReaderTest
{
    @Test
    void testMHtmlWithUtf8Encoding() throws Exception
    {
        var baseName = "Exceptional_isomorphism_Wikipedia";
        var cas = createJCas();

        var sut = createReader( //
                MHtmlDocumentReader.class, //
                MHtmlDocumentReader.PARAM_SOURCE_LOCATION,
                "src/test/resources/mhtml/" + baseName + ".mhtml");

        sut.getNext(cas.getCas());

        assertThat(cas.getDocumentText()) //
                .isEqualTo(contentOf(new File("src/test/resources/mhtml/" + baseName + ".txt"),
                        UTF_8));
    }

    @Test
    void testMHtmlWithWindows1252Encoding() throws Exception
    {
        var baseName = "A_KAE";
        var cas = createJCas();

        var sut = createReader( //
                MHtmlDocumentReader.class, //
                MHtmlDocumentReader.PARAM_SOURCE_LOCATION,
                "src/test/resources/mhtml/" + baseName + ".mhtml");

        sut.getNext(cas.getCas());

        assertThat(cas.getDocumentText()) //
                .isEqualTo(contentOf(new File("src/test/resources/mhtml/" + baseName + ".txt"),
                        UTF_8));
    }
}

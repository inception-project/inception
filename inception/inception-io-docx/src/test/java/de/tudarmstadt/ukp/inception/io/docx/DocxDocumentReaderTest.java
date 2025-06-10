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
package de.tudarmstadt.ukp.inception.io.docx;

import static org.apache.uima.fit.factory.CollectionReaderFactory.createReader;
import static org.assertj.core.api.Assertions.assertThat;

import org.apache.uima.fit.factory.CasFactory;
import org.dkpro.core.api.xml.type.XmlElement;
import org.dkpro.core.api.xml.type.XmlTextNode;
import org.junit.jupiter.api.Test;

class DocxDocumentReaderTest
{
    @Test
    void test() throws Exception
    {
        var sut = createReader( //
                DocxDocumentReader.class, //
                DocxDocumentReader.PARAM_SOURCE_LOCATION, "classpath*:docx/text.docx");

        var cas = CasFactory.createCas();
        sut.getNext(cas);

        // CasIOUtils.save(cas, System.out, XMI_PRETTY);

        assertThat(cas.select(XmlElement.class).asList()).hasSize(15);
        assertThat(cas.select(XmlTextNode.class).filter(n -> n.getCaptured())).hasSize(1);
        assertThat(cas.select(XmlTextNode.class).filter(n -> !n.getCaptured())).hasSize(0);
        assertThat(cas.getDocumentText()).contains("This is a text.");
    }
}

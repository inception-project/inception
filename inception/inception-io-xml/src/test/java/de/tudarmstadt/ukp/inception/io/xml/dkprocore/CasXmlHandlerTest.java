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
package de.tudarmstadt.ukp.inception.io.xml.dkprocore;

import static de.tudarmstadt.ukp.inception.support.xml.XmlParserUtils.enableNamespaceSupport;
import static de.tudarmstadt.ukp.inception.support.xml.XmlParserUtils.newSaxParser;
import static de.tudarmstadt.ukp.inception.support.xml.XmlParserUtils.newSaxParserFactory;
import static java.nio.charset.StandardCharsets.UTF_8;
import static org.apache.commons.io.IOUtils.toInputStream;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;

import org.apache.uima.fit.factory.JCasFactory;
import org.apache.uima.jcas.JCas;
import org.dkpro.core.api.xml.type.XmlAttribute;
import org.dkpro.core.api.xml.type.XmlDocument;
import org.dkpro.core.api.xml.type.XmlElement;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import de.tudarmstadt.ukp.inception.support.xml.XmlParserUtils;

public class CasXmlHandlerTest
{
    private JCas jcas;
    private CasXmlHandler sut;

    @BeforeEach
    void setup() throws Exception
    {
        jcas = JCasFactory.createJCas();
        sut = new CasXmlHandler(jcas);
    }

    @Test
    void testWithDefaultNamespace() throws Exception
    {
        var xml = "<root xmlns='http://namespace.org'/>";

        var parser = XmlParserUtils.newSaxParser();
        parser.parse(toInputStream(xml, UTF_8), sut);

        assertThat(jcas.select(XmlElement.class).asList()) //
                .extracting(XmlElement::getUri, XmlElement::getLocalName, XmlElement::getQName) //
                .containsExactly(tuple(null, null, "root"));
        assertThat(jcas.select(XmlDocument.class).get().getRoot().getAttributes()) //
                .extracting(XmlAttribute::getUri, XmlAttribute::getLocalName,
                        XmlAttribute::getQName, XmlAttribute::getValue) //
                .containsExactly(tuple(null, "xmlns", "xmlns", "http://namespace.org"));
    }

    @Test
    void testWithPrefixedNamespace() throws Exception
    {
        var xml = "<ns:root xmlns:ns='http://namespace.org'/>";

        var parser = XmlParserUtils.newSaxParser();
        parser.parse(toInputStream(xml, UTF_8), sut);

        assertThat(jcas.select(XmlElement.class).asList()) //
                .extracting(XmlElement::getUri, XmlElement::getLocalName, XmlElement::getQName) //
                .containsExactly(tuple(null, null, "ns:root"));
        assertThat(jcas.select(XmlDocument.class).get().getRoot().getAttributes()) //
                .extracting(XmlAttribute::getUri, XmlAttribute::getLocalName,
                        XmlAttribute::getQName, XmlAttribute::getValue) //
                // Yes, the XML parser does set both localname and qname to the same value!
                .containsExactly(tuple(null, "xmlns:ns", "xmlns:ns", "http://namespace.org"));
    }

    @Test
    void testWithPrefixedNamespace_NS_enabled() throws Exception
    {
        var xml = "<ns:root xmlns:ns='http://namespace.org' x='true'/>";

        var parser = newSaxParser(enableNamespaceSupport(newSaxParserFactory()));
        parser.parse(toInputStream(xml, UTF_8), sut);

        assertThat(jcas.select(XmlElement.class).asList()) //
                .extracting(XmlElement::getUri, XmlElement::getLocalName, XmlElement::getQName) //
                .containsExactly(tuple("http://namespace.org", "root", "ns:root"));
        assertThat(jcas.select(XmlDocument.class).get().getRoot().getAttributes()) //
                .extracting(XmlAttribute::getUri, XmlAttribute::getLocalName,
                        XmlAttribute::getQName, XmlAttribute::getValue) //
                // Namespace-aware parsers filter out the xmlns attributes!
                .containsExactly(tuple(null, "x", "x", "true"));
    }
}

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

import static org.apache.uima.fit.util.FSCollectionFactory.createFSArray;
import static org.assertj.core.api.Assertions.assertThat;

import java.io.StringWriter;

import org.apache.uima.fit.factory.JCasFactory;
import org.apache.uima.jcas.JCas;
import org.dkpro.core.api.xml.type.XmlAttribute;
import org.dkpro.core.api.xml.type.XmlDocument;
import org.dkpro.core.api.xml.type.XmlElement;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import de.tudarmstadt.ukp.inception.support.xml.XmlEventLoggingFilter;
import de.tudarmstadt.ukp.inception.support.xml.XmlParserUtils;

class Cas2SaxEventsTest
{
    private JCas jcas;
    private StringWriter out;
    private Cas2SaxEvents sut;
    private XmlEventLoggingFilter loggingFilter;

    @BeforeEach
    void setup() throws Exception
    {
        jcas = JCasFactory.createJCas();
        out = new StringWriter();
        loggingFilter = new XmlEventLoggingFilter(XmlParserUtils.makeXmlSerializer(out));
        sut = new Cas2SaxEvents(loggingFilter);
    }

    @Test
    void testWithDefaultNamespace() throws Exception
    {
        jcas.setDocumentText("");

        var root = new XmlElement(jcas, 0, 0);
        var rootAttrXmlns = new XmlAttribute(jcas);
        rootAttrXmlns.setLocalName("xmlns");
        rootAttrXmlns.setQName("xmlns");
        rootAttrXmlns.setValue("http://namespace.org");
        root.setAttributes(createFSArray(jcas, rootAttrXmlns));
        root.setQName("root");
        jcas.addFsToIndexes(root);

        var doc = new XmlDocument(jcas, 0, 0);
        doc.setRoot(root);
        jcas.addFsToIndexes(doc);

        sut.setNamespaces(false);
        sut.process(jcas);

        assertThat(out.toString()).isEqualTo("<root xmlns=\"http://namespace.org\"/>");
        assertThat(loggingFilter.getEvents()).containsExactly( //
                "[START-DOCUMENT]", //
                "[START-ELEMENT [root] {}[]] [[xmlns]{}[xmlns]]=[http://namespace.org]", //
                "[END-ELEMENT [root] {}[]]", //
                "[END-DOCUMENT]");
    }

    @Test
    void testWithPrefixedNamespace() throws Exception
    {
        var root = new XmlElement(jcas, 0, 0);
        var rootAttrXmlns = new XmlAttribute(jcas);
        rootAttrXmlns.setLocalName("xmlns:ns");
        rootAttrXmlns.setQName("xmlns:ns");
        rootAttrXmlns.setValue("http://namespace.org");
        root.setAttributes(createFSArray(jcas, rootAttrXmlns));
        root.setQName("ns:root");
        jcas.addFsToIndexes(root);

        var doc = new XmlDocument(jcas, 0, 0);
        doc.setRoot(root);
        jcas.addFsToIndexes(doc);

        sut.setNamespaces(false);
        sut.process(jcas);

        assertThat(out.toString()).isEqualTo("<ns:root xmlns:ns=\"http://namespace.org\"/>");
        assertThat(loggingFilter.getEvents()).containsExactly( //
                "[START-DOCUMENT]", //
                "[START-ELEMENT [ns:root] {}[]] [[xmlns:ns]{}[xmlns:ns]]=[http://namespace.org]", //
                "[END-ELEMENT [ns:root] {}[]]", //
                "[END-DOCUMENT]");
    }

    @Test
    void testWithPrefixedNamespace_NS_enabled() throws Exception
    {
        var root = new XmlElement(jcas, 0, 0);
        var rootAttrXmlns = new XmlAttribute(jcas);
        rootAttrXmlns.setLocalName("ns");
        rootAttrXmlns.setQName("xmlns:ns");
        rootAttrXmlns.setValue("http://namespace.org");
        root.setAttributes(createFSArray(jcas, rootAttrXmlns));
        root.setUri("http://namespace.org");
        root.setLocalName("root");
        root.setQName("ns:root");
        jcas.addFsToIndexes(root);

        var doc = new XmlDocument(jcas, 0, 0);
        doc.setRoot(root);
        jcas.addFsToIndexes(doc);

        sut.setNamespaces(true);
        sut.process(jcas);

        assertThat(out.toString()).isEqualTo("<ns:root xmlns:ns=\"http://namespace.org\"/>");
        assertThat(loggingFilter.getEvents()).containsExactly( //
                "[START-DOCUMENT]", //
                "[START-PREFIX-MAPPING [ns] -> http://namespace.org]", //
                "[START-ELEMENT [ns:root] {http://namespace.org}[root]] [[xmlns:ns]{}[ns]]=[http://namespace.org]", //
                "[END-ELEMENT [ns:root] {http://namespace.org}[root]]", //
                "[END-PREFIX-MAPPING [ns]]", //
                "[END-DOCUMENT]");
    }
}

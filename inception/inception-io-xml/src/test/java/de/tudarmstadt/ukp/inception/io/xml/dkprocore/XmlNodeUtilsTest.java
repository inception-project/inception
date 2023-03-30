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

import static de.tudarmstadt.ukp.inception.io.xml.dkprocore.XmlNodeUtils.parseXmlStringToCas;
import static de.tudarmstadt.ukp.inception.io.xml.dkprocore.XmlNodeUtils.removeWithDescendantsFromTree;
import static de.tudarmstadt.ukp.inception.io.xml.dkprocore.XmlNodeUtils.selectElements;
import static de.tudarmstadt.ukp.inception.io.xml.dkprocore.XmlNodeUtils.serializeCasToXmlString;
import static java.util.Arrays.asList;
import static org.apache.uima.fit.util.FSCollectionFactory.createFSArray;
import static org.assertj.core.api.Assertions.assertThat;

import org.apache.uima.fit.factory.JCasFactory;
import org.apache.uima.jcas.tcas.Annotation;
import org.dkpro.core.api.xml.type.XmlDocument;
import org.dkpro.core.api.xml.type.XmlElement;
import org.dkpro.core.api.xml.type.XmlNode;
import org.junit.jupiter.api.Test;

class XmlNodeUtilsTest
{
    @Test
    void testRemoveFromTree() throws Exception
    {
        var text = "This is a test";

        var jcas = JCasFactory.createJCas();
        jcas.setDocumentText(text);

        var doc = new XmlDocument(jcas, 0, text.length());
        doc.addToIndexes();

        var root = new XmlElement(jcas, 0, text.length());
        root.addToIndexes();
        doc.setRoot(root);

        var el1 = new XmlElement(jcas, 0, 4);
        var el2 = new XmlElement(jcas, 5, 9);
        var el3 = new XmlElement(jcas, 10, 14);
        var el4 = new XmlElement(jcas, 5, 7);
        var el5 = new XmlElement(jcas, 8, 9);
        asList(el1, el2, el3).forEach(el -> el.setParent(root));
        asList(el4, el5).forEach(el -> el.setParent(el2));
        asList(el1, el2, el3, el4, el5).forEach(XmlElement::addToIndexes);
        root.setChildren(createFSArray(jcas, el1, el2, el3));
        el2.setChildren(createFSArray(jcas, el4, el5));

        assertThat(el1.getCoveredText()).isEqualTo("This");
        assertThat(el2.getCoveredText()).isEqualTo("is a");
        assertThat(el3.getCoveredText()).isEqualTo("test");
        assertThat(el4.getCoveredText()).isEqualTo("is");
        assertThat(el5.getCoveredText()).isEqualTo("a");

        XmlNodeUtils.removeFromTree(el2);

        assertThat(root.getChildren().toArray(new XmlNode[root.getChildren().size()]))
                .extracting(Annotation::getCoveredText) //
                .containsExactly("This", "is", "a", "test");
    }

    @Test
    void testRemoveWithDescendantsFromTree() throws Exception
    {
        var jcas = JCasFactory.createJCas();
        parseXmlStringToCas(jcas, "<root><e1>This <e2>is a</e2> test.</e1></root>");

        var el2 = jcas.select(XmlElement.class) //
                .filter(e -> e.getQName().equals("e1")) //
                .findFirst().get();

        removeWithDescendantsFromTree(el2);

        assertThat(serializeCasToXmlString(jcas)).isEqualTo("<root>This is a test.</root>");
    }

    @Test
    void testRemoveWithDescendantsFromTree2() throws Exception
    {
        var jcas = JCasFactory.createJCas();
        parseXmlStringToCas(jcas, "<root><e1>This <e2>is a</e2> test.</e1></root>");

        var el2 = jcas.select(XmlElement.class) //
                .filter(e -> e.getQName().equals("e2")) //
                .findFirst().get();

        removeWithDescendantsFromTree(el2);

        assertThat(serializeCasToXmlString(jcas))
                .isEqualTo("<root><e1>This is a test.</e1></root>");
    }

    @Test
    void testSelectElements() throws Exception
    {
        var jcas = JCasFactory.createJCas();
        parseXmlStringToCas(jcas, "<root><e1>This <e2>is a</e2> test.</e1></root>");

        var root = jcas.select(XmlDocument.class).get().getRoot();
        var result = selectElements(root, asList("root", "e1"));

        assertThat(result) //
                .extracting(XmlElement::getQName) //
                .containsExactly("e1");
    }
}

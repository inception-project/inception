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

import static de.tudarmstadt.ukp.inception.io.xml.dkprocore.XmlNodeUtils.containsXmlDocumentStructure;
import static de.tudarmstadt.ukp.inception.io.xml.dkprocore.XmlNodeUtils.dfs;
import static de.tudarmstadt.ukp.inception.io.xml.dkprocore.XmlNodeUtils.firstChild;
import static de.tudarmstadt.ukp.inception.io.xml.dkprocore.XmlNodeUtils.getAttributeValue;
import static de.tudarmstadt.ukp.inception.io.xml.dkprocore.XmlNodeUtils.getMandatoryAttributeValue;
import static de.tudarmstadt.ukp.inception.io.xml.dkprocore.XmlNodeUtils.hasAttributeValue;
import static de.tudarmstadt.ukp.inception.io.xml.dkprocore.XmlNodeUtils.hasAttributeWithValue;
import static de.tudarmstadt.ukp.inception.io.xml.dkprocore.XmlNodeUtils.parseXmlStringToCas;
import static de.tudarmstadt.ukp.inception.io.xml.dkprocore.XmlNodeUtils.removeWithDescendantsFromTree;
import static de.tudarmstadt.ukp.inception.io.xml.dkprocore.XmlNodeUtils.removeXmlDocumentStructure;
import static de.tudarmstadt.ukp.inception.io.xml.dkprocore.XmlNodeUtils.rootPath;
import static de.tudarmstadt.ukp.inception.io.xml.dkprocore.XmlNodeUtils.selectElements;
import static de.tudarmstadt.ukp.inception.io.xml.dkprocore.XmlNodeUtils.serializeCasToXmlString;
import static de.tudarmstadt.ukp.inception.io.xml.dkprocore.XmlNodeUtils.textContent;
import static de.tudarmstadt.ukp.inception.io.xml.dkprocore.XmlNodeUtils.transferXmlDocumentStructure;
import static java.util.Arrays.asList;
import static org.apache.uima.fit.util.FSCollectionFactory.createFSArray;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.uima.fit.factory.CasFactory;
import org.apache.uima.fit.factory.JCasFactory;
import org.apache.uima.jcas.JCas;
import org.apache.uima.jcas.tcas.Annotation;
import org.dkpro.core.api.xml.type.XmlAttribute;
import org.dkpro.core.api.xml.type.XmlDocument;
import org.dkpro.core.api.xml.type.XmlElement;
import org.dkpro.core.api.xml.type.XmlNode;
import org.dkpro.core.api.xml.type.XmlTextNode;
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

    @Test
    void testAttributeHelpers() throws Exception
    {
        var jcas = JCasFactory.createJCas();

        var el = new XmlElement(jcas, 0, 0);
        var attr = new XmlAttribute(jcas);
        attr.setQName("foo");
        attr.setValue("bar");
        el.setAttributes(createFSArray(jcas, attr));
        jcas.addFsToIndexes(el);

        assertThat(hasAttributeWithValue(el, "foo", "bar")).isTrue();
        assertThat(hasAttributeValue(el, "foo", "bar")).isTrue();

        assertThat(getAttributeValue(el, "foo")).isPresent().contains("bar");

        assertThat(getMandatoryAttributeValue(el, "foo")).isEqualTo("bar");

        assertThatThrownBy(() -> getMandatoryAttributeValue(el, "missing"))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void testTextContentDfsFirstChildRootPath() throws Exception
    {
        var jcas = JCasFactory.createJCas();
        parseXmlStringToCas(jcas, "<root><e1>This <e2>is a</e2> test.</e1></root>");

        var root = jcas.select(XmlDocument.class).get().getRoot();

        // firstChild
        var maybeE1 = firstChild(root, "e1");
        assertThat(maybeE1).isPresent();

        var e1 = maybeE1.get();

        // textContent
        assertThat(textContent(e1)).isEqualTo("This is a test.");

        // dfs contains text nodes
        assertThat(dfs(e1)).anyMatch(n -> n instanceof XmlTextNode);

        // rootPath for e2
        var e2 = jcas.select(XmlElement.class) //
                .filter(e -> e.getQName().equals("e2")) //
                .findFirst().get();
        assertThat(rootPath(e2)).containsExactly("root", "e1", "e2");
    }

    @Test
    void testContainsTransferAndRemoveXmlDocumentStructure() throws Exception
    {
        var source = JCasFactory.createJCas();
        parseXmlStringToCas(source, "<root><e1>abc</e1></root>");

        var target = CasFactory.createCas();

        // source contains XML structure
        assertThat(containsXmlDocumentStructure(source.getCas())).isTrue();

        int copied = transferXmlDocumentStructure(target, source.getCas());
        assertThat(copied).isGreaterThan(0);

        assertThat(containsXmlDocumentStructure(target)).isTrue();

        int removed = removeXmlDocumentStructure(target);
        assertThat(removed).isGreaterThan(0);

        assertThat(containsXmlDocumentStructure(target)).isFalse();
    }

    @Test
    void testAllPrefixMappings() throws Exception
    {
        var jcas = JCasFactory.createJCas();

        var root = new XmlElement(jcas, 0, 0);
        var attrDefault = new XmlAttribute(jcas);
        attrDefault.setQName("xmlns");
        attrDefault.setValue("urn:root");
        var attrARoot = new XmlAttribute(jcas);
        attrARoot.setQName("xmlns:a");
        attrARoot.setValue("urn:a-root");
        root.setAttributes(createFSArray(jcas, attrDefault, attrARoot));
        jcas.addFsToIndexes(root);

        var child = new XmlElement(jcas, 0, 0);
        var attrAChild = new XmlAttribute(jcas);
        attrAChild.setQName("xmlns:a");
        attrAChild.setValue("urn:a-child");
        var attrB = new XmlAttribute(jcas);
        attrB.setQName("xmlns:b");
        attrB.setValue("urn:b");
        child.setAttributes(createFSArray(jcas, attrAChild, attrB));
        child.setParent(root);
        jcas.addFsToIndexes(child);

        var grand = new XmlElement(jcas, 0, 0);
        grand.setParent(child);
        jcas.addFsToIndexes(grand);

        var mappings = XmlNodeUtils.allPrefixMappings(grand);
        assertThat(mappings).containsEntry("", "urn:root");
        assertThat(mappings).containsEntry("a", "urn:a-child");
        assertThat(mappings).containsEntry("b", "urn:b");
        assertThat(mappings.keySet()).hasSize(3);
    }

    @Test
    public void thatPrefixedQNameMatchesExpandedSelector() throws Exception
    {
        JCas jcas = JCasFactory.createJCas();

        var element = new XmlElement(jcas);
        element.setQName("m:math");
        element.setLocalName("math");

        var ns = Map.of("m", "http://www.w3.org/1998/Math/MathML");

        var selectors = List.of("{http://www.w3.org/1998/Math/MathML}math");

        assertTrue(XmlNodeUtils.matchesAny(element, ns, selectors));
        assertFalse(XmlNodeUtils.matchesAny(element, ns, List.of("math")));
    }

    @Test
    public void thatDefaultNamespaceMatchesExpandedSelector() throws Exception
    {
        var jcas = JCasFactory.createJCas();

        var element = new XmlElement(jcas);
        element.setQName("math");
        element.setLocalName("math");
        element.setUri("http://www.w3.org/1998/Math/MathML");

        var ns = new HashMap<String, String>();

        var selectors = List.of("{http://www.w3.org/1998/Math/MathML}math");

        assertTrue(XmlNodeUtils.matchesAny(element, ns, selectors));
    }
}

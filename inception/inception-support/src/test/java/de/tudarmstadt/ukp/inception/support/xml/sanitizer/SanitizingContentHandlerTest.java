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
package de.tudarmstadt.ukp.inception.support.xml.sanitizer;

import static de.tudarmstadt.ukp.inception.support.xml.XmlParserUtils.makeXmlSerializer;
import static de.tudarmstadt.ukp.inception.support.xml.XmlParserUtils.newSaxParser;
import static java.nio.charset.StandardCharsets.UTF_8;
import static java.util.Arrays.asList;
import static org.apache.commons.io.IOUtils.toInputStream;
import static org.assertj.core.api.Assertions.assertThat;

import java.io.StringWriter;
import java.util.List;
import java.util.Map;

import javax.xml.namespace.QName;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import de.tudarmstadt.ukp.inception.support.xml.DefaultHandlerToContentHandlerAdapter;

public class SanitizingContentHandlerTest
{
    static List<Arguments> builderVariants()
    {
        return asList( //
                Arguments.of("CASE-SENSITIVE", PolicyCollectionBuilder.caseSensitive()), //
                Arguments.of("CASE-INSENSITIVE", PolicyCollectionBuilder.caseInsensitive()));
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("builderVariants")
    void thatDefaultIsDroppingEverything(String aName, PolicyCollectionBuilder aBuilder)
        throws Exception
    {
        var buffer = new StringWriter();
        var policy = aBuilder.build();

        var sut = new SanitizingContentHandler(makeXmlSerializer(buffer), policy);

        sut.startDocument();
        sut.startElement("root");
        sut.startElement("child");
        sut.characters("text");
        sut.endElement("child");
        sut.endElement("root");
        sut.endDocument();

        assertThat(buffer.toString()).isEqualTo("    ");
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("builderVariants")
    void thatElementsCanBeDroppedSelectively(String aName, PolicyCollectionBuilder aBuilder)
        throws Exception
    {
        var buffer = new StringWriter();
        var policy = aBuilder //
                .defaultElementAction(ElementAction.PASS) //
                .defaultAttributeAction(AttributeAction.PASS) //
                .disallowElements("child") //
                .build();

        var sut = new SanitizingContentHandler(makeXmlSerializer(buffer), policy);

        sut.startDocument();
        sut.startElement("root");
        sut.startElement("child");
        sut.characters("text");
        sut.endElement("child");
        sut.endElement("root");
        sut.endDocument();

        assertThat(buffer.toString()).isEqualTo("<root>    </root>");
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("builderVariants")
    void thatElementsCanBeSkippedSelectively(String aName, PolicyCollectionBuilder aBuilder)
        throws Exception
    {
        var buffer = new StringWriter();
        var policy = aBuilder //
                .defaultElementAction(ElementAction.PASS) //
                .defaultAttributeAction(AttributeAction.PASS) //
                .skipElements("child") //
                .build();

        var sut = new SanitizingContentHandler(makeXmlSerializer(buffer), policy);

        sut.startDocument();
        sut.startElement("root");
        sut.startElement("child");
        sut.characters("text");
        sut.endElement("child");
        sut.endElement("root");
        sut.endDocument();

        assertThat(buffer.toString()).isEqualTo("<root>text</root>");
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("builderVariants")
    void thatChildElementsCanBePreservedSelectively(String aName, PolicyCollectionBuilder aBuilder)
        throws Exception
    {
        var buffer = new StringWriter();
        var policy = aBuilder //
                .allowElements("root", "child2") //
                .build();

        var sut = new SanitizingContentHandler(makeXmlSerializer(buffer), policy);

        sut.startDocument();
        sut.startElement("root");
        sut.startElement("child");
        sut.startElement("child2");
        sut.characters("text");
        sut.endElement("child2");
        sut.endElement("child");
        sut.endElement("root");
        sut.endDocument();

        assertThat(buffer.toString()).isEqualTo("<root><child2>text</child2></root>");
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("builderVariants")
    void thatChildElementsCanBePreservedSelectively2(String aName, PolicyCollectionBuilder aBuilder)
        throws Exception
    {
        var buffer = new StringWriter();
        var policy = aBuilder //
                .allowElements("root", "child2") //
                .disallowElements("child") //
                .build();

        var sut = new SanitizingContentHandler(makeXmlSerializer(buffer), policy);

        sut.startDocument();
        sut.startElement("root");
        sut.startElement("child");
        sut.startElement("child2");
        sut.characters("text");
        sut.endElement("child2");
        sut.endElement("child");
        sut.endElement("root");
        sut.endDocument();

        assertThat(buffer.toString()).isEqualTo("<root><child2>text</child2></root>");
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("builderVariants")
    void thatAttributesCanBeDroppedSelectively(String aName, PolicyCollectionBuilder aBuilder)
        throws Exception
    {
        var buffer = new StringWriter();
        var policy = aBuilder //
                .defaultElementAction(ElementAction.PASS) //
                .defaultAttributeAction(AttributeAction.PASS) //
                .disallowAttributes("attr1").onElements("child") //
                .disallowAttributes("attr1").globally() //
                .build();

        var sut = new SanitizingContentHandler(makeXmlSerializer(buffer), policy);

        sut.startDocument();
        sut.startElement("root", Map.of("attr1", "x"));
        sut.startElement("child", Map.of("attr1", "x"));
        sut.startElement("child", Map.of("attr2", "y"));
        sut.characters("text");
        sut.endElement("child");
        sut.endElement("child");
        sut.endElement("root");
        sut.endDocument();

        assertThat(buffer.toString())
                .isEqualTo("<root><child><child attr2=\"y\">text</child></child></root>");
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("builderVariants")
    void thatDisallowedAttributesAreDroppped(String aName, PolicyCollectionBuilder aBuilder)
        throws Exception
    {
        var root = new QName("root");
        var child = new QName("child");
        var attr1 = new QName("attr1");
        var attr2 = new QName("attr2");

        var buffer = new StringWriter();
        var policy = aBuilder //
                .allowElements(root) //
                .allowElements(child) //
                .allowAttributes(attr1).onElements(child) //
                .build();

        var sut = new SanitizingContentHandler(makeXmlSerializer(buffer), policy);

        sut.startDocument();
        sut.startElement(root);
        sut.startElement(child, Map.of(attr1, "x", attr2, "y"));
        sut.characters("text");
        sut.endElement(child);
        sut.endElement(root);
        sut.endDocument();

        assertThat(buffer.toString()).isEqualTo("<root><child attr1=\"x\">text</child></root>");
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("builderVariants")
    void thatPrunedBranchesAreDropped(String aName, PolicyCollectionBuilder aBuilder)
        throws Exception
    {
        var buffer = new StringWriter();
        var policy = aBuilder //
                .defaultElementAction(ElementAction.DROP) //
                .defaultAttributeAction(AttributeAction.DROP) //
                .allowElements("root", "child") //
                .pruneElements("prune") //
                .build();

        var sut = new SanitizingContentHandler(makeXmlSerializer(buffer), policy);

        sut.startDocument();
        sut.startElement("root");
        sut.startElement("prune");
        sut.startElement("child");
        sut.characters("text");
        sut.endElement("child");
        sut.endElement("prune");
        sut.endElement("root");
        sut.endDocument();

        assertThat(buffer.toString()).isEqualTo("<root>    </root>");
    }

    @Test
    void thatCaseInsensitiveModeWorks() throws Exception
    {
        var root = new QName("root");
        var child = new QName("child");
        var attr1 = new QName("attr1");

        var buffer = new StringWriter();
        var policy = PolicyCollectionBuilder.caseInsensitive() //
                .allowElements(root) //
                .allowElements(child) //
                .allowAttributes(attr1).onElements(child) //
                .build();

        var sut = new SanitizingContentHandler(makeXmlSerializer(buffer), policy);

        sut.startDocument();
        sut.startElement("ROOT");
        sut.startElement("child", Map.of("Attr1", "x", "aTTR2", "y"));
        sut.characters("text");
        sut.endElement("CHILD");
        sut.endElement("root");
        sut.endDocument();

        assertThat(buffer.toString()).isEqualTo("<ROOT><child Attr1=\"x\">text</child></ROOT>");
    }

    @Test
    void thatCaseSensitiveModeWorks() throws Exception
    {
        var root = new QName("ROOT");
        var child = new QName("child");
        var attr1 = new QName("attr1");

        var buffer = new StringWriter();
        var policy = PolicyCollectionBuilder.caseSensitive() //
                .allowElements(root) //
                .allowElements(child) //
                .allowAttributes(attr1).onElements(child) //
                .build();

        var sut = new SanitizingContentHandler(makeXmlSerializer(buffer), policy);

        sut.startDocument();
        sut.startElement("ROOT");
        sut.startElement("child", Map.of("attr1", "x", "aTTR1", "y"));
        sut.startElement("CHILD", Map.of("Attr1", "x", "aTTR2", "y"));
        sut.characters("text");
        sut.endElement("CHILD");
        sut.endElement("child");
        sut.endElement("root");
        sut.endDocument();

        assertThat(buffer.toString()).isEqualTo("<ROOT><child attr1=\"x\">    </child></ROOT>");
    }

    @Test
    void thatSanitizingDefaultXmlParserWorks() throws Exception
    {
        var root = new QName("http://namespace.org", "ROOT");

        var buffer = new StringWriter();
        var policy = PolicyCollectionBuilder.caseSensitive() //
                .allowElements(root) //
                .build();

        var sut = new SanitizingContentHandler(makeXmlSerializer(buffer), policy);

        var xml = "<ns:ROOT xmlns:ns='http://namespace.org'/>";

        var parser = newSaxParser();
        parser.parse(toInputStream(xml, UTF_8), new DefaultHandlerToContentHandlerAdapter<>(sut));

        assertThat(buffer.toString()).isEqualTo("<ns:ROOT xmlns:ns=\"http://namespace.org\"/>");
    }

    @Test
    void thatNamespaceDeclarationsPass() throws Exception
    {
        var root = new QName("http://namespace.org", "ROOT");

        var buffer = new StringWriter();
        var policy = PolicyCollectionBuilder.caseSensitive() //
                .allowElements(root) //
                .build();

        var sut = new SanitizingContentHandler(makeXmlSerializer(buffer), policy);

        var xml = "<ns:ROOT xmlns:ns='http://namespace.org' xmlns:other='otherNs' xmlns='default'/>";

        var parser = newSaxParser();
        parser.parse(toInputStream(xml, UTF_8), new DefaultHandlerToContentHandlerAdapter<>(sut));

        assertThat(buffer.toString()).isEqualTo(
                "<ns:ROOT xmlns:ns=\"http://namespace.org\" xmlns:other=\"otherNs\" xmlns=\"default\"/>");
    }
}

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
package de.tudarmstadt.ukp.inception.io.html.dkprocore;

import static org.apache.uima.fit.factory.AnalysisEngineFactory.createEngineDescription;
import static org.apache.uima.fit.factory.CollectionReaderFactory.createReader;
import static org.apache.uima.fit.factory.CollectionReaderFactory.createReaderDescription;
import static org.apache.uima.fit.util.JCasUtil.select;
import static org.assertj.core.api.Assertions.assertThat;
import static org.dkpro.core.testing.IOTestRunner.testOneWay;

import java.io.File;
import java.io.IOException;

import org.apache.uima.collection.CollectionReader;
import org.apache.uima.fit.factory.JCasFactory;
import org.apache.uima.jcas.JCas;
import org.custommonkey.xmlunit.XMLAssert;
import org.dkpro.core.io.xmi.XmiWriter;
import org.dkpro.core.testing.DkproTestContext;
import org.dkpro.core.testing.TestOptions;
import org.junit.Rule;
import org.junit.Test;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import de.tudarmstadt.ukp.dkpro.core.api.segmentation.type.Heading;
import de.tudarmstadt.ukp.dkpro.core.api.segmentation.type.Paragraph;
import de.tudarmstadt.ukp.inception.io.xml.dkprocore.XmlDocumentWriter;

public class HtmlDocumentReaderTest
{
    @Test
    public void testReadFileWithOnlyBody() throws Exception
    {
        JCas jcas = JCasFactory.createJCas();

        CollectionReader reader = createReader( //
                HtmlDocumentReader.class, //
                HtmlDocumentReader.PARAM_SOURCE_LOCATION, "src/test/resources/html/test.html",
                HtmlDocumentReader.PARAM_LANGUAGE, "en");

        reader.getNext(jcas.getCas());

        assertThat(jcas.getDocumentText()) //
                .isEqualTo(
                        " Heading  This is the first paragraph.   This is the second paragraph.  ");

        assertThat(select(jcas, Heading.class)) //
                .extracting(Heading::getCoveredText) //
                .containsExactly("Heading");

        assertThat(select(jcas, Paragraph.class)) //
                .extracting(Paragraph::getCoveredText) //
                .containsExactly("This is the first paragraph.", "This is the second paragraph.");
    }

    @Test
    public void testReadFileWithOnlyBodyAndWriteAsXml() throws Exception
    {
        testOneWay( //
                createReaderDescription( //
                        HtmlDocumentReader.class, //
                        HtmlDocumentReader.PARAM_LANGUAGE, "en", //
                        HtmlDocumentReader.PARAM_NORMALIZE_WHITESPACE, false),
                createEngineDescription( //
                        XmlDocumentWriter.class), //
                "html/test-document.xml", "html/test.html", //
                new TestOptions().resultAssertor(this::assertXmlEquals));
    }

    @Test
    public void testReadFileWithOnlyBodyAndWriteAsXmi() throws Exception
    {
        testOneWay( //
                createReaderDescription( //
                        HtmlDocumentReader.class, //
                        HtmlDocumentReader.PARAM_LANGUAGE, "en"), //
                createEngineDescription(XmiWriter.class), //
                "html/test-document.xmi", "html/test.html");
    }

    @Test
    public void testReadFileWithHead() throws Exception
    {
        JCas jcas = JCasFactory.createJCas();

        CollectionReader reader = createReader( //
                HtmlDocumentReader.class, //
                HtmlDocumentReader.PARAM_SOURCE_LOCATION,
                "src/test/resources/html/test-with-head.html", //
                HtmlDocumentReader.PARAM_LANGUAGE, "en");

        reader.getNext(jcas.getCas());

        assertThat(jcas.getDocumentText()) //
                .isEqualTo(
                        " Heading  This is the first paragraph.   This is the second paragraph.  ");

        assertThat(select(jcas, Heading.class)) //
                .extracting(Heading::getCoveredText) //
                .containsExactly("Heading");

        assertThat(select(jcas, Paragraph.class)) //
                .extracting(Paragraph::getCoveredText) //
                .containsExactly("This is the first paragraph.", "This is the second paragraph.");
    }

    @Test
    public void testReadFileWithHeadAndWriteAsXmi() throws Exception
    {
        testOneWay( //
                createReaderDescription( //
                        HtmlDocumentReader.class, //
                        HtmlDocumentReader.PARAM_LANGUAGE, "en"), //
                createEngineDescription(XmiWriter.class), //
                "html/test-with-head.xmi", "html/test-with-head.html");
    }

    @Test
    public void testReadFileWithHeadAndDoctypeAndWriteAsXmi() throws Exception
    {
        testOneWay( //
                createReaderDescription( //
                        HtmlDocumentReader.class, //
                        HtmlDocumentReader.PARAM_LANGUAGE, "en"), //
                createEngineDescription(XmiWriter.class), //
                "html/test-with-head-and-doctype.xmi", "html/test-with-head-and-doctype.html");
    }

    private void assertXmlEquals(File expected, File actual)
    {
        try {
            XMLAssert.assertXMLEqual(new InputSource(expected.getPath()),
                    new InputSource(actual.getPath()));
        }
        catch (SAXException | IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Rule
    public DkproTestContext testContext = new DkproTestContext();
}

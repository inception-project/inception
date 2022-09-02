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

import static de.tudarmstadt.ukp.dkpro.core.api.segmentation.TrimUtils.trim;
import static de.tudarmstadt.ukp.inception.io.html.dkprocore.internal.JSoupUtil.appendNormalisedText;
import static de.tudarmstadt.ukp.inception.io.html.dkprocore.internal.JSoupUtil.lastCharIsWhitespace;
import static org.dkpro.core.api.parameter.ComponentParameters.DEFAULT_ENCODING;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.io.IOUtils;
import org.apache.uima.UimaContext;
import org.apache.uima.cas.CAS;
import org.apache.uima.collection.CollectionException;
import org.apache.uima.fit.descriptor.ConfigurationParameter;
import org.apache.uima.fit.descriptor.MimeTypeCapability;
import org.apache.uima.fit.descriptor.ResourceMetaData;
import org.apache.uima.fit.descriptor.TypeCapability;
import org.apache.uima.jcas.JCas;
import org.apache.uima.resource.ResourceInitializationException;
import org.dkpro.core.api.io.JCasResourceCollectionReader_ImplBase;
import org.dkpro.core.api.parameter.ComponentParameters;
import org.dkpro.core.api.parameter.MimeTypes;
import org.dkpro.core.api.resources.CompressionUtils;
import org.dkpro.core.api.xml.type.XmlElement;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Attribute;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.nodes.Node;
import org.jsoup.nodes.TextNode;
import org.jsoup.select.NodeTraversor;
import org.jsoup.select.NodeVisitor;
import org.xml.sax.helpers.AttributesImpl;

import com.ibm.icu.text.CharsetDetector;

import de.tudarmstadt.ukp.dkpro.core.api.segmentation.type.Div;
import de.tudarmstadt.ukp.dkpro.core.api.segmentation.type.Heading;
import de.tudarmstadt.ukp.dkpro.core.api.segmentation.type.Paragraph;
import eu.openminted.share.annotations.api.DocumentationResource;

/**
 * Reads the contents of a given URL and strips the HTML. Returns the textual contents. Also
 * recognizes headings and paragraphs.
 */
@ResourceMetaData(name = "HTML Reader")
@DocumentationResource("${docbase}/format-reference.html#format-${command}")
@MimeTypeCapability({ MimeTypes.APPLICATION_XHTML, MimeTypes.TEXT_HTML })
@TypeCapability(outputs = { //
        "de.tudarmstadt.ukp.dkpro.core.api.metadata.type.DocumentMetaData",
        "de.tudarmstadt.ukp.dkpro.core.api.segmentation.type.Heading",
        "de.tudarmstadt.ukp.dkpro.core.api.segmentation.type.Paragraph",
        "org.dkpro.core.api.xml.type.XmlAttribute", "org.dkpro.core.api.xml.type.XmlDocument",
        "org.dkpro.core.api.xml.type.XmlElement", "org.dkpro.core.api.xml.type.XmlNode",
        "org.dkpro.core.api.xml.type.XmlTextNode" })
public class HtmlDocumentReader
    extends JCasResourceCollectionReader_ImplBase
{
    /**
     * Automatically detect encoding.
     *
     * @see CharsetDetector
     */
    public static final String ENCODING_AUTO = "auto";

    /**
     * Name of configuration parameter that contains the character encoding used by the input files.
     */
    public static final String PARAM_SOURCE_ENCODING = ComponentParameters.PARAM_SOURCE_ENCODING;
    @ConfigurationParameter(name = PARAM_SOURCE_ENCODING, defaultValue = DEFAULT_ENCODING)
    private String sourceEncoding;

    /**
     * Normalize whitespace.
     */
    public static final String PARAM_NORMALIZE_WHITESPACE = "normalizeWhitespace";
    @ConfigurationParameter(name = PARAM_NORMALIZE_WHITESPACE, defaultValue = "true")
    private boolean normalizeWhitespace;

    private Map<String, Integer> mappings = new HashMap<>();

    @Override
    public void initialize(UimaContext aContext) throws ResourceInitializationException
    {
        super.initialize(aContext);

        mappings.put("h1", Heading.type);
        mappings.put("h2", Heading.type);
        mappings.put("h3", Heading.type);
        mappings.put("h4", Heading.type);
        mappings.put("h5", Heading.type);
        mappings.put("h6", Heading.type);
        mappings.put("p", Paragraph.type);
    }

    @Override
    public void getNext(JCas aJCas) throws IOException, CollectionException
    {
        Resource res = nextFile();
        initCas(aJCas, res);

        CAS cas = aJCas.getCas();

        String html;
        try (InputStream is = new BufferedInputStream(
                CompressionUtils.getInputStream(res.getLocation(), res.getInputStream()))) {

            if (ENCODING_AUTO.equals(sourceEncoding)) {
                CharsetDetector detector = new CharsetDetector();
                html = IOUtils.toString(detector.getReader(is, null));
            }
            else {
                html = IOUtils.toString(is, sourceEncoding);
            }
        }

        Document doc = Jsoup.parse(html);

        CasXmlHandler handler = new CasXmlHandler(aJCas);

        NodeVisitor visitor = new NodeVisitor()
        {
            @Override
            public void head(Node node, int depth)
            {
                try {
                    if (node instanceof Document) {
                        handler.startDocument();
                        handler.captureText(false);
                    }
                    else if (node instanceof TextNode) {
                        TextNode textNode = (TextNode) node;
                        StringBuilder buffer = new StringBuilder();
                        if (normalizeWhitespace) {
                            appendNormalisedText(buffer, textNode);
                        }
                        else {
                            buffer.append(textNode.getWholeText());
                        }
                        char[] text = buffer.toString().toCharArray();
                        handler.characters(text, 0, text.length);
                    }
                    else if (node instanceof Element) {
                        Element element = (Element) node;
                        if (handler.getText().length() > 0
                                && (element.isBlock() || element.nodeName().equals("br"))
                                && !lastCharIsWhitespace(handler.getText())) {
                            char[] text = " ".toCharArray();
                            handler.characters(text, 0, text.length);
                        }

                        AttributesImpl attributes = new AttributesImpl();

                        if (element.attributes() != null) {
                            for (Attribute attr : element.attributes()) {
                                attributes.addAttribute("", "", attr.getKey(), "CDATA",
                                        attr.getValue());
                            }
                        }

                        if ("body".equals(element.tagName())) {
                            handler.captureText(true);
                        }

                        handler.startElement("", "", element.tagName(), attributes);
                    }
                }
                catch (Exception e) {
                    throw new RuntimeException(e);
                }
            }

            @Override
            public void tail(Node node, int depth)
            {
                try {
                    if (node instanceof Document) {
                        handler.endDocument();
                    }
                    else if (node instanceof Element) {
                        Element element = (Element) node;

                        // Fetch the current element
                        XmlElement elementFS = handler.getCurrentElement();

                        // Close the current element so that it gets its end offset
                        handler.endElement("", "", element.tagName());

                        if ("body".equals(element.tagName())) {
                            handler.captureText(false);
                        }

                        Integer type = mappings.get(node.nodeName());
                        if (type != null) {
                            int[] span = { elementFS.getBegin(), elementFS.getEnd() };
                            trim(handler.getText(), span);
                            Div div = (Div) cas.createAnnotation(aJCas.getCasType(type), span[0],
                                    span[1]);
                            div.setDivType(node.nodeName());
                            div.addToIndexes();
                        }
                    }
                }
                catch (Exception e) {
                    throw new RuntimeException(e);
                }
            }
        };

        NodeTraversor.traverse(visitor, doc);
    }
}

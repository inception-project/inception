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

import static de.tudarmstadt.ukp.inception.io.html.dkprocore.internal.JSoupUtil.appendNormalisedText;
import static de.tudarmstadt.ukp.inception.io.html.dkprocore.internal.JSoupUtil.lastCharIsWhitespace;
import static de.tudarmstadt.ukp.inception.support.text.TrimUtils.trim;

import java.util.Map;

import org.apache.uima.jcas.JCas;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.nodes.Node;
import org.jsoup.nodes.TextNode;
import org.jsoup.select.NodeVisitor;
import org.xml.sax.helpers.AttributesImpl;

import de.tudarmstadt.ukp.dkpro.core.api.segmentation.type.Div;
import de.tudarmstadt.ukp.inception.io.xml.dkprocore.CasXmlHandler;

public class CasXmlNodeVisitor
    implements NodeVisitor
{
    private final CasXmlHandler handler;
    private final JCas jcas;
    private final boolean normalizeWhitespace;
    private Map<String, Integer> mappings;

    public CasXmlNodeVisitor(JCas aJCas, boolean aNormalizeWhitespace)
    {
        jcas = aJCas;
        handler = new CasXmlHandler(aJCas);
        normalizeWhitespace = aNormalizeWhitespace;
    }

    public void setMappings(Map<String, Integer> aMappings)
    {
        mappings = aMappings;
    }

    @Override
    public void head(Node node, int depth)
    {
        try {
            if (node instanceof Document) {
                handler.startDocument();
                handler.captureText(false);
            }
            else if (node instanceof TextNode textNode) {
                var buffer = new StringBuilder();
                if (normalizeWhitespace) {
                    appendNormalisedText(buffer, textNode);
                }
                else {
                    buffer.append(textNode.getWholeText());
                }
                char[] text = buffer.toString().toCharArray();
                handler.characters(text, 0, text.length);
            }
            else if (node instanceof Element element) {
                // Insert a space character after <BR> elements to give the tokenizer an opportunity
                // to insert a token here.
                if (!handler.getText().isEmpty()
                        && (element.isBlock() || "br".equalsIgnoreCase(element.nodeName()))
                        && !lastCharIsWhitespace(handler.getText())) {
                    var text = " ".toCharArray();
                    handler.characters(text, 0, text.length);
                }

                var attributes = new AttributesImpl();

                if (element.attributes() != null) {
                    for (var attr : element.attributes()) {
                        attributes.addAttribute("", "", attr.getKey(), "CDATA", attr.getValue());
                    }
                }

                if ("body".equalsIgnoreCase(element.tagName())) {
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
            else if (node instanceof Element element) {
                // Fetch the current element
                var elementFS = handler.getCurrentElement();

                // Close the current element so that it gets its end offset
                handler.endElement("", "", element.tagName());

                if ("body".equalsIgnoreCase(element.tagName())) {
                    handler.captureText(false);
                }

                if (mappings != null) {
                    var type = mappings.get(node.nodeName());
                    if (type != null) {
                        int[] span = { elementFS.getBegin(), elementFS.getEnd() };
                        trim(handler.getText(), span);
                        Div div = (Div) jcas.getCas().createAnnotation(jcas.getCasType(type),
                                span[0], span[1]);
                        div.setDivType(node.nodeName());
                        div.addToIndexes();
                    }
                }
            }
        }
        catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}

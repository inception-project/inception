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

import static org.apache.commons.lang3.StringUtils.defaultString;
import static org.apache.commons.lang3.StringUtils.startsWith;
import static org.apache.uima.fit.util.JCasUtil.selectSingle;

import java.util.LinkedHashMap;
import java.util.Map;

import org.apache.uima.jcas.JCas;
import org.dkpro.core.api.xml.type.XmlDocument;
import org.dkpro.core.api.xml.type.XmlElement;
import org.dkpro.core.api.xml.type.XmlNode;
import org.dkpro.core.api.xml.type.XmlTextNode;
import org.xml.sax.ContentHandler;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.AttributesImpl;

import de.tudarmstadt.ukp.inception.support.xml.ContentHandlerAdapter;

public class Cas2SaxEvents
{
    private static final String XMLNS = "xmlns";
    private static final String XMLNS_PREFIX = "xmlns:";

    protected final ContentHandlerAdapter handler;
    private final LinkedHashMap<String, String> namespaceMappings = new LinkedHashMap<>();

    private boolean namespaces = false;

    public Cas2SaxEvents(ContentHandler aHandler)
    {
        handler = new ContentHandlerAdapter(aHandler);
    }

    public void setNamespaces(boolean aNamespaces)
    {
        namespaces = aNamespaces;
    }

    public void process(JCas aJCas) throws SAXException
    {
        var doc = selectSingle(aJCas, XmlDocument.class);

        process(doc);
    }

    public void process(XmlDocument aDoc) throws SAXException
    {
        namespaceMappings.clear();
        handler.startDocument();

        if (aDoc.getRoot() == null) {
            throw new SAXException("Document has no root element");
        }

        process(aDoc.getRoot());

        handler.endDocument();
    }

    public void process(XmlElement aElement) throws SAXException
    {
        var localMappings = new LinkedHashMap<String, String>();

        if (namespaces) {
            for (var xmlns : prefixMappings(aElement).entrySet()) {
                var oldValue = namespaceMappings.put(xmlns.getKey(), xmlns.getValue());
                if (oldValue == null) {
                    localMappings.put(xmlns.getKey(), xmlns.getValue());
                    handler.startPrefixMapping(xmlns.getKey(), xmlns.getValue());
                }
            }
        }

        var attributes = attributes(aElement);
        var uri = defaultString(aElement.getUri());
        var localName = defaultString(aElement.getLocalName());
        var qName = defaultString(aElement.getQName());

        handler.startElement(uri, localName, qName, attributes);

        processChildren(aElement);

        handler.endElement(uri, localName, qName);

        if (namespaces) {
            for (String xmlns : localMappings.keySet()) {
                handler.endPrefixMapping(xmlns);
            }
        }
    }

    public void processChildren(XmlElement aElement) throws SAXException
    {
        if (aElement.getChildren() != null) {
            for (XmlNode child : aElement.getChildren()) {
                if (child instanceof XmlElement) {
                    process((XmlElement) child);
                }
                else if (child instanceof XmlTextNode) {
                    process((XmlTextNode) child);
                }
            }
        }
    }

    public Map<String, String> prefixMappings(XmlElement aElement)
    {
        var mappings = new LinkedHashMap<String, String>();
        if (aElement.getAttributes() != null) {
            for (var attr : aElement.getAttributes()) {
                if (XMLNS.equals(attr.getQName())) {
                    mappings.put("", attr.getValue());
                }
                if (startsWith(attr.getQName(), XMLNS_PREFIX)) {
                    mappings.put(attr.getQName().substring(XMLNS_PREFIX.length()), attr.getValue());
                }
            }
        }
        return mappings;
    }

    public AttributesImpl attributes(XmlElement aElement)
    {
        // FIXME: SAX parsers would skip xmlns attributes if namespace support is enabled. Maybe
        // we should do the same?

        var attrs = new AttributesImpl();
        if (aElement.getAttributes() != null) {
            for (var attr : aElement.getAttributes()) {
                attrs.addAttribute(defaultString(attr.getUri()), defaultString(attr.getLocalName()),
                        defaultString(attr.getQName()), defaultString(attr.getValueType(), "CDATA"),
                        defaultString(attr.getValue()));
            }
        }
        return attrs;
    }

    public void process(XmlTextNode aChild) throws SAXException
    {
        char[] text;

        if (aChild.getCaptured()) {
            text = aChild.getCoveredText().toCharArray();
        }
        else {
            text = aChild.getText().toCharArray();
        }

        handler.characters(text, 0, text.length);
    }
}

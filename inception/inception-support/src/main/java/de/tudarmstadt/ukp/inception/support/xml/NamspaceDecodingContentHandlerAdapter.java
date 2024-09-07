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
package de.tudarmstadt.ukp.inception.support.xml;

import static org.apache.commons.lang3.StringUtils.startsWith;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Stack;

import javax.xml.XMLConstants;
import javax.xml.namespace.QName;

import org.apache.commons.lang3.StringUtils;
import org.xml.sax.Attributes;
import org.xml.sax.ContentHandler;
import org.xml.sax.SAXException;

public abstract class NamspaceDecodingContentHandlerAdapter
    extends ContentHandlerAdapter
{
    private static final String XMLNS = "xmlns";
    private static final String XMLNS_PREFIX = "xmlns:";

    private final Stack<Frame> stack;

    private final Map<String, String> namespaceMappings = new LinkedHashMap<>();

    public NamspaceDecodingContentHandlerAdapter(ContentHandler aDelegate)
    {
        super(aDelegate);
        stack = new Stack<>();
        namespaceMappings.put(XMLConstants.XML_NS_PREFIX, XMLConstants.XML_NS_URI);
    }

    @Override
    public void startDocument() throws SAXException
    {
        stack.clear();
        namespaceMappings.clear();
        namespaceMappings.put(XMLConstants.XML_NS_PREFIX, XMLConstants.XML_NS_URI);
        super.startDocument();
    }

    @Override
    public void startPrefixMapping(String aPrefix, String aUri) throws SAXException
    {
        namespaceMappings.put(aPrefix, aUri);

        super.startPrefixMapping(aPrefix, aUri);
    }

    @Override
    public void endPrefixMapping(String aPrefix) throws SAXException
    {
        namespaceMappings.remove(aPrefix);

        super.endPrefixMapping(aPrefix);
    }

    @Override
    public void startElement(String aUri, String aLocalName, String aQName, Attributes aAtts)
        throws SAXException
    {
        var localNamespaces = new LinkedHashMap<String, String>();
        for (var nsDecl : prefixMappings(aAtts).entrySet()) {
            var oldValue = namespaceMappings.put(nsDecl.getKey(), nsDecl.getValue());
            if (oldValue == null) {
                localNamespaces.put(nsDecl.getKey(), nsDecl.getValue());
            }
        }

        var element = toQName(aUri, aLocalName, aQName);

        stack.push(new Frame(element, localNamespaces));

        super.startElement(aUri, aLocalName, aQName, aAtts);
    }

    private Map<String, String> prefixMappings(Attributes aAtts)
    {
        var mappings = new LinkedHashMap<String, String>();
        if (aAtts != null) {
            for (int i = 0; i < aAtts.getLength(); i++) {
                String qName = aAtts.getQName(i);
                if (XMLNS.equals(qName)) {
                    mappings.put("", aAtts.getValue(i));
                }
                if (startsWith(qName, XMLNS_PREFIX)) {
                    mappings.put(qName.substring(XMLNS_PREFIX.length()), aAtts.getValue(i));
                }
            }
        }
        return mappings;
    }

    @Override
    public void endElement(String aUri, String aLocalName, String aQName) throws SAXException
    {
        var frame = stack.pop();
        super.endElement(frame.element.getNamespaceURI(), frame.element.getLocalPart(), aQName);
        frame.namespaces.keySet().forEach(namespaceMappings::remove);
    }

    private static final class Frame
    {
        final QName element;
        final Map<String, String> namespaces;

        public Frame(QName aElement, Map<String, String> aLocalNamespaces)
        {
            element = aElement;

            if (aLocalNamespaces != null && !aLocalNamespaces.isEmpty()) {
                namespaces = aLocalNamespaces;
            }
            else {
                namespaces = Collections.emptyMap();
            }
        }

        @Override
        public String toString()
        {
            return "[" + element.getLocalPart() + "]";
        }
    }

    protected QName toQName(String aUri, String aLocalName, String aQName)
    {
        String prefix = XMLConstants.DEFAULT_NS_PREFIX;
        String localName = aLocalName;

        // Workaround bug: localname may contain prefix
        if (localName != null) {
            var li = localName.indexOf(':');
            if (li >= 0) {
                localName = localName.substring(li + 1);
            }
        }

        var qi = aQName.indexOf(':');
        if (qi >= 0) {
            prefix = aQName.substring(0, qi);
        }

        if (StringUtils.isEmpty(localName)) {
            if (qi >= 0) {
                localName = aQName.substring(qi + 1, aQName.length());
            }
            else {
                localName = aQName;
            }
        }

        String uri = aUri;
        if (StringUtils.isEmpty(uri)) {
            uri = namespaceMappings.getOrDefault(prefix, XMLConstants.NULL_NS_URI);
        }

        return new QName(uri, localName, prefix);
    }
}

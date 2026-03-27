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

import static de.tudarmstadt.ukp.inception.io.xml.dkprocore.Cas2SaxEvents.ProcessElementOptions.SKIP_NAMESPACES;
import static de.tudarmstadt.ukp.inception.io.xml.dkprocore.XmlNodeUtils.prefixMappings;
import static java.util.Collections.unmodifiableMap;
import static org.apache.commons.lang3.StringUtils.defaultString;
import static org.apache.uima.fit.util.JCasUtil.selectSingle;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;

import org.apache.commons.lang3.ArrayUtils;
import org.apache.uima.jcas.JCas;
import org.dkpro.core.api.xml.type.XmlDocument;
import org.dkpro.core.api.xml.type.XmlElement;
import org.dkpro.core.api.xml.type.XmlTextNode;
import org.xml.sax.ContentHandler;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.AttributesImpl;

import de.tudarmstadt.ukp.inception.support.xml.ContentHandlerAdapter;

public class Cas2SaxEvents
{
    public enum ProcessElementOptions
    {
        SKIP_NAMESPACES
    }

    private final Map<String, String> namespaceMappings = new LinkedHashMap<>();

    protected final ContentHandlerAdapter handler;

    private boolean namespaces = false;

    public Cas2SaxEvents(ContentHandler aHandler)
    {
        handler = new ContentHandlerAdapter(aHandler);
    }

    public void setNamespaces(boolean aNamespaces)
    {
        namespaces = aNamespaces;
    }

    Map<String, String> getNamespaceMappings()
    {
        return unmodifiableMap(namespaceMappings);
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

    public void process(XmlElement aElement, ProcessElementOptions... aOptions) throws SAXException
    {
        var skipNamespaces = ArrayUtils.contains(aOptions, SKIP_NAMESPACES);

        var localMappings = skipNamespaces ? null : openNamespaces(aElement);

        var attributes = attributes(aElement);
        var uri = defaultString(aElement.getUri());
        var localName = defaultString(aElement.getLocalName());
        var qName = defaultString(aElement.getQName());

        handler.startElement(uri, localName, qName, attributes);

        processChildren(aElement);

        handler.endElement(uri, localName, qName);

        if (localMappings != null) {
            closeNamespaces(localMappings);
        }
    }

    protected void closeNamespaces(Map<String, String> localMappings) throws SAXException
    {
        if (namespaces) {
            for (var xmlns : localMappings.entrySet()) {
                var prefix = xmlns.getKey();
                var previous = xmlns.getValue();

                namespaceMappings.remove(prefix);
                handler.endPrefixMapping(prefix);

                if (previous != null) {
                    // Restore the previous mapping in our internal map so that subsequent
                    // siblings resolve prefixes correctly. Do NOT emit a startPrefixMapping
                    // here because the handler already received the original start event
                    // when the previous mapping was introduced.
                    namespaceMappings.put(prefix, previous);
                }
            }
        }
    }

    protected Map<String, String> openNamespaces(XmlElement aElement) throws SAXException
    {
        var localMappings = new LinkedHashMap<String, String>();

        if (namespaces) {
            for (var xmlns : prefixMappings(aElement).entrySet()) {
                var prefix = xmlns.getKey();
                var newValue = xmlns.getValue();
                var oldValue = namespaceMappings.get(prefix);

                if (oldValue == null) {
                    // Newly introduced mapping
                    namespaceMappings.put(prefix, newValue);
                    localMappings.put(prefix, null);
                    handler.startPrefixMapping(prefix, newValue);
                }
                else if (!oldValue.equals(newValue)) {
                    // Redeclaration/override: remember previous value so we can restore it
                    // when closing the element, and emit a startPrefixMapping for the
                    // new value so the SAX handler sees the change.
                    namespaceMappings.put(prefix, newValue);
                    localMappings.put(prefix, oldValue);
                    handler.startPrefixMapping(prefix, newValue);
                }
                // If oldValue equals newValue then nothing to do.
            }
        }
        return localMappings;
    }

    public void processChildren(XmlElement aElement) throws SAXException
    {
        if (aElement.getChildren() != null) {
            for (var child : aElement.getChildren()) {
                if (child instanceof XmlElement childElement) {
                    process(childElement);
                }
                else if (child instanceof XmlTextNode childTextNode) {
                    process(childTextNode);
                }
            }
        }
    }

    public AttributesImpl attributes(XmlElement aElement)
    {
        return XmlNodeUtils.attributes(aElement);
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

    public boolean matchesAny(XmlElement aElement, Collection<String> aSelectors)
    {
        return XmlNodeUtils.matchesAny(aElement, namespaceMappings, aSelectors);
    }
}

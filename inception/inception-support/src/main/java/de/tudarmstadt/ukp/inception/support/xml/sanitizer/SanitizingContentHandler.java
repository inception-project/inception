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

import static de.tudarmstadt.ukp.inception.support.xml.XmlParserUtils.getQName;
import static java.util.Comparator.comparing;
import static java.util.stream.Collectors.toList;
import static org.apache.commons.lang3.StringUtils.startsWith;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.Set;
import java.util.Stack;

import javax.xml.XMLConstants;
import javax.xml.namespace.QName;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xml.sax.Attributes;
import org.xml.sax.ContentHandler;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.AttributesImpl;

import de.tudarmstadt.ukp.inception.support.xml.ContentHandlerAdapter;

public class SanitizingContentHandler
    extends ContentHandlerAdapter
{
    private final Logger log = LoggerFactory.getLogger(getClass());

    private static final String MASKED = "MASKED-";
    private static final String XMLNS = "xmlns";
    private static final String XMLNS_PREFIX = "xmlns:";

    private final PolicyCollection policies;

    private final Stack<Frame> stack;

    private char filteredCharacter = ' ';

    private final LinkedHashMap<String, String> namespaceMappings = new LinkedHashMap<>();

    private Set<QName> maskedElements = new HashSet<>();
    private Map<QName, Set<QName>> maskedAttributes = new HashMap<>();

    public SanitizingContentHandler(ContentHandler aDelegate, PolicyCollection aPolicies)
    {
        super(aDelegate);
        stack = new Stack<>();
        policies = aPolicies;
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
    public void startElement(String aUri, String aLocalName, String aQName, Attributes aAtts)
        throws SAXException
    {
        var localNamespace = new LinkedHashMap<String, String>();
        for (Entry<String, String> xmlns : prefixMappings(aAtts).entrySet()) {
            var oldValue = namespaceMappings.put(xmlns.getKey(), xmlns.getValue());
            if (oldValue == null) {
                localNamespace.put(xmlns.getKey(), xmlns.getValue());
            }
        }

        var element = toQName(aUri, aLocalName, aQName);

        var policy = policies.forElement(element);
        var action = policy.map(ElementPolicy::getAction)
                .orElse(policies.getDefaultElementAction());
        switch (action) {
        case PASS:
            var attributes = sanitizeAttributes(element, aAtts);
            startElement(element, attributes, policy, action, localNamespace);
            break;
        case SKIP: // fall-through
        case DROP:
            startElement(element, null, policy, action, localNamespace);
            break;
        default:
            throw new SAXException("Unsupported element action: [" + action + "]");
        }
    }

    private void startElement(QName aElement, Attributes aAtts, Optional<ElementPolicy> aPolicy,
            ElementAction aAction, Map<String, String> aLocalNamespaces)
        throws SAXException
    {
        QName element = aElement;

        if ((aAction == ElementAction.DROP || aAction == ElementAction.SKIP)
                && policies.isDebug()) {
            element = maskElement(element);
        }

        stack.push(new Frame(element, aPolicy, aAction, aLocalNamespaces));

        if (aAction == ElementAction.PASS || policies.isDebug()) {
            super.startElement(element.getNamespaceURI(), element.getLocalPart(), getQName(element),
                    aAtts);
        }
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
        if (frame.action == ElementAction.PASS || policies.isDebug()) {
            var qName = getQName(frame.element);
            super.endElement(frame.element.getNamespaceURI(), frame.element.getLocalPart(), qName);
        }

        frame.namespaces.keySet().forEach(namespaceMappings::remove);

        if (stack.isEmpty()) {
            if (policies.isDebug() && log.isDebugEnabled()) {
                log.debug("[{}] Masked elements: {}", policies.getName(), maskedElements.stream() //
                        .map(QName::toString) //
                        .sorted() //
                        .collect(toList()));
                for (var element : maskedAttributes.keySet().stream()
                        .sorted(comparing(QName::getLocalPart)).collect(toList())) {
                    log.debug("[{}] Masked attributes on {}: {}", policies.getName(), element,
                            maskedAttributes.get(element).stream().map(QName::toString) //
                                    .sorted() //
                                    .collect(toList()));
                }
            }
        }
    }

    @Override
    public void characters(char[] aCh, int aStart, int aLength) throws SAXException
    {
        var action = stack.isEmpty() ? policies.getDefaultElementAction() : stack.peek().action;
        switch (action) {
        case DROP:
            var placeholder = new char[aLength];
            Arrays.fill(placeholder, filteredCharacter);
            super.characters(placeholder, 0, aLength);
            break;
        case SKIP: // pass-through
        case PASS:
            super.characters(aCh, aStart, aLength);
            break;
        default:
            throw new SAXException("Unsupported element action: [" + action + "]");
        }
    }

    @Override
    public void ignorableWhitespace(char[] aCh, int aStart, int aLength) throws SAXException
    {
        var action = stack.peek().action;
        switch (action) {
        case DROP:
            var placeholder = new char[aLength];
            Arrays.fill(placeholder, filteredCharacter);
            super.ignorableWhitespace(placeholder, 0, aLength);
            break;
        case SKIP: // pass-through
        case PASS:
            super.ignorableWhitespace(aCh, aStart, aLength);
            break;
        default:
            throw new SAXException("Unsupported element action: [" + action + "]");
        }
    }

    private Attributes sanitizeAttributes(QName aElement, Attributes aAtts) throws SAXException
    {
        if (aAtts == null) {
            return null;
        }

        AttributesImpl sanitizedAttributes = new AttributesImpl();
        for (int i = 0; i < aAtts.getLength(); i++) {
            sanitizeAttribute(sanitizedAttributes, aElement, aAtts, i);
        }
        return sanitizedAttributes;
    }

    private void sanitizeAttribute(AttributesImpl aSanitizedAttributes, QName aElement,
            Attributes aAtts, int i)
        throws SAXException
    {
        var uri = aAtts.getURI(i);
        var localName = aAtts.getLocalName(i);
        var qName = aAtts.getQName(i);
        var type = aAtts.getType(i);
        var value = aAtts.getValue(i);

        var attribute = toQName(uri, localName, qName);

        var action = policies.forAttribute(aElement, attribute, type, value)
                .orElse(policies.getDefaultAttributeAction());

        if ("xmlns".equals(attribute.getPrefix())) {
            action = AttributeAction.PASS;
        }

        switch (action) {
        case PASS:
            aSanitizedAttributes.addAttribute(uri, localName, qName, type, value);
            break;
        case PASS_NO_NS:
            aSanitizedAttributes.addAttribute("", attribute.getLocalPart(),
                    attribute.getLocalPart(), type, value);
            break;
        case DROP:
            if (policies.isDebug()) {
                attribute = maskAttribute(aElement, attribute);
                aSanitizedAttributes.addAttribute(attribute.getNamespaceURI(),
                        attribute.getLocalPart(), getQName(attribute), type, value);
            }
            break;
        default:
            throw new SAXException("Unsupported attribute action: [" + action + "]");
        }
    }

    private static final class Frame
    {
        final QName element;
        final @SuppressWarnings("unused") Optional<ElementPolicy> policy;
        final ElementAction action;
        final Map<String, String> namespaces;

        public Frame(QName aElement, Optional<ElementPolicy> aPolicy, ElementAction aAction,
                Map<String, String> aLocalNamespaces)
        {
            element = aElement;
            policy = aPolicy;
            action = aAction;

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
            return "[" + element.getLocalPart() + " -> " + action + "]";
        }
    }

    private QName maskElement(QName aElement)
    {
        maskedElements.add(aElement);
        return mask(aElement);
    }

    private QName maskAttribute(QName aElement, QName aAttribute)
    {
        maskedAttributes.computeIfAbsent(aElement, k -> new HashSet<>()).add(aAttribute);
        return mask(aAttribute);
    }

    private QName mask(QName element)
    {
        return new QName(element.getNamespaceURI(), MASKED + element.getLocalPart(),
                element.getPrefix());
    }

    private QName toQName(String aUri, String aLocalName, String aQName)
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

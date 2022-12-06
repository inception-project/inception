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

import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.Stack;

import javax.xml.XMLConstants;
import javax.xml.namespace.QName;

import org.apache.commons.lang3.StringUtils;
import org.apache.wicket.util.string.Strings;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xml.sax.Attributes;
import org.xml.sax.ContentHandler;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.AttributesImpl;

import de.tudarmstadt.ukp.clarin.webanno.support.xml.ContentHandlerAdapter;

public class SanitizingContentHandler
    extends ContentHandlerAdapter
{
    private final Logger log = LoggerFactory.getLogger(getClass());

    private static final String MASKED = "MASKED-";

    private final PolicyCollection policies;

    private final Stack<Frame> stack;

    private char filteredCharacter = ' ';

    private Set<QName> maskedElements = new HashSet<>();
    private Map<QName, Set<QName>> maskedAttributes = new HashMap<>();

    public SanitizingContentHandler(ContentHandler aDelegate, PolicyCollection aPolicies)
    {
        super(aDelegate);
        stack = new Stack<>();
        policies = aPolicies;
    }

    @Override
    public void startElement(String aUri, String aLocalName, String aQName, Attributes aAtts)
        throws SAXException
    {
        var element = toQName(aUri, aLocalName, aQName);

        var policy = policies.forElement(element);
        var action = policy.map(ElementPolicy::getAction)
                .orElse(policies.getDefaultElementAction());
        switch (action) {
        case PASS:
            var attributes = sanitizeAttributes(element, aAtts);
            startElement(element, attributes, policy, action);
            break;
        case DROP:
            startElement(element, null, policy, action);
            break;
        default:
            throw new SAXException("Unsupported element action: [" + action + "]");
        }
    }

    private void startElement(QName aElement, Attributes aAtts, Optional<ElementPolicy> aPolicy,
            ElementAction aAction)
        throws SAXException
    {
        QName element = aElement;

        if (aAction == ElementAction.DROP && policies.isDebug()) {
            element = maskElement(element);
        }

        stack.push(new Frame(element, aPolicy, aAction));

        if (aAction == ElementAction.PASS || policies.isDebug()) {
            super.startElement(element.getNamespaceURI(), element.getLocalPart(), getQName(element),
                    aAtts);
        }
    }

    @Override
    public void endElement(String aUri, String aLocalName, String aQName) throws SAXException
    {
        var frame = stack.pop();
        if (frame.action == ElementAction.PASS || policies.isDebug()) {
            var qName = getQName(frame.element);
            super.endElement(frame.element.getNamespaceURI(), frame.element.getLocalPart(), qName);
        }

        if (stack.isEmpty()) {
            if (policies.isDebug() && log.isDebugEnabled()) {
                log.debug("Masked elements: {}",
                        maskedElements.stream().sorted(comparing(QName::getLocalPart)) //
                                .map(this::toPrefixedForm) //
                                .collect(toList()));
                for (var element : maskedAttributes.keySet().stream()
                        .sorted(comparing(QName::getLocalPart)).collect(toList())) {
                    log.debug("Masked attributes on {}: {}", element,
                            maskedAttributes.get(element).stream()
                                    .sorted(comparing(QName::getLocalPart)) //
                                    .map(this::toPrefixedForm) //
                                    .collect(toList()));
                }
            }
        }
    }

    private String toPrefixedForm(QName aQName)
    {
        if (Strings.isEmpty(aQName.getPrefix())) {
            return aQName.getLocalPart();
        }

        return aQName.getPrefix() + ":" + aQName.getLocalPart();
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
        final Optional<ElementPolicy> policy;
        final ElementAction action;

        public Frame(QName aElement, Optional<ElementPolicy> aPolicy, ElementAction aAction)
        {
            element = aElement;
            policy = aPolicy;
            action = aAction;
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

        return new QName(aUri, localName, prefix);
    }
}

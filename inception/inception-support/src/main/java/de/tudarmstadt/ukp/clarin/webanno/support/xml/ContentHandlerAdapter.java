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
package de.tudarmstadt.ukp.clarin.webanno.support.xml;

import static de.tudarmstadt.ukp.inception.support.xml.XmlParserUtils.getQName;
import static java.util.Collections.emptyMap;

import java.util.Map;

import javax.xml.namespace.QName;

import org.xml.sax.Attributes;
import org.xml.sax.ContentHandler;
import org.xml.sax.Locator;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.AttributesImpl;

public class ContentHandlerAdapter
    implements ContentHandler
{
    private static final String CDATA = "CDATA";

    protected final ContentHandler delegate;

    public ContentHandlerAdapter(ContentHandler aDelegate)
    {
        delegate = aDelegate;
    }

    @Override
    public void setDocumentLocator(Locator aLocator)
    {
        delegate.setDocumentLocator(aLocator);
    }

    @Override
    public void startDocument() throws SAXException
    {
        delegate.startDocument();
    }

    @Override
    public void endDocument() throws SAXException
    {
        delegate.endDocument();
    }

    @Override
    public void startPrefixMapping(String aPrefix, String aUri) throws SAXException
    {
        delegate.startPrefixMapping(aPrefix, aUri);
    }

    @Override
    public void endPrefixMapping(String aPrefix) throws SAXException
    {
        delegate.endPrefixMapping(aPrefix);
    }

    public void startElement(String aLocalName) throws SAXException
    {
        startElement(aLocalName, emptyMap());
    }

    public void startElement(QName aElement) throws SAXException
    {
        startElement(aElement, emptyMap());
    }

    public void startElement(QName aElement, Map<QName, String> aAttributes) throws SAXException
    {
        Attributes atts = null;
        if (aAttributes != null && !aAttributes.isEmpty()) {
            var mutableAttributes = new AttributesImpl();
            for (var entry : aAttributes.entrySet()) {
                var attribute = entry.getKey();
                mutableAttributes.addAttribute(attribute.getNamespaceURI(),
                        attribute.getLocalPart(), getQName(attribute), CDATA, entry.getValue());
            }
            atts = mutableAttributes;
        }

        startElement(aElement.getNamespaceURI(), aElement.getLocalPart(), getQName(aElement), atts);
    }

    public void startElement(String aLocalName, Map<String, String> aAttributes) throws SAXException
    {
        Attributes atts = null;
        if (aAttributes != null && !aAttributes.isEmpty()) {
            var mutableAttributes = new AttributesImpl();
            for (var entry : aAttributes.entrySet()) {
                mutableAttributes.addAttribute(null, entry.getKey(), entry.getKey(), CDATA,
                        entry.getValue());
            }
            atts = mutableAttributes;
        }

        startElement(null, aLocalName, aLocalName, atts);
    }

    @Override
    public void startElement(String aUri, String aLocalName, String aQName, Attributes aAtts)
        throws SAXException
    {
        delegate.startElement(aUri, aLocalName, aQName, aAtts);
    }

    public void endElement(String aLocalName) throws SAXException
    {
        endElement(null, aLocalName, aLocalName);
    }

    public void endElement(QName aElement) throws SAXException
    {
        endElement(aElement.getNamespaceURI(), aElement.getLocalPart(), getQName(aElement));
    }

    @Override
    public void endElement(String aUri, String aLocalName, String aQName) throws SAXException
    {
        delegate.endElement(aUri, aLocalName, aQName);
    }

    public void characters(String aString) throws SAXException
    {
        characters(aString.toCharArray(), 0, aString.length());
    }

    @Override
    public void characters(char[] aCh, int aStart, int aLength) throws SAXException
    {
        delegate.characters(aCh, aStart, aLength);
    }

    @Override
    public void ignorableWhitespace(char[] aCh, int aStart, int aLength) throws SAXException
    {
        delegate.ignorableWhitespace(aCh, aStart, aLength);
    }

    @Override
    public void processingInstruction(String aTarget, String aData) throws SAXException
    {
        delegate.processingInstruction(aTarget, aData);
    }

    @Override
    public void skippedEntity(String aName) throws SAXException
    {
        delegate.skippedEntity(aName);
    }
}

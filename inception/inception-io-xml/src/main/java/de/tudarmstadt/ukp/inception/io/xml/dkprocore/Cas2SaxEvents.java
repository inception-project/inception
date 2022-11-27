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
import static org.apache.uima.fit.util.JCasUtil.selectSingle;

import org.apache.uima.jcas.JCas;
import org.dkpro.core.api.xml.type.XmlAttribute;
import org.dkpro.core.api.xml.type.XmlDocument;
import org.dkpro.core.api.xml.type.XmlElement;
import org.dkpro.core.api.xml.type.XmlNode;
import org.dkpro.core.api.xml.type.XmlTextNode;
import org.xml.sax.ContentHandler;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.AttributesImpl;

public class Cas2SaxEvents
{
    protected final ContentHandler handler;

    public Cas2SaxEvents(ContentHandler aHandler)
    {
        handler = aHandler;
    }

    public void process(JCas aJCas) throws SAXException
    {
        XmlDocument doc = selectSingle(aJCas, XmlDocument.class);

        process(doc);
    }

    public void process(XmlDocument aDoc) throws SAXException
    {
        handler.startDocument();

        if (aDoc.getRoot() == null) {
            throw new SAXException("Document has no root element");
        }

        process(aDoc.getRoot());

        handler.endDocument();
    }

    public void process(XmlElement aElement) throws SAXException
    {
        AttributesImpl attributes = attributes(aElement);
        String uri = defaultString(aElement.getUri());
        String localName = defaultString(aElement.getLocalName());
        String qName = defaultString(aElement.getQName());

        handler.startElement(uri, localName, qName, attributes);

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

        handler.endElement(uri, localName, qName);
    }

    public AttributesImpl attributes(XmlElement aElement)
    {
        AttributesImpl attrs = new AttributesImpl();
        if (aElement.getAttributes() != null) {
            for (XmlAttribute attr : aElement.getAttributes()) {
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

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

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.xml.sax.Attributes;
import org.xml.sax.ContentHandler;
import org.xml.sax.SAXException;

/**
 * This class is for testing - do not use in production.
 */
public class XmlEventLoggingFilter
    extends ContentHandlerAdapter
{
    List<String> events = new ArrayList<>();

    public XmlEventLoggingFilter(ContentHandler aDelegate)
    {
        super(aDelegate);
    }

    public List<String> getEvents()
    {
        return Collections.unmodifiableList(events);
    }

    @Override
    public void startDocument() throws SAXException
    {
        events.add("[START-DOCUMENT]");
        super.startDocument();
    }

    @Override
    public void endDocument() throws SAXException
    {
        events.add("[END-DOCUMENT]");
        super.endDocument();
    }

    @Override
    public void startElement(String aUri, String aLocalName, String aQName, Attributes aAtts)
        throws SAXException
    {
        StringBuilder attributes = new StringBuilder();
        for (int i = 0; i < aAtts.getLength(); i++) {
            if (i > 0) {
                attributes.append(" ");
            }
            attributes.append("[");
            attributes.append("[");
            attributes.append(aAtts.getQName(i));
            attributes.append("]");
            attributes.append("{");
            attributes.append(aAtts.getURI(i));
            attributes.append("}[");
            attributes.append(aAtts.getLocalName(i));
            attributes.append("]]=");
            attributes.append("[");
            attributes.append(aAtts.getValue(i));
            attributes.append("]");
        }
        events.add("[START-ELEMENT [" + aQName + "] {" + aUri + "}[" + aLocalName + "]] "
                + attributes);
        delegate.startElement(aUri, aLocalName, aQName, aAtts);
    }

    @Override
    public void endElement(String aUri, String aLocalName, String aQName) throws SAXException
    {
        events.add("[END-ELEMENT [" + aQName + "] {" + aUri + "}[" + aLocalName + "]]");
        delegate.endElement(aUri, aLocalName, aQName);
    }

    @Override
    public void startPrefixMapping(String aPrefix, String aUri) throws SAXException
    {
        events.add("[START-PREFIX-MAPPING [" + aPrefix + "] -> " + aUri + "]");
        super.startPrefixMapping(aPrefix, aUri);
    }

    @Override
    public void endPrefixMapping(String aPrefix) throws SAXException
    {
        events.add("[END-PREFIX-MAPPING [" + aPrefix + "]]");
        super.endPrefixMapping(aPrefix);
    }
}

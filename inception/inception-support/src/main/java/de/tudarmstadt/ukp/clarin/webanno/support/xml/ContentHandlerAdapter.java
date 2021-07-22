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

import org.xml.sax.Attributes;
import org.xml.sax.ContentHandler;
import org.xml.sax.Locator;
import org.xml.sax.SAXException;

public class ContentHandlerAdapter
    implements ContentHandler
{
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

    @Override
    public void startElement(String aUri, String aLocalName, String aQName, Attributes aAtts)
        throws SAXException
    {
        delegate.startElement(aUri, aLocalName, aQName, aAtts);
    }

    @Override
    public void endElement(String aUri, String aLocalName, String aQName) throws SAXException
    {
        delegate.endElement(aUri, aLocalName, aQName);
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

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

import java.io.IOException;

import org.xml.sax.Attributes;
import org.xml.sax.InputSource;
import org.xml.sax.Locator;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;
import org.xml.sax.helpers.DefaultHandler;

public class DefaultHandlerAdapter<T extends DefaultHandler>
    extends DefaultHandler
{
    protected final T delegate;

    public DefaultHandlerAdapter(T aDelegate)
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

    @Override
    public void warning(SAXParseException aE) throws SAXException
    {
        delegate.warning(aE);
    }

    @Override
    public void error(SAXParseException aE) throws SAXException
    {
        delegate.error(aE);
    }

    @Override
    public void fatalError(SAXParseException aE) throws SAXException
    {
        delegate.fatalError(aE);
    }

    @Override
    public void notationDecl(String aName, String aPublicId, String aSystemId) throws SAXException
    {
        delegate.notationDecl(aName, aPublicId, aSystemId);
    }

    @Override
    public InputSource resolveEntity(String aPublicId, String aSystemId)
        throws IOException, SAXException
    {
        return delegate.resolveEntity(aPublicId, aSystemId);
    }

    @Override
    public void unparsedEntityDecl(String aName, String aPublicId, String aSystemId,
            String aNotationName)
        throws SAXException
    {
        delegate.unparsedEntityDecl(aName, aPublicId, aSystemId, aNotationName);
    }
}

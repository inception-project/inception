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
package de.tudarmstadt.ukp.inception.io.bioc.xml;

import org.xml.sax.Attributes;
import org.xml.sax.ContentHandler;
import org.xml.sax.Locator;
import org.xml.sax.SAXException;

/**
 * @deprecated Experimental code that was deprecated.
 */
@Deprecated
public class SplittingContentHandler
    implements ContentHandler
{
    private final ContentHandler delegate;
    private final String splitQName;

    private int depth = 0;
    private int depthOpened = -1;
    private boolean open = false;

    public SplittingContentHandler(ContentHandler aDelegate, String aSplitQName)
    {
        delegate = aDelegate;
        splitQName = aSplitQName;
    }

    @Override
    public void setDocumentLocator(Locator aLocator)
    {
        delegate.setDocumentLocator(aLocator);
    }

    @Override
    public void startDocument() throws SAXException
    {
        if (open) {
            delegate.startDocument();
        }
    }

    @Override
    public void endDocument() throws SAXException
    {
        if (open) {
            delegate.endDocument();
        }
    }

    @Override
    public void startPrefixMapping(String aPrefix, String aUri) throws SAXException
    {
        if (open) {
            delegate.startPrefixMapping(aPrefix, aUri);
        }
    }

    @Override
    public void endPrefixMapping(String aPrefix) throws SAXException
    {
        if (open) {
            delegate.endPrefixMapping(aPrefix);
        }
    }

    @Override
    public void startElement(String aUri, String aLocalName, String aQName, Attributes aAtts)
        throws SAXException
    {
        depth++;

        if (splitQName.equals(aQName)) {
            depthOpened = depth;
            delegate.startDocument();
            // FIXME It would be reasonable to remember the prefix mappings and to send them to the
            // delegate at this point.
        }

        if (open) {
            delegate.startElement(aUri, aLocalName, aQName, aAtts);
        }
    }

    @Override
    public void endElement(String aUri, String aLocalName, String aQName) throws SAXException
    {
        if (open) {
            delegate.endElement(aUri, aLocalName, aQName);
        }

        if (depthOpened == depth) {
            delegate.endDocument();
            depthOpened = -1;
        }

        depth--;

        if (depth < 0) {
            throw new SAXException("Illegal XML structure");
        }
    }

    @Override
    public void characters(char[] aCh, int aStart, int aLength) throws SAXException
    {
        if (open) {
            delegate.characters(aCh, aStart, aLength);
        }
    }

    @Override
    public void ignorableWhitespace(char[] aCh, int aStart, int aLength) throws SAXException
    {
        if (open) {
            delegate.ignorableWhitespace(aCh, aStart, aLength);
        }

    }

    @Override
    public void processingInstruction(String aTarget, String aData) throws SAXException
    {
        if (open) {
            delegate.processingInstruction(aTarget, aData);
        }
    }

    @Override
    public void skippedEntity(String aName) throws SAXException
    {
        if (open) {
            delegate.skippedEntity(aName);
        }
    }
}

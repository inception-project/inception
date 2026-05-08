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

import org.xml.sax.ContentHandler;
import org.xml.sax.SAXException;

public class TextSanitizingContentHandler
    extends ContentHandlerAdapter
{
    public TextSanitizingContentHandler(ContentHandler aDelegate)
    {
        super(aDelegate);
    }

    @Override
    public void characters(char[] aCh, int aStart, int aLength) throws SAXException
    {
        String s = sanitizeVisibleText(new String(aCh, aStart, aLength));
        delegate.characters(s.toCharArray(), 0, s.length());
    }

    @Override
    public void ignorableWhitespace(char[] aCh, int aStart, int aLength) throws SAXException
    {
        String s = sanitizeVisibleText(new String(aCh, aStart, aLength));
        delegate.ignorableWhitespace(s.toCharArray(), 0, s.length());
    }

    private String sanitizeVisibleText(String aText)
    {
        // Replace newline characters before sending to the browser to avoid the character
        // offsets in the browser to get out-of-sync with the server-side offsets. E.g. some
        // browsers tend to completely discard the `\r`.
        return aText.replace('\r', ' ');
    }
}

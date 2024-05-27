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

import org.xml.sax.Attributes;
import org.xml.sax.ContentHandler;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.AttributesImpl;

import de.tudarmstadt.ukp.inception.support.xml.ContentHandlerAdapter;

/**
 * Replaces characters which are illegal in XML 1.0 or XML 1.1 with a replacement character. The
 * characters are replaced in text nodes as well as in attribute values.
 */
public class IllegalXmlCharacterSanitizingContentHandler
    extends ContentHandlerAdapter
{
    private boolean xml11 = false;
    private char replacementChar = ' ';

    public IllegalXmlCharacterSanitizingContentHandler(ContentHandler aDelegate)
    {
        super(aDelegate);
    }

    public void setXml11(boolean aXml11)
    {
        xml11 = aXml11;
    }

    public void setReplacementChar(char aReplacementChar)
    {
        replacementChar = aReplacementChar;
    }

    @Override
    public void startElement(String aUri, String aLocalName, String aQName, Attributes aAtts)
        throws SAXException
    {
        var newAtts = new AttributesImpl();
        for (int i = 0; i < aAtts.getLength(); i++) {
            var uri = aAtts.getURI(i);
            var localName = aAtts.getLocalName(i);
            var qName = aAtts.getQName(i);
            var type = aAtts.getType(i);
            var value = sanitizeIllegalXmlCharacters(aAtts.getValue(i));
            newAtts.addAttribute(uri, localName, qName, type, value);
        }

        super.startElement(aUri, aLocalName, aQName, newAtts);
    }

    @Override
    public void characters(char[] aCh, int aStart, int aLength) throws SAXException
    {
        String s = sanitizeIllegalXmlCharacters(new String(aCh, aStart, aLength));
        delegate.characters(s.toCharArray(), 0, s.length());
    }

    @Override
    public void ignorableWhitespace(char[] aCh, int aStart, int aLength) throws SAXException
    {
        String s = sanitizeIllegalXmlCharacters(new String(aCh, aStart, aLength));
        delegate.ignorableWhitespace(s.toCharArray(), 0, s.length());
    }

    private String sanitizeIllegalXmlCharacters(String aText)
    {
        char[] chars = aText.toCharArray();
        for (int i = 0; i < chars.length; i++) {
            char c = chars[i];
            if ((c >= 0xD800) && (c <= 0xDBFF)) {
                // The case for Unicode code points #x10000-#x10FFFF. Check if a high surrogate is
                // followed by a low surrogate, which is the only allowable combination.
                int iNext = i + 1;
                if (iNext < chars.length) {
                    char cNext = chars[iNext];
                    if (!((cNext >= 0xDC00) && (cNext <= 0xDFFF))) {
                        chars[i] = replacementChar;
                        continue;
                    }
                    else {
                        i++;
                        continue;
                    }
                }
            }

            if (!isValidXmlUtf16int(c)) {
                // Replace invalid UTF-16 codepoints
                chars[i] = replacementChar;
            }
        }

        return new String(chars);
    }

    private boolean isValidXmlUtf16int(char c)
    {
        if (xml11) {
            return (c >= 0x1 && c <= 0xD7FF) || (c >= 0xE000) && (c <= 0xFFFD);
        }
        else {
            return ((c == 0x9) || (c == 0xA) || (c == 0xD) || ((c >= 0x20) && (c <= 0xD7FF))
                    || (c >= 0xE000 && c <= 0xFFFD));
        }
    }
}

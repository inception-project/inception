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
package de.tudarmstadt.ukp.inception.externaleditor.xml;

import static javax.xml.transform.OutputKeys.INDENT;
import static javax.xml.transform.OutputKeys.METHOD;
import static javax.xml.transform.OutputKeys.OMIT_XML_DECLARATION;

import java.io.Writer;
import java.util.HashSet;
import java.util.Set;

import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.sax.SAXTransformerFactory;
import javax.xml.transform.sax.TransformerHandler;
import javax.xml.transform.stream.StreamResult;

import org.dkpro.core.api.xml.type.XmlDocument;
import org.dkpro.core.api.xml.type.XmlElement;
import org.dkpro.core.api.xml.type.XmlNode;
import org.dkpro.core.api.xml.type.XmlTextNode;
import org.xml.sax.ContentHandler;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.AttributesImpl;

import de.tudarmstadt.ukp.inception.io.xml.dkprocore.Cas2SaxEvents;
import de.tudarmstadt.ukp.inception.support.xml.TextSanitizingContentHandler;
import de.tudarmstadt.ukp.inception.support.xml.XmlParserUtils;

public class XmlCas2SaxEvents
    extends Cas2SaxEvents
{
    public static final String DATA_CAPTURE_ROOT = "data-capture-root";

    private final XmlDocument xml;
    private final Set<XmlNode> captureRoots;

    public XmlCas2SaxEvents(XmlDocument aXml, ContentHandler aHandler)
    {
        super(aHandler);
        xml = aXml;

        if (xml.getCaptureRoots() != null) {
            captureRoots = new HashSet<>();
            xml.getCaptureRoots().forEach(captureRoots::add);
        }
        else {
            captureRoots = null;
        }
    }

    public static ContentHandler makeSerializer(Writer out) throws TransformerConfigurationException
    {
        SAXTransformerFactory tf = XmlParserUtils.newTransformerFactory();
        TransformerHandler th = tf.newTransformerHandler();
        th.getTransformer().setOutputProperty(OMIT_XML_DECLARATION, "yes");
        th.getTransformer().setOutputProperty(METHOD, "xml");
        th.getTransformer().setOutputProperty(INDENT, "no");
        th.setResult(new StreamResult(out));

        ContentHandler sh = new TextSanitizingContentHandler(th);

        return sh;
    }

    @Override
    public void process(XmlElement aElement) throws SAXException
    {
        // HACK: adding a wrapper because otherwise RecogitoJS cannot insert its own
        // wrapper...
        if (captureRoots != null && aElement == xml.getRoot() && captureRoots.contains(aElement)) {
            handler.startElement(null, null, "wrapper", null);
        }

        super.process(aElement);

        if (captureRoots != null && aElement == xml.getRoot() && captureRoots.contains(aElement)) {
            handler.endElement(null, null, "wrapper");
        }
    }

    @Override
    public AttributesImpl attributes(XmlElement aElement)
    {
        AttributesImpl attrs = super.attributes(aElement);
        if (captureRoots != null && captureRoots.contains(aElement)) {
            attrs.addAttribute(null, null, DATA_CAPTURE_ROOT, null, "");
        }
        return attrs;
    }

    @Override
    public void process(XmlTextNode aChild) throws SAXException
    {
        // Contrary to the default behavior, we only want to send captured text to the browser
        if (aChild.getCaptured()) {
            var text = aChild.getCoveredText().toCharArray();
            handler.characters(text, 0, text.length);
        }
    }
}

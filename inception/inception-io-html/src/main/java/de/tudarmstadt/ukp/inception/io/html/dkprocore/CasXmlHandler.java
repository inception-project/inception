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
package de.tudarmstadt.ukp.inception.io.html.dkprocore;

import static org.apache.commons.lang3.StringUtils.trimToNull;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Deque;
import java.util.List;

import org.apache.uima.jcas.JCas;
import org.apache.uima.jcas.cas.FSArray;
import org.dkpro.core.api.xml.type.XmlAttribute;
import org.dkpro.core.api.xml.type.XmlDocument;
import org.dkpro.core.api.xml.type.XmlElement;
import org.dkpro.core.api.xml.type.XmlNode;
import org.dkpro.core.api.xml.type.XmlTextNode;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

public class CasXmlHandler
    extends DefaultHandler
{
    private final JCas jcas;
    private final StringBuilder text;
    private final Deque<StackFrame> stack;

    private XmlDocument docNode;
    private boolean captureText = true;

    public CasXmlHandler(JCas aJCas)
    {
        jcas = aJCas;
        text = new StringBuilder();
        stack = new ArrayDeque<>();
    }

    @Override
    public void startDocument() throws SAXException
    {
        if (docNode != null || !stack.isEmpty() || text.length() != 0) {
            throw new SAXException("Illegal document start event when data has already been seen.");
        }

        docNode = new XmlDocument(jcas);
        docNode.setBegin(text.length());
    }

    @Override
    public void endDocument() throws SAXException
    {
        docNode.setEnd(text.length());
        docNode.addToIndexes();

        jcas.setDocumentText(text.toString());
    };

    @Override
    public void startElement(String aUri, String aLocalName, String aQName, Attributes aAttributes)
        throws SAXException
    {
        if (docNode == null) {
            throw new SAXException(
                    "Illegal element start event when document start has not been seen.");
        }

        XmlElement element = new XmlElement(jcas);
        element.setBegin(text.length());
        element.setUri(trimToNull(aUri));
        element.setLocalName(trimToNull(aLocalName));
        element.setQName(trimToNull(aQName));

        if (aAttributes.getLength() > 0) {
            var attributes = new FSArray<XmlAttribute>(jcas, aAttributes.getLength());
            for (int i = 0; i < aAttributes.getLength(); i++) {
                XmlAttribute attribute = new XmlAttribute(jcas);
                attribute.setUri(trimToNull(aAttributes.getURI(i)));
                attribute.setLocalName(trimToNull(aAttributes.getLocalName(i)));
                attribute.setQName(trimToNull(aAttributes.getQName(i)));
                attribute.setValueType(trimToNull(aAttributes.getType(i)));
                attribute.setValue(aAttributes.getValue(i));
                attributes.set(i, attribute);
            }
            element.setAttributes(attributes);
        }

        attachToParent(element);

        boolean capture;
        StackFrame parentFrame = stack.peek();
        if (parentFrame != null) {
            capture = parentFrame.isCaptureText();
        }
        else {
            capture = captureText;
        }

        stack.push(new StackFrame(element, capture));
    }

    @Override
    public void endElement(String aUri, String aLocalName, String aQName) throws SAXException
    {
        StackFrame frame = stack.pop();

        XmlElement element = frame.getElement();
        element.setEnd(text.length());

        // Fill in children
        if (!frame.getChildren().isEmpty()) {
            var children = new FSArray<XmlNode>(jcas, frame.getChildren().size());
            for (int i = 0; i < frame.getChildren().size(); i++) {
                children.set(i, frame.getChildren().get(i));
            }
            element.setChildren(children);
        }

        element.addToIndexes();
    }

    @Override
    public void characters(char[] aCh, int aStart, int aLength) throws SAXException
    {
        if (stack.isEmpty()) {
            // We ignore any characters outside the root elements. These could include e.g.
            // whitespace in the context of a doctype before the root element or trailing whitespace
            // after the root element.
            return;
        }

        XmlTextNode textNode = new XmlTextNode(jcas);
        textNode.setBegin(text.length());

        if (stack.peek().isCaptureText()) {
            text.append(aCh, aStart, aLength);
            textNode.setCaptured(true);
        }
        else {
            textNode.setText(new String(aCh, aStart, aLength));
            textNode.setCaptured(false);
        }

        textNode.setEnd(text.length());
        textNode.addToIndexes();

        attachToParent(textNode);
    }

    @Override
    public void ignorableWhitespace(char[] aCh, int aStart, int aLength) throws SAXException
    {
        characters(aCh, aStart, aLength);
    }

    private void attachToParent(XmlNode aNode)
    {
        StackFrame parentFrame = stack.peek();
        if (parentFrame != null) {
            aNode.setParent(parentFrame.getElement());
            parentFrame.addChild(aNode);
        }
        else {
            docNode.setRoot((XmlElement) aNode);
        }
    }

    public CharSequence getText()
    {
        return text;
    }

    public Collection<StackFrame> getStack()
    {
        return Collections.unmodifiableCollection(stack);
    }

    public XmlElement getCurrentElement()
    {
        return stack.peek().getElement();
    }

    public void captureText(boolean aCapture)
    {
        if (stack.isEmpty()) {
            captureText = aCapture;
        }
        else {
            stack.peek().setCaptureText(aCapture);
        }
    }

    private static class StackFrame
    {
        private final XmlElement element;
        private final List<XmlNode> children = new ArrayList<>();
        private boolean captureText;

        public StackFrame(XmlElement aElement, boolean aCaptureText)
        {
            element = aElement;
            captureText = aCaptureText;
        }

        public XmlElement getElement()
        {
            return element;
        }

        public void addChild(XmlNode aChild)
        {
            children.add(aChild);
        }

        public List<XmlNode> getChildren()
        {
            return children;
        }

        public boolean isCaptureText()
        {
            return captureText;
        }

        public void setCaptureText(boolean aCaptureText)
        {
            captureText = aCaptureText;
        }
    }
}

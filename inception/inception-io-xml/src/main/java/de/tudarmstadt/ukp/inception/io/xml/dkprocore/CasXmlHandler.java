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

import static java.util.Collections.unmodifiableSet;
import static org.apache.commons.lang3.StringUtils.trimToNull;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Deque;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;

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

import de.tudarmstadt.ukp.dkpro.core.api.segmentation.type.Sentence;
import de.tudarmstadt.ukp.inception.support.uima.SegmentationUtils;
import it.unimi.dsi.fastutil.ints.IntArrayList;

public class CasXmlHandler
    extends DefaultHandler
{
    private final JCas jcas;
    private final StringBuilder text;
    private final Deque<StackFrame> stack;
    private final Set<String> blockElements = new HashSet<>();

    private XmlDocument docNode;
    private boolean captureText = true;
    private boolean splitSentencesInBlockElements = true;
    private boolean commitText = true;

    private final Set<ElementListener> listeners = new LinkedHashSet<>();

    public CasXmlHandler(JCas aJCas)
    {
        jcas = aJCas;
        text = new StringBuilder();
        stack = new ArrayDeque<>();
    }

    public void setCommitText(boolean aCommitText)
    {
        commitText = aCommitText;
    }

    public void addListener(ElementListener aListener)
    {
        listeners.add(aListener);
    }

    public void removeListener(ElementListener aListener)
    {
        listeners.remove(aListener);
    }

    public void setBlockElements(Collection<String> aElements)
    {
        blockElements.clear();
        if (aElements != null) {
            blockElements.addAll(aElements);
        }
    }

    public Set<String> getBlockElements()
    {
        return unmodifiableSet(blockElements);
    }

    public void setSplitSentencesInBlockElements(boolean aSplitSentencesInBlockElements)
    {
        splitSentencesInBlockElements = aSplitSentencesInBlockElements;
    }

    public boolean isSplitSentencesInBlockElements()
    {
        return splitSentencesInBlockElements;
    }

    @Override
    public void startDocument() throws SAXException
    {
        if (docNode != null || !stack.isEmpty() || text.length() != 0) {
            throw new SAXException("Illegal document start event when data has already been seen.");
        }

        docNode = new XmlDocument(jcas);
        docNode.setBegin(text.length());

        for (var l : listeners) {
            l.startDocument(docNode);
        }
    }

    @Override
    public void endDocument() throws SAXException
    {
        docNode.setEnd(text.length());
        docNode.addToIndexes();

        for (var l : listeners) {
            l.endDocument(docNode);
        }

        if (commitText) {
            jcas.setDocumentText(text.toString());
        }

        if (!blockElements.isEmpty()) {
            if (splitSentencesInBlockElements) {
                splitSentencesRespectingBlockElements(jcas, blockElements);
            }
            else {
                treatBlockElementsAsSentences(jcas, blockElements);
            }
        }
    }

    @Override
    public void startElement(String aUri, String aLocalName, String aQName, Attributes aAttributes)
        throws SAXException
    {
        if (docNode == null) {
            throw new SAXException(
                    "Illegal element start event when document start has not been seen.");
        }

        var element = new XmlElement(jcas);
        element.setBegin(text.length());
        element.setUri(trimToNull(aUri));
        element.setLocalName(trimToNull(aLocalName));
        element.setQName(trimToNull(aQName));

        if (aAttributes != null && aAttributes.getLength() > 0) {
            var attributes = new FSArray<XmlAttribute>(jcas, aAttributes.getLength());
            for (int i = 0; i < aAttributes.getLength(); i++) {
                var attribute = new XmlAttribute(jcas);
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
        var parentFrame = stack.peek();
        if (parentFrame != null) {
            capture = parentFrame.isCaptureText();
        }
        else {
            capture = captureText;
        }

        stack.push(new StackFrame(element, capture));

        for (var l : listeners) {
            l.startElement(element);
        }
    }

    @Override
    public void endElement(String aUri, String aLocalName, String aQName) throws SAXException
    {
        StackFrame frame = stack.pop();

        var element = frame.getElement();
        element.setEnd(text.length());

        // Fill in children
        if (!frame.getChildren().isEmpty()) {
            var children = new FSArray<XmlNode>(jcas, frame.getChildren().size());
            for (int i = 0; i < frame.getChildren().size(); i++) {
                children.set(i, frame.getChildren().get(i));
            }
            element.setChildren(children);
        }

        for (var l : frame.onEndElementCallbacks) {
            l.accept(element);
        }

        for (var l : listeners) {
            l.endElement(element);
        }

        element.addToIndexes();
    }

    @Override
    public void characters(char[] aCh, int aStart, int aLength)
    {
        if (stack.isEmpty()) {
            // We ignore any characters outside the root elements. These could include e.g.
            // whitespace in the context of a doctype before the root element or trailing whitespace
            // after the root element.
            return;
        }

        var textNode = new XmlTextNode(jcas);
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
    public void ignorableWhitespace(char[] aCh, int aStart, int aLength)
    {
        characters(aCh, aStart, aLength);
    }

    private void attachToParent(XmlNode aNode)
    {
        var parentFrame = stack.peek();
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

    protected Collection<StackFrame> getStack()
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
            return;
        }

        stack.peek().setCaptureText(aCapture);
    }

    public void onEndElement(Consumer<XmlElement> aCallback)
    {
        if (stack.isEmpty()) {
            throw new IllegalStateException(
                    "onEndElement callback can only be added if an element has been opened");
        }

        stack.peek().onEndElement(aCallback);
    }

    public boolean isCapturingText()
    {
        if (stack.isEmpty()) {
            return captureText;
        }

        return stack.peek().isCaptureText();
    }

    static void splitSentencesRespectingBlockElements(JCas aJCas, Set<String> aZoningElements)
    {
        var boundaries = new IntArrayList();
        boundaries.add(0);
        boundaries.add(aJCas.getDocumentText().length());

        for (var e : aJCas.select(XmlElement.class)) {
            if (aZoningElements.contains(e.getQName())) {
                boundaries.add(e.getBegin());
                boundaries.add(e.getEnd());
            }
        }

        var sortedBoundaries = boundaries.intStream().distinct().sorted().toArray();

        if (sortedBoundaries.length < 2) {
            sortedBoundaries = new int[] { 0, aJCas.getDocumentText().length() };
        }

        for (int i = 1; i < sortedBoundaries.length; i++) {
            SegmentationUtils.splitSentences(aJCas.getCas(), sortedBoundaries[i - 1],
                    sortedBoundaries[i]);
        }
    }

    static void treatBlockElementsAsSentences(JCas aJCas, Set<String> aZoningElements)
    {
        var boundaries = new IntArrayList();
        boundaries.add(0);
        boundaries.add(aJCas.getDocumentText().length());

        var xmlElementIterator = aJCas.select(XmlElement.class).iterator();
        while (xmlElementIterator.hasNext()) {
            var e = xmlElementIterator.next();
            if (aZoningElements.contains(e.getQName())) {
                var zone = e;

                boundaries.add(zone.getBegin());
                boundaries.add(zone.getEnd());

                // Skip over elements covered by the current sentence
                while (xmlElementIterator.hasNext() && zone.covering(e)) {
                    e = xmlElementIterator.next();
                }
            }
        }

        var sortedBoundaries = boundaries.intStream().distinct().sorted().toArray();

        for (int i = 1; i < sortedBoundaries.length; i++) {
            var sentence = new Sentence(aJCas, sortedBoundaries[i - 1], sortedBoundaries[i]);
            sentence.trim();
            if (sentence.getBegin() != sentence.getEnd()) {
                sentence.addToIndexes();
            }
        }
    }

    public static interface ElementListener
    {
        default void startDocument(XmlDocument aDocument) throws SAXException
        {
            // Do nothing
        }

        default void endDocument(XmlDocument aDocument) throws SAXException
        {
            // Do nothing
        }

        default void startElement(XmlElement aElement) throws SAXException
        {
            // Do nothing
        }

        default void endElement(XmlElement aElement) throws SAXException
        {
            // Do nothing
        }
    }

    protected static final class StackFrame
    {
        private final XmlElement element;
        private final List<XmlNode> children = new ArrayList<>();
        private boolean captureText;
        private final List<Consumer<XmlElement>> onEndElementCallbacks = new ArrayList<>(1);

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

        void onEndElement(Consumer<XmlElement> aCallback)
        {
            onEndElementCallbacks.add(aCallback);
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

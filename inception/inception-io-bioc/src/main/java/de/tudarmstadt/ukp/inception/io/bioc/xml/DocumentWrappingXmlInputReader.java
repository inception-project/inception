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

import java.util.NoSuchElementException;

import javax.xml.stream.XMLEventFactory;
import javax.xml.stream.XMLEventReader;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.events.XMLEvent;
import javax.xml.stream.util.EventReaderDelegate;

/**
 * @deprecated Experimental code that was deprecated.
 */
@Deprecated
public class DocumentWrappingXmlInputReader
    extends EventReaderDelegate
{
    // private static final Logger LOG =
    // LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    private boolean startDocumentRead = false;
    private boolean endDocumentRead = false;
    private final XMLEventFactory eventFactory = XMLEventFactory.newInstance();
    private boolean locationSet = false;
    private int depth = 0;
    private boolean done = false;

    public DocumentWrappingXmlInputReader(XMLEventReader reader)
    {
        super(reader);
    }

    @Override
    public XMLEvent nextEvent() throws XMLStreamException
    {
        initLocationIfNecessary();

        if (!startDocumentRead) {
            startDocumentRead = true;
            // LOG.info("GET startDocument");
            return eventFactory.createStartDocument();
        }

        if (done) {
            if (!endDocumentRead) {
                endDocumentRead = true;
                // LOG.info("GET endDocument");
                return eventFactory.createEndDocument();
            }

            throw new NoSuchElementException("Event stream exhausted");
        }

        XMLEvent event = getParent().nextEvent();
        if (event.isStartElement()) {
            depth++;
        }

        if (event.isEndElement()) {
            depth--;
            if (depth == 0) {
                done = true;
            }
        }

        // LOG.info("GET pass-through {}", event);
        return event;
    }

    private void initLocationIfNecessary() throws XMLStreamException
    {
        if (!locationSet) {
            eventFactory.setLocation(getParent().peek().getLocation());
            locationSet = true;
        }
    }

    @Override
    public XMLEvent peek() throws XMLStreamException
    {
        initLocationIfNecessary();

        if (!startDocumentRead) {
            // LOG.info("PEEK startDocument");
            return eventFactory.createStartDocument();
        }

        XMLEvent event = getParent().nextEvent();

        if (depth == 1 && !endDocumentRead) {
            // LOG.info("PEEK endDocument");
            return eventFactory.createEndDocument();
        }

        // LOG.info("PEEK pass-through {}", event);
        return event;
    }

    @Override
    public boolean hasNext()
    {
        if (!startDocumentRead && super.hasNext()) {
            // LOG.info("HAS-NEXT data available and document not started yet");
            return true;
        }

        if (!done) {
            var v = super.hasNext();
            // LOG.info("HAS-NEXT pass-through {}", v);
            return v;
        }

        if (done && !endDocumentRead) {
            // LOG.info("HAS-NEXT done but document not ended yet");
            return true;
        }

        return false;
    }

    public static DocumentWrappingXmlInputReader wrapInDocument(XMLEventReader aDelegate)
    {
        return new DocumentWrappingXmlInputReader(aDelegate);
    }
}

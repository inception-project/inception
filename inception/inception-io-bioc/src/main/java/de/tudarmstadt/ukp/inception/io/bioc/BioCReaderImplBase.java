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
package de.tudarmstadt.ukp.inception.io.bioc;

import static de.tudarmstadt.ukp.inception.support.xml.XmlParserUtils.isStartElement;
import static java.util.Arrays.asList;

import java.io.IOException;
import java.io.InputStream;
import java.util.Optional;

import javax.xml.stream.XMLEventReader;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.events.XMLEvent;

import org.apache.uima.UimaContext;
import org.apache.uima.collection.CollectionException;
import org.apache.uima.resource.ResourceInitializationException;
import org.dkpro.core.api.io.JCasResourceCollectionReader_ImplBase;
import org.dkpro.core.api.resources.CompressionUtils;

import de.tudarmstadt.ukp.inception.support.xml.XmlParserUtils;

public abstract class BioCReaderImplBase
    extends JCasResourceCollectionReader_ImplBase
    implements BioCComponent
{
    private InputStream is;
    private XMLInputFactory xmlInputFactory;
    private XMLEventReader xmlEventReader;
    private Resource res;

    private String collectionSource;
    private String collectionDate;
    private String collectionKey;

    @Override
    public void initialize(UimaContext aContext) throws ResourceInitializationException
    {
        super.initialize(aContext);

        xmlInputFactory = XmlParserUtils.newXmlInputFactory();
    }

    public String getCollectionSource()
    {
        return collectionSource;
    }

    public String getCollectionDate()
    {
        return collectionDate;
    }

    public String getCollectionKey()
    {
        return collectionKey;
    }

    @Override
    public void destroy()
    {
        closeFile();
        super.destroy();
    }

    public Resource currentResource()
    {
        return res;
    }

    public XMLEventReader getXmlEventReader()
    {
        return xmlEventReader;
    }

    protected boolean isFileOpen()
    {
        return is != null;
    }

    protected void openNextFile() throws IOException, XMLStreamException, CollectionException
    {
        if (!super.hasNext()) {
            return;
        }

        res = nextFile();
        is = CompressionUtils.getInputStream(res.getLocation(), res.getInputStream());
        xmlEventReader = xmlInputFactory.createXMLEventReader(is);
    }

    protected boolean seekNextBioCDocumentInFile() throws XMLStreamException
    {
        XMLEvent e = null;
        while ((e = xmlEventReader.peek()) != null) {
            if (isStartElement(e, E_DOCUMENT)) {
                return true;
            }
            else {
                xmlEventReader.next();
            }
        }

        closeFile();

        return false;
    }

    protected void closeFile()
    {
        collectionKey = null;
        collectionSource = null;
        collectionDate = null;
        res = null;
        xmlEventReader = null;
        if (is != null) {
            try {
                is.close();
            }
            catch (IOException e) {
                // Ignore
            }
        }
    }

    protected void readCollectionMetdata() throws XMLStreamException
    {
        if (isFileOpen()) {
            collectionSource = null;
            collectionDate = null;
            collectionKey = null;
            while ((collectionSource == null || collectionDate == null || collectionKey == null)
                    && xmlEventReader.hasNext()) {
                var event = xmlEventReader.nextEvent();

                tryReadingMetadata(event, E_SOURCE).ifPresent($ -> collectionSource = $);
                tryReadingMetadata(event, E_DATE).ifPresent($ -> collectionDate = $);
                tryReadingMetadata(event, E_KEY).ifPresent($ -> collectionKey = $);

                if (xmlEventReader.hasNext()) {
                    var nextEvent = xmlEventReader.peek();
                    if (nextEvent.isStartElement() && asList(E_DOCUMENT, E_INFON)
                            .contains(nextEvent.asStartElement().getName().getLocalPart())) {
                        // Make sure we do not consume the documents while looking for collection
                        // metadata. While all metadata fields are mandatory in BioC, it does not
                        // mean that some of them may not be missing anyway...
                        break;
                    }
                }
            }
        }
    }

    private Optional<String> tryReadingMetadata(XMLEvent event, String element)
        throws XMLStreamException
    {
        if (event.isStartElement()) {
            if (event.asStartElement().getName().getLocalPart().equals(element)) {
                event = xmlEventReader.nextEvent();
                if (event.isCharacters()) {
                    return Optional.of(event.asCharacters().getData());
                }
                else if (!event.isEndElement()) {
                    xmlEventReader.next(); // Reader closing element
                }
            }
        }

        return Optional.empty();
    }
}

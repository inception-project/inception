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

import static de.tudarmstadt.ukp.inception.io.bioc.xml.DocumentWrappingXmlInputReader.wrapInDocument;
import static java.util.stream.Collectors.toList;

import java.io.IOException;
import java.util.Set;

import javax.xml.stream.XMLStreamException;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.transform.sax.SAXResult;
import javax.xml.transform.stax.StAXSource;

import org.apache.uima.UimaContext;
import org.apache.uima.collection.CollectionException;
import org.apache.uima.jcas.JCas;
import org.apache.uima.resource.ResourceInitializationException;
import org.dkpro.core.api.xml.type.XmlDocument;
import org.dkpro.core.api.xml.type.XmlElement;
import org.xml.sax.SAXException;

import de.tudarmstadt.ukp.dkpro.core.api.metadata.type.MetaDataStringField;
import de.tudarmstadt.ukp.dkpro.core.api.segmentation.type.Sentence;
import de.tudarmstadt.ukp.inception.io.xml.dkprocore.CasXmlHandler;
import de.tudarmstadt.ukp.inception.io.xml.dkprocore.CasXmlHandler.ElementListener;
import de.tudarmstadt.ukp.inception.support.xml.XmlParserUtils;

public class BioCXmlDocumentReader
    extends BioCReaderImplBase
{
    private static final Set<String> ELEMENTS_NOT_TO_TO_CAPTURE = Set.of(E_OFFSET, E_INFON, E_ID);

    private Transformer transformer;
    private boolean documentAvailable = false;

    private String source;
    private String date;
    private String key;

    @Override
    public void initialize(UimaContext aContext) throws ResourceInitializationException
    {
        super.initialize(aContext);

        try {
            transformer = XmlParserUtils.newTransformerFactory().newTransformer();
        }
        catch (TransformerConfigurationException e) {
            throw new ResourceInitializationException(e);
        }

        try {
            documentAvailable = seekNextBioCDocument();
        }
        catch (XMLStreamException | CollectionException | IOException e) {
            throw new ResourceInitializationException(e);
        }
    }

    @Override
    public boolean hasNext() throws IOException, CollectionException
    {
        return documentAvailable;
    }

    @Override
    public void getNext(JCas aJCas) throws IOException, CollectionException
    {
        initCas(aJCas, currentResource());

        addMetadataField(aJCas, "key", key);
        addMetadataField(aJCas, "source", source);
        addMetadataField(aJCas, "date", date);

        CasXmlHandler handler = new CasXmlHandler(aJCas);
        handler.addListener(new ElementListener()
        {
            @Override
            public void startDocument(XmlDocument aDocument) throws SAXException
            {
                handler.startElement(null, null, E_COLLECTION, null);
            }

            @Override
            public void endDocument(XmlDocument aDocument) throws SAXException
            {
                handler.endElement(null, null, E_COLLECTION);
            }

            @Override
            public void startElement(XmlElement aElement)
            {
                if (ELEMENTS_NOT_TO_TO_CAPTURE.contains(aElement.getQName())) {
                    handler.captureText(false);
                }
            }
        });

        try {
            transformer.transform(new StAXSource(wrapInDocument(getXmlEventReader())),
                    new SAXResult(handler));
        }
        catch (TransformerException | XMLStreamException e) {
            throw new IOException(e);
        }

        transferAnnotations(aJCas);

        try {
            documentAvailable = seekNextBioCDocument();
        }
        catch (XMLStreamException e) {
            throw new IOException(e);
        }
    }

    private void addMetadataField(JCas aJCas, String aKey, String aValue)
    {
        var keyField = new MetaDataStringField(aJCas);
        keyField.setKey(aKey);
        keyField.setValue(aValue);
        keyField.addToIndexes();
    }

    private boolean seekNextBioCDocument()
        throws XMLStreamException, CollectionException, IOException
    {
        if (!isFileOpen()) {
            openNextFile();

            if (isFileOpen()) {
                var xmlEventReader = getXmlEventReader();

                source = null;
                date = null;
                key = null;
                while ((source == null || date == null || key == null)
                        && xmlEventReader.hasNext()) {
                    var event = xmlEventReader.nextEvent();
                    if (event.isStartElement()) {
                        if (event.asStartElement().getName().getLocalPart().equals(E_KEY)) {
                            event = xmlEventReader.nextEvent();
                            key = event.asCharacters().getData();
                            xmlEventReader.nextEvent(); // Reader closing element
                        }
                        else if (event.asStartElement().getName().getLocalPart().equals(E_SOURCE)) {
                            event = xmlEventReader.nextEvent();
                            source = event.asCharacters().getData();
                            xmlEventReader.nextEvent(); // Reader closing element
                        }
                        else if (event.asStartElement().getName().getLocalPart().equals(E_DATE)) {
                            event = xmlEventReader.nextEvent();
                            date = event.asCharacters().getData();
                            xmlEventReader.nextEvent(); // Reader closing element
                        }
                    }
                }
            }
        }

        if (isFileOpen()) {
            return seekNextBioCDocumentInFile();
        }

        return false;
    }

    @Override
    protected void closeFile()
    {
        key = null;
        source = null;
        date = null;
        super.closeFile();
    }

    private void transferAnnotations(JCas aJCas)
    {
        transferSentences(aJCas);
    }

    private void transferSentences(JCas aJCas)
    {
        var sentenceElements = aJCas.select(XmlElement.class) //
                .filter(e -> E_SENTENCE.equals(e.getQName()))//
                .collect(toList());

        for (var sentenceElement : sentenceElements) {
            var sentence = new Sentence(aJCas, sentenceElement.getBegin(),
                    sentenceElement.getEnd());
            sentence.trim();
            sentence.addToIndexes();

            // We do not remove the sentence elements from the XML tree. That way, we do not have to
            // re-generated them on export. It means we cannot change the boundaries of sentences,
            // but if we did that we would be running out-of-sync with the offsets referenced in
            // the BioC file anyway. That is a potential drawback of the XML-based approach to
            // handling BioC files.
            // XmlNodeUtils.removeFromTree(sentenceElement);
        }
    }
}

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

import static de.tudarmstadt.ukp.inception.io.bioc.BioCComponent.addCollectionMetadataField;
import static de.tudarmstadt.ukp.inception.io.bioc.xml.DocumentWrappingXmlInputReader.wrapInDocument;

import java.io.IOException;

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

import de.tudarmstadt.ukp.inception.io.bioc.xml.BioC2XmlCas;
import de.tudarmstadt.ukp.inception.io.xml.dkprocore.CasXmlHandler;
import de.tudarmstadt.ukp.inception.io.xml.dkprocore.CasXmlHandler.ElementListener;
import de.tudarmstadt.ukp.inception.support.xml.XmlParserUtils;

/**
 * @deprecated Experimental code that was deprecated in favor of {@link BioCReader}
 */
@Deprecated
public class BioCXmlDocumentReader
    extends BioCReaderImplBase
{
    private Transformer transformer;
    private boolean documentAvailable = false;

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

        addCollectionMetadataField(aJCas, E_KEY, getCollectionKey());
        addCollectionMetadataField(aJCas, E_SOURCE, getCollectionSource());
        addCollectionMetadataField(aJCas, E_DATE, getCollectionDate());

        CasXmlHandler handler = new CasXmlHandler(aJCas);
        handler.addListener(newElementFilter(handler));

        try {
            transformer.transform(new StAXSource(wrapInDocument(getXmlEventReader())),
                    new SAXResult(handler));
        }
        catch (TransformerException | XMLStreamException e) {
            throw new IOException(e);
        }

        new BioC2XmlCas().transferAnnotations(aJCas);

        try {
            documentAvailable = seekNextBioCDocument();
        }
        catch (XMLStreamException e) {
            throw new IOException(e);
        }
    }

    private ElementListener newElementFilter(CasXmlHandler handler)
    {
        return new ElementListener()
        {
            @Override
            public void startDocument(XmlDocument aDocument) throws SAXException
            {
                handler.startElement(null, null, E_COLLECTION, null);
                handler.captureText(false);
            }

            @Override
            public void endDocument(XmlDocument aDocument) throws SAXException
            {
                handler.endElement(null, null, E_COLLECTION);
            }

            @Override
            public void startElement(XmlElement aElement)
            {
                var parent = aElement.getParent();
                if (parent != null
                        && (E_PASSAGE.equals(parent.getQName())
                                || E_SENTENCE.equals(parent.getQName()))
                        && E_TEXT.equals(aElement.getQName())) {
                    handler.captureText(true);
                }
                else {
                    handler.captureText(false);
                }
            }
        };
    }

    private boolean seekNextBioCDocument()
        throws XMLStreamException, CollectionException, IOException
    {
        if (!isFileOpen()) {
            openNextFile();

            readCollectionMetdata();
        }

        if (isFileOpen()) {
            return seekNextBioCDocumentInFile();
        }

        return false;
    }

}

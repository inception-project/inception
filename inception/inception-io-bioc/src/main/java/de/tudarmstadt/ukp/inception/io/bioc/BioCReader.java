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

import java.io.IOException;
import java.util.Optional;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;
import javax.xml.stream.XMLStreamException;

import org.apache.uima.UimaContext;
import org.apache.uima.collection.CollectionException;
import org.apache.uima.fit.factory.JCasBuilder;
import org.apache.uima.jcas.JCas;
import org.apache.uima.resource.ResourceInitializationException;

import de.tudarmstadt.ukp.inception.io.bioc.model.BioCDocument;
import de.tudarmstadt.ukp.inception.io.bioc.model.BioCToCas;

public class BioCReader
    extends BioCReaderImplBase
{
    private JAXBContext context;
    private Unmarshaller unmarshaller;
    private Optional<BioCDocument> nextDocument;

    @Override
    public void initialize(UimaContext aContext) throws ResourceInitializationException
    {
        super.initialize(aContext);

        try {
            context = JAXBContext.newInstance(BioCDocument.class);
        }
        catch (JAXBException e) {
            throw new ResourceInitializationException(e);
        }

        try {
            nextDocument = nextBioCDocument();
        }
        catch (CollectionException | XMLStreamException | JAXBException | IOException e) {
            throw new ResourceInitializationException(e);
        }
    }

    @Override
    public void getNext(JCas aJCas) throws IOException, CollectionException
    {
        initCas(aJCas, currentResource());

        addCollectionMetadataField(aJCas, E_KEY, getCollectionKey());
        addCollectionMetadataField(aJCas, E_SOURCE, getCollectionSource());
        addCollectionMetadataField(aJCas, E_DATE, getCollectionDate());

        var document = nextDocument.get();

        // if (getCollectionSource() != null) {
        // DocumentMetaData.get(aJCas).setDocumentId(getCollectionSource());
        // }
        //
        // if (document.getId() != null) {
        // DocumentMetaData.get(aJCas).setDocumentId(document.getId());
        // }

        JCasBuilder jb = new JCasBuilder(aJCas);
        new BioCToCas().readDocument(jb, document);
        jb.close();

        try {
            nextDocument = nextBioCDocument();
        }
        catch (XMLStreamException | JAXBException e) {
            throw new IOException(e);
        }
    }

    @Override
    public boolean hasNext() throws IOException, CollectionException
    {
        return nextDocument.isPresent();
    }

    private Optional<BioCDocument> nextBioCDocument()
        throws XMLStreamException, JAXBException, CollectionException, IOException
    {
        if (!isFileOpen()) {
            openNextFile();
            readCollectionMetdata();
        }

        if (isFileOpen()) {
            return nextBioCDocumentInFile();
        }

        return Optional.empty();
    }

    @Override
    protected void openNextFile() throws IOException, XMLStreamException, CollectionException
    {
        super.openNextFile();
        try {
            unmarshaller = context.createUnmarshaller();
        }
        catch (JAXBException e) {
            new IOException(e);
        }
    }

    @Override
    protected void closeFile()
    {
        unmarshaller = null;
        super.closeFile();
    }

    private Optional<BioCDocument> nextBioCDocumentInFile() throws XMLStreamException, JAXBException
    {
        if (seekNextBioCDocumentInFile()) {
            var document = unmarshaller.unmarshal(getXmlEventReader(), BioCDocument.class)
                    .getValue();
            return Optional.of(document);
        }

        closeFile();

        return Optional.empty();
    }
}

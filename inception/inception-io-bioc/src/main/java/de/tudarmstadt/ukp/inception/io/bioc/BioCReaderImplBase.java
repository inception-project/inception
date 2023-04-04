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

import java.io.IOException;
import java.io.InputStream;

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
{
    public static final String E_DOCUMENT = "document";
    public static final String E_COLLECTION = "collection";
    public static final String E_SENTENCE = "sentence";
    public static final String E_INFON = "infon";
    public static final String E_OFFSET = "offset";
    public static final String E_KEY = "key";
    public static final String E_DATE = "date";
    public static final String E_ID = "id";
    public static final String E_SOURCE = "source";

    private InputStream is;
    private XMLInputFactory xmlInputFactory;
    private XMLEventReader xmlEventReader;
    private Resource res;

    @Override
    public void initialize(UimaContext aContext) throws ResourceInitializationException
    {
        super.initialize(aContext);

        xmlInputFactory = XmlParserUtils.newXmlInputFactory();
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
}

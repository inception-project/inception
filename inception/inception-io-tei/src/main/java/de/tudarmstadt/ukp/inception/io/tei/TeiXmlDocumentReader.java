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
package de.tudarmstadt.ukp.inception.io.tei;

import static org.apache.uima.fit.util.FSCollectionFactory.createFSArray;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;

import org.apache.uima.UimaContext;
import org.apache.uima.collection.CollectionException;
import org.apache.uima.fit.descriptor.MimeTypeCapability;
import org.apache.uima.fit.descriptor.ResourceMetaData;
import org.apache.uima.fit.descriptor.TypeCapability;
import org.apache.uima.jcas.JCas;
import org.apache.uima.resource.ResourceInitializationException;
import org.dkpro.core.api.io.JCasResourceCollectionReader_ImplBase;
import org.dkpro.core.api.parameter.MimeTypes;
import org.dkpro.core.api.resources.CompressionUtils;
import org.dkpro.core.api.xml.type.XmlDocument;
import org.dkpro.core.api.xml.type.XmlElement;
import org.dkpro.core.api.xml.type.XmlNode;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

import de.tudarmstadt.ukp.inception.io.xml.dkprocore.CasXmlHandler;
import de.tudarmstadt.ukp.inception.io.xml.dkprocore.CasXmlHandler.ElementListener;
import de.tudarmstadt.ukp.inception.support.xml.XmlParserUtils;

@ResourceMetaData(name = "TEI-XML Document Reader")
@MimeTypeCapability({ MimeTypes.APPLICATION_XML, MimeTypes.TEXT_XML })
@TypeCapability(outputs = { //
        "de.tudarmstadt.ukp.dkpro.core.api.metadata.type.DocumentMetaData", //
        "org.dkpro.core.api.xml.type.XmlAttribute", //
        "org.dkpro.core.api.xml.type.XmlDocument", //
        "org.dkpro.core.api.xml.type.XmlElement", //
        "org.dkpro.core.api.xml.type.XmlNode", //
        "org.dkpro.core.api.xml.type.XmlTextNode" })
public class TeiXmlDocumentReader
    extends JCasResourceCollectionReader_ImplBase
{
    private SAXParser parser;

    @Override
    public void initialize(UimaContext aContext) throws ResourceInitializationException
    {
        super.initialize(aContext);

        try {
            parser = XmlParserUtils.newSaxParser();
        }
        catch (ParserConfigurationException | SAXException e) {
            throw new ResourceInitializationException(e);
        }
    }

    @Override
    public void getNext(JCas aJCas) throws IOException, CollectionException
    {
        var res = nextFile();
        initCas(aJCas, res);

        try (var is = new BufferedInputStream(
                CompressionUtils.getInputStream(res.getLocation(), res.getInputStream()))) {

            var source = new InputSource(is);
            source.setPublicId(res.getLocation());
            source.setSystemId(res.getLocation());

            var handler = makeXmlToCasHandler(aJCas);

            parser.parse(source, handler);
        }
        catch (IOException e) {
            throw e;
        }
        catch (Exception e) {
            throw new CollectionException(e);
        }
    }

    private DefaultHandler makeXmlToCasHandler(JCas aJCas)
    {
        var handler = new CasXmlHandler(aJCas);
        handler.captureText(false);
        handler.addListener(new ElementListener()
        {
            private List<XmlNode> captureRoots = new ArrayList<>();

            @Override
            public void startElement(XmlElement aElement)
            {
                if ("text".equals(aElement.getQName()) && !handler.isCapturingText()) {
                    handler.captureText(true);
                    captureRoots.add(aElement);
                }
            }

            @Override
            public void endDocument(XmlDocument aDocument)
            {
                aDocument.setCaptureRoots(createFSArray(aJCas, captureRoots));
            }
        });
        return handler;
    }
}

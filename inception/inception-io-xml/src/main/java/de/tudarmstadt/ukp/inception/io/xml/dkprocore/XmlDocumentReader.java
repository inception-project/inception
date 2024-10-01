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

import java.io.BufferedInputStream;
import java.io.IOException;
import java.util.Set;

import org.apache.uima.collection.CollectionException;
import org.apache.uima.fit.descriptor.ConfigurationParameter;
import org.apache.uima.fit.descriptor.MimeTypeCapability;
import org.apache.uima.fit.descriptor.ResourceMetaData;
import org.apache.uima.fit.descriptor.TypeCapability;
import org.apache.uima.jcas.JCas;
import org.dkpro.core.api.io.JCasResourceCollectionReader_ImplBase;
import org.dkpro.core.api.parameter.MimeTypes;
import org.dkpro.core.api.resources.CompressionUtils;
import org.xml.sax.InputSource;

import de.tudarmstadt.ukp.inception.support.xml.XmlParserUtils;

// import eu.openminted.share.annotations.api.Component;
// import eu.openminted.share.annotations.api.constants.OperationType;

/**
 * Simple XML reader which loads all text from the XML file into the CAS document text and generates
 * XML annotations for all XML elements, attributes and text nodes.
 * 
 * @see XmlDocumentWriter
 */
// @Component(value = OperationType.READER)
@ResourceMetaData(name = "XML Document Reader")
// @DocumentationResource("${docbase}/format-reference.html#format-${command}")
// @Parameters(
// exclude = {
// XmlDocumentReader.PARAM_SOURCE_LOCATION })
@MimeTypeCapability({ MimeTypes.APPLICATION_XML, MimeTypes.TEXT_XML })
@TypeCapability(outputs = { //
        "de.tudarmstadt.ukp.dkpro.core.api.metadata.type.DocumentMetaData", //
        "org.dkpro.core.api.xml.type.XmlAttribute", //
        "org.dkpro.core.api.xml.type.XmlDocument", //
        "org.dkpro.core.api.xml.type.XmlElement", //
        "org.dkpro.core.api.xml.type.XmlNode", //
        "org.dkpro.core.api.xml.type.XmlTextNode" })
public class XmlDocumentReader
    extends JCasResourceCollectionReader_ImplBase
{
    public static final String PARAM_BLOCK_ELEMENTS = "blockElements";
    @ConfigurationParameter(name = PARAM_BLOCK_ELEMENTS, mandatory = false)
    private Set<String> blockElements;

    public static final String PARAM_SPLIT_SENTENCES_IN_BLOCK_ELEMENTS = "splitSentencesInBlockElements";
    @ConfigurationParameter(name = PARAM_SPLIT_SENTENCES_IN_BLOCK_ELEMENTS, defaultValue = "true")
    private boolean splitSentencesInBlockElements;

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

            var handler = new CasXmlHandler(aJCas);
            handler.setBlockElements(blockElements);
            handler.setSplitSentencesInBlockElements(splitSentencesInBlockElements);

            var parser = XmlParserUtils.newSaxParser();
            parser.parse(source, handler);
        }
        catch (IOException e) {
            throw e;
        }
        catch (Exception e) {
            throw new CollectionException(e);
        }
    }
}

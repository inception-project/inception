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

import static org.dkpro.core.api.parameter.ComponentParameters.DEFAULT_ENCODING;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.io.IOUtils;
import org.apache.uima.UimaContext;
import org.apache.uima.collection.CollectionException;
import org.apache.uima.fit.descriptor.ConfigurationParameter;
import org.apache.uima.fit.descriptor.MimeTypeCapability;
import org.apache.uima.fit.descriptor.ResourceMetaData;
import org.apache.uima.fit.descriptor.TypeCapability;
import org.apache.uima.jcas.JCas;
import org.apache.uima.resource.ResourceInitializationException;
import org.dkpro.core.api.io.JCasResourceCollectionReader_ImplBase;
import org.dkpro.core.api.parameter.ComponentParameters;
import org.dkpro.core.api.parameter.MimeTypes;
import org.dkpro.core.api.resources.CompressionUtils;
import org.jsoup.Jsoup;
import org.jsoup.select.NodeTraversor;

import com.ibm.icu.text.CharsetDetector;

import de.tudarmstadt.ukp.dkpro.core.api.segmentation.type.Heading;
import de.tudarmstadt.ukp.dkpro.core.api.segmentation.type.Paragraph;
import eu.openminted.share.annotations.api.DocumentationResource;

/**
 * Reads the contents of a given URL and strips the HTML. Returns the textual contents. Also
 * recognizes headings and paragraphs.
 */
@ResourceMetaData(name = "HTML Reader")
@DocumentationResource("${docbase}/format-reference.html#format-${command}")
@MimeTypeCapability({ MimeTypes.APPLICATION_XHTML, MimeTypes.TEXT_HTML })
@TypeCapability(outputs = { //
        "de.tudarmstadt.ukp.dkpro.core.api.metadata.type.DocumentMetaData",
        "de.tudarmstadt.ukp.dkpro.core.api.segmentation.type.Heading",
        "de.tudarmstadt.ukp.dkpro.core.api.segmentation.type.Paragraph",
        "org.dkpro.core.api.xml.type.XmlAttribute", "org.dkpro.core.api.xml.type.XmlDocument",
        "org.dkpro.core.api.xml.type.XmlElement", "org.dkpro.core.api.xml.type.XmlNode",
        "org.dkpro.core.api.xml.type.XmlTextNode" })
public class HtmlDocumentReader
    extends JCasResourceCollectionReader_ImplBase
{
    /**
     * Automatically detect encoding.
     *
     * @see CharsetDetector
     */
    public static final String ENCODING_AUTO = "auto";

    /**
     * Name of configuration parameter that contains the character encoding used by the input files.
     */
    public static final String PARAM_SOURCE_ENCODING = ComponentParameters.PARAM_SOURCE_ENCODING;
    @ConfigurationParameter(name = PARAM_SOURCE_ENCODING, defaultValue = DEFAULT_ENCODING)
    private String sourceEncoding;

    /**
     * Normalize whitespace.
     */
    public static final String PARAM_NORMALIZE_WHITESPACE = "normalizeWhitespace";
    @ConfigurationParameter(name = PARAM_NORMALIZE_WHITESPACE, defaultValue = "true")
    private boolean normalizeWhitespace;

    private Map<String, Integer> mappings = new HashMap<>();

    @Override
    public void initialize(UimaContext aContext) throws ResourceInitializationException
    {
        super.initialize(aContext);

        mappings.put("h1", Heading.type);
        mappings.put("h2", Heading.type);
        mappings.put("h3", Heading.type);
        mappings.put("h4", Heading.type);
        mappings.put("h5", Heading.type);
        mappings.put("h6", Heading.type);
        mappings.put("p", Paragraph.type);
    }

    @Override
    public void getNext(JCas aJCas) throws IOException, CollectionException
    {
        Resource res = nextFile();
        initCas(aJCas, res);

        String html;
        try (var is = new BufferedInputStream(
                CompressionUtils.getInputStream(res.getLocation(), res.getInputStream()))) {

            if (ENCODING_AUTO.equals(sourceEncoding)) {
                CharsetDetector detector = new CharsetDetector();
                html = IOUtils.toString(detector.getReader(is, null));
            }
            else {
                html = IOUtils.toString(is, sourceEncoding);
            }
        }

        var doc = Jsoup.parse(html);

        var visitor = new CasXmlNodeVisitor(aJCas, normalizeWhitespace);
        visitor.setMappings(mappings);

        NodeTraversor.traverse(visitor, doc);
    }
}

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

import static javax.xml.transform.OutputKeys.INDENT;
import static javax.xml.transform.OutputKeys.METHOD;
import static javax.xml.transform.OutputKeys.OMIT_XML_DECLARATION;

import java.io.OutputStream;

import javax.xml.transform.sax.SAXTransformerFactory;
import javax.xml.transform.sax.TransformerHandler;
import javax.xml.transform.stream.StreamResult;

import org.apache.uima.analysis_engine.AnalysisEngineProcessException;
import org.apache.uima.fit.descriptor.ConfigurationParameter;
import org.apache.uima.fit.descriptor.MimeTypeCapability;
import org.apache.uima.fit.descriptor.ResourceMetaData;
import org.apache.uima.fit.descriptor.TypeCapability;
import org.apache.uima.jcas.JCas;
import org.dkpro.core.api.io.JCasFileWriter_ImplBase;
import org.dkpro.core.api.parameter.ComponentParameters;
import org.dkpro.core.api.parameter.MimeTypes;

import de.tudarmstadt.ukp.inception.support.xml.XmlParserUtils;

/**
 * Simple XML write takes the XML annotations for elements, attributes and text nodes and renders
 * them into an XML file.
 * 
 * @see XmlDocumentReader
 */
@ResourceMetaData(name = "XML Document Writer")
// @DocumentationResource("${docbase}/format-reference.html#format-${command}")
@MimeTypeCapability({ MimeTypes.APPLICATION_XML, MimeTypes.TEXT_XML })
@TypeCapability(inputs = { "de.tudarmstadt.ukp.dkpro.core.api.metadata.type.DocumentMetaData",
        "org.dkpro.core.api.xml.type.XmlAttribute", "org.dkpro.core.api.xml.type.XmlDocument",
        "org.dkpro.core.api.xml.type.XmlElement", "org.dkpro.core.api.xml.type.XmlNode",
        "org.dkpro.core.api.xml.type.XmlTextNode" })
public class XmlDocumentWriter
    extends JCasFileWriter_ImplBase
{
    /**
     * Specify the suffix of output files. Default value <code>.txt</code>. If the suffix is not
     * needed, provide an empty string as value.
     */
    public static final String PARAM_FILENAME_EXTENSION = ComponentParameters.PARAM_FILENAME_EXTENSION;
    @ConfigurationParameter(name = PARAM_FILENAME_EXTENSION, mandatory = true, defaultValue = ".xml")
    private String filenameSuffix;

    /**
     * Whether to omit the XML preamble.
     */
    public static final String PARAM_OMIT_XML_DECLARATION = "omitXmlDeclaration";
    @ConfigurationParameter(name = PARAM_OMIT_XML_DECLARATION, mandatory = true, defaultValue = "true")
    private boolean omitXmlDeclaration;

    /**
     * Output method.
     */
    public static final String PARAM_OUTPUT_METHOD = "outputMethod";
    @ConfigurationParameter(name = PARAM_OUTPUT_METHOD, mandatory = true, defaultValue = "xml")
    private String outputMethod;

    /**
     * Indent output .
     */
    public static final String PARAM_INDENT = "indent";
    @ConfigurationParameter(name = PARAM_INDENT, mandatory = true, defaultValue = "false")
    private boolean indent;

    @Override
    public void process(JCas aJCas) throws AnalysisEngineProcessException
    {
        try (OutputStream docOS = getOutputStream(aJCas, filenameSuffix)) {
            SAXTransformerFactory tf = XmlParserUtils.newTransformerFactory();
            TransformerHandler th = tf.newTransformerHandler();
            if (omitXmlDeclaration) {
                th.getTransformer().setOutputProperty(OMIT_XML_DECLARATION, "yes");
            }
            th.getTransformer().setOutputProperty(METHOD, outputMethod);
            th.getTransformer().setOutputProperty(INDENT, indent ? "yes" : "no");
            th.setResult(new StreamResult(docOS));

            Cas2SaxEvents serializer = new Cas2SaxEvents(th);
            serializer.process(aJCas);
        }
        catch (Exception e) {
            throw new AnalysisEngineProcessException(e);
        }
    }
}

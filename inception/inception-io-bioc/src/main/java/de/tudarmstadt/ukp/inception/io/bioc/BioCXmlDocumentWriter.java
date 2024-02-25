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

import static javax.xml.transform.OutputKeys.INDENT;
import static javax.xml.transform.OutputKeys.METHOD;
import static javax.xml.transform.OutputKeys.OMIT_XML_DECLARATION;

import java.io.IOException;
import java.io.OutputStream;
import java.io.StringReader;
import java.io.StringWriter;

import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;

import org.apache.uima.analysis_engine.AnalysisEngineProcessException;
import org.apache.uima.fit.descriptor.ConfigurationParameter;
import org.apache.uima.jcas.JCas;
import org.dkpro.core.api.io.JCasFileWriter_ImplBase;
import org.xml.sax.SAXException;

import de.tudarmstadt.ukp.inception.io.bioc.xml.Cas2BioCSaxEvents;
import de.tudarmstadt.ukp.inception.support.xml.XmlParserUtils;

/**
 * @deprecated Experimental code that was deprecated in favor of {@link BioCWriter}
 */
@Deprecated
public class BioCXmlDocumentWriter
    extends JCasFileWriter_ImplBase
{
    /**
     * Indent output .
     */
    public static final String PARAM_INDENT = "indent";
    @ConfigurationParameter(name = PARAM_INDENT, mandatory = true, defaultValue = "false")
    private boolean indent;

    @Override
    public void process(JCas aJCas) throws AnalysisEngineProcessException
    {
        if (indent) {
            String xmlString;
            try (var stringWriter = new StringWriter()) {
                var tf = XmlParserUtils.newTransformerFactory();
                var th = tf.newTransformerHandler();
                th.setResult(new StreamResult(stringWriter));
                var serializer = new Cas2BioCSaxEvents(th);
                serializer.process(aJCas);
                xmlString = stringWriter.toString();
            }
            catch (IOException | SAXException | TransformerConfigurationException e) {
                throw new AnalysisEngineProcessException(e);
            }

            try (OutputStream docOS = getOutputStream(aJCas, ".xml")) {
                var tf = XmlParserUtils.newTransformerFactory();
                tf.setAttribute("indent-number", 2);
                var transformer = tf.newTransformer();
                transformer.setOutputProperty(OMIT_XML_DECLARATION, "yes");
                transformer.setOutputProperty(INDENT, "yes");
                transformer.transform(new StreamSource(new StringReader(xmlString)),
                        new StreamResult(docOS));
            }
            catch (IOException | TransformerException e) {
                throw new AnalysisEngineProcessException(e);
            }
        }
        else {
            try (OutputStream docOS = getOutputStream(aJCas, ".xml")) {
                var tf = XmlParserUtils.newTransformerFactory();
                var th = tf.newTransformerHandler();
                th.getTransformer().setOutputProperty(OMIT_XML_DECLARATION, "yes");
                th.getTransformer().setOutputProperty(METHOD, "xml");
                th.getTransformer().setOutputProperty(INDENT, "no");
                th.setResult(new StreamResult(docOS));

                var serializer = new Cas2BioCSaxEvents(th);
                serializer.process(aJCas);
            }
            catch (IOException | SAXException | TransformerConfigurationException e) {
                throw new AnalysisEngineProcessException(e);
            }
        }
    }
}

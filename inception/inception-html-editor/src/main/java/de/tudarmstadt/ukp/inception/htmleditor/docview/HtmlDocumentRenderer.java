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
package de.tudarmstadt.ukp.inception.htmleditor.docview;

import static javax.xml.transform.OutputKeys.INDENT;
import static javax.xml.transform.OutputKeys.METHOD;
import static javax.xml.transform.OutputKeys.OMIT_XML_DECLARATION;
import static org.apache.commons.lang3.StringUtils.equalsIgnoreCase;
import static org.apache.uima.fit.util.JCasUtil.selectSingle;

import java.io.IOException;
import java.io.StringWriter;
import java.io.Writer;

import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.sax.SAXTransformerFactory;
import javax.xml.transform.sax.TransformerHandler;
import javax.xml.transform.stream.StreamResult;

import org.apache.uima.cas.CAS;
import org.apache.uima.cas.CASException;
import org.apache.uima.fit.util.JCasUtil;
import org.dkpro.core.api.xml.type.XmlDocument;
import org.dkpro.core.api.xml.type.XmlElement;
import org.xml.sax.ContentHandler;
import org.xml.sax.SAXException;

import de.tudarmstadt.ukp.clarin.webanno.support.xml.TextSanitizingContentHandler;
import de.tudarmstadt.ukp.inception.io.xml.dkprocore.Cas2SaxEvents;
import de.tudarmstadt.ukp.inception.support.xml.XmlParserUtils;

public class HtmlDocumentRenderer
{
    private boolean renderOnlyBody = true;

    public void setRenderOnlyBody(boolean aRenderOnlyBody)
    {
        renderOnlyBody = aRenderOnlyBody;
    }

    public String render(CAS aCas)
        throws IOException, TransformerConfigurationException, CASException, SAXException
    {
        try (Writer out = new StringWriter()) {
            SAXTransformerFactory tf = XmlParserUtils.newTransformerFactory();
            TransformerHandler th = tf.newTransformerHandler();
            th.getTransformer().setOutputProperty(OMIT_XML_DECLARATION, "yes");
            th.getTransformer().setOutputProperty(METHOD, "xml");
            th.getTransformer().setOutputProperty(INDENT, "no");
            th.setResult(new StreamResult(out));

            ContentHandler sh = new TextSanitizingContentHandler(th);

            if (!JCasUtil.exists(aCas.getJCas(), XmlDocument.class)) {
                String text = aCas.getDocumentText();

                sh.startDocument();
                sh.characters(text.toCharArray(), 0, text.length());
                sh.endDocument();
                return out.toString();
            }

            // The HtmlDocumentReader only extracts text from the body. So here we need to limit
            // rendering to the body so that the text and the annotations align properly. Also,
            // we wouldn't want to render anything outside the body anyway.
            var rootElement = selectSingle(aCas.getJCas(), XmlDocument.class).getRoot();

            Cas2SaxEvents serializer = new Cas2SaxEvents(sh);
            if (renderOnlyBody) {
                XmlElement body = rootElement.getChildren().stream() //
                        .filter(e -> e instanceof XmlElement) //
                        .map(e -> (XmlElement) e) //
                        .filter(e -> equalsIgnoreCase("body", e.getQName())) //
                        .findFirst().orElseThrow();
                serializer.process(body);
            }
            else {
                serializer.process(rootElement);
            }

            return out.toString();
        }
    }
}

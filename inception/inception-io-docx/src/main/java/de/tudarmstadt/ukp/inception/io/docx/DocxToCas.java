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
package de.tudarmstadt.ukp.inception.io.docx;

import java.io.IOException;
import java.io.InputStream;

import javax.xml.parsers.ParserConfigurationException;

import org.apache.uima.jcas.JCas;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import de.tudarmstadt.ukp.inception.io.xml.dkprocore.CasXmlHandler;
import de.tudarmstadt.ukp.inception.support.xml.XmlParserUtils;

public class DocxToCas
{
    public void loadXml(JCas aJCas, InputStream aInputStream) throws IOException
    {
        var handler = new CasXmlHandler(aJCas);
        handler.captureText(true);

        try {
            var parser = XmlParserUtils.newSaxParser();
            parser.parse(new InputSource(aInputStream), handler);
        }
        catch (ParserConfigurationException | SAXException e) {
            throw new IOException(e);
        }
    }
}

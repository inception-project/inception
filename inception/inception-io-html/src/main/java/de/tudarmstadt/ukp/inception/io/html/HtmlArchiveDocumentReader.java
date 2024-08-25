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
package de.tudarmstadt.ukp.inception.io.html;

import java.io.IOException;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import org.apache.uima.collection.CollectionException;
import org.apache.uima.fit.descriptor.ConfigurationParameter;
import org.apache.uima.fit.descriptor.MimeTypeCapability;
import org.apache.uima.fit.descriptor.ResourceMetaData;
import org.apache.uima.fit.descriptor.TypeCapability;
import org.apache.uima.jcas.JCas;
import org.dkpro.core.api.io.JCasResourceCollectionReader_ImplBase;
import org.dkpro.core.api.parameter.MimeTypes;
import org.jsoup.Jsoup;
import org.jsoup.select.NodeTraversor;

import de.tudarmstadt.ukp.inception.io.html.dkprocore.CasXmlNodeVisitor;

/**
 * Reads the contents of a given URL and strips the HTML from a ZIP. Returns the textual contents. v
 */
@ResourceMetaData(name = "HTML Archive Reader")
@MimeTypeCapability({ MimeTypes.APPLICATION_XHTML, MimeTypes.TEXT_HTML })
@TypeCapability(outputs = { //
        "de.tudarmstadt.ukp.dkpro.core.api.metadata.type.DocumentMetaData", //
        "org.dkpro.core.api.xml.type.XmlAttribute", //
        "org.dkpro.core.api.xml.type.XmlDocument", //
        "org.dkpro.core.api.xml.type.XmlElement", //
        "org.dkpro.core.api.xml.type.XmlNode", //
        "org.dkpro.core.api.xml.type.XmlTextNode" })
public class HtmlArchiveDocumentReader
    extends JCasResourceCollectionReader_ImplBase
{
    /**
     * Normalize whitespace.
     */
    public static final String PARAM_NORMALIZE_WHITESPACE = "normalizeWhitespace";
    @ConfigurationParameter(name = PARAM_NORMALIZE_WHITESPACE, defaultValue = "true")
    private boolean normalizeWhitespace;

    @Override
    public void getNext(JCas aJCas) throws IOException, CollectionException
    {
        var res = nextFile();
        initCas(aJCas, res);

        try (var zipFile = new ZipFile(res.getResource().getFile())) {
            for (var zipEnumerate = zipFile.entries(); zipEnumerate.hasMoreElements();) {
                var entry = (ZipEntry) zipEnumerate.nextElement();
                if (entry.getName().endsWith("index.html") && !entry.getName().contains("/")) {
                    var doc = Jsoup.parse(zipFile.getInputStream(entry), "UTF-8", "");
                    var visitor = new CasXmlNodeVisitor(aJCas, normalizeWhitespace);
                    NodeTraversor.traverse(visitor, doc);
                    break;
                }
            }
        }
        catch (IOException e) {
            throw e;
        }
        catch (Exception e) {
            throw new CollectionException(e);
        }
    }
}

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
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import org.apache.uima.UimaContext;
import org.apache.uima.collection.CollectionException;
import org.apache.uima.jcas.JCas;
import org.apache.uima.resource.ResourceInitializationException;
import org.dkpro.core.api.io.JCasResourceCollectionReader_ImplBase;

public class DocxDocumentReader
    extends JCasResourceCollectionReader_ImplBase
{
    @Override
    public void initialize(UimaContext aContext) throws ResourceInitializationException
    {
        super.initialize(aContext);
    }

    @Override
    public void getNext(JCas aJCas) throws IOException, CollectionException
    {
        var res = nextFile();
        initCas(aJCas, res);

        try (var zipFile = new ZipFile(res.getResource().getFile())) {
            for (var entries = zipFile.entries(); entries.hasMoreElements();) {
                var entry = (ZipEntry) entries.nextElement();
                if (entry.getName().equals("word/document.xml")) {
                    var loader = new DocxToCas();
                    loader.loadXml(aJCas, zipFile.getInputStream(entry));
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

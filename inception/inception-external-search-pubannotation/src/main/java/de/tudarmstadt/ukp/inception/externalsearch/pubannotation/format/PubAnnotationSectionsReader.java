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
package de.tudarmstadt.ukp.inception.externalsearch.pubannotation.format;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;

import org.apache.uima.collection.CollectionException;
import org.apache.uima.jcas.JCas;
import org.dkpro.core.api.io.JCasResourceCollectionReader_ImplBase;
import org.dkpro.core.api.resources.CompressionUtils;

import de.tudarmstadt.ukp.dkpro.core.api.segmentation.type.Div;
import de.tudarmstadt.ukp.inception.externalsearch.pubannotation.model.PubAnnotationDocumentSection;
import de.tudarmstadt.ukp.inception.support.json.JSONUtil;

public class PubAnnotationSectionsReader
    extends JCasResourceCollectionReader_ImplBase
{
    @Override
    public void getNext(JCas aCAS) throws IOException, CollectionException
    {
        Resource res = nextFile();
        initCas(aCAS, res);

        try (InputStream is = new BufferedInputStream(
                CompressionUtils.getInputStream(res.getLocation(), res.getInputStream()))) {

            List<PubAnnotationDocumentSection> sections = JSONUtil.getObjectMapper().readValue(is,
                    PubAnnotationDocumentSection.JACKSON_LIST_TYPE_REF);

            StringBuilder sb = new StringBuilder();
            for (PubAnnotationDocumentSection section : sections) {
                if (sb.length() != 0) {
                    sb.append("\n\n");
                }
                int begin = sb.length();
                sb.append(section.getText());
                int end = sb.length();

                Div div = new Div(aCAS, begin, end);
                div.setId(String.valueOf(section.getDivId()));
                div.setDivType(section.getSection());
                div.addToIndexes();
            }

            aCAS.setDocumentText(sb.toString());
        }
    }
}

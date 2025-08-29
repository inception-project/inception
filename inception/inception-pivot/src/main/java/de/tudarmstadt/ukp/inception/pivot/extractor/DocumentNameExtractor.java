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
package de.tudarmstadt.ukp.inception.pivot.extractor;

import java.util.Optional;

import org.apache.uima.cas.text.AnnotationFS;

import de.tudarmstadt.ukp.dkpro.core.api.metadata.type.DocumentMetaData;
import de.tudarmstadt.ukp.inception.pivot.api.extractor.AnnotationExtractor;

public class DocumentNameExtractor
    implements AnnotationExtractor<AnnotationFS, String>
{
    private Object cacheMarker;
    private String documentName;

    @Override
    public String getName()
    {
        return "<document>";
    }

    @Override
    public Class<? extends String> getResultType()
    {
        return String.class;
    }

    @Override
    public boolean isWeak()
    {
        return true;
    }

    @Override
    public Optional<String> getTriggerType()
    {
        // return Optional.of(CAS.TYPE_NAME_DOCUMENT_ANNOTATION);
        return Optional.empty();
    }

    @Override
    public String extract(AnnotationFS aAnn)
    {
        // Avoid fetching the DocumentMetaData annotation each time
        if (cacheMarker == aAnn.getCAS().getDocumentAnnotation()) {
            return documentName;
        }

        cacheMarker = aAnn.getCAS().getDocumentAnnotation();
        documentName = null;

        var dmd = DocumentMetaData.get(aAnn.getCAS());
        if (dmd != null) {
            documentName = dmd.getDocumentTitle();
        }

        return documentName;
    }
}

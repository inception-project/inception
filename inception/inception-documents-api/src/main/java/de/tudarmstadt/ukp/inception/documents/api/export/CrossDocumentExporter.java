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
package de.tudarmstadt.ukp.inception.documents.api.export;

import java.io.IOException;
import java.io.OutputStream;
import java.util.List;
import java.util.Map;

import org.springframework.http.MediaType;

import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationDocument;
import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationFeature;
import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationLayer;
import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationSet;
import de.tudarmstadt.ukp.clarin.webanno.model.SourceDocument;
import de.tudarmstadt.ukp.inception.support.extensionpoint.ExtensionPoint_ImplBase;

public interface CrossDocumentExporter
{
    static final String EXT_CSV = ".csv";
    static final String EXT_JSON = ".json";

    /**
     * @return identifier for the extension unique within the respective
     *         {@link ExtensionPoint_ImplBase}.
     */
    String getId();

    boolean accepts(AnnotationLayer aLayer, AnnotationFeature aFeature);

    void export(OutputStream aOut, AnnotationLayer aLayer, AnnotationFeature aFeature,
            Map<SourceDocument, List<AnnotationDocument>> aAllAnnDocs,
            List<AnnotationSet> aDataOwners)
        throws IOException;

    String getFileExtension();

    MediaType getMediaType();
}

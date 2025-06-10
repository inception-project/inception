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
package de.tudarmstadt.ukp.inception.workload.dynamic;

import static de.tudarmstadt.ukp.clarin.webanno.model.AnnotationDocumentStateChangeFlag.EXPLICIT_ANNOTATOR_USER_ACTION;
import static de.tudarmstadt.ukp.inception.support.uima.FeatureStructureBuilder.buildFS;
import static java.nio.charset.StandardCharsets.UTF_8;
import static org.apache.commons.io.IOUtils.toInputStream;

import de.tudarmstadt.ukp.clarin.webanno.api.casstorage.session.CasStorageSession;
import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationDocument;
import de.tudarmstadt.ukp.dkpro.core.api.ner.type.NamedEntity;
import de.tudarmstadt.ukp.inception.documents.api.DocumentService;

public class Fixtures
{
    public static void importTestSourceDocumentAndAddNamedEntity(DocumentService documentService,
            AnnotationDocument annotationDocument)
        throws Exception
    {
        try (var session = CasStorageSession.open()) {
            documentService.uploadSourceDocument(toInputStream("This is a test.", UTF_8),
                    annotationDocument.getDocument());
            var cas = documentService.readAnnotationCas(annotationDocument);
            buildFS(cas, NamedEntity.class.getName()) //
                    .withFeature("value", "test") //
                    .buildAndAddToIndexes();
            documentService.writeAnnotationCas(cas, annotationDocument,
                    EXPLICIT_ANNOTATOR_USER_ACTION);
        }
    }
}

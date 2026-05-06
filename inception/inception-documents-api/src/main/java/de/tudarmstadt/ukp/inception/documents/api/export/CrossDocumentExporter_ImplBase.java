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

import static de.tudarmstadt.ukp.clarin.webanno.api.casstorage.CasAccessMode.SHARED_READ_ONLY_ACCESS;
import static de.tudarmstadt.ukp.clarin.webanno.api.casstorage.CasUpgradeMode.AUTO_CAS_UPGRADE;
import static de.tudarmstadt.ukp.clarin.webanno.model.SourceDocumentState.CURATION_FINISHED;
import static de.tudarmstadt.ukp.clarin.webanno.model.SourceDocumentState.CURATION_IN_PROGRESS;
import static de.tudarmstadt.ukp.inception.support.WebAnnoConst.CURATION_USER;
import static java.util.Arrays.asList;

import java.io.IOException;
import java.util.List;

import org.apache.uima.cas.CAS;
import org.springframework.http.MediaType;

import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationDocument;
import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationSet;
import de.tudarmstadt.ukp.clarin.webanno.model.SourceDocument;
import de.tudarmstadt.ukp.inception.documents.api.DocumentService;

public abstract class CrossDocumentExporter_ImplBase
    implements CrossDocumentExporter
{
    protected static final MediaType MEDIA_TYPE_TEXT_CSV = MediaType.valueOf("text/csv");

    protected final DocumentService documentService;

    public CrossDocumentExporter_ImplBase(DocumentService aDocumentService)
    {
        documentService = aDocumentService;
    }

    @Override
    public String getId()
    {
        return getClass().getName();
    }

    private CAS loadInitialCas(SourceDocument aDocument) throws IOException
    {
        return documentService.createOrReadInitialCas(aDocument, AUTO_CAS_UPGRADE,
                SHARED_READ_ONLY_ACCESS);
    }

    private CAS loadCas(SourceDocument aDocument, String aDataOwner) throws IOException
    {
        return documentService.readAnnotationCas(aDocument, AnnotationSet.forUser(aDataOwner),
                AUTO_CAS_UPGRADE, SHARED_READ_ONLY_ACCESS);
    }

    protected CAS loadCasOrInitialCas(SourceDocument aDocument, String aDataOwner,
            List<AnnotationDocument> aAnnDocs)
        throws IOException
    {
        // If the annotation document belongs to the curation user but the curation has not
        // even started yet, then we load the initial CAS.
        if (CURATION_USER.equals(aDataOwner)) {
            if (!asList(CURATION_IN_PROGRESS, CURATION_FINISHED).contains(aDocument.getState())) {
                return loadInitialCas(aDocument);
            }

            return loadCas(aDocument, aDataOwner);
        }

        // If the there is no annotation document for the data owner it implies that the
        // annotation state is NEW - so we load the initial CAS.
        if (aAnnDocs.stream().noneMatch(annDoc -> aDataOwner.equals(annDoc.getUser()))) {
            return loadInitialCas(aDocument);
        }

        // If there is no CAS for the data owner, it also implies that the
        // annotation state is NEW - so we load the initial CAS.
        if (!documentService.existsCas(aDocument, AnnotationSet.forUser(aDataOwner))) {
            return loadInitialCas(aDocument);
        }

        return loadCas(aDocument, aDataOwner);
    }
}

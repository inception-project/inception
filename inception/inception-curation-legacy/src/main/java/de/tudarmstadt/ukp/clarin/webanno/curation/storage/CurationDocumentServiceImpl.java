/*
# * Licensed to the Technische Universität Darmstadt under one
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
package de.tudarmstadt.ukp.clarin.webanno.curation.storage;

import static de.tudarmstadt.ukp.clarin.webanno.api.WebAnnoConst.CURATION_USER;

import java.io.IOException;
import java.sql.Timestamp;
import java.util.Date;
import java.util.List;
import java.util.Optional;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;

import org.apache.commons.lang3.Validate;
import org.apache.uima.UIMAException;
import org.apache.uima.cas.CAS;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import de.tudarmstadt.ukp.clarin.webanno.api.AnnotationSchemaService;
import de.tudarmstadt.ukp.clarin.webanno.api.CasStorageService;
import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationDocumentState;
import de.tudarmstadt.ukp.clarin.webanno.model.Project;
import de.tudarmstadt.ukp.clarin.webanno.model.SourceDocument;
import de.tudarmstadt.ukp.clarin.webanno.model.SourceDocumentState;

@Component(CurationDocumentService.SERVICE_NAME)
public class CurationDocumentServiceImpl
    implements CurationDocumentService
{
    private final CasStorageService casStorageService;
    private final AnnotationSchemaService annotationService;

    @PersistenceContext
    private EntityManager entityManager;

    @Autowired
    public CurationDocumentServiceImpl(CasStorageService aCasStorageService,
            AnnotationSchemaService aAnnotationService)
    {
        casStorageService = aCasStorageService;
        annotationService = aAnnotationService;
    }

    @Override
    @PreAuthorize("hasAnyRole('ROLE_ADMIN','ROLE_USER')")
    @Transactional
    public void writeCurationCas(CAS aCas, SourceDocument aDocument, boolean aUpdateTimestamp)
        throws IOException
    {
        casStorageService.writeCas(aDocument, aCas, CURATION_USER);
        if (aUpdateTimestamp) {
            aDocument.setTimestamp(new Timestamp(new Date().getTime()));
            entityManager.merge(aDocument);
        }
    }

    @Override
    public CAS readCurationCas(SourceDocument aDocument) throws IOException
    {
        return casStorageService.readCas(aDocument, CURATION_USER);
    }

    @Override
    public void deleteCurationCas(SourceDocument aDocument) throws IOException
    {
        casStorageService.deleteCas(aDocument, CURATION_USER);
    }

    @Override
    public void upgradeCurationCas(CAS aCas, SourceDocument aDocument)
        throws UIMAException, IOException
    {
        annotationService.upgradeCas(aCas, aDocument, CURATION_USER);
    }

    @Override
    @Transactional
    public List<SourceDocument> listCuratableSourceDocuments(Project aProject)
    {
        Validate.notNull(aProject, "Project must be specified");

        List<SourceDocument> docs = entityManager
                .createQuery(
                        "SELECT DISTINCT adoc.document FROM AnnotationDocument AS adoc "
                                + "WHERE adoc.project = :project AND adoc.state = (:state) "
                                + "AND adoc.document.trainingDocument = false",
                        SourceDocument.class)
                .setParameter("project", aProject)
                .setParameter("state", AnnotationDocumentState.FINISHED).getResultList();
        docs.sort(SourceDocument.NAME_COMPARATOR);
        return docs;
    }

    @Override
    public Optional<Long> getCurationCasTimestamp(SourceDocument aDocument) throws IOException
    {
        Validate.notNull(aDocument, "Source document must be specified");

        return casStorageService.getCasTimestamp(aDocument, CURATION_USER);
    }

    @Override
    @Transactional
    public List<SourceDocument> listCuratedDocuments(Project aProject)
    {
        Validate.notNull(aProject, "Project must be specified");

        String query = String.join("\n", "FROM SourceDocument WHERE", "  project = :project AND",
                "  state = :state");

        return entityManager.createQuery(query, SourceDocument.class)
                .setParameter("project", aProject)
                .setParameter("state", SourceDocumentState.CURATION_FINISHED).getResultList();
    }

    @Override
    @Transactional
    public boolean isCurationFinished(SourceDocument aDocument)
    {
        Validate.notNull(aDocument, "Source document must be specified");

        String query = String.join("\n", "FROM SourceDocument WHERE", "  id = :id");

        SourceDocument d = entityManager.createQuery(query, SourceDocument.class)
                .setParameter("id", aDocument.getId()).getSingleResult();

        return SourceDocumentState.CURATION_FINISHED.equals(d.getState());
    }

    @Override
    public boolean existsCurationCas(SourceDocument aDocument) throws IOException
    {
        return casStorageService.existsCas(aDocument, CURATION_USER);
    }
}

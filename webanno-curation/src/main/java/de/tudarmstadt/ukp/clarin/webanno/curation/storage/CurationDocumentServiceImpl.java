/*
# * Copyright 2012
 * Ubiquitous Knowledge Processing (UKP) Lab and FG Language Technology
 * Technische Universität Darmstadt
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package de.tudarmstadt.ukp.clarin.webanno.curation.storage;

import static de.tudarmstadt.ukp.clarin.webanno.api.WebAnnoConst.CURATION_USER;

import java.io.File;
import java.io.IOException;
import java.sql.Timestamp;
import java.util.Date;
import java.util.List;
import java.util.Optional;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.Validate;
import org.apache.uima.UIMAException;
import org.apache.uima.cas.CAS;
import org.apache.uima.jcas.JCas;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import de.tudarmstadt.ukp.clarin.webanno.api.AnnotationSchemaService;
import de.tudarmstadt.ukp.clarin.webanno.api.CasStorageService;
import de.tudarmstadt.ukp.clarin.webanno.api.WebAnnoConst;
import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationDocumentState;
import de.tudarmstadt.ukp.clarin.webanno.model.Project;
import de.tudarmstadt.ukp.clarin.webanno.model.SourceDocument;
import de.tudarmstadt.ukp.clarin.webanno.support.logging.Logging;

@Component(CurationDocumentService.SERVICE_NAME)
public class CurationDocumentServiceImpl
    implements CurationDocumentService
{
    private final Logger log = LoggerFactory.getLogger(getClass());

    private @Autowired CasStorageService casStorageService;
    private @Autowired AnnotationSchemaService annotationService;

    @PersistenceContext
    private EntityManager entityManager;

    public CurationDocumentServiceImpl()
    {
        // Nothing to do
    }

    @Override
    public void removeCurationDocumentContent(SourceDocument aSourceDocument, String aUsername)
        throws IOException
    {
        if (new File(casStorageService.getAnnotationFolder(aSourceDocument),
                WebAnnoConst.CURATION_USER + ".ser").exists()) {
            FileUtils.forceDelete(new File(casStorageService.getAnnotationFolder(aSourceDocument),
                    WebAnnoConst.CURATION_USER + ".ser"));

            try (MDC.MDCCloseable closable = MDC.putCloseable(Logging.KEY_PROJECT_ID,
                    String.valueOf(aSourceDocument.getProject().getId()))) {
                Project project = aSourceDocument.getProject();
                log.info("Removed curation of source document [{}]({}) from project [{}]({})",
                        aSourceDocument.getName(), aSourceDocument.getId(), project.getName(),
                        project.getId());
            }
        }
    }

    @Override
    @PreAuthorize("hasAnyRole('ROLE_ADMIN','ROLE_USER')")
    @Transactional
    public void writeCurationCas(JCas aJcas, SourceDocument aDocument, boolean aUpdateTimestamp)
        throws IOException
    {
        casStorageService.writeCas(aDocument, aJcas, CURATION_USER);
        if (aUpdateTimestamp) {
            aDocument.setTimestamp(new Timestamp(new Date().getTime()));
            entityManager.merge(aDocument);
        }
    }

    @Override
    public JCas readCurationCas(SourceDocument aDocument)
        throws IOException
    {
        return casStorageService.readCas(aDocument, CURATION_USER);
    }

    @Override
    public void deleteCurationCas(SourceDocument aDocument)
        throws IOException
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
    public List<SourceDocument> listCuratableSourceDocuments(Project aProject)
    {
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
}

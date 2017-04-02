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
package de.tudarmstadt.ukp.clarin.webanno.api.dao;

import static de.tudarmstadt.ukp.clarin.webanno.api.WebAnnoConst.CORRECTION_USER;
import static de.tudarmstadt.ukp.clarin.webanno.api.WebAnnoConst.CURATION_USER;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.Date;
import java.util.List;
import javax.annotation.Resource;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;

import org.apache.commons.io.FileUtils;
import org.apache.uima.UIMAException;
import org.apache.uima.cas.CAS;
import org.apache.uima.jcas.JCas;
import org.hibernate.Session;
import org.hibernate.jdbc.Work;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.transaction.annotation.Transactional;

import de.tudarmstadt.ukp.clarin.webanno.api.AnnotationService;
import de.tudarmstadt.ukp.clarin.webanno.api.CasStorageService;
import de.tudarmstadt.ukp.clarin.webanno.api.CorrectionDocumentService;
import de.tudarmstadt.ukp.clarin.webanno.api.CurationDocumentService;
import de.tudarmstadt.ukp.clarin.webanno.api.DocumentService;
import de.tudarmstadt.ukp.clarin.webanno.api.SettingsService;
import de.tudarmstadt.ukp.clarin.webanno.api.UserDao;
import de.tudarmstadt.ukp.clarin.webanno.api.WebAnnoConst;
import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationDocumentState;
import de.tudarmstadt.ukp.clarin.webanno.model.Project;
import de.tudarmstadt.ukp.clarin.webanno.model.SourceDocument;
import de.tudarmstadt.ukp.clarin.webanno.model.User;
import de.tudarmstadt.ukp.clarin.webanno.support.logging.Logging;

public class RepositoryServiceDbData
    implements 
    CorrectionDocumentService, CurationDocumentService, SettingsService
{
    private final Logger log = LoggerFactory.getLogger(getClass());

    @Value(value = "${repository.path}")
    private File dir;
    
    @Resource(name = "casStorageService")
    private CasStorageService casStorageService;
    
    @Resource(name = "annotationService")
    private AnnotationService annotationService;

    @Resource(name = "documentService")
    private DocumentService documentService;

    @Resource(name = "userRepository")
    private UserDao userRepository;

    @Value(value = "${ui.brat.sentences.number}")
    private int numberOfSentences;

    @PersistenceContext
    private EntityManager entityManager;

    public RepositoryServiceDbData()
    {

    }

    @Override
    public boolean existsCorrectionCas(SourceDocument aSourceDocument)
        throws IOException
    {
        try {
            readCorrectionCas(aSourceDocument);
            return true;
        }
        catch (FileNotFoundException e) {
            return false;
        }
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
    public void writeCorrectionCas(JCas aJcas, SourceDocument aDocument, User aUser)
        throws IOException
    {
        casStorageService.writeCas(aDocument, aJcas, CORRECTION_USER);
    }

    @Override
    @PreAuthorize("hasAnyRole('ROLE_ADMIN','ROLE_USER')")
    @Transactional
    public void writeCurationCas(JCas aJcas, SourceDocument aDocument, User aUser,
            boolean aUpdateTimestamp)
        throws IOException
    {
        casStorageService.writeCas(aDocument, aJcas, CURATION_USER);
        if (aUpdateTimestamp) {
            aDocument.setTimestamp(new Timestamp(new Date().getTime()));
            entityManager.merge(aDocument);
        }
    }

    @Override
    public JCas readCorrectionCas(SourceDocument aDocument)
        throws IOException
    {
        return casStorageService.readCas(aDocument, CORRECTION_USER);
    }

    @Override
    public JCas readCurationCas(SourceDocument aDocument)
        throws IOException
    {
        return casStorageService.readCas(aDocument, CURATION_USER);
    }

    @Override
    public void upgradeCorrectionCas(CAS aCas, SourceDocument aDocument)
        throws UIMAException, IOException
    {
        annotationService.upgradeCas(aCas, aDocument, CORRECTION_USER);
    }
       
    @Override
    public String getDatabaseDriverName()
    {
        final StringBuilder sb = new StringBuilder();
        Session session = entityManager.unwrap(Session.class);
        session.doWork(new Work()
        {
            @Override
            public void execute(Connection aConnection)
                throws SQLException
            {
                sb.append(aConnection.getMetaData().getDriverName());
            }
        });

        return sb.toString();
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
    public int getNumberOfSentences()
    {
        return numberOfSentences;
    }
}

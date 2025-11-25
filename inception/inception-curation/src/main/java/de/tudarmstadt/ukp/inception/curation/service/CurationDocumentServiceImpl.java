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
package de.tudarmstadt.ukp.inception.curation.service;

import static de.tudarmstadt.ukp.clarin.webanno.model.AnnotationSet.CURATION_SET;
import static de.tudarmstadt.ukp.clarin.webanno.model.PermissionLevel.ANNOTATOR;
import static de.tudarmstadt.ukp.inception.support.WebAnnoConst.CURATION_USER;

import java.io.IOException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Optional;

import org.apache.commons.lang3.Validate;
import org.apache.uima.UIMAException;
import org.apache.uima.cas.CAS;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

import de.tudarmstadt.ukp.clarin.webanno.api.casstorage.CasStorageService;
import de.tudarmstadt.ukp.clarin.webanno.api.casstorage.ConcurentCasModificationException;
import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationDocumentState;
import de.tudarmstadt.ukp.clarin.webanno.model.Project;
import de.tudarmstadt.ukp.clarin.webanno.model.SourceDocument;
import de.tudarmstadt.ukp.clarin.webanno.model.SourceDocumentState;
import de.tudarmstadt.ukp.clarin.webanno.security.model.User;
import de.tudarmstadt.ukp.inception.curation.config.CurationDocumentServiceAutoConfiguration;
import de.tudarmstadt.ukp.inception.project.api.ProjectService;
import de.tudarmstadt.ukp.inception.schema.api.AnnotationSchemaService;
import jakarta.persistence.EntityManager;

/**
 * <p>
 * This class is exposed as a Spring Component via
 * {@link CurationDocumentServiceAutoConfiguration#curationDocumentService}.
 * </p>
 */
public class CurationDocumentServiceImpl
    implements CurationDocumentService
{
    private final EntityManager entityManager;
    private final CasStorageService casStorageService;
    private final AnnotationSchemaService annotationService;
    private final ProjectService projectService;

    @Autowired
    public CurationDocumentServiceImpl(CasStorageService aCasStorageService,
            AnnotationSchemaService aAnnotationService, ProjectService aProjectService,
            EntityManager aEntityManager)
    {
        casStorageService = aCasStorageService;
        annotationService = aAnnotationService;
        entityManager = aEntityManager;
        projectService = aProjectService;
    }

    @Override
    @Transactional
    public void writeCurationCas(CAS aCas, SourceDocument aDocument, boolean aUpdateTimestamp)
        throws IOException
    {
        casStorageService.writeCas(aDocument, aCas, CURATION_SET);
        if (aUpdateTimestamp) {
            aDocument.setTimestamp(new Timestamp(new Date().getTime()));
            entityManager.merge(aDocument);
        }
    }

    @Override
    public CAS readCurationCas(SourceDocument aDocument) throws IOException
    {
        return casStorageService.readCas(aDocument, CURATION_SET);
    }

    @Override
    public void deleteCurationCas(SourceDocument aDocument) throws IOException
    {
        casStorageService.deleteCas(aDocument, CURATION_SET);
    }

    @Override
    public void upgradeCurationCas(CAS aCas, SourceDocument aDocument)
        throws UIMAException, IOException
    {
        annotationService.upgradeCas(aCas, aDocument, CURATION_USER);
    }

    @Override
    @Transactional
    public List<User> listCuratableUsers(SourceDocument aSourceDocument)
    {
        Validate.notNull(aSourceDocument, "Document must be specified");

        var query = String.join("\n", //
                "SELECT u FROM User u", //
                " JOIN AnnotationDocument as d", //
                "   ON d.user = u.username", //
                " JOIN ProjectPermission AS perm", //
                "   ON d.project = perm.project AND d.user = perm.user", //
                "WHERE u.username = d.user", //
                "  AND perm.level = :level", //
                "  AND d.document = :document", //
                "  AND (d.state = :state or d.annotatorState = :ignore)", //
                "ORDER BY u.username ASC");

        return new ArrayList<>(entityManager //
                .createQuery(query, User.class) //
                .setParameter("document", aSourceDocument) //
                .setParameter("level", ANNOTATOR) //
                .setParameter("state", AnnotationDocumentState.FINISHED) //
                .setParameter("ignore", AnnotationDocumentState.IGNORE) //
                .getResultList());
    }

    @Override
    @Transactional
    public List<SourceDocument> listCuratableSourceDocuments(Project aProject)
    {
        Validate.notNull(aProject, "Project must be specified");

        // Get all annotators in the project
        var users = projectService.listUsersWithRoleInProject(aProject, ANNOTATOR);
        // Bail out already. HQL doesn't seem to like queries with an empty parameter right of "in"
        if (users.isEmpty()) {
            return new ArrayList<>();
        }

        String query = String.join("\n", //
                "SELECT DISTINCT adoc.document", //
                "FROM AnnotationDocument AS adoc", //
                "WHERE adoc.project = :project", //
                "AND adoc.user in (:users)", //
                "AND (adoc.state = :state or adoc.annotatorState = :ignore)", //
                "ORDER BY adoc.document.name");

        List<SourceDocument> docs = entityManager.createQuery(query, SourceDocument.class) //
                .setParameter("project", aProject) //
                .setParameter("users", users.stream().map(User::getUsername).toList()) //
                .setParameter("state", AnnotationDocumentState.FINISHED) //
                .setParameter("ignore", AnnotationDocumentState.IGNORE) //
                .getResultList();

        return docs;
    }

    @Override
    public Optional<Long> getCurationCasTimestamp(SourceDocument aDocument) throws IOException
    {
        Validate.notNull(aDocument, "Source document must be specified");

        return casStorageService.getCasTimestamp(aDocument, CURATION_SET);
    }

    @Override
    public Optional<Long> verifyCurationCasTimestamp(SourceDocument aDocument, long aTimeStamp,
            String aContextAction)
        throws IOException, ConcurentCasModificationException
    {
        return casStorageService.verifyCasTimestamp(aDocument, CURATION_SET, aTimeStamp,
                aContextAction);
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
    @Transactional(readOnly = true)
    public boolean isCurationFinished(SourceDocument aDocument)
    {
        Validate.notNull(aDocument, "Source document must be specified");

        var query = String.join("\n", "FROM SourceDocument WHERE", "  id = :id");

        var d = entityManager.createQuery(query, SourceDocument.class) //
                .setParameter("id", aDocument.getId()) //
                .getSingleResult();

        return SourceDocumentState.CURATION_FINISHED.equals(d.getState());
    }

    @Override
    public boolean existsCurationCas(SourceDocument aDocument) throws IOException
    {
        return casStorageService.existsCas(aDocument, CURATION_SET);
    }
}

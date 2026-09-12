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
package de.tudarmstadt.ukp.inception.testscenarios.curation;

import static de.tudarmstadt.ukp.clarin.webanno.model.SourceDocumentState.CURATION_FINISHED;
import static java.util.Arrays.asList;

import java.util.List;
import java.util.Optional;

import org.springframework.core.annotation.Order;

import de.tudarmstadt.ukp.clarin.webanno.model.SourceDocument;
import de.tudarmstadt.ukp.clarin.webanno.project.initializers.NamedEntityLayerInitializer;
import de.tudarmstadt.ukp.clarin.webanno.security.UserDao;
import de.tudarmstadt.ukp.inception.documents.api.DocumentService;
import de.tudarmstadt.ukp.inception.project.api.ProjectInitializer;
import de.tudarmstadt.ukp.inception.project.api.ProjectService;

/**
 * Two curatable documents that differ in editability: the first stays in
 * {@code ANNOTATION_FINISHED} (a curator may edit it), the second is moved to
 * {@code CURATION_FINISHED} (a curator may not).
 * <p>
 * This exists to test code that answers "may this document be edited?" while <em>switching</em>
 * documents. A single-document project cannot distinguish an implementation that asks about the
 * document being opened from one that answers about the document the page still holds - both give
 * the same answer when there is only ever one document. With two documents of differing
 * editability, opening the second right after the first makes the two implementations disagree.
 */
@Order(9000)
public class CurationMixedEditabilityScenarioInitializer
    extends CurationScenarioInitializer_ImplBase
{
    private static final String EDITABLE_DOCUMENT = "curation-sample-editable.txt";
    private static final String FINISHED_DOCUMENT = "curation-sample-finished.txt";

    public CurationMixedEditabilityScenarioInitializer(DocumentService aDocumentService,
            ProjectService aProjectService, UserDao aUserService)
    {
        super(aDocumentService, aProjectService, aUserService);
    }

    @Override
    public String getName()
    {
        return "TEST: curation-ready, mixed editability";
    }

    @Override
    public Optional<String> getDescription()
    {
        return Optional.of("Two curatable documents, one editable and one with curation already "
                + "finished. For testing editability checks across a document switch.");
    }

    @Override
    protected String getProjectNameSuffix()
    {
        return "curation mixed editability";
    }

    @Override
    protected List<String> listDocuments()
    {
        return asList(EDITABLE_DOCUMENT, FINISHED_DOCUMENT);
    }

    @Override
    protected Class<?> getDocumentOwner()
    {
        return CurationMixedEditabilityScenarioInitializer.class;
    }

    @Override
    protected void finalizeDocument(SourceDocument aDocument, int aIndex)
    {
        if (FINISHED_DOCUMENT.equals(aDocument.getName())) {
            // A curator may not edit a document whose curation is already finished, so this is the
            // document that must come back as not editable.
            getDocumentService().setSourceDocumentState(aDocument, CURATION_FINISHED);
        }
    }

    @Override
    public List<Class<? extends ProjectInitializer>> getDependencies()
    {
        return asList(NamedEntityLayerInitializer.class);
    }
}

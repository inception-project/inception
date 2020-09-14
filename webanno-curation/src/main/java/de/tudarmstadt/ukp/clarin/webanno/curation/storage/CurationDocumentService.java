/*
 * Copyright 2017
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

import java.io.IOException;
import java.util.List;

import de.tudarmstadt.ukp.clarin.webanno.api.MergeDocumentService;
import de.tudarmstadt.ukp.clarin.webanno.model.Project;
import de.tudarmstadt.ukp.clarin.webanno.model.SourceDocument;

public interface CurationDocumentService extends MergeDocumentService
{
    String SERVICE_NAME = "curationDocumentService";
    
    List<SourceDocument> listCuratableSourceDocuments(Project aProject);
    
    /**
     * List all curated source documents.
     *
     * @param project
     *            the project.
     * @return the source documents.
     */
    List<SourceDocument> listCuratedDocuments(Project project);

    boolean isCurationFinished(SourceDocument aDocument);

    void deleteCurationCas(SourceDocument aDocument) throws IOException;

}

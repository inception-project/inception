/*
 * Copyright 2017
 * Ubiquitous Knowledge Processing (UKP) Lab
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
package de.tudarmstadt.ukp.inception.search;

import java.io.IOException;
import java.util.List;
import java.util.Map;

import org.apache.uima.cas.CAS;

import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationDocument;
import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationFeature;
import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationLayer;
import de.tudarmstadt.ukp.clarin.webanno.model.Project;
import de.tudarmstadt.ukp.clarin.webanno.model.SourceDocument;
import de.tudarmstadt.ukp.clarin.webanno.security.model.User;
import de.tudarmstadt.ukp.inception.search.model.Index;

public interface SearchService
{
    static final String SERVICE_NAME = "searchService";

    List<SearchResult> query(User aUser, Project aProject, String aQuery)
        throws IOException, ExecutionException;

    List<SearchResult> query(User aUser, Project aProject, String aQuery, SourceDocument aDocument)
        throws IOException, ExecutionException;

    Map<String, List<SearchResult>> query(User aUser, Project aProject, String aQuery,
        SourceDocument aDocument, AnnotationLayer aAnnotationLayer,
        AnnotationFeature aAnnotationFeature) throws IOException, ExecutionException;

    void reindex(Project aproject) throws IOException;

    Index getIndex(Project aProject);

    boolean isIndexValid(Project aProject);
    
    void indexDocument(SourceDocument aSourceDocument, CAS aJCas);

    void indexDocument(AnnotationDocument aAnnotationDocument, CAS aJCas);

    boolean isIndexInProgress(Project aProject);
}

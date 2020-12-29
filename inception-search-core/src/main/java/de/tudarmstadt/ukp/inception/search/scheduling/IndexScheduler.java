/*
 * Copyright 2020
 * Ubiquitous Knowledge Processing (UKP) Lab
 * Technische Universität Darmstadt
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package de.tudarmstadt.ukp.inception.search.scheduling;

import org.apache.uima.cas.CAS;

import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationDocument;
import de.tudarmstadt.ukp.clarin.webanno.model.Project;
import de.tudarmstadt.ukp.clarin.webanno.model.SourceDocument;
import de.tudarmstadt.ukp.inception.search.config.SearchServiceAutoConfiguration;

/**
 * Indexer scheduler. Does the project re-indexing in an asynchronous way.
 * <p>
 * This class is exposed as a Spring Component via
 * {@link SearchServiceAutoConfiguration#indexScheduler}.
 * </p>
 */
public interface IndexScheduler
{
    void enqueueReindexTask(Project aProject);

    void enqueueIndexDocument(SourceDocument aSourceDocument, CAS aCas);

    void enqueueIndexDocument(AnnotationDocument aAnnotationDocument, CAS aCas);

    boolean isIndexInProgress(Project aProject);

    boolean isBusy();
}

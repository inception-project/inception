/*
 * Copyright 2017
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
package de.tudarmstadt.ukp.inception.search.scheduling.tasks;

import org.apache.uima.cas.CAS;
import org.springframework.beans.factory.annotation.Autowired;

import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationDocument;
import de.tudarmstadt.ukp.inception.search.SearchService;

/**
 * (Re)indexes the annotation document for a specific user.
 */
public class IndexAnnotationDocumentTask
    extends Task
{
    private @Autowired SearchService searchService;
    
    public IndexAnnotationDocumentTask(AnnotationDocument aAnnotationDocument, CAS aJCas)
    {
        super(aAnnotationDocument, aJCas);
    }

    @Override
    public void run()
    {
        searchService.indexDocument(super.getAnnotationDocument(), super.getCas());
    }
    
    @Override
    public boolean matches(Task aTask)
    {
        if (!(aTask instanceof IndexAnnotationDocumentTask)) {
            return false;
        }
        
        return getAnnotationDocument().getId() == aTask.getAnnotationDocument().getId();
    }

}

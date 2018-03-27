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
package de.tudarmstadt.ukp.inception.search.index.mimir;

import org.springframework.stereotype.Component;

import de.tudarmstadt.ukp.clarin.webanno.api.AnnotationSchemaService;
import de.tudarmstadt.ukp.clarin.webanno.api.DocumentService;
import de.tudarmstadt.ukp.clarin.webanno.api.ProjectService;
import de.tudarmstadt.ukp.clarin.webanno.model.Project;
import de.tudarmstadt.ukp.inception.search.index.Index;
import de.tudarmstadt.ukp.inception.search.index.IndexFactoryImplBase;
import gate.util.GateException;

@Component("mimirDocumentIndexFactory")
public class MimirDocumentIndexFactory
    extends IndexFactoryImplBase
{
    @Override
    public String getDisplayName()
    {
        return "mimirDocumentIndexFactory";
    }

    @Override
    public Index getNewIndex(Project aProject, AnnotationSchemaService aAnnotationSchemaService,
            DocumentService aDocumentService, ProjectService aProjectService, String aDir)
    {
        Index indexBase = null;
        try {
            indexBase = new MimirDocumentIndex(aProject, aAnnotationSchemaService, aDocumentService,
                    aProjectService, aDir);
        }
        catch (GateException e) {
            log.error("Unable to index source document", e);
        }
        return indexBase;
    }

    @Override
    public int getOrder()
    {
        return 0;
    }

}

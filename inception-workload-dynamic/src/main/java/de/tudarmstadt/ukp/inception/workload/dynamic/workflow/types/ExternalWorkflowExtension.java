/*
 * Copyright 2020
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
package de.tudarmstadt.ukp.inception.workload.dynamic.workflow.types;

import java.util.List;

import de.tudarmstadt.ukp.clarin.webanno.model.SourceDocument;
import de.tudarmstadt.ukp.inception.workload.dynamic.workflow.WorkflowExtension;

/**
 * External workflow extension type
 */
public class ExternalWorkflowExtension
    implements WorkflowExtension
{
    public static final String EXTERNAL_EXTENSION = "external";

    @Override
    public String getLabel()
    {
        return "External workflow";
    }

    @Override
    public String getId()
    {
        return EXTERNAL_EXTENSION;
    }

    @Override
    public List<SourceDocument> rankDocuments(List<SourceDocument> aSourceDocuments)
    {
        //TODO correct implementation for the external workflow
        return aSourceDocuments;
    }


}

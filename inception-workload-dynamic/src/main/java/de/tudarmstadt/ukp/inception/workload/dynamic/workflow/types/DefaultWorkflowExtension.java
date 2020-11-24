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
 * Default workflow extension type
 */
public class DefaultWorkflowExtension implements WorkflowExtension
{
    public static final String DEFAULT_WORKFLOW = "default";

    @Override
    public String getLabel()
    {
        return "Default workflow";
    }

    @Override
    public String getId()
    {
        return DEFAULT_WORKFLOW;
    }

    @Override
    public List<SourceDocument> getNextDocument(List<SourceDocument> aSourceDocuments)
    {
        return aSourceDocuments;
    }

}

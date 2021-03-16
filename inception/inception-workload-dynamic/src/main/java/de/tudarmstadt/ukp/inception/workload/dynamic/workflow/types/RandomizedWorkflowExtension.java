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
package de.tudarmstadt.ukp.inception.workload.dynamic.workflow.types;

import java.util.Collections;
import java.util.List;

import de.tudarmstadt.ukp.clarin.webanno.model.SourceDocument;
import de.tudarmstadt.ukp.inception.workload.dynamic.workflow.WorkflowExtension;

/**
 * Randomized workflow extension type
 */
public class RandomizedWorkflowExtension
    implements WorkflowExtension
{
    public static final String RANDOMIZED_WORKFLOW = "randomized";

    @Override
    public String getLabel()
    {
        return "Randomized workflow";
    }

    @Override
    public String getId()
    {
        return RANDOMIZED_WORKFLOW;
    }

    @Override
    public List<SourceDocument> rankDocuments(List<SourceDocument> aSourceDocuments)
    {
        // Shuffling and returning the list
        Collections.shuffle(aSourceDocuments);
        return aSourceDocuments;
    }

}

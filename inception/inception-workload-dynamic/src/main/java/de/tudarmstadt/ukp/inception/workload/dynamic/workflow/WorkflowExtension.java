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
package de.tudarmstadt.ukp.inception.workload.dynamic.workflow;

import java.util.List;

import de.tudarmstadt.ukp.clarin.webanno.model.Project;
import de.tudarmstadt.ukp.clarin.webanno.model.SourceDocument;
import de.tudarmstadt.ukp.inception.support.extensionpoint.Extension;

/**
 * Extensions for the workflow.
 */
public interface WorkflowExtension
    extends Extension<Project>
{
    @Override
    default boolean accepts(Project project)
    {
        return true;
    }

    String getLabel();

    /**
     * @param aSourceDocuments
     *            the list of possible documents to work on next
     * @return List of {@link SourceDocument} changed as required by the specific workflow strategy
     */
    List<SourceDocument> rankDocuments(List<SourceDocument> aSourceDocuments);
}

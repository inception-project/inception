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

import static java.util.stream.Collectors.toList;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;

import de.tudarmstadt.ukp.clarin.webanno.model.Project;
import de.tudarmstadt.ukp.inception.support.extensionpoint.ExtensionPoint_ImplBase;
import de.tudarmstadt.ukp.inception.workload.dynamic.workflow.types.WorkflowType;

public class WorkflowExtensionPointImpl
    extends ExtensionPoint_ImplBase<Project, WorkflowExtension>
    implements WorkflowExtensionPoint
{
    public WorkflowExtensionPointImpl(
            @Lazy @Autowired(required = false) List<WorkflowExtension> aExtensions)
    {
        super(aExtensions);
    }

    @Override
    public List<WorkflowExtension> getExtensions(Project aContext)
    {
        Map<String, WorkflowExtension> byRole = new LinkedHashMap<>();
        for (WorkflowExtension extension : super.getExtensions(aContext)) {
            byRole.put(extension.getId(), extension);
        }
        return new ArrayList<>(byRole.values());
    }

    @Override
    public WorkflowExtension getDefault()
    {
        return getExtensions().get(0);
    }

    @Override
    public List<WorkflowType> getTypes()
    {
        return getExtensions().stream()
                .map(manExt -> new WorkflowType(manExt.getId(), manExt.getLabel()))
                .collect(toList());
    }
}

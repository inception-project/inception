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
package de.tudarmstadt.ukp.inception.workload.dynamic.workflow;

import java.io.Serializable;

import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;

public class WorkflowManagerType
    implements Serializable
{
    private static final long serialVersionUID = 6963958947605837721L;

    private final String uiName;
    private final String workflowManagerExtensionId;

    public WorkflowManagerType(String aWorkflowManagerExtensionId, String aUiName)
    {
        uiName = aUiName;
        workflowManagerExtensionId = aWorkflowManagerExtensionId;
    }

    public String getUiName()
    {
        return uiName;
    }
    
    public String getWorkloadManagerExtensionId()
    {
        return workflowManagerExtensionId;
    }

    @Override
    public boolean equals(final Object other)
    {
        if (!(other instanceof WorkflowManagerType)) {
            return false;
        }
        WorkflowManagerType castOther = (WorkflowManagerType) other;
        return new EqualsBuilder()
                .append(workflowManagerExtensionId, castOther.workflowManagerExtensionId)
                .isEquals();
    }

    @Override
    public int hashCode()
    {
        return new HashCodeBuilder().append(workflowManagerExtensionId).toHashCode();
    }
}

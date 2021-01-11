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

import java.io.Serializable;

import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;

/**
 * WorkflowType represents the type of workflow currently enabled. Consists of a uiName which
 * represents the String in the UI and a workloadManagerExtensionId.
 */
public class WorkflowType
    implements Serializable
{
    private static final long serialVersionUID = 6963958947605837721L;

    private final String uiName;
    private final String workflowExtensionId;

    public WorkflowType(String aWorkflowExtensionId, String aUiName)
    {
        uiName = aUiName;
        workflowExtensionId = aWorkflowExtensionId;
    }

    public String getUiName()
    {
        return uiName;
    }

    public String getWorkflowExtensionId()
    {
        return workflowExtensionId;
    }

    @Override
    public boolean equals(final Object other)
    {
        if (!(other instanceof WorkflowType)) {
            return false;
        }
        WorkflowType castOther = (WorkflowType) other;
        return new EqualsBuilder().append(workflowExtensionId, castOther.workflowExtensionId)
                .isEquals();
    }

    @Override
    public int hashCode()
    {
        return new HashCodeBuilder().append(workflowExtensionId).toHashCode();
    }
}

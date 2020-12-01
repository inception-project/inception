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
package de.tudarmstadt.ukp.inception.workload.extension;

import java.io.Serializable;

import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;

public class WorkloadManagerType
    implements Serializable
{
    private static final long serialVersionUID = 6963958947605837721L;

    private final String uiName;
    private final String workloadManagerExtensionId;

    public WorkloadManagerType(String aWorkloadManagerExtensionId, String aUiName)
    {
        uiName = aUiName;
        workloadManagerExtensionId = aWorkloadManagerExtensionId;
    }

    public String getUiName()
    {
        return uiName;
    }

    public String getWorkloadManagerExtensionId()
    {
        return workloadManagerExtensionId;
    }

    @Override
    public boolean equals(final Object other)
    {
        if (!(other instanceof WorkloadManagerType)) {
            return false;
        }
        WorkloadManagerType castOther = (WorkloadManagerType) other;
        return new EqualsBuilder()
                .append(workloadManagerExtensionId, castOther.workloadManagerExtensionId)
                .isEquals();
    }

    @Override
    public int hashCode()
    {
        return new HashCodeBuilder().append(workloadManagerExtensionId).toHashCode();
    }
}

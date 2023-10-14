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
package de.tudarmstadt.ukp.clarin.webanno.api.export;

import java.io.Serializable;
import java.util.UUID;

import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;

public class ProjectExportTaskHandle
    implements Serializable
{
    private static final long serialVersionUID = -105340488296444406L;

    private final String runId;

    public ProjectExportTaskHandle()
    {
        runId = UUID.randomUUID().toString();
    }

    public ProjectExportTaskHandle(String aRunId)
    {
        runId = aRunId;
    }

    public String getRunId()
    {
        return runId;
    }

    @Override
    public boolean equals(final Object other)
    {
        if (!(other instanceof ProjectExportTaskHandle)) {
            return false;
        }
        ProjectExportTaskHandle castOther = (ProjectExportTaskHandle) other;
        return new EqualsBuilder().append(runId, castOther.runId).isEquals();
    }

    @Override
    public int hashCode()
    {
        return new HashCodeBuilder().append(runId).toHashCode();
    }
}

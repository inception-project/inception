/*
 * Copyright 2020
 * Ubiquitous Knowledge Processing (UKP) Lab
 * Technische Universität Darmstadt
 * 
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
package de.tudarmstadt.ukp.inception.curation.model;

import java.io.Serializable;

import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;

public class CurationSettingsId
    implements Serializable
{
    private static final long serialVersionUID = 1L;

    private Long projectId;
    private String username;

    public CurationSettingsId()
    {
    }

    public CurationSettingsId(Long aProjectId, String aUsername)
    {
        projectId = aProjectId;
        username = aUsername;
    }

    @Override
    public boolean equals(final Object other)
    {
        if (!(other instanceof CurationSettingsId)) {
            return false;
        }
        CurationSettingsId castOther = (CurationSettingsId) other;
        return new EqualsBuilder().append(projectId, castOther.projectId)
                .append(username, castOther.username).isEquals();
    }

    @Override
    public int hashCode()
    {
        return new HashCodeBuilder().append(projectId).append(username).toHashCode();
    }
}

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
package de.tudarmstadt.ukp.inception.feature.lookup;

import java.io.Serializable;
import java.util.Objects;

import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;

@JsonSerialize
public class LookupEntry
    implements Serializable
{
    private static final long serialVersionUID = -5686362017417313135L;

    private static final String ID = "id";
    private static final String LABEL = "l";
    private static final String DESCRIPTION = "d";

    private final String id;
    private final String uiLabel;
    private final String description;

    @JsonCreator
    public LookupEntry( //
            @JsonProperty(ID) String aId, //
            @JsonProperty(LABEL) String aUiLabel, //
            @JsonProperty(DESCRIPTION) String aDescription)
    {
        id = aId;
        uiLabel = aUiLabel;
        description = aDescription;
    }

    @JsonProperty(ID)
    public String getIdentifier()
    {
        return id;
    }

    @JsonProperty(LABEL)
    public String getUiLabel()
    {
        return uiLabel;
    }

    @JsonProperty(DESCRIPTION)
    public String getDescription()
    {
        return description;
    }

    @Override
    public boolean equals(final Object other)
    {
        if (!(other instanceof LookupEntry)) {
            return false;
        }
        LookupEntry castOther = (LookupEntry) other;
        return Objects.equals(id, castOther.id);
    }

    @Override
    public int hashCode()
    {
        return Objects.hash(id);
    }

    @Override
    public String toString()
    {
        return new ToStringBuilder(this, ToStringStyle.SIMPLE_STYLE).append("id", id)
                .append("uiLabel", uiLabel).append("description", description).toString();
    }
}

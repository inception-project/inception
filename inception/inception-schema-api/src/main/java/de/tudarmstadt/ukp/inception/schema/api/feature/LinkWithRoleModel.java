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
package de.tudarmstadt.ukp.inception.schema.api.feature;

import java.io.Serializable;

import org.apache.uima.cas.FeatureStructure;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;

/**
 * Represents a link with a role in the UI.
 */
@JsonSerialize
@JsonInclude(value = JsonInclude.Include.NON_DEFAULT)
public class LinkWithRoleModel
    implements Serializable
{
    private static final long serialVersionUID = 2027345278696308900L;

    public static final String CLICK_HINT = "<Click to activate>";

    public String role;
    public String label = CLICK_HINT;
    public int targetAddr = -1;
    public boolean autoCreated;

    private LinkWithRoleModel(Builder builder)
    {
        role = builder.role;
        label = builder.label;
        targetAddr = builder.targetAddr;
        autoCreated = builder.autoCreated;
    }

    public LinkWithRoleModel()
    {
        // Needed for deserialization
    }

    @Deprecated
    public LinkWithRoleModel(LinkWithRoleModel aOther)
    {
        role = aOther.role;
        label = aOther.label;
        targetAddr = aOther.targetAddr;
    }

    @Deprecated
    public LinkWithRoleModel(String aRole, FeatureStructure aFS)
    {
        role = aRole;
        label = aRole;
        targetAddr = aFS.getAddress();
    }

    @Deprecated
    public LinkWithRoleModel(String aRole, String aLabel, int aTargetAddr)
    {
        role = aRole;
        label = aLabel;
        targetAddr = aTargetAddr;
    }

    @Override
    public int hashCode()
    {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((label == null) ? 0 : label.hashCode());
        result = prime * result + ((role == null) ? 0 : role.hashCode());
        result = prime * result + targetAddr;
        return result;
    }

    @Override
    public boolean equals(Object obj)
    {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        LinkWithRoleModel other = (LinkWithRoleModel) obj;
        if (label == null) {
            if (other.label != null) {
                return false;
            }
        }
        else if (!label.equals(other.label)) {
            return false;
        }
        if (role == null) {
            if (other.role != null) {
                return false;
            }
        }
        else if (!role.equals(other.role)) {
            return false;
        }
        if (targetAddr != other.targetAddr) {
            return false;
        }
        return true;
    }

    @Override
    public String toString()
    {
        StringBuilder builder = new StringBuilder();
        builder.append("LinkWithRoleModel [role=");
        builder.append(role);
        builder.append(", label=");
        builder.append(label);
        builder.append(", targetAddr=");
        builder.append(targetAddr);
        builder.append(", autoCreated=");
        builder.append(autoCreated);
        builder.append("]");
        return builder.toString();
    }

    public static Builder builder()
    {
        return new Builder();
    }

    public static final class Builder
    {
        private String role;
        private String label = CLICK_HINT;
        private int targetAddr = -1;
        private boolean autoCreated;

        private Builder()
        {
        }

        public Builder withRole(String aRole)
        {
            role = aRole;
            return this;
        }

        public Builder withLabel(String aLabel)
        {
            label = aLabel;
            return this;
        }

        public Builder withTargetAddr(int aTargetAddr)
        {
            targetAddr = aTargetAddr;
            return this;
        }

        public Builder withTarget(FeatureStructure aFS)
        {
            targetAddr = aFS.getAddress();
            return this;
        }

        public Builder withAutoCreated(boolean aAutoCreated)
        {
            autoCreated = aAutoCreated;
            return this;
        }

        public LinkWithRoleModel build()
        {
            return new LinkWithRoleModel(this);
        }
    }
}

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

import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;

public class FeatureType
    implements Serializable
{
    private static final long serialVersionUID = 5538322359085905396L;

    private final String name;
    private final String uiName;
    private final String featureSupportId;
    private final boolean internal;

    public FeatureType(String aName, String aUiName, String aFeatureSupportId)
    {
        name = aName;
        uiName = aUiName;
        featureSupportId = aFeatureSupportId;
        internal = false;
    }

    public FeatureType(String aName, String aUiName, String aFeatureSupportId, boolean aInternal)
    {
        name = aName;
        uiName = aUiName;
        featureSupportId = aFeatureSupportId;
        internal = aInternal;
    }

    public String getName()
    {
        return name;
    }

    public String getUiName()
    {
        return uiName;
    }

    public String getFeatureSupportId()
    {
        return featureSupportId;
    }

    /**
     * Check if the type is reserved for internal use and the user cannot create features of this
     * type.
     * 
     * @return {@code true} if the type is reserved for internal use.
     */
    public boolean isInternal()
    {
        return internal;
    }

    @Override
    public String toString()
    {
        return new ToStringBuilder(this, ToStringStyle.JSON_STYLE) //
                .append("name", name) //
                .append("featureSupportId", featureSupportId) //
                .toString();
    }

    @Override
    public int hashCode()
    {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((featureSupportId == null) ? 0 : featureSupportId.hashCode());
        result = prime * result + ((name == null) ? 0 : name.hashCode());
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
        FeatureType other = (FeatureType) obj;
        if (featureSupportId == null) {
            if (other.featureSupportId != null) {
                return false;
            }
        }
        else if (!featureSupportId.equals(other.featureSupportId)) {
            return false;
        }
        if (name == null) {
            if (other.name != null) {
                return false;
            }
        }
        else if (!name.equals(other.name)) {
            return false;
        }
        return true;
    }
}

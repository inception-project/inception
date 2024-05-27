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
package de.tudarmstadt.ukp.inception.schema.api.layer;

import java.io.Serializable;

public class LayerType
    implements Serializable
{
    private static final long serialVersionUID = 4990576447229259657L;

    private final String name;
    private final String uiName;
    private final String layerSupportId;
    private final boolean internal;

    public LayerType(String aName, String aUiName, String alayerSupportId)
    {
        name = aName;
        uiName = aUiName;
        layerSupportId = alayerSupportId;
        internal = false;
    }

    public LayerType(String aName, String aUiName, String alayerSupportId, boolean aInternal)
    {
        name = aName;
        uiName = aUiName;
        layerSupportId = alayerSupportId;
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

    public String getlayerSupportId()
    {
        return layerSupportId;
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
    public int hashCode()
    {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((layerSupportId == null) ? 0 : layerSupportId.hashCode());
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
        LayerType other = (LayerType) obj;
        if (layerSupportId == null) {
            if (other.layerSupportId != null) {
                return false;
            }
        }
        else if (!layerSupportId.equals(other.layerSupportId)) {
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

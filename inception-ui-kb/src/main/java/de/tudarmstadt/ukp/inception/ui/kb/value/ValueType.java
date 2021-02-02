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
package de.tudarmstadt.ukp.inception.ui.kb.value;

import java.io.Serializable;

/**
 * A {@link ValueType} reports with the name, uiName and ValueTypeSupport Id
 */
public class ValueType
    implements Serializable
{
    private static final long serialVersionUID = 5538322359085905396L;

    private final String name;
    private final String uiName;
    private final String valueTypeSupportId;

    public ValueType(String aName, String aUiName, String aValueTypeSupportId)
    {
        name = aName;
        uiName = aUiName;
        valueTypeSupportId = aValueTypeSupportId;
    }

    public String getName()
    {
        return name;
    }

    public String getUiName()
    {
        return uiName;
    }

    public String getValueTypeSupportId()
    {
        return valueTypeSupportId;
    }

    @Override
    public int hashCode()
    {
        final int prime = 31;
        int result = 1;
        result = prime * result
                + ((valueTypeSupportId == null) ? 0 : valueTypeSupportId.hashCode());
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
        ValueType other = (ValueType) obj;
        if (valueTypeSupportId == null) {
            if (other.valueTypeSupportId != null) {
                return false;
            }
        }
        else if (!valueTypeSupportId.equals(other.valueTypeSupportId)) {
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

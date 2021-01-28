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
package de.tudarmstadt.ukp.clarin.webanno.constraints.evaluator;

import java.io.Serializable;

/**
 * Class for containing possible values based on a rule and evaluation Also includes flag values
 */
public class PossibleValue
    implements Comparable<PossibleValue>, Serializable
{
    private static final long serialVersionUID = -310345685698644725L;

    private final String value;
    private final boolean important;

    public PossibleValue(String aValue, boolean aImportant)
    {
        value = aValue;
        important = aImportant;
    }

    public String getValue()
    {
        return value;
    }

    public boolean isImportant()
    {
        return important;
    }

    @Override
    public int hashCode()
    {
        final int prime = 31;
        int result = 1;
        result = prime * result + (important ? 1231 : 1237);
        result = prime * result + ((value == null) ? 0 : value.hashCode());
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
        PossibleValue other = (PossibleValue) obj;
        if (important != other.important) {
            return false;
        }
        if (value == null) {
            if (other.value != null) {
                return false;
            }
        }
        else if (!value.equals(other.value)) {
            return false;
        }
        return true;
    }

    @Override
    public String toString()
    {
        return "PossibleValue [value=" + value + ", important=" + important + "]";
    }

    /*
     * (non-Javadoc)
     * 
     * @see java.lang.Comparable#compareTo(java.lang.Object)
     */
    @Override
    public int compareTo(PossibleValue o)
    {
        // Reverse sorting, so that important things are at top.
        if (!this.isImportant() && o.isImportant()) {
            return 1;
        }
        else if (this.isImportant() && !o.isImportant()) {
            return -1;
        }
        else {
            // Sort based on string value if important tags are same, A-Z
            return this.getValue().compareTo(o.getValue());
        }

    }
}

/*
 * Copyright 2017
 * Ubiquitous Knowledge Processing (UKP) Lab
 * Technische Universität Darmstadt
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package de.tudarmstadt.ukp.inception.io.brat.dkprocore.internal.model;

public class BratEventArgumentDecl
{
    private final String name;
    private final String cardinality;
    private final String range;

    public BratEventArgumentDecl(String aName, String aCardinality)
    {
        this(aName, aCardinality, BratConstants.RANGE_ANY);
    }

    public BratEventArgumentDecl(String aName, String aCardinality, String aRange)
    {
        name = aName;
        cardinality = aCardinality;
        range = aRange;
    }

    public String getName()
    {
        return name;
    }

    public String getCardinality()
    {
        return cardinality;
    }

    public String getRange()
    {
        return range;
    }

    @Override
    public String toString()
    {
        StringBuilder sb = new StringBuilder();

        // Slot
        sb.append(name);

        // Cardinality: nothing, '?' or '+'
        sb.append(cardinality);

        // Separator
        sb.append(':');

        // Range
        sb.append(range);
        return sb.toString();
    }

    @Override
    public int hashCode()
    {
        final int prime = 31;
        int result = 1;
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
        BratEventArgumentDecl other = (BratEventArgumentDecl) obj;
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

/*
 * Copyright 2019
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
package de.tudarmstadt.ukp.inception.recommendation.imls.stringmatch.span.gazeteer.model;

import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.apache.commons.lang3.builder.ToStringBuilder;

public class GazeteerEntry
{
    public final String text;
    public final String label;

    public GazeteerEntry(String aText, String aLabel)
    {
        text = aText;
        label = aLabel;
    }

    @Override
    public boolean equals(final Object other)
    {
        if (!(other instanceof GazeteerEntry)) {
            return false;
        }
        GazeteerEntry castOther = (GazeteerEntry) other;
        return new EqualsBuilder() //
                .append(text, castOther.text) //
                .append(label, castOther.label) //
                .isEquals();
    }

    @Override
    public int hashCode()
    {
        return new HashCodeBuilder() //
                .append(text) //
                .append(label) //
                .toHashCode();
    }

    @Override
    public String toString()
    {
        return new ToStringBuilder(this) //
                .append("text", text) //
                .append("label", label) //
                .toString();
    }
}

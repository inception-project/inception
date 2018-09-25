/*
 * Copyright 2018
 * Ubiquitous Knowledge Processing (UKP) Lab
 * Technische Universität Darmstadt
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package de.tudarmstadt.ukp.clarin.webanno.conll.sequencecodec;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;

public class SequenceItem
{
    private final int begin;
    private final int end;
    private final String label;

    public SequenceItem(int aBegin, int aEnd, String aLabel)
    {
        super();
        begin = aBegin;
        end = aEnd;
        label = aLabel;
    }

    public int getBegin()
    {
        return begin;
    }

    public int getEnd()
    {
        return end;
    }

    public String getLabel()
    {
        return label;
    }

    @Override
    public String toString()
    {
        return new ToStringBuilder(this, ToStringStyle.NO_CLASS_NAME_STYLE)
                .append("begin", begin)
                .append("end", end)
                .append("label", label)
                .toString();
    }
    
    public static List<SequenceItem> of(String... aLabels)
    {
        return of(1, aLabels);
    }

    public static List<SequenceItem> of(int aOffset, String... aLabels)
    {
        int begin = 0;
        List<SequenceItem> result = new ArrayList<>(aLabels.length);
        for (String label : aLabels) {
            result.add(new SequenceItem(begin + aOffset, begin + aOffset, label));
            begin++;
        }
        return result;
    }

    @Override
    public boolean equals(final Object other)
    {
        if (!(other instanceof SequenceItem)) {
            return false;
        }
        SequenceItem castOther = (SequenceItem) other;
        return new EqualsBuilder().append(begin, castOther.begin).append(end, castOther.end)
                .append(label, castOther.label).isEquals();
    }

    @Override
    public int hashCode()
    {
        return new HashCodeBuilder().append(begin).append(end).append(label).toHashCode();
    }
}

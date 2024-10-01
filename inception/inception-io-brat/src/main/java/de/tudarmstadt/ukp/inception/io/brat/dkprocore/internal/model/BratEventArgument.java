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

import java.io.IOException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.lang3.StringUtils;

import com.fasterxml.jackson.core.JsonGenerator;

public class BratEventArgument
{
    private static final Pattern PATTERN = Pattern.compile(
            "(?<SLOT>[a-zA-Z_][a-zA-Z0-9_-]*[a-zA-Z_-]+)(?<INDEX>[0-9]+)?:(?<TARGET>[ET][0-9]+)");

    private static final String TARGET = "TARGET";
    private static final String SLOT = "SLOT";
    private static final String INDEX = "INDEX";

    private final String target;
    private final String slot;
    private final int index;

    public BratEventArgument(String aSlot, int aIndex, String aTarget)
    {
        target = aTarget;
        slot = aSlot;
        index = aIndex;
    }

    public String getTarget()
    {
        return target;
    }

    public String getSlot()
    {
        return slot;
    }

    public int getIndex()
    {
        return index;
    }

    public void write(JsonGenerator aJG) throws IOException
    {
        aJG.writeStartArray();
        aJG.writeString(index == 0 ? slot : slot + index);
        aJG.writeString(target);
        aJG.writeEndArray();
    }

    @Override
    public String toString()
    {
        StringBuilder sb = new StringBuilder();
        sb.append(slot);
        if (index > 0) {
            sb.append(index);
        }
        sb.append(':');
        sb.append(target);
        return sb.toString();
    }

    public static BratEventArgument parse(String aLine)
    {
        Matcher m = PATTERN.matcher(aLine);

        if (!m.matches()) {
            throw new IllegalArgumentException("Illegal event argument format [" + aLine + "]");
        }

        int index = 0;
        if (StringUtils.isNumeric(m.group(INDEX))) {
            index = Integer.valueOf(m.group(INDEX));
        }

        return new BratEventArgument(m.group(SLOT), index, m.group(TARGET));
    }
}

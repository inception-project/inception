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

import com.fasterxml.jackson.core.JsonGenerator;

public class BratAttribute
{
    private static final Pattern PATTERN = Pattern
            .compile("(?<ID>[AM][0-9]+)" + "[\\t](?<TYPE>[a-zA-Z_][a-zA-Z0-9_\\-:]+)"
                    + " (?<TARGET>[TREN][0-9]+)" + "(?: (?<VALUES>.*))?");

    private static final String ID = "ID";
    private static final String TYPE = "TYPE";
    private static final String TARGET = "TARGET";
    private static final String VALUES = "VALUES";

    private final String id;
    private String target;
    private final String name;
    private final String[] values;

    public BratAttribute(int aId, String aName, String aTarget, String... aValues)
    {
        this("A" + aId, aName, aTarget, aValues);
    }

    public BratAttribute(String aId, String aName, String aTarget, String... aValues)
    {
        id = aId;
        target = aTarget;
        name = aName;
        values = aValues;
    }

    public String getId()
    {
        return id;
    }

    public void setTarget(String aTarget)
    {
        target = aTarget;
    }

    public String getTarget()
    {
        return target;
    }

    public String getName()
    {
        return name;
    }

    public String[] getValues()
    {
        return values;
    }

    public void write(JsonGenerator aJG) throws IOException
    {
        // Format: [${ID}, ${TYPE}, ${TARGET}]
        // ['A1', 'Notorious', 'T4']

        aJG.writeStartArray();
        aJG.writeString(id);
        aJG.writeString(name);
        aJG.writeString(target);
        aJG.writeEndArray();
    }

    @Override
    public String toString()
    {
        StringBuilder sb = new StringBuilder();
        sb.append(id);
        sb.append('\t');
        sb.append(name);
        sb.append(' ');
        sb.append(target);
        if (values != null) {
            for (String value : values) {
                sb.append(' ');
                sb.append(value);
            }
        }

        return sb.toString();
    }

    public static BratAttribute parse(String aLine)
    {
        Matcher m = PATTERN.matcher(aLine);

        if (!m.matches()) {
            throw new IllegalArgumentException("Illegal attribute format [" + aLine + "]");
        }

        String values = m.group(VALUES);
        if (values == null) {
            return new BratAttribute(m.group(ID), m.group(TYPE), m.group(TARGET), new String[0]);
        }
        else {
            return new BratAttribute(m.group(ID), m.group(TYPE), m.group(TARGET),
                    values.split(" "));
        }

    }
}

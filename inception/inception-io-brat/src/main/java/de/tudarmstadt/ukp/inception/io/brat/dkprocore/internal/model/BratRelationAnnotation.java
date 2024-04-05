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

public class BratRelationAnnotation
    extends BratAnnotation
{
    private static final Pattern PATTERN = Pattern
            .compile("(?<ID>R[0-9]+)[\\t]" + "(?<TYPE>[a-zA-Z0-9_][a-zA-Z0-9_-]+) "
                    + "(?<ARG1LABEL>[a-zA-Z][a-zA-Z0-9]+):" + "(?<ARG1TARGET>[ET][0-9]+) "
                    + "(?<ARG2LABEL>[a-zA-Z][a-zA-Z0-9]+):" + "(?<ARG2TARGET>[ET][0-9]+)\\s*");

    private static final String ID = "ID";
    private static final String TYPE = "TYPE";
    private static final String ARG1_LABEL = "ARG1LABEL";
    private static final String ARG1_TARGET = "ARG1TARGET";
    private static final String ARG2_LABEL = "ARG2LABEL";
    private static final String ARG2_TARGET = "ARG2TARGET";

    private final String arg1Label;
    private final String arg1Target;
    private final String arg2Label;
    private final String arg2Target;

    public BratRelationAnnotation(int aId, String aType, String aArg1Label, String aArg1Target,
            String aArg2Label, String aArg2Target)
    {
        this("R" + aId, aType, aArg1Label, aArg1Target, aArg2Label, aArg2Target);
    }

    public BratRelationAnnotation(String aId, String aType, String aArg1Label, String aArg1Target,
            String aArg2Label, String aArg2Target)
    {
        super(aId, aType);
        arg1Label = aArg1Label;
        arg1Target = aArg1Target;
        arg2Label = aArg2Label;
        arg2Target = aArg2Target;
    }

    public String getArg1Label()
    {
        return arg1Label;
    }

    public String getArg1Target()
    {
        return arg1Target;
    }

    public String getArg2Label()
    {
        return arg2Label;
    }

    public String getArg2Target()
    {
        return arg2Target;
    }

    @Override
    public void write(JsonGenerator aJG) throws IOException
    {
        // Format: [${ID}, ${TYPE}, [[${ARGNAME}, ${TARGET}], [${ARGNAME}, ${TARGET}]]]
        // ['R1', 'Anaphora', [['Anaphor', 'T2'], ['Entity', 'T1']]]

        aJG.writeStartArray();
        aJG.writeString(getId());
        aJG.writeString(getType());
        aJG.writeStartArray();
        aJG.writeStartArray();
        aJG.writeString(arg1Label);
        aJG.writeString(arg1Target);
        aJG.writeEndArray();
        aJG.writeStartArray();
        aJG.writeString(arg2Label);
        aJG.writeString(arg2Target);
        aJG.writeEndArray();
        aJG.writeEndArray();
        aJG.writeEndArray();
    }

    @Override
    public String toString()
    {
        return getId() + '\t' + getType() + ' ' + arg1Label + ':' + arg1Target + ' ' + arg2Label
                + ':' + arg2Target;
    }

    public static BratRelationAnnotation parse(String aLine)
    {
        Matcher m = PATTERN.matcher(aLine);

        if (!m.matches()) {
            throw new IllegalArgumentException(
                    "Illegal relation annotation format [" + aLine + "]");
        }

        return new BratRelationAnnotation(m.group(ID), m.group(TYPE), m.group(ARG1_LABEL),
                m.group(ARG1_TARGET), m.group(ARG2_LABEL), m.group(ARG2_TARGET));
    }
}

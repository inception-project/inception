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

public class BratNoteAnnotation
    extends BratAnnotation
{
    private static final Pattern PATTERN = Pattern.compile("(?<ID>#[0-9]+)\\t"
            + "(?<TYPE>[a-zA-Z_][a-zA-Z0-9_\\-]+) " + "(?<TARGET>[ETR][0-9]+)\\t" + "(?<NOTE>.*)");

    private static final String ID = "ID";
    private static final String TYPE = "TYPE";
    private static final String TARGET = "TARGET";
    private static final String NOTE = "NOTE";

    private final String target;
    private final String note;

    public BratNoteAnnotation(int aId, String aType, String aTarget, String aNote)
    {
        this("#" + aId, aType, aTarget, aNote);
    }

    public BratNoteAnnotation(String aId, String aType, String aTarget, String aNote)
    {
        super(aId, aType);
        target = aTarget;
        note = aNote;
    }

    public String getTarget()
    {
        return target;
    }

    public String getNote()
    {
        return note;
    }

    @Override
    public void write(JsonGenerator aJG) throws IOException
    {
        // Format: [${TARGET}, ${TYPE}, ${NOTE}]
        // ['T1', 'AnnotatorNotes', 'Hurrah!']

        aJG.writeStartArray();
        aJG.writeString(getType());
        aJG.writeString(target);
        aJG.writeString(note);
        aJG.writeEndArray();
    }

    @Override
    public String toString()
    {
        return getId() + '\t' + getType() + ' ' + target + '\t' + note;
    }

    public static BratNoteAnnotation parse(String aLine)
    {
        Matcher m = PATTERN.matcher(aLine);

        if (!m.matches()) {
            throw new IllegalArgumentException("Illegal text annotation format [" + aLine + "]");
        }

        return new BratNoteAnnotation(m.group(ID), m.group(TYPE), m.group(TARGET), m.group(NOTE));
    }
}

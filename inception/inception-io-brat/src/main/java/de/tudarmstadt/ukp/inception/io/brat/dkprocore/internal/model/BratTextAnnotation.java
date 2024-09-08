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
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.fasterxml.jackson.core.JsonGenerator;

public class BratTextAnnotation
    extends BratAnnotation
{
    private static final Pattern PATTERN = Pattern
            .compile("(?<ID>T[0-9]+)\\t" + "(?<TYPE>[a-zA-Z0-9_][a-zA-Z0-9_\\-]*) "
                    + "(?<OFFSETS>[0-9]+ [0-9]+(;[0-9]+ [0-9]+)*)\\t" + "(?<TEXT>.*)");

    private static final String ID = "ID";
    private static final String TYPE = "TYPE";
    private static final String OFFSETS = "OFFSETS";
    private static final String TEXT = "TEXT";

    private final String[] texts;

    private final List<Offsets> offsets;

    public BratTextAnnotation(int aId, String aType, List<Offsets> aOffsets, String[] aTexts)
    {
        this("T" + aId, aType, aOffsets, aTexts);
    }

    public BratTextAnnotation(String aId, String aType, List<Offsets> aOffsets, String[] aTexts)
    {
        super(aId, aType);
        offsets = aOffsets;
        texts = aTexts;
    }

    private static String[] splitText(String aText, List<Offsets> aOffsets)
    {
        String[] result = new String[aOffsets.size()];
        String pieceOfText = aText;
        for (int i = 0; i < aOffsets.size(); i++) {
            int size = aOffsets.get(i).getEnd() - aOffsets.get(i).getBegin();
            result[i] = aText.substring(0, size);
            pieceOfText = pieceOfText.substring(size);
        }
        return result;
    }

    public List<Offsets> getOffsets()
    {
        return offsets;
    }

    public String[] getText()
    {
        return texts;
    }

    @Override
    public void write(JsonGenerator aJG) throws IOException
    {
        // Format: [${ID}, ${TYPE}, [[${START}, ${END}]]]
        // note that range of the offsets are [${START},${END})
        // ['T1', 'Person', [[0, 11]]]

        aJG.writeStartArray();
        aJG.writeString(getId());
        aJG.writeString(getType());
        aJG.writeStartArray();
        for (int i = 0; i < offsets.size(); i++) {
            // handle discontinuous annotations
            aJG.writeStartArray();
            aJG.writeNumber(offsets.get(i).getBegin());
            aJG.writeNumber(offsets.get(i).getEnd());
            aJG.writeEndArray();
        }
        aJG.writeEndArray();
        aJG.writeEndArray();
    }

    @Override
    public String toString()
    {
        return getId() + '\t' + getType() + ' ' + generateOffset(offsets) + '\t'
                + String.join(" ", texts);
    }

    private String generateOffset(List<Offsets> aOffsets)
    {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < offsets.size(); i++) {
            sb.append(String.format("%s %s", offsets.get(i).getBegin(), offsets.get(i).getEnd()));
            if (i < offsets.size() - 1) {
                sb.append(";");
            }
        }
        return sb.toString();
    }

    private static List<Offsets> generateOffsetsString(String aOffsetsStr)
    {
        String[] offsetsArray = aOffsetsStr.split(";");
        List<Offsets> offsetsList = new ArrayList<>();
        for (int i = 0; i < offsetsArray.length; i++) {
            String[] beginEnd = offsetsArray[i].split(" ");
            int effectiveBegin = Integer.parseInt(beginEnd[0]);
            int effectiveEnd = Integer.parseInt(beginEnd[1]);
            if (i > 0 && effectiveBegin <= (1 + offsetsList.get(offsetsList.size() - 1).getEnd())) {
                // in case of adjacent or overlapping discontinuous annotations, merge the spans
                // 1 2;3 4 -> 1 4
                offsetsList.get(offsetsList.size() - 1).setEnd(effectiveEnd);
            }
            else {
                // in case of non-adjacent discontinuous annotation, create two offsets
                // 1 2;4 5 -> 1 2 and 4 5
                offsetsList.add(new Offsets(effectiveBegin, effectiveEnd));
            }
        }
        return offsetsList;
    }

    public static BratTextAnnotation parse(String aLine)
    {
        Matcher m = PATTERN.matcher(aLine);

        if (!m.matches()) {
            throw new IllegalArgumentException("Illegal text annotation format [" + aLine + "]");
        }
        List<Offsets> offsetsLocal = generateOffsetsString(m.group(OFFSETS));
        String[] textsLocal = splitText(m.group(TEXT), offsetsLocal);
        return new BratTextAnnotation(m.group(ID), m.group(TYPE), offsetsLocal, textsLocal);
    }
}

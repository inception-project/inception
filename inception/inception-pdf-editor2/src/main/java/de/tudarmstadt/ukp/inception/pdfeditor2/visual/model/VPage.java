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
package de.tudarmstadt.ukp.inception.pdfeditor2.visual.model;

import static java.util.Arrays.asList;

import java.io.Serializable;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;

import de.tudarmstadt.ukp.inception.support.json.BeanAsArraySerializer;

@JsonSerialize(using = BeanAsArraySerializer.class)
@JsonPropertyOrder(value = { "index", "begin", "end", "lines" })
public class VPage
    implements Serializable
{
    private static final long serialVersionUID = -919352254298051756L;

    private final int index;
    private final List<VLine> lines;
    private final int begin;
    private final int end;
    private final String text;
    private final float width;
    private final float height;

    public VPage(int aIndex, float aWidth, float aHeight, int aBegin, int aEnd, String aText,
            VLine... aLines)
    {
        this(aIndex, aWidth, aHeight, aBegin, aEnd, aText, asList(aLines));
    }

    public VPage(int aIndex, float aWidth, float aHeight, int aBegin, int aEnd, String aText,
            List<VLine> aLines)
    {
        index = aIndex;
        width = aWidth;
        height = aHeight;
        lines = aLines;
        text = aText;
        begin = aBegin;
        end = aEnd;
    }

    // public VPage(int aIndex, int aBegin, int aEnd, String aText, VGlyph... aGlyphs)
    // {
    // this(aIndex, aBegin, aEnd, aText, asList(aGlyphs));
    // }
    //
    // public VPage(int aIndex, int aBegin, int aEnd, String aText, List<VGlyph> aGlyphs)
    // {
    // width = -1;
    // height = -1;
    // index = aIndex;
    // glyphs = aGlyphs;
    // text = aText;
    // begin = aBegin;
    // end = aEnd;
    //
    // float lineY = -1;
    // float lineHeight = -1;
    // for (var glyph : aGlyphs) {
    // lineY = Math.min(glyph.getFontY(), lineY);
    // lineHeight = Math.max(glyph.getFontHeight(), lineHeight);
    // }
    //
    // lines = asList(new VLine(aBegin, aEnd, aText, lineY, lineHeight, glyphs));
    // }

    public int getIndex()
    {
        return index;
    }

    public List<VLine> getLines()
    {
        return lines;
    }

    public String getText()
    {
        return text;
    }

    public int getBegin()
    {
        return begin;
    }

    public int getEnd()
    {
        return end;
    }

    public float getHeight()
    {
        return height;
    }

    public float getWidth()
    {
        return width;
    }
}

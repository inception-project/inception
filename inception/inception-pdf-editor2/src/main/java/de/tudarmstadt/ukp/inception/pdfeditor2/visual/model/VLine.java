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

import java.util.List;

import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;

import de.tudarmstadt.ukp.inception.support.json.BeanAsArraySerializer;

@JsonSerialize(using = BeanAsArraySerializer.class)
@JsonPropertyOrder(value = { "dir", "base", "extent", "glyphs" })
public class VLine
{
    private final List<VGlyph> glyphs;
    private final float dir;
    private final float base;
    private final float extent;
    private final int begin;
    private final int end;
    private final String text;

    public VLine(int aBegin, int aEnd, String aText, float aDir, float aBase, float aExtent,
            List<VGlyph> aGlyphs)
    {
        begin = aBegin;
        end = aEnd;
        text = aText;
        dir = aDir;
        base = aBase;
        extent = aExtent;
        glyphs = aGlyphs;
    }

    public List<VGlyph> getGlyphs()
    {
        return glyphs;
    }

    public int getBegin()
    {
        return begin;
    }

    public int getEnd()
    {
        return end;
    }

    public String getText()
    {
        return text;
    }

    public float getDir()
    {
        return dir;
    }

    public float getBase()
    {
        return base;
    }

    public float getExtent()
    {
        return extent;
    }
}

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

import java.io.Serializable;
import java.util.List;

import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;

import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;

import de.tudarmstadt.ukp.inception.support.json.BeanAsArraySerializer;

@JsonSerialize(using = BeanAsArraySerializer.class)
@JsonPropertyOrder(value = { "dir", "x", "y", "w", "h", "glyphs" })
public class VChunk
    implements Serializable
{
    private static final long serialVersionUID = 4919070694537670645L;

    private final List<VGlyph> glyphs;
    private final float dir;
    private final float x;
    private final float y;
    private final float w;
    private final float h;
    private final int begin;
    private final int end;
    private final String text;

    public VChunk(int aBegin, int aEnd, String aText, float aDir, float aX, float aY, float aW,
            float aH, List<VGlyph> aGlyphs)
    {
        begin = aBegin;
        end = aEnd;
        text = aText;
        dir = aDir;
        x = aX;
        y = aY;
        w = aW;
        h = aH;
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

    public float getX()
    {
        return x;
    }

    public float getY()
    {
        return y;
    }

    public float getH()
    {
        return h;
    }

    public float getW()
    {
        return w;
    }

    @Override
    public String toString()
    {
        return new ToStringBuilder(this, ToStringStyle.JSON_STYLE).append("text", text)
                .append("begin", begin).append("end", end).append("dir", dir).append("x", x)
                .append("y", y).append("w", w).append("h", h).toString();
    }
}

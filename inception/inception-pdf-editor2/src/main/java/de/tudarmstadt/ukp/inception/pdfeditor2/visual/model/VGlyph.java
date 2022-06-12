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

import java.awt.geom.Rectangle2D;
import java.io.Serializable;

import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;

import de.tudarmstadt.ukp.inception.support.json.BeanAsArraySerializer;

@JsonSerialize(using = BeanAsArraySerializer.class)
@JsonPropertyOrder(value = { "begin", "unicode", "base", "extent" })
public class VGlyph
    implements Serializable
{
    private static final long serialVersionUID = -5353182196666028678L;

    private final int page;
    private final int begin;
    private final String unicode;
    private final float dir;
    private final float base;
    private final float extent;
    private final float fontX;
    private final float fontY;
    private final float fontWidth;
    private final float fontHeight;

    public VGlyph(int aBegin, int aPage, String aUnicode, float aDir, Rectangle2D.Double aFontShape)
    {
        this(aBegin, aPage, aUnicode, aDir, (float) aFontShape.x, (float) aFontShape.y,
                (float) aFontShape.width, (float) aFontShape.height);
    }

    public VGlyph(int aBegin, int aPage, String aUnicode, float aDir, float aFontX, float aFontY,
            float aFontWidth, float aFontHeight)
    {
        page = aPage;
        begin = aBegin;
        unicode = aUnicode;
        dir = aDir;
        fontX = aFontX;
        fontY = aFontY;
        fontWidth = aFontWidth;
        fontHeight = aFontHeight;

        if (dir == 0 || dir == 180) {
            base = fontX;
            extent = fontWidth;
        }
        else {
            base = fontY;
            extent = fontHeight;
        }
    }

    public int getBegin()
    {
        return begin;
    }

    public int getEnd()
    {
        return begin + unicode.length();
    }

    public String getUnicode()
    {
        return unicode;
    }

    public float getFontX()
    {
        return fontX;
    }

    public float getFontY()
    {
        return fontY;
    }

    public float getFontWidth()
    {
        return fontWidth;
    }

    public float getFontHeight()
    {
        return fontHeight;
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

    @Override
    public String toString()
    {
        StringBuilder sb = new StringBuilder();
        sb.append(begin);
        sb.append('\t');
        sb.append(page + 1);
        sb.append('\t');
        sb.append(unicode);
        sb.append('\t');
        sb.append(fontX);
        sb.append(' ');
        sb.append(fontY);
        sb.append(' ');
        sb.append(fontWidth);
        sb.append(' ');
        sb.append(fontHeight);
        return sb.toString();
    }
}

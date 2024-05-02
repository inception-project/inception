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

import com.fasterxml.jackson.core.JsonGenerator;

public class BratTextAnnotationDrawingDecl
    extends BratDrawingDecl
{
    private final String bgColor;
    private final String fgColor;
    private final String borderColor;

    public BratTextAnnotationDrawingDecl(String aType, String aFgColor, String aBgColor)
    {
        this(aType, aFgColor, aBgColor, "darken");
    }

    public BratTextAnnotationDrawingDecl(String aType, String aFgColor, String aBgColor,
            String aBorderColor)
    {
        super(aType);
        bgColor = aBgColor;
        fgColor = aFgColor;
        borderColor = aBorderColor;
    }

    public String getBgColor()
    {
        return bgColor;
    }

    public String getFgColor()
    {
        return fgColor;
    }

    public String getBorderColor()
    {
        return borderColor;
    }

    @Override
    public void write(JsonGenerator aJG) throws IOException
    {
        aJG.writeStringField("fgColor", getFgColor());
        aJG.writeStringField("bgColor", getBgColor());
        aJG.writeStringField("borderColor", getBorderColor());
    }

    @Override
    public String getSpec()
    {
        return "fgColor:" + fgColor + ", bgColor:" + bgColor + ", borderColor:" + borderColor;
    }
}

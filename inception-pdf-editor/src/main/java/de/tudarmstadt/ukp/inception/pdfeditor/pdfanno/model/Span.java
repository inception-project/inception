/*
 * Copyright 2018
 * Ubiquitous Knowledge Processing (UKP) Lab
 * Technische Universität Darmstadt
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package de.tudarmstadt.ukp.inception.pdfeditor.pdfanno.model;

public class Span {

    private String id;

    private int page;

    private String label;

    private String color;

    /**
     * This field is part of the PDFAnno Anno File format for spans.
     * It is redundant as startPos and endPos already can be used to obtain the text.
     * It could be left out in toAnnoFileString method as PDFAnno still can render it.
     * However this results in missing text when export functionality in PDFAnno is used.
     */
    private String text;

    private int startPos;

    private int endPos;

    public Span(String aId, String aLabel, String aColor)
    {
        id = aId;
        label = aLabel;
        color = aColor;
    }

    public Span(String aId, int aPage, String aLabel, String aColor, String aText,
                int aStartPos, int aEndPos)
    {
        id = aId;
        page = aPage;
        label = aLabel;
        color = aColor;
        text = aText;
        startPos = aStartPos;
        endPos = aEndPos;
    }

    public String getId()
    {
        return id;
    }

    public int getPage()
    {
        return page;
    }

    public String getLabel()
    {
        return label;
    }

    public String getColor()
    {
        return color;
    }

    public String getText()
    {
        return text;
    }

    public int getStartPos()
    {
        return startPos;
    }

    public int getEndPos()
    {
        return endPos;
    }

    public void setPage(int aPage)
    {
        page = aPage;
    }

    public void setText(String aText)
    {
        text = aText;
    }

    public void setStartPos(int aStartPos)
    {
        startPos = aStartPos;
    }

    public void setEndPos(int aEndPos)
    {
        endPos = aEndPos;
    }

    public String toAnnoFileString()
    {
        return "[[spans]]\n" +
            "id = \"" + id +  "\"\n" +
            "page = " + page + "\n" +
            "label = \"" + label.replace("`", "\\`").replace("\"", "\\\\\"") + "\"\n" +
            "color = \"" + color + "\"\n" +
            "text = \"" + text.replace("`", "\\`").replace("\"", "\\\\\"") + "\"\n" +
            "textrange = [" + startPos + "," + endPos + "]\n";
    }

}

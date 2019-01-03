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

    private int id;

    private int page;

    private String label;

    private String text;

    private int startPos;

    private int endPos;

    public Span(int aId, int aPage, String aText,
                int aStartPos, int aEndPos)
    {
        id = aId;
        page = aPage;
        text = aText;
        startPos = aStartPos;
        endPos = aEndPos;
    }

    public Span(int aId, int aPage, String aLabel, String aText,
                int aStartPos, int aEndPos)
    {
        id = aId;
        page = aPage;
        label = aLabel;
        text = aText;
        startPos = aStartPos;
        endPos = aEndPos;
    }

    public int getId()
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

    public String toAnnoFileString()
    {
        return "[[spans]]\n" +
            "id = \"" + id +  "\"\n" +
            "page = " + page + "\n" +
            "label = \"" + label + "\"\n" +
            "text = \"" + text + "\"\n" +
            "textrange = [" + startPos + "," + endPos + "]\n";
    }

}

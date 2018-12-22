/*
 * Copyright 2017
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

public class Span
    implements Annotation
{

    private int id;

    private int page;

    private String label;

    private String text;

    private int startPos;

    private int endPos;

    public Span(int id, int page, String text,
                int startPos, int endPos)
    {
        this.id = id;
        this.page = page;
        this.text = text;
        this.startPos = startPos;
        this.endPos = endPos;
    }

    public Span(int id, int page, String label, String text,
                int startPos, int endPos)
    {
        this.id = id;
        this.page = page;
        this.label = label;
        this.text = text;
        this.startPos = startPos;
        this.endPos = endPos;
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

    @Override
    public String toString()
    {
        return "[[spans]]\n" +
            "id = \"" + id +  "\"\n" +
            "page = " + page + "\n" +
            "label = \"" + label + "\"\n" +
            "text = \"" + text + "\"\n" +
            "textrange = [" + startPos + "," + endPos + "]\n";
    }

}

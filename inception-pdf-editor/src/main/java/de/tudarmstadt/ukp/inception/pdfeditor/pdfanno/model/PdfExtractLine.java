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

/**
 * Represents a line within a PDFExtract file.
 */
public class PdfExtractLine
{

    private int page;

    private int position;

    private String value;

    private String displayPositions;

    public PdfExtractLine()
    {
    }

    public PdfExtractLine(int page, int position, String value, String displayPositions)
    {
        this.page = page;
        this.position = position;
        this.value = value;
        this.displayPositions = displayPositions;
    }

    public int getPage()
    {
        return page;
    }

    public void setPage(int page)
    {
        this.page = page;
    }

    public int getPosition()
    {
        return position;
    }

    public void setPosition(int position)
    {
        this.position = position;
    }

    public String getValue()
    {
        return value;
    }

    public void setValue(String value)
    {
        this.value = value;
    }

    public String getDisplayPositions()
    {
        return displayPositions;
    }

    public void setDisplayPositions(String displayPositions)
    {
        this.displayPositions = displayPositions;
    }
}

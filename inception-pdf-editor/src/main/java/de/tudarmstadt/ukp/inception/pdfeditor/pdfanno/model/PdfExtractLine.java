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

import java.util.Objects;

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

    public PdfExtractLine(int aPage, int aPosition, String aValue, String aDisplayPositions)
    {
        page = aPage;
        position = aPosition;
        value = aValue;
        displayPositions = aDisplayPositions;
    }

    public int getPage()
    {
        return page;
    }

    public void setPage(int aPage)
    {
        page = aPage;
    }

    public int getPosition()
    {
        return position;
    }

    public void setPosition(int aPosition)
    {
        position = aPosition;
    }

    public String getValue()
    {
        return value;
    }

    public void setValue(String aValue)
    {
        value = aValue;
    }

    public String getDisplayPositions()
    {
        return displayPositions;
    }

    public void setDisplayPositions(String aDisplayPositions)
    {
        displayPositions = aDisplayPositions;
    }

    @Override
    public boolean equals(Object aObject) {
        if (this == aObject) return true;
        if (aObject == null || getClass() != aObject.getClass()) return false;
        PdfExtractLine that = (PdfExtractLine) aObject;
        return page == that.page &&
            position == that.position &&
            Objects.equals(value, that.value) &&
            Objects.equals(displayPositions, that.displayPositions);
    }

    @Override
    public int hashCode() {
        return Objects.hash(page, position, value, displayPositions);
    }
}

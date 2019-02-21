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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.collections4.BidiMap;
import org.apache.commons.collections4.bidimap.DualHashBidiMap;

/**
 * Represents a PDFExtract file.
 * This file contains information about the content of a PDF document.
 * This includes characters and their order and position but also about
 * draw operations and their positions.
 */
public class PdfExtractFile
{

    private String pdftxt;

    private String stringContent;

    /**
     * Contains position mapping for a character between PDFExtract string
     * including and excluding Draw Operations.
     */
    private BidiMap<Integer, Integer> stringPositionMap;

    /**
     * Map of line numbers and lines contained in a PDFExtract file.
     */
    private Map<Integer, PdfExtractLine> extractLines;

    public PdfExtractFile(String aPdftxt)
    {
        setPdftxt(aPdftxt);
    }

    public void setPdftxt(String aPdftxt)
    {
        stringPositionMap = new DualHashBidiMap<>();
        extractLines = new HashMap<>();
        pdftxt = aPdftxt;

        StringBuilder sb = new StringBuilder();
        String[] lines = pdftxt.split("\n");

        int extractLineIndex = 1;
        int strContentIndex = 0;

        for (String line : lines)
        {
            PdfExtractLine extractLine = new PdfExtractLine();
            String[] columns = line.split("\t");
            extractLine.setPage(Integer.parseInt(columns[0].trim()));
            extractLine.setPosition(extractLineIndex);
            extractLine.setValue(columns[1].trim());
            extractLine.setDisplayPositions(columns.length > 2 ? columns[2].trim() : "");
            extractLines.put(extractLineIndex, extractLine);

            // if value of PdfExtractLine is in brackets it is a draw operation and is ignored
            if (!extractLine.getValue().matches("^\\[.*\\]$")
                && !extractLine.getValue().equals("NO_UNICODE"))
            {
                sb.append(extractLine.getValue());
                stringPositionMap.put(strContentIndex, extractLineIndex);
                strContentIndex++;
            }
            extractLineIndex++;
        }

        stringContent = sb.toString();
    }

    public String getPdftxt()
    {
        return pdftxt;
    }

    public String getStringContent()
    {
        return stringContent;
    }

    /**
     * Gets PdfExtractLines between the given range in the string-only content
     */
    public List<PdfExtractLine> getStringPdfExtractLines(int aStart, int aEnd)
    {
        List<PdfExtractLine> lines = new ArrayList<>();
        for (int i = aStart; i <= aEnd; i++)
        {
            lines.add(getStringPdfExtractLine(i));
        }
        return lines;
    }

    public PdfExtractLine getStringPdfExtractLine(int aPosition)
    {
        return extractLines.get(stringPositionMap.get(aPosition));
    }

    /**
     * Gets the index in the actual PdfExtract string content for a PdfExtractLine index.
     * PdfExtract string content does not include draw operations in the index counting.
     */
    public int getStringIndex(int pdfExtractLine)
    {
        return stringPositionMap.getKey(pdfExtractLine);
    }
}

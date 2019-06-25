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

import java.io.Serializable;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import org.ahocorasick.trie.Emit;
import org.ahocorasick.trie.Trie;

import it.unimi.dsi.fastutil.ints.Int2IntMap;
import it.unimi.dsi.fastutil.ints.Int2IntOpenHashMap;

/**
 * Represents a PDFExtract file.
 * This file contains information about the content of a PDF document.
 * This includes characters and their order and position but also about
 * draw operations and their positions.
 */
public class PdfExtractFile implements Serializable
{

    private static final long serialVersionUID = -8596941152876909935L;

    /**
     * Contains PDFExtract file raw content
     */
    private String pdftxt;

    /**
     * Map of line numbers and lines contained in a PDFExtract file.
     */
    private Map<Integer, PdfExtractLine> extractLines;

    /**
     * Contains PDFExtract string content without draw operations
     */
    private String stringContent;

    /**
     * Contains PDFExtract string content without draw operations
     * and is sanitized with the help of substitutionTable.xml
     */
    private String sanitizedContent;

    /**
     * Mapping for every character from PDFExtract lines to stringContent
     */
    private Int2IntMap extractToString;

    /**
     * Mapping for every character from stringContent to PDFExtract lines
     */
    private Int2IntMap stringToExtract;

    /**
     * Mapping for every character from stringContent to sanitizedContent
     */
    private Int2IntMap stringToSanitized;

    /**
     * Mapping for every character from sanitizedContent to stringContent
     */
    private Int2IntMap sanitizedToString;

    /**
     * Mapping for characters from stringContent to sanitizedContent where a character
     * is mapped to a character sequence instead of a single character
     */
    private Map<Integer, Offset> stringToSanitizedSequence;

    /**
     * Mapping for characters from sanitizedContent to stringContent where a character
     * is mapped to a character sequence instead of a single character
     */
    private Map<Integer, Offset> sanitizedToStringSequence;

    /**
     * Contains substitutionTable.xml content
     */
    private Map<String, String> substitutionTable;

    /**
     * Maps a page number to its corresponding begin and end offset
     */
    private Map<Integer, Offset> pageOffsetMap;

    private int maxPageNumber;

    public PdfExtractFile(String aPdftxt, Map<String, String> aSubstitutionTable)
    {
        initializeStringContent(aPdftxt);
        initializeLSanitizedContent(aSubstitutionTable);
    }

    private void initializeLSanitizedContent(Map<String, String> aSubstitutionTable)
    {
        substitutionTable = aSubstitutionTable;
        sanitizedContent = stringContent;
        sanitizedToString = new Int2IntOpenHashMap();
        stringToSanitized = new Int2IntOpenHashMap();
        stringToSanitizedSequence = new HashMap<>();
        sanitizedToStringSequence = new HashMap<>();

        // build Aho-Corasick Trie to search for ligature occurences and replace them
        Trie.TrieBuilder trieBuilder = Trie.builder();
        substitutionTable.keySet().forEach(key -> trieBuilder.addKeyword(key));
        Trie trie = trieBuilder.build();
        Collection<Emit> emits = trie.parseText(sanitizedContent);
        Map<Integer, Emit> occurrences = new HashMap<>();
        for (Emit emit : emits) {
            occurrences.put(emit.getStart(), emit);
        }

        int stringIndex = 0;
        int sanitizedIndex = 0;
        StringBuilder sb = new StringBuilder();
        // iterate over stringContent and create sanitizedContent containing replaced ligatures
        // also create according mappings for characters
        while (stringIndex < stringContent.length()) {
            char c = stringContent.charAt(stringIndex);
            if (occurrences.containsKey(stringIndex)) {
                // start of a substitution was found
                Emit emit = occurrences.get(stringIndex);
                String string = emit.getKeyword();
                String sanitized = substitutionTable.get(string);
                int stringLen = string.length();
                int sanitizedLen = sanitized.length();

                // build mapping from stringContent to sanitizedContent
                for (int i = 0; i < stringLen; i++) {
                    if (sanitizedLen == 1) {
                        stringToSanitized.put(stringIndex + i, sanitizedIndex);
                    } else {
                        stringToSanitizedSequence.put(stringIndex + i,
                            new Offset(sanitizedIndex, sanitizedIndex + sanitizedLen - 1));
                    }
                }

                // build mapping from sanitizedContent to stringContent
                for (int i = 0; i < sanitizedLen; i++) {
                    sb.append(sanitized.charAt(i));
                    if (stringLen == 1) {
                        sanitizedToString.put(sanitizedIndex + i, stringIndex);
                    } else {
                        sanitizedToStringSequence.put(sanitizedIndex + i,
                            new Offset(stringIndex, stringIndex + stringLen - 1));
                    }
                }

                stringIndex += stringLen;
                sanitizedIndex += sanitizedLen;
            } else {
                sb.append(c);
                sanitizedToString.put(sanitizedIndex, stringIndex);
                stringToSanitized.put(stringIndex, sanitizedIndex);
                sanitizedIndex++;
                stringIndex++;
            }
        }

        sanitizedContent = sb.toString();
    }

    private void initializeStringContent(String aPdftxt)
    {
        stringToExtract = new Int2IntOpenHashMap();
        extractToString = new Int2IntOpenHashMap();
        extractLines = new HashMap<>();
        pageOffsetMap = new HashMap<>();
        pdftxt = aPdftxt;

        StringBuilder sb = new StringBuilder();
        String[] lines = pdftxt.split("\n");

        int extractLineIndex = 1;
        int strContentIndex = 0;
        int lastPage = 1;
        int pageBeginIndex = 1;

        for (String line : lines)
        {
            PdfExtractLine extractLine = new PdfExtractLine();
            String[] columns = line.split("\t");
            int page = Integer.parseInt(columns[0].trim());
            extractLine.setPage(page);
            extractLine.setPosition(extractLineIndex);
            extractLine.setValue(columns[1].trim());
            extractLine.setDisplayPositions(columns.length > 2 ? columns[2].trim() : "");
            extractLines.put(extractLineIndex, extractLine);

            stringToExtract.put(strContentIndex, extractLineIndex);
            extractToString.put(extractLineIndex, strContentIndex);

            if (page > lastPage) {
                pageOffsetMap.put(lastPage, new Offset(pageBeginIndex, extractLineIndex - 1));
                lastPage = page;
                pageBeginIndex = extractLineIndex;
            }

            // if value of PdfExtractLine is in brackets it is a draw operation and is ignored
            // if value is "NO_UNICODE" also skip, unicode mapping is unavailable for this character
            if (!extractLine.getValue().matches("^\\[.*\\]$")
                && !extractLine.getValue().equals("NO_UNICODE"))
            {
                sb.append(extractLine.getValue());
                strContentIndex++;
            }
            extractLineIndex++;
        }

        // add last page
        pageOffsetMap.put(lastPage, new Offset(pageBeginIndex, extractLineIndex - 1));
        maxPageNumber = lastPage;
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

    public String getSanitizedContent()
    {
        return sanitizedContent;
    }

    public PdfExtractLine getStringPdfExtractLine(int aIndex)
    {
        return extractLines.get(aIndex);
    }

    /**
     * Get the Offset for a given index in the sanitizedContent string
     */
    public Offset getExtractIndex(int aSanitizedIndex)
    {
        if (sanitizedToStringSequence.containsKey(aSanitizedIndex)) {
            Offset offset = sanitizedToStringSequence.get(aSanitizedIndex);
            return new Offset(
                stringToExtract.get(offset.getBegin()), stringToExtract.get(offset.getEnd()));
        } else {
            int extractIndex = stringToExtract.get(sanitizedToString.get(aSanitizedIndex));
            return new Offset(extractIndex, extractIndex);
        }
    }

    /**
     * Get the Offset for a given line index in the PDFExtract file
     */
    public Offset getStringIndex(int aExtractIndex)
    {
        int stringIndex = extractToString.get(aExtractIndex);
        if (stringToSanitizedSequence.containsKey(stringIndex) ) {
            return stringToSanitizedSequence.get(stringIndex);
        } else {
            int sanitizedIndex = stringToSanitized.get(stringIndex);
            return new Offset(sanitizedIndex, sanitizedIndex);
        }
    }

    /**
     * Get begin and end offset of a page.
     */
    public Offset getPageOffset(int aPage)
    {
        return pageOffsetMap.get(aPage);
    }

    public int getMaxPageNumber()
    {
        return maxPageNumber;
    }
}

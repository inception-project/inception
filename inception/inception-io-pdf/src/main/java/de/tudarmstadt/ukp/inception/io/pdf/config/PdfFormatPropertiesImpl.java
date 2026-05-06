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
package de.tudarmstadt.ukp.inception.io.pdf.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties("format.pdf")
public class PdfFormatPropertiesImpl
    implements PdfFormatProperties
{
    /**
     * Try extracting a basic HTML structure (mainly paragraphs) to improve the rendering of the
     * document in HTML-based editors.
     */
    private boolean generateHtmlStructure = true;

    /**
     * Enable to order the text as it appears on screen. Disable to leave the text in the order it
     * appears in the PDF file. In most cases, the order as it appears in the file is the correct
     * order.
     */
    private boolean sortByPosition = false;

    /**
     * Some PDFs overlap the same text multiple times to make it look bold. Keep the setting enabled
     * to remove the duplicate text.
     */
    private boolean suppressDuplicateOverlappingText = true;

    /** Enable to respect the bead structures in the PDF file (if available). */
    private boolean shouldSeparateByBeads = true;

    /**
     * Enable to try preserving line breaks and using consecutive line breaks to identify
     * paragraphs.
     */
    private boolean addMoreFormatting = true;

    /**
     * Consider an indented line to be a new paragraph if the indentation is greater than this
     * value.
     */
    private float indentThreshold = 2.0f;

    /** Lines further apart than this value are considered to be separate paragraphs. */
    private float dropThreshold = 2.5f;

    /**
     * Controls when spaces are introduced between characters. Increase to reduce the number of
     * spaces. Reduce if spaces are missing between characters.
     */
    private float averageCharTolerance = 0.3f;

    /**
     * Controls when spaces are introduced between characters. Increase to reduce the number of
     * spaces. Reduce if spaces are missing between characters.
     */
    private float spacingTolerance = 0.5f;

    @Override
    public boolean isSortByPosition()
    {
        return sortByPosition;
    }

    public void setSortByPosition(boolean aSortByPosition)
    {
        sortByPosition = aSortByPosition;
    }

    @Override
    public boolean isSuppressDuplicateOverlappingText()
    {
        return suppressDuplicateOverlappingText;
    }

    public void setSuppressDuplicateOverlappingText(boolean aSuppressDuplicateOverlappingText)
    {
        suppressDuplicateOverlappingText = aSuppressDuplicateOverlappingText;
    }

    @Override
    public boolean isShouldSeparateByBeads()
    {
        return shouldSeparateByBeads;
    }

    public void setShouldSeparateByBeads(boolean aShouldSeparateByBeads)
    {
        shouldSeparateByBeads = aShouldSeparateByBeads;
    }

    @Override
    public boolean isAddMoreFormatting()
    {
        return addMoreFormatting;
    }

    public void setAddMoreFormatting(boolean aAddMoreFormatting)
    {
        addMoreFormatting = aAddMoreFormatting;
    }

    @Override
    public float getIndentThreshold()
    {
        return indentThreshold;
    }

    public void setIndentThreshold(float aIndentThreshold)
    {
        indentThreshold = aIndentThreshold;
    }

    @Override
    public float getDropThreshold()
    {
        return dropThreshold;
    }

    public void setDropThreshold(float aDropThreshold)
    {
        dropThreshold = aDropThreshold;
    }

    @Override
    public float getAverageCharTolerance()
    {
        return averageCharTolerance;
    }

    public void setAverageCharTolerance(float aAverageCharTolerance)
    {
        averageCharTolerance = aAverageCharTolerance;
    }

    @Override
    public float getSpacingTolerance()
    {
        return spacingTolerance;
    }

    public void setSpacingTolerance(float aSpacingTolerance)
    {
        spacingTolerance = aSpacingTolerance;
    }

    @Override
    public boolean isGenerateHtmlStructure()
    {
        return generateHtmlStructure;
    }

    public void setGenerateHtmlStructure(boolean aGenerateHtmlStructure)
    {
        generateHtmlStructure = aGenerateHtmlStructure;
    }
}

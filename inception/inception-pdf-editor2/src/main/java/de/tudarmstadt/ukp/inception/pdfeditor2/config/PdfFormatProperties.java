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
package de.tudarmstadt.ukp.inception.pdfeditor2.config;

import de.tudarmstadt.ukp.inception.pdfeditor2.visual.VisualPDFTextStripper;

public interface PdfFormatProperties
{
    /**
     * @return Whether to sort the text as it appears on screen or leave it as it appears in the PDF
     *         file. This may help with PDFs generated by writers that output text sorted by style.
     *         However, it may cause problems with other types of PDFs, e.g. such that contain
     *         watermark text in the background.
     * 
     * @see VisualPDFTextStripper#setSortByPosition(boolean)
     */
    boolean isSortByPosition();

    /**
     * @return Whether to suppress duplicate overlapping text. By default the text stripper will
     *         attempt to remove text that overlaps each other. Word paints the same character
     *         several times in order to make it look bold. By setting this to false all text will
     *         be extracted, which means that certain sections will be duplicated, but better
     *         performance will be noticed.
     * 
     * @see VisualPDFTextStripper#setSuppressDuplicateOverlappingText(boolean)
     */
    boolean isSuppressDuplicateOverlappingText();

    /**
     * @return whether the text stripper should group the text output by a list of beads.
     * 
     * @see VisualPDFTextStripper#setShouldSeparateByBeads(boolean)
     */
    boolean isShouldSeparateByBeads();

    /**
     * @return whether There will some additional text formatting be added.
     * 
     * @see VisualPDFTextStripper#setAddMoreFormatting(boolean)
     */
    boolean isAddMoreFormatting();

    /**
     * @return the multiple of whitespace character widths for the current text which the current
     *         line start can be indented from the previous line start beyond which the current line
     *         start is considered to be a paragraph start.
     * 
     * @see VisualPDFTextStripper#setIndentThreshold(float)
     */
    float getIndentThreshold();

    /**
     * @return the minimum whitespace, as a multiple of the max height of the current characters
     *         beyond which the current line start is considered to be a paragraph start.
     * 
     * @see VisualPDFTextStripper#setDropThreshold(float)
     */
    float getDropThreshold();

    /**
     * @return the character width-based tolerance value that is used to estimate where spaces in
     *         text should be added. Note that the default value for this has been determined from
     *         trial and error. Setting this value larger will reduce the number of spaces added.
     * 
     * @see VisualPDFTextStripper#setAverageCharTolerance(float)
     */
    float getAverageCharTolerance();

    /**
     * @return the space width-based tolerance value that is used to estimate where spaces in text
     *         should be added. Note that the default value for this has been determined from trial
     *         and error. Setting this value larger will reduce the number of spaces added.
     * 
     * @see VisualPDFTextStripper#setSpacingTolerance(float)
     */
    float getSpacingTolerance();

    boolean isGenerateHtmlStructure();

}

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

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties("format.pdf")
public class PdfFormatPropertiesImpl
    implements PdfFormatProperties
{
    private boolean generateHtmlStructure = true;

    private boolean sortByPosition = false;
    private boolean suppressDuplicateOverlappingText = true;
    private boolean shouldSeparateByBeads = true;

    private boolean addMoreFormatting = true;
    private float indentThreshold = 2.0f;
    private float dropThreshold = 2.5f;

    private float averageCharTolerance = 0.3f;
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

/*
 * Copyright 2019
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
package de.tudarmstadt.ukp.inception.externalsearch.cluster;

import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;

import de.tudarmstadt.ukp.inception.externalsearch.ExternalSearchResult;

public class ExtractedUnit
{
    private final ExternalSearchResult result;
    private final String text;
    private final double score;

    public ExtractedUnit(String aText, double aScore, ExternalSearchResult aResult)
    {
        text = aText;
        score = aScore;
        result = aResult;
    }

    public String getText()
    {
        return text;
    }

    public double getScore()
    {
        return score;
    }
    
    /**
     * The original result from which this unit was extracted.
     */
    public ExternalSearchResult getResult()
    {
        return result;
    }

    @Override
    public String toString()
    {
        return new ToStringBuilder(this, ToStringStyle.NO_CLASS_NAME_STYLE).append("text", text)
                .append("score", score).toString();
    }
}

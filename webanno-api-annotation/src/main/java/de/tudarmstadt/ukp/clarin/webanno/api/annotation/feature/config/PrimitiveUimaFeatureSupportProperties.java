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
package de.tudarmstadt.ukp.clarin.webanno.api.annotation.feature.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties("annotation.feature-support.string")
public class PrimitiveUimaFeatureSupportProperties
{
    /**
     * If the tagset is larger than the threshold, an auto-complete field is used instead of a
     * standard combobox.
     */
    private int autoCompleteThreshold = 75;

    /**
     * When an auto-complete field is used, this determines the maximum number of items shown in the
     * dropdown menu.
     */
    private int autoCompleteMaxResults = 100;

    public int getAutoCompleteThreshold()
    {
        return autoCompleteThreshold;
    }

    public void setAutoCompleteThreshold(int aAutoCompleteThreshold)
    {
        autoCompleteThreshold = aAutoCompleteThreshold;
    }

    public int getAutoCompleteMaxResults()
    {
        return autoCompleteMaxResults;
    }

    public void setAutoCompleteMaxResults(int aAutoCompleteMaxResults)
    {
        autoCompleteMaxResults = aAutoCompleteMaxResults;
    }
}

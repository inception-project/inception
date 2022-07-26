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
package de.tudarmstadt.ukp.inception.annotation.feature.string;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * <p>
 * This class is exposed as a Spring Component via {@code AnnotationSchemaServiceAutoConfiguration}.
 * </p>
 */
@ConfigurationProperties("annotation.feature-support.string")
public class StringFeatureSupportPropertiesImpl
    implements StringFeatureSupportProperties
{
    private int comboBoxThreshold = 6;

    private int autoCompleteThreshold = 75;

    private int autoCompleteMaxResults = 100;

    @Override
    public int getComboBoxThreshold()
    {
        return comboBoxThreshold;
    }

    public void setComboBoxThreshold(int aComboBoxThreshold)
    {
        comboBoxThreshold = aComboBoxThreshold;
    }

    @Override
    public int getAutoCompleteThreshold()
    {
        return autoCompleteThreshold;
    }

    public void setAutoCompleteThreshold(int aAutoCompleteThreshold)
    {
        autoCompleteThreshold = aAutoCompleteThreshold;
    }

    @Override
    public int getAutoCompleteMaxResults()
    {
        return autoCompleteMaxResults;
    }

    public void setAutoCompleteMaxResults(int aAutoCompleteMaxResults)
    {
        autoCompleteMaxResults = aAutoCompleteMaxResults;
    }
}

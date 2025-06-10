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
package de.tudarmstadt.ukp.inception.search.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import de.tudarmstadt.ukp.inception.preferences.PreferenceKey;
import de.tudarmstadt.ukp.inception.preferences.PreferenceValue;

@JsonIgnoreProperties(ignoreUnknown = true)
public class AnnotationSearchState
    implements PreferenceValue
{
    public static final PreferenceKey<AnnotationSearchState> KEY_SEARCH_STATE = new PreferenceKey<>(
            AnnotationSearchState.class, "annotation/search");

    private static final long serialVersionUID = 6273739145955045285L;

    private boolean caseSensitiveDocumentText = false;
    private boolean caseSensitiveFeatureValues = true;

    public void setCaseSensitive(boolean aCaseSensitive)
    {
        caseSensitiveDocumentText = aCaseSensitive;
        caseSensitiveFeatureValues = aCaseSensitive;
    }

    public boolean isCaseSensitiveDocumentText()
    {
        return caseSensitiveDocumentText;
    }

    public void setCaseSensitiveDocumentText(boolean aCaseSensitiveDocumentText)
    {
        caseSensitiveDocumentText = aCaseSensitiveDocumentText;
    }

    public boolean isCaseSensitiveFeatureValues()
    {
        return caseSensitiveFeatureValues;
    }

    public void setCaseSensitiveFeatureValues(boolean aCaseSensitiveFeatureValues)
    {
        caseSensitiveFeatureValues = aCaseSensitiveFeatureValues;
    }
}

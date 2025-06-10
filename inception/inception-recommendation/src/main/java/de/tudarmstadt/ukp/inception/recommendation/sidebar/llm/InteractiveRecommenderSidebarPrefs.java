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
package de.tudarmstadt.ukp.inception.recommendation.sidebar.llm;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import de.tudarmstadt.ukp.inception.preferences.PreferenceKey;
import de.tudarmstadt.ukp.inception.preferences.PreferenceValue;
import de.tudarmstadt.ukp.inception.recommendation.imls.llm.support.prompt.PromptingMode;
import de.tudarmstadt.ukp.inception.recommendation.imls.llm.support.response.ExtractionMode;

@JsonIgnoreProperties(ignoreUnknown = true)
public class InteractiveRecommenderSidebarPrefs
    implements PreferenceValue
{
    private static final long serialVersionUID = 6165556429555703817L;

    public static final PreferenceKey<InteractiveRecommenderSidebarPrefs> KEY_INTERACTIVE_RECOMMENDER_SIDEBAR_PREFS = //
            new PreferenceKey<>(InteractiveRecommenderSidebarPrefs.class,
                    "annotation/editor/interactive-recommender-sidebar");

    private Long lastRecommenderUsed;
    private Long lastLayerUsed;
    private Long lastFeatureUsed;
    private PromptingMode lastPromptingModeUsed;
    private ExtractionMode lastExtractionModeUsed;
    private String lastPromptUsed;

    private Boolean lastJustificationEnabled;

    public Long getLastRecommenderUsed()
    {
        return lastRecommenderUsed;
    }

    public void setLastRecommenderUsed(Long aLastRecommenderUsed)
    {
        lastRecommenderUsed = aLastRecommenderUsed;
    }

    public Long getLastLayerUsed()
    {
        return lastLayerUsed;
    }

    public void setLastLayerUsed(Long aLastLayerUsed)
    {
        lastLayerUsed = aLastLayerUsed;
    }

    public Long getLastFeatureUsed()
    {
        return lastFeatureUsed;
    }

    public void setLastFeatureUsed(Long aLastFeatureUsed)
    {
        lastFeatureUsed = aLastFeatureUsed;
    }

    public PromptingMode getLastPromptingModeUsed()
    {
        return lastPromptingModeUsed;
    }

    public void setLastPromptingModeUsed(PromptingMode aLastPromptingModeUsed)
    {
        lastPromptingModeUsed = aLastPromptingModeUsed;
    }

    public ExtractionMode getLastExtractionModeUsed()
    {
        return lastExtractionModeUsed;
    }

    public void setLastExtractionModeUsed(ExtractionMode aLastExtractionModeUsed)
    {
        lastExtractionModeUsed = aLastExtractionModeUsed;
    }

    public String getLastPromptUsed()
    {
        return lastPromptUsed;
    }

    public void setLastPromptUsed(String aLastPromptUsed)
    {
        lastPromptUsed = aLastPromptUsed;
    }

    public void setLastJustificationEnabled(Boolean aJustificationEnabled)
    {
        lastJustificationEnabled = aJustificationEnabled;
    }

    public Boolean isLastJustificationEnabled()
    {
        return lastJustificationEnabled;
    }
}

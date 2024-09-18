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
package de.tudarmstadt.ukp.inception.recommendation.api.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import de.tudarmstadt.ukp.inception.preferences.PreferenceValue;

@JsonIgnoreProperties(ignoreUnknown = true)
public class RecommenderGeneralSettings
    implements PreferenceValue
{
    private static final long serialVersionUID = -1889889346307217345L;

    private boolean annotatorAllowedToExportModel = true;

    private boolean waitForRecommendersOnOpenDocument = false;

    private boolean showRecommendationsWhenViewingOtherUser = false;

    private boolean showRecommendationsWhenViewingCurationUser = true;

    public boolean isWaitForRecommendersOnOpenDocument()
    {
        return waitForRecommendersOnOpenDocument;
    }

    public void setWaitForRecommendersOnOpenDocument(boolean aWaitForRecommendersOnOpenDocument)
    {
        waitForRecommendersOnOpenDocument = aWaitForRecommendersOnOpenDocument;
    }

    public boolean isShowRecommendationsWhenViewingOtherUser()
    {
        return showRecommendationsWhenViewingOtherUser;
    }

    public void setShowRecommendationsWhenViewingOtherUser(
            boolean aShowRecommendationsWhenViewingOtherUser)
    {
        showRecommendationsWhenViewingOtherUser = aShowRecommendationsWhenViewingOtherUser;
    }

    public boolean isShowRecommendationsWhenViewingCurationUser()
    {
        return showRecommendationsWhenViewingCurationUser;
    }

    public void setShowRecommendationsWhenViewingCurationUser(
            boolean aShowRecommendationsWhenViewingCurationUser)
    {
        showRecommendationsWhenViewingCurationUser = aShowRecommendationsWhenViewingCurationUser;
    }

    public void setAnnotatorAllowedToExportModel(boolean aAnnotatorAllowedToExportModel)
    {
        annotatorAllowedToExportModel = aAnnotatorAllowedToExportModel;
    }

    public boolean isAnnotatorAllowedToExportModel()
    {
        return annotatorAllowedToExportModel;
    }
}

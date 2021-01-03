/*
 * Copyright 2021
 * Ubiquitous Knowledge Processing (UKP) Lab
 * Technische Universität Darmstadt
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package de.tudarmstadt.ukp.inception.ui.core.dashboard.dashlet;

import static de.tudarmstadt.ukp.clarin.webanno.api.WebAnnoConst.CURATION_USER;

import com.fasterxml.jackson.annotation.JsonProperty;

import de.tudarmstadt.ukp.clarin.webanno.model.SourceDocument;
import de.tudarmstadt.ukp.inception.log.model.LoggedEvent;

class Activity
{
    public final @JsonProperty long id;
    public final @JsonProperty long projectId;
    public final @JsonProperty long documentId;
    public final @JsonProperty String documentName;
    public final @JsonProperty String user;
    public final @JsonProperty String annotator;
    public final @JsonProperty long timestamp;
    public final @JsonProperty String link;
    public final @JsonProperty String type;

    public Activity(LoggedEvent aEvent, SourceDocument aDocument, String aLink)
    {
        super();
        id = aEvent.getId();
        projectId = aEvent.getProject();
        documentId = aEvent.getDocument();
        documentName = aDocument.getName();
        user = aEvent.getUser();
        annotator = aEvent.getAnnotator();
        timestamp = aEvent.getCreated().getTime();
        link = aLink;
        type = CURATION_USER.equals(aEvent.getAnnotator()) ? "Curation" : "Annotation";
    }
}

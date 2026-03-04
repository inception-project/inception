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
package de.tudarmstadt.ukp.inception.remoteapi.next.model;

import java.time.Instant;

import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationSession;
import io.swagger.v3.oas.annotations.media.Schema;

public class RAnnotationSession
{
    @Schema(description = "Unique session identifier.")
    public final long id;

    @Schema(description = "Username of the annotator.")
    public final String annotator;

    @Schema(description = "When the annotator opened the document.")
    public final Instant openedAt;

    @Schema(description = "When the session ended. Null if the tab was never cleanly closed.")
    public final Instant closedAt;

    @Schema(description = "Time (ms) the annotator had the page open and was actively "
            + "interacting (mouse/keyboard). Pauses on idle or tab-switch.")
    public final long activeTimeMs;

    @Schema(description = "Number of annotation changes (create/update/delete) made this session.")
    public final int changesCount;

    public RAnnotationSession(AnnotationSession aSession)
    {
        id = aSession.getId();
        annotator = aSession.getUser();
        openedAt = aSession.getOpenedAt();
        closedAt = aSession.getClosedAt();
        activeTimeMs = aSession.getActiveTimeMs();
        changesCount = aSession.getChangesCount();
    }
}

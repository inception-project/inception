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
package de.tudarmstadt.ukp.inception.experimental.api;

import java.io.IOException;

import de.tudarmstadt.ukp.clarin.webanno.api.annotation.event.*;
import de.tudarmstadt.ukp.inception.experimental.api.messages.request.*;

public interface AnnotationSystemAPI
{
    void handleNewDocument(NewDocumentRequest aNewDocumentRequest) throws IOException;

    void handleNewViewport(NewViewportRequest aNewViewportRequest) throws IOException;

    void handleSelectSpan(SelectSpanRequest aSelectSpanRequest)
        throws IOException;

    void handleUpdateSpan(UpdateSpanRequest aUpdateSpanRequest)
        throws IOException;

    void handleCreateSpan(CreateSpanRequest aCreateSpanRequest)
        throws IOException;

    void handleDeleteSpan(DeleteSpanRequest aDeleteSpanRequest) throws IOException;

    void handleSelectRelation(SelectSpanRequest aSelectSpanRequest)
        throws IOException;

    void handleUpdateRelation(UpdateSpanRequest aUpdateSpanRequest)
        throws IOException;

    void handleCreateRelation(CreateSpanRequest aCreateSpanRequest)
        throws IOException;

    void handleDeleteRelation(DeleteSpanRequest aDeleteSpanRequest) throws IOException;

    void handleSaveWordAlignment(SaveWordAlignmentRequest aSaveWordAlignmentRequest);

    void createErrorMessage(String aMessage, String aUser) throws IOException;

    void onSpanUpdatedEventHandler(FeatureValueUpdatedEvent aEvent);

    void onSpanCreatedEventHandler(SpanCreatedEvent aEvent) throws IOException;

    void onSpanDeletedEventHandler(SpanDeletedEvent aEvent) throws IOException;

    void onRelationCreatedEventHandler(RelationCreatedEvent aEvent);

    void onRelationDeletedEventHandler(RelationDeletedEvent aEvent);
}

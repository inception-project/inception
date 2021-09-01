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

import de.tudarmstadt.ukp.clarin.webanno.api.annotation.event.FeatureValueUpdatedEvent;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.event.RelationCreatedEvent;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.event.RelationDeletedEvent;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.event.SpanCreatedEvent;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.event.SpanDeletedEvent;
import de.tudarmstadt.ukp.inception.experimental.api.messages.request.DeleteAnnotationRequest;
import de.tudarmstadt.ukp.inception.experimental.api.messages.request.DocumentRequest;
import de.tudarmstadt.ukp.inception.experimental.api.messages.request.UpdateFeaturesRequest;
import de.tudarmstadt.ukp.inception.experimental.api.messages.request.create.CreateArcRequest;
import de.tudarmstadt.ukp.inception.experimental.api.messages.request.create.CreateSpanRequest;

public interface AnnotationSystemAPI
{
    void handleDocumentRequest(DocumentRequest aDocumentRequest) throws IOException;

    void handleCreateSpan(CreateSpanRequest aCreateSpanRequest)
        throws IOException;

    void handleUpdateFeatures(UpdateFeaturesRequest aUpdateFeaturesRequest) throws IOException;

    void handleCreateArc(CreateArcRequest aCreateArcRequest)
        throws IOException;

    void handleDeleteAnnotation(DeleteAnnotationRequest aDeleteAnnotationRequest) throws IOException;

    void createAdviceMessage(String aMessage, String aUser) throws IOException;

    void onSpanCreatedEventHandler(SpanCreatedEvent aEvent) throws IOException;

    void onSpanDeletedEventHandler(SpanDeletedEvent aEvent) throws IOException;

    void onArcCreatedEventHandler(RelationCreatedEvent aEvent) throws IOException;

    void onArcDeletedEventHandler(RelationDeletedEvent aEvent) throws IOException;

    void onFeatureUpdatedEventHandler(FeatureValueUpdatedEvent aEvent) throws IOException;

}

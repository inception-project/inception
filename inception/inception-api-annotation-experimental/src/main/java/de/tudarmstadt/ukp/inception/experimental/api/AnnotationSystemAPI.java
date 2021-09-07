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
import de.tudarmstadt.ukp.inception.experimental.api.messages.response.AdviceMessage;
import de.tudarmstadt.ukp.inception.experimental.api.websocket.AnnotationProcessAPI;

/**
 * Interface-class for the System API
 *
 * The System API handles all back-end computations and listens to all Events regarding
 * the annotation workflow (events in
 * @see inception-api-annotation/src/main/java/../../event/*
 *
 * There are two different types of methods in this class:
 *      handle() methods:
 *          All handle() methods retrieve a class object as argument that has been generated from the
 *          clients message JSON string payload in the ProcessAPI. All handle() methods simply
 *          use the retrieved data to perform the requested operation, like deleting a span-annotation
 *          or updating a feature value of an annotation.
 *          NOTE: All handle() methods contain a try-catch block to handle errors. Whenever an error
 *          occurs, informative data is send back to the client that has sent the request. This is done in
 *          the catch-block via 'createAdviceMessage()'.
 *      onEventHandler() methods:
 *          As already explained, these methods listen to specific events created on the server.
 *          This 'listening' is performed via an '@EventListener' annotation before the method definition
 *          retrieving the corresponding event as method-parameter. The onEventHandler() message always
 *          retrieve the data from the Event and create a suitable class-object as response.
 *          Finally, they always invoke the suitable method in the
 *          @see AnnotationProcessAPI to forward the data from the event to the clients.
 *          NOTE: Events can be triggered in the back-end at any time.
 *
 * NOTE: The System API also contains many private support methods. These can only be
 * found in the
 * @see AnnotationSystemAPI implementation. All support methods are commented in detail.
 *
 * NOTE: For further information please look into the README file
 *
 **/
public interface AnnotationSystemAPI
{
    /**
     * handle() methods as explained in Interface-definition.
     * param: Classes parsed from the messages JSON string payload.
     * Their implementation details vary a lot, however their purpose is
     * always the same:
     *      1. Retrieve the CAS for a given sourcedocument
     *      2. Retrieve the corresponding TypeAdapter
     *      3. Handle the request
     */
    void handleDocumentRequest(DocumentRequest aDocumentRequest) throws IOException;

    void handleCreateSpan(CreateSpanRequest aCreateSpanRequest)
        throws IOException;

    void handleUpdateFeatures(UpdateFeaturesRequest aUpdateFeaturesRequest) throws IOException;

    void handleCreateArc(CreateArcRequest aCreateArcRequest)
        throws IOException;

    void handleDeleteAnnotation(DeleteAnnotationRequest aDeleteAnnotationRequest) throws IOException;

    void createAdviceMessage(String aMessage, String aUser, AdviceMessage.TYPE aType) throws IOException;

    /**
     * onEventHandler() classes as explained in Interface-definition.
     * param: The event through which they got triggered. They always
     * create a suitable class from the event and forwards the data to the
     * Process API where the data is forwarded to the clients.
     */

    void onSpanCreatedEventHandler(SpanCreatedEvent aEvent) throws IOException;

    void onSpanDeletedEventHandler(SpanDeletedEvent aEvent) throws IOException;

    void onArcCreatedEventHandler(RelationCreatedEvent aEvent) throws IOException;

    void onArcDeletedEventHandler(RelationDeletedEvent aEvent) throws IOException;

    void onFeatureUpdatedEventHandler(FeatureValueUpdatedEvent aEvent) throws IOException;

}

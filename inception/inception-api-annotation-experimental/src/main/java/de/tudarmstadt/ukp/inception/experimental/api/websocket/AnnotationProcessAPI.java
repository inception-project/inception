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
package de.tudarmstadt.ukp.inception.experimental.api.websocket;

import java.io.IOException;

import de.tudarmstadt.ukp.inception.experimental.api.messages.response.*;
import de.tudarmstadt.ukp.inception.experimental.api.messages.response.relation.*;
import de.tudarmstadt.ukp.inception.experimental.api.messages.response.span.*;
import org.springframework.messaging.Message;

public interface AnnotationProcessAPI
{
    void receiveNewDocumentRequest(Message<String> aMessage) throws IOException;

    void sendNewDocumentResponse(NewDocumentResponse aNewDocumentResponse, String aUser)
        throws IOException;

    void receiveNewViewportRequest(Message<String> aMessage) throws IOException;

    void sendNewViewportResponse(NewViewportResponse aNewViewportResponse, String aUser)
        throws IOException;

    void receiveSelectAnnotationRequest(Message<String> aMessage) throws IOException;

    void receiveSelectRelationRequest(Message<String> aMessage) throws IOException;

    void sendSelectAnnotationResponse(SelectSpanResponse aSelectSpanResponse, String aUser)
        throws IOException;

    void sendSelectRelationResponse(SelectRelationResponse aSelectRelationResponse, String aUser)
        throws IOException;

    void receiveUpdateAnnotationRequest(Message<String> aMessage) throws IOException;

    void receiveUpdateRelationRequest(Message<String> aMessage) throws IOException;

    void sendUpdateAnnotationResponse(UpdateSpanResponse aUpdateSpanResponse, String aProjectID,
            String aDocumentID, String aViewport)
        throws IOException;

    void sendUpdateRelationResponse(UpdateRelationResponse aUpdateRelationResponse,
            String aProjectID, String aDocumentID, String aViewport)
        throws IOException;

    void receiveCreateAnnotationRequest(Message<String> aMessage) throws IOException;

    void sendCreateAnnotationResponse(CreateSpanResponse aCreateSpanResponse, String aProjectID,
            String aDocumentID, String aViewport)
        throws IOException;

    void receiveCreateRelationRequest(Message<String> aMessage) throws IOException;

    void sendCreateRelationResponse(CreateRelationResponse aCreateRelationResponse,
            String aProjectID, String aDocumentID, String aViewport)
        throws IOException;

    void receiveDeleteAnnotationRequest(Message<String> aMessage) throws IOException;

    void sendDeleteAnnotationResponse(DeleteSpanResponse aDeleteSpanResponse, String aProjectID,
            String aDocumentID, String aViewport)
        throws IOException;

    void receiveDeleteRelationRequest(Message<String> aMessage) throws IOException;

    void sendDeleteRelationResponse(DeleteRelationResponse aDeleteRelationResponse,
            String aProjectID, String aDocumentID, String aViewport)
        throws IOException;

    void receiveAllSpansRequest(Message<String> aMessage) throws  IOException;

    void sendAllSpansResponse(AllSpanResponse aAllSpanResponse, String aUser) throws IOException;

    void receiveAllRelationsRequest(Message<String> aMessage) throws  IOException;

    void sendAllRelationsResponse(AllRelationResponse aAllRelationResponse, String aUser) throws IOException;

    void sendErrorMessage(ErrorMessage aErrorMessage, String aUser) throws IOException;

}

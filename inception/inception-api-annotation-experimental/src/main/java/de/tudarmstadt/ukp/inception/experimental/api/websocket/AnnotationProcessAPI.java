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

import org.springframework.messaging.Message;

import de.tudarmstadt.ukp.inception.experimental.api.messages.response.DocumentResponse;
import de.tudarmstadt.ukp.inception.experimental.api.messages.response.ErrorMessage;
import de.tudarmstadt.ukp.inception.experimental.api.messages.response.ViewportResponse;
import de.tudarmstadt.ukp.inception.experimental.api.messages.response.arc.CreateArcMessage;
import de.tudarmstadt.ukp.inception.experimental.api.messages.response.arc.DeleteArcMessage;
import de.tudarmstadt.ukp.inception.experimental.api.messages.response.arc.SelectArcResponse;
import de.tudarmstadt.ukp.inception.experimental.api.messages.response.arc.UpdateArcMessage;
import de.tudarmstadt.ukp.inception.experimental.api.messages.response.span.CreateSpanMessage;
import de.tudarmstadt.ukp.inception.experimental.api.messages.response.span.DeleteSpanMessage;
import de.tudarmstadt.ukp.inception.experimental.api.messages.response.span.SelectSpanResponse;
import de.tudarmstadt.ukp.inception.experimental.api.messages.response.span.UpdateSpanMessage;


public interface AnnotationProcessAPI
{
    void receiveDocumentRequest(Message<String> aMessage) throws IOException;

    void sendDocumentResponse(DocumentResponse aDocumentResponse, String aUser)
        throws IOException;

    void receiveViewportRequest(Message<String> aMessage) throws IOException;

    void sendViewportResponse(ViewportResponse aViewportResponse, String aUser)
        throws IOException;

    void receiveSelectSpanRequest(Message<String> aMessage) throws IOException;

    void receiveSelectArcRequest(Message<String> aMessage) throws IOException;

    void sendSelectSpanResponse(SelectSpanResponse aSelectSpanResponse, String aUser)
        throws IOException;

    void sendSelectArcResponse(SelectArcResponse aSelectArcResponse, String aUser)
        throws IOException;

    void receiveUpdateSpanRequest(Message<String> aMessage) throws Exception;

    void receiveUpdateArcRequest(Message<String> aMessage) throws Exception;

    void sendUpdateSpan(UpdateSpanMessage aUpdateSpanMessage, String aProjectID,
                                      String aDocumentID, String aViewport)
        throws IOException;

    void sendUpdateArc(UpdateArcMessage aUpdateArcMessage,
                                    String aProjectID, String aDocumentID, String aViewport)
        throws IOException;

    void receiveCreateSpanRequest(Message<String> aMessage) throws IOException;

    void sendCreateSpan(CreateSpanMessage aCreateSpanMessage, String aProjectID,
                                      String aDocumentID, String aViewport)
        throws IOException;

    void receiveCreateArcRequest(Message<String> aMessage) throws IOException;

    void sendCreateArc(CreateArcMessage aCreateArcMessage,
                                    String aProjectID, String aDocumentID, String aViewport)
        throws IOException;

    void receiveDeleteSpanRequest(Message<String> aMessage) throws IOException;

    void sendDeleteSpan(DeleteSpanMessage aDeleteSpanMessage, String aProjectID,
                                      String aDocumentID, String aViewport)
        throws IOException;

    void receiveDeleteArcRequest(Message<String> aMessage) throws IOException;

    void sendDeleteArc(DeleteArcMessage aDeleteArcMessage,
                                    String aProjectID, String aDocumentID, String aViewport)
        throws IOException;

    void sendErrorMessage(ErrorMessage aErrorMessage, String aUser) throws IOException;

}

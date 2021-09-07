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

import de.tudarmstadt.ukp.inception.experimental.api.messages.response.AdviceMessage;
import de.tudarmstadt.ukp.inception.experimental.api.messages.response.DeleteAnnotationMessage;
import de.tudarmstadt.ukp.inception.experimental.api.messages.response.DocumentMessage;
import de.tudarmstadt.ukp.inception.experimental.api.messages.response.UpdateFeatureMessage;
import de.tudarmstadt.ukp.inception.experimental.api.messages.response.create.ArcCreatedMessage;
import de.tudarmstadt.ukp.inception.experimental.api.messages.response.create.SpanCreatedMessage;


/**
 * Interface-class for the Process API
 *
 * The Process API represents the communication end-point between client (editor) and server (back-end).
 * All data sent by any of the clients via websocket will be retrieved here.
 *
 * Therefore, each receive() method have a '@MessageMapping' annotation for a certain topic.
 * The topics can be found
 * @see AnnotationProcessAPIImpl
 *
 * Every send() method ends with 'simpMessagingTemplate.convertAndSend()'.
 * This sends the message to a certain topic with a payload which contains
 * always a 'JSONUtil.toJsonString()' representation of a message
 * from the '../messages' package
 *
 * LOG.debug() has been added for all of the methods in order to better track incoming
 * and outgoing data.
 *
 * NOTE: Not every sending of a message must precede an incoming message. The server can
 * send a message to ANY client without the client requesting something.
 * Event Types created on the server-side in 'inception-api-annotation/src/main/java/../../event/*'
 *
 * NOTE: For further information please look into the README file
 **/
public interface AnnotationProcessAPI
{
    void receiveDocumentRequest(Message<String> aMessage) throws IOException;

    void sendDocumentResponse(DocumentMessage aDocumentResponse, String aUser)
        throws IOException;

    void receiveUpdateFeaturesRequest(Message<String> aMessage) throws Exception;

    void sendUpdateFeatures(UpdateFeatureMessage aUpdateFeaturesMessage, String aProjectID,
                            String aDocumentID)
        throws IOException;

    void receiveCreateSpanRequest(Message<String> aMessage) throws IOException;

    void sendCreateSpan(SpanCreatedMessage aCreateSpanMessage, String aProjectID,
                        String aDocumentID)
        throws IOException;

    void receiveCreateArcRequest(Message<String> aMessage) throws IOException;

    void sendCreateArc(ArcCreatedMessage aCreateArcMessage,
                       String aProjectID, String aDocumentID)
        throws IOException;

    void receiveDeleteAnnotationRequest(Message<String> aMessage) throws IOException;

    void sendDeleteAnnotation(DeleteAnnotationMessage aDeleteAnnotationMessage, String aProjectID,
                              String aDocumentID)
        throws IOException;


    void sendAdviceMessage(AdviceMessage aAdviceMessage, String aUser) throws IOException;

}

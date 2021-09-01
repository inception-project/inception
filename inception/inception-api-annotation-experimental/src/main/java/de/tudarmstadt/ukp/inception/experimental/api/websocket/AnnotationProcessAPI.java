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
import de.tudarmstadt.ukp.inception.experimental.api.messages.response.UpdateFeaturesMessage;
import de.tudarmstadt.ukp.inception.experimental.api.messages.response.create.ArcCreatedMessage;
import de.tudarmstadt.ukp.inception.experimental.api.messages.response.create.SpanCreatedMessage;


public interface AnnotationProcessAPI
{
    void receiveDocumentRequest(Message<String> aMessage) throws IOException;

    void sendDocumentResponse(DocumentMessage aDocumentResponse, String aUser)
        throws IOException;

    void receiveUpdateFeaturesRequest(Message<String> aMessage) throws Exception;

    void sendUpdateFeatures(UpdateFeaturesMessage aUpdateFeaturesMessage, String aProjectID,
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

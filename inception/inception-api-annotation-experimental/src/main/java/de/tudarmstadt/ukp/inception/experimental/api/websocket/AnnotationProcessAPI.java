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

import de.tudarmstadt.ukp.inception.experimental.api.message.AnnotationMessage;
import de.tudarmstadt.ukp.inception.experimental.api.message.DocumentMessage;
import de.tudarmstadt.ukp.inception.experimental.api.message.ErrorMessage;
import de.tudarmstadt.ukp.inception.experimental.api.message.ViewportMessage;

public interface AnnotationProcessAPI
{
    void handleReceiveDocumentRequest(Message<String> aMessage) throws IOException;

    void handleSendDocumentRequest(DocumentMessage documentMessage, String aUser)
        throws IOException;

    void handleReceiveViewportRequest(Message<String> aMessage) throws IOException;

    void handleSendViewportRequest(ViewportMessage aViewportMessage, String aUser)
        throws IOException;

    void handleReceiveSelectAnnotation(Message<String> aMessage) throws IOException;

    void handleSendSelectAnnotation(AnnotationMessage aAnnotationMessage, String aUser)
        throws IOException;

    void handleReceiveCreateAnnotation(Message<String> aMessage) throws IOException;

    void handleReceiveDeleteAnnotation(Message<String> aMessage) throws IOException;

    void handleSendUpdateAnnotation(AnnotationMessage aAnnotationMessage, String aProjectID,
            String aDocumentID, String aViewport)
        throws IOException;

    void handleSendErrorMessage(ErrorMessage aErrorMessage, String aUser)
        throws IOException;

}

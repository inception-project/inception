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

import org.apache.uima.cas.CAS;
import org.springframework.messaging.Message;

public interface WebsocketController
{
    void handleDocumentRequest(Message<String> aMessage) throws IOException;

    void handleViewportRequest(Message<String> aMessage) throws IOException;

    void handleSelectAnnotation(Message<String> aMessage) throws IOException;

    void handleNewAnnotation(Message<String> aMessage) throws IOException;

    void handleDeleteAnnotation(Message<String> aMessage) throws IOException;

    /**
     * Returns CAS from websocket message. All three String parameters are contained in the header
     * of the websocket message
     *
     * @param aProject
     *            string
     * @param aDocument
     *            string
     * @param aUser
     *            string
     * @return CAS
     */
    CAS getCasForDocument(String aProject, String aDocument, String aUser);

    String[] purifyPayload(String aPayload);
}

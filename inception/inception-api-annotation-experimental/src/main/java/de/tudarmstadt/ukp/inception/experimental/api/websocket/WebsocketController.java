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

import org.apache.uima.cas.CAS;
import org.springframework.context.ApplicationEvent;
import org.springframework.messaging.Message;
import org.springframework.messaging.handler.annotation.DestinationVariable;

import de.tudarmstadt.ukp.inception.htmleditor.annotatorjs.model.Annotation;

public interface WebsocketController
{
    /***
     * Push messages on received application events to named user
     */
    void onApplicationEvent(ApplicationEvent aEvent);

    void handleNewDocument(Message<String> aMessage);

    String publishNewDocument(@DestinationVariable String aClient, String aText);

    void handleRequestViewport(Message<String> aMessage);

    String publishRequestViewport(@DestinationVariable String aClient, String aText);

    void handleSelectAnnotation(Message<String> aMessage);

    Annotation publishSelectAnnotation(@DestinationVariable String aClient, Annotation aAnnotation);

    String handleNewAnnotation(Message<String> aMessage);

    String handleDeleteAnnotation(Message<String> aMessage);

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


}

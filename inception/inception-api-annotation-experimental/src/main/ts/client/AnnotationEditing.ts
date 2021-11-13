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
import { Client, Stomp, StompSubscription, messageCallbackType, IFrame } from '@stomp/stompjs';

/**
 * Implementation of the Interface AnnotationExperienceAPI within that package.
 *
 * For further details @see interface class (AnnotationExperienceAPI.ts).
 *
 **/
export class AnnotationEditing {

    stompClient: Client;
    webSocket: WebSocket;
    initSubscription: StompSubscription;
    updateSubscription: StompSubscription;

    /**
     * Constructor: creates the Annotation Experience API.
     * @param aProjectId: ID of the project, required in order to subscribe to the correct channels.
     * @param aDocumentId: ID of the document, required in order to subscribe to the correct channels.
     * @param aAnnotatorName: String representation of the annotatorName, required in order to subscribe to the correct channels.
     * @param aUrl: The URL required by Websocket and the stomp broker in order to establish a connection
     * @NOTE: the connect() method will automatically be performed, there is no need to create a Websocket connection
    * manually.
    */
    constructor(aUrl: string) {
        this.connect(aUrl)
    }


    /**
     * Creates the Websocket connection with the stomp broker.
     * @NOTE: When a connection is established, onConnect() will be called automatically so
     * there is no need to call it. Also, all subscriptions will be handled automatically.
     */
    connect(aUrl: string) {
        if (this.stompClient) {
            throw "Already connected";
        }

        this.stompClient = Stomp.over(() => this.webSocket = new WebSocket(aUrl));
        this.stompClient.reconnectDelay = 5000;
        this.stompClient.onConnect = () => {
            this.stompClient.subscribe("/user/queue/errors", this.handleProtocolError);
        }
        this.stompClient.onStompError = this.handleBrokerError;
        this.stompClient.activate();
    }

    disconnect() {
        this.stompClient.deactivate();
        this.webSocket.close();
    }

    handleBrokerError(receipt: IFrame) {
        console.log('Broker reported error: ' + receipt.headers['message']);
        console.log('Additional details: ' + receipt.body);
    }

    handleProtocolError(msg) {
        console.log(msg);
    }

    subscribeToViewport(aViewportTopic: string, initCallback: messageCallbackType, updateCallback: messageCallbackType) {
        this.unsubscribeFromViewport();
        this.initSubscription = this.stompClient.subscribe('/app' + aViewportTopic, initCallback);
        this.updateSubscription = this.stompClient.subscribe('/topic' + aViewportTopic, updateCallback);
    }

    unsubscribeFromViewport() {
        if (this.initSubscription) {
            this.initSubscription.unsubscribe();
        }
        if (this.updateSubscription) {
            this.updateSubscription.unsubscribe();
        }
    }
}


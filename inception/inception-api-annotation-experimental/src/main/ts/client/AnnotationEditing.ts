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
import {Client, Stomp, StompSubscription} from '@stomp/stompjs';

/**
 * Implementation of the Interface AnnotationExperienceAPI within that package.
 *
 * For further details @see interface class (AnnotationExperienceAPI.ts).
 *
 **/
export class AnnotationEditing {

    stompClient: Client;
    webSocket: WebSocket;
    subscription: StompSubscription;

    /**
     * Constructor: creates the Annotation Experience API.
     * @param aProjectId: ID of the project, required in order to subscribe to the correct channels.
     * @param aDocumentId: ID of the document, required in order to subscribe to the correct channels.
     * @param aAnnotatorName: String representation of the annotatorName, required in order to subscribe to the correct channels.
     * @param aUrl: The URL required by Websocket and the stomp broker in order to establish a connection
     * @NOTE: the connect() method will automatically be performed, there is no need to create a Websocket connection
    * manually.
*/
    constructor(aUrl: string)
    {
        this.connect(aUrl)
    }


    /**
     * Creates the Websocket connection with the stomp broker.
     * @NOTE: When a connection is established, onConnect() will be called automatically so
     * there is no need to call it. Also, all subscriptions will be handled automatically.
     */
    connect(aUrl: string)
    {
        if (this.stompClient) {
            throw "Already connected"; 
        }

        this.stompClient = Stomp.over(() => this.webSocket = new WebSocket(aUrl));
        this.stompClient.onConnect = () => { 
            this.stompClient.subscribe("/user/queue/errors",  this.handleProtocolError);
        }
        this.stompClient.onStompError = this.handleBrokerError;
        this.stompClient.reconnectDelay = 5000;
        this.stompClient.activate();
    }

    disconnect()
    {
        this.stompClient.deactivate();
        this.webSocket.close();
    }

    handleBrokerError(frame) {
        console.log('Broker reported error: ' + frame.headers['message']);
        console.log('Additional details: ' + frame.body);
    }

    handleProtocolError(msg) {
        console.log(msg);
    }

    subscribeToViewport(aViewportTopic: string, messageCallbackType)
    {
        this.stompClient.subscribe(aViewportTopic, callback);
    }

    unsubscribeFromViewport() 
    {

    }

    requestDocument(aAnnotatorName: string, aProjectId: number, aViewport: Viewport[])
    {
        this.stompClient.publish({
            destination: "/app/document_request", body: JSON.stringify(
                new DocumentRequest(aAnnotatorName, aProjectId, aViewport))
        });
    }

    requestCreateArc(aAnnotatorName: string, aProjectId: number, aDocumentId: number, aSourceId: number, aTargetId: number, aLayer: number)
    {
        this.stompClient.publish({
            destination: "/app/arc_create",
            body: JSON.stringify(new CreateArcRequest(aAnnotatorName, aProjectId, aDocumentId, aSourceId, aTargetId, aLayer))
        });
    }

    requestCreateSpan(aAnnotatorName: string, aProjectId: number, aDocumentId: number, aBegin: number, aEnd: number, aLayer: number)
    {
        this.stompClient.publish({
            destination: "/app/span_create",
            body: JSON.stringify(new CreateSpanRequest(aAnnotatorName, aProjectId, aDocumentId, aBegin, aEnd, aLayer))
        });
    }

    requestDeleteAnnotation(aAnnotatorName: string, aProjectId: number, aDocumentId: number, aAnnotationId: number, aLayer: number)
    {
        this.stompClient.publish({
            destination: "/app/annotation_delete", body: JSON.stringify(
                new DeleteAnnotationRequest(aAnnotatorName, aProjectId, aDocumentId, aAnnotationId, aLayer))
        });
    }

    requestUpdateFeature(aAnnotatorName: string, aProjectId: number, aDocumentId: number, aAnnotationId: number, aLayerId: number, aFeature: FeatureX, aValue: any)
    {
        this.stompClient.publish({
            destination: "/app/features_update", body: JSON.stringify(
                new UpdateFeatureRequest(aAnnotatorName, aProjectId, aDocumentId, aAnnotationId, aLayerId, aFeature, aValue))
        });
    }


    onDocument(aMessage: DocumentMessage)
    {
        console.log('RECEIVED DOCUMENT' + aMessage);
        this.notify();
        this.annotationEditor.viewport = aMessage.viewport;
    }

    onSpanCreate(aMessage: SpanCreatedMessage)
    {
        console.log('RECEIVED SPAN CREATE' + aMessage);
        this.notify();
        this.annotationEditor.viewport[0].spans.push(new Span(aMessage.spanId, aMessage.begin, aMessage.end, aMessage.layerId, aMessage.features, aMessage.color))
    }

    onArcCreate(aMessage: ArcCreatedMessage)
    {
        console.log('RECEIVED ARC CREATE' + aMessage);
        this.notify();
        this.annotationEditor.viewport[0].arcs.push(new Arc(aMessage.arcId, aMessage.sourceId, aMessage.targetId, aMessage.layerId, aMessage.features, aMessage.color))
    }

    onAnnotationDelete(aMessage: DeleteAnnotationMessage)
    {
        console.log('RECEIVED DELETE ANNOTATION' + aMessage);
        this.notify();
        let annotationID = aMessage.annotationId;
    }

    onFeaturesUpdate(aMessage: UpdateFeatureMessage) {
        console.log('RECEIVED UPDATE ANNOTATION' + aMessage);
        this.notify();

        let annotationID = aMessage.annotationId;
        let feature = aMessage.feature
        let newValue = aMessage.value;
    }

    onError(aMessage: AdviceMessage)
    {
        console.log('RECEIVED ERROR MESSAGE' + aMessage);
        this.notify();
        console.log(aMessage);
    }
}


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
import {Client, Stomp} from '@stomp/stompjs';
import {AnnotationExperienceAPI} from "./AnnotationExperienceAPI"
import {Span} from "./model/Span";
import {AdviceMessage} from "./messages/response/AdviceMessage";
import {FeatureX} from "./model/FeatureX";
import {DocumentMessage} from "./messages/response/DocumentMessage";
import {DocumentRequest} from "./messages/request/DocumentRequest";
import {UpdateFeatureMessage} from "./messages/response/UpdateFeatureMessage";
import {SpanCreatedMessage} from "./messages/response/create/SpanCreatedMessage";
import {DeleteAnnotationMessage} from "./messages/response/DeleteAnnotationMessage";
import {ArcCreatedMessage} from "./messages/response/create/ArcCreatedMessage";
import {CreateArcRequest} from "./messages/request/create/CreateArcRequest";
import {CreateSpanRequest} from "./messages/request/create/CreateSpanRequest";
import {Viewport} from "./model/Viewport";
import {DeleteAnnotationRequest} from "./messages/request/DeleteAnnotationRequest";
import {UpdateFeatureRequest} from "./messages/request/UpdateFeatureRequest";
import {Arc} from "./model/Arc";

/**
 * Implementation of the Interface AnnotationExperienceAPI within that package.
 *
 * For further details @see interface class (AnnotationExperienceAPI.ts).
 *
 **/
export class AnnotationExperienceAPIImpl implements AnnotationExperienceAPI {

    //Websocket and stomp broker
    private stompClient: Client;

    /**
     * Constructor: creates the Annotation Experience API.
     * @param aProjectId: ID of the project, required in order to subscribe to the correct channels.
     * @param aDocumentId: ID of the document, required in order to subscribe to the correct channels.
     * @param aAnnotatorName: String representation of the annotatorName, required in order to subscribe to the correct channels.
     * @param aUrl: The URL required by Websocket and the stomp broker in order to establish a connection
     *
     * @NOTE: the connect() method will automatically be performed, there is no need to create a Websocket connection
     * manually.
     */
    constructor(aProjectId: number, aDocumentId: number, aAnnotatorName: string, aUrl: string)
    {
        this.connect(aProjectId, aDocumentId, aAnnotatorName, aUrl)
    }


    /**
     * Creates the Websocket connection with the stomp broker.
     * @NOTE: When a connection is established, onConnect() will be called automatically so
     * there is no need to call it. Also, all subscriptions will be handled automatically.
     */
    connect(aProjectId: number, aDocumentId: number, aAnnotatorName: string, aUrl: string)
    {
        this.stompClient = Stomp.over(function () {
            return new WebSocket(aUrl);
        });

        const that = this;

        // ------ STOMP CONNECTION ESTABLISHED ----//
        this.stompClient.onConnect = function () {
            that.onConnect(aAnnotatorName, aProjectId, aDocumentId)
        }

        // --------- STOMP ERROR HANDLING -------- //
        this.stompClient.onStompError = function (frame) {
            console.log('Broker reported error: ' + frame.headers['message']);
            console.log('Additional details: ' + frame.body);
        };
    }

    /**
     * Method called automatically after a websocket connection has been established.
     * @param aAnnotatorName: String representation of the annotatorName, required in order to subscribe to the correct channels.
     * @param aProjectId: ID of the project, required in order to subscribe to the correct channels.
     * @param aDocumentId: ID of the document, required in order to subscribe to the correct channels.
     */
    onConnect(aAnnotatorName: string, aProjectId: number, aDocumentId: number) {
        let that = this;

        // ------ DEFINE STANDARD SUBSCRIPTION CHANNELS WITH ACTIONS ------ //
        this.stompClient.subscribe("/queue/document/" + aAnnotatorName, function (msg) {
            that.onDocument(Object.assign(new DocumentMessage(), JSON.parse(msg.body)));
        }, {id: "document_request"});

        this.stompClient.subscribe("/queue/error_message/" + aAnnotatorName, function (msg) {
            that.onError(Object.assign(new AdviceMessage(), JSON.parse(msg.body)));
        }, {id: "error_message"});

        this.stompClient.subscribe("/topic/features_update/" +
            aProjectId + "/" +
            aDocumentId, function (msg) {
            that.onFeaturesUpdate(Object.assign(new UpdateFeatureMessage(), JSON.parse(msg.body)));
        }, {id: "span_update"});

        this.stompClient.subscribe("/topic/span_create/" +
            aProjectId + "/" +
            aDocumentId, function (msg) {
            that.onSpanCreate(Object.assign(new SpanCreatedMessage(), JSON.parse(msg.body)));
        }, {id: "span_create"});

        this.stompClient.subscribe("/topic/arc_create/" +
            aProjectId + "/" +
            aDocumentId, function (msg) {
            that.onArcCreate(Object.assign(new ArcCreatedMessage(), JSON.parse(msg.body)));
        }, {id: "relation_create"});

        this.stompClient.subscribe("/topic/annotation_delete/" +
            aProjectId + "/" +
            aDocumentId, function (msg) {
            that.onAnnotationDelete(Object.assign(new DeleteAnnotationMessage(), JSON.parse(msg.body)));
        }, {id: "span_delete"});

        this.stompClient.activate();
    }

    unsubscribe(aViewport: Viewport)
    {
        this.stompClient.unsubscribe(aChannel);
    }

    disconnect()
    {
        this.stompClient.deactivate();
    }

    requestDocument(aAnnotatorName: string, aProjectId: number, aDocumentId: number, aViewport: Viewport[])
    {
        this.stompClient.publish({
            destination: "/app/document_request", body: JSON.stringify(
                new DocumentRequest(aAnnotatorName, aProjectId, aDocumentId, aViewport))
        });
    }

    requestCreateArc(aAnnotatorName: string, aProjectId: number, aDocumentId: number, aSourceId: string, aTargetId: string, aLayer: number)
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
        /*
        this.viewport = aMessage.viewport;
        this.viewport.documentText = this.viewport.documentText.split("\n").join("");
        this.documentID = aMessage.documentId;
        this.spans = aMessage.spans;
        this.arcs = aMessage.arcs;

         */
    }

    onSpanCreate(aMessage: SpanCreatedMessage)
    {

        console.log('RECEIVED SPAN CREATE' + aMessage);
        let span = new Span(aMessage.spanId, aMessage.begin, aMessage.end, aMessage.layerId, aMessage.features, aMessage.color)

    }

    onArcCreate(aMessage: ArcCreatedMessage)
    {
        console.log('RECEIVED ARC CREATE' + aMessage);
        let arc = new Arc(aMessage.arcId, aMessage.sourceId, aMessage.targetId, aMessage.layerId, aMessage.features, aMessage.color)
    }

    onAnnotationDelete(aMessage: DeleteAnnotationMessage)
    {
        console.log('RECEIVED DELETE ANNOTATION' + aMessage);
        /*
        this.spans.forEach((item, index) => {
            if (item.id === aMessage.annotationId) {
                this.spans.splice(index, 1);
            }
        });
        this.arcs.forEach((item, index) => {
            if (item.id === aMessage.annotationId) {
                this.arcs.splice(index, 1);
            }
        });
         */
    }

    onFeaturesUpdate(aMessage: UpdateFeatureMessage) {
        console.log('RECEIVED UPDATE ANNOTATION' + aMessage);
        /*
        let arc = this.arcs.findIndex(r => r.id == aMessage.annotationId);
        if (arc != null) {
            this.arcs[arc].features = aMessage.features;
            return;
        } else {
            let span = this.spans.findIndex(r => r.id == aMessage.annotationId);
            this.arcs[arc].features = aMessage.features;
        }

         */
    }

    onError(aMessage: AdviceMessage)
    {
        console.log(aMessage);
    }
}


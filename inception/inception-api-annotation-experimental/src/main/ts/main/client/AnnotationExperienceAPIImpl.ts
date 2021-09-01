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
import {Arc} from "./model/Arc";
import {DocumentMessage} from "./messages/response/DocumentMessage";
import {DocumentRequest} from "./messages/request/DocumentRequest";
import {UpdateFeaturesMessage} from "./messages/response/UpdateFeaturesMessage";
import {SpanCreatedMessage} from "./messages/response/create/SpanCreatedMessage";
import {DeleteAnnotationMessage} from "./messages/response/DeleteAnnotationMessage";
import {ArcCreatedMessage} from "./messages/response/create/ArcCreatedMessage";
import {CreateArcRequest} from "./messages/request/create/CreateArcRequest";
import {CreateSpanRequest} from "./messages/request/create/CreateSpanRequest";
import {Viewport} from "./model/Viewport";
import {DeleteAnnotationRequest} from "./messages/request/DeleteAnnotationRequest";
import {UpdateFeaturesRequest} from "./messages/request/UpdateFeaturesRequest";

export class AnnotationExperienceAPIImpl implements AnnotationExperienceAPI {

    //Websocket and stomp broker
    private stompClient: Client;

    constructor(aClientName: string, aProjectId: number, aDocumentId: number)
    {
        this.connect(aClientName, aProjectId, aDocumentId)
    }


    //CREATE WEBSOCKET CONNECTION
    connect(aClientName: string, aProjectId: number, aDocumentId: number)
    {
        this.stompClient = Stomp.over(function () {
            return new WebSocket(localStorage.getItem("url"));
        });

        const that = this;

        this.stompClient.onConnect = function (frame) {
            that.onConnect(aClientName, aProjectId, aDocumentId)
        }


        // ------ STOMP ERROR HANDLING ------ //

        this.stompClient.onStompError = function (frame) {
            console.log('Broker reported error: ' + frame.headers['message']);
            console.log('Additional details: ' + frame.body);
        };

        this.stompClient.activate();
    }

    onConnect(aClientName: string, aProjectId: number, aDocumentId: number) {
        let that = this;

        // ------ DEFINE STANDARD SUBSCRIPTION CHANNELS WITH ACTIONS ------ //

        this.stompClient.subscribe("/queue/document/" + aClientName, function (msg) {
            that.onDocument(Object.assign(new DocumentMessage(), JSON.parse(msg.body)));
        }, {id: "document_request"});

        this.stompClient.subscribe("/queue/error_message/" + aClientName, function (msg) {
            that.onError(Object.assign(new AdviceMessage(), JSON.parse(msg.body)));
        }, {id: "error_message"});

        this.stompClient.subscribe("/topic/features_update/" +
            aProjectId + "/" +
            aDocumentId, function (msg) {
            that.onFeaturesUpdate(Object.assign(new UpdateFeaturesMessage(), JSON.parse(msg.body)));
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
    }

    unsubscribe(aChannel: string)
    {
        this.stompClient.unsubscribe(aChannel);
    }

    disconnect()
    {
        this.stompClient.deactivate();
    }

    onDocument(aMessage)
    {
        this.viewport = aMessage.viewport;
        this.viewport.documentText = this.viewport.documentText.split("\n").join("");
        this.documentID = aMessage.documentId;
        this.spans = aMessage.spans;
        this.arcs = aMessage.arcs;
    }

    onSpanCreate(aMessage)
    {
        let span = new Span(aMessage.spanId, aMessage.coveredText, aMessage.begin, aMessage.end, aMessage.type, aMessage.features, aMessage.color)
        this.spans.push(span)
    }

    onError(aMessage)
    {
        console.log(aMessage);
    }

    onArcCreate(aMessage)
    {
        let relation = new Arc(aMessage.arcId, aMessage.sourceId, aMessage.targetId, aMessage.sourceCoveredText, aMessage.targetCoveredText, aMessage.color, aMessage.type, aMessage.features)
        this.arcs.push(relation)
    }

    onAnnotationDelete(aMessage: DeleteAnnotationMessage)
    {
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
    }

    onFeaturesUpdate(aMessage: UpdateFeaturesMessage) {
        console.log('RECEIVED UPDATE ANNOTATION');
        let arc = this.arcs.findIndex(r => r.id == aMessage.annotationId);
        if (arc != null) {
            this.arcs[arc].features = aMessage.features;
            return;
        } else {
            let span = this.spans.findIndex(r => r.id == aMessage.annotationId);
            this.arcs[arc].features = aMessage.features;
        }
    }

    requestCreateArc(aAnnotatorName: string, aProjectId: number, aDocumentId: number, aSourceId: string, aTargetId: string, aLayer: string)
    {
        this.stompClient.publish({
            destination: "/app/arc_create",
            body: JSON.stringify(new CreateArcRequest(aAnnotatorName, aProjectId, aDocumentId, aSourceId, aTargetId, aLayer))
        });
    }

    requestCreateSpan(aAnnotatorName: string, aProjectId: number, aDocumentId: number, aBegin: number, aEnd: number, aLayer: string)
    {
        console.log("SENDING: " + aBegin + " _ " + aEnd)
        this.stompClient.publish({
            destination: "/app/span_create",
            body: JSON.stringify(new CreateSpanRequest(aAnnotatorName, aProjectId, aDocumentId, aBegin, aEnd, aLayer))
        });
    }


    requestDocument(aAnnotatorName: string, aProjectId: number, aDocumentId: number, aViewport: Viewport)
    {
        this.stompClient.publish({
            destination: "/app/document_request", body: JSON.stringify(
                new DocumentRequest(aAnnotatorName, aProjectId, aDocumentId, aViewport))
        });
    }

    requestDeleteAnnotation(aAnnotatorName: string, aProjectId: number, aDocumentId: number, aAnnotationId: number, aLayer: string)
    {
        this.stompClient.publish({
            destination: "/app/annotation_delete", body: JSON.stringify(
                new DeleteAnnotationRequest(aAnnotatorName, aProjectId, aDocumentId, aAnnotationId, aLayer))
        });
    }

    requestUpdateFeatures(aAnnotatorName: string, aProjectId: number, aDocumentId: number, aAnnotationId: number, aNewFeature: FeatureX[])
    {
        this.stompClient.publish({
            destination: "/app/features_update", body: JSON.stringify(
                new UpdateFeaturesRequest(aAnnotatorName, aProjectId, aDocumentId,aAnnotationId, aNewFeature))
        });
    }
}


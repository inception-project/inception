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
import {ErrorMessage} from "./messages/response/ErrorMessage";
import {SelectSpanRequest} from "./messages/request/span/SelectSpanRequest";
import {UpdateSpanRequest} from "./messages/request/span/UpdateSpanRequest";
import {CreateSpanRequest} from "./messages/request/span/CreateSpanRequest";
import {DeleteSpanRequest} from "./messages/request/span/DeleteSpanRequest";
import {SelectSpanResponse} from "./messages/response/span/SelectSpanResponse";
import {Viewport} from "./model/Viewport";
import {SelectArcResponse} from "./messages/response/arc/SelectArcResponse";
import {SelectArcRequest} from "./messages/request/arc/SelectArcRequest";
import {UpdateArcRequest} from "./messages/request/arc/UpdateArcRequest";
import {CreateArcRequest} from "./messages/request/arc/CreateArcRequest";
import {FeatureX} from "./model/FeatureX";
import {Arc} from "./model/Arc";
import {DocumentResponse} from "./messages/response/DocumentResponse";
import {ViewportResponse} from "./messages/response/ViewportResponse";
import {UpdateSpanMessage} from "./messages/response/span/UpdateSpanMessage";
import {CreateSpanMessage} from "./messages/response/span/CreateSpanMessage";
import {DeleteSpanMessage} from "./messages/response/span/DeleteSpanMessage";
import {UpdateArcMessage} from "./messages/response/arc/UpdateArcMessage";
import {CreateArcMessage} from "./messages/response/arc/CreateArcMessage";
import {DeleteArcMessage} from "./messages/response/arc/DeleteArcMessage";
import {DocumentRequest} from "./messages/request/DocumentRequest";
import {ViewportRequest} from "./messages/request/ViewportRequest";

export class AnnotationExperienceAPIImpl implements AnnotationExperienceAPI {

    //Websocket and stomp broker
    private stompClient: Client;

    //States to remember by client
    private clientName: string;
    private projectID: number;
    private documentID: number;

    //Text and annotations
    private text: string[];
    private spans: Span[];
    private selectedSpan: Span;

    private arcs: Arc[];
    private selectedArc: Arc;

    //Viewport
    private viewport: Viewport;

    constructor() {
        this.connect();
    }


    //CREATE WEBSOCKET CONNECTION
    connect() {

        //TODO find better solution
        this.stompClient = Stomp.over(function () {
            return new WebSocket(localStorage.getItem("url"));
        });

        this.viewport = new Viewport([[0, 79], [80, 140], [141, 189]], null);

        const that = this;

        this.stompClient.onConnect = function (frame) {
            that.onConnect(frame)
        }


        // ------ STOMP ERROR HANDLING ------ //

        this.stompClient.onStompError = function (frame) {
            console.log('Broker reported error: ' + frame.headers['message']);
            console.log('Additional details: ' + frame.body);
        };

        this.stompClient.activate();
    }

    onConnect(frame) {
        let that = this;
        //Receive username from inital message exchange header
        const header = frame.headers;
        let data: keyof typeof header;
        for (data in header) {
            that.clientName = header[data];
            break;
        }

        //Receive project and document from URL
        this.projectID = Number(document.location.href.split("/")[5]);
        this.documentID = Number(document.location.href.split("=")[1].split("&")[0]);

        // ------ DEFINE STANDARD SUBSCRIPTION CHANNELS WITH ACTIONS ------ //

        this.stompClient.subscribe("/queue/new_document_for_client/" + that.clientName, function (msg) {
            that.onNewDocument(Object.assign(new DocumentResponse(), JSON.parse(msg.body)));
        }, {id: "new_document"});

        this.stompClient.subscribe("/queue/new_viewport_for_client/" + that.clientName, function (msg) {
            that.onNewViewport(Object.assign(new ViewportResponse(), JSON.parse(msg.body)));
        }, {id: "new_viewport"});

        this.stompClient.subscribe("/queue/selected_annotation_for_client/" + that.clientName, function (msg) {
            that.onSpanSelect(Object.assign(new SelectSpanResponse(), JSON.parse(msg.body)));
        }, {id: "selected_annotation"});

        this.stompClient.subscribe("/queue/selected_relation_for_client/" + that.clientName, function (msg) {
            that.onArcSelect(Object.assign(new SelectArcResponse(), JSON.parse(msg.body)));
        }, {id: "selected_relation"});

        this.stompClient.subscribe("/queue/error_for_client/" + that.clientName, function (msg) {
            that.onError(Object.assign(new ErrorMessage(), JSON.parse(msg.body)));
        }, {id: "error_message"});

    }

    multipleSubscriptions() {
        const that = this;
        for (let i = 0; i < this.viewport.viewport.length; i++) {
            for (let j = this.viewport.viewport[i][0]; j <= this.viewport.viewport[i][1]; j++) {

                this.stompClient.subscribe("/topic/span_update_for_clients/" +
                    this.projectID + "/" +
                    this.documentID + "/" +
                    j, function (msg) {
                    that.onSpanUpdate(Object.assign(new UpdateSpanMessage(), JSON.parse(msg.body)));
                }, {id: "span_update_" + j});

                this.stompClient.subscribe("/topic/span_create_for_clients/" +
                    this.projectID + "/" +
                    this.documentID + "/" +
                    j, function (msg) {
                    that.onSpanCreate(Object.assign(new CreateSpanMessage(), JSON.parse(msg.body)));
                }, {id: "span_create_" + j});

                this.stompClient.subscribe("/topic/span_delete_for_clients/" +
                    this.projectID + "/" +
                    this.documentID + "/" +
                    j, function (msg) {
                    that.onSpanDelete(Object.assign(new DeleteSpanMessage(), JSON.parse(msg.body)));
                }, {id: "span_delete_" + j});

                this.stompClient.subscribe("/topic/relation_update_for_clients/" +
                    this.projectID + "/" +
                    this.documentID + "/" +
                    j, function (msg) {
                    that.onArcUpdate(Object.assign(new UpdateArcMessage(), JSON.parse(msg.body)));
                }, {id: "relation_update_" + j});

                this.stompClient.subscribe("/topic/relation_create_for_clients/" +
                    this.projectID + "/" +
                    this.documentID + "/" +
                    j, function (msg) {
                    that.onArcCreate(Object.assign(new CreateArcMessage(), JSON.parse(msg.body)));
                }, {id: "relation_create_" + j});

                this.stompClient.subscribe("/topic/relation_delete_for_clients/" +
                    this.projectID + "/" +
                    this.documentID + "/" +
                    j, function (msg) {
                    that.onArcDelete(Object.assign(new DeleteArcMessage(), JSON.parse(msg.body)));
                }, {id: "relation_delete_" + j});
            }
        }
    }

    unsubscribe(aChannel: string)
    {
        this.stompClient.unsubscribe(aChannel);
    }

    disconnect()
    {
        this.stompClient.deactivate();
    }

    onNewDocument(aMessage)
    {
        this.documentID = aMessage.documentId;
        this.text = aMessage.viewportText;
        this.spans = aMessage.spans;
        this.arcs = aMessage.arcs;

        this.multipleSubscriptions()
    }

    onNewViewport(aMessage)
    {
        this.text = aMessage.viewportText;
        this.spans = aMessage.spans;
        this.arcs = aMessage.arcs;

        this.multipleSubscriptions();
    }

    onSpanDelete(aMessage)
    {
        this.spans.forEach((item, index) => {
            if (item.id.toString() === aMessage.spanId.toString()) {
                this.spans.splice(index, 1);
            }
        });
    }

    onSpanCreate(aMessage)
    {
        let span = new Span(aMessage.spanId, aMessage.coveredText, aMessage.begin, aMessage.end, aMessage.type, aMessage.features, aMessage.color)
        this.spans.push(span)
    }

    onSpanSelect(aMessage)
    {
        this.selectedSpan = new Span(aMessage.spanId, aMessage.coveredText, aMessage.begin, aMessage.end, aMessage.type, aMessage.feature, aMessage.color)
    }

    onSpanUpdate(aMessage)
    {
        console.log('RECEIVED ANNOTATION UPDATE')
        this.spans[this.spans.findIndex(s => s.id == aMessage.id)].features = aMessage.feature;
    }

    onError(aMessage)
    {
        console.log('RECEIVED ERROR MESSAGE');
        console.log(aMessage);
    }

    onArcCreate(aMessage)
    {
        let relation = new Arc(aMessage.arcId, aMessage.sourceId, aMessage.targetId, aMessage.sourceCoveredText, aMessage.targetCoveredText, aMessage.color, aMessage.type, aMessage.features)
        this.arcs.push(relation)
    }

    onArcDelete(aMessage)
    {
        this.arcs.forEach((item, index) => {
            if (item.id === aMessage.arcId) {
                this.arcs.splice(index, 1);
            }
        });
    }

    onArcSelect(aMessage)
    {
        this.selectedArc = new Arc(aMessage.arcId, aMessage.sourceId, aMessage.targetId, aMessage.sourceCoveredText, aMessage.targetCoveredText, aMessage.color, aMessage.type, aMessage.features)
    }

    onArcUpdate(aMessage)
    {
        console.log('RECEIVED UPDATE ARC');
        let arc = this.arcs.findIndex(r => r.id == aMessage.arcId);

        this.arcs[arc].features = aMessage.newFeatures;
    }

    onDocument(aMessage) {
    }

    onViewport(aMessage) {
    }

    requestCreateArc(aClientName: string, aUserName: string, aProjectId: number, aDocumentId: number, aSourceId: string, aTargetId: string, aLayer: string)
    {
        this.stompClient.publish({
            destination: "/app/new_relation_from_client",
            body: JSON.stringify(new CreateArcRequest(aClientName, aUserName, aProjectId, aDocumentId, aSourceId, aTargetId, aLayer))
        });
    }

    requestCreateSpan(aClientName: string, aUserName: string, aProjectId: number, aDocumentId: number, aBegin: number, aEnd: number, aLayer: string)
    {
        this.stompClient.publish({
            destination: "/app/new_annotation_from_client",
            body: JSON.stringify(new CreateSpanRequest(aClientName, aUserName, aProjectId, aDocumentId, aBegin, aEnd, aLayer))
        });
    }

    requestDeleteArc(aClientName: string, aUserName: string, aProjectId: number, aDocumentId: number, aArcId: string, aLayer: string) {
    }

    requestDeleteSpan(aClientName: string, aUserName: string, aProjectId: number, aDocumentId: number, aSpanId: string, aLayer: string)
    {
        this.stompClient.publish({
            destination: "/app/delete_annotation_from_client",
            body: JSON.stringify(new DeleteSpanRequest(aClientName, aUserName, aProjectId, aDocumentId, aSpanId, aLayer))
        });
    }

    requestDocument(aClientName: string, aUserName: string, aProjectId: number, aDocumentId: number, aViewport: Viewport)
    {
        const that = this;
        this.viewport = aViewport;
        that.stompClient.publish({
            destination: "/app/new_document_from_client", body: JSON.stringify(
                new DocumentRequest(aClientName, aUserName, aProjectId, aDocumentId, aViewport))
        });
    }

    requestSelectArc(aClientName: string, aUserName: string, aProjectId: number, aDocumentId: number, aArcId: string)
    {
        this.stompClient.publish({
            destination: "/app/select_relation_from_client",
            body: JSON.stringify(new SelectArcRequest(aClientName, aUserName, aProjectId, aDocumentId, aArcId))
        });
    }

    requestSelectSpan(aClientName: string, aUserName: string, aProjectId: number, aDocumentId: number, aSpanId: string)
    {
        this.stompClient.publish({
            destination: "/app/select_annotation_from_client",
            body: JSON.stringify(new SelectSpanRequest(aClientName, aUserName, aProjectId, aDocumentId, aSpanId))
        });
    }

    requestUpdateArc(aClientName: string, aUserName: string, aProjectId: number, aDocumentId: number, aArcId: string, aNewFeature: FeatureX[])
    {
        this.stompClient.publish({
            destination: "/app/update_relation_from_client",
            body: JSON.stringify(new UpdateArcRequest(aClientName, aUserName, aProjectId, aDocumentId, aArcId, aNewFeature))
        });
    }

    requestUpdateSpan(aClientName: string, aUserName: string, aProjectId: number, aDocumentId: number, aSpanId: string, aNewFeature: FeatureX[])
    {
        this.stompClient.publish({
            destination: "/app/update_annotation_from_client",
            body: JSON.stringify(new UpdateSpanRequest(aClientName, aUserName, aProjectId, aDocumentId, aSpanId, aNewFeature))
        });
    }

    requestViewport(aClientName: string, aUserName: string, aProjectId: number, aDocumentId: number, aViewport: Viewport)
    {
        this.viewport = aViewport;
        this.stompClient.publish({
            destination: "/app/new_viewport_from_client",
            body: JSON.stringify(new ViewportRequest(aClientName, aUserName, aProjectId, aDocumentId, aViewport))
        });
    }
}


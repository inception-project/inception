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
import {UpdateSpanMessage} from "./messages/response/span/UpdateSpanMessage";
import {CreateSpanMessage} from "./messages/response/span/CreateSpanMessage";
import {DeleteSpanMessage} from "./messages/response/span/DeleteSpanMessage";
import {UpdateArcMessage} from "./messages/response/arc/UpdateArcMessage";
import {CreateArcMessage} from "./messages/response/arc/CreateArcMessage";
import {DeleteArcMessage} from "./messages/response/arc/DeleteArcMessage";
import {DocumentRequest} from "./messages/request/DocumentRequest";
import {DeleteArcRequest} from "./messages/request/arc/DeleteArcRequest";

export class AnnotationExperienceAPIImpl implements AnnotationExperienceAPI {

    //Websocket and stomp broker
    private stompClient: Client;

    //States to remember by client
    clientName: string;
    projectID: number;
    documentID: number;

    //Text and annotations
    spans: Span[];
    selectedSpan: Span;

    arcs: Arc[];
    selectedArc: Arc;

    //Viewport
    viewport: Viewport;

    constructor() {
        this.connect();
    }


    //CREATE WEBSOCKET CONNECTION
    connect() {

        //TODO find better solution
        this.stompClient = Stomp.over(function () {
            return new WebSocket(localStorage.getItem("url"));
        });

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

        this.viewport = new Viewport(null, null, null);

        // ------ DEFINE STANDARD SUBSCRIPTION CHANNELS WITH ACTIONS ------ //

        this.stompClient.subscribe("/queue/document/" + that.clientName, function (msg) {
            that.onDocument(Object.assign(new DocumentResponse(), JSON.parse(msg.body)));
        }, {id: "document_request"});

        this.stompClient.subscribe("/queue/select_span/" + that.clientName, function (msg) {
            that.onSpanSelect(Object.assign(new SelectSpanResponse(), JSON.parse(msg.body)));
        }, {id: "selected_span"});

        this.stompClient.subscribe("/queue/select_arc/" + that.clientName, function (msg) {
            that.onArcSelect(Object.assign(new SelectArcResponse(), JSON.parse(msg.body)));
        }, {id: "selected_arc"});

        this.stompClient.subscribe("/queue/error_message/" + that.clientName, function (msg) {
            that.onError(Object.assign(new ErrorMessage(), JSON.parse(msg.body)));
        }, {id: "error_message"});

        this.stompClient.subscribe("/topic/span_update/" +
            this.projectID + "/" +
            this.documentID, function (msg) {
            that.onSpanUpdate(Object.assign(new UpdateSpanMessage(), JSON.parse(msg.body)));
        }, {id: "span_update"});

        this.stompClient.subscribe("/topic/span_create/" +
            this.projectID + "/" +
            this.documentID, function (msg) {
            that.onSpanCreate(Object.assign(new CreateSpanMessage(), JSON.parse(msg.body)));
        }, {id: "span_create"});

        this.stompClient.subscribe("/topic/span_delete/" +
            this.projectID + "/" +
            this.documentID , function (msg) {
            that.onSpanDelete(Object.assign(new DeleteSpanMessage(), JSON.parse(msg.body)));
        }, {id: "span_delete"});

        this.stompClient.subscribe("/topic/arc_update/" +
            this.projectID + "/" +
            this.documentID, function (msg) {
            that.onArcUpdate(Object.assign(new UpdateArcMessage(), JSON.parse(msg.body)));
        }, {id: "relation_update"});

        this.stompClient.subscribe("/topic/arc_create/" +
            this.projectID + "/" +
            this.documentID + "/", function (msg) {
            that.onArcCreate(Object.assign(new CreateArcMessage(), JSON.parse(msg.body)));
        }, {id: "relation_create"});

        this.stompClient.subscribe("/topic/arc_delete/" +
            this.projectID + "/" +
            this.documentID, function (msg) {
            that.onArcDelete(Object.assign(new DeleteArcMessage(), JSON.parse(msg.body)));
        }, {id: "relation_delete"});

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
        this.spans = aMessage.spans;
        this.arcs = aMessage.arcs;
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
        this.spans[this.spans.findIndex(s => s.id == aMessage.id)].features = aMessage.feature;
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

    requestCreateArc(aClientName: string, aProjectId: number, aDocumentId: number, aSourceId: string, aTargetId: string, aLayer: string)
    {
        this.stompClient.publish({
            destination: "/app/arc_create",
            body: JSON.stringify(new CreateArcRequest(aClientName, aProjectId, aDocumentId, aSourceId, aTargetId, aLayer))
        });
    }

    requestCreateSpan(aClientName: string, aProjectId: number, aDocumentId: number, aBegin: number, aEnd: number, aLayer: string)
    {
        this.stompClient.publish({
            destination: "/app/span_create",
            body: JSON.stringify(new CreateSpanRequest(aClientName, aProjectId, aDocumentId, aBegin, aEnd, aLayer))
        });
    }

    requestDeleteArc(aClientName: string, aProjectId: number, aDocumentId: number, aArcId: string, aLayer: string)
    {
        this.stompClient.publish({
        destination: "/app/arc_delete",
        body: JSON.stringify(new DeleteArcRequest(aClientName, aProjectId, aDocumentId, aArcId, aLayer))
        });
    }

    requestDeleteSpan(aClientName: string, aProjectId: number, aDocumentId: number, aSpanId: string, aLayer: string)
    {
        this.stompClient.publish({
            destination: "/app/span_delete",
            body: JSON.stringify(new DeleteSpanRequest(aClientName, aProjectId, aDocumentId, aSpanId, aLayer))
        });
    }

    requestDocument(aClientName: string, aProjectId: number, aDocumentId: number)
    {
        const that = this;
        that.stompClient.publish({
            destination: "/app/document_request", body: JSON.stringify(
                new DocumentRequest(aClientName, aProjectId, aDocumentId, this.viewport))
        });
    }

    requestSelectArc(aClientName: string, aProjectId: number, aDocumentId: number, aArcId: string)
    {
        this.stompClient.publish({
            destination: "/app/select_arc",
            body: JSON.stringify(new SelectArcRequest(aClientName, aProjectId, aDocumentId, aArcId))
        });
    }

    requestSelectSpan(aClientName: string, aProjectId: number, aDocumentId: number, aSpanId: string)
    {
        this.stompClient.publish({
            destination: "/app/select_span",
            body: JSON.stringify(new SelectSpanRequest(aClientName, aProjectId, aDocumentId, aSpanId))
        });
    }

    requestUpdateArc(aClientName: string, aProjectId: number, aDocumentId: number, aArcId: string, aNewFeature: FeatureX[])
    {
        this.stompClient.publish({
            destination: "/app/update_arc",
            body: JSON.stringify(new UpdateArcRequest(aClientName, aProjectId, aDocumentId, aArcId, aNewFeature))
        });
    }

    requestUpdateSpan(aClientName: string, aProjectId: number, aDocumentId: number, aSpanId: string, aNewFeature: FeatureX[])
    {
        this.stompClient.publish({
            destination: "/app/update_span",
            body: JSON.stringify(new UpdateSpanRequest(aClientName, aProjectId, aDocumentId, aSpanId, aNewFeature))
        });
    }
}


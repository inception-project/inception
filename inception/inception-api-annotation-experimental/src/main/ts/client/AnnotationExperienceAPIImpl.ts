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
import {NewDocumentResponse} from "./messages/response/NewDocumentResponse";
import {NewViewportResponse} from "./messages/response/NewViewportResponse";
import {ErrorMessage} from "./messages/response/ErrorMessage";
import {NewDocumentRequest} from "./messages/request/NewDocumentRequest";
import {NewViewportRequest} from "./messages/request/NewViewportRequest";
import {SelectSpanRequest} from "./messages/request/span/SelectSpanRequest";
import {UpdateSpanRequest} from "./messages/request/span/UpdateSpanRequest";
import {CreateSpanRequest} from "./messages/request/span/CreateSpanRequest";
import {DeleteSpanRequest} from "./messages/request/span/DeleteSpanRequest";
import {SelectSpanResponse} from "./messages/response/span/SelectSpanResponse";
import {UpdateSpanResponse} from "./messages/response/span/UpdateSpanResponse";
import {CreateSpanResponse} from "./messages/response/span/CreateSpanResponse";
import {DeleteSpanResponse} from "./messages/response/span/DeleteSpanResponse";
import {Relation} from "./model/Relation";
import {UpdateRelationResponse} from "./messages/response/relation/UpdateRelationResponse";
import {CreateRelationResponse} from "./messages/response/relation/CreateRelationResponse";
import {SelectRelationResponse} from "./messages/response/relation/SelectRelationResponse";
import {SelectRelationRequest} from "./messages/request/relation/SelectRelationRequest";
import {DeleteRelationRequest} from "./messages/request/relation/DeleteRelationRequest";
import {UpdateRelationRequest} from "./messages/request/relation/UpdateRelationRequest";
import {CreateRelationRequest} from "./messages/request/relation/CreateRelationRequest";
import {DeleteRelationResponse} from "./messages/response/relation/DeleteRelationResponse";
import {AllSpanResponse} from "./messages/response/span/AllSpanResponse";
import {AllRelationResponse} from "./messages/response/relation/AllRelationResponse";
import {AllSpanRequest} from "./messages/request/span/AllSpanRequest";
import {AllRelationRequest} from "./messages/request/relation/AllRelationRequest";
import {Viewport} from "./model/Viewport";

export class AnnotationExperienceAPIImpl implements AnnotationExperienceAPI {

    //Websocket and stomp broker
    private _stompClient: Client;

    //States to remember by client
    private _clientName: string;
    private _projectID: number;
    private _documentID: number;

    //Text and annotations
    private _text: string[];
    private _spans: Span[];
    private _selectedSpan: Span;

    private _relations: Relation[];
    private _selectedRelation: Relation;

    //Viewport
    private _viewport: Viewport;

    constructor() {
        this.connect();
    }


    //CREATE WEBSOCKET CONNECTION
    connect() {

        //TODO find better solution

        this._stompClient = Stomp.over(function () {
            return new WebSocket(localStorage.getItem("url"));
        });

        this._viewport = new Viewport([[0,79],[80,140],[141,189]]);

        const that = this;

        this._stompClient.onConnect = function (frame) {

            //Receive username from inital message exchange header
            const header = frame.headers;
            let data: keyof typeof header;
            for (data in header) {
                that._clientName = header[data];
                break;
            }

            //Receive project and document from URL

            that._projectID = Number(document.location.href.split("/")[5]);
            that._documentID = Number(document.location.href.split("=")[1].split("&")[0]);

            // ------ DEFINE STANDARD SUBSCRIPTION CHANNELS WITH ACTIONS ------ //

            that._stompClient.subscribe("/queue/new_document_for_client/" + that._clientName, function (msg) {
                that.onNewDocument(Object.assign(new NewDocumentResponse(), JSON.parse(msg.body)));
            }, {id: "new_document"});

            that._stompClient.subscribe("/queue/new_viewport_for_client/" + that._clientName, function (msg) {
                that.onNewViewport(Object.assign(new NewViewportResponse(), JSON.parse(msg.body)));
            }, {id: "new_viewport"});

            that._stompClient.subscribe("/queue/selected_annotation_for_client/" + that._clientName, function (msg) {
                that.onSpanSelect(Object.assign(new SelectSpanResponse(), JSON.parse(msg.body)));
            }, {id: "selected_annotation"});

            that._stompClient.subscribe("/queue/selected_relation_for_client/" + that._clientName, function (msg) {
                that.onRelationSelect(Object.assign(new SelectRelationResponse(), JSON.parse(msg.body)));
            }, {id: "selected_relation"});

            that._stompClient.subscribe("/queue/error_for_client/" + that._clientName, function (msg) {
                that.onError(Object.assign(new ErrorMessage(), JSON.parse(msg.body)));
            }, {id: "error_message"});

            that._stompClient.subscribe("/queue/all_spans_for_client/" + that._clientName, function (msg) {
                that.onAllSpans(Object.assign(new AllSpanResponse(), JSON.parse(msg.body)));
            }, {id: "all_spans"});

            that._stompClient.subscribe("/queue/all_relations_for_client/" + that._clientName, function (msg) {
                that.onAllRelations(Object.assign(new AllRelationResponse(), JSON.parse(msg.body)));
            }, {id: "error_message"});
        };


        // ------ STOMP ERROR HANDLING ------ //

        this._stompClient.onStompError = function (frame) {
            console.log('Broker reported error: ' + frame.headers['message']);
            console.log('Additional details: ' + frame.body);
        };

        this._stompClient.activate();
    }

    multipleSubscriptions()
    {
        const that = this;
        for (let i = 0; i < this._viewport.viewport.length; i++) {
            for (let j = this._viewport.viewport[i][0]; j <= this._viewport.viewport[i][1]; j++) {

                this._stompClient.subscribe("/topic/span_update_for_clients/" +
                    this._projectID + "/" +
                    this._documentID + "/" +
                    j, function (msg) {
                    that.onSpanUpdate(Object.assign(new UpdateSpanResponse(), JSON.parse(msg.body)));
                }, {id: "span_update_" + j});

                this._stompClient.subscribe("/topic/span_create_for_clients/" +
                    this._projectID + "/" +
                    this._documentID + "/" +
                    j, function (msg) {
                    that.onSpanCreate(Object.assign(new CreateSpanResponse(), JSON.parse(msg.body)));
                }, {id: "span_create_" + j});

                this._stompClient.subscribe("/topic/span_delete_for_clients/" +
                    this._projectID + "/" +
                    this._documentID + "/" +
                    j, function (msg) {
                    that.onSpanDelete(Object.assign(new DeleteSpanResponse(), JSON.parse(msg.body)));
                }, {id: "span_delete_" + j});

                this._stompClient.subscribe("/topic/relation_update_for_clients/" +
                    this._projectID + "/" +
                    this._documentID + "/" +
                    j, function (msg) {
                    that.onRelationUpdate(Object.assign(new UpdateRelationResponse(), JSON.parse(msg.body)));
                }, {id: "relation_update_" + j});

                this._stompClient.subscribe("/topic/relation_create_for_clients/" +
                    this._projectID + "/" +
                    this._documentID + "/" +
                    j, function (msg) {
                    that.onRelationCreate(Object.assign(new CreateRelationResponse(), JSON.parse(msg.body)));
                }, {id: "relation_create_" + j});

                this._stompClient.subscribe("/topic/relation_delete_for_clients/" +
                    this._projectID + "/" +
                    this._documentID + "/" +
                    j, function (msg) {
                    that.onRelationDelete(Object.assign(new DeleteRelationResponse(), JSON.parse(msg.body)));
                }, {id: "relation_delete_" + j});
            }
        }
    }

    unsubscribe(aChannel: string) {
        this._stompClient.unsubscribe(aChannel);
    }

    disconnect() {
        this._stompClient.deactivate();
    }

    requestNewDocumentFromServer(aClientName: string, aUserName: string, aProjectId: number, aDocumentId: number, aViewport: Viewport) {
        const that = this;
        this._viewport = aViewport;
        that._stompClient.publish({
            destination: "/app/new_document_from_client", body: JSON.stringify(
                new NewDocumentRequest(aClientName, aUserName, aProjectId, aDocumentId, aViewport))
        });
    }

    requestNewViewportFromServer(aClientName: string, aUserName: string, aProjectId: number, aDocumentId: number, aViewport: Viewport) {
        this._viewport = aViewport;
        this._stompClient.publish({
            destination: "/app/new_viewport_from_client",
            body: JSON.stringify(new NewViewportRequest(aClientName, aUserName, aProjectId, aDocumentId, aViewport))
        });
    }


    requestSelectSpanFromServer(aClientName: string, aUserName: string, aProjectId: number, aDocumentId: number, aAnnotationAddress: number)
    {
        this._stompClient.publish({
            destination: "/app/select_annotation_from_client",
            body: JSON.stringify(new SelectSpanRequest(aClientName, aUserName, aProjectId, aDocumentId, aAnnotationAddress))
        });
    }

    requestUpdateSpanFromServer(aClientName: string, aUserName: string, aProjectId: number, aDocumentId: number, aAnnotationAddress: number, aNewFeature: string[])
    {
        this._stompClient.publish({
            destination: "/app/update_annotation_from_client",
            body: JSON.stringify(new UpdateSpanRequest(aClientName, aUserName, aProjectId, aDocumentId, aAnnotationAddress, aNewFeature))
        });
    }

    requestCreateSpanFromServer(aClientName: string, aUserName: string, aProjectId: number, aDocumentId: number, aBegin: number, aEnd: number, aType: string, aFeature: string) {
        this._stompClient.publish({
            destination: "/app/new_annotation_from_client",
            body: JSON.stringify(new CreateSpanRequest(aClientName, aUserName, aProjectId, aDocumentId, aBegin, aEnd, aType, aFeature))
        });
    }

    requestDeleteSpanFromServer(aClientName: string, aUserName: string, aProjectId: number, aDocumentId: number, aAnnotationAddress: number) {
        this._stompClient.publish({
            destination: "/app/delete_annotation_from_client",
            body: JSON.stringify(new DeleteSpanRequest(aClientName, aUserName, aProjectId, aDocumentId, aAnnotationAddress))
        });
    }

    requestSelectRelationFromServer(aClientName: string, aUserName: string, aProjectId: number, aDocumentId: number, aRelationAddress: number)
    {
        this._stompClient.publish({
            destination: "/app/select_relation_from_client",
            body: JSON.stringify(new SelectRelationRequest(aClientName, aUserName, aProjectId, aDocumentId, aRelationAddress))
        });
    }


    requestUpdateRelationFromServer(aClientName: string, aUserName: string, aProjectId: number, aDocumentId: number, aRelationAddress: number, aNewFlavor: string, aNewRelation: string)
    {
        this._stompClient.publish({
        destination: "/app/update_relation_from_client",
        body: JSON.stringify(new UpdateRelationRequest(aClientName, aUserName, aProjectId, aDocumentId, aRelationAddress, aNewFlavor, aNewRelation))
    });
    }

    requestCreateRelationFromServer(aClientName: string, aUserName: string, aProjectId: number, aDocumentId: number, aGovernorId : number, aDependentId : number, aDependencyType : string, aFlavor : string)
    {
        this._stompClient.publish({
            destination: "/app/new_relation_from_client",
            body: JSON.stringify(new CreateRelationRequest(aClientName, aUserName, aProjectId, aDocumentId, aGovernorId, aDependentId, aDependencyType, aFlavor))
        });
    }

    requestDeleteRelationFromServer(aClientName: string, aUserName: string, aProjectId: number, aDocumentId: number, aRelationAddress: number)
    {
        this._stompClient.publish({
            destination: "/app/delete_relation_from_client",
            body: JSON.stringify(new DeleteRelationRequest(aClientName, aUserName, aProjectId, aDocumentId, aRelationAddress))
        });
    }

    requestAllSpansFromServer(aClientName: string, aUserName: string, aProjectId: number, aDocumentId: number)
    {
        this._stompClient.publish({
            destination: "/app/all_spans_from_client",
            body: JSON.stringify(new AllSpanRequest(aClientName, aUserName, aProjectId, aDocumentId))
        });
    }

    requestAllRelationsFromServer(aClientName: string, aUserName: string, aProjectId: number, aDocumentId: number)
    {
        this._stompClient.publish({
        destination: "/app/all_relations_from_client",
        body: JSON.stringify(new AllRelationRequest(aClientName, aUserName, aProjectId, aDocumentId))
    });
    }


    onNewDocument(aMessage: NewDocumentResponse)
    {
        this._documentID = aMessage.documentId;
        this._text = aMessage.viewportText;
        this._spans = aMessage.spans;
        this._relations = aMessage.relations;

        this.multipleSubscriptions()
    }

    onNewViewport(aMessage: NewViewportResponse)
    {
        this._text = aMessage.viewportText;
        this._spans = aMessage.spans;
        this._relations = aMessage.relations;

        this.multipleSubscriptions();
    }

    onSpanDelete(aMessage: DeleteSpanResponse) {
        this._spans.forEach((item, index) => {
            if (item.id.toString() === aMessage.spanAddress.toString()) {
                this._spans.splice(index, 1);
            }
        });
    }

    onSpanCreate(aMessage: CreateSpanResponse) {
        let span = new Span(aMessage.spanAddress, aMessage.coveredText, aMessage.begin, aMessage.end, aMessage.type, aMessage.feature, aMessage.color)
        this.spans.push(span)
    }

    onSpanSelect(aMessage: SelectSpanResponse) {
        this._selectedSpan = new Span(aMessage.spanAddress, aMessage.coveredText, aMessage.begin, aMessage.end, aMessage.type, aMessage.feature, aMessage.color)
    }

    onSpanUpdate(aMessage: UpdateSpanResponse) {
        console.log('RECEIVED ANNOTATION UPDATE');
        console.log(aMessage);

        this.spans[this.spans.findIndex(s => s.id == aMessage.spanAddress)].feature = aMessage.feature;
    }


    onRelationSelect(aMessage: SelectRelationResponse)
    {
        console.log(aMessage.relationAddress)
        console.log(aMessage.governorId)
        console.log(aMessage.governorCoveredText)

        this.selectedRelation = new Relation(aMessage.relationAddress,aMessage.governorId, aMessage.dependentId,aMessage.governorCoveredText, aMessage.dependentCoveredText, aMessage.color, aMessage.dependencyType, aMessage.flavor)
    }

    onRelationCreate(aMessage: CreateRelationResponse)
    {
        let relation = new Relation(aMessage.relationAddress, aMessage.governorId, aMessage.dependentId,aMessage.governorCoveredText, aMessage.dependentCoveredText, aMessage.color, aMessage.dependencyType, aMessage.flavor)
        this.relations.push(relation)
    }

    onRelationDelete(aMessage: DeleteRelationResponse)
    {
        this._relations.forEach((item, index) => {
            if (item.id.toString() === aMessage.relationAddress.toString()) {
                this._relations.splice(index, 1);
            }
        });
    }
    onRelationUpdate(aMessage: UpdateRelationResponse)
    {
        console.log('RECEIVED UPDATE RELATION');
        console.log(aMessage);

        let relation = this.relations.findIndex(r => r.id == aMessage.relationAddress);

        this.relations[relation].dependencyType = aMessage.newDependencyType;
        this.relations[relation].flavor = aMessage.newFlavor;
    }

    onAllSpans(aMessage: AllSpanResponse)
    {
        this.spans = aMessage.span;
    }

    onAllRelations(aMessage: AllRelationResponse)
    {
        this.relations = aMessage.relation;
    }


    onError(aMessage: ErrorMessage) {
        console.log('RECEIVED ERROR MESSAGE');
        console.log(aMessage);
    }

    /**
     * Getter and Setter
     */

    get clientName(): string {
        return this._clientName;
    }

    set clientName(value: string) {
        this._clientName = value;
    }

    get projectID(): number {
        return this._projectID;
    }

    set projectID(value: number) {
        this._projectID = value;
    }

    get documentID(): number {
        return this._documentID;
    }

    set documentID(value: number) {
        this._documentID = value;
    }

    get text(): string[] {
        return this._text;
    }

    set text(value: string[]) {
        this._text = value;
    }

    get spans(): Span[] {
        return this._spans;
    }

    set spans(value: Span[]) {
        this._spans = value;
    }

    get selectedSpan(): Span {
        return this._selectedSpan;
    }

    set selectedSpan(value: Span) {
        this._selectedSpan = value;
    }

    get relations(): Relation[] {
        return this._relations;
    }

    set relations(value: Relation[]) {
        this._relations = value;
    }

    get selectedRelation(): Relation {
        return this._selectedRelation;
    }

    set selectedRelation(value: Relation) {
        this._selectedRelation = value;
    }

    get viewport(): Viewport {
        return this._viewport;
    }

    set viewport(value: Viewport) {
        this._viewport = value;
    }
}


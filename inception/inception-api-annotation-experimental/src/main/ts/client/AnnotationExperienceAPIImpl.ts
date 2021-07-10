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
import {SelectAnnotationResponse} from "./messages/response/SelectAnnotationResponse";
import {ErrorMessage} from "./messages/response/ErrorMessage";
import {UpdateAnnotationResponse} from "./messages/response/UpdateAnnotationResponse";
import {CreateAnnotationResponse} from "./messages/response/CreateAnnotationResponse";
import {DeleteAnnotationResponse} from "./messages/response/DeleteAnnotationResponse";
import {NewDocumentRequest} from "./messages/request/NewDocumentRequest";
import {NewViewportRequest} from "./messages/request/NewViewportRequest";
import {SelectAnnotationRequest} from "./messages/request/SelectAnnotationRequest";
import {UpdateAnnotationRequest} from "./messages/request/UpdateAnnotationRequest";
import {CreateAnnotationRequest} from "./messages/request/CreateAnnotationRequest";
import {DeleteAnnotationRequest} from "./messages/request/DeleteAnnotationRequest";

export class AnnotationExperienceAPIImpl implements AnnotationExperienceAPI {

    //Websocket and stomp broker
    stompClient: Client;

    //States to remember by client
    clientName: string;
    projectID: number;
    documentID: number;


    //Text and annotations
    text: string[];
    span: Span[];
    selectedSpan: Span;

    //Viewport
    viewport: number[][];

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

            //Receive username from inital message exchange header
            const header = frame.headers;
            let data: keyof typeof header;
            for (data in header) {
                that.clientName = header[data];
                break;
            }

            //Receive project and document from URL

            that.projectID = Number(document.location.href.split("/")[5]);
            that.documentID = Number(document.location.href.split("=")[1].split("&")[0]);

            // ------ DEFINE STANDARD SUBSCRIPTION CHANNELS WITH ACTIONS ------ //

            that.stompClient.subscribe("/queue/new_document_for_client/" + that.clientName, function (msg) {
                that.onNewDocument(Object.assign(new NewDocumentResponse(), JSON.parse(msg.body)));
            }, {id: "new_document"});

            that.stompClient.subscribe("/queue/new_viewport_for_client/" + that.clientName, function (msg) {
                that.onNewViewport(Object.assign(new NewViewportResponse(), JSON.parse(msg.body)));
            }, {id: "new_viewport"});

            that.stompClient.subscribe("/queue/selected_annotation_for_client/" + that.clientName, function (msg) {
                that.onAnnotationSelect(Object.assign(new SelectAnnotationResponse(), JSON.parse(msg.body)));
            }, {id: "selected_annotation"});

            that.stompClient.subscribe("/queue/error_for_client/" + that.clientName, function (msg) {
                that.onError(Object.assign(new ErrorMessage(), JSON.parse(msg.body)));
            }, {id: "error_message"});
        };


        // ------ STOMP ERROR HANDLING ------ //

        this.stompClient.onStompError = function (frame) {
            console.log('Broker reported error: ' + frame.headers['message']);
            console.log('Additional details: ' + frame.body);
        };

        this.stompClient.activate();
    }

    unsubscribe(aChannel: string) {
        this.stompClient.unsubscribe(aChannel);
    }

    disconnect() {
        this.stompClient.deactivate();
    }

    requestNewDocumentFromServer(aClientName: string, aUserName: string, aProjectId: number, aDocumentId: number, aViewportType: string, aViewport: number[][], aRecommenderEnabled: boolean) {
        this.viewport = aViewport;
        this.stompClient.publish({
            destination: "/app/new_document_from_client", body: JSON.stringify(
                new NewDocumentRequest(aClientName, aUserName, aProjectId, aDocumentId, aViewportType, aViewport, aRecommenderEnabled))
        });
    }

    requestNewViewportFromServer(aClientName: string, aUserName: string, aProjectId: number, aDocumentId: number, aViewportType: string, aViewport: number[][], aRecommenderEnabled: boolean) {
        this.viewport = aViewport;
        this.stompClient.publish({
            destination: "/app/new_viewport_from_client",
            body: JSON.stringify(new NewViewportRequest(aClientName, aUserName, aProjectId, aDocumentId, aViewportType, aViewport, aRecommenderEnabled))
        });
    }

    requestSelectAnnotationFromServer(aClientName: string, aUserName: string, aProjectId: number, aDocumentId: number, aAnnotationAddress: number) {
        this.stompClient.publish({
            destination: "/app/select_annotation_from_client",
            body: JSON.stringify(new SelectAnnotationRequest(aClientName, aUserName, aProjectId, aDocumentId, aAnnotationAddress))
        });
    }

    requestUpdateAnnotationFromServer(aClientName: string, aUserName: string, aProjectId: number, aDocumentId: number, aAnnotationAddress: number, aNewType: string) {
        this.stompClient.publish({
            destination: "/app/update_annotation_from_client",
            body: JSON.stringify(new UpdateAnnotationRequest(aClientName, aUserName, aProjectId, aDocumentId, aAnnotationAddress, aNewType))
        });
    }

    requestCreateAnnotationFromServer(aClientName: string, aUserName: string, aProjectId: number, aDocumentId: number, aBegin: number, aEnd: number) {
        this.stompClient.publish({
            destination: "/app/new_annotation_from_client",
            body: JSON.stringify(new CreateAnnotationRequest(aClientName, aUserName, aProjectId, aDocumentId, aBegin, aEnd))
        });
    }

    requestDeleteAnnotationFromServer(aClientName: string, aUserName: string, aProjectId: number, aDocumentId: number, aAnnotationAddress: number) {

        this.stompClient.publish({
            destination: "/app/delete_annotation_from_client",
            body: JSON.stringify(new DeleteAnnotationRequest(aClientName, aUserName, aProjectId, aDocumentId, aAnnotationAddress))
        });
    }

    onNewDocument(aMessage: NewDocumentResponse) {
        console.log("RECEIVED DOCUMENT");
        console.log(aMessage);

        console.log("---------")
        console.log(this.viewport)

        const that = this;

        this.documentID = aMessage.documentId;
        this.text = aMessage.viewportText;
        this.span = aMessage.spanAnnotations;

        for (let i = 0; i < this.viewport.length; i++) {
            for (let j = this.viewport[i][0]; j <= this.viewport[i][1]; j++) {

                this.stompClient.subscribe("/topic/annotation_update_for_clients/" +
                    this.projectID + "/" +
                    this.documentID + "/" +
                    j, function (msg) {
                    that.onAnnotationUpdate(Object.assign(new UpdateAnnotationResponse(), JSON.parse(msg.body)));
                }, {id: "annotation_update_" + j});
            }
        }
    }

    onNewViewport(aMessage: NewViewportResponse) {
        console.log('RECEIVED VIEWPORT');
        console.log(aMessage);

        const that = this;

        this.text = aMessage.viewportText;
        this.span = aMessage.spanAnnotations;

        for (let i = 0; i < this.viewport.length; i++) {
            for (let j = this.viewport[i][0]; j <= this.viewport[i][1]; j++) {

                this.stompClient.subscribe("/topic/annotation_update_for_clients/" +
                    this.projectID + "/" +
                    this.documentID + "/" +
                    j, function (msg) {
                    that.onAnnotationUpdate(Object.assign(new UpdateAnnotationResponse(), JSON.parse(msg.body)));
                }, {id: "annotation_update_" + j});
            }
        }
    }

    onAnnotationSelect(aMessage: SelectAnnotationResponse) {
        console.log('RECEIVED SELECTED ANNOTATION');
        console.log(aMessage);
        this.selectedSpan = new Span(aMessage.annotationAddress, aMessage.coveredText, aMessage.begin, aMessage.end, aMessage.type, aMessage.feature, aMessage.color)
    }

    onAnnotationUpdate(aMessage: UpdateAnnotationResponse) {
        console.log('RECEIVED ANNOTATION UPDATE');
        console.log(aMessage);
    }

    onAnnotationCreate(aMessage: CreateAnnotationResponse) {
        console.log('RECEIVED CREATE ANNOTATIONS');
        let newAnnotation = new Span(aMessage.annotationAddress, aMessage.coveredText, aMessage.begin, aMessage.end, aMessage.type, aMessage.feature, aMessage.color)
        this.span.push(newAnnotation)
    }

    onAnnotationDelete(aMessage: DeleteAnnotationResponse) {
        console.log('RECEIVED ANNOTATION DELETE');
        this.span.forEach((item, index) => {
            if (item.id.toString() === aMessage.annotationAddress.toString()) {
                this.span.splice(index, 1);
            }
        });
    }

    onError(aMessage: ErrorMessage) {
        console.log('RECEIVED ERROR MESSAGE');
        console.log(aMessage);

        console.log(aMessage.errorMessage)

    }
}

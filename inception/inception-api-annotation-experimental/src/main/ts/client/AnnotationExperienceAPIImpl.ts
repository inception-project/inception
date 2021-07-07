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
import {AnnotationExperienceAPIVisualization} from "./visualization/AnnotationExperienceAPIVisualization";
import {AnnotationExperienceAPIActionHandler} from "./actionhandling/AnnotationExperienceAPIActionHandler";
import {ServerMessage} from "./util/ServerMessage";
import {AnnotationExperienceAPI} from "./AnnotationExperienceAPI"
import {Annotation} from "./util/Annotation";

export class AnnotationExperienceAPIImpl implements AnnotationExperienceAPI {

    //Websocket and stomp broker
    stompClient: Client;
    connected: boolean = false;

    //States to remember by client
    client: string;
    projectID: string;

    documentID: string;


    //Text and annotations
    text: String[];
    annotations: Annotation[];
    selectedAnnotation: Annotation;

    //Viewport
    viewport: number[][];

    //Visualizer
    readonly visualizer: AnnotationExperienceAPIVisualization;

    //Actionhandler
    readonly actionhandler: AnnotationExperienceAPIActionHandler;

    constructor() {
        this.connect();

        //Visualizer
        this.visualizer = new AnnotationExperienceAPIVisualization(this);

        //ActionHandler
        this.actionhandler = new AnnotationExperienceAPIActionHandler(this);

        this.actionhandler.registerDefaultActionHandler();
    }


    //CREATE WEBSOCKET CONNECTION
    connect() {
        if (this.connected) {
            console.log("You are already connected")
            return;
        }

        this.stompClient = Stomp.over(function () {
            return new WebSocket(localStorage.getItem("url"));
        });

        const that = this;

        this.stompClient.onConnect = function (frame) {
            that.connected = true;

            //Receive username from inital message exchange header
            const header = frame.headers;
            let data: keyof typeof header;
            for (data in header) {
                that.client = header[data];
                break;
            }

            //Receive project and document from URL

            that.projectID = document.location.href.split("/")[5];
            that.documentID = document.location.href.split("=")[1].split("&")[0];

            // ------ DEFINE STANDARD SUBSCRIPTION CHANNELS WITH ACTIONS ------ //

            that.stompClient.subscribe("/queue/new_document_for_client/" + that.client, function (msg) {
                that.receiveNewDocumentMessageByServer(Object.assign(new ServerMessage(), JSON.parse(msg.body)));
            }, {id: "new_document"});

            that.stompClient.subscribe("/queue/new_viewport_for_client/" + that.client, function (msg) {
                that.receiveNewViewportMessageByServer(Object.assign(new ServerMessage(), JSON.parse(msg.body)));
            }, {id: "new_viewport"});

            that.stompClient.subscribe("/queue/selected_annotation_for_client/" + that.client, function (msg) {
                that.receiveSelectedAnnotationMessageByServer(Object.assign(new ServerMessage(), JSON.parse(msg.body)));
            }, {id: "selected_annotation"});

            that.stompClient.subscribe("/queue/error_for_client/" + that.client, function (msg) {
                that.receiveErrorMessageByServer(Object.assign(new ServerMessage(), JSON.parse(msg.body)));
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

    // ------ DISCONNECT -------- //
    disconnect() {
        if (this.connected) {
            console.log("Disconnecting now");
            this.connected = false;
            this.stompClient.deactivate();
        }
    }

    editAnnotation(aId, aAnnotationType) {
        let json = {
            username: this.client,
            clientName: this.client,
            project: this.projectID,
            document: this.documentID,
            annotationAddress: aId,
            annotationType: aAnnotationType
        };

        //TODO correct edit
        //this.stompClient.publish({destination: "/app/select_annotation_by_client", body: JSON.stringify(json)});
    }

    sendDocumentMessageToServer(aUsername, aDocument, aOffset, aOffsetType) {

        let json = {
            clientName: this.client,
            username: aUsername,
            project: this.projectID,
            document: aDocument,
            viewport: aOffset,
            offsetType: aOffsetType
        }

        this.viewport = json.viewport;
        this.stompClient.publish({destination: "/app/new_document_by_client", body: JSON.stringify(json)});
    }

    sendViewportMessageToServer(aUsername, aViewport, aOffsetType) {
        let json = {
            clientName: this.client,
            username: aUsername,
            project: this.projectID,
            document: this.documentID,
            viewport: aViewport,
            offsetType: aOffsetType
        };
        this.viewport = aViewport;
        this.stompClient.publish({destination: "/app/new_viewport_by_client", body: JSON.stringify(json)});
    }

    sendSelectAnnotationMessageToServer(aUsername, aId) {
        let json = {
            clientName: this.client,
            username: aUsername,
            project: this.projectID,
            document: this.documentID,
            annotationAddress: aId
        };
        this.stompClient.publish({destination: "/app/select_annotation_by_client", body: JSON.stringify(json)});
    }

    sendUpdateAnnotationMessageToServer(aUsername, aDocument, aId, aNewAnnotationType, aNewAnnotationFeature) {
        let json = {
            username: aUsername,
            project: this.projectID,
            document: aDocument,
            annotationAddress: aId,
            annotationType: aNewAnnotationType,
            annotationFeature: aNewAnnotationFeature
        };
        this.stompClient.publish({destination: "/app/update_annotation_by_client", body: JSON.stringify(json)});
    }

    sendCreateAnnotationMessageToServer(aUsername, aDocument, aBegin, aEnd, aAnnotationType) {
        let json = {
            username: aUsername,
            project: this.projectID,
            document: this.documentID,
            annotationOffsetBegin: aBegin,
            annotationOffsetEnd: aEnd,
            annotationType: aAnnotationType
        };
        this.stompClient.publish({destination: "/app/new_annotation_by_client", body: JSON.stringify(json)});
    }

    sendDeleteAnnotationMessageToServer(aId: string, aAnnotationType: string) {
        let json = {
            username: this.client,
            project: this.projectID,
            document: this.documentID,
            annotationAddress: aId,
            annotationType: aAnnotationType
        };
        this.stompClient.publish({destination: "/app/delete_annotation_by_client", body: JSON.stringify(json)});
    }

    receiveNewDocumentMessageByServer(aMessage: ServerMessage) {
        console.log("RECEIVED DOCUMENT");
        console.log(aMessage);

        console.log("---------")
        console.log(this.viewport)

        const that = this;

        this.documentID = aMessage.document.toString();
        this.text = aMessage.viewportText;
        this.annotations = aMessage.annotations;

        for (let i = 0; i < this.viewport.length; i++) {
            for (let j = this.viewport[i][0]; j <= this.viewport[i][1]; j++) {

                this.stompClient.subscribe("/topic/annotation_update_for_clients/" +
                    this.projectID + "/" +
                    this.documentID + "/" +
                    j, function (msg) {
                    that.receiveAnnotationMessageByServer(Object.assign(new ServerMessage(), JSON.parse(msg.body)));
                }, {id: "annotation_update_" + j});
            }
        }
    }

    receiveNewViewportMessageByServer(aMessage: ServerMessage) {
        console.log('RECEIVED VIEWPORT');
        console.log(aMessage);

        const that = this;

        this.text = aMessage.viewportText;
        this.annotations = aMessage.annotations;

        for (let i = 0; i < aMessage.viewport.length; i++) {
            for (let j = aMessage.viewport[i][0]; j <= aMessage.viewport[i][1]; j++) {

                this.stompClient.subscribe("/topic/annotation_update_for_clients/" +
                    this.projectID + "/" +
                    this.documentID + "/" +
                    j, function (msg) {
                    that.receiveAnnotationMessageByServer(Object.assign(new ServerMessage(), JSON.parse(msg.body)));
                }, {id: "annotation_update_" + j});
            }
        }
    }

    receiveSelectedAnnotationMessageByServer(aMessage: ServerMessage) {
        console.log('RECEIVED SELECTED ANNOTATION');
        console.log(aMessage);
        this.selectedAnnotation = new Annotation(aMessage.annotationAddress.toString(), aMessage.annotationText, aMessage.annotationOffsetBegin, aMessage.annotationOffsetEnd, aMessage.annotationType)
    }

    receiveAnnotationMessageByServer(aMessage: ServerMessage) {
        console.log('RECEIVED ANNOTATION MESSAGE');
        console.log(aMessage);

        //TODO Both toString() necessary, but how to do it better?
        //RECEIVED DELETE
        if (aMessage.delete) {
            console.log("DELETE")
            this.annotations.forEach((item, index) => {
                if (item.id.toString() === aMessage.annotationAddress.toString()) {
                    this.annotations.splice(index, 1);
                }
            });
            //RECEIVED EDIT
        } else if (aMessage.edit) {
            console.log("UPDATE")
            //RECEIVED NEW ANNOTATION

        } else {
            console.log("NEW")
            let newAnnotation = new Annotation(aMessage.annotationAddress.toString(), aMessage.annotationText, aMessage.annotationOffsetBegin, aMessage.annotationOffsetEnd, aMessage.annotationType)
            this.annotations.push(newAnnotation)
        }
    }

    receiveErrorMessageByServer(aMessage: ServerMessage)
    {
        console.log('RECEIVED ERROR MESSAGE');
        console.log(aMessage);

        console.log(aMessage.errorMessage)

    }
}

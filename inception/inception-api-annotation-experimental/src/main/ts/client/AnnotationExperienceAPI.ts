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

export class AnnotationExperienceAPI {

    //Websocket and stomp broker
    stompClient: Client;
    connected: boolean = false;

    //States to remember by client
    username: string;
    projectID: string;
    documentID: string;

    //Visualizer
    visualizer: AnnotationExperienceAPIVisualization;

    //Actionhandler
    actionhandler: AnnotationExperienceAPIActionHandler;

    constructor() {
        this.connect();

        //Visualizer
        this.visualizer = new AnnotationExperienceAPIVisualization();

        //ActionHandler
        this.actionhandler = new AnnotationExperienceAPIActionHandler(this);
    }


    //CREATE WEBSOCKET CONNECTION
    connect() {
        if (this.connected) {
            console.log("You are already connected")
            return;
        }

        let url: string = (window.location.protocol.startsWith("https") ? "wss://" : "ws://")
            + window.location.host + "/inception_app_webapp_war_exploded/ws-endpoint";

        this.stompClient = Stomp.over(function () {
            return new WebSocket(url);
        });

        const that = this;

        this.stompClient.onConnect = function (frame) {
            that.connected = true;

            //Receive username from inital message exchange header
            const header = frame.headers;
            let data: keyof typeof header;
            for (data in header) {
                that.username = header[data];
                break;
            }

            //Receive project and document from URL
            that.projectID = document.location.href.split("/")[5];
            that.documentID = document.location.href.split("=")[1].split("&")[0];

            // ------ DEFINE SUBSCRIPTION CHANNELS WITH ACTIONS ------ //

            that.stompClient.subscribe("/queue/new_document_for_client/" + that.username, function (msg) {
                that.receiveNewDocumentMessageByServer(JSON.parse(msg.body));
            }, {id: "new_document"});

            that.stompClient.subscribe("/queue/new_viewport_for_client/" + that.username, function (msg) {
                that.receiveNewViewportMessageByServer(JSON.parse(msg.body));
            }, {id: "new_viewport"});

            that.stompClient.subscribe("/queue/selected_annotation_for_client/" + that.username, function (msg) {
                that.receiveSelectedAnnotationMessageByServer(JSON.parse(msg.body));
            }, {id: "selected_annotation"});

        };

        // ------ ERROR HANDLING ------ //

        this.stompClient.onStompError = function (frame) {
            console.log('Broker reported error: ' + frame.headers['message']);
            console.log('Additional details: ' + frame.body);
        };

        this.stompClient.activate();

    }

    unsubscribe(aChannel: string)
    {
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

    editAnnotation()
    {

    }


    sendDocumentMessageToServer()
    {
        let json = {
            username: this.username,
            project: this.projectID,
            viewport: [[0,20],[21,33],[37,50]]

        };


        this.visualizer.viewport = json.viewport;

        this.stompClient.publish({destination: "/app/new_document_by_client", body: JSON.stringify(json)});
    }

    sendDocumentMessageToServer(aUsername: string, aDocument: string)
    {
        let json = {
            username: aUsername,
            project: this.projectID,
            document: aDocument,
            viewport: [[0,20],[21,33],[37,50]]
        };


        this.visualizer.viewport = json.viewport;

        this.stompClient.publish({destination: "/app/new_document_by_client", body: JSON.stringify(json)});
    }

    sendViewportMessageToServer(aViewport: number[][])
    {
        let json = {
            username: this.username,
            project: this.projectID,
            document: this.documentID,
            viewport: aViewport
        };

        /* TODO adapt to char array
        for (let i = 0; i < this.viewport.length; i++) {
            for (let j = this.viewport[i][0]; j <= this.viewport[i][1]; j++) {
                this.unsubscribe("annotation_update_" + j.toString());
            }
        }

         */

        this.visualizer.viewport = aViewport;

        this.stompClient.publish({destination: "/app/new_viewport_by_client", body: JSON.stringify(json)});
    }

    sendSelectAnnotationMessageToServer(aId: string)
    {
        let json = {
            username: this.username,
            project: this.projectID,
            document: this.documentID,
            annotationAddress: aId
        };
        this.stompClient.publish({destination: "/app/select_annotation_by_client", body: JSON.stringify(json)});
    }

    sendCreateAnnotationMessageToServer(begin: string, end: string, aType: string)
    {
        let json = {
            username: this.username,
            project: this.projectID,
            document: this.documentID,
            annotationOffsetBegin: begin,
            annotationOffsetEnd: end,
            type: aType
        }
        this.stompClient.publish({destination: "/app/new_annotation_by_client", body: JSON.stringify(json)});
    }

    sendUpdateAnnotationMessageToServer(aId: string, aType: string,)
    {
        let json = {
            username: this.username,
            project: this.projectID,
            document: this.documentID,
            annotationAddress: aId,
            annotationType: aType
        }
        this.stompClient.publish({destination: "/app/update_annotation_by_client", body: JSON.stringify(json)});
    }

    sendDeleteAnnotationMessageToServer(aId: string)
    {
        let json = {
            username: this.username,
            project: this.projectID,
            document: this.documentID,
            annotationAddress: aId
        }
        this.stompClient.publish({destination: "/app/delete_annotation_by_client", body: JSON.stringify(json)});
    }

    // ---------------- RECEIVE ----------------------- //

    receiveNewDocumentMessageByServer(aMessage: string)
    {
        console.log('RECEIVED DOCUMENT:');

        const that = this;

        //Parse data
        let keys = Object.keys(aMessage)
        let values = keys.map(k => aMessage[k])

        this.documentID = values[0];
        this.visualizer.setText(values[1]);
        this.visualizer.setAnnotations(values[2]);

        /* TODO due to new char offset
        //Multiple subscriptions due to viewport
        for (let i = 0; i < this.viewport.length; i++) {
            for (let j = this.viewport[i][0]; j < this.viewport[i][1]; j++) {
                this.stompClient.subscribe("/topic/annotation_update_for_clients/" + this.projectID + "/" + this.documentID + "/" + j, function (msg) {
                    that.receiveAnnotationMessageByServer(JSON.parse(msg.body));
                }, {id: "annotation_update_" + j});
            }
        }

         */
    }

    receiveNewViewportMessageByServer(aMessage: string)
    {
        console.log('RECEIVED VIEWPORT');

        const that = this;

        //Parse data
        let keys = Object.keys(aMessage)
        let values = keys.map(k => aMessage[k])

        this.visualizer.setText(values[0]);
        this.visualizer.setAnnotations(values[1]);

        /* TODO due to char offset
        //Multiple subscriptions due to viewport
        for (let i = 0; i < this.viewport.length; i++) {
            for (let j = this.viewport[i][0]; j <= this.viewport[i][1]; j++) {
                this.stompClient.subscribe("/topic/annotation_update_for_clients/" + this.projectID + "/" + this.documentID + "/" + j, function (msg) {
                    that.receiveAnnotationMessageByServer(JSON.parse(msg.body));
                }, {id: "annotation_update_" + j});
            }
        }

         */
    }

    receiveSelectedAnnotationMessageByServer(aMessage: string)
    {
        console.log('RECEIVED SELECTED ANNOTATION:');
        //Parse data
        let keys = Object.keys(aMessage)
        let values = keys.map(k => aMessage[k])
        console.log(keys)
        console.log(values)
    }

    receiveAnnotationMessageByServer(aMessage: string)
    {
        console.log('RECEIVED ANNOTATION MESSAGE:');
        //Parse data
        let keys = Object.keys(aMessage)
        let values = keys.map(k => aMessage[k])
        console.log(keys)
        console.log(values)
    }
}

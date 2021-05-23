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
import {Annotation} from "../common/Annotation";

class Annotator {

    //Websocket and stomp broker
    stompClient: Client;
    connected: boolean = false;

    //States to remember by client
    username: string;
    project: string;
    document: string;

    //Viewport
    viewPortBegin: number;
    viewPortEnd: number;
    viewPortSize: number;

    //Text shown
    text: string;

    constructor(aViewPortSize: number) {
        const that = this;
        this.viewPortSize = aViewPortSize;

        //Click events on annotation page specific items
        onclick = function (aEvent) {
            let elem = <Element>aEvent.target;

            if (elem.tagName === 'text') {
                that.sendSelectAnnotationMessageToServer(new Annotation("test", "TTTT"));
            }
            // --------- NEXT DOCUMENT ------- //
            if (elem.className === 'far fa-caret-square-right') {
                that.sendNewDocumentMessageToServer();
            }

            // ---------- NEW VIEWPORT --------- //
            if (elem.className === 'fas fa-step-forward') {
                that.sendNewViewportMessageToServer(that.viewPortEnd + 1, that.viewPortEnd + 1 + that.viewPortSize);
            }

            if (elem.className === 'fas fa-step-backward') {
                that.sendNewViewportMessageToServer(that.viewPortBegin - 1 - that.viewPortSize, that.viewPortBegin - 1);
            }

            //Negativ values indicates end of the Document
            if (elem.className === 'fas fa-fast-forward') {
                that.sendNewViewportMessageToServer(0 - that.viewPortSize, 0);
            }

            if (elem.className === 'fas fa-fast-backward') {
                that.sendNewViewportMessageToServer(0, that.viewPortSize);
            }
        }

        ondblclick = function (aEvent) {
            let elem = <Element>aEvent.target;
            if (elem.tagName === 'text') {
                that.sendCreateAnnotationMessageToServer(new Annotation("test", "TTTT"));
            }
        }
        this.connect();
    }


    //CREATE WEBSOCKET CONNECTION
    connect() {
        if (this.connected) {
            console.log("You are already connected")
            return;
        }

        let url: string = (window.location.protocol.startsWith("https") ? "wss://" : "ws://")
            + window.location.host
            + "/inception_app_webapp_war_exploded/ws-endpoint";

        this.stompClient = Stomp.over(function () {
            return new WebSocket(url);
        });

        //REQUIRED DUE TO JS SCOPE
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

    // ------ DISCONNECT -------- //
    disconnect() {
        if (this.connected) {
            console.log("Disconnecting now");
            this.connected = false;
            this.stompClient.deactivate();
        }
    }

    // ------------------------------- //


    /** ----------- Actions ----------- **/

    editAnnotation = function (aEvent)
    {
        //TODO
    }

    unsubscribe(aChannel: string)
    {
        this.stompClient.unsubscribe(aChannel);
    }

    /** ----------- Event handling ------------------ **/

    // ---------------- SEND ------------------------- //
    sendNewDocumentMessageToServer()
    {
        let json = {
            username: this.username,
            project: this.project,
            viewPortSize: this.viewPortSize
        };
        this.stompClient.publish({destination: "/app/new_document_by_client", body: JSON.stringify(json)});
    }

    sendNewViewportMessageToServer(aBegin: number, aEnd: number)
    {
        let json = {
            username: this.username,
            project: this.project,
            document: this.document,
            begin: aBegin,
            end: aEnd
        };
        this.stompClient.publish({destination: "/app/new_viewport_by_client", body: JSON.stringify(json)});
    }

    sendSelectAnnotationMessageToServer(aSelectedAnnotation: Annotation)
    {
        let json = {
            username: this.username,
            project: this.project,
            document: this.document,
            id: aSelectedAnnotation.id
        };
        this.stompClient.publish({destination: "/app/select_annotation_by_client", body: JSON.stringify(json)});
    }

    sendCreateAnnotationMessageToServer(aSelectedAnnotation: Annotation)
    {
        let json = {
            username: this.username,
            project: this.project,
            document: this.document,
            Annotation: aSelectedAnnotation
        }
        this.stompClient.publish({destination: "/app/new_annotation_by_client", body: JSON.stringify(json)});
    }

    sendDeleteAnnotationMessageToServer(aSelectedAnnotation: Annotation)
    {
        let json = {
            username: this.username,
            project: this.project,
            document: this.document,
            id: aSelectedAnnotation.id
        }
        this.stompClient.publish({destination: "/app/delete_annotation_by_client", body: JSON.stringify(json)});
    }

    // ---------------- RECEIVE ----------------------- //

    receiveNewDocumentMessageByServer(aMessage: string)
    {
        console.log('RECEIVED DOCUMENT: ' + aMessage);

        const that = this;

        //Parse data
        let keys = Object.keys(aMessage)
        let values = keys.map(k => aMessage[k])

        this.document = values[0];
        this.text = values[1];

        //Unsubscribe channels for previous document
        for (let i = 0; i <= this.viewPortSize; i++) {
            this.unsubscribe("annotation_update_" + i.toString());
        };

        this.viewPortBegin = 0;
        this.viewPortEnd = this.viewPortBegin + this.viewPortSize;

        //Multiple subscriptions due to viewport
        for (let i = this.viewPortBegin; i <= this.viewPortEnd; i++) {
            this.stompClient.subscribe("/topic/annotation_update_for_clients/" + this.project + "/" + this.document + "/" + i, function (msg) {
                that.receiveAnnotationMessageByServer(JSON.parse(msg.body));
            }, {id: "annotation_update_" + i});
        }
    }

    receiveNewViewportMessageByServer(aMessage: string)
    {
        console.log('RECEIVED VIEWPORT: ' + aMessage);
    }

    receiveSelectedAnnotationMessageByServer(aMessage: string)
    {
        console.log('RECEIVED SELECTED ANNOTATION: ' + aMessage);
    }

    receiveAnnotationMessageByServer(aMessage: string)
    {
        console.log('RECEIVED ANNOTATION MESSAGE: ' + aMessage);
    }
}

let annotator = new Annotator(1);
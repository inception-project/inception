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

class Annotator {
    stompClient: Client;
    connected: boolean = false;

    document: string;
    project: string;
    username: string;
    viewPortBegin: number;
    viewPortEnd: number;
    viewPortSize: number;

    constructor() {
        const that = this;
        this.viewPortSize = 2;

        //Testing only
        //Click events triggering either select annotation or create annotation
        onclick = function (aEvent) {
            let elem = <Element>aEvent.target;
            if (elem.tagName === 'text') {
                that.sendSelectAnnotationMessageToServer();
            }
            // --------- NEXT DOCUMENT ------- //
            if (elem.className === 'far fa-caret-square-right') {
                that.sendNewDocumentMessageToServer("Doc4");
            }

            // ---------- NEW VIEWPORT --------- //
            if (elem.className === 'fas fa-step-forward') {
                that.sendNewViewportMessageToServer(that.viewPortEnd + 1, that.viewPortEnd + 1 + that.viewPortSize);
            }

            if (elem.className === 'fas fa-step-backward') {
                that.sendNewViewportMessageToServer(that.viewPortBegin - 1 - that.viewPortSize, that.viewPortBegin - 1);
            }

            if (elem.className === 'fas fa-fast-forward') {
                that.sendNewViewportMessageToServer(100, 1111);
            }

            if (elem.className === 'fas fa-fast-backward') {
                that.sendNewViewportMessageToServer(0, that.viewPortSize);
            }
        }

        ondblclick = function (aEvent) {
            let elem = <Element>aEvent.target;
            if (elem.tagName === 'text') {
                that.sendCreateAnnotationMessageToServer();
            }
        }

        this.document = "Doc4";
        this.project = "Annotation Study";

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
            + "/inception_app_webapp_war_exploded/ws";

        this.stompClient = Stomp.over(function () {
            return new WebSocket(url);
        });

        //REQUIRED DUE TO JS SCOPE
        const that = this;

        this.stompClient.onConnect = function (frame) {
            that.connected = true;

            const header = frame.headers;
            let data: keyof typeof header;
            for (data in header) {
                that.username = header[data];
                break;
            }

            // ------ DEFINE ALL SUBSCRIPTION CHANNELS WITH ACTIONS ------ //

            //Client receive initial data
            that.stompClient.subscribe("/queue/new_document_for_client/" + that.username, function (msg) {
                that.receiveNewDocumentMessageByServer(JSON.parse(msg.body));
            });
            that.stompClient.subscribe("/queue/connection_message/" + that.username, function (msg) {
                console.log("RECEIVED inital data")
            });

        };


        // ------ ERROR HANDLING ------ //

        this.stompClient.onStompError = function (frame) {
            console.log('Broker reported error: ' + frame.headers['message']);
            console.log('Additional details: ' + frame.body);
        };

        // ------------------------------- //

        this.stompClient.activate();
    }

    // ------ DISCONNECT -------- //
    disconnect()
    {
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

    unsubscribe(channel: string)
    {
        this.stompClient.unsubscribe(channel);
    }


    /** -------------------------------- **/


    /** ----------- Event handling ------------------ **/

    // ---------------- SEND ------------------------- //


    sendNewDocumentMessageToServer(aDocument : string) {
        if (!aDocument) {
            this.stompClient.publish({destination: "/app/new_document_by_client", body: "INIT"});
        } else {
            let json = JSON.stringify(
                {
                    username: this.username,
                    project: this.project,
                    nextDocument: aDocument,
                });
            this.stompClient.publish({destination: "/app/new_document_by_client", body: json});
        }
    }

    sendNewViewportMessageToServer(aBegin: number, aEnd: number)
    {
        let json = JSON.stringify(
            {
                username: this.username,
                project: this.project,
                document: this.document,
                begin: aBegin, end: aEnd
            });
        this.stompClient.publish({destination: "/app/new_viewport_by_client", body: json});
    }

    sendSelectAnnotationMessageToServer()
    {
        let json = JSON.stringify(
            {
                username: this.username,
                project: this.project,
                document: this.document,
                begin: 0, end: 8
            });
        this.stompClient.publish({destination: "/app/select_annotation_by_client", body: json});
    }

    sendCreateAnnotationMessageToServer()
    {
        let json = {
            username: this.username,
            project: this.project,
            document: this.document,
            begin: this.viewPortBegin,
            end: this.viewPortEnd
        }
        this.stompClient.publish({destination: "/app/new_annotation_by_client", body: JSON.stringify(json)});
    }

    sendDeleteAnnotationMessageToServer()
    {
        this.stompClient.publish({destination: "/app/delete_annotation_by_client", body: "DELETE"});
    }

    // ---------------- RECEIVE ----------------------- //

    receiveNewDocumentMessageByServer(aMessage: string)
    {
        console.log('RECEIVED DOCUMENT: ' + aMessage);

        let keys = Object.keys(aMessage)

        /*
        let values = keys.map(k => aMessage[k])
        console.log(keys);
        console.log(values);

         */

        //TODO RECEIVE CORRECT VALUES FROM MESSAGE
        this.project = "Annotation Study";
        this.document = "Doc4";
        //TODO UNSUBSCRIBE ALL PREVIOUS

        const that = this;

        this.stompClient.subscribe("/queue/new_viewport_for_client/" + this.username, function (msg) {
            that.receiveNewViewportMessageByServer(JSON.parse(msg.body));
        });

        this.stompClient.subscribe("/queue/selected_annotation_for_client/" + this.username, function (msg) {
            that.receiveSelectedAnnotationMessageByServer(JSON.parse(msg.body));
        });

        this.viewPortBegin = 0;
        this.viewPortEnd = this.viewPortBegin + this.viewPortSize;

        //Multiple subscriptions due to viewport
        for (let i = this.viewPortBegin; i <= this.viewPortEnd; i++) {
            this.stompClient.subscribe("/topic/annotation_created_for_clients/" + this.project + "/" + this.document + "/" + i, function (msg) {
                that.receiveNewAnnotationMessageByServer(JSON.parse(msg.body));
            });

            this.stompClient.subscribe("/topic/annotation_deleted_for_clients/" + this.project + "/" + this.document + "/" + i, function (msg) {
                that.receiveDeleteAnnotationMessageByServer(JSON.parse(msg.body));
            });
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

    receiveNewAnnotationMessageByServer(aMessage: string)
    {
        console.log('RECEIVED NEW ANNOTATION: ' + aMessage);
    }

    receiveDeleteAnnotationMessageByServer(aMessage: string)
    {
        console.log('RECEIVED DELETE ANNOTATION: ' + aMessage);
    }

    /** ---------------------------------------------- **/
}

let annotator = new Annotator()
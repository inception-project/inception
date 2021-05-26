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

class AnnotationExperienceAPI {

    //Websocket and stomp broker
    stompClient: Client;
    connected: boolean = false;

    //States to remember by client
    username: string;
    projectID: string;
    documentID: string;

    //Viewport
    viewPortBegin: number;
    viewPortEnd: number;
    viewPortSize: number;

    //Text
    text: String[];

    constructor(aViewPortSize: number) {
        const that = this;
        this.viewPortSize = aViewPortSize;

        //Click events on annotation page specific items
        onclick = function (aEvent) {
            let elem = <Element>aEvent.target;

            if (elem.tagName === 'rect') {
                console.log(elem)
                console.log(elem.attributes)
                console.log("--------")
                console.log(elem.parentElement)
                console.log(elem.parentElement.id)
                console.log(elem.parentElement.attributes)
                that.sendSelectAnnotationMessageToServer(elem.attributes[9].nodeValue);
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
                that.sendCreateAnnotationMessageToServer("ID", "TYPE", "SENTENCE_OF_ANNOTATION");
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

    setVisibleText()
    {
        let textDIV = document.getElementById("text")
        //Reset previous text
        textDIV.innerHTML= '';

        //Append new text
        for (let i = this.viewPortBegin; i <= this.viewPortEnd; i++) {
            let div = document.createElement("div");
            let node = document.createElement("sentence");
            node.innerText = this.text[i] + ' ';
            div.appendChild(node);
            textDIV.appendChild(div);
        }
    }

    drawAnnotation()
    {
    }

    editAnnotation = function ()
    {

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
            project: this.projectID,
            viewPortSize: this.viewPortSize
        };
        this.stompClient.publish({destination: "/app/new_document_by_client", body: JSON.stringify(json)});
    }

    sendNewViewportMessageToServer(aBegin: number, aEnd: number)
    {
        let json = {
            username: this.username,
            project: this.projectID,
            document: this.documentID,
            begin: aBegin,
            end: aEnd
        };
        this.stompClient.publish({destination: "/app/new_viewport_by_client", body: JSON.stringify(json)});
    }

    sendSelectAnnotationMessageToServer(aId: string)
    {
        let json = {
            username: this.username,
            project: this.projectID,
            document: this.documentID,
            id: aId
        };
        this.stompClient.publish({destination: "/app/select_annotation_by_client", body: JSON.stringify(json)});
    }

    sendCreateAnnotationMessageToServer(aId : string, aType : string, aViewport: string)
    {
        let json = {
            username: this.username,
            project: this.projectID,
            document: this.documentID,
            id: aId,
            type: aType,
            viewport: aViewport
        }
        this.stompClient.publish({destination: "/app/new_annotation_by_client", body: JSON.stringify(json)});
    }

    sendUpdateAnnotationMessageToServer(aId : string, aType : string,)
    {
        let json = {
            username: this.username,
            project: this.projectID,
            document: this.documentID,
            id: aId,
            type: aType
        }
        this.stompClient.publish({destination: "/app/delete_annotation_by_client", body: JSON.stringify(json)});
    }

    sendDeleteAnnotationMessageToServer(aId: string)
    {
        let json = {
            username: this.username,
            project: this.projectID,
            document: this.documentID,
            id: aId,
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

        console.log(values);

        this.documentID = values[0];
        this.text = values[1];
        this.setVisibleText();

        //Unsubscribe channels for previous document
        for (let i = 0; i <= this.viewPortSize; i++) {
            this.unsubscribe("annotation_update_" + i.toString());
        };

        this.viewPortBegin = 0;
        this.viewPortEnd = this.viewPortBegin + this.viewPortSize;

        //Multiple subscriptions due to viewport
        for (let i = this.viewPortBegin; i <= this.viewPortEnd; i++) {
            this.stompClient.subscribe("/topic/annotation_update_for_clients/" + this.projectID + "/" + this.documentID + "/" + i, function (msg) {
                that.receiveAnnotationMessageByServer(JSON.parse(msg.body));
            }, {id: "annotation_update_" + i});
        }

        //Draw the visible annotations
        if (values[2] != null) {
        }

        //Remember new documentID from new URL
        this.documentID = document.location.href.split("=")[1].split("&")[0];

    }

    receiveNewViewportMessageByServer(aMessage: string)
    {
        console.log('RECEIVED VIEWPORT: ' + aMessage);
        //Parse data
        let keys = Object.keys(aMessage)
        let values = keys.map(k => aMessage[k])
        console.log(values)
        this.text = values[2];
        this.setVisibleText();

    }

    receiveSelectedAnnotationMessageByServer(aMessage: string)
    {
        console.log('RECEIVED SELECTED ANNOTATION: ' + aMessage);
        //Parse data
        let keys = Object.keys(aMessage)
        let values = keys.map(k => aMessage[k])
        console.log(keys)
        console.log(values)
    }

    receiveAnnotationMessageByServer(aMessage: string)
    {
        console.log('RECEIVED ANNOTATION MESSAGE: ' + aMessage);
        //Parse data
        let keys = Object.keys(aMessage)
        let values = keys.map(k => aMessage[k])
        console.log(keys)
        console.log(values)
    }
}

let annotator = new AnnotationExperienceAPI(2);
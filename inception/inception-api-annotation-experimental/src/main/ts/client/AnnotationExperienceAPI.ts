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

    //Div to use for text
    element: string;

    constructor(aViewPortSize: number) {
        const that = this;
        this.viewPortSize = aViewPortSize;
        this.element = "text";

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
                that.sendDocumentMessageToServer();
            }

            // ---------- NEW VIEWPORT --------- //
            if (elem.className === 'fas fa-step-forward') {
                that.sendViewportMessageToServer(that.viewPortEnd + 1, that.viewPortEnd + that.viewPortSize);
            }

            if (elem.className === 'fas fa-step-backward') {
                that.sendViewportMessageToServer(that.viewPortBegin - that.viewPortSize, that.viewPortBegin - 1);
            }

            //aBegin = null, aEnd = size
            if (elem.className === 'fas fa-fast-forward') {
                that.sendViewportMessageToServer(-100, that.viewPortSize);
            }

            if (elem.className === 'fas fa-fast-backward') {
                that.sendViewportMessageToServer(0, that.viewPortSize - 1);
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

    setVisibleText(elementId: string)
    {
        let textDIV = document.getElementById(elementId.toString())
        //Reset previous text
        textDIV.innerHTML= '';
        let k = 0;

        for (let i = 0; i < this.viewPortSize; i++) {
            let words = this.text[i].split(" ");
            let sentence = document.createElement("div");
            sentence.className = "sentence";
            sentence.setAttribute("sentence-id", i.toString());

            for (let j = 0; j <= words.length; j++, k++) {
                if (j < words.length) {
                    let text_ = document.createElement("text");
                    text_.innerText = words[j]
                    text_.className = "word";
                    text_.setAttribute("word_id", k.toString());
                    sentence.appendChild(text_);

                    k++;
                    let spaceElement = document.createElement("text");
                    spaceElement.className = "space";
                    spaceElement.innerText = " ";
                    spaceElement.setAttribute("word_id", k.toString());
                    if (j != words.length - 1) {
                        sentence.appendChild(spaceElement);
                    }
                } else {
                    let fullStopElement = document.createElement("text");
                    fullStopElement.className = "stop";
                    fullStopElement.innerText = ".";
                    fullStopElement.setAttribute("word_id", k.toString());
                    sentence.appendChild(fullStopElement);
                }
            }
            textDIV.appendChild(sentence);
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
    sendDocumentMessageToServer()
    {
        let json = {
            username: this.username,
            project: this.projectID,
            viewPortSize: this.viewPortSize
        };
        this.stompClient.publish({destination: "/app/new_document_by_client", body: JSON.stringify(json)});
    }

    sendViewportMessageToServer(aBegin: number, aEnd: number)
    {
       if (aBegin < 0 && aBegin != -100) {
            aBegin = 0;
            aEnd = this.viewPortSize - 1;
        }

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
        this.setVisibleText(this.element);

        //Unsubscribe channels for previous document
        for (let i = this.viewPortBegin; i < this.viewPortBegin + this.viewPortSize; i++) {
            this.unsubscribe("annotation_update_" + i.toString());
        }

        this.viewPortBegin = 0;
        this.viewPortEnd = this.viewPortSize - 1;

        //Multiple subscriptions due to viewport
        for (let i = 0; i < this.viewPortSize; i++) {
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

        const that = this;


        //Parse data
        let keys = Object.keys(aMessage)
        let values = keys.map(k => aMessage[k])

        console.log(values[0])
        console.log(values[1])
        console.log(values[2])

        //Unsubscribe channels for previous document
        for (let i = this.viewPortBegin; i < this.viewPortBegin + this.viewPortSize; i++) {
            this.unsubscribe("annotation_update_" + i.toString());
        }

        this.viewPortBegin = values[0];
        this.viewPortEnd = values[1];
        this.text = values[2];
        this.setVisibleText(this.element);


        //Multiple subscriptions due to viewport
        for (let i = this.viewPortBegin; i < this.viewPortBegin + this.viewPortSize; i++) {
            this.stompClient.subscribe("/topic/annotation_update_for_clients/" + this.projectID + "/" + this.documentID + "/" + i, function (msg) {
                that.receiveAnnotationMessageByServer(JSON.parse(msg.body));
            }, {id: "annotation_update_" + i});
        }

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
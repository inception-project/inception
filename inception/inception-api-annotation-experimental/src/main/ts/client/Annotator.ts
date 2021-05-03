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

class Annotator
{
    stompClient : Client;
    events: [];
    connected : boolean = false;

    constructor()
    {
        let that = this;

        if (document.getElementById('connect-button') == null) {
            setTimeout(function(){
                document.getElementById('connect-button').addEventListener("click", (e:Event) =>
                    that.connect());
                document.getElementById('disconnect-button').addEventListener("click", (e: Event) =>
                    that.disconnect());
                document.getElementById('send-to-server').addEventListener("click", (e: Event) =>
                    that.sendMessageToServer());
            }, 1000);
        } else {
            document.getElementById('connect-button').addEventListener("click", (e:Event) =>
                that.connect());
            document.getElementById('disconnect-button').addEventListener("click", (e: Event) =>
                that.disconnect());
            document.getElementById('disconnect-button').addEventListener("click", (e: Event) =>
                that.sendMessageToServer());
        }

    }

    connect()
    {
        if (this.connected) {
            console.log("You are already connected")
            return;
        }

        let url : string = (window.location.protocol.startsWith("https") ? "wss://" : "ws://")
            + window.location.host
            + "/inception_app_webapp_war_exploded/ws";

        //this.connection = new WebSocket(url);
        this.stompClient = Stomp.over(function() {
            return new WebSocket(url);
        });

        this.stompClient.onConnect = function (frame) {
            that.connected = true;

            that.stompClient.subscribe('/queue/selected_annotation', function (msg) {
                console.log('Received data2: ' + JSON.stringify(msg) + frame);
                that.events = JSON.parse(msg.body);
            });
        };

        this.stompClient.onStompError = function (frame) {
            console.log('Broker reported error: ' + frame.headers['message']);
            console.log('Additional details: ' + frame.body);
        };
        const that = this;
        this.stompClient.activate();
    }

    disconnect()
    {
        if (this.connected) {
            console.log("Disconnecting now");
            this.connected = false;
            this.stompClient.deactivate();
        }
    }

    //Event handling

    // ---------------- SEND ------------------------- //

    sendMessageToServer()
    {
        this.stompClient.publish({destination: "/app/select_annotation_by_client", body:"TEST_TEXT"});
    }

    // ---------------- RECEIVE ---------------------- //

    receiveServerMessage(serverMessage: String)
    {
        switch (serverMessage) {
            case "newAnnotationForClient":
                console.log("Server Message __ New Annotation for Client: ")
                break;
            case "deletedAnnotationForClient":
                console.log("Server Message __ Deleted Annotation for Client: ")
                break;
            case "newConnectedClientForClient":
                console.log("Server Message __ New Connected Client Message for Client: ")
                break;
            case "selectedAnnotationForClient":
                console.log("Server Message __ Selected Annotation for Client: ")
                break;

        }
    }

}

let annotator = new Annotator()
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

import {ClientMessage, ClientMessageConnectedClient, ServerMessage} from "../common/Data";
import {Client} from '@stomp/stompjs';

class Annotator
{
    stompClient : Client;
    events: [];
    connected : boolean = false;
    clientMsg : ClientMessageConnectedClient;

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
                    that.sendMessageToServer(that.clientMsg));
            }, 1000);
        } else {
            document.getElementById('connect-button').addEventListener("click", (e:Event) =>
                that.connect());
            document.getElementById('disconnect-button').addEventListener("click", (e: Event) =>
                that.disconnect());
            document.getElementById('disconnect-button').addEventListener("click", (e: Event) =>
                that.sendMessageToServer(null));
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
        this.stompClient = new Client( {
            brokerURL: url
        });

        this.stompClient.onConnect = function (frame) {
            that.connected = true;
            console.log("CONNECTED!, Frame: " + frame);

            that.stompClient.subscribe('/app', function (msg) {
                console.log('Received initial data: ' + JSON.stringify(msg));
                that.events = JSON.parse(msg.body);
            });
            that.stompClient.subscribe('/topic', function (msg) {
                console.log('Received: ' + JSON.stringify(msg));
                that.events.unshift(JSON.parse(msg.body));
                that.events.pop();
            });
            // Do something, all subscribes must be done is this callback
            // This is needed because this will be executed after a (re)connect
        };

        this.stompClient.onStompError = function (frame) {
            // Will be invoked in case of error encountered at Broker
            // Bad login/passcode typically will cause an error
            // Complaint brokers will set `message` header with a brief message. Body may contain details.
            // Compliant brokers will terminate the connection after any error
            console.log('Broker reported error: ' + frame.headers['message']);
            console.log('Additional details: ' + frame.body);
        };
        const that = this;
        this.stompClient.activate();
        /*
        this.stompClient.conn({}, function (frame)
        {


            that.connection.addEventListener('message', () => {
                console.log("MESSAGE")
                });
            that.connection.addEventListener('open', () => {
                console.log("OPEN")
            });
            that.connection.addEventListener('close', () => {
                console.log("CLOSE")
            });
        })

         */
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

    sendMessageToServer(msg: ClientMessage)
    {
        console.log("Should be sending now: " + msg)
      //  this.stompClient.send(JSON.stringify(msg));
    }

    // ---------------- RECEIVE ---------------------- //

    receiveServerMessage(serverMessage: ServerMessage)
    {
        switch (serverMessage.type) {
            case "newAnnotationForClient":
                console.log("Server Message __ New Annotation for Client (" + this.connection + "): " + serverMessage)
                break;
            case "deletedAnnotationForClient":
                console.log("Server Message __ Deleted Annotation for Client (" + this.connection + "): " + serverMessage)
                break;
            case "newConnectedClientForClient":
                console.log("Server Message __ New Connected Client Message for Client (" + this.connection + "): " + serverMessage)
                break;
            case "selectedAnnotationForClient":
                console.log("Server Message __ Selected Annotation for Client (" + this.connection + "): " + serverMessage)
                break;

        }
    }

}

let annotator = new Annotator()
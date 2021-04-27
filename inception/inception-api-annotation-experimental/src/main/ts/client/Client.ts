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

import {ClientMessage, ServerMessage} from "../common/Data";

export class Client
{

    connection: WebSocket

    constructor()
    {
        let url : string = (window.location.protocol.startsWith("https") ? "wss://" : "ws://") + window.location.host;
        this.connection = new WebSocket(url);

        //Required due to JS scope
        const that = this;

        this.connection.onopen = function () {
            console.log("Client connection of is now OPEN");
        }

        this.connection.onclose = function () {
            console.log("Client connection is now CLOSED");
        }

        this.connection.onmessage = function (msg) {
            try {
                that.receiveServerMessage(JSON.parse(msg.data))
            } catch (exception) {
                console.log("Message is not valid JSON: " + msg.data);
            }

        }
    }

    //Event handling

    // ---------------- SEND ------------------------- //

    sendMessageToServer(msg: ClientMessage)
    {
        this.connection.send(JSON.stringify(msg));
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

const client = new Client();
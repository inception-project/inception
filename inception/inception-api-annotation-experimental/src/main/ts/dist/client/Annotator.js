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
import SockJS from 'sockjs-client';
import Stomp from 'stompjs';
class Annotator {
    constructor() {
        this.connected = false;
        let that = this;
        if (document.getElementById('connect-button') == null) {
            setTimeout(function () {
                document.getElementById('connect-button').addEventListener("click", (e) => that.connect());
                document.getElementById('disconnect-button').addEventListener("click", (e) => that.disconnect());
                document.getElementById('send-to-server').addEventListener("click", (e) => that.sendMessageToServer(that.clientMsg));
            }, 2000);
        }
        else {
            document.getElementById('connect-button').addEventListener("click", (e) => that.connect());
            document.getElementById('disconnect-button').addEventListener("click", (e) => that.disconnect());
            document.getElementById('disconnect-button').addEventListener("click", (e) => that.sendMessageToServer(null));
        }
    }
    connect() {
        if (this.connected) {
            console.log("You are already connected");
            return;
        }
        let url = (window.location.protocol.startsWith("https") ? "wss://" : "ws://")
            + window.location.host
            + "/inception_app_webapp_war_exploded/ws-endpoint";
        this.connection = new SockJS('/ws-endpoint');
        this.stompClient = Stomp.over(this.connection);
        const that = this;
        this.stompClient.connect({}, function (frame) {
            console.log('Connected: ' + frame);
            that.connected = true;
            that.stompClient.subscribe('/user/queue/errors', function (msg) {
                console.error('Websocket server error: ' + JSON.stringify(msg));
            });
            that.stompClient.subscribe('/app', function (msg) {
                console.log('Received initial data: ' + JSON.stringify(msg));
                that.events = JSON.parse(msg.body);
            });
            that.stompClient.subscribe('/topic', function (msg) {
                console.log('Received: ' + JSON.stringify(msg));
                that.events.unshift(JSON.parse(msg.body));
                that.events.pop();
            });
            that.connection.addEventListener('message', () => {
                console.log("MESSAGE");
            });
            that.connection.addEventListener('open', () => {
                console.log("OPEN");
            });
            that.connection.addEventListener('close', () => {
                console.log("CLOSE");
            });
        });
    }
    disconnect() {
        if (this.connected) {
            console.log("Annotator '" + "' disconnecting now");
            this.connection.close();
            this.connected = false;
        }
    }
    //Event handling
    // ---------------- SEND ------------------------- //
    sendMessageToServer(msg) {
        this.connection.send(JSON.stringify(msg));
    }
    // ---------------- RECEIVE ---------------------- //
    receiveServerMessage(serverMessage) {
        switch (serverMessage.type) {
            case "newAnnotationForClient":
                console.log("Server Message __ New Annotation for Client (" + this.connection + "): " + serverMessage);
                break;
            case "deletedAnnotationForClient":
                console.log("Server Message __ Deleted Annotation for Client (" + this.connection + "): " + serverMessage);
                break;
            case "newConnectedClientForClient":
                console.log("Server Message __ New Connected Client Message for Client (" + this.connection + "): " + serverMessage);
                break;
            case "selectedAnnotationForClient":
                console.log("Server Message __ Selected Annotation for Client (" + this.connection + "): " + serverMessage);
                break;
        }
    }
}
let annotator = new Annotator();
//# sourceMappingURL=Annotator.js.map
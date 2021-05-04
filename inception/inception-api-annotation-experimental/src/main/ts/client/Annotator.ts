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

import {Client, Frame, Stomp} from '@stomp/stompjs';

class Annotator
{
    stompClient : Client;
    connected : boolean = false;

    constructor()
    {
        let that = this;

        // DEFINE ACTIONS / EVENTS

        // ------------------------- SHOWCASE ONLY --------------------------- //

        if (document.getElementById('connect-button') == null) {
            setTimeout(function(){
                document.getElementById('connect-button').addEventListener("click", (e:Event) =>
                    that.connect());
                document.getElementById('disconnect-button').addEventListener("click", (e: Event) =>
                    that.disconnect());
                document.getElementById('send-to-server').addEventListener("click", (e: Event) =>
                    that.sendSelectAnnotationMessageToServer());
            }, 1000);
        } else {
            document.getElementById('connect-button').addEventListener("click", (e:Event) =>
                that.connect());
            document.getElementById('disconnect-button').addEventListener("click", (e: Event) =>
                that.disconnect());
            document.getElementById('disconnect-button').addEventListener("click", (e: Event) =>
                that.sendSelectAnnotationMessageToServer());
        }
    }


    //CREATE WEBSOCKET CONNECTION (WILL BE IN DEFAULT CONSTRUCTOR)
    connect()
    {
        if (this.connected) {
            console.log("You are already connected")
            return;
        }

        let url : string = (window.location.protocol.startsWith("https") ? "wss://" : "ws://")
            + window.location.host
            + "/inception_app_webapp_war_exploded/ws";

        this.stompClient = Stomp.over(function() {
            return new WebSocket(url);
        });

        //REQUIRED DUE TO JS SCOPE
        const that = this;

        this.stompClient.onConnect = function (frame) {
            that.connected = true;


            // ------ DEFINE ALL SUBSCRIPTION CHANNELS WITH ACTIONS ------ //

            that.stompClient.subscribe('/queue/selected_annotation_for_client', function (msg) {
                that.receiveSelectedAnnotationMessageByServer(JSON.parse(msg.body), frame);
            });

            that.stompClient.subscribe('/topic/new_annotation_for_client', function (msg) {
                that.receiveNewAnnotationMessageByServer(JSON.parse(msg.body), frame);
            });

            that.stompClient.subscribe('/topic/delete_annotation_for_client', function (msg) {
                that.receiveDeleteAnnotationMessageByServer(JSON.parse(msg.body), frame);
            });

            // ------------------------------------------------------------ //

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


    /** ----------- Event handling ------------------ **/

    // ---------------- SEND ------------------------- //

    sendSelectAnnotationMessageToServer()
    {
        this.stompClient.publish({destination: "/app/select_annotation_by_client", body:"SELECT"});
        this.stompClient.publish({destination: "/app/new_annotation_by_client", body:"NEW"});
        this.stompClient.publish({destination: "/app/delete_annotation_by_client", body:"DELETE"});
    }

    sendCreateAnnotationMessageToServer()
    {
        this.stompClient.publish({destination: "/app/new_annotation_by_client", body:"NEW"});
    }

    sendDeleteAnnotationMessageToServer()
    {
        this.stompClient.publish({destination: "/app/delete_annotation_by_client", body:"DELETE"});
    }

    // ------------------------------------------------ //


    // ---------------- RECEIVE ----------------------- //

    receiveSelectedAnnotationMessageByServer(aMessage : string, aFrame : Frame)
    {
        console.log('RECEIVED SELECTED ANNOTATION: ' + JSON.stringify(aMessage) + aFrame);
    }

    receiveNewAnnotationMessageByServer(aMessage : string, aFrame : Frame)
    {
        console.log('RECEIVED NEW ANNOTATION: ' + JSON.stringify(aMessage) + aFrame);
    }

    receiveDeleteAnnotationMessageByServer(aMessage : string, aFrame : Frame)
    {
        console.log('RECEIVED DELETE ANNOTATION: ' + JSON.stringify(aMessage) + aFrame);
    }


    /** ---------------------------------------------- **/
}

let annotator = new Annotator()
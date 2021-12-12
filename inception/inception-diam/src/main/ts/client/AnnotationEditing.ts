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
import { Client, Stomp, StompSubscription, IFrame, ActivationState, frameCallbackType } from '@stomp/stompjs';
import { Viewport } from './newmodel/Viewport';
import * as jsonpatch from 'fast-json-patch';
import { timeStamp } from 'console';

declare var Wicket: any;

/**
 * This callback will accept the annotation data.
 */
export declare type dataCallback = (data: Viewport) => void;

export class AnnotationEditing {

    private ajaxEndpoint: string;
    private stompClient: Client;
    private webSocket: WebSocket;
    private initSubscription: StompSubscription;
    private updateSubscription: StompSubscription;

    private data: Viewport;
    private diff: any;

    public onConnect: frameCallbackType;

    connect(aWsEndpoint: string, aAjaxEndpoint: string) {
        if (this.stompClient) {
            throw "Already connected";
        }

        this.ajaxEndpoint = aAjaxEndpoint;

        let protocol = (window.location.protocol === 'https:' ? 'wss:' : 'ws:');
        let wsEndpoint = new URL(aWsEndpoint)
        wsEndpoint.protocol = protocol;

        this.stompClient = Stomp.over(() => this.webSocket = new WebSocket(wsEndpoint.toString()));
        this.stompClient.reconnectDelay = 5000;

        this.stompClient.onConnect = frame => {
            this.stompClient.subscribe("/user/queue/errors", this.handleProtocolError);
            if (this.onConnect) {
                this.onConnect(frame);
            }
        }

        this.stompClient.onStompError = this.handleBrokerError;

        this.stompClient.activate();
    }

    disconnect() {
        this.stompClient.deactivate();
        this.webSocket.close();
    }

    select(vid: string) {
        Wicket.Ajax.ajax({
            "m": "GET",
            // "c": dispatcher.wicketId,
            "u": this.ajaxEndpoint,
            "ep": {
                "action": "selectAnnotation",
                "id": vid
            },
            // success
            "sh": [function () { }],
            // error
            "fh": [function () { }]
        });
    }

    private handleBrokerError(receipt: IFrame) {
        console.log('Broker reported error: ' + receipt.headers['message']);
        console.log('Additional details: ' + receipt.body);
    }

    private handleProtocolError(msg) {
        console.log(msg);
    }

    subscribeToViewport(aViewportTopic: string, callback: dataCallback) {
        this.unsubscribeFromViewport();
        this.initSubscription = this.stompClient.subscribe('/app' + aViewportTopic, msg => {
            this.data = JSON.parse(msg.body);
            this.diff = null;
            callback(this.data);
        });
        this.updateSubscription = this.stompClient.subscribe('/topic' + aViewportTopic, msg => {
            var update = JSON.parse(msg.body);
            this.data = jsonpatch.applyPatch(this.data, update.diff).newDocument;
            this.diff = update.diff;
            callback(this.data);
        });
    }

    unsubscribeFromViewport() {
        if (this.initSubscription) {
            this.initSubscription.unsubscribe();
        }
        if (this.updateSubscription) {
            this.updateSubscription.unsubscribe();
        }
    }
}


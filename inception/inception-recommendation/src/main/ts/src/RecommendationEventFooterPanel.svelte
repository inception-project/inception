<script lang="ts">
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

    import { onMount, onDestroy } from "svelte"
    import { get_current_component } from 'svelte/internal'
    import { Client, Stomp, IFrame } from "@stomp/stompjs"

    interface RRecommenderLogMessage {
        level: "INFO" | "WARN" | "ERROR"
        message: string
    }

    export let wsEndpointUrl: string; // should this be full ws://... url
    export let topicChannel: string;
    export let feedbackPanelId = null;

    let socket: WebSocket = null;
    let stompClient: Client = null;
    let connected = false;
    let subscription = null;
    let feedbackPanelExtension = new FeedbackPanelExtension(feedbackPanelId);
    let element = null;
    let self = get_current_component()

    export function connect(): void {
        if (connected) return;

        let protocol = window.location.protocol === "https:" ? "wss:" : "ws:";
        let wsEndpoint = new URL(wsEndpointUrl);
        wsEndpoint.protocol = protocol;

        stompClient = Stomp.over(() => (socket = new WebSocket(wsEndpoint.toString())));
        stompClient.onConnect = () => onConnect();
        stompClient.onStompError = handleBrokerError;
        stompClient.activate();
    }

    export function onConnect() {
        connected = true;
        subscription = stompClient.subscribe(
            "/user/queue/errors",
            function (msg) {
                console.error(
                    "Websocket server error: " + JSON.stringify(msg.body)
                );
            }
        );
        stompClient.subscribe("/topic" + topicChannel, (msg) => messageRecieved(msg));
    }

    export function messageRecieved(msg) {
        if (!document.body.contains(element)) {
            console.debug("Element is not part of the DOM anymore. Disconnecting and suiciding.")
            self.$destroy()
            return;
        }

        var msgBody = JSON.parse(msg.body) as RRecommenderLogMessage;
        switch (msgBody.level) {
            case "ERROR":
                feedbackPanelExtension.addErrorToFeedbackPanel(msgBody.message);
                break;
            case "WARN":
                feedbackPanelExtension.addEWarningToFeedbackPanel(
                    msgBody.message
                );
                break;
            case "INFO":
                feedbackPanelExtension.addInfoToFeedbackPanel(msgBody.message);
                break;
            default:
                break;
        }
    }

    export function disconnect() {
        subscription.unsubscribe();
        stompClient.deactivate();
        socket.close();
        connected = false;
    }

    function handleBrokerError(receipt: IFrame) {
        console.log("Broker reported error: " + receipt.headers.message);
        console.log("Additional details: " + receipt.body);
    }

    onMount(async () => {
        connect();
    });

    onDestroy(async () => {
        disconnect();
    });
</script>

<div bind:this={element} />

<style>
</style>

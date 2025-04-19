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
    import { Client, Stomp, StompSubscription } from "@stomp/stompjs"

    interface RRecommenderLogMessage {
        level: "INFO" | "WARN" | "ERROR"
        message: string
        addClasses: string[]
        removeClasses: string[]
    }

    interface Props {
        wsEndpointUrl: string; // should this be full ws://... url
        topicChannel: string;
        feedbackPanelId?: any;
        csrfToken: string;
    }

    let {
        wsEndpointUrl,
        topicChannel,
        feedbackPanelId = null,
        csrfToken
    }: Props = $props();

    let socket: WebSocket | null = null;
    let stompClient: Client | null = null;
    let connected = false;
    let subscription: StompSubscription | null = null;
    let feedbackPanelExtension = new FeedbackPanelExtension(feedbackPanelId);
    let element: Element | null = $state(null);

    export function connect(): void {
        if (connected) return;

        let protocol = window.location.protocol === "https:" ? "wss:" : "ws:";
        let wsEndpoint = new URL(wsEndpointUrl);
        wsEndpoint.protocol = protocol;

        stompClient = Stomp.over(() => (socket = new WebSocket(wsEndpoint.toString())));
        stompClient.connectHeaders = {
            'X-CSRF-TOKEN': csrfToken
        } 
        stompClient.onConnect = () => onConnect();
        stompClient.onStompError = handleBrokerError;
        stompClient.activate();
    }

    export function onConnect() {
        if (!stompClient) return;

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

    export function messageRecieved(msg: any) {
        if (!element) return;

        if (!document.body.contains(element)) {
            console.debug("Element is not part of the DOM anymore. Disconnecting and suiciding.")
            disconnect()
            return;
        }

        var msgBody = JSON.parse(msg.body) as RRecommenderLogMessage;
        // console.log(msgBody)
        msgBody.removeClasses?.forEach(c => document.body.classList.remove(c))
        msgBody.addClasses?.forEach(c => document.body.classList.add(c))
        switch (msgBody.level) {
            case "ERROR":
                feedbackPanelExtension.addErrorToFeedbackPanel(msgBody.message);
                break;
            case "WARN":
                feedbackPanelExtension.addEWarningToFeedbackPanel(msgBody.message);
                break;
            case "INFO":
                feedbackPanelExtension.addInfoToFeedbackPanel(msgBody.message);
                break;
            default:
                break;
        }
    }

    export function disconnect() {
        if (subscription) subscription.unsubscribe();
        if (stompClient) stompClient.deactivate();
        if (socket) socket.close();
        connected = false;
        subscription = null;
        stompClient = null;
        socket = null;
    }

    function handleBrokerError(receipt: any) {
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

<div bind:this={element}>
</div>

<style>
</style>

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

    import { onMount, onDestroy } from "svelte";
    import { Client, Stomp, type IFrame } from "@stomp/stompjs";
    import { marked } from "marked";
    import { factory } from "@inception-project/inception-diam";
    import DOMPurify from "dompurify";
    import { assistantState } from "./AssistantState.svelte";

    interface MPerformanceMetrics {
        duration: number;
        tokens: number;
    }

    interface MReference {
        id: string;
        counter: number;
        documentId: number;
        documentName: string;
        begin: number;
        end: number;
        score: number;
    }

    interface MChatMessage {
        id: string;
        role: string;
        actor?: string;
        internal: boolean;
        performance?: MPerformanceMetrics;
        context?: string;
        references?: MReference[];
    }

    interface MTextMessage extends MChatMessage {
        thinking: string;
        message: string;
        done: boolean;
    }

    interface MCallResponse extends MChatMessage {
        toolName: string;
        arguments: any;
        payload: any;
    }

    interface Props {
        ajaxEndpointUrl?: string;
        wsEndpointUrl: string; // should this be full ws://... url
        csrfToken: string;
        topicChannel: string;
        dataOwner: string;
        documentId: number;
    }

    let {
        ajaxEndpointUrl,
        wsEndpointUrl,
        csrfToken,
        topicChannel,
        dataOwner,
        documentId
    }: Props = $props();

    let socket: WebSocket = null;
    let stompClient: Client = null;
    let ajaxClient = factory().createAjaxClient(ajaxEndpointUrl);
    let connected = false;
    let subscription = null;

    let element = null;
    let chatContainer = null;
    let autoScroll = true;

    let messages : MChatMessage[] = $state([]);
    let messageInput;
    let waitingForResponse = $state(false);

    let speechAvailable = "speechSynthesis" in window;
    let utteranceBuffer = "";
    let utteranceQueue: SpeechSynthesisUtterance[] = [];
    let isSpeaking = false;

    // Our canonical reference format
    const refIdReplacementPattern = /\s*{{ref::([\w-]+)}}(\.*)/g

    // Some models (deepseek-r1) can't be bothered to properly use our reference syntax
    // and keep referring to documents using the "Document XXXXXXXX" syntax...
    const docIdReplacementPattern = /\s*[Dd]ocument[\s,]+([0-9a-f]{8})(\.*)/g

    marked.setOptions({
        breaks: true,
        gfm: true,
        async: false,
    });

    const userPreferencesKey = "assistant/general";
    const defaultPreferences = {
        speechSynthesisEnabled: false,
    };
    let preferences = Object.assign({}, defaultPreferences);

    ajaxClient.loadPreferences(userPreferencesKey).then((p) => {
        preferences = Object.assign(preferences, defaultPreferences, p);
        console.log("Loaded preferences", preferences);

        assistantState.speechSynthesisEnabled = 
            preferences.speechSynthesisEnabled ?? defaultPreferences.speechSynthesisEnabled;
    });

    $effect(() => { 
        preferences.speechSynthesisEnabled = assistantState.speechSynthesisEnabled;
        ajaxClient.savePreferences(userPreferencesKey, preferences);
    });

    $effect(() => {
        if (!assistantState.speechSynthesisEnabled) {
            speechSynthesis.cancel;
        }
    })    

    export function connect(): void {
        if (connected) return;

        let protocol = window.location.protocol === "https:" ? "wss:" : "ws:";
        let wsEndpoint = new URL(wsEndpointUrl);
        wsEndpoint.protocol = protocol;

        stompClient = Stomp.over(
            () => (socket = new WebSocket(wsEndpoint.toString())),
        );
        stompClient.connectHeaders = {
            "X-CSRF-TOKEN": csrfToken,
        };
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
                    "Websocket server error: " + JSON.stringify(msg.body),
                );
            },
        );
        stompClient.subscribe("/app" + topicChannel, (msg) => onMessage(msg));
        stompClient.subscribe("/topic" + topicChannel, (msg) => onMessage(msg));
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

    export function onMessage(msg) {
        if (!document.body.contains(element)) {
            console.debug(
                "Element is not part of the DOM anymore. Disconnecting and suiciding.",
            );
            disconnect();
            return;
        }

        var msgBody = JSON.parse(msg.body);

        if (!(msgBody instanceof Array)) {
            msgBody = [msgBody];
        }

        msgBody.forEach((incomingMessage) => {
            dispatchMessage(incomingMessage);
        });
    }

    function dispatchMessage(incomingMessage: any) {
        const type = incomingMessage["@type"];
        console.log(
            `Received message of type ${type} with id ${incomingMessage.id}`,
        );
        if (type === "textMessage") {
            onTextMessage(incomingMessage);
        } else if (type === "callResponse") {
            onCallResponse(incomingMessage);
        } else if (type === "clearCmd") {
            onClearCommand();
        }
    }

    function onClearCommand() {
        messages = [];
    }

    function onTextMessage(msg: MTextMessage) {
        if (waitingForResponse && msg.role === "assistant" && !msg.internal) {
            waitingForResponse = false;
        }

        const index = messages.findIndex((message) => message.id === msg.id);

        // If message is new, add it
        if (index === -1) {
            console.log(
                `Starting message ${msg.id} with message fragment: ${msg.thinking || msg.message}`,
            );
            if (msg.context) {
                // Insert before the message with id == msg.context
                const ctxIndex = messages.findIndex((m) => m.id === msg.context);
                if (ctxIndex !== -1) {
                    messages = [
                        ...messages.slice(0, ctxIndex),
                        msg,
                        ...messages.slice(ctxIndex),
                    ];
                } else {
                    // If context id not found, just append
                    messages = [...messages, msg];
                }
            } else {
                    // If no context id also just append
                messages = [...messages, msg];
            }

            if (
                msg.role == "assistant" &&
                msg.message &&
                !msg.internal &&
                !msg.done
            ) {
                utteranceBuffer = msg.message;
            }
            return;
        }

        // Merge with existing message
        console.log(
            `Merging message ${msg.id} with message fragment: ${msg.thinking || msg.message}`,
        );
        messages = [
            ...messages.slice(0, index),
            {
                ...messages[index],
                message: (messages[index].message || "") + (msg.message || ""),
                thinking: (messages[index].thinking || "") + (msg.thinking || ""),
                references: [
                    ...(messages[index].references || []),
                    ...(msg.references || []).filter(
                        (newRef) => !(messages[index].references || []).some((existingRef) => existingRef.id === newRef.id)
                    )
                ],
                performance: msg.performance,
                done: msg.done,
            },
            ...messages.slice(index + 1),
        ];

        if (
            speechAvailable &&
            msg.role == "assistant" &&
            msg.message &&
            !msg.internal
        ) {
            speak(msg);
        }

        if (msg.role == "assistant" && msg.done) {
            waitingForResponse = false;
        }
    }

    function onCallResponse(msg: MCallResponse) {
        if (msg.context) {
            // Insert before the message with id == msg.context
            const ctxIndex = messages.findIndex((m) => m.id === msg.context);
            if (ctxIndex !== -1) {
                messages = [
                    ...messages.slice(0, ctxIndex),
                    msg,
                    ...messages.slice(ctxIndex),
                ];
            } else {
                // If context id not found, just append
                messages = [...messages, msg];
            }
        } else {
                // If no context id also just append
            messages = [...messages, msg];
        }
    }

    function speak(msg: MTextMessage) {
        utteranceBuffer += msg.message;

        // Remove references (we don't want to speak them)
        utteranceBuffer = utteranceBuffer.replace(/{{ref::([\w-]+)}}/g, "");

        if (msg.done) {
            enqueueUtterance(utteranceBuffer);
            utteranceBuffer = "";
            return;
        }

        // Speak when sentence seems complete (we don't handle abbreviations)
        const trimmedBuffer = utteranceBuffer.trimEnd()
        const lastChar = trimmedBuffer.charAt(trimmedBuffer.length - 1);
        // console.log(`Checking if utterance ending in [${lastChar}] is complete: [${utteranceBuffer}]`);
        if ([".", "!", "?", ":", ";"].includes(lastChar)) {
            // console.log(`Enqueuing utterance at sentence end: [${utteranceBuffer}]`);
            enqueueUtterance(utteranceBuffer);
            utteranceBuffer = "";
        } else {
            // Speak line by line
            let lineBreak = utteranceBuffer.indexOf("\n");
            if (lineBreak > 0) {
                // console.log(`Enqueuing utterance at line end: [${utteranceBuffer}]`);
                enqueueUtterance(utteranceBuffer.substring(0, lineBreak));
                utteranceBuffer = utteranceBuffer.substring(lineBreak);
            }
        }
    }

    function enqueueUtterance(text: string) {
        if (!assistantState.speechSynthesisEnabled) {
            return;
        }

        utteranceQueue.push(new SpeechSynthesisUtterance(text));
        processUtteranceQueue();
    }

    function processUtteranceQueue() {
        if (isSpeaking) {
            console.log("Speech synthesis is already in progress.");
            return;
        }

        if (utteranceQueue.length === 0) {
            console.log("Speech synthesis queue is empty.");
            return;
        }

        const utterance = utteranceQueue.shift();
        isSpeaking = true;
        speechSynthesis.speak(utterance);
        console.log("Speaking: " + utterance.text);

        utterance.onend = () => {
            isSpeaking = false;
            processUtteranceQueue();
        };
    }

    function toggleSpeechSynthesis() {
        assistantState.speechSynthesisEnabled = !assistantState.speechSynthesisEnabled
    }

    function handleScroll() {
        let threshold = 16;
        autoScroll =
            chatContainer.scrollTop + chatContainer.clientHeight >=
            chatContainer.scrollHeight - threshold;
    }

    function copyToClipboard(message: MTextMessage) {
        let usedReferences = {};
        let text = message.message.replace(
            refIdReplacementPattern,
            (match: string, refId: string, dots: string) => {
                const refSelector = (ref) => ref.id === refId;
                const reference = message.references.find(refSelector);
                const refNum = message.references.findIndex(refSelector) + 1;

                if (reference) {
                    usedReferences[refNum] = reference;
                    return `[^${refNum}]`;
                }

                return match;
            },
        );

        if (Object.keys(usedReferences).length > 0) {
            text += "\n\nReferences:";
        }

        for (let refNum in usedReferences) {
            const reference = usedReferences[refNum];
            text += `\n[^${refNum}]: ${reference.documentName} (score: ${reference.score.toFixed(4)})`;
        }

        navigator.clipboard.writeText(text).then(
            () => {
                console.log("Copied to clipboard successfully!");
            },
            (err) => {
                console.error("Could not copy text: ", err);
            }
        );
    }

    onMount(async () => {
        connect();
    });

    onDestroy(async () => {
        disconnect();
    });

    $effect(() => {
        messages; // Just to trigger the effect
        if (autoScroll && chatContainer) {
            chatContainer.scrollTop = chatContainer.scrollHeight;
        }
    });

    function renderThinking(message: MTextMessage) {
        if (!message?.thinking) {
            return "";
        }

        const trimmedMessage = message.thinking.replace(/{{ref::[\w-]*}?$/, "");

        const rawHtml = marked(trimmedMessage) as string;
        var pureHtml = DOMPurify.sanitize(rawHtml, { RETURN_DOM: false });

        // Replace all references with the respective reference link
        pureHtml = replaceReferencesWithHtmlLinks(message, pureHtml, refIdReplacementPattern);
        pureHtml = replaceReferencesWithHtmlLinks(message, pureHtml, docIdReplacementPattern);

        return pureHtml;
    }

    function renderContent(message: MTextMessage) {
        if (!message?.message) {
            return "";
        }

        const trimmedMessage = message.message.replace(/{{ref::[\w-]*}?$/, "");

        const rawHtml = marked(trimmedMessage) as string;
        var pureHtml = DOMPurify.sanitize(rawHtml, { RETURN_DOM: false });

        // Replace all references with the respective reference link
        pureHtml = replaceReferencesWithHtmlLinks(message, pureHtml, refIdReplacementPattern);
        pureHtml = replaceReferencesWithHtmlLinks(message, pureHtml, docIdReplacementPattern);

        return pureHtml;
    }

    function renderCallArguments(message: MCallResponse) {
        if (!message?.arguments) {
            return "no arguments";
        }

        return JSON.stringify(message.arguments, null, 2);
    }

    function renderCallPayload(message: MCallResponse) {
        if (!message?.payload) {
            return "no payload";
        }

        return JSON.stringify(message.payload, null, 2);
    }

    function replaceReferencesWithHtmlLinks(message, text, pattern) {
        return text.replace(
            pattern,
            (match: string, refId: string, dots: string) => {
                const refSelector = (ref) => ref.id === refId;
                const reference = message.references.find(refSelector);
                const refNum = message.references.findIndex(refSelector) + 1;

                if (reference) {
                    return `${dots}<span class="reference badge rounded-pill text-bg-secondary mx-1" data-msg="${message.id}" data-ref="${reference.id}" title="${escapeXML(reference.documentName)} (score: ${reference.score.toFixed(4)})">${refNum}</span>`;
                }

                return match;
            },
        );
    }

    function escapeXML(str) {
        return str.replace(/[<>&'"]/g, (char) => {
            switch (char) {
                case "<":
                    return "&lt;";
                case ">":
                    return "&gt;";
                case "&":
                    return "&amp;";
                case "'":
                    return "&apos;";
                case '"':
                    return "&quot;";
                default:
                    return char;
            }
        });
    }

    function sendMessage(message: string, inputElement: HTMLInputElement) {
        if (stompClient && stompClient.connected) {
            // Cancel current speech synthesis and clear the utterance queue
            speechSynthesis.cancel();
            utteranceQueue = [];
            utteranceBuffer = "";
            isSpeaking = false;

            stompClient.publish({
                destination: "/app" + topicChannel,
                headers: {
                    user: dataOwner,
                    document: documentId
                },
                body: message,
            });
            inputElement.value = "";
            waitingForResponse = true;
            autoScroll = true;
        }
    }

    function handleKeyDown(event) {
        if (event.key === "Enter") {
            sendMessage(event.target.value, event.target);
            messageInput.value = "";
        }
    }

    function toggleCollapse(event) {
        const messageElement = event.currentTarget.parentElement;
        messageElement.classList.toggle("collapsed");
    }

    function handleClick(event) {
        const target = event.target;
        const msgId = target.getAttribute("data-msg");
        const refId = target.getAttribute("data-ref");
        if (msgId && refId) {
            event.preventDefault();
            const message = messages.find((msg) => msg.id === msgId);
            const reference = message.references.find(
                (ref) => ref.id === refId,
            );
            if (reference) {
                ajaxClient.scrollTo({
                    docId: reference.documentId,
                    offset: [reference.begin, reference.end],
                });
            }
        }
    }
</script>

<div bind:this={element} class="d-flex flex-column flex-content chat">
    <div
        class="scrolling flex-content px-3 py-1"
        bind:this={chatContainer}
        onscroll={handleScroll}
    >
        {#each messages as message}
            <!-- svelte-ignore a11y_click_events_have_key_events -->
            <!-- svelte-ignore a11y_no_noninteractive_tabindex -->
            <div
                class="message"
                data-id={message.id}
                data-role={message.role}
                data-internal={message.internal}
                class:collapsed={message.internal}
                tabIndex={message.internal ? 0 : undefined}
            >
                <div
                    class="message-header text-body-secondary"
                    onclick={(message.internal) ? toggleCollapse : null}
                    role={message.internal ? "button" : undefined}
                >
                    {#if message.role === "assistant"}
                        <i
                            class="fas fa-robot me-1"
                            title="Assistant message"
                        ></i>
                    {:else if message.role === "user"}
                        <i class="fas fa-user me-1" title="User message"></i>
                    {:else if message.role === "system"}
                        <i class="fas fa-cog me-1" title="System message"></i>
                    {:else if message.role === "tool"}
                        <i class="fas fa-hammer me-1" title="Tool"></i>
                    {/if}
                    {message.actor ? message.actor : message.role}
                    {#if !message.internal}
                        <!-- svelte-ignore a11y_consider_explicit_label -->
                        <button
                            class="btn btn-sm btn-link text-body-secondary float-end fw-lighter p-0 copy-button"
                            onclick={() => copyToClipboard(message)}
                        >
                            <i class="far fa-copy" title="Copy message"></i>
                        </button>
                    {/if}
                    {#if message.internal}
                        <span
                            class="mx-2 text-body-secondary float-end fw-lighter"
                        >
                            <i class="fas fa-info" title="Internal message"></i>
                        </span>
                    {/if}
                </div>
                <!-- svelte-ignore a11y_no_static_element_interactions -->
                {#if message["@type"] === "textMessage"}
                    {@const thinking = renderThinking(message)}
                    {#if thinking}
                        <div class="message-thinking collapsed">
                            <div class="message-thinking-header" onclick={toggleCollapse}>
                                Thinking...
                            </div>
                            <div class="message-thinking-body">
                                {@html thinking}
                            </div>
                        </div>
                    {/if}
                    <!-- svelte-ignore a11y_no_static_element_interactions -->
                    <div
                        class="message-body"
                        class:dots={!message.done}
                        onclick={handleClick}
                    >
                        {@html renderContent(message)}
                    </div>
                {:else if message["@type"] === "callResponse"}
                    <div class="message-body">
                        <strong>Called tool: {message.toolName}</strong>
                        <div>{renderCallArguments(message)}</div>
                        <div>{renderCallPayload(message)}</div>
                    </div>
                {:else}
                    <div class="message-body">
                        Unknown message type: {message["@type"]}
                    </div>
                {/if}
                {#if message.performance}
                    <div class="message-footer fw-ligher">
                        <small
                            ><i class="fas fa-pause me-1"></i>{(message.performance
                                .delay / 1000).toFixed(2)}s</small
                        >
                        <small
                            ><i class="far fa-clock ms-2 me-1"></i>{(message.performance
                                .duration / 1000).toFixed(2)}s</small
                        >
                        <small
                            ><i class="fas fa-stream ms-2 me-1"></i>{(
                                message.performance.tokens /
                                (message.performance.duration / 1000)
                            ).toFixed(2)}t/s</small
                        >
                    </div>
                {/if}
            </div>
        {/each}
        {#if waitingForResponse}
            <div class="message" data-role="assistant">
                <div class="message-body"><span class="dots"></span></div>
            </div>
        {/if}
    </div>
    <div class="px-3 py-1">
        {#if speechAvailable}
            <!-- svelte-ignore a11y_consider_explicit_label -->
            <button
                class="float-end btn btn-sm btn-link text-body-secondary"
                onclick={toggleSpeechSynthesis}
            >
                <i
                    class="fas"
                    class:fa-volume-up={assistantState.speechSynthesisEnabled}
                    class:fa-volume-mute={!assistantState.speechSynthesisEnabled}
                ></i>
            </button>
        {/if}
        <div class="message composer" data-role="user">
            <input
                type="text"
                placeholder="Type your message here..."
                bind:this={messageInput}
                onkeydown={handleKeyDown}
            />
        </div>
    </div>
</div>

<!-- svelte-ignore css_unused_selector -->
<style lang="scss">
    .chat {
        background-color: var(--bs-secondary-bg);
    }

    @keyframes dots {
        0%,
        20% {
            content: "";
        }
        40% {
            content: ".";
        }
        60% {
            content: "..";
        }
        80%,
        100% {
            content: "...";
        }
    }

    .dots::after {
        content: "";
        display: inline-block;
        animation: dots 1.5s steps(1, end) infinite;
    }

    .message {
        color: var(--bs-body-color);
        font-size: var(--bs-body-font-size);
        line-height: var(--bs-body-line-height);
        clear: both;
        padding: 8px;
        position: relative;
        margin: 8px 0;
        word-wrap: break-word;
        width: 100%;
        max-width: 100%;
        border-radius: 0.25em;

        &.composer {
            border-radius: 5px;

            input {
                border: none;
                background: transparent;
                width: 100%;
                padding: 8px;
                outline: none;
                border-radius: 5px;
                margin: 0;
                box-sizing: border-box;
            }
        }

        :global(.reference) {
            font-size-adjust: 0.5;
            //        font-size: x-small;
            vertical-align: top;
            border-radius: 0.25em;
            cursor: pointer;
        }

        .message-header {
            display: block;
            font-size: smaller;
            color: var(--bs-body-color-secondary);
        }

        .message-thinking {
            /* opacity: 0.7; */
            margin-bottom: 0.5em;

            .message-thinking-header {
                display: inline;
                cursor: pointer;
                font-size: 0.8em;
                border-radius: 0.5em;
                border: 1px solid var(--bs-border-color-translucent);
                padding: 0.05em 0.3em;
            }

            .message-thinking-body {
                padding-left: 0.5em;
                border-left: solid 2px var(--bs-border-color-translucent);
            }
        }

        .message-body, .message-thinking-body {
            display: block;
            font-size: smaller;

            :global(p) {
                margin-bottom: 0.5em;
            }

            :global(p:last-child) {
                margin-bottom: 0px;
            }

            :global(code) {
                white-space: break-spaces;
            }

            :global(h1),
            :global(h2),
            :global(h3),
            :global(h4),
            :global(h5),
            :global(h6) {
                font-size: var(--bs-body-font-size);
                line-height: var(--bs-body-line-height);
                font-weight: bolder;
                font-variant: small-caps;
            }

            :global(ol),
            :global(ul) {
                margin-bottom: 1.5em;
                padding-left: 1.5em;
            }
        }

        .message-footer {
            opacity: 0.5;
            display: block;
            font-size: x-small;
            padding-top: 0.25em;
            color: var(--bs-body-color-secondary);
        }

        &[data-role="user"] {
            background-color: var(--bs-info-bg-subtle);
        }

        &[data-role="assistant"] {
            background-color: var(--bs-success-bg-subtle);
        }

        &[data-role="tool"] .message-body {
            word-break: break-word;
        }

        &[data-internal="true"] {
            background-color: var(--bs-tertiary-bg);
            padding: 4px 8px;

            .message-body {
                font-size: smaller;
            }

            :global(pre) {
                margin-bottom: 0.2rem;
                background-color: #00000010;
                border: solid 1px;
                border-color: var(--bs-border-color-translucent);
                padding: 0.25rem;
            }

            :global(p:has(+ pre)) {
                margin-top: 0.5rem;
                margin-bottom: 0;
            }

            :global(code) {
                font-family: var(--bs-body-font-family);
            }
        }

        &.collapsed .message-body, .message-thinking.collapsed .message-thinking-body {
            display: none;
        }

        .copy-button {
            visibility: hidden
        }

        &:hover .copy-button {
            visibility: visible
        }
    }
</style>

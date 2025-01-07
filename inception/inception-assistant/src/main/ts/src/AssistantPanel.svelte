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

    import { onMount, onDestroy, afterUpdate } from "svelte"
    import { get_current_component } from 'svelte/internal'
    import { Client, Stomp, IFrame } from "@stomp/stompjs"
    import { marked } from 'marked'
    import { factory } from "@inception-project/inception-diam";
    import DOMPurify from 'dompurify'

    interface MPerformanceMetrics {
        duration: number,
    }

    interface MReference {
        id: string, 
        counter: number,
        documentId: number,
        documentName: string, 
        begin: number,
        end: number;
        score: number
    }

    interface MTextMessage {
        id: string,
        role: string,
        actor?: string,
        message: string,
        done: boolean,
        internal: boolean
        performance?: MPerformanceMetrics
        references?: MReference[]
    }

    export let wsEndpointUrl: string; // should this be full ws://... url
    export let topicChannel: string;
    export let csrfToken: string;
    export let ajaxEndpointUrl: string;

    let socket: WebSocket = null;
    let stompClient: Client = null;
    let connected = false;
    let subscription = null;
    let element = null;
    let chatContainer = null;
    let self = get_current_component()
    let messages = [] as MTextMessage[];
    let messageInput;
    let waitingForResponse = false;
    let autoScroll = true;
    let ajaxClient = factory().createAjaxClient(ajaxEndpointUrl);

    marked.setOptions({
        breaks: true,
        gfm: true, 
        async: false
    });

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
        connected = true;
        subscription = stompClient.subscribe(
            "/user/queue/errors",
            function (msg) {
                console.error(
                    "Websocket server error: " + JSON.stringify(msg.body)
                );
            }
        );
        stompClient.subscribe("/app" + topicChannel, (msg) => onMessageRecieved(msg));
        stompClient.subscribe("/topic" + topicChannel, (msg) => onMessageRecieved(msg));
    }

    export function onMessageRecieved(msg) {
        if (!document.body.contains(element)) {
            console.debug("Element is not part of the DOM anymore. Disconnecting and suiciding.")
            self.$destroy()
            return;
        }

        var msgBody = JSON.parse(msg.body);

        if (!(msgBody instanceof Array)) {
            msgBody = [msgBody];
        }

        msgBody.forEach(incomingMessage => {
            handleMessage(incomingMessage);
        });
    }

    function handleMessage(incomingMessage: any) {
        const type = incomingMessage['@type']
        if (type === "textMessage") {
            onTextMessage(incomingMessage);
        }
        else if (type === "clearCmd") {
            messages = []
        }
    }

    function onTextMessage(msg: MTextMessage) {
        if (waitingForResponse && msg.role === "assistant" && !msg.internal) {
            waitingForResponse = false
        }
        
        const index = messages.findIndex(message => message.id === msg.id)

        // If message is new, add it
        if (index === -1) {
            messages = [...messages, msg]
            return
        } 

        // If done, replace existing message
        if (msg.done) {
            messages = [
                ...messages.slice(0,index),
                msg,
                ...messages.slice(index+1)
            ]
            return
        }

        // Merge with existing message
        messages = [
            ...messages.slice(0,index),
            { ...messages[index],message: (messages[index].message || '') + (msg.message || '') },
            ...messages.slice(index+1)
        ];
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

    function handleScroll() {
        let threshold = 16;
        autoScroll = chatContainer.scrollTop + chatContainer.clientHeight >= chatContainer.scrollHeight - threshold;    }

    onMount(async () => {
        connect();
    });

    onDestroy(async () => {
        disconnect();
    });

    afterUpdate(() => {
        if (autoScroll) {
            chatContainer.scrollTop = chatContainer.scrollHeight;
        }
    });

    function renderMessage(message: MTextMessage) {
        if (!message?.message) {
            return '';
        }

        const trimmedMessage = message.message.replace(/{{ref::[\w-]*}?$/, '');;

        const rawHtml = marked(trimmedMessage) as string
        var pureHtml = DOMPurify.sanitize(rawHtml, { RETURN_DOM: false });
        var refNum = 0;

        // Replace all `{{ref::X}}` with the respective reference link
        pureHtml = pureHtml.replace(/\s*{{ref::([\w-]+)}}(\.*)/g, (match, refId, dots) => {
            const reference = message.references.find(ref => ref.id === refId);
            if (reference) {
                refNum++
                return `${dots}<span class="reference badge rounded-pill text-bg-secondary mx-1" data-msg="${message.id}" data-ref="${reference.id}" title="${escapeXML(reference.documentName)}">${refNum}</span>`;
            }

            // If no matching reference is found, keep the original text
            console.log(`Reference with id ${refId} not found in message ${message.id}`);
            return match;
        });

        return pureHtml;
    }

    function escapeXML(str) {
        return str.replace(/[<>&'"]/g, (char) => {
            switch (char) {
                case '<': return '&lt;';
                case '>': return '&gt;';
                case '&': return '&amp;';
                case '\'': return '&apos;';
                case '"': return '&quot;';
                default: return char;
            }
        });
    }

    function sendMessage(message: string, inputElement: HTMLInputElement) {
        if (stompClient && stompClient.connected) {
            stompClient.publish({destination: "/app" + topicChannel, body: message})
            inputElement.value = ''
            waitingForResponse = true
            autoScroll = true;
        }
    }

    function handleKeyDown(event) {
        if (event.key === 'Enter') {
            sendMessage(event.target.value, event.target);
            messageInput.value = '';
        }
    }

    function toggleMessage(event) {
        const messageElement = event.currentTarget.parentElement;
        messageElement.classList.toggle('collapsed');
    }

    function handleClick(event) {
        const target = event.target;
        const msgId = target.getAttribute('data-msg');
        const refId = target.getAttribute('data-ref');
        if (msgId && refId) {
            event.preventDefault();
            const message = messages.find(msg => msg.id === msgId);
            const reference = message.references.find(ref => ref.id === refId);
            if (reference) {
                ajaxClient.scrollTo({docId: reference.documentId, offset: [reference.begin, reference.end]});
            }
        }
    }
</script>

<div bind:this={element} class="d-flex flex-column flex-content chat">
    <div class="scrolling flex-content px-3 py-1" bind:this={chatContainer} on:scroll={handleScroll}>
        {#each messages as message}
            <!-- svelte-ignore a11y-click-events-have-key-events -->
            <!-- svelte-ignore a11y-no-noninteractive-tabindex -->
            <div class="message" data-role="{message.role}" data-internal="{message.internal}" 
                 class:collapsed={message.internal}
                 tabIndex={message.internal ? 0 : undefined}>
                <div class="message-header text-body-secondary"
                    on:click={message.internal ? toggleMessage : null} 
                    role={message.internal ? "button" : undefined}>
                    {#if message.role === "assistant"}
                      <i class="fas fa-robot me-1" title="Assistant message"/>
                    {:else if message.role === "user"}
                        <i class="fas fa-user me-1" title="User message"/>
                    {:else if message.role === "system"}
                        <i class="fas fa-cog me-1" title="System message"/>
                    {/if}
                    {message.actor ? message.actor : message.role}
                    {#if message.internal}
                        <span class="mx-2 text-body-secondary float-end fw-lighter">
                            <i class="fas fa-info" title="Internal message"/>
                        </span>
                    {/if}
                </div>
                <!-- svelte-ignore a11y-no-static-element-interactions -->
                <div class="message-body" class:dots="{!message.done}" on:click={handleClick}>
                    {@html renderMessage(message)}
                </div>
                {#if message.performance}
                    <div class="message-footer">
                        <span><i class="far fa-clock me-1"/>{message.performance.duration / 1000}s</span>
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
        <div class="message composer" data-role="user">
            <input type="text" placeholder="Type your message here" bind:this={messageInput} 
                   on:keydown={handleKeyDown}/>
        </div>
    </div>
</div>

<!-- svelte-ignore css-unused-selector -->
<style lang="scss">
  .chat {
    background-color: var(--bs-secondary-bg);
  }

  @keyframes dots {
    0%, 20% {
        content: '';
    }
    40% {
        content: '.';
    }
    60% {
        content: '..';
    }
    80%, 100% {
        content: '...';
    }
  }

  .dots::after {
    content: '';
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

    .message-body {
      display: block;
      font-size: smaller;

      :global(p) {
        margin-bottom: 0.5em;
      }

      :global(code) {
        white-space: break-spaces;
      }

      :global(h1), :global(h2), :global(h3), :global(h4), :global(h5), :global(h6) {
        font-size: var(--bs-body-font-size);
        line-height: var(--bs-body-line-height);
        font-weight: bolder;
        font-variant: small-caps;
      }

      :global(ol), :global(ul) {
        margin-bottom: 1.5em;
        padding-left: 1.5em;
      }
    }

    .message-footer {
      display: block;
      font-size: x-small;
      color: var(--bs-body-color-secondary);
    }

    &[data-role="user"] {
        background-color: var(--bs-info-bg-subtle);
    }
    
    &[data-role="assistant"] {
      background-color: var(--bs-success-bg-subtle);
    }

    &[data-internal="true"] {
      background-color: var(--bs-tertiary-bg);
      padding: 4px;

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

    &.collapsed .message-body {
      display: none;
    }
  }
</style>

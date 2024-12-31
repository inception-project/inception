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
    import DOMPurify from 'dompurify'

    interface MAssistantMessage {
        id: string,
        role: string,
        message: string,
        done: boolean,
        internal: boolean
    }

    export let wsEndpointUrl: string; // should this be full ws://... url
    export let topicChannel: string;
    export let csrfToken: string;

    let socket: WebSocket = null;
    let stompClient: Client = null;
    let connected = false;
    let subscription = null;
    let element = null;
    let chatContainer = null;
    let self = get_current_component()
    let messages = [] as MAssistantMessage[];
    let messageInput;
    let waitingForResponse = false;
    let autoScroll = true;

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
        stompClient.subscribe("/app" + topicChannel, (msg) => messageRecieved(msg));
        stompClient.subscribe("/topic" + topicChannel, (msg) => messageRecieved(msg));
    }

    export function messageRecieved(msg) {
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
            if (waitingForResponse && incomingMessage.role == "assistant" && !incomingMessage.internal) {
                waitingForResponse = false
            }

            const index = messages.findIndex(message => message.id === incomingMessage.id);
            if (index !== -1) {
                if (incomingMessage.done) {
                    messages = [
                        ...messages.slice(0, index),
                        incomingMessage,
                        ...messages.slice(index + 1)
                    ];
                }
                else {
                    messages = [
                        ...messages.slice(0, index),
                        { ...messages[index], message: messages[index].message + incomingMessage.message },
                        ...messages.slice(index + 1)
                    ];
                }
            } else {
                messages = [...messages, incomingMessage];
            }
        });
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

    function scrollToBottom() {
        if (autoScroll) {
            chatContainer.scrollTop = chatContainer.scrollHeight;
        }
    }

    function handleScroll() {
        autoScroll = chatContainer.scrollTop + chatContainer.clientHeight >= chatContainer.scrollHeight;
    }

    onMount(async () => {
        connect();
    });

    onDestroy(async () => {
        disconnect();
    });

    afterUpdate(() => {
        scrollToBottom();
    });

    function renderMessage(message: MAssistantMessage) {
        const rawHtml = marked(message.message) as string
        return DOMPurify.sanitize(rawHtml, { RETURN_DOM: false })
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
</script>

<div bind:this={element} class="d-flex flex-column flex-content chat">
    <div class="scrolling flex-content px-3 py-1" bind:this={chatContainer} on:scroll={handleScroll}>
        {#each messages as message}
            <div class="message" data-actor="{message.role}" data-internal="{message.internal}">
                <div class="role text-body-secondary">{message.role}
                    {#if message.internal}
                    <span class="ms-2 text-body-secondary">(internal)</span>
                    {/if}
                </div>
                <div class="message-body" class:dots="{!message.done}">{@html renderMessage(message)}</div>
            </div>
        {/each}
        {#if waitingForResponse}
            <div class="message" data-actor="assistant">
                <div class="role text-body-secondary">assistant</div>
                <div class="message-body"><span class="dots"></span></div>
            </div>
        {/if}
    </div>
    <div class="px-3 py-1">
        <div class="message composer" data-actor="user">
            <input type="text" placeholder="Type your message here" bind:this={messageInput} on:keydown={handleKeyDown}/>
        </div>
    </div>
</div>

<!-- svelte-ignore css-unused-selector -->
<style lang="scss">
  .chat {
    background-color: beige;
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
    color: #000;
    clear: both;
    line-height: 18px;
    font-size: 15px;
    padding: 8px;
    position: relative;
    margin: 8px 0;
    word-wrap: break-word;
    width: 100%;
    max-width: 100%;
    border-radius: 0.25em;
    
    &.composer {
      background: #f0f0f0;
      border-radius: 5px;

      input {
        border: none;
        background: transparent;
        width: 100%;
        padding: 8px;
        font-size: 15px;
        line-height: 18px;
        outline: none;
        border-radius: 5px;
        margin: 0;
        box-sizing: border-box;
      }
    }

    .message-body {
      display: block;

      :global(p):last-child {
        margin-bottom: 0;
      }

      :global(code) {
        white-space: break-spaces;
      }
    }

    &[data-actor="user"] {
      background: #e1ffc7;
    }
    
    &[data-actor="assistant"] {
      background: #fff;
    }

    &[data-internal="true"] {
      font-size: x-small;
      background-color: #ffffff80;

      :global(pre) {
        margin-bottom: 0.2rem;
      }
    }
  }
</style>

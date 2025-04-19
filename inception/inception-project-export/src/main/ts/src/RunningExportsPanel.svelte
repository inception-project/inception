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
    import { Client, Stomp, IFrame } from "@stomp/stompjs"

    interface RExportLogMessage {
        level: 'INFO' | 'WARN' | 'ERROR'
        message: string
    }

    interface MProjectExportStateUpdate {
        timestamp: number
        id: string
        progress: number
        state: 'NOT_STARTED' | 'RUNNING' | 'COMPLETED' | 'CANCELLED' | 'FAILED'
        title: string
        url: string
        messageCount: number
        messages: RExportLogMessage[]
        removed?: boolean
        latestMessage?: RExportLogMessage
    }

    interface Props {
        wsEndpointUrl: string; // should this be full ws://... url
        csrfToken: string;
        topicChannel: string;
        exports: MProjectExportStateUpdate[];
        connected?: boolean;
    }

    let {
        wsEndpointUrl,
        csrfToken,
        topicChannel,
        exports = $bindable(),
        connected = $bindable(false)
    }: Props = $props();

    let cancelPending = new Set()
    let socket: WebSocket = null
    let stompClient: Client = null
    let connectionError = null

    export function connect(): void {
      if (connected){
        return
      }

      let protocol = (window.location.protocol === 'https:' ? 'wss:' : 'ws:')
      let wsEndpoint = new URL(wsEndpointUrl)
      wsEndpoint.protocol = protocol

      stompClient = Stomp.over(() => socket = new WebSocket(wsEndpoint.toString()))
      stompClient.connectHeaders = {
        'X-CSRF-TOKEN': csrfToken
      }
      stompClient.onConnect = () => { 
          connected = true
          stompClient.subscribe('/user/queue/errors', function (msg) {
            console.error('Websocket server error: ' + JSON.stringify(msg.body));
          })
          stompClient.subscribe('/app' + topicChannel, function (msg) {
            exports = JSON.parse(msg.body)
          })
          stompClient.subscribe('/topic' + topicChannel, function (msg) {
            var msgBody = JSON.parse(msg.body)
            var index = exports.findIndex(item => item.id === msgBody.id)
            if (index === -1) {
              if (!msgBody.removed) {
                exports = [...exports, msgBody]
              }
              else {
                cancelPending.delete(msgBody.id)
              }
            }
            else {
              if (!msgBody.removed) {
                exports = exports.map((e, i) => i !== index ? e : msgBody)
              }
              else {
                exports = exports.filter((e , i) => i !== index)
                cancelPending.delete(msgBody.id)
              }
            }
          })
        }

        stompClient.onStompError = handleBrokerError

        stompClient.activate()
    }


    export function disconnect () {
        stompClient && stompClient.deactivate()
        socket && socket.close()
        connected = false;
    }

    function handleBrokerError (receipt: IFrame) {
        console.log('Broker reported error: ' + receipt.headers.message)
        console.log('Additional details: ' + receipt.body)
    }
    
    export function download(item: MProjectExportStateUpdate): void {
      window.location = item.url + '/data';
    }
    
    export function cancel(task: MProjectExportStateUpdate): void {
      cancelPending.add(task.id);
      stompClient.publish({
        destination: '/app/export/' + task.id + '/cancel',
        headers: {
          'X-CSRF-TOKEN': csrfToken
        },
        body: ''
      })
    }

    export function loadMessages(item: MProjectExportStateUpdate): void {
        fetch(item.url + '/log')
            .then(res => res.json())
            .then(data => item.messages = data)
    }
    
    export function closeMessages(item): void {
      item.messages = null;
    }

    onMount(async () => {
        connect()
    })

    onDestroy(async () => {
        disconnect()
    })
</script>

<div class="flex-content flex-v-container">
    {#if connectionError}
        <div class="flex-content flex-h-container no-data-notice">
            {connectionError}
        </div>
    {/if}
    {#if !connected}
        <div class="flex-content flex-h-container no-data-notice">
            Connecting...
        </div>
    {/if}
    {#if connected && !exports?.length}
        <div class="flex-content flex-h-container no-data-notice">
            No exports.
        </div>
    {/if}
    {#if exports?.length}
        <ul class="list-group list-group-flush">
            {#each exports as item}
                <li class="list-group-item p-2">
                <div class="d-flex w-100 justify-content-between"> 
                    <h5 class="mb-1">{item.title}</h5>
                    {#if !cancelPending.has(item.id)}
                        <div>
                            <button type="button" class="btn-close" aria-label="Close" onclick={cancel(item)}></button>
                        </div>
                    {/if}
                    {#if cancelPending.has(item.id)}
                        <div>
                            Aborted!
                        </div>
                    {/if}
                </div>
                {#if item.state === 'RUNNING'}
                    <div>
                        <progress max="100" value="{item.progress}" class="w-100"></progress>
                    </div>
                {/if}
                {#if item.state === 'COMPLETED'}
                    <div class="text-center">
                        <a href="{item.url + '/data'}" class="animated pulse btn btn-primary">
                            <i class="fas fa-download"></i> Download
                        </a>
                        <!--
                        <button type="button" class="animated pulse btn btn-primary" on:click="{download(item)}">
                        <i class="fas fa-download"></i> Download
                        </button>
                        -->
                    </div>
                {/if}
                {#if item.messages?.length}
                    <div class="card">
                        <div class="card-header small">
                            Messages
                            <button type="button" class="btn-close float-end" aria-label="Close" onclick={closeMessages(item)}></button>
                        </div>
                        <div class="card-body" style="max-height: 10em; min-height: 3em; overflow: auto;">
                            {#each item.messages as message}
                                <div>
                                    <small>
                                        {#if message.level === 'ERROR'}
                                            <i class="text-danger fas fa-exclamation-triangle"></i>
                                        {:else if message.level === 'WARN'}
                                            <i class="text-warning fas fa-exclamation-triangle"></i>
                                        {:else}
                                            <i class="text-muted fas fa-info-circle"></i>
                                        {/if}
                                        {message.message}
                                    </small>
                                </div>
                            {/each}
                        </div>
                    </div>
                {:else if item.latestMessage}
                    <div class="p-2">
                        <small>
                            {#if item.latestMessage.level === 'ERROR'}
                                <i class="text-danger fas fa-exclamation-triangle"></i>
                            {:else if item.latestMessage.level === 'WARN'}
                                <i class="text-warning fas fa-exclamation-triangle"></i>
                            {:else}
                                <i class="text-muted fas fa-info-circle"></i>
                            {/if}
                            {item.latestMessage.message}
                            {#if item.messageCount > 1 && item.state !== 'RUNNING'}
                                <span onclick={loadMessages(item)} class="float-end badge rounded-pill bg-light text-dark" style="cursor: pointer;">
                                    Show all {item.messageCount} messages...
                                </span>
                            {/if}
                        </small>
                    </div>
                {/if}
                </li>
            {/each}
        </ul>
    {/if}
</div>

<style>
.animated {
    animation-duration: 1s;
    animation-fill-mode: both;
}

.pulse {
    animation-name: pulse;
}

@keyframes pulse {
    0% { transform: scale3d(1,1,1) }
    50% { transform: scale3d(1.05,1.05,1.05) }
    100% { transform: scale3d(1,1,1) }
}
</style>
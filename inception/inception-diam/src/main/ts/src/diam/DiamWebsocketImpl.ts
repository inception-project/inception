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
import { Client, Stomp, StompSubscription, IFrame, frameCallbackType } from '@stomp/stompjs'
import { DiamWebsocket, DiamWebsocketConnectOptions } from '@inception-project/inception-js-api'
import * as jsonpatch from 'fast-json-patch'

/**
 * This callback will accept the annotation data.
 */
export declare type dataCallback = (data: any) => void;

export class DiamWebsocketImpl implements DiamWebsocket {
  private stompClient: Client
  private webSocket: WebSocket
  private initSubscription: StompSubscription
  private updateSubscription: StompSubscription
  private diff: any

  private data: any

  public onConnect: frameCallbackType

  connect (options: string | DiamWebsocketConnectOptions) {
    if (this.stompClient) {
      console.log('Already connected')
      return
    }

    const wsEndpoint = new URL((options instanceof String || typeof options === 'string') ? 
        options as string : options.url)

    const protocol = (window.location.protocol === 'https:' ? 'wss:' : 'ws:')
    wsEndpoint.protocol = protocol

    this.stompClient = Stomp.over(() => new WebSocket(wsEndpoint.toString()))
    this.stompClient.reconnectDelay = 5000
    
    if (typeof options !== 'string' && options?.csrfToken) {
      this.stompClient.connectHeaders = {
        'X-CSRF-TOKEN': options.csrfToken
      } 
    }

    this.stompClient.onConnect = frame => {
      this.stompClient.subscribe('/user/queue/errors', this.handleProtocolError)
      if (this.onConnect) {
        this.onConnect(frame)
      }
    }

    this.stompClient.onStompError = this.handleBrokerError

    this.stompClient.activate()
  }

  disconnect () {
    this.stompClient.deactivate()
    this.stompClient.webSocket?.close()
  }

  private handleBrokerError (receipt: IFrame) {
    console.log('Broker reported error: ' + receipt.headers.message)
    console.log('Additional details: ' + receipt.body)
  }

  private handleProtocolError (msg) {
    console.log(msg)
  }

  subscribeToViewport (aViewportTopic: string, callback: dataCallback) {
    this.unsubscribeFromViewport()
    this.initSubscription = this.stompClient.subscribe('/app' + aViewportTopic, msg => {
      this.data = JSON.parse(msg.body)
      this.diff = null
      callback(this.data)
    })
    this.updateSubscription = this.stompClient.subscribe('/topic' + aViewportTopic, msg => {
      const update = JSON.parse(msg.body)
      this.data = jsonpatch.applyPatch(this.data, update.diff).newDocument
      this.diff = update.diff
      callback(this.data)
    })
  }

  unsubscribeFromViewport () {
    if (this.initSubscription) {
      this.initSubscription.unsubscribe()
    }
    if (this.updateSubscription) {
      this.updateSubscription.unsubscribe()
    }
  }
}

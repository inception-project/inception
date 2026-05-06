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
import { Client, Stomp, type StompSubscription, type IFrame, type frameCallbackType, type IMessage } from '@stomp/stompjs';
import { type DiamWebsocket, type DiamWebsocketConnectOptions, type DiamWebsocketSubscribeOptions } from '@inception-project/inception-js-api';
import * as jsonpatch from 'fast-json-patch';

/**
 * This callback will accept the annotation data.
 */
export declare type dataCallback = (data: any) => void;

export class DiamWebsocketImpl implements DiamWebsocket {
    private stompClient?: Client;
    private initSubscription!: StompSubscription;
    private updateSubscription!: StompSubscription;

    private data: any;

    public onConnect!: frameCallbackType;

    connect(options: string | DiamWebsocketConnectOptions) {
        if (this.stompClient) {
            console.debug('Already connected');
            return;
        }

        const wsEndpoint = new URL(
            options instanceof String || typeof options === 'string'
                ? (options as string)
                : options.url
        );

        const protocol = window.location.protocol === 'https:' ? 'wss:' : 'ws:';
        wsEndpoint.protocol = protocol;

        this.stompClient = Stomp.over(() => new WebSocket(wsEndpoint.toString()));
        this.stompClient.reconnectDelay = 5000;

        if (typeof options !== 'string' && options?.csrfToken) {
            this.stompClient.connectHeaders = {
                'X-CSRF-TOKEN': options.csrfToken,
            };
        }

        this.stompClient.onConnect = (frame) => {
            if (!this.stompClient) return
            this.stompClient.subscribe('/user/queue/errors', this.handleProtocolError);
            if (this.onConnect) {
                this.onConnect(frame);
            }
        };

        this.stompClient.onStompError = this.handleBrokerError;

        this.stompClient.activate();
    }

    disconnect() {
        if (this.stompClient) {
            this.stompClient.deactivate();
            this.stompClient.webSocket?.close();
            this.stompClient = undefined;
        }
    }

    private handleBrokerError(receipt: IFrame) {
        console.log('Broker reported error: ' + receipt.headers.message);
        console.log('Additional details: ' + receipt.body);
    }

    private handleProtocolError(msg: IMessage) {
        console.log(msg);
    }

    subscribeToViewport(aViewportTopic: string, callback: dataCallback, options?: DiamWebsocketSubscribeOptions) {
        if (!this.stompClient) return

        let selectorMap: Record<string, string|string[]> = {
            'X-DIAM-FORMAT': 'compact_v2',
            'X-DIAM-EXTENSIONS': []
        };

        if (options) {
            if (options.enableExtensions) {
                // Validate that extensions is an array of allowed names (lowercase letters and hyphens)
                if (!Array.isArray(options.enableExtensions)) {
                    throw new Error('options.enableExtensions must be an array of extension names');
                }
                for (const e of options.enableExtensions) {
                    if (typeof e !== 'string' || !/^[a-z-]+$/.test(e)) {
                        throw new Error(`Invalid extension name: ${e}. Must match /^[a-z-]+$/`);
                    }
                }
                selectorMap['X-DIAM-EXTENSIONS'] = options.enableExtensions || [];
            }
            if (options.format) {
                selectorMap['X-DIAM-FORMAT'] = options.format;
            }
        }

        let headers = makeHeaders(selectorMap);
        let selector = buildSelectorHeader(selectorMap);
        if (selector) {
            headers['selector'] = selector;
        }

        this.unsubscribeFromViewport();

        this.initSubscription = this.stompClient.subscribe('/app' + aViewportTopic, (msg) => {
            this.data = JSON.parse(msg.body);
            callback(this.data);
        }, headers);

        this.updateSubscription = this.stompClient.subscribe('/topic' + aViewportTopic, (msg) => {
            const update = JSON.parse(msg.body);
            this.data = jsonpatch.applyPatch(this.data, update.diff).newDocument;
            callback(this.data);
        }, headers);
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

export function makeHeaders(selectorMap: Record<string, string | string[]>): Record<string, string> {
    let headers: Record<string, string> = {}
    for (const [key, value] of Object.entries(selectorMap)) {
        if (value === null || value === undefined || value === '') {
            continue;
        }

        if (Array.isArray(value)) {
            // Arrays: Sort and deduplicate then perform equality check against
            // the canonical JSON representation
            const sortedValues = Array.from(new Set([...value].map(String).sort()));
            headers[key] = JSON.stringify(sortedValues);
        } else {
            headers[key] = value;
        }
    }
    return headers;
}

/**
 * Converts a map of header requirements into a Spring SpEL selector string.
 * @param selectorMap - A dictionary where keys are header names and values are 
 * either a single string (exact match) or an array of strings (all must match).
 * @returns The formatted SpEL selector string, or undefined if no valid selectors were provided.
 */
export function buildSelectorHeader(selectorMap: Record<string, string | string[]>): string | undefined {
    const selectorConditions: string[] = [];

    for (const [key, value] of Object.entries(selectorMap)) {
        // Skip null, undefined, or empty strings
        if (value === null || value === undefined || value === '') {
            continue; 
        }

        if (Array.isArray(value)) {
            // Arrays: Sort and deduplicate then perform equality check against
            // the canonical JSON representation
            const sortedValues = Array.from(new Set([...value].map(String).sort()));
            selectorConditions.push(`headers['${key}'] == '${JSON.stringify(sortedValues)}'`);
        } else {
            // Strings: Enforce an exact match
            selectorConditions.push(`headers['${key}'] == '${value}'`);
        }
    }

    // Join all conditions with 'and', or return undefined if empty
    return selectorConditions.length > 0 ? selectorConditions.join(' and ') : undefined;
}

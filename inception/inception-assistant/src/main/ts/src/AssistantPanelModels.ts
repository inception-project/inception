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

// Shared type definitions for AssistantPanel

export interface MPerformanceMetrics {
    duration: number;
    tokens: number;
}

export interface MReference {
    id: string;
    counter: number;
    documentId: number;
    documentName: string;
    begin: number;
    end: number;
    score: number;
}

export interface MChatMessage {
    id: string;
    role: string;
    actor?: string;
    internal: boolean;
    performance?: MPerformanceMetrics;
    context?: string;
    references?: MReference[];

    // Frontend only
    collapsed?: boolean;
    collapsible?: boolean;
}

export interface MTextMessage extends MChatMessage {
    thinking: string;
    thinkingSummary: string;
    content: string;
    done: boolean;

    // Frontend only
    thinkingCollapsed?: boolean;
}

export interface MCallResponse extends MChatMessage {
    toolName: string;
    arguments: any;
    payload: any;
}

// Group item types for rendering grouped messages in the UI
export interface MGroupSingle {
    type: "single";
    message: MChatMessage;
}

export interface MGroupGroup {
    type: "group";
    messages: MChatMessage[];
    id: string;
    lastMessage: MChatMessage;
    lastHasThinkingAndContent: boolean;
    activeThinkingMessage: MTextMessage | null;
    activeThinkingHtml: string;
    lastContentHtml: string;
    lastCompletedThinkingMessage?: MTextMessage | null;
    lastCompletedThinkingHtml?: string;
}

export type MGroupItem = MGroupSingle | MGroupGroup;

// Type guard functions for runtime type checking
export function isTextMessage(m: MChatMessage): m is MTextMessage {
    return 'thinking' in m || 'content' in m || 'done' in m;
}

export function isCallResponse(m: MChatMessage): m is MCallResponse {
    return m['@type'] === 'callResponse';
}

export function isGroupableMessage(m: MChatMessage): boolean {
    // Messages with streaming/thinking content are groupable
    if (isTextMessage(m) && m.thinking) {
        return true;
    }
    
    // Call responses and error envelope types are groupable
    if (m['@type'] === 'callResponse' || 
        m['@type'] === 'error' || 
        m['@type'] === 'errorMessage') {
        return true;
    }
    
    // System messages with actor 'Error' are groupable so that tool errors
    // are shown as part of the running group instead of terminating it
    if (m.role === 'system' && m.actor === 'Error') {
        return true;
    }
    
    return false;
}

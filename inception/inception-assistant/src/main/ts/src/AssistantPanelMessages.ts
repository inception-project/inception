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
/*
 * Helper functions for grouping and rendering assistant messages.
 */
import { marked } from 'marked';
import DOMPurify from 'dompurify';
import { replaceReferencesWithHtmlLinks } from './AssistantPanelReferences';
import type { MTextMessage, MChatMessage, MGroupItem } from './AssistantPanelModels';
import { isTextMessage, isGroupableMessage } from './AssistantPanelModels';

// Configure marked consistently for these rendering helpers
marked.setOptions({
    breaks: true,
    gfm: true,
    async: false,
});

export function renderThinking(message: MTextMessage): string {
    if (!message?.thinking) {
        return '';
    }

    const trimmedMessage = message.thinking.replace(/{{ref::[\w-]*}?$/, '');

    const rawHtml = marked(trimmedMessage) as string;
    const pureHtml = DOMPurify.sanitize(rawHtml, { RETURN_DOM: false });

    // Replace all references with the respective reference link
    return replaceReferencesWithHtmlLinks(message, pureHtml);
}

export function renderContent(message: MTextMessage): string {
    if (!message?.content) {
        return '';
    }

    const trimmedMessage = message.content.replace(/{{ref::[\w-]*}?$/, '');

    const rawHtml = marked(trimmedMessage) as string;
    const pureHtml = DOMPurify.sanitize(rawHtml, { RETURN_DOM: false });

    // Replace all references with the respective reference link
    return replaceReferencesWithHtmlLinks(message, pureHtml);
}

export function buildGroups(messages: MChatMessage[]): MGroupItem[] {
    console.debug('[assistant] buildGroups called with', messages.length, 'messages');
    const groups: MGroupItem[] = [];
    let temp: MChatMessage[] = [];

    const flushTemp = (closed: boolean = false) => {
        if (temp.length === 0) return;
        // Use only the first message ID to keep the group ID stable during streaming
        const id = temp[0].id;
        const last = temp[temp.length - 1];
        // find the most recent *active* thinking (incomplete) and the most recent *completed* thinking
        let activeThinking: MTextMessage | null = null;
        let lastCompletedThinking: MTextMessage | null = null;
        for (let i = temp.length - 1; i >= 0; i--) {
            const maybe = temp[i];
            if (isTextMessage(maybe) && maybe.thinking) {
                if (!activeThinking && !maybe.done) {
                    activeThinking = maybe;
                }
                if (!lastCompletedThinking && maybe.done) {
                    lastCompletedThinking = maybe;
                }
                if (activeThinking && lastCompletedThinking) break;
            }
        }
        const lastAsText = isTextMessage(last) ? last : null;
        groups.push({
            type: 'group',
            messages: temp.slice(),
            id,
            lastMessage: last,
            lastHasThinkingAndContent: !!lastAsText?.thinking && !!lastAsText?.content,
            activeThinkingMessage: activeThinking,
            activeThinkingHtml: activeThinking ? renderThinking(activeThinking) : '',
            lastCompletedThinkingMessage: lastCompletedThinking,
            lastCompletedThinkingHtml: lastCompletedThinking
                ? renderThinking(lastCompletedThinking)
                : '',
            lastContentHtml: lastAsText ? renderContent(lastAsText) : '',
            // mark whether this group was closed by encountering a non-groupable
            // message. A closed group can be shown as completed in the UI.
            closed: closed,
        });
        temp = [];
    };

    for (const m of messages) {
        if (m.role === 'user') {
            // do not group user messages: flush any pending group and emit a single
            // mark the group as closed because encountering a user message ends
            // the running/grouping sequence.
            flushTemp(true);
            console.debug('[assistant] Adding user message as single:', m.id);
            groups.push({ type: 'single', message: m });
            continue;
        }

        if (isGroupableMessage(m)) {
            console.debug('[assistant] Message is groupable:', m.id, m);
            temp.push(m);
        } else {
            // Flush any pending groupable messages first. This non-groupable
            // message terminates the current group (closed=true).
            flushTemp(true);
            // Non-groupable messages are shown directly (not collapsible)
            console.debug('[assistant] Adding non-groupable message as single:', m.id, m);
            groups.push({ type: 'single', message: m });
        }
    }

    // Final flush at end-of-input: do not mark as closed since no terminating
    // non-groupable message was seen after the buffered group.
    flushTemp(false);
    console.debug('[assistant] buildGroups result:', groups.length, 'groups', groups);
    return groups;
}

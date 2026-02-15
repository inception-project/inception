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
import { describe, it, expect } from 'vitest';
import { buildGroups } from '../src/AssistantPanelMessages';
import type { MTextMessage, MChatMessage } from '../src/AssistantPanelModels';

function textMessage(id: string, opts: Partial<MTextMessage> = {}): MTextMessage {
    return {
        id,
        role: 'assistant',
        internal: false,
        thinking: opts.thinking ?? '',
        thinkingSummary: opts.thinkingSummary ?? '',
        content: opts.content ?? '',
        done: opts.done ?? false,
    } as MTextMessage;
}

describe('AssistantPanelMessages.buildGroups', () => {
    it('keeps user messages as single and non-groupable assistant messages as single', () => {
        const msgs: MChatMessage[] = [
            { id: 'u1', role: 'user', internal: false },
            { id: 'a1', role: 'assistant', internal: false, content: 'Hello' } as any,
        ];

        const groups = buildGroups(msgs);
        expect(groups.length).toBe(2);
        expect(groups[0].type).toBe('single');
        expect(groups[1].type).toBe('single');
    });

    it('identifies active and last completed thinking when active thinking exists', () => {
        const m1 = textMessage('t1', { thinking: 'first', thinkingSummary: 'first sum', done: true });
        const m2 = textMessage('t2', { thinking: 'second', thinkingSummary: 'second sum', done: false });
        const m3 = { id: 'c1', role: 'assistant', internal: false, content: 'final' } as any;

        const groups = buildGroups([m1, m2, m3]);
        // m1 and m2 are groupable (have thinking), m3 is not (no thinking, no special type)
        expect(groups.length).toBe(2);
        const g = groups[0] as any;
        expect(g.type).toBe('group');
        expect(g.activeThinkingMessage).toBeDefined();
        expect(g.activeThinkingMessage.id).toBe('t2');
        expect(g.lastCompletedThinkingMessage).toBeDefined();
        expect(g.lastCompletedThinkingMessage.id).toBe('t1');
        // m3 should be a separate single item
        expect(groups[1].type).toBe('single');
        expect((groups[1] as any).message.id).toBe('c1');
    });

    it('separates non-groupable text messages from groupable ones', () => {
        const m1 = textMessage('t1', { thinking: 'first', thinkingSummary: 'first sum', done: true });
        const m2 = { id: 'c1', role: 'assistant', internal: false, content: 'final' } as any;

        const groups = buildGroups([m1, m2]);
        // The thinking message should be in a group, and the plain text message should be a single
        expect(groups.length).toBe(2);
        expect(groups[0].type).toBe('group');
        expect((groups[0] as any).messages.length).toBe(1);
        expect((groups[0] as any).messages[0].id).toBe('t1');
        expect(groups[1].type).toBe('single');
        expect((groups[1] as any).message.id).toBe('c1');
    });

    it('includes system Error messages in the running group (do not terminate group)', () => {
        const m1 = textMessage('t1', { thinking: 'first', thinkingSummary: 'first sum', done: true });
        const m2 = textMessage('t2', { thinking: 'second', thinkingSummary: 'second sum', done: false });
        const err = { id: 'e1', role: 'system', actor: 'Error', content: 'Error: There is no tool named `find`.', done: true } as any;
        const groups = buildGroups([m1, m2, err]);
        expect(groups.length).toBe(1);
        const g = groups[0] as any;
        expect(g.type).toBe('group');
        // The error message should be included in the group's messages
        expect(g.messages.some((mm) => mm.id === 'e1')).toBe(true);
    });
});

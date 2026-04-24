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
import { SpeechManager } from '../src/AssistantPanelSpeech';
import type { MTextMessage } from '../src/AssistantPanelModels';

describe('SpeechManager', () => {
    it('does not drop trailing text after refs in streamed chunks', async () => {
        const spoken: string[] = [];

        // Mock SpeechSynthesisUtterance and speechSynthesis for Node test environment
        // @ts-ignore
        global.SpeechSynthesisUtterance = class {
            text: string;
            onend: (() => void) | null = null;
            constructor(t: string) {
                this.text = t;
            }
        } as any;

        // @ts-ignore
        global.speechSynthesis = {
            speak: (utt: any) => {
                spoken.push(utt.text);
                // simulate async end
                if (typeof utt.onend === 'function') setTimeout(() => utt.onend(), 0);
            },
            cancel: () => {},
        } as any;

        const sm = new SpeechManager(() => true);

        const base: Partial<MTextMessage> = {
            id: 'm1',
            role: 'assistant',
            internal: false,
            thinking: '',
            thinkingSummary: '',
        };

        // Streamed chunks: punctuation, refs, trailing text
        sm.speak({
            ...(base as MTextMessage),
            content:
                'Because he wanted his claim to echo across the galaxy—even past the Shards of the Throne and the ancient “Imperial Throne” variant!',
            done: false,
        });
        sm.speak({ ...(base as MTextMessage), content: ' {{ref::ads}}{{ref::fes}}', done: false });
        sm.speak({ ...(base as MTextMessage), content: ' the pointe', done: true });

        // Wait for any async onend handlers to run
        await new Promise((r) => setTimeout(r, 50));

        // Expect that the spoken output includes the trailing text 'the pointe'
        expect(spoken.join(' ')).toContain('the pointe');
    });
});

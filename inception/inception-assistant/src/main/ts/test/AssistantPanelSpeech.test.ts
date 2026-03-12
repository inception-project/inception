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
import { stripMarkdown } from '../src/AssistantPanelSpeech';

describe('AssistantPanelSpeech.stripMarkdown', () => {
    it('returns empty string when input is empty', () => {
        expect(stripMarkdown('')).toBe('');
    });

    it('strips emphasis and preserves words', () => {
        const inText = '**bold** and _italic_';
        expect(stripMarkdown(inText)).toBe('bold and italic');
    });

    it('removes links but keeps link text', () => {
        const inText = 'See [example](https://example.com) now';
        expect(stripMarkdown(inText)).toBe('See example now');
    });

    it('handles headings and code spans', () => {
        const inText = '# Title\nSome `code` here';
        expect(stripMarkdown(inText)).toBe('Title Some code here');
    });

    it('collapses multiple whitespace and newlines', () => {
        const inText = 'Line1\n\n\n   Line2   \t\n';
        expect(stripMarkdown(inText)).toBe('Line1 Line2');
    });
});

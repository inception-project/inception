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
import {
    stripRefs,
    replaceReferencesWithHtmlLinks,
    findReferencesInText,
    formatReferenceTitle,
    refIdReplacementPattern,
    docIdReplacementPattern,
} from '../src/AssistantPanelReferences';
import type { MChatMessage, MReference } from '../src/AssistantPanelModels';

describe('AssistantPanelReferences', () => {
    const reference: MReference = {
        id: 'r1',
        counter: 1,
        documentId: 42,
        documentName: 'Example Document',
        begin: 0,
        end: 10,
        score: 0.987,
    };

    const message: MChatMessage = {
        id: 'm1',
        role: 'assistant',
        internal: false,
        references: [reference],
    };

    it('strips ref and doc markers', () => {
        const input = `This is a reference {{ref::r1}} and a Document 12345678.`;
        const out = stripRefs(input);
        expect(out).not.toContain('{{ref::r1}}');
        expect(out).not.toMatch(/Document\s+12345678/);
    });

    it('finds references in text', () => {
        const input = `Before {{ref::r1}} middle Document 1a2b3c4d after`;
        const found = findReferencesInText(input);
        expect(found.length).toBeGreaterThanOrEqual(2);
        expect(found.some((f) => f.type === 'ref' && f.id === 'r1')).toBe(true);
        expect(found.some((f) => f.type === 'doc' && f.id === '1a2b3c4d')).toBe(true);
    });

    it('replaces id refs with html links', () => {
        const html = `See more {{ref::r1}}.`;
        const replaced = replaceReferencesWithHtmlLinks(message, html, refIdReplacementPattern);
        expect(replaced).toContain('data-ref="r1"');
        expect(replaced).toContain('>1<');
        expect(replaced).toContain('Example Document');
    });

    it('formats reference title with score', () => {
        const title = formatReferenceTitle(reference);
        expect(title).toContain('Example Document');
        expect(title).toContain('(score: 0.9870)');
    });

    it('filters out hallucinated references that do not exist', () => {
        const html = `See {{ref::r1}} and {{ref::nonexistent}} here.`;
        const replaced = replaceReferencesWithHtmlLinks(message, html);
        // Real reference should be replaced
        expect(replaced).toContain('data-ref="r1"');
        expect(replaced).toContain('>1<');
        // Hallucinated reference should be completely removed
        expect(replaced).not.toContain('{{ref::nonexistent}}');
        expect(replaced).not.toContain('nonexistent');
        // Leading whitespace before hallucinated ref is consumed by the pattern
        expect(replaced).toContain(' and here');
    });

    it('preserves dots when filtering hallucinated references', () => {
        const html = `Text {{ref::r1}}... and {{ref::fake}}...`;
        const replaced = replaceReferencesWithHtmlLinks(message, html);
        // Real reference with dots
        expect(replaced).toMatch(/data-ref="r1".*\.\.\./);
        // Hallucinated reference should be removed but dots preserved
        expect(replaced).not.toContain('{{ref::fake}}');
        // Leading whitespace before hallucinated ref is consumed by pattern
        expect(replaced).toContain('and...');
    });
});

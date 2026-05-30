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
import { describe, it, expect, beforeEach, afterEach } from 'vitest';
import { TeiDocumentStructure } from './TeiDocumentStructure';

let container: HTMLElement;

beforeEach(() => {
    container = document.createElement('div');
    document.body.appendChild(container);
});

afterEach(() => {
    // Keep the DOM clean across tests so leftovers can't make later
    // assertions order-dependent.
    while (document.body.firstChild) {
        document.body.removeChild(document.body.firstChild);
    }
});

/**
 * Build a TEI-flavoured element subtree programmatically. innerHTML cannot be
 * used because the HTML parser treats `<head>` specially (it is hoisted out
 * of body content), which would mangle TEI's structural `<head>` element
 * used as a section title.
 */
function el(name: string, ...children: (string | Node)[]): HTMLElement {
    const e = document.createElement(name);
    for (const c of children) {
        e.appendChild(typeof c === 'string' ? document.createTextNode(c) : c);
    }
    return e;
}

describe('TeiDocumentStructure', () => {
    describe('sectionSelector', () => {
        it("selects 'div' elements", () => {
            expect(new TeiDocumentStructure().sectionSelector).toBe('div');
        });
    });

    describe('preprocess', () => {
        it('does not modify the DOM', () => {
            container.appendChild(
                el(
                    'div',
                    el('head', 'A'),
                    el('p', 'x'),
                    el('div', el('head', 'B'), el('p', 'y'))
                )
            );
            const before = container.innerHTML;
            new TeiDocumentStructure().preprocess(container);
            expect(container.innerHTML).toBe(before);
        });
    });

    describe('extractTitle', () => {
        it('returns the trimmed text of a direct child head', () => {
            const section = el('div', el('head', '  Section title  '));
            container.appendChild(section);
            expect(new TeiDocumentStructure().extractTitle(section)).toBe(
                'Section title'
            );
        });

        it('returns undefined when there is no direct child head', () => {
            const section = el('div', el('p', 'just a paragraph'));
            container.appendChild(section);
            expect(new TeiDocumentStructure().extractTitle(section)).toBeUndefined();
        });

        it('returns undefined when head exists but is empty/whitespace', () => {
            const section = el('div', el('head', '   '));
            container.appendChild(section);
            expect(new TeiDocumentStructure().extractTitle(section)).toBeUndefined();
        });

        it('only considers head as a direct child, not a descendant', () => {
            // A <head> deeper in the tree (e.g. on a nested div) must not be
            // picked up as the title of an outer div, otherwise the outline
            // would show the nested heading on the wrong row.
            const outer = el('div', el('p', 'body'), el('div', el('head', 'nested')));
            container.appendChild(outer);
            expect(new TeiDocumentStructure().extractTitle(outer)).toBeUndefined();
        });
    });

    describe('scrollTarget', () => {
        it('returns the direct child head when one is present', () => {
            const head = el('head', 'X');
            const section = el('div', head, el('p'));
            container.appendChild(section);
            expect(new TeiDocumentStructure().scrollTarget(section)).toBe(head);
        });

        it('falls back to the section element when no direct head exists', () => {
            const section = el('div', el('p', 'only body'));
            container.appendChild(section);
            expect(new TeiDocumentStructure().scrollTarget(section)).toBe(section);
        });
    });

    describe('outline structure on a representative TEI tree', () => {
        it('matches the title text of each nested div with a head', () => {
            container.appendChild(
                el(
                    'div',
                    el('head', 'Chapter 1'),
                    el('p', 'intro'),
                    el('div', el('head', '1.1'), el('p', '...')),
                    el('div', el('head', '1.2'))
                )
            );
            container.appendChild(el('div', el('head', 'Chapter 2')));

            const s = new TeiDocumentStructure();
            const titles = Array.from(container.querySelectorAll('div')).map(
                (d) => s.extractTitle(d) ?? '(none)'
            );
            expect(titles).toEqual(['Chapter 1', '1.1', '1.2', 'Chapter 2']);
        });
    });
});

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
import { normalizeSectionsForPinning } from './SectionPinning';

const TEI_NS = 'http://www.tei-c.org/ns/1.0';
const XHTML_NS = 'http://www.w3.org/1999/xhtml';

let container: HTMLElement;

beforeEach(() => {
    container = document.createElement('div');
    document.body.appendChild(container);
});

afterEach(() => {
    while (document.body.firstChild) {
        document.body.removeChild(document.body.firstChild);
    }
});

/**
 * Concatenate all text-node data in document order. Used to verify that the
 * wrap pass does not reorder text content -- the invariant that annotation
 * character offsets depend on.
 */
function textInOrder(root: Node): string {
    const walker = document.createTreeWalker(root, NodeFilter.SHOW_TEXT, null);
    let out = '';
    let n: Node | null;
    while ((n = walker.nextNode())) out += n.nodeValue ?? '';
    return out;
}

/** Build a `<tei:div>...</tei:div>` element in the TEI namespace. */
function teiEl(name: string, ...children: (Node | string)[]): Element {
    const el = document.createElementNS(TEI_NS, name);
    for (const c of children) {
        el.appendChild(typeof c === 'string' ? document.createTextNode(c) : c);
    }
    return el;
}

describe('normalizeSectionsForPinning', () => {
    it('is a no-op and returns empty when no section names are configured', () => {
        container.appendChild(teiEl('div', 'one'));
        const before = container.innerHTML;

        const selector = normalizeSectionsForPinning(container, new Set());

        expect(selector).toBe('');
        expect(container.querySelectorAll('sec-wrap').length).toBe(0);
        expect(container.innerHTML).toBe(before);
    });

    it('wraps a single namespaced element and returns the wrapper selector', () => {
        const div = teiEl('div', 'body');
        container.appendChild(div);

        const selector = normalizeSectionsForPinning(container, new Set(['div']));

        expect(selector).toBe('sec-wrap');
        const wraps = container.querySelectorAll('sec-wrap');
        expect(wraps.length).toBe(1);
        // Wrapper is HTML-namespace so the editor's HTMLElement-only inline-style
        // pinning will work on it -- the whole point of the wrap.
        expect(wraps[0].namespaceURI).toBe(XHTML_NS);
        expect(wraps[0]).toBeInstanceOf(HTMLElement);
        expect(wraps[0].firstElementChild).toBe(div);
        expect(div.namespaceURI).toBe(TEI_NS);
        expect(wraps[0].getAttribute('data-section-type')).toBe('div');
    });

    it('leaves elements already in the HTML namespace untouched and returns their names', () => {
        const xhtmlSection = document.createElement('section');
        xhtmlSection.textContent = 'already html';
        container.appendChild(xhtmlSection);

        const selector = normalizeSectionsForPinning(container, new Set(['section']));

        // Already an HTMLElement, so pinning works directly: no wrapping, and the
        // section boundary stays the original element name.
        expect(selector).toBe('section');
        expect(container.querySelectorAll('sec-wrap').length).toBe(0);
        expect(xhtmlSection.parentElement).toBe(container);
    });

    it('returns a combined selector when HTML and namespaced sections are mixed', () => {
        // Same configured local name ('div') matches both an already-HTML <div>
        // (left in place) and a namespaced TEI <div> (wrapped). The returned
        // selector must reach the wrapper AND the untouched HTML div, but not
        // the original TEI div now buried inside the wrapper.
        const htmlDiv = document.createElement('div');
        htmlDiv.textContent = 'html section';
        const teiDiv = teiEl('div', 'tei section');
        container.appendChild(htmlDiv);
        container.appendChild(teiDiv);

        const selector = normalizeSectionsForPinning(container, new Set(['div']));

        expect(container.querySelectorAll('sec-wrap').length).toBe(1);
        // What the editor will actually select afterwards:
        const matched = Array.from(container.querySelectorAll(selector));
        const wrap = container.querySelector('sec-wrap')!;
        expect(matched).toContain(wrap); // the wrapped (was namespaced) section
        expect(matched).toContain(htmlDiv); // the still-unwrapped HTML section
        expect(matched).not.toContain(teiDiv); // buried original must not double-match
        expect(matched.length).toBe(2);
    });

    it('wraps nested sections at each level', () => {
        const inner = teiEl('div', 'inner text');
        const outer = teiEl('div', 'outer ', inner);
        container.appendChild(outer);

        normalizeSectionsForPinning(container, new Set(['div']));

        const wraps = container.querySelectorAll('sec-wrap');
        expect(wraps.length).toBe(2);
        for (const w of Array.from(wraps)) {
            expect(w.firstElementChild?.namespaceURI).toBe(TEI_NS);
            expect(w.firstElementChild?.localName).toBe('div');
        }
        const outerWrap = container.firstElementChild!;
        expect(outerWrap.localName).toBe('sec-wrap');
        expect(outerWrap.firstElementChild).toBe(outer);
        expect(outer.querySelector('sec-wrap')?.firstElementChild).toBe(inner);
    });

    it('preserves text content in document order so annotation offsets do not shift', () => {
        const inner = teiEl('div', 'inner ');
        const section1 = teiEl('div', 'first ', inner, 'middle ');
        const section2 = teiEl('div', 'second ');
        container.appendChild(document.createTextNode('lead '));
        container.appendChild(section1);
        container.appendChild(document.createTextNode(' between '));
        container.appendChild(section2);
        container.appendChild(document.createTextNode(' trail'));
        const before = textInOrder(container);

        normalizeSectionsForPinning(container, new Set(['div']));

        expect(textInOrder(container)).toBe(before);
    });

    it('is idempotent: running twice does not double-wrap and keeps reporting the wrapper selector', () => {
        const div = teiEl('div', 'body');
        container.appendChild(div);

        const first = normalizeSectionsForPinning(container, new Set(['div']));
        const second = normalizeSectionsForPinning(container, new Set(['div']));

        expect(container.querySelectorAll('sec-wrap').length).toBe(1);
        expect(container.firstElementChild?.firstElementChild).toBe(div);
        // The selector must stay 'sec-wrap' on the re-run: the wrappers still
        // exist, so downstream must keep targeting them rather than the
        // now-buried original elements.
        expect(first).toBe('sec-wrap');
        expect(second).toBe('sec-wrap');
    });

    it('wraps a namespaced element named sec-wrap rather than mistaking it for the synthetic wrapper', () => {
        // A section element that happens to be a non-HTML <sec-wrap> (here in the
        // TEI namespace) is itself unpinnable and must be wrapped. The idempotency
        // skip must not fire on it just because its parent's local name matches.
        const teiSecWrap = teiEl('sec-wrap', 'body');
        container.appendChild(teiSecWrap);

        const selector = normalizeSectionsForPinning(container, new Set(['sec-wrap']));

        expect(selector).toBe('sec-wrap');
        const wrap = container.firstElementChild!;
        // The wrapper is the synthetic XHTML one; the TEI sec-wrap is its child.
        expect(wrap.namespaceURI).toBe(XHTML_NS);
        expect(wrap).toBeInstanceOf(HTMLElement);
        expect(wrap.firstElementChild).toBe(teiSecWrap);
        expect(teiSecWrap.namespaceURI).toBe(TEI_NS);
        // Running again must not double-wrap the now-pinnable structure.
        normalizeSectionsForPinning(container, new Set(['sec-wrap']));
        expect(container.querySelectorAll('sec-wrap').length).toBe(2); // xhtml wrapper + tei child, no more
        expect(wrap.firstElementChild).toBe(teiSecWrap);
    });

    it('only wraps elements whose local name is configured', () => {
        const wanted = teiEl('div', 'wrapme');
        const skipped = teiEl('p', 'leaveme');
        container.appendChild(wanted);
        container.appendChild(skipped);

        normalizeSectionsForPinning(container, new Set(['div']));

        expect(container.querySelectorAll('sec-wrap').length).toBe(1);
        expect(container.querySelector('sec-wrap')?.firstElementChild).toBe(wanted);
        expect(skipped.parentElement).toBe(container);
    });
});

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
// The selector the function returns for "a wrapped section": the synthetic
// wrappers are addressed by their data-section-type attribute, not by the bare
// 'sec-wrap' tag name, so a buried namespaced original sharing that local name
// is never matched.
const WRAPPER_SELECTOR = 'sec-wrap[data-section-type]';

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

        expect(selector).toBe(WRAPPER_SELECTOR);
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

        const selector = normalizeSectionsForPinning(container, new Set(['div']));

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
        // Lock down the returned selector: all-namespaced => WRAPPER_SELECTOR,
        // and it must resolve to exactly both wrappers (outer + inner), not the
        // buried originals.
        expect(selector).toBe(WRAPPER_SELECTOR);
        expect(new Set(container.querySelectorAll(selector))).toEqual(new Set(Array.from(wraps)));
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
        // The selector must stay stable on the re-run: the wrappers still exist,
        // so downstream must keep targeting them rather than the now-buried
        // original elements.
        expect(first).toBe(WRAPPER_SELECTOR);
        expect(second).toBe(WRAPPER_SELECTOR);
        // ...and it must resolve to exactly the one wrapper, never the buried
        // original.
        const wrap = container.firstElementChild!;
        expect(Array.from(container.querySelectorAll(second))).toEqual([wrap]);
    });

    it('wraps a namespaced element named sec-wrap rather than mistaking it for the synthetic wrapper', () => {
        // A section element that happens to be a non-HTML <sec-wrap> (here in the
        // TEI namespace) is itself unpinnable and must be wrapped. The idempotency
        // skip must not fire on it just because its parent's local name matches.
        const teiSecWrap = teiEl('sec-wrap', 'body');
        container.appendChild(teiSecWrap);

        const selector = normalizeSectionsForPinning(container, new Set(['sec-wrap']));

        expect(selector).toBe(WRAPPER_SELECTOR);
        const wrap = container.firstElementChild!;
        // The wrapper is the synthetic XHTML one; the TEI sec-wrap is its child.
        expect(wrap.namespaceURI).toBe(XHTML_NS);
        expect(wrap).toBeInstanceOf(HTMLElement);
        expect(wrap.firstElementChild).toBe(teiSecWrap);
        expect(teiSecWrap.namespaceURI).toBe(TEI_NS);
        // The returned selector must address only the pinnable wrapper, NOT the
        // buried namespaced original (which shares the local name 'sec-wrap').
        // This is the whole reason wrappers are selected by data-section-type.
        const matched = Array.from(container.querySelectorAll(selector));
        expect(matched).toEqual([wrap]);
        expect(matched).not.toContain(teiSecWrap);
        // Running again must not double-wrap the now-pinnable structure.
        normalizeSectionsForPinning(container, new Set(['sec-wrap']));
        expect(container.querySelectorAll('sec-wrap').length).toBe(2); // xhtml wrapper + tei child, no more
        expect(wrap.firstElementChild).toBe(teiSecWrap);
    });

    it('wraps a configured XHTML <sec-wrap> that lacks the synthetic marker attribute', () => {
        // An XHTML <sec-wrap> is already an HTMLElement, so it needs no wrapping
        // and is left in place (it goes through the already-pinnable branch).
        // The point: it must NOT be treated as one of our synthetic wrappers --
        // only elements carrying data-section-type are. Here it is configured as
        // a section, so it should be reported via the original-name selector.
        const plainSecWrap = document.createElement('sec-wrap'); // XHTML, no marker
        plainSecWrap.textContent = 'plain';
        container.appendChild(plainSecWrap);

        const selector = normalizeSectionsForPinning(container, new Set(['sec-wrap']));

        // Already HTML => nothing wrapped => original-name selector, and it has
        // no data-section-type so WRAPPER_SELECTOR would not match it.
        expect(selector).toBe('sec-wrap');
        expect(plainSecWrap.parentElement).toBe(container);
        expect(plainSecWrap.hasAttribute('data-section-type')).toBe(false);
        expect(Array.from(container.querySelectorAll(WRAPPER_SELECTOR))).toEqual([]);
    });

    it('mixed selector reaches an HTML section that is nested inside a namespaced <sec-wrap>', () => {
        // Regression for the :not() guard: a bare ':not(sec-wrap *)' would wrongly
        // exclude a section nested in a *namespaced* <sec-wrap> (type selectors
        // ignore namespace). The attribute-based ':not(sec-wrap[data-section-type] *)'
        // must only exclude descendants of our synthetic wrappers.
        const teiSecWrap = teiEl('sec-wrap'); // a non-wrapper that shares the name
        const htmlSection = document.createElement('div');
        htmlSection.textContent = 'nested html section';
        teiSecWrap.appendChild(htmlSection);
        const teiDiv = teiEl('div', 'namespaced section'); // forces a real wrapper
        container.appendChild(teiSecWrap);
        container.appendChild(teiDiv);

        const selector = normalizeSectionsForPinning(container, new Set(['div']));

        const matched = Array.from(container.querySelectorAll(selector));
        const realWrap = container.querySelector(WRAPPER_SELECTOR)!;
        expect(matched).toContain(realWrap); // wrapped namespaced div
        // The HTML <div> nested under the *namespaced* <sec-wrap> is NOT inside a
        // synthetic wrapper, so it must still be selected.
        expect(matched).toContain(htmlSection);
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

    it('handles section local names containing CSS-special characters', () => {
        // XML element names legitimately contain dots etc. A raw selector like
        // 'foo.bar' would parse as element foo + class bar and silently match
        // nothing, leaving the section unwrapped and unpinnable. The name must be
        // escaped before use in querySelectorAll / :is(...).
        const dotted = teiEl('foo.bar', 'body');
        container.appendChild(dotted);

        const selector = normalizeSectionsForPinning(container, new Set(['foo.bar']));

        // It must have been wrapped (found despite the special character)...
        const wrap = container.querySelector('sec-wrap')!;
        expect(wrap).toBeTruthy();
        expect(wrap.firstElementChild).toBe(dotted);
        expect(wrap.getAttribute('data-section-type')).toBe('foo.bar');
        // ...and the returned selector must be usable without throwing.
        expect(() => container.querySelectorAll(selector)).not.toThrow();
        expect(Array.from(container.querySelectorAll(selector))).toEqual([wrap]);
    });

    it('mixed guard keeps a genuine HTML section nested inside a wrapped namespaced ancestor', () => {
        // Regression: the buried original is the wrapper's *direct* child, but a
        // real already-HTML section can sit DEEPER inside a wrapped namespaced
        // ancestor. A descendant-combinator :not(sec-wrap *) guard would wrongly
        // drop it; the child-combinator guard must keep it.
        const nestedHtml = document.createElement('div');
        nestedHtml.textContent = 'nested html section';
        const teiAncestor = teiEl('div', 'a', nestedHtml); // wrapped; deep html div kept
        const htmlTop = document.createElement('div');
        htmlTop.textContent = 'top html section';
        container.appendChild(htmlTop);
        container.appendChild(teiAncestor);

        const selector = normalizeSectionsForPinning(container, new Set(['div']));

        const matched = Array.from(container.querySelectorAll(selector));
        const wrap = container.querySelector(WRAPPER_SELECTOR)!;
        expect(matched).toContain(wrap); // the wrapped namespaced ancestor
        expect(matched).toContain(htmlTop); // top-level HTML section
        expect(matched).toContain(nestedHtml); // deep HTML section must NOT be excluded
        expect(matched).not.toContain(teiAncestor); // buried original still excluded
    });
});

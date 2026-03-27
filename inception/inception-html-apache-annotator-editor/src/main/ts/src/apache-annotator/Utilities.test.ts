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
import { describe, it, expect, beforeEach, vi } from 'vitest';

// Mock `highlightText` before importing the utilities so the module import uses the mock.
vi.mock('@apache-annotator/dom', () => ({
    highlightText: (range: Range, tagName = 'mark', attributes: Record<string, string> = {}) => {
        const el = document.createElement(tagName);
        for (const k in attributes) {
            el.setAttribute(k, attributes[k]);
        }
        // The real highlighter marks annotation highlights with class `iaa-highlighted`.
        // Mimic that behaviour so extendHighlightOverProtectedElements can locate them.
        if (attributes && 'data-iaa-id' in attributes) {
            const existing = el.getAttribute('class');
            el.setAttribute('class', (existing ? existing + ' ' : '') + 'iaa-highlighted');
        }
        try {
            range.surroundContents(el);
        } catch (e) {
            // Fallback: replace range contents with the element wrapping cloned contents
            const contents = range.cloneContents();
            el.append(...Array.from(contents.childNodes));
            range.deleteContents();
            range.insertNode(el);
        }
        return () => {
            el.after(...el.childNodes);
            el.remove();
        };
    },
}));

import {
    querySelectorAllInRange,
    extendHighlightOverProtectedElements,
    safeHighlightText,
    groupHighlightsByVid,
    closestHighlight,
    getInlineLabelClientRect,
    isPointInRect,
    removeClassFromAncestors,
    closestNsAware,
    compileNsSelector,
    closestWithMatcher,
    expandSelectionOverProtectedElements,
} from './Utilities';

describe('expandSelectionOverProtectedElements', () => {
    beforeEach(() => {
        document.body.innerHTML = '';
    });

    it('uses the element itself when the boundary node is an Element and expands to protected ancestor', () => {
        document.body.innerHTML = `
            <div id="root">
                <div class="protected">protected text</div>
                <div class="other">other</div>
            </div>
        `;

        const prot = document.querySelector('.protected') as Element;
        const other = document.querySelector('.other') as Element;

        const sel: any = {
            anchorNode: prot,
            anchorOffset: 2,
            focusNode: other.firstChild,
            focusOffset: 1,
        };

        const res = expandSelectionOverProtectedElements(sel as Selection, (el) =>
            el.classList.contains('protected')
        );

        expect(res.anchorNode).toBe(prot);
        expect(res.anchorOffset).toBe(0);
    });

    it('when the boundary is a Text node expands to the protected ancestor', () => {
        document.body.innerHTML = `
            <div id="root">
                <div class="protected">abc</div>
            </div>
        `;

        const prot = document.querySelector('.protected') as Element;
        const text = prot.firstChild as Text;

        const sel: any = {
            anchorNode: text,
            anchorOffset: 1,
            focusNode: text,
            focusOffset: 2,
        };

        const res = expandSelectionOverProtectedElements(sel as Selection, (el) =>
            el.classList.contains('protected')
        );

        expect(res.anchorNode).toBe(prot);
        expect(res.anchorOffset).toBe(0);
        expect(res.focusNode).toBe(prot);
        expect(res.focusOffset).toBe(prot.textContent?.length);
    });
});

describe('querySelectorAllInRange', () => {
    beforeEach(() => {
        document.body.innerHTML = '';
    });

    it('finds matching elements inside a range', () => {
        const root = document.createElement('div');
        root.innerHTML = `
            <span class="match">one</span>
            <span class="nomatch">two</span>
            <div class="match">three</div>
        `;
        document.body.appendChild(root);

        const range = document.createRange();
        range.selectNodeContents(root);

        const found = querySelectorAllInRange(range, '.match');
        expect(found.length).toBe(2);
        expect(found[0].textContent?.trim()).toBe('one');
        expect(found[1].textContent?.trim()).toBe('three');
    });

    it('includes the commonAncestorContainer when it matches the selector', () => {
        const root = document.createElement('div');
        root.className = 'match';
        root.innerHTML = `<div class="inner">x</div>`;
        document.body.appendChild(root);

        // select contents of the root so commonAncestorContainer is `root`
        const range = document.createRange();
        range.selectNodeContents(root);

        const found = querySelectorAllInRange(range, '.match');
        // root should be returned because it matches and intersects the range
        expect(found.some((e) => e === root)).toBe(true);
    });

    it('does not return elements outside the range', () => {
        const root = document.createElement('div');
        root.innerHTML = `
            <div id="a" class="match">inside</div>
            <div id="b">middle</div>
            <div id="c" class="match">outside</div>
        `;
        document.body.appendChild(root);

        const a = root.querySelector('#a') as Element;
        const b = root.querySelector('#b') as Element;

        // create a range that covers only the first two nodes (a..b)
        const range = document.createRange();
        range.setStartBefore(a);
        range.setEndAfter(b);

        const found = querySelectorAllInRange(range, '.match');
        // only #a should be present
        expect(found.length).toBe(1);
        expect(found[0].id).toBe('a');
    });

    it('handles ranges whose commonAncestorContainer is a Text node', () => {
        const root = document.createElement('div');
        root.innerHTML = `
            <div class="match">hello world</div>
        `;
        document.body.appendChild(root);

        const el = root.querySelector('.match') as Element;
        const text = el.firstChild as Text;

        // Create a range wholly inside the text node so commonAncestorContainer
        // will be the Text node.
        const range = document.createRange();
        range.setStart(text, 1);
        range.setEnd(text, 2);

        const found = querySelectorAllInRange(range, '.match');
        expect(found.length).toBe(1);
        expect(found[0]).toBe(el);
    });
});

describe('closestNsAware', () => {
    beforeEach(() => {
        document.body.innerHTML = '';
    });

    it('finds an ancestor by CSS selector', () => {
        const outer = document.createElement('div');
        outer.className = 'foo';
        const inner = document.createElement('span');
        outer.appendChild(inner);
        document.body.appendChild(outer);

        const found = closestNsAware(inner, '.foo');
        expect(found).toBe(outer);
    });

    it('finds an ancestor by namespace-aware selector', () => {
        const ns = 'http://example.com/ns';
        const outer = document.createElementNS(ns, 'free_to_read');
        const inner = document.createElement('span');
        outer.appendChild(inner);
        document.body.appendChild(outer);

        const found = closestNsAware(inner, `{${ns}}free_to_read`);
        expect(found).toBe(outer);
    });

    it('matches wildcard namespace selector and supports comma-separated list', () => {
        const ns = 'http://another/ns';
        const outer = document.createElementNS(ns, 'free_to_read');
        const inner = document.createElement('i');
        outer.appendChild(inner);
        document.body.appendChild(outer);

        // include a CSS selector first which does not match, then the ns selector
        const found = closestNsAware(inner, '.does-not-exist, {*}free_to_read');
        expect(found).toBe(outer);
    });
});

describe('compileNsSelector and closestWithMatcher', () => {
    beforeEach(() => {
        document.body.innerHTML = '';
    });

    it('compileNsSelector returns a matcher for CSS selectors', () => {
        const el = document.createElement('div');
        el.className = 'a';
        document.body.appendChild(el);

        const matcher = compileNsSelector('.a');
        expect(matcher(el)).toBe(true);
        expect(matcher(document.body)).toBe(false);
    });

    it('compileNsSelector returns null for empty or undefined selector', () => {
        // @ts-ignore allow calling with undefined for test
        const m1 = compileNsSelector(undefined);
        expect(m1).toBeNull();

        const m2 = compileNsSelector('');
        expect(m2).toBeNull();
    });

    it('compileNsSelector returns a matcher for namespace selectors', () => {
        const ns = 'http://example.org/ns';
        const el = document.createElementNS(ns, 'free_to_read');
        document.body.appendChild(el);

        const matcher = compileNsSelector(`{${ns}}free_to_read`);
        expect(matcher(el)).toBe(true);
    });

    it('compileNsSelector supports comma-separated mixed selectors and closestWithMatcher finds ancestor', () => {
        const ns = 'http://mix/ns';
        const outer = document.createElementNS(ns, 'free_to_read');
        outer.className = 'outer';
        const mid = document.createElement('div');
        mid.className = 'mid';
        const inner = document.createElement('span');
        outer.appendChild(mid);
        mid.appendChild(inner);
        document.body.appendChild(outer);

        const matcher = compileNsSelector('.does-not-exist, {*}free_to_read');
        // matcher should match the outer element
        expect(matcher(outer)).toBe(true);

        // closestWithMatcher starting at inner should find outer
        const found = closestWithMatcher(inner, matcher);
        expect(found).toBe(outer);
    });
});

describe('extendHighlightOverProtectedElements', () => {
    it('wraps protected elements with new highlights and returned callback removes them and calls old callback', () => {
        document.body.innerHTML = `
            <div id="root">
                <div class="protected"><mark class="iaa-highlighted" data-iaa-id="v1">inside</mark></div>
                <mark class="iaa-highlighted" data-iaa-id="v1">outside</mark>
            </div>
        `;

        const root = document.getElementById('root') as Element;
        const protectedEl = root.querySelector('.protected') as Element;

        const range = document.createRange();
        range.selectNodeContents(root);

        let oldCalled = false;
        const oldCallback = () => {
            oldCalled = true;
        };

        const returned = extendHighlightOverProtectedElements(
            range,
            '.protected',
            `.iaa-highlighted[data-iaa-id="v1"]`,
            { 'data-iaa-id': 'v1' },
            oldCallback
        );

        // Protected element should be wrapped by a mark
        const wrapper = protectedEl.parentElement as Element;
        expect(wrapper).not.toBeNull();
        expect(wrapper.tagName.toLowerCase()).toBe('mark');
        expect(wrapper.getAttribute('data-iaa-id')).toBe('v1');

        // Calling returned callback removes wrapper and calls the old callback
        returned();
        expect(oldCalled).toBe(true);

        // After callback, the protected element should no longer be wrapped by a mark
        const afterParent = protectedEl.parentElement as Element;
        expect(afterParent).not.toBeNull();
        expect(afterParent.tagName.toLowerCase()).not.toBe('mark');
    });

    it('keeps only outermost protected elements when protected elements are nested', () => {
        document.body.innerHTML = `
            <div id="root">
                <div class="protected outer">
                    <div class="protected inner"><mark class="iaa-highlighted" data-iaa-id="v1">inside</mark></div>
                </div>
            </div>
        `;

        const root = document.getElementById('root') as Element;
        const outer = root.querySelector('.outer') as Element;
        const inner = root.querySelector('.inner') as Element;

        const range = document.createRange();
        range.selectNodeContents(root);

        let oldCalled = false;
        const oldCallback = () => {
            oldCalled = true;
        };

        const returned = extendHighlightOverProtectedElements(
            range,
            '.protected',
            `.iaa-highlighted[data-iaa-id="v1"]`,
            { 'data-iaa-id': 'v1' },
            oldCallback
        );

        // Only the outermost protected element should have been wrapped by a new mark
        const outerWrapper = outer.parentElement as Element;
        expect(outerWrapper).not.toBeNull();
        expect(outerWrapper.tagName.toLowerCase()).toBe('mark');
        expect(outerWrapper.getAttribute('data-iaa-id')).toBe('v1');

        // The inner protected element itself must NOT have been wrapped separately
        expect(inner.parentElement).not.toBeNull();
        expect(inner.parentElement?.tagName.toLowerCase()).toBe('div');

        // Cleanup should call the original callback
        returned();
        expect(oldCalled).toBe(true);
    });

    it('returns original callback when protection selector is undefined', () => {
        document.body.innerHTML = `<div><mark class="iaa-highlighted" data-iaa-id="v1">x</mark></div>`;
        const range = document.createRange();
        range.selectNodeContents(document.body);

        let called = false;
        const cb = () => (called = true);

        const returned = extendHighlightOverProtectedElements(
            range,
            undefined,
            `.iaa-highlighted[data-iaa-id="v1"]`,
            {},
            cb
        );
        returned();
        expect(called).toBe(true);
    });
});

describe('safeHighlightText', () => {
    it('wraps highlights and uses extendHighlightOverProtectedElements when data-iaa-id is present', () => {
        document.body.innerHTML = `
            <div id="root">
                <div class="protected">inside</div>
                <div class="outside">outside</div>
            </div>
        `;

        const root = document.getElementById('root') as Element;
        const protectedEl = root.querySelector('.protected') as Element;

        const range = document.createRange();
        range.selectNodeContents(root);

        // Call safeHighlightText with explicit protectedElementSelector
        const returned = safeHighlightText(range, '.protected', 'mark', { 'data-iaa-id': 'v1' });

        // The protected element should be wrapped by a mark
        const wrapper = protectedEl.parentElement as Element;
        expect(wrapper).not.toBeNull();
        expect(wrapper.tagName.toLowerCase()).toBe('mark');
        expect(wrapper.getAttribute('data-iaa-id')).toBe('v1');

        // Cleanup should remove wrapper
        returned();
        const afterParent = protectedEl.parentElement as Element;
        expect(afterParent).not.toBeNull();
        expect(afterParent.tagName.toLowerCase()).not.toBe('mark');
    });

    it('does not expand highlight inside MathML when math is not protected', () => {
        document.body.innerHTML = `
            <div id="root">
                before <math xmlns="http://www.w3.org/1998/Math/MathML"><mi>alpha</mi><mo>+</mo><mi>beta</mi></math> after
            </div>
        `;

        const mi = document.querySelector('mi') as Element;
        expect(mi).not.toBeNull();

        const range = document.createRange();
        range.selectNodeContents(mi);

        const returned = safeHighlightText(range, undefined, 'mark', { 'data-iaa-id': 'm-math' });

        // The mark should be inside the <mi>, not wrapping the <math>
        const mark = mi.querySelector('mark') as Element;
        expect(mark).not.toBeNull();
        const math = document.querySelector('math') as Element;
        expect(math.parentElement?.tagName.toLowerCase()).not.toBe('mark');

        returned();
    });

    it('expands highlight to wrap protected MathML element', () => {
        document.body.innerHTML = `
            <div id="root">
                before <math xmlns="http://www.w3.org/1998/Math/MathML"><mi>alpha</mi><mo>+</mo><mi>beta</mi></math> after
            </div>
        `;

        const mi = document.querySelector('mi') as Element;
        expect(mi).not.toBeNull();

        const range = document.createRange();
        range.selectNodeContents(mi);

        // Pass a namespace-aware selector matching the math element's namespace/localName
        const returned = safeHighlightText(
            range,
            '{http://www.w3.org/1998/Math/MathML}math',
            'mark',
            { 'data-iaa-id': 'm-prot' }
        );

        const math = document.querySelector('math') as Element;
        // The math element should now be wrapped by the mark
        expect(math.parentElement).not.toBeNull();
        expect(math.parentElement?.tagName.toLowerCase()).toBe('mark');
        expect(math.parentElement?.getAttribute('data-iaa-id')).toBe('m-prot');

        returned();
    });
});

describe('closestHighlight', () => {
    beforeEach(() => {
        document.body.innerHTML = '';
    });

    it('returns the closest highlight element for a text node inside a highlight', () => {
        document.body.innerHTML = `<div><mark data-iaa-id="v1">hello</mark></div>`;
        const mark = document.querySelector('mark') as HTMLElement;
        const text = mark.firstChild as Text;

        const found = closestHighlight(text);
        expect(found).toBe(mark);
    });

    it('returns the element itself when the element is a highlight', () => {
        document.body.innerHTML = `<div><mark data-iaa-id="v2">world</mark></div>`;
        const mark = document.querySelector('mark') as HTMLElement;

        const found = closestHighlight(mark);
        expect(found).toBe(mark);
    });

    it('returns null for nodes outside any highlight or for null target', () => {
        document.body.innerHTML = `<div><span>plain</span></div>`;
        const span = document.querySelector('span') as HTMLElement;
        const text = span.firstChild as Text;

        expect(closestHighlight(text)).toBeNull();
        expect(closestHighlight(null)).toBeNull();
    });
});

describe('getInlineLabelClientRect', () => {
    beforeEach(() => {
        document.body.innerHTML = '';
        // Ensure Range.prototype.getClientRects exists and can be mocked
        // (some jsdom environments may not implement this fully)
        // We'll override it in tests as needed.
    });

    it('computes rectangle when firstChild is a Text node using Range.getClientRects', () => {
        const mark = document.createElement('mark');
        mark.textContent = 'hello';
        document.body.appendChild(mark);

        // Mock highlight.getClientRects
        (mark as any).getClientRects = () => [
            { left: 10, top: 5, height: 12, right: 30, width: 20 },
        ];

        // Mock Range.getClientRects to return a rect for the text node
        const originalRangeGet = (Range.prototype as any).getClientRects;
        (Range.prototype as any).getClientRects = function () {
            return [{ left: 15, top: 7, height: 8, right: 90, width: 75 }];
        };

        const rect = getInlineLabelClientRect(mark);
        expect(rect.left).toBe(10);
        expect(rect.top).toBe(5);
        // width should be cr.left - r.left = 15 - 10 = 5
        expect(rect.width).toBe(5);
        expect(rect.height).toBe(12);

        // restore
        (Range.prototype as any).getClientRects = originalRangeGet;
    });

    it('computes rectangle when firstChild is an Element using child.getClientRects', () => {
        const mark = document.createElement('mark');
        const span = document.createElement('span');
        span.textContent = 'label';
        mark.appendChild(span);
        document.body.appendChild(mark);

        (mark as any).getClientRects = () => [
            { left: 20, top: 10, height: 6, right: 60, width: 40 },
        ];
        (span as any).getClientRects = () => [
            { left: 25, top: 12, height: 4, right: 50, width: 25 },
        ];

        const rect = getInlineLabelClientRect(mark);
        expect(rect.left).toBe(20);
        expect(rect.top).toBe(10);
        // width should be cr.left - r.left = 25 - 20 = 5
        expect(rect.width).toBe(5);
        expect(rect.height).toBe(6);
    });

    it('throws for unexpected node types (no firstChild)', () => {
        const mark = document.createElement('mark');
        document.body.appendChild(mark);
        expect(() => getInlineLabelClientRect(mark)).toThrow('Unexpected node type');
    });
});

describe('isPointInRect', () => {
    beforeEach(() => {
        document.body.innerHTML = '';
    });

    it('returns true for a point strictly inside the rect', () => {
        const rect = new DOMRect(10, 20, 30, 40); // left=10, top=20, width=30, height=40
        expect(isPointInRect({ x: 20, y: 30 }, rect)).toBe(true);
    });

    it('returns true for a point on the rect edge', () => {
        const rect = new DOMRect(0, 0, 10, 10);
        expect(isPointInRect({ x: 0, y: 5 }, rect)).toBe(true); // left edge
        expect(isPointInRect({ x: 10, y: 5 }, rect)).toBe(true); // right edge
        expect(isPointInRect({ x: 5, y: 0 }, rect)).toBe(true); // top edge
        expect(isPointInRect({ x: 5, y: 10 }, rect)).toBe(true); // bottom edge
    });

    it('returns false for a point outside the rect', () => {
        const rect = new DOMRect(0, 0, 10, 10);
        expect(isPointInRect({ x: -1, y: 5 }, rect)).toBe(false);
        expect(isPointInRect({ x: 5, y: 11 }, rect)).toBe(false);
    });
});

describe('removeClassFromAncestors', () => {
    beforeEach(() => {
        document.body.innerHTML = '';
    });

    it('removes the class from all ancestor elements', () => {
        const outer = document.createElement('div');
        outer.className = 'keep x';
        const middle = document.createElement('div');
        middle.className = 'x';
        const inner = document.createElement('div');
        outer.appendChild(middle);
        middle.appendChild(inner);
        document.body.appendChild(outer);

        removeClassFromAncestors(inner, 'x');

        expect(middle.classList.contains('x')).toBe(false);
        expect(outer.classList.contains('x')).toBe(false);
    });

    it('stops traversal at the provided root (inclusive)', () => {
        const outer = document.createElement('div');
        outer.className = 'x';
        const middle = document.createElement('div');
        middle.className = 'x';
        const inner = document.createElement('div');
        outer.appendChild(middle);
        middle.appendChild(inner);
        document.body.appendChild(outer);

        // stop at `middle`; `middle` should be cleared, `outer` should remain
        removeClassFromAncestors(inner, 'x', middle);

        expect(middle.classList.contains('x')).toBe(false);
        expect(outer.classList.contains('x')).toBe(true);
    });

    it('ignores errors thrown when removing class and continues', () => {
        const outer = document.createElement('div');
        outer.className = 'x';
        const middle = document.createElement('div');
        middle.className = 'x';
        const inner = document.createElement('div');
        outer.appendChild(middle);
        middle.appendChild(inner);
        document.body.appendChild(outer);

        // make outer.classList.remove throw
        const originalRemove = (outer.classList as any).remove;
        (outer.classList as any).remove = () => {
            throw new Error('boom');
        };

        expect(() => removeClassFromAncestors(inner, 'x')).not.toThrow();

        // middle should have been cleared, outer still contains 'x' because remove threw
        expect(middle.classList.contains('x')).toBe(false);
        expect(outer.classList.contains('x')).toBe(true);

        // restore original
        (outer.classList as any).remove = originalRemove;
    });
});

describe('groupHighlightsByVid', () => {
    it('groups highlights by data-iaa-id preserving order', () => {
        document.body.innerHTML = `
            <div>
                <mark class="iaa-highlighted" data-iaa-id="a">one</mark>
                <mark class="iaa-highlighted" data-iaa-id="b">two</mark>
                <mark class="iaa-highlighted" data-iaa-id="a">three</mark>
            </div>
        `;

        const list = document.querySelectorAll('.iaa-highlighted') as NodeListOf<Element>;
        const groups = groupHighlightsByVid(list);

        expect(groups.size).toBe(2);
        expect(groups.has('a')).toBe(true);
        expect(groups.has('b')).toBe(true);

        const a = groups.get('a')!;
        expect(a.length).toBe(2);
        expect(a[0].textContent?.trim()).toBe('one');
        expect(a[1].textContent?.trim()).toBe('three');

        const b = groups.get('b')!;
        expect(b.length).toBe(1);
        expect(b[0].textContent?.trim()).toBe('two');
    });

    it('returns empty map for empty list', () => {
        document.body.innerHTML = `<div></div>`;
        const list = document.querySelectorAll('.iaa-highlighted') as NodeListOf<Element>;
        const groups = groupHighlightsByVid(list);
        expect(groups.size).toBe(0);
    });
});

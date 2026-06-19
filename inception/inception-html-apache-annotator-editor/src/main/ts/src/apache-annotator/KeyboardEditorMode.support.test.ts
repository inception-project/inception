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
import { describe, it, expect, beforeEach } from 'vitest';

import {
    caretOffsetOf,
    findFirstWordStartOffset,
    isSelectionBackward,
    findProtectedBounds,
    safeCaretOffset,
} from './KeyboardEditorMode.support';

// Matcher used across the protected-element tests: an element is protected when it
// carries the `protected` class. Mirrors how the editor flags atomic content.
const isProtected = (el: Element) => el.classList.contains('protected');

beforeEach(() => {
    document.body.innerHTML = '';
});

describe('caretOffsetOf', () => {
    it('returns the absolute offset of the caret within the root', () => {
        document.body.innerHTML = `<div id="root"><span>abc</span><span>defg</span></div>`;
        const root = document.getElementById('root') as Element;
        const secondText = root.querySelectorAll('span')[1].firstChild as Text;

        const range = document.createRange();
        range.setStart(secondText, 2); // caret between 'de' and 'fg' -> 'abc' + 2 = 5
        range.collapse(true);

        expect(caretOffsetOf(root, range)).toBe(5);
    });

    it('returns 0 for a caret at the very start of the content', () => {
        document.body.innerHTML = `<div id="root">hello</div>`;
        const root = document.getElementById('root') as Element;
        const range = document.createRange();
        range.setStart(root.firstChild as Text, 0);
        range.collapse(true);

        expect(caretOffsetOf(root, range)).toBe(0);
    });

});

describe('findFirstWordStartOffset', () => {
    it('skips leading whitespace and returns the first non-space offset', () => {
        document.body.innerHTML = `<div id="root">   <span>  hi</span></div>`;
        const root = document.getElementById('root') as Element;
        // '   ' (3) + '  ' (2) = 5 before the 'h'.
        expect(findFirstWordStartOffset(root)).toBe(5);
    });

    it('returns the offset within the first text node that has content', () => {
        document.body.innerHTML = `<div id="root">ab</div>`;
        const root = document.getElementById('root') as Element;
        expect(findFirstWordStartOffset(root)).toBe(0);
    });

    it('returns 0 when the content is entirely whitespace', () => {
        document.body.innerHTML = `<div id="root">   <span>   </span></div>`;
        const root = document.getElementById('root') as Element;
        expect(findFirstWordStartOffset(root)).toBe(0);
    });
});

describe('isSelectionBackward', () => {
    it('is false for a forward selection within a single text node', () => {
        document.body.innerHTML = `<div id="root">abcdef</div>`;
        const text = (document.getElementById('root') as Element).firstChild as Text;
        expect(isSelectionBackward({ anchorNode: text, anchorOffset: 1, focusNode: text, focusOffset: 4 }))
            .toBe(false);
    });

    it('is true for a backward selection within a single text node', () => {
        document.body.innerHTML = `<div id="root">abcdef</div>`;
        const text = (document.getElementById('root') as Element).firstChild as Text;
        expect(isSelectionBackward({ anchorNode: text, anchorOffset: 4, focusNode: text, focusOffset: 1 }))
            .toBe(true);
    });

    it('is true when the focus node precedes the anchor node in document order', () => {
        document.body.innerHTML = `<div id="root"><span id="a">aa</span><span id="b">bb</span></div>`;
        const a = (document.getElementById('a') as Element).firstChild as Text;
        const b = (document.getElementById('b') as Element).firstChild as Text;
        // anchor in b, focus in a -> focus precedes anchor -> backward
        expect(isSelectionBackward({ anchorNode: b, anchorOffset: 0, focusNode: a, focusOffset: 0 }))
            .toBe(true);
        // anchor in a, focus in b -> forward
        expect(isSelectionBackward({ anchorNode: a, anchorOffset: 0, focusNode: b, focusOffset: 0 }))
            .toBe(false);
    });

    it('is false when either endpoint is missing', () => {
        expect(isSelectionBackward({ anchorNode: null, anchorOffset: 0, focusNode: null, focusOffset: 0 }))
            .toBe(false);
    });
});

describe('findProtectedBounds', () => {
    it('returns null when no matcher is configured', () => {
        document.body.innerHTML = `<div id="root"><span class="protected">x</span></div>`;
        const root = document.getElementById('root') as Element;
        const node = root.querySelector('.protected')!.firstChild;
        expect(findProtectedBounds(root, node, undefined)).toBeNull();
    });

    it('returns null when the node is not inside a protected element', () => {
        document.body.innerHTML = `<div id="root"><span>plain</span></div>`;
        const root = document.getElementById('root') as Element;
        const node = root.querySelector('span')!.firstChild;
        expect(findProtectedBounds(root, node, isProtected)).toBeNull();
    });

    it('returns the offset bounds of the enclosing protected element', () => {
        document.body.innerHTML = `<div id="root">ab<span class="protected">CDE</span>fg</div>`;
        const root = document.getElementById('root') as Element;
        const node = root.querySelector('.protected')!.firstChild;
        // 'ab' precedes the protected span (start 2), text 'CDE' has length 3 (end 5).
        expect(findProtectedBounds(root, node, isProtected)).toEqual({ start: 2, end: 5 });
    });

    it('walks up to the OUTERMOST protected ancestor when protected elements nest', () => {
        document.body.innerHTML =
            `<div id="root">ab<span class="protected outer">C<span class="protected inner">DE</span>F</span>g</div>`;
        const root = document.getElementById('root') as Element;
        const innerText = root.querySelector('.inner')!.firstChild;
        // Outer protected span starts after 'ab' (2), spans 'CDEF' (length 4) -> end 6.
        expect(findProtectedBounds(root, innerText, isProtected)).toEqual({ start: 2, end: 6 });
    });
});

describe('safeCaretOffset', () => {
    beforeEach(() => {
        document.body.innerHTML = `<div id="root">ab<span class="protected">CDE</span>fg</div>`;
    });

    it('snaps an offset inside a protected element to the protected start', () => {
        const root = document.getElementById('root') as Element;
        // Offset 3 lands inside 'CDE' (protected, offsets 2..5) -> snaps back to 2.
        expect(safeCaretOffset(root, 3, isProtected)).toBe(2);
    });

    it('leaves an offset outside any protected element unchanged', () => {
        const root = document.getElementById('root') as Element;
        expect(safeCaretOffset(root, 1, isProtected)).toBe(1);
    });

    it('returns the offset unchanged when no matcher is configured', () => {
        const root = document.getElementById('root') as Element;
        expect(safeCaretOffset(root, 3, undefined)).toBe(3);
    });
});

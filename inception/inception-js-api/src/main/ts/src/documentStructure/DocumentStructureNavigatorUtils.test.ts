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
import {
    type TocLevel,
    generateTOC,
    generateTOCIndex,
} from './DocumentStructureNavigatorUtils';

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
 * Build a synthetic section subtree using a custom element name. We avoid
 * `<section>` to stay clear of HTML's section-content semantics and any
 * special parser handling.
 */
function sec(title: string | undefined, ...children: Node[]): HTMLElement {
    const e = document.createElement('test-sec');
    if (title !== undefined) e.setAttribute('data-title', title);
    for (const c of children) e.appendChild(c);
    return e;
}

/** Default extractor used by the suite. */
function extractTitle(el: Element): string | undefined {
    return el.getAttribute('data-title') ?? undefined;
}

/**
 * Render the TOC as an indented title list for compact assertions.
 */
function outline(toc: TocLevel, depth = 0): string[] {
    const lines: string[] = [];
    for (const child of toc.children) {
        lines.push(`${'  '.repeat(depth)}${child.title ?? '(untitled)'}`);
        lines.push(...outline(child, depth + 1));
    }
    return lines;
}

describe('generateTOC', () => {
    it('returns an empty root TOC for an empty container', () => {
        const toc = generateTOC(container, 'test-sec', extractTitle);
        expect(toc.element).toBe(container);
        expect(toc.children).toEqual([]);
    });

    it('produces no children when sections lack titles', () => {
        container.appendChild(sec(undefined));
        container.appendChild(sec(undefined));
        const toc = generateTOC(container, 'test-sec', extractTitle);
        expect(toc.children).toEqual([]);
    });

    it('treats sibling top-level sections as direct children of the root', () => {
        container.appendChild(sec('A'));
        container.appendChild(sec('B'));
        const toc = generateTOC(container, 'test-sec', extractTitle);
        expect(outline(toc)).toEqual(['A', 'B']);
    });

    it('nests sections by DOM containment', () => {
        container.appendChild(sec('A', sec('A.1'), sec('A.2', sec('A.2.a'))));
        container.appendChild(sec('B'));
        const toc = generateTOC(container, 'test-sec', extractTitle);
        expect(outline(toc)).toEqual(['A', '  A.1', '  A.2', '    A.2.a', 'B']);
    });

    it('returns to the right ancestor when stepping out of a deep section', () => {
        container.appendChild(
            sec('A', sec('A.1', sec('A.1.a'))) // deep
        );
        container.appendChild(sec('B')); // sibling of A
        const toc = generateTOC(container, 'test-sec', extractTitle);
        expect(outline(toc)).toEqual(['A', '  A.1', '    A.1.a', 'B']);
        // B must be a child of the root, not of A or A.1
        const a = toc.children[0];
        const b = toc.children[1];
        expect(b.parent).toBe(toc);
        expect(b.parent).not.toBe(a);
    });

    it('skips untitled sections but threads their titled descendants up to the nearest titled ancestor', () => {
        // Outer 'A' has an untitled child wrapper that contains a titled grand-child.
        // The grand-child should appear as a direct child of A in the TOC.
        container.appendChild(sec('A', sec(undefined, sec('A.X'))));
        const toc = generateTOC(container, 'test-sec', extractTitle);
        expect(outline(toc)).toEqual(['A', '  A.X']);
        expect(toc.children[0].children[0].parent).toBe(toc.children[0]);
    });

    it('populates element / title / parent / children consistently on each level', () => {
        const outer = sec('A', sec('A.1'));
        container.appendChild(outer);
        const toc = generateTOC(container, 'test-sec', extractTitle);

        const a = toc.children[0];
        expect(a.title).toBe('A');
        expect(a.element).toBe(outer);
        expect(a.parent).toBe(toc);
        expect(a.children.length).toBe(1);

        const a1 = a.children[0];
        expect(a1.title).toBe('A.1');
        expect(a1.parent).toBe(a);
        expect(a1.children).toEqual([]);
    });

    it('only matches elements with the given selector', () => {
        container.appendChild(sec('match'));
        const other = document.createElement('not-a-sec');
        other.setAttribute('data-title', 'ignore');
        container.appendChild(other);
        const toc = generateTOC(container, 'test-sec', extractTitle);
        expect(outline(toc)).toEqual(['match']);
    });
});

describe('generateTOCIndex', () => {
    it('returns an empty map for an empty TOC', () => {
        const toc = generateTOC(container, 'test-sec', extractTitle);
        expect(generateTOCIndex(toc)).toEqual({});
    });

    it('indexes every titled section by its element id', () => {
        const a = sec('A');
        a.id = 'a';
        const a1 = sec('A.1');
        a1.id = 'a-1';
        a.appendChild(a1);
        const b = sec('B');
        b.id = 'b';
        container.appendChild(a);
        container.appendChild(b);

        const toc = generateTOC(container, 'test-sec', extractTitle);
        const idx = generateTOCIndex(toc);

        expect(Object.keys(idx).sort()).toEqual(['a', 'a-1', 'b']);
        expect(idx['a'].title).toBe('A');
        expect(idx['a-1'].title).toBe('A.1');
        expect(idx['b'].title).toBe('B');
    });

    it('skips sections that have no id', () => {
        const a = sec('A'); // no id
        const a1 = sec('A.1');
        a1.id = 'a-1';
        a.appendChild(a1);
        container.appendChild(a);

        const idx = generateTOCIndex(generateTOC(container, 'test-sec', extractTitle));
        expect(Object.keys(idx)).toEqual(['a-1']);
    });
});

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
import { HtmlDocumentStructure } from './HtmlDocumentStructure';

let container: HTMLElement;

beforeEach(() => {
    container = document.createElement('div');
    document.body.appendChild(container);
});

afterEach(() => {
    // Several tests query via `container.ownerDocument` (e.g. for id
    // uniqueness in the collision test). Stale containers and stale ids left
    // by earlier tests would otherwise leak across cases and make
    // assertions order-dependent.
    while (document.body.firstChild) {
        document.body.removeChild(document.body.firstChild);
    }
});

/**
 * Concatenate all text-node data in document order. Used to verify that the
 * wrapping pass does not reorder text content -- the invariant that
 * annotation character offsets depend on.
 */
function textInOrder(root: Node): string {
    const walker = document.createTreeWalker(root, NodeFilter.SHOW_TEXT, null);
    let out = '';
    let n: Node | null;
    while ((n = walker.nextNode())) out += n.nodeValue ?? '';
    return out;
}

/**
 * Build a simple `tag>title|tag>title` outline string from the synthetic
 * section tree rooted at `root`, for compact assertions about the resulting
 * shape.
 */
function outline(root: Element): string[] {
    const result: string[] = [];
    const walker = root.ownerDocument.createTreeWalker(
        root,
        NodeFilter.SHOW_ELEMENT,
        (n) =>
            (n as Element).localName === 'sec-wrap'
                ? NodeFilter.FILTER_ACCEPT
                : NodeFilter.FILTER_SKIP
    );
    const depth = new Map<Element, number>();
    let cur: Element | null;
    while ((cur = walker.nextNode() as Element | null)) {
        // Compute nesting depth by walking up through sec-wrap ancestors.
        let d = 0;
        let p: Element | null = cur.parentElement;
        while (p && p !== root) {
            if (p.localName === 'sec-wrap') d++;
            p = p.parentElement;
        }
        depth.set(cur, d);
        const heading = cur.querySelector(':scope > h1, :scope > h2, :scope > h3, :scope > h4, :scope > h5, :scope > h6');
        const title = heading?.textContent?.trim() ?? '?';
        result.push(`${'  '.repeat(d)}${heading?.localName ?? '?'}: ${title}`);
    }
    return result;
}

describe('HtmlDocumentStructure', () => {
    describe('preprocess (heading wrapping)', () => {
        it('is a no-op on a document with no headings', () => {
            container.innerHTML = '<p>one</p><p>two</p>';
            const before = container.innerHTML;
            new HtmlDocumentStructure().preprocess(container);
            expect(container.querySelectorAll('sec-wrap').length).toBe(0);
            expect(container.innerHTML).toBe(before);
        });

        it('wraps a single heading and its trailing content', () => {
            container.innerHTML = '<h1>A</h1><p>body</p>';
            new HtmlDocumentStructure().preprocess(container);
            const wraps = container.querySelectorAll('sec-wrap');
            expect(wraps.length).toBe(1);
            expect(wraps[0].firstElementChild?.localName).toBe('h1');
            expect(wraps[0].querySelector('p')?.textContent).toBe('body');
        });

        it('nests h2 under preceding h1', () => {
            container.innerHTML = '<h1>A</h1><p/><h2>B</h2><p/>';
            new HtmlDocumentStructure().preprocess(container);
            expect(outline(container)).toEqual(['h1: A', '  h2: B']);
        });

        it('opens a sibling section on equal-level heading', () => {
            container.innerHTML = '<h1>A</h1><h1>B</h1>';
            new HtmlDocumentStructure().preprocess(container);
            expect(outline(container)).toEqual(['h1: A', 'h1: B']);
        });

        it('returns to outer level on higher-rank heading', () => {
            container.innerHTML =
                '<h1>A</h1><h2>A.1</h2><h3>A.1.a</h3><h2>A.2</h2><h1>B</h1>';
            new HtmlDocumentStructure().preprocess(container);
            expect(outline(container)).toEqual([
                'h1: A',
                '  h2: A.1',
                '    h3: A.1.a',
                '  h2: A.2',
                'h1: B',
            ]);
        });

        it('recurses into wrapper elements without direct headings', () => {
            // body has no direct heading; main and article wrap the content
            container.innerHTML =
                '<main><article><h1>A</h1><p/><h2>B</h2></article></main>';
            new HtmlDocumentStructure().preprocess(container);
            // The wrap happens inside the deepest wrapper that actually holds
            // headings (the <article>). The outline still threads through.
            expect(outline(container)).toEqual(['h1: A', '  h2: B']);
        });

        it('recurses into wrapper siblings when outer container has direct headings (regression)', () => {
            // Copilot reviewer case: outer has h1 AND a <section> with nested h2.
            // Before fix the h2 was silently dropped from the outline.
            container.innerHTML =
                '<h1>A</h1><section><h2>B</h2></section>';
            new HtmlDocumentStructure().preprocess(container);
            expect(outline(container)).toEqual(['h1: A', '  h2: B']);
        });

        it('preserves document-order text across the wrap pass', () => {
            container.innerHTML =
                '\n  <h1>A</h1>\n  <p>one</p>\n  <h2>B</h2>\n  <p>two</p>\n';
            const before = textInOrder(container);
            new HtmlDocumentStructure().preprocess(container);
            const after = textInOrder(container);
            expect(after).toBe(before);
        });

        it('preserves text-node order with text/comments interleaved between elements', () => {
            // Annotation character offsets are sensitive to the order in
            // which text nodes appear when walking the DOM. Interleaved
            // whitespace text nodes must move with the elements they sit
            // between, not be left behind.
            container.appendChild(document.createTextNode('before-h1 '));
            const h1 = document.createElement('h1');
            h1.textContent = 'Heading';
            container.appendChild(h1);
            container.appendChild(document.createTextNode(' between '));
            const p = document.createElement('p');
            p.textContent = 'Body';
            container.appendChild(p);
            container.appendChild(document.createTextNode(' after'));
            const before = textInOrder(container);

            new HtmlDocumentStructure().preprocess(container);

            const after = textInOrder(container);
            expect(after).toBe(before);
        });

        it('preserves comment nodes in document order', () => {
            container.appendChild(document.createComment('c1'));
            const h1 = document.createElement('h1');
            h1.textContent = 'A';
            container.appendChild(h1);
            container.appendChild(document.createComment('c2'));

            const collect = () => {
                const out: string[] = [];
                const w = document.createTreeWalker(
                    container,
                    NodeFilter.SHOW_COMMENT | NodeFilter.SHOW_TEXT
                );
                let n: Node | null;
                while ((n = w.nextNode())) {
                    out.push(
                        `${n.nodeType === Node.COMMENT_NODE ? 'C' : 'T'}:${n.nodeValue}`
                    );
                }
                return out;
            };
            const before = collect();

            new HtmlDocumentStructure().preprocess(container);

            expect(collect()).toEqual(before);
        });

        it('skips generated IDs that already exist in the document', () => {
            // An imported HTML file might already carry an id colliding with
            // our generated prefix. We must not introduce a duplicate id.
            const preExisting = document.createElement('div');
            preExisting.id = 'sec-wrap-0';
            container.appendChild(preExisting);

            const heading = document.createElement('h1');
            heading.textContent = 'A';
            container.appendChild(heading);

            new HtmlDocumentStructure().preprocess(container);

            const generated = container.querySelector('sec-wrap');
            expect(generated).not.toBeNull();
            expect(generated!.id).not.toBe('sec-wrap-0');
            // pre-existing element is untouched
            expect(preExisting.id).toBe('sec-wrap-0');
            // and the generated id is unique
            expect(
                container.ownerDocument.querySelectorAll(`[id="${generated!.id}"]`).length
            ).toBe(1);
        });

        it('sets data-level to the heading rank', () => {
            container.innerHTML = '<h2>X</h2><h4>Y</h4>';
            new HtmlDocumentStructure().preprocess(container);
            const wraps = container.querySelectorAll('sec-wrap');
            expect(wraps[0].getAttribute('data-level')).toBe('2');
            expect(wraps[1].getAttribute('data-level')).toBe('4');
        });
    });

    describe('extractTitle', () => {
        it('returns the trimmed text of a direct child heading', () => {
            container.innerHTML = '<sec-wrap><h2>  Section title  </h2></sec-wrap>';
            const section = container.querySelector('sec-wrap')!;
            expect(new HtmlDocumentStructure().extractTitle(section)).toBe(
                'Section title'
            );
        });

        it('returns undefined if there is no direct child heading', () => {
            container.innerHTML = '<sec-wrap><div><h2>nope</h2></div></sec-wrap>';
            const section = container.querySelector('sec-wrap')!;
            expect(new HtmlDocumentStructure().extractTitle(section)).toBeUndefined();
        });

        it('returns undefined if the heading is empty/whitespace', () => {
            container.innerHTML = '<sec-wrap><h2>   </h2></sec-wrap>';
            const section = container.querySelector('sec-wrap')!;
            expect(new HtmlDocumentStructure().extractTitle(section)).toBeUndefined();
        });
    });

    describe('scrollTarget', () => {
        it('returns the direct child heading when one is present', () => {
            container.innerHTML = '<sec-wrap><h2>X</h2><p/></sec-wrap>';
            const section = container.querySelector('sec-wrap')!;
            const heading = section.querySelector('h2')!;
            expect(new HtmlDocumentStructure().scrollTarget(section)).toBe(heading);
        });

        it('falls back to the section element when no direct heading exists', () => {
            container.innerHTML = '<sec-wrap><div>x</div></sec-wrap>';
            const section = container.querySelector('sec-wrap')!;
            expect(new HtmlDocumentStructure().scrollTarget(section)).toBe(section);
        });
    });

    describe('sectionSelector', () => {
        it('matches the synthetic wrapper element', () => {
            const s = new HtmlDocumentStructure();
            expect(s.sectionSelector).toBe('sec-wrap');
            container.innerHTML = '<sec-wrap/><div/><sec-wrap/>';
            expect(container.querySelectorAll(s.sectionSelector).length).toBe(2);
        });
    });
});

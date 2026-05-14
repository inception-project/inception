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
import type { DocumentStructureStrategy } from '@inception-project/inception-js-api';

const HEADING_SELECTOR = 'h1, h2, h3, h4, h5, h6';
const DIRECT_HEADING_SELECTOR =
    ':scope > h1, :scope > h2, :scope > h3, :scope > h4, :scope > h5, :scope > h6';

/**
 * Document structure strategy for HTML.
 *
 * HTML has no explicit notion of section nesting -- headings (h1-h6) are
 * siblings of the content they head, not ancestors. This strategy wraps
 * heading-delimited regions of the document into nested section elements so
 * that content following a heading becomes a descendant of that heading's
 * section.
 *
 * The wrapping preserves document order for ALL node types (elements, text,
 * comments) so that the character-offset positions used by annotations
 * remain unchanged.
 */
export class HtmlDocumentStructure implements DocumentStructureStrategy {
    readonly sectionSelector = 'sec-wrap';

    preprocess(container: Element): void {
        const counter = { n: 0 };
        this.wrapHeadingSectionsIn(container, counter);
    }

    extractTitle(section: Element): string | undefined {
        const heading = section.querySelector(DIRECT_HEADING_SELECTOR);
        const text = heading?.textContent?.trim();
        return text || undefined;
    }

    scrollTarget(section: Element): Element {
        return section.querySelector(DIRECT_HEADING_SELECTOR) ?? section;
    }

    private wrapHeadingSectionsIn(container: Element, counter: { n: number }) {
        const doc = container.ownerDocument;
        const isHeading = (node: Node): node is HTMLElement =>
            node.nodeType === Node.ELEMENT_NODE &&
            /^h[1-6]$/i.test((node as Element).localName);

        // Snapshot ALL child nodes (elements, text, comments) so we can move
        // them in document order. Iterating .children would drop text nodes,
        // leaving them stranded at the top level and changing the order in
        // which text appears -- which would shift annotation offsets.
        const childNodes = [...container.childNodes];

        // If there are no headings directly here, recurse into element
        // children that themselves contain headings. This handles documents
        // wrapped in <article>, <main>, etc. Text/comment nodes at this
        // level are left in place, preserving their document position.
        const hasDirectHeading = childNodes.some(isHeading);
        if (!hasDirectHeading) {
            for (const node of childNodes) {
                if (node.nodeType !== Node.ELEMENT_NODE) continue;
                const el = node as Element;
                if (el.querySelector(HEADING_SELECTOR)) {
                    this.wrapHeadingSectionsIn(el, counter);
                }
            }
            return;
        }

        const stack: { level: number; section: HTMLElement }[] = [];
        for (const node of childNodes) {
            if (isHeading(node)) {
                const level = parseInt(node.localName.charAt(1), 10);
                while (stack.length && stack[stack.length - 1].level >= level) {
                    stack.pop();
                }
                const section = doc.createElement('sec-wrap');
                section.setAttribute('data-level', String(level));
                section.id = `sec-wrap-${counter.n++}`;
                const parent = stack.length ? stack[stack.length - 1].section : container;
                parent.appendChild(section);
                section.appendChild(node);
                stack.push({ level, section });
            } else {
                const parent = stack.length ? stack[stack.length - 1].section : container;
                parent.appendChild(node);
            }
        }
    }
}

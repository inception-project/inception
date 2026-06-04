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

const XHTML_NS = 'http://www.w3.org/1999/xhtml';
const WRAPPER_TAG = 'sec-wrap';

/**
 * Make every configured section element pinnable by the layout code.
 *
 * The visualizer pins each section's width/height by writing inline styles, but
 * it only operates on HTMLElement (see the `instanceof HTMLElement` guard in
 * ApacheAnnotatorVisualizer.materializeWidthAndHeight). A section element in a
 * non-HTML XML namespace is a plain Element with no working `.style` -- the CSS
 * engine ignores its `style` attribute even when set -- so it is silently
 * skipped, which collapses the document area in Safari/Firefox (#6091).
 *
 * To make such an element pinnable it must become an HTML element, so this wraps
 * each one in a synthetic XHTML-namespace {@code <sec-wrap>} and uses the
 * wrapper as the section boundary. The wrap is structural only: no text nodes
 * are added, removed, or reordered, so annotation character offsets are
 * unchanged. The original element stays as the wrapper's only child, so format
 * CSS keyed on its tag name still applies. Elements already in the HTML
 * namespace are left as-is -- they are already pinnable.
 *
 * This is purely a rendering fix and knows nothing about document structure
 * (titles, headings, outline): that is the navigator's concern, handled
 * separately by the {@link DocumentStructureStrategy}.
 *
 * @returns the CSS selector the editor should use for "a section" afterwards:
 *   {@code 'sec-wrap'} when any wrapping happened, the original element-name
 *   selector when every section was already HTML, or {@code ''} when no section
 *   elements were configured.
 */
export function normalizeSectionsForPinning(
    container: Element,
    sectionElementLocalNames: Set<string>
): string {
    if (sectionElementLocalNames.size === 0) return '';

    const doc = container.ownerDocument;
    const selector = [...sectionElementLocalNames].join(',');
    if (!doc) return selector;

    // Snapshot so we can mutate the tree while iterating. Document order means
    // an ancestor is wrapped before its descendants, which is safe: a descendant
    // moves into the new wrapper and is still wrapped on a later iteration.
    const targets = Array.from(container.querySelectorAll(selector));

    let wrapped = false;
    for (const el of targets) {
        if (el.namespaceURI === XHTML_NS) continue; // already pinnable

        const parent = el.parentNode;
        if (!parent) continue;
        // Idempotent: skip if already wrapped (e.g. preprocess run twice).
        if (
            parent.nodeType === Node.ELEMENT_NODE &&
            (parent as Element).localName === WRAPPER_TAG
        ) {
            continue;
        }

        const wrapper = doc.createElementNS(XHTML_NS, WRAPPER_TAG);
        wrapper.setAttribute('data-section-type', el.localName);
        parent.insertBefore(wrapper, el);
        wrapper.appendChild(el);
        wrapped = true;
    }

    // If every section was already HTML, no wrappers exist and the section
    // boundaries are still the original elements.
    return wrapped ? WRAPPER_TAG : selector;
}

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
 *   - {@code ''} when no section elements were configured.
 *   - the original element-name selector (e.g. {@code 'div,p'}) when nothing
 *     was wrapped because every matching section was already HTML.
 *   - {@code 'sec-wrap'} when every matching section got wrapped.
 *   - a combined selector (e.g.
 *     {@code 'sec-wrap, :is(div,p):not(sec-wrap *)'}) when the document mixes
 *     already-HTML sections (left in place) with namespaced ones (wrapped), so
 *     downstream targets the wrappers AND the still-unwrapped HTML sections.
 *     The {@code :not(sec-wrap *)} keeps the original elements now buried inside
 *     a wrapper from matching twice.
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

    // Track the resulting DOM (not just this run's actions) so the returned
    // selector is correct even on a re-run or a mixed document.
    let hasWrappers = false;
    let hasUnwrapped = false;
    for (const el of targets) {
        if (el.namespaceURI === XHTML_NS) {
            hasUnwrapped = true; // already pinnable, stays as itself
            continue;
        }

        const parent = el.parentNode;
        if (!parent) continue;
        // Idempotent: skip if already wrapped (e.g. run twice on the same tree).
        if (
            parent.nodeType === Node.ELEMENT_NODE &&
            (parent as Element).localName === WRAPPER_TAG
        ) {
            hasWrappers = true;
            continue;
        }

        const wrapper = doc.createElementNS(XHTML_NS, WRAPPER_TAG);
        wrapper.setAttribute('data-section-type', el.localName);
        parent.insertBefore(wrapper, el);
        wrapper.appendChild(el);
        hasWrappers = true;
    }

    if (!hasWrappers) return selector;
    if (!hasUnwrapped) return WRAPPER_TAG;
    // Mixed: target wrappers plus the still-unwrapped originals. The
    // :not(sec-wrap *) guard excludes originals now buried inside a wrapper so
    // they don't match alongside their own wrapper.
    return `${WRAPPER_TAG}, :is(${selector}):not(${WRAPPER_TAG} *)`;
}

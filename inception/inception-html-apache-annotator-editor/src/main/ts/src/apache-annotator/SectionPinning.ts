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
// Attribute stamped on every synthetic wrapper (and only on those). Selecting by
// it -- rather than by the bare tag name -- avoids matching a buried namespaced
// original that happens to share the local name 'sec-wrap'. `querySelectorAll`
// matches local names ignoring namespace, so the tag name alone is ambiguous.
const WRAPPER_ATTR = 'data-section-type';
const WRAPPER_SELECTOR = `${WRAPPER_TAG}[${WRAPPER_ATTR}]`;

/**
 * Whether {@code node} is one of *our* synthetic wrappers. The single source of
 * truth for "is this a wrapper" across this module: a wrapper is an XHTML-
 * namespace {@code <sec-wrap>} carrying the {@code data-section-type} attribute
 * we stamp on it. Checking the attribute (not just tag + namespace) is what
 * distinguishes our wrapper from an unrelated {@code <sec-wrap>} that some other
 * markup might contain. Equivalent to matching {@link WRAPPER_SELECTOR}.
 */
function isSyntheticWrapper(node: Node | null): node is Element {
    return (
        node != null &&
        node.nodeType === Node.ELEMENT_NODE &&
        (node as Element).localName === WRAPPER_TAG &&
        (node as Element).namespaceURI === XHTML_NS &&
        (node as Element).hasAttribute(WRAPPER_ATTR)
    );
}

/**
 * Escape a section element's local name for safe use as a CSS type selector.
 * The names come straight from format configuration ({@code props.sectionElements})
 * and XML local names legitimately contain characters that are special in CSS --
 * e.g. a dot ({@code <foo.bar>} would otherwise parse as element {@code foo}
 * class {@code bar} and silently match nothing) or a leading digit (which throws
 * {@code DOMException: Invalid selector} and would abort editor init). Prefer the
 * platform {@code CSS.escape}; fall back to a minimal escaper when it is absent
 * (e.g. the jsdom test environment).
 */
function escapeName(name: string): string {
    const css = (globalThis as { CSS?: { escape?: (s: string) => string } }).CSS;
    if (css?.escape) return css.escape(name);
    // Minimal fallback: backslash-escape every non-(alphanumeric/-/_) character,
    // and a leading digit, matching the relevant parts of CSS.escape's contract.
    return name.replace(/^[0-9]/, (d) => `\\3${d} `).replace(/[^a-zA-Z0-9\-_]/g, (c) => `\\${c}`);
}

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
 * Wrappers are identified by the {@code data-section-type} attribute they carry
 * (selector {@code sec-wrap[data-section-type]}), never by the bare tag name:
 * {@code querySelectorAll} matches local names ignoring namespace, so a buried
 * namespaced original whose local name is also {@code sec-wrap} would otherwise
 * be mistaken for a real wrapper.
 *
 * @returns the CSS selector the editor should use for "a section" afterwards:
 *   - {@code ''} when no section elements were configured.
 *   - the original element-name selector (e.g. {@code 'div,p'}) when nothing
 *     was wrapped because every matching section was already HTML.
 *   - {@code 'sec-wrap[data-section-type]'} when every matching section got
 *     wrapped.
 *   - a combined selector (e.g. {@code 'sec-wrap[data-section-type],
 *     :is(div,p):not(sec-wrap[data-section-type] > *)'}) when the document mixes
 *     already-HTML sections (left in place) with namespaced ones (wrapped), so
 *     downstream targets the wrappers AND the still-unwrapped HTML sections. The
 *     {@code :not(sec-wrap[data-section-type] > *)} guard uses the child
 *     combinator so it excludes only each wrapper's direct buried original, not
 *     a genuine HTML section nested deeper inside a wrapped ancestor.
 */
export function normalizeSectionsForPinning(
    container: Element,
    sectionElementLocalNames: Set<string>
): string {
    if (sectionElementLocalNames.size === 0) return '';

    const doc = container.ownerDocument;
    // Escape each name: XML local names may contain CSS-special characters.
    const selector = [...sectionElementLocalNames].map(escapeName).join(',');
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
        // A synthetic wrapper we created earlier (only reachable when the config
        // literally lists 'sec-wrap', or on a re-run): count it as a wrapper, not
        // an unwrapped HTML section, so the returned selector stays stable.
        if (isSyntheticWrapper(el)) {
            hasWrappers = true;
            continue;
        }
        if (el.namespaceURI === XHTML_NS) {
            hasUnwrapped = true; // already pinnable, stays as itself
            continue;
        }

        const parent = el.parentNode;
        if (!parent) continue;
        // Idempotent: skip if already wrapped (e.g. run twice on the same tree).
        // Only one of *our* synthetic wrappers counts -- a namespaced
        // <foo:sec-wrap>, or any unrelated <sec-wrap> without our marker
        // attribute, is NOT a wrapper, is still a non-HTMLElement, and must
        // itself be wrapped to be pinnable.
        if (isSyntheticWrapper(parent)) {
            hasWrappers = true;
            continue;
        }

        const wrapper = doc.createElementNS(XHTML_NS, WRAPPER_TAG);
        wrapper.setAttribute(WRAPPER_ATTR, el.localName);
        parent.insertBefore(wrapper, el);
        wrapper.appendChild(el);
        hasWrappers = true;
    }

    if (!hasWrappers) return selector;
    if (!hasUnwrapped) return WRAPPER_SELECTOR;
    // Mixed: target the synthetic wrappers plus the still-unwrapped originals.
    // The first half matches only real wrappers (via the data-section-type
    // attribute). The :not(...) guard uses the CHILD combinator -- a buried
    // original is always the *direct* child of its wrapper (we appendChild it),
    // so `WRAPPER_SELECTOR > *` excludes exactly those originals. A descendant
    // combinator (`WRAPPER_SELECTOR *`) would over-exclude: a genuine already-
    // HTML section nested deeper inside a wrapped namespaced ancestor would be
    // wrongly dropped from the section set.
    return `${WRAPPER_SELECTOR}, :is(${selector}):not(${WRAPPER_SELECTOR} > *)`;
}

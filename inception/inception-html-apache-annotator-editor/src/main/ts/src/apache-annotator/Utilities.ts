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

import { highlightText } from '@apache-annotator/dom';
import type { VID } from '@inception-project/inception-js-api/src/model/VID';

export interface SelectionLike {
    anchorNode: Node | null | undefined;
    anchorOffset: number;
    focusNode: Node | null | undefined;
    focusOffset: number;
}

/**
 * Return all Element nodes that match a CSS selector and intersect a given DOM Range.
 *
 * @param {Range} range - The DOM Range whose intersection with candidate elements is tested.
 * @param {string} selector - CSS selector used to filter elements.
 * @returns {Element[]} An array of elements that match `selector` and intersect the `range`.
 */
export function querySelectorAllInRange(range: Range, selector: string): Element[] {
    // Normalize the TreeWalker root to an Element. When the range's
    // commonAncestorContainer is a Text node the TreeWalker would have no
    // Element descendants and the function would return an empty list even
    // though ancestor Elements (including the commonAncestorContainer's
    // parent) may intersect the range. Using the parent Element as root
    // ensures those ancestors are considered.
    const walkerRoot: Node =
        range.commonAncestorContainer instanceof Element
            ? range.commonAncestorContainer
            : (range.commonAncestorContainer.parentElement as Node) ||
              range.commonAncestorContainer;

    // console.log(`Searching in ${walkerRoot.textContent}`)
    const walker = document.createTreeWalker(walkerRoot, NodeFilter.SHOW_ELEMENT, {
        acceptNode: (node: Node) => {
            return range.intersectsNode(node) ? NodeFilter.FILTER_ACCEPT : NodeFilter.FILTER_REJECT;
        },
    });

    const result: Element[] = [];
    // Start at the root node instead of skipping it
    let current = walker.currentNode as Element;

    while (current) {
        // We must verify the initial node is an Element and intersects,
        // as the TreeWalker does not run the acceptNode filter on the starting node.
        if (
            current instanceof Element &&
            range.intersectsNode(current) &&
            current.matches(selector)
        ) {
            result.push(current);
        }
        current = walker.nextNode() as Element;
    }
    return result;
}

/**
 * Locates highlights generated for the annotation the specified id in the given range. If these
 * highlights have been created within the boundaries of a protected element, the highlights in
 * this element are removed and a highlight surrounding the protected element is created instead.
 * The new highlight may be larger than the original ones as it always at least contains the
 * protected element.
 *
 * @param target the range where the highlight is to be created
 * @param protectedElementsSelector CSS selector string or matcher function for protected elements
 * @param id VID of the annotation
 * @param attributes attributes of the annotation highlight
 * @param callback callback to remove the highlight
 * @returns updated callback to remove the highlight
 */
export function extendHighlightOverProtectedElements(
    target: Range,
    protectedElementsSelector: string | ((el: Element) => boolean) | undefined,
    highlightSelector: string,
    attributes: Record<string, string>,
    callback: () => void
) {
    if (!protectedElementsSelector) return callback;

    let protectedElementsMatcher: ((el: Element) => boolean) | null;
    if (typeof protectedElementsSelector === 'function') {
        protectedElementsMatcher = protectedElementsSelector as (el: Element) => boolean;
    } else {
        protectedElementsMatcher = compileNsSelector(protectedElementsSelector);
    }

    if (!protectedElementsMatcher) return callback;

    // Clone the range so we don't mutate the caller's target range unexpectedly
    const searchRange = target.cloneRange();

    // Helper to get an element from a node (handles Text nodes)
    const elementFor = (node: Node): Element | null => {
        return node instanceof Element ? node : node.parentElement;
    };

    const startEl = elementFor(searchRange.startContainer);
    const endEl = elementFor(searchRange.endContainer);

    // Expand the range such that the start is before the closest protected ancestor of the start container and the end
    // is after the closest protected ancestor of the end container. This ensures that all protected elements
    // intersecting with the annotation are included in the range.
    const startProtectedAncestor = startEl
        ? closestWithMatcher(startEl, protectedElementsMatcher)
        : null;
    const endProtectedAncestor = endEl ? closestWithMatcher(endEl, protectedElementsMatcher) : null;

    if (startProtectedAncestor) {
        searchRange.setStartBefore(startProtectedAncestor);
    }
    if (endProtectedAncestor) {
        searchRange.setEndAfter(endProtectedAncestor);
    }

    // Find all protected elements that contain highlights for the annotation and remove the highlights
    const protectedElements = new Set<Element>();
    const highlights = querySelectorAllInRange(searchRange, highlightSelector);
    for (const highlight of highlights) {
        let protectedElement = closestWithMatcher(highlight as Element, protectedElementsMatcher);
        if (protectedElement == null) continue;

        // Climb to the outermost protected ancestor for this highlight so that
        // nested protected elements yield only the outermost wrapper (avoid
        // creating nested mark wrappers). This ensures highlights inside an
        // inner protected element cause the outer protected ancestor to be
        // wrapped instead of wrapping both inner and outer.
        let outer = protectedElement;
        let p = protectedElement.parentElement;
        while (p && protectedElementsMatcher(p)) {
            outer = p;
            p = p.parentElement;
        }
        protectedElements.add(outer);
        highlight.after(...highlight.childNodes);
        highlight.remove();
    }

    // Filter protected elements to keep only the outermost ones. If protected
    // elements are nested the inner ones should be discarded so we don't create
    // nested wrappers (the backend expands to the outermost protected span).
    const protectedElementsArr = Array.from(protectedElements);
    const outerProtectedElements = protectedElementsArr.filter(
        (el) => !protectedElementsArr.some((other) => other !== el && other.contains(el))
    );

    // Add new highlights surrounding the (outermost) protected elements
    const newHighlights: Element[] = [];
    for (const protectedElement of outerProtectedElements) {
        const highlight = document.createElement('mark');
        if (attributes) {
            for (const name in attributes) {
                highlight.setAttribute(name, attributes[name]);
            }
        }
        const range = document.createRange();
        range.selectNode(protectedElement);
        range.surroundContents(highlight);
        newHighlights.push(highlight);
    }

    // Extend the callback to also remove the new highlights
    const oldCallback = callback;
    callback = () => {
        newHighlights.forEach((e) => {
            e.after(...e.childNodes);
            e.remove();
        });
        oldCallback();
    };
    return callback;
}

/**
 * Highlight a DOM Range and, when relevant, extend the highlight over
 * protected elements.
 *
 * This wraps `highlightText` to create the DOM highlight element and then
 * delegates to `extendHighlightOverProtectedElements` to detect protected
 * elements (by selector) that should be wrapped as a whole. The returned
 * callback removes the created highlight(s).
 *
 * @param {Range} target - The DOM Range to highlight.
 * @param {string|function|undefined} protectedElementSelector - CSS selector string or a matcher function for elements
 *   that must be treated as atomic/protected; if `undefined` no extension is performed.
 * @param {string} [tagName='mark'] - Tag name used for the highlight element.
 * @param {Record<string,string>} [attributes] - Attributes applied to the highlight element (e.g. `data-iaa-id`).
 * @returns {() => void} A callback that removes the highlight(s) when invoked.
 */
export function safeHighlightText(
    target: Range,
    protectedElementSelector?: string | ((el: Element) => boolean),
    tagName?: string,
    attributes?: Record<string, string>
): () => void {
    let callback = highlightText(target, tagName, attributes);

    if (attributes && 'data-iaa-id' in attributes) {
        callback = extendHighlightOverProtectedElements(
            target,
            protectedElementSelector,
            `.iaa-highlighted[data-iaa-id="${attributes['data-iaa-id']}"]`,
            attributes,
            callback
        );
    }

    if (attributes && 'class' in attributes && attributes['class'].includes('iaa-ping-marker')) {
        callback = extendHighlightOverProtectedElements(
            target,
            protectedElementSelector,
            `.iaa-ping-marker`,
            attributes,
            callback
        );
    }

    if (attributes && 'id' in attributes && attributes['id'] === 'iaa-scroll-marker') {
        callback = extendHighlightOverProtectedElements(
            target,
            protectedElementSelector,
            `#iaa-scroll-marker`,
            attributes,
            callback
        );
    }

    return callback;
}

/**
 * Groups highlights by their VID.
 *
 * @param highlights list of highlights.
 * @returns groups of highlights by VID.
 */
export function groupHighlightsByVid(highlights: NodeListOf<Element>) {
    const spansByVid = new Map<VID, Array<Element>>();
    for (const highlight of Array.from(highlights)) {
        const vid = highlight.getAttribute('data-iaa-id');
        if (!vid) continue;

        let sectionGroup = spansByVid.get(vid);
        if (!sectionGroup) {
            sectionGroup = [];
            spansByVid.set(vid, sectionGroup);
        }
        sectionGroup.push(highlight);
    }
    return spansByVid;
}

/**
 * Utility function to find the closest highlight element to the given target.
 *
 * @param target a DOM node.
 * @returns the closest highlight element or null if none is found.
 */
export function closestHighlight(target: Node | null): HTMLElement | null {
    if (!(target instanceof Node)) {
        return null;
    }

    if (target instanceof Text) {
        const parent = target.parentElement;
        if (!parent) return null;
        target = parent;
    }

    const targetElement = target as Element;
    return targetElement.closest('[data-iaa-id]');
}

/**
 * Utility function to find all highlights that are ancestors of the given target.
 *
 * @param target a DOM node.
 * @returns all highlight elements that are ancestors of the given target.
 */
export function highlights(target: Node | null): HTMLElement[] {
    let hl = closestHighlight(target);
    const result: HTMLElement[] = [];
    while (hl) {
        result.push(hl);
        hl = closestHighlight(hl.parentElement);
    }
    return result;
}

/**
 * Calculates the rectangle of the inline label for the given highlight.
 *
 * @param highlight a highlight element.
 * @returns the inline label rectangle.
 */
export function getInlineLabelClientRect(highlight: Element): DOMRect {
    const r = highlight.getClientRects()[0];

    // TODO: It may be possible to implement this in a simpler way using
    // `window.getComputedStyle(highlight, ':before')`

    let cr: DOMRect;
    if (highlight.firstChild instanceof Text) {
        const range = document.createRange();
        range.selectNode(highlight.firstChild);
        cr = range.getClientRects()[0];
    } else if (highlight.firstChild instanceof Element) {
        cr = highlight.firstChild.getClientRects()[0];
    } else {
        throw new Error('Unexpected node type');
    }

    return new DOMRect(r.left, r.top, cr ? cr.left - r.left : 0, r.height);
}

/**
 * Determine whether a point lies inside (or on the edge of) a DOMRect.
 *
 * The check uses inclusive bounds: a point on the rect's left/right/top/bottom
 * edges is considered inside. Coordinates are expected to be in the same
 * coordinate space as the provided `rect` (typically viewport coordinates).
 *
 * @param {{x:number,y:number}} point - Coordinates of the point to test.
 * @param {DOMRect} rect - Rectangle to test against.
 * @returns {boolean} `true` when the point is within or on the boundary of `rect`, otherwise `false`.
 */
export function isPointInRect(point: { x: number; y: number }, rect: DOMRect): boolean {
    return (
        point.x >= rect.left &&
        point.x <= rect.right &&
        point.y >= rect.top &&
        point.y <= rect.bottom
    );
}

/**
 * Remove a CSS class from all ancestor elements of a starting element.
 *
 * Traverses the ancestor chain starting at `start.parentElement` and removes
 * `className` from each ancestor encountered. If `root` is provided the
 * traversal stops after processing `root` (the `root` node is also processed).
 * Any errors thrown while removing the class (for example on exotic/native
 * nodes that do not allow classList mutation) are ignored.
 *
 * @param {Element} start - Element whose ancestors will be processed (starts at parentElement).
 * @param {string} className - CSS class to remove from ancestor elements.
 * @param {Element | null} [root] - Optional ancestor at which to stop (inclusive). If omitted, traversal continues to the document root.
 */
export function removeClassFromAncestors(start: Element, className: string, root?: Element | null) {
    let current: Element | null = start.parentElement;
    while (current) {
        try {
            current.classList.remove(className);
        } catch {
            // ignore errors removing class from exotic nodes
        }
        if (root && current === root) break;
        current = current.parentElement;
    }
}

/**
 * Find the closest ancestor (including `start`) that matches one of the
 * comma-separated selectors. Each selector may be either a normal CSS
 * selector or a namespace-aware selector of the form `"{namespaceURI}localName"`.
 *
 * Examples:
 * - `div, span` (regular CSS selectors)
 * - `{http://example.com/ns}free_to_read` (namespace-aware match)
 * - `{*}free_to_read` (match any namespace for localName `free_to_read`)
 *
 * The function walks the ancestor chain and returns the first element that
 * matches any of the provided selectors. CSS selectors are tested using
 * `Element.matches()`. Namespace selectors are matched by comparing
 * `element.namespaceURI` and `element.localName`.
 *
 * @param {Element | null} start - Element at which to start the search (included).
 * @param {string} selector - Comma-separated list of CSS or namespace-aware selectors.
 * @returns {Element | null} The closest matching ancestor or `null` when none found.
 */
export function closestNsAware(start: Element | null, selector: string): Element | null {
    if (!start || !selector) return null;
    const matcher = compileNsSelector(selector);
    if (!matcher) return null;
    return closestWithMatcher(start, matcher);
}

/**
 * Compile a comma-separated selector string (CSS and namespace-aware tokens)
 * into a matcher function that tests a single Element. This avoids reparsing
 * the selector when matching many elements.
 *
 * @param selector comma-separated list of selectors (CSS or `{ns}localName`).
 */
export function compileNsSelector(selector?: string | null): ((el: Element) => boolean) | null {
    if (!selector) return null;
    selector = selector.trim();

    if (selector === '') return null;
    type Token =
        | { type: 'css'; sel: string }
        | { type: 'ns'; namespace: string | '*'; localName: string };

    const parts: string[] = [];
    let buf = '';
    let depth = 0;
    for (let i = 0; i < selector.length; i++) {
        const ch = selector[i];
        if (ch === '{') {
            depth++;
            buf += ch;
        } else if (ch === '}') {
            depth = Math.max(0, depth - 1);
            buf += ch;
        } else if (ch === ',' && depth === 0) {
            parts.push(buf.trim());
            buf = '';
        } else {
            buf += ch;
        }
    }
    if (buf.trim()) parts.push(buf.trim());

    const tokens: Token[] = [];
    for (const p of parts) {
        const m = p.match(/^\{([^}]*)\}(.+)$/);
        if (m) {
            const ns = m[1] === '*' ? '*' : m[1];
            tokens.push({ type: 'ns', namespace: ns, localName: m[2] });
        } else {
            tokens.push({ type: 'css', sel: p });
        }
    }

    // if all tokens are plain CSS selectors (no namespace-aware selectors), return a matcher that carries the original
    // comma-separated selector so `closestWithMatcher` can delegate to `Element.closest()` once.
    if (tokens.length > 0 && tokens.every((t) => t.type === 'css')) {
        const sel = selector; // trimmed original selector
        const fn = (el: Element) => {
            try {
                return el.matches(sel);
            } catch {
                return false;
            }
        };
        (fn as any).__cssSelector = sel;
        return fn;
    }

    return (el: Element) => {
        for (const t of tokens) {
            if (t.type === 'css') {
                try {
                    if (el.matches(t.sel)) return true;
                } catch {
                    // invalid selector -> ignore
                }
            } else {
                const ns = t.namespace;
                const ln = t.localName;
                if ((ns === '*' || el.namespaceURI === ns) && el.localName === ln) return true;
            }
        }
        return false;
    };
}

/**
 * Walk ancestors starting at `start` (included) and return the first
 * element for which `matcher` returns true.
 */
export function closestWithMatcher(
    start: Element | null,
    matcher: (el: Element) => boolean
): Element | null {
    if (!start) return null;

    // If the matcher carries a single CSS selector, delegate to native `closest()` once.
    const cssSel = (matcher as any).__cssSelector;
    if (cssSel && typeof cssSel === 'string') {
        try {
            return start.closest(cssSel);
        } catch {
            // fall back to manual walk on invalid selector
        }
    }

    let current = start;
    while (current) {
        if (matcher(current)) return current;
        current = current.parentElement;
    }
    return null;
}

/**
 * Expand a DOM Selection so that any selection boundary that lies inside
 * (or on) a protected element is moved to cover the entire protected
 * ancestor element.
 *
 * @param sel The DOM `Selection` to expand. Only `anchorNode`, `anchorOffset`,
 *            `focusNode` and `focusOffset` are used.
 * @param protectedElementsMatcher Optional matcher function that returns
 *                                 `true` for elements that must be treated as
 *                                 atomic/protected.
 * @returns A `SelectionLike` with potentially adjusted boundary nodes and offsets.
 */
export function expandSelectionOverProtectedElements(
    sel: Selection,
    protectedElementsMatcher?: (el: Element) => boolean
): SelectionLike {
    // To decide whether a selection boundary lies within a protected element
    // we check an element anchor: if the boundary node is an Element use it,
    // otherwise use its parentElement. However, we must NOT overwrite the
    // original selection node/offset because the selection offsets are
    // relative to the original Text node. Only when we actually expand the
    // boundary to a protected ancestor do we replace the node/offset.
    const anchorBoundaryEl =
        sel.anchorNode instanceof Element ? sel.anchorNode : sel.anchorNode?.parentElement;
    let anchorNode: Node | null | undefined = sel.anchorNode;
    let anchorOffset = sel.anchorOffset;

    if (anchorBoundaryEl instanceof Element && protectedElementsMatcher) {
        const protectedAncestor = closestWithMatcher(anchorBoundaryEl, protectedElementsMatcher);
        if (protectedAncestor) {
            anchorNode = protectedAncestor;
            anchorOffset = 0;
        }
    }

    // Same handling for the focus boundary: check a boundary element but
    // preserve the original focus node/offset unless we expand to a protected
    // ancestor.
    const focusBoundaryEl =
        sel.focusNode instanceof Element ? sel.focusNode : sel.focusNode?.parentElement;
    let focusNode: Node | null | undefined = sel.focusNode;
    let focusOffset = sel.focusOffset;

    if (focusBoundaryEl instanceof Element && protectedElementsMatcher) {
        const protectedAncestor = closestWithMatcher(focusBoundaryEl, protectedElementsMatcher);
        if (protectedAncestor) {
            focusNode = protectedAncestor;
            focusOffset = protectedAncestor.textContent?.length || 0;
        }
    }

    if (focusNode == null) {
        focusNode = sel.focusNode;
    }

    // console.log(`Anchor node ${sel.anchorNode} ${sel.anchorOffset} -> ${anchorNode} ${anchorOffset}`)
    // console.log(`Focus node ${sel.focusNode} ${sel.focusOffset} -> ${focusNode} ${focusOffset}`)

    return { anchorNode, anchorOffset, focusNode, focusOffset };
}

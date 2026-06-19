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

// Pure offset/selection helpers extracted from KeyboardEditorMode so the caret
// positioning logic can be unit-tested without a live editor instance. These
// functions take their inputs explicitly instead of reading instance state.

import { calculateStartOffset, offsetToRange } from '@inception-project/inception-js-api';
import { nodeToElement } from './Utilities';

export interface ProtectedBounds {
    start: number;
    end: number;
}

export type ProtectedElementsMatcher = (el: Element) => boolean;

/**
 * Absolute document offset of a range's collapsed-start position, relative to
 * `root`. Returns null when the start container cannot be located under `root`.
 */
export function caretOffsetOf(root: Node, range: Range): number | null {
    const base = calculateStartOffset(root, range.startContainer);
    if (base < 0) return null;
    return base + range.startOffset;
}

/**
 * Offset of the first non-whitespace character under `contentRoot`, or 0 when the
 * content is entirely whitespace (or the walk throws).
 */
export function findFirstWordStartOffset(contentRoot: Node, doc?: Document): number {
    try {
        const ownerDoc = doc ?? contentRoot.ownerDocument ?? document;
        const walker = ownerDoc.createTreeWalker(contentRoot, NodeFilter.SHOW_TEXT, {
            acceptNode(node: Node) {
                const v = node.nodeValue || '';
                return /\S/.test(v) ? NodeFilter.FILTER_ACCEPT : NodeFilter.FILTER_REJECT;
            },
        } as any);
        let n = walker.nextNode();
        while (n) {
            const txt = n.nodeValue || '';
            const idx = txt.search(/\S/);
            if (idx >= 0) {
                return calculateStartOffset(contentRoot, n) + idx;
            }
            n = walker.nextNode();
        }
    } catch {
        // ignore
    }
    return 0;
}

interface SelectionLike {
    anchorNode: Node | null;
    anchorOffset: number;
    focusNode: Node | null;
    focusOffset: number;
}

/**
 * Whether the selection runs right-to-left, i.e. its focus precedes its anchor in
 * document order. Used to preserve selection direction when re-applying a range.
 */
export function isSelectionBackward(sel: SelectionLike): boolean {
    const { anchorNode, anchorOffset, focusNode, focusOffset } = sel;
    if (!anchorNode || !focusNode) return false;
    if (anchorNode === focusNode) return focusOffset < anchorOffset;
    // The selection is backward if the focus precedes the anchor in document order.
    return !!(anchorNode.compareDocumentPosition(focusNode) & Node.DOCUMENT_POSITION_PRECEDING);
}

/**
 * Offset bounds of the OUTERMOST protected ancestor of `node`, or null when `node`
 * is not inside a protected element (or no matcher is configured). Walks all the way
 * up because protected elements may nest and the caret must treat the whole atomic
 * unit as a single obstacle.
 */
export function findProtectedBounds(
    contentRoot: Node,
    node: Node | null,
    protectedElementsMatcher?: ProtectedElementsMatcher
): ProtectedBounds | null {
    if (!protectedElementsMatcher) return null;

    let el = nodeToElement(node);
    let outermost: Element | null = null;
    while (el && el !== contentRoot && contentRoot.contains(el)) {
        if (protectedElementsMatcher(el)) {
            outermost = el;
        }
        el = el.parentElement;
    }
    if (!outermost) return null;
    const start = calculateStartOffset(contentRoot, outermost);
    const end = start + (outermost.textContent ? outermost.textContent.length : 0);
    return { start, end };
}

/**
 * Snap an offset that lands inside a protected element to the start of the OUTERMOST
 * protected ancestor, so restoration never drops the caret into an atomic element.
 * Returns the offset unchanged when it is already safe.
 */
export function safeCaretOffset(
    contentRoot: Node,
    offset: number,
    protectedElementsMatcher?: ProtectedElementsMatcher
): number {
    const range = offsetToRange(contentRoot, offset, offset);
    const bounds = range
        ? findProtectedBounds(contentRoot, range.startContainer, protectedElementsMatcher)
        : null;
    return bounds ? bounds.start : offset;
}

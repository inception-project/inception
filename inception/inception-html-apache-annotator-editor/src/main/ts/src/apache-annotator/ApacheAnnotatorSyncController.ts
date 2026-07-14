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
import {
    type ViewportScrollPosition,
    type ViewportScrollTarget,
    offsetToRange,
    calculateStartOffset,
    calculateEndOffset,
} from '@inception-project/inception-js-api';
import { removeClassFromAncestors } from './Utilities';

// CSS `display` values whose boxes are block-level; an element with one of these owns a
// distinct block in the flow and so can anchor a text offset (a box whose height is a
// meaningful denominator for the intra-anchor scroll fraction).
const ANCHOR_DISPLAY_CATEGORIES = new Set([
    'block',
    'flex',
    'grid',
    'table',
    'table-row',
    'list-item',
]);

// Our own overlay elements: they sit above the content at the viewport top but carry no document
// offsets, so scroll-sync anchoring must skip them and look at the content behind them instead.
const OVERLAY_SELECTOR =
    '.iaa-section-control, .iaa-vertical-marker-focus, .iaa-ping-marker,' +
    ' iaa-visible-annotations-panel, iaa-visible-annotations-panel-spacer';

/**
 * Editor-to-editor viewport synchronization for the Apache Annotator visualizer. Implements the
 * {@code AnnotationEditor} viewport-sync protocol: {@link getViewportScrollPosition} reports the
 * document offset currently at the viewport top (plus how far the top has scrolled into it), and
 * {@link scrollToViewportPosition} places a given offset+fraction back at the viewport top.
 * Interpolating in character space rather than pixels keeps two editors in sync even when they wrap
 * the same text to different heights.
 *
 * The controller is deliberately decoupled from the visualizer: it depends only on the editor root
 * element and a lazily-read scroll container (the container is assigned after the visualizer is
 * constructed, so it is passed as a getter). It touches DOM/geometry only — no tracker, rendering,
 * or paging state — which keeps this measurement-heavy anchor-finding logic isolated and testable.
 */
export class ApacheAnnotatorSyncController {
    private readonly root: Element;
    private readonly getScrollContainer: () => Element | undefined;

    /** Last viewport position applied by {@link scrollToViewportPosition}, for per-frame dedup. */
    private lastAppliedViewportScrollPosition: { begin: number; fraction: number } | undefined =
        undefined;

    constructor(root: Element, getScrollContainer: () => Element | undefined) {
        this.root = root;
        this.getScrollContainer = getScrollContainer;
    }

    /**
     * @return the scroll position of the block element currently at the top of the scroll viewport as document offsets
     * plus the fraction of it that has already been scrolled past.
     * @see {@link scrollToViewportPosition}.
     */
    getViewportScrollPosition(): ViewportScrollPosition | null {
        const container = this.getScrollContainer();
        if (!container) return null;

        const containerRect = container.getBoundingClientRect();
        const anchor = this.findViewportTopAnchor(containerRect);
        if (!anchor) return null;

        const anchorRect = anchor.getBoundingClientRect();
        const begin = calculateStartOffset(this.root, anchor);
        const end = calculateEndOffset(this.root, anchor);
        const fraction =
            anchorRect.height > 0
                ? Math.min(1, Math.max(0, (containerRect.top - anchorRect.top) / anchorRect.height))
                : 0;
        return { begin, end, fraction };
    }

    scrollToViewportPosition(pos: ViewportScrollTarget): void {
        const container = this.getScrollContainer();
        if (!container) return;

        const range = offsetToRange(this.root, pos.begin, pos.begin);
        if (!range) return;

        const start = range.startContainer;
        const startElement = start instanceof Element ? start : start.parentElement;
        const block = this.closestAnchorElement(startElement);
        if (!block) return;

        // Skip if the scroll request would not actually move the scroll position
        const fraction = Math.min(1, Math.max(0, pos.fraction));
        const last = this.lastAppliedViewportScrollPosition;
        if (last && last.begin === pos.begin && Math.abs(last.fraction - fraction) < 0.01) {
            return;
        }
        this.lastAppliedViewportScrollPosition = { begin: pos.begin, fraction };

        // If necessary unhide the target block before measuring. We unhide only if hidden because a write (even a noop one) to the DOM can invalidate the layout
        if (block.closest('.iaa-secluded')) {
            removeClassFromAncestors(block, 'iaa-secluded', this.root);
        }

        const containerRect = container.getBoundingClientRect();
        const blockRect = block.getBoundingClientRect();
        container.scrollTop =
            container.scrollTop + (blockRect.top - containerRect.top) + fraction * blockRect.height;
    }

    /**
     * Reset the per-frame dedup memory. Call when a (programmatic) scroll completes so the next sync
     * position is applied even if it coincides with the last one applied during the scroll.
     */
    reset(): void {
        this.lastAppliedViewportScrollPosition = undefined;
    }

    /**
     * Try locating the block-level element that straddles the top of the scroll viewport.
     */
    private findViewportTopAnchor(containerRect: DOMRect): Element | null {
        const doc = this.root.ownerDocument;
        const rootRect = this.root.getBoundingClientRect();
        const left = Math.max(containerRect.left, rootRect.left);
        const right = Math.min(containerRect.right, rootRect.right);
        if (right <= left) return null;

        const seamY = containerRect.top;
        const width = right - left;
        // x offsets guard against a probe column falling in a gutter/float; y offsets step past the
        // seam's top-margin gap (see method doc). Neither needs to land on the target block - any
        // in-root node lets blockAtSeam() resolve the anchor by structure.
        const entryX = [left + width * 0.5, left + width * 0.25, left + width * 0.75];
        const entryY = [seamY + 8, seamY + 24, seamY + 48, seamY + 96];

        for (const y of entryY) {
            for (const x of entryX) {
                for (const el of doc.elementsFromPoint(x, y)) {
                    if (el === this.root || !this.root.contains(el)) continue;
                    // Skip our own overlays - they sit above the content but carry no offsets
                    if (el.closest('svg')) continue;
                    if (el.closest(OVERLAY_SELECTOR)) continue;

                    const anchor = this.blockAtSeam(el, seamY);
                    if (anchor) return anchor;
                }
            }
        }

        return null;
    }

    /**
     * From an entry node near the viewport top, return the block-level element with text that
     * straddles {@code seamY} (its box spans the seam line), searching in document order. If the
     * entry node's own block sits entirely above the seam (its content has scrolled past), we
     * advance to the following blocks; if it sits entirely below, we accept it as the first block
     * at or after the seam. Straddling is preferred because {@link getViewportScrollPosition}
     * measures how far the viewport top has scrolled <em>into</em> the anchor, which is only
     * meaningful for a block the seam actually crosses.
     * <p>
     * When straddling blocks nest (e.g. a section and a paragraph inside it both cross the seam) we
     * return the <em>innermost</em> one: the deepest block gives the finest-grained height for the
     * scroll fraction, matching the granularity the previous ancestor-only anchor produced.
     */
    private blockAtSeam(entry: Element, seamY: number): Element | null {
        let start = this.closestBlockWithText(entry);
        if (!start) return null;

        const straddles = (el: Element) => {
            const rect = el.getBoundingClientRect();
            return rect.top <= seamY && rect.bottom > seamY;
        };

        // The entry probe can land on a block just below the seam (e.g. the table row under the one
        // that crosses it). The straddling block is then earlier in document order, which the
        // forward walk below never revisits - so back up while the start block sits entirely below
        // the seam and the previous block reaches up to or across it.
        while (start.getBoundingClientRect().top > seamY) {
            const prev = this.prevBlockWithText(start);
            if (!prev || prev.getBoundingClientRect().top > seamY) break;
            start = prev;
        }

        let firstBelow: Element | null = null;
        let innermostStraddler: Element | null = null;
        let current: Element | null = start;
        while (current && current !== this.root) {
            if (straddles(current)) {
                // Descend into nested straddlers, but stop once the walk leaves this subtree - a
                // later straddling sibling is a different, lower block, not a finer anchor.
                if (!innermostStraddler || innermostStraddler.contains(current)) {
                    innermostStraddler = current;
                } else {
                    break;
                }
            } else if (innermostStraddler) {
                break; // left the straddling run; keep the innermost we found
            } else if (!firstBelow) {
                const rect = current.getBoundingClientRect();
                if (rect.top > seamY) {
                    firstBelow = current; // first fully-below block, a fallback if nothing straddles
                }
            }
            current = this.nextBlockWithText(current);
        }

        return innermostStraddler ?? firstBelow ?? start;
    }

    private closestBlockWithText(element: Element | null): Element | null {
        let current = element;
        while (current && current !== this.root) {
            if (this.isBlockWithText(current)) return current;
            current = current.parentElement;
        }
        return null;
    }

    private nextBlockWithText(element: Element): Element | null {
        const walker = this.blockWalker();
        walker.currentNode = element;
        return walker.nextNode() as Element | null;
    }

    private prevBlockWithText(element: Element): Element | null {
        const walker = this.blockWalker();
        walker.currentNode = element;
        return walker.previousNode() as Element | null;
    }

    private blockWalker(): TreeWalker {
        return this.root.ownerDocument.createTreeWalker(this.root, NodeFilter.SHOW_ELEMENT, {
            acceptNode: (node) =>
                this.isBlockWithText(node as Element)
                    ? NodeFilter.FILTER_ACCEPT
                    : NodeFilter.FILTER_SKIP,
        });
    }

    private isBlockWithText(element: Element): boolean {
        // The editor root/scroll container itself is never a content anchor: it spans the whole
        // viewport, so accepting it would yield a meaningless full-height fraction denominator. It
        // can otherwise slip through here as block-with-text, e.g. when the document's first block
        // is at the seam and the back-up walk steps past it to the container.
        if (element === this.root) return false;
        if (!element.textContent) return false;
        if (element.closest('svg')) return false;
        if (element.closest(OVERLAY_SELECTOR)) return false;
        return ANCHOR_DISPLAY_CATEGORIES.has(window.getComputedStyle(element).display);
    }

    private closestAnchorElement(element: Element | null): Element | null {
        let current = element;
        while (current && current !== this.root) {
            if (ANCHOR_DISPLAY_CATEGORIES.has(window.getComputedStyle(current).display)) {
                return current;
            }
            current = current.parentElement;
        }
        return current;
    }
}

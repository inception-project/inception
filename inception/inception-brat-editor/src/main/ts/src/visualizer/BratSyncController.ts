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
} from '@inception-project/inception-js-api';
import { ScrollSyncRowCache, type RowGeometry, type RowGeometrySource } from './ScrollSyncRowCache';

/** Fraction of the scroll range over which to blend toward whole-container progress at each end. */
const EXTREME_BLEND_BAND = 0.15;

/**
 * The row anchoring the viewport top: the last row whose top is at or above the viewport top
 * (top <= 0). When the viewport is scrolled into the padding above the first row (every row has a
 * positive top), the first row anchors it - the caller then derives a negative fraction from it.
 * Rows are assumed sorted top-ascending. Returns null for an empty list.
 */
export function anchorRowForViewport(rows: readonly RowGeometry[]): RowGeometry | null {
    if (rows.length === 0) return null;
    let anchor = rows[0];
    for (const row of rows) {
        if (row.top <= 0) {
            anchor = row;
        } else {
            break;
        }
    }
    return anchor;
}

/**
 * Build the {@link ViewportScrollPosition} the editor reports, from its current rows and scroll
 * metrics. Pure counterpart of {@link BratSyncController.getViewportScrollPosition} - the DOM-free
 * math, so it can be unit-tested with synthetic rows. Returns null when there are no rows.
 *
 * The fraction is intentionally NOT clamped below zero: when the viewport top sits in the padding
 * above the first row, a negative fraction lets the receiver align there too instead of stopping a
 * line short of the top. It IS clamped to at most 1.
 */
export function viewportScrollPositionFromRows(
    rows: readonly RowGeometry[],
    scrollTop: number,
    maxScroll: number
): ViewportScrollPosition | null {
    const anchor = anchorRowForViewport(rows);
    if (!anchor) return null;

    // Overall scroll progress lets the receiver blend toward its own extreme near the top/bottom
    // (see ViewportScrollPosition.scrollProgress), so scrolling just off an extreme no longer snaps.
    const scrollProgress = maxScroll > 0 ? scrollTop / maxScroll : 0;

    const fraction = anchor.height > 0 ? -anchor.top / anchor.height : 0;
    return {
        begin: anchor.begin,
        end: anchor.end,
        fraction: Math.min(1, fraction),
        scrollProgress,
    };
}

/**
 * Given the receiver's current rows and a requested {@link ViewportScrollTarget}, compute the
 * absolute scrollTop that places the target offset+fraction at the viewport top. Pure counterpart
 * of {@link BratSyncController.scrollToViewportPosition} - the DOM-free math, so it can be
 * unit-tested with synthetic rows. Returns null when there are no rows to anchor against.
 *
 * When the offset falls outside the current window (the two editors paged to different parts of the
 * same document), the target is clamped to the nearest rendered row so the view aligns to the top
 * or bottom of what is in the DOM instead of doing nothing.
 */
export function targetScrollTopForPosition(
    rows: readonly RowGeometry[],
    pos: ViewportScrollTarget,
    currentScrollTop: number,
    maxScroll: number
): number | null {
    if (rows.length === 0) return null;

    // --- Offset-anchored target (accurate in the document interior) ---
    // Convert the incoming anchor to a fractional CHARACTER offset - the layout-independent quantity
    // the two editors agree on. Interpolating here in pixels off the source's row height would
    // jitter, because the same sentence wraps to a different height on each side; in character space
    // that difference washes out (each side re-derives pixels from its own rows below).
    const end = pos.end ?? pos.begin;
    const targetOffset = pos.begin + pos.fraction * (end - pos.begin);

    // Find the row whose offset span contains targetOffset (nearest by span if it lands in an
    // inter-row gap; clamp to the ends only if truly outside the window).
    let anchor = rows.find((r) => targetOffset >= r.begin && targetOffset <= r.end);
    if (!anchor) {
        if (targetOffset < rows[0].begin) {
            anchor = rows[0];
        } else if (targetOffset > rows[rows.length - 1].end) {
            anchor = rows[rows.length - 1];
        } else {
            anchor = rows.reduce((best, r) => {
                const d = Math.min(
                    Math.abs(r.begin - targetOffset),
                    Math.abs(r.end - targetOffset)
                );
                const bestD = Math.min(
                    Math.abs(best.begin - targetOffset),
                    Math.abs(best.end - targetOffset)
                );
                return d < bestD ? r : best;
            }, rows[0]);
        }
    }

    // Interpolate the target offset's position WITHIN the receiver's own row, so the fraction is
    // re-expressed against this editor's row height, not the source's. rows[i].top is
    // viewport-relative, so this is the ABSOLUTE scrollTop that puts the target at the top.
    const span = anchor.end - anchor.begin;
    const rowFraction =
        span > 0 ? Math.min(1, Math.max(0, (targetOffset - anchor.begin) / span)) : 0;
    const anchoredTop = currentScrollTop + anchor.top + rowFraction * anchor.height;

    // --- Blend toward whole-container scroll-progress near the extremes ---
    // Offset-anchoring aligns viewport TOPS, so near a scroll extreme it lands short of this editor's
    // own extreme (the two editors have different amounts of content below the shared top row). A
    // pure progress mapping (progress * maxScroll) hits the extremes exactly but drifts in the
    // interior. Blend: weight toward progress only within a transition band next to each extreme, so
    // the extremes are exact AND the interior stays offset-accurate, with no discontinuity (snap) at
    // the boundary between the two regimes.
    if (pos.scrollProgress !== undefined && maxScroll > 0) {
        const progressTop = pos.scrollProgress * maxScroll;
        const p = pos.scrollProgress;
        // 1 at an extreme -> 0 by EXTREME_BLEND_BAND in from it.
        const nearness = Math.max(0, 1 - Math.min(p, 1 - p) / EXTREME_BLEND_BAND);
        return nearness * progressTop + (1 - nearness) * anchoredTop;
    }

    return anchoredTop;
}

/**
 * Editor-to-editor viewport synchronization for the brat visualizer. Implements the
 * {@code AnnotationEditor} viewport-sync protocol: {@link getViewportScrollPosition} reports the
 * visual row currently at the viewport top (plus how far the top has advanced through it), and
 * {@link scrollToViewportPosition} places a given offset+fraction back at the viewport top.
 * Anchoring is done in document-character space rather than pixels so two editors stay in sync even
 * when they wrap the same text to different heights.
 *
 * The controller is decoupled from the visualizer: it depends only on a lazily-read scroll
 * container and a lazily-assembled {@link RowGeometrySource} (both supplied as getters, since they
 * are only meaningful after a render). It owns the {@link ScrollSyncRowCache} so the expensive
 * per-layout DOM measurement lives with the code that consumes it; the visualizer calls
 * {@link invalidate} from its render funnel.
 */
export class BratSyncController {
    private readonly getScrollContainer: () => HTMLElement | null;
    private readonly getRowGeometrySource: () => RowGeometrySource | null;

    private rowGeometryCache = new ScrollSyncRowCache();

    /**
     * @param getScrollContainer resolves the current scroll container (null before it exists).
     * @param getRowGeometrySource assembles the current inputs for row measurement (null when there
     * is no rendered document to measure).
     */
    constructor(
        getScrollContainer: () => HTMLElement | null,
        getRowGeometrySource: () => RowGeometrySource | null
    ) {
        this.getScrollContainer = getScrollContainer;
        this.getRowGeometrySource = getRowGeometrySource;
    }

    /** Discard the cached row geometry so the next sync read re-measures. Call on every render. */
    invalidate(): void {
        this.rowGeometryCache.invalidate();
    }

    /**
     * The visual rows of the current window, each as its viewport-relative top edge, its height
     * (the gap to the next row, so it spans the full inter-line distance), and the document offset
     * span it covers. The heavy DOM measurement is cached per layout by {@link ScrollSyncRowCache};
     * this only supplies the current inputs and projects into the live viewport.
     *
     * Anchoring viewport-sync on rows rather than individual chunks (tokens) is what keeps the
     * follower smooth: a row changes far less often than a token as one scrolls, and the fraction
     * moves continuously across the whole inter-row distance instead of resetting at every token.
     */
    private computeRowGeometry(): RowGeometry[] | null {
        const container = this.getScrollContainer();
        const source = this.getRowGeometrySource();
        if (!container || !source) return null;

        return this.rowGeometryCache.getRows(container, source);
    }

    /**
     * Report the visual row currently at the top of the scroll viewport as document offsets plus
     * the fraction of that row the viewport top has advanced through. Resolves the live scroll
     * container and rows, then defers to the pure {@link viewportScrollPositionFromRows}.
     *
     * Offsets are document-absolute (the window offset is added back by the row cache), so a
     * receiver on a different editor can map them into its own layout. brat is server-paged: only
     * the current window is in the DOM, so the reported anchor is necessarily within it.
     */
    getViewportScrollPosition(): ViewportScrollPosition | null {
        const container = this.getScrollContainer();
        if (!container) return null;

        const rows = this.computeRowGeometry();
        if (!rows) return null;

        const maxScroll = container.scrollHeight - container.clientHeight;
        return viewportScrollPositionFromRows(rows, container.scrollTop, maxScroll);
    }

    /**
     * Continuous top-alignment for scroll synchronization: resolve the live scroll container and
     * rows, compute the target scrollTop via the pure {@link targetScrollTopForPosition}, and apply
     * it. Deliberately not sharing {@code scrollTo}'s machinery - no pings, no smooth animation, no
     * centering - so a stream of these (one per animation frame while the other editor scrolls)
     * stays jank-free. Assigning scrollTop clamps to the scroll bounds, so an out-of-range target
     * simply scrolls as far as the container allows.
     */
    scrollToViewportPosition(pos: ViewportScrollTarget): void {
        const container = this.getScrollContainer();
        if (!container) return;

        const rows = this.computeRowGeometry();
        if (!rows || rows.length === 0) return;

        const maxScroll = container.scrollHeight - container.clientHeight;
        const target = targetScrollTopForPosition(rows, pos, container.scrollTop, maxScroll);
        if (target === null) return;

        container.scrollTop = target;
    }
}

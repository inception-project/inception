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
import { Chunk } from './Chunk';

/** A visual row of the current window, projected into the viewport (top is scroll-relative). */
export interface RowGeometry {
    /** The row's top edge relative to the scroll viewport top (0 = at the viewport top). */
    top: number;
    /** The full inter-line advance to the next row (so rows tile the vertical space). */
    height: number;
    /** Document-absolute begin offset of the text on this row. */
    begin: number;
    /** Document-absolute end offset of the text on this row. */
    end: number;
}

/** The inputs the cache needs to (re)measure the DOM. Supplied by the visualizer on rebuild. */
export interface RowGeometrySource {
    /** The rendered SVG root whose chunk-text elements are measured. */
    svgNode: SVGSVGElement;
    /** The current window's chunks, indexed by {@code Chunk.index} (== array position). */
    chunks: ReadonlyArray<Chunk>;
    /** Offset of the current window's start within the document, added back so offsets are absolute. */
    windowBegin: number;
    /** Fallback line height used when a single-row window has no inter-row gap to measure. */
    defaultLineHeight: number;
    /**
     * Document-relative bottom edge of the laid-out content (the trailing y after the render loop
     * placed the last row), in the same units as the measured row tops (the brat SVG renders at a
     * vertical user->pixel scale of 1; see {@link DocumentData.contentHeight}). Used to give the
     * LAST row its true height - it has no successor row whose top would otherwise reveal it.
     * Undefined before the first render completes; falls back to the previous gap when absent.
     */
    contentBottom?: number;
}

/**
 * Caches the visual-row geometry the brat editor reports for viewport synchronization.
 *
 * Measuring rows from the DOM (grouping chunk-text client rects by their top coordinate) is the
 * expensive part, but it only depends on the LAYOUT, not the scroll position: a row's height and
 * its document-offset span do not change as one scrolls, and its top merely translates by the
 * scrollTop. So the measurement is done once per layout (storing each top DOCUMENT-relative, i.e.
 * from the top of the scrollable content) and cached; each per-frame read just subtracts the live
 * scrollTop to project the tops back into the viewport - no DOM measurement per frame.
 *
 * The visualizer calls {@link invalidate} at the start of every render (the single funnel through
 * which render, patch, repage, resize and density/width tweaks all pass); the table is rebuilt
 * lazily on the next {@link getRows}, so the measurement never runs at all unless viewport-sync
 * actually asks for the geometry.
 *
 * Note the {@code g.row} groups report a zero-height client rect and so cannot be measured
 * directly; the per-chunk grouping is the way to recover row tops from the DOM.
 */
export class ScrollSyncRowCache {
    /** Cached rows with DOCUMENT-relative tops (scroll-invariant). Null means "rebuild on next use". */
    private cache: Array<{
        topAbs: number;
        height: number;
        begin: number;
        end: number;
    }> | null = null;

    /** Discard the cached geometry so the next {@link getRows} re-measures. */
    invalidate(): void {
        this.cache = null;
    }

    /**
     * The current window's rows projected into the given scroll container's viewport, rebuilding the
     * cached layout measurement from {@code source} on first use after an {@link invalidate}. Returns
     * null when there is nothing to measure (no container-independent geometry yet).
     */
    getRows(container: HTMLElement, source: RowGeometrySource): RowGeometry[] | null {
        if (!this.cache) {
            this.cache = this.build(container, source);
        }
        if (!this.cache) return null;

        // Project the cached document-relative tops into the current viewport. This is the only
        // scroll-dependent part; heights and offset spans are layout properties and stay as cached.
        const scrollTop = container.scrollTop;
        return this.cache.map((row) => ({
            top: row.topAbs - scrollTop,
            height: row.height,
            begin: row.begin,
            end: row.end,
        }));
    }

    private build(
        container: HTMLElement,
        source: RowGeometrySource
    ): Array<{ topAbs: number; height: number; begin: number; end: number }> | null {
        const chunkElements = source.svgNode.querySelectorAll('text:not(.spacing)[data-chunk-id]');
        if (!chunkElements || chunkElements.length === 0) return null;

        // Document-relative reference: adding scrollTop to the viewport-relative rect turns a
        // scroll-dependent top into a scroll-invariant one that the cache can reuse across frames.
        const containerTop = container.getBoundingClientRect().top - container.scrollTop;
        const windowBegin = source.windowBegin;

        // Group chunk texts by their (rounded) top coordinate = one visual row.
        const rowsByTop = new Map<number, { topAbs: number; begin: number; end: number }>();
        for (const el of chunkElements) {
            const chunkId = el.getAttribute('data-chunk-id');
            if (chunkId == null) continue;
            const index = parseInt(chunkId, 10);
            // chunks are built in index order (see buildChunksFromTokenOffsets), so the array
            // position equals chunk.index - a direct lookup instead of an O(n) find. Guard against
            // the invariant ever breaking so we never pair an element with the wrong chunk.
            const chunk = source.chunks[index];
            if (!chunk || chunk.index !== index) continue;

            const rect = el.getBoundingClientRect();
            const key = Math.round(rect.top);
            const row = rowsByTop.get(key);
            if (row) {
                row.begin = Math.min(row.begin, chunk.from + windowBegin);
                row.end = Math.max(row.end, chunk.to + windowBegin);
            } else {
                rowsByTop.set(key, {
                    topAbs: rect.top - containerTop,
                    begin: chunk.from + windowBegin,
                    end: chunk.to + windowBegin,
                });
            }
        }

        if (rowsByTop.size === 0) return null;

        const rows = [...rowsByTop.values()].sort((a, b) => a.topAbs - b.topAbs);

        // Bottom edge of the laid-out content in the same document-relative frame as the row tops.
        // contentBottom comes from the render loop in SVG user space (origin = the SVG's own top,
        // vertical scale 1); rebasing by the SVG's top - measured the same way the row tops are -
        // lands it in the containerTop frame, correct even with padding/header above the SVG.
        const contentBottomAbs =
            source.contentBottom != null
                ? source.svgNode.getBoundingClientRect().top - containerTop + source.contentBottom
                : null;

        // A row's height is the distance to the next row (the full inter-line advance). The last
        // row has no successor row to reveal its height, so measure it against the content bottom
        // (its true height, tower included); fall back to the previous gap, then a nominal line
        // height. Guard against a bad measurement (non-positive) by not trusting contentBottom then.
        return rows.map((row, i) => {
            let height: number;
            if (i + 1 < rows.length) {
                height = rows[i + 1].topAbs - row.topAbs;
            } else if (contentBottomAbs != null && contentBottomAbs - row.topAbs > 0) {
                height = contentBottomAbs - row.topAbs;
            } else if (rows.length >= 2) {
                height = row.topAbs - rows[i - 1].topAbs;
            } else {
                height = source.defaultLineHeight;
            }
            return { topAbs: row.topAbs, height, begin: row.begin, end: row.end };
        });
    }
}

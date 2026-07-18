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
    type AnnotationEditor,
    type TocLevel,
    type ViewportScrollPosition,
    type ViewportScrollTarget,
    type ViewportSyncPeer,
    generateTOCKeyIndex,
    offsetToRange,
    calculateStartOffset,
    calculateEndOffset,
} from '@inception-project/inception-js-api';
import { removeClassFromAncestors } from './Utilities';

/**
 * Optional document-structure inputs the controller uses for by-key scroll sync (Rule 2)   . All are
 * read lazily so the controller sees the current document's structure without being reconstructed.
 * When absent (or when they yield no key), the controller uses only offset/fraction anchoring, so
 * behaviour is identical to an editor with no structure.
 */
export interface SyncStructureAccessors {
    /** Selector matching section elements (empty string when the document has no structure). */
    sectionSelector: () => string;
    /** The structural key for a section element, or undefined when it has none. */
    extractKey: (section: Element) => string | undefined;
    /** The current document's TOC tree, or undefined before a document is loaded. */
    tocRoot: () => TocLevel | undefined;
}

/**
 * The document as a flat list of non-overlapping "scroll-sync regions" for by-key sync (Rule 2).
 *
 * The document is partitioned by the START offsets of its keyed sections: a region runs from one
 * section start to the NEXT section start (never back up to an enclosing section), so regions never
 * nest or overlap and every position belongs to exactly one region -- the non-overlap is what keeps
 * the intra-region fraction continuous across section boundaries.
 *
 * Each region is identified by the key of the section that opens it, so two documents sharing a
 * structure produce the same ordered region keys and align region-by-region. The leading region
 * (document start up to the first keyed section) has no opening section and gets a fixed sentinel
 * key so both documents still match it.
 */
export const LEADING_REGION_KEY = '@@sync-leading-region';

export interface SyncRegion {
    key: string;
    /** Character-offset span of the region: [start, end). */
    start: number;
    end: number;
    /**
     * The section element that opens this region, or undefined for the leading region (which starts
     * at the document top). Its pixel top defines the region boundary the fraction is measured
     * against; the region ends where the next region's element begins.
     */
    startElement?: Element;
}

/**
 * Build the sorted, non-overlapping regions from a TOC. Region boundaries are the keyed sections'
 * start offsets (via {@code calculateStartOffset}); {@code docEnd} closes the last region. Sections
 * without a key are ignored (they do not partition -- their content belongs to the enclosing
 * region). Duplicate/shared keys keep their first occurrence in document order, matching
 * {@link generateTOCKeyIndex}. Returns regions in ascending start order, always beginning with the
 * leading region [0, firstSectionStart) keyed {@link LEADING_REGION_KEY}.
 */
export function buildSyncRegions(root: Element, tocRoot: TocLevel, docEnd: number): SyncRegion[] {
    const byKey = generateTOCKeyIndex(tocRoot);

    type Boundary = { key: string; start: number; element?: Element };
    const starts: Boundary[] = [];
    for (const key of Object.keys(byKey)) {
        const el = byKey[key].element;
        if (!el) continue;
        const start = calculateStartOffset(root, el);
        if (start < 0) continue;
        starts.push({ key, start, element: el });
    }

    starts.sort((a, b) => a.start - b.start);

    // Boundary list: leading sentinel at 0, then each section start (deduped by offset). Two sections
    // sharing a start offset would make a zero-width region; keep the first key at that offset.
    const boundaries: Boundary[] = [{ key: LEADING_REGION_KEY, start: 0 }];
    for (const s of starts) {
        if (s.start <= 0) continue; // a section at offset 0 replaces the leading region below
        const prev = boundaries[boundaries.length - 1];
        if (s.start === prev.start) continue; // same offset -> first key wins
        boundaries.push(s);
    }
    // If a keyed section starts exactly at 0, it owns the leading region instead of the sentinel.
    const zeroStart = starts.find((s) => s.start === 0);
    if (zeroStart) boundaries[0] = zeroStart;

    const regions: SyncRegion[] = [];
    for (let i = 0; i < boundaries.length; i++) {
        const start = boundaries[i].start;
        const end = i + 1 < boundaries.length ? boundaries[i + 1].start : docEnd;
        if (end <= start) continue; // drop empty/inverted regions (e.g. docEnd before last start)
        regions.push({
            key: boundaries[i].key,
            start,
            end,
            startElement: boundaries[i].element,
        });
    }
    return regions;
}

/**
 * How to place the receiver's viewport for an incoming by-key sync position, once the receiver's
 * own regions are compared against the sender's key sequence.
 */
export type RegionTargetPlan =
    /**
     * The receiver has a region for `pos.sectionKey` directly: place at that region's own fraction.
     *
     * `atDocumentStart`/`atDocumentEnd` say whether this match sits at THIS document's first/last
     * region AND (when the sender's sequence is available) at the SENDER's first/last region too --
     * i.e. whether the two documents' overall extremes genuinely correspond here. A matched key can
     * be common to both documents while sitting at wildly different overall positions (e.g. a shared
     * "Chapter 1" that opens the main document but is the 13th region of a reference document with 12
     * unmatched front-matter sections first) -- there, the sender being at overall scrollProgress 0
     * does NOT mean this document's matching region is anywhere near ITS OWN top. Both flags default
     * true when the sender sent no sequence (nothing to disprove correspondence with).
     */
    | { kind: 'exact'; regionIndex: number; atDocumentStart: boolean; atDocumentEnd: boolean }
    /**
     * Neither `pos.sectionKey` nor any bracketing key before/after it (in the sender's sequence) has
     * a region here: no shared structure to align on at all.
     */
    | { kind: 'none' }
    /**
     * The sender's current region has no local match, but a shared key exists on both sides of it in
     * the sender's sequence. Interpolate the receiver's viewport between the two bracketing regions,
     * scaled by how far the sender has progressed from the "before" key to the "after" key.
     */
    | { kind: 'interpolate'; beforeRegionIndex: number; afterRegionIndex: number; progress: number }
    /**
     * Only a "before" key matches (the sender is past the last key it shares with the receiver, e.g.
     * a trailing document-only section): freeze at that region's end.
     */
    | { kind: 'freeze-after'; regionIndex: number }
    /**
     * Only an "after" key matches (the sender is before the first key it shares with the receiver):
     * freeze at that region's start.
     */
    | { kind: 'freeze-before'; regionIndex: number };

/**
 * Decide how to place the receiver's viewport for a by-key sync position, given the receiver's own
 * regions and the sender's full ordered key sequence.
 *
 * The sender's sequence is required to bracket a miss: the receiver alone has no notion of "where in
 * the sender's document" an unmatched key sits, only the sender's own region order tells us that. See
 * {@link RegionTargetPlan} for the five outcomes.
 *
 * {@link LEADING_REGION_KEY} is never accepted as a BRACKET (only ever as an exact match): it is a
 * fixed sentinel every document's region list starts with, so it is trivially "shared" even when the
 * two documents' actual leading content is unrelated (e.g. a title page here, a different preface
 * there). Treating it as a real bracket anchor would interpolate the receiver toward its own leading
 * region while the sender is still in an unmatched section before the first real heading, then snap
 * back once the sender reaches a genuinely shared key -- a scroll-down-then-back-up jump.
 */
export function resolveRegionTarget(
    regions: SyncRegion[],
    sectionKey: string,
    sectionKeySequence: string[] | undefined,
    sectionFraction = 0
): RegionTargetPlan {
    const exactIdx = regions.findIndex((r) => r.key === sectionKey);
    if (exactIdx >= 0) {
        const senderIdx = sectionKeySequence?.indexOf(sectionKey) ?? -1;
        const senderKnown = sectionKeySequence !== undefined && senderIdx >= 0;
        return {
            kind: 'exact',
            regionIndex: exactIdx,
            atDocumentStart: exactIdx === 0 && (!senderKnown || senderIdx === 0),
            atDocumentEnd:
                exactIdx === regions.length - 1 &&
                (!senderKnown || senderIdx === (sectionKeySequence as string[]).length - 1),
        };
    }

    if (!sectionKeySequence || sectionKeySequence.length === 0) return { kind: 'none' };

    const senderIdx = sectionKeySequence.indexOf(sectionKey);
    if (senderIdx < 0) return { kind: 'none' };

    const localIndexOf = new Map(regions.map((r, i) => [r.key, i]));

    let beforeSenderIdx = -1;
    let beforeRegionIndex = -1;
    for (let i = senderIdx - 1; i >= 0; i--) {
        const key = sectionKeySequence[i];
        if (key === LEADING_REGION_KEY) continue;
        const idx = localIndexOf.get(key);
        if (idx !== undefined) {
            beforeSenderIdx = i;
            beforeRegionIndex = idx;
            break;
        }
    }

    let afterSenderIdx = -1;
    let afterRegionIndex = -1;
    for (let i = senderIdx + 1; i < sectionKeySequence.length; i++) {
        const key = sectionKeySequence[i];
        if (key === LEADING_REGION_KEY) continue;
        const idx = localIndexOf.get(key);
        if (idx !== undefined) {
            afterSenderIdx = i;
            afterRegionIndex = idx;
            break;
        }
    }

    if (beforeRegionIndex < 0 && afterRegionIndex < 0) return { kind: 'none' };
    if (beforeRegionIndex < 0) return { kind: 'freeze-before', regionIndex: afterRegionIndex };
    if (afterRegionIndex < 0) return { kind: 'freeze-after', regionIndex: beforeRegionIndex };

    // Both found: the sender's progress from the "before" key to the "after" key, in sender-key-index
    // space, is the fraction to place the receiver between the two bracketing regions. The sender's
    // current position is `senderIdx` keys past `beforeSenderIdx`, plus its fractional progress
    // through its own current region (so movement is smooth within a single unmatched region rather
    // than stepping only at key boundaries), out of the `afterSenderIdx - beforeSenderIdx` keys
    // between the two bracketing keys.
    const span = afterSenderIdx - beforeSenderIdx;
    const progress = span > 0 ? (senderIdx - beforeSenderIdx + sectionFraction) / span : 0;
    return {
        kind: 'interpolate',
        beforeRegionIndex,
        afterRegionIndex,
        progress: Math.min(1, Math.max(0, progress)),
    };
}

/**
 * @return whether a resolved {@link RegionTargetPlan} should be placed via
 * {@link blendTowardScrollProgress} (blending the computed pixel target toward the sender's overall
 * scroll progress near a document extreme) rather than applied as-is.
 *
 * Only `'exact'` can qualify, and even then only when the matched region sits at BOTH documents'
 * corresponding overall extreme (`atDocumentStart`/`atDocumentEnd` -- see {@link RegionTargetPlan}).
 * There, the sender's overall progress is a meaningful stand-in for "how close to MY OWN top/bottom
 * should I be", which is exactly what the blend is for.
 *
 * For `'freeze-before'`/`'freeze-after'`/`'interpolate'` the sender is partly or wholly in content this
 * document does not have, so its overall progress has no such correspondence: blending toward it pulls
 * the receiver away from the resolved target as the sender's progress climbs, then back as the blend's
 * near-extreme weighting fades -- a scroll-down-then-back-up wobble instead of a hold (found live
 * 2026-07-17, reference doc `tei-book1-outline.xml` scrolling through its unmatched leading sections
 * before "Chapter 1"). Applying `target` directly for those three kinds is what makes a freeze an
 * actual freeze and an interpolation a straight line.
 *
 * The `atDocumentStart`/`atDocumentEnd` guard is what makes an `'exact'` match qualify only at
 * genuinely corresponding extremes: a shared key can sit at very different overall positions in the
 * two documents (the front-matter case in {@link RegionTargetPlan}), and without the guard blending
 * snapped the receiver toward its own document start instead of the matched region -- found live
 * 2026-07-17, scrolling the MAIN doc (which starts at "Chapter 1") while linked to a reference
 * document that reaches "Chapter 1" only after 12 unmatched front-matter sections.
 */
export function shouldBlendTowardScrollProgress(plan: RegionTargetPlan): boolean {
    if (plan.kind !== 'exact') return false;
    return plan.atDocumentStart || plan.atDocumentEnd;
}

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
 * Fraction of the scroll range over which to blend toward whole-container progress at each end.
 */
const EXTREME_BLEND_BAND = 0.15;

/**
 * Blend an offset-anchored scrollTop toward whole-container scroll progress near the extremes.
 *
 * Offset-anchoring aligns viewport top positions, so near a scroll extreme it lands short of this editor's
 * own extreme (the two editors have different amounts of content below the shared top block). A
 * pure progress mapping hits the extremes exactly but drifts in the interior. Weighting toward
 * progress only within a band next to each extreme makes the extremes exact AND keeps the interior
 * offset-accurate, with no snap at the boundary between the two regimes.
 */
export function blendTowardScrollProgress(
    anchoredTop: number,
    scrollProgress: number | undefined,
    maxScroll: number
): number {
    if (scrollProgress === undefined || maxScroll <= 0) return anchoredTop;

    const progressTop = scrollProgress * maxScroll;
    const p = scrollProgress;
    // 1 at an extreme -> 0 by EXTREME_BLEND_BAND in from it.
    const nearness = Math.max(0, 1 - Math.min(p, 1 - p) / EXTREME_BLEND_BAND);
    return nearness * progressTop + (1 - nearness) * anchoredTop;
}

/**
 * Whether two {@code scrollProgress} values would drive {@link blendTowardScrollProgress} to the
 * same place, for the per-frame dedup in {@link ApacheAnnotatorSyncController}. Uses the same 0.01
 * tolerance as the fraction comparison there.
 *
 * A present value never matches an absent one: dropping progress switches the receiver from the
 * blended target back to the bare offset anchor, which is a real move, not a no-op.
 */
export function sameScrollProgress(a: number | undefined, b: number | undefined): boolean {
    if (a === undefined || b === undefined) return a === b;
    return Math.abs(a - b) < 0.01;
}

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

    /**
     * Last viewport position applied by {@link scrollToViewportPosition}, for per-frame dedup.
     *
     * {@code scrollProgress} is part of the key because it independently moves the applied
     * scrollTop via the extreme blend: two positions agreeing on begin/fraction can still target
     * different pixels near an extreme, and dropping progress from the key would dedup those away.
     *
     * Dedup is per-regime, never across the two: the by-key path ({@link scrollToRegion}) and the
     * offset path ({@link scrollToOffset}) compute different pixel targets from the same
     * begin/fraction (region-fractional vs. block-anchored), so a value applied by one regime must
     * not suppress a re-apply by the other. Each guard therefore also checks {@code sectionKey}:
     * {@link scrollToRegion} requires the last apply to have been by-key ({@code sectionFraction}
     * present), {@link scrollToOffset} requires it to have been offset ({@code sectionKey} absent).
     */
    private lastAppliedViewportScrollPosition:
        | {
              begin: number;
              fraction: number;
              scrollProgress: number | undefined;
              sectionKey?: string;
              sectionFraction?: number;
          }
        | undefined = undefined;

    /** The hub this controller has registered its editor with, and the id it used. */
    private hub?: ViewportSyncPeer;
    private hubPeerId?: string;

    /** Document-structure inputs for by-key sync; undefined until {@link setDocumentStructure}. */
    private structure?: SyncStructureAccessors;

    /**
     * Memoized flat scroll-sync regions for the current tocRoot, so per-scroll region lookup does not
     * re-walk the TOC. Rebuilt lazily when the tocRoot identity changes (e.g. a new document is
     * loaded), which is why the tocRoot it was built from is remembered alongside it.
     */
    private regions?: SyncRegion[];
    private regionsTocRoot?: TocLevel;
    /** The keys of {@link regions} in order, memoized alongside it so the per-frame producer path
     * (see {@link augmentWithSectionPosition}) reuses one array instead of remapping every frame. */
    private regionKeys?: string[];

    constructor(root: Element, getScrollContainer: () => Element | undefined) {
        this.root = root;
        this.getScrollContainer = getScrollContainer;
    }

    /**
     * Provide (or replace) the document-structure inputs used for by-key scroll sync. Called by the
     * editor once the document outline exists. Without this, the controller uses offset anchoring
     * only.
     */
    setDocumentStructure(structure: SyncStructureAccessors): void {
        this.structure = structure;
        // Drop any memoized regions; they will be rebuilt lazily from the new structure's tocRoot.
        this.regions = undefined;
        this.regionsTocRoot = undefined;
    }

    /**
     * Register {@code aEditor} with the scroll-sync {@code aHub} under {@code aId}, scoped to the
     * editor's current scroll container. This controller owns viewport-sync for the editor, so it is
     * the natural place to hand the container to the hub - the container getter it already holds is
     * exactly the scope the hub needs. The caller (the editor) is responsible for calling this only
     * once its viewport exists. Re-registers if called again (e.g. after the container is rebuilt).
     */
    connectToHub(aHub: ViewportSyncPeer, aId: string, aEditor: AnnotationEditor): void {
        this.hub = aHub;
        this.hubPeerId = aId;
        aHub.register(aId, aEditor, this.getScrollContainer() ?? undefined);
    }

    /** Remove the editor from the hub it was registered with via {@link connectToHub}. */
    disconnectFromHub(): void {
        if (this.hub && this.hubPeerId) {
            this.hub.unregister(this.hubPeerId);
        }
        this.hub = undefined;
        this.hubPeerId = undefined;
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

        // Overall scroll progress lets the receiver blend toward its own extreme near the top/bottom
        // (see ViewportScrollPosition.scrollProgress), so scrolling just off an extreme no longer snaps.
        const maxScroll = container.scrollHeight - container.clientHeight;
        const scrollProgress = maxScroll > 0 ? container.scrollTop / maxScroll : 0;

        const pos: ViewportScrollPosition = { begin, end, fraction, scrollProgress };

        // Additive by-key augmentation (Rule 2). Leaves the offset result above untouched; only
        // attaches a region key + intra-region fraction when the viewport top falls in a keyed region.
        // Any missing step (no structure, no region) leaves the fields unset, and the receiver falls
        // back to offset anchoring.
        this.augmentWithSectionPosition(pos, container);

        return pos;
    }

    /**
     * Attach {@code sectionKey} / {@code sectionFraction} to {@code pos} for by-key sync, using the
     * flat scroll-sync region containing the viewport top. Pure follow-on: it never alters the offset
     * fields, so a receiver without matching regions still has the offset path to fall back on.
     *
     * The fraction is in PIXEL space, not character space: characters are not evenly distributed down
     * a region (a heading is few characters but many pixels tall), so a char fraction would advance
     * unevenly against a smooth scroll. The viewport-top scroll position is monotonic with scrolling,
     * and the receiver works in the same pixel space as its exact inverse.
     */
    private augmentWithSectionPosition(pos: ViewportScrollPosition, container: Element): void {
        const regions = this.syncRegions();
        if (!regions) return; // no structure/outline -> offset anchoring only

        // The viewport top expressed as a scroll position (0 = document top). Region tops are in the
        // same coordinate (see regionTopScrollPos), so bracketing is a direct comparison.
        const viewportTop = container.scrollTop;
        const containerTop = container.getBoundingClientRect().top;

        for (let i = 0; i < regions.length; i++) {
            const topPx = this.regionTopScrollPos(container, regions[i], containerTop);
            const bottomPx =
                i + 1 < regions.length
                    ? this.regionTopScrollPos(container, regions[i + 1], containerTop)
                    : container.scrollHeight;
            // Bracket the viewport top; the last region also catches anything at/after its bottom.
            const isLast = i === regions.length - 1;
            if (viewportTop < topPx) break; // regions are sorted; nothing earlier can match
            if (viewportTop < bottomPx || isLast) {
                const height = bottomPx - topPx;
                if (height <= 0) return;
                pos.sectionKey = regions[i].key;
                pos.sectionFraction = Math.min(1, Math.max(0, (viewportTop - topPx) / height));
                pos.sectionKeySequence = this.regionKeys;
                return;
            }
        }
    }

    /**
     * The scroll position (0 = document top) at which a region's top reaches the viewport top. For
     * the leading region (no start element) that is 0; otherwise it is the section element's top
     * translated into scroll-position space. Shared by the producer's region lookup and the receiver's
     * placement so both sides agree on region boundaries exactly.
     */
    private regionTopScrollPos(
        container: Element,
        region: SyncRegion,
        containerTop = container.getBoundingClientRect().top
    ): number {
        if (!region.startElement) return 0;
        return container.scrollTop + (this.elementTop(region.startElement) - containerTop);
    }

    scrollToViewportPosition(pos: ViewportScrollTarget): void {
        const container = this.getScrollContainer();
        if (!container) return;

        // By-key sync (Rule 2): if the source tagged a region key this document also has, scroll
        // region-fractionally (the inverse of the producer). Otherwise fall back to offset anchoring.
        if (pos.sectionKey !== undefined && this.scrollToRegion(container, pos)) return;

        this.scrollToOffset(container, pos);
    }

    /**
     * Region-fractional scroll. Resolves `pos.sectionKey` against this document's own regions via
     * {@link resolveRegionTarget}:
     * - an exact region match scrolls to `region.start + fraction*(regionHeight)`, the inverse of the
     *   producer (unchanged from before bracketing existed);
     * - a miss that brackets between two shared keys interpolates the viewport between those regions'
     *   boundaries;
     * - a miss with only a "before" or only an "after" bracketing key freezes at that region's
     *   boundary (the sender is past the last, or before the first, key shared with this document);
     * - no bracket at all (`kind: 'none'`) returns false so the caller falls back to offset anchoring.
     */
    private scrollToRegion(container: Element, pos: ViewportScrollTarget): boolean {
        const regions = this.syncRegions();
        if (!regions || pos.sectionKey === undefined) return false;

        const plan = resolveRegionTarget(
            regions,
            pos.sectionKey,
            pos.sectionKeySequence,
            pos.sectionFraction ?? 0
        );
        if (plan.kind === 'none') return false;

        // Region top/bottom as scroll positions, via the SAME helper the producer uses so both sides
        // agree on the boundary exactly. Bottom = the next region's top, or the document bottom for
        // the last region.
        const regionBoundary = (index: number): number => {
            const next = regions[index + 1];
            return next ? this.regionTopScrollPos(container, next) : container.scrollHeight;
        };

        let target: number;
        switch (plan.kind) {
            case 'exact': {
                const topPx = this.regionTopScrollPos(container, regions[plan.regionIndex]);
                const bottomPx = regionBoundary(plan.regionIndex);
                const height = bottomPx - topPx;
                if (height <= 0) return false;
                const frac = Math.min(1, Math.max(0, pos.sectionFraction ?? 0));
                target = topPx + frac * height;
                break;
            }
            case 'freeze-after': {
                // Past the last shared key: hold at that region's own end (its boundary with
                // whatever unmatched content follows it here), rather than following the sender
                // further into content this document doesn't have.
                target = regionBoundary(plan.regionIndex);
                break;
            }
            case 'freeze-before': {
                // Before the first shared key: hold at that region's start.
                target = this.regionTopScrollPos(container, regions[plan.regionIndex]);
                break;
            }
            case 'interpolate': {
                const topPx = this.regionTopScrollPos(container, regions[plan.beforeRegionIndex]);
                const bottomPx = this.regionTopScrollPos(container, regions[plan.afterRegionIndex]);
                target = topPx + plan.progress * (bottomPx - topPx);
                break;
            }
        }

        // Dedup identical re-applies (per-frame), same axis as the offset path. For 'freeze-before'/
        // 'freeze-after' this under-dedups (the sender's sectionFraction keeps changing while frozen,
        // so the comparison below never matches and scrollTop is reassigned every frame) -- harmless
        // since the reassigned value is the same pixel target each time, just not free.
        const frac = Math.min(1, Math.max(0, pos.sectionFraction ?? 0));
        const last = this.lastAppliedViewportScrollPosition;
        if (
            last &&
            last.sectionKey === pos.sectionKey &&
            last.sectionFraction !== undefined &&
            Math.abs(last.sectionFraction - frac) < 0.001 &&
            sameScrollProgress(last.scrollProgress, pos.scrollProgress)
        ) {
            return true;
        }
        this.lastAppliedViewportScrollPosition = {
            begin: pos.begin,
            fraction: Math.min(1, Math.max(0, pos.fraction)),
            scrollProgress: pos.scrollProgress,
            sectionKey: pos.sectionKey,
            sectionFraction: frac,
        };

        // See shouldBlendTowardScrollProgress for why only an 'exact' match AT A CORRESPONDING
        // DOCUMENT EXTREME blends toward the sender's overall scrollProgress; everything else applies
        // `target` as-is.
        if (shouldBlendTowardScrollProgress(plan)) {
            const maxScroll = container.scrollHeight - container.clientHeight;
            container.scrollTop = blendTowardScrollProgress(target, pos.scrollProgress, maxScroll);
        } else {
            container.scrollTop = target;
        }
        return true;
    }

    /**
     * Read a region start element's viewport-top. Pure measurement: this runs on the read/measure
     * path ({@link getViewportScrollPosition} via {@link regionTopScrollPos}, every scroll frame),
     * so it must never write to the DOM. Un-secluding here would strip {@code iaa-secluded} off
     * sections merely being measured, permanently defeating the seclusion/windowing optimization for
     * large documents. It is also unnecessary: a top-level secluded section is {@code visibility:
     * hidden}, which keeps its layout box, so its top measures correctly while still hidden. (The
     * offset scroll path in {@link scrollToOffset} legitimately unhides its target -- it is about to
     * scroll to that block, not just measure it.)
     */
    private elementTop(element: Element): number {
        return element.getBoundingClientRect().top;
    }

    /** Offset-anchored scroll, used when by-key sync does not apply. */
    private scrollToOffset(container: Element, pos: ViewportScrollTarget): void {
        const range = offsetToRange(this.root, pos.begin, pos.begin);
        if (!range) return;

        const start = range.startContainer;
        const startElement = start instanceof Element ? start : start.parentElement;
        const block = this.closestAnchorElement(startElement);
        if (!block) return;

        // Skip if the scroll request would not actually move the scroll position
        const fraction = Math.min(1, Math.max(0, pos.fraction));
        const last = this.lastAppliedViewportScrollPosition;
        if (
            last &&
            last.sectionKey === undefined && // only dedup against a prior offset-path apply (see field doc)
            last.begin === pos.begin &&
            Math.abs(last.fraction - fraction) < 0.01 &&
            sameScrollProgress(last.scrollProgress, pos.scrollProgress)
        ) {
            return;
        }
        this.lastAppliedViewportScrollPosition = {
            begin: pos.begin,
            fraction,
            scrollProgress: pos.scrollProgress,
        };

        // If necessary unhide the target block before measuring. We unhide only if hidden because a write (even a noop one) to the DOM can invalidate the layout
        if (block.closest('.iaa-secluded')) {
            removeClassFromAncestors(block, 'iaa-secluded', this.root);
        }

        const containerRect = container.getBoundingClientRect();
        const blockRect = block.getBoundingClientRect();
        const anchoredTop =
            container.scrollTop + (blockRect.top - containerRect.top) + fraction * blockRect.height;

        const maxScroll = container.scrollHeight - container.clientHeight;
        container.scrollTop = blendTowardScrollProgress(anchoredTop, pos.scrollProgress, maxScroll);
    }

    /**
     * The current document's flat scroll-sync regions, memoized and rebuilt when the tocRoot changes.
     * Returns undefined when there is no structure/outline (then sync uses plain offset anchoring).
     */
    private syncRegions(): SyncRegion[] | undefined {
        const tocRoot = this.structure?.tocRoot();
        if (!tocRoot) return undefined;

        if (this.regions === undefined || this.regionsTocRoot !== tocRoot) {
            const docEnd = calculateEndOffset(this.root, this.root);
            this.regions = buildSyncRegions(this.root, tocRoot, docEnd);
            this.regionKeys = this.regions.map((r) => r.key);
            this.regionsTocRoot = tocRoot;
        }
        return this.regions.length ? this.regions : undefined;
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

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
import { describe, it, expect, beforeEach, afterEach } from 'vitest';
import { buildDocumentStructure, type TocLevel } from '@inception-project/inception-js-api';

import {
    blendTowardScrollProgress,
    buildSyncRegions,
    LEADING_REGION_KEY,
    resolveRegionTarget,
    sameScrollProgress,
    shouldBlendTowardScrollProgress,
    type SyncRegion,
} from './ApacheAnnotatorSyncController';
import { calculateEndOffset } from '@inception-project/inception-js-api';

// The band over which the blend ramps, mirrored from the implementation. Kept as a literal rather
// than imported so a change to the constant makes these expectations fail loudly instead of
// silently re-deriving themselves.
const BAND = 0.15;
const MAX_SCROLL = 1000;

describe('blendTowardScrollProgress', () => {
    describe('pass-through cases', () => {
        it('returns the anchored top unchanged when the source sent no progress', () => {
            // The pre-extension wire format: peers that never send progress must keep the old
            // pure offset-anchored behaviour.
            expect(blendTowardScrollProgress(400, undefined, MAX_SCROLL)).toBe(400);
        });

        it('returns the anchored top unchanged when the container cannot scroll', () => {
            expect(blendTowardScrollProgress(400, 0.5, 0)).toBe(400);
        });

        it('returns the anchored top unchanged for a negative scroll range', () => {
            // scrollHeight < clientHeight can yield a negative maxScroll; treat as unscrollable
            // rather than blending toward a nonsensical negative progressTop.
            expect(blendTowardScrollProgress(400, 0.5, -50)).toBe(400);
        });
    });

    describe('at the extremes, progress wins exactly', () => {
        // The whole point of the blend: an offset anchor lands short of the extreme because the two
        // editors hold different amounts of content below the shared top block. At p=0/p=1 the
        // anchored value must be overridden completely, or scrolling to an end never quite arrives.
        it('snaps to the very top at progress 0, ignoring a short anchored top', () => {
            expect(blendTowardScrollProgress(120, 0, MAX_SCROLL)).toBe(0);
        });

        it('snaps to the very bottom at progress 1, ignoring a short anchored top', () => {
            expect(blendTowardScrollProgress(880, 1, MAX_SCROLL)).toBe(MAX_SCROLL);
        });
    });

    describe('in the interior, the offset anchor wins', () => {
        it('leaves the anchored top untouched at the exact band edge', () => {
            // nearness hits 0 precisely at BAND in from an extreme.
            expect(blendTowardScrollProgress(400, BAND, MAX_SCROLL)).toBeCloseTo(400);
            expect(blendTowardScrollProgress(400, 1 - BAND, MAX_SCROLL)).toBeCloseTo(400);
        });

        it('leaves the anchored top untouched in mid-document', () => {
            expect(blendTowardScrollProgress(400, 0.5, MAX_SCROLL)).toBeCloseTo(400);
        });

        it('leaves the anchored top untouched well outside the band', () => {
            expect(blendTowardScrollProgress(250, 0.25, MAX_SCROLL)).toBeCloseTo(250);
            expect(blendTowardScrollProgress(750, 0.75, MAX_SCROLL)).toBeCloseTo(750);
        });
    });

    describe('within the band, the two regimes mix', () => {
        it('weights progress and anchor evenly at half the band from the top', () => {
            // p = BAND/2 -> nearness = 0.5, so the result is the midpoint of the two candidates.
            const p = BAND / 2;
            const progressTop = p * MAX_SCROLL; // 75
            const anchoredTop = 200;
            expect(blendTowardScrollProgress(anchoredTop, p, MAX_SCROLL)).toBeCloseTo(
                0.5 * progressTop + 0.5 * anchoredTop
            );
        });

        it('weights progress and anchor evenly at half the band from the bottom', () => {
            const p = 1 - BAND / 2;
            const progressTop = p * MAX_SCROLL; // 925
            const anchoredTop = 800;
            expect(blendTowardScrollProgress(anchoredTop, p, MAX_SCROLL)).toBeCloseTo(
                0.5 * progressTop + 0.5 * anchoredTop
            );
        });

        it('moves monotonically from the anchor toward progress as an extreme nears', () => {
            // Approaching the top with an anchor that overshoots it: each step must pull further
            // toward progressTop, never bounce back. A non-monotonic ramp would read as jitter.
            const anchoredTop = 300;
            const results = [BAND, BAND * 0.75, BAND * 0.5, BAND * 0.25, 0].map((p) =>
                blendTowardScrollProgress(anchoredTop, p, MAX_SCROLL)
            );
            for (let i = 1; i < results.length; i++) {
                expect(results[i]).toBeLessThan(results[i - 1]);
            }
        });

        it('is continuous across the band edge, so there is no snap', () => {
            // Just inside vs. just outside the band must differ only marginally - the property that
            // makes blending preferable to switching regimes at a threshold.
            const anchoredTop = 300;
            const outside = blendTowardScrollProgress(anchoredTop, BAND + 0.001, MAX_SCROLL);
            const inside = blendTowardScrollProgress(anchoredTop, BAND - 0.001, MAX_SCROLL);
            expect(Math.abs(outside - inside)).toBeLessThan(2);
        });
    });
});

describe('sameScrollProgress', () => {
    // This guards the dedup key in ApacheAnnotatorSyncController: near an extreme, two positions
    // agreeing on begin/fraction can still blend to different pixels, so progress must take part in
    // the comparison or those scrolls get dropped as duplicates.
    it('treats values within the dedup tolerance as the same', () => {
        expect(sameScrollProgress(0.5, 0.5)).toBe(true);
        expect(sameScrollProgress(0.5, 0.505)).toBe(true);
    });

    it('treats values beyond the dedup tolerance as different', () => {
        expect(sameScrollProgress(0.5, 0.52)).toBe(false);
        expect(sameScrollProgress(0, 1)).toBe(false);
    });

    it('treats two absent values as the same', () => {
        expect(sameScrollProgress(undefined, undefined)).toBe(true);
    });

    it('never matches a present value against an absent one', () => {
        // Losing progress switches the receiver from the blended target back to the bare offset
        // anchor - a real move, not a no-op.
        expect(sameScrollProgress(0.5, undefined)).toBe(false);
        expect(sameScrollProgress(undefined, 0.5)).toBe(false);
        // Zero is a present value, not an absence - the top extreme must not read as "no progress".
        expect(sameScrollProgress(0, undefined)).toBe(false);
    });
});

describe('buildSyncRegions', () => {
    // Regions partition the document by keyed-section START offsets into a flat, non-overlapping
    // list. calculateStartOffset is character-based (not layout), so this is exercisable in jsdom.
    let container: HTMLElement;

    beforeEach(() => {
        container = document.createElement('div');
        document.body.appendChild(container);
    });

    afterEach(() => {
        while (document.body.firstChild) {
            document.body.removeChild(document.body.firstChild);
        }
    });

    // A minimal structure strategy: sections are <sec> elements; the key is their data-key attr.
    const strategy = {
        sectionSelector: 'sec',
        preprocess: () => {},
        extractTitle: (s: Element) => s.getAttribute('data-title') ?? undefined,
        extractKey: (s: Element) => s.getAttribute('data-key') ?? undefined,
    };

    function sec(text: string, key: string | undefined, ...children: Node[]): HTMLElement {
        const e = document.createElement('sec');
        e.setAttribute('data-title', text);
        if (key !== undefined) e.setAttribute('data-key', key);
        e.appendChild(document.createTextNode(text));
        for (const c of children) e.appendChild(c);
        return e;
    }

    function regions() {
        const toc: TocLevel = buildDocumentStructure(container, strategy);
        return buildSyncRegions(container, toc, calculateEndOffset(container, container));
    }

    it('partitions the document into non-overlapping regions in document order', () => {
        // A leading paragraph (no section), then two sibling keyed sections.
        container.appendChild(document.createTextNode('intro'));
        container.appendChild(sec('AAAA', 'k-a'));
        container.appendChild(sec('BB', 'k-b'));

        const rs = regions();
        // leading [0,5) then k-a [5,9) then k-b [9,11)
        expect(rs.map((r) => r.key)).toEqual([LEADING_REGION_KEY, 'k-a', 'k-b']);
        expect(rs.map((r) => [r.start, r.end])).toEqual([
            [0, 5],
            [5, 9],
            [9, 11],
        ]);
        // No overlap: each region's end is the next region's start.
        for (let i = 1; i < rs.length; i++) expect(rs[i].start).toBe(rs[i - 1].end);
    });

    it('uses a nested section START as a boundary, running the parent region only up to it', () => {
        // Chapter "CH" contains child "SUB"; the chapter region must END where SUB begins, not
        // continue underneath it -- that non-overlap is the whole point.
        const sub = sec('SUB', 'k-sub');
        const chapter = sec('CH', 'k-ch', sub);
        container.appendChild(chapter);

        const rs = regions();
        // "CH" text is offset 0..2, then SUB starts at 2.
        expect(rs.map((r) => r.key)).toEqual(['k-ch', 'k-sub']);
        expect(rs[0].start).toBe(0);
        expect(rs[0].end).toBe(2); // chapter region stops at SUB start
        expect(rs[1].start).toBe(2); // sub region starts there, no overlap
    });

    it('ignores keyless sections (their content belongs to the enclosing region)', () => {
        container.appendChild(sec('AAAA', 'k-a', sec('xx', undefined)));

        const rs = regions();
        expect(rs.map((r) => r.key)).toEqual(['k-a']);
    });

    it('keeps the first key when two sections share a start offset / key', () => {
        // Two sections at the same offset can't both open a region; first in doc order wins.
        const first = sec('First', 'dup');
        container.appendChild(first);
        // second section immediately after, distinct offset, same key -> deduped by key
        container.appendChild(sec('Second', 'dup'));

        const rs = regions();
        // leading region is empty (first section at 0), so only the 'dup' region(s) — deduped to one
        expect(rs.filter((r) => r.key === 'dup').length).toBe(1);
    });

    it('produces a single leading region spanning the whole document when there are no sections', () => {
        container.appendChild(document.createTextNode('just text'));
        const rs = regions();
        expect(rs.length).toBe(1);
        expect(rs[0].key).toBe(LEADING_REGION_KEY);
        expect(rs[0].start).toBe(0);
        expect(rs[0].end).toBe('just text'.length);
    });
});

describe('resolveRegionTarget', () => {
    // Pure key-order reasoning, no geometry -- regions only need a `key` for these tests, so offsets
    // are irrelevant filler.
    function region(key: string): SyncRegion {
        return { key, start: 0, end: 0 };
    }

    it('matches directly when the receiver has a region for the key (interior, not at either extreme)', () => {
        const regions = [region('a'), region('b'), region('c')];
        expect(resolveRegionTarget(regions, 'b', ['a', 'b', 'c'])).toEqual({
            kind: 'exact',
            regionIndex: 1,
            atDocumentStart: false,
            atDocumentEnd: false,
        });
    });

    it('matches directly even with no sender sequence supplied, defaulting both extreme flags true (nothing to disprove correspondence)', () => {
        const regions = [region('a'), region('b')];
        expect(resolveRegionTarget(regions, 'b', undefined)).toEqual({
            kind: 'exact',
            regionIndex: 1,
            atDocumentStart: false,
            atDocumentEnd: true,
        });
    });

    it('reports no bracket when the key is absent from the sender sequence entirely', () => {
        const regions = [region('a'), region('b')];
        // 'z' isn't in the sender's own sequence -- can't bracket at all.
        expect(resolveRegionTarget(regions, 'z', ['a', 'b'])).toEqual({ kind: 'none' });
        expect(resolveRegionTarget(regions, 'z', undefined)).toEqual({ kind: 'none' });
    });

    it('reports no bracket when neither neighbour of the missing key exists locally', () => {
        const regions = [region('a')];
        // 'z' sits between two other keys the receiver also lacks.
        expect(resolveRegionTarget(regions, 'z', ['y', 'z', 'x'])).toEqual({ kind: 'none' });
    });

    it('interpolates between the nearest shared keys when the sender key is an interior gap (the "Characters" case)', () => {
        // Sender: Chapter 1, Characters (unmatched), Chapter 2. Receiver only has Chapter 1 / Chapter 2.
        const regions = [region('chapter-1'), region('chapter-2')];
        const senderSeq = ['chapter-1', 'characters', 'chapter-2'];

        const plan = resolveRegionTarget(regions, 'characters', senderSeq, 0.5);
        expect(plan).toEqual({
            kind: 'interpolate',
            beforeRegionIndex: 0,
            afterRegionIndex: 1,
            progress: 0.75, // 1 key-step past chapter-1, plus 0.5 through "characters", out of 2 steps
        });
    });

    it('interpolates smoothly across the gap as the sender scrolls through the unmatched region', () => {
        const regions = [region('chapter-1'), region('chapter-2')];
        const senderSeq = ['chapter-1', 'characters', 'chapter-2'];

        const start = resolveRegionTarget(regions, 'characters', senderSeq, 0);
        const mid = resolveRegionTarget(regions, 'characters', senderSeq, 0.5);
        const end = resolveRegionTarget(regions, 'characters', senderSeq, 1);
        expect(start.kind).toBe('interpolate');
        expect(mid.kind).toBe('interpolate');
        expect(end.kind).toBe('interpolate');
        if (start.kind === 'interpolate' && mid.kind === 'interpolate' && end.kind === 'interpolate') {
            expect(start.progress).toBeLessThan(mid.progress);
            expect(mid.progress).toBeLessThan(end.progress);
        }
    });

    it('spans multiple unmatched keys between the same bracket', () => {
        const regions = [region('a'), region('e')];
        const senderSeq = ['a', 'b', 'c', 'd', 'e'];
        // 'c' is the middle of 3 unmatched keys between 'a' and 'e' -> halfway across.
        expect(resolveRegionTarget(regions, 'c', senderSeq)).toEqual({
            kind: 'interpolate',
            beforeRegionIndex: 0,
            afterRegionIndex: 1,
            progress: 0.5,
        });
    });

    it('freezes at the last shared region when the sender is past every key this document has (trailing gap)', () => {
        const regions = [region('chapter-1'), region('chapter-2')];
        const senderSeq = ['chapter-1', 'chapter-2', 'appendix'];

        expect(resolveRegionTarget(regions, 'appendix', senderSeq)).toEqual({
            kind: 'freeze-after',
            regionIndex: 1,
        });
    });

    it('freezes at the first shared region when the sender is before every key this document has (leading gap)', () => {
        const regions = [region('chapter-1'), region('chapter-2')];
        const senderSeq = ['preface', 'chapter-1', 'chapter-2'];

        expect(resolveRegionTarget(regions, 'preface', senderSeq)).toEqual({
            kind: 'freeze-before',
            regionIndex: 0,
        });
    });

    it('clamps progress into [0,1] even if sectionFraction is out of range', () => {
        const regions = [region('a'), region('c')];
        const senderSeq = ['a', 'b', 'c'];
        expect(resolveRegionTarget(regions, 'b', senderSeq, 5).kind).toBe('interpolate');
        const plan = resolveRegionTarget(regions, 'b', senderSeq, 5);
        if (plan.kind === 'interpolate') expect(plan.progress).toBe(1);
    });

    it('does not treat the shared LEADING_REGION_KEY sentinel as a real bracket (regression)', () => {
        // Both documents trivially have a leading region (every region list starts with one), but
        // their actual leading content is unrelated (e.g. a different title page/TOC). The sender is
        // still inside ITS leading region -- key equals LEADING_REGION_KEY exactly, which IS an exact
        // match here (both docs are "in their own leading region"), so this must resolve 'exact', not
        // interpolate toward chapter-1. Both sides' leading region is at index 0 of its own sequence,
        // so atDocumentStart is true here.
        const regions = [region(LEADING_REGION_KEY), region('chapter-1'), region('chapter-2')];
        const senderSeq = [LEADING_REGION_KEY, 'chapter-1', 'chapter-2'];

        expect(resolveRegionTarget(regions, LEADING_REGION_KEY, senderSeq, 0.5)).toEqual({
            kind: 'exact',
            regionIndex: 0,
            atDocumentStart: true,
            atDocumentEnd: false,
        });
    });

    it('reports atDocumentStart=false for an exact match at THIS document\'s first region when the SENDER\'s matching key is not at the start of ITS sequence (the "Chapter 1" asymmetric-extreme regression)', () => {
        // Live bug (2026-07-17): main doc starts directly at "1" (Chapter 1, index 0 of its own
        // sequence). The reference doc (tei-book1-outline.xml) shares that same key "1", but only
        // after 12 unmatched front-matter regions, so key "1" sits at regionIndex 13 there, NOT 0.
        // Scrolling the main doc from its very top (sender sectionKey "1", scrollProgress near 0)
        // must NOT snap the reference doc toward ITS OWN top (which would land it back on
        // "Characters") -- atDocumentStart must be false so the blend is skipped.
        const regions = [
            region('front-1'),
            region('front-2'),
            region('1'), // "Chapter 1" -- the shared key, NOT this document's first region
            region('2'),
        ];
        const senderSeq = ['1', '2']; // sender's OWN sequence: "1" opens it, at index 0

        const plan = resolveRegionTarget(regions, '1', senderSeq);
        expect(plan).toEqual({
            kind: 'exact',
            regionIndex: 2,
            atDocumentStart: false, // regionIndex 2 !== 0 locally, regardless of the sender's index 0
            atDocumentEnd: false,
        });
    });

    it('reports atDocumentStart=true when the exact match is at index 0 on BOTH sides (symmetric documents, the ordinary case)', () => {
        const regions = [region('1'), region('2'), region('3')];
        const senderSeq = ['1', '2', '3', '4'];

        expect(resolveRegionTarget(regions, '1', senderSeq)).toEqual({
            kind: 'exact',
            regionIndex: 0,
            atDocumentStart: true,
            atDocumentEnd: false,
        });
    });

    it('reports atDocumentEnd=true only when the match is at the last region on BOTH sides', () => {
        const regions = [region('1'), region('2')];
        // Sender's sequence has more content after '2' -> sender is not at ITS OWN end there.
        const senderSeqNotAtEnd = ['1', '2', '3'];
        expect(resolveRegionTarget(regions, '2', senderSeqNotAtEnd)).toEqual({
            kind: 'exact',
            regionIndex: 1,
            atDocumentStart: false,
            atDocumentEnd: false,
        });

        const senderSeqAtEnd = ['0', '1', '2'];
        expect(resolveRegionTarget(regions, '2', senderSeqAtEnd)).toEqual({
            kind: 'exact',
            regionIndex: 1,
            atDocumentStart: false,
            atDocumentEnd: true,
        });
    });

    it('freezes before chapter-1 while the sender is in an unmatched intro section, not interpolating via the leading sentinel (regression)', () => {
        // Refdoc structure: leading sentinel region, then an unmatched "toc" section (e.g. a table of
        // contents heading that has no counterpart in the main doc), then "chapter-1" which IS shared.
        // Before the LEADING_REGION_KEY exclusion, 'toc' would have found LEADING_REGION_KEY as its
        // "before" bracket (trivially present in both region lists) and interpolated the receiver
        // forward from its own leading region toward chapter-1 -- then snapped back once the sender
        // actually reached chapter-1's exact match. That produced the reported
        // scroll-down-then-back-up jump. The correct behaviour is freeze-before at chapter-1's start:
        // there is no genuinely shared anchor before it.
        const regions = [region(LEADING_REGION_KEY), region('chapter-1'), region('chapter-2')];
        const senderSeq = [LEADING_REGION_KEY, 'toc', 'chapter-1', 'chapter-2'];

        expect(resolveRegionTarget(regions, 'toc', senderSeq, 0.5)).toEqual({
            kind: 'freeze-before',
            regionIndex: 1, // chapter-1
        });
    });
});

describe('shouldBlendTowardScrollProgress', () => {
    // Regression #1 (found live 2026-07-17): resolveRegionTarget correctly returned 'freeze-before'
    // for every scroll step while the sender was in tei-book1-outline.xml's unmatched leading
    // sections, but the main doc still visibly scrolled down and back up before settling. Root cause
    // was NOT in resolveRegionTarget -- it was that scrollToRegion blended every resolved target
    // toward the SENDER's overall scrollProgress via blendTowardScrollProgress, unconditionally. That
    // blend is only meaningful for an 'exact' match (both docs at an equivalent point in shared
    // structure); for freeze/interpolate outcomes the sender's overall progress has no correspondence
    // to the receiver's frozen/bracketed position, so blending toward it dragged the "frozen" receiver
    // up and back down as the sender's progress crossed the extreme-blend band near the document top.
    //
    // Regression #2 (found live 2026-07-17, same day): even after fixing #1, scrolling the MAIN doc
    // (which opens directly on "Chapter 1") still snapped the reference doc to (approximately) its own
    // document start instead of to its "Chapter 1" region -- 13 regions in. The match WAS 'exact'
    // (both docs share the "Chapter 1" key), but the sender's overall scrollProgress-near-0 was blended
    // in anyway, because 'exact' alone was treated as sufficient. It is not: the reference doc's
    // "Chapter 1" is not at ITS OWN document start, so the sender being at ITS OWN start has no
    // correspondence to the receiver's position. Fix: 'exact' only blends when the match is ALSO at a
    // corresponding extreme on both sides (RegionTargetPlan.atDocumentStart / atDocumentEnd).
    it('blends only for an exact match at a corresponding document start', () => {
        expect(
            shouldBlendTowardScrollProgress({
                kind: 'exact',
                regionIndex: 0,
                atDocumentStart: true,
                atDocumentEnd: false,
            })
        ).toBe(true);
    });

    it('blends only for an exact match at a corresponding document end', () => {
        expect(
            shouldBlendTowardScrollProgress({
                kind: 'exact',
                regionIndex: 5,
                atDocumentStart: false,
                atDocumentEnd: true,
            })
        ).toBe(true);
    });

    it('does NOT blend for an exact match that is at neither extreme (interior match)', () => {
        expect(
            shouldBlendTowardScrollProgress({
                kind: 'exact',
                regionIndex: 2,
                atDocumentStart: false,
                atDocumentEnd: false,
            })
        ).toBe(false);
    });

    it('does NOT blend for an exact match at THIS document\'s extreme when the sender is not correspondingly at its own extreme (the asymmetric "Chapter 1" case)', () => {
        // This is the exact shape resolveRegionTarget would never actually produce (atDocumentStart
        // requires BOTH sides), included here to pin the predicate's own logic independent of how the
        // plan was built.
        expect(
            shouldBlendTowardScrollProgress({
                kind: 'exact',
                regionIndex: 0,
                atDocumentStart: false,
                atDocumentEnd: false,
            })
        ).toBe(false);
    });

    it('does not blend for freeze-before, freeze-after, or interpolate', () => {
        expect(shouldBlendTowardScrollProgress({ kind: 'freeze-before', regionIndex: 0 })).toBe(false);
        expect(shouldBlendTowardScrollProgress({ kind: 'freeze-after', regionIndex: 0 })).toBe(false);
        expect(
            shouldBlendTowardScrollProgress({
                kind: 'interpolate',
                beforeRegionIndex: 0,
                afterRegionIndex: 1,
                progress: 0.5,
            })
        ).toBe(false);
    });

    it('never blends for none (though none never reaches this call in practice)', () => {
        expect(shouldBlendTowardScrollProgress({ kind: 'none' })).toBe(false);
    });
});

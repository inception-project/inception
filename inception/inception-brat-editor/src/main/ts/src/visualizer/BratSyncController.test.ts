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
import { describe, it, expect } from 'vitest';

import {
    anchorRowForViewport,
    viewportScrollPositionFromRows,
    targetScrollTopForPosition,
} from './BratSyncController';
import { type RowGeometry } from './ScrollSyncRowCache';

// Three tiled rows of height 20, offsets contiguous. `top` is viewport-relative; the caller sets it
// per scenario to place the viewport top within / above / below a given row.
function rows(...tops: number[]): RowGeometry[] {
    return tops.map((top, i) => ({
        top,
        height: 20,
        begin: i * 100,
        end: i * 100 + 100,
    }));
}

describe('anchorRowForViewport', () => {
    it('returns null for no rows', () => {
        expect(anchorRowForViewport([])).toBeNull();
    });

    it('anchors on the last row whose top is at or above the viewport top', () => {
        // rows at -30, -10, 10: the viewport top (0) sits within the second row (top -10)
        const anchor = anchorRowForViewport(rows(-30, -10, 10));
        expect(anchor?.begin).toBe(100);
    });

    it('anchors on a row whose top is exactly at the viewport top', () => {
        const anchor = anchorRowForViewport(rows(-20, 0, 20));
        expect(anchor?.begin).toBe(100);
    });

    it('anchors on the first row when all rows are below the viewport top', () => {
        // Scrolled into the padding above the first row: every top > 0.
        const anchor = anchorRowForViewport(rows(5, 25, 45));
        expect(anchor?.begin).toBe(0);
    });

    it('anchors on the last row when all rows are above the viewport top', () => {
        const anchor = anchorRowForViewport(rows(-60, -40, -20));
        expect(anchor?.begin).toBe(200);
    });
});

describe('viewportScrollPositionFromRows', () => {
    it('returns null for no rows', () => {
        expect(viewportScrollPositionFromRows([], 0, 100)).toBeNull();
    });

    it('reports the anchor offsets and the intra-row fraction', () => {
        // Anchor is the second row (top -10, height 20): fraction = -(-10)/20 = 0.5
        const pos = viewportScrollPositionFromRows(rows(-30, -10, 10), 200, 1000);
        expect(pos).not.toBeNull();
        expect(pos!.begin).toBe(100);
        expect(pos!.end).toBe(200);
        expect(pos!.fraction).toBeCloseTo(0.5);
    });

    it('yields a negative fraction when scrolled above the first row', () => {
        // First row anchors with a positive top (10) -> fraction = -10/20 = -0.5
        const pos = viewportScrollPositionFromRows(rows(10, 30, 50), 0, 1000);
        expect(pos!.begin).toBe(0);
        expect(pos!.fraction).toBeCloseTo(-0.5);
    });

    it('clamps the fraction to at most 1', () => {
        // Only the first row is at/above the viewport top, so it anchors with top -40 (the later
        // rows are below the seam). raw fraction = -(-40)/20 = 2, clamped to 1.
        const pos = viewportScrollPositionFromRows(rows(-40, 10, 30), 500, 1000);
        expect(pos!.begin).toBe(0);
        expect(pos!.fraction).toBe(1);
    });

    it('computes scrollProgress as scrollTop / maxScroll', () => {
        const pos = viewportScrollPositionFromRows(rows(0, 20, 40), 250, 1000);
        expect(pos!.scrollProgress).toBeCloseTo(0.25);
    });

    it('reports zero progress when there is no scrollable range', () => {
        const pos = viewportScrollPositionFromRows(rows(0, 20, 40), 0, 0);
        expect(pos!.scrollProgress).toBe(0);
    });

    it('uses a zero fraction for a degenerate zero-height anchor', () => {
        const zeroHeight: RowGeometry[] = [{ top: -5, height: 0, begin: 0, end: 10 }];
        const pos = viewportScrollPositionFromRows(zeroHeight, 0, 100);
        expect(pos!.fraction).toBe(0);
    });
});

describe('targetScrollTopForPosition', () => {
    it('returns null for no rows', () => {
        expect(targetScrollTopForPosition([], { begin: 0, fraction: 0 }, 0, 100)).toBeNull();
    });

    it('anchors the target offset within the containing row', () => {
        // rows tiled at top 0/20/40 with scrollTop 1000. Target offset 150 lands in row 1
        // (begin 100, end 200): rowFraction = (150-100)/100 = 0.5.
        // anchoredTop = 1000 + row.top(20) + 0.5*height(20) = 1030.
        const target = targetScrollTopForPosition(
            rows(0, 20, 40),
            { begin: 100, end: 200, fraction: 0.5 },
            1000,
            10000
        );
        expect(target).toBeCloseTo(1030);
    });

    it('interpolates the incoming anchor in character space before locating the row', () => {
        // begin 100, end 200, fraction 0.5 -> targetOffset 150, same as above.
        const target = targetScrollTopForPosition(
            rows(0, 20, 40),
            { begin: 100, end: 200, fraction: 0.5 },
            0,
            10000
        );
        // row 1 top 20, rowFraction 0.5 -> 20 + 10 = 30
        expect(target).toBeCloseTo(30);
    });

    it('defaults end to begin when omitted (targetOffset == begin)', () => {
        const target = targetScrollTopForPosition(
            rows(0, 20, 40),
            { begin: 100, fraction: 0.5 },
            0,
            10000
        );
        // targetOffset = 100 -> row 1 begin, rowFraction 0 -> top 20
        expect(target).toBeCloseTo(20);
    });

    it('clamps to the first row when the offset is before the window', () => {
        const target = targetScrollTopForPosition(
            rows(0, 20, 40),
            { begin: -50, fraction: 0 },
            0,
            10000
        );
        // clamped to row 0 (top 0); offset below its begin -> rowFraction clamped to 0
        expect(target).toBeCloseTo(0);
    });

    it('clamps to the last row when the offset is after the window', () => {
        const target = targetScrollTopForPosition(
            rows(0, 20, 40),
            { begin: 5000, fraction: 0 },
            0,
            10000
        );
        // clamped to row 2 (top 40); offset above its end -> rowFraction clamped to 1 -> 40 + 20
        expect(target).toBeCloseTo(60);
    });

    it('picks the nearest row by offset when the target lands in an inter-row gap', () => {
        // Rows with a gap in offset space: [0,100] and [200,300]. Target 190 is nearest row 1's end.
        const gapped: RowGeometry[] = [
            { top: 0, height: 20, begin: 0, end: 100 },
            { top: 20, height: 20, begin: 200, end: 300 },
        ];
        const target = targetScrollTopForPosition(gapped, { begin: 190, fraction: 0 }, 0, 10000);
        // nearest is row 1 (|200-190|=10 < |100-190|=90); offset below its begin -> rowFraction 0 -> top 20
        expect(target).toBeCloseTo(20);
    });

    it('ignores the progress blend in the document interior', () => {
        // scrollProgress 0.5 is well outside the 0.15 band from either extreme -> nearness 0,
        // so the anchored target is returned unchanged.
        const target = targetScrollTopForPosition(
            rows(0, 20, 40),
            { begin: 100, end: 200, fraction: 0, scrollProgress: 0.5 },
            0,
            10000
        );
        expect(target).toBeCloseTo(20); // pure anchored value, no blend
    });

    it('lands exactly on the progress-mapped top at an extreme', () => {
        // scrollProgress 0 -> nearness 1 -> target = progressTop = 0 * maxScroll = 0,
        // regardless of the anchored value.
        const target = targetScrollTopForPosition(
            rows(0, 20, 40),
            { begin: 200, end: 300, fraction: 1, scrollProgress: 0 },
            0,
            10000
        );
        expect(target).toBeCloseTo(0);
    });

    it('blends partially within the transition band', () => {
        // scrollProgress 0.075 = half the 0.15 band -> nearness 0.5.
        // progressTop = 0.075 * 10000 = 750. anchored: begin 100 fraction 0 -> row1 top 20.
        // blended = 0.5*750 + 0.5*20 = 385.
        const target = targetScrollTopForPosition(
            rows(0, 20, 40),
            { begin: 100, end: 200, fraction: 0, scrollProgress: 0.075 },
            0,
            10000
        );
        expect(target).toBeCloseTo(385);
    });

    it('skips the blend when there is no scrollable range', () => {
        // maxScroll 0 -> blend guarded off, anchored value returned even though scrollProgress is set.
        const target = targetScrollTopForPosition(
            rows(0, 20, 40),
            { begin: 100, end: 200, fraction: 0, scrollProgress: 0 },
            0,
            0
        );
        expect(target).toBeCloseTo(20);
    });
});

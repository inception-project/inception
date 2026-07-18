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

import { blendTowardScrollProgress } from './ViewportSyncUtils';

// The band over which the blend ramps, mirrored from the implementation. Kept as a literal rather
// than imported so a change to the constant makes these expectations fail loudly instead of
// silently re-deriving themselves.
const BAND = 0.15;
const MAX_SCROLL = 1000;

describe('blendTowardScrollProgress', () => {
    describe('pass-through cases', () => {
        it('returns the anchored top unchanged when the source sent no progress', () => {
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
        // editors hold different amounts of content below the shared top page. At p=0/p=1 the
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

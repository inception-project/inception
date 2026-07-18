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

/**
 * Fraction of the scroll range over which to blend toward whole-container progress at each end.
 *
 * Deliberately a per-editor constant: brat (`BratSyncController.ts`) and the Apache Annotator
 * editor (`ApacheAnnotatorSyncController.ts`) each carry their own copy of this band and of
 * {@link blendTowardScrollProgress}. The three editors anchor at different granularities - rows,
 * pages (here), and block elements - so it is not established that one band suits all of them, and
 * a shared helper in `inception-js-api` would freeze that assumption into the public API. Fork this
 * value if PDF's page-anchoring turns out to want a different band.
 *
 * @see blendTowardScrollProgress
 */
const EXTREME_BLEND_BAND = 0.15;

/**
 * Blend an offset-anchored scrollTop toward whole-container scroll progress near the extremes.
 *
 * Offset-anchoring aligns viewport TOPS, so near a scroll extreme it lands short of this editor's
 * own extreme (the two editors have different amounts of content below the shared top page). A pure
 * progress mapping hits the extremes exactly but drifts in the interior. Weighting toward progress
 * only within a band next to each extreme makes the extremes exact AND keeps the interior
 * offset-accurate, with no snap at the boundary between the two regimes.
 *
 * Kept out of `pdfanno.ts` to stay clear of that module's global mutable state: the blend needs
 * only resolved numbers, which makes it the one piece of PDF viewport-sync that separates cleanly
 * and can be unit-tested on its own. A full sync-controller class like the other editors have
 * (`BratSyncController`, `ApacheAnnotatorSyncController`) was considered and judged not worth the
 * wider extraction here: the surrounding anchoring logic also needs `textLayer` for page/offset
 * mapping plus the page-element helpers, and what it would buy back is mostly geometry that can
 * only be tested against live layout anyway. Returns {@code anchoredTop} unchanged when the source
 * sent no progress or this container cannot scroll.
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

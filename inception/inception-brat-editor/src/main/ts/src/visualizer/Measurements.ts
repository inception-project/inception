/*
 * ## INCEpTION ##
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
 *
 * ## brat ##
 * Copyright (C) 2010-2012 The brat contributors, all rights reserved.
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of
 * this software and associated documentation files (the "Software"), to deal in
 * the Software without restriction, including without limitation the rights to
 * use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies
 * of the Software, and to permit persons to whom the Software is furnished to do
 * so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

import type { Container } from '@svgdotjs/svg.js';
import type { Marker } from './Chunk';
import { Fragment } from './Fragment';
import type { SegmenterAdapter, SegmenterAnalysis } from './SegmenterAnalysis';
import type { Svg, Text } from '@svgdotjs/svg.js';

const TRACE_MEASUREMENT = false;

export class Measurements {
    widths: Record<string, number>;
    height: number;
    y: number;

    constructor(widths: Record<string, number>, height: number, y: number) {
        this.widths = widths;
        this.height = height;
        this.y = y;
        Object.seal(this);
    }
}

/**
 * Calculate pixel positions for a Fragment within an SVG text element.
 *
 * Side effects:
 * - Mutates `fragment.curly` to an object `{ from: number, to: number }`.
 * - Throws an Error when the fragment's first character is not contained in its chunk.
 *
 * Behavior:
 * - Computes character indices relative to the containing `chunk` and
 *   adjusts for XML whitespace collapsing.
 * - Uses `calculateSubstringPositionRobust` for substring positioning.
 *
 * @param fragment - Fragment to measure (must have `from`, `to`, and `chunk`).
 * @param text - SVG text element used for measurement.
 * @param rtlmode - Whether RTL measurement rules apply.
 * @throws {Error} When the fragment is not contained in its declared chunk.
 */
export function calculateFragmentTextElementMeasure(
    fragment: Fragment,
    text: SVGTextElement,
    rtlmode: boolean,
    segmenterAdapter?: SegmenterAdapter
): void {
    if (TRACE_MEASUREMENT) {
        console.trace(
            '[calculateFragmentTextElementMeasure] Chunk ',
            [fragment.chunk.from, fragment.chunk.to, fragment.chunk.text],
            ' - Measuring fragment',
            [fragment.from, fragment.to, fragment.labelText]
        );
    }

    let firstChar = fragment.from - fragment.chunk.from;
    if (firstChar < 0) {
        throw new Error(
            `Fragment [${fragment.from}, ${fragment.to}] (${fragment.text}) is not contained in its designated chunk [${fragment.chunk.from}, ${fragment.chunk.to}]. Aborting rendering due to inconsistent source data.`
        );
    }
    let lastChar = fragment.to - fragment.chunk.from - 1;

    // Adjust for XML whitespace (#832, #1009)
    const textUpToFirstChar = fragment.chunk.text.substring(0, firstChar);
    const textUpToLastChar = fragment.chunk.text.substring(0, lastChar);
    const textUpToFirstCharUnspaced = textUpToFirstChar.replace(/\s\s+/g, ' ');
    const textUpToLastCharUnspaced = textUpToLastChar.replace(/\s\s+/g, ' ');
    firstChar -= textUpToFirstChar.length - textUpToFirstCharUnspaced.length;
    lastChar -= textUpToLastChar.length - textUpToLastCharUnspaced.length;

    // Handle zero-width or invalid ranges (e.g., lastChar < firstChar)
    // so we don't pass illegal ranges into the robust RTL routine.
    // if (lastChar < firstChar) {
    //     let startPos: number, endPos: number;
    //     [startPos, endPos] = calculateSubstringPositionRobust(
    //         fragment,
    //         text,
    //         firstChar,
    //         lastChar,
    //         rtlmode,
    //         segmenterAdapter
    //     );

    //     fragment.curly = {
    //         from: Math.min(startPos, endPos),
    //         to: Math.max(startPos, endPos),
    //     };
    //     console.debug("[calculateFragmentTextElementMeasure] Chunk ", [fragment.chunk.from, fragment.chunk.to, fragment.chunk.text], " - Set zero-width fragment curlies to ", [fragment.curly.from, fragment.curly.to]);
    //     return;
    // }

    let startPos: number, endPos: number;
    //        if (rtlmode) {
    // This rendering is much slower than the "old" version that brat uses, but it is more reliable in RTL mode.
    [startPos, endPos] = calculateSubstringPositionRobust(
        fragment,
        text,
        firstChar,
        lastChar,
        rtlmode,
        segmenterAdapter
    );
    // In RTL mode, positions are negative (left to right) - seems not to be the case anymore since
    // we use the intl.Segmenter
    // startPos = -startPos;
    // endPos = -endPos;
    // } else {
    //     // Using the old faster method in LTR mode. YES, this means that subtoken annotations of RTL
    //     // tokens in LTR mode will render incorrectly. If somebody needs that, we should do a smarter
    //     // selection of the rendering mode. This is the old measurement code which doesn't work
    //     // properly because browsers treat the x coordinate very differently. Our width-based
    //     // measurement is more reliable.
    //     [startPos, endPos] = calculateSubstringPositionFast(text, firstChar, lastChar);
    // }

    // Make sure that startPos and endPos are properly ordered on the X axis
    fragment.curly = {
        from: Math.min(startPos, endPos),
        to: Math.max(startPos, endPos),
    };

    if (TRACE_MEASUREMENT) {
        console.trace(
            '[calculateFragmentTextElementMeasure] Chunk ',
            [fragment.chunk.from, fragment.chunk.to, fragment.chunk.text],
            ' - Set fragment curlies to ',
            [fragment.curly.from, fragment.curly.to]
        );
    }
}

/**
 * Try to compute substring pixel coordinates using a grapheme-cluster segmenter.
 *
 * This routine uses a provided `SegmenterAdapter` (if available) to obtain a
 * `SegmenterAnalysis` (cached on `fragment.chunk.segmenterAnalysis`) that lists
 * grapheme cluster start indices (`clusterStarts`). It maps the logical
 * character range `firstChar..lastChar` (0-based, relative to the chunk text)
 * to the DOM character indices of the first and last covered clusters and
 * queries the `SVGTextElement` for the corresponding start/end X positions.
 *
 * If successful this returns a two-element array `[startX, endX]` with pixel
 * coordinates relative to the start of `text`. If the adapter is missing,
 * yields no usable clusters, or a DOM measurement fails, the function
 * returns `null` so the caller can fall back to the legacy measurement.
 *
 * @param {Fragment} fragment - Fragment whose chunk is used for caching and text access.
 * @param {SVGTextElement} text - SVG text element used for DOM measurements.
 * @param {number} firstChar - Logical index (0-based, relative to the chunk text) of first character.
 * @param {number} lastChar - Logical index (0-based, relative to the chunk text) of last character.
 *                           May be < `firstChar` for zero-width ranges.
 * @param {SegmenterAdapter} [segAdapter] - Optional adapter implementing `analyzeText(text): SegmenterAnalysis`.
 * @returns {[number, number] | null} `[startX, endX]` in pixels, or `null` when not applicable.
 *
 * Notes: DOM exceptions during `getStartPositionOfChar`/`getEndPositionOfChar`
 * are caught and logged; callers must handle `null` by using the legacy path.
 */
export function calculateSubstringPositionSegmenter(
    fragment: Fragment,
    text: SVGTextElement,
    firstChar: number,
    lastChar: number,
    segAdapter?: SegmenterAdapter
): [number, number] | null {
    if (!text.textContent || text.textContent.length === 0) {
        return [0, 0];
    }

    // Attempt to use segmenter analysis for more reliable character-to-glyph mapping,
    // especially in BiDi/mixed-direction scenarios. The analysis is cached on the chunk
    // to avoid redundant adapter calls for multiple fragments within the same chunk.
    let analysis: SegmenterAnalysis | undefined;
    try {
        analysis = fragment.chunk.segmenterAnalysis;
        if (!analysis && segAdapter) {
            const domText = text.textContent || '';
            fragment.chunk.segmenterAnalysis = segAdapter.analyzeText(domText);
            analysis = fragment.chunk.segmenterAnalysis;
        }
    } catch (e) {
        console.warn(
            '[calculateSubstringPositionRobust] Segmenter failed, falling back to legacy measurement',
            e
        );
        return null;
    }

    try {
        if (!analysis) {
            // No analysis available: caller should fall back to legacy measurement.
            return null;
        }
        if (analysis.clusterStarts.length == 0) {
            return [0, 0];
        }

        const clusters: number[] = analysis.clusterStarts;

        const findClusterIndex = (chIndex: number) => {
            let lo = 0;
            let hi = clusters.length - 1;
            while (lo <= hi) {
                const mid = (lo + hi) >> 1;
                const s = clusters[mid];
                const next = clusters[mid + 1] ?? Number.MAX_SAFE_INTEGER;
                if (chIndex >= s && chIndex < next) return mid;
                if (chIndex < s) hi = mid - 1;
                else lo = mid + 1;
            }
            return Math.max(0, clusters.length - 1);
        };

        const firstCluster = findClusterIndex(firstChar);
        const lastCluster = lastChar < firstChar ? firstCluster : findClusterIndex(lastChar);

        const startCharIndex = clusters[firstCluster];
        const nextStart = clusters[lastCluster + 1];
        const lastCharIndex =
            typeof nextStart === 'number' ? nextStart - 1 : text.getNumberOfChars() - 1;

        let startPos = text.getStartPositionOfChar(startCharIndex).x;
        let endPos =
            lastCharIndex >= 0
                ? text.getEndPositionOfChar(lastCharIndex).x
                : text.getComputedTextLength();
        if (lastChar < 0) {
            // Zero-width at start
            endPos = startPos;
        } else if (lastChar < firstChar) {
            // Zero-width in middle or at end
            startPos = endPos;
        }
        return [startPos, endPos];
    } catch (e) {
        console.warn('[calculateSubstringPositionRobust] Measurement failed', e);
    }

    return null;
}

/**
 * Calculate pixel X-coordinates for a substring inside an SVG text element,
 * with special handling for BiDi and grapheme clusters.
 *
 * Behavior:
 * - If a `segAdapter` is provided (or a cached `fragment.chunk.segmenterAnalysis` exists),
 *   segmenter analysis will be used to compute grapheme-cluster-aware positions,
 *   which improves accuracy for complex scripts and mixed-direction text.
 * - If segmenter analysis is not available the function falls back to a
 *   legacy robust algorithm that uses per-character SVG measurements and
 *   applies BiDi reordering and a ligature correction factor.
 *
 * Notes:
 * - Returned coordinates are left-to-right pixel offsets relative to the
 *   start of the supplied `text` element; negate values when rendering in RTL.
 * - The function expects `fragment` to reference a `chunk` whose logical
 *   text corresponds to the measured `text` element.
 *
 * @param fragment - Fragment used for caching and to locate the containing chunk.
 * @param text - SVG text element used for measurement (subset of SVGTextContentElement).
 * @param firstChar - Logical index (inclusive) of the first character within the chunk.
 * @param lastChar - Logical index (inclusive) of the last character within the chunk.
 * @param rtlmode - Whether RTL reordering rules apply.
 * @param segAdapter - Optional `SegmenterAdapter` used to analyze grapheme clusters;
 *   when present its analysis will be cached on `fragment.chunk.segmenterAnalysis`.
 * @returns A two-element array `[startX, endX]` with pixel X-coordinates.
 */
export function calculateSubstringPositionRobust(
    fragment: Fragment,
    text: SVGTextElement,
    firstChar: number,
    lastChar: number,
    rtlmode: boolean,
    segAdapter?: SegmenterAdapter
): [number, number] {
    var result = calculateSubstringPositionSegmenter(
        fragment,
        text,
        firstChar,
        lastChar,
        segAdapter
    );
    if (result !== null) {
        return result;
    }

    return calculateSubstringPositionRobustLegacy(fragment, text, firstChar, lastChar, rtlmode);
}

/**
 * Robust substring position measurement that maps logical character indices
 * to visual glyph positions, handling BiDi reordering and mixed-direction blocks.
 *
 * Returns pixel X-coordinates `[startX, endX]` for the substring covering
 * characters `firstChar..lastChar` within the given `text` element.
 *
 * Behavior:
 * - Validates inputs and returns `[0,0]` for invalid/out-of-range indices or empty text.
 * - Uses cached metrics stored on `fragment.chunk.rtlsizes` when present:
 *   `{ charDirection, charAttrs, corrFactor }`.
 * - Otherwise, computes per-glyph widths/directions via the SVG API,
 *   optionally reorders visual blocks (for BiDi) and computes a correction
 *   factor (`corrFactor = computedTextLength / sum(widths)`) to compensate for ligatures.
 * - The returned coordinates are left-to-right pixel positions relative to
 *   the start of the text element. In RTL rendering the caller negates values.
 *
 * Notes:
 * - This algorithm expects `charAttrs` to include an entry for each glyph;
 *   incorrect reordering or missing glyphs may cause undefined behavior.
 * - The inclusion of the last character's width follows legacy logic and
 *   depends on character directions and `rtlmode`.
 *
 * @param fragment - Fragment used for caching and chunk reference.
 * @param text - SVG text element to measure (subset of SVGTextElement used).
 * @param firstChar - Logical index of the first character (inclusive).
 * @param lastChar - Logical index of the last character (inclusive).
 * @param rtlmode - Whether RTL reordering rules apply.
 * @returns A two-element array `[startX, endX]` with pixel X-coordinates.
 */
export function calculateSubstringPositionRobustLegacy(
    fragment: Fragment,
    text: SVGTextElement,
    firstChar: number,
    lastChar: number,
    rtlmode: boolean
): [number, number] {
    var numberOfChars = text.getNumberOfChars();
    if (
        numberOfChars <= 0 ||
        firstChar < 0 ||
        lastChar < 0 ||
        firstChar >= numberOfChars ||
        lastChar >= numberOfChars
    ) {
        return [0, 0];
    }

    // Legacy robust algorithm (preserved) — computes per-char widths,
    // applies a reordering of blocks and uses a correction factor for
    // ligatures. Kept as fallback for environments without ICU.

    const zeroWidth = lastChar < firstChar;
    let charDirection: Array<'rtl' | 'ltr'>;
    let charAttrs: Array<{ order: number; width: number; direction: 'rtl' | 'ltr' }>;
    let corrFactor = 1;

    if (fragment.chunk.rtlsizes) {
        // Use cached metrics
        charDirection = fragment.chunk.rtlsizes.charDirection;
        charAttrs = fragment.chunk.rtlsizes.charAttrs;
        corrFactor = fragment.chunk.rtlsizes.corrFactor;
    } else {
        // Calculate metrics
        charDirection = [];
        charAttrs = [];

        // Cannot use fragment.chunk.text.length here because invisible characters do not count.
        // Using text.getNumberOfChars() instead.
        for (let idx = 0; idx < text.getNumberOfChars(); idx++) {
            const cw = text.getEndPositionOfChar(idx).x - text.getStartPositionOfChar(idx).x;
            const dir = isRTL(text.textContent.charCodeAt(idx)) ? 'rtl' : 'ltr';
            charAttrs.push({
                order: idx,
                width: Math.abs(cw),
                direction: dir,
            });
            charDirection.push(dir);
            if (TRACE_MEASUREMENT) {
                console.trace(
                    '[calculateSubstringPositionRobust] char ' +
                        idx +
                        ' [' +
                        text.textContent[idx] +
                        '] ' +
                        'begin:' +
                        text.getStartPositionOfChar(idx).x +
                        ' end:' +
                        text.getEndPositionOfChar(idx).x +
                        ' width:' +
                        Math.abs(cw) +
                        ' dir:' +
                        charDirection[charDirection.length - 1]
                );
            }
        }

        // Re-order widths if necessary
        if (charAttrs.length > 1) {
            const idx = 0;
            let blockBegin = idx;
            let blockEnd = idx;

            // Figure out next block
            while (blockEnd < charAttrs.length) {
                while (charDirection[blockBegin] === charDirection[blockEnd]) {
                    blockEnd++;
                }

                if (charDirection[blockBegin] === (rtlmode ? 'ltr' : 'rtl')) {
                    charAttrs = charAttrs
                        .slice(0, blockBegin)
                        .concat(charAttrs.slice(blockBegin, blockEnd).reverse())
                        .concat(charAttrs.slice(blockEnd));
                }

                blockBegin = blockEnd;
            }
        }

        // The actual character width on screen is not necessarily the width that can be
        // obtained by subtracting start from end position. In particular Arabic connects
        // characters quite a bit such that the width on screen may be less. Here we
        // try to compensate for this using a correction factor.
        let widthsSum = 0;
        for (let idx = 0; idx < charAttrs.length; idx++) {
            widthsSum += charAttrs[idx].width;
        }
        corrFactor = text.getComputedTextLength() / widthsSum;
        if (TRACE_MEASUREMENT) {
            console.trace('[calculateSubstringPositionRobust] width sums: ' + widthsSum);
            console.trace(
                '[calculateSubstringPositionRobust] computed length: ' +
                    text.getComputedTextLength()
            );
            console.trace('[calculateSubstringPositionRobust] corrFactor: ' + corrFactor);
        }
        fragment.chunk.rtlsizes = {
            charDirection,
            charAttrs,
            corrFactor,
        } as any;
    }

    // startPos = Math.min(0, Math.min(text.getStartPositionOfChar(charOrder[0]).x, text.getEndPositionOfChar(charOrder[0]).x));
    let startPos = 0;
    if (TRACE_MEASUREMENT) {
        console.trace('[calculateSubstringPositionRobust] startPos[initial]: ' + startPos);
    }
    for (let i = 0; i < charAttrs.length && i < firstChar; i++) {
        // In RTL mode on RTL chars, for some reason we should not add the width of the first char.
        // But if we are in RTL mode and hit an LTR char (i.e. displaying normal LTR text in RTL mode)
        // when we need to include it... don't ask me why... REC 2021-11-27
        if (charDirection[i] === 'ltr' || charAttrs[i].order !== firstChar) {
            startPos += charAttrs[i].width;
        }
        if (TRACE_MEASUREMENT) {
            console.trace(
                '[calculateSubstringPositionRobust] startPos[' +
                    i +
                    ']  ' +
                    text.textContent[charAttrs[i].order] +
                    ' width ' +
                    charAttrs[i].width +
                    ' : ' +
                    startPos
            );
        }
    }
    startPos = startPos * corrFactor;

    // endPos = Math.min(0, Math.min(text.getStartPositionOfChar(charOrder[0]).x, text.getEndPositionOfChar(charOrder[0]).x));
    let endPos = 0;
    if (zeroWidth) {
        endPos = startPos;
    } else {
        if (TRACE_MEASUREMENT) {
            console.trace('[calculateSubstringPositionRobust] endPos[initial]: ' + endPos);
        }
        let i = 0;
        for (; i < charAttrs.length && charAttrs[i].order !== lastChar; i++) {
            endPos += charAttrs[i].width;
            if (TRACE_MEASUREMENT) {
                console.trace(
                    '[calculateSubstringPositionRobust] endPos[' +
                        i +
                        ']  ' +
                        text.textContent[charAttrs[i].order] +
                        ' width ' +
                        charAttrs[i].width +
                        ' : ' +
                        endPos
                );
            }
        }

        if (i < charDirection.length && charDirection[i] === (rtlmode ? 'rtl' : 'ltr')) {
            if (TRACE_MEASUREMENT) {
                console.trace(
                    '[calculateSubstringPositionRobust] endPos[' +
                        i +
                        ']  ' +
                        text.textContent[charAttrs[i].order] +
                        ' width ' +
                        charAttrs[i].width +
                        ' : ' +
                        endPos
                );
            }
            endPos += charAttrs[i].width;
        }
        endPos = endPos * corrFactor;
    }

    if (TRACE_MEASUREMENT) {
        console.trace('[calculateSubstringPositionRobust] start/endPos: ', [startPos, endPos]);
    }
    return [startPos, endPos];
}

/**
 * Fast fallback measurement for substring positions (LTR-optimized).
 *
 * Uses SVG text metrics (`getStartPositionOfChar`, `getEndPositionOfChar`,
 * `getComputedTextLength`) to compute the x-coordinates of a substring
 * defined by `firstChar..lastChar`. This is a fast path and does not
 * attempt to handle complex BiDi or grapheme-cluster cases — use the
 * robust RTL routine for mixed-direction text.
 *
 * @param text - The SVG text element to measure (implements the subset of SVGTextContentElement used here).
 * @param firstChar - Index of the first character (inclusive) in logical order.
 * @param lastChar - Index of the last character (inclusive) in logical order.
 * @returns A two-element array `[startX, endX]` with pixel X-coordinates for the substring.
 */
export function calculateSubstringPositionFast(
    text: SVGTextContentElement,
    firstChar: number,
    lastChar: number
) {
    if (TRACE_MEASUREMENT) {
        console.trace(
            '[calculateSubstringPositionFast] Text',
            text.textContent,
            ` - Calculating positions for char range`,
            [firstChar, lastChar]
        );
    }
    try {
        let startPos: number;
        if (firstChar < text.getNumberOfChars()) {
            startPos = text.getStartPositionOfChar(firstChar).x;
            if (TRACE_MEASUREMENT) {
                console.trace(
                    '[calculateSubstringPositionFast] Text',
                    text.textContent,
                    ` - First char index ${firstChar} maps to character "${text.textContent?.[firstChar]}" at position ${startPos}`
                );
            }
        } else {
            startPos = text.getComputedTextLength();
            if (TRACE_MEASUREMENT) {
                console.trace(
                    '[calculateSubstringPositionFast] Text',
                    text.textContent,
                    ` - First char index ${firstChar} is out of bounds (text length ${text.getNumberOfChars()}); using text length ${startPos} as start position`
                );
            }
        }

        let endPos: number;
        if (lastChar < firstChar) {
            endPos = startPos;
            if (TRACE_MEASUREMENT) {
                console.trace(
                    '[calculateSubstringPositionFast] Text',
                    text.textContent,
                    ` - Last char index ${lastChar} is less than first char index ${firstChar}; treating as zero-width range with end position equal to start position ${endPos}`
                );
            }
        } else {
            endPos = text.getEndPositionOfChar(lastChar).x;
            if (TRACE_MEASUREMENT) {
                console.trace(
                    '[calculateSubstringPositionFast] Text',
                    text.textContent,
                    ` - Last char index ${lastChar} maps to character "${text.textContent?.[lastChar]}" at position ${endPos}`
                );
            }
        }

        if (TRACE_MEASUREMENT) {
            console.trace(
                '[calculateSubstringPositionFast] Text',
                text.textContent,
                ' - Calculated substring positions:',
                [startPos, endPos]
            );
        }
        return [startPos, endPos];
    } catch (e) {
        console.error(
            `[calculateSubstringPositionFast] Unable to calculate width of range ${firstChar}-${lastChar} on [${text}]`,
            e
        );
        return [0, 0];
    }
}

/**
 * Calculate and assign the visual start X-coordinate for a marker tuple.
 *
 * This helper handles marked-text tuples of the form `[id, start?, char#, offset]`.
 * It corrects the provided character index for browser-collapsed whitespace
 * (consecutive spaces/tabs/newlines) and writes the starting X-coordinate
 * (in pixels, relative to the start of `text`) into `fragment[3]`.
 *
 * Side effects:
 * - Mutates `fragment[2]` to the adjusted character index.
 * - Sets `fragment[3]` to the computed starting X-coordinate.
 *
 * Notes:
 * - The function expects `text` to implement the relevant subset of
 *   `SVGTextContentElement` (`getStartPositionOfChar`/`getEndPositionOfChar`).
 * - This is a pure DOM-measurement helper and does not perform any
 *   dispatcher calls or logging.
 *
 * @param marker - Marker tuple `[id, start?, char#, offset]`.
 * @param text - SVG text element used for measurement.
 */
export function calculateMarkerTextElementMeasure(marker: Marker, text: SVGTextElement): void {
    if (marker[2] < 0) {
        marker[2] = 0;
    }

    // Adjust for the browser collapsing whitespace
    let lastCharSpace = true;
    let collapsedSpaces = 0;
    const tc = text.textContent || '';
    for (let i = 0; i < marker[2]; i++) {
        const c = tc[i];
        if (/\s/.test(c)) {
            if (lastCharSpace) {
                collapsedSpaces++;
            }
            lastCharSpace = true;
        } else {
            lastCharSpace = false;
        }
    }

    marker[2] -= collapsedSpaces;

    if (!marker[2]) {
        // start
        marker[3] = text.getStartPositionOfChar(marker[2]).x;
    } else {
        marker[3] = text.getEndPositionOfChar(marker[2] - 1).x + 1;
    }
}

/**
 * Calculate pixel positions and related measurements for a fragment or
 * marker inside an SVG text element.
 *
 * Behavior:
 * - If `fragment` is a `Marker` (marked-text tuple), this function
 *   adjusts for browser-collapsed whitespace and writes the start X
 *   coordinate into `fragment[3]`.
 * - If `fragment` is a `Fragment`, this function computes the left and
 *   right X pixel coordinates that correspond to the logical character
 *   range covered by the fragment and stores them on
 *   `fragment.curly = { from: number, to: number }`.
 *
 * Notes:
 * - Coordinates are left-to-right pixel offsets relative to the start
 *   of the supplied `text` element. Callers that render RTL text should
 *   negate these values when necessary (the caller/wrapper handles
 *   negation in the rendering path).
 * - This method uses SVGTextContentElement metrics
 *   (`getStartPositionOfChar`, `getEndPositionOfChar`,
 *   `getComputedTextLength`) and mutates the passed `fragment` object.
 *
 * @param fragment - Fragment object or marker tuple.
 * @param text - SVG text element used for measurements.
 * @param rtlmode - Whether RTL measurement rules apply.
 * @throws {Error} When the fragment's first character is not contained in
 *   its declared chunk (indicates inconsistent source data).
 */
export function calculateChunkTextElementMeasure(
    fragment: Fragment | Marker,
    text: SVGTextElement,
    rtlmode: boolean,
    segmenterAdapter?: SegmenterAdapter
): void {
    if (fragment instanceof Fragment) {
        calculateFragmentTextElementMeasure(fragment, text, rtlmode, segmenterAdapter);
    } else {
        // it's markedText [id, start?, char#, offset]
        calculateMarkerTextElementMeasure(fragment, text);
    }
}

/**
 * Measure rendered widths and height for a set of text strings.
 *
 * Creates a temporary SVG group, renders each key of `textsHash` as a plain
 * text node (optionally with `cssClass` applied), and computes the widths
 * for each rendered string using `getComputedTextLength`. If a `callback`
 * is provided, it will be invoked for every object in `textsHash[text]` as
 * `callback(object, svgTextNode)` so callers can associate fragments or
 * markers with the created DOM node for further per-fragment measurements.
 *
 * @param textsHash - Map from text content to an array of objects that
 *   will be passed to `callback` for that text node.
 * @param cssClass - Optional CSS class applied to the temporary group used for measurement.
 * @param callback - Optional function called as `callback(object, svgTextNode)`,
 *   where the second argument is the underlying `SVGTextContentElement`.
 * @returns A `Measurements` instance containing per-string `widths`, the
 *   computed `height`, and baseline `y` coordinate.
 */
export function getTextMeasurements(
    svg: Svg,
    textsHash: Record<string, Array<unknown>>,
    cssClass?: string,
    callback?: Function
): Measurements {
    // make some text elements, find out the dimensions
    // Ensure the temporary measurement group is attached to the provided svg root
    const textMeasureGroup: Container = svg.group().addTo(svg);
    if (cssClass) {
        textMeasureGroup.addClass(cssClass);
    }

    for (const text in textsHash) {
        if (Object.prototype.hasOwnProperty.call(textsHash, text)) {
            // create plain text nodes directly under the measure group
            textMeasureGroup.plain(text);
        }
    }

    // measuring goes on here
    const widths: Record<string, number> = {};
    for (const svgText of textMeasureGroup.children()) {
        const text = (svgText as Text).text();
        widths[text] = (svgText.node as SVGTextContentElement).getComputedTextLength();

        if (callback) {
            textsHash[text].map((object) => callback(object, svgText.node));
        }
    }

    // Add dummy element used only to get the text height even if we have no chunks
    textMeasureGroup.plain('TEXT');

    const bbox = textMeasureGroup.bbox();
    textMeasureGroup.remove();

    return new Measurements(widths, bbox.height, bbox.y);
}

function isRTL(charCode: number): boolean {
    const t1 = charCode >= 0x0591 && charCode <= 0x07ff;
    const t2 = charCode === 0x200f;
    const t3 = charCode === 0x202e;
    const t4 = charCode >= 0xfb1d && charCode <= 0xfdfd;
    const t5 = charCode >= 0xfe70 && charCode <= 0xfefc;
    return t1 || t2 || t3 || t4 || t5;
}

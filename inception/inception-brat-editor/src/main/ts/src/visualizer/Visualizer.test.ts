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

import { Visualizer } from './Visualizer';
import { Fragment } from './Fragment';
import { Entity, ENTITY } from './Entity';
import { Chunk } from './Chunk';

// Minimal mock that provides the subset of SVGTextElement the algorithm uses.
class MockText {
    textContent: string;
    private widths: number[];
    private computedOverride: number | undefined;

    constructor(textContent: string, widths: number[], computedOverride?: number) {
        this.textContent = textContent;
        this.widths = widths.slice();
        this.computedOverride = computedOverride;
    }

    getNumberOfChars() {
        return this.widths.length;
    }

    getStartPositionOfChar(idx: number) {
        return { x: this.cumWidth(idx) } as any;
    }

    getEndPositionOfChar(idx: number) {
        return { x: this.cumWidth(idx + 1) } as any;
    }

    getComputedTextLength() {
        if (this.computedOverride !== undefined) return this.computedOverride;
        return this.cumWidth(this.widths.length);
    }

    private cumWidth(n: number) {
        let s = 0;
        for (let i = 0; i < n; i++) s += this.widths[i] || 0;
        return s;
    }
}

describe('Visualizer.calculateSubstringPositionRobust', () => {
    it('computes positions for basic LTR ranges (computed path)', () => {
        const widths = [10, 10, 10, 10, 10];
        const text = new MockText('abcde', widths);

        const entity = new Entity('e1', 'T', [[0, 5]], ENTITY);
        const fragment = new Fragment(0, entity, 0, 5);
        fragment.chunk = new Chunk(0, 'abcde', 0, 5, '');

        // single char (0,0) -> width of first glyph
        let res = Visualizer.calculateSubstringPositionRobust(
            fragment as any,
            text as any,
            0,
            0,
            false
        );
        expect(res[0]).toBeCloseTo(0);
        expect(res[1]).toBeCloseTo(10);

        // mid-range (1,3) -> [10,40]
        res = Visualizer.calculateSubstringPositionRobust(fragment as any, text as any, 1, 3, false);
        expect(res[0]).toBeCloseTo(10);
        expect(res[1]).toBeCloseTo(40);

        // full chunk
        res = Visualizer.calculateSubstringPositionRobust(fragment as any, text as any, 0, 4, false);
        expect(res[0]).toBeCloseTo(0);
        expect(res[1]).toBeCloseTo(text.getComputedTextLength());
    });

    it('applies correction factor (corrFactor) from computed length', () => {
        const widths = [10, 10, 10, 10, 10];
        // Force getComputedTextLength to return 75 while widths sum to 50 -> corrFactor = 1.5
        const text = new MockText('abcde', widths, 75);

        const entity = new Entity('e2', 'T', [[0, 5]], ENTITY);
        const fragment = new Fragment(0, entity, 0, 5);
        fragment.chunk = new Chunk(0, 'abcde', 0, 5, '');

        const res = Visualizer.calculateSubstringPositionRobust(fragment as any, text as any, 1, 3, false);
        // unscaled would be [10,40] -> scaled by 1.5 -> [15,60]
        expect(res[0]).toBeCloseTo(15);
        expect(res[1]).toBeCloseTo(60);
    });

    it('produces the same result using cached rtlsizes (cached path)', () => {
        const widths = [10, 20, 30, 40, 50];
        const text = new MockText('abcde', widths);

        const entity = new Entity('e3', 'T', [[0, 5]], ENTITY);
        const fragment = new Fragment(0, entity, 0, 5);
        fragment.chunk = new Chunk(0, 'abcde', 0, 5, '');

        // First invocation computes and stores chunk.rtlsizes
        const computed = Visualizer.calculateSubstringPositionRobust(fragment as any, text as any, 1, 3, false);

        // Second invocation should reuse cached metrics and return identical numbers
        const cached = Visualizer.calculateSubstringPositionRobust(fragment as any, text as any, 1, 3, false);
        expect(cached[0]).toBeCloseTo(computed[0]);
        expect(cached[1]).toBeCloseTo(computed[1]);
    });

    it('returns [0,0] for invalid/out-of-bounds ranges', () => {
        const widths = [10, 10, 10];
        const text = new MockText('abc', widths);

        const entity = new Entity('e4', 'T', [[0, 3]], ENTITY);
        const fragment = new Fragment(0, entity, 0, 3);
        fragment.chunk = new Chunk(0, 'abc', 0, 3, '');

        // negative firstChar
        let res = Visualizer.calculateSubstringPositionRobust(fragment as any, text as any, -1, 2, true);
        expect(res).toEqual([0, 0]);

        // lastChar equal to getNumberOfChars() is out of range
        res = Visualizer.calculateSubstringPositionRobust(fragment as any, text as any, 0, 3, true);
        expect(res).toEqual([0, 0]);
    });
});

describe('Visualizer.calculateSubstringPositionFast', () => {
    it('computes positions for simple LTR cases', () => {
        const widths = [10, 10, 10, 10, 10];
        const text = new MockText('abcde', widths);

        let res = Visualizer.calculateSubstringPositionFast(text as any, 0, 0);
        expect(res[0]).toBeCloseTo(0);
        expect(res[1]).toBeCloseTo(10);

        res = Visualizer.calculateSubstringPositionFast(text as any, 1, 3);
        expect(res[0]).toBeCloseTo(10);
        expect(res[1]).toBeCloseTo(40);
    });

    it('returns zero-length when lastChar < firstChar', () => {
        const widths = [10, 10, 10];
        const text = new MockText('abc', widths);

        const res = Visualizer.calculateSubstringPositionFast(text as any, 2, 1);
        expect(res[0]).toBeCloseTo(20);
        expect(res[1]).toBeCloseTo(20);
    });

    it('handles firstChar beyond end when lastChar < firstChar', () => {
        const widths = [10, 10, 10, 10, 10];
        const text = new MockText('abcde', widths);

        const res = Visualizer.calculateSubstringPositionFast(text as any, 5, 4);
        expect(res[0]).toBeCloseTo(text.getComputedTextLength());
        expect(res[1]).toBeCloseTo(text.getComputedTextLength());
    });
});

describe('Visualizer.calculateMarkerTextElementMeasure', () => {
    it('computes start X for simple marker indices', () => {
        const widths = [10, 10, 10, 10, 10];
        const text = new MockText('abcde', widths);

        // marker tuple: [id, start?, char#, offset]
        const marker: any = ['m1', undefined, 2, 0];
        Visualizer.calculateMarkerTextElementMeasure(marker, text as any);

        // expected: end position of char 1 + 1 => cumWidth(2) + 1 = 20 + 1
        expect(marker[3]).toBeCloseTo(text.getEndPositionOfChar(1).x + 1);
    });

    it('collapses consecutive whitespace when computing index', () => {
        // text: a  b  (two spaces)
        const widths = [10, 4, 4, 10];
        const text = new MockText('a  b', widths);

        // marker points to logical char index 3 (the 'b') in the raw text
        const marker: any = ['m2', undefined, 3, 0];
        Visualizer.calculateMarkerTextElementMeasure(marker, text as any);

        // collapsed representation treats consecutive spaces as one, so the
        // effective index reduces by 1; start X should be end of first space + 1
        expect(marker[3]).toBeCloseTo(text.getEndPositionOfChar(1).x + 1);
    });

    it('normalizes negative indices to 0', () => {
        const widths = [8, 8, 8];
        const text = new MockText('abc', widths);

        const marker: any = ['m3', undefined, -1, 0];
        Visualizer.calculateMarkerTextElementMeasure(marker, text as any);

        // negative index becomes 0 -> start position of char 0
        expect(marker[2]).toBe(0);
        expect(marker[3]).toBeCloseTo(text.getStartPositionOfChar(0).x);
    });
});

describe('Visualizer.calculateFragmentTextElementMeasure', () => {
    it('computes positions for basic LTR fragment ranges', () => {
        const widths = [10, 10, 10, 10, 10];
        const text = new MockText('abcde', widths);

        const entity = new Entity('e-frag-1', 'T', [[0, 5]], ENTITY);
        const fragment = new Fragment(0, entity, 1, 4);
        fragment.chunk = new Chunk(0, 'abcde', 0, 5, '');

        Visualizer.calculateFragmentTextElementMeasure(fragment as any, text as any, false);

        expect(fragment.curly.from).toBeCloseTo(10);
        expect(fragment.curly.to).toBeCloseTo(40);
    });

    it('throws when fragment is not contained in its chunk', () => {
        const widths = [10, 10, 10];
        const text = new MockText('abc', widths);

        const entity = new Entity('e-frag-2', 'T', [[0, 3]], ENTITY);
        const fragment = new Fragment(0, entity, 0, 2);
        // make the chunk start at 1 so fragment.from - chunk.from < 0
        fragment.chunk = new Chunk(1, 'bc', 1, 3, '');

        expect(() =>
            Visualizer.calculateFragmentTextElementMeasure(fragment as any, text as any, false)
        ).toThrow(Error);
    });
});

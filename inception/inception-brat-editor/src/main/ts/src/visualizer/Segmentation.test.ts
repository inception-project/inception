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

import { tokenise, sentenceSplit } from './Segmentation';

describe('Segmentation.tokenise', () => {
    it('splits on whitespace and returns exclusive end indices', () => {
        expect(tokenise('Hello world')).toEqual([[0, 5], [6, 11]]);
    });

    it('handles leading, trailing and multiple whitespace', () => {
        expect(tokenise('  Hello   world  ')).toEqual([[2, 7], [10, 15]]);
    });

    it('treats tabs and newlines as separators', () => {
        expect(tokenise('a\tb\nc')).toEqual([[0, 1], [2, 3], [4, 5]]);
    });

    it('returns empty for empty or whitespace-only strings', () => {
        expect(tokenise('')).toEqual([]);
        expect(tokenise('   \t\n')).toEqual([]);
    });
});

describe('Segmentation.sentenceSplit', () => {
    it('splits lines on newline and returns exclusive end indices', () => {
        expect(sentenceSplit('Line1\nLine2\n')).toEqual([[0, 5], [6, 11]]);
    });

    it('returns trailing text as last sentence when no final newline', () => {
        expect(sentenceSplit('One\nTwo\nThree')).toEqual([[0, 3], [4, 7], [8, 13]]);
    });

    it('ignores blank lines and trims leading/trailing whitespace when locating sentences', () => {
        expect(sentenceSplit('  First\n\nSecond\n  ')).toEqual([[2, 7], [9, 15]]);
    });

    it('returns empty for strings with no non-whitespace characters', () => {
        expect(sentenceSplit('')).toEqual([]);
        expect(sentenceSplit('   \n  ')).toEqual([]);
    });
});

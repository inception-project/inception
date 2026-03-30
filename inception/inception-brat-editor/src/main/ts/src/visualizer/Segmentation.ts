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

import type { Offsets } from "@inception-project/inception-js-api";

/**
 * Tokenise a string into whitespace-separated tokens and return their offsets.
 *
 * This simple tokeniser treats any Unicode whitespace (matched by `/\s/`) as
 * a separator and returns an array of `Offsets` where each offset is a
 * two-element tuple `[start, end)` using an exclusive `end` index. Tokens are
 * maximal runs of non-whitespace characters; leading and trailing whitespace
 * are ignored and consecutive whitespace is collapsed.
 *
 * Example:
 *   tokenise(' Hello  world\n') -> [[1,6],[8,13]]
 *
 * @param text - Input string to split into tokens.
 * @returns Array of `[start, end)` offsets for each token found.
 */
export function tokenise(text: string): Array<Offsets> {
    const tokenOffsets: Array<Offsets> = [];
    let tokenStart: number | null = null;
    let lastCharPos: number | null = null;

    for (let i = 0; i < text.length; i++) {
        const c = text[i];
        // Have we found the start of a token?
        if (tokenStart == null && !/\s/.test(c)) {
            tokenStart = i;
            lastCharPos = i;
            // Have we found the end of a token?
        } else if (/\s/.test(c) && tokenStart != null) {
            tokenOffsets.push([tokenStart, i]);
            tokenStart = null;
            // Is it a non-whitespace character?
        } else if (!/\s/.test(c)) {
            lastCharPos = i;
        }
    }

    // Do we have a trailing token?
    if (tokenStart !== null && lastCharPos !== null) {
        tokenOffsets.push([tokenStart, lastCharPos + 1]);
    }

    return tokenOffsets;
}

/**
 * Split text into sentence-like spans using newline characters as boundaries.
 *
 * This function treats the newline character `"\n"` as a sentence boundary.
 * A sentence begins at the first non-whitespace character after a boundary
 * (or the start of the text) and ends at the position of the newline
 * (exclusive). Trailing text without a final newline is returned as the last
 * sentence. This is an intentionally simple splitter and not a linguistic
 * sentence tokenizer — use it when sentences are separated by newlines.
 *
 * Example:
 *   sentenceSplit('Line1\nLine2\n') -> [[0,5],[6,11]]
 *
 * @param text - Input string to split by newlines.
 * @returns Array of `[start, end)` offsets for each sentence found.
 */
export function sentenceSplit(text: string): Array<Offsets> {
    const sentenceOffsets: Array<Offsets> = [];
    let sentStart: number | null = null;
    let lastCharPos = -1;

    for (let i = 0; i < text.length; i++) {
        const c = text[i];
        // Have we found the start of a sentence?
        if (sentStart == null && !/\s/.test(c)) {
            sentStart = i;
            lastCharPos = i;
            // Have we found the end of a sentence?
        } else if (c === '\n' && sentStart != null) {
            sentenceOffsets.push([sentStart, i]);
            sentStart = null;
            // Is it a non-whitespace character?
        } else if (!/\s/.test(c)) {
            lastCharPos = i;
        }
    }
    // Do we have a trailing sentence without a closing newline?
    if (sentStart != null) {
        sentenceOffsets.push([sentStart, lastCharPos + 1]);
    }

    return sentenceOffsets;
}

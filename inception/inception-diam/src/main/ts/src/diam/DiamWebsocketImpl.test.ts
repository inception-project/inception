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

import { makeHeaders, buildSelectorHeader } from './DiamWebsocketImpl';

describe('makeHeaders', () => {
    it('includes empty array values as JSON string "[]"', () => {
        const selectorMap = { 'X-DIAM-EXTENSIONS': [] as string[] };
        const headers = makeHeaders(selectorMap);
        expect(headers).toHaveProperty('X-DIAM-EXTENSIONS', '[]');
    });

    it('includes non-empty array as JSON string', () => {
        const selectorMap = { 'X-DIAM-EXTENSIONS': ['spell', 'cur'] };
        const headers = makeHeaders(selectorMap);
        expect(headers['X-DIAM-EXTENSIONS']).toBe(JSON.stringify(['cur', 'spell']));
    });
});

describe('buildSelectorHeader', () => {
    it('returns equality selector for empty arrays', () => {
        const selectorMap = { 'X-DIAM-EXTENSIONS': [] as string[] };
        const selector = buildSelectorHeader(selectorMap);
        expect(selector).toBe("headers['X-DIAM-EXTENSIONS'] == '[]'");
    });

    it('returns equality selector for non-empty arrays (sorted)', () => {
        const selectorMap = { 'X-DIAM-EXTENSIONS': ['b', 'a'] };
        const selector = buildSelectorHeader(selectorMap);
        expect(selector).toBe("headers['X-DIAM-EXTENSIONS'] == '[\"a\",\"b\"]'");
    });

    it('returns undefined for empty map', () => {
        const selector = buildSelectorHeader({});
        expect(selector).toBeUndefined();
    });

    it('builds combined selector for format and extensions', () => {
        const selector = buildSelectorHeader({ 'X-DIAM-FORMAT': 'compact', 'X-DIAM-EXTENSIONS': [] });
        expect(selector).toBe("headers['X-DIAM-FORMAT'] == 'compact' and headers['X-DIAM-EXTENSIONS'] == '[]'");
    });
});

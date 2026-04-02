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

import { findClosestHorizontalScrollable, findClosestVerticalScrollable } from './DomUtils';

describe('findClosestHorizontalScrollable', () => {
    it('returns the closest ancestor with overflowX:auto and scrollWidth>clientWidth', () => {
        const container = document.createElement('div');
        const child = document.createElement('div');
        container.appendChild(child);

        // Simulate overflowing content
        Object.defineProperty(container, 'scrollWidth', { value: 200, configurable: true });
        Object.defineProperty(container, 'clientWidth', { value: 100, configurable: true });
        container.style.overflowX = 'auto';

        expect(findClosestHorizontalScrollable(child)).toBe(container);
    });

    it('returns the ancestor when overflowX is scroll', () => {
        const container = document.createElement('div');
        const child = document.createElement('div');
        container.appendChild(child);
        container.style.overflowX = 'scroll';
        Object.defineProperty(container, 'scrollWidth', { value: 0, configurable: true });
        Object.defineProperty(container, 'clientWidth', { value: 0, configurable: true });

        expect(findClosestHorizontalScrollable(child)).toBe(container);
    });

    it('aborts and returns null when encountering an intermediate element with class "scrollable"', () => {
        const outer = document.createElement('div');
        const mid = document.createElement('div');
        const inner = document.createElement('div');
        outer.appendChild(mid);
        mid.appendChild(inner);

        // Outer is actually scrollable, mid is marked as "scrollable" but not itself scrollable
        Object.defineProperty(outer, 'scrollWidth', { value: 300, configurable: true });
        Object.defineProperty(outer, 'clientWidth', { value: 100, configurable: true });
        outer.style.overflowX = 'auto';
        mid.classList.add('scrollable');

        expect(findClosestHorizontalScrollable(inner)).toBeNull();
    });

    it('returns null for null input and for starting at HTML element', () => {
        expect(findClosestHorizontalScrollable(null)).toBeNull();
        expect(findClosestHorizontalScrollable(document.documentElement)).toBeNull();
    });
});

describe('findClosestVerticalScrollable', () => {
    it('returns the closest ancestor with overflowY:auto and scrollHeight>clientHeight', () => {
        const container = document.createElement('div');
        const child = document.createElement('div');
        container.appendChild(child);

        Object.defineProperty(container, 'scrollHeight', { value: 200, configurable: true });
        Object.defineProperty(container, 'clientHeight', { value: 100, configurable: true });
        container.style.overflowY = 'auto';

        expect(findClosestVerticalScrollable(child)).toBe(container);
    });

    it('returns the ancestor when overflowY is scroll', () => {
        const container = document.createElement('div');
        const child = document.createElement('div');
        container.appendChild(child);
        container.style.overflowY = 'scroll';
        Object.defineProperty(container, 'scrollHeight', { value: 0, configurable: true });
        Object.defineProperty(container, 'clientHeight', { value: 0, configurable: true });

        expect(findClosestVerticalScrollable(child)).toBe(container);
    });

    it('aborts and returns null when encountering an intermediate element with class "scrollable"', () => {
        const outer = document.createElement('div');
        const mid = document.createElement('div');
        const inner = document.createElement('div');
        outer.appendChild(mid);
        mid.appendChild(inner);

        Object.defineProperty(outer, 'scrollHeight', { value: 300, configurable: true });
        Object.defineProperty(outer, 'clientHeight', { value: 100, configurable: true });
        outer.style.overflowY = 'auto';
        mid.classList.add('scrollable');

        expect(findClosestVerticalScrollable(inner)).toBeNull();
    });

    it('returns null for null input and for starting at HTML element', () => {
        expect(findClosestVerticalScrollable(null)).toBeNull();
        expect(findClosestVerticalScrollable(document.documentElement)).toBeNull();
    });
});

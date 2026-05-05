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
import { beforeEach, afterEach, describe, it, expect, vi } from 'vitest';
import { ViewportTracker, TRACKING_CLASS } from './ViewportTracker';

// Minimal mock for IntersectionObserver used to simulate intersection events.
class MockIntersectionObserver {
    static lastInstance: MockIntersectionObserver | null = null;
    cb: IntersectionObserverCallback;
    observed: Element[] = [];

    constructor(cb: IntersectionObserverCallback) {
        this.cb = cb;
        MockIntersectionObserver.lastInstance = this;
    }

    observe(el: Element) {
        this.observed.push(el);
    }

    unobserve(el: Element) {
        this.observed = this.observed.filter((e) => e !== el);
    }

    disconnect() {
        this.observed = [];
    }

    // Simulate entries being observed
    simulate(entries: Partial<IntersectionObserverEntry>[] = []) {
        // Cast to the right type for callback invocation
        const full = entries.map((e) => ({ isIntersecting: !!e.isIntersecting, target: e.target } as IntersectionObserverEntry));
        this.cb(full, this as unknown as IntersectionObserver);
    }
}

describe('ViewportTracker (pause/resume)', () => {
    let origIO: any;

    beforeEach(() => {
        origIO = (global as any).IntersectionObserver;
        (global as any).IntersectionObserver = MockIntersectionObserver;
        vi.useFakeTimers();
    });

    afterEach(() => {
        (global as any).IntersectionObserver = origIO;
        vi.useRealTimers();
    });

    it('calls callback when entries change and respects debounce', () => {
        const container = document.createElement('div');
        const child = document.createElement('div');
        child.style.display = 'block';
        child.textContent = 'hello';
        container.appendChild(child);
        document.body.appendChild(container);

        const cb = vi.fn();
        const tracker = new ViewportTracker(container, cb);

        const obs = MockIntersectionObserver.lastInstance;
        expect(obs).toBeTruthy();

        // Simulate an intersecting entry
        obs!.simulate([{ isIntersecting: true, target: child }]);

        // advance past the debounce delay (250ms)
        vi.advanceTimersByTime(300);

        expect(cb).toHaveBeenCalled();

        tracker.disconnect();
    });

    it('suppresses callbacks while paused and resumes after resume', () => {
        const container = document.createElement('div');
        const child = document.createElement('div');
        child.style.display = 'block';
        child.textContent = 'world';
        container.appendChild(child);
        document.body.appendChild(container);

        const cb = vi.fn();
        const tracker = new ViewportTracker(container, cb);
        const obs1 = MockIntersectionObserver.lastInstance;
        expect(obs1).toBeTruthy();

        // Pause tracking
        tracker.pause();

        // Simulate intersection while paused
        obs1!.simulate([{ isIntersecting: true, target: child }]);
        vi.advanceTimersByTime(300);
        expect(cb).not.toHaveBeenCalled();

        // Resume tracking
        tracker.resume();
        const obs2 = MockIntersectionObserver.lastInstance;
        expect(obs2).toBeTruthy();

        // Simulate after resume
        obs2!.simulate([{ isIntersecting: true, target: child }]);
        vi.advanceTimersByTime(300);
        expect(cb).toHaveBeenCalled();

        tracker.disconnect();
    });

    it('respects ignoreSelector and TRACKING_CLASS selection', () => {
        const container = document.createElement('div');
        const ignored = document.createElement('div');
        ignored.classList.add('ignore');
        ignored.style.display = 'block';
        ignored.textContent = 'ignore me';

        const tracked = document.createElement('div');
        tracked.style.display = 'block';
        tracked.textContent = 'track me';

        // Force tracking by adding the tracking class to another element
        const forced = document.createElement('div');
        forced.classList.add(TRACKING_CLASS);
        forced.style.display = 'block';
        forced.textContent = 'forced';

        container.appendChild(ignored);
        container.appendChild(tracked);
        container.appendChild(forced);
        document.body.appendChild(container);

        const cb = vi.fn();
        // Use ignoreSelector to filter out the ignored element
        const tracker = new ViewportTracker(container, cb, { ignoreSelector: '.ignore' });
        const obs = MockIntersectionObserver.lastInstance;
        expect(obs).toBeTruthy();

        // The observed elements should include 'tracked' and 'forced' but not 'ignored'
        expect(obs!.observed).toContain(tracked);
        expect(obs!.observed).toContain(forced);
        expect(obs!.observed).not.toContain(ignored);

        tracker.disconnect();
    });

    it('calculates window range correctly for visible elements', () => {
        const container = document.createElement('div');
        const a = document.createElement('div');
        a.style.display = 'block';
        a.textContent = 'aaa';
        const b = document.createElement('div');
        b.style.display = 'block';
        b.textContent = 'bbbb';
        container.appendChild(a);
        container.appendChild(b);
        document.body.appendChild(container);

        const cb = vi.fn();
        const tracker = new ViewportTracker(container, cb);
        const obs = MockIntersectionObserver.lastInstance;
        expect(obs).toBeTruthy();

        obs!.simulate([{ isIntersecting: true, target: a }, { isIntersecting: true, target: b }]);
        vi.advanceTimersByTime(300);

        expect(cb).toHaveBeenCalled();
        const calledWith = cb.mock.calls[0][0] as [number, number];
        const expectedEnd = (a.textContent + b.textContent).length;
        expect(calledWith[0]).toBe(0);
        expect(calledWith[1]).toBe(expectedEnd);

        tracker.disconnect();
    });

    it('prunes stale visibleElements after DOM replacement on re-initialization', () => {
        const container = document.createElement('div');
        const childA = document.createElement('div');
        childA.style.display = 'block';
        childA.textContent = 'A';
        container.appendChild(childA);
        document.body.appendChild(container);

        const cb = vi.fn();
        const tracker = new ViewportTracker(container, cb);
        const obs1 = MockIntersectionObserver.lastInstance!;

        // Mark childA visible
        obs1.simulate([{ isIntersecting: true, target: childA }]);
        vi.advanceTimersByTime(300);
        expect(tracker.visibleElements.has(childA)).toBe(true);

        // Replace childA with childB to mimic a render replacing DOM nodes
        container.removeChild(childA);
        const childB = document.createElement('div');
        childB.style.display = 'block';
        childB.textContent = 'B';
        container.appendChild(childB);

        // Trigger re-initialization via pause/resume
        tracker.pause();
        tracker.resume();

        // Stale ref to detached childA must be dropped
        expect(tracker.visibleElements.has(childA)).toBe(false);

        // The new element must now be observed
        const obs2 = MockIntersectionObserver.lastInstance!;
        expect(obs2.observed).toContain(childB);
        expect(obs2.observed).not.toContain(childA);

        tracker.disconnect();
    });

    it('does not schedule another callback when entries change but range stays the same', () => {
        const container = document.createElement('div');
        const child = document.createElement('div');
        child.style.display = 'block';
        child.textContent = 'abc';
        container.appendChild(child);
        document.body.appendChild(container);

        const cb = vi.fn();
        const tracker = new ViewportTracker(container, cb);
        const obs = MockIntersectionObserver.lastInstance!;

        // First intersection — initial callback fires regardless of range change
        obs.simulate([{ isIntersecting: true, target: child }]);
        vi.advanceTimersByTime(300);
        expect(cb).toHaveBeenCalledTimes(1);

        // Element leaves the viewport: no add → no callback scheduled
        obs.simulate([{ isIntersecting: false, target: child }]);
        vi.advanceTimersByTime(300);
        expect(cb).toHaveBeenCalledTimes(1);

        // Element re-enters: this counts as an add, but the computed range is identical
        // to the current range, so no additional callback should be scheduled.
        obs.simulate([{ isIntersecting: true, target: child }]);
        vi.advanceTimersByTime(300);
        expect(cb).toHaveBeenCalledTimes(1);

        tracker.disconnect();
    });

    it('forceRefresh does nothing while paused and resume triggers immediate callback', () => {
        const container = document.createElement('div');
        const child = document.createElement('div');
        child.style.display = 'block';
        child.textContent = 'x';
        container.appendChild(child);
        document.body.appendChild(container);

        const cb = vi.fn();
        const tracker = new ViewportTracker(container, cb);

        tracker.pause();
        tracker.forceRefresh();
        expect(cb).not.toHaveBeenCalled();

        tracker.resume();
        expect(cb).toHaveBeenCalled();

        tracker.disconnect();
    });
});

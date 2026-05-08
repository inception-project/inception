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
import { calculateEndOffset, calculateStartOffset } from './OffsetUtils';

export type ViewportTrackerCallback = (range: [number, number]) => void;

export type ViewportTrackerOptions = {
    sectionSelector?: string;
    ignoreSelector?: string;
};

export const NO_TRACKING_CLASS = 'data-i7n-no-tracking';
export const TRACKING_CLASS = 'data-i7n-tracking';

export class ViewportTracker {
    private _visibleElements = new Set<Element>();
    private _currentRange: [number, number] = [0, 0];

    private root: Element;
    private observer?: IntersectionObserver;

    private initialized = false;
    private redrawTimeoutId?: ReturnType<typeof setTimeout>;
    private debounceDelay = 250;
    private callback: ViewportTrackerCallback;

    private options?: ViewportTrackerOptions;
    private paused = false;
    private pauseCount = 0;
    private trackedElements: Element[] = [];

    /**
     * @param element the element containing the elemnts to track
     * @param callback the callback to invoke when the visible elements change
     * @param options optional CSS selector indicating which elements are considered sections
     * and should be tracked as a whole or an options object
     */
    public constructor(
        element: Element,
        callback: ViewportTrackerCallback,
        options?: string | ViewportTrackerOptions,
        ignoreClasses?: string[]
    ) {
        this.root = element;
        this.callback = callback;
        if (typeof options === 'string') {
            this.options = { sectionSelector: options };
        } else {
            this.options = options;
        }

        this.initializeElementTracking(this.root);
    }

    /**
     * Pause observation and prevent callback scheduling. Observers are disconnected.
     */
    public pause(): void {
        // Support nested pause calls by counting them. Only perform the actual
        // disconnect on the transition from 0 -> 1.
        this.pauseCount++;
        if (this.pauseCount > 1) {
            this.paused = true;
            console.debug(`ViewportTracker pause count increased to ${this.pauseCount}; already paused`);
            return;
        }

        console.debug('Pausing ViewportTracker');

        this.paused = true;
        if (this.observer) {
            try {
                this.observer.disconnect();
            } catch (e) {
                // ignore
            }
        }
        // stop any pending callback
        clearTimeout(this.redrawTimeoutId);
        this.redrawTimeoutId = undefined;
    }

    /**
     * Resume observation. Reinitializes tracking and triggers an immediate refresh.
     *
     * @param options optional settings: { waitFrames?: number } delays the actual
     * resume by the given number of requestAnimationFrame ticks. Useful to allow
     * the document layout to stabilise after large DOM writes.
     */
    public resume(options?: { waitFrames?: number; suppressInitialRefresh?: boolean }): void {
        // If there are nested pauses, decrement the counter and only resume when
        // the counter reaches zero.
        if (this.pauseCount > 0) {
            this.pauseCount--;
            if (this.pauseCount > 0) {
                this.paused = true;
                console.debug(`ViewportTracker pause count decreased to ${this.pauseCount}; still paused`);
                return;
            }
        }

        if (!this.paused) return;

        console.debug('Resuming ViewportTracker with options', options);

        const waitFrames = options?.waitFrames ?? 0;
        const suppressInitial = options?.suppressInitialRefresh ?? false;

        const doResume = () => {
            // If already resumed meanwhile, do nothing
            if (!this.paused) return;
            this.paused = false;
            // Reinitialize tracking (recreates observer and observations)
            this.initializeElementTracking(this.root);
            // Immediately refresh range so caller can react right away unless
            // explicitly suppressed (useful when resume is called after a
            // rendering pass that already has up-to-date annotations).
            if (!suppressInitial) {
                this.forceRefresh();
            }
        };

        if (waitFrames <= 0) {
            doResume();
            return;
        }

        // Use requestAnimationFrame if available, fall back to setTimeout for
        // non-browser environments (tests, SSR).
        const raf: (cb: FrameRequestCallback) => number =
            typeof (globalThis as any).requestAnimationFrame === 'function'
                ? (globalThis as any).requestAnimationFrame.bind(globalThis)
                : (cb: FrameRequestCallback) => {
                      return (setTimeout(() => cb(Date.now()), 16) as unknown) as number;
                  };

        let frames = 0;
        const tick = () => {
            frames++;
            if (frames >= waitFrames) {
                doResume();
            } else {
                raf(tick as FrameRequestCallback);
            }
        };

        raf(tick as FrameRequestCallback);
    }

    /**
     * Immediately recalculate the visible range and invoke the callback.
     */
    public forceRefresh(): void {
        // If paused, do not call the callback.
        if (this.paused) {
            return;
        }
        clearTimeout(this.redrawTimeoutId);
        this.redrawTimeoutId = undefined;
        const newRange = this.calculateWindowRange();

        // If we have already been initialized and the range did not change,
        // there is no need to invoke the callback again — this avoids
        // redundant loads when resuming the tracker after a rendering pass.
        if (
            this.initialized &&
            this._currentRange[0] === newRange[0] &&
            this._currentRange[1] === newRange[1]
        ) {
            return;
        }

        this._currentRange = newRange;
        try {
            this.callback(this._currentRange);
        } catch (err) {
            // Swallow callback errors to avoid breaking the tracker
            console.error('ViewportTracker callback error', err);
        }
    }

    public disconnect() {
        if (this.observer) {
            this.observer.disconnect();
        }
        this._visibleElements.clear();
        this._currentRange = [0, 0];
        this.initialized = false;
        clearTimeout(this.redrawTimeoutId);
        this.redrawTimeoutId = undefined;
    }

    public get currentRange(): [number, number] {
        return this._currentRange;
    }

    public get visibleElements(): Set<Element> {
        return this._visibleElements;
    }

    private shouldTrack(element: Element): boolean {
        if (element.classList.contains(TRACKING_CLASS)) {
            return true;
        }

        if (this.options?.ignoreSelector && element.matches(this.options.ignoreSelector)) {
            return false;
        }

        const style = getComputedStyle(element);
        if (!style.display) {
            return false;
        }

        if (!element.textContent) {
            return false;
        }

        return (
            style.display === 'block' ||
            style.display === 'flex' ||
            style.display === 'grid' ||
            style.display === 'table-row' ||
            style.display === 'list-item'
        );

        // return !style.display.startsWith('inline') && !style.display.includes('math')
    }

    private initializeElementTracking(element: Element): void {
        const startTime = new Date().getTime();
        if (this.observer) {
            this.observer.disconnect();
        }

        let leafTrackingCandidates: Set<Element>;
        let trackingCandidates = Array.from(element.querySelectorAll('*')).filter((e) =>
            this.shouldTrack(e)
        );
        console.debug(`Found ${trackingCandidates.length} tracking candidates`);

        // const displayStyles = new Set<string>()
        // trackingCandidates.map(e => getComputedStyle(e).display).forEach(e => displayStyles.add(e))
        // console.debug('Display styles found: ', displayStyles)

        if (trackingCandidates.length > 0) {
            trackingCandidates = trackingCandidates.map((e) => {
                if (!this.options?.sectionSelector || e.matches(this.options.sectionSelector)) {
                    return e;
                }

                return e.closest(this.options.sectionSelector) || e;
            });
            leafTrackingCandidates = new Set<Element>([...trackingCandidates]);
            trackingCandidates.map(
                (e) => e.parentElement && leafTrackingCandidates.delete(e.parentElement)
            );
        } else {
            leafTrackingCandidates = new Set<Element>([element]);
        }
        console.debug(`Found ${leafTrackingCandidates.size} leaf tracking elements`);

        const options = {
            root: element.ownerDocument.body,
            rootMargin: '0px',
            threshold: 0.0,
        };

        this.trackedElements = Array.from(leafTrackingCandidates);

        // Drop any element refs that are no longer tracked. Without this, the set grows
        // unbounded as renders replace DOM nodes, and stale refs prevent the "added" count
        // from reflecting reality. Prune in place so external references obtained via the
        // public getter continue to observe updates.
        this._visibleElements.forEach((e) => {
            if (!leafTrackingCandidates.has(e)) {
                this._visibleElements.delete(e)
            }
        })

        // If paused we do not create an observer now
        if (this.paused) {
            console.debug('ViewportTracker is paused; skipping observer creation');
            return;
        }

        this.observer = new IntersectionObserver((e, o) => this.handleIntersectRange(e, o), options);
        leafTrackingCandidates.forEach((e) => this.observer!.observe(e));

        const endTime = new Date().getTime();
        console.debug(
            `Tracking visibility of ${leafTrackingCandidates.size} elements in ${Math.abs(endTime - startTime)}ms`
        );
    }

    private handleIntersectRange(
        entries: IntersectionObserverEntry[],
        observer: IntersectionObserver
    ): void {
        if (this.paused) {
            // Ignore any entries while paused
            return;
        }
        // Avoid triggering the callback if no new elements have become visible
        let visibleElementsAdded = 0;

        entries.forEach((entry) => {
            if (entry.isIntersecting) {
                const sizeBefore = this._visibleElements.size;
                this._visibleElements.add(entry.target);
                if (sizeBefore < this._visibleElements.size) visibleElementsAdded++;
            } else {
                this._visibleElements.delete(entry.target);
            }
        });

        if (visibleElementsAdded || !this.initialized) {
            console.debug(
                `Visible elements changed: ${visibleElementsAdded} added, ${this._visibleElements.size} visible elements in total`
            );
            const newRange = this.calculateWindowRange();
            const rangeChanged =
                newRange[0] !== this._currentRange[0] || newRange[1] !== this._currentRange[1];
            this._currentRange = newRange;
            // The first time the callback is called, we want to make sure that the annotations are
            // loaded at least once. Subsequent fires only invoke the callback if the visible range
            // actually changed - otherwise re-observed elements after a render pass can spuriously
            // re-trigger a load even though there is nothing new to fetch.
            if (!this.initialized || rangeChanged) {
                this.initialized = true;
                this.initiateAction();
            }
        }
    }

    private initiateAction(): void {
        clearTimeout(this.redrawTimeoutId);
        this.redrawTimeoutId = setTimeout(() => {
            this._currentRange = this.calculateWindowRange();
            console.debug('Invoking ViewportTracker callback with range', this._currentRange);
            this.callback(this._currentRange);
        }, this.debounceDelay);
    }

    private calculateWindowRange(): [number, number] {
        const startTime = new Date().getTime();
        let begin = Number.MAX_SAFE_INTEGER;
        let end = Number.MIN_SAFE_INTEGER;
        this._visibleElements.forEach((el) => {
            begin = Math.min(begin, calculateStartOffset(this.root, el));
            end = Math.max(end, calculateEndOffset(this.root, el));
        });
        const endTime = new Date().getTime();

        console.debug(
            `Visible: ${begin}-${end} (${this._visibleElements.size} visible elements, ${Math.abs(endTime - startTime)}ms)`
        );
        return [begin, end];
    }
}

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
import { calculateEndOffset, calculateStartOffset } from './OffsetUtils'

export type ViewportTrackerCallback = (range: [number, number]) => void

export type ViewportTrackerOptions = {
  sectionSelector?: string,
  ignoreSelector?: string
}

export const NO_TRACKING_CLASS = 'data-i7n-no-tracking'
export const TRACKING_CLASS = 'data-i7n-tracking'

export class ViewportTracker {
  private _visibleElements = new Set<Element>()
  private _currentRange: [number, number] = [0, 0]

  private root: Element
  private observer: IntersectionObserver

  private initialized = false
  private redrawTimeoutId: ReturnType<typeof setTimeout>
  private debounceDelay = 250
  private callback: ViewportTrackerCallback

  private options?: ViewportTrackerOptions

  /**
   * @param element the element containing the elemnts to track
   * @param callback the callback to invoke when the visible elements change
   * @param options optional CSS selector indicating which elements are considered sections
   * and should be tracked as a whole or an options object
   */
  public constructor (element: Element, callback: ViewportTrackerCallback, options?: string | ViewportTrackerOptions, ignoreClasses?: string[]) {
    this.root = element
    this.callback = callback
    if (typeof options === 'string') {
      this.options = { sectionSelector: options }
    }
    else {
      this.options = options;
    }

    this.initializeElementTracking(this.root)
  }

  public disconnect() {
    if (this.observer) {
      this.observer.disconnect()
    }
    this._visibleElements.clear()
    this._currentRange = [0, 0]
    this.initialized = false
    clearTimeout(this.redrawTimeoutId)
    this.redrawTimeoutId = undefined
  }

  public get currentRange (): [number, number] {
    return this._currentRange
  }

  public get visibleElements (): Set<Element> {
    return this._visibleElements
  }

  private shouldTrack (element: Element): boolean {
    if (element.classList.contains(TRACKING_CLASS)) {
      return true
    }

    if (this.options?.ignoreSelector && element.matches(this.options.ignoreSelector)) {
      return false
    }

    const style = getComputedStyle(element)
    if (!style.display) {
      return false
    }

    if (!element.textContent) {
      return false;
    }

    return style.display === 'block' || style.display === 'flex' || style.display === 'grid' ||
      style.display === 'table-row' || style.display === 'list-item'

    // return !style.display.startsWith('inline') && !style.display.includes('math')
  }

  private initializeElementTracking (element: Element): void {
    const startTime = new Date().getTime()
    if (this.observer) {
      this.observer.disconnect()
    }

    let leafTrackingCandidates: Set<Element>
    let trackingCandidates = Array.from(element.querySelectorAll('*'))
      .filter(e => this.shouldTrack(e))
    console.debug(`Found ${trackingCandidates.length} tracking candidates`)

    // const displayStyles = new Set<string>()
    // trackingCandidates.map(e => getComputedStyle(e).display).forEach(e => displayStyles.add(e))
    // console.debug('Display styles found: ', displayStyles)

    if (trackingCandidates.length > 0) {
      trackingCandidates = trackingCandidates.map(e => {
        if (!this.options?.sectionSelector || e.matches(this.options.sectionSelector)) {
          return e
        }

        return e.closest(this.options.sectionSelector) || e
      })
      leafTrackingCandidates = new Set<Element>([...trackingCandidates])
      trackingCandidates.map(e => e.parentElement && leafTrackingCandidates.delete(e.parentElement))
    } else {
      leafTrackingCandidates = new Set<Element>([element])
    }
    console.debug(`Found ${leafTrackingCandidates.size} leaf tracking elements`)

    const options = {
      root: element.ownerDocument.body,
      rootMargin: '0px',
      threshold: 0.0
    }

    this.observer = new IntersectionObserver((e, o) => this.handleIntersectRange(e, o), options)

    leafTrackingCandidates.forEach(e => this.observer.observe(e))

    const endTime = new Date().getTime()
    console.debug(`Tracking visibility of ${leafTrackingCandidates.size} elements in ${Math.abs(endTime - startTime)}ms`)
  }

  private handleIntersectRange (entries: IntersectionObserverEntry[], observer: IntersectionObserver): void {
    // Avoid triggering the callback if no new elements have become visible
    let visibleElementsAdded = 0

    entries.forEach(entry => {
      if (entry.isIntersecting) {
        const sizeBefore = this._visibleElements.size
        this._visibleElements.add(entry.target)
        if (sizeBefore < this._visibleElements.size) visibleElementsAdded++
      } else {
        this._visibleElements.delete(entry.target)
      }
    })

    if (visibleElementsAdded || !this.initialized) {
      console.debug(`Visible elements changed: ${visibleElementsAdded} added, ${this._visibleElements.size} visible elements in total`)
      // the first time the callback is called, we want to make sure that the annotations are
      // loaded at least once
      this.initialized = true
      this.initiateAction()
    }
  }

  private initiateAction (): void {
    clearTimeout(this.redrawTimeoutId)
    this.redrawTimeoutId = setTimeout(() => {
      this._currentRange = this.calculateWindowRange()
      this.callback(this._currentRange)
    }, this.debounceDelay)
  }

  private calculateWindowRange (): [number, number] {
    const startTime = new Date().getTime()
    let begin = Number.MAX_SAFE_INTEGER
    let end = Number.MIN_SAFE_INTEGER
    this._visibleElements.forEach(el => {
      begin = Math.min(begin, calculateStartOffset(this.root, el))
      end = Math.max(end, calculateEndOffset(this.root, el))
    })
    const endTime = new Date().getTime()

    console.debug(`Visible: ${begin}-${end} (${this._visibleElements.size} visible elements, ${Math.abs(endTime - startTime)}ms)`)
    return [begin, end]
  }
}

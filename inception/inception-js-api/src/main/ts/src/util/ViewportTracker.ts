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
import { isConstructorDeclaration, textChangeRangeIsUnchanged } from 'typescript'
import { calculateEndOffset, calculateStartOffset } from './OffsetUtils'

export type ViewportTrackerCallback = (range: [number, number]) => void

export class ViewportTracker {
  private _visibleElements = new Set<Element>()
  private _currentRange: [number, number] = [0, 0]

  private root: Element
  private observer: IntersectionObserver

  private initialized = false
  private redrawTimeoutId: ReturnType<typeof setTimeout>
  private debounceDelay = 250
  private callback: ViewportTrackerCallback

  private sectionSelector?: string

  /**
   * @param element the element containing the elemnts to track
   * @param callback the callback to invoke when the visible elements change
   * @param sectionSelector optional CSS selector indicating which elements are considered sections
   * and should be tracked as a whole
   */
  public constructor(element: Element, callback: ViewportTrackerCallback, sectionSelector?: string) {
    this.root = element
    this.callback = callback
    this.sectionSelector = sectionSelector

    this.initializeElementTracking(this.root)
  }

  public get currentRange(): [number, number] {
    return this._currentRange
  }

  public get visibleElements(): Set<Element> {
    return this.visibleElements;
  }

  private shouldTrack(element: Element): boolean {
    const style = getComputedStyle(element)
    if (!style.display) {
      return false
    }
    return !style.display.startsWith('inline')
  }

  private initializeElementTracking(element: Element): void {
    const startTime = new Date().getTime()
    if (this.observer) {
      this.observer.disconnect()
    }

    let leafTrackingCandidates: Set<Element>
    let trackingCandidates = Array.from(element.querySelectorAll('*'))
      .filter(e => this.shouldTrack(e))
    console.debug(`Found ${trackingCandidates.length} tracking candidates`)

    if (trackingCandidates.length > 0) {
      trackingCandidates = trackingCandidates.map(e => {
        if (!this.sectionSelector || e.matches(this.sectionSelector)) {
          return e
        }

        return e.closest(this.sectionSelector) || e
      })
      leafTrackingCandidates = new Set<Element>([...trackingCandidates])
      trackingCandidates.map(e => e.parentElement && leafTrackingCandidates.delete(e.parentElement))
    } else {
      leafTrackingCandidates = new Set<Element>([element])
    }
    console.debug(`Found ${leafTrackingCandidates.size} leaf tracking elements`)

    const options = {
      root: element.ownerDocument,
      rootMargin: '0px',
      threshold: 0.0
    }

    this.observer = new IntersectionObserver((e, o) => this.handleIntersectRange(e, o), options)

    leafTrackingCandidates.forEach(e => this.observer.observe(e))

    const endTime = new Date().getTime()
    console.log(`Tracking visibility of ${leafTrackingCandidates.size} elements in ${Math.abs(endTime - startTime)}ms`)
  }

  private handleIntersectRange(entries: IntersectionObserverEntry[], observer: IntersectionObserver): void {
    // Avoid triggering the callback if no new elements have become visible
    let visibleElementsAdded = 0

    entries.forEach(entry => {
      if (entry.isIntersecting) {
        this._visibleElements.add(entry.target)
        visibleElementsAdded++
      } else {
        this._visibleElements.delete(entry.target)
      }
    })

    if (visibleElementsAdded || !this.initialized) {
      console.log(`Visible elements changed: ${visibleElementsAdded} added, ${this._visibleElements.size} visible elements in total`)
      // the first time the callback is called, we want to make sure that the annotations are 
      // loaded at least once
      this.initialized = true
      this.initiateAction()
    }
  }

  private initiateAction(): void {
    clearTimeout(this.redrawTimeoutId)
    this.redrawTimeoutId = setTimeout(() => {
      this._currentRange = this.calculateWindowRange()
      this.callback(this._currentRange)
    }, this.debounceDelay)
  }

  private calculateWindowRange(): [number, number] {
    const startTime = new Date().getTime()
    let begin = Number.MAX_SAFE_INTEGER
    let end = Number.MIN_SAFE_INTEGER
    this._visibleElements.forEach(el => {
      begin = Math.min(begin, calculateStartOffset(this.root, el))
      end = Math.max(end, calculateEndOffset(this.root, el))
    })
    const endTime = new Date().getTime()

    console.log(`Visible: ${begin}-${end} (${this._visibleElements.size} visible elements, ${Math.abs(endTime - startTime)}ms)`)
    return [begin, end]
  }
}

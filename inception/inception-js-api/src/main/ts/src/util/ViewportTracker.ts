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

export class ViewportTracker {
  private root: Element
  private visibleElements = new Set<Element>()
  private observer: IntersectionObserver
  private redrawTimeoutId: ReturnType<typeof setTimeout>
  private debounceDelay = 250
  private callback: ViewportTrackerCallback
  private _currentRange: [number, number] = [0, 0]

  public constructor (element: Element, callback: ViewportTrackerCallback) {
    this.root = element
    this.callback = callback

    this.initializeElementTracking(this.root)
  }

  public get currentRange (): [number, number] {
    return this._currentRange
  }

  private shouldTrack (element: Element): boolean {
    const style = getComputedStyle(element)
    if (!style.display) {
      return false
    }
    return !style.display.startsWith('inline')
  }

  private initializeElementTracking (element: Element): void {
    const startTime = new Date().getTime()
    if (this.observer) {
      this.observer.disconnect()
    }

    let leafTrackingCandidates: Set<Element>
    const trackingCandidates = Array.from(element.querySelectorAll('*'))
      .filter(e => this.shouldTrack(e))
    console.debug(`Found ${trackingCandidates.length} tracking candidates`)

    if (trackingCandidates.length > 0) {
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

  private handleIntersectRange (entries: IntersectionObserverEntry[], observer: IntersectionObserver): void {
    entries.forEach(entry => {
      if (entry.isIntersecting) {
        this.visibleElements.add(entry.target)
      } else {
        this.visibleElements.delete(entry.target)
      }
    })

    this.initiateAction()
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
    this.visibleElements.forEach(el => {
      begin = Math.min(begin, calculateStartOffset(this.root, el))
      end = Math.max(end, calculateEndOffset(this.root, el))
    })
    const endTime = new Date().getTime()

    console.log(`Visible: ${begin}-${end} (${this.visibleElements.size} visible elements, ${Math.abs(endTime - startTime)}ms)`)
    return [begin, end]
  }
}

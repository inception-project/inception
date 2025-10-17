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
import './ApacheAnnotatorEditor.scss'
import { unpackCompactAnnotatedTextV2, type DiamAjax, type DiamLoadAnnotationsOptions, type VID, ViewportTracker, offsetToRange, AnnotatedText, Span, TextMarker, type Offsets, AnnotationOverEvent, AnnotationOutEvent } from '@inception-project/inception-js-api'
import { type CompactAnnotatedText } from '@inception-project/inception-js-api/src/model/compact_v2'
import { highlightText } from '@apache-annotator/dom'
import { annotatorState } from './ApacheAnnotatorState.svelte'
import { ResizeManager } from './ResizeManager'
import { bgToFgColor } from '@inception-project/inception-js-api/src/util/Coloring'
import { SectionAnnotationVisualizer } from './SectionAnnotationVisualizer'
import { SectionAnnotationCreator } from './SectionAnnotationCreator'

export const CLASS_RELATED = 'iaa-related'

export const NO_LABEL = '◌'
export const ERROR_LABEL = '🔴'
export const INFO_LABEL = 'ℹ️'
export const WARN_LABEL = '⚠️'

export class ApacheAnnotatorVisualizer {
  private ajax: DiamAjax
  readonly root: Element
  private toCleanUp = new Set<Function>()
  private resizer: ResizeManager
  private tracker: ViewportTracker

  private sectionSelector: string
  private sectionAnnotationVisualizer: SectionAnnotationVisualizer
  private sectionAnnotationCreator: SectionAnnotationCreator

  private data? : AnnotatedText

  private scrolling = false
  private lastScrollTop: number | undefined = undefined
  private removeScrollMarkers: (() => void)[] = []
  private removeScrollMarkersTimeout: number | undefined = undefined
  private removePingMarkers: (() => void)[] = []
  private removePingMarkersTimeout: number | undefined = undefined

  private alpha = '55'

  constructor (element: Element, ajax: DiamAjax, sectionSelector: string) {
    this.ajax = ajax
    this.root = element
    this.sectionSelector = sectionSelector

    this.tracker = new ViewportTracker(this.root, () => this.loadAnnotations())
    this.resizer = new ResizeManager(this, this.ajax)

    this.sectionAnnotationCreator = new SectionAnnotationCreator(this.root, this.ajax, this.sectionSelector)
    this.sectionAnnotationVisualizer = new SectionAnnotationVisualizer(this.root, this.ajax, this.sectionSelector)

    // Event handlers for the resizer component
    this.root.addEventListener('mouseover', e => this.showResizer(e))

    // Event handlers for custom events
    this.root.addEventListener('mouseover', event => {
      if (!(event instanceof MouseEvent) || !(event.target instanceof HTMLElement)) return
      const vid = event.target.getAttribute('data-iaa-id')
      if (!vid) return
      const annotation = this.data?.getAnnotation(vid)
      if (!annotation) return
      event.target.dispatchEvent(new AnnotationOverEvent(annotation, event))
    })
    this.root.addEventListener('mouseout', event => {
      if (!(event instanceof MouseEvent) || !(event.target instanceof HTMLElement)) return
      const vid = event.target.getAttribute('data-iaa-id')
      if (!vid) return
      const annotation = this.data?.getAnnotation(vid)
      if (!annotation) return
      event.target.dispatchEvent(new AnnotationOutEvent(annotation, event))
    })

    // Add event handlers for highlighting extent of the annotation the mouse is currently over
    this.root.addEventListener('mouseover', e => this.addAnnotationHighlight(e as MouseEvent))
    this.root.addEventListener('mouseout', e => this.removeAnnotationHighight(e as MouseEvent))
  }

  private showResizer (event: Event): void {
    if (!(event instanceof MouseEvent) || !(event.target instanceof HTMLElement)) return

    const vid = event.target.getAttribute('data-iaa-id')
    if (vid) this.resizer.show(vid)
  }

  private addAnnotationHighlight (event: MouseEvent) {
    if (!(event.target instanceof Element)) return

    const vid = event.target.getAttribute('data-iaa-id')
    if (!vid) return

    this.getHighlightsForAnnotation(vid).forEach(e => e.classList.add('iaa-hover'))
  }

  private removeAnnotationHighight (event: MouseEvent) {
    if (!(event.target instanceof Element)) return

    this.root.querySelectorAll('.iaa-hover').forEach(e => e.classList.remove('iaa-hover'))
  }

  loadAnnotations (): void {
    // scrollTo uses a timeout to work around the problem that the browser does not always properly
    // scroll to the target element. We want to avoid loading annotations while scrolling is still
    // in progress. Once scrolling is complete, we should get triggered by the ViewportTracker.
    if (this.scrolling) return

    const options: DiamLoadAnnotationsOptions = {
      range: this.tracker.currentRange,
      includeText: false,
      clipSpans: false,
      format: 'compact_v2'
    }

    console.log(`Loading annotations for range ${JSON.stringify(options.range)}`)

    this.ajax.loadAnnotations(options)
      .then((doc: CompactAnnotatedText) => {
        this.data = unpackCompactAnnotatedTextV2(doc)
        this.renderAnnotations(this.data)
      })
  }

  private renderAnnotations (doc: AnnotatedText): void {
    console.log(`Client-side starting`)
    const startTime = performance.now()

    this.clearHighlights()
    this.resizer.hide()

    if (doc.spans) {
      console.log(`Loaded ${doc.spans.size} span annotations`)
      doc.spans.forEach(span => this.renderSpanAnnotation(doc, span))

      this.removeSpuriousZeroWidthHighlights()
      if (!annotatorState.showEmptyHighlights) {
        this.removeWhitepaceOnlyHighlights()
      }

      this.postProcessHighlights()
    }

    if (annotatorState.showAggregatedLabels) {
      this.sectionAnnotationVisualizer.render(doc)
    }
    else {
      this.sectionAnnotationVisualizer.clear()
    }

    this.sectionAnnotationCreator.render(doc)

    if (doc.textMarkers) {
      doc.textMarkers.forEach(marker => this.renderTextMarker(doc, marker))
      this.getAllTextMarkers().forEach(e => {
        if (!e.textContent) {
          e.remove()
        }
      })
      this.renderVerticalSelectionMarker(doc)
    }

    if (doc.relations) {
      this.renderSelectedRelationEndpointHighlights(doc)
    }

    const endTime = performance.now()

    console.log(`Client-side rendering took ${endTime - startTime}ms`)
  }

  private renderVerticalSelectionMarker (doc: AnnotatedText) {
    // We assume for the moment that only one annotation can be selected at a time
    const selectedAnnotationVids = doc.markedAnnotations.get('focus') || []
    if (selectedAnnotationVids.length === 0) return

    const highlights = Array.from(this.root.querySelectorAll('.iaa-marker-focus'))
    if (highlights.length === 0) return

    let top: number | undefined
    let bottom: number | undefined
    highlights.forEach(hl => {
      const r = hl.getBoundingClientRect()
      // Is the highlighted element actually visible or is it "display: none"
      if ((hl.classList.contains('iaa-zero-width') && r.height !== 0) || (r.width !== 0 && r.height !== 0)) {
        top = top === undefined ? r.top : Math.min(top, r.top)
        bottom = bottom === undefined ? r.bottom : Math.max(bottom, r.bottom)
      }
    })

    if (top === undefined || bottom === undefined) {
      console.warn('Cannot determine top/bottom for vertical selection marker')
      return
    }

    const scrollerContainerRect = this.root.closest('.i7n-wrapper')?.getBoundingClientRect() || this.root.getBoundingClientRect()
    const scrollTop = this.root.closest('.i7n-wrapper')?.scrollTop || this.root.scrollTop || 0

    const vhl = this.root.ownerDocument.createElement('div')
    vhl.classList.add('iaa-vertical-marker-focus')
    vhl.style.top = `${top - scrollerContainerRect.top + scrollTop}px`
    vhl.style.height = `${bottom - top}px`
    this.root.appendChild(vhl)

    const selectedAnnotation = doc.spans.get(selectedAnnotationVids[0])
    if (!selectedAnnotation) return

    if (!(selectedAnnotation.clippingFlags?.startsWith('s'))) {
      const terminator = this.root.ownerDocument.createElement('div')
      terminator.classList.add('iaa-vertical-marker-focus')
      terminator.classList.add('terminator-start')
      terminator.addEventListener('click', e => {
        e.stopPropagation()
        this.scrollTo({ offset: selectedAnnotation.offsets[0][1] + doc.window[0], position: 'unused' })
      })
      vhl.appendChild(terminator)
    }

    if (!(selectedAnnotation.clippingFlags?.endsWith('e'))) {
      const terminator = this.root.ownerDocument.createElement('div')
      terminator.classList.add('iaa-vertical-marker-focus')
      terminator.classList.add('terminator-end')
      terminator.addEventListener('click', e => {
        e.stopPropagation()
        this.scrollTo({ offset: selectedAnnotation.offsets[0][0] + doc.window[0], position: 'unused' })
      })
      vhl.appendChild(terminator)
    }
  }

  /**
   * The highlighter may create highlighs that are empty (they do not even contain whitespace). This
   * method removes such highlights.
   */
  private removeSpuriousZeroWidthHighlights () {
    this.getAllHighlights().forEach(e => {
      if (!e.classList.contains('iaa-zero-width') && !e.textContent) {
        // Removing the entire highlight element here should be fine as it should not contain any
        // relevant DOM nodes, e.g. nodes relevant to text offsets.
        e.remove()
      }
    })
  }

  /**
   * Some highlights may only contain whitepace. This method removes such highlights.
   */
  private removeWhitepaceOnlyHighlights (selector: string = '.iaa-highlighted') {
    const start = performance.now();
    const candidates = this.root.querySelectorAll(selector)
    console.log(`Found ${candidates.length} elements matching [${selector}] to remove whitespace-only highlights`)
    candidates.forEach(e => {
      if (!e.classList.contains('iaa-zero-width') && !e.textContent?.trim()) {
        e.after(...e.childNodes)
        e.remove()
      }
    })
    const end = performance.now();
    console.log(`Removing whitespace only highlights took ${end - start}ms`)
  }

  private postProcessHighlights () {
    // Find all the highlights that belong to the same annotation (VID)
    const highlightsByVid = groupHighlightsByVid(this.getAllHighlights())

    // Add special CSS classes to the first and last highlight of each annotation
    for (const highlights of highlightsByVid.values()) {
      if (highlights.length) {
        if (annotatorState.showLabels) {
          highlights.forEach(e => e.classList.add('iaa-inline-label'))
        }
        highlights[0].classList.add('iaa-first-highlight')
        highlights[highlights.length - 1].classList.add('iaa-last-highlight')
      }
    }
  }

  private getAllHighlights () {
    return this.root.querySelectorAll('.iaa-highlighted')
  }

  private getAllTextMarkers () {
    return this.root.querySelectorAll('.iaa-text-marker')
  }

  private renderSelectedRelationEndpointHighlights (doc: AnnotatedText) {
    const selectedAnnotationVids = doc.markedAnnotations.get('focus') || []
    for (const relation of doc.relations.values()) {
      if (!selectedAnnotationVids.includes(relation.vid)) {
        continue
      }

      const sourceVid = relation.arguments[0][0]
      const targetVid = relation.arguments[1][0]
      this.getHighlightsForAnnotation(sourceVid).forEach(e => e.classList.add(CLASS_RELATED))
      this.getHighlightsForAnnotation(targetVid).forEach(e => e.classList.add(CLASS_RELATED))
    }
  }

  // eslint-disable-next-line no-undef
  getHighlightsForAnnotation (vid: VID): NodeListOf<Element> {
    return this.root.querySelectorAll(`[data-iaa-id="${vid}"].iaa-highlighted`)
  }

  private renderTextMarker (doc: AnnotatedText, marker: TextMarker) {
    const range = offsetToRange(this.root, marker.offsets[0][0] + doc.window[0], marker.offsets[0][1] + doc.window[0])

    if (!range) {
      console.debug('Could not render text marker: ' + marker)
      return
    }

    const attributes = {
      class: `iaa-text-marker iaa-marker-${marker.type}`
    }

    this.toCleanUp.add(highlightText(range, 'mark', attributes))
  }

  private renderSpanAnnotation (doc: AnnotatedText, span: Span) {
    const begin = span.offsets[0][0] + doc.window[0]
    const end = span.offsets[0][1] + doc.window[0]

    const classList = ['iaa-highlighted']
    const ms = doc.annotationMarkers.get(span.vid) || []
    ms.forEach(m => classList.push(`iaa-marker-${m.type}`))

    if (begin === end) classList.push('iaa-zero-width')

    const hasError = span.comments?.find(comment => comment.type === 'error')
    if (hasError) classList.push('iaa-marker-error')

    const hasInfo = span.comments?.find(comment => comment.type === 'info')
    if (hasInfo) classList.push('iaa-marker-info')
    

    const styleList = [
      `--iaa-color: ${bgToFgColor(span.color || '#000000')}`,
      `--iaa-background-color: ${span.color || '#000000'}${this.alpha}`,
      `--iaa-border-color: ${span.color || '#000000'}`
    ]

    var decorations = ''
    if (classList.includes('iaa-marker-info')) decorations += INFO_LABEL
    if (classList.includes('iaa-marker-warn')) decorations += WARN_LABEL
    if (classList.includes('iaa-marker-error')) decorations += ERROR_LABEL
    if (decorations) decorations += ' '

    let label = `${decorations}${span.label || `[${span.layer.name}]` || NO_LABEL}`
    if (span.score && !span.hideScore) {
      label += ` [${span.score.toFixed(2)}]`
    }

    const attributes = {
      'data-iaa-id': `${span.vid}`,
      'data-iaa-label': label,
      class: classList.join(' '),
      style: styleList.join('; ')
    }

    const viewportBegin = this.tracker.currentRange[0]
    const viewportEnd = this.tracker.currentRange[1]

    if (viewportBegin <= begin && end <= viewportEnd) {
      // Quick and easy if the annotation fits entirely into the visible viewport
      const startTime = performance.now()
      this.renderHighlight(span, begin, end, attributes)
      const endTime = performance.now()
      // console.debug(`Rendering span with size ${end - begin} took ${Math.abs(endTime - startTime)}ms`)
    } else {
      // Try optimizing for long spans to improve rendering performance
      let fragmentCount = 0
      const startTime = performance.now()

      const coreBegin = Math.max(begin, viewportBegin)
      const coreEnd = Math.min(end, viewportEnd)
      if (coreBegin <= coreEnd) {
        this.renderHighlight(span, coreBegin, coreEnd, attributes)
        fragmentCount++
      }

      attributes.class += ' iaa-zero-width' // Prevent prefix/suffix fragmens from being cleaned up
      if (!(viewportBegin <= begin && begin <= viewportEnd)) {
        this.renderHighlight(span, begin, begin, attributes)
        fragmentCount++
      }

      if (!(viewportBegin <= end && end <= viewportEnd)) {
        this.renderHighlight(span, end, end, attributes)
        fragmentCount++
      }
      const endTime = performance.now()
      // console.debug(`Rendering span with size ${end - begin} took ${Math.abs(endTime - startTime)}ms (${fragmentCount} fragments)`)
    }
  }

  renderHighlight (span: Span, begin: number, end: number, attributes: {class: string}): void {
    const range = offsetToRange(this.root, begin, end)
    if (!range) {
      console.debug(`Could not determine fragment range for (${begin},${end}) of annotation ${span}`)
      return
    }

    if (range.collapsed && !attributes.class.includes('iaa-zero-width')) {
      console.debug(`Fragment range for (${begin},${end}) of annotation ${span} is collapsed`)
      return
    }

    this.toCleanUp.add(highlightText(range, 'mark', attributes))
  }

  private clearScrollMarkers () {
    if (this.removeScrollMarkersTimeout) {
      this.cancelScheduled(this.removeScrollMarkersTimeout)
      this.removeScrollMarkersTimeout = undefined
      this.removeScrollMarkers.forEach(remove => remove())
      this.removeScrollMarkers = []
    }
  }

  private renderPingMarkers(pingRanges?: Offsets[]) {
    if (!pingRanges) return

    console.log('Rendering ping markers')

    for (const pingOffset of pingRanges || []) {
      const pingRange = offsetToRange(this.root, pingOffset[0], pingOffset[1])
      if (pingRange) {
        this.removePingMarkers.push(highlightText(pingRange, 'mark', { class: 'iaa-ping-marker' }))
      }
    }

    this.removeWhitepaceOnlyHighlights('.iaa-ping-marker')
    this.removeSpuriousZeroWidthHighlights()

    if (this.removePingMarkers.length > 0) {
      this.removePingMarkersTimeout = this.schedule(2000, () => this.clearPingMarkers())
    }
 }

  private clearPingMarkers () {
    console.log('Clearing ping markers');
    
    if (this.removePingMarkersTimeout) {
      this.cancelScheduled(this.removePingMarkersTimeout)
      this.removePingMarkersTimeout = undefined
      this.removePingMarkers.forEach(remove => remove())
      this.removePingMarkers = []
    }
  }

  scrollTo (args: { offset: number, position?: string, pingRanges?: Offsets[] }): void {
    const range = offsetToRange(this.root, args.offset, args.offset)
    if (!range) return

    this.clearScrollMarkers()
    this.clearPingMarkers()

    // Add scroll marker
    const removeScrollMarker = highlightText(range, 'mark', { id: 'iaa-scroll-marker' })
    this.removeScrollMarkers = [removeScrollMarker]

    if (!annotatorState.showEmptyHighlights) {
      this.removeWhitepaceOnlyHighlights('.iaa-ping-marker')
    }

    this.root.querySelectorAll('.iaa-ping-marker').forEach(e => {
      if (!e.textContent) e.remove()
    })

    // The scroll target may be hidden. In this case, we need to find the next visible element.
    let scrollTarget: Element | null = this.root.querySelector('#iaa-scroll-marker')
    while (scrollTarget !== null) {
      const targetStyle = window.getComputedStyle(scrollTarget)
      if (targetStyle.display === 'none' || targetStyle.visibility === 'hidden') {
        if (scrollTarget.nextElementSibling) {
          scrollTarget = scrollTarget.nextElementSibling
        } else {
          scrollTarget = scrollTarget.parentElement
        }
        continue
      }
      break
    }

    const finalScrollTarget = scrollTarget
    if (finalScrollTarget) {
      // As part of scrolling and rendering, it may be possible that CSS scroll padding properties
      // are set on our scroll root element. For such a case, we schedule a new scroll action
      // which would then take the new padding into account. We do this as long as the transient
      // markers are still there.
      var scrollIntoViewFunc = () => { 
        finalScrollTarget.scrollIntoView({ behavior: 'auto', block: 'center', inline: 'nearest' })
        if (this.removeScrollMarkers.length > 0) {
          this.schedule(100, scrollIntoViewFunc)
        }
        
        if (this.root instanceof HTMLElement) {
          if (this.root.scrollTop === this.lastScrollTop) {
            this.scrollToComplete(args.pingRanges)
          }
          else {
            this.lastScrollTop = this.root.scrollTop
          }
        }
      }

      this.scrolling = true
      this.sectionAnnotationVisualizer.suspend()
      this.sectionAnnotationCreator.suspend()
      this.removeScrollMarkersTimeout = this.schedule(100, scrollIntoViewFunc)
    }
  }

  private schedule(timeout: number, callback: () => void): number{
    if (typeof window.requestIdleCallback=="undefined") {
      return window.setTimeout(callback, timeout)
    } else {
      return window.requestIdleCallback(callback, { timeout: timeout })
    }
  }

  private cancelScheduled(handle: number) {
    if (typeof window.cancelIdleCallback=="undefined") {
      window.clearTimeout(handle)
    } else {
      window.cancelIdleCallback(handle)
    }
  }

  private scrollToComplete(pingRanges?: Offsets[]) {
    console.log('Scrolling complete')

    this.clearScrollMarkers()
    this.renderPingMarkers(pingRanges)
    this.root.normalize() // https://github.com/apache/incubator-annotator/issues/120

    this.scrolling = false
    this.sectionAnnotationCreator.resume()
    this.sectionAnnotationVisualizer.resume()
    this.lastScrollTop = undefined

    // Workaround for Firefox somehow scrolling the page body a bit when we scroll
    // inside the iframe during page loading
    if (navigator.userAgent.includes("Firefox")) {
      if (window?.parent?.document?.body?.scrollTop) {
        window.parent.document.body.scrollTop = 0
      }
    }
  }

  private clearHighlights (): void {
    this.root.querySelectorAll('.iaa-vertical-marker-focus').forEach(e => e.remove())

    if (!this.toCleanUp || this.toCleanUp.size === 0) {
      return
    }

    const startTime = performance.now()
    const highlightCount = this.toCleanUp.size
    this.toCleanUp.forEach(cleanup => cleanup())
    this.toCleanUp.clear()
    this.root.normalize() // https://github.com/apache/incubator-annotator/issues/120
    const endTime = performance.now()
    console.log(`Cleaning up ${highlightCount} annotations and normalizing DOM took ${Math.abs(endTime - startTime)}ms`)
  }

  destroy (): void {
    this.sectionAnnotationCreator.destroy()
    this.sectionAnnotationVisualizer.destroy()
    this.tracker.disconnect()
    this.clearHighlights()
  }
}

/**
 * Groups highlights by their VID.
 *
 * @param highlights list of highlights.
 * @returns groups of highlights by VID.
 */
// eslint-disable-next-line no-undef
export function groupHighlightsByVid (highlights: NodeListOf<Element>) {
  const spansByVid = new Map<VID, Array<Element>>()
  for (const highlight of Array.from(highlights)) {
    const vid = highlight.getAttribute('data-iaa-id')
    if (!vid) continue

    let sectionGroup = spansByVid.get(vid)
    if (!sectionGroup) {
      sectionGroup = []
      spansByVid.set(vid, sectionGroup)
    }
    sectionGroup.push(highlight)
  }
  return spansByVid
}

/**
 * Utility function to find the closest highlight element to the given target.
 *
 * @param target a DOM node.
 * @returns the closest highlight element or null if none is found.
 */
export function closestHighlight (target: Node | null): HTMLElement | null {
  if (!(target instanceof Node)) {
    return null
  }

  if (target instanceof Text) {
    const parent = target.parentElement
    if (!parent) return null
    target = parent
  }

  const targetElement = target as Element
  return targetElement.closest('[data-iaa-id]')
}

/**
 * Utility function to find all highlights that are ancestors of the given target.
 *
 * @param target a DOM node.
 * @returns all highlight elements that are ancestors of the given target.
 */
export function highlights (target: Node | null): HTMLElement[] {
  let hl = closestHighlight(target)
  const result: HTMLElement[] = []
  while (hl) {
    result.push(hl)
    hl = closestHighlight(hl.parentElement)
  }
  return result
}

/**
 * Calculates the rectangle of the inline label for the given highlight.
 *
 * @param highlight a highlight element.
 * @returns the inline label rectangle.
 */
export function getInlineLabelClientRect (highlight: Element): DOMRect {
  const r = highlight.getClientRects()[0]

  // TODO: It may be possible to implement this in a simpler way using
  // `window.getComputedStyle(highlight, ':before')`

  let cr: DOMRect
  if (highlight.firstChild instanceof Text) {
    const range = document.createRange()
    range.selectNode(highlight.firstChild)
    cr = range.getClientRects()[0]
  } else if (highlight.firstChild instanceof Element) {
    cr = highlight.firstChild.getClientRects()[0]
  } else {
    throw new Error('Unexpected node type')
  }

  return new DOMRect(r.left, r.top, cr.left - r.left, r.height)
}

/**
 * Checks if the given point is inside the given DOMRect.
 */
export function isPointInRect (point: { x: number; y: number }, rect: DOMRect): boolean {
  return point.x >= rect.left && point.x <= rect.right && point.y >= rect.top && point.y <= rect.bottom
}

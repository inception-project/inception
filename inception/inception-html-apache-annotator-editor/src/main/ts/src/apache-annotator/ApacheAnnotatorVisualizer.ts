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
import { unpackCompactAnnotatedTextV2, DiamAjax, DiamLoadAnnotationsOptions, VID, ViewportTracker, offsetToRange, AnnotatedText, Span, TextMarker, Offsets } from '@inception-project/inception-js-api'
import { CompactAnnotatedText } from '@inception-project/inception-js-api/src/model/compact_v2'
import { highlightText } from '@apache-annotator/dom'
import { showEmptyHighlights, showLabels } from './ApacheAnnotatorState'
import { ResizeManager } from './ResizeManager'

export const CLASS_RELATED = 'iaa-related'

export const NO_LABEL = '◌'

export class ApacheAnnotatorVisualizer {
  private ajax: DiamAjax
  readonly root: Element
  private toCleanUp = new Set<Function>()
  private observer: IntersectionObserver
  private resizer: ResizeManager
  private tracker: ViewportTracker
  private showInlineLabels = false
  private showEmptyHighlights = false

  private removePingMarkers: (() => void)[] = []
  private removePingMarkersTimeout: number | undefined = undefined
  private alpha = '55'

  constructor (element: Element, ajax: DiamAjax) {
    this.ajax = ajax
    this.root = element

    this.tracker = new ViewportTracker(this.root, () => this.loadAnnotations())
    this.resizer = new ResizeManager(this, this.ajax)

    // Event handlers for the resizer component
    this.root.addEventListener('mouseover', e => this.showResizer(e))

    // Add event handlers for highlighting extent of the annotation the mouse is currently over
    this.root.addEventListener('mouseover', e => this.addAnnotationHighlight(e as MouseEvent))
    this.root.addEventListener('mouseout', e => this.removeAnnotationHighight(e as MouseEvent))

    showLabels.subscribe(enabled => {
      this.showInlineLabels = enabled
      this.loadAnnotations()
    })

    showEmptyHighlights.subscribe(enabled => {
      this.showEmptyHighlights = enabled
      this.loadAnnotations()
    })
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
    const options: DiamLoadAnnotationsOptions = {
      range: this.tracker.currentRange,
      includeText: false,
      clipSpans: false,
      format: 'compact_v2'
    }

    console.log(`Loading annotations for range ${JSON.stringify(options.range)}`)

    this.ajax.loadAnnotations(options)
      .then((doc: CompactAnnotatedText) => this.renderAnnotations(unpackCompactAnnotatedTextV2(doc)))
  }

  private renderAnnotations (doc: AnnotatedText): void {
    const startTime = new Date().getTime()

    this.clearHighlights()
    this.resizer.hide()

    if (doc.spans) {
      console.log(`Loaded ${doc.spans.size} span annotations`)
      doc.spans.forEach(span => this.renderSpanAnnotation(doc, span))
      this.removeSpuriousZeroWidthHighlights()
      if (!this.showEmptyHighlights) {
        this.removeWhitepaceOnlyHighlights()
      }

      this.postProcessHighlights()
    }

    if (doc.textMarkers) {
      doc.textMarkers.forEach(marker => this.renderTextMarker(doc, marker))
      this.renderVerticalSelectionMarker(doc)
    }

    if (doc.relations) {
      this.renderSelectedRelationEndpointHighlights(doc)
    }

    const endTime = new Date().getTime()
    console.log(`Client-side rendering took ${Math.abs(endTime - startTime)}ms`)
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

    const scrollerContainerRect = this.root.getBoundingClientRect()

    const vhl = this.root.ownerDocument.createElement('div')
    vhl.classList.add('iaa-vertical-marker-focus')
    vhl.style.top = `${top - scrollerContainerRect.top + this.root.scrollTop}px`
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
        // Removing the entire highlight element here should be find as it should not contain any
        // relevant DOM nodes, e.g. nodes relevant to text offsets.
        e.remove()
      }
    })
  }

  /**
   * Some highliths may only contain whitepace. This method removes such highlights.
   */
  private removeWhitepaceOnlyHighlights () {
    this.getAllHighlights().forEach(e => {
      if (!e.classList.contains('iaa-zero-width') && !e.textContent?.trim()) {
        e.after(...e.childNodes)
        e.remove()
      }
    })
  }

  private postProcessHighlights () {
    // Find all the highlights that belong to the same annotation (VID)
    const highlightsByVid = groupHighlightsByVid(this.getAllHighlights())

    // Add special CSS classes to the first and last highlight of each annotation
    for (const highlights of highlightsByVid.values()) {
      if (highlights.length) {
        if (this.showInlineLabels) {
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
    return this.root.querySelectorAll(`[data-iaa-id="${vid}"]`)
  }

  private renderTextMarker (doc: AnnotatedText, marker: TextMarker) {
    const range = offsetToRange(this.root, marker.offsets[0][0] + doc.window[0], marker.offsets[0][1] + doc.window[0])

    if (!range) {
      console.debug('Could not render text marker: ' + marker)
      return
    }

    const attributes = {
      class: `iaa-marker-${marker[0]}`
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

    const styleList = [
      `--iaa-background-color: ${span.color || '#000000'}${this.alpha}`,
      `--iaa-border-color: ${span.color || '#000000'}`
    ]

    const attributes = {
      'data-iaa-id': `${span.vid}`,
      'data-iaa-label': `${span.label || NO_LABEL}`,
      class: classList.join(' '),
      style: styleList.join('; ')
    }

    const viewportBegin = this.tracker.currentRange[0]
    const viewportEnd = this.tracker.currentRange[1]

    if (viewportBegin <= begin && end <= viewportEnd) {
      // Quick and easy if the annotation fits entirely into the visible viewport
      const startTime = new Date().getTime()
      this.renderHighlight(span, begin, end, attributes)
      const endTime = new Date().getTime()
      console.debug(`Rendering span with size ${end - begin} took ${Math.abs(endTime - startTime)}ms`)
    } else {
      // Try optimizing for long spans to improve rendering performance
      let fragmentCount = 0
      const startTime = new Date().getTime()

      const coreBegin = Math.max(begin, viewportBegin)
      const coreEnd = Math.min(end, viewportEnd)
      this.renderHighlight(span, coreBegin, coreEnd, attributes)
      fragmentCount++

      attributes.class += ' iaa-zero-width' // Prevent prefix/suffix fragmens from being cleaned up
      if (!(viewportBegin <= begin && begin <= viewportEnd)) {
        this.renderHighlight(span, begin, begin, attributes)
        fragmentCount++
      }

      if (!(viewportBegin <= end && end <= viewportEnd)) {
        this.renderHighlight(span, end, end, attributes)
        fragmentCount++
      }
      const endTime = new Date().getTime()
      console.debug(`Rendering span with size ${end - begin} took ${Math.abs(endTime - startTime)}ms (${fragmentCount} fragments)`)
    }
  }

  renderHighlight (span: Span, begin: number, end: number, attributes: {}): void {
    const range = offsetToRange(this.root, begin, end)
    if (!range) {
      console.debug(`Could not determine fragment range for (${begin},${end}) of annotation ${span}`)
      return
    }
    this.toCleanUp.add(highlightText(range, 'mark', attributes))
  }

  scrollTo (args: { offset: number, position?: string, pingRanges?: Offsets[] }): void {
    const range = offsetToRange(this.root, args.offset, args.offset)
    if (!range) return

    window.clearTimeout(this.removePingMarkersTimeout)
    this.removePingMarkers.forEach(remove => remove())

    const removeScrollMarker = highlightText(range, 'mark', { id: 'iaa-scroll-marker' })
    this.removePingMarkers = []
    for (const pingOffset of args.pingRanges || []) {
      const pingRange = offsetToRange(this.root, pingOffset[0], pingOffset[1])
      if (!pingRange) continue
      this.removePingMarkers.push(highlightText(pingRange, 'mark', { class: 'iaa-ping-marker' }))
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

    if (scrollTarget) {
      scrollTarget.scrollIntoView({ behavior: 'auto', block: 'center', inline: 'nearest' })
    }

    removeScrollMarker()
    this.removePingMarkersTimeout = window.setTimeout(() => {
      this.removePingMarkers.forEach(remove => remove())
      console.log('ping removed')
    }, 2000)
  }

  private clearHighlights (): void {
    this.root.querySelectorAll('.iaa-vertical-marker-focus').forEach(e => e.remove())

    if (!this.toCleanUp || this.toCleanUp.size === 0) {
      return
    }

    const startTime = new Date().getTime()
    const highlightCount = this.toCleanUp.size
    this.toCleanUp.forEach(cleanup => cleanup())
    this.toCleanUp.clear()
    this.root.normalize() // https://github.com/apache/incubator-annotator/issues/120
    const endTime = new Date().getTime()
    console.log(`Cleaning up ${highlightCount} annotations and normalizing DOM took ${Math.abs(endTime - startTime)}ms`)
  }

  destroy (): void {
    if (this.observer) {
      this.observer.disconnect()
    }

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

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
import './SectionAnnotationVisualizer.scss'
import { AnnotatedText, bgToFgColor, DiamAjax, Span, VID } from "@inception-project/inception-js-api"

export class SectionAnnotationVisualizer {
  private sectionSelector: string
  private ajax: DiamAjax
  private root: Element

  public constructor(root: Element, ajax: DiamAjax, sectionSelector: string) {
    this.root = root
    this.ajax = ajax
    this.sectionSelector = sectionSelector

    if (this.sectionSelector) {
      const root = this.root.closest('.i7n-wrapper') || this.root
      // on scrolling the window, we need to ensure that the panels stay visible
      root.addEventListener('scroll', () => this.ensurePanelVisibility())
    }
  }

  render(doc: AnnotatedText) {
    if (this.sectionSelector) {
      this.clear()
      this.renderSectionGroups(doc)
      this.ensurePanelVisibility()
    }
  }

  clear() {
    if (this.sectionSelector) {
      this.root.parentElement?.querySelectorAll('.iaa-visible-annotations-panel').forEach(e => e.remove())
      this.root.querySelectorAll('.iaa-visible-annotations-panel-spacer').forEach(e => e.remove())
    }
  }

  private ensurePanelVisibility() {
    const panels = Array.from(this.root.parentNode?.querySelectorAll('.iaa-visible-annotations-panel') || [])

    const root = this.root.closest('.i7n-wrapper') || this.root
    // const root = this.root
    const rootRect = root.getBoundingClientRect()
    const scrollY = (root.scrollTop || 0) - rootRect.top
    
    let lastSectionPanelBottom = rootRect.top
    for (const panel of (panels as HTMLElement[])) {
      const sectionId = panel.getAttribute('data-iaa-applies-to')
      const spacer = this.root.querySelector(`.iaa-visible-annotations-panel-spacer[data-iaa-applies-to="${sectionId}"]`)
      if (!spacer) {
        console.warn(`No spacer found for section [${sectionId}]`)
        continue
      }
      const section = this.root.querySelector(`[id="${sectionId}"]`)
      if (!section) {
        console.warn(`Cannot find element for section [${sectionId}]`)
        continue
      }

      const sectionRect = section.getBoundingClientRect()
      const spacerRect = spacer.getBoundingClientRect() // Dimensions same as panel

      const sectionLeavingViewport = sectionRect.bottom - spacerRect.height < rootRect.top
      // console.log(`Leaving viewport = ${sectionLeavingViewport}`)
      if (sectionLeavingViewport) {
        const hiddenUnderHigherLevelPanel = lastSectionPanelBottom && (sectionRect.bottom + rootRect.top - spacerRect.height) < lastSectionPanelBottom
        if (hiddenUnderHigherLevelPanel) {
          // If there is already a higher-level panel stacked then we snap the panel back to its
          // spacer immediately
          panel.style.top = `${spacerRect.top + scrollY}px`
        }
        else {
          // Otherwise, we move the panel along with the bottom of the section
          panel.style.top = `${sectionRect.bottom + scrollY - spacerRect.height}px`
        }
        continue
      }

      const shouldKeepPanelVisibleAtTop = spacerRect.top < lastSectionPanelBottom && !(sectionRect.bottom < lastSectionPanelBottom)
      if (shouldKeepPanelVisibleAtTop) {
        // Keep the panel at the top of the viewport if the spacer is above the viewport
        // and the section is still visible
        panel.style.top = `${scrollY + lastSectionPanelBottom}px`
        lastSectionPanelBottom = panel.getBoundingClientRect().bottom
        continue
      }

      // Otherwise, keep the panel at the same position as the spacer
      panel.style.top = `${spacerRect.top + scrollY}px`
    }

    if (root instanceof HTMLElement) {
      root.style.scrollPaddingTop = `${lastSectionPanelBottom - rootRect.top}px`
    }
  }

  private renderSectionGroups(doc: AnnotatedText) {
    const selectedAnnotationVids = doc.markedAnnotations.get('focus') || []

    // find all currently visible marked elements
    const highlights = this.root.querySelectorAll(`[data-iaa-id]`)

    // Find all the spans that belong to the same annotation (VID)
    const highlightsByVid = this.groupHighlightsByVid(highlights)

    // Establish the section for each annotation
    const sectionElements = new Map<VID, Element>()
    for (const [vid, spans] of highlightsByVid) {
      const sectionElement = this.findCommonAncestor(spans, this.sectionSelector)
      sectionElements.set(vid, sectionElement)
    }

    // Create an annotation panel for each section
    const annotationPanelsBySectionElement = new Map<Element, HTMLElement>()
    const annotationPanelsByVid = new Map<VID, Element>()
    for (const [vid, sectionElement] of sectionElements) {
      let panel = annotationPanelsBySectionElement.get(sectionElement)
      if (!panel) {
        if (!sectionElement.id) {
          console.warn(`Cannot create section panel for section without ID`, sectionElement)
          continue
        }

        panel = this.createAnnotationPanel()
        panel.setAttribute('data-iaa-applies-to', sectionElement.id)
        this.addAnnotationPanel(panel)
        annotationPanelsBySectionElement.set(sectionElement, panel)
      }

      annotationPanelsByVid.set(vid, panel)
    }

    // Render the annotations for each section
    for (const vid of highlightsByVid.keys()) {
      const panel = annotationPanelsByVid.get(vid)
      if (!panel) continue

      let span = doc.spans.get(vid)
      if (span) {
        panel.appendChild(this.createAnnotationPanelItem(span, selectedAnnotationVids))
      }
    }

    // Fit the panels to the sections
    const panels = Array.from(this.root.parentNode?.querySelectorAll('.iaa-visible-annotations-panel') || [])
    for (const panel of (panels as HTMLElement[])) {
      const appliesTo = panel.getAttribute('data-iaa-applies-to')
      if (!appliesTo) continue

      const sectionElement = document.getElementById(appliesTo) as HTMLElement
      if (!sectionElement) continue

      const vspace = panel.getBoundingClientRect().height
      const spacer = document.createElement('div')
      spacer.setAttribute('data-iaa-applies-to', sectionElement.id)
      spacer.classList.add('iaa-visible-annotations-panel-spacer')
      spacer.style.height = `${vspace}px`
      sectionElement.parentElement?.insertBefore(spacer, sectionElement)

      const spacerRect = spacer.getBoundingClientRect()
      const scrollY = spacer.ownerDocument.defaultView?.scrollY || 0
      panel.style.top = `${spacerRect.top + scrollY}px`
    }
  }

  private groupHighlightsByVid(spans: NodeListOf<Element>) {
    const spansByVid = new Map<VID, Array<Element>>()
    for (const span of spans) {
      const vid = span.getAttribute('data-iaa-id')
      if (!vid)
        continue

      let sectionGroup = spansByVid.get(vid)
      if (!sectionGroup) {
        sectionGroup = []
        spansByVid.set(vid, sectionGroup)
      }
      sectionGroup.push(span)
    }
    return spansByVid
  }

  private findCommonAncestor(sectionGroup: Element[], selector?: string): Element {
    const range = document.createRange()
    range.setStartBefore(sectionGroup[0])
    range.setEndAfter(sectionGroup[sectionGroup.length - 1])
    const parent = range.commonAncestorContainer as Element

    if (!selector || (parent.matches(selector))) {
      return parent
    }

    return parent.closest(selector) || parent
  }

  private createAnnotationPanel(): HTMLElement {
    const panel = document.createElement('ul')
    panel.classList.add('iaa-visible-annotations-panel')
    return panel
  }

  private createAnnotationPanelItem(ann: Span, selectedAnnotationVids: VID[]): Element {
    const e = document.createElement('li')
    e.setAttribute('data-iaa-id', `${ann.vid}`)
    e.classList.add('iaa-annotation-panel-item')
    if (selectedAnnotationVids.includes(ann.vid)) {
      e.classList.add('iaa-focussed')
    }
    e.textContent = ann.label || `[${ann.layer.name}]` || "No label"
    e.style.color = bgToFgColor(ann.color || '#000000')
    e.style.backgroundColor = `${ann.color}`
    e.addEventListener('click', event => this.selectAnnotation(event))
    e.addEventListener('mouseenter', event => this.addAnnotationHighlight(event))
    e.addEventListener('mouseleave', event => this.removeAnnotationHighight(event))
    return e
  }

  private selectAnnotation(event: MouseEvent) {
    if (!(event.target instanceof Element)) return

    const vid = event.target.getAttribute('data-iaa-id')
    if (!vid) return

    this.ajax.selectAnnotation(vid, { scrollTo: true })
  }

  private addAnnotationHighlight(event: MouseEvent) {
    if (!(event.target instanceof Element)) return

    const vid = event.target.getAttribute('data-iaa-id')
    if (!vid) return

    this.root.querySelectorAll(`[data-iaa-id="${vid}"]`).forEach(e => {
      e.classList.add('iaa-hover')
    })
  }

  private removeAnnotationHighight(event: MouseEvent) {
    if (!(event.target instanceof Element)) return

    this.root.querySelectorAll(`.iaa-hover`).forEach(e => {
      e.classList.remove('iaa-hover')
    })
  }

  private addAnnotationPanel(panel: Element) {
    if (this.root) {
      this.root.appendChild(panel)
    }
    else {
      console.error('Parent element of root element not found - cannot add visible annotations panel')
    }
  }
}
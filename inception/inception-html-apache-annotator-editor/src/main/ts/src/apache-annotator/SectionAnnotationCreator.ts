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
import './SectionAnnotationCreator.scss'
import { AnnotatedText, calculateEndOffset, calculateStartOffset, DiamAjax } from "@inception-project/inception-js-api"
import { getScrollY } from './SectionAnnotationVisualizer'

export class SectionAnnotationCreator {
  private sectionSelector: string
  private ajax: DiamAjax
  private root: Element

  private observer: IntersectionObserver
  private observerDebounceTimeout: number | undefined
  private suspended = false

  private _previewFrame: HTMLIFrameElement | undefined
  private previewRenderTimeout: number | undefined
  private previewScrollTimeout: number | undefined

  public constructor(root: Element, ajax: DiamAjax, sectionSelector: string) {
    this.root = root
    this.ajax = ajax
    this.sectionSelector = sectionSelector

    if (this.sectionSelector) {
      this.initializeElementTracking()
      this.initializeSectionTypeAttributes()
    }
  }

  public suspend() {
    this.suspended = true
  }

  public resume() {
    this.suspended = false
  }

  public destroy() {
    this.observer.disconnect()
    this.root.querySelectorAll('.iaa-section-control').forEach(e => e.remove())
    this.hidePreviewFrame()
  }

  private initializeSectionTypeAttributes() {
    this.root.querySelectorAll(this.sectionSelector).forEach((e, i) => {
      e.setAttribute('data-iaa-section-type', e.localName)
    })
  }

  private initializeElementTracking(): void {
    const options = {
      root: this.root.ownerDocument,
      rootMargin: '0px',
      threshold: 0.0
    }

    this.observer = new IntersectionObserver((e, o) => this.handleIntersect(e, o), options);
    this.root.querySelectorAll(this.sectionSelector).forEach(e => this.observer.observe(e))
  }

  render(doc: AnnotatedText) {
    if (this.sectionSelector) {
      this.ensureVisibility()
    }
  }

  private ensureVisibility() {
    const scrollY = getScrollY(this.root)
    const panels = Array.from(this.root.querySelectorAll('.iaa-section-control') || [])

    const panelsTops = new Map<HTMLElement, number>()
    for (const panel of (panels as HTMLElement[])) {
      const sectionId = panel.getAttribute('data-iaa-applies-to')
      const section = this.root.querySelector(`[id="${sectionId}"]`)
      if (!section) {
        console.warn(`Cannot find element for section [${sectionId}]`)
        continue
      }
      const sectionRect = section.getBoundingClientRect()
      panelsTops.set(panel, sectionRect.top)
    }

    // Update the position of the panels all at once to avoid layout thrashing
    for (const [panel, top] of panelsTops) {
      panel.style.top = `${top + scrollY}px`
    }
  }

  private handleIntersect(entries: IntersectionObserverEntry[], observer: IntersectionObserver): void {
    if (this.observerDebounceTimeout) {
      window.cancelAnimationFrame(this.observerDebounceTimeout)
      this.observerDebounceTimeout = undefined
    }

    this.observerDebounceTimeout = window.requestAnimationFrame(() => {
      const rootRect = this.root.getBoundingClientRect()
      const scrollY = (this.root.scrollTop || 0) - rootRect.top

      for (const entry of entries) {
        const sectionId = entry.target.id
        const sectionRect = entry.boundingClientRect
        let panel = this.root.querySelector(`.iaa-section-control[data-iaa-applies-to="${sectionId}"]`) as HTMLElement

        if (entry.isIntersecting && !panel) {
          panel = this.createControl()
          panel.setAttribute('data-iaa-applies-to', sectionId)
          panel.style.top = `${sectionRect.top + scrollY}px`
          this.root.appendChild(panel)
        }

        if (!entry.isIntersecting && panel) {
          panel.remove()
        }
      }
    });
  }

  private createControl(): HTMLElement {
    const panel = document.createElement('div')
    panel.classList.add('iaa-section-control')
    panel.textContent = '⊕'
    panel.addEventListener('click', event => this.createAnnotation(event))
    panel.addEventListener('mouseenter', event => this.addSectionHighlight(event))
    panel.addEventListener('mouseleave', event => this.removeSectionHighight(event))
    return panel
  }

  private createAnnotation(event: MouseEvent) {
    if (!(event.target instanceof Element)) return

    const section  = this.getSectionForControl(event.target)
    if (!section) return

    this.ajax.createSpanAnnotation([[calculateStartOffset(this.root, section), calculateEndOffset(this.root, section)]])
  }

  private addSectionHighlight(event: MouseEvent) {
    if (!(event.target instanceof Element)) return

    const section  = this.getSectionForControl(event.target)
    if (!section) return

    section.setAttribute('data-iaa-section-control-hover', '')

    const sectionRect = section.getBoundingClientRect()
    if (sectionRect.bottom > window.innerHeight + window.pageYOffset) {
      this.showPreviewFrame()
    }
  }

  private removeSectionHighight(event: MouseEvent) {
    if (!(event.target instanceof Element)) return

    if (this._previewFrame && event.relatedTarget === this._previewFrame) {
      // If the preview pane appears over the control that triggers the preview, try scrolling
      // the contro up into the center of the view so that it can still be seen
      event.target.scrollIntoView({ block: 'center' })
      return
    }

    this.root.querySelectorAll('[data-iaa-section-control-hover]').forEach(e => {
      e.removeAttribute('data-iaa-section-control-hover')
    })

    this.hidePreviewFrame()
  }

  private showPreviewFrame() {
    const root = this.root.closest('.i7n-wrapper') || this.root
    const rootRect = root.getBoundingClientRect()
    this.hidePreviewFrame()
    this._previewFrame = document.createElement('iframe')
    this._previewFrame.classList.add('iaa-preview-frame')
    this._previewFrame.src = "about:blank"
    this._previewFrame.style.display = 'block'
    this._previewFrame.style.left = `${rootRect.left}px`
    this._previewFrame.style.width = `${rootRect.width}px`
    this._previewFrame.addEventListener('mouseleave', event => this.removeSectionHighight(event))
    root.appendChild(this._previewFrame);

    // Give browser time to insert the IFrame - inserting an IFrame causes it to reload. If we
    // add the content to early, the browser may erase it. In particular Safari and Firefox 
    // have this problem while in Chrome it seems the IFrame append is handled instantaneously
    // so that in princple we wouldn't have needed the timeout.
    this.previewRenderTimeout = window.setTimeout(() => this.renderPreviewFrame())
  }

  private renderPreviewFrame() {
    this.previewRenderTimeout = undefined

    if (!this._previewFrame) return

    const doc = this._previewFrame.contentDocument
    if (!doc) return

    document.head.querySelectorAll('link[rel="stylesheet"]').forEach(s => {
      doc.head.appendChild(s.cloneNode(true))
    })
    const root = this.root.closest('.i7n-wrapper') || this.root
    const copy = root.cloneNode(true) as HTMLElement
    if (!doc.body) {
      // Fix for browsers like Firefox, which do not create a body element by default
      const body = doc.createElement('body');
      doc.documentElement ? doc.documentElement.appendChild(body) : doc.appendChild(body);
    }
    doc.body.appendChild(copy)
    copy.querySelectorAll(".iaa-preview-frame, .iaa-section-control, " + 
      ".iaa-visible-annotations-panel, .iaa-visible-annotations-panel-spacer").forEach(n => n.remove())

    const block = this._previewFrame.contentDocument?.querySelector('[data-iaa-section-control-hover]')
    if (!block) return

    this.scrollPreviewFrame(block)
  }

  private scrollPreviewFrame(block: Element) {
    const scrollMarker = document.createElement('div')
    scrollMarker.classList.add('scroll-marker')
    scrollMarker.style.width = '0px'
    scrollMarker.style.height = '0px'
    block.after(scrollMarker)
    this.previewScrollTimeout = window.setTimeout(() => {
      this.previewScrollTimeout = undefined
      this.scrollIntoView(scrollMarker)
    })
  }

  private hidePreviewFrame() {
    if (this.previewRenderTimeout) {
      window.clearTimeout(this.previewRenderTimeout)
      this.previewRenderTimeout = undefined
    }
    if (this.previewScrollTimeout) {
      window.clearTimeout(this.previewScrollTimeout)
      this.previewScrollTimeout = undefined
    }
    this._previewFrame?.remove()
    this._previewFrame = undefined
  }

  // element.scrollIntoView may stop at the wrong location for larger documents, so we need to
  // retry if the target element is not yet visible
  private scrollIntoView(scrollMarker: HTMLElement) {
    const viewport = this._previewFrame?.contentWindow
    if (!viewport) return

    function checkVisibility() {
      if (!viewport) return
      const rect = scrollMarker.getBoundingClientRect();
      const visible = rect.top >= 0 && rect.left >= 0 && rect.bottom <= viewport.innerHeight && rect.right <= viewport.innerWidth

      // If the element is not visible, schedule another check with setTimeout
      if (!visible) {
        // scrollIntoView({block: 'end'}) is inaccurate for large documents. What seems to work 
        // better is placing a marker element after the block end and then scroll to that using
        // scrollIntoView({block: 'center'})
        scrollMarker.scrollIntoView({ block: 'center'});
        setTimeout(() => checkVisibility(), 500);
      }
      else {
        console.log("Scrolling done")
        // scrollMarker.remove()
      }
    }

    // Call the checkVisibility function
    checkVisibility();
  }

  private getSectionForControl(element: Element): Element | null {
    const sectionId = element.getAttribute('data-iaa-applies-to')
    if (!sectionId) return null

    const section = this.root.querySelector(`[id="${sectionId}"]`)
    if (!section) {
      console.warn(`Cannot find element for section [${sectionId}]`)
      return null
    }

    return section
  }
}
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
import AbstractAnnotation from './pdfanno/core/src/model/AbstractAnnotation'
import type { AnnotationEditor, DiamAjax, Offsets } from '@inception-project/inception-js-api'
import './PdfAnnotationEditor.scss'
import { initPdfAnno, getAnnotations as doLoadAnnotations, scrollTo, destroy as destroyPdfAnno } from './pdfanno/pdfanno'

export class PdfAnnotationEditor implements AnnotationEditor {
  private ajax: DiamAjax
  private root: Element

  public constructor (element: Element, ajax: DiamAjax) {
    this.ajax = ajax
    this.root = element

    // Prevent right-click from triggering a selection event
    this.root.addEventListener('mousedown', e => this.cancelRightClick(e), { capture: true })
    this.root.addEventListener('mouseup', e => this.cancelRightClick(e), { capture: true })
    this.root.addEventListener('mouseclick', e => this.cancelRightClick(e), { capture: true })

    this.root.addEventListener('annotationSelected', ev => this.onAnnotationSelected(ev))
    this.root.addEventListener('createSpanAnnotation', ev => this.onCreateSpanAnnotation(ev))
    this.root.addEventListener('createRelationAnnotation', ev => this.onCreateRelationAnnotation(ev))
    this.root.addEventListener('doubleClickAnnotation', ev => this.onDoubleClickAnnotation(ev))
    this.root.addEventListener('openContextMenu', ev => this.onOpenContextMenu(ev))
  }

  async init (): Promise<void> {
    return initPdfAnno(this.ajax).then(() => console.info('PdfAnnotationEditor reporting for duty!'))
  }

  loadAnnotations (): void {
    doLoadAnnotations()
  }

  scrollTo (args: { offset: number, position?: string, pingRanges?: Offsets[] }): void {
    // console.log(`SCROLLING! ${args.offset} ${args.position}`)
    scrollTo(args)
  }

  private cancelRightClick (e: Event): void {
    if (e instanceof MouseEvent) {
      if (e.button === 2) {
        console.log("cancelled")
        e.preventDefault()
        e.stopPropagation()
      }
    }
  }

  onOpenContextMenu (ev: Event) {
    if (ev instanceof CustomEvent) {
      const ann = ev.detail.ann as AbstractAnnotation
      const oev = ev.detail.originalEvent

      if (!(oev instanceof MouseEvent) || !(oev.target instanceof Node)) return

      if (ann.vid) this.ajax.openContextMenu(ann.vid, oev)
    }
  }

  onAnnotationSelected (ev: Event) {
    if (ev instanceof CustomEvent) {
      const ann = ev.detail as AbstractAnnotation
      if (ann.vid) {
        this.ajax.selectAnnotation(ann.vid)
      }
    }
  }

  onCreateSpanAnnotation (ev: Event) {
    if (ev instanceof CustomEvent) {
      const { begin, end } = ev.detail
      this.ajax.createSpanAnnotation([[begin, end]])
    }
  }

  onDoubleClickAnnotation (ev: Event) {
    if (ev instanceof CustomEvent) {
      const ann = ev.detail as AbstractAnnotation
      if (ann.vid) {
        this.ajax.triggerExtensionAction(ann.vid)
      }
    }
  }

  onCreateRelationAnnotation (ev: Event) {
    if (ev instanceof CustomEvent) {
      const { origin, target, originalEvent } = ev.detail
      this.ajax.createRelationAnnotation(origin, target, originalEvent)
    }
  }

  destroy (): void {
    destroyPdfAnno()
  }
}

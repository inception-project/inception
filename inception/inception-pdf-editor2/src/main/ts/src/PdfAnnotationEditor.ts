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
import type { AnnotationEditor, DiamAjax } from '@inception-project/inception-js-api'
import './PdfAnnotationEditor.css'
import { initPdfAnno, getAnnotations as doLoadAnnotations, scrollTo } from './pdfanno/pdfanno'
import AbstractAnnotation from './pdfanno/core/src/annotation/abstract'

export class PdfAnnotationEditor implements AnnotationEditor {
  private ajax: DiamAjax
  private root: Element

  public constructor (element: Element, ajax: DiamAjax) {
    this.ajax = ajax
    this.root = element

    element.addEventListener('annotationSelected', ev => this.onAnnotationSelected(ev))
    element.addEventListener('createSpanAnnotation', ev => this.onCreateSpanAnnotation(ev))
    element.addEventListener('createRelationAnnotation', ev => this.onCreateRelationAnnotation(ev))
    element.addEventListener('doubleClickAnnotation', ev => this.onDoubleClickAnnotation(ev))
  }

  async init (): Promise<void> {
    return initPdfAnno(this.ajax).then(() => console.info('PdfAnnotationEditor reporting for duty!'))
  }

  loadAnnotations (): void {
    doLoadAnnotations()
  }

  jumpTo (args: { offset: number; position: string }): void {
    console.log(`JUMPING! ${args.offset} ${args.position}`)
    scrollTo(args.offset, args.position)
  }

  onAnnotationSelected (ev: Event) {
    if (ev instanceof CustomEvent) {
      const ann = ev.detail as AbstractAnnotation
      this.ajax.selectAnnotation(ann.vid)
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
      this.ajax.triggerExtensionAction(ann.vid)
    }
  }

  onCreateRelationAnnotation (ev: Event) {
    if (ev instanceof CustomEvent) {
      const { origin, target } = ev.detail
      this.ajax.createRelationAnnotation(origin, target)
    }
  }

  destroy (): void {
    console.log('Destroy not implemented yet')
  }
}

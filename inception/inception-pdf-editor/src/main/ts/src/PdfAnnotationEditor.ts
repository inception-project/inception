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
import { initPdfAnno, getAnnotations as doLoadAnnotations } from './pdfanno/pdfanno'
import AbstractAnnotation from './pdfanno/core/src/annotation/abstract'
import SpanAnnotation from './pdfanno/core/src/annotation/span'

export class PdfAnnotationEditor implements AnnotationEditor {
  private ajax: DiamAjax
  private root: Element

  public constructor (element: Element, ajax: DiamAjax) {
    this.ajax = ajax
    this.root = element

    console.info('PdfAnnotationEditor reporting for duty!')

    initPdfAnno()

    element.addEventListener('annotationSelected', ev => this.onAnnotationSelected(ev))
    element.addEventListener('createSpanAnnotation', ev => this.onCreateSpanAnnotation(ev))
    element.addEventListener('createRelationAnnotation', ev => this.onCreateRelationAnnotation(ev))
    element.addEventListener('doubleClickAnnotation', ev => this.onDoubleClickAnnotation(ev))
  }

  loadAnnotations (): void {
    doLoadAnnotations()
  }

  onAnnotationSelected (ev: CustomEvent) {
    const ann = ev.detail as AbstractAnnotation
    if (!ann.selected) {
      return
    }

    this.ajax.selectAnnotation(ann.uuid)
  }

  onCreateSpanAnnotation (ev: CustomEvent) {
    const { begin, end } = ev.detail

    // FIXME: Cannot use DIAM here because the server side needs to perform an offset conversion
    // from the PDF offsets to the backend offsets
    // this.ajax.createSpanAnnotation([[begin, end]]);

    const data = {
      action: 'createSpan',
      begin,
      end
    }
    parent.Wicket.Ajax.ajax({
      m: 'POST',
      ep: data,
      u: window.apiUrl
    })
  }

  onDoubleClickAnnotation (ev: CustomEvent) {
    const ann = ev.detail as AbstractAnnotation

    this.ajax.triggerExtensionAction(ann.uuid)
  }

  onCreateRelationAnnotation (ev: CustomEvent) {
    const { origin, target } = ev.detail

    this.ajax.createRelationAnnotation(origin, target)
  }

  destroy (): void {
    console.log('Destroy not implemented yet')
  }
}

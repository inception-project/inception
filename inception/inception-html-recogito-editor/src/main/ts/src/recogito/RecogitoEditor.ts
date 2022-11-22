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
import '@recogito/recogito-js/dist/recogito.min.css'
import { Recogito } from '@recogito/recogito-js/src'
import Connections from '@recogito/recogito-connections/src'
import { AnnotationEditor, CompactAnnotatedText, CompactSpan, DiamAjax, VID } from '@inception-project/inception-js-api'
import { CompactRelation } from '@inception-project/inception-js-api/src/model/compact/CompactRelation'
import './RecogitoEditor.scss'
import { DiamLoadAnnotationsOptions } from '@inception-project/inception-js-api/src/diam/DiamAjax'
import { ViewportTracker } from '@inception-project/inception-js-api/src/util/ViewportTracker'
import { calculateStartOffset, offsetToRange } from '@inception-project/inception-js-api/src/util/OffsetUtils'
import convert from 'color-convert'

interface WebAnnotationBodyItem {
  type: string;
  value: string;
  purpose: string;
}

interface WebAnnotationAnnotationTarget {
  id: string;
}

interface WebAnnotationTextPositionSelector {
  selector: {
    start: number;
    end: number;
  }
}

interface WebAnnotation {
  id: string;
  type: string;
  motivation?: string;
  target: WebAnnotationTextPositionSelector | Array<WebAnnotationAnnotationTarget>;
  body: WebAnnotationBodyItem | Array<WebAnnotationBodyItem>;
}

export class RecogitoEditor implements AnnotationEditor {
  private ajax: DiamAjax
  private recogito: Recogito
  private connections: any
  private root: Element
  private annotations: Record<string, WebAnnotation[]> = {}
  private leftView?: Element
  private rightView?: Element
  private leftTracker?: ViewportTracker
  private rightTracker?: ViewportTracker

  public constructor (element: Element, ajax: DiamAjax) {
    this.ajax = ajax
    this.root = element

    this.recogito = new Recogito({
      content: element,
      disableEditor: true,
      mode: 'pre'
    })

    this.recogito.on('createAnnotation', annotation => this.createSpan(annotation))
    this.recogito.on('selectAnnotation', annotation => this.selectAnnotation(annotation))

    element.addEventListener('contextmenu', e => this.openContextMenu(e))
    // Prevent right-click from triggering a selection event
    element.addEventListener('mousedown', e => this.cancelRightClick(e), { capture: true })
    element.addEventListener('mouseup', e => this.cancelRightClick(e), { capture: true })
    element.addEventListener('mouseclick', e => this.cancelRightClick(e), { capture: true })

    this.connections = Connections(this.recogito, { disableEditor: true, showLabels: true })
    this.connections.canvas.on('createConnection', annotation => this.createRelation(annotation))
    this.connections.canvas.on('selectConnection', annotation => this.selectAnnotation(annotation))
    // this.recogito.on('updateConnection', annotation => this.createAnnotation(annotation))
    // this.recogito.on('deleteConnection', annotation => this.createAnnotation(annotation))

    this.installColorRenderingPatch(this.recogito)

    this.leftView = element.querySelector('.view-left') || undefined
    if (this.leftView) {
      this.leftTracker = new ViewportTracker(this.leftView, () => this.loadAnnotations())
    }

    this.rightView = element.querySelector('.view-right') || undefined
    if (this.rightView) {
      this.rightTracker = new ViewportTracker(this.rightView, () => this.loadAnnotations())
    }
  }

  /**
   * Recogito does not support rendering annotations with a custom color. This is a workaround.
   */
  private installColorRenderingPatch (recogito: Recogito) {
    const _setAnnotations = recogito.setAnnotations
    recogito.setAnnotations = annotations => {
      // Set annotations on instance first
      return _setAnnotations(annotations).then(() => {
        for (const annotation of annotations) {
          for (const element of this.root.querySelectorAll(`[data-id="${annotation.id}"]`)) {
            const c = convert.hex.rgb(annotation.body.color)

            // Span annotation
            if (element instanceof HTMLElement) {
              element.style.backgroundColor = `rgba(${c[0]}, ${c[1]}, ${c[2]}, 0.2)`
              element.style.borderBottomColor = annotation.body.color
            }

            // Relation annotation
            if (element instanceof SVGElement) {
              element.querySelectorAll('.r6o-connections-edge-path-inner').forEach(path => {
                if (path instanceof SVGElement) {
                  path.style.stroke = annotation.body.color
                }
              })
            }
          }
        }
      })
    }
  }

  /**
   * Prevent right click from triggering a selection event.
   */
  private cancelRightClick (e: Event): void {
    if (e instanceof MouseEvent) {
      if (e.button === 2) {
        e.preventDefault()
        e.stopPropagation()
      }
    }
  }

  private openContextMenu (e: Event): void {
    if (!(e instanceof MouseEvent) || !(e.target instanceof Element)) {
      return
    }

    const target = e.target as Element
    const annotationSpan = target.closest('.r6o-annotation')

    if (!annotationSpan || !annotationSpan.getAttribute('data-id')) {
      return
    }

    // The RecogitoJS annotation IDs start with a hash `#` which we need to remove
    const annotationId = annotationSpan.getAttribute('data-id')?.substring(1) as VID

    this.ajax.openContextMenu(annotationId, e)
    e.preventDefault()
    e.stopPropagation()
  }

  public loadAnnotations (): void {
    console.log('loadAnnotations')
    Promise.all([
      this.loadView(this.rightView, this.rightTracker?.currentRange),
      this.loadView(this.leftView, this.leftTracker?.currentRange)
    ]).then(() => {
      this.renderDocument()
    })
  }

  public loadView (view?: Element, range? : [number, number]): Promise<void> {
    return new Promise((resolve, reject) => {
      if (!view || !range) {
        resolve()
        return
      }

      const offset = calculateStartOffset(this.root, view)
      range = [range[0] + offset, range[1] + offset]

      const options: DiamLoadAnnotationsOptions = {
        range,
        includeText: false
      }

      this.ajax.loadAnnotations(options)
        .then((doc: CompactAnnotatedText) => this.convertAnnotations(doc, view || this.root))
        .then(() => resolve())
    })
  }

  private renderDocument (): void {
    console.log('renderDocument')

    if (!this.recogito) {
      console.error('It seems RecogitoJS has not yet been initialized', this)
      return
    }

    const allAnnotations: Array<WebAnnotation> = []
    for (const key in this.annotations) {
      allAnnotations.push(...this.annotations[key])
    }

    console.info(`Rendering ${allAnnotations.length} annotations`)

    // Workaround for https://github.com/recogito/recogito-connections/issues/16
    for (const connection of this.connections.canvas.connections) {
      connection.remove()
    }
    this.connections.canvas.connections = []

    this.recogito.setAnnotations(allAnnotations)
  }

  private convertAnnotations (doc: CompactAnnotatedText, view: Element) {
    const webAnnotations: Array<WebAnnotation> = []

    if (doc.spans) {
      webAnnotations.push(...this.compactSpansToWebAnnotation(doc))
    }

    if (doc.relations) {
      webAnnotations.push(...this.compactRelationsToWebAnnotation(doc))
    }

    const viewId = view.classList.contains('view-left') ? 'left' : 'right'
    this.annotations[viewId] = webAnnotations

    console.info(`Loaded ${webAnnotations.length} annotations from server (${doc.spans?.length || 0} spans and ${doc.relations?.length || 0} relations)`)
  }

  private compactSpansToWebAnnotation (doc: CompactAnnotatedText): Array<WebAnnotation> {
    const offset = doc.window[0]
    const spans = doc.spans as Array<CompactSpan>
    return spans.map(span => {
      // console.log(`From ${span[1][0][0]}-${span[1][0][1]} +${offset}`, this.root)
      return {
        id: '#' + span[0],
        type: 'Annotation',
        body: {
          type: 'TextualBody',
          purpose: 'tagging',
          color: span[2]?.c || '#000000',
          value: span[2]?.l || ''
        },
        target: {
          selector: { type: 'TextPositionSelector', start: offset + span[1][0][0], end: offset + span[1][0][1] }
        }
      }
    })
  }

  private compactRelationsToWebAnnotation (doc: CompactAnnotatedText): Array<WebAnnotation> {
    const relations = doc.relations as Array<CompactRelation>
    return relations.map(relation => {
      return {
        id: '#' + relation[0],
        type: 'Annotation',
        body: {
          type: 'TextualBody',
          purpose: 'tagging',
          color: relation[2]?.c || '#000000',
          value: relation[2]?.l || ''
        },
        motivation: 'linking',
        target: [
          { id: '#' + relation[1][0][0] },
          { id: '#' + relation[1][1][0] }
        ]
      }
    })
  }

  public destroy (): void {
    this.connections.destroy()
    this.recogito.destroy()
  }

  private createSpan (annotation): void {
    const target = annotation.target

    for (let i = 0; i < target.selector.length; i++) {
      if (target.selector[i].type === 'TextPositionSelector') {
        const begin = target.selector[i].start
        const end = target.selector[i].end

        this.ajax.createSpanAnnotation([[begin, end]])
        return
      }
    }
  }

  private createRelation (annotation): void {
    const target = annotation.target

    // The RecogitoJS annotation IDs start with a hash `#` which we need to remove
    const sourceId = target[0].id?.substring(1) as VID
    const targetId = target[1].id?.substring(1) as VID

    this.ajax.createRelationAnnotation(sourceId, targetId)
  }

  private selectAnnotation (annotation): void {
    // The RecogitoJS annotation IDs start with a hash `#` which we need to remove
    this.ajax.selectAnnotation(annotation.id.substring('1'))
  }

  scrollTo (args: { offset: number; position: string; }): void {
    console.log('Implement scrollTo')
    const range = offsetToRange(this.root, args.offset, args.offset)
    if (!range) return
    range.startContainer?.parentElement?.scrollIntoView(
      { behavior: 'auto', block: 'center', inline: 'nearest' })
  }
}

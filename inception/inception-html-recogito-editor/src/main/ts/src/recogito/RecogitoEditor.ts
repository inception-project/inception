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
import { AnnotationEditor, CompactAnnotatedText, CompactAnnotationMarker, CompactSpan, DiamAjax, VID } from '@inception-project/inception-js-api'
import { CompactRelation } from '@inception-project/inception-js-api/src/model/compact/CompactRelation'
import './RecogitoEditor.scss'
import { DiamLoadAnnotationsOptions } from '@inception-project/inception-js-api/src/diam/DiamAjax'
import { ViewportTracker } from '@inception-project/inception-js-api/src/util/ViewportTracker'
import { offsetToRange } from '@inception-project/inception-js-api/src/util/OffsetUtils'
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
  private annotations: WebAnnotation[]
  private tracker: ViewportTracker

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

    this.installRenderingPatch(this.recogito)

    this.tracker = new ViewportTracker(this.root, () => this.loadAnnotations())
  }

  /**
   * Recogito does not support rendering annotations with a custom color. This is a workaround.
   */
  private installRenderingPatch (recogito: Recogito) {
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
              annotation.body.classes.forEach(c => element.classList.add(c))
            }

            // Relation annotation
            if (element instanceof SVGElement) {
              annotation.body.classes.forEach(c => element.classList.add(c))
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
    this.loadView(this.root, this.tracker?.currentRange).then(() => this.renderDocument())
  }

  public loadView (view?: Element, range? : [number, number]): Promise<void> {
    return new Promise((resolve, reject) => {
      if (!view || !range) {
        resolve()
        return
      }

      console.log(`loadView(${range})`)

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

    console.info(`Rendering ${this.annotations.length} annotations`)

    // Workaround for https://github.com/recogito/recogito-connections/issues/16
    for (const connection of this.connections.canvas.connections) {
      connection.remove()
    }
    this.connections.canvas.connections = []

    this.recogito.setAnnotations(this.annotations)
  }

  private convertAnnotations (doc: CompactAnnotatedText, view: Element) {
    const webAnnotations: Array<WebAnnotation> = []
    const annotationMarkers = this.makeMarkerMap(doc.annotationMarkers)

    if (doc.spans) {
      webAnnotations.push(...this.compactSpansToWebAnnotation(doc, annotationMarkers))
    }

    if (doc.relations) {
      webAnnotations.push(...this.compactRelationsToWebAnnotation(doc, annotationMarkers))
    }

    this.annotations = webAnnotations

    console.info(`Loaded ${webAnnotations.length} annotations from server (${doc.spans?.length || 0} spans and ${doc.relations?.length || 0} relations)`)
  }

  private compactSpansToWebAnnotation (doc: CompactAnnotatedText, annotationMarkers: Map<VID, Array<CompactAnnotationMarker>>): Array<WebAnnotation> {
    const offset = doc.window[0]
    const spans = doc.spans as Array<CompactSpan>
    return spans.map(span => {
      // console.log(`From ${span[1][0][0]}-${span[1][0][1]} +${offset}`, this.root)

      const classList = ['i7n-highlighted']
      const ms = annotationMarkers.get(span[0]) || []
      ms.forEach(m => classList.push(`i7n-marker-${m[0]}`))

      return {
        id: '#' + span[0],
        type: 'Annotation',
        body: {
          type: 'TextualBody',
          purpose: 'tagging',
          color: span[2]?.c || '#000000',
          value: span[2]?.l || '',
          classes: classList
        },
        target: {
          selector: { type: 'TextPositionSelector', start: offset + span[1][0][0], end: offset + span[1][0][1] }
        }
      }
    })
  }

  private compactRelationsToWebAnnotation (doc: CompactAnnotatedText, annotationMarkers: Map<VID, Array<CompactAnnotationMarker>>): Array<WebAnnotation> {
    const relations = doc.relations as Array<CompactRelation>
    return relations.map(relation => {
      const classList = ['i7n-highlighted']
      const ms = annotationMarkers.get(relation[0]) || []
      ms.forEach(m => classList.push(`i7n-marker-${m[0]}`))

      return {
        id: '#' + relation[0],
        type: 'Annotation',
        body: {
          type: 'TextualBody',
          purpose: 'tagging',
          color: relation[2]?.c || '#000000',
          value: relation[2]?.l || '',
          classes: classList
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

  private makeMarkerMap<T> (markerList: T[] | undefined): Map<VID, Array<T>> {
    const markerMap = new Map<VID, Array<T>>()
    if (markerList) {
      markerList.forEach(marker => {
        marker[1].forEach(vid => {
          let ms = markerMap.get(vid)
          if (!ms) {
            ms = []
            markerMap.set(vid, ms)
          }
          ms.push(marker)
        })
      })
    }
    return markerMap
  }
}

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
import ConnectionsPlugin from '@recogito/recogito-connections/src'
import { AnnotatedText, type AnnotationEditor, AnnotationOutEvent, AnnotationOverEvent, type DiamAjax, type VID, unpackCompactAnnotatedTextV2 } from '@inception-project/inception-js-api'
import './RecogitoEditor.scss'
import { type DiamLoadAnnotationsOptions } from '@inception-project/inception-js-api/src/diam/DiamAjax'
import { ViewportTracker } from '@inception-project/inception-js-api/src/util/ViewportTracker'
import { offsetToRange } from '@inception-project/inception-js-api/src/util/OffsetUtils'
import convert from 'color-convert'
import { type CompactAnnotatedText } from '@inception-project/inception-js-api/src/model/compact_v2'
import { annotatorState } from './RecogitoEditorState.svelte'
import { mount, unmount } from 'svelte'
import RecogitoEditorToolbar from './RecogitoEditorToolbar.svelte'
import AnnotationDetailPopOver from '@inception-project/inception-js-api/src/widget/AnnotationDetailPopOver.svelte'

export const NO_LABEL = '◌'

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
  private alpha = '55'
  private ajax: DiamAjax
  private recogito: Recogito
  private connections: ConnectionsPlugin
  private root: Element
  private annotations: WebAnnotation[]
  private tracker: ViewportTracker
  private data? : AnnotatedText
  private toolbar: RecogitoEditorToolbar
  private popover: AnnotationDetailPopOver
  private laskMouseMoveEvent: MouseEvent | undefined
  private userPreferencesKey: string

  public constructor (element: Element, ajax: DiamAjax, userPreferencesKey: string) {
    this.ajax = ajax
    this.root = element
    this.userPreferencesKey = userPreferencesKey

    const defaultPreferences = {
      showLabels: false
    }
    let preferences = Object.assign({}, defaultPreferences)

    ajax.loadPreferences(userPreferencesKey).then((p) => {
      preferences = Object.assign(preferences, defaultPreferences, p)
      console.log('Loaded preferences', preferences)

      annotatorState.showLabels = 
          preferences.showLabels ?? defaultPreferences.showLabels;
    }).then(() => {
      this.toolbar = this.createToolbar()

      const wrapper = element.ownerDocument.createElement('div')
      Array.from(element.childNodes).forEach((child) => wrapper.appendChild(child))
      element.appendChild(wrapper)

      this.recogito = new Recogito({
        content: wrapper,
        disableEditor: true,
        mode: 'pre'
      })

      this.recogito.on('createAnnotation', annotation => this.createSpan(annotation))
      this.recogito.on('selectAnnotation', annotation => this.selectAnnotation(annotation))

      element.addEventListener('contextmenu', e => this.openContextMenu(e))
      // Prevent right-click from triggering a selection event
      element.addEventListener('mousemove', e => this.trackMousePosition(e), { capture: true })
      element.addEventListener('mousedown', e => this.cancelRightClick(e), { capture: true })
      element.addEventListener('mouseup', e => this.cancelRightClick(e), { capture: true })
      element.addEventListener('mouseclick', e => this.cancelRightClick(e), { capture: true })

      this.installSpanRenderingPatch(this.recogito)

      this.connections = ConnectionsPlugin(this.recogito, { disableEditor: true, showLabels: annotatorState.showLabels })
      this.connections.canvas.on('createConnection', annotation => this.createRelation(annotation))
      this.connections.canvas.on('selectConnection', annotation => this.selectAnnotation(annotation))
      this.connections.canvas
      // this.recogito.on('updateConnection', annotation => this.createAnnotation(annotation))
      // this.recogito.on('deleteConnection', annotation => this.createAnnotation(annotation))

      this.installRelationRenderingPatch(this.recogito)

      // Event handlers for custom events
      this.root.ownerDocument.body.addEventListener('mouseover', event => {
        if (!(event instanceof MouseEvent) || !(event.target instanceof Element)) return
        const vid = event.target.closest("[data-id]")?.getAttribute('data-id')?.substring(1)
        if (!vid) return
        const annotation = this.data?.getAnnotation(vid)
        if (!annotation) return
        this.root.dispatchEvent(new AnnotationOverEvent(annotation, event))
      })
      this.root.ownerDocument.body.addEventListener('mouseout', event => {
        if (!(event instanceof MouseEvent) || !(event.target instanceof Element)) return
        const vid = event.target.closest("[data-id]")?.getAttribute('data-id')?.substring(1)
        if (!vid) return
        const annotation = this.data?.getAnnotation(vid)
        if (!annotation) return
        this.root.dispatchEvent(new AnnotationOutEvent(annotation, event))
      })

      // Add event handlers for highlighting extent of the annotation the mouse is currently over
      this.root.ownerDocument.body.addEventListener('mouseover', e => this.addAnnotationHighlight(e as MouseEvent))
      this.root.ownerDocument.body.addEventListener('mouseout', e => this.removeAnnotationHighight(e as MouseEvent))

      this.tracker = new ViewportTracker(this.root, () => this.loadAnnotations(), { ignoreSelector: '.r6o-relations-layer' })

      this.popover = mount(AnnotationDetailPopOver, {
        target: this.root.ownerDocument.body,
        props: {
          root: this.root,
          ajax: this.ajax
        }
      })
    })
  }

  private addAnnotationHighlight (event: MouseEvent) {
    if (!(event.target instanceof Element)) return

    const vid = event.target.getAttribute('data-id')?.substring(1)
    if (!vid) return

    this.getHighlightsForAnnotation(vid).forEach(e => e.classList.add('iaa-hover'))
  }

  // eslint-disable-next-line no-undef
  getHighlightsForAnnotation (vid: VID): NodeListOf<Element> {
    return this.root.querySelectorAll(`[data-id="#${vid}"]`)
  }

  private removeAnnotationHighight (event: MouseEvent) {
    if (!(event.target instanceof Element)) return

    this.root.querySelectorAll('.iaa-hover').forEach(e => e.classList.remove('iaa-hover'))
  }

  private createToolbar () {
    // Svelte components are appended to the target element. However, we want the toolbar to come
    // first in the DOM, so we first create a container element and prepend it to the body.
    const toolbarContainer = this.root.ownerDocument.createElement('div')
    toolbarContainer.style.position = 'sticky'
    toolbarContainer.style.top = '0px'
    toolbarContainer.style.zIndex = '10000'
    toolbarContainer.style.backgroundColor = '#fff'
    this.root.ownerDocument.body.insertBefore(toolbarContainer, this.root.ownerDocument.body.firstChild)

    // @ts-ignore - VSCode does not seem to understand the Svelte component
    return mount(RecogitoEditorToolbar, { 
      target: toolbarContainer, 
      props: {
        userPreferencesKey: this.userPreferencesKey,
        ajax: this.ajax
      },
      events: {
        'renderingPreferencesChanged': (e: Event) => { this.loadAnnotations() },
      }
    })
  }

    /**
   * Recogito does not support rendering annotations with a custom color. This is a workaround.
   */
  private installSpanRenderingPatch (recogito: Recogito) {
    const _setAnnotations = recogito.setAnnotations
    recogito.setAnnotations = annotations => {
      // Set annotations on instance first
      return _setAnnotations(annotations).then(() => {
        this.postProcessHighlights(this.root.querySelectorAll(`[data-id]`))

        for (const annotation of annotations) {
          for (const element of this.root.querySelectorAll(`[data-id="${annotation.id}"]`)) {
            const c = convert.hex.rgb(annotation.body.color)

            // Span annotation
            if (element instanceof HTMLElement) {
              const styleList = [
                `--iaa-background-color: ${annotation.body.color || '#000000'}${this.alpha}`,
                `--iaa-border-color: ${annotation.body.color || '#000000'}`
              ]

              element.setAttribute("data-iaa-label", annotation.body.value)
              element.setAttribute("style", styleList.join('; '))
              annotation.body.classes.forEach(c => element.classList.add(c))
            }
          }
        }

        this.removeWhitepaceOnlyHighlights()
      })
    }
  }

  /**
   * Some highlights may only contain whitepace. This method removes such highlights.
   */
    private removeWhitepaceOnlyHighlights (selector: string = '.iaa-highlighted') {
      this.root.querySelectorAll(selector).forEach(e => {
        if (!e.classList.contains('iaa-zero-width') && !e.textContent?.trim()) {
          e.after(...e.childNodes)
          e.remove()
        }
      })
    }
  
  /**
   * Recogito does not support rendering annotations with a custom color. This is a workaround.
   */
  private installRelationRenderingPatch (recogito: Recogito) {
    const _setAnnotations = recogito.setAnnotations
    recogito.setAnnotations = annotations => {
      // Set annotations on instance first
      return _setAnnotations(annotations).then(() => {
        this.postProcessHighlights(this.root.querySelectorAll(`[data-id]`))

        for (const annotation of annotations) {
          for (const element of this.root.querySelectorAll(`[data-id="${annotation.id}"]`)) {
            const c = convert.hex.rgb(annotation.body.color)

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

  private trackMousePosition (e: Event): void {
    if (e instanceof MouseEvent) {
      this.laskMouseMoveEvent = e;
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
        includeText: false,
        clipSpans: true,
        clipArcs: false,
        longArcs: true,
        format: 'compact_v2'
      }

      console.log(`Loading annotations for range ${JSON.stringify(options.range)}`)

      this.ajax.loadAnnotations(options)
        .then((doc: CompactAnnotatedText) => {
          this.data = unpackCompactAnnotatedTextV2(doc)
          this.convertAnnotations(this.data)
        })
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
    this.connections.canvas.config.showLabels = annotatorState.showLabels

    this.recogito.setAnnotations(this.annotations)
  }

  private convertAnnotations (doc: AnnotatedText) {
    const webAnnotations: Array<WebAnnotation> = []

    if (doc.spans) {
      webAnnotations.push(...this.spansToWebAnnotation(doc))
    }

    if (doc.relations) {
      webAnnotations.push(...this.relationsToWebAnnotation(doc))
    }

    this.annotations = webAnnotations

    console.info(`Loaded ${webAnnotations.length} annotations from server (${doc.spans?.size || 0} spans and ${doc.relations?.size || 0} relations)`)
  }

  private spansToWebAnnotation (doc: AnnotatedText): Array<WebAnnotation> {
    return Array.from(doc.spans.values()).map(span => {
      const begin = span.offsets[0][0] + doc.window[0]
      const end = span.offsets[0][1] + doc.window[0]
  
      // console.log(`From ${span[1][0][0]}-${span[1][0][1]} +${offset}`, this.root)

      const classList = ['iaa-highlighted']

      if (begin === end) classList.push('iaa-zero-width')

      const ms = doc.annotationMarkers.get(span.vid) || []
      ms.forEach(m => classList.push(`i7n-marker-${m[0]}`))

      return {
        id: '#' + span.vid,
        type: 'Annotation',
        body: {
          type: 'TextualBody',
          purpose: 'tagging',
          color: span.color || '#000000',
          value: span.label || `[${span.layer.name}]` || NO_LABEL,
          classes: classList
        },
        target: {
          selector: { type: 'TextPositionSelector', start: begin, end: end }
        }
      }
    })
  }

  private postProcessHighlights (elements: NodeListOf<Element>) {
    // Find all the highlights that belong to the same annotation (VID)
    const highlightsByVid = this.groupHighlightsByVid(elements)

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

  /**
   * Groups highlights by their VID.
   *
   * @param highlights list of highlights.
   * @returns groups of highlights by VID.
   */
  // eslint-disable-next-line no-undef
  private groupHighlightsByVid (highlights: NodeListOf<Element>) {
    const spansByVid = new Map<VID, Array<Element>>()
    for (const highlight of Array.from(highlights)) {
      const vid = highlight.getAttribute('data-id')?.substring(1)
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

  private relationsToWebAnnotation (doc: AnnotatedText): Array<WebAnnotation> {
    return Array.from(doc.relations.values()).map(relation => {
      const classList = ['iaa-highlighted']
      const ms = doc.annotationMarkers.get(relation.vid) || []
      ms.forEach(m => classList.push(`i7n-marker-${m[0]}`))

      return {
        id: '#' + relation.vid,
        type: 'Annotation',
        body: {
          type: 'TextualBody',
          purpose: 'tagging',
          color: relation.color || '#000000',
          value: relation.label || `[${relation.layer.name}]` || NO_LABEL,
          classes: classList
        },
        motivation: 'linking',
        target: [
          { id: '#' + relation.arguments[0].targetId },
          { id: '#' + relation.arguments[1].targetId }
        ]
      }
    })
  }

  public destroy (): void {
    if (this.popover) {
      unmount(this.popover)
    }
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
    if (!this.laskMouseMoveEvent) return

    const target = annotation.target

    // The RecogitoJS annotation IDs start with a hash `#` which we need to remove
    const sourceId = target[0].id?.substring(1) as VID
    const targetId = target[1].id?.substring(1) as VID

    this.ajax.createRelationAnnotation(sourceId, targetId, this.laskMouseMoveEvent)
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

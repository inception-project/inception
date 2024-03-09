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
import { AnnotationEditor, DiamAjax, calculateStartOffset } from '@inception-project/inception-js-api'
import { highlights, ApacheAnnotatorVisualizer } from './ApacheAnnotatorVisualizer'
import { ApacheAnnotatorSelector } from './ApacheAnnotatorSelector'
import ApacheAnnotatorToolbar from './ApacheAnnotatorToolbar.svelte'
import { showEmptyHighlights, showLabels } from './ApacheAnnotatorState'
import AnnotationDetailPopOver from '@inception-project/inception-js-api/src/widget/AnnotationDetailPopOver.svelte'
import { Writable } from 'svelte/store'

export class ApacheAnnotatorEditor implements AnnotationEditor {
  private ajax: DiamAjax
  private root: Element
  private vis: ApacheAnnotatorVisualizer
  private selector: ApacheAnnotatorSelector
  private toolbar: ApacheAnnotatorToolbar
  private popover: AnnotationDetailPopOver

  public constructor (element: Element, ajax: DiamAjax, userPreferencesKey: string) {
    this.ajax = ajax
    this.root = element

    const defaultPreferences = {
      showLabels: false,
      showEmptyHighlights: false
    }
    let preferences = Object.assign({}, defaultPreferences)

    ajax.loadPreferences(userPreferencesKey).then((p) => {
      preferences = Object.assign(preferences, defaultPreferences, p)
      console.log('Loaded preferences', preferences)
      let preferencesDebounceTimeout: number | undefined = undefined

      function bindPreference(writable: Writable<any>, propertyName: string) {
        writable.set(
          preferences[propertyName] !== undefined
            ? preferences[propertyName]
            : defaultPreferences[propertyName]
        )

        writable.subscribe((value) => {
          preferences[propertyName] = value
          if (preferencesDebounceTimeout) {
            window.clearTimeout(preferencesDebounceTimeout)
            preferencesDebounceTimeout = undefined
          }
          preferencesDebounceTimeout = window.setTimeout(() => { 
            console.log("Saved preferences")
            ajax.savePreferences(userPreferencesKey, preferences)
          }, 250)
        })
      }

      bindPreference(showLabels, "showLabels")
      bindPreference(showEmptyHighlights, "showEmptyHighlights")
    }).then(() => {
      this.vis = new ApacheAnnotatorVisualizer(this.root, this.ajax)
      this.selector = new ApacheAnnotatorSelector(this.root, this.ajax)
      this.toolbar = this.createToolbar()

      this.popover = new AnnotationDetailPopOver({
        target: this.root.ownerDocument.body,
        props: {
          root: this.root,
          ajax: this.ajax
        }
      })

      // Event handler for creating an annotion or selecting an annotation
      this.root.addEventListener('mouseup', e => this.onMouseUp(e))

      // Event handler for opening the context menu
      this.root.addEventListener('contextmenu', e => this.onRightClick(e))

      // Prevent right-click from triggering a selection event
      this.root.addEventListener('mousedown', e => this.cancelRightClick(e), { capture: true })
      this.root.addEventListener('mouseup', e => this.cancelRightClick(e), { capture: true })
      this.root.addEventListener('mouseclick', e => this.cancelRightClick(e), { capture: true })
    })
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
    return new ApacheAnnotatorToolbar({ target: toolbarContainer, props: {} })
  }

  private cancelRightClick (e: Event): void {
    if (e instanceof MouseEvent) {
      if (e.button === 2) {
        e.preventDefault()
        e.stopPropagation()
      }
    }
  }

  onMouseUp (event: Event): void {
    const sel = window.getSelection()
    if (!sel) return

    if (sel.isCollapsed) {
      if (!this.selector.isVisible()) {
        this.selector.showSelector(event)
      }
      return
    }

    if (!sel.anchorNode || !sel.focusNode) return

    const anchorOffset = calculateStartOffset(this.root, sel.anchorNode) + sel.anchorOffset
    const focusOffset = calculateStartOffset(this.root, sel.focusNode) + sel.focusOffset
    sel.removeAllRanges()

    const begin = Math.min(anchorOffset, focusOffset)
    const end = Math.max(anchorOffset, focusOffset)
    this.ajax.createSpanAnnotation([[begin, end]], '')
  }

  onRightClick (event: Event): void {
    if (!(event instanceof MouseEvent) || !(event.target instanceof Node)) return

    const hls = highlights(event.target)
    if (hls.length === 0) return

    // If the user shift-right-clicks, open the normal browser context menu. This is useful
    // e.g. during debugging / developing
    if (event.shiftKey) return

    if (hls.length === 1) {
      event.preventDefault()
      const vid = hls[0].getAttribute('data-iaa-id')
      if (vid) this.ajax.openContextMenu(vid, event)
    }
  }

  loadAnnotations (): void {
    this.vis?.loadAnnotations()
  }

  scrollTo (args: { offset: number; position: string; }): void {
    this.vis?.scrollTo(args)
  }

  destroy (): void {
    this.vis?.destroy()
    this.selector?.destroy()
  }
}

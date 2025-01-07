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
import { AnnotationEditor, DiamAjax, Offsets, calculateStartOffset } from '@inception-project/inception-js-api'
import { highlights, ApacheAnnotatorVisualizer } from './ApacheAnnotatorVisualizer'
import { ApacheAnnotatorSelector } from './ApacheAnnotatorSelector'
import ApacheAnnotatorToolbar from './ApacheAnnotatorToolbar.svelte'
import { showDocumentStructure, documentStructureWidth, showLabels, showImages, showTables, showEmptyHighlights, showAggregatedLabels } from './ApacheAnnotatorState'
import AnnotationDetailPopOver from '@inception-project/inception-js-api/src/widget/AnnotationDetailPopOver.svelte'
import { Writable } from 'svelte/store'

interface SelectionLike {
  anchorNode: Node | null | undefined
  anchorOffset: number
  focusNode: Node | null | undefined
  focusOffset: number
}

export class ApacheAnnotatorEditor implements AnnotationEditor {
  private ajax: DiamAjax
  private root: Element
  private vis: ApacheAnnotatorVisualizer
  private selector: ApacheAnnotatorSelector
  private toolbar: ApacheAnnotatorToolbar
  private popover: AnnotationDetailPopOver
  private sectionSelector: string
  private horizSplitPane: HTMLElement
  private documentStructureNavigator: Element

  public constructor (element: Element, ajax: DiamAjax, userPreferencesKey: string, sectionElementLocalNames: Set<string>) {
    this.ajax = ajax
    this.root = element
    this.sectionSelector = [...sectionElementLocalNames].join(',')

    const defaultPreferences = {
      showLabels: false,
      showAggregatedLabels: true,
      showEmptyHighlights: false,
      showDocumentStructure: false,
      showImages: true,
      showTables: true,
      documentStructureWidth: 0.2,
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
      bindPreference(showAggregatedLabels, "showAggregatedLabels")
      bindPreference(showEmptyHighlights, "showEmptyHighlights")
      bindPreference(showDocumentStructure, "showDocumentStructure")
      bindPreference(showImages, "showImages")
      bindPreference(showTables, "showTables")
      bindPreference(documentStructureWidth, "documentStructureWidth")
    }).then(() => {
      this.ensureSectionElementsHaveAnId()

      // Move all content into a document container
      const documentContainer = this.root.ownerDocument.createElement('div')
      documentContainer.classList.add('iaa-document-container');
      [...this.root.ownerDocument.body.children].forEach(child => documentContainer.appendChild(child))

      // Set up a container for the document navigation sidebar
      const navigatorContainer = this.root.ownerDocument.createElement('div')
      navigatorContainer.classList.add('iaa-document-navigator');

      // Set up a split pane to host the document and the document structure navigation sidebar
      this.horizSplitPane = this.createHorizontalSplitPane(navigatorContainer, documentContainer)

      // Add the split pane to the document
      this.root.ownerDocument.body.appendChild(this.horizSplitPane)

      // Add the editor components for the document container
      this.vis = new ApacheAnnotatorVisualizer(this.root, this.ajax, this.sectionSelector)
      this.selector = new ApacheAnnotatorSelector(this.root, this.ajax)

      // Add auxiliary controls
      this.documentStructureNavigator = this.createDocumentNavigator(navigatorContainer)
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

      showDocumentStructure.subscribe(enabled => {
        navigatorContainer.style.display = enabled ? 'flex' : 'none'
      })

      showImages.subscribe(enabled => {
        if (enabled) {
          this.root.classList.remove("iaa-hide-images")
        }
        else {
          this.root.classList.add("iaa-hide-images")
        }
      })

      showTables.subscribe(enabled => {
        if (enabled) {
          this.root.classList.remove("iaa-hide-tables")
        }
        else {
          this.root.classList.add("iaa-hide-tables")
        }
      })

      // Delay subscription a bit so the browser has time to render and we can obtain the width
      // of the split pane
      window.setTimeout(() => documentStructureWidth.subscribe(relativeWidth => {
        const totalWidth = this.horizSplitPane.getBoundingClientRect().width
        const width = totalWidth * Math.max(0.1, Math.min(0.9, relativeWidth))
        console.log(`width: ${width} / totalWidth: ${totalWidth}`)
        navigatorContainer.style.width = `${width}px`
        navigatorContainer.style.minWidth = `${width}px`
        navigatorContainer.style.maxWidth = `${width}px`
      }), 10)
    })
  }

  /**
   * Set up a wrapper around the editor content and move the root content node into the wrapper
   * The wrapper creates the opportunity to add the document structure sidebar besides the
   * document content.
   */
  private createHorizontalSplitPane(leftPane: HTMLElement, rightPane: HTMLElement): HTMLElement {
    const divider = this.root.ownerDocument.createElement('div')
    const pane = document.createElement('div')
    pane.classList.add('iaa-split-pane')
    pane.appendChild(leftPane)
    pane.appendChild(divider)
    pane.appendChild(rightPane)

    let origin = 0
    let totalWidth = 0
    let leftStartWidth = 0
    let glass: HTMLElement | undefined = undefined

    divider.classList.add('iaa-divider')
    divider.addEventListener('mousedown', e => { 
      if (e.buttons !== 1) return

      origin = e.clientX
      totalWidth = pane.getBoundingClientRect().width
      leftStartWidth = leftPane.getBoundingClientRect().width
      glass = document.createElement('div')
      glass.classList.add('iaa-glass')
      glass.addEventListener('mouseup', e => {
        if (e.buttons !== 1) return
        glass?.remove()
        glass = undefined
        const width = leftPane.getBoundingClientRect().width
        const relativeWidth = width / totalWidth
        documentStructureWidth.set(relativeWidth)
      })
      glass.addEventListener('mousemove', e => { 
        if (e.buttons !== 1) {
          glass?.remove()
          glass = undefined
          return
        }
  
        const delta = e.clientX - origin
        let relativeWidth =  Math.max(0, Math.min(leftStartWidth + delta, totalWidth)) / totalWidth
        relativeWidth = Math.max(0.1, Math.min(0.9, relativeWidth))
        let leftWidth = relativeWidth * totalWidth
        leftPane.style.width = `${leftWidth}px`
        leftPane.style.minWidth = `${leftWidth}px`
        leftPane.style.maxWidth = `${leftWidth}px`
        documentStructureWidth.set(relativeWidth)
      }, true)
      this.root.ownerDocument.body.appendChild(glass)
    })

    return pane
  }

  private ensureSectionElementsHaveAnId() {
    if (this.sectionSelector) {
      this.root.querySelectorAll(this.sectionSelector).forEach((e, i) => {
        if (!e.id) {
          e.id = `i7n-sec-${i}`
        }
      })
    }
  }

  private createToolbar (): ApacheAnnotatorToolbar {
    // Svelte components are appended to the target element. However, we want the toolbar to come
    // first in the DOM, so we first create a container element and prepend it to the body.
    const toolbarContainer = this.root.ownerDocument.createElement('div')
    toolbarContainer.style.position = 'sticky'
    toolbarContainer.style.top = '0px'
    toolbarContainer.style.zIndex = '10000'
    toolbarContainer.style.backgroundColor = '#fff'
    this.root.ownerDocument.body.insertBefore(toolbarContainer, this.root.ownerDocument.body.firstChild)

    // @ts-ignore - VSCode does not seem to understand the Svelte component
    return new ApacheAnnotatorToolbar({ target: toolbarContainer, props: { sectionSelector: this.sectionSelector } })
  }

  private createDocumentNavigator (target: HTMLElement) {
    return this.root.ownerDocument.createElement('div');
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

  scrollTo (args: { offset: number, position?: string, pingRanges?: Offsets[] }): void {
    this.vis?.scrollTo(args)
  }

  destroy (): void {
    if (this.popover?.$destroy) {
      this.popover.$destroy()
    }
    this.vis?.destroy()
    this.selector?.destroy()
  }
}

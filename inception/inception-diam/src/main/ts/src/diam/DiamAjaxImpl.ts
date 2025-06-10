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
import { Annotation, DiamAjax, Offsets, VID, LazyDetailGroup, DiamAjaxConnectOptions, DiamLoadAnnotationsOptions, DiamSelectAnnotationOptions } from '@inception-project/inception-js-api'

declare const Wicket: any

// type SuccessHandler = (attrs, jqXHR, data, textStatus: string) => void;
// type ErrorHandler = (attrs, jqXHR, errorMessage, textStatus: string) => void;

if (!Object.prototype.hasOwnProperty.call(document, 'DIAM_TRANSPORT_BUFFER')) {
  (document as any).DIAM_TRANSPORT_BUFFER = {}
}

const TRANSPORT_BUFFER: any = (document as any).DIAM_TRANSPORT_BUFFER

export class DiamAjaxImpl implements DiamAjax {
  private ajaxEndpoint: string
  private csrfToken: string

  constructor (options: string | DiamAjaxConnectOptions) {
    if (options instanceof String || typeof options === 'string') {
      this.ajaxEndpoint = options as string
    }
    else {
      this.ajaxEndpoint = options.url
      this.csrfToken = options.csrfToken
    }
  }

  /**
   * @returns if the focus is (probably) in a feature editor
   */
  private static isFocusInFeatureEditor (): boolean {
    return document.activeElement?.closest('.feature-editors-sidebar') != null
  }

  /**
   * When the focus is in a feature editor, then we need to delay our calls to the server to give
   * any "onFocusLost" event time to be processed.
   *
   * @param ajaxCall the AJAX call function to execute
   */
  private static performAjaxCall (ajaxCall): void {
    if (this.isFocusInFeatureEditor()) {
      // Cause the feature editor to commit its value
      if (document.activeElement instanceof HTMLElement) {
        document.activeElement.blur()
      }
      setTimeout(() => ajaxCall(), 50)
      return
    }

    ajaxCall()
  }

  selectAnnotation (id: VID, options?: DiamSelectAnnotationOptions): void {
    DiamAjaxImpl.performAjaxCall(() => {
      Wicket.Ajax.ajax({
        m: 'POST',
        u: this.ajaxEndpoint,
        ep: {
          action: 'selectAnnotation',
          id,
          scrollTo: options?.scrollTo
        }
      })
    })
  }

  scrollTo (args: { docId?: number, id?: VID, offset?: Offsets, offsets?: Array<Offsets> }): void {
    let effectiveOffsets: Array<Offsets> | undefined
    if (args.offset) {
      effectiveOffsets = [args.offset]
    }
    else {
      effectiveOffsets = args.offsets
    }

    DiamAjaxImpl.performAjaxCall(() => {
      Wicket.Ajax.ajax({
        m: 'POST',
        u: this.ajaxEndpoint,
        ep: {
          action: 'scrollTo',
          docId: args.docId,
          id: args.id,
          offsets: JSON.stringify(effectiveOffsets)
        }
      })
    })
  }

  createSpanAnnotation (offsets: Array<Offsets>, spanText?: string): void {
    DiamAjaxImpl.performAjaxCall(() => {
      Wicket.Ajax.ajax({
        m: 'POST',
        u: this.ajaxEndpoint,
        ep: {
          action: 'spanOpenDialog',
          offsets: JSON.stringify(offsets),
          spanText
        }
      })
    })
  }

  moveSpanAnnotation (id: VID, offsets: Array<Offsets>): void {
    DiamAjaxImpl.performAjaxCall(() => {
      Wicket.Ajax.ajax({
        m: 'POST',
        u: this.ajaxEndpoint,
        ep: {
          action: 'moveSpan',
          id,
          offsets: JSON.stringify(offsets)
        }
      })
    })
  }

  createRelationAnnotation (originSpanId: VID, targetSpanId: VID, evt: MouseEvent): void {
    let { clientX, clientY, overlay } = this.calculateClientPosition(evt)

    DiamAjaxImpl.performAjaxCall(() => {
      new Promise<void>((resolve, reject) => {
        Wicket.Ajax.ajax({
          m: 'POST',
          u: this.ajaxEndpoint,
          ep: {
            action: 'arcOpenDialog',
            originSpanId,
            targetSpanId,
            clientX,
            clientY
          },
          sh: [() => {
            resolve()
          }],
          eh: [() => {
            reject(new Error('Error while trying to create relation'))
          }]
        })
      }).then(() => this.closeOverlayWhenContextMenuIsHidden(overlay))
    })
  }

  deleteAnnotation (id: VID): void {
    DiamAjaxImpl.performAjaxCall(() => {
      Wicket.Ajax.ajax({
        m: 'POST',
        u: this.ajaxEndpoint,
        ep: {
          action: 'deleteAnnotation',
          id
        }
      })
    })
  }

  triggerExtensionAction (id: VID): void {
    DiamAjaxImpl.performAjaxCall(() => {
      Wicket.Ajax.ajax({
        m: 'POST',
        u: this.ajaxEndpoint,
        ep: {
          action: 'doAction',
          id
        }
      })
    })
  }

  static newToken (): string {
    return btoa(String.fromCharCode(...window.crypto.getRandomValues(new Uint8Array(16))))
  }

  static storeResult (token: string, result: string) {
    TRANSPORT_BUFFER[token] = JSON.parse(result)
  }

  static clearResult (token: string): any {
    if (Object.prototype.hasOwnProperty.call(TRANSPORT_BUFFER, token)) {
      const result = TRANSPORT_BUFFER[token]
      delete TRANSPORT_BUFFER[token]
      return result
    }

    return undefined
  }

  loadAnnotations (options?: DiamLoadAnnotationsOptions): Promise<any> {
    const token = DiamAjaxImpl.newToken()

    const params: Record<string, any> = {
      action: 'loadAnnotations',
      token
    }

    if (typeof options === 'string' || options instanceof String) {
      // Backwards compatibility with INCEpTION 23.x
      params.format = options
    } else if (options) {
      if (options.includeText === false) {
        params.text = options.includeText
      }

      if (options.longArcs === true) {
        params.longArcs = options.longArcs
      }

      if (options.clipSpans === false) {
        params.clip = options.clipSpans
      }

      if (options.clipArcs === false) {
        params.clipArcs = options.clipArcs
      }

      if (options.format) {
        params.format = options.format
      }

      if (options.range) {
        params.begin = options.range[0]
        params.end = options.range[1]
      }
    }

    return new Promise((resolve, reject) => {
      Wicket.Ajax.ajax({
        m: 'POST',
        u: this.ajaxEndpoint,
        ep: params,
        sh: [() => {
          const result = DiamAjaxImpl.clearResult(token)
          if (result === undefined) {
            reject(new Error('Server did not place result into transport buffer'))
            return
          }

          resolve(result)
        }],
        eh: [() => {
          DiamAjaxImpl.clearResult(token)

          reject(new Error('Unable to load annotation'))
        }]
      })
    })
  }

  loadLazyDetails (idOrAnnotation: VID | Annotation, optionaLayerId?: number): Promise<LazyDetailGroup[]> {
    const token = DiamAjaxImpl.newToken()

    let id : VID
    if (Object.prototype.hasOwnProperty.call(idOrAnnotation, 'vid')) {
      id = (idOrAnnotation as Annotation).vid
    } else {
      id = idOrAnnotation as VID
    }

    let layerId : number
    if (optionaLayerId) {
      layerId = optionaLayerId
    } else {
      layerId = (idOrAnnotation as Annotation).layer.id
    }

    const params: Record<string, any> = {
      action: 'normData',
      token,
      id,
      layerId
    }

    return new Promise((resolve, reject) => {
      Wicket.Ajax.ajax({
        m: 'POST',
        u: this.ajaxEndpoint,
        ep: params,
        sh: [() => {
          const result = DiamAjaxImpl.clearResult(token)
          if (result === undefined) {
            reject(new Error('Server did not place result into transport buffer'))
            return
          }

          const detailGroups : LazyDetailGroup[] = []
          for (const detailGroup of result) {
            const group : LazyDetailGroup = {
              title: detailGroup.title,
              details: []
            }

            for (const detail of detailGroup.details as []) {
              group.details.push({ label: detail[0], value: detail[1] })
            }

            detailGroups.push(group)
          }

          resolve(detailGroups)
        }],
        eh: [() => {
          DiamAjaxImpl.clearResult(token)

          reject(new Error('Unable to load lazy details'))
        }]
      })
    })
  }

  loadPreferences (key: string): Promise<any> {
    const token = DiamAjaxImpl.newToken()

    const params: Record<string, any> = {
      action: 'loadPreferences',
      token,
      key
    }

    return new Promise((resolve, reject) => {
      Wicket.Ajax.ajax({
        m: 'POST',
        u: this.ajaxEndpoint,
        ep: params,
        sh: [() => {
          const result = DiamAjaxImpl.clearResult(token)
          if (result === undefined) {
            reject(new Error('Server did not place result into transport buffer'))
            return
          }

          resolve(result)
        }],
        eh: [() => {
          DiamAjaxImpl.clearResult(token)

          reject(new Error('Unable to load preferences'))
        }]
      })
    })
  }

  savePreferences (key: string, data: Record<string, any>): Promise<void> {
    const token = DiamAjaxImpl.newToken()

    const params: Record<string, any> = {
      action: 'savePreferences',
      token,
      key,
      data: JSON.stringify(data)
    }

    return new Promise<void>((resolve, reject) => {
      Wicket.Ajax.ajax({
        m: 'POST',
        u: this.ajaxEndpoint,
        ep: params,
        sh: [() => {
          DiamAjaxImpl.clearResult(token)
          resolve()
        }],
        eh: [() => {
          DiamAjaxImpl.clearResult(token)

          reject(new Error('Unable to save preferences'))
        }]
      })
    })
  }

  openContextMenu (id: VID, evt: MouseEvent): void {
    let { clientX, clientY, overlay } = this.calculateClientPosition(evt)

    DiamAjaxImpl.performAjaxCall(() => {
      new Promise<void>((resolve, reject) => {
        Wicket.Ajax.ajax({
          m: 'POST',
          u: this.ajaxEndpoint,
          ep: {
            action: 'contextMenu',
            id,
            clientX,
            clientY
          },
          sh: [() => {
            resolve()
          }],
          eh: [() => {
            reject(new Error('Unable to open context menu'))
          }]
        })
      }).then(() => this.closeOverlayWhenContextMenuIsHidden(overlay))
    })
  }

  private calculateClientPosition(evt: MouseEvent | null) : { clientX: number; clientY: number; overlay?: HTMLElement } {
    if (!evt) {
      return { 
        clientX: Math.round(window.innerWidth / 2), 
        clientY: Math.round(window.innerHeight / 2), 
        overlay: undefined };
    }

    let clientX = evt.clientX
    let clientY = evt.clientY

    let overlay: HTMLElement | undefined = undefined

    // If the editor is in an IFrame, we need to adjust the coordinates.
    // We also need to ensure that clicks outside the context menu are not
    // directed to the IFrame content
    if (evt?.target != null && 'ownerDocument' in evt?.target) {
      const target = evt.target as HTMLElement
      const eventContextWindow = target.ownerDocument?.defaultView
      if (window !== eventContextWindow) {
        const frameId = eventContextWindow?.frameElement?.id
        if (frameId) {
          const frame = parent.window.document.getElementById(frameId)
          const rect = frame?.getBoundingClientRect()
          if (rect && frame) {
            clientX += rect.left
            clientY += rect.top

            // The context menu opens outside of the IFrame and if we click anywhere in the IFrame
            // outside of the context menu, it won't close. The overlay hides the contents of the
            // IFrame and ensures that the context menu receives mouse events outside itself and
            // then can close itself
            overlay = this.createOverlay(frame)
          }
        }
      }
    }

    clientX = Math.round(clientX)
    clientY = Math.round(clientY)

    return { clientX, clientY, overlay }
  }

  private createOverlay (frame: HTMLElement): HTMLElement {
    const overlay = document.createElement('div') as HTMLElement
    overlay.style.position = 'absolute'
    overlay.style.top = '0px'
    overlay.style.left = '0px'
    overlay.style.width = `${frame.clientWidth}px`
    overlay.style.height = `${frame.clientHeight}px`
    overlay.style.zIndex = '100' // context menu is 999
    frame.parentElement?.insertBefore(overlay, frame)
    return overlay
  }

  private closeOverlayWhenContextMenuIsHidden (overlay?: HTMLElement): void {
    if (!overlay) {
      return
    }

    const contextMenu = overlay.parentElement?.querySelector('.context-menu') as HTMLElement | null
    if (!contextMenu) {
      return
    }

    // Now we keep an eye on the context menu - when it gets hidden, we also remove the overlay
    const remover = () => {
      if (contextMenu.style.display === 'none') {
        overlay.remove()
      } else {
        setTimeout(remover, 20)
      }
    }
    setTimeout(remover, 20)
  }
}

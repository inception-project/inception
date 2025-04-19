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
import { calculateEndOffset, calculateStartOffset, type DiamAjax, positionToOffset, type VID } from '@inception-project/inception-js-api'
import AnnotationResizeHandle from './AnnotationResizeHandle.svelte'
import { ApacheAnnotatorVisualizer } from './ApacheAnnotatorVisualizer.svelte'
import { mount, unmount } from 'svelte'

export class ResizeManager {
  private visualizer: ApacheAnnotatorVisualizer
  private ajax: DiamAjax

  private id?: VID
  private beginHandle: AnnotationResizeHandle
  private endHandle: AnnotationResizeHandle
  private hideTimer?: number

  // eslint-disable-next-line no-undef
  private mouseOverHandler?: EventListener

  public constructor (visualizer: ApacheAnnotatorVisualizer, ajax: DiamAjax) {
    this.visualizer = visualizer
    this.ajax = ajax

    this.visualizer.root.addEventListener('dragover', e => this.handleDragOver(e))

    this.beginHandle = mount(AnnotationResizeHandle, {
      target: this.visualizer.root,
      props: { position: 'begin' },
      events: {
        'resize-handle-released': e => this.handleResizeHandleReleased(e)
      }
    })

    this.endHandle = mount(AnnotationResizeHandle, {
      target: this.visualizer.root,
      props: { position: 'end' },
      events: { 
        'resize-handle-released': e => this.handleResizeHandleReleased(e)
      } 
    })

    this.hide()
  }

  public destroy() {
    this.hide()
    if (this.beginHandle) {
      unmount(this.beginHandle)
      this.beginHandle = undefined
    }
    if (this.endHandle) {
      unmount(this.endHandle)
      this.endHandle = undefined
    }
  }

  show (id: VID): void {
    if (this.id === id) return

    if (this.id) this.hide()
    this.id = id

    const highlights = Array.from(this.visualizer.getHighlightsForAnnotation(id))
    if (!highlights.length) return
    this.beginHandle.highlight = highlights[0] as HTMLElement
    this.endHandle.highlight = highlights[highlights.length - 1] as HTMLElement

    this.mouseOverHandler = e => this.handleMouseOver(e)
    this.visualizer.root.addEventListener('mouseover', this.mouseOverHandler)
  }

  private handleDragOver (e: Event): void {
    // prevent default to allow drop - any position in the visualizer is a valid drop target
    e.preventDefault()
  }

  private handleResizeHandleReleased (e: Event): void {
    if (!(e instanceof CustomEvent) || !(e.detail?.event instanceof MouseEvent) || !this.id) return

    const position = e.detail.position
    const dragEvent = e.detail.event as DragEvent

    let begin: number
    let end: number
    if (position === 'begin') {
      begin = positionToOffset(this.visualizer.root, dragEvent.clientX, dragEvent.clientY)
      end = calculateEndOffset(this.visualizer.root, this.endHandle?.highlight)
    } else {
      begin = calculateStartOffset(this.visualizer.root, this.beginHandle?.highlight)
      end = positionToOffset(this.visualizer.root, dragEvent.clientX, dragEvent.clientY)
    }

    this.ajax.moveSpanAnnotation(this.id, [[begin, end]])
    this.hide()
  }

  private handleMouseOver (e: Event): void {
    if (!(e instanceof MouseEvent) || !(e.target instanceof Element)) return

    if (this.id === e.target.getAttribute('data-iaa-id')) {
      this.cancelAutoHide()
      return
    }

    if (this.beginHandle?.handle.contains(e.target) || this.endHandle?.handle.contains(e.target)) {
      this.cancelAutoHide()
      return
    }

    this.autoHide()
  }

  cancelAutoHide (): void {
    if (this.hideTimer === undefined) return
    window.clearTimeout(this.hideTimer)
    this.hideTimer = undefined
  }

  autoHide (): void {
    function hideOrRescheduleAutoHide (context: ResizeManager) {
      if (context.beginHandle?.dragging || context.endHandle?.dragging) {
        context.hideTimer = window.setTimeout(() => hideOrRescheduleAutoHide(context), 1000)
      } else {
        context.hide()
      }
    }

    this.cancelAutoHide()
    this.hideTimer = window.setTimeout(() => hideOrRescheduleAutoHide(this), 1000)
  }

  hide (): void {
    this.cancelAutoHide()
    if (this.mouseOverHandler) {
      this.visualizer.root.removeEventListener('mouseover', this.mouseOverHandler)
      this.mouseOverHandler = undefined
    }
    this.id = undefined
    this.beginHandle.highlight = undefined
    this.endHandle.highlight = undefined
  }
}

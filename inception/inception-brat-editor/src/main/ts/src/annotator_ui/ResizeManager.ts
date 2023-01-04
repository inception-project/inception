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
import { DiamAjax, VID, caretRangeFromPoint } from '@inception-project/inception-js-api'
import { Dispatcher } from '../dispatcher/Dispatcher'
import { Visualizer } from '../visualizer/Visualizer'
import AnnotationResizeHandle from './AnnotationResizeHandle.svelte'

export class ResizeManager {
  private visualizer: Visualizer
  private ajax: DiamAjax
  private dispatcher: Dispatcher

  private id?: VID
  private beginHandle: AnnotationResizeHandle
  private endHandle: AnnotationResizeHandle
  private hideTimer?: number

  // eslint-disable-next-line no-undef
  private mouseOverHandler?: EventListener

  public constructor (dispatcher: Dispatcher, visualizer: Visualizer, ajax: DiamAjax) {
    this.visualizer = visualizer
    this.ajax = ajax
    this.dispatcher = dispatcher

    dispatcher.on('doneRendering', this, this.hide)

    // @ts-expect-error - VSCode does not seem to understand the Svelte component
    this.beginHandle = new AnnotationResizeHandle({
      target: this.visualizer.svgContainer,
      props: { position: 'begin' }
    })
    // @ts-expect-error - VSCode does not seem to understand the Svelte component
    this.endHandle = new AnnotationResizeHandle({
      target: this.visualizer.svgContainer,
      props: { position: 'end' }
    })

    // @ts-expect-error - VSCode does not seem to understand the Svelte component
    this.beginHandle.$on('resize-handle-released', e => this.handleResizeHandleReleased(e))

    // @ts-expect-error - VSCode does not seem to understand the Svelte component
    this.endHandle.$on('resize-handle-released', e => this.handleResizeHandleReleased(e))

    this.hide()

    // Event handlers for the resizer component
    this.visualizer.svgContainer.addEventListener('mouseover', e => this.showResizer(e))
  }

  private showResizer (event: Event): void {
    if (!(event instanceof MouseEvent) || !(event.target instanceof Element)) return

    if (this.beginHandle?.dragging || this.endHandle?.dragging) {
      console.debug("Resizer is dragging, don't show resizer")
      return
    }

    const vid = event.target.getAttribute('data-span-id')
    if (vid) this.show(vid)
  }

  show (id: VID): void {
    if (this.id === id) return

    if (this.id) this.hide()
    this.id = id

    const highlights = Array.from(this.visualizer.getHighlightsForSpan(id))
    if (!highlights.length) return
    this.beginHandle.highlight = highlights[0]
    this.endHandle.highlight = highlights[highlights.length - 1]

    this.mouseOverHandler = e => this.handleMouseOver(e)
    this.visualizer.svgContainer.addEventListener('mouseover', this.mouseOverHandler)
  }

  private handleResizeHandleReleased (e: Event): void {
    if (!(e instanceof CustomEvent) || !(e.detail?.event instanceof MouseEvent) || !this.id) return

    const position = e.detail.position
    const dragEvent = e.detail.event as MouseEvent

    let begin : number | null = null
    let end : number | null = null
    if (position === 'begin') {
      const range = caretRangeFromPoint(dragEvent.clientX, dragEvent.clientY)
      begin = this.visualizer.rangeToOffsets(range)?.[0] || null
      const offsets = this.visualizer.data?.spans[this.id]?.offsets
      end = offsets ? offsets[offsets.length - 1][1] : null
    } else {
      const offsets = this.visualizer.data?.spans[this.id]?.offsets
      begin = offsets ? offsets[0][0] : null
      const range = caretRangeFromPoint(dragEvent.clientX, dragEvent.clientY)
      end = this.visualizer.rangeToOffsets(range)?.[0] || null
    }

    if (begin === null || end === null) return

    this.ajax.moveSpanAnnotation(this.id, [[begin, end]])
    this.hide()
  }

  private handleMouseOver (e: Event): void {
    if (!(e instanceof MouseEvent) || !(e.target instanceof Element)) return

    console.debug('ResizeManager.handleMouseOver')

    if (this.id === e.target.getAttribute('data-span-id')) {
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
      this.visualizer.svgContainer.removeEventListener('mouseover', this.mouseOverHandler)
      this.mouseOverHandler = undefined
    }
    this.id = undefined
    if (this.beginHandle) this.beginHandle.highlight = undefined
    if (this.endHandle) this.endHandle.highlight = undefined
  }
}

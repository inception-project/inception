/*
 * ## INCEpTION ##
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
 *
 * ## brat ##
 * Copyright (C) 2010-2012 The brat contributors, all rights reserved.
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of
 * this software and associated documentation files (the "Software"), to deal in
 * the Software without restriction, including without limitation the rights to
 * use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies
 * of the Software, and to permit persons to whom the Software is furnished to do
 * so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */
import { Dispatcher } from '../dispatcher/Dispatcher'
import { Box, Svg, SVGTypeMapping, Point } from '@svgdotjs/svg.js'
import { DocumentData } from '../visualizer/DocumentData'
import { INSTANCE as Configuration } from '../configuration/Configuration'
import { INSTANCE as Util } from '../util/Util'
import { DiamAjax } from '@inception-project/inception-js-api'
import { EntityTypeDto, VID } from '../protocol/Protocol'
import { findClosestChunkElement, Visualizer } from '../visualizer/Visualizer'

export class AnnotatorUI {
  private data: DocumentData

  private arcDragOrigin?: VID
  private arcDragOriginBox: Box
  private arcDragOriginGroup: SVGTypeMapping<SVGGElement>
  private arcDragArc: SVGTypeMapping<SVGPathElement>
  private arcDragJustStarted = false
  private spanDragJustStarted = false
  private dragStartedAt?: MouseEvent & { target: Element }
  private selectionInProgress = false

  private arcOptions?: { type?: string, old_target?: string, origin?: VID, target?: VID }
  private spanTypes: Record<string, EntityTypeDto>
  private selRect: SVGRectElement[] | null = null
  private lastStartRec : DOMRect | null = null
  private lastEndRec : DOMRect | null = null

  private draggedArcHeight = 30

  private svg: Svg
  private dispatcher: Dispatcher

  private svgPosition?: Coordinates

  private clickCount = 0
  private clickTimer: number | null = null
  private CLICK_DELAY = 300

  private ajax: DiamAjax
  private visualizer: Visualizer

  constructor (dispatcher: Dispatcher, visualizer: Visualizer, ajax: DiamAjax) {
    console.debug('Setting up annotator-ui editor...')

    this.dispatcher = dispatcher
    this.svg = visualizer.svg
    this.visualizer = visualizer
    this.ajax = ajax

    dispatcher
      .on('dataReady', this, this.rememberData)
      .on('spanAndAttributeTypesLoaded', this, this.spanAndAttributeTypesLoaded)
      .on('isReloadOkay', this, this.isReloadOkay)
      .on('keydown', this, this.onKeyDown)
      .on('click', this, this.onClick)
      .on('dragstart', this, (evt: MouseEvent) => evt.preventDefault())
      .on('mousedown', this, this.onMouseDown)
      .on('mouseup', this, this.onMouseUp)
      .on('mousemove', this, this.onMouseMove)
      .on('contextmenu', this, this.contextMenu)
  }

  private clearSelection () {
    window.getSelection()?.removeAllRanges()

    if (this.selRect) {
      this.selRect.forEach(rect => rect.remove())
      this.selRect = null
      this.lastStartRec = null
      this.lastEndRec = null
    }
  }

  private makeSelRect (rx: number, ry: number, rw: number, rh: number, col?) {
    const selRect = document.createElementNS('http://www.w3.org/2000/svg', 'rect')
    selRect.setAttributeNS(null, 'width', rw.toString())
    selRect.setAttributeNS(null, 'height', rh.toString())
    selRect.setAttributeNS(null, 'x', rx.toString())
    selRect.setAttributeNS(null, 'y', ry.toString())
    selRect.setAttributeNS(null, 'fill', col === undefined ? 'lightblue' : col)
    return selRect
  }

  private onKeyDown (evt: KeyboardEvent) {
    if (evt.code === 'Escape') {
      this.stopArcDrag()
    }
  }

  /**
   * Distinguish between double clicks and single clicks . This is relevant when clicking on
   * annotations. For clicking on text nodes, this is not really relevant.
   */
  private onClick (evt: MouseEvent) {
    this.clickCount++

    const singleClickAction = Configuration.singleClickEdit
      ? this.selectAnnotation
      : this.handleExtensionAction
    const doubleClickAction = Configuration.singleClickEdit
      ? this.handleExtensionAction
      : this.selectAnnotation

    if (this.clickCount === 1) {
      this.clickTimer = window.setTimeout(() => {
        try {
          singleClickAction.call(this, evt) // perform single-click action
        } finally {
          this.clickCount = 0 // after action performed, reset counter
        }
      }, this.CLICK_DELAY)
    } else {
      if (this.clickTimer !== null) {
        clearTimeout(this.clickTimer) // prevent single-click action
      }
      try {
        doubleClickAction.call(this, evt) // perform double-click action
      } finally {
        this.clickCount = 0 // after action performed, reset counter
      }
    }
  }

  private handleExtensionAction (evt: MouseEvent) : void {
    if (!(evt.target instanceof Element)) return

    const id = evt.target.getAttribute('data-span-id') || evt.target.getAttribute('data-arc-ed')
    if (id) {
      evt.preventDefault()
      this.ajax.triggerExtensionAction(id)
    }
  }

  private selectAnnotation (evt: MouseEvent & { target: Element }) {
    // Prevent selection of annotation while creating an arc
    if (this.arcDragOrigin) return

    if (evt.target.getAttribute('data-arc-role')) {
      this.selectRelation(evt)
      return
    }

    if (evt.target.getAttribute('data-span-id')) {
      this.selectSpan(evt)
    }
  }

  private selectRelation (evt: MouseEvent & { target: Element }) {
    this.clearSelection()
    const originSpanId = evt.target.getAttribute('data-arc-origin')
    const targetSpanId = evt.target.getAttribute('data-arc-target')
    const type = evt.target.getAttribute('data-arc-role')
    this.arcOptions = {
      origin: originSpanId !== null ? originSpanId : undefined,
      target: targetSpanId !== null ? targetSpanId : undefined,
      old_target: targetSpanId !== null ? targetSpanId : undefined,
      type: type !== null ? type : undefined
    }

    const eventDescId = evt.target.getAttribute('data-arc-ed')
    if (eventDescId) {
      this.ajax.selectAnnotation(eventDescId)
    }
  }

  private selectSpan (evt: MouseEvent) {
    if (!(evt.target instanceof Element)) return

    const id = evt.target.getAttribute('data-span-id')

    if (id) {
      this.clearSelection()
      this.ajax.selectAnnotation(id)
    }
  }

  private startArcDrag (originId: string | undefined | null) {
    this.clearSelection()

    if (!originId) return

    const originEntity = this.data.spans[originId]
    if (!originEntity) {
      console.warn(`Unable to find origin entity with id ${originId}`)
      return
    }
    if (!originEntity.headFragment.group) {
      console.warn('Origin entity has no head-fragment group', originEntity)
      return
    }

    this.svg.addClass('unselectable')
    this.svgPosition = $(this.svg.node).offset()
    this.arcDragOrigin = originId
    this.arcDragArc = this.svg.path()
      .fill('none')
      .attr('marker-end', 'url(#drag_arrow)')
      .addClass('drag_stroke')
    this.arcDragOriginGroup = originEntity.headFragment.group
    this.arcDragOriginGroup.addClass('highlight')
    this.arcDragOriginBox = Util.realBBox(originEntity.headFragment)

    this.arcDragJustStarted = true
  }

  private onMouseDown (evt: MouseEvent & { target: Element }) : boolean {
    // When the right mouse button is pressed, it does never constitue the start of a selection
    if (evt.button === 2) return true

    if (!(evt.target instanceof Element)) return true

    // Instead of calling startArcDrag() immediately, we defer this to onMouseMove
    if (this.arcDragOrigin) return true

    // is it arc drag start?
    if (evt.target.getAttribute('data-span-id')) {
      this.dragStartedAt = evt // XXX do we really need the whole evt?
      this.selectionInProgress = true
      this.dispatcher.post('selectionStarted')
      return false
    }

    if (evt.target.getAttribute('data-chunk-id')) {
      this.spanDragJustStarted = true
      this.selectionInProgress = true
      this.dispatcher.post('selectionStarted')
      return true
    }

    return true
  }

  private onMouseMove (evt: MouseEvent) : void {
    if (!evt.buttons && this.selectionInProgress) {
      this.dispatcher.post('selectionEnded')
    }

    if (this.arcDragOrigin && !evt.buttons) {
      // Mouse button was released outside the event scope - cancel arc drag
      this.stopArcDrag()
      this.arcDragJustStarted = false
      return
    }

    if (!this.arcDragOrigin && this.dragStartedAt) {
      // When the user has pressed the mouse button, we monitor the mouse cursor. If the cursor
      // moves more than a certain distance, we start the arc-drag operation. Starting this
      // operation is expensive because figuring out where the arc is to be drawn is requires
      // fetching bounding boxes - and this triggers a blocking/expensive reflow operation in
      // the browser.
      const deltaX = Math.abs(this.dragStartedAt.pageX - evt.pageX)
      const deltaY = Math.abs(this.dragStartedAt.pageY - evt.pageY)
      if (deltaX > 5 || deltaY > 5) {
        this.arcOptions = undefined
        const target = this.dragStartedAt.target
        this.startArcDrag(target.getAttribute('data-span-id'))
      }
    }

    if (this.arcDragOrigin) {
      this.onMouseMoveArcCreation(evt)
    } else {
      this.onMouseMoveSpanSelection(evt)
    }

    this.arcDragJustStarted = false
    this.spanDragJustStarted = false
  }

  private onMouseMoveArcCreation (evt: MouseEvent) : void {
    if (this.arcDragJustStarted) {
      this.initializeArcDragTargets(evt)
    }

    this.clearSelection()
    const mx = evt.pageX - this.svgPosition.left
    const my = evt.pageY - this.svgPosition.top
    const y = Math.min(this.arcDragOriginBox.y, my) - this.draggedArcHeight
    const center = this.arcDragOriginBox.x + this.arcDragOriginBox.width / 2
    const dx = (center - mx) / 4
    this.arcDragArc.plot([
      ['M', center, this.arcDragOriginBox.y],
      ['C', center - dx, y, mx + dx, y, mx, my]
    ])
  }

  private initializeArcDragTargets (evt: MouseEvent) : void {
    if (!this.arcDragOrigin) return

    // show the possible targets
    const span = this.data.spans[this.arcDragOrigin]
    const spanDesc = this.spanTypes[span.type]

    if (!spanDesc || !spanDesc.arcs || spanDesc.arcs.length === 0) {
      return
    }

    const noNumArcType = this.arcOptions && this.arcOptions.type

    spanDesc.arcs.map(possibleArc => {
      if (!((this.arcOptions && possibleArc.type === noNumArcType) || !(this.arcOptions && this.arcOptions.old_target))) {
        return undefined
      }

      if (!possibleArc.targets || possibleArc.targets.length === 0) {
        return undefined
      }

      possibleArc.targets.map(possibleTarget => {
        this.svg.find('.span_' + possibleTarget)
          // When shift is pressed when the drag starts, then a self-referencing edge is allowed
          .filter(e => evt.shiftKey || !e.hasClass(`[data-span-id="${this.arcDragOrigin}"]`))
          .map(e => e.addClass('reselectTarget'))
        return undefined
      })

      return undefined
    })
  }

  private onMouseMoveSpanSelection (evt: MouseEvent) : void {
    if (this.spanDragJustStarted) {
      // If user starts selecting text, suppress all pointer events on annotations to
      // avoid the selection jumping around. During selection, we don't need the annotations
      // to react on mouse events anyway.
      this.svg.find('.row, .sentnum').map(e => e.attr('pointer-events', 'none'))
    }

    // A. Scerri FireFox chunk

    // if not, then is it span selection? (ctrl key cancels)
    const sel = window.getSelection()
    if (sel === null) return

    // let chunkIndexFrom = sel.anchorNode && $(sel.anchorNode.parentNode).attr('data-chunk-id')
    // let chunkIndexTo = sel.focusNode && $(sel.focusNode.parentNode).attr('data-chunk-id')
    let chunkIndexFrom = findClosestChunkElement(sel.anchorNode)?.getAttribute('data-chunk-id')
    let chunkIndexTo = findClosestChunkElement(sel.focusNode)?.getAttribute('data-chunk-id')
    // fallback for firefox (at least):
    // it's unclear why, but for firefox the anchor and focus node parents are always undefined,
    // the the anchor and focus nodes themselves do (often) have the necessary chunk ID. However,
    // anchor offsets are almost always wrong, so we'll just make a guess at what the user might
    // be interested in tagging instead of using what's given.
    let anchorOffset = -1
    let focusOffset = -1
    let sp: Point
    let startsAt: SVGTextContentElement
    if (chunkIndexFrom === undefined && chunkIndexTo === undefined &&
      (sel.anchorNode instanceof Element) && (sel.focusNode instanceof Element) &&
      sel.anchorNode.getAttribute('data-chunk-id') &&
      sel.focusNode.getAttribute('data-chunk-id')) {
      // Lets take the actual selection range and work with that
      // Note for visual line up and more accurate positions a vertical offset of 8 and horizontal of 2 has been used!
      const range = sel.getRangeAt(0)
      const svgOffset = $(this.svg.node).offset()
      let flip = false
      let tries = 0
      // First try and match the start offset with a position, if not try it against the other end
      while (tries < 2) {
        sp = this.svg.point((flip ? evt.pageX : this.dragStartedAt.pageX) - svgOffset.left,
          (flip ? evt.pageY : this.dragStartedAt.pageY) - (svgOffset.top + 8))
        startsAt = range.startContainer as SVGTextContentElement
        anchorOffset = startsAt.getCharNumAtPosition(sp)
        chunkIndexFrom = startsAt && $(startsAt).attr('data-chunk-id')
        if (anchorOffset !== -1) {
          break
        }
        flip = true
        tries++
      }

      // Now grab the end offset
      sp.x = (flip ? this.dragStartedAt.pageX : evt.pageX) - svgOffset.left
      sp.y = (flip ? this.dragStartedAt.pageY : evt.pageY) - (svgOffset.top + 8)
      const endsAt = range.endContainer as SVGTextContentElement
      focusOffset = endsAt.getCharNumAtPosition(sp)

      // If we cannot get a start and end offset stop here
      if (anchorOffset === -1 || focusOffset === -1) {
        return
      }

      // If we are in the same container it does the selection back to front when dragged right to left, across different containers the start is the start and the end if the end!
      if (range.startContainer === range.endContainer && anchorOffset > focusOffset) {
        const t = anchorOffset
        anchorOffset = focusOffset
        focusOffset = t
        flip = false
      }
      chunkIndexTo = endsAt && endsAt.getAttribute('data-chunk-id')

      // Now take the start and end character rectangles
      const startRec = startsAt.getExtentOfChar(anchorOffset)
      startRec.y += 2
      const endRec = endsAt.getExtentOfChar(focusOffset)
      endRec.y += 2

      // If nothing has changed then stop here
      if (this.lastStartRec != null && this.lastStartRec.x === startRec.x && this.lastStartRec.y == startRec.y && this.lastEndRec != null && this.lastEndRec.x == endRec.x && this.lastEndRec.y == endRec.y) {
        return
      }

      if (this.selRect == null) {
        let rx = startRec.x
        const ry = startRec.y
        let rw = (endRec.x + endRec.width) - startRec.x
        if (rw < 0) {
          rx += rw
          rw = -rw
        }
        const rh = Math.max(startRec.height, endRec.height)

        this.selRect = []
        const activeSelRect = this.makeSelRect(rx, ry, rw, rh)
        this.selRect.push(activeSelRect)
        startsAt.parentNode.parentNode.parentNode.insertBefore(activeSelRect, startsAt.parentNode.parentNode)
      } else {
        if (startRec.x != this.lastStartRec.x && endRec.x != this.lastEndRec.x && (startRec.y != this.lastStartRec.y || endRec.y != this.lastEndRec.y)) {
          if (startRec.y < this.lastStartRec.y) {
            this.selRect[0].setAttributeNS(null, 'width', this.lastStartRec.width)
            this.lastEndRec = this.lastStartRec
          } else if (endRec.y > this.lastEndRec.y) {
            this.selRect[this.selRect.length - 1].setAttributeNS(null, 'x',
              parseFloat(this.selRect[this.selRect.length - 1].getAttributeNS(null, 'x')) +
              parseFloat(this.selRect[this.selRect.length - 1].getAttributeNS(null, 'width')) -
              this.lastEndRec.width)
            this.selRect[this.selRect.length - 1].setAttributeNS(null, 'width', 0)
            this.lastStartRec = this.lastEndRec
          }
        }

        // Start has moved
        const flip = !(startRec.x === this.lastStartRec?.x && startRec.y === this.lastStartRec.y)
        // If the height of the start or end changed we need to check whether
        // to remove multi line highlights no longer needed if the user went back towards their start line
        // and whether to create new ones if we moved to a newline
        if (((endRec.y !== this.lastEndRec?.y)) || ((startRec.y !== this.lastStartRec?.y))) {
          // First check if we have to remove the first highlights because we are moving towards the end on a different line
          let ss = 0
          for (; ss !== this.selRect.length; ss++) {
            if (startRec.y <= parseFloat(this.selRect[ss].getAttributeNS(null, 'y'))) {
              break
            }
          }
          // Next check for any end highlights if we are moving towards the start on a different line
          let es = this.selRect.length - 1
          for (; es !== -1; es--) {
            if (endRec.y >= parseFloat(this.selRect[es].getAttributeNS(null, 'y'))) {
              break
            }
          }
          // TODO put this in loops above, for efficiency the array slicing could be done separate still in single call
          let trunc = false
          if (ss < this.selRect.length) {
            for (let s2 = 0; s2 !== ss; s2++) {
              this.selRect[s2].remove()
              es--
              trunc = true
            }
            this.selRect = this.selRect.slice(ss)
          }
          if (es > -1) {
            for (let s2 = this.selRect.length - 1; s2 !== es; s2--) {
              this.selRect[s2].remove()
              trunc = true
            }
            this.selRect = this.selRect.slice(0, es + 1)
          }

          // If we have truncated the highlights we need to readjust the last one
          if (trunc) {
            const activeSelRect = flip ? this.selRect[0] : this.selRect[this.selRect.length - 1]
            if (flip) {
              let rw = 0
              if (startRec.y === endRec.y) {
                rw = (endRec.x + endRec.width) - startRec.x
              } else {
                rw = (parseFloat(activeSelRect.getAttributeNS(null, 'x')) +
                  parseFloat(activeSelRect.getAttributeNS(null, 'width'))) -
                  startRec.x
              }
              activeSelRect.setAttributeNS(null, 'x', startRec.x)
              activeSelRect.setAttributeNS(null, 'y', startRec.y)
              activeSelRect.setAttributeNS(null, 'width', rw.toString())
            } else {
              const rw = (endRec.x + endRec.width) - parseFloat(activeSelRect.getAttributeNS(null, 'x'))
              activeSelRect.setAttributeNS(null, 'width', rw.toString())
            }
          } else {
            // We didn't truncate anything but we have moved to a new line so we need to create a new highlight
            const lastSel = flip ? this.selRect[0] : this.selRect[this.selRect.length - 1]
            const startBox = (startsAt.parentNode as SVGGraphicsElement).getBBox()
            const endBox = (endsAt.parentNode as SVGGraphicsElement).getBBox()

            if (flip) {
              lastSel.setAttributeNS(null, 'width',
                (parseFloat(lastSel.getAttributeNS(null, 'x')) +
                  parseFloat(lastSel.getAttributeNS(null, 'width'))) -
                endBox.x)
              lastSel.setAttributeNS(null, 'x', endBox.x)
            } else {
              lastSel.setAttributeNS(null, 'width',
                (startBox.x + startBox.width) -
                parseFloat(lastSel.getAttributeNS(null, 'x')))
            }
            let rx = 0
            let ry = 0
            let rw = 0
            let rh = 0
            if (flip) {
              rx = startRec.x
              ry = startRec.y
              rw = $(this.svg.node).width() - startRec.x
              rh = startRec.height
            } else {
              rx = endBox.x
              ry = endRec.y
              rw = (endRec.x + endRec.width) - endBox.x
              rh = endRec.height
            }
            const newRect = this.makeSelRect(rx, ry, rw, rh)
            if (flip) {
              this.selRect.unshift(newRect)
            } else {
              this.selRect.push(newRect)
            }

            // Place new highlight in appropriate slot in SVG graph
            startsAt.parentNode.parentNode.parentNode.insertBefore(newRect, startsAt.parentNode.parentNode)
          }
        } else {
          // The user simply moved left or right along the same line so just adjust the current highlight
          const activeSelRect = flip ? this.selRect[0] : this.selRect[this.selRect.length - 1]
          // If the start moved shift the highlight and adjust width
          if (flip) {
            const rw = (parseFloat(activeSelRect.getAttributeNS(null, 'x')) +
              parseFloat(activeSelRect.getAttributeNS(null, 'width'))) -
              startRec.x
            activeSelRect.setAttributeNS(null, 'x', startRec.x)
            activeSelRect.setAttributeNS(null, 'y', startRec.y)
            activeSelRect.setAttributeNS(null, 'width', rw)
          } else {
            // If the end moved then simple change the width
            const rw = (endRec.x + endRec.width) -
              parseFloat(activeSelRect.getAttributeNS(null, 'x'))
            activeSelRect.setAttributeNS(null, 'width', rw)
          }
        }
      }
      this.lastStartRec = startRec
      this.lastEndRec = endRec
    }
  }

  private stopArcDrag (target?: Element) : void {
    // Clear the dragStartAt saved event
    this.dragStartedAt = undefined

    this.svg.removeClass('unselectable')
    this.svg.removeClass('reselect')
    this.svg.node.querySelectorAll('.reselectTarget').forEach(e => e.classList.remove('reselectTarget'))

    if (!this.arcDragOrigin) return

    if (!target) {
      this.svg.node.querySelectorAll('.badTarget').forEach(e => {
        e.parentElement?.classList.remove('highlight')
        e.classList.remove('badTarget')
      })
    } else {
      target.classList.remove('badTarget')
      target?.parentElement?.classList.remove('highlight')
    }
    this.arcDragOriginGroup.removeClass('highlight')
    this.arcDragArc?.remove()
    this.arcDragOrigin = undefined
    if (this.arcOptions) {
      $('g[data-from="' + this.arcOptions.origin + '"][data-to="' + this.arcOptions.target + '"]').removeClass('reselect')
    }
  }

  private onMouseUp (evt: MouseEvent) : void {
    if (!evt.buttons && this.selectionInProgress) {
      this.dispatcher.post('selectionEnded')
    }

    // Restore pointer events on annotations
    this.svg.find('.row, .sentnum').map(e => e.attr('pointer-events', null))

    if (!(evt.target instanceof Element)) return

    // three things that are clickable in SVG
    const targetSpanId = evt.target.getAttribute('data-span-id')
    const targetChunkId = evt.target.getAttribute('data-chunk-id')
    const targetArcRole = evt.target.getAttribute('data-arc-role')
    // The targetArcRole check must be excluded from the negation - it cancels this handler when
    // doing a mouse-up on a relation
    if (!(targetSpanId || targetChunkId) || targetArcRole) {
      // misclick
      this.clearSelection()
      this.stopArcDrag(evt.target)
      return
    }

    // is it arc drag end?
    if (this.arcDragOrigin) {
      this.endArcCreation(evt, targetSpanId)
      return
    }

    // CTRL cancels span creation
    if (evt.ctrlKey) return

    const sel = window.getSelection()
    let offsets = evt.shiftKey ? this.visualizer.selectionToPoint(sel) : this.visualizer.selectionToOffsets(sel)

    this.clearSelection()
    this.stopArcDrag(evt.target)

    // Abort if there is no range or if the range is zero-width and the shift key is not pressed
    if (!offsets || (offsets[0] === offsets[1] && !evt.shiftKey)) return

    this.spanDragJustStarted = false
    this.ajax.createSpanAnnotation([offsets])
  }

  private endArcCreation (evt: MouseEvent, targetSpanId: VID | null) {
    if (!(evt.target instanceof Element) || !this.arcDragOrigin) return

    try {
      const targetValid = evt.target.classList.contains('reselectTarget')
      let id: VID | null
      if ((id = targetSpanId) && targetValid && (evt.shiftKey || this.arcDragOrigin !== id)) {
        const originSpan = this.data.spans[this.arcDragOrigin]
        const targetSpan = this.data.spans[id]

        if (this.arcOptions && this.arcOptions.old_target) {
          this.arcOptions.target = targetSpan.id
          this.dispatcher.post('ajax', [this.arcOptions, 'edited'])
        } else {
          this.arcOptions = {
            origin: originSpan.id,
            target: targetSpan.id
          }

          this.ajax.createRelationAnnotation(originSpan.id, targetSpan.id, evt)
        }
      }
    } finally {
      this.stopArcDrag(evt.target)
    }
  }

  private rememberData (data: DocumentData) : void {
    if (data && !data.exception) {
      this.data = data
    }
  }

  private spanAndAttributeTypesLoaded (_spanTypes: Record<string, EntityTypeDto>,
    _entityAttributeTypes, _eventAttributeTypes, _relationTypesHash) {
    this.spanTypes = _spanTypes
  }

  private contextMenu (evt: MouseEvent) : void {
    if (evt.target instanceof Element) {
      // If the user shift-right-clicks, open the normal browser context menu. This is useful
      // e.g. during debugging / developing
      if (evt.shiftKey) {
        return
      }

      this.stopArcDrag()

      const id = evt.target.getAttribute('data-span-id')
      if (id) {
        evt.preventDefault()
        this.ajax.openContextMenu(id, evt)
      }
    }
  }

  private isReloadOkay () : boolean {
    // do not reload while the user is in the middle of editing
    return this.arcDragOrigin == null
  }
}

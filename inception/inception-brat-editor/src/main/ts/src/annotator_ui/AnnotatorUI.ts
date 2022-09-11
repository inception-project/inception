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
import { Entity } from '../visualizer/Entity'
import { INSTANCE as Configuration } from '../configuration/Configuration'
import { INSTANCE as Util } from '../util/Util'
import { DiamAjax, Offsets } from '@inception-project/inception-js-api'
import { EntityTypeDto } from '../protocol/Protocol'

export class AnnotatorUI {
  private data: DocumentData

  private arcDragOrigin?: string
  private arcDragOriginBox: Box
  private arcDragOriginGroup: SVGTypeMapping<SVGGElement>
  private arcDragArc: SVGTypeMapping<SVGPathElement>
  private arcDragJustStarted = false
  private spanDragJustStarted = false
  private dragStartedAt?: MouseEvent & { target: Element }
  private selectionInProgress = false

  private spanOptions?: { action?: string, offsets?: Array<Offsets>, type?: string, id?: string }
  private arcOptions?: { type?: string, old_target?: string, action?: string, origin?: string, target?: string, old_type?: string, left?: string, right?: string }
  private editedSpan: Entity
  private editedFragment: string | null = null
  private spanTypes: Record<string, EntityTypeDto>
  private selRect: SVGRectElement[]
  private lastStartRec = null
  private lastEndRec = null

  private draggedArcHeight = 30

  // for normalization: appropriate DBs per type
  private normDbsByType = {}

  private svg: Svg
  private dispatcher: Dispatcher

  private svgPosition: JQueryCoordinates

  private clickCount = 0
  private clickTimer: number | null = null
  private CLICK_DELAY = 300

  private ajax: DiamAjax

  constructor (dispatcher: Dispatcher, svg: Svg, ajax: DiamAjax) {
    console.debug('Setting up annotator-ui editor...')

    this.dispatcher = dispatcher
    this.svg = svg
    this.ajax = ajax

    dispatcher
      .on('getValidArcTypesForDrag', this, this.getValidArcTypesForDrag)
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

  // FIXME I think we don't need this anymore because we don't work with these numerical suffixes
  // REC 2021-11-27
  private stripNumericSuffix (s) {
    // utility function, originally for stripping numerix suffixes
    // from arc types (e.g. "Theme2" -> "Theme"). For values
    // without suffixes (including non-strings), returns given value.
    if (typeof (s) !== 'string') {
      return s // can't strip
    }
    const m = s.match(/^(.*?)(\d*)$/)
    return m !== null ? m[1] : s // always matches
  }

  private clearSelection () {
    window.getSelection()?.removeAllRanges()

    if (this.selRect != null) {
      for (let s = 0; s !== this.selRect.length; s++) {
        this.selRect[s].parentNode.removeChild(this.selRect[s])
      }
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
      : this.customAction
    const doubleClickAction = Configuration.singleClickEdit
      ? this.customAction
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

  private customAction (evt: MouseEvent & { target: Element }) {
    if (evt.target.getAttribute('data-span-id')) {
      this.customSpanAction(evt)
      return
    }

    if (evt.target.getAttribute('data-arc-ed')) {
      this.customArcAction(evt)
    }
  }

  private customArcAction (evt: MouseEvent) {
    if (!(evt.target instanceof Element)) return

    const id = evt.target.getAttribute('data-arc-ed')
    const type = evt.target.getAttribute('data-arc-role')
    const attrOrigin = evt.target.getAttribute('data-arc-origin')
    const attrTarget = evt.target.getAttribute('data-arc-target')

    if (id && type && attrOrigin && attrTarget) {
      const originSpan = this.data.spans[attrOrigin]
      const targetSpan = this.data.spans[attrTarget]
      this.sendTriggerCustomArcAction(id, type, originSpan, targetSpan)
    }
  }

  private sendTriggerCustomArcAction (id: string, type: string, originSpan: Entity, targetSpan: Entity) {
    this.dispatcher.post('ajax', [{
      action: 'doAction',
      arcId: id,
      arcType: type,
      originSpanId: originSpan.id,
      originType: originSpan.type,
      targetSpanId: targetSpan.id,
      targetType: targetSpan.type
    }])
  }

  private customSpanAction (evt: MouseEvent & { target: Element }) {
    const id = evt.target.getAttribute('data-span-id')

    if (id) {
      evt.preventDefault()
      this.editedSpan = this.data.spans[id]
      this.editedFragment = evt.target.getAttribute('data-fragment-id')
      this.sendTriggerCustomSpanAction(id, this.editedSpan.fragmentOffsets)
    }
  }

  private sendTriggerCustomSpanAction (id: string, offsets: ReadonlyArray<Offsets>) {
    this.dispatcher.post('ajax', [{
      action: 'doAction',
      offsets: JSON.stringify(offsets),
      id,
      labelText: this.editedSpan.labelText,
      type: this.editedSpan.type
    }])
  }

  private selectAnnotation (evt: MouseEvent & { target: Element }) {
    // must not be creating an arc
    if (this.arcDragOrigin) return

    if (evt.target.getAttribute('data-arc-role')) {
      this.selectArc(evt)
      return
    }

    if (evt.target.getAttribute('data-span-id')) {
      this.selectSpanAnnotation(evt)
    }
  }

  private selectArc (evt: MouseEvent & { target: Element }) {
    this.clearSelection()
    const originSpanId = evt.target.getAttribute('data-arc-origin')
    const targetSpanId = evt.target.getAttribute('data-arc-target')
    const type = evt.target.getAttribute('data-arc-role')
    this.arcOptions = {
      action: 'createArc',
      origin: originSpanId !== null ? originSpanId : undefined,
      target: targetSpanId !== null ? targetSpanId : undefined,
      old_target: targetSpanId !== null ? targetSpanId : undefined,
      type: type !== null ? type : undefined,
      old_type: type !== null ? type : undefined
    }

    const eventDescId = evt.target.getAttribute('data-arc-ed')
    if (eventDescId) {
      const eventDesc = this.data.eventDescs[eventDescId]
      if (eventDesc.equiv) {
        this.arcOptions.left = eventDesc.leftSpans.join(',')
        this.arcOptions.right = eventDesc.rightSpans.join(',')
      }

      this.ajax.selectAnnotation(eventDescId)
    }
  }

  private selectSpanAnnotation (evt: MouseEvent) {
    if (!(evt.target instanceof Element)) return

    const target = evt.target
    const id = target.getAttribute('data-span-id')

    if (id) {
      this.clearSelection()
      this.editedSpan = this.data.spans[id]
      this.editedFragment = target.getAttribute('data-fragment-id')
      const offsets: Array<Offsets> = this.editedSpan.fragmentOffsets

      this.spanOptions = {
        action: 'createSpan',
        offsets,
        type: this.editedSpan.type,
        id
      }

      this.ajax.selectAnnotation(id)
    }
  }

  private startArcDrag (originId) {
    this.clearSelection()

    if (!this.data.spans[originId]) {
      return
    }

    this.svg.addClass('unselectable')
    this.svgPosition = $(this.svg.node).offset()
    this.arcDragOrigin = originId
    this.arcDragArc = this.svg.path()
      .fill('none')
      .attr('marker-end', 'url(#drag_arrow)')
      .addClass('drag_stroke')
    this.arcDragOriginGroup = this.data.spans[this.arcDragOrigin].headFragment.group
    this.arcDragOriginGroup.addClass('highlight')
    this.arcDragOriginBox = Util.realBBox(this.data.spans[this.arcDragOrigin].headFragment)
    this.arcDragOriginBox.center = this.arcDragOriginBox.x + this.arcDragOriginBox.width / 2

    this.arcDragJustStarted = true
  }

  private getValidArcTypesForDrag (targetId, targetType): string[] {
    const arcType = this.stripNumericSuffix(this.arcOptions && this.arcOptions.type)
    if (!this.arcDragOrigin || targetId === this.arcDragOrigin) return []

    const originType = this.data.spans[this.arcDragOrigin].type
    const spanType = this.spanTypes[originType]
    const result: string[] = []
    if (spanType && spanType.arcs) {
      $.each(spanType.arcs, (arcNo, arc) => {
        if (arcType && arcType !== arc.type) return

        if ($.inArray(targetType, arc.targets) !== -1) {
          result.push(arc.type)
        }
      })
    }
    return result
  }

  onMouseDown (evt: MouseEvent & { target: Element }) {
    if (!(evt.target instanceof Element)) return

    // Instead of calling startArcDrag() immediately, we defer this to onMouseMove
    if (this.arcDragOrigin) return

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
    }
  }

  onMouseMove (evt: MouseEvent) {
    if (!evt.buttons && this.selectionInProgress) {
      this.dispatcher.post('selectionEnded')
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
        const id = target.getAttribute('data-span-id')
        this.startArcDrag(id)
      }
    }

    if (this.arcDragOrigin) {
      if (this.arcDragJustStarted) {
        this.initializeArcDragTargets(evt)
      }

      this.clearSelection()
      const mx = evt.pageX - this.svgPosition.left
      const my = evt.pageY - this.svgPosition.top + 5 // TODO FIXME why +5?!?
      const y = Math.min(this.arcDragOriginBox.y, my) - this.draggedArcHeight
      const dx = (this.arcDragOriginBox.center - mx) / 4
      this.arcDragArc.plot([
        ['M', this.arcDragOriginBox.center, this.arcDragOriginBox.y],
        ['C', this.arcDragOriginBox.center - dx, y, mx + dx, y, mx, my]])
    } else {
      if (this.spanDragJustStarted) {
        // If user starts selecting text, suppress all pointer events on annotations to
        // avoid the selection jumping around. During selection, we don't need the annotations
        // to react on mouse events anyway.
        this.svg.find('.row, .sentnum').map(e => e.attr('pointer-events', 'none'))
      }

      this.onMouseMoveSpanSelection(evt)
    }
    this.arcDragJustStarted = false
    this.spanDragJustStarted = false
  }

  private initializeArcDragTargets (evt: MouseEvent) {
    // show the possible targets
    const span = this.data.spans[this.arcDragOrigin]
    const spanDesc = this.spanTypes[span.type]

    if (!spanDesc || !spanDesc.arcs || spanDesc.arcs.length === 0) {
      return
    }

    // separate out possible numeric suffix from type for highlight
    // (instead of e.g. "Theme3", need to look for "Theme")
    const noNumArcType = this.stripNumericSuffix(this.arcOptions && this.arcOptions.type)

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

  private onMouseMoveSpanSelection (evt: MouseEvent) {
    // A. Scerri FireFox chunk

    // if not, then is it span selection? (ctrl key cancels)
    const sel = window.getSelection()
    if (sel === null) {
      return
    }

    let chunkIndexFrom = sel.anchorNode && $(sel.anchorNode.parentNode).attr('data-chunk-id')
    let chunkIndexTo = sel.focusNode && $(sel.focusNode.parentNode).attr('data-chunk-id')
    // fallback for firefox (at least):
    // it's unclear why, but for firefox the anchor and focus
    // node parents are always undefined, the the anchor and
    // focus nodes themselves do (often) have the necessary
    // chunk ID. However, anchor offsets are almost always
    // wrong, so we'll just make a guess at what the user might
    // be interested in tagging instead of using what's given.
    let anchorOffset = -1
    let focusOffset = -1
    let sp: Point
    let startsAt: SVGTextContentElement
    if (chunkIndexFrom === undefined && chunkIndexTo === undefined &&
      $(sel.anchorNode).attr('data-chunk-id') &&
      $(sel.focusNode).attr('data-chunk-id')) {
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
      chunkIndexTo = endsAt && $(endsAt).attr('data-chunk-id')

      // Now take the start and end character rectangles
      const startRec = startsAt.getExtentOfChar(anchorOffset)
      startRec.y += 2
      const endRec = endsAt.getExtentOfChar(focusOffset)
      endRec.y += 2

      // If nothing has changed then stop here
      if (this.lastStartRec != null && this.lastStartRec.x == startRec.x && this.lastStartRec.y == startRec.y && this.lastEndRec != null && this.lastEndRec.x == endRec.x && this.lastEndRec.y == endRec.y) {
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
        const flip = !(startRec.x === this.lastStartRec.x && startRec.y === this.lastStartRec.y)
        // If the height of the start or end changed we need to check whether
        // to remove multi line highlights no longer needed if the user went back towards their start line
        // and whether to create new ones if we moved to a newline
        if (((endRec.y !== this.lastEndRec.y)) || ((startRec.y !== this.lastStartRec.y))) {
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
              this.selRect[s2].parentNode.removeChild(this.selRect[s2])
              es--
              trunc = true
            }
            this.selRect = this.selRect.slice(ss)
          }
          if (es > -1) {
            for (let s2 = this.selRect.length - 1; s2 !== es; s2--) {
              this.selRect[s2].parentNode.removeChild(this.selRect[s2])
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

  stopArcDrag (target?: JQuery) {
    // BEGIN WEBANNO EXTENSION - #1610 - Improve brat visualization interaction performance
    // Clear the dragStartAt saved event
    this.dragStartedAt = undefined
    // END WEBANNO EXTENSION - #1610 - Improve brat visualization interaction performance
    if (this.arcDragOrigin) {
      if (!target) {
        target = $('.badTarget')
      }
      target.removeClass('badTarget')
      this.arcDragOriginGroup.removeClass('highlight')
      if (target) {
        target.parent().removeClass('highlight')
      }
      if (this.arcDragArc) {
        try {
          this.arcDragArc.remove()
        } catch (err) {
          // Ignore - could be spurious TypeError: null is not an object (evaluating 'a.parentNode.removeChild')
        }
      }
      this.arcDragOrigin = undefined
      if (this.arcOptions) {
        $('g[data-from="' + this.arcOptions.origin + '"][data-to="' + this.arcOptions.target + '"]').removeClass('reselect')
      }
      this.svg.removeClass('reselect')
    }
    this.svg.removeClass('unselectable')
    $('.reselectTarget').removeClass('reselectTarget')
  }

  onMouseUp (evt: MouseEvent) {
    if (!evt.buttons && this.selectionInProgress) {
      this.dispatcher.post('selectionEnded')
    }

    // Restore pointer events on annotations
    this.svg.find('.row, .sentnum').map(e => e.attr('pointer-events', null))

    const target = $(evt.target)

    // three things that are clickable in SVG
    const targetSpanId = target.data('span-id')
    const targetChunkId = target.data('chunk-id')
    const targetArcRole = target.data('arc-role')
    // BEGIN WEBANNO EXTENSION - #1579 - Send event when action-clicking on a relation
    /*
            if (!(targetSpanId !== undefined || targetChunkId !== undefined || targetArcRole !== undefined)) {
    */
    // The targetArcRole check must be excluded from the negation - it cancels this handler when
    // doing a mouse-up on a relation
    if (!(targetSpanId !== undefined || targetChunkId !== undefined) || targetArcRole !== undefined) {
      // END WEBANNO EXTENSION - #1579 - Send event when action-clicking on a relation
      // misclick
      this.clearSelection()
      this.stopArcDrag(target)
      return
    }

    // is it arc drag end?
    if (this.arcDragOrigin) {
      const origin = this.arcDragOrigin
      const targetValid = target.hasClass('reselectTarget')
      this.stopArcDrag(target)
      // WEBANNO EXTENSION BEGIN - #277 - self-referencing arcs for custom layers
      let id
      if ((id = target.attr('data-span-id')) && targetValid && (evt.shiftKey || origin != id)) {
        //          if ((id = target.attr('data-span-id')) && origin != id && targetValid) {
        // WEBANNO EXTENSION END - #277 - self-referencing arcs for custom layers
        const originSpan = this.data.spans[origin]
        const targetSpan = this.data.spans[id]

        if (this.arcOptions && this.arcOptions.old_target) {
          this.arcOptions.target = targetSpan.id
          this.dispatcher.post('ajax', [this.arcOptions, 'edited'])
        } else {
          this.arcOptions = {
            action: 'createArc',
            origin: originSpan.id,
            target: targetSpan.id
          }

          this.ajax.createRelationAnnotation(originSpan.id, targetSpan.id)
        }
      }
    } else if (!evt.ctrlKey) {
      // if not, then is it span selection? (ctrl key cancels)
      const sel = window.getSelection()

      // Try getting anchor and focus node via the selection itself. This works in Chrome and
      // Safari.
      let anchorNode = sel.anchorNode && $(sel.anchorNode).closest('*[data-chunk-id]')
      let anchorOffset = sel.anchorOffset
      let focusNode = sel.focusNode && $(sel.focusNode).closest('*[data-chunk-id]')
      let focusOffset = sel.focusOffset

      // If using the selection was not successful, try using the ranges instead. This should
      // work on Firefox.
      if ((anchorNode == null || !anchorNode[0] || focusNode == null || !focusNode[0]) && sel.type != 'None') {
        anchorNode = $(sel.getRangeAt(0).startContainer).closest('*[data-chunk-id]')
        anchorOffset = sel.getRangeAt(0).startOffset
        focusNode = $(sel.getRangeAt(sel.rangeCount - 1).endContainer).closest('*[data-chunk-id]')
        focusOffset = sel.getRangeAt(sel.rangeCount - 1).endOffset
      }

      // If neither approach worked, give up - the user didn't click on selectable text.
      if (anchorNode == null || !anchorNode[0] || focusNode == null || !focusNode[0]) {
        this.clearSelection()
        this.stopArcDrag(target)
        return
      }

      let chunkIndexFrom = anchorNode && anchorNode.attr('data-chunk-id')
      let chunkIndexTo = focusNode && focusNode.attr('data-chunk-id')

      // BEGIN WEBANNO EXTENSION - #316 Text selection behavior while dragging mouse
      // BEGIN WEBANNO EXTENSION - #724 - Cross-row selection is jumpy
      if (focusNode && anchorNode && focusNode[0] === anchorNode[0] && focusNode.hasClass('spacing')) {
        if (evt.shiftKey) {
          if (anchorOffset === 0) {
            // Move anchor to the end of the previous node
            anchorNode = focusNode = anchorNode.prev()
            anchorOffset = focusOffset = anchorNode.text().length
            chunkIndexFrom = chunkIndexTo = anchorNode.attr('data-chunk-id')
          } else {
            // Move anchor to the beginning of the next node
            anchorNode = focusNode = anchorNode.next()
            anchorOffset = focusOffset = 0
            chunkIndexFrom = chunkIndexTo = anchorNode.attr('data-chunk-id')
          }
        } else {
          // misclick
          this.clearSelection()
          this.stopArcDrag(target)
          return
        }
      } else {
        // If we hit a spacing element, then we shift the anchors left or right, depending on
        // the direction of the selected range.
        if (anchorNode.hasClass('spacing')) {
          if (Number(chunkIndexFrom) < Number(chunkIndexTo)) {
            anchorNode = anchorNode.next()
            anchorOffset = 0
            chunkIndexFrom = anchorNode.attr('data-chunk-id')
          } else if (anchorNode.hasClass('row-initial')) {
            anchorNode = anchorNode.next()
            anchorOffset = 0
          } else {
            anchorNode = anchorNode.prev()
            anchorOffset = anchorNode.text().length
          }
        }
        if (focusNode.hasClass('spacing')) {
          if (Number(chunkIndexFrom) > Number(chunkIndexTo)) {
            focusNode = focusNode.next()
            focusOffset = 0
            chunkIndexTo = focusNode.attr('data-chunk-id')
          } else if (focusNode.hasClass('row-initial')) {
            focusNode = focusNode.next()
            focusOffset = 0
          } else {
            focusNode = focusNode.prev()
            focusOffset = focusNode.text().length
          }
        }
      }
      // END WEBANNO EXTENSION - #724 - Cross-row selection is jumpy
      // END WEBANNO EXTENSION - #316 Text selection behavior while dragging mouse

      if (chunkIndexFrom !== undefined && chunkIndexTo !== undefined) {
        const chunkFrom = this.data.chunks[chunkIndexFrom]
        const chunkTo = this.data.chunks[chunkIndexTo]
        let selectedFrom = chunkFrom.from + anchorOffset
        let selectedTo = chunkTo.from + focusOffset
        sel.removeAllRanges()

        if (selectedFrom > selectedTo) {
          const tmp = selectedFrom; selectedFrom = selectedTo; selectedTo = tmp
        }
        // trim
        while (selectedFrom < selectedTo && ' \n\t'.indexOf(this.data.text.substr(selectedFrom, 1)) !== -1) selectedFrom++
        while (selectedFrom < selectedTo && ' \n\t'.indexOf(this.data.text.substr(selectedTo - 1, 1)) !== -1) selectedTo--

        // shift+click allows zero-width spans
        if (selectedFrom === selectedTo && !evt.shiftKey) {
          // simple click (zero-width span)
          return
        }

        this.spanOptions = {
          action: 'createSpan',
          offsets: [[selectedFrom, selectedTo]]
        }

        // normal span select in standard annotation mode or reselect: show selector
        this.spanDragJustStarted = false
        const spanText = this.data.text.substring(selectedFrom, selectedTo)
        this.ajax.createSpanAnnotation(this.spanOptions.offsets, spanText)
      }
    }
  }

  toggleCollapsible ($el, state?) {
    const opening = state !== undefined ? state : !$el.hasClass('open')
    const $collapsible = $el.parent().find('.collapsible:first')
    if (opening) {
      $collapsible.addClass('open')
      $el.addClass('open')
    } else {
      $collapsible.removeClass('open')
      $el.removeClass('open')
    }
  }

  collapseHandler (evt) {
    this.toggleCollapsible($(evt.target))
  }

  rememberData (data: DocumentData) {
    if (data && !data.exception) {
      this.data = data
    }
  }

  spanAndAttributeTypesLoaded (_spanTypes: Record<string, EntityTypeDto>, _entityAttributeTypes, _eventAttributeTypes, _relationTypesHash) {
    this.spanTypes = _spanTypes
  }

  contextMenu (evt: MouseEvent) {
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

  isReloadOkay () {
    // do not reload while the user is in the middle of editing
    return this.arcDragOrigin == null
  }
}

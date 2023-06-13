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

import { SVGTypeMapping } from '@svgdotjs/svg.js'
import { Chunk } from './Chunk'
import { Entity } from './Entity'
import { RectBox } from './RectBox'

export class Fragment {
  id: number
  span: Entity
  from: number
  to: number
  rectBox: RectBox
  text : string
  chunk: Chunk
  indexNumber : number
  drawOrder: number
  towerId : number
  curly: { from: number, to: number }
  drawCurly = false
  labelText: string
  glyphedLabelText: string
  group: SVGTypeMapping<SVGGElement>
  rect: SVGTypeMapping<SVGElement>
  left: number
  right: number
  width: number
  height: number
  nestingHeight: number
  nestingHeightLR: number
  nestingHeightRL: number
  nestingDepth: number
  nestingDepthLR: number
  nestingDepthRL: number
  highlightPos: {x: number, y: number, w: number, h: number} | undefined

  constructor (id: number, span: Entity, from: number, to: number) {
    this.id = id
    this.span = span
    this.from = from
    this.to = to
    // Object.seal(this)
  }

  /**
   * Helper method to calculate the bounding box from known values instead of asking the browser
   * which would then force a slow re-layout.
   */
  rowBBox () {
    const box = Object.assign({}, this.rectBox) // clone
    const chunkTranslation = this.chunk.translation
    box.x += chunkTranslation.x
    box.y += chunkTranslation.y
    return box
  }

  /**
   * @param {Fragment} a
   * @param {Fragment} b
   */
  static midpointComparator = (a: Fragment, b: Fragment) => {
    const tmp = a.from + a.to - b.from - b.to
    if (!tmp) {
      return 0
    }
    return tmp < 0 ? -1 : 1
  }

  static compare (a: Fragment, b: Fragment) {
    const aSpan = a.span
    const bSpan = b.span

    // spans with more fragments go first
    let tmp = aSpan.fragments.length - bSpan.fragments.length
    if (tmp) {
      return tmp < 0 ? 1 : -1
    }

    // longer arc distances go last
    tmp = aSpan.avgDist - bSpan.avgDist
    if (tmp) {
      return tmp < 0 ? -1 : 1
    }

    // spans with more arcs go last
    tmp = aSpan.numArcs - bSpan.numArcs
    if (tmp) {
      return tmp < 0 ? -1 : 1
    }

    // compare the span widths,
    // put wider on bottom so they don't mess with arcs, or shorter
    // on bottom if there are no arcs.
    const ad = a.to - a.from
    const bd = b.to - b.from
    tmp = ad - bd
    if (aSpan.numArcs === 0 && bSpan.numArcs === 0) {
      tmp = -tmp
    }

    if (tmp) {
      return tmp < 0 ? 1 : -1
    }

    tmp = aSpan.refedIndexSum - bSpan.refedIndexSum
    if (tmp) {
      return tmp < 0 ? -1 : 1
    }

    // if no other criterion is found, sort by type to maintain
    // consistency
    // TODO: isn't there a cmp() in JS?
    if (aSpan.type < bSpan.type) {
      return -1
    } else if (aSpan.type > bSpan.type) {
      return 1
    }

    return 0
  }
}

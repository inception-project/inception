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
import { SVGTypeMapping, Svg } from '@svgdotjs/svg.js'
import { Chunk } from './Chunk'

export class Row {
  group: SVGTypeMapping<SVGGElement>
  background: SVGTypeMapping<SVGGElement>
  arcs: SVGTypeMapping<SVGGElement>
  chunks: Chunk[] = []
  hasAnnotations = false
  maxArcHeight = 0
  maxSpanHeight = 0
  boxHeight = 0
  sentence: number
  index: number
  backgroundIndex: number
  heightsStart: number
  heightsEnd: number
  heightsAdjust: number
  textY: number
  translation: { x: number, y: number } = { x: 0, y: 0 }

  constructor (svg: Svg) {
    this.group = svg.group().addClass('row')
    this.background = svg.group().addTo(this.group)
    // Object.seal(this)
  }

  updateFragmentHeight (defaultHeight: number) {
    if (this.chunks.length === 0) {
      this.maxSpanHeight = defaultHeight
      return
    }

    for (const chunk of this.chunks) {
      for (const fragment of chunk.fragments) {
        if (this.maxSpanHeight < fragment.height) {
          this.maxSpanHeight = fragment.height
        }
      }
    }
  }

  updateRowBoxHeight (rowSpacing: number, rowPadding: number) {
    // This is the fix for brat #724, but the numbers are guessed.
    this.boxHeight = Math.max(this.maxArcHeight + 5, this.maxSpanHeight + 1.5) // XXX TODO HACK: why 5, 1.5?
    if (this.hasAnnotations) {
      this.boxHeight += rowSpacing + 1.5 // XXX TODO HACK: why 1.5?
    } else {
      this.boxHeight -= 5 // XXX TODO HACK: why -5?
    }

    this.boxHeight += rowPadding
  }
}

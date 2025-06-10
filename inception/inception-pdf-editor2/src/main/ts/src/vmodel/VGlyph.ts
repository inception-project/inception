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

import { Rectangle } from './Rectangle'
import { type JsonVGlyph, type JsonVChunk } from './VModelJsonDeserializer'
import { VPage } from './VPage'

export class VGlyph {
  page: number
  begin: number
  end: number
  unicode: string
  bbox: Rectangle

  constructor (aPage: VPage, aLine: JsonVChunk, aGlyph: JsonVGlyph) {
    this.page = aPage.index
    this.begin = aGlyph[0]
    this.end = aGlyph[1]
    this.unicode = aGlyph[2]
    if (aLine[0] === 0 || aLine[0] === 180) {
      this.bbox = new Rectangle({ p: aPage.index, x: aGlyph[3], y: aLine[2], w: aGlyph[4], h: aLine[4] })
    } else {
      this.bbox = new Rectangle({ p: aPage.index, x: aLine[1], y: aGlyph[3], w: aLine[3], h: aGlyph[4] })
    }
  }
}

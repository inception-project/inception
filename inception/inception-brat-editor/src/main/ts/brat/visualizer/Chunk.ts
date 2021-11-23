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
import { SVG, Svg } from "@svgdotjs/svg.js";
import { Fragment } from "./Fragment";
import { Row } from "./Row";

export class Chunk {
  index = undefined;
  text = undefined;
  from = undefined;
  to = undefined;
  space = undefined;
  fragments: Fragment[] = [];
  lastSpace = undefined;
  nextSpace = undefined;
  sentence: number = undefined;
  group: SVGGElement = undefined;
  highlightGroup: SVGGElement = undefined;
  markedTextStart = undefined;
  markedTextEnd = undefined;
  right = undefined;
  row: Row = undefined;
  textX = undefined;
  translation = undefined;
  firstFragmentIndex = undefined;
  lastFragmentIndex = undefined;
  rtlsizes = undefined;

  constructor(index, text, from, to, space) {
    this.index = index;
    this.text = text;
    this.from = from;
    this.to = to;
    this.space = space;
    Object.seal(this);
  }

  renderText(svg: Svg, rowTextGroup: SVGGElement ) {
    svg.text(this.text)
      .translate(this.textX, this.row.textY)
      .attr('data-chunk-id', this.index)
      .addTo(SVG(rowTextGroup));
  }
}

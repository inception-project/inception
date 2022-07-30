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

import { Point } from './Point'

export class Rectangle {
  p: number
  x: number
  y: number
  w: number
  h: number

  constructor (aRectangle: {p: number, x: number, y: number, w: number, h: number}) {
    this.p = aRectangle.p
    this.x = aRectangle.x
    this.y = aRectangle.y
    this.w = aRectangle.w
    this.h = aRectangle.h
  }

  contains (aP: Point): boolean {
    return this.x <= aP.x && aP.x <= (this.x + this.w) &&
        this.y <= aP.y && aP.y <= (this.y + this.h)
  }

  setPosition (p: {top: number, bottom: number, left: number, right: number}) {
    this.y = p.top
    this.h = p.bottom - p.top
    this.x = p.left
    this.w = p.right - p.left
  }

  get top () : number {
    return this.y
  }

  get bottom () : number {
    return this.y + this.h
  }

  get left () : number {
    return this.x
  }

  get right () : number {
    return this.x + this.w
  }
}

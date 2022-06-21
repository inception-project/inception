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
import { ColorCode, RoleDto, VID } from '../protocol/Protocol'
import { Arc } from './Arc'
import { Comment } from './Comment'
import { Role } from './Role'

export type GeneralEventType = string;

export const EQUIV: GeneralEventType = 'equiv'
export const RELATION: GeneralEventType = 'relation'

/**
 * An n-ary relation which is used to represent either a simple relation (relation === true) or
 * an equivalence (equiv === true) or an event (equiv === false and relation === false).
 */
export class EventDesc {
  id: VID
  triggerId: VID
  roles: Array<Role> = []
  equivArc: Arc
  equiv = false
  relation = false
  leftSpans: Array<VID>
  rightSpans: Array<VID>
  annotatorNotes
  comment: Comment
  labelText: string
  color: ColorCode
  shadowClass: string

  constructor (id: VID, triggerId: VID, roles: Array<RoleDto>, generalType?: GeneralEventType) {
    this.id = id
    this.triggerId = triggerId
    this.equiv = generalType === EQUIV
    this.relation = generalType === RELATION
    // Object.seal(this);
    roles.map(role => this.roles.push({ type: role[0], targetId: role[1] }))
  }
}

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
import { VID, Relation, AnnotatedText } from '..'
import { CompactArgument, unpackCompactArgument } from './CompactArgument'
import { CompactRelationAttributes } from './CompactRelationAttributes'

export type CompactRelation = [
  layerId: number,
  vid: VID,
  arguments: Array<CompactArgument>,
  attributes?: CompactRelationAttributes
]

export function unpackCompactRelation (doc: AnnotatedText, raw: CompactRelation): Relation {
  const cooked = new Relation()
  cooked.layer = doc.__getOrCreateLayer(raw[0])
  cooked.vid = raw[1]
  cooked.arguments = raw[2].map(arg => unpackCompactArgument(doc, arg))
  cooked.color = raw[3]?.c
  cooked.label = raw[3]?.l
  return cooked
}

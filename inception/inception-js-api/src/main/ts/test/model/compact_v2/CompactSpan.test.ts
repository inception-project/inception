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
import { it, expect } from 'vitest'
import { AnnotatedText, Span, Comment } from '../../../src/model'
import { CompactComment, unpackCompactComments } from '../../../src/model/compact_v2/CompactComment'

it('Unpacking works', async () => {
  const doc = new AnnotatedText()
  const ann = new Span({ vid: 'T1', layer: undefined, offsets: [[0, 1]] })
  doc.addAnnotation(ann)
  const input : CompactComment[] = [
    ['info comment 1'],
    ['info comment 2', 'I'],
    ['error comment', 'E']
  ]

  const result = unpackCompactComments(doc, ann, input)

  expect(result).to.deep.equal([
    new Comment({ targetId: 'T1', target: ann, type: 'info', comment: 'info comment 1' }),
    new Comment({ targetId: 'T1', target: ann, type: 'info', comment: 'info comment 2' }),
    new Comment({ targetId: 'T1', target: ann, type: 'error', comment: 'error comment' })
  ])
})

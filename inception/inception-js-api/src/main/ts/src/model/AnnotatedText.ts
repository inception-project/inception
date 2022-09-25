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
import { Offsets } from './Offsets'
import { VID } from './VID'
import { AnnotationMarker } from './AnnotationMarker'
import { Relation } from './Relation'
import { Span } from './Span'
import { TextMarker } from './TextMarker'
import { Layer } from './Layer'

export class AnnotatedText {
  window: Offsets
  text?: string
  layers: Map<number, Layer> = new Map<number, Layer>()
  relations: Map<VID, Relation> = new Map<VID, Relation>()
  spans: Map<VID, Span> = new Map<VID, Span>()
  annotationMarkers: Map<VID, AnnotationMarker[]>
  textMarkers: TextMarker[]

  __getOrCreateLayer (id: number): Layer {
    let layer = this.layers.get(id)
    if (!layer) {
      layer = new Layer()
      layer.id = id
      this.layers.set(id, layer)
    }
    return layer
  }

  public * annotations () {
    yield * this.spans.values()
    yield * this.relations.values()
  }
}

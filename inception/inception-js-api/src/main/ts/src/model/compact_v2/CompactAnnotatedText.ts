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
import { AnnotatedText, Offsets, VID } from '..'
import { CompactAnnotationMarker, unpackCompactAnnotationMarker } from './CompactAnnotationMarker'
import { CompactLayer, unpackCompactLayer } from './CompactLayer'
import { CompactRelation, unpackCompactRelation } from './CompactRelation'
import { CompactSpan, unpackCompactSpan } from './CompactSpan'
import { CompactTextMarker, unpackCompactTextMarker } from './CompactTextMarker'

export interface CompactAnnotatedText {
  text?: string
  layers?: Array<CompactLayer>
  window: Offsets
  relations?: Array<CompactRelation>
  spans?: Array<CompactSpan>
  annotationMarkers?: Array<CompactAnnotationMarker>
  textMarkers?: Array<CompactTextMarker>
}

export function unpackCompactAnnotatedText (raw: CompactAnnotatedText): AnnotatedText {
  const cooked = new AnnotatedText()
  cooked.text = raw.text
  cooked.window = raw.window
  raw.layers?.forEach(layer => cooked.layers.set(layer.id, unpackCompactLayer(layer)))

  for (const span of raw.spans || []) {
    const cookedSpan = unpackCompactSpan(cooked, span)
    cooked.spans.set(cookedSpan.vid, cookedSpan)
  }

  for (const rel of raw.relations || []) {
    const cookedRel = unpackCompactRelation(cooked, rel)
    cooked.relations.set(cookedRel.vid, cookedRel)
  }

  const annotationMarkers = raw.annotationMarkers?.map(unpackCompactAnnotationMarker)
  cooked.annotationMarkers = makeMarkerMap(annotationMarkers)
  cooked.textMarkers = raw.textMarkers?.map(unpackCompactTextMarker) || []
  return cooked
}

/**
 * Converts a list of {@link CompactAnnotationMarker}s to an easily accessible map using the {@link VID}
 * as key and the set of markers on that annotation as values.
 *
 * @param markerList a list of {@link CompactAnnotationMarker}s
 * @returns the map
 */
export function makeMarkerMap<T> (markerList: T[] | undefined): Map<VID, Array<T>> {
  const markerMap = new Map<VID, Array<T>>()
  if (markerList) {
    markerList.forEach(marker => {
      marker[1].forEach(vid => {
        let ms = markerMap.get(vid)
        if (!ms) {
          ms = []
          markerMap.set(vid, ms)
        }
        ms.push(marker)
      })
    })
  }
  return markerMap
}

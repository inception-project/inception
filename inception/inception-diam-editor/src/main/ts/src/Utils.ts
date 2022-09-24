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
import { AnnotatedText, Offsets, Relation, Span, VID } from '@inception-project/inception-js-api'
import { compareOffsets } from '@inception-project/inception-js-api/src/model/Offsets'

export function uniqueLabels (data: AnnotatedText): string[] {
  if (!data) return []

  const sortedLabelsWithDuplicates = Array.from(data.annotations(), (ann) => ann?.label || '')
    .sort()

  const sortedLabels: string[] = []
  for (let i = 0; i < sortedLabelsWithDuplicates.length; i++) {
    if (i === 0 || sortedLabelsWithDuplicates[i - 1] !== sortedLabelsWithDuplicates[i]) {
      sortedLabels.push(sortedLabelsWithDuplicates[i])
    }
  }

  console.log(sortedLabels)
  return sortedLabels
}

export function uniqueOffsets (data: AnnotatedText): Offsets[] {
  if (!data) return []

  const sortedOffsetsWithDuplicates = Array.from(data.spans.values(), (span) => span.offsets[0])
    .sort(compareOffsets)

  const sortedOffsets: Offsets[] = []
  for (let i = 0; i < sortedOffsetsWithDuplicates.length; i++) {
    if (
      i === 0 ||
      compareOffsets(
        sortedOffsetsWithDuplicates[i - 1],
        sortedOffsetsWithDuplicates[i]
      ) !== 0
    ) {
      sortedOffsets.push(sortedOffsetsWithDuplicates[i])
    }
  }

  // console.log(sortedSpanOffsets)
  return sortedOffsets
}

export function groupBy<T> (data: IterableIterator<T>, keyMapper: (s: T) => string): Record<string, T[]> {
  if (!data) return {}

  const groupedSpans = {}
  for (const d of data) {
    const key = `${keyMapper(d)}`
    if (groupedSpans[key] === undefined) {
      groupedSpans[key] = []
    }
    groupedSpans[key].push(d)
  }

  return groupedSpans
}

/**
 * Groups spans by label.
 *
 * @param data spans
 * @returns grouped spans
 */
export function groupSpansByLabel (data: AnnotatedText): Record<string, Span[]> {
  const groups = groupBy(data?.spans.values(), (s) => s.label || '')
  for (const items of Object.values(groups)) {
    items.sort((a, b) => compareOffsets(a.offsets[0], b.offsets[0]))
  }
  return groups
}

/**
 * Groups relations by label.
 *
 * @param data relations
 * @returns grouped relations
 */
export function groupRelationsByLabel (data: AnnotatedText): Record<string, Relation[]> {
  const groups = groupBy(data?.relations.values(), (s) => s.label || '')
  for (const items of Object.values(groups)) {
    items.sort((a, b) => compareOffsets(
      (a.arguments[0].target as Span).offsets[0],
      (b.arguments[0].target as Span).offsets[0]))
  }
  return groups
}

/**
 * Groups spans by position. The key in the returned map contains the start/end of the offset.
 *
 * @param data spans
 * @returns grouped spans
 */
export function groupSpansByPosition (data: AnnotatedText): Record<string, Span[]> {
  return groupBy(data?.spans.values(), (s) => `${s.offsets}`)
}

export function groupRelationsByPosition (data: AnnotatedText): Record<string, Relation[]> {
  return groupBy(data?.relations.values(), (r) => `${(r.arguments[0].target as Span).offsets}`)
}

export function highlightClasses (vid: VID, data: AnnotatedText): string {
  const classes: string[] = []
  for (const marker of data.annotationMarkers.get(vid) || []) {
    classes.push(`marker-${marker.type}`)
  }
  return classes.join(' ')
}

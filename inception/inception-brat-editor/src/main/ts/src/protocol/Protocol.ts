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

import { Offsets } from '@inception-project/inception-js-api'

export type VID = string | number
export type CommentType = 'AnnotatorNotes' | 'EditHighlight' | 'AnnotationError'
  | 'AnnotationIncomplete' | 'AnnotationUnconfirmed' | 'AnnotationWarning' | 'MissingAnnotation'
  | 'ChangedAnnotation' | 'Normalized' | 'True_positive' | 'False_positive' | 'False_negative'
export type MarkerType = 'edited' | 'focus' | 'matchfocus' | 'match' | 'warn'
export type ClippedState = '' | 's' | 'e' | 'se'
export type ColorCode = string

/**
 * @property {string} l - label
 * @property {string} c - color
 * @property {string} h - hover text
 * @property {boolean} a - action buttons
 */
export type EntityAttributesDto = {
  l: string;
  c: ColorCode;
  h: string;
  a: boolean;
  s: number;
  cl: ClippedState;
}

export type AnnotationCommentDto = [
  id: VID,
  commentType: CommentType,
  comment: string
]

export type SentenceCommentDto = [
  anchor: ['sent', number, VID],
  commentType: CommentType,
  comment: string
]

export type CommentDto = AnnotationCommentDto | SentenceCommentDto;

export type AnnotationMarkerDto = [
  id: VID,
  type: MarkerType
];

export type SentenceMarkerDto = [
  kind: 'sent',
  type: MarkerType,
  index: number
]

export type TextMarkerDto = [
  begin: number,
  end: number
]

export type MarkerDto = AnnotationMarkerDto | SentenceMarkerDto | TextMarkerDto;

/**
 * The roles used during arc annotation in the form of [["Arg1","p_21346"],["Arg2","p_21341"]]
 * to denote a given arc annotation such as dependency parsing and coreference resolution
 *
 * @see
 */
export type RoleDto = [
  type: number,
  target: VID
];

export type EntityDto = [
  id: VID,
  type: string,
  offsets: Array<Offsets>,
  attributes?: EntityAttributesDto
]

/**
 * A relation between span annotations -&gt; an arc annotation.
 *
 * Example
 *
 * <pre><code>
 * "relations":[[
 *   "d_48420",
 *   "SUBJ",
 *   [["Arg1","p_21346"],["Arg2","p_21341"]]],
 *   ...
 * </pre></code>
 */

export type RelationDto = [
  id: VID,
  type: string,
  arguments: [arg0: RoleDto, arg1: RoleDto],
  labelText?: string,
  color?: ColorCode
]

export type RelationProperties = {
  symmetric: boolean;
}

/**
 * Type of an arc. Defines properties such as color, possible targets, etc.
 *
 * Example:
 *
 * <pre><code>
 * "arcs":[{
 *   "type": "anaphoric",
 *   "color": "green",
 *   "arrowHead": "triangle,5",
 *   "labels":["anaphoric"],
 *   "targets":["nam"],
 *   "dashArray":""
 * }]....
 * </code><pre>
 */
export type RelationTypeDto = {
  type: string;
  color: ColorCode;
  arrowHead: string;
  dashArray: string;
  labels: Array<string>;
  targets: Array<string>;
  labelArrow: string; // deprecated? not supported server-side
  args; // Absolutely no clue; deprecated? not supported server-side
  properties: RelationProperties; // deprecated? not supported server-side
  children: RelationTypeDto[] // deprecated? not supported server-side
}

/**
 * Different attributes of an Entity used for its visualisation formats. It looks like
 *
 * <pre><code>
 * {
 *   "name": "Named Entity",
 *   "type": "Named Entity",
 *   "unused": true,
 *   "fgColor": "black",
 *   "bgColor": "cyan",
 *   "borderColor": "green",
 *   "labels":[],
 *   "children":[{
 *     "name":"LOC",
 *     "type":"LOC",
 *     "unused":false,
 *     "fgColor":"black",
 *     "bgColor":"cyan",
 *     "borderColor":"green",
 *     "labels":["LOC"],
 *     "children":[],
 *     "attributes":[],
 *     "arcs":[]
 *    },
 *    ...
 * </pre></code>
 */
export type EntityTypeDto = {
  name: string;
  type: string;
  fgColor: ColorCode;
  bgColor: ColorCode;
  borderColor: ColorCode;
  labels: Array<string>;
  arcs: Array<RelationTypeDto>;
  children: Array<EntityTypeDto>;
  // children: Array<SpanType>; // deprecated?
  // attributes: Array<string>; // deprecated?
  // unused: boolean; // deprecated?
  // hotkey: string; // deprecated?
}

/**
 * @deprecated Not used by server side
 */
export type AttributeDto = [
  id: string,
  name: string,
  spanId: VID,
  value: string
]

/**
 * @deprecated Not used by server side
 */
export type EquivDto = [
  a: string,
  b: 'Equiv',
  ...spanId: Array<string>
]

/**
 * @deprecated Not used by server side
 */
export type EventDto = [
  id: string,
  triggerId: string,
  roles: Array<RoleDto>
]

/**
 * @deprecated Not used by server side
 */
export type TriggerDto = [
  id: string,
  type: string,
  offsets: Array<Offsets>
]

/**
 * This class represents the JSON object that we receive from the server. Note that the class is
 * currently only used for documentation purposes. The JSON we get is not really cast into an
 * object of this class.
 */
export type SourceData = {
  text: string;
  comments: Array<CommentDto>;
  entities: Array<EntityDto>;
  relations: Array<RelationDto>;
  sentence_offsets: Array<Offsets>;
  token_offsets: Array<Offsets>;
  sentence_number_offset: number;
  windowBegin: number;
  windowEnd: number;
  rtl_mode: boolean;
  font_zoom: number;
  args: Record<MarkerType, MarkerDto>;

  /**
   * @deprecated INCEpTION does not use attributes
   */
  attributes: Array<AttributeDto>;

  /**
   * @deprecated INCEpTION does not use triggers
   */
  triggers: Array<TriggerDto>; // deprecated?

  /**
   * @deprecated INCEpTION does not use equivs
   */
  equivs: Array<EquivDto>; // deprecated?

  /**
   * @deprecated INCEpTION does not use events
   */
  events: Array<EventDto>; // deprecated?
}

export const WARN: MarkerType = 'warn'
export const EDITED: MarkerType = 'edited'
export const FOCUS: MarkerType = 'focus'
export const MATCH_FOCUS: MarkerType = 'matchfocus'
export const MATCH: MarkerType = 'match'

export const EDIT_HIGHLIGHT: CommentType = 'EditHighlight'
export const ANNOTATION_ERROR: CommentType = 'AnnotationError'
export const ANNOTATION_INCOMPLETE: CommentType = 'AnnotationIncomplete'
export const ANNOTATION_UNCONFIRMED: CommentType = 'AnnotationUnconfirmed'
export const ANNOTATION_WARNING: CommentType = 'AnnotationWarning'
export const ANNOTATOR_NOTES: CommentType = 'AnnotatorNotes'
export const MISSING_ANNOTATION: CommentType = 'MissingAnnotation'
export const CHANGED_ANNOTATION: CommentType = 'ChangedAnnotation'
export const NORMALIZED: CommentType = 'Normalized'
export const TRUE_POSITIVE: CommentType = 'True_positive'
export const FALSE_POSITIVE: CommentType = 'False_positive'
export const FALSE_NEGATIVE: CommentType = 'False_negative'

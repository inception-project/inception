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

import { Sizes } from './Sizes'
import { Measurements } from './Measurements'
import { DocumentData } from './DocumentData'
import { ENTITY, Entity, TRIGGER } from './Entity'
import { EQUIV, EventDesc, RELATION } from './EventDesc'
import { Chunk, type Marker } from './Chunk'
import { Fragment } from './Fragment'
import { Arc } from './Arc'
import { Row } from './Row'
import { RectBox } from './RectBox'
import { AttributeType, type ValType } from './AttributeType'
import { type CollectionLoadedResponse } from './CollectionLoadedResponse'
import { type RelationTypeDto, type EntityTypeDto, type EntityDto, type CommentDto, type SourceData, type TriggerDto, type AttributeDto, type EquivDto, type ColorCode, type MarkerType,type MarkerDto, type RelationDto, EDITED, FOCUS, MATCH_FOCUS, MATCH,type  RoleDto, type VID, WARN } from '../protocol/Protocol'
import type { Dispatcher, Message } from '../dispatcher/Dispatcher'
import * as jsonpatch from 'fast-json-patch'
import { type Operation } from 'fast-json-patch'
import { scrollbarWidth } from '../util/ScrollbarWidth'
import '@svgdotjs/svg.filter.js'
import { SVG, Element as SVGJSElement, Svg, Container, Text as SVGText, type PathCommand, Rect, type ArrayXY, type SVGTypeMapping, Defs, G } from '@svgdotjs/svg.js'
import { INSTANCE as Configuration } from '../configuration/Configuration'
import { INSTANCE as Util } from '../util/Util'
import { AnnotationOutEvent, AnnotationOverEvent, type Offsets, Relation, Span } from '@inception-project/inception-js-api'
declare const $: JQueryStatic

/**
 * [lastRow, textDesc[3], lastX - boxX, textDesc[4]]
 */
type MarkedTextHighlight = [row: Row, xBegin: number, xEnd: number, type: string];

type MarkedText = [begin: number, end: number, type: string];

/**
 * Sets default values for a wide range of optional attributes.
 *
 * @param {SourceData} sourceData
 */
function setSourceDataDefaults (sourceData: SourceData) {
  // The following are empty lists if not set
  $.each([
    'attributes',
    'comments',
    'entities',
    'equivs',
    'events',
    'relations',
    'triggers'
  ], (attrNo, attr) => {
    if (sourceData[attr] === undefined) {
      sourceData[attr] = []
    }
  })

  // Avoid exception due to undefined text in tokenise and sentenceSplit
  if (sourceData.text === undefined) {
    sourceData.text = ''
  }

  // If we lack sentence offsets we fall back on naive sentence splitting
  if (sourceData.sentence_offsets === undefined) {
    sourceData.sentence_offsets = sentenceSplit(sourceData.text)
  }

  // Similarly we fall back on whitespace tokenisation
  if (sourceData.token_offsets === undefined) {
    sourceData.token_offsets = tokenise(sourceData.text)
  }
}

// Set default values for a variety of collection attributes
function setCollectionDefaults (collectionData) {
  // The following are empty lists if not set
  const attrs = ['entity_attribute_types', 'entity_types',
    'event_attribute_types', 'event_types',
    'relation_attribute_types', 'relation_types']
  for (const attr of attrs) {
    if (collectionData[attr] === undefined) {
      collectionData[attr] = []
    }
  }
}

export class Visualizer {
  private dispatcher: Dispatcher

  private rtlmode = false
  private fontZoom = 100

  svg: Svg
  svgContainer: HTMLElement
  private highlightGroup: SVGTypeMapping<SVGGElement>

  private baseCanvasWidth = 0
  private canvasWidth = 0

  data?: DocumentData
  private sourceData?: SourceData
  private requestedData: SourceData | null = null // FIXME Do we really need requestedData AND sourceData?

  private args: Partial<Record<MarkerType, MarkerDto>> = {}

  private selectionInProgress = false
  private dataChangedButNotRendered = false
  private isRenderRequested = false
  private drawing = false
  private redraw = false

  private entityTypes: Record<string, EntityTypeDto> = {}
  private relationTypes: Record<string, RelationTypeDto> = {}
  private entityAttributeTypes: Record<string, AttributeType> = {}
  private eventAttributeTypes: Record<string, AttributeType> = {}
  private isCollectionLoaded = false

  private markedText: Array<MarkedText> = []
  private highlight: Array<Rect> = []
  private highlightArcs?: SVGJSElement[]

  // OPTIONS
  private collapseArcs = false
  private collapseArcSpace = false
  private roundCoordinates = true // try to have exact pixel offsets
  private boxTextMargin = { x: 0, y: 1.5 } // effect is inverse of "margin" for some reason
  private highlightRounding = { x: 3, y: 3 } // rx, ry for highlight boxes
  private spaceWidths: Record<string, number> = {
    ' ': 4,
    '\u00a0': 4,
    '\u200b': 0,
    '\u3000': 8,
    '\t': 12,
    '\n': 4
  }

  private coloredCurlies = true // color curlies by box BG
  private arcSlant = 15 // 10;
  private minArcSlant = 8
  private arcHorizontalSpacing = 10 // min space boxes with connecting arc
  private rowSpacing = -5 // for some funny reason approx. -10 gives "tight" packing.

  private sentNumMargin = 40
  private smoothArcCurves = true // whether to use curves (vs lines) in arcs
  private smoothArcSteepness = 0.5 // steepness of smooth curves (control point)
  private reverseArcControlx = 5 // control point distance for "UFO catchers"

  // "shadow" effect settings (note, error, incompelete)
  rectShadowSize = 3
  rectShadowRounding = 2.5
  arcLabelShadowSize = 1
  arcLabelShadowRounding = 5
  shadowStroke = 2.5 // TODO XXX: this doesn't affect anything..?

  // "marked" effect settings (edited, focus, match)
  markedSpanSize = 6
  markedArcSize = 2
  markedArcStroke = 7 // TODO XXX: this doesn't seem to do anything..?

  rowPadding = 2
  nestingAdjustYStepSize = 2 // size of height adjust for nested/nesting spans
  nestingAdjustXStepSize = 1 // size of height adjust for nested/nesting spans

  shadowClassPattern = 'True_positive|False_positive|False_negative|AnnotationError|AnnotationWarning|AnnotatorNotes|Normalized|AnnotationIncomplete|AnnotationUnconfirmed|rectEditHighlight|EditHighlight_arc|MissingAnnotation|ChangedAnnotation'

  highlightSpanSequence: string
  highlightArcSequence: string
  highlightTextSequence: string
  // different sequence for "mere" matches (as opposed to "focus" and "edited" highlights)
  highlightMatchSequence = '#FFFF00' // plain yellow

  fragmentConnectorDashArray = '1,3,3,3'
  fragmentConnectorColor = '#000000'
  // END OPTIONS

  commentPrioLevels: string[] = [
    'Unconfirmed', 'Incomplete', 'Warning', 'Error', 'AnnotatorNotes',
    'AddedAnnotation', 'MissingAnnotation', 'ChangedAnnotation'
  ]

  renderErrors: Record<string, boolean> = {
    unableToReadTextFile: true,
    annotationFileNotFound: true,
    isDirectoryError: true
  }

  constructor (dispatcher, svgId: string) {
    console.debug('Setting up brat visualizer module...')

    this.dispatcher = dispatcher

    const svgContainer = document.getElementById(svgId)
    if (!svgContainer) {
      throw Error('Could not find container with id="' + svgId + '"')
    }
    this.svgContainer = svgContainer

    // var highlightSequence = '#FFFC69;#FFCC00;#FFFC69'; // a bit toned town
    const highlightSequence = '#FF9632;#FFCC00;#FF9632' // yellow - deep orange
    this.highlightSpanSequence = highlightSequence
    this.highlightArcSequence = highlightSequence
    this.highlightTextSequence = highlightSequence

    // create the svg wrapper
    this.svgContainer.style.visibility = 'hidden'

    this.svg = SVG().addTo(this.svgContainer)
    this.triggerRender()

    this.registerHandlers(this.svgContainer, [
      'mouseover', 'mouseout', 'mousemove', 'mouseup', 'mousedown',
      'dragstart', 'dblclick', 'click',
      'contextmenu'
    ])

    this.registerHandlers(document, [
      'keydown', 'keypress',
      'touchstart', 'touchend'
    ])

    this.registerHandlers(window, [
      'resize'
    ])

    this.dispatcher //
      .on('collectionChanged', this, this.collectionChanged) //
      .on('collectionLoaded', this, this.collectionLoaded) //
      .on('renderData', this, this.renderData) //
      .on('rerender', this, this.rerender) //
      .on('renderDataPatch', this, this.renderDataPatch) //
      .on('triggerRender', this, this.triggerRender) //
      .on('requestRenderData', this, this.requestRenderData) //
      .on('isReloadOkay', this, this.isReloadOkay) //
      .on('resetData', this, this.resetData) //
      .on('abbrevs', this, this.setAbbrevs) //
      .on('textBackgrounds', this, this.setTextBackgrounds) //
      .on('layoutDensity', this, this.setLayoutDensity) //
      .on('svgWidth', this, this.setSvgWidth) //
      .on('loadAnnotations', this, this.loadAnnotations) //
      .on('mouseover', this, this.onMouseOver) //
      .on('mouseout', this, this.onMouseOut) //
      .on('selectionStarted', this, this.onSelectionStarted) //
      .on('selectionEnded', this, this.onSelectionEnded)

    // Object.seal(this);
  }

  scrollTo (args: { offset: number, position?: string, pingRanges?: Offsets[] }): void {
    const chunk = this.findChunkClosestToOffset(args.offset - (this.sourceData?.windowBegin || 0))

    if (!chunk) {
      console.warn('Could not find chunk at offset', args.offset)
      return
    }

    const chunkElement = this.getChunkElementWithId(chunk.index)
    if (chunkElement) {
      // console.log('Scrolling to ', chunkElement)
      chunkElement.scrollIntoView({ behavior: 'smooth', block: 'center' })

      // Remove any current pings
      this.svg.node.querySelectorAll('.ping').forEach((ping) => ping.remove())

      // Render new pings
      if (args.pingRanges) {
        for (const range of args.pingRanges) {
          this.renderPingMarker([range[0] - (this.sourceData?.windowBegin || 0), 
            range[1] - (this.sourceData?.windowBegin || 0)])
        }
      }

      // const ping = this.svg.rect(this.data?.sizes.texts.widths[chunk.text], this.data?.sizes.texts.height)
      //   .addClass('ping')
      //   .move(chunk.textX, chunk.row.textY + (this.data?.sizes.texts.y || 0))
      //   .addTo(this.highlightGroup)
      // ping.animate(5000, 0, 'now').attr({ opacity: 0 }).after(() => ping.remove())
    }
  }

  renderPingMarker(range: Offsets): void {
    const overlappingChunks = this.findChunksInRange(range);
  
    for (const chunk of overlappingChunks) {
      const ping = this.svg
        .rect(this.data?.sizes.texts.widths[chunk.text], this.data?.sizes.texts.height)
        .addClass('ping')
        .move(chunk.textX, chunk.row.textY + (this.data?.sizes.texts.y || 0))
        .addTo(this.highlightGroup);
  
      ping
        .animate(5000, 0, 'now')
        .attr({ opacity: 0 })
        .after(() => ping.remove());
    }
  }
  /**
   * Get the priority of the given comment class.
   *
   * @returns a numerical value representing the priority.
   */
  commentPriority (commentClass: string) {
    if (commentClass === undefined) {
      return -1
    }

    const len = this.commentPrioLevels.length
    for (let i = 0; i < len; i++) {
      if (commentClass.indexOf(this.commentPrioLevels[i]) !== -1) {
        return i
      }
    }

    return 0
  }

  setMarked (docData: DocumentData, sourceData: SourceData, markedType: MarkerType) {
    if (!this.data || !this.args[markedType]) {
      return
    }

    for (const marker of this.args[markedType]) {
      // Sentence marker
      if (marker[0] === 'sent') {
        docData.markedSent[marker[1]] = true
        continue
      }

      // INCEpTION does not use equivs, so we should not need this
      // if (marker[0] === 'equiv') { // [equiv, Equiv, T1]
      //   for (const equiv of sourceData.equivs) {
      //     if (equiv[1] === marker[1]) {
      //       let len = equiv.length
      //       for (let i = 2; i < len; i++) {
      //         if (equiv[i] === marker[2]) {
      //           // found it
      //           len -= 3
      //           for (let n = 1; n <= len; n++) {
      //             const arc = docData.eventDescs[equiv[0] + '*' + n].equivArc
      //             arc.marked = markedType
      //           }
      //           continue // next equiv
      //         }
      //       }
      //     }
      //   }

      //   continue
      // }

      if (!Array.isArray(marker)) {
        continue
      }

      // Text marker
      if (marker.length === 2) {
        const begin = parseInt(marker[0], 10)
        const end = parseInt(marker[1], 10)
        this.markedText.push([begin, end, markedType])
        continue
      }

      // Annotation marker (span or arc)
      const span = this.data.spans[marker[0]]
      if (span) {
        if (marker.length === 3) { // arc
          for (const arc of span.outgoing) {
            if (arc.target === marker[2] && arc.type === marker[1]) {
              arc.marked = markedType;
            }
          }
        } else { // span
          span.marked = markedType
        }

        continue
      }

      const eventDesc = this.data.eventDescs[marker[0]]
      if (eventDesc) { // relation
        for (const arc of this.data.spans[eventDesc.triggerId].outgoing) {
          if (arc.eventDescId === marker[0]) {
            arc.marked = markedType
          }
        }

        continue
      }

      // try for trigger
      for (const eventDesc of Object.values(this.data.eventDescs)) {
        if (eventDesc.triggerId === marker[0]) {
          this.data.spans[eventDesc.id].marked = markedType
        }
      }
    }
  }

  findArcHeight (fromIndex: number, toIndex: number, fragmentHeights: number[]) {
    let height = 0
    for (let i = fromIndex; i <= toIndex; i++) {
      if (fragmentHeights[i] > height) { height = fragmentHeights[i] }
    }
    height += Configuration.visual.arcSpacing * (this.fontZoom / 50.0)
    return height
  }

  adjustFragmentHeights (fromIndex: number, toIndex: number, fragmentHeights: number[], height: number) {
    for (let i = fromIndex; i <= toIndex; i++) {
      if (fragmentHeights[i] < height) { fragmentHeights[i] = height }
    }
  }

  applyHighlighting (docData: DocumentData, sourceData: SourceData) {
    this.markedText = []
    this.setMarked(docData, sourceData, EDITED) // set by editing process
    this.setMarked(docData, sourceData, MATCH_FOCUS) // set by search process, focused match
    this.setMarked(docData, sourceData, MATCH) // set by search process, other (non-focused) match
    this.setMarked(docData, sourceData, WARN) // set by editing process
    this.setMarked(docData, sourceData, FOCUS) // set by URL
  }

  /**
   * Calculate average arc distances. Average distance of arcs (0 for no arcs).
   */
  calculateAverageArcDistances (spans: Entity[]) {
    for (const span of spans) {
      span.avgDist = span.numArcs ? span.totalDist / span.numArcs : 0
    }
  }

  /**
   * Collect fragment texts into span texts
   */
  collectFragmentTextsIntoSpanTexts (spans: Entity[]) {
    for (const span of spans) {
      const fragmentTexts : string[] = []
      for (const fragment of (span.fragments || [])) {
        fragmentTexts.push(fragment.text)
      }
      span.text = fragmentTexts.join('')
    }
  }

  /**
   * @returns list of span IDs in the order they should be drawn.
   */
  determineDrawOrder (spans: Record<VID, Entity>): Array<VID> {
    const spanDrawOrderPermutation = Object.keys(spans)
    spanDrawOrderPermutation.sort((a, b) => {
      const spanA = spans[a]
      const spanB = spans[b]

      // We're jumping all over the chunks, but it's enough that we're doing everything inside
      // each chunk in the right order. Should it become necessary to actually do these in
      // linear order, put in a similar condition for spanX.headFragment.chunk.index; but it
      // should not be needed.
      const tmp = spanA.headFragment.drawOrder - spanB.headFragment.drawOrder
      if (tmp) {
        return tmp < 0 ? -1 : 1
      }

      return 0
    })
    return spanDrawOrderPermutation
  }

  organizeFragmentsIntoTowers (sortedFragments: Array<Fragment>) {
    let lastFragment : Fragment | undefined
    let towerId = -1

    for (const fragment of sortedFragments) {
      if (!lastFragment || (lastFragment.from !== fragment.from || lastFragment.to !== fragment.to)) {
        towerId++
      }
      fragment.towerId = towerId
      lastFragment = fragment
    }
  }

  applyMarkedTextToChunks (markedText: Array<MarkedText>, chunks: Array<Chunk>) {
    const numChunks = chunks.length
    // note the location of marked text with respect to chunks
    let startChunk = 0
    let currentChunk: number
    $.each(markedText, (textNo, textPos) => {
      let from = textPos[0]
      let to = textPos[1]
      const markedType = textPos[2]

      if (from < 0) { from = 0 }
      if (to < 0) { to = 0 }
      if (to >= this.data.text.length) { to = this.data.text.length - 1 }
      if (from > to) { from = to }

      while (startChunk < numChunks) {
        const chunk = chunks[startChunk]
        if (from <= chunk.to) {
          chunk.markedTextStart.push([textNo, true, from - chunk.from, null, markedType])
          break
        }
        startChunk++
      }

      if (startChunk === numChunks) {
        this.dispatcher.post('messages', [[['Wrong text offset', 'error']]])
        return
      }

      currentChunk = startChunk
      while (currentChunk < numChunks) {
        const chunk = chunks[currentChunk]
        if (to <= chunk.to) {
          chunk.markedTextEnd.push([textNo, false, to - chunk.from])
          break
        }
        currentChunk++
      }

      if (currentChunk === numChunks) {
        this.dispatcher.post('messages', [[['Wrong text offset', 'error']]])
        const chunk = chunks[chunks.length - 1]
        chunk.markedTextEnd.push([textNo, false, chunk.text.length])
      }
    }) // markedText
  }

  buildSpansFromEntities (documentText: string, entities: Array<EntityDto>): Record<VID, Entity> {
    if (!entities) {
      return {}
    }

    const spans: Record<VID, Entity> = {}
    for (const entity of entities) {
      const id = entity[0]
      const type = entity[1]
      const offsets = entity[2] // offsets given as array of (start, end) pairs
      const span = new Entity(id, type, offsets, ENTITY)

      if (entity[3]) {
        const attributes = entity[3]
        if (Object.prototype.hasOwnProperty.call(attributes, 'l')) {
          span.labelText = attributes.l
        }
        if (Object.prototype.hasOwnProperty.call(attributes, 'c')) {
          span.color = attributes.c
        }
        if (Object.prototype.hasOwnProperty.call(attributes, 'h')) {
          span.hovertext = attributes.h
        }
        if (Object.prototype.hasOwnProperty.call(attributes, 'a')) {
          span.actionButtons = !!(attributes.a)
        }
        if (Object.prototype.hasOwnProperty.call(attributes, 's')) {
          span.score = attributes.s
        }
        if (Object.prototype.hasOwnProperty.call(attributes, 'cl') && attributes.cl) {
          span.clippedAtStart = attributes.cl.startsWith('s')
          span.clippedAtEnd = attributes.cl.endsWith('e')
        }
      }

      span.splitMultilineOffsets(documentText)

      spans[id] = span
    }

    return spans
  }

  /**
   * @deprecated Triggers are not used by INCEptION
   */
  buildSpansFromTriggers (triggers: Array<TriggerDto>): Record<string, [Entity, Array<EventDesc>]> {
    if (!triggers || !this.data) {
      return {}
    }

    const triggerHash: Record<string, [Entity, Array<EventDesc>]> = {}
    for (const trigger of triggers) {
      //                          (id,         type,       offsets,    generalType)
      const triggerSpan = new Entity(trigger[0], trigger[1], trigger[2], TRIGGER)

      triggerSpan.splitMultilineOffsets(this.data.text)

      triggerHash[trigger[0]] = [triggerSpan, []] // triggerSpan, eventlist
    }

    return triggerHash
  }

  /**
   * @deprecated Not used by INCEpTION
   */
  private buildEventDescsFromTriggers (docData: DocumentData, sourceData: SourceData, triggerHash: Record<string, [Entity, Array<EventDesc>]>) {
    if (!triggerHash) {
      return
    }

    for (const eventRow of sourceData.events) {
      const id = eventRow[0]
      const triggerId = eventRow[1]
      const roles = eventRow[2]

      const eventDesc = docData.eventDescs[id] = new EventDesc(id, triggerId, roles)
      const trigger = triggerHash[eventDesc.triggerId]
      const span = trigger[0].copy(eventDesc.id)
      trigger[1].push(span)
      docData.spans[eventDesc.id] = span
    }
  }

  private splitSpansIntoFragments (spans: Entity[]) {
    if (!spans) {
      return
    }

    spans.map(span => span.buildFragments())
  }

  /**
   * @deprecated INCEpTION does not use equivs
   */
  private buildEventDescsFromEquivs (equivs: Array<EquivDto>, spans: Record<VID, Entity>, eventDescs: Record<VID, EventDesc>) {
    if (!equivs) {
      return
    }

    $.each(equivs, (equivNo, equiv) => {
      // equiv: ['*', 'Equiv', spanId...]
      equiv[0] = '*' + equivNo
      const equivSpans = equiv.slice(2)
      const okEquivSpans: Array<VID> = []

      // collect the equiv spans in an array
      for (const equivSpan of equivSpans) {
        if (spans[equivSpan]) {
          okEquivSpans.push(equivSpan)
        }
        // TODO: #404, inform the user with a message?
      }

      // sort spans in the equiv by their midpoint
      okEquivSpans.sort((a, b) => Entity.compare(spans, a, b))

      // generate the arcs
      const len = okEquivSpans.length
      for (let i = 1; i < len; i++) {
        const id = okEquivSpans[i - 1]
        const tiggerId = okEquivSpans[i - 1]
        const roles: Array<RoleDto> = [[parseInt(equiv[1]), okEquivSpans[i]]]
        const eventDesc = eventDescs[equiv[0] + '*' + i] = new EventDesc(id, tiggerId, roles, EQUIV)
        eventDesc.leftSpans = okEquivSpans.slice(0, i)
        eventDesc.rightSpans = okEquivSpans.slice(i)
      }
    })
  }

  buildEventDescsFromRelations (relations: Array<RelationDto>, eventDescs: Record<string, EventDesc>) {
    if (!relations) {
      return
    }

    for (const rel of relations) {
      // rel[2] is args, rel[2][a][0] is role and rel[2][a][1] is value for a in (0,1)
      let argsDesc = this.relationTypes[rel[1]]
      argsDesc = argsDesc && argsDesc.args

      let t1, t2
      if (argsDesc) {
        // sort the arguments according to the config
        const args = {}
        args[rel[2][0][0]] = rel[2][0][1]
        args[rel[2][1][0]] = rel[2][1][1]
        t1 = args[argsDesc[0].role]
        t2 = args[argsDesc[1].role]
      } else {
        // (or leave as-is in its absence)
        t1 = rel[2][0][1]
        t2 = rel[2][1][1]
      }

      //                                (id, triggerId, roles,   klass)
      eventDescs[rel[0]] = new EventDesc(t1, t1, [[rel[1], t2]], RELATION)

      if (rel[3]) {
        eventDescs[rel[0]].labelText = rel[3]
      }

      if (rel[4]) {
        eventDescs[rel[0]].color = rel[4]
      }
    }
  }

  /**
   * @deprecated INCEpTION does not use attributes
   */
  private assignAttributesToSpans (attributes: Array<AttributeDto>, spans: Record<VID, Entity>) {
    if (!attributes) {
      return
    }

    for (const attr of attributes) {
      const id = attr[0]
      const name = attr[1]
      const spanId = attr[2]
      const value = attr[3]

      // TODO: might wish to check what's appropriate for the type
      // instead of using the first attribute def found
      const attrType = (this.eventAttributeTypes[name] || this.entityAttributeTypes[name])
      const attrValue = attrType && attrType.values[attrType.bool || value]
      const span = spans[spanId]

      if (!span) {
        this.dispatcher.post('messages', [[['Annotation ' + spanId + ', referenced from attribute ' + id + ', does not exist.', 'error']]])
        continue
      }

      const valText = (attrValue && attrValue.name) || value
      const attrText = attrType
        ? (attrType.bool ? attrType.name : (attrType.name + ': ' + valText))
        : (value ? name : name + ': ' + value)
      span.attributeText.push(attrText)
      span.attributes[name] = value

      Object.assign(span.attributeMerge, attrValue)
    }
  }

  private assignComments (docData: DocumentData, comments: Array<CommentDto>, triggerHash: Record<string, unknown>) {
    if (!comments) {
      return
    }

    for (const comment of comments) {
      // comment: [entityId, type, text]
      // TODO error handling
      // sentence id: ['sent', sentId]
      if (comment[0] instanceof Array && comment[0][0] === 'sent') {
        // sentence comment
        const sent = comment[0][1]
        const id = comment[0][2]
        let text = comment[2]
        if (docData.sentComment[sent]) {
          text = docData.sentComment[sent].text + '<br/>' + text
        }
        docData.sentComment[sent] = { id, type: comment[1], text }
        continue
      }

      const id = (comment[0] as string)
      const trigger = triggerHash[id]
      const commentEntities = trigger
        ? trigger[1] // trigger: [span, ...]
        : id in docData.spans
          ? [docData.spans[id]] // span: [span]
          : id in docData.eventDescs
            ? [docData.eventDescs[id]] // arc: [eventDesc]
            : []

      for (const entity of commentEntities) {
        // if duplicate comment for entity:
        // overwrite type, concatenate comment with a newline
        if (!entity.comment) {
          entity.comment = { type: comment[1], text: comment[2] }
        } else {
          entity.comment.type = comment[1]
          entity.comment.text += '\n' + comment[2]
        }

        // partially duplicate marking of annotator note comments
        if (comment[1] === 'AnnotatorNotes') {
          entity.annotatorNotes = comment[2]
        }

        // prioritize type setting when multiple comments are present
        if (this.commentPriority(comment[1]) > this.commentPriority(entity.shadowClass)) {
          entity.shadowClass = comment[1]
        }
      }
    }
  }

  buildSortedFragments (spans: Record<string, Entity>): Fragment[] {
    if (!spans) {
      return []
    }

    const sortedFragments : Fragment[] = []

    Object.values(spans).map((span: Entity) => span.fragments.map(
      fragment => sortedFragments.push(fragment)))

    sortedFragments.sort(function (a, b) {
      let x = a.from
      let y = b.from
      if (x === y) {
        x = a.to
        y = b.to
      }
      return ((x < y) ? -1 : ((x > y) ? 1 : 0))
    })

    return sortedFragments
  }

  /**
   * @param {number[][]} tokenOffsets
   * @param {Fragment[]} sortedFragments
   * @return {Chunk[]}
   */
  buildChunksFromTokenOffsets (docData: DocumentData, tokenOffsets: number[][], sortedFragments: Fragment[]): Chunk[] {
    if (!tokenOffsets) {
      return []
    }

    let currentFragmentId = 0
    let startFragmentId = 0
    const numFragments = sortedFragments.length
    let lastTo = 0
    let firstFrom: number | null = null
    let chunkNo = 0
    let space: string
    let chunk: Chunk | null = null
    const chunks : Chunk[] = []

    for (const fragment of sortedFragments) {
      if (fragment.span.id === 'rel:0-before') {
        chunk = new Chunk(chunkNo++, '', fragment.from, fragment.to, '')
        chunk.virtual = true
        fragment.chunk = chunk
        chunk.fragments.push(fragment)
        chunks.push(chunk)
        break;
      }
    }

    for (const [from, to] of tokenOffsets) {
      if (firstFrom === null) {
        firstFrom = from
      }

      // Is the token end inside a span annotation / fragment?
      if (startFragmentId && to > sortedFragments[startFragmentId - 1].to) {
        while (startFragmentId < numFragments && to > sortedFragments[startFragmentId].from) {
          startFragmentId++
        }
      }
      currentFragmentId = startFragmentId
      while (currentFragmentId < numFragments && to >= sortedFragments[currentFragmentId].to) {
        currentFragmentId++
      }
      // if yes, the next token is in the same chunk
      if (currentFragmentId < numFragments && to > sortedFragments[currentFragmentId].from) {
        continue
      }

      // otherwise, create the chunk found so far
      space = docData.text.substring(lastTo, firstFrom)
      const text = docData.text.substring(firstFrom, to)
      if (chunk) {
        chunk.nextSpace = space
      }

      //               (index,     text, from,      to, space) {
      chunk = new Chunk(chunkNo++, text, firstFrom, to, space)
      chunk.lastSpace = space
      chunks.push(chunk)
      lastTo = to
      firstFrom = null
    }

    for (const fragment of sortedFragments) {
      if (fragment.span.id === 'rel:1-after') {
        chunk = new Chunk(chunkNo++, '', fragment.from, fragment.to, '')
        chunk.virtual = true
        fragment.chunk = chunk
        chunk.fragments.push(fragment)
        chunks.push(chunk)
        break;
      }
    }

    return chunks
  }

  assignSentenceNumbersToChunks (firstSentence: number, sentenceOffsets: Offsets[], chunks: Chunk[]) {
    if (!sentenceOffsets) {
      return
    }

    const numChunks = chunks.length
    let chunkNo = 0
    let sentenceNo = firstSentence

    for (const [sentFrom, sentTo] of sentenceOffsets) {
      // Skip all chunks that belonged to the previous sentence
      let chunk : Chunk | undefined
      while (chunkNo < numChunks && (chunk = chunks[chunkNo]).from < sentFrom) {
        chunkNo++
      }

      // No more chunks
      if (chunkNo >= numChunks) {
        break
      }

      // If the current chunk is not within the current sentence, then it was an empty sentence
      if (!covering(sentFrom, sentTo, chunks[chunkNo].from, chunks[chunkNo].to)) {
        sentenceNo++
        continue
      }

      sentenceNo++
      if (chunk) chunk.sentence = sentenceNo
      // console.trace("ASSIGN: line break ", sentenceNo ," at ", chunk);
      // increase chunkNo counter for next seek iteration
      chunkNo++
    }
  }

  assignFragmentsToChunks (chunks: Chunk[], sortedFragments: Fragment[]) {
    if (!sortedFragments) {
      return
    }

    // Avoid assigining fragments to virtual chunks link those created for rel:0-before and rel:1-after
    chunks = chunks.filter(chunk => !chunk.virtual)

    for (const fragment of sortedFragments) {
      // The before and after fragments have already been assigned to their own chunks in 
      // buildChunksFromTokenOffsets
      if (fragment.span.id === 'rel:0-before' || fragment.span.id === 'rel:1-after') continue

      let chunk = chunks.find(c => overlapping(c.from, c.to, fragment.from, fragment.to));

      if (!chunk) {
        console.warn('Could not find chunk for fragment', fragment);
        continue
      }

      chunk.fragments.push(fragment)
      fragment.text = chunk.text.substring(fragment.from - chunk.from, fragment.to - chunk.from)
      fragment.chunk = chunk
    }
  }

  /**
   * Builds the args based on the EventDescs and links them up to the spans.
   *
   * Side effects:
   * - Fields on spans are changed: totalDist, numArcs, outgoing, incoming
   * - data.arcById index is populated.
   */
  assignArcsToSpans (docData: DocumentData, eventDescs: Record<string, EventDesc>, spans: Record<string, Entity>): Arc[] {
    if (!eventDescs || !spans) {
      return []
    }

    const arcs : Arc[] = []

    for (const [eventNo, eventDesc] of Object.entries(eventDescs)) {
      const origin = spans[eventDesc.id]

      if (!origin) {
        // TODO: include missing trigger ID in error message
        this.dispatcher.post('messages', [[['<strong>ERROR</strong><br/>Trigger for event "' + eventDesc.id + '" not found <br/>(please correct the source data)', 'error', 5]]])
        continue
      }

      const here = origin.headFragment.from + origin.headFragment.to
      for (const role of eventDesc.roles) {
        const target = spans[role.targetId]
        if (!target) {
          this.dispatcher.post('messages', [[['<strong>ERROR</strong><br/>"' + role.targetId + '" (referenced from "' + eventDesc.id + '") not found <br/>(please correct the source data)', 'error', 5]]])
          continue
        }

        const there = target.headFragment.from + target.headFragment.to
        const dist = Math.abs(here - there)
        const arc = new Arc(eventDesc, role, dist, eventNo)

        origin.totalDist += dist
        origin.numArcs++
        origin.outgoing.push(arc)

        target.totalDist += dist
        target.numArcs++
        target.incoming.push(arc)

        arcs.push(arc)
        docData.arcById[arc.eventDescId] = arc
      } // roles
    } // eventDescs

    return arcs
  }

  /**
   * Populates the "data" field based on the "sourceData" JSON that we received from the server.
   */
  setData (sourceData: SourceData) {
    this.sourceData = sourceData
    this.rtlmode = this.sourceData.rtl_mode
    this.fontZoom = this.sourceData.font_zoom
    this.args = this.sourceData.args ?? {}

    this.dispatcher.post('newSourceData', [this.sourceData])

    this.data = new DocumentData(this.sourceData.text)

    // collect annotation data
    this.data.spans = this.buildSpansFromEntities(this.data.text, this.sourceData.entities)

    // REC 2021-06-29: Not sure if we need this at all since INCEpTION only uses "entities" and
    // doesn't do brat-style events/trigger. We prepare spans-with-slots on the server side
    // already and render them as brat-entities and brat-relations
    const triggerHash = this.buildSpansFromTriggers(this.sourceData.triggers)
    this.buildEventDescsFromTriggers(this.data, this.sourceData, triggerHash)

    // split span annotations into span fragments (for discontinuous spans)
    this.splitSpansIntoFragments(Object.values(this.data.spans))
    this.buildEventDescsFromEquivs(this.sourceData.equivs, this.data.spans, this.data.eventDescs)
    this.buildEventDescsFromRelations(this.sourceData.relations, this.data.eventDescs)
    this.assignAttributesToSpans(this.sourceData.attributes, this.data.spans)
    this.assignComments(this.data, this.sourceData.comments, triggerHash)

    // prepare span boundaries for token containment testing
    // sort fragments by beginning, then by end
    const sortedFragments = this.buildSortedFragments(this.data.spans)

    // token containment testing (chunk recognition)
    this.data.chunks = this.buildChunksFromTokenOffsets(this.data, this.sourceData.token_offsets, sortedFragments)

    this.assignSentenceNumbersToChunks(this.sourceData.sentence_number_offset - 1,
      this.sourceData.sentence_offsets, this.data.chunks)
    this.assignFragmentsToChunks(this.data.chunks, sortedFragments)
    this.data.arcs = this.assignArcsToSpans(this.data, this.data.eventDescs, this.data.spans)
    this.applyHighlighting(this.data, this.sourceData)

    if (this.data.spans) {
      this.calculateAverageArcDistances(Object.values(this.data.spans))
      this.collectFragmentTextsIntoSpanTexts(Object.values(this.data.spans))
    }

    this.calculateFragmentDrawingOrder(this.data, sortedFragments)

    this.dispatcher.post('dataReady', [this.data])
  }

  private renderText (docData: DocumentData) {
    const spanAnnTexts = {}
    for (const chunk of docData.chunks) {
      chunk.markedTextStart = []
      chunk.markedTextEnd = []

      for (const fragment of chunk.fragments) {
        if (chunk.firstFragmentIndex === undefined) {
          chunk.firstFragmentIndex = fragment.towerId
        }
        chunk.lastFragmentIndex = fragment.towerId

        this.updateFragmentLabelText(fragment)

        const svgtext = this.svg.plain('').build(true) // one "text" element per row
        const postfixArray : [AttributeType, ValType][] = []
        let prefix = ''
        let postfix = ''
        let warning = false
        for (const [attrType, valType] of Object.entries(fragment.span.attributes)) {
          // TODO: might wish to check what's appropriate for the type
          // instead of using the first attribute def found
          const attr = (this.eventAttributeTypes[attrType] || this.entityAttributeTypes[attrType])
          if (!attr) {
            // non-existent type
            warning = true
            continue
          }

          const val = attr.values[attr.bool || valType]
          if (!val) {
            // non-existent value
            warning = true
            continue
          }

          if ($.isEmptyObject(val)) {
            // defined, but lacks any visual presentation
            warning = true
            continue
          }

          if (val.glyph) {
            if (val.position === 'left') {
              prefix = val.glyph + prefix

              const t = svgtext.tspan(val.glyph)
              t.addClass('glyph')
              if (val.glyphColor) {
                t.fill(val.glyphColor)
              }
            } else { // XXX right is implied - maybe change
              postfixArray.push([attr, val])
              postfix += val.glyph
            }
          }
        }

        let text = fragment.labelText
        if (prefix !== '') {
          text = prefix + ' ' + text
          svgtext.plain(' ')
        }

        svgtext.plain(fragment.labelText)
        if (postfixArray.length) {
          text += ' ' + postfix
          svgtext.plain(' ')
          for (const el of postfixArray) {
            const t = svgtext.tspan(el[1].glyph)
            t.addClass('glyph')
            if (el[1].glyphColor) {
              t.fill(el[1].glyphColor)
            }
          }
        }

        if (warning) {
          svgtext.tspan('#').addClass('glyph').addClass('attribute_warning')
          text += ' #'
        }
        fragment.glyphedLabelText = text

        if (!spanAnnTexts[text]) {
          spanAnnTexts[text] = true
          docData.spanAnnTexts[text] = svgtext
        }
      }
    }
  }

  private updateFragmentLabelText (fragment: Fragment) {
    const spanLabels = Util.getSpanLabels(this.entityTypes, fragment.span.type)
    fragment.labelText = Util.spanDisplayForm(this.entityTypes, fragment.span.type)

    // Find the most appropriate label according to text width
    if (Configuration.abbrevsOn && spanLabels) {
      let labelIdx = 1 // first abbrev
      const maxLength = (fragment.to - fragment.from) / 0.8
      while (fragment.labelText.length > maxLength &&
        spanLabels[labelIdx]) {
        fragment.labelText = spanLabels[labelIdx]
        labelIdx++
      }
    }

    if (fragment.span.labelText) {
      fragment.labelText = fragment.span.labelText
    }
    else {
      fragment.labelText = '(' + fragment.labelText + ')'
    }

    if (fragment.span.score) {
      fragment.labelText += ' [' + fragment.span.score.toFixed(2) + ']'
    }
  }

  private calculateFragmentDrawingOrder (docData: DocumentData, sortedFragments: Fragment[]) {
    for (let i = 0; i < 2; i++) {
      // preliminary sort to assign heights for basic cases
      // (first round) and cases resolved in the previous round(s).
      for (const chunk of docData.chunks) {
        // sort and then re-number
        chunk.fragments.sort(Fragment.compare)
        for (const [fragmentNo, fragment] of chunk.fragments.entries()) {
          fragment.indexNumber = fragmentNo
        }
      }

      // nix the sums, so we can sum again
      for (const span of Object.values(docData.spans)) {
        span.refedIndexSum = 0
      }

      // resolved cases will now have indexNumber set to indicate their relative order. Sum those
      // for referencing cases for use in iterative resorting
      for (const arc of docData.arcs) {
        docData.spans[arc.origin].refedIndexSum += docData.spans[arc.target].headFragment.indexNumber
      }
    }

    // Final sort of fragments in chunks for drawing purposes
    // Also identify the marked text boundaries regarding chunks
    for (const chunk of docData.chunks) {
      // and make the next sort take this into account. Note that this will
      // now resolve first-order dependencies between sort orders but not
      // second-order or higher.
      chunk.fragments.sort(Fragment.compare)
      for (const [fragmentNo, fragment] of chunk.fragments.entries()) {
        fragment.drawOrder = fragmentNo
      }
    }

    docData.spanDrawOrderPermutation = this.determineDrawOrder(docData.spans)

    // resort the fragments for linear order by center so we can organize them into towers
    sortedFragments.sort(Fragment.midpointComparator)
    // sort fragments into towers, calculate average arc distances
    this.organizeFragmentsIntoTowers(sortedFragments)

    // find curlies (only the first fragment drawn in a tower)
    for (const spanId of docData.spanDrawOrderPermutation) {
      const span = docData.spans[spanId]

      for (const fragment of span.fragments) {
        if (!docData.towers[fragment.towerId]) {
          docData.towers[fragment.towerId] = []
          fragment.drawCurly = true
          fragment.span.drawCurly = true
        }
        docData.towers[fragment.towerId].push(fragment)
      }
    }
  }

  resetData () {
    if (this.sourceData) {
      this.setData(this.sourceData)
    }
    this.renderData(undefined)
  }

  fastTranslateGroup (element: SVGTypeMapping<SVGGElement>, x: number, y: number) {
    element.attr('transform', 'translate(' + x + ', ' + y + ')')
    // element.translate(x, y);
  }

  fastTranslate (element: Row | Chunk, x: number, y: number) {
    this.fastTranslateGroup(element.group, x, y)
    element.translation = { x, y }
  }

  addHeaderAndDefs (): Defs {
    const defs = this.svg.defs()

    const filter = defs.filter()
    filter.id('Gaussian_Blur')
    filter.gaussianBlur(2, 2)
    filter.addTo(defs)

    this.renderDragArcMarker(defs)

    return defs
  }

  /**
   * @param cssClass arguments to the {@link SVGWrapper.group}} call creating the temporary group used for measurement
   */
  getTextMeasurements (textsHash: Record<string, Array<unknown>>, cssClass?: string, callback?: Function): Measurements {
    // make some text elements, find out the dimensions
    const textMeasureGroup: Container = this.svg.group().addTo(this.svg)
    if (cssClass) {
      textMeasureGroup.addClass(cssClass)
    }

    for (const text in textsHash) {
      if (Object.prototype.hasOwnProperty.call(textsHash, text)) {
        this.svg.plain(text).addTo(textMeasureGroup)
      }
    }

    // measuring goes on here
    const widths: Record<string, number> = {}
    for (const svgText of textMeasureGroup.children()) {
      const text = (svgText as SVGText).text()
      widths[text] = (svgText.node as SVGTextContentElement).getComputedTextLength()

      if (callback) {
        textsHash[text].map(object => callback(object, svgText.node))
      }
    }

    // Add dummy element used only to get the text height even if we have no chunks
    this.svg.plain("TEXT").addTo(textMeasureGroup)

    const bbox = textMeasureGroup.bbox()
    textMeasureGroup.remove()

    return new Measurements(widths, bbox.height, bbox.y)
  }

  /**
   * @return {Sizes}
   */
  calculateTextMeasurements (docData: DocumentData): Sizes {
    const textSizes = this.calculateChunkTextMeasures(docData)
    const fragmentSizes = this.calculateFragmentTextMeasures(docData)
    const arcSizes = this.calculateArcTextMeasurements(docData)

    return new Sizes(textSizes, arcSizes, fragmentSizes)
  }

  calculateChunkTextMeasures (docData: DocumentData): Measurements {
    // get the span text sizes
    const chunkTexts: Record<string, Array<unknown>> = {} // set of span texts
    for (const chunk of docData.chunks) {
      chunk.row = undefined // reset
      if (!Object.prototype.hasOwnProperty.call(chunkTexts, chunk.text)) {
        chunkTexts[chunk.text] = []
      }

      // here we also need all the spans that are contained in chunks with this text, because we
      // need to know the position of the span text within the respective chunk text
      const chunkText = chunkTexts[chunk.text]
      chunkText.push(...chunk.fragments)
      // and also the markedText boundaries
      chunkText.push(...chunk.markedTextStart)
      chunkText.push(...chunk.markedTextEnd)
    }

    return this.getTextMeasurements(chunkTexts, undefined, (fragment, text) =>
      this.calculateChunkTextElementMeasure(fragment, text))
  }

  /**
   * measure the text position in pixels
   */
  private calculateChunkTextElementMeasure (fragment: Fragment | Marker, text: SVGTextElement) {
    if (!(fragment instanceof Fragment)) {
      // it's markedText [id, start?, char#, offset]
      if (fragment[2] < 0) { fragment[2] = 0 }

      // Adjust for the browser collapsing whitespace
      let lastCharSpace = true
      let collapsedSpaces = 0
      const tc = text.textContent || "";
      for (let i = 0; i < fragment[2]; i++) {
        const c = tc[i]
        if (/\s/.test(c)) {
          if (lastCharSpace) {
            collapsedSpaces++
          }
          lastCharSpace = true
        } else {
          lastCharSpace = false
        }
      }

      fragment[2] -= collapsedSpaces

      if (!fragment[2]) { // start
        fragment[3] = text.getStartPositionOfChar(fragment[2]).x
      } else {
        fragment[3] = text.getEndPositionOfChar(fragment[2] - 1).x + 1
      }
      return
    }

    let firstChar = fragment.from - fragment.chunk.from
    if (firstChar < 0) {
      firstChar = 0
      this.dispatcher.post('messages', [[['<strong>WARNING</strong>' +
          '<br/> ' +
          'The fragment [' + fragment.from + ', ' + fragment.to + '] (' + fragment.text + ') is not ' +
          'contained in its designated chunk [' +
          fragment.chunk.from + ', ' + fragment.chunk.to + '] most likely ' +
          'due to the fragment starting or ending with a space, please ' +
          'verify the sanity of your data since we are unable to ' +
          'visualise this fragment correctly and will drop leading ' +
          'space characters',
      'warning', 15]]])
    }
    let lastChar = fragment.to - fragment.chunk.from - 1

    // Adjust for XML whitespace (#832, #1009)
    const textUpToFirstChar = fragment.chunk.text.substring(0, firstChar)
    const textUpToLastChar = fragment.chunk.text.substring(0, lastChar)
    const textUpToFirstCharUnspaced = textUpToFirstChar.replace(/\s\s+/g, ' ')
    const textUpToLastCharUnspaced = textUpToLastChar.replace(/\s\s+/g, ' ')
    firstChar -= textUpToFirstChar.length - textUpToFirstCharUnspaced.length
    lastChar -= textUpToLastChar.length - textUpToLastCharUnspaced.length

    let startPos: number, endPos: number
    if (this.rtlmode) {
      // This rendering is much slower than the "old" version that brat uses, but it is more reliable in RTL mode.
      [startPos, endPos] = this.calculateSubstringPositionRobust(fragment, text, firstChar, lastChar)
      // In RTL mode, positions are negative (left to right)
      startPos = -startPos
      endPos = -endPos
    } else {
      // Using the old faster method in LTR mode. YES, this means that subtoken annotations of RTL
      // tokens in LTR mode will render incorrectly. If somebody needs that, we should do a smarter
      // selection of the rendering mode. This is the old measurement code which doesn't work
      // properly because browsers treat the x coordinate very differently. Our width-based
      // measurement is more reliable.
      // Cannot use fragment.chunk.text.length here because invisible
      // characters do not count. Using text.getNumberOfChars() instead.
      [startPos, endPos] = this.calculateSubstringPositionFast(text, firstChar, lastChar)
    }

    // Make sure that startPos and endPos are properly ordered on the X axis
    fragment.curly = {
      from: Math.min(startPos, endPos),
      to: Math.max(startPos, endPos)
    }
  }

  private calculateSubstringPositionRobust (fragment: Fragment, text: SVGTextElement, firstChar: number, lastChar: number): [number, number] {
    if (text.textContent === null || text.textContent.length === 0) {
      return [0, 0]
    }

    let charDirection: Array<'rtl' | 'ltr'>
    let charAttrs: Array<{ order: number, width: number, direction: 'rtl' | 'ltr' }>
    let corrFactor = 1

    if (fragment.chunk.rtlsizes) {
      // Use cached metrics
      charDirection = fragment.chunk.rtlsizes.charDirection
      charAttrs = fragment.chunk.rtlsizes.charAttrs
      corrFactor = fragment.chunk.rtlsizes.corrFactor
    } else {
      // Calculate metrics
      charDirection = []
      charAttrs = []

      // Cannot use fragment.chunk.text.length here because invisible characters do not count.
      // Using text.getNumberOfChars() instead.
      for (let idx = 0; idx < text.getNumberOfChars(); idx++) {
        const cw = text.getEndPositionOfChar(idx).x - text.getStartPositionOfChar(idx).x
        const dir = isRTL(text.textContent.charCodeAt(idx)) ? 'rtl' : 'ltr'
        charAttrs.push({
          order: idx,
          width: Math.abs(cw),
          direction: dir
        })
        charDirection.push(dir)
        // console.log("char " +  idx + " [" + text.textContent[idx] + "] " +
        //   "begin:" + text.getStartPositionOfChar(idx).x +
        //   " end:" + text.getEndPositionOfChar(idx).x +
        //   " width:" + Math.abs(cw) +
        //   " dir:" + charDirection[charDirection.length-1]);
      }

      // Re-order widths if necessary
      if (charAttrs.length > 1) {
        const idx = 0
        let blockBegin = idx
        let blockEnd = idx

        // Figure out next block
        while (blockEnd < charAttrs.length) {
          while (charDirection[blockBegin] === charDirection[blockEnd]) {
            blockEnd++
          }

          if (charDirection[blockBegin] === (this.rtlmode ? 'ltr' : 'rtl')) {
            charAttrs = charAttrs.slice(0, blockBegin)
              .concat(charAttrs.slice(blockBegin, blockEnd).reverse())
              .concat(charAttrs.slice(blockEnd))
          }

          blockBegin = blockEnd
        }
      }

      // console.log("order: " + charOrder);
      // The actual character width on screen is not necessarily the width that can be
      // obtained by subtracting start from end position. In particular Arabic connects
      // characters quite a bit such that the width on screen may be less. Here we
      // try to compensate for this using a correction factor.
      let widthsSum = 0
      for (let idx = 0; idx < charAttrs.length; idx++) {
        widthsSum += charAttrs[idx].width
      }
      corrFactor = text.getComputedTextLength() / widthsSum
      // console.log("width sums: " + widthsSum);
      // console.log("computed length: " + text.getComputedTextLength());
      // console.log("corrFactor: " + corrFactor);
      fragment.chunk.rtlsizes = {
        charDirection,
        charAttrs,
        corrFactor
      }
    }

    // startPos = Math.min(0, Math.min(text.getStartPositionOfChar(charOrder[0]).x, text.getEndPositionOfChar(charOrder[0]).x));
    let startPos = 0
    // console.log("startPos[initial]: " + startPos);
    for (let i = 0; i < charAttrs.length && i < firstChar; i++) {
      // In RTL mode on RTL chars, for some reason we should not add the width of the first char.
      // But if we are in RTL mode and hit an LTR char (i.e. displaying normal LTR text in RTL mode)
      // when we need to include it... don't ask me why... REC 2021-11-27
      if (charDirection[i] === 'ltr' || charAttrs[i].order !== firstChar) {
        startPos += charAttrs[i].width
      }
      // console.log("startPos["+i+"]  "+text.textContent[charOrder[i]]+" width "+charWidths[i]+" : " + startPos);
    }

    // endPos = Math.min(0, Math.min(text.getStartPositionOfChar(charOrder[0]).x, text.getEndPositionOfChar(charOrder[0]).x));
    let endPos = 0
    // console.log("endPos[initial]: " + endPos);
    let i = 0
    for (; charAttrs[i].order !== lastChar && i < charAttrs.length; i++) {
      endPos += charAttrs[i].width
      // console.log("endPos["+i+"]  "+text.textContent[charOrder[i]]+" width "+charWidths[i]+" : " + endPos);
    }

    if (charDirection[i] === (this.rtlmode ? 'rtl' : 'ltr')) {
      // console.log("endPos["+i+"]  "+text.textContent[charOrder[i]]+" width "+charWidths[i]+" : " + endPos);
      endPos += charAttrs[i].width
    }

    startPos = startPos * corrFactor
    endPos = endPos * corrFactor
    // console.log("startPos: " + startPos);
    // console.log("endPos: " + endPos);
    return [startPos, endPos]
  }

  private calculateSubstringPositionFast (text: SVGTextContentElement, firstChar: number, lastChar: number) {
    try {
{      let startPos: number
      if (firstChar < text.getNumberOfChars()) {
        startPos = text.getStartPositionOfChar(firstChar).x
      } else {
        startPos = text.getComputedTextLength()
      }
      const endPos = (lastChar < firstChar)
        ? startPos
        : text.getEndPositionOfChar(lastChar).x
      return [startPos, endPos]
}    }
    catch (e) {
      console.error(`Unable to calculate width of range ${firstChar}-${lastChar} on [${text}]`, e)
      return [0, 0]
    }
  }

  /**
   * Get the fragment annotation text sizes.
   */
  private calculateFragmentTextMeasures (docData: DocumentData): Measurements {
    const fragmentTexts = {}

    let noSpans = true
    if (docData.spans) {
      for (const span of Object.values(docData.spans)) {
        if (span.fragments) {
          for (const fragment of span.fragments) {
            fragmentTexts[fragment.glyphedLabelText] = true
            noSpans = false
          }
        }
      }
    }

    if (noSpans) {
      fragmentTexts.$ = true // dummy so we can at least get the height
    }

    return this.getTextMeasurements(fragmentTexts, 'span')
  }

  /**
   * Get the arc annotation text sizes (for all labels).
   *
   * @return {Measurements}
   */
  private calculateArcTextMeasurements (docData: DocumentData): Measurements {
    const arcs = docData.arcs
    const spans = docData.spans
    const eventDescs = docData.eventDescs

    const arcTexts = {}
    if (arcs) {
      arcs.forEach(arc => {
        let labels = Util.getArcLabels(this.entityTypes, spans[arc.origin].type, arc.type, this.relationTypes)
        if (!labels.length) {
          labels = [arc.type]
        }

        if (arc.eventDescId && eventDescs[arc.eventDescId] && eventDescs[arc.eventDescId].labelText) {
          labels = [eventDescs[arc.eventDescId].labelText]
        }

        labels.forEach(label => { arcTexts[label] = true })
      })
    }

    return this.getTextMeasurements(arcTexts, 'arcs')
  }

  /**
   * Adjust all fragments in a tower so they have the same width.
   */
  private adjustTowerAnnotationSizes (docData: DocumentData) {
    const fragmentWidths = docData.sizes.fragments.widths
    Object.values(docData.towers).forEach(tower => {
      let maxWidth = 0
      tower.forEach(fragment => { maxWidth = Math.max(maxWidth, fragmentWidths[fragment.glyphedLabelText]) })
      tower.forEach(fragment => { fragment.width = maxWidth })
    })
  }

  private makeArrow (spec: string): string | undefined {
    const parsedSpec = spec.split(',')
    const type = parsedSpec[0]
    if (type === 'none') {
      return
    }

    let width = 5
    let height = 5
    let color = 'black'
    if (!isNaN(parseFloat(parsedSpec[1])) && parsedSpec[2]) {
      if (!isNaN(parseFloat(parsedSpec[2])) && parsedSpec[3]) {
        // 3 args, 2 numeric: assume width, height, color
        width = parseFloat(parsedSpec[1])
        height = parseFloat(parsedSpec[2])
        color = parsedSpec[3] || 'black'
      } else {
        // 2 args, 1 numeric: assume width/height, color
        width = height = parseFloat(parsedSpec[1])
        color = parsedSpec[2] || 'black'
      }
    } else {
      // other: assume color only
      color = parsedSpec[1] || 'black'
    }

    // hash needs to be replaced as IDs don't permit it.
    const arrowId = 'arrow_' + spec.replace(/#/g, '').replace(/,/g, '_')

    // FIXME Looks like we create a new arrow definition for every arc, overriding previous defs
    // if necessary - maybe we should remember if an arrow type has already been declared and if
    // so not re-declare it.
    if (type === 'triangle') {
      const arrow = this.svg.marker(width, height)
        .id(arrowId)
        .ref(width, height / 2)
        .orient('auto')
        .attr({
          markerUnits: 'strokeWidth',
          fill: color
        })
        .addTo(this.svg.defs())

      this.svg.polygon([[0, 0], [width, height / 2], [0, height], [width / 12, height / 2]])
        .addTo(arrow)
    }

    return arrowId
  }

  calculateMaxTextWidth (sizes: Sizes): number {
    let maxTextWidth = 0
    for (const text in sizes.texts.widths) {
      if (Object.prototype.hasOwnProperty.call(sizes.texts.widths, text)) {
        maxTextWidth = Math.max(maxTextWidth, sizes.texts.widths[text])
      }
    }
    return maxTextWidth
  }

  private renderLayoutFloorsAndCurlies (docData: DocumentData, spanDrawOrderPermutation: string[]) {
    // reserve places for spans
    const floors : number[] = []
    // reservations[chunk][floor] = [[from, to, headroom]...]
    const reservations : Array<Array<Array<[from: number, to: number, headroom: number]>>> = []
    const inf = 1.0 / 0.0

    $.each(spanDrawOrderPermutation, (spanIdNo, spanId) => {
      const span = docData.spans[spanId]
      const spanDesc = this.entityTypes[span.type]
      const bgColor = ((spanDesc && spanDesc.bgColor) || '#ffffff')

      if (bgColor === 'hidden') {
        span.hidden = true
        return
      }

      const f1 = span.fragments[0]
      const f2 = span.fragments[span.fragments.length - 1]

      const x1 = (f1.curly.from + f1.curly.to - f1.width) / 2 -
        Configuration.visual.margin.x - (docData.sizes.fragments.height / 2)
      const i1 = f1.chunk.index

      const x2 = (f2.curly.from + f2.curly.to + f2.width) / 2 +
        Configuration.visual.margin.x + (docData.sizes.fragments.height / 2)
      const i2 = f2.chunk.index

      // Start from the ground level, going up floor by floor.
      // If no more floors, make a new available one.
      // If a floor is available and there is no carpet, mark it as carpet.
      // If a floor is available and there is carpet and height
      //   difference is at least fragment height + curly, OK.
      // If a floor is not available, forget about carpet.
      // --
      // When OK, calculate exact ceiling.
      // If there isn't one, make a new floor, copy reservations
      //   from floor below (with decreased ceiling)
      // Make the reservation from the carpet to just below the
      //   current floor.
      //
      // TODO drawCurly and height could be prettified to only check
      // actual positions of curlies
      let carpet : number | null = 0
      const thisCurlyHeight = span.drawCurly ? Configuration.visual.curlyHeight : 0
      const height = docData.sizes.fragments.height + thisCurlyHeight + Configuration.visual.boxSpacing +
        2 * Configuration.visual.margin.y - 3
      for (const floor of floors) {
        let floorAvailable = true
        for (let i = i1; i <= i2; i++) {
          if (!(reservations[i] && reservations[i][floor])) {
            continue
          }
          const from = (i === i1) ? x1 : -inf
          const to = (i === i2) ? x2 : inf
          
          for (const res of reservations[i][floor]) {
            if (res[0] < to && from < res[1]) {
              floorAvailable = false
              break
            }
          }
        }

        if (floorAvailable) {
          if (carpet === null) {
            carpet = floor
          } else if (height + carpet <= floor) {
            // found our floor!
            break
          }
        } else {
          carpet = null
        }
      }

      const reslen = reservations.length
      const makeNewFloorIfNeeded = function (floor: number) {
        let floorNo = $.inArray(floor, floors)
        if (floorNo === -1) {
          floors.push(floor)
          floors.sort(Util.cmp)
          floorNo = $.inArray(floor, floors)
          if (floorNo !== 0) {
            // copy reservations from the floor below
            const parquet = floors[floorNo - 1]
            for (let i = 0; i <= reslen; i++) {
              if (reservations[i]) {
                if (!reservations[i][parquet]) {
                  reservations[i][parquet] = []
                }

                const footroom = floor - parquet
                for (const res of reservations[i][parquet]) {
                  if (res[2] > footroom) {
                    if (!reservations[i][floor]) {
                      reservations[i][floor] = []
                    }
                    reservations[i][floor].push([res[0], res[1], res[2] - footroom])
                  }
                }
              }
            }
          }
        }
        return floorNo
      }

      const ceiling = carpet + height
      makeNewFloorIfNeeded(ceiling)
      const carpetNo = makeNewFloorIfNeeded(carpet)
      // make the reservation
      let floor : number
      let floorNo : number
      for (floorNo = carpetNo; (floor = floors[floorNo]) !== undefined && floor < ceiling; floorNo++) {
        const headroom = ceiling - floor
        for (let i = i1; i <= i2; i++) {
          const from = (i === i1) ? x1 : 0
          const to = (i === i2) ? x2 : inf
          if (!reservations[i]) { reservations[i] = [] }
          if (!reservations[i][floor]) { reservations[i][floor] = [] }
          reservations[i][floor].push([from, to, headroom]) // XXX maybe add fragment; probably unnecessary
        }
      }

      span.floor = carpet + thisCurlyHeight
    })
  }

  private renderChunks (docData: DocumentData, sourceData: SourceData, chunks: Chunk[], maxTextWidth: number): [Row[], number[], MarkedTextHighlight[]] {
    let currentX: number
    if (this.rtlmode) {
      currentX = this.canvasWidth - (Configuration.visual.margin.x + this.sentNumMargin + this.rowPadding)
    } else {
      currentX = Configuration.visual.margin.x + this.sentNumMargin + this.rowPadding
    }

    const rows: Row[] = []
    const fragmentHeights: number[] = []
    const openTextHighlights = {}

    let sentenceToggle = 0
    let sentenceNumber = sourceData.sentence_number_offset

    let row = new Row(this.svg)
    row.sentence = sentenceNumber
    row.backgroundIndex = sentenceToggle
    row.index = 0

    const markedTextHighlights: Array<MarkedTextHighlight> = []
    let rowIndex = 0

    chunks.forEach((chunk: Chunk) => {
      let spaceWidth = 0
      if (chunk.lastSpace) {
        const spaceLen = chunk.lastSpace.length || 0
        let spacePos: number
        if (chunk.sentence) {
          // If this is line-initial spacing, fetch the sentence to which the chunk belongs
          // so we can determine where it begins
          const sentFrom = sourceData.sentence_offsets[chunk.sentence - sourceData.sentence_number_offset][0]
          spacePos = spaceLen - (chunk.from - sentFrom)
        } else {
          spacePos = 0
        }
        for (let i = spacePos; i < spaceLen; i++) {
          spaceWidth += this.spaceWidths[chunk.lastSpace[i]] * (this.fontZoom / 100.0) || 0
        }
        currentX += this.rtlmode ? -spaceWidth : spaceWidth
      }

      chunk.group = this.svg.group().addTo(row.group)
      chunk.highlightGroup = this.svg.group().addClass('chunk-highlights').addTo(chunk.group)

      let y = 0
      let hasLeftArcs = false
      let hasRightArcs = false
      let hasInternalArcs = false
      let hasAnnotations = false
      let chunkFrom = Infinity
      let chunkTo = 0
      let chunkHeight = 0
      let spacing = 0
      let spacingChunkId: number | undefined = undefined
      let spacingRowBreak = 0

      // Render the fragments for the current chunk
      chunk.fragments.forEach(fragment => {
        const span = fragment.span

        if (span.hidden || span.id === "rel:0-before" || span.id === "rel:1-after") {
          return
        }

        const spanDesc = this.entityTypes[span.type]
        let bgColor = ((spanDesc && spanDesc.bgColor) || '#ffffff')
        let fgColor = ((spanDesc && spanDesc.fgColor) || '#000000')
        let borderColor = ((spanDesc && spanDesc.borderColor) || '#000000')

        if (span.color) {
          bgColor = span.color
          fgColor = bgToFgColor(bgColor)
          borderColor = 'darken'
        }

        // special case: if the border 'color' value is 'darken',
        // then just darken the BG color a bit for the border.
        if (borderColor === 'darken') {
          borderColor = Util.adjustColorLightness(bgColor, -0.6)
        }

        fragment.group = this.svg.group().addTo(chunk.group).addClass('span')

        if (!y) {
          y = -docData.sizes.texts.height
        }
        // x : center of fragment on x axis
        let x = (fragment.curly.from + fragment.curly.to) / 2

        // XXX is it maybe sizes.texts?
        let yy = y + docData.sizes.fragments.y
        // hh : fragment height
        let hh = docData.sizes.fragments.height
        // ww : fragment width
        let ww = fragment.width
        // xx : left edge of fragment
        let xx = x - ww / 2

        // text margin fine-tuning
        yy += this.boxTextMargin.y
        hh -= 2 * this.boxTextMargin.y
        xx += this.boxTextMargin.x
        ww -= 2 * this.boxTextMargin.x
        // cue/type part required for adding reselect class when drawing arcs
        let rectClass = 'span_' + (span.cue || span.type) + ' span_default'

        // attach e.g. "False_positive" into the type
        if (span.comment && span.comment.type) {
          rectClass += ' ' + span.comment.type
        }

        // inner coordinates of fragment (excluding margins)
        let bx = xx - Configuration.visual.margin.x - this.boxTextMargin.x
        const by = yy - Configuration.visual.margin.y
        const bw = ww + 2 * Configuration.visual.margin.x
        const bh = hh + 2 * Configuration.visual.margin.y

        if (this.roundCoordinates) {
          x = (x | 0) + 0.5
          bx = (bx | 0) + 0.5
        }

        if (span.marked) {
          this.renderSpanMarkedRect(yy, bx, by, bw, bh, fragment)
        }

        // Nicely spread out labels/text and leave space for mark highlight such that adding
        // the mark doesn't change the overall layout
        chunkFrom = Math.min(bx - this.markedSpanSize, chunkFrom)
        chunkTo = Math.max(bx + bw + this.markedSpanSize, chunkTo)
        let fragmentHeight = bh + 2 * this.markedSpanSize
        if (span.shadowClass && span.shadowClass.match(this.shadowClassPattern)) {
          this.renderFragmentShadowRect(yy, bx, by, bw, bh, fragment)
          chunkFrom = Math.min(bx - this.rectShadowSize, chunkFrom)
          chunkTo = Math.max(bx + bw + this.rectShadowSize, chunkTo)
          fragmentHeight = Math.max(bh + 2 * this.rectShadowSize, fragmentHeight)
        }

        fragment.rect = this.renderFragmentRect(bx, by, bw, bh, yy, fragment, rectClass, bgColor, borderColor)
        fragment.left = bx // TODO put it somewhere nicer?
        fragment.right = bx + bw // TODO put it somewhere nicer?
        if (!(span.shadowClass || span.marked)) {
          chunkFrom = Math.min(bx, chunkFrom)
          chunkTo = Math.max(bx + bw, chunkTo)
          fragmentHeight = Math.max(bh, fragmentHeight)
        }

        fragment.rectBox = new RectBox(bx, by - span.floor, bw, bh)
        fragment.height = span.floor + hh +
          3 * Configuration.visual.margin.y +
          Configuration.visual.curlyHeight +
          (Configuration.visual.arcSpacing * (this.fontZoom / 100.0))
        const spacedTowerId = fragment.towerId * 2
        if (!fragmentHeights[spacedTowerId] || fragmentHeights[spacedTowerId] < fragment.height) {
          fragmentHeights[spacedTowerId] = fragment.height
        }

        if (span.attributeMerge.box === 'crossed') {
          this.renderFragmentCrossOut(xx, yy, hh, fragment)
        }

        fragment.group.add(docData.spanAnnTexts[fragment.glyphedLabelText].clone()
          .amove(x, y - span.floor)
          .fill(fgColor))

        // Make curlies to show the fragment
        if (fragment.drawCurly) {
          this.renderCurly(fragment, x, yy, hh)
          chunkFrom = Math.min(fragment.curly.from, chunkFrom)
          chunkTo = Math.max(fragment.curly.to, chunkTo)
          fragmentHeight = Math.max(Configuration.visual.curlyHeight, fragmentHeight)
        }

        if (fragment === span.headFragment) {
          const checkLeftRightArcs = (arc: Arc, refSpan: Entity, leftSpan: Entity) => {
            const refChunk = leftSpan.headFragment.chunk
            if (!refChunk || !refChunk.row) {
              hasRightArcs = true
              return
            }

            let border: number
            if (refChunk.row.index === rowIndex) {
              border = refChunk.translation.x + leftSpan.fragments[leftSpan.fragments.length - 1].right
            } else {
              if (this.rtlmode) {
                border = 0
              } else {
                border = Configuration.visual.margin.x + this.sentNumMargin + this.rowPadding
              }
            }

            let labels = Util.getArcLabels(this.entityTypes, refSpan.type, arc.type, this.relationTypes)
            if (!labels.length) { labels = [arc.type] }

            if (arc.eventDescId && docData.eventDescs[arc.eventDescId]) {
              if (docData.eventDescs[arc.eventDescId].labelText) {
                labels = [docData.eventDescs[arc.eventDescId].labelText]
              }
            }

            const labelNo = Configuration.abbrevsOn ? labels.length - 1 : 0
            const smallestLabelWidth = docData.sizes.arcs.widths[labels[labelNo]] + 2 * this.minArcSlant

            const gap = Math.abs(currentX + (this.rtlmode ? -bx : bx) - border)

            let arcSpacing = smallestLabelWidth - gap
            if (!hasLeftArcs || spacing < arcSpacing) {
              spacing = arcSpacing
              spacingChunkId = refChunk.index + 1
            }
            arcSpacing = smallestLabelWidth - bx
            if (!hasLeftArcs || spacingRowBreak < arcSpacing) {
              spacingRowBreak = arcSpacing
            }
            hasLeftArcs = true
          }

          // find the gap to fit the backwards arcs, but only on
          // head fragment - other fragments don't have arcs
          for (const arc of span.incoming) {
            const leftSpan = docData.spans[arc.origin]
            checkLeftRightArcs(arc, leftSpan, leftSpan)
          }

          for (const arc of span.outgoing) {
            const leftSpan = docData.spans[arc.target]
            checkLeftRightArcs(arc, span, leftSpan)
          }

          for (const arc of span.incoming) {
            const origin = docData.spans[arc.origin].headFragment.chunk
            if (origin && chunk.index === origin.index) {
              hasInternalArcs = true
            }
          }
        }

        fragmentHeight += span.floor || Configuration.visual.curlyHeight
        if (fragmentHeight > chunkHeight) {
          chunkHeight = fragmentHeight
        }

        hasAnnotations = true
      }) // fragments

      // positioning of the chunk
      chunk.right = chunkTo
      const textWidth = docData.sizes.texts.widths[chunk.text]
      chunkHeight += docData.sizes.texts.height
      // If chunkFrom becomes negative (LTR) or chunkTo becomes positive (RTL), then boxX becomes positive
      const boxX = this.rtlmode ? chunkTo : -Math.min(chunkFrom, 0)

      let boxWidth : number
      if (this.rtlmode) {
        boxWidth = Math.max(textWidth, -chunkFrom) - Math.min(0, -chunkTo)
      } else {
        boxWidth = Math.max(textWidth, chunkTo) - Math.min(0, chunkFrom)
      }

      if (spacing > 0) {
        currentX += this.rtlmode ? -spacing : spacing
      }

      const rightBorderForArcs = hasRightArcs ? this.arcHorizontalSpacing : (hasInternalArcs ? this.arcSlant : 0)
      const leftBorderForArcs = hasLeftArcs ? this.arcHorizontalSpacing : (hasInternalArcs ? this.arcSlant : 0)
      const lastX = currentX
      const lastRow = row

      // Is there a sentence break at the current chunk (i.e. it is the first chunk in a new
      // sentence) - if yes and the current sentence is not the same as the sentence to which
      // the chunk belongs, then fill in additional rows
      if (chunk.sentence) {
        while (sentenceNumber < chunk.sentence - 1) {
          sentenceNumber++
          row.arcs = this.svg.group().addTo(row.group).addClass('arcs')
          rows.push(row)

          row = new Row(this.svg)
          row.sentence = sentenceNumber
          sentenceToggle = 1 - sentenceToggle
          row.backgroundIndex = sentenceToggle
          row.index = ++rowIndex
        }
        // Not changing row background color here anymore - we do this later now when the next
        // row is added
      }

      let chunkDoesNotFit : boolean
      if (this.rtlmode) {
        chunkDoesNotFit = currentX - boxWidth - leftBorderForArcs <=
          2 * Configuration.visual.margin.x
      } else {
        chunkDoesNotFit = currentX + boxWidth + rightBorderForArcs >=
          this.canvasWidth - 2 * Configuration.visual.margin.x
      }

      // Check if a new row needs to be started and if so start it
      if (chunk.sentence > sourceData.sentence_number_offset || chunkDoesNotFit) {
        // the chunk does not fit
        row.arcs = this.svg.group().addTo(row.group).addClass('arcs')
        let indent = 0
        if (chunk.lastSpace) {
          const spaceLen = chunk.lastSpace.length || 0
          let spacePos : number
          if (chunk.sentence) {
            // If this is line-initial spacing, fetch the sentence to which the chunk belongs
            // so we can determine where it begins
            const sentFrom = sourceData.sentence_offsets[chunk.sentence - sourceData.sentence_number_offset][0]
            spacePos = spaceLen - (chunk.from - sentFrom)
          } else {
            spacePos = 0
          }
          for (let i = spacePos; i < spaceLen; i++) {
            indent += this.spaceWidths[chunk.lastSpace[i]] * (this.fontZoom / 100.0) || 0
          }
        }

        if (this.rtlmode) {
          currentX = this.canvasWidth - (Configuration.visual.margin.x + this.sentNumMargin + this.rowPadding +
            (hasRightArcs ? this.arcHorizontalSpacing : (hasInternalArcs ? this.arcSlant : 0)) - /* +
                          spaceWidth */
            indent)
        } else {
          currentX = Configuration.visual.margin.x + this.sentNumMargin + this.rowPadding +
            (hasLeftArcs ? this.arcHorizontalSpacing : (hasInternalArcs ? this.arcSlant : 0)) + /* +
                        spaceWidth */
            indent
        }

        if (hasLeftArcs) {
          const adjustedCurTextWidth = docData.sizes.texts.widths[chunk.text] + this.arcHorizontalSpacing
          if (adjustedCurTextWidth > maxTextWidth) {
            maxTextWidth = adjustedCurTextWidth
          }
        }

        if (spacingRowBreak > 0) {
          currentX += this.rtlmode ? -spacingRowBreak : spacingRowBreak
          spacing = 0 // do not center intervening elements
        }

        // Finish up current row
        rows.push(row)
        chunk.group.remove()

        // Start new row
        row = new Row(this.svg)
        if (chunk.sentence) {
          // Change row background color if a new sentence is starting
          sentenceToggle = 1 - sentenceToggle
        }
        row.backgroundIndex = sentenceToggle
        row.index = ++rowIndex
        chunk.group.addTo(row.group)
        chunk.group = SVG(row.group.node.lastElementChild as SVGGElement)
        chunk.group.node.querySelectorAll("g.span").forEach((element, index) => {
          chunk.fragments[index].group = SVG(element as SVGGElement)
        })
        chunk.group.node.querySelectorAll('rect[data-span-id]').forEach((element, index) => {
          chunk.fragments[index].rect = SVG(element as SVGElement)
        })
      }

      // break the text highlights when the row breaks
      if (row.index !== lastRow.index) {
        $.each(openTextHighlights, (textId, textDesc) => {
          if (textDesc[3] !== lastX) {
            let newDesc: MarkedTextHighlight
            if (this.rtlmode) {
              newDesc = [lastRow, textDesc[3], lastX - boxX, textDesc[4]]
            } else {
              newDesc = [lastRow, textDesc[3], lastX + boxX, textDesc[4]]
            }
            markedTextHighlights.push(newDesc)
          }
          textDesc[3] = currentX
        })
      }

      // open text highlights
      for (const textDesc of chunk.markedTextStart) {
        textDesc[3] += currentX + (this.rtlmode ? -boxX : boxX);
        openTextHighlights[textDesc[0]] = textDesc;
      }

      // close text highlights
      for (const textDesc of chunk.markedTextEnd) {
        textDesc[3] += currentX + (this.rtlmode ? -boxX : boxX);
        const startDesc = openTextHighlights[textDesc[0]];
        delete openTextHighlights[textDesc[0]];
        markedTextHighlights.push([row, startDesc[3], textDesc[3], startDesc[4]]);
      }

      // XXX check this - is it used? should it be lastRow?
      if (hasAnnotations) {
        row.hasAnnotations = true
      }

      if (chunk.sentence > sourceData.sentence_number_offset) {
        row.sentence = ++sentenceNumber
      }

      // if we added a gap, center the intervening elements
      if (spacing > 0 && spacingChunkId !== undefined) {
        spacing /= 2
        const firstChunkInRow = row.chunks[row.chunks.length - 1]
        if (firstChunkInRow === undefined) {
          console.log('warning: firstChunkInRow undefined, chunk:', chunk)
        } else { // valid firstChunkInRow
          if (spacingChunkId < firstChunkInRow.index) {
            spacingChunkId = firstChunkInRow.index + 1
          }
          for (let chunkIndex = spacingChunkId; chunkIndex < chunk.index; chunkIndex++) {
            const movedChunk = docData.chunks[chunkIndex]
            this.fastTranslate(movedChunk, movedChunk.translation.x + spacing, 0)
            movedChunk.textX += spacing
          }
        }
      }

      // Assign chunk to row
      row.chunks.push(chunk)
      chunk.row = row

      this.fastTranslate(chunk, currentX + (this.rtlmode ? -boxX : boxX), 0)
      chunk.textX = currentX + (this.rtlmode ? -boxX : boxX)
      currentX += this.rtlmode ? -boxWidth : boxWidth
    }) // chunks

    // Add trailing empty rows
    while (sentenceNumber < (sourceData.sentence_offsets.length + sourceData.sentence_number_offset - 1)) {
      sentenceNumber++
      row.arcs = this.svg.group().addTo(row.group).addClass('arcs')
      rows.push(row)
      row = new Row(this.svg)
      row.sentence = sentenceNumber
      sentenceToggle = 1 - sentenceToggle
      row.backgroundIndex = sentenceToggle
      row.index = ++rowIndex
    }

    // finish the last row
    row.arcs = this.svg.group().addTo(row.group).addClass('arcs')
    rows.push(row)

    return [rows, fragmentHeights, markedTextHighlights]
  }

  private renderChunksPass2 (docData: DocumentData, textGroup: SVGTypeMapping<SVGGElement>, markedTextHighlights: MarkedTextHighlight[]) {
    // chunk index sort functions for overlapping fragment drawing
    // algorithm; first for left-to-right pass, sorting primarily
    // by start offset, second for right-to-left pass by end
    // offset. Secondary sort by fragment length in both cases.
    let currentChunk: Chunk

    const lrChunkComp = (a: number, b: number) => {
      const ac = currentChunk.fragments[a]
      const bc = currentChunk.fragments[b]
      const startDiff = Util.cmp(ac.from, bc.from)
      return startDiff !== 0 ? startDiff : Util.cmp(bc.to - bc.from, ac.to - ac.from)
    }

    const rlChunkComp = (a: number, b: number) => {
      const ac = currentChunk.fragments[a]
      const bc = currentChunk.fragments[b]
      const endDiff = Util.cmp(bc.to, ac.to)
      return endDiff !== 0 ? endDiff : Util.cmp(bc.to - bc.from, ac.to - ac.from)
    }

    let prevChunk: Chunk
    let rowTextGroup: SVGTypeMapping<SVGGElement> | undefined
    $.each(docData.chunks, (chunkNo, chunk) => {
      // context for sort
      currentChunk = chunk

      // Add spacers to reduce jumpyness of selection
      if (!rowTextGroup || prevChunk.row !== chunk.row) {
        if (rowTextGroup) {
          this.horizontalSpacer(rowTextGroup, 0, prevChunk.row.textY, 1, {
            'data-chunk-id': prevChunk.index,
            class: 'row-final spacing'
          })
        }
        rowTextGroup = this.svg.group().addTo(textGroup).addClass('text-row')
      }
      prevChunk = chunk

      const nextChunk = docData.chunks[chunkNo + 1]

      if (this.rtlmode) {
        // Render every text chunk as a SVG text so we maintain control over the layout. When
        // rendering as a SVG span (as brat does), then the browser changes the layout on the
        // X-axis as it likes in RTL mode.
        if (!rowTextGroup.node.firstChild) {
          this.horizontalSpacer(rowTextGroup, 0, chunk.row.textY, 1, {
            class: 'row-initial spacing',
            'data-chunk-id': chunk.index
          })
        }

        chunk.renderText(this.svg, rowTextGroup)

        // If there needs to be space between this chunk and the next one, add a spacer
        // item that stretches across the entire inter-chunk space. This ensures a
        // smooth selection.
        if (nextChunk) {
          const spaceX = chunk.textX - docData.sizes.texts.widths[chunk.text]
          const spaceWidth = chunk.textX - docData.sizes.texts.widths[chunk.text] - nextChunk.textX
          this.horizontalSpacer(rowTextGroup, spaceX, chunk.row.textY, spaceWidth, {
            'data-chunk-id': chunk.index
          })
        }
      } else {
        // Original rendering using tspan in ltr mode as it play nicer with selection
        if (!rowTextGroup.node.firstChild) {
          this.horizontalSpacer(rowTextGroup, 0, chunk.row.textY, 1, {
            class: 'row-initial spacing',
            'data-chunk-id': chunk.index
          })
        }

        chunk.renderText(this.svg, rowTextGroup)

        // If there needs to be space between this chunk and the next one, add a spacer
        // item that stretches across the entire inter-chunk space. This ensures a
        // smooth selection.
        if (nextChunk) {
          const spaceX = chunk.textX + docData.sizes.texts.widths[chunk.text]
          const spaceWidth = nextChunk.textX - spaceX
          this.horizontalSpacer(rowTextGroup, spaceX, chunk.row.textY, spaceWidth, {
            'data-chunk-id': chunk.index
          })
        }
      }

      // chunk backgrounds
      if (chunk.fragments.length) {
        const orderedIdx : number[] = []
        for (let i = chunk.fragments.length - 1; i >= 0; i--) {
          orderedIdx.push(i)
        }

        // Mark entity nesting height/depth (number of
        // nested/nesting entities). To account for crossing
        // brackets in a (mostly) reasonable way, determine
        // depth/height separately in a left-to-right traversal
        // and a right-to-left traversal.
        orderedIdx.sort(lrChunkComp)

        // eslint-disable-next-line no-lone-blocks
        {
          let openFragments : Fragment[] = []
          for (let i = 0; i < orderedIdx.length; i++) {
            const current = chunk.fragments[orderedIdx[i]]
            current.nestingHeightLR = 0
            current.nestingDepthLR = 0
            const stillOpen : Fragment[] = []
            for (let o = 0; o < openFragments.length; o++) {
              if (openFragments[o].to > current.from) {
                stillOpen.push(openFragments[o])
                openFragments[o].nestingHeightLR++
              }
            }
            openFragments = stillOpen
            current.nestingDepthLR = openFragments.length
            openFragments.push(current)
          }
        }

        // re-sort for right-to-left traversal by end position
        orderedIdx.sort(rlChunkComp)

        // eslint-disable-next-line no-lone-blocks
        {
          let openFragments : Fragment[] = []
          for (let i = 0; i < orderedIdx.length; i++) {
            const current = chunk.fragments[orderedIdx[i]]
            current.nestingHeightRL = 0
            current.nestingDepthRL = 0
            const stillOpen : Fragment[] = []
            for (let o = 0; o < openFragments.length; o++) {
              if (openFragments[o].from < current.to) {
                stillOpen.push(openFragments[o])
                openFragments[o].nestingHeightRL++
              }
            }
            openFragments = stillOpen
            current.nestingDepthRL = openFragments.length
            openFragments.push(current)
          }
        }

        // the effective depth and height are the max of those
        // for the left-to-right and right-to-left traversals.
        for (let i = 0; i < orderedIdx.length; i++) {
          const c = chunk.fragments[orderedIdx[i]]
          c.nestingHeight = c.nestingHeightLR > c.nestingHeightRL ? c.nestingHeightLR : c.nestingHeightRL
          c.nestingDepth = c.nestingDepthLR > c.nestingDepthRL ? c.nestingDepthLR : c.nestingDepthRL
        }

        // Re-order by nesting height and draw in order
        orderedIdx.sort(function (a, b) {
          return Util.cmp(chunk.fragments[b].nestingHeight, chunk.fragments[a].nestingHeight)
        })

        for (let i = 0; i < chunk.fragments.length; i++) {
          const fragment = chunk.fragments[orderedIdx[i]]
          if (fragment.span.hidden || fragment.span.id === "rel:0-before" || fragment.span.id === "rel:1-after") {
            continue
          }

          const spanDesc = this.entityTypes[fragment.span.type]
          let bgColor = ((spanDesc && spanDesc.bgColor) || '#ffffff')

          if (fragment.span.color) {
            bgColor = fragment.span.color
          }

          // Tweak for nesting depth/height. Recognize just three levels for now: normal, nested,
          // and nesting, where nested+nesting yields normal. (Currently testing minor tweak: don't
          // shrink for depth 1 as the nesting highlight will grow anyway [check nestingDepth > 1])
          let shrink = 0
          if (fragment.nestingDepth > 1 && fragment.nestingHeight === 0) {
            shrink = 1
          } else if (fragment.nestingDepth === 0 && fragment.nestingHeight > 0) {
            shrink = -1
          }

          const yShrink = shrink * this.nestingAdjustYStepSize
          const xShrink = shrink * this.nestingAdjustXStepSize
          // bit lighter
          const lightBgColor = Util.adjustColorLightness(bgColor, 0.8)
          // tweak for Y start offset (and corresponding height reduction): text rarely hits font
          // max height, so this tends to look better
          const yStartTweak = 1

          // Store highlight coordinates
          fragment.highlightPos = {
            x: chunk.textX + (this.rtlmode ? (fragment.curly.from - xShrink) : (fragment.curly.from + xShrink)),
            y: chunk.row.textY + docData.sizes.texts.y + yShrink + yStartTweak,
            w: fragment.curly.to - fragment.curly.from - 2 * xShrink,
            h: docData.sizes.texts.height - 2 * yShrink - yStartTweak
          }

          // Avoid exception because width < 0 is not allowed
          if (fragment.highlightPos.w <= 0) {
            fragment.highlightPos.w = 1
          }

          // Render highlight
          this.svg.rect(fragment.highlightPos.w, fragment.highlightPos.h)
            .addClass('chunk-highlight')
            .move(fragment.highlightPos.x, fragment.highlightPos.y)
            .attr({
              fill: lightBgColor,
              rx: this.highlightRounding.x,
              ry: this.highlightRounding.y
            })
            .addTo(this.highlightGroup)
        }
      }
    })

    // Prevent text selection from being jumpy
    if (rowTextGroup && currentChunk) {
      this.horizontalSpacer(rowTextGroup, 0, currentChunk.row.textY, 1, {
        'data-chunk-id': currentChunk.index,
        class: 'row-final spacing'
      })
    }

    // draw the markedText
    for (const textRowDesc of markedTextHighlights) {
      this.svg.rect(textRowDesc[2] - textRowDesc[1] + 4, docData.sizes.fragments.height + 4)
        .translate(textRowDesc[1] - 2, textRowDesc[0].textY - docData.sizes.fragments.height)
        .addClass(textRowDesc[3])
        .addTo(this.highlightGroup)
    }
  }

  private renderBumpFragmentHeightsMinimumToArcStartHeight (fragmentHeights: number[]) {
    const actStartHeight = Configuration.visual.arcStartHeight * (this.fontZoom / 100.0)
    for (let i = 0; i < fragmentHeights.length; i++) {
      if (!fragmentHeights[i] || fragmentHeights[i] < actStartHeight) {
        fragmentHeights[i] = actStartHeight
      }
    }
  }

  private renderCalculateArcJumpHeight (docData: DocumentData, fragmentHeights: number[]) {
    // find out how high the arcs have to go
    for (const arc of docData.arcs) {
      arc.jumpHeight = 0
      let fromFragment = docData.spans[arc.origin].headFragment
      let toFragment = docData.spans[arc.target].headFragment
      if (fromFragment.span.hidden || toFragment.span.hidden) {
        arc.hidden = true
        return
      }

      if (fromFragment.towerId > toFragment.towerId) {
        const tmp = fromFragment
        fromFragment = toFragment
        toFragment = tmp
      }

      let from: number, to: number
      if (fromFragment.chunk.index === toFragment.chunk.index) {
        from = fromFragment.towerId
        to = toFragment.towerId
      } else {
        from = fromFragment.towerId + 1
        to = toFragment.towerId - 1
      }

      for (let i = from; i <= to; i++) {
        const targetJumpHeight = fragmentHeights[i * 2]
        if (arc.jumpHeight < targetJumpHeight) {
          arc.jumpHeight = targetJumpHeight
        }
      }
    }
  }

  private renderSortArcs (docData: DocumentData) {
    // sort the arcs
    docData.arcs.sort((a, b) => {
      // first write those that have less to jump over
      let tmp = a.jumpHeight - b.jumpHeight
      if (tmp) { return tmp < 0 ? -1 : 1 }
      // if equal, then those that span less distance
      tmp = a.dist - b.dist
      if (tmp) { return tmp < 0 ? -1 : 1 }
      // if equal, then those where heights of the targets are smaller
      tmp = docData.spans[a.origin].headFragment.height + docData.spans[a.target].headFragment.height -
      docData.spans[b.origin].headFragment.height - docData.spans[b.target].headFragment.height
      if (tmp) { return tmp < 0 ? -1 : 1 }
      // if equal, then those with the lower origin
      tmp = docData.spans[a.origin].headFragment.height - docData.spans[b.origin].headFragment.height
      if (tmp) { return tmp < 0 ? -1 : 1 }
      // if equal, they're just equal.
      return 0
    })
  }

  private renderAssignFragmentsToRows (rows: Row[], fragmentHeights: number[]) {
    const arcStartHeight = Configuration.visual.arcStartHeight * (this.fontZoom / 100.0)

    // see which fragments are in each row
    let heightsStart = 0
    let heightsRowsAdded = 0
    $.each(rows, (rowId, row) => {
      let seenFragment = false
      row.heightsStart = row.heightsEnd = heightsStart
      $.each(row.chunks, (chunkId, chunk) => {
        if (chunk.lastFragmentIndex !== undefined) {
          // fragmentful chunk
          seenFragment = true
          const heightsEndIndex = chunk.lastFragmentIndex * 2 + heightsRowsAdded
          if (row.heightsEnd < heightsEndIndex) {
            row.heightsEnd = heightsEndIndex
          }
          const heightsStartIndex = chunk.firstFragmentIndex * 2 + heightsRowsAdded
          if (row.heightsStart > heightsStartIndex) {
            row.heightsStart = heightsStartIndex
          }
        }
      })
      fragmentHeights.splice(row.heightsStart, 0, arcStartHeight)
      heightsRowsAdded++

      row.heightsAdjust = heightsRowsAdded
      if (seenFragment) {
        row.heightsEnd += 2
      }
      heightsStart = row.heightsEnd + 1
    })
  }

  private renderDragArcMarker (defs: Defs) {
    // draw the drag arc marker
    const arrowhead = this.svg.marker(5, 5)
      .id('drag_arrow')
      .ref(5, 2.5)
      .orient('auto')
      .addClass('drag_fill')
      .attr('markerUnits', 'strokeWidth')
      .addTo(defs)

    this.svg.polyline([[0, 0], [5, 2.5], [0, 5], [0.2, 2.5]]).addTo(arrowhead)
  }

  private renderArcs (docData: DocumentData, rows: Row[], fragmentHeights: number[]) {
    const arrows : Record<string, string> = {}
    const arrow = this.makeArrow('none')
    if (arrow) {
      arrows.none = arrow
    }

    const arcCache = {}
    for (const arc of docData.arcs) {
      this.renderArc(docData, arc, arrows, rows, fragmentHeights, arcCache)
    }
  }

  private renderArc (docData: DocumentData, arc: Arc, arrows : Record<string, string>, rows: Row[], fragmentHeights: number[], arcCache) {
    if (arc.hidden) return

    const originSpan = docData.spans[arc.origin]
    const targetSpan = docData.spans[arc.target]

    const leftToRight = originSpan.headFragment.towerId < targetSpan.headFragment.towerId
    const left = leftToRight ? originSpan.headFragment : targetSpan.headFragment
    const right = leftToRight ? targetSpan.headFragment : originSpan.headFragment

    // fall back on relation types in case we still don't have
    // an arc description, with final fallback to unnumbered relation
    let arcDesc = this.relationTypes[arc.type]

    // if it's not a relationship, see if we can find it in span descriptions
    // TODO: might make more sense to reformat this as dict instead of searching through the list every type
    const spanDesc = this.entityTypes[originSpan.type]
    if (!arcDesc && spanDesc && spanDesc.arcs) {
      for (const arcDescIter of spanDesc.arcs) {
        if (arcDescIter.type === arc.type) {
          arcDesc = arcDescIter
        }
      }
    }

    let color = ((arcDesc && arcDesc.color) || '#000000')
    if (color === 'hidden') {
      return
    }

    if (arc.eventDescId && docData.eventDescs[arc.eventDescId]) {
      if (docData.eventDescs[arc.eventDescId].color) {
        color = docData.eventDescs[arc.eventDescId].color
      }
    }

    const symmetric = arcDesc && arcDesc.properties && arcDesc.properties.symmetric
    const dashArray = arcDesc && arcDesc.dashArray
    const arrowHead = ((arcDesc && arcDesc.arrowHead) || 'triangle,5') + ',' + color
    const labelArrowHead = ((arcDesc && arcDesc.labelArrow) || 'triangle,5') + ',' + color

    const leftBox = left.rowBBox()
    const rightBox = right.rowBBox()
    const leftRow = left.chunk.row.index // row with the left end of the arc?
    const rightRow = right.chunk.row.index // row with the right end of the arc?

    if (!arrows[arrowHead]) {
      const arrow = this.makeArrow(arrowHead)
      if (arrow) {
        arrows[arrowHead] = arrow
      }
    }

    if (!arrows[labelArrowHead]) {
      const arrow = this.makeArrow(labelArrowHead)
      if (arrow) {
        arrows[labelArrowHead] = arrow
      }
    }

    // find the next height
    let height = 0
    let fromIndex2: number, toIndex2: number
    if (left.chunk.index === right.chunk.index) {
      fromIndex2 = left.towerId * 2 + left.chunk.row.heightsAdjust
      toIndex2 = right.towerId * 2 + right.chunk.row.heightsAdjust
    } else {
      fromIndex2 = left.towerId * 2 + 1 + left.chunk.row.heightsAdjust
      toIndex2 = right.towerId * 2 - 1 + right.chunk.row.heightsAdjust
    }

    if (!this.collapseArcSpace) {
      height = this.findArcHeight(fromIndex2, toIndex2, fragmentHeights)
      this.adjustFragmentHeights(fromIndex2, toIndex2, fragmentHeights, height)

      // Adjust the height to align with pixels when rendered
      // TODO: on at least Chrome, it doesn't make a difference: lines come out pixel-width even without it. Check.
      height += 0.5
    }

    let chunkReverse = false
    const ufoCatcher = originSpan.headFragment.chunk.index === targetSpan.headFragment.chunk.index
    if (ufoCatcher) {
      if (this.rtlmode) {
        chunkReverse = leftBox.x + leftBox.width / 2 > rightBox.x + rightBox.width / 2
      } else {
        chunkReverse = leftBox.x + leftBox.width / 2 < rightBox.x + rightBox.width / 2
      }
    }
    const ufoCatcherMod = ufoCatcher ? chunkReverse ? -0.5 : 0.5 : 1

    for (let rowIndex = leftRow; rowIndex <= rightRow; rowIndex++) {
      const row = rows[rowIndex]
      if (!row.chunks.length) {
        continue
      }

      row.hasAnnotations = true

      const fromIndex2R = rowIndex === leftRow ? fromIndex2 : row.heightsStart
      const toIndex2R = rowIndex === rightRow ? toIndex2 : row.heightsEnd
      if (this.collapseArcSpace) {
        height = this.findArcHeight(fromIndex2R, toIndex2R, fragmentHeights)
      }

      const arcGroup = this.svg.group()
        .attr('data-from', arc.origin)
        .attr('data-to', arc.target)
        .attr('data-id', arc.eventDescId)
        .addTo(row.arcs)

      // Calculate x position of left side of the arc in current row  
      let from: number
      if (rowIndex === leftRow && left.span.id !== "rel:0-before") {
        if (this.rtlmode) {
          from = leftBox.x + (chunkReverse ? leftBox.width : 0)
        } else {
          from = leftBox.x + (chunkReverse ? 0 : leftBox.width)
        }
      } else {
        from = this.rtlmode ? this.canvasWidth - 2 * Configuration.visual.margin.y - this.sentNumMargin : this.sentNumMargin
      }

      // Calculate x position of right side of the arc in current row  
      let to: number
      if (rowIndex === rightRow && right.span.id !== "rel:1-after") {
        if (this.rtlmode) {
          to = rightBox.x + (chunkReverse ? 0 : rightBox.width)
        } else {
          to = rightBox.x + (chunkReverse ? rightBox.width : 0)
        }
      } else {
        to = this.rtlmode ? 0 : this.canvasWidth - 2 * Configuration.visual.margin.y
      }

      let adjustHeight = true
      if (this.collapseArcs) {
        let arcCacheKey = arc.type + ' ' + rowIndex + ' ' + from + ' ' + to
        if (rowIndex === leftRow) { arcCacheKey = left.span.id + ' ' + arcCacheKey }
        if (rowIndex === rightRow) { arcCacheKey += ' ' + right.span.id }
        const rowHeight = arcCache[arcCacheKey]
        if (rowHeight !== undefined) {
          height = rowHeight
          adjustHeight = false
        } else {
          arcCache[arcCacheKey] = height
        }
      }

      if (this.collapseArcSpace && adjustHeight) {
        this.adjustFragmentHeights(fromIndex2R, toIndex2R, fragmentHeights, height)

        // Adjust the height to align with pixels when rendered
        // TODO: on at least Chrome, this doesn't make a difference:
        // the lines come out pixel-width even without it. Check.
        height += 0.5
      }

      let shadowGroup: SVGTypeMapping<SVGGElement> | undefined
      if (arc.shadowClass || arc.marked) {
        shadowGroup = this.svg.group().addTo(arcGroup)
      }

      let { textStart, textEnd } = this.renderArcLabel(docData, arc, to, from, height, arcGroup, shadowGroup)

      if (this.roundCoordinates) {
        // don't ask
        height = (height | 0) + 0.5
      }
      if (height > row.maxArcHeight) { row.maxArcHeight = height }

      let myArrowHead = (arcDesc && arcDesc.arrowHead)
      let arrowName = (symmetric
        ? myArrowHead || 'none'
        : (leftToRight ? 'none' : myArrowHead || 'triangle,5')
      ) + ',' + color
      let arrowType = arrows[arrowName]
      let arrowDecl = arrowType && ('url(#' + arrowType + ')')

      let arrowAtLabelAdjust = 0
      const labelArrowDecl = null
      const myLabelArrowHead = (arcDesc && arcDesc.labelArrow)
      if (myLabelArrowHead) {
        const labelArrowName = (leftToRight
          ? (symmetric && myLabelArrowHead) || 'none'
          : myLabelArrowHead || 'triangle,5') + ',' + color
        const labelArrowSplit = labelArrowName.split(',')
        arrowAtLabelAdjust = (labelArrowSplit[0] !== 'none' && parseInt(labelArrowSplit[1], 10)) || 0
        if (ufoCatcher) {
          arrowAtLabelAdjust = -arrowAtLabelAdjust
        }
      }

      const renderArcSegmentPath = (path: PathCommand[]) => {
        this.svg.path(path)
          .css('stroke', color)
          .stroke({ dasharray: dashArray })
          .attr({
            'marker-start': labelArrowDecl,
            'marker-end': arrowDecl
          })
          .addTo(arcGroup)

        if (arc.marked && shadowGroup) {
          this.svg.path(path)
            .addClass('shadow_EditHighlight_arc')
            .stroke({
              width: this.markedArcStroke,
              dasharray: dashArray
            })
            .addTo(shadowGroup)
        }

        if (arc.shadowClass && shadowGroup) {
          this.svg.path(path)
            .addClass(`shadow_${arc.shadowClass}`)
            .stroke({
              width: this.shadowStroke,
              dasharray: dashArray
            })
            .addTo(shadowGroup)
        }
      }

      let pathLeft = this.makeLeftArcSegmentPath(left, textStart, arrowAtLabelAdjust, height, rowIndex, leftRow, from, ufoCatcherMod, ufoCatcher, leftBox, leftToRight, arc)
      renderArcSegmentPath(pathLeft)

      if (!symmetric) {
        myArrowHead = (arcDesc && arcDesc.arrowHead)
        arrowName = (leftToRight
          ? myArrowHead || 'triangle,5'
          : 'none') + ',' + color
      }

      arrowType = arrows[arrowName]
      arrowDecl = arrowType && ('url(#' + arrowType + ')')

      let pathRight = this.makeRightArcSegmentPath(right, textEnd, arrowAtLabelAdjust, height, rowIndex, rightRow, to, ufoCatcherMod, ufoCatcher, rightBox, leftToRight, arc)
      renderArcSegmentPath(pathRight)
    } // arc rows
  }

  private makeRightArcSegmentPath(right: Fragment, textEnd: number, arrowAtLabelAdjust: number, height: number, rowIndex: number, rightRow: number, to: number, ufoCatcherMod: number, ufoCatcher: boolean, rightBox: RectBox, leftToRight: boolean, arc: Arc) {
    const arrowEnd = textEnd + arrowAtLabelAdjust
    let path: PathCommand[] = [['M', arrowEnd, -height]]
    if (rowIndex !== rightRow || right.span.id === "rel:1-after") {
      // Render straight line pointing to the end of the row
      path.push(['L', to, -height])
      return path
    }

    // Render curve pointing to the right annotation endpoint
    let cornerx = to - (this.rtlmode ? -1 : 1) * ufoCatcherMod * this.arcSlant

    // TODO: duplicates above in part, make funcs
    // for normal cases, should not be past textEnd even if narrow
    if (this.rtlmode) {
      if (!ufoCatcher && cornerx > arrowEnd - 1) {
        cornerx = arrowEnd - 1
      }
    } else {
      if (!ufoCatcher && cornerx < arrowEnd + 1) {
        cornerx = arrowEnd + 1
      }
    }

    if (this.smoothArcCurves) {
      let controlx: number
      let endy: number
      if (this.rtlmode) {
        controlx = ufoCatcher
          ? cornerx + 2 * ufoCatcherMod * this.reverseArcControlx
          : this.smoothArcSteepness * to + (1 - this.smoothArcSteepness) * cornerx
        endy = rightBox.y + (leftToRight && !arc.equiv
          ? Configuration.visual.margin.y
          : rightBox.height / 2)
      } else {
        controlx = ufoCatcher
          ? cornerx - 2 * ufoCatcherMod * this.reverseArcControlx
          : this.smoothArcSteepness * to + (1 - this.smoothArcSteepness) * cornerx
        endy = rightBox.y + (leftToRight && !arc.equiv
          ? Configuration.visual.margin.y
          : rightBox.height / 2)
      }

      // no curving for short lines covering short vertical
      // distances, the arrowheads can go off (#925)
      if (Math.abs(-height - endy) < 2 &&
        Math.abs(cornerx - to) < 5) {
        endy = -height
      }

      path.push(['L', cornerx, -height])
      path.push(['Q', controlx, -height, to, endy])
    } else {
      path.push(['L', cornerx, -height])
      path.push(['L', to, rightBox.y + (leftToRight && !arc.equiv ? Configuration.visual.margin.y : rightBox.height / 2)])
    }
    return path
  }

  private makeLeftArcSegmentPath(left: Fragment, textStart: number, arrowAtLabelAdjust: number, height: number, rowIndex: number, leftRow: number, from: number, ufoCatcherMod: number, ufoCatcher: boolean, leftBox: RectBox, leftToRight: boolean, arc: Arc) {
    const arrowStart = textStart - arrowAtLabelAdjust
    if (isNaN(arrowStart)) {
      debugger
    }
    let path: PathCommand[] = [['M', arrowStart, -height]]
    if (rowIndex !== leftRow || left.span.id === "rel:0-before") {
      // Render straight line pointing to the start of the row
      path.push(['L', from, -height])
      return path
    }

    // Render curve pointing to the left annotation endpoint
    let cornerx = from + (this.rtlmode ? -1 : 1) * ufoCatcherMod * this.arcSlant
    if (this.rtlmode) {
      if (!ufoCatcher && cornerx < arrowStart + 1) {
        cornerx = arrowStart + 1
      }
    } else {
      if (!ufoCatcher && cornerx > arrowStart - 1) {
        cornerx = arrowStart - 1
      }
    }

    if (this.smoothArcCurves) {
      let controlx: number
      let endy: number
      if (this.rtlmode) {
        controlx = ufoCatcher
          ? cornerx - 2 * ufoCatcherMod * this.reverseArcControlx
          : this.smoothArcSteepness * from + (1 - this.smoothArcSteepness) * cornerx
        endy = leftBox.y + (leftToRight && !arc.equiv
          ? Configuration.visual.margin.y
          : leftBox.height / 2)
      } else {
        controlx = ufoCatcher
          ? cornerx + 2 * ufoCatcherMod * this.reverseArcControlx
          : this.smoothArcSteepness * from + (1 - this.smoothArcSteepness) * cornerx
        endy = leftBox.y + (leftToRight || arc.equiv
          ? leftBox.height / 2
          : Configuration.visual.margin.y)
      }

      // no curving for short lines covering short vertical
      // distances, the arrowheads can go off (#925)
      if (Math.abs(-height - endy) < 2 &&
        Math.abs(cornerx - from) < 5) {
        endy = -height
      }
      path.push(['L', cornerx, -height])
      path.push(['Q', controlx, -height, from, endy])
    } else {
      path.push(['L', cornerx, -height])
      path.push(['L', from, leftBox.y + (leftToRight || arc.equiv ? leftBox.height / 2 : Configuration.visual.margin.y)])
    }
    return path
  }

  private renderArcLabel(docData: DocumentData, arc: Arc, to: number, from: number, height: number, arcGroup: G, shadowGroup: import("@svgdotjs/svg.js").G | undefined) {
    const originType = docData.spans[arc.origin].type
    const arcLabels = Util.getArcLabels(this.entityTypes, originType, arc.type, this.relationTypes)
    let labelText = Util.arcDisplayForm(this.entityTypes, originType, arc.type, this.relationTypes)
    // if (Configuration.abbrevsOn && !ufoCatcher && arcLabels) {
    if (Configuration.abbrevsOn && arcLabels) {
      let labelIdx = 1 // first abbreviation

      // strictly speaking 2*arcSlant would be needed to allow for
      // the full-width arcs to fit, but judged abbreviated text
      // to be more important than the space for arcs.
      const maxLength = (to - from) - (this.arcSlant)
      while (docData.sizes.arcs.widths[labelText] > maxLength && arcLabels[labelIdx]) {
        labelText = arcLabels[labelIdx]
        labelIdx++
      }
    }

    if (arc.eventDescId && docData.eventDescs[arc.eventDescId]) {
      if (docData.eventDescs[arc.eventDescId].labelText) {
        labelText = docData.eventDescs[arc.eventDescId].labelText
      }
    }

    // guess at the correct baseline shift to get vertical centering.
    // (CSS dominant-baseline can't be used as not all SVG renders support it.)
    const baselineShift = docData.sizes.arcs.height / 4
    this.svg.plain(labelText)
      .amove((from + to) / 2, -height + baselineShift)
      .attr({
        // 'fill': color,
        fill: '#000000',
        'data-arc-role': arc.type,
        'data-arc-origin': arc.origin,
        'data-arc-target': arc.target,
        // TODO: confirm this is unused and remove.
        // 'data-arc-id': arc.id,
        'data-arc-ed': arc.eventDescId
      })
      .addTo(arcGroup)

    const width = docData.sizes.arcs.widths[labelText]
    const textBox = {
      x: (from + to - width) / 2,
      width,
      y: -height - docData.sizes.arcs.height / 2,
      height: docData.sizes.arcs.height
    }

    if (arc.marked && shadowGroup) {
      this.svg.rect()
        .move(textBox.x - this.markedArcSize, textBox.y - this.markedArcSize)
        .width(textBox.width + 2 * this.markedArcSize)
        .height(textBox.height + 2 * this.markedArcSize)
        .attr({
          filter: 'url(#Gaussian_Blur)',
          class: 'shadow_EditHighlight',
          rx: this.markedArcSize,
          ry: this.markedArcSize
        })
        .addTo(shadowGroup)
    }

    if (arc.shadowClass && shadowGroup) {
      this.renderArcShadow(arc, shadowGroup, textBox)
    }
    let textStart = textBox.x
    let textEnd = textBox.x + textBox.width

    // adjust by margin for arc drawing
    textStart -= Configuration.visual.arcTextMargin * (this.fontZoom / 100.0)
    textEnd += Configuration.visual.arcTextMargin * (this.fontZoom / 100.0)

    if (from > to) {
      const tmp = textStart
      textStart = textEnd
      textEnd = tmp
    }

    return { textStart, textEnd }
  }

  renderFragmentConnectors (docData: DocumentData, rows: Row[]) {
    for (const span of Object.values(docData.spans)) {
      const numConnectors = span.fragments.length - 1
      for (let connectorNo = 0; connectorNo < numConnectors; connectorNo++) {
        const left = span.fragments[connectorNo]
        const right = span.fragments[connectorNo + 1]

        const leftBox = left.rowBBox()
        const rightBox = right.rowBBox()
        const leftRow = left.chunk.row.index
        const rightRow = right.chunk.row.index

        for (let rowIndex = leftRow; rowIndex <= rightRow; rowIndex++) {
          const row = rows[rowIndex]
          if (row.chunks.length) {
            row.hasAnnotations = true

            let from: number
            if (rowIndex === leftRow) {
              from = this.rtlmode ? leftBox.x : leftBox.x + leftBox.width
            } else {
              from = this.rtlmode ? this.canvasWidth - 2 * Configuration.visual.margin.y - this.sentNumMargin : this.sentNumMargin
            }

            let to: number
            if (rowIndex === rightRow) {
              to = this.rtlmode ? rightBox.x + rightBox.width : rightBox.x
            } else {
              to = this.rtlmode ? 0 : this.canvasWidth - 2 * Configuration.visual.margin.y
            }

            let height = leftBox.y + leftBox.height - Configuration.visual.margin.y
            if (this.roundCoordinates) {
              // don't ask
              height = (height | 0) + 0.5
            }

            this.svg.path([['M', from, height], ['L', to, height]])
              .css('stroke', this.fragmentConnectorColor)
              .stroke({ dasharray: this.fragmentConnectorDashArray })
              .addTo(row.arcs)
          }
        } // rowIndex
      } // connectorNo
    }
  }

  /**
   * @return {number} how much the actual horizontal space required extends over the allocated space
   */
  renderResizeSvg (docData: DocumentData, y: number, textGroup: SVGTypeMapping<SVGGElement>, maxTextWidth: number): number {
    // resize the SVG
    let width = maxTextWidth + this.sentNumMargin + 2 * Configuration.visual.margin.x + 1

    // Loops over the rows to check if the width calculated so far is still not enough. This
    // currently happens sometimes if there is a single annotation on many words preventing
    // wrapping within the annotation (aka oversizing).
    for (const textRow of textGroup.children().filter(e => e.hasClass('text-row'))) {
      // const rowInitialSpacing = $($(textRow).children('.row-initial')[0]);
      const rowFinalSpacing = textRow.children().filter(e => e.hasClass('row-final'))[0]
      const lastChunkWidth = docData.sizes.texts.widths[rowFinalSpacing.prev().node.textContent]
      const lastChunkOffset = parseFloat(rowFinalSpacing.prev().node.getAttribute('x'))
      if (this.rtlmode) {
        // Not sure what to calculate here
      } else {
        if (lastChunkOffset + lastChunkWidth > width) {
          width = lastChunkOffset + lastChunkWidth
        }
      }
    }

    let oversized = Math.max(width - this.canvasWidth, 0)
    if (oversized > 0) {
      this.svg.attr('width', `${this.baseCanvasWidth}`)
      this.canvasWidth = width
      // Allow some extra space for arcs
      this.canvasWidth += 32
      oversized += 32
    }

    this.svg.attr('width', `${this.canvasWidth}`)
    Util.profileStart('height')
    this.svg.attr('height', `${y}`)
    Util.profileEnd('height')
    this.svg.attr('viewBox', `0 0 ${this.canvasWidth} ${y}`)

    // Originally, this code was within the oversized > 0 block above, but we moved it here
    // to prevent erratic jumping
    this.svg.attr('height', `${y + 4}`) // Need to take the hairline border into account here

    return oversized
  }

  renderRows (docData: DocumentData, rows: Row[], sentNumGroup: SVGTypeMapping<SVGGElement>, backgroundGroup: SVGTypeMapping<SVGGElement>): number {
    // position the rows
    let y = Configuration.visual.margin.y
    let currentSent = 0
    for (const row of rows) {
      // find the maximum fragment height
      row.updateFragmentHeight(docData.sizes.texts.height)
      row.updateRowBoxHeight(this.rowSpacing, this.rowPadding)

      if (row.sentence) {
        currentSent = row.sentence
      }

      this.renderRowBackground(docData, backgroundGroup, row, y, currentSent)

      y += row.boxHeight
      y += docData.sizes.texts.height
      row.textY = y - this.rowPadding

      this.renderRowNumber(docData, sentNumGroup, row, y)

      let rowY = y - this.rowPadding
      if (this.roundCoordinates) {
        rowY = rowY | 0
      }
      this.fastTranslate(row, 0, rowY)

      y += Configuration.visual.margin.y
    }

    y += Configuration.visual.margin.y

    return y
  }

  renderRowBackground (docData: DocumentData, backgroundGroup: SVGTypeMapping<SVGGElement>, row: Row, y: number, currentSent: number) {
    let bgClass: string
    if (Configuration.textBackgrounds === 'striped') {
      // give every other sentence a different bg class
      bgClass = 'background' + row.backgroundIndex
    } else {
      // plain "standard" bg
      bgClass = 'background0'
    }

    const textSizes = docData.sizes.texts
    this.svg.rect(this.canvasWidth, row.boxHeight + textSizes.height + 1)
      .move(0, y + textSizes.y + textSizes.height)
      .addClass(bgClass)
      .addTo(backgroundGroup)

    if (row.sentence && docData.markedSent[currentSent]) {
      this.svg.rect()
        .move(this.rtlmode ? this.canvasWidth - this.sentNumMargin : 0, y + textSizes.y + textSizes.height)
        .width(this.sentNumMargin)
        .height(row.boxHeight + textSizes.height + 1)
        .addClass('backgroundHighlight')
        .addTo(backgroundGroup)
    }
  }

  renderRowNumber (docData: DocumentData, sentNumGroup: SVGTypeMapping<SVGGElement>, row: Row, y: number) {
    if (row.sentence) {
      // Render sentence number as link
      let textX: number
      if (this.rtlmode) {
        textX = (this.canvasWidth - this.sentNumMargin + Configuration.visual.margin.x)
      } else {
        textX = (this.sentNumMargin - Configuration.visual.margin.x)
      }
      const text = this.svg.plain(`${row.sentence}`)
        .attr('data-sent', row.sentence)
        .css('cursor', 'pointer')
      // Setting the attributes directly is faster than calling amove() since the latter internally
      // triggers a relayout
      text.node.setAttribute('x', `${textX}`)
      text.node.setAttribute('y', `${y - this.rowPadding}`)
      text.addTo(sentNumGroup)

      const sentComment = docData.sentComment[row.sentence]
      if (sentComment) {
        const box = text.rbox(sentNumGroup)
        // TODO: using rectShadowSize, but this shadow should probably have its own setting for shadow size
        const highlight = this.svg.rect()
          .move(
            this.rtlmode ? box.x - this.rectShadowSize : box.x - this.rectShadowSize,
            box.y - this.rectShadowSize)
          .width(box.width + 2 * this.rectShadowSize)
          .height(box.height + 2 * this.rectShadowSize)
          .attr({
            class: 'shadow_' + sentComment.type,
            filter: 'url(#Gaussian_Blur)',
            rx: this.rectShadowRounding,
            ry: this.rectShadowRounding,
            'data-sent': row.sentence
          })
          .addTo(sentNumGroup)

        text.insertAfter(highlight)
        text.css('fill', '#000')
      }
    }
  }

  renderAdjustLayoutForScriptDirection (oversized, rows: Row[], textGroup: SVGTypeMapping<SVGGElement>, sentNumGroup: SVGTypeMapping<SVGGElement>) {
    if (oversized <= 0) return

    const scrollable = findClosestHorizontalScrollable(this.svg.node)

    if (this.rtlmode) {
      rows.map(row => this.fastTranslate(row, oversized, row.translation.y))

      this.fastTranslateGroup(this.highlightGroup, oversized, 0)
      this.fastTranslateGroup(textGroup, oversized, 0)
      this.fastTranslateGroup(sentNumGroup, oversized, 0)

      if (scrollable) {
        scrollable.scrollLeft = oversized + 4
      }
    } else {
      if (scrollable) {
        scrollable.scrollLeft = 0
      }
    }
  }

  renderAdjustBackgroundsForOversize (oversized: number, backgroundGroup: SVGTypeMapping<SVGGElement>) {
    if (oversized <= 0) return

    // Allow some extra space for arcs
    backgroundGroup.width(this.canvasWidth)
    for (const element of backgroundGroup.children()) {
      // We render the backgroundHighlight only in the margin, so we have to translate
      // it instead of transforming it.

      if (element.hasClass('backgroundHighlight')) {
        if (this.rtlmode) {
          element.translate(oversized, 0)
        }
      } else {
        element.width(this.canvasWidth)
      }
    }
  }

  renderAdjustLayoutRowSpacing (docData: DocumentData, textGroup: SVGTypeMapping<SVGGElement>) {
    // Go through each row and adjust the row-initial and row-final spacing
    for (const textRow of textGroup.children().filter(c => c.hasClass('text-row'))) {
      const rowInitialSpacing = textRow.children().filter(c => c.hasClass('row-initial'))[0]
      const rowFinalSpacing = textRow.children().filter(c => c.hasClass('row-final'))[0]
      // const firstChunkWidth = docData.sizes.texts.widths[rowInitialSpacing.next()[0].textContent];
      const lastChunkWidth = docData.sizes.texts.widths[rowFinalSpacing.prev().node.textContent]
      const lastChunkOffset = parseFloat(rowFinalSpacing.prev().node.getAttribute('x'))

      if (this.rtlmode) {
        const initialSpacingWidth = Configuration.visual.margin.x + this.rowPadding + 1
        const initialSpacingX = this.canvasWidth - this.sentNumMargin - initialSpacingWidth
        rowInitialSpacing.attr('x', initialSpacingX) // Faster than x() which calls bbox()
        rowInitialSpacing.attr('textLength', initialSpacingWidth)

        const finalSpacingX = Math.max(lastChunkOffset - lastChunkWidth, 0)
        const finalSpacingWidth = finalSpacingX
        rowFinalSpacing.attr('x', finalSpacingX) // Faster than x() which calls bbox()
        rowFinalSpacing.attr('textLength', finalSpacingWidth)
      } else {
        const initialSpacingX = this.sentNumMargin
        const initialSpacingWidth = Configuration.visual.margin.x + this.rowPadding + 1
        rowInitialSpacing.attr('x', initialSpacingX) // Faster than x() which calls bbox()
        rowInitialSpacing.attr('textLength', initialSpacingWidth)

        const finalSpacingX = lastChunkOffset + lastChunkWidth + 1
        const finalSpacingWidth = this.canvasWidth - finalSpacingX
        rowFinalSpacing.attr('x', finalSpacingX) // Faster than x() which calls bbox()
        rowFinalSpacing.attr('textLength', finalSpacingWidth)
      }
    }
  }

  renderAdjustLayoutInterRowSpacers (docData: DocumentData, y: number, textGroup: SVGTypeMapping<SVGGElement>) {
    // Go through each row and add an unselectable spacer between this row and the next row
    // While the space is unselectable, it will still help in guiding the browser into which
    // direction the selection should in principle go and thus avoids jumpyness.
    let prevRowRect = { y: 0, height: 0 }

    const textRows = $(textGroup.node).children('.text-row')
    textRows.each((rowIndex, textRow) => {
      const rowRect = {
        y: parseFloat($(textRow).children()[0].getAttribute('y')) + 2, height: docData.sizes.texts.height
      }
      const spaceHeight = rowRect.y - (prevRowRect.y + rowRect.height) + 2

      // Adding a spacer between the rows. We make is a *little* bit larger than necessary
      // to avoid exposing areas where the background shines through and which would again
      // cause jumpyness during selection.
      const beforeSpacer = this.verticalSpacer(
        Math.floor(prevRowRect.y),
        Math.ceil(spaceHeight))
      if (beforeSpacer) {
        textRow.before(beforeSpacer)
      }

      prevRowRect = rowRect

      // Add a spacer below the final row until the end of the canvas
      if (rowIndex === textRows.length - 1) {
        const lastSpacerY = Math.floor(rowRect.y + rowRect.height)
        const afterSpacer = this.verticalSpacer(
          Math.floor(rowRect.y + rowRect.height),
          Math.ceil(y - lastSpacerY) + 1)
        if (afterSpacer) {
          textRow.after(afterSpacer)
        }
      }
    })
  }

  /**
   * @param {SourceData} sourceData
   */
  renderDataReal (sourceData?: SourceData) {
    // Check if the SVG is actually on the page
    if (!this.svgContainer.ownerDocument.contains(this.svg.node)) {
      console.trace('Recieved render request for stale SVG that is no longer on the page. Ignoring.')
      return
    }

    try {
      Util.profileEnd('before render')
      Util.profileStart('render')
      this.dispatcher.post('startedRendering', [this.args])

      if (!sourceData && !this.data) {
        this.dispatcher.post('doneRendering', [this.args])
        return
      }

      this.svgContainer.style.visibility = 'visible'
      if (this.drawing) {
        this.redraw = true
        this.dispatcher.post('doneRendering', [this.args])
        return
      }

      if (sourceData) {
        this.setData(sourceData)
      }

      // Prevent re-rendering from interfering with the selection
      if (this.selectionInProgress) {
        this.dataChangedButNotRendered = true
        return
      }

      this.redraw = false
      this.drawing = true

      if (this.data) {
        this.renderText(this.data) // chunks

        // sort by "from"; we don't need to sort by "to" as well,
        // because unlike spans, chunks are disjunct
        this.markedText.sort((a, b) => Util.cmp(a[0], b[0]))
        this.applyMarkedTextToChunks(this.markedText, this.data.chunks)
      }

      this.svg.clear()
      this.svg.attr('direction', null)

      if (!this.data || !this.sourceData) {
        return
      }

      Util.profileStart('init')
      if (this.rtlmode) {
        this.svg.attr('direction', 'rtl')
      }

      this.svg.attr('style', `font-size: ${this.fontZoom}%;`)
      this.sentNumMargin = 40 * (this.fontZoom / 100.0)

      const scrollable = findClosestVerticalScrollable(this.svg.node)
      const svgWidth = $(this.svgContainer).width()
      this.baseCanvasWidth = svgWidth
      this.canvasWidth = svgWidth - (!scrollable ? scrollbarWidth() : 0)

      this.addHeaderAndDefs()

      const backgroundGroup: SVGTypeMapping<SVGGElement> = this.svg.group()
        .addClass('background')
        .attr('pointer-events', 'none')

      const sentNumGroup: SVGTypeMapping<SVGGElement> = this.svg.group()
        .addClass('sentnum').addClass('unselectable')

      this.highlightGroup = this.svg.group().addClass('highlights')

      const textGroup: SVGTypeMapping<SVGGElement> = this.svg.group()
        .addClass('text')
        .addClass('contain-selection')
      Util.profileEnd('init')

      Util.profileStart('measures')
      this.data.sizes = this.calculateTextMeasurements(this.data)
      this.adjustTowerAnnotationSizes(this.data)
      const maxTextWidth = this.calculateMaxTextWidth(this.data.sizes)
      Util.profileEnd('measures')

      Util.profileStart('chunks')
      this.renderLayoutFloorsAndCurlies(this.data, this.data.spanDrawOrderPermutation)
      const [rows, fragmentHeights, markedTextHighlights] = this.renderChunks(this.data, this.sourceData, this.data.chunks, maxTextWidth)
      Util.profileEnd('chunks')

      Util.profileStart('arcsPrep')
      this.renderBumpFragmentHeightsMinimumToArcStartHeight(fragmentHeights)
      this.renderCalculateArcJumpHeight(this.data, fragmentHeights)
      this.renderSortArcs(this.data)
      this.renderAssignFragmentsToRows(rows, fragmentHeights)
      Util.profileEnd('arcsPrep')

      Util.profileStart('arcs')
      this.renderArcs(this.data, rows, fragmentHeights)
      Util.profileEnd('arcs')

      Util.profileStart('fragmentConnectors')
      this.renderFragmentConnectors(this.data, rows)
      Util.profileEnd('fragmentConnectors')

      Util.profileStart('rows')
      const y = this.renderRows(this.data, rows, sentNumGroup, backgroundGroup)
      Util.profileEnd('rows')

      Util.profileStart('chunkFinish')
      this.renderChunksPass2(this.data, textGroup, markedTextHighlights)
      Util.profileEnd('chunkFinish')

      Util.profileStart('finish')

      Util.profileStart('adjust margin')
      this.renderAdjustMargin(y, sentNumGroup)
      Util.profileEnd('adjust margin')

      Util.profileStart('resize SVG')
      const oversized = this.renderResizeSvg(this.data, y, textGroup, maxTextWidth)
      Util.profileEnd('resize SVG')

      Util.profileStart('adjust for oversize depending on script')
      this.renderAdjustLayoutForScriptDirection(oversized, rows, textGroup, sentNumGroup)
      Util.profileEnd('adjust for oversize depending on script')

      Util.profileStart('adjust backgrounds')
      this.renderAdjustBackgroundsForOversize(oversized, backgroundGroup)
      Util.profileEnd('adjust backgrounds')

      Util.profileStart('row-spacing-adjust')
      this.renderAdjustLayoutRowSpacing(this.data, textGroup)
      Util.profileEnd('row-spacing-adjust')

      Util.profileStart('inter-row space')
      this.renderAdjustLayoutInterRowSpacers(this.data, y, textGroup)
      Util.profileEnd('inter-row space')

      Util.profileEnd('finish')
      Util.profileEnd('render')
      Util.profileReport()

      this.drawing = false
      if (this.redraw) {
        this.redraw = false
      }

      this.dispatcher.post('doneRendering', [this.args])
    } catch (e) {
    // We are sure not to be drawing anymore, reset the state
      this.drawing = false
      console.error('Rendering terminated due to: ' + e, e.stack)
    }
  }

  private renderAdjustMargin (y: number, sentNumGroup: SVGTypeMapping<SVGGElement>) {
    let path: PathCommand[]
    if (this.rtlmode) {
      path = [['M', this.canvasWidth - this.sentNumMargin, 0],
        ['L', this.canvasWidth - this.sentNumMargin, y]]
    } else {
      path = [['M', this.sentNumMargin, 0],
        ['L', this.sentNumMargin, y]]
    }
    this.svg.path(path).addTo(sentNumGroup)
  }

  /**
   * @param {SourceData} sourceData
   */
  renderData (sourceData?: SourceData) {
    Util.profileEnd('invoke getDocument')

    // Fill in default values that don't necessarily go over the protocol
    if (sourceData) {
      setSourceDataDefaults(sourceData)
    }

    this.renderDataReal(sourceData)
  }

  /**
   * Re-render using the known source data. This is necessary if the source data has changed e.g.
   * due to a partial update from the server.
   */
  rerender () {
    this.renderDataReal(this.sourceData)
  }

  /**
   * Differential updates for brat view.
   */
  renderDataPatch (patchData: ReadonlyArray<Operation>) {
    Util.profileEnd('invoke getDocument')
    try {
      this.sourceData = jsonpatch.applyPatch(this.sourceData, patchData).newDocument
      this.rerender()
    } catch (error) {
      console.warn('Error applying patch - reloading annotations', error)
      this.dispatcher.post('loadAnnotations', [])
    }
  }

  renderDocument () {
    Util.profileStart('invoke getDocument')
    this.dispatcher.post('ajax', [{ action: 'getDocument' }, 'renderData'])
  }

  triggerRender () {
    if (this.svg && ((this.isRenderRequested && this.isCollectionLoaded) || this.requestedData)) {
      this.isRenderRequested = false

      if (this.requestedData) {
        Util.profileClear()
        Util.profileStart('before render')
        this.renderData(this.requestedData)
        return
      }

      Util.profileClear()
      Util.profileStart('before render')
      this.renderDocument()
    }
  }

  requestRenderData (sourceData) {
    this.requestedData = sourceData
    this.triggerRender()
  }

  collectionChanged () {
    this.isCollectionLoaded = false
  }

  loadAnnotations () {
    this.isRenderRequested = true
    this.triggerRender()
  }

  onMouseOutSpan (evt: MouseEvent) {
    if (!this.data) return

    const target = $(evt.target)
    const id = target.attr('data-span-id')
    const span = this.data.spans[id]

    if (span.hidden) { return }

    if (evt.target) {
      evt.target.dispatchEvent(new AnnotationOutEvent({ vid: id, layer: { id: span.type, name: Util.spanDisplayForm(this.entityTypes, span.type) } }, evt.originalEvent))
    }
  }

  onMouseOverSpan (evt: MouseEvent) {
    if (!this.data) return

    const target = $(evt.target)
    const id = target.attr('data-span-id')
    const span = this.data.spans[id]

    if (span.hidden) { return }

    if (evt.target) {
      const fakeSpan = new Span()
      fakeSpan.vid = id
      fakeSpan.document = { text: this.data.text }
      fakeSpan.layer = { id: span.type, name: Util.spanDisplayForm(this.entityTypes, span.type) }
      if (span.comment) {
        if (span.comment.type === 'AnnotationError') {
          fakeSpan.comments = [{ type: 'error', comment: span.comment.text }]
        } else {
          fakeSpan.comments = [{ type: 'info', comment: span.comment.text }]
        }
      }
      evt.target.dispatchEvent(new AnnotationOverEvent(fakeSpan, evt.originalEvent))
    }

    if (span.actionButtons) {
      this.dispatcher.post('displaySpanButtons', [evt, target])
    }

    this.renderHighlightBoxes(span)

    const equivs: Record<string, boolean> = {}
    const spans: Record<string, boolean> = {}
    spans[id] = true
    // find all arcs, normal and equiv. Equivs need to go far (#1023)
    const addArcAndSpan = (arc: Arc) => {
      if (arc.equiv) {
        equivs[arc.eventDescId.substr(0, arc.eventDescId.indexOf('*', 2) + 1)] = true
        const eventDesc = this.data.eventDescs[arc.eventDescId]
        for (const ospan of eventDesc.leftSpans.concat(eventDesc.rightSpans)) {
          spans[ospan] = true
        }
      } else {
        spans[arc.origin] = true
      }
    }
    span.incoming.map(arc => addArcAndSpan(arc))
    span.outgoing.map(arc => addArcAndSpan(arc))

    this.highlightArcs = this.svg
      .find(`g[data-from="${id}"], g[data-to="${id}"]`)
      .map(e => e.addClass('highlight'))

    const equivSelector = Object.keys(equivs).map(equiv => `[data-arc-ed^="${equiv}"]`)
    if (equivSelector.length) {
      this.highlightArcs.push(...this.svg
        .find(equivSelector.join(', '))
        .map(e => {
          e.parent()?.addClass('highlight')
          return e
        }))
    }

    const spanIds = Object.keys(spans).map(spanId => `[data-span-id="${spanId}"]`)
    if (spanIds.length) {
      this.svg
        .find(spanIds.join(', '))
        .map(e => {
          e.parent()?.addClass('highlight')
          return e
        })
    }
  }

  private renderHighlightBoxes (span: Entity) {
    if (span.hidden) return

    const spanDesc = this.entityTypes[span.type]
    const bgColor = span.color || ((spanDesc && spanDesc.bgColor) || '#ffffff')

    this.highlight = []
    for (const fragment of span.fragments) {
      this.renderHighlightBox(fragment, bgColor)
    }
  }

  private renderHighlightBox (fragment: Fragment, bgColor: string) {
    if (!fragment.highlightPos) return

    const highlightBox = this.svg.rect(fragment.highlightPos.w, fragment.highlightPos.h)
      .translate(fragment.highlightPos.x, fragment.highlightPos.y)
      .fill(bgColor)
      .opacity(0.75)
      .attr({
        rx: this.highlightRounding.x,
        ry: this.highlightRounding.y
      })
      .addTo(this.highlightGroup)
    this.highlight.push(highlightBox)
  }

  onMouseOutArc (evt: MouseEvent) {
    if (!this.data) return

    const target = $(evt.target)
    const originSpanId = target.attr('data-arc-origin')
    const role = target.attr('data-arc-role')
    const arcEventDescId: string = target.attr('data-arc-ed')
    let arcId: string | undefined

    if (arcEventDescId) {
      const eventDesc = this.data.eventDescs[arcEventDescId]
      if (eventDesc.relation) {
        // among arcs, only ones corresponding to relations have "independent" IDs
        arcId = arcEventDescId
      }
    }

    const originSpanType = this.data.spans[originSpanId].type || ''

    if (arcId) {
      if (evt.target) {
        const labelText = Util.arcDisplayForm(this.entityTypes, originSpanType, role, this.relationTypes)
        evt.target.dispatchEvent(new AnnotationOutEvent({ vid: arcId, layer: { id: role, name: labelText } }, evt.originalEvent))
      }
    }
  }

  onMouseOverArc (evt: MouseEvent) {
    if (!this.data) return

    const target = $(evt.target)
    const originSpanId = target.attr('data-arc-origin')
    const targetSpanId = target.attr('data-arc-target')
    const role = target.attr('data-arc-role')
    const symmetric = (this.relationTypes &&
      this.relationTypes[role] &&
      this.relationTypes[role].properties &&
      this.relationTypes[role].properties.symmetric)

    // NOTE: no commentText, commentType for now
    /** @type {string} */ const arcEventDescId: string = target.attr('data-arc-ed')
    let commentText = ''
    let commentType = ''
    let arcId: string | undefined

    if (arcEventDescId) {
      const eventDesc = this.data.eventDescs[arcEventDescId]
      const comment = eventDesc.comment
      if (comment) {
        commentText = comment.text
        commentType = comment.type
        if (commentText === '' && commentType) {
          // default to type if missing text
          commentText = commentType
        }
      }
      if (eventDesc.relation) {
        // among arcs, only ones corresponding to relations have "independent" IDs
        arcId = arcEventDescId
      }
    }

    const originSpanType = this.data.spans[originSpanId].type || ''

    if (arcId) {
      if (evt.target) {
        const fakeRelation = new Relation()
        fakeRelation.vid = arcId
        const labelText = Util.arcDisplayForm(this.entityTypes, originSpanType, role, this.relationTypes)
        fakeRelation.layer = { id: role, name: labelText }
        if (commentText) {
          if (commentType === 'AnnotationError') {
            fakeRelation.comments = [{ type: 'error', comment: commentText }]
          } else {
            fakeRelation.comments = [{ type: 'info', comment: commentText }]
          }
        }
        evt.target.dispatchEvent(new AnnotationOverEvent(fakeRelation, evt.originalEvent))
      }

      this.highlightArcs = this.svg
        .find(`g[data-id="${arcId}"]`)
        .map(e => e.addClass('highlight'))
    } else {
      this.highlightArcs = this.svg
        .find(`g[data-from="${originSpanId}"][data-to="${targetSpanId}"]`)
        .map(e => e.addClass('highlight'))
    }

    this.svg
      .find(`[data-span-id="${originSpanId}"], [data-span-id="${targetSpanId}"]`)
      .map(e => {
        e.parent()?.addClass('highlight')
        return e
      })
  }

  onMouseOverSentence (evt: MouseEvent) {
    if (!this.data || !(evt.target instanceof Element)) return

    const id = evt.target.getAttribute('data-sent')
    if (id) {
      const comment = this.data.sentComment[id]
      if (comment) {
        if (evt.target) {
          const fakeSpan = new Span()
          fakeSpan.vid = comment.id
          fakeSpan.document = { text: this.data.text }
          fakeSpan.layer = { id: 0, name: Util.spanDisplayForm(this.entityTypes, comment.type) }
          evt.target.dispatchEvent(new AnnotationOverEvent(fakeSpan, evt.originalEvent))
        }
      }
    }
  }

  onMouseOver (evt: MouseEvent) {
    if (!(evt.target instanceof Element)) return

    const target = evt.target
    if (target.getAttribute('data-span-id')) {
      this.onMouseOverSpan(evt)
      return
    }

    if (target.getAttribute('data-arc-role')) {
      this.onMouseOverArc(evt)
      return
    }

    if (target.getAttribute('data-sent')) {
      this.onMouseOverSentence(evt)
    }
  }

  onMouseOut (evt: MouseEvent) {
    if (!(evt.target instanceof Element)) return

    const target = evt.target
    target.classList.remove('badTarget')

    if (this.highlight) {
      this.highlight.map(h => h.remove())
      this.highlight = []
    }

    this.svg.node.querySelectorAll('.highlight').forEach(e => e.classList.remove('highlight'))
    this.highlightArcs = undefined

    // if (this.highlightArcs) {
    //   for (const arc of this.highlightArcs) {
    //     arc.removeClass('highlight')
    //   }
    // }

    if (target.getAttribute('data-span-id')) {
      this.onMouseOutSpan(evt)
      return
    }

    if (target.getAttribute('data-arc-role')) {
      this.onMouseOutArc(evt)
    }
  }

  onSelectionStarted () {
    this.selectionInProgress = true
  }

  onSelectionEnded () {
    this.selectionInProgress = false
    if (this.dataChangedButNotRendered) {
      this.dataChangedButNotRendered = false
      setTimeout(() => this.rerender(), 50)
    }
  }

  setAbbrevs (_abbrevsOn: boolean) {
    // TODO: this is a slightly weird place to tweak the configuration
    Configuration.abbrevsOn = _abbrevsOn
    this.dispatcher.post('configurationChanged')
  }

  setTextBackgrounds (_textBackgrounds : 'striped' | 'none') {
    Configuration.textBackgrounds = _textBackgrounds
    this.dispatcher.post('configurationChanged')
  }

  setLayoutDensity (_density: number) {
    // dispatcher.post('messages', [[['Setting layout density ' + _density, 'comment']]]);
    // TODO: store standard settings instead of hard-coding
    // them here (again)
    if (_density < 2) {
      // dense
      Configuration.visual.margin = { x: 1, y: 0 }
      Configuration.visual.boxSpacing = 1
      Configuration.visual.curlyHeight = 1
      Configuration.visual.arcSpacing = 7
      Configuration.visual.arcStartHeight = 18
    } else if (_density > 2) {
      // spacious
      Configuration.visual.margin = { x: 2, y: 1 }
      Configuration.visual.boxSpacing = 3
      Configuration.visual.curlyHeight = 6
      Configuration.visual.arcSpacing = 12
      Configuration.visual.arcStartHeight = 23
    } else {
      // standard
      Configuration.visual.margin = { x: 2, y: 1 }
      Configuration.visual.boxSpacing = 1
      Configuration.visual.curlyHeight = 4
      Configuration.visual.arcSpacing = 9
      Configuration.visual.arcStartHeight = 19
    }
    this.dispatcher.post('configurationChanged')
  }

  setSvgWidth (_width: string) {
    this.svg.attr('width', `${_width}`)
    if (Configuration.svgWidth !== _width) {
      Configuration.svgWidth = _width
      this.dispatcher.post('configurationChanged')
    }
  }

  // register event listeners
  registerHandlers (element: Element | Window | Document, events : Message[]) {
    const target = $(element)
    for (const eventName of events) {
      target.bind(eventName, (evt) => this.dispatcher.post(eventName, [evt], 'all'))
    }
  }

  loadSpanTypes (types: EntityTypeDto[]) {
    for (const type of types) {
      if (type) {
        this.entityTypes[type.type] = type
        const children = type.children
        if (children && children.length) {
          this.loadSpanTypes(children)
        }
      }
    }
  }

  // INCEpTION does not use event attributes and entity attributes
  //   loadAttributeTypes (responseTypes) {
  //   const processed = {}
  //   for (const type of responseTypes) {
  //     processed[type.type] = type
  //     // count the values; if only one, it's a boolean attribute
  //     const values: string[] = []
  //     for (const i in type.values) {
  //       if (Object.prototype.hasOwnProperty.call(type.values, i)) {
  //         values.push(i)
  //       }
  //     }
  //     if (values.length === 1) {
  //       type.bool = values[0]
  //     }
  //   }
  //   return processed
  // }

  loadRelationTypes (relationTypes: RelationTypeDto[]) {
    for (const relType of relationTypes) {
      if (relType) {
        this.relationTypes[relType.type] = relType
        const children = relType.children
        if (children && children.length) {
          this.loadRelationTypes(children)
        }
      }
    }
  }

  collectionLoaded (response: CollectionLoadedResponse) {
    setCollectionDefaults(response)
    // INCEpTION does not use event attributes and entity attributes
    // this.eventAttributeTypes = this.loadAttributeTypes(response.event_attribute_types)
    // this.entityAttributeTypes = this.loadAttributeTypes(response.entity_attribute_types)

    this.entityTypes = {}
    this.loadSpanTypes(response.entity_types)
    // INCEpTION does not use events
    // this.loadSpanTypes(response.event_types)

    this.relationTypes = {}
    this.loadRelationTypes(response.relation_types)

    const arcBundle = (response.visual_options || {}).arc_bundle || 'none'
    this.collapseArcs = arcBundle === 'all'
    this.collapseArcSpace = arcBundle !== 'none'

    this.dispatcher.post('spanAndAttributeTypesLoaded', [
      this.entityTypes, this.entityAttributeTypes, this.eventAttributeTypes, this.relationTypes])

    this.isCollectionLoaded = true
    this.triggerRender()
  }

  isReloadOkay () {
    // do not reload while the user is in the dialog
    return !this.drawing
  }

  verticalSpacer (y: number, height: number): SVGForeignObjectElement | undefined {
    if (height > 0) {
      const foreignObject = document.createElementNS('http://www.w3.org/2000/svg', 'foreignObject')
      const spacer = document.createElement('span')
      $(spacer)
        .addClass('unselectable')
        .css('display', 'inline-block')
        .css('width', '100%')
        .css('height', '100%')
        .text('\u00a0')
      $(foreignObject)
        .attr('x', this.rtlmode ? 0 : this.sentNumMargin)
        .attr('y', y)
        .attr('width', this.rtlmode ? this.canvasWidth - this.sentNumMargin : this.canvasWidth)
        .attr('height', height)
        .append(spacer)
      return foreignObject
    }
  }

  horizontalSpacer (group: SVGTypeMapping<SVGGElement>, x: number, y: number, width: number, attrs: Record<string, unknown>) {
    if (width <= 0) {
      return
    }

    // To visualize the spacing use \u2588, otherwise \u00a0
    this.svg.plain(this.rtlmode ? '\u200f\u00a0' : '\u00a0')
      .amove(x, y)
      .addClass('spacing')
      .attr({
        textLength: width,
        lengthAdjust: 'spacingAndGlyphs'
      })
      .attr(attrs)
      .addTo(group)
  }

  renderArcShadow (arc: Arc, shadowGroup: SVGTypeMapping<SVGGElement>, textBox: { x: number; y: number; width: number; height: number; }) {
    return this.svg.rect()
      .move(textBox.x - this.arcLabelShadowSize, textBox.y - this.arcLabelShadowSize)
      .width(textBox.width + 2 * this.arcLabelShadowSize)
      .height(textBox.height + 2 * this.arcLabelShadowSize)
      .attr({
        class: 'shadow_' + arc.shadowClass,
        filter: 'url(#Gaussian_Blur)',
        rx: this.arcLabelShadowRounding,
        ry: this.arcLabelShadowRounding
      })
      .addTo(shadowGroup)
  }

  renderSpanMarkedRect (yy: number, bx: number, by: number, bw: number, bh: number, fragment: Fragment): SVGJSElement {
    return this.svg.rect(bw + 2 * this.markedSpanSize, bh + 2 * this.markedSpanSize)
      .addClass('marked-rect')
      .move(bx - this.markedSpanSize, yy - this.markedSpanSize - Configuration.visual.margin.y - fragment.span.floor)
      .attr({
        filter: 'url(#Gaussian_Blur)',
        class: 'shadow_EditHighlight',
        rx: this.markedSpanSize,
        ry: this.markedSpanSize
      })
      .addTo(fragment.chunk.highlightGroup)
  }

  renderFragmentShadowRect (yy: number, bx: number, by: number, bw: number, bh: number, fragment: Fragment): SVGJSElement {
    return this.svg.rect()
      .addClass('fragment-shadow')
      .move(bx - this.rectShadowSize, yy - this.rectShadowSize - Configuration.visual.margin.y - fragment.span.floor)
      .width(bw + 2 * this.rectShadowSize)
      .height(bh + 2 * this.rectShadowSize)
      .attr({
        class: 'shadow_' + fragment.span.shadowClass,
        filter: 'url(#Gaussian_Blur)',
        rx: this.rectShadowRounding,
        ry: this.rectShadowRounding
      })
      .addTo(fragment.group)
  }

  renderFragmentRect (bx: number, by: number, bw: number, bh: number, yy: number, fragment: Fragment, rectClass: string, bgColor: string, borderColor: string): SVGTypeMapping<SVGElement> {
    const span = fragment.span
    const bx1 = bx
    const bx2 = bx1 + bw
    const by1 = yy - Configuration.visual.margin.y - span.floor
    const by2 = by1 + bh
    const poly: ArrayXY[] = []

    if (span.clippedAtStart && span.fragments[0] === fragment) {
      poly.push([bx1, by2])
      poly.push([bx1 - bh / 2, (by1 + by2) / 2])
      poly.push([bx1, by1])
    } else {
      poly.push([bx1, by2])
      poly.push([bx1, by1])
    }

    if (span.clippedAtEnd && span.fragments[span.fragments.length - 1] === fragment) {
      poly.push([bx2, by1])
      poly.push([bx2 + bh / 2, (by1 + by2) / 2])
      poly.push([bx2, by2])
    } else {
      poly.push([bx2, by1])
      poly.push([bx2, by2])
    }

    return this.svg.polygon(poly)
      .addClass(rectClass)
      .addClass('fragment')
      .fill(bgColor)
      .stroke({
        color: borderColor,
        dasharray: span.attributeMerge.dashArray as string
      })
      .attr({
        rx: Configuration.visual.margin.x,
        ry: Configuration.visual.margin.y,
        'data-span-id': span.id,
        'data-fragment-id': span.segmentedOffsetsMap[fragment.id]
      })
      .addTo(fragment.group)
  }

  renderFragmentCrossOut (xx: number, yy: number, hh: number, fragment: Fragment) {
    const span = fragment.span

    this.svg.path([ //
      ['M', xx, yy - Configuration.visual.margin.y - span.floor],
      ['L', xx + fragment.width, yy + hh + Configuration.visual.margin.y - span.floor]])
      .addClass('boxcross')
      .addTo(fragment.group)

    this.svg.path([ //
      ['M', xx + fragment.width, yy - Configuration.visual.margin.y - span.floor],
      ['L', xx, yy + hh + Configuration.visual.margin.y - span.floor]])
      .addClass('boxcross')
      .addTo(fragment.group)
  }

  renderCurly (fragment: Fragment, x: number, yy: number, hh: number) {
    const span = fragment.span

    let curlyColor = 'grey'
    if (this.coloredCurlies) {
      const spanDesc = this.entityTypes[span.type]
      let bgColor: string
      if (span.color) {
        bgColor = span.color
      } else {
        bgColor = ((spanDesc && spanDesc.bgColor) || '#000000')
      }

      curlyColor = Util.adjustColorLightness(bgColor, -0.6)
    }

    const bottom = yy + hh + Configuration.visual.margin.y - span.floor + 1
    const curlyHeight = Configuration.visual.curlyHeight
    this.svg.path([
      ['M', fragment.curly.from, bottom + curlyHeight],
      ['C', fragment.curly.from, bottom,
        x, bottom + curlyHeight,
        x, bottom],
      ['C', x, bottom + curlyHeight,
        fragment.curly.to, bottom,
        fragment.curly.to, bottom + curlyHeight]])
      .attr({
        class: 'curly',
        stroke: curlyColor
      })
      .addTo(fragment.group)
  }

  findChunkClosestToOffset (offset: number) : Chunk | null {
    let closestChunk : Chunk | null = null
    let closestNonVirtualChunk : Chunk | null = null
    let minDiff = Infinity
    let nonVirtualMinDiff = Infinity
    for (const chunk of this.data?.chunks || []) {
      const diff = Math.min(Math.abs(chunk.from - offset), Math.abs(chunk.to - offset))

      if (!chunk.virtual && diff < nonVirtualMinDiff) {
        closestNonVirtualChunk = chunk
        nonVirtualMinDiff = diff
      }

      if (diff < minDiff) {
        closestChunk = chunk
        minDiff = diff
      }
    }

    // Try to return a non-virtual chunk because we can highlight that with a ping.
    // Only if no non-virtual chunk is found, return the closest chunk.
    return closestNonVirtualChunk || closestChunk;
  }

  findChunksInRange(range: Offsets): Chunk[] {
    const overlappingChunks: Chunk[] = [];
  
    for (const chunk of this.data?.chunks || []) {
      // Check if the chunk overlaps with the range [begin, end]
      if (chunk.from <= range[1] && chunk.to >= range[0]) {
        overlappingChunks.push(chunk);
      }
    }
  
    return overlappingChunks;
  }
  getChunkElementWithId (id: VID): Element | null {
    return this.svg.node.querySelector(`text:not(.spacing)[data-chunk-id="${id}"]`)
  }

  /**
   * @returns the highlights for the given span id. Those are the elements representing the label
   *  bubbles above the text.
   * @param id the span id
   */
  getHighlightElementsForSpan (id: VID): ArrayLike<Element> {
    return this.svg.node.querySelectorAll(`[data-span-id="${id}"]`)
  }

  selectionToPoint (sel: Selection | null) : Offsets | null {
    if (!sel || !sel.rangeCount) return null

    const anchorNode = sel.getRangeAt(0).startContainer
    const anchorOffset = sel.getRangeAt(0).startOffset

    if (!anchorNode) return null

    const range = new Range()
    range.setStart(anchorNode, anchorOffset)
    range.setEnd(anchorNode, anchorOffset)

    return this.rangeToOffsets(range)
  }

  selectionToOffsets (sel: Selection | null) : Offsets | null {
    if (!sel || !sel.rangeCount) return null

    const anchorNode = sel.getRangeAt(0).startContainer
    const anchorOffset = sel.getRangeAt(0).startOffset
    const focusNode = sel.getRangeAt(sel.rangeCount - 1).endContainer
    const focusOffset = sel.getRangeAt(sel.rangeCount - 1).endOffset

    if (!anchorNode || !focusNode) return null

    const range = new Range()
    range.setStart(anchorNode, anchorOffset)
    range.setEnd(focusNode, focusOffset)

    return this.rangeToOffsets(range)
  }

  rangeToOffsets (range: Range | null) : Offsets | null {
    if (!range || !this.data) return null

    let anchorNode = findClosestChunkElement(range.startContainer)
    let anchorOffset = range.startOffset
    let focusNode = findClosestChunkElement(range.endContainer)
    let focusOffset = range.endOffset

    // If neither approach worked, give up - the user didn't click on selectable text.
    if (!anchorNode || !focusNode) {
      return null
    }

    let chunkIndexFrom = anchorNode.getAttribute('data-chunk-id')
    let chunkIndexTo = focusNode.getAttribute('data-chunk-id')

    // Is the selection fully contained in a single spacing element?
    // If yes, move it to the begin or end of the previous or next chunk.
    if (focusNode === anchorNode && focusNode.classList.contains('spacing')) {
      if (anchorOffset === 0) {
        // Move anchor to the end of the previous node
        anchorNode = focusNode = anchorNode.previousElementSibling
        anchorOffset = focusOffset = anchorNode?.textContent?.length || 0
        chunkIndexFrom = chunkIndexTo = anchorNode?.getAttribute('data-chunk-id') || null
      } else {
        // Move anchor to the beginning of the next node
        anchorNode = focusNode = anchorNode.nextElementSibling
        anchorOffset = focusOffset = 0
        chunkIndexFrom = chunkIndexTo = anchorNode?.getAttribute('data-chunk-id') || null
      }
    } else {
      // Is the selection partially contained in a spacing element?
      // If we hit a spacing element, then we shift the anchors left or right, depending on
      // the direction of the selected range.
      if (anchorNode.classList.contains('spacing')) {
        if (Number(chunkIndexFrom) < Number(chunkIndexTo)) {
          anchorNode = anchorNode.nextElementSibling || null
          anchorOffset = 0
          chunkIndexFrom = anchorNode?.getAttribute('data-chunk-id') || null
        } else if (anchorNode.classList.contains('row-initial')) {
          anchorNode = anchorNode.nextElementSibling || null
          anchorOffset = 0
        } else {
          anchorNode = anchorNode.previousElementSibling || null
          anchorOffset = anchorNode?.textContent?.length || 0
        }
      }
      if (focusNode.classList.contains('spacing')) {
        if (Number(chunkIndexFrom) > Number(chunkIndexTo)) {
          focusNode = focusNode.nextElementSibling || null
          focusOffset = 0
          chunkIndexTo = focusNode?.getAttribute('data-chunk-id') || null
        } else if (focusNode.classList.contains('row-initial')) {
          focusNode = focusNode.nextElementSibling || null
          focusOffset = 0
        } else {
          focusNode = focusNode.previousElementSibling || null
          focusOffset = focusNode?.textContent?.length || 0
        }
      }
    }

    if (chunkIndexFrom === null || chunkIndexTo === null) return null

    const chunkFrom = this.data.chunks[Number(chunkIndexFrom)]
    const chunkTo = this.data.chunks[Number(chunkIndexTo)]
    let selectedFrom = chunkFrom.from + anchorOffset
    let selectedTo = chunkTo.from + focusOffset

    if (selectedFrom > selectedTo) {
      const tmp = selectedFrom; selectedFrom = selectedTo; selectedTo = tmp
    }

    // trim
    while (selectedFrom < selectedTo && ' \n\t'.indexOf(this.data.text.substr(selectedFrom, 1)) !== -1) selectedFrom++
    while (selectedFrom < selectedTo && ' \n\t'.indexOf(this.data.text.substr(selectedTo - 1, 1)) !== -1) selectedTo--

    return [selectedFrom, selectedTo]
  }
}

/**
 * Utility function to find the closest highlight element to the given target.
 *
 * @param target a DOM node.
 * @returns the closest highlight element or null if none is found.
 */
export function findClosestChunkElement (target: Node | null): Element | null {
  if (target instanceof Text) {
    target = target.parentElement
  }

  if (target instanceof Element) {
    return target.closest('[data-chunk-id]')
  }

  return null
}

function isRTL (charCode: number): boolean {
  const t1 = (charCode >= 0x0591 && charCode <= 0x07FF)
  const t2 = (charCode === 0x200F)
  const t3 = (charCode === 0x202E)
  const t4 = (charCode >= 0xFB1D && charCode <= 0xFDFD)
  const t5 = (charCode >= 0xFE70 && charCode <= 0xFEFC)
  return t1 || t2 || t3 || t4 || t5
}

// http://24ways.org/2010/calculating-color-contrast/
// http://stackoverflow.com/questions/11867545/change-text-color-based-on-brightness-of-the-covered-background-area
function bgToFgColor (hexcolor: ColorCode) {
  const r = parseInt(hexcolor.substr(1, 2), 16)
  const g = parseInt(hexcolor.substr(3, 2), 16)
  const b = parseInt(hexcolor.substr(5, 2), 16)
  const yiq = ((r * 299) + (g * 587) + (b * 114)) / 1000
  return (yiq >= 128) ? '#000000' : '#ffffff'
}

function findClosestHorizontalScrollable (node: Element | null) {
  if (node === null || node.tagName === 'HTML') {
    return null
  }

  if (
    (node instanceof HTMLElement) &&
    ((node.style.overflowX === 'auto' && node.scrollWidth > node.clientWidth) ||
    (node.style.overflowX === 'scroll'))
  ) {
    return node
  }

  // Abort if the node is marked as scrollable but does presently not have a scrollbar. This is
  // to avoid that we consider scrollbars too far out
  if (node.classList.contains('scrollable')) {
    return null
  }

  return findClosestHorizontalScrollable(node.parentElement)
}

function findClosestVerticalScrollable (node: Element | null) {
  if (node === null || node.tagName === 'HTML') {
    return null
  }

  if (
    (node instanceof HTMLElement) &&
    ((node.style.overflowY === 'auto' && node.scrollHeight > node.clientHeight) ||
    (node.style.overflowY === 'scroll'))
  ) {
    return node
  }

  // Abort if the node is marked as scrollable but does presently not have a scrollbar. This is
  // to avoid that we consider scrollbars too far out
  if (node.classList.contains('scrollable')) {
    return null
  }

  return findClosestVerticalScrollable(node.parentElement)
}

/**
 * A naive whitespace tokeniser
 */
function tokenise (text: string): Array<Offsets> {
  const tokenOffsets: Array<Offsets> = []
  let tokenStart: number | null = null
  let lastCharPos: number | null = null

  for (let i = 0; i < text.length; i++) {
    const c = text[i]
    // Have we found the start of a token?
    if (tokenStart == null && !/\s/.test(c)) {
      tokenStart = i
      lastCharPos = i
      // Have we found the end of a token?
    } else if (/\s/.test(c) && tokenStart != null) {
      tokenOffsets.push([tokenStart, i])
      tokenStart = null
      // Is it a non-whitespace character?
    } else if (!/\s/.test(c)) {
      lastCharPos = i
    }
  }

  // Do we have a trailing token?
  if (tokenStart !== null && lastCharPos !== null) {
    tokenOffsets.push([tokenStart, lastCharPos + 1])
  }

  return tokenOffsets
}

/**
 * A naive newline sentence splitter
 */
function sentenceSplit (text: string): Array<Offsets> {
  const sentenceOffsets: Array<Offsets> = []
  let sentStart: number | null = null
  let lastCharPos = -1

  for (let i = 0; i < text.length; i++) {
    const c = text[i]
    // Have we found the start of a sentence?
    if (sentStart == null && !/\s/.test(c)) {
      sentStart = i
      lastCharPos = i
      // Have we found the end of a sentence?
    } else if (c === '\n' && sentStart != null) {
      sentenceOffsets.push([sentStart, i])
      sentStart = null
      // Is it a non-whitespace character?
    } else if (!/\s/.test(c)) {
      lastCharPos = i
    }
  }
  // Do we have a trailing sentence without a closing newline?
  if (sentStart != null) {
    sentenceOffsets.push([sentStart, lastCharPos + 1])
  }

  return sentenceOffsets
}

function overlapping(aXBegin: number, aXEnd: number, aYBegin: number, aYEnd: number): boolean {
  return aYBegin === aXBegin || aYEnd === aXEnd || (aXBegin < aYEnd && aYBegin < aXEnd);
} 

function covering(aXBegin: number, aXEnd: number, aYBegin: number, aYEnd: number): boolean {
  return aXBegin <= aYBegin && aYEnd <= aXEnd;
}

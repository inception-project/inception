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
const Visualizer = (function ($, window, undefined) {
  let svg;

  class Sizes {
    /** @type {Measurements} */ texts;
    /** @type {Measurements} */ fragments;
    /** @type {Measurements} */ arcs;

    constructor(texts, arcSizes, fragments) {
      Object.seal(this);
      this.texts = texts;
      this.arcs = arcSizes;
      this.fragments = fragments;
    }
  }

  /**
   * This class represents the JSON object that we receive from the server. Note that the class is
   * currently only used for documentation purposes. The JSON we get is not really cast into an
   * object of this class.
   */
  class SourceData {
    /** @type {String} */ text;
    /** @type {[string, string, string, string, string]} id, name, spanId, value, cueSpanId */ attributes = [];
    /** @type {[[string, string, string]]} id, type, comment */ comments = [];
    /** @type {[[string, string, [[number, number]], {}]]} id, type, offsets, attributes */ entities = [];
    equivs = [];
    events = [];
    normalizations = [];
    relations = [];
    triggers = [];
    /** @type {number[][]} */ sentence_offsets;
    /** @type {number[][]} */ token_offsets;
    /** @type {number} */ sentence_number_offset;
    /** @type {boolean} */ rtl_mode;
    /** @type {number} */ font_zoom;
    /** @type {{}} */ args;

    /**
     * @type {string}
     * @deprecated INCEpTION does not use the collection name.
     */
    collection;

    /**
     * @type {string}
     * @deprecated INCEpTION does not use the document name.
     */
    document;

    constructor() {
      Object.seal(this);
    }
  }

  /**
   * Class currently defined only for documentation purposes
   */
  class VisualOptions {
    /** @type {boolean} */ arc_bundle;

    constructor() {
      Object.seal(this);
    }
  }

  /**
   * Class currently defined only for documentation purposes
   */
  class CollectionLoadedResponse {
    event_attribute_types;
    entity_attribute_types;
    entity_types;
    event_types;
    unconfigured_types;
    relation_types;
    /** @type {VisualOptions} */ visual_options;

    constructor() {
      Object.seal(this);
    }
  }

  /**
   * Document data prepared for rendering. The JSON data we get from the server is converted into
   * this pre-rendering representation which already determines e.g. the draw orders and such but
   * which doesn't yet create the actual SVG representation.
   */
  class DocumentData {
    /** @type {string} */ text;
    /** @type {Chunk[]} */ chunks = [];
    /** @type {Object.<string, Span>} */ spans = {};
    /** @type {Object.<string, EventDesc>} */ eventDescs = {};
    /** @type {Arc[]} */ arcs = [];
    /** @type {Object.<string, Arc>} */ arcById = {};
    sentComment = {};
    /** @type {Object.<string, boolean>} */ markedSent = {};
    spanAnnTexts = {};
    /** @type {Object.<string, Fragment[]>} */ towers = {};
    /** @type {Array.<*>} */ spanDrawOrderPermutation = []
    /** @type {Sizes} */ sizes = new Sizes();

    constructor(text) {
      Object.seal(this);

      this.text = text;
    }
  }

  class RectBox {
    /** @type {number} */ x;
    /** @type {number} */ y;
    /** @type {number} */ width;
    /** @type {number} */ height;

    /**
     * @param {number} x
     * @param {number} y
     * @param {number} width
     * @param {number} height
     */
    constructor(x, y, width, height) {
      Object.seal(this);
      this.x = x;
      this.y = y;
      this.width = width;
      this.height = height;
    }
  }

  class Fragment {
    /** @type {number} */ id;
    /** @type {Span} */ span;
    /** @type {number} */ from;
    /** @type {number} */ to;
    rectBox;
    text;
    /** @type {Chunk} */ chunk;
    indexNumber;
    drawOrder;
    towerId;
    curly;
    /** @type {boolean} */ drawCurly = false;
    /** @type {string} */ labelText;
    glyphedLabelText;
    group;
    rect;
    left;
    right;
    /** @type {number} */ width;
    /** @type {number} */ height;
    nestingHeight;
    nestingHeightLR;
    nestingHeightRL;
    nestingDepth;
    nestingDepthLR;
    nestingDepthRL;
    highlightPos;

    /**
     * @param {Number} id
     * @param {Span} span
     * @param {Number} from
     * @param {Number} to
     */
    constructor(id, span, from, to) {
      Object.seal(this);

      this.id = id;
      this.span = span;
      this.from = from;
      this.to = to;
    }

    /**
     * @param {Fragment} a
     * @param {Fragment} b
     */
    static midpointComparator = (a, b) => {
      let tmp = a.from + a.to - b.from - b.to;
      if (!tmp) {
        return 0;
      }
      return tmp < 0 ? -1 : 1;
    };

    /**
     * @param {Fragment} a
     * @param {Fragment} b
     */
    static compare(a, b) {
      let tmp;
      const aSpan = a.span;
      const bSpan = b.span;

      // spans with more fragments go first
      tmp = aSpan.fragments.length - bSpan.fragments.length;
      if (tmp) {
        return tmp < 0 ? 1 : -1;
      }

      // longer arc distances go last
      tmp = aSpan.avgDist - bSpan.avgDist;
      if (tmp) {
        return tmp < 0 ? -1 : 1;
      }

      // spans with more arcs go last
      tmp = aSpan.numArcs - bSpan.numArcs;
      if (tmp) {
        return tmp < 0 ? -1 : 1;
      }

      // compare the span widths,
      // put wider on bottom so they don't mess with arcs, or shorter
      // on bottom if there are no arcs.
      const ad = a.to - a.from;
      const bd = b.to - b.from;
      tmp = ad - bd;
      if (aSpan.numArcs === 0 && bSpan.numArcs === 0) {
        tmp = -tmp;
      }

      if (tmp) {
        return tmp < 0 ? 1 : -1;
      }

      tmp = aSpan.refedIndexSum - bSpan.refedIndexSum;
      if (tmp) {
        return tmp < 0 ? -1 : 1;
      }

      // if no other criterion is found, sort by type to maintain
      // consistency
      // TODO: isn't there a cmp() in JS?
      if (aSpan.type < bSpan.type) {
        return -1;
      } else if (aSpan.type > bSpan.type) {
        return 1;
      }

      return 0;
    }
  }

  class Span {
    /** @type {string} */ id;
    /** @type {string} */ type;
    /** @type {number} */ totalDist = 0;
    /** @type {number} */ numArcs = 0;
    generalType;
    /** @type {Fragment} */ headFragment = null;
    /** @type {[[number, number]]} */ unsegmentedOffsets;
    /** @type {[[number, number]]} */ offsets = [];
    segmentedOffsetsMap = {};
    /** @type {boolean} */ hidden = false;
    /** @type {boolean} */ clippedAtStart = false;
    /** @type {boolean} */ clippedAtEnd = false;
    /** @type {Arc[]} */ incoming = [];
    /** @type {Arc[]} */ outgoing = [];
    /** @type {Object.<string, string>} */ attributes = {};
    /** @type {string[]} */attributeText = [];
    attributeCues = {};
    attributeCueFor = {};
    annotatorNotes = undefined;
    attributeMerge = {}; // for box, cross, etc. that are span-global
    /** @type {Fragment[]} */ fragments = [];
    /** @type {string} */ normalized;
    /** @type {[[string, string, string]]} */ normalizations = [];
    wholeFrom = undefined;
    wholeTo = undefined;
    comment = undefined; // { type: undefined, text: undefined };
    drawCurly = false;
    /** @type {string} */ labelText;
    refedIndexSum = undefined;
    /** @type {string} */ color;
    /** @type {string} */ shadowClass;
    floor;
    marked;
    avgDist;
    /** @type {string} */ text;
    /** @type {string} */ cue;

    /**
     * @param {*} id
     * @param {string} type
     * @param {[[number, number]]} offsets
     * @param {string} generalType
     */
    constructor(id, type, offsets, generalType) {
      Object.seal(this);

      this.id = id;
      this.type = type;
      this.unsegmentedOffsets = offsets;
      this.generalType = generalType;

      this.initContainers();
    }

    /**
     * @param {Object<string, Span>} spans
     * @param {Span} a
     * @param {Span} b
     */
    static compare(spans, a, b) {
      const aSpan = spans[a];
      const bSpan = spans[b];
      const tmp = aSpan.headFragment.from + aSpan.headFragment.to - bSpan.headFragment.from - bSpan.headFragment.to;
      if (tmp) {
        return tmp < 0 ? -1 : 1;
      }
      return 0;
    }

    initContainers() {
      this.incoming = [];
      this.outgoing = [];
      this.attributes = {};
      this.attributeText = [];
      this.attributeCues = {};
      this.attributeCueFor = {};
      this.attributeMerge = {};
      this.fragments = [];
      this.normalizations = [];
    }

    /**
     * @param {string} text
     */
    splitMultilineOffsets(text) {
      this.segmentedOffsetsMap = {};

      for (let fi = 0, nfi = 0; fi < this.unsegmentedOffsets.length; fi++) {
        /** @type {number | any} */ let begin = this.unsegmentedOffsets[fi][0];
        const end = this.unsegmentedOffsets[fi][1];

        for (let ti = begin; ti < end; ti++) {
          const c = text.charAt(ti);
          if (c === '\n' || c === '\r') {
            if (begin !== null) {
              this.offsets.push([begin, ti]);
              this.segmentedOffsetsMap[nfi++] = fi;
              begin = null;
            }
          } else if (begin === null) {
            begin = ti;
          }
        }

        if (begin !== null) {
          this.offsets.push([begin, end]);
          this.segmentedOffsetsMap[nfi++] = fi;
        }
      }
    }

    /**
     * Create a partial copy of the span with a new ID.
     *
     * @param {String} id
     * @returns the copy.
     */
    copy(id) {
      let span = $.extend(new Span(id, undefined, this.unsegmentedOffsets.slice(), undefined), this); // clone
      // read-only; shallow copy is fine
      span.offsets = this.offsets;
      span.segmentedOffsetsMap = this.segmentedOffsetsMap;
      return span;
    }

    buildFragments() {
      $.each(this.offsets, (offsetsNo, offsets) => {
        let from = parseInt(offsets[0], 10);
        let to = parseInt(offsets[1], 10);
        let fragment = new Fragment(offsetsNo, this, from, to);
        this.fragments.push(fragment);
      });

      // ensure ascending order
      this.fragments.sort(Fragment.midpointComparator);
      this.wholeFrom = this.fragments[0].from;
      this.wholeTo = this.fragments[this.fragments.length - 1].to;
      this.headFragment = this.fragments[true ? this.fragments.length - 1 : 0];
    }
  }

  class Comment {
    /** @type {string} */ text;
    /** @type {string} */ type;

    constructor() {
      Object.seal(this);
    }
  }

  class EventDesc {
    id;
    triggerId;
    roles = [];
    /** @type {boolean} */ equiv = false;
    /** @type {Arc} */ equivArc = undefined;
    relation = false;
    leftSpans = undefined;
    rightSpans = undefined;
    annotatorNotes = undefined;
    /** @type {Comment} */ comment = undefined;
    labelText = undefined;
    /** @type {string} */ color = undefined
    /** @type {string} */ shadowClass = undefined;

    constructor(id, triggerId, roles, klass) {
      Object.seal(this);

      this.id = id;
      this.triggerId = triggerId;
      roles.map(role => this.roles.push({type: role[0], targetId: role[1]}));

      this.equiv = klass === "equiv";
      this.relation = klass === "relation";
    }
  }

  class Chunk {
    index;
    text;
    from;
    to;
    space;
    /** @type {Fragment[]} */fragments = [];
    lastSpace;
    nextSpace;
    /** @type {number} */ sentence;
    /** @type {SVGElement} */ group;
    /** @type {SVGElement} */ highlightGroup;
    markedTextStart;
    markedTextEnd;
    right;
    /** @type {Row} */ row;
    textX;
    translation;
    firstFragmentIndex;
    lastFragmentIndex;
    rtlsizes;

    constructor(index, text, from, to, space) {
      Object.seal(this);

      this.index = index;
      this.text = text;
      this.from = from;
      this.to = to;
      this.space = space;
    }
  }

  class Arc {
    annotatorNotes = undefined;
    comment = undefined;
    /** @type {string} ID */ origin;
    /** @type {string} ID */ target;
    dist;
    type;
    /** @type {string} */ shadowClass;
    /** @type {number} */ jumpHeight = 0;
    /** @type {boolean} */ equiv = false;
    eventDescId;
    /** @type {boolean} */ relation = false;
    /** @type {[[string, string, string]]} */ normalizations = [];
    marked;
    /** @type {string} */ normalized;
    /** @type {boolean} */ hidden = false;

    /**
     *
     * @param {EventDesc} eventDesc
     * @param role
     * @param dist
     * @param eventNo
     */
    constructor(eventDesc, role, dist, eventNo) {
      Object.seal(this);

      this.origin = eventDesc.id;
      this.target = role.targetId;
      this.dist = dist;
      this.type = role.type;
      this.shadowClass = eventDesc.shadowClass;

      if (eventDesc.equiv) {
        this.equiv = true;
        this.eventDescId = eventNo;
        eventDesc.equivArc = this;
      } else if (eventDesc.relation) {
        this.relation = true;
        this.eventDescId = eventNo;
      }
    }
  }

  class Row {
    group;
    background;
    chunks = [];
    /** @type {boolean} */ hasAnnotations = false;
    /** @type {number} */ maxArcHeight = 0;
    /** @type {number} */ maxSpanHeight = 0;
    sentence;
    index;
    backgroundIndex;
    arcs;
    heightsStart;
    heightsEnd;
    heightsAdjust;
    textY;
    translation;

    constructor(svg) {
      this.group = svg.group({'class': 'row'});
      this.background = svg.group(this.group);
      Object.seal(this);
    }
  }

  class Measurements {
    /** @type {Object.<string, number>} */ widths;
    /** @type {number} */ height;
    /** @type {number} */ y;

    /**
     * @param {Object.<string, number>} widths
     * @param {number} height
     * @param {number} y
     */
    constructor(widths, height, y) {
      Object.seal(this);

      this.widths = widths;
      this.height = height;
      this.y = y;
    }
  }

  /**
   * Sets default values for a wide range of optional attributes.
   *
   * @param {SourceData} sourceData
   */
  function setSourceDataDefaults(sourceData) {
    // The following are empty lists if not set
    $.each([
      'attributes',
      'comments',
      'entities',
      'equivs',
      'events',
      'normalizations',
      'relations',
      'triggers',
    ], (attrNo, attr) => {
      if (sourceData[attr] === undefined) {
        sourceData[attr] = [];
      }
    });

    // Avoid exception due to undefined text in tokenise and sentenceSplit
    if (sourceData.text === undefined) {
      sourceData.text = "";
    }

    // If we lack sentence offsets we fall back on naive sentence splitting
    if (sourceData.sentence_offsets === undefined) {
      sourceData.sentence_offsets = sentenceSplit(sourceData.text);
    }

    // Similarily we fall back on whitespace tokenisation
    if (sourceData.token_offsets === undefined) {
      sourceData.token_offsets = tokenise(sourceData.text);
    }
  }

  // Set default values for a variety of collection attributes
  function setCollectionDefaults(collectionData) {
    // The following are empty lists if not set
    $.each([
      'entity_attribute_types',
      'entity_types',
      'event_attribute_types',
      'event_types',
      'relation_attribute_types',
      'relation_types',
      'unconfigured_types',
    ], (attrNo, attr) => {
      if (collectionData[attr] === undefined) {
        collectionData[attr] = [];
      }
    });
  }

  /**
   * Class currently defined only for documentation purposes
   */
  class AttributeType {
    /** @type {string} */ name;
    values = {};
    bool;
  }

  /**
   * Class currently defined only for documentation purposes
   */
  class PropertyDefinition {
    /** @type {boolean} */ symmetric;
  }

  /**
   * Class currently defined only for documentation purposes
   */
  class SpanType {
    /** @type {string} */ bgColor;
    /** @type {string} */ borderColor;
    /** @type {string} */ fgColor;
    arcs;
  }

  /**
   * Class currently defined only for documentation purposes
   */
  class RelationType {
    /** @type {Object.<string, PropertyDefinition>} */ properties;
    arrowHead;
    labelArrow;
    args;
  }

  class Visualizer {
    /** @type {Dispatcher} */ dispatcher;

    /** @type {boolean} */ rtlmode = false;
    /** @type {number} */ fontZoom = 100;

    /** @type {SVGWrapper} */ svg;
    /** @type {jQuery} */ $svg;
    /** @type {jQuery} */$svgDiv;
    highlightGroup;

    baseCanvasWidth = 0;
    canvasWidth = 0;

    /** @type {DocumentData} */ data = null;
    /** @type {SourceData} */ sourceData = null;
    requestedData = null;

    /**
     * @type {string}
     * @deprecated INCEpTION does not use the collection name.
     */
    coll = undefined;
    /**
     * @type {string}
     * @deprecated INCEpTION does not use the document name.
     */
    doc = undefined;

    /** @type {Object.<string, string[]>} */ args = null;

    /** @type {boolean} */ isRenderRequested = false;
    /** @type {boolean} */ drawing = false;
    /** @type {boolean} */ redraw = false;
    arcDragOrigin;

    /** @type {Object.<string, SpanType>} */ spanTypes = {};
    /** @type {Object.<string, RelationType>} */ relationTypesHash = {};
    /** @type {Object.<string, AttributeType>} */ entityAttributeTypes = {};
    /** @type {Object.<string, AttributeType>} */ eventAttributeTypes = {};
    /** @type {boolean} */ isCollectionLoaded = false;

    /** @type {[[number, number, string]]} */ markedText = [];
    highlight;
    highlightArcs;
    highlightSpans;
    commentId;

    // OPTIONS
    /** @type {number} */ forceWidth = undefined;
    /** @type {boolean} */ collapseArcs = false;
    /** @type {boolean} */ collapseArcSpace = false;
    /** @type {boolean} */ roundCoordinates = true; // try to have exact pixel offsets
    boxTextMargin = {x: 0, y: 1.5}; // effect is inverse of "margin" for some reason
    highlightRounding = {x: 3, y: 3}; // rx, ry for highlight boxes
    /** @type {Object.<string, number>} */ spaceWidths = {
      ' ': 4,
      '\u00a0': 4,
      '\u200b': 0,
      '\u3000': 8,
      '\t': 12,
      '\n': 4
    };
    /** @type {boolean} */ coloredCurlies = true; // color curlies by box BG
    /** @type {number} */ arcSlant = 15; //10;
    /** @type {number} */ minArcSlant = 8;
    /** @type {number} */ arcHorizontalSpacing = 10; // min space boxes with connecting arc
    /** @type {number} */ rowSpacing = -5; // for some funny reason approx. -10 gives "tight" packing.

    /** @type {number} */ sentNumMargin = 40;
    /** @type {boolean} */ smoothArcCurves = true; // whether to use curves (vs lines) in arcs
    /** @type {number} */ smoothArcSteepness = 0.5; // steepness of smooth curves (control point)
    /** @type {number} */ reverseArcControlx = 5; // control point distance for "UFO catchers"

    // "shadow" effect settings (note, error, incompelete)
    /** @type {number} */ rectShadowSize = 3;
    /** @type {number} */ rectShadowRounding = 2.5;
    /** @type {number} */ arcLabelShadowSize = 1;
    /** @type {number} */ arcLabelShadowRounding = 5;
    /** @type {number} */ shadowStroke = 2.5; // TODO XXX: this doesn't affect anything..?

    // "marked" effect settings (edited, focus, match)
    /** @type {number} */  markedSpanSize = 6;
    /** @type {number} */ markedArcSize = 2;
    /** @type {number} */ markedArcStroke = 7; // TODO XXX: this doesn't seem to do anything..?

    /** @type {number} */ rowPadding = 2;
    /** @type {number} */ nestingAdjustYStepSize = 2; // size of height adjust for nested/nesting spans
    /** @type {number} */ nestingAdjustXStepSize = 1; // size of height adjust for nested/nesting spans

    /** @type {string} */ shadowClassPattern = 'True_positive|False_positive|False_negative|AnnotationError|AnnotationWarning|AnnotatorNotes|Normalized|AnnotationIncomplete|AnnotationUnconfirmed|rectEditHighlight|EditHighlight_arc|MissingAnnotation|ChangedAnnotation';

    highlightSpanSequence;
    highlightArcSequence;
    highlightTextSequence;
    // different sequence for "mere" matches (as opposed to "focus" and "edited" highlights)
    highlightMatchSequence = '#FFFF00'; // plain yellow

    fragmentConnectorDashArray = '1,3,3,3';
    fragmentConnectorColor = '#000000';
    // END OPTIONS

    commentPrioLevels = [
      'Unconfirmed', 'Incomplete', 'Warning', 'Error', 'AnnotatorNotes',
      'AddedAnnotation', 'MissingAnnotation', 'ChangedAnnotation'
    ];

    renderErrors = {
      unableToReadTextFile: true,
      annotationFileNotFound: true,
      isDirectoryError: true
    };

    constructor(dispatcher, svgId) {
      Object.seal(this);

      this.dispatcher = dispatcher;
      this.$svgDiv = $('#' + svgId);

      if (!this.$svgDiv.length) {
        throw Error('Could not find container with id="' + svgId + '"');
      }

      // create the svg wrapper
      this.$svgDiv = $(this.$svgDiv).hide();
      this.$svgDiv.svg({
        /**
         * @param {SVGWrapper} _svg
         */
        onLoad: (_svg) => {
          this.svg = _svg;
          this.$svg = $(_svg.root());
          this.triggerRender();
        }
      });

      //var highlightSequence = '#FFFC69;#FFCC00;#FFFC69'; // a bit toned town
      const highlightSequence = '#FF9632;#FFCC00;#FF9632'; // yellow - deep orange
      this.highlightSpanSequence = highlightSequence;
      this.highlightArcSequence = highlightSequence;
      this.highlightTextSequence = highlightSequence;

      this.registerHandlers(this.$svgDiv, [
        'mouseover', 'mouseout', 'mousemove', 'mouseup', 'mousedown',
        'dragstart', 'dblclick', 'click',
        'contextmenu'
      ]);

      this.registerHandlers($(document), [
        'keydown', 'keypress',
        'touchstart', 'touchend'
      ]);

      this.registerHandlers($(window), [
        'resize'
      ]);

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
        .on('current', this, this.gotCurrent) //
        .on('clearSVG', this, this.clearSVG) //
        .on('mouseover', this, this.onMouseOver) //
        .on('mouseout', this, this.onMouseOut);
    }

    rowBBox(fragment) {
      const box = $.extend({}, fragment.rectBox); // clone
      const chunkTranslation = fragment.chunk.translation;
      box.x += chunkTranslation.x;
      box.y += chunkTranslation.y;
      return box;
    }

    /**
     * Get the priority of the given comment class.
     *
     * @param {String} commentClass
     * @returns a numerical value representing the priority.
     */
    commentPriority(commentClass) {
      if (commentClass === undefined) {
        return -1;
      }

      const len = this.commentPrioLevels.length;
      for (let i = 0; i < len; i++) {
        if (commentClass.indexOf(this.commentPrioLevels[i]) !== -1) {
          return i;
        }
      }

      return 0;
    }

    clearSVG() {
      this.data = null;
      this.sourceData = null;
      this.svg.clear();
      this.$svgDiv.hide();
    }

    setMarked(markedType) {
      if (!this.args[markedType]) {
        return;
      }

      this.args[markedType].map(marked => {
        if (marked[0] === 'sent') {
          data.markedSent[marked[1]] = true;
          return;
        }

        if (marked[0] === 'equiv') { // [equiv, Equiv, T1]
          $.each(this.sourceData.equivs, (equivNo, equiv) => {
            if (equiv[1] === marked[1]) {
              let len = equiv.length;
              for (let i = 2; i < len; i++) {
                if (equiv[i] === marked[2]) {
                  // found it
                  len -= 3;
                  for (let n = 1; n <= len; n++) {
                    let arc = this.data.eventDescs[equiv[0] + "*" + n].equivArc;
                    arc.marked = markedType;
                  }
                  return; // next equiv
                }
              }
            }
          });
          return;
        }

        if (marked.length === 2) {
          this.markedText.push([parseInt(marked[0], 10), parseInt(marked[1], 10), markedType]);
          return;
        }

        let span = this.data.spans[marked[0]];
        if (span) {
          if (marked.length === 3) { // arc
            $.each(span.outgoing, (arcNo, arc) => {
              if (arc.target === marked[2] && arc.type === marked[1]) {
                arc.marked = markedType;
              }
            });
          } else { // span
            span.marked = markedType;
          }

          return;
        }

        let eventDesc = this.data.eventDescs[marked[0]];
        if (eventDesc) { // relation
          $.each(this.data.spans[eventDesc.triggerId].outgoing, (arcNo, arc) => {
            if (arc.eventDescId === marked[0]) {
              arc.marked = markedType;
            }
          });
          return;
        }

        // try for trigger
        $.each(this.data.eventDescs, (eventDescNo, eventDesc) => {
          if (eventDesc.triggerId === marked[0]) {
            this.data.spans[eventDesc.id].marked = markedType;
          }
        });
      });
    }

    findArcHeight(fromIndex, toIndex, fragmentHeights) {
      let height = 0;
      for (let i = fromIndex; i <= toIndex; i++) {
        if (fragmentHeights[i] > height)
          height = fragmentHeights[i];
      }
      height += Configuration.visual.arcSpacing;
      return height;
    }

    adjustFragmentHeights(fromIndex, toIndex, fragmentHeights, height) {
      for (let i = fromIndex; i <= toIndex; i++) {
        if (fragmentHeights[i] < height)
          fragmentHeights[i] = height;
      }
    };

    applyHighlighting() {
      this.markedText = [];
      this.setMarked('edited'); // set by editing process
      this.setMarked('focus'); // set by URL
      this.setMarked('matchfocus'); // set by search process, focused match
      this.setMarked('match'); // set by search process, other (non-focused) match
    }

    /**
     * Calculate average arc distances. Average distance of arcs (0 for no arcs).
     *
     * @param {Span[]} spans
     */
    calculateAverageArcDistances(spans) {
      spans.map(span => span.avgDist = span.numArcs ? span.totalDist / span.numArcs : 0);
    }

    /**
     * Collect fragment texts into span texts
     *
     * @param {Span[]} spans
     */
    collectFragmentTextsIntoSpanTexts(spans) {
      spans.map(span => {
        let fragmentTexts = [];
        span.fragments && span.fragments.map(fragment => fragmentTexts.push(fragment.text));
        span.text = fragmentTexts.join('');
      });
    }

    /**
     * @param {Object.<string,Span>} spans
     * @returns list of span IDs in the order they should be drawn.
     */
    determineDrawOrder(spans) {
      let spanDrawOrderPermutation = Object.keys(spans);
      spanDrawOrderPermutation.sort((a, b) => {
        const spanA = spans[a];
        const spanB = spans[b];

        // We're jumping all over the chunks, but it's enough that we're doing everything inside
        // each chunk in the right order. Should it become necessary to actually do these in
        // linear order, put in a similar condition for spanX.headFragment.chunk.index; but it
        // should not be needed.
        const tmp = spanA.headFragment.drawOrder - spanB.headFragment.drawOrder;
        if (tmp) {
          return tmp < 0 ? -1 : 1;
        }

        return 0;
      });
      return spanDrawOrderPermutation;
    }

    organizeFragmentsIntoTowers(sortedFragments) {
      let lastFragment = null;
      let towerId = -1;

      sortedFragments.map(fragment => {
        if (!lastFragment || (lastFragment.from !== fragment.from || lastFragment.to !== fragment.to)) {
          towerId++;
        }
        fragment.towerId = towerId;
        lastFragment = fragment;
      });
    }

    applyMarkedTextToChunks(markedText, chunks) {
      let numChunks = chunks.length;
      // note the location of marked text with respect to chunks
      let startChunk = 0;
      let currentChunk;
      $.each(markedText, (textNo, textPos) => {
        let from = textPos[0];
        let to = textPos[1];
        let markedType = textPos[2];

        if (from < 0)
          from = 0;
        if (to < 0)
          to = 0;
        if (to >= this.data.text.length)
          to = this.data.text.length - 1;
        if (from > to)
          from = to;

        while (startChunk < numChunks) {
          let chunk = chunks[startChunk];
          if (from <= chunk.to) {
            chunk.markedTextStart.push([textNo, true, from - chunk.from, null, markedType]);
            break;
          }
          startChunk++;
        }

        if (startChunk === numChunks) {
          this.dispatcher.post('messages', [[['Wrong text offset', 'error']]]);
          return;
        }

        currentChunk = startChunk;
        while (currentChunk < numChunks) {
          let chunk = chunks[currentChunk];
          if (to <= chunk.to) {
            chunk.markedTextEnd.push([textNo, false, to - chunk.from]);
            break;
          }
          currentChunk++;
        }

        if (currentChunk === numChunks) {
          this.dispatcher.post('messages', [[['Wrong text offset', 'error']]]);
          let chunk = chunks[chunks.length - 1];
          chunk.markedTextEnd.push([textNo, false, chunk.text.length]);
        }
      }); // markedText
    }

    applyNormalizations(normalizations) {
      if (!normalizations) {
        return;
      }

      normalizations.map(norm => {
        const target = norm[0];
        const refdb = norm.length > 1 ? norm[1] : "#"; // See Renderer.QUERY_LAYER_LEVEL_DETAILS
        const refid = norm.length > 2 ? norm[2] : "";
        const reftext = norm.length > 3 ? norm[3] : null;

        const span = this.data.spans[target];
        if (span) {
          span.normalizations.push([refdb, refid, reftext]);
          span.normalized = 'Normalized';
          return;
        }

        const arc = this.data.arcById[target];
        if (arc) {
          arc.normalizations.push([refdb, refid, reftext]);
          arc.normalized = "Normalized";
          return;
        }

        this.dispatcher.post('messages', [[['Annotation ' + target + ' does not exist.', 'error']]]);
      });
    }

    /**
     *
     * @param {string} documentText
     * @param {[[string, string, [[number, number]], {}]]} entities
     * @return {Object.<string, Span>}
     */
    buildSpansFromEntities(documentText, entities) {
      if (!entities) {
        return {};
      }

      let spans = {};
      entities.map(entity => {
        const id = entity[0];
        const type = entity[1];
        const offsets = entity[2]; // offsets given as array of (start, end) pairs
        const span = new Span(id, type, offsets, 'entity');

        if (entity[3]) {
          const attributes = entity[3];
          if (attributes.hasOwnProperty('l')) {
            span.labelText = attributes.l;
          }
          if (attributes.hasOwnProperty('c')) {
            span.color = attributes.c;
          }
          if (attributes.hasOwnProperty('h')) {
            span.hovertext = attributes.h;
          }
          if (attributes.hasOwnProperty('a')) {
            span.actionButtons = !!(attributes.a);
          }
          if (attributes.hasOwnProperty('cl') && attributes.cl) {
            span.clippedAtStart = attributes.cl.startsWith("s");
            span.clippedAtEnd = attributes.cl.endsWith("e");
          }
        }

        span.splitMultilineOffsets(documentText);

        spans[id] = span;
      });

      return spans;
    }

    /**
     * @param {[][]}  triggers
     * @return {Object<string,any[]>} values are [triggerSpan, [event, event, event, ...]]
     */
    buildSpansFromTriggers(triggers) {
      if (!triggers) {
        return {};
      }

      const triggerHash = {};
      triggers.map(trigger => {
        //                          (id,         type,       offsets,    generalType)
        const triggerSpan = new Span(trigger[0], trigger[1], trigger[2], 'trigger');

        triggerSpan.splitMultilineOffsets(this.data.text);

        triggerHash[trigger[0]] = [triggerSpan, []]; // triggerSpan, eventlist
      });
      return triggerHash;
    }

    /**
     * @param {Object.<string,any[]>} triggerHash
     */
    buildEventDescsFromTriggers(triggerHash) {
      if (!triggerHash) {
        return;
      }

      this.sourceData.events.map(eventRow => {
        const id = eventRow[0];
        const triggerId = eventRow[1];
        const roles = eventRow[2]

        const eventDesc = this.data.eventDescs[id] = new EventDesc(id, triggerId, roles);
        const trigger = triggerHash[eventDesc.triggerId];
        const span = trigger[0].copy(eventDesc.id);
        trigger[1].push(span);
        this.data.spans[eventDesc.id] = span;
      });
    }

    /**
     * @param {Span[]} spans
     */
    splitSpansIntoFragments(spans) {
      if (!spans) {
        return;
      }

      spans.map(span => span.buildFragments());
    }

    /**
     *
     * @param equivs
     * @param {Object<string, Span>} spans
     * @param {Object.<string, EventDesc>} eventDescs
     */
    buildEventDescsFromEquivs(equivs, spans, eventDescs) {
      if (!equivs) {
        return;
      }

      $.each(equivs, (equivNo, equiv) => {
        // equiv: ['*', 'Equiv', spanId...]
        equiv[0] = "*" + equivNo;
        let equivSpans = equiv.slice(2);
        let okEquivSpans = [];

        // collect the equiv spans in an array
        equivSpans.map(equivSpan => {
          if (spans[equivSpan]) {
            okEquivSpans.push(equivSpan);
          }
          // TODO: #404, inform the user with a message?
        });

        // sort spans in the equiv by their midpoint
        okEquivSpans.sort((a, b) => Span.compare(spans, a, b));

        // generate the arcs
        const len = okEquivSpans.length;
        for (let i = 1; i < len; i++) {
          const id = okEquivSpans[i - 1];
          const tiggerId = okEquivSpans[i - 1];
          const roles = [[equiv[1], okEquivSpans[i]]];
          const eventDesc = eventDescs[equiv[0] + '*' + i] = new EventDesc(id, tiggerId, roles, 'equiv');
          eventDesc.leftSpans = okEquivSpans.slice(0, i);
          eventDesc.rightSpans = okEquivSpans.slice(i);
        }
      });
    }

    /**
     * @param relations
     * @param {Object.<string, EventDesc>} eventDescs
     */
    buildEventDescsFromRelations(relations, eventDescs) {
      if (!relations) {
        return;
      }

      relations.map(rel => {
        // rel[2] is args, rel[2][a][0] is role and rel[2][a][1] is value for a in (0,1)
        let argsDesc = this.relationTypesHash[rel[1]];
        argsDesc = argsDesc && argsDesc.args;

        let t1, t2;
        if (argsDesc) {
          // sort the arguments according to the config
          const args = {};
          args[rel[2][0][0]] = rel[2][0][1];
          args[rel[2][1][0]] = rel[2][1][1];
          t1 = args[argsDesc[0].role];
          t2 = args[argsDesc[1].role];
        } else {
          // (or leave as-is in its absence)
          t1 = rel[2][0][1];
          t2 = rel[2][1][1];
        }

        //                                (id, triggerId, roles,   klass)
        eventDescs[rel[0]] = new EventDesc(t1, t1, [[rel[1], t2]], 'relation');

        if (rel[3]) {
          eventDescs[rel[0]].labelText = rel[3];
        }

        if (rel[4]) {
          eventDescs[rel[0]].color = rel[4];
        }
      });
    }

    // INCEpTION does not use attributes...
    /**
     * @param {[string, string, string, string, string]} attributes id, name, spanId, value, cueSpanId
     * @param {Object.<string, Span>} spans
     */
    assignAttributesToSpans(attributes, spans) {
      if (!attributes) {
        return;
      }

      attributes.map(attr => {
        // attr: [id, name, spanId, value, cueSpanId]
        const id = attr[0];
        const name = attr[1];
        const spanId = attr[2];
        const value = attr[3];
        const cueSpanId = attr[4];

        // TODO: might wish to check what's appropriate for the type
        // instead of using the first attribute def found
        const attrType = (this.eventAttributeTypes[name] || this.entityAttributeTypes[name]);
        const attrValue = attrType && attrType.values[attrType.bool || value];
        const span = spans[spanId];

        if (!span) {
          this.dispatcher.post('messages', [[['Annotation ' + spanId + ', referenced from attribute ' + id + ', does not exist.', 'error']]]);
          return;
        }

        let valText = (attrValue && attrValue.name) || value;
        let attrText = attrType
          ? (attrType.bool ? attrType.name : (attrType.name + ': ' + valText))
          : (value ? name : name + ': ' + value);
        span.attributeText.push(attrText);
        span.attributes[name] = value;

        if (cueSpanId) { // cue
          span.attributeCues[name] = cueSpanId;
          let cueSpan = spans[cueSpanId];
          cueSpan.attributeCueFor[spans[1]] = spanId;
          cueSpan.cue = 'CUE'; // special css type
        }

        $.extend(span.attributeMerge, attrValue);
      });
    }

    /**
     * @param {[]} comments
     * @param triggerHash
     */
    assignComments(comments, triggerHash) {
      if (!comments) {
        return;
      }

      comments.map(comment => {
        // comment: [entityId, type, text]
        // TODO error handling
        // sentence id: ['sent', sentId]
        if (comment[0] instanceof Array && comment[0][0] === 'sent') {
          // sentence comment
          let sent = comment[0][1];
          let text = comment[2];
          if (this.data.sentComment[sent]) {
            text = this.data.sentComment[sent].text + '<br/>' + text;
          }
          this.data.sentComment[sent] = {type: comment[1], text: text};
          return;
        }

        let id = comment[0];
        let trigger = triggerHash[id];
        let commentEntities = trigger
          ? trigger[1] // trigger: [span, ...]
          : id in this.data.spans
            ? [this.data.spans[id]] // span: [span]
            : id in this.data.eventDescs
              ? [this.data.eventDescs[id]] // arc: [eventDesc]
              : [];

        commentEntities.map(entity => {
          // if duplicate comment for entity:
          // overwrite type, concatenate comment with a newline
          if (!entity.comment) {
            entity.comment = {type: comment[1], text: comment[2]};
          } else {
            entity.comment.type = comment[1];
            entity.comment.text += "\n" + comment[2];
          }

          // partially duplicate marking of annotator note comments
          if (comment[1] === "AnnotatorNotes") {
            entity.annotatorNotes = comment[2];
          }

          // prioritize type setting when multiple comments are present
          if (this.commentPriority(comment[1]) > this.commentPriority(entity.shadowClass)) {
            entity.shadowClass = comment[1];
          }
        });
      });
    }

    /**
     * @param {Object.<string, Span>} spans
     * @return {Fragment[]}
     */
    buildSortedFragments(spans) {
      if (!spans) {
        return [];
      }

      const sortedFragments = [];

      Object.values(spans).map(span => span.fragments.map(
        fragment => sortedFragments.push(fragment)));

      sortedFragments.sort(function (a, b) {
        let x = a.from;
        let y = b.from;
        if (x === y) {
          x = a.to;
          y = b.to;
        }
        return ((x < y) ? -1 : ((x > y) ? 1 : 0));
      });

      return sortedFragments;
    }

    /**
     * @param {number[][]} tokenOffsets
     * @param {Fragment[]} sortedFragments
     * @return {Chunk[]}
     */
    buildChunksFromTokenOffsets(tokenOffsets, sortedFragments) {
      if (!tokenOffsets) {
        return [];
      }

      let currentFragmentId = 0;
      let startFragmentId = 0;
      let numFragments = sortedFragments.length;
      let lastTo = 0;
      let firstFrom = null;
      let chunkNo = 0;
      let space;
      let chunk = null;
      let chunks = [];

      tokenOffsets.map(offset => {
        let from = offset[0];
        let to = offset[1];
        if (firstFrom === null) {
          firstFrom = from;
        }

        // Is the token end inside a span?
        if (startFragmentId && to > sortedFragments[startFragmentId - 1].to) {
          while (startFragmentId < numFragments && to > sortedFragments[startFragmentId].from) {
            startFragmentId++;
          }
        }
        currentFragmentId = startFragmentId;
        while (currentFragmentId < numFragments && to >= sortedFragments[currentFragmentId].to) {
          currentFragmentId++;
        }
        // if yes, the next token is in the same chunk
        if (currentFragmentId < numFragments && to > sortedFragments[currentFragmentId].from) {
          return;
        }

        // otherwise, create the chunk found so far
        space = this.data.text.substring(lastTo, firstFrom);
        let text = this.data.text.substring(firstFrom, to);
        if (chunk) {
          chunk.nextSpace = space;
        }
        //               (index,     text, from,      to, space) {
        chunk = new Chunk(chunkNo++, text, firstFrom, to, space);
        chunk.lastSpace = space;
        chunks.push(chunk);
        lastTo = to;
        firstFrom = null;
      });

      return chunks;
    }

    assignSentenceNumbersToChunks(firstSentence, sentenceOffsets, chunks) {
      if (!sentenceOffsets) {
        return;
      }

      let numChunks = chunks.length;
      let chunkNo = 0;
      let sentenceNo = firstSentence;

      sentenceOffsets.map(offset => {
        let from = offset[0];
        let to = offset[1];

        // Skip all chunks that belonged to the previous sentence
        let chunk;
        while (chunkNo < numChunks && (chunk = chunks[chunkNo]).from < from) {
          chunkNo++;
        }

        // No more chunks
        if (chunkNo >= numChunks) {
          return false;
        }

        // If the current chunk is not within the current sentence, then it was an empty sentence
        if (chunks[chunkNo].from >= to) {
          sentenceNo++;
          return;
        }

        sentenceNo++;
        chunk.sentence = sentenceNo;
        // console.trace("ASSIGN: line break ", sentenceNo ," at ", chunk);
        // increase chunkNo counter for next seek iteration
        chunkNo++;
      });
    }

    assignFragmentsToChunks(sortedFragments) {
      if (!sortedFragments) {
        return;
      }

      let currentChunkId = 0;
      let chunk;
      sortedFragments.map(fragment => {
        while (fragment.to > (chunk = this.data.chunks[currentChunkId]).to) {
          currentChunkId++;
        }
        chunk.fragments.push(fragment);
        fragment.text = chunk.text.substring(fragment.from - chunk.from, fragment.to - chunk.from);
        fragment.chunk = chunk;
      });
    }

    /**
     * Builds the args based on the EventDescs and links them up to the spans.
     *
     * Side effects:
     * - Fields on spans are changed: totalDist, numArcs, outgoing, incoming
     * - data.arcById index is populated.
     *
     * @param {Object.<string, EventDesc>} eventDescs;
     * @param {Object.<string, Span>} spans
     * @returns {Arc[]}
     */
    assignArcsToSpans(eventDescs, spans) {
      if (!eventDescs || !spans) {
        return [];
      }

      let arcs = [];

      Object.entries(eventDescs).map(([eventNo, eventDesc]) => {
        let origin = spans[eventDesc.id];

        if (!origin) {
          // TODO: include missing trigger ID in error message
          this.dispatcher.post('messages', [[['<strong>ERROR</strong><br/>Trigger for event "' + eventDesc.id + '" not found <br/>(please correct the source data)', 'error', 5]]]);
          return;
        }

        let here = origin.headFragment.from + origin.headFragment.to;
        eventDesc.roles.map(role => {
          let target = spans[role.targetId];
          if (!target) {
            this.dispatcher.post('messages', [[['<strong>ERROR</strong><br/>"' + role.targetId + '" (referenced from "' + eventDesc.id + '") not found <br/>(please correct the source data)', 'error', 5]]]);
            return;
          }

          let there = target.headFragment.from + target.headFragment.to;
          let dist = Math.abs(here - there);
          let arc = new Arc(eventDesc, role, dist, eventNo);

          origin.totalDist += dist;
          origin.numArcs++;
          origin.outgoing.push(arc);

          target.totalDist += dist;
          target.numArcs++;
          target.incoming.push(arc);

          arcs.push(arc);
          this.data.arcById[arc.eventDescId] = arc;
        }); // roles
      }); // eventDescs

      return arcs;
    }

    /**
     * Populates the "data" field based on the "sourceData" JSON that we recieved from the server.
     *
     * @param {SourceData} sourceData
     */
    setData(sourceData) {
      this.sourceData = sourceData;
      this.rtlmode = this.sourceData.rtl_mode;
      this.fontZoom = this.sourceData.font_zoom;
      this.args = this.sourceData.args ?? {};

      this.dispatcher.post('newSourceData', [this.sourceData]);

      this.data = new DocumentData(this.sourceData.text);

      // collect annotation data
      this.data.spans = this.buildSpansFromEntities(this.data.text, this.sourceData.entities);

      // REC 2021-06-29: Not sure if we need this at all since INCEpTION only uses "entities" and
      // doesn't do brat-style events/trigger. We prepare spans-with-slots on the server side
      // already and render them as brat-entities and brat-relations
      const triggerHash = this.buildSpansFromTriggers(this.sourceData.triggers);
      this.buildEventDescsFromTriggers(triggerHash);

      // split spans into span fragments (for discontinuous spans)
      this.splitSpansIntoFragments(Object.values(this.data.spans));
      this.buildEventDescsFromEquivs(this.sourceData.equivs, this.data.spans, this.data.eventDescs);
      this.buildEventDescsFromRelations(this.sourceData.relations, this.data.eventDescs);
      this.assignAttributesToSpans(this.sourceData.attributes, this.data.spans);
      this.assignComments(this.sourceData.comments, triggerHash);

      // prepare span boundaries for token containment testing
      // sort fragments by beginning, then by end
      const sortedFragments = this.buildSortedFragments(this.data.spans);

      // token containment testing (chunk recognition)
      this.data.chunks = this.buildChunksFromTokenOffsets(this.sourceData.token_offsets, sortedFragments);

      this.assignSentenceNumbersToChunks(this.sourceData.sentence_number_offset - 1,
        this.sourceData.sentence_offsets, this.data.chunks);
      this.assignFragmentsToChunks(sortedFragments);
      this.data.arcs = this.assignArcsToSpans(this.data.eventDescs, this.data.spans);
      this.applyNormalizations(this.sourceData.normalizations);
      this.applyHighlighting();

      if (this.data.spans) {
        this.calculateAverageArcDistances(Object.values(this.data.spans));
        this.collectFragmentTextsIntoSpanTexts(Object.values(this.data.spans));
      }

      for (let i = 0; i < 2; i++) {
        // preliminary sort to assign heights for basic cases
        // (first round) and cases resolved in the previous round(s).
        this.data.chunks.map(chunk => {
          // sort and then re-number
          chunk.fragments.sort(Fragment.compare);
          chunk.fragments.map((fragment, fragmentNo) => fragment.indexNumber = fragmentNo);
        });

        // nix the sums, so we can sum again
        Object.values(this.data.spans).map(span => span.refedIndexSum = 0);

        // resolved cases will now have indexNumber set to indicate their relative order. Sum those
        // for referencing cases for use in iterative resorting
        this.data.arcs.map(arc => {
          this.data.spans[arc.origin].refedIndexSum += this.data.spans[arc.target].headFragment.indexNumber;
        });
      }

      // Final sort of fragments in chunks for drawing purposes
      // Also identify the marked text boundaries regarding chunks
      this.data.chunks.map(chunk => {
        // and make the next sort take this into account. Note that this will
        // now resolve first-order dependencies between sort orders but not
        // second-order or higher.
        chunk.fragments.sort(Fragment.compare);
        chunk.fragments.map((fragment, fragmentNo) => fragment.drawOrder = fragmentNo);
      });

      this.data.spanDrawOrderPermutation = this.determineDrawOrder(this.data.spans);

      // resort the fragments for linear order by center so we can organize them into towers
      sortedFragments.sort(Fragment.midpointComparator);
      // sort fragments into towers, calculate average arc distances
      this.organizeFragmentsIntoTowers(sortedFragments);

      // find curlies (only the first fragment drawn in a tower)
      this.data.spanDrawOrderPermutation.map(spanId => {
        const span = this.data.spans[spanId];

        span.fragments.map(fragment => {
          if (!this.data.towers[fragment.towerId]) {
            this.data.towers[fragment.towerId] = [];
            fragment.drawCurly = true;
            fragment.span.drawCurly = true;
          }
          this.data.towers[fragment.towerId].push(fragment);
        });
      });

      const spanAnnTexts = {};
      this.data.chunks.map(chunk => {
        chunk.markedTextStart = [];
        chunk.markedTextEnd = [];

        $.each(chunk.fragments, (fragmentNo, fragment) => {
          if (chunk.firstFragmentIndex === undefined) {
            chunk.firstFragmentIndex = fragment.towerId;
          }
          chunk.lastFragmentIndex = fragment.towerId;

          const spanLabels = Util.getSpanLabels(this.spanTypes, fragment.span.type);
          fragment.labelText = Util.spanDisplayForm(this.spanTypes, fragment.span.type);
          // Find the most appropriate label according to text width
          if (Configuration.abbrevsOn && spanLabels) {
            let labelIdx = 1; // first abbrev
            const maxLength = (fragment.to - fragment.from) / 0.8;
            while (fragment.labelText.length > maxLength &&
            spanLabels[labelIdx]) {
              fragment.labelText = spanLabels[labelIdx];
              labelIdx++;
            }
          }

          fragment.labelText = "(" + fragment.labelText + ")";
          if (fragment.span.labelText) {
            fragment.labelText = fragment.span.labelText;
          }
          const svgtext = this.svg.createText(); // one "text" element per row
          const postfixArray = [];
          let prefix = '';
          let postfix = '';
          let warning = false;
          $.each(fragment.span.attributes, (attrType, valType) => {
            // TODO: might wish to check what's appropriate for the type
            // instead of using the first attribute def found
            const attr = (this.eventAttributeTypes[attrType] || this.entityAttributeTypes[attrType]);
            if (!attr) {
              // non-existent type
              warning = true;
              return;
            }
            const val = attr.values[attr.bool || valType];
            if (!val) {
              // non-existent value
              warning = true;
              return;
            }
            if ($.isEmptyObject(val)) {
              // defined, but lacks any visual presentation
              warning = true;
              return;
            }
            if (val.glyph) {
              if (val.position === "left") {
                prefix = val.glyph + prefix;
                const tspan_attrs = {'class': 'glyph'};
                if (val.glyphColor) {
                  tspan_attrs.fill = val.glyphColor;
                }
                svgtext.span(val.glyph, tspan_attrs);
              } else { // XXX right is implied - maybe change
                postfixArray.push([attr, val]);
                postfix += val.glyph;
              }
            }
          });
          let text = fragment.labelText;
          if (prefix !== '') {
            text = prefix + ' ' + text;
            svgtext.string(' ');
          }
          svgtext.string(fragment.labelText);
          if (postfixArray.length) {
            text += ' ' + postfix;
            svgtext.string(' ');
            $.each(postfixArray, function (elNo, el) {
              const tspan_attrs = {'class': 'glyph'};
              if (el[1].glyphColor) {
                tspan_attrs.fill = el[1].glyphColor;
              }
              svgtext.span(el[1].glyph, tspan_attrs);
            });
          }
          if (warning) {
            svgtext.span("#", {'class': 'glyph attribute_warning'});
            text += ' #';
          }
          fragment.glyphedLabelText = text;

          if (!spanAnnTexts[text]) {
            spanAnnTexts[text] = true;
            this.data.spanAnnTexts[text] = svgtext;
          }
        }); // chunk.fragments
      }); // chunks

      // sort by "from"; we don't need to sort by "to" as well,
      // because unlike spans, chunks are disjunct
      this.markedText.sort((a, b) => Util.cmp(a[0], b[0]));
      this.applyMarkedTextToChunks(this.markedText, this.data.chunks);

      this.dispatcher.post('dataReady', [this.data]);
    }

    resetData() {
      this.setData(this.sourceData);
      this.renderData(undefined);
    }

    translate(element, x, y) {
      $(element.group).attr('transform', 'translate(' + x + ', ' + y + ')');
      element.translation = {x: x, y: y};
    }

    /**
     * @return {SVGElement} The definitions node.
     */
    addHeaderAndDefs() {
      const defs = this.svg.defs();
      const $blurFilter = $($.parseXML(('<filter id="Gaussian_Blur"><feGaussianBlur in="SourceGraphic" stdDeviation="2" /></filter>')));
      this.svg.add(defs, $blurFilter.children(0));
      return defs;
    }

    /**
     * @param textsHash
     * @param options arguments to the {@link SVGWrapper.group}} call creating the temporary group used for measurement
     * @param callback
     * @return {Measurements}
     */
    getTextMeasurements(textsHash, options, callback) {
      // make some text elements, find out the dimensions
      // noinspection JSValidateTypes
      /** @type {SVGGElement} */ const textMeasureGroup = this.svg.group(options);

      // changed from $.each because of #264 ('length' can appear)
      for (let text in textsHash) {
        if (textsHash.hasOwnProperty(text)) {
          this.svg.text(textMeasureGroup, 0, 0, text);
        }
      }

      // measuring goes on here
      /** @type {Object.<string, number>} */ const widths = {};
      $(textMeasureGroup).find('text').each(function (svgTextNo, svgText) {
        const text = $(svgText).text();
        widths[text] = this.getComputedTextLength();

        if (callback) {
          $.each(textsHash[text], function (text, object) {
            callback(object, svgText);
          });
        }
      });
      const bbox = textMeasureGroup.getBBox();
      this.svg.remove(textMeasureGroup);

      return new Measurements(widths, bbox.height, bbox.y);
    }

    /**
     * @return {Sizes}
     */
    calculateTextMeasurements() {
      const textSizes = this.calculateChunkTextMeasures();
      const fragmentSizes = this.calculateFragmentTextMeasures();
      const arcSizes = this.calculateArcTextMeasurements();

      return new Sizes(textSizes, arcSizes, fragmentSizes);
    }

    calculateChunkTextMeasures() {
      // get the span text sizes
      /** @type {Object.<string, []>} */ const chunkTexts = {}; // set of span texts
      this.data.chunks.map(chunk => {
        chunk.row = undefined; // reset
        if (!chunkTexts.hasOwnProperty(chunk.text)) {
          chunkTexts[chunk.text] = [];
        }

        // here we also need all the spans that are contained in
        // chunks with this text, because we need to know the position
        // of the span text within the respective chunk text
        const chunkText = chunkTexts[chunk.text];
        chunkText.push.apply(chunkText, chunk.fragments);
        // and also the markedText boundaries
        chunkText.push.apply(chunkText, chunk.markedTextStart);
        chunkText.push.apply(chunkText, chunk.markedTextEnd);
      });

      return this.getTextMeasurements(chunkTexts, undefined, (fragment, text) => {
        if (!(fragment instanceof Fragment)) {
          // it's markedText [id, start?, char#, offset]
          if (fragment[2] < 0)
            fragment[2] = 0;
          if (!fragment[2]) { // start
            fragment[3] = text.getStartPositionOfChar(fragment[2]).x;
          } else {
            fragment[3] = text.getEndPositionOfChar(fragment[2] - 1).x + 1;
          }
          return;
        }

        // measure the fragment text position in pixels
        let firstChar = fragment.from - fragment.chunk.from;
        if (firstChar < 0) {
          firstChar = 0;
          this.dispatcher.post('messages', [[['<strong>WARNING</strong>' +
          '<br/> ' +
          'The fragment [' + fragment.from + ', ' + fragment.to + '] (' + fragment.text + ') is not ' +
          'contained in its designated chunk [' +
          fragment.chunk.from + ', ' + fragment.chunk.to + '] most likely ' +
          'due to the fragment starting or ending with a space, please ' +
          'verify the sanity of your data since we are unable to ' +
          'visualise this fragment correctly and will drop leading ' +
          'space characters',
            'warning', 15]]]);
        }
        let lastChar = fragment.to - fragment.chunk.from - 1;

        // Adjust for XML whitespace (#832, #1009)
        const textUpToFirstChar = fragment.chunk.text.substring(0, firstChar);
        const textUpToLastChar = fragment.chunk.text.substring(0, lastChar);
        const textUpToFirstCharUnspaced = textUpToFirstChar.replace(/\s\s+/g, ' ');
        const textUpToLastCharUnspaced = textUpToLastChar.replace(/\s\s+/g, ' ');
        firstChar -= textUpToFirstChar.length - textUpToFirstCharUnspaced.length;
        lastChar -= textUpToLastChar.length - textUpToLastCharUnspaced.length;

        let /** @type {number} */ startPos, endPos;
        if (this.rtlmode) {
          // This rendering is much slower than the "old" version that brat uses, but it is more reliable in RTL mode.
          [startPos, endPos] = this.calculateSubstringWidthRobust(fragment, text, firstChar, lastChar);
          // In RTL mode, positions are negative (left to right)
          startPos = -startPos;
          endPos = -endPos;
        } else {
          // Using the old faster method in LTR mode. YES, this means that subtoken
          // annotations of RTL tokens in LTR mode will render incorrectly. If somebody
          // needs that, we should do a smarter selection of the rendering mode.
          // This is the old measurement code which doesn't work properly because browsers
          // treat the x coordinate very differently. Our width-based measurement is more
          // reliable.
          // Cannot use fragment.chunk.text.length here because invisible
          // characters do not count. Using text.getNumberOfChars() instead.
          [startPos, endPos] = this.calculateSubstringWidthFast(text, firstChar, lastChar);
        }

        // Make sure that startPos and endPos are properly ordered on the X axis
        fragment.curly = {
          from: Math.min(startPos, endPos),
          to: Math.max(startPos, endPos)
        };
      });
    }

    calculateSubstringWidthRobust(fragment, text, firstChar, lastChar) {
      let charDirection;
      let charAttrs;
      let corrFactor = 1;

      if (fragment.chunk.rtlsizes) {
        // Use cached metrics
        charDirection = fragment.chunk.rtlsizes.charDirection;
        charAttrs = fragment.chunk.rtlsizes.charAttrs;
        corrFactor = fragment.chunk.rtlsizes.corrFactor;
      } else {
        // Calculate metrics
        charDirection = [];
        charAttrs = [];

        // Cannot use fragment.chunk.text.length here because invisible
        // characters do not count. Using text.getNumberOfChars() instead.
        //var step1Start = new Date();
        for (let idx = 0; idx < text.getNumberOfChars(); idx++) {
          const cw = text.getEndPositionOfChar(idx).x - text.getStartPositionOfChar(idx).x;
          const dir = isRTL(text.textContent.charCodeAt(idx)) ? "rtl" : "ltr";
          charAttrs.push({
            order: idx,
            width: Math.abs(cw),
            direction: dir
          });
          charDirection.push(dir);
          //		            	  console.log("char " +  idx + " [" + text.textContent[idx] + "] " +
          //		            	  		"begin:" + text.getStartPositionOfChar(idx).x +
          //		            	  		" end:" + text.getEndPositionOfChar(idx).x +
          //		            	  		" width:" + Math.abs(cw) +
          //		            	  		" dir:" + charDirection[charDirection.length-1]);
        }
        //console.log("Collected widths in " + (new Date() - step1Start));
        // Re-order widths if necessary
        //var step2Start = new Date();
        if (charAttrs.length > 1) {
          let idx = 0;
          let blockBegin = idx;
          let blockEnd = idx;

          // Figure out next block
          while (blockEnd < charAttrs.length) {
            while (charDirection[blockBegin] === charDirection[blockEnd]) {
              blockEnd++;
            }

            if (charDirection[blockBegin] === (this.rtlmode ? "ltr" : "rtl")) {
              charAttrs = charAttrs.slice(0, blockBegin)
                .concat(charAttrs.slice(blockBegin, blockEnd).reverse())
                .concat(charAttrs.slice(blockEnd));
            }

            blockBegin = blockEnd;
          }
        }
        //	          console.log("order: " + charOrder);
        //console.log("Established character order in " + (new Date() - step2Start));
        //var step3Start = new Date();
        // The actual character width on screen is not necessarily the width that can be
        // obtained by subtracting start from end position. In particular Arabic connects
        // characters quite a bit such that the width on screen may be less. Here we
        // try to compensate for this using a correction factor.
        let widthsSum = 0;
        for (let idx = 0; idx < charAttrs.length; idx++) {
          widthsSum += charAttrs[idx].width;
        }
        corrFactor = text.getComputedTextLength() / widthsSum;
        //console.log("Final calculations in " + (new Date() - step3Start));
        //	          	  console.log("width sums: " + widthsSum);
        //	          	  console.log("computed length: " + text.getComputedTextLength());
        //	          	  console.log("corrFactor: " + corrFactor);
        fragment.chunk.rtlsizes = {
          charDirection: charDirection,
          charAttrs: charAttrs,
          corrFactor: corrFactor
        };

        //console.log("Completed calculating static RTL metrics in " + (new Date() -
        //		  start) + " for " + text.getNumberOfChars() + " characters.");
      }

      // startPos = Math.min(0, Math.min(text.getStartPositionOfChar(charOrder[0]).x, text.getEndPositionOfChar(charOrder[0]).x));
      let startPos = 0;
      // console.log("startPos[initial]: " + startPos);
      for (let i = 0; charAttrs[i].order !== firstChar && i < charAttrs.length; i++) {
        startPos += charAttrs[i].width;
        // console.log("startPos["+i+"]  "+text.textContent[charOrder[i]]+" width "+charWidths[i]+" : " + startPos);
      }
      /* REC: Not sure if this served a purpose or ever worked...
      if (charDirection[i] === (this.rtlmode ? "ltr" : "rtl")) {
        startPos += charAttrs[i].width;
        // console.log("startPos["+i+"]  "+text.textContent[charOrder[i]]+" width "+charWidths[i]+" : " + startPos);
      }
      */
      startPos = startPos * corrFactor;
      // console.log("startPos: " + startPos);
      // endPos = Math.min(0, Math.min(text.getStartPositionOfChar(charOrder[0]).x, text.getEndPositionOfChar(charOrder[0]).x));
      let endPos = 0;
      //	           	  console.log("endPos[initial]: " + endPos);
      for (let i = 0; charAttrs[i].order !== lastChar && i < charAttrs.length; i++) {
        endPos += charAttrs[i].width;
        //	            	  console.log("endPos["+i+"]  "+text.textContent[charOrder[i]]+" width "+charWidths[i]+" : " + endPos);
      }
      if (charDirection[i] === (this.rtlmode ? "rtl" : "ltr")) {
        //	            	  console.log("endPos["+i+"]  "+text.textContent[charOrder[i]]+" width "+charWidths[i]+" : " + endPos);
        endPos += charAttrs[i].width;
      }
      endPos = endPos * corrFactor;
      //	        	  console.log("endPos: " + endPos);
      return [startPos, endPos];
    }

    calculateSubstringWidthFast(text, firstChar, lastChar) {
      let startPos, endPos;
      if (firstChar < text.getNumberOfChars()) {
        startPos = text.getStartPositionOfChar(firstChar).x;
      } else {
        startPos = text.getComputedTextLength();
      }
      endPos = (lastChar < firstChar)
        ? startPos
        : text.getEndPositionOfChar(lastChar).x;
      return [startPos, endPos];
    }

    /**
     * Get the fragment annotation text sizes.
     *
     * @return {Measurements}
     */
    calculateFragmentTextMeasures() {
      const fragmentTexts = {};

      let noSpans = true;
      if (this.data.spans) {
        Object.values(this.data.spans).map(span => {
          if (span.fragments) {
            span.fragments.map(fragment => {
              fragmentTexts[fragment.glyphedLabelText] = true;
              noSpans = false;
            });
          }
        });
      }

      if (noSpans) {
        fragmentTexts.$ = true; // dummy so we can at least get the height
      }

      return this.getTextMeasurements(fragmentTexts, {'class': 'span'});
    }

    /**
     * Get the arc annotation text sizes (for all labels).
     *
     * @return {Measurements}
     */
    calculateArcTextMeasurements() {
      const arcs = this.data.arcs;
      const spans = this.data.spans;
      const eventDescs = this.data.eventDescs;

      const arcTexts = {};
      if (arcs) {
        arcs.map(arc => {
          let labels = Util.getArcLabels(this.spanTypes, spans[arc.origin].type, arc.type, this.relationTypesHash);
          if (!labels.length) {
            labels = [arc.type];
          }

          if (arc.eventDescId && eventDescs[arc.eventDescId] && eventDescs[arc.eventDescId].labelText) {
            labels = [eventDescs[arc.eventDescId].labelText];
          }

          labels.map(label => arcTexts[label] = true);
        });
      }

      return this.getTextMeasurements(arcTexts, {'class': 'arcs'});
    }

    /**
     * Adjust all fragments in a tower so they have the same width.
     */
    adjustTowerAnnotationSizes() {
      const fragmentWidths = this.data.sizes.fragments.widths;
      Object.values(this.data.towers).map(tower => {
        let maxWidth = 0;
        tower.map(fragment => maxWidth = Math.max(maxWidth, fragmentWidths[fragment.glyphedLabelText]));
        tower.map(fragment => fragment.width = maxWidth);
      });
    }

    /**
     * @param {SVGElement} defs
     * @param spec
     * @return {string|undefined}
     */
    makeArrow(defs, spec) {
      const parsedSpec = spec.split(',');
      const type = parsedSpec[0];
      if (type === 'none') {
        return;
      }

      let width = 5;
      let height = 5;
      let color = "black";
      if ($.isNumeric(parsedSpec[1]) && parsedSpec[2]) {
        if ($.isNumeric(parsedSpec[2]) && parsedSpec[3]) {
          // 3 args, 2 numeric: assume width, height, color
          width = parsedSpec[1];
          height = parsedSpec[2];
          color = parsedSpec[3] || 'black';
        } else {
          // 2 args, 1 numeric: assume width/height, color
          width = height = parsedSpec[1];
          color = parsedSpec[2] || 'black';
        }
      } else {
        // other: assume color only
        color = parsedSpec[1] || 'black';
      }

      // hash needs to be replaced as IDs don't permit it.
      const arrowId = 'arrow_' + spec.replace(/#/g, '').replace(/,/g, '_');

      let arrow;
      if (type === 'triangle') {
        arrow = this.svg.marker(defs, arrowId,
          width, height / 2, width, height, 'auto',
          {
            markerUnits: 'strokeWidth',
            'fill': color,
          });
        this.svg.polyline(arrow, [[0, 0], [width, height / 2], [0, height], [width / 12, height / 2]]);
      }
      return arrowId;
    }

    /**
     * @return {number}
     */
    calculateMaxTextWidth(sizes) {
      let maxTextWidth = 0;
      for (let text in sizes.texts.widths) {
        if (sizes.texts.widths.hasOwnProperty(text)) {
          maxTextWidth = Math.max(maxTextWidth, sizes.texts.widths[text]);
        }
      }
      return maxTextWidth;
    }

    renderLayoutFloorsAndCurlies(spanDrawOrderPermutation) {
      // reserve places for spans
      const floors = [];
      const reservations = []; // reservations[chunk][floor] = [[from, to, headroom]...]
      const inf = 1.0 / 0.0;

      $.each(spanDrawOrderPermutation, (spanIdNo, spanId) => {
        const span = this.data.spans[spanId];
        const spanDesc = this.spanTypes[span.type];
        const bgColor = ((spanDesc && spanDesc.bgColor) || '#ffffff');

        if (bgColor === "hidden") {
          span.hidden = true;
          return;
        }

        const f1 = span.fragments[0];
        const f2 = span.fragments[span.fragments.length - 1];

        const x1 = (f1.curly.from + f1.curly.to - f1.width) / 2 -
          Configuration.visual.margin.x - (this.data.sizes.fragments.height / 2);
        const i1 = f1.chunk.index;

        const x2 = (f2.curly.from + f2.curly.to + f2.width) / 2 +
          Configuration.visual.margin.x + (this.data.sizes.fragments.height / 2);
        const i2 = f2.chunk.index;

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
        let carpet = 0;
        let outside = true;
        const thisCurlyHeight = span.drawCurly ? Configuration.visual.curlyHeight : 0;
        const height = this.data.sizes.fragments.height + thisCurlyHeight + Configuration.visual.boxSpacing +
          2 * Configuration.visual.margin.y - 3;
        $.each(floors, (floorNo, floor) => {
          let floorAvailable = true;
          for (let i = i1; i <= i2; i++) {
            if (!(reservations[i] && reservations[i][floor])) {
              continue;
            }
            const from = (i === i1) ? x1 : -inf;
            const to = (i === i2) ? x2 : inf;
            $.each(reservations[i][floor], (resNo, res) => {
              if (res[0] < to && from < res[1]) {
                floorAvailable = false;
                return false;
              }
            });
          }
          if (floorAvailable) {
            if (carpet === null) {
              carpet = floor;
            } else if (height + carpet <= floor) {
              // found our floor!
              outside = false;
              return false;
            }
          } else {
            carpet = null;
          }
        });

        const reslen = reservations.length;
        const makeNewFloorIfNeeded = function (floor) {
          let floorNo = $.inArray(floor, floors);
          if (floorNo === -1) {
            floors.push(floor);
            floors.sort(Util.cmp);
            floorNo = $.inArray(floor, floors);
            if (floorNo !== 0) {
              // copy reservations from the floor below
              const parquet = floors[floorNo - 1];
              for (let i = 0; i <= reslen; i++) {
                if (reservations[i]) {
                  if (!reservations[i][parquet]) {
                    reservations[i][parquet] = [];
                  }
                  const footroom = floor - parquet;
                  $.each(reservations[i][parquet], function (resNo, res) {
                    if (res[2] > footroom) {
                      if (!reservations[i][floor]) {
                        reservations[i][floor] = [];
                      }
                      reservations[i][floor].push([res[0], res[1], res[2] - footroom]);
                    }
                  });
                }
              }
            }
          }
          return floorNo;
        };
        const ceiling = carpet + height;
        const ceilingNo = makeNewFloorIfNeeded(ceiling);
        const carpetNo = makeNewFloorIfNeeded(carpet);
        // make the reservation
        let floor, floorNo;
        for (floorNo = carpetNo; (floor = floors[floorNo]) !== undefined && floor < ceiling; floorNo++) {
          const headroom = ceiling - floor;
          for (let i = i1; i <= i2; i++) {
            const from = (i === i1) ? x1 : 0;
            const to = (i === i2) ? x2 : inf;
            if (!reservations[i])
              reservations[i] = {};
            if (!reservations[i][floor])
              reservations[i][floor] = [];
            reservations[i][floor].push([from, to, headroom]); // XXX maybe add fragment; probably unnecessary
          }
        }
        span.floor = carpet + thisCurlyHeight;
      });
    }

    /**
     * @param {SourceData} sourceData
     * @param {Chunk[]} chunks
     * @param {number} maxTextWidth
     */
    renderChunks(sourceData, chunks, maxTextWidth) {
      /** @type {number} */ let currentX;
      if (this.rtlmode) {
        currentX = this.canvasWidth - (Configuration.visual.margin.x + this.sentNumMargin + this.rowPadding);
      } else {
        currentX = Configuration.visual.margin.x + this.sentNumMargin + this.rowPadding;
      }

      /** @type {Row[]} */ const rows = [];
      /** @type {number[]} */ const fragmentHeights = [];
      const openTextHighlights = {};

      let sentenceToggle = 0;
      let sentenceNumber = sourceData.sentence_number_offset;

      let row = new Row(this.svg);
      row.sentence = sentenceNumber;
      row.backgroundIndex = sentenceToggle;
      row.index = 0;

      const textMarkedRows = [];
      let rowIndex = 0;

      chunks.map(chunk => {
        let spaceWidth = 0;
        if (chunk.lastSpace) {
          const spaceLen = chunk.lastSpace.length || 0;
          let spacePos;
          if (chunk.sentence) {
            // If this is line-initial spacing, fetch the sentence to which the chunk belongs
            // so we can determine where it begins
            const sentFrom = sourceData.sentence_offsets[chunk.sentence - sourceData.sentence_number_offset][0];
            spacePos = spaceLen - (chunk.from - sentFrom);
          } else {
            spacePos = 0;
          }
          for (let i = spacePos; i < spaceLen; i++) {
            spaceWidth += this.spaceWidths[chunk.lastSpace[i]] * (this.fontZoom / 100.0) || 0;
          }
          currentX += this.rtlmode ? -spaceWidth : spaceWidth;
        }

        chunk.group = this.svg.group(row.group);
        chunk.highlightGroup = this.svg.group(chunk.group);

        let y = 0;
        let hasLeftArcs, hasRightArcs, hasInternalArcs;
        let hasAnnotations;
        let chunkFrom = Infinity;
        let chunkTo = 0;
        let chunkHeight = 0;
        let spacing = 0;
        let spacingChunkId = null;
        let spacingRowBreak = 0;

        chunk.fragments.map(fragment => {
          const span = fragment.span;

          if (span.hidden) {
            return;
          }

          const spanDesc = this.spanTypes[span.type];
          let bgColor = ((spanDesc && spanDesc.bgColor) || '#ffffff');
          let fgColor = ((spanDesc && spanDesc.fgColor) ||  '#000000');
          let borderColor = ((spanDesc && spanDesc.borderColor) || '#000000');

          if (span.color) {
            bgColor = span.color;
            fgColor = bgToFgColor(bgColor);
            borderColor = 'darken';
          }

          // special case: if the border 'color' value is 'darken',
          // then just darken the BG color a bit for the border.
          if (borderColor === 'darken') {
            borderColor = Util.adjustColorLightness(bgColor, -0.6);
          }

          fragment.group = this.svg.group(chunk.group, { 'class': 'span' });

          if (!y) {
            y = -this.data.sizes.texts.height;
          }
          // x : center of fragment on x axis
          let x = (fragment.curly.from + fragment.curly.to) / 2;

          // XXX is it maybe sizes.texts?
          let yy = y + this.data.sizes.fragments.y;
          // hh : fragment height
          let hh = this.data.sizes.fragments.height;
          // ww : fragment width
          let ww = fragment.width;
          // xx : left edge of fragment
          let xx = x - ww / 2;

          // text margin fine-tuning
          yy += this.boxTextMargin.y;
          hh -= 2 * this.boxTextMargin.y;
          xx += this.boxTextMargin.x;
          ww -= 2 * this.boxTextMargin.x;
          let rectClass = 'span_' + (span.cue || span.type) + ' span_default'; // TODO XXX first part unneeded I think; remove

          // attach e.g. "False_positive" into the type
          if (span.comment && span.comment.type) {
            rectClass += ' ' + span.comment.type;
          }

          // inner coordinates of fragment (excluding margins)
          let bx = xx - Configuration.visual.margin.x - this.boxTextMargin.x;
          const by = yy - Configuration.visual.margin.y;
          const bw = ww + 2 * Configuration.visual.margin.x;
          const bh = hh + 2 * Configuration.visual.margin.y;

          if (this.roundCoordinates) {
            x = (x | 0) + 0.5;
            bx = (bx | 0) + 0.5;
          }

          let markedRect;
          if (span.marked) {
            markedRect = this.renderSpanMarkedRect(bx, by, bw, bh, chunk);
          }

          // Nicely spread out labels/text and leave space for mark highlight such that adding
          // the mark doesn't change the overall layout
          chunkFrom = Math.min(bx - this.markedSpanSize, chunkFrom);
          chunkTo = Math.max(bx + bw + this.markedSpanSize, chunkTo);
          let fragmentHeight = bh + 2 * this.markedSpanSize;
          let shadowRect;
          if (span.shadowClass && span.shadowClass.match(this.shadowClassPattern)) {
            shadowRect = this.renderFragmentShadowRect(bx, by, bw, bh, fragment);
            chunkFrom = Math.min(bx - this.rectShadowSize, chunkFrom);
            chunkTo = Math.max(bx + bw + this.rectShadowSize, chunkTo);
            fragmentHeight = Math.max(bh + 2 * this.rectShadowSize, fragmentHeight);
          }

          fragment.rect = this.renderFragmentRect(bx, by, bw, bh, yy, fragment, rectClass, bgColor, borderColor);
          fragment.left = bx; // TODO put it somewhere nicer?
          fragment.right = bx + bw; // TODO put it somewhere nicer?
          if (!(span.shadowClass || span.marked)) {
            chunkFrom = Math.min(bx, chunkFrom);
            chunkTo = Math.max(bx + bw, chunkTo);
            fragmentHeight = Math.max(bh, fragmentHeight);
          }

          fragment.rectBox = new RectBox(bx, by - span.floor, bw, bh);
          fragment.height = span.floor + hh + 3 * Configuration.visual.margin.y + Configuration.visual.curlyHeight + Configuration.visual.arcSpacing;
          const spacedTowerId = fragment.towerId * 2;
          if (!fragmentHeights[spacedTowerId] || fragmentHeights[spacedTowerId] < fragment.height) {
            fragmentHeights[spacedTowerId] = fragment.height;
          }

          $(fragment.rect).attr('y', yy - Configuration.visual.margin.y - span.floor);
          if (shadowRect) {
            $(shadowRect).attr('y', yy - this.rectShadowSize - Configuration.visual.margin.y - span.floor);
          }
          if (markedRect) {
            $(markedRect).attr('y', yy - this.markedSpanSize - Configuration.visual.margin.y - span.floor);
          }

          if (span.attributeMerge.box === "crossed") {
            this.renderFragmentCrossOut(xx, yy, hh, fragment);
          }

          this.svg.text(fragment.group, x, y - span.floor, this.data.spanAnnTexts[fragment.glyphedLabelText], {fill: fgColor});

          // Make curlies to show the fragment
          if (fragment.drawCurly) {
            this.renderCurly(fragment, x, yy, hh);
            chunkFrom = Math.min(fragment.curly.from, chunkFrom);
            chunkTo = Math.max(fragment.curly.to, chunkTo);
            fragmentHeight = Math.max(Configuration.visual.curlyHeight, fragmentHeight);
          }

          if (fragment === span.headFragment) {
            // find the gap to fit the backwards arcs, but only on
            // head fragment - other fragments don't have arcs
            $.each(span.incoming, (arcId, arc) => {
              const leftSpan = this.data.spans[arc.origin];
              const origin = leftSpan.headFragment.chunk;
              let border;
              if (chunk.index === origin.index) {
                hasInternalArcs = true;
              }
              if (origin.row) {
                let labels = Util.getArcLabels(this.spanTypes, leftSpan.type, arc.type, this.relationTypesHash);
                if (!labels.length) {
                  labels = [arc.type];
                }

                if (arc.eventDescId && this.data.eventDescs[arc.eventDescId]) {
                  if (this.data.eventDescs[arc.eventDescId].labelText) {
                    labels = [this.data.eventDescs[arc.eventDescId].labelText];
                  }
                }

                if (origin.row.index === rowIndex) {
                  border = origin.translation.x + leftSpan.fragments[leftSpan.fragments.length - 1].right;
                } else {
                  if (this.rtlmode) {
                    border = 0;
                  } else {
                    border = Configuration.visual.margin.x + this.sentNumMargin + this.rowPadding;
                  }
                }

                const labelNo = Configuration.abbrevsOn ? labels.length - 1 : 0;
                const smallestLabelWidth = this.data.sizes.arcs.widths[labels[labelNo]] + 2 * this.minArcSlant;

                const gap = Math.abs(currentX + (this.rtlmode ? -bx : bx) - border);

                let arcSpacing = smallestLabelWidth - gap;
                if (!hasLeftArcs || spacing < arcSpacing) {
                  spacing = arcSpacing;
                  spacingChunkId = origin.index + 1;
                }
                arcSpacing = smallestLabelWidth - bx;
                if (!hasLeftArcs || spacingRowBreak < arcSpacing) {
                  spacingRowBreak = arcSpacing;
                }
                hasLeftArcs = true;
              } else {
                hasRightArcs = true;
              }
            });

            $.each(span.outgoing, (arcId, arc) => {
              const leftSpan = this.data.spans[arc.target];
              const target = leftSpan.headFragment.chunk;
              let border;
              if (target.row) {
                let labels = Util.getArcLabels(this.spanTypes, span.type, arc.type, this.relationTypesHash);
                if (!labels.length)
                  labels = [arc.type];

                if (arc.eventDescId && this.data.eventDescs[arc.eventDescId]) {
                  if (this.data.eventDescs[arc.eventDescId].labelText) {
                    labels = [this.data.eventDescs[arc.eventDescId].labelText];
                  }
                }

                if (target.row.index === rowIndex) {
                  // same row, but before this
                  border = target.translation.x + leftSpan.fragments[leftSpan.fragments.length - 1].right;
                } else {
                  if (this.rtlmode) {
                    border = 0;
                  } else {
                    border = Configuration.visual.margin.x + this.sentNumMargin + this.rowPadding;
                  }
                }

                const labelNo = Configuration.abbrevsOn ? labels.length - 1 : 0;
                const smallestLabelWidth = this.data.sizes.arcs.widths[labels[labelNo]] + 2 * this.minArcSlant;

                const gap = Math.abs(currentX + (this.rtlmode ? -bx : bx) - border);

                let arcSpacing = smallestLabelWidth - gap;
                if (!hasLeftArcs || spacing < arcSpacing) {
                  spacing = arcSpacing;
                  spacingChunkId = target.index + 1;
                }
                arcSpacing = smallestLabelWidth - bx;
                if (!hasLeftArcs || spacingRowBreak < arcSpacing) {
                  spacingRowBreak = arcSpacing;
                }
                hasLeftArcs = true;
              } else {
                hasRightArcs = true;
              }
            });
          }
          fragmentHeight += span.floor || Configuration.visual.curlyHeight;
          if (fragmentHeight > chunkHeight) {
            chunkHeight = fragmentHeight;
          }
          hasAnnotations = true;
        }); // fragments

        // positioning of the chunk
        chunk.right = chunkTo;
        const textWidth = this.data.sizes.texts.widths[chunk.text];
        chunkHeight += this.data.sizes.texts.height;
        // If chunkFrom becomes negative (LTR) or chunkTo becomes positive (RTL), then boxX becomes positive
        const boxX = this.rtlmode ? chunkTo : -Math.min(chunkFrom, 0);

        let boxWidth;
        if (this.rtlmode) {
          boxWidth = Math.max(textWidth, -chunkFrom) - Math.min(0, -chunkTo);
        } else {
          boxWidth = Math.max(textWidth, chunkTo) - Math.min(0, chunkFrom);
        }

        if (spacing > 0) {
          currentX += this.rtlmode ? -spacing : spacing;
        }

        const rightBorderForArcs = hasRightArcs ? this.arcHorizontalSpacing : (hasInternalArcs ? this.arcSlant : 0);
        const leftBorderForArcs = hasLeftArcs ? this.arcHorizontalSpacing : (hasInternalArcs ? this.arcSlant : 0);
        const lastX = currentX;
        const lastRow = row;

        // Is there a sentence break at the current chunk (i.e. it is the first chunk in a new
        // sentence) - if yes and the current sentence is not the same as the sentence to which
        // the chunk belongs, then fill in additional rows
        if (chunk.sentence) {
          while (sentenceNumber < chunk.sentence - 1) {
            sentenceNumber++;
            row.arcs = this.svg.group(row.group, {'class': 'arcs'});
            rows.push(row);

            row = new Row(this.svg);
            row.sentence = sentenceNumber;
            sentenceToggle = 1 - sentenceToggle;
            row.backgroundIndex = sentenceToggle;
            row.index = ++rowIndex;
          }
          // Not changing row background color here anymore - we do this later now when the next
          // row is added
        }

        let chunkDoesNotFit;
        if (this.rtlmode) {
          chunkDoesNotFit = currentX - boxWidth - leftBorderForArcs <=
            2 * Configuration.visual.margin.x;
        } else {
          chunkDoesNotFit = currentX + boxWidth + rightBorderForArcs >=
            this.canvasWidth - 2 * Configuration.visual.margin.x;
        }

        if (chunk.sentence > sourceData.sentence_number_offset || chunkDoesNotFit) {
          // the chunk does not fit
          row.arcs = this.svg.group(row.group, {'class': 'arcs'});
          let indent = 0;
          if (chunk.lastSpace) {
            const spaceLen = chunk.lastSpace.length || 0;
            let spacePos;
            if (chunk.sentence) {
              // If this is line-initial spacing, fetch the sentence to which the chunk belongs
              // so we can determine where it begins
              const sentFrom = sourceData.sentence_offsets[chunk.sentence - sourceData.sentence_number_offset][0];
              spacePos = spaceLen - (chunk.from - sentFrom);
            } else {
              spacePos = 0;
            }
            for (let i = spacePos; i < spaceLen; i++) {
              indent += this.spaceWidths[chunk.lastSpace[i]] * (this.fontZoom / 100.0) || 0;
            }
          }

          if (this.rtlmode) {
            currentX = this.canvasWidth - (Configuration.visual.margin.x + this.sentNumMargin + this.rowPadding +
              (hasRightArcs ? this.arcHorizontalSpacing : (hasInternalArcs ? this.arcSlant : 0)) /* +
                          spaceWidth*/
              - indent);
          } else {
            currentX = Configuration.visual.margin.x + this.sentNumMargin + this.rowPadding +
              (hasLeftArcs ? this.arcHorizontalSpacing : (hasInternalArcs ? this.arcSlant : 0)) /*+
                        spaceWidth*/
              + indent;
          }

          if (hasLeftArcs) {
            const adjustedCurTextWidth = this.data.sizes.texts.widths[chunk.text] + this.arcHorizontalSpacing;
            if (adjustedCurTextWidth > maxTextWidth) {
              maxTextWidth = adjustedCurTextWidth;
            }
          }

          if (spacingRowBreak > 0) {
            currentX += this.rtlmode ? -spacingRowBreak : spacingRowBreak;
            spacing = 0; // do not center intervening elements
          }

          // new row
          rows.push(row);

          this.svg.remove(chunk.group);
          row = new Row(this.svg);
          // Change row background color if a new sentence is starting
          if (chunk.sentence) {
            sentenceToggle = 1 - sentenceToggle;
          }
          row.backgroundIndex = sentenceToggle;
          row.index = ++rowIndex;
          this.svg.add(row.group, chunk.group);
          chunk.group = row.group.lastElementChild;
          $(chunk.group).children("g[class='span']").each((index, element) => chunk.fragments[index].group = element);
          $(chunk.group).find("rect[data-span-id]").each((index, element) => chunk.fragments[index].rect = element);
        }

        // break the text highlights when the row breaks
        if (row.index !== lastRow.index) {
          $.each(openTextHighlights, (textId, textDesc) => {
            if (textDesc[3] !== lastX) {
              let newDesc;
              if (this.rtlmode) {
                newDesc = [lastRow, textDesc[3], lastX - boxX, textDesc[4]];
              } else {
                newDesc = [lastRow, textDesc[3], lastX + boxX, textDesc[4]];
              }
              textMarkedRows.push(newDesc);
            }
            textDesc[3] = currentX;
          });
        }

        // open text highlights
        $.each(chunk.markedTextStart, (textNo, textDesc) => {
          textDesc[3] += currentX + (this.rtlmode ? -boxX : boxX);
          openTextHighlights[textDesc[0]] = textDesc;
        });

        // close text highlights
        $.each(chunk.markedTextEnd, (textNo, textDesc) => {
          textDesc[3] += currentX + (this.rtlmode ? -boxX : boxX);
          let startDesc = openTextHighlights[textDesc[0]];
          delete openTextHighlights[textDesc[0]];
          textMarkedRows.push([row, startDesc[3], textDesc[3], startDesc[4]]);
        });

        // XXX check this - is it used? should it be lastRow?
        if (hasAnnotations) {
          row.hasAnnotations = true;
        }

        if (chunk.sentence > sourceData.sentence_number_offset) {
          row.sentence = ++sentenceNumber;
        }

        if (spacing > 0) {
          // if we added a gap, center the intervening elements
          spacing /= 2;
          const firstChunkInRow = row.chunks[row.chunks.length - 1];
          if (firstChunkInRow === undefined) {
            console.log('warning: firstChunkInRow undefined, chunk:', chunk);
          } else { // valid firstChunkInRow
            if (spacingChunkId < firstChunkInRow.index) {
              spacingChunkId = firstChunkInRow.index + 1;
            }
            for (let chunkIndex = spacingChunkId; chunkIndex < chunk.index; chunkIndex++) {
              const movedChunk = this.data.chunks[chunkIndex];
              this.translate(movedChunk, movedChunk.translation.x + spacing, 0);
              movedChunk.textX += spacing;
            }
          }
        }

        row.chunks.push(chunk);
        chunk.row = row;

        this.translate(chunk, currentX + (this.rtlmode ? -boxX : boxX), 0);
        chunk.textX = currentX + (this.rtlmode ? -boxX : boxX);
        currentX += this.rtlmode ? -boxWidth : boxWidth;
      }); // chunks

      // Add trailing empty rows
      while (sentenceNumber < (sourceData.sentence_offsets.length + sourceData.sentence_number_offset - 1)) {
        sentenceNumber++;
        row.arcs = this.svg.group(row.group, {'class': 'arcs'});
        rows.push(row);
        row = new Row(this.svg);
        row.sentence = sentenceNumber;
        sentenceToggle = 1 - sentenceToggle;
        row.backgroundIndex = sentenceToggle;
        row.index = ++rowIndex;
      }

      // finish the last row
      row.arcs = this.svg.group(row.group, {'class': 'arcs'});
      rows.push(row);

      return [rows, fragmentHeights, textMarkedRows];
    }

    renderChunksPass2(textGroup, textMarkedRows) {
      // chunk index sort functions for overlapping fragment drawing
      // algorithm; first for left-to-right pass, sorting primarily
      // by start offset, second for right-to-left pass by end
      // offset. Secondary sort by fragment length in both cases.
      let currentChunk;

      const lrChunkComp = (a, b) => {
        const ac = currentChunk.fragments[a];
        const bc = currentChunk.fragments[b];
        const startDiff = Util.cmp(ac.from, bc.from);
        return startDiff !== 0 ? startDiff : Util.cmp(bc.to - bc.from, ac.to - ac.from);
      };

      const rlChunkComp = (a, b) => {
        const ac = currentChunk.fragments[a];
        const bc = currentChunk.fragments[b];
        const endDiff = Util.cmp(bc.to, ac.to);
        return endDiff !== 0 ? endDiff : Util.cmp(bc.to - bc.from, ac.to - ac.from);
      };

      let prevChunk = null;
      let rowTextGroup = null;
      $.each(this.data.chunks, (chunkNo, chunk) => {
        // context for sort
        currentChunk = chunk;

        // Add spacers to reduce jumpyness of selection
        if (!rowTextGroup || prevChunk.row !== chunk.row) {
          if (rowTextGroup) {
            this.horizontalSpacer(this.svg, rowTextGroup, 0, prevChunk.row.textY, 1, {
              'data-chunk-id': prevChunk.index,
              'class': 'row-final spacing'
            });
          }
          rowTextGroup = this.svg.group(textGroup, {'class': 'text-row'});
        }
        prevChunk = chunk;

        const nextChunk = this.data.chunks[chunkNo + 1];

        if (this.rtlmode) {
          // Render every text chunk as a SVG text so we maintain control over the layout. When
          // rendering as a SVG span (as brat does), then the browser changes the layout on the
          // X-axis as it likes in RTL mode.
          if (!rowTextGroup.firstChild) {
            this.horizontalSpacer(svg, rowTextGroup, 0, chunk.row.textY, 1, {
              'class': 'row-initial spacing',
              'data-chunk-id': chunk.index
            });
          }

          this.svg.text(rowTextGroup, chunk.textX, chunk.row.textY, chunk.text, {
            'data-chunk-id': chunk.index
          });

          // If there needs to be space between this chunk and the next one, add a spacer
          // item that stretches across the entire inter-chunk space. This ensures a
          // smooth selection.
          if (nextChunk) {
            const spaceX = chunk.textX - this.data.sizes.texts.widths[chunk.text];
            const spaceWidth = chunk.textX - this.data.sizes.texts.widths[chunk.text] - nextChunk.textX;
            this.horizontalSpacer(this.svg, rowTextGroup, spaceX, chunk.row.textY, spaceWidth, {
              'data-chunk-id': chunk.index
            });
          }
        } else {
          // Original rendering using tspan in ltr mode as it play nicer with selection
          if (!rowTextGroup.firstChild) {
            this.horizontalSpacer(this.svg, rowTextGroup, 0, chunk.row.textY, 1, {
              'class': 'row-initial spacing',
              'data-chunk-id': chunk.index
            });
          }

          this.svg.text(rowTextGroup, chunk.textX, chunk.row.textY, chunk.text, {
            'data-chunk-id': chunk.index
          });

          // If there needs to be space between this chunk and the next one, add a spacer
          // item that stretches across the entire inter-chunk space. This ensures a
          // smooth selection.
          if (nextChunk) {
            const spaceX = chunk.textX + this.data.sizes.texts.widths[chunk.text];
            const spaceWidth = nextChunk.textX - spaceX;
            this.horizontalSpacer(this.svg, rowTextGroup, spaceX, chunk.row.textY, spaceWidth, {
              'data-chunk-id': chunk.index
            });
          }
        }

        // chunk backgrounds
        if (chunk.fragments.length) {
          const orderedIdx = [];
          for (let i = chunk.fragments.length - 1; i >= 0; i--) {
            orderedIdx.push(i);
          }

          // Mark entity nesting height/depth (number of
          // nested/nesting entities). To account for crossing
          // brackets in a (mostly) reasonable way, determine
          // depth/height separately in a left-to-right traversal
          // and a right-to-left traversal.
          orderedIdx.sort(lrChunkComp);

          {
            let openFragments = [];
            for (let i = 0; i < orderedIdx.length; i++) {
              const current = chunk.fragments[orderedIdx[i]];
              current.nestingHeightLR = 0;
              current.nestingDepthLR = 0;
              const stillOpen = [];
              for (let o = 0; o < openFragments.length; o++) {
                if (openFragments[o].to > current.from) {
                  stillOpen.push(openFragments[o]);
                  openFragments[o].nestingHeightLR++;
                }
              }
              openFragments = stillOpen;
              current.nestingDepthLR = openFragments.length;
              openFragments.push(current);
            }
          }

          // re-sort for right-to-left traversal by end position
          orderedIdx.sort(rlChunkComp);

          {
            let openFragments = [];
            for (let i = 0; i < orderedIdx.length; i++) {
              const current = chunk.fragments[orderedIdx[i]];
              current.nestingHeightRL = 0;
              current.nestingDepthRL = 0;
              const stillOpen = [];
              for (let o = 0; o < openFragments.length; o++) {
                if (openFragments[o].from < current.to) {
                  stillOpen.push(openFragments[o]);
                  openFragments[o].nestingHeightRL++;
                }
              }
              openFragments = stillOpen;
              current.nestingDepthRL = openFragments.length;
              openFragments.push(current);
            }
          }

          // the effective depth and height are the max of those
          // for the left-to-right and right-to-left traversals.
          for (let i = 0; i < orderedIdx.length; i++) {
            const c = chunk.fragments[orderedIdx[i]];
            c.nestingHeight = c.nestingHeightLR > c.nestingHeightRL ? c.nestingHeightLR : c.nestingHeightRL;
            c.nestingDepth = c.nestingDepthLR > c.nestingDepthRL ? c.nestingDepthLR : c.nestingDepthRL;
          }

          // Re-order by nesting height and draw in order
          orderedIdx.sort(function (a, b) {
            return Util.cmp(chunk.fragments[b].nestingHeight, chunk.fragments[a].nestingHeight);
          });

          for (let i = 0; i < chunk.fragments.length; i++) {
            const fragment = chunk.fragments[orderedIdx[i]];
            if (fragment.span.hidden) {
              continue;
            }

            const spanDesc = this.spanTypes[fragment.span.type];
            let bgColor = ((spanDesc && spanDesc.bgColor) || '#ffffff');


            if (fragment.span.color) {
              bgColor = fragment.span.color;
            }

            // Tweak for nesting depth/height. Recognize just three
            // levels for now: normal, nested, and nesting, where
            // nested+nesting yields normal. (Currently testing
            // minor tweak: don't shrink for depth 1 as the nesting
            // highlight will grow anyway [check nestingDepth > 1])
            let shrink = 0;
            if (fragment.nestingDepth > 1 && fragment.nestingHeight === 0) {
              shrink = 1;
            } else if (fragment.nestingDepth === 0 && fragment.nestingHeight > 0) {
              shrink = -1;
            }

            const yShrink = shrink * this.nestingAdjustYStepSize;
            const xShrink = shrink * this.nestingAdjustXStepSize;
            // bit lighter
            const lightBgColor = Util.adjustColorLightness(bgColor, 0.8);
            // tweak for Y start offset (and corresponding height
            // reduction): text rarely hits font max height, so this
            // tends to look better
            const yStartTweak = 1;

            // Store highlight coordinates
            fragment.highlightPos = {
              x: chunk.textX + (this.rtlmode ? (fragment.curly.from - xShrink) : (fragment.curly.from + xShrink)),
              y: chunk.row.textY + this.data.sizes.texts.y + yShrink + yStartTweak,
              w: fragment.curly.to - fragment.curly.from - 2 * xShrink,
              h: this.data.sizes.texts.height - 2 * yShrink - yStartTweak,
            };

            // Avoid exception because width < 0 is not allowed
            if (fragment.highlightPos.w <= 0) {
              fragment.highlightPos.w = 1;
            }

            // Render highlight
            this.svg.rect(this.highlightGroup,
              fragment.highlightPos.x, fragment.highlightPos.y,
              fragment.highlightPos.w, fragment.highlightPos.h,
              {
                fill: lightBgColor,
                rx: this.highlightRounding.x,
                ry: this.highlightRounding.y,
              });
          }
        }
      });

      // Prevent text selection from being jumpy
      if (rowTextGroup) {
        this.horizontalSpacer(this.svg, rowTextGroup, 0, currentChunk.row.textY, 1, {
          'data-chunk-id': currentChunk.index,
          'class': 'row-final spacing'
        });
      }

      // draw the markedText
      $.each(textMarkedRows, (textRowNo, textRowDesc) => {
        this.svg.rect(this.highlightGroup,
          textRowDesc[1] - 2, textRowDesc[0].textY - this.data.sizes.fragments.height,
          textRowDesc[2] - textRowDesc[1] + 4, this.data.sizes.fragments.height + 4,
          {'class': textRowDesc[3]}
        );
      });
    }

    renderBumpFragmentHeightsMinimumToArcStartHeight(fragmentHeights) {
      for (let i = 0; i < fragmentHeights.length; i++) {
        if (!fragmentHeights[i] || fragmentHeights[i] < Configuration.visual.arcStartHeight) {
          fragmentHeights[i] = Configuration.visual.arcStartHeight;
        }
      }
    }

    /**
     * @param {number[]} fragmentHeights
     */
    renderCalculateArcJumpHeight(fragmentHeights) {
      // find out how high the arcs have to go
      $.each(this.data.arcs, (arcNo, arc) => {
        arc.jumpHeight = 0;
        let fromFragment = this.data.spans[arc.origin].headFragment;
        let toFragment = this.data.spans[arc.target].headFragment;
        if (fromFragment.span.hidden || toFragment.span.hidden) {
          arc.hidden = true;
          return;
        }

        if (fromFragment.towerId > toFragment.towerId) {
          const tmp = fromFragment;
          fromFragment = toFragment;
          toFragment = tmp;
        }

        let from, to;
        if (fromFragment.chunk.index === toFragment.chunk.index) {
          from = fromFragment.towerId;
          to = toFragment.towerId;
        } else {
          from = fromFragment.towerId + 1;
          to = toFragment.towerId - 1;
        }

        for (let i = from; i <= to; i++) {
          if (arc.jumpHeight < fragmentHeights[i * 2]) {
            arc.jumpHeight = fragmentHeights[i * 2];
          }
        }
      });
    }

    renderSortArcs() {
      // sort the arcs
      this.data.arcs.sort((a, b) => {
        // first write those that have less to jump over
        let tmp = a.jumpHeight - b.jumpHeight;
        if (tmp)
          return tmp < 0 ? -1 : 1;
        // if equal, then those that span less distance
        tmp = a.dist - b.dist;
        if (tmp)
          return tmp < 0 ? -1 : 1;
        // if equal, then those where heights of the targets are smaller
        tmp = this.data.spans[a.origin].headFragment.height + this.data.spans[a.target].headFragment.height -
          this.data.spans[b.origin].headFragment.height - this.data.spans[b.target].headFragment.height;
        if (tmp)
          return tmp < 0 ? -1 : 1;
        // if equal, then those with the lower origin
        tmp = this.data.spans[a.origin].headFragment.height - this.data.spans[b.origin].headFragment.height;
        if (tmp)
          return tmp < 0 ? -1 : 1;
        // if equal, they're just equal.
        return 0;
      });
    }

    /**
     * @param {Row[]} rows
     * @param {number[]} fragmentHeights
     */
    renderAssignFragmentsToRows(rows, fragmentHeights) {
      // see which fragments are in each row
      let heightsStart = 0;
      let heightsRowsAdded = 0;
      $.each(rows, (rowId, row) => {
        let seenFragment = false;
        row.heightsStart = row.heightsEnd = heightsStart;
        $.each(row.chunks, (chunkId, chunk) => {
          if (chunk.lastFragmentIndex !== undefined) {
            // fragmentful chunk
            seenFragment = true;
            const heightsEndIndex = chunk.lastFragmentIndex * 2 + heightsRowsAdded;
            if (row.heightsEnd < heightsEndIndex) {
              row.heightsEnd = heightsEndIndex;
            }
            const heightsStartIndex = chunk.firstFragmentIndex * 2 + heightsRowsAdded;
            if (row.heightsStart > heightsStartIndex) {
              row.heightsStart = heightsStartIndex;
            }
          }
        });
        fragmentHeights.splice(row.heightsStart, 0, Configuration.visual.arcStartHeight);
        heightsRowsAdded++;

        row.heightsAdjust = heightsRowsAdded;
        if (seenFragment) {
          row.heightsEnd += 2;
        }
        heightsStart = row.heightsEnd + 1;
      });
    }

    /**
     * @param {SVGElement} defs
     */
    renderDragArcMarker(defs) {
      // draw the drag arc marker
      const arrowhead = this.svg.marker(defs, 'drag_arrow',
        5, 2.5, 5, 5, 'auto',
        {
          markerUnits: 'strokeWidth',
          'class': 'drag_fill',
        });
      this.svg.polyline(arrowhead, [[0, 0], [5, 2.5], [0, 5], [0.2, 2.5]]);
      const arcDragArc = this.svg.path(this.svg.createPath(), {
        markerEnd: 'url(#drag_arrow)',
        'class': 'drag_stroke',
        fill: 'none',
        visibility: 'hidden',
      });
      this.dispatcher.post('arcDragArcDrawn', [arcDragArc]);
    }

    /**
     * @param {Row[]} rows
     * @param {number[]} fragmentHeights
     * @param {SVGElement} defs
     */
    renderArcs(rows, fragmentHeights, defs) {
      const arrows = {};
      const arrow = this.makeArrow(defs, 'none');
      if (arrow) {
        arrows['none'] = arrow;
      }

      const arcCache = {};

      // add the arcs
      $.each(this.data.arcs, (arcNo, arc) => {
        if (arc.hidden) {
          return;
        }

        // separate out possible numeric suffix from type
        let noNumArcType;
        let splitArcType;
        if (arc.type) {
          splitArcType = arc.type.match(/^(.*)(\d*)$/);
          noNumArcType = splitArcType[1];
        }

        const originSpan = this.data.spans[arc.origin];
        const targetSpan = this.data.spans[arc.target];

        const leftToRight = originSpan.headFragment.towerId < targetSpan.headFragment.towerId;
        let left, right;
        if (leftToRight) {
          left = originSpan.headFragment;
          right = targetSpan.headFragment;
        } else {
          left = targetSpan.headFragment;
          right = originSpan.headFragment;
        }

        // fall back on relation types in case we still don't have
        // an arc description, with final fallback to unnumbered relation
        let arcDesc = this.relationTypesHash[arc.type] || this.relationTypesHash[noNumArcType];

        // if it's not a relationship, see if we can find it in span descriptions
        // TODO: might make more sense to reformat this as dict instead of searching through the list every type
        const spanDesc = this.spanTypes[originSpan.type];
        if (!arcDesc && spanDesc && spanDesc.arcs) {
          $.each(spanDesc.arcs, (arcDescNo, arcDescIter) => {
            if (arcDescIter.type === arc.type) {
              arcDesc = arcDescIter;
            }
          });
        }

        // last fall back on unnumbered type if not found in full
        if (!arcDesc && noNumArcType && noNumArcType !== arc.type && spanDesc && spanDesc.arcs) {
          $.each(spanDesc.arcs, (arcDescNo, arcDescIter) => {
            if (arcDescIter.type === noNumArcType) {
              arcDesc = arcDescIter;
            }
          });
        }

        // empty default
        if (!arcDesc) {
          arcDesc = {};
        }

        let color = ((arcDesc && arcDesc.color) || '#000000');
        if (color === 'hidden') {
          return;
        }

        if (arc.eventDescId && this.data.eventDescs[arc.eventDescId]) {
          if (this.data.eventDescs[arc.eventDescId].color) {
            color = [this.data.eventDescs[arc.eventDescId].color];
          }
        }

        const symmetric = arcDesc && arcDesc.properties && arcDesc.properties.symmetric;
        const dashArray = arcDesc && arcDesc.dashArray;
        const arrowHead = ((arcDesc && arcDesc.arrowHead) || 'triangle,5') + ',' + color;
        const labelArrowHead = ((arcDesc && arcDesc.labelArrow) || 'triangle,5') + ',' + color;

        const leftBox = this.rowBBox(left);
        const rightBox = this.rowBBox(right);
        const leftRow = left.chunk.row.index;
        const rightRow = right.chunk.row.index;

        if (!arrows[arrowHead]) {
          const arrow = this.makeArrow(defs, arrowHead);
          if (arrow) {
            arrows[arrowHead] = arrow;
          }
        }
        if (!arrows[labelArrowHead]) {
          const arrow = this.makeArrow(defs, labelArrowHead);
          if (arrow) {
            arrows[labelArrowHead] = arrow;
          }
        }

        // find the next height
        let height;
        let fromIndex2, toIndex2;
        if (left.chunk.index === right.chunk.index) {
          fromIndex2 = left.towerId * 2 + left.chunk.row.heightsAdjust;
          toIndex2 = right.towerId * 2 + right.chunk.row.heightsAdjust;
        } else {
          fromIndex2 = left.towerId * 2 + 1 + left.chunk.row.heightsAdjust;
          toIndex2 = right.towerId * 2 - 1 + right.chunk.row.heightsAdjust;
        }

        if (!this.collapseArcSpace) {
          height = this.findArcHeight(fromIndex2, toIndex2, fragmentHeights);
          this.adjustFragmentHeights(fromIndex2, toIndex2, fragmentHeights, height);

          // Adjust the height to align with pixels when rendered
          // TODO: on at least Chrome, it doesn't make a difference: lines come out pixel-width even without it. Check.
          height += 0.5;
        }

        let chunkReverse = false;
        const ufoCatcher = originSpan.headFragment.chunk.index === targetSpan.headFragment.chunk.index;
        if (ufoCatcher) {
          chunkReverse = leftBox.x + leftBox.width / 2 < rightBox.x + rightBox.width / 2;
        }
        const ufoCatcherMod = ufoCatcher ? chunkReverse ? -0.5 : 0.5 : 1;

        for (let rowIndex = leftRow; rowIndex <= rightRow; rowIndex++) {
          const row = rows[rowIndex];
          if (!row.chunks.length) {
            continue;
          }

          row.hasAnnotations = true;

          const fromIndex2R = rowIndex === leftRow ? fromIndex2 : row.heightsStart;
          const toIndex2R = rowIndex === rightRow ? toIndex2 : row.heightsEnd;
          if (this.collapseArcSpace) {
            height = this.findArcHeight(fromIndex2R, toIndex2R, fragmentHeights);
          }

          const arcGroup = this.svg.group(row.arcs, {
            'data-from': arc.origin,
            'data-to': arc.target,
            'data-id': arc.eventDescId
          });

          let from;
          if (rowIndex === leftRow) {
            if (this.rtlmode) {
              from = leftBox.x + (chunkReverse ? leftBox.width : 0);
            } else {
              from = leftBox.x + (chunkReverse ? 0 : leftBox.width);
            }
          } else {
            from = this.rtlmode ? this.canvasWidth - 2 * Configuration.visual.margin.y - this.sentNumMargin : this.sentNumMargin;
          }

          let to;
          if (rowIndex === rightRow) {
            if (this.rtlmode) {
              to = rightBox.x + (chunkReverse ? 0 : rightBox.width);
            } else {
              to = rightBox.x + (chunkReverse ? rightBox.width : 0);
            }
          } else {
            to = this.rtlmode ? 0 : this.canvasWidth - 2 * Configuration.visual.margin.y;
          }

          let adjustHeight = true;
          if (this.collapseArcs) {
            let arcCacheKey = arc.type + ' ' + rowIndex + ' ' + from + ' ' + to;
            if (rowIndex === leftRow)
              arcCacheKey = left.span.id + ' ' + arcCacheKey;
            if (rowIndex === rightRow)
              arcCacheKey += ' ' + right.span.id;
            const rowHeight = arcCache[arcCacheKey];
            if (rowHeight !== undefined) {
              height = rowHeight;
              adjustHeight = false;
            } else {
              arcCache[arcCacheKey] = height;
            }
          }

          if (this.collapseArcSpace && adjustHeight) {
            this.adjustFragmentHeights(fromIndex2R, toIndex2R, fragmentHeights, height);

            // Adjust the height to align with pixels when rendered
            // TODO: on at least Chrome, this doesn't make a difference:
            // the lines come out pixel-width even without it. Check.
            height += 0.5;
          }

          const originType = this.data.spans[arc.origin].type;
          const arcLabels = Util.getArcLabels(this.spanTypes, originType, arc.type, this.relationTypesHash);
          let labelText = Util.arcDisplayForm(this.spanTypes, originType, arc.type, this.relationTypesHash);
          // if (Configuration.abbrevsOn && !ufoCatcher && arcLabels) {
          if (Configuration.abbrevsOn && arcLabels) {
            let labelIdx = 1; // first abbreviation

            // strictly speaking 2*arcSlant would be needed to allow for
            // the full-width arcs to fit, but judged abbreviated text
            // to be more important than the space for arcs.
            const maxLength = (to - from) - (this.arcSlant);
            while (this.data.sizes.arcs.widths[labelText] > maxLength && arcLabels[labelIdx]) {
              labelText = arcLabels[labelIdx];
              labelIdx++;
            }
          }

          if (arc.eventDescId && this.data.eventDescs[arc.eventDescId]) {
            if (this.data.eventDescs[arc.eventDescId].labelText) {
              labelText = this.data.eventDescs[arc.eventDescId].labelText;
            }
          }

          let shadowGroup;
          if (arc.shadowClass || arc.marked) {
            shadowGroup = this.svg.group(arcGroup);
          }

          const options = {
            //'fill': color,
            'fill': '#000000',
            'data-arc-role': arc.type,
            'data-arc-origin': arc.origin,
            'data-arc-target': arc.target,
            // TODO: confirm this is unused and remove.
            //'data-arc-id': arc.id,
            'data-arc-ed': arc.eventDescId,
          };

          // construct SVG text, showing possible trailing index
          // numbers (as in e.g. "Theme2") as subscripts
          let svgText;
          if (!splitArcType[2]) {
            // no subscript, simple string suffices
            svgText = labelText;
          } else {
            // Need to parse out possible numeric suffixes to avoid
            // duplicating number in label and its subscript
            const splitLabelText = labelText.match(/^(.*?)(\d*)$/);
            const noNumLabelText = splitLabelText[1];

            svgText = this.svg.createText();
            // TODO: to address issue #453, attaching options also
            // to spans, not only primary text. Make sure there
            // are no problems with this.
            svgText.span(noNumLabelText, options);
            const subscriptSettings = {
              'dy': '0.3em',
              'font-size': '80%'
            };
            // alternate possibility
            //                 var subscriptSettings = {
            //                   'baseline-shift': 'sub',
            //                   'font-size': '80%'
            //                 };
            $.extend(subscriptSettings, options);
            svgText.span(splitArcType[2], subscriptSettings);
          }

          // guess at the correct baseline shift to get vertical centering.
          // (CSS dominant-baseline can't be used as not all SVG renders support it.)
          const baseline_shift = this.data.sizes.arcs.height / 4;
          this.svg.text(arcGroup, (from + to) / 2, -height + baseline_shift, svgText, options);

          const width = this.data.sizes.arcs.widths[labelText];
          const textBox = {
            x: (from + to - width) / 2,
            width: width,
            y: -height - this.data.sizes.arcs.height / 2,
            height: this.data.sizes.arcs.height,
          };

          if (arc.marked) {
            this.svg.rect(shadowGroup,
              textBox.x - this.markedArcSize, textBox.y - this.markedArcSize,
              textBox.width + 2 * this.markedArcSize, textBox.height + 2 * this.markedArcSize, {
                filter: 'url(#Gaussian_Blur)',
                'class': "shadow_EditHighlight",
                rx: this.markedArcSize,
                ry: this.markedArcSize,
              });
          }

          if (arc.shadowClass) {
            this.renderArcShadow(arc, shadowGroup, textBox);
          }
          let textStart = textBox.x;
          let textEnd = textBox.x + textBox.width;

          // adjust by margin for arc drawing
          textStart -= Configuration.visual.arcTextMargin;
          textEnd += Configuration.visual.arcTextMargin;

          if (from > to) {
            const tmp = textStart;
            textStart = textEnd;
            textEnd = tmp;
          }

          let path;

          if (this.roundCoordinates) {
            // don't ask
            height = (height | 0) + 0.5;
          }
          if (height > row.maxArcHeight)
            row.maxArcHeight = height;

          let myArrowHead = (arcDesc && arcDesc.arrowHead);
          let arrowName = (symmetric ? myArrowHead || 'none' :
              (leftToRight ? 'none' : myArrowHead || 'triangle,5')
          ) + ',' + color;
          let arrowType = arrows[arrowName];
          let arrowDecl = arrowType && ('url(#' + arrowType + ')');

          let arrowAtLabelAdjust = 0;
          const labelArrowDecl = null;
          const myLabelArrowHead = (arcDesc && arcDesc.labelArrow);
          if (myLabelArrowHead) {
            const labelArrowName = (leftToRight ?
              symmetric && myLabelArrowHead || 'none' :
              myLabelArrowHead || 'triangle,5') + ',' + color;
            const labelArrowSplit = labelArrowName.split(',');
            arrowAtLabelAdjust = labelArrowSplit[0] !== 'none' && parseInt(labelArrowSplit[1], 10) || 0;
            if (ufoCatcher) {
              arrowAtLabelAdjust = -arrowAtLabelAdjust;
            }
          }
          const arrowStart = textStart - arrowAtLabelAdjust;
          path = this.svg.createPath().move(arrowStart, -height);
          if (rowIndex === leftRow) {
            let cornerx = from + (this.rtlmode ? -1 : 1) * ufoCatcherMod * this.arcSlant;
            if (this.rtlmode) {
              if (!ufoCatcher && cornerx < arrowStart + 1) {
                cornerx = arrowStart + 1;
              }
            } else {
              if (!ufoCatcher && cornerx > arrowStart - 1) {
                cornerx = arrowStart - 1;
              }
            }

            if (this.smoothArcCurves) {
              let controlx;
              let endy;
              if (this.rtlmode) {
                controlx = ufoCatcher ? cornerx - 2 * ufoCatcherMod * this.reverseArcControlx : this.smoothArcSteepness * from + (1 - this.smoothArcSteepness) * cornerx;
                endy = leftBox.y + (leftToRight && !arc.equiv ? Configuration.visual.margin.y : leftBox.height / 2);
              } else {
                controlx = ufoCatcher ? cornerx + 2 * ufoCatcherMod * this.reverseArcControlx : this.smoothArcSteepness * from + (1 - this.smoothArcSteepness) * cornerx;
                endy = leftBox.y + (leftToRight || arc.equiv ? leftBox.height / 2 : Configuration.visual.margin.y);
              }

              // no curving for short lines covering short vertical
              // distances, the arrowheads can go off (#925)
              if (Math.abs(-height - endy) < 2 &&
                Math.abs(cornerx - from) < 5) {
                endy = -height;
              }
              path.line(cornerx, -height).curveQ(controlx, -height, from, endy);
            } else {
              path.line(cornerx, -height).line(from, leftBox.y + (leftToRight || arc.equiv ? leftBox.height / 2 : Configuration.visual.margin.y));
            }
          } else {
            path.line(from, -height);
          }

          this.svg.path(arcGroup, path, {
            markerEnd: arrowDecl,
            markerStart: labelArrowDecl,
            style: 'stroke: ' + color,
            'strokeDashArray': dashArray,
          });

          if (arc.marked) {
            this.svg.path(shadowGroup, path, {
              'class': 'shadow_EditHighlight_arc',
              strokeWidth: this.markedArcStroke,
              'strokeDashArray': dashArray,
            });
          }

          if (arc.shadowClass) {
            this.svg.path(shadowGroup, path, {
              'class': 'shadow_' + arc.shadowClass,
              strokeWidth: this.shadowStroke,
              'strokeDashArray': dashArray,
            });
          }

          if (!symmetric) {
            myArrowHead = (arcDesc && arcDesc.arrowHead);
            arrowName = (leftToRight ?
              myArrowHead || 'triangle,5' :
              'none') + ',' + color;
          }

          arrowType = arrows[arrowName];
          arrowDecl = arrowType && ('url(#' + arrowType + ')');

          const arrowEnd = textEnd + arrowAtLabelAdjust;
          path = this.svg.createPath().move(arrowEnd, -height);
          if (rowIndex === rightRow) {
            let cornerx = to - (this.rtlmode ? -1 : 1) * ufoCatcherMod * this.arcSlant;

            // TODO: duplicates above in part, make funcs
            // for normal cases, should not be past textEnd even if narrow
            if (this.rtlmode) {
              if (!ufoCatcher && cornerx > arrowEnd - 1) {
                cornerx = arrowEnd - 1;
              }
            } else {
              if (!ufoCatcher && cornerx < arrowEnd + 1) {
                cornerx = arrowEnd + 1;
              }
            }
            if (this.smoothArcCurves) {
              let controlx;
              let endy;
              if (this.rtlmode) {
                controlx = ufoCatcher ? cornerx - 2 * ufoCatcherMod * this.reverseArcControlx : this.smoothArcSteepness * to + (1 - this.smoothArcSteepness) * cornerx;
                endy = rightBox.y + (leftToRight && !arc.equiv ? Configuration.visual.margin.y : rightBox.height / 2);
              } else {
                controlx = ufoCatcher ? cornerx - 2 * ufoCatcherMod * this.reverseArcControlx : this.smoothArcSteepness * to + (1 - this.smoothArcSteepness) * cornerx;
                endy = rightBox.y + (leftToRight && !arc.equiv ? Configuration.visual.margin.y : rightBox.height / 2);
              }

              // no curving for short lines covering short vertical
              // distances, the arrowheads can go off (#925)
              if (Math.abs(-height - endy) < 2 &&
                Math.abs(cornerx - to) < 5) {
                endy = -height;
              }
              path.line(cornerx, -height).curveQ(controlx, -height, to, endy);
            } else {
              path.line(cornerx, -height).line(to, rightBox.y + (leftToRight && !arc.equiv ? Configuration.visual.margin.y : rightBox.height / 2));
            }
          } else {
            path.line(to, -height);
          }

          this.svg.path(arcGroup, path, {
            markerEnd: arrowDecl,
            markerStart: labelArrowDecl,
            style: 'stroke: ' + color,
            'strokeDashArray': dashArray,
          });

          if (arc.marked) {
            this.svg.path(shadowGroup, path, {
              'class': 'shadow_EditHighlight_arc',
              strokeWidth: this.markedArcStroke,
              'strokeDashArray': dashArray,
            });
          }

          if (shadowGroup) {
            this.svg.path(shadowGroup, path, {
              'class': 'shadow_' + arc.shadowClass,
              strokeWidth: this.shadowStroke,
              'strokeDashArray': dashArray,
            });
          }
        } // arc rows
      }); // arcs
    }

    renderFragmentConnectors(rows) {
      $.each(this.data.spans, (spanNo, span) => {
        const numConnectors = span.fragments.length - 1;
        for (let connectorNo = 0; connectorNo < numConnectors; connectorNo++) {
          const left = span.fragments[connectorNo];
          const right = span.fragments[connectorNo + 1];

          const leftBox = this.rowBBox(left);
          const rightBox = this.rowBBox(right);
          const leftRow = left.chunk.row.index;
          const rightRow = right.chunk.row.index;

          for (let rowIndex = leftRow; rowIndex <= rightRow; rowIndex++) {
            const row = rows[rowIndex];
            if (row.chunks.length) {
              row.hasAnnotations = true;

              let from;
              if (rowIndex === leftRow) {
                from = this.rtlmode ? leftBox.x : leftBox.x + leftBox.width;
              } else {
                from = this.rtlmode ? this.canvasWidth - 2 * Configuration.visual.margin.y - this.sentNumMargin : this.sentNumMargin;
              }

              let to;
              if (rowIndex === rightRow) {
                to = this.rtlmode ? rightBox.x + rightBox.width : rightBox.x;
              } else {
                to = this.rtlmode ? 0 : this.canvasWidth - 2 * Configuration.visual.margin.y;
              }

              let height = leftBox.y + leftBox.height - Configuration.visual.margin.y;
              if (this.roundCoordinates) {
                // don't ask
                height = (height | 0) + 0.5;
              }

              const path = this.svg.createPath().move(from, height).line(to, height);
              this.svg.path(row.arcs, path, {
                style: 'stroke: ' + this.fragmentConnectorColor,
                'strokeDashArray': this.fragmentConnectorDashArray
              });
            }
          } // rowIndex
        } // connectorNo
      }); // spans
    }

    /**
     * @param {number} y
     * @param {SVGElement} textGroup
     * @param {number} maxTextWidth
     * @return {number} how much the actual horizontal space required extends over the allocated space
     */
    renderResizeSvg(y, textGroup, maxTextWidth) {
      // resize the SVG
      let width = maxTextWidth + this.sentNumMargin + 2 * Configuration.visual.margin.x + 1;

      // Loops over the rows to check if the width calculated so far is still not enough. This
      // currently happens sometimes if there is a single annotation on many words preventing
      // wrapping within the annotation (aka oversizing).
      $(textGroup).children(".text-row").each((rowIndex, textRow) => {
        // const rowInitialSpacing = $($(textRow).children('.row-initial')[0]);
        const rowFinalSpacing = $($(textRow).children('.row-final')[0]);
        const lastChunkWidth = this.data.sizes.texts.widths[rowFinalSpacing.prev()[0].textContent];
        const lastChunkOffset = parseFloat(rowFinalSpacing.prev()[0].getAttribute('x'));
        if (this.rtlmode) {
          // Not sure what to calculate here
        } else {
          if (lastChunkOffset + lastChunkWidth > width) {
            width = lastChunkOffset + lastChunkWidth;
          }
        }
      });

      let oversized = Math.max(width - this.canvasWidth, 0);
      if (oversized > 0) {
        this.$svgDiv.width(this.baseCanvasWidth);
        this.canvasWidth = width;
        // Allow some extra space for arcs
        this.canvasWidth += 32;
        oversized += 32;
      }

      this.$svg.width(this.canvasWidth);
      Util.profileStart('height');
      this.$svg.height(y);
      Util.profileEnd('height');
      this.$svg.attr("viewBox", "0 0 " + this.canvasWidth + " " + y);

      // Originally, this code was within the oversized > 0 block above, but we moved it here
      // to prevent erratic jumping
      this.$svgDiv.height(y + 4); // Need to take the hairline border into account here

      return oversized;
    }

    /**
     * @param rows
     * @param {SVGElement} backgroundGroup
     * @return {[number, SVGElement]}
     */
    renderRows(rows, backgroundGroup) {
      // position the rows
      let y = Configuration.visual.margin.y;
      const sentNumGroup = this.svg.group({'class': 'sentnum unselectable'});
      let currentSent;
      $.each(rows, (rowId, row) => {
        // find the maximum fragment height
        $.each(row.chunks, function (chunkId, chunk) {
          $.each(chunk.fragments, function (fragmentId, fragment) {
            if (row.maxSpanHeight < fragment.height)
              row.maxSpanHeight = fragment.height;
          });
        });
        if (row.sentence) {
          currentSent = row.sentence;
        }
        // SLOW (#724) and replaced with calculations:
        //
        // var rowBox = row.group.getBBox();
        // // Make it work on IE
        // rowBox = { x: rowBox.x, y: rowBox.y, height: rowBox.height, width: rowBox.width };
        // // Make it work on Firefox and Opera
        // if (rowBox.height == -Infinity) {
        //   rowBox = { x: 0, y: 0, height: 0, width: 0 };
        // }
        // XXX TODO HACK: find out where 5 and 1.5 come from!
        // This is the fix for #724, but the numbers are guessed.
        let rowBoxHeight = Math.max(row.maxArcHeight + 5, row.maxSpanHeight + 1.5); // XXX TODO HACK: why 5, 1.5?
        if (row.hasAnnotations) {
          // rowBox.height = -rowBox.y + rowSpacing;
          rowBoxHeight += this.rowSpacing + 1.5; // XXX TODO HACK: why 1.5?
        } else {
          rowBoxHeight -= 5; // XXX TODO HACK: why -5?
        }

        rowBoxHeight += this.rowPadding;

        let bgClass;
        if (Configuration.textBackgrounds === "striped") {
          // give every other sentence a different bg class
          bgClass = 'background' + row.backgroundIndex;
        } else {
          // plain "standard" bg
          bgClass = 'background0';
        }

        let sizes = this.data.sizes;
        this.svg.rect(backgroundGroup,
          0, y + sizes.texts.y + sizes.texts.height,
          this.canvasWidth, rowBoxHeight + sizes.texts.height + 1, {
            'class': bgClass,
          });

        if (row.sentence && this.data.markedSent[currentSent]) {
          if (this.rtlmode) {
            this.svg.rect(backgroundGroup,
              this.canvasWidth - this.sentNumMargin,
              y + sizes.texts.y + sizes.texts.height,
              this.sentNumMargin,
              rowBoxHeight + sizes.texts.height + 1,
              {'class': 'backgroundHighlight'});
          } else {
            this.svg.rect(backgroundGroup,
              0,
              y + sizes.texts.y + sizes.texts.height,
              this.sentNumMargin,
              rowBoxHeight + sizes.texts.height + 1,
              {'class': 'backgroundHighlight'});
          }
        }

        y += rowBoxHeight;
        y += sizes.texts.height;
        row.textY = y - this.rowPadding;
        if (row.sentence) {
          const link = sentNumGroup;

          // Render sentence number as link
          {
            let text;
            if (this.rtlmode) {
              text = this.svg.text(link, this.canvasWidth - this.sentNumMargin + Configuration.visual.margin.x, y - this.rowPadding,
                '' + row.sentence, {'data-sent': row.sentence});
            } else {
              text = this.svg.text(link, this.sentNumMargin - Configuration.visual.margin.x, y - this.rowPadding,
                '' + row.sentence, {'data-sent': row.sentence});
            }
            $(text).css('cursor', 'pointer');
          }

          const sentComment = this.data.sentComment[row.sentence];
          if (sentComment) {
            const box = text.getBBox();
            this.svg.remove(text);
            // TODO: using rectShadowSize, but this shadow should
            // probably have its own setting for shadow size
            this.svg.rect(sentNumGroup,
              this.rtlmode ? box.x + this.rowPadding + this.rectShadowSize : box.x - this.rectShadowSize,
              box.y - this.rectShadowSize,
              box.width + 2 * this.rectShadowSize,
              box.height + 2 * this.rectShadowSize,
              {
                'class': 'shadow_' + sentComment.type,
                filter: 'url(#Gaussian_Blur)',
                rx: this.rectShadowRounding,
                ry: this.rectShadowRounding,
                'data-sent': row.sentence,
              });

            {
              let text;
              if (this.rtlmode) {
                text = this.svg.text(sentNumGroup, this.canvasWidth - this.sentNumMargin + Configuration.visual.margin.x, y - this.rowPadding,
                  '' + row.sentence, {'data-sent': row.sentence});
              } else {
                text = this.svg.text(sentNumGroup, this.sentNumMargin - Configuration.visual.margin.x, y - this.rowPadding,
                  '' + row.sentence, {'data-sent': row.sentence});
              }
              $(text).css('cursor', 'pointer');
            }
          }
        }

        let rowY = y - this.rowPadding;
        if (this.roundCoordinates) {
          rowY = rowY | 0;
        }
        this.translate(row, 0, rowY);
        y += Configuration.visual.margin.y;
      });
      y += Configuration.visual.margin.y;

      return [y, sentNumGroup];
    }

    renderAdjustLayoutForRtl(oversized, rows, glowGroup, textGroup, sentNumGroup) {
      this.$svg.attr("direction", "rtl");
      if (oversized > 0) {
        $.each(rows, (index, row) => this.translate(row, oversized, row.translation.y));
        $(glowGroup).attr('transform', 'translate(' + oversized + ', ' + 0 + ')');
        $(this.highlightGroup).attr('transform', 'translate(' + oversized + ', ' + 0 + ')');
        $(textGroup).attr('transform', 'translate(' + oversized + ', ' + 0 + ')');
        $(sentNumGroup).attr('transform', 'translate(' + oversized + ', ' + 0 + ')');
        const scrollable = findClosestHorizontalScrollable(this.$svgDiv);
        if (scrollable) {
          scrollable.scrollLeft(oversized + 4);
        }
      }
    }

    /**
     * @param {number} oversized
     * @param backgroundGroup
     */
    renderAdjustLayoutForOversize(oversized, backgroundGroup) {
      if (oversized <= 0) {
        return;
      }

      // Allow some extra space for arcs
      $(backgroundGroup).attr('width', this.canvasWidth);
      $(backgroundGroup).children().each((index, element) => {
        // We render the backgroundHighlight only in the margin, so we have to translate
        // it instead of transforming it.
        if ($(element).attr('class') === 'backgroundHighlight') {
          if (this.rtlmode) {
            $(element).attr('transform', 'translate(' + oversized + ', ' + 0 + ')');
          }
        } else {
          $(element).attr('width', this.canvasWidth);
        }
      });
    }

    renderAdjustLayoutRowSpacing(textGroup) {
      // Go through each row and adjust the row-initial and row-final spacing
      $(textGroup).children(".text-row").each((rowIndex, textRow) => {
        const rowInitialSpacing = $($(textRow).children('.row-initial')[0]);
        const rowFinalSpacing = $($(textRow).children('.row-final')[0]);
        const firstChunkWidth = this.data.sizes.texts.widths[rowInitialSpacing.next()[0].textContent];
        const lastChunkWidth = this.data.sizes.texts.widths[rowFinalSpacing.prev()[0].textContent];
        const lastChunkOffset = parseFloat(rowFinalSpacing.prev()[0].getAttribute('x'));

        if (this.rtlmode) {
          const initialSpacingWidth = Configuration.visual.margin.x + this.rowPadding + 1;
          const initialSpacingX = this.canvasWidth - this.sentNumMargin - initialSpacingWidth;
          rowInitialSpacing.attr('x', initialSpacingX);
          rowInitialSpacing.attr('textLength', initialSpacingWidth);

          const finalSpacingX = lastChunkOffset - lastChunkWidth;
          const finalSpacingWidth = finalSpacingX;
          rowFinalSpacing.attr('x', finalSpacingX);
          rowFinalSpacing.attr('textLength', finalSpacingWidth);
        } else {
          const initialSpacingX = this.sentNumMargin;
          const initialSpacingWidth = Configuration.visual.margin.x + this.rowPadding + 1;
          rowInitialSpacing.attr('x', initialSpacingX);
          rowInitialSpacing.attr('textLength', initialSpacingWidth);

          const finalSpacingX = lastChunkOffset + lastChunkWidth + 1;
          const finalSpacingWidth = this.canvasWidth - finalSpacingX;
          rowFinalSpacing.attr('x', finalSpacingX);
          rowFinalSpacing.attr('textLength', finalSpacingWidth);
        }
      });
    }

    renderAdjustLayoutInterRowSpacers(y, textGroup) {
      // Go through each row and add an unselectable spacer between this row and the next row
      // While the space is unselectable, it will still help in guiding the browser into which
      // direction the selection should in principle go and thus avoids jumpyness.
      let prevRowRect = {y: 0, height: 0};

      const textRows = $(textGroup).children(".text-row");
      textRows.each((rowIndex, textRow) => {
        const rowRect = {
          y: parseFloat($(textRow).children()[0].getAttribute('y')) + 2, height: this.data.sizes.texts.height
        };
        const spaceHeight = rowRect.y - (prevRowRect.y + rowRect.height) + 2;

        // Adding a spacer between the rows. We make is a *little* bit larger than necessary
        // to avoid exposing areas where the background shines through and which would again
        // cause jumpyness during selection.
        textRow.before(this.verticalSpacer(
          Math.floor(prevRowRect.y),
          Math.ceil(spaceHeight)));

        prevRowRect = rowRect;

        // Add a spacer below the final row until the end of the canvas
        if (rowIndex === textRows.length - 1) {
          const lastSpacerY = Math.floor(rowRect.y + rowRect.height);
          textRow.after(this.verticalSpacer(
            Math.floor(rowRect.y + rowRect.height),
            Math.ceil(y - lastSpacerY) + 1));
        }
      });
    }

    /**
     * @param {SourceData} sourceData
     */
    renderDataReal(sourceData) {
      Util.profileEnd('before render');
      Util.profileStart('render');

      Util.profileStart('init');
      if (!sourceData && !this.data) {
        this.dispatcher.post('doneRendering', [this.coll, this.doc, this.args]);
        return;
      }

      this.$svgDiv.show();
      if ((this.sourceData && this.sourceData.collection && (this.sourceData.document !== this.doc || this.sourceData.collection !== this.coll)) || this.drawing) {
        this.redraw = true;
        this.dispatcher.post('doneRendering', [this.coll, this.doc, this.args]);
        return;
      }

      this.redraw = false;
      this.drawing = true;

      if (sourceData) {
        this.setData(sourceData);
      }

      this.svg.clear(true);

      if (!this.data) {
        return;
      }

      this.$svg.css("font-size", this.fontZoom + "%");
      this.sentNumMargin = 40 * (this.fontZoom / 100.0);

      // establish the width according to the enclosing element
      this.baseCanvasWidth = this.forceWidth || this.$svgDiv.width();
      this.canvasWidth = this.forceWidth || (this.$svgDiv.width() - $.scrollbarWidth());

      // Take hairline border of SVG into account
      this.canvasWidth -= 4;

      const defs = this.addHeaderAndDefs();
      const backgroundGroup = this.svg.group({'class': 'background', 'pointer-events': 'none'});
      const glowGroup = this.svg.group({'class': 'glow'});
      this.highlightGroup = this.svg.group({'class': 'highlight'});
      const textGroup = this.svg.group({'class': 'text'});
      Util.profileEnd('init');

      Util.profileStart('measures');
      this.data.sizes = this.calculateTextMeasurements();
      this.adjustTowerAnnotationSizes();
      let maxTextWidth = this.calculateMaxTextWidth(this.data.sizes);
      Util.profileEnd('measures');

      Util.profileStart('chunks');
      this.renderLayoutFloorsAndCurlies(this.data.spanDrawOrderPermutation);
      let rows, fragmentHeights, textMarkedRows;
      [rows, fragmentHeights, textMarkedRows] = this.renderChunks(sourceData, this.data.chunks, maxTextWidth);
      Util.profileEnd('chunks');

      Util.profileStart('arcsPrep');
      this.renderBumpFragmentHeightsMinimumToArcStartHeight(fragmentHeights);
      this.renderCalculateArcJumpHeight(fragmentHeights);
      this.renderSortArcs();
      this.renderAssignFragmentsToRows(rows, fragmentHeights);
      this.renderDragArcMarker(defs);
      Util.profileEnd('arcsPrep');

      Util.profileStart('arcs');
      this.renderArcs(rows, fragmentHeights, defs);
      Util.profileEnd('arcs');

      Util.profileStart('fragmentConnectors');
      this.renderFragmentConnectors(rows);
      Util.profileEnd('fragmentConnectors');

      Util.profileStart('rows');
      let sentNumGroup, y;
      [y, sentNumGroup] = this.renderRows(rows, backgroundGroup);
      Util.profileEnd('rows');

      Util.profileStart('chunkFinish');
      this.renderChunksPass2(textGroup, textMarkedRows);
      Util.profileEnd('chunkFinish');

      Util.profileStart('finish');

      Util.profileStart('adjust margin');
      if (this.rtlmode) {
        this.svg.path(sentNumGroup, this.svg.createPath().move(this.canvasWidth - this.sentNumMargin, 0).line(this.canvasWidth - this.sentNumMargin, y));
      } else {
        this.svg.path(sentNumGroup, this.svg.createPath().move(this.sentNumMargin, 0).line(this.sentNumMargin, y));
      }
      Util.profileEnd('adjust margin');

      Util.profileStart('resize SVG');
      let oversized = this.renderResizeSvg(y, textGroup, maxTextWidth);
      Util.profileEnd('resize SVG');

      if (this.rtlmode) {
        Util.profileStart('set up RTL');
        this.renderAdjustLayoutForRtl(oversized, rows, glowGroup, textGroup, sentNumGroup);
        Util.profileEnd('set up RTL');
      }

      Util.profileStart('adjust backgrounds');
      this.renderAdjustLayoutForOversize(oversized, backgroundGroup);
      Util.profileEnd('adjust backgrounds');

      Util.profileStart('row-spacing-adjust');
      this.renderAdjustLayoutRowSpacing(textGroup);
      Util.profileEnd('row-spacing-adjust');

      Util.profileStart('inter-row space');
      this.renderAdjustLayoutInterRowSpacers(y, textGroup);
      Util.profileEnd('inter-row space');

      Util.profileEnd('finish');
      Util.profileEnd('render');
      Util.profileReport();

      this.drawing = false;
      if (this.redraw) {
        this.redraw = false;
      }

      this.dispatcher.post('doneRendering', [this.coll, this.doc, this.args]);
    }

    /**
     * @param {SourceData} sourceData
     */
    renderData(sourceData) {
      Util.profileEnd('invoke getDocument');

      // Fill in default values that don't necessarily go over the protocol
      if (sourceData) {
        setSourceDataDefaults(sourceData);
      }

      this.dispatcher.post('startedRendering', [this.coll, this.doc, this.args]);
      this.dispatcher.post('spin');

      setTimeout(() => {
        try {
          this.renderDataReal(sourceData);
        } catch (e) {
          // We are sure not to be drawing anymore, reset the state
          this.drawing = false;
          console.error('Rendering terminated due to: ' + e, e.stack);
          this.dispatcher.post('renderError: Fatal', [sourceData, e]);
        }

        this.dispatcher.post('unspin');
      }, 0);
    }

    /**
     * Re-render using the known source data. This is necessary if the source data has changed e.g.
     * due to a partial update from the server.
     */
    rerender() {
      this.dispatcher.post('startedRendering', [this.coll, this.doc, this.args]);
      this.dispatcher.post('spin');

      try {
        this.renderDataReal(this.sourceData);
      } catch (e) {
        // We are sure not to be drawing anymore, reset the state
        this.drawing = false;
        // TODO: Hook printout into dispatch elsewhere?
        console.warn('Rendering terminated due to: ' + e, e.stack);
        this.dispatcher.post('renderError: Fatal', [this.sourceData, e]);
      }

      this.dispatcher.post('unspin');
    }

    /**
     * Differential updates for brat view.
     *
     * @param {*} patchData
     */
    renderDataPatch(patchData) {
      Util.profileEnd('invoke getDocument');
      this.sourceData = jsonpatch.applyPatch(this.sourceData, patchData).newDocument;
      this.rerender();
    }

    renderDocument() {
      Util.profileStart('invoke getDocument');
      this.dispatcher.post('ajax', [{
        action: 'getDocument',
        collection: this.coll,
        'document': this.doc,
      }, 'renderData', {
        collection: this.coll,
        'document': this.doc
      }]);
    }

    triggerRender() {
      if (this.svg && ((this.isRenderRequested && this.isCollectionLoaded) || this.requestedData)) {
        this.isRenderRequested = false;

        if (this.requestedData) {
          Util.profileClear();
          Util.profileStart('before render');
          this.renderData(this.requestedData);
          return;
        }

        if (this.doc.length) {
          Util.profileClear();
          Util.profileStart('before render');
          this.renderDocument();
          return;
        }

        this.dispatcher.post(0, 'renderError:noFileSpecified');
      }
    }

    requestRenderData(sourceData) {
      this.requestedData = sourceData;
      this.triggerRender();
    }

    collectionChanged() {
      this.isCollectionLoaded = false;
    }

    gotCurrent(coll, doc, args, reloadData) {
      this.coll = coll;
      this.doc = doc;
      this.args = args;

      if (reloadData) {
        this.isRenderRequested = true;
        this.triggerRender();
      }
    }

    /**
     * @param {MouseEvent} evt
     */
    onMouseOverSpan(evt) {
      const target = $(evt.target);
      let id = target.attr('data-span-id');
      this.commentId = id;
      const span = this.data.spans[id];
      this.dispatcher.post('displaySpanComment', [
        evt, target, id, span.type, span.attributeText,
        span.text,
        span.hovertext,
        span.comment && span.comment.text,
        span.comment && span.comment.type,
        span.normalizations
      ]);

      if (span.actionButtons) {
        this.dispatcher.post('displaySpanButtons', [evt, target, span.id]);
      }

      if (span.hidden)
        return;

      const spanDesc = this.spanTypes[span.type];
      let bgColor = span.color || ((spanDesc && spanDesc.bgColor) || '#ffffff');

      this.highlight = [];
      $.each(span.fragments, (fragmentNo, fragment) => {
        this.highlight.push(this.svg.rect(this.highlightGroup,
          fragment.highlightPos.x, fragment.highlightPos.y,
          fragment.highlightPos.w, fragment.highlightPos.h,
          {
            'fill': bgColor, opacity: 0.75,
            rx: this.highlightRounding.x,
            ry: this.highlightRounding.y,
          }));
      });

      if (this.arcDragOrigin) {
        target.parent().addClass('highlight');
        return;
      }

      /** @type {Object.<string, boolean>} */ const equivs = {};
      /** @type {Object.<string, boolean>} */ const spans = {};
      spans[id] = true;
      // find all arcs, normal and equiv. Equivs need to go far (#1023)
      const addArcAndSpan = (arc, span) => {
        if (arc.equiv) {
          equivs[arc.eventDescId.substr(0, arc.eventDescId.indexOf('*', 2) + 1)] = true;
          const eventDesc = this.data.eventDescs[arc.eventDescId];
          $.each(eventDesc.leftSpans.concat(eventDesc.rightSpans), (ospanId, ospan) => spans[ospan] = true);
        } else {
          spans[arc.origin] = true;
        }
      };
      span.incoming.map(arc => addArcAndSpan(arc, arc.origin));
      span.outgoing.map(arc => addArcAndSpan(arc, arc.target));

      /** @type {string[]} */ const equivSelector = [];
      Object.keys(equivs).map(equiv => equivSelector.push('[data-arc-ed^="' + equiv + '"]'));
      this.highlightArcs = this.$svg.find(equivSelector.join(', ')).parent()
        .add('g[data-from="' + id + '"], g[data-to="' + id + '"]' + equivSelector, this.$svg)
        .addClass('highlight');

      /** @type {string[]} */ const spanIds = [];
      Object.keys(spans).map(spanId => spanIds.push('rect[data-span-id="' + spanId + '"]'));
      this.highlightSpans = this.$svg.find(spanIds.join(', ')).parent().addClass('highlight');
    }

    /**
     * @param {MouseEvent} evt
     */
    onMouseOverArc(evt) {
      const target = $(evt.target);
      const originSpanId = target.attr('data-arc-origin');
      const targetSpanId = target.attr('data-arc-target');
      const role = target.attr('data-arc-role');
      const symmetric = (this.relationTypesHash &&
        this.relationTypesHash[role] &&
        this.relationTypesHash[role].properties &&
        this.relationTypesHash[role].properties.symmetric);

      // NOTE: no commentText, commentType for now
      /** @type {string} */ const arcEventDescId = target.attr('data-arc-ed');
      let commentText = '';
      let commentType = '';
      let arcId;

      if (arcEventDescId) {
        const eventDesc = this.data.eventDescs[arcEventDescId];
        let comment = eventDesc.comment;
        if (comment) {
          commentText = comment.text;
          commentType = comment.type;
          if (commentText === '' && commentType) {
            // default to type if missing text
            commentText = commentType;
          }
        }
        if (eventDesc.relation) {
          // among arcs, only ones corresponding to relations have "independent" IDs
          arcId = arcEventDescId;
        }
      }

      const originSpanType = this.data.spans[originSpanId].type || '';
      const targetSpanType = this.data.spans[targetSpanId].type || '';
      let normalizations = [];
      if (arcId) {
        normalizations = this.data.arcById[arcId].normalizations;
      }

      this.dispatcher.post('displayArcComment', [
        evt, target, symmetric, arcId,
        originSpanId, originSpanType, role,
        targetSpanId, targetSpanType,
        commentText, commentType, normalizations
      ]);

      if (arcId) {
        this.highlightArcs = this.$svg.find('g[data-id="' + arcId + '"]').addClass('highlight');
      } else {
        this.highlightArcs = this.$svg.find('g[data-from="' + originSpanId + '"][data-to="' + targetSpanId + '"]').addClass('highlight');
      }

      this.highlightSpans = $(this.$svg).find('rect[data-span-id="' + originSpanId + '"], rect[data-span-id="' + targetSpanId + '"]').parent().addClass('highlight');
    }

    /**
     * @param {MouseEvent} evt
     */
    onMouseOverSentence(evt) {
      const target = $(evt.target);
      let id = target.attr('data-span-id');
      let comment = this.data.sentComment[id];
      if (comment) {
        this.dispatcher.post('displaySentComment', [evt, target, comment.text, comment.type]);
      }
    }

    /**
     * @param {MouseEvent} evt
     */
    onMouseOver(evt) {
      const target = $(evt.target);
      if (target.attr('data-span-id')) {
        this.onMouseOverSpan(evt);
        return;
      }

      if (!this.arcDragOrigin && target.attr('data-arc-role')) {
        this.onMouseOverArc(evt);
        return;
      }

      if (target.attr('data-sent')) {
        this.onMouseOverSentence(evt);
      }
    }

    onMouseOut(evt) {
      const target = $(evt.target);
      target.removeClass('badTarget');
      this.dispatcher.post('hideComment');

      if (this.highlight) {
        this.highlight.map((h) => this.svg.remove(h));
        this.highlight = undefined;
      }

      if (this.highlightSpans) {
        this.highlightArcs.removeClass('highlight');
        this.highlightSpans.removeClass('highlight');
        this.highlightSpans = undefined;
      }
    }

    setAbbrevs(_abbrevsOn) {
      // TODO: this is a slightly weird place to tweak the configuration
      Configuration.abbrevsOn = _abbrevsOn;
      this.dispatcher.post('configurationChanged');
    }

    setTextBackgrounds(_textBackgrounds) {
      Configuration.textBackgrounds = _textBackgrounds;
      this.dispatcher.post('configurationChanged');
    }

    setLayoutDensity(_density) {
      //dispatcher.post('messages', [[['Setting layout density ' + _density, 'comment']]]);
      // TODO: store standard settings instead of hard-coding
      // them here (again)
      if (_density < 2) {
        // dense
        Configuration.visual.margin = {x: 1, y: 0};
        Configuration.visual.boxSpacing = 1;
        Configuration.visual.curlyHeight = 1;
        Configuration.visual.arcSpacing = 7;
        Configuration.visual.arcStartHeight = 18;
      } else if (_density > 2) {
        // spacious
        Configuration.visual.margin = {x: 2, y: 1};
        Configuration.visual.boxSpacing = 3;
        Configuration.visual.curlyHeight = 6;
        Configuration.visual.arcSpacing = 12;
        Configuration.visual.arcStartHeight = 23;
      } else {
        // standard
        Configuration.visual.margin = {x: 2, y: 1};
        Configuration.visual.boxSpacing = 1;
        Configuration.visual.curlyHeight = 4;
        Configuration.visual.arcSpacing = 9;
        Configuration.visual.arcStartHeight = 19;
      }
      this.dispatcher.post('configurationChanged');
    }

    setSvgWidth(_width) {
      this.$svgDiv.width(_width);
      if (Configuration.svgWidth !== _width) {
        Configuration.svgWidth = _width;
        this.dispatcher.post('configurationChanged');
      }
    }

    // register event listeners
    registerHandlers(element, events) {
      $.each(events, (eventNo, eventName) => {
        element.bind(eventName, (evt) => this.dispatcher.post(eventName, [evt], 'all'));
      });
    }

    loadSpanTypes(types) {
      $.each(types, (typeNo, type) => {
        if (type) {
          this.spanTypes[type.type] = type;
          const children = type.children;
          if (children && children.length) {
            this.loadSpanTypes(children);
          }
        }
      });
    }

    loadAttributeTypes(response_types) {
      const processed = {};
      $.each(response_types, (aTypeNo, aType) => {
        processed[aType.type] = aType;
        // count the values; if only one, it's a boolean attribute
        const values = [];
        for (const i in aType.values) {
          if (aType.values.hasOwnProperty(i)) {
            values.push(i);
          }
        }
        if (values.length === 1) {
          aType.bool = values[0];
        }
      });
      return processed;
    }

    loadRelationTypes(relation_types) {
      $.each(relation_types, (relTypeNo, relType) => {
        if (relType) {
          this.relationTypesHash[relType.type] = relType;
          const children = relType.children;
          if (children && children.length) {
            this.loadRelationTypes(children);
          }
        }
      });
    }

    /**
     * @param {CollectionLoadedResponse} response
     */
    collectionLoaded(response) {
      setCollectionDefaults(response);
      this.eventAttributeTypes = this.loadAttributeTypes(response.event_attribute_types);
      this.entityAttributeTypes = this.loadAttributeTypes(response.entity_attribute_types);

      this.spanTypes = {};
      this.loadSpanTypes(response.entity_types);
      this.loadSpanTypes(response.event_types);
      this.loadSpanTypes(response.unconfigured_types);

      this.relationTypesHash = {};
      this.loadRelationTypes(response.relation_types);
      this.loadRelationTypes(response.unconfigured_types);

      // TODO XXX: isn't the following completely redundant with
      // loadRelationTypes?
      $.each(response.relation_types, (relTypeNo, relType) => this.relationTypesHash[relType.type] = relType);
      const arcBundle = (response.visual_options || {}).arc_bundle || 'none';
      this.collapseArcs = arcBundle === "all";
      this.collapseArcSpace = arcBundle !== "none";

      this.dispatcher.post('spanAndAttributeTypesLoaded', [
        this.spanTypes, this.entityAttributeTypes, this.eventAttributeTypes, this.relationTypesHash]);

      this.isCollectionLoaded = true;
      this.triggerRender();
    }

    isReloadOkay() {
      // do not reload while the user is in the dialog
      return !this.drawing;
    }

    verticalSpacer(y, height) {
      if (height > 0) {
        const foreignObject = document.createElementNS('http://www.w3.org/2000/svg', 'foreignObject');
        const spacer = document.createElement('span');
        $(spacer)
          .addClass('unselectable')
          .css('display', 'inline-block')
          .css('width', '100%')
          .css('height', '100%')
          .text('\u00a0');
        $(foreignObject)
          .attr("x", this.rtlmode ? 0 : this.sentNumMargin)
          .attr("y", y)
          .attr("width", this.rtlmode ? this.canvasWidth - this.sentNumMargin : this.canvasWidth)
          .attr("height", height)
          .append(spacer);
        return foreignObject;
      }
    }

    horizontalSpacer(svg, group, x, y, width, attrs) {
      if (width > 0) {
        const attributes = $.extend({
          textLength: width,
          lengthAdjust: 'spacingAndGlyphs',
          'class': 'spacing'
        }, attrs);
        // To visualize the spacing use \u2588, otherwise \u00a0
        this.svg.text(group, x, y, this.rtlmode ? '\u200f\u00a0' : '\u00a0', attributes);
      }
    }

    renderArcShadow(arc, shadowGroup, textBox) {
      this.svg.rect(shadowGroup,
        textBox.x - this.arcLabelShadowSize,
        textBox.y - this.arcLabelShadowSize,
        textBox.width + 2 * this.arcLabelShadowSize,
        textBox.height + 2 * this.arcLabelShadowSize, {
          'class': 'shadow_' + arc.shadowClass,
          filter: 'url(#Gaussian_Blur)',
          rx: this.arcLabelShadowRounding,
          ry: this.arcLabelShadowRounding,
        });
    }

    /**
     * @param {number} bx
     * @param {number} by
     * @param {number} bw
     * @param {number} bh
     * @param {Chunk} chunk
     * @return {SVGElement}
     */
    renderSpanMarkedRect(bx, by, bw, bh, chunk) {
      return this.svg.rect(chunk.highlightGroup,
        bx - this.markedSpanSize, by - this.markedSpanSize,
        bw + 2 * this.markedSpanSize, bh + 2 * this.markedSpanSize, {
          filter: 'url(#Gaussian_Blur)',
          'class': "shadow_EditHighlight",
          rx: this.markedSpanSize,
          ry: this.markedSpanSize,
        });
    }

    /**
     * @param {number} bx
     * @param {number} by
     * @param {number} bw
     * @param {number} bh
     * @param {Fragment} fragment
     * @return {SVGElement}
     */
    renderFragmentShadowRect(bx, by, bw, bh, fragment) {
      const span = fragment.span;
      return this.svg.rect(fragment.group,
        bx - this.rectShadowSize, by - this.rectShadowSize,
        bw + 2 * this.rectShadowSize, bh + 2 * this.rectShadowSize, {
          'class': 'shadow_' + span.shadowClass,
          filter: 'url(#Gaussian_Blur)',
          rx: this.rectShadowRounding,
          ry: this.rectShadowRounding,
        });
    }

    /**
     * @param {number} bx
     * @param {number} by
     * @param {number} bw
     * @param {number} bh
     * @param {number} yy
     * @param {Fragment} fragment
     * @param rectClass
     * @param bgColor
     * @param borderColor
     * @return {SVGElement}
     */
    renderFragmentRect(bx, by, bw, bh, yy, fragment, rectClass, bgColor, borderColor) {
      const span = fragment.span;
      const bx1 = bx;
      const bx2 = bx1 + bw;
      const by1 = yy - Configuration.visual.margin.y - span.floor;
      const by2 = by1 + bh;
      const poly = [];

      if (span.clippedAtStart && span.fragments[0] === fragment) {
        poly.push([bx1, by2]);
        poly.push([bx1 - bh / 2, (by1 + by2) / 2]);
        poly.push([bx1, by1]);
      } else {
        poly.push([bx1, by2]);
        poly.push([bx1, by1]);
      }

      if (span.clippedAtEnd && span.fragments[span.fragments.length - 1] === fragment) {
        poly.push([bx2, by1]);
        poly.push([bx2 + bh / 2, (by1 + by2) / 2]);
        poly.push([bx2, by2]);
      } else {
        poly.push([bx2, by1]);
        poly.push([bx2, by2]);
      }

      return this.svg.polygon(fragment.group, poly, {
        'class': rectClass,
        fill: bgColor,
        stroke: borderColor,
        rx: Configuration.visual.margin.x,
        ry: Configuration.visual.margin.y,
        'data-span-id': span.id,
        'data-fragment-id': span.segmentedOffsetsMap[fragment.id],
        'strokeDashArray': span.attributeMerge.dashArray,
      });
    }

    renderFragmentCrossOut(xx, yy, hh, fragment) {
      const span = fragment.span;
      this.svg.path(fragment.group, svg.createPath()
          .move(xx, yy - Configuration.visual.margin.y - span.floor)
          .line(xx + fragment.width, yy + hh + Configuration.visual.margin.y - span.floor),
        {'class': 'boxcross'});
      this.svg.path(fragment.group, svg.createPath()
          .move(xx + fragment.width, yy - Configuration.visual.margin.y - span.floor)
          .line(xx, yy + hh + Configuration.visual.margin.y - span.floor),
        {'class': 'boxcross'});

    }

    renderCurly(fragment, x, yy, hh) {
      const span = fragment.span;
      let curlyColor = 'grey';
      if (this.coloredCurlies) {
        let spanDesc = this.spanTypes[span.type];
        let bgColor;
        if (span.color) {
          bgColor = span.color;
        }
        else {
          bgColor = ((spanDesc && spanDesc.bgColor) || '#000000');
        }

        curlyColor = Util.adjustColorLightness(bgColor, -0.6);
      }

      const bottom = yy + hh + Configuration.visual.margin.y - span.floor + 1;
      this.svg.path(fragment.group, this.svg.createPath()
          .move(fragment.curly.from, bottom + Configuration.visual.curlyHeight)
          .curveC(fragment.curly.from, bottom,
            x, bottom + Configuration.visual.curlyHeight,
            x, bottom)
          .curveC(x, bottom + Configuration.visual.curlyHeight,
            fragment.curly.to, bottom,
            fragment.curly.to, bottom + Configuration.visual.curlyHeight),
        {
          'class': 'curly',
          'stroke': curlyColor,
        });
    }
  }

  function isRTL(charCode) {
    const t1 = (0x0591 <= charCode && charCode <= 0x07FF);
    const t2 = (charCode === 0x200F);
    const t3 = (charCode === 0x202E);
    const t4 = (0xFB1D <= charCode && charCode <= 0xFDFD);
    const t5 = (0xFE70 <= charCode && charCode <= 0xFEFC);
    return t1 || t2 || t3 || t4 || t5;
  }

  // http://24ways.org/2010/calculating-color-contrast/
  // http://stackoverflow.com/questions/11867545/change-text-color-based-on-brightness-of-the-covered-background-area
  function bgToFgColor(hexcolor) {
    const r = parseInt(hexcolor.substr(1, 2), 16);
    const g = parseInt(hexcolor.substr(3, 2), 16);
    const b = parseInt(hexcolor.substr(5, 2), 16);
    const yiq = ((r * 299) + (g * 587) + (b * 114)) / 1000;
    return (yiq >= 128) ? '#000000' : '#ffffff';
  }

  // WEBANNO EXTENSION BEGIN - RTL - Need to find scrollable ancestor
  // https://stackoverflow.com/a/35940276/2511197
  function findClosestHorizontalScrollable(node) {
    if (node === null || node.is('html')) {
      return null;
    }

    if (
      (node.css('overflow-x') === 'auto' && node.prop('scrollWidth') > node.prop('clientWidth')) ||
      (node.css('overflow-x') === 'scroll')
    ) {
      return node;
    } else {
      return findClosestHorizontalScrollable(node.parent());
    }
  }
  // WEBANNO EXTENSION END - RTL - Need to find scrollable ancestor

  /**
   * A naive whitespace tokeniser
   * @returns {number[][]}
   */
  function tokenise(text) {
    const tokenOffsets = [];
    let tokenStart = null;
    let lastCharPos = null;

    for (let i = 0; i < text.length; i++) {
      const c = text[i];
      // Have we found the start of a token?
      if (tokenStart == null && !/\s/.test(c)) {
        tokenStart = i;
        lastCharPos = i;
        // Have we found the end of a token?
      } else if (/\s/.test(c) && tokenStart != null) {
        tokenOffsets.push([tokenStart, i]);
        tokenStart = null;
        // Is it a non-whitespace character?
      } else if (!/\s/.test(c)) {
        lastCharPos = i;
      }
    }
    // Do we have a trailing token?
    if (tokenStart != null) {
      tokenOffsets.push([tokenStart, lastCharPos + 1]);
    }

    return tokenOffsets;
  }

  /**
   * A naive newline sentence splitter
   *
   * @returns {number[][]}
   */
  function sentenceSplit(text) {
    const sentenceOffsets = [];
    let sentStart = null;
    let lastCharPos = null;

    for (let i = 0; i < text.length; i++) {
      const c = text[i];
      // Have we found the start of a sentence?
      if (sentStart == null && !/\s/.test(c)) {
        sentStart = i;
        lastCharPos = i;
        // Have we found the end of a sentence?
      } else if (c === '\n' && sentStart != null) {
        sentenceOffsets.push([sentStart, i]);
        sentStart = null;
        // Is it a non-whitespace character?
      } else if (!/\s/.test(c)) {
        lastCharPos = i;
      }
    }
    // Do we have a trailing sentence without a closing newline?
    if (sentStart != null) {
      sentenceOffsets.push([sentStart, lastCharPos + 1]);
    }

    return sentenceOffsets;
  }

  return Visualizer;
})(jQuery, window);

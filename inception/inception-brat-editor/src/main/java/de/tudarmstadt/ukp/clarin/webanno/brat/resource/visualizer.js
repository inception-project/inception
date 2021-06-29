/*
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
// -*- Mode: JavaScript; tab-width: 2; indent-tabs-mode: nil; -*-
// vim:set ft=javascript ts=2 sw=2 sts=2 cindent:

var Visualizer = (function ($, window, undefined) {
  var svg;

  class DocumentData {
    text;
    chunks = [];
    spans = {};
    eventDescs = {};
    sentComment = {};
    arcs = [];
    arcById = {};
    markedSent = {};
    spanAnnTexts = {};
    towers = {};
    spanDrawOrderPermutation = []
    sizes = {};

    constructor(text) {
      Object.seal(this);

      this.text = text;
    }
  }

  class Fragment {
    id;
    span;
    from;
    to;
    rectBox;
    text;
    chunk;
    indexNumber;
    drawOrder;
    towerId;
    curly;
    drawCurly = false;
    labelText;
    glyphedLabelText;
    group;
    rect;
    left;
    right;
    width;
    height;
    nestingHeight;
    nestingHeightLR;
    nestingHeightRL;
    nestingDepth;
    nestingDepthLR;
    nestingDepthRL;
    highlightPos;

    constructor(id, span, from, to) {
      Object.seal(this);

      this.id = id;
      this.span = span;
      this.from = from;
      this.to = to;
    }
  }

  class Span {
    id;
    type;
    totalDist = 0;
    numArcs = 0;
    generalType;
    headFragment = null; // Fragment
    unsegmentedOffsets;
    offsets = [];
    segmentedOffsetsMap = {};
    clippedAtStart = false;
    clippedAtEnd = false;
    incoming = [];
    outgoing = [];
    attributes = {};
    attributeText = [];
    attributeCues = {};
    attributeCueFor = {};
    attributeMerge = {}; // for box, cross, etc. that are span-global
    fragments = [];
    normalized;
    normalizations = [];
    wholeFrom = undefined;
    wholeTo = undefined;
    comment = undefined; // { type: undefined, text: undefined };
    drawCurly = false;
    labelText;
    refedIndexSum = undefined;
    color;
    shadowClass;
    floor;
    marked;

    constructor(id, type, offsets, generalType) {
      Object.seal(this);

      this.id = id;
      this.type = type;
      this.unsegmentedOffsets = offsets;
      this.generalType = generalType;

      this.initContainers();
    }

    initContainers(offsets) {
      this.incoming = [];
      this.outgoing = [];
      this.attributes = {};
      this.attributeText = [];
      this.attributeCues = {};
      this.attributeCueFor = {};
      this.attributeMerge = {}; // for box, cross, etc. that are span-global
      this.fragments = [];
      this.normalizations = [];
    }

    splitMultilineOffsets(text) {
      this.segmentedOffsetsMap = {};

      for (var fi = 0, nfi = 0; fi < this.unsegmentedOffsets.length; fi++) {
        var begin = this.unsegmentedOffsets[fi][0];
        var end = this.unsegmentedOffsets[fi][1];

        for (var ti = begin; ti < end; ti++) {
          var c = text.charAt(ti);
          if (c == '\n' || c == '\r') {
            if (begin !== null) {
              this.offsets.push([begin, ti]);
              this.segmentedOffsetsMap[nfi++] = fi;
              begin = null;
            }
          }
          else if (begin === null) {
            begin = ti;
          }
        }

        if (begin !== null) {
          this.offsets.push([begin, end]);
          this.segmentedOffsetsMap[nfi++] = fi;
        }
      }
    }

    copy(id) {
      var span = $.extend(new Span(), this); // clone
      span.id = id;
      // protect from shallow copy
      span.initContainers();
      span.unsegmentedOffsets = this.unsegmentedOffsets.slice();
      // read-only; shallow copy is fine
      span.offsets = this.offsets;
      span.segmentedOffsetsMap = this.segmentedOffsetsMap;
      return span;
    }
  }

  class EventDesc {
    id;
    triggerId;
    roles = [];
    equiv = false;
    relation = false;
    // leftSpans = undefined;
    // rightSpans = undefined;
    // annotatorNotes = undefined;
    labelText = undefined;
    color = undefined

    constructor(id, triggerId, roles, klass) {
      Object.seal(this);
      
      this.id = id;
      this.triggerId = triggerId;
      roles.forEach(role => this.roles.push({ type: role[0], targetId: role[1] }));

      this.equiv = klass == "equiv";
      this.relation = klass == "relation";
    }
  }

  class Chunk {
    index;
    text;
    from;
    to;
    space;
    fragments = [];
    lastSpace;
    nextSpace;
    sentence = undefined;
    group = undefined;
    highlightGroup = undefined;
    markedTextStart = undefined;
    markedTextEnd = undefined;
    right = undefined;
    row = undefined;
    textX = undefined;
    translation = undefined;
    firstFragmentIndex;
    lastFragmentIndex;

    constructor(index, text, from, to, space, spans) {
      Object.seal(this);

      this.index = index;
      this.text = text;
      this.from = from;
      this.to = to;
      this.space = space;
    }
  }

  class Arc {
    origin;
    target;
    dist;
    type;
    shadowClass;
    jumpHeight = 0;
    equiv = false;
    eventDescId;
    relation = false;
    eventDescId;
    normalizations = [];
    marked;

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
    hasAnnotations = false;
    maxArcHeight = 0;
    maxSpanHeight = 0;
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
      this.group = svg.group({ 'class': 'row' });
      this.background = svg.group(this.group);
      Object.seal(this);
    }
  }

  class Measurements {
    widths;
    height;
    y;

    constructor(widths, height, y) {
      Object.seal(this);

      this.widths = widths;
      this.height = height;
      this.y = y;
    }
  }

  /**
   * Sets default values for a wide range of optional attributes.
   */ 
  var setSourceDataDefaults = function (sourceData) {
    // The following are empty lists if not set
    $.each([
      'attributes',
      'comments',
      'entities',
      'equivs',
      'events',
      'modifications',
      'normalizations',
      'relations',
      'triggers',
    ], (attrNo, attr) => {
      if (sourceData[attr] === undefined) {
        sourceData[attr] = [];
      }
    });

    // BEGIN WEBANNO EXTENSION - #1074 - Fix potential NPE
    // Avoid exception due to undefined text in tokenise and sentenceSplit
    if (sourceData.text === undefined) {
      sourceData.text = "";
    }
    // END WEBANNO EXTENSION

    // If we lack sentence offsets we fall back on naive sentence splitting
    if (sourceData.sentence_offsets === undefined) {
      sourceData.sentence_offsets = sentenceSplit(sourceData.text);
    }

    // Similarily we fall back on whitespace tokenisation
    if (sourceData.token_offsets === undefined) {
      sourceData.token_offsets = tokenise(sourceData.text);
    }
  };

  // Set default values for a variety of collection attributes
  var setCollectionDefaults = function (collectionData) {
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
  };

  class Visualizer {
    dispatcher;
    // WEBANNO EXTENSION BEGIN - RTL support - DEV mode switch
    rtlmode = false;
    // WEBANNO EXTENSION END

    // WEBANNO EXTENSION BEGIN - #588 - Better handling of setting brat font size 
    fontZoom = 100;
    // WEBANNO EXTENSION END - #588 - Better handling of setting brat font size 

    svg;
    $svg;
    $svgDiv;
    _svg;

    baseCanvasWidth = 0;
    canvasWidth = 0;
  
    data = null;
    sourceData = null;
    requestedData = null;
    renderErrors;

    coll = null;
    doc = null;
    args = null;

    isRenderRequested;
    drawing = false;
    redraw = false;
    arcDragOrigin;

    relationTypesHash;
    isCollectionLoaded = false;
    entityAttributeTypes = null;
    eventAttributeTypes = null;
    spanTypes = null;
    highlightGroup;
    collapseArcs = false;
    collapseArcSpace = false;

    highlight;
    highlightArcs;
    highlightSpans;
    commentId;

    // OPTIONS
    roundCoordinates = true; // try to have exact pixel offsets
    boxTextMargin = { x: 0, y: 1.5 }; // effect is inverse of "margin" for some reason
    highlightRounding = { x: 3, y: 3 }; // rx, ry for highlight boxes
    spaceWidths = {
      ' ': 4,
      '\u00a0': 4,
      '\u200b': 0,
      '\u3000': 8,
      '\t': 12,
      '\n': 4
    };
    coloredCurlies = true; // color curlies by box BG
    arcSlant = 15; //10;
    minArcSlant = 8;
    arcHorizontalSpacing = 10; // min space boxes with connecting arc
    rowSpacing = -5; // for some funny reason approx. -10 gives "tight" packing.

    sentNumMargin = 40;
    smoothArcCurves = true; // whether to use curves (vs lines) in arcs
    smoothArcSteepness = 0.5; // steepness of smooth curves (control point)
    reverseArcControlx = 5; // control point distance for "UFO catchers"

    // "shadow" effect settings (note, error, incompelete)
    rectShadowSize = 3;
    rectShadowRounding = 2.5;
    arcLabelShadowSize = 1;
    arcLabelShadowRounding = 5;
    shadowStroke = 2.5; // TODO XXX: this doesn't affect anything..?

    // "marked" effect settings (edited, focus, match)
    markedSpanSize = 6;
    markedArcSize = 2;
    markedArcStroke = 7; // TODO XXX: this doesn't seem to do anything..?

    rowPadding = 2;
    nestingAdjustYStepSize = 2; // size of height adjust for nested/nesting spans
    nestingAdjustXStepSize = 1; // size of height adjust for nested/nesting spans

    highlightSpanSequence;
    highlightArcSequence;
    highlightTextSequence;
    highlightDuration = '2s';
    // different sequence for "mere" matches (as opposed to "focus" and "edited" highlights)
    highlightMatchSequence = '#FFFF00'; // plain yellow

    fragmentConnectorDashArray = '1,3,3,3';
    fragmentConnectorColor = '#000000';
    // END OPTIONS

    commentPrioLevels = [
      'Unconfirmed', 'Incomplete', 'Warning', 'Error', 'AnnotatorNotes',
      'AddedAnnotation', 'MissingAnnotation', 'ChangedAnnotation'
    ];

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
        onLoad: (_svg) => {
          this.svg = Visualizer.svg = _svg;
          this.$svg = $(this.svg._svg);
          this.triggerRender();
        }
      });

      //var highlightSequence = '#FFFC69;#FFCC00;#FFFC69'; // a bit toned town
      var highlightSequence = '#FF9632;#FFCC00;#FF9632'; // yellow - deep orange
      this.highlightSpanSequence = highlightSequence;
      this.highlightArcSequence = highlightSequence;
      this.highlightTextSequence = highlightSequence;

      this.arcDragOrigin = null; // TODO

      this.renderErrors = {
        unableToReadTextFile: true,
        annotationFileNotFound: true,
        isDirectoryError: true
      };

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

      dispatcher.
        on('collectionChanged', this, this.collectionChanged).
        on('collectionLoaded', this, this.collectionLoaded).
        on('renderData', this, this.renderData).
        on('rerender', this, this.rerender).
        on('renderDataPatch', this, this.renderDataPatch).
        on('triggerRender', this, this.triggerRender).
        on('requestRenderData', this, this.requestRenderData).
        on('isReloadOkay', this, this.isReloadOkay).
        on('resetData', this, this.resetData).
        on('abbrevs', this, this.setAbbrevs).
        on('textBackgrounds', this, this.setTextBackgrounds).
        on('layoutDensity', this, this.setLayoutDensity).
        on('svgWidth', this, this.setSvgWidth).
        on('current', this, this.gotCurrent).
        on('clearSVG', this, this.clearSVG).
        on('mouseover', this, this.onMouseOver).
        on('mouseout', this, this.onMouseOut);
    }

    rowBBox(fragment) {
      var box = $.extend({}, fragment.rectBox); // clone
      var chunkTranslation = fragment.chunk.translation;
      box.x += chunkTranslation.x;
      box.y += chunkTranslation.y;
      return box;
    }

    commentPriority(commentClass) {
      if (commentClass === undefined) {
        return -1;
      }

      var len = this.commentPrioLevels.length;
      for (var i = 0; i < len; i++) {
        if (commentClass.indexOf(this.commentPrioLevels[i]) != -1) {
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
      $.each(args[markedType] || [], (markedNo, marked) => {
        if (marked[0] == 'sent') {
          data.markedSent[marked[1]] = true;
        } else if (marked[0] == 'equiv') { // [equiv, Equiv, T1]
          $.each(sourceData.equivs, (equivNo, equiv) => {
            if (equiv[1] == marked[1]) {
              var len = equiv.length;
              for (var i = 2; i < len; i++) {
                if (equiv[i] == marked[2]) {
                  // found it
                  len -= 3;
                  for (var i = 1; i <= len; i++) {
                    var arc = this.data.eventDescs[equiv[0] + "*" + i].equivArc;
                    arc.marked = markedType;
                  }
                  return; // next equiv
                }
              }
            }
          });
        } else if (marked.length == 2) {
          markedText.push([parseInt(marked[0], 10), parseInt(marked[1], 10), markedType]);
        } else {
          var span = this.data.spans[marked[0]];
          if (span) {
            if (marked.length == 3) { // arc
              $.each(span.outgoing, (arcNo, arc) => {
                if (arc.target == marked[2] && arc.type == marked[1]) {
                  arc.marked = markedType;
                }
              });
            } else { // span
              span.marked = markedType;
            }
          } else {
            var eventDesc = this.data.eventDescs[marked[0]];
            if (eventDesc) { // relation
              $.each(this.data.spans[eventDesc.triggerId].outgoing, (arcNo, arc) => {
                if (arc.eventDescId == marked[0]) {
                  arc.marked = markedType;
                }
              });
            } else { // try for trigger
              $.each(this.data.eventDescs, (eventDescNo, eventDesc) => {
                if (eventDesc.triggerId == marked[0]) {
                  this.data.spans[eventDesc.id].marked = markedType;
                }
              });
            }
          }
        }
      });
    }

    findArcHeight(fromIndex, toIndex, fragmentHeights) {
      var height = 0;
      for (var i = fromIndex; i <= toIndex; i++) {
        if (fragmentHeights[i] > height)
          height = fragmentHeights[i];
      }
      height += Configuration.visual.arcSpacing;
      return height;
    }

    adjustFragmentHeights(fromIndex, toIndex, fragmentHeights, height) {
      for (var i = fromIndex; i <= toIndex; i++) {
        if (fragmentHeights[i] < height)
          fragmentHeights[i] = height;
      }
    };

    fragmentComparator(a, b) {
      var tmp;
      var aSpan = a.span;
      var bSpan = b.span;

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
      var ad = a.to - a.from;
      var bd = b.to - b.from;
      tmp = ad - bd;
      if (aSpan.numArcs == 0 && bSpan.numArcs == 0) {
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

    setData(_sourceData) {
      this.sourceData = _sourceData;

      // WEBANNO EXTENSION BEGIN - RTL support - mode switch
      this.rtlmode = this.sourceData.rtl_mode;
      // WEBANNO EXTENSION END
      // WEBANNO EXTENSION BEGIN - #588 - Better handling of setting brat font size 
      this.fontZoom = this.sourceData.font_zoom;
      // WEBANNO EXTENSION END - #588 - Better handling of setting brat font size 
      // WEBANNO EXTENSION BEGIN - #406 - Sharable link for annotation documents  
      this.args = this.sourceData.args;
      // WEBANNO EXTENSION END - #406 - Sharable link for annotation documents  

      if (!this.args) {
        this.args = {};
      }

      this.dispatcher.post('newSourceData', [this.sourceData]);
      this.data = new DocumentData(this.sourceData.text);

      // collect annotation data
      $.each(this.sourceData.entities, (entityNo, entity) => {
        // offsets given as array of (start, end) pairs
        //                 (id,        type,      offsets,   generalType)
        let span = new Span(entity[0], entity[1], entity[2], 'entity');

        // WEBANNO EXTENSION BEGIN - #820 - Allow setting label/color individually + #587 Customize mouse hover text
        if (entity[3]) {
          let attributes = entity[3];
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
        // WEBANNO EXTENSION END
        
        span.splitMultilineOffsets(this.data.text);
        this.data.spans[entity[0]] = span;
      });

      var triggerHash = {};
      $.each(this.sourceData.triggers, (triggerNo, trigger) => {
        //                        (id,         type,       offsets,    generalType)
        var triggerSpan = new Span(trigger[0], trigger[1], trigger[2], 'trigger');
        triggerSpan.splitMultilineOffsets(data.text);
        triggerHash[trigger[0]] = [triggerSpan, []]; // triggerSpan, eventlist
      });

      $.each(this.sourceData.events, (eventNo, eventRow) => {
        var eventDesc = this.data.eventDescs[eventRow[0]] =
          //           (id,          triggerId,   roles,        klass)
          new EventDesc(eventRow[0], eventRow[1], eventRow[2]);
        var trigger = triggerHash[eventDesc.triggerId];
        var span = trigger[0].copy(eventDesc.id);
        trigger[1].push(span);
        this.data.spans[eventDesc.id] = span;
      });

      // XXX modifications: delete later
      $.each(this.sourceData.modifications, (modNo, mod) => {
        // mod: [id, spanId, modification]
        if (!this.data.spans[mod[2]]) {
          this.dispatcher.post('messages', [[['<strong>ERROR</strong><br/>Event ' + mod[2] + ' (referenced from modification ' + mod[0] + ') does not occur in document ' + this.data.document + '<br/>(please correct the source data)', 'error', 5]]]);
          return;
        }
        this.data.spans[mod[2]][mod[1]] = true;
      });

      var midpointComparator = (a, b) => {
        var tmp = a.from + a.to - b.from - b.to;
        if (!tmp)
          return 0;
        return tmp < 0 ? -1 : 1;
      };

      // split spans into span fragments (for discontinuous spans)
      $.each(this.data.spans, (spanNo, span) => {
        $.each(span.offsets, (offsetsNo, offsets) => {
          var from = parseInt(offsets[0], 10);
          var to = parseInt(offsets[1], 10);
          var fragment = new Fragment(offsetsNo, span, from, to);
          span.fragments.push(fragment);
        });
        // ensure ascending order
        span.fragments.sort(midpointComparator);
        span.wholeFrom = span.fragments[0].from;
        span.wholeTo = span.fragments[span.fragments.length - 1].to;
        span.headFragment = span.fragments[(true) ? span.fragments.length - 1 : 0]; // TODO configurable!
      });

      var spanComparator = (a, b) => {
        var aSpan = this.data.spans[a];
        var bSpan = this.data.spans[b];
        var tmp = aSpan.headFragment.from + aSpan.headFragment.to - bSpan.headFragment.from - bSpan.headFragment.to;
        if (tmp) {
          return tmp < 0 ? -1 : 1;
        }
        return 0;
      };

      $.each(this.sourceData.equivs, (equivNo, equiv) => {
        // equiv: ['*', 'Equiv', spanId...]
        equiv[0] = "*" + equivNo;
        var equivSpans = equiv.slice(2);
        var okEquivSpans = [];
        // collect the equiv spans in an array
        $.each(equivSpans, (equivSpanNo, equivSpan) => {
          if (this.data.spans[equivSpan])
            okEquivSpans.push(equivSpan);
          // TODO: #404, inform the user with a message?
        });
        // sort spans in the equiv by their midpoint
        okEquivSpans.sort(spanComparator);
        // generate the arcs
        var len = okEquivSpans.length;
        for (var i = 1; i < len; i++) {
          var eventDesc = this.data.eventDescs[equiv[0] + '*' + i] =
            //           (id,                  triggerId,           roles,                         klass)
            new EventDesc(okEquivSpans[i - 1], okEquivSpans[i - 1], [[equiv[1], okEquivSpans[i]]], 'equiv');
          eventDesc.leftSpans = okEquivSpans.slice(0, i);
          eventDesc.rightSpans = okEquivSpans.slice(i);
        }
      });

      $.each(this.sourceData.relations, (relNo, rel) => {
        // rel[2] is args, rel[2][a][0] is role and rel[2][a][1] is value for a in (0,1)
        var argsDesc = this.relationTypesHash[rel[1]];
        argsDesc = argsDesc && argsDesc.args;
        var t1, t2;
        if (argsDesc) {
          // sort the arguments according to the config
          var args = {};
          args[rel[2][0][0]] = rel[2][0][1];
          args[rel[2][1][0]] = rel[2][1][1];
          t1 = args[argsDesc[0].role];
          t2 = args[argsDesc[1].role];
        } else {
          // (or leave as-is in its absence)
          t1 = rel[2][0][1];
          t2 = rel[2][1][1];
        }
        //                                          (id, triggerId, roles,   klass)
        this.data.eventDescs[rel[0]] = new EventDesc(t1, t1, [[rel[1], t2]], 'relation');
        // WEBANNO EXTENSION BEGIN - #820 - Allow setting label/color individually
        if (rel[3]) {
          this.data.eventDescs[rel[0]].labelText = rel[3];
        }
        if (rel[4]) {
          this.data.eventDescs[rel[0]].color = rel[4];
        }
        // WEBANNO EXTENSION END
      });

      // attributes
      $.each(this.sourceData.attributes, (attrNo, attr) => {
        // attr: [id, name, spanId, value, cueSpanId
        // TODO: might wish to check what's appropriate for the type
        // instead of using the first attribute def found
        var attrType = (eventAttributeTypes[attr[1]] ||
          entityAttributeTypes[attr[1]]);
        var attrValue = attrType && attrType.values[attrType.bool || attr[3]];
        var span = this.data.spans[attr[2]];
        if (!span) {
          this.dispatcher.post('messages', [[['Annotation ' + attr[2] + ', referenced from attribute ' + attr[0] + ', does not exist.', 'error']]]);
          return;
        }
        var valText = (attrValue && attrValue.name) || attr[3];
        var attrText = attrType
          ? (attrType.bool ? attrType.name : (attrType.name + ': ' + valText))
          : (attr[3] == true ? attr[1] : attr[1] + ': ' + attr[3]);
        span.attributeText.push(attrText);
        span.attributes[attr[1]] = attr[3];
        if (attr[4]) { // cue
          span.attributeCues[attr[1]] = attr[4];
          var cueSpan = this.data.spans[attr[4]];
          cueSpan.attributeCueFor[this.data.spans[1]] = attr[2];
          cueSpan.cue = 'CUE'; // special css type
        }
        $.extend(span.attributeMerge, attrValue);
      });

      // comments
      this.sourceData.comments.forEach(comment => {
        // comment: [entityId, type, text]
        // TODO error handling
        // sentence id: ['sent', sentId]
        if (comment[0] instanceof Array && comment[0][0] == 'sent') {
          // sentence comment
          var sent = comment[0][1];
          var text = comment[2];
          if (this.data.sentComment[sent]) {
            text = this.data.sentComment[sent].text + '<br/>' + text;
          }
          this.data.sentComment[sent] = { type: comment[1], text: text };
        } else {
          var id = comment[0];
          var trigger = triggerHash[id];
          var eventDesc = this.data.eventDescs[id];
          var commentEntities = trigger
            ? trigger[1] // trigger: [span, ...]
            : id in this.data.spans
              ? [this.data.spans[id]] // span: [span]
              : id in this.data.eventDescs
                ? [this.data.eventDescs[id]] // arc: [eventDesc]
                : [];
          commentEntities.forEach(entity => {
            // if duplicate comment for entity:
            // overwrite type, concatenate comment with a newline
            if (!entity.comment) {
              entity.comment = { type: comment[1], text: comment[2] };
            } else {
              entity.comment.type = comment[1];
              entity.comment.text += "\n" + comment[2];
            }
            // partially duplicate marking of annotator note comments
            if (comment[1] == "AnnotatorNotes") {
              entity.annotatorNotes = comment[2];
            }
            // prioritize type setting when multiple comments are present
            if (this.commentPriority(comment[1]) > this.commentPriority(entity.shadowClass)) {
              entity.shadowClass = comment[1];
            }
          });
        }
      });

      // prepare span boundaries for token containment testing
      var sortedFragments = [];
      $.each(this.data.spans, function (spanNo, span) {
        $.each(span.fragments, function (fragmentNo, fragment) {
          sortedFragments.push(fragment);
        });
      });
      // sort fragments by beginning, then by end
      sortedFragments.sort(function (a, b) {
        var x = a.from;
        var y = b.from;
        if (x == y) {
          x = a.to;
          y = b.to;
        }
        return ((x < y) ? -1 : ((x > y) ? 1 : 0));
      });
      var currentFragmentId = 0;
      var startFragmentId = 0;
      var numFragments = sortedFragments.length;
      var lastTo = 0;
      var firstFrom = null;
      var chunkNo = 0;
      var space;
      var chunk = null;

      // token containment testing (chunk recognition)
      this.sourceData.token_offsets.forEach(offset => {
        var from = offset[0];
        var to = offset[1];
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
        var text = this.data.text.substring(firstFrom, to);
        if (chunk) {
          chunk.nextSpace = space;
        }
        //               (index,     text, from,      to, space) {
        chunk = new Chunk(chunkNo++, text, firstFrom, to, space);
        chunk.lastSpace = space;
        this.data.chunks.push(chunk);
        lastTo = to;
        firstFrom = null;
      });

      var numChunks = chunkNo;

      // find sentence boundaries in relation to chunks
      chunkNo = 0;
      // WEBANNO EXTENSION BEGIN - #1315 - Various issues with line-oriented mode
      /*
              var sentenceNo = 0;
              var pastFirst = false;
              $.each(sourceData.sentence_offsets, function() {
                var from = this[0];
                if (chunkNo >= numChunks) return false;
                if (data.chunks[chunkNo].from > from) return;
                var chunk;
                while (chunkNo < numChunks && (chunk = data.chunks[chunkNo]).from < from) {
                  chunkNo++;
                }
                chunkNo++;
                if (pastFirst && from <= chunk.from) {
                  var numNL = chunk.space.split("\n").length - 1;
                  if (!numNL) numNL = 1;
                  sentenceNo += numNL;
                  chunk.sentence = sentenceNo;
                } else {
                  pastFirst = true;
                }
              });
      */
      var sentenceNo = this.sourceData.sentence_number_offset - 1;

      this.sourceData.sentence_offsets.forEach(offset => {
        var from = offset[0];
        var to = offset[1];
        var chunk;

        // Skip all chunks that belonged to the previous sentence
        while (chunkNo < numChunks && (chunk = this.data.chunks[chunkNo]).from < from) {
          chunkNo++;
        }

        // No more chunks
        if (chunkNo >= numChunks)
          return false;

        // If the current chunk is not within the current sentence, then it was an empty sentence
        if (this.data.chunks[chunkNo].from >= to) {
          sentenceNo++;
          return;
        }

        sentenceNo++;
        chunk.sentence = sentenceNo;
        // console.trace("ASSIGN: line break ", sentenceNo ," at ", chunk);
        // increase chunkNo counter for next seek iteration
        chunkNo++;
      });
      // WEBANNO EXTENSION END - #1315 - Various issues with line-oriented mode

      // assign fragments to appropriate chunks
      var currentChunkId = 0;
      var chunk;
      sortedFragments.forEach(fragment => {
        while (fragment.to > (chunk = this.data.chunks[currentChunkId]).to) {
          currentChunkId++;
        }
        chunk.fragments.push(fragment);
        fragment.text = chunk.text.substring(fragment.from - chunk.from, fragment.to - chunk.from);
        fragment.chunk = chunk;
      });

      // assign arcs to spans; calculate arc distances
      Object.entries(this.data.eventDescs).forEach(([eventNo, eventDesc]) => {
        var dist = 0;
        var origin = this.data.spans[eventDesc.id];
        if (!origin) {
          // TODO: include missing trigger ID in error message
          this.dispatcher.post('messages', [[['<strong>ERROR</strong><br/>Trigger for event "' + eventDesc.id + '" not found in ' + data.document + '<br/>(please correct the source data)', 'error', 5]]]);
          return;
        }

        var here = origin.headFragment.from + origin.headFragment.to;
        eventDesc.roles.forEach(role => {
          var target = this.data.spans[role.targetId];
          if (!target) {
            this.dispatcher.post('messages', [[['<strong>ERROR</strong><br/>"' + role.targetId + '" (referenced from "' + eventDesc.id + '") not found in ' + data.document + '<br/>(please correct the source data)', 'error', 5]]]);
            return;
          }
          var there = target.headFragment.from + target.headFragment.to;
          var dist = Math.abs(here - there);
          var arc = new Arc(eventDesc, role, dist, eventNo);
          origin.totalDist += dist;
          origin.numArcs++;
          target.totalDist += dist;
          target.numArcs++;
          this.data.arcs.push(arc);
          target.incoming.push(arc);
          origin.outgoing.push(arc);
          this.data.arcById[arc.eventDescId] = arc;
        }); // roles
      }); // eventDescs


      // normalizations
      this.sourceData.normalizations.forEach(norm => {
        var target = norm[0];
        var refdb = norm.length > 1 ? norm[1] : "#"; // See Renderer.QUERY_LAYER_LEVEL_DETAILS
        var refid = norm.length > 2 ? norm[2] : "";
        var reftext = norm.length > 3 ? norm[3] : null;

        var span = this.data.spans[target];
        if (span) {
          span.normalizations.push([refdb, refid, reftext]);
          span.normalized = 'Normalized';
          return;
        }

        var arc = this.data.arcById[target];
        if (arc) {
          arc.normalizations.push([refdb, refid, reftext]);
          arc.normalized = "Normalized";
          return;
        }

        this.dispatcher.post('messages', [[['Annotation ' + target + ' does not exist.', 'error']]]);
      });

      // highlighting
      var markedText = [];
      this.setMarked('edited'); // set by editing process
      this.setMarked('focus'); // set by URL
      this.setMarked('matchfocus'); // set by search process, focused match
      this.setMarked('match'); // set by search process, other (non-focused) match

      Object.entries(this.data.spans).forEach(span => {
        // calculate average arc distances
        // average distance of arcs (0 for no arcs)
        span.avgDist = span.numArcs ? span.totalDist / span.numArcs : 0;

        // collect fragment texts into span texts
        var fragmentTexts = [];
        span.fragments && span.fragments.forEach(fragment => {
          // TODO heuristics
          fragmentTexts.push(fragment.text);
        });
        span.text = fragmentTexts.join('');
      }); // data.spans

      for (var i = 0; i < 2; i++) {
        // preliminary sort to assign heights for basic cases
        // (first round) and cases resolved in the previous
        // round(s).
        this.data.chunks.forEach(chunk => {
          // sort and then re-number
          chunk.fragments.sort(this.fragmentComparator);
          chunk.fragments.forEach((fragment, fragmentNo) => fragment.indexNumber = fragmentNo);
        });

        // nix the sums, so we can sum again
        $.each(this.data.spans, function (spanNo, span) {
          span.refedIndexSum = 0;
        });

        // resolved cases will now have indexNumber set
        // to indicate their relative order. Sum those for referencing cases
        // for use in iterative resorting
        this.data.arcs.forEach(arc => {
          this.data.spans[arc.origin].refedIndexSum += this.data.spans[arc.target].headFragment.indexNumber;
        });
      }

      // Final sort of fragments in chunks for drawing purposes
      // Also identify the marked text boundaries regarding chunks
      this.data.chunks.forEach(chunk => {
        // and make the next sort take this into account. Note that this will
        // now resolve first-order dependencies between sort orders but not
        // second-order or higher.
        chunk.fragments.sort(this.fragmentComparator);
        chunk.fragments.forEach((fragment, fragmentNo) => fragment.drawOrder = fragmentNo);
      });

      this.data.spanDrawOrderPermutation = Object.keys(this.data.spans);
      this.data.spanDrawOrderPermutation.sort((a, b) => {
        var spanA = this.data.spans[a];
        var spanB = this.data.spans[b];

        // We're jumping all over the chunks, but it's enough that
        // we're doing everything inside each chunk in the right
        // order. should it become necessary to actually do these in
        // linear order, put in a similar condition for
        // spanX.headFragment.chunk.index; but it should not be
        // needed.
        var tmp = spanA.headFragment.drawOrder - spanB.headFragment.drawOrder;
        if (tmp) {
          return tmp < 0 ? -1 : 1;
        }

        return 0;
      });

      // resort the spans for linear order by center
      sortedFragments.sort(midpointComparator);

      // sort fragments into towers, calculate average arc distances
      var lastFragment = null;
      var towerId = -1;
      $.each(sortedFragments, function (i, fragment) {
        if (!lastFragment || (lastFragment.from != fragment.from || lastFragment.to != fragment.to)) {
          towerId++;
        }
        fragment.towerId = towerId;
        lastFragment = fragment;
      }); // sortedFragments


      // find curlies (only the first fragment drawn in a tower)
      this.data.spanDrawOrderPermutation.forEach(spanId => {
        var span = this.data.spans[spanId];

        span.fragments.forEach(fragment => {
          if (!this.data.towers[fragment.towerId]) {
            this.data.towers[fragment.towerId] = [];
            fragment.drawCurly = true;
            fragment.span.drawCurly = true;
          }
          this.data.towers[fragment.towerId].push(fragment);
        });
      });

      var spanAnnTexts = {};
      $.each(this.data.chunks, (chunkNo, chunk) => {
        chunk.markedTextStart = [];
        chunk.markedTextEnd = [];

        $.each(chunk.fragments, (fragmentNo, fragment) => {
          if (chunk.firstFragmentIndex == undefined) {
            chunk.firstFragmentIndex = fragment.towerId;
          }
          chunk.lastFragmentIndex = fragment.towerId;

          var spanLabels = Util.getSpanLabels(this.spanTypes, fragment.span.type);
          fragment.labelText = Util.spanDisplayForm(this.spanTypes, fragment.span.type);
          // Find the most appropriate label according to text width
          if (Configuration.abbrevsOn && spanLabels) {
            var labelIdx = 1; // first abbrev
            var maxLength = (fragment.to - fragment.from) / 0.8;
            while (fragment.labelText.length > maxLength &&
              spanLabels[labelIdx]) {
              fragment.labelText = spanLabels[labelIdx];
              labelIdx++;
            }
          }

          // WEBANNO EXTENSION BEGIN - #709 - Optimize render data size for annotations without labels
          fragment.labelText = "(" + fragment.labelText + ")";
          // WEBANNO EXTENSION END - #709 - Optimize render data size for annotations without labels
          // WEBANNO EXTENSION BEGIN - #820 - Allow setting label/color individually
          if (fragment.span.labelText) {
            fragment.labelText = fragment.span.labelText;
          }
          // WEBANNO EXTENSION END
          var svgtext = this.svg.createText(); // one "text" element per row
          var postfixArray = [];
          var prefix = '';
          var postfix = '';
          var warning = false;
          $.each(fragment.span.attributes, (attrType, valType) => {
            // TODO: might wish to check what's appropriate for the type
            // instead of using the first attribute def found
            var attr = (eventAttributeTypes[attrType] ||
              entityAttributeTypes[attrType]);
            if (!attr) {
              // non-existent type
              warning = true;
              return;
            }
            var val = attr.values[attr.bool || valType];
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
              if (val.position == "left") {
                prefix = val.glyph + prefix;
                var tspan_attrs = { 'class': 'glyph' };
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
          var text = fragment.labelText;
          if (prefix !== '') {
            text = prefix + ' ' + text;
            svgtext.string(' ');
          }
          svgtext.string(fragment.labelText);
          if (postfixArray.length) {
            text += ' ' + postfix;
            svgtext.string(' ');
            $.each(postfixArray, function (elNo, el) {
              var tspan_attrs = { 'class': 'glyph' };
              if (el[1].glyphColor) {
                tspan_attrs.fill = el[1].glyphColor;
              }
              svgtext.span(el[1].glyph, tspan_attrs);
            });
          }
          if (warning) {
            svgtext.span("#", { 'class': 'glyph attribute_warning' });
            text += ' #';
          }
          fragment.glyphedLabelText = text;

          if (!spanAnnTexts[text]) {
            spanAnnTexts[text] = true;
            this.data.spanAnnTexts[text] = svgtext;
          }
        }); // chunk.fragments
      }); // chunks

      var numChunks = this.data.chunks.length;
      // note the location of marked text with respect to chunks
      var startChunk = 0;
      var currentChunk;
      // sort by "from"; we don't need to sort by "to" as well,
      // because unlike spans, chunks are disjunct
      markedText.sort((a, b) => Util.cmp(a[0], b[0]));
      $.each(markedText, (textNo, textPos) => {
        var from = textPos[0];
        var to = textPos[1];
        var markedType = textPos[2];
        if (from < 0)
          from = 0;
        if (to < 0)
          to = 0;
        if (to >= this.data.text.length)
          to = this.data.text.length - 1;
        if (from > to)
          from = to;
        while (startChunk < numChunks) {
          var chunk = this.data.chunks[startChunk];
          if (from <= chunk.to) {
            chunk.markedTextStart.push([textNo, true, from - chunk.from, null, markedType]);
            break;
          }
          startChunk++;
        }
        if (startChunk == numChunks) {
          this.dispatcher.post('messages', [[['Wrong text offset', 'error']]]);
          return;
        }
        currentChunk = startChunk;
        while (currentChunk < numChunks) {
          var chunk = this.data.chunks[currentChunk];
          if (to <= chunk.to) {
            chunk.markedTextEnd.push([textNo, false, to - chunk.from]);
            break;
          }
          currentChunk++;
        }
        if (currentChunk == numChunks) {
          this.dispatcher.post('messages', [[['Wrong text offset', 'error']]]);
          var chunk = this.data.chunks[this.data.chunks.length - 1];
          chunk.markedTextEnd.push([textNo, false, chunk.text.length]);
          return;
        }
      }); // markedText

      this.dispatcher.post('dataReady', [this.data]);
    }

    resetData() {
      this.setData(this.sourceData);
      this.renderData();
    }

    translate(element, x, y) {
      $(element.group).attr('transform', 'translate(' + x + ', ' + y + ')');
      element.translation = { x: x, y: y };
    }

    addHeaderAndDefs() {
      var commentName = (this.coll + '/' + this.doc).replace('--', '-\\-');
      this.$svg.append('<!-- document: ' + commentName + ' -->');
      var defs = this.svg.defs();
      var $blurFilter = $($.parseXML(('<filter id="Gaussian_Blur"><feGaussianBlur in="SourceGraphic" stdDeviation="2" /></filter>')));
      this.svg.add(defs, $blurFilter.children(0));
      return defs;
    }

    getTextMeasurements(textsHash, options, callback) {
      // make some text elements, find out the dimensions
      var textMeasureGroup = this.svg.group(options);

      // changed from $.each because of #264 ('length' can appear)
      for (var text in textsHash) {
        if (textsHash.hasOwnProperty(text)) {
          this.svg.text(textMeasureGroup, 0, 0, text);
        }
      }

      // measuring goes on here
      var widths = {};
      $(textMeasureGroup).find('text').each(function (svgTextNo, svgText) {
        var text = $(svgText).text();
        widths[text] = this.getComputedTextLength();

        if (callback) {
          $.each(textsHash[text], function (text, object) {
            callback(object, svgText);
          });
        }
      });
      var bbox = textMeasureGroup.getBBox();
      this.svg.remove(textMeasureGroup);

      return new Measurements(widths, bbox.height, bbox.y);
    }

    getTextAndSpanTextMeasurements() {
      // get the span text sizes
      var chunkTexts = {}; // set of span texts
      this.data.chunks.forEach(chunk => {
        chunk.row = undefined; // reset
        if (!chunkTexts.hasOwnProperty(chunk.text)) {
          chunkTexts[chunk.text] = [];
        }

        // here we also need all the spans that are contained in
        // chunks with this text, because we need to know the position
        // of the span text within the respective chunk text
        var chunkText = chunkTexts[chunk.text];
        chunkText.push.apply(chunkText, chunk.fragments);
        // and also the markedText boundaries
        chunkText.push.apply(chunkText, chunk.markedTextStart);
        chunkText.push.apply(chunkText, chunk.markedTextEnd);
      });

      var textSizes = this.getTextMeasurements(chunkTexts, undefined, (fragment, text) => {
        if (fragment instanceof Fragment) { // it's a fragment!
          // measure the fragment text position in pixels
          var firstChar = fragment.from - fragment.chunk.from;
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
          var lastChar = fragment.to - fragment.chunk.from - 1;

          // Adjust for XML whitespace (#832, #1009)
          var textUpToFirstChar = fragment.chunk.text.substring(0, firstChar);
          var textUpToLastChar = fragment.chunk.text.substring(0, lastChar);
          var textUpToFirstCharUnspaced = textUpToFirstChar.replace(/\s\s+/g, ' ');
          var textUpToLastCharUnspaced = textUpToLastChar.replace(/\s\s+/g, ' ');
          firstChar -= textUpToFirstChar.length - textUpToFirstCharUnspaced.length;
          lastChar -= textUpToLastChar.length - textUpToLastCharUnspaced.length;

          // BEGIN WEBANNO EXTENSION - RTL support 
          // - #265 rendering with quotation marks 
          // - #278 Sub-token annotation of LTR text in RTL mode  
          if (this.rtlmode) {
            // This rendering is much slower than the "old" version that brat uses, but it
            // is more reliable in RTL mode.
            var charDirection = null;
            var charAttrs = null;
            var corrFactor = 1;

            if ('rtlsizes' in fragment.chunk) {
              // Use cached metrics
              charDirection = fragment.chunk.rtlsizes.charDirection;
              charAttrs = fragment.chunk.rtlsizes.charAttrs;
              corrFactor = fragment.chunk.rtlsizes.corrFactor;
            }
            else {
              // Calculate metrics
              var start = new Date();

              charDirection = [];
              charAttrs = [];

              // WebAnno #307 Cannot use fragment.chunk.text.length here because invisible
              // characters do not count. Using text.getNumberOfChars() instead.
              //var step1Start = new Date();
              for (var idx = 0; idx < text.getNumberOfChars(); idx++) {
                var cw = text.getEndPositionOfChar(idx).x - text.getStartPositionOfChar(idx).x;
                var dir = isRTL(text.textContent.charCodeAt(idx)) ? "rtl" : "ltr";
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
                var idx = 0;
                var blockBegin = idx;
                var blockEnd = idx;

                // Figure out next block
                while (blockEnd < charAttrs.length) {
                  while (charDirection[blockBegin] == charDirection[blockEnd]) {
                    blockEnd++;
                  }

                  if (charDirection[blockBegin] == (rtlmode ? "ltr" : "rtl")) {
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
              var widthsSum = 0;
              for (var idx = 0; idx < charAttrs.length; idx++) {
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

            //startPos = Math.min(0, Math.min(text.getStartPositionOfChar(charOrder[0]).x, text.getEndPositionOfChar(charOrder[0]).x));
            var startPos = 0;
            //	           	  console.log("startPos[initial]: " + startPos);
            for (var i = 0; charAttrs[i].order != firstChar && i < charAttrs.length; i++) {
              startPos += charAttrs[i].width;
              //	            	  console.log("startPos["+i+"]  "+text.textContent[charOrder[i]]+" width "+charWidths[i]+" : " + startPos);
            }
            if (charDirection[i] == (this.rtlmode ? "ltr" : "rtl")) {
              startPos += charAttrs[i].width;
              //	            	  console.log("startPos["+i+"]  "+text.textContent[charOrder[i]]+" width "+charWidths[i]+" : " + startPos);
            }
            startPos = startPos * corrFactor;
            //	        	  console.log("startPos: " + startPos);
            //endPos = Math.min(0, Math.min(text.getStartPositionOfChar(charOrder[0]).x, text.getEndPositionOfChar(charOrder[0]).x));
            var endPos = 0;
            //	           	  console.log("endPos[initial]: " + endPos);
            for (var i = 0; charAttrs[i].order != lastChar && i < charAttrs.length; i++) {
              endPos += charAttrs[i].width;
              //	            	  console.log("endPos["+i+"]  "+text.textContent[charOrder[i]]+" width "+charWidths[i]+" : " + endPos);
            }
            if (charDirection[i] == (this.rtlmode ? "rtl" : "ltr")) {
              //	            	  console.log("endPos["+i+"]  "+text.textContent[charOrder[i]]+" width "+charWidths[i]+" : " + endPos);
              endPos += charAttrs[i].width;
            }
            endPos = endPos * corrFactor;
            //	        	  console.log("endPos: " + endPos);
          }
          else {
            // Using the old faster method in LTR mode. YES, this means that subtoken 
            // annotations of RTL tokens in LTR mode will render incorrectly. If somebody
            // needs that, we should do a smarter selection of the rendering mode.
            // This is the old measurement code which doesn't work properly because browsers
            // treat the x coordinate very differently. Our width-based measurement is more
            // reliable.
            // WebAnno #307 Cannot use fragment.chunk.text.length here because invisible
            // characters do not count. Using text.getNumberOfChars() instead.
            if (firstChar < text.getNumberOfChars()) {
              startPos = text.getStartPositionOfChar(firstChar).x;
            } else {
              startPos = text.getComputedTextLength();
            }
            endPos = (lastChar < firstChar)
              ? startPos
              : text.getEndPositionOfChar(lastChar).x;
          }
          // END WEBANNO EXTENSION - RTL support - #265 rendering with quotation marks 
          // WEBANNO EXTENSION BEGIN - RTL support - Curlies coordinates
          // In RTL mode, positions are negative (left to right)
          if (this.rtlmode) {
            startPos = -startPos;
            endPos = -endPos;
          }

          // Make sure that startpos and endpos are properly ordered on the X axis
          fragment.curly = {
            from: Math.min(startPos, endPos),
            to: Math.max(startPos, endPos)
          };
          // WEBANNO EXTENSION END              
        } else { // it's markedText [id, start?, char#, offset]
          if (fragment[2] < 0)
            fragment[2] = 0;
          if (!fragment[2]) { // start
            fragment[3] = text.getStartPositionOfChar(fragment[2]).x;
          } else {
            fragment[3] = text.getEndPositionOfChar(fragment[2] - 1).x + 1;
          }
        }
      });

      // get the fragment annotation text sizes
      var fragmentTexts = {};
      var noSpans = true;
      $.each(this.data.spans, (spanNo, span) => {
        $.each(span.fragments, (fragmentNo, fragment) => {
          fragmentTexts[fragment.glyphedLabelText] = true;
          noSpans = false;
        });
      });

      if (noSpans) {
        fragmentTexts.$ = true; // dummy so we can at least get the height
      }

      var fragmentSizes = this.getTextMeasurements(fragmentTexts, { 'class': 'span' });

      return {
        texts: textSizes,
        fragments: fragmentSizes
      };
    }

    addArcTextMeasurements(sizes) {
      // get the arc annotation text sizes (for all labels)
      var arcTexts = {};
      $.each(this.data.arcs, (arcNo, arc) => {
        var labels = Util.getArcLabels(this.spanTypes, this.data.spans[arc.origin].type, arc.type, this.relationTypesHash);
        if (!labels.length) {
          labels = [arc.type];
        }

        // WEBANNO EXTENSION BEGIN - #820 - Allow setting label/color individually
        if (arc.eventDescId && this.data.eventDescs[arc.eventDescId] &&
          this.data.eventDescs[arc.eventDescId].labelText) {
          labels = [this.data.eventDescs[arc.eventDescId].labelText];
        }
        // WEBANNO EXTENSION END - #820 - Allow setting label/color individually

        $.each(labels, (labelNo, label) => arcTexts[label] = true);
      });

      var arcSizes = this.getTextMeasurements(arcTexts, { 'class': 'arcs' });
      sizes.arcs = arcSizes;
    }

    adjustTowerAnnotationSizes() {
      // find biggest annotation in each tower
      $.each(this.data.towers, (towerNo, tower) => {
        var maxWidth = 0;

        $.each(tower, (fragmentNo, fragment) => {
          var width = this.data.sizes.fragments.widths[fragment.glyphedLabelText];
          if (width > maxWidth) {
            maxWidth = width;
          }
        }); // tower

        $.each(tower, (fragmentNo, fragment) => fragment.width = maxWidth);
      }); // data.towers
    }

    makeArrow(defs, spec) {
      var parsedSpec = spec.split(',');
      var type = parsedSpec[0];
      if (type == 'none') {
        return;
      }

      var width = 5;
      var height = 5;
      var color = "black";
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
        width = height = 5;
        color = parsedSpec[1] || 'black';
      }
      // hash needs to be replaced as IDs don't permit it.
      var arrowId = 'arrow_' + spec.replace(/#/g, '').replace(/,/g, '_');

      var arrow;
      if (type == 'triangle') {
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

    renderDataReal(sourceData) {
      var that = this;

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

      // clear the SVG
      this.svg.clear(true);

      if (!this.data || this.data.length == 0) {
        return;
      }

      // WEBANNO EXTENSION BEGIN - #588 - Better handling of setting brat font size 
      this.$svg.css("font-size", this.fontZoom + "%");
      this.sentNumMargin = 40 * (this.fontZoom / 100.0);
      // WEBANNO EXTENSION END - #588 - Better handling of setting brat font size 
      // WEBANNO EXTENSION BEGIN - Flex-Layout - need to discover scrollbar width programmatically
      /*
      // establish the width according to the enclosing element
      this.canvasWidth = this.forceWidth || $svgDiv.width();
      */
      // establish the width according to the enclosing element
      this.baseCanvasWidth = this.forceWidth || this.$svgDiv.width();
      this.canvasWidth = this.forceWidth || (this.$svgDiv.width() - $.scrollbarWidth());
      // WEBANNO EXTENSION END - Flex-Layout - need to discover scrollbar width programmatically
      // WEBANNO EXTENSION BEGIN - #289 - Layout slightly shifts when SVG is rendered 
      // Take hairline border of SVG into account
      this.canvasWidth -= 4;
      // WEBANNO EXTENSION END - #289 - Layout slightly shifts when SVG is rendered 
      var defs = this.addHeaderAndDefs();

      // BEGIN WEBANNO EXTENSION - #724 - Cross-row selection is jumpy
      /*
              var backgroundGroup = svg.group({ 'class': 'background' });
      */
      var backgroundGroup = this.svg.group({ 'class': 'background', 'pointer-events': 'none' });
      // END WEBANNO EXTENSION - #724 - Cross-row selection is jumpy
      var glowGroup = this.svg.group({ 'class': 'glow' });
      this.highlightGroup = this.svg.group({ 'class': 'highlight' });
      var textGroup = this.svg.group({ 'class': 'text' });

      Util.profileEnd('init');
      Util.profileStart('measures');

      var sizes = this.getTextAndSpanTextMeasurements();
      this.data.sizes = sizes;

      this.adjustTowerAnnotationSizes();
      var maxTextWidth = 0;
      for (var text in sizes.texts.widths) {
        if (sizes.texts.widths.hasOwnProperty(text)) {
          var width = sizes.texts.widths[text];
          if (width > maxTextWidth)
            maxTextWidth = width;
        }
      }

      Util.profileEnd('measures');
      Util.profileStart('chunks');

      // WEBANNO EXTENSION BEGIN - RTL support - [currentX] initial position
      /*
      var currentX = Configuration.visual.margin.x + this.sentNumMargin + rowPadding;
      */
      var currentX;
      if (this.rtlmode) {
        currentX = this.canvasWidth - (Configuration.visual.margin.x + this.sentNumMargin + this.rowPadding);
      } else {
        currentX = Configuration.visual.margin.x + this.sentNumMargin + this.rowPadding;
      }
      // WEBANNO EXTENSION END
      var rows = [];
      var fragmentHeights = [];
      var sentenceToggle = 0;
      // WEBANNO EXTENSION BEGIN - #180 - Use sentence number offset received from server
      var sentenceNumber = sourceData.sentence_number_offset;
      // WEBANNO EXTENSION END
      var row = new Row(this.svg);
      row.sentence = sentenceNumber;
      row.backgroundIndex = sentenceToggle;
      row.index = 0;
      var rowIndex = 0;
      var twoBarWidths; // HACK to avoid measuring space's width
      var openTextHighlights = {};
      var textMarkedRows = [];

      this.addArcTextMeasurements(sizes);

      // reserve places for spans
      var floors = [];
      var reservations = []; // reservations[chunk][floor] = [[from, to, headroom]...]
      for (var i = 0; i <= this.data.lastFragmentIndex; i++) {
        reservation[i] = {};
      }
      var inf = 1.0 / 0.0;

      $.each(this.data.spanDrawOrderPermutation, (spanIdNo, spanId) => {
        var span = this.data.spans[spanId];
        var spanDesc = this.spanTypes[span.type];
        var bgColor = ((spanDesc && spanDesc.bgColor) ||
          (this.spanTypes.SPAN_DEFAULT && this.spanTypes.SPAN_DEFAULT.bgColor) || '#ffffff');

        if (bgColor == "hidden") {
          span.hidden = true;
          return;
        }

        var f1 = span.fragments[0];
        var f2 = span.fragments[span.fragments.length - 1];

        var x1 = (f1.curly.from + f1.curly.to - f1.width) / 2 -
          Configuration.visual.margin.x - (sizes.fragments.height / 2);
        var i1 = f1.chunk.index;

        var x2 = (f2.curly.from + f2.curly.to + f2.width) / 2 +
          Configuration.visual.margin.x + (sizes.fragments.height / 2);
        var i2 = f2.chunk.index;

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
        var carpet = 0;
        var outside = true;
        var thisCurlyHeight = span.drawCurly ? Configuration.visual.curlyHeight : 0;
        var height = sizes.fragments.height + thisCurlyHeight + Configuration.visual.boxSpacing +
          2 * Configuration.visual.margin.y - 3;
        $.each(floors, (floorNo, floor) => {
          var floorAvailable = true;
          for (var i = i1; i <= i2; i++) {
            if (!(reservations[i] && reservations[i][floor])) {
              continue;
            }
            var from = (i == i1) ? x1 : -inf;
            var to = (i == i2) ? x2 : inf;
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
        var reslen = reservations.length;
        var makeNewFloorIfNeeded = function (floor) {
          var floorNo = $.inArray(floor, floors);
          if (floorNo == -1) {
            floors.push(floor);
            floors.sort(Util.cmp);
            floorNo = $.inArray(floor, floors);
            if (floorNo != 0) {
              // copy reservations from the floor below
              var parquet = floors[floorNo - 1];
              for (var i = 0; i <= reslen; i++) {
                if (reservations[i]) {
                  if (!reservations[i][parquet]) {
                    reservations[i][parquet] = [];
                  }
                  var footroom = floor - parquet;
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
        var ceiling = carpet + height;
        var ceilingNo = makeNewFloorIfNeeded(ceiling);
        var carpetNo = makeNewFloorIfNeeded(carpet);
        // make the reservation
        var floor, floorNo;
        for (floorNo = carpetNo; (floor = floors[floorNo]) !== undefined && floor < ceiling; floorNo++) {
          var headroom = ceiling - floor;
          for (var i = i1; i <= i2; i++) {
            var from = (i == i1) ? x1 : 0;
            var to = (i == i2) ? x2 : inf;
            if (!reservations[i])
              reservations[i] = {};
            if (!reservations[i][floor])
              reservations[i][floor] = [];
            reservations[i][floor].push([from, to, headroom]); // XXX maybe add fragment; probably unnecessary
          }
        }
        span.floor = carpet + thisCurlyHeight;
      });

      $.each(this.data.chunks, (chunkNo, chunk) =>{
        var spaceWidth = 0;
        if (chunk.lastSpace) {
          // WEBANNO EXTENSION BEGIN - #1315 - Various issues with line-oriented mode
          /*
          var spaceLen = chunk.lastSpace.length || 0;
          var spacePos = chunk.lastSpace.lastIndexOf("\n") + 1;
          if (spacePos || !chunkNo || !chunk.sentence) {
            // indent if
            // * non-first paragraph first word
            // * first paragraph first word
            // * non-first word in a line
            // don't indent if
            // * the first word in a non-paragraph line
            for (var i = spacePos; i < spaceLen; i++) spaceWidth += spaceWidths[chunk.lastSpace[i]] || 0;
            currentX += spaceWidth;
          }
          */
          var spaceLen = chunk.lastSpace.length || 0;
          var spacePos;
          if (chunk.sentence) {
            // If this is line-initial spacing, fetch the sentence to which the chunk belongs
            // so we can determine where it begins
            var sentFrom = sourceData.sentence_offsets[chunk.sentence - sourceData.sentence_number_offset][0];
            spacePos = spaceLen - (chunk.from - sentFrom);
          }
          else {
            spacePos = 0;
          }
          for (var i = spacePos; i < spaceLen; i++) {
            spaceWidth += this.spaceWidths[chunk.lastSpace[i]] * (this.fontZoom / 100.0) || 0;
          }
          currentX += this.rtlmode ? -spaceWidth : spaceWidth;
          // WEBANNO EXTENSION END - #1315 - Various issues with line-oriented mode
        }

        chunk.group = this.svg.group(row.group);
        chunk.highlightGroup = this.svg.group(chunk.group);

        var y = 0;
        var minArcDist;
        var hasLeftArcs, hasRightArcs, hasInternalArcs;
        var hasAnnotations;
        var chunkFrom = Infinity;
        var chunkTo = 0;
        var chunkHeight = 0;
        var spacing = 0;
        var spacingChunkId = null;
        var spacingRowBreak = 0;

        $.each(chunk.fragments, (fragmentNo, fragment) => {
          var span = fragment.span;

          if (span.hidden) {
            return;
          }

          var spanDesc = this.spanTypes[span.type];
          var bgColor = ((spanDesc && spanDesc.bgColor) ||
            (this.spanTypes.SPAN_DEFAULT && this.spanTypes.SPAN_DEFAULT.bgColor) || '#ffffff');
          var fgColor = ((spanDesc && spanDesc.fgColor) ||
            (this.spanTypes.SPAN_DEFAULT &&
              this.spanTypes.SPAN_DEFAULT.fgColor) || '#000000');
          var borderColor = ((spanDesc && spanDesc.borderColor) ||
            (this.spanTypes.SPAN_DEFAULT &&
              this.spanTypes.SPAN_DEFAULT.borderColor) || '#000000');

          // WEBANNO EXTENSION BEGIN - #820 - Allow setting label/color individually
          if (span.color) {
            bgColor = span.color;
            fgColor = bgToFgColor(bgColor);
            borderColor = 'darken';
          }
          // WEBANNO EXTENSION END

          // special case: if the border 'color' value is 'darken',
          // then just darken the BG color a bit for the border.
          if (borderColor == 'darken') {
            borderColor = Util.adjustColorLightness(bgColor, -0.6);
          }

          fragment.group = this.svg.group(chunk.group, {
            'class': 'span',
          });

          var fragmentHeight = 0;

          if (!y) {
            y = -sizes.texts.height;
          }
          // x : center of fragment on x axis
          var x = (fragment.curly.from + fragment.curly.to) / 2;

          // XXX is it maybe sizes.texts?
          var yy = y + sizes.fragments.y;
          // hh : fragment height
          var hh = sizes.fragments.height;
          // ww : fragment width
          var ww = fragment.width;
          // xx : left edge of fragment
          var xx = x - ww / 2;

          // text margin fine-tuning
          yy += this.boxTextMargin.y;
          hh -= 2 * this.boxTextMargin.y;
          xx += this.boxTextMargin.x;
          ww -= 2 * this.boxTextMargin.x;
          var rectClass = 'span_' + (span.cue || span.type) + ' span_default'; // TODO XXX first part unneeded I think; remove

          // attach e.g. "False_positive" into the type
          if (span.comment && span.comment.type) { 
            rectClass += ' ' + span.comment.type; 
          }

          // inner coordinates of fragment (excluding margins)
          var bx = xx - Configuration.visual.margin.x - this.boxTextMargin.x;
          var by = yy - Configuration.visual.margin.y;
          var bw = ww + 2 * Configuration.visual.margin.x;
          var bh = hh + 2 * Configuration.visual.margin.y;

          if (this.roundCoordinates) {
            x = (x | 0) + 0.5;
            bx = (bx | 0) + 0.5;
          }

          var shadowRect;
          var markedRect;
          if (span.marked) {
            markedRect = this.svg.rect(chunk.highlightGroup,
              bx - this.markedSpanSize, by - this.markedSpanSize,
              bw + 2 * this.markedSpanSize, bh + 2 * this.markedSpanSize, {
              filter: 'url(#Gaussian_Blur)',
              'class': "shadow_EditHighlight",
              rx: this.markedSpanSize,
              ry: this.markedSpanSize,
            });
            // WEBANNO EXTENSION BEGIN - Issue #1319 - Glowing highlight causes 100% CPU load
            /*
                          svg.other(markedRect, 'animate', {
                            'data-type': span.marked,
                            attributeName: 'fill',
                            values: (span.marked == 'match'? highlightMatchSequence
                                      : highlightSpanSequence),
                            dur: highlightDuration,
                            repeatCount: 'indefinite',
                            begin: 'indefinite'
                          });
            */
            // WEBANNO EXTENSION END - Issue #1319 - Glowing highlight causes 100% CPU load
          }
          // BEGIN WEBANNO EXTENSION - Issue #273 - Layout doesn't space out labels sufficiently 
          // Nicely spread out labels/text and leave space for mark highlight such that adding
          // the mark doesn't change the overall layout
          chunkFrom = Math.min(bx - this.markedSpanSize, chunkFrom);
          chunkTo = Math.max(bx + bw + this.markedSpanSize, chunkTo);
          fragmentHeight = Math.max(bh + 2 * this.markedSpanSize, fragmentHeight);
          // WEBANNO EXTENSION END - Issue #273 - Layout doesn't space out labels sufficiently 
          // .match() removes unconfigured shadows, which were
          // always showing up as black.
          // TODO: don't hard-code configured shadowclasses.
          if (span.shadowClass &&
            span.shadowClass.match('True_positive|False_positive|False_negative|AnnotationError|AnnotationWarning|AnnotatorNotes|Normalized|AnnotationIncomplete|AnnotationUnconfirmed|rectEditHighlight|EditHighlight_arc|MissingAnnotation|ChangedAnnotation ')) {
            shadowRect = svg.rect(fragment.group,
              bx - rectShadowSize, by - rectShadowSize,
              bw + 2 * rectShadowSize, bh + 2 * rectShadowSize, {
              'class': 'shadow_' + span.shadowClass,
              filter: 'url(#Gaussian_Blur)',
              rx: rectShadowRounding,
              ry: rectShadowRounding,
            });
            chunkFrom = Math.min(bx - rectShadowSize, chunkFrom);
            chunkTo = Math.max(bx + bw + rectShadowSize, chunkTo);
            fragmentHeight = Math.max(bh + 2 * rectShadowSize, fragmentHeight);
          }
          /*
          fragment.rect = svg.rect(fragment.group,
              bx, by, bw, bh, {

              'class': rectClass,
              fill: bgColor,
              stroke: borderColor,
              rx: Configuration.visual.margin.x,
              ry: Configuration.visual.margin.y,
              'data-span-id': span.id,
              'data-fragment-id': span.segmentedOffsetsMap[fragment.id],
              'strokeDashArray': span.attributeMerge.dashArray,
            });*/
          var bx1 = bx;
          var bx2 = bx1 + bw;
          var by1 = yy - Configuration.visual.margin.y - span.floor;
          var by2 = by1 + bh;
          var poly = [];
          if (span.clippedAtStart && span.fragments[0] == fragment) {
            poly.push([bx1, by2]);
            poly.push([bx1 - bh / 2, (by1 + by2) / 2]);
            poly.push([bx1, by1]);
          }
          else {
            poly.push([bx1, by2]);
            poly.push([bx1, by1]);
          }

          if (span.clippedAtEnd && span.fragments[span.fragments.length - 1] == fragment) {
            poly.push([bx2, by1]);
            poly.push([bx2 + bh / 2, (by1 + by2) / 2]);
            poly.push([bx2, by2]);
          }
          else {
            poly.push([bx2, by1]);
            poly.push([bx2, by2]);
          }

          fragment.rect = this.svg.polygon(fragment.group, poly, {
            'class': rectClass,
            fill: bgColor,
            stroke: borderColor,
            rx: Configuration.visual.margin.x,
            ry: Configuration.visual.margin.y,
            'data-span-id': span.id,
            'data-fragment-id': span.segmentedOffsetsMap[fragment.id],
            'strokeDashArray': span.attributeMerge.dashArray,
          });

          // BEGIN WEBANNO EXTENSION - WebAnno does not support marking normalizations
          /*
          // TODO XXX: quick nasty hack to allow normalizations
          // to be marked visually; do something cleaner!
          if (span.normalized) {
            $(fragment.rect).addClass(span.normalized);
          }
          */
          // WEBANNO EXTENSION END
          // BEGIN WEBANNO EXTENSION - RTL support
          fragment.left = bx; // TODO put it somewhere nicer?

          // WEBANNO EXTENSION END
          fragment.right = bx + bw; // TODO put it somewhere nicer?
          if (!(span.shadowClass || span.marked)) {
            chunkFrom = Math.min(bx, chunkFrom);
            chunkTo = Math.max(bx + bw, chunkTo);
            fragmentHeight = Math.max(bh, fragmentHeight);
          }

          fragment.rectBox = { x: bx, y: by - span.floor, width: bw, height: bh };
          fragment.height = span.floor + hh + 3 * Configuration.visual.margin.y + Configuration.visual.curlyHeight + Configuration.visual.arcSpacing;
          var spacedTowerId = fragment.towerId * 2;
          if (!fragmentHeights[spacedTowerId] || fragmentHeights[spacedTowerId] < fragment.height) {
            fragmentHeights[spacedTowerId] = fragment.height;
          }
          $(fragment.rect).attr('y', yy - Configuration.visual.margin.y - span.floor);
          if (shadowRect) {
            $(shadowRect).attr('y', yy - rectShadowSize - Configuration.visual.margin.y - span.floor);
          }
          if (markedRect) {
            $(markedRect).attr('y', yy - this.markedSpanSize - Configuration.visual.margin.y - span.floor);
          }
          if (span.attributeMerge.box === "crossed") {
            this.svg.path(fragment.group, svg.createPath().
              move(xx, yy - Configuration.visual.margin.y - span.floor).
              line(xx + fragment.width,
                yy + hh + Configuration.visual.margin.y - span.floor),
              { 'class': 'boxcross' });
            this.svg.path(fragment.group, svg.createPath().
              move(xx + fragment.width, yy - Configuration.visual.margin.y - span.floor).
              line(xx, yy + hh + Configuration.visual.margin.y - span.floor),
              { 'class': 'boxcross' });
          }
          this.svg.text(fragment.group, x, y - span.floor, this.data.spanAnnTexts[fragment.glyphedLabelText], { fill: fgColor });

          // Make curlies to show the fragment
          if (fragment.drawCurly) {
            var curlyColor = 'grey';
            if (this.coloredCurlies) {
              var spanDesc = this.spanTypes[span.type];
              var bgColor = ((spanDesc && spanDesc.bgColor) ||
                (this.spanTypes.SPAN_DEFAULT && this.spanTypes.SPAN_DEFAULT.fgColor) ||
                '#000000');
              // WEBANNO EXTENSION BEGIN - #820 - Allow setting label/color individually
              if (span.color) {
                bgColor = span.color;
              }
              // WEBANNO EXTENSION END
              curlyColor = Util.adjustColorLightness(bgColor, -0.6);
            }

            var bottom = yy + hh + Configuration.visual.margin.y - span.floor + 1;
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
            chunkFrom = Math.min(fragment.curly.from, chunkFrom);
            chunkTo = Math.max(fragment.curly.to, chunkTo);
            fragmentHeight = Math.max(Configuration.visual.curlyHeight, fragmentHeight);
          }

          if (fragment == span.headFragment) {
            // find the gap to fit the backwards arcs, but only on
            // head fragment - other fragments don't have arcs
            $.each(span.incoming, (arcId, arc) => {
              var leftSpan = this.data.spans[arc.origin];
              var origin = leftSpan.headFragment.chunk;
              var border;
              if (chunk.index == origin.index) {
                hasInternalArcs = true;
              }
              if (origin.row) {
                var labels = Util.getArcLabels(this.spanTypes, leftSpan.type, arc.type, this.relationTypesHash);
                if (!labels.length) {
                  labels = [arc.type];
                }
                // WEBANNO EXTENSION BEGIN - #820 - Allow setting label/color individually
                if (arc.eventDescId && this.data.eventDescs[arc.eventDescId]) {
                  if (this.data.eventDescs[arc.eventDescId].labelText) {
                    labels = [this.data.eventDescs[arc.eventDescId].labelText];
                  }
                }
                // WEBANNO EXTENSION END
                // WEBANNO EXTENSION BEGIN - RTL support - chunk spacing with arcs                  
                /*
                if (origin.row.index == rowIndex) {
                  // same row, but before this
                  border = origin.translation.x + leftSpan.fragments[leftSpan.fragments.length - 1].right;
                } else {
                  border = Configuration.visual.margin.x + this.sentNumMargin + this.rowPadding;
                }
                */
                if (origin.row.index == rowIndex) {
                  border = origin.translation.x + leftSpan.fragments[leftSpan.fragments.length - 1].right;
                } else {
                  if (this.rtlmode) {
                    border = 0;
                  } else {
                    border = Configuration.visual.margin.x + this.sentNumMargin + this.rowPadding;
                  }
                }
                // WEBANNO EXTENSION END
                var labelNo = Configuration.abbrevsOn ? labels.length - 1 : 0;
                var smallestLabelWidth = sizes.arcs.widths[labels[labelNo]] + 2 * this.minArcSlant;
                // WEBANNO EXTENSION BEGIN - RTL support - chunk spacing with arcs
                /*
                var gap = currentX + bx - border;
                */
                var gap = Math.abs(currentX + (this.rtlmode ? -bx : bx) - border);
                // WEBANNO EXTENSION END
                var arcSpacing = smallestLabelWidth - gap;
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
              var leftSpan = this.data.spans[arc.target];
              var target = leftSpan.headFragment.chunk;
              var border;
              if (target.row) {
                var labels = Util.getArcLabels(this.spanTypes, span.type, arc.type, this.relationTypesHash);
                if (!labels.length)
                  labels = [arc.type];
                // WEBANNO EXTENSION BEGIN - #820 - Allow setting label/color individually
                if (arc.eventDescId && this.data.eventDescs[arc.eventDescId]) {
                  if (this.data.eventDescs[arc.eventDescId].labelText) {
                    labels = [this.data.eventDescs[arc.eventDescId].labelText];
                  }
                }
                // WEBANNO EXTENSION END
                // WEBANNO EXTENSION BEGIN - RTL support - chunk spacing with arcs
                /*
                if (target.row.index == rowIndex) {
                  // same row, but before this
                  border = target.translation.x + leftSpan.fragments[leftSpan.fragments.length - 1].right;
                } else {
                  border = Configuration.visual.margin.x + this.sentNumMargin + this.rowPadding;
                }
                */
                if (target.row.index == rowIndex) {
                  // same row, but before this
                  border = target.translation.x + leftSpan.fragments[leftSpan.fragments.length - 1].right;
                } else {
                  if (this.rtlmode) {
                    border = 0;
                  } else {
                    border = Configuration.visual.margin.x + this.sentNumMargin + this.rowPadding;
                  }
                }
                // WEBANNO EXTENSION END
                var labelNo = Configuration.abbrevsOn ? labels.length - 1 : 0;
                var smallestLabelWidth = sizes.arcs.widths[labels[labelNo]] + 2 * this.minArcSlant;
                // WEBANNO EXTENSION BEGIN - RTL support - chunk spacing with arcs
                /*
                var gap = currentX + bx - border;
                */
                var gap = Math.abs(currentX + (this.rtlmode ? -bx : bx) - border);
                // WEBANNO EXTENSION END
                var arcSpacing = smallestLabelWidth - gap;
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
        var textWidth = sizes.texts.widths[chunk.text];
        chunkHeight += sizes.texts.height;
        // WEBANNO EXTENSION BEGIN - RTL support - [boxX] adjustment for decoration
        /*
                  // If chunkFrom becomes negative, then boxX becomes positive
                  var boxX = -Math.min(chunkFrom, 0);
        */
        // If chunkFrom becomes negative (LTR) or chunkTo becomes positive (RTL), then boxX becomes positive
        var boxX = this.rtlmode ? chunkTo : -Math.min(chunkFrom, 0);
        // WEBANNO EXTENSION END         
        // WEBANNO EXTENSION BEGIN - RTL support - [boxWidth] calculation of boxWidth
        /*
                  var boxWidth =
                      Math.max(textWidth, chunkTo) -
                      Math.min(0, chunkFrom);
        */
        var boxWidth;
        if (this.rtlmode) {
          boxWidth = Math.max(textWidth, -chunkFrom) - Math.min(0, -chunkTo);
        }
        else {
          boxWidth = Math.max(textWidth, chunkTo) - Math.min(0, chunkFrom);
        }
        // WEBANNO EXTENSION END
        // if (hasLeftArcs) {
        // TODO change this with smallestLeftArc
        // var spacing = this.arcHorizontalSpacing - (currentX - lastArcBorder);
        // arc too small?
        // WEBANNO EXTENSION BEGIN - RTL support - [currentX] adjustment for spacing (arcs)
        /*
        if (spacing > 0) currentX += spacing;
        */
        if (spacing > 0) {
          currentX += this.rtlmode ? -spacing : spacing;
        }
        // WEBANNO EXTENSION END
        // }
        var rightBorderForArcs = hasRightArcs ? this.arcHorizontalSpacing : (hasInternalArcs ? this.arcSlant : 0);
        // WEBANNO EXTENSION BEGIN - RTL support - leftBorderForArcs
        var leftBorderForArcs = hasLeftArcs ? this.arcHorizontalSpacing : (hasInternalArcs ? this.arcSlant : 0);
        // WEBANNO EXTENSION END
        var lastX = currentX;
        var lastRow = row;

        // Is there a sentence break at the current chunk (i.e. it is the first chunk in a new
        // sentence) - if yes and the current sentence is not the same as the sentence to which 
        // the chunk belongs, then fill in additional rows
        if (chunk.sentence) {
          // WEBANNO EXTENSION BEGIN - #1315 - Various issues with line-oriented mode
          /*
          while (sentenceNumber < chunk.sentence) {
            sentenceNumber++;
            row.arcs = this.svg.group(row.group, { 'class': 'arcs' });
            rows.push(row);
            row = new Row(this.svg);
            sentenceToggle = 1 - sentenceToggle;
            row.backgroundIndex = sentenceToggle;
            row.index = ++rowIndex;
          }
          sentenceToggle = 1 - sentenceToggle;
          */
          while (sentenceNumber < chunk.sentence - 1) {
            sentenceNumber++;
            row.arcs = this.svg.group(row.group, { 'class': 'arcs' });
            rows.push(row);

            row = new Row(this.svg);
            row.sentence = sentenceNumber;
            sentenceToggle = 1 - sentenceToggle;
            row.backgroundIndex = sentenceToggle;
            row.index = ++rowIndex;
          }
          // Not changing row background color here anymore - we do this later now when the next
          // row is added
          // WEBANNO EXTENSION END - #1315 - Various issues with line-oriented mode
        }

        // WEBANNO EXTENSION BEGIN - RTL support - soft-wrap long sentences
        /*
        if (chunk.sentence ||
            currentX + boxWidth + rightBorderForArcs >= this.canvasWidth - 2 * Configuration.visual.margin.x) {
        */
        var chunkDoesNotFit = false;
        if (this.rtlmode) {
          chunkDoesNotFit = currentX - boxWidth - leftBorderForArcs <=
            2 * Configuration.visual.margin.x;
        }
        else {
          chunkDoesNotFit = currentX + boxWidth + rightBorderForArcs >=
            this.canvasWidth - 2 * Configuration.visual.margin.x;
        }

        // WEBANNO EXTENSION BEGIN - #1315 - Various issues with line-oriented mode
        if (chunk.sentence > sourceData.sentence_number_offset || chunkDoesNotFit) {
          // WEBANNO EXTENSION END - #1315 - Various issues with line-oriented mode
          // WEBANNO EXTENSION END
          // the chunk does not fit
          row.arcs = this.svg.group(row.group, { 'class': 'arcs' });
          // WEBANNO EXTENSION BEGIN - RTL support - [currentX] reset after soft-wrap
          /*
          // TODO: related to issue #571
          // replace arcHorizontalSpacing with a calculated value
          currentX = Configuration.visual.margin.x + sentNumMargin + rowPadding +
              (hasLeftArcs ? this.arcHorizontalSpacing : (hasInternalArcs ? this.arcSlant : 0)) +
              spaceWidth;
          */
          // WEBANNO EXTENSION BEGIN - #1315 - Various issues with line-oriented mode
          var indent = 0;
          if (chunk.lastSpace) {
            var spaceLen = chunk.lastSpace.length || 0;
            var spacePos;
            if (chunk.sentence) {
              // If this is line-initial spacing, fetch the sentence to which the chunk belongs
              // so we can determine where it begins
              var sentFrom = sourceData.sentence_offsets[chunk.sentence - sourceData.sentence_number_offset][0];
              spacePos = spaceLen - (chunk.from - sentFrom);
            }
            else {
              spacePos = 0;
            }
            for (var i = spacePos; i < spaceLen; i++) {
              indent += this.spaceWidths[chunk.lastSpace[i]] * (this.fontZoom / 100.0) || 0;
            }
          }

          if (this.rtlmode) {
            currentX = this.canvasWidth - (Configuration.visual.margin.x + this.sentNumMargin + this.rowPadding +
              (hasRightArcs ? this.arcHorizontalSpacing : (hasInternalArcs ? this.arcSlant : 0)) /* +
                          spaceWidth*/
              - indent);
          }
          else {
            currentX = Configuration.visual.margin.x + this.sentNumMargin + this.rowPadding +
              (hasLeftArcs ? this.arcHorizontalSpacing : (hasInternalArcs ? this.arcSlant : 0)) /*+
                        spaceWidth*/
              + indent;
          }
          // WEBANNO EXTENSION END - #1315 - Various issues with line-oriented mode
          // WEBANNO EXTENSION END - RTL support - [currentX] reset after soft-wrap
          if (hasLeftArcs) {
            var adjustedCurTextWidth = sizes.texts.widths[chunk.text] + this.arcHorizontalSpacing;
            if (adjustedCurTextWidth > maxTextWidth) {
              maxTextWidth = adjustedCurTextWidth;
            }
          }
          if (spacingRowBreak > 0) {
            // WEBANNO EXTENSION BEGIN - RTL support - [currentX] adjustment for spacingRowBreak (for arcs)
            /*
            currentX += spacingRowBreak;
            */
            currentX += this.rtlmode ? -spacingRowBreak : spacingRowBreak;
            // WEBANNO EXTENSION END
            spacing = 0; // do not center intervening elements
          }

          // new row
          rows.push(row);

          this.svg.remove(chunk.group);
          row = new Row(this.svg);
          // WEBANNO EXTENSION BEGIN - #1315 - Various issues with line-oriented mode
          // Change row background color if a new sentence is starting
          if (chunk.sentence) {
            sentenceToggle = 1 - sentenceToggle;
          }
          // WEBANNO EXTENSION END - #1315 - Various issues with line-oriented mode
          row.backgroundIndex = sentenceToggle;
          row.index = ++rowIndex;
          this.svg.add(row.group, chunk.group);
          chunk.group = row.group.lastElementChild;
          $(chunk.group).children("g[class='span']").
            each((index, element) => chunk.fragments[index].group = element);
          $(chunk.group).find("rect[data-span-id]").
            each((index, element) => chunk.fragments[index].rect = element);
        }

        // break the text highlights when the row breaks
        if (row.index !== lastRow.index) {
          $.each(openTextHighlights, (textId, textDesc) => {
            if (textDesc[3] != lastX) {
              // WEBANNO EXTENSION BEGIN - RTL support - breaking highlights (?)
              /*
              var newDesc = [lastRow, textDesc[3], lastX + boxX, textDesc[4]];
              */
              var newDesc;
              if (this.rtlmode) {
                newDesc = [lastRow, textDesc[3], lastX - boxX, textDesc[4]];
              } else {
                newDesc = [lastRow, textDesc[3], lastX + boxX, textDesc[4]];
              }
              // WEBANNO EXTENSION END
              textMarkedRows.push(newDesc);
            }
            textDesc[3] = currentX;
          });
        }

        // open text highlights
        $.each(chunk.markedTextStart, (textNo, textDesc) => {
          // WEBANNO EXTENSION BEGIN - RTL support - breaking highlights (?)
          /*
          textDesc[3] += currentX + boxX;
          */
          textDesc[3] += currentX + (this.rtlmode ? -boxX : boxX);
          // WEBANNO EXTENSION END
          openTextHighlights[textDesc[0]] = textDesc;
        });

        // close text highlights
        $.each(chunk.markedTextEnd, (textNo, textDesc) => {
          // WEBANNO EXTENSION BEGIN - RTL support - breaking highlights (?)
          /*
          textDesc[3] += currentX + boxX;
          */
          textDesc[3] += currentX + (this.rtlmode ? -boxX : boxX);
          // WEBANNO EXTENSION END
          var startDesc = openTextHighlights[textDesc[0]];
          delete openTextHighlights[textDesc[0]];
          markedRow = [row, startDesc[3], textDesc[3], startDesc[4]];
          textMarkedRows.push(markedRow);
        });

        // XXX check this - is it used? should it be lastRow?
        if (hasAnnotations) {
          row.hasAnnotations = true;
        }

        // WEBANNO EXTENSION BEGIN - #1315 - Various issues with line-oriented mode
        /*
        if (chunk.sentence) {
        */
        if (chunk.sentence > sourceData.sentence_number_offset) {
          // WEBANNO EXTENSION END - #1315 - Various issues with line-oriented mode
          row.sentence = ++sentenceNumber;
        }

        if (spacing > 0) {
          // if we added a gap, center the intervening elements
          spacing /= 2;
          var firstChunkInRow = row.chunks[row.chunks.length - 1];
          if (firstChunkInRow === undefined) {
            console.log('warning: firstChunkInRow undefined, chunk:', chunk);
          } else { // valid firstChunkInRow
            if (spacingChunkId < firstChunkInRow.index) {
              spacingChunkId = firstChunkInRow.index + 1;
            }
            for (var chunkIndex = spacingChunkId; chunkIndex < chunk.index; chunkIndex++) {
              var movedChunk = this.data.chunks[chunkIndex];
              this.translate(movedChunk, movedChunk.translation.x + spacing, 0);
              movedChunk.textX += spacing;
            }
          }
        }

        row.chunks.push(chunk);
        chunk.row = row;

        // WEBANNO EXTENSION BEGIN - RTL support - chunk - translate position (based on currentX/boxX)
        /*
        this.translate(chunk, currentX + boxX, 0);
        chunk.textX = currentX + boxX;
        */
        this.translate(chunk, currentX + (this.rtlmode ? -boxX : boxX), 0);
        chunk.textX = currentX + (this.rtlmode ? -boxX : boxX);
        // WEBANNO EXTENSION END
        // WEBANNO EXTENSION BEGIN - RTL support - [currentX] adjustment for boxWidth (chunk)
        /*
        currentX += boxWidth;
        */
        currentX += this.rtlmode ? -boxWidth : boxWidth;
        // WEBANNO EXTENSION END
      }); // chunks

      // WEBANNO EXTENSION BEGIN - #1315 - Various issues with line-oriented mode
      // Add trailing empty rows
      while (sentenceNumber < (sourceData.sentence_offsets.length + sourceData.sentence_number_offset - 1)) {
        sentenceNumber++;
        row.arcs = this.svg.group(row.group, { 'class': 'arcs' });
        rows.push(row);
        row = new Row(this.svg);
        row.sentence = sentenceNumber;
        sentenceToggle = 1 - sentenceToggle;
        row.backgroundIndex = sentenceToggle;
        row.index = ++rowIndex;
      }
      // WEBANNO EXTENSION BEGIN - #1315 - Various issues with line-oriented mode
      // finish the last row
      row.arcs = this.svg.group(row.group, { 'class': 'arcs' });
      rows.push(row);

      Util.profileEnd('chunks');
      Util.profileStart('arcsPrep');

      var arrows = {};
      var arrow = this.makeArrow(defs, 'none');
      if (arrow) {
        arrows['none'] = arrow;
      }

      var len = fragmentHeights.length;
      for (var i = 0; i < len; i++) {
        if (!fragmentHeights[i] || fragmentHeights[i] < Configuration.visual.arcStartHeight) {
          fragmentHeights[i] = Configuration.visual.arcStartHeight;
        }
      }

      // find out how high the arcs have to go
      $.each(this.data.arcs, (arcNo, arc) => {
        arc.jumpHeight = 0;
        var fromFragment = this.data.spans[arc.origin].headFragment;
        var toFragment = this.data.spans[arc.target].headFragment;
        if (fromFragment.span.hidden || toFragment.span.hidden) {
          arc.hidden = true;
          return;
        }
        if (fromFragment.towerId > toFragment.towerId) {
          var tmp = fromFragment; fromFragment = toFragment; toFragment = tmp;
        }
        var from, to;
        if (fromFragment.chunk.index == toFragment.chunk.index) {
          from = fromFragment.towerId;
          to = toFragment.towerId;
        } else {
          from = fromFragment.towerId + 1;
          to = toFragment.towerId - 1;
        }
        for (var i = from; i <= to; i++) {
          if (arc.jumpHeight < fragmentHeights[i * 2]) {
            arc.jumpHeight = fragmentHeights[i * 2];
          }
        }
      });

      // sort the arcs
      this.data.arcs.sort((a, b) => {
        // first write those that have less to jump over
        var tmp = a.jumpHeight - b.jumpHeight;
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

      // see which fragments are in each row
      var heightsStart = 0;
      var heightsRowsAdded = 0;
      $.each(rows, (rowId, row) => {
        var seenFragment = false;
        row.heightsStart = row.heightsEnd = heightsStart;
        $.each(row.chunks, (chunkId, chunk) => {
          if (chunk.lastFragmentIndex !== undefined) {
            // fragmentful chunk
            seenFragment = true;
            var heightsIndex = chunk.lastFragmentIndex * 2 + heightsRowsAdded;
            if (row.heightsEnd < heightsIndex) {
              row.heightsEnd = heightsIndex;
            }
            var heightsIndex = chunk.firstFragmentIndex * 2 + heightsRowsAdded;
            if (row.heightsStart > heightsIndex) {
              row.heightsStart = heightsIndex;
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

      // draw the drag arc marker
      var arrowhead = this.svg.marker(defs, 'drag_arrow',
        5, 2.5, 5, 5, 'auto',
        {
          markerUnits: 'strokeWidth',
          'class': 'drag_fill',
        });
        this.svg.polyline(arrowhead, [[0, 0], [5, 2.5], [0, 5], [0.2, 2.5]]);
      var arcDragArc = this.svg.path(this.svg.createPath(), {
        markerEnd: 'url(#drag_arrow)',
        'class': 'drag_stroke',
        fill: 'none',
        visibility: 'hidden',
      });
      this.dispatcher.post('arcDragArcDrawn', [arcDragArc]);

      Util.profileEnd('arcsPrep');
      Util.profileStart('arcs');

      var arcCache = {};
      // add the arcs
      $.each(this.data.arcs, (arcNo, arc) => {
        if (arc.hidden) {
          return;
        }

        // separate out possible numeric suffix from type
        var noNumArcType;
        var splitArcType;
        if (arc.type) {
          splitArcType = arc.type.match(/^(.*)(\d*)$/);
          noNumArcType = splitArcType[1];
        }

        var originSpan = this.data.spans[arc.origin];
        var targetSpan = this.data.spans[arc.target];

        var leftToRight = originSpan.headFragment.towerId < targetSpan.headFragment.towerId;
        var left, right;
        if (leftToRight) {
          left = originSpan.headFragment;
          right = targetSpan.headFragment;
        } else {
          left = targetSpan.headFragment;
          right = originSpan.headFragment;
        }

        // fall back on relation types in case we still don't have
        // an arc description, with final fallback to unnumbered relation
        var arcDesc = this.relationTypesHash[arc.type] || this.relationTypesHash[noNumArcType];

        // if it's not a relationship, see if we can find it in span
        // descriptions
        // TODO: might make more sense to reformat this as dict instead
        // of searching through the list every type
        var spanDesc = this.spanTypes[originSpan.type];
        if (!arcDesc && spanDesc && spanDesc.arcs) {
          $.each(spanDesc.arcs, (arcDescNo, arcDescIter) => {
            if (arcDescIter.type == arc.type) {
              arcDesc = arcDescIter;
            }
          });
        }

        // last fall back on unnumbered type if not found in full
        if (!arcDesc && noNumArcType && noNumArcType != arc.type &&
          spanDesc && spanDesc.arcs) {
          $.each(spanDesc.arcs, (arcDescNo, arcDescIter) => {
            if (arcDescIter.type == noNumArcType) {
              arcDesc = arcDescIter;
            }
          });
        }

        // empty default
        if (!arcDesc) {
          arcDesc = {};
        }

        var color = ((arcDesc && arcDesc.color) ||
          (this.spanTypes.ARC_DEFAULT && this.spanTypes.ARC_DEFAULT.color) ||
          '#000000');
        if (color == 'hidden') {
          return;
        }

        // WEBANNO EXTENSION BEGIN - #820 - Allow setting label/color individually
        if (arc.eventDescId && this.data.eventDescs[arc.eventDescId]) {
          if (this.data.eventDescs[arc.eventDescId].color) {
            color = [this.data.eventDescs[arc.eventDescId].color];
          }
        }
        // WEBANNO EXTENSION END
        var symmetric = arcDesc && arcDesc.properties && arcDesc.properties.symmetric;
        var dashArray = arcDesc && arcDesc.dashArray;
        var arrowHead = ((arcDesc && arcDesc.arrowHead) ||
          (this.spanTypes.ARC_DEFAULT && this.spanTypes.ARC_DEFAULT.arrowHead) ||
          'triangle,5') + ',' + color;
        var labelArrowHead = ((arcDesc && arcDesc.labelArrow) ||
          (this.spanTypes.ARC_DEFAULT && this.spanTypes.ARC_DEFAULT.labelArrow) ||
          'triangle,5') + ',' + color;

        var leftBox = this.rowBBox(left);
        var rightBox = this.rowBBox(right);
        var leftRow = left.chunk.row.index;
        var rightRow = right.chunk.row.index;

        if (!arrows[arrowHead]) {
          var arrow = this.makeArrow(defs, arrowHead);
          if (arrow) {
            arrows[arrowHead] = arrow;
          }
        }
        if (!arrows[labelArrowHead]) {
          var arrow = this.makeArrow(defs, labelArrowHead);
          if (arrow) {
            arrows[labelArrowHead] = arrow;
          }
        }

        // find the next height
        var height;

        var fromIndex2, toIndex2;
        if (left.chunk.index == right.chunk.index) {
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
          // TODO: on at least Chrome, this doesn't make a difference:
          // the lines come out pixel-width even without it. Check.
          height += 0.5;
        }
        var leftSlantBound, rightSlantBound;

        var chunkReverse = false;
        var ufoCatcher = originSpan.headFragment.chunk.index == targetSpan.headFragment.chunk.index;
        if (ufoCatcher) {
          chunkReverse = leftBox.x + leftBox.width / 2 < rightBox.x + rightBox.width / 2;
        }
        var ufoCatcherMod = ufoCatcher ? chunkReverse ? -0.5 : 0.5 : 1;

        for (var rowIndex = leftRow; rowIndex <= rightRow; rowIndex++) {
          var row = rows[rowIndex];
          if (row.chunks.length) {
            row.hasAnnotations = true;

            if (this.collapseArcSpace) {
              var fromIndex2R = rowIndex == leftRow ? fromIndex2 : row.heightsStart;
              var toIndex2R = rowIndex == rightRow ? toIndex2 : row.heightsEnd;
              height = findArcHeight(fromIndex2R, toIndex2R, fragmentHeights);
            }

            var arcGroup = this.svg.group(row.arcs, {
              'data-from': arc.origin,
              'data-to': arc.target,
              'data-id': arc.eventDescId
            });
            var from, to;

            // WEBANNO EXTENSION BEGIN - RTL support - arc from
            /*
            if (rowIndex == leftRow) {
              from = leftBox.x + (chunkReverse ? 0 : leftBox.width);
            } else {
              from = this.sentNumMargin;
            }
            */
            if (rowIndex == leftRow) {
              if (this.rtlmode) {
                from = leftBox.x + (chunkReverse ? leftBox.width : 0);
              } else {
                from = leftBox.x + (chunkReverse ? 0 : leftBox.width);
              }
            } else {
              from = this.rtlmode ? this.canvasWidth - 2 * Configuration.visual.margin.y - this.sentNumMargin : this.sentNumMargin;
            }
            // WEBANNO EXTENSION END
            // WEBANNO EXTENSION BEGIN - RTL support - arc to
            /*
            if (rowIndex == rightRow) {
              to = rightBox.x + (chunkReverse ? rightBox.width : 0);
            } else {
              to = this.canvasWidth - 2 * Configuration.visual.margin.y;
            }
            */
            if (rowIndex == rightRow) {
              if (this.rtlmode) {
                to = rightBox.x + (chunkReverse ? 0 : rightBox.width);
              } else {
                to = rightBox.x + (chunkReverse ? rightBox.width : 0);
              }
            } else {
              to = this.rtlmode ? 0 : this.canvasWidth - 2 * Configuration.visual.margin.y;
            }
            // WEBANNO EXTENSION END
            var adjustHeight = true;
            if (this.collapseArcs) {
              var arcCacheKey = arc.type + ' ' + rowIndex + ' ' + from + ' ' + to;
              if (rowIndex == leftRow)
                arcCacheKey = left.span.id + ' ' + arcCacheKey;
              if (rowIndex == rightRow)
                arcCacheKey += ' ' + right.span.id;
              var rowHeight = arcCache[arcCacheKey];
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

            var originType = this.data.spans[arc.origin].type;
            var arcLabels = Util.getArcLabels(this.spanTypes, originType, arc.type, this.relationTypesHash);
            var labelText = Util.arcDisplayForm(this.spanTypes, originType, arc.type, this.relationTypesHash);
            // if (Configuration.abbrevsOn && !ufoCatcher && arcLabels) {
            if (Configuration.abbrevsOn && arcLabels) {
              var labelIdx = 1; // first abbreviation

              // strictly speaking 2*arcSlant would be needed to allow for
              // the full-width arcs to fit, but judged unabbreviated text
              // to be more important than the space for arcs.
              var maxLength = (to - from) - (this.arcSlant);
              while (sizes.arcs.widths[labelText] > maxLength &&
                arcLabels[labelIdx]) {
                labelText = arcLabels[labelIdx];
                labelIdx++;
              }
            }

            // WEBANNO EXTENSION BEGIN - #820 - Allow setting label/color individually
            if (arc.eventDescId && this.data.eventDescs[arc.eventDescId]) {
              if (this.data.eventDescs[arc.eventDescId].labelText) {
                labelText = this.data.eventDescs[arc.eventDescId].labelText;
              }
            }
            // WEBANNO EXTENSION END - #820 - Allow setting label/color individually
            var shadowGroup;
            if (arc.shadowClass || arc.marked) {
              shadowGroup = this.svg.group(arcGroup);
            }
            var options = {
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
            var svgText;
            if (!splitArcType[2]) {
              // no subscript, simple string suffices
              svgText = labelText;
            } else {
              // Need to parse out possible numeric suffixes to avoid
              // duplicating number in label and its subscript
              var splitLabelText = labelText.match(/^(.*?)(\d*)$/);
              var noNumLabelText = splitLabelText[1];

              svgText = this.svg.createText();
              // TODO: to address issue #453, attaching options also
              // to spans, not only primary text. Make sure there
              // are no problems with this.
              svgText.span(noNumLabelText, options);
              var subscriptSettings = {
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
            // (CSS dominant-baseline can't be used as not all SVG rendereds support it.)
            var baseline_shift = sizes.arcs.height / 4;
            var text = this.svg.text(arcGroup, (from + to) / 2, -height + baseline_shift,
              svgText, options);

            var width = sizes.arcs.widths[labelText];
            var textBox = {
              x: (from + to - width) / 2,
              width: width,
              y: -height - sizes.arcs.height / 2,
              height: sizes.arcs.height,
            };
            if (arc.marked) {
              var markedRect = this.svg.rect(shadowGroup,
                textBox.x - this.markedArcSize, textBox.y - this.markedArcSize,
                textBox.width + 2 * this.markedArcSize, textBox.height + 2 * this.markedArcSize, {
                filter: 'url(#Gaussian_Blur)',
                'class': "shadow_EditHighlight",
                rx: this.markedArcSize,
                ry: this.markedArcSize,
              });
              // WEBANNO EXTENSION BEGIN - Issue #1319 - Glowing highlight causes 100% CPU load
              /*
              this.svg.other(markedRect, 'animate', {
                'data-type': arc.marked,
                attributeName: 'fill',
                values: (arc.marked == 'match' ? highlightMatchSequence
                          : highlightArcSequence),
                dur: highlightDuration,
                repeatCount: 'indefinite',
                begin: 'indefinite'
              });
              */
              // WEBANNO EXTENSION END - Issue #1319 - Glowing highlight causes 100% CPU load
            }
            if (arc.shadowClass) {
              this.svg.rect(shadowGroup,
                textBox.x - arcLabelShadowSize,
                textBox.y - arcLabelShadowSize,
                textBox.width + 2 * arcLabelShadowSize,
                textBox.height + 2 * arcLabelShadowSize, {
                'class': 'shadow_' + arc.shadowClass,
                filter: 'url(#Gaussian_Blur)',
                rx: arcLabelShadowRounding,
                ry: arcLabelShadowRounding,
              });
            }
            var textStart = textBox.x;
            var textEnd = textBox.x + textBox.width;

            // adjust by margin for arc drawing
            textStart -= Configuration.visual.arcTextMargin;
            textEnd += Configuration.visual.arcTextMargin;

            if (from > to) {
              var tmp = textStart; textStart = textEnd; textEnd = tmp;
            }

            var path;

            if (this.roundCoordinates) {
              // don't ask
              height = (height | 0) + 0.5;
            }
            if (height > row.maxArcHeight)
              row.maxArcHeight = height;

            var myArrowHead = ((arcDesc && arcDesc.arrowHead) ||
              (this.spanTypes.ARC_DEFAULT && this.spanTypes.ARC_DEFAULT.arrowHead));
            var arrowName = (symmetric ? myArrowHead || 'none' :
              (leftToRight ? 'none' : myArrowHead || 'triangle,5')
            ) + ',' + color;
            var arrowType = arrows[arrowName];
            var arrowDecl = arrowType && ('url(#' + arrowType + ')');

            var arrowAtLabelAdjust = 0;
            var labelArrowDecl = null;
            var myLabelArrowHead = ((arcDesc && arcDesc.labelArrow) ||
              (this.spanTypes.ARC_DEFAULT && this.spanTypes.ARC_DEFAULT.labelArrow));
            if (myLabelArrowHead) {
              var labelArrowName = (leftToRight ?
                symmetric && myLabelArrowHead || 'none' :
                myLabelArrowHead || 'triangle,5') + ',' + color;
              var labelArrowSplit = labelArrowName.split(',');
              arrowAtLabelAdjust = labelArrowSplit[0] != 'none' && parseInt(labelArrowSplit[1], 10) || 0;
              var labelArrowType = arrows[labelArrowName];
              var labelArrowDecl = labelArrowType && ('url(#' + labelArrowType + ')');
              if (ufoCatcher) {
                arrowAtLabelAdjust = -arrowAtLabelAdjust;
              }
            }
            var arrowStart = textStart - arrowAtLabelAdjust;
            path = this.svg.createPath().move(arrowStart, -height);
            if (rowIndex == leftRow) {
              // WEBANNO EXTENSION BEGIN - RTL support - arc slant
              /*
              var cornerx = from + ufoCatcherMod * arcSlant;
              */
              var cornerx = from + (this.rtlmode ? -1 : 1) * ufoCatcherMod * this.arcSlant;
              // WEBANNO EXTENSION END
              // for normal cases, should not be past textStart even if narrow
              // WEBANNO EXTENSION BEGIN - RTL support - arc slant     
              /*
              if (!ufoCatcher && cornerx > arrowStart - 1) { cornerx = arrowStart - 1; }
              */
              if (this.rtlmode) {
                if (!ufoCatcher && cornerx < arrowStart + 1) { cornerx = arrowStart + 1; }
              } else {
                if (!ufoCatcher && cornerx > arrowStart - 1) { cornerx = arrowStart - 1; }
              }
              // WEBANNO EXTENSION END
              if (this.smoothArcCurves) {
                // WEBANNO EXTENSION BEGIN - RTL support - arc slant
                // var controlx = ufoCatcher ? cornerx + 2*ufoCatcherMod*this.reverseArcControlx : this.smoothArcSteepness*from+(1-this.smoothArcSteepness)*cornerx;
                // var endy = leftBox.y + (leftToRight || arc.equiv ? leftBox.height / 2 : Configuration.visual.margin.y);
                var controlx;
                var endy;
                if (this.rtlmode) {
                  controlx = ufoCatcher ? cornerx - 2 * ufoCatcherMod * this.reverseArcControlx : this.smoothArcSteepness * from + (1 - this.smoothArcSteepness) * cornerx;
                  endy = leftBox.y + (leftToRight && !arc.equiv ? Configuration.visual.margin.y : leftBox.height / 2);
                } else {
                  controlx = ufoCatcher ? cornerx + 2 * ufoCatcherMod * this.reverseArcControlx : this.smoothArcSteepness * from + (1 - this.smoothArcSteepness) * cornerx;
                  endy = leftBox.y + (leftToRight || arc.equiv ? leftBox.height / 2 : Configuration.visual.margin.y);
                }
                // WEBANNO EXTENSION END
                // no curving for short lines covering short vertical
                // distances, the arrowheads can go off (#925)
                if (Math.abs(-height - endy) < 2 &&
                  Math.abs(cornerx - from) < 5) {
                  endy = -height;
                }
                path.line(cornerx, -height).
                  curveQ(controlx, -height, from, endy);
              } else {
                path.line(cornerx, -height).
                  line(from, leftBox.y + (leftToRight || arc.equiv ? leftBox.height / 2 : Configuration.visual.margin.y));
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
                strokeWidth: markedArcStroke,
                'strokeDashArray': dashArray,
              });
              // WEBANNO EXTENSION BEGIN - Issue #1319 - Glowing highlight causes 100% CPU load
              /*
              this.svg.other(markedRect, 'animate', {
                'data-type': arc.marked,
                attributeName: 'fill',
                values: (arc.marked == 'match' ? highlightMatchSequence
                          : highlightArcSequence),
                dur: highlightDuration,
                repeatCount: 'indefinite',
                begin: 'indefinite'
              });
              */
              // WEBANNO EXTENSION END - Issue #1319 - Glowing highlight causes 100% CPU load
            }
            if (arc.shadowClass) {
              this.svg.path(shadowGroup, path, {
                'class': 'shadow_' + arc.shadowClass,
                strokeWidth: shadowStroke,
                'strokeDashArray': dashArray,
              });
            }
            if (!symmetric) {
              myArrowHead = ((arcDesc && arcDesc.arrowHead) ||
                (spanTypes.ARC_DEFAULT && spanTypes.ARC_DEFAULT.arrowHead));
              arrowName = (leftToRight ?
                myArrowHead || 'triangle,5' :
                'none') + ',' + color;
            }
            var arrowType = arrows[arrowName];
            var arrowDecl = arrowType && ('url(#' + arrowType + ')');

            var arrowAtLabelAdjust = 0;
            var labelArrowDecl = null;
            var myLabelArrowHead = ((arcDesc && arcDesc.labelArrow) ||
              (this.spanTypes.ARC_DEFAULT && this.spanTypes.ARC_DEFAULT.labelArrow));
            if (myLabelArrowHead) {
              var labelArrowName = (leftToRight ?
                myLabelArrowHead || 'triangle,5' :
                symmetric && myLabelArrowHead || 'none') + ',' + color;
              var labelArrowSplit = labelArrowName.split(',');
              arrowAtLabelAdjust = labelArrowSplit[0] != 'none' && parseInt(labelArrowSplit[1], 10) || 0;
              var labelArrowType = arrows[labelArrowName];
              var labelArrowDecl = labelArrowType && ('url(#' + labelArrowType + ')');
              if (ufoCatcher) {
                arrowAtLabelAdjust = -arrowAtLabelAdjust;
              }
            }
            var arrowEnd = textEnd + arrowAtLabelAdjust;
            path = this.svg.createPath().move(arrowEnd, -height);
            if (rowIndex == rightRow) {
              // WEBANNO EXTENSION BEGIN - RTL support - arc slant
              /*
              var cornerx  = to - ufoCatcherMod * arcSlant;
              */
              var cornerx = to - (this.rtlmode ? -1 : 1) * ufoCatcherMod * this.arcSlant;
              // WEBANNO EXTENSION END
              // TODO: duplicates above in part, make funcs
              // for normal cases, should not be past textEnd even if narrow
              // WEBANNO EXTENSION BEGIN - RTL support - arc slant
              /*
              if (!ufoCatcher && cornerx < arrowEnd + 1) { cornerx = arrowEnd + 1; }
              */
              if (this.rtlmode) {
                if (!ufoCatcher && cornerx > arrowEnd - 1) { cornerx = arrowEnd - 1; }
              } else {
                if (!ufoCatcher && cornerx < arrowEnd + 1) { cornerx = arrowEnd + 1; }
              }
              if (this.smoothArcCurves) {
                // WEBANNO EXTENSION BEGIN - RTL support - arc slant
                /*
                var controlx = ufoCatcher ? cornerx - 2*ufoCatcherMod*this.reverseArcControlx : this.smoothArcSteepness*to+(1-this.smoothArcSteepness)*cornerx;
                var endy = rightBox.y + (leftToRight && !arc.equiv ? Configuration.visual.margin.y : rightBox.height / 2);
                */
                var controlx;
                var endy;
                if (this.rtlmode) {
                  controlx = ufoCatcher ? cornerx - 2 * ufoCatcherMod * this.reverseArcControlx : this.smoothArcSteepness * to + (1 - this.smoothArcSteepness) * cornerx;
                  endy = rightBox.y + (leftToRight && !arc.equiv ? Configuration.visual.margin.y : rightBox.height / 2);
                } else {
                  controlx = ufoCatcher ? cornerx - 2 * ufoCatcherMod * this.reverseArcControlx : this.smoothArcSteepness * to + (1 - this.smoothArcSteepness) * cornerx;
                  endy = rightBox.y + (leftToRight && !arc.equiv ? Configuration.visual.margin.y : rightBox.height / 2);
                }
                // WEBANNO EXTENSION END
                // no curving for short lines covering short vertical
                // distances, the arrowheads can go off (#925)
                if (Math.abs(-height - endy) < 2 &&
                  Math.abs(cornerx - to) < 5) {
                  endy = -height;
                }
                path.line(cornerx, -height).
                  curveQ(controlx, -height, to, endy);
              } else {
                path.line(cornerx, -height).
                  line(to, rightBox.y + (leftToRight && !arc.equiv ? Configuration.visual.margin.y : rightBox.height / 2));
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
                strokeWidth: markedArcStroke,
                'strokeDashArray': dashArray,
              });
            }
            if (shadowGroup) {
              this.svg.path(shadowGroup, path, {
                'class': 'shadow_' + arc.shadowClass,
                strokeWidth: shadowStroke,
                'strokeDashArray': dashArray,
              });
            }
          }
        } // arc rows
      }); // arcs

      Util.profileEnd('arcs');
      Util.profileStart('fragmentConnectors');

      $.each(this.data.spans, (spanNo, span) => {
        var numConnectors = span.fragments.length - 1;
        for (var connectorNo = 0; connectorNo < numConnectors; connectorNo++) {
          var left = span.fragments[connectorNo];
          var right = span.fragments[connectorNo + 1];

          var leftBox = this.rowBBox(left);
          var rightBox = this.rowBBox(right);
          var leftRow = left.chunk.row.index;
          var rightRow = right.chunk.row.index;

          for (var rowIndex = leftRow; rowIndex <= rightRow; rowIndex++) {
            var row = rows[rowIndex];
            if (row.chunks.length) {
              row.hasAnnotations = true;

              var from;
              if (rowIndex == leftRow) {
                from = this.rtlmode ? leftBox.x : leftBox.x + leftBox.width;
              } else {
                from = this.rtlmode ? this.canvasWidth - 2 * Configuration.visual.margin.y - this.sentNumMargin : this.sentNumMargin;
              }

              var to;
              if (rowIndex == rightRow) {
                to = this.rtlmode ? rightBox.x + rightBox.width : rightBox.x;
              } else {
                to = this.rtlmode ? 0 : this.canvasWidth - 2 * Configuration.visual.margin.y;
              }
              // WEBANNO EXTENSION END
              var height = leftBox.y + leftBox.height - Configuration.visual.margin.y;
              if (this.roundCoordinates) {
                // don't ask
                height = (height | 0) + 0.5;
              }

              var path = this.svg.createPath().move(from, height).line(to, height);
              this.svg.path(row.arcs, path, {
                style: 'stroke: ' + this.fragmentConnectorColor,
                'strokeDashArray': this.fragmentConnectorDashArray
              });
            }
          } // rowIndex
        } // connectorNo
      }); // spans

      Util.profileEnd('fragmentConnectors');
      Util.profileStart('rows');

      // position the rows
      var y = Configuration.visual.margin.y;
      // BEGIN WEBANNO EXTENSION - #724 - Cross-row selection is jumpy
      /*
      var sentNumGroup = this.svg.group({'class': 'sentnum'});
      */
      var sentNumGroup = this.svg.group({ 'class': 'sentnum unselectable' });
      // END WEBANNO EXTENSION - #724 - Cross-row selection is jumpy
      var currentSent;
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
        var rowBoxHeight = Math.max(row.maxArcHeight + 5, row.maxSpanHeight + 1.5); // XXX TODO HACK: why 5, 1.5?
        if (row.hasAnnotations) {
          // rowBox.height = -rowBox.y + rowSpacing;
          rowBoxHeight += this.rowSpacing + 1.5; // XXX TODO HACK: why 1.5?
        } else {
          rowBoxHeight -= 5; // XXX TODO HACK: why -5?
        }

        rowBoxHeight += this.rowPadding;

        // WEBANNO EXTENSION BEGIN - RTL support - Sentence number in margin
        /*
        var bgClass;
        if (data.markedSent[currentSent]) {
          // specifically highlighted
          bgClass = 'backgroundHighlight';
        } else if (Configuration.textBackgrounds == "striped") {
          // give every other sentence a different bg class
          bgClass = 'background'+ row.backgroundIndex;
        } else {
          // plain "standard" bg
          bgClass = 'background0';
        }
        svg.rect(backgroundGroup,
          0, y + sizes.texts.y + sizes.texts.height,
          this.canvasWidth, rowBoxHeight + sizes.texts.height + 1, {
          'class': bgClass,
        });
        */
        var bgClass;
        if (Configuration.textBackgrounds == "striped") {
          // give every other sentence a different bg class
          bgClass = 'background' + row.backgroundIndex;
        } else {
          // plain "standard" bg
          bgClass = 'background0';
        }
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
              { 'class': 'backgroundHighlight' });
          } else {
            this.svg.rect(backgroundGroup,
              0,
              y + sizes.texts.y + sizes.texts.height,
              this.sentNumMargin,
              rowBoxHeight + sizes.texts.height + 1,
              { 'class': 'backgroundHighlight' });
          }
        }
        // WEBANNO EXTENSION END
        y += rowBoxHeight;
        y += sizes.texts.height;
        row.textY = y - this.rowPadding;
        if (row.sentence) {
          // WEBANNO EXTENSION BEGIN - Just render sentence number as text to avoid need to load url_monitor    
          /*
          var sentence_hash = new URLHash(coll, doc, { focus: [[ 'sent', row.sentence ]] } );
          var link = this.svg.link(sentNumGroup, sentence_hash.getHash());
          */
          var link = sentNumGroup;
          // WEBANNO EXTENSION END
          // WEBANNO EXTENSION BEGIN - RTL support - Sentence number in margin           
          /*
          var text = this.svg.text(link, sentNumMargin - Configuration.visual.margin.x, y - rowPadding,
              '' + row.sentence, { 'data-sent': row.sentence });
          */
          // Render sentence number as link
          var text;
          if (this.rtlmode) {
            text = this.svg.text(link, this.canvasWidth - this.sentNumMargin + Configuration.visual.margin.x, y - this.rowPadding,
              '' + row.sentence, { 'data-sent': row.sentence });
          } else {
            text = this.svg.text(link, this.sentNumMargin - Configuration.visual.margin.x, y - this.rowPadding,
              '' + row.sentence, { 'data-sent': row.sentence });
          }
          // WEBANNO EXTENSION END
          // WEBANNO EXTENSION BEGIN - #406 Sharable link for annotation documents
          $(text).css('cursor', 'pointer');
          // WEBANNO EXTENSION END - #406 Sharable link for annotation documents
          var sentComment = this.data.sentComment[row.sentence];
          if (sentComment) {
            var box = text.getBBox();
            this.svg.remove(text);
            // TODO: using rectShadowSize, but this shadow should
            // probably have its own setting for shadow size
            shadowRect = this.svg.rect(sentNumGroup,
              // WEBANNO EXTENSION BEGIN - RTL support - Sentence comment in margin
              /*
              box.x - rectShadowSize, box.y - rectShadowSize,
              box.width + 2 * rectShadowSize, box.height + 2 * rectShadowSize, {
              */
                this.rtlmode ? box.x + rowPadding + rectShadowSize : box.x - rectShadowSize,
              box.y - rectShadowSize,
              box.width + 2 * rectShadowSize,
              box.height + 2 * rectShadowSize,
              {
                // WEBANNO EXTENSION END - RTL support - Sentence comment in margin
                'class': 'shadow_' + sentComment.type,
                filter: 'url(#Gaussian_Blur)',
                rx: rectShadowRounding,
                ry: rectShadowRounding,
                'data-sent': row.sentence,
              });
            // WEBANNO EXTENSION BEGIN - RTL support - Sentence comment in margin           
            /*
            var text = this.svg.text(sentNumGroup, sentNumMargin - Configuration.visual.margin.x, y - rowPadding,
                '' + row.sentence, { 'data-sent': row.sentence });
            */
            // Render sentence comment
            var text;
            if (this.rtlmode) {
              text = this.svg.text(sentNumGroup, this.canvasWidth - this.sentNumMargin + Configuration.visual.margin.x, y - this.rowPadding,
                '' + row.sentence, { 'data-sent': row.sentence });
            } else {
              text = this.svg.text(sentNumGroup, this.sentNumMargin - Configuration.visual.margin.x, y - this.rowPadding,
                '' + row.sentence, { 'data-sent': row.sentence });
            }
            // WEBANNO EXTENSION END            
            // WEBANNO EXTENSION BEGIN - #406 Sharable link for annotation documents
            $(text).css('cursor', 'pointer');
            // WEBANNO EXTENSION END - #406 Sharable link for annotation documents
          }
        }

        var rowY = y - this.rowPadding;
        if (this.roundCoordinates) {
          rowY = rowY | 0;
        }
        this.translate(row, 0, rowY);
        y += Configuration.visual.margin.y;
      });
      y += Configuration.visual.margin.y;

      Util.profileEnd('rows');
      Util.profileStart('chunkFinish');

      // chunk index sort functions for overlapping fragment drawing
      // algorithm; first for left-to-right pass, sorting primarily
      // by start offset, second for right-to-left pass by end
      // offset. Secondary sort by fragment length in both cases.
      var currentChunk;
      var lrChunkComp = (a, b) => {
        var ac = currentChunk.fragments[a];
        var bc = currentChunk.fragments[b];
        var startDiff = Util.cmp(ac.from, bc.from);
        return startDiff != 0 ? startDiff : Util.cmp(bc.to - bc.from, ac.to - ac.from);
      };
      var rlChunkComp = (a, b) => {
        var ac = currentChunk.fragments[a];
        var bc = currentChunk.fragments[b];
        var endDiff = Util.cmp(bc.to, ac.to);
        return endDiff != 0 ? endDiff : Util.cmp(bc.to - bc.from, ac.to - ac.from);
      };

      // BEGIN WEBANNO EXTENSION - #724 - Cross-row selection is jumpy
      /*
      var sentenceText = null;
      */
      var prevChunk = null;
      var rowTextGroup = null;
      // END WEBANNO EXTENSION - #724 - Cross-row selection is jumpy
      $.each(this.data.chunks, (chunkNo, chunk) => {
        // context for sort
        currentChunk = chunk;

        // BEGIN WEBANNO EXTENSION - #724 - Cross-row selection is jumpy
        /*
        // text rendering
        if (chunk.sentence) {
          if (sentenceText) {
            // svg.text(textGroup, sentenceText); // avoids jQuerySVG bug
            svg.text(textGroup, 0, 0, sentenceText);
          }
          sentenceText = null;
        }
        if (!sentenceText) {
          sentenceText = svg.createText();
        }
        */
        if (!rowTextGroup || prevChunk.row != chunk.row) {
          if (rowTextGroup) {
            this.horizontalSpacer(this.svg, rowTextGroup, 0, prevChunk.row.textY, 1, {
              'data-chunk-id': prevChunk.index,
              'class': 'row-final spacing'
            });
          }
          rowTextGroup = this.svg.group(textGroup, { 'class': 'text-row' });
        }
        prevChunk = chunk;
        // END WEBANNO EXTENSION - #724 - Cross-row selection is jumpy
        var nextChunk = this.data.chunks[chunkNo + 1];
        var nextSpace = nextChunk ? nextChunk.space : '';
        // WEBANNO EXTENSION BEGIN - RTL support - Render chunks as SVG text
        /*
        sentenceText.span(chunk.text + nextSpace, {
          x: chunk.textX,
          y: chunk.row.textY,
          'data-chunk-id': chunk.index
        });
        */
        if (this.rtlmode) {
          // Render every text chunk as a SVG text so we maintain control over the layout. When 
          // rendering as a SVG span (as brat does), then the browser changes the layout on the 
          // X-axis as it likes in RTL mode.
          // BEGIN WEBANNO EXTENSION - #316 Text selection behavior while dragging mouse
          // BEGIN WEBANNO EXTENSION - #724 - Cross-row selection is jumpy
          /*
          svg.text(textGroup, chunk.textX, chunk.row.textY, chunk.text + nextSpace, {
            'data-chunk-id': chunk.index
          });
          */
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
            var spaceX = chunk.textX - sizes.texts.widths[chunk.text];
            var spaceWidth = chunk.textX - sizes.texts.widths[chunk.text] - nextChunk.textX;
            this.horizontalSpacer(this.svg, rowTextGroup, spaceX, chunk.row.textY, spaceWidth, {
              'data-chunk-id': chunk.index
            });
          }
          // END WEBANNO EXTENSION - #724 - Cross-row selection is jumpy
          // END WEBANNO EXTENSION - #316 Text selection behavior while dragging mouse
        }
        else {
          // Original rendering using tspan in ltr mode as it play nicer with selection
          // BEGIN WEBANNO EXTENSION - #316 Text selection behavior while dragging mouse
          // BEGIN WEBANNO EXTENSION - #724 - Cross-row selection is jumpy
          /*
          sentenceText.span(chunk.text + nextSpace, {
            x: chunk.textX,
            y: chunk.row.textY,
            'data-chunk-id': chunk.index});
          */
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
            var spaceX = chunk.textX + sizes.texts.widths[chunk.text];
            var spaceWidth = nextChunk.textX - spaceX;
            this.horizontalSpacer(this.svg, rowTextGroup, spaceX, chunk.row.textY, spaceWidth, {
              'data-chunk-id': chunk.index
            });
          }
          // END WEBANNO EXTENSION - #724 - Cross-row selection is jumpy
          // END WEBANNO EXTENSION - #316 Text selection behavior while dragging mouse
        }
        // WEBANNO EXTENSION END
        // chunk backgrounds
        if (chunk.fragments.length) {
          var orderedIdx = [];
          for (var i = chunk.fragments.length - 1; i >= 0; i--) {
            orderedIdx.push(i);
          }

          // Mark entity nesting height/depth (number of
          // nested/nesting entities). To account for crossing
          // brackets in a (mostly) reasonable way, determine
          // depth/height separately in a left-to-right traversal
          // and a right-to-left traversal.
          orderedIdx.sort(lrChunkComp);

          var openFragments = [];
          for (var i = 0; i < orderedIdx.length; i++) {
            var current = chunk.fragments[orderedIdx[i]];
            current.nestingHeightLR = 0;
            current.nestingDepthLR = 0;
            var stillOpen = [];
            for (var o = 0; o < openFragments.length; o++) {
              if (openFragments[o].to > current.from) {
                stillOpen.push(openFragments[o]);
                openFragments[o].nestingHeightLR++;
              }
            }
            openFragments = stillOpen;
            current.nestingDepthLR = openFragments.length;
            openFragments.push(current);
          }

          // re-sort for right-to-left traversal by end position
          orderedIdx.sort(rlChunkComp);

          openFragments = [];
          for (var i = 0; i < orderedIdx.length; i++) {
            var current = chunk.fragments[orderedIdx[i]];
            current.nestingHeightRL = 0;
            current.nestingDepthRL = 0;
            var stillOpen = [];
            for (var o = 0; o < openFragments.length; o++) {
              if (openFragments[o].from < current.to) {
                stillOpen.push(openFragments[o]);
                openFragments[o].nestingHeightRL++;
              }
            }
            openFragments = stillOpen;
            current.nestingDepthRL = openFragments.length;
            openFragments.push(current);
          }

          // the effective depth and height are the max of those
          // for the left-to-right and right-to-left traversals.
          for (var i = 0; i < orderedIdx.length; i++) {
            var c = chunk.fragments[orderedIdx[i]];
            c.nestingHeight = c.nestingHeightLR > c.nestingHeightRL ? c.nestingHeightLR : c.nestingHeightRL;
            c.nestingDepth = c.nestingDepthLR > c.nestingDepthRL ? c.nestingDepthLR : c.nestingDepthRL;
          }

          // Re-order by nesting height and draw in order
          orderedIdx.sort(function (a, b) { return Util.cmp(chunk.fragments[b].nestingHeight, chunk.fragments[a].nestingHeight); });

          for (var i = 0; i < chunk.fragments.length; i++) {
            var fragment = chunk.fragments[orderedIdx[i]];
            if (fragment.span.hidden) {
              continue;
            }

            var spanDesc = this.spanTypes[fragment.span.type];
            var bgColor = ((spanDesc && spanDesc.bgColor) ||
              (this.spanTypes.SPAN_DEFAULT && this.spanTypes.SPAN_DEFAULT.bgColor) ||
              '#ffffff');

            // WEBANNO EXTENSION BEGIN - #820 - Allow setting label/color individually
            if (fragment.span.color) {
              bgColor = fragment.span.color;
            }
            // WEBANNO EXTENSION END
            // Tweak for nesting depth/height. Recognize just three
            // levels for now: normal, nested, and nesting, where
            // nested+nesting yields normal. (Currently testing
            // minor tweak: don't shrink for depth 1 as the nesting
            // highlight will grow anyway [check nestingDepth > 1])
            var shrink = 0;
            if (fragment.nestingDepth > 1 && fragment.nestingHeight == 0) {
              shrink = 1;
            } else if (fragment.nestingDepth == 0 && fragment.nestingHeight > 0) {
              shrink = -1;
            }
            var yShrink = shrink * this.nestingAdjustYStepSize;
            var xShrink = shrink * this.nestingAdjustXStepSize;
            // bit lighter
            var lightBgColor = Util.adjustColorLightness(bgColor, 0.8);
            // tweak for Y start offset (and corresponding height
            // reduction): text rarely hits font max height, so this
            // tends to look better
            var yStartTweak = 1;
            // store to have same mouseover highlight without recalc
            // WEBANNO EXTENSION BEGIN - RTL support - Highlight positions
            /*
                          fragment.highlightPos = {
                              x: chunk.textX + fragment.curly.from + xShrink,
                              y: chunk.row.textY + sizes.texts.y + yShrink + yStartTweak,
                              w: fragment.curly.to - fragment.curly.from - 2*xShrink,
                              h: sizes.texts.height - 2*yShrink - yStartTweak,
                          };
            */
            // Store highlight coordinates
            fragment.highlightPos = {
              x: chunk.textX + (this.rtlmode ? (fragment.curly.from - xShrink) : (fragment.curly.from + xShrink)),
              y: chunk.row.textY + sizes.texts.y + yShrink + yStartTweak,
              w: fragment.curly.to - fragment.curly.from - 2 * xShrink,
              h: sizes.texts.height - 2 * yShrink - yStartTweak,
            };
            // WEBANNO EXTENSION END
            // WEBANNO EXTENSION BEGIN - #361 Avoid rendering exception with zero-width spans               
            // Avoid exception because width < 0 is not allowed
            if (fragment.highlightPos.w <= 0) {
              fragment.highlightPos.w = 1;
            }
            // WEBANNO EXTENSION END - #361 Avoid rendering exception with zero-width spans
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

      // BEGIN WEBANNO EXTENSION - #724 - Cross-row selection is jumpy
      /*
      if (sentenceText) {
        // this.svg.text(textGroup, sentenceText); // avoids jQuerySVG bug
        this.svg.text(textGroup, 0, 0, sentenceText);
      }
      */
      if (rowTextGroup) {
        this.horizontalSpacer(this.svg, rowTextGroup, 0, currentChunk.row.textY, 1, {
          'data-chunk-id': currentChunk.index,
          'class': 'row-final spacing'
        });
      }
      // END WEBANNO EXTENSION - #724 - Cross-row selection is jumpy
      // draw the markedText
      $.each(textMarkedRows, (textRowNo, textRowDesc) => {
        var textHighlight = this.svg.rect(this.highlightGroup,
          textRowDesc[1] - 2, textRowDesc[0].textY - sizes.fragments.height,
          textRowDesc[2] - textRowDesc[1] + 4, sizes.fragments.height + 4,
          // WEBANNO EXTENSION BEGIN - #876 - Add ability to highlight text in brat view
          // WEBANNO EXTENSION BEGIN - #1119 - Improve control over markers
          /*
          { fill: 'yellow' } // TODO: put into css file, as default - turn into class
          */
          { 'class': textRowDesc[3] }
          // WEBANNO EXTENSION END - #1119 - Improve control over markers
          // WEBANNO EXTENSION END - #876 - Add ability to highlight text in brat view
        );
        // WEBANNO EXTENSION BEGIN - Issue #1319 - Glowing highlight causes 100% CPU load
        /*
        // NOTE: changing highlightTextSequence here will give
        // different-colored highlights
        // TODO: entirely different settings for non-animations?
        var markedType = textRowDesc[3];
        svg.other(textHighlight, 'animate', {
          'data-type': markedType,
          attributeName: 'fill',
          values: (markedType == 'match' ? highlightMatchSequence
                    : highlightTextSequence),
          dur: highlightDuration,
          repeatCount: 'indefinite',
          begin: 'indefinite'
        });
        */
        // WEBANNO EXTENSION END - Issue #1319 - Glowing highlight causes 100% CPU load
      });


      Util.profileEnd('chunkFinish');
      Util.profileStart('finish');

      // WEBANNO EXTENSION BEGIN - RTL support - Render sentence number margin separator
      /*
      this.svg.path(sentNumGroup, this.svg.createPath().
        move(sentNumMargin, 0).
        line(sentNumMargin, y));
      */
      Util.profileStart('adjust margin');
      if (this.rtlmode) {
        this.svg.path(sentNumGroup, this.svg.createPath().
          move(this.canvasWidth - this.sentNumMargin, 0).
          line(this.canvasWidth - this.sentNumMargin, y));
      } else {
        this.svg.path(sentNumGroup, this.svg.createPath().
          move(this.sentNumMargin, 0).
          line(this.sentNumMargin, y));
      }
      // WEBANNO EXTENSION END        
      Util.profileEnd('adjust margin');
      Util.profileStart('resize SVG');
      // resize the SVG
      var width = maxTextWidth + this.sentNumMargin + 2 * Configuration.visual.margin.x + 1;
      // WEBANNO EXTENSION BEGIN - #286 - Very long span annotations cause ADEP to disappear 
      // Add scrolling box
      /*
      if (width > this.canvasWidth) this.canvasWidth = width;
      */
      // Loops over the rows to check if the width calculated so far is still not enough. This
      // currently happens sometimes if there is a single annotation on many words preventing
      // wrapping within the annotation (aka oversizing).
      $(textGroup).children(".text-row").each((rowIndex, textRow) => {
        var rowInitialSpacing = $($(textRow).children('.row-initial')[0]);
        var rowFinalSpacing = $($(textRow).children('.row-final')[0]);
        var lastChunkWidth = sizes.texts.widths[rowFinalSpacing.prev()[0].textContent];
        var lastChunkOffset = parseFloat(rowFinalSpacing.prev()[0].getAttribute('x'));
        if (this.rtlmode) {
          // Not sure what to calculate here
        }
        else {
          if (lastChunkOffset + lastChunkWidth > width) {
            width = lastChunkOffset + lastChunkWidth;
          }
        }
      });

      var oversized = Math.max(width - this.canvasWidth, 0);
      if (oversized > 0) {
        this.$svgDiv.width(this.baseCanvasWidth);
        this.canvasWidth = width;
        // Allow some extra space for arcs
        this.canvasWidth += 32;
        oversized += 32;
      }
      // WEBANNO EXTENSION END
      this.$svg.width(this.canvasWidth);
      Util.profileStart('height');
      this.$svg.height(y);
      Util.profileEnd('height');
      this.$svg.attr("viewBox", "0 0 " + this.canvasWidth + " " + y);

      // WEBANNO EXTENSION BEGIN #331 - Interface jumps to the top
      // Originally, this code was within the oversized > 0 block above, but we moved it here
      // to prevent erratic jumping
      this.$svgDiv.height(y + 4); // Need to take the hairline border into account here

      // WEBANNO EXTENSION END #331 - Interface jumps to the top
      Util.profileEnd('resize SVG');
      Util.profileStart('set up RTL');

      // WEBANNO EXTENSION BEGIN - RTL support - Set SVG canvas to RTL mode
      if (this.rtlmode) {
        this.$svg.attr("direction", "rtl");
        // WEBANNO EXTENSION BEGIN - #300 - RTL, line breaks and Scrollbars
        if (oversized > 0) {
          $.each(rows, (index, row) => this.translate(row, oversized, row.translation.y));
          $(glowGroup).attr('transform', 'translate(' + oversized + ', ' + 0 + ')');
          $(this.highlightGroup).attr('transform', 'translate(' + oversized + ', ' + 0 + ')');
          $(textGroup).attr('transform', 'translate(' + oversized + ', ' + 0 + ')');
          $(sentNumGroup).attr('transform', 'translate(' + oversized + ', ' + 0 + ')');
          var scrollable = findClosestHorizontalScrollable($svgDiv);
          if (scrollable) {
            scrollable.scrollLeft(oversized + 4);
          }
        }
        // WEBANNO EXTENSION END - #300 - RTL, line breaks and Scrollbars
      }
      // WEBANNO EXTENSION END
      Util.profileEnd('set up RTL');
      Util.profileStart('adjust backgrounds');
      // WEBANNO EXTENSION BEGIN - #286 - Very long span annotations cause ADEP to disappear 
      // Allow some extra space for arcs
      if (oversized > 0) {
        $(backgroundGroup).attr('width', this.canvasWidth);
        $(backgroundGroup).children().each((index, element) => {
          // We render the backgroundHighlight only in the margin, so we have to translate
          // it instead of transforming it.
          if ($(element).attr('class') == 'backgroundHighlight') {
            if (this.rtlmode) {
              $(element).attr('transform', 'translate(' + oversized + ', ' + 0 + ')');
            }
          }
          else {
            $(element).attr('width', this.canvasWidth);
          }
        });
      }
      // WEBANNO EXTENSION END
      Util.profileEnd('adjust backgrounds');
      Util.profileStart('row-spacing-adjust');
      // Go through each row and adjust the row-initial and row-final spacing
      $(textGroup).children(".text-row").each((rowIndex, textRow) => {
        var rowInitialSpacing = $($(textRow).children('.row-initial')[0]);
        var rowFinalSpacing = $($(textRow).children('.row-final')[0]);
        var firstChunkWidth = sizes.texts.widths[rowInitialSpacing.next()[0].textContent];
        var lastChunkWidth = sizes.texts.widths[rowFinalSpacing.prev()[0].textContent];
        var lastChunkOffset = parseFloat(rowFinalSpacing.prev()[0].getAttribute('x'));

        if (this.rtlmode) {
          var initialSpacingX = this.canvasWidth - this.sentNumMargin;
          var initialSpacingWidth = initialSpacingX - (Configuration.visual.margin.x + this.rowPadding + 1 + firstChunkWidth);
          rowInitialSpacing.attr('x', initialSpacingX);
          rowInitialSpacing.attr('textLength', initialSpacingWidth);

          var finalSpacingX = lastChunkOffset + 1;
          var finalSpacingWidth = lastChunkWidth;
          rowFinalSpacing.attr('x', finalSpacingX);
          rowFinalSpacing.attr('textLength', finalSpacingWidth);
        }
        else {
          var initialSpacingX = this.sentNumMargin;
          var initialSpacingWidth = Configuration.visual.margin.x + this.rowPadding + 1;
          rowInitialSpacing.attr('x', initialSpacingX);
          rowInitialSpacing.attr('textLength', initialSpacingWidth);

          var finalSpacingX = lastChunkOffset + lastChunkWidth + 1;
          var finalSpacingWidth = this.canvasWidth - finalSpacingX;
          rowFinalSpacing.attr('x', finalSpacingX);
          rowFinalSpacing.attr('textLength', finalSpacingWidth);
        }
      });
      Util.profileEnd('row-spacing-adjust');
      // BEGIN WEBANNO EXTENSION - #724 - Cross-row selection is jumpy 
      Util.profileStart('inter-row space');
      // Go through each row and add an unselectable spacer between this row and the next row
      // While the space is unselectable, it will still help in guiding the browser into which
      // direction the selection should in principle go and thus avoids jumpyness.
      var prevRowRect = { y: 0, height: 0 };

      var textRows = $(textGroup).children(".text-row");
      textRows.each((rowIndex, textRow) => {
        var rowRect = {
          y: parseFloat($(textRow).children()[0].getAttribute('y')) + 2, height: sizes.texts.height
        };
        var spaceHeight = rowRect.y - (prevRowRect.y + rowRect.height) + 2;

        // Adding a spacer between the rows. We make is a *little* bit larger than necessary
        // to avoid exposing areas where the background shines through and which would again
        // cause jumpyness during selection.
        textRow.before(this.verticalSpacer(
          Math.floor(prevRowRect.y),
          Math.ceil(spaceHeight)));

        prevRowRect = rowRect;

        // Add a spacer below the final row until the end of the canvas
        if (rowIndex == textRows.length - 1) {
          var lastSpacerY = Math.floor(rowRect.y + rowRect.height);
          textRow.after(this.verticalSpacer(
            Math.floor(rowRect.y + rowRect.height),
            Math.ceil(y - lastSpacerY) + 1));
        }
      });
      Util.profileEnd('inter-row space');
      // END WEBANNO EXTENSION - #724 - Cross-row selection is jumpy 
      Util.profileEnd('finish');
      Util.profileEnd('render');
      Util.profileReport();

      this.drawing = false;
      if (this.redraw) {
        this.redraw = false;
      }
      this.$svg.find('animate').each(function () {
        if (this.beginElement) { // protect against non-SMIL browsers
          this.beginElement();
        }
      });
      this.dispatcher.post('doneRendering', [this.coll, this.doc, this.args]);
    }

    renderData(sourceData) {
      Util.profileEnd('invoke getDocument');

      if (sourceData && sourceData.exception) {
        if (this.renderErrors[this.sourceData.exception]) {
          this.dispatcher.post('renderError:' + sourceData.exception, [sourceData]);
        } else {
          this.dispatcher.post('unknownError', [sourceData.exception]);
        }
        return;
      } 

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

          this.renderData(requestedData);
        } else if (this.doc.length) {

          Util.profileClear();
          Util.profileStart('before render');

          this.renderDocument();
        } else {
          this.dispatcher.post(0, 'renderError:noFileSpecified');
        }
      }
    }

    requestRenderData(sourceData) {
      this.requestedData = sourceData;
      this.triggerRender();
    }

    collectionChanged() {
      this.isCollectionLoaded = false;
    }

    gotCurrent(_coll, _doc, _args, reloadData) {
      this.coll = _coll;
      this.doc = _doc;
      this.args = _args;

      if (reloadData) {
        this.isRenderRequested = true;
        this.triggerRender();
      }
    }

    // event handlers
    onMouseOver(evt) {
      var target = $(evt.target);
      var id;
      if (id = target.attr('data-span-id')) {
        this.commentId = id;
        var span = this.data.spans[id];
        this.dispatcher.post('displaySpanComment', [
          evt, target, id, span.type, span.attributeText,
          span.text,
          span.hovertext,
          span.comment && span.comment.text,
          span.comment && span.comment.type,
          span.normalizations
        ]);
        // BEGIN WEBANNO EXTENSION - #1697 - Explicit UI for accepting/recejcting recommendations
        if (span.actionButtons) {
          this.dispatcher.post('displaySpanButtons', [evt, target, span.id]);
        }
        // END WEBANNO EXTENSION - #1697 - Explicit UI for accepting/recejcting recommendations
        var spanDesc = this.spanTypes[span.type];
        var bgColor = ((spanDesc && spanDesc.bgColor) ||
          (this.spanTypes.SPAN_DEFAULT && this.spanTypes.SPAN_DEFAULT.bgColor) ||
          '#ffffff');
        if (span.hidden)
          return;
        // WEBANNO EXTENSION BEGIN - #820 - Allow setting label/color individually
        if (span.color) {
          bgColor = span.color;
        }
        // WEBANNO EXTENSION END
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
        } else {
          var equivs = {};
          var spans = {};
          spans[id] = true;
          var spanIds = [];
          // find all arcs, normal and equiv. Equivs need to go far (#1023)
          var addArcAndSpan = (arc, span) => {
            if (arc.equiv) {
              equivs[arc.eventDescId.substr(0, arc.eventDescId.indexOf('*', 2) + 1)] = true;
              var eventDesc = this.data.eventDescs[arc.eventDescId];
              $.each(eventDesc.leftSpans.concat(eventDesc.rightSpans), (ospanId, ospan) => spans[ospan] = true);
            } else {
              spans[arc.origin] = true;
            }
          };
          $.each(span.incoming, (arcNo, arc) =>  addArcAndSpan(arc, arc.origin));
          $.each(span.outgoing, (arcNo, arc) =>  addArcAndSpan(arc, arc.target));
          var equivSelector = [];
          $.each(equivs, (equiv, dummy) => equivSelector.push('[data-arc-ed^="' + equiv + '"]'));

          // BEGIN WEBANNO EXTENSION - #246 - Highlighting in curation confused
          /*
          this.highlightArcs = $svg.
              find(equivSelector.join(', ')).
              parent().
              add('g[data-from="' + id + '"], g[data-to="' + id + '"]' + equivSelector).
              addClass('highlight');
          */
          this.highlightArcs = this.$svg.
            find(equivSelector.join(', ')).
            parent().
            add('g[data-from="' + id + '"], g[data-to="' + id + '"]' + equivSelector, this.$svg).
            addClass('highlight');
          // END WEBANNO EXTENSION - #246 - Highlighting in curation confused 
          $.each(spans, (spanId, dummy) => spanIds.push('rect[data-span-id="' + spanId + '"]'));
          this.highlightSpans = this.$svg.
            find(spanIds.join(', ')).
            parent().
            addClass('highlight');
        }
      } else if (!this.arcDragOrigin && (id = target.attr('data-arc-role'))) {
        var originSpanId = target.attr('data-arc-origin');
        var targetSpanId = target.attr('data-arc-target');
        var role = target.attr('data-arc-role');
        var symmetric = (this.relationTypesHash &&
          this.relationTypesHash[role] &&
          this.relationTypesHash[role].properties &&
          this.relationTypesHash[role].properties.symmetric);
        // NOTE: no commentText, commentType for now
        var arcEventDescId = target.attr('data-arc-ed');
        var commentText = '';
        var commentType = '';
        var arcId;
        if (arcEventDescId) {
          var eventDesc = this.data.eventDescs[arcEventDescId];
          var comment = eventDesc.comment;
          if (comment) {
            commentText = comment.text;
            commentType = comment.type;
            if (commentText == '' && commentType) {
              // default to type if missing text
              commentText = commentType;
            }
          }
          if (eventDesc.relation) {
            // among arcs, only ones corresponding to relations have
            // "independent" IDs
            arcId = arcEventDescId;
          }
        }
        var originSpanType = this.data.spans[originSpanId].type || '';
        var targetSpanType = this.data.spans[targetSpanId].type || '';
        var normalizations = [];
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
          this.highlightArcs = this.$svg.
            find('g[data-id="' + arcId + '"]').
            addClass('highlight');
        }
        else {
          this.highlightArcs = this.$svg.
            find('g[data-from="' + originSpanId + '"][data-to="' + targetSpanId + '"]').
            addClass('highlight');
        }

        this.highlightSpans = $(this.$svg).
          find('rect[data-span-id="' + originSpanId + '"], rect[data-span-id="' + targetSpanId + '"]').
          parent().
          addClass('highlight');
      } else if (id = target.attr('data-sent')) {
        var comment = this.data.sentComment[id];
        if (comment) {
          this.dispatcher.post('displaySentComment', [evt, target, comment.text, comment.type]);
        }
      }
    }

    onMouseOut(evt) {
      var target = $(evt.target);
      target.removeClass('badTarget');
      this.dispatcher.post('hideComment');
      if (this.highlight) {
        this.highlight.forEach((h) => this.svg.remove(h));
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
        Configuration.visual.margin = { x: 1, y: 0 };
        Configuration.visual.boxSpacing = 1;
        Configuration.visual.curlyHeight = 1;
        Configuration.visual.arcSpacing = 7;
        Configuration.visual.arcStartHeight = 18;
      } else if (_density > 2) {
        // spacious
        Configuration.visual.margin = { x: 2, y: 1 };
        Configuration.visual.boxSpacing = 3;
        Configuration.visual.curlyHeight = 6;
        Configuration.visual.arcSpacing = 12;
        Configuration.visual.arcStartHeight = 23;
      } else {
        // standard
        Configuration.visual.margin = { x: 2, y: 1 };
        Configuration.visual.boxSpacing = 1;
        Configuration.visual.curlyHeight = 4;
        Configuration.visual.arcSpacing = 9;
        Configuration.visual.arcStartHeight = 19;
      }
      this.dispatcher.post('configurationChanged');
    }

    setSvgWidth(_width) {
      this.$svgDiv.width(_width);
      if (Configuration.svgWidth != _width) {
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
          var children = type.children;
          if (children && children.length) {
            this.loadSpanTypes(children);
          }
        }
      });
    }

    loadAttributeTypes(response_types) {
      var processed = {};
      $.each(response_types, (aTypeNo, aType) => {
        processed[aType.type] = aType;
        // count the values; if only one, it's a boolean attribute
        var values = [];
        for (var i in aType.values) {
          if (aType.values.hasOwnProperty(i)) {
            values.push(i);
          }
        }
        if (values.length == 1) {
          aType.bool = values[0];
        }
      });
      return processed;
    }

    loadRelationTypes(relation_types) {
      $.each(relation_types, (relTypeNo, relType) => {
        if (relType) {
          this.relationTypesHash[relType.type] = relType;
          var children = relType.children;
          if (children && children.length) {
            this.loadRelationTypes(children);
          }
        }
      });
    }

    collectionLoaded(response) {
      if (response.exception) {
        return;
      }

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
      var arcBundle = (response.visual_options || {}).arc_bundle || 'none';
      this.collapseArcs = arcBundle == "all";
      this.collapseArcSpace = arcBundle != "none";

      this.dispatcher.post('spanAndAttributeTypesLoaded', [
        this.spanTypes, this.entityAttributeTypes, this.eventAttributeTypes, this.relationTypesHash]);

      this.isCollectionLoaded = true;
      this.triggerRender();
    }

    isReloadOkay() {
      // do not reload while the user is in the dialog
      return !this.drawing;
    }

    // BEGIN WEBANNO EXTENSION - #724 - Cross-row selection is jumpy
    verticalSpacer(y, height) {
      if (height > 0) {
        var foreignObject = document.createElementNS('http://www.w3.org/2000/svg', 'foreignObject');
        var spacer = document.createElement('span');
        $(spacer)
          .addClass('unselectable')
          .css('display', 'inline-block')
          .css('width', '100%')
          .css('height', '100%')
          // .css('background-color', 'yellow')
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
        var attributes = $.extend({
          textLength: width,
          lengthAdjust: 'spacingAndGlyphs',
          //'fill': 'blue',
          'class': 'spacing'
        }, attrs);
        // To visualize the spacing use \u2588, otherwise \u00a0
        this.svg.text(group, x, y, this.rtlmode ? '\u200f\u00a0' : '\u00a0', attributes);
        /*
        svg.rect(group,
          x - (rtlmode ? width : 0), y - 10,
          width, 10,
          { fill: 'blue' });
        */
      }
    }
    // END WEBANNO EXTENSION - #724 - Cross-row selection is jumpy
  }

  // BEGIN WEBANNO EXTENSION - RTL support - #278 Sub-token annotation of LTR text in RTL mode  
  var isRTL = function isRTL(charCode) {
    var t1 = (0x0591 <= charCode && charCode <= 0x07FF);
    var t2 = (charCode == 0x200F);
    var t3 = (charCode == 0x202E);
    var t4 = (0xFB1D <= charCode && charCode <= 0xFDFD);
    var t5 = (0xFE70 <= charCode && charCode <= 0xFEFC);
    return t1 || t2 || t3 || t4 || t5;
  };
  // WEBANNO EXTENSION END - RTL support - #278 Sub-token annotation of LTR text in RTL mode  

  // WEBANNO EXTENSION BEGIN - #820 - Allow setting label/color individually
  // http://24ways.org/2010/calculating-color-contrast/
  // http://stackoverflow.com/questions/11867545/change-text-color-based-on-brightness-of-the-covered-background-area
  var bgToFgColor = function (hexcolor) {
    var r = parseInt(hexcolor.substr(1, 2), 16);
    var g = parseInt(hexcolor.substr(3, 2), 16);
    var b = parseInt(hexcolor.substr(5, 2), 16);
    var yiq = ((r * 299) + (g * 587) + (b * 114)) / 1000;
    return (yiq >= 128) ? '#000000' : '#ffffff';
  }
  // WEBANNO EXTENSION END

  // WEBANNO EXTENSION BEGIN - RTL - Need to find scrollable ancestor
  // https://stackoverflow.com/a/35940276/2511197
  function findClosestHorizontalScrollable(node) {
    if (node === null || node.is('html')) {
      return null;
    }

    if (
      (node.css('overflow-x') == 'auto' && node.prop('scrollWidth') > node.prop('clientWidth')) ||
      (node.css('overflow-x') == 'scroll')
    ) {
      return node;
    } else {
      return findClosestHorizontalScrollable(node.parent());
    }
  }
  // WEBANNO EXTENSION END - RTL - Need to find scrollable ancestor

  // A naive whitespace tokeniser
  var tokenise = function (text) {
    var tokenOffsets = [];
    var tokenStart = null;
    var lastCharPos = null;

    for (var i = 0; i < text.length; i++) {
      var c = text[i];
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
  };

  // A naive newline sentence splitter
  var sentenceSplit = function (text) {
    var sentenceOffsets = [];
    var sentStart = null;
    var lastCharPos = null;

    for (var i = 0; i < text.length; i++) {
      var c = text[i];
      // Have we found the start of a sentence?
      if (sentStart == null && !/\s/.test(c)) {
        sentStart = i;
        lastCharPos = i;
        // Have we found the end of a sentence?
      } else if (c == '\n' && sentStart != null) {
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
  };

  return Visualizer;
})(jQuery, window);

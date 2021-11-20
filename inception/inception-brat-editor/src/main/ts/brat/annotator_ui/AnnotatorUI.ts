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
import type Dispatcher from "../dispatcher";

import type { Configuration as ConfigurationType } from "../configuration/Configuration";
declare var Configuration: ConfigurationType;

import type { Util as UtilType } from "../util/Util";
declare var Util: UtilType;

export class AnnotatorUI {
  arcDragOrigin = null;
  arcDragOriginBox = null;
  arcDragOriginGroup = null;
  arcDragArc = null;
  arcDragJustStarted = false;
  sourceData = null;
  data = null;
  searchConfig = null;
  spanOptions = null;
  rapidSpanOptions = null;
  arcOptions = null;
  spanKeymap = null;
  keymap = null;
  coll = null;
  doc = null;
  reselectedSpan = null;
  selectedFragment = null;
  editedSpan = null;
  editedFragment = null;
  repeatingArcTypes = [];
  spanTypes = null;
  entityAttributeTypes = null;
  eventAttributeTypes = null;
  allAttributeTypes = null; // TODO: temp workaround, remove
  relationTypesHash = null;
  showValidAttributes; // callback function
  showValidNormalizations; // callback function
  dragStartedAt = null;
  selRect = null;
  lastStartRec = null;
  lastEndRec = null;

  draggedArcHeight = 30;
  spanTypesToShowBeforeCollapse = 30;
  maxNormSearchHistory = 10;

  // TODO: this is an ugly hack, remove (see comment with assignment)
  lastRapidAnnotationEvent = null;
  // TODO: another avoidable global; try to work without
  rapidAnnotationDialogVisible = false;

  // amount by which to lighten (adjust "L" in HSL space) span
  // colors for type selection box BG display. 0=no lightening,
  // 1=white BG (no color)
  spanBoxTextBgColorLighten = 0.4;

  // for normalization: URLs bases by norm DB name
  normDbUrlByDbName = {};
  normDbUrlBaseByDbName = {};
  // for normalization: appropriate DBs per type
  normDbsByType = {};
  // for normalization
  oldSpanNormIdValue = '';
  lastNormSearches = [];

  user: string;
  svg;
  svgElement: JQuery;
  svgId: string;
  dispatcher: Dispatcher;
  args;

  undoStack = [];

  spanForm;
  rapidSpanForm;
  svgPosition: JQueryCoordinates;
  arcDrag: any;

  constructor(dispatcher, svg) {
    this.svg = svg;
    this.dispatcher = dispatcher;
    this.user = null;
    this.svgElement = $(svg._svg);
    this.svgId = this.svgElement.parent().attr('id');

    $('#clear_notes_button').button();
    $('#clear_notes_button').click(this.clearSpanNotes);
    $('#clear_norm_button').button();
    $('#clear_norm_button').click(this.clearSpanNorm);
    $('#span_norm_db').change(this.spanNormDbUpdate);
    // see http://stackoverflow.com/questions/1948332/detect-all-changes-to-a-input-type-text-immediately-using-jquery
    $('#span_norm_id').bind('propertychange keyup input paste', this.spanNormIdUpdate);
    // nice-looking select for normalization
    $('#span_norm_db').addClass('ui-widget ui-state-default ui-button-text');

    let $waiter = $('#waiter');
    $waiter.dialog({
      closeOnEscape: false,
      buttons: {},
      modal: true,
      open: function (evt, ui) {
        $(evt.target).parent().find(".ui-dialog-titlebar-close").hide();
      }
    });
    // hide the waiter (Sampo said it's annoying)
    // we don't elliminate it altogether because it still provides the
    // overlay to prevent interaction
    $waiter.parent().css('opacity', '0');

    // TODO: why are these globals defined here instead of at the top?
    this.spanForm = $('#span_form');
    this.rapidSpanForm = $('#rapid_span_form');

    dispatcher.
      on('init', this, this.init).
      on('getValidArcTypesForDrag', this, this.getValidArcTypesForDrag).
      on('dataReady', this, this.rememberData).
      on('collectionLoaded', this, this.rememberSpanSettings).
      on('collectionLoaded', this, this.setupTaggerUI).
      on('collectionLoaded', this, this.setupNormalizationUI).
      on('spanAndAttributeTypesLoaded', this, this.spanAndAttributeTypesLoaded).
      on('newSourceData', this, this.onNewSourceData).
      on('user', this, this.userReceived).
      on('edited', this, this.edited).
      on('current', this, this.gotCurrent).
      on('isReloadOkay', this, this.isReloadOkay).
      on('keydown', this, this.onKeyDown).
      on('click', this, this.onClick).
      on('dragstart', this, this.preventDefault).
      on('mousedown', this, this.onMouseDown).
      on('mouseup', this, this.onMouseUp).
      on('mousemove', this, this.onMouseMove).
      on('annotationSpeed', this, this.setAnnotationSpeed).
      on('contextmenu', this, this.contextMenu);
  }

  stripNumericSuffix(s) {
    // utility function, originally for stripping numerix suffixes
    // from arc types (e.g. "Theme2" -> "Theme"). For values
    // without suffixes (including non-strings), returns given value.
    if (typeof (s) != "string") {
      return s; // can't strip
    }
    var m = s.match(/^(.*?)(\d*)$/);
    return m[1]; // always matches
  }

  // WEBANNO EXTENSION BEGIN
  // We do not use the brat forms
  /*
        var hideForm = function() {
          keymap = null;
          rapidAnnotationDialogVisible = false;
        };
  */
  // WEBANNO EXTENSIONE END

  clearSelection() {
    window.getSelection().removeAllRanges();
    if (this.selRect != null) {
      for (var s = 0; s != this.selRect.length; s++) {
        this.selRect[s].parentNode.removeChild(this.selRect[s]);
      }
      this.selRect = null;
      this.lastStartRec = null;
      this.lastEndRec = null;
    }
  }

  makeSelRect(rx, ry, rw, rh, col?) {
    var selRect = document.createElementNS("http://www.w3.org/2000/svg", "rect");
    selRect.setAttributeNS(null, "width", rw);
    selRect.setAttributeNS(null, "height", rh);
    selRect.setAttributeNS(null, "x", rx);
    selRect.setAttributeNS(null, "y", ry);
    selRect.setAttributeNS(null, "fill", col == undefined ? "lightblue" : col);
    return selRect;
  };

  onKeyDown(evt) {
    var code = evt.which;

    if (code === $.ui.keyCode.ESCAPE) {
      this.stopArcDrag();
      if (this.reselectedSpan) {
        $(this.reselectedSpan.rect).removeClass('reselect');
        this.reselectedSpan = null;
        this.svgElement.removeClass('reselect');
      }
      return;
    }

    // in rapid annotation mode, prioritize the keys 0..9 for the
    // ordered choices in the quick annotation dialog.
    if (Configuration.rapidModeOn && this.rapidAnnotationDialogVisible &&
      "0".charCodeAt(0) <= code && code <= "9".charCodeAt(0)) {
      var idx = String.fromCharCode(code);
      var $input = $('#rapid_span_' + idx);
      if ($input.length) {
        $input.click();
      }
    }

    if (!this.keymap) return;

    // disable shortcuts when working with elements that you could
    // conceivably type in
    var target = evt.target;
    var nodeName = target.nodeName.toLowerCase();
    var nodeType = target.type && target.type.toLowerCase();
    if (nodeName == 'input' && (nodeType == 'text' || nodeType == 'password')) return;
    if (nodeName == 'textarea' || nodeName == 'select') return;

    var prefix = '';
    if (evt.altKey) {
      prefix = "A-";
    }
    if (evt.ctrlKey) {
      prefix = "C-";
    }
    if (evt.shiftKey) {
      prefix = "S-";
    }
    var binding = this.keymap[prefix + code];
    if (!binding) binding = this.keymap[prefix + String.fromCharCode(code)];
    if (binding) {
      var boundInput = $('#' + binding)[0];
      if (boundInput && !boundInput.disabled) {
        boundInput.click();
        evt.preventDefault();
        return false;
      }
    }
  }

  // WEBANNO EXTENSION BEGIN - #520 Perform javascript action on click 
  clickCount = 0;
  clickTimer = null;
  CLICK_DELAY = 300;

  // Distinguish between double clicks and single clicks . This is relevant when clicking on 
  // annotations. For clicking on text nodes, this is not really relevant.
  onClick(evt) {
    this.clickCount++;

    var singleClickAction = Configuration.singleClickEdit ?
      this.editAnnotation : this.customJSAction;
    var doubleClickAction = Configuration.singleClickEdit ?
    this.customJSAction : this.editAnnotation;

    if (this.clickCount === 1) {
      this.clickTimer = setTimeout(() => {
        try {
          singleClickAction.call(this, evt); // perform single-click action
        }
        finally {
          this.clickCount = 0;                    // after action performed, reset counter
        }
      }, this.CLICK_DELAY);
    } else {
      clearTimeout(this.clickTimer);              // prevent single-click action
      try {
        doubleClickAction.call(this, evt);   // perform double-click action
      }
      finally {
        this.clickCount = 0;                      // after action performed, reset counter
      }
    }
  }

  customJSAction(evt) {
    // must be logged in
    if (this.user === null) return;
    var target = $(evt.target);
    var id;
    // single click actions for spans
    if (id = target.attr('data-span-id')) {
      this.preventDefault(evt);
      this.editedSpan = this.data.spans[id];
      this.editedFragment = target.attr('data-fragment-id');
      var offsets = [];
      $.each(this.editedSpan.fragments, (fragmentNo, fragment) => {
        offsets.push([fragment.from, fragment.to]);
      });
      this.dispatcher.post('ajax', [{
        action: 'doAction',
        offsets: $.toJSON(offsets),
        id: id,
        labelText: this.editedSpan.labelText,
        type: this.editedSpan.type
      }, 'serverResult']);
    }
    // BEGIN WEBANNO EXTENSION - #1579 - Send event when action-clicking on a relation
    else if (id = target.attr('data-arc-ed')) {
      var type = target.attr('data-arc-role');
      var originSpan = this.data.spans[target.attr('data-arc-origin')];
      var targetSpan = this.data.spans[target.attr('data-arc-target')];

      this.dispatcher.post('ajax', [{
        action: 'doAction',
        arcId: id,
        arcType: type,
        originSpanId: originSpan.id,
        originType: originSpan.type,
        targetSpanId: targetSpan.id,
        targetType: targetSpan.type
      }, 'serverResult']);
    }
    // END WEBANNO EXTENSION - #1579 - Send event when action-clicking on a relation
    // WEBANNO EXTENSION BEGIN - #406 Sharable link for annotation documents
    // single click action on sentence id
    else if (id = target.attr('data-sent')) {
      this.preventDefault(evt);
      if (window.UrlUtil) {
        window.UrlUtil.putFragmentParameter('f', id);
        window.UrlUtil.sentParametersOnInitialPageLoad = false;
        window.UrlUtil.sendUrlParameters();
      }
    }
    // WEBANNO EXTENSION END - #406 Sharable link for annotation documents
  }
  // WEBANNO EXTENSION END - #520 Perform javascript action on click 

  // WEBANNO EXTENSION BEGIN - #863 Allow configuration of default value for "auto-scroll" etc.
  /*
        var onDblClick = function(evt) {
  */
  editAnnotation(evt) {
    // WEBANNO EXTENSION END - #863 Allow configuration of default value for "auto-scroll" etc.
    // must be logged in
    if (this.user === null) return;
    // must not be reselecting a span or an arc
    if (this.reselectedSpan || this.arcDragOrigin) return;

    var target = $(evt.target);
    var id;

    // do we edit an arc?
    if (id = target.attr('data-arc-role')) {
      // TODO
      this.clearSelection();
      var originSpanId = target.attr('data-arc-origin');
      var targetSpanId = target.attr('data-arc-target');
      var type = target.attr('data-arc-role');
      var originSpan = this.data.spans[originSpanId];
      var targetSpan = this.data.spans[targetSpanId];
      this.arcOptions = {
        action: 'createArc',
        origin: originSpanId,
        target: targetSpanId,
        old_target: targetSpanId,
        type: type,
        old_type: type,
        collection: this.coll,
        'document': this.doc
      };
      var eventDescId = target.attr('data-arc-ed');
      if (eventDescId) {
        var eventDesc = this.data.eventDescs[eventDescId];
        if (eventDesc.equiv) {
          this.arcOptions['left'] = eventDesc.leftSpans.join(',');
          this.arcOptions['right'] = eventDesc.rightSpans.join(',');
        }
      }
      $('#arc_origin').text(Util.spanDisplayForm(this.spanTypes, originSpan.type) + ' ("' + originSpan.text + '")');
      $('#arc_target').text(Util.spanDisplayForm(this.spanTypes, targetSpan.type) + ' ("' + targetSpan.text + '")');
      var arcId = eventDescId || [originSpanId, type, targetSpanId];
      // WEBANNO EXTENSION BEGIN
      this.fillArcTypesAndDisplayForm(evt, originSpanId, originSpan.type, targetSpanId, targetSpan.type, type, arcId);
      // WEBANNO EXTENSION END
      // for precise timing, log dialog display to user.
      this.dispatcher.post('logAction', ['arcEditSelected']);

      // if not an arc, then do we edit a span?
    } else if (id = target.attr('data-span-id')) {
      this.clearSelection();
      this.editedSpan = this.data.spans[id];
      this.editedFragment = target.attr('data-fragment-id');
      var offsets = [];
      $.each(this.editedSpan.fragments, (fragmentNo, fragment) => {
        offsets.push([fragment.from, fragment.to]);
      });
      this.spanOptions = {
        action: 'createSpan',
        offsets: offsets,
        type: this.editedSpan.type,
        id: id,
      };
      // WEBANNO EXTENSION BEGIN
      this.fillSpanTypesAndDisplayForm(evt, offsets, this.editedSpan.text, this.editedSpan, id);
      // WEBANNO EXTENSION END

      // for precise timing, log annotation display to user.
      this.dispatcher.post('logAction', ['spanEditSelected']);
    }
  };

  startArcDrag(originId) {
    this.clearSelection();

    if (!this.data.spans[originId]) {
      return;
    }

    this.svgElement.addClass('unselectable');
    this.svgPosition = this.svgElement.offset();
    this.arcDragOrigin = originId;
    this.arcDragArc = this.svg.path(this.svg.createPath(), {
      markerEnd: 'url(#drag_arrow)',
      'class': 'drag_stroke',
      fill: 'none',
    });
    this.arcDragOriginGroup = $(this.data.spans[this.arcDragOrigin].group);
    this.arcDragOriginGroup.addClass('highlight');
    this.arcDragOriginBox = Util.realBBox(this.data.spans[this.arcDragOrigin].headFragment);
    this.arcDragOriginBox.center = this.arcDragOriginBox.x + this.arcDragOriginBox.width / 2;

    this.arcDragJustStarted = true;
  };

  getValidArcTypesForDrag(targetId, targetType) {
    var arcType = this.stripNumericSuffix(this.arcOptions && this.arcOptions.type);
    if (!this.arcDragOrigin || targetId == this.arcDragOrigin) return null;

    var originType = this.data.spans[this.arcDragOrigin].type;
    var spanType = this.spanTypes[originType];
    var result = [];
    if (spanType && spanType.arcs) {
      $.each(spanType.arcs, (arcNo, arc) => {
        if (arcType && arcType != arc.type) return;

        if ($.inArray(targetType, arc.targets) != -1) {
          result.push(arc.type);
        }
      });
    }
    return result;
  };

  onMouseDown(evt) {
    // Instead of calling startArcDrag() immediately, we defer this to onMouseMove
    if (!this.user || this.arcDragOrigin) return;

    // is it arc drag start?
    if ($(evt.target).attr('data-span-id')) {
      this.dragStartedAt = evt; // XXX do we really need the whole evt?
      return false;
    }
  };

  onMouseMove(evt) {
    // BEGIN WEBANNO EXTENSION - #1610 - Improve brat visualization interaction performance
    if (!this.arcDragOrigin && this.dragStartedAt) {
      // When the user has pressed the mouse button, we monitor the mouse cursor. If the cursor
      // moves more than a certain distance, we start the arc-drag operation. Starting this
      // operation is expensive because figuring out where the arc is to be drawn is requires
      // fetching bounding boxes - and this triggers a blocking/expensive reflow operation in
      // the browser.
      var deltaX = Math.abs(this.dragStartedAt.pageX - evt.pageX);
      var deltaY = Math.abs(this.dragStartedAt.pageY - evt.pageY);
      if (deltaX > 5 || deltaY > 5) {
        this.arcOptions = null;
        var target = $(this.dragStartedAt.target);
        var id = target.attr('data-span-id');
        this.startArcDrag(id);

        // BEGIN WEBANNO EXTENSION - #724 - Cross-row selection is jumpy
        // If user starts selecting text, suppress all pointer events on annotations to
        // avoid the selection jumping around. During selection, we don't need the annotations
        // to react on mouse events anyway.
        if (target.attr('data-chunk-id')) {
          $(this.svgElement).children('.row, .sentnum').each((index, row) => {
            $(row).css('pointer-events', 'none');
          });
        }
        // END WEBANNO EXTENSION - #724 - Cross-row selection is jumpy
      }
    }
    // END WEBANNO EXTENSION - #1610 - Improve brat visualization interaction performance

    if (this.arcDragOrigin) {
      if (this.arcDragJustStarted) {
        // show the possible targets
        var span = this.data.spans[this.arcDragOrigin] || {};
        var spanDesc = this.spanTypes[span.type] || {};

        // separate out possible numeric suffix from type for highlight
        // (instead of e.g. "Theme3", need to look for "Theme")
        var noNumArcType = this.stripNumericSuffix(this.arcOptions && this.arcOptions.type);
        // var targetClasses = [];
        var $targets = $();
        $.each(spanDesc.arcs || [], (possibleArcNo, possibleArc) => {
          if ((this.arcOptions && possibleArc.type == noNumArcType) || !(this.arcOptions && this.arcOptions.old_target)) {
            $.each(possibleArc.targets || [], (possibleTargetNo, possibleTarget) => {
              // speedup for #642: relevant browsers should support
              // this function: http://www.quirksmode.org/dom/w3c_core.html#t11
              // so we get off jQuery and get down to the metal:
              // targetClasses.push('.span_' + possibleTarget);
              $targets = $targets.add(this.svgElement[0].getElementsByClassName('span_' + possibleTarget));
            });
          }
        });
        // $(targetClasses.join(',')).not('[data-span-id="' + arcDragOrigin + '"]').addClass('reselectTarget');
        // WEBANNO EXTENSION BEGIN - #277 - self-referencing arcs for custom layers
        if (evt.shiftKey) {
          $targets.addClass('reselectTarget');
        }
        else {
          $targets.not('[data-span-id="' + this.arcDragOrigin + '"]').addClass('reselectTarget');
        }
        // WEBANNO EXTENSION END - #277 - self-referencing arcs for custom layers 
      }
      this.clearSelection();
      var mx = evt.pageX - this.svgPosition.left;
      var my = evt.pageY - this.svgPosition.top + 5; // TODO FIXME why +5?!?
      var y = Math.min(this.arcDragOriginBox.y, my) - this.draggedArcHeight;
      var dx = (this.arcDragOriginBox.center - mx) / 4;
      var path = this.svg.createPath().
        move(this.arcDragOriginBox.center, this.arcDragOriginBox.y).
        curveC(this.arcDragOriginBox.center - dx, y,
          mx + dx, y,
          mx, my);
      this.arcDragArc.setAttribute('d', path.path());
    } else {
      // A. Scerri FireFox chunk

      // if not, then is it span selection? (ctrl key cancels)
      var sel = window.getSelection();
      var chunkIndexFrom = sel.anchorNode && $(sel.anchorNode.parentNode).attr('data-chunk-id');
      var chunkIndexTo = sel.focusNode && $(sel.focusNode.parentNode).attr('data-chunk-id');
      // fallback for firefox (at least):
      // it's unclear why, but for firefox the anchor and focus
      // node parents are always undefined, the the anchor and
      // focus nodes themselves do (often) have the necessary
      // chunk ID. However, anchor offsets are almost always
      // wrong, so we'll just make a guess at what the user might
      // be interested in tagging instead of using what's given.
      var anchorOffset = null;
      var focusOffset = null;
      if (chunkIndexFrom === undefined && chunkIndexTo === undefined &&
        $(sel.anchorNode).attr('data-chunk-id') &&
        $(sel.focusNode).attr('data-chunk-id')) {
        // Lets take the actual selection range and work with that
        // Note for visual line up and more accurate positions a vertical offset of 8 and horizontal of 2 has been used!
        var range = sel.getRangeAt(0);
        var svgOffset = $(this.svg._svg).offset();
        var flip = false;
        var tries = 0;
        // First try and match the start offset with a position, if not try it against the other end
        while (tries < 2) {
          var sp = this.svg._svg.createSVGPoint();
          sp.x = (flip ? evt.pageX : this.dragStartedAt.pageX) - svgOffset.left;
          sp.y = (flip ? evt.pageY : this.dragStartedAt.pageY) - (svgOffset.top + 8);
          var startsAt = range.startContainer;
          anchorOffset = startsAt.getCharNumAtPosition(sp);
          chunkIndexFrom = startsAt && $(startsAt).attr('data-chunk-id');
          if (anchorOffset != -1) {
            break;
          }
          flip = true;
          tries++;
        }

        // Now grab the end offset
        sp.x = (flip ? this.dragStartedAt.pageX : evt.pageX) - svgOffset.left;
        sp.y = (flip ? this.dragStartedAt.pageY : evt.pageY) - (svgOffset.top + 8);
        var endsAt = range.endContainer;
        focusOffset = endsAt.getCharNumAtPosition(sp);

        // If we cannot get a start and end offset stop here
        if (anchorOffset == -1 || focusOffset == -1) {
          return;
        }
        // If we are in the same container it does the selection back to front when dragged right to left, across different containers the start is the start and the end if the end!
        if (range.startContainer == range.endContainer && anchorOffset > focusOffset) {
          var t = anchorOffset;
          anchorOffset = focusOffset;
          focusOffset = t;
          flip = false;
        }
        chunkIndexTo = endsAt && $(endsAt).attr('data-chunk-id');

        // Now take the start and end character rectangles
        let startRec = startsAt.getExtentOfChar(anchorOffset);
        startRec.y += 2;
        let endRec = endsAt.getExtentOfChar(focusOffset);
        endRec.y += 2;

        // If nothing has changed then stop here
        if (this.lastStartRec != null && this.lastStartRec.x == startRec.x && this.lastStartRec.y == startRec.y && this.lastEndRec != null && this.lastEndRec.x == endRec.x && this.lastEndRec.y == endRec.y) {
          return;
        }

        if (this.selRect == null) {
          var rx = startRec.x;
          var ry = startRec.y;
          var rw = (endRec.x + endRec.width) - startRec.x;
          if (rw < 0) {
            rx += rw;
            rw = -rw;
          }
          var rh = Math.max(startRec.height, endRec.height);

          this.selRect = new Array();
          var activeSelRect = this.makeSelRect(rx, ry, rw, rh);
          this.selRect.push(activeSelRect);
          startsAt.parentNode.parentNode.parentNode.insertBefore(activeSelRect, startsAt.parentNode.parentNode);
        } else {
          if (startRec.x != this.lastStartRec.x && endRec.x != this.lastEndRec.x && (startRec.y != this.lastStartRec.y || endRec.y != this.lastEndRec.y)) {
            if (startRec.y < this.lastStartRec.y) {
              this.selRect[0].setAttributeNS(null, "width", this.lastStartRec.width);
              this.lastEndRec = this.lastStartRec;
            } else if (endRec.y > this.lastEndRec.y) {
              this.selRect[this.selRect.length - 1].setAttributeNS(null, "x",
                parseFloat(this.selRect[this.selRect.length - 1].getAttributeNS(null, "x"))
                + parseFloat(this.selRect[this.selRect.length - 1].getAttributeNS(null, "width"))
                - this.lastEndRec.width);
              this.selRect[this.selRect.length - 1].setAttributeNS(null, "width", 0);
              this.lastStartRec = this.lastEndRec;
            }
          }

          // Start has moved
          var flip = !(startRec.x == this.lastStartRec.x && startRec.y == this.lastStartRec.y);
          // If the height of the start or end changed we need to check whether
          // to remove multi line highlights no longer needed if the user went back towards their start line
          // and whether to create new ones if we moved to a newline
          if (((endRec.y != this.lastEndRec.y)) || ((startRec.y != this.lastStartRec.y))) {
            // First check if we have to remove the first highlights because we are moving towards the end on a different line
            var ss = 0;
            for (; ss != this.selRect.length; ss++) {
              if (startRec.y <= parseFloat(this.selRect[ss].getAttributeNS(null, "y"))) {
                break;
              }
            }
            // Next check for any end highlights if we are moving towards the start on a different line
            var es = this.selRect.length - 1;
            for (; es != -1; es--) {
              if (endRec.y >= parseFloat(this.selRect[es].getAttributeNS(null, "y"))) {
                break;
              }
            }
            // TODO put this in loops above, for efficiency the array slicing could be done separate still in single call
            var trunc = false;
            if (ss < this.selRect.length) {
              for (var s2 = 0; s2 != ss; s2++) {
                this.selRect[s2].parentNode.removeChild(this.selRect[s2]);
                es--;
                trunc = true;
              }
              this.selRect = this.selRect.slice(ss);
            }
            if (es > -1) {
              for (var s2 = this.selRect.length - 1; s2 != es; s2--) {
                this.selRect[s2].parentNode.removeChild(this.selRect[s2]);
                trunc = true;
              }
              this.selRect = this.selRect.slice(0, es + 1);
            }

            // If we have truncated the highlights we need to readjust the last one
            if (trunc) {
              let activeSelRect = flip ? this.selRect[0] : this.selRect[this.selRect.length - 1];
              if (flip) {
                var rw = 0;
                if (startRec.y == endRec.y) {
                  rw = (endRec.x + endRec.width) - startRec.x;
                } else {
                  rw = (parseFloat(activeSelRect.getAttributeNS(null, "x"))
                    + parseFloat(activeSelRect.getAttributeNS(null, "width")))
                    - startRec.x;
                }
                activeSelRect.setAttributeNS(null, "x", startRec.x);
                activeSelRect.setAttributeNS(null, "y", startRec.y);
                activeSelRect.setAttributeNS(null, "width", rw.toString());
              } else {
                var rw = (endRec.x + endRec.width) - parseFloat(activeSelRect.getAttributeNS(null, "x"));
                activeSelRect.setAttributeNS(null, "width", rw.toString());
              }
            } else {
              // We didnt truncate anything but we have moved to a new line so we need to create a new highlight
              var lastSel = flip ? this.selRect[0] : this.selRect[this.selRect.length - 1];
              var startBox = startsAt.parentNode.getBBox();
              var endBox = endsAt.parentNode.getBBox();

              if (flip) {
                lastSel.setAttributeNS(null, "width",
                  (parseFloat(lastSel.getAttributeNS(null, "x"))
                    + parseFloat(lastSel.getAttributeNS(null, "width")))
                  - endBox.x);
                lastSel.setAttributeNS(null, "x", endBox.x);
              } else {
                lastSel.setAttributeNS(null, "width",
                  (startBox.x + startBox.width)
                  - parseFloat(lastSel.getAttributeNS(null, "x")));
              }
              var rx = 0;
              var ry = 0;
              var rw = 0;
              var rh = 0;
              if (flip) {
                rx = startRec.x;
                ry = startRec.y;
                rw = $(this.svg._svg).width() - startRec.x;
                rh = startRec.height;
              } else {
                rx = endBox.x;
                ry = endRec.y;
                rw = (endRec.x + endRec.width) - endBox.x;
                rh = endRec.height;
              }
              var newRect = this.makeSelRect(rx, ry, rw, rh);
              if (flip) {
                this.selRect.unshift(newRect);
              } else {
                this.selRect.push(newRect);
              }

              // Place new highlight in appropriate slot in SVG graph
              startsAt.parentNode.parentNode.parentNode.insertBefore(newRect, startsAt.parentNode.parentNode);
            }
          } else {
            // The user simply moved left or right along the same line so just adjust the current highlight
            let activeSelRect = flip ? this.selRect[0] : this.selRect[this.selRect.length - 1];
            // If the start moved shift the highlight and adjust width
            if (flip) {
              var rw = (parseFloat(activeSelRect.getAttributeNS(null, "x"))
                + parseFloat(activeSelRect.getAttributeNS(null, "width")))
                - startRec.x;
              activeSelRect.setAttributeNS(null, "x", startRec.x);
              activeSelRect.setAttributeNS(null, "y", startRec.y);
              activeSelRect.setAttributeNS(null, "width", rw);
            } else {
              // If the end moved then simple change the width
              var rw = (endRec.x + endRec.width)
                - parseFloat(activeSelRect.getAttributeNS(null, "x"));
              activeSelRect.setAttributeNS(null, "width", rw);
            }
          }
        }
        this.lastStartRec = startRec;
        this.lastEndRec = endRec;
      }
    }
    this.arcDragJustStarted = false;
  };

  adjustToCursor(evt, element, centerX, centerY) {
    var screenHeight = $(window).height() - 8; // TODO HACK - no idea why -8 is needed
    var screenWidth = $(window).width() - 8;
    var elementHeight = element.height();
    var elementWidth = element.width();
    var cssSettings = {};
    var eLeft;
    var eTop;
    if (centerX) {
      eLeft = evt.clientX - elementWidth / 2;
    } else {
      eLeft = evt.clientX;
    }
    if (centerY) {
      eTop = evt.clientY - elementHeight / 2;
    } else {
      eTop = evt.clientY;
    }
    // Try to make sure the element doesn't go off-screen.
    // If this isn't possible (the element is larger than the screen),
    // alight top-left corner of screen and dialog as a compromise.
    if (screenWidth > elementWidth) {
      eLeft = Math.min(Math.max(eLeft, 0), screenWidth - elementWidth);
    } else {
      eLeft = 0;
    }
    if (screenHeight > elementHeight) {
      eTop = Math.min(Math.max(eTop, 0), screenHeight - elementHeight);
    } else {
      eTop = 0;
    }
    element.css({ top: eTop, left: eLeft });
  };

  updateCheckbox($input) {
    var $widget = $input.button('widget');
    var $textspan = $widget.find('.ui-button-text');
    $textspan.html(($input[0].checked ? '&#x2611; ' : '&#x2610; ') + $widget.attr('data-bare'));
  };

  fillSpanTypesAndDisplayForm(evt, offsets, spanText, span, id?) {

    if (id) {
      this.dispatcher.post('ajax', [{
        action: 'spanOpenDialog',
        offsets: $.toJSON(offsets),
        id: id,
        type: span.type,
        spanText: spanText
      }, 'serverResult']);
    }
    else {
      this.dispatcher.post('ajax', [{
        action: 'spanOpenDialog',
        offsets: $.toJSON(offsets),
        spanText: spanText
      }, 'serverResult']);
    }
  };
  // WEBANNO EXTENSION END

  submitReselect() {
    $(this.reselectedSpan.rect).removeClass('reselect');
    this.reselectedSpan = null;
    this.spanForm.submit();
  };

  clearSpanNotes(evt) {
    $('#span_notes').val('');
  }


  clearSpanNorm(evt) {
    this.clearNormalizationUI();
  }

  // invoked on response to ajax request for id lookup
  setSpanNormText(response) {
    if (response.exception) {
      // TODO: better response to failure
      this.dispatcher.post('messages', [[['Lookup error', 'warning', -1]]]);
      return false;
    }
    // set input style according to whether we have a valid value
    var $idinput = $('#span_norm_id');
    // TODO: make sure the key echo in the response matches the
    // current value of the $idinput
    $idinput.removeClass('valid_value').removeClass('invalid_value');
    if (response.value === null) {
      $idinput.addClass('invalid_value');
      this.hideNormalizationRefLink();
    } else {
      $idinput.addClass('valid_value');
      this.updateNormalizationRefLink();
    }
    $('#span_norm_txt').val(response.value);
  }

  // on any change to the normalization DB, clear everything and
  // update link
  spanNormDbUpdate(evt) {
    this.clearNormalizationUI();
    this.updateNormalizationDbLink();
  }

  // on any change to the normalization ID, update the text of the
  // reference
  spanNormIdUpdate(evt) {
    var key = $(this).val();
    var db = $('#span_norm_db').val();
    if (key != this.oldSpanNormIdValue) {
      if (key.match(/^\s*$/)) {
        // don't query empties, just clear instead
        this.clearNormalizationUI();
      } else {
        this.dispatcher.post('ajax', [{
          action: 'normGetName',
          database: db,
          key: key,
          collection: this.coll
        }, 'normGetNameResult']);
      }
      this.oldSpanNormIdValue = key;
    }
  }

  // We send a request to the backend to open the dialog
  fillArcTypesAndDisplayForm(evt, originSpanId, originType, targetSpanId, targetType, arcType?, arcId?) {

    if (arcId) {
      this.dispatcher.post('ajax', [{
        action: 'arcOpenDialog',
        arcId: arcId,
        arcType: arcType,
        originSpanId: originSpanId,
        originType: originType,
        targetSpanId: targetSpanId,
        targetType: targetType

      }, 'serverResult']);
    }
    else {
      this.dispatcher.post('ajax', [{
        action: 'arcOpenDialog',
        originSpanId: originSpanId,
        originType: originType,
        targetSpanId: targetSpanId,
        targetType: targetType
      }, 'serverResult']);
    }
  };

  stopArcDrag(target?) {
    // BEGIN WEBANNO EXTENSION - #1610 - Improve brat visualization interaction performance
    // Clear the dragStartAt saved event
    this.dragStartedAt = null;
    // END WEBANNO EXTENSION - #1610 - Improve brat visualization interaction performance
    if (this.arcDragOrigin) {
      if (!target) {
        target = $('.badTarget');
      }
      target.removeClass('badTarget');
      this.arcDragOriginGroup.removeClass('highlight');
      if (target) {
        target.parent().removeClass('highlight');
      }
      if (this.arcDragArc) {
        try {
          this.svg.remove(this.arcDragArc);
        }
        catch (err) {
          // Ignore - could be spurious TypeError: null is not an object (evaluating 'a.parentNode.removeChild')
        }
        this.arcDrag = null;
      }
      this.arcDragOrigin = null;
      if (this.arcOptions) {
        $('g[data-from="' + this.arcOptions.origin + '"][data-to="' + this.arcOptions.target + '"]').removeClass('reselect');
      }
      this.svgElement.removeClass('reselect');
    }
    this.svgElement.removeClass('unselectable');
    $('.reselectTarget').removeClass('reselectTarget');
  };

  onMouseUp(evt) {
    if (this.user === null) return;

    // BEGIN WEBANNO EXTENSION - #724 - Cross-row selection is jumpy
    // Restore pointer events on annotations
    if (this.dragStartedAt && $(this.dragStartedAt.target).attr('data-chunk-id')) {
      $(this.svgElement).children('.row, .sentnum').each((index, row) => {
        $(row).css('pointer-events', 'auto');
      });
    }
    // END WEBANNO EXTENSION - #724 - Cross-row selection is jumpy

    var target = $(evt.target);

    // three things that are clickable in SVG
    var targetSpanId = target.data('span-id');
    var targetChunkId = target.data('chunk-id');
    var targetArcRole = target.data('arc-role');
    // BEGIN WEBANNO EXTENSION - #1579 - Send event when action-clicking on a relation
    /*
            if (!(targetSpanId !== undefined || targetChunkId !== undefined || targetArcRole !== undefined)) {
    */
    // The targetArcRole check must be excluded from the negation - it cancels this handler when
    // doing a mouse-up on a relation
    if (!(targetSpanId !== undefined || targetChunkId !== undefined) || targetArcRole !== undefined) {
      // END WEBANNO EXTENSION - #1579 - Send event when action-clicking on a relation
      // misclick
      this.clearSelection();
      this.stopArcDrag(target);
      return;
    }

    // is it arc drag end?
    if (this.arcDragOrigin) {
      var origin = this.arcDragOrigin;
      var targetValid = target.hasClass('reselectTarget');
      this.stopArcDrag(target);
      // WEBANNO EXTENSION BEGIN - #277 - self-referencing arcs for custom layers 
      let id;
      if ((id = target.attr('data-span-id')) && targetValid && (evt.shiftKey || origin != id)) {
        //          if ((id = target.attr('data-span-id')) && origin != id && targetValid) {
        // WEBANNO EXTENSION END - #277 - self-referencing arcs for custom layers 
        var originSpan = this.data.spans[origin];
        var targetSpan = this.data.spans[id];
        if (this.arcOptions && this.arcOptions.old_target) {
          this.arcOptions.target = targetSpan.id;
          this.dispatcher.post('ajax', [this.arcOptions, 'edited']);
        } else {
          this.arcOptions = {
            action: 'createArc',
            origin: originSpan.id,
            target: targetSpan.id,
            collection: this.coll,
            'document': this.doc
          };
          $('#arc_origin').text(Util.spanDisplayForm(this.spanTypes, originSpan.type) + ' ("' + originSpan.text + '")');
          $('#arc_target').text(Util.spanDisplayForm(this.spanTypes, targetSpan.type) + ' ("' + targetSpan.text + '")');
          this.fillArcTypesAndDisplayForm(evt, originSpan.id, originSpan.type, targetSpan.id, targetSpan.type);
          // for precise timing, log dialog display to user.
          this.dispatcher.post('logAction', ['arcSelected']);
        }
      }
    } else if (!evt.ctrlKey) {
      // if not, then is it span selection? (ctrl key cancels)
      var sel = window.getSelection();

      // Try getting anchor and focus node via the selection itself. This works in Chrome and
      // Safari.
      var anchorNode = sel.anchorNode && $(sel.anchorNode).closest('*[data-chunk-id]');
      var anchorOffset = sel.anchorOffset;
      var focusNode = sel.focusNode && $(sel.focusNode).closest('*[data-chunk-id]');
      var focusOffset = sel.focusOffset;

      // If using the selection was not successful, try using the ranges instead. This should
      // work on Firefox.
      if ((anchorNode == null || !anchorNode[0] || focusNode == null || !focusNode[0]) && sel.type != "None") {
        anchorNode = $(sel.getRangeAt(0).startContainer).closest('*[data-chunk-id]');
        anchorOffset = sel.getRangeAt(0).startOffset;
        focusNode = $(sel.getRangeAt(sel.rangeCount - 1).endContainer).closest('*[data-chunk-id]');
        focusOffset = sel.getRangeAt(sel.rangeCount - 1).endOffset;
      }

      // If neither approach worked, give up - the user didn't click on selectable text.
      if (anchorNode == null || !anchorNode[0] || focusNode == null || !focusNode[0]) {
        this.clearSelection();
        this.stopArcDrag(target);
        return;
      }

      var chunkIndexFrom = anchorNode && anchorNode.attr('data-chunk-id');
      var chunkIndexTo = focusNode && focusNode.attr('data-chunk-id');

      // BEGIN WEBANNO EXTENSION - #316 Text selection behavior while dragging mouse
      // BEGIN WEBANNO EXTENSION - #724 - Cross-row selection is jumpy
      if (focusNode && anchorNode && focusNode[0] == anchorNode[0] && focusNode.hasClass('spacing')) {
        if (evt.shiftKey) {
          if (anchorOffset == 0) {
            // Move anchor to the end of the previous node
            anchorNode = focusNode = anchorNode.prev();
            anchorOffset = focusOffset = anchorNode.text().length;
            chunkIndexFrom = chunkIndexTo = anchorNode.attr('data-chunk-id');
          }
          else {
            // Move anchor to the beginning of the next node
            anchorNode = focusNode = anchorNode.next();
            anchorOffset = focusOffset = 0;
            chunkIndexFrom = chunkIndexTo = anchorNode.attr('data-chunk-id');
          }
        }
        else {
          // misclick
          this.clearSelection();
          this.stopArcDrag(target);
          return;
        }
      }
      else {
        // If we hit a spacing element, then we shift the anchors left or right, depending on
        // the direction of the selected range.
        if (anchorNode.hasClass('spacing')) {
          if (Number(chunkIndexFrom) < Number(chunkIndexTo)) {
            anchorNode = anchorNode.next();
            anchorOffset = 0;
            chunkIndexFrom = anchorNode.attr('data-chunk-id');
          }
          else if (anchorNode.hasClass('row-initial')) {
            anchorNode = anchorNode.next();
            anchorOffset = 0;
          }
          else {
            anchorNode = anchorNode.prev();
            anchorOffset = anchorNode.text().length;
          }
        }
        if (focusNode.hasClass('spacing')) {
          if (Number(chunkIndexFrom) > Number(chunkIndexTo)) {
            focusNode = focusNode.next();
            focusOffset = 0;
            chunkIndexTo = focusNode.attr('data-chunk-id');
          }
          else if (focusNode.hasClass('row-initial')) {
            focusNode = focusNode.next();
            focusOffset = 0;
          }
          else {
            focusNode = focusNode.prev();
            focusOffset = focusNode.text().length;
          }
        }
      }
      // END WEBANNO EXTENSION - #724 - Cross-row selection is jumpy
      // END WEBANNO EXTENSION - #316 Text selection behavior while dragging mouse

      if (chunkIndexFrom !== undefined && chunkIndexTo !== undefined) {
        var chunkFrom = this.data.chunks[chunkIndexFrom];
        var chunkTo = this.data.chunks[chunkIndexTo];
        var selectedFrom = chunkFrom.from + anchorOffset;
        var selectedTo = chunkTo.from + focusOffset;
        sel.removeAllRanges();

        if (selectedFrom > selectedTo) {
          var tmp = selectedFrom; selectedFrom = selectedTo; selectedTo = tmp;
        }
        // trim
        while (selectedFrom < selectedTo && " \n\t".indexOf(this.data.text.substr(selectedFrom, 1)) !== -1) selectedFrom++;
        while (selectedFrom < selectedTo && " \n\t".indexOf(this.data.text.substr(selectedTo - 1, 1)) !== -1) selectedTo--;

        // shift+click allows zero-width spans
        if (selectedFrom === selectedTo && !evt.shiftKey) {
          // simple click (zero-width span)
          return;
        }

        var newOffset = [selectedFrom, selectedTo];
        if (this.reselectedSpan) {
          var newOffsets = this.reselectedSpan.offsets.slice(0); // clone
          this.spanOptions.old_offsets = JSON.stringify(this.reselectedSpan.offsets);
          if (this.selectedFragment !== null) {
            if (this.selectedFragment !== false) {
              newOffsets.splice(this.selectedFragment, 1);
            }
            newOffsets.push(newOffset);
            newOffsets.sort(Util.cmpArrayOnFirstElement);
            this.spanOptions.offsets = newOffsets;
          } else {
            this.spanOptions.offsets = [newOffset];
          }
        } else {
          this.spanOptions = {
            action: 'createSpan',
            offsets: [newOffset]
          }
        }

        if (!Configuration.rapidModeOn || this.reselectedSpan != null) {
          // normal span select in standard annotation mode
          // or reselect: show selector
          var spanText = this.data.text.substring(selectedFrom, selectedTo);
          // WEBANNO EXTENSION BEGIN
          this.fillSpanTypesAndDisplayForm(evt, this.spanOptions.offsets, spanText, this.reselectedSpan);
          // WEBANNO EXTENSION END
          // for precise timing, log annotation display to user.
          this.dispatcher.post('logAction', ['spanSelected']);
        } else {
          // normal span select in rapid annotation mode: call
          // server for span type candidates
          var spanText = this.data.text.substring(selectedFrom, selectedTo);
          // TODO: we're currently storing the event to position the
          // span form using adjustToCursor() (which takes an event),
          // but this is clumsy and suboptimal (user may have scrolled
          // during the ajax invocation); think of a better way.
          this.lastRapidAnnotationEvent = evt;
          this.dispatcher.post('ajax', [{
            action: 'suggestSpanTypes',
            collection: this.coll,
            'document': this.doc,
            start: selectedFrom,
            end: selectedTo,
            text: spanText,
            model: $('#rapid_model').val(),
          }, 'suggestedSpanTypes']);
        }
      }
    }
  };

  toggleCollapsible($el, state?) {
    var opening = state !== undefined ? state : !$el.hasClass('open');
    var $collapsible = $el.parent().find('.collapsible:first');
    if (opening) {
      $collapsible.addClass('open');
      $el.addClass('open');
    } else {
      $collapsible.removeClass('open');
      $el.removeClass('open');
    }
  };

  collapseHandler(evt) {
    this.toggleCollapsible($(evt.target));
  }

  rememberData(_data) {
    if (_data && !_data.exception) {
      this.data = _data;
    }
  };

  addSpanTypesToDivInner($parent, types, category?) {
    if (!types) return;

    $.each(types, (typeNo, type) => {
      if (type === null) {
        $parent.append('<hr/>');
      } else {
        var name = type.name;
        var $input = $('<input type="radio" name="span_type"/>').
          attr('id', 'span_' + type.type).
          attr('value', type.type);
        if (category) {
          $input.attr('category', category);
        }
        // use a light version of the span color as BG
        var spanBgColor = this.spanTypes[type.type] && this.spanTypes[type.type].bgColor || '#ffffff';
        spanBgColor = Util.adjustColorLightness(spanBgColor, this.spanBoxTextBgColorLighten);
        var $label = $('<label class="span_type_label"/>').
          attr('for', 'span_' + type.type).
          text(name);
        if (type.unused) {
          $input.attr({
            disabled: 'disabled',
            unused: 'unused'
          });
          $label.css('font-weight', 'bold');
        } else {
          $label.css('background-color', spanBgColor);
        }
        var $collapsible = $('<div class="collapsible open"/>');
        var $content = $('<div class="item_content"/>').
          append($input).
          append($label).
          append($collapsible);
        var $collapser = $('<div class="collapser open"/>');
        var $div = $('<div class="item"/>');
        // WEBANNO EXTENSION BEGIN
        // Avoid exception when no children are set
        if (type.children && type.children.length) {
          // WEBANNO EXTENSION END
          $div.append($collapser)
        }
        $div.append($content);
        this.addSpanTypesToDivInner($collapsible, type.children, category);
        $parent.append($div);
        if (type.hotkey) {
          this.spanKeymap[type.hotkey] = 'span_' + type.type;
          let name = $label.html();
          var replace = true;
          name = name.replace(new RegExp("(&[^;]*?)?(" + type.hotkey + ")", 'gi'),
            (all, entity, letter) => {
              if (replace && !entity) {
                replace = false;
                var hotkey = type.hotkey.toLowerCase() == letter
                  ? type.hotkey.toLowerCase()
                  : type.hotkey.toUpperCase();
                return '<span class="accesskey">' + Util.escapeHTML(hotkey) + '</span>';
              }
              return all;
            });
          $label.html(name);
        }
      }
    });
  };

  addSpanTypesToDiv($top, types, heading) {
    let $scroller = $('<div class="scroller"/>');
    let $legend = $('<legend/>').text(heading);
    let $fieldset = $('<fieldset/>').append($legend).append($scroller);
    $top.append($fieldset);
    this.addSpanTypesToDivInner($scroller, types);
  };

  addAttributeTypesToDiv($top, types, category) {
    $.each(types, (attrNo, attr) => {
      var escapedType = Util.escapeQuotes(attr.type);
      var attrId = category + '_attr_' + escapedType;
      if (attr.unused) {
        var $input = $('<input type="hidden" id="' + attrId + '" value=""/>');
        $top.append($input);
      } else if (attr.bool) {
        var escapedName = Util.escapeQuotes(attr.name);
        var $input = $('<input type="checkbox" id="' + attrId +
          '" value="' + escapedType +
          '" category="' + category + '"/>');
        var $label = $('<label class="attribute_type_label" for="' + attrId +
          '" data-bare="' + escapedName + '">&#x2610; ' +
          escapedName + '</label>');
        $top.append($input).append($label);
        $input.button();
        $input.change(this.onBooleanAttrChange);
      } else {
        var $div = $('<div class="ui-button ui-button-text-only attribute_type_label"/>');
        var $select = $('<select id="' + attrId + '" class="ui-widget ui-state-default ui-button-text" category="' + category + '"/>');
        var $option = $('<option class="ui-state-default" value=""/>').text(attr.name + ': ?');
        $select.append($option);
        $.each(attr.values, (valType, value) => {
          $option = $('<option class="ui-state-active" value="' + Util.escapeQuotes(valType) + '"/>').text(attr.name + ': ' + (value.name || valType));
          $select.append($option);
        });
        $div.append($select);
        $top.append($div);
        $select.change(this.onMultiAttrChange);
      }
    });
  }

  setSpanTypeSelectability(category) {
    // TODO: this implementation is incomplete: we should ideally
    // disable not only categories of types (events or entities),
    // but the specific set of types that are incompatible with
    // the current attribute settings.

    // just assume all attributes are event attributes
    // TODO: support for entity attributes
    // TODO2: the above comment is almost certainly false, check and remove
    $('#span_form input:not([unused])').removeAttr('disabled');
    var $toDisable;
    if (category == "event") {
      $toDisable = $('#span_form input[category="entity"]');
    } else if (category == "entity") {
      $toDisable = $('#span_form input[category="event"]');
    } else {
      console.error('Unrecognized attribute category:', category);
      $toDisable = $();
    }
    var $checkedToDisable = $toDisable.filter(':checked');
    $toDisable.attr('disabled', true);
    // the disable may leave the dialog in a state where nothing
    // is checked, which would cause error on "OK". In this case,
    // check the first valid choice.
    if ($checkedToDisable.length) {
      var $toCheck = $('#span_form input[category="' + category + '"][disabled!="disabled"]:first');
      // so weird, attr('checked', 'checked') fails sometimes, so
      // replaced with more "metal" version
      $toCheck[0].checked = true;
    }
  }

  onMultiAttrChange(evt) {
    if ($(this).val() == '') {
      $('#span_form input:not([unused])').removeAttr('disabled');
    } else {
      var attrCategory = evt.target.getAttribute('category');
      this.setSpanTypeSelectability(attrCategory);
      if (evt.target.selectedIndex) {
        $(evt.target).addClass('ui-state-active');
      } else {
        $(evt.target).removeClass('ui-state-active');
      }
    }
  }

  onBooleanAttrChange(evt) {
    var attrCategory = evt.target.getAttribute('category');
    this.setSpanTypeSelectability(attrCategory);
    this.updateCheckbox($(evt.target));
  };

  rememberSpanSettings(response) {
    this.spanKeymap = {};

    // TODO: check for exceptions in response

    // fill in entity and event types
    var $entityScroller = $('#entity_types div.scroller').empty();
    this.addSpanTypesToDivInner($entityScroller, response.entity_types, 'entity');
    var $eventScroller = $('#event_types div.scroller').empty();
    this.addSpanTypesToDivInner($eventScroller, response.event_types, 'event');

    // fill in attributes
    var $entattrs = $('#entity_attributes div.scroller').empty();
    this.addAttributeTypesToDiv($entattrs, this.entityAttributeTypes, 'entity');

    var $eveattrs = $('#event_attributes div.scroller').empty();
    this.addAttributeTypesToDiv($eveattrs, this.eventAttributeTypes, 'event');
  };

  tagCurrentDocument(taggerId) {
    var tagOptions = {
      action: 'tag',
      collection: this.coll,
      'document': this.doc,
      tagger: taggerId,
    };
    this.dispatcher.post('ajax', [tagOptions, 'edited']);
  }

  setupTaggerUI(response) {
    var taggers = response.ner_taggers || [];
    let $taggerButtons = $('#tagger_buttons').empty();
    $.each(taggers, (taggerNo, tagger) => {
      // expect a tuple with ID, name, model, and URL
      var taggerId = tagger[0];
      var taggerName = tagger[1];
      var taggerModel = tagger[2];
      if (!taggerId || !taggerName || !taggerModel) {
        this.dispatcher.post('messages', [[['Invalid tagger specification received from server', 'error']]]);
        return true; // continue
      }
      var $row = $('<div class="optionRow"/>');
      var $label = $('<span class="optionLabel">' + Util.escapeHTML(taggerName) + '</span>');
      var $button = $('<input id="tag_' + Util.escapeHTML(taggerId) + '_button" type="button" value="' + Util.escapeHTML(taggerModel) + '" tabindex="-1" title="Automatically tag the current document."/>');
      $row.append($label).append($button);
      $taggerButtons.append($row);
      $button.click((evt) => {
        this.tagCurrentDocument(taggerId);
      });
    });
    $taggerButtons.find('input').button();
    // if nothing was set up, hide the whole fieldset and show
    // a message to this effect, else the other way around
    if ($taggerButtons.find('input').length == 0) {
      $('#auto_tagging_fieldset').hide();
      $('#no_tagger_message').show();
    } else {
      $('#auto_tagging_fieldset').show();
      $('#no_tagger_message').hide();
    }
  }

  // recursively traverses type hierarchy (entity_types or
  // event_types) and stores normalizations in normDbsByType.
  rememberNormDbsForType(types) {
    if (!types) return;

    $.each(types, (typeNo, type) => {
      if (type === null) {
        // spacer, no-op
      } else {
        this.normDbsByType[type.type] = type.normalizations || [];
        // WEBANNO EXTENSION BEGIN
        // Avoid exception when no children are set
        if (type.children && type.children.length) {
          // WEBANNO EXTENSION END
          this.rememberNormDbsForType(type.children);
        }
      }
    });
  };

  setupNormalizationUI(response) {
    var norm_resources = response.normalization_config || [];
    var $norm_select = $('#span_norm_db');
    // clear possible existing
    $norm_select.empty();
    // fill in new
    let html = [];
    $.each(norm_resources, (normNo, norm) => {
      var normName = norm[0], normUrl = norm[1], normUrlBase = norm[2];
      var serverDb = norm[3];
      html.push('<option value="' + Util.escapeHTML(normName) + '">' +
        Util.escapeHTML(normName) + '</option>');
      // remember the urls for updates
      this.normDbUrlByDbName[normName] = normUrl;
      this.normDbUrlBaseByDbName[normName] = normUrlBase;
    });
    // remember per-type appropriate DBs
    this.normDbsByType = {};
    this.rememberNormDbsForType(response.entity_types);
    this.rememberNormDbsForType(response.event_types);
    // set up HTML
    $norm_select.html(html.join(''));
    // if we have nothing, just hide the whole thing
    if (!norm_resources.length) {
      $('#norm_fieldset').hide();
    } else {
      $('#norm_fieldset').show();
    }
  }

  // hides the reference link in the normalization UI
  hideNormalizationRefLink() {
    $('#span_norm_ref_link').hide();
  }

  // updates the reference link in the normalization UI according
  // to the current value of the normalization DB and ID.
  updateNormalizationRefLink() {
    var $normId = $('#span_norm_id');
    var $normLink = $('#span_norm_ref_link');
    var normId = $normId.val();
    var $normDb = $('#span_norm_db');
    var normDb = $normDb.val();
    if (!normId || !normDb || normId.match(/^\s*$/)) {
      $normLink.hide();
    } else {
      var base = this.normDbUrlBaseByDbName[normDb];
      // assume hidden unless everything goes through
      $normLink.hide();
      if (!base) {
        // base URL is now optional, just skip link generation if not set
        ;
      } else if (base.indexOf('%s') == -1) {
        this.dispatcher.post('messages', [[['Base URL "' + base + '" for ' + normDb + ' does not contain "%s"', 'error']]]);
      } else {
        // TODO: protect against strange chars in ID
        let link = base.replace('%s', normId);
        $normLink.attr('href', link);
        $normLink.show();
      }
    }
  }

  // updates the DB search link in the normalization UI according
  // to the current value of the normalization DB.
  updateNormalizationDbLink() {
    var $dbLink = $('#span_norm_db_link');
    var $normDb = $('#span_norm_db');
    var normDb = $normDb.val();
    if (!normDb) return; // no normalisation configured
    var link = this.normDbUrlByDbName[normDb];
    if (!link || link.match(/^\s*$/)) {
      this.dispatcher.post('messages', [[['No URL for ' + normDb, 'error']]]);
      $dbLink.hide();
    } else {
      // TODO: protect against weirdness in DB link
      $dbLink.attr('href', link);
      $dbLink.show();
    }
  }

  // resets user-settable normalization-related UI elements to a
  // blank state (does not blank #span_norm_db <select>).
  clearNormalizationUI() {
    var $normId = $('#span_norm_id');
    var $normText = $('#span_norm_txt');
    $normId.val('');
    $normId.removeClass('valid_value').removeClass('invalid_value');
    $normText.val('');
    this.updateNormalizationRefLink();
  }

  // returns the normalizations currently filled in the span
  // dialog, or empty list if there are none
  spanNormalizations() {
    // Note that only no or one normalization is supported in the
    // UI at the moment.
    var normalizations = [];
    var normDb = $('#span_norm_db').val();
    var normId = $('#span_norm_id').val();
    var normText = $('#span_norm_txt').val();
    // empty ID -> no normalization
    if (!normId.match(/^\s*$/)) {
      normalizations.push([normDb, normId, normText]);
    }
    return normalizations;
  }

  // returns attributes that are valid for the selected type in
  // the span dialog
  spanAttributes(typeRadio) {
    typeRadio = typeRadio || $('#span_form input:radio:checked');
    var attributes = {};
    var attributeTypes;
    var category = typeRadio.attr('category');
    if (category == 'entity') {
      attributeTypes = this.entityAttributeTypes;
    } else if (category == 'event') {
      attributeTypes = this.eventAttributeTypes;
    } else {
      console.error('Unrecognized type category:', category);
    }
    $.each(attributeTypes, (attrNo, attr) => {
      var $input = $('#' + category + '_attr_' + Util.escapeQuotes(attr.type));
      if (attr.bool) {
        attributes[attr.type] = $input[0].checked;
      } else if ($input[0].selectedIndex) {
        attributes[attr.type] = $input.val();
      }
    });
    return attributes;
  }

  spanAndAttributeTypesLoaded(_spanTypes, _entityAttributeTypes, _eventAttributeTypes, _relationTypesHash) {
    this.spanTypes = _spanTypes;
    this.entityAttributeTypes = _entityAttributeTypes;
    this.eventAttributeTypes = _eventAttributeTypes;
    this.relationTypesHash = _relationTypesHash;
    // for easier access
    this.allAttributeTypes = $.extend({}, this.entityAttributeTypes, this.eventAttributeTypes);
  };

  gotCurrent(_coll, _doc, _args) {
    this.coll = _coll;
    this.doc = _doc;
    this.args = _args;
  };

  edited(response) {
    var x = response.exception;
    if (x) {
      if (x == 'annotationIsReadOnly') {
        this.dispatcher.post('messages', [[["This document is read-only and can't be edited.", 'error']]]);
      } else if (x == 'spanOffsetOverlapError') {
        // createSpan with overlapping frag offsets; reset offsets
        // @amadanmath: who holds the list of offsets for a span?
        // how to reset this?
      } else {
        this.dispatcher.post('messages', [[['Unknown error ' + x, 'error']]]);
      }
      if (this.reselectedSpan) {
        $(this.reselectedSpan.rect).removeClass('reselect');
        this.reselectedSpan = null;
      }
      this.svgElement.removeClass('reselect');
      $('#waiter').dialog('close');
    } else {
      if (response.edited == undefined) {
        console.warn('Warning: server response to edit has', response.edited, 'value for "edited"');
      } else {
        this.args.edited = response.edited;
      }
      var sourceData = response.annotations;
      sourceData.document = this.doc;
      sourceData.collection = this.coll;
      // this "prevent" is to protect against reloading (from the
      // server) the very data that we just received as part of the
      // response to the edit.
      if (response.undo != undefined) {
        this.undoStack.push([this.coll, sourceData.document, response.undo]);
      }
      this.dispatcher.post('preventReloadByURL');
      this.dispatcher.post('setArguments', [this.args]);
      this.dispatcher.post('renderData', [sourceData]);
    }
  };

  preventDefault(evt) {
    evt.preventDefault();
  }

  // WEBANNO EXTENSION BEGIN - #1388 Support context menu
  contextMenu(evt) {
    // If the user shift-right-clicks, open the normal browser context menu. This is useful
    // e.g. during debugging / developing
    if (evt.shiftKey) {
      return;
    }

    this.stopArcDrag();

    var target = $(evt.target);
    var id;
    if (id = target.attr('data-span-id')) {
      this.preventDefault(evt);
      var offsets = [];
      $.each(this.data.spans[id], (fragmentNo, fragment) => {
        offsets.push([fragment.from, fragment.to]);
      });
      this.dispatcher.post('ajax', [{
        action: 'contextMenu',
        offsets: $.toJSON(offsets),
        id: id,
        type: this.data.spans[id].type,
        clientX: evt.clientX,
        clientY: evt.clientY
      }, 'serverResult']);
    }
  }
  // WEBANNO EXTENSION END - #1388 Support context menu

  isReloadOkay() {
    // do not reload while the user is in the middle of editing
    return this.arcDragOrigin == null && this.reselectedSpan == null;
  };

  userReceived(_user) {
    this.user = _user;
  }

  setAnnotationSpeed(speed) {
    if (speed == 1) {
      Configuration.confirmModeOn = true;
    } else {
      Configuration.confirmModeOn = false;
    }
    if (speed == 3) {
      Configuration.rapidModeOn = true;
    } else {
      Configuration.rapidModeOn = false;
    }
    this.dispatcher.post('configurationChanged');
  };

  onNewSourceData(_sourceData) {
    this.sourceData = _sourceData;
  }

  init() {
    this.dispatcher.post('annotationIsAvailable');
  }
}

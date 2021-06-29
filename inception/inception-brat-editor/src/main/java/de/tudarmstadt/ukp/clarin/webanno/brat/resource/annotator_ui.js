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
// -*- Mode: JavaScript; tab-width: 2; indent-tabs-mode: nil; -*-
// vim:set ft=javascript ts=2 sw=2 sts=2 cindent:
var AnnotatorUI = (function ($, window, undefined) {
  var AnnotatorUI = function (dispatcher, svg) {
    var that = this;
    var arcDragOrigin = null;
    var arcDragOriginBox = null;
    var arcDragOriginGroup = null;
    var arcDragArc = null;
    var arcDragJustStarted = false;
    var sourceData = null;
    var data = null;
    var searchConfig = null;
    var spanOptions = null;
    var rapidSpanOptions = null;
    var arcOptions = null;
    var spanKeymap = null;
    var keymap = null;
    var coll = null;
    var doc = null;
    var reselectedSpan = null;
    var selectedFragment = null;
    var editedSpan = null;
    var editedFragment = null;
    var repeatingArcTypes = [];
    var spanTypes = null;
    var entityAttributeTypes = null;
    var eventAttributeTypes = null;
    var allAttributeTypes = null; // TODO: temp workaround, remove
    var relationTypesHash = null;
    var showValidAttributes; // callback function
    var showValidNormalizations; // callback function
    var dragStartedAt = null;
    var selRect = null;
    var lastStartRec = null;
    var lastEndRec = null;

    var draggedArcHeight = 30;
    var spanTypesToShowBeforeCollapse = 30;
    var maxNormSearchHistory = 10;

    // TODO: this is an ugly hack, remove (see comment with assignment)
    var lastRapidAnnotationEvent = null;
    // TODO: another avoidable global; try to work without
    var rapidAnnotationDialogVisible = false;

    // amount by which to lighten (adjust "L" in HSL space) span
    // colors for type selection box BG display. 0=no lightening,
    // 1=white BG (no color)
    var spanBoxTextBgColorLighten = 0.4;

    // for normalization: URLs bases by norm DB name
    var normDbUrlByDbName = {};
    var normDbUrlBaseByDbName = {};
    // for normalization: appropriate DBs per type
    var normDbsByType = {};
    // for normalization
    var oldSpanNormIdValue = '';
    var lastNormSearches = [];

    that.user = null;
    var svgElement = $(svg._svg);
    var svgId = svgElement.parent().attr('id');

    var stripNumericSuffix = function (s) {
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

    var clearSelection = function () {
      window.getSelection().removeAllRanges();
      if (selRect != null) {
        for (var s = 0; s != selRect.length; s++) {
          selRect[s].parentNode.removeChild(selRect[s]);
        }
        selRect = null;
        lastStartRec = null;
        lastEndRec = null;
      }
    };

    var makeSelRect = function (rx, ry, rw, rh, col) {
      var selRect = document.createElementNS("http://www.w3.org/2000/svg", "rect");
      selRect.setAttributeNS(null, "width", rw);
      selRect.setAttributeNS(null, "height", rh);
      selRect.setAttributeNS(null, "x", rx);
      selRect.setAttributeNS(null, "y", ry);
      selRect.setAttributeNS(null, "fill", col == undefined ? "lightblue" : col);
      return selRect;
    };

    var onKeyDown = function (evt) {
      var code = evt.which;

      if (code === $.ui.keyCode.ESCAPE) {
        stopArcDrag();
        if (reselectedSpan) {
          $(reselectedSpan.rect).removeClass('reselect');
          reselectedSpan = null;
          svgElement.removeClass('reselect');
        }
        return;
      }

      // in rapid annotation mode, prioritize the keys 0..9 for the
      // ordered choices in the quick annotation dialog.
      if (Configuration.rapidModeOn && rapidAnnotationDialogVisible &&
        "0".charCodeAt() <= code && code <= "9".charCodeAt()) {
        var idx = String.fromCharCode(code);
        var $input = $('#rapid_span_' + idx);
        if ($input.length) {
          $input.click();
        }
      }

      if (!keymap) return;

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
      var binding = keymap[prefix + code];
      if (!binding) binding = keymap[prefix + String.fromCharCode(code)];
      if (binding) {
        var boundInput = $('#' + binding)[0];
        if (boundInput && !boundInput.disabled) {
          boundInput.click();
          evt.preventDefault();
          return false;
        }
      }
    };

    // WEBANNO EXTENSION BEGIN - #520 Perform javascript action on click 
    var clickCount = 0;
    var clickTimer = null;
    var CLICK_DELAY = 300;

    // Distinguish between double clicks and single clicks . This is relevant when clicking on 
    // annotations. For clicking on text nodes, this is not really relevant.
    var onClick = function (evt) {
      clickCount++;

      var singleClickAction = Configuration.singleClickEdit ?
        editAnnotation : customJSAction;
      var doubleClickAction = Configuration.singleClickEdit ?
        customJSAction : editAnnotation;

      if (clickCount === 1) {
        clickTimer = setTimeout(function () {
          try {
            singleClickAction.call(self, evt); // perform single-click action
          }
          finally {
            clickCount = 0;                    // after action performed, reset counter
          }
        }, CLICK_DELAY);
      } else {
        clearTimeout(clickTimer);              // prevent single-click action
        try {
          doubleClickAction.call(self, evt);   // perform double-click action
        }
        finally {
          clickCount = 0;                      // after action performed, reset counter
        }
      }
    }

    var customJSAction = function (evt) {
      // must be logged in
      if (that.user === null) return;
      var target = $(evt.target);
      var id;
      // single click actions for spans
      if (id = target.attr('data-span-id')) {
        preventDefault(evt);
        editedSpan = data.spans[id];
        editedFragment = target.attr('data-fragment-id');
        var offsets = [];
        $.each(editedSpan.fragments, function (fragmentNo, fragment) {
          offsets.push([fragment.from, fragment.to]);
        });
        dispatcher.post('ajax', [{
          action: 'doAction',
          offsets: $.toJSON(offsets),
          id: id,
          labelText: editedSpan.labelText,
          type: editedSpan.type
        }, 'serverResult']);
      }
      // BEGIN WEBANNO EXTENSION - #1579 - Send event when action-clicking on a relation
      else if (id = target.attr('data-arc-ed')) {
        var type = target.attr('data-arc-role');
        var originSpan = data.spans[target.attr('data-arc-origin')];
        var targetSpan = data.spans[target.attr('data-arc-target')];

        dispatcher.post('ajax', [{
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
        preventDefault(evt);
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
    var editAnnotation = function (evt) {
      // WEBANNO EXTENSION END - #863 Allow configuration of default value for "auto-scroll" etc.
      // must be logged in
      if (that.user === null) return;
      // must not be reselecting a span or an arc
      if (reselectedSpan || arcDragOrigin) return;

      var target = $(evt.target);
      var id;

      // do we edit an arc?
      if (id = target.attr('data-arc-role')) {
        // TODO
        clearSelection();
        var originSpanId = target.attr('data-arc-origin');
        var targetSpanId = target.attr('data-arc-target');
        var type = target.attr('data-arc-role');
        var originSpan = data.spans[originSpanId];
        var targetSpan = data.spans[targetSpanId];
        arcOptions = {
          action: 'createArc',
          origin: originSpanId,
          target: targetSpanId,
          old_target: targetSpanId,
          type: type,
          old_type: type,
          collection: coll,
          'document': doc
        };
        var eventDescId = target.attr('data-arc-ed');
        if (eventDescId) {
          var eventDesc = data.eventDescs[eventDescId];
          if (eventDesc.equiv) {
            arcOptions['left'] = eventDesc.leftSpans.join(',');
            arcOptions['right'] = eventDesc.rightSpans.join(',');
          }
        }
        $('#arc_origin').text(Util.spanDisplayForm(spanTypes, originSpan.type) + ' ("' + originSpan.text + '")');
        $('#arc_target').text(Util.spanDisplayForm(spanTypes, targetSpan.type) + ' ("' + targetSpan.text + '")');
        var arcId = eventDescId || [originSpanId, type, targetSpanId];
        // WEBANNO EXTENSION BEGIN
        fillArcTypesAndDisplayForm(evt, originSpanId, originSpan.type, targetSpanId, targetSpan.type, type, arcId);
        // WEBANNO EXTENSION END
        // for precise timing, log dialog display to user.
        dispatcher.post('logAction', ['arcEditSelected']);

        // if not an arc, then do we edit a span?
      } else if (id = target.attr('data-span-id')) {
        clearSelection();
        editedSpan = data.spans[id];
        editedFragment = target.attr('data-fragment-id');
        var offsets = [];
        $.each(editedSpan.fragments, function (fragmentNo, fragment) {
          offsets.push([fragment.from, fragment.to]);
        });
        spanOptions = {
          action: 'createSpan',
          offsets: offsets,
          type: editedSpan.type,
          id: id,
        };
        // WEBANNO EXTENSION BEGIN
        fillSpanTypesAndDisplayForm(evt, offsets, editedSpan.text, editedSpan, id);
        // WEBANNO EXTENSION END

        // for precise timing, log annotation display to user.
        dispatcher.post('logAction', ['spanEditSelected']);
      }
    };

    var startArcDrag = function (originId) {
      clearSelection();

      if (!data.spans[originId]) {
        return;
      }

      svgElement.addClass('unselectable');
      svgPosition = svgElement.offset();
      arcDragOrigin = originId;
      arcDragArc = svg.path(svg.createPath(), {
        markerEnd: 'url(#drag_arrow)',
        'class': 'drag_stroke',
        fill: 'none',
      });
      arcDragOriginGroup = $(data.spans[arcDragOrigin].group);
      arcDragOriginGroup.addClass('highlight');
      arcDragOriginBox = Util.realBBox(data.spans[arcDragOrigin].headFragment);
      arcDragOriginBox.center = arcDragOriginBox.x + arcDragOriginBox.width / 2;

      arcDragJustStarted = true;
    };

    var getValidArcTypesForDrag = function (targetId, targetType) {
      var arcType = stripNumericSuffix(arcOptions && arcOptions.type);
      if (!arcDragOrigin || targetId == arcDragOrigin) return null;

      var originType = data.spans[arcDragOrigin].type;
      var spanType = spanTypes[originType];
      var result = [];
      if (spanType && spanType.arcs) {
        $.each(spanType.arcs, function (arcNo, arc) {
          if (arcType && arcType != arc.type) return;

          if ($.inArray(targetType, arc.targets) != -1) {
            result.push(arc.type);
          }
        });
      }
      return result;
    };

    var onMouseDown = function (evt) {
      // Instead of calling startArcDrag() immediately, we defer this to onMouseMove
      if (!that.user || arcDragOrigin) return;

      // is it arc drag start?
      if ($(evt.target).attr('data-span-id')) {
        dragStartedAt = evt; // XXX do we really need the whole evt?
        return false;
      }
    };

    var onMouseMove = function (evt) {
      // BEGIN WEBANNO EXTENSION - #1610 - Improve brat visualization interaction performance
      if (!arcDragOrigin && dragStartedAt) {
        // When the user has pressed the mouse button, we monitor the mouse cursor. If the cursor
        // moves more than a certain distance, we start the arc-drag operation. Starting this
        // operation is expensive because figuring out where the arc is to be drawn is requires
        // fetching bounding boxes - and this triggers a blocking/expensive reflow operation in
        // the browser.
        var deltaX = Math.abs(dragStartedAt.pageX - evt.pageX);
        var deltaY = Math.abs(dragStartedAt.pageY - evt.pageY);
        if (deltaX > 5 || deltaY > 5) {
          arcOptions = null;
          var target = $(dragStartedAt.target);
          var id = target.attr('data-span-id');
          startArcDrag(id);

          // BEGIN WEBANNO EXTENSION - #724 - Cross-row selection is jumpy
          // If user starts selecting text, suppress all pointer events on annotations to
          // avoid the selection jumping around. During selection, we don't need the annotations
          // to react on mouse events anyway.
          if (target.attr('data-chunk-id')) {
            $(svgElement).children('.row, .sentnum').each(function (index, row) {
              $(row).css('pointer-events', 'none');
            });
          }
          // END WEBANNO EXTENSION - #724 - Cross-row selection is jumpy
        }
      }
      // END WEBANNO EXTENSION - #1610 - Improve brat visualization interaction performance

      if (arcDragOrigin) {
        if (arcDragJustStarted) {
          // show the possible targets
          var span = data.spans[arcDragOrigin] || {};
          var spanDesc = spanTypes[span.type] || {};

          // separate out possible numeric suffix from type for highlight
          // (instead of e.g. "Theme3", need to look for "Theme")
          var noNumArcType = stripNumericSuffix(arcOptions && arcOptions.type);
          // var targetClasses = [];
          var $targets = $();
          $.each(spanDesc.arcs || [], function (possibleArcNo, possibleArc) {
            if ((arcOptions && possibleArc.type == noNumArcType) || !(arcOptions && arcOptions.old_target)) {
              $.each(possibleArc.targets || [], function (possibleTargetNo, possibleTarget) {
                // speedup for #642: relevant browsers should support
                // this function: http://www.quirksmode.org/dom/w3c_core.html#t11
                // so we get off jQuery and get down to the metal:
                // targetClasses.push('.span_' + possibleTarget);
                $targets = $targets.add(svgElement[0].getElementsByClassName('span_' + possibleTarget));
              });
            }
          });
          // $(targetClasses.join(',')).not('[data-span-id="' + arcDragOrigin + '"]').addClass('reselectTarget');
          // WEBANNO EXTENSION BEGIN - #277 - self-referencing arcs for custom layers
          if (evt.shiftKey) {
            $targets.addClass('reselectTarget');
          }
          else {
            $targets.not('[data-span-id="' + arcDragOrigin + '"]').addClass('reselectTarget');
          }
          // WEBANNO EXTENSION END - #277 - self-referencing arcs for custom layers 
        }
        clearSelection();
        var mx = evt.pageX - svgPosition.left;
        var my = evt.pageY - svgPosition.top + 5; // TODO FIXME why +5?!?
        var y = Math.min(arcDragOriginBox.y, my) - draggedArcHeight;
        var dx = (arcDragOriginBox.center - mx) / 4;
        var path = svg.createPath().
          move(arcDragOriginBox.center, arcDragOriginBox.y).
          curveC(arcDragOriginBox.center - dx, y,
            mx + dx, y,
            mx, my);
        arcDragArc.setAttribute('d', path.path());
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
          var svgOffset = $(svg._svg).offset();
          var flip = false;
          var tries = 0;
          // First try and match the start offset with a position, if not try it against the other end
          while (tries < 2) {
            var sp = svg._svg.createSVGPoint();
            sp.x = (flip ? evt.pageX : dragStartedAt.pageX) - svgOffset.left;
            sp.y = (flip ? evt.pageY : dragStartedAt.pageY) - (svgOffset.top + 8);
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
          sp.x = (flip ? dragStartedAt.pageX : evt.pageX) - svgOffset.left;
          sp.y = (flip ? dragStartedAt.pageY : evt.pageY) - (svgOffset.top + 8);
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
          startRec = startsAt.getExtentOfChar(anchorOffset);
          startRec.y += 2;
          endRec = endsAt.getExtentOfChar(focusOffset);
          endRec.y += 2;

          // If nothing has changed then stop here
          if (lastStartRec != null && lastStartRec.x == startRec.x && lastStartRec.y == startRec.y && lastEndRec != null && lastEndRec.x == endRec.x && lastEndRec.y == endRec.y) {
            return;
          }

          if (selRect == null) {
            var rx = startRec.x;
            var ry = startRec.y;
            var rw = (endRec.x + endRec.width) - startRec.x;
            if (rw < 0) {
              rx += rw;
              rw = -rw;
            }
            var rh = Math.max(startRec.height, endRec.height);

            selRect = new Array();
            var activeSelRect = makeSelRect(rx, ry, rw, rh);
            selRect.push(activeSelRect);
            startsAt.parentNode.parentNode.parentNode.insertBefore(activeSelRect, startsAt.parentNode.parentNode);
          } else {
            if (startRec.x != lastStartRec.x && endRec.x != lastEndRec.x && (startRec.y != lastStartRec.y || endRec.y != lastEndRec.y)) {
              if (startRec.y < lastStartRec.y) {
                selRect[0].setAttributeNS(null, "width", lastStartRec.width);
                lastEndRec = lastStartRec;
              } else if (endRec.y > lastEndRec.y) {
                selRect[selRect.length - 1].setAttributeNS(null, "x",
                  parseFloat(selRect[selRect.length - 1].getAttributeNS(null, "x"))
                  + parseFloat(selRect[selRect.length - 1].getAttributeNS(null, "width"))
                  - lastEndRec.width);
                selRect[selRect.length - 1].setAttributeNS(null, "width", 0);
                lastStartRec = lastEndRec;
              }
            }

            // Start has moved
            var flip = !(startRec.x == lastStartRec.x && startRec.y == lastStartRec.y);
            // If the height of the start or end changed we need to check whether
            // to remove multi line highlights no longer needed if the user went back towards their start line
            // and whether to create new ones if we moved to a newline
            if (((endRec.y != lastEndRec.y)) || ((startRec.y != lastStartRec.y))) {
              // First check if we have to remove the first highlights because we are moving towards the end on a different line
              var ss = 0;
              for (; ss != selRect.length; ss++) {
                if (startRec.y <= parseFloat(selRect[ss].getAttributeNS(null, "y"))) {
                  break;
                }
              }
              // Next check for any end highlights if we are moving towards the start on a different line
              var es = selRect.length - 1;
              for (; es != -1; es--) {
                if (endRec.y >= parseFloat(selRect[es].getAttributeNS(null, "y"))) {
                  break;
                }
              }
              // TODO put this in loops above, for efficiency the array slicing could be done separate still in single call
              var trunc = false;
              if (ss < selRect.length) {
                for (var s2 = 0; s2 != ss; s2++) {
                  selRect[s2].parentNode.removeChild(selRect[s2]);
                  es--;
                  trunc = true;
                }
                selRect = selRect.slice(ss);
              }
              if (es > -1) {
                for (var s2 = selRect.length - 1; s2 != es; s2--) {
                  selRect[s2].parentNode.removeChild(selRect[s2]);
                  trunc = true;
                }
                selRect = selRect.slice(0, es + 1);
              }

              // If we have truncated the highlights we need to readjust the last one
              if (trunc) {
                var activeSelRect = flip ? selRect[0] : selRect[selRect.length - 1];
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
                  activeSelRect.setAttributeNS(null, "width", rw);
                } else {
                  var rw = (endRec.x + endRec.width) - parseFloat(activeSelRect.getAttributeNS(null, "x"));
                  activeSelRect.setAttributeNS(null, "width", rw);
                }
              } else {
                // We didnt truncate anything but we have moved to a new line so we need to create a new highlight
                var lastSel = flip ? selRect[0] : selRect[selRect.length - 1];
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
                  rw = $(svg._svg).width() - startRec.x;
                  rh = startRec.height;
                } else {
                  rx = endBox.x;
                  ry = endRec.y;
                  rw = (endRec.x + endRec.width) - endBox.x;
                  rh = endRec.height;
                }
                var newRect = makeSelRect(rx, ry, rw, rh);
                if (flip) {
                  selRect.unshift(newRect);
                } else {
                  selRect.push(newRect);
                }

                // Place new highlight in appropriate slot in SVG graph
                startsAt.parentNode.parentNode.parentNode.insertBefore(newRect, startsAt.parentNode.parentNode);
              }
            } else {
              // The user simply moved left or right along the same line so just adjust the current highlight
              var activeSelRect = flip ? selRect[0] : selRect[selRect.length - 1];
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
          lastStartRec = startRec;
          lastEndRec = endRec;
        }
      }
      arcDragJustStarted = false;
    };

    var adjustToCursor = function (evt, element, centerX, centerY) {
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

    var updateCheckbox = function ($input) {
      var $widget = $input.button('widget');
      var $textspan = $widget.find('.ui-button-text');
      $textspan.html(($input[0].checked ? '&#x2611; ' : '&#x2610; ') + $widget.attr('data-bare'));
    };

    var fillSpanTypesAndDisplayForm = function (evt, offsets, spanText, span, id) {

      if (id) {
        dispatcher.post('ajax', [{
          action: 'spanOpenDialog',
          offsets: $.toJSON(offsets),
          id: id,
          type: span.type,
          spanText: spanText
        }, 'serverResult']);
      }
      else {
        dispatcher.post('ajax', [{
          action: 'spanOpenDialog',
          offsets: $.toJSON(offsets),
          spanText: spanText
        }, 'serverResult']);
      }
    };
    // WEBANNO EXTENSION END

    var submitReselect = function () {
      $(reselectedSpan.rect).removeClass('reselect');
      reselectedSpan = null;
      spanForm.submit();
    };

    var clearSpanNotes = function (evt) {
      $('#span_notes').val('');
    }
    $('#clear_notes_button').button();
    $('#clear_notes_button').click(clearSpanNotes);

    var clearSpanNorm = function (evt) {
      clearNormalizationUI();
    }
    $('#clear_norm_button').button();
    $('#clear_norm_button').click(clearSpanNorm);

    // invoked on response to ajax request for id lookup
    var setSpanNormText = function (response) {
      if (response.exception) {
        // TODO: better response to failure
        dispatcher.post('messages', [[['Lookup error', 'warning', -1]]]);
        return false;
      }
      // set input style according to whether we have a valid value
      var $idinput = $('#span_norm_id');
      // TODO: make sure the key echo in the response matches the
      // current value of the $idinput
      $idinput.removeClass('valid_value').removeClass('invalid_value');
      if (response.value === null) {
        $idinput.addClass('invalid_value');
        hideNormalizationRefLink();
      } else {
        $idinput.addClass('valid_value');
        updateNormalizationRefLink();
      }
      $('#span_norm_txt').val(response.value);
    }

    // on any change to the normalization DB, clear everything and
    // update link
    var spanNormDbUpdate = function (evt) {
      clearNormalizationUI();
      updateNormalizationDbLink();
    }
    $('#span_norm_db').change(spanNormDbUpdate);

    // on any change to the normalization ID, update the text of the
    // reference
    var spanNormIdUpdate = function (evt) {
      var key = $(this).val();
      var db = $('#span_norm_db').val();
      if (key != oldSpanNormIdValue) {
        if (key.match(/^\s*$/)) {
          // don't query empties, just clear instead
          clearNormalizationUI();
        } else {
          dispatcher.post('ajax', [{
            action: 'normGetName',
            database: db,
            key: key,
            collection: coll
          }, 'normGetNameResult']);
        }
        oldSpanNormIdValue = key;
      }
    }

    // see http://stackoverflow.com/questions/1948332/detect-all-changes-to-a-input-type-text-immediately-using-jquery
    $('#span_norm_id').bind('propertychange keyup input paste', spanNormIdUpdate);
    // nice-looking select for normalization
    $('#span_norm_db').addClass('ui-widget ui-state-default ui-button-text');

    // We send a request to the backend to open the dialog
    var fillArcTypesAndDisplayForm = function (evt, originSpanId, originType, targetSpanId, targetType, arcType, arcId) {

      if (arcId) {
        dispatcher.post('ajax', [{
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
        dispatcher.post('ajax', [{
          action: 'arcOpenDialog',
          originSpanId: originSpanId,
          originType: originType,
          targetSpanId: targetSpanId,
          targetType: targetType
        }, 'serverResult']);

      }
    };

    var stopArcDrag = function (target) {
      // BEGIN WEBANNO EXTENSION - #1610 - Improve brat visualization interaction performance
      // Clear the dragStartAt saved event
      dragStartedAt = null;
      // END WEBANNO EXTENSION - #1610 - Improve brat visualization interaction performance
      if (arcDragOrigin) {
        if (!target) {
          target = $('.badTarget');
        }
        target.removeClass('badTarget');
        arcDragOriginGroup.removeClass('highlight');
        if (target) {
          target.parent().removeClass('highlight');
        }
        if (arcDragArc) {
          try {
            svg.remove(arcDragArc);
          }
          catch (err) {
            // Ignore - could be spurious TypeError: null is not an object (evaluating 'a.parentNode.removeChild')
          }
          arcDrag = null;
        }
        arcDragOrigin = null;
        if (arcOptions) {
          $('g[data-from="' + arcOptions.origin + '"][data-to="' + arcOptions.target + '"]').removeClass('reselect');
        }
        svgElement.removeClass('reselect');
      }
      svgElement.removeClass('unselectable');
      $('.reselectTarget').removeClass('reselectTarget');
    };

    var onMouseUp = function (evt) {
      if (that.user === null) return;

      // BEGIN WEBANNO EXTENSION - #724 - Cross-row selection is jumpy
      // Restore pointer events on annotations
      if (dragStartedAt && $(dragStartedAt.target).attr('data-chunk-id')) {
        $(svgElement).children('.row, .sentnum').each(function (index, row) {
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
        clearSelection();
        stopArcDrag(target);
        return;
      }

      // is it arc drag end?
      if (arcDragOrigin) {
        var origin = arcDragOrigin;
        var targetValid = target.hasClass('reselectTarget');
        stopArcDrag(target);
        // WEBANNO EXTENSION BEGIN - #277 - self-referencing arcs for custom layers 
        if ((id = target.attr('data-span-id')) && targetValid && (evt.shiftKey || origin != id)) {
          //          if ((id = target.attr('data-span-id')) && origin != id && targetValid) {
          // WEBANNO EXTENSION END - #277 - self-referencing arcs for custom layers 
          var originSpan = data.spans[origin];
          var targetSpan = data.spans[id];
          if (arcOptions && arcOptions.old_target) {
            arcOptions.target = targetSpan.id;
            dispatcher.post('ajax', [arcOptions, 'edited']);
          } else {
            arcOptions = {
              action: 'createArc',
              origin: originSpan.id,
              target: targetSpan.id,
              collection: coll,
              'document': doc
            };
            $('#arc_origin').text(Util.spanDisplayForm(spanTypes, originSpan.type) + ' ("' + originSpan.text + '")');
            $('#arc_target').text(Util.spanDisplayForm(spanTypes, targetSpan.type) + ' ("' + targetSpan.text + '")');
            fillArcTypesAndDisplayForm(evt, originSpan.id, originSpan.type, targetSpan.id, targetSpan.type);
            // for precise timing, log dialog display to user.
            dispatcher.post('logAction', ['arcSelected']);
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
          clearSelection();
          stopArcDrag(target);
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
            clearSelection();
            stopArcDrag(target);
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
          var chunkFrom = data.chunks[chunkIndexFrom];
          var chunkTo = data.chunks[chunkIndexTo];
          var selectedFrom = chunkFrom.from + anchorOffset;
          var selectedTo = chunkTo.from + focusOffset;
          sel.removeAllRanges();

          if (selectedFrom > selectedTo) {
            var tmp = selectedFrom; selectedFrom = selectedTo; selectedTo = tmp;
          }
          // trim
          while (selectedFrom < selectedTo && " \n\t".indexOf(data.text.substr(selectedFrom, 1)) !== -1) selectedFrom++;
          while (selectedFrom < selectedTo && " \n\t".indexOf(data.text.substr(selectedTo - 1, 1)) !== -1) selectedTo--;

          // shift+click allows zero-width spans
          if (selectedFrom === selectedTo && !evt.shiftKey) {
            // simple click (zero-width span)
            return;
          }

          var newOffset = [selectedFrom, selectedTo];
          if (reselectedSpan) {
            var newOffsets = reselectedSpan.offsets.slice(0); // clone
            spanOptions.old_offsets = JSON.stringify(reselectedSpan.offsets);
            if (selectedFragment !== null) {
              if (selectedFragment !== false) {
                newOffsets.splice(selectedFragment, 1);
              }
              newOffsets.push(newOffset);
              newOffsets.sort(Util.cmpArrayOnFirstElement);
              spanOptions.offsets = newOffsets;
            } else {
              spanOptions.offsets = [newOffset];
            }
          } else {
            spanOptions = {
              action: 'createSpan',
              offsets: [newOffset]
            }
          }

          if (!Configuration.rapidModeOn || reselectedSpan != null) {
            // normal span select in standard annotation mode
            // or reselect: show selector
            var spanText = data.text.substring(selectedFrom, selectedTo);
            // WEBANNO EXTENSION BEGIN
            fillSpanTypesAndDisplayForm(evt, spanOptions.offsets, spanText, reselectedSpan);
            // WEBANNO EXTENSION END
            // for precise timing, log annotation display to user.
            dispatcher.post('logAction', ['spanSelected']);
          } else {
            // normal span select in rapid annotation mode: call
            // server for span type candidates
            var spanText = data.text.substring(selectedFrom, selectedTo);
            // TODO: we're currently storing the event to position the
            // span form using adjustToCursor() (which takes an event),
            // but this is clumsy and suboptimal (user may have scrolled
            // during the ajax invocation); think of a better way.
            lastRapidAnnotationEvent = evt;
            dispatcher.post('ajax', [{
              action: 'suggestSpanTypes',
              collection: coll,
              'document': doc,
              start: selectedFrom,
              end: selectedTo,
              text: spanText,
              model: $('#rapid_model').val(),
            }, 'suggestedSpanTypes']);
          }
        }
      }
    };

    var toggleCollapsible = function ($el, state) {
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

    var collapseHandler = function (evt) {
      toggleCollapsible($(evt.target));
    }

    var rememberData = function (_data) {
      if (_data && !_data.exception) {
        data = _data;
      }
    };

    var addSpanTypesToDivInner = function ($parent, types, category) {
      if (!types) return;

      $.each(types, function (typeNo, type) {
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
          var spanBgColor = spanTypes[type.type] && spanTypes[type.type].bgColor || '#ffffff';
          spanBgColor = Util.adjustColorLightness(spanBgColor, spanBoxTextBgColorLighten);
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
          addSpanTypesToDivInner($collapsible, type.children, category);
          $parent.append($div);
          if (type.hotkey) {
            spanKeymap[type.hotkey] = 'span_' + type.type;
            var name = $label.html();
            var replace = true;
            name = name.replace(new RegExp("(&[^;]*?)?(" + type.hotkey + ")", 'gi'),
              function (all, entity, letter) {
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
    var addSpanTypesToDiv = function ($top, types, heading) {
      $scroller = $('<div class="scroller"/>');
      $legend = $('<legend/>').text(heading);
      $fieldset = $('<fieldset/>').append($legend).append($scroller);
      $top.append($fieldset);
      addSpanTypesToDivInner($scroller, types);
    };
    var addAttributeTypesToDiv = function ($top, types, category) {
      $.each(types, function (attrNo, attr) {
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
          $input.change(onBooleanAttrChange);
        } else {
          var $div = $('<div class="ui-button ui-button-text-only attribute_type_label"/>');
          var $select = $('<select id="' + attrId + '" class="ui-widget ui-state-default ui-button-text" category="' + category + '"/>');
          var $option = $('<option class="ui-state-default" value=""/>').text(attr.name + ': ?');
          $select.append($option);
          $.each(attr.values, function (valType, value) {
            $option = $('<option class="ui-state-active" value="' + Util.escapeQuotes(valType) + '"/>').text(attr.name + ': ' + (value.name || valType));
            $select.append($option);
          });
          $div.append($select);
          $top.append($div);
          $select.change(onMultiAttrChange);
        }
      });
    }

    var setSpanTypeSelectability = function (category) {
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

    var onMultiAttrChange = function (evt) {
      if ($(this).val() == '') {
        $('#span_form input:not([unused])').removeAttr('disabled');
      } else {
        var attrCategory = evt.target.getAttribute('category');
        setSpanTypeSelectability(attrCategory);
        if (evt.target.selectedIndex) {
          $(evt.target).addClass('ui-state-active');
        } else {
          $(evt.target).removeClass('ui-state-active');
        }
      }
    }

    var onBooleanAttrChange = function (evt) {
      var attrCategory = evt.target.getAttribute('category');
      setSpanTypeSelectability(attrCategory);
      updateCheckbox($(evt.target));
    };

    var rememberSpanSettings = function (response) {
      spanKeymap = {};

      // TODO: check for exceptions in response

      // fill in entity and event types
      var $entityScroller = $('#entity_types div.scroller').empty();
      addSpanTypesToDivInner($entityScroller, response.entity_types, 'entity');
      var $eventScroller = $('#event_types div.scroller').empty();
      addSpanTypesToDivInner($eventScroller, response.event_types, 'event');

      // fill in attributes
      var $entattrs = $('#entity_attributes div.scroller').empty();
      addAttributeTypesToDiv($entattrs, entityAttributeTypes, 'entity');

      var $eveattrs = $('#event_attributes div.scroller').empty();
      addAttributeTypesToDiv($eveattrs, eventAttributeTypes, 'event');
    };

    var tagCurrentDocument = function (taggerId) {
      var tagOptions = {
        action: 'tag',
        collection: coll,
        'document': doc,
        tagger: taggerId,
      };
      dispatcher.post('ajax', [tagOptions, 'edited']);
    }

    var setupTaggerUI = function (response) {
      var taggers = response.ner_taggers || [];
      $taggerButtons = $('#tagger_buttons').empty();
      $.each(taggers, function (taggerNo, tagger) {
        // expect a tuple with ID, name, model, and URL
        var taggerId = tagger[0];
        var taggerName = tagger[1];
        var taggerModel = tagger[2];
        if (!taggerId || !taggerName || !taggerModel) {
          dispatcher.post('messages', [[['Invalid tagger specification received from server', 'error']]]);
          return true; // continue
        }
        var $row = $('<div class="optionRow"/>');
        var $label = $('<span class="optionLabel">' + Util.escapeHTML(taggerName) + '</span>');
        var $button = $('<input id="tag_' + Util.escapeHTML(taggerId) + '_button" type="button" value="' + Util.escapeHTML(taggerModel) + '" tabindex="-1" title="Automatically tag the current document."/>');
        $row.append($label).append($button);
        $taggerButtons.append($row);
        $button.click(function (evt) {
          tagCurrentDocument(taggerId);
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
    var rememberNormDbsForType = function (types) {
      if (!types) return;

      $.each(types, function (typeNo, type) {
        if (type === null) {
          // spacer, no-op
        } else {
          normDbsByType[type.type] = type.normalizations || [];
          // WEBANNO EXTENSION BEGIN
          // Avoid exception when no children are set
          if (type.children && type.children.length) {
            // WEBANNO EXTENSION END
            rememberNormDbsForType(type.children);
          }
        }
      });
    };

    var setupNormalizationUI = function (response) {
      var norm_resources = response.normalization_config || [];
      var $norm_select = $('#span_norm_db');
      // clear possible existing
      $norm_select.empty();
      // fill in new
      html = [];
      $.each(norm_resources, function (normNo, norm) {
        var normName = norm[0], normUrl = norm[1], normUrlBase = norm[2];
        var serverDb = norm[3];
        html.push('<option value="' + Util.escapeHTML(normName) + '">' +
          Util.escapeHTML(normName) + '</option>');
        // remember the urls for updates
        normDbUrlByDbName[normName] = normUrl;
        normDbUrlBaseByDbName[normName] = normUrlBase;
      });
      // remember per-type appropriate DBs
      normDbsByType = {};
      rememberNormDbsForType(response.entity_types);
      rememberNormDbsForType(response.event_types);
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
    var hideNormalizationRefLink = function () {
      $('#span_norm_ref_link').hide();
    }

    // updates the reference link in the normalization UI according
    // to the current value of the normalization DB and ID.
    var updateNormalizationRefLink = function () {
      var $normId = $('#span_norm_id');
      var $normLink = $('#span_norm_ref_link');
      var normId = $normId.val();
      var $normDb = $('#span_norm_db');
      var normDb = $normDb.val();
      if (!normId || !normDb || normId.match(/^\s*$/)) {
        $normLink.hide();
      } else {
        var base = normDbUrlBaseByDbName[normDb];
        // assume hidden unless everything goes through
        $normLink.hide();
        if (!base) {
          // base URL is now optional, just skip link generation if not set
          ;
        } else if (base.indexOf('%s') == -1) {
          dispatcher.post('messages', [[['Base URL "' + base + '" for ' + normDb + ' does not contain "%s"', 'error']]]);
        } else {
          // TODO: protect against strange chars in ID
          link = base.replace('%s', normId);
          $normLink.attr('href', link);
          $normLink.show();
        }
      }
    }

    // updates the DB search link in the normalization UI according
    // to the current value of the normalization DB.
    var updateNormalizationDbLink = function () {
      var $dbLink = $('#span_norm_db_link');
      var $normDb = $('#span_norm_db');
      var normDb = $normDb.val();
      if (!normDb) return; // no normalisation configured
      var link = normDbUrlByDbName[normDb];
      if (!link || link.match(/^\s*$/)) {
        dispatcher.post('messages', [[['No URL for ' + normDb, 'error']]]);
        $dbLink.hide();
      } else {
        // TODO: protect against weirdness in DB link
        $dbLink.attr('href', link);
        $dbLink.show();
      }
    }

    // resets user-settable normalization-related UI elements to a
    // blank state (does not blank #span_norm_db <select>).
    var clearNormalizationUI = function () {
      var $normId = $('#span_norm_id');
      var $normText = $('#span_norm_txt');
      $normId.val('');
      oldSpanNormIdValue = '';
      $normId.removeClass('valid_value').removeClass('invalid_value');
      $normText.val('');
      updateNormalizationRefLink();
    }

    // returns the normalizations currently filled in the span
    // dialog, or empty list if there are none
    var spanNormalizations = function () {
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
    var spanAttributes = function (typeRadio) {
      typeRadio = typeRadio || $('#span_form input:radio:checked');
      var attributes = {};
      var attributeTypes;
      var category = typeRadio.attr('category');
      if (category == 'entity') {
        attributeTypes = entityAttributeTypes;
      } else if (category == 'event') {
        attributeTypes = eventAttributeTypes;
      } else {
        console.error('Unrecognized type category:', category);
      }
      $.each(attributeTypes, function (attrNo, attr) {
        var $input = $('#' + category + '_attr_' + Util.escapeQuotes(attr.type));
        if (attr.bool) {
          attributes[attr.type] = $input[0].checked;
        } else if ($input[0].selectedIndex) {
          attributes[attr.type] = $input.val();
        }
      });
      return attributes;
    }

    var spanAndAttributeTypesLoaded = function (_spanTypes,
      _entityAttributeTypes,
      _eventAttributeTypes,
      _relationTypesHash) {
      spanTypes = _spanTypes;
      entityAttributeTypes = _entityAttributeTypes;
      eventAttributeTypes = _eventAttributeTypes;
      relationTypesHash = _relationTypesHash;
      // for easier access
      allAttributeTypes = $.extend({},
        entityAttributeTypes,
        eventAttributeTypes);
    };

    var gotCurrent = function (_coll, _doc, _args) {
      coll = _coll;
      doc = _doc;
      args = _args;
    };

    var undoStack = [];
    var edited = function (response) {
      var x = response.exception;
      if (x) {
        if (x == 'annotationIsReadOnly') {
          dispatcher.post('messages', [[["This document is read-only and can't be edited.", 'error']]]);
        } else if (x == 'spanOffsetOverlapError') {
          // createSpan with overlapping frag offsets; reset offsets
          // @amadanmath: who holds the list of offsets for a span?
          // how to reset this?
        } else {
          dispatcher.post('messages', [[['Unknown error ' + x, 'error']]]);
        }
        if (reselectedSpan) {
          $(reselectedSpan.rect).removeClass('reselect');
          reselectedSpan = null;
        }
        svgElement.removeClass('reselect');
        $('#waiter').dialog('close');
      } else {
        if (response.edited == undefined) {
          console.warn('Warning: server response to edit has', response.edited, 'value for "edited"');
        } else {
          args.edited = response.edited;
        }
        var sourceData = response.annotations;
        sourceData.document = doc;
        sourceData.collection = coll;
        // this "prevent" is to protect against reloading (from the
        // server) the very data that we just received as part of the
        // response to the edit.
        if (response.undo != undefined) {
          undoStack.push([coll, sourceData.document, response.undo]);
        }
        dispatcher.post('preventReloadByURL');
        dispatcher.post('setArguments', [args]);
        dispatcher.post('renderData', [sourceData]);
      }
    };


    // TODO: why are these globals defined here instead of at the top?
    var spanForm = $('#span_form');
    var rapidSpanForm = $('#rapid_span_form');

    var preventDefault = function (evt) {
      evt.preventDefault();
    }

    // WEBANNO EXTENSION BEGIN - #1388 Support context menu
    var contextMenu = function (evt) {
      // If the user shift-right-clicks, open the normal browser context menu. This is useful
      // e.g. during debugging / developing
      if (evt.shiftKey) {
        return;
      }

      stopArcDrag();

      var target = $(evt.target);
      var id;
      if (id = target.attr('data-span-id')) {
        preventDefault(evt);
        var offsets = [];
        $.each(data.spans[id], function (fragmentNo, fragment) {
          offsets.push([fragment.from, fragment.to]);
        });
        dispatcher.post('ajax', [{
          action: 'contextMenu',
          offsets: $.toJSON(offsets),
          id: id,
          type: data.spans[id].type,
          clientX: evt.clientX,
          clientY: evt.clientY
        }, 'serverResult']);
      }
    }
    // WEBANNO EXTENSION END - #1388 Support context menu

    var $waiter = $('#waiter');
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

    var isReloadOkay = function () {
      // do not reload while the user is in the middle of editing
      return arcDragOrigin == null && reselectedSpan == null;
    };

    var userReceived = function (_user) {
      that.user = _user;
    }

    var setAnnotationSpeed = function (speed) {
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
      dispatcher.post('configurationChanged');
    };

    var onNewSourceData = function (_sourceData) {
      sourceData = _sourceData;
    }

    var init = function () {
      dispatcher.post('annotationIsAvailable');
    };

    dispatcher.
      on('init', init).
      on('getValidArcTypesForDrag', getValidArcTypesForDrag).
      on('dataReady', rememberData).
      on('collectionLoaded', rememberSpanSettings).
      on('collectionLoaded', setupTaggerUI).
      on('collectionLoaded', setupNormalizationUI).
      on('spanAndAttributeTypesLoaded', spanAndAttributeTypesLoaded).
      on('newSourceData', onNewSourceData).
      on('user', userReceived).
      on('edited', edited).
      on('current', gotCurrent).
      on('isReloadOkay', isReloadOkay).
      on('keydown', onKeyDown).
      on('click', onClick).
      on('dragstart', preventDefault).
      on('mousedown', onMouseDown).
      on('mouseup', onMouseUp).
      on('mousemove', onMouseMove).
      on('annotationSpeed', setAnnotationSpeed).
      on('contextmenu', contextMenu);
  };

  return AnnotatorUI;
})(jQuery, window);

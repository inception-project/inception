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
import type { Dispatcher } from "../dispatcher/Dispatcher";

import type { Util as UtilType } from "../util/Util";
declare var Util: UtilType;

import type { Configuration as ConfigurationType } from "../configuration/Configuration";
declare var Configuration: ConfigurationType;

export class VisualizerUI {
  defaultFloatFormat = '%.1f/right';

  documentListing = null; // always documents of current collection
  selectorData = null;    // can be search results when available
  searchActive = false;   // whether search results received and in use
  loadedSearchData = null;

  currentForm;
  spanTypes = null;
  relationTypesHash = null;
  // TODO: confirm unnecessary and remove
  //       var attributeTypes = null;
  data = null;
  mtime = null;
  searchConfig = null;
  coll;
  doc;
  args;
  collScroll;
  docScroll;
  user = null;
  annotationAvailable = false;
  currentDocumentSVGsaved = false;
  fileBrowserClosedWithSubmit = false;

  // normalization: server-side DB by norm DB name
  normServerDbByNormDbName = {};

  matchFocus = '';
  matches = '';

  dispatcher: Dispatcher;
  svgElement: JQuery;
  svgId: string;

  constructor(dispatcher: Dispatcher, svg) {
    this.commentPopup = $('#commentpopup');
    this.svgElement = $(svg._svg);
    this.svgId = this.svgElement.parent().attr('id');
    this.dispatcher = dispatcher;
    this.dispatcher.
      on('init', this, this.init).
      on('dataReady', this, this.rememberData).
      on('annotationIsAvailable', this, this.annotationIsAvailable).
      on('displaySpanComment', this, this.displaySpanComment).
      on('displayArcComment', this, this.displayArcComment).
      on('displaySentComment', this, this.displaySentComment).
      on('hideComment', this, this.hideComment).
      on('resize', this, this.onResize).
      on('collectionLoaded', this, this.rememberNormDb).
      on('spanAndAttributeTypesLoaded', this, this.spanAndAttributeTypesLoaded).
      on('isReloadOkay', this, this.isReloadOkay).
      on('doneRendering', this, this.onDoneRendering).
      on('startedRendering', this, this.onStartedRendering).
      on('mousemove', this, this.onMouseMove).
      on('displaySpanButtons', this, this.displaySpanButtons).
      on('acceptButtonClicked', this, this.acceptAction).
      on('rejectButtonClicked', this, this.rejectAction).
      on('contextmenu', this, this.contextMenu);
  }

  /* START comment popup - related */

  adjustToCursor(evt, element, offset, top, right) {
    // get the real width, without wrapping
    element.css({ left: 0, top: 0 });
    var screenHeight = $(window).height();
    var screenWidth = $(window).width();
    // FIXME why the hell is this 22 necessary?!?
    var elementHeight = element.height() + 22;
    var elementWidth = element.width() + 22;
    var x, y;
    offset = offset || 0;
    if (top) {
      y = evt.clientY - elementHeight - offset;
      if (y < 0)
        top = false;
    }
    if (!top) {
      y = evt.clientY + offset;
    }
    if (right) {
      x = evt.clientX + offset;
      if (x >= screenWidth - elementWidth)
        right = false;
    }
    if (!right) {
      x = evt.clientX - elementWidth - offset;
    }
    if (y < 0)
      y = 0;
    if (x < 0)
      x = 0;
    element.css({ top: y, left: x });
  }

  commentPopup;
  commentDisplayed = false;

  displayCommentTimer = null;
  displayComment(evt, target, comment, commentText, commentType, immediately?) {
    var idtype;
    if (commentType) {
      // label comment by type, with special case for default note type
      var commentLabel;
      if (commentType == 'AnnotatorNotes') {
        commentLabel = '<b>Note:</b> ';
      } else {
        commentLabel = '<b>' + Util.escapeHTML(commentType) + ':</b> ';
      }
      comment += "<hr/>";
      comment += commentLabel + Util.escapeHTMLwithNewlines(commentText);
      idtype = 'comment_' + commentType;
    }
    this.commentPopup[0].className = idtype;
    this.commentPopup.html(comment);
    this.adjustToCursor(evt, this.commentPopup, 10, true, true);
    clearTimeout(this.displayCommentTimer);
    /* slight "tooltip" delay to allow highlights to be seen
               before the popup obstructs them. */
    this.displayCommentTimer = setTimeout(() => {
      // BEGIN WEBANNO EXTENSION - #1610 - Improve brat visualization interaction performance
      // - Show/hide comments immediately instead of using an animation to avoid costly reflows
      /*
      commentPopup.stop(true, true).fadeIn(0);
      */
      this.commentPopup.show();
      // END WEBANNO EXTENSION - #1610 - Improve brat visualization interaction performance
      this.commentDisplayed = true;
    }, immediately ? 0 : 500);
  };

  // to avoid clobbering on delayed response
  commentPopupNormInfoSeqId = 0;

  normInfoSortFunction(a, b) {
    // images at the top
    if (a[0].toLowerCase() == '<img>') return -1;
    if (b[0].toLowerCase() == '<img>') return 1;
    // otherwise stable
    return Util.cmp(a[2], b[2]);
  }

  displaySpanComment(evt, target, spanId, spanType, mods, spanText, hoverText,
    commentText, commentType, normalizations) {

    var immediately = false;
    var comment = ('<div><span class="comment_type_id_wrapper">' +
      '<span class="comment_type">' + Util.escapeHTML(Util.spanDisplayForm(this.spanTypes, spanType)) + '</span>' + ' ' +
      '<span class="comment_id">' + 'ID:' + Util.escapeHTML(spanId) + '</span></span>');
    if (mods.length) {
      comment += '<div>' + Util.escapeHTML(mods.join(', ')) + '</div>';
    }

    comment += '</div>';

    if (hoverText != null) {
      comment += ('<div class="comment_text">' + Util.escapeHTML(hoverText) + '</div>');
    } else if (spanText) {
      comment += ('<div class="comment_text">"' + Util.escapeHTML(spanText) + '"</div>');
    }

    var validArcTypesForDrag = this.dispatcher.post('getValidArcTypesForDrag', [spanId, spanType]);
    if (validArcTypesForDrag && validArcTypesForDrag[0]) {
      if (validArcTypesForDrag[0].length) {
        comment += '<div>' + validArcTypesForDrag[0].join(', ') + '</div>';
      } else {
        $('rect[data-span-id="' + spanId + '"]').addClass('badTarget');
      }
      immediately = true;
    }

    // process normalizations
    var normsToQuery = [];
    comment += this.processNormalizations(normalizations, normsToQuery);

    // display initial comment HTML 
    this.displayComment(evt, target, comment, commentText, commentType, immediately);

    // initiate AJAX calls for the normalization data to query
    $.each(normsToQuery, (normNo, norm) => this.initiateNormalizationAjaxCall(spanId, spanType, norm));
  }

  displayArcComment(evt, target, symmetric, arcId, originSpanId, originSpanType,
    role, targetSpanId, targetSpanType, commentText, commentType, normalizations) {
    var arcRole = target.attr('data-arc-role');
    // in arrowStr, &#8212 == mdash, &#8594 == Unicode right arrow
    var arrowStr = symmetric ? '&#8212;' : '&#8594;';
    var arcDisplayForm = Util.arcDisplayForm(this.spanTypes, this.data.spans[originSpanId].type, arcRole, this.relationTypesHash);
    var comment = "";
    comment += ('<span class="comment_type_id_wrapper">' +
      '<span class="comment_type">' + Util.escapeHTML(Util.spanDisplayForm(this.spanTypes, originSpanType)) + ' ' + arrowStr + ' ' + Util.escapeHTML(arcDisplayForm) + ' ' + arrowStr + ' ' + Util.escapeHTML(Util.spanDisplayForm(this.spanTypes, targetSpanType)) + '</span>' +
      '<span class="comment_id">' + (arcId ? 'ID:' + arcId : Util.escapeHTML(originSpanId) + arrowStr + Util.escapeHTML(targetSpanId)) + '</span>' +
      '</span>');
    comment += ('<div class="comment_text">' + Util.escapeHTML('"' + this.data.spans[originSpanId].text + '"') + arrowStr + Util.escapeHTML('"' + this.data.spans[targetSpanId].text + '"') + '</div>');

    // process normalizations
    var normsToQuery = [];
    comment += this.processNormalizations(normalizations, normsToQuery);

    this.displayComment(evt, target, comment, commentText, commentType);

    // initiate AJAX calls for the normalization data to query
    $.each(normsToQuery, (normNo, norm) => this.initiateNormalizationAjaxCall(arcId, arcRole, norm));
  };

  processNormalizations(normalizations, normsToQuery) {
    let comment = "";
    $.each(normalizations != null ? normalizations : [], function (normNo, norm) {
      var dbName = norm[0],
        dbKey = norm[1];
      if (norm[2]) {
        var cateogory = norm[0],
          key = norm[1];
        let value = norm[2];
        // no DB, just attach "human-readable" text provided
        // with the annotation, if any
        if (cateogory) {
          comment += `<hr/>
                      <span class="comment_id">${Util.escapeHTML(cateogory)}</span>'
                      `;
        }

        if (key) {
          comment += `<span class="norm_info_label">${Util.escapeHTML(key)}</span>
                     `;
        }

        comment += `<span class="norm_info_value">${Util.escapeHTML(value).replace(/\n/g, "<br/>")}</span>
                    <br/>
                    `;
      } else {
        // DB available, add drop-off point to HTML and store
        // query parameters
        var dbName = norm[0],
          dbKey = norm[1];
        this.commentPopupNormInfoSeqId++;
        if (dbKey) {
          comment += ('<hr/>' + '<span class="comment_id">' + Util.escapeHTML(dbName) + ': ' + Util.escapeHTML(dbKey) + '</span><br/>');
        }
        else {
          comment += '<hr/>';
        }
        comment += ('<div id="norm_info_drop_point_' + this.commentPopupNormInfoSeqId + '"/>');
        normsToQuery.push([dbName, dbKey, this.commentPopupNormInfoSeqId]);
      }
    });
    return comment;
  }

  initiateNormalizationAjaxCall(id, type, normq) {
    // TODO: cache some number of most recent norm_get_data results
    var dbName = normq[0],
      dbKey = normq[1],
      infoSeqId = normq[2];
    this.dispatcher.post('ajax', [{
      action: 'normData',
      database: dbName,
      key: dbKey,
      collection: this.coll,
      id: id,
      type: type,
    }, function (response) {
      if (response.exception) {
        ; // TODO: response to error
      } else if (!response.results) {
        ; // TODO: response to missing key
      } else {
        // extend comment popup with normalization data
        let norminfo = '';
        // flatten outer (name, attr, info) array (idx for sort)
        let infos = [];
        var idx = 0;
        for (var j = 0; j < response.results.length; j++) {
          var label = response.results[j][0];
          var value = response.results[j][1];
          infos.push([label, value, idx++]);
        }

        // sort, prioritizing images (to get floats right)
        infos = infos.sort(this.normInfoSortFunction);
        // generate HTML
        for (var i = 0; i < infos.length; i++) {
          var label = infos[i][0];
          var value = infos[i][1];
          if (label && value) {
            // special treatment for some label values
            if (label.toLowerCase() == '<img>') {
              // image
              norminfo += ('<img class="norm_info_img" src="' + value + '"/>');
            } else {
              // normal, as text
              // max length restriction
              if (value.length > 300) {
                value = value.substr(0, 300) + ' ...';
              }

              norminfo += ('<span class="norm_info_label">' + Util.escapeHTML(label) + '</span>' + '<span class="norm_info_value">' + ': ' + Util.escapeHTML(value).replace(/\n/g, "<br/>") + '</span>' + '<br/>');
            }
          }
        }
        var drop = $('#norm_info_drop_point_' + infoSeqId);
        if (drop) {
          drop.html(norminfo);
        } else {
          console.log('norm info drop point not found!'); //TODO XXX
        }
      }
    }]);
  }

  displaySentComment(
    evt, target, commentText, commentType) {
    this.displayComment(evt, target, '', commentText, commentType);
  };

  hideComment() {
    clearTimeout(this.displayCommentTimer);
    if (this.commentDisplayed) {
      // BEGIN WEBANNO EXTENSION - #1610 - Improve brat visualization interaction performance
      // - Show/hide comments immediately instead of using an animation to avoid costly reflows
      /*
                              commentPopup.stop(true, true).fadeOut(0, function() {
                                  commentDisplayed = false;
                              });
      */
      this.commentPopup.hide();
      this.commentDisplayed = false;
      // END WEBANNO EXTENSION - #1610 - Improve brat visualization interaction performance
    }
    clearTimeout(this.displayButtonsTimer);
  };

  onMouseMove(evt) {
    if (this.commentDisplayed) {
      this.adjustToCursor(evt, this.commentPopup, 10, true, true);
    }
  };

  /* END comment popup - related */

  // BEGIN WEBANNO EXTENSION - #1697 - Explicit UI for accepting/recejcting recommendations
  displayButtonsTimer = null;
  buttonsShown = false;
  acceptAction(evt, offsets, editedSpan, id) {
    // must be logged in
    if (this.user === null) return;

    evt.preventDefault();
    this.dispatcher.post('ajax', [{
      action: 'acceptAction',
      offsets: JSON.stringify(offsets),
      id: id,
      labelText: editedSpan.labelText,
      type: editedSpan.type
    }, 'serverResult']);

  };

  rejectAction(evt, offsets, editedSpan, id) {
    // must be logged in
    if (this.user === null) return;

    evt.preventDefault();
    this.dispatcher.post('ajax', [{
      action: 'rejectAction',
      offsets: JSON.stringify(offsets),
      id: id,
      labelText: editedSpan.labelText,
      type: editedSpan.type
    }, 'serverResult']);
  };

  displaySpanButtons(evt, target, spanId) {
    var id;
    if (id = target.attr('data-span-id')) {
      var spanPosition = target.position();

      var spanWidth = target.width();
      var spanHeight = target.height();
      var editedSpan = this.data.spans[id];
      var offsets = [];

      $.each(editedSpan.fragments, function (fragmentNo, fragment) {
        offsets.push([fragment.from, fragment.to]);
      });

      var acceptBtn = $(`<button class="span_accept_btn">Accept</button>`)
        .css({ top: 0, left: 0, width: 45, height: spanHeight })
        .on('click', ((evt) => {
          this.dispatcher.post('acceptButtonClicked', [evt, offsets, editedSpan, id]);
        }));

      var rejectBtn = $('<button class="span_reject_btn">Reject</button>')
        .css({ top: 0, right: 0, width: 45, height: spanHeight })
        .on('click', ((evt) => {
          this.dispatcher.post('rejectButtonClicked', [evt, offsets, editedSpan, id]);
        }));

      var buttonsWrapper = $('#span_btns_wrapper')
        .css({
          top: spanPosition.top, left: spanPosition.left - acceptBtn.width(),
          width: acceptBtn.width() * 2 + spanWidth
        })
        .mouseleave(function () {
          this.hideSpanButtons();
        });
      //hide the buttons when comments are hidden (i.e. mouse left the span)
      this.dispatcher.on('hideComment', this.hideSpanButtons);

      clearTimeout(this.displayButtonsTimer);
      this.displayButtonsTimer = setTimeout(function () {
        // make sure that no buttons exist and then add button
        buttonsWrapper.empty().append(acceptBtn).append(rejectBtn);

        this.buttonsShown = true;
        buttonsWrapper.show();
      }, 100);
    }
  };

  hideSpanButtons() {
    if ($("#span_btns_wrapper:hover").length != 0) {
      return;
    }
    clearTimeout(this.displayButtonsTimer);
    if (this.buttonsShown) {
      $('#span_btns_wrapper').empty().hide();
      this.buttonsShown = false;
    }
  };
  // END WEBANNO EXTENSION - #1697 - Explicit UI for accepting/recejcting recommendations

  /* START form management - related */

  rememberNormDb(response) {
    // the visualizer needs to remember aspects of the norm setup
    // so that it can avoid making queries for unconfigured or
    // missing normalization DBs.
    var norm_resources = response.normalization_config || [];
    $.each(norm_resources, function (normNo, norm) {
      var normName = norm[0];
      var serverDb = norm[3];
      this.normServerDbByNormDbName[normName] = serverDb;
    });
  }

  onDoneRendering(coll, doc, args) {
    if (args && !args.edited) {
      var $inFocus = $('#svg animate[data-type="focus"]:first').parent();
      if ($inFocus.length) {
        var svgtop = $('svg').offset().top;
        $('html,body').
          animate({ scrollTop: $inFocus.offset().top - svgtop - window.innerHeight / 2 }, { duration: 'slow', easing: 'swing' });
      }
    }
    this.dispatcher.post('allowReloadByURL');
    if (!this.currentForm) {
      $('#waiter').dialog('close');
    }
  }

  onStartedRendering() {
    if (!this.currentForm) {
      $('#waiter').dialog('open');
    }
  }

  resizeFunction(evt) {
    this.dispatcher.post('rerender');
  };

  resizerTimeout = null;
  onResize = function (evt) {
    if (evt.target === window) {
      clearTimeout(this.resizerTimeout);
      this.resizerTimeout = setTimeout(this.resizeFunction, 300);
    }
  };

  // WEBANNO EXTENSION BEGIN
  // Sending the whoami and getting the user is mandatory because many things in brat will not work
  // unless it believes that the user has logged in.
  // WEBANNO EXTENSION END
  init() {
    // Need to set a  user because many things in brat will not work otherwise
    this.user = "dummy";
    this.dispatcher.post('user', [this.user]);
    // WEBANNO EXTENSION END
    // WEBANNO EXTENSION BEGIN
    // /*
    this.dispatcher.post('ajax', [{ action: 'loadConf' }, function (response) {
      if (response.config != undefined) {
        Configuration = response.config;
        // TODO: make whole-object assignment work
        // @amadanmath: help! This code is horrible
        // Configuration.svgWidth = storedConf.svgWidth;
        this.dispatcher.post('svgWidth', [Configuration.svgWidth]);
        // Configuration.abbrevsOn = storedConf.abbrevsOn == "true";
        // Configuration.textBackgrounds = storedConf.textBackgrounds;
        // Configuration.rapidModeOn = storedConf.rapidModeOn == "true";
        // Configuration.confirmModeOn = storedConf.confirmModeOn == "true";
        // Configuration.autorefreshOn = storedConf.autorefreshOn == "true";
        if (Configuration.autorefreshOn) {
          this.checkForDocumentChanges();
        }
        // Configuration.visual.margin.x = parseInt(storedConf.visual.margin.x);
        // Configuration.visual.margin.y = parseInt(storedConf.visual.margin.y);
        // Configuration.visual.boxSpacing = parseInt(storedConf.visual.boxSpacing);
        // Configuration.visual.curlyHeight = parseInt(storedConf.visual.curlyHeight);
        // Configuration.visual.arcSpacing = parseInt(storedConf.visual.arcSpacing);
        // Configuration.visual.arcStartHeight = parseInt(storedConf.visual.arcStartHeight);
      }
      this.dispatcher.post('configurationUpdated');
    }]);
  };

  spanAndAttributeTypesLoaded(_spanTypes, _entityAttributeTypes, _eventAttributeTypes, _relationTypesHash) {
    this.spanTypes = _spanTypes;
    this.relationTypesHash = _relationTypesHash;
  };

  annotationIsAvailable() {
    this.annotationAvailable = true;
  };


  isReloadOkay() {
    // do not reload while the user is in the dialog
    return this.currentForm == null;
  };

  rememberData(_data) {
    if (_data && !_data.exception) {
      this.data = _data;
    }
  };

  // WEBANNO EXTENSION BEGIN
  contextMenu(evt) {
    // If the user shift-right-clicks, open the normal browser context menu. This is useful
    // e.g. during debugging / developing
    if (evt.shiftKey) {
      return;
    }

    var target = $(evt.target);
    var id;
    var type;

    if (target.attr('data-arc-ed')) {
      id = target.attr('data-arc-ed');
      type = target.attr('data-arc-role');
    }

    if (target.attr('data-span-id')) {
      id = target.attr('data-span-id');
      type = this.data.spans[id].type;
    }

    if (id) {
      evt.preventDefault();
      this.dispatcher.post('ajax', [{
        action: 'contextMenu',
        id: id,
        type: type,
        clientX: evt.clientX,
        clientY: evt.clientY
      }, 'serverResult']);
    }
  }
  // WEBANNO EXTENSION END
};

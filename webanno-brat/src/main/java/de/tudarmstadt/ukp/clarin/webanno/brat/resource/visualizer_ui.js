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
var VisualizerUI = (function($, window, undefined) {
    var VisualizerUI = function(dispatcher, svg) {
      var that = this;

      var messagePostOutFadeDelay = 1000;
      var messageDefaultFadeDelay = 3000;
      var defaultFloatFormat = '%.1f/right';

      var documentListing = null; // always documents of current collection
      var selectorData = null;    // can be search results when available
      var searchActive = false;   // whether search results received and in use
      var loadedSearchData = null;

      var currentForm;
      var spanTypes = null;
      var relationTypesHash = null;
      // TODO: confirm unnecessary and remove
//       var attributeTypes = null;
      var data = null;
      var mtime = null;
      var searchConfig = null;
      var coll, doc, args;
      var collScroll;
      var docScroll;
      var user = null;
      var annotationAvailable = false;

      var svgElement = $(svg._svg);
      var svgId = svgElement.parent().attr('id');

      var maxMessages = 100;

      var currentDocumentSVGsaved = false;
      var fileBrowserClosedWithSubmit = false;


      // normalization: server-side DB by norm DB name
      var normServerDbByNormDbName = {};

      var matchFocus = '';
      var matches = '';

// WEBANNO EXTENSION BEGIN
// Not required by WebAnno
/*
      // START "no svg" message - related

      var noSvgTimer = null;

      // this is necessary for centering
      $('#no_svg_wrapper').css('display', 'table');
      // on initial load, hide the "no SVG" message
      $('#no_svg_wrapper').hide();

      var hideNoDocMessage = function() {
        clearTimeout(noSvgTimer);
        $('#no_svg_wrapper').hide(0);
        $('#source_files').show();
      }

      var showNoDocMessage = function() {
        clearTimeout(noSvgTimer);
        noSvgTimer = setTimeout(function() {
          $('#no_svg_wrapper').fadeIn(500);
        }, 2000);
        $('#source_files').hide();
      }
      
      // END "no svg" message - related
*/
// WEBANNO EXTENSION END

// WEBANNO EXTENSION BEGIN
// Not required by WebAnno
/*
      // START collection browser sorting - related

      var lastGoodCollection = '/';
      var sortOrder = [2, 1]; // column (0..), sort order (1, -1)
      var collectionSortOrder; // holds previous sort while search is active
      var docSortFunction = function(a, b) {
          // parent at the top
          if (a[2] === '..') return -1;
          if (b[2] === '..') return 1;

          // then other collections
          var aIsColl = a[0] == "c";
          var bIsColl = b[0] == "c";
          if (aIsColl !== bIsColl) return aIsColl ? -1 : 1;

          // desired column in the desired order
          var col = sortOrder[0];
          var aa = a[col];
          var bb = b[col];
          if (selectorData.header[col - 2][1] === 'string-reverse') {
            aa = aa.split('').reverse().join('');
            bb = bb.split('').reverse().join('');
          }
          if (aa != bb) return (aa < bb) ? -sortOrder[1] : sortOrder[1];

          // prevent random shuffles on columns with duplicate values
          // (alphabetical order of documents)
          aa = a[2];
          bb = b[2];
          if (aa != bb) return (aa < bb) ? -1 : 1;
          return 0;
      };

      var makeSortChangeFunction = function(sort, th, thNo) {
          $(th).click(function() {
              // TODO: avoid magic numbers in access to the selector
              // data (column 0 is type, 1 is args, rest is data)
              if (sort[0] === thNo + 1) sort[1] = -sort[1];
              else {
                var type = selectorData.header[thNo - 1][1];
                var ascending = type === "string";
                sort[0] = thNo + 1;
                sort[1] = ascending ? 1 : -1;
              }
              selectorData.items.sort(docSortFunction);
              docScroll = 0;
              showFileBrowser(); // resort
          });
      }

      // END collection browser sorting - related
*/
// WEBANNO EXTENSION END


      /* START message display - related */

      var showPullupTrigger = function() {
        $('#pulluptrigger').show('puff');
      }

      var $messageContainer = $('#messages');
      var $messagepullup = $('#messagepullup');
      var pullupTimer = null;
      var displayMessages = function(msgs) {
        var initialMessageNum = $messagepullup.children().length;

        if (msgs === false) {
          $messageContainer.children().each(function(msgElNo, msgEl) {
              $(msgEl).remove();
          });
        } else {
          $.each(msgs, function(msgNo, msg) {
            var element;
            var timer = null;
            try {
              element = $('<div class="' + msg[1] + '">' + msg[0] + '</div>');
            }
            catch(x) {
              escaped = msg[0].replace(/&/g, "&amp;").replace(/</g, "&lt;").replace(/>/g, "&gt;");
              element = $('<div class="error"><b>[ERROR: could not display the following message normally due to malformed XML:]</b><br/>' + escaped + '</div>');
            }
            var pullupElement = element.clone();
            $messageContainer.append(element);
            $messagepullup.append(pullupElement.css('display', 'none'));
            slideToggle(pullupElement, true, true);

            var fader = function() {
              if ($messagepullup.is(':visible')) {
                element.remove();
              } else {
                element.hide('slow', function() {
                  element.remove();
                });
              }
            };
            var delay = (msg[2] === undefined)
                          ? messageDefaultFadeDelay
                          : (msg[2] === -1)
                              ? null
                              : (msg[2] * 1000);
            if (delay === null) {
              var button = $('<input type="button" value="OK"/>');
              element.prepend(button);
              button.click(function(evt) {
                timer = setTimeout(fader, 0);
              });
            } else {
              timer = setTimeout(fader, delay);
              element.mouseover(function() {
                  clearTimeout(timer);
                  element.show();
              }).mouseout(function() {
                  timer = setTimeout(fader, messagePostOutFadeDelay);
              });
            }
            // setTimeout(fader, messageDefaultFadeDelay);
          });

          // limited history - delete oldest
          var $messages = $messagepullup.children();
          for (var i = 0; i < $messages.length - maxMessages; i++) {
            $($messages[i]).remove();
          }
        }

        // if there is change in the number of messages, may need to
        // tweak trigger visibility
        var messageNum = $messagepullup.children().length;
        if (messageNum != initialMessageNum) {
          if (messageNum == 0) {
            // all gone; nothing to trigger
            $('#pulluptrigger').hide('slow');
          } else if (initialMessageNum == 0) {
            // first messages, show trigger at fade
            setTimeout(showPullupTrigger, messageDefaultFadeDelay+250);
          }
        }
      };

// WEBANNO EXTENSION BEGIN
// Not required by WebAnno
/*
      // hide pullup trigger by default, show on first message
      $('#pulluptrigger').hide();
      $('#pulluptrigger').
        mouseenter(function(evt) {
          $('#pulluptrigger').hide('puff');
          clearTimeout(pullupTimer);
          slideToggle($messagepullup.stop(), true, true, true);
        });
*/
// WEBANNO EXTENSION END
      $('#messagepullup').
        mouseleave(function(evt) {
          setTimeout(showPullupTrigger, 500);
          clearTimeout(pullupTimer);
          pullupTimer = setTimeout(function() {
            slideToggle($messagepullup.stop(), false, true, true);
          }, 500);
        });


      /* END message display - related */


      /* START comment popup - related */

            var adjustToCursor = function(evt, element, offset, top, right) {
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
                        if (y < 0) top = false;
                    }
                    if (!top) {
                        y = evt.clientY + offset;
                    }
                    if (right) {
                        x = evt.clientX + offset;
                        if (x >= screenWidth - elementWidth) right = false;
                    }
                    if (!right) {
                        x = evt.clientX - elementWidth - offset;
                    }
                    if (y < 0) y = 0;
                    if (x < 0) x = 0;
        			element.css({ top: y, left: x });
                };

            var commentPopup = $('#commentpopup');
            var commentDisplayed = false;

            var displayCommentTimer = null;
            var displayComment = function(evt, target, comment, commentText, commentType, immediately) {
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
                    commentPopup[0].className = idtype;
                    commentPopup.html(comment);
                    adjustToCursor(evt, commentPopup, 10, true, true);
                    clearTimeout(displayCommentTimer);
/* slight "tooltip" delay to allow highlights to be seen
           before the popup obstructs them. */
                    displayCommentTimer = setTimeout(function() {
// BEGIN WEBANNO EXTENSION - #1610 - Improve brat visualization interaction performance
// - Show/hide comments immediately instead of using an animation to avoid costly reflows
/*
                        commentPopup.stop(true, true).fadeIn(0);
*/
                        commentPopup.show();
// END WEBANNO EXTENSION - #1610 - Improve brat visualization interaction performance
                        commentDisplayed = true;
                    }, immediately ? 0 : 500);
                };

            // to avoid clobbering on delayed response
            var commentPopupNormInfoSeqId = 0;

            var normInfoSortFunction = function(a, b) {
                    // images at the top
                    if (a[0].toLowerCase() == '<img>') return -1;
                    if (b[0].toLowerCase() == '<img>') return 1;
                    // otherwise stable
                    return Util.cmp(a[2], b[2]);
                }

// WEBANNO EXTENSION BEGIN - #587 Customize mouse hover text   
            var displaySpanComment = function(
                evt, target, spanId, spanType, mods, spanText, hoverText, commentText, commentType, normalizations) {
// WEBANNO EXTENSION END - #587 Customize mouse hover text

                    var immediately = false;
                    var comment = ('<div><span class="comment_type_id_wrapper">' + '<span class="comment_type">' + Util.escapeHTML(Util.spanDisplayForm(spanTypes, spanType)) + '</span>' + ' ' + '<span class="comment_id">' + 'ID:' + Util.escapeHTML(spanId) + '</span></span>');
                    if (mods.length) {
                        comment += '<div>' + Util.escapeHTML(mods.join(', ')) + '</div>';
                    }

                    comment += '</div>';
                    
// WEBANNO EXTENSION BEGIN - #587 Customize mouse hover text
                    if (hoverText) {
                        comment += ('<div class="comment_text">' + Util.escapeHTML(hoverText) + '</div>');
                    } else if (spanText) {
                        comment += ('<div class="comment_text">"' + Util.escapeHTML(spanText) + '"</div>');
                    }
// WEBANNO EXTENSION END - #587 Customize mouse hover text
                    
                    var validArcTypesForDrag = dispatcher.post('getValidArcTypesForDrag', [spanId, spanType]);
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
                    $.each(normalizations, function(normNo, norm) {
                        var dbName = norm[0],
                            dbKey = norm[1];
// WEBANNO EXTENSION BEGIN - #1293 Display information via the brat "normalization" mechanism
/*
                        var dbName = norm[0],
                            dbKey = norm[1];
                        comment += ('<hr/>' + '<span class="comment_id">' + Util.escapeHTML(dbName) + ':' + Util.escapeHTML(dbKey) + '</span>');
                        if (dbName in normServerDbByNormDbName && normServerDbByNormDbName[dbName] != '<NONE>') {
                            // DB available, add drop-off point to HTML and store
                            // query parameters
                            commentPopupNormInfoSeqId++;
                            comment += ('<br/><div id="norm_info_drop_point_' + commentPopupNormInfoSeqId + '"/>');
                            normsToQuery.push([dbName, dbKey, commentPopupNormInfoSeqId]);
                        } else {
                            // no DB, just attach "human-readable" text provided
                            // with the annotation, if any
                            if (norm[2]) {
                                comment += ('<br/><span class="norm_info_value">' + Util.escapeHTML(norm[2]) + '</span>');
                            }
                        }
*/
                        if (norm[2]) {
                            var cateogory = norm[0],
                                key = norm[1];
                                value = norm[2];
                            // no DB, just attach "human-readable" text provided
                            // with the annotation, if any
                            if (cateogory) {
                              comment += ```
                                  <hr/>
                                  <span class="comment_id">${Util.escapeHTML(cateogory)}</span>'
                                  ```;
                            }
                            
                            if (key) {
                              comment += ```
                                  <span class="norm_info_label">${Util.escapeHTML(key)}</span>
                                  ```;
                            }
                            
                            comment += ```
                                  <span class="norm_info_value">${Util.escapeHTML(value).replace(/\n/g, "<br/>")}</span>
                                  <br/>
                                  ```;
                        } else {
                            // DB available, add drop-off point to HTML and store
                            // query parameters
                            var dbName = norm[0],
                                dbKey = norm[1];
                            commentPopupNormInfoSeqId++;
                            comment += ('<hr/>' + '<span class="comment_id">' + Util.escapeHTML(dbName) + ':' + Util.escapeHTML(dbKey) + '</span>');
                            comment += ('<br/><div id="norm_info_drop_point_' + commentPopupNormInfoSeqId + '"/>');
                            normsToQuery.push([dbName, dbKey, commentPopupNormInfoSeqId]);
                        }
// WEBANNO EXTENSION END - #1293 Display information via the brat "normalization" mechanism
                    });

                    // display initial comment HTML 
                    displayComment(evt, target, comment, commentText, commentType, immediately);

                    // initiate AJAX calls for the normalization data to query
                    $.each(normsToQuery, function(normqNo, normq) {
                        // TODO: cache some number of most recent norm_get_data results
                        var dbName = normq[0],
                            dbKey = normq[1],
                            infoSeqId = normq[2];
                        dispatcher.post('ajax', [{
                            action: 'normData',
                            database: dbName,
                            key: dbKey,
                            collection: coll,
// WEBANNO EXTENSION BEGIN - #1293 Display information via the brat "normalization" mechanism
                            id: spanId,
                            type: spanType,
// WEBANNO EXTENSION END - #1293 Display information via the brat "normalization" mechanism
                        }, function(response) {
                            if (response.exception) {; // TODO: response to error
// WEBANNO EXTENSION BEGIN - #1293 Display information via the brat "normalization" mechanism
/*
                            } else if (!response.value) {; // TODO: response to missing key
*/
                            } else if (!response.results) {; // TODO: response to missing key
// WEBANNO EXTENSION END - #1293 Display information via the brat "normalization" mechanism
                            } else {
                                // extend comment popup with normalization data
                                norminfo = '';
                                // flatten outer (name, attr, info) array (idx for sort)
                                infos = [];
                                var idx = 0;
// WEBANNO EXTENSION BEGIN - #1293 Display information via the brat "normalization" mechanism
/*
                                for (var i = 0; i < response.value.length; i++) {
                                    for (var j = 0; j < response.value[i].length; j++) {
                                        var label = response.value[i][j][0];
                                        var value = response.value[i][j][1];
                                        infos.push([label, value, idx++]);
                                    }
                                }
*/
                                for (var j = 0; j < response.results.length; j++) {
                                    var label = response.results[j][0];
                                    var value = response.results[j][1];
                                    infos.push([label, value, idx++]);
                                }
// WEBANNO EXTENSION END - #1293 Display information via the brat "normalization" mechanism
                                // sort, prioritizing images (to get floats right)
                                infos = infos.sort(normInfoSortFunction);
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

                                            norminfo += ('<span class="norm_info_label">' + Util.escapeHTML(label) + '</span>' + '<span class="norm_info_value">' + ':' + Util.escapeHTML(value).replace(/\n/g, "<br/>") + '</span>' + '<br/>');
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
                    });
                };

            var displayArcComment = function(
                evt, target, symmetric, arcId, originSpanId, originSpanType, role, targetSpanId, targetSpanType, commentText, commentType) {
                    var arcRole = target.attr('data-arc-role');
                    // in arrowStr, &#8212 == mdash, &#8594 == Unicode right arrow
                    var arrowStr = symmetric ? '&#8212;' : '&#8594;';
                    var arcDisplayForm = Util.arcDisplayForm(spanTypes, data.spans[originSpanId].type, arcRole, relationTypesHash);
                    var comment = "";
                    comment += ('<span class="comment_type_id_wrapper">' + '<span class="comment_type">' + Util.escapeHTML(Util.spanDisplayForm(spanTypes, originSpanType)) + ' ' + arrowStr + ' ' + Util.escapeHTML(arcDisplayForm) + ' ' + arrowStr + ' ' + Util.escapeHTML(Util.spanDisplayForm(spanTypes, targetSpanType)) + '</span>' + '<span class="comment_id">' + (arcId ? 'ID:' + arcId : Util.escapeHTML(originSpanId) + arrowStr + Util.escapeHTML(targetSpanId)) + '</span>' + '</span>');
                    comment += ('<div class="comment_text">' + Util.escapeHTML('"' + data.spans[originSpanId].text + '"') + arrowStr + Util.escapeHTML('"' + data.spans[targetSpanId].text + '"') + '</div>');
                    displayComment(evt, target, comment, commentText, commentType);
                };

            var displaySentComment = function(
                evt, target, commentText, commentType) {
                    displayComment(evt, target, '', commentText, commentType);
                };

            var hideComment = function() {
                    clearTimeout(displayCommentTimer);
                    if (commentDisplayed) {
// BEGIN WEBANNO EXTENSION - #1610 - Improve brat visualization interaction performance
// - Show/hide comments immediately instead of using an animation to avoid costly reflows
/*
                        commentPopup.stop(true, true).fadeOut(0, function() {
                            commentDisplayed = false;
                        });
*/
                        commentPopup.hide();
                        commentDisplayed = false;
// END WEBANNO EXTENSION - #1610 - Improve brat visualization interaction performance
                    }
                };

            var onMouseMove = function(evt) {
                    if (commentDisplayed) {
                        adjustToCursor(evt, commentPopup, 10, true, true);
                    }
                };

            /* END comment popup - related */


            /* START form management - related */

            initForm = function(form, opts) {
                opts = opts || {};
                var formId = form.attr('id');

                // alsoResize is special
                var alsoResize = opts.alsoResize;
                delete opts.alsoResize;

                // Always add OK and Cancel
                var buttons = (opts.buttons || []);
                if (opts.no_ok) {
                    delete opts.no_ok;
                } else {
                    buttons.push({
                        id: formId + "-ok",
                        text: "OK",
              			click: function() { form.submit(); }
                    });
                }
                if (opts.no_cancel) {
                    delete opts.no_cancel;
                } else {
                    buttons.push({
                        id: formId + "-cancel",
                        text: "Cancel",
			            click: function() { form.dialog('close'); }
                    });
                }
                delete opts.buttons;

                opts = $.extend({
                    autoOpen: false,
                    closeOnEscape: true,
                    buttons: buttons,
                    modal: true
                }, opts);
			
                form.dialog(opts);
                form.bind('dialogclose', function() {
                    if (form == currentForm) {
                        currentForm = null;
                    }
                });

                // HACK: jQuery UI's dialog does not support alsoResize
                // nor does resizable support a jQuery object of several
                // elements
                // See: http://bugs.jqueryui.com/ticket/4666
                if (alsoResize) {
                    form.parent().resizable('option', 'alsoResize', '#' + form.attr('id') + ', ' + alsoResize);
                }
            };

            var showForm = function(form) {
                    currentForm = form;
                    // as suggested in http://stackoverflow.com/questions/2657076/jquery-ui-dialog-fixed-positioning
                    form.parent().css({
                        position: "fixed"
                    });
                    form.dialog('open');
                    slideToggle($('#pulldown').stop(), false);
                    return form;
                };

            var hideForm = function() {
                    if (!currentForm) return;
                    // currentForm.fadeOut(function() { currentForm = null; });
                    currentForm.dialog('close');
                    currentForm = null;
                };

            var rememberNormDb = function(response) {
                    // the visualizer needs to remember aspects of the norm setup
                    // so that it can avoid making queries for unconfigured or
                    // missing normalization DBs.
                    var norm_resources = response.normalization_config || [];
                    $.each(norm_resources, function(normNo, norm) {
                        var normName = norm[0];
                        var serverDb = norm[3];
                        normServerDbByNormDbName[normName] = serverDb;
                    });
                }


            var onKeyDown = function(evt) {
                    var code = evt.which;

                    if (code === $.ui.keyCode.ESCAPE) {
                        dispatcher.post('messages', [false]);
                        return;
                    }

                    if (currentForm) {
                        if (code === $.ui.keyCode.ENTER) {
                            // don't trigger submit in textareas to allow multiline text
                            // entry
                            // NOTE: spec seems to require this to be upper-case,
                            // but at least chrome 8.0.552.215 returns lowercased
                            var nodeType = evt.target.type ? evt.target.type.toLowerCase() : '';
                            if (evt.target.nodeName && evt.target.nodeName.toLowerCase() == 'input' && (nodeType == 'text' || nodeType == 'password')) {
                                currentForm.trigger('submit');
                                return false;
                            }
                        } else if (evt.ctrlKey && (code == 'F'.charCodeAt(0) || code == 'G'.charCodeAt(0))) {
                            // prevent Ctrl-F/Ctrl-G in forms
                            evt.preventDefault();
                            return false;
                        }
                        return;
                    }

                    if (code === $.ui.keyCode.TAB) {
                        //  showFileBrowser();
                        return false;
                    } else if (code == $.ui.keyCode.LEFT) {
                        //   return moveInFileBrowser(-1);
                    } else if (code === $.ui.keyCode.RIGHT) {
                        //   return moveInFileBrowser(+1);
                    } else if (evt.shiftKey && code === $.ui.keyCode.UP) {
                        //  autoPaging(true);
                    } else if (evt.shiftKey && code === $.ui.keyCode.DOWN) {
                        // autoPaging(false);
                    } else if (evt.ctrlKey && code == 'F'.charCodeAt(0)) {
                        // evt.preventDefault();
                        // showSearchForm();
                    } else if (searchActive && evt.ctrlKey && code == 'G'.charCodeAt(0)) {
                        //  evt.preventDefault();
                        //   return moveInFileBrowser(+1);
                    } else if (searchActive && evt.ctrlKey && code == 'K'.charCodeAt(0)) {
                        //  evt.preventDefault();
                        //   clearSearchResults();
                    }
                };

            var onDoneRendering = function(coll, doc, args) {
                    if (args && !args.edited) {
                        var $inFocus = $('#svg animate[data-type="focus"]:first').parent();
                        if ($inFocus.length) {
                            var svgtop = $('svg').offset().top;
                            $('html,body').
                            animate({ scrollTop: $inFocus.offset().top - svgtop - window.innerHeight / 2 }, { duration: 'slow', easing: 'swing'});
                        }
                    }
                    dispatcher.post('allowReloadByURL');
                    if (!currentForm) {
                        $('#waiter').dialog('close');
                    }
                }

            var onStartedRendering = function() {
                    hideForm();
                    if (!currentForm) {
                        $('#waiter').dialog('open');
                    }
                }

            var invalidateSavedSVG = function() {
                    // assuming that invalidation of the SVG invalidates all stored
                    // static visualizations, as others are derived from the SVG
                    $('#download_stored').hide();
                    // have a way to regenerate if dialog open when data invalidated
                    $('#stored_file_regenerate').show();
                    currentDocumentSVGsaved = false;
                };

            var slideToggle = function(el, show, autoHeight, bottom) {
                    var el = $(el);
                    var visible = el.is(":visible");
                    var height;

                    if (show === undefined) show = !visible;

                    // @amadanmath: commenting this out appears to remove the annoying
                    // misfeature where it's possible to stop the menu halfway by
                    // mousing out and back in during closing. Please check that
                    // this doesn't introduce other trouble and remove these lines.
                    //         if (show === visible) return false;
                    if (!autoHeight) {
                        height = el.data("cachedHeight");
                    } else {
                        el.height('auto');
                    }
                    if (!height) {
                        height = el.show().height();
                        el.data('cachedHeight', height);
          				if (!visible) el.hide().css({ height: 0 });
                    }

                    if (show) {
          					el.show().animate({ height: height }, {
                            duration: 150,
                            complete: function() {
                                if (autoHeight) {
                                    el.height('auto');
                                }
                            },
            				step: bottom ? function(now, fx) {
                                fx.elem.scrollTop = fx.elem.scrollHeight;
                            } : undefined
                        });
                    } else {
          				el.animate({ height: 0 }, {
                            duration: 300,
                            complete: function() {
                                el.hide();
                            }
                        });
                    }
                }

                // TODO: copy from annotator_ui; DRY it up
            var adjustFormToCursor = function(evt, element) {
                    var screenHeight = $(window).height() - 8; // TODO HACK - no idea why -8 is needed
                    var screenWidth = $(window).width() - 8;
                    var elementHeight = element.height();
                    var elementWidth = element.width();
                    var y = Math.min(evt.clientY, screenHeight - elementHeight);
                    var x = Math.min(evt.clientX, screenWidth - elementWidth);
        			element.css({ top: y, left: x });
                };
            var viewspanForm = $('#viewspan_form');
            var onDblClick = function(evt) {
                    if (user && annotationAvailable) return;
                    var target = $(evt.target);
                    var id;
                    if (id = target.attr('data-span-id')) {
                        window.getSelection().removeAllRanges();
                        var span = data.spans[id];

                        var urlHash = URLHash.parse(window.location.hash);
          				urlHash.setArgument('focus', [[span.id]]);
                        $('#viewspan_highlight_link').show().attr('href', urlHash.getHash());

                        $('#viewspan_selected').text(span.text);
                        var encodedText = encodeURIComponent(span.text);
                        $.each(searchConfig, function(searchNo, search) {
                            $('#viewspan_' + search[0]).attr('href', search[1].replace('%s', encodedText));
                        });
                        // annotator comments
                        $('#viewspan_notes').val(span.annotatorNotes || '');
                        dispatcher.post('showForm', [viewspanForm]);
                        $('#viewspan_form-ok').focus();
                        adjustFormToCursor(evt, viewspanForm.parent());
                    }
                };
            viewspanForm.submit(function(evt) {
                dispatcher.post('hideForm');
                return false;
            });

            var resizeFunction = function(evt) {
// WEBANNO EXTENSION BEGIN - #1519 - Optimize re-rendering of brat view when window is resizes
/*
            	window.location.reload();
*/
            	dispatcher.post('rerender');
// WEBANNO EXTENSION BEGIN - #1519 - Optimize re-rendering of brat view when window is resizes
            };

              var resizerTimeout = null;
              var onResize = function(evt) {
                if (evt.target === window) {
                  clearTimeout(resizerTimeout);
                  resizerTimeout = setTimeout(resizeFunction, 300);
                }
              };
              
// WEBANNO EXTENSION BEGIN
// Sending the whoami and getting the user is mandatory because many things in brat will not work
// unless it believes that the user has logged in.
// WEBANNO EXTENSION END
      var init = function() {
// WEBANNO EXTENSION BEGIN
/*
        dispatcher.post('initForm', [viewspanForm, {
            width: 760,
            no_cancel: true
          }]);
        dispatcher.post('ajax', [{
            action: 'whoami'
          }, function(response) {
            var auth_button = $('#auth_button');
            if (response.user) {
              user = response.user;
              dispatcher.post('messages', [[['Welcome back, user "' + user + '"', 'comment']]]);
              auth_button.val('Logout ' + user);
              dispatcher.post('user', [user]);
              $('.login').show();
            } else {
              user = null;
              auth_button.val('Login');
              dispatcher.post('user', [null]);
              $('.login').hide();
              // don't show tutorial if there's a specific document (annoyance)
              if (!doc) {
                dispatcher.post('showForm', [tutorialForm]);
                $('#tutorial-ok').focus();
              }
            }
          },
          { keep: true }
        ]);
*/
        // Need to set a  user because many things in brat will not work otherwise
        user = "dummy";
        dispatcher.post('user', [user]);
// WEBANNO EXTENSION END
// WEBANNO EXTENSION BEGIN
// /*
        dispatcher.post('ajax', [{ action: 'loadConf' }, function(response) {
          if (response.config != undefined) {
// WEBANNO EXTENSION BEGIN
// WebAnno sends the configuration as a proper JSON object - no need to parse it
/*
            // TODO: check for exceptions
            try {
              Configuration = JSON.parse(response.config);
            } catch(x) {
              // XXX Bad config
              Configuration = {};
              dispatcher.post('messages', [[['Corrupted configuration; resetting.', 'error']]]);
              configurationChanged();
            }
*/
            Configuration = response.config;
// WEBANNO EXTENSION END
            // TODO: make whole-object assignment work
            // @amadanmath: help! This code is horrible
            // Configuration.svgWidth = storedConf.svgWidth;
            dispatcher.post('svgWidth', [Configuration.svgWidth]);
            // Configuration.abbrevsOn = storedConf.abbrevsOn == "true";
            // Configuration.textBackgrounds = storedConf.textBackgrounds;
            // Configuration.rapidModeOn = storedConf.rapidModeOn == "true";
            // Configuration.confirmModeOn = storedConf.confirmModeOn == "true";
            // Configuration.autorefreshOn = storedConf.autorefreshOn == "true";
            if (Configuration.autorefreshOn) {
              checkForDocumentChanges();
            }
            // Configuration.visual.margin.x = parseInt(storedConf.visual.margin.x);
            // Configuration.visual.margin.y = parseInt(storedConf.visual.margin.y);
            // Configuration.visual.boxSpacing = parseInt(storedConf.visual.boxSpacing);
            // Configuration.visual.curlyHeight = parseInt(storedConf.visual.curlyHeight);
            // Configuration.visual.arcSpacing = parseInt(storedConf.visual.arcSpacing);
            // Configuration.visual.arcStartHeight = parseInt(storedConf.visual.arcStartHeight);
          }
          dispatcher.post('configurationUpdated');
        }]);
      };
// WEBANNO EXTENSION BEGIN
/*
      var noFileSpecified = function() {
        // not (only) an error, so no messaging
        dispatcher.post('clearSVG');
        showFileBrowser();
      }

      var showUnableToReadTextFile = function() {
        dispatcher.post('messages', [[['Unable to read the text file.', 'error']]]);
        dispatcher.post('clearSVG');
        showFileBrowser();
      };

      var showAnnotationFileNotFound = function() {
        dispatcher.post('messages', [[['Annotation file not found.', 'error']]]);
        dispatcher.post('clearSVG');
        showFileBrowser();
      };

      var showUnknownError = function(exception) {
        dispatcher.post('messages', [[['Unknown error: ' + exception, 'error']]]);
        dispatcher.post('clearSVG');
        showFileBrowser();
      };

      var reloadDirectoryWithSlash = function(sourceData) {
        var collection = sourceData.collection + sourceData.document + '/';
        dispatcher.post('setCollection', [collection, '', sourceData.arguments]);
      };

      // TODO: confirm attributeTypes unnecessary and remove
//       var spanAndAttributeTypesLoaded = function(_spanTypes, _attributeTypes) {
//         spanTypes = _spanTypes;
//         attributeTypes = _attributeTypes;
//       };
      // TODO: spanAndAttributeTypesLoaded is obviously not descriptive of
      // the full function. Rename reasonably.
*/
// WEBANNO EXTENSION END

      var spanAndAttributeTypesLoaded = function(_spanTypes, _entityAttributeTypes, _eventAttributeTypes, _relationTypesHash) {
        spanTypes = _spanTypes;
        relationTypesHash = _relationTypesHash;
      };

      var annotationIsAvailable = function() {
        annotationAvailable = true;
      };


      var isReloadOkay = function() {
        // do not reload while the user is in the dialog
        return currentForm == null;
      };

      var configurationChanged = function() {
        // just assume that any config change makes stored
        // visualizations invalid. This is a bit excessive (not all
        // options affect visualization) but mostly harmless.
        invalidateSavedSVG();

        // save configuration changed by user action
        dispatcher.post('ajax', [{
                    action: 'saveConf',
                    config: JSON.stringify(Configuration),
                }, null]);
      };

// WEBANNO EXTENSION BEGIN
/*
      var updateConfigurationUI = function() {
        // update UI to reflect non-user config changes (e.g. load)
        
        // Annotation mode
        if (Configuration.confirmModeOn) {
          $('#annotation_speed1')[0].checked = true;
        } else if (Configuration.rapidModeOn) {
          $('#annotation_speed3')[0].checked = true;
        } else {
          $('#annotation_speed2')[0].checked = true;
        }
        $('#annotation_speed input').button('refresh');

        // Label abbrevs
        $('#label_abbreviations_on')[0].checked  = Configuration.abbrevsOn;
        $('#label_abbreviations_off')[0].checked = !Configuration.abbrevsOn; 
        $('#label_abbreviations input').button('refresh');

        // Text backgrounds        
        $('#text_backgrounds input[value="'+Configuration.textBackgrounds+'"]')[0].checked = true;
        $('#text_backgrounds input').button('refresh');

        // SVG width
        var splitSvgWidth = Configuration.svgWidth.match(/^(.*?)(px|\%)$/);
        if (!splitSvgWidth) {
          // TODO: reset to sensible value?
          dispatcher.post('messages', [[['Error parsing SVG width "'+Configuration.svgWidth+'"', 'error', 2]]]);
        } else {
          $('#svg_width_value')[0].value = splitSvgWidth[1];
          $('#svg_width_unit input[value="'+splitSvgWidth[2]+'"]')[0].checked = true;
          $('#svg_width_unit input').button('refresh');
        }

        // Autorefresh
        $('#autorefresh_mode')[0].checked = Configuration.autorefreshOn;
        $('#autorefresh_mode').button('refresh');

        // Type Collapse Limit
        $('#type_collapse_limit')[0].value = Configuration.typeCollapseLimit;
      }

      $('#prev').button().click(function() {
        return moveInFileBrowser(-1);
      });
      $('#next').button().click(function() {
        return moveInFileBrowser(+1);
      });
      $('#footer').show();

      $('#source_collection_conf_on, #source_collection_conf_off').change(function() {
        var conf = $('#source_collection_conf_on').is(':checked') ? 1 : 0;
        var $source_collection_link = $('#source_collection a');
        var link = $source_collection_link.attr('href').replace(/&include_conf=./, '&include_conf=' + conf);
        $source_collection_link.attr('href', link);
      });
*/
// WEBANNO EXTENSION END

      var rememberData = function(_data) {
        if (_data && !_data.exception) {
          data = _data;
        }
      };
      
// WEBANNO EXTENSION BEGIN
/*
      var onScreamingHalt = function() {
        $('#waiter').dialog('close');
        $('#pulldown, #navbuttons, #spinner').remove();
        dispatcher.post('hideForm');
      };
*/
// WEBANNO EXTENSION END

// WEBANNO EXTENSION BEGIN
      var contextMenu = function(evt) {
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
          type = data.spans[id].type;
        }
  
        if (id) {
          evt.preventDefault();
          dispatcher.post('ajax', [ {
            action: 'contextMenu',
            id: id,
            type: type,
            clientX: evt.clientX,
            clientY: evt.clientY
          }, 'serverResult']);
        }
      }
// WEBANNO EXTENSION END
      
      
      dispatcher.
          on('init', init).
          on('dataReady', rememberData).
          on('annotationIsAvailable', annotationIsAvailable).
// WEBANNO EXTENSION BEGIN
/*
          on('messages', displayMessages).
*/
// WEBANNO EXTENSION END
          on('displaySpanComment', displaySpanComment).
          on('displayArcComment', displayArcComment).
          on('displaySentComment', displaySentComment).
// WEBANNO EXTENSION BEGIN
/*
          on('docChanged', onDocChanged).
*/
// WEBANNO EXTENSION END
          on('hideComment', hideComment).
// WEBANNO EXTENSION BEGIN
/*
          on('showForm', showForm).
          on('hideForm', hideForm).
          on('initForm', initForm).
*/
// WEBANNO EXTENSION END
          on('resize', onResize).
          on('collectionLoaded', rememberNormDb).
// WEBANNO EXTENSION BEGIN
/*
          on('collectionLoaded', collectionLoaded).
*/
// WEBANNO EXTENSION END
          on('spanAndAttributeTypesLoaded', spanAndAttributeTypesLoaded).
          on('isReloadOkay', isReloadOkay).
// WEBANNO EXTENSION BEGIN
/*
          on('current', gotCurrent).
*/
// WEBANNO EXTENSION END
          on('doneRendering', onDoneRendering).
          on('startedRendering', onStartedRendering).
// WEBANNO EXTENSION BEGIN
/*
          on('newSourceData', onNewSourceData).
          on('savedSVG', savedSVGreceived).
          on('renderError:noFileSpecified', noFileSpecified).
          on('renderError:annotationFileNotFound', showAnnotationFileNotFound).
          on('renderError:unableToReadTextFile', showUnableToReadTextFile).
          on('renderError:isDirectoryError', reloadDirectoryWithSlash).
          on('unknownError', showUnknownError).
*/
// WEBANNO EXTENSION END
          on('keydown', onKeyDown).
          on('mousemove', onMouseMove).
          on('contextmenu', contextMenu);
// WEBANNO EXTENSION BEGIN
/*
          on('dblclick', onDblClick).
          on('touchstart', onTouchStart).
          on('touchend', onTouchEnd).
          on('resize', onResize).
          on('searchResultsReceived', searchResultsReceived).
          on('clearSearch', clearSearch).
          on('clearSVG', showNoDocMessage).
          on('screamingHalt', onScreamingHalt).
          on('configurationChanged', configurationChanged).
          on('configurationUpdated', updateConfigurationUI);
*/
// WEBANNO EXTENSION END
    };
    
    return VisualizerUI;
})(jQuery, window);

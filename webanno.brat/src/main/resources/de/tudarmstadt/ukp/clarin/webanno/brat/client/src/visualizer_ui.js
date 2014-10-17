// -*- Mode: JavaScript; tab-width: 2; indent-tabs-mode: nil; -*-
// vim:set ft=javascript ts=2 sw=2 sts=2 cindent:
var VisualizerUI = (function($, window, undefined) {
    var VisualizerUI = function(dispatcher, svg) {
            var that = this;

            var messagePostOutFadeDelay = 1000;
            var messageDefaultFadeDelay = 3000;
            var defaultFloatFormat = '%.1f/right';

            var documentListing = null; // always documents of current collection
            var selectorData = null; // can be search results when available
            var searchActive = false; // whether search results received and in use
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

            /* START "no svg" message - related */


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
                            var delay = (msg[2] === undefined) ? messageDefaultFadeDelay : (msg[2] === -1) ? null : (msg[2] * 1000);
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
                            setTimeout(showPullupTrigger, messageDefaultFadeDelay + 250);
                        }
                    }
                };


            $('#messagepullup').
            mouseleave(function(evt) {
                setTimeout(showPullupTrigger, 500);
                clearTimeout(pullupTimer);
                pullupTimer = setTimeout(function() {
                    slideToggle($messagepullup.stop(), false, true, true);
                }, 500);
            });

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
                        commentPopup.stop(true, true).fadeIn();
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

            var displaySpanComment = function(
                evt, target, spanId, spanType, mods, spanText, commentText, commentType, normalizations) {

                    var immediately = false;
                    var comment = ('<div><span class="comment_type_id_wrapper">' + '<span class="comment_type">' + Util.escapeHTML(Util.spanDisplayForm(spanTypes, spanType)) + '</span>' + ' ' + '<span class="comment_id">' + 'ID:' + Util.escapeHTML(spanId) + '</span></span>');
                    if (mods.length) {
                        comment += '<div>' + Util.escapeHTML(mods.join(', ')) + '</div>';
                    }

                    comment += '</div>';
                    comment += ('<div class="comment_text">"' + Util.escapeHTML(spanText) + '"</div>');
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
                        }, function(response) {
                            if (response.exception) {; // TODO: response to error
                            } else if (!response.value) {; // TODO: response to missing key
                            } else {
                                // extend comment popup with normalization data
                                norminfo = '';
                                // flatten outer (name, attr, info) array (idx for sort)
                                infos = [];
                                var idx = 0;
                                for (var i = 0; i < response.value.length; i++) {
                                    for (var j = 0; j < response.value[i].length; j++) {
                                        var label = response.value[i][j][0];
                                        var value = response.value[i][j][1];
                                        infos.push([label, value, idx++]);
                                    }
                                }
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

                                            norminfo += ('<span class="norm_info_label">' + Util.escapeHTML(label) + '</span>' + '<span class="norm_info_value">' + ':' + Util.escapeHTML(value) + '</span>' + '<br/>');
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
                        commentPopup.stop(true, true).fadeOut(function() {
                            commentDisplayed = false;
                        });
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
                        var svgtop = $('svg').offset().top;
                        var $inFocus = $('#svg animate[data-type="focus"]:first').parent();
                        if ($inFocus.length) {
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
            	window.location.reload();
              };

              var resizerTimeout = null;
              var onResize = function(evt) {
                if (evt.target === window) {
                  clearTimeout(resizerTimeout);
                  resizerTimeout = setTimeout(resizeFunction, 100); // TODO is 100ms okay?
                }
              };
              
            var init = function() {
                    dispatcher.post('initForm', [viewspanForm,
                    {
                        width: 760,
                        no_cancel: true
                    }]);
                    dispatcher.post('ajax', [{
                        action: 'whoami'
                    }, function(response) {
                        if (response.user) {
                            user = response.user;
              				dispatcher.post('messages', [[['Welcome back, user "' + user + '"', 'comment']]]);
                            dispatcher.post('user', [user]);
                        } else {
                            user = null;
                            dispatcher.post('user', [null]);

                            // don't show tutorial if there's a specific document (annoyance)
                        }
                    }, 
                    { keep: true }
                  ]);
                };
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

            var rememberData = function(_data) {
                    if (_data && !_data.exception) {
                        data = _data;
                    }
                };
            dispatcher.
            on('init', init).
            on('dataReady', rememberData).
            on('annotationIsAvailable', annotationIsAvailable).
            on('messages', displayMessages).
            on('displaySpanComment', displaySpanComment).
            on('displayArcComment', displayArcComment).
            on('displaySentComment', displaySentComment).
            on('hideComment', hideComment).
            on('showForm', showForm).
            on('hideForm', hideForm).
            on('initForm', initForm).
            on('resize', onResize).
            on('collectionLoaded', rememberNormDb).
            on('spanAndAttributeTypesLoaded', spanAndAttributeTypesLoaded).
            on('isReloadOkay', isReloadOkay).
            on('doneRendering', onDoneRendering).
            on('startedRendering', onStartedRendering).
            on('keydown', onKeyDown).
            on('mousemove', onMouseMove).
            on('dblclick', onDblClick);
        };

    return VisualizerUI;
})(jQuery, window);
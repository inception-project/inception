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
import type { Dispatcher } from '../dispatcher/Dispatcher'

import { INSTANCE as Configuration } from '../configuration/Configuration'

import { INSTANCE as Util } from '../util/Util'
import { DocumentData } from '../visualizer/DocumentData'
import { RelationTypeDto, EntityTypeDto, VID, CommentType } from '../protocol/Protocol'
import { Offsets } from '@inception-project/inception-js-api'
import { Entity } from '../visualizer/Entity'

export class VisualizerUI {
  private spanTypes: Record<string, EntityTypeDto> = null
  private relationTypesHash:Record<string, RelationTypeDto> = null
  private data: DocumentData = null

  // normalization: server-side DB by norm DB name
  private normServerDbByNormDbName = {}

  private dispatcher: Dispatcher

  private commentPopup: JQuery
  private commentDisplayed = false
  private displayCommentTimer = null

  constructor (dispatcher: Dispatcher) {
    console.debug('Setting up brat visualizer-ui module...')

    this.commentPopup = $('#commentpopup')
    this.dispatcher = dispatcher
    this.dispatcher
      .on('init', this, this.init)
      .on('dataReady', this, this.rememberData)
      .on('displaySpanComment', this, this.displaySpanComment)
      .on('displayArcComment', this, this.displayArcComment)
      .on('displaySentComment', this, this.displaySentComment)
      .on('hideComment', this, this.hideComment)
      .on('resize', this, this.onResize)
      .on('collectionLoaded', this, this.rememberNormDb)
      .on('spanAndAttributeTypesLoaded', this, this.spanAndAttributeTypesLoaded)
      .on('doneRendering', this, this.onDoneRendering)
      .on('mousemove', this, this.onMouseMove)
      .on('displaySpanButtons', this, this.displaySpanButtons)
      .on('acceptButtonClicked', this, this.acceptAction)
      .on('rejectButtonClicked', this, this.rejectAction)
      .on('contextmenu', this, this.contextMenu)
  }

  /* START comment popup - related */

  adjustToCursor (evt: MouseEvent, element, offset, top, right) {
    // get the real width, without wrapping
    element.css({ left: 0, top: 0 })
    const screenWidth = $(window).width()
    // FIXME why the hell is this 22 necessary?!?
    const elementHeight = element.height() + 22
    const elementWidth = element.width() + 22
    let x, y
    offset = offset || 0
    if (top) {
      y = evt.clientY - elementHeight - offset
      if (y < 0) { top = false }
    }
    if (!top) {
      y = evt.clientY + offset
    }
    if (right) {
      x = evt.clientX + offset
      if (x >= screenWidth - elementWidth) { right = false }
    }
    if (!right) {
      x = evt.clientX - elementWidth - offset
    }
    if (y < 0) { y = 0 }
    if (x < 0) { x = 0 }
    element.css({ top: y, left: x })
  }

  displaySentComment (evt: MouseEvent, commentText: string, commentType: CommentType) {
    this.displayComment(evt, '', commentText, commentType)
  }

  displayComment (evt: MouseEvent, comment: string, commentText: string, commentType: CommentType, immediately?: boolean) {
    let idtype = ''
    if (commentType) {
      // label comment by type, with special case for default note type
      let commentLabel: string
      if (commentType === 'AnnotatorNotes') {
        commentLabel = '<b>Note:</b> '
      } else {
        commentLabel = '<b>' + Util.escapeHTML(commentType) + ':</b> '
      }
      comment += '<hr/>'
      comment += commentLabel + Util.escapeHTMLwithNewlines(commentText)
      idtype = 'comment_' + commentType
    }
    this.commentPopup[0].className = idtype
    this.commentPopup.html(comment)
    this.adjustToCursor(evt, this.commentPopup, 10, true, true)
    clearTimeout(this.displayCommentTimer)
    /* slight "tooltip" delay to allow highlights to be seen
               before the popup obstructs them. */
    this.displayCommentTimer = setTimeout(() => {
      this.commentPopup.show()
      this.commentDisplayed = true
    }, immediately ? 0 : 500)
  }

  // to avoid clobbering on delayed response
  commentPopupNormInfoSeqId = 0

  normInfoSortFunction (a, b) {
    // images at the top
    if (a[0].toLowerCase() === '<img>') return -1
    if (b[0].toLowerCase() === '<img>') return 1
    // otherwise stable
    return Util.cmp(a[2], b[2])
  }

  displaySpanComment (evt, target, spanId, spanType, mods, spanText, hoverText,
    commentText, commentType, normalizations) {
    let immediately = false
    let comment = ('<div><span class="comment_type_id_wrapper">' +
      '<span class="comment_type">' + Util.escapeHTML(Util.spanDisplayForm(this.spanTypes, spanType)) + '</span>' + ' ' +
      '<span class="comment_id">' + 'ID:' + Util.escapeHTML(spanId) + '</span></span>')
    if (mods.length) {
      comment += '<div>' + Util.escapeHTML(mods.join(', ')) + '</div>'
    }

    comment += '</div>'

    if (hoverText != null) {
      comment += ('<div class="comment_text">' + Util.escapeHTML(hoverText) + '</div>')
    } else if (spanText) {
      comment += ('<div class="comment_text">"' + Util.escapeHTML(spanText) + '"</div>')
    }

    const validArcTypesForDrag = this.dispatcher.post('getValidArcTypesForDrag', [spanId, spanType])
    if (validArcTypesForDrag && validArcTypesForDrag[0]) {
      if (validArcTypesForDrag[0].length) {
        comment += '<div>' + validArcTypesForDrag[0].join(', ') + '</div>'
      } else {
        $('rect[data-span-id="' + spanId + '"]').addClass('badTarget')
      }
      immediately = true
    }

    // process normalizations
    const normsToQuery = []
    comment += this.processNormalizations(normalizations, normsToQuery)

    // display initial comment HTML
    this.displayComment(evt, comment, commentText, commentType, immediately)

    // initiate AJAX calls for the normalization data to query
    $.each(normsToQuery, (normNo, norm) => this.initiateNormalizationAjaxCall(spanId, spanType, norm))
  }

  displayArcComment (evt, target, symmetric, arcId, originSpanId, originSpanType,
    role, targetSpanId, targetSpanType, commentText, commentType, normalizations) {
    const arcRole = target.attr('data-arc-role')
    // in arrowStr, &#8212 == mdash, &#8594 == Unicode right arrow
    const arrowStr = symmetric ? '&#8212;' : '&#8594;'
    const arcDisplayForm = Util.arcDisplayForm(this.spanTypes, this.data.spans[originSpanId].type, arcRole, this.relationTypesHash)
    let comment = ''
    comment += ('<span class="comment_type_id_wrapper">' +
      '<span class="comment_type">' + Util.escapeHTML(Util.spanDisplayForm(this.spanTypes, originSpanType)) + ' ' + arrowStr + ' ' + Util.escapeHTML(arcDisplayForm) + ' ' + arrowStr + ' ' + Util.escapeHTML(Util.spanDisplayForm(this.spanTypes, targetSpanType)) + '</span>' +
      '<span class="comment_id">' + (arcId ? 'ID:' + arcId : Util.escapeHTML(originSpanId) + arrowStr + Util.escapeHTML(targetSpanId)) + '</span>' +
      '</span>')
    comment += ('<div class="comment_text">' + Util.escapeHTML('"' + this.data.spans[originSpanId].text + '"') + arrowStr + Util.escapeHTML('"' + this.data.spans[targetSpanId].text + '"') + '</div>')

    // process normalizations
    const normsToQuery = []
    comment += this.processNormalizations(normalizations, normsToQuery)

    this.displayComment(evt, comment, commentText, commentType)

    // initiate AJAX calls for the normalization data to query
    $.each(normsToQuery, (normNo, norm) => this.initiateNormalizationAjaxCall(arcId, arcRole, norm))
  }

  processNormalizations (normalizations, normsToQuery) {
    let comment = ''
    $.each(normalizations != null ? normalizations : [], (normNo, norm) => {
      if (norm[2]) {
        const cateogory = norm[0]
        const key = norm[1]
        const value = norm[2]
        // no DB, just attach "human-readable" text provided with the annotation, if any
        if (cateogory) {
          comment += `<hr/>
                      <span class="comment_id">${Util.escapeHTML(cateogory)}</span>'
                      `
        }

        if (key) {
          comment += `<span class="norm_info_label">${Util.escapeHTML(key)}</span>
                     `
        }

        comment += `<span class="norm_info_value">${Util.escapeHTML(value)?.replace(/\n/g, '<br/>')}</span>
                    <br/>
                    `
      } else {
        // DB available, add drop-off point to HTML and store query parameters
        const dbName = norm[0]
        const dbKey = norm[1]
        this.commentPopupNormInfoSeqId++
        if (dbKey) {
          comment += `<hr/>
                      <span class="comment_id">${Util.escapeHTML(dbName)}: ${Util.escapeHTML(dbKey)}</span>
                      <br/>`
        } else {
          comment += '<hr/>'
        }
        comment += `<div id="norm_info_drop_point_${this.commentPopupNormInfoSeqId}"></div>`
        normsToQuery.push([dbName, dbKey, this.commentPopupNormInfoSeqId])
      }
    })
    return comment.replace(/^\s*/gm, '')
  }

  initiateNormalizationAjaxCall (id, type, normq) {
    // TODO: cache some number of most recent norm_get_data results
    const dbName = normq[0]
    const dbKey = normq[1]
    const infoSeqId = normq[2]
    this.dispatcher.post('ajax', [{
      action: 'normData',
      database: dbName,
      key: dbKey,
      id,
      type
    }, (response) => {
      if (response.exception) {
        // TODO: response to error
      } else if (!response.results) {
        // TODO: response to missing key
      } else {
        // extend comment popup with normalization data
        let norminfo = ''
        // flatten outer (name, attr, info) array (idx for sort)
        let infos : [string, string, number][] = []
        let idx = 0
        for (let j = 0; j < response.results.length; j++) {
          const label = response.results[j][0] as string
          const value = response.results[j][1] as string
          infos.push([label, value, idx++])
        }

        // sort, prioritizing images (to get floats right)
        infos = infos.sort(this.normInfoSortFunction)
        // generate HTML
        for (let i = 0; i < infos.length; i++) {
          const label = infos[i][0] as string
          let value = infos[i][1] as string
          if (label && value) {
            // special treatment for some label values
            if (label.toLowerCase() === '<img>') {
              norminfo += `<img class="norm_info_img" src="${value}"/>`
            } else {
              // normal, as text max length restriction
              if (value.length > 300) {
                value = value.substr(0, 300) + ' ...'
              }

              norminfo += `<span class="norm_info_label">${Util.escapeHTML(label)}</span>
                           <span class="norm_info_value">: ${Util.escapeHTML(value)?.replace(/\n/g, '<br/>')}</span>
                           <br/>`
            }
          }
        }
        const drop = $('#norm_info_drop_point_' + infoSeqId)
        if (drop) {
          drop.html(norminfo)
        } else {
          console.log('norm info drop point not found!') // TODO XXX
        }
      }
    }])
  }

  hideComment () {
    clearTimeout(this.displayCommentTimer)
    if (this.commentDisplayed) {
      // BEGIN WEBANNO EXTENSION - #1610 - Improve brat visualization interaction performance
      // - Show/hide comments immediately instead of using an animation to avoid costly reflows
      /*
                              commentPopup.stop(true, true).fadeOut(0, function() {
                                  commentDisplayed = false;
                              });
      */
      this.commentPopup.hide()
      this.commentDisplayed = false
      // END WEBANNO EXTENSION - #1610 - Improve brat visualization interaction performance
    }
    clearTimeout(this.displayButtonsTimer)
  }

  onMouseMove (evt: MouseEvent) {
    if (this.commentDisplayed) {
      this.adjustToCursor(evt, this.commentPopup, 10, true, true)
    }
  }

  /* END comment popup - related */

  // BEGIN WEBANNO EXTENSION - #1697 - Explicit UI for accepting/recejcting recommendations
  displayButtonsTimer = null
  buttonsShown = false
  acceptAction (evt: MouseEvent, offsets, editedSpan, id) {
    evt.preventDefault()
    this.dispatcher.post('ajax', [{
      action: 'acceptAction',
      offsets: JSON.stringify(offsets),
      id,
      labelText: editedSpan.labelText,
      type: editedSpan.type
    }])
  }

  rejectAction (evt: MouseEvent, offsets: Array<Offsets>, editedSpan: Entity, id: VID) {
    evt.preventDefault()
    this.dispatcher.post('ajax', [{
      action: 'rejectAction',
      offsets: JSON.stringify(offsets),
      id,
      labelText: editedSpan.labelText,
      type: editedSpan.type
    }])
  }

  displaySpanButtons (evt: Event, target: JQuery) {
    const id = target.attr('data-span-id')
    if (!id) {
      return
    }

    const spanPosition = target.position()

    const spanWidth = target.width()
    const spanHeight = target.height()
    const editedSpan = this.data.spans[id]
    const offsets: Array<Offsets> = []

    $.each(editedSpan.fragments, (fragmentNo, fragment) => {
      offsets.push([fragment.from, fragment.to])
    })

    const acceptBtn = $('<button class="span_accept_btn">Accept</button>')
      .css({ top: 0, left: 0, width: 45, height: spanHeight })
      .on('click', (evt) => {
        this.dispatcher.post('acceptButtonClicked', [evt, offsets, editedSpan, id])
      })

    const rejectBtn = $('<button class="span_reject_btn">Reject</button>')
      .css({ top: 0, right: 0, width: 45, height: spanHeight })
      .on('click', (evt) => {
        this.dispatcher.post('rejectButtonClicked', [evt, offsets, editedSpan, id])
      })

    const buttonsWrapper = $('#span_btns_wrapper')
      .css({
        top: spanPosition.top,
        left: spanPosition.left - acceptBtn.width(),
        width: acceptBtn.width() * 2 + spanWidth
      })
      .mouseleave(this.hideSpanButtons)
    // hide the buttons when comments are hidden (i.e. mouse left the span)
    this.dispatcher.on('hideComment', this, this.hideSpanButtons)

    clearTimeout(this.displayButtonsTimer)
    this.displayButtonsTimer = setTimeout(() => {
      // make sure that no buttons exist and then add button
      buttonsWrapper.empty().append(acceptBtn).append(rejectBtn)

      this.buttonsShown = true
      buttonsWrapper.show()
    }, 100)
  }

  hideSpanButtons () {
    if ($('#span_btns_wrapper:hover').length != 0) {
      return
    }
    clearTimeout(this.displayButtonsTimer)
    if (this.buttonsShown) {
      $('#span_btns_wrapper').empty().hide()
      this.buttonsShown = false
    }
  }
  // END WEBANNO EXTENSION - #1697 - Explicit UI for accepting/recejcting recommendations

  /* START form management - related */

  rememberNormDb (response) {
    // the visualizer needs to remember aspects of the norm setup
    // so that it can avoid making queries for unconfigured or
    // missing normalization DBs.
    const norm_resources = response.normalization_config || []
    $.each(norm_resources, (normNo, norm) => {
      const normName = norm[0]
      const serverDb = norm[3]
      this.normServerDbByNormDbName[normName] = serverDb
    })
  }

  onDoneRendering (args) {
    if (args && !args.edited) {
      // FIXME REC 2021-11-21 - Good idea but won't work in INCEpTION since there could
      // be multiple SVGs on screen. Should be removed or done differently.
      const $inFocus = $('#svg animate[data-type="focus"]:first').parent()
      if ($inFocus.length) {
        const svgtop = $('svg').offset().top
        $('html,body')
          .animate({ scrollTop: $inFocus.offset().top - svgtop - window.innerHeight / 2 }, { duration: 'slow', easing: 'swing' })
      }
    }
    this.dispatcher.post('allowReloadByURL')
  }

  resizerTimeout : number
  onResize (evt: Event) {
    if (evt.target === window) {
      clearTimeout(this.resizerTimeout)
      this.resizerTimeout = setTimeout(() => this.dispatcher.post('rerender'), 300)
    }
  }

  init () {
    // WEBANNO EXTENSION BEGIN
    // /*
    this.dispatcher.post('ajax', [{ action: 'loadConf' }, (response) => {
      if (response.config !== undefined) {
        Object.assign(Configuration, response.config)
        // TODO: make whole-object assignment work
        // @amadanmath: help! This code is horrible
        // Configuration.svgWidth = storedConf.svgWidth;
        this.dispatcher.post('svgWidth', [Configuration.svgWidth])
        // Configuration.abbrevsOn = storedConf.abbrevsOn == "true";
        // Configuration.textBackgrounds = storedConf.textBackgrounds;
        // Configuration.visual.margin.x = parseInt(storedConf.visual.margin.x);
        // Configuration.visual.margin.y = parseInt(storedConf.visual.margin.y);
        // Configuration.visual.boxSpacing = parseInt(storedConf.visual.boxSpacing);
        // Configuration.visual.curlyHeight = parseInt(storedConf.visual.curlyHeight);
        // Configuration.visual.arcSpacing = parseInt(storedConf.visual.arcSpacing);
        // Configuration.visual.arcStartHeight = parseInt(storedConf.visual.arcStartHeight);
      }
      this.dispatcher.post('configurationUpdated')
    }])
  }

  spanAndAttributeTypesLoaded (_spanTypes, _entityAttributeTypes, _eventAttributeTypes, _relationTypesHash) {
    this.spanTypes = _spanTypes
    this.relationTypesHash = _relationTypesHash
  }

  isReloadOkay () {
    return true
  }

  rememberData (data: DocumentData) {
    if (data && !data.exception) {
      this.data = data
    }
  }

  contextMenu (evt: MouseEvent) {
    // If the user shift-right-clicks, open the normal browser context menu. This is useful
    // e.g. during debugging / developing
    if (evt.shiftKey) {
      return
    }

    const target = $(evt.target)
    let id: string
    let type: string

    if (target.attr('data-arc-ed')) {
      id = target.attr('data-arc-ed')
      type = target.attr('data-arc-role')
    }

    if (target.attr('data-span-id')) {
      id = target.attr('data-span-id')
      type = this.data.spans[id].type
    }

    if (id) {
      evt.preventDefault()
      this.dispatcher.post('ajax', [{
        action: 'contextMenu',
        id,
        type,
        clientX: evt.clientX,
        clientY: evt.clientY
      }])
    }
  }
}

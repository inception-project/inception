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
import { DiamAjax, Offsets } from '@inception-project/inception-js-api'
import { Entity } from '../visualizer/Entity'
import { AttributeType } from '../visualizer/AttributeType'

export class VisualizerUI {
  private spanTypes: Record<string, EntityTypeDto> = {}
  private relationTypesHash: Record<string, RelationTypeDto> = {}
  private data: DocumentData | null = null

  private dispatcher: Dispatcher

  private commentPopup: JQuery
  private commentDisplayed = false
  private displayCommentTimer: number | undefined = undefined

  private ajax: DiamAjax

  constructor (dispatcher: Dispatcher, ajax: DiamAjax) {
    console.debug('Setting up brat visualizer-ui module...')

    this.ajax = ajax
    this.commentPopup = $('#commentpopup')
    this.dispatcher = dispatcher
    this.dispatcher
      .on('init', this, this.init)
      .on('dataReady', this, this.rememberData)
//      .on('displaySpanComment', this, this.displaySpanComment)
//      .on('displayArcComment', this, this.displayArcComment)
//      .on('displaySentComment', this, this.displaySentComment)
      .on('hideComment', this, this.hideComment)
      .on('resize', this, this.onResize)
      .on('spanAndAttributeTypesLoaded', this, this.spanAndAttributeTypesLoaded)
      .on('doneRendering', this, this.onDoneRendering)
      .on('mousemove', this, this.onMouseMove)
      .on('displaySpanButtons', this, this.displaySpanButtons)
      .on('acceptButtonClicked', this, this.acceptAction)
      .on('rejectButtonClicked', this, this.rejectAction)
      .on('contextmenu', this, this.contextMenu)
  }

  /* START comment popup - related */

  /**
   * @deprecated To be replaced with the new Popover component
   */
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

  /**
   * @deprecated To be replaced with the new Popover component
   */
  displaySentComment (evt: MouseEvent, commentText: string, commentType: CommentType) {
    this.displayComment(evt, '', commentText, commentType)
  }

  /**
   * @deprecated To be replaced with the new Popover component
   */
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

  /**
   * @deprecated To be replaced with the new Popover component
   */
  compareLazyDetails (a, b) {
    // images at the top
    if (a[0].toLowerCase() === '<img>') return -1
    if (b[0].toLowerCase() === '<img>') return 1
    // otherwise stable
    return Util.cmp(a[2], b[2])
  }

  /**
   * @deprecated To be replaced with the new Popover component
   */
  displaySpanComment (evt, target, spanId, spanType, mods, spanText, hoverText,
    commentText, commentType) {
    const immediately = false
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

    comment += '<div id="lazy_details_drop_point"></div>'

    // display initial comment HTML
    this.displayComment(evt, comment, commentText, commentType, immediately)

    // initiate AJAX calls for the normalization data to query
    this.initiateNormalizationAjaxCall(spanId, spanType)
  }

  /**
   * @deprecated To be replaced with the new Popover component
   */
  displayArcComment (evt, target, symmetric, arcId, originSpanId, originSpanType,
    role, targetSpanId, targetSpanType, commentText, commentType) {
    if (!this.data) return
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
    comment += '<div id="lazy_details_drop_point"></div>'

    this.displayComment(evt, comment, commentText, commentType)

    // initiate AJAX calls for the normalization data to query
    this.initiateNormalizationAjaxCall(arcId, arcRole)
  }

  /**
   * @deprecated To be replaced with the new Popover component
   */
  initiateNormalizationAjaxCall (id: VID, type: number) {
    this.ajax.loadLazyDetails(id, type).then(detailGroups => {
      // extend comment popup with normalization data
      let norminfo = ''

      for (const group of detailGroups) {
        const details = group.details
        // flatten outer (name, attr, info) array (idx for sort)
        let infos: [string, string, number][] = []
        let idx = 0
        for (let j = 0; j < details.length; j++) {
          infos.push([details[j].label, details[j].value, idx++])
        }

        // sort, prioritizing images (to get floats right)
        infos = infos.sort(this.compareLazyDetails)

        // generate HTML
        if (group.title) {
          norminfo += `<hr/>
            <span class="comment_id">${group.title}</span>
            <br/>`
        }

        for (let i = 0; i < infos.length; i++) {
          const label = infos[i][0] as string
          let value = infos[i][1] as string
          if (label && value) {
            // special treatment for some label values
            if (label.toLowerCase() === '<img>') {
              norminfo += `<img class="norm_info_img" crossorigin src="${value}"/>`
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
      }

      const drop = $('#lazy_details_drop_point')
      if (drop) {
        drop.html(norminfo)
      } else {
        console.log('Lazy details drop point not found!') // TODO XXX
      }
    })
  }

  /**
   * @deprecated To be replaced with the new Popover component
   */
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

  /**
   * @deprecated To be replaced with the new Popover component
   */
  onMouseMove (evt: MouseEvent) {
    if (this.commentDisplayed) {
      this.adjustToCursor(evt, this.commentPopup, 10, true, true)
    }
  }

  /* END comment popup - related */

  // BEGIN WEBANNO EXTENSION - #1697 - Explicit UI for accepting/recejcting recommendations
  displayButtonsTimer: number | undefined = undefined
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
  }

  resizerTimeout: number
  onResize (evt: Event) {
    if (!evt || evt.target === window) {
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

  spanAndAttributeTypesLoaded (_spanTypes: Record<string, EntityTypeDto>,
    _entityAttributeTypes: Record<string, AttributeType>, _eventAttributeTypes,
    _relationTypesHash: Record<string, RelationTypeDto>) {
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

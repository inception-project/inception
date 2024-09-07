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

import { DocumentData } from '../visualizer/DocumentData'
import { RelationTypeDto, EntityTypeDto, VID } from '../protocol/Protocol'
import { DiamAjax, Offsets } from '@inception-project/inception-js-api'
import { Entity } from '../visualizer/Entity'
import { AttributeType } from '../visualizer/AttributeType'

export class VisualizerUI {
  private spanTypes: Record<string, EntityTypeDto> = {}
  private relationTypesHash: Record<string, RelationTypeDto> = {}
  private data: DocumentData | null = null

  private dispatcher: Dispatcher

  private ajax: DiamAjax

  constructor (dispatcher: Dispatcher, ajax: DiamAjax) {
    console.debug('Setting up brat visualizer-ui module...')

    this.ajax = ajax
    this.dispatcher = dispatcher
    this.dispatcher
      .on('init', this, this.init)
      .on('dataReady', this, this.rememberData)
      .on('resize', this, this.onResize)
      .on('spanAndAttributeTypesLoaded', this, this.spanAndAttributeTypesLoaded)
      .on('doneRendering', this, this.onDoneRendering)
      .on('displaySpanButtons', this, this.displaySpanButtons)
      .on('acceptButtonClicked', this, this.acceptAction)
      .on('rejectButtonClicked', this, this.rejectAction)
      .on('contextmenu', this, this.contextMenu)
  }

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

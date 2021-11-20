/*
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
 */
import type Dispatcher from "../dispatcher";

export class CurationMod {
  data;
  dispatcher: Dispatcher;
  svg: any;

  constructor(dispatcher, svg) {
    this.dispatcher = dispatcher;
    this.svg = svg;

    dispatcher.
      on('click', this, this.onClick).
      on('dataReady', this, this.rememberData).
      on('serverResult', this, this.serverResult).
      on('contextmenu', this, this.contextMenu);
  }

  // send click to server
  onClick(evt) {
    var target = $(evt.target);
    // if clicked on a span, send ajax call to server
    let type = target.attr('data-arc-role');
    if (type) {
      var originSpanId = target.attr('data-arc-origin');
      var targetSpanId = target.attr('data-arc-target');
      var id = target.attr('data-arc-ed');
      //var originSpan = data.spans[originSpanId];
      //var targetSpan = data.spans[targetSpanId];
      this.dispatcher.post('ajax', [{
        action: 'selectArcForMerge',
        originSpanId: originSpanId,
        targetSpanId: targetSpanId,
        id: id,
        type: type
      }, 'serverResult']);
    }
    if (id = target.attr('data-span-id')) {
      var editedSpan = this.data.spans[id];
      this.dispatcher.post('ajax', [{
        action: 'selectSpanForMerge',
        id: id,
        type: editedSpan.type,
      }, 'serverResult']);
    }
    // TODO check for arcs
  };

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

  // callback function which is called after ajax response has arrived
  serverResult(response) {
    // dummy, probably not called at all
    if (response.exception) {
      // TODO: better response to failure
      this.dispatcher.post('messages', [[['Lookup error', 'warning', -1]]]);
      return false;
    }
    alert(response);
  };

  // remember data at initialization
  rememberData(_data) {
    if (_data && !_data.exception) {
      this.data = _data;
    }
  };
}

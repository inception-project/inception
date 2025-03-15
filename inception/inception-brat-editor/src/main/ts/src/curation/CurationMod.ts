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
import { Dispatcher } from '../dispatcher/Dispatcher'
import { DocumentData } from '../visualizer/DocumentData'
import { DiamAjax } from '@inception-project/inception-js-api'

export class CurationMod {
  private data: DocumentData
  private dispatcher: Dispatcher
  private ajax: DiamAjax

  constructor (dispatcher: Dispatcher, ajax: DiamAjax) {
    this.dispatcher = dispatcher

    dispatcher
      .on('click', this, this.onClick)
      .on('dataReady', this, this.rememberData)
      .on('contextmenu', this, this.contextMenu)
  }

  // send click to server
  onClick (evt: MouseEvent) {
    const target = evt.target as HTMLElement;

    // if clicked on a span, send ajax call to server
    const type = target.getAttribute('data-arc-role');
    if (type) {
      const originSpanId = target.getAttribute('data-arc-origin');
      const targetSpanId = target.getAttribute('data-arc-target');
      this.dispatcher.post('ajax', [{
        action: 'selectArcForMerge',
        originSpanId,
        targetSpanId,
        id: target.getAttribute('data-arc-ed'),
        type
      }]);
    }

    const spanId = target.getAttribute('data-span-id');
    if (spanId) {
      const editedSpan = this.data.spans[spanId];
      this.dispatcher.post('ajax', [{
        action: 'selectSpanForMerge',
        id: spanId,
        type: editedSpan.type
      }]);
    }
  }

  contextMenu(evt: MouseEvent) {
    // If the user shift-right-clicks, open the normal browser context menu. This is useful
    // e.g. during debugging / developing
    if (evt.shiftKey) {
      return;
    }

    const target = evt.target as HTMLElement;
    let id: string | undefined;
    let type: string | undefined;

    if (target.getAttribute('data-arc-ed')) {
      id = target.getAttribute('data-arc-ed')!;
      type = target.getAttribute('data-arc-role')!;
    }

    if (target.getAttribute('data-span-id')) {
      id = target.getAttribute('data-span-id')!;
      type = this.data.spans[id].type;
    }

    if (id) {
      evt.preventDefault();
      this.dispatcher.post('ajax', [{
        action: 'contextMenu',
        id,
        type,
        clientX: evt.clientX,
        clientY: evt.clientY
      }]);
    }
  }

  /**
   * Remember data at initialization
   */
  rememberData (data: DocumentData) {
    if (data && !data.exception) {
      this.data = data
    }
  }
}

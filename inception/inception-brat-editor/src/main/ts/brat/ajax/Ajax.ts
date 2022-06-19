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

import { Dispatcher } from '../dispatcher/Dispatcher'

declare let Wicket

// vim:set ft=javascript ts=2 sw=2 sts=2 cindent:
export class Ajax {
  private pending = 0
  private count = 0
  private pendingList = {}
  private dispatcher: Dispatcher
  private ajaxUrl: string

  // We attach the JSON send back from the server to this HTML element
  // because we cannot directly pass it from Wicket to the caller in ajax.js.
  private wicketId: string

  constructor (dispatcher: Dispatcher, wicketId: string, ajaxUrl: string) {
    console.debug('Setting up brat ajax module...')

    this.dispatcher = dispatcher
    this.ajaxUrl = ajaxUrl
    this.wicketId = wicketId
    dispatcher
      .on('isReloadOkay', this, this.isReloadOkay)
      .on('ajax', this, this.ajaxCall)
  }

  // merge data will get merged into the response data
  // before calling the callback
  ajaxCall (data, callback: Function, merge) {
    merge = merge || {}
    this.pending++
    const id = this.count++

    // special value: `merge.keep = true` prevents obsolescence
    this.pendingList[id] = merge.keep || false
    delete merge.keep

    // If no protocol version is explicitly set, set it to current
    if (data.protocol === undefined) {
      // TODO: Extract the protocol version somewhere global
      data.protocol = 1
    }

    // WEBANNO EXTENSION BEGIN
    Wicket.Ajax.ajax({
      m: 'POST',
      c: this.wicketId,
      u: this.ajaxUrl,
      ep: data,
      // success
      sh: [() => {
        let response
        if (Wicket.$(this.wicketId) !== null) {
          response = Wicket.$(this.wicketId).temp
          delete Wicket.$(this.wicketId).temp
        }

        if (response === undefined) {
          console.log('Server response did not contain brat data - ignoring')
          // This is likely a wicket ajax-redirect and nothing that relates to brat.
          // We simply ignore this.
          return
        }
        // WEBANNO EXTENSION END

        this.pending--

        // If no exception is set, verify the server results
        if (response.exception === undefined && response.action !== data.action) {
          console.error(`Action ${data.action} returned the results of action ${response.action}`)
          response.exception = true
          this.dispatcher.post('messages', [[[`Protocol error: Action ${data.action} returned the results of action ${response.action} maybe the server is unable to run, please run tools/troubleshooting.sh from your installation to diagnose it`, 'error', -1]]])
        }

        // If the request is obsolete, do nothing; if not...
        if (Object.prototype.hasOwnProperty.call(this.pendingList, id)) {
          this.dispatcher.post('messages', [response.messages])
          delete this.pendingList[id]
          if (response.exception === true) {
            // if .exception is just Boolean true, do not process the callback;
          } else if (callback) {
            // if it is anything else, the callback is responsible for handling it
            $.extend(response, merge)
            this.dispatcher.postAsync(callback, [response])
          }
        }
        // WEBANNO EXTENSION BEGIN
      }],
      // error
      fh: [() => {
        this.pending--
        // TODO find some way or access or pass on the response, textStatus and errorThrown from BratAnnotator or Wicket.
        // In the original ajax.js, these are parameters to the error callback.
        //            dispatcher.post('messages', [[['Error: Action' + data.action + ' failed on error ' + response.statusText, 'error']]]);
        //            console.error(textStatus + ':', errorThrown, response);
      }]
    })
    // WEBANNO EXTENSION END
    return id
  }

  isReloadOkay () : boolean {
    // do not reload while data is pending
    return this.pending === 0
  }
}

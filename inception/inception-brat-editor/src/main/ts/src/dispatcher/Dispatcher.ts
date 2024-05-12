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

export type Message = 'dispatchAsynchError' | 'collectionLoaded' | 'requestRenderData' | 'messages'
  | 'startedRendering' | 'doneRendering' | 'dataReady'
  | 'ajax' | 'acceptButtonClicked' | 'rejectButtonClicked' | 'rerender'
  | 'svgWidth' | 'configurationUpdated' | 'newSourceData' | 'init' | 'click'
  | 'contextmenu' | 'isReloadOkay' | 'spanAndAttributeTypesLoaded'
  | 'keydown' | 'dragstart' | 'mousedown' | 'mouseup' | 'mousemove'
  | 'resize' | 'displaySpanButtons' | 'configurationChanged'
  | 'mouseover' | 'mouseout' | 'dblclick' | 'keypress' | 'touchstart' | 'touchend'
  | 'collectionChanged' | 'renderData' | 'renderDataPatch' | 'triggerRender'
  | 'resetData' | 'abbrevs' | 'textBackgrounds' | 'layoutDensity' | 'loadAnnotations'
  | 'selectionStarted' | 'selectionEnded';

export class Dispatcher {
  table : Record<string, [Object, Function][]> = {}

  on (message: Message, host: Object, handler: Function) {
    if (this.table[message] === undefined) {
      this.table[message] = []
    }
    this.table[message].push([host, handler])
    return this
  }

  // Notify listeners that we encountered an error in an asynch call
  inAsynchError = false // To avoid error avalanches
  handleAsynchError (e) {
    if (!this.inAsynchError) {
      this.inAsynchError = true
      // TODO: Hook printout into dispatch elsewhere?
      console.warn('Handled async error:', e)
      this.post('dispatchAsynchError', [e])
      this.inAsynchError = false
    } else {
      console.warn('Dropped asynch error:', e)
    }
  }

  postAsync (callback: Function | Message, args?) {
    if (args === undefined) {
      args = []
    }

    setTimeout(() => {
      try {
        if (typeof (callback) === 'function') {
          (callback as Function)(...args)
        } else {
          this.post(callback as Message, args)
        }
      } catch (e) {
        this.handleAsynchError(e)
      }
    }, 0)
  }

  post (message: Message, args?, returnType?: 'any' | 'all') {
    // console.trace(`brat dispacher processing ${message}`)

    if (args === undefined) {
      args = []
    }

    const results : unknown[] = []

    // a proper message, propagate to all interested parties
    const todo = this.table[message]
    if (todo !== undefined) {
      for (const [host, handler] of todo) {
        results.push(handler.apply(host, args))
      }
      /* DEBUG
                } else {
                  console.warn('Message ' + message + ' has no subscribers.'); // DEBUG
      */
    }

    if (returnType === 'any') {
      let i = results.length
      while (i--) {
        if (results[i] !== false) return results[i]
      }
      return false
    }

    if (returnType === 'all') {
      let i = results.length
      while (i--) {
        if (results[i] === false) return results[i]
      }
    }

    return results
  }
}

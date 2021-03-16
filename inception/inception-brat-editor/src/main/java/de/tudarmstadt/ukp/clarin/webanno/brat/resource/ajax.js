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
var Ajax = (function($, window, undefined) {
    var Ajax = function(dispatcher) {
      var that = this;
      var pending = 0;
      var count = 0;
      var pendingList = {};

      // merge data will get merged into the response data
      // before calling the callback
      var ajaxCall = function(data, callback, merge) {
        merge = merge || {};
        dispatcher.post('spin');
        pending++;
        var id = count++;

        // special value: `merge.keep = true` prevents obsolescence
        pendingList[id] = merge.keep || false;
        delete merge.keep;

        // If no protocol version is explicitly set, set it to current
        if (data['protocol'] === undefined) {
          // TODO: Extract the protocol version somewhere global
          data['protocol'] = 1;
        }

// WEBANNO EXTENSION BEGIN
        Wicket.Ajax.ajax({
          "m" : "POST",
          "c" : dispatcher.wicketId,
          "u" : dispatcher.ajaxUrl,
          "ep" : data,
          // success
          "sh" : [ function() {
        	var response = undefined;
        	if (Wicket.$(dispatcher.wicketId) !== null) {
	            response = Wicket.$(dispatcher.wicketId).temp;
	            delete Wicket.$(dispatcher.wicketId).temp;
        	}
            if (response === undefined) {
                console.log('Server response did not contain brat data - ignoring');
            	// This is likely a wicket ajax-redirect and nothing that relates to brat.
            	// We simply ignore this.
                return;
            }
// WEBANNO EXTENSION END
            pending--;
            // If no exception is set, verify the server results
            if (response.exception == undefined && response.action !== data.action) {
              console.error('Action ' + data.action +
                ' returned the results of action ' + response.action);
              response.exception = true;
              dispatcher.post('messages', [[['Protocol error: Action' + data.action + ' returned the results of action ' + response.action + ' maybe the server is unable to run, please run tools/troubleshooting.sh from your installation to diagnose it', 'error', -1]]]);
            }

            // If the request is obsolete, do nothing; if not...
            if (pendingList.hasOwnProperty(id)) {
              dispatcher.post('messages', [response.messages]);
              if (response.exception == 'configurationError'
                  || response.exception == 'protocolVersionMismatch') {
                // this is a no-rescue critical failure.
                // Stop *everything*.
                pendingList = {};
                dispatcher.post('screamingHalt');
                // If we had a protocol mismatch, prompt the user for a reload
                if (response.exception == 'protocolVersionMismatch') {
                  if(confirm('The server is running a different version ' +
                      'from brat than your client, possibly due to a ' +
                      'server upgrade. Would you like to reload the ' +
                      'current page to update your client to the latest ' +
                      'version?')) {
                    window.location.reload(true);
                  } else {
                    dispatcher.post('messages', [[['Fatal Error: Protocol ' +
                        'version mismatch, please contact the administrator',
                        'error', -1]]]);
                  }
                }
                return;
              }

              delete pendingList[id];

              // if .exception is just Boolean true, do not process
              // the callback; if it is anything else, the
              // callback is responsible for handling it
              if (response.exception == true) {
                $('#waiter').dialog('close');
              } else if (callback) {
                $.extend(response, merge);
                dispatcher.post(0, callback, [response]);
              }
            }
            dispatcher.post('unspin');
// WEBANNO EXTENSION BEGIN
          }],
          // error
          "fh" : [ function() {
            pending--;
            dispatcher.post('unspin');
            $('#waiter').dialog('close');
            // TODO find some way or access or pass on the response, textStatus and errorThrown from BratAnnotator or Wicket.
            // In the original ajax.js, these are parameters to the error callback.
//            dispatcher.post('messages', [[['Error: Action' + data.action + ' failed on error ' + response.statusText, 'error']]]);
//            console.error(textStatus + ':', errorThrown, response);
          }]});
// WEBANNO EXTENSION END
        return id;
      };

      var isReloadOkay = function() {
        // do not reload while data is pending
        return pending == 0;
      };

      var makeObsolete = function(all) {
        if (all) {
          pendingList = {};
        } else {
          $.each(pendingList, function(id, keep) {
            if (!keep) delete pendingList[id];
          });
        }
      }

      dispatcher.
          on('isReloadOkay', isReloadOkay).
          on('makeAjaxObsolete', makeObsolete).
          on('ajax', ajaxCall);
    };

    return Ajax;
})(jQuery, window);

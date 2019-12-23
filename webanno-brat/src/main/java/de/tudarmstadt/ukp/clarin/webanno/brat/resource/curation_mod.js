/*
 * Copyright 2014
 * Ubiquitous Knowledge Processing (UKP) Lab and FG Language Technology
 * Technische Universität Darmstadt
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
var CurationMod = (function($, window, undefined) {
  var CurationMod = function(dispatcher, svg) {
    var data;
    
    // send click to server
    var onClick = function(evt) {
      var target = $(evt.target);
      // if clicked on a span, send ajax call to server
      if (type = target.attr('data-arc-role')) {
          var originSpanId = target.attr('data-arc-origin');
          var targetSpanId = target.attr('data-arc-target');
          var id = target.attr('data-arc-ed');
          //var originSpan = data.spans[originSpanId];
          //var targetSpan = data.spans[targetSpanId];
          dispatcher.post('ajax', [ {
            action: 'selectArcForMerge',
            originSpanId: originSpanId,
            targetSpanId: targetSpanId,
            id: id,
            type: type
          }, 'serverResult']);
      }
      if (id = target.attr('data-span-id')) {
        var editedSpan = data.spans[id];
        dispatcher.post('ajax', [ {
          action: 'selectSpanForMerge',
          id: id,
          type: editedSpan.type,
        }, 'serverResult']);
      }
      // TODO check for arcs
    };
    
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
    
    // callback function which is called after ajax response has arrived
    var serverResult = function(response) {
      // dummy, probably not called at all
          if (response.exception) {
              // TODO: better response to failure
              dispatcher.post('messages', [[['Lookup error', 'warning', -1]]]);
              return false;
          }        
          alert(response);
    };
    
    // remember data at initialization
    var rememberData = function(_data) {
      if (_data && !_data.exception) {
        data = _data;
      }
    };
    
    // register events
    dispatcher.
      on('click', onClick).
      on('dataReady', rememberData).
      on('serverResult', serverResult).
      on('contextmenu', contextMenu);

  };

  return CurationMod;
})(jQuery, window);

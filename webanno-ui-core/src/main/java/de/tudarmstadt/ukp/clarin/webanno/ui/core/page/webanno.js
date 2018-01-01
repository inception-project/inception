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
$(document)
  .ready(
    function() {
      function hideBusysign() {
        document.getElementById('spinner').style.display = 'none';
      }

      function showBusysign() {
        document.getElementById('spinner').style.display = 'inline';
      }

      function clickFunc(eventData) {
        var clickedElement = (window.event) ? event.srcElement : eventData.target;
        if (clickedElement.parentNode && ((
            clickedElement.tagName.toUpperCase() == 'BUTTON'
            || (clickedElement.tagName.toUpperCase() == 'INPUT' && (
                clickedElement.type.toUpperCase() == 'BUTTON' 
                || clickedElement.type.toUpperCase() == 'SUBMIT'))
          )
          && clickedElement.parentNode.id.toUpperCase() != 'NOBUSY'))
        {
          showBusysign();
        }
      }

      document.getElementsByTagName('body')[0].onclick = clickFunc;
      hideBusysign();
      if (typeof Wicket != 'undefined') {
        Wicket.Event.subscribe('/ajax/call/beforeSend', function(
            attributes, jqXHR, settings) {
          showBusysign()
        });
        Wicket.Event.subscribe('/ajax/call/complete', function(
            attributes, jqXHR, textStatus) {
          hideBusysign()
        });
      }
    });


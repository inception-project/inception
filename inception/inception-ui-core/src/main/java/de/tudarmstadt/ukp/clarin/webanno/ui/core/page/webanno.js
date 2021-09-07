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
$(document)
  .ready(
    function() {
      function hideBusysign() {
        document.getElementById('spinner').style.display = 'none';
      }

      function showBusysign() {
        document.getElementById('spinner').style.display = 'inline';
      }
      
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
    
$( document ).ready(function() { 
  // Prevent the sticky dropdowns from closing on every click inside the dropdown
  $('.sticky-dropdown').each((i, e) => addEventListener('hide.bs.dropdown', function(e) {
    if (e.clickEvent && $(e.clickEvent.target).closest('.sticky-dropdown').length) {
      e.preventDefault();
    }
  }));
});
    

//wrap given script in try-catch block
function tryCatch(jsCall) {
	try {
		jsCall();
	} catch (e) {
		console.warn('Call terminated due to: ' + e + ', script was:\n' + String(jsCall) + '\n', e.stack);
	}
}
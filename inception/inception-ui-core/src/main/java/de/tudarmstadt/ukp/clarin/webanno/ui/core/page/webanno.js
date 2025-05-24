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
$( document ).ready(function() {
  function hideBusysign() {
    let spinner = document.getElementById('spinner')
    if (spinner) spinner.style.display = 'none';
  }

  function showBusysign() {
    let spinner = document.getElementById('spinner')
    if (spinner) spinner.style.display = 'inline';
  }
  
  function initializeStickyDropdowns() {
    // Prevent the sticky dropdowns from closing on every click inside the dropdown
    document.querySelectorAll('.sticky-dropdown, .k-popup').forEach(element => {
      element.addEventListener('hide.bs.dropdown', function(event) {
        if (event.clickEvent && event.target.closest('.sticky-dropdown, .k-popup')) {
          event.preventDefault();
        }
      });
    });
  }
  
  hideBusysign();
  initializeStickyDropdowns();
  if (typeof Wicket != 'undefined') {
    Wicket.Event.subscribe('/ajax/call/beforeSend', function(attributes, jqXHR, settings) {
      showBusysign();
    });
    Wicket.Event.subscribe('/ajax/call/complete', function(attributes, jqXHR, textStatus) {
      hideBusysign();
      initializeStickyDropdowns();
    });
  }
});

//wrap given script in try-catch block
function tryCatch(jsCall) {
	try {
		jsCall();
	} catch (e) {
		console.warn('Call terminated due to: ' + e + ', script was:\n' + String(jsCall) + '\n', e.stack);
	}
}
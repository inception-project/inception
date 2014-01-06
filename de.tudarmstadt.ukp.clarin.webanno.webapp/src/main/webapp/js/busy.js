/*******************************************************************************
 * Copyright 2012
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 ******************************************************************************/
// Reference: https://cwiki.apache.org/confluence/pages/viewpage.action?pageId=87917

	 window.onload = setupFunc;
	 
	 function setupFunc() {
	   document.getElementsByTagName('body')[0].onclick = clickFunc;
	   hideBusysign();
       Wicket.Ajax.registerPostCallHandler(hideBusysign);
       Wicket.Ajax.registerFailureHandler(hideBusysign);
	 }
	 
	 function hideBusysign() {
	   document.getElementById('bysy_indicator').style.display ='none';
	 }
	 
	 function showBusysign() {
	   document.getElementById('bysy_indicator').style.display ='inline';
	 }
	 
	 function clickFunc(eventData) {
	   var clickedElement = (window.event) ? event.srcElement : eventData.target;
	   if (clickedElement.tagName.toUpperCase() == 'A') {
	     showBusysign();
	   }
	 }
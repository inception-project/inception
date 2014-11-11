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
$(document).ready(function() {
  $(".top-sticker").each(function(index, sticker) {
    var shadow = $('<div></div>').insertBefore($(sticker));
    
    var initialPos = shadow.position();
    $(sticker).css("position", "fixed");
    
    function adjustSticker(e) {
        shadow.css("height", sticker.offsetHeight + "px");
        
        var pos = shadow.position();
        if (pos.top <= initialPos.top) {
            sticker.style.top = Math.max(0, pos.top - $(window).scrollTop()) + "px";
        }
        sticker.style.left = Math.max(0,pos.left-$(window).scrollLeft()) + "px"
    }
    
    $(document).on("scroll", adjustSticker);
    $(document).on("resize", adjustSticker);
    $(document).on("load", adjustSticker);
    adjustSticker();
  })
});

$(document).ready(function() {
  function hideBusysign() {
	document.getElementById('spinner').style.display ='none';
  }
		 
  function showBusysign() {
    document.getElementById('spinner').style.display ='inline';
  }
		 
  function clickFunc(eventData) {
    var clickedElement = (window.event) ? event.srcElement : eventData.target;
	if ((clickedElement.tagName.toUpperCase() == 'BUTTON' || 
			clickedElement.tagName.toUpperCase() == 'A' || 
			clickedElement.parentNode.tagName.toUpperCase() == 'A' || 
		(clickedElement.tagName.toUpperCase() == 'INPUT' && 
				(clickedElement.type.toUpperCase() == 'BUTTON' || 
						clickedElement.type.toUpperCase() == 'SUBMIT')))
		     && clickedElement.parentNode.id.toUpperCase() != 'NOBUSY' ) {
		     showBusysign();
    }
  }
  
  document.getElementsByTagName('body')[0].onclick = clickFunc;
  hideBusysign();
  Wicket.Event.subscribe('/ajax/call/beforeSend', function( attributes, jqXHR, settings ) {
    showBusysign()
  });
  Wicket.Event.subscribe('/ajax/call/complete', function( attributes, jqXHR, textStatus) {
    hideBusysign()
  });
});
	 

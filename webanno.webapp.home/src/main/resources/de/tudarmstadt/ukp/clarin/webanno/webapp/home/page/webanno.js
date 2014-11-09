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
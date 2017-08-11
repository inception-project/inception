/*!
 * jQuery scrollbarWidth - v0.2 - 2/11/2009
 * http://benalman.com/projects/jquery-misc-plugins/
 * 
 * Copyright (c) 2010 "Cowboy" Ben Alman
 * Dual licensed under the MIT and GPL licenses.
 * http://benalman.com/about/license/
 * 
 * ---
 * 
 * WebAnno: Changed to render test div outside screen area.
 */

// Calculate the scrollbar width dynamically!

(function($,undefined,width){
  '$:nomunge'; // Used by YUI compressor.
  
  $.scrollbarWidth = function() {
    var parent,
      child;
    
    if ( width === undefined ) {
      parent = $('<div style="position: absolute; top: -200px; width:50px;height:50px;overflow:auto"><div/></div>').appendTo('body');
      child = parent.children();
      width = child.innerWidth() - child.height( 99 ).innerWidth();
      parent.remove();
    }
    
    return width;
  };
  
})(jQuery);

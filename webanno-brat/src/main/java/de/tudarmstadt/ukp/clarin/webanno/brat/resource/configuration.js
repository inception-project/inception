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
var Configuration = (function(window, undefined) {
    var abbrevsOn = true;
    var textBackgrounds = "striped";
    var svgWidth = '100%';
    var rapidModeOn = false;
    var confirmModeOn = true;
    var autorefreshOn = false;
    
    var visual = {
      margin: { x: 2, y: 1 },
      arcTextMargin: 1,
      boxSpacing: 1,
      curlyHeight: 4,
      arcSpacing: 9, //10;
      arcStartHeight: 19, //23; //25;
    }

    return {
      abbrevsOn: abbrevsOn,
      textBackgrounds: textBackgrounds,
      visual: visual,
      svgWidth: svgWidth,
      rapidModeOn: rapidModeOn,
      confirmModeOn: confirmModeOn,
      autorefreshOn: autorefreshOn,
    };
})(window);
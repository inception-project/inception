/*!
 * jQuery scrollbarWidth - v0.2 - 2/11/2009
 * http://benalman.com/projects/jquery-misc-plugins/
 *
 * Copyright (c) 2010 "Cowboy" Ben Alman
 * Dual licensed under the MIT and GPL licenses.
 * http://benalman.com/about/license/
 *
 * Permission is hereby granted, free of charge, to any person
 * obtaining a copy of this software and associated documentation
 * files (the "Software"), to deal in the Software without
 * restriction, including without limitation the rights to use,
 * copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the
 * Software is furnished to do so, subject to the following
 * conditions:
 *
 * The above copyright notice and this permission notice shall be
 * included in all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND,
 * EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES
 * OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND
 * NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT
 * HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY,
 * WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING
 * FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR
 * OTHER DEALINGS IN THE SOFTWARE.
 *
 * ---
 *
 * WebAnno: Changed to render test div outside screen area.
 * INCEpTION: turned into a simple function
 */

// Calculate the scrollbar width dynamically!

let cachedScrollBarWidth: number | undefined

export function scrollbarWidth (): number {
  if (cachedScrollBarWidth !== undefined) {
    return cachedScrollBarWidth
  }

  const parent = $('<div style="position: absolute; top: -200px; width:50px;height:50px;overflow:auto"><div/></div>').appendTo('body')
  const child = parent.children()
  cachedScrollBarWidth = child.innerWidth() - child.height(99).innerWidth()
  parent.remove()
  return cachedScrollBarWidth
}

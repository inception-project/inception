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
 *
 * Derived under the conditions of the MIT license from below:
 *
 * Based on Annotator v.1.2.10 (built at: 26 Feb 2015)
 * http://annotatorjs.org
 *
 * Copyright 2015, the Annotator project contributors.
 * Dual licensed under the MIT and GPLv3 licenses.
 * https://github.com/openannotation/annotator/blob/master/LICENSE
 */
import $ from 'jquery';
import { findChild } from './xpath';

// Remants of I18N
export const _t = (msgid: string) => msgid;

// Public: Flatten a nested array structure
//
// Returns an array
export function flatten(array: any) {
  var flatten = function (ary: any) {
    let flat = [];

    for (let el of Array.from(ary)) {
      flat = flat.concat(el && $.isArray(el) ? flatten(el) : el);
    }

    return flat;
  };

  return flatten(array);
};


// Public: decides whether node A is an ancestor of node B.
//
// This function purposefully ignores the native browser function for this,
// because it acts weird in PhantomJS.
// Issue: https://github.com/ariya/phantomjs/issues/11479
export function contains(parent: any, child: any) {
  let node = child;
  while (node != null) {
    if (node === parent) { return true; }
    node = node.parentNode;
  }
  return false;
};

// Public: Finds all text nodes within the elements in the current collection.
//
// Returns a new jQuery collection of text nodes.
export function getTextNodes(jq: { map: (arg0: () => any) => any; }) {
  var getTextNodes = function (node: { nodeType: any; lastChild: any; previousSibling: any; }) {
    if (node && (node.nodeType !== Node.TEXT_NODE)) {
      const nodes = [];

      // If not a comment then traverse children collecting text nodes.
      // We traverse the child nodes manually rather than using the .childNodes
      // property because IE9 does not update the .childNodes property after
      // .splitText() is called on a child text node.
      if (node.nodeType !== Node.COMMENT_NODE) {
        // Start at the last child and walk backwards through siblings.
        node = node.lastChild;
        while (node) {
          nodes.push(getTextNodes(node));
          node = node.previousSibling;
        }
      }

      // Finally reverse the array so that nodes are in the correct order.
      return nodes.reverse();
    } else {
      return node;
    }
  };

  return jq.map(function () { return flatten(getTextNodes(this)); });
};

// Public: determine the last text node inside or before the given node
export function getLastTextNodeUpTo(n: { nodeType: any; lastChild: any; previousSibling: any; }) {
  switch (n.nodeType) {
    case Node.TEXT_NODE:
      return n; // We have found our text node.
    case Node.ELEMENT_NODE:
      // This is an element, we need to dig in
      if (n.lastChild != null) { // Does it have children at all?
        const result = getLastTextNodeUpTo(n.lastChild);
        if (result != null) { return result; }
      }
      break;
    default:
  }
  // Not a text node, and not an element node.
  // Could not find a text node in current node, go backwards
  n = n.previousSibling;
  if (n != null) {
    return getLastTextNodeUpTo(n);
  } else {
    return null;
  }
};

// Public: determine the first text node in or after the given jQuery node.
export function getFirstTextNodeNotBefore(n: { nodeType: any; firstChild: any; nextSibling: any; }) {
  switch (n.nodeType) {
    case Node.TEXT_NODE:
      return n; // We have found our text node.
    case Node.ELEMENT_NODE:
      // This is an element, we need to dig in
      if (n.firstChild != null) { // Does it have children at all?
        const result = getFirstTextNodeNotBefore(n.firstChild);
        if (result != null) { return result; }
      }
      break;
    default:
  }
  // Not a text or an element node.
  // Could not find a text node in current node, go forward
  n = n.nextSibling;
  if (n != null) {
    return getFirstTextNodeNotBefore(n);
  } else {
    return null;
  }
};

// Public: read out the text value of a range using the selection API
//
// This method selects the specified range, and asks for the string
// value of the selection. What this returns is very close to what the user
// actually sees.
export function readRangeViaSelection(range: { toRange: () => any; }) {
  const sel = getGlobal().getSelection(); // Get the browser selection object
  sel.removeAllRanges();                 // clear the selection
  sel.addRange(range.toRange());          // Select the range
  return sel.toString();                        // Read out the selection
};

export function nodeFromXPath(xp: { substring: (arg0: number) => { (): any; new(): any; split: { (arg0: string): any; new(): any; }; }; }, root: any) {
  const steps = xp.substring(1).split("/");
  let node = root;
  for (let step of Array.from(steps)) {
    let [name, idx] = Array.from((step as String).split("["));
    let idxN = (idx != null) ? parseInt((idx != null ? idx.split("]") : undefined)[0]) : 1;
    node = findChild(node, name.toLowerCase(), idxN);
  }

  return node;
};

export function escape(html: string) {
  return html
    .replace(/&(?!\w+;)/g, '&amp;')
    .replace(/</g, '&lt;')
    .replace(/>/g, '&gt;')
    .replace(/"/g, '&quot;');
}

export const uuid = (function () { let counter = 0; return () => counter++; })();

export const getGlobal = () => (function () { return this; })();

// Return the maximum z-index of any element in $elements (a jQuery collection).
export function maxZIndex($elements: any) {
  const all = Array.from($elements).map((el: any) =>
    // Use parseFloat since we may get scientific notation for large values.
    $(el).css('position') === 'static' ? -1 : parseFloat($(el).css('z-index')) || -1);
  return Math.max.apply(Math, all);
};

export function mousePosition(e: { pageY: number; pageX: number; }, offsetEl: any) {
  // If the offset element is not a positioning root use its offset parent
  let needle: any;
  if ((needle = $(offsetEl).css('position'), !['absolute', 'fixed', 'relative'].includes(needle))) {
    offsetEl = $(offsetEl).offsetParent()[0];
  }
  const offset = $(offsetEl).offset();
  return {
    top: e.pageY - offset.top,
    left: e.pageX - offset.left
  };
};

// Checks to see if an event parameter is provided and contains the prevent
// default method. If it does it calls it.
//
// This is useful for methods that can be optionally used as callbacks
// where the existence of the parameter must be checked before calling.
export const preventEventDefault = (event: any) => __guardMethod__(event, 'preventDefault', (o: { preventDefault: () => any; }) => o.preventDefault());

function __guard__(value: any, transform: { (x: any): any; (arg0: any): any; }) {
  return (typeof value !== 'undefined' && value !== null) ? transform(value) : undefined;
}
function __guardMethod__(obj: { [x: string]: any; }, methodName: string, transform: { (o: any): any; (arg0: any, arg1: any): any; }) {
  if (typeof obj !== 'undefined' && obj !== null && typeof obj[methodName] === 'function') {
    return transform(obj, methodName);
  } else {
    return undefined;
  }
}

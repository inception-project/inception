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
export function findChild(node: { hasChildNodes: () => any; childNodes: any; }, type: any, index: number) {
  if (!node.hasChildNodes()) {
    throw new Error("XPath error: node has no children!");
  }
  const children = node.childNodes;
  let found = 0;
  for (let child of Array.from(children)) {
    const name = getNodeName(child);
    if (name === type) {
      found += 1;
      if (found === index) {
        return child;
      }
    }
  }
  throw new Error("XPath error: wanted child not found.");
};

// Get the node name for use in generating an xpath expression.
export function getNodeName(node: { nodeName: { toLowerCase: () => any; }; }) {
  const nodeName = node.nodeName.toLowerCase();
  switch (nodeName) {
    case "#text": return "text()";
    case "#comment": return "comment()";
    case "#cdata-section": return "cdata-section()";
    default: return nodeName;
  }
};

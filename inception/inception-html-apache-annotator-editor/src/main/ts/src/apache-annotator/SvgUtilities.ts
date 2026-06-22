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

export const SVG_NS = 'http://www.w3.org/2000/svg';

/**
 * Create an SVG element with an optional class and attributes, stringifying numeric values.
 * Centralises the createElementNS + setAttribute + classList boilerplate.
 */
export function svgEl<K extends keyof SVGElementTagNameMap>(
    tag: K,
    className?: string,
    attrs: Record<string, string | number> = {}
): SVGElementTagNameMap[K] {
    const el = document.createElementNS(SVG_NS, tag) as SVGElementTagNameMap[K];
    if (className) el.classList.add(className);
    for (const [k, v] of Object.entries(attrs)) el.setAttribute(k, String(v));
    return el;
}

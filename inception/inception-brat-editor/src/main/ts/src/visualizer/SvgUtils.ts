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
import type { SVGTypeMapping } from '@svgdotjs/svg.js';
import type { Chunk } from './Chunk';
import type { Row } from './Row';

/**
 * Apply a fast translation to an SVG group by setting its `transform`
 * attribute directly.
 *
 * This helper writes the `transform` attribute on the underlying SVG group
 * element for minimal DOM overhead. It is intended for performance-sensitive
 * translations where using higher-level library calls may be slower.
 *
 * @param element - SVG group wrapper (e.g. an `SVG.js` group) to translate.
 * @param x - Horizontal translation in pixels.
 * @param y - Vertical translation in pixels.
 */
export function fastTranslateGroup(
    element: SVGTypeMapping<SVGGElement>,
    x: number,
    y: number
): void {
    element.attr('transform', 'translate(' + x + ', ' + y + ')');
    // element.translate(x, y);
}

/**
 * Quickly translate a `Row` or `Chunk` by applying a transform to its
 * SVG group and updating the element's cached `translation` property.
 *
 * This is a lightweight helper that calls `fastTranslateGroup` to set the
 * `transform` attribute directly for performance, then stores the provided
 * translation on the element to avoid subsequent DOM reads.
 *
 * @param element - Target `Row` or `Chunk` exposing a `group` (SVG group)
 *   and a `translation` field that will be updated.
 * @param x - Horizontal translation in pixels.
 * @param y - Vertical translation in pixels.
 */
export function fastTranslate(element: Row | Chunk, x: number, y: number): void {
    fastTranslateGroup(element.group, x, y);
    element.translation = { x, y };
}

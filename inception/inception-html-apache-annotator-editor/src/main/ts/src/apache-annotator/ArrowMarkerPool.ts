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

import { svgEl } from './SvgUtilities';

/**
 * Geometry of the triangular arrowhead {@link SVGMarkerElement}. The defaults describe a 7×7
 * user-space head whose tip sits at the path end, suitable for relation arcs.
 */
export interface ArrowMarkerGeometry {
    viewBox: string;
    refX: number;
    refY: number;
    width: number;
    height: number;
    orient: string;
    markerUnits: string;
    /** Path drawn inside the marker; it is filled with the relation color. */
    path: string;
}

export const DEFAULT_ARROW_MARKER_GEOMETRY: ArrowMarkerGeometry = {
    viewBox: '0 0 10 10',
    refX: 9,
    refY: 5,
    width: 7,
    height: 7,
    orient: 'auto-start-reverse',
    markerUnits: 'userSpaceOnUse',
    path: 'M 0 0 L 10 5 L 0 10 z'
};

/**
 * Lazily creates one arrowhead `<marker>` per distinct color inside a container element and
 * returns its id for use in `marker-start`/`marker-end`.
 *
 * Ids are minted from an internal counter rather than derived from the color string. A marker's
 * fill is fixed at creation, so two distinct colors that mapped to the same id would render one
 * with the other's arrowhead color. A sanitized-color id scheme (e.g. stripping non-alphanumeric
 * characters) is not injective — `rgb(10,2,30)` and `rgb(1,0,230)` collapse to the same key — so
 * we key the lookup on the raw color and let the counter guarantee unique ids.
 *
 * Intentionally local to the Apache Annotator editor: it only emits a single fixed `<path>` triangle
 * and is not yet general enough to serve the brat / pdf editors.
 */
export class ArrowMarkerPool {
    private readonly ids = new Map<string, string>();
    private seq = 0;

    constructor(
        private readonly container: SVGElement,
        private readonly idPrefix = 'iaa-arrowhead-',
        private readonly geometry: ArrowMarkerGeometry = DEFAULT_ARROW_MARKER_GEOMETRY
    ) {}

    /** Returns the id of the arrowhead marker for `color`, creating it on first request. */
    markerFor(color: string): string {
        const existing = this.ids.get(color);
        if (existing) return existing;

        const id = this.idPrefix + this.seq++;
        this.ids.set(color, id);

        const g = this.geometry;
        const marker = svgEl('marker', undefined, {
            id,
            viewBox: g.viewBox,
            refX: g.refX,
            refY: g.refY,
            markerWidth: g.width,
            markerHeight: g.height,
            orient: g.orient,
            markerUnits: g.markerUnits,
        });

        const tri = svgEl('path', undefined, { d: g.path, fill: color });
        marker.appendChild(tri);

        this.container.appendChild(marker);
        return id;
    }

    /**
     * Forgets all markers handed out so far. Call this after emptying the container so that ids
     * are reissued from scratch for the next render; it does not remove DOM nodes itself.
     */
    clear(): void {
        this.ids.clear();
        this.seq = 0;
    }
}

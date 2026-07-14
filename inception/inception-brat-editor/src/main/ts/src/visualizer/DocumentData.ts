/*
 * ## INCEpTION ##
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

import { Arc } from './Arc';
import { Chunk } from './Chunk';
import { EventDesc } from './EventDesc';
import { Fragment } from './Fragment';
import { Sizes } from './Sizes';
import { Entity } from './Entity';
import { Text as SVGText } from '@svgdotjs/svg.js';
import { Comment } from './Comment';
import { VID } from '../protocol/Protocol';

/**
 * Document data prepared for rendering. The JSON data we get from the server is converted into
 * this pre-rendering representation which already determines e.g. the draw orders and such but
 * which doesn't yet create the actual SVG representation.
 */
export class DocumentData {
    /** The full document text this data was built for. */
    text: string;

    /** The text split into chunks (roughly, tokens/whitespace-delimited pieces) used for layout. */
    chunks: Array<Chunk> = [];

    /** Span annotations (entities) indexed by their {@link VID}. */
    spans: Record<VID, Entity> = {};

    /** Arcs indexed by the id of the {@link EventDesc} they were derived from. */
    arcById: Record<VID, Arc> = {};

    /** All arcs (relations/event roles) to be rendered. */
    arcs: Array<Arc> = [];

    /**
     * Event descriptors indexed by event number. These are built both from actual events and
     * synthesized from relations (see {@code buildEventDescsFromRelations}) and carry the arc
     * label/color information.
     */
    eventDescs: Record<VID, EventDesc> = {};

    /** Comments attached to a whole sentence/row, indexed by sentence (row) number. */
    sentComment: Record<number, Comment> = {};

    /** Sentences/rows flagged for highlighting, indexed by sentence (row) number. */
    markedSent: Record<number, boolean> = {};

    /**
     * Template SVG text elements. Clone these and fill in any missing information (translate, fill)
     * before adding them to the SVG.
     */
    spanAnnTexts: Record<string, SVGText> = {};

    /**
     * Span fragments grouped into vertical stacks ("towers") by their {@code towerId}, so that
     * fragments sharing a tower are laid out at a consistent height.
     */
    towers: Record<string, Fragment[]> = {};

    /**
     * The order in which spans are drawn/stacked, as a permutation of span {@link VID}s produced by
     * {@code determineDrawOrder}.
     */
    spanDrawOrderPermutation: Array<VID> = [];

    /** Precomputed layout sizes (font metrics, box dimensions) used during rendering. */
    sizes: Sizes;

    /**
     * Set when the server response represents an error rather than renderable data; the UI checks
     * this and skips rendering when true.
     */
    exception = false;

    /**
     * The trailing y after the last row was placed by {@code renderRows}, i.e. the bottom edge of
     * the laid-out content in SVG user units. The brat SVG is rendered with {@code viewBox} height
     * equal to its pixel height (see {@code Visualizer.renderData}), so the vertical user->pixel
     * scale is 1 and this value is directly comparable to viewport pixel measurements. Undefined
     * until the first render completes.
     */
    contentHeight?: number;

    constructor(text: string) {
        this.text = text;
        // Object.seal(this)
    }
}

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
 */

/**
 * Find the nearest ancestor element that is horizontally scrollable.
 *
 * Walks up the DOM from `node` and returns the closest ancestor that is an
 * `HTMLElement` whose horizontal overflow indicates a scrollable container:
 * either `overflow-x: scroll` or `overflow-x: auto` and `scrollWidth > clientWidth`.
 * The search stops at the document root (`<html>`) or when encountering an
 * element that has the `scrollable` class but currently does not show a
 * scrollbar (this avoids choosing containers that are marked scrollable but
 * not active).
 *
 * @param node - Starting element for the search. May be `null`.
 * @returns The closest horizontally scrollable `HTMLElement`, or `null` if
 *          none is found.
 */
export function findClosestHorizontalScrollable(node: Element | null) {
    if (node === null || node.tagName === 'HTML') {
        return null;
    }

    if (
        node instanceof HTMLElement &&
        ((node.style.overflowX === 'auto' && node.scrollWidth > node.clientWidth) ||
            node.style.overflowX === 'scroll')
    ) {
        return node;
    }

    // Abort if the node is marked as scrollable but does presently not have a scrollbar. This is
    // to avoid that we consider scrollbars too far out
    if (node.classList.contains('scrollable')) {
        return null;
    }

    return findClosestHorizontalScrollable(node.parentElement);
}

/**
 * Find the nearest ancestor element that is vertically scrollable.
 *
 * Walks up the DOM from `node` and returns the closest ancestor that is an
 * `HTMLElement` whose vertical overflow indicates a scrollable container:
 * either `overflow-y: scroll` or `overflow-y: auto` and `scrollHeight > clientHeight`.
 * The search stops at the document root (`<html>`) or when encountering an
 * element that has the `scrollable` class but currently does not show a
 * scrollbar.
 *
 * @param node - Starting element for the search. May be `null`.
 * @returns The closest vertically scrollable `HTMLElement`, or `null` if
 *          none is found.
 */
export function findClosestVerticalScrollable(node: Element | null) {
    if (node === null || node.tagName === 'HTML') {
        return null;
    }

    if (
        node instanceof HTMLElement &&
        ((node.style.overflowY === 'auto' && node.scrollHeight > node.clientHeight) ||
            node.style.overflowY === 'scroll')
    ) {
        return node;
    }

    // Abort if the node is marked as scrollable but does presently not have a scrollbar. This is
    // to avoid that we consider scrollbars too far out
    if (node.classList.contains('scrollable')) {
        return null;
    }

    return findClosestVerticalScrollable(node.parentElement);
}

/**
 * Utility function to find the closest highlight element to the given target.
 *
 * @param target a DOM node.
 * @returns the closest highlight element or null if none is found.
 */
export function findClosestChunkElement(target: Node | null): Element | null {
    if (target instanceof Text) {
        target = target.parentElement;
    }

    if (target instanceof Element) {
        return target.closest('[data-chunk-id]');
    }

    return null;
}

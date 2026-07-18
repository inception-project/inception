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
import type { DocumentStructureStrategy } from './DocumentStructureStrategy';

export interface TocLevel {
    parent?: TocLevel;
    label?: string;
    title?: string;
    key?: string;
    element?: Element;
    tocElement?: Element;
    children: TocLevel[];
}

export function generateTOCIndex(root: TocLevel): Record<string, TocLevel> {
    const result: Record<string, TocLevel> = {};
    _generateTOCIndex(root, result);
    return result;
}

function _generateTOCIndex(root: TocLevel, result: Record<string, TocLevel>): void {
    if (!root) return;
    if (root.element && root.element.id) {
        result[root.element.id] = root;
    }
    if (root.children?.length > 0) {
        for (const child of root.children) {
            _generateTOCIndex(child, result);
        }
    }
}

/**
 * Index the TOC by structural key (TocLevel.key) rather than by DOM id. Used
 * by by-key scroll sync to look up the section in a peer document that matches
 * a given key. Only sections with a defined key are indexed -- keyless
 * sections are intentionally absent, so a lookup miss signals "no matching
 * key" and the caller degrades to fractional sync.
 *
 * If two sections share a key (which explicit source keys should prevent, but
 * a weaker positional fallback key may not), the first one encountered in
 * document order wins.
 */
export function generateTOCKeyIndex(root: TocLevel): Record<string, TocLevel> {
    const result: Record<string, TocLevel> = {};
    _generateTOCKeyIndex(root, result);
    return result;
}

function _generateTOCKeyIndex(root: TocLevel, result: Record<string, TocLevel>): void {
    if (!root) return;
    if (root.key !== undefined && !(root.key in result)) {
        result[root.key] = root;
    }
    if (root.children?.length > 0) {
        for (const child of root.children) {
            _generateTOCKeyIndex(child, result);
        }
    }
}

export function generateTOC(
    rootElement: Element,
    selector: string,
    extractTitle: (section: Element) => string | undefined,
    extractKey?: (section: Element) => string | undefined
): TocLevel {
    const tocRoot: TocLevel = { element: rootElement, children: [] };
    const walker = rootElement.ownerDocument.createTreeWalker(
        rootElement,
        NodeFilter.SHOW_ELEMENT,
        {
            acceptNode: (node) =>
                (node as Element).matches(selector)
                    ? NodeFilter.FILTER_ACCEPT
                    : NodeFilter.FILTER_SKIP,
        }
    );

    const stack: TocLevel[] = [tocRoot];
    let currentNode = walker.nextNode() as Element | null;
    while (currentNode) {
        let currentLevel = stack[stack.length - 1];

        const newLevel: TocLevel = {
            parent: currentLevel,
            title: extractTitle(currentNode),
            key: extractKey?.(currentNode),
            element: currentNode,
            children: [],
        };

        while (currentLevel.element && !currentLevel.element.contains(currentNode)) {
            stack.pop();
            currentLevel = stack[stack.length - 1];
            newLevel.parent = currentLevel;
        }

        if (newLevel.title) {
            currentLevel.children.push(newLevel);
            stack.push(newLevel);
        }

        currentNode = walker.nextNode() as Element | null;
    }
    return tocRoot;
}

/**
 * Build the TOC for a document from a {@link DocumentStructureStrategy},
 * wiring the strategy's per-section accessors (title + key) into
 * {@link generateTOC}. This is the single place that composes a strategy with
 * the generic tree builder, so callers that own a strategy (the structure
 * navigator, and editors that also need the tree for scroll sync) do not each
 * repeat the callback wiring.
 */
export function buildDocumentStructure(
    container: Element,
    structure: DocumentStructureStrategy
): TocLevel {
    return generateTOC(
        container,
        structure.sectionSelector,
        (s) => structure.extractTitle(s),
        (s) => structure.extractKey(s)
    );
}

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

/**
 * Format-specific behaviour needed to derive a document outline. The
 * navigator code itself is generic; each format (HTML, TEI, JATS, ...)
 * supplies an instance of this interface.
 *
 * Implementations must NOT change the document's text content, the order in
 * which text appears in document order, or the character offsets of any
 * existing text node, because annotation offsets are pinned to those.
 */
export interface DocumentStructureStrategy {
    /**
     * CSS selector matching every section element in the document after
     * preprocess() has run.
     */
    readonly sectionSelector: string;

    /**
     * Optional DOM transformation that manifests section boundaries so the
     * tree matches sectionSelector. For HTML this wraps heading-delimited
     * regions; for formats with explicit structure (TEI, JATS) it is a
     * no-op.
     */
    preprocess(container: Element): void;

    /**
     * Display label for a section in the TOC.
     */
    extractTitle(section: Element): string | undefined;

    /**
     * The element to scroll into view when the user clicks a TOC entry.
     * Usually the heading/title inside the section. Falls back to the
     * section itself.
     */
    scrollTarget(section: Element): Element;
}

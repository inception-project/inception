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

/*
 * Shared reference handling utilities for AssistantPanel.
 */

import type { MChatMessage, MReference } from "./AssistantPanelModels";

export const refIdReplacementPattern = /\s*{{ref::([\w-]+)}}(\.*)/g;
export const docIdReplacementPattern = /\s*[Dd]ocument[\s,]+([0-9a-f]{8})(\.*)/g;

export function stripRefs(text: string): string {
    if (!text) return text;
    return text.replace(refIdReplacementPattern, "").replace(docIdReplacementPattern, "");
}

function escapeXML(str: string): string {
    return str.replace(/[<>&'"]/g, (char) => {
        switch (char) {
            case "<":
                return "&lt;";
            case ">":
                return "&gt;";
            case "&":
                return "&amp;";
            case "'":
                return "&apos;";
            case '"':
                return "&quot;";
            default:
                return char;
        }
    });
}

export function formatReferenceTitle(reference: MReference): string {
    const hasValidScore = Number.isFinite(reference.score);
    return hasValidScore
        ? `${escapeXML(reference.documentName)} (score: ${reference.score.toFixed(4)})`
        : escapeXML(reference.documentName);
}

/**
 * Replace references in HTML text with badge links for the UI.
 * If `pattern` is omitted, both id-refs and document refs are processed.
 */
export function replaceReferencesWithHtmlLinks(message: MChatMessage, text: string, pattern?: RegExp): string {
    if (!text) return text;

    const applyPattern = (pat: RegExp, src: string) =>
        src.replace(pat, (match: string, refId: string, dots: string) => {
            const refSelector = (ref: MReference) => ref.id === refId;
            const reference = (message.references || []).find(refSelector);
            const refNum = (message.references || []).findIndex(refSelector) + 1;

            if (reference) {
                const title = formatReferenceTitle(reference);
                return `${dots}<span class="reference badge rounded-pill text-bg-secondary mx-1" data-msg="${message.id}" data-ref="${reference.id}" title="${title}">${refNum}</span>`;
            }

            // If reference doesn't exist, filter it out (keep only the dots if any)
            return dots || "";
        });

    if (pattern) {
        return applyPattern(pattern, text);
    }

    // Default: apply both patterns in order
    let out = applyPattern(refIdReplacementPattern, text);
    out = applyPattern(docIdReplacementPattern, out);
    return out;
}

export function findReferencesInText(text: string): Array<{ type: "ref" | "doc"; id: string; match: string }> {
    const results: Array<{ type: "ref" | "doc"; id: string; match: string }> = [];
    let m: RegExpExecArray | null;
    const r1 = new RegExp(refIdReplacementPattern);
    while ((m = r1.exec(text))) {
        results.push({ type: "ref", id: m[1], match: m[0] });
    }
    const r2 = new RegExp(docIdReplacementPattern);
    while ((m = r2.exec(text))) {
        results.push({ type: "doc", id: m[1], match: m[0] });
    }
    return results;
}

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

import type { MTextMessage } from "./AssistantPanelModels";
import { stripRefs } from "./AssistantPanelReferences";
import { marked } from "marked";

/**
 * Convert Markdown to plain text using a lightweight marked renderer.
 */
export function stripMarkdown(text: string): string {
    if (!text) return text;
    // Parse markdown to HTML. We do not use the async option, so cast to string.
    const html = marked.parse(text) as string;

    // If running in a browser or jsdom-like environment, convert HTML to a
    // DOM fragment and use its textContent for accurate whitespace handling.
    try {
        if (typeof document !== "undefined" && typeof document.createRange === "function") {
            const frag = document.createRange().createContextualFragment(html);
            const txt = frag.textContent || "";
            return txt.replace(/\s+/g, " ").trim();
        }
    } catch (e) {
        // Log the exception before falling back so we have visibility in browsers
        // or test environments when DOM parsing fails.
        // eslint-disable-next-line no-console
        console.warn("stripMarkdown: DOM parsing failed, falling back to tag-stripping.", e);
    }

    // Fallback: strip tags from HTML
    let out = html.replace(/<[^>]*>/g, "");
    out = out.replace(/\s+/g, " ").trim();
    return out;
}

/**
 * Manages speech synthesis for assistant messages.
 * Buffers text and enqueues utterances for sentence-by-sentence or line-by-line playback.
 */
export class SpeechManager {
    // Buffer that stores the incoming raw text chunks until we decide to
    // create an utterance. Name `utteranceBuffer` kept for familiarity; it
    // contains the unstripped (raw) content until `stripRefs()` is applied
    // right before enqueuing.
    private utteranceBuffer = "";
    private utteranceQueue: SpeechSynthesisUtterance[] = [];
    private isSpeaking = false;
    private enabled: () => boolean;

    constructor(enabled: () => boolean) {
        this.enabled = enabled;
    }

    /**
     * Check if speech synthesis is available in the browser.
     */
    isAvailable(): boolean {
        return "speechSynthesis" in window;
    }

    /**
     * Process an incoming text message for speech synthesis.
     * Buffers content and speaks complete sentences or lines.
     */
    speak(msg: MTextMessage): void {
        // Accumulate raw (unstripped) buffer so we can reason about unfinished refs
        this.utteranceBuffer += msg.content;

        if (msg.done) {
            this.enqueueUtterance(this.utteranceBuffer);
            this.utteranceBuffer = "";
            return;
        }

        // Speak when sentence seems complete (we don't handle abbreviations)
        const trimmedRaw = this.utteranceBuffer.trimEnd();

        // Determine the last *significant* character by skipping trailing
        // Markdown/formatting characters (e.g. '*', '_', '`', closing parens,
        // quotes) so that constructs like '*Yes.*' are recognized as ending
        // with '.' rather than '*'.
        const formattingTrail = new Set(["*", "_", "`", '"', "'", "’", ")", "]", "}"]);
        let i = trimmedRaw.length - 1;
        while (i >= 0 && formattingTrail.has(trimmedRaw.charAt(i))) {
            i--;
        }
        const lastChar = i >= 0 ? trimmedRaw.charAt(i) : "";

        if ([".", "!", "?", ";", ":"].includes(lastChar)) {
            // Detect unfinished reference markers in the raw (pre-stripped) buffer
            const hasUnclosedRef = /{{[^}]*$/.test(this.utteranceBuffer);
            if (lastChar === ":" && hasUnclosedRef) {
                // Defer speaking until the reference is complete
            } else {
                this.enqueueUtterance(this.utteranceBuffer);
                this.utteranceBuffer = "";
            }
        } else {
            // Speak line by line using the raw buffer
            const lineBreak = this.utteranceBuffer.indexOf("\n");
            if (lineBreak > 0) {
                const segment = this.utteranceBuffer.substring(0, lineBreak + 1);
                this.enqueueUtterance(segment);
                this.utteranceBuffer = this.utteranceBuffer.substring(lineBreak + 1);
            }
        }
    }

    /**
     * Initialize the utterance buffer for a new message.
     * Called when a new assistant message starts.
     */
    initializeBuffer(content: string): void {
        this.utteranceBuffer = content;
    }

    /**
     * Cancel current speech synthesis and clear the queue.
     */
    cancel(): void {
        speechSynthesis.cancel();
        this.utteranceQueue = [];
        this.utteranceBuffer = "";
        this.isSpeaking = false;
    }

    private enqueueUtterance(text: string): void {
        if (!this.enabled()) {
            return;
        }

        // Strip reference markers and Markdown at the last moment before creating the utterance
        let cleaned = stripRefs(text);
        cleaned = stripMarkdown(cleaned);
        this.utteranceQueue.push(new SpeechSynthesisUtterance(cleaned));
        this.processUtteranceQueue();
    }

    private processUtteranceQueue(): void {
        if (this.isSpeaking) {
            console.debug("Speech synthesis is already in progress.");
            return;
        }

        if (this.utteranceQueue.length === 0) {
            console.debug("Speech synthesis queue is empty.");
            return;
        }

        const utterance = this.utteranceQueue.shift();
        this.isSpeaking = true;

        // Assign onend before calling speak to ensure we don't miss synchronous
        // or very-short utterance callbacks (some environments may invoke onend
        // synchronously for empty/short texts or in tests).
        utterance.onend = () => {
            this.isSpeaking = false;
            this.processUtteranceQueue();
        };

        console.info("Speaking: " + utterance.text);
        speechSynthesis.speak(utterance);
    }
}

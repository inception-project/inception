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
import type { Offsets } from '../model/Offsets';

/**
 * Vertical scroll position: where the top edge of an editor's viewport currently falls in the
 * document, expressed layout-independently along the one-dimensional character-offset axis (there
 * is no x/y here - only vertical scroll). The position is an anchor element the viewport top cuts
 * through, given as the character-offset interval {@code begin..end}, plus {@code fraction}: how
 * far the viewport top has progressed through that interval. Together they denote a fractional
 * character offset {@code begin + fraction*(end-begin)}.
 */
export type ViewportScrollPosition = {
    /** Document character offset at which the topmost visible anchor element begins. */
    begin: number;
    /** End offset of the anchor element, so a receiver can size the anchor. */
    end: number;
    /** Progress (0..1) of the viewport top through the anchor element. */
    fraction: number;
    /**
     * The source's overall scroll progress: scrollTop / maxScroll, in [0,1] (0 = at the very top,
     * 1 = at the very bottom).
     */
    scrollProgress?: number;
};

/**
 * Input accepted by {@link AnnotationEditor.scrollToViewportPosition}. A relaxation of
 * {@link ViewportScrollPosition} in which {@code end} is optional: the receiver only needs
 * {@code begin} + {@code fraction} to align its viewport and defaults {@code end} to {@code begin}
 * when absent. A {@link ViewportScrollPosition} is assignable here, so producers can forward one
 * directly.
 */
export type ViewportScrollTarget = {
    /** Document character offset at which the anchor element begins. */
    begin: number;
    /** End offset of the anchor element. Defaults to {@code begin} when omitted. */
    end?: number;
    /** Progress (0..1) of the viewport top through the anchor element. */
    fraction: number;
    /**
     * The source's overall scroll progress: scrollTop / maxScroll, in [0,1] (0 = at the very top,
     * 1 = at the very bottom).
     */
    scrollProgress?: number;
};

/**
 * The peer-facing view of the host-page scroll-sync hub: the subset an editor uses to add and
 * remove itself as a sync peer. Kept minimal (and defined here rather than importing the concrete
 * hub) so an editor can register without depending on the external-editor module that owns the hub.
 */
export interface ViewportSyncPeer {
    /**
     * Register {@code aEditor} as a scroll-sync peer under {@code aId}. The editor passes the element
     * whose scrolling moves its viewport as {@code aScope} - the hub listens on that element's owner
     * document and uses it to tell the editor's own scroll apart from unrelated scrolls of nested
     * scrollers sharing the same document. Omit {@code aScope} when the editor scrolls its whole
     * document.
     */
    register(aId: string, aEditor: AnnotationEditor, aScope?: Element): void;

    /** Remove the peer previously registered under {@code aId}, detaching its scroll listener. */
    unregister(aId: string): void;
}

export interface AnnotationEditor {
    loadAnnotations(): void;

    /**
     * Editor should scroll to the given offset. The offset is given in relation to the entire
     * doocument, not to the viewport. So if the editor is showing only a part of the document in
     * its viewport, the offset should be adjusted accordingly.
     */
    scrollTo(args: { offset: number; position?: string; pingRanges?: Offsets[] }): void;

    /**
     * Report the vertical scroll position currently at the top of the viewport. May return null when the
     * position cannot be determined (e.g. before initialization has completed).
     */
    getViewportScrollPosition?(): ViewportScrollPosition | null;

    /**
     * Place the viewport top at the given anchor offset plus intra-anchor fraction. The
     * editor resolves the offset in its own layout and clamps to its own scroll bounds.
     * <p>
     * The anchor + fraction denote a fractional character offset (begin + fraction*(end-begin)) at
     * the viewport top. Interpolating in character space rather than pixels keeps the sync stable
     * when the two editors wrap the same text to different heights - each side converts that offset
     * to a pixel position with its own layout. {@code end} defaults to {@code begin} if omitted.
     */
    scrollToViewportPosition?(pos: ViewportScrollTarget): void;

    /**
     * Add this editor to the scroll-sync {@code aHub} under {@code aId} (a host-page markup id chosen
     * by the backend, which drives linking). The editor registers itself - rather than the hub or
     * the factory reaching in - because it owns its layout and so knows which of the elements it owns
     * actually scrolls; it passes that element to {@link ViewportSyncPeer.register} as the scope.
     * The editor should call this once its viewport is built (so the scroll container exists), and
     * re-run it if the container is rebuilt. Implementations remember {@code aHub}/{@code aId} so
     * {@link disconnectViewportSync} can undo it.
     */
    connectViewportSync?(aHub: ViewportSyncPeer, aId: string): void;

    /** Remove this editor from the hub it was connected to via {@link connectViewportSync}. */
    disconnectViewportSync?(): void;

    destroy(): void;
}

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
import { type AnnotationEditor } from '@inception-project/inception-js-api';

/**
 * Suppress scroll echoing of recieving peers for this many milliseconds after a scroll event is received to avoid
 * events echoing back and forth between two editors.
 */
const SUPPRESS_ECHO_MS = 250;

/**
 * A registered editor participating in scroll synchronization.
 */
type PeerEditor = {
    id: string;
    editor: AnnotationEditor;
    scrollTarget: EventTarget;
    listener: (event: Event) => void;
    /**
     * The element whose scrolling moves this editor's viewport, or undefined if the editor scrolls
     * its whole document.
     */
    scope?: Element;
    rafHandle?: number;
    suppressUntil: number;
};

/**
 * Host-page hub that mirrors scroll positions between the annotation editors on a page.
 *
 * Editors are registered under the markup id of their host element as they initialize.
 */
export class ViewportSyncHub {
    private peers = new Map<string, PeerEditor>();
    private links = new Map<string, string>();

    /**
     * Register an editor as a scroll-sync peer under the given (host-page markup) id. When {@code aScope}
     * is omitted the editor's whole document is treated as the viewport.
     */
    register(aId: string, aEditor: AnnotationEditor, aScope?: Element): void {
        if (!aId) return;

        this.unregister(aId);

        const scrollTarget: EventTarget = aScope?.ownerDocument ?? aScope ?? document;
        const peer: PeerEditor = {
            id: aId,
            editor: aEditor,
            scrollTarget,
            scope: aScope,
            listener: (event: Event) => this.onScroll(peer, event),
            suppressUntil: 0,
        };
        scrollTarget.addEventListener('scroll', peer.listener, {
            capture: true,
            passive: true,
        });
        this.peers.set(aId, peer);
        console.debug(`[ViewportSyncHub] Registered editor [${aId}]`);
    }

    unregister(aId: string): void {
        const peer = this.peers.get(aId);
        if (!peer) return;

        peer.scrollTarget.removeEventListener('scroll', peer.listener, { capture: true });
        if (peer.rafHandle !== undefined) cancelAnimationFrame(peer.rafHandle);
        this.peers.delete(aId);
        console.debug(`[ViewportSyncHub] Unregistered editor [${aId}]`);
    }

    /** Make the two editors track each other's scrolling. Replaces any existing links of either. */
    link(aId: string, aOtherId: string): void {
        if (!aId || !aOtherId || aId === aOtherId) return;

        this.unlink(aId);
        this.unlink(aOtherId);
        this.links.set(aId, aOtherId);
        this.links.set(aOtherId, aId);
        console.debug(`[ViewportSyncHub] Linked editors [${aId}] and [${aOtherId}]`);
    }

    unlink(aId: string): void {
        const partner = this.links.get(aId);
        if (partner === undefined) return;

        this.links.delete(aId);
        if (this.links.get(partner) === aId) {
            this.links.delete(partner);
        }
        console.debug(`[ViewportSyncHub] Unlinked editor [${aId}]`);
    }

    private onScroll(aPeer: PeerEditor, aEvent: Event): void {
        // Ignore scrolls of other editors sharing the same (captured) document - only scrolls of
        // this peer's own scroll container (or a scroller nested inside it) count. The scope is
        // that container, so the container's own scroll event has target === scope, which
        // `contains` accepts (an element contains itself). scroll targets a Document when the whole
        // page/iframe scrolls, in which case there is no scoping to do.
        if (aPeer.scope) {
            const target = aEvent.target;
            if (target instanceof Node && !aPeer.scope.contains(target)) return;
        } else {
            // Scope-less peers - the editor did not declare a scroll container, so its viewport is
            // the document itself (e.g. an XML document scrolling as a whole inside its iframe). The
            // capture-phase listener also sees scrolls of scrollers nested inside the document (a
            // scrollable table, <pre>, resize box); those don't move the editor's viewport, so
            // mirroring them is at best wasted work and at worst a spurious position. Only the
            // document's own scroll - target is the Document or its root scrolling element - counts.
            // Editors whose viewport is an inner wrapper (brat, Apache Annotator) declare that
            // wrapper via getScrollContainer() and take the scoped branch above instead.
            const target = aEvent.target;
            const doc = aPeer.scrollTarget as Document;
            const root = doc.scrollingElement ?? doc.documentElement;
            if (target !== doc && target !== root) return;
        }

        // Coalesce the high-frequency scroll events into one emission per animation frame
        if (aPeer.rafHandle !== undefined) return;

        aPeer.rafHandle = requestAnimationFrame(() => {
            aPeer.rafHandle = undefined;

            if (performance.now() < aPeer.suppressUntil) return;

            const partnerId = this.links.get(aPeer.id);
            if (partnerId === undefined) return;

            const partner = this.peers.get(partnerId);
            if (!partner || !partner.editor.scrollToViewportPosition) return;

            const pos = aPeer.editor.getViewportScrollPosition?.();
            if (!pos) return;

            partner.suppressUntil = performance.now() + SUPPRESS_ECHO_MS;
            partner.editor.scrollToViewportPosition({
                begin: pos.begin,
                end: pos.end,
                fraction: pos.fraction,
                scrollProgress: pos.scrollProgress,
            });
        });
    }
}

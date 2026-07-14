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
import {
    calculateStartOffset,
    offsetToRange,
    positionToOffset,
    type DiamAjax,
} from '@inception-project/inception-js-api';
import './KeyboardEditorMode.scss';
import { annotatorState } from './ApacheAnnotatorState.svelte';
import { getSafeRangeForSelection, nodeToElement, rangeClientRect } from './Utilities';
import * as support from './KeyboardEditorMode.support';
import type { ApacheAnnotatorSelector } from './ApacheAnnotatorSelector';

export class KeyboardEditorMode {
    private root: Element;
    private documentContainer: HTMLElement;
    private ajax: DiamAjax;
    private protectedElementsMatcher?: (el: Element) => boolean;
    private selector?: ApacheAnnotatorSelector;

    private keyboardHandlerCleanup: (() => void) | undefined = undefined;
    private contentEditableCleanup: (() => void) | undefined = undefined;
    // Absolute offset of the last collapsed caret known to sit OUTSIDE any protected
    // element. Used as the travel-direction origin when the caret subsequently lands
    // inside a protected element, so it can be skipped to the correct (far) edge
    // without depending on which key was pressed.
    private lastSafeCaretOffset: number | null = null;
    // The selection we placed deliberately (a click or a programmatic jump). The
    // selection sanitizer skips exactly this change, matched by the selection's
    // identity, so a deliberate caret placement is never snapped away. This replaces
    // an earlier timer-based skip that raced the (asynchronous) selectionchange event.
    private programmaticSelection: { node: Node; offset: number } | null = null;
    // Where to place the caret when focus returns to the document (Escape from a
    // feature editor, or reclaiming orphaned focus after a deletion). Set to the end
    // of a just-created annotation, or to the caret position from before a
    // select/delete; whichever happened most recently wins.
    private rememberedCaretOffset: number | null = null;

    // Custom caret overlay shown while keyboard mode is active (the native caret is
    // hidden) so the editing position is easier to spot in dense text.
    private caretOverlay?: HTMLElement;
    private caretOverlayCleanup?: () => void;
    private lastCaretRectKey: string | null = null;
    private caretMoveTimer?: number;
    private caretOverlayRaf?: number;

    public constructor(
        root: Element,
        documentContainer: HTMLElement,
        ajax: DiamAjax,
        protectedElementsMatcher?: (el: Element) => boolean,
        selector?: ApacheAnnotatorSelector
    ) {
        this.root = root;
        this.documentContainer = documentContainer;
        this.ajax = ajax;
        this.protectedElementsMatcher = protectedElementsMatcher;
        this.selector = selector;
    }

    public enable(enable: boolean) {
        // Remove existing handlers first so a repeated enable(true) does not stack a
        // second set of listeners. The content-editable handlers in particular would
        // otherwise leak and never reset contentEditable back to 'false', so tear them
        // down unconditionally here (not only on the !enable path) before re-enabling.
        if (this.keyboardHandlerCleanup) {
            this.keyboardHandlerCleanup();
            this.keyboardHandlerCleanup = undefined;
        }
        this.disableCaretOverlay();
        this.disableContentEditableMode();

        if (!enable) {
            return;
        }

        this.documentContainer.tabIndex = 0;

        this.documentContainer.focus();
        const doc = this.root.ownerDocument;
        const sel = doc.getSelection();
        const contentRoot = this.documentContainer;
        if (sel) {
            const withinRoot = !!sel.anchorNode && contentRoot.contains(sel.anchorNode as Node);
            if (sel.rangeCount === 0 || !withinRoot) {
                const firstOffset = this.findFirstWordStartOffset(contentRoot);
                const startRange = offsetToRange(contentRoot, firstOffset, firstOffset);
                if (startRange) {
                    sel.removeAllRanges();
                    sel.addRange(startRange);
                }
            }
        }

        const handler = (e: KeyboardEvent) => this.onKeyDown(e);
        doc.addEventListener('keydown', handler, true);

        const selectionHandler = this.selectionHandler.bind(this);
        doc.addEventListener('selectionchange', selectionHandler);
        doc.addEventListener('mouseup', selectionHandler);

        const clickHandler = this.clickHandler.bind(this);
        this.documentContainer.addEventListener('click', clickHandler);

        // The editor runs inside a same-origin iframe while the feature editor
        // sidebar lives in the parent document. After creating an annotation the
        // browser focus moves to that sidebar, so to let the user return to the
        // document with Escape we must also listen on the top document.
        const topDoc = this.getTopDocument();
        const topEscapeHandler = (e: KeyboardEvent) => this.onTopDocumentKeyDown(e);
        // When the feature editor that holds focus is removed (e.g. its annotation
        // is deleted), the browser drops focus to <body> instead of returning it to
        // the document. Watch for the sidebar losing focus to nothing so we can
        // reclaim it and let the user keep annotating from the keyboard.
        const topFocusOutHandler = (e: FocusEvent) => this.onTopDocumentFocusOut(e);
        if (topDoc && topDoc !== doc) {
            topDoc.addEventListener('keydown', topEscapeHandler, true);
            topDoc.addEventListener('focusout', topFocusOutHandler, true);
        }

        this.keyboardHandlerCleanup = () => {
            doc.removeEventListener('keydown', handler, true);
            doc.removeEventListener('selectionchange', selectionHandler);
            doc.removeEventListener('mouseup', selectionHandler);
            this.documentContainer.removeEventListener('click', clickHandler);
            if (topDoc && topDoc !== doc) {
                topDoc.removeEventListener('keydown', topEscapeHandler, true);
                topDoc.removeEventListener('focusout', topFocusOutHandler, true);
            }
        };

        this.enableContentEditableMode();

        this.enableCaretOverlay();
    }

    private clickHandler(ev: MouseEvent) {
        try {
            if (ev.button !== 0) return;
            const contentRoot = this.documentContainer;
            if (!contentRoot.contains(ev.target as Node)) return;
            this.placeCaretAtEvent(ev);
            // Mark the just-placed caret as deliberate so the sanitizer skips exactly
            // this selection change (matched by identity, not a wall-clock timer).
            this.markSelectionProgrammatic();
        } catch (e) {
            console.debug('clickHandler error', e);
        }
    }

    private selectionHandler() {
        // Skip the change we just made deliberately (a click or programmatic jump),
        // matched by the selection's identity. A stale marker (selection has since
        // moved elsewhere) is dropped so normal sanitization resumes.
        if (this.consumeProgrammaticSelection()) return;

        if (!annotatorState.keyboardCursorEnabled || !annotatorState.protectElements) return;

        try {
            const doc = this.root.ownerDocument;
            const sel = doc.getSelection();
            const contentRoot = this.documentContainer;
            if (sel && sel.rangeCount > 0) {
                if (sel.isCollapsed) {
                    const curRange = sel.getRangeAt(0);
                    const curPos = this.caretOffsetOf(curRange);
                    const bounds = this.findProtectedBounds(sel.anchorNode as Node | null);
                    if (bounds) {
                        const { start, end } = bounds;
                        const prev = this.lastSafeCaretOffset;
                        let targetPos: number;
                        if (prev != null && prev <= start) {
                            // Entered from before the element -> moving forward -> skip to end.
                            targetPos = end;
                        } else if (prev != null && prev >= end) {
                            // Entered from after the element -> moving backward -> skip to start.
                            targetPos = start;
                        } else if (curPos != null) {
                            // No usable origin: fall back to the nearest edge.
                            targetPos =
                                Math.abs(curPos - start) <= Math.abs(curPos - end) ? start : end;
                        } else {
                            targetPos = start;
                        }
                        const newRange = offsetToRange(contentRoot, targetPos, targetPos);
                        if (newRange) {
                            sel.removeAllRanges();
                            sel.addRange(newRange);
                        }
                        // The snapped edge is itself a safe origin for the next move.
                        this.lastSafeCaretOffset = targetPos;
                    } else if (curPos != null) {
                        // Caret is in safe territory; remember it as the direction origin.
                        this.lastSafeCaretOffset = curPos;
                    }
                } else {
                    // Only sanitize a range selection when one of its boundaries
                    // actually lands inside a protected element (svg/math). The
                    // sanitizer rebuilds the selection by round-tripping it through
                    // CAS character offsets; a replaced element such as an <img>
                    // contributes zero characters, so the positions just before and
                    // just after it collapse to the same offset. Re-applying that
                    // round-tripped range every selectionchange would therefore snap
                    // the focus back to the image and trap it there, preventing
                    // Shift+Arrow from extending the selection across the image into
                    // the text beyond. When no boundary is inside a protected element
                    // there is nothing to expand, so leave the native selection alone
                    // -- the browser already selects across images correctly.
                    const boundaryInProtected =
                        this.findProtectedBounds(sel.anchorNode as Node | null) != null ||
                        this.findProtectedBounds(sel.focusNode as Node | null) != null;
                    if (!boundaryInProtected) return;

                    const safeRange = getSafeRangeForSelection(
                        sel,
                        contentRoot,
                        this.protectedElementsMatcher,
                        annotatorState.protectElements
                    );
                    if (safeRange) {
                        this.applyRangeToSelection(sel, safeRange);
                    }
                }
            }
        } catch (e) {
            console.debug('selectionHandler error', e);
        }
    }

    private onKeyDown(e: KeyboardEvent) {
        if (!annotatorState.keyboardCursorEnabled) return;

        // While the overlapping-annotation selector popup is open, let it handle the
        // keyboard (arrow navigation, Enter to select, Escape to dismiss). We must not
        // move the caret or run our own Escape handling underneath it.
        if (this.selector?.isVisible()) {
            return;
        }

        // Escape reclaims focus from outside the document (e.g. the feature editor
        // sidebar that gets focused after creating an annotation) so the user can
        // continue navigating/annotating with the keyboard without reaching for the
        // mouse. Handle this before the INPUT/TEXTAREA early-return below because the
        // focus is typically inside a feature editor input at that point.
        if (e.key === 'Escape') {
            const active = this.root.ownerDocument.activeElement;
            if (active && !this.documentContainer.contains(active)) {
                e.preventDefault();
                e.stopPropagation();
                this.returnFocusToEditor();
                return;
            }
        }

        const tgt = e.target as Element | null;
        if (tgt instanceof HTMLElement) {
            if (['INPUT', 'TEXTAREA', 'SELECT'].includes(tgt.tagName)) {
                return;
            }
        }

        const doc = this.root.ownerDocument;
        const sel = doc.getSelection();
        if (!sel) return;

        // Refresh the caret overlay right after the browser applies a caret-moving
        // key. The browser coalesces selectionchange events, so during fast movement
        // it may not fire per keypress; this guarantees the overlay repositions and
        // pulses for every individual move. Scheduled on the next frame so the caret
        // has actually moved before we measure it.
        if (
            [
                'ArrowLeft',
                'ArrowRight',
                'ArrowUp',
                'ArrowDown',
                'Home',
                'End',
                'PageUp',
                'PageDown',
            ].includes(e.key)
        ) {
            this.scheduleCaretOverlayUpdate();
        }

        // Space selects the annotation at the caret. If multiple annotations
        // overlap there, a keyboard-navigable popup is shown to pick one.
        if (e.key === ' ') {
            e.preventDefault();
            const range = sel.rangeCount ? sel.getRangeAt(0) : null;
            if (range && this.selector) {
                // Remember the exact caret position before selecting so that pressing
                // Escape in the feature editor returns the caret here (not merely to
                // the start of the selected annotation), and so it can be restored if
                // the selected annotation is subsequently deleted.
                this.rememberedCaretOffset = this.caretOffsetOf(range);
                this.selector.selectAtCaret(range, this.documentContainer);
            }
            return;
        }

        // Backspace deletes the annotation at the caret. If multiple annotations
        // overlap there, the same popup is shown to pick which one to delete.
        if (e.key === 'Backspace') {
            e.preventDefault();
            const range = sel.rangeCount ? sel.getRangeAt(0) : null;
            if (range && this.selector) {
                // Remember the caret so focus/caret can be restored after the
                // deletion re-renders the document.
                this.rememberedCaretOffset = this.caretOffsetOf(range);
                this.selector.deleteAtCaret(range, this.documentContainer);
            }
            return;
        }

        if (e.key === 'Enter' && !e.shiftKey) {
            if (sel.isCollapsed) return;
            e.preventDefault();

            const contentRoot = this.documentContainer;
            const safeRange = getSafeRangeForSelection(
                sel,
                contentRoot,
                this.protectedElementsMatcher,
                annotatorState.protectElements
            );
            if (!safeRange) return;

            const anchorOffset =
                calculateStartOffset(contentRoot, safeRange.startContainer) + safeRange.startOffset;
            const focusOffset =
                calculateStartOffset(contentRoot, safeRange.endContainer) + safeRange.endOffset;
            const begin = Math.min(anchorOffset, focusOffset);
            const end = Math.max(anchorOffset, focusOffset);

            // Remember where the annotation ended so that pressing Escape in the
            // feature editor can return the caret here and let the user continue.
            // This supersedes any remembered select/delete caret position.
            this.rememberedCaretOffset = end;

            sel.removeAllRanges();
            this.ajax.createSpanAnnotation([[begin, end]], '');
            return;
        }
    }

    /**
     * Resolve the absolute document offset of a (collapsed) caret range, or null if it
     * cannot be determined. calculateStartOffset returns -1 on failure; guard against
     * it so a corrupted -1-based offset is never remembered and later used to misplace
     * the caret at the start of the document.
     */
    private caretOffsetOf(range: Range): number | null {
        return support.caretOffsetOf(this.documentContainer, range);
    }

    private findFirstWordStartOffset(contentRoot: Node): number {
        return support.findFirstWordStartOffset(contentRoot, this.root?.ownerDocument ?? undefined);
    }

    /**
     * Record the current selection as one we placed deliberately so the sanitizer
     * skips exactly that change (see the programmaticSelection field).
     */
    private markSelectionProgrammatic() {
        const sel = this.root.ownerDocument.getSelection();
        if (sel && sel.rangeCount > 0) {
            const r = sel.getRangeAt(0);
            this.programmaticSelection = { node: r.startContainer, offset: r.startOffset };
        } else {
            this.programmaticSelection = null;
        }
    }

    /**
     * If the current selection is still the one we just placed deliberately, consume
     * the marker and return true so the sanitizer skips this change. A stale marker is
     * dropped (returns false) so normal sanitization resumes.
     */
    private consumeProgrammaticSelection(): boolean {
        const pending = this.programmaticSelection;
        if (!pending) return false;
        this.programmaticSelection = null;
        const sel = this.root.ownerDocument.getSelection();
        if (sel && sel.rangeCount > 0) {
            const r = sel.getRangeAt(0);
            if (r.startContainer === pending.node && r.startOffset === pending.offset) {
                return true;
            }
        }
        return false;
    }

    private isSelectionBackward(sel: Selection): boolean {
        return support.isSelectionBackward(sel);
    }

    private applyRangeToSelection(sel: Selection, range: Range) {
        try {
            // A Range is always normalized start->end, so re-applying it would
            // discard the selection's direction and flip a backward (right-to-left)
            // selection to forward. That moves the focus to the wrong end and makes
            // the next Shift+Left collapse the selection instead of extending it.
            // Preserve the original direction so keyboard extension keeps growing.
            const backward = this.isSelectionBackward(sel);
            if ((sel as any).setBaseAndExtent) {
                if (backward) {
                    (sel as any).setBaseAndExtent(
                        range.endContainer,
                        range.endOffset,
                        range.startContainer,
                        range.startOffset
                    );
                } else {
                    (sel as any).setBaseAndExtent(
                        range.startContainer,
                        range.startOffset,
                        range.endContainer,
                        range.endOffset
                    );
                }
            } else {
                sel.removeAllRanges();
                sel.addRange(range);
            }
        } catch (e) {
            console.debug('applyRangeToSelection error', e);
        }
    }

    private findProtectedBounds(node: Node | null): { start: number; end: number } | null {
        return support.findProtectedBounds(
            this.documentContainer,
            node,
            this.protectedElementsMatcher
        );
    }

    private safeCaretOffset(offset: number): number {
        return support.safeCaretOffset(
            this.documentContainer,
            offset,
            this.protectedElementsMatcher
        );
    }

    private enableContentEditableMode() {
        try {
            const host = this.documentContainer;
            host.contentEditable = 'true';

            const beforeInput = (ev: InputEvent) => ev.preventDefault();
            const pasteHandler = (ev: ClipboardEvent) => ev.preventDefault();
            const dropHandler = (ev: DragEvent) => ev.preventDefault();

            const inputHandler = (ev: Event) => {
                ev.stopImmediatePropagation();
                if ((ev as any).preventDefault) (ev as any).preventDefault();
            };

            const keyBlock = (ev: KeyboardEvent) => {
                const nav = [
                    'ArrowLeft',
                    'ArrowRight',
                    'ArrowUp',
                    'ArrowDown',
                    'Tab',
                    'Home',
                    'End',
                    'PageUp',
                    'PageDown',
                ];
                if (nav.includes(ev.key) || ev.ctrlKey || ev.metaKey || ev.altKey) return;
                if (
                    ev.key.length === 1 ||
                    ev.key === 'Backspace' ||
                    ev.key === 'Delete' ||
                    ev.key === 'Enter'
                ) {
                    ev.preventDefault();
                    ev.stopImmediatePropagation();
                }
            };

            host.addEventListener('beforeinput', beforeInput as EventListener);
            host.addEventListener('input', inputHandler as EventListener);
            host.addEventListener('paste', pasteHandler as EventListener);
            host.addEventListener('cut', pasteHandler as EventListener);
            host.addEventListener('drop', dropHandler as EventListener);
            host.addEventListener('keydown', keyBlock as EventListener);

            this.contentEditableCleanup = () => {
                host.removeEventListener('beforeinput', beforeInput as EventListener);
                host.removeEventListener('input', inputHandler as EventListener);
                host.removeEventListener('paste', pasteHandler as EventListener);
                host.removeEventListener('cut', pasteHandler as EventListener);
                host.removeEventListener('drop', dropHandler as EventListener);
                host.removeEventListener('keydown', keyBlock as EventListener);
                host.contentEditable = 'false';
                this.contentEditableCleanup = undefined;
            };
        } catch (e) {
            console.debug('enableContentEditableMode error', e);
        }
    }

    private disableContentEditableMode() {
        if (this.contentEditableCleanup) {
            this.contentEditableCleanup();
            this.contentEditableCleanup = undefined;
        }
    }

    private placeCaretAtEvent(ev: MouseEvent) {
        const doc = this.root.ownerDocument;
        const contentRoot = this.documentContainer;
        const pos = positionToOffset(contentRoot, ev.clientX, ev.clientY);
        if (pos < 0) return;

        const range = offsetToRange(contentRoot, pos, pos);
        if (!range) return;

        const sel = doc.getSelection();
        if (!sel) return;

        sel.removeAllRanges();
        sel.addRange(range);

        if (annotatorState.protectElements) {
            const safeRange = getSafeRangeForSelection(sel, contentRoot);
            if (safeRange) {
                this.applyRangeToSelection(sel, safeRange);
            }
        }

        // Record the resulting collapsed caret as the safe direction origin so a
        // subsequent arrow press into a protected element skips the right way.
        const placed = sel.rangeCount ? sel.getRangeAt(0) : null;
        if (placed && placed.collapsed) {
            const off = this.caretOffsetOf(placed);
            if (off != null) this.lastSafeCaretOffset = off;
        }
    }

    /**
     * Resolve the top-level document (the parent INCEpTION page that hosts the
     * feature editor sidebar). Returns null if it cannot be reached, e.g. because
     * the hosting iframe turns out to be cross-origin.
     */
    private getTopDocument(): Document | null {
        try {
            const win = this.root.ownerDocument.defaultView;
            const top = win?.top;
            return top ? top.document : null;
        } catch {
            return null;
        }
    }

    private onTopDocumentKeyDown(e: KeyboardEvent) {
        if (!annotatorState.keyboardCursorEnabled) return;
        if (e.key !== 'Escape') return;

        try {
            const topDoc = this.getTopDocument();
            const active = topDoc?.activeElement as Element | null;
            // Only reclaim focus when it currently sits in the feature editor
            // sidebar (where it lands after creating an annotation).
            if (active && active.closest && active.closest('.feature-editors-sidebar')) {
                e.preventDefault();
                e.stopPropagation();
                this.returnFocusToEditor();
            }
        } catch (err) {
            console.debug('onTopDocumentKeyDown error', err);
        }
    }

    /**
     * Reclaim focus into the document after it was orphaned to {@code <body>}.
     * This happens when the feature editor that held focus is removed because its
     * annotation was deleted, leaving the user unable to continue keyboard
     * navigation. We only act when the focus is genuinely lost (i.e. sitting on the
     * top document body) so that we never steal focus while the user is still
     * typing in a feature editor (after creating or selecting an annotation).
     */
    private onTopDocumentFocusOut(e: FocusEvent) {
        if (!annotatorState.keyboardCursorEnabled) return;
        // If focus moved to a real element there is nothing to reclaim. A removed
        // focused element transfers focus to nothing (relatedTarget == null).
        if (e.relatedTarget) return;

        const doc = this.root.ownerDocument;
        const win = doc.defaultView || window;
        // Defer until the focus transition has settled so activeElement reflects
        // the final state (body when the focused element was removed).
        win.setTimeout(() => {
            try {
                if (!annotatorState.keyboardCursorEnabled) return;
                const topDoc = this.getTopDocument();
                if (!topDoc || topDoc === doc) return;
                const active = topDoc.activeElement;
                // Focus landed on a real control (e.g. another feature editor) or
                // moved into the editor iframe -> leave it alone.
                if (active && active !== topDoc.body) return;
                this.returnFocusToEditor(this.rememberedCaretOffset);
            } catch (err) {
                console.debug('onTopDocumentFocusOut error', err);
            }
        }, 0);
    }

    /**
     * Move focus back into the document and place the caret at the given offset. When
     * no offset is given, fall back to the position remembered from the last action:
     * the end of the most recently created annotation, or otherwise the caret position
     * from before the last select/delete. Used both for Escape-to-return after creating
     * or selecting an annotation and for reclaiming orphaned focus after a deletion.
     */
    private returnFocusToEditor(caretOffset?: number | null) {
        const doc = this.root.ownerDocument;

        // Commit any pending feature editor value by blurring whatever is focused
        // before we steal focus, so the label the user just typed is not lost. The
        // focused element may live in the parent document, where a cross-realm
        // `instanceof HTMLElement` would fail, so we duck-type the blur() call.
        const blurActive = (d: Document | null | undefined) => {
            const active = d?.activeElement as any;
            if (active && active !== this.documentContainer && typeof active.blur === 'function') {
                active.blur();
            }
        };
        blurActive(this.getTopDocument());
        blurActive(doc);

        // Focusing the container would, by default, scroll it into view — which jumps
        // the viewport to the top of the document. The caret is restored just below to
        // where the user already was (on-screen), so suppress the automatic scroll.
        this.documentContainer.focus({ preventScroll: true });

        const requested = caretOffset != null ? caretOffset : this.rememberedCaretOffset;
        if (requested != null) {
            const contentRoot = this.documentContainer;
            // Never restore the caret inside a protected element; snap it out if needed.
            const offset = this.safeCaretOffset(requested);
            const range = offsetToRange(contentRoot, offset, offset);
            if (range) {
                const sel = doc.getSelection();
                if (sel) {
                    sel.removeAllRanges();
                    sel.addRange(range);
                }
                // Remember as the safe direction origin for subsequent navigation.
                this.lastSafeCaretOffset = offset;
            }
        }
        this.rememberedCaretOffset = null;
    }

    /**
     * Move the keyboard caret to the given document offset. Used when the view is
     * scrolled to a new location by something other than caret movement — opening a
     * document (scroll to the last edit location) or selecting an annotation in the
     * sidebar. Without this the caret stays where it was, so the first cursor-key press
     * scrolls the view back to it, undoing the jump.
     *
     * Only acts while keyboard navigation is enabled. Deliberately does not move focus:
     * the jump may originate from the sidebar, and stealing focus into the document
     * would be intrusive. Setting a collapsed selection neither scrolls nor focuses on
     * its own, so the explicit scroll remains in charge of the viewport.
     */
    public moveCaretToOffset(offset: number | null | undefined) {
        if (!annotatorState.keyboardCursorEnabled) return;
        if (offset == null || offset < 0) return;

        try {
            const doc = this.root.ownerDocument;
            const contentRoot = this.documentContainer;
            // Never place the caret inside a protected element; snap it out if needed.
            const safe = this.safeCaretOffset(offset);
            const range = offsetToRange(contentRoot, safe, safe);
            if (!range) return;

            const sel = doc.getSelection();
            if (!sel) return;

            sel.removeAllRanges();
            sel.addRange(range);

            // Record the placed caret as the safe direction origin for the next move.
            this.lastSafeCaretOffset = safe;

            // Treat this like a deliberate caret placement (as with a click): mark the
            // selection so the sanitizer skips exactly this change (matched by identity)
            // and a stale directional snap does not relocate the caret away from the
            // jump target.
            this.markSelectionProgrammatic();

            // Keep the custom caret overlay in sync with the programmatic move.
            this.updateCaretOverlay();
        } catch (e) {
            console.debug('moveCaretToOffset error', e);
        }
    }

    /**
     * Show a custom caret overlay while keyboard mode is active. The native caret is
     * only a thin 1px line that is easily lost in dense text, so we hide it (via the
     * {@code iaa-caret-custom} class) and draw our own thicker, coloured bar at the
     * caret position. The bar follows the caret on selection changes, scrolling and
     * resizing, and briefly pulses whenever it moves to draw the eye to the new spot.
     */
    private enableCaretOverlay() {
        if (this.caretOverlay) return;

        const doc = this.root.ownerDocument;
        const win = doc.defaultView || window;

        this.documentContainer.classList.add('iaa-caret-custom');

        const overlay = doc.createElement('div');
        overlay.className = 'iaa-caret-overlay';
        overlay.style.display = 'none';
        // The glow is a dedicated child element (rather than a pseudo-element on the
        // overlay) so its CSS transition can be driven independently of the overlay's
        // blink animation -- see the .iaa-caret-moving .iaa-caret-ring rules in the SCSS.
        const ring = doc.createElement('div');
        ring.className = 'iaa-caret-ring';
        overlay.appendChild(ring);
        doc.body.appendChild(overlay);
        this.caretOverlay = overlay;
        this.lastCaretRectKey = null;

        const update = () => this.scheduleCaretOverlayUpdate();
        // selectionchange catches caret moves and (re)placement; scroll and resize
        // keep the fixed-position overlay aligned with the content underneath it.
        doc.addEventListener('selectionchange', update);
        win.addEventListener('scroll', update, true);
        win.addEventListener('resize', update);

        this.caretOverlayCleanup = () => {
            doc.removeEventListener('selectionchange', update);
            win.removeEventListener('scroll', update, true);
            win.removeEventListener('resize', update);
        };

        this.updateCaretOverlay();
    }

    /**
     * Coalesce caret-overlay refreshes into a single measurement per animation frame.
     * selectionchange, capture-phase scroll (which fires for every nested scroller),
     * resize and per-keystroke refreshes would otherwise each force a synchronous
     * layout via getBoundingClientRect/getClientRects — janky on large documents.
     */
    private scheduleCaretOverlayUpdate() {
        if (this.caretOverlayRaf != null) return;
        const win = this.root.ownerDocument.defaultView || window;
        this.caretOverlayRaf = win.requestAnimationFrame(() => {
            this.caretOverlayRaf = undefined;
            this.updateCaretOverlay();
        });
    }

    private disableCaretOverlay() {
        if (this.caretOverlayCleanup) {
            this.caretOverlayCleanup();
            this.caretOverlayCleanup = undefined;
        }
        if (this.caretOverlayRaf != null) {
            const win = this.root.ownerDocument.defaultView || window;
            win.cancelAnimationFrame(this.caretOverlayRaf);
            this.caretOverlayRaf = undefined;
        }
        if (this.caretMoveTimer != null) {
            const win = this.root.ownerDocument.defaultView || window;
            win.clearTimeout(this.caretMoveTimer);
            this.caretMoveTimer = undefined;
        }
        this.caretOverlay?.remove();
        this.caretOverlay = undefined;
        this.lastCaretRectKey = null;
        this.documentContainer.classList.remove('iaa-caret-custom');
    }

    private updateCaretOverlay() {
        const overlay = this.caretOverlay;
        if (!overlay) return;

        const hide = () => {
            overlay.style.display = 'none';
            this.lastCaretRectKey = null;
        };

        if (!annotatorState.keyboardCursorEnabled) {
            hide();
            return;
        }

        const doc = this.root.ownerDocument;
        const sel = doc.getSelection();
        // Only show the caret for a collapsed selection; while text is selected the
        // native selection highlight already marks the position.
        if (!sel || sel.rangeCount === 0 || !sel.isCollapsed) {
            hide();
            return;
        }

        const range = sel.getRangeAt(0);
        const contentRoot = this.documentContainer;
        if (!contentRoot.contains(range.startContainer)) {
            hide();
            return;
        }

        const rect = rangeClientRect(range, true);
        if (!rect) {
            hide();
            return;
        }

        let height = rect.height;
        if (!height) {
            // A collapsed range at a boundary can report zero height; fall back to
            // the line/font metrics of the surrounding element.
            const el = nodeToElement(range.startContainer);
            const cs = el ? (doc.defaultView || window).getComputedStyle(el) : null;
            height = (cs && (parseFloat(cs.lineHeight) || parseFloat(cs.fontSize))) || 16;
        }

        overlay.style.display = 'block';
        overlay.style.left = `${rect.left}px`;
        overlay.style.top = `${rect.top}px`;
        overlay.style.height = `${height}px`;

        const key = `${Math.round(rect.left)},${Math.round(rect.top)}`;
        // Mark the caret as moving on an actual move (not first appearance or a
        // no-op update) so it stays solid and glowing during continuous movement.
        if (this.lastCaretRectKey !== null && this.lastCaretRectKey !== key) {
            this.markCaretMoved();
        }
        this.lastCaretRectKey = key;
    }

    /**
     * Mark the caret as currently moving: it is held solid (no blink) with a steady
     * glow while movement continues, and only fades back to the idle blinking caret
     * once movement has paused briefly. This keeps the caret clearly visible during
     * continuous navigation instead of flickering as a per-move pulse fades in and
     * out.
     */
    private markCaretMoved() {
        const overlay = this.caretOverlay;
        if (!overlay) return;
        const win = this.root.ownerDocument.defaultView || window;
        overlay.classList.add('iaa-caret-moving');
        if (this.caretMoveTimer != null) win.clearTimeout(this.caretMoveTimer);
        this.caretMoveTimer = win.setTimeout(() => {
            this.caretMoveTimer = undefined;
            this.caretOverlay?.classList.remove('iaa-caret-moving');
        }, 220);
    }

    public destroy() {
        if (this.keyboardHandlerCleanup) {
            this.keyboardHandlerCleanup();
            this.keyboardHandlerCleanup = undefined;
        }
        this.disableCaretOverlay();
        this.disableContentEditableMode();
    }
}

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
import type { DiamAjax, VID } from '@inception-project/inception-js-api';
import { getInlineLabelClientRect, highlights, isPointInRect, rangeClientRect } from './Utilities';
import { NO_LABEL } from './ApacheAnnotatorVisualizer.svelte';
import { createPopper, type Instance } from '@popperjs/core';

/**
 * How the overlapping-annotation popup behaves:
 * - `mouse`: opened by clicking a stack of annotations. Each entry can be selected
 *   (click the label) or deleted (click the ❌), so both affordances are shown.
 * - `keyboard-select`: opened via the keyboard to pick an annotation to select. Only
 *   selection is offered (no ❌); activating an entry selects it.
 * - `keyboard-delete`: opened via the keyboard to pick an annotation to delete.
 *   Activating an entry deletes it and the entry is styled as destructive.
 */
type PopupMode = 'mouse' | 'keyboard-select' | 'keyboard-delete';

export class ApacheAnnotatorSelector {
    private ajax: DiamAjax;
    private root: Element;

    private popup: Instance | undefined;
    private popupContent: HTMLElement | undefined;
    private popupAnchor: HTMLElement | undefined;

    // Keyboard navigation state for the overlapping-annotation popup
    private menuItems: HTMLElement[] = [];
    private activeIndex: number = -1;
    private focusFallback: HTMLElement | undefined;
    private popupKeydownHandler: ((e: KeyboardEvent) => void) | undefined;
    private popupMode: PopupMode = 'mouse';

    public constructor(element: Element, ajax: DiamAjax) {
        this.ajax = ajax;
        this.root = element;

        this.root.addEventListener('mousedown', (e) => this.onMouseDown(e));
    }

    /**
     * Select the annotation(s) at the given caret range. Intended for keyboard-driven
     * selection. If a single annotation covers the caret it is selected directly;
     * if multiple annotations overlap, a keyboard-navigable popup is shown so the user
     * can pick one. Returns true if there was at least one annotation at the caret.
     *
     * @param range a (typically collapsed) caret range inside the document
     * @param focusFallback element to return focus to if the popup is dismissed via Escape
     */
    public selectAtCaret(range: Range, focusFallback?: HTMLElement): boolean {
        return this.actionAtCaret(range, 'keyboard-select', focusFallback);
    }

    /**
     * Delete the annotation(s) at the given caret range. Intended for keyboard-driven
     * deletion. If a single annotation covers the caret it is deleted directly; if
     * multiple annotations overlap, a keyboard-navigable popup is shown so the user can
     * pick which one to delete. Returns true if there was at least one annotation at the
     * caret.
     *
     * @param range a (typically collapsed) caret range inside the document
     * @param focusFallback element to return focus to after deleting or when the popup is
     *     dismissed via Escape
     */
    public deleteAtCaret(range: Range, focusFallback?: HTMLElement): boolean {
        return this.actionAtCaret(range, 'keyboard-delete', focusFallback);
    }

    private actionAtCaret(
        range: Range,
        mode: 'keyboard-select' | 'keyboard-delete',
        focusFallback?: HTMLElement
    ): boolean {
        this.destroyPopup();

        const hls = highlights(range.startContainer);
        if (hls.length === 0) return false;

        if (hls.length === 1) {
            const vid = hls[0].getAttribute('data-iaa-id');
            if (!vid) return false;
            if (mode === 'keyboard-delete') {
                this.ajax.deleteAnnotation(vid);
                focusFallback?.focus();
            } else {
                this.ajax.selectAnnotation(vid);
            }
            return true;
        }

        const rect = rangeClientRect(range);
        if (!rect) return false;

        this.focusFallback = focusFallback;
        this.createPopup(rect.left, rect.bottom, hls, true, mode);
        return true;
    }

    private onMouseDown(event: Event): void {
        // Destroy popup if clicked outside of popup
        if (!this.isVisible()) {
            return;
        }

        if (event.target instanceof Node && this.popupContent?.contains(event.target as Node)) {
            return;
        }

        this.destroyPopup();
    }

    private destroyPopup() {
        if (!this.popup) return;

        if (this.popupContent && this.popupKeydownHandler) {
            this.popupContent.removeEventListener('keydown', this.popupKeydownHandler);
        }
        this.popup.destroy();
        this.popupContent?.remove();
        this.popupAnchor?.remove();
        this.popup = undefined;
        this.popupContent = undefined;
        this.popupAnchor = undefined;
        this.popupKeydownHandler = undefined;
        this.menuItems = [];
        this.activeIndex = -1;
        this.focusFallback = undefined;
        this.popupMode = 'mouse';
    }

    public isVisible(): boolean {
        return this.popup !== undefined;
    }

    public showSelector(event: Event): void {
        const mouseEvent = event as MouseEvent;

        this.destroyPopup();

        const hls = event.target instanceof Node ? highlights(event.target) : [];
        // No need to show selector if there is no annotation
        if (hls.length === 0) return;

        if (
            hls.length === 1 ||
            isPointInRect(
                { x: mouseEvent.clientX, y: mouseEvent.clientY },
                getInlineLabelClientRect(hls[0])
            )
        ) {
            // No need to show selector if there is only a single annotation or if the user clicked on the
            // inline label
            const vid = hls[0].getAttribute('data-iaa-id');
            if (!vid) return;
            this.ajax.selectAnnotation(vid);
            return;
        }

        this.createPopup(mouseEvent.clientX, mouseEvent.clientY, hls, false, 'mouse');
    }

    private createPopup(
        clientX: number,
        clientY: number,
        hls: HTMLElement[],
        focusForKeyboard: boolean,
        mode: PopupMode = 'mouse'
    ) {
        this.popupMode = mode;

        this.popupAnchor = document.createElement('div');
        this.popupAnchor.style.position = 'absolute';
        this.popupAnchor.style.top = `${clientY + window.scrollY}px`;
        this.popupAnchor.style.left = `${clientX}px`;
        this.popupAnchor.style.pointerEvents = 'none';
        this.popupAnchor.style.visibility = 'hidden';
        this.root.ownerDocument.body.appendChild(this.popupAnchor);

        // The ❌ per-entry button is only offered for mouse interaction. In the
        // keyboard modes the popup does exactly one thing (select or delete), made
        // explicit by a header instead.
        const showDeleteButton = mode === 'mouse';

        this.popupContent = document.createElement('div');
        this.popupContent.classList.add('iaa-menu');
        // Signal delete intent so the active entry can be styled as destructive.
        if (mode === 'keyboard-delete') {
            this.popupContent.classList.add('iaa-menu-delete');
        }
        // Make the menu focusable so it can capture keyboard navigation keys.
        this.popupContent.tabIndex = -1;

        // In the keyboard modes, show a header indicating whether picking an entry
        // selects or deletes it.
        if (mode !== 'mouse') {
            const header = document.createElement('div');
            header.classList.add('iaa-menu-header');
            header.textContent = mode === 'keyboard-delete' ? 'Delete…' : 'Select…';
            this.popupContent.appendChild(header);
        }

        this.menuItems = [];
        for (const hl of hls) {
            const vid = hl.getAttribute('data-iaa-id');
            if (!vid) continue;

            const menuItem = document.createElement('div');
            menuItem.classList.add('iaa-menu-item');
            menuItem.dataset.vid = vid;

            const label = hl.getAttribute('data-iaa-label') || NO_LABEL;
            const labelArea = document.createElement('div');
            labelArea.classList.add('iaa-label');
            labelArea.textContent = label !== NO_LABEL ? label : 'no label';
            labelArea.style.cursor = 'pointer';
            // Activating an entry selects it, except in keyboard-delete mode where it
            // deletes it.
            labelArea.addEventListener('click', (e) => this.activateItem(e, vid));
            menuItem.appendChild(labelArea);

            if (showDeleteButton) {
                const deleteButton = document.createElement('a');
                deleteButton.classList.add('iaa-btn');
                deleteButton.textContent = '❌';
                deleteButton.addEventListener('click', (e) => this.onDeleteAnnotation(e, vid));
                menuItem.appendChild(deleteButton);
            }

            this.menuItems.push(menuItem);
            this.popupContent.appendChild(menuItem);
        }
        this.root.ownerDocument.body.appendChild(this.popupContent);

        this.popup = createPopper(this.popupAnchor, this.popupContent, { placement: 'top' });

        this.popupKeydownHandler = (e: KeyboardEvent) => this.onPopupKeyDown(e);
        this.popupContent.addEventListener('keydown', this.popupKeydownHandler);

        if (focusForKeyboard) {
            this.setActive(0);
            this.popupContent.focus();
        }
    }

    private setActive(index: number) {
        if (this.menuItems.length === 0) return;
        const n = this.menuItems.length;
        const next = ((index % n) + n) % n;
        this.menuItems.forEach((item, i) =>
            item.classList.toggle('iaa-menu-item-active', i === next)
        );
        this.activeIndex = next;
        this.menuItems[next].scrollIntoView({ block: 'nearest' });
    }

    private onPopupKeyDown(e: KeyboardEvent) {
        if (!this.popupContent) return;

        switch (e.key) {
            case 'ArrowDown':
                e.preventDefault();
                e.stopPropagation();
                this.setActive(this.activeIndex + 1);
                break;
            case 'ArrowUp':
                e.preventDefault();
                e.stopPropagation();
                this.setActive(this.activeIndex - 1);
                break;
            case 'Enter':
                e.preventDefault();
                e.stopPropagation();
                if (this.activeIndex >= 0) {
                    const vid = this.menuItems[this.activeIndex]?.dataset.vid;
                    if (vid) this.activateItem(e, vid);
                }
                break;
            case 'Escape': {
                e.preventDefault();
                e.stopPropagation();
                const fallback = this.focusFallback;
                this.destroyPopup();
                fallback?.focus();
                break;
            }
        }
    }

    /**
     * Perform the popup's primary action on an entry: delete in keyboard-delete mode,
     * otherwise select.
     */
    private activateItem(event: Event, id: VID) {
        if (this.popupMode === 'keyboard-delete') {
            this.onDeleteAnnotation(event, id);
        } else {
            this.onSelectAnnotation(event, id);
        }
    }

    private onSelectAnnotation(event: Event, id: VID) {
        console.log(`Selecting annotation ${id}`);
        event.stopPropagation();
        this.destroyPopup();
        this.ajax.selectAnnotation(id);
    }

    private onDeleteAnnotation(event: Event, id: VID) {
        console.log(`Deleting annotation ${id}`);
        event.stopPropagation();
        // Capture before destroyPopup() clears it: after a keyboard-driven delete we
        // return focus to the document so the user can keep annotating without the
        // mouse. For mouse-driven deletes (no fallback) this is a no-op.
        const fallback = this.focusFallback;
        this.destroyPopup();
        this.ajax.deleteAnnotation(id);
        fallback?.focus();
    }

    destroy(): void {
        this.destroyPopup();
    }
}

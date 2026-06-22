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
import { type DiamAjax, type VID } from '@inception-project/inception-js-api';
import { type RelationVisualizer } from './RelationVisualizer';
import { GRIP_CLASS } from './RelationGripLayer';
import { distinctHighlightStack, getInlineLabelClientRect, isPointInRect } from './Utilities';

/**
 * The relation-creation drag state machine.
 *
 *   IDLE → (grip mousedown) → ARMED → (move > threshold) → DRAGGING → (mouseup) → COMMIT/CANCEL
 *
 * The move/up listeners live on `window` in the capture phase so a release outside the iframe still
 * resolves and so the terminating mouseup can be stopped before the editor root reads the selection
 * and creates a span. A grip click that never crosses the threshold does nothing.
 */

const DRAG_THRESHOLD_PX = 5;

/**
 * Show the target-disambiguation popup and resolve with the chosen vid. Dismissing the popup must
 * not call `onPick` — that is a cancel. Wired to ApacheAnnotatorSelector.pickRelationTarget.
 */
export type PickRelationTarget = (
    clientX: number,
    clientY: number,
    targets: HTMLElement[],
    onPick: (vid: VID) => void
) => void;

/** Toggled on the content root during a drag for the grabbing cursor and to suppress text selection. */
const CLASS_DRAGGING = 'iaa-relation-dragging';

type State = 'idle' | 'armed' | 'dragging';

export class RelationDragController {
    private root: Element;
    private ajax: DiamAjax;
    private relationVisualizer: RelationVisualizer;
    private pickTarget: PickRelationTarget;

    private state: State = 'idle';
    private sourceVid: VID | null = null;
    /** Rubber-band origin (viewport coords): the centre of the source grip at mousedown. */
    private sourcePoint = { x: 0, y: 0 };
    private color = '';
    /** mousedown position, for the move-distance threshold that separates a click from a drag. */
    private originX = 0;
    private originY = 0;

    public constructor(
        root: Element,
        ajax: DiamAjax,
        relationVisualizer: RelationVisualizer,
        pickTarget: PickRelationTarget
    ) {
        this.root = root;
        this.ajax = ajax;
        this.relationVisualizer = relationVisualizer;
        this.pickTarget = pickTarget;
    }

    /** Arm a drag from a grip (its mousedown handler, wired by RelationGripLayer). */
    public onGripPointerDown(grip: HTMLElement, e: MouseEvent): void {
        if (e.button !== 0) return; // primary button only
        const vid = grip.dataset.gripVid;
        if (!vid) return;

        // Keep the gesture off the editor: no text selection, and (keyboard mode) no caret drop.
        e.preventDefault();
        e.stopPropagation();

        this.state = 'armed';
        this.sourceVid = vid;
        const r = grip.getBoundingClientRect();
        this.sourcePoint = { x: r.left + r.width / 2, y: r.top + r.height / 2 };
        this.color = grip.style.getPropertyValue('--iaa-grip-color').trim();
        this.originX = e.clientX;
        this.originY = e.clientY;

        window.addEventListener('mousemove', this.onWindowMove, true);
        window.addEventListener('mouseup', this.onWindowUp, true);
        window.addEventListener('keydown', this.onWindowKey, true);
    }

    private onWindowMove = (e: MouseEvent): void => {
        if (this.state === 'armed') {
            const dist = Math.hypot(e.clientX - this.originX, e.clientY - this.originY);
            if (dist <= DRAG_THRESHOLD_PX) return;
            this.state = 'dragging';
            this.root.classList.add(CLASS_DRAGGING);
            this.relationVisualizer.beginDragArc(this.color);
        }

        if (this.state === 'dragging') {
            this.relationVisualizer.updateDragArc(this.sourcePoint, { x: e.clientX, y: e.clientY });
            e.preventDefault();
        }
    };

    private onWindowUp = (e: MouseEvent): void => {
        if (this.state === 'dragging') {
            // Stop the editor root's mouseup from also reading the selection and creating a span
            // where the drag ended.
            e.preventDefault();
            e.stopPropagation();
            this.commitAt(e);
        }
        // ARMED → up with no drag is a plain grip click: do nothing.
        this.teardown();
    };

    private onWindowKey = (e: KeyboardEvent): void => {
        if (e.key === 'Escape') {
            e.preventDefault();
            e.stopPropagation();
            this.teardown();
        }
    };

    /** Resolve the drop target under the cursor and create the relation (or cancel). */
    private commitAt(e: MouseEvent): void {
        if (this.sourceVid == null) return;
        const source = this.sourceVid;

        const under = this.root.ownerDocument.elementFromPoint(e.clientX, e.clientY);

        // A grip proxies its span: grips sit over the span start (the inline-label region) with
        // pointer-events:auto and survive the drag, so a release aimed there lands on the grip, whose
        // data-grip-vid — not data-iaa-id — means highlights() resolves nothing. Target the grip's
        // span directly (also lets a fanned grip pick one of several overlapping spans without a popup).
        const grip = under instanceof Element ? under.closest<HTMLElement>('.' + GRIP_CLASS) : null;
        if (grip?.dataset.gripVid) {
            this.commitRelation(source, grip.dataset.gripVid, e);
            return;
        }

        // Resolve the span stack under the cursor (overlay and rubber band are pointer-events:none,
        // so elementFromPoint reaches the content). The stack is innermost-first, one entry per vid.
        const stack = distinctHighlightStack(under);

        if (stack.length === 0) {
            return; // nothing under the drop → cancel
        }

        // A drop on an inline label targets exactly that label's annotation (the innermost highlight)
        // even when it is nested in others — otherwise the drop resolves the whole ancestor stack and
        // pops up the menu though the label already names one span. Mirrors the click-selection path
        // in ApacheAnnotatorSelector.showSelector.
        const innermost = stack[0];
        if (stack.length === 1 || this.droppedOnInlineLabel(e, innermost)) {
            const vid = innermost.getAttribute('data-iaa-id');
            if (vid) this.commitRelation(source, vid, e);
            return;
        }

        // Several overlapping spans under shared text → disambiguate via the selector popup.
        // The drop event is captured for the deferred commit so the relation-type dialog opens at the
        // drop; dismissing the popup never calls back, i.e. cancels.
        this.pickTarget(e.clientX, e.clientY, stack, (vid) => {
            this.commitRelation(source, vid, e);
        });
    }

    /**
     * Create the relation unless it would be an unintended self-loop. A relation whose target is its
     * own source is only created when Shift is held at drop time — otherwise a tiny drag that releases
     * back over the source (e.g. onto the source's own still-visible grip) would silently create a
     * span-to-itself loop. Mirrors brat, where self-loops require Shift (AnnotatorUI.endArcCreation).
     */
    private commitRelation(source: VID, target: VID, e: MouseEvent): void {
        if (source === target && !e.shiftKey) return;
        this.ajax.createRelationAnnotation(source, target, e);
    }

    /**
     * Whether the drop point is on `hl`'s inline label. The class guard scopes this to label mode
     * (the class is only set when labels are shown); getInlineLabelClientRect throws on a highlight
     * with no leading text node, hence the try/catch.
     */
    private droppedOnInlineLabel(e: MouseEvent, hl: HTMLElement): boolean {
        if (!hl.classList.contains('iaa-inline-label')) return false;
        try {
            return isPointInRect({ x: e.clientX, y: e.clientY }, getInlineLabelClientRect(hl));
        } catch {
            return false;
        }
    }

    private teardown(): void {
        window.removeEventListener('mousemove', this.onWindowMove, true);
        window.removeEventListener('mouseup', this.onWindowUp, true);
        window.removeEventListener('keydown', this.onWindowKey, true);
        this.root.classList.remove(CLASS_DRAGGING);
        this.relationVisualizer.clearDragArc();
        this.state = 'idle';
        this.sourceVid = null;
    }

    /** Abort any in-flight drag and detach listeners (called from the visualizer's destroy). */
    public destroy(): void {
        this.teardown();
    }
}

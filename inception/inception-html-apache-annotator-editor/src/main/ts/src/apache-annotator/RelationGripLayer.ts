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
import './RelationGripLayer.scss';
import { type VID } from '@inception-project/inception-js-api';
import { collectFragmentRects, type HighlightResolver } from './RelationVisualizer';
import { distinctHighlightStack, fanAngle } from './Utilities';

/**
 * Surfaces relation-creation "grips" on hover. A grip is a small
 * drag affordance anchored above the start of a hovered span. It is *not* the highlighted text, so
 * it never competes with the native text-selection gesture that creates spans.
 *
 * Ownership contract with ApacheAnnotatorVisualizer (mirrors RelationVisualizer): the main
 * visualizer decides *when* grips are refreshed or cleared (hover events, re-render, scroll). This
 * class owns *what* is drawn and installs no recompute triggers of its own.
 */

const LAYER_CLASS = 'iaa-relation-grip-layer';
/** Class on each grip knob. Exported so the drag controller can recognise a grip as a drop target
 * and resolve it to its span via `data-grip-vid` (a grip proxies its span; see RelationDragController). */
export const GRIP_CLASS = 'iaa-relation-grip';
/** Echo class added to every fragment of a grip's owning vid while the grip is hovered:
 * the inverse of normal highlight-hover — pointing at the grip emphasises the span it proxies, making
 * the grip→span mapping unambiguous. Styled in RelationGripLayer.scss. */
const ECHO_CLASS = 'iaa-relation-source-candidate';

/** Vertical gap (px) between a grip's anchor (the span-start, on the text baseline box) and the
 * knob, so the knob sits just above the line rather than on top of the ink. */
const GRIP_RISE_PX = 4;

/**
 * Linger (ms) before grips are torn down once the pointer leaves the highlight. A grip sits in the
 * empty space *above* the line, so travelling text→grip necessarily crosses a non-highlight gap;
 * clearing immediately would dismiss the grip before the cursor can reach it. The deferred clear is
 * cancelled the moment the pointer lands on a grip (or back on the stack).
 */
const GRIP_LINGER_MS = 400;

/** Total angular spread of a fan of coincident grips, symmetric around vertical. */
const FAN_SPREAD_RAD = (90 * Math.PI) / 180;
/** Smallest fan radius; the actual radius grows with the knob count (see layoutFan). */
const FAN_BASE_RADIUS_PX = 12;
/** Minimum straight-line gap (chord) kept between adjacent knobs so each stays clickable. */
const FAN_MIN_CHORD_PX = 11;
/** Anchors within this many px (both axes) count as the same span-start and get fanned together. */
const ANCHOR_TOLERANCE_PX = 2;

/** A grip's resolved anchor before fan layout: where its span starts on screen, and its colour. */
interface GripSpec {
    vid: VID;
    /** Span-start fragment left/top in viewport coordinates (pre origin-subtraction). */
    left: number;
    top: number;
    color: string;
}

export class RelationGripLayer {
    private root: Element;
    private resolveHighlights: HighlightResolver;

    /**
     * Transient overlay holding the grip knobs. It is a sibling of the inline <mark>s under the
     * content root — deliberately NOT among the `.iaa-highlighted` marks — so `highlights()` (and
     * therefore the drop hit-test) never mistakes a grip for an annotation.
     */
    private layer: HTMLElement;

    /** Layer top-left in viewport coordinates, captured fresh whenever grips are (re)built. */
    private origin = { left: 0, top: 0 };

    /**
     * Sorted, comma-joined vids of the stack the grips currently represent; null when no grips are
     * shown. Used to skip rebuilding while the pointer stays within the same highlight stack.
     */
    private currentKey: string | null = null;

    /** Pending linger timeout handle (see GRIP_LINGER_MS); undefined when none is scheduled. */
    private clearTimer: number | undefined;

    /**
     * Fragments currently carrying the echo class (the hovered grip's owning vid). Tracked so the
     * echo can be lifted even when the grip is torn down without a mouseleave — e.g. grips are
     * cleared on scroll / re-render while one is hovered, which does not reliably fire mouseleave.
     */
    private echoed: Element[] = [];

    /** Invoked on a grip's mousedown to arm a relation drag (RelationDragController); optional so the
     * layer can be used for grip display alone. */
    private onGripPointerDown?: (grip: HTMLElement, e: MouseEvent) => void;

    public constructor(
        root: Element,
        resolveHighlights: HighlightResolver,
        onGripPointerDown?: (grip: HTMLElement, e: MouseEvent) => void
    ) {
        this.root = root;
        this.resolveHighlights = resolveHighlights;
        this.onGripPointerDown = onGripPointerDown;

        this.layer = document.createElement('div');
        this.layer.classList.add(LAYER_CLASS);
        // The layer itself is inert; individual grips opt back into pointer events so they can be
        // hovered and dragged.
        this.layer.style.pointerEvents = 'none';
        this.root.appendChild(this.layer);
    }

    /**
     * React to the pointer moving over the content. Pass the event target; grips are (re)built for
     * the hovered highlight stack, or cleared when the pointer is over neither a highlight nor a
     * grip. Hovering a grip keeps the current grips (it is not a highlight, so a naive re-derive
     * would otherwise tear them down).
     */
    public handlePointerOver(target: EventTarget | null): void {
        // Pointer is on a grip (or the layer): keep the grips and cancel any pending teardown so the
        // user can travel from the text up to the knob across the empty gap without losing it.
        if (target instanceof Element && target.closest('.' + LAYER_CLASS)) {
            this.cancelPendingClear();
            return;
        }

        const vids = this.vidsAt(target);
        if (vids.length === 0) {
            // Off the highlight but maybe heading for the grip — defer the teardown (linger).
            this.scheduleClear();
            return;
        }
        this.cancelPendingClear();
        this.show(vids);
    }

    /** Defer a teardown by GRIP_LINGER_MS unless one is already pending; cancelled if the pointer
     * reaches a grip or returns to the stack. */
    private scheduleClear(): void {
        if (this.clearTimer !== undefined || this.currentKey === null) return;
        this.clearTimer = window.setTimeout(() => {
            this.clearTimer = undefined;
            this.clear();
        }, GRIP_LINGER_MS);
    }

    private cancelPendingClear(): void {
        if (this.clearTimer !== undefined) {
            window.clearTimeout(this.clearTimer);
            this.clearTimer = undefined;
        }
    }

    /** Distinct vids of the highlight stack under the given node. */
    private vidsAt(target: EventTarget | null): VID[] {
        const node = target instanceof Node ? target : null;
        return distinctHighlightStack(node).map((h) => h.getAttribute('data-iaa-id') as VID);
    }

    private show(vids: VID[]): void {
        const key = [...vids].sort().join(',');
        if (key === this.currentKey) return;

        this.clear();
        this.captureOrigin();

        const viewport = this.viewportRect();
        const specs: GripSpec[] = [];
        for (const vid of vids) {
            const spec = this.resolveGrip(vid, viewport);
            if (spec) specs.push(spec);
        }

        // Spans that start at the same point on screen produce coincident knobs; fan each such group
        // so every knob stays individually targetable. Without inline labels, overlapping
        // spans genuinely share a start, so this is where fanning matters. In inline-label mode each
        // span's label tag occupies a distinct x, so anchors differ, every group is a singleton, and
        // layoutFan degenerates to the plain single-knob placement.
        for (const group of this.groupByAnchor(specs)) {
            this.layoutFan(group);
        }
        this.currentKey = key;
    }

    /** Resolve a vid to its grip anchor (span start on the first in-view line) and colour; null if
     * the span has no visible fragment (e.g. it is secluded / scrolled off). */
    private resolveGrip(vid: VID, viewport: DOMRect): GripSpec | null {
        const hls = this.resolveHighlights(vid);
        if (hls.length === 0) return null;

        // collectFragmentRects returns one rect per visual line, sorted top-to-bottom, so the first
        // in-view rect is the span's start on screen (anchoring to an off-screen earlier fragment of
        // a multi-line span would put the grip outside the viewport).
        const fragment = collectFragmentRects(hls).find(
            (r) => r.bottom >= viewport.top && r.top <= viewport.bottom
        );
        if (!fragment) return null;

        // Color-code each grip after its span so the grip→span mapping is unambiguous.
        // `--iaa-color` holds the *contrast* foreground (black/white via bgToFgColor) used for
        // label text — identical across most spans and useless for distinguishing them.
        // `--iaa-border-color` carries the span's actual color, which is what we want.
        return { vid, left: fragment.left, top: fragment.top, color: this.gripColor(hls[0]) };
    }

    /** Bucket specs whose anchors coincide (same span start on the same line, within tolerance). */
    private groupByAnchor(specs: GripSpec[]): GripSpec[][] {
        const groups: GripSpec[][] = [];
        for (const spec of specs) {
            const group = groups.find(
                (g) =>
                    Math.abs(g[0].left - spec.left) <= ANCHOR_TOLERANCE_PX &&
                    Math.abs(g[0].top - spec.top) <= ANCHOR_TOLERANCE_PX
            );
            if (group) group.push(spec);
            else groups.push([spec]);
        }
        return groups;
    }

    /**
     * Lay out one group of coincident grips as a symmetric upward arc centred on the span start.
     * A single grip keeps the original placement (the middle of the fan); additional knobs splay out
     * (±sin) and up (1−cos). The radius grows with the knob count so adjacent knobs keep a clickable
     * gap (chord ≥ FAN_MIN_CHORD_PX) even when several spans share a start.
     */
    private layoutFan(group: GripSpec[]): void {
        const n = group.length;
        const delta = n > 1 ? FAN_SPREAD_RAD / (n - 1) : 0;
        const radius =
            n > 1
                ? Math.max(FAN_BASE_RADIUS_PX, FAN_MIN_CHORD_PX / (2 * Math.sin(delta / 2)))
                : 0;

        group.forEach((spec, i) => {
            const angle = fanAngle(i, n, FAN_SPREAD_RAD);
            const left = spec.left - this.origin.left + radius * Math.sin(angle);
            const top =
                spec.top - this.origin.top - GRIP_RISE_PX - radius * (1 - Math.cos(angle));
            this.layer.appendChild(this.createGrip(spec, left, top));
        });
    }

    /** Create one grip knob at the given layer-relative position. */
    private createGrip(spec: GripSpec, left: number, top: number): HTMLElement {
        const grip = document.createElement('div');
        grip.className = GRIP_CLASS;
        // NOT data-iaa-id: highlights()/closestHighlight key on [data-iaa-id], so a grip carrying it
        // would be resolved as a drop target / annotation. data-grip-vid keeps the vid off that path.
        grip.dataset.gripVid = String(spec.vid);
        grip.style.left = left + 'px';
        grip.style.top = top + 'px';
        if (this.onGripPointerDown) {
            grip.addEventListener('mousedown', (e) => this.onGripPointerDown!(grip, e));
        }
        // Echo: hovering the grip emphasises its span. mouseenter/leave (not over/out)
        // because the grip has no children to bubble from and we want exactly the grip's own bounds.
        grip.addEventListener('mouseenter', () => this.echo(spec.vid));
        grip.addEventListener('mouseleave', () => this.clearEcho());
        if (spec.color) grip.style.setProperty('--iaa-grip-color', spec.color);
        return grip;
    }

    /** Emphasise every fragment of `vid` (the hovered grip's span); replaces any prior echo. */
    private echo(vid: VID): void {
        this.clearEcho();
        const hls = this.resolveHighlights(vid);
        hls.forEach((h) => h.classList.add(ECHO_CLASS));
        this.echoed = Array.from(hls);
    }

    /** Lift the current echo, if any. Idempotent. */
    private clearEcho(): void {
        for (const el of this.echoed) el.classList.remove(ECHO_CLASS);
        this.echoed = [];
    }

    private gripColor(highlight: Element): string {
        const style = getComputedStyle(highlight);
        return (
            style.getPropertyValue('--iaa-border-color').trim() ||
            style.getPropertyValue('--iaa-color').trim()
        );
    }

    private viewportRect(): DOMRect {
        const wrapper = this.root.closest('.i7n-wrapper') || this.root;
        return wrapper.getBoundingClientRect();
    }

    private captureOrigin(): void {
        const r = this.layer.getBoundingClientRect();
        this.origin = { left: r.left, top: r.top };
    }

    /** Remove all grips. Called by the main visualizer on re-render and scroll, and internally
     * before rebuilding for a new stack. */
    public clear(): void {
        this.cancelPendingClear();
        this.clearEcho();
        if (this.currentKey === null && !this.layer.firstChild) return;
        this.layer.replaceChildren();
        this.currentKey = null;
    }

    public destroy(): void {
        this.cancelPendingClear();
        this.layer.remove();
    }
}

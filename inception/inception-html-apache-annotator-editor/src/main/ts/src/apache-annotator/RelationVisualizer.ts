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
import './RelationVisualizer.scss';
import {
    AnnotatedText,
    AnnotationOutEvent,
    AnnotationOverEvent,
    type DiamAjax,
    Relation,
    type VID,
} from '@inception-project/inception-js-api';
import { ArrowMarkerPool } from './ArrowMarkerPool';
import { SVG_NS, svgEl } from './SvgUtilities';
import { fanAngle } from './Utilities';

/**
 * Renders relations for the Apache Annotator editor:
 *  - relations whose endpoints both fall inside an "effect-range band" are drawn as full arcs;
 *  - relations crossing the band boundary are drawn as directional (up/down) stubs with a
 *    clickable tip that jumps to the far endpoint;
 *  - pass-through relations (both ends outside the band) are not drawn.
 *
 * Ownership contract with ApacheAnnotatorVisualizer: the main visualizer owns *when* to
 * recompute (scroll/resize/reflow scheduling, debouncing). This class owns *what* to draw and
 * must NOT install its own recompute triggers — it is driven via render().
 */

const CLASS_RELATED = 'iaa-related';

/** Applied to an arc/stub while the mouse is over it (visual "active" state). */
const CLASS_ACTIVE = 'iaa-relation-active';

/**
 * Applied to the currently selected (focused) relation's arc/stub. Shares the active styling, but
 * is kept independent of hover so it persists when the mouse leaves (the hover handler only toggles
 * CLASS_ACTIVE).
 */
const CLASS_SELECTED = 'iaa-relation-selected';

/** On the overlay: relation labels are shown at rest. Without it, labels appear only on hover. */
const CLASS_LABELS_VISIBLE = 'iaa-relation-labels-visible';

/** The rubber-band arc drawn while a relation is being created by dragging. */
const CLASS_DRAG = 'iaa-relation-drag';

/**
 * Effect-range band size as a fraction of the seclusion margin.
 *
 * The band must sit strictly *between* the viewport and the non-secluded region. The non-secluded
 * region is the viewport grown by the seclusion margin (the IntersectionObserver `rootMargin` in
 * ApacheAnnotatorVisualizer). Choosing a fraction < 1 guarantees the band is contained in the
 * non-secluded region, so any endpoint we classify as "in band" is actually rendered (has a real
 * rect) rather than secluded — which is exactly the precondition the full-arc / stub-origin
 * geometry relies on. It also keeps on-screen arc density bounded independent of document length.
 */
const BAND_SECLUSION_FRACTION = 0.5;

/** Default stroke colour when a relation carries none. */
const DEFAULT_COLOR = '#888888';

/** Length (px) of a directional stub leaving its in-band endpoint. */
const STUB_LENGTH_PX = 26;

/** Total angular spread (radians) over which a span's stubs in one direction are fanned. */
const FAN_SPREAD_RAD = (60 * Math.PI) / 180;

/** Beyond this many stubs in one direction from one span, collapse into a single counted badge. */
const FAN_COLLAPSE_THRESHOLD = 3;

/**
 * Proximity tolerance (px) for deciding two arc endpoints are "the same point" when clustering arcs
 * that share an endpoint (see clusterArcs / sharesEndpoint). Two relations leaving the same span in
 * the same vertical direction anchor to the very same point (distance 0), so this is really a slack
 * for measurement noise and for near- (not exactly-) coincident endpoints. (Arcs leaving one span in
 * opposite directions anchor to opposite edges — see anchorOf — so they fall in separate clusters,
 * which is correct: they don't overlap.) Kept small (~one character) so unrelated relations
 * that merely pass close by are not merged. A fixed grid is unusable here — it splits points that
 * straddle a cell boundary no matter how close.
 */
const ARC_CLUSTER_TOL_PX = 14;

/** Bow (px) of the first arc in a cluster. Kept compact so the lowest arc hugs its own line. */
const ARC_BASE_BOW_PX = 12;

/**
 * Extra bow (px) added per stacked arc so each arc in a cluster curves higher than the last. A stacked
 * arc's apex (where its label sits) rises by only *half* the bow step, so this must be ≳ 2× the label
 * height (10px) for the labels not to overlap — hence 22, not the visually-sufficient ~12 the arcs
 * alone would need. At wider line spacing this is scaled up further (see ARC_BOW_REFERENCE_LINE_HEIGHT).
 */
const ARC_BOW_STEP_PX = 22;

/**
 * The per-stack bow step (not the base bow) is scaled by the content's line-height relative to this
 * reference (the `mid` preset's 1.8), so when the user picks a roomier line spacing the extra vertical
 * space is spent spreading stacked arcs — and their labels — further apart. The base bow stays fixed
 * so the first arc keeps a compact height and never rides up into the line above. Clamped to keep
 * extreme line-heights from going silly.
 */
const ARC_BOW_REFERENCE_LINE_HEIGHT = 1.8;
const ARC_BOW_SCALE_MIN = 0.6;
const ARC_BOW_SCALE_MAX = 1.2;

/**
 * Fraction of the bow kept for a perfectly vertical chord. The bow is a *perpendicular* offset, so on
 * a near-vertical arc it pushes the curve sideways — a fixed offset that the eye reads as strong
 * curvature there, while the same offset on a long horizontal arc is an invisible vertical sag. We
 * therefore scale the bow by the chord's horizontalness (cos²θ): full bow when horizontal, down to
 * this fraction when vertical. cos² (rather than |cos|) makes the falloff bite by the diagonal — a 45°
 * arc keeps ~60%, not ~78% — which is where the over-curving is already visible. Not zero, so stacked
 * vertical arcs still separate a little.
 */
const ARC_VERTICAL_BOW_MIN_FACTOR = 0.25;

/** Beyond this many arcs in one tight cluster, collapse into a single bundled arc + count badge. */
const ARC_COLLAPSE_THRESHOLD = 4;

/** Resolves the rendered highlight elements (the inline <mark>s) for an annotation VID. */
export type HighlightResolver = (vid: VID) => NodeListOf<Element>;

interface Endpoint {
    vid: VID;
    /**
     * Window-relative begin offset, or null when unknown (span absent from doc.spans, or a placeholder
     * with no offsets). Used only as the *fallback* for up/down direction when geometry is unavailable
     * — i.e. for a secluded out-of-band partner (see stubGoesUp). null is deliberate: a `?? 0` default
     * is indistinguishable from a genuine document-start endpoint and would bias every unknown partner
     * to compare as "earlier".
     */
    offset: number | null;
    /** Label of the endpoint span, used for the stub hover preview. */
    label: string;
    /**
     * In-band highlight fragments in viewport coordinates, one rect per visual line. A highlight
     * that wraps across lines yields several fragments; the anchor is chosen among them at draw
     * time (see chooseFragment / nearestFragmentPair). Anchoring to a single union bounding box
     * instead would put the attachment point in the empty gap between wrapped fragments, off the
     * actual ink. Empty when no fragment intersects the band.
     */
    rects: DOMRect[];
    /**
     * Vertical centre (viewport coords) of this endpoint's rendered fragments when it is *out of band*
     * but still rendered (not secluded). An out-of-band endpoint lies wholly above or below the band —
     * if any fragment reached the band it would be in-band — so this single y is enough to place a stub
     * partner up or down without consulting offsets. null when in-band (irrelevant) or secluded
     * (unmeasured — fall back to offset order).
     */
    outerMidY: number | null;
    inBand: boolean;
}

/** A single stub to be drawn from an in-band span toward an out-of-band partner. */
interface Stub {
    relation: Relation;
    near: Endpoint;
    far: Endpoint;
    color: string;
    goesUp: boolean;
}

/**
 * A full arc to be drawn between two in-band endpoints. The anchor points are resolved (and cached
 * here) *before* bucketing so arcs can be grouped by rendered geometry rather than by endpoint VID.
 * Coordinates are overlay-local (already passed through toLocal).
 */
interface Arc {
    relation: Relation;
    sAnchor: { x: number; y: number };
    tAnchor: { x: number; y: number };
}

export class RelationVisualizer {
    private root: Element;
    private ajax: DiamAjax;
    private resolveHighlights: HighlightResolver;

    private overlay: SVGSVGElement;
    private defs: SVGDefsElement;

    /** Overlay top-left in viewport coordinates, captured fresh at the start of each render. */
    private origin = { left: 0, top: 0 };
    /** Pools one arrowhead marker per color in <defs>; reset each render via clear(). */
    private arrowMarkers: ArrowMarkerPool;
    /** VIDs of the focused (selected) annotations for the current render; drives CLASS_SELECTED. */
    private focusedVids = new Set<VID>();
    /** Effect-range band (viewport coordinates) for the current render; consulted by anchorOf. */
    private band = { top: 0, bottom: 0 };

    /** Line-spacing-derived bow multiplier for the current render (see ARC_BOW_REFERENCE_LINE_HEIGHT). */
    private bowScale = 1;

    /**
     * Inline marks currently carrying CLASS_RELATED (the focus-coupling tint). Tracked so clear()
     * can strip the class from exactly these — O(coupled), at most a couple of elements — instead of
     * a full-subtree querySelectorAll over the content root on every render.
     */
    private coupledElements = new Set<Element>();

    // Per-render memoization (band and DOM are constant within one render() call, so a vid's
    // resolved highlights and endpoint geometry never change mid-render). Both are cleared at the
    // top of render(). A span that participates in several relations — or is both a source and a
    // target — is thus measured once instead of once per appearance.
    private highlightCache = new Map<VID, NodeListOf<Element>>();
    private endpointCache = new Map<VID, Endpoint>();
    /** Per-render cache of label-tag rects (inline-label mode); null = no in-band label for the vid. */
    private labelRectCache = new Map<VID, DOMRect | null>();

    /** The transient relation-creation rubber-band path, while a drag is in progress; else undefined. */
    private dragArc?: SVGPathElement;

    public constructor(root: Element, ajax: DiamAjax, resolveHighlights: HighlightResolver) {
        this.root = root;
        this.ajax = ajax;
        this.resolveHighlights = resolveHighlights;

        this.overlay = document.createElementNS(SVG_NS, 'svg');
        this.overlay.classList.add('iaa-relation-overlay');
        // The overlay is non-interactive; only the stub tips opt back into pointer events.
        this.overlay.style.pointerEvents = 'none';

        this.defs = document.createElementNS(SVG_NS, 'defs');
        this.overlay.appendChild(this.defs);
        this.arrowMarkers = new ArrowMarkerPool(this.defs);

        // Positioning context: the overlay is a child of `root` (the content layer that
        // also holds the inline <mark>s) and scrolls together with them. Coordinates are mapped via
        // toLocal(), which subtracts the overlay's *current* client rect — an SVG with no viewBox
        // maps user-space (0,0) to its own top-left corner, so this is correct regardless of the
        // overlay's offset parent or the scroll position, as long as the rect is read fresh each
        // render (see captureOrigin). overflow:visible (in the SCSS) keeps arcs from being clipped.
        this.root.appendChild(this.overlay);
    }

    // suspend()/resume() are driven by the main visualizer's scroll lifecycle, but they are
    // intentionally NO-OPS: the overlay is an absolutely-positioned child of the content root and
    // scrolls together with the inline marks, so it needs neither hiding nor recomputation during a
    // scroll. We deliberately do NOT keep a `suspended` flag to gate render() — doing so previously
    // blanked all relations, because the main's suspend/resume pairing is scroll-scoped and can
    // leave us suspended after the initial load.
    public suspend(): void {
        // intentionally no-op — see comment above
    }

    public resume(): void {
        // intentionally no-op — see comment above
    }

    /**
     * Control whether relation labels are shown at rest. When false, labels are hidden until the
     * relation is hovered (the active state reveals them — see RelationVisualizer.scss). This is a
     * pure CSS-class toggle on the persistent overlay, so it takes effect without a re-render.
     */
    public setLabelsAlwaysVisible(visible: boolean): void {
        this.overlay.classList.toggle(CLASS_LABELS_VISIBLE, visible);
    }

    // --- relation-creation rubber-band ------------------------------------------------------
    //
    // RelationDragController drives these; the band is drawn into the *shared* overlay so it uses
    // the same coordinate system and arrowhead pool as the real arcs. The overlay rect is captured
    // once at beginDragArc — a creation drag is a short gesture during which the content does not
    // scroll — and reused per move, so updateDragArc does no per-frame layout read.

    /** Begin a rubber-band drag arc in the given color. */
    public beginDragArc(color: string): void {
        // The overlay may be unsized if the document carries no relations yet; size + locate it now.
        this.sizeOverlayToContent();
        this.captureOrigin();
        this.clearDragArc();

        const stroke = color || DEFAULT_COLOR;
        this.dragArc = svgEl('path', CLASS_DRAG, {
            d: '',
            stroke,
            'marker-end': `url(#${this.arrowMarkers.markerFor(stroke)})`,
        });
        // Inert: it must never intercept the drop hit-test (document.elementFromPoint).
        this.dragArc.style.pointerEvents = 'none';
        this.overlay.appendChild(this.dragArc);
    }

    /** Re-plot the rubber band from a source point to the cursor (both in viewport coordinates). */
    public updateDragArc(
        fromClient: { x: number; y: number },
        toClient: { x: number; y: number }
    ): void {
        if (!this.dragArc) return;
        const s = this.toLocal(fromClient.x, fromClient.y);
        const t = this.toLocal(toClient.x, toClient.y);
        const c = bezierControlPoint(s.x, s.y, t.x, t.y, 18);
        this.dragArc.setAttribute('d', `M ${s.x} ${s.y} Q ${c.x} ${c.y} ${t.x} ${t.y}`);
    }

    /** Remove the rubber band (drag committed or cancelled). */
    public clearDragArc(): void {
        this.dragArc?.remove();
        this.dragArc = undefined;
    }

    public destroy(): void {
        this.clear();
        this.overlay.remove();
    }

    public clear(): void {
        // Reset the overlay to just an empty <defs>: replaceChildren drops every arc/stub/marker in
        // one operation (and re-parents the reused defs node), then empties the defs' pooled markers.
        this.overlay.replaceChildren(this.defs);
        this.defs.replaceChildren();
        this.arrowMarkers.clear();
        this.coupledElements.forEach((e) => e.classList.remove(CLASS_RELATED));
        this.coupledElements.clear();
    }

    /**
     * Redraw the relation overlay for the given document. Called from
     * ApacheAnnotatorVisualizer.renderAnnotations and on its recompute events.
     */
    public render(
        doc: AnnotatedText,
        bandInputs: { viewport: DOMRect; seclusionMargin: number },
        opts: { remeasureContent?: boolean } = {}
    ): void {
        // remeasureContent: the document content geometry may have changed (data load, line-spacing
        // reflow, resize), so the content-derived measurements must be refreshed. On a bare scroll
        // re-band the content has NOT reflowed — only the viewport (and thus the band) moved — so the
        // overlay's content extent and the line-height-derived bow scale are unchanged: reuse the
        // cached values and skip a forced layout flush (sizeOverlayToContent) plus a getComputedStyle
        // every frame. (The per-endpoint geometry below is still re-measured each frame.)
        const remeasureContent = opts.remeasureContent ?? true;

        this.clear();
        if (!doc.relations || doc.relations.size === 0) return;

        if (remeasureContent) this.sizeOverlayToContent();
        this.captureOrigin();

        this.highlightCache.clear();
        this.endpointCache.clear();
        this.labelRectCache.clear();

        const band = this.computeBand(bandInputs);
        this.band = band;
        if (remeasureContent) this.bowScale = this.measureBowScale();
        this.focusedVids = new Set(doc.markedAnnotations?.get('focus') || []);

        // Collect full arcs flat, then cluster by endpoint proximity *after* the loop (see
        // clusterArcs) so that not only stacked relations between the same two spans but also arcs to
        // distinct-yet-coincident endpoints (A→B and A→C with B,C on the same word) end up in one
        // cluster and get fanned apart. Stubs are grouped by their in-band span + direction so a
        // span's many out-of-band relations can be fanned / collapsed.
        const arcs: Arc[] = [];
        const stubGroups = new Map<string, Stub[]>();

        // Endpoint coupling is focus-only. The arcs/stubs already advertise every rendered relation,
        // so tinting all endpoints all the time is just noise; we couple only the focused relation as a
        // cheap "these two belong together" cue. Collected here but applied *after* the measurement
        // loop: coupleEndpoints writes classList on the inline marks, and doing that mid-loop would
        // invalidate layout and force the next endpoint's getClientRects() to re-flush (thrash).
        const coupledVids = new Set<VID>();

        for (const relation of doc.relations.values()) {
            const sourceVid = relation.arguments[0]?.targetId;
            const targetVid = relation.arguments[1]?.targetId;
            if (sourceVid == null || targetVid == null) continue;

            const source = this.resolveEndpoint(doc, sourceVid, band);
            const target = this.resolveEndpoint(doc, targetVid, band);

            if (this.focusedVids.has(relation.vid)) {
                coupledVids.add(sourceVid);
                coupledVids.add(targetVid);
            }

            const color = relation.color || DEFAULT_COLOR;

            if (source.inBand && target.inBand) {
                const sAnchor = this.anchorOf(source, target);
                const tAnchor = this.anchorOf(target, source);
                arcs.push({ relation, sAnchor, tAnchor });
            } else if (source.inBand) {
                this.queueStub(stubGroups, {
                    relation,
                    near: source,
                    far: target,
                    color,
                    goesUp: this.stubGoesUp(source, target),
                });
            } else if (target.inBand) {
                this.queueStub(stubGroups, {
                    relation,
                    near: target,
                    far: source,
                    color,
                    goesUp: this.stubGoesUp(target, source),
                });
            }
            // else: pass-through (both out of band) -> draw nothing.
        }

        // Apply coupling now that all measurement is done — no getClientRects() runs after this, so
        // the classList writes cannot trigger a layout re-flush.
        coupledVids.forEach((vid) => this.coupleEndpoints(vid));

        for (const cluster of clusterArcs(arcs)) {
            this.drawArcBucket(cluster);
        }
        for (const stubs of stubGroups.values()) {
            this.drawStubGroup(stubs);
        }
    }

    // --- geometry helpers -------------------------------------------------------------------

    private resolveEndpoint(
        doc: AnnotatedText,
        vid: VID,
        band: { top: number; bottom: number }
    ): Endpoint {
        const cached = this.endpointCache.get(vid);
        if (cached) return cached;

        const highlights = this.resolveHighlightsCached(vid);
        const span = doc.spans?.get(vid);
        const offset = span?.offsets?.[0]?.[0] ?? null;
        const label = span?.label || '';

        // Skip the getClientRects() forced layout for endpoints that cannot be in the band. An
        // in-band endpoint is by construction non-secluded: the band (viewport grown by
        // BAND_SECLUSION_FRACTION of the seclusion margin) is a strict subset of the non-secluded
        // region (viewport grown by the full margin — see computeBand). So if *every* highlight of
        // this annotation is secluded, all its fragments lie at least a full margin from the
        // viewport and would all be filtered out by the band test anyway — measuring them is pure
        // waste. closest() is a structural walk that does not flush layout, unlike getClientRects().
        // (We require *all* highlights to be secluded so an annotation straddling the seclusion
        // boundary, with a fragment still reaching into the band, is measured normally.)
        const allSecluded =
            highlights.length > 0 &&
            Array.from(highlights).every((h) => h.closest('.iaa-secluded') != null);

        // One rect per visual line (a wrapped highlight contributes several). Measure once, then keep
        // only the fragments that fall inside the band — those are the candidate anchor fragments.
        const allFragments = allSecluded ? [] : collectFragmentRects(highlights);
        const rects = allFragments.filter((r) => r.bottom >= band.top && r.top <= band.bottom);
        const inBand = rects.length > 0;

        // For an out-of-band-but-rendered endpoint, remember where it sits vertically so a stub can
        // point toward it from real geometry rather than offsets (see stubGoesUp). Its fragments all
        // lie on one side of the band, so their mid-y is unambiguous. Free: collectFragmentRects above
        // already measured them.
        const outerMidY =
            !inBand && allFragments.length > 0
                ? (Math.min(...allFragments.map((r) => r.top)) +
                      Math.max(...allFragments.map((r) => r.bottom))) /
                  2
                : null;

        const endpoint: Endpoint = { vid, offset, label, rects, outerMidY, inBand };
        this.endpointCache.set(vid, endpoint);
        return endpoint;
    }

    /**
     * Up/down direction for a stub from the in-band `near` endpoint toward its out-of-band `far`
     * partner: true points up (partner earlier/above), false down (partner later/below).
     *
     * Geometry first: a rendered (non-secluded) partner lies wholly above or below the band, so its
     * vertical position alone decides the direction — robust, and immune to the offset hazards above
     * (a missing/zero begin offset). Only when the partner is secluded (no geometry) do we fall back
     * to document order by begin offset, and only when both offsets are actually known; otherwise we
     * default downward (treat the partner as later in the document).
     */
    private stubGoesUp(near: Endpoint, far: Endpoint): boolean {
        if (far.outerMidY != null) {
            return far.outerMidY < this.nearAnchorMidY(near);
        }
        if (far.offset != null && near.offset != null) {
            return far.offset < near.offset;
        }
        return false;
    }

    /**
     * Vertical centre (viewport coords) of the anchor the stub will actually leave from, so the
     * direction test matches the drawn geometry in *both* label modes: the inline-label tag when
     * labels are on and in band (where full arcs and stubs attach — see anchorOf / drawStubGroup),
     * otherwise the in-band span fragments. labelRectFor is memoized for this render.
     */
    private nearAnchorMidY(near: Endpoint): number {
        const labelRect = this.labelRectFor(near.vid);
        if (labelRect) return labelRect.top + labelRect.height / 2;
        if (near.rects.length > 0) {
            const top = Math.min(...near.rects.map((r) => r.top));
            const bottom = Math.max(...near.rects.map((r) => r.bottom));
            return (top + bottom) / 2;
        }
        return 0;
    }

    /** Memoized {@link resolveHighlights} for the current render (cleared at the top of render()). */
    private resolveHighlightsCached(vid: VID): NodeListOf<Element> {
        let highlights = this.highlightCache.get(vid);
        if (!highlights) {
            highlights = this.resolveHighlights(vid);
            this.highlightCache.set(vid, highlights);
        }
        return highlights;
    }

    private computeBand(bandInputs: { viewport: DOMRect; seclusionMargin: number }): {
        top: number;
        bottom: number;
    } {
        // Band = viewport grown by a fraction of the seclusion margin, so it lies strictly between
        // the viewport and the non-secluded region. Coordinates are viewport-space, which
        // is what the endpoint rects (getBoundingClientRect) are measured in.
        const { viewport, seclusionMargin } = bandInputs;
        const margin = seclusionMargin * BAND_SECLUSION_FRACTION;
        return { top: viewport.top - margin, bottom: viewport.bottom + margin };
    }

    /** Convert a viewport-space point to overlay-local coordinates. */
    private toLocal(clientX: number, clientY: number): { x: number; y: number } {
        return { x: clientX - this.origin.left, y: clientY - this.origin.top };
    }

    private captureOrigin(): void {
        const r = this.overlay.getBoundingClientRect();
        this.origin = { left: r.left, top: r.top };
    }

    private sizeOverlayToContent(): void {
        const el = this.root as HTMLElement;
        // The overlay is an absolutely-positioned child of `root`, so its own size contributes to
        // root.scrollWidth/scrollHeight. Measuring without collapsing it first ratchets the overlay
        // up and never lets it shrink: once the content has been tall (e.g. a roomy line-spacing
        // preset) the oversized overlay keeps root.scrollHeight large even after the content reflows
        // shorter, so the user can scroll past the text into a blank region where no arcs are drawn
        // (#6108). Collapse to zero first so scroll* reflects the *content* extent, not the stale
        // overlay. scrollWidth/scrollHeight force a layout flush, so the collapse costs no extra reflow.
        this.overlay.setAttribute('width', '0');
        this.overlay.setAttribute('height', '0');
        this.overlay.style.width = '0';
        this.overlay.style.height = '0';
        const width = el.scrollWidth;
        const height = el.scrollHeight;
        this.overlay.setAttribute('width', String(width));
        this.overlay.setAttribute('height', String(height));
        this.overlay.style.width = width + 'px';
        this.overlay.style.height = height + 'px';
    }

    // --- full arcs --------------------------------------------------------------------------

    /**
     * Overlay-local anchor for `from`'s arc endpoint, attached at the chosen rect's horizontal centre.
     *
     * The edge (top vs bottom) is chosen by where the partner sits: if `toward` lies on a clearly
     * lower line we leave from the *bottom* edge, otherwise the *top* edge. This keeps a downward arc
     * from having to climb over the top of its own span before heading down (and an upward arc from
     * dropping below its span first). "Lower line" = the partner's nearest fragment's vertical centre
     * is below this fragment's bottom, i.e. they don't share a line.
     *
     * In inline-label mode we prefer the annotation's *label tag* (the "ACTOR"/"A"/"B" box): unlike
     * the span text, every overlapping annotation at the same position renders its own label, so the
     * label boxes are spatially distinct and give each relation a unique, non-overlapping attachment
     * point without any artificial endpoint offset. Otherwise — labels off, or the label is out of
     * band — we fall back to the span fragment nearest `toward` (so a wrapped highlight leaves real
     * ink instead of the empty gap a union bounding box would centre on). inBand guarantees `from`
     * carries at least one rect, so nearestFragmentPair always returns a defined pair.
     */
    private anchorOf(from: Endpoint, toward: Endpoint): { x: number; y: number } {
        const { source: fromRect, target: towardRect } = nearestFragmentPair(
            from.rects,
            toward.rects
        );
        const below = towardRect.top + towardRect.height / 2 > fromRect.bottom;

        const labelRect = this.labelRectFor(from.vid);
        if (labelRect) {
            const y = below ? labelRect.bottom : labelRect.top;
            return this.toLocal(labelRect.left + labelRect.width / 2, y);
        }
        const y = below ? fromRect.bottom : fromRect.top;
        return this.toLocal(fromRect.left + fromRect.width / 2, y);
    }

    /**
     * Rect of the annotation's inline label tag, or null when not applicable (labels off, or the
     * labelled first highlight is not in the band). The label is a `::before` pseudo-element on the
     * `.iaa-first-highlight` mark, so it cannot be measured directly; we instead take the box between
     * the mark's left edge and where its real text content begins. A Range over the mark's child
     * nodes covers only the text (pseudo-elements are not in the DOM), so its left edge marks the end
     * of the label. Result is memoized per render (a shared endpoint is measured once).
     */
    private labelRectFor(vid: VID): DOMRect | null {
        const cached = this.labelRectCache.get(vid);
        if (cached !== undefined) return cached;

        let result: DOMRect | null = null;
        const highlights = this.resolveHighlightsCached(vid);
        let firstMark: Element | undefined;
        for (const h of highlights) {
            if (
                h.classList.contains('iaa-inline-label') &&
                h.classList.contains('iaa-first-highlight')
            ) {
                firstMark = h;
                break;
            }
        }

        // markRect is the first *line* of the labelled mark (the line carrying the ::before label).
        const markRect = firstMark?.getClientRects()[0];
        // Only use the label when it actually sits in the band — otherwise a span whose start is far
        // off-screen would drag the arc endpoint out of view; fall back to the in-band fragment.
        if (markRect && markRect.bottom >= this.band.top && markRect.top <= this.band.bottom) {
            const range = document.createRange();
            range.selectNodeContents(firstMark!);
            // Use the range's FIRST client rect (the text run on the label's line), not its bounding
            // box: a wrapped mark's bounding box spans every line, so its left edge is the far-left
            // start of a *later* line, which would make the "label box" swallow the whole first line
            // and anchor the arc in the span text. The first client rect's left is where the real
            // text begins on the label's line, i.e. immediately after the ::before label.
            const contentRect = range.getClientRects()[0];
            const right =
                contentRect && contentRect.left > markRect.left ? contentRect.left : markRect.right;
            result = new DOMRect(
                markRect.left,
                markRect.top,
                right - markRect.left,
                markRect.height
            );
        }

        this.labelRectCache.set(vid, result);
        return result;
    }

    /**
     * Draw one cluster of full arcs that share an endpoint (see clusterArcs). We sort them stably,
     * then either fan them apart by bow or — only when the cluster is *tight* (every arc coincident
     * at both ends, i.e. truly stacked/redundant) and over threshold — collapse them into a single
     * bundled arc with a count badge that expands on hover. A loose cluster (arcs leaving a common
     * hub toward different places) is always fanned, never collapsed: a single representative arc
     * would misrepresent where the others actually go. A cluster containing the focused relation is
     * never collapsed, so the selection is always visible.
     *
     * Fanning is done *per side* of the hub: arcs leaving the shared endpoint in opposite horizontal
     * directions don't overlap, so each side restarts its bow tiers at the compact base bow (stackIndex
     * 0) instead of one side inheriting the other's tiers and floating needlessly high. This mirrors
     * the up/down split the stub code already does (see queueStub).
     */
    private drawArcBucket(arcs: Arc[]): void {
        arcs.sort(arcStackOrder);
        const hasFocused = arcs.some((a) => this.focusedVids.has(a.relation.vid));
        if (arcs.length > ARC_COLLAPSE_THRESHOLD && !hasFocused && isTightCluster(arcs)) {
            this.drawCollapsedArcBucket(arcs);
            return;
        }

        // Partition by the side each arc leaves the shared hub (left of it, right of it, or straight
        // up/down), then stack each side independently so every side's lowest arc hugs its line.
        const hub = clusterHub(arcs);
        const sides = new Map<number, Arc[]>();
        for (const arc of arcs) {
            const far = near(arc.sAnchor, hub) ? arc.tAnchor : arc.sAnchor;
            const side = Math.sign(far.x - hub.x);
            const group = sides.get(side);
            if (group) group.push(arc);
            else sides.set(side, [arc]);
        }
        // arcs is already in arcStackOrder, so each side's slice preserves that order.
        for (const group of sides.values()) {
            group.forEach((arc, i) => this.drawFullArc(arc, i, this.overlay));
        }
    }

    /**
     * Bow (and hence Bézier control point) for the i-th stacked arc. The endpoints are left exactly
     * where they were anchored — we deliberately do NOT offset them, so every arc stays attached to
     * its real endpoint (its label tag in inline-label mode; the span otherwise) rather than floating
     * above the text. Separation of stacked arcs therefore comes entirely from the bow, which grows
     * monotonically with the stack index (i = 0, 1, 2 …) and stays strictly positive (i = 0 lies on
     * the chord; bezierControlPoint biases every arc upward and applies Math.abs, so the stack never
     * folds onto itself). When several arcs share an endpoint the arrowheads necessarily coincide
     * there — they are told apart by hovering (each bow is individually targetable) and, on the side
     * where the endpoints differ, by the label-tag anchoring that keeps overlapping annotations'
     * attachment points distinct (see anchorOf).
     */
    /**
     * Bow multiplier from the content's current line-height, normalised to the `mid` preset so the
     * default spacing is 1× (see ARC_BOW_REFERENCE_LINE_HEIGHT). Computed as a unitless ratio
     * (line-height ÷ font-size) so it is independent of the absolute font size. `getComputedStyle`
     * usually resolves unitless line-heights to px, so we divide back out; the `'normal'` and bare-
     * number cases are handled for completeness.
     */
    private measureBowScale(): number {
        const cs = getComputedStyle(this.root as Element);
        const fontSize = parseFloat(cs.fontSize) || 16;
        const lh = cs.lineHeight;
        let ratio: number;
        if (lh === 'normal') ratio = 1.2;
        else if (lh.endsWith('px')) ratio = (parseFloat(lh) || fontSize) / fontSize;
        else ratio = parseFloat(lh) || ARC_BOW_REFERENCE_LINE_HEIGHT;
        const scale = ratio / ARC_BOW_REFERENCE_LINE_HEIGHT;
        return Math.min(ARC_BOW_SCALE_MAX, Math.max(ARC_BOW_SCALE_MIN, scale));
    }

    private arcStackGeometry(
        arc: Arc,
        stackIndex: number
    ): {
        s: { x: number; y: number };
        t: { x: number; y: number };
        c: { x: number; y: number };
    } {
        const s = arc.sAnchor;
        const t = arc.tAnchor;
        // Damp the bow as the chord tilts from horizontal (factor 1) toward vertical (factor
        // ARC_VERTICAL_BOW_MIN_FACTOR), keeping horizontal arcs unchanged while flattening the sideways
        // bulge a vertical arc would otherwise show (see ARC_VERTICAL_BOW_MIN_FACTOR).
        const len = Math.hypot(t.x - s.x, t.y - s.y);
        const horizontalness = len === 0 ? 0 : Math.abs(t.x - s.x) / len;
        const damp =
            ARC_VERTICAL_BOW_MIN_FACTOR +
            (1 - ARC_VERTICAL_BOW_MIN_FACTOR) * horizontalness * horizontalness;
        // Only the per-stack step is scaled by line spacing, NOT the base bow: the first arc
        // (stackIndex 0) keeps a fixed, compact height so it stays close to its own line at any
        // spacing, while wider spacing is spent spreading the *higher* arcs apart (see
        // ARC_BOW_REFERENCE_LINE_HEIGHT). Scaling the base too made the whole stack ride up into the
        // line above in the roomier presets.
        const distance = (ARC_BASE_BOW_PX + ARC_BOW_STEP_PX * stackIndex * this.bowScale) * damp;
        const c = bezierControlPoint(s.x, s.y, t.x, t.y, distance);
        return { s, t, c };
    }

    private drawFullArc(arc: Arc, stackIndex: number, container: SVGElement): void {
        const relation = arc.relation;
        const color = relation.color || DEFAULT_COLOR;
        const { s, t, c } = this.arcStackGeometry(arc, stackIndex);

        const d = `M ${s.x} ${s.y} Q ${c.x} ${c.y} ${t.x} ${t.y}`;
        const path = svgEl('path', 'iaa-relation-arc', {
            d,
            stroke: color,
            'marker-end': `url(#${this.arrowMarkers.markerFor(color)})`,
        });
        container.appendChild(path);

        // With no backend-supplied label, fall back to the parenthesised layer name rather than the
        // generic "ArgN" role label carried on the second argument. The label element is always
        // created (when there is text); whether it shows at rest is a CSS concern driven by the
        // overlay's labels-visible mode, and on hover it is revealed via the active state.
        const label =
            relation.label || (relation.layer?.name ? `(${relation.layer.name})` : undefined);
        const active: SVGElement[] = [path];
        if (label) {
            // Place the label on the curve's own apex — the point at t = 0.5, which is
            // 0.25*P0 + 0.5*C + 0.25*P2. We deliberately add NO per-index lift: the apex already
            // rises with the bow, and a separate lift detached the label from its arc (it grew at the
            // full bow step while the apex rises only ~half of it). Stacked arcs separate their labels
            // via the apex itself — vertically by the bow and horizontally when the endpoints differ.
            const mx = 0.25 * s.x + 0.5 * c.x + 0.25 * t.x;
            const my = 0.25 * s.y + 0.5 * c.y + 0.25 * t.y;
            const text = svgEl('text', 'iaa-relation-label', { x: mx, y: my, fill: color });
            text.textContent = label;
            container.appendChild(text);
            active.push(text);
        }

        // Invisible wide hit path on top of the thin arc so it is easy to hover; it drives the
        // active state (which also reveals the label when labels are hover-only) and the enter/leave
        // annotation events for the popover.
        const hit = svgEl('path', 'iaa-relation-hit', { d });
        this.attachRelationHover(hit, active, relation);
        container.appendChild(hit);
    }

    /**
     * Collapse a dense bucket into a single representative arc topped by a `×N` badge, hiding the
     * individual fanned arcs until hover (mirrors drawCollapsedFan for stubs). Hovering the
     * representative arc reveals the full fan; each revealed arc is then independently hoverable and
     * clickable. The representative's wide hit path overlaps the median fanned arc's hit path, so the
     * pointer stays within the group while travelling between them and the fan does not flicker.
     */
    private drawCollapsedArcBucket(arcs: Arc[]): void {
        const n = arcs.length;
        const color = arcs[0].relation.color || DEFAULT_COLOR;
        const g = svgEl('g', 'iaa-relation-arc-bundle');

        // Hidden expansion: the individual arcs, fanned, revealed on hover. The rects are already
        // measured, so this introduces no recompute.
        const expanded = svgEl('g', 'iaa-relation-bundle-expanded');
        expanded.style.display = 'none';
        arcs.forEach((arc, i) => this.drawFullArc(arc, i, expanded));

        // Collapsed: a single representative arc at the median bow, with a count badge at its apex.
        const collapsed = svgEl('g', 'iaa-relation-bundle-collapsed');
        const mid = (n - 1) / 2;
        const { s, t, c } = this.arcStackGeometry(arcs[0], mid);
        const d = `M ${s.x} ${s.y} Q ${c.x} ${c.y} ${t.x} ${t.y}`;
        const path = svgEl('path', 'iaa-relation-arc', {
            d,
            stroke: color,
            'marker-end': `url(#${this.arrowMarkers.markerFor(color)})`,
        });
        const mx = 0.25 * s.x + 0.5 * c.x + 0.25 * t.x;
        const my = 0.25 * s.y + 0.5 * c.y + 0.25 * t.y;
        const badge = svgEl('text', 'iaa-relation-fan-badge', { x: mx, y: my, fill: color });
        badge.textContent = `×${n}`;
        const title = svgEl('title');
        title.textContent = arcs
            .map((a) => a.relation.label || a.relation.layer?.name || '')
            .filter(Boolean)
            .join('\n');
        const hit = svgEl('path', 'iaa-relation-hit', { d });
        this.blockEditorGestures(hit);
        collapsed.appendChild(path);
        collapsed.appendChild(hit);
        collapsed.appendChild(badge);
        collapsed.appendChild(title);

        g.appendChild(expanded);
        g.appendChild(collapsed);
        this.wireExpandOnHover(g, expanded, collapsed);
        this.overlay.appendChild(g);
    }

    /** Reveal the fanned `expanded` group on hover, swapping it for the `collapsed` representative. */
    private wireExpandOnHover(g: SVGGElement, expanded: SVGElement, collapsed: SVGElement): void {
        g.addEventListener('mouseenter', () => {
            expanded.style.display = '';
            collapsed.style.display = 'none';
        });
        g.addEventListener('mouseleave', () => {
            expanded.style.display = 'none';
            collapsed.style.display = '';
        });
    }

    // --- stubs & fan-out --------------------------------------------------------------------

    private queueStub(groups: Map<string, Stub[]>, stub: Stub): void {
        // Group per in-band span and per direction: a span's up-stubs and down-stubs fan out
        // independently.
        const key = `${stub.near.vid}|${stub.goesUp ? 'up' : 'down'}`;
        const list = groups.get(key) || [];
        list.push(stub);
        groups.set(key, list);
    }

    private drawStubGroup(stubs: Stub[]): void {
        // TODO: cross-span stub collision is not handled. We only fan a single span's stubs here;
        // neighbouring spans' fans can still overlap. Global collision handling is deferred.
        const near = stubs[0].near;
        const goesUp = stubs[0].goesUp;

        // Leave from the label tag in inline-label mode (consistent with full arcs — the label is
        // unique per overlapping annotation), at its top edge for an up-stub and its bottom edge for a
        // down-stub. Otherwise leave from the in-band fragment closest to the travel direction: the
        // topmost fragment's top edge going up, the bottommost's bottom edge going down.
        const labelRect = this.labelRectFor(near.vid);
        let anchorClient: { x: number; y: number };
        if (labelRect) {
            anchorClient = {
                x: labelRect.left + labelRect.width / 2,
                y: goesUp ? labelRect.top : labelRect.bottom,
            };
        } else {
            if (!near.rects.length) return;
            const fragment = goesUp
                ? near.rects.reduce((a, b) => (b.top < a.top ? b : a))
                : near.rects.reduce((a, b) => (b.bottom > a.bottom ? b : a));
            anchorClient = {
                x: fragment.left + fragment.width / 2,
                y: goesUp ? fragment.top : fragment.bottom,
            };
        }
        const anchor = this.toLocal(anchorClient.x, anchorClient.y);

        if (stubs.length > FAN_COLLAPSE_THRESHOLD) {
            this.drawCollapsedFan(anchor, goesUp, stubs);
            return;
        }

        const n = stubs.length;
        stubs.forEach((stub, i) => {
            this.overlay.appendChild(this.fanStub(anchor, goesUp, stub, i, n));
        });
    }

    // Build one fanned stub: an angle symmetric around vertical, its tip, and the stub group with
    // its tip attached. The lean is purely cosmetic — direction accuracy to an off-screen point is
    // explicitly out of scope. The `n === 1` guard keeps the single-stub case (and any
    // future lowering of FAN_COLLAPSE_THRESHOLD) from dividing by zero.
    private fanStub(
        anchor: { x: number; y: number },
        goesUp: boolean,
        stub: Stub,
        i: number,
        n: number
    ): SVGGElement {
        const angle = fanAngle(i, n, FAN_SPREAD_RAD);
        const tip = {
            x: anchor.x + STUB_LENGTH_PX * Math.sin(angle),
            y: anchor.y + (goesUp ? -1 : 1) * STUB_LENGTH_PX * Math.cos(angle),
        };
        const g = this.makeStub(anchor, tip, stub, goesUp);
        this.attachTip(g, tip, stub);
        return g;
    }

    private drawCollapsedFan(
        anchor: { x: number; y: number },
        goesUp: boolean,
        stubs: Stub[]
    ): void {
        const color = stubs[0].color;
        const g = svgEl('g', 'iaa-relation-fan');

        // Hidden expansion: the individual stubs, fanned, revealed on hover. This is a
        // purely local visual toggle and introduces no recompute (the rects are already measured).
        const expanded = svgEl('g', 'iaa-relation-fan-expanded');
        expanded.style.display = 'none';
        const n = stubs.length;
        stubs.forEach((stub, i) => {
            expanded.appendChild(this.fanStub(anchor, goesUp, stub, i, n));
        });

        // Collapsed badge: a single straight stub topped by a count.
        const collapsed = svgEl('g', 'iaa-relation-fan-collapsed');
        const tipY = anchor.y + (goesUp ? -STUB_LENGTH_PX : STUB_LENGTH_PX);
        const line = svgEl('path', 'iaa-relation-stub', {
            d: `M ${anchor.x} ${anchor.y} L ${anchor.x} ${tipY}`,
            stroke: color,
        });
        const badge = svgEl('text', 'iaa-relation-fan-badge', {
            x: anchor.x,
            y: tipY + (goesUp ? -2 : 9),
            fill: color,
        });
        badge.textContent = `${goesUp ? '▴' : '▾'}${stubs.length}`;
        const title = svgEl('title');
        title.textContent = stubs
            .map((s) => s.far.label)
            .filter(Boolean)
            .join('\n');
        collapsed.appendChild(line);
        collapsed.appendChild(badge);
        collapsed.appendChild(title);

        // A transparent backdrop keeps the hover continuous across the whole fan region so the
        // pointer can travel from the badge to any expanded tip without collapsing.
        const half = STUB_LENGTH_PX;
        const hit = svgEl('rect', undefined, {
            x: anchor.x - half,
            y: goesUp ? anchor.y - half - 6 : anchor.y,
            width: half * 2,
            height: half + 6,
            fill: 'transparent',
        });
        hit.style.pointerEvents = 'auto';
        this.blockEditorGestures(hit);

        g.appendChild(hit);
        g.appendChild(expanded);
        g.appendChild(collapsed);
        this.wireExpandOnHover(g, expanded, collapsed);
        this.overlay.appendChild(g);
    }

    private makeStub(
        anchor: { x: number; y: number },
        tip: { x: number; y: number },
        stub: Stub,
        goesUp: boolean
    ): SVGGElement {
        const d = `M ${anchor.x} ${anchor.y} L ${tip.x} ${tip.y}`;
        const g = svgEl('g');
        const path = svgEl('path', 'iaa-relation-stub', {
            d,
            stroke: stub.color,
            'marker-end': `url(#${this.arrowMarkers.markerFor(stub.color)})`,
        });
        g.appendChild(path);

        // Wide invisible hit line so the thin stub is easy to hover. mouseenter/mouseleave on the
        // group also covers the tip target, so the whole stub reacts as one.
        const hit = svgEl('path', 'iaa-relation-hit', { d });
        g.appendChild(hit);

        this.attachRelationHover(g, [g], stub.relation);
        return g;
    }

    /**
     * Keep an interactive overlay target's mouse gestures from reaching the editor root, whose
     * mouseup handler would otherwise read a (stale/empty) selection and try to create a span — the
     * overlay sits after all text, so the resolved offset lands past the document end. preventDefault
     * on mousedown also stops a text selection from starting under the target; click still fires.
     */
    private blockEditorGestures(el: SVGElement): void {
        el.addEventListener('mousedown', (e) => {
            e.stopPropagation();
            e.preventDefault();
        });
        el.addEventListener('mouseup', (e) => e.stopPropagation());
    }

    /**
     * Make an arc/stub react to the mouse: toggle the active state on its visible elements and emit
     * the bubbling enter/leave annotation events the detail popover listens for. Uses
     * mouseenter/mouseleave (no per-child refiring) so a multi-element stub reacts as one unit, and
     * dispatches from the hover target so the event bubbles through the overlay to the editor root.
     */
    private attachRelationHover(
        target: SVGElement,
        active: SVGElement[],
        relation: Relation
    ): void {
        // The selected relation stays in the active look permanently (independent of hover).
        if (this.focusedVids.has(relation.vid)) {
            active.forEach((el) => el.classList.add(CLASS_SELECTED));
        }

        target.addEventListener('mouseenter', (e) => {
            active.forEach((el) => el.classList.add(CLASS_ACTIVE));
            target.dispatchEvent(new AnnotationOverEvent(relation, e));
        });
        target.addEventListener('mouseleave', (e) => {
            active.forEach((el) => el.classList.remove(CLASS_ACTIVE));
            target.dispatchEvent(new AnnotationOutEvent(relation, e));
        });

        // A click selects the relation. blockEditorGestures keeps the mousedown/mouseup/click from
        // reaching the editor root: in normal mode that would open the selector / try to create a
        // span; in keyboard mode it would move the (contentEditable) caret. stopPropagation on the
        // click additionally keeps it from the keyboard mode's caret-placement click handler.
        this.blockEditorGestures(target);
        target.addEventListener('click', (e) => {
            e.stopPropagation();
            this.ajax.selectAnnotation(relation.vid);
        });
    }

    /** Add a clickable, hoverable hit target at a stub tip that jumps to the far endpoint. */
    private attachTip(g: SVGGElement, tip: { x: number; y: number }, stub: Stub): void {
        const hit = svgEl('circle', 'iaa-relation-stub-tip', {
            cx: tip.x,
            cy: tip.y,
            r: 8,
            fill: 'transparent',
        });
        hit.style.pointerEvents = 'auto';
        hit.style.cursor = 'pointer';
        this.blockEditorGestures(hit);
        hit.addEventListener('click', (e) => {
            e.stopPropagation();
            this.ajax.selectAnnotation(stub.far.vid, { scrollTo: true });
        });
        const title = svgEl('title');
        title.textContent = stub.far.label || String(stub.far.vid);
        hit.appendChild(title);
        g.appendChild(hit);
    }

    private coupleEndpoints(vid: VID): void {
        this.resolveHighlightsCached(vid).forEach((e) => {
            e.classList.add(CLASS_RELATED);
            this.coupledElements.add(e);
        });
    }
}

/** Whether two points are within ARC_CLUSTER_TOL_PX of each other. */
function near(a: { x: number; y: number }, b: { x: number; y: number }): boolean {
    return Math.hypot(a.x - b.x, a.y - b.y) <= ARC_CLUSTER_TOL_PX;
}

/**
 * Stable stacking order within a cluster: the arc to a higher target sits below the arc to a lower one
 * (target y, then target x), with source y and finally VID as deterministic tie-breakers for arcs
 * whose anchors coincide.
 */
function arcStackOrder(a: Arc, b: Arc): number {
    const aVid = String(a.relation.vid);
    const bVid = String(b.relation.vid);
    return (
        a.tAnchor.y - b.tAnchor.y ||
        a.tAnchor.x - b.tAnchor.x ||
        a.sAnchor.y - b.sAnchor.y ||
        (aVid < bVid ? -1 : aVid > bVid ? 1 : 0)
    );
}

/**
 * The endpoint shared by every arc in a cluster (the point they fan out from). Tested against the
 * representative's two anchors; the one near an endpoint of *every* arc is the hub. Falls back to the
 * representative's source for a degenerate chain cluster (single-link clustering can in principle link
 * A–B and B–C with no common point), where no per-side split is meaningful anyway.
 */
function clusterHub(arcs: Arc[]): { x: number; y: number } {
    for (const candidate of [arcs[0].sAnchor, arcs[0].tAnchor]) {
        if (arcs.every((a) => near(a.sAnchor, candidate) || near(a.tAnchor, candidate))) {
            return candidate;
        }
    }
    return arcs[0].sAnchor;
}

/** Whether two arcs share at least one endpoint (within tolerance), in any orientation. */
function sharesEndpoint(a: Arc, b: Arc): boolean {
    return (
        near(a.sAnchor, b.sAnchor) ||
        near(a.tAnchor, b.tAnchor) ||
        near(a.sAnchor, b.tAnchor) ||
        near(a.tAnchor, b.sAnchor)
    );
}

/** Whether every arc in a cluster coincides with the first at *both* ends (a redundant stack). */
function isTightCluster(arcs: Arc[]): boolean {
    const rep = arcs[0];
    return arcs.every(
        (a) =>
            (near(a.sAnchor, rep.sAnchor) && near(a.tAnchor, rep.tAnchor)) ||
            (near(a.sAnchor, rep.tAnchor) && near(a.tAnchor, rep.sAnchor))
    );
}

/**
 * Group arcs that share an endpoint into clusters that will be fanned apart by bow, so arcs meeting
 * at a common point (relations from one source span, into one target span, multiple relations
 * between the same two spans, or A→B and A→C with B,C coincident) each get a distinct, individually
 * hoverable curve instead of stacking invisibly. Matching is unordered and pair-agnostic.
 *
 * Greedy single-link clustering against each cluster's representative (its first arc). We match on a
 * *shared* endpoint rather than requiring both endpoints near, because label-tag anchoring spreads
 * coincident targets onto their distinct label boxes (so A and B on one word land ~25px apart) — the
 * arcs would no longer be "near at both ends" yet still collide at the shared source and need
 * separating. Whether such a loose cluster may also be *collapsed* is decided separately
 * (isTightCluster). A fixed grid is unusable here: it splits points that straddle a cell boundary no
 * matter how close. n is tiny (arcs with both endpoints in the band), so the scan cost is moot.
 */
function clusterArcs(arcs: Arc[]): Arc[][] {
    const clusters: Arc[][] = [];
    for (const arc of arcs) {
        const hit = clusters.find((c) => sharesEndpoint(arc, c[0]));
        if (hit) hit.push(arc);
        else clusters.push([arc]);
    }
    return clusters;
}

/**
 * Collapse the client rects of an annotation's highlight elements into one rect per visual line.
 *
 * A highlight that wraps across lines reports several client rects (one per line fragment); several
 * adjacent highlight elements on the same line report several rects too. We union rects that share a
 * line so each returned rect spans exactly one line's highlighted run — giving a horizontal centre
 * that lies on actual ink rather than in the gap a full bounding box would straddle.
 *
 * Line membership is decided by comparing a fragment's vertical *centre* against a stable per-line
 * reference centre (the line's first fragment, by top), with a tolerance of half the shorter box.
 * We deliberately do NOT test whether the centre falls inside the accumulated union's band: the union
 * grows downward as fragments merge, so one tall fragment (an inline image, a larger-font child) whose
 * box overflows into the next line would keep swallowing the lines below it, merging two visual lines
 * into a single rect whose horizontal centre lands in the empty gap between them.
 */
export function collectFragmentRects(highlights: NodeListOf<Element>): DOMRect[] {
    const rects: DOMRect[] = [];
    highlights.forEach((h) => {
        for (const r of h.getClientRects()) {
            if (r.width > 0 || r.height > 0) rects.push(r);
        }
    });

    const lines: DOMRect[] = [];
    const refMids: number[] = [];
    const refHeights: number[] = [];
    for (const r of rects.sort((a, b) => a.top - b.top)) {
        const mid = r.top + r.height / 2;
        const i = lines.length - 1;
        if (i >= 0 && Math.abs(mid - refMids[i]) <= Math.min(refHeights[i], r.height) / 2) {
            lines[i] = unionRect(lines[i], r);
        } else {
            lines.push(r);
            refMids.push(mid);
            refHeights.push(r.height);
        }
    }
    return lines;
}

function unionRect(a: DOMRect, b: DOMRect): DOMRect {
    const left = Math.min(a.left, b.left);
    const top = Math.min(a.top, b.top);
    const right = Math.max(a.right, b.right);
    const bottom = Math.max(a.bottom, b.bottom);
    return new DOMRect(left, top, right - left, bottom - top);
}

/** The pair of fragments (one per side) whose centres are closest — the ends an arc should join. */
function nearestFragmentPair(
    source: DOMRect[],
    target: DOMRect[]
): { source: DOMRect; target: DOMRect } {
    let best = { source: source[0], target: target[0] };
    let bestDistSq = Infinity;
    for (const s of source) {
        const scx = s.left + s.width / 2;
        const scy = s.top + s.height / 2;
        for (const t of target) {
            const dx = scx - (t.left + t.width / 2);
            const dy = scy - (t.top + t.height / 2);
            const distSq = dx * dx + dy * dy;
            if (distSq < bestDistSq) {
                bestDistSq = distSq;
                best = { source: s, target: t };
            }
        }
    }
    return best;
}

/**
 * Perpendicular control point for a quadratic Bézier bowing `distance` px off the chord midpoint
 * (adapted from pdf-editor2's findBezierControlPoint).
 */
function bezierControlPoint(
    x1: number,
    y1: number,
    x2: number,
    y2: number,
    distance: number
): { x: number; y: number } {
    const cx = (x1 + x2) / 2;
    const cy = (y1 + y2) / 2;
    const dx = x2 - x1;
    const dy = y2 - y1;
    const len = Math.hypot(dx, dy);
    if (len === 0) return { x: cx, y: cy - distance };
    // Unit normal (perpendicular). Bias upward (negative y) so arcs bow over the text.
    let nx = -dy / len;
    let ny = dx / len;
    if (ny > 0) {
        nx = -nx;
        ny = -ny;
    }
    return { x: cx + nx * Math.abs(distance), y: cy + ny * Math.abs(distance) };
}

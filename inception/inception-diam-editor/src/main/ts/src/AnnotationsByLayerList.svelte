<script lang="ts">
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
        AnnotatedText,
        Annotation,
        AnnotationOverEvent,
        AnnotationOutEvent,
        DiamAjax,
        Relation,
        Span,
        Layer,
    } from "@inception-project/inception-js-api";
    import { compareOffsets } from "@inception-project/inception-js-api/src/model/Offsets";
    import LabelBadge from "./LabelBadge.svelte";
    import SpanText from "./SpanText.svelte";
    import { compareSpanText, groupBy, uniqueLayers } from "./Utils";
    import { sortByScore, recommendationsFirst } from "./AnnotationBrowserState"

    export let ajaxClient: DiamAjax;
    export let data: AnnotatedText;

    let groupedAnnotations: Record<string, Annotation[]>;
    let groups: { layer: Layer, collapsed: boolean }[]
    let collapsedGroups = new Set<number>()

    $: {
        const sortedLayers = uniqueLayers(data)

        groups = sortedLayers.map(layer => {
            return { layer: layer, collapsed: collapsedGroups.has(layer.id) };
        });

        const relations = data?.relations.values() || []
        const spans = data?.spans.values() || []
        groupedAnnotations = groupBy(
            [...spans, ...relations],
            (s) => s.layer.name
        )

        for (const items of Object.values(groupedAnnotations)) {
            items.sort((a, b) => {
                if (a instanceof Span && !(b instanceof Span)) {
                    return -1;
                }

                if (a instanceof Relation && !(b instanceof Relation)) {
                    return 1;
                }

                const aIsRec = a.vid.toString().startsWith("rec:")
                const bIsRec = b.vid.toString().startsWith("rec:")
                if ($sortByScore && aIsRec && !bIsRec) {
                    return $recommendationsFirst ? -1 : 1;
                }

                if (a instanceof Span && b instanceof Span) {
                    if ($sortByScore && aIsRec && bIsRec) {
                        return b.score - a.score;
                    }
                    return (
                        compareSpanText(data, a, b) ||
                        compareOffsets(a.offsets[0], b.offsets[0])
                    );
                }

                if (a instanceof Relation && b instanceof Relation) {
                    if ($sortByScore && aIsRec && bIsRec) {
                        return b.score - a.score;
                    }
                    return compareOffsets(
                        (a.arguments[0].target as Span).offsets[0],
                        (b.arguments[0].target as Span).offsets[0]
                    );
                }

                console.error("Unexpected annotation type combination", a, b);
            });
        }
    }

    function scrollTo(ann: Annotation) {
        ajaxClient.scrollTo({ id: ann.vid });
    }

    function mouseOverAnnotation(event: MouseEvent, annotation: Annotation) {
      event.target.dispatchEvent(new AnnotationOverEvent(annotation, event))
    }

    function mouseOutAnnotation(event: MouseEvent, annotation: Annotation) {
      event.target.dispatchEvent(new AnnotationOutEvent(annotation, event))
    }

    function toggleCollapsed(group) {
        if (!collapsedGroups.has(group.layer.id)) {
            collapsedGroups.add(group.layer.id)
        }
        else {
            collapsedGroups.delete(group.layer.id)
        }
        data = data // Trigger reactive update
    }

    function collapseAll() {
        for (const group of groups) {
            collapsedGroups.add(group.layer.id)
        }
        data = data // Trigger reactive update
    }

    function expandAll() {
        collapsedGroups.clear()
        data = data // Trigger reactive update
    }
</script>

{#if !data}
    <div class="m-auto d-flex flex-column justify-content-center">
        <div class="d-flex flex-row justify-content-center">
            <div class="spinner-border text-muted" role="status">
                <span class="visually-hidden">Loading...</span>
            </div>
        </div>
    </div>
{:else}
    <div class="d-flex flex-row flex-wrap">
        <div class="form-check form-switch mx-2">
            <input
                class="form-check-input"
                type="checkbox"
                role="switch"
                id="sortByScore"
                bind:checked={$sortByScore}
            />
            <label class="form-check-label" for="sortByScore"
                >Sort by score</label
            >
        </div>
        <div class="form-check form-switch mx-2" class:d-none={!$sortByScore}>
            <input
                class="form-check-input"
                type="checkbox"
                role="switch"
                id="recommendationsFirst"
                bind:checked={$recommendationsFirst}
            />
            <label class="form-check-label" for="recommendationsFirst"
                >Suggestions first</label
            >
        </div>
    </div>
    <div class="d-flex flex-row flex-wrap">
        <button class="btn btn-outline-secondary btn-sm p-0 m-1" style="width: 2em;" on:click={expandAll}>
            <i class="fas fa-caret-down"/>
        </button>
        <button class="btn btn-outline-secondary btn-sm p-0 m-1" style="width: 2em;" on:click={collapseAll}>
            <i class="fas fa-caret-down group-collapsed"/>
        </button>
    </div>
    <div class="flex-content fit-child-snug">
        {#if groups || groups?.length}
            <ul class="scrolling flex-content list-group list-group-flush">
                {#each groups as group}
                    <li class="list-group-item py-0 px-0 border-0">
                        <!-- svelte-ignore a11y-click-events-have-key-events -->
                        <div
                            class="px-2 py-1 bg-light-subtle fw-bold sticky-top border-top border-bottom"
                            on:click={() => toggleCollapsed(group)}
                        >
                            <button class="btn btn-link p-0" style="color: var(--bs-body-color)">
                                <i class="fas fa-caret-down d-inline-block" class:group-collapsed={group.collapsed}/>
                            </button>
                            <span>{group.layer.name}</span>
                        </div>
                        <ul class="px-0 list-group list-group-flush" class:d-none={group.collapsed}>
                            {#if groupedAnnotations[group.layer.name]}
                            {#each groupedAnnotations[group.layer.name] as ann}
                                <!-- svelte-ignore a11y-mouse-events-have-key-events -->
                                <li
                                    class="list-group-item list-group-item-action p-0 d-flex"
                                    on:mouseover={ev => mouseOverAnnotation(ev, ann)}
                                    on:mouseout={ev => mouseOutAnnotation(ev, ann)}
                                >
                                    <div
                                        class="text-secondary bg-light-subtle border-end px-2 d-flex align-items-center"
                                    >
                                        {#if ann instanceof Span}
                                            <div class="annotation-type-marker i7n-icon-span"/>
                                        {:else if ann instanceof Relation}
                                            <div class="annotation-type-marker i7n-icon-relation"/>
                                        {/if}
                                    </div>
                                    <!-- svelte-ignore a11y-click-events-have-key-events -->
                                    <div
                                        class="flex-grow-1 my-1 mx-2 position-relative overflow-hidden"
                                        on:click={() => scrollTo(ann)}
                                    >
                                        <div class="float-end labels">
                                            <LabelBadge
                                                annotation={ann}
                                                {ajaxClient}
                                                showText={true}
                                            />
                                        </div>

                                        {#if ann instanceof Span}
                                            <SpanText {data} span={ann} />
                                        {:else if ann instanceof Relation}
                                            <SpanText
                                                {data}
                                                span={ann.arguments[0].target}
                                            />
                                        {/if}
                                    </div>
                                </li>
                            {/each}
                            {:else}
                            <li class="list-group-item list-group-item-action p-2 text-center text-secondary bg-light">
                                No occurrences
                            </li>
                            {/if}
                        </ul>
                    </li>
                {/each}
            </ul>
        {/if}
    </div>
{/if}

<style lang="scss">
    .labels {
        background: linear-gradient(to right, transparent 0px, var(--bs-body-bg) 15px);
        padding-left: 20px;
        z-index: 10;
        position: relative;
    }

    .annotation-type-marker {
        width: 1em;
        text-align: center;
    }

    .list-group-flush > .list-group-item:last-child {
        border-bottom-width: 1px;
    }

    .group-collapsed {
        transform: rotate(-90deg);
    }
</style>

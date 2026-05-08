<script lang="ts">
    import { run } from 'svelte/legacy';

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
        AnnotationOverEvent,
        AnnotationOutEvent,
        Relation,
        Span,
        Layer,
        compareOffsets,
        type Annotation,
        type DiamAjax
    } from "@inception-project/inception-js-api";
    import LabelBadge from "./LabelBadge.svelte";
    import SpanText from "./SpanText.svelte";
    import { compareSpanText, debounce, filterAnnotations, groupRelationsBySource, groupBy, uniqueLayers } from "./Utils";
    import { stateStore } from "./AnnotationBrowserState.svelte"

    interface Props {
        ajaxClient: DiamAjax;
        data: AnnotatedText;
    }

    let { ajaxClient, data = $bindable() }: Props = $props();

    let collapsedGroups: number[] = $state([]);
    let filter = $state('');

    // Base grouping of annotations by layer
    let groupedAnnotations = $derived.by(() => {
        if (!data) return {};
        const relations = data.relations.values() || [];
        const spans = data.spans.values() || [];
        return groupBy(
            [...spans, ...relations],
            (s) => s.layer.name
        );
    });

    // Grouping relations by source
    let groupedRelations = $derived(groupRelationsBySource(data));

    // Groups with collapsed state
    let groups = $derived.by(() => {
        const sortedLayers = uniqueLayers(data);
        return sortedLayers.map(layer => {
            return { layer: layer, collapsed: collapsedGroups.includes(layer.id) };
        });
    });

    // Derived state for filtered and sorted annotations
    let filteredAndSortedAnnotations = $derived.by(() => {
        const result: Record<string, Annotation[]> = {};
        
        for (let [key, items] of Object.entries(groupedAnnotations)) {
            let filtered = filterAnnotations(data, items, filter);
            filtered.sort((a, b) => {
                if (a instanceof Span && !(b instanceof Span)) {
                    return -1;
                }

                if (a instanceof Relation && !(b instanceof Relation)) {
                    return 1;
                }

                const aIsRec = a.vid.toString().startsWith("rec:")
                const bIsRec = b.vid.toString().startsWith("rec:")
                if (stateStore.sortByScore && aIsRec && !bIsRec) {
                    return stateStore.recommendationsFirst ? -1 : 1;
                }

                if (a instanceof Span && b instanceof Span) {
                    if (stateStore.sortByScore && aIsRec && bIsRec) {
                        return (b.score ?? 0) - (a.score ?? 0);
                    }

                    return (
                        compareSpanText(data, a, b) ||
                        compareOffsets(a.offsets[0], b.offsets[0])
                    )
                }

                if (a instanceof Relation && b instanceof Relation) {
                    if (stateStore.sortByScore && aIsRec && bIsRec) {
                        return (b.score ?? 0) - (a.score ?? 0);
                    }

                    const targetA = a.arguments[0].target as Span
                    const targetB = b.arguments[0].target as Span
                    return compareOffsets(targetA.offsets[0], targetB.offsets[0]);
                }

                console.error("Unexpected annotation type combination", a, b);
                return 0;
            });
            result[key] = filtered;
        }
        
        return result;
    });

    function scrollToSpan(span: Span) {
        ajaxClient.scrollTo({ id: span.vid, offset: span.offsets[0] });
    }

    function mouseOverAnnotation(event: MouseEvent, annotation: Annotation) {
      event.target.dispatchEvent(new AnnotationOverEvent(annotation, event))
    }

    function mouseOutAnnotation(event: MouseEvent, annotation: Annotation) {
      event.target.dispatchEvent(new AnnotationOutEvent(annotation, event))
    }

    function toggleCollapsed(group) {
        if (!collapsedGroups.includes(group.layer.id)) {
            collapsedGroups.push(group.layer.id)
        }
        else {
            collapsedGroups = collapsedGroups.filter(id => id !== group.layer.id)
        }
    }

    function collapseAll() {
        for (const group of groups) {
            collapsedGroups.push(group.layer.id)
        }
    }

    function expandAll() {
        collapsedGroups = []
    }

    const updateFilter = debounce(newFilter => { filter = newFilter }, 300);

    // Function to handle input changes
    function handleFilterChange(event: Event) {
        updateFilter((event.target as HTMLInputElement).value)
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
        <input type="text" class="form-control rounded-0" oninput={handleFilterChange} placeholder="Filter"/>
    </div>
    <div class="d-flex flex-row flex-wrap">
        <div class="form-check form-switch mx-2">
            <input
                class="form-check-input"
                type="checkbox"
                role="switch"
                id="sortByScore"
                bind:checked={stateStore.sortByScore}
            />
            <label class="form-check-label" for="sortByScore"
                >Sort by score</label
            >
        </div>
        <div class="form-check form-switch mx-2" class:d-none={!stateStore.sortByScore}>
            <input
                class="form-check-input"
                type="checkbox"
                role="switch"
                id="recommendationsFirst"
                bind:checked={stateStore.recommendationsFirst}
            />
            <label class="form-check-label" for="recommendationsFirst"
                >Suggestions first</label
            >
        </div>
    </div>
    <div class="d-flex flex-row flex-wrap">
        <button class="btn btn-outline-secondary btn-sm p-0 m-1" style="width: 2em;" onclick={expandAll}>
            <i class="fas fa-caret-down"></i>
        </button>
        <button class="btn btn-outline-secondary btn-sm p-0 m-1" style="width: 2em;" onclick={collapseAll}>
            <i class="fas fa-caret-down group-collapsed"> </i>
        </button>
    </div>
    <div class="flex-content fit-child-snug">
        {#if groups || groups?.length}
            <ul class="scrolling flex-content list-group list-group-flush">
                {#each groups as group}
                    <li class="list-group-item py-0 px-0 border-0">
                        <!-- svelte-ignore a11y_click_events_have_key_events -->
                        <!-- svelte-ignore a11y_no_static_element_interactions -->
                        <div
                            class="px-2 py-1 bg-light-subtle fw-bold sticky-top border-top border-bottom"
                            onclick={() => toggleCollapsed(group)}
                        >
                            <button class="btn btn-link p-0" style="color: var(--bs-body-color)">
                                <i class="fas fa-caret-down d-inline-block" class:group-collapsed={group.collapsed}></i>
                            </button>
                            <span class="badge rounded-pill text-bg-secondary float-end m-1">{filteredAndSortedAnnotations[group.layer.name]?.length || 0}</span>
                            <span>{group.layer.name} {group.layer.type}</span>
                        </div>
                        <ul class="px-0 list-group list-group-flush" class:d-none={group.collapsed}>
                            {#if filteredAndSortedAnnotations[group.layer.name]}
                                {#each filteredAndSortedAnnotations[group.layer.name] as ann}
                                    <!-- svelte-ignore a11y_mouse_events_have_key_events -->
                                    {#if ann instanceof Span}
                                        <li
                                            class="list-group-item list-group-item-action p-0 d-flex"
                                            onmouseover={ev => mouseOverAnnotation(ev, ann)}
                                            onmouseout={ev => mouseOutAnnotation(ev, ann)}
                                        >
                                            <!-- svelte-ignore a11y_click_events_have_key_events -->
                                            <!-- svelte-ignore a11y_no_static_element_interactions -->
                                            <div class="flex-grow-1 my-1 mx-2 position-relative overflow-hidden"  onclick={() => scrollToSpan(ann)}>
                                                <div class="float-end labels">
                                                    <LabelBadge
                                                        {data}
                                                        annotation={ann}
                                                        {ajaxClient}
                                                        showText={true}
                                                    />
                                                </div>
                                                <SpanText {data} span={ann} />
                                            </div>
                                        </li>

                                        {@const relations = groupedRelations[`${ann.vid}`]}
                                        {#if relations}
                                            {#each relations as relation}
                                                {@const target = relation.arguments[1].target}
                                                <!-- svelte-ignore a11y_mouse_events_have_key_events -->
                                                <li
                                                    class="list-group-item list-group-item-action p-0 d-flex"
                                                    onmouseover={(ev) =>
                                                        mouseOverAnnotation(ev, relation)}
                                                    onmouseout={(ev) =>
                                                        mouseOutAnnotation(ev, relation)}
                                                >
                                                    <div
                                                        class="text-secondary bg-light-subtle border-end px-2 d-flex align-items-center"
                                                    >
                                                        <span>↳</span>
                                                    </div>
                                                    <!-- svelte-ignore a11y_click_events_have_key_events -->
                                                    <!-- svelte-ignore a11y_no_static_element_interactions -->
                                                    <div
                                                        class="flex-grow-1 my-1 mx-2 overflow-hidden"
                                                        onclick={() => scrollToSpan(target)}
                                                    >
                                                        <div class="float-end labels">
                                                            <LabelBadge
                                                                {data}
                                                                annotation={relation}
                                                                {ajaxClient}
                                                            />
                                                        </div>
                    
                                                        <SpanText {data} span={target} />
                                                    </div>
                                                </li>
                                            {/each}
                                        {/if}                                       
                                    {:else if ann instanceof Relation && group.layer.type === "relation"}
                                        <li
                                            class="list-group-item p-0 d-flex"
                                            onmouseover={ev => mouseOverAnnotation(ev, ann)}
                                            onmouseout={ev => mouseOutAnnotation(ev, ann)}
                                    >
                                            <!-- svelte-ignore a11y_click_events_have_key_events -->
                                            <!-- svelte-ignore a11y_no_static_element_interactions -->
                                            <div class="flex-grow-1 my-1 mx-0 position-relative overflow-hidden">
                                                <div class="me-2">
                                                    <LabelBadge
                                                        {data}
                                                        annotation={ann}
                                                        {ajaxClient}
                                                        showText={true}
                                                    />
                                                </div>
                                                <div class="d-flex flex-row list-group-item-action" onclick={() => scrollToSpan(ann.arguments[0].target)}>
                                                    <div class="text-secondary bg-light-subtle border-end px-2 d-flex align-items-center">
                                                        <span style="transform: rotate(90deg);">↳</span>
                                                    </div>
                                                    <SpanText
                                                        {data}
                                                        span={ann.arguments[0].target}
                                                    />
                                                </div>
                                                <div class="d-flex flex-row list-group-item-action" onclick={() => scrollToSpan(ann.arguments[1].target)}>
                                                    <div class="text-secondary bg-light-subtle border-end px-2 d-flex align-items-center">
                                                        <span>↳</span>
                                                    </div>
                                                    <SpanText
                                                        {data}
                                                        span={ann.arguments[1].target}
                                                    />
                                                </div>
                                            </div>
                                        </li>
                                    {/if}
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
    @import "@inception-project/inception-js-api/src/style/InceptionEditorIcons.scss";
    @import "@inception-project/inception-js-api/src/style/InceptionEditorColors.scss";

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

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
        compareOffsets,
        type Annotation,
        type DiamAjax,
    } from "@inception-project/inception-js-api";
    import LabelBadge from "./LabelBadge.svelte";
    import SpanText from "./SpanText.svelte";
    import { compareSpanText, debounce, filterAnnotations, groupBy, renderLabel, uniqueLabels } from "./Utils";
    import { stateStore } from "./AnnotationBrowserState.svelte"

    interface Props {
        ajaxClient: DiamAjax;
        data: AnnotatedText;
        pinnedGroups: string[];
    }

    let { ajaxClient, data = $bindable(), pinnedGroups }: Props = $props();

    let groupedAnnotations: Record<string, Annotation[]> = $state()
    let groups: { label: string, collapsed: boolean }[] = $state()
    let collapsedGroups: string[] = $state([]);
    let filter = $state('');

    run(() => {
        const sortedLabels = [...pinnedGroups, ...uniqueLabels(data).filter(v => !pinnedGroups.includes(v))]

        groups = sortedLabels.map(label => {
            return { label: label, collapsed: collapsedGroups.includes(label) };
        });

        const relations = data?.relations.values() || []
        const spans = data?.spans.values() || []
        groupedAnnotations = groupBy(
            [...spans, ...relations],
            (s) => renderLabel(data, s)
        )
    })

    $effect(() => {
            for (let [key, items] of Object.entries(groupedAnnotations)) {
            items = filterAnnotations(data, items, filter)
            items.sort((a, b) => {
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
                        return b.score - a.score;
                    }

                    return (
                        compareSpanText(data, a, b) ||
                        compareOffsets(a.offsets[0], b.offsets[0])
                    )
                }

                if (a instanceof Relation && b instanceof Relation) {
                    if (stateStore.sortByScore && aIsRec && bIsRec) {
                        return b.score - a.score;
                    }

                    const targetA = a.arguments[0].target as Span
                    const targetB = b.arguments[0].target as Span
                    return compareOffsets(targetA.offsets[0], targetB.offsets[0]);
                }

                console.error("Unexpected annotation type combination", a, b);
            });
            groupedAnnotations[key] = items
        }
    });

    function scrollTo(ann: Annotation) {
        if (ann instanceof Span) {
            ajaxClient.scrollTo({ id: ann.vid, offset: ann.offsets[0] });
            return;
        }

        ajaxClient.scrollTo({ id: ann.vid });
    }

    function mouseOverAnnotation(event: MouseEvent, annotation: Annotation) {
      event.target.dispatchEvent(new AnnotationOverEvent(annotation, event))
    }
    
    function mouseOutAnnotation(event: MouseEvent, annotation: Annotation) {
      event.target.dispatchEvent(new AnnotationOutEvent(annotation, event))
    }

    function toggleCollapsed(group) {
        if (!collapsedGroups.includes(group.label)) {
            collapsedGroups.push(group.label)
        }
        else {
            collapsedGroups = collapsedGroups.filter(label => label !== group.label)
        }
    }

    function collapseAll() {
        for (const group of groups) {
            collapsedGroups.push(group.label)
        }
    }

    function expandAll() {
        collapsedGroups = []
    }

    const updateFilter = debounce(newFilter => { filter = newFilter }, 300);

    // Function to handle input changes
    function handleFilterChange(event) {
        updateFilter(event.target.value)
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
            <i class="fas fa-caret-down group-collapsed"></i>
        </button>
    </div>
    <div class="flex-content fit-child-snug">
        {#if groups || groups?.length}
            <ul class="scrolling flex-content list-group list-group-flush">
                {#each groups as group}
                    <li class="list-group-item py-0 px-0 border-0">
                        <!-- svelte-ignore a11y_click_events_have_key_events -->
                        <div
                            class="px-2 py-1 bg-light-subtle fw-bold sticky-top border-top border-bottom"
                            onclick={() => toggleCollapsed(group)}
                        >
                            <button class="btn btn-link p-0" style="color: var(--bs-body-color)">
                                <i class="fas fa-caret-down d-inline-block" class:group-collapsed={group.collapsed}></i>
                            </button>
                            <span class="badge rounded-pill text-bg-secondary float-end m-1">{groupedAnnotations[group.label].length}</span>
                            <span>{group.label || "No label"}</span>
                        </div>
                        <ul class="px-0 list-group list-group-flush" class:d-none={group.collapsed}>
                            {#if groupedAnnotations[group.label]}
                                {#each groupedAnnotations[group.label] as ann}
                                    <!-- svelte-ignore a11y_mouse_events_have_key_events -->
                                    <li
                                        class="list-group-item list-group-item-action p-0 d-flex"
                                        onmouseover={ev => mouseOverAnnotation(ev, ann)}
                                        onmouseout={ev => mouseOutAnnotation(ev, ann)}
                                    >
                                        <div
                                            class="text-secondary bg-light-subtle border-end px-2 d-flex align-items-center"
                                        >
                                            {#if ann instanceof Span}
                                                <div class="annotation-type-marker i7n-icon-span"></div>
                                            {:else if ann instanceof Relation}
                                                <div class="annotation-type-marker i7n-icon-relation"></div>
                                            {/if}
                                        </div>
                                        <!-- svelte-ignore a11y_click_events_have_key_events -->
                                        <div
                                            class="flex-grow-1 my-1 mx-2 position-relative overflow-hidden"
                                            onclick={() => scrollTo(ann)}
                                        >
                                            <div class="float-end labels">
                                                <LabelBadge
                                                    {data}
                                                    annotation={ann}
                                                    {ajaxClient}
                                                    showText={false}
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

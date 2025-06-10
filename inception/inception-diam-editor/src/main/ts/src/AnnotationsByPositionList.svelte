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
    } from "@inception-project/inception-js-api";
    import type {
        Annotation,
        DiamAjax,
        Offsets,
    } from "@inception-project/inception-js-api";
    import LabelBadge from "./LabelBadge.svelte";
    import SpanText from "./SpanText.svelte";
    import {
        debounce,
        groupRelationsByPosition,
        groupSpansByPosition,
        uniqueOffsets,
    } from "./Utils";

    interface Props {
        ajaxClient: DiamAjax;
        data: AnnotatedText;
    }

    let { ajaxClient, data }: Props = $props();

    let groupedSpans: Record<string, Span[]> = $derived(groupSpansByPosition(data));
    let groupedRelations: Record<string, Relation[]> = $derived(groupRelationsByPosition(data));
    let sortedSpanOffsets: Offsets[] = $state();
    let filter = $state('');

    run(() => { 
        const normalizedFilter = filter.replace(/\s+/g, ' ').toLowerCase()
        sortedSpanOffsets = uniqueOffsets(data).filter(offset => {
            let coveredText = data.text?.substring(offset[0], offset[1]) || '';
            return coveredText.replace(/\s+/g, ' ').toLowerCase().includes(normalizedFilter)
        })
    });

    function scrollToSpan(span: Span) {
        ajaxClient.scrollTo({ id: span.vid, offset: span.offsets[0] });
    }

    function scrollToRelation(relation: Relation) {
        ajaxClient.scrollTo({ id: relation.vid, offset: relation.arguments[0].target.offsets[0] });
    }

    function mouseOverAnnotation(event: MouseEvent, annotation: Annotation) {
        event.target.dispatchEvent(new AnnotationOverEvent(annotation, event));
    }

    function mouseOutAnnotation(event: MouseEvent, annotation: Annotation) {
        event.target.dispatchEvent(new AnnotationOutEvent(annotation, event));
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
    <div class="flex-content fit-child-snug">
        {#if sortedSpanOffsets || sortedSpanOffsets?.length}
            <ul class="scrolling flex-content list-group list-group-flush">
                {#each sortedSpanOffsets as offsets}
                    {@const spans = groupedSpans[`${offsets}`]}
                    {@const firstSpan = spans[0]}
                    <!-- svelte-ignore a11y_mouse_events_have_key_events -->
                    <li class="list-group-item list-group-item-action p-0">
                        <!-- svelte-ignore a11y_click_events_have_key_events -->
                        <div
                            class="flex-grow-1 my-1 mx-2 overflow-hidden"
                            onclick={() => scrollToSpan(firstSpan)}
                        >
                            <div class="float-end labels">
                                {#each spans as span}
                                    <span
                                        onmouseover={(ev) =>
                                            mouseOverAnnotation(ev, span)}
                                        onmouseout={(ev) =>
                                            mouseOutAnnotation(ev, span)}
                                    >
                                        <LabelBadge
                                            {data}
                                            annotation={span}
                                            {ajaxClient}
                                        />
                                    </span>
                                {/each}
                            </div>
                            <SpanText {data} span={firstSpan} />
                        </div>
                    </li>

                    {@const relations = groupedRelations[`${offsets}`]}
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
                                <div
                                    class="flex-grow-1 my-1 mx-2 overflow-hidden"
                                    onclick={() => scrollToRelation(relation)}
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
                {/each}
            </ul>
        {/if}
    </div>
{/if}

<style lang="scss">
    .labels {
        background: linear-gradient(
            to right,
            transparent 0px,
            var(--bs-body-bg) 15px
        );
        padding-left: 20px;
        z-index: 10;
        position: relative;
    }

    .list-group-flush > .list-group-item:last-child {
        border-bottom-width: 1px;
    }
</style>

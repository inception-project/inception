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
        DiamAjax,
        Relation,
        Span,
    } from "@inception-project/inception-js-api";
    import { compareOffsets } from "@inception-project/inception-js-api/src/model/Offsets";
    import LabelBadge from "./LabelBadge.svelte";
    import SpanText from "./SpanText.svelte";
    import { compareSpanText, groupBy, uniqueLabels } from "./Utils";

    export let ajaxClient: DiamAjax;
    export let data: AnnotatedText;

    let groupedAnnotations: Record<string, Annotation[]>;
    let sortedLabels: string[];
    let sortByScore: boolean = true;
    let recommendationsFirst: boolean = false;

    $: sortedLabels = uniqueLabels(data);
    $: {
        const relations = data?.relations.values() || [];
        const spans = data?.spans.values() || [];
        groupedAnnotations = groupBy(
            [...spans, ...relations],
            (s) => s.label || ""
        );
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
                if (sortByScore && aIsRec && !bIsRec) {
                    return recommendationsFirst ? -1 : 1;
                }

                if (a instanceof Span && b instanceof Span) {
                    if (sortByScore && aIsRec && bIsRec) {
                        return b.score - a.score;
                    }
                    return (
                        compareSpanText(data, a, b) ||
                        compareOffsets(a.offsets[0], b.offsets[0])
                    );
                }

                if (a instanceof Relation && b instanceof Relation) {
                    if (sortByScore && aIsRec && bIsRec) {
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
</script>

{#if !data}
    <div class="mt-5 d-flex flex-column justify-content-center">
        <div class="d-flex flex-row justify-content-center">
            <div class="spinner-border text-muted" role="status">
                <span class="sr-only">Loading...</span>
            </div>
        </div>
    </div>
{:else}
    <div class="d-flex flex-column">
        <div class="form-check form-switch mx-2">
            <input
                class="form-check-input"
                type="checkbox"
                role="switch"
                id="sortByScore"
                bind:checked={sortByScore}
            />
            <label class="form-check-label" for="sortByScore"
                >Sort by score</label
            >
        </div>
        <div class="form-check form-switch mx-2" class:d-none={!sortByScore}>
            <input
                class="form-check-input"
                type="checkbox"
                role="switch"
                id="recommendationsFirst"
                bind:checked={recommendationsFirst}
            />
            <label class="form-check-label" for="recommendationsFirst"
                >Recommendations first</label
            >
        </div>
    </div>
    <div class="flex-content fit-child-snug">
        {#if sortedLabels || sortedLabels?.length}
            <ul class="scrolling flex-content list-group list-group-flush">
                {#each sortedLabels as label}
                    <li class="list-group-item py-0 px-0 border-0">
                        <div
                            class="px-2 py-1 bg.-light fw-bold sticky-top bg-light border-top border-bottom"
                        >
                            {label || "No label"}
                        </div>
                        <ul class="px-0 list-group list-group-flush">
                            {#each groupedAnnotations[label] as ann}
                                <li
                                    class="list-group-item list-group-item-action p-0 d-flex"
                                >
                                    <div
                                        class="text-secondary bg-light border-end px-2 d-flex align-items-center"
                                    >
                                        {#if ann instanceof Span}
                                            <div class="annotation-type-marker">
                                                ␣
                                            </div>
                                        {:else if ann instanceof Relation}
                                            <div class="annotation-type-marker">
                                                →
                                            </div>
                                        {/if}
                                    </div>
                                    <!-- svelte-ignore a11y-click-events-have-key-events -->
                                    <div
                                        class="flex-grow-1 py-1 px-2"
                                        on:click={() => scrollTo(ann)}
                                    >
                                        <div class="float-end">
                                            <LabelBadge
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
                        </ul>
                    </li>
                {/each}
            </ul>
        {/if}
    </div>
{/if}

<style lang="scss">
    .annotation-type-marker {
        width: 1em;
        text-align: center;
    }

    .list-group-flush > .list-group-item:last-child {
        border-bottom-width: 1px;
    }
</style>

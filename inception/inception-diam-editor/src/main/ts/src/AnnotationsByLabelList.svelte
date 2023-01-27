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
        DiamAjax,
        Relation,
        Span,
    } from "@inception-project/inception-js-api";
    import LabelBadge from "./LabelBadge.svelte";
    import SpanText from "./SpanText.svelte";
    import { groupRelationsByLabel, groupSpansByLabel, uniqueLabels } from "./Utils";

    export let ajaxClient: DiamAjax;
    export let data: AnnotatedText;

    let groupedSpans: Record<string, Span[]>;
    let sortedLabels: string[];

    $: groupedSpans = groupSpansByLabel(data);
    $: groupedRelations = groupRelationsByLabel(data);
    $: sortedLabels = uniqueLabels(data);

    function scrollToSpan (span: Span) {
        ajaxClient.scrollTo({ id: span.vid, offset: span.offsets[0] });
    }

    function scrollToRelation (relation: Relation) {
        ajaxClient.scrollTo({ id: relation.vid });
    }

    function handleDelete (ev: MouseEvent) {
      ajaxClient.deleteAnnotation(annotation.vid)
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
<div class="flex-content fit-child-snug">
    {#if sortedLabels || sortedLabels?.length}
        <ul class="scrolling flex-content list-group list-group-flush">
            {#each sortedLabels as label}
                {@const spans = groupedSpans[`${label}`] || []}
                {@const relations = groupedRelations[`${label}`] || []}
                <li class="list-group-item py-1 px-0 border-0">
                    <div class="px-2 py-1 bg.-light fw-bold sticky-top bg-body">
                        {label || 'No label'}
                    </div>
                    <ul class="ps-3 pe-0">
                        {#each spans as span}
                            <li class="list-group-item list-group-item-action p-0 d-flex">
                                <div class="text-secondary bg-light border-end px-2 d-flex align-items-center">
                                    <div class="annotation-type-marker">␣</div>
                                </div>
                                <!-- svelte-ignore a11y-click-events-have-key-events -->
                                <div class="flex-grow-1 py-1 px-2" on:click={() => scrollToSpan(span)}>
                                    <div class="float-end">
                                        <LabelBadge annotation={span} {ajaxClient} showText={false} />
                                        <button class="btn btn-outline-danger border border-0 btn-sm ms-1 fw-normal" on:click={handleDelete} title="Delete">
                                            <i class="fas fa-times"></i>
                                        </button>
                                    </div>
                
                                    <SpanText {data} span={span} />
                                </div>
                            </li>
                        {/each}
                        {#each relations as relation}
                            <li class="list-group-item list-group-item-action p-0 d-flex">
                                <div class="text-secondary bg-light border-end px-2 d-flex align-items-center">
                                    <div class="annotation-type-marker">→</div>
                                </div>
                                <!-- svelte-ignore a11y-click-events-have-key-events -->
                                <div class="flex-grow-1 py-1 px-2" on:click={() => scrollToRelation(relation)}>
                                    <div class="float-end">
                                        <LabelBadge annotation={relation} {ajaxClient} showText={false} />
                                        <button class="btn btn-outline-danger border border-0 btn-sm ms-1 fw-normal" on:click={handleDelete} title="Delete">
                                            <i class="fas fa-times"></i>
                                        </button>
                                    </div>

                                    <SpanText {data} span={relation.arguments[0].target} />
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
</style>
  
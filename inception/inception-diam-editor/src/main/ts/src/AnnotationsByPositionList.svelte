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
        Offsets,
        Relation,
        Span,
    } from "@inception-project/inception-js-api"
    import LabelBadge from "./LabelBadge.svelte"
    import SpanText from "./SpanText.svelte"
    import { groupRelationsByPosition, groupSpansByPosition, uniqueOffsets } from "./Utils"

    export let ajaxClient: DiamAjax;
    export let data: AnnotatedText;

    let groupedSpans: Record<string, Span[]>
    let groupedRelations: Record<string, Relation[]>
    let sortedSpanOffsets: Offsets[]

    $: groupedSpans = groupSpansByPosition(data)
    $: groupedRelations = groupRelationsByPosition(data)
    $: sortedSpanOffsets = uniqueOffsets(data)  
</script>

<div class="flex-content fit-child-snug">
    {#if sortedSpanOffsets || sortedSpanOffsets?.length}
        <ul class="scrolling flex-content list-group list-group-flush">
            {#each sortedSpanOffsets as offsets}
                {@const spans = groupedSpans[`${offsets}`]}
                {@const firstSpan = spans[0]}
                <li class="list-group-item p-0">
                    <div class="flex-grow-1 py-1 px-2">
                        <div class="float-end">
                            {#each spans as span}
                                <LabelBadge annotation={span} {ajaxClient} />
                            {/each}
                        </div>
                        <SpanText {data} span={firstSpan} />
                    </div>
                </li>

                {@const relations = groupedRelations[`${offsets}`]}
                {#if relations} 
                    {#each relations as relation}
                        {@const target = relation.arguments[1].target}
                        <li class="list-group-item p-0 d-flex">
                            <div class="text-secondary bg-light border-end px-2 d-flex align-items-center">
                                <span>↳</span>
                            </div>
                            <div class="flex-grow-1 py-1 px-2">
                                <div class="float-end">
                                    <LabelBadge annotation={relation} {ajaxClient} />
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

<style>
</style>

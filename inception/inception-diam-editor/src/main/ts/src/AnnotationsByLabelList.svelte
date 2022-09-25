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
        Span,
    } from "@inception-project/inception-js-api";
    import LabelBadge from "./LabelBadge.svelte";
    import SpanText from "./SpanText.svelte";
    import { groupRelationsByLabel, groupSpansByLabel, uniqueLabels, uniqueOffsets } from "./Utils";

    export let ajaxClient: DiamAjax;
    export let data: AnnotatedText;

    let groupedSpans: Record<string, Span[]>;
    let sortedLabels: string[];

    $: groupedSpans = groupSpansByLabel(data);
    $: groupedRelations = groupRelationsByLabel(data);
    $: sortedLabels = uniqueLabels(data);
</script>

<div class="flex-content fit-child-snug">
    {#if sortedLabels || sortedLabels?.length}
        <ul class="scrolling flex-content list-group list-group-flush">
            {#each sortedLabels as label}
                {@const spans = groupedSpans[`${label}`] || []}
                {@const relations = groupedRelations[`${label}`] || []}
                <li class="list-group-item py-1 px-0">
                    <div class="px-2 py-1 bg.-light fw-bold">
                        {label || 'No label'}
                    </div>
                    <ul class="ps-3 pe-0">
                        {#each spans as span}
                            <li class="list-group-item py-1 px-2">
                                <div class="float-end">
                                    <LabelBadge annotation={span} {ajaxClient} />
                                </div>
            
                                <SpanText {data} span={span} />
                            </li>
                        {/each}
                        {#each relations as relation}
                            <li class="list-group-item py-1 px-2">
                                <div class="float-end">
                                    <LabelBadge annotation={relation} {ajaxClient} />
                                </div>
            
                                <SpanText {data} span={relation.arguments[0].target} />
                            </li>
                        {/each}
                    </ul>
                </li>
            {/each}
        </ul>
    {/if}
</div>

<style>
</style>

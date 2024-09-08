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

    import { AnnotatedText, Span } from "@inception-project/inception-js-api";

    export let data: AnnotatedText
    export let span: Span

    const maxLength = 100
    const showTextAfter = true // Experimental

    $: begin = span.offsets[0][0]
    $: end = span.offsets[0][1]
    $: text = data.text.substring(begin, end).trim().replace(/\s+/g, ' ')
    $: textAfter = (showTextAfter && text.length < maxLength) ? 
        data.text.substring(end, end + maxLength - text.length).trim().replace(/\s+/g, ' ') : ''
</script>

{#if text.length === 0}
    <span class="text-muted">(empty)</span>
{:else if text.length > maxLength}
    <!-- The AnnotationDetailPopOver displays the text now provided that the lazy detail provider
         of the span layer sends it. - title="{text.substring(0,1000)}" -->
    <span>{text.substring(0, 50)}</span>
    <span class="text-muted trailing-text">…</span>
{:else}
    <span style="overflow-wrap: normal;">{text}</span>
    {#if textAfter.length > 0}
        <span class="text-muted trailing-text">{textAfter}</span>
    {/if}
{/if}

<style lang="scss">
    .trailing-text {
        font-weight: lighter;
        width: 0px;
        display: inline-block;
        white-space: nowrap;
    }
</style>

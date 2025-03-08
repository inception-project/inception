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
    import { AnnotatedText, Annotation, DiamAjax } from "@inception-project/inception-js-api";
    import { bgToFgColor } from "@inception-project/inception-js-api/src/util/Coloring";
    import { renderDecorations, renderLabel } from "./Utils";

    export let data: AnnotatedText
    export let annotation: Annotation;
    export let ajaxClient: DiamAjax;
    export let showText: boolean = true;

    $: backgroundColor = annotation.color || "var(--bs-secondary)";
    $: textColor = bgToFgColor(backgroundColor);
    $: hasError = annotation.comments?.find(comment => comment.type === 'error')
    $: hasInfo = annotation.comments?.find(comment => comment.type === 'info')

    function handleSelect(ev: MouseEvent) {
        ajaxClient.selectAnnotation(annotation.vid, { scrollTo: true });
    }

    function handleAccept(ev: MouseEvent) {
        ajaxClient.selectAnnotation(annotation.vid, { scrollTo: true });
    }

    function handleMerge(ev: MouseEvent) {
        ajaxClient.selectAnnotation(annotation.vid, { scrollTo: true });
    }

    function handleReject(ev: MouseEvent) {
        ajaxClient.triggerExtensionAction(annotation.vid);
    }

    function handleDelete(ev: MouseEvent) {
        ajaxClient.deleteAnnotation(annotation.vid);
    }

    function handleContextMenu(ev: MouseEvent) {
        if (ev.shiftKey) return

        ev.preventDefault()

        ajaxClient.openContextMenu(annotation.vid, ev);
    }
</script>

<!-- svelte-ignore a11y-click-events-have-key-events -->
{#if annotation.vid.toString().startsWith("rec:")}
    {@const decorations = renderDecorations(data, annotation)}
    <div class="btn-group mb-1 ms-0 btn-group-recommendation bg-body" role="group">
        <button
            type="button"
            class="btn-accept btn btn-outline-success btn-sm py-0 px-1"
            on:click={handleAccept}
            title="Accept"
        >
            <i class="far fa-check-circle" />
            {#if decorations}
                <span  class="me-1">{decorations}</span>
            {/if}
            {#if showText}
                <span class="label">{renderLabel(data, annotation)}</span>
            {/if}
            <!-- Negative scores used only for sorting/ranking but not shown -->
            {#if annotation.score && !annotation.hideScore}
                <span class="small font-monospace score"
                    >{annotation.score.toFixed(2)}</span
                >
            {/if}
        </button>
        <button
            type="button"
            class="btn-reject btn btn-outline-danger btn-sm py-0 px-1"
            on:click={handleReject}
            title="Reject"
        >
            <i class="far fa-times-circle" />
        </button>
    </div>
{:else if annotation.vid.toString().startsWith("cur:")}
    {@const decorations = renderDecorations(data, annotation)}
    <button
        type="button"
        class="btn-merge btn btn-colored btn-sm pt-0 mb-1 px-1 border-dark mb-1"
        style="color: {textColor}; background-color: {backgroundColor}"
        on:click={handleMerge}
        title="Merge"
    >
        <i class="fas fa-clipboard-check" />
        {#if decorations}
            <span  class="me-1">{decorations}</span>
        {/if}
        {#if showText}
            <span class="label">{renderLabel(data, annotation)}</span>
        {/if}
        <!-- Negative scores used only for sorting/ranking but not shown -->
        {#if annotation.score && !annotation.hideScore}
            <span class="small font-monospace score"
                >{annotation.score.toFixed(2)}</span
            >
        {/if}
    </button>
{:else}
    {@const decorations = renderDecorations(data, annotation)}
    <div class="input-group mb-1 ms-1 bg-body flex-nowrap text-break" role="group">
        {#if hasError || hasInfo}
            <span class="input-group-text py-0 px-1">
                {#if hasError}<i class="fas fa-exclamation-circle" style="color: var(--i7n-error-color)"></i>{/if}
                {#if hasInfo}<i class="fas fa-exclamation-circle" style="color: var(--i7n-info-color)"></i>{/if}
            </span>
        {/if}
        <button
            type="button"
            class="btn-select btn btn-colored btn-sm py-0 px-1 border-dark"
            style="color: {textColor}; background-color: {backgroundColor}"
            on:click={handleSelect}
            on:contextmenu={handleContextMenu}
            title="Select"
        >
            {#if decorations}
                <span  class="me-1">{decorations}</span>
            {/if}
            {#if showText}
                {renderLabel(data, annotation)}
            {:else}
                <i class="fas fa-crosshairs" />
            {/if}
        </button>
        <button
            type="button"
            class="btn-delete btn btn-colored btn-sm py-0 px-1 border-dark"
            style="color: {textColor}; background-color: {backgroundColor}"
            on:click={handleDelete}
            on:contextmenu={handleContextMenu}
            title="Delete"
        >
            <i class="far fa-times-circle" />
        </button>
    </div>
{/if}

<style lang="scss">
    .label {
        word-break: break-all;
    }

    .btn-colored:hover {
        filter: brightness(0.8);
    }

    .btn-group-recommendation {
        .btn-accept, .btn-merge {
            .score {
                color: var(--bs-secondary);
                &:hover {
                    color: var(--bs-white);
                }
            }
        }
    }
</style>

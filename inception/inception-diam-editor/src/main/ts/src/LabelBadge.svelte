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
    import { Annotation, DiamAjax } from "@inception-project/inception-js-api";
    import { bgToFgColor } from "@inception-project/inception-js-api/src/util/Coloring";

    export let annotation: Annotation;
    export let ajaxClient: DiamAjax;
    export let showText: boolean = true;

    $: backgroundColor = annotation.color || "var(--bs-secondary)";
    $: textColor = bgToFgColor(backgroundColor);

    function handleSelect(ev: MouseEvent) {
        ajaxClient.selectAnnotation(annotation.vid, { scrollTo: true });
    }

    function handleAccept(ev: MouseEvent) {
        ajaxClient.selectAnnotation(annotation.vid, { scrollTo: true });
    }

    function handleReject(ev: MouseEvent) {
        ajaxClient.triggerExtensionAction(annotation.vid);
    }

    function handleDelete(ev: MouseEvent) {
        ajaxClient.deleteAnnotation(annotation.vid);
    }

    function handleScrollTo (ev: MouseEvent) {
        ajaxClient.scrollTo({ id: annotation.vid });
    }
</script>

<!-- svelte-ignore a11y-click-events-have-key-events -->
{#if annotation.vid.toString().startsWith("rec:")}
    <div class="btn-group mb-0 ms-1" role="group">
        <!-- {#if showText}
            <button
                type="button"
                class="btn btn-outline-secondary btn-sm py-0 px-1 "
                on:click={handleScrollTo}
                title="Scroll to suggestion"
            >
                <i class="fas fa-crosshairs" />
            </button>
        {/if} -->
        <button
            type="button"
            class="btn btn-outline-success btn-sm py-0 px-1"
            on:click={handleAccept}
            title="Accept"
        >
            <i class="far fa-check-circle" />
            {#if showText}
              {annotation.label || "No label"}
            {/if}
          </button>
        <button
            type="button"
            class="btn btn-outline-danger btn-sm py-0 px-1"
            on:click={handleReject}
            title="Reject"
        >
            <i class="far fa-times-circle" />
        </button>
    </div>
{:else}
    <div class="btn-group mb-0 ms-1" role="group">
        <button
            type="button"
            class="btn btn-colored btn-sm py-0 px-1 border-dark"
            style="color: {textColor}; background-color: {backgroundColor}"
            on:click={handleSelect}
            title="Select"
        >
            {#if showText}
                {annotation.label || "No label"}
            {:else}
                <i class="fas fa-crosshairs" />
            {/if}
        </button>
        <button
            type="button"
            class="btn btn-colored btn-sm py-0 px-1 border-dark"
            style="color: {textColor}; background-color: {backgroundColor}"
            on:click={handleDelete}
            title="Delete"
        >
            <i class="far fa-times-circle" />
        </button>
    </div>
{/if}

<style lang="scss">
    .btn-colored:hover {
        filter: brightness(0.8);
    }
</style>

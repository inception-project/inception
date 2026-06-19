<!--
  Licensed to the Technische Universität Darmstadt under one
  or more contributor license agreements.  See the NOTICE file
  distributed with this work for additional information
  regarding copyright ownership.  The Technische Universität Darmstadt
  licenses this file to you under the Apache License, Version 2.0 (the
  "License"); you may not use this file except in compliance
  with the License.

  http://www.apache.org/licenses/LICENSE-2.0

  Unless required by applicable law or agreed to in writing, software
  distributed under the License is distributed on an "AS IS" BASIS,
  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
  See the License for the specific language governing permissions and
  limitations under the License.
-->
<script lang="ts">
    import type { DiamAjax } from '@inception-project/inception-js-api';
    import { annotatorState } from './ApacheAnnotatorState.svelte';
    import { createEventDispatcher } from 'svelte';

    interface Props {
        ajax: DiamAjax;
        userPreferencesKey: string;
        sectionSelector: string;
    }

    let { ajax, userPreferencesKey, sectionSelector }: Props = $props();

    let dispatch = createEventDispatcher();

    const defaultPreferences = {
        showLabels: true,
        showAggregatedLabels: false,
        showEmptyHighlights: false,
        showDocumentStructure: false,
        showImages: true,
        showTables: true,
        documentStructureWidth: 0.2,
    };

    $effect(() => {
        annotatorState.showDocumentStructure;
        annotatorState.documentStructureWidth;
        savePreferences();
        dispatch('documentStructurePreferencesChanged', {});
    });

    $effect(() => {
        annotatorState.showImages;
        annotatorState.showTables;
        savePreferences();
        dispatch('cssRenderingPreferencesChanged', {});
    });

    $effect(() => {
        annotatorState.showLabels;
        annotatorState.showAggregatedLabels;
        annotatorState.showEmptyHighlights;
        annotatorState.protectElements;
        savePreferences();
        dispatch('renderingPreferencesChanged', {});
    });

    $effect(() => {
        annotatorState.keyboardCursorEnabled;
        savePreferences();
        dispatch('keyboardCursorPreferencesChanged', {});
    });

    let preferencesDebounceTimeout: number | undefined = undefined;

    function savePreferences() {
        if (preferencesDebounceTimeout) {
            window.clearTimeout(preferencesDebounceTimeout);
            preferencesDebounceTimeout = undefined;
        }
        preferencesDebounceTimeout = window.setTimeout(() => {
            console.log('Saved preferences');
            ajax.savePreferences(userPreferencesKey, {
                showLabels: annotatorState.showLabels,
                showAggregatedLabels: annotatorState.showAggregatedLabels,
                showEmptyHighlights: annotatorState.showEmptyHighlights,
                showDocumentStructure: annotatorState.showDocumentStructure,
                showImages: annotatorState.showImages,
                showTables: annotatorState.showTables,
                keyboardCursorEnabled: annotatorState.keyboardCursorEnabled,
                documentStructureWidth: annotatorState.documentStructureWidth,
                protectElements: annotatorState.protectElements,
            });
        }, 250);
    }
</script>

<div
    class="bootstrap card-header border-0 border-bottom rounded-0 p-1 d-flex flex-row flex-wrap align-items-center"
    role="toolbar"
>
    <div class="btn-group btn-group-sm" role="group" aria-label="Display options">
        <input
            class="btn-check"
            type="checkbox"
            id="inlineLabelsEnabled"
            autocomplete="off"
            bind:checked={annotatorState.showLabels}
        />
        <label
            class="btn btn-outline-secondary"
            for="inlineLabelsEnabled"
            title="Inline labels"
            aria-label="Inline labels"
        >
            <i class="fas fa-tag"></i>
        </label>

        {#if sectionSelector}
            <input
                class="btn-check"
                type="checkbox"
                id="aggregatedLabelsEnabled"
                autocomplete="off"
                bind:checked={annotatorState.showAggregatedLabels}
            />
            <label
                class="btn btn-outline-secondary"
                for="aggregatedLabelsEnabled"
                title="Section labels"
                aria-label="Section labels"
            >
                <i class="fas fa-tags"></i>
            </label>
        {/if}

        <!--
        <input class="btn-check" type="checkbox" id="showEmptyHighlights" autocomplete="off" bind:checked={annotatorState.showEmptyHighlights}>
        <label class="btn btn-outline-secondary" for="showEmptyHighlights" title="Empty highlights" aria-label="Empty highlights"><i class="fas fa-square"></i></label>
        -->

        <input
            class="btn-check"
            type="checkbox"
            id="imagesEnabled"
            autocomplete="off"
            bind:checked={annotatorState.showImages}
        />
        <label
            class="btn btn-outline-secondary"
            for="imagesEnabled"
            title="Images"
            aria-label="Images"
        >
            <i class="fas fa-image"></i>
        </label>

        <input
            class="btn-check"
            type="checkbox"
            id="tablesEnabled"
            autocomplete="off"
            bind:checked={annotatorState.showTables}
        />
        <label
            class="btn btn-outline-secondary"
            for="tablesEnabled"
            title="Tables"
            aria-label="Tables"
        >
            <i class="fas fa-table"></i>
        </label>

        <input
            class="btn-check"
            type="checkbox"
            id="documentStructureEnabled"
            autocomplete="off"
            bind:checked={annotatorState.showDocumentStructure}
        />
        <label
            class="btn btn-outline-secondary"
            for="documentStructureEnabled"
            title="Outline"
            aria-label="Outline"
        >
            <i class="fas fa-list-ul"></i>
        </label>

        <input
            class="btn-check"
            type="checkbox"
            id="keyboardCursorEnabled"
            autocomplete="off"
            bind:checked={annotatorState.keyboardCursorEnabled}
        />
        <label
            class="btn btn-outline-secondary"
            for="keyboardCursorEnabled"
            title="Keyboard cursor"
            aria-label="Keyboard cursor"
        >
            <i class="fas fa-keyboard"></i>
        </label>
    </div>
</div>

<!-- svelte-ignore css_unused_selector -->
<style lang="scss">
    @use '../../node_modules/bootstrap/scss/bootstrap.scss';

    .bootstrap {
        // Ensure that Bootstrap properly applies to the component
        @extend body;
    }
</style>

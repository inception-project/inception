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
    import { annotatorState, type LineSpacing } from './ApacheAnnotatorState.svelte';
    import { createEventDispatcher } from 'svelte';

    const lineSpacingOptions: { value: LineSpacing; title: string; label: string }[] = [
        { value: 'low', title: 'Compact line spacing', label: 'S' },
        { value: 'mid', title: 'Normal line spacing', label: 'M' },
        { value: 'high', title: 'Relaxed line spacing', label: 'L' },
        { value: 'xhigh', title: 'Extra relaxed line spacing', label: 'X' },
    ];

    interface Props {
        ajax: DiamAjax;
        userPreferencesKey: string;
        sectionSelector: string;
    }

    let { ajax, userPreferencesKey, sectionSelector }: Props = $props();

    let dispatch = createEventDispatcher();

    // Svelte runs every $effect once on mount. The editor applies the loaded preferences
    // explicitly during setup, so those mount runs must not (a) PUT the just-loaded values
    // straight back (a no-op save) nor (b) re-trigger the line-spacing relayout the visualizer
    // constructor already performed. `initialized` (plain, not $state, so flipping it never
    // re-runs an effect) gates both; a trailing effect flips it after the first cycle. The
    // load-bearing mount dispatches (annotation load, hide-classes, navigator, relation labels)
    // are intentionally left to fire — only the save and the redundant line-spacing dispatch
    // are suppressed.
    let initialized = false;

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

    $effect(() => {
        annotatorState.lineSpacing;
        savePreferences();
        // Redundant on mount: the constructor already materialized geometry with the loaded
        // line spacing applied. Only dispatch on genuine user changes.
        if (!initialized) return;
        dispatch('lineSpacingPreferencesChanged', {});
    });

    $effect(() => {
        annotatorState.showRelationLabels;
        savePreferences();
        dispatch('relationLabelPreferencesChanged', {});
    });

    // Declared last so it runs after every effect above has completed its initial (mount) pass,
    // marking the end of setup. From here on, the effects act on genuine user changes.
    $effect(() => {
        initialized = true;
    });

    let preferencesDebounceTimeout: number | undefined = undefined;

    function savePreferences() {
        // Skip the mount-time pass: it would PUT the just-loaded values straight back.
        if (!initialized) return;

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
                lineSpacing: annotatorState.lineSpacing,
                showRelationLabels: annotatorState.showRelationLabels,
            });
        }, 250);
    }
</script>

<div
    class="bootstrap card-header border-0 border-bottom rounded-0 p-1 d-flex flex-row flex-wrap align-items-center"
    role="toolbar"
>
    <div class="btn-group btn-group-sm" role="group" aria-label="Outline">
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
    </div>

    <div class="btn-group btn-group-sm ms-1" role="group" aria-label="Labels">
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

        <input
            class="btn-check"
            type="checkbox"
            id="relationLabelsEnabled"
            autocomplete="off"
            bind:checked={annotatorState.showRelationLabels}
        />
        <label
            class="btn btn-outline-secondary"
            for="relationLabelsEnabled"
            title="Relation labels (otherwise shown on hover)"
            aria-label="Relation labels"
        >
            <i class="fas fa-diagram-project"></i>
        </label>
    </div>

    <div class="btn-group btn-group-sm ms-1" role="group" aria-label="Content">
        <!--
        <input class="btn-check" type="checkbox" id="showEmptyHighlights" autocomplete="off" bind:checked={annotatorState.showEmptyHighlights}>
        <label class="btn btn-outline-secondary" for="showEmptyHighlights" title="Empty highlights" aria-label="Empty highlights"><i class="fas fa-square"></i></label>
        -->

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
    </div>

    <div
        class="input-group input-group-sm w-auto ms-1"
        role="group"
        aria-label="Line spacing"
    >
        <span class="input-group-text" title="Line spacing" aria-hidden="true">
            <i class="fas fa-bars"></i>
        </span>

        {#each lineSpacingOptions as { value, title, label } (value)}
            <input
                class="btn-check"
                type="radio"
                name="lineSpacing"
                id="lineSpacing-{value}"
                autocomplete="off"
                checked={annotatorState.lineSpacing === value}
                onchange={() => (annotatorState.lineSpacing = value)}
            />
            <label
                class="btn btn-outline-secondary"
                for="lineSpacing-{value}"
                {title}
                aria-label={title}
            >
                {label}
            </label>
        {/each}
    </div>

    <div class="btn-group btn-group-sm ms-auto" role="group" aria-label="Keyboard">
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

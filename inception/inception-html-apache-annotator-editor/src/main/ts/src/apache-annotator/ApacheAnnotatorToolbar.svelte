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
  import { annotatorState } from './ApacheAnnotatorState.svelte'
  import { createEventDispatcher } from 'svelte'

  interface Props {
    ajax: DiamAjax;
    userPreferencesKey: string;
    sectionSelector: string;
  }

  let { ajax, userPreferencesKey, sectionSelector }: Props = $props();

  let dispatch = createEventDispatcher()

  const defaultPreferences = {
      showLabels: false,
      showAggregatedLabels: true,
      showEmptyHighlights: false,
      showDocumentStructure: false,
      showImages: true,
      showTables: true,
      documentStructureWidth: 0.2,
    }

    $effect(() => { 
      annotatorState.showDocumentStructure;
      annotatorState.documentStructureWidth;
      savePreferences()
      dispatch('documentStructurePreferencesChanged', {});
    });

    $effect(() => { 
      annotatorState.showImages;
      annotatorState.showTables;
      savePreferences()
      dispatch('cssRenderingPreferencesChanged', {});
    });

    $effect(() => { 
      annotatorState.showLabels;
      annotatorState.showAggregatedLabels;
      annotatorState.showEmptyHighlights;
      savePreferences()
      dispatch('renderingPreferencesChanged', {});
    });

    function savePreferences() {
      ajax.savePreferences(userPreferencesKey, {
        showLabels: annotatorState.showLabels,
        showAggregatedLabels: annotatorState.showAggregatedLabels,
        showEmptyHighlights: annotatorState.showEmptyHighlights,
        showDocumentStructure: annotatorState.showDocumentStructure,
        showImages: annotatorState.showImages,
        showTables: annotatorState.showTables,
        documentStructureWidth: annotatorState.documentStructureWidth,
      });
    }
</script>

<div class="bootstrap card card-header border-0 border-bottom rounded-0 p-1" role="toolbar">
  <div class="d-flex">
    <div class="form-check form-switch mx-2">
      <input class="form-check-input" type="checkbox" role="switch" id="inlineLabelsEnabled" bind:checked={annotatorState.showLabels}>
      <label class="form-check-label" for="inlineLabelsEnabled">Inline labels</label>
    </div>
    {#if sectionSelector}
      <div class="form-check form-switch mx-2">
        <input class="form-check-input" type="checkbox" role="switch" id="aggregatedLabelsEnabled" bind:checked={annotatorState.showAggregatedLabels}>
        <label class="form-check-label" for="aggregatedLabelsEnabled">Section labels</label>
      </div>
    {/if}
    <!--
    <div class="form-check form-switch mx-2">
      <input class="form-check-input" type="checkbox" role="switch" id="showEmptyHighlights" bind:checked={annotatorState.showEmptyHighlights}>
      <label class="form-check-label" for="showEmptyHighlights">Empties</label>
    </div>
    -->
    <div class="form-check form-switch mx-2">
      <input class="form-check-input" type="checkbox" role="switch" id="imagesEnabled" bind:checked={annotatorState.showImages}>
      <label class="form-check-label" for="imagesEnabled">Images</label>
    </div>
    <div class="form-check form-switch mx-2">
      <input class="form-check-input" type="checkbox" role="switch" id="tablesEnabled" bind:checked={annotatorState.showTables}>
      <label class="form-check-label" for="tablesEnabled">Tables</label>
    </div>
  </div>
</div>

<!-- svelte-ignore css_unused_selector -->
<style lang="scss">
  @import '../../node_modules/bootstrap/scss/bootstrap.scss';

  .bootstrap {
    // Ensure that Bootstrap properly applies to the component
    @extend body
  }
</style>

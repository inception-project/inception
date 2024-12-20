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
  import { showLabels, showAggregatedLabels, showEmptyHighlights, showImages, showTables } from './ApacheAnnotatorState'

  export let sectionSelector : string
</script>

<div class="bootstrap card card-header border-0 border-bottom rounded-0 p-1" role="toolbar">
  <div class="d-flex">
    <div class="form-check form-switch mx-2">
      <input class="form-check-input" type="checkbox" role="switch" id="inlineLabelsEnabled" bind:checked={$showLabels}>
      <label class="form-check-label" for="inlineLabelsEnabled">Inline labels</label>
    </div>
    {#if sectionSelector}
      <div class="form-check form-switch mx-2">
        <input class="form-check-input" type="checkbox" role="switch" id="aggregatedLabelsEnabled" bind:checked={$showAggregatedLabels}>
        <label class="form-check-label" for="aggregatedLabelsEnabled">Section labels</label>
      </div>
    {/if}
    <!--
    <div class="form-check form-switch mx-2">
      <input class="form-check-input" type="checkbox" role="switch" id="showEmptyHighlights" bind:checked={$showEmptyHighlights}>
      <label class="form-check-label" for="showEmptyHighlights">Empties</label>
    </div>
    -->
    <div class="form-check form-switch mx-2">
      <input class="form-check-input" type="checkbox" role="switch" id="imagesEnabled" bind:checked={$showImages}>
      <label class="form-check-label" for="imagesEnabled">Images</label>
    </div>
    <div class="form-check form-switch mx-2">
      <input class="form-check-input" type="checkbox" role="switch" id="tablesEnabled" bind:checked={$showTables}>
      <label class="form-check-label" for="tablesEnabled">Tables</label>
    </div>
  </div>
</div>

<!-- svelte-ignore css-unused-selector -->
<style lang="scss">
  @import '../../node_modules/bootstrap/scss/bootstrap.scss';

  .bootstrap {
    // Ensure that Bootstrap properly applies to the component
    @extend body
  }
</style>

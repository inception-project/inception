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
    import { Annotation, DiamAjax } from "@inception-project/inception-js-api"
    import { bgToFgColor } from "@inception-project/inception-js-api/src/util/Coloring"

    export let annotation: Annotation
    export let ajaxClient: DiamAjax
    export let showText: boolean = true

    $: backgroundColor = annotation.color || "var(--bs-secondary)"
    $: textColor = bgToFgColor(backgroundColor)

    function handleClick (ev: MouseEvent) {
      ajaxClient.selectAnnotation(annotation.vid, { scrollTo: true })
    }

    function handleAccept (ev: MouseEvent) {
      ajaxClient.selectAnnotation(annotation.vid, { scrollTo: true })
    }

    function handleReject (ev: MouseEvent) {
      ajaxClient.triggerExtensionAction(annotation.vid)
    }
</script>

<!-- svelte-ignore a11y-click-events-have-key-events -->
{#if annotation.vid.toString().startsWith('rec:')}
<div class="btn-group" role="group">
  <button type="button" class="btn btn-success py-0 px-1" on:click={handleAccept} title="Accept">
    <i class="far fa-check-circle"></i>
  </button>
  <button type="button" class="btn btn-danger py-0 px-1"  on:click={handleReject} title="Reject">
    <i class="far fa-times-circle"></i>
  </button>
</div>
{:else}  
<span
    class="badge border border-dark ms-1 fw-normal"
    on:click={handleClick}
    title={`${annotation.vid}@${annotation.layer.name}`}
    role="button" style="color: {textColor}; background-color: {backgroundColor}"
>
  {showText ? annotation.label || "No label" : "\u00A0"}
</span>
{/if}

<style>
  .badge {
    font-family: sans-serif;
    white-space: normal;
    --bs-badge-padding-y: 0.15rem;
  }
</style>

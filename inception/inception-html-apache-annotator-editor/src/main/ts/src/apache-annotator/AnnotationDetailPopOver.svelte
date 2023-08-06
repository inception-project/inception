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
<svelte:options accessors={true}/>

<script lang="ts">
  import { Annotation, AnnotationOverEvent, Relation, Span, DiamAjax, LazyDetailGroup } from '@inception-project/inception-js-api'
  import { onMount } from 'svelte'

  export let ajax : DiamAjax
  export let root : Element
  export let top : number = 0
  export let left : number = 0
  export let width : number = 400
  export let annotation : Annotation | undefined = undefined

  let popover : HTMLElement
  let detailGroups : LazyDetailGroup[]

  onMount(() => {
    // React to mouse hovering over annotation
    root.addEventListener(AnnotationOverEvent.eventType, (e: AnnotationOverEvent) => {
      if (!(e.originalEvent instanceof MouseEvent) || !(e.target instanceof HTMLElement)) return

      annotation = e.annotation
    })

    // Follow the mouse around
    root.addEventListener('mousemove', (e: MouseEvent) => {
      if (!annotation) return

      const rect = popover.getBoundingClientRect()

      const x = e.clientX
      const y = e.clientY + 16

      // Flip up if the popover is about to be clipped at the bottom
      if (y + rect.height > window.innerHeight) { 
        top = y - rect.height
      }
      else {
        top = y
      }

      // Shift left if the popover is about to be clipped on the right
      if (x + rect.width > window.innerWidth) { 
        left = Math.max(0, window.innerWidth - rect.width)
      }
      else {
        left = x
      }
    })

    // Hide popover when leaving the annotation
    root.addEventListener('mouseout', e => { 
      if (annotation) annotation = undefined 
    })
  })

  $: {
    if (annotation) {
      ajax.loadLazyDetails(annotation).then(response => detailGroups = response)
    }
  }
</script>

<div bind:this={popover} class="bootstrap popover position-fixed shadow" style:top="{top}px" style:left="{left}px" style:--width="{width}px" class:d-none={!annotation}>
  <div class="popover-header p-0 d-flex">
    <div class="border-end border-secondary px-1">
      <span class="annotation-type-marker"
        class:i7n-icon-span={annotation instanceof Span}
        class:i7n-icon-relation={annotation instanceof Relation}/>
    </div>
    <div class="flex-grow-1 px-1">{annotation?.layer?.name}</div>
    <div class="text-body-secondary px-1">ID: {annotation?.vid}</div>
  </div>
  {#if  annotation}
    <div class="popover-body p-1">
      {#each annotation.comments as comment}
        <div class="i7n-marker-{comment.type}">{comment.comment}</div>
      {/each}
      {#if detailGroups}
        {#each detailGroups as detailGroup}
          {#if detailGroup.title}
            <div class="fw-bold">{detailGroup.title}</div>
          {/if}
          {#each detailGroup.details as detail}
            <div><span class="fw-semibold">{detail.label}:</span> {detail.value}</div>
          {/each}
        {/each}
      {/if}
    </div>
  {/if}
</div>

<!-- svelte-ignore css-unused-selector -->
<style lang="scss">
  @import '../../node_modules/bootstrap/scss/bootstrap.scss';
  @import '../../node_modules/@inception-project/inception-js-api/src/style/InceptionEditorIcons.scss';
  @import '../../node_modules/@inception-project/inception-js-api/src/style/InceptionEditorColors.scss';

  .bootstrap {
    // Ensure that Bootstrap properly applies to the component
    @extend body
  }

  .popover {
    min-width: var(--width);
    width: var(--width);
    max-width: var(--width);
    pointer-events: none;
  }

  .annotation-type-marker {
    width: 1em;
    display: inline-block;
    text-align: center;
  }

  .i7n-marker-error {
    color: var(--i7n-error-color);
  }
</style>

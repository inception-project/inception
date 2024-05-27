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
  import { caretRangeFromPoint } from '@inception-project/inception-js-api'
  import { createEventDispatcher, onMount } from 'svelte'

  export let highlight: HTMLElement = undefined
  export let position: 'begin' | 'end'
  export let handle: HTMLElement = undefined
  export let marker: HTMLElement = undefined
  export let dragging = false

  let borderRadius = 0
  let borderWidth = 0
  let handleX = 0
  let handleY = 0
  let handleHeight = 0
  let handleWidth = 6
  let markerX = 0
  let markerY = 0
  let markerHeight = 0
  let opacity = 1
  let visibility = 'hidden'
  let markerVisibility = 'hidden'
  let scrollContainer : HTMLElement

  const dispatch = createEventDispatcher();

  onMount(() => {
    scrollContainer = handle.closest('.i7n-wrapper')
  })

  $: {
    const rects = highlight ? highlight.getClientRects() : []
    visibility = rects.length > 0 ? 'visible' : 'hidden'
    opacity = dragging ? 0.0 : 1

    if (highlight && scrollContainer && rects.length > 0) {
      const beginRect = rects[0]
      const endRect = rects[rects.length - 1]
      const scrollerContainerRect = scrollContainer.getBoundingClientRect()

      const style = window.getComputedStyle(highlight)
      borderWidth = 0
      style.borderWidth.split(' ').forEach(r => borderWidth = Math.max(borderWidth, parseInt(r)))

      let x = position === 'begin' ? beginRect.left : (endRect.right - borderWidth)
      x += scrollContainer.scrollLeft - scrollerContainerRect.left
      handleX = x

      let y = position === 'begin' ? beginRect.top : endRect.top
      y += scrollContainer.scrollTop - scrollerContainerRect.top
      handleY = y

      handleHeight = position === 'begin' ? beginRect.height : endRect.height

      if (!dragging) {
        borderRadius = 0
        style.borderRadius.split(' ').forEach(r => borderRadius = Math.max(borderRadius, parseInt(r)))
      } else {
        borderRadius = 0
      }
    }
  }

  function handleDragStart(event: MouseEvent) {
    event.preventDefault() // Avoid mouse down and dragging causing a text selection

    dragging = true
    // Unfortunately, Firefox support for drag events is broken and they provide bad clientX/Y
    // coordinates. So we have to use the dragover event instead.
    // https://bugzilla.mozilla.org/show_bug.cgi?id=505521#c95
    // Also in Safari, clientX/clientY in dragend is broken, so we check mouse movement
    scrollContainer.addEventListener('mousemove', handleDrag, { capture: true })
    scrollContainer.addEventListener('mouseup', handleDragEnd, { capture: true })
   }

  function handleDrag(event: DragEvent | MouseEvent) {
    if (!dragging) return

    if (event.buttons === 0) {
      // The mouse button has been released outside of the window
      cancelDrag()
      return
    }

    const range = caretRangeFromPoint(event.clientX, event.clientY)

    if (!range) {
      markerVisibility = 'hidden'
      return
    }

    const rect = range.getBoundingClientRect()
    const scrollerContainerRect = scrollContainer.getBoundingClientRect()
    markerX = rect.left + scrollContainer.scrollLeft - scrollerContainerRect.left
    markerY = rect.top + scrollContainer.scrollTop - scrollerContainerRect.top
    markerHeight = rect.height
    markerVisibility = 'visible'
  }

  function handleDragEnd(event: MouseEvent) {
    // Prevent the drag-end from turning into a mouse-up event which would trigger a selection
    // of the annotation
    event.stopPropagation()
    event.preventDefault()
    const validTarget = markerVisibility === 'visible'
    cancelDrag()
    if (!validTarget) return
    dispatch(`resize-handle-released`, { position, event })
  }

  function cancelDrag() {
    // See comment in handleDragStart
    scrollContainer.removeEventListener('mousemove', handleDrag, { capture: true })
    scrollContainer.removeEventListener('mouseup', handleDragEnd, { capture: true })
    dragging = false
    markerVisibility = 'hidden'
  }
</script>

<span bind:this={handle} class="handle" role="button"
  style:visibility style:opacity style:--border-width="{borderWidth}px" style:--handle-width="{handleWidth}px"
  style:top="{handleY}px" style:left="{handleX}px" style:height="{handleHeight}px"
  on:mousedown={handleDragStart}>
  <span class="inner-handle {position}" style:--border-radius="{borderRadius}px" style:--border-width="{borderWidth}px"></span>
</span>
<span bind:this={marker} class="marker" style:visibility="{markerVisibility}"
  style:top="{markerY}px" style:left="{markerX}px" style:height="{markerHeight}px"/>

<!-- svelte-ignore css-unused-selector -->
<style lang="scss">
  .handle {
    position: absolute;
    display: inline-flex;
    overflow: visible;
    box-sizing: border-box;
    width: var(--handle-width);
    cursor: grab;

    &:active {
      cursor: grabbing;
    }

    .inner-handle {
      pointer-events: none;
      display: inline-flex;
      border: var(--handle-width) rgba(0, 0, 0, 0.0);
      border-left-style: solid;
      border-right-style: solid;
      border-radius: var(--border-radius);
      min-width: calc(2 * var(--border-radius));

      &.begin {
        border-left-color: var(--i7n-focus-bounds-color);
        margin-left: calc(var(--handle-width) / -2);
      }

      &.end {
        border-right-color: var(--i7n-focus-bounds-color);
        margin-left: calc(-2 * var(--border-radius) - var(--handle-width));
      }

      &::before {
        content: '\00a0';
      }
    }
  }

  .marker {
    position: absolute;
    display: inline-flex;
    background-color: var(--i7n-focus-bounds-color);
    width: 4px;
    margin-left: -2px;
    pointer-events: none;
  }
</style>

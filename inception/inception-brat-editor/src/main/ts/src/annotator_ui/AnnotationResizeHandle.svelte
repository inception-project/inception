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
  import { run } from 'svelte/legacy';

  import { caretRangeFromPoint } from '@inception-project/inception-js-api'
  import { createEventDispatcher, onMount } from 'svelte'
  import { findClosestChunkElement } from '../visualizer/Visualizer'

  interface Props {
    highlight?: Element;
    position: 'begin' | 'end';
  }

  let {
    highlight = undefined,
    position,
    handle = $bindable(),
    marker = $bindable(),
    dragging = $bindable(false)
  }: Props = $props();

  let borderRadius = $state(0)
  let borderWidth = $state(0)
  let handleX = $state(0)
  let handleY = $state(0)
  let handleHeight = $state(0)
  let handleWidth = 6
  let markerX = $state(0)
  let markerY = $state(0)
  let markerHeight = $state(0)
  let opacity = $state(1)
  let visibility = $state('hidden')
  let markerVisibility = $state('hidden')
  let scrollContainer : Element = $state()

  const dispatch = createEventDispatcher();

  export const isDragging = () => dragging;
  export const getHighlight = () => highlight;
  export const setHighlight = (h) => highlight = h;
  export const getHandle = () => handle;

  onMount(() => {
    scrollContainer = handle.parentElement
  })

  run(() => {
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
  });

  function handleDragStart(event: MouseEvent) {
    event.preventDefault() // Avoid mouse down and dragging causing a text selection

    dragging = true
    // We do not get drag events over an SVG, so we need to listen to "mousemove" and "mouseup" events
    scrollContainer.addEventListener('mousemove', handleDrag, { capture: true })
    scrollContainer.addEventListener('mouseup', handleDragEnd, { capture: true })
   }

  function handleDrag(event: MouseEvent) {
    if (!dragging) return

    if (event.buttons === 0) {
      // The mouse button has been released outside of the window
      cancelDrag()
      return
    }

    const range = caretRangeFromPoint(event.clientX, event.clientY)

    const chunk = findClosestChunkElement(range.startContainer)
    if (!range || !chunk) {
      markerVisibility = 'hidden'
      return
    }

    const scrollerContainerRect = scrollContainer.getBoundingClientRect()
    const rect = range.getBoundingClientRect()

    // In Safari and Firefox, the bounding rect of the caret rect is completely broken and we need
    // to fix it up. We can detect if the caret rect is broken by checking if the caret rect is
    // withing the rect of the container element of the range (which should always be true!).
    let container = range.commonAncestorContainer instanceof Element ? range.commonAncestorContainer : range.commonAncestorContainer.parentElement
    const containerRect = container.getBoundingClientRect()

    const isRectConsistent = containerRect.left <= rect.x && rect.x <= containerRect.right 
      && containerRect.top <= rect.y && rect.y <= containerRect.bottom
    if (!isRectConsistent) {
      rect.x = rect.x - scrollerContainerRect.left + containerRect.left
      rect.y = rect.y - scrollerContainerRect.top + containerRect.top

      // Firefox has the additional problem that the caret rect always snaps to the begin of the 
      // ancestor range, so we need to adjust the rect to the actual caret position based on the 
      // offset within the container. This is a very rough approximation...
      const avgGlyphWidth = containerRect.width / container.textContent.length
      const isRtl = container.closest('svg[direction="rtl"]') !== null
      const tolerance = 5
      const expectedX = isRtl ? 
        containerRect.right - (avgGlyphWidth * range.startOffset) :
        containerRect.left + (avgGlyphWidth * range.startOffset)
      if (Math.abs(rect.x - expectedX) > tolerance) {
        rect.x = expectedX
      }
    }

    markerX = rect.left + scrollContainer.scrollLeft - scrollerContainerRect.left
    markerY = rect.top + scrollContainer.scrollTop - scrollerContainerRect.top
    markerHeight = rect.height || containerRect.height // Firefox does not return a height for the caret rect
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
  onmousedown={handleDragStart}>
  <span class="inner-handle {position}" style:--border-radius="{borderRadius}px" style:--border-width="{borderWidth}px"></span>
</span>
<span bind:this={marker} class="marker" style:visibility="{markerVisibility}"
  style:top="{markerY}px" style:left="{markerX}px" style:height="{markerHeight}px"></span>

<!-- svelte-ignore css_unused_selector -->
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

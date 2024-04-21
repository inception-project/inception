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
<svelte:options accessors={true} />

<script lang="ts">
    import {
        Annotation,
        AnnotationOverEvent,
        AnnotationOutEvent,
        Relation,
        Span,
        DiamAjax,
        LazyDetailGroup,
    } from "../..";
    import { onDestroy, onMount } from "svelte";

    export let ajax: DiamAjax;
    export let root: Element;
    export let top: number = 0;
    export let left: number = 0;
    export let width: number = 400;
    export let annotation: Annotation | undefined = undefined;

    const renderDelay = 10;
    const showDelay = 1000;
    const hideDelay = 500;
    const yOffset = 32;

    let popover: HTMLElement;
    let detailGroups: LazyDetailGroup[];
    let popoverTimeoutId: number | undefined;
    let loading = false;

    onMount(() => {
        root.addEventListener(AnnotationOverEvent.eventType, onAnnotationOver);
        root.addEventListener(AnnotationOutEvent.eventType, onAnnotationOut);
        root.addEventListener("mousemove", onMouseMove);
        root.addEventListener("mousedown", onMouseDown);
    });

    onDestroy(() => {
        root.removeEventListener(AnnotationOverEvent.eventType, onAnnotationOver);
        root.removeEventListener(AnnotationOutEvent.eventType, onAnnotationOut);
        root.removeEventListener("mousemove", onMouseMove);
        root.removeEventListener("mousedown", onMouseDown);
    })

    $: {
        if (annotation) {
            loading = true
            ajax.loadLazyDetails(annotation)
                .then((response) => { 
                    loading = false
                    detailGroups = response
                })
                .catch(() => {
                    loading = false
                    detailGroups = [{
                        title: "Error", 
                        details: [{label: "", value: "Unable to load details."
                    }]}]
            })
        }
    }

    function onMouseMove(e: MouseEvent) {
        if (!annotation) return;

        // if (!popoverTimeoutId && annotation) {
        //     annotation = undefined
        //     return
        // }

        movePopover(e);
    }

    function onMouseDown(e: MouseEvent) {
        if (popoverTimeoutId) {
            window.clearTimeout(popoverTimeoutId);
            popoverTimeoutId = undefined;
        }
        annotation = undefined;
    }

    function onAnnotationOver(e: AnnotationOverEvent) {
        const originalEvent = e.originalEvent;
        if (!(originalEvent instanceof MouseEvent))
            return;

        if (popoverTimeoutId) window.clearTimeout(popoverTimeoutId);
        popoverTimeoutId = undefined;

        if (annotation && annotation.vid !== e.annotation.vid) {
            annotation = e.annotation;
            detailGroups = undefined
        } else if (!annotation || annotation.vid !== e.annotation.vid) {
            showPopoverWithDelay(e.annotation, originalEvent)
        }
    }

    function onAnnotationOut(e: AnnotationOutEvent) {
        if (!(e.originalEvent instanceof MouseEvent))
            return;

        hidePopoverWithDelay();
    }

    function showPopoverWithDelay(ann: Annotation, originalEvent: MouseEvent) {
        popoverTimeoutId = window.setTimeout(() => {
                popoverTimeoutId = undefined;
                annotation = ann;
                popoverTimeoutId = window.setTimeout(() => {
                    movePopover(originalEvent);
                    popoverTimeoutId = undefined;
                }, renderDelay);
            }, showDelay);
    }

    function hidePopoverWithDelay() {
        if (popoverTimeoutId) {
            window.clearTimeout(popoverTimeoutId);
            popoverTimeoutId = undefined;
        }

        popoverTimeoutId = window.setTimeout(() => {
            if (annotation) {
                annotation = undefined;
            }
            popoverTimeoutId = undefined
        }, hideDelay);
    }

    function movePopover(e: MouseEvent) {
        const rect = popover.getBoundingClientRect();

        const x = e.clientX;
        const y = e.clientY;

        // Flip up if the popover is about to be clipped at the bottom
        if (y + rect.height + yOffset > window.innerHeight) {
            top = y - rect.height - yOffset;
        } else {
            top = y + yOffset;  
        }

        // Shift left if the popover is about to be clipped on the right
        if (x + rect.width > window.innerWidth) {
            left = Math.max(0, window.innerWidth - rect.width);
        } else {
            left = x;
        }
    }
</script>

<div
    bind:this={popover}
    class="bootstrap popover position-fixed shadow"
    style:top="{top}px"
    style:left="{left}px"
    style:--width="{width}px"
    class:d-none={!annotation}
>
    <div class="popover-header p-0 d-flex">
        <div class="border-end border-secondary px-1">
            <span
                class="annotation-type-marker"
                class:i7n-icon-span={annotation instanceof Span}
                class:i7n-icon-relation={annotation instanceof Relation}
            />
        </div>
        <div class="flex-grow-1 px-1">{annotation?.layer?.name}</div>
        <div class="text-body-secondary px-1">ID: {annotation?.vid}</div>
    </div>
    {#if annotation}
        <div class="popover-body p-0">
            {#if annotation.comments}
                <div class="p-1">
                    {#each annotation.comments as comment}
                        <div class="i7n-marker-{comment.type}">{comment.comment}</div>
                    {/each}
                </div>
            {/if}

            {#if loading}
                <div class="d-flex flex-column justify-content-center" class:border-top={annotation.comments}>
                    <div class="d-flex flex-row justify-content-center">
                        <div class="spinner-border spinner-border-sm text-muted" role="status">
                            <span class="visually-hidden">Loading...</span>
                        </div>
                    </div>
                </div>
            {:else if detailGroups}
                <ul class="list-group list-group-flush" class:border-top={annotation.comments}>
                    {#each detailGroups as detailGroup}
                        <li class="list-group-item p-1">
                            {#if detailGroup.title}
                                <div class="fw-bold">{detailGroup.title}</div>
                            {/if}
                            {#each detailGroup.details as detail}
                                <div class:ps-2={detailGroup.title}>
                                    <span class="fw-semibold">{detail.label}:</span>
                                    {detail.value}
                                </div>
                            {/each}
                        </li>
                    {/each}
               </ul>
            {/if}
        </div>
    {/if}
</div>

<!-- svelte-ignore css-unused-selector -->
<style lang="scss">
    @import "bootstrap/scss/bootstrap.scss";
    @import "../style/InceptionEditorIcons.scss";
    @import "../style/InceptionEditorColors.scss";

    .bootstrap {
        // Ensure that Bootstrap properly applies to the component
        @extend body;
    }

    .popover {
        width: var(--width);
        max-width: calc(min(var(--width), 90vw));
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

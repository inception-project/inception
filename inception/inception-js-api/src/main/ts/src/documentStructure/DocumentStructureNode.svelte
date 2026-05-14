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
    import { type TocLevel } from './DocumentStructureNavigatorUtils';
    import type { DocumentStructureStrategy } from './DocumentStructureStrategy';
    import { onMount } from 'svelte';
    import DocumentStructureNode from './DocumentStructureNode.svelte';

    function removeClassFromAncestors(
        start: Element,
        className: string,
        root?: Element | null
    ) {
        let current: Element | null = start.parentElement;
        while (current) {
            try {
                current.classList.remove(className);
            } catch {
                // ignore errors removing class from exotic nodes
            }
            if (root && current === root) break;
            current = current.parentElement;
        }
    }

    interface Props {
        root: Element;
        tocLevel: TocLevel;
        structure: DocumentStructureStrategy;
        initiallyExpanded?: boolean;
    }

    let { root, tocLevel, structure, initiallyExpanded = false }: Props = $props();
    // svelte-ignore state_referenced_locally
    let expandedState = $state(initiallyExpanded);
    let tocElement = $state<HTMLElement | undefined>(undefined);

    let hasChildren = $derived(!!tocLevel.children && tocLevel.children.length > 0);
    let displayTitle = $derived(
        ((tocLevel.label ? tocLevel.label + ' ' : '') + (tocLevel.title || '')).trim()
    );

    function scrollTo() {
        if (!tocLevel.element) return;
        const target = structure.scrollTarget(tocLevel.element) as HTMLElement;
        removeClassFromAncestors(tocLevel.element, 'iaa-secluded', root);
        target.scrollIntoView({ behavior: 'instant', block: 'start', inline: 'nearest' });
    }

    function toggle() {
        expandedState = !expandedState;
    }

    onMount(() => {
        tocLevel.tocElement = tocElement;
    });
</script>

{#if tocLevel.parent}
    <li bind:this={tocElement} class="node" class:expanded={expandedState}>
        <div class="entry">
            <button
                type="button"
                onclick={toggle}
                class="handle"
                class:invisible={!hasChildren}
                aria-expanded={hasChildren ? expandedState : undefined}
                aria-label="Toggle {displayTitle}"
                tabindex={hasChildren ? 0 : -1}
            >
                <span aria-hidden="true">▶</span>
            </button>
            <button
                type="button"
                onclick={scrollTo}
                class="link"
                title={displayTitle}
            >
                {#if tocLevel.label}
                    <span class="text-muted fw-lighter">{tocLevel.label}</span>
                {/if}
                {tocLevel.title}
            </button>
        </div>
        {#if hasChildren}
            <div class="children" class:open={expandedState} class:closed={!expandedState}>
                <ul>
                    {#each tocLevel.children as child (child)}
                        <DocumentStructureNode {root} tocLevel={child} {structure} />
                    {/each}
                </ul>
            </div>
        {/if}
    </li>
{:else}
    <ul class="root">
        {#if hasChildren}
            {#each tocLevel.children as child (child)}
                <DocumentStructureNode {root} tocLevel={child} {structure} />
            {/each}
        {/if}
    </ul>
{/if}

<!-- svelte-ignore css_unused_selector -->
<style lang="scss">
    @import '../../node_modules/bootstrap/scss/bootstrap.scss';

    :root {
        --animation-speed: 200ms;
    }

    .node {
        white-space: nowrap;
        list-style-type: none;
        transition:
            background-color var(--animation-speed) linear,
            border-left-color var(--animation-speed) linear;
        border-left-width: 3px;
        border-left-style: solid;
        border-left-color: var(--bs-body-bg);
        padding-left: 3px;

        &:global(.active) {
            border-left-color: var(--bs-primary);
            background-color: var(--bs-primary-bg-subtle);
        }
    }

    ul {
        padding-left: 1em;
        margin: 0px;
    }

    li {
        padding-top: 0.25em;
        padding-bottom: 0.25em;
        margin: 0px;
    }

    .root {
        padding-left: 5px;
    }

    .entry {
        display: flex;
        align-items: baseline;
        gap: 0.25em;
    }

    // Reset native <button> chrome so the buttons read as inline text but stay
    // keyboard-focusable with the browser's default focus outline.
    button {
        background: transparent;
        border: 0;
        padding: 0;
        margin: 0;
        color: inherit;
        font: inherit;
        text-align: left;
        cursor: pointer;
    }

    .handle {
        display: inline-block;
        transition: transform var(--animation-speed) linear;
        user-select: none;
        flex: 0 0 auto;
    }

    .link {
        flex: 1 1 auto;
        white-space: nowrap;
        overflow: hidden;
        text-overflow: ellipsis;
    }

    .children {
        transition: transform var(--animation-speed) linear;
        transform-origin: top;
    }

    .open {
        display: block;
        animation: expand var(--animation-speed);
    }

    .closed {
        display: none;
    }

    .expanded > .entry > .handle {
        transform: rotate(90deg);
    }

    @keyframes expand {
        from {
            transform: scaleY(0);
        }
        to {
            transform: scaleY(1);
        }
    }
</style>

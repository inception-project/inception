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
    import { onMount, onDestroy } from 'svelte';
    import {
        type TocLevel,
        generateTOC,
    } from './DocumentStructureNavigatorUtils';
    import type { DocumentStructureStrategy } from './DocumentStructureStrategy';
    import DocumentStructureNode from './DocumentStructureNode.svelte';

    interface Props {
        documentContainer: HTMLElement;
        structure: DocumentStructureStrategy;
    }

    let { documentContainer, structure }: Props = $props();

    let tocRoot: TocLevel = generateTOC(
        documentContainer,
        structure.sectionSelector,
        (s) => structure.extractTitle(s)
    );
    let observer: IntersectionObserver | undefined;
    let observeTargetToLevel = new Map<Element, TocLevel>();

    function collectObserveTargets(level: TocLevel) {
        if (level.parent && level.element) {
            observeTargetToLevel.set(level.element, level);
        }
        for (const child of level.children) {
            collectObserveTargets(child);
        }
    }

    function handleIntersect(entries: IntersectionObserverEntry[]) {
        for (const entry of entries) {
            const level = observeTargetToLevel.get(entry.target);
            const tocEl = level?.tocElement;
            if (!tocEl) continue;
            if (entry.isIntersecting) {
                tocEl.classList.add('active');
            } else {
                tocEl.classList.remove('active');
            }
        }
    }

    onMount(() => {
        collectObserveTargets(tocRoot);
        observer = new IntersectionObserver(handleIntersect, {
            root: null,
            rootMargin: '0px',
            threshold: 0,
        });
        for (const target of observeTargetToLevel.keys()) {
            observer.observe(target);
        }
    });

    onDestroy(() => {
        observer?.disconnect();
        observer = undefined;
        observeTargetToLevel.clear();
    });
</script>

<nav class="iaa-toc" aria-label="Document outline">
    {#if tocRoot.children.length === 0}
        <div class="iaa-toc-empty">No headings</div>
    {:else}
        <DocumentStructureNode
            root={documentContainer}
            tocLevel={tocRoot}
            {structure}
            initiallyExpanded={true}
        />
    {/if}
</nav>

<style lang="scss">
    .iaa-toc {
        font-size: 0.85rem;
        padding: 0.5rem;
        box-sizing: border-box;
        height: 100%;
    }

    .iaa-toc-empty {
        color: var(--bs-secondary, #888);
        font-style: italic;
        padding: 0.25rem;
    }
</style>

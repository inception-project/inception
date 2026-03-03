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

    import { mount, onMount, onDestroy } from "svelte";
    import {
        AnnotatedText,
        unpackCompactAnnotatedTextV2,
    } from "@inception-project/inception-js-api";
    import AnnotationDetailPopOver from '@inception-project/inception-js-api/src/widget/AnnotationDetailPopOver.svelte'
    import { factory } from "@inception-project/inception-diam";
    import { stateStore } from "./AnnotationBrowserState.svelte";
    import AnnotationsByPositionList from "./AnnotationsByPositionList.svelte";
    import AnnotationsByLabelList from "./AnnotationsByLabelList.svelte";
    import AnnotationsByLayerList from "./AnnotationsByLayerList.svelte";

    let { wsEndpointUrl, csrfToken, topicChannel, ajaxEndpointUrl, pinnedGroups, userPreferencesKey } = $props();

    let element: HTMLElement | undefined = $state(undefined);
    let connected = false;

    let defaultPreferences = {
        mode: "by-label",
        sortByScore: true,
        recommendationsFirst: false,
    };
    let preferences = Object.assign({}, defaultPreferences);
    let modes = {
        "by-position": "Group by position",
        "by-label": "Group by label",
        "by-layer": "Group by layer",
    };
    let tooManyAnnotations = $state(false);

    let data: AnnotatedText | undefined = $state(undefined);

    let wsClient = factory().createWebsocketClient();
    wsClient.onConnect = () =>
        wsClient.subscribeToViewport(topicChannel, (d) => messageRecieved(d));

    let ajaxClient = factory().createAjaxClient(ajaxEndpointUrl);

    ajaxClient.loadPreferences(userPreferencesKey).then((p) => {
        preferences = Object.assign(preferences, defaultPreferences, p);
        console.log("Loaded preferences", preferences);
        stateStore.groupingMode = preferences.mode || defaultPreferences.mode;
        stateStore.sortByScore = preferences.sortByScore !== undefined
                ? preferences.sortByScore
                : defaultPreferences.sortByScore;
        stateStore.recommendationsFirst = preferences.recommendationsFirst !== undefined
                ? preferences.recommendationsFirst
                : defaultPreferences.recommendationsFirst
    });

    $effect(() => { 
        preferences.mode = stateStore.groupingMode;
        preferences.sortByScore = stateStore.sortByScore;
        preferences.recommendationsFirst = stateStore.recommendationsFirst;
        ajaxClient.savePreferences(userPreferencesKey, preferences);
    });

    export function messageRecieved(d) {
        if (!document.body.contains(element)) {
            console.debug(
                "Element is not part of the DOM anymore. Disconnecting and suiciding."
            );
            disconnect()
            return;
        }

        let preData = unpackCompactAnnotatedTextV2(d);
        if (preData.spans.size + preData.relations.size > 25000) {
            console.error(`Too many annotations: ${preData.spans.size} spans ${preData.relations.size} relations`)
            data = undefined
            tooManyAnnotations = true
        }
        else {
            console.info(`Loaded annotations: ${preData.spans.size} spans ${preData.relations.size} relations`)
            tooManyAnnotations = false
            data = preData;
        }
    }

    export function connect(): void {
        if (connected) return;
        wsClient.connect({url: wsEndpointUrl, csrfToken});
    }

    export function disconnect() {
        wsClient.unsubscribeFromViewport();
        wsClient.disconnect();
        connected = false;
    }

    onMount(async () => { 
        connect()
    });

    onDestroy(async () => { 
        disconnect() 
    });

    function cancelRightClick (e: Event): void {
    if (e instanceof MouseEvent) {
      if (e.button === 2) {
        e.preventDefault()
        e.stopPropagation()
      }
    }
  }</script>

<!-- svelte-ignore a11y_click_events_have_key_events -->
<div class="flex-content flex-v-container" bind:this={element} 
    onclickcapture={cancelRightClick} onmousedowncapture={cancelRightClick} 
    onmouseupcapture={cancelRightClick}>

    {#if element}
        <AnnotationDetailPopOver root={element} ajax={ajaxClient} />
    {/if}

    <select bind:value={stateStore.groupingMode} class="form-select rounded-0">
        {#each Object.keys(modes) as value}<option {value}
                >{modes[value]}</option
            >{/each}
    </select>
    {#if data}
        {#if tooManyAnnotations}
            <div class="m-auto text-center text-muted">
                <div class="fs-1"><i class="far fa-dizzy"></i></div>
                <div>Too many annotations</div>
            </div>
        {:else if stateStore.groupingMode == "by-position"}
            <AnnotationsByPositionList {ajaxClient} {data} />
        {:else if stateStore.groupingMode == "by-layer"}
            <AnnotationsByLayerList {ajaxClient} {data} />
        {:else}
            <AnnotationsByLabelList {ajaxClient} {data} {pinnedGroups} />
        {/if}
    {/if}
</div>

<style lang="scss">
     @import '@inception-project/inception-js-api/src/style/InceptionEditorIcons.scss';
</style>

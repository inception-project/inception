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

    import { onMount, onDestroy } from "svelte"
    import { AnnotatedText, unpackCompactAnnotatedTextV2 } from "@inception-project/inception-js-api"
    import { factory } from "@inception-project/inception-diam"
    import AnnotationsByPositionList from "./AnnotationsByPositionList.svelte"
    import AnnotationsByLabelList from "./AnnotationsByLabelList.svelte"

    export let wsEndpointUrl: string
    export let topicChannel: string
    export let ajaxEndpointUrl: string
    export let connected = false

    let mode = 'Group by position';
	let modes = [
		'Group by position',
		'Group by label'
	]

    let data: AnnotatedText

    let wsClient = factory().createWebsocketClient();
    wsClient.onConnect = () => wsClient.subscribeToViewport(topicChannel, d => {
        data = unpackCompactAnnotatedTextV2(d)
    })

    let ajaxClient = factory().createAjaxClient(ajaxEndpointUrl)

    export function connect(): void {
        if (connected) return
        console.log("Connecting to " + wsEndpointUrl)
        wsClient.connect(wsEndpointUrl)
    }

    export function disconnect() {
        wsClient.unsubscribeFromViewport()
        wsClient.disconnect()
        connected = false
    }

    onMount(async () => connect())

    onDestroy(async () => disconnect())
</script>

<div class="flex-content flex-v-container">
    <select bind:value={mode} class="form-select">
        {#each modes as value}<option {value}>{value}</option>{/each}
    </select>
    {#if mode=='Group by position'}
        <AnnotationsByPositionList {ajaxClient} {data} />
    {:else}
        <AnnotationsByLabelList {ajaxClient} {data} />
    {/if}
</div>

<style>
</style>

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

    import { onMount } from "svelte"
    import dayjs from "dayjs"
    import relativeTime from "dayjs/plugin/relativeTime"
    import localizedFormat from "dayjs/plugin/localizedFormat"

    dayjs.extend(relativeTime)
    dayjs.extend(localizedFormat)

    export let dataUrl: string
    export let activities = [];

    onMount(async () => {
        const res = await fetch(dataUrl)
        activities = await res.json()
    })

    export function formatRelativeTime(timestamp) {
        return dayjs().to(dayjs(timestamp));
    }

    export function formatTime(timestamp) {
        return dayjs(timestamp).format("LLLL");
    }

    export function displayUserInfo(activity) {
        return (
            activity.user != activity.annotator &&
            activity.annotator != "CURATION_USER"
        )
    }
</script>

<div class="card border-0 flex-content flex-v-container">
    <div class="card-header rounded-0">Recent activity</div>
    {#if !activities || activities.length === 0}
        <div class="mt-5 d-flex flex-column justify-content-center">
            <div class="d-flex flex-row justify-content-center">
                <div class="spinner-border text-muted" role="status">
                    <span class="sr-only">Loading...</span>
                </div>
            </div>
        </div>
    {:else}
        <ul class="list-group list-group-flush scrolling flex-content flex-v-container">
            {#each activities as activity}
                <li class="list-group-item">
                    <span class="badge bg-secondary float-end">
                        {activity.type}
                    </span>
                    <a
                        href={activity.link}
                        title={activity.documentName}
                        style="display: block"
                        class="text-truncate"
                    >
                        {activity.documentName}
                    </a>
                    <div class="flex-h-container">
                        <small class="text-muted flex-content">
                            {#if displayUserInfo(activity)}
                                <span>
                                    <i class="far fa-user" />
                                    {activity.annotator}
                                </span>
                            {/if}
                        </small>
                        <small class="text-muted">
                            <i class="far fa-clock" />&nbsp;
                            <span title={formatTime(activity.timestamp)}>
                                {formatRelativeTime(activity.timestamp)}
                            </span>
                        </small>
                    </div>
                </li>
            {/each}
        </ul>
    {/if}
</div>

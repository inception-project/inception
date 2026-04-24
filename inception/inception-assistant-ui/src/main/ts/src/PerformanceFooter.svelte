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
    
     export let performance: any;
    export let show: boolean = false;
</script>

{#if performance && show}
    <div class="performance-footer fw-ligher">
        <small><i class="fas fa-pause me-1"></i>{(performance.delay / 1000).toFixed(2)}s</small>
        <small><i class="far fa-clock ms-2 me-1"></i>{(performance.duration / 1000).toFixed(2)}s</small>
        <small
            ><i class="fas fa-stream ms-2 me-1"></i>{(
                performance.tokens / (performance.duration / 1000)
            ).toFixed(2)}t/s</small
        >
    </div>
{/if}

<style lang="scss">
    .performance-footer {
        opacity: 0.5;
        display: block;
        font-size: x-small;
        padding-top: 0.25em;
        color: var(--bs-body-color-secondary);
    }
</style>

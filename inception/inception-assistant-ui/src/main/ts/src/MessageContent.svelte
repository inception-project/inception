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

    import { renderContent } from './AssistantPanelMessages';

    export let message: any;
    export let thinkingHtml: string | null = null;
    export let onToggleThinking: (m: any) => void;
    export let onToggleCollapse: (m: any) => void;
    export let onCopy: (m: any) => void;
    export let onClickHandler: (e: Event) => void;
</script>

{#if thinkingHtml}
    <div class="message-thinking">
        <div
            class="message-thinking-header"
            onclick={() => onToggleThinking(message)}
            role="button"
            tabindex="0"
        >
            {#if message.thinkingSummary}
                {message.thinkingSummary}
            {:else}
                Thinking<span class:dots={!message.done}></span>
            {/if}
        </div>
        {#if !message.thinkingCollapsed}
            <div class="message-thinking-body">
                {@html thinkingHtml}
            </div>
        {/if}
    </div>
{/if}

{#if message.content || message.role !== 'assistant'}
    <div class="message-frame" data-role={message.role} data-internal={message.internal}>
        <div
            class="message-header text-body-secondary"
            onclick={message.collapsible ? () => onToggleCollapse(message) : null}
            role={message.collapsible ? 'button' : undefined}
        >
            {#if message.role === 'assistant'}
                <i class="fas fa-robot me-1" title="Assistant message"></i>
            {:else if message.role === 'user'}
                <i class="fas fa-user me-1" title="User message"></i>
            {:else if message.role === 'system'}
                <i class="fas fa-cog me-1" title="System message"></i>
            {/if}
            {message.actor ? message.actor : message.role}
            {#if !message.collapsible}
                <button
                    class="btn btn-sm btn-link text-body-secondary float-end fw-lighter p-0 copy-button"
                    onclick={() => onCopy(message)}
                >
                    <i class="far fa-copy" title="Copy message"></i>
                </button>
            {/if}
            {#if message.internal}
                <span class="mx-2 text-body-secondary float-end fw-lighter">
                    <i class="fas fa-info" title="Internal message"></i>
                </span>
            {/if}
        </div>
        {#if !message.collapsed}
            {#if message['@type'] === 'textMessage'}
                {#if message.content}
                    <div class="message-body" class:dots={!message.done} onclick={onClickHandler}>
                        {@html renderContent(message)}
                    </div>
                {/if}
            {:else if message['@type'] === 'callResponse'}
                <div class="message-body">
                    <strong>Called tool: {message.toolName}</strong>
                    <div>{message.arguments ? JSON.stringify(message.arguments, null, 2) : 'no arguments'}</div>
                    <div class="call-payload">{typeof message.payload === 'string' ? message.payload : message.payload ? JSON.stringify(message.payload, null, 2) : 'no payload'}</div>
                </div>
            {:else}
                <div class="message-body">Unknown message type: {message['@type']}</div>
            {/if}
        {/if}
    </div>
{/if}

<style lang="scss">
    @keyframes dots {
        0%,
        20% {
            content: '';
        }
        40% {
            content: '.';
        }
        60% {
            content: '..';
        }
        80%,
        100% {
            content: '...';
        }
    }

    .dots::after {
        content: '';
        display: inline-block;
        animation: dots 1.5s steps(1, end) infinite;
    }

    .message-thinking {
        .message-thinking-header {
            display: inline;
            cursor: pointer;
            font-size: 0.8em;
            padding: 0.05em 0.3em;
        }

        .message-thinking-body {
            display: block;
            font-size: smaller;
            padding-left: 0.5em;
            border-left: solid 2px var(--bs-border-color-translucent);
            max-height: 6em;
            overflow: auto;
        }
    }

    .message-body {
        display: block;
        font-size: smaller;

        :global(p) {
            margin-bottom: 0.5em;
        }

        :global(p:last-child) {
            margin-bottom: 0px;
        }

        :global(code) {
            white-space: break-spaces;
        }

        :global(h1),
        :global(h2),
        :global(h3),
        :global(h4),
        :global(h5),
        :global(h6) {
            font-size: var(--bs-body-font-size);
            line-height: var(--bs-body-line-height);
            font-weight: bolder;
            font-variant: small-caps;
        }

        :global(ol),
        :global(ul) {
            margin-bottom: 1.5em;
            padding-left: 1.5em;
        }

        :global(.reference) {
            font-size-adjust: 0.5;
            //        font-size: x-small;
            vertical-align: top;
            border-radius: 0.25em;
            cursor: pointer;
        }
    }

    .message-frame {
        border-radius: 0.25em;
        padding: 5px;
        margin: 3px 0;
    }

    .message-frame[data-role='user'] {
        background-color: var(--bs-info-bg-subtle);
    }

    .message-frame[data-role='assistant'] {
        background-color: var(--bs-success-bg-subtle);
    }

    .message-frame[data-internal='true'] {
        background-color: var(--bs-tertiary-bg);
        padding: 4px 8px;

        .message-body {
            font-size: smaller;
        }

        pre {
            margin-bottom: 0.2rem;
            background-color: #00000010;
            border: solid 1px;
            border-color: var(--bs-border-color-translucent);
            padding: 0.25rem;
        }

        p:has(+ pre) {
            margin-top: 0.5rem;
            margin-bottom: 0;
        }

        code {
            font-family: var(--bs-body-font-family);
        }
    }

    .call-payload {
        white-space: pre-wrap;
        max-height: 6em;
        overflow: auto;
    }

    .message-header {
        display: block;
        font-size: smaller;
        color: var(--bs-body-color-secondary);
    }


    .copy-button {
        visibility: hidden;
    }

    :global(.message:hover) .copy-button {
        visibility: visible;
    }
</style>

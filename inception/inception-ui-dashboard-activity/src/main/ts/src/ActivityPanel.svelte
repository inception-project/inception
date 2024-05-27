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

    import { onMount } from "svelte";

    import dayjs from "dayjs";
    import relativeTime from "dayjs/plugin/relativeTime";
    import localizedFormat from "dayjs/plugin/localizedFormat";

    const OTHER_ACTIVITIES = "other activities";

    const EVENTS = {
        RecommendationAcceptedEvent: "recommendations accepted",
        RecommendationRejectedEvent: "recommendations rejected",
        RelationCreatedEvent: "annotations created",
        SpanCreatedEvent: "annotations created",
        RelationDeletedEvent: "annotations created",
        SpanDeletedEvent: "annotations deleted",
        FeatureValueUpdatedEvent: "annotation updates",
        SpanMovedEvent: "annotation updates",
    };

    interface ActivitySummary {
        from: string;
        to: string;
        globalItems: ActivitySummaryItem[];
        perDocumentItems: Record<string, ActivitySummaryItem[]>;
    }

    interface ActivitySummaryItem {
        event: string;
        count: number;
        subsumed: Iterable<String>;
    }

    dayjs.extend(relativeTime);
    dayjs.extend(localizedFormat);

    export let year: number;

    export let overviewDataUrl: string;
    export let overviewData = undefined;
    export let overviewDataLoading = true;

    export let summaryDataUrl: string;
    export let summaryData: ActivitySummary = undefined;
    export let summaryDataLoading = false;

    onMount(async () => {
        year = new Date().getFullYear();
        const res = await fetch(`${overviewDataUrl}?year=${year}`);
        overviewData = await res.json();
        overviewDataLoading = false;
    });

    function getActivity(week: number, dayOfWeek: number) {
        const date = calculateDate(week, dayOfWeek);
        const dateStr = date.format("YYYY-MM-DD");
        const item = overviewData.items[dateStr];
        return item
            ? { ...item, date: dateStr }
            : {
                  date: dateStr,
                  count: 0,
                  outOfRange:
                      date.isBefore(overviewData.from) ||
                      date.isAfter(overviewData.to),
              };
    }

    function calculateDate(week: number, dayOfWeek: number) {
        const firstDayOfYear = dayjs(`${year}-01-01`);
        const firstDayDayOfWeek = firstDayOfYear.day();
        const date = firstDayOfYear.add(
            week * 7 + dayOfWeek - firstDayDayOfWeek,
            "day",
        );
        return date;
    }

    async function loadSummaryData(date) {
        console.log(date);
        const res = await fetch(summaryDataUrl + `?from=${date}`);
        summaryData = await res.json();

        summaryData.globalItems = translateAndAggregateEvents(
            summaryData.globalItems,
        );
        const originalPerDoc = summaryData.perDocumentItems || {};
        for (const [documentName, items] of Object.entries(originalPerDoc)) {
            summaryData.perDocumentItems[documentName] =
                translateAndAggregateEvents(items);
        }
    }

    function getMonthStartingInWeek(week) {
        for (let dayOfWeek = 0; dayOfWeek < 7; dayOfWeek++) {
            const date = calculateDate(week, dayOfWeek);
            if (date.date() === 1) {
                return date.format("MMM");
            }
        }
        return null;
    }

    function translateAndAggregateEvents(
        sourceItems: ActivitySummaryItem[],
    ): ActivitySummaryItem[] {
        if (!sourceItems || sourceItems.length === 0) {
            return [];
        }

        const otherEvents = new Set<String>();
        const eventTypeAggregator = {};
        let otherCount = 0;
        for (const item of sourceItems) {
            const key = EVENTS[item.event];
            if (!key) {
                otherEvents.add(item.event);
                otherCount += item.count;
                continue;
            }
            const value = eventTypeAggregator[key];
            eventTypeAggregator[key] = value ? value + item.count : item.count;
        }

        const result = Object.entries(eventTypeAggregator)
            .sort(([keyA], [keyB]) => keyA.localeCompare(keyB))
            .map(([key, count]) => ({
                event: key,
                count: count as number,
                subsumed: undefined,
            }));
        result.push({
            event: OTHER_ACTIVITIES,
            count: otherCount,
            subsumed: otherEvents,
        });
        return result as ActivitySummaryItem[];
    }

    async function loadOverview() {
        overviewDataLoading = true;
        const res = await fetch(`${overviewDataUrl}?year=${year}`);
        overviewData = await res.json();
        overviewDataLoading = false;
        return overviewData;
    }

    async function decreaseYear() {
        year--;
        overviewData = await loadOverview();
    }

    async function increaseYear() {
        year++;
        overviewData = await loadOverview();
    }
</script>

{#if overviewDataLoading}
    <div class="mt-5 d-flex flex-column justify-content-center">
        <div class="d-flex flex-row justify-content-center">
            <div class="spinner-border text-muted" role="status">
                <span class="visually-hidden">Loading...</span>
            </div>
        </div>
    </div>
{:else if !overviewData}
    <div class="flex-content no-data-notice m-0">
        <span>No recent activity</span>
    </div>
{:else}
    <div class="sticky-top bg-body p-2 border-bottom">
        <div class="d-flex flex-row justify-content-between mb-3">
            <button
                class="btn btn-sm btn-outline-secondary"
                on:click={decreaseYear}
                style:visibility={year > 1980 ? "visible" : "hidden"}
                ><i class="fas fa-chevron-left"></i> {year - 1}</button
            >
            <h3 class="m-0 p-0">{year}</h3>
            <button
                class="btn btn-sm btn-outline-secondary"
                on:click={increaseYear}
                style:visibility={year < 3000 ? "visible" : "hidden"}
                >{year + 1} <i class="fas fa-chevron-right"></i></button
            >
        </div>
        <div class="d-flex justify-content-center overflow-auto">
            <table class="mx-3 my-2 flex-shrink-0">
                <tr>
                    <td></td>
                    {#each { length: 53 } as _, week}
                        {@const monthStartingInWeek =
                            getMonthStartingInWeek(week)}
                        {#if monthStartingInWeek}
                            <td class="col-legend text-nowrap"
                                >{monthStartingInWeek}</td
                            >
                        {:else}
                            <td></td>
                        {/if}
                    {/each}
                </tr>
                {#each { length: 7 } as _, dayOfWeek}
                    <tr>
                        {#if dayOfWeek === 0 || dayOfWeek === 1 || dayOfWeek === 3 || dayOfWeek === 5}
                            <td
                                class="row-legend align-top text-end"
                                rowspan={dayOfWeek === 1 ||
                                dayOfWeek === 3 ||
                                dayOfWeek === 5
                                    ? 2
                                    : 1}
                            >
                                {#if dayOfWeek === 1}
                                    Mon
                                {:else if dayOfWeek === 3}
                                    Wed
                                {:else if dayOfWeek === 5}
                                    Fri
                                {/if}
                            </td>
                        {/if}
                        {#each { length: 53 } as _, week}
                            {@const item = getActivity(week, dayOfWeek)}
                            <!-- svelte-ignore a11y-click-events-have-key-events -->
                            <td
                                class="cell"
                                style:cursor="pointer"
                                style:visibility={item?.outOfRange
                                    ? "hidden"
                                    : "visible"}
                                style:--opacity={Math.min(
                                    100.0,
                                    item?.count | 1,
                                ) / 100.0}
                                title={dayjs(item.date).format("dddd, LL") +
                                    " - " +
                                    item.count +
                                    " actions"}
                                on:click={() =>
                                    !item?.outOfRange &&
                                    loadSummaryData(item.date)}
                            >
                            </td>
                        {/each}
                    </tr>
                {/each}
            </table>
        </div>
    </div>
{/if}

{#if summaryDataLoading}
    <div class="mt-5 d-flex flex-column justify-content-center">
        <div class="d-flex flex-row justify-content-center">
            <div class="spinner-border text-muted" role="status">
                <span class="visually-hidden">Loading...</span>
            </div>
        </div>
    </div>
{:else}
    {#if summaryData}
        <div
            class="d-flex flex-row justify-content-between small text-muted m-2"
        >
            <div>{dayjs(summaryData.from).format("LL")}</div>
        </div>
    {/if}
    {#if summaryData && !summaryData?.globalItems?.length && !summaryData?.perDocumentItems?.length}
        <div class="flex-content no-data-notice m-0">
            <span>No activity</span>
        </div>
    {:else if summaryData}
        <div class="list-group m-2">
            {#each Object.entries(summaryData.perDocumentItems).sort( ([a], [b]) => a.localeCompare(b), ) as [document, items]}
                <div
                    class="list-group-item d-flex justify-content-between align-items-start"
                >
                    <div class="ms-1 me-auto">
                        <div class="fw-bold text-break">
                            <i class="far fa-file"></i>
                            {document}
                        </div>
                        {#each items || [] as item}
                            <div
                                class="ms-3 me-auto small"
                                title={Array.from(item?.subsumed || []).join(
                                    ", ",
                                )}
                            >
                                {item.count}
                                {item.event}
                            </div>
                        {/each}
                    </div>
                </div>
            {/each}
            <div
                class="list-group-item d-flex justify-content-between align-items-start"
            >
                <div class="ms-1 me-auto">
                    <div class="fw-bold text-break">
                        Project-level activities
                    </div>
                    {#each summaryData.globalItems || [] as item}
                        <div
                            class="ms-3 me-auto small"
                            title={Array.from(item?.subsumed || []).join(", ")}
                        >
                            {item.count}
                            {item.event}
                        </div>
                    {/each}
                </div>
            </div>
        </div>
    {/if}
{/if}

<style lang="scss">
    table {
        border-spacing: 1px;
        border-collapse: separate;
    }

    .col-legend {
        font-size: 8px;
        max-width: 10px;
    }

    .row-legend {
        font-size: 8px;
    }

    .cell {
        border-radius: 2px;
        border-style: solid;
        border-color: rgba(#0d6efd, 0.2);
        border-width: 1px;
        width: 10px;
        height: 10px;
        // Could be done better using color-mix or relative-color but both are not well supported
        // yet - https://caniuse.com/?search=color-mix
        background: rgba(#0d6efd, var(--opacity));
        transition:
            all 125ms ease-in,
            opacity 500ms ease-out;

        &:hover {
            border-color: rgba(#fd7e14, 0.2);
            box-shadow: 0 0 5px #fd7e14;
            transform: scale(1.5);
        }
    }
</style>

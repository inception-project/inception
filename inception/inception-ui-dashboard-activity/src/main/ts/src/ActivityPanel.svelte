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
    import isBetween from "dayjs/plugin/isBetween";

    const OTHER_ACTIVITIES = "other activities";

    const EVENTS = {
        RecommendationAcceptedEvent: "recommendations accepted",
        RecommendationRejectedEvent: "recommendations rejected",
        RelationCreatedEvent: "relation creations",
        SpanCreatedEvent: "span creations",
        RelationDeletedEvent: "relation deletions",
        SpanDeletedEvent: "span deletions",
        FeatureValueUpdatedEvent: "feature value updates",
        SpanMovedEvent: "annotation moves",
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
    dayjs.extend(isBetween);

    let {
        year,
        dataOwner,
        overviewDataUrl,
        overviewData = $bindable(undefined),
        overviewDataLoading = $bindable(true),
        summaryDataUrl,
        summaryData = $bindable(undefined),
        summaryDataLoading = $bindable(false)
    }: {
        year: number;
        dataOwner: string;
        overviewDataUrl: string;
        overviewData?: any;
        overviewDataLoading?: boolean;
        summaryDataUrl: string;
        summaryData?: ActivitySummary;
        summaryDataLoading?: boolean;
    } = $props();

    // Flag to prevent saving to localStorage before initial load
    let isInitialized = $state(false);

    onMount(async () => {
        const savedYear = window.activityYear;
        const savedGranularity = window.activityGranularity;
        const savedGrouping = window.activityGrouping;
        const savedRange = window.activityRange;

        year = savedYear ? parseInt(savedYear, 10) : new Date().getFullYear();
        if (savedGranularity) clickBehavior = savedGranularity as "day" | "week" | "month";
        if (savedGrouping) groupingBehavior = savedGrouping as "byDocument" | "overall";
        if (savedRange) {
            selectedRange = JSON.parse(savedRange);
            loadSummaryData(selectedRange.from, selectedRange.to);
        }

        overviewData = loadOverview();
        
        // Mark as initialized after restoring values
        isInitialized = true;
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

    /**
     * Calculate cell opacity for a given action count.
     * - count === 0 -> 0.0 (invisible)
     * - count > 0 -> baseline opacity (e.g. 0.2) to ensure visibility for small counts
     * - scales up towards 1.0 using a log curve to compress large values
     */
    function getCellOpacity(count?: number) {
        const c = Number(count || 0);
        if (c <= 0) return 0.0;

        const baseline = 0.1; // minimum visible opacity for any non-zero count

        // Use a logarithmic-like scaling: normalized = log(1 + c) / log(1 + maxScale)
        // Choose a maxScale that maps reasonable counts to near-1.0. If counts grow beyond
        // maxScale they'll still approach 1.0 slowly.
        const maxScale = 1000; // adjust depending on typical counts in your data
        const normalized = Math.log(1 + c) / Math.log(1 + maxScale);

        // Interpolate between baseline and 1.0
        return Math.min(1.0, baseline + (1 - baseline) * normalized);
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

    let processedSummaryData = $derived.by(() => {
        if (!summaryData) return undefined;

        let processedData = {...summaryData};

        processedData.globalItems = translateAndAggregateEvents(processedData.globalItems);

        if (groupingBehavior === "overall") {
            const aggregateItems: Record<string, ActivitySummaryItem> = {};
            for (const items of Object.values(processedData.perDocumentItems || {})) {
                for (const item of items || []) {
                    if (!aggregateItems[item.event]) {
                        aggregateItems[item.event] = {event: item.event, count: 0};
                    }

                    aggregateItems[item.event].count += item.count;
                }
            }

            processedData.perDocumentItems = {
                "Documents": Object.values(aggregateItems)
            };
        }

        const originalPerDoc = processedData.perDocumentItems || {};
        processedData.perDocumentItems = Object.fromEntries(
            Object.entries(originalPerDoc).map(([documentName, items]) => [
                documentName,
                translateAndAggregateEvents(items)
            ])
        );

        return processedData;
    })

    async function loadSummaryData(from: string, to?: string) {
        let url = summaryDataUrl + `?from=${encodeURIComponent(from)}`;
        if (to) {
            url += `&to=${encodeURIComponent(to)}`;
        }
        if (dataOwner) {
            url += `&dataOwner=${encodeURIComponent(dataOwner)}`;
        }

        const res = await fetch(url);
        summaryData = await res.json() as ActivitySummary;
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
        let url = `${overviewDataUrl}?year=${encodeURIComponent(year)}`;
        if (dataOwner) {
            url += `&dataOwner=${encodeURIComponent(dataOwner)}`;
        }
        const res = await fetch(url);
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

    // Add a reactive variable to track the selected click behavior
    let clickBehavior: "day" | "week" | "month" = $state("day");
    let groupingBehavior: "byDocument" | "overall" = $state("byDocument");

    // Add a reactive variable to store the selected range
    let selectedRange: { from: string; to: string } = $state({ from: "", to: "" });

    // Add a function to handle loading data based on the selected behavior
    function handleCellClick(item, week, dayOfWeek) {
        if (item?.outOfRange) return;

        if (clickBehavior === "day") {
            selectedRange = { from: item.date, to: item.date };
            loadSummaryData(item.date);
        } else if (clickBehavior === "week") {
            const startOfWeek = calculateDate(week, 0).format("YYYY-MM-DD");
            const endOfWeek = calculateDate(week, 6).format("YYYY-MM-DD");
            selectedRange = { from: startOfWeek, to: endOfWeek };
            loadSummaryData(startOfWeek, endOfWeek);
        } else if (clickBehavior === "month") {
            const date = calculateDate(week, dayOfWeek);
            const startOfMonth = date.startOf("month").format("YYYY-MM-DD");
            const endOfMonth = date.endOf("month").format("YYYY-MM-DD");
            selectedRange = { from: startOfMonth, to: endOfMonth };
            loadSummaryData(startOfMonth, endOfMonth);
        }
    }

    $effect(() => {
        // Only save to the window object after initialization to avoid overwriting restored values
        if (!isInitialized) return;
        
        if (year) window.activityYear = year.toString();
        window.activityGranularity = clickBehavior;
        window.activityGrouping = groupingBehavior;
        if (selectedRange) window.activityRange = JSON.stringify(selectedRange);
    });
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
    <div class="sticky-top bg-body p-2">
        <div class="d-flex flex-row justify-content-between mb-3">
            <button
                class="btn btn-sm btn-outline-secondary"
                onclick={decreaseYear}
                style:visibility={year > 1980 ? "visible" : "hidden"}
                ><i class="fas fa-chevron-left"></i> {year - 1}</button
            >
            <h3 class="m-0 p-0">{year}</h3>
            <button
                class="btn btn-sm btn-outline-secondary"
                onclick={increaseYear}
                style:visibility={year < 3000 ? "visible" : "hidden"}
                >{year + 1} <i class="fas fa-chevron-right"></i></button
            >
        </div>
        <div class="d-flex justify-content-center overflow-auto">
            <div>
                <table class="mx-3 my-2 flex-shrink-0">
                    <tbody>
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
                                    <!-- svelte-ignore a11y_click_events_have_key_events -->
                                    <td
                                        class="cell"
                                        class:highlight={
                                            !item?.outOfRange &&
                                            selectedRange.from &&
                                            dayjs(item.date).isBetween(
                                                selectedRange.from,
                                                selectedRange.to,
                                                null,
                                                "[]",
                                            )
                                        }
                                        style:cursor="pointer"
                                        style:visibility={item?.outOfRange
                                            ? "hidden"
                                            : "visible"}
                                        style:--opacity={getCellOpacity(item?.count)}
                                        title={dayjs(item.date).format("dddd, LL") +
                                            " - " +
                                            item.count +
                                            " actions"}
                                        onclick={() => handleCellClick(item, week, dayOfWeek)}
                                    >
                                    </td>
                                {/each}
                            </tr>
                        {/each}
                    </tbody>
                </table>
            </div>
            <div class="d-flex flex-column justify-content-center">
                <div class="btn-group-vertical" role="group" aria-label="Granularity">
                    <button
                        type="button"
                        class="btn btn-sm btn-outline-primary"
                        class:active={clickBehavior === "day"}
                        onclick={() => (clickBehavior = "day")}
                    >
                        Day
                    </button>
                    <button
                        type="button"
                        class="btn btn-sm btn-outline-primary"
                        class:active={clickBehavior === "week"}
                        onclick={() => (clickBehavior = "week")}
                    >
                        Week
                    </button>
                    <button
                        type="button"
                        class="btn btn-sm btn-outline-primary"
                        class:active={clickBehavior === "month"}
                        onclick={() => (clickBehavior = "month")}
                    >
                        Month
                    </button>
                </div>
            </div>
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
    {#if processedSummaryData}
        <div
            class="d-flex flex-row justify-content-between small text-muted m-2"
        >
            <div>{dayjs(processedSummaryData.from).format("LL")} - {dayjs(processedSummaryData.to).format("LL")}</div>
            <div>
                <div class="btn-group" role="group" aria-label="Grouping">
                    <button
                        type="button"
                        class="btn btn-sm btn-outline-primary"
                        class:active={groupingBehavior === "byDocument"}
                        onclick={() => (groupingBehavior = "byDocument")}
                    >
                        by document
                    </button>
                    <button
                        type="button"
                        class="btn btn-sm btn-outline-primary"
                        class:active={groupingBehavior === "overall"}
                        onclick={() => (groupingBehavior = "overall")}
                    >
                        overall
                    </button>
                </div>
            </div>
        </div>
    {/if}
    {#if processedSummaryData && (!processedSummaryData.globalItems || processedSummaryData.globalItems.length === 0) && (!processedSummaryData.perDocumentItems || Object.keys(processedSummaryData.perDocumentItems).length === 0)}
        <div class="flex-content no-data-notice m-0">
            <span>No activity</span>
        </div>
    {:else if processedSummaryData}
        <div class="list-group m-2">
            {#each Object.entries(processedSummaryData.perDocumentItems).sort( ([a], [b]) => a.localeCompare(b), ) as [document, items]}
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
            {#if processedSummaryData.globalItems && processedSummaryData.globalItems.length > 0}
                <div
                    class="list-group-item d-flex justify-content-between align-items-start"
                >
                    <div class="ms-1 me-auto">
                        <div class="fw-bold text-break">
                            Project-level activities
                        </div>
                        {#each processedSummaryData.globalItems || [] as item}
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
            {/if}
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

    .no-data-notice {
        padding: 0px !important;
        margin: 0px !important;
    }

    .cell {
        border-radius: 5px;
        border-style: solid;
        border-color: rgba(#0d6efd, 0.2);
        border-width: 2px;
        width: 16px;
        height: 16px;
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

    .cell.highlight {
        border-color: rgba(#fd7e14, 0.5);
    }
</style>

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

// Kendo's splitter parses pane size with parseInt(), truncating fractional percent. To
// preserve the server's stored precision we keep the percent intent here and re-apply
// it as integer pixels (which Kendo handles exactly) on init and on window resize.
function initInceptionAjaxSplitter(selector, url, orientation, pctIntent) {
  const RESIZE_DEBOUNCE_MS = 150;
  const STATE_KEY = 'inceptionAjaxSplitter';
  const vertical = orientation === 'vertical';
  const extentProp = vertical ? 'offsetHeight' : 'offsetWidth';
  const extentFn = vertical ? 'height' : 'width';

  const $splitter = $(selector);
  if (!$splitter.length) return;
  const splitter = $splitter.data('kendoSplitter');
  if (!splitter) return;

  // Wicket may re-render the splitter (e.g. on sidebar toggle), re-running this init. Keep any
  // prior state so a drag survives, but always rebind the handlers below - stale bindings from the
  // previous widget instance silently break size persistence.
  let state = $splitter.data(STATE_KEY);
  if (state) {
    state.url = url;
    if (Array.isArray(pctIntent)) state.pctIntent = pctIntent.slice();
  } else {
    state = {
      url: url,
      // null entries are pixel/auto panes - never persisted as percent. Prefer the percentages
      // passed in by the server: Kendo rewrites each pane's `size` to pixels during init, so
      // reading them back off the widget can capture a mid-layout value and lock the pane there.
      pctIntent: Array.isArray(pctIntent) ? pctIntent.slice()
        : splitter.options.panes.map(paneOption => {
          if (paneOption && typeof paneOption.size === 'string'
              && paneOption.size.indexOf('%') > 0) {
            const parsed = parseFloat(paneOption.size);
            return isNaN(parsed) ? null : parsed;
          }
          return null;
        }),
      // Suppresses the resize handler during programmatic re-layout so rounding drift is never
      // persisted as a user drag.
      applying: false,
      lastSnapshot: null,
      // Extent at which applyPixels() last applied the intent; null = never (it bails on a zero
      // extent). See the resize handler for why null must not count as "extent changed".
      settledExtent: null
    };
    $splitter.data(STATE_KEY, state);
  }

  // Kendo's denominator: element extent minus the splitbars (along the splitter axis).
  const computeAvailableExtent = () => {
    let availableExtent = $splitter[extentFn]();
    if (!availableExtent) return 0;
    $splitter.children('.k-splitbar').each(function () {
      availableExtent -= this[extentProp];
    });
    return availableExtent;
  };

  const applyPixels = () => {
    const availableExtent = computeAvailableExtent();
    if (availableExtent <= 0) return;
    const panes = $splitter.children('.k-pane');
    panes.each(function (i, pane) {
      if (!pane.id) pane.id = $splitter[0].id + '_p' + i;
    });
    state.applying = true;
    try {
      state.pctIntent.forEach((percent, i) => {
        if (percent === null) return;
        const pane = panes.get(i);
        if (!pane) return;
        splitter.size('#' + pane.id, Math.round(percent * availableExtent / 100) + 'px');
      });
      state.lastSnapshot = snapshot();
      state.settledExtent = availableExtent;
    }
    finally {
      state.applying = false;
    }
  };

  const snapshot = () => {
    const availableExtent = computeAvailableExtent();
    if (availableExtent <= 0) return null;
    const sizes = [];
    $splitter.children('.k-pane').each(function (i, pane) {
      sizes.push(pane[extentProp] / availableExtent * 100);
    });
    return sizes.join(',');
  };

  applyPixels();

  // A splitter still being laid out reports a near-zero extent, so the applyPixels() above may
  // have bailed. Re-apply the intent whenever the extent actually changes, covering both the
  // initial settle and later relayouts.
  if (state.sizeObserver) state.sizeObserver.disconnect();
  if (typeof ResizeObserver === 'function') {
    let lastExtent = computeAvailableExtent();
    state.sizeObserver = new ResizeObserver(() => {
      if (state.applying) return;
      const extent = computeAvailableExtent();
      // Sub-pixel jitter must not re-apply, or dragging fights the observer.
      if (extent <= 0 || Math.abs(extent - lastExtent) < 1) return;
      lastExtent = extent;
      applyPixels();
    });
    state.sizeObserver.observe($splitter[0]);
  }

  let resizeTimer;
  const eventNs = '.splitter_' + $splitter[0].id;
  state.eventNs = eventNs;
  $(window).off(eventNs).on('resize' + eventNs, () => {
    clearTimeout(resizeTimer);
    resizeTimer = setTimeout(applyPixels, RESIZE_DEBOUNCE_MS);
  });

  // Unbind only our own handler so listeners registered via SplitterBehavior's
  // ISplitterListener / options.resize survive a re-init.
  if (state.resizeHandler) splitter.unbind('resize', state.resizeHandler);
  state.resizeHandler = () => {
    if (state.applying) return;
    const currentSnapshot = snapshot();
    if (currentSnapshot === null || currentSnapshot === state.lastSnapshot) return;

    // Kendo fires `resize` for any relayout, not just a user drag. Mid-layout ratios must not
    // become the new intent, or the pane is persisted at whatever fraction it held while the
    // container was still settling. So when the extent has changed since we last applied the
    // intent, re-assert it instead of adopting the ratios.
    //
    // Unless we never applied it (settledExtent === null): then there is no known-good intent to
    // restore and re-applying would snap the pane back, discarding this drag. The extent is
    // non-zero here, so the layout has settled and the drag is genuine.
    const availableExtent = computeAvailableExtent();
    if (state.settledExtent !== null && Math.abs(state.settledExtent - availableExtent) >= 1) {
      state.settledExtent = availableExtent;
      applyPixels();
      return;
    }
    state.settledExtent = availableExtent;

    state.lastSnapshot = currentSnapshot;
    $splitter.children('.k-pane').each(function (i, pane) {
      if (state.pctIntent[i] !== null) {
        state.pctIntent[i] = pane[extentProp] / availableExtent * 100;
      }
    });
    Wicket.Ajax.get({ u: state.url, ep: { sizes: currentSnapshot } });
  };
  splitter.bind('resize', state.resizeHandler);
}

// Tear down the Kendo widget and the listeners bound by init. Must run before Wicket replaces the
// splitter DOM, or the lingering widget reference briefly leaves the new panes unstyled.
function destroyInceptionAjaxSplitter(selector) {
  const $splitter = $(selector);
  if (!$splitter.length) return;
  const state = $splitter.data('inceptionAjaxSplitter');
  if (state && state.eventNs) {
    $(window).off(state.eventNs);
  }
  if (state && state.sizeObserver) state.sizeObserver.disconnect();
  $splitter.removeData('inceptionAjaxSplitter');
  const splitter = $splitter.data('kendoSplitter');
  if (splitter) splitter.destroy();
  $splitter.removeData('kendoSplitter');
}

// Re-create the splitter in place with a new pane configuration, reusing the existing pane DOM so
// its content survives (e.g. an editor iframe's scroll position). Use instead of a destroy plus
// full Wicket re-render when pane content must be preserved.
function reconfigureInceptionAjaxSplitter(selector, panes, url, orientation, pctIntent) {
  const $splitter = $(selector);
  if (!$splitter.length) return;
  const state = $splitter.data('inceptionAjaxSplitter');
  if (state && state.eventNs) {
    $(window).off(state.eventNs);
  }
  if (state && state.sizeObserver) state.sizeObserver.disconnect();
  const existing = $splitter.data('kendoSplitter');
  if (existing) existing.destroy();
  $splitter.removeData('kendoSplitter');
  $splitter.removeData('inceptionAjaxSplitter');
  $splitter.children('.k-splitbar').remove();
  $splitter.kendoSplitter({ orientation: orientation, panes: panes });
  initInceptionAjaxSplitter(selector, url, orientation, pctIntent);
}

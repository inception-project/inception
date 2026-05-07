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
function initInceptionAjaxSplitter(selector, url, orientation) {
  const RESIZE_DEBOUNCE_MS = 150;
  const STATE_KEY = 'inceptionAjaxSplitter';
  const vertical = orientation === 'vertical';
  const extentProp = vertical ? 'offsetHeight' : 'offsetWidth';
  const extentFn = vertical ? 'height' : 'width';

  const $splitter = $(selector);
  if (!$splitter.length) return;
  const splitter = $splitter.data('kendoSplitter');
  if (!splitter) return;

  // Wicket may re-render the splitter component (e.g. on sidebar toggle), causing this
  // init to run again. Preserve any prior pctIntent/lastSnapshot so a user's drag isn't
  // lost across re-renders, but always rebind handlers below — relying on stale bindings
  // from a previous widget instance silently breaks size persistence.
  let state = $splitter.data(STATE_KEY);
  if (state) {
    state.url = url;
  } else {
    state = {
      url: url,
      // null entries are pixel/auto panes — never persisted as percent.
      pctIntent: splitter.options.panes.map(paneOption => {
        if (paneOption && typeof paneOption.size === 'string'
            && paneOption.size.indexOf('%') > 0) {
          const parsed = parseFloat(paneOption.size);
          return isNaN(parsed) ? null : parsed;
        }
        return null;
      }),
      // Suppresses the resize handler during programmatic re-layout so sub-pixel rounding
      // drift never gets persisted as a user drag.
      applying: false,
      lastSnapshot: null
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

  let resizeTimer;
  const eventNs = '.splitter_' + $splitter[0].id;
  state.eventNs = eventNs;
  $(window).off(eventNs).on('resize' + eventNs, () => {
    clearTimeout(resizeTimer);
    resizeTimer = setTimeout(applyPixels, RESIZE_DEBOUNCE_MS);
  });

  // Unbind only our own previous handler (if re-init) so any resize listener
  // registered via SplitterBehavior's ISplitterListener / options.resize survives.
  if (state.resizeHandler) splitter.unbind('resize', state.resizeHandler);
  state.resizeHandler = () => {
    if (state.applying) return;
    const currentSnapshot = snapshot();
    if (currentSnapshot === null || currentSnapshot === state.lastSnapshot) return;
    state.lastSnapshot = currentSnapshot;
    const availableExtent = computeAvailableExtent();
    $splitter.children('.k-pane').each(function (i, pane) {
      if (state.pctIntent[i] !== null) {
        state.pctIntent[i] = pane[extentProp] / availableExtent * 100;
      }
    });
    Wicket.Ajax.get({ u: state.url, ep: { sizes: currentSnapshot } });
  };
  splitter.bind('resize', state.resizeHandler);
}

// Tear down both the Kendo widget and the window resize listener bound by the init above.
// Callers must invoke this before Wicket replaces the splitter DOM, otherwise the lingering
// widget reference can briefly leave the new pane elements unstyled during the Ajax DOM swap.
function destroyInceptionAjaxSplitter(selector) {
  const $splitter = $(selector);
  if (!$splitter.length) return;
  const state = $splitter.data('inceptionAjaxSplitter');
  if (state && state.eventNs) {
    $(window).off(state.eventNs);
  }
  $splitter.removeData('inceptionAjaxSplitter');
  const splitter = $splitter.data('kendoSplitter');
  if (splitter) splitter.destroy();
}

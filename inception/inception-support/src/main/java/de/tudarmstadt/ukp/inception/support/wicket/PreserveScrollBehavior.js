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

// When Wicket replaces a component via AJAX, the browser discards the scroll position of
// every scrollable element in the replaced subtree - the replacement nodes are freshly
// created and start at scrollTop 0. Wicket publishes '/dom/node/removing' with the old
// element and '/dom/node/added' with its replacement, and since Wicket re-resolves the
// replacement by the *same* markup id, that id is a stable key across the swap.
//
// Wicket only fires those events for the element it was actually asked to repaint, which
// is usually an ancestor (e.g. a splitter container) rather than the scrollable element
// itself. So we snapshot/restore by walking the subtree for elements carrying the marker
// class, keyed by their own markup ids.
function initInceptionPreserveScroll(markerClass) {
  const STATE_KEY = 'inceptionPreserveScroll';

  if (window[STATE_KEY]) {
    // Already installed - a page may bind the behavior more than once (or re-render the
    // component carrying it). Just record the additional marker class; the subscriptions
    // below must not be registered twice or every element would be snapshotted N times.
    window[STATE_KEY].markerClasses.add(markerClass);
    return;
  }

  const state = {
    markerClasses: new Set([markerClass]),
    // Scroll offsets keyed by markup id, captured on '/dom/node/removing' and consumed by
    // the matching '/dom/node/added'. Entries that are never consumed (the element was
    // removed outright rather than replaced) are cleared at the end of the request.
    offsets: new Map()
  };
  window[STATE_KEY] = state;

  const selector = () => Array.from(state.markerClasses)
      .map(cls => '.' + cls)
      .join(',');

  // The marker may sit on the replaced element itself or anywhere beneath it.
  const scrollablesIn = (element) => {
    if (!element || !element.querySelectorAll) return [];
    const sel = selector();
    const found = Array.from(element.querySelectorAll(sel));
    if (element.matches && element.matches(sel)) found.unshift(element);
    return found;
  };

  Wicket.Event.subscribe(Wicket.Event.Topic.DOM_NODE_REMOVING, (jqEvent, element) => {
    scrollablesIn(element).forEach(scrollable => {
      // Elements without an id cannot be matched up again after the swap.
      if (!scrollable.id) return;
      if (!scrollable.scrollTop && !scrollable.scrollLeft) return;
      state.offsets.set(scrollable.id, {
        top: scrollable.scrollTop,
        left: scrollable.scrollLeft
      });
    });
  });

  Wicket.Event.subscribe(Wicket.Event.Topic.DOM_NODE_ADDED, (jqEvent, element) => {
    scrollablesIn(element).forEach(scrollable => {
      const offset = state.offsets.get(scrollable.id);
      if (!offset) return;
      state.offsets.delete(scrollable.id);

      // Restoring immediately would clamp against a scrollHeight that has not settled yet
      // if the new content lays out asynchronously (images, fonts, lazily filled lists),
      // leaving the element short of where the user was. Applying it now covers the common
      // synchronous case without a visible jump, and again after layout covers the rest.
      const apply = () => {
        scrollable.scrollTop = offset.top;
        scrollable.scrollLeft = offset.left;
      };
      apply();
      requestAnimationFrame(apply);
    });
  });

  // A replaced element is not guaranteed to come back (it may have been removed, or moved
  // out of the repainted subtree). Dropping leftovers keeps a stale offset from being
  // applied to some unrelated later render that happens to reuse the id.
  Wicket.Event.subscribe(Wicket.Event.Topic.AJAX_CALL_COMPLETE, () => {
    requestAnimationFrame(() => state.offsets.clear());
  });
}

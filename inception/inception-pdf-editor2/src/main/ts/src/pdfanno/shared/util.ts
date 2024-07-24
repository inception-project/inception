/**
 * Utility.
 */

export function anyOf (target, candidates) {
  return candidates.filter(c => c === target).length > 0
}

/**
 * Dispatch a custom event to `window` object.
 */
export function dispatchWindowEvent (eventName, data?) {
  var event = document.createEvent('CustomEvent')
  event.initCustomEvent(eventName, true, true, data)
  window.dispatchEvent(event)
}

/**
 * Parse URL queries, and return it as a Map.
 * @returns {{}}
 */
export function parseUrlQuery () {
  return window.location.search
    .replace('?', '')
    .split('&')
    .reduce((map, keyValue) => {
      const [ key, value ] = keyValue.split('=')
      map[key] = value
      return map
    }, {})
}

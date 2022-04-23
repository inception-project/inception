/**
 * Dispatch a custom event to `window` object.
 */
export function dispatchWindowEvent (eventName: string, data?) {
  var event = document.createEvent('CustomEvent')
  event.initCustomEvent(eventName, true, true, data)
  window.dispatchEvent(event)
}

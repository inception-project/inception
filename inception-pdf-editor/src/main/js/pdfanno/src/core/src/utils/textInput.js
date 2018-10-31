/**
 * Enable Text input enable.
 */
export function enable ({ uuid, text, disable = false, autoFocus = false, blurListener = null }) {
  var event = document.createEvent('CustomEvent')
  event.initCustomEvent('enableTextInput', true, true, ...arguments)
  window.dispatchEvent(event)
  console.log('dispatchEvent:', event, arguments[0])
}

/**
 * Disable the text input.
 */
export function disable () {
  var event = document.createEvent('CustomEvent')
  event.initCustomEvent('disappearTextInput', true, true)
  window.dispatchEvent(event)
}

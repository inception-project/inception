import $ from 'jquery'

/**
 * Event listeners.
 */

/**
 * Initializer.
 */
export function setup () {
    $(document).on('keydown', e => {
        if (e.keyCode === 17 || e.keyCode === 91) { // 17:ctrlKey, 91:cmdKey
            dispatchWindowEvent('manageCtrlKey', 'on')
        }
    }).on('keyup', e => {
        // Allow any keyboard events for <input/>.
        if (e.target.tagName.toLowerCase() === 'input') {
            return
        }

        dispatchWindowEvent('manageCtrlKey', 'off')
    })
}

/**
 * Dispatch a custom event to `window` object.
 */
function dispatchWindowEvent (eventName, data) {
    var event = document.createEvent('CustomEvent')
    event.initCustomEvent(eventName, true, true, data)
    window.dispatchEvent(event)
}

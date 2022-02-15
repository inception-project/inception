/**
 * Core facilities for Label Input.
 */
import $ from 'jquery'
import * as alertDialog from '../../uis/alertDialog'
import * as color from './color'

let currentTab

export function setCurrentTab (tab) {
    currentTab = tab
}

export function getCurrentTab (tab) {
    return currentTab
}

/**
 * A blur event listener.
 */
let _blurListener

/**
 * The uuid for the current annotation.
 */
let currentUUID

/**
 * The cache for the DOM of inputLabel.
 */
let $inputLabel
window.addEventListener('DOMContentLoaded', () => {
    $inputLabel = $('#inputLabel')
})

/**
 * The function which saves a text.
 */
let _saveAnnotationText

/**
 * Setup the core module.
 */
export function setup (saveAnnotationText) {
    _saveAnnotationText = saveAnnotationText
}

/**
 * Enable the Label Input UI.
 */
export function enable ({ uuid, text, disable = false, autoFocus = false, blurListener = null }) {

    currentUUID = uuid

    if (_blurListener) {
        _blurListener()
        _blurListener = null

    }

    $inputLabel
        .attr('disabled', 'disabled')
        .val(text || '')
        .off('blur')
        .off('keyup')

    if (disable === false) {
        $inputLabel
            .removeAttr('disabled')
            .on('keyup', () => {
                watchColor(uuid)
            })
    }

    if (autoFocus) {
        $inputLabel.focus()
    }

    $inputLabel.on('blur', () => {
        if (blurListener) {
            blurListener()
            _blurListener = blurListener
        }
        saveText(uuid)
    })
}

/**
 * Disable the Label Input UI.
 */
export function disable () {
    currentUUID = null
    $inputLabel
        .attr('disabled', 'disabled')
        .val('')
}

/**
 * Check the uuid is the current one in Label Input.
 */
export function isCurrent (uuid) {
    return currentUUID === uuid
}

/**
 * Save the text an user wrote, to the annotation ( specified by uuid ).
 */
function saveText (uuid) {
    const text = $inputLabel.val()

    // Check the text valid.
    if (!isValidInput(text)) {
        alertDialog.show({ message : 'Nor white space, tab, or line break are not permitted.' })
        return
    }

    _saveAnnotationText(uuid, text)
}

function watchColor (uuid) {
    const aText = $inputLabel.val()
    const aColor = color.find(currentTab, aText)
    color.notifyColorChanged({ color : aColor, uuid })
}

/**
 * Check the text is permitted to save.
 *
 * Nor White space, tab or line break are not permitted.
 */
export function isValidInput (text) {
    return !/\s/.test(text)
}

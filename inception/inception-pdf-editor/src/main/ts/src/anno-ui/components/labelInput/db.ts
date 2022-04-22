import * as annoUiCore from '../../core'

/**
 * Storage for label settings.
 */

// LocalStorage key to save label data.
function LSKEY_LABEL_LIST () {
    return annoUiCore.applicationName() + '-label-list'
}

/**
 * Get the labels from the storage.
 */
export function getLabelList () {
    return JSON.parse(localStorage.getItem(LSKEY_LABEL_LIST()) || '{}')
}

/**
 * Save the labels to the storage.
 */
export function saveLabelList (data) {
    localStorage.setItem(LSKEY_LABEL_LIST(), JSON.stringify(data))
}

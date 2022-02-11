/**
 * Listeners for Input Label.
 */
import * as core from './core'

/**
 * The function which gets selected annotations.
 */
let _getSelectedAnnotations

/**
 * Set window event listeners.
 */
export function setup (getSelectedAnnotations) {

    _getSelectedAnnotations = getSelectedAnnotations

    // Enable the text input.
    window.addEventListener('enableTextInput', e => {
        core.enable(e.detail)
    })

    // Disable the text input.
    window.addEventListener('disappearTextInput', e => {
        core.disable(e.detail)
    })

    // The event an annotation was deleted.
    window.addEventListener('annotationDeleted', e => {
        handleAnnotationDeleted(e.detail)
    })

    // The event an annotation was hovered in.
    window.addEventListener('annotationHoverIn', e => {
        handleAnnotationHoverIn(e.detail)
    })

    // The event an annotation was hovered out.
    window.addEventListener('annotationHoverOut', e => {
        handleAnnotationHoverOut(e.detail)
    })

    // The event an annotation was selected.
    window.addEventListener('annotationSelected', e => {
        handleAnnotationSelected(e.detail)
    })

    // The event an annotation was deselected.
    window.addEventListener('annotationDeselected', () => {
        handleAnnotationDeselected()
    })
}

/**
 * When an annotation is deleted.
 */
function handleAnnotationDeleted ({ uuid }) {
    if (core.isCurrent(uuid)) {
        core.disable(...arguments)
    }
}

/**
 * When an annotation started to be hovered.
 */
function handleAnnotationHoverIn (annotation) {
    if (_getSelectedAnnotations().length === 0) {
        core.enable({ uuid : annotation.uuid, text : annotation.text, disable : true })
    }
}

/**
 * When an annotation ended to be hovered.
 */
function handleAnnotationHoverOut (annotation) {
    if (_getSelectedAnnotations().length === 0) {
        core.disable()
    }
}

/**
 * When an annotation is selected.
 */
function handleAnnotationSelected (annotation) {
    if (_getSelectedAnnotations().length === 1) {
        core.enable({ uuid : annotation.uuid, text : annotation.text })
    } else {
        core.disable()
    }
}

/**
 * When an annotation is deselected.
 */
function handleAnnotationDeselected () {
    const annos = _getSelectedAnnotations()
    if (annos.length === 1) {
        core.enable({ uuid : annos[0].uuid, text : annos[0].text })
    } else {
        core.disable()
    }
}

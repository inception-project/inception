/**
 * UI parts - Input Label.
 */
import './index.css'

import * as core from './core'
import * as behavior from './behavior'
import * as listener from './listener'
import * as color from './color'

/**
 * Setup the Label Input.
 */
export function setup ({
    getSelectedAnnotations,
    saveAnnotationText,
    createSpanAnnotation,
    createRelAnnotation,
    createRectAnnotation = null,
    colorChangeListener = function () {},
    namingRuleForExport = behavior.defaultNamingRuleForExport
}) {

    // Define core functions.
    core.setup(saveAnnotationText)

    // Define user actions.
    behavior.setup({ createSpanAnnotation, createRelAnnotation, createRectAnnotation, namingRuleForExport })

    // Define window event listeners.
    listener.setup(getSelectedAnnotations)

    color.setup(colorChangeListener)
}

export const getColorMap = color.getColorMap
// export getColorMap

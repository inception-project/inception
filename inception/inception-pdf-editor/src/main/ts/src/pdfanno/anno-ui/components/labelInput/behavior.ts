/**
 * Define the behaviors of label input component.
 */
import * as alertDialog from '../../uis/alertDialog'
import * as db from './db'
import * as core from './core'
import * as color from './color'

/**
 * Setup the behaviors for Input Label.
 */
export function setup ({ createSpanAnnotation, createRelAnnotation, createRectAnnotation, namingRuleForExport }) {

    core.setCurrentTab('span')

    // Set add button behavior.
    setupAddButton()
}

export function defaultNamingRuleForExport (exportProcess) {
    exportProcess('pdfanno.conf')
}

/**
 * Set the add button behavior.
 */
function setupAddButton () {
    $('.js-label-tab-content').on('click', '.js-add-label-button', e => {
        let $this = $(e.currentTarget)

        let text = $this.parent().find('input').val()
        let type = $this.parents('[data-type]').data('type')

        // Check the text valid.
        if (!core.isValidInput(text)) {
            alertDialog.show({ message : 'Nor white space, tab, or line break are not permitted.' })
            return
        }

        // Chose one at random.
        let aColor = color.choice()

        let d = db.getLabelList()
        let labelObject = d[type] || { labels : [] }
        labelObject.labels.push([ text, aColor ])
        d[type] = labelObject
        db.saveLabelList(d)

        // Re-render.
        $(`.js-label-tab[data-type="${core.getCurrentTab()}"]`).click()

        // Notify color changed.
        color.notifyColorChanged({
            text,
            color    : aColor,
            annoType : core.getCurrentTab()
        })
    })
}
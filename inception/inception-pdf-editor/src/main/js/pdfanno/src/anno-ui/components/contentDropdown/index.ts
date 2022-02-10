import $ from 'jquery'
/**
 * UI parts - Content dropdown.
 */

/**
 * Setup the dropdown of PDFs.
 */
export function setup ({
    initialText,
    overrideWarningMessage,
    contentReloadHandler
}) {

    $('#dropdownPdf .js-text').text(initialText)
    $('#dropdownPdf .js-text').data('initial-text', initialText)

    // TODO pdfという単語を削除したい..

    $('#dropdownPdf').on('click', 'a', e => {

        const $this = $(e.currentTarget)

        // Get the name of PDF clicked.
        const pdfName = $this.find('.js-content-name').text()

        // Get the name of PDF currently displayed.
        const currentPDFName = $('#dropdownPdf .js-text').text()

        // No action, if the current PDF is selected.
        if (currentPDFName === pdfName) {
            console.log('Not reload. the contents are same.')
            return
        }

        // Confirm to override.
        if (currentPDFName !== initialText) {
            if (!window.confirm(overrideWarningMessage)) {
                return
            }
        }

        // Update PDF's name displayed.
        $('#dropdownPdf .js-text').text(pdfName)

        // Update the dropdown selection.
        $('#dropdownPdf .fa-check').addClass('no-visible')
        $this.find('.fa-check').removeClass('no-visible')

        // Reset annotations' dropdowns.
        resetCheckPrimaryAnnoDropdown()
        resetCheckReferenceAnnoDropdown()

        // Close dropdown.
        $('#dropdownPdf').click()

        // Reload Content.
        contentReloadHandler(pdfName)

        return false
    })
}

/**
 * Reset the primary annotation dropdown selection.
 */
function resetCheckPrimaryAnnoDropdown () {
    $('#dropdownAnnoPrimary .js-text').text('Anno File')
    $('#dropdownAnnoPrimary .fa-check').addClass('no-visible')
}

/**
 * Reset the reference annotation dropdown selection.
 */
function resetCheckReferenceAnnoDropdown () {
    $('#dropdownAnnoReference .js-text').text('Reference Files')
    $('#dropdownAnnoReference .fa-check').addClass('no-visible')
}

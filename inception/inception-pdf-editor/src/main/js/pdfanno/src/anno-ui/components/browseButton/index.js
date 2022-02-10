/**
 * UI parts - Browse button.
 */
import * as alertDialog from '../../uis/alertDialog'

let _displayCurrentReferenceAnnotations
let _displayCurrentPrimaryAnnotations
let _getContentFiles
let _getAnnoFiles
let _closePDFViewer

/**
 * Setup the behavior of a Browse Button.
 */
export function setup ({
    loadFiles,
    clearAllAnnotations,
    displayCurrentReferenceAnnotations,
    displayCurrentPrimaryAnnotations,
    getContentFiles,
    getAnnoFiles,
    closePDFViewer,
    callbackLoadedFiles
}) {
    _displayCurrentReferenceAnnotations = displayCurrentReferenceAnnotations
    _displayCurrentPrimaryAnnotations = displayCurrentPrimaryAnnotations
    _getContentFiles = getContentFiles
    _getAnnoFiles = getAnnoFiles
    _closePDFViewer = closePDFViewer

    // Enable to select the same directory twice or more.
    $('.js-file :file').on('click', ev => {
        $('input[type="file"]').val(null)
    })

    $('.js-file :file').on('change', ev => {

        const files = Array.from(ev.target.files)

        let error = isValidDirectorySelect(files)
        if (error) {
            alertDialog.show({ message : error })
            return
        }

        files.sort((a, b) => {
            return a.name.localeCompare(b.name)
        })
        loadFiles(files).then(() => {

            // Get current visuals.
            const current = getCurrentFileNames()

            // Initialize PDF Viewer.
            clearAllAnnotations()

            // Setup PDF Dropdown.
            setPDFDropdownList()

            // Setup Anno Dropdown.
            setAnnoDropdownList()

            // Display a PDF and annotations.
            restoreBeforeState(current)

            callbackLoadedFiles && callbackLoadedFiles(current)
        })
    })
}

/**
 * Check whether the directory the user specified is valid.
 */
function isValidDirectorySelect (files) {

    // Error, if no contents exits.
    if (!files || files.length === 0) {
        return 'No files specified.'
    }

    // Error, if the user select a file - not a directory.
    let relativePath = files[0].webkitRelativePath
    if (!relativePath) {
        return 'Please select a directory, NOT a file.'
    }

    // OK.
    return null
}

/**
 * Restore the state before Browse button was clicked.
 */
function restoreBeforeState (currentDisplay) {

    let files

    let isPDFClosed = false

    // Restore the check state of a content.
    files = _getContentFiles().filter(c => c.name === currentDisplay.pdfName)
    if (files.length > 0) {
        $('#dropdownPdf .js-text').text(files[0].name)
        $('#dropdownPdf a').each((index, element) => {
            let $elm = $(element)
            if ($elm.find('.js-content-name').text() === currentDisplay.pdfName) {
                $elm.find('.fa-check').removeClass('no-visible')
            }
        })

    } else {

        isPDFClosed = true

        _closePDFViewer()
    }

    // Restore the check state of a primaryAnno.
    files = _getAnnoFiles().filter(c => c.name === currentDisplay.primaryAnnotationName)
    if (files.length > 0 && isPDFClosed === false) {
        $('#dropdownAnnoPrimary .js-text').text(currentDisplay.primaryAnnotationName)
        $('#dropdownAnnoPrimary a').each((index, element) => {
            let $elm = $(element)
            if ($elm.find('.js-annoname').text() === currentDisplay.primaryAnnotationName) {
                $elm.find('.fa-check').removeClass('no-visible')
            }
        })
        setTimeout(() => {
            _displayCurrentPrimaryAnnotations()
        }, 100)
    }

    // Restore the check states of referenceAnnos.
    let names = currentDisplay.referenceAnnotationNames
    let colors = currentDisplay.referenceAnnotationColors
    names = names.filter((name, i) => {
        let found = false
        let annos = _getAnnoFiles().filter(c => c.name === name)
        if (annos.length > 0) {
            $('#dropdownAnnoReference a').each((index, element) => {
                let $elm = $(element)
                if ($elm.find('.js-annoname').text() === name) {
                    $elm.find('.fa-check').removeClass('no-visible')
                    $elm.find('.js-anno-palette').spectrum('set', colors[i])
                    found = true
                }
            })
        }
        return found
    })

    if (names.length > 0 && isPDFClosed === false) {
        $('#dropdownAnnoReference .js-text').text(names.join(','))
        setTimeout(() => {
            _displayCurrentReferenceAnnotations()
        }, 500)

    }

}

/**
 * Get the file names which currently are displayed.
 */
function getCurrentFileNames () {

    let text

    // a PDF name.
    text = $('#dropdownPdf .js-text').text()
    let pdfName = (text !== getContentDropdownInitialText() ? text : null)

    // a Primary anno.
    text = $('#dropdownAnnoPrimary .js-text').text()
    let primaryAnnotationName = (text !== 'Anno File' ? text : null)

    let referenceAnnotationNames = []
    let referenceAnnotationColors = []
    $('#dropdownAnnoReference a').each((index, element) => {
        let $elm = $(element)
        if ($elm.find('.fa-check').hasClass('no-visible') === false) {
            let annoName = $elm.find('.js-annoname').text()
            referenceAnnotationNames.push(annoName)
            let color = $elm.find('.js-anno-palette').spectrum('get').toHexString()
            referenceAnnotationColors.push(color)
        }
    })

    return {
        pdfName,
        primaryAnnotationName,
        referenceAnnotationNames,
        referenceAnnotationColors
    }
}

/**
 * Reset and setup the PDF dropdown.
 */
function setPDFDropdownList () {

    // Reset the state of the PDF dropdown.
    $('#dropdownPdf .js-text').text(getContentDropdownInitialText())
    $('#dropdownPdf li').remove()

    // Create and setup the dropdown menu.
    const snipets = _getContentFiles().map(content => {
        return `
            <li>
                <a href="#">
                    <i class="fa fa-check no-visible"></i>&nbsp
                    <span class="js-content-name">${content.name}</span>
                </a>
            </li>
        `
    })
    $('#dropdownPdf ul').append(snipets.join(''))
}

/**
 * Reset and setup the primary/reference annotation dropdown.
 */
function setAnnoDropdownList () {

    // Reset the UI of primary/reference anno dropdowns.
    $('#dropdownAnnoPrimary ul').html('')
    $('#dropdownAnnoReference ul').html('')
    $('#dropdownAnnoPrimary .js-text').text('Anno File')
    $('#dropdownAnnoReference .js-text').text('Reference Files')

    // Setup anno / reference dropdown.
    _getAnnoFiles().forEach(file => {

        let snipet = `
            <li>
                <a href="#">
                    <i class="fa fa-check no-visible" aria-hidden="true"></i>
                    <span class="js-annoname">${file.name}</span>
                </a>
            </li>
        `
        $('#dropdownAnnoPrimary ul').append(snipet)
        $('#dropdownAnnoReference ul').append(snipet)
    })

}

function getContentDropdownInitialText () {
    let value = $('#dropdownPdf .js-text').data('initial-text')
    return (value === undefined || value === '') ? 'PDF File' : value
}

import * as constants from '../shared/constants'

/*
 * Download the result of pdfextract.jar.
 */
import * as annoUI from 'anno-ui'
import { parseUrlQuery } from '../shared/util'

/*
 * Setup the function.
 */
export function setup () {
  reset()
  setupDownloadButton()
  window.addEventListener('didCloseViewer', disable)
  window.addEventListener('willChangeContent', disable)
  window.addEventListener('didChangeContent', enable)
}

/*
 * Reset events.
 */
function reset () {
  $('#downloadPDFTextButton').off('click')
  window.removeEventListener('didCloseViewer', disable)
  window.removeEventListener('willChangeContent', disable)
  window.removeEventListener('didChangeContent', enable)
}

/*
 * Setup the download button.
 */
function setupDownloadButton () {

  $('#downloadPDFTextButton').on('click', e => {

    // Reset the selected state.
    $(e.currentTarget).blur()

    // Get the result of pdfextract.
    const pdftxt = window.annoPage.pdftxt
    if (!pdftxt) {
      annoUI.ui.alertDialog.show({ message : 'No pdftxt is available.' })
      return
    }

    // File name for download.
    const fname = getDownloadFileName()

    // Download.
    let blob = new Blob([pdftxt])
    let blobURL = window.URL.createObjectURL(blob)
    let a = document.createElement('a')
    document.body.appendChild(a) // for firefox working correctly.
    a.download = fname
    a.href = blobURL
    a.click()
    a.parentNode.removeChild(a)
  })
}

/*
 * Enable UI.
 */
function enable () {
  $('#downloadPDFTextButton').removeAttr('disabled')
}

/*
 * Disable UI.
 */
function disable () {
  $('#downloadPDFTextButton').attr('disabled', 'disabled')
}

/**
 * Get the file name for download.
 */
function getDownloadFileName () {

  if (parseUrlQuery()['paper_id']) {
    return parseUrlQuery()['paper_id'] + '.pdf.txt'
  }

  // TODO Refactoring. this function is similar to the one in downloadButton.

  // The name of Primary Annotation.
  let primaryAnnotationName
  $('#dropdownAnnoPrimary a').each((index, element) => {
    let $elm = $(element)
    if ($elm.find('.fa-check').hasClass('no-visible') === false) {
      primaryAnnotationName = $elm.find('.js-annoname').text()
    }
  })
  if (primaryAnnotationName) {
    // return primaryAnnotationName.replace('.anno', '') + 'pdftxt'
    return primaryAnnotationName.replace(`.${constants.ANNO_FILE_EXTENSION}`, '') + 'pdftxt'
  }

  // The name of Content.
  let pdfFileName = window.annoPage.getCurrentContentFile().name
  return pdfFileName + '.txt'
}

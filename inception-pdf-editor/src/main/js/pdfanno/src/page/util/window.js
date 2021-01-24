/**
 * Utility for window.
 */

/**
 * Set the confirm dialog at leaving the page.
 */
export function listenWindowLeaveEvent () {
  window.annotationUpdated = true
  $(window).off('beforeunload').on('beforeunload', () => {
    return 'You don\'t save the annotations yet.\nAre you sure to leave ?'
  })
}

/**
 * Unset the confirm dialog at leaving the page.
 */
export function unlistenWindowLeaveEvent () {
  window.annotationUpdated = false
  $(window).off('beforeunload')
}

/**
 Adjust the height of viewer according to window height.
 */
export function adjustViewerSize () {

  // Only pdfanno.
  if ($('#dropdownPdf ul').length === 0) {
    return
  }

  window.removeEventListener('resize', resizeHandler)
  window.addEventListener('resize', resizeHandler)
  resizeHandler()
}

/**
 * Resize the height of elements adjusting to the window.
 */
export function resizeHandler () {

  // PDFViewer.
  let height = $(window).innerHeight() - $('#viewerInner').offset().top
  $('#viewerInner').css('height', `${height}px`)

  $('.loadingInProgress').css('height', '99%')

  // TODO move to anno-ui.

  // Dropdown for PDF.
  let height1 = $(window).innerHeight() - ($('#dropdownPdf ul').offset().top || 120)
  $('#dropdownPdf ul').css('max-height', `${height1 - 20}px`)

  // Dropdown for Primary Annos.
  let height2 = $(window).innerHeight() - ($('#dropdownAnnoPrimary ul').offset().top || 120)
  $('#dropdownAnnoPrimary ul').css('max-height', `${height2 - 20}px`)

  // Dropdown for Anno list.
  let height3 = $(window).innerHeight() - ($('#dropdownAnnoList ul').offset().top || 120)
  $('#dropdownAnnoList ul').css('max-height', `${height3 - 20}px`)

  // Dropdown for Reference Annos.
  let height4 = $(window).innerHeight() - ($('#dropdownAnnoReference ul').offset().top || 120)
  $('#dropdownAnnoReference ul').css('max-height', `${height4 - 20}px`)

  // Tools.
  let height5 = $(window).innerHeight() - $('#tools').offset().top
  $('#tools').css('height', `${height5}px`)
}

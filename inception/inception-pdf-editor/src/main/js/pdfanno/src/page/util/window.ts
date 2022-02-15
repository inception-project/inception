import $ from 'jquery'

/**
 * Utility for window.
 */

/**
 Adjust the height of viewer according to window height.
 */
export function adjustViewerSize() {

  window.removeEventListener('resize', resizeHandler)
  window.addEventListener('resize', resizeHandler)
  resizeHandler()
}

/**
 * Resize the height of elements adjusting to the window.
 */
export function resizeHandler() {

  // PDFViewer.
  let height = $(window).innerHeight() - $('#viewerInner').offset().top
  $('#viewerInner').css('height', `${height}px`)

  $('.loadingInProgress').css('height', '100%')
}

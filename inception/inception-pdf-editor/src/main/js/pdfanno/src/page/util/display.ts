import $ from 'jquery'
/**
 * The utilities for display.
 */

/**
 * Show or hide a loding.
 */
export function showLoader (display) {
  if (display) {
    $('#pdfLoading').removeClass('close hidden')
  } else {
    $('#pdfLoading').addClass('close')
    setTimeout(function () {
      $('#pdfLoading').addClass('hidden')
    }, 1000)
  }
}


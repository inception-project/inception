/**
 * The utilities for display.
 */

/**
 * Setup the color pickers.
 */
export function setupColorPicker () {

  const colors = [
    'rgb(255, 128, 0)', 'hsv 100 70 50', 'yellow', 'blanchedalmond',
    'red', 'green', 'blue', 'violet'
  ]

  // Setup colorPickers.
  $('.js-anno-palette').spectrum({
    showPaletteOnly        : true,
    showPalette            : true,
    hideAfterPaletteSelect : true,
    palette                : [
      colors.slice(0, Math.floor(colors.length / 2)),
      colors.slice(Math.floor(colors.length / 2), colors.length)
    ]
  })
  // Set initial color.
  $('.js-anno-palette').each((i, elm) => {
    $(elm).spectrum('set', colors[ i % colors.length ])
  })

  // Setup behavior.
  $('.js-anno-palette').off('change').on('change', window.annoPage.displayAnnotation.bind(null, false))
}

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


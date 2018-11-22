const packageJson = require('../../package.json')
import * as pathParse from 'path-parse'

/**
 * PDFExtract Download Button.
 */

export function setup () {
  $('#downloadPDFExtractButton').on('click', e => {
    $(e.currentTarget).blur()
    const a = $('<a>').attr({
      href     : packageJson.pdfextract.url,
      download : pathParse(packageJson.pdfextract.url).base
    }).appendTo($('body'))
    a[0].click()
    a.remove()
    return false
  })
}

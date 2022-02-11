import * as pako from 'pako'

/**
 *  Functions depending on pdfanno-core.js.
 */

/**
 * Read .pdf.txt.gz file
 * @export
 * @param {*} file
 * @returns
 */
export function readPdftxt (file) {
  const reader = new FileReader()
  return new Promise((resolve, reject) => {
    reader.onload = () => {
      resolve(pako.inflate(reader.result, {to : 'string'}))
    }
    reader.readAsArrayBuffer(file)
  })
}

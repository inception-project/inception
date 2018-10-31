import * as constants from '../../shared/constants'
import * as pako from 'pako'

/**
 *  Functions depending on pdfanno-core.js.
 */

/**
 * Load PDFs and Annos via Browse button.
 */
export function loadFiles (files) {

  let { pdfNames, annoNames } = getContents(files)

  return new Promise((resolve, reject) => {

    let promises = []

    // Load pdfs.
    let p = pdfNames.map(file => {
      return new Promise((resolve, reject) => {

        let fileReader = new FileReader()

        fileReader.onload = event => {
          resolve({
            type    : 'content',
            name    : _excludeBaseDirName(file.pdf.webkitRelativePath),
            content : event.target.result,
            file    : file.file
          })
        }
        fileReader.readAsArrayBuffer(file.pdf)
      })
    })
    promises = promises.concat(p)

    // Load annos.
    p = annoNames.map(file => {
      return new Promise((resolve, reject) => {

        let fileReader = new FileReader()

        fileReader.onload = event => {
          resolve({
            type    : 'anno',
            name    : _excludeBaseDirName(file.webkitRelativePath),
            content : event.target.result
          })
        }
        fileReader.readAsText(file)
      })
    })
    promises = promises.concat(p)

    // Wait for complete.
    Promise.all(promises).then(results => {

      results = sortByName(results)

      const contents = results.filter(r => r.type === 'content')
      const annos = results.filter(r => r.type === 'anno')

      resolve({ contents, annos })
    })
  })
}

/**
 * Sort objects by name.
 */
function sortByName (items) {
  items.sort((a, b) => {
    if (a.name < b.name) {
      return -1
    } else if (a.name > b.name) {
      return 1
    } else {
      return 0
    }
  })
  return items
}

/**
 * Extract PDFs and annotations from files the user specified.
 */
function getContents (files) {
  let pdfNames = []
  let annoNames = []
  const re = /(.*\.pdf)\.[^.]+\.txt\.gz/i

  for (let i = 0; i < files.length; i++) {

    let pdf = files[i]
    let relativePath = pdf.webkitRelativePath

    let frgms = relativePath.split('/')
    if (frgms.length > 2) {
      continue
    }

    // console.log('Load:', relativePath)

    // Get files only PDFs or Anno files.
    if (relativePath.match(/\.pdf$/i)) {
      // find pdftxt file
      const found = findPdftxt(re, files, relativePath)
      if (found) {
        pdfNames.push({pdf, file : found})
      }
    } else if (relativePath.match(new RegExp(`\\.${constants.ANNO_FILE_EXTENSION}$`, 'i'))) {
      annoNames.push(pdf)
    }
  }

  return {
    pdfNames,
    annoNames
  }
}

/**
 * Find pdftxt file
 * @param {String} re
 * @param {Array<String>} files
 * @param {String} pdf
 */
function findPdftxt (re, files, pdf) {
  return files.find(v => {
    const match = v.webkitRelativePath.match(re)
    return match && match.length >= 2 && pdf === match[1]
  })
}

/**
 * Get a filename from a path.
 */
function _excludeBaseDirName (filePath) {
  let frgms = filePath.split('/')
  return frgms[frgms.length - 1]
}

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

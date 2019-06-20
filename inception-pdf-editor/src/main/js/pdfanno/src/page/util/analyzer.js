/**
 * Convert analyze results as page-based.
 *
 * version 0.3.0
 *   https://github.com/paperai/paperanno-ja/issues/145#issuecomment-405008065
 *   page char x y w h
 *   1<TAB>A<TAB>298.66446 724.539 5.4545503 13.145466
 *   2<TAB>[MOVE_TO]<TAB>129.75435 61.265076
 *   2<TAB>NO_UNICODE<TAB>130.1669 217.66483 4.314926 8.606305
 *
 * version 0.2.4
 *   position page type char x y w h ? ? ? ?
 *   9426<TAB>4<TAB>TEXT<TAB>G<TAB>435.29147 62.84513 7.7976065 13.014011<TAB>435.6371 65.52354 7.3116064 7.4520063
 *   9427<TAB>4<TAB>DRAW<TAB>MOVE_TO<TAB>129.75435<TAB>79.0188
 *
 */
export function customizeAnalyzeResult (pdftxt) {

  let pages = []
  let page
  let body
  let meta
  let tokenId = 0
  pdftxt.split('\n').forEach(line => {

    tokenId++

    if (page && !line) {
      body += ' '
      meta.push(line)
    } else {
      let info = line.split('\t')

      if (!isTextLine(info)) {
        return
      }

      info = parseInfo([tokenId].concat(info))

      let {
        page : pageNumber,
        type,
        char
      } = extractMeta(info)
      if (!page) {
        page = pageNumber
        body = ''
        meta = []
      } else if (page !== pageNumber) {
        pages.push({
          body,
          meta,
          page
        })
        body = ''
        meta = []
        page = pageNumber
      }
      if (type === 'TEXT') {
        // Special replace, like "NO_UNICODE"
        if (char === 'NO_UNICODE') {
          char = '?'
        }
        body += char
        meta.push(info)
      }
    }
  })
  pages.push({
    body,
    meta,
    page
  })

  return pages
}

/**
 * Check the line is TEXT.
 */
function isTextLine (info) {
  // info must be "page<TAB>char<TAB>x y w h"
  if (info.length === 3 && info[2].split(' ').length === 4) {
    if (info[1].length >= 2 && info[1].startsWith('[')) {
      // 1 [MOVE_TO] 129.75435 61.265076
      return false
    }
  } else {
    // 1 [FILL_PATH]
    return false
  }
  return true
}

/**
 * Parse Info data.
 * @param {*} info
 * @returns
 */
function parseInfo (info) {
  return [
    parseInt(info[0]),
    parseInt(info[1]),
    'TEXT',
    info[2],
    info[3].split(' ').map(parseFloat)
  ]
}

/**
 * Interpret the meta data.
 */
export function extractMeta (meta) {
  const [ x, y, w, h ] = meta[4]
  return {
    position : meta[0],
    page     : meta[1],
    type     : 'TEXT',
    char     : meta[3],
    x,
    y,
    w,
    h
  }
}

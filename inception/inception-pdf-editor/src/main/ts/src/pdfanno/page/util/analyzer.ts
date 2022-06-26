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
 */
export interface Page {
  body: string;
  glyphs: Info[];
  page: number;
}

export type Info = [
  position: number,
  page: number,
  type: string,
  char: string,
  meta: number[]
]

export class Meta {
  position: number
  pageNumber: number
  type: string
  glyph: string
  x: number
  y: number
  w: number
  h: number

  static parse (info: Info): Meta {
    const [x, y, w, h] = info[4]
    const m = new Meta()
    m.position = info[0]
    m.pageNumber = info[1]
    m.type = 'TEXT'
    m.glyph = info[3]
    m.x = x
    m.y = y
    m.w = w
    m.h = h
    return m
  }
}

export function loadPageInformation (pdftxt: string): Page[] {
  const pages: Page[] = []
  let page: number
  let body: string
  let meta: Info[]
  let tokenId = 0

  pdftxt.split('\n').forEach(line => {
    tokenId++

    if (page && !line) {
      body += ' '
      meta.push(undefined)
      return
    }

    const lineFields = line.split('\t')
    if (!isTextLine(lineFields)) {
      return
    }

    const info = parseInfo(tokenId, lineFields)

    let { pageNumber, type, glyph } = extractMeta(info)

    if (!page) {
      page = pageNumber
      body = ''
      meta = []
    } else if (page !== pageNumber) {
      pages.push({ body, glyphs: meta, page })
      body = ''
      meta = []
      page = pageNumber
    }

    if (type === 'TEXT') {
      // Special replace, like "NO_UNICODE"
      if (glyph === 'NO_UNICODE') {
        glyph = '?'
      }
      body += glyph
      meta.push(info)
    }
  })

  pages.push({ body, glyphs: meta, page })

  return pages
}

/**
 * Check the line is TEXT.
 */
function isTextLine (lineFields: string[]): boolean {
  // info must be "page<TAB>char<TAB>x y w h"
  if (lineFields.length === 3 && lineFields[2].split(' ').length === 4) {
    if (lineFields[1].length >= 2 && lineFields[1].startsWith('[')) {
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
 */
function parseInfo (tokenId: number, lineFields: string[]): Info {
  return [
    tokenId,
    parseInt(lineFields[0]),
    'TEXT',
    lineFields[1],
    lineFields[2].split(' ').map(parseFloat)
  ]
}

/**
 * Interpret the meta data.
 */
export function extractMeta (meta): Meta {
  return Meta.parse(meta)
}

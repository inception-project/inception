/**
 * Create text layers which enable users to select texts.
 */
import { loadPageInformation, extractMeta, Page, Meta } from './util/analyzer'

/**
 * Text layer data.
 */
let pages: Page[] = []

/**
 * Setup text layers.
 */
export function setup (analyzeData: string) {
  pages = loadPageInformation(analyzeData)
}

function getPage (num: number): Page {
  return pages.find(p => p.page === num)
}

/**
 * Find index between characters in the text.
 * Used for zero-width span annotations.
 * @param pageNum - the page number.
 * @param point - { x, y } coords.
 * @return {*} - The nearest text index to the given point.
 */
export function findIndex (pageNum: number, point: { x: number, y: number }): number {
  const page = getPage(pageNum)

  if (!page) {
    return null
  }

  const infoList = page.glyphs

  for (let i = 0, len = infoList.length; i < len; i++) {
    const info = infoList[i]

    if (!info) {
      continue
    }

    const { position, x, y, w, h } = extractMeta(info)

    if (y <= point.y && point.y <= (y + h)) {
      if (x <= point.x && point.x <= x + w / 2) {
        return position
      } else if (x + w / 2 < point.x && point.x <= x + w) {
        return position + 1
      }
    }
  }

  return null
}

/**
 * Find the glyph at the given position in the PDF document. This operation uses the glyph
 * position data that is provided by the server.
 *
 * @param pageNum - the page number.
 * @param point - { x, y } coords.
 * @returns {*} - The text data if found, whereas null.
 */
export function findGlyphAtPoint (pageNum: number, point: {x: number, y: number}): Meta {
  const page = getPage(pageNum)

  if (!page) {
    return null
  }

  const infoList = page.glyphs

  for (let i = 0, len = infoList.length; i < len; i++) {
    const info = infoList[i]

    if (!info) {
      continue
    }

    const m = extractMeta(info)

    // is Hit?
    if (m.x <= point.x && point.x <= (m.x + m.w) && m.y <= point.y && point.y <= (m.y + m.h)) {
      return m
    }
  }

  return null
}

/**
 * Find the texts.
 * @param pageNum - the page number.
 * @param startPosition - the start position in pdftxt.
 * @param endPosition - the end position in pdftxt.
 * @param allowZeroWidth - whether zero-width spans are allowed or not, required for range usage
 * @returns {Array} - the texts.
 */
export function getGlyphsInRange (pageNum: number, startPosition: number, endPosition: number, allowZeroWidth?: boolean): Meta[] {
  const items = []

  if (startPosition == null || endPosition == null) {
    return items
  }

  const page = getPage(pageNum)

  if (!page) {
    return null
  }

  const glyphs = page.glyphs

  let inRange = false

  for (let index = 0, len = glyphs.length; index < len; index++) {
    const info = glyphs[index]

    if (!info) {
      if (inRange) {
        items.push(null)
      }
      continue
    }

    const data = extractMeta(info)
    var last = data
    const { position } = data

    // if zero-width spans are allowed interpret ranges different from PDFAnno default
    // [3,3] is zero-width, [3,4] is one character
    if (allowZeroWidth) {
      if (startPosition === endPosition && startPosition === position) {
        data.w = 1
        data.x = data.x - 1
        items.push(data)
        break
      }

      if (startPosition <= position && position < endPosition) {
        inRange = true
        items.push(data)
      }

      if (position === endPosition) {
        break
      }

      if (last.position < data.position) last = data
      // if zero-width spans are not allowed interpret ranges as PDFAnno does usually
      // [3,3] is one character, [3,4] are two characters
    } else {
      if (startPosition <= position) {
        inRange = true
        items.push(data)
      }

      if (endPosition <= position) {
        break
      }
    }
  }

  // handle case where position is the very last index for a zero-width span
  // which does not exist in the pdfextract file and therefore cannot be found
  // use the previous character box and "append" the zero-width span after it
  if (items.length === 0 && startPosition === endPosition && pageNum === pages.length && allowZeroWidth) {
    items.push({
      position: startPosition,
      page: last.pageNumber,
      type: last.type,
      char: '',
      x: last.x + last.w,
      y: last.y,
      w: 1,
      h: last.h
    })
  }

  return items
}

export class Rect {
  top: number
  left: number
  right: number
  bottom: number
  x: number
  y: number
  width: number
  height: number

  static fromMeta (m: Meta): Rect {
    const r = new Rect()
    r.top = m.y
    r.left = m.x
    r.right = m.x + m.w
    r.bottom = m.y + m.h
    r.x = m.x
    r.y = m.y
    r.width = m.w
    r.height = m.h
    return r
  }
}

/**
 * Merge user selections.
 */
export function mergeRects (metas: Meta[]): Rect[] {
  // Remove null.
  metas = metas.filter(m => m)

  if (metas.length === 0) {
    return []
  }

  // Normalize.
  const rects = metas.map(m => Rect.fromMeta(m))

  // a vertical margin of error.
  const error = 5 * scale()

  let tmp = rects[0]
  const newRects = [tmp]
  for (let i = 1; i < rects.length; i++) {
    // Same line -> Merge rects.
    if (withinMargin(rects[i].top, tmp.top, error)) {
      tmp.top = Math.min(tmp.top, rects[i].top)
      tmp.left = Math.min(tmp.left, rects[i].left)
      tmp.right = Math.max(tmp.right, rects[i].right)
      tmp.bottom = Math.max(tmp.bottom, rects[i].bottom)
      tmp.x = tmp.left
      tmp.y = tmp.top
      tmp.width = tmp.right - tmp.left
      tmp.height = tmp.bottom - tmp.top

      // New line -> Create a new rect.
    } else {
      tmp = rects[i]
      newRects.push(tmp)
    }
  }

  return newRects
}

/**
 * Check the value(x) within the range.
 */
function withinMargin (x: number, base: number, margin: number) {
  return (base - margin) <= x && x <= (base + margin)
}

/**
 * Returns the scaling factor of the PDF. If the PDF has not been drawn yet, a factor of 1 is
 * assumed.
 */
function scale () {
  if (window.PDFViewerApplication.pdfViewer.getPageView(0) === undefined) {
    return 1
  } else {
    return window.PDFViewerApplication.pdfViewer.getPageView(0).viewport.scale
  }
}

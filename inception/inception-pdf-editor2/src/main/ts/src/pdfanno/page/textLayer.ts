/**
 * Create text layers which enable users to select texts.
 */
import { Offsets } from '@inception-project/inception-js-api'
import { VGlyph } from '../../vmodel/VGlyph'
import { deserializeVModelFromJson } from '../../vmodel/VModelJsonDeserializer'
import { VPage } from '../../vmodel/VPage'

/**
 * Text layer data.
 */
let pages: VPage[] = []

/**
 * Setup text layers.
 */
export function setup (analyzeData: string) {
  pages = deserializeVModelFromJson(analyzeData)
}

export function getPage (num: number): VPage | undefined {
  return pages.find(p => p.index === num)
}

export function findPageForOffset (offset: number): VPage | undefined {
  const page = pages.find(p => p.range[0] <= offset && offset < p.range[1])
  if (!page) {
    console.error(`No page found for offset [${offset}]. Last offset is [${pages[pages.length - 1].range[1]}]`)
  }
  return page
}

/**
 * Find index between characters in the text.
 * Used for zero-width span annotations.
 * @param pageNum - the page number.
 * @param point - { x, y } coords.
 * @return {*} - The nearest text index to the given point.
 */
export function findCharacterOffset (pageNum: number, point: { x: number, y: number }): number | undefined {
  const page = getPage(pageNum)

  if (!page) {
    return undefined
  }

  for (const g of page.glyphs) {
    if (g.bbox.x <= point.y && point.y <= (g.bbox.y + g.bbox.h)) {
      if (g.bbox.x <= point.x && point.x <= g.bbox.x + g.bbox.w / 2) {
        return g.begin
      } else if (g.bbox.x + g.bbox.w / 2 < point.x && point.x <= g.bbox.x + g.bbox.w) {
        return g.begin + 1
      }
    }
  }

  return undefined
}

/**
 * Find the glyph at the given position in the PDF document. This operation uses the glyph
 * position data that is provided by the server.
 *
 * @param pageNum - the page number.
 * @param point - { x, y } coords.
 * @returns {*} - The text data if found, whereas null.
 */
export function findGlyphAtPoint (pageNum: number, point: { x: number, y: number }): VGlyph | undefined {
  const page = getPage(pageNum)

  if (!page) {
    return undefined
  }

  return page.glyphs.find(g => g.bbox.contains(point))
}

function overlapping (range1: Offsets, range2: Offsets): boolean {
  const aXBegin = range1[0]
  const aXEnd = range1[1]
  const aYBegin = range2[0]
  const aYEnd = range2[1]
  return aYBegin === aXBegin || aYEnd === aXEnd || (aXBegin < aYEnd && aYBegin < aXEnd)
}

export function getGlyphsInRange (range: Offsets): VGlyph[] {
  if (!range) {
    return []
  }

  const glyphs = []
  let currentPage = findPageForOffset(range[0])
  while (currentPage && overlapping(range, currentPage.range)) {
    for (const g of currentPage.glyphs) {
      if (range[0] <= g.begin && g.begin < range[1]) {
        glyphs.push(g)
      }
    }
    currentPage = getPage(currentPage.index + 1)
  }

  return glyphs
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

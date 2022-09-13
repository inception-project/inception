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

export function getPageBefore (num: number): VPage | undefined {
  let pageBefore : VPage | undefined
  for (const page of pages) {
    if (page.index === num) {
      return pageBefore
    }
    pageBefore = page
  }
  return pageBefore
}

export function getPageAfter (num: number): VPage | undefined {
  let stop = false
  for (const page of pages) {
    if (stop) {
      return page
    }

    if (page.index === num) {
      stop = true
    }
  }

  return undefined
}

export function findPageForTextOffset (offset: number): VPage | undefined {
  const page = pages.find(p => p.range[0] <= offset && offset < p.range[1])
  if (!page) {
    console.error(`No page found for offset [${offset}]. Last offset is [${pages[pages.length - 1].range[1]}]`)
  }
  return page
}

/**
 * Find the glyph at the given position in the PDF document. This operation uses the glyph
 * position data that is provided by the server.
 *
 * @param pageNum - the page number.
 * @param point - { x, y } coords.
 * @returns {*} - The text data if found, whereas null.
 */
export function findGlyphAtPointWithinPage (pageNum: number, point: { x: number, y: number }): VGlyph | undefined {
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

export function getGlyphAtTextOffset (offset: number): VGlyph | null {
  const page = findPageForTextOffset(offset)
  if (!page) {
    return null
  }

  const glyph = page.glyphs.find(g => g.begin <= offset && offset < g.end)
  if (!glyph) {
    return null
  }

  return glyph
}

export function getGlyphsInRange (range: Offsets): VGlyph[] {
  if (!range) {
    return []
  }

  const glyphs : VGlyph[] = []
  let currentPage = findPageForTextOffset(range[0])
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
export function scale () {
  if (globalThis.PDFViewerApplication.pdfViewer.getPageView(0) === undefined) {
    return 1
  } else {
    return globalThis.PDFViewerApplication.pdfViewer.getPageView(0).viewport.scale
  }
}

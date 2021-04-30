/**
 * Search functions.
 */
import { paddingBetweenPages } from '../shared/coords'
import { customizeAnalyzeResult, extractMeta } from './util/analyzer'
import { searchUI } from 'anno-ui'

/**
 * the Color for search results.
 */
const SEARCH_COLOR = '#FF0'

/**
 * the Color for a selected search result.
 */
const SEARCH_COLOR_HIGHLIGHT = '#0F0'

/**
 * The highlights for search.
 */
let searchHighlights = []

/**
 * Setup the search function.
 */
export function setup (analyzeData) {
  searchUI.setup({
    pages                : customizeAnalyzeResult(analyzeData),
    scrollTo             : highlightSearchResult.bind(this),
    searchResultRenderer : renderHighlight.bind(this),
    resetUIAfter         : resetUI.bind(this)
  })
  searchUI.enableSearchUI()

  // Adjusting window resize.
  window.addEventListener('annotationlayercreated', () => {
    searchHighlights.forEach(span => {
      span.render()
    })
  })
}

/**
 * Get the current highlight.
 */
export function getSearchHighlight () {
  const searchPosition = searchUI.searchPosition()
  if (searchPosition > -1) {
    return searchHighlights[searchPosition]
  }
  return null
}

/**
 * Highlight a single search result.
 */
function highlightSearchResult (searchPosition) {

  const highlight = searchHighlights[searchPosition]
  highlight.color = SEARCH_COLOR_HIGHLIGHT
  highlight.render()

  // Reset others.
  searchHighlights.forEach((span, i) => {
    if (searchPosition !== i) {
      span.color = SEARCH_COLOR
      span.render()
    }
  })

  // Scroll to.
  let pageHeight = window.annoPage.getViewerViewport().height
  let scale = window.annoPage.getViewerViewport().scale
  let _y = (pageHeight + paddingBetweenPages) * (highlight.page - 1) + highlight.rectangles[0].top * scale
  _y -= 100
  $('#viewer').parent()[0].scrollTop = _y
}

/**
 * Reset the UI display.
 * @see anno-ui.searchUI#resetUI()
 */
function resetUI () {
  searchHighlights.forEach(span => span.destroy())
  searchHighlights = []
}

/**
 * render search results as highlight.
 */
function renderHighlight (positions, pageData) {

  positions.forEach(position => {

    const targets = pageData.meta.slice(position.start, position.end).map(meta => {
      return extractMeta(meta)
    })
    if (targets.length > 0) {
      const startPosition = targets[0].position
      const endPosition = targets[targets.length - 1].position
      const mergedRect = window.mergeRects(targets)
      const selectedText = targets.map(t => {
        return t ? t.char : ' '
      }).join('')
      const spanAnnotation = window.saveSpan({
        rects        : mergedRect,
        page         : pageData.page,
        save         : false,
        focusToLabel : false,
        color        : SEARCH_COLOR,
        knob         : false,
        border       : false,
        textRange    : [ startPosition, endPosition ],
        selectedText
      })
      spanAnnotation.disable()
      searchHighlights.push(spanAnnotation)
    }
  })

  if (searchHighlights.length > 0) {
    // Highlight one at the current page.
    const currentPage = window.PDFViewerApplication.page
    let found = false
    for (let i = 0; i < searchHighlights.length; i++) {
      if (currentPage === searchHighlights[i].page) {
        searchUI.setSearchPosition(i)
        found = true
        break
      }
    }

    // If there is no result at the current page, set the index 0.
    if (!found) {
      searchUI.setSearchPosition(0)
    }
  }
}

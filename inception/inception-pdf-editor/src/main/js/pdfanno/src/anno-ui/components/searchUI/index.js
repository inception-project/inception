import $ from 'jquery'

/**
 * Search functions.
 */

/**
 * The analyze data per pages.
 */
let _pages = []

/**
 * Search type ( text / dictionary )
 */
let _searchType = null

/**
 * The position where a search result is highlighted.
 */
let _searchPosition = -1

/**
 * Texts for dictionary search.
 */
let dictonaryTexts

let _scrollTo

let _searchResultRenderer

let _resetUIAfter

let _hitCount

/**
 * Setup the search function.
 */
export function setup ({
    pages,
    scrollTo,
    searchResultRenderer,
    resetUIAfter
}) {
    _pages = pages
    _scrollTo = scrollTo
    _searchResultRenderer = searchResultRenderer
    _resetUIAfter = resetUIAfter
}

export function enableSearchUI () {
    $('#searchWord, .js-dict-match-file').removeAttr('disabled')
}

export function searchPosition () {
    return _searchPosition
}

export function setSearchPosition (value) {
    _searchPosition = value
    highlightSearchResult()
}

export function searchType () {
    return _searchType
}

window.addEventListener('DOMContentLoaded', () => {

    const DELAY = 500
    let timerId

    $('#searchWord').on('keyup', e => {

        // Enter key.
        if (e.keyCode === 13) {
            nextResult()
            return
        }

        if (timerId) {
            clearTimeout(timerId)
            timerId = null
        }

        timerId = setTimeout(() => {
            timerId = null
            _searchType = 'text'
            doSearch()
        }, DELAY)
    })

    $('.js-search-case-sensitive, .js-search-regexp').on('change', () => {
        _searchType = 'text'
        doSearch()
    })

    $('.js-search-case-sensitive, .js-search-regexp').on('click', e => {
        $(e.currentTarget).blur()
    })

    $('.js-search-clear').on('click', e => {
        // Clear search.
        $('#searchWord').val('')
        _searchType = null
        doSearch()
        $(e.currentTarget).blur()
    })

    $('.js-search-prev, .js-search-next').on('click', e => {

        if (_searchType !== 'text') {
            return
        }

        // No action for no results.
        if (_hitCount === 0) {
            return
        }

        if ($(e.currentTarget).hasClass('js-search-prev')) {
            prevResult()
        } else {
            nextResult()
        }
    })
})

/**
 * Highlight the prev search result.
 */
function prevResult () {
    _searchPosition--
    if (_searchPosition < 0) {
        _searchPosition = _hitCount - 1
    }
    highlightSearchResult()
}

/**
 * Highlight the next search result.
 */
function nextResult () {
    _searchPosition++
    if (_searchPosition >= _hitCount) {
        _searchPosition = 0
    }
    highlightSearchResult()
}

/**
 * Highlight a single search result.
 */
function highlightSearchResult () {
    switch (_searchType) {
        case 'text':
            $('.search-current-position').text(_searchPosition + 1)
            break
        case 'dictionary':
            $('.js-dict-match-cur-pos').text(_searchPosition + 1)
            break
    }
    _scrollTo(_searchPosition)
}

/**
 * Search the position of  a word / words which an user input.
 */
function search ({ hay, needle, isCaseSensitive = false, useRegexp = false }) {
    if (!needle) {
        return []
    }
    const SPECIAL_CHARS_REGEX = /[-[\]/{}()*+?.\\^$|]/g
    const flags = 'g' + (isCaseSensitive === false ? 'i' : '')
    if (useRegexp === false) {
        needle = needle.replace(SPECIAL_CHARS_REGEX, '\\$&')
    }
    let re = new RegExp(needle, flags)
    let positions = []
    let match
    while ((match = re.exec(hay)) != null) {
        positions.push({
            start : match.index,
            end   : match.index + match[0].length
        })
    }
    return positions
}

/**
 * Reset the UI display.
 */
function resetUI () {
    $('.search-hit').addClass('hidden')
    $('.search-current-position, .search-hit-count').text('0')
    $('.js-dict-match-cur-pos, .js-dict-match-hit-counts').text('000')
    _resetUIAfter()
}

/**
 * Search the word and display.
 */
function doSearch ({ query = null } = {}) {

    // Check enable.
    if ($('#searchWord').is('[disabled]')) {
        console.log('Search function is not enabled yet.')
        return
    }

    resetUI()

    let text
    let isCaseSensitive
    let useRegexp
    if (_searchType === 'text') {
        text = $('#searchWord').val()
        isCaseSensitive = $('.js-search-case-sensitive')[0].checked
        useRegexp = $('.js-search-regexp')[0].checked
    } else {
        text = query
        isCaseSensitive = $('.js-dict-match-case-sensitive')[0].checked
        useRegexp = true
    }

    console.log(`doSearch: _searchType=${_searchType} text="${text}", caseSensitive=${isCaseSensitive}, regexp=${useRegexp}`)

    // Reset.
    _searchPosition = -1

    // The min length of text for searching.
    const MIN_LEN = 2
    if (!text || text.length < MIN_LEN) {
        return
    }

    _hitCount = 0
    _pages.forEach(page => {

        // Search.
        const _positions = search({ hay : page.body, needle : text, isCaseSensitive, useRegexp })
        _searchResultRenderer(_positions, page, text)
        _hitCount += _positions.length
    })

    if (_searchType === 'text') {
        $('.search-hit').removeClass('hidden')
        $('.search-current-position').text(_searchPosition + 1)
        $('.search-hit-count').text(_hitCount)
    } else {
        // Dict matching.
        $('.js-dict-match-cur-pos').text(_searchPosition + 1)
        $('.js-dict-match-hit-counts').text(_hitCount)
    }
}

/**
 * Dictonary Matching.
 */
window.addEventListener('DOMContentLoaded', () => {

    // Clear prev cache.
    $('.js-dict-match-file :file').on('click', e => {
        $(e.currentTarget).val(null)
    })

    // Load a dictionary for matching.
    $('.js-dict-match-file :file').on('change', e => {

        const files = e.target.files
        if (files.length === 0) {
            window.annoUI.ui.alertDialog.show({ message : 'Select a file.' })
            return
        }

        const fname = files[0].name
        $('.js-dict-match-file-name').text(fname)

        let fileReader = new FileReader()
        fileReader.onload = ev => {
            const texts = ev.target.result.split('\n').map(t => {
                return t.trim()
            }).filter(t => {
                return t
            })
            if (texts.length === 0) {
                window.annoUI.ui.alertDialog.show({ message : 'No text is found in the dictionary file.' })
                return
            }
            dictonaryTexts = texts
            searchByDictionary(texts)
        }
        fileReader.readAsText(files[0])
    })

    // Clear search results.
    $('.js-dict-match-clear').on('click', e => {
        _searchType = null
        doSearch()
        $(e.currentTarget).blur()
    })

    // Go to the prev/next result.
    $('.js-dict-match-prev, .js-dict-match-next').on('click', e => {

        if (_searchType !== 'dictionary') {
            return
        }

        // No action for no results.
        if (_hitCount === 0) {
            return
        }

        // go to next or prev.
        let num = 1
        if ($(e.currentTarget).hasClass('js-dict-match-prev')) {
            num = -1
        }
        _searchPosition += num
        if (_searchPosition < 0) {
            _searchPosition = _hitCount - 1
        } else if (_searchPosition >= _hitCount) {
            _searchPosition = 0
        }

        highlightSearchResult()
    })

    // Set the search behavior.
    $('.js-dict-match-case-sensitive').on('change', () => {
        searchByDictionary(dictonaryTexts)
    })
})

/**
 * Search by a dict file.
 */
function searchByDictionary (texts = []) {
    console.log('searchByDictionary:', texts)
    _searchType = 'dictionary'
    const query = texts.join('|')
    doSearch({ query })
}

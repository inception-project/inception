/**
 * UI parts - Anno List Dropdown.
 */

/**
 * Setup the dropdown for Anno list.
 */
export function setup ({
    getAnnotations,
    scrollToAnnotation
}) {
    // Show the list of primary annotations.
    $('#dropdownAnnoList').on('click', () => {
        // Create html snipets.
        let elements = getAnnotations().map(a => {
            let icon
            if (a.type === 'span') {
                icon = '<i class="fa fa-pencil"></i>'
            } else if (a.type === 'relation' && a.direction === 'one-way') {
                icon = '<i class="fa fa-long-arrow-right"></i>'
            } else if (a.type === 'relation' && a.direction === 'two-way') {
                icon = '<i class="fa fa-arrows-h"></i>'
            } else if (a.type === 'relation' && a.direction === 'link') {
                icon = '<i class="fa fa-minus"></i>'
            } else if (a.type === 'area') {
                icon = '<i class="fa fa-square-o"></i>'
            }

            let snipet = `
                <li>
                    <a href="#" data-id="${a.uuid}">
                        ${icon}&nbsp&nbsp;<span>${a.text || ''}</span>
                    </a>
                </li>
            `
            return snipet
        })
        $('#dropdownAnnoList ul').html(elements)
    })

    // Jump to the page that the selected annotation is at.
    $('#dropdownAnnoList').on('click', 'a', e => {
        let id = $(e.currentTarget).data('id')

        scrollToAnnotation(id)

        // Close the dropdown.
        $('#dropdownAnnoList').click()
    })

    // Update the number of display, at adding / updating/ deleting annotations.
    function watchPrimaryAnno (e) {
        $('#dropdownAnnoList .js-count').text(getAnnotations().length)
    }
    $(window)
        .off('annotationrendered annotationUpdated annotationDeleted', watchPrimaryAnno)
        .on('annotationrendered annotationUpdated annotationDeleted', watchPrimaryAnno)
}

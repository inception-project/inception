let _applicationName = 'pdfanno'

export function setup ({
    applicationName
}) {
    _applicationName = applicationName
}

export function applicationName () {
    return _applicationName
}

export const validLabelTypes = ['span', 'one-way', 'two-way', 'link']

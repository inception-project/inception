import toml from 'toml'
import * as annoUtils from '../../utils'
import { validLabelTypes } from '../../core'

/**
 * Read the label list from File object
 * @param File(Blob) object
 * @return Promise.resolve(labelData) ... success, returned labelData (this will use to `db.setLabelList(labelData)`)
 * @return Promise.reject(DOMError) ... error occurred on read the fileObj
 * @return Promise.reject(TypeError) ... invalid label type is found in read from fileObj
 */
export default async function (fileObj) {
    const tomlString = await annoUtils.loadFileAsText(fileObj)
    if (tomlString === '') {
        throw new TypeError('Empty data')
    }
    const labelData = toml.parse(tomlString)
    for (let key in labelData) {
        if (!validLabelTypes.includes(key)) {
            throw new TypeError('Invalid label type; ' + key)
        }
    }
    return labelData
}

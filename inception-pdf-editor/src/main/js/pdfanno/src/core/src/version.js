// let packageJson = require('json-loader!../../../package.json')
const packageJson = require('../../../package.json')
/**
 * Paper Anno Version.
 * This is overwritten at build.
 */
export let ANNO_VERSION = packageJson.version
export let PDFEXTRACT_VERSION = packageJson.pdfextract.version

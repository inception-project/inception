/**
 * Utilities for TOML format.
 */
import toml from 'toml'

/**
 * Create a TOML String from jsObject.
 */
export function toTomlString (obj, root = true) {

  let lines = []

  // `version` and `pdfextract-version` are the first.
  if ('pdfanno' in obj) {
    lines.push(`pdfanno = "${obj['pdfanno']}"`)
    lines.push(`pdfextract = "${obj['pdfextract']}"`)
    lines.push('')
    delete obj['pdfanno']
    delete obj['pdfextract']
  }

  Object.keys(obj).forEach(prop => {

    let val = obj[prop]
    if (typeof val === 'string') {
      lines.push(`${prop} = "${val}"`)
      root && lines.push('')

    } else if (typeof val === 'number') {
      lines.push(`${prop} = ${val}`)
      root && lines.push('')

    } else if (prop === 'spans' || prop === 'relations') {
      if (isArray(val)) {
        val.forEach(v => {
          lines.push(`[[${prop}]]`)
          lines.push(toTomlString(v, false))
          root && lines.push('')
        })
      }

    } else if (isArray(val)) {
      lines.push(`${prop} = ${JSON.stringify(val)}`)
      root && lines.push('')

    }
  })

  return lines.join('\n')
}

/**
 * Create a TOML String from jsObject.
 */
export function toTomlString040 (obj, root = true) {

  let lines = []

  // `version` and `pdfextract-version` are the first.
  if ('version' in obj) {
    lines.push(`version = "${obj['version']}"`)
    lines.push(`pdfextract-version = "${obj['pdfextract-version']}"`)
    lines.push('')
    delete obj['version']
    delete obj['pdfextract-version']
  }

  // #paperanno-ja/issues/38
  // Make all values in `position` as string.
  if ('position' in obj) {
    let position = obj.position
    position = position.map(p => {
      if (typeof p === 'number') {
        return String(p)
      } else {
        return p.map(v => String(v))
      }
    })
    obj.position = position
  }

  Object.keys(obj).forEach(prop => {

    let val = obj[prop]
    if (typeof val === 'string') {
      lines.push(`${prop} = "${val}"`)
      root && lines.push('')

    } else if (typeof val === 'number') {
      lines.push(`${prop} = ${val}`)
      root && lines.push('')

    } else if (isArray(val)) {
      lines.push(`${prop} = ${JSON.stringify(val)}`)
      root && lines.push('')

    } else if (typeof val === 'object') {
      lines.push(`[${prop}]`)
      lines.push(toTomlString040(val, false))
      root && lines.push('')
    }
  })

  return lines.join('\n')
}

/**
 * Create a object from TOML string.
 */
export function fromTomlString (tomlString) {
  try {
    return toml.parse(tomlString)
  } catch (e) {
    console.log('ERROR:', e)
    console.log('TOML:\n', tomlString)
    return null
  }
}

/**
 * Check the val is array.
 */
function isArray (val) {
  return val && 'length' in val
}

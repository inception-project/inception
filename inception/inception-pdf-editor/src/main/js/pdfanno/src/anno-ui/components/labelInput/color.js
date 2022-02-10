// labelInput/color.js
import * as db from './db'

/**
 * Colors for a picker.
 */
export const colors = [
    // Pick from https://www.materialui.co/colors.
    '#FFEB3B', // yellow
    '#FF5722', // orange
    '#795548', // brown
    '#F44336', // red
    '#E91E63', // pink
    '#9C27B0', // purple
    '#3F51B5', // blue
    '#4CAF50'  // green
]

const defaultColor = '#AAA'

let _colorChangeListener

export function setup (colorChangeListener) {
    _colorChangeListener = colorChangeListener
}

export function choice () {
    return colors[Math.floor(Math.random() * colors.length) % colors.length]
}

export function getPaletteColors () {
    return [
        colors.slice(0, Math.floor(colors.length / 2)),
        colors.slice(Math.floor(colors.length / 2), colors.length)
    ]
}

/**
* Find a color for the text in the type.
*/
export function find (type, text) {

    // Default color.
    let color = defaultColor

    const labelList = db.getLabelList()
    labelList[type].labels.forEach(item => {
        // old style.
        if (typeof item === 'string') {
            return
        }
        const [aText, aColor] = item
        if (text === aText) {
            color = aColor
        }
    })

    return color
}

export function notifyColorChanged ({ text, color, uuid, annoType }) {
    _colorChangeListener(...arguments)
}

/**
 * Get the color map.

    Example:
    ---
    {
        "span" : {
            "label1" : color1,
            "label2" : color2
        },
        "one-way" : {
            "label1" : color1,
            "label2" : color2
        },
        "two-way" : {
            "label1" : color1,
            "label2" : color2
        },
        "link-way" : {
            "label1" : color1,
            "label2" : color2
        },
        "default" : defaultColor
    }
 */
export function getColorMap () {
    const labelMap = db.getLabelList()
    Object.keys(labelMap).forEach(type => {
        labelMap[type].labels.forEach(item => {
            // old style.
            if (typeof item === 'string') {
                labelMap[type][item] = colors[0]
            } else {
                labelMap[type][item[0]] = item[1]
            }
        })
        delete labelMap[type].labels
    })
    labelMap.default = defaultColor
    return labelMap
}

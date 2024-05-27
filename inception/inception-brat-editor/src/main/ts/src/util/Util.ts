/*
 * ## INCEpTION ##
 * Licensed to the Technische Universität Darmstadt under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The Technische Universität Darmstadt
 * licenses this file to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * ## brat ##
 * Copyright (C) 2010-2012 The brat contributors, all rights reserved.
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of
 * this software and associated documentation files (the "Software"), to deal in
 * the Software without restriction, including without limitation the rights to
 * use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies
 * of the Software, and to permit persons to whom the Software is furnished to do
 * so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */
import { DiamAjax } from '@inception-project/inception-js-api'
import { Box } from '@svgdotjs/svg.js'
import { Dispatcher } from '../dispatcher/Dispatcher'
import { EntityTypeDto, RelationTypeDto } from '../protocol/Protocol'
import { Fragment } from '../visualizer/Fragment'
import { Visualizer } from '../visualizer/Visualizer'
import { VisualizerUI } from '../visualizer_ui/VisualizerUI'

export class Util {
  cmp (a: number, b: number) {
    return a < b ? -1 : a > b ? 1 : 0
  }

  cmpArrayOnFirstElement (a: unknown[], b: unknown[]) {
    const _a = a[0]
    const _b = b[0]
    return _a < _b ? -1 : _a > _b ? 1 : 0
  }

  realBBox (span: Fragment) : Box {
    const box = span.rect.bbox()
    const chunkTranslation = span.chunk.translation
    const rowTranslation = span.chunk.row.translation
    box.x += chunkTranslation.x + rowTranslation.x
    box.y += chunkTranslation.y + rowTranslation.y
    return box
  }

  escapeHTML (str: string | null) {
    // WEBANNO EXTENSION BEGIN - No issue - More robust escaping
    if (str === null) {
      return null
    }
    // WEBANNO EXTENSION END - No issue - More robust escaping

    return str.replace(/&/g, '&amp;').replace(/</g, '&lt;').replace(/>/g, '&gt;')
  }

  escapeHTMLandQuotes (str: string) {
    // WEBANNO EXTENSION BEGIN - No issue - More robust escaping
    if (str === null) {
      return null
    }
    // WEBANNO EXTENSION END - No issue - More robust escaping

    return str.replace(/&/g, '&amp;').replace(/</g, '&lt;').replace(/>/g, '&gt;').replace(/"/g, '&quot;')
  }

  escapeHTMLwithNewlines (str: string) {
    // WEBANNO EXTENSION BEGIN - No issue - More robust escaping
    if (str === null) {
      return null
    }
    // WEBANNO EXTENSION END - No issue - More robust escaping

    return str.replace(/&/g, '&amp;').replace(/</g, '&lt;').replace(/>/g, '&gt;').replace(/\n/g, '<br/>')
  }

  escapeQuotes (str: string) {
    // WEBANNO EXTENSION BEGIN - No issue - More robust escaping
    if (str === null) {
      return null
    }
    // WEBANNO EXTENSION END - No issue - More robust escaping

    // we only use double quotes for HTML attributes
    return str.replace(/"/g, '&quot;')
  }

  getSpanLabels (spanTypes: Record<string, EntityTypeDto>, spanType: string) : string[] {
    const type = spanTypes[spanType]
    return (type && type.labels) || []
  }

  spanDisplayForm (spanTypes: Record<string, EntityTypeDto>, spanType: string) : string {
    const labels = this.getSpanLabels(spanTypes, spanType)
    if (labels[0]) {
      return labels[0]
    }

    const sep = spanType.indexOf('_')
    if (sep >= 0) {
      return spanType.substring(sep + 1)
    }

    return spanType
  }

  getArcLabels (spanTypes: Record<string, EntityTypeDto>, spanType, arcType, relationTypesHash: Record<string, RelationTypeDto>) : string[] {
    const type = spanTypes[spanType]
    const arcTypes = (type && type.arcs) || []
    let arcDesc : RelationTypeDto | null = null
    // also consider matches without suffix number, if any
    let noNumArcType : string | undefined
    if (arcType) {
      const splitType = arcType.match(/^(.*?)(\d*)$/)
      noNumArcType = splitType[1]
    }

    for (const arcDescI of arcTypes) {
      if (arcDescI.type === arcType || arcDescI.type === noNumArcType) {
        arcDesc = arcDescI
        break
      }
    }

    // fall back to relation types for unconfigured or missing def
    if (!arcDesc && noNumArcType) {
      arcDesc = Object.assign({}, relationTypesHash[arcType] || relationTypesHash[noNumArcType])
    }
    // WEBANNO EXTENSION BEGIN - #709 - Optimize render data size for annotations without labels
    /*
          return arcDesc && arcDesc.labels || [];
    */
    return (arcDesc && arcDesc.labels.map(label => '(' + label + ')')) || []
    // WEBANNO EXTENSION END - #709 - Optimize render data size for annotations without labels
  }

  arcDisplayForm (spanTypes: Record<string, EntityTypeDto>, spanType: string, arcType: string, relationTypesHash) {
    const labels = this.getArcLabels(spanTypes, spanType, arcType, relationTypesHash)
    return labels[0] || arcType
  }

  // color name RGB list, converted from
  // http://www.w3schools.com/html/html_colornames.asp
  // with perl as
  //     perl -e 'print "var colors = {\n"; while(<>) { /(\S+)\s+\#([0-9a-z]{2})([0-9a-z]{2})([0-9a-z]{2})\s*/i or die "Failed to parse $_"; ($r,$g,$b)=(hex($2),hex($3),hex($4)); print "    '\''",lc($1),"'\'':\[$r,$g,$b\],\n" } print "};\n" '
  colors = {
    aliceblue: [240, 248, 255],
    antiquewhite: [250, 235, 215],
    aqua: [0, 255, 255],
    aquamarine: [127, 255, 212],
    azure: [240, 255, 255],
    beige: [245, 245, 220],
    bisque: [255, 228, 196],
    black: [0, 0, 0],
    blanchedalmond: [255, 235, 205],
    blue: [0, 0, 255],
    blueviolet: [138, 43, 226],
    brown: [165, 42, 42],
    burlywood: [222, 184, 135],
    cadetblue: [95, 158, 160],
    chartreuse: [127, 255, 0],
    chocolate: [210, 105, 30],
    coral: [255, 127, 80],
    cornflowerblue: [100, 149, 237],
    cornsilk: [255, 248, 220],
    crimson: [220, 20, 60],
    cyan: [0, 255, 255],
    darkblue: [0, 0, 139],
    darkcyan: [0, 139, 139],
    darkgoldenrod: [184, 134, 11],
    darkgray: [169, 169, 169],
    darkgrey: [169, 169, 169],
    darkgreen: [0, 100, 0],
    darkkhaki: [189, 183, 107],
    darkmagenta: [139, 0, 139],
    darkolivegreen: [85, 107, 47],
    darkorange: [255, 140, 0],
    darkorchid: [153, 50, 204],
    darkred: [139, 0, 0],
    darksalmon: [233, 150, 122],
    darkseagreen: [143, 188, 143],
    darkslateblue: [72, 61, 139],
    darkslategray: [47, 79, 79],
    darkslategrey: [47, 79, 79],
    darkturquoise: [0, 206, 209],
    darkviolet: [148, 0, 211],
    deeppink: [255, 20, 147],
    deepskyblue: [0, 191, 255],
    dimgray: [105, 105, 105],
    dimgrey: [105, 105, 105],
    dodgerblue: [30, 144, 255],
    firebrick: [178, 34, 34],
    floralwhite: [255, 250, 240],
    forestgreen: [34, 139, 34],
    fuchsia: [255, 0, 255],
    gainsboro: [220, 220, 220],
    ghostwhite: [248, 248, 255],
    gold: [255, 215, 0],
    goldenrod: [218, 165, 32],
    gray: [128, 128, 128],
    grey: [128, 128, 128],
    green: [0, 128, 0],
    greenyellow: [173, 255, 47],
    honeydew: [240, 255, 240],
    hotpink: [255, 105, 180],
    indianred: [205, 92, 92],
    indigo: [75, 0, 130],
    ivory: [255, 255, 240],
    khaki: [240, 230, 140],
    lavender: [230, 230, 250],
    lavenderblush: [255, 240, 245],
    lawngreen: [124, 252, 0],
    lemonchiffon: [255, 250, 205],
    lightblue: [173, 216, 230],
    lightcoral: [240, 128, 128],
    lightcyan: [224, 255, 255],
    lightgoldenrodyellow: [250, 250, 210],
    lightgray: [211, 211, 211],
    lightgrey: [211, 211, 211],
    lightgreen: [144, 238, 144],
    lightpink: [255, 182, 193],
    lightsalmon: [255, 160, 122],
    lightseagreen: [32, 178, 170],
    lightskyblue: [135, 206, 250],
    lightslategray: [119, 136, 153],
    lightslategrey: [119, 136, 153],
    lightsteelblue: [176, 196, 222],
    lightyellow: [255, 255, 224],
    lime: [0, 255, 0],
    limegreen: [50, 205, 50],
    linen: [250, 240, 230],
    magenta: [255, 0, 255],
    maroon: [128, 0, 0],
    mediumaquamarine: [102, 205, 170],
    mediumblue: [0, 0, 205],
    mediumorchid: [186, 85, 211],
    mediumpurple: [147, 112, 216],
    mediumseagreen: [60, 179, 113],
    mediumslateblue: [123, 104, 238],
    mediumspringgreen: [0, 250, 154],
    mediumturquoise: [72, 209, 204],
    mediumvioletred: [199, 21, 133],
    midnightblue: [25, 25, 112],
    mintcream: [245, 255, 250],
    mistyrose: [255, 228, 225],
    moccasin: [255, 228, 181],
    navajowhite: [255, 222, 173],
    navy: [0, 0, 128],
    oldlace: [253, 245, 230],
    olive: [128, 128, 0],
    olivedrab: [107, 142, 35],
    orange: [255, 165, 0],
    orangered: [255, 69, 0],
    orchid: [218, 112, 214],
    palegoldenrod: [238, 232, 170],
    palegreen: [152, 251, 152],
    paleturquoise: [175, 238, 238],
    palevioletred: [216, 112, 147],
    papayawhip: [255, 239, 213],
    peachpuff: [255, 218, 185],
    peru: [205, 133, 63],
    pink: [255, 192, 203],
    plum: [221, 160, 221],
    powderblue: [176, 224, 230],
    purple: [128, 0, 128],
    red: [255, 0, 0],
    rosybrown: [188, 143, 143],
    royalblue: [65, 105, 225],
    saddlebrown: [139, 69, 19],
    salmon: [250, 128, 114],
    sandybrown: [244, 164, 96],
    seagreen: [46, 139, 87],
    seashell: [255, 245, 238],
    sienna: [160, 82, 45],
    silver: [192, 192, 192],
    skyblue: [135, 206, 235],
    slateblue: [106, 90, 205],
    slategray: [112, 128, 144],
    slategrey: [112, 128, 144],
    snow: [255, 250, 250],
    springgreen: [0, 255, 127],
    steelblue: [70, 130, 180],
    tan: [210, 180, 140],
    teal: [0, 128, 128],
    thistle: [216, 191, 216],
    tomato: [255, 99, 71],
    turquoise: [64, 224, 208],
    violet: [238, 130, 238],
    wheat: [245, 222, 179],
    white: [255, 255, 255],
    whitesmoke: [245, 245, 245],
    yellow: [255, 255, 0],
    yellowgreen: [154, 205, 50]
  }

  // color parsing function originally from
  // http://plugins.jquery.com/files/jquery.color.js.txt
  // (with slight modifications)

  // Parse strings looking for color tuples [255,255,255]
  rgbNumRE = /rgb\(\s*([0-9]{1,3})\s*,\s*([0-9]{1,3})\s*,\s*([0-9]{1,3})\s*\)/
  rgbPercRE = /rgb\(\s*([0-9]+(?:\.[0-9]+)?)%\s*,\s*([0-9]+(?:\.[0-9]+)?)%\s*,\s*([0-9]+(?:\.[0-9]+)?)%\s*\)/
  rgbHash6RE = /#([a-fA-F0-9]{2})([a-fA-F0-9]{2})([a-fA-F0-9]{2})/
  rgbHash3RE = /#([a-fA-F0-9])([a-fA-F0-9])([a-fA-F0-9])/

  strToRgb (color: string) {
    let result: RegExpExecArray | null

    // Check if we're already dealing with an array of colors
    //         if ( color && color.constructor == Array && color.length == 3 )
    //             return color;

    // Look for rgb(num,num,num)
    result = this.rgbNumRE.exec(color)
    if (result) { return [parseInt(result[1]), parseInt(result[2]), parseInt(result[3])] }

    // Look for rgb(num%,num%,num%)
    result = this.rgbPercRE.exec(color)
    if (result) { return [parseFloat(result[1]) * 2.55, parseFloat(result[2]) * 2.55, parseFloat(result[3]) * 2.55] }

    // Look for #a0b1c2
    result = this.rgbHash6RE.exec(color)
    if (result) { return [parseInt(result[1], 16), parseInt(result[2], 16), parseInt(result[3], 16)] }

    // Look for #fff
    result = this.rgbHash3RE.exec(color)
    if (result) { return [parseInt(result[1] + result[1], 16), parseInt(result[2] + result[2], 16), parseInt(result[3] + result[3], 16)] }

    // Otherwise, we're most likely dealing with a named color
    return this.colors[(color && color.trim().toLowerCase()) || '']
  }

  rgbToStr (rgb : [number, number, number]) {
    // TODO: there has to be a better way, even in JS
    let r = Math.floor(rgb[0]).toString(16)
    let g = Math.floor(rgb[1]).toString(16)
    let b = Math.floor(rgb[2]).toString(16)
    // pad
    r = r.length < 2 ? '0' + r : r
    g = g.length < 2 ? '0' + g : g
    b = b.length < 2 ? '0' + b : b
    return ('#' + r + g + b)
  }

  // Functions rgbToHsl and hslToRgb originally from
  // http://mjijackson.com/2008/02/rgb-to-hsl-and-rgb-to-hsv-color-model-conversion-algorithms-in-javascript
  // implementation of functions in Wikipedia
  // (with slight modifications)

  // RGB to HSL color conversion
  rgbToHsl (rgb: [number, number, number]): [number, number, number] {
    const r = rgb[0] / 255; const g = rgb[1] / 255; const b = rgb[2] / 255
    const max = Math.max(r, g, b); const min = Math.min(r, g, b)
    const l = (max + min) / 2
    let h: number
    let s: number

    if (max === min) {
      h = s = 0 // achromatic
    } else {
      const d = max - min
      s = l > 0.5 ? d / (2 - max - min) : d / (max + min)
      switch (max) {
        case r: h = (g - b) / d + (g < b ? 6 : 0); break
        case g: h = (b - r) / d + 2; break
        default: h = (r - g) / d + 4; break
      }
      h /= 6
    }

    return [h, s, l]
  }

  hue2rgb (p: number, q: number, t: number) {
    if (t < 0) t += 1
    if (t > 1) t -= 1
    if (t < 1 / 6) return p + (q - p) * 6 * t
    if (t < 1 / 2) return q
    if (t < 2 / 3) return p + (q - p) * (2 / 3 - t) * 6
    return p
  }

  hslToRgb (hsl: [number, number, number]) : [number, number, number] {
    const h = hsl[0]; const s = hsl[1]; const l = hsl[2]

    let r, g, b

    if (s === 0) {
      r = g = b = l // achromatic
    } else {
      const q = l < 0.5 ? l * (1 + s) : l + s - l * s
      const p = 2 * l - q
      r = this.hue2rgb(p, q, h + 1 / 3)
      g = this.hue2rgb(p, q, h)
      b = this.hue2rgb(p, q, h - 1 / 3)
    }

    return [r * 255, g * 255, b * 255]
  }

  adjustLightnessCache = {}

  /**
   * Given color string and -1<=adjust<=1, returns color string where lightness (in the HSL sense)
   * is adjusted by the given amount, the larger the lighter: -1 gives black, 1 white, and 0
   * the given color.
   */
  adjustColorLightness (colorstr: string, adjust: number) {
    if (!(colorstr in this.adjustLightnessCache)) {
      this.adjustLightnessCache[colorstr] = {}
    }
    if (!(adjust in this.adjustLightnessCache[colorstr])) {
      const rgb = this.strToRgb(colorstr)
      if (rgb === undefined) {
        // failed color string conversion; just return the input
        this.adjustLightnessCache[colorstr][adjust] = colorstr
      } else {
        const hsl = this.rgbToHsl(rgb)
        if (adjust > 0.0) {
          hsl[2] = 1.0 - ((1.0 - hsl[2]) * (1.0 - adjust))
        } else {
          hsl[2] = (1.0 + adjust) * hsl[2]
        }
        const lightRgb = this.hslToRgb(hsl)
        this.adjustLightnessCache[colorstr][adjust] = this.rgbToStr(lightRgb)
      }
    }
    return this.adjustLightnessCache[colorstr][adjust]
  }

  keyValRE = /^([^=]+)=(.*)$/ // key=value
  isDigitsRE = /^[0-9]+$/

  profiles = {}
  profileStarts: Record<string, Date> = {}
  profileOn = false
  profileEnable (on) {
    if (on === undefined) on = true
    this.profileOn = on
  } // profileEnable

  profileClear () {
    if (!this.profileOn) return
    this.profiles = {}
    this.profileStarts = {}
  } // profileClear

  profileStart (label: string) {
    if (!this.profileOn) return
    this.profileStarts[label] = new Date()
  } // profileStart

  profileEnd (label: string) {
    if (!this.profileOn) return
    if (!this.profileStarts[label]) {
      console.log(`profileEnd(${label}) called without profileStart`)
      return
    }
    const profileElapsed = new Date().valueOf() - this.profileStarts[label].valueOf()
    if (!this.profiles[label]) this.profiles[label] = 0
    this.profiles[label] += profileElapsed
  } // profileEnd

  profileReport () {
    if (!this.profileOn) return
    if (window.console) {
      $.each(this.profiles, (label, time) => {
        console.log('profile ' + label, time)
      })
      console.log('-------')
    }
  } // profileReport

  // container: ID or jQuery element
  // collData: the collection data (in the format of the result of
  //   http://.../brat/ajax.cgi?action=getCollectionInformation&collection=...
  // docData: the document data (in the format of the result of
  //   http://.../brat/ajax.cgi?action=getDocument&collection=...&document=...
  // returns the embedded visualizer's dispatcher object
  embed (container: string, ajax: DiamAjax, collData, docData, callback?: Function) {
    const dispatcher = new Dispatcher()

    // eslint-disable-next-line no-new
    new Visualizer(dispatcher, container)
    // eslint-disable-next-line no-new
    new VisualizerUI(dispatcher, ajax)
    docData.collection = null
    if (callback) callback(dispatcher)
    dispatcher.post('collectionLoaded', [collData])
    dispatcher.post('requestRenderData', [docData])
    return dispatcher
  }

  // container: ID
  // collDataURL: the URL of the collection data, or collection data
  //   object (if pre-fetched)
  // docDataURL: the url of the document data (if pre-fetched, use
  //   simple `embed` instead)
  // callback: optional; the callback to call afterwards; it will be
  //   passed the embedded visualizer's dispatcher object
  embedByURL (container: string, ajax: DiamAjax, collDataURL: string, docDataURL: string, callback?: Function) {
    let collData, docData
    const handler = () => {
      if (collData && docData) {
        this.embed(container, ajax, collData, docData, callback)
      }
    }
    if (typeof (container) === 'string') {
      $.getJSON(collDataURL, (data) => { collData = data; handler() })
    } else {
      collData = collDataURL
    }
    $.getJSON(docDataURL, (data) => { docData = data; handler() })
  }
}

export const INSTANCE = new Util()

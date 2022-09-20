
export type ColorCode = string

// http://24ways.org/2010/calculating-color-contrast/
// http://stackoverflow.com/questions/11867545/change-text-color-based-on-brightness-of-the-covered-background-area
export function bgToFgColor (hexcolor: ColorCode) {
  const r = parseInt(hexcolor.substr(1, 2), 16)
  const g = parseInt(hexcolor.substr(3, 2), 16)
  const b = parseInt(hexcolor.substr(5, 2), 16)
  const yiq = ((r * 299) + (g * 587) + (b * 114)) / 1000
  return (yiq >= 128) ? '#000000' : '#ffffff'
}

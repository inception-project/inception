// TODO: Drop this Glyph class in favor of Meta and rename Meta to Glyph
type Glyph = [
  begin: number,
  page: number,
  type: string,
  char: string,
  meta: number[]
]

/**
 * Convert analyze results as page-based.
 */
interface Page {
  page: number;
  range: [number, number];
  glyphs: Glyph[];
}

class Meta {
  position: number
  pageNumber: number
  glyph: string
  x: number
  y: number
  w: number
  h: number

  static parse (info: Glyph): Meta {
    const [x, y, w, h] = info[4]
    const m = new Meta()
    m.position = info[0]
    m.pageNumber = info[1]
    m.glyph = info[3]
    m.x = x
    m.y = y
    m.w = w
    m.h = h
    return m
  }
}

/**
 * Interpret the meta data.
 */
function extractMeta (meta): Meta {
  return Meta.parse(meta)
}

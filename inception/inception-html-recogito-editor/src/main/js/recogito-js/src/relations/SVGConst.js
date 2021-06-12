// Rounded path corner radius
const BORDER_RADIUS = 3;

// Horizontal distance between connection line and annotation highlight
const LINE_DISTANCE = 6.5;

const SVGConst = {

  NAMESPACE : 'http://www.w3.org/2000/svg',

  // Rounded corner arc radius
  BORDER_RADIUS : BORDER_RADIUS,

  // Horizontal distance between connection line and annotation highlight
  LINE_DISTANCE : LINE_DISTANCE,

  // Possible rounded corner SVG arc configurations: clock position + clockwise/counterclockwise
  ARC_0CW : 'a' + BORDER_RADIUS + ',' + BORDER_RADIUS + ' 0 0 1 ' + BORDER_RADIUS + ',' + BORDER_RADIUS,
  ARC_0CC : 'a' + BORDER_RADIUS + ',' + BORDER_RADIUS + ' 0 0 0 -' + BORDER_RADIUS + ',' + BORDER_RADIUS,
  ARC_3CW : 'a' + BORDER_RADIUS + ',' + BORDER_RADIUS + ' 0 0 1 -' + BORDER_RADIUS + ',' + BORDER_RADIUS,
  ARC_3CC : 'a' + BORDER_RADIUS + ',' + BORDER_RADIUS + ' 0 0 0 -' + BORDER_RADIUS + ',-' + BORDER_RADIUS,
  ARC_6CW : 'a' + BORDER_RADIUS + ',' + BORDER_RADIUS + ' 0 0 1 -' + BORDER_RADIUS + ',-' + BORDER_RADIUS,
  ARC_6CC : 'a' + BORDER_RADIUS + ',' + BORDER_RADIUS + ' 0 0 0 ' + BORDER_RADIUS + ',-' + BORDER_RADIUS,
  ARC_9CW : 'a' + BORDER_RADIUS + ',' + BORDER_RADIUS + ' 0 0 1 ' + BORDER_RADIUS + ',-' + BORDER_RADIUS,
  ARC_9CC : 'a' + BORDER_RADIUS + ',' + BORDER_RADIUS + ' 0 0 0 ' + BORDER_RADIUS + ',' + BORDER_RADIUS

}

export default SVGConst;
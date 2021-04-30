import EventEmitter from 'tiny-emitter';
import Bounds from './Bounds';
import Handle from './Handle';
import CONST from './SVGConst';
import { getNodeById } from './RelationUtils'

/**
 * The connecting line between two annotation highlights.
 */
export default class Connection extends EventEmitter {

  constructor(contentEl, svgEl, nodeOrAnnotation) {
    super();

    this.svgEl = svgEl;

    // SVG elements
    this.path = document.createElementNS(CONST.NAMESPACE, 'path'),
    this.startDot = document.createElementNS(CONST.NAMESPACE, 'circle'),
    this.endDot = document.createElementNS(CONST.NAMESPACE, 'circle'),

    svgEl.appendChild(this.path);
    svgEl.appendChild(this.startDot);
    svgEl.appendChild(this.endDot);

    // Connections are initialized either from a relation annotation
    // (when loading), or as a 'floating' relation, attached to a start
    // node (when drawing a new one).
    const props = nodeOrAnnotation.type === 'Annotation' ?
     this.initFromAnnotation(contentEl, svgEl, nodeOrAnnotation) :
     this.initFromStartNode(svgEl, nodeOrAnnotation);

    this.annotation = props.annotation;

    // 'Descriptive' instance properties
    this.fromNode = props.fromNode;
    this.fromBounds = props.fromBounds;

    this.toNode = props.toNode;
    this.toBounds = props.toBounds;

    this.currentEnd = props.currentEnd;

    this.handle = props.handle;

    // A floating connection is not yet attached to an end node.
    this.floating = props.floating;

    this.redraw();
  }

  /** Initializes a fixed connection from an annotation **/
  initFromAnnotation = function(contentEl, svgEl, annotation) {
    const [ fromId, toId ] = annotation.target.map(t => t.id);
    const relation = annotation.bodies[0].value;

    const fromNode = getNodeById(contentEl, fromId);
    const fromBounds = new Bounds(fromNode.elements, svgEl);

    const toNode = getNodeById(contentEl, toId);
    const toBounds = new Bounds(toNode.elements, svgEl);

    const currentEnd = toNode;

    const handle = new Handle(relation, svgEl);

    // RelationsLayer uses click as a selection event
    handle.on('click', () => this.emit('click', {
      annotation,
      from: fromNode.annotation,
      to: toNode.annotation,
      midX: this.currentMidXY[0],
      midY: this.currentMidXY[1]
    }));

    return { annotation, fromNode, fromBounds, toNode, toBounds, currentEnd, handle, floating: false };
  }

  /** Initializes a floating connection from a start node **/
  initFromStartNode = function(svgEl, fromNode) {
    const fromBounds = new Bounds(fromNode.elements, svgEl);
    return { fromNode, fromBounds, floating: true };
  }

  /**
   * Fixes the end of the connection to the current end node,
   * turning a floating connection into a non-floating one.
   */
  unfloat = function() {
    if (this.currentEnd.elements)
      this.floating = false;
  }

  /** Moves the end of a (floating!) connection to the given [x,y] or node **/
  dragTo = function(xyOrNode) {
    if (this.floating) {
      this.currentEnd = xyOrNode;
      if (xyOrNode.elements) {
        this.toNode = xyOrNode;
        this.toBounds = new Bounds(xyOrNode.elements, this.svgEl);
      }
    }
  }

  destroy = () => {
    this.svgEl.removeChild(this.path);
    this.svgEl.removeChild(this.startDot);
    this.svgEl.removeChild(this.endDot);

    if (this.handle)
      this.handle.destroy();
  }

  /** Redraws this connection **/
  redraw = function() {
    if (this.currentEnd) {
      const end = this.endXY;

      const startsAtTop = end[1] <= (this.fromBounds.top + this.fromBounds.height / 2);
      const start = (startsAtTop) ?
        this.fromBounds.topHandleXY : this.fromBounds.bottomHandleXY;

      const deltaX = end[0] - start[0];
      const deltaY = end[1] - start[1];

      const half = (Math.abs(deltaX) + Math.abs(deltaY)) / 2; // Half of length, for middot pos computation
      const midX = (half > Math.abs(deltaX)) ? start[0] + deltaX : start[0] + half * Math.sign(deltaX);

      let midY; // computed later

      const orientation = (half > Math.abs(deltaX)) ?
        (deltaY > 0) ? 'down' : 'up' :
        (deltaX > 0) ? 'right' : 'left';

      const d = CONST.LINE_DISTANCE - CONST.BORDER_RADIUS; // Shorthand: vertical straight line length

      // Path that starts at the top edge of the annotation highlight
      const compileBottomPath = function() {
        const arc1 = (deltaX > 0) ? CONST.ARC_9CC : CONST.ARC_3CW;
        const arc2 = (deltaX > 0) ? CONST.ARC_0CW : CONST.ARC_0CC;

        midY = (half > Math.abs(deltaX)) ?
          start[1] + half - Math.abs(deltaX) + CONST.LINE_DISTANCE :
          start[1] + CONST.LINE_DISTANCE;

        return 'M' + start[0] +
                ' ' + start[1] +
                'v' + d +
                arc1 +
                'h' + (deltaX - 2 * Math.sign(deltaX) * CONST.BORDER_RADIUS) +
                arc2 +
                'V' + end[1];
      };

      // Path that starts at the bottom edge of the annotation highlight
      const compileTopPath = function() {
        const arc1 = (deltaX > 0) ? CONST.ARC_9CW : CONST.ARC_3CC;
        const arc2 = (deltaX > 0) ?
          (deltaY >= 0) ? CONST.ARC_0CW : CONST.ARC_6CC :
          (deltaY >= 0) ? CONST.ARC_0CC : CONST.ARC_6CW;

        midY = (half > Math.abs(deltaX)) ?
          start[1] - (half - Math.abs(deltaX)) - CONST.LINE_DISTANCE :
          start[1] - CONST.LINE_DISTANCE;

        return 'M' + start[0] +
                ' ' + start[1] +
                'v-' + (CONST.LINE_DISTANCE - CONST.BORDER_RADIUS) +
                arc1 +
                'h' + (deltaX - 2 * Math.sign(deltaX) * CONST.BORDER_RADIUS) +
                arc2 +
                'V' + end[1];
      };

      this.startDot.setAttribute('cx', start[0]);
      this.startDot.setAttribute('cy', start[1]);
      this.startDot.setAttribute('r', 2);
      this.startDot.setAttribute('class', 'start');

      this.endDot.setAttribute('cx', end[0]);
      this.endDot.setAttribute('cy', end[1]);
      this.endDot.setAttribute('r', 2);
      this.endDot.setAttribute('class', 'end');

      if (startsAtTop)
        this.path.setAttribute('d', compileTopPath());
      else
        this.path.setAttribute('d', compileBottomPath());

      this.path.setAttribute('class', 'connection');

      this.currentMidXY = [ midX, midY ];

      if (this.handle)
        this.handle.setPosition(this.currentMidXY, orientation);
    }
  }

  /**
   * Redraws this connection, and additionally forces a recompute of
   * the start and end coordinates. This is only needed if the position
   * of the annotation highlights changes, e.g. after a window resize.
   */
  recompute = () => {
    this.fromBounds.recompute();

    if (this.currentEnd.elements)
      this.toBounds.recompute();

    this.redraw();
  }

  /**
   * Returns true if the given relation matches this connection,
   * meaning that this connection has the same start and end point
   * as recorded in the relation.
   */
  matchesRelation = relation =>
    relation.from.isEqual(this.fromNode.annotation) &&
    relation.to.isEqual(this.toNode.annotation);

  /** Getter/setter shorthands **/

  get isFloating() {
    return this.floating;
  }

  get startAnnotation() {
    return this.fromNode.annotation;
  }

  get endAnnotation() {
    return this.toNode.annotation;
  }

  get endXY() {
    return (this.currentEnd instanceof Array) ?
      this.currentEnd :
        (this.fromBounds.top > this.toBounds.top) ?
          this.toBounds.bottomHandleXY : this.toBounds.topHandleXY;
  }

  get midXY() {
    return this.currentMidXY;
  }

}

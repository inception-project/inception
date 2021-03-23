import EventEmitter from 'tiny-emitter';
import Connection from '../Connection';
import HoverEmphasis from './HoverEmphasis';
import { getNodeForEvent } from '../RelationUtils';
import { WebAnnotation } from '@recogito/recogito-client-core';

/**
 * Wraps an event handler for event delegation. This way, we
 * can listen to events emitted by children matching the given
 * selector, rather than attaching (loads of!) handlers to each
 * child individually.
 */
const delegatingHandler = (selector, handler) => evt => {
  if (evt.target.matches(selector))
    handler(evt);
}

/**
 * The drawing tool for creating a new relation.
 */
export default class DrawingTool extends EventEmitter {

  constructor(contentEl, svgEl) {
    super();

    this.contentEl = contentEl;
    this.svgEl = svgEl;

    this.currentHover = null;
    this.currentConnection = null;
  }

  attachHandlers = () => {
    this.contentEl.classList.add('r6o-noselect');

    this.contentEl.addEventListener('mousedown', this.onMouseDown);
    this.contentEl.addEventListener('mousemove', this.onMouseMove);
    this.contentEl.addEventListener('mouseup', this.onMouseUp);

    this.contentEl.addEventListener('mouseover', this.onEnterAnnotation);
    this.contentEl.addEventListener('mouseout', this.onLeaveAnnotation);

    document.addEventListener('keydown', this.onKeyDown);
  }

  detachHandlers = () => {
    this.contentEl.classList.remove('r6o-noselect');

    this.contentEl.removeEventListener('mousedown', this.onMouseDown);
    this.contentEl.removeEventListener('mousemove', this.onMouseMove);
    this.contentEl.removeEventListener('mouseup', this.onMouseUp);

    this.contentEl.removeEventListener('mouseover', this.onEnterAnnotation);
    this.contentEl.removeEventListener('mouseleave', this.onLeaveAnnotation);

    document.removeEventListener('keydown', this.onKeyDown);
  }

  onMouseDown = evt => {
    const node = getNodeForEvent(evt);
    if (node) {
      if (this.currentConnection) {
        this.completeConnection(node);
      } else {
        this.startNewConnection(node);
      }
    }
  }

  onMouseMove = evt => {
    if (this.currentConnection && this.currentConnection.isFloating) {
      if (this.currentHover)  {
        this.currentConnection.dragTo(this.currentHover.node);
      } else {
        const { top, left } = this.contentEl.getBoundingClientRect();
        this.currentConnection.dragTo([ evt.pageX - left, evt.pageY - top ]);
      }
    }
  }

  /**
   * We want to support both possible drawing modes: click once for start
   * and once for end; or click and hold to start, drag to end and release.
   */
  onMouseUp = () => {
    if (this.currentHover && this.currentConnection && this.currentConnection.isFloating) {
      // If this is a different node than the start node, complete the connection
      if (this.currentHover.annotation !== this.currentConnection.startAnnotation) {
        this.completeConnection(this.currentHover.node);
      }
    }
  }

  onKeyDown = evt => {
    if (evt.which === 27) { // Escape
      this.reset();
      this.emit('cancelDrawing');
    }
  }

  /** Emphasise hovered annotation **/
  onEnterAnnotation = delegatingHandler('.r6o-annotation', evt => {
    if (this.currentHover)
      this.hover();

    this.hover(getNodeForEvent(evt).elements);
  });

  /** Clear hover emphasis **/
  onLeaveAnnotation = delegatingHandler('.r6o-annotation', evt => {
    this.hover();
  });

  /** Drawing code for hover emphasis */
  hover = elements => {
    if (elements) {
      this.currentHover = new HoverEmphasis(this.svgEl, elements);
    } else { // Clear hover
      if (this.currentHover)
        this.currentHover.destroy();

      this.currentHover = null;
    }
  }

  /** Start drawing a new connection line **/
  startNewConnection = fromNode => {
    this.currentConnection = new Connection(this.contentEl, this.svgEl, fromNode);
    this.contentEl.classList.add('r6o-drawing');
    this.render();
  }

  /** Complete drawing of a new connection **/
  completeConnection = function() {
    this.currentConnection.unfloat();

    this.contentEl.classList.remove('r6o-drawing');

    const from = this.currentConnection.startAnnotation;
    const to = this.currentConnection.endAnnotation;
    const [ midX, midY ] = this.currentConnection.midXY;

    const annotation = WebAnnotation.create({
      target: [
        { id: from.id },
        { id: to.id }
      ]
    });

    this.emit('createRelation', { annotation, from, to, midX, midY });
  }

  reset = () => {
    if (this.currentConnection) {
      this.currentConnection.destroy();
      this.currentConnection = null;
      this.contentEl.classList.remove('r6o-drawing');
    }
  }

  render = () => {
    if (this.currentConnection) {
      this.currentConnection.redraw();
      requestAnimationFrame(this.render);
    }
  }

  set enabled(enabled) {
    if (enabled) {
      this.attachHandlers();
    } else {
      this.detachHandlers();
      this.contentEl.classList.remove('r6o-drawing');

      if (this.currentConnection) {
        this.currentConnection.destroy();
        this.currentConnection = null;
      }
    }
  }

}

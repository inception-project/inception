import Connection from './Connection';
import DrawingTool from './drawing/DrawingTool';
import CONST from './SVGConst';
import EventEmitter from 'tiny-emitter';

import './RelationsLayer.scss';

export default class RelationsLayer extends EventEmitter {

  constructor(contentEl) {
    super();

    this.connections = [];

    this.contentEl = contentEl;

    this.svg = document.createElementNS(CONST.NAMESPACE, 'svg');
    this.svg.setAttribute('class', 'r6o-relations-layer');
    this.contentEl.appendChild(this.svg);

    this.drawingTool = new DrawingTool(contentEl, this.svg);

    // Forward events
    this.drawingTool.on('createRelation', relation => this.emit('createRelation', relation));
    this.drawingTool.on('cancelDrawing', () => this.emit('cancelDrawing'));

    // Redraw on window resize
    window.addEventListener('resize', () => requestAnimationFrame(() => {
      this.recomputeAll();
    }));
  }

  /** Shorthand **/
  createConnection = annotation => {
    const c = new Connection(this.contentEl, this.svg, annotation);

    // Forward click event as selection, unless we're read-only
    c.on('click', relation => this.emit('selectRelation', relation));

    return c
  }

  init = annotations => {
    // Filter annotations for 'relationship annotation' shape first
    this.connections = annotations.filter(annotation => {
      const allTargetsHashIDs = annotation.targets.every(t => t.id && t.id.startsWith('#'))
      return allTargetsHashIDs && annotation.motivation === 'linking';
    }).reduce((conns, annotation) => {
      try {
        const c = this.createConnection(annotation);
        return [ ...conns, c ];
      } catch (error) {
        console.log(error);
        console.log(`Error rendering relation for annotation ${annotation.id}`);
        return conns;
      }
    }, []);

    setTimeout(() => this.recomputeAll(), 1);
  }

  recomputeAll = () => {
    this.connections.forEach(conn => {
      conn.recompute();
    })
  }

  addOrUpdateRelation = (relation, maybePrevious) => {
    const previous = maybePrevious ?
      this.connections.find(c => c.matchesRelation(relation)) : null;

    if (previous) {
      // Replace existing
      this.connections = this.connections.map(connection => {
        if (connection == previous) {
          connection.destroy();
          return this.createConnection(relation.annotation);
        } else {
          return connection;
        }
      });
    } else {
      // Add new
      const c = this.createConnection(relation.annotation);
      this.connections.push(c);
    }
  }

  removeRelation = relation => {
    const toRemove = this.connections.find(c => c.matchesRelation(relation));

    if (toRemove) {
      this.connections = this.connections.filter(c => c !== toRemove);
      toRemove.destroy();
    }
  }

  /** Overrides the ID for an existing relation **/
  overrideRelationId = (originalId, forcedId) => {
    const conn = this.connections.find(c => c.annotation.id == originalId);
    const updatedAnnotation = conn.annotation.clone({ id : forcedId });
    conn.annotation = updatedAnnotation;
    return conn;
  }

  /** Overrides the given source or target annotation **/
  overrideTargetAnnotation = (originalAnnotation, forcedAnnotation) => {
    const affectedFrom = this.connections.filter(c => c.fromNode.annotation == originalAnnotation);
    affectedFrom.forEach(c => c.fromNode.annotation = forcedAnnotation);

    const affectedTo = this.connections.filter(c => c.toNode.annotation == originalAnnotation);
    affectedTo.forEach(c => c.toNode.annotation = forcedAnnotation);
  }

  getAllRelations = () => {
    return this.connections.map(c => c.annotation);
  }

  /**
   * Get the relations that have the given annotation as start
   * or end node.
   */
  getConnectionsFor = annotation => {
    return this.connections.filter(c =>
      c.startAnnotation.isEqual(annotation) || c.endAnnotation.isEqual(annotation));
  }

  destroyConnectionsFor = annotation => {
    const connections = this.getConnectionsFor(annotation);
    connections.forEach(c => c.destroy());
    this.connections = this.connections.filter(c => !connections.includes(c));
  }

  show = () =>
    this.svg.style.display = 'inital';

  hide = () => {
    this.drawingEnabled = false;
    this.svg.style.display = 'none';
  }

  startDrawing = () =>
    this.drawingTool.enabled = true;

  stopDrawing = () =>
    this.drawingTool.enabled = false;

  resetDrawing = () =>
    this.drawingTool.reset();

  get readOnly() {
    this.svg.classList.contains('readonly');
  }

  set readOnly(readOnly) {
    if (readOnly)
      this.svg.setAttribute('class', 'r6o-relations-layer readonly');
    else
      this.svg.setAttribute('class', 'r6o-relations-layer');
  }

}

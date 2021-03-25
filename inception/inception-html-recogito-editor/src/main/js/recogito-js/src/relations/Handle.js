import EventEmitter from 'tiny-emitter';
import CONST from './SVGConst';

const escapeHtml = unsafe => {
  return unsafe
    .replace(/&/g, '&amp;')
    .replace(/</g, '&lt;')
    .replace(/>/g, '&gt;')
    .replace(/"/g, '&quot;')
    .replace(/'/g, '&#039;');
}

export default class Handle extends EventEmitter {

  constructor(label, svg) {
    super();

    this.svg = svg;

    this.g = document.createElementNS(CONST.NAMESPACE, 'g');
    this.text  = document.createElementNS(CONST.NAMESPACE, 'text');
    this.rect  = document.createElementNS(CONST.NAMESPACE, 'rect');
    this.arrow = document.createElementNS(CONST.NAMESPACE, 'path');

    // Append first and init afterwards, so we can query text width/height
    this.g.appendChild(this.rect);
    this.g.appendChild(this.text);
    this.g.appendChild(this.arrow);
    this.svg.appendChild(this.g);
    
    this.g.setAttribute('class', 'handle');

    this.text.innerHTML = escapeHtml(label);

    this.bounds = this.text.getBBox();

    this.text.setAttribute('dy', 2);
    this.text.setAttribute('dx', - Math.round(this.bounds.width / 2));

    this.rect.setAttribute('rx', 2); // Rounded corners
    this.rect.setAttribute('ry', 2);
    this.rect.setAttribute('width', Math.round(this.bounds.width) + 5);
    this.rect.setAttribute('height',  Math.round(this.bounds.height));

    this.arrow.setAttribute('class', 'r6o-arrow');    

    this.rect.addEventListener('click', () => this.emit('click'));
  }

  setPosition = (xy, orientation) => {
    const x = Math.round(xy[0]) - 0.5;
    const y = Math.round(xy[1]);

    const dx = Math.round(this.bounds.width / 2);

    const createArrow = function() {
      if (orientation === 'left')
        return 'M' + (xy[0] - dx - 8) + ',' + (xy[1] - 4) + 'l-7,4l7,4';
      else if (orientation === 'right')
        return 'M' + (xy[0] + dx + 8) + ',' + (xy[1] - 4) + 'l7,4l-7,4';
      else if (orientation === 'down')
        return 'M' + (xy[0] - 4) + ',' + (xy[1] + 12) + 'l4,7l4,-7';
      else
        return 'M' + (xy[0] - 4) + ',' + (xy[1] - 12) + 'l4,-7l4,7';
    };

    this.rect.setAttribute('x', x - 3 - dx);
    this.rect.setAttribute('y', y - 6.5);

    this.text.setAttribute('x', x);
    this.text.setAttribute('y', y);

    this.arrow.setAttribute('d', createArrow());
  }

  destroy = () =>
    this.svg.removeChild(this.g);

};
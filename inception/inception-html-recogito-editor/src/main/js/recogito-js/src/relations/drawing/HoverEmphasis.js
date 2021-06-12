import Bounds from '../Bounds';
import CONST from '../SVGConst';

export default class HoverEmphasis {
  
  constructor(svgEl, elements) {
    this.annotation = elements[0].annotation;
    this.node = { annotation: this.annotation, elements };
    
    this.outlines = document.createElementNS(CONST.NAMESPACE, 'g');

    const bounds = new Bounds(elements, svgEl);

    bounds.rects.forEach(r => {
      const rect = document.createElementNS(CONST.NAMESPACE, 'rect');
      
      rect.setAttribute('x', r.left - 0.5);
      rect.setAttribute('y', r.top - 0.5);
      rect.setAttribute('width', r.width + 1);
      rect.setAttribute('height', r.height);
      rect.setAttribute('class', 'hover');

      this.outlines.appendChild(rect);
    });

    svgEl.appendChild(this.outlines);

    this.svgEl = svgEl;
  }

  destroy = () => {
    this.svgEl.removeChild(this.outlines);
  }

}
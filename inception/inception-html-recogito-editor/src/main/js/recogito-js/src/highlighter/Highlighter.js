const TEXT = 3; // HTML DOM node type for text nodes

const RENDER_BATCH_SIZE = 100; // Number of annotations to render in one frame

const uniqueItems = items => Array.from(new Set(items))

export default class Highlighter {

  constructor(element, formatter) {
    this.el = element;
    this.formatter = formatter;
  }

  init = annotations => new Promise((resolve, _) => {    
    const startTime = performance.now();

    // Discard all annotations without a TextPositionSelector
    const highlights = annotations.filter(a => a.selector('TextPositionSelector'));

    // Sorting bottom to top significantly speeds things up,
    // because walkTextNodes will have a lot less to walk 
    highlights.sort((a, b) => b.start - a.start);

    // Render loop
    const render = annotations => {
      const batch = annotations.slice(0, RENDER_BATCH_SIZE);
      const remainder = annotations.slice(RENDER_BATCH_SIZE);

      requestAnimationFrame(() => {
        batch.forEach(this._addAnnotation);
        if (remainder.length > 0) {
          render(remainder);
        } else {
          console.log(`Rendered ${highlights.length}, took ${performance.now() - startTime}ms`);
          resolve();
        }
      });
    }

    render(highlights);
  })

  _addAnnotation = annotation => {
    try {
      const [ domStart, domEnd ] = this.charOffsetsToDOMPosition([ annotation.start, annotation.end ]);

      const range = document.createRange();
      range.setStart(domStart.node, domStart.offset);
      range.setEnd(domEnd.node, domEnd.offset);

      const spans = this.wrapRange(range);
      this.applyStyles(annotation, spans);
      this.bindAnnotation(annotation, spans);
    } catch (error) {
      console.warn('Could not render annotation')
      console.warn(error);
      console.warn(annotation.underlying);
    }
  }

  _findAnnotationSpans = annotation => {
    const allAnnotationSpans = document.querySelectorAll('.r6o-annotation');
    return Array.prototype.slice.call(allAnnotationSpans)
      .filter(span => span.annotation.isEqual(annotation));
  }

  getAllAnnotations = () => {
    const allAnnotationSpans = document.querySelectorAll('.r6o-annotation');
    const allAnnotations = Array.from(allAnnotationSpans).map(span => span.annotation);
    return [...new Set(allAnnotations)];
  }

  addOrUpdateAnnotation = (annotation, maybePrevious) => {
    // TODO index annotation to make this faster
    const annoSpans = this._findAnnotationSpans(annotation);
    const prevSpans = maybePrevious ? this._findAnnotationSpans(maybePrevious) : [];
    const spans = uniqueItems(annoSpans.concat(prevSpans));

    if (spans.length > 0) {
      // naive approach
      this._unwrapHighlightings(spans);
      this.el.normalize();
      this._addAnnotation(annotation);
    } else {
      this._addAnnotation(annotation);
    }
  }

  removeAnnotation = annotation => {
    const spans = this._findAnnotationSpans(annotation);
    if (spans) {
      this._unwrapHighlightings(spans)
      this.el.normalize();
    }
  }

  /** 
   * Forces a new ID on the annotation with the given ID. This method handles 
   * the ID update within the Highlighter ONLY. It's up to the application to 
   * keep the RelationsLayer in sync! 
   * 
   * @returns the updated annotation for convenience
   */
  overrideId = (originalId, forcedId) => {    
    const allSpans = document.querySelectorAll(`.r6o-annotation[data-id="${originalId}"]`);
    const annotation = allSpans[0].annotation; 

    const updatedAnnotation = annotation.clone({ id : forcedId });
    this.bindAnnotation(updatedAnnotation, allSpans);

    return updatedAnnotation;
  }

  _unwrapHighlightings(highlightSpans) {
    for (const span of highlightSpans) {
      const parent = span.parentNode;
      parent.insertBefore(document.createTextNode(span.textContent), span);
      parent.removeChild(span);
    }
  }

  applyStyles = (annotation, spans) => {
    const extraClasses = this.formatter ? this.formatter(annotation) : '';
    spans.forEach(span => span.className = `r6o-annotation ${extraClasses}`.trim());
  }

  bindAnnotation = (annotation, elements) => {
    elements.forEach(el => {
      el.annotation = annotation;
      el.dataset.id = annotation.id;
    });
  }

  walkTextNodes = (node, stopOffset, nodeArray) => {
    const nodes = (nodeArray) ? nodeArray : [];

    const offset = (function() {
          var runningOffset = 0;
          nodes.forEach(function(node) {
            runningOffset += node.textContent.length;;
          });
          return runningOffset;
        })();

    let keepWalking = true;

    if (offset > stopOffset)
      return false;

    if (node.nodeType === TEXT)
      nodes.push(node);

    node = node.firstChild;

    while(node && keepWalking) {
      keepWalking = this.walkTextNodes(node, stopOffset, nodes);
      node = node.nextSibling;
    }

    return nodes;
  }

  charOffsetsToDOMPosition = charOffsets => {
    const maxOffset = Math.max.apply(null, charOffsets);

    const textNodeProps = (() => {
      let start = 0;
      return this.walkTextNodes(this.el, maxOffset).map(function(node) {
        var nodeLength = node.textContent.length,
            nodeProps = { node: node, start: start, end: start + nodeLength };

        start += nodeLength;
        return nodeProps;
      });
    })();

    return this.calculateDomPositionWithin(textNodeProps, charOffsets);
  }

  /**
   * Given a rootNode, this helper gets all text between a given
   * start- and end-node. Basically combines walkTextNodes (above)
   * with a hand-coded dropWhile & takeWhile.
   */
  textNodesBetween = (startNode, endNode, rootNode) => {
    // To improve performance, don't walk the DOM longer than necessary
    var stopOffset = (function() {
          var rangeToEnd = document.createRange();
          rangeToEnd.setStart(rootNode, 0);
          rangeToEnd.setEnd(endNode, endNode.textContent.length);
          return rangeToEnd.toString().length;
        })(),

        allTextNodes = this.walkTextNodes(rootNode, stopOffset),

        nodesBetween = [],
        len = allTextNodes.length,
        take = false,
        n, i;

    for (i=0; i<len; i++) {
      n = allTextNodes[i];

      if (n === endNode) take = false;

      if (take) nodesBetween.push(n);

      if (n === startNode) take = true;
    }

    return nodesBetween;
  }

  calculateDomPositionWithin = (textNodeProperties, charOffsets) => {
    var positions = [];

    textNodeProperties.forEach(function(props, i) {
      charOffsets.forEach(function(charOffset, j)  {
        if (charOffset >= props.start && charOffset <= props.end) {
          // Don't attach nodes for the same charOffset twice
          var previousOffset = (positions.length > 0) ?
                positions[positions.length - 1].charOffset : false;

          if (previousOffset !== charOffset)
            positions.push({
              charOffset: charOffset,
              node: props.node,
              offset: charOffset - props.start
            });
        }
      });

      // Break (i.e. return false) if all positions are computed
      return positions.length < charOffsets.length;
    });

    return positions;
  }

  wrapRange = (range, commonRoot) => {
    const root = commonRoot ? commonRoot : this.el;

    const surround = range => {
      var wrapper = document.createElement('SPAN');
      range.surroundContents(wrapper);
      return wrapper;
    };

    if (range.startContainer === range.endContainer) {
      return [ surround(range) ];
    } else {
      // The tricky part - we need to break the range apart and create
      // sub-ranges for each segment
      var nodesBetween =
        this.textNodesBetween(range.startContainer, range.endContainer, root);

      // Start with start and end nodes
      var startRange = document.createRange();
      startRange.selectNodeContents(range.startContainer);
      startRange.setStart(range.startContainer, range.startOffset);
      var startWrapper = surround(startRange);

      var endRange = document.createRange();
      endRange.selectNode(range.endContainer);
      endRange.setEnd(range.endContainer, range.endOffset);
      var endWrapper = surround(endRange);

      // And wrap nodes in between, if any
      var centerWrappers = nodesBetween.reverse().map(function(node) {
        const wrapper = document.createElement('SPAN');
        node.parentNode.insertBefore(wrapper, node);
        wrapper.appendChild(node);
        return wrapper;
      });

      return [ startWrapper ].concat(centerWrappers,  [ endWrapper ]);
    }
  }

  getAnnotationsAt = element => {
    // Helper to get all annotations in case of multipe nested annotation spans
    var getAnnotationsRecursive = function(element, a) {
          var annotations = (a) ? a : [ ],
              parent = element.parentNode;

          annotations.push(element.annotation);

          return (parent.classList.contains('r6o-annotation')) ?
            getAnnotationsRecursive(parent, annotations) : annotations;
        },

        sortByRangeLength = function(annotations) {
          return annotations.sort(function(a, b) {
            return (a.end - a.start) - (b.end - b.start);
          });
        };

    return sortByRangeLength(getAnnotationsRecursive(element));
  }

}

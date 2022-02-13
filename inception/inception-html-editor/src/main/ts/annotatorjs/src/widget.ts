import $ from 'jquery';
import { Delegator } from './class';
import * as Util from './util';

// Public: Base class for the Editor and Viewer elements. Contains methods that
// are shared between the two.
export class Widget extends Delegator {
  classes: {};
  element: any;
  static initClass() {
    // Classes used to alter the widgets state.
    this.prototype.classes = {
      hide: 'annotator-hide',
      invert: {
        x: 'annotator-invert-x',
        y: 'annotator-invert-y'
      }
    };
  }

  // Public: Creates a new Widget instance.
  //
  // element - The Element that represents the widget in the DOM.
  // options - An Object literal of options.
  //
  // Examples
  //
  //   element = document.createElement('div')
  //   widget  = new Annotator.Widget(element)
  //
  // Returns a new Widget instance.
  constructor(element: any, options: any) {
    super(element, options);
    this.classes = $.extend({}, Widget.prototype.classes, this.classes);
  }

  // Public: Unbind the widget's events and remove its element from the DOM.
  //
  // Returns nothing.
  destroy() {
    this.removeEvents();
    return this.element.remove();
  }

  checkOrientation() {
    this.resetOrientation();

    const window = $(Util.getGlobal());
    const widget = this.element.children(":first");
    const offset = widget.offset();
    const viewport = {
      top: window.scrollTop(),
      right: window.width() + window.scrollLeft()
    };
    const current = {
      top: offset.top,
      right: offset.left + widget.width()
    };

    if ((current.top - viewport.top) < 0) {
      this.invertY();
    }

    if ((current.right - viewport.right) > 0) {
      this.invertX();
    }

    return this;
  }

  // Public: Resets orientation of widget on the X & Y axis.
  //
  // Examples
  //
  //   widget.resetOrientation() # Widget is original way up.
  //
  // Returns itself for chaining.
  resetOrientation() {
    this.element.removeClass(this.classes.invert.x).removeClass(this.classes.invert.y);
    return this;
  }

  // Public: Inverts the widget on the X axis.
  //
  // Examples
  //
  //   widget.invertX() # Widget is now right aligned.
  //
  // Returns itself for chaining.
  invertX() {
    this.element.addClass(this.classes.invert.x);
    return this;
  }

  // Public: Inverts the widget on the Y axis.
  //
  // Examples
  //
  //   widget.invertY() # Widget is now upside down.
  //
  // Returns itself for chaining.
  invertY() {
    this.element.addClass(this.classes.invert.y);
    return this;
  }

  // Public: Find out whether or not the widget is currently upside down
  //
  // Returns a boolean: true if the widget is upside down
  isInvertedY() {
    return this.element.hasClass(this.classes.invert.y);
  }

  // Public: Find out whether or not the widget is currently right aligned
  //
  // Returns a boolean: true if the widget is right aligned
  isInvertedX() {
    return this.element.hasClass(this.classes.invert.x);
  }
}
Widget.initClass();


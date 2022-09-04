/* 
 * Licensed to the Technische Universität Darmstadt under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The Technische Universität Darmstadt 
 * licenses this file to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.
 *  
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * Derived under the conditions of the MIT license from below:
 *
 * Based on Annotator v.1.2.10 (built at: 26 Feb 2015)
 * http://annotatorjs.org
 *
 * Copyright 2015, the Annotator project contributors.
 * Dual licensed under the MIT and GPLv3 licenses.
 * https://github.com/openannotation/annotator/blob/master/LICENSE
 */

import $ from 'jquery';
import { Widget } from './widget';
import * as Util from './util';
import { _t } from './util';

// Public: Creates an element for editing annotations.
export class Editor extends Widget {
  events: { "form submit": string; ".annotator-save click": string; ".annotator-cancel click": string; ".annotator-cancel mouseover": string; "textarea keydown": string; };
  classes: { hide: string; focus: string; };
  html: string;
  options: {};
  fields: {};
  annotation: {};
  element: any;
  static initClass() {

    // Events to be bound to @element.
    this.prototype.events = {
      "form submit": "submit",
      ".annotator-save click": "submit",
      ".annotator-cancel click": "hide",
      ".annotator-cancel mouseover": "onCancelButtonMouseover",
      "textarea keydown": "processKeypress"
    };

    // Classes to toggle state.
    this.prototype.classes = {
      hide: 'annotator-hide',
      focus: 'annotator-focus'
    };

    // HTML template for @element.
    this.prototype.html = `\
<div class="annotator-outer annotator-editor">
  <form class="annotator-widget">
    <ul class="annotator-listing"></ul>
    <div class="annotator-controls">
      <a href="#cancel" class="annotator-cancel">` + _t('Cancel') + `</a>
<a href="#save" class="annotator-save annotator-focus">` + _t('Save') + `</a>
    </div>
  </form>
</div>\
`;

    this.prototype.options = {};
    // Configuration options
  }

  // Public: Creates an instance of the Editor object. This will create the
  // @element from the @html string and set up all events.
  //
  // options - An Object literal containing options. There are currently no
  //           options implemented.
  //
  // Examples
  //
  //   # Creates a new editor, adds a custom field and
  //   # loads an annotation for editing.
  //   editor = new Annotator.Editor
  //   editor.addField({
  //     label: 'My custom input field',
  //     type:  'textarea'
  //     load:  someLoadCallback
  //     save:  someSaveCallback
  //   })
  //   editor.load(annotation)
  //
  // Returns a new Editor instance.
  constructor(options?: any) {
    super(Editor.prototype.html, options);
    this.show = this.show.bind(this);
    this.hide = this.hide.bind(this);
    this.load = this.load.bind(this);
    this.submit = this.submit.bind(this);
    this.processKeypress = this.processKeypress.bind(this);
    this.onCancelButtonMouseover = this.onCancelButtonMouseover.bind(this);

    this.fields = [];
    this.annotation = {};
  }

  // Public: Displays the Editor and fires a "show" event.
  // Can be used as an event callback and will call Event#preventDefault()
  // on the supplied event.
  //
  // event - Event object provided if method is called by event
  //         listener (default:undefined)
  //
  // Examples
  //
  //   # Displays the editor.
  //   editor.show()
  //
  //   # Displays the editor on click (prevents default action).
  //   $('a.show-editor').bind('click', editor.show)
  //
  // Returns itself.
  show(event?: undefined) {
    Util.preventEventDefault(event);

    this.element.removeClass(this.classes.hide);
    this.element.find('.annotator-save').addClass(this.classes.focus);

    // invert if necessary
    this.checkOrientation();

    // give main textarea focus
    this.element.find(":input:first").focus();

    this.setupDraggables();

    return this.publish('show');
  }

  // Public: Hides the Editor and fires a "hide" event. Can be used as an event
  // callback and will call Event#preventDefault() on the supplied event.
  //
  // event - Event object provided if method is called by event
  //         listener (default:undefined)
  //
  // Examples
  //
  //   # Hides the editor.
  //   editor.hide()
  //
  //   # Hide the editor on click (prevents default action).
  //   $('a.hide-editor').bind('click', editor.hide)
  //
  // Returns itself.
  hide(event?: undefined) : Editor {
    Util.preventEventDefault(event);

    this.element.addClass(this.classes.hide);
    return this.publish('hide') as Editor;
  }

  // Public: Loads an annotation into the Editor and displays it setting
  // Editor#annotation to the provided annotation. It fires the "load" event
  // providing the current annotation subscribers can modify the annotation
  // before it updates the editor fields.
  //
  // annotation - An annotation Object to display for editing.
  //
  // Examples
  //
  //   # Displays the editor with the annotation loaded.
  //   editor.load({text: 'My Annotation'})
  //
  //   editor.on('load', (annotation) ->
  //     console.log annotation.text
  //   ).load({text: 'My Annotation'})
  //   # => Outputs "My Annotation"
  //
  // Returns itself.
  load(annotation: any) {
    this.annotation = annotation;

    this.publish('load', [this.annotation]);

    for (let field of Array.from(this.fields)) {
      field.load(field.element, this.annotation);
    }

    return this.show();
  }

  // Public: Hides the Editor and passes the annotation to all registered fields
  // so they can update its state. It then fires the "save" event so that other
  // parties can further modify the annotation.
  // Can be used as an event callback and will call Event#preventDefault() on the
  // supplied event.
  //
  // event - Event object provided if method is called by event
  //         listener (default:undefined)
  //
  // Examples
  //
  //   # Submits the editor.
  //   editor.submit()
  //
  //   # Submits the editor on click (prevents default action).
  //   $('button.submit-editor').bind('click', editor.submit)
  //
  //   # Appends "Comment: " to the annotation comment text.
  //   editor.on('save', (annotation) ->
  //     annotation.text = "Comment: " + annotation.text
  //   ).submit()
  //
  // Returns itself.
  submit(event?: undefined) {
    Util.preventEventDefault(event);

    for (let field of Array.from(this.fields)) {
      field.submit(field.element, this.annotation);
    }

    this.publish('save', [this.annotation]);

    return this.hide();
  }

  // Public: Adds an additional form field to the editor. Callbacks can be provided
  // to update the view and anotations on load and submission.
  //
  // options - An options Object. Options are as follows:
  //           id     - A unique id for the form element will also be set as the
  //                    "for" attribute of a label if there is one. Defaults to
  //                    a timestamp. (default: "annotator-field-{timestamp}")
  //           type   - Input type String. One of "input", "textarea",
  //                    "checkbox", "select" (default: "input")
  //           label  - Label to display either in a label Element or as place-
  //                    holder text depending on the type. (default: "")
  //           load   - Callback Function called when the editor is loaded with a
  //                    new annotation. Receives the field <li> element and the
  //                    annotation to be loaded.
  //           submit - Callback Function called when the editor is submitted.
  //                    Receives the field <li> element and the annotation to be
  //                    updated.
  //
  // Examples
  //
  //   # Add a new input element.
  //   editor.addField({
  //     label: "Tags",
  //
  //     # This is called when the editor is loaded use it to update your input.
  //     load: (field, annotation) ->
  //       # Do something with the annotation.
  //       value = getTagString(annotation.tags)
  //       $(field).find('input').val(value)
  //
  //     # This is called when the editor is submitted use it to retrieve data
  //     # from your input and save it to the annotation.
  //     submit: (field, annotation) ->
  //       value = $(field).find('input').val()
  //       annotation.tags = getTagsFromString(value)
  //   })
  //
  //   # Add a new checkbox element.
  //   editor.addField({
  //     type: 'checkbox',
  //     id: 'annotator-field-my-checkbox',
  //     label: 'Allow anyone to see this annotation',
  //     load: (field, annotation) ->
  //       # Check what state of input should be.
  //       if checked
  //         $(field).find('input').attr('checked', 'checked')
  //       else
  //         $(field).find('input').removeAttr('checked')

  //     submit: (field, annotation) ->
  //       checked = $(field).find('input').is(':checked')
  //       # Do something.
  //   })
  //
  // Returns the created <li> Element.
  addField(options: any) {
    const field = $.extend({
      id: 'annotator-field-' + Util.uuid(),
      type: 'input',
      label: '',
      load() { },
      submit() { }
    }, options);

    let input = null;
    const element = $('<li class="annotator-item" />');
    field.element = element[0];

    switch (field.type) {
      case 'textarea': input = $('<textarea />'); break;
      case 'input': case 'checkbox': input = $('<input />'); break;
      case 'select': input = $('<select />'); break;
    }

    element.append(input);

    input.attr({
      id: field.id,
      placeholder: field.label
    });

    if (field.type === 'checkbox') {
      input[0].type = 'checkbox';
      element.addClass('annotator-checkbox');
      element.append($('<label />', { for: field.id, html: field.label }));
    }

    this.element.find('ul:first').append(element);

    this.fields.push(field);

    return field.element;
  }

  checkOrientation() {
    super.checkOrientation().checkOrientation();

    const list = this.element.find('ul');
    const controls = this.element.find('.annotator-controls');

    if (this.element.hasClass(this.classes.invert.y)) {
      controls.insertBefore(list);
    } else if (controls.is(':first-child')) {
      controls.insertAfter(list);
    }

    return this;
  }

  // Event callback. Listens for the following special keypresses.
  // - escape: Hides the editor
  // - enter:  Submits the editor
  //
  // event - A keydown Event object.
  //
  // Returns nothing
  processKeypress(event: { keyCode: number; shiftKey: any; }) {
    if (event.keyCode === 27) { // "Escape" key => abort.
      return this.hide();
    } else if ((event.keyCode === 13) && !event.shiftKey) {
      // If "return" was pressed without the shift key, we're done.
      return this.submit();
    }
  }

  // Event callback. Removes the focus class from the submit button when the
  // cancel button is hovered.
  //
  // Returns nothing
  onCancelButtonMouseover() {
    return this.element.find('.' + this.classes.focus).removeClass(this.classes.focus);
  }

  // Sets up mouse events for resizing and dragging the editor window.
  // window events are bound only when needed and throttled to only update
  // the positions at most 60 times a second.
  //
  // Returns nothing.
  setupDraggables() {
    let cornerItem: any;
    this.element.find('.annotator-resize').remove();

    // Find the first/last item element depending on orientation
    if (this.element.hasClass(this.classes.invert.y)) {
      cornerItem = this.element.find('.annotator-item:last');
    } else {
      cornerItem = this.element.find('.annotator-item:first');
    }

    if (cornerItem) {
      $('<span class="annotator-resize"></span>').appendTo(cornerItem);
    }

    let mousedown = null;
    const {
      classes
    } = this;
    const editor = this.element;
    let textarea = null;
    const resize = editor.find('.annotator-resize');
    const controls = editor.find('.annotator-controls');
    let throttle = false;

    const onMousedown = function (event: { target: any; pageY: any; pageX: any; preventDefault: () => any; }) {
      if (event.target === this) {
        mousedown = {
          element: this,
          top: event.pageY,
          left: event.pageX
        };

        // Find the first text area if there is one.
        textarea = editor.find('textarea:first');

        $(window).bind({
          'mouseup.annotator-editor-resize': onMouseup,
          'mousemove.annotator-editor-resize': onMousemove
        });
        return event.preventDefault();
      }
    };

    var onMouseup = function () {
      mousedown = null;
      return $(window).unbind('.annotator-editor-resize');
    };

    var onMousemove = (event: { pageY: number; pageX: number; }) => {
      if (mousedown && (throttle === false)) {
        const diff = {
          top: event.pageY - mousedown.top,
          left: event.pageX - mousedown.left
        };

        if (mousedown.element === resize[0]) {
          const height = textarea.height();
          const width = textarea.width();

          const directionX = editor.hasClass(classes.invert.x) ? -1 : 1;
          const directionY = editor.hasClass(classes.invert.y) ? 1 : -1;

          textarea.height(height + (diff.top * directionY));
          textarea.width(width + (diff.left * directionX));

          // Only update the mousedown object if the dimensions
          // have changed, otherwise they have reached their minimum
          // values.
          if (textarea.height() !== height) { mousedown.top = event.pageY; }
          if (textarea.width() !== width) { mousedown.left = event.pageX; }

        } else if (mousedown.element === controls[0]) {
          editor.css({
            top: parseInt(editor.css('top'), 10) + diff.top,
            left: parseInt(editor.css('left'), 10) + diff.left
          });

          mousedown.top = event.pageY;
          mousedown.left = event.pageX;
        }

        throttle = true;
        return setTimeout(() => throttle = false
          , 1000 / 60);
      }
    };

    resize.bind('mousedown', onMousedown);
    return controls.bind('mousedown', onMousedown);
  }
}

Editor.initClass();

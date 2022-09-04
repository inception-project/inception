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

// Public: Creates an element for viewing annotations.
export class Viewer extends Widget {
  // User customisable options available.
  static readonly DEFAULT_OPTIONS = { readOnly: false };

  // HTML templates for @element and @item properties.
  static html = {
    element: `\
      <div class="annotator-outer annotator-viewer">
      <ul class="annotator-widget annotator-listing"></ul>
      </div>`,
    item: `\
      <li class="annotator-annotation annotator-item">
      <span class="annotator-controls">
        <a href="#" title="View as webpage" class="annotator-link">View as webpage</a>
        <button title="Edit" class="annotator-edit">Edit</button>
        <button title="Delete" class="annotator-delete">Delete</button>
      </span>
      </li>`
  };

  item: any;
  fields: {};
  annotations: {};
  element: any;

  // Public: Creates an instance of the Viewer object. This will create the
  // @element from the @html.element string and set up all events.
  //
  // options - An Object literal containing options.
  //
  // Examples
  //
  //   # Creates a new viewer, adds a custom field and displays an annotation.
  //   viewer = new Annotator.Viewer()
  //   viewer.addField({
  //     load: someLoadCallback
  //   })
  //   viewer.load(annotation)
  //
  // Returns a new Viewer instance.
  constructor(options: any) {
     // Configuration options
     super($(Viewer.html.element)[0], { ...Viewer.DEFAULT_OPTIONS, ...options });

    // Events to be bound to the @element.
    Object.assign(this.events, {
      ".annotator-edit click": "onEditClick",
      ".annotator-delete click": "onDeleteClick",
      ".annotator-select click": "onSelectClick"
    });

    // Classes for toggling annotator state.
    Object.assign(this.classes, {
      hide: 'annotator-hide',
      showControls: 'annotator-visible'
    });

    this.show = this.show.bind(this);
    this.hide = this.hide.bind(this);
    this.load = this.load.bind(this);
    this.onEditClick = this.onEditClick.bind(this);
    this.onDeleteClick = this.onDeleteClick.bind(this);
    this.onSelectClick = this.onSelectClick.bind(this);

    this.item = $(Viewer.html.item)[0];
    this.fields = [];
    this.annotations = [];

    this.addEvents();
  }

  // Public: Displays the Viewer and first the "show" event. Can be used as an
  // event callback and will call Event#preventDefault() on the supplied event.
  //
  // event - Event object provided if method is called by event
  //         listener (default:undefined)
  //
  // Examples
  //
  //   # Displays the editor.
  //   viewer.show()
  //
  //   # Displays the viewer on click (prevents default action).
  //   $('a.show-viewer').bind('click', viewer.show)
  //
  // Returns itself.
  show(event?: undefined) {
    Util.preventEventDefault(event);

    const controls = this.element
      .find('.annotator-controls')
      .addClass(this.classes.showControls);
    setTimeout((() => controls.removeClass(this.classes.showControls)), 500);

    this.element.removeClass(this.classes.hide);
    return this.checkOrientation().publish('show');
  }

  // Public: Checks to see if the Viewer is currently displayed.
  //
  // Examples
  //
  //   viewer.show()
  //   viewer.isShown() # => Returns true
  //
  //   viewer.hide()
  //   viewer.isShown() # => Returns false
  //
  // Returns true if the Viewer is visible.
  isShown() {
    return !this.element.hasClass(this.classes.hide);
  }

  // Public: Hides the Editor and fires the "hide" event. Can be used as an event
  // callback and will call Event#preventDefault() on the supplied event.
  //
  // event - Event object provided if method is called by event
  //         listener (default:undefined)
  //
  // Examples
  //
  //   # Hides the editor.
  //   viewer.hide()
  //
  //   # Hide the viewer on click (prevents default action).
  //   $('a.hide-viewer').bind('click', viewer.hide)
  //
  // Returns itself.
  hide(event?: any) : Viewer {
    Util.preventEventDefault(event);

    this.element.addClass(this.classes.hide);
    return this.publish('hide') as Viewer;
  }

  // Public: Loads annotations into the viewer and shows it. Fires the "load"
  // event once the viewer is loaded passing the annotations into the callback.
  //
  // annotation - An Array of annotation elements.
  //
  // Examples
  //
  //   viewer.load([annotation1, annotation2, annotation3])
  //
  // Returns itself.
  load(annotations: {}) {
    this.annotations = annotations || [];

    const list = this.element.find('ul:first').empty();
    for (let annotation of Array.from(this.annotations)) {
      const item = $(this.item).clone().appendTo(list).data('annotation', annotation);
      const controls = item.find('.annotator-controls');

      const link = controls.find('.annotator-link');
      const edit = controls.find('.annotator-edit');
      const del = controls.find('.annotator-delete');
      const select = item.find('.annotator-select');

      const links = new LinkParser(annotation.links || []).get('alternate', { 'type': 'text/html' });
      if ((links.length === 0) || (links[0].href == null)) {
        link.remove();
      } else {
        link.attr('href', links[0].href);
      }

      // INCEPTION EXTENSION BEGIN
      //     if @options.readOnly
      //       edit.remove()
      //       del.remove()
      //     else
      //       controller = {
      //         showEdit: -> edit.removeAttr('disabled')
      //         hideEdit: -> edit.attr('disabled', 'disabled')
      //         showDelete: -> del.removeAttr('disabled')
      //         hideDelete: -> del.attr('disabled', 'disabled')
      //       }
      edit.remove();
      del.remove();
      const controller = {};
      // INCEPTION EXTENSION END

      for (let field of Array.from(this.fields)) {
        const element = $(field.element).clone().appendTo(item)[0];
        field.load(element, annotation, controller);
      }
    }

    this.publish('load', [this.annotations]);

    return this.show();
  }

  // Public: Adds an additional field to an annotation view. A callback can be
  // provided to update the view on load.
  //
  // options - An options Object. Options are as follows:
  //           load - Callback Function called when the view is loaded with an
  //                  annotation. Receives a newly created clone of @item and
  //                  the annotation to be displayed (it will be called once
  //                  for each annotation being loaded).
  //
  // Examples
  //
  //   # Display a user name.
  //   viewer.addField({
  //     # This is called when the viewer is loaded.
  //     load: (field, annotation) ->
  //       field = $(field)
  //
  //       if annotation.user
  //         field.text(annotation.user) # Display the user
  //       else
  //         field.remove()              # Do not display the field.
  //   })
  //
  // Returns itself.
  addField(options: any) : Viewer {
    const field = $.extend({
      load() { }
    }, options);

    field.element = $('<div />')[0];
    this.fields.push(field);
    field.element;
    return this;
  }

  // Callback function: called when the edit button is clicked.
  //
  // event - An Event object.
  //
  // Returns nothing.
  onEditClick(event: any) {
    return this.onButtonClick(event, 'edit');
  }

  // Callback function: called when the delete button is clicked.
  //
  // event - An Event object.
  //
  // Returns nothing.
  onDeleteClick(event: any) {
    return this.onButtonClick(event, 'delete');
  }

  // Callback function: called when the select button is clicked.
  //
  // event - An Event object.
  //
  // Returns nothing.
  onSelectClick(event: any) {
    return this.onButtonClick(event, 'select');
  }

  // Fires an event of type and passes in the associated annotation.
  //
  // event - An Event object.
  // type  - The type of event to fire. Either "edit" or "delete".
  //
  // Returns nothing.
  onButtonClick(event: { target: any; }, type: string) {
    const item = $(event.target).parents('.annotator-annotation');

    return this.publish(type, [item.data('annotation')]);
  }
}

// Private: simple parser for hypermedia link structure
//
// Examples:
//
//   links = [
//     { rel: 'alternate', href: 'http://example.com/pages/14.json', type: 'application/json' },
//     { rel: 'prev': href: 'http://example.com/pages/13' }
//   ]
//
//   lp = LinkParser(links)
//   lp.get('alternate')                      # => [ { rel: 'alternate', href: 'http://...', ... } ]
//   lp.get('alternate', {type: 'text/html'}) # => []
//
class LinkParser {
  data: any;
  constructor(data: any) {
    this.data = data;
  }

  get(rel: string, cond: { [x: string]: any; type?: string; }) {
    let k: string | number;
    if (cond == null) { cond = {}; }
    cond = $.extend({}, cond, { rel });
    const keys = ((() => {
      const result = [];
      for (k of Object.keys(cond || {})) {
        const v = cond[k];
        result.push(k);
      }
      return result;
    })());
    return (() => {
      const result1 = [];
      for (var d of Array.from(this.data)) {
        const match = keys.reduce(((m: any, k: string | number) => m && (d[k] === cond[k])), true);
        if (match) {
          result1.push(d);
        } else {
          continue;
        }
      }
      return result1;
    })();
  }
}

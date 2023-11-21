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

// Selection and range creation reference for the following code: lal
// http://www.quirksmode.org/dom/range_intro.html lala
//
// I've removed any support for IE TextRange (see commit d7085bf2 for code)
// for the moment, having no means of testing it.

import $ from 'jquery';
import { Delegator } from './class';
import { Viewer } from './viewer';
import { Editor } from './editor';
import { BrowserRange, sniff } from './range';
import * as Util from './util';
import { _t } from './util';

export class Annotator extends Delegator {
  // User customisable options available.
  static readonly DEFAULT_OPTIONS = { readOnly: false };

  static html = {
    adder: '<div class="annotator-adder"><button>' + _t('Annotate') + '</button></div>',
    wrapper: '<div class="annotator-wrapper"></div>'
  };

  plugins: {} = {};
  editor: Editor;
  viewer: Viewer;
  selectedRanges: any = null;
  mouseIsDown: boolean = false;
  ignoreMouseup: boolean = false;
  viewerHideTimer: any = null;
  adder: any;
  element: JQuery<HTMLElement>;
  annotation: { ranges?: any; quote?: any; highlights?: any; color?: any; id?: any; };
  wrapper: JQuery<HTMLElement>;

  // Public: Creates an instance of the Annotator. Requires a DOM Element in
  // which to watch for annotations as well as any options.
  //
  // NOTE: If the Annotator is not supported by the current browser it will not
  // perform any setup and simply return a basic object. This allows plugins
  // to still be loaded but will not function as expected. 
  //
  // element - A DOM Element in which to annotate.
  // options - An options Object. NOTE: There are currently no user options.
  //
  // Examples
  //
  //   annotator = new Annotator(document.body)
  // 
  // Returns a new instance of the Annotator.
  constructor(element: any, options: any) {
    // Start Annotator in read-only mode. No controls will be shown.
    super(element, { ...Annotator.DEFAULT_OPTIONS, ...options });

    console.info("New AnnotatorJS instance created!");

    // Events to be bound on Annotator#element.
    Object.assign(this.events, {
      ".annotator-adder button click": "onAdderClick",
      ".annotator-adder button mousedown": "onAdderMousedown",
      ".annotator-hl mouseover": "onHighlightMouseover",
      ".annotator-hl mouseout": "startViewerHideTimer"
    });

    this.showEditor = this.showEditor.bind(this);
    this.onEditorHide = this.onEditorHide.bind(this);
    this.onEditorSubmit = this.onEditorSubmit.bind(this);
    this.showViewer = this.showViewer.bind(this);
    this.startViewerHideTimer = this.startViewerHideTimer.bind(this);
    this.clearViewerHideTimer = this.clearViewerHideTimer.bind(this);
    this.checkForStartSelection = this.checkForStartSelection.bind(this);
    this.checkForEndSelection = this.checkForEndSelection.bind(this);
    this.onHighlightMouseover = this.onHighlightMouseover.bind(this);
    this.onAdderMousedown = this.onAdderMousedown.bind(this);
    this.onAdderClick = this.onAdderClick.bind(this);
    this.onEditAnnotation = this.onEditAnnotation.bind(this);
    this.onDeleteAnnotation = this.onDeleteAnnotation.bind(this);
    this.onSelectAnnotation = this.onSelectAnnotation.bind(this);
    this.plugins = {};

    if (!this.options.readOnly) { this._setupDocumentEvents(); }
    this._setupWrapper()._setupViewer()._setupEditor();
    this._setupDynamicStyle();

    // Create adder
    this.adder = $(Annotator.html.adder).appendTo(this.wrapper).hide();

    this.addEvents();
  }

  // Wraps the children of @element in a @wrapper div. NOTE: This method will also
  // remove any script elements inside @element to prevent them re-executing.
  //
  // Returns itself to allow chaining.
  _setupWrapper() {
    this.wrapper = $(Annotator.html.wrapper);

    // We need to remove all scripts within the element before wrapping the
    // contents within a div. Otherwise when scripts are reappended to the DOM
    // they will re-execute. This is an issue for scripts that call
    // document.write() - such as ads - as they will clear the page.
    this.element.find('script').remove();
    this.element.wrapInner(this.wrapper);
    this.wrapper = this.element.find('.annotator-wrapper');

    return this;
  }

  // Creates an instance of Annotator.Viewer and assigns it to the @viewer
  // property, appends it to the @wrapper and sets up event listeners.
  //
  // Returns itself to allow chaining.
  _setupViewer() {
    this.viewer = new Viewer({ readOnly: this.options.readOnly });
    this.viewer
      .hide()
      .addField({
        load: (field: any, annotation: { text: any; }) => {
          if (annotation.text) {
            $(field).html("<div class='annotator-select'>" + Util.escape(annotation.text) + "</div>");
          } else {
            $(field).html(`<div class='annotator-select'><i>${_t('No Comment')}</i></div>`);
          }
          return this.publish('annotationViewerTextField', [field, annotation]);
        }
      })
      .on("edit", this.onEditAnnotation)
      .on("delete", this.onDeleteAnnotation)
      .on("select", this.onSelectAnnotation)
      .element.appendTo(document.body)
      .bind("mouseover", this.clearViewerHideTimer)
      .bind("mouseout", this.startViewerHideTimer);
    this.viewer.addEvents();
    return this;
  }

  // Creates an instance of the Annotator.Editor and assigns it to @editor.
  // Appends this to the @wrapper and sets up event listeners.
  //
  // Returns itself for chaining.
  _setupEditor(): Annotator {
    this.editor = new Editor();
    this.editor
      .hide()
      .on('hide', this.onEditorHide)
      .on('save', this.onEditorSubmit);
    this.editor.addField({
      type: 'textarea',
      label: _t('Comments') + '\u2026',
      load(field: any, annotation: { text: any; }) {
        return $(field).find('textarea').val(annotation.text || '');
      },
      submit(field: any, annotation: { text: any; }) {
        return annotation.text = $(field).find('textarea').val();
      }
    });

    this.editor.element.appendTo(this.wrapper);
    return this;
  }

  // Sets up the selection event listeners to watch mouse actions on the document.
  //
  // Returns itself for chaining.
  _setupDocumentEvents(): Annotator {
    $(document).on({
      "mouseup": this.checkForEndSelection,
      "mousedown": this.checkForStartSelection
    });
    return this;
  }

  // Sets up any dynamically calculated CSS for the Annotator.
  //
  // Returns itself for chaining.
  _setupDynamicStyle() {
    let style = $('#annotator-dynamic-style');

    if (!style.length) {
      style = $('<style id="annotator-dynamic-style"></style>').appendTo(document.head);
    }

    const sel = '*' + (['adder', 'outer', 'notice', 'filter'].map((x: any) => `:not(.annotator-${x})`)).join('');

    // use the maximum z-index in the page
    let max = Util.maxZIndex($(document.body).find(sel));

    // but don't go smaller than 1010, because this isn't bulletproof --
    // dynamic elements in the page (notifications, dialogs, etc.) may well
    // have high z-indices that we can't catch using the above method.
    max = Math.max(max, 1000);

    style.text([
      ".annotator-adder, .annotator-outer, .annotator-notice {",
      `  z-index: ${max + 20};`,
      "}",
      ".annotator-filter {",
      `  z-index: ${max + 10};`,
      "}"
    ].join("\n")
    );

    return this;
  }

  clearAnnotations() {
    this.wrapper.find('.annotator-hl').each(function () {
      $(this).contents().insertBefore(this);
      return $(this).remove();
    });
  }

  // Public: Destroy the current Annotator instance, unbinding all events and
  // disposing of all relevant elements.
  //
  // Returns nothing.
  destroy(...params) {
    let delegates = super.destroy(...params);
    delegates.forEach(e => e.destroy());

    $(document).off({
      "mouseup": this.checkForEndSelection,
      "mousedown": this.checkForStartSelection
    });

    $('#annotator-dynamic-style').remove();

    this.adder.remove();
    this.viewer.destroy();
    this.editor.destroy();

    this.clearAnnotations();

    this.wrapper.contents().insertBefore(this.wrapper);
    this.wrapper.remove();
    this.element.data('annotator', null);

    for (let name in this.plugins) {
      const plugin = this.plugins[name];
      if (typeof this.plugins[name].destroy === 'function') {
        this.plugins[name].destroy();
      }
    }

    return [];
  }

  // Public: Gets the current selection excluding any nodes that fall outside of
  // the @wrapper. Then returns and Array of NormalizedRange instances.
  //
  // Examples
  //
  //   # A selection inside @wrapper
  //   annotation.getSelectedRanges()
  //   # => Returns [NormalizedRange]
  //
  //   # A selection outside of @wrapper
  //   annotation.getSelectedRanges()
  //   # => Returns []
  //
  // Returns Array of NormalizedRange instances.
  getSelectedRanges() {
    let r: any;
    const selection = Util.getGlobal().getSelection();

    let ranges = [];
    const rangesToIgnore = [];
    if (!selection.isCollapsed) {
      ranges = (() => {
        const result = [];
        for (let i = 0, end = selection.rangeCount, asc = 0 <= end; asc ? i < end : i > end; asc ? i++ : i--) {
          r = selection.getRangeAt(i);
          const browserRange = new BrowserRange(r);
          const normedRange = browserRange.normalize().limit(this.wrapper[0]);

          // If the new range falls fully outside the wrapper, we
          // should add it back to the document but not return it from
          // this method
          if (normedRange === null) { rangesToIgnore.push(r); }

          result.push(normedRange);
        }
        return result;
      })();

      // BrowserRange#normalize() modifies the DOM structure and deselects the
      // underlying text as a result. So here we remove the selected ranges and
      // reapply the new ones.
      selection.removeAllRanges();
    }

    for (r of Array.from(rangesToIgnore)) {
      selection.addRange(r);
    }

    // Remove any ranges that fell outside of @wrapper.
    return $.grep(ranges, function (range: { toRange: () => any; }) {
      // Add the normed range back to the selection if it exists.
      if (range) { selection.addRange(range.toRange()); }
      return range;
    });
  }

  // Public: Creates and returns a new annotation object. Publishes the
  // 'beforeAnnotationCreated' event to allow the new annotation to be modified.
  //
  // Examples
  //
  //   annotator.createAnnotation() # Returns {}
  //
  //   annotator.on 'beforeAnnotationCreated', (annotation) ->
  //     annotation.myProperty = 'This is a custom property'
  //   annotator.createAnnotation() # Returns {myProperty: "This is a…"}
  //
  // Returns a newly created annotation Object.
  createAnnotation() {
    const annotation = {};
    this.publish('beforeAnnotationCreated', [annotation]);
    return annotation;
  }

  // Public: Initialises an annotation either from an object representation or
  // an annotation created with Annotator#createAnnotation(). It finds the
  // selected range and highlights the selection in the DOM.
  //
  // annotation - An annotation Object to initialise.
  //
  // Examples
  //
  //   # Create a brand new annotation from the currently selected text.
  //   annotation = annotator.createAnnotation()
  //   annotation = annotator.setupAnnotation(annotation)
  //   # annotation has now been assigned the currently selected range
  //   # and a highlight appended to the DOM.
  //
  //   # Add an existing annotation that has been stored elsewhere to the DOM.
  //   annotation = getStoredAnnotationWithSerializedRanges()
  //   annotation = annotator.setupAnnotation(annotation)
  //
  // Returns the initialised annotation.
  setupAnnotation(annotation: { ranges?: any; quote?: any; highlights?: any; color?: any; id?: any; }) {
    const root = this.wrapper[0];
    if (!annotation.ranges) { annotation.ranges = this.selectedRanges; }

    const normedRanges = [];
    for (let r of Array.from(annotation.ranges)) {
      try {
        normedRanges.push(sniff(r).normalize(root));
      } catch (e) {
        if (e instanceof RangeError) {
          this.publish('rangeNormalizeFail', [annotation, r, e]);
        } else {
          // Oh Javascript, why you so crap? This will lose the traceback.
          throw e;
        }
      }
    }

    annotation.quote = [];
    annotation.ranges = [];
    annotation.highlights = [];

    for (let normed of Array.from(normedRanges)) {
      annotation.quote.push($.trim(normed.text()));
      // INCEPTION EXTENSION BEGIN
      // The backend is unable to interpret start/end XPath expressions, so we need to get all 
      // offsets relative to the annotator-wrapper.
      //     annotation.ranges.push     normed.serialize(@wrapper[0], '.annotator-hl')
      annotation.ranges.push(normed.serialize(this.wrapper[0], ':not(.annotator-wrapper)'));
      // INCEPTION EXTENSION END
      if (annotation.color) {
        $.merge(annotation.highlights, this.highlightRange(normed, `background-color: ${annotation.color}`));
      } else {
        $.merge(annotation.highlights, this.highlightRange(normed));
      }
    }

    // Join all the quotes into one string.
    annotation.quote = annotation.quote.join(' / ');

    // Save the annotation data on each highlighter element.
    $(annotation.highlights).data('annotation', annotation);
    $(annotation.highlights).attr('data-annotation-id', annotation.id);

    return annotation;
  }

  // Public: Publishes the 'beforeAnnotationUpdated' and 'annotationUpdated'
  // events. Listeners wishing to modify an updated annotation should subscribe
  // to 'beforeAnnotationUpdated' while listeners storing annotations should
  // subscribe to 'annotationUpdated'.
  //
  // annotation - An annotation Object to update.
  //
  // Examples
  //
  //   annotation = {tags: 'apples oranges pears'}
  //   annotator.on 'beforeAnnotationUpdated', (annotation) ->
  //     # validate or modify a property.
  //     annotation.tags = annotation.tags.split(' ')
  //   annotator.updateAnnotation(annotation)
  //   # => Returns ["apples", "oranges", "pears"]
  //
  // Returns annotation Object.
  updateAnnotation(annotation: { highlights: any; id: any; }) {
    this.publish('beforeAnnotationUpdated', [annotation]);
    $(annotation.highlights).attr('data-annotation-id', annotation.id);
    this.publish('annotationUpdated', [annotation]);
    return annotation;
  }

  // Public: Deletes the annotation by removing the highlight from the DOM.
  // Publishes the 'annotationDeleted' event on completion.
  //
  // annotation - An annotation Object to delete.
  //
  // Returns deleted annotation.
  deleteAnnotation(annotation: { highlights: any; }) {
    if (annotation.highlights != null) {
      for (let h of Array.from(annotation.highlights)) {
        if (h.parentNode != null) {
          const child = h.childNodes[0];
          $(h).replaceWith(h.childNodes);
        }
      }
    }

    this.publish('annotationDeleted', [annotation]);
    return annotation;
  }

  // Public: Selects the annotation by calling out to the server
  //
  // annotation - An annotation Object to select.
  //
  // Returns selected annotation.
  selectAnnotation(annotation: any) {
    this.publish('annotationSelected', [annotation]);
    return annotation;
  }

  // Public: Loads an Array of annotations into the @element. Breaks the task
  // into chunks of 10 annotations.
  //
  // annotations - An Array of annotation Objects.
  //
  // Examples
  //
  //   loadAnnotationsFromStore (annotations) ->
  //     annotator.loadAnnotations(annotations)
  //
  // Returns itself for chaining.
  loadAnnotations(annotations: { slice?: any; }) {
    if (annotations == null) { annotations = []; }
    var loader = (annList: { splice?: any; length?: any; }) => {
      if (annList == null) { annList = []; }
      const now = annList.splice(0, 10);

      for (let n of Array.from(now)) {
        this.setupAnnotation(n);
      }

      // If there are more to do, do them after a 10ms break (for browser
      // responsiveness).
      if (annList.length > 0) {
        return setTimeout((() => loader(annList)), 10);
      } else {
        return this.publish('annotationsLoaded', [clone]);
      }
    };

    var clone = annotations.slice();
    loader(annotations);

    return this;
  }

  // Public: Calls the Store#dumpAnnotations() method.
  //
  // Returns dumped annotations Array or false if Store is not loaded.
  dumpAnnotations() {
    if (this.plugins['Store']) {
      return this.plugins['Store'].dumpAnnotations();
    } else {
      console.warn(_t("Can't dump annotations without Store plugin."));
      return false;
    }
  }

  // Public: Wraps the DOM Nodes within the provided range with a highlight
  // element of the specified class and returns the highlight Elements.
  //
  // normedRange - A NormalizedRange to be highlighted.
  // cssClass - A CSS class to use for the highlight (default: 'annotator-hl')
  //
  // Returns an array of highlight Elements.
  highlightRange(normedRange: { textNodes: () => any; }, cssStyle: string, cssClass: string) {
    if (cssStyle == null) { cssStyle = ''; }
    if (cssClass == null) { cssClass = 'annotator-hl'; }
    const white = /^\s*$/;

    const hl = $(`<span class='${cssClass}' style='${cssStyle}'></span>`);

    // Ignore text nodes that contain only whitespace characters. This prevents
    // spans being injected between elements that can only contain a restricted
    // subset of nodes such as table rows and lists. This does mean that there
    // may be the odd abandoned whitespace node in a paragraph that is skipped
    // but better than breaking table layouts.
    return (() => {
      const result = [];
      for (let node of Array.from(normedRange.textNodes())) {
        if (!white.test(node.nodeValue)) {
          result.push($(node).wrapAll(hl).parent().show()[0]);
        }
      }
      return result;
    })();
  }

  // Public: highlight a list of ranges
  //
  // normedRanges - An array of NormalizedRanges to be highlighted.
  // cssClass - A CSS class to use for the highlight (default: 'annotator-hl')
  //
  // Returns an array of highlight Elements.
  highlightRanges(normedRanges: any, cssStyle: string, cssClass: string) {
    if (cssStyle == null) { cssStyle = ''; }
    if (cssClass == null) { cssClass = 'annotator-hl'; }
    const highlights = [];
    for (let r of Array.from(normedRanges)) {
      $.merge(highlights, this.highlightRange(r, cssStyle, cssClass));
    }
    return highlights;
  }

  // Public: Registers a plugin with the Annotator. A plugin can only be
  // registered once. The plugin will be instantiated in the following order.
  //
  // 1. A new instance of the plugin will be created (providing the @element and
  //    options as params) then assigned to the @plugins registry.
  // 2. The current Annotator instance will be attached to the plugin.
  // 3. The Plugin#pluginInit() method will be called if it exists.
  //
  // name    - Plugin to instantiate. Must be in the Annotator.Plugins namespace.
  // options - Any options to be provided to the plugin constructor.
  //
  // Examples
  //
  //   annotator
  //     .addPlugin('Tags')
  //     .addPlugin('Store', {
  //       prefix: '/store'
  //     })
  //     .addPlugin('Permissions', {
  //       user: 'Bill'
  //     })
  //
  // Returns itself to allow chaining.
  addPlugin(name: string, plugin: Plugin) {
    if (this.plugins[name]) {
      console.error(_t("You cannot have more than one instance of any plugin."));
    } else {
      this.plugins[name] = plugin;
      this.plugins[name].annotator = this;
      if (typeof this.plugins[name].pluginInit === 'function') {
        this.plugins[name].pluginInit();
      }
    }
    return this; // allow chaining
  }

  // Public: Loads the @editor with the provided annotation and updates its
  // position in the window.
  //
  // annotation - An annotation to load into the editor.
  // location   - Position to set the Editor in the form {top: y, left: x}
  //
  // Examples
  //
  //   annotator.showEditor({text: "my comment"}, {top: 34, left: 234})
  //
  // Returns itself to allow chaining.
  showEditor(annotation: any, location: any) {
    this.editor.element.css(location);
    this.editor.load(annotation);
    this.publish('annotationEditorShown', [this.editor, annotation]);
    return this;
  }

  // Callback method called when the @editor fires the "hide" event. Itself
  // publishes the 'annotationEditorHidden' event and resets the @ignoreMouseup
  // property to allow listening to mouse events.
  //
  // Returns nothing.
  onEditorHide() {
    this.publish('annotationEditorHidden', [this.editor]);
    return this.ignoreMouseup = false;
  }

  // Callback method called when the @editor fires the "save" event. Itself
  // publishes the 'annotationEditorSubmit' event and creates/updates the
  // edited annotation.
  //
  // Returns nothing.
  onEditorSubmit(annotation: any) {
    return this.publish('annotationEditorSubmit', [this.editor, annotation]);
  }

  // Public: Loads the @viewer with an Array of annotations and positions it
  // at the location provided. Calls the 'annotationViewerShown' event.
  //
  // annotation - An Array of annotations to load into the viewer.
  // location   - Position to set the Viewer in the form {top: y, left: x}
  //
  // Examples
  //
  //   annotator.showViewer(
  //    [{text: "my comment"}, {text: "my other comment"}],
  //    {top: 34, left: 234})
  //   )
  //
  // Returns itself to allow chaining.
  showViewer(annotations: any, location: any) {
    this.viewer.element.css(location);
    this.viewer.load(annotations);

    return this.publish('annotationViewerShown', [this.viewer, annotations]);
  }

  // Annotator#element event callback. Allows 250ms for mouse pointer to get from
  // annotation highlight to @viewer to manipulate annotations. If timer expires
  // the @viewer is hidden.
  //
  // Returns nothing.
  startViewerHideTimer() {
    // Don't do this if timer has already been set by another annotation.
    if (!this.viewerHideTimer) {
      return this.viewerHideTimer = setTimeout(this.viewer.hide, 250);
    }
  }

  // Viewer#element event callback. Clears the timer set by
  // Annotator#startViewerHideTimer() when the @viewer is moused over.
  //
  // Returns nothing.
  clearViewerHideTimer() {
    clearTimeout(this.viewerHideTimer);
    return this.viewerHideTimer = false;
  }

  // Annotator#element callback. Sets the @mouseIsDown property used to
  // determine if a selection may have started to true. Also calls
  // Annotator#startViewerHideTimer() to hide the Annotator#viewer.
  //
  // event - A mousedown Event object.
  //
  // Returns nothing.
  checkForStartSelection(event: { target: any; }) {
    if (!event || !this.isAnnotator(event.target)) {
      this.startViewerHideTimer();
    }
    return this.mouseIsDown = true;
  }

  // Annotator#element callback. Checks to see if a selection has been made
  // on mouseup and if so displays the Annotator#adder. If @ignoreMouseup is
  // set will do nothing. Also resets the @mouseIsDown property.
  //
  // event - A mouseup Event object.
  //
  // Returns nothing.
  checkForEndSelection(event: any) {
    this.mouseIsDown = false;

    // This prevents the note image from jumping away on the mouseup
    // of a click on icon.
    if (this.ignoreMouseup) {
      return;
    }

    // Get the currently selected ranges.
    this.selectedRanges = this.getSelectedRanges();

    for (let range of Array.from(this.selectedRanges)) {
      const container = range.commonAncestor;
      if (this.isAnnotator(container)) { return; }
    }

    // INCEPTION EXTENSION BEGIN
    // When a user selects a span of text, we want to immediately create an annotation at that position.
    // We do not first want to display the "adder" icon and we also don't want to use the AnnotatorJS
    // editor.
    //    if event and @selectedRanges.length
    //      @adder
    //        .css(Util.mousePosition(event, @wrapper[0]))
    //        .show()
    //    else
    //      @adder.hide()
    this.annotation = this.setupAnnotation(this.createAnnotation());
    $(this.annotation.highlights).removeClass('annotator-hl-temporary');
    if (this.annotation.ranges.length > 0) {
      this.publish('annotationCreated', [this.annotation]);
    }

  }
  // INCEPTION EXTENSION END


  // Public: Determines if the provided element is part of the annotator plugin.
  // Useful for ignoring mouse actions on the annotator elements.
  // NOTE: The @wrapper is not included in this check.
  //
  // element - An Element or TextNode to check.
  //
  // Examples
  //
  //   span = document.createElement('span')
  //   annotator.isAnnotator(span) # => Returns false
  //
  //   annotator.isAnnotator(annotator.viewer.element) # => Returns true
  //
  // Returns true if the element is a child of an annotator element.
  isAnnotator(element: any) {
    return !!$(element)
      .parents()
      .addBack()
      .filter('[class^=annotator-]')
      .not('[class^=annotator-hl]')
      .not(this.wrapper)
      .length;
  }

  // Annotator#element callback. Displays viewer with all annotations
  // associated with highlight Elements under the cursor.
  //
  // event - A mouseover Event object.
  //
  // Returns nothing.
  onHighlightMouseover(event: { target: any; }) {
    // Cancel any pending hiding of the viewer.
    this.clearViewerHideTimer();

    // Don't do anything if we're making a selection
    if (this.mouseIsDown) { return false; }

    // If the viewer is already shown, hide it first
    if (this.viewer.isShown()) { this.viewer.hide(); }

    const annotations = $(event.target)
      .parents('.annotator-hl')
      .addBack()
      .map(function () { return $(this).data("annotation"); })
      .toArray();

    // Now show the viewer with the wanted annotations
    return this.showViewer(annotations, Util.mousePosition(event, document.body));
  }

  // Annotator#element callback. Sets @ignoreMouseup to true to prevent
  // the annotation selection events firing when the adder is clicked.
  //
  // event - A mousedown Event object
  //
  // Returns nothing.
  onAdderMousedown(event: { preventDefault: () => void; }) {
    if (event != null) {
      event.preventDefault();
    }
    return this.ignoreMouseup = true;
  }

  // Annotator#element callback. Displays the @editor in place of the @adder and
  // loads in a newly created annotation Object. The click event is used as well
  // as the mousedown so that we get the :active state on the @adder when clicked
  //
  // event - A mousedown Event object
  //
  // Returns nothing.
  onAdderClick(event: { preventDefault: () => void; }) {
    if (event != null) {
      event.preventDefault();
    }

    // Hide the adder
    const position = this.adder.position();
    this.adder.hide();

    // Show a temporary highlight so the user can see what they selected
    // Also extract the quotation and serialize the ranges
    const annotation = this.setupAnnotation(this.createAnnotation());
    $(annotation.highlights).addClass('annotator-hl-temporary');

    // Subscribe to the editor events

    // Make the highlights permanent if the annotation is saved
    const save = () => {
      cleanup();
      $(annotation.highlights).removeClass('annotator-hl-temporary');
      // Fire annotationCreated events so that plugins can react to them
      return this.publish('annotationCreated', [annotation]);
    };

    // Remove the highlights if the edit is cancelled
    const cancel = () => {
      cleanup();
      return this.deleteAnnotation(annotation);
    };

    // Don't leak handlers at the end
    var cleanup = () => {
      this.unsubscribe('annotationEditorHidden', cancel);
      return this.unsubscribe('annotationEditorSubmit', save);
    };

    this.subscribe('annotationEditorHidden', cancel);
    this.subscribe('annotationEditorSubmit', save);

    // Display the editor.
    return this.showEditor(annotation, position);
  }

  // Annotator#viewer callback function. Displays the Annotator#editor in the
  // positions of the Annotator#viewer and loads the passed annotation for
  // editing.
  //
  // annotation - An annotation Object for editing.
  //
  // Returns nothing.
  onEditAnnotation(annotation: any) {
    const offset = this.viewer.element.position();

    // Subscribe once to editor events

    // Update the annotation when the editor is saved
    const update = () => {
      cleanup();
      return this.updateAnnotation(annotation);
    };

    // Remove handlers when the editor is hidden
    var cleanup = () => {
      this.unsubscribe('annotationEditorHidden', cleanup);
      return this.unsubscribe('annotationEditorSubmit', update);
    };

    this.subscribe('annotationEditorHidden', cleanup);
    this.subscribe('annotationEditorSubmit', update);

    // Replace the viewer with the editor
    this.viewer.hide();
    return this.showEditor(annotation, offset);
  }

  // Annotator#viewer callback function. Deletes the annotation provided to the
  // callback.
  //
  // annotation - An annotation Object for deletion.
  //
  // Returns nothing.
  onDeleteAnnotation(annotation: any) {
    this.viewer.hide();

    // Delete highlight elements.
    return this.deleteAnnotation(annotation);
  }

  // Annotator#viewer callback function. Selects the annotation provided to the
  // callback.
  //
  // annotation - An annotation Object for selection.
  //
  // Returns nothing.
  onSelectAnnotation(annotation: any) {
    this.viewer.hide();

    // Select highlight elements.
    return this.selectAnnotation(annotation);
  }
}

// Store a reference to the current Annotator object.
const _Annotator = Annotator;

// Create namespace for Annotator plugins
export class Plugin extends Delegator {
  constructor(element: any, options: any) {
    super(element, options);
  }

  pluginInit() { }
};

// Sniff the browser environment and attempt to add missing functionality.
const g = Util.getGlobal();

if (((g.document != null ? g.document.evaluate : undefined) == null)) {
  $.getScript('http://assets.annotateit.org/vendor/xpath.min.js');
}

if ((g.getSelection == null)) {
  $.getScript('http://assets.annotateit.org/vendor/ierange.min.js');
}

if ((g.JSON == null)) {
  $.getScript('http://assets.annotateit.org/vendor/json2.min.js');
}

// Ensure the Node constants are defined
if ((g.Node == null)) {
  g.Node = {
    ELEMENT_NODE: 1,
    ATTRIBUTE_NODE: 2,
    TEXT_NODE: 3,
    CDATA_SECTION_NODE: 4,
    ENTITY_REFERENCE_NODE: 5,
    ENTITY_NODE: 6,
    PROCESSING_INSTRUCTION_NODE: 7,
    COMMENT_NODE: 8,
    DOCUMENT_NODE: 9,
    DOCUMENT_TYPE_NODE: 10,
    DOCUMENT_FRAGMENT_NODE: 11,
    NOTATION_NODE: 12
  };
}

// Create global access for Annotator
$.fn.annotator = function (options: string) {
  const args = Array.prototype.slice.call(arguments, 1);
  return this.each(function () {
    // check the data() cache, if it's there we'll call the method requested
    let instance = $.data(this, 'annotator');
    if (options === 'destroy') {
      $.removeData(this, 'annotator');
      return (instance != null ? instance.destroy(args) : undefined);
    } else if (instance) {
      return options && instance[options].apply(instance, args);
    } else {
      instance = new Annotator(this, options);
      return $.data(this, 'annotator', instance);
    }
  });
};

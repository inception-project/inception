import React from 'react';
import ReactDOM from 'react-dom';
import axios from 'axios';
import Emitter from 'tiny-emitter';
import '@babel/polyfill';
import {
  WebAnnotation, 
  addPolyfills,
  createEnvironment,
  setLocale 
} from '@recogito/recogito-client-core';
import TextAnnotator from './TextAnnotator';
import { deflateHTML } from './utils';

addPolyfills(); // Extra polyfills that babel doesn't include

import '@recogito/recogito-client-core/themes/default';

/**
 * The entrypoint into the application. Provides the
 * externally visible JavaScript API.
 */
export class Recogito {

  constructor(config) {
    // API calls to this instance are forwarded through a ref
    this._app = React.createRef();

    // Event handling via tiny-emitter
    this._emitter = new Emitter();

    // Environment settings container
    this._environment = createEnvironment();

    // The content element (which contains the text we want to annotate)
    // is wrapped in a DIV ('wrapperEl'). The application container DIV,
    // which holds the editor popup, will be attached as a child to the
    // wrapper element (=a sibling to the content element). This way,
    // content and editor share the same CSS position reference frame.
    //
    // <wrapperEl>
    //   <contentEl />
    //   <appContainerEl />
    // </wrapperEl>
    //
    let contentEl = (config.content.nodeType) ?
      config.content : document.getElementById(config.content);

    // Unless this is preformatted text, remove multi spaces and
    // empty text nodes, so that HTML char offsets == browser offsets.
    if (config.mode !== 'pre')
      contentEl = deflateHTML(contentEl);

    const wrapperEl = document.createElement('DIV');
    wrapperEl.style.position = 'relative';
    contentEl.parentNode.insertBefore(wrapperEl, contentEl);
    wrapperEl.appendChild(contentEl);

    const appContainerEl = document.createElement('DIV');
    wrapperEl.appendChild(appContainerEl);

    setLocale(config.locale);

    ReactDOM.render(
      <TextAnnotator
        ref={this._app}
        env={this._environment}
        contentEl={contentEl}
        wrapperEl={wrapperEl}
        config={config}
        onAnnotationSelected={this.handleAnnotationSelected}
        onAnnotationCreated={this.handleAnnotationCreated}
        onAnnotationUpdated={this.handleAnnotationUpdated}
        onAnnotationDeleted={this.handleAnnotationDeleted}
        relationVocabulary={config.relationVocabulary} />, appContainerEl);
  }

  handleAnnotationSelected = annotation =>
    this._emitter.emit('selectAnnotation', annotation.underlying);

  handleAnnotationCreated = (annotation, overrideId) =>
    this._emitter.emit('createAnnotation', annotation.underlying, overrideId);

  handleAnnotationUpdated = (annotation, previous) =>
    this._emitter.emit('updateAnnotation', annotation.underlying, previous.underlying);

  handleAnnotationDeleted = annotation =>
    this._emitter.emit('deleteAnnotation', annotation.underlying);

  /******************/
  /*  External API  */
  /******************/

  /**
   * Adds a JSON-LD WebAnnotation to the annotation layer.
   */
  addAnnotation = annotation =>
    this._app.current.addAnnotation(new WebAnnotation(annotation));

  /** Clears the user auth information **/
  clearAuthInfo = () =>
    this._environment.user = null;

  /**
   * Returns all annotations
   */
  getAnnotations = () => {
    const annotations = this._app.current.getAnnotations();
    return annotations.map(a => a.underlying);
  }

  /**
   * Loads JSON-LD WebAnnotations from the given URL.
   */
  loadAnnotations = url => axios.get(url).then(response => {
    const annotations = response.data.map(a => new WebAnnotation(a));
    this._app.current.setAnnotations(annotations);
    return annotations;
  });

  /**
   * Removes an event handler.
   *
   * If no callback, removes all handlers for
   * the given event.
   */
  off = (event, callback) =>
    this._emitter.off(event, callback);

  /**
   * Adds an event handler.
   */
  on = (event, handler) =>
    this._emitter.on(event, handler);

  /**
   * Removes the given JSON-LD WebAnnotation from the annotation layer.
   */
  removeAnnotation = annotation =>
    this._app.current.removeAnnotation(new WebAnnotation(annotation));

  /** Initializes with the list of WebAnnotations **/
  setAnnotations = annotations => {
    const webannotations = annotations.map(a => new WebAnnotation(a));
    this._app.current.setAnnotations(webannotations);
  }

  /** Sets user auth information **/
  setAuthInfo = authinfo =>
    this._environment.user = authinfo;

  /**
   * Activates annotation or relationship drawing mode.
   * @param mode a string, either ANNOTATION (default) or RELATIONS
   */
  setMode = mode =>
    this._app.current.setMode(mode);

  /** Sets the current 'server time', to avoid problems with locally-generated timestamps **/
  setServerTime = timestamp =>
    this._environment.setServerTime(timestamp);

}

export const init = config => new Recogito(config);

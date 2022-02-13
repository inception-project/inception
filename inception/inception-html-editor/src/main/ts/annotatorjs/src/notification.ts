import { Delegator } from "./class";
import * as Util from './util';

// Public: A simple notification system that can be used to display information,
// warnings and errors to the user. Display of notifications are controlled
// cmpletely by CSS by adding/removing the @options.classes.show class. This
// allows styling/animation using CSS rather than hardcoding styles.
export class Notification extends Delegator {
  static readonly DEFAULT_OPTIONS = {
    html: "<div class='annotator-notice'></div>",
    classes: {
      show: "annotator-notice-show",
      info: "annotator-notice-info",
      success: "annotator-notice-success",
      error: "annotator-notice-error"
    }
  };

  events: { click: string; };
  options: { html: string; classes: { show: string; info: string; success: string; error: string; }; };
  currentStatus: any;

  // Public: Creates an instance of  Notification and appends it to the
  // document body.
  //
  // options - The following options can be provided.
  //           classes - A Object literal of classes used to determine state.
  //           html    - An HTML string used to create the notification.
  //
  // Examples
  //
  //   # Displays a notification with the text "Hello World"
  //   notification = new Annotator.Notification
  //   notification.show("Hello World")
  //
  // Returns
  constructor(options?: any) {
    const mergedOptions = { ...Notification.DEFAULT_OPTIONS, ...options };
    super($(mergedOptions.html).appendTo(document.body)[0], mergedOptions);

    Object.assign(this.events, { "click": "hide" });

    this.show = this.show.bind(this);
    this.hide = this.hide.bind(this);

    this.addEvents();
  }

  // Public: Displays the annotation with message and optional status. The
  // message will hide itself after 5 seconds or if the user clicks on it.
  //
  // message - A message String to display (HTML will be escaped).
  // status  - A status constant. This will apply a class to the element for
  //           styling. (default: Annotator.Notification.INFO)
  //
  // Examples
  //
  //   # Displays a notification with the text "Hello World"
  //   notification.show("Hello World")
  //
  //   # Displays a notification with the text "An error has occurred"
  //   notification.show("An error has occurred", Annotator.Notification.ERROR)
  //
  // Returns itself.
  show(message: any, status: any) {
    if (status == null) { status = INFO; }
    this.currentStatus = status;
    $(this.element)
      .addClass(this.options.classes.show)
      .addClass(this.options.classes[this.currentStatus])
      .html(Util.escape(message || ""));

    setTimeout(this.hide, 5000);
    return this;
  }

  // Public: Hides the notification.
  //
  // Examples
  //
  //   # Hides the notification.
  //   notification.hide()
  //
  // Returns itself.
  hide() {
    if (this.currentStatus == null) { this.currentStatus = INFO; }
    $(this.element)
      .removeClass(this.options.classes.show)
      .removeClass(this.options.classes[this.currentStatus]);
    return this;
  }
}

// Constants for controlling the display of the notification. Each constant
// adds a different class to the Notification#element.
export const INFO = 'info';
export const SUCCESS = 'success';
export const ERROR = 'error';

// Attach notification methods to the Annotation object on document ready.
const notification = new Notification();
export const showNotification = notification.show;
export const hideNotification = notification.hide;

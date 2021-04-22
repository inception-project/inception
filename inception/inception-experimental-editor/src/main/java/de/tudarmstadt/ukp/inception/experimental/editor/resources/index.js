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
 */

var __create = Object.create;
var __defProp = Object.defineProperty;
var __getProtoOf = Object.getPrototypeOf;
var __hasOwnProp = Object.prototype.hasOwnProperty;
var __getOwnPropNames = Object.getOwnPropertyNames;
var __getOwnPropDesc = Object.getOwnPropertyDescriptor;
var __markAsModule = (target) => __defProp(target, "__esModule", {value: true});
var __commonJS = (cb, mod) => () => (mod || cb((mod = {exports: {}}).exports, mod), mod.exports);
var __export = (target, all) => {
  for (var name in all)
    __defProp(target, name, {get: all[name], enumerable: true});
};
var __reExport = (target, module2, desc) => {
  if (module2 && typeof module2 === "object" || typeof module2 === "function") {
    for (let key of __getOwnPropNames(module2))
      if (!__hasOwnProp.call(target, key) && key !== "default")
        __defProp(target, key, {get: () => module2[key], enumerable: !(desc = __getOwnPropDesc(module2, key)) || desc.enumerable});
  }
  return target;
};
var __toModule = (module2) => {
  return __reExport(__markAsModule(__defProp(module2 != null ? __create(__getProtoOf(module2)) : {}, "default", module2 && module2.__esModule && "default" in module2 ? {get: () => module2.default, enumerable: true} : {value: module2, enumerable: true})), module2);
};

// node_modules/tiny-emitter/index.js
var require_tiny_emitter = __commonJS((exports, module2) => {
  function E() {
  }
  E.prototype = {
    on: function(name, callback, ctx) {
      var e = this.e || (this.e = {});
      (e[name] || (e[name] = [])).push({
        fn: callback,
        ctx
      });
      return this;
    },
    once: function(name, callback, ctx) {
      var self = this;
      function listener() {
        self.off(name, listener);
        callback.apply(ctx, arguments);
      }
      ;
      listener._ = callback;
      return this.on(name, listener, ctx);
    },
    emit: function(name) {
      var data = [].slice.call(arguments, 1);
      var evtArr = ((this.e || (this.e = {}))[name] || []).slice();
      var i = 0;
      var len = evtArr.length;
      for (i; i < len; i++) {
        evtArr[i].fn.apply(evtArr[i].ctx, data);
      }
      return this;
    },
    off: function(name, callback) {
      var e = this.e || (this.e = {});
      var evts = e[name];
      var liveEvents = [];
      if (evts && callback) {
        for (var i = 0, len = evts.length; i < len; i++) {
          if (evts[i].fn !== callback && evts[i].fn._ !== callback)
            liveEvents.push(evts[i]);
        }
      }
      liveEvents.length ? e[name] = liveEvents : delete e[name];
      return this;
    }
  };
  module2.exports = E;
  module2.exports.TinyEmitter = E;
});

// node_modules/websocket-ts/lib/backoff/backoff.js
var require_backoff = __commonJS((exports) => {
  "use strict";
  Object.defineProperty(exports, "__esModule", {value: true});
});

// node_modules/websocket-ts/lib/backoff/constantbackoff.js
var require_constantbackoff = __commonJS((exports) => {
  "use strict";
  Object.defineProperty(exports, "__esModule", {value: true});
  exports.ConstantBackoff = void 0;
  var ConstantBackoff = function() {
    function ConstantBackoff2(backoff) {
      this.reset = function() {
      };
      this.backoff = backoff;
    }
    ConstantBackoff2.prototype.next = function() {
      return this.backoff;
    };
    return ConstantBackoff2;
  }();
  exports.ConstantBackoff = ConstantBackoff;
});

// node_modules/websocket-ts/lib/backoff/exponentialbackoff.js
var require_exponentialbackoff = __commonJS((exports) => {
  "use strict";
  Object.defineProperty(exports, "__esModule", {value: true});
  exports.ExponentialBackoff = void 0;
  var ExponentialBackoff = function() {
    function ExponentialBackoff2(initial, expMax) {
      this.initial = initial;
      this.expMax = expMax;
      this.expCurrent = 1;
      this.current = this.initial;
    }
    ExponentialBackoff2.prototype.next = function() {
      var backoff = this.current;
      if (this.expMax > this.expCurrent++)
        this.current = this.current * 2;
      return backoff;
    };
    ExponentialBackoff2.prototype.reset = function() {
      this.expCurrent = 1;
      this.current = this.initial;
    };
    return ExponentialBackoff2;
  }();
  exports.ExponentialBackoff = ExponentialBackoff;
});

// node_modules/websocket-ts/lib/backoff/linearbackoff.js
var require_linearbackoff = __commonJS((exports) => {
  "use strict";
  Object.defineProperty(exports, "__esModule", {value: true});
  exports.LinearBackoff = void 0;
  var LinearBackoff = function() {
    function LinearBackoff2(initial, increment, maximum) {
      this.initial = initial;
      this.increment = increment;
      this.maximum = maximum;
      this.current = this.initial;
    }
    LinearBackoff2.prototype.next = function() {
      var backoff = this.current;
      var next = this.current + this.increment;
      if (this.maximum === void 0)
        this.current = next;
      else if (next <= this.maximum)
        this.current = next;
      return backoff;
    };
    LinearBackoff2.prototype.reset = function() {
      this.current = this.initial;
    };
    return LinearBackoff2;
  }();
  exports.LinearBackoff = LinearBackoff;
});

// node_modules/websocket-ts/lib/buffer/buffer.js
var require_buffer = __commonJS((exports) => {
  "use strict";
  Object.defineProperty(exports, "__esModule", {value: true});
});

// node_modules/websocket-ts/lib/buffer/lrubuffer.js
var require_lrubuffer = __commonJS((exports) => {
  "use strict";
  Object.defineProperty(exports, "__esModule", {value: true});
  exports.LRUBuffer = void 0;
  var LRUBuffer = function() {
    function LRUBuffer2(len) {
      this.writePtr = 0;
      this.wrapped = false;
      this.buffer = Array(len);
    }
    LRUBuffer2.prototype.len = function() {
      return this.wrapped ? this.buffer.length : this.writePtr;
    };
    LRUBuffer2.prototype.cap = function() {
      return this.buffer.length;
    };
    LRUBuffer2.prototype.read = function(es) {
      if (es === null || es === void 0 || es.length === 0 || this.buffer.length === 0)
        return 0;
      if (this.writePtr === 0 && !this.wrapped)
        return 0;
      var first = this.wrapped ? this.writePtr : 0;
      var last = first - 1 < 0 ? this.buffer.length - 1 : first - 1;
      for (var i = 0; i < es.length; i++) {
        var r = (first + i) % this.buffer.length;
        es[i] = this.buffer[r];
        if (r === last)
          return i + 1;
      }
      return es.length;
    };
    LRUBuffer2.prototype.write = function(es) {
      if (es === null || es === void 0 || es.length === 0 || this.buffer.length === 0)
        return 0;
      var start = es.length > this.buffer.length ? es.length - this.buffer.length : 0;
      for (var i = 0; i < es.length - start; i++) {
        this.buffer[this.writePtr] = es[start + i];
        this.writePtr = (this.writePtr + 1) % this.buffer.length;
        if (this.writePtr === 0)
          this.wrapped = true;
      }
      return es.length;
    };
    LRUBuffer2.prototype.forEach = function(fn) {
      if (this.writePtr === 0 && !this.wrapped)
        return 0;
      var cur = this.wrapped ? this.writePtr : 0;
      var last = this.wrapped ? cur - 1 < 0 ? this.buffer.length - 1 : cur - 1 : this.writePtr - 1;
      var len = this.len();
      while (true) {
        fn(this.buffer[cur]);
        if (cur === last)
          break;
        cur = (cur + 1) % this.buffer.length;
      }
      return len;
    };
    LRUBuffer2.prototype.clear = function() {
      this.writePtr = 0;
      this.wrapped = false;
    };
    return LRUBuffer2;
  }();
  exports.LRUBuffer = LRUBuffer;
});

// node_modules/websocket-ts/lib/buffer/timebuffer.js
var require_timebuffer = __commonJS((exports) => {
  "use strict";
  Object.defineProperty(exports, "__esModule", {value: true});
  exports.TimeBuffer = void 0;
  var TimeBuffer = function() {
    function TimeBuffer2(maxAge) {
      this.maxAge = maxAge;
    }
    TimeBuffer2.prototype.cap = function() {
      return Number.POSITIVE_INFINITY;
    };
    TimeBuffer2.prototype.len = function() {
      this.forwardTail();
      var cur = this.tail;
      var i = 0;
      while (cur !== void 0) {
        i++;
        cur = cur.n;
      }
      return i;
    };
    TimeBuffer2.prototype.read = function(es) {
      this.forwardTail();
      if (es.length === 0)
        return 0;
      var cur = this.tail;
      var i = 0;
      while (cur !== void 0) {
        es[i++] = cur.e;
        if (i === es.length)
          break;
        cur = cur.n;
      }
      return i;
    };
    TimeBuffer2.prototype.write = function(es) {
      for (var i = 0; i < es.length; i++)
        this.putElement(es[i]);
      return es.length;
    };
    TimeBuffer2.prototype.forEach = function(fn) {
      this.forwardTail();
      var cur = this.tail;
      var i = 0;
      while (cur !== void 0) {
        fn(cur.e);
        i++;
        cur = cur.n;
      }
      return i;
    };
    TimeBuffer2.prototype.putElement = function(e) {
      var newElement = {e, t: Date.now(), n: void 0};
      if (this.tail === void 0)
        this.tail = newElement;
      if (this.head === void 0)
        this.head = newElement;
      else {
        this.head.n = newElement;
        this.head = newElement;
      }
    };
    TimeBuffer2.prototype.forwardTail = function() {
      if (this.tail === void 0)
        return;
      var d = Date.now();
      while (d - this.tail.t > this.maxAge) {
        if (this.tail === this.head) {
          this.tail = void 0;
          this.head = void 0;
        } else
          this.tail = this.tail.n;
        if (this.tail === void 0)
          break;
      }
    };
    TimeBuffer2.prototype.clear = function() {
    };
    return TimeBuffer2;
  }();
  exports.TimeBuffer = TimeBuffer;
});

// node_modules/websocket-ts/lib/websocket.js
var require_websocket = __commonJS((exports) => {
  "use strict";
  Object.defineProperty(exports, "__esModule", {value: true});
  exports.Websocket = exports.WebsocketEvents = void 0;
  var WebsocketEvents;
  (function(WebsocketEvents2) {
    WebsocketEvents2["open"] = "open";
    WebsocketEvents2["close"] = "close";
    WebsocketEvents2["error"] = "error";
    WebsocketEvents2["message"] = "message";
    WebsocketEvents2["retry"] = "retry";
  })(WebsocketEvents = exports.WebsocketEvents || (exports.WebsocketEvents = {}));
  var Websocket2 = function() {
    function Websocket3(url, protocols, buffer, backoff) {
      var _this = this;
      this.eventListeners = {open: [], close: [], error: [], message: [], retry: []};
      this.closedByUser = false;
      this.retries = 0;
      this.handleOpenEvent = function(ev) {
        return _this.handleEvent(WebsocketEvents.open, ev);
      };
      this.handleCloseEvent = function(ev) {
        return _this.handleEvent(WebsocketEvents.close, ev);
      };
      this.handleErrorEvent = function(ev) {
        return _this.handleEvent(WebsocketEvents.error, ev);
      };
      this.handleMessageEvent = function(ev) {
        return _this.handleEvent(WebsocketEvents.message, ev);
      };
      this.url = url;
      this.protocols = protocols;
      this.buffer = buffer;
      this.backoff = backoff;
      this.tryConnect();
    }
    Object.defineProperty(Websocket3.prototype, "underlyingWebsocket", {
      get: function() {
        return this.websocket;
      },
      enumerable: false,
      configurable: true
    });
    Websocket3.prototype.send = function(data) {
      var _a;
      if (this.closedByUser)
        return;
      if (this.websocket === void 0 || this.websocket.readyState !== this.websocket.OPEN)
        (_a = this.buffer) === null || _a === void 0 ? void 0 : _a.write([data]);
      else
        this.websocket.send(data);
    };
    Websocket3.prototype.close = function(code, reason) {
      var _a;
      this.closedByUser = true;
      (_a = this.websocket) === null || _a === void 0 ? void 0 : _a.close(code, reason);
    };
    Websocket3.prototype.addEventListener = function(type, listener, options) {
      var eventListener = {listener, options};
      var eventListeners = this.eventListeners[type];
      eventListeners.push(eventListener);
    };
    Websocket3.prototype.removeEventListener = function(type, listener, options) {
      this.eventListeners[type] = this.eventListeners[type].filter(function(l) {
        return l.listener !== listener && (l.options === void 0 || l.options !== options);
      });
    };
    Websocket3.prototype.dispatchEvent = function(type, ev) {
      var _this = this;
      var listeners = this.eventListeners[type];
      var onceListeners = [];
      listeners.forEach(function(l) {
        l.listener(_this, ev);
        if (l.options !== void 0 && l.options.once)
          onceListeners.push(l);
      });
      onceListeners.forEach(function(l) {
        return _this.removeEventListener(type, l.listener, l.options);
      });
    };
    Websocket3.prototype.tryConnect = function() {
      if (this.websocket !== void 0) {
        this.websocket.removeEventListener(WebsocketEvents.open, this.handleOpenEvent);
        this.websocket.removeEventListener(WebsocketEvents.close, this.handleCloseEvent);
        this.websocket.removeEventListener(WebsocketEvents.error, this.handleErrorEvent);
        this.websocket.removeEventListener(WebsocketEvents.message, this.handleMessageEvent);
        this.websocket.close();
      }
      this.websocket = new WebSocket(this.url, this.protocols);
      this.websocket.addEventListener(WebsocketEvents.open, this.handleOpenEvent);
      this.websocket.addEventListener(WebsocketEvents.close, this.handleCloseEvent);
      this.websocket.addEventListener(WebsocketEvents.error, this.handleErrorEvent);
      this.websocket.addEventListener(WebsocketEvents.message, this.handleMessageEvent);
    };
    Websocket3.prototype.handleEvent = function(type, ev) {
      var _a, _b, _c;
      switch (type) {
        case WebsocketEvents.close:
          if (!this.closedByUser)
            this.reconnect();
          break;
        case WebsocketEvents.open:
          this.retries = 0;
          (_a = this.backoff) === null || _a === void 0 ? void 0 : _a.reset();
          (_b = this.buffer) === null || _b === void 0 ? void 0 : _b.forEach(this.send.bind(this));
          (_c = this.buffer) === null || _c === void 0 ? void 0 : _c.clear();
          break;
      }
      this.dispatchEvent(type, ev);
    };
    Websocket3.prototype.reconnect = function() {
      var _this = this;
      if (this.backoff === void 0)
        return;
      var backoff = this.backoff.next();
      setTimeout(function() {
        _this.dispatchEvent(WebsocketEvents.retry, new CustomEvent(WebsocketEvents.retry, {
          detail: {
            retries: ++_this.retries,
            backoff
          }
        }));
        _this.tryConnect();
      }, backoff);
    };
    return Websocket3;
  }();
  exports.Websocket = Websocket2;
});

// node_modules/websocket-ts/lib/websocketBuilder.js
var require_websocketBuilder = __commonJS((exports) => {
  "use strict";
  Object.defineProperty(exports, "__esModule", {value: true});
  exports.WebsocketBuilder = void 0;
  var websocket_1 = require_websocket();
  var WebsocketBuilder2 = function() {
    function WebsocketBuilder3(url) {
      this.ws = null;
      this.onOpenListeners = [];
      this.onCloseListeners = [];
      this.onErrorListeners = [];
      this.onMessageListeners = [];
      this.onRetryListeners = [];
      this.url = url;
    }
    WebsocketBuilder3.prototype.withProtocols = function(p) {
      this.protocols = p;
      return this;
    };
    WebsocketBuilder3.prototype.withBackoff = function(backoff) {
      this.backoff = backoff;
      return this;
    };
    WebsocketBuilder3.prototype.withBuffer = function(buffer) {
      this.buffer = buffer;
      return this;
    };
    WebsocketBuilder3.prototype.onOpen = function(listener, options) {
      this.onOpenListeners.push({listener, options});
      return this;
    };
    WebsocketBuilder3.prototype.onClose = function(listener, options) {
      this.onCloseListeners.push({listener, options});
      return this;
    };
    WebsocketBuilder3.prototype.onError = function(listener, options) {
      this.onErrorListeners.push({listener, options});
      return this;
    };
    WebsocketBuilder3.prototype.onMessage = function(listener, options) {
      this.onMessageListeners.push({listener, options});
      return this;
    };
    WebsocketBuilder3.prototype.onRetry = function(listener, options) {
      this.onRetryListeners.push({listener, options});
      return this;
    };
    WebsocketBuilder3.prototype.build = function() {
      var _this = this;
      if (this.ws !== null)
        return this.ws;
      this.ws = new websocket_1.Websocket(this.url, this.protocols, this.buffer, this.backoff);
      this.onOpenListeners.forEach(function(h) {
        var _a;
        return (_a = _this.ws) === null || _a === void 0 ? void 0 : _a.addEventListener(websocket_1.WebsocketEvents.open, h.listener, h.options);
      });
      this.onCloseListeners.forEach(function(h) {
        var _a;
        return (_a = _this.ws) === null || _a === void 0 ? void 0 : _a.addEventListener(websocket_1.WebsocketEvents.close, h.listener, h.options);
      });
      this.onErrorListeners.forEach(function(h) {
        var _a;
        return (_a = _this.ws) === null || _a === void 0 ? void 0 : _a.addEventListener(websocket_1.WebsocketEvents.error, h.listener, h.options);
      });
      this.onMessageListeners.forEach(function(h) {
        var _a;
        return (_a = _this.ws) === null || _a === void 0 ? void 0 : _a.addEventListener(websocket_1.WebsocketEvents.message, h.listener, h.options);
      });
      this.onRetryListeners.forEach(function(h) {
        var _a;
        return (_a = _this.ws) === null || _a === void 0 ? void 0 : _a.addEventListener(websocket_1.WebsocketEvents.retry, h.listener, h.options);
      });
      return this.ws;
    };
    return WebsocketBuilder3;
  }();
  exports.WebsocketBuilder = WebsocketBuilder2;
});

// node_modules/websocket-ts/lib/index.js
var require_lib = __commonJS((exports) => {
  "use strict";
  var __createBinding = exports && exports.__createBinding || (Object.create ? function(o, m, k, k2) {
    if (k2 === void 0)
      k2 = k;
    Object.defineProperty(o, k2, {enumerable: true, get: function() {
      return m[k];
    }});
  } : function(o, m, k, k2) {
    if (k2 === void 0)
      k2 = k;
    o[k2] = m[k];
  });
  var __exportStar = exports && exports.__exportStar || function(m, exports2) {
    for (var p in m)
      if (p !== "default" && !Object.prototype.hasOwnProperty.call(exports2, p))
        __createBinding(exports2, m, p);
  };
  Object.defineProperty(exports, "__esModule", {value: true});
  __exportStar(require_backoff(), exports);
  __exportStar(require_constantbackoff(), exports);
  __exportStar(require_exponentialbackoff(), exports);
  __exportStar(require_linearbackoff(), exports);
  __exportStar(require_buffer(), exports);
  __exportStar(require_lrubuffer(), exports);
  __exportStar(require_timebuffer(), exports);
  __exportStar(require_websocket(), exports);
  __exportStar(require_websocketBuilder(), exports);
});

var import_tiny_emitter = __toModule(require_tiny_emitter());

// util/Websocket.ts
var import_websocket_ts = __toModule(require_lib());
var Websocket = class {
  constructor() {
    this._createWebsocket = function(url) {
      this.websocket = new import_websocket_ts.WebsocketBuilder(url).onOpen((aWebsocket, e) => {
        console.log("opened");
      }).onClose((aWebsocket, e) => {
        console.log("closed");
      }).onError((aWebsocket, e) => {
        console.log("error");
      }).onMessage((aWebsocket, e) => {
        aWebsocket.send(e.data);
      }).build();
    };
  }
};

// drawer/Draw.ts
var Draw = class {
  constructor() {
    this._drawAnnotation = (aPosition, aType) => {
    };
    this._removeAnnotation = (aPosition, aType) => {
    };
    this._highlightAnnotation = (aPosition, aType) => {
      console.log("Would highlight now");
    };
  }
};

// index.ts
var Experimental = class {
  constructor() {
    this._eventSelectAnnotation = (event) => {
      this.emitter.emit("_send_select_annotation", this._selectAnnotation(event));
    };
    this._eventSendCreateAnnotation = () => {
      this.emitter.emit("_send_create_annotation", this._sendCreateAnnotation());
    };
    this._eventReceiveCreatedAnnotation = () => {
      this.emitter.emit("_receive_create_annotation", this._receiveCreatedAnnotation());
    };
    this._eventSendDeleteAnnotation = () => {
      this.emitter.emit("_send_delete_annotation", this._sendDeleteAnnotation());
    };
    this._eventReceiveDeleteAnnotation = () => {
      this.emitter.emit("_receive_delete_annotation", this._receiveDeleteAnnotation());
    };
    this._eventSendUpdateAnnotation = () => {
      this.emitter.emit("_send_update_Annotation", this._sendUpdateAnnotation());
    };
    this._eventReceiveUpdateAnnotation = () => {
      this.emitter.emit("_receive_update_Annotation", this._receiveUpdateAnnotation());
    };
    this.on = (event, handler) => {
      this.emitter.on(event, handler);
    };
    this._sendCreateAnnotation = () => {
    };
    this._receiveCreatedAnnotation = () => {
    };
    this._sendDeleteAnnotation = () => {
    };
    this._receiveDeleteAnnotation = () => {
    };
    this._selectAnnotation = (event) => {
      console.log(event);
      console.log(event.currentTarget);
      this.drawer._highlightAnnotation(null, null);
    };
    this._sendUpdateAnnotation = () => {
    };
    this._receiveUpdateAnnotation = () => {
    };
    this.drawer = new Draw();
    this.emitter = new import_tiny_emitter.TinyEmitter();
    this.websocket = new Websocket();
    this.on(onmouseup, this._eventSelectAnnotation);
    console.log("API Created");
  }
};
console.log("init -- Experimental - Annotation - API");
var API = new Experimental();
// Annotate the CommonJS export names for ESM import in node:
0 && (module.exports = {
  Experimental
});

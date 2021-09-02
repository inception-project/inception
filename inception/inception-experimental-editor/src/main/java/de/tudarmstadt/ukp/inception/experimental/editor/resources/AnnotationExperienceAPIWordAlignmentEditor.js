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
var __defProp = Object.defineProperty;
var __markAsModule = (target) => __defProp(target, "__esModule", {value: true});
var __export = (target, all) => {
  for (var name in all)
    __defProp(target, name, {get: all[name], enumerable: true});
};

// main/editors/wordalignment/AnnotationExperienceAPIWordAlignmentEditor.ts
__markAsModule(exports);
__export(exports, {
  AnnotationExperienceAPIWordAlignmentEditor: () => AnnotationExperienceAPIWordAlignmentEditor
});

// ../../../../inception-api-annotation-experimental/src/main/ts/node_modules/@stomp/stompjs/esm6/byte.js
var BYTE = {
  LF: "\n",
  NULL: "\0"
};

// ../../../../inception-api-annotation-experimental/src/main/ts/node_modules/@stomp/stompjs/esm6/frame-impl.js
var FrameImpl = class {
  constructor(params) {
    const {command, headers, body, binaryBody, escapeHeaderValues, skipContentLengthHeader} = params;
    this.command = command;
    this.headers = Object.assign({}, headers || {});
    if (binaryBody) {
      this._binaryBody = binaryBody;
      this.isBinaryBody = true;
    } else {
      this._body = body || "";
      this.isBinaryBody = false;
    }
    this.escapeHeaderValues = escapeHeaderValues || false;
    this.skipContentLengthHeader = skipContentLengthHeader || false;
  }
  get body() {
    if (!this._body && this.isBinaryBody) {
      this._body = new TextDecoder().decode(this._binaryBody);
    }
    return this._body;
  }
  get binaryBody() {
    if (!this._binaryBody && !this.isBinaryBody) {
      this._binaryBody = new TextEncoder().encode(this._body);
    }
    return this._binaryBody;
  }
  static fromRawFrame(rawFrame, escapeHeaderValues) {
    const headers = {};
    const trim = (str) => str.replace(/^\s+|\s+$/g, "");
    for (const header of rawFrame.headers.reverse()) {
      const idx = header.indexOf(":");
      const key = trim(header[0]);
      let value = trim(header[1]);
      if (escapeHeaderValues && rawFrame.command !== "CONNECT" && rawFrame.command !== "CONNECTED") {
        value = FrameImpl.hdrValueUnEscape(value);
      }
      headers[key] = value;
    }
    return new FrameImpl({
      command: rawFrame.command,
      headers,
      binaryBody: rawFrame.binaryBody,
      escapeHeaderValues
    });
  }
  toString() {
    return this.serializeCmdAndHeaders();
  }
  serialize() {
    const cmdAndHeaders = this.serializeCmdAndHeaders();
    if (this.isBinaryBody) {
      return FrameImpl.toUnit8Array(cmdAndHeaders, this._binaryBody).buffer;
    } else {
      return cmdAndHeaders + this._body + BYTE.NULL;
    }
  }
  serializeCmdAndHeaders() {
    const lines = [this.command];
    if (this.skipContentLengthHeader) {
      delete this.headers["content-length"];
    }
    for (const name of Object.keys(this.headers || {})) {
      const value = this.headers[name];
      if (this.escapeHeaderValues && this.command !== "CONNECT" && this.command !== "CONNECTED") {
        lines.push(`${name}:${FrameImpl.hdrValueEscape(`${value}`)}`);
      } else {
        lines.push(`${name}:${value}`);
      }
    }
    if (this.isBinaryBody || !this.isBodyEmpty() && !this.skipContentLengthHeader) {
      lines.push(`content-length:${this.bodyLength()}`);
    }
    return lines.join(BYTE.LF) + BYTE.LF + BYTE.LF;
  }
  isBodyEmpty() {
    return this.bodyLength() === 0;
  }
  bodyLength() {
    const binaryBody = this.binaryBody;
    return binaryBody ? binaryBody.length : 0;
  }
  static sizeOfUTF8(s) {
    return s ? new TextEncoder().encode(s).length : 0;
  }
  static toUnit8Array(cmdAndHeaders, binaryBody) {
    const uint8CmdAndHeaders = new TextEncoder().encode(cmdAndHeaders);
    const nullTerminator = new Uint8Array([0]);
    const uint8Frame = new Uint8Array(uint8CmdAndHeaders.length + binaryBody.length + nullTerminator.length);
    uint8Frame.set(uint8CmdAndHeaders);
    uint8Frame.set(binaryBody, uint8CmdAndHeaders.length);
    uint8Frame.set(nullTerminator, uint8CmdAndHeaders.length + binaryBody.length);
    return uint8Frame;
  }
  static marshall(params) {
    const frame = new FrameImpl(params);
    return frame.serialize();
  }
  static hdrValueEscape(str) {
    return str.replace(/\\/g, "\\\\").replace(/\r/g, "\\r").replace(/\n/g, "\\n").replace(/:/g, "\\c");
  }
  static hdrValueUnEscape(str) {
    return str.replace(/\\r/g, "\r").replace(/\\n/g, "\n").replace(/\\c/g, ":").replace(/\\\\/g, "\\");
  }
};

// ../../../../inception-api-annotation-experimental/src/main/ts/node_modules/@stomp/stompjs/esm6/parser.js
var NULL = 0;
var LF = 10;
var CR = 13;
var COLON = 58;
var Parser = class {
  constructor(onFrame, onIncomingPing) {
    this.onFrame = onFrame;
    this.onIncomingPing = onIncomingPing;
    this._encoder = new TextEncoder();
    this._decoder = new TextDecoder();
    this._token = [];
    this._initState();
  }
  parseChunk(segment, appendMissingNULLonIncoming = false) {
    let chunk;
    if (segment instanceof ArrayBuffer) {
      chunk = new Uint8Array(segment);
    } else {
      chunk = this._encoder.encode(segment);
    }
    if (appendMissingNULLonIncoming && chunk[chunk.length - 1] !== 0) {
      const chunkWithNull = new Uint8Array(chunk.length + 1);
      chunkWithNull.set(chunk, 0);
      chunkWithNull[chunk.length] = 0;
      chunk = chunkWithNull;
    }
    for (let i = 0; i < chunk.length; i++) {
      const byte = chunk[i];
      this._onByte(byte);
    }
  }
  _collectFrame(byte) {
    if (byte === NULL) {
      return;
    }
    if (byte === CR) {
      return;
    }
    if (byte === LF) {
      this.onIncomingPing();
      return;
    }
    this._onByte = this._collectCommand;
    this._reinjectByte(byte);
  }
  _collectCommand(byte) {
    if (byte === CR) {
      return;
    }
    if (byte === LF) {
      this._results.command = this._consumeTokenAsUTF8();
      this._onByte = this._collectHeaders;
      return;
    }
    this._consumeByte(byte);
  }
  _collectHeaders(byte) {
    if (byte === CR) {
      return;
    }
    if (byte === LF) {
      this._setupCollectBody();
      return;
    }
    this._onByte = this._collectHeaderKey;
    this._reinjectByte(byte);
  }
  _reinjectByte(byte) {
    this._onByte(byte);
  }
  _collectHeaderKey(byte) {
    if (byte === COLON) {
      this._headerKey = this._consumeTokenAsUTF8();
      this._onByte = this._collectHeaderValue;
      return;
    }
    this._consumeByte(byte);
  }
  _collectHeaderValue(byte) {
    if (byte === CR) {
      return;
    }
    if (byte === LF) {
      this._results.headers.push([this._headerKey, this._consumeTokenAsUTF8()]);
      this._headerKey = void 0;
      this._onByte = this._collectHeaders;
      return;
    }
    this._consumeByte(byte);
  }
  _setupCollectBody() {
    const contentLengthHeader = this._results.headers.filter((header) => {
      return header[0] === "content-length";
    })[0];
    if (contentLengthHeader) {
      this._bodyBytesRemaining = parseInt(contentLengthHeader[1], 10);
      this._onByte = this._collectBodyFixedSize;
    } else {
      this._onByte = this._collectBodyNullTerminated;
    }
  }
  _collectBodyNullTerminated(byte) {
    if (byte === NULL) {
      this._retrievedBody();
      return;
    }
    this._consumeByte(byte);
  }
  _collectBodyFixedSize(byte) {
    if (this._bodyBytesRemaining-- === 0) {
      this._retrievedBody();
      return;
    }
    this._consumeByte(byte);
  }
  _retrievedBody() {
    this._results.binaryBody = this._consumeTokenAsRaw();
    this.onFrame(this._results);
    this._initState();
  }
  _consumeByte(byte) {
    this._token.push(byte);
  }
  _consumeTokenAsUTF8() {
    return this._decoder.decode(this._consumeTokenAsRaw());
  }
  _consumeTokenAsRaw() {
    const rawResult = new Uint8Array(this._token);
    this._token = [];
    return rawResult;
  }
  _initState() {
    this._results = {
      command: void 0,
      headers: [],
      binaryBody: void 0
    };
    this._token = [];
    this._headerKey = void 0;
    this._onByte = this._collectFrame;
  }
};

// ../../../../inception-api-annotation-experimental/src/main/ts/node_modules/@stomp/stompjs/esm6/types.js
var StompSocketState;
(function(StompSocketState2) {
  StompSocketState2[StompSocketState2["CONNECTING"] = 0] = "CONNECTING";
  StompSocketState2[StompSocketState2["OPEN"] = 1] = "OPEN";
  StompSocketState2[StompSocketState2["CLOSING"] = 2] = "CLOSING";
  StompSocketState2[StompSocketState2["CLOSED"] = 3] = "CLOSED";
})(StompSocketState || (StompSocketState = {}));
var ActivationState;
(function(ActivationState2) {
  ActivationState2[ActivationState2["ACTIVE"] = 0] = "ACTIVE";
  ActivationState2[ActivationState2["DEACTIVATING"] = 1] = "DEACTIVATING";
  ActivationState2[ActivationState2["INACTIVE"] = 2] = "INACTIVE";
})(ActivationState || (ActivationState = {}));

// ../../../../inception-api-annotation-experimental/src/main/ts/node_modules/@stomp/stompjs/esm6/versions.js
var Versions = class {
  constructor(versions) {
    this.versions = versions;
  }
  supportedVersions() {
    return this.versions.join(",");
  }
  protocolVersions() {
    return this.versions.map((x) => `v${x.replace(".", "")}.stomp`);
  }
};
Versions.V1_0 = "1.0";
Versions.V1_1 = "1.1";
Versions.V1_2 = "1.2";
Versions.default = new Versions([
  Versions.V1_0,
  Versions.V1_1,
  Versions.V1_2
]);

// ../../../../inception-api-annotation-experimental/src/main/ts/node_modules/@stomp/stompjs/esm6/augment-websocket.js
function augmentWebsocket(webSocket, debug) {
  webSocket.terminate = function() {
    const noOp = () => {
    };
    this.onerror = noOp;
    this.onmessage = noOp;
    this.onopen = noOp;
    const ts = new Date();
    const origOnClose = this.onclose;
    this.onclose = (closeEvent) => {
      const delay = new Date().getTime() - ts.getTime();
      debug(`Discarded socket closed after ${delay}ms, with code/reason: ${closeEvent.code}/${closeEvent.reason}`);
    };
    this.close();
    origOnClose.call(this, {
      code: 4001,
      reason: "Heartbeat failure, discarding the socket",
      wasClean: false
    });
  };
}

// ../../../../inception-api-annotation-experimental/src/main/ts/node_modules/@stomp/stompjs/esm6/stomp-handler.js
var StompHandler = class {
  constructor(_client, _webSocket, config = {}) {
    this._client = _client;
    this._webSocket = _webSocket;
    this._serverFrameHandlers = {
      CONNECTED: (frame) => {
        this.debug(`connected to server ${frame.headers.server}`);
        this._connected = true;
        this._connectedVersion = frame.headers.version;
        if (this._connectedVersion === Versions.V1_2) {
          this._escapeHeaderValues = true;
        }
        this._setupHeartbeat(frame.headers);
        this.onConnect(frame);
      },
      MESSAGE: (frame) => {
        const subscription = frame.headers.subscription;
        const onReceive = this._subscriptions[subscription] || this.onUnhandledMessage;
        const message = frame;
        const client = this;
        const messageId = this._connectedVersion === Versions.V1_2 ? message.headers.ack : message.headers["message-id"];
        message.ack = (headers = {}) => {
          return client.ack(messageId, subscription, headers);
        };
        message.nack = (headers = {}) => {
          return client.nack(messageId, subscription, headers);
        };
        onReceive(message);
      },
      RECEIPT: (frame) => {
        const callback = this._receiptWatchers[frame.headers["receipt-id"]];
        if (callback) {
          callback(frame);
          delete this._receiptWatchers[frame.headers["receipt-id"]];
        } else {
          this.onUnhandledReceipt(frame);
        }
      },
      ERROR: (frame) => {
        this.onStompError(frame);
      }
    };
    this._counter = 0;
    this._subscriptions = {};
    this._receiptWatchers = {};
    this._partialData = "";
    this._escapeHeaderValues = false;
    this._lastServerActivityTS = Date.now();
    this.configure(config);
  }
  get connectedVersion() {
    return this._connectedVersion;
  }
  get connected() {
    return this._connected;
  }
  configure(conf) {
    Object.assign(this, conf);
  }
  start() {
    const parser = new Parser((rawFrame) => {
      const frame = FrameImpl.fromRawFrame(rawFrame, this._escapeHeaderValues);
      if (!this.logRawCommunication) {
        this.debug(`<<< ${frame}`);
      }
      const serverFrameHandler = this._serverFrameHandlers[frame.command] || this.onUnhandledFrame;
      serverFrameHandler(frame);
    }, () => {
      this.debug("<<< PONG");
    });
    this._webSocket.onmessage = (evt) => {
      this.debug("Received data");
      this._lastServerActivityTS = Date.now();
      if (this.logRawCommunication) {
        const rawChunkAsString = evt.data instanceof ArrayBuffer ? new TextDecoder().decode(evt.data) : evt.data;
        this.debug(`<<< ${rawChunkAsString}`);
      }
      parser.parseChunk(evt.data, this.appendMissingNULLonIncoming);
    };
    this._onclose = (closeEvent) => {
      this.debug(`Connection closed to ${this._client.brokerURL}`);
      this._cleanUp();
      this.onWebSocketClose(closeEvent);
    };
    this._webSocket.onclose = this._onclose;
    this._webSocket.onerror = (errorEvent) => {
      this.onWebSocketError(errorEvent);
    };
    this._webSocket.onopen = () => {
      const connectHeaders = Object.assign({}, this.connectHeaders);
      this.debug("Web Socket Opened...");
      connectHeaders["accept-version"] = this.stompVersions.supportedVersions();
      connectHeaders["heart-beat"] = [
        this.heartbeatOutgoing,
        this.heartbeatIncoming
      ].join(",");
      this._transmit({command: "CONNECT", headers: connectHeaders});
    };
  }
  _setupHeartbeat(headers) {
    if (headers.version !== Versions.V1_1 && headers.version !== Versions.V1_2) {
      return;
    }
    if (!headers["heart-beat"]) {
      return;
    }
    const [serverOutgoing, serverIncoming] = headers["heart-beat"].split(",").map((v) => parseInt(v, 10));
    if (this.heartbeatOutgoing !== 0 && serverIncoming !== 0) {
      const ttl = Math.max(this.heartbeatOutgoing, serverIncoming);
      this.debug(`send PING every ${ttl}ms`);
      this._pinger = setInterval(() => {
        if (this._webSocket.readyState === StompSocketState.OPEN) {
          this._webSocket.send(BYTE.LF);
          this.debug(">>> PING");
        }
      }, ttl);
    }
    if (this.heartbeatIncoming !== 0 && serverOutgoing !== 0) {
      const ttl = Math.max(this.heartbeatIncoming, serverOutgoing);
      this.debug(`check PONG every ${ttl}ms`);
      this._ponger = setInterval(() => {
        const delta = Date.now() - this._lastServerActivityTS;
        if (delta > ttl * 2) {
          this.debug(`did not receive server activity for the last ${delta}ms`);
          this._closeOrDiscardWebsocket();
        }
      }, ttl);
    }
  }
  _closeOrDiscardWebsocket() {
    if (this.discardWebsocketOnCommFailure) {
      this.debug("Discarding websocket, the underlying socket may linger for a while");
      this._discardWebsocket();
    } else {
      this.debug("Issuing close on the websocket");
      this._closeWebsocket();
    }
  }
  forceDisconnect() {
    if (this._webSocket) {
      if (this._webSocket.readyState === StompSocketState.CONNECTING || this._webSocket.readyState === StompSocketState.OPEN) {
        this._closeOrDiscardWebsocket();
      }
    }
  }
  _closeWebsocket() {
    this._webSocket.onmessage = () => {
    };
    this._webSocket.close();
  }
  _discardWebsocket() {
    if (!this._webSocket.terminate) {
      augmentWebsocket(this._webSocket, (msg) => this.debug(msg));
    }
    this._webSocket.terminate();
  }
  _transmit(params) {
    const {command, headers, body, binaryBody, skipContentLengthHeader} = params;
    const frame = new FrameImpl({
      command,
      headers,
      body,
      binaryBody,
      escapeHeaderValues: this._escapeHeaderValues,
      skipContentLengthHeader
    });
    let rawChunk = frame.serialize();
    if (this.logRawCommunication) {
      this.debug(`>>> ${rawChunk}`);
    } else {
      this.debug(`>>> ${frame}`);
    }
    if (this.forceBinaryWSFrames && typeof rawChunk === "string") {
      rawChunk = new TextEncoder().encode(rawChunk);
    }
    if (typeof rawChunk !== "string" || !this.splitLargeFrames) {
      this._webSocket.send(rawChunk);
    } else {
      let out = rawChunk;
      while (out.length > 0) {
        const chunk = out.substring(0, this.maxWebSocketChunkSize);
        out = out.substring(this.maxWebSocketChunkSize);
        this._webSocket.send(chunk);
        this.debug(`chunk sent = ${chunk.length}, remaining = ${out.length}`);
      }
    }
  }
  dispose() {
    if (this.connected) {
      try {
        const disconnectHeaders = Object.assign({}, this.disconnectHeaders);
        if (!disconnectHeaders.receipt) {
          disconnectHeaders.receipt = `close-${this._counter++}`;
        }
        this.watchForReceipt(disconnectHeaders.receipt, (frame) => {
          this._closeWebsocket();
          this._cleanUp();
          this.onDisconnect(frame);
        });
        this._transmit({command: "DISCONNECT", headers: disconnectHeaders});
      } catch (error) {
        this.debug(`Ignoring error during disconnect ${error}`);
      }
    } else {
      if (this._webSocket.readyState === StompSocketState.CONNECTING || this._webSocket.readyState === StompSocketState.OPEN) {
        this._closeWebsocket();
      }
    }
  }
  _cleanUp() {
    this._connected = false;
    if (this._pinger) {
      clearInterval(this._pinger);
    }
    if (this._ponger) {
      clearInterval(this._ponger);
    }
  }
  publish(params) {
    const {destination, headers, body, binaryBody, skipContentLengthHeader} = params;
    const hdrs = Object.assign({destination}, headers);
    this._transmit({
      command: "SEND",
      headers: hdrs,
      body,
      binaryBody,
      skipContentLengthHeader
    });
  }
  watchForReceipt(receiptId, callback) {
    this._receiptWatchers[receiptId] = callback;
  }
  subscribe(destination, callback, headers = {}) {
    headers = Object.assign({}, headers);
    if (!headers.id) {
      headers.id = `sub-${this._counter++}`;
    }
    headers.destination = destination;
    this._subscriptions[headers.id] = callback;
    this._transmit({command: "SUBSCRIBE", headers});
    const client = this;
    return {
      id: headers.id,
      unsubscribe(hdrs) {
        return client.unsubscribe(headers.id, hdrs);
      }
    };
  }
  unsubscribe(id, headers = {}) {
    headers = Object.assign({}, headers);
    delete this._subscriptions[id];
    headers.id = id;
    this._transmit({command: "UNSUBSCRIBE", headers});
  }
  begin(transactionId) {
    const txId = transactionId || `tx-${this._counter++}`;
    this._transmit({
      command: "BEGIN",
      headers: {
        transaction: txId
      }
    });
    const client = this;
    return {
      id: txId,
      commit() {
        client.commit(txId);
      },
      abort() {
        client.abort(txId);
      }
    };
  }
  commit(transactionId) {
    this._transmit({
      command: "COMMIT",
      headers: {
        transaction: transactionId
      }
    });
  }
  abort(transactionId) {
    this._transmit({
      command: "ABORT",
      headers: {
        transaction: transactionId
      }
    });
  }
  ack(messageId, subscriptionId, headers = {}) {
    headers = Object.assign({}, headers);
    if (this._connectedVersion === Versions.V1_2) {
      headers.id = messageId;
    } else {
      headers["message-id"] = messageId;
    }
    headers.subscription = subscriptionId;
    this._transmit({command: "ACK", headers});
  }
  nack(messageId, subscriptionId, headers = {}) {
    headers = Object.assign({}, headers);
    if (this._connectedVersion === Versions.V1_2) {
      headers.id = messageId;
    } else {
      headers["message-id"] = messageId;
    }
    headers.subscription = subscriptionId;
    return this._transmit({command: "NACK", headers});
  }
};

// ../../../../inception-api-annotation-experimental/src/main/ts/node_modules/@stomp/stompjs/esm6/client.js
var __awaiter = function(thisArg, _arguments, P, generator) {
  function adopt(value) {
    return value instanceof P ? value : new P(function(resolve) {
      resolve(value);
    });
  }
  return new (P || (P = Promise))(function(resolve, reject) {
    function fulfilled(value) {
      try {
        step(generator.next(value));
      } catch (e) {
        reject(e);
      }
    }
    function rejected(value) {
      try {
        step(generator["throw"](value));
      } catch (e) {
        reject(e);
      }
    }
    function step(result) {
      result.done ? resolve(result.value) : adopt(result.value).then(fulfilled, rejected);
    }
    step((generator = generator.apply(thisArg, _arguments || [])).next());
  });
};
var Client = class {
  constructor(conf = {}) {
    this.stompVersions = Versions.default;
    this.connectionTimeout = 0;
    this.reconnectDelay = 5e3;
    this.heartbeatIncoming = 1e4;
    this.heartbeatOutgoing = 1e4;
    this.splitLargeFrames = false;
    this.maxWebSocketChunkSize = 8 * 1024;
    this.forceBinaryWSFrames = false;
    this.appendMissingNULLonIncoming = false;
    this.state = ActivationState.INACTIVE;
    const noOp = () => {
    };
    this.debug = noOp;
    this.beforeConnect = noOp;
    this.onConnect = noOp;
    this.onDisconnect = noOp;
    this.onUnhandledMessage = noOp;
    this.onUnhandledReceipt = noOp;
    this.onUnhandledFrame = noOp;
    this.onStompError = noOp;
    this.onWebSocketClose = noOp;
    this.onWebSocketError = noOp;
    this.logRawCommunication = false;
    this.onChangeState = noOp;
    this.connectHeaders = {};
    this._disconnectHeaders = {};
    this.configure(conf);
  }
  get webSocket() {
    return this._stompHandler ? this._stompHandler._webSocket : void 0;
  }
  get disconnectHeaders() {
    return this._disconnectHeaders;
  }
  set disconnectHeaders(value) {
    this._disconnectHeaders = value;
    if (this._stompHandler) {
      this._stompHandler.disconnectHeaders = this._disconnectHeaders;
    }
  }
  get connected() {
    return !!this._stompHandler && this._stompHandler.connected;
  }
  get connectedVersion() {
    return this._stompHandler ? this._stompHandler.connectedVersion : void 0;
  }
  get active() {
    return this.state === ActivationState.ACTIVE;
  }
  _changeState(state) {
    this.state = state;
    this.onChangeState(state);
  }
  configure(conf) {
    Object.assign(this, conf);
  }
  activate() {
    if (this.state === ActivationState.DEACTIVATING) {
      this.debug("Still DEACTIVATING, please await call to deactivate before trying to re-activate");
      throw new Error("Still DEACTIVATING, can not activate now");
    }
    if (this.active) {
      this.debug("Already ACTIVE, ignoring request to activate");
      return;
    }
    this._changeState(ActivationState.ACTIVE);
    this._connect();
  }
  _connect() {
    return __awaiter(this, void 0, void 0, function* () {
      if (this.connected) {
        this.debug("STOMP: already connected, nothing to do");
        return;
      }
      yield this.beforeConnect();
      if (!this.active) {
        this.debug("Client has been marked inactive, will not attempt to connect");
        return;
      }
      if (this.connectionTimeout > 0) {
        if (this._connectionWatcher) {
          clearTimeout(this._connectionWatcher);
        }
        this._connectionWatcher = setTimeout(() => {
          if (this.connected) {
            return;
          }
          this.debug(`Connection not established in ${this.connectionTimeout}ms, closing socket`);
          this.forceDisconnect();
        }, this.connectionTimeout);
      }
      this.debug("Opening Web Socket...");
      const webSocket = this._createWebSocket();
      this._stompHandler = new StompHandler(this, webSocket, {
        debug: this.debug,
        stompVersions: this.stompVersions,
        connectHeaders: this.connectHeaders,
        disconnectHeaders: this._disconnectHeaders,
        heartbeatIncoming: this.heartbeatIncoming,
        heartbeatOutgoing: this.heartbeatOutgoing,
        splitLargeFrames: this.splitLargeFrames,
        maxWebSocketChunkSize: this.maxWebSocketChunkSize,
        forceBinaryWSFrames: this.forceBinaryWSFrames,
        logRawCommunication: this.logRawCommunication,
        appendMissingNULLonIncoming: this.appendMissingNULLonIncoming,
        discardWebsocketOnCommFailure: this.discardWebsocketOnCommFailure,
        onConnect: (frame) => {
          if (this._connectionWatcher) {
            clearTimeout(this._connectionWatcher);
            this._connectionWatcher = void 0;
          }
          if (!this.active) {
            this.debug("STOMP got connected while deactivate was issued, will disconnect now");
            this._disposeStompHandler();
            return;
          }
          this.onConnect(frame);
        },
        onDisconnect: (frame) => {
          this.onDisconnect(frame);
        },
        onStompError: (frame) => {
          this.onStompError(frame);
        },
        onWebSocketClose: (evt) => {
          this._stompHandler = void 0;
          if (this.state === ActivationState.DEACTIVATING) {
            this._resolveSocketClose();
            this._resolveSocketClose = void 0;
            this._changeState(ActivationState.INACTIVE);
          }
          this.onWebSocketClose(evt);
          if (this.active) {
            this._schedule_reconnect();
          }
        },
        onWebSocketError: (evt) => {
          this.onWebSocketError(evt);
        },
        onUnhandledMessage: (message) => {
          this.onUnhandledMessage(message);
        },
        onUnhandledReceipt: (frame) => {
          this.onUnhandledReceipt(frame);
        },
        onUnhandledFrame: (frame) => {
          this.onUnhandledFrame(frame);
        }
      });
      this._stompHandler.start();
    });
  }
  _createWebSocket() {
    let webSocket;
    if (this.webSocketFactory) {
      webSocket = this.webSocketFactory();
    } else {
      webSocket = new WebSocket(this.brokerURL, this.stompVersions.protocolVersions());
    }
    webSocket.binaryType = "arraybuffer";
    return webSocket;
  }
  _schedule_reconnect() {
    if (this.reconnectDelay > 0) {
      this.debug(`STOMP: scheduling reconnection in ${this.reconnectDelay}ms`);
      this._reconnector = setTimeout(() => {
        this._connect();
      }, this.reconnectDelay);
    }
  }
  deactivate() {
    return __awaiter(this, void 0, void 0, function* () {
      let retPromise;
      if (this.state !== ActivationState.ACTIVE) {
        this.debug(`Already ${ActivationState[this.state]}, ignoring call to deactivate`);
        return Promise.resolve();
      }
      this._changeState(ActivationState.DEACTIVATING);
      if (this._reconnector) {
        clearTimeout(this._reconnector);
      }
      if (this._stompHandler && this.webSocket.readyState !== StompSocketState.CLOSED) {
        retPromise = new Promise((resolve, reject) => {
          this._resolveSocketClose = resolve;
        });
      } else {
        this._changeState(ActivationState.INACTIVE);
        return Promise.resolve();
      }
      this._disposeStompHandler();
      return retPromise;
    });
  }
  forceDisconnect() {
    if (this._stompHandler) {
      this._stompHandler.forceDisconnect();
    }
  }
  _disposeStompHandler() {
    if (this._stompHandler) {
      this._stompHandler.dispose();
      this._stompHandler = null;
    }
  }
  publish(params) {
    this._stompHandler.publish(params);
  }
  watchForReceipt(receiptId, callback) {
    this._stompHandler.watchForReceipt(receiptId, callback);
  }
  subscribe(destination, callback, headers = {}) {
    return this._stompHandler.subscribe(destination, callback, headers);
  }
  unsubscribe(id, headers = {}) {
    this._stompHandler.unsubscribe(id, headers);
  }
  begin(transactionId) {
    return this._stompHandler.begin(transactionId);
  }
  commit(transactionId) {
    this._stompHandler.commit(transactionId);
  }
  abort(transactionId) {
    this._stompHandler.abort(transactionId);
  }
  ack(messageId, subscriptionId, headers = {}) {
    this._stompHandler.ack(messageId, subscriptionId, headers);
  }
  nack(messageId, subscriptionId, headers = {}) {
    this._stompHandler.nack(messageId, subscriptionId, headers);
  }
};

// ../../../../inception-api-annotation-experimental/src/main/ts/node_modules/@stomp/stompjs/esm6/compatibility/heartbeat-info.js
var HeartbeatInfo = class {
  constructor(client) {
    this.client = client;
  }
  get outgoing() {
    return this.client.heartbeatOutgoing;
  }
  set outgoing(value) {
    this.client.heartbeatOutgoing = value;
  }
  get incoming() {
    return this.client.heartbeatIncoming;
  }
  set incoming(value) {
    this.client.heartbeatIncoming = value;
  }
};

// ../../../../inception-api-annotation-experimental/src/main/ts/node_modules/@stomp/stompjs/esm6/compatibility/compat-client.js
var CompatClient = class extends Client {
  constructor(webSocketFactory) {
    super();
    this.maxWebSocketFrameSize = 16 * 1024;
    this._heartbeatInfo = new HeartbeatInfo(this);
    this.reconnect_delay = 0;
    this.webSocketFactory = webSocketFactory;
    this.debug = (...message) => {
      console.log(...message);
    };
  }
  _parseConnect(...args) {
    let closeEventCallback;
    let connectCallback;
    let errorCallback;
    let headers = {};
    if (args.length < 2) {
      throw new Error("Connect requires at least 2 arguments");
    }
    if (typeof args[1] === "function") {
      [headers, connectCallback, errorCallback, closeEventCallback] = args;
    } else {
      switch (args.length) {
        case 6:
          [
            headers.login,
            headers.passcode,
            connectCallback,
            errorCallback,
            closeEventCallback,
            headers.host
          ] = args;
          break;
        default:
          [
            headers.login,
            headers.passcode,
            connectCallback,
            errorCallback,
            closeEventCallback
          ] = args;
      }
    }
    return [headers, connectCallback, errorCallback, closeEventCallback];
  }
  connect(...args) {
    const out = this._parseConnect(...args);
    if (out[0]) {
      this.connectHeaders = out[0];
    }
    if (out[1]) {
      this.onConnect = out[1];
    }
    if (out[2]) {
      this.onStompError = out[2];
    }
    if (out[3]) {
      this.onWebSocketClose = out[3];
    }
    super.activate();
  }
  disconnect(disconnectCallback, headers = {}) {
    if (disconnectCallback) {
      this.onDisconnect = disconnectCallback;
    }
    this.disconnectHeaders = headers;
    super.deactivate();
  }
  send(destination, headers = {}, body = "") {
    headers = Object.assign({}, headers);
    const skipContentLengthHeader = headers["content-length"] === false;
    if (skipContentLengthHeader) {
      delete headers["content-length"];
    }
    this.publish({
      destination,
      headers,
      body,
      skipContentLengthHeader
    });
  }
  set reconnect_delay(value) {
    this.reconnectDelay = value;
  }
  get ws() {
    return this.webSocket;
  }
  get version() {
    return this.connectedVersion;
  }
  get onreceive() {
    return this.onUnhandledMessage;
  }
  set onreceive(value) {
    this.onUnhandledMessage = value;
  }
  get onreceipt() {
    return this.onUnhandledReceipt;
  }
  set onreceipt(value) {
    this.onUnhandledReceipt = value;
  }
  get heartbeat() {
    return this._heartbeatInfo;
  }
  set heartbeat(value) {
    this.heartbeatIncoming = value.incoming;
    this.heartbeatOutgoing = value.outgoing;
  }
};

// ../../../../inception-api-annotation-experimental/src/main/ts/node_modules/@stomp/stompjs/esm6/compatibility/stomp.js
var Stomp = class {
  static client(url, protocols) {
    if (protocols == null) {
      protocols = Versions.default.protocolVersions();
    }
    const wsFn = () => {
      const klass = Stomp.WebSocketClass || WebSocket;
      return new klass(url, protocols);
    };
    return new CompatClient(wsFn);
  }
  static over(ws) {
    let wsFn;
    if (typeof ws === "function") {
      wsFn = ws;
    } else {
      console.warn("Stomp.over did not receive a factory, auto reconnect will not work. Please see https://stomp-js.github.io/api-docs/latest/classes/Stomp.html#over");
      wsFn = () => ws;
    }
    return new CompatClient(wsFn);
  }
};
Stomp.WebSocketClass = null;

// ../../../../inception-api-annotation-experimental/src/main/ts/main/client/model/Span.ts
var Span = class {
  constructor(aId, aCoveredText, aBegin, aEnd, aLayerId, aFeatures, aColor) {
    this.id = aId;
    this.coveredText = aCoveredText;
    this.begin = aBegin;
    this.end = aEnd;
    this.layerId = aLayerId;
    this.features = aFeatures;
    this.color = aColor;
  }
};

// ../../../../inception-api-annotation-experimental/src/main/ts/main/client/messages/response/AdviceMessage.ts
var AdviceMessage = class {
};

// ../../../../inception-api-annotation-experimental/src/main/ts/main/client/messages/response/DocumentMessage.ts
var DocumentMessage = class {
};

// ../../../../inception-api-annotation-experimental/src/main/ts/main/client/messages/request/DocumentRequest.ts
var DocumentRequest = class {
  constructor(aAnnotatorName, aProjectId, aSourceDocumentId, aViewport) {
    this.annotatorName = aAnnotatorName;
    this.projectId = aProjectId;
    this.sourceDocumentId = aSourceDocumentId;
    this.viewport = aViewport;
  }
};

// ../../../../inception-api-annotation-experimental/src/main/ts/main/client/messages/response/UpdateFeaturesMessage.ts
var UpdateFeaturesMessage = class {
};

// ../../../../inception-api-annotation-experimental/src/main/ts/main/client/messages/response/create/SpanCreatedMessage.ts
var SpanCreatedMessage = class {
};

// ../../../../inception-api-annotation-experimental/src/main/ts/main/client/messages/response/DeleteAnnotationMessage.ts
var DeleteAnnotationMessage = class {
};

// ../../../../inception-api-annotation-experimental/src/main/ts/main/client/messages/response/create/ArcCreatedMessage.ts
var ArcCreatedMessage = class {
};

// ../../../../inception-api-annotation-experimental/src/main/ts/main/client/messages/request/create/CreateArcRequest.ts
var CreateArcRequest = class {
  constructor(aAnnotatorName, aProjectId, aSourceDocumentId, aSourceId, aTargetId, aLayerId) {
    this.annotatorName = aAnnotatorName;
    this.projectId = aProjectId;
    this.sourceDocumentId = aSourceDocumentId;
    this.sourceId = aSourceId;
    this.targetId = aTargetId;
    this.layerId = aLayerId;
  }
};

// ../../../../inception-api-annotation-experimental/src/main/ts/main/client/messages/request/create/CreateSpanRequest.ts
var CreateSpanRequest = class {
  constructor(aAnnotatorName, aProjectId, aSourceDocumentId, aBegin, aEnd, aLayerId) {
    this.annotatorName = aAnnotatorName;
    this.projectId = aProjectId;
    this.sourceDocumentId = aSourceDocumentId;
    this.begin = aBegin;
    this.end = aEnd;
    this.layerId = aLayerId;
  }
};

// ../../../../inception-api-annotation-experimental/src/main/ts/main/client/messages/request/DeleteAnnotationRequest.ts
var DeleteAnnotationRequest = class {
  constructor(aAnnotatorName, aProjectId, aSourceDocumentId, aAnnotationId, aLayerId) {
    this.annotatorName = aAnnotatorName;
    this.projectId = aProjectId;
    this.sourceDocumentId = aSourceDocumentId;
    this.annotationId = aAnnotationId;
    this.layerId = aLayerId;
  }
};

// ../../../../inception-api-annotation-experimental/src/main/ts/main/client/messages/request/UpdateFeaturesRequest.ts
var UpdateFeaturesRequest = class {
  constructor(aAnnotatorName, aProjectId, aSourceDocumentId, aAnnotationId, aNewFeatures) {
    this.annotatorName = aAnnotatorName;
    this.projectId = aProjectId;
    this.sourceDocumentId = aSourceDocumentId;
    this.annotationId = aAnnotationId;
    this.newFeatures = aNewFeatures;
  }
};

// ../../../../inception-api-annotation-experimental/src/main/ts/main/client/AnnotationExperienceAPIImpl.ts
var AnnotationExperienceAPIImpl = class {
  constructor(aProjectId, aDocumentId, aClientName, aUrl) {
    alert("CREATED");
    this.connect(aProjectId, aDocumentId, aClientName, aUrl);
  }
  connect(aProjectId, aDocumentId, aClientName, aUrl) {
    alert("OPENING NOW WEBSOCKET VIA " + aUrl);
    this.stompClient = Stomp.over(function() {
      return new WebSocket(aUrl);
    });
    const that = this;
    this.stompClient.onConnect = function(frame) {
      that.onConnect(frame, aClientName, aProjectId, aDocumentId);
    };
    this.stompClient.onStompError = function(frame) {
      console.log("Broker reported error: " + frame.headers["message"]);
      console.log("Additional details: " + frame.body);
    };
  }
  onConnect(frame, aClientName, aProjectId, aDocumentId) {
    let that = this;
    alert("onCON");
    this.stompClient.subscribe("/queue/document/" + aClientName, function(msg) {
      that.onDocument(Object.assign(new DocumentMessage(), JSON.parse(msg.body)));
    }, {id: "document_request"});
    this.stompClient.subscribe("/queue/error_message/" + aClientName, function(msg) {
      that.onError(Object.assign(new AdviceMessage(), JSON.parse(msg.body)));
    }, {id: "error_message"});
    this.stompClient.subscribe("/topic/features_update/" + aProjectId + "/" + aDocumentId, function(msg) {
      that.onFeaturesUpdate(Object.assign(new UpdateFeaturesMessage(), JSON.parse(msg.body)));
    }, {id: "span_update"});
    this.stompClient.subscribe("/topic/span_create/" + aProjectId + "/" + aDocumentId, function(msg) {
      that.onSpanCreate(Object.assign(new SpanCreatedMessage(), JSON.parse(msg.body)));
    }, {id: "span_create"});
    this.stompClient.subscribe("/topic/arc_create/" + aProjectId + "/" + aDocumentId, function(msg) {
      that.onArcCreate(Object.assign(new ArcCreatedMessage(), JSON.parse(msg.body)));
    }, {id: "relation_create"});
    this.stompClient.subscribe("/topic/annotation_delete/" + aProjectId + "/" + aDocumentId, function(msg) {
      that.onAnnotationDelete(Object.assign(new DeleteAnnotationMessage(), JSON.parse(msg.body)));
    }, {id: "span_delete"});
    this.stompClient.activate();
    alert("ACTIVATE");
  }
  unsubscribe(aChannel) {
    this.stompClient.unsubscribe(aChannel);
  }
  disconnect() {
    this.stompClient.deactivate();
  }
  requestDocument(aAnnotatorName, aProjectId, aDocumentId, aViewport) {
    this.stompClient.publish({
      destination: "/app/document_request",
      body: JSON.stringify(new DocumentRequest(aAnnotatorName, aProjectId, aDocumentId, aViewport))
    });
  }
  requestCreateArc(aAnnotatorName, aProjectId, aDocumentId, aSourceId, aTargetId, aLayer) {
    this.stompClient.publish({
      destination: "/app/arc_create",
      body: JSON.stringify(new CreateArcRequest(aAnnotatorName, aProjectId, aDocumentId, aSourceId, aTargetId, aLayer))
    });
  }
  requestCreateSpan(aAnnotatorName, aProjectId, aDocumentId, aBegin, aEnd, aLayer) {
    console.log("SENDING: " + aBegin + " _ " + aEnd);
    this.stompClient.publish({
      destination: "/app/span_create",
      body: JSON.stringify(new CreateSpanRequest(aAnnotatorName, aProjectId, aDocumentId, aBegin, aEnd, aLayer))
    });
  }
  requestDeleteAnnotation(aAnnotatorName, aProjectId, aDocumentId, aAnnotationId, aLayer) {
    this.stompClient.publish({
      destination: "/app/annotation_delete",
      body: JSON.stringify(new DeleteAnnotationRequest(aAnnotatorName, aProjectId, aDocumentId, aAnnotationId, aLayer))
    });
  }
  requestUpdateFeatures(aAnnotatorName, aProjectId, aDocumentId, aAnnotationId, aNewFeature) {
    this.stompClient.publish({
      destination: "/app/features_update",
      body: JSON.stringify(new UpdateFeaturesRequest(aAnnotatorName, aProjectId, aDocumentId, aAnnotationId, aNewFeature))
    });
  }
  onDocument(aMessage) {
    console.log("RECEIVED DOCUMENT" + aMessage);
  }
  onSpanCreate(aMessage) {
    console.log("RECEIVED SPAN CREATE" + aMessage);
    let span = new Span(aMessage.spanId, aMessage.coveredText, aMessage.begin, aMessage.end, aMessage.type, aMessage.features, aMessage.color);
  }
  onError(aMessage) {
    console.log(aMessage);
  }
  onArcCreate(aMessage) {
    console.log("RECEIVED ARC CREATE" + aMessage);
  }
  onAnnotationDelete(aMessage) {
    console.log("RECEIVED DELETE ANNOTATION" + aMessage);
  }
  onFeaturesUpdate(aMessage) {
    console.log("RECEIVED UPDATE ANNOTATION" + aMessage);
  }
};

// main/editors/wordalignment/visualization/AnnotationExperienceAPIWordAlignmentEditorVisualization.ts
var AnnotationExperienceAPIWordAlignmentEditorVisualization = class {
  constructor(aAnnotationExperienceAPIWordAlignmentEditor) {
    this.oddLanguage = "English";
    this.evenLanguage = "German";
    this.headerOffset = 75;
    this.annotationExperienceAPIWordAlignmentEditor = aAnnotationExperienceAPIWordAlignmentEditor;
  }
  showText(aElementId) {
    let words, initalOffset;
    let container = document.getElementById(aElementId.toString());
    container.innerHTML = "";
    let language = document.createElement("b");
    if (aElementId === "odd_unit_container") {
      language.innerText = this.oddLanguage;
      words = this.annotationExperienceAPIWordAlignmentEditor.oddSentence.split(" ");
      initalOffset = this.calculateInitialOffset(this.annotationExperienceAPIWordAlignmentEditor.oddSentenceOffset);
    } else {
      language.innerText = this.evenLanguage;
      words = this.annotationExperienceAPIWordAlignmentEditor.evenSentence.split(" ");
      initalOffset = this.calculateInitialOffset(this.annotationExperienceAPIWordAlignmentEditor.evenSentenceOffset);
    }
    container.setAttribute("style", "float:left; width: 200px");
    container.appendChild(language);
    container.appendChild(document.createElement("hr"));
    let svg = document.getElementById("svg");
    let heightOriginal = document.getElementById("odd_unit_container").offsetHeight;
    let heightTranslation = document.getElementById("even_unit_container").offsetHeight;
    if (heightOriginal > heightTranslation) {
      svg.setAttribute("height", String(heightOriginal));
    } else {
      svg.setAttribute("height", String(heightTranslation));
    }
    let begin, end;
    for (let i = 0; i < words.length; i++) {
      let wordDIV = document.createElement("div");
      wordDIV.className = "form-group";
      wordDIV.style.height = "40px";
      if (i === 0) {
        begin = initalOffset;
      } else {
        begin = end + 1;
      }
      end = begin + words[i].length;
      let wordLABEL = document.createElement("label");
      wordLABEL.setAttribute("style", "text-align: right");
      wordLABEL.id = "offset_" + begin + "_" + end;
      wordLABEL.innerText = words[i];
      let wordINPUT = document.createElement("input");
      wordINPUT.setAttribute("size", "1");
      wordINPUT.setAttribute("maxlength", "2");
      if (aElementId === "odd_unit_container") {
        wordINPUT.setAttribute("style", "float:left; text-align: center");
        wordINPUT.id = "original_word_" + i;
        wordINPUT.value = String(i);
        wordINPUT.disabled = true;
        wordDIV.appendChild(wordINPUT);
        wordDIV.appendChild(wordLABEL);
      } else {
        wordINPUT.setAttribute("style", "float:right; text-align: center");
        wordINPUT.id = "translated_word_" + i;
        wordDIV.appendChild(wordLABEL);
        wordDIV.appendChild(wordINPUT);
      }
      container.appendChild(wordDIV);
    }
    this.drawLines();
  }
  drawLines() {
    let that = this;
    let svg = document.getElementById("svg");
    svg.innerHTML = "";
    let arcs = that.annotationExperienceAPIWordAlignmentEditor.annotationExperienceAPI.arcs;
    let spans = that.annotationExperienceAPIWordAlignmentEditor.annotationExperienceAPI.spans;
    for (let i = 0; i < arcs.length; i++) {
      let yGovernor = null;
      let govID = null;
      let yDependent = null;
      let depID = null;
      for (let j = 0; j < spans.length; j++) {
        if (spans[j].id == arcs[i].sourceId) {
          for (let k = 0; k < document.getElementById("odd_unit_container").children.length - 2; k++) {
            if (document.getElementById("odd_unit_container").children[k + 2].children[1].id.split("_")[1] == spans[j].begin) {
              yGovernor = this.headerOffset + k * 48;
              govID = spans[j].id;
            }
          }
        }
        if (spans[j].id == arcs[i].targetId) {
          for (let k = 0; k < document.getElementById("even_unit_container").children.length - 2; k++) {
            if (document.getElementById("even_unit_container").children[k + 2].children[0].id.split("_")[1] == spans[j].begin) {
              yDependent = this.headerOffset + k * 48;
              depID = spans[j].id;
            }
          }
        }
        if (yGovernor != null && yDependent != null) {
          let line = document.createElementNS("http://www.w3.org/2000/svg", "line");
          line.setAttribute("x1", "5");
          line.setAttribute("y1", yGovernor.toString());
          line.setAttribute("x2", "95");
          line.setAttribute("y2", yDependent.toString());
          line.setAttribute("gov_id", govID.toString());
          line.setAttribute("dep_id", depID.toString());
          if (arcs.length > 0) {
            line.style.stroke = "#9a001b";
          }
          line.style.strokeWidth = "2";
          svg.appendChild(line);
          break;
        }
      }
    }
  }
  calculateInitialOffset(aUnitNumber) {
    let offset = 0;
    let units = this.annotationExperienceAPIWordAlignmentEditor.annotationExperienceAPI.viewport.documentText.split(".");
    for (let i = 0; i < aUnitNumber; i++) {
      let words = units[i].split(" ");
      for (let j = 0; j < words.length; j++) {
        offset += words[j].length + 1;
      }
      offset++;
    }
    return offset;
  }
  showDependencies() {
    let spans = this.annotationExperienceAPIWordAlignmentEditor.annotationExperienceAPI.spans;
    for (let i = 0; i < document.getElementById("odd_unit_container").children.length - 2; i++) {
      for (let j = 0; j < spans.length; j++) {
        if (spans[j].begin === Number(document.getElementById("odd_unit_container").children[i + 2].children[1].id.split("_")[2]) && spans[j].end === Number(document.getElementById("odd_unit_container").children[i + 2].children[1].id.split("_")[3])) {
          document.getElementById("odd_unit_container").children[i + 2].children[1].setAttribute("style", "border = 2px solid " + spans[i].color);
          break;
        }
      }
    }
    for (let i = 0; i < document.getElementById("even_unit_container").children.length - 2; i++) {
      for (let j = 0; j < spans.length; j++) {
        if (spans[j].begin === Number(document.getElementById("even_unit_container").children[i + 2].children[0].id.split("_")[2]) && spans[j].end === Number(document.getElementById("even_unit_container").children[i + 2].children[0].id.split("_")[3])) {
          document.getElementById("even_unit_container").children[i + 2].children[1].style = "border = 2px solid " + spans[i].color;
          break;
        }
      }
    }
  }
  refreshEditor() {
    document.getElementById("svg").innerHTML = "";
    this.showText("odd_unit_container");
    this.showText("even_unit_container");
  }
};

// main/editors/wordalignment/action/AnnotationExperienceAPIWordAlignmentEditorActionHandler.ts
var AnnotationExperienceAPIWordAlignmentEditorActionHandler = class {
  constructor(aAnnotationExperienceAPIWordAlignmentEditor) {
    this.annotationExperienceAPIWordAlignmentEditor = aAnnotationExperienceAPIWordAlignmentEditor;
    this.registerDefaultActionHandler();
  }
  registerDefaultActionHandler() {
    let that = this;
    onclick = function(aEvent) {
      let elem = aEvent.target;
      if (elem.className === "far fa-caret-square-right" || "far fa-caret-square-left") {
        setTimeout(function() {
          that.annotationExperienceAPIWordAlignmentEditor.annotationExperienceAPI.requestDocument(that.annotationExperienceAPIWordAlignmentEditor.annotatorName, that.annotationExperienceAPIWordAlignmentEditor.projectId, that.annotationExperienceAPIWordAlignmentEditor.documentId, that.annotationExperienceAPIWordAlignmentEditor.viewport);
          setTimeout(function() {
            that.annotationExperienceAPIWordAlignmentEditor.annotationExperienceAPIVisualization.refreshEditor();
          }, 2e3);
        }, 200);
        document.getElementById("save_alignment").disabled = false;
      }
      if (elem.className === "fas fa-step-forward" || elem.className === "fas fa-step-backward") {
        setTimeout(function() {
          let offset = Number(document.getElementsByTagName("input")[2].value);
          that.annotationExperienceAPIWordAlignmentEditor.oddSentence = that.sentences[offset - 1];
          that.annotationExperienceAPIWordAlignmentEditor.evenSentence = that.sentences[offset];
          that.annotationExperienceAPIWordAlignmentEditor.oddSentenceOffset = offset - 1;
          that.annotationExperienceAPIWordAlignmentEditor.evenSentenceOffset = offset;
          that.annotationExperienceAPIWordAlignmentEditor.annotationExperienceAPIVisualization.refreshEditor();
          document.getElementById("save_alignment").disabled = false;
        }, 200);
      }
      if (elem.id === "delete_alignment") {
        that.annotationExperienceAPIWordAlignmentEditor.resetAlignments();
      }
      if (elem.id === "save_alignment") {
        that.annotationExperienceAPIWordAlignmentEditor.saveAlignments();
      }
      if (elem.id === "show_relations") {
        that.annotationExperienceAPIWordAlignmentEditor.annotationExperienceAPIVisualization.drawLines();
      }
    };
  }
};

// main/editors/wordalignment/AnnotationExperienceAPIWordAlignmentEditor.ts
var AnnotationExperienceAPIWordAlignmentEditor = class {
  constructor(aProjectId, aDocumentId, aAnnotatorName, aUrl) {
    this.oddSentenceOffset = 0;
    this.evenSentenceOffset = 1;
    alert("RUNNING");
    this.projectId = aProjectId;
    this.documentId = aDocumentId;
    this.annotatorName = aAnnotatorName;
    this.annotationExperienceAPI = new AnnotationExperienceAPIImpl(aProjectId, aDocumentId, aAnnotatorName, aUrl);
    this.annotationExperienceAPIVisualization = new AnnotationExperienceAPIWordAlignmentEditorVisualization(this);
    this.annotationExperienceAPIWordAlignmentEditorActionHandler = new AnnotationExperienceAPIWordAlignmentEditorActionHandler(this);
  }
  saveAlignments() {
    let pairs = [];
    const that = this;
    if (!this.inputsValid) {
      alert("Word alignment is not 1:1.");
      return;
    }
    this.spanType = document.getElementsByClassName("filter-option-inner-inner")[0].innerText;
    let oddUnitContainerSize = document.getElementById("odd_unit_container").children.length - 2;
    let evenUnitContainerSize = document.getElementById("even_unit_container").children.length - 2;
    for (let i = 0; i < oddUnitContainerSize; i++) {
      for (let j = 0; j < evenUnitContainerSize; j++) {
        let oddUnitContainerElementInputValue = document.getElementById("odd_unit_container").children[i + 2].children[0].value;
        let evenUnitContainerElementInputValue = document.getElementById("even_unit_container").children[j + 2].children[1].value;
        if (oddUnitContainerElementInputValue === evenUnitContainerElementInputValue) {
          let oddUnitContainerElementText = document.getElementById("odd_unit_container").children[i + 2].children[1];
          let evenUnitContainerElementText = document.getElementById("even_unit_container").children[j + 2].children[0];
          let oddUnitContainerElementTextId = oddUnitContainerElementText.id.split("_");
          let evenUnitContainerElementTextId = evenUnitContainerElementText.id.split("_");
          pairs.push([
            oddUnitContainerElementTextId[1],
            evenUnitContainerElementTextId[1]
          ]);
          this.createSpanRequest(oddUnitContainerElementTextId[1], oddUnitContainerElementTextId[2], null);
          this.createSpanRequest(evenUnitContainerElementTextId[1], evenUnitContainerElementTextId[2], null);
        }
      }
    }
    setTimeout(function() {
      let source, target;
      for (let i = 0; i < pairs.length; i++) {
        for (let j = 0; j < that.spans.length; j++) {
          if (pairs[i][0] == that.spans[j].begin) {
            source = that.spans[j];
          }
          if (pairs[i][1] == that.spans[j].begin) {
            target = that.spans[j];
          }
        }
        that.annotationExperienceAPI.requestCreateArc(that.annotatorName, that.projectId, that.documentId, source.id, target.id, null);
      }
    }, 1500);
    document.getElementById("save_alignment").disabled = true;
  }
  inputsValid() {
    let values = [];
    if (!document.getElementById("multipleSelect").checked) {
      for (let i = 0; i < document.getElementById("even_unit_container").children.length - 2; i++) {
        if (values.indexOf(document.getElementById("even_unit_container").children[i + 2].children[1].value) > -1) {
          return false;
        }
        values.push(document.getElementById("even_unit_container").children[i + 2].children[1].value);
      }
    }
    return true;
  }
  createSpanRequest(aBegin, aEnd, aLayer) {
    let that = this;
    this.annotationExperienceAPI.requestCreateSpan(that.annotatorName, that.projectId, that.documentId, Number(aBegin), Number(aEnd), aLayer);
  }
  resetAlignments() {
    let that = this;
    for (let i = 0; i < that.arcs.length; i++) {
      this.annotationExperienceAPI.requestDeleteAnnotation(that.annotatorName, that.projectId, that.documentId, that.arcs[i].id, that.arcs[i].layerId);
    }
    for (let i = 0; i < this.spans.length; i++) {
      that.annotationExperienceAPI.requestDeleteAnnotation(that.annotatorName, that.projectId, that.documentId, that.spans[i].id, that.spans[i].layerId);
    }
    document.getElementById("save_alignment").disabled = false;
  }
};

define("model/FeatureX", ["require", "exports"], function (require, exports) {
    "use strict";
    Object.defineProperty(exports, "__esModule", { value: true });
    exports.FeatureX = void 0;
    var FeatureX = (function () {
        function FeatureX(aName, aValue) {
            this.name = aName;
            this.value = aValue;
        }
        return FeatureX;
    }());
    exports.FeatureX = FeatureX;
});
define("model/Span", ["require", "exports"], function (require, exports) {
    "use strict";
    Object.defineProperty(exports, "__esModule", { value: true });
    exports.Span = void 0;
    var Span = (function () {
        function Span(aId, aBegin, aEnd, aLayerId, aFeatures, aColor) {
            this.id = aId;
            this.begin = aBegin;
            this.end = aEnd;
            this.layerId = aLayerId;
            this.features = aFeatures;
            this.color = aColor;
        }
        return Span;
    }());
    exports.Span = Span;
});
define("model/Arc", ["require", "exports"], function (require, exports) {
    "use strict";
    Object.defineProperty(exports, "__esModule", { value: true });
    exports.Arc = void 0;
    var Arc = (function () {
        function Arc(aId, aSourceId, aTargetId, aLayerId, aFeatures, aColor) {
            this.id = aId;
            this.sourceId = aSourceId;
            this.targetId = aTargetId;
            this.layerId = aLayerId;
            this.features = aFeatures;
            this.color = aColor;
        }
        return Arc;
    }());
    exports.Arc = Arc;
});
define("model/Viewport", ["require", "exports"], function (require, exports) {
    "use strict";
    Object.defineProperty(exports, "__esModule", { value: true });
    exports.Viewport = void 0;
    var Viewport = (function () {
        function Viewport(aSourceDocumentId, aDocumentText, aBegin, aEnd, aLayers, aSpans, aArcs) {
            this.layers = [];
            this.spans = [];
            this.arcs = [];
            this.sourceDocumentId = aSourceDocumentId;
            this.documentText = aDocumentText;
            this.begin = aBegin;
            this.end = aEnd;
            this.layers = aLayers;
            this.spans = aSpans;
            this.arcs = aArcs;
        }
        return Viewport;
    }());
    exports.Viewport = Viewport;
});
define("messages/response/AdviceMessage", ["require", "exports"], function (require, exports) {
    "use strict";
    Object.defineProperty(exports, "__esModule", { value: true });
    exports.AdviceMessage = void 0;
    var AdviceMessage = (function () {
        function AdviceMessage() {
        }
        return AdviceMessage;
    }());
    exports.AdviceMessage = AdviceMessage;
});
define("messages/response/DocumentMessage", ["require", "exports"], function (require, exports) {
    "use strict";
    Object.defineProperty(exports, "__esModule", { value: true });
    exports.DocumentMessage = void 0;
    var DocumentMessage = (function () {
        function DocumentMessage() {
        }
        return DocumentMessage;
    }());
    exports.DocumentMessage = DocumentMessage;
});
define("messages/response/UpdateFeatureMessage", ["require", "exports"], function (require, exports) {
    "use strict";
    Object.defineProperty(exports, "__esModule", { value: true });
    exports.UpdateFeatureMessage = void 0;
    var UpdateFeatureMessage = (function () {
        function UpdateFeatureMessage() {
        }
        return UpdateFeatureMessage;
    }());
    exports.UpdateFeatureMessage = UpdateFeatureMessage;
});
define("messages/response/DeleteAnnotationMessage", ["require", "exports"], function (require, exports) {
    "use strict";
    Object.defineProperty(exports, "__esModule", { value: true });
    exports.DeleteAnnotationMessage = void 0;
    var DeleteAnnotationMessage = (function () {
        function DeleteAnnotationMessage() {
        }
        return DeleteAnnotationMessage;
    }());
    exports.DeleteAnnotationMessage = DeleteAnnotationMessage;
});
define("messages/response/create/SpanCreatedMessage", ["require", "exports"], function (require, exports) {
    "use strict";
    Object.defineProperty(exports, "__esModule", { value: true });
    exports.SpanCreatedMessage = void 0;
    var SpanCreatedMessage = (function () {
        function SpanCreatedMessage() {
        }
        return SpanCreatedMessage;
    }());
    exports.SpanCreatedMessage = SpanCreatedMessage;
});
define("messages/response/create/ArcCreatedMessage", ["require", "exports"], function (require, exports) {
    "use strict";
    Object.defineProperty(exports, "__esModule", { value: true });
    exports.ArcCreatedMessage = void 0;
    var ArcCreatedMessage = (function () {
        function ArcCreatedMessage() {
        }
        return ArcCreatedMessage;
    }());
    exports.ArcCreatedMessage = ArcCreatedMessage;
});
define("AnnotationExperienceAPI", ["require", "exports"], function (require, exports) {
    "use strict";
    Object.defineProperty(exports, "__esModule", { value: true });
});
define("messages/request/DocumentRequest", ["require", "exports"], function (require, exports) {
    "use strict";
    Object.defineProperty(exports, "__esModule", { value: true });
    exports.DocumentRequest = void 0;
    var DocumentRequest = (function () {
        function DocumentRequest(aAnnotatorName, aProjectId, aViewport) {
            this.annotatorName = aAnnotatorName;
            this.projectId = aProjectId;
            this.viewport = aViewport;
        }
        return DocumentRequest;
    }());
    exports.DocumentRequest = DocumentRequest;
});
define("messages/request/create/CreateArcRequest", ["require", "exports"], function (require, exports) {
    "use strict";
    Object.defineProperty(exports, "__esModule", { value: true });
    exports.CreateArcRequest = void 0;
    var CreateArcRequest = (function () {
        function CreateArcRequest(aAnnotatorName, aProjectId, aSourceDocumentId, aSourceId, aTargetId, aLayerId) {
            this.annotatorName = aAnnotatorName;
            this.projectId = aProjectId;
            this.sourceDocumentId = aSourceDocumentId;
            this.sourceId = aSourceId;
            this.targetId = aTargetId;
            this.layerId = aLayerId;
        }
        return CreateArcRequest;
    }());
    exports.CreateArcRequest = CreateArcRequest;
});
define("messages/request/create/CreateSpanRequest", ["require", "exports"], function (require, exports) {
    "use strict";
    Object.defineProperty(exports, "__esModule", { value: true });
    exports.CreateSpanRequest = void 0;
    var CreateSpanRequest = (function () {
        function CreateSpanRequest(aAnnotatorName, aProjectId, aSourceDocumentId, aBegin, aEnd, aLayerId) {
            this.annotatorName = aAnnotatorName;
            this.projectId = aProjectId;
            this.sourceDocumentId = aSourceDocumentId;
            this.begin = aBegin;
            this.end = aEnd;
            this.layerId = aLayerId;
        }
        return CreateSpanRequest;
    }());
    exports.CreateSpanRequest = CreateSpanRequest;
});
define("messages/request/DeleteAnnotationRequest", ["require", "exports"], function (require, exports) {
    "use strict";
    Object.defineProperty(exports, "__esModule", { value: true });
    exports.DeleteAnnotationRequest = void 0;
    var DeleteAnnotationRequest = (function () {
        function DeleteAnnotationRequest(aAnnotatorName, aProjectId, aSourceDocumentId, aAnnotationId, aLayerId) {
            this.annotatorName = aAnnotatorName;
            this.projectId = aProjectId;
            this.sourceDocumentId = aSourceDocumentId;
            this.annotationId = aAnnotationId;
            this.layerId = aLayerId;
        }
        return DeleteAnnotationRequest;
    }());
    exports.DeleteAnnotationRequest = DeleteAnnotationRequest;
});
define("messages/request/UpdateFeatureRequest", ["require", "exports"], function (require, exports) {
    "use strict";
    Object.defineProperty(exports, "__esModule", { value: true });
    exports.UpdateFeatureRequest = void 0;
    var UpdateFeatureRequest = (function () {
        function UpdateFeatureRequest(aAnnotatorName, aProjectId, aSourceDocumentId, aAnnotationId, aLayerId, aFeature, aValue) {
            this.annotatorName = aAnnotatorName;
            this.projectId = aProjectId;
            this.sourceDocumentId = aSourceDocumentId;
            this.annotationId = aAnnotationId;
            this.layerId = aLayerId;
            this.feature = aFeature;
            this.value = aValue;
        }
        return UpdateFeatureRequest;
    }());
    exports.UpdateFeatureRequest = UpdateFeatureRequest;
});
define("AnnotationExperienceAPIImpl", ["require", "exports", "@stomp/stompjs", "model/Span", "messages/response/AdviceMessage", "messages/response/DocumentMessage", "messages/request/DocumentRequest", "messages/response/UpdateFeatureMessage", "messages/response/create/SpanCreatedMessage", "messages/response/DeleteAnnotationMessage", "messages/response/create/ArcCreatedMessage", "messages/request/create/CreateArcRequest", "messages/request/create/CreateSpanRequest", "messages/request/DeleteAnnotationRequest", "messages/request/UpdateFeatureRequest", "model/Arc"], function (require, exports, stompjs_1, Span_1, AdviceMessage_1, DocumentMessage_1, DocumentRequest_1, UpdateFeatureMessage_1, SpanCreatedMessage_1, DeleteAnnotationMessage_1, ArcCreatedMessage_1, CreateArcRequest_1, CreateSpanRequest_1, DeleteAnnotationRequest_1, UpdateFeatureRequest_1, Arc_1) {
    "use strict";
    Object.defineProperty(exports, "__esModule", { value: true });
    exports.AnnotationExperienceAPIImpl = void 0;
    var AnnotationExperienceAPIImpl = (function () {
        function AnnotationExperienceAPIImpl(aProjectId, aDocumentId, aAnnotatorName, aUrl, aAnnotationEditor) {
            this.annotationEditor = aAnnotationEditor;
            this.connect(aProjectId, aDocumentId, aAnnotatorName, aUrl);
        }
        AnnotationExperienceAPIImpl.prototype.connect = function (aProjectId, aDocumentId, aAnnotatorName, aUrl) {
            console.log(aUrl);
            this.stompClient = stompjs_1.Stomp.over(function () {
                return new WebSocket(aUrl);
            });
            var that = this;
            this.stompClient.onConnect = function () {
                that.onConnect(aAnnotatorName, aProjectId, aDocumentId);
            };
            this.stompClient.onStompError = function (frame) {
                console.log('Broker reported error: ' + frame.headers['message']);
                console.log('Additional details: ' + frame.body);
            };
            this.stompClient.reconnectDelay = 50;
            this.stompClient.activate();
        };
        AnnotationExperienceAPIImpl.prototype.onConnect = function (aAnnotatorName, aProjectId, aDocumentId) {
            var that = this;
            this.stompClient.subscribe("/queue/document/" + aAnnotatorName, function (msg) {
                that.onDocument(Object.assign(new DocumentMessage_1.DocumentMessage(), JSON.parse(msg.body)));
            }, { id: "document_request" });
            this.stompClient.subscribe("/queue/error_message/" + aAnnotatorName, function (msg) {
                that.onError(Object.assign(new AdviceMessage_1.AdviceMessage(), JSON.parse(msg.body)));
            }, { id: "error_message" });
            this.stompClient.subscribe("/topic/features_update/" +
                aProjectId + "/" +
                aDocumentId, function (msg) {
                that.onFeaturesUpdate(Object.assign(new UpdateFeatureMessage_1.UpdateFeatureMessage(), JSON.parse(msg.body)));
            }, { id: "span_update" });
            this.stompClient.subscribe("/topic/span_create/" +
                aProjectId + "/" +
                aDocumentId, function (msg) {
                that.onSpanCreate(Object.assign(new SpanCreatedMessage_1.SpanCreatedMessage(), JSON.parse(msg.body)));
            }, { id: "span_create" });
            this.stompClient.subscribe("/topic/arc_create/" +
                aProjectId + "/" +
                aDocumentId, function (msg) {
                that.onArcCreate(Object.assign(new ArcCreatedMessage_1.ArcCreatedMessage(), JSON.parse(msg.body)));
            }, { id: "relation_create" });
            this.stompClient.subscribe("/topic/annotation_delete/" +
                aProjectId + "/" +
                aDocumentId, function (msg) {
                that.onAnnotationDelete(Object.assign(new DeleteAnnotationMessage_1.DeleteAnnotationMessage(), JSON.parse(msg.body)));
            }, { id: "span_delete" });
        };
        AnnotationExperienceAPIImpl.prototype.unsubscribe = function (aViewport) {
        };
        AnnotationExperienceAPIImpl.prototype.disconnect = function () {
            this.stompClient.deactivate();
        };
        AnnotationExperienceAPIImpl.prototype.requestDocument = function (aAnnotatorName, aProjectId, aViewport) {
            this.stompClient.publish({
                destination: "/app/document_request", body: JSON.stringify(new DocumentRequest_1.DocumentRequest(aAnnotatorName, aProjectId, aViewport))
            });
        };
        AnnotationExperienceAPIImpl.prototype.requestCreateArc = function (aAnnotatorName, aProjectId, aDocumentId, aSourceId, aTargetId, aLayer) {
            this.stompClient.publish({
                destination: "/app/arc_create",
                body: JSON.stringify(new CreateArcRequest_1.CreateArcRequest(aAnnotatorName, aProjectId, aDocumentId, aSourceId, aTargetId, aLayer))
            });
        };
        AnnotationExperienceAPIImpl.prototype.requestCreateSpan = function (aAnnotatorName, aProjectId, aDocumentId, aBegin, aEnd, aLayer) {
            this.stompClient.publish({
                destination: "/app/span_create",
                body: JSON.stringify(new CreateSpanRequest_1.CreateSpanRequest(aAnnotatorName, aProjectId, aDocumentId, aBegin, aEnd, aLayer))
            });
        };
        AnnotationExperienceAPIImpl.prototype.requestDeleteAnnotation = function (aAnnotatorName, aProjectId, aDocumentId, aAnnotationId, aLayer) {
            this.stompClient.publish({
                destination: "/app/annotation_delete", body: JSON.stringify(new DeleteAnnotationRequest_1.DeleteAnnotationRequest(aAnnotatorName, aProjectId, aDocumentId, aAnnotationId, aLayer))
            });
        };
        AnnotationExperienceAPIImpl.prototype.requestUpdateFeature = function (aAnnotatorName, aProjectId, aDocumentId, aAnnotationId, aLayerId, aFeature, aValue) {
            this.stompClient.publish({
                destination: "/app/features_update", body: JSON.stringify(new UpdateFeatureRequest_1.UpdateFeatureRequest(aAnnotatorName, aProjectId, aDocumentId, aAnnotationId, aLayerId, aFeature, aValue))
            });
        };
        AnnotationExperienceAPIImpl.prototype.onDocument = function (aMessage) {
            console.log('RECEIVED DOCUMENT' + aMessage);
            this.annotationEditor.viewport = aMessage.viewport;
        };
        AnnotationExperienceAPIImpl.prototype.onSpanCreate = function (aMessage) {
            console.log('RECEIVED SPAN CREATE' + aMessage);
            this.annotationEditor.viewport[0].spans.push(new Span_1.Span(aMessage.spanId, aMessage.begin, aMessage.end, aMessage.layerId, aMessage.features, aMessage.color));
        };
        AnnotationExperienceAPIImpl.prototype.onArcCreate = function (aMessage) {
            console.log('RECEIVED ARC CREATE' + aMessage);
            this.annotationEditor.viewport[0].arcs.push(new Arc_1.Arc(aMessage.arcId, aMessage.sourceId, aMessage.targetId, aMessage.layerId, aMessage.features, aMessage.color));
        };
        AnnotationExperienceAPIImpl.prototype.onAnnotationDelete = function (aMessage) {
            console.log('RECEIVED DELETE ANNOTATION' + aMessage);
            var annotationID = aMessage.annotationId;
        };
        AnnotationExperienceAPIImpl.prototype.onFeaturesUpdate = function (aMessage) {
            console.log('RECEIVED UPDATE ANNOTATION' + aMessage);
            var annotationID = aMessage.annotationId;
            var feature = aMessage.feature;
            var newValue = aMessage.value;
        };
        AnnotationExperienceAPIImpl.prototype.onError = function (aMessage) {
            console.log(aMessage);
        };
        return AnnotationExperienceAPIImpl;
    }());
    exports.AnnotationExperienceAPIImpl = AnnotationExperienceAPIImpl;
});

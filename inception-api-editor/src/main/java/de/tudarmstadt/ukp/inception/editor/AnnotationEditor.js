AnnotationEditor = {
    layer : "_none",
    test : function() {
        console.log("test")
    },

    actionDeleteAnnotation : function(begin, end) {

    },

    actionCreateAnnotation : function(type, begin, end) {

    },

    actionSelectLayer : function(layer) {
        AnnotationEditor.layer = layer;
    }
}

console.log("AnnotationEditor.init::Registered");
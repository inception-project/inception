//Tests with chai
//As preparation only
import {Annotation} from "../../main/ts/annotation/Annotation";
import {Experimental} from "../../main/ts";

/*
var assert = require('chai').assert
    , annotation = new Annotation(0,3,"Test", "POS")

assert.typeOf(annotation, Annotation);
assert.lengthOf(annotation.end - annotation.begin, 4);
 */

const tests = {
    chai : require('chai'),
    annotations: Array(Annotation),
    experimental : new Experimental(),

    runAllTests : function()
    {
        this.createAnnotationTest(new Annotation(0,1,"Test", "POS", "Noun"));
        this.deleteAnnotationTest(this.annotations.get(0));
    },

    createAnnotationTest : function(aAnnotation : Annotation)
    {
        this.annotations.push(aAnnotation)
    },

    deleteAnnotationTest : function(aAnnotation : Annotation)
    {
        this.annotations.filter(annotation => annotation !== aAnnotation)
    }
}
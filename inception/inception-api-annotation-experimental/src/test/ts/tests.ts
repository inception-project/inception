//Tests with chai
//As preparation only
import {Annotation} from "../../main/ts/annotation/Annotation";
import {Experimental} from "../../main/ts";

var experimental = new Experimental();

/*
var assert = require('chai').assert
    , annotation = new Annotation(0,3,"Test", "POS")

assert.typeOf(annotation, Annotation);
assert.lengthOf(annotation.end - annotation.begin, 4);
 */

const tests = {
    assert : require('chai').assert,

    runAllTests : function()
    {
        this.createAnnotationTest(new Annotation(0,1,"Test", "NER"));
        this.deleteAnnotationTest();
    },

    createAnnotationTest : function(annotation : Annotation)
    {

    },

    deleteAnnotationTest : function(annotation : Annotation)
    {

    }
}
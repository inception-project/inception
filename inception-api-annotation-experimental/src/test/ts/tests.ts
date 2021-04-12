//Tests with chai
//As preparation only
import {Annotation} from "../../main/ts/annotation/Annotation";

var assert = require('chai').assert
    , annotation = new Annotation(0,3,"Test", "POS")

assert.typeOf(annotation, Annotation);
assert.lengthOf(annotation.end - annotation.begin, 4);
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
import {Annotation} from "./annotation/Annotation";
import {Eventhandler} from "./events/Eventhandler";
import * as ConfigFile from "./config.json";

/**
 * Typescript API
 */
export class Experimental {
    annotations: Annotation[];
    layers: string[];
    event: Eventhandler;
    document : File;

    constructor() {
        //TODO define what is needed to be configurable
        ConfigFile.EditorColors.forEach(color => {
            console.log(color)
        })

        ConfigFile.Layers.forEach(layer => {
            this.layers.push(layer.name)
            console.log(layer)
        })

        //Init eventhandler
        this.event = new Eventhandler(this);

    }

    _initAnnotations = (aAnnotations: Annotation[]) => {
        this._setAnnotations(aAnnotations)
    }

    _loadDocument = (aDocument : File) => {
        this.document = aDocument;
    }


    _createAnnotation = (aAnnotation: { begin: number; end: number; text: string; type: string; }) => {
        try {
            const annotation = new Annotation(aAnnotation.begin, aAnnotation.end, aAnnotation.text, aAnnotation.type);
            this.annotations.push(annotation)

        } catch (exception) {
            console.warn('Could not add the new annotation. Begin: ' +  aAnnotation.begin +
                ', End: ' + aAnnotation.end +
                '. Exception: ' + exception)
        }
    }

    _deleteAnnotation = (aAnnotation: Annotation) => {
        try {
            this.annotations.filter(annotation => annotation !== aAnnotation)
        } catch (exception) {
            console.warn('Could not remove the annotaion at: ' + aAnnotation.begin + '. Exception: ' + exception)
        }
    }


    // --------------------------------------------------- //


    // ---------------- Getter and Setter ---------------- //


    _selectAnnotation = (aBegin: number, aType: string) => {
        return this.annotations.filter(annotation => ((annotation.begin == aBegin) && (annotation.type == aType)))
    }

    _updateAnnotation = (aBegin: number, aType: string, aNewType: string) => {
        const annotation = this.annotations.find(anno =>
            (anno.begin == aBegin) && (anno.type = aType))

        annotation._changeType(aNewType)
    }

    _getAnnotations = () => {
        return this.annotations
    }

    _setAnnotations = (aAnnotations: Annotation[]) => {
        this.annotations = aAnnotations;
    }
    // --------------------------------------------------- //
}
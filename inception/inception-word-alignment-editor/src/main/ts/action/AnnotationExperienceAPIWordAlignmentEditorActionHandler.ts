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

import {WordAlignmentEditor} from "../AnnotationExperienceAPIWordAlignmentEditor";
import {Viewport} from "../../../../../inception-api-annotation-experimental/src/main/ts/client/model/Viewport";
import {FeatureX} from "../../../../../inception-api-annotation-experimental/src/main/ts/client/model/FeatureX";

export class AnnotationExperienceAPIWordAlignmentEditorActionHandler {
    annotationExperienceAPIWordAlignmentEditor: WordAlignmentEditor;

    sentences : string[];

    constructor(aAnnotationExperienceAPIWordAlignmentEditor: WordAlignmentEditor) {
        this.annotationExperienceAPIWordAlignmentEditor = aAnnotationExperienceAPIWordAlignmentEditor;
        this.registerDefaultActionHandler();
    }


    registerDefaultActionHandler() {
        let that = this;
        onclick = function (aEvent) {
            let elem = <Element>aEvent.target;
            if (elem.className === 'far fa-caret-square-right' || elem.className === 'far fa-caret-square-left') {
                setTimeout(function() {
                    that.annotationExperienceAPIWordAlignmentEditor.annotationExperienceAPI.requestDocument(
                        that.annotationExperienceAPIWordAlignmentEditor.annotatorName,
                        that.annotationExperienceAPIWordAlignmentEditor.projectId,
                        that.annotationExperienceAPIWordAlignmentEditor.viewport);
                    setTimeout(function () {
                        console.log(that.annotationExperienceAPIWordAlignmentEditor.viewport[0].documentText)
                        that.sentences = that.annotationExperienceAPIWordAlignmentEditor.viewport[0].documentText.split(".");
                        that.annotationExperienceAPIWordAlignmentEditor.oddSentence = that.sentences[0];
                        that.annotationExperienceAPIWordAlignmentEditor.evenSentence = that.sentences[1];

                        that.annotationExperienceAPIWordAlignmentEditor.oddSentenceOffset = 0;
                        that.annotationExperienceAPIWordAlignmentEditor.evenSentenceOffset = 1;

                        that.annotationExperienceAPIWordAlignmentEditor.annotationExperienceAPIVisualization.refreshEditor();
                    }, 2000)
                }, 200);

                document.getElementById("save_alignment").disabled = false;
            }

            if (elem.className === 'fas fa-step-forward' || elem.className === 'fas fa-step-backward') {
                setTimeout(function() {
                    let offset = Number(document.getElementsByTagName("input")[2].value);
                    that.annotationExperienceAPIWordAlignmentEditor.oddSentence = that.sentences[offset - 1];
                    that.annotationExperienceAPIWordAlignmentEditor.evenSentence = that.sentences[offset];

                    that.annotationExperienceAPIWordAlignmentEditor.oddSentenceOffset = offset - 1;
                    that.annotationExperienceAPIWordAlignmentEditor.evenSentenceOffset = offset;

                    that.annotationExperienceAPIWordAlignmentEditor.annotationExperienceAPIVisualization.refreshEditor();

                    document.getElementById("save_alignment").disabled= false;
                }, 200);
            }

            if (elem.id === 'delete_alignment') {
                that.annotationExperienceAPIWordAlignmentEditor.resetAlignments();
            }

            if (elem.id === 'save_alignment') {
                that.annotationExperienceAPIWordAlignmentEditor.saveAlignments();
            }

            if (elem.id === 'show_relations') {
                that.annotationExperienceAPIWordAlignmentEditor.annotationExperienceAPIVisualization.drawLines();
            }

            if (elem.id === 'new') {

                let layersToAdd : number[] = [];

                for(let i = 0; i < that.annotationExperienceAPIWordAlignmentEditor.layers.length; i++) {
                    layersToAdd.push(that.annotationExperienceAPIWordAlignmentEditor.layers[i][0]);
                }
                let v = new Viewport(41738,"",0,74,layersToAdd,null,null);
                that.annotationExperienceAPIWordAlignmentEditor.annotationExperienceAPI.requestDocument("admin",20, [v]);
            }

            if (elem.id === 'createS') {
                that.annotationExperienceAPIWordAlignmentEditor.annotationExperienceAPI.requestCreateSpan("admin",20,41738,0,9,278)
            }
            if (elem.id === 'createR') {
                that.annotationExperienceAPIWordAlignmentEditor.annotationExperienceAPI.requestCreateArc("admin",20,41738,that.annotationExperienceAPIWordAlignmentEditor.viewport[0].spans[0].id,that.annotationExperienceAPIWordAlignmentEditor.viewport[0].spans[0].id,279)
            }

            if (elem.id === 'delete') {
                that.annotationExperienceAPIWordAlignmentEditor.annotationExperienceAPI.requestDeleteAnnotation("admin",20,41738,that.annotationExperienceAPIWordAlignmentEditor.viewport[0].spans[0].id,278)
            }

            if (elem.id === 'update') {
                that.annotationExperienceAPIWordAlignmentEditor.annotationExperienceAPI.requestUpdateFeature("admin",20,41738,that.annotationExperienceAPIWordAlignmentEditor.viewport[0].spans[0].id,278,that.annotationExperienceAPIWordAlignmentEditor.viewport[0].spans[0].features[2],"TEST")
            }
        }
    }
}
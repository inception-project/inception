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

import {AnnotationExperienceAPIWordAlignmentEditor} from "../AnnotationExperienceAPIWordAlignmentEditor";

export class AnnotationExperienceAPIWordAlignmentEditorActionHandler {
    annotationExperienceAPIWordAlignmentEditor: AnnotationExperienceAPIWordAlignmentEditor;

    sentences : string[];

    constructor(aAnnotationExperienceAPIWordAlignmentEditor: AnnotationExperienceAPIWordAlignmentEditor) {
        this.annotationExperienceAPIWordAlignmentEditor = aAnnotationExperienceAPIWordAlignmentEditor;
        this.registerDefaultActionHandler();
    }


    registerDefaultActionHandler() {
        let that = this;
        onclick = function (aEvent) {
            let elem = <Element>aEvent.target;
            if (elem.className === 'far fa-caret-square-right' || 'far fa-caret-square-left') {
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
        }
    }
}
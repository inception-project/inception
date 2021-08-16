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

import {AnnotationExperienceAPI} from "../../../../../../../../inception-api-annotation-experimental/src/main/ts/client/AnnotationExperienceAPI";
import {AnnotationExperienceAPIWordAlignmentEditor} from "../AnnotationExperienceAPIWordAlignmentEditor";
import {Viewport} from "../../../../../../../../inception-api-annotation-experimental/src/main/ts/client/model/Viewport";

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
            if (elem.className === 'far fa-caret-square-right') {
                that.annotationExperienceAPIWordAlignmentEditor.annotationExperienceAPI.requestDocument(
                    that.annotationExperienceAPIWordAlignmentEditor.annotationExperienceAPI.clientName,
                    that.annotationExperienceAPIWordAlignmentEditor.annotationExperienceAPI.projectID,
                    Number(document.location.href.split("=")[1].split("&")[0]));
                setTimeout(function () {
                    that.sentences = that.annotationExperienceAPIWordAlignmentEditor.annotationExperienceAPI.viewport.documentText.split(".");

                    that.annotationExperienceAPIWordAlignmentEditor.oddSentence = that.sentences[0];
                    that.annotationExperienceAPIWordAlignmentEditor.currentSentence++;
                    that.annotationExperienceAPIWordAlignmentEditor.evenSentence = that.sentences[1];
                    that.annotationExperienceAPIWordAlignmentEditor.currentSentence++;
                }, 2000)

                document.getElementById("save_alignment").disabled= false;
            }

            if (elem.className === 'fas fa-step-forward' || elem.className === 'fas fa-step-backward') {

                that.annotationExperienceAPIWordAlignmentEditor.oddSentence = that.sentences[String(Number(document.getElementsByTagName("input")[2].value) - 1)];;
                that.annotationExperienceAPIWordAlignmentEditor.evenSentence = that.sentences[Number(document.getElementsByTagName("input")[2].value)];

                document.getElementById("save_alignment").disabled= false;
            }

            if (elem.id === 'delete_alignment') {
                that.annotationExperienceAPIWordAlignmentEditor.resetAlignments();
            }

            if (elem.id === 'save_alignment') {
                that.annotationExperienceAPIWordAlignmentEditor.saveAlignments();
            }
        }
    }
}
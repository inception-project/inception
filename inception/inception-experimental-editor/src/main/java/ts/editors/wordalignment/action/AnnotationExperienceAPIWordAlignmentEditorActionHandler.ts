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

export class AnnotationExperienceAPIWordAlignmentEditorActionHandler {
    annotationExperienceAPIWordAlignmentEditor: AnnotationExperienceAPIWordAlignmentEditor;

    constructor(aAnnotationExperienceAPIWordAlignmentEditor: AnnotationExperienceAPIWordAlignmentEditor) {
        this.annotationExperienceAPIWordAlignmentEditor = aAnnotationExperienceAPIWordAlignmentEditor;
    }


    registerDefaultActionHandler() {
        let that = this;
        onclick = function (aEvent) {
            let elem = <Element>aEvent.target;
            console.log(elem)
            if (elem.id === 'next_sentence') {
                that.annotationExperienceAPIWordAlignmentEditor.annotationExperienceAPI.requestNewDocumentFromServer("admin", "admin", 20, 41718, "SENTENCE", [[that.annotationExperienceAPIWordAlignmentEditor.currentSentenceCount]], false)
                setTimeout(function () {
                    that.annotationExperienceAPIWordAlignmentEditor.annotationExperienceAPIVisualization.showText("sentence")
                    that.annotationExperienceAPIWordAlignmentEditor.annotationExperienceAPI.requestNewDocumentFromServer("admin", "admin", 20, 41717, "SENTENCE", [[that.annotationExperienceAPIWordAlignmentEditor.currentSentenceCount]], false)
                    setTimeout(function () {
                        that.annotationExperienceAPIWordAlignmentEditor.annotationExperienceAPIVisualization.showText("alignment")
                    }, 1000)
                }, 1000)

                setTimeout(function () {
                    ++that.annotationExperienceAPIWordAlignmentEditor.currentSentenceCount;
                }, 3000);

            }

            if (elem.id === 'save_alignment') {
                that.annotationExperienceAPIWordAlignmentEditor.saveAlignments();
            }
        }
    }
}
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
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import {AnnotationExperienceAPIWordAlignmentEditor} from "../../wordalignment/AnnotationExperienceAPIWordAlignmentEditor";
import {AnnotationExperienceAPIBasicEditor} from "../AnnotationExperienceAPIBasicEditor";

export class AnnotationExperienceAPIBasicEditorActionHandler {
    annotationExperienceAPIBasicEditor: AnnotationExperienceAPIBasicEditor;

    sentences : string[];

    constructor(annotationExperienceAPIBasicEditor: AnnotationExperienceAPIBasicEditor) {
        this.annotationExperienceAPIBasicEditor = annotationExperienceAPIBasicEditor;
        this.registerDefaultActionHandler();
    }


    registerDefaultActionHandler() {
        let that = this;
        onclick = function (aEvent) {
            let elem = <Element>aEvent.target;
            if (elem.className === 'far fa-caret-square-right' || 'far fa-caret-square-left') {
                setTimeout(function() {
                    that.annotationExperienceAPIBasicEditor.annotationExperienceAPI.requestDocument(
                        that.annotationExperienceAPIBasicEditor.annotationExperienceAPI.clientName,
                        that.annotationExperienceAPIBasicEditor.annotationExperienceAPI.projectID,
                        Number(document.location.href.split("=")[1].split("&")[0]));
                    setTimeout(function () {
                        that.sentences = that.annotationExperienceAPIBasicEditor.annotationExperienceAPI.viewport.documentText.split(".");
                        that.annotationExperienceAPIBasicEditor.annotationExperienceAPIVisualization.refreshEditor("textarea")
                    }, 2000)
                }, 200);
            }

            if (elem.className === 'fas fa-step-forward' || elem.className === 'fas fa-step-backward') {
                setTimeout(function() {
                    that.annotationExperienceAPIBasicEditor.annotationExperienceAPIVisualization.refreshEditor("textarea");
                }, 200);
            }
        }
    }
}
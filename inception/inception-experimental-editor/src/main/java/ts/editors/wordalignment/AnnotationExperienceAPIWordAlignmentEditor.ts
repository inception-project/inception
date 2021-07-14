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
import {AnnotationExperienceAPIImpl} from "../../../../../../../inception-api-annotation-experimental/src/main/ts/client/AnnotationExperienceAPIImpl";
import {AnnotationExperienceAPIWordAlignmentEditorVisualization} from "./visualization/AnnotationExperienceAPIWordAlignmentEditorVisualization";
import {AnnotationExperienceAPIWordAlignmentEditorActionHandler} from "./action/AnnotationExperienceAPIWordAlignmentEditorActionHandler";

export class AnnotationExperienceAPIWordAlignmentEditor
{
    annotationExperienceAPI: AnnotationExperienceAPIImpl;
    annotationExperienceAPIVisualization: AnnotationExperienceAPIWordAlignmentEditorVisualization;
    annotationExperienceAPIWordAlignmentEditorActionHandler: AnnotationExperienceAPIWordAlignmentEditorActionHandler
    currentSentence : string[];
    currentAlignment : string[];
    currentSentenceCount : number = 0;

    constructor()
    {
        this.annotationExperienceAPI = new AnnotationExperienceAPIImpl();
        this.annotationExperienceAPIVisualization = new AnnotationExperienceAPIWordAlignmentEditorVisualization(this);
        this.annotationExperienceAPIWordAlignmentEditorActionHandler = new AnnotationExperienceAPIWordAlignmentEditorActionHandler(this);
        this.annotationExperienceAPIWordAlignmentEditorActionHandler.registerDefaultActionHandler();
    }

    saveAlignments()
    {
        let pairs : string = "";
        for (let i = 0; i < document.getElementById("align_words").children.length; i++) {
            for (let j = 0; j < document.getElementById("align_words").children.length; j++) {
                if (document.getElementById("align_words").children[j].value == '') {
                    continue;
                }
                if (document.getElementById("align_words").children[j].value == i) {
                    pairs = pairs.concat(this.currentSentence[i] + ":" + this.currentAlignment[j] + ",")
                }
            }
        }
        this.annotationExperienceAPI.requestSaveWordAlignment("admin","admin",
            this.annotationExperienceAPI.projectID, this.currentSentenceCount, pairs)
    }
}
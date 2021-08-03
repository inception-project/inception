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

export class AnnotationExperienceAPIWordAlignmentEditor {
    annotationExperienceAPI: AnnotationExperienceAPIImpl;
    annotationExperienceAPIVisualization: AnnotationExperienceAPIWordAlignmentEditorVisualization;
    annotationExperienceAPIWordAlignmentEditorActionHandler: AnnotationExperienceAPIWordAlignmentEditorActionHandler

    originalLanguageSentence : string;
    originalOffsetBegin : number
    translatedLanguageSentence: string;
    translatedOffsetBegin: number;

    constructor()
    {
        this.annotationExperienceAPI = new AnnotationExperienceAPIImpl();
        this.annotationExperienceAPIVisualization = new AnnotationExperienceAPIWordAlignmentEditorVisualization(this);
        this.annotationExperienceAPIWordAlignmentEditorActionHandler = new AnnotationExperienceAPIWordAlignmentEditorActionHandler(this);
        this.annotationExperienceAPIWordAlignmentEditorActionHandler.registerDefaultActionHandler();
    }

    saveAlignments() {
        let pairs = []
        let values = []
        const that = this;

        if (!document.getElementById("multipleSelect").checked) {
            for (let i = 0; i < document.getElementById("container_german").children.length - 2; i++) {
                if (values.indexOf(document.getElementById("container_german").children[i + 2].children[1].value) > -1) {
                    alert("Word alignment is not 1:1.")
                    return;
                }
                values.push(document.getElementById("container_german").children[i + 2].children[1].value)
            }
        }

        for (let i = 0; i < document.getElementById("container_english").children.length - 2; i++) {
            for (let j = 0; j < document.getElementById("container_german").children.length - 2; j++) {
                if ((document.getElementById("container_english").children[i + 2].children[0].value) ===
                    (document.getElementById("container_german").children[j + 2].children[1].value)) {
                    pairs.push([
                        document.getElementById("container_english").children[i + 2].children[1].id.split("_")[2],
                        document.getElementById("container_english").children[i + 2].children[1].innerText,
                        document.getElementById("container_german").children[j + 2].children[0].id.split("_")[2],
                        document.getElementById("container_german").children[j + 2].children[0].innerText]
                        );
                    this.annotationExperienceAPI.requestCreateSpanFromServer(
                        that.annotationExperienceAPI.clientName,
                        that.annotationExperienceAPI.clientName,
                        that.annotationExperienceAPI.projectID,
                        this.annotationExperienceAPI.documentID,
                        Number(document.getElementById("container_english").children[i + 2].children[1].id.split("_")[2]),
                        Number(document.getElementById("container_english").children[i + 2].children[1].id.split("_")[3]),
                        "webanno.custom.Word_Alignment_Span")

                    this.annotationExperienceAPI.requestCreateSpanFromServer(
                        that.annotationExperienceAPI.clientName,
                        that.annotationExperienceAPI.clientName,
                        that.annotationExperienceAPI.projectID,
                        this.annotationExperienceAPI.documentID,
                        Number(document.getElementById("container_german").children[i + 2].children[0].id.split("_")[2]),
                        Number(document.getElementById("container_german").children[i + 2].children[0].id.split("_")[3]),
                        "webanno.custom.Word_Alignment_Span")

                    document.getElementById("container_german").children[j + 2].children[1].setAttribute("disabled", "true");
                }
            }
        }

        setTimeout(function () {
            let governor, dependent;
            for (let i = 0; i < pairs.length; i++) {
                for (let j = 0; j < that.annotationExperienceAPI.spans.length; j++) {
                    if (pairs[i][0] == that.annotationExperienceAPI.spans[j].begin &&
                        pairs[i][1] == that.annotationExperienceAPI.spans[j].coveredText) {
                        governor = that.annotationExperienceAPI.spans[j];
                    }
                    if (pairs[i][2] == that.annotationExperienceAPI.spans[j].begin &&
                        pairs[i][3] == that.annotationExperienceAPI.spans[j].coveredText) {
                        dependent = that.annotationExperienceAPI.spans[j];
                    }
                }
                that.annotationExperienceAPI.requestCreateRelationFromServer(
                    that.annotationExperienceAPI.clientName,
                    that.annotationExperienceAPI.clientName,
                    that.annotationExperienceAPI.projectID,
                    that.annotationExperienceAPI.documentID,
                    governor.id,
                    dependent.id,
                    "webanno.custom.Word_Alignment_Relation")
            }
        }, 5000)

        document.getElementById("save_alignment").disabled= true

    }

    resetAlignments() {

        let that = this;

        for (let i = 0; i <this.annotationExperienceAPI.relations.length; i++) {
            this.annotationExperienceAPI.requestDeleteRelationFromServer(
                that.annotationExperienceAPI.clientName,
                that.annotationExperienceAPI.clientName,
                that.annotationExperienceAPI.projectID,
                that.annotationExperienceAPI.documentID,
                that.annotationExperienceAPI.relations[i].id,
                that.annotationExperienceAPI.relations[i].type

            )
        }

        for (let i = 0; i <this.annotationExperienceAPI.spans.length; i++) {
            this.annotationExperienceAPI.requestDeleteSpanFromServer(
                that.annotationExperienceAPI.clientName,
                that.annotationExperienceAPI.clientName,
                that.annotationExperienceAPI.projectID,
                that.annotationExperienceAPI.documentID,
                that.annotationExperienceAPI.spans[i].id,
                that.annotationExperienceAPI.spans[i].type
            )
        }
        document.getElementById("save_alignment").disabled = false;
    }
}
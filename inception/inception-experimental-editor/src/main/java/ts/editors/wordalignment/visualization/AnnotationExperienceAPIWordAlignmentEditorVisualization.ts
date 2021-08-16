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
import {Span} from "../../../../../../../../inception-api-annotation-experimental/src/main/ts/client/model/Span";
import {AnnotationExperienceAPIWordAlignmentEditor} from "../AnnotationExperienceAPIWordAlignmentEditor";

export class AnnotationExperienceAPIWordAlignmentEditorVisualization {

    annotationExperienceAPIWordAlignmentEditor: AnnotationExperienceAPIWordAlignmentEditor;

    oddLanguage : string = "English";
    evenLanguage : string = "German";

    headerOffset = 75;

    constructor(aAnnotationExperienceAPIWordAlignmentEditor: AnnotationExperienceAPIWordAlignmentEditor) {
        this.annotationExperienceAPIWordAlignmentEditor = aAnnotationExperienceAPIWordAlignmentEditor;
    }

    showText(aElementId: string) {
        let words;

        let container = document.getElementById(aElementId.toString())

        let arcs = this.annotationExperienceAPIWordAlignmentEditor.annotationExperienceAPI.arcs;

        //Reset previous text
        container.innerHTML = '';

        let language = document.createElement("b")

        if (aElementId === "odd_unit_container") {
            language.innerText = this.oddLanguage;
            words = this.annotationExperienceAPIWordAlignmentEditor.oddSentence.split(" ");

        } else {
            language.innerText = this.evenLanguage;
            words = this.annotationExperienceAPIWordAlignmentEditor.evenSentence.split(" ");
        }

        container.setAttribute("style", "float:left; width: 200px");

        container.appendChild(language);
        container.appendChild(document.createElement("hr"))


        let svg = document.getElementById("svg");
        let heightOriginal = document.getElementById("odd_unit_container").offsetHeight;
        let heightTranslation = document.getElementById("even_unit_container").offsetHeight;
        if (heightOriginal > heightTranslation) {
            svg.setAttribute("height", String(heightOriginal));
        } else {
            svg.setAttribute("height", String(heightTranslation));
        }


        for (let i = 0; i < words.length; i++) {
            let wordDIV = document.createElement("div");
            wordDIV.className = "form-group";
            wordDIV.style.height = "40px";

            let wordLABEL = document.createElement("label");
            wordLABEL.setAttribute("style", "text-align: right")
            wordLABEL.id = "word_" + i;
            wordLABEL.innerText = words[i];

            let wordINPUT = document.createElement("input");
            wordINPUT.setAttribute("size", "1")
            wordINPUT.setAttribute("maxlength", "2")

            if (aElementId === "odd_unit_container") {
                wordINPUT.setAttribute("style", "float:left; text-align: center")
                wordINPUT.id = "original_word_" + i;
                wordINPUT.value = String(i);
                wordINPUT.disabled = true;
                wordDIV.appendChild(wordINPUT);
                wordDIV.appendChild(wordLABEL);

            } else {
                wordINPUT.setAttribute("style", "float:right; text-align: center")
                wordINPUT.id = "translated_word_" + i;
                wordDIV.appendChild(wordLABEL);
                wordDIV.appendChild(wordINPUT);

            }

            container.appendChild(wordDIV);
        }

        this.drawLines();
    }

    drawLines() {

        let that = this;

        let svg = document.getElementById("svg");
        let arcs = that.annotationExperienceAPIWordAlignmentEditor.annotationExperienceAPI.arcs;
        let spans = that.annotationExperienceAPIWordAlignmentEditor.annotationExperienceAPI.spans;

        for (let i = 0; i < arcs.length; i++) {
            let yGovernor = null;
            let govID = null
            let yDependent = null;
            let depID = null;
            for (let j = 0; j < spans.length; j++) {
                if (spans[j].id == arcs[i].sourceId) {
                    for (let k = 0; k < document.getElementById("odd_unit_container").children.length - 2; k++) {
                        if (document.getElementById("odd_unit_container").children[k + 2].children[1].id.split("_")[2]
                            == spans[j].begin) {
                            yGovernor = this.headerOffset + k * 48;
                            govID = spans[j].id
                        }
                    }
                }
                if (spans[j].id == arcs[i].targetId) {
                    for (let k = 0; k < document.getElementById("even_unit_container").children.length - 2; k++) {
                        if (document.getElementById("even_unit_container").children[k + 2].children[0].id.split("_")[2]
                            == spans[j].begin) {
                            yDependent = this.headerOffset + k * 48;
                            depID = spans[j].id
                            continue;
                        }
                    }
                }

                if (yGovernor != null && yDependent != null) {

                    let line = document.createElementNS("http://www.w3.org/2000/svg", "line");
                    line.setAttribute("x1", "5");
                    line.setAttribute("y1", (yGovernor).toString());
                    line.setAttribute("x2", "95");
                    line.setAttribute("y2", (yDependent).toString());
                    line.setAttribute("gov_id", (govID).toString());
                    line.setAttribute("dep_id", (depID).toString());
                    line.addEventListener('mouseover', function(e) {

                        for (let i = 0; i < arcs.length; i++) {
                            if ((arcs[i].sourceId == (Number(line.getAttribute("gov_id")))) &&
                                (arcs[i].targetId == (Number(line.getAttribute("dep_id"))))) {
                                    that.annotationExperienceAPIWordAlignmentEditor.annotationExperienceAPI.requestSelectArc(
                                        that.annotationExperienceAPIWordAlignmentEditor.annotationExperienceAPI.clientName,
                                        that.annotationExperienceAPIWordAlignmentEditor.annotationExperienceAPI.projectID,
                                        that.annotationExperienceAPIWordAlignmentEditor.annotationExperienceAPI.documentID,
                                        arcs[i].id)
                                }
                            setTimeout(function () {
                                let relation = that.annotationExperienceAPIWordAlignmentEditor.annotationExperienceAPI.selectedArc;
                                alert("RelationID: " + relation.id + ", \n" +
                                    "SourceID: " + relation.sourceId + ", \n" +
                                    "SourceText: " + relation.targetCoveredText + ", \n" +
                                    "TargetID: " + relation.targetId + ", \n" +
                                    "TargetText: " + relation.targetCoveredText);
                            }, 2000)
                        }
                    });
                    //TODO color
                    if (arcs.length > 0) {
                        line.style.stroke = "#9a001b";
                    }
                    line.style.strokeWidth = "2";
                    svg.appendChild(line);
                    break;
                }
            }
        }
    }

    showDependencies() {
        let spans = this.annotationExperienceAPIWordAlignmentEditor.annotationExperienceAPI.spans;

        for (let i = 0; i < document.getElementById("odd_unit_container").children.length - 2; i++) {
            for (let j = 0; j < spans.length; j++) {
                if (spans[j].begin === Number(document.getElementById("odd_unit_container").children[i + 2].children[1].id.split("_")[2]) &&
                    spans[j].end === Number(document.getElementById("odd_unit_container").children[i + 2].children[1].id.split("_")[3])) {
                    document.getElementById("odd_unit_container").children[i + 2].children[1].setAttribute("style", "border = 2px solid " + spans[i].color);
                    break;
                }
            }
        }

        for (let i = 0; i < document.getElementById("even_unit_container").children.length - 2; i++) {
            for (let j = 0; j < spans.length; j++) {
                if (spans[j].begin === Number(document.getElementById("even_unit_container").children[i + 2].children[0].id.split("_")[2]) &&
                    spans[j].end === Number(document.getElementById("even_unit_container").children[i + 2].children[0].id.split("_")[3])) {
                    document.getElementById("even_unit_container").children[i + 2].children[1].style = "border = 2px solid " + spans[i].color;

                    break;
                }
            }
        }

    }

    refreshEditor() {
        document.getElementById("svg").innerHTML = '';

        this.showText("odd_unit_container");
        this.showText("even_unit_container");

    }


}
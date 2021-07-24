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

    constructor(aAnnotationExperienceAPIWordAlignmentEditor: AnnotationExperienceAPIWordAlignmentEditor) {
        this.annotationExperienceAPIWordAlignmentEditor = aAnnotationExperienceAPIWordAlignmentEditor;
    }

    showText(aElementId: string) {
        let words;

        let container = document.getElementById(aElementId.toString())

        let relations = this.annotationExperienceAPIWordAlignmentEditor.annotationExperienceAPI.relations;
        let spans = this.annotationExperienceAPIWordAlignmentEditor.annotationExperienceAPI.spans;

        //Reset previous text
        container.innerHTML = '';

        let language = document.createElement("b")

        if (aElementId === "container_english") {
            language.innerText = "English";
            words = this.annotationExperienceAPIWordAlignmentEditor.originalLanguageSentence.split(" ");

        } else {
            language.innerText = "Deutsch";
            words = this.annotationExperienceAPIWordAlignmentEditor.translatedLanguageSentence.split(" ");
        }

        container.setAttribute("style", "float:left; width: 200px");

        container.appendChild(language);
        container.appendChild(document.createElement("hr"))

        let prevOffset = 0;


        for (let i = 0; i < words.length; i++) {
            let wordDIV = document.createElement("div");
            wordDIV.className = "form-group";

            let wordLABEL = document.createElement("label");
            wordLABEL.setAttribute("style", "text-align: right")
            wordLABEL.id = "word_offset_" + (prevOffset).toString() + "_" + (prevOffset + words[i].length).toString();
            wordLABEL.innerText = words[i];

            for (let i = 0; i < spans.length; i++) {
                if (spans[i].begin === Number(wordLABEL.id.split("_")[2]) &&
                    spans[i].end === Number(wordLABEL.id.split("_")[3])) {
                    console.log("FOUND for: " + wordLABEL.innerText + ", COLOR: " +  spans[i].color)
                    wordLABEL.style.border = "2px solid " + spans[i].color;
                    break;
                }
            }

            prevOffset = prevOffset + words[i].length + 1;

            let wordINPUT = document.createElement("input");
            wordINPUT.setAttribute("size", "1")
            wordINPUT.setAttribute("maxlength", "2")

            if (aElementId === "container_english") {
                wordINPUT.setAttribute("style", "float:left; text-align: center")
                wordINPUT.id = "original_word_" + i;
                wordINPUT.value = String(i);
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
    }

    showDependencies()
    {
        let spans = this.annotationExperienceAPIWordAlignmentEditor.annotationExperienceAPI.spans;

        for (let i = 0; i < document.getElementById("container_english").children.length - 2; i++) {
            for (let j = 0; j < spans.length; j++) {
                console.log(spans[j].begin)
                console.log(spans[j].end)
                console.log(Number(document.getElementById("container_english").children[i+2].children[1].id.split("_")[2]))
                console.log(Number(document.getElementById("container_english").children[i+2].children[1].id.split("_")[3]))
                if (spans[j].begin === Number(document.getElementById("container_english").children[i+2].children[1].id.split("_")[2]) &&
                    spans[j].end === Number(document.getElementById("container_english").children[i+2].children[1].id.split("_")[3])) {
                    document.getElementById("container_english").children[i + 2].children[1].setAttribute("style", "border = 2px solid " + spans[i].color);
                    console.log("FOUND");
                    break;
                }
            }
        }

        console.log("_______________________________")
        for (let i = 0; i < document.getElementById("container_german").children.length - 2; i++) {
            for (let j = 0; j < spans.length; j++) {
                console.log(spans[j].begin)
                console.log(spans[j].end)
                console.log(Number(document.getElementById("container_german").children[i+2].children[0].id.split("_")[2]))
                console.log(Number(document.getElementById("container_german").children[i+2].children[0].id.split("_")[3]))
                if (spans[j].begin === Number(document.getElementById("container_german").children[i+2].children[0].id.split("_")[2]) &&
                    spans[j].end === Number(document.getElementById("container_german").children[i+2].children[0].id.split("_")[3])) {
                    document.getElementById("container_german").children[i + 2].children[1].style = "border = 2px solid " + spans[i].color;
                    console.log("FOUND");
                    break;
                }
            }
        }

    }

    refreshEditor()
    {
        this.showText("container_english");
        this.showText("container_german");
    }



}
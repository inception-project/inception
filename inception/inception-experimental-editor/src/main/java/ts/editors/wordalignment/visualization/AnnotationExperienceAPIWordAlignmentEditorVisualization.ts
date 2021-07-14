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

    annotationExperienceAPIWordAlignmentEditor : AnnotationExperienceAPIWordAlignmentEditor;
    sentenceCount : number = 0;
    wordBegins : number[];

    CHARACTER_WIDTH = 18;

    constructor(aAnnotationExperienceAPIWordAlignmentEditor : AnnotationExperienceAPIWordAlignmentEditor)
    {
        this.annotationExperienceAPIWordAlignmentEditor = aAnnotationExperienceAPIWordAlignmentEditor;
    }

    showText(aElementId: string)
    {
        let textArea = document.getElementById(aElementId.toString())
        //Reset previous text
        textArea.innerHTML = '';
        //Sentences
        let sent = this.annotationExperienceAPIWordAlignmentEditor.annotationExperienceAPI.text.join("").split("|");
        let words = sent[0].split(".")[0].split(" ");

        let field;
        if (aElementId === "sentence") {
            field = document.getElementById("sent_words")
            this.annotationExperienceAPIWordAlignmentEditor.currentSentence = words;
        } else {
            field = document.getElementById("align_words")
            this.annotationExperienceAPIWordAlignmentEditor.currentAlignment = words;
        }
        field.innerHTML = '';

        //SVG element
        let svg = document.createElementNS("http://www.w3.org/2000/svg", "svg");
        svg.setAttribute("version", "1.2");
        svg.setAttribute("viewBox", "0 0 " + textArea.offsetWidth + " " + 20);
        svg.setAttribute("style","font-size: 150%; width: " + textArea.offsetWidth+ "px; height: " + 40 + "px");

        let textElement = document.createElementNS("http://www.w3.org/2000/svg", "g");
        textElement.setAttribute("class", "sentences");
        textElement.style.display = "block";

        let xPrev: number = 0;

            let sentence = document.createElementNS("http://www.w3.org/2000/svg", "g");
            sentence.setAttribute("class", "text-row");

            for (let j = 0; j < words.length; j++) {
                if (words[j] === "|") {
                    break;
                }
                let word = document.createElementNS("http://www.w3.org/2000/svg", "text");
                word.textContent = words[j];
                word.setAttribute("x", ((this.CHARACTER_WIDTH * words[j].length) + xPrev).toString());
                word.setAttribute("y", "15");
                xPrev += (this.CHARACTER_WIDTH * words[j].length) + 15;
                sentence.appendChild(word);
                sentence.appendChild(this.drawRect(word.getAttribute("x"),this.CHARACTER_WIDTH * words[j].length))

                let space = document.createElementNS("http://www.w3.org/2000/svg", "text");
                space.textContent = " ";
                space.setAttribute("x", (xPrev + this.CHARACTER_WIDTH).toString());
                word.setAttribute("y", "15");
                xPrev += this.CHARACTER_WIDTH;
                sentence.appendChild(space);
                this.addTextField(aElementId, word.getAttribute("x"),this.CHARACTER_WIDTH * words[j].length, j);
            }
            textElement.appendChild(sentence);

        svg.appendChild(textElement);

        textArea.appendChild(svg);

    }

    drawRect(aX: string, width: number) {
       let rect = document.createElementNS("http://www.w3.org/2000/svg", "rect");
       rect.setAttribute("x", (Number(aX) - 4).toString());
       rect.setAttribute("y", "-8");
       rect.setAttribute("width", (width - 5).toString());
       rect.setAttribute("height", "30");
       rect.setAttribute("fill", "none");
       rect.setAttribute("stroke", "black");
       return rect;
    }

    addTextField(aElementId: string, aX: string, width: number, i : number) {
        let field;

        let textField = document.createElement("input");
        if (aElementId === "sentence") {
            field = document.getElementById("sent_words")
            textField.id = "sent_word_id_" + i;
            textField.value =  i.toString();

        } else {
            field = document.getElementById("align_words")
            textField.id = "align_word_id_" + i;
        }
        textField.setAttribute("x", aX);
        textField.setAttribute("y", "0");
        textField.setAttribute("size", "1");
        textField.setAttribute("maxlength", "2");

        field.appendChild(textField)

    }


    refreshEditor(aEditor: string)
    {
        this.showText(aEditor);
    }

}
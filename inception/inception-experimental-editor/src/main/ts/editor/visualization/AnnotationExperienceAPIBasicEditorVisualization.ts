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

import {Span} from "../../../../../inception-api-annotation-experimental/src/main/ts/client/model/Span";
import {AnnotationExperienceAPIBasicEditor} from "../AnnotationExperienceAPIBasicEditor";

export class AnnotationExperienceAPIBasicEditorVisualization {

    annotationExperienceAPIBasicEditor: AnnotationExperienceAPIBasicEditor;

    SENTENCE_OFFSET_WIDTH = 45;
    CHARACTER_WIDTH = 9;

    //Editor element
    lineColorFirst: string = "#BBBBBB";
    lineColorSecond: string = "#CCCCCC";

    //Viewport
    unitBegin: number = 0;
    unitCount: number = 5;

    //Additional drawing
    showHighlighting: boolean = true;
    showUnitNumbers: boolean = true;
    showBackground: boolean = true;

    constructor(aAnnotationExperienceAPIBasicEditor: AnnotationExperienceAPIBasicEditor) {
        this.annotationExperienceAPIBasicEditor = aAnnotationExperienceAPIBasicEditor;
    }

    showText(aElementId: string) {
        let textArea = document.getElementById(aElementId.toString())
        //Reset previous text
        textArea.innerHTML = '';

        //Sentences
        let units = this.annotationExperienceAPIBasicEditor.viewport[0].documentText.split(".");

        //SVG element
        let svg = document.createElementNS("http://www.w3.org/2000/svg", "svg");
        svg.setAttribute("version", "1.2");
        svg.setAttribute("viewBox", "0 0 " + textArea.offsetWidth + " " + this.unitCount * 20);
        svg.setAttribute("style", "font-size: 100%; width: " + textArea.offsetWidth + "px; height: " + this.unitCount * 20 + "px");

        let textElement = document.createElementNS("http://www.w3.org/2000/svg", "g");
        textElement.setAttribute("class", "sentences");
        textElement.style.display = "block";

        let xBegin: number = 0;

        this.unitBegin = Number(document.getElementsByTagName("input")[2].value) - 1

        if (this.unitBegin + this.unitCount > units.length) {
            this.unitCount = units.length - this.unitBegin;
        }

        if (this.showBackground) {
            svg.appendChild(this.drawBackground());
        }

        if (this.showUnitNumbers) {
            svg.appendChild(this.drawUnitNumbers())
            textElement.style.width = (svg.width - this.SENTENCE_OFFSET_WIDTH).toString();
            xBegin = this.SENTENCE_OFFSET_WIDTH;
        }

        let k = 0;

        let xPrev: number = xBegin;


        for (let i = 0; i < this.unitCount; i++) {
            let sentenceOffset = this.calculateInitialOffset(this.unitBegin + i)

            let sentence = document.createElementNS("http://www.w3.org/2000/svg", "g");
            sentence.setAttribute("class", "text-row");
            sentence.setAttribute("sentence-id", (this.unitBegin + i).toString());

            for (let j = 0; j < units[this.unitBegin + i].length; j++, k++) {
                let char = document.createElementNS("http://www.w3.org/2000/svg", "text");
                char.textContent = units[this.unitBegin + i][j];
                char.setAttribute("x", xPrev.toString());
                char.setAttribute("y", ((i + 1) * 20 - 5).toString());
                char.setAttribute("char_pos", (sentenceOffset + j).toString());
                xPrev += this.CHARACTER_WIDTH;
                sentence.appendChild(char);
            }
            textElement.appendChild(sentence);
            xPrev = xBegin;
        }

        svg.appendChild(textElement);


        textArea.appendChild(svg);


        //Highlighting
        if (this.showHighlighting) {
            svg.appendChild(this.drawAnnotation(this.annotationExperienceAPIBasicEditor.viewport[0].spans, aElementId));
        }
    }

    drawBackground() {
        //Background
        let background = document.createElementNS("http://www.w3.org/2000/svg", "g");
        background.setAttribute("class", "background")
        background.innerHTML = "";

        for (let i = 0; i < this.unitCount; i++) {
            let rect = document.createElementNS("http://www.w3.org/2000/svg", "rect");
            rect.setAttribute("x", "0");
            rect.setAttribute("y", (i * 20).toString());
            rect.setAttribute("width", "100%");
            rect.setAttribute("height", "20");
            if (i % 2 == 0) {
                rect.setAttribute("fill", this.lineColorFirst);
            } else {
                rect.setAttribute("fill", this.lineColorSecond);
            }
            background.appendChild(rect);
        }
        return background;
    }

    calculateInitialOffset(aUnitNumber: Number) {
        let offset: number = 0;
        let units = this.annotationExperienceAPIBasicEditor.viewport[0].documentText.split(".");
        for (let i = 0; i < aUnitNumber; i++) {
            let words = units[i].split(" ");
            for (let j = 0; j < words.length; j++) {
                offset += words[j].length + 1;
            }
            offset++;
        }
        return offset;
    }

    drawUnitNumbers() {
        //Unitnumbers
        let unitNumbers = document.createElementNS("http://www.w3.org/2000/svg", "g");
        unitNumbers.setAttribute("class", "sentence_numbers");
        unitNumbers.style.display = "block";
        unitNumbers.innerHTML = "";

        for (let i = 0; i < this.unitCount; i++) {
            let number = document.createElementNS("http://www.w3.org/2000/svg", "text");
            number.textContent = (this.unitBegin + i + 1).toString() + "."
            number.setAttribute("x", "10");
            number.setAttribute("y", ((i + 1) * 20 - 5).toString());
            unitNumbers.appendChild(number);
        }
        return unitNumbers;

    }

    drawAnnotation(aAnnotations: Span[], aEditor: string) {
        let highlighting = document.createElementNS("http://www.w3.org/2000/svg", "g");
        highlighting.setAttribute("class", "annotations")
        highlighting.style.display = "block";
        highlighting.innerHTML = "";

        console.log(aAnnotations)

        if (aAnnotations.length > 0) {
            let units = this.annotationExperienceAPIBasicEditor.viewport[0].documentText;
            this.unitCount = units.length - 1;
            let i = 0;

            let childElement = 0;

            if (this.showBackground) {
                childElement++;
            }
            if (this.showUnitNumbers) {
                childElement++;
            }

            for (let child of document.getElementById(aEditor).children[0].children[childElement].children) {
                let text_row = document.createElementNS("http://www.w3.org/2000/svg", "g");
                text_row.setAttribute("class", "span")

                for (let span of aAnnotations) {

                    let begin: string;
                    let end: string;
                    let check: boolean = false;

                    let word: String = "";


                    console.log("CURRENT ANNOTATION: " + span.begin + ", " + span.end)
                    for (let char of child.children) {

                        console.log("POS: " + char.getAttribute("char_pos"))


                        if (span.begin.toString() === char.getAttribute("char_pos")) {
                            begin = char.getAttribute("x");
                            word = word.concat(char.textContent);
                            check = true;
                            console.log("FOUND (1)")
                            continue;
                        }

                        if (span.end.toString() === char.getAttribute("char_pos")) {

                            console.log("FOUND (2)")
                            end = char.getAttribute("x")
                            let rect = document.createElementNS("http://www.w3.org/2000/svg", "rect");
                            rect.setAttribute("x", Number(begin).toString());
                            rect.setAttribute("y", (i * 20).toString());
                            rect.setAttribute("width", (Number(end) - Number(begin)).toString());
                            rect.setAttribute("height", "20");
                            rect.setAttribute("id", String(span.id));
                            rect.setAttribute("layerId", String(span.layerId));
                            rect.setAttribute("fill", span.color);
                            rect.style.opacity = "0.5";
                            text_row.appendChild(rect);

                        } else {

                            if (check) {
                                word = word.concat(char.textContent);
                            }
                        }
                    }
                }

                highlighting.appendChild(text_row);
                i++;
            }
            return highlighting;
        } else {
            return highlighting;
        }
    }

    setLineColors(aLineColorFirst: string, aLineColorSecond: string, aEditor: string) {
        this.lineColorFirst = aLineColorFirst;
        this.lineColorSecond = aLineColorSecond;

        this.refreshEditor(aEditor);
    }

    resetLineColor(aEditor: string) {
        this.lineColorFirst = "#BBBBBB";
        this.lineColorSecond = "#CCCCCC"

        this.refreshEditor(aEditor);
    }

    refreshEditor(aEditor: string) {
        this.showText(aEditor);
    }

    setShowHighlighting(aShowHighlighting: boolean, aEditor: string) {
        this.showHighlighting = aShowHighlighting;
        this.refreshEditor(aEditor);
    }

    setShowSentenceNumbers(aShowSentenceNumbers: boolean, aEditor: string) {
        this.showUnitNumbers = aShowSentenceNumbers;
        this.refreshEditor(aEditor);
    }

    setShowHighLighting(aHighlightingEnabled: boolean, aEditor: string) {
        this.showHighlighting = aHighlightingEnabled;
        this.showText(aEditor);
    }
}
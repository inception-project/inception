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
import {Annotation} from "../../../../../../inception-api-annotation-experimental/src/main/ts/client/util/Annotation";
import {AnnotationType} from "../../../../../../inception-api-annotation-experimental/src/main/ts/client/util/AnnotationTypes";
import {AnnotationExperienceAPI} from "../../../../../../inception-api-annotation-experimental/src/main/ts/client/AnnotationExperienceAPI";

export class AnnotationExperienceAPIVisualization {

    annotationExperienceAPI : AnnotationExperienceAPI;

    SENTENCE_OFFSET_WIDTH = 45;
    CHARACTER_WIDTH = 9;

    //Editor element
    lineColorFirst: string = "#BBBBBB";
    lineColorSecond: string = "#CCCCCC";

    //Viewport
    sentenceCount: number;

    //Additional drawing
    showHighlighting: boolean = true;
    showSentenceNumbers: boolean = true;
    showBackground: boolean = true;

    constructor(aAnnotationExperience : AnnotationExperienceAPI)
    {
        this.annotationExperienceAPI = aAnnotationExperience;
    }

    showText(aElementId: string) {
        let textArea = document.getElementById(aElementId.toString())
        //Reset previous text
        textArea.innerHTML = '';

        //Sentences
        let sentences = this.annotationExperienceAPI.text.join("").split("|");
        this.sentenceCount = sentences.length - 1;

        //SVG element
        let svg = document.createElementNS("http://www.w3.org/2000/svg", "svg");
        svg.setAttribute("version", "1.2");
        svg.setAttribute("viewBox", "0 0 " + textArea.offsetWidth + " " + this.sentenceCount * 20);
        svg.setAttribute("style","font-size: 100%; width: " + textArea.offsetWidth+ "px; height: " + this.sentenceCount * 20 + "px");

        let textElement = document.createElementNS("http://www.w3.org/2000/svg", "g");
        textElement.setAttribute("class", "sentences");
        textElement.style.display = "block";

        let xBegin : number = 0;

        if (this.showBackground) {
            svg.appendChild(this.drawBackground());
        }

        if (this.showSentenceNumbers) {
            svg.appendChild(this.drawSentenceNumbers())
            textElement.style.width = (svg.width - this.SENTENCE_OFFSET_WIDTH).toString();
            xBegin = this.SENTENCE_OFFSET_WIDTH;
        }

        let k = 0;

        let xPrev: number = xBegin;

        for (let i = 0; i < sentences.length; i++) {

            let sentence = document.createElementNS("http://www.w3.org/2000/svg", "g");
            sentence.setAttribute("class", "text-row");
            sentence.setAttribute("sentence-id", (i + 1).toString());

            for (let j = 0; j < sentences[i].length; j++, k++) {
                if (sentences[i][j] === "|") {
                    break;
                }
                let char = document.createElementNS("http://www.w3.org/2000/svg", "text");
                char.textContent = sentences[i][j];
                char.setAttribute("x", xPrev.toString());
                char.setAttribute("y", ((i + 1) * 20 - 5).toString());
                char.setAttribute("char_pos", (this.annotationExperienceAPI.viewport[i][0] + j).toString());
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
            svg.appendChild(this.drawAnnotation(this.annotationExperienceAPI.annotations, aElementId));
        }
    }

    drawBackground() {
        //Background
        let background = document.createElementNS("http://www.w3.org/2000/svg", "g");
        background.setAttribute("class","background")
        background.innerHTML = "";

        for (let i = 0; i < this.sentenceCount; i++) {
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

    drawSentenceNumbers() {
        //Sentencenumbers
        let sentenceNumbers = document.createElementNS("http://www.w3.org/2000/svg", "g");
        sentenceNumbers.setAttribute("class","sentence_numbers");
        sentenceNumbers.style.display = "block";
        sentenceNumbers.innerHTML = "";

        for (let i = 0; i < this.sentenceCount; i++) {
            let number = document.createElementNS("http://www.w3.org/2000/svg", "text");
            number.textContent = (this.sentenceCount + i + 1).toString() + "."
            number.setAttribute("x", "10");
            number.setAttribute("y", ((i + 1) * 20 - 5).toString());
            sentenceNumbers.appendChild(number);
        }
        return sentenceNumbers;

    }

    drawAnnotation(aAnnotations: Annotation[], aEditor: string) {
        let highlighting = document.createElementNS("http://www.w3.org/2000/svg", "g");
        highlighting.setAttribute("class","annotations")
        highlighting.style.display = "block";
        highlighting.innerHTML = "";

        if (aAnnotations.length > 0) {

            let offset: number;
            if (this.showSentenceNumbers) {
                offset = 45;
            } else {
                offset = 4;
            }

            let sentences = this.annotationExperienceAPI.text.join(" ").split("|");
            this.sentenceCount = sentences.length - 1;
            let i = 0;

            let childElement = 0;

            if (this.showBackground) {
                childElement++;
            }
            if (this.showSentenceNumbers) {
                childElement++;
            }

            for (let child of document.getElementById(aEditor).children[0].children[childElement].children) {
                let text_row = document.createElementNS("http://www.w3.org/2000/svg", "g");
                text_row.setAttribute("class", "span")

                for (let annotation of this.annotationExperienceAPI.annotations) {

                    let begin: string;
                    let end: string;
                    let check : boolean = false;

                    let word : String = "";


                    for (let char of child.children) {

                        if (annotation.begin.toString() === char.getAttribute("char_pos")) {
                            begin = char.getAttribute("x");
                            word = word.concat(char.textContent);
                            check = true;
                            continue;
                        }

                        if (annotation.end.toString() === char.getAttribute("char_pos")) {

                            if (begin != null && word === annotation.word) {
                                end = char.getAttribute("x")
                                let rect = document.createElementNS("http://www.w3.org/2000/svg", "rect");
                                rect.setAttribute("x", Number(begin).toString());
                                rect.setAttribute("y", (i * 20).toString());
                                rect.setAttribute("width", (Number(end) - Number(begin)).toString());
                                rect.setAttribute("height", "20");
                                rect.setAttribute("id", annotation.id);
                                rect.setAttribute("type", annotation.type);
                                rect.setAttribute("fill", this.getColorForAnnotation(annotation.type));
                                rect.style.opacity = "0.5";
                                text_row.appendChild(rect);
                                break;
                            } else {
                                break;
                            }
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

    getColorForAnnotation(aType: string) {
        switch (aType) {
            case AnnotationType.POS:
                return "#0088FF";
            case AnnotationType.CHUNK:
                return "#4466AA";
            case AnnotationType.LEMMA:
                return "#04CCCC";
            case AnnotationType.NER:
                return "#228822";
            case AnnotationType.COREFERENCE:
                return "#934AA1"
            default:
                return "#AAAAAA";
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
        this.showSentenceNumbers = aShowSentenceNumbers;
        this.refreshEditor(aEditor);
    }

    setShowHighLighting(aHighlightingEnabled: boolean, aEditor: string) {
        this.showHighlighting = aHighlightingEnabled;
        this.showText(aEditor);
    }
}
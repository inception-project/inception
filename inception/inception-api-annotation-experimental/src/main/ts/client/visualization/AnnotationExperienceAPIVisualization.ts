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
import {Annotation} from "../util/Annotation";


export class AnnotationExperienceAPIVisualization {
    //Text
    text: String[];
    annotations: Annotation[];

    //Editor element
    lineColorFirst: string = "#BBBBBB";
    lineColorSecond: string = "#CCCCCC";

    //Viewport
    sentenceCount: number;
    viewport: number[][];

    //Additional drawing
    showHighlighting: boolean = false;
    showSentenceNumbers: boolean = false;
    showBackground: boolean = false;

    showText(aElementId: string) {
        let textArea = document.getElementById(aElementId.toString())
        //Reset previous text
        textArea.innerHTML = '';

        //Sentences
        let sentences = this.text.join("").split("|");
        this.sentenceCount = sentences.length - 1;

        //SVG element
        let svg = document.createElement("svg");
        svg.setAttribute("version", "1.2");
        svg.setAttribute("viewBox", "0 0 " + textArea.offsetWidth + " " + this.sentenceCount * 20);
        svg.style.fontSize="100%";
        svg.style.width="100%";
        svg.style.height="100%";

        if (this.showBackground) {
            svg.appendChild(this.drawBackground());
        }

        if (this.showSentenceNumbers) {
            svg.appendChild(this.drawSentenceNumbers())
        }

        let k = 0;

        let textElement = document.createElement("g");
        textElement.className = "text";


        for (let i = 0; i < sentences.length; i++) {
            let sentence = document.createElement("g");
            sentence.className = "text-row";
            sentence.style.display = "block";
            sentence.setAttribute("sentence-id", (i + 1).toString());

            let xPrev: number;
            if (this.showSentenceNumbers) {
                xPrev = 45;
            } else {
                xPrev = 4;
            }

            for (let j = 0; j < sentences[i].length; j++, k++) {

                if (sentences[i][j] === "|") {
                    break;
                }

                let char = document.createElement("text");
                char.innerText = sentences[i][j];
                char.className = "char";
                char.setAttribute("x", xPrev.toString());
                char.setAttribute("y", ((i + 1) * 20 - 5).toString());
                char.setAttribute("char_pos", (this.viewport[i][0] + j).toString());
                xPrev += 9;
                sentence.appendChild(char);
            }
            textElement.appendChild(sentence);
        }

        svg.appendChild(textElement);


        //Highlighting
        if (this.showHighlighting) {
            svg.appendChild(this.drawAnnotation(this.annotations, aElementId));
        }

        textArea.appendChild(svg);
    }

    drawBackground() {
        //Background
        let background = document.createElement("g");
        background.className = "background";
        background.innerHTML = "";

        for (let i = 0; i < this.sentenceCount; i++) {
            let rect = document.createElement("rect");
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
        let sentenceNumbers = document.createElement("g");
        sentenceNumbers.className = "sentence-numbers";
        sentenceNumbers.style.display = "block";
        sentenceNumbers.innerHTML = "";

        for (let i = 0; i < this.sentenceCount; i++) {
            let number = document.createElement("text")
            number.className = "sn";
            number.innerText = (this.sentenceCount + i + 1).toString() + "."
            number.setAttribute("x", "10");
            number.setAttribute("y", ((i + 1) * 20 - 5).toString());
            sentenceNumbers.appendChild(number);
        }
        return sentenceNumbers;

    }

    drawAnnotation(aAnnotations: Annotation[], aEditor: string) {
        let highlighting = document.createElement("g");
        highlighting.className = "highlighting";
        highlighting.innerHTML = "";

        if (aAnnotations.length > 0) {

            let offset: number;
            if (this.showSentenceNumbers) {
                offset = 45;
            } else {
                offset = 4;
            }

            let sentences = this.text.join(" ").split("||");
            this.sentenceCount = sentences.length - 1;
            let i = 0;

            console.log(document.getElementsByClassName("text-row"))

            for (let sent of document.getElementsByClassName("text-row")) {
                console.log("SENT: " + i)
                let text_row = document.createElement("g");
                text_row.className = "annotation";
                text_row.style.display = "block";

                for (let annotation of this.annotations) {

                    let begin: string;
                    let end: string;

                    for (let char of sent.children) {
                        if (annotation.begin.toString() === char.getAttribute("char_pos")) {
                            begin = char.getAttribute("x");
                            continue;
                        }

                        if (annotation.end.toString() === char.getAttribute("char_pos")) {
                            end = char.getAttribute("x")
                            let rect = document.createElement("rect");
                            rect.setAttribute("x", (document.getElementById(aEditor).offsetWidth + Number(begin) + offset).toString());
                            rect.setAttribute("y", (i * 20).toString());
                            rect.setAttribute("width", (Number(end) - Number(begin)).toString());
                            rect.setAttribute("height", "20");
                            rect.setAttribute("id", annotation.id);
                            rect.setAttribute("type", annotation.type);
                            rect.setAttribute("fill", this.getColorForAnnotation(annotation.type));
                            rect.style.opacity = "0.5";
                            text_row.appendChild(rect);
                            break;
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
        return "#87CEEB";
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

    setText(aText: string[]) {
        this.text = aText;
    }

    setAnnotations(aAnnotations: Annotation[]) {
        this.annotations = aAnnotations;
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
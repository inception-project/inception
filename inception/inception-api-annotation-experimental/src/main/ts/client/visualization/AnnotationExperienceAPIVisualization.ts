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
export class AnnotationExperienceAPIVisualization {
    //Text
    text: String[];
    showSentenceNumbers: boolean = false;
    showBackground: boolean = false;
    annotations: Object[];

    //Editor element
    lineColorFirst: string = "#BBBBBB";
    lineColorSecond: string = "#CCCCCC";

    //Viewport
    viewport: number[][];
    sentenceCount: number;

    //Highlighting
    highlightingEnabled: boolean = false;

    showText(aElementId: string)
    {
        let textArea = document.getElementById(aElementId.toString())
        //Reset previous text
        textArea.innerHTML = '';

        //SVG element
        let svg = document.createElement("svg");
        svg.setAttribute("version", "1.2");
        svg.setAttribute("viewbox", "0 0 " + textArea.offsetWidth + " " + this.text.length * 20);
        svg.style.display = "font-size: 100%; width: 100%; height: 100%";

        if (this.showBackground) {
            svg.appendChild(this.createBackground());
        }

        if (this.showSentenceNumbers) {
            svg.appendChild(this.createSentenceNumbers())
        }

        let k = 0;

        let textElement = document.createElement("g");
        textElement.className = "text";
        textElement.style.display = "font-size: 100%; width: 100%; height: 100%";

        let sentences = this.text.join(" ").split("||");
        this.sentenceCount = sentences.length - 1 ;

        for (let i = 0; i < sentences.length; i++) {
            let sentence = document.createElement("g");
            sentence.className = "text-row";
            sentence.style.display = "block";
            sentence.setAttribute("sentence-id", (i + 1).toString());

            let spaceElement = document.createElement("text");
            spaceElement.className = "space";
            spaceElement.innerText = " ";
            spaceElement.setAttribute("x", "0");
            spaceElement.setAttribute("y", ((i + 1) * 20 - 5).toString());
            spaceElement.setAttribute("word_id", k.toString());

            sentence.appendChild(spaceElement);
            let xPrev: number;
            if (this.showSentenceNumbers) {
                xPrev = 45;
            } else {
                xPrev = 4;
            }

            let sent = sentences[i].split(" ");

            for (let j = 0; j < sent.length; j++, k++) {

                if (sent[j] === "||") {
                    break;
                }

                let word = document.createElement("text");
                word.innerText = sent[j]
                word.className = "word";
                word.setAttribute("x", xPrev.toString());
                word.setAttribute("y", ((i + 1) * 20 - 5).toString());
                word.setAttribute("word_id", k.toString());
                xPrev += word.innerText.length * 9;
                sentence.appendChild(word);

                if (j != this.text.length - 1) {
                    spaceElement = document.createElement("text");
                    spaceElement.className = "space";
                    spaceElement.innerText = " ";
                    spaceElement.setAttribute("x", xPrev.toString());
                    spaceElement.setAttribute("y", ((i + 1) * 20 - 5).toString());
                    spaceElement.setAttribute("word_id", k.toString());
                    xPrev += 4;
                    sentence.appendChild(spaceElement);
                }
            }
            textElement.appendChild(sentence);
        }

        svg.appendChild(textElement);


        //Highlighting
        if (this.highlightingEnabled) {
            svg.appendChild(this.drawAnnotation(this.annotations, aElementId));
        }

        textArea.appendChild(svg);
    }

    setShowSentenceNumbers(aShowSentenceNumbers: boolean, aEditor: string) {
        this.showSentenceNumbers = aShowSentenceNumbers;
        this.refreshEditor(aEditor);
    }

    setHighLighting(aHighlightingEnabled: boolean, aEditor: string)
    {
        this.highlightingEnabled = aHighlightingEnabled;
        this.refreshEditor(aEditor);
    }

    createSentenceNumbers() {
        //Sentencenumbers
        let sentenceNumbers = document.createElement("g");
        sentenceNumbers.className = "text";
        sentenceNumbers.style.display = "font-size: 100%; width: 100%; height: 100%";

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

    createBackground() {
        //Background
        let background = document.createElement("g");
        background.className = "background";
        background.style.display = "font-size: 100%; width: 100%; height: 100%";

        for (let i = 0; i < this.sentenceCount; i++) {
            let rect = document.createElement("rect");
            rect.setAttribute("x", "0");
            rect.setAttribute("y", (i * 20).toString());
            rect.setAttribute("width", "100%");
            rect.setAttribute("height", "20");
            if (i % 2 == 0) {
                rect.setAttribute("fill", this.lineColorFirst);
            } else{
                rect.setAttribute("fill", this.lineColorSecond);
            }
            background.appendChild(rect);
        }
        return background;
    }

    drawAnnotation(aAnnotations: Object[], aEditor: string) {
        let highlighting = document.createElement("g");
        highlighting.className = "highlighting";
        highlighting.style.display = "font-size: 100%; width: 100%; height: 100%";

        highlighting.innerHTML = "";

        let editor = document.createElement(aEditor);

        if (aAnnotations.length > 0) {
            //Parse data
            let keys = Object.keys(aAnnotations)
            let values = keys.map(k => aAnnotations[k])

            let offset: number;
            if (this.showSentenceNumbers) {
                offset = 45;
            } else {
                offset = 4;
            }

            for (let val of values) {
                let annotation = document.createElement("g");
                annotation.className = "annotation";

                let rect = document.createElement("rect");
                rect.setAttribute("x", (editor.offsetWidth + (Number(val.begin * 8) + offset)).toString());
                rect.setAttribute("y", "0");
                rect.setAttribute("width", (Number(val.word.length * 8).toString()));
                rect.setAttribute("height", "20");
                rect.setAttribute("id", val.id);
                rect.setAttribute("type", val.type);
                rect.setAttribute("fill", this.getColorForAnnotation(val.type));
                rect.style.opacity = "0.5";

                annotation.appendChild(rect);
                highlighting.appendChild(annotation);
            }
            return highlighting;
        } else {
            return highlighting;
        }
    }

    getColorForAnnotation(type: string) {
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
        let editor = document.getElementById("textarea");
        let content = editor.innerHTML;
        editor.innerHTML = content;

    }

    setText(aText: string[]) {
        this.text = aText;
    }

    setAnnotations(aAnnotations: Object[]) {
        this.annotations = aAnnotations;
    }
}
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
var __defProp = Object.defineProperty;
var __markAsModule = (target) => __defProp(target, "__esModule", {value: true});
var __export = (target, all) => {
  for (var name in all)
    __defProp(target, name, {get: all[name], enumerable: true});
};

// main/editors/basic/AnnotationExperienceAPIBasicEditor.ts
__markAsModule(exports);
__export(exports, {
  AnnotationExperienceAPIBasicEditor: () => AnnotationExperienceAPIBasicEditor
});

// main/editors/basic/visualization/AnnotationExperienceAPIBasicEditorVisualization.ts
var AnnotationExperienceAPIBasicEditorVisualization = class {
  constructor(aAnnotationExperienceAPIBasicEditor) {
    this.SENTENCE_OFFSET_WIDTH = 45;
    this.CHARACTER_WIDTH = 9;
    this.lineColorFirst = "#BBBBBB";
    this.lineColorSecond = "#CCCCCC";
    this.unitBegin = 0;
    this.unitCount = 5;
    this.showHighlighting = true;
    this.showUnitNumbers = true;
    this.showBackground = true;
    this.annotationExperienceAPIBasicEditor = aAnnotationExperienceAPIBasicEditor;
  }
  showText(aElementId) {
    let textArea = document.getElementById(aElementId.toString());
    textArea.innerHTML = "";
    let units = this.annotationExperienceAPIBasicEditor.annotationExperienceAPI.viewport.documentText.split(".");
    let svg = document.createElementNS("http://www.w3.org/2000/svg", "svg");
    svg.setAttribute("version", "1.2");
    svg.setAttribute("viewBox", "0 0 " + textArea.offsetWidth + " " + this.unitCount * 20);
    svg.setAttribute("style", "font-size: 100%; width: " + textArea.offsetWidth + "px; height: " + this.unitCount * 20 + "px");
    let textElement = document.createElementNS("http://www.w3.org/2000/svg", "g");
    textElement.setAttribute("class", "sentences");
    textElement.style.display = "block";
    let xBegin = 0;
    this.unitBegin = Number(document.getElementsByTagName("input")[2].value) - 1;
    if (this.unitBegin + this.unitCount > units.length) {
      this.unitCount = units.length - this.unitBegin;
    }
    if (this.showBackground) {
      svg.appendChild(this.drawBackground());
    }
    if (this.showUnitNumbers) {
      svg.appendChild(this.drawUnitNumbers());
      textElement.style.width = (svg.width - this.SENTENCE_OFFSET_WIDTH).toString();
      xBegin = this.SENTENCE_OFFSET_WIDTH;
    }
    let k = 0;
    let xPrev = xBegin;
    for (let i = 0; i < this.unitCount; i++) {
      let sentenceOffset = this.calculateInitialOffset(this.unitBegin + i);
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
    if (this.showHighlighting) {
      svg.appendChild(this.drawAnnotation(this.annotationExperienceAPIBasicEditor.annotationExperienceAPI.spans, aElementId));
    }
  }
  drawBackground() {
    let background = document.createElementNS("http://www.w3.org/2000/svg", "g");
    background.setAttribute("class", "background");
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
  calculateInitialOffset(aUnitNumber) {
    let offset = 0;
    let units = this.annotationExperienceAPIBasicEditor.annotationExperienceAPI.viewport.documentText.split(".");
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
    let unitNumbers = document.createElementNS("http://www.w3.org/2000/svg", "g");
    unitNumbers.setAttribute("class", "sentence_numbers");
    unitNumbers.display = "block";
    unitNumbers.innerHTML = "";
    for (let i = 0; i < this.unitCount; i++) {
      let number = document.createElementNS("http://www.w3.org/2000/svg", "text");
      number.textContent = (this.unitBegin + i + 1).toString() + ".";
      number.setAttribute("x", "10");
      number.setAttribute("y", ((i + 1) * 20 - 5).toString());
      unitNumbers.appendChild(number);
    }
    return unitNumbers;
  }
  drawAnnotation(aAnnotations, aEditor) {
    let highlighting = document.createElementNS("http://www.w3.org/2000/svg", "g");
    highlighting.setAttribute("class", "annotations");
    highlighting.style.display = "block";
    highlighting.innerHTML = "";
    if (aAnnotations.length > 0) {
      let units = this.annotationExperienceAPIBasicEditor.annotationExperienceAPI.viewport.documentText;
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
        text_row.setAttribute("class", "span");
        for (let span of aAnnotations) {
          let begin;
          let end;
          let check = false;
          let word = "";
          for (let char of child.children) {
            if (span.begin.toString() === char.getAttribute("char_pos")) {
              begin = char.getAttribute("x");
              word = word.concat(char.textContent);
              check = true;
              continue;
            }
            if (span.end.toString() === char.getAttribute("char_pos")) {
              if (begin != null && word === span.coveredText) {
                end = char.getAttribute("x");
                let rect = document.createElementNS("http://www.w3.org/2000/svg", "rect");
                rect.setAttribute("x", Number(begin).toString());
                rect.setAttribute("y", (i * 20).toString());
                rect.setAttribute("width", (Number(end) - Number(begin)).toString());
                rect.setAttribute("height", "20");
                rect.setAttribute("id", String(span.id));
                rect.setAttribute("type", span.type);
                rect.setAttribute("fill", span.color);
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
  setLineColors(aLineColorFirst, aLineColorSecond, aEditor) {
    this.lineColorFirst = aLineColorFirst;
    this.lineColorSecond = aLineColorSecond;
    this.refreshEditor(aEditor);
  }
  resetLineColor(aEditor) {
    this.lineColorFirst = "#BBBBBB";
    this.lineColorSecond = "#CCCCCC";
    this.refreshEditor(aEditor);
  }
  refreshEditor(aEditor) {
    this.showText(aEditor);
  }
  setShowHighlighting(aShowHighlighting, aEditor) {
    this.showHighlighting = aShowHighlighting;
    this.refreshEditor(aEditor);
  }
  setShowSentenceNumbers(aShowSentenceNumbers, aEditor) {
    this.showUnitNumbers = aShowSentenceNumbers;
    this.refreshEditor(aEditor);
  }
  setShowHighLighting(aHighlightingEnabled, aEditor) {
    this.showHighlighting = aHighlightingEnabled;
    this.showText(aEditor);
  }
};

// main/editors/basic/action/AnnotationExperienceAPIBasicEditorActionHandler.ts
var AnnotationExperienceAPIBasicEditorActionHandler = class {
  constructor(annotationExperienceAPIBasicEditor) {
    this.annotationExperienceAPIBasicEditor = annotationExperienceAPIBasicEditor;
    this.registerDefaultActionHandler();
  }
  registerDefaultActionHandler() {
    let that = this;
    onclick = function(aEvent) {
      let elem = aEvent.target;
      if (elem.className === "far fa-caret-square-right" || "far fa-caret-square-left") {
        setTimeout(function() {
          that.annotationExperienceAPIBasicEditor.annotationExperienceAPI.requestDocument(that.annotationExperienceAPIBasicEditor.annotationExperienceAPI.clientName, that.annotationExperienceAPIBasicEditor.annotationExperienceAPI.projectID, Number(document.location.href.split("=")[1].split("&")[0]));
          setTimeout(function() {
            that.sentences = that.annotationExperienceAPIBasicEditor.annotationExperienceAPI.viewport.documentText.split(".");
            that.annotationExperienceAPIBasicEditor.annotationExperienceAPIVisualization.refreshEditor("textarea");
          }, 2e3);
        }, 200);
      }
      if (elem.className === "fas fa-step-forward" || elem.className === "fas fa-step-backward") {
        setTimeout(function() {
          that.annotationExperienceAPIBasicEditor.annotationExperienceAPIVisualization.refreshEditor("textarea");
        }, 200);
      }
    };
  }
};

// main/editors/basic/AnnotationExperienceAPIBasicEditor.ts
var AnnotationExperienceAPIBasicEditor = class {
  constructor() {
    this.annotationExperienceAPIVisualization = new AnnotationExperienceAPIBasicEditorVisualization(this);
    this.annotationExperienceAPIActionHandler = new AnnotationExperienceAPIBasicEditorActionHandler(this);
  }
};

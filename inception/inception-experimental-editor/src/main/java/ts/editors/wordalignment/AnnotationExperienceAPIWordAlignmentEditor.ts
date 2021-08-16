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

    currentSentence : number = 0;
    oddSentence: string;
    oddSentenceOffset: number = 0;
    evenSentence: string;
    evenSentenceOffset: number = 0;
    spanType : string;
    relationType : string;

    constructor()
    {
        this.annotationExperienceAPI = new AnnotationExperienceAPIImpl();
        this.annotationExperienceAPIVisualization = new AnnotationExperienceAPIWordAlignmentEditorVisualization(this);
        this.annotationExperienceAPIWordAlignmentEditorActionHandler = new AnnotationExperienceAPIWordAlignmentEditorActionHandler(this);
    }

    saveAlignments()
    {
        let pairs = []
        const that = this;

        if (!this.inputsValid) {
            alert("Word alignment is not 1:1.")
            return;
        }

        let oddUnitContainerSize =  document.getElementById("odd_unit_container").children.length - 2;
        let evenUnitContainerSize = document.getElementById("even_unit_container").children.length - 2;

        for (let i = 0; i < oddUnitContainerSize; i++) {
            for (let j = 0; j < evenUnitContainerSize; j++) {

                let oddUnitContainerElementInputValue = document.getElementById("odd_unit_container").children[i + 2].children[0].value;
                let evenUnitContainerElementInpupValue = document.getElementById("even_unit_container").children[j + 2].children[1].value;

                if (oddUnitContainerElementInputValue === evenUnitContainerElementInpupValue) {

                    let oddUnitContainerElementText = document.getElementById("odd_unit_container").children[i + 2].children[1];
                    let evenUnitContainerElementText = document.getElementById("even_unit_container").children[j + 2].children[0];

                    let oddUnitContainerElementTextId = oddUnitContainerElementText.id.split("_");
                    let evenUnitContainerElementTextId = evenUnitContainerElementText.id.split("_");

                    let selectedSpanLayer =  document.getElementsByClassName("filter-option-inner-inner")[0].innerText;

                    pairs.push([
                        oddUnitContainerElementTextId[2],
                        oddUnitContainerElementText.innerText,
                        evenUnitContainerElementTextId[2],
                        evenUnitContainerElementText.innerText]
                    );

                    this.createSpanRequest(oddUnitContainerElementTextId[2], oddUnitContainerElementTextId[3], selectedSpanLayer);
                    this.createSpanRequest(evenUnitContainerElementTextId[2], evenUnitContainerElementTextId[3], selectedSpanLayer);

                    document.getElementById("even_unit_container").children[j + 2].children[1].setAttribute("disabled", "true");
                }
            }
        }

        setTimeout(function () {
            let source, target;
            for (let i = 0; i < pairs.length; i++) {
                for (let j = 0; j < that.annotationExperienceAPI.spans.length; j++) {
                    if (pairs[i][0] == that.annotationExperienceAPI.spans[j].begin &&
                        pairs[i][1] == that.annotationExperienceAPI.spans[j].coveredText) {
                        source = that.annotationExperienceAPI.spans[j];
                    }
                    if (pairs[i][2] == that.annotationExperienceAPI.spans[j].begin &&
                        pairs[i][3] == that.annotationExperienceAPI.spans[j].coveredText) {
                        target = that.annotationExperienceAPI.spans[j];
                    }
                }
                that.annotationExperienceAPI.requestCreateArc(
                    that.annotationExperienceAPI.clientName,
                    that.annotationExperienceAPI.projectID,
                    that.annotationExperienceAPI.documentID,
                    source.id,
                    target.id,
                    that.relationType)
            }
        }, 5000)

        document.getElementById("save_alignment").disabled = true

    }

    inputsValid()
    {
        let values = []
        if (!document.getElementById("multipleSelect").checked) {
            for (let i = 0; i < document.getElementById("even_unit_container").children.length - 2; i++) {
                if (values.indexOf(document.getElementById("even_unit_container").children[i + 2].children[1].value) > -1) {
                    return false;
                }
                values.push(document.getElementById("even_unit_container").children[i + 2].children[1].value)
            }
        }
        return true;
    }

    createSpanRequest(aBegin: string, aEnd : string, aLayer: string)
    {
        let that = this;

        this.annotationExperienceAPI.requestCreateSpan(
            that.annotationExperienceAPI.clientName,
            that.annotationExperienceAPI.projectID,
            that.annotationExperienceAPI.documentID,
            Number(aBegin),
            Number(aEnd),
            aLayer);
    }

    resetAlignments()
    {

        let that = this;

        for (let i = 0; i < this.annotationExperienceAPI.arcs.length; i++) {
            this.annotationExperienceAPI.requestDeleteArc(
                that.annotationExperienceAPI.clientName,
                that.annotationExperienceAPI.projectID,
                that.annotationExperienceAPI.documentID,
                that.annotationExperienceAPI.arcs[i].id,
                that.annotationExperienceAPI.arcs[i].type
            )
        }

        for (let i = 0; i < this.annotationExperienceAPI.spans.length; i++) {
            this.annotationExperienceAPI.requestDeleteSpan(
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
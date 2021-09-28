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
import {AnnotationExperienceAPIImpl} from "../../../../inception-api-annotation-experimental/src/main/ts/client/AnnotationExperienceAPIImpl";
import {AnnotationExperienceAPI} from "../../../../inception-api-annotation-experimental/src/main/ts/client/AnnotationExperienceAPI";
import {Viewport} from "../../../../inception-api-annotation-experimental/src/main/ts/client/model/Viewport";
import {Observer} from "../../../../inception-api-annotation-experimental/src/main/ts/client/Observer";
import {AnnotationExperienceAPIWordAlignmentEditorVisualization} from "./visualization/AnnotationExperienceAPIWordAlignmentEditorVisualization";
import {AnnotationExperienceAPIWordAlignmentEditorActionHandler} from "./action/AnnotationExperienceAPIWordAlignmentEditorActionHandler";

export class WordAlignmentEditor implements Observer
{

    annotationExperienceAPI: AnnotationExperienceAPIImpl;
    annotationExperienceAPIVisualization: AnnotationExperienceAPIWordAlignmentEditorVisualization;
    annotationExperienceAPIWordAlignmentEditorActionHandler: AnnotationExperienceAPIWordAlignmentEditorActionHandler

    //States
    projectId: number;
    annotatorName: string;

    layers: [number,string][];

    viewport : Viewport[] = [];
    sentences : string[];

    oddSentence: string;
    oddSentenceOffset: number = 0;
    evenSentence: string;
    evenSentenceOffset: number = 1;
    spanType : number;
    arcType : number = 279;

    constructor(aProjectId: number, aDocumentId: number, aAnnotatorName: string, aUrl: string, aLayers: [number,string][])
    {
        this.projectId = aProjectId;
        this.annotatorName = aAnnotatorName;
        this.layers = aLayers;

        let layersToAdd : number[] = [];

        for(let i = 0; i < aLayers.length; i++) {
            layersToAdd.push(aLayers[i][0]);
        }

        this.viewport.push(new Viewport(aDocumentId,"",0, 75, layersToAdd, null, null));

        this.annotationExperienceAPI = new AnnotationExperienceAPIImpl(aProjectId, this.viewport[0].sourceDocumentId, aAnnotatorName, aUrl, this);
        this.annotationExperienceAPIVisualization = new AnnotationExperienceAPIWordAlignmentEditorVisualization(this);
        this.annotationExperienceAPIWordAlignmentEditorActionHandler = new AnnotationExperienceAPIWordAlignmentEditorActionHandler(this);

    }

    update(annotationExperienceAPI: AnnotationExperienceAPI)
    {
        console.log("RECEIVED NOTIFICATION")
    }

    saveAlignments()
    {
        let pairs = []
        const that = this;

        if (!this.inputsValid) {
            alert("Word alignment is not 1:1.")
            return;
        }

        this.spanType = 278;

        let oddUnitContainerSize =  document.getElementById("odd_unit_container").children.length - 2;
        let evenUnitContainerSize = document.getElementById("even_unit_container").children.length - 2;

        for (let i = 0; i < oddUnitContainerSize; i++) {
            for (let j = 0; j < evenUnitContainerSize; j++) {

                let oddUnitContainerElementInputValue = document.getElementById("odd_unit_container").children[i + 2].children[0].value;
                let evenUnitContainerElementInputValue = document.getElementById("even_unit_container").children[j + 2].children[1].value;

                if (oddUnitContainerElementInputValue === evenUnitContainerElementInputValue) {

                    let oddUnitContainerElementText = document.getElementById("odd_unit_container").children[i + 2].children[1];
                    let evenUnitContainerElementText = document.getElementById("even_unit_container").children[j + 2].children[0];

                    let oddUnitContainerElementTextId = oddUnitContainerElementText.id.split("_");
                    let evenUnitContainerElementTextId = evenUnitContainerElementText.id.split("_");

                    console.log("ADDMING NOW SPAN FROM " + oddUnitContainerElementTextId[1] + " to " + oddUnitContainerElementTextId[2]);
                    console.log("ADDMING NOW SPAN FROM " + evenUnitContainerElementTextId[1] + " to " + evenUnitContainerElementTextId[2]);

                    pairs.push([
                        oddUnitContainerElementTextId[1],
                        evenUnitContainerElementTextId[1]]
                    );

                    this.createSpanRequest(oddUnitContainerElementTextId[1], oddUnitContainerElementTextId[2], this.spanType);
                    this.createSpanRequest(evenUnitContainerElementTextId[1], evenUnitContainerElementTextId[2], this.spanType);
                }
            }
        }

        setTimeout(function () {
            console.log(pairs)
            console.log(that.viewport[0].spans)
            for (let i = 0; i < pairs.length; i++) {
                let source, target;
                for (let j = 0; j < that.viewport[0].spans.length; j++) {
                    console.log(that.viewport[0].spans[j])
                    console.log(pairs[i][0])


                    if (pairs[i][0] == that.viewport[0].spans[j].begin.toString()) {
                        source = that.viewport[0].spans[j];
                        console.log("FOUND SRC")
                    }
                    console.log(pairs[i][1])
                    if (pairs[i][1] == that.viewport[0].spans[j].begin.toString()) {
                        target = that.viewport[0].spans[j];
                        console.log("FOUND DEP")
                    }
                    if (source != null && target != null) {
                        console.log("BREAK now")
                        break;
                    }
                }
                that.annotationExperienceAPI.requestCreateArc(
                    that.annotatorName,
                    that.projectId,
                    that.viewport[0].sourceDocumentId,
                    source.id,
                    target.id,
                    that.arcType)
            }
        }, 2000)

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

    createSpanRequest(aBegin: string, aEnd : string, aLayer: number)
    {
        let that = this;

        this.annotationExperienceAPI.requestCreateSpan(
            that.annotatorName,
            that.projectId,
            that.viewport[0].sourceDocumentId,
            Number(aBegin),
            Number(aEnd),
            aLayer);
    }

    resetAlignments()
    {
        let that = this;

        for (let i = 0; i < that.viewport[0].arcs.length; i++) {
            this.annotationExperienceAPI.requestDeleteAnnotation(
                that.annotatorName,
                that.projectId,that.viewport[0].sourceDocumentId, that.viewport[0].arcs[i].id, that.viewport[0].arcs[i].layerId
            )
        }

        for (let i = 0; i < that.viewport[0].spans.length; i++) {
            that.annotationExperienceAPI.requestDeleteAnnotation(
                that.annotatorName,
                that.projectId,
                that.viewport[0].sourceDocumentId,
                that.viewport[0].spans[i].id,
                that.viewport[0].spans[i].layerId
            )
        }
        document.getElementById("save_alignment").disabled = false;
    }
}
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

export class AnnotationExperienceAPIWordAlignmentEditor
{
    annotationExperienceAPI: AnnotationExperienceAPIImpl;
    annotationExperienceAPIVisualization: AnnotationExperienceAPIWordAlignmentEditorVisualization;
    annotationExperienceAPIWordAlignmentEditorActionHandler: AnnotationExperienceAPIWordAlignmentEditorActionHandler
    projectId: number;
    clientName: string;

    pairs : [string, string][];

    originalLanguageSentence : string;
    originalUser: string;
    originalDocument: number;

    translatedLanguageSentence: string;
    translatedUser: string;
    translatedDocument : number;

    constructor()
    {
        this.annotationExperienceAPI = new AnnotationExperienceAPIImpl();
        this.annotationExperienceAPIVisualization = new AnnotationExperienceAPIWordAlignmentEditorVisualization(this);
        this.annotationExperienceAPIWordAlignmentEditorActionHandler = new AnnotationExperienceAPIWordAlignmentEditorActionHandler(this);
        this.annotationExperienceAPIWordAlignmentEditorActionHandler.registerDefaultActionHandler();

    }

    saveAlignments()
    {
        let values = []
        const that = this;
        for (let i = 0; i < document.getElementById("container_german").children.length - 2; i++) {
             if (values.indexOf(document.getElementById("container_german").children[i+2].children[1].value) > -1) {
                 alert("Word alignment is not 1:1.")
                 return;
             }
             values.push(document.getElementById("container_german").children[i+2].children[1].value)
        }


        this.projectId = this.annotationExperienceAPI.projectID
        for (let i = 0; i < document.getElementById("container_english").children.length - 2; i++) {
            for (let j = 0; j < document.getElementById("container_german").children.length - 2; j++) {
                if ((document.getElementById("container_english").children[i + 2].children[0].value) ===
                    (document.getElementById("container_german").children[j + 2].children[1].value)) {
                    this.pairs.push([document.getElementById("container_english").children[i + 2].children[1].id.split("_")[2],
                        (document.getElementById("container_german").children[j + 2].children[0].id.split("_")[2])]);
                    this.annotationExperienceAPI.requestCreateSpanFromServer(
                        this.clientName,
                        this.originalUser,
                        this.projectId,
                        this.originalDocument,
                        Number(document.getElementById("container_english").children[i + 2].children[1].id.split("_")[2]),
                        Number(document.getElementById("container_english").children[i + 2].children[1].id.split("_")[3]),
                        "Word_Alignment_Span",
                        "Word_Alignment_Span")

                    this.annotationExperienceAPI.requestCreateSpanFromServer(
                        this.clientName,
                        this.translatedUser,
                        this.projectId,
                        this.translatedDocument,
                        Number(document.getElementById("container_german").children[i + 2].children[0].id.split("_")[2]),
                        Number(document.getElementById("container_german").children[i + 2].children[0].id.split("_")[3]),
                        "Word_Alignment_Span",
                        "Word_Alignment_Span")
                }
            }
        }

       setTimeout(function () {
           let spans = that.annotationExperienceAPI.spans;
           for (let i = 0; i < that.pairs.length; i++) {
               for (let j = 0; j < spans.length; j++) {
                   let gov, dep;
                   console.log(that.pairs[i])
                   console.log(spans[j])
               }
           }
        /*
           that.annotationExperienceAPI.requestCreateRelationFromServer(
               that.clientName,
               that.clientName,
               that.projectId,
               that.translatedDocument,null,null, "Word_Alignment_Relation", "Relation")

         */
       }, 4000)

    }
//requestCreateRelationFromServer(aClientName: string, aUserName: string, aProjectId: number, aDocumentId: number, aGovernorId : number, aDependentId : number, aDependencyType : string, aFlavor : string)

  //
}
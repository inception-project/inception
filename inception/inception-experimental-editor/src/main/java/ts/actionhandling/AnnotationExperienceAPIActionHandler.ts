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

import {AnnotationExperienceAPI} from "../../../../../../inception-api-annotation-experimental/src/main/ts/client/AnnotationExperienceAPI";

export class AnnotationExperienceAPIActionHandler {
    annotationExperienceAPI: AnnotationExperienceAPI;

    constructor(aAnnotationExperienceAPI: AnnotationExperienceAPI) {
        this.annotationExperienceAPI = aAnnotationExperienceAPI;
    }


    registerOnClickActionHandler(aTagName: string, aAction: string, aViewport: number[][]) {
        let that = this;

        let elem = document.querySelector("." + aTagName);
        if (elem != null) {
            //Click events on annotation page specific items
            switch (aAction) {
                case "select":
                    for (let item of document.getElementsByClassName(aTagName)) {
                        item.setAttribute("onclick", 'annotationExperienceAPI.sendSelectAnnotationMessageToServer(evt.target.attributes[3].value)');
                    }
                    break;
                case "new_document":
                    elem.addEventListener("click", () => {
                        //that.annotationExperienceAPI.sendDocumentMessageToServer();
                    });
                    break;
                case "viewport":
                    elem.addEventListener("click", () => {
                        //that.annotationExperienceAPI.sendViewportMessageToServer(aViewport);
                    });
                    break;
                default:
                    console.error("Can not register single click action, reason: Action-type not found.");
                    return;
            }

            console.log("Action: " + aAction + " is registered for elements: " + aTagName)
        } else {
            console.error("Can not register single click action, reason: Element not found.");
        }
    }

    registerOnDoubleClickActionHandler(aTagName: string, aAction: string) {
        let elements = document.getElementsByClassName(aTagName);
        if (elements != null) {
            switch (aAction) {
                case  "create":
                    for (let item of elements) {
                        /*
                        item.setAttribute("ondblclick",
                            'annotationExperienceAPI.sendCreateAnnotationMessageToServer(evt.target.attributes[3].value,' +
                            'document.getElementsByClassName("dropdown")[0].children[1].getAttribute("title"),' +
                            'evt.target.parentElement.attributes[1].value)');

                         */
                    }
                    break;
                default:
                    console.error("Can not register double click action, reason: Action-type not found.")
                    return;
            }
        }

        console.log("Action: " + aAction + " is registered for elements: " + aTagName)
    }

    registerDefaultActionHandler() {
        //Click events on annotation page specific items
        let that = this;
        onclick = function (aEvent) {
            let elem = <Element>aEvent.target;
            console.log(elem)
            if (elem.tagName === 'rect') {
                //TODO for correct user
                that.annotationExperienceAPI.requestSelectAnnotationFromServer("admin", elem.attributes[4].value)
            }
        }

        ondblclick = function (aEvent) {
            let elem = <Element>aEvent.target;
            console.log(elem)
        }
    }
}
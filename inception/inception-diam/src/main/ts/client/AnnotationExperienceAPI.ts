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
import {Observer} from "./Observer";
import {Viewport} from "./model/Viewport";
import {FeatureX} from "./model/FeatureX";
import {AdviceMessage} from "./messages/response/AdviceMessage";
import {DocumentMessage} from "./messages/response/DocumentMessage";
import {UpdateFeatureMessage} from "./messages/response/UpdateFeatureMessage";
import {DeleteAnnotationMessage} from "./messages/response/DeleteAnnotationMessage";
import {SpanCreatedMessage} from "./messages/response/create/SpanCreatedMessage";
import {ArcCreatedMessage} from "./messages/response/create/ArcCreatedMessage";

/**
 * Interface-class for the Experience API.
 *
 * The experience API is the most important one when creating custom editors. All endpoints required
 * for a custom editor will be here. The Experience API forwards any requests to the back-end that were
 * made by the editor and will receive any updates from the server, which any custom editor must
 * fetch and display.
 *
 * When working with the Experience API two different kind of endpoints are important:
 *      request() functions:
 *          This type of function always sends a new request to the server. They publish  a new message to a
 *          specific topic that the server listens to. Input parameters, which will be added as a JSON string
 *          to the payload, represent the required data by the server in order to process the request accordingly
 *          (like the span ID when deleting a span). The JSON string will be created from the corresponding class
 *          of the request, e.g. when requestUpdateSpanFromServer() is called, there will be always send a JSON string
 *          representation of the class UpdateSpanRequest.
 *          RequestFromServer() functions are always built the same way: The stompClient publishes data to a specific topic
 *          (stompClient.publish()) containing a specific JSON payload (body: JSON.stringify()).
 *
 *      on() functions:
 *          This type of functions receive and process the data received from the server. They fire whenever data
 *          on a specific topic is published.
 *
 *
 * The Experience API also handles automatically any subscriptions and un-subscriptions of topics. Whenever the
 * Experience API, there is no need to bother about any communication or back-end implementation, everything
 * will be handled automatically.
 *
 * @NOTE: It is important to fetch any data received from the server, because it is not always the case,
 * that a client sends data to the server and receives an reply. There are many uses cases when the server
 * sends data to client(s) without them requesting it. An example therefore is the following:
 *      All annotation of a document are delete on the server-side. This event triggers the back-end API
 *      to forward a deleteAnnotationMessage (one message for every deleted annotations) to all clients displaying the
 *      viewport that is effected by the change.
 *
 * @NOTE: Adding new request() and on() methods is fairly easy following the streamlined process in the README.
 *
 * @CAUTION: It is not adviced to alter any of the endpoints implementation details.
 *
 * @NOTE: For further information please look into the README file
 *
 **/
export interface AnnotationExperienceAPI
{
    /**
     * Unsubscribing for everything in a specific viewport.
     * Altering this function plaese with caution: Normally the
     * subscribe/unsubscribe mechanism should work autmatically.
     * @param aViewport: Everything within that viewport will be unsubscribed.
     */
    unsubscribe(aViewport: Viewport);

    /**
     * Self explaining: Disconnect from the current websocket connection
     */
    disconnect();


    /**
     * Observer attach and detach methods to provide better usage of the API
     * for all editors
     * @param observer
     */
    attach(observer: Observer);

    detach(observer: Observer);

    notify();

    /**
     * request() functions as explained in Interface-definition.
     * @params: Everything required by the server to handle the request.
     */

    requestDocument(aAnnotatorName: string, aProjectId: number, aViewport: Viewport[]);

    requestUpdateFeature(aAnnotatorName: string, aProjectId: number, aDocumentId: number,
                                aAnnotationId: number, aLayerId: number, aFeature: FeatureX, aValue: any)

    requestCreateSpan(aAnnotatorName: string, aProjectId: number,
                                aDocumentId: number, aBegin: number, aEnd: number, aLayer: number);

    requestDeleteAnnotation(aAnnotatorName: string,  aProjectId: number,
                                aDocumentId: number, aAnnotationId: number, aLayer: number);

    requestCreateArc(aAnnotatorName: string, aProjectId: number, aDocumentId: number,
                                aSourceId : number, aTargetId : number, aLayer: number)


    /**
     * on() classes as explained in Interface-definition.
     * @param aMessage of a certain type. All the data can be parsed directly from that messages payload
     */

    onDocument(aMessage: DocumentMessage);

    onSpanCreate(aMessage: SpanCreatedMessage);

    onArcCreate(aMessage: ArcCreatedMessage);

    onFeaturesUpdate(aMessage: UpdateFeatureMessage);

    onAnnotationDelete(aMessage: DeleteAnnotationMessage);

    onError(aMessage: AdviceMessage);
}
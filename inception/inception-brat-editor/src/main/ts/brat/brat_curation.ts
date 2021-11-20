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
import { Ajax } from "./ajax/Ajax";
import { INSTANCE as Util } from "./util/Util";
import { CurationMod } from "./curation/CurationMod";

declare let Wicket;

function brat(markupId: string, controllerCallbackUrl: string, collCallbackUrl: string, docCallbackUrl: string) {
  Util.embedByURL(markupId, collCallbackUrl, docCallbackUrl,
    function (dispatcher) {
      dispatcher.wicketId = markupId;
      dispatcher.ajaxUrl = controllerCallbackUrl;
      new Ajax(dispatcher);
      new CurationMod(dispatcher, markupId);
      Wicket.$(markupId).dispatcher = dispatcher;
    });
}

export = brat;
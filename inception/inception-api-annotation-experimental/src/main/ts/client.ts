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

/*
 *  Typescript Annotation API
 */


import io, {Socket} from "socket.io-client"
//import {initEventHandlers} from "./util/websocket/events/ClientEventHandler";

console.log("init -- Experimental - Annotation - API")

declare global {
    interface Window {
        SOCKET : Socket
    }
}

const socket = io()


//Used to send messages, treated as global reference and immutable, do not override
window.SOCKET = socket

onmouseup = function(event)
{
    //Currently only get the position
    //Later on, filter specific events
    console.log("mouse location:", event.clientX, event.clientY)

}

//initEventHandlers()



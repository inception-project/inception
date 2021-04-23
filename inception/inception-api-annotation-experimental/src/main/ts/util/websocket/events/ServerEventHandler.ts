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
import {Socket} from "socket.io";
import {SocketType} from "../libs/SocketType";
import {DataService} from "../services/DataService";

export const initEventHandlers = (
    socket: Socket, dataService : DataService) =>
{
    socket.on(SocketType.SEND_CREATE_ANNOTATION, async () => {
        socket.emit(SocketType.SEND_CREATE_ANNOTATION)
    })
    socket.on(SocketType.SEND_SELECT_ANNOTATION, async () => {
        const data = dataService.getData();
        console.log(data)
        socket.emit(SocketType.SEND_SELECT_ANNOTATION)
    })

}

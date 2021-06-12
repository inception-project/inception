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

import express from 'express';
import * as ws from 'ws';
import {
    ClientMessage,
    ServerMessage
} from "../common/Data";
import {Annotation} from "../common/annotation/Annotation";

//Data of each client
export type ClientData = {
    socket : ws,
    id : number
}

export class Server
{
    _express : express.Express;
    _wsServer : ws.Server;
    clients: ClientData[] = [];
    annotations : Annotation[];

    constructor()
    {
        console.log(" ---- Starting Service now ----");
        const server = this._express.listen(8080, () => console.log("Listening"))
        this._wsServer = new ws.Server({
            noServer: true
        });

        server.on('upgrade', (reqest, socket, head) => {
            this._wsServer.handleUpgrade(reqest, socket, head, socket => {
                console.log("Should handle now");
            });
        });


        this._wsServer.on('connection', (socketClient: ws) => {

            this.clients.push(this.receiveClientConnect(socketClient, this.clients))


            socketClient.on('message', (msg: ws.Data) => {
                this.receiveClientMessage(socketClient, msg)

            });
            socketClient.on('close', () => {
                this.receiveClientDisconnect(socketClient)
                //Remove from list
            });
        });
    }

    getAnnotationById(aId, string) {

    }

    //Eventhandling


    // ------------------- SEND ---------------------- //

    sendMessageToClient(clientSocket: ws, msg: ServerMessage)
    {
        let message = JSON.stringify(msg);
        clientSocket.send(message)
    }


    sendMessageToAllClients(clientSocket: ws, msg: ClientMessage, clients: ClientData[])
    {
        let message = JSON.stringify(msg);
        for (let client of clients) {
            if (client.socket != clientSocket) {
                client.socket.send(message);
            }
        }
    }

    // ------------------- RECEIVE -------------------- //

    receiveClientConnect(socketClient : ws, clients: ClientData[])
    {

        let clientData: ClientData = {
            socket: socketClient,
            id: clients.length + 1,
        }
        console.log("Client Connect, " + socketClient);
        return clientData;
    }

    receiveClientDisconnect(socketClient : ws)
    {
        console.log("Client Disconnect, " + socketClient);

    }
    receiveClientMessage(socketClient : ws, msg : ws.Data)
    {
        console.log("Client message, " + socketClient + ", " + msg)
        let message = JSON.parse(<string>msg)

        switch (message.type) {
            case "selectedAnnotationForClient":

                console.log("Client requests data for: " + msg);

                //Parsing
                const requestedAnnotation = this.getAnnotationById(null, null)

                break;
            case "newAnnotationForClient":
                break;
            case "deletedAnnotationForClient":
                break;
            case "newConnectedClientForClient":
                break;
            case "newDocumentForClient":
                break;
        }
    }



}

const server = new Server()
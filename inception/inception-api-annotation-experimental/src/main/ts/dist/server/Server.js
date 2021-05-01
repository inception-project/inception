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
import * as ws from 'ws';
export class Server {
    constructor() {
        this.clients = [];
        console.log(" ---- Starting Service now ----");
        const server = this._express.listen(8080, () => console.log("Listening"));
        this._wsServer = new ws.Server({
            noServer: true
        });
        server.on('upgrade', (reqest, socket, head) => {
            this._wsServer.handleUpgrade(reqest, socket, head, socket => {
                console.log("Should handle now");
            });
        });
        this._wsServer.on('connection', (socketClient) => {
            this.clients.push(this.receiveClientConnect(socketClient, this.clients));
            socketClient.on('message', (msg) => {
                this.receiveClientMessage(socketClient, msg);
            });
            socketClient.on('close', () => {
                this.receiveClientDisconnect(socketClient);
                //Remove from list
            });
        });
    }
    getAnnotationById(aId, string) {
    }
    //Eventhandling
    // ------------------- SEND ---------------------- //
    sendMessageToClient(clientSocket, msg) {
        let message = JSON.stringify(msg);
        clientSocket.send(message);
    }
    sendMessageToAllClients(clientSocket, msg, clients) {
        let message = JSON.stringify(msg);
        for (let client of clients) {
            if (client.socket != clientSocket) {
                client.socket.send(message);
            }
        }
    }
    // ------------------- RECEIVE -------------------- //
    receiveClientConnect(socketClient, clients) {
        let clientData = {
            socket: socketClient,
            id: clients.length + 1,
        };
        console.log("Client Connect, " + socketClient);
        return clientData;
    }
    receiveClientDisconnect(socketClient) {
        console.log("Client Disconnect, " + socketClient);
    }
    receiveClientMessage(socketClient, msg) {
        console.log("Client message, " + socketClient + ", " + msg);
        let message = JSON.parse(msg);
        switch (message.type) {
            case "selectedAnnotationForClient":
                console.log("Client requests data for: " + msg);
                //Parsing
                const requestedAnnotation = this.getAnnotationById(null, null);
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
const server = new Server();
//# sourceMappingURL=Server.js.map
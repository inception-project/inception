import {Client, Frame, Stomp} from '@stomp/stompjs';

class Annotator
{
    stompClient : Client;
    connected : boolean = false;

    document : string;
    project : string;
    username : string;
    viewPortBegin : number;
    viewPortEnd : number;

    constructor()
    {
        const that = this;

        //Testing only
        //Click events triggering either select annotation or create annotation
        onclick = function (aEvent)
        {
            let elem = <Element>aEvent.target;
            if (elem.tagName === 'text') {
                console.log(elem);
                that.sendSelectAnnotationMessageToServer();
            }


        }
        ondblclick = function (aEvent)
        {
            let elem = <Element>aEvent.target;
            if (elem.tagName === 'text') {
                console.log(elem);
                that.sendCreateAnnotationMessageToServer();
            }


        }

        // Experimental yet only (will be retrieved automatically)
        this.username = "admin";
        this.document = "Doc4";
        this.project = "Annotation Study";
        this.viewPortBegin = 0;
        this.viewPortEnd = 10;

        this.connect();
    }


    //CREATE WEBSOCKET CONNECTION
    connect()
    {
        if (this.connected) {
            console.log("You are already connected")
            return;
        }

        let url : string = (window.location.protocol.startsWith("https") ? "wss://" : "ws://")
            + window.location.host
            + "/inception_app_webapp_war_exploded/ws";

        this.stompClient = Stomp.over(function() {
            return new WebSocket(url);
        });

        //REQUIRED DUE TO JS SCOPE
        const that = this;

        this.stompClient.onConnect = function (frame) {
            that.connected = true;

            const prop = frame.headers; //Will be needed for getting client details

            // ------ DEFINE ALL SUBSCRIPTION CHANNELS WITH ACTIONS ------ //

            that.stompClient.subscribe("/queue/new_document_for_client/" + that.username, function (msg) {
                that.receiveNewDocumentMessageByServer(JSON.parse(msg.body), frame);
            });

            that.stompClient.subscribe("/queue/new_viewport_for_client/" + that.username, function (msg) {
                that.receiveNewViewportMessageByServer(JSON.parse(msg.body), frame);
            });

            that.stompClient.subscribe("/queue/selected_annotation_for_client/" + that.username, function (msg) {
                that.receiveSelectedAnnotationMessageByServer(JSON.parse(msg.body), frame);
            });

            //Multiple subscriptions due to viewport
            for (let i = that.viewPortBegin; i <= that.viewPortEnd; i++) {
                that.stompClient.subscribe("/topic/annotation_created_for_clients/" + that.project + "/" + that.document + "/" + i, function (msg) {
                    that.receiveNewAnnotationMessageByServer(JSON.parse(msg.body), frame);
                });

                that.stompClient.subscribe("/topic/annotation_deleted_for_clients/" + that.project + "/" + that.document + "/" + i, function (msg) {
                    that.receiveDeleteAnnotationMessageByServer(JSON.parse(msg.body), frame);
                });
            }
            // ------------------------------------------------------------ //

        };


        // ------ ERROR HANDLING ------ //

        this.stompClient.onStompError = function (frame) {
            console.log('Broker reported error: ' + frame.headers['message']);
            console.log('Additional details: ' + frame.body);
        };

        // ------------------------------- //

        this.stompClient.activate();
    }

    // ------ DISCONNECT -------- //
    disconnect()
    {
        if (this.connected) {
            console.log("Disconnecting now");
            this.connected = false;
            this.stompClient.deactivate();
        }
    }
    // ------------------------------- //


    /** ----------- Actions ----------- **/

    editAnnotation = function(aEvent)
    {
        //TODO
    }

    unsubscribe(channel : string)
    {
        this.stompClient.unsubscribe(channel);
    }


    /** -------------------------------- **/


    /** ----------- Event handling ------------------ **/

    // ---------------- SEND ------------------------- //

    sendNewDocumentMessageToServer()
    {
        this.stompClient.publish({destination: "/app/new_document_by_client", body:"NEW DOCUMENT REQUIRED"});

    }

    sendNewViewportMessageToServer()
    {
        this.stompClient.publish({destination: "/app/new_viewport_by_client", body:"NEW VIEWPORT REQUIRED"});
    }

    sendSelectAnnotationMessageToServer()
    {
        let json = JSON.stringify(
            { username: this.username,
                   project: this.project,
                   document: this.document,
                   begin: 0, end: 8});
        this.stompClient.publish({destination: "/app/select_annotation_by_client", body: json});
    }

    sendCreateAnnotationMessageToServer()
    {
        let json = {
            username : this.username,
            project : this.project,
            document : this.document,
            begin : 0,
            end : 8
        }
        this.stompClient.publish({destination: "/app/new_annotation_by_client", body: JSON.stringify(json)});
    }

    sendDeleteAnnotationMessageToServer()
    {
        this.stompClient.publish({destination: "/app/delete_annotation_by_client", body:"DELETE"});
    }

    // ------------------------------------------------ //


    // ---------------- RECEIVE ----------------------- //
    receiveNewDocumentMessageByServer(aMessage : string, aFrame : Frame)
    {
        console.log('RECEIVED DOCUMENT: ' + JSON.parse(aMessage) + ',' + aFrame);
    }

    receiveNewViewportMessageByServer(aMessage : string, aFrame : Frame)
    {
        console.log('RECEIVED VIEWPORT: ' + JSON.parse(aMessage) + ',' + aFrame);
    }

    receiveSelectedAnnotationMessageByServer(aMessage : string, aFrame : Frame)
    {
        console.log('RECEIVED SELECTED ANNOTATION: ' + JSON.parse(aMessage) + ',' + aFrame);
    }

    receiveNewAnnotationMessageByServer(aMessage : string, aFrame : Frame)
    {
        console.log('RECEIVED NEW ANNOTATION: ' + JSON.stringify(aMessage) + aFrame);
    }

    receiveDeleteAnnotationMessageByServer(aMessage : string, aFrame : Frame)
    {
        console.log('RECEIVED DELETE ANNOTATION: ' + JSON.stringify(aMessage) + aFrame);
    }


    /** ---------------------------------------------- **/
}

let annotator = new Annotator()
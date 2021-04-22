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
import * as ConfigFile from "./config.json";
import {TinyEmitter} from "tiny-emitter";
import {Websocket} from "./util/Websocket";
import {Draw} from "./drawer/Draw";

/**
 * Typescript API
 */
export class Experimental
{
    //Eventhandler
    emitter : TinyEmitter

    //URL for websocket
    url : string

    //Drawer
    drawer : Draw

    websocket : Websocket

    constructor()
    {
        //TODO define what is needed to be configurable in config.json

        //Drawer
        this.drawer = new Draw();

        //init eventhandler and emitter
        this.emitter = new TinyEmitter()

        //init websocket
        this.websocket = new Websocket();
        //this.websocket._createWebsocket("ws://localhost:8080/p/20/annotate");

        //Create event handlers
        this.on(onmouseup,this._eventSelectAnnotation)

        console.log("API Created")
    }

    // ----------------- Events ----------------- //

    _eventSelectAnnotation = (event : Event) =>
    {
        this.emitter.emit('_send_select_annotation', this._selectAnnotation(event))
    }

    _eventSendCreateAnnotation = () =>
    {
        this.emitter.emit('_send_create_annotation', this._sendCreateAnnotation())
    }

    _eventReceiveCreatedAnnotation = () =>
    {
        this.emitter.emit('_receive_create_annotation', this._receiveCreatedAnnotation())
    }

    _eventSendDeleteAnnotation = () =>
    {
        this.emitter.emit('_send_delete_annotation', this._sendDeleteAnnotation())
    }

    _eventReceiveDeleteAnnotation = () =>
    {
        this.emitter.emit('_receive_delete_annotation', this._receiveDeleteAnnotation())
    }

    _eventSendUpdateAnnotation = () =>
    {
        this.emitter.emit('_send_update_Annotation', this._sendUpdateAnnotation());
    }

    _eventReceiveUpdateAnnotation = () =>
    {
        this.emitter.emit('_receive_update_Annotation', this._receiveUpdateAnnotation());
    }

    // ----------- Add event handler ------------- //
    on = (event, handler) =>
    {
        this.emitter.on(event, handler)
    }


    // ------------------------------------------- //

    _sendCreateAnnotation = () =>
    {
        //Send to server that new Annotation has been craeted
        //Draw highlight
    }

    _receiveCreatedAnnotation = () =>
    {
        //Update from server that annotation was created
        //Draw highlight
    }

    _sendDeleteAnnotation = () =>
    {
        //Send to server
        //Remove highlighting
    }

    _receiveDeleteAnnotation = () =>
    {
        //Update from server that annotation was deleted
        //Remove highlighting
    }

    _selectAnnotation = (event : Event) =>
    {
        console.log(event)
        console.log(event.currentTarget)
        this.drawer._highlightAnnotation(null, null)
        //Tell server where Client has clicked
        //Draw highlighting
    }

    _sendUpdateAnnotation = () =>
    {
        //Send update to server -> Redraw
    }

    _receiveUpdateAnnotation = () =>
    {
        //Update received from server to update annotation -> Redraw
    }

    // --------------------------------------------------- //

}

console.log("init -- Experimental - Annotation - API")
var API = new Experimental()
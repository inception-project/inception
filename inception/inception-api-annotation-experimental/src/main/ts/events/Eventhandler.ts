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
import {Experimental} from "../index";

export class Eventhandler
{
    editor : Experimental

    constructor(aEditor : Experimental)
    {
        this.editor = aEditor
    }

    //Create annotation on single word
    _onMouseDoubleClick = (aEvent : Event) =>
    {
        this.editor._createAnnotation(null)
    }

    //Select annotation
    _onMouseSingleClick = (aEvent : Event) =>
    {
        this.editor._selectAnnotation(null,null)
    }

    //After dragging, custom length
    _onMouseUp = (aEvent : Event)  =>
    {
        this.editor._createAnnotation(null)
    }
}
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

/**
 * Class required for Messaging between Server and Client.
 * Basis for JSON
 * AdviceMessage: Message received by a client that contains a Message that shall be displayed and
 * a Message TYPE (ERROR, HINT, VALIDATION, INFO)
 *
 * Attributes:
 * @adviceMessage: String representation of the content that shall be displayed
 * @adviceMessageType: String representation for the TYPE of the message.
 *
 * @NOTE: Whenever an AdviceMessage is received please show it to the user accordingly
 **/

export class AdviceMessage
{
    adviceMessage : string;
    adviceMessageTyoe: string;
}
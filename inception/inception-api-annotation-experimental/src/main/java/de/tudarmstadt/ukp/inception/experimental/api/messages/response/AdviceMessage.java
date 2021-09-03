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
package de.tudarmstadt.ukp.inception.experimental.api.messages.response;

/**
 * Class required for Messaging between Server and Client.
 * Basis for JSON
 * AdviceMessage: Message published to a specific client when something unexpected happend.
 * AdviceMessage must contain of the following @TYPE (public enum)
 *  * ERROR: For error messages
 *  * HINT: When a hint shall be displayed for the annotator
 *  * VALIDATION: When an input was incorrect
 *  * INFO: Info message, e.g. for performing something successfully
 *
 * Attributes:
 * @adviceMessage: String representation of the content that shall be displayed at the client-side
 * @adviceMessageType: @TYPE for the message.
 **/
public class AdviceMessage
{
    public enum TYPE {
        ERROR,
        HINT,
        VALIDATION,
        INFO
    }
    private String adviceMessage;
    private TYPE adviceMessageType;

    public AdviceMessage(String aAdviceMessage, TYPE aAdviceMessageType)
    {
        adviceMessage = aAdviceMessage;
        adviceMessageType = aAdviceMessageType;
    }

    public String getAdviceMessage()
    {
        return adviceMessage;
    }

    public void setAdviceMessage(String aAdviceMessage)
    {
        adviceMessage = aAdviceMessage;
    }

    public TYPE getAdviceMessageType()
    {
        return adviceMessageType;
    }

    public void setAdviceMessageType(TYPE aAdviceMessageType)
    {
        adviceMessageType = aAdviceMessageType;
    }
}

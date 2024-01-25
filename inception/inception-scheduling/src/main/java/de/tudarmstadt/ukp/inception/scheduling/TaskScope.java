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
package de.tudarmstadt.ukp.inception.scheduling;

public enum TaskScope
{
    /**
     * Task can be cleared as soon as it has ended.
     */
    EPHEMERAL,

    /**
     * Task can be cleared when the last session of the user is cleared or when the system is
     * restarted.
     */
    LAST_USER_SESSION,

    /**
     * Task can be cleared when the project is deleted or when the system is restarted.
     */
    PROJECT;

    boolean isDestroyOnEnd()
    {
        return this == EPHEMERAL;
    }

    boolean isRemoveWhenUserSessionEnds()
    {
        return this == EPHEMERAL;
    }

    boolean isRemoveWhenLastUserSessionEnds()
    {
        return this == LAST_USER_SESSION || this == EPHEMERAL;
    }
}

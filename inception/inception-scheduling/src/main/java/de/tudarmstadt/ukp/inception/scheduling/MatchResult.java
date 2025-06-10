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

public enum MatchResult
{
    /**
     * Task does not match.
     */
    NO_MATCH,

    /**
     * Unqueue any existing matching tasks. Scheduled and running tasks as left alone. Enqueue the
     * new task.
     */
    UNQUEUE_EXISTING_AND_QUEUE_THIS,

    /**
     * Discard the incoming task if it matches an already enqueued task. If a matching task is
     * already scheduled or running, then queue the incoming task.
     */
    DISCARD_OR_QUEUE_THIS,

    /**
     * Queue this task. It will be run immediately or after other matching tasks have been run. It
     * will not run parallel to matching tasks.
     */
    QUEUE_THIS;
}

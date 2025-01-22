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
package de.tudarmstadt.ukp.inception.assistant.config;

import java.time.Duration;

public interface AssistantDocumentIndexProperties
{
    /**
     * @return maximum time to wait when trying to perform an exclusive action on an index for
     *         another exclusive action to finish.
     */
    Duration getBorrowWaitTimeout();

    /**
     * @return time how often the pool is checked for idle indices that can be removed and how long
     *         indices remain open when not being used.
     */
    Duration getIdleEvictionDelay();

    /**
     * @return time that an index should remain in the pool before being evicted. This is meant to
     *         ensure that indices that are used regularly remain a while in the pool before we have
     *         to open them again.
     */
    Duration getMinIdleTime();

    /**
     * @return maximum size in LLM tokens that a RAG chunk should have
     */
    int getMaxChunks();

    /**
     * @return the minimum score a chunk must have with respect to the user query to be used by the
     *         RAG
     */
    double getMinScore();

    int getChunkSize();

    int getUnitOverlap();
}

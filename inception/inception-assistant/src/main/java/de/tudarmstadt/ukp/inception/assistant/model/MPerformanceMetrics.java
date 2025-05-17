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
package de.tudarmstadt.ukp.inception.assistant.model;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;

/**
 * @param duration
 *            time it took to produce the messages in milliseconds
 */
@JsonSerialize
public record MPerformanceMetrics(long delay, long duration, int tokens) {

    private MPerformanceMetrics(Builder builder)
    {
        this(builder.delay, builder.duration, builder.tokens);
    }

    public MPerformanceMetrics merge(MPerformanceMetrics aPerformance)
    {
        if (aPerformance == null) {
            return this;
        }

        return MPerformanceMetrics.builder() //
                .withDelay(Math.max(delay(), aPerformance.delay())) // )
                .withDuration((duration() + aPerformance.duration())) //
                .build();
    }

    public static Builder builder()
    {
        return new Builder();
    }

    public static final class Builder
    {
        private long delay;
        private long duration;
        private int tokens;

        private Builder()
        {
        }

        public Builder withDelay(long aDelay)
        {
            delay = aDelay;
            return this;
        }

        public Builder withDuration(long aDuration)
        {
            duration = aDuration;
            return this;
        }

        public Builder withTokens(int aTokens)
        {
            tokens = aTokens;
            return this;
        }

        public MPerformanceMetrics build()
        {
            return new MPerformanceMetrics(this);
        }
    }
}

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
package de.tudarmstadt.ukp.inception.annotation.layer.behavior.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import de.tudarmstadt.ukp.inception.annotation.layer.behavior.SpanAnchoringModeBehavior;
import de.tudarmstadt.ukp.inception.annotation.layer.behavior.SpanCrossSentenceBehavior;
import de.tudarmstadt.ukp.inception.annotation.layer.behavior.SpanOverlapBehavior;

@Configuration
public class LayerBehaviorAutoConfiguration
{
    @Bean
    public SpanAnchoringModeBehavior spanAnchoringModeBehavior()
    {
        return new SpanAnchoringModeBehavior();
    }

    @Bean
    public SpanCrossSentenceBehavior spanCrossSentenceBehavior()
    {
        return new SpanCrossSentenceBehavior();
    }

    @Bean
    public SpanOverlapBehavior spanOverlapBehavior()
    {
        return new SpanOverlapBehavior();
    }
}

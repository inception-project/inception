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
package de.tudarmstadt.ukp.inception.annotation.layer.span.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * <p>
 * This class is exposed as a Spring Component via {@link SpanLayerAutoConfiguration}.
 * </p>
 */
@ConfigurationProperties("recommender")
public class SpanRecommenderPropertiesImpl
    implements SpanRecommenderProperties
{
    /**
     * If enabled, span recommendations rendered in the brat editor get hover-overlay Accept/Reject
     * buttons in addition to the default click-to-accept / right-click-to-reject interaction.
     */
    private boolean actionButtonsEnabled;

    @Override
    public boolean isActionButtonsEnabled()
    {
        return actionButtonsEnabled;
    }

    public void setActionButtonsEnabled(boolean aActionButtonsEnabled)
    {
        actionButtonsEnabled = aActionButtonsEnabled;
    }
}

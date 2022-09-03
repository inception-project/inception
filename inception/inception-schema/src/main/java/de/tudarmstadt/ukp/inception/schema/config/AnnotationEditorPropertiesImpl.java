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
package de.tudarmstadt.ukp.inception.schema.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.jmx.export.annotation.ManagedAttribute;
import org.springframework.jmx.export.annotation.ManagedResource;

import de.tudarmstadt.ukp.inception.rendering.config.AnnotationEditorProperties;

/**
 * <p>
 * This class is exposed as a Spring Component via {@code AnnotationSchemaServiceAutoConfiguration}.
 * </p>
 */
@ConfigurationProperties("ui")
@ManagedResource
public class AnnotationEditorPropertiesImpl
    implements AnnotationEditorProperties
{
    private boolean tokenLayerEditable;
    private boolean sentenceLayerEditable;
    private boolean configurableJavaScriptActionEnabled = false;

    @ManagedAttribute
    @Override
    public boolean isTokenLayerEditable()
    {
        return tokenLayerEditable;
    }

    @ManagedAttribute
    public void setTokenLayerEditable(boolean aTokenLayerEditable)
    {
        tokenLayerEditable = aTokenLayerEditable;
    }

    @ManagedAttribute
    @Override
    public boolean isSentenceLayerEditable()
    {
        return sentenceLayerEditable;
    }

    @ManagedAttribute
    public void setSentenceLayerEditable(boolean aSentenceLayerEditable)
    {
        sentenceLayerEditable = aSentenceLayerEditable;
    }

    public void setConfigurableJavaScriptActionEnabled(boolean aConfigurableJavaScriptActionEnabled)
    {
        configurableJavaScriptActionEnabled = aConfigurableJavaScriptActionEnabled;
    }

    @Deprecated
    @Override
    public boolean isConfigurableJavaScriptActionEnabled()
    {
        return configurableJavaScriptActionEnabled;
    }
}

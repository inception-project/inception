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

import de.tudarmstadt.ukp.clarin.webanno.api.annotation.model.VID;
import de.tudarmstadt.ukp.inception.experimental.api.model.FeatureX;

/**
 * Class required for Messaging between Server and Client.
 * Basis for JSON
 * UpdateFeatureMessage: Message published to clients that a feature value of a feature has been changed for
 * a specific annotation
 *
 * Attributes:
 * annotationId: The ID of the annotation for which a feature value has been changed
 * feature: The feature which has an updated @value
 * value: The new value for a @feature
 **/
public class UpdateFeatureMessage
{
    private VID annotationId;
    private FeatureX feature;
    private Object value;

    public UpdateFeatureMessage(VID aAnnotationId, FeatureX aFeature, Object aValue)
    {
        annotationId = aAnnotationId;
        feature = aFeature;
        value = aValue;
    }

    public VID getAnnotationId()
    {
        return annotationId;
    }

    public void setAnnotationId(VID aAnnotationId)
    {
        annotationId = aAnnotationId;
    }

    public FeatureX getFeature()
    {
        return feature;
    }

    public void setFeature(FeatureX aFeature)
    {
        feature = aFeature;
    }

    public Object getValue()
    {
        return value;
    }

    public void setValue(Object aValue)
    {
        value = aValue;
    }
}

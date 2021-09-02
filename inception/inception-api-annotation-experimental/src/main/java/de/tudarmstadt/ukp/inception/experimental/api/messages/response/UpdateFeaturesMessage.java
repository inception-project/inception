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
import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationFeature;

public class UpdateFeaturesMessage
{
    private VID annotationId;
    private AnnotationFeature feature;
    private Object value;

    public UpdateFeaturesMessage(VID aAnnotationId, AnnotationFeature aFeature, Object aValue)
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

    public AnnotationFeature getFeature()
    {
        return feature;
    }

    public void setFeature(AnnotationFeature aFeature)
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

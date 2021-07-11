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

import org.apache.uima.cas.Feature;
import org.apache.uima.cas.Type;

import de.tudarmstadt.ukp.clarin.webanno.api.annotation.model.VID;

public class UpdateAnnotationResponse
{
    private VID annotationAddress;
    private Type type;
    private Feature feature;
    private String color;

    public VID getAnnotationAddress()
    {
        return annotationAddress;
    }

    public void setAnnotationAddress(VID aAnnotationAddress) {
        annotationAddress = aAnnotationAddress;
    }

    public Type getType()
    {
        return type;
    }

    public void setType(Type aType)
    {
        type = aType;
    }

    public Feature getFeature()
    {
        return feature;
    }

    public void setFeature(Feature aFeature)
    {
        feature = aFeature;
    }

    public String getColor()
    {
        return color;
    }

    public void setColor(String aColor)
    {
        color = aColor;
    }
}

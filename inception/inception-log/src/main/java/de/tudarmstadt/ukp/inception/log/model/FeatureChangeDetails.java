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
package de.tudarmstadt.ukp.inception.log.model;

import java.util.Collection;
import java.util.stream.Collectors;

import org.apache.uima.cas.FeatureStructure;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;

import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationFeature;

@JsonInclude(Include.NON_NULL)
public class FeatureChangeDetails
{
    private AnnotationDetails ann;
    private String feature;
    private Object value;
    private Object previousValue;

    public FeatureChangeDetails()
    {
        // Nothing to do
    }

    public FeatureChangeDetails(FeatureStructure aFS, AnnotationFeature aFeature, Object aNew,
            Object aOld)
    {
        ann = new AnnotationDetails(aFS);
        setFeature(aFeature.getName());
        setValue(aNew);
        setPreviousValue(aOld);
    }

    public AnnotationDetails getAnnotation()
    {
        return ann;
    }

    public void setAnnotation(AnnotationDetails aAnn)
    {
        ann = aAnn;
    }

    public String getFeature()
    {
        return feature;
    }

    public void setFeature(String aFeature)
    {
        feature = aFeature;
    }

    public Object getValue()
    {
        return value;
    }

    public void setValue(Object aValue)
    {
        value = sanitize(aValue);
    }

    public Object getPreviousValue()
    {
        return previousValue;
    }

    public void setPreviousValue(Object aPreviousValue)
    {
        previousValue = sanitize(aPreviousValue);
    }

    private Object sanitize(Object aObject)
    {
        if (aObject instanceof Collection) {
            Collection<?> values = (Collection<?>) aObject;
            return values.stream().map(it -> sanitize(it)).collect(Collectors.toList());
        }
        if (aObject instanceof FeatureStructure) {
            // Not an optimal solution but avoids stack overflow errors at the moment
            return aObject.toString();
        }
        else {
            return aObject;
        }
    }
}

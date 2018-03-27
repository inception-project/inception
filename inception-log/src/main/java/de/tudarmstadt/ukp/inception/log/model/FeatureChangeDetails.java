/*
 * Copyright 2018
 * Ubiquitous Knowledge Processing (UKP) Lab
 * Technische Universität Darmstadt
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package de.tudarmstadt.ukp.inception.log.model;

import org.apache.uima.cas.text.AnnotationFS;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;

@JsonInclude(Include.NON_NULL)
public class FeatureChangeDetails
{
    private AnnotationDetails ann;
    private Object value;
    private Object previousValue;

    public FeatureChangeDetails()
    {
        // Nothing to do
    }
    
    public FeatureChangeDetails(AnnotationFS aFS, Object aNew, Object aOld)
    {
        ann = new AnnotationDetails(aFS);
        value = aNew;
        previousValue = aOld;
    }

    public AnnotationDetails getAnnotation()
    {
        return ann;
    }

    public void setAnnotation(AnnotationDetails aAnn)
    {
        ann = aAnn;
    }

    public Object getValue()
    {
        return value;
    }

    public void setValue(Object aValue)
    {
        value = aValue;
    }

    public Object getPreviousValue()
    {
        return previousValue;
    }

    public void setPreviousValue(Object aPreviousValue)
    {
        previousValue = aPreviousValue;
    }
}

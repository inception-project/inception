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
package de.tudarmstadt.ukp.inception.annotation.events;

import org.apache.uima.cas.FeatureStructure;
import org.apache.uima.cas.text.AnnotationFS;

import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationFeature;
import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationLayer;
import de.tudarmstadt.ukp.clarin.webanno.model.SourceDocument;
import de.tudarmstadt.ukp.inception.rendering.model.Range;
import de.tudarmstadt.ukp.inception.support.uima.ICasUtil;
import de.tudarmstadt.ukp.inception.support.wicket.event.HybridApplicationUIEvent;

public class FeatureValueUpdatedEvent
    extends AnnotationEvent
    implements HybridApplicationUIEvent
{
    private static final long serialVersionUID = -6246331778850797138L;

    private final FeatureStructure fs;
    private final AnnotationFeature feature;
    private final Object oldValue;
    private final Object newValue;

    public FeatureValueUpdatedEvent(Object aSource, SourceDocument aDocument, String aUser,
            AnnotationLayer aLayer, FeatureStructure aFS, AnnotationFeature aFeature,
            Object aNewValue, Object aOldValue)
    {
        super(aSource, aDocument, aUser, aLayer);

        fs = aFS;
        feature = aFeature;
        oldValue = aOldValue;
        newValue = aNewValue;
    }

    public FeatureStructure getFS()
    {
        return fs;
    }

    public AnnotationFeature getFeature()
    {
        return feature;
    }

    @Override
    public Range getAffectedRange()
    {
        if (fs instanceof AnnotationFS) {
            return new Range((AnnotationFS) fs);
        }

        return Range.UNDEFINED;
    }

    public Object getOldValue()
    {
        return oldValue;
    }

    public Object getNewValue()
    {
        return newValue;
    }

    @Override
    public String toString()
    {
        StringBuilder builder = new StringBuilder();
        builder.append("FeatureValueUpdatedEvent [");
        if (getDocument() != null) {
            builder.append("docID=");
            builder.append(getDocument());
            builder.append(", user=");
            builder.append(getDocumentOwner());
            builder.append(", ");
        }
        builder.append("addr=");
        builder.append(ICasUtil.getAddr(fs));
        builder.append(", feature=");
        builder.append(feature.getName());
        builder.append(", old=");
        builder.append(oldValue);
        builder.append(", new=");
        builder.append(newValue);
        builder.append("]");
        return builder.toString();
    }
}

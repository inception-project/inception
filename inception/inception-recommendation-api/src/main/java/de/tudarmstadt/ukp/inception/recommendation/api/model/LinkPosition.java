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
package de.tudarmstadt.ukp.inception.recommendation.api.model;

import org.apache.uima.cas.text.AnnotationFS;

public class LinkPosition
    extends ArcPosition_ImplBase<LinkPosition>
{
    private static final long serialVersionUID = 4899546086036031468L;

    private final String feature;

    public LinkPosition(String aFeature, AnnotationFS aSource, AnnotationFS aTarget)
    {
        super(aSource, aTarget);
        feature = aFeature;
    }

    public LinkPosition(String aFeature, int aSourceBegin, int aSourceEnd, int aTargetBegin,
            int aTargetEnd)
    {
        super(aSourceBegin, aSourceEnd, aTargetBegin, aTargetEnd);
        feature = aFeature;
    }

    public LinkPosition(LinkPosition aOther)
    {
        super(aOther);
        feature = aOther.feature;
    }

}

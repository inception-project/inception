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
package de.tudarmstadt.ukp.inception.schema.api;

import org.apache.uima.cas.text.AnnotationFS;

import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationLayer;

public class AttachedAnnotation
{
    public static enum Direction
    {
        INCOMING, OUTGOING, LOOP;
    }

    private final AnnotationFS endpoint;
    private final AnnotationFS relation;
    private final AnnotationLayer layer;
    private final Direction direction;

    public AttachedAnnotation(AnnotationLayer aLayer, AnnotationFS aEndpoint, Direction aDirection)
    {
        this(aLayer, null, aEndpoint, aDirection);
    }

    public AttachedAnnotation(AnnotationLayer aLayer, AnnotationFS aRelation,
            AnnotationFS aEndpoint, Direction aDirection)
    {
        layer = aLayer;
        relation = aRelation;
        endpoint = aEndpoint;
        direction = aDirection;
    }

    public AnnotationLayer getLayer()
    {
        return layer;
    }

    public AnnotationFS getRelation()
    {
        return relation;
    }

    public AnnotationFS getEndpoint()
    {
        return endpoint;
    }

    public Direction getDirection()
    {
        return direction;
    }
}

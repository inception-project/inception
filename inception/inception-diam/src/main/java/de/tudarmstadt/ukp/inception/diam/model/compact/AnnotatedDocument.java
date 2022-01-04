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
package de.tudarmstadt.ukp.inception.diam.model.compact;

import static com.fasterxml.jackson.annotation.JsonInclude.Include.NON_EMPTY;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonInclude;

import de.tudarmstadt.ukp.inception.diam.model.ajax.AjaxResponse;

public class AnnotatedDocument
    extends AjaxResponse
{
    public static final String COMMAND = "getAnnotatedDocument";

    private String text;

    private @JsonInclude(NON_EMPTY) List<Relation> relations = new ArrayList<>();

    private @JsonInclude(NON_EMPTY) List<Span> entities = new ArrayList<>();
    private @JsonInclude(NON_EMPTY) List<AnnotationComment> comments = new ArrayList<>();
    private @JsonInclude(NON_EMPTY) List<LazyDetailQuery> normalizations = new ArrayList<>();

    private Map<String, List<Marker>> args = new HashMap<>();

    public AnnotatedDocument()
    {
        super(COMMAND);
    }

    public String getText()
    {
        return text;
    }

    public void setText(String aText)
    {
        text = aText;
    }

    public List<AnnotationComment> getComments()
    {
        return comments;
    }

    public void setComments(List<AnnotationComment> aComments)
    {
        comments = aComments;
    }

    public void addComment(AnnotationComment aComment)
    {
        comments.add(aComment);
    }

    public List<LazyDetailQuery> getNormalizations()
    {
        return normalizations;
    }

    public void setNormalizations(List<LazyDetailQuery> aNormalizations)
    {
        normalizations = aNormalizations;
    }

    public void addNormalization(LazyDetailQuery aNormalization)
    {
        normalizations.add(aNormalization);
    }

    public List<Relation> getRelations()
    {
        return relations;
    }

    public void setRelations(List<Relation> aRelations)
    {
        relations = aRelations;
    }

    public void addRelation(Relation aRelation)
    {
        relations.add(aRelation);
    }

    public List<Span> getEntities()
    {
        return entities;
    }

    public void setEntities(List<Span> aEntities)
    {
        entities = aEntities;
    }

    public void addEntity(Span aEntity)
    {
        entities.add(aEntity);
    }

    public void addMarker(Marker aMarker)
    {
        List<Marker> markers = args.get(aMarker.getType());
        if (markers == null) {
            markers = new ArrayList<>();
            args.put(aMarker.getType(), markers);
        }
        markers.add(aMarker);
    }

    public Map<String, List<Marker>> getArgs()
    {
        return args;
    }

    public void setArgs(Map<String, List<Marker>> aArgs)
    {
        args = aArgs;
    }
}

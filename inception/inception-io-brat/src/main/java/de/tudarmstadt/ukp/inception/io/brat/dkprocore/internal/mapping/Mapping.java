/*
 * Copyright 2019
 * Ubiquitous Knowledge Processing (UKP) Lab
 * Technische Universität Darmstadt
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package de.tudarmstadt.ukp.inception.io.brat.dkprocore.internal.mapping;

import static java.util.Collections.emptyMap;
import static java.util.function.Function.identity;
import static java.util.stream.Collectors.toMap;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import org.apache.commons.collections4.MultiValuedMap;
import org.apache.commons.collections4.multimap.ArrayListValuedHashMap;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

public class Mapping
{
    private final TypeMappings textTypeMapppings;
    private final TypeMappings relationTypeMapppings;
    private final Map<String, SpanMapping> textAnnotations;
    private final Map<String, RelationMapping> relations;
    private final MultiValuedMap<String, CommentMapping> comments;

    public Mapping()
    {
        this(new TypeMappings(), new TypeMappings(), new ArrayList<>(), new ArrayList<>(),
                new ArrayList<>());
    }

    @JsonCreator
    public Mapping(@JsonProperty(value = "textTypeMapppings") TypeMappings aTextTypeMapppings,
            @JsonProperty(value = "relationTypeMapppings") TypeMappings aRelationTypeMapppings,
            @JsonProperty(value = "spans") List<SpanMapping> aTextAnnotations,
            @JsonProperty(value = "relations") List<RelationMapping> aRelations,
            @JsonProperty(value = "comments") List<CommentMapping> aComments)
    {
        textTypeMapppings = aTextTypeMapppings;
        relationTypeMapppings = aRelationTypeMapppings;

        textAnnotations = aTextAnnotations != null
                ? aTextAnnotations.stream().collect(toMap(SpanMapping::getType, identity()))
                : emptyMap();
        relations = aRelations != null
                ? aRelations.stream().collect(toMap(RelationMapping::getType, identity()))
                : emptyMap();

        comments = new ArrayListValuedHashMap<>();
        if (aComments != null) {
            aComments.forEach(mapping -> comments.put(mapping.getType(), mapping));
        }
    }

    public TypeMappings getTextTypeMapppings()
    {
        return textTypeMapppings;
    }

    public TypeMappings getRelationTypeMapppings()
    {
        return relationTypeMapppings;
    }

    public SpanMapping getSpanMapping(String aType)
    {
        return textAnnotations.get(aType);
    }

    public RelationMapping getRelationMapping(String aType)
    {
        return relations.get(aType);
    }

    public Collection<CommentMapping> getCommentMapping(String aType)
    {
        return comments.get(aType);
    }
}

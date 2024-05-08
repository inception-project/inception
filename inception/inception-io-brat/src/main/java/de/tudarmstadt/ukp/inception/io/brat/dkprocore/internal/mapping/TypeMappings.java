/*
 * Copyright 2017
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

import static java.util.stream.Collectors.joining;
import static java.util.stream.Collectors.toList;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.StreamSupport;

import org.apache.uima.cas.Type;
import org.apache.uima.cas.TypeSystem;

import com.fasterxml.jackson.annotation.JsonCreator;

import de.tudarmstadt.ukp.inception.io.brat.dkprocore.internal.model.BratAnnotation;

public class TypeMappings
{
    private final List<TypeMapping> parsedMappings;
    private final Map<String, Type> brat2UimaMappingCache;
    private final Map<String, String> uima2BratMappingCache;

    public TypeMappings()
    {
        parsedMappings = new ArrayList<>();
        brat2UimaMappingCache = new HashMap<>();
        uima2BratMappingCache = new HashMap<>();
    }

    @JsonCreator
    public TypeMappings(List<TypeMapping> aMappings)
    {
        parsedMappings = aMappings;
        brat2UimaMappingCache = new HashMap<>();
        uima2BratMappingCache = new HashMap<>();
    }

    public TypeMappings(String... aMappings)
    {
        parsedMappings = new ArrayList<>();

        if (aMappings != null) {
            for (String m : aMappings) {
                parsedMappings.add(TypeMapping.parse(m));
            }
        }

        brat2UimaMappingCache = new HashMap<>();
        uima2BratMappingCache = new HashMap<>();
    }

    private String apply(String aType)
    {
        String type = aType;
        for (TypeMapping m : parsedMappings) {
            if (m.matches(aType)) {
                type = m.apply();
                break;
            }
        }
        return type;
    }

    public TypeMapping getMappingByBratType(String aBratType)
    {
        return parsedMappings.stream() //
                .filter(mapping -> mapping.matches(aBratType)) //
                .findFirst() //
                .orElse(null);
    }

    public Type getUimaType(TypeSystem aTs, BratAnnotation aAnno)
    {
        var t = brat2UimaMappingCache.get(aAnno.getType());
        if (t != null) {
            return t;
        }

        // brat doesn't like dots in name names, so we had replaced them with dashes.
        // Now revert.
        var type = apply(aAnno.getType().replace("-", "."));
        t = aTs.getType(type);

        // if the lookup didn't work with replacing the dashes, try without, e.g. because the
        // brat name *really* contains dashes and we only resolve them through mapping
        if (t == null) {
            type = apply(aAnno.getType());
            t = aTs.getType(type);
        }

        if (t != null) {
            brat2UimaMappingCache.put(aAnno.getType(), t);
            return t;
        }

        var shortNameCandidates = StreamSupport.stream(aTs.spliterator(), false) //
                .filter($ -> $.getShortName().equals(aAnno.getType())) //
                .collect(toList());

        if (shortNameCandidates.size() == 1) {
            t = shortNameCandidates.get(0);
            brat2UimaMappingCache.put(aAnno.getType(), t);
            return t;
        }

        if (shortNameCandidates.size() > 1) {
            throw new IllegalStateException("Multiple possible  UIMA types for brat type ["
                    + aAnno.getType() + "]: ["
                    + shortNameCandidates.stream().map(Type::getName).collect(joining(", ")) + "]");
        }

        throw new IllegalStateException(
                "Unable to find appropriate UIMA type for brat type [" + aAnno.getType() + "]");
    }

    public String getBratType(Type aType)
    {
        String bratType = uima2BratMappingCache.get(aType.getName());

        if (bratType == null) {
            String uimaType = aType.getName();

            for (TypeMapping m : parsedMappings) {
                if (m.matches(aType.getName())) {
                    uimaType = m.apply();
                    break;
                }
            }

            // brat doesn't like dots in name names, so we had replaced them with dashes.
            bratType = uimaType.replace(".", "-");
            uima2BratMappingCache.put(uimaType, bratType);
        }

        return bratType;
    }
}

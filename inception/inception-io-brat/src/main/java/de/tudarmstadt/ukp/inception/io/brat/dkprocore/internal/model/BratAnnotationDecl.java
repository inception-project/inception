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
package de.tudarmstadt.ukp.inception.io.brat.dkprocore.internal.model;

import java.util.LinkedHashSet;
import java.util.Set;

public class BratAnnotationDecl
{
    private final String superType;
    private final String type;

    private final Set<BratAnnotationDecl> subTypes = new LinkedHashSet<>();

    public BratAnnotationDecl(String aSuperType, String aType)
    {
        superType = aSuperType;
        type = aType;
    }

    public String getSuperType()
    {
        return superType;
    }

    public String getType()
    {
        return type;
    }

    public void addSubType(BratAnnotationDecl aDecl)
    {
        subTypes.add(aDecl);
    }

    public Set<BratAnnotationDecl> getSubTypes()
    {
        return subTypes;
    }
}

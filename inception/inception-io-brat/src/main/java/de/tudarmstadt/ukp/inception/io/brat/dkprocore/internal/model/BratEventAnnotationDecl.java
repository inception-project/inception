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

import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.Set;

public class BratEventAnnotationDecl
    extends BratAnnotationDecl
{
    private final Set<BratEventArgumentDecl> slots = new LinkedHashSet<>();

    public BratEventAnnotationDecl(String aSuperType, String aType, BratEventArgumentDecl... aSlots)
    {
        super(aSuperType, aType);
        Arrays.stream(aSlots).forEach(s -> addSlot(s));
    }

    public void addSlot(BratEventArgumentDecl aSlot)
    {
        slots.add(aSlot);
    }

    public Set<BratEventArgumentDecl> getSlots()
    {
        return slots;
    }

    @Override
    public String toString()
    {
        StringBuilder sb = new StringBuilder();
        sb.append(getType());
        sb.append('\t');
        boolean first = true;
        for (BratEventArgumentDecl slot : slots) {
            if (!first) {
                sb.append(", ");
            }
            first = false;

            sb.append(slot.toString());
        }

        return sb.toString();
    }
}

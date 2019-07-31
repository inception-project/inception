/*
 * Copyright 2019
 * Ubiquitous Knowledge Processing (UKP) Lab and FG Language Technology
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
package de.tudarmstadt.ukp.clarin.webanno.curation.casdiff.internal;

public class AID
{
    public final int addr;
    public final String feature;
    public final int index;

    public AID(int aAddr)
    {
        this(aAddr, null, -1);
    }
    
    public AID(int aAddr, String aFeature, int aIndex)
    {
        addr = aAddr;
        feature = aFeature;
        index = aIndex;
    }

    @Override
    public String toString()
    {
        StringBuilder builder = new StringBuilder();
        builder.append("AID [addr=");
        builder.append(addr);
        if (feature != null) {
            builder.append(", feature=");
            builder.append(feature);
            builder.append(", index=");
            builder.append(index);
        }
        builder.append("]");
        return builder.toString();
    }
}

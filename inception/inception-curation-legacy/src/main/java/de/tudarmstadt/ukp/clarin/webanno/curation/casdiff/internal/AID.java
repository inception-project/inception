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
package de.tudarmstadt.ukp.clarin.webanno.curation.casdiff.internal;

import java.io.Serializable;
import java.util.Objects;

import de.tudarmstadt.ukp.inception.rendering.vmodel.VID;

public class AID
    implements Serializable
{
    private static final long serialVersionUID = 2509249097545433991L;

    public final int addr;
    public final String feature;
    public final int index;

    public AID(VID aVid)
    {
        this(aVid.getId(), null, -1);
    }

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
    public int hashCode()
    {
        return Objects.hash(addr, feature, index);
    }

    @Override
    public boolean equals(Object obj)
    {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        AID other = (AID) obj;
        return addr == other.addr && Objects.equals(feature, other.feature) && index == other.index;
    }

    @Override
    public String toString()
    {
        var builder = new StringBuilder();
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

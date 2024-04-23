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
package de.tudarmstadt.ukp.clarin.webanno.curation.casdiff;

import static de.tudarmstadt.ukp.inception.support.uima.ICasUtil.selectFsByAddr;
import static java.util.stream.Collectors.joining;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

import org.apache.uima.cas.CAS;
import org.apache.uima.cas.FeatureStructure;

import de.tudarmstadt.ukp.clarin.webanno.curation.casdiff.api.Position;
import de.tudarmstadt.ukp.clarin.webanno.curation.casdiff.internal.AID;
import de.tudarmstadt.ukp.inception.support.uima.ICasUtil;

/**
 * A single configuration seen at a particular position. The configuration may have been observed in
 * multiple CASes.
 */
public class Configuration
    implements Serializable
{
    private static final long serialVersionUID = 5387873327207575817L;

    private final Position position;
    final Map<String, AID> fsAddresses = new TreeMap<>();
    private List<AID> extras;

    /**
     * Flag indicating that there is at least once CAS group containing more than one annotation at
     * this position - i.e. a stacked annotation.
     */
    private boolean stacked = false;

    public String getRepresentativeCasGroupId()
    {
        return fsAddresses.entrySet().iterator().next().getKey();
    }

    public Set<String> getCasGroupIds()
    {
        return fsAddresses.keySet();
    }

    public Configuration(Position aPosition)
    {
        position = aPosition;
    }

    public Position getPosition()
    {
        return position;
    }

    public boolean isStacked()
    {
        return stacked;
    }

    /**
     * Visible for testing only!
     */
    @SuppressWarnings("javadoc")
    public void add(String aCasGroupId, AID aAID)
    {
        var old = fsAddresses.put(aCasGroupId, aAID);
        if (old != null) {
            if (extras == null) {
                extras = new ArrayList<>();
            }
            extras.add(old);
            stacked = true;
        }
    }

    void add(String aCasGroupId, FeatureStructure aFS)
    {
        add(aCasGroupId, new AID(ICasUtil.getAddr(aFS)));
    }

    void add(String aCasGroupId, FeatureStructure aFS, String aFeature, int aSlot)
    {
        add(aCasGroupId, new AID(ICasUtil.getAddr(aFS), aFeature, aSlot));
    }

    public AID getRepresentativeAID()
    {
        var e = fsAddresses.entrySet().iterator().next();
        return e.getValue();
    }

    public FeatureStructure getRepresentative(Map<String, CAS> aCasMap)
    {
        var e = fsAddresses.entrySet().iterator().next();
        return selectFsByAddr(aCasMap.get(e.getKey()), e.getValue().addr);
    }

    public AID getAID(String aCasGroupId)
    {
        return fsAddresses.get(aCasGroupId);
    }

    public boolean contains(String aCasGroupId, FeatureStructure aFS)
    {
        var aid = new AID(ICasUtil.getAddr(aFS));

        return contains(aCasGroupId, aid);
    }

    public boolean contains(String aCasGroupId, AID aAID)
    {
        if (aAID.equals(fsAddresses.get(aCasGroupId))) {
            return true;
        }

        if (extras == null) {
            return false;
        }

        return extras.contains(aAID);
    }

    private <T extends FeatureStructure> FeatureStructure getFs(String aCasGroupId, Class<T> aClass,
            Map<String, CAS> aCasMap)
    {
        AID aid = fsAddresses.get(aCasGroupId);
        if (aid == null) {
            return null;
        }

        CAS cas = aCasMap.get(aCasGroupId);
        if (cas == null) {
            return null;
        }

        return ICasUtil.selectFsByAddr(cas, aid.addr);
    }

    public FeatureStructure getFs(String aCasGroupId, Map<String, CAS> aCasMap)
    {
        return getFs(aCasGroupId, FeatureStructure.class, aCasMap);
    }

    private <T extends FeatureStructure> List<FeatureStructure> getFses(String aCasGroupId,
            Class<T> aClass, Map<String, CAS> aCasMap)
    {
        AID aid = fsAddresses.get(aCasGroupId);
        if (aid == null) {
            return Collections.emptyList();
        }

        CAS cas = aCasMap.get(aCasGroupId);
        if (cas == null) {
            return Collections.emptyList();
        }
        var allFs = new ArrayList<FeatureStructure>();
        allFs.add(ICasUtil.selectFsByAddr(cas, aid.addr));

        if (extras != null) {
            for (var eAid : extras) {
                allFs.add(ICasUtil.selectFsByAddr(cas, eAid.addr));
            }
        }

        return allFs;
    }

    public List<FeatureStructure> getFses(String aCasGroupId, Map<String, CAS> aCasMap)
    {
        return getFses(aCasGroupId, FeatureStructure.class, aCasMap);
    }

    @Override
    public String toString()
    {
        var sb = new StringBuilder();
        sb.append('[');
        for (var e : fsAddresses.entrySet()) {
            if (sb.length() > 1) {
                sb.append(", ");
            }
            sb.append(e.getKey());
            sb.append(": ");
            sb.append(e.getValue());
            if (extras != null) {
                sb.append(" (extras: ");
                sb.append(extras.stream().map(String::valueOf).collect(joining(", ")));
                sb.append(")");
            }
        }
        sb.append("] -> ");
        sb.append(getRepresentativeAID());
        return sb.toString();
    }
}

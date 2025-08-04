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

import static java.util.Arrays.asList;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import org.apache.uima.cas.FeatureStructure;

import de.tudarmstadt.ukp.clarin.webanno.curation.casdiff.internal.AID;
import de.tudarmstadt.ukp.inception.curation.api.Position;

/**
 * The set of configurations seen at a particular position.
 */
public class ConfigurationSet
    implements Serializable
{
    private static final long serialVersionUID = -2820621316555472339L;

    private final Position position;

    private List<Configuration> configurations = new ArrayList<>();
    private Set<String> casGroupIds = new LinkedHashSet<>();

    private EnumSet<Tag> tags = EnumSet.noneOf(Tag.class);
    private Map<String, Set<Object>> values = new HashMap<>();

    public ConfigurationSet(Position aPosition)
    {
        position = aPosition;
    }

    public ConfigurationSet addTags(Tag... aTag)
    {
        if (aTag != null) {
            tags.addAll(asList(aTag));
        }

        return this;
    }

    public ConfigurationSet removeTags(Tag... aTag)
    {
        if (aTag != null) {
            tags.removeAll(asList(aTag));
        }

        return this;
    }

    /**
     * @return tags added during agreement calculation (not by {@link CasDiff}!)
     */
    public EnumSet<Tag> getTags()
    {
        return tags;
    }

    public boolean hasTag(Tag aTag)
    {
        return tags.contains(aTag);
    }

    public void addValue(String aDataOwner, Object aValue)
    {
        values.computeIfAbsent(aDataOwner, $ -> new HashSet<>()).add(aValue);
    }

    /**
     * @param aDataOwner
     *            owner of the values to be retrieved
     * @return values added during agreement calculation (not by {@link CasDiff}!)
     */
    public Set<Object> getValues(String aDataOwner)
    {
        return values.get(aDataOwner);
    }

    /**
     * @return the total number of configurations recorded in this set. If a configuration has been
     *         seen in multiple CASes, it will be counted multiple times.
     */
    public int getRecordedConfigurationCount()
    {
        int i = 0;
        for (var cfg : configurations) {
            i += cfg.getCasGroupIds().size();
        }
        return i;
    }

    /**
     * @return the IDs of the CASes in which this configuration set has been observed.
     */
    public Set<String> getCasGroupIds()
    {
        return casGroupIds;
    }

    public void addCasGroupId(String aCasGroupId)
    {
        casGroupIds.add(aCasGroupId);
    }

    /**
     * @return the different configurations observed in this set.
     */
    public List<Configuration> getConfigurations()
    {
        return configurations;
    }

    public void addConfiguration(Configuration aCfg)
    {
        configurations.add(aCfg);
    }

    public Optional<Configuration> findConfiguration(String aCasGroupId, FeatureStructure aFS)
    {
        return configurations.stream().filter(cfg -> cfg.contains(aCasGroupId, aFS)).findFirst();
    }

    public Optional<Configuration> findConfiguration(String aCasGroupId, AID aAID)
    {
        return configurations.stream().filter(cfg -> cfg.contains(aCasGroupId, aAID)).findFirst();
    }

    /**
     * @param aCasGroupId
     *            a CAS ID
     * @return the different configurations observed in this set for the given CAS ID.
     */
    public List<Configuration> getConfigurations(String aCasGroupId)
    {
        var configurationsForUser = new ArrayList<Configuration>();
        for (var cfg : configurations) {
            if (cfg.fsAddresses.keySet().contains(aCasGroupId)) {
                configurationsForUser.add(cfg);
            }
        }
        return configurationsForUser;
    }

    /**
     * @return the position of this configuration set.
     */
    public Position getPosition()
    {
        return position;
    }

    /**
     * @return flag indicating that there is at least once CAS group containing more than one
     *         annotation at this position - i.e. a stacked annotation.
     */
    public boolean containsStackedConfigurations()
    {
        // User has annotated the position with multiple different configurations
        for (var casGroupId : getCasGroupIds()) {
            if (getConfigurations(casGroupId).size() > 1) {
                return true;
            }
        }

        // User has annotate the position multiple times with the same configuration
        return getConfigurations().stream() //
                .filter(Configuration::containsDuplicates) //
                .findAny().isPresent();
    }

    @Override
    public String toString()
    {
        var sb = new StringBuilder();
        sb.append('[');
        sb.append(position);
        sb.append(']');
        return sb.toString();
    }
}

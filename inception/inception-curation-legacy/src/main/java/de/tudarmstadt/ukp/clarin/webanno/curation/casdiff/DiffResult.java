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
import static org.apache.commons.collections4.CollectionUtils.subtract;

import java.io.PrintStream;
import java.io.Serializable;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import org.apache.uima.cas.FeatureStructure;

import de.tudarmstadt.ukp.clarin.webanno.curation.casdiff.internal.AID;
import de.tudarmstadt.ukp.inception.curation.api.Position;

/**
 * A description of the differences between CASes.
 */
public class DiffResult
    implements Serializable
{
    private static final long serialVersionUID = 5208017972858534258L;

    private final Map<Position, ConfigurationSet> data;
    private final Set<String> casGroupIds;
    private final Map<ConfigurationSet, Set<String>> unseenCasGroupIDsCache = new HashMap<>();

    private Boolean cachedHasDifferences;

    DiffResult(CasDiff aDiff)
    {
        data = Collections.unmodifiableMap(aDiff.configSets);
        casGroupIds = new LinkedHashSet<>(aDiff.casses.keySet());
    }

    public Set<String> getCasGroupIds()
    {
        return casGroupIds;
    }

    public boolean hasDifferences()
    {
        if (cachedHasDifferences == null) {
            cachedHasDifferences = !getDifferingConfigurationSets().isEmpty();
        }

        return cachedHasDifferences;
    }

    public boolean hasDifferencesWithExceptions(String... aCasGroupIDsToIgnore)
    {
        return !getDifferingConfigurationSetsWithExceptions(aCasGroupIDsToIgnore).isEmpty();
    }

    public Collection<Position> getPositions()
    {
        return data.keySet();
    }

    public Collection<ConfigurationSet> getConfigurationSets()
    {
        return data.values();
    }

    /**
     * @param aPosition
     *            a position.
     * @return the configuration set for the given position.
     */
    public ConfigurationSet getConfigurationSet(Position aPosition)
    {
        return data.get(aPosition);
    }

    public Optional<Configuration> findConfiguration(String aRepresentativeCasGroupId, AID aAid)
    {
        for (var cfgSet : getConfigurationSets()) {
            var cfg = cfgSet.findConfiguration(aRepresentativeCasGroupId, aAid);
            if (cfg.isPresent()) {
                return cfg;
            }
        }

        return Optional.empty();
    }

    public Optional<Configuration> findConfiguration(String aRepresentativeCasGroupId,
            FeatureStructure aFS)
    {
        for (var cfgSet : getConfigurationSets()) {
            var cfg = cfgSet.findConfiguration(aRepresentativeCasGroupId, aFS);
            if (cfg.isPresent()) {
                return cfg;
            }
        }

        return Optional.empty();
    }

    /**
     * Determine if all CASes see agreed on the given configuration set. This method returns
     * {@code false} if there was disagreement (there are multiple configurations in the set). When
     * using this method, make sure you also take into account whether the set is actually complete
     * (cf. {@link #isComplete(ConfigurationSet)}.
     * 
     * @param aConfigurationSet
     *            a configuration set.
     * @return if all seen CASes agreed on this set.
     */
    public boolean isAgreement(ConfigurationSet aConfigurationSet)
    {
        return isAgreementWithExceptions(aConfigurationSet);
    }

    /**
     * Determine if all CASes see agreed on the given configuration set. This method returns
     * {@code false} if there was disagreement (there are multiple configurations in the set). When
     * using this method, make sure you also take into account whether the set is actually complete
     * (cf. {@link #isComplete(ConfigurationSet)}.
     * 
     * @param aConfigurationSet
     *            a configuration set.
     * @param aCasGroupIDsToIgnore
     *            the exceptions - these CAS group IDs do not count towards completeness.
     * @return if all seen CASes agreed on this set.
     */
    public boolean isAgreementWithExceptions(ConfigurationSet aConfigurationSet,
            String... aCasGroupIDsToIgnore)
    {
        if (data.get(aConfigurationSet.getPosition()) != aConfigurationSet) {
            throw new IllegalArgumentException(
                    "Configuration set does not belong to this diff or positions mismatch");
        }

        // Shortcut: no exceptions
        if (aCasGroupIDsToIgnore == null || aCasGroupIDsToIgnore.length == 0) {
            // If there is only a single configuration in the set, we call it an agreement
            return aConfigurationSet.getConfigurations().size() == 1;
        }

        var exceptions = new HashSet<>(asList(aCasGroupIDsToIgnore));
        return aConfigurationSet.getConfigurations().stream()
                // Ignore configuration sets containing only exceptions and nothing else
                .filter(cfg -> !subtract(cfg.getCasGroupIds(), exceptions).isEmpty())
                // We can stop once we found 2 because if there are more than two configurations
                // then it cannot be an agreement.
                .limit(2)
                // So if there is exactly one configuration remaining, it is an agreement
                .count() == 1;

        // Issue 21 GitHub - REC - not really sure if we should call this an agreement
        // // If there are multiple configurations in the set, we only call it an agreement if
        // // at least one of these configurations has been made by all annotators
        // for (Configuration cfg : aConfigurationSet.configurations) {
        // HashSet<String> unseenGroupCasIDs = new HashSet<>(casGroupIds);
        // unseenGroupCasIDs.removeAll(cfg.fsAddresses.keySet());
        // if (unseenGroupCasIDs.isEmpty()) {
        // return true;
        // }
        // }
    }

    /**
     * Determine if the given set has been observed in all CASes.
     * 
     * @param aConfigurationSet
     *            a configuration set.
     * @return if seen in all CASes.
     */
    public boolean isComplete(ConfigurationSet aConfigurationSet)
    {
        return isCompleteWithExceptions(aConfigurationSet);
    }

    /**
     * Determine if the given set has been observed in all CASes but not considering the CASes from
     * the given CAS groups.
     * 
     * @param aConfigurationSet
     *            a configuration set.
     * @param aCasGroupIDsToIgnore
     *            the exceptions - these CAS group IDs do not count towards completeness.
     * @return if seen in all CASes.
     */
    public boolean isCompleteWithExceptions(ConfigurationSet aConfigurationSet,
            String... aCasGroupIDsToIgnore)
    {
        if (data.get(aConfigurationSet.getPosition()) != aConfigurationSet) {
            throw new IllegalArgumentException(
                    "Configuration set does not belong to this diff or positions mismatch");
        }

        Set<String> unseenGroupCasIDs = unseenCasGroupIDsCache.get(aConfigurationSet);
        if (unseenGroupCasIDs == null) {
            unseenGroupCasIDs = new HashSet<>(casGroupIds);
            for (Configuration cfg : aConfigurationSet.getConfigurations()) {
                unseenGroupCasIDs.removeAll(cfg.fsAddresses.keySet());
            }
            unseenCasGroupIDsCache.put(aConfigurationSet, unseenGroupCasIDs);
        }

        // Short-cut: no exceptions to consider
        if (aCasGroupIDsToIgnore == null || aCasGroupIDsToIgnore.length == 0) {
            return unseenGroupCasIDs.isEmpty();
        }

        // Short-cut: the common use-case is to ignore a single exception, usually the curator
        if (aCasGroupIDsToIgnore.length == 1 && unseenGroupCasIDs.size() == 1) {
            return unseenGroupCasIDs.contains(aCasGroupIDsToIgnore[0]);
        }

        // The set is complete if the unseen CAS group IDs match exactly the exceptions.
        return subtract(unseenGroupCasIDs, asList(aCasGroupIDsToIgnore)).isEmpty();
    }

    public Map<Position, ConfigurationSet> getDifferingConfigurationSets()
    {
        return getDifferingConfigurationSetsWithExceptions();
    }

    public Map<Position, ConfigurationSet> getDifferingConfigurationSetsWithExceptions(
            String... aCasGroupIDsToIgnore)
    {
        var diffs = new LinkedHashMap<Position, ConfigurationSet>();
        for (var e : data.entrySet()) {
            if (!isAgreementWithExceptions(e.getValue(), aCasGroupIDsToIgnore)) {
                diffs.put(e.getKey(), e.getValue());
            }
        }

        return diffs;
    }

    public Map<Position, ConfigurationSet> getIncompleteConfigurationSets()
    {
        return getIncompleteConfigurationSetsWithExceptions();
    }

    /**
     * @return the incomplete configuration sets per position
     * @param aCasGroupIDsToIgnore
     *            the exceptions - these CAS group IDs do not count towards completeness.
     */
    public Map<Position, ConfigurationSet> getIncompleteConfigurationSetsWithExceptions(
            String... aCasGroupIDsToIgnore)
    {
        var diffs = new LinkedHashMap<Position, ConfigurationSet>();
        for (var e : data.entrySet()) {
            if (!isCompleteWithExceptions(e.getValue(), aCasGroupIDsToIgnore)) {
                diffs.put(e.getKey(), e.getValue());
            }
        }

        return diffs;
    }

    public int size()
    {
        return data.size();
    }

    public int size(String aType)
    {
        int n = 0;
        for (var pos : data.keySet()) {
            if (pos.getType().equals(aType)) {
                n++;
            }
        }

        return n;
    }

    public void print(PrintStream aOut)
    {
        for (var p : getPositions()) {
            var configurationSet = getConfigurationSet(p);
            aOut.printf("=== %s -> %s %s%n", p,
                    configurationSet.containsStackedConfigurations() ? "STACKED"
                            : isAgreement(configurationSet) ? "AGREE" : "DISAGREE",
                    isComplete(configurationSet) ? "COMPLETE" : "INCOMPLETE");
            for (var cfg : configurationSet.getConfigurations()) {
                aOut.printf("  %s%n", cfg);
            }
        }
    }
}

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
package de.tudarmstadt.ukp.clarin.webanno.constraints.model;

import static java.util.Collections.unmodifiableMap;
import static java.util.Collections.unmodifiableSet;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.uima.cas.FeatureStructure;

import de.tudarmstadt.ukp.clarin.webanno.constraints.grammar.ASTConstraintsSet;

public class ParsedConstraints
    implements Serializable
{
    private static final long serialVersionUID = -2401965871743170805L;

    private final Map<String, String> imports = new LinkedHashMap<>();
    private final List<Scope> scopes = new ArrayList<>();
    private final Map<String, Scope> scopeMap;
    // Contains possible scenarios for which rules are available.
    private final Set<FSFPair> pathsUsedInRestrictions;

    public ParsedConstraints(Map<String, String> aAliases, Map<String, List<Rule>> aScopes)
    {
        imports.putAll(aAliases);

        for (var e : aScopes.entrySet()) {
            var scope = new Scope(e.getKey(), e.getValue());
            scopes.add(scope);
        }

        pathsUsedInRestrictions = indexPathsUsedInRestrictions();
        scopeMap = buildScopeMap();
    }

    public ParsedConstraints(Map<String, String> aAliases, List<Scope> aScopes)
    {
        imports.putAll(aAliases);
        scopes.addAll(aScopes);
        pathsUsedInRestrictions = indexPathsUsedInRestrictions();
        scopeMap = buildScopeMap();
    }

    public ParsedConstraints(ASTConstraintsSet astConstraintsSet)
    {
        imports.putAll(astConstraintsSet.getImports());

        for (var ruleGroup : astConstraintsSet.getScopes().entrySet()) {
            var rules = new ArrayList<Rule>();
            for (var astRule : ruleGroup.getValue()) {
                rules.add(new Rule(astRule));
            }
            scopes.add(new Scope(ruleGroup.getKey(), rules));
        }

        scopeMap = buildScopeMap();
        pathsUsedInRestrictions = indexPathsUsedInRestrictions();
    }

    public Map<String, String> getImports()
    {
        return imports;
    }

    public String getShortName(String aLongName)
    {
        for (var e : imports.entrySet()) {
            if (e.getValue().equals(aLongName)) {
                return e.getKey();
            }
        }
        return null;
    }

    public List<Scope> getScopes()
    {
        return scopes;
    }

    public Scope getScopeByName(String scopeName)
    {
        if (scopeMap == null) { // initialize map if not set already
        }
        return scopeMap.get(scopeName);
    }

    /**
     * @return if rules exists or not
     */
    public boolean isPathUsedInAnyRestriction(FeatureStructure aContext, String aPath)
    {
        return isPathUsedInAnyRestriction(aContext.getType().getName(), aPath);
    }

    /**
     * @return if rules exists or not
     */
    public boolean isPathUsedInAnyRestriction(String aContextTypeName, String aPath)
    {
        if (pathsUsedInRestrictions == null) {
            indexPathsUsedInRestrictions();
        }

        var shortTypeName = getShortName(aContextTypeName);
        if (shortTypeName == null) {
            return false;
        }

        if (getScopeByName(shortTypeName) == null) {
            return false;
        }

        if (pathsUsedInRestrictions.contains(new FSFPair(shortTypeName, aPath))) {
            // If it has rules satisfying with proper input FS and affecting feature
            return true;
        }

        return false;
    }

    private Map<String, Scope> buildScopeMap()
    {
        var map = new HashMap<String, Scope>();
        for (var scope : scopes) {
            map.put(scope.getScopeName(), scope);
        }
        return unmodifiableMap(map);
    }

    /**
     * @return set of pairs where the key of each pair is a scope and the value is a restriction
     *         path
     */
    private Set<FSFPair> indexPathsUsedInRestrictions()
    {
        var rs = new HashSet<FSFPair>();
        FSFPair _temp;
        for (var scope : scopes) {
            for (var rule : scope.getRules()) {
                for (var restriction : rule.getRestrictions()) {
                    _temp = new FSFPair(scope.getScopeName(), restriction.getPath());
                    if (!rs.contains(_temp)) {
                        rs.add(_temp);
                    }
                }
            }
        }
        return unmodifiableSet(rs);
    }

    private String printImports()
    {
        var output = new StringBuilder();
        for (var e : imports.entrySet()) {
            output.append(e.getKey());
            output.append(" is short for ");
            output.append(e.getValue());
            output.append(System.lineSeparator());
        }
        return output.toString();
    }

    @Override
    public String toString()
    {
        return "Imports\n[" + printImports() + "]\n" + scopes.toString() + "]\n]";
    }
}

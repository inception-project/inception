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
import java.util.List;
import java.util.Map;
import java.util.Set;

import de.tudarmstadt.ukp.clarin.webanno.constraints.grammar.ASTConstraintsSet;

/***
 * Serialized Class containing objects after parsing and creating objects based on rules file.
 */
public class ParsedConstraints
    implements Serializable
{
    private static final long serialVersionUID = -2401965871743170805L;

    private final Map<String, String> imports;
    private final List<Scope> scopes;
    private final Map<String, Scope> scopeMap;
    // Contains possible scenarios for which rules are available.
    private final Set<FSFPair> rulesSet;

    public ParsedConstraints(Map<String, String> aAliases, List<Scope> aScopes)
    {
        imports = aAliases;
        scopes = aScopes;
        rulesSet = buildRulesSet();
        scopeMap = buildScopeMap();
    }

    public ParsedConstraints(ASTConstraintsSet astConstraintsSet)
    {
        imports = astConstraintsSet.getImports();
        scopes = new ArrayList<>();

        for (var ruleGroup : astConstraintsSet.getScopes().entrySet()) {
            var rules = new ArrayList<Rule>();
            for (var astRule : ruleGroup.getValue()) {
                rules.add(new Rule(astRule));
            }
            scopes.add(new Scope(ruleGroup.getKey(), rules));
        }

        scopeMap = buildScopeMap();
        rulesSet = buildRulesSet();
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
    public boolean areThereRules(String aFS, String aFeature)
    {
        if (rulesSet == null) {
            buildRulesSet();
        }

        if (getShortName(aFS) == null) {
            return false;
        }

        if (getScopeByName(getShortName(aFS)) == null) {
            return false;
        }

        var _tempFsfPair = new FSFPair(getShortName(aFS), aFeature);
        if (rulesSet.contains(_tempFsfPair)) {
            // If it has rules satisfying with proper input FS and affecting feature
            return true;
        }

        return false;
    }

    private Map<String, Scope> buildScopeMap()
    {
        var scopeMap = new HashMap<String, Scope>();
        for (Scope scope : scopes) {
            scopeMap.put(scope.getScopeName(), scope);
        }
        return unmodifiableMap(scopeMap);
    }

    /**
     * @return Set with values of different conditions for which rules are available.
     */
    private Set<FSFPair> buildRulesSet()
    {
        var rulesSet = new HashSet<FSFPair>();
        FSFPair _temp;
        for (var scope : scopes) {
            for (var rule : scope.getRules()) {
                for (var restriction : rule.getRestrictions()) {
                    _temp = new FSFPair(scope.getScopeName(), restriction.getPath());
                    if (!rulesSet.contains(_temp)) {
                        rulesSet.add(_temp);
                    }
                }
            }
        }
        return unmodifiableSet(rulesSet);
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

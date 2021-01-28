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

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import de.tudarmstadt.ukp.clarin.webanno.constraints.grammar.ASTConstraintsSet;
import de.tudarmstadt.ukp.clarin.webanno.constraints.grammar.ASTRule;

/***
 * Serialized Class containing objects after parsing and creating objects based on rules file.
 */
public class ParsedConstraints
    implements Serializable
{
    private static final long serialVersionUID = -2401965871743170805L;

    private final Map<String, String> imports;
    private final List<Scope> scopes;
    private Map<String, Scope> scopeMap = null;
    // Contains possible scenarios for which rules are available.
    private Set<FSFPair> rulesSet = null;

    public ParsedConstraints(Map<String, String> aAliases, List<Scope> aScopes)
    {
        imports = aAliases;
        scopes = aScopes;
    }

    public ParsedConstraints(ASTConstraintsSet astConstraintsSet)
    {
        imports = astConstraintsSet.getImports();
        scopes = new ArrayList<>();

        for (Entry<String, List<ASTRule>> ruleGroup : astConstraintsSet.getScopes().entrySet()) {
            List<Rule> rules = new ArrayList<Rule>();
            for (ASTRule astRule : ruleGroup.getValue()) {
                rules.add(new Rule(astRule));
            }
            scopes.add(new Scope(ruleGroup.getKey(), rules));
        }
    }

    @Override
    public String toString()
    {
        return "Imports\n[" + printImports() + "]\n" + scopes.toString() + "]\n]";
    }

    private String printImports()
    {
        StringBuilder output = new StringBuilder();
        for (Entry<String, String> e : imports.entrySet()) {
            output.append(e.getKey());
            output.append(" is short for ");
            output.append(e.getValue());
            output.append(System.lineSeparator());
        }
        return output.toString();
    }

    public Map<String, String> getImports()
    {
        return imports;
    }

    public String getShortName(String aLongName)
    {
        for (Entry<String, String> e : imports.entrySet()) {
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
            scopeMap = new HashMap<>();
            for (Scope scope : scopes) {
                scopeMap.put(scope.getScopeName(), scope);
            }
        }
        return scopeMap.get(scopeName);
    }

    /**
     * Checks if rules exists or not
     */
    public boolean areThereRules(String featureStructure, String feature)
    {
        if (rulesSet == null) {
            buildRulesSet();
        }

        if (getShortName(featureStructure) == null) {
            return false;
        }
        if (getScopeByName(getShortName(featureStructure)) == null) {
            return false;
        }
        FSFPair _tempFsfPair = new FSFPair(getShortName(featureStructure), feature);
        if (rulesSet.contains(_tempFsfPair)) {
            // If it has rules satisfying with proper input FS and affecting feature
            return true;
        }
        return false;
    }

    /**
     * Fill Set with values of different conditions for which rules are available.
     */
    private void buildRulesSet()
    {
        rulesSet = new HashSet<>();
        FSFPair _temp;
        for (Scope scope : scopes) {
            for (Rule rule : scope.getRules()) {
                for (Restriction restriction : rule.getRestrictions()) {
                    _temp = new FSFPair(scope.getScopeName(), restriction.getPath());
                    if (!rulesSet.contains(_temp)) {
                        rulesSet.add(_temp);
                    }
                }
            }
        }
    }
}

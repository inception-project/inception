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

import static java.util.Arrays.asList;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import de.tudarmstadt.ukp.clarin.webanno.constraints.grammar.ASTCondition;
import de.tudarmstadt.ukp.clarin.webanno.constraints.grammar.ASTRestriction;
import de.tudarmstadt.ukp.clarin.webanno.constraints.grammar.ASTRule;

/**
 * Class representing object representation of a Rule, contains Condition(s) and Restriction(s)
 */
public class Rule
    implements Serializable
{
    private static final long serialVersionUID = 5230339537568449002L;

    private final List<Condition> conditions;
    private final List<Restriction> restrictions;

    public Rule(ASTRule aRule)
    {
        conditions = new ArrayList<>();
        for (ASTCondition astCondition : aRule.getConditions().getConditions()) {
            conditions.add(new Condition(astCondition));
        }

        restrictions = new ArrayList<>();
        for (ASTRestriction astRestriction : aRule.getRestrictions().getRestrictions()) {
            restrictions.add(new Restriction(astRestriction));
        }
    }

    public Rule(Condition aCondition, Restriction aRestriction)
    {
        conditions = asList(aCondition);
        restrictions = asList(aRestriction);
    }

    public Rule(List<Condition> aConditions, List<Restriction> aRestrictions)
    {
        conditions = aConditions;
        restrictions = aRestrictions;
    }

    public List<Condition> getConditions()
    {
        return conditions;
    }

    public List<Restriction> getRestrictions()
    {
        return restrictions;
    }

    @Override
    public int hashCode()
    {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((conditions == null) ? 0 : conditions.hashCode());
        result = prime * result + ((restrictions == null) ? 0 : restrictions.hashCode());
        return result;
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
        Rule other = (Rule) obj;
        if (conditions == null) {
            if (other.conditions != null) {
                return false;
            }
        }
        else if (!conditions.equals(other.conditions)) {
            return false;
        }
        if (restrictions == null) {
            if (other.restrictions != null) {
                return false;
            }
        }
        else if (!restrictions.equals(other.restrictions)) {
            return false;
        }
        return true;
    }

    @Override
    public String toString()
    {
        return "Rule [conditions=" + conditions + ", restrictions=" + restrictions + "]";
    }
}

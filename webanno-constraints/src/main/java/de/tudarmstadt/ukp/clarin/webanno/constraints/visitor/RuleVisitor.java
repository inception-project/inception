/*
 * Copyright 2015
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
package de.tudarmstadt.ukp.clarin.webanno.constraints.visitor;

import java.util.ArrayList;
import java.util.List;

import de.tudarmstadt.ukp.clarin.webanno.constraints.grammar.syntaxtree.CLRuleDeclaration;
import de.tudarmstadt.ukp.clarin.webanno.constraints.grammar.visitor.GJVoidDepthFirst;
import de.tudarmstadt.ukp.clarin.webanno.constraints.model.Condition;
import de.tudarmstadt.ukp.clarin.webanno.constraints.model.Restriction;
import de.tudarmstadt.ukp.clarin.webanno.constraints.model.Rule;
/**
 * Visitor for Rules
 *
 */
public class RuleVisitor
    extends GJVoidDepthFirst<List<Rule>>
{
    @Override
    public void visit(CLRuleDeclaration aRuleNode, List<Rule> aRules)
    {
        List<Condition> conditions = new ArrayList<>();
        List<Restriction> restrictions = new ArrayList<>();

        // super.visit(aN, aArgu);

        aRuleNode.accept(new ConditionVisitor(), conditions);
        aRuleNode.accept(new RestrictionVisitor(), restrictions);

        aRules.add(new Rule(conditions, restrictions));
    }
}

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

import de.tudarmstadt.ukp.clarin.webanno.constraints.grammar.syntaxtree.CLScopedDeclarations;
import de.tudarmstadt.ukp.clarin.webanno.constraints.grammar.visitor.GJVoidDepthFirst;
import de.tudarmstadt.ukp.clarin.webanno.constraints.model.Rule;
import de.tudarmstadt.ukp.clarin.webanno.constraints.model.Scope;

/**
 * Visitor for scopeName
 */
public class ScopeVisitor
    extends GJVoidDepthFirst<List<Scope>>
{
    @Override
    public void visit(CLScopedDeclarations n, List<Scope> argu)
    {
        String scope = n.f0.f0.tokenImage;

        List<Rule> rules = new ArrayList<>();
        n.accept(new RuleVisitor(), rules);

        argu.add(new Scope(scope, rules));
    }
}

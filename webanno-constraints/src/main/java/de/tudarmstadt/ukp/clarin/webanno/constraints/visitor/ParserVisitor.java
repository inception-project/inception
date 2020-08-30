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
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import de.tudarmstadt.ukp.clarin.webanno.constraints.grammar.syntaxtree.CLParse;
import de.tudarmstadt.ukp.clarin.webanno.constraints.grammar.visitor.GJNoArguDepthFirst;
import de.tudarmstadt.ukp.clarin.webanno.constraints.model.ParsedConstraints;
import de.tudarmstadt.ukp.clarin.webanno.constraints.model.Scope;
/**
 * Visitor for complete rules including scopes.
 *
 */
public class ParserVisitor
    extends GJNoArguDepthFirst<ParsedConstraints>
{
    private Map<String, String> imports = new LinkedHashMap<>();
    private List<Scope> scopes = new ArrayList<>();

    @Override
    public ParsedConstraints visit(CLParse n)
    {

        n.accept(new ImportVisitor(), imports);
        n.accept(new ScopeVisitor(), scopes);

        return new ParsedConstraints(imports, scopes);
    }
}

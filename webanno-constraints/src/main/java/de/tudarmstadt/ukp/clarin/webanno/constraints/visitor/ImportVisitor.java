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

import java.util.Map;

import de.tudarmstadt.ukp.clarin.webanno.constraints.grammar.syntaxtree.CLImportDeclaration;
import de.tudarmstadt.ukp.clarin.webanno.constraints.grammar.visitor.GJVoidDepthFirst;

public class ImportVisitor
    extends GJVoidDepthFirst<Map<String, String>>
{
    @Override
    public void visit(CLImportDeclaration aImportNode, Map<String, String> aAliasTable)
    {
        super.visit(aImportNode, aAliasTable);

        StringBuilder qualifiedTypeName = new StringBuilder();
        aImportNode.cLQualifiedTypeName.accept(new TokenNodesToStringVisitor(), qualifiedTypeName);

        StringBuilder shortTypeName = new StringBuilder();
        aImportNode.cLShortTypeName.accept(new TokenNodesToStringVisitor(), shortTypeName);

        aAliasTable.put(shortTypeName.toString(), qualifiedTypeName.toString());
    }
}

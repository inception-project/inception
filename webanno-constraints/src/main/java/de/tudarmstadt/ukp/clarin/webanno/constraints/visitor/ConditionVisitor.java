/*******************************************************************************
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
 ******************************************************************************/
package de.tudarmstadt.ukp.clarin.webanno.constraints.visitor;

import java.util.List;

import de.tudarmstadt.ukp.clarin.webanno.constraints.grammar.syntaxtree.NodeToken;
import de.tudarmstadt.ukp.clarin.webanno.constraints.grammar.syntaxtree.Path;
import de.tudarmstadt.ukp.clarin.webanno.constraints.grammar.visitor.DepthFirstVisitor;
import de.tudarmstadt.ukp.clarin.webanno.constraints.grammar.visitor.GJVoidDepthFirst;
import de.tudarmstadt.ukp.clarin.webanno.constraints.model.Condition;
/**
 * Visitor for Condition
 * @author aakash
 *
 */
public class ConditionVisitor
    extends GJVoidDepthFirst<List<Condition>>
{
    private String path;
    private String value;

    @Override
    public void visit(
            de.tudarmstadt.ukp.clarin.webanno.constraints.grammar.syntaxtree.Condition aN,
            List<Condition> aArgu)
    {
        path = null;
        value = QuoteUtil.unquote(aN.f2.tokenImage);

        super.visit(aN, aArgu);

        aArgu.add(new Condition(path, value));
    }

    @Override
    public void visit(Path aN, List<Condition> aArgu)
    {
        super.visit(aN, aArgu);

        aN.accept(new DepthFirstVisitor()
        {
            @Override
            public void visit(NodeToken aN)
            {
                path = aN.tokenImage;
            }
        });
    }
}

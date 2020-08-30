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

import java.util.List;

import de.tudarmstadt.ukp.clarin.webanno.constraints.grammar.syntaxtree.CLFlagImportant;
import de.tudarmstadt.ukp.clarin.webanno.constraints.grammar.syntaxtree.CLRestriction;
import de.tudarmstadt.ukp.clarin.webanno.constraints.grammar.visitor.DepthFirstVisitor;
import de.tudarmstadt.ukp.clarin.webanno.constraints.grammar.visitor.GJVoidDepthFirst;
import de.tudarmstadt.ukp.clarin.webanno.constraints.model.Restriction;

/**
 * Visitor for Restriction
 */
public class RestrictionVisitor
    extends GJVoidDepthFirst<List<Restriction>>
{
    private boolean flagImportant;

    @Override
    public void visit(CLRestriction aRestrictionNode, List<Restriction> aRestrictions)
    {
        flagImportant = false;

        StringBuilder path = new StringBuilder();
        aRestrictionNode.cLPath.accept(new TokenNodesToStringVisitor(), path);

        String value = QuoteUtil.unquote(aRestrictionNode.cLRestrictionValue.nodeToken.tokenImage);
        
        if (aRestrictionNode.nodeOptional.present()) {
            aRestrictionNode.nodeOptional.accept(new DepthFirstVisitor() {
                @Override
                public void visit(CLFlagImportant n)
                {
                    flagImportant = true;
                }
            });
        }

        aRestrictions.add(new Restriction(path.toString(), value, flagImportant));
    }
}

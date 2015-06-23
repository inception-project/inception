package de.tudarmstadt.ukp.clarin.webanno.constraints.visitor;

import java.util.ArrayList;
import java.util.List;

import de.tudarmstadt.ukp.clarin.webanno.constraints.grammar.syntaxtree.ScopedDeclarations;
import de.tudarmstadt.ukp.clarin.webanno.constraints.grammar.visitor.GJVoidDepthFirst;
import de.tudarmstadt.ukp.clarin.webanno.constraints.model.Condition;
import de.tudarmstadt.ukp.clarin.webanno.constraints.model.Rule;
import de.tudarmstadt.ukp.clarin.webanno.constraints.model.Scope;

public class ScopeVisitor
    extends GJVoidDepthFirst<List<Scope>>
{

    @Override
    public void visit(ScopedDeclarations n, List<Scope> argu)
    {

        String scope = n.f0.toString();

        List<Rule> rules = new ArrayList<>();
        n.accept(new RuleVisitor(), rules);

        argu.add(new Scope(scope, rules));

        // super.visit(n, argu);
    }

}

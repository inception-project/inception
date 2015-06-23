package de.tudarmstadt.ukp.clarin.webanno.constraints.visitor;

import java.util.ArrayList;
import java.util.List;

import de.tudarmstadt.ukp.clarin.webanno.constraints.grammar.syntaxtree.RuleDeclaration;
import de.tudarmstadt.ukp.clarin.webanno.constraints.grammar.visitor.GJVoidDepthFirst;
import de.tudarmstadt.ukp.clarin.webanno.constraints.model.Condition;
import de.tudarmstadt.ukp.clarin.webanno.constraints.model.Restriction;
import de.tudarmstadt.ukp.clarin.webanno.constraints.model.Rule;

public class RuleVisitor
    extends GJVoidDepthFirst<List<Rule>>
{
    @Override
    public void visit(RuleDeclaration aN, List<Rule> aArgu)
    {
        List<Condition> conditions = new ArrayList<>();
        List<Restriction> restrictions = new ArrayList<>();

        // super.visit(aN, aArgu);

        aN.accept(new ConditionVisitor(), conditions);
        aN.accept(new RestrictionVisitor(), restrictions);

        aArgu.add(new Rule(conditions, restrictions));
    }
}

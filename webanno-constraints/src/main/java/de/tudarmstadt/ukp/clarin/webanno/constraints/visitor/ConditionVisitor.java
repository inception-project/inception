package de.tudarmstadt.ukp.clarin.webanno.constraints.visitor;

import java.util.List;

import de.tudarmstadt.ukp.clarin.webanno.constraints.grammar.syntaxtree.NodeToken;
import de.tudarmstadt.ukp.clarin.webanno.constraints.grammar.syntaxtree.Path;
import de.tudarmstadt.ukp.clarin.webanno.constraints.grammar.visitor.DepthFirstVisitor;
import de.tudarmstadt.ukp.clarin.webanno.constraints.grammar.visitor.GJVoidDepthFirst;
import de.tudarmstadt.ukp.clarin.webanno.constraints.model.Condition;

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
